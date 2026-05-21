#!/usr/bin/env python3
"""Generate ``catalog/v1/models.json`` from the upstream sherpa-onnx TTS index.

Scrapes https://k2-fsa.github.io/sherpa/onnx/tts/all/, extracts every TTS
voice's metadata + bundle URL + speaker layout, then stream-downloads each
bundle to compute its sha256 (without persisting the tarball). Output is a
deterministic, alphabetically-sorted JSON file matching the ``VoiceCard``
Kotlin schema used by HayaiTTS.

Per the task spec: failing entries are logged to stderr and OMITTED. Exit
code is always 0 unless the upstream index itself is unreachable.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import sys
import threading
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass, field

import requests

INDEX_ROOT = "https://k2-fsa.github.io/sherpa/onnx/tts/all/"

# Map mdBook language directory -> BCP47 tag(s). The id's `en_US` locale wins
# when present; this dict is the fallback for slugs with no locale segment.
LANGUAGE_DIR_BCP47: dict[str, list[str]] = {
    "Albanian": ["sq"], "Arabic": ["ar"], "Basque": ["eu"], "Bulgarian": ["bg"],
    "Catalan": ["ca"], "Chinese": ["zh-CN"], "Croatian": ["hr"], "Czech": ["cs"],
    "Danish": ["da"], "Dutch": ["nl"], "English": ["en-US"], "Estonian": ["et"],
    "Finnish": ["fi"], "French": ["fr-FR"], "Georgian": ["ka"], "German": ["de-DE"],
    "Greek": ["el"], "Hindi": ["hi"], "Hungarian": ["hu"], "Icelandic": ["is"],
    "Indonesian": ["id"], "Italian": ["it-IT"], "Japanese": ["ja-JP"], "Kazakh": ["kk"],
    "Korean": ["ko-KR"], "Kurdish": ["ku"], "Latvian": ["lv"], "Lithuanian": ["lt"],
    "Luxembourgish": ["lb"], "Malayalam": ["ml"], "Nepali": ["ne"],
    "Norwegian": ["no"], "Persian": ["fa"], "Polish": ["pl"], "Portuguese": ["pt"],
    "Romanian": ["ro"], "Russian": ["ru"], "Serbian": ["sr"], "Slovak": ["sk"],
    "Slovenian": ["sl"], "Spanish": ["es-ES"], "Swahili": ["sw"], "Swedish": ["sv"],
    "Turkish": ["tr"], "Ukrainian": ["uk"], "Urdu": ["ur"], "Vietnamese": ["vi"],
    "Welsh": ["cy"], "Chinese-English": ["zh-CN", "en-US"],
}

# Best-effort license per family — sourced from the upstream training repos.
FAMILY_DEFAULT_LICENSE = {
    "piper": "MIT", "vits": "Apache-2.0", "matcha": "CC-BY-4.0",
    "kokoro": "Apache-2.0", "kitten": "Apache-2.0", "supertonic": "CC-BY-NC-4.0",
}

# Families that `SherpaTtsRuntime` currently builds `OfflineTtsConfig`s for.
# Mirrors the non-CUSTOM entries of the Kotlin `ModelFamily` enum. Anything
# outside this set is emitted into the catalog with `available: false` so
# Browse can surface it as "Coming Soon" until the runtime config builder
# lands.
RUNTIME_SUPPORTED_FAMILIES = {
    "piper", "vits", "matcha", "kokoro", "kitten",
    "zipvoice", "pocket", "supertonic",
}


@dataclass
class Voice:
    """In-memory representation matching the Kotlin ``VoiceCard`` schema."""
    id: str
    family: str
    title: str
    languages: list[str]
    speakers: list[dict]
    sampleRateHz: int
    approxSizeMb: int
    tier: str
    license: str
    bundleUrl: str
    sha256: str | None = None
    available: bool = True
    vocoderUrl: str | None = None
    vocoderFileName: str | None = None
    vocoderSha256: str | None = None
    modelFileName: str | None = None
    lexiconFileName: str | None = None
    dictDirName: str | None = None
    demoUrl: str | None = None
    sampleAudioUrl: str | None = None

    def to_json(self) -> dict:
        # Required fields; optional ones only when set so the JSON stays compact.
        d: dict = {
            "id": self.id, "family": self.family, "title": self.title,
            "languages": self.languages, "speakers": self.speakers,
            "sampleRateHz": self.sampleRateHz, "approxSizeMb": self.approxSizeMb,
            "tier": self.tier, "license": self.license,
            "bundleUrl": self.bundleUrl, "sha256": self.sha256,
            "available": self.available,
        }
        for k in ("vocoderUrl", "vocoderFileName", "vocoderSha256",
                  "modelFileName", "lexiconFileName", "dictDirName",
                  "demoUrl", "sampleAudioUrl"):
            v = getattr(self, k)
            if v is not None:
                d[k] = v
        return d


@dataclass
class RunStats:
    written: int = 0
    skipped: list[tuple[str, str]] = field(default_factory=list)
    total_bytes: int = 0
    started_at: float = field(default_factory=time.time)
    lock: threading.Lock = field(default_factory=threading.Lock)


def log(msg: str) -> None:
    sys.stdout.buffer.write((msg + "\n").encode("utf-8", errors="replace"))
    sys.stdout.flush()


def err(msg: str) -> None:
    sys.stderr.buffer.write((msg + "\n").encode("utf-8", errors="replace"))
    sys.stderr.flush()


def fetch_index_searchindex() -> str:
    """Return the contents of the rotating ``searchindex-*.js`` referenced by
    the TTS index page (its filename hash changes on each upstream rebuild)."""
    r = requests.get(INDEX_ROOT, timeout=30); r.raise_for_status()
    m = re.search(r'searchindex_js\s*=\s*"([^"]+\.js)"', r.text) or \
        re.search(r'src="(searchindex-[A-Za-z0-9]+\.js)"', r.text)
    if not m:
        raise RuntimeError("Could not find searchindex JS on TTS index page")
    r2 = requests.get(INDEX_ROOT + m.group(1), timeout=60); r2.raise_for_status()
    return r2.text


def discover_model_pages(search_js: str) -> list[tuple[str, str]]:
    """``[(language_dir, slug)]`` for every two-segment .html page referenced
    from the mdBook ``doc_urls`` array that maps to a known TTS family."""
    m = re.search(r'"doc_urls"\s*:\s*\[(.*?)\]', search_js, re.S)
    if not m:
        raise RuntimeError("doc_urls array missing from searchindex JS")
    pairs: set[tuple[str, str]] = set()
    for u in re.findall(r'"([^"]+)"', m.group(1)):
        path = u.split("#", 1)[0]
        if not path.endswith(".html") or path.count("/") != 1: continue
        if path.endswith("/index.html") or path == "print.html": continue
        lang, slug_html = path.split("/", 1)
        slug = slug_html.removesuffix(".html")
        if family_for_slug(slug) == "unknown": continue
        pairs.add((lang, slug))
    return sorted(pairs)


def family_for_slug(slug: str) -> str:
    if slug.startswith("vits-piper-"): return "piper"
    if slug.startswith("vits-"): return "vits"
    if slug.startswith("matcha-"): return "matcha"
    if slug.startswith("kokoro-"): return "kokoro"
    if slug.startswith("kitten-"): return "kitten"
    # The three SOTA k2-fsa families ship their bundles (and document their
    # subpages) under `sherpa-onnx-<family>-...` names. Match the family by
    # substring rather than by prefix so the heuristic catches both the
    # `sherpa-onnx-` prefix and any short-form slug a future upstream commit
    # might use.
    if "supertonic" in slug: return "supertonic"
    if "zipvoice" in slug: return "zipvoice"
    if "pocket-tts" in slug or slug.startswith("pocket-"): return "pocket"
    return "unknown"


def parse_subpage(language: str, slug: str) -> Voice | None:
    """Hit one model subpage and extract every field for a Voice record."""
    url = INDEX_ROOT + language + "/" + slug + ".html"
    try:
        r = requests.get(url, timeout=30); r.raise_for_status()
    except Exception as e:
        err(f"  subpage fetch failed for {slug}: {e}"); return None
    text = r.text

    bm = re.search(
        r'https://github\.com/k2-fsa/sherpa-onnx/releases/download/tts-models/[^"\s<)]+\.tar\.bz2',
        text)
    if not bm:
        err(f"  no tts-models tarball link on subpage {slug}"); return None
    bundle_url = bm.group(0).replace("/./", "/")
    voice_id = bundle_url.rsplit("/", 1)[-1].removesuffix(".tar.bz2")
    family = family_for_slug(voice_id)
    if family == "unknown":
        err(f"  unknown family for id={voice_id} (slug={slug})"); return None

    num_speakers, sample_rate = parse_speakers_and_rate(text)
    if num_speakers is None or sample_rate is None:
        err(f"  missing speakers/rate for {voice_id}"); return None

    named = parse_named_speakers(text)
    if named:
        speakers = []
        for sid, name in sorted(named.items()):
            g, conf = infer_gender(voice_id, name, family)
            speakers.append({"id": sid, "name": name, "gender": g, "genderConfidence": conf})
    elif num_speakers == 1:
        single_name = derive_single_speaker_name(voice_id)
        g, conf = infer_gender(voice_id, single_name, family)
        speakers = [{"id": 0, "name": single_name, "gender": g, "genderConfidence": conf}]
    else:
        speakers = [{"id": i, "name": f"speaker_{i}", "gender": "U", "genderConfidence": "unknown"}
                    for i in range(num_speakers)]

    vocoder_url = vocoder_file = None
    if family == "matcha":
        m = re.search(
            r'https://github\.com/k2-fsa/sherpa-onnx/releases/download/vocoder-models/[^"\s<)]+\.onnx',
            text)
        if not m:
            err(f"  matcha {voice_id} has no vocoder URL — skipping"); return None
        vocoder_url = m.group(0).replace("/./", "/")
        vocoder_file = vocoder_url.rsplit("/", 1)[-1]

    return Voice(
        id=voice_id, family=family,
        title=derive_title(speakers, family),
        languages=derive_languages(voice_id, language, text),
        speakers=speakers, sampleRateHz=sample_rate,
        approxSizeMb=0,  # filled by HEAD pass
        tier=derive_tier(voice_id, family),
        license=FAMILY_DEFAULT_LICENSE.get(family, "unknown"),
        bundleUrl=bundle_url,
        # Families outside `RUNTIME_SUPPORTED_FAMILIES` are catalogued but
        # surfaced as "Coming Soon" in Browse until `SherpaTtsRuntime` learns
        # to build configs for them.
        available=family in RUNTIME_SUPPORTED_FAMILIES,
        vocoderUrl=vocoder_url, vocoderFileName=vocoder_file,
        modelFileName=derive_model_filename(text, family),
        lexiconFileName="lexicon.txt" if "lexicon.txt" in text else None,
        dictDirName="dict" if re.search(r'\bdict/', text) else None,
        demoUrl=demo_url_for(family, voice_id),
    )


# Hosted demos let users hear a voice before downloading. We point at the
# upstream HuggingFace Space for each family where one is publicly maintained
# and ungated. Per-voice deep links don't exist on most spaces, so the Space
# root is the best we can offer; the user picks the voice from the Space UI.
def demo_url_for(family: str, voice_id: str) -> str | None:
    if family in ("piper", "vits"):
        # k2-fsa's official sherpa-onnx TTS demo Space exposes a model picker
        # that includes every Piper voice we catalogue.
        return "https://huggingface.co/spaces/k2-fsa/text-to-speech"
    if family == "kokoro":
        return "https://huggingface.co/spaces/hexgrad/Kokoro-TTS"
    if family == "kitten":
        return "https://huggingface.co/KittenML/kitten-tts-nano-0.2"
    if family == "matcha":
        return "https://huggingface.co/spaces/shivammehta25/Matcha-TTS"
    if family == "supertonic":
        return "https://huggingface.co/Supertone/supertonic"
    return None


def parse_speakers_and_rate(text: str) -> tuple[int | None, int | None]:
    section = re.search(
        r'Number of speakers.*?Sample rate(.*?)(?:Speaker IDs|Speaker ID|speaker name|Download the)',
        text, re.S)
    if not section: return None, None
    chunk = re.sub(r'\s+', ' ', re.sub(r'<[^>]+>', ' ', section.group(1))).strip()
    m = re.match(r'(\d+)\s+(\d+)', chunk)
    return (int(m.group(1)), int(m.group(2))) if m else (None, None)


def parse_named_speakers(text: str) -> dict[int, str]:
    # The "speaker name to speaker ID" section may sit either before or after
    # its inverse; bound it at the next <h2 / Download / API marker.
    section = re.search(
        r'speaker name to speaker ID(.*?)(?:<h2|Download the|API)',
        text, re.S | re.I)
    if not section: return {}
    out: dict[int, str] = {}
    for m in re.finditer(r'([A-Za-z][\w\-]*)\s*-&gt;\s*(\d+)', section.group(1)):
        out.setdefault(int(m.group(2)), m.group(1))
    return out


def derive_single_speaker_name(voice_id: str) -> str:
    parts = voice_id.split("-")
    locale_re = re.compile(r'^[a-z]{2}_[A-Z]{2}$')
    for i, p in enumerate(parts):
        if locale_re.match(p) and i + 1 < len(parts):
            return parts[i + 1]
    skip = {"low", "medium", "high", "fp16", "fp32", "int8"}
    cleaned = [p for p in parts if p not in skip]
    return cleaned[-1] if cleaned else parts[-1]


def derive_title(speakers: list[dict], family: str) -> str:
    if len(speakers) == 1:
        n = speakers[0]["name"]; return n[:1].upper() + n[1:]
    fam = {"piper": "Piper", "vits": "VITS", "matcha": "Matcha",
           "kokoro": "Kokoro", "kitten": "Kitten",
           "supertonic": "Supertonic"}.get(family, family.capitalize())
    return f"{fam} ({len(speakers)} speakers)"


def derive_tier(voice_id: str, family: str) -> str:
    """Tier heuristic per spec section 3."""
    if family in ("kokoro", "matcha", "supertonic"): return "high"
    if voice_id.endswith(("-x_low", "-low")): return "low"
    if voice_id.endswith("-high"): return "high"
    if "-medium" in voice_id: return "mid"
    if family == "kitten": return "low"
    return "mid"


def derive_languages(voice_id: str, language_dir: str, text: str) -> list[str]:
    """Priority: explicit locale in id -> Supertonic-style list -> directory."""
    m = re.search(r'\b([a-z]{2})_([A-Z]{2})\b', voice_id)
    if m: return [f"{m.group(1)}-{m.group(2)}"]
    m2 = re.search(r'supports\s+\d+\s+languages?\s*:\s*([^.<]+)', text)
    if m2:
        codes = [c.strip() for c in m2.group(1).split(",") if c.strip()]
        if codes: return codes
    m3 = re.search(r'It supports only\s+([A-Za-z\-]+)', text)
    if m3: return LANGUAGE_DIR_BCP47.get(m3.group(1), [m3.group(1).lower()])
    return LANGUAGE_DIR_BCP47.get(language_dir, ["und"])


def derive_model_filename(text: str, family: str) -> str | None:
    """Pick up the on-disk weight filename from the Python sample when it
    differs from the family default. Returns None to use the default."""
    patterns = [r'config\.model\.vits\.model\s*=\s*"([^"]+\.onnx)"',
                r'\bmodel\s*=\s*"([^"]+\.onnx)"']
    defaults = {"piper": {"model.onnx"}, "vits": {"model.onnx"},
                "matcha": {"model-steps-3.onnx"},
                "kokoro": {"model.onnx"}, "kitten": {"model.onnx"}}
    for pat in patterns:
        m = re.search(pat, text)
        if not m: continue
        fname = m.group(1).rsplit("/", 1)[-1]
        return None if fname in defaults.get(family, set()) else fname
    return None


def gender_for(speaker_name: str) -> str:
    """Kokoro `af`/`am`/`bf`/`bm` prefixes + Kitten `-f`/`-m` suffixes."""
    n = speaker_name.lower()
    if n.startswith(("af", "bf", "cf")) or n.endswith(("-f", "_f")): return "F"
    if n.startswith(("am", "bm", "cm")) or n.endswith(("-m", "_m")): return "M"
    return "U"


# Hand-curated gender map for Piper single-speaker voices. The upstream Piper
# release model cards usually identify the gender in prose; we transcribe the
# known set here so the Browse "Female / Male" filter has data to work with.
#
# Voices NOT in this map fall through to "U" (Unknown) and the catalog tags
# them `genderConfidence: "unknown"` so the UI can render them distinctly.
# Add new entries here as upstream ships new voices.
#
# Keys are the voice's derived speaker name (per `derive_single_speaker_name`),
# normalized lowercase.
PIPER_VOICE_GENDERS: dict[str, str] = {
    # en_US
    "amy": "F", "kathleen": "F", "lessac": "F", "ljspeech": "F",
    "libritts": "U", "libritts_r": "U",  # multi-speaker — handled per-name elsewhere
    "joe": "M", "ryan": "M", "kusal": "M", "danny": "M", "norman": "M",
    "hfc_female": "F", "hfc_male": "M",
    "glados": "F", "arctic": "M", "l2arctic": "U",
    # en_GB
    "alan": "M", "alba": "F", "aru": "F", "cori": "F",
    "jenny_dioco": "F", "jenny": "F", "semaine": "U",
    "southern_english_female": "F", "northern_english_male": "M",
    "vctk": "U",  # multi-speaker
    # es_ES / es_MX
    "davefx": "M", "sharvard": "U", "claude": "M", "ald": "F",
    "carlfm": "M",
    # de_DE
    "thorsten": "M", "thorsten_emotional": "M",
    "eva_k": "F", "karlsson": "M", "kerstin": "F",
    "pavoque": "M", "ramona": "F",
    # fr_FR
    "gilles": "M", "siwis": "F", "tom": "M", "upmc": "U",
    # it_IT
    "paola": "F", "riccardo": "M",
    # pl_PL
    "darkman": "M", "gosia": "F",
    # pt_BR / pt_PT
    "faber": "M", "edresson": "M", "tugão": "M", "tugao": "M",
    # ru_RU
    "denis": "M", "dmitri": "M", "irina": "F", "ruslan": "M",
    # uk_UA
    "lada": "F", "ukrainian_tts": "U",
    # vi_VN
    "vais1000": "M", "25hours_single": "U",
    # zh_CN
    "huayan": "F",
    # ar_JO
    "kareem": "M",
    # cs_CZ
    "jirka": "M",
    # da_DK
    "talesyntese": "U",
    # el_GR
    "rapunzelina": "F",
    # fa_IR
    "amir": "M", "ganji": "M", "gyro": "M",
    "haaniye": "F",
    # fi_FI
    "harri": "M",
    # hu_HU
    "anna": "F", "berta": "F", "imre": "M",
    # is_IS
    "bui": "M", "salka": "F", "steinn": "M", "ugla": "F",
    # ka_GE
    "natia": "F",
    # kk_KZ
    "iseke": "F", "issai": "M", "raya": "F",
    # lb_LU
    "marylux": "F",
    # ne_NP
    "google": "U", "chitwanian": "M",
    # nl_BE / nl_NL
    "nathalie": "F", "rdh": "M",
    "mls": "U",  # multi-speaker libri/MLS
    # no_NO
    "talesyntese_no": "U",
    # ro_RO
    "mihai": "M",
    # sk_SK
    "lili": "F",
    # sl_SI
    "artur": "M",
    # sr_RS
    "serbski_institut": "U",
    # sv_SE
    "nst": "U", "lisa": "F",
    # sw_KE
    "sw_lanfrica": "U",
    # tr_TR
    "dfki": "M", "fahrettin": "M", "fettah": "M",
}


def infer_gender(voice_id: str, speaker_name: str, family: str) -> tuple[str, str]:
    """Return (gender, confidence) where:
      - confidence = "declared" when the upstream metadata explicitly carries gender
        (Kokoro/Kitten naming patterns).
      - confidence = "inferred" when we mapped a voice name through the Piper table.
      - confidence = "unknown" otherwise.
    The UI uses confidence to surface a "Has gender data" filter that excludes
    the unknowns so users can find labelled voices.
    """
    # Kokoro/Kitten declare gender via name prefix/suffix.
    pattern_gender = gender_for(speaker_name)
    if pattern_gender != "U":
        return pattern_gender, "declared"
    # Piper single-speaker voices — lookup the canonical name.
    if family in ("piper", "vits"):
        mapped = PIPER_VOICE_GENDERS.get(speaker_name.lower())
        if mapped is not None and mapped != "U":
            return mapped, "inferred"
    return "U", "unknown"


def head_size_mb(url: str) -> int:
    try:
        r = requests.head(url, allow_redirects=True, timeout=30)
        cl = r.headers.get("content-length")
        if cl: return max(1, round(int(cl) / 1_000_000))
    except Exception:
        pass
    return 0


def stream_sha256(url: str, *, label: str, cache_dir: str | None,
                  cache_key: str, stats: RunStats) -> tuple[str | None, int]:
    """Stream ``url`` through SHA-256 without saving to disk. Resumes from
    ``cache_dir/<cache_key>.sha256`` when present. Thread-safe."""
    cache_path = os.path.join(cache_dir, cache_key + ".sha256") if cache_dir else None
    if cache_path and os.path.isfile(cache_path):
        with open(cache_path, "r", encoding="utf-8") as fh:
            parts = fh.read().strip().split(None, 1)
        if len(parts) == 2 and re.fullmatch(r'[0-9a-f]{64}', parts[0]):
            return parts[0], int(parts[1])
    try:
        with requests.get(url, stream=True, timeout=180) as r:
            r.raise_for_status()
            h, n = hashlib.sha256(), 0
            for chunk in r.iter_content(chunk_size=256 * 1024):
                if not chunk: continue
                h.update(chunk); n += len(chunk)
            digest = h.hexdigest()
            with stats.lock:
                stats.total_bytes += n
            if cache_path:
                tmp = cache_path + ".tmp"
                with open(tmp, "w", encoding="utf-8") as fh:
                    fh.write(f"{digest} {n}\n")
                os.replace(tmp, cache_path)
            return digest, n
    except Exception as e:
        err(f"  hash failed for {label}: {e}"); return None, 0


def hash_voice(v: Voice, *, idx: int, total: int, cache_dir: str | None,
               skip_hash: bool, stats: RunStats,
               progress_lock: threading.Lock) -> Voice | None:
    """Per-voice work for the thread pool: HEAD for size, hash bundle, hash
    vocoder if present. Returns the populated Voice or None on failure."""
    v.approxSizeMb = head_size_mb(v.bundleUrl)
    if skip_hash:
        with progress_lock:
            log(f"[{idx}/{total}] {v.id}: {v.approxSizeMb} MB sha256=SKIPPED")
        return v
    digest, n = stream_sha256(v.bundleUrl, label=v.id, cache_dir=cache_dir,
                              cache_key=v.id, stats=stats)
    if digest is None:
        with stats.lock: stats.skipped.append((v.id, "bundle hash failed"))
        return None
    v.sha256 = digest
    if not v.approxSizeMb and n:
        v.approxSizeMb = max(1, round(n / 1_000_000))
    if v.vocoderUrl:
        vd, _ = stream_sha256(v.vocoderUrl, label=f"{v.id}#vocoder",
                              cache_dir=cache_dir,
                              cache_key=v.id + "__vocoder", stats=stats)
        if vd is None:
            with stats.lock: stats.skipped.append((v.id, "vocoder hash failed"))
            return None
        v.vocoderSha256 = vd
    with progress_lock:
        log(f"[{idx}/{total}] {v.id}: {v.approxSizeMb} MB sha256={digest[:12]}…")
    return v


def build_voices(pages: list[tuple[str, str]], *, limit: int | None,
                 cache_dir: str | None, skip_hash: bool,
                 workers: int, stats: RunStats) -> list[Voice]:
    todo = pages if limit is None else pages[:limit]
    total = len(todo)
    log(f"Discovered {total} model subpages.")

    # Parse pass — concurrent fetches against the upstream static mdBook host.
    raw: list[Voice] = []
    parse_failures: list[str] = []
    with ThreadPoolExecutor(max_workers=workers) as ex:
        futures = {ex.submit(parse_subpage, lang, slug): slug for (lang, slug) in todo}
        done = 0
        for f in as_completed(futures):
            v = f.result(); done += 1
            (raw.append(v) if v is not None else parse_failures.append(futures[f]))
            if done % 25 == 0 or done == total:
                log(f"  parsed {done}/{total}")
    for slug in parse_failures:
        stats.skipped.append((slug, "parse failed"))

    # Dedupe by id (same Kokoro lives under several language dirs); union langs.
    by_id: dict[str, Voice] = {}
    for v in raw:
        if v.id in by_id:
            for lang in v.languages:
                if lang not in by_id[v.id].languages:
                    by_id[v.id].languages.append(lang)
        else:
            by_id[v.id] = v
    log(f"Unique voices after dedupe: {len(by_id)}")
    ordered = sorted(by_id.values(), key=lambda x: x.id)

    if cache_dir:
        os.makedirs(cache_dir, exist_ok=True)

    # Hash pass — concurrent streaming. 8 workers is the empirical sweet spot
    # against the GitHub release CDN (more starts hitting 429).
    voices: list[Voice] = []
    progress_lock = threading.Lock()
    with ThreadPoolExecutor(max_workers=workers) as ex:
        futures = [
            ex.submit(hash_voice, v, idx=i, total=len(ordered),
                      cache_dir=cache_dir, skip_hash=skip_hash,
                      stats=stats, progress_lock=progress_lock)
            for i, v in enumerate(ordered, 1)
        ]
        for f in as_completed(futures):
            result = f.result()
            if result is not None:
                voices.append(result)
    return voices


def write_catalog(voices: list[Voice], output_path: str) -> None:
    """Canonical deterministic JSON: voices sorted by id, field keys sorted
    alphabetically, 2-space indent, ``\\n`` line endings, trailing newline."""
    payload = {
        "version": 1,
        "voices": [v.to_json() for v in sorted(voices, key=lambda v: v.id)],
    }
    rendered = json.dumps(payload, indent=2, ensure_ascii=False, sort_keys=True)
    out_dir = os.path.dirname(output_path)
    if out_dir:
        os.makedirs(out_dir, exist_ok=True)
    with open(output_path, "w", encoding="utf-8", newline="\n") as fh:
        fh.write(rendered + "\n")


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser(description="Build HayaiTTS voice catalog")
    ap.add_argument("--output", default=os.path.join("catalog", "v1", "models.json"))
    ap.add_argument("--cache-dir", default=None,
                    help="Persist <id>.sha256 files for resumeable reruns")
    ap.add_argument("--limit", type=int, default=None,
                    help="Only process the first N voices (dev)")
    ap.add_argument("--skip-hash", action="store_true",
                    help="Skip SHA-256 (dev only — yields null sha256)")
    ap.add_argument("--workers", type=int, default=8,
                    help="Concurrent HTTP streams for hashing (default 8)")
    args = ap.parse_args(argv)

    stats = RunStats()
    try:
        search_js = fetch_index_searchindex()
    except Exception as e:
        err(f"Fatal: could not fetch sherpa-onnx TTS index: {e}"); return 1
    pages = discover_model_pages(search_js)
    voices = build_voices(pages, limit=args.limit, cache_dir=args.cache_dir,
                          skip_hash=args.skip_hash, workers=args.workers,
                          stats=stats)
    stats.written = len(voices)
    write_catalog(voices, args.output)
    elapsed = time.time() - stats.started_at
    log("\n=== Summary ===")
    log(f"  voices written : {stats.written}")
    log(f"  voices skipped : {len(stats.skipped)}")
    for sid, reason in stats.skipped:
        log(f"     - {sid}: {reason}")
    log(f"  bytes hashed   : {stats.total_bytes:,} "
        f"({stats.total_bytes / 1_000_000_000:.2f} GB)")
    log(f"  runtime        : {elapsed:.1f}s")
    log(f"  output         : {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
