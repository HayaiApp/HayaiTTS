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
        tar.extractall(into)
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


def tts_kokoro(voice_dir: Path):
    import sherpa_onnx  # type: ignore
    model = resolve_model_file(voice_dir, KOKORO_MODEL_CANDIDATES)
    voices_bin = voice_dir / KOKORO_VOICES_FILE
    if not voices_bin.is_file():
        raise RuntimeError(f"Kokoro voice at {voice_dir} is missing {KOKORO_VOICES_FILE}")
    tokens = str(voice_dir / TOKENS_FILE)
    return sherpa_onnx.OfflineTts(
        sherpa_onnx.OfflineTtsConfig(
            model=sherpa_onnx.OfflineTtsModelConfig(
                kokoro=sherpa_onnx.OfflineTtsKokoroModelConfig(
                    model=model,
                    voices=str(voices_bin),
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


def render_one(
    voice: dict[str, Any],
    *,
    out_dir: Path,
    cache_dir: Path | None,
    work_dir: Path,
) -> bool:
    voice_id = voice["id"]
    family = (voice.get("family") or "").lower()
    out_mp3 = out_dir / f"{voice_id}.mp3"
    if out_mp3.exists() and out_mp3.stat().st_size > 0:
        return True
    builder = FAMILY_BUILDERS.get(family)
    if builder is None:
        log(f"skip {voice_id}: unsupported family '{family}'")
        return False
    bundle_url = voice["bundleUrl"]
    text = phrase_for(voice.get("languages") or ["en"])
    log(f"render {voice_id} [{family}] '{text[:50]}…'")

    with tempfile.TemporaryDirectory(dir=str(work_dir)) as tmp:
        tmp_path = Path(tmp)
        archive = tmp_path / Path(bundle_url).name
        try:
            download(bundle_url, archive, cache_dir=cache_dir)
            extract_root = extract_tar_bz2(archive, tmp_path / "unpacked")

            # Matcha bundles ship the vocoder as a sibling URL, not bundled.
            # The Kotlin side downloads it separately when missing; we
            # mirror that here.
            if family == "matcha" and not (extract_root / MATCHA_VOCODER_FILE).is_file():
                vocoder_url = voice.get("vocoderUrl")
                if vocoder_url:
                    voc_archive = extract_root / MATCHA_VOCODER_FILE
                    download(vocoder_url, voc_archive, cache_dir=cache_dir)

            tts = builder(extract_root)
            audio = tts.generate(text, sid=0, speed=1.0)
            wav_out = tmp_path / "sample.wav"
            write_wav(audio.samples, audio.sample_rate, wav_out)
            encode_mp3(wav_out, out_mp3)
            log(f"  ✓ {out_mp3.name} ({out_mp3.stat().st_size // 1024} KB @ {audio.sample_rate} Hz)")
            return True
        except Exception as e:
            log(f"  ✗ {voice_id}: {type(e).__name__}: {e}")
            return False


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
    args = ap.parse_args()

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

    produced = 0
    skipped = 0
    failed = 0
    started = time.monotonic()
    for v in voices:
        if only_families is not None and (v.get("family") or "").lower() not in only_families:
            continue
        if args.only_missing and (out_dir / f"{v['id']}.mp3").exists():
            v["sampleAudioUrl"] = family_url(v["id"])
            skipped += 1
            continue
        ok = render_one(v, out_dir=out_dir, cache_dir=cache_dir, work_dir=work_dir)
        if ok:
            v["sampleAudioUrl"] = family_url(v["id"])
            produced += 1
        else:
            failed += 1
        if args.limit and produced >= args.limit:
            break

    elapsed = time.monotonic() - started
    log(f"rendered {produced} · cached {skipped} · failed {failed} in {elapsed:.0f}s")

    with catalog_path.open("w", encoding="utf-8", newline="\n") as f:
        json.dump(catalog, f, ensure_ascii=False, indent=2)
        f.write("\n")
    log(f"catalog updated → {catalog_path}")
    return 0 if failed == 0 else 0  # never fail the workflow on individual voice errors


if __name__ == "__main__":
    sys.exit(main())
