"""Render ``docs/MODELS.md`` from ``catalog/v1/models.json``.

Runs as the second step of the weekly ``catalog-refresh`` workflow so the
human-readable model index in the repo never drifts from the machine-readable
JSON the app actually loads. The markdown file is what the README links to —
keep it deterministic (sorted by family then title) so diffs against
upstream churn stay readable.

Usage::

    python tools/catalog/build_model_list.py \\
        --catalog catalog/v1/models.json \\
        --output docs/MODELS.md
"""
from __future__ import annotations

import argparse
import json
import sys
from collections import Counter, defaultdict
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

FAMILY_BLURB: dict[str, str] = {
    "piper": (
        "Compact VITS-based voices from the rhasspy/piper project. 10–60 MB "
        "per voice, sub-second on a 2020+ phone, ~70 languages covered."
    ),
    "kokoro": (
        "Higher-quality multilingual VITS variant (Kokoro-82M). 80–360 MB per "
        "voice; English bundles ship 1–50 speakers in a single model."
    ),
    "kitten": (
        "Tiny English-only VITS distillations tuned for low-end phones. "
        "<60 MB, fastest synthesis on the catalog."
    ),
    "matcha": (
        "Diffusion-based Matcha-TTS voices. Ships a vocoder side-asset "
        "alongside the main weights — Browse handles the dual download."
    ),
    "supertonic": (
        "Newest (2026) multilingual model from Supertone. Single 100–200 MB "
        "bundle covering ~30 languages × 10 speakers."
    ),
    "zipvoice": (
        "Flow-matching voice-cloning model. Accepts a reference clip + "
        "transcript and synthesises the target text in the cloned voice."
    ),
    "pocket": (
        "Compact voice-cloning model. Same reference-audio API as ZipVoice "
        "but with a smaller voice-embedding cache and lighter weights."
    ),
}

FAMILY_ORDER = [
    "piper", "kokoro", "kitten", "matcha", "supertonic", "zipvoice", "pocket",
]


def fmt_size_mb(mb: int | None) -> str:
    if mb is None or mb <= 0:
        return "—"
    return f"{mb} MB"


def fmt_languages(langs: list[str]) -> str:
    if not langs:
        return "—"
    if len(langs) <= 4:
        return ", ".join(langs)
    return f"{len(langs)} languages"


def fmt_speakers(spks: list[dict[str, Any]]) -> str:
    n = len(spks)
    if n == 0:
        return "—"
    if n == 1:
        return "1"
    return str(n)


def fmt_license(lic: str | None) -> str:
    if not lic:
        return "—"
    if lic.startswith("http"):
        return f"[License]({lic})"
    return lic


def supports_cloning(family: str) -> bool:
    return family in {"zipvoice", "pocket"}


def render(catalog_path: Path, output_path: Path) -> None:
    data = json.loads(catalog_path.read_text(encoding="utf-8"))
    voices = data["voices"]

    by_family: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for v in voices:
        by_family[v["family"]].append(v)
    for k in by_family:
        by_family[k].sort(key=lambda v: v["title"].lower())

    fam_counts = Counter(v["family"] for v in voices)
    langs: set[str] = set()
    for v in voices:
        langs.update(v["languages"])
    sizes = [int(v.get("approxSizeMb", 0) or 0) for v in voices]
    clone_count = sum(1 for v in voices if supports_cloning(v["family"]))

    generated_at = datetime.now(timezone.utc).strftime("%Y-%m-%d")

    lines: list[str] = []
    lines.append("# Voice catalog")
    lines.append("")
    lines.append(
        f"*Auto-generated from `catalog/v1/models.json` on {generated_at} "
        "by `tools/catalog/build_model_list.py` (run from the weekly "
        "[catalog-refresh](../.github/workflows/catalog-refresh.yml) workflow). "
        "Do not edit by hand — the next refresh will overwrite your changes.*"
    )
    lines.append("")
    lines.append("## Summary")
    lines.append("")
    lines.append(f"- **{len(voices)} voices** across **{len(fam_counts)} model families**")
    lines.append(f"- **{len(langs)} languages** covered")
    if sizes:
        median = sorted(sizes)[len(sizes) // 2]
        lines.append(
            f"- Bundle size: {min(sizes)}–{max(sizes)} MB "
            f"(median {median} MB)"
        )
    if clone_count > 0:
        lines.append(f"- **{clone_count} voices** support reference-audio cloning")
    lines.append("")
    lines.append("### By family")
    lines.append("")
    lines.append("| Family | Voices | Notes |")
    lines.append("|---|---:|---|")
    for fam in FAMILY_ORDER:
        if fam_counts.get(fam, 0) == 0:
            continue
        lines.append(
            f"| **{fam}** | {fam_counts[fam]} | {FAMILY_BLURB.get(fam, '—')} |"
        )
    # Surface any family the script doesn't have copy for, so we never
    # silently drop a new family upstream adds.
    for fam, count in fam_counts.items():
        if fam in FAMILY_ORDER:
            continue
        lines.append(f"| **{fam}** | {count} | _(family blurb pending — add to `FAMILY_BLURB`)_ |")
    lines.append("")

    for fam in FAMILY_ORDER + [f for f in fam_counts if f not in FAMILY_ORDER]:
        rows = by_family.get(fam)
        if not rows:
            continue
        lines.append(f"## {fam} ({len(rows)})")
        if fam in FAMILY_BLURB:
            lines.append("")
            lines.append(FAMILY_BLURB[fam])
        lines.append("")
        lines.append("| Title | Languages | Speakers | Size | Tier | Quality | RTF | License |")
        lines.append("|---|---|---:|---:|---|---|---:|---|")
        for v in rows:
            cloning = " · 🎤 cloning" if supports_cloning(v["family"]) else ""
            quality = v.get("quality") or "—"
            rtf = v.get("renderRtf")
            rtf_cell = f"{rtf:.2f}" if isinstance(rtf, (int, float)) else "—"
            lines.append(
                f"| {v['title']}{cloning} | {fmt_languages(v['languages'])} | "
                f"{fmt_speakers(v['speakers'])} | {fmt_size_mb(int(v.get('approxSizeMb') or 0))} | "
                f"{v.get('tier', '—')} | {quality} | {rtf_cell} | "
                f"{fmt_license(v.get('license'))} |"
            )
        lines.append("")

    lines.append("---")
    lines.append("")
    lines.append(
        "_Source of truth: [`catalog/v1/models.json`](../catalog/v1/models.json). "
        "Each voice entry includes the bundle URL, sha256, sample rate, and "
        "per-(speaker, language) audition URLs in the JSON; this page is just "
        "the human-readable index._"
    )
    lines.append("")

    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text("\n".join(lines), encoding="utf-8")


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--catalog", type=Path, default=Path("catalog/v1/models.json"))
    ap.add_argument("--output", type=Path, default=Path("docs/MODELS.md"))
    args = ap.parse_args()
    render(args.catalog, args.output)
    return 0


if __name__ == "__main__":
    sys.exit(main())
