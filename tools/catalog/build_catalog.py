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

# `sources` lives alongside this script. Add the script dir to sys.path so
# the import resolves whether invoked as `python tools/catalog/build_catalog.py`
# (CI, cwd=repo-root) or from within tools/catalog/.
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import sources  # noqa: E402

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

# Meta MMS bundles carry a 3-letter ISO-639-3 code (`vits-mms-eng`,
# `vits-mms-deu`…). Map the common ones to a BCP-47 tag; anything not in the
# table keeps its 3-letter code as the language tag rather than being dropped.
ISO6393_TO_BCP47: dict[str, str] = {
    "eng": "en", "deu": "de-DE", "ger": "de-DE", "fra": "fr-FR", "fre": "fr-FR",
    "spa": "es-ES", "por": "pt", "ita": "it-IT", "rus": "ru", "ukr": "uk",
    "pol": "pl", "nld": "nl", "dut": "nl", "tha": "th", "vie": "vi",
    "ind": "id", "jpn": "ja-JP", "kor": "ko-KR", "zho": "zh-CN",
    "cmn": "zh-CN", "nan": "nan", "ara": "ar", "hin": "hi", "ben": "bn",
    "tur": "tr", "fas": "fa", "per": "fa", "ron": "ro", "rum": "ro",
    "ces": "cs", "cze": "cs", "slk": "sk", "slo": "sk", "hun": "hu",
    "fin": "fi", "swe": "sv", "dan": "da", "nor": "no", "ell": "el",
    "gre": "el", "heb": "he", "swa": "sw", "tam": "ta", "tel": "te",
    "urd": "ur", "guj": "gu", "kan": "kn", "mal": "ml", "mar": "mr",
}


def mms_language(voice_id: str) -> list[str] | None:
    """If ``voice_id`` is a Meta MMS slug (``vits-mms-<iso3>``), return its
    BCP-47 language list, mapping the ISO-639-3 code where known and keeping
    the raw 3-letter code as a fallback. Returns None for non-MMS ids."""
    m = re.match(r'^vits-mms-([a-z]{3})$', voice_id)
    if not m:
        return None
    code = m.group(1)
    return [ISO6393_TO_BCP47.get(code, code)]


# Best-effort license per family — sourced from the upstream training repos.
FAMILY_DEFAULT_LICENSE = {
    "piper": "MIT", "vits": "Apache-2.0", "matcha": "CC-BY-4.0",
    "kokoro": "Apache-2.0", "kitten": "Apache-2.0", "supertonic": "CC-BY-NC-4.0",
    "zipvoice": "Apache-2.0", "pocket": "Apache-2.0",
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
    # Per-(speaker, language) audition clips. Populated by the offline
    # `tools/samples/render_samples.py` pipeline, preserved here on
    # catalog refresh so the field doesn't disappear between renders.
    speakerSamples: list[dict] | None = None

    # ---------- network-enriched metadata (Stage 2) -------------------
    # Filled by `enrich_voices()` from upstream model cards / HF + GitHub
    # APIs. All optional — absent values stay omitted from to_json() so the
    # output is byte-identical to today's when enrichment is skipped.
    description: str | None = None
    dataset: str | None = None
    author: str | None = None
    sourceUrl: str | None = None
    baseModel: str | None = None
    tags: list[str] = field(default_factory=list)
    recommendedUseCases: list[str] = field(default_factory=list)
    quality: str | None = None
    # Internal-only: the dataset's own license (often differs from the model
    # weights' license, e.g. MIT weights trained on a CC-BY-NC corpus). Used
    # to decide whether to append a `dataset-license:` tag. NEVER serialized.
    _dataset_license: str | None = None

    def to_json(self) -> dict:
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
                  "demoUrl", "sampleAudioUrl", "speakerSamples",
                  "description", "dataset", "author", "sourceUrl",
                  "baseModel", "quality"):
            v = getattr(self, k)
            if v is not None:
                d[k] = v
        # List fields emit only when non-empty (match optional-field style).
        for k in ("tags", "recommendedUseCases"):
            v = getattr(self, k)
            if v:
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
        sampleAudioUrl=sample_audio_url_for(family, voice_id),
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


# Direct sample audio URLs that the in-app SampleAudioPlayer can stream via
# MediaPlayer. When this is populated the Voice Detail screen plays the clip
# inline; otherwise the user is offered the [demoUrl] HF Space as a fallback.
#
# Piper voices live at:
#   https://huggingface.co/rhasspy/piper-voices/resolve/main/<lang>/<lang_LC>/<name>/<quality>/samples/speaker_0.mp3
# We derive the path from the canonical voice id `<lang_LC>-<name>-<quality>`.
PIPER_LANG_PARENT: dict[str, str] = {
    # Two-letter top-level dirs in the rhasspy/piper-voices HF repo.
    "en": "en", "es": "es", "fr": "fr", "de": "de", "it": "it",
    "pt": "pt", "ru": "ru", "nl": "nl", "pl": "pl", "ar": "ar",
    "cs": "cs", "da": "da", "el": "el", "fa": "fa", "fi": "fi",
    "hu": "hu", "is": "is", "ja": "ja", "ka": "ka", "kk": "kk",
    "lb": "lb", "ne": "ne", "no": "no", "ro": "ro", "sk": "sk",
    "sl": "sl", "sr": "sr", "sv": "sv", "sw": "sw", "tr": "tr",
    "uk": "uk", "vi": "vi", "zh": "zh",
}


def sample_audio_url_for(family: str, voice_id: str) -> str | None:
    if family == "piper":
        # sherpa-onnx Piper ids are formatted `vits-piper-<lang>_<REGION>-<name>-<quality>`.
        # The HF piper-voices repo layout is:
        # `<lang>/<lang>_<REGION>/<name>/<quality>/samples/speaker_0.mp3`
        # Note: names can contain `_` (e.g. SA_dii, upc_ona) so we use a
        # non-greedy capture and pin on the closing quality suffix.
        m = re.match(
            r'^vits-piper-([a-z]{2})_([A-Z]{2})-(.+)-(low|medium|high|x_low)$',
            voice_id,
        )
        if not m:
            return None
        lang, region, name, quality = m.group(1), m.group(2), m.group(3), m.group(4)
        parent = PIPER_LANG_PARENT.get(lang)
        if parent is None:
            return None
        return (
            "https://huggingface.co/rhasspy/piper-voices/resolve/main/"
            f"{parent}/{lang}_{region}/{name}/{quality}/samples/speaker_0.mp3"
        )
    if family == "kokoro":
        # The upstream "csukuangfj/kokoro-multi-lang-v1_*" releases ship
        # pre-rendered demo waves named after the speaker_id. We pick
        # speaker 0 as the canonical preview.
        return "https://huggingface.co/spaces/hexgrad/Kokoro-TTS/resolve/main/sample.wav"
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
    # Meta MMS voices are narrowband single-speaker — always "low".
    if voice_id.startswith("vits-mms-"): return "low"
    if family in ("kokoro", "matcha", "supertonic"): return "high"
    if voice_id.endswith(("-x_low", "-low")): return "low"
    if voice_id.endswith("-high"): return "high"
    if "-medium" in voice_id: return "mid"
    if family == "kitten": return "low"
    return "mid"


def derive_languages(voice_id: str, language_dir: str, text: str) -> list[str]:
    """Priority: MMS ISO-639-3 -> explicit locale in id -> Supertonic-style
    list -> directory."""
    mms = mms_language(voice_id)
    if mms: return mms
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
    """Kokoro `<accent><gender>_<name>` voice codes (``af_sky``, ``am_adam``,
    ``bf_emma``…) + Kitten `-f`/`-m` suffixes (``expr-voice-2-f``).

    The Kokoro gender code is a single letter (``f``/``m``) following a
    one-letter accent code (a/b/c/e/…). We match it precisely — anchored as
    the whole 2-char token or followed by ``_`` — so bare Piper names that
    merely *start* with those letters (``amy``, ``alba``, ``carlfm``) don't
    get a spurious "declared" gender.
    """
    n = speaker_name.lower()
    if re.match(r'^[a-z]f(_|$)', n) or n.endswith(("-f", "_f")):
        return "F"
    if re.match(r'^[a-z]m(_|$)', n) or n.endswith(("-m", "_m")):
        return "M"
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


# High-precision name → gender sets for the heuristic layer. These are the
# voices whose gender we know from the upstream model cards but that aren't
# pinned by an explicit naming convention. Kept separate from
# PIPER_VOICE_GENDERS (which is the curated "inferred" table) so the heuristic
# can also catch VITS/Coqui/MMS slugs that reuse these names.
_HEURISTIC_FEMALE = {
    "amy", "lessac", "cori", "jenny", "jenny_dioco", "kristin", "kathleen",
    "alba", "aru", "southern_english_female", "hfc_female", "ljspeech",
    "eva_k", "kerstin", "ramona", "siwis", "paola", "gosia", "irina",
    "lada", "huayan", "rapunzelina", "haaniye", "anna", "berta", "salka",
    "ugla", "natia", "iseke", "raya", "marylux", "nathalie", "lili",
    "lisa", "glados",
}
_HEURISTIC_MALE = {
    "ryan", "joe", "alan", "thorsten", "danny", "kusal",
    "northern_english_male", "hfc_male", "bryce", "john", "norman",
    "davefx", "carlfm", "karlsson", "pavoque", "gilles", "tom",
    "riccardo", "darkman", "faber", "edresson", "denis", "dmitri",
    "ruslan", "vais1000", "kareem", "jirka", "amir", "ganji", "gyro",
    "harri", "imre", "bui", "steinn", "issai", "chitwanian", "rdh",
    "mihai", "artur", "dfki", "fahrettin", "fettah", "claude",
}
# Anonymous multi-speaker rosters — always "unknown" regardless of any
# substring match (e.g. an `mls` corpus voice should not inherit a gender).
_ANON_ROSTER_TOKENS = ("libritts", "vctk", "mls")


def gender_heuristic(name: str | None) -> str | None:
    """Pattern + high-precision name heuristic. Sits between the curated Piper
    table and the "unknown" fallback. Returns ``"F"``/``"M"`` or None.

    Anonymous multi-speaker roster ids (libritts/vctk/mls) deliberately fall
    through to None so we don't guess a gender for an unlabelled speaker."""
    if not name:
        return None
    n = name.lower()
    if any(tok in n for tok in _ANON_ROSTER_TOKENS):
        return None
    # Explicit suffix/substring patterns.
    if n.endswith(("_female", "-female", "-f", "_f")) or "hfc_female" in n:
        return "F"
    if n.endswith(("_male", "-male", "-m", "_m")) or "hfc_male" in n:
        return "M"
    if n in _HEURISTIC_FEMALE:
        return "F"
    if n in _HEURISTIC_MALE:
        return "M"
    return None


def infer_gender(voice_id: str, speaker_name: str, family: str) -> tuple[str, str]:
    """Return (gender, confidence) with precedence:
      declared  → upstream metadata explicitly carries gender (Kokoro/Kitten
                  naming patterns).
      inferred  → mapped a voice name through the curated Piper table.
      heuristic → matched a pattern / high-precision name set.
      unknown   → none of the above.
    The UI uses confidence to surface a "Has gender data" filter that excludes
    the unknowns so users can find labelled voices.
    """
    # Kokoro/Kitten declare gender via name prefix/suffix.
    pattern_gender = gender_for(speaker_name)
    if pattern_gender != "U":
        return pattern_gender, "declared"
    # Piper single-speaker voices — lookup the curated canonical-name table.
    if family in ("piper", "vits"):
        mapped = PIPER_VOICE_GENDERS.get(speaker_name.lower())
        if mapped is not None and mapped != "U":
            return mapped, "inferred"
    # Heuristic layer — pattern suffixes + high-precision name sets.
    heur = gender_heuristic(speaker_name)
    if heur is not None:
        return heur, "heuristic"
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


def _bundle_url_ok(url: str) -> bool:
    """Defensive HEAD-check for a release bundle. Returns False on any error
    or non-success status so a bad/withdrawn asset never breaks the refresh."""
    try:
        r = requests.head(url, allow_redirects=True, timeout=30)
        # GitHub release downloads 302 to a signed S3 URL; allow_redirects
        # resolves that, so a 200 here means the bytes are reachable.
        return r.status_code == 200
    except Exception:
        return False


def _fetch_release_assets() -> list[dict]:
    """Enumerate the FULL `tts-models` release via the paginated assets
    endpoint. The embedded asset list on the `/releases/tags/` object is
    capped by GitHub and silently truncates large releases (it drops at least
    one bundle today), so we resolve the release id then page through
    `/releases/<id>/assets`. FAIL-SOFT: returns [] on any error."""
    headers = {"Accept": "application/vnd.github+json",
               "User-Agent": "HayaiTTS-catalog-builder/1"}
    tok = os.environ.get("GITHUB_TOKEN")
    if tok:
        headers["Authorization"] = f"Bearer {tok}"
    try:
        r = requests.get(
            "https://api.github.com/repos/k2-fsa/sherpa-onnx/releases/tags/tts-models",
            timeout=30, headers=headers,
        )
        r.raise_for_status()
        release = r.json()
    except Exception as e:
        err(f"Release-asset fallback failed (tag lookup): {e}")
        return []
    release_id = release.get("id")
    if not release_id:
        # No id to paginate with — fall back to the embedded (capped) list.
        return release.get("assets", []) or []
    assets: list[dict] = []
    for page in range(1, 30):  # hard ceiling; 7 pages today
        try:
            r = requests.get(
                f"https://api.github.com/repos/k2-fsa/sherpa-onnx/releases/{release_id}/assets",
                params={"per_page": 100, "page": page},
                timeout=30, headers=headers,
            )
            r.raise_for_status()
            batch = r.json()
        except Exception as e:
            err(f"Release-asset pagination stopped at page {page}: {e}")
            break
        if not isinstance(batch, list):
            # A rate-limit/error body can sneak through as a 200 dict; never
            # let it reach discover_release_only_bundles (which calls .get()
            # per asset) and crash the weekly refresh.
            err(f"Release-asset page {page} returned non-list "
                f"({type(batch).__name__}); stopping pagination")
            break
        if not batch:
            break
        assets.extend(batch)
        if len(batch) < 100:
            break
    if not assets:
        # Pagination yielded nothing (e.g. token-scoped 403) — use the
        # embedded list rather than dropping the entire fallback.
        return release.get("assets", []) or []
    return assets


def discover_release_only_bundles(known_ids: set[str]) -> list[Voice]:
    """Pick up family bundles that exist on the upstream `tts-models` release
    but aren't documented on the mdBook index.

    The doc-page scraper misses any model k2-fsa hasn't written a subpage
    for yet — historically the ZipVoice + Pocket cloning catalog, and any
    Meta MMS / Coqui / MeloTTS bundle the index hasn't caught up with.
    Querying the full (paginated) release-asset list directly is the
    gap-filler. Anything not already covered by [known_ids] gets a minimal
    Voice record with the data we can pull from the bundle filename alone;
    speaker counts come from the JNI runtime once the user installs.

    DEFENSIVE: each new bundle URL is HEAD-checked; a bundle that 404s or
    errors is SKIPPED and logged — a bad asset never breaks the refresh.
    """
    assets = _fetch_release_assets()
    if not assets:
        return []

    # First pass: build candidate Voice records for every release-only
    # bundle that classifies to a known family and isn't already ingested.
    # No network here — the HEAD-check happens in a concurrent second pass.
    candidates: list[Voice] = []
    seen: set[str] = set()
    for asset in assets:
        name = asset.get("name", "")
        if not name.endswith(".tar.bz2"):
            continue
        # Some assets are SDK-version wrappers (`sherpa-onnx-wasm-simd-1.12.x-…`)
        # that bundle the *same* model inside a per-build directory. Strip
        # the leading `sherpa-onnx-wasm-…-` prefix so the canonical model
        # id is what we key on.
        voice_id = name.removesuffix(".tar.bz2")
        wasm_strip = re.match(
            r"sherpa-onnx-(?:wasm-simd|wasm-tts)-\d+\.\d+\.\d+-(.+)$",
            voice_id,
        )
        if wasm_strip:
            voice_id = wasm_strip.group(1)
        # `voice_id` now is the canonical slug. Bail if we have already
        # ingested it (doc scraper or earlier release pass).
        if voice_id in known_ids or voice_id in seen:
            continue
        family = family_for_slug(voice_id)
        if family == "unknown":
            continue
        seen.add(voice_id)

        bundle_url = asset.get("browser_download_url", "")
        if not bundle_url:
            continue
        # Default to a single anonymous "Voice 1" — the actual speaker
        # roster comes from `OfflineTts.numSpeakers()` once the user
        # installs. Speaker labels in-app already collapse anonymous
        # `speaker_N` placeholders to "Voice N+1".
        speakers = [{
            "id": 0, "name": "speaker_0", "gender": "U",
            "genderConfidence": "unknown",
        }]
        langs = derive_languages_from_slug(voice_id, family)
        candidates.append(Voice(
            id=voice_id,
            family=family,
            title=derive_title(speakers, family),
            languages=langs,
            speakers=speakers,
            sampleRateHz=24000,
            approxSizeMb=0,
            tier=derive_tier(voice_id, family),
            license=FAMILY_DEFAULT_LICENSE.get(family, "Apache-2.0"),
            bundleUrl=bundle_url,
            available=family in RUNTIME_SUPPORTED_FAMILIES,
            demoUrl=demo_url_for(family, voice_id),
            sampleAudioUrl=None,
        ))

    # Second pass: DEFENSIVE concurrent HEAD-check. A bundle that 404s or
    # errors is SKIPPED and logged — a bad asset never breaks the refresh.
    # (The doc-scraper path implicitly verified its bundles by fetching the
    # subpage; release-only bundles have no such gate.) Concurrency keeps
    # this tractable when many candidates are new.
    extras: list[Voice] = []
    skipped_unreachable: list[str] = []
    if candidates:
        with ThreadPoolExecutor(max_workers=8) as ex:
            checks = {ex.submit(_bundle_url_ok, c.bundleUrl): c
                      for c in candidates}
            for fut in as_completed(checks):
                c = checks[fut]
                ok = False
                try:
                    ok = fut.result()
                except Exception:
                    ok = False
                if ok:
                    extras.append(c)
                else:
                    skipped_unreachable.append(c.id)
                    err(f"  release bundle unreachable, skipping: {c.id}")
        extras.sort(key=lambda v: v.id)
    if extras:
        by_fam: dict[str, int] = {}
        for v in extras:
            by_fam[v.family] = by_fam.get(v.family, 0) + 1
        summary = ", ".join(f"{c}× {f}" for f, c in sorted(by_fam.items()))
        log(f"Release-asset fallback added {len(extras)} bundle(s) ({summary}).")
    if skipped_unreachable:
        log(f"Release-asset fallback skipped {len(skipped_unreachable)} "
            f"unreachable bundle(s): {', '.join(skipped_unreachable[:10])}"
            + (" …" if len(skipped_unreachable) > 10 else ""))
    return extras


def derive_languages_from_slug(voice_id: str, family: str) -> list[str]:
    """Best-effort language tag extraction from a cloning-bundle filename.

    The slugs aren't structured — `sherpa-onnx-zipvoice-distill-int8-zh-en-emilia`
    embeds two BCP-47 codes (`zh`, `en`) followed by a training-corpus
    name. Strip the family + qualifier tokens and keep anything that
    looks like a 2-letter language code.
    """
    # Meta MMS bundles encode their language as an ISO-639-3 code.
    mms = mms_language(voice_id)
    if mms:
        return mms
    lower = voice_id.lower()
    # Tokens that come before the language list and should be ignored.
    junk = {
        "sherpa", "onnx", "zipvoice", "pocket", "tts", "distill",
        "int8", "fp16", "fp32", "emilia", "v1", "v2", "v3",
    }
    parts = re.split(r"[-_]", lower)
    languages: list[str] = []
    for p in parts:
        # Strip any trailing date / version digits ("2026-01-26" → skip)
        if p.isdigit():
            continue
        if p in junk:
            continue
        if len(p) == 2 and p.isalpha():
            languages.append(p)
    # Pocket TTS is currently English-only per the upstream README.
    if family == "pocket" and not languages:
        languages = ["en"]
    if not languages:
        languages = ["en"]
    return languages


def _enrich_one(v: Voice, *, cache_dir: str | None,
                piper_index: dict, hf_cache: dict, gh_cache: dict,
                hf_lock: threading.Lock, gh_lock: threading.Lock) -> None:
    """Enrich a single Voice in place. FAIL-SOFT: any source returning {}/None
    just leaves the corresponding field untouched."""
    fam = v.family
    try:
        if fam in ("piper", "vits"):
            _enrich_piper(v, cache_dir=cache_dir, piper_index=piper_index)
        elif fam in sources.FAMILY_HF_REPO:
            _enrich_hf_family(v, cache_dir=cache_dir, hf_cache=hf_cache,
                              hf_lock=hf_lock)
        elif fam in sources.FAMILY_GH_REPO:
            _enrich_gh_family(v, cache_dir=cache_dir, gh_cache=gh_cache,
                              gh_lock=gh_lock)
        # License safety net (Stage 4): never emit "unknown" for a known
        # family — fall back to the family default.
        if v.license in (None, "", "unknown"):
            v.license = FAMILY_DEFAULT_LICENSE.get(fam, v.license or "unknown")
    except Exception as e:
        err(f"  enrich failed for {v.id}: {e}")


def _enrich_piper(v: Voice, *, cache_dir: str | None, piper_index: dict) -> None:
    """Piper/VITS-piper: authoritative speaker name from the index, dataset +
    sourceUrl from the MODEL_CARD, dataset-license tag when NC."""
    key = sources.piper_key_for(v.id)
    if not key:
        return
    entry = piper_index.get(key)
    if not entry:
        return

    # Authoritative single-speaker name from the index, then re-infer gender.
    try:
        num = entry.get("num_speakers")
        idx_name = entry.get("name")
        if num == 1 and idx_name and len(v.speakers) == 1:
            v.speakers[0]["name"] = idx_name
            g, conf = infer_gender(v.id, idx_name, v.family)
            v.speakers[0]["gender"] = g
            v.speakers[0]["genderConfidence"] = conf
            v.title = derive_title(v.speakers, v.family)
    except Exception:
        pass

    # Quality from the structured index.
    q = entry.get("quality")
    if isinstance(q, str) and q and not v.quality:
        v.quality = q

    # Dataset / source URL / dataset-license from the MODEL_CARD.
    card = sources.piper_model_card(entry, cache_dir=cache_dir)
    if card.get("dataset") and not v.dataset:
        v.dataset = card["dataset"]
    if card.get("datasetUrl") and not v.sourceUrl:
        v.sourceUrl = card["datasetUrl"]
    ds_lic = card.get("datasetLicense")
    if ds_lic:
        v._dataset_license = ds_lic
        # Stage 4: weights license stays MIT, but flag a non-commercial
        # training corpus so the UI / users can see the restriction.
        if "NC" in ds_lic.upper().replace(" ", ""):
            spdx = sources.normalize_license(ds_lic)
            tag = f"dataset-license:{spdx}"
            if tag not in v.tags:
                v.tags.append(tag)


def _enrich_hf_family(v: Voice, *, cache_dir: str | None,
                      hf_cache: dict, hf_lock: threading.Lock) -> None:
    """Kokoro/Kitten/Supertonic: one HF API call per family (memoized).
    Sets license (SPDX over family default); fills baseModel/author if empty."""
    repo = sources.FAMILY_HF_REPO.get(v.family)
    if not repo:
        return
    with hf_lock:
        if repo not in hf_cache:
            hf_cache[repo] = sources.hf_model_meta(repo, cache_dir=cache_dir)
        meta = hf_cache[repo]
    if not meta:
        return
    lic = sources.normalize_license(meta.get("license"))
    if lic:
        v.license = lic
    if meta.get("baseModel") and not v.baseModel:
        v.baseModel = meta["baseModel"]
    if meta.get("author") and not v.author:
        v.author = meta["author"]


def _enrich_gh_family(v: Voice, *, cache_dir: str | None,
                      gh_cache: dict, gh_lock: threading.Lock) -> None:
    """Matcha/ZipVoice/Pocket: GitHub license endpoint → license when it
    yields a clean SPDX. One call per repo (memoized)."""
    repo = sources.FAMILY_GH_REPO.get(v.family)
    if not repo:
        return
    with gh_lock:
        if repo not in gh_cache:
            gh_cache[repo] = sources.github_license(repo, cache_dir=cache_dir)
        spdx = gh_cache[repo]
    if spdx:
        lic = sources.normalize_license(spdx)
        if lic:
            v.license = lic


def enrich_voices(voices: list[Voice], *, cache_dir: str | None,
                  no_enrich: bool, workers: int) -> None:
    """Network-enrichment pass. Runs AFTER dedupe/release-merge and BEFORE the
    hash pass. Mutates ``voices`` in place. FAIL-SOFT throughout — a dead
    network leaves every field exactly as the scraper produced it."""
    if no_enrich:
        log("Enrichment skipped (--no-enrich).")
        return
    piper_index = sources.load_piper_index(cache_dir)
    log(f"Enriching {len(voices)} voices "
        f"(piper index: {len(piper_index)} entries)…")
    hf_cache: dict = {}
    gh_cache: dict = {}
    hf_lock = threading.Lock()
    gh_lock = threading.Lock()
    pool = min(workers, 4)
    with ThreadPoolExecutor(max_workers=pool) as ex:
        futures = [
            ex.submit(_enrich_one, v, cache_dir=cache_dir,
                      piper_index=piper_index, hf_cache=hf_cache,
                      gh_cache=gh_cache, hf_lock=hf_lock, gh_lock=gh_lock)
            for v in voices
        ]
        for f in as_completed(futures):
            # _enrich_one catches Exception internally; this guard covers the
            # residual (a future that died before its try-block) so enrichment
            # can never redden the weekly refresh.
            try:
                f.result()
            except Exception as e:
                err(f"  enrich future raised unexpectedly: {e}")
    log("Enrichment pass complete.")


def build_voices(pages: list[tuple[str, str]], *, limit: int | None,
                 cache_dir: str | None, skip_hash: bool, no_enrich: bool,
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

    # Release-asset fallback. Picks up bundles k2-fsa publishes but
    # hasn't documented on the mdBook index yet — currently the entire
    # ZipVoice + Pocket cloning catalog, plus any future family they
    # release before writing a subpage for it.
    for extra in discover_release_only_bundles(known_ids=set(by_id.keys())):
        by_id[extra.id] = extra
    log(f"Total unique voices (incl. release-only): {len(by_id)}")

    ordered = sorted(by_id.values(), key=lambda x: x.id)

    if cache_dir:
        os.makedirs(cache_dir, exist_ok=True)

    # Enrichment pass — authoritative names, dataset/source URLs, SPDX
    # licenses. Runs after dedupe/release-merge, before the hash pass.
    enrich_voices(ordered, cache_dir=cache_dir, no_enrich=no_enrich,
                  workers=workers)

    # Family-count summary so catalog size growth is visible at a glance.
    by_fam_total: dict[str, int] = {}
    for v in ordered:
        by_fam_total[v.family] = by_fam_total.get(v.family, 0) + 1
    summary = ", ".join(f"{c}× {f}" for f, c in sorted(by_fam_total.items()))
    log(f"Catalog composition by family ({len(ordered)} total): {summary}")

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
    alphabetically, 2-space indent, ``\\n`` line endings, trailing newline.

    Preserves ``sampleAudioUrl`` and ``speakerSamples`` fields from any
    pre-existing catalog at ``output_path``. The catalog-refresh workflow
    runs **before** the sample-render workflow, so without this preservation
    every refresh would wipe out the rendered-sample URLs that
    render_samples.py wrote on the previous Monday.
    """
    # Everything in this list gets carried forward from the previous
    # catalog if the scraper didn't produce a fresh value. These are the
    # fields populated by downstream pipelines (render-samples bundle
    # probe + curated overlay) that the scraper has no way to recompute.
    PRESERVE_KEYS = (
        "sampleAudioUrl", "speakerSamples",
        # Bundle-probe enrichment
        "description", "quality", "dataset", "phonemeType", "vocabSize",
        "author", "sourceUrl", "baseModel", "renderRtf",
        "renderDurationMs", "defaultLengthScale", "defaultNoiseScale",
        "defaultNoiseScaleW", "bundleStructure",
        # Overlay-only fields
        "tags", "recommendedUseCases",
        # If render-samples upgraded the speakers list with named
        # speakers + inferred gender, keep that — the scrape would
        # otherwise reset us back to anonymous placeholders for the
        # release-only voices.
        "speakers",
    )
    preserved: dict[str, dict] = {}
    if os.path.exists(output_path):
        try:
            with open(output_path, "r", encoding="utf-8") as fh:
                existing = json.load(fh)
            for v in existing.get("voices", []):
                vid = v.get("id")
                if not vid:
                    continue
                keep: dict = {}
                for k in PRESERVE_KEYS:
                    val = v.get(k)
                    if val not in (None, "", [], {}):
                        keep[k] = val
                if keep:
                    preserved[vid] = keep
        except Exception:
            # Bad/missing existing catalog → nothing to preserve, refresh proceeds.
            preserved = {}

    rows: list[dict] = []
    for v in sorted(voices, key=lambda v: v.id):
        row = v.to_json()
        carried = preserved.get(v.id) or {}
        # `speakers` needs special handling: the carried list may be
        # richer (render-samples added named speakers + inferred
        # gender) but the scraper's fresh list may also be authoritative
        # for doc-scraped voices the upstream just expanded. Keep the
        # one with more named speakers, falling back to the fresh
        # version when they tie.
        if carried.get("speakers") and row.get("speakers"):
            new_named = sum(
                1 for s in row["speakers"]
                if s.get("name") and not s["name"].startswith("speaker_")
            )
            old_named = sum(
                1 for s in carried["speakers"]
                if s.get("name") and not s["name"].startswith("speaker_")
            )
            if old_named > new_named:
                row["speakers"] = carried["speakers"]
            del carried["speakers"]
        row.update(carried)
        rows.append(row)

    payload = {"version": 1, "voices": rows}
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
    ap.add_argument("--no-enrich", action="store_true",
                    help="Skip ALL network enrichment (dev only — faster runs)")
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
                          skip_hash=args.skip_hash, no_enrich=args.no_enrich,
                          workers=args.workers, stats=stats)
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
