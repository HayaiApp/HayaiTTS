#!/usr/bin/env python3
"""Cached, throttled, FAIL-SOFT network helpers for catalog enrichment.

Every public function in this module returns ``{}``/``None`` (never raises)
on ANY error — a network hiccup, a 404, a malformed payload, a missing
token, all of it. The weekly catalog refresh must never go red because a
metadata source was briefly unreachable; enrichment is strictly additive.

The on-disk cache mirrors the ``--cache-dir`` convention that
``build_catalog.py`` already uses for ``<id>.sha256`` files, adding a
``meta/`` subdirectory with a 7-day TTL and serve-stale-on-failure: if the
network is down but a stale copy exists, we hand back the stale copy rather
than dropping the field.
"""
from __future__ import annotations

import hashlib
import json
import os
import re
import sys
import threading
import time

import requests

USER_AGENT = "HayaiTTS-catalog-builder/1 (+https://github.com/HayaiApp/HayaiTTS)"

# ~6 requests/second across all threads. HF + the GitHub API both tolerate
# this comfortably; the throttle exists mostly to be a polite citizen and to
# stay well under any anonymous rate limit.
_MIN_INTERVAL = 1.0 / 6.0
_throttle_lock = threading.Lock()
_last_request_at = 0.0


# --------------------------------------------------------------------------
# Family → upstream repository maps (Stage 1 spec).
# --------------------------------------------------------------------------
FAMILY_HF_REPO = {
    "kokoro": "hexgrad/Kokoro-82M",
    "kitten": "KittenML/kitten-tts-nano-0.2",
    "supertonic": "Supertone/supertonic",
}

FAMILY_GH_REPO = {
    "matcha": "shivammehta25/Matcha-TTS",
    "zipvoice": "k2-fsa/ZipVoice",
    # Pocket TTS runs out of the k2-fsa sherpa-onnx release; its license is
    # the sherpa-onnx repo license. (csukuangfj/pocket-tts is the upstream
    # model home but the SPDX we want for the bundle is the runtime repo's.)
    "pocket": "k2-fsa/sherpa-onnx",
}


# --------------------------------------------------------------------------
# Logging — keep it on stderr so it never pollutes the JSON on stdout.
# --------------------------------------------------------------------------
def _log(msg: str) -> None:
    try:
        sys.stderr.buffer.write((msg + "\n").encode("utf-8", errors="replace"))
        sys.stderr.flush()
    except Exception:
        pass


def _throttle() -> None:
    """Block until at least ``_MIN_INTERVAL`` has elapsed since the last
    request issued by ANY thread. Cheap module-wide rate limiter."""
    global _last_request_at
    with _throttle_lock:
        now = time.monotonic()
        wait = _MIN_INTERVAL - (now - _last_request_at)
        if wait > 0:
            time.sleep(wait)
        _last_request_at = time.monotonic()


def _auth_headers(url: str) -> dict[str, str]:
    """Attach an optional token if the env carries one. Tokens are NEVER
    required — anonymous access is the supported path; tokens only raise the
    rate limit / reach gated repos when a maintainer provides one."""
    headers = {"User-Agent": USER_AGENT}
    host = url.split("/", 3)[2] if "://" in url else ""
    if "huggingface.co" in host:
        tok = os.environ.get("HF_TOKEN")
        if tok:
            headers["Authorization"] = f"Bearer {tok}"
    elif "github.com" in host:
        tok = os.environ.get("GITHUB_TOKEN")
        if tok:
            headers["Authorization"] = f"Bearer {tok}"
    return headers


def _meta_dir(cache_dir: str | None) -> str | None:
    if not cache_dir:
        return None
    d = os.path.join(cache_dir, "meta")
    try:
        os.makedirs(d, exist_ok=True)
    except Exception:
        return None
    return d


def _cache_path(cache_dir: str | None, cache_key: str) -> str | None:
    d = _meta_dir(cache_dir)
    if not d:
        return None
    # Hash the key so arbitrary URLs / ids never produce illegal filenames.
    safe = hashlib.sha256(cache_key.encode("utf-8")).hexdigest()[:32]
    return os.path.join(d, safe + ".txt")


def _read_cache(path: str | None) -> tuple[str | None, float | None]:
    """Return (text, age_seconds) for a cache file, or (None, None)."""
    if not path or not os.path.isfile(path):
        return None, None
    try:
        with open(path, "r", encoding="utf-8") as fh:
            text = fh.read()
        age = time.time() - os.path.getmtime(path)
        return text, age
    except Exception:
        return None, None


def _write_cache(path: str | None, text: str) -> None:
    if not path:
        return
    try:
        tmp = path + ".tmp"
        with open(tmp, "w", encoding="utf-8") as fh:
            fh.write(text)
        os.replace(tmp, path)
    except Exception:
        pass


def cached_get_text(
    url: str,
    *,
    cache_dir: str | None,
    cache_key: str,
    ttl_days: float = 7.0,
    timeout: int = 30,
) -> str | None:
    """GET ``url`` as text with a disk cache.

    Fresh cache (age < ttl) is returned without any network call. On a stale
    or absent cache we fetch; on a fetch failure we serve the stale copy if
    one exists, otherwise return None. Never raises.
    """
    path = _cache_path(cache_dir, cache_key)
    cached, age = _read_cache(path)
    ttl_seconds = ttl_days * 86400.0
    if cached is not None and age is not None and age < ttl_seconds:
        return cached
    try:
        _throttle()
        r = requests.get(url, headers=_auth_headers(url), timeout=timeout)
        r.raise_for_status()
        text = r.text
        _write_cache(path, text)
        return text
    except Exception as e:
        if cached is not None:
            _log(f"  [meta] {url} failed ({e}); serving stale cache")
            return cached
        _log(f"  [meta] {url} failed ({e}); no cache, returning None")
        return None


def cached_get_json(url: str, **kw):  # -> dict | list | None
    """``cached_get_text`` + ``json.loads``. Returns the parsed object or
    None on any fetch/parse failure."""
    text = cached_get_text(url, **kw)
    if text is None:
        return None
    try:
        return json.loads(text)
    except Exception as e:
        _log(f"  [meta] JSON parse failed for {url}: {e}")
        return None


# --------------------------------------------------------------------------
# Piper voices index (rhasspy/piper-voices/voices.json).
# --------------------------------------------------------------------------
_PIPER_INDEX_CACHE: dict[int, dict] = {}
_PIPER_INDEX_LOCK = threading.Lock()


def load_piper_index(cache_dir: str | None) -> dict[str, dict]:
    """One GET of the rhasspy piper voices index, memoized per process.

    Returns a dict keyed by piper id (``en_US-amy-medium``) whose values
    carry ``name``, structured ``language``, ``quality``, ``num_speakers``,
    ``speaker_id_map``, ``files`` and ``aliases``. Returns ``{}`` on failure.
    """
    key = id(cache_dir) if cache_dir is None else hash(cache_dir)
    with _PIPER_INDEX_LOCK:
        if key in _PIPER_INDEX_CACHE:
            return _PIPER_INDEX_CACHE[key]
        data = cached_get_json(
            "https://huggingface.co/rhasspy/piper-voices/raw/main/voices.json",
            cache_dir=cache_dir,
            cache_key="piper-voices-index",
            ttl_days=7.0,
            timeout=60,
        )
        if not isinstance(data, dict):
            data = {}
        _PIPER_INDEX_CACHE[key] = data
        return data


def piper_key_for(voice_id: str) -> str | None:
    """Recover the piper-voices key (``en_US-amy-medium``) from a sherpa-onnx
    voice id (``vits-piper-en_US-amy-medium`` or with an ``-fp16``/``-int8``/
    ``-fp32`` precision suffix). Returns None when the id isn't a piper id."""
    if not voice_id.startswith("vits-piper-"):
        return None
    key = voice_id[len("vits-piper-"):]
    key = re.sub(r"-(?:fp16|fp32|int8)$", "", key)
    return key or None


_DATASET_BLOCK_RE = re.compile(
    r"##\s*Dataset\s*(.*?)(?:\n##\s|\Z)", re.S | re.I
)
_DATASET_URL_RE = re.compile(r"URL:\s*(\S+)", re.I)
_DATASET_LICENSE_RE = re.compile(r"License:\s*([^\n]+)", re.I)


def piper_model_card(entry: dict, *, cache_dir: str | None) -> dict:
    """Fetch the MODEL_CARD referenced by ``entry['files']`` and regex its
    ``## Dataset`` block. Returns ``{dataset, datasetUrl, datasetLicense}``
    (any subset present), or ``{}`` on failure."""
    try:
        files = entry.get("files") or {}
        card_path = next(
            (p for p in files if p.rsplit("/", 1)[-1] == "MODEL_CARD"),
            None,
        )
        if not card_path:
            return {}
        url = "https://huggingface.co/rhasspy/piper-voices/raw/main/" + card_path
        text = cached_get_text(
            url, cache_dir=cache_dir, cache_key="piper-card:" + card_path,
            ttl_days=7.0,
        )
        if not text:
            return {}
        block_m = _DATASET_BLOCK_RE.search(text)
        block = block_m.group(1) if block_m else text
        out: dict = {}
        url_m = _DATASET_URL_RE.search(block)
        if url_m:
            raw_url = url_m.group(1).strip().rstrip(".,)")
            if raw_url.lower().startswith("http"):
                out["datasetUrl"] = raw_url
                # Derive a friendly dataset name from the URL host/path.
                name = _dataset_name_from_url(raw_url)
                if name:
                    out["dataset"] = name
        lic_m = _DATASET_LICENSE_RE.search(block)
        if lic_m:
            lic = lic_m.group(1).strip()
            if lic and lic.lower() not in ("see url", "see above"):
                out["datasetLicense"] = lic
        return out
    except Exception as e:
        _log(f"  [meta] piper_model_card failed: {e}")
        return {}


def _dataset_name_from_url(url: str) -> str | None:
    """Best-effort corpus name from a dataset URL. Conservative — returns
    None when we can't make a clean guess (the field is optional)."""
    u = url.lower()
    known = {
        "ljspeech": "LJSpeech", "libritts_r": "LibriTTS-R", "libritts": "LibriTTS",
        "vctk": "VCTK", "openslr": "OpenSLR", "mimic3": "Mimic3",
        "mls": "MLS", "thorsten": "Thorsten-Voice", "css10": "CSS10",
        "commonvoice": "Common Voice", "common_voice": "Common Voice",
    }
    for needle, label in known.items():
        if needle in u:
            return label
    return None


# --------------------------------------------------------------------------
# HuggingFace model metadata.
# --------------------------------------------------------------------------
_NOISE_TAG_PREFIXES = ("license:", "region:", "arxiv:", "doi:", "base_model:")


def hf_model_meta(repo: str, *, cache_dir: str | None) -> dict:
    """GET ``https://huggingface.co/api/models/<repo>`` and distil the bits we
    care about. Returns ``{license, baseModel, author, hfTags}`` (only the
    keys we could populate), or ``{}`` on failure."""
    if not repo:
        return {}
    try:
        data = cached_get_json(
            "https://huggingface.co/api/models/" + repo,
            cache_dir=cache_dir,
            cache_key="hf-model:" + repo,
            ttl_days=7.0,
        )
        if not isinstance(data, dict):
            return {}
        out: dict = {}
        card = data.get("cardData") or {}
        lic = card.get("license")
        if isinstance(lic, str) and lic:
            out["license"] = lic
        base = card.get("base_model")
        if isinstance(base, list) and base:
            out["baseModel"] = base[0]
        elif isinstance(base, str) and base:
            out["baseModel"] = base
        author = data.get("author")
        if isinstance(author, str) and author:
            out["author"] = author
        raw_tags = data.get("tags") or []
        clean = [
            t for t in raw_tags
            if isinstance(t, str)
            and not any(t.startswith(p) for p in _NOISE_TAG_PREFIXES)
        ]
        if clean:
            out["hfTags"] = clean
        return out
    except Exception as e:
        _log(f"  [meta] hf_model_meta failed for {repo}: {e}")
        return {}


# --------------------------------------------------------------------------
# GitHub license endpoint.
# --------------------------------------------------------------------------
def github_license(repo: str, *, cache_dir: str | None) -> str | None:
    """GET ``https://api.github.com/repos/<repo>/license`` → ``spdx_id``.
    Returns None on failure, a 404, or a ``NOASSERTION`` spdx (no usable
    license declared)."""
    if not repo:
        return None
    try:
        data = cached_get_json(
            "https://api.github.com/repos/" + repo + "/license",
            cache_dir=cache_dir,
            cache_key="gh-license:" + repo,
            ttl_days=7.0,
        )
        if not isinstance(data, dict):
            return None
        lic = data.get("license") or {}
        spdx = lic.get("spdx_id")
        if isinstance(spdx, str) and spdx and spdx != "NOASSERTION":
            return spdx
        return None
    except Exception as e:
        _log(f"  [meta] github_license failed for {repo}: {e}")
        return None


# --------------------------------------------------------------------------
# SPDX canonicalisation.
# --------------------------------------------------------------------------
# Lowercased input → canonical SPDX identifier. Covers the licenses that
# actually show up across the families we enrich.
_SPDX_CANON = {
    "mit": "MIT",
    "apache-2.0": "Apache-2.0",
    "apache2.0": "Apache-2.0",
    "apache 2.0": "Apache-2.0",
    "apache license 2.0": "Apache-2.0",
    "bsd-2-clause": "BSD-2-Clause",
    "bsd-3-clause": "BSD-3-Clause",
    "gpl-3.0": "GPL-3.0",
    "gpl-3.0-only": "GPL-3.0-only",
    "lgpl-3.0": "LGPL-3.0",
    "mpl-2.0": "MPL-2.0",
    "unlicense": "Unlicense",
    "cc0-1.0": "CC0-1.0",
    "cc0": "CC0-1.0",
    "cc-by-4.0": "CC-BY-4.0",
    "cc by 4.0": "CC-BY-4.0",
    "cc-by-sa-4.0": "CC-BY-SA-4.0",
    "cc by sa 4.0": "CC-BY-SA-4.0",
    "cc-by-nc-4.0": "CC-BY-NC-4.0",
    "cc by nc 4.0": "CC-BY-NC-4.0",
    "cc-by-nc-sa-4.0": "CC-BY-NC-SA-4.0",
    "cc-by-nc-nd-4.0": "CC-BY-NC-ND-4.0",
    "cc-by-3.0": "CC-BY-3.0",
}


# Creative Commons license URLs → SPDX. Piper MODEL_CARDs frequently write
# the dataset license as a creativecommons.org link rather than a code.
_CC_URL_RE = re.compile(
    r"creativecommons\.org/licenses/([a-z\-]+)/(\d\.\d)", re.I
)


def _cc_url_to_spdx(url: str) -> str | None:
    m = _CC_URL_RE.search(url)
    if not m:
        return None
    parts = m.group(1).lower()  # e.g. "by-nc-sa"
    ver = m.group(2)
    return "CC-" + parts.upper() + "-" + ver


def normalize_license(raw: str | None) -> str:
    """Map an arbitrary license string to a canonical SPDX id where we can.

    Handles SPDX-ish names, common spacing variants, and creativecommons.org
    URLs. Falls back to returning the trimmed input unchanged (so an unknown
    but declared license is preserved rather than discarded). Returns ``""``
    for empty/None input."""
    if not raw:
        return ""
    s = str(raw).strip()
    # Creative Commons URL form.
    if "creativecommons.org/licenses/" in s.lower():
        cc = _cc_url_to_spdx(s)
        if cc:
            return cc
    key = re.sub(r"\s+", " ", s.lower())
    if key in _SPDX_CANON:
        return _SPDX_CANON[key]
    # Normalise common spacing/case for CC licenses written as
    # "CC BY-NC 4.0" etc.
    key2 = key.replace(" ", "-")
    if key2 in _SPDX_CANON:
        return _SPDX_CANON[key2]
    return s
