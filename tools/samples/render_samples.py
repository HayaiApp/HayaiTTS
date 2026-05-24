"""
Render a fixed per-locale sample sentence through every voice in
`catalog/v1/models.json` and write the result as a single MP3 per voice.

Triggered from `.github/workflows/render-samples.yml` on a GitHub-hosted
runner. The script is bandwidth-bound (a few GB of upstream model
bundles); paid runner minutes would be wasted.

Output layout:

    samples/<voice_id>.mp3       (uploaded to the `samples-rolling` release)
    catalog/v1/models.json       (patched with `sampleAudioUrl` for every
                                  voice that rendered successfully)

The script is idempotent: re-running skips voices whose MP3 already
exists on disk. Family config builders mirror
`app/src/main/.../tts/SherpaTtsRuntime.kt` exactly — adding a new family
upstream means porting the matching Kotlin builder here.

Supported families: Piper, VITS, Matcha, Kokoro, Kitten, Supertonic.
ZipVoice and Pocket are wired in `SherpaTtsRuntime.kt` but currently
have zero catalog entries; renderers are present as no-ops for the day
they appear.
"""

from __future__ import annotations

import argparse
import json
import shutil
import subprocess
import sys
import tarfile
import tempfile
import time
import wave
from pathlib import Path
from typing import Any

import requests

SAMPLES_RELEASE_TAG = "samples-rolling"
SAMPLES_DOWNLOAD_BASE = (
    f"https://github.com/HayaiApp/HayaiTTS/releases/download/{SAMPLES_RELEASE_TAG}"
)

# Per-locale sample phrase. Pangrams where they exist, short culturally
# familiar lines otherwise. Keep under ~80 chars so synthesis stays under
# ~5 s and the MP3 stays small enough for cellular streaming.
PHRASES: dict[str, str] = {
    "en": "The quick brown fox jumps over the lazy dog.",
    "es": "El veloz murciélago hindú comía feliz cardillo y kiwi.",
    "fr": "Portez ce vieux whisky au juge blond qui fume.",
    "de": "Franz jagt im komplett verwahrlosten Taxi quer durch Bayern.",
    "it": "Ma la volpe, col suo balzo, ha raggiunto il quieto Fido.",
    "pt": "À noite, vovô Kowalsky vê o ímã cair no pé do pinguim.",
    "ru": "Съешь же ещё этих мягких французских булок, да выпей чаю.",
    "ja": "いろはにほへと ちりぬるを わかよたれそ つねならむ。",
    "ko": "다람쥐 헌 쳇바퀴에 타고파.",
    "zh": "天行健,君子以自强不息。",
    "ar": "نص حكيم له سر قاطع وذو شأن عظيم مكتوب على ثوب أخضر.",
    "nl": "Pa's wijze lynx bezag vroom het fikse aquaduct.",
    "pl": "Pchnąć w tę łódź jeża lub ośm skrzyń fig.",
    "uk": "Чуєш їх, доцю, га? Кумедна ж ти, прощайте!",
    "cs": "Příliš žluťoučký kůň úpěl ďábelské ódy.",
    "sk": "Kŕdeľ ďatľov učí koňa žrať kôru.",
    "el": "Ταχίστη αλώπηξ βαφής ψημένη γη, δραστήρια.",
    "tr": "Pijamalı hasta yağız şoföre çabucak güvendi.",
    "fa": "هر سرزمینی فرهنگ خود را دارد.",
    "fi": "Albert osti fagotin ja töräytti puhkuvan melodian.",
    "hu": "Egy hűtlen vejét fülöncsípő, dühös mexikói úr Wesselényinél mázol.",
    "is": "Þú getur þetta!",
    "vi": "Bạn có thể nghe thử trước khi tải.",
    "ro": "Muzicologă în bej vând whisky și tequila, preț fix.",
    "sv": "Flygande bäckasiner söka hwila på mjuka tuvor.",
    "no": "Vår sære Zulu fra badeøya spilte jo whist og quickstep i min taxi.",
    "da": "Quizdeltagerne spiste jordbær med fløde, mens cirkusklovnen Walther spillede på xylofon.",
    "ka": "ნუთუ კეთილს არ მოგვცემს ბედი?",
    "kk": "Бұл — өмірдегі ең тамаша күн!",
    "lb": "De Kueb fléit iwwer de Bësch.",
    "ne": "तपाईंलाई आज कस्तो छ?",
    "sl": "Pet bližnjih fantov kuha žgance.",
    "sr": "Љубазни фењерџија чађавог лица хоће да ми покаже штос.",
    "sw": "Kila siku ni siku ya kujifunza kitu kipya.",
    "ca": "Jove xef, porti whisky amb quinze glaçons d'hidrogen, coi!",
    "cy": "Parciais fy jac codi baw hud llawn dŵr ger tŷ Mabon.",
}


# Mirrors SherpaTtsRuntime.kt's `*_CANDIDATES` lists. Order matters: the
# first match wins.
VITS_MODEL_CANDIDATES = ("model.onnx", "vits-vctk.onnx", "vits-vctk.int8.onnx")
MATCHA_ACOUSTIC_CANDIDATES = ("model-steps-3.onnx", "model-steps-6.onnx", "acoustic.onnx")
KOKORO_MODEL_CANDIDATES = ("model.onnx", "kokoro-multi-lang-v1_0.onnx", "kokoro-en-v0_19.onnx")
KITTEN_MODEL_CANDIDATES = ("model.onnx", "model.int8.onnx")
ZIPVOICE_ENCODER_CANDIDATES = ("encoder.int8.onnx", "encoder.onnx")
ZIPVOICE_DECODER_CANDIDATES = ("decoder.int8.onnx", "decoder.onnx")
ZIPVOICE_VOCODER_CANDIDATES = ("vocos_24khz.onnx", "vocos_22khz.onnx", "vocoder.onnx")
RULE_FST_FILES = ("date.fst", "number.fst", "phone.fst")

TOKENS_FILE = "tokens.txt"
LEXICON_FILE = "lexicon.txt"
MATCHA_VOCODER_FILE = "vocos-22khz-univ.onnx"
KOKORO_VOICES_FILE = "voices.bin"
ESPEAK_DIR = "espeak-ng-data"
DICT_DIR = "dict"


def phrase_for(languages: list[str]) -> str:
    for tag in languages:
        head = tag.split("-")[0].lower()
        if head in PHRASES:
            return PHRASES[head]
    return PHRASES["en"]


def phrase_for_lang(language: str) -> str:
    """Phrase for a single BCP47 tag (e.g. ``"en-US"`` → English pangram).
    Falls back to English when the language has no PHRASES entry."""
    head = language.split("-")[0].lower()
    return PHRASES.get(head, PHRASES["en"])


def render_languages(voice: dict[str, Any]) -> list[str]:
    """Languages we render audition clips for, per voice.

    Multi-language voices (Kokoro multi-lang, anything with more than one
    BCP47 tag in the ``languages`` list) get one clip per language. Every
    other voice gets a single clip in its primary language — the upstream
    model itself only speaks that one anyway.
    """
    langs = voice.get("languages") or []
    if not langs:
        return ["en"]
    voice_id = (voice.get("id") or "").lower()
    family = (voice.get("family") or "").lower()
    is_multilang = (
        len(langs) > 1
        or "multi-lang" in voice_id
        or (family == "kokoro" and ("v1_" in voice_id or "multi" in voice_id))
    )
    return list(langs) if is_multilang else [langs[0]]


def combo_filename(voice_id: str, sid: int, language: str) -> str:
    """Per-(speaker, language) output file name. Format kept intentionally
    URL-safe (only alnum, ``-``, ``_``, ``.``) so the GitHub release asset
    upload accepts it untouched."""
    return f"{voice_id}__sid{sid}__{language}.mp3"


def combo_url(voice_id: str, sid: int, language: str) -> str:
    return f"{SAMPLES_DOWNLOAD_BASE}/{combo_filename(voice_id, sid, language)}"


def family_url(voice_id: str) -> str:
    return f"{SAMPLES_DOWNLOAD_BASE}/{voice_id}.mp3"


def log(msg: str) -> None:
    sys.stdout.write(msg + "\n")
    sys.stdout.flush()


def download(url: str, dest: Path, *, cache_dir: Path | None = None) -> None:
    if cache_dir is not None:
        cache_file = cache_dir / dest.name
        if cache_file.exists() and cache_file.stat().st_size > 0:
            shutil.copy2(cache_file, dest)
            return
    log(f"    download {url}")
    started = time.monotonic()
    bytes_in = 0
    with requests.get(url, stream=True, timeout=180) as r:
        r.raise_for_status()
        with dest.open("wb") as f:
            for chunk in r.iter_content(chunk_size=1024 * 1024):
                if chunk:
                    f.write(chunk)
                    bytes_in += len(chunk)
    dur = time.monotonic() - started
    log(f"    fetched {bytes_in // (1024 * 1024)} MB in {dur:.1f}s")
    if cache_dir is not None:
        cache_dir.mkdir(parents=True, exist_ok=True)
        shutil.copy2(dest, cache_dir / dest.name)


def extract_tar_bz2(archive: Path, into: Path) -> Path:
    into.mkdir(parents=True, exist_ok=True)
    with tarfile.open(archive, "r:*") as tar:
        # safe extract: refuse paths that escape `into`
        root = str(into.resolve())
        for m in tar.getmembers():
            target = (into / m.name).resolve()
            if not str(target).startswith(root):
                raise RuntimeError(f"unsafe path in archive: {m.name}")
        # filter='data' silences Python 3.14's deprecation warning and is
        # the safe choice — strips device files, blocks paths outside the
        # extraction root, ignores ownership/permissions metadata.
        tar.extractall(into, filter="data")
    # Bundles usually contain a single top-level directory; some are flat.
    children = [c for c in into.iterdir() if c.is_dir()]
    return children[0] if len(children) == 1 else into


def resolve_model_file(voice_dir: Path, candidates: tuple[str, ...]) -> str:
    for name in candidates:
        c = voice_dir / name
        if c.is_file():
            return str(c)
    # Last-resort: any .onnx in the root.
    for f in voice_dir.iterdir():
        if f.is_file() and f.suffix == ".onnx":
            return str(f)
    raise RuntimeError(f"No .onnx weight in {voice_dir} (tried {candidates})")


def optional_path(p: Path) -> str:
    return str(p) if p.exists() else ""


def collect_rule_fsts(voice_dir: Path) -> str:
    present = [str(voice_dir / n) for n in RULE_FST_FILES if (voice_dir / n).is_file()]
    return ",".join(present)


# --------------------------------------------------------------------------
# Family-aware sherpa-onnx config builders. Each takes the unpacked voice
# directory and returns an OfflineTts ready to .generate(). Mirrors the
# Kotlin builders in SherpaTtsRuntime.kt one-for-one so behaviour matches
# what the app does on-device.
# --------------------------------------------------------------------------

def tts_vits(voice_dir: Path):
    import sherpa_onnx  # type: ignore
    model = resolve_model_file(voice_dir, VITS_MODEL_CANDIDATES)
    tokens = str(voice_dir / TOKENS_FILE)
    return sherpa_onnx.OfflineTts(
        sherpa_onnx.OfflineTtsConfig(
            model=sherpa_onnx.OfflineTtsModelConfig(
                vits=sherpa_onnx.OfflineTtsVitsModelConfig(
                    model=model,
                    tokens=tokens,
                    lexicon=optional_path(voice_dir / LEXICON_FILE),
                    data_dir=optional_path(voice_dir / ESPEAK_DIR),
                    dict_dir=optional_path(voice_dir / DICT_DIR),
                    length_scale=1.0,
                    noise_scale=0.667,
                    noise_scale_w=0.8,
                ),
                num_threads=2,
                debug=False,
                provider="cpu",
            ),
            rule_fsts=collect_rule_fsts(voice_dir),
            max_num_sentences=1,
        )
    )


def tts_matcha(voice_dir: Path):
    import sherpa_onnx  # type: ignore
    acoustic = resolve_model_file(voice_dir, MATCHA_ACOUSTIC_CANDIDATES)
    vocoder = voice_dir / MATCHA_VOCODER_FILE
    if not vocoder.is_file():
        raise RuntimeError(f"Matcha voice at {voice_dir} is missing {MATCHA_VOCODER_FILE}")
    tokens = str(voice_dir / TOKENS_FILE)
    return sherpa_onnx.OfflineTts(
        sherpa_onnx.OfflineTtsConfig(
            model=sherpa_onnx.OfflineTtsModelConfig(
                matcha=sherpa_onnx.OfflineTtsMatchaModelConfig(
                    acoustic_model=acoustic,
                    vocoder=str(vocoder),
                    tokens=tokens,
                    lexicon=optional_path(voice_dir / LEXICON_FILE),
                    data_dir=optional_path(voice_dir / ESPEAK_DIR),
                    dict_dir=optional_path(voice_dir / DICT_DIR),
                    length_scale=1.0,
                ),
                num_threads=2,
                debug=False,
                provider="cpu",
            ),
            rule_fsts=collect_rule_fsts(voice_dir),
            max_num_sentences=1,
        )
    )


def tts_kokoro(voice_dir: Path, lang_hint: str = ""):
    import sherpa_onnx  # type: ignore
    model = resolve_model_file(voice_dir, KOKORO_MODEL_CANDIDATES)
    voices_bin = voice_dir / KOKORO_VOICES_FILE
    if not voices_bin.is_file():
        raise RuntimeError(f"Kokoro voice at {voice_dir} is missing {KOKORO_VOICES_FILE}")
    tokens = str(voice_dir / TOKENS_FILE)

    # Multi-lang Kokoro (v1.0, v1.1, …) ships per-language lexicon-<locale>.txt
    # files. Monolingual v0.19 ships only the global `lexicon.txt`.
    #
    # Concatenating BOTH for a multi-lang bundle would be a mistake: sherpa
    # loads the lexicons in sequence and emits thousands of
    # "Duplicated word: …" warnings on every render, because the per-language
    # `lexicon-en.txt` overlaps almost entirely with the global `lexicon.txt`.
    # Pick one or the other based on layout, never both.
    single_lex = voice_dir / LEXICON_FILE
    multi_lexes = sorted(voice_dir.glob("lexicon-*.txt"))
    is_multi = bool(multi_lexes) or "multi-lang" in voice_dir.name or "v1_" in voice_dir.name
    lexicon_paths: list[str] = []
    if is_multi and multi_lexes:
        # Per-language only — sherpa picks the right one via the `lang` arg.
        lexicon_paths.extend(str(lx) for lx in multi_lexes)
    elif single_lex.is_file():
        lexicon_paths.append(str(single_lex))
    lexicon = ",".join(lexicon_paths)
    kokoro_kwargs: dict[str, Any] = dict(
        model=model,
        voices=str(voices_bin),
        tokens=tokens,
        lexicon=lexicon,
        data_dir=optional_path(voice_dir / ESPEAK_DIR),
        dict_dir=optional_path(voice_dir / DICT_DIR),
        length_scale=1.0,
    )
    if is_multi:
        # Sherpa-onnx exposes `lang` on the Kokoro config; passing it
        # selects which lexicon file drives tokenisation.
        kokoro_kwargs["lang"] = lang_hint or "en"

    return sherpa_onnx.OfflineTts(
        sherpa_onnx.OfflineTtsConfig(
            model=sherpa_onnx.OfflineTtsModelConfig(
                kokoro=sherpa_onnx.OfflineTtsKokoroModelConfig(**kokoro_kwargs),
                num_threads=2,
                debug=False,
                provider="cpu",
            ),
            rule_fsts=collect_rule_fsts(voice_dir),
            max_num_sentences=1,
        )
    )


def tts_kitten(voice_dir: Path):
    import sherpa_onnx  # type: ignore
    model = resolve_model_file(voice_dir, KITTEN_MODEL_CANDIDATES)
    voices_bin = voice_dir / KOKORO_VOICES_FILE  # Kitten reuses Kokoro's voices.bin layout
    if not voices_bin.is_file():
        raise RuntimeError(f"Kitten voice at {voice_dir} is missing {KOKORO_VOICES_FILE}")
    tokens = str(voice_dir / TOKENS_FILE)
    return sherpa_onnx.OfflineTts(
        sherpa_onnx.OfflineTtsConfig(
            model=sherpa_onnx.OfflineTtsModelConfig(
                kitten=sherpa_onnx.OfflineTtsKittenModelConfig(
                    model=model,
                    voices=str(voices_bin),
                    tokens=tokens,
                    data_dir=optional_path(voice_dir / ESPEAK_DIR),
                    length_scale=1.0,
                ),
                num_threads=2,
                debug=False,
                provider="cpu",
            ),
            max_num_sentences=1,
        )
    )


def tts_supertonic(voice_dir: Path):
    import sherpa_onnx  # type: ignore

    def req(name: str) -> str:
        f = voice_dir / name
        if not f.is_file():
            raise RuntimeError(f"Supertonic voice at {voice_dir} is missing {name}")
        return str(f)

    return sherpa_onnx.OfflineTts(
        sherpa_onnx.OfflineTtsConfig(
            model=sherpa_onnx.OfflineTtsModelConfig(
                supertonic=sherpa_onnx.OfflineTtsSupertonicModelConfig(
                    duration_predictor=req("duration_predictor.int8.onnx"),
                    text_encoder=req("text_encoder.int8.onnx"),
                    vector_estimator=req("vector_estimator.int8.onnx"),
                    vocoder=req("vocoder.int8.onnx"),
                    tts_json=req("tts.json"),
                    unicode_indexer=req("unicode_indexer.bin"),
                    voice_style=req("voice.bin"),
                ),
                num_threads=2,
                debug=False,
                provider="cpu",
            ),
            max_num_sentences=1,
        )
    )


def tts_zipvoice(voice_dir: Path):
    import sherpa_onnx  # type: ignore
    encoder = resolve_model_file(voice_dir, ZIPVOICE_ENCODER_CANDIDATES)
    decoder = resolve_model_file(voice_dir, ZIPVOICE_DECODER_CANDIDATES)
    vocoder = resolve_model_file(voice_dir, ZIPVOICE_VOCODER_CANDIDATES)
    tokens = str(voice_dir / TOKENS_FILE)
    return sherpa_onnx.OfflineTts(
        sherpa_onnx.OfflineTtsConfig(
            model=sherpa_onnx.OfflineTtsModelConfig(
                zipvoice=sherpa_onnx.OfflineTtsZipVoiceModelConfig(
                    tokens=tokens,
                    encoder=encoder,
                    decoder=decoder,
                    vocoder=vocoder,
                    data_dir=optional_path(voice_dir / ESPEAK_DIR),
                    lexicon=optional_path(voice_dir / LEXICON_FILE),
                ),
                num_threads=2,
                debug=False,
                provider="cpu",
            ),
            max_num_sentences=1,
        )
    )


def tts_pocket(voice_dir: Path):
    import sherpa_onnx  # type: ignore

    def req(name: str) -> str:
        f = voice_dir / name
        if not f.is_file():
            raise RuntimeError(f"Pocket voice at {voice_dir} is missing {name}")
        return str(f)

    return sherpa_onnx.OfflineTts(
        sherpa_onnx.OfflineTtsConfig(
            model=sherpa_onnx.OfflineTtsModelConfig(
                pocket=sherpa_onnx.OfflineTtsPocketModelConfig(
                    lm_flow=req("lm_flow.int8.onnx"),
                    lm_main=req("lm_main.int8.onnx"),
                    encoder=req("encoder.onnx"),
                    decoder=req("decoder.int8.onnx"),
                    text_conditioner=req("text_conditioner.onnx"),
                    vocab_json=req("vocab.json"),
                    token_scores_json=req("token_scores.json"),
                ),
                num_threads=2,
                debug=False,
                provider="cpu",
            ),
            max_num_sentences=1,
        )
    )


FAMILY_BUILDERS = {
    "piper": tts_vits,
    "vits": tts_vits,
    "matcha": tts_matcha,
    "kokoro": tts_kokoro,
    "kitten": tts_kitten,
    "supertonic": tts_supertonic,
    "zipvoice": tts_zipvoice,
    "pocket": tts_pocket,
}


def write_wav(samples, sample_rate: int, out_wav: Path) -> None:
    """Write float32 samples as PCM16 WAV. ffmpeg encodes to MP3 next."""
    import struct
    pcm = bytearray()
    for s in samples:
        v = max(-1.0, min(1.0, float(s)))
        pcm += struct.pack("<h", int(v * 32767))
    with wave.open(str(out_wav), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(sample_rate)
        w.writeframes(bytes(pcm))


def encode_mp3(in_wav: Path, out_mp3: Path) -> None:
    """ffmpeg → 96 kbps mono mp3 (~60 KB per 5 s clip, cellular-friendly)."""
    subprocess.run(
        [
            "ffmpeg", "-y", "-loglevel", "error",
            "-i", str(in_wav),
            "-codec:a", "libmp3lame",
            "-b:a", "96k",
            "-ac", "1",
            str(out_mp3),
        ],
        check=True,
    )


def _render_inline(
    voice: dict[str, Any],
    out_mp3: Path,
    work_dir: Path,
    cache_dir: Path | None,
    *,
    target_sid: int = 0,
    target_lang: str | None = None,
) -> bool:
    """Render one (voice, sid, language) combination inside the worker.

    Native ``exit(-1)`` here only kills the worker, not the parent.
    espeak-ng's global singleton state lives and dies inside the worker,
    so each combo gets a fresh data_dir.
    """
    family = (voice.get("family") or "").lower()
    builder = FAMILY_BUILDERS.get(family)
    if builder is None:
        log(f"skip {voice['id']}: unsupported family '{family}'")
        return False
    bundle_url = voice["bundleUrl"]
    langs = voice.get("languages") or []
    if target_lang is None:
        target_lang = langs[0] if langs else "en"
    text = phrase_for_lang(target_lang)

    with tempfile.TemporaryDirectory(dir=str(work_dir)) as tmp:
        tmp_path = Path(tmp)
        archive = tmp_path / Path(bundle_url).name
        download(bundle_url, archive, cache_dir=cache_dir)
        extract_root = extract_tar_bz2(archive, tmp_path / "unpacked")

        # Matcha bundles ship the vocoder as a sibling URL, not bundled.
        if family == "matcha" and not (extract_root / MATCHA_VOCODER_FILE).is_file():
            vocoder_url = voice.get("vocoderUrl")
            if vocoder_url:
                download(vocoder_url, extract_root / MATCHA_VOCODER_FILE, cache_dir=cache_dir)

        lang_head = target_lang.split("-")[0].lower()
        if family == "kokoro":
            tts = builder(extract_root, lang_hint=lang_head)
        else:
            tts = builder(extract_root)
        audio = tts.generate(text, sid=target_sid, speed=1.0)
        wav_out = tmp_path / "sample.wav"
        write_wav(audio.samples, audio.sample_rate, wav_out)
        encode_mp3(wav_out, out_mp3)
        log(f"  ✓ {out_mp3.name} ({out_mp3.stat().st_size // 1024} KB @ {audio.sample_rate} Hz)")
        return True


def render_one(
    voice: dict[str, Any],
    *,
    out_dir: Path,
    cache_dir: Path | None,
    work_dir: Path,
    target_sid: int,
    target_lang: str,
    in_subprocess: bool = True,
    per_voice_timeout: float = 240.0,
) -> bool:
    """Render one (voice, sid, language) combination.

    By default spawns a fresh Python subprocess so:
      (a) sherpa-onnx's process-global espeak-ng state can't leak between
          renders (the first combo's data_dir would otherwise be reused
          for every subsequent combo);
      (b) a native ``exit(-1)`` from the sherpa-onnx C++ layer only kills
          the worker, leaving the parent loop alive to continue.
    """
    voice_id = voice["id"]
    family = (voice.get("family") or "").lower()
    out_mp3 = out_dir / combo_filename(voice_id, target_sid, target_lang)
    if out_mp3.exists() and out_mp3.stat().st_size > 0:
        return True
    if family not in FAMILY_BUILDERS:
        log(f"skip {voice_id}: unsupported family '{family}'")
        return False
    text = phrase_for_lang(target_lang)
    log(f"render {voice_id} [{family}] sid={target_sid} lang={target_lang} '{text[:50]}…'")

    if not in_subprocess:
        try:
            return _render_inline(
                voice, out_mp3, work_dir, cache_dir,
                target_sid=target_sid, target_lang=target_lang,
            )
        except Exception as e:
            log(f"  ✗ {voice_id}: {type(e).__name__}: {e}")
            return False

    cmd = [
        sys.executable,
        str(Path(__file__).resolve()),
        "--render-one",
        json.dumps(voice, ensure_ascii=False),
        "--output", str(out_dir),
        "--cache", str(cache_dir or ""),
        "--target-sid", str(target_sid),
        "--target-lang", target_lang,
    ]
    try:
        proc = subprocess.run(
            cmd,
            timeout=per_voice_timeout,
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
        )
    except subprocess.TimeoutExpired:
        log(f"  ✗ {voice_id}: TimeoutExpired (>{per_voice_timeout:.0f}s)")
        return False
    if proc.stdout:
        sys.stdout.write(proc.stdout)
    if proc.stderr:
        sys.stderr.write(proc.stderr)
    if proc.returncode != 0:
        log(f"  ✗ {voice_id}: subprocess exit {proc.returncode}")
        return False
    return out_mp3.exists() and out_mp3.stat().st_size > 0


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--catalog", default="catalog/v1/models.json")
    ap.add_argument("--output", default="samples")
    ap.add_argument("--cache", default=".sample_cache")
    ap.add_argument(
        "--limit",
        type=int,
        default=0,
        help="Stop after rendering this many voices (0 = unlimited)",
    )
    ap.add_argument(
        "--only-missing",
        action="store_true",
        help="Skip voices whose MP3 already exists on disk",
    )
    ap.add_argument(
        "--family",
        action="append",
        help="Restrict to one or more families (repeatable)",
    )
    ap.add_argument(
        "--render-one",
        metavar="VOICE_JSON",
        help="Single-voice worker mode: render the given catalog row "
             "(passed as a JSON-encoded string) and exit. Used internally "
             "by the subprocess isolation layer; humans should not call "
             "this directly.",
    )
    ap.add_argument(
        "--no-subprocess",
        action="store_true",
        help="Render inline in the current process (faster startup, but a "
             "native exit() from sherpa-onnx kills the whole run). Default "
             "is to isolate each voice in a fresh subprocess.",
    )
    ap.add_argument(
        "--target-sid",
        type=int,
        default=0,
        help="Worker-mode only: the speaker id to render. Ignored in "
             "parent mode (which iterates over every speaker).",
    )
    ap.add_argument(
        "--target-lang",
        default="",
        help="Worker-mode only: the BCP47 language tag to render. Ignored "
             "in parent mode (which iterates over every render-language).",
    )
    ap.add_argument(
        "--voices",
        default="",
        help="Comma-separated list of voice IDs to render (matrix-shard "
             "mode). Empty = render every voice in the catalog.",
    )
    ap.add_argument(
        "--patch-output",
        default="",
        help="Path to write per-voice patches (sampleAudioUrl + "
             "speakerSamples) as JSON instead of mutating the catalog "
             "in place. Used by the matrix workflow so shards never write "
             "to the same file.",
    )
    args = ap.parse_args()

    # Worker mode: one (voice, sid, lang) triple, no orchestration. Called
    # by the parent when rendering with subprocess isolation. We intentionally
    # let exceptions propagate so the parent sees a non-zero exit code.
    if args.render_one is not None:
        voice = json.loads(args.render_one)
        out_dir = Path(args.output)
        out_dir.mkdir(parents=True, exist_ok=True)
        cache_dir = Path(args.cache) if args.cache else None
        work_dir = Path(tempfile.gettempdir())
        target_lang = args.target_lang or (voice.get("languages") or ["en"])[0]
        out_mp3 = out_dir / combo_filename(voice["id"], args.target_sid, target_lang)
        try:
            ok = _render_inline(
                voice, out_mp3, work_dir, cache_dir,
                target_sid=args.target_sid, target_lang=target_lang,
            )
        except Exception as e:
            log(f"  ✗ {voice['id']}: {type(e).__name__}: {e}")
            return 1
        return 0 if ok else 1

    catalog_path = Path(args.catalog)
    out_dir = Path(args.output)
    cache_dir = Path(args.cache) if args.cache else None
    out_dir.mkdir(parents=True, exist_ok=True)
    if cache_dir is not None:
        cache_dir.mkdir(parents=True, exist_ok=True)

    with catalog_path.open("r", encoding="utf-8") as f:
        catalog = json.load(f)

    voices: list[dict[str, Any]] = catalog.get("voices", [])
    work_dir = Path(tempfile.gettempdir())

    only_families: set[str] | None = None
    if args.family:
        only_families = {f.lower() for f in args.family}

    only_voices: set[str] | None = None
    if args.voices.strip():
        only_voices = {vid.strip() for vid in args.voices.split(",") if vid.strip()}

    patches: dict[str, dict] = {}

    produced = 0
    skipped = 0
    failed = 0
    voices_done = 0
    started = time.monotonic()
    for v in voices:
        if only_families is not None and (v.get("family") or "").lower() not in only_families:
            continue
        if only_voices is not None and v["id"] not in only_voices:
            continue
        speakers = v.get("speakers") or []
        if not speakers:
            speakers = [{"id": 0}]
        langs_to_render = render_languages(v)
        primary_lang = langs_to_render[0]
        voice_id = v["id"]
        combos: list[dict[str, Any]] = []
        any_produced_for_voice = False

        for sp in speakers:
            sid = int(sp.get("id", 0))
            for lang in langs_to_render:
                combo_path = out_dir / combo_filename(voice_id, sid, lang)
                if args.only_missing and combo_path.exists() and combo_path.stat().st_size > 0:
                    combos.append({"speakerId": sid, "language": lang, "url": combo_url(voice_id, sid, lang)})
                    skipped += 1
                    continue
                ok = render_one(
                    v,
                    out_dir=out_dir,
                    cache_dir=cache_dir,
                    work_dir=work_dir,
                    target_sid=sid,
                    target_lang=lang,
                    in_subprocess=not args.no_subprocess,
                )
                if ok:
                    combos.append({"speakerId": sid, "language": lang, "url": combo_url(voice_id, sid, lang)})
                    produced += 1
                    any_produced_for_voice = True
                else:
                    failed += 1

        # Persist whatever combos we got onto the catalog row, so the app's
        # `sampleFor(sid, lang)` can resolve a URL even when some combos
        # failed but others succeeded.
        if combos:
            v["speakerSamples"] = combos
            # Back-compat: the canonical single-URL field points at the
            # (sid=0, primary_lang) clip when it exists, else the first
            # successfully-rendered combo. The matching MP3 is also copied
            # to `<voice_id>.mp3` so legacy releases keep resolving.
            canonical = next(
                (c for c in combos if c["speakerId"] == 0 and c["language"] == primary_lang),
                combos[0],
            )
            canonical_src = out_dir / combo_filename(voice_id, canonical["speakerId"], canonical["language"])
            canonical_dst = out_dir / f"{voice_id}.mp3"
            if canonical_src.exists() and (
                not canonical_dst.exists()
                or canonical_dst.stat().st_size != canonical_src.stat().st_size
            ):
                shutil.copy2(canonical_src, canonical_dst)
            v["sampleAudioUrl"] = family_url(voice_id)
            patches[voice_id] = {
                "sampleAudioUrl": v["sampleAudioUrl"],
                "speakerSamples": combos,
            }

        if any_produced_for_voice:
            voices_done += 1
        if args.limit and voices_done >= args.limit:
            break

    elapsed = time.monotonic() - started
    log(
        f"rendered {produced} combos across {voices_done} voices · "
        f"cached {skipped} · failed {failed} in {elapsed:.0f}s"
    )

    if args.patch_output:
        # Matrix-shard mode: don't touch the catalog (other shards are
        # running in parallel against the same file in their own runners).
        # Write a per-voice patch artifact that the publish job merges.
        patch_path = Path(args.patch_output)
        patch_path.parent.mkdir(parents=True, exist_ok=True)
        with patch_path.open("w", encoding="utf-8", newline="\n") as f:
            json.dump(patches, f, ensure_ascii=False, indent=2)
            f.write("\n")
        log(f"patches written → {patch_path} ({len(patches)} voices)")
    else:
        with catalog_path.open("w", encoding="utf-8", newline="\n") as f:
            json.dump(catalog, f, ensure_ascii=False, indent=2)
            f.write("\n")
        log(f"catalog updated → {catalog_path}")
    return 0 if failed == 0 else 0  # never fail the workflow on individual voice errors


if __name__ == "__main__":
    sys.exit(main())
