"""
Render a fixed per-locale sample sentence through every voice in
`catalog/v1/models.json` and write the result as a single MP3 per voice.

Triggered from `.github/workflows/render-samples.yml` on a GitHub-hosted
runner (NOT blacksmith). The script is deliberately bandwidth-bound, not
CPU-bound — we tolerate the ~10 GB of upstream bundle traffic so the
catalog can advertise self-hosted samples instead of relying on
HuggingFace Spaces or upstream piper-voices URLs.

Output layout:

    samples/<voice_id>.mp3       (uploaded to the `samples-rolling` release)
    catalog/v1/models.json       (patched with `sampleAudioUrl` pointing at
                                  the self-hosted URL for every rendered voice)

The script is idempotent: re-running skips voices whose MP3 already exists
on disk and whose catalog row already has the correct sampleAudioUrl.

Currently renders **Piper** voices only (174/188 catalogue entries today).
Other families (Kokoro, Kitten, Matcha, Supertonic) have family-specific
config shapes that need their own builders; those still fall back to the
HuggingFace Space link in the app. Adding them is a straight port of
`SherpaTtsRuntime.kt`'s per-family branches; the seam is `synthesize()`
below.
"""

from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess
import sys
import tarfile
import tempfile
from pathlib import Path
from typing import Any

import requests

SAMPLES_RELEASE_TAG = "samples-rolling"
SAMPLES_DOWNLOAD_BASE = (
    f"https://github.com/HayaiApp/HayaiTTS/releases/download/{SAMPLES_RELEASE_TAG}"
)

# Per-locale sample phrase. Picks a pangram or culturally familiar short
# sentence so the user hears expressive prosody, not just a flat reading.
# Keep these under ~80 characters so synthesis stays under ~5 seconds.
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


def phrase_for(languages: list[str]) -> str:
    """Pick a phrase using the BCP-47 tag's language subtag."""
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
    """Download with optional disk cache keyed by URL last segment."""
    if cache_dir is not None:
        cache_file = cache_dir / dest.name
        if cache_file.exists() and cache_file.stat().st_size > 0:
            shutil.copy2(cache_file, dest)
            return
    log(f"  downloading {url}")
    with requests.get(url, stream=True, timeout=120) as r:
        r.raise_for_status()
        with dest.open("wb") as f:
            for chunk in r.iter_content(chunk_size=1024 * 1024):
                if chunk:
                    f.write(chunk)
    if cache_dir is not None:
        cache_dir.mkdir(parents=True, exist_ok=True)
        shutil.copy2(dest, cache_dir / dest.name)


def extract_tar_bz2(archive: Path, into: Path) -> Path:
    """Extract a tar.bz2; return the path to the first top-level directory."""
    into.mkdir(parents=True, exist_ok=True)
    with tarfile.open(archive, "r:*") as tar:
        # safe extract: refuse paths that escape `into`
        for m in tar.getmembers():
            target = (into / m.name).resolve()
            if not str(target).startswith(str(into.resolve())):
                raise RuntimeError(f"unsafe path in archive: {m.name}")
        tar.extractall(into)
    # Find the unpacked dir
    children = [c for c in into.iterdir() if c.is_dir()]
    if len(children) != 1:
        # Fallback to `into` itself (some bundles are flat)
        return into
    return children[0]


def render_piper(voice_dir: Path, text: str, out_wav: Path) -> int:
    """Synthesize via sherpa-onnx Python. Returns the sample rate."""
    import sherpa_onnx  # type: ignore

    model_path = next(voice_dir.glob("**/*.onnx"))
    tokens_path = voice_dir / "tokens.txt"
    if not tokens_path.exists():
        # tokens may be nested one level under voice name
        candidates = list(voice_dir.glob("**/tokens.txt"))
        if not candidates:
            raise RuntimeError(f"no tokens.txt in {voice_dir}")
        tokens_path = candidates[0]
    # data_dir is sherpa-onnx's espeak-ng data; the apt package provides it.
    data_dir = "/usr/share/espeak-ng-data"

    config = sherpa_onnx.OfflineTtsConfig(
        model=sherpa_onnx.OfflineTtsModelConfig(
            vits=sherpa_onnx.OfflineTtsVitsModelConfig(
                model=str(model_path),
                tokens=str(tokens_path),
                data_dir=data_dir,
            ),
            num_threads=2,
            debug=False,
            provider="cpu",
        ),
        max_num_sentences=1,
    )
    tts = sherpa_onnx.OfflineTts(config)
    audio = tts.generate(text, sid=0, speed=1.0)

    # Write PCM16 WAV; ffmpeg encodes to MP3 in the next step.
    import wave
    import struct
    rate = audio.sample_rate
    pcm = bytearray()
    for s in audio.samples:
        v = max(-1.0, min(1.0, float(s)))
        pcm += struct.pack("<h", int(v * 32767))
    with wave.open(str(out_wav), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(rate)
        w.writeframes(bytes(pcm))
    return rate


def encode_mp3(in_wav: Path, out_mp3: Path) -> None:
    """ffmpeg -> 96 kbps mono mp3. Small enough to stream on cellular."""
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
    """Render one voice. Returns True if a sample was produced."""
    voice_id = voice["id"]
    family = voice.get("family", "")
    out_mp3 = out_dir / f"{voice_id}.mp3"
    if out_mp3.exists() and out_mp3.stat().st_size > 0:
        return True
    if family not in ("piper", "vits"):
        # Other families need their own sherpa-onnx config shape; skipping
        # for v1 (they keep the HF Space fallback).
        return False
    bundle_url = voice["bundleUrl"]
    text = phrase_for(voice.get("languages") or ["en"])
    log(f"render {voice_id} ({family}) → '{text[:40]}…'")

    with tempfile.TemporaryDirectory(dir=str(work_dir)) as tmp:
        tmp_path = Path(tmp)
        archive = tmp_path / Path(bundle_url).name
        try:
            download(bundle_url, archive, cache_dir=cache_dir)
            extract_root = extract_tar_bz2(archive, tmp_path / "unpacked")
            wav_out = tmp_path / "sample.wav"
            render_piper(extract_root, text, wav_out)
            encode_mp3(wav_out, out_mp3)
            log(f"  ✓ {out_mp3.name} ({out_mp3.stat().st_size // 1024} KB)")
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
        help="Skip voices that already have sampleAudioUrl in the catalog",
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

    produced = 0
    for v in voices:
        if args.only_missing and v.get("sampleAudioUrl"):
            continue
        if render_one(v, out_dir=out_dir, cache_dir=cache_dir, work_dir=work_dir):
            v["sampleAudioUrl"] = family_url(v["id"])
            produced += 1
            if args.limit and produced >= args.limit:
                break

    with catalog_path.open("w", encoding="utf-8", newline="\n") as f:
        json.dump(catalog, f, ensure_ascii=False, indent=2)
        f.write("\n")

    log(f"rendered {produced} samples → {out_dir}")
    log(f"catalog updated → {catalog_path}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
