"""Compute a cost-balanced sharding plan for the sample-render workflow.

The render workload is dominated by Kokoro multi-lang voices (≈100 speakers
× ≈10 languages ≈ 1000 renders each); Piper voices are 1 render each. A
naive round-robin shards Kokoro alongside Piper and starves whichever job
lands on the multi-lang voices. We bin-pack by ``speakers × render_langs``
so every shard finishes in roughly the same wall time.

Output schema written to ``--output``:

    [
      {"index": 0, "voices": ["piper-en_US-amy-low", ...], "cost": 87},
      {"index": 1, "voices": [...], "cost": 85},
      ...
    ]

The workflow's `render` matrix iterates over each shard's ``index``;
inside the job, the shard's voice list is loaded from this same JSON.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

# Local import — both scripts live in the same dir at runtime.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from render_samples import render_languages  # type: ignore  # noqa: E402


def voice_cost(voice: dict) -> int:
    speakers = voice.get("speakers") or [{"id": 0}]
    langs = render_languages(voice)
    return max(1, len(speakers)) * max(1, len(langs))


def bin_pack(voices: list[dict], shards: int) -> list[dict]:
    """Longest-Processing-Time greedy: assign each voice (descending cost)
    to the currently-lightest shard. Within a few % of optimal for our
    workload size."""
    bins: list[dict] = [{"index": i, "voices": [], "cost": 0} for i in range(shards)]
    ordered = sorted(voices, key=lambda v: voice_cost(v), reverse=True)
    for v in ordered:
        target = min(bins, key=lambda b: b["cost"])
        target["voices"].append(v["id"])
        target["cost"] += voice_cost(v)
    return bins


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--catalog", default="catalog/v1/models.json")
    ap.add_argument("--shards", type=int, default=12)
    ap.add_argument("--output", default="shard-plan.json")
    args = ap.parse_args(argv)

    with open(args.catalog, "r", encoding="utf-8") as fh:
        catalog = json.load(fh)
    voices = catalog.get("voices", [])
    plan = bin_pack(voices, args.shards)

    with open(args.output, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(plan, fh, ensure_ascii=False, indent=2)
        fh.write("\n")

    total_cost = sum(b["cost"] for b in plan)
    print(f"Planned {len(voices)} voices into {args.shards} shards (total cost {total_cost}):")
    for b in plan:
        print(f"  shard {b['index']}: {len(b['voices']):3d} voices · cost {b['cost']:5d}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
