"""Merge the hand-curated ``tools/catalog/overlays/voices.yaml`` onto
``catalog/v1/models.json``.

Runs as the third step of the weekly ``catalog-refresh`` workflow,
after the upstream scrape + the model-list regen. The overlay carries
descriptions, tags, recommended use cases, source URLs and other fields
that nothing else in the pipeline can populate — see the YAML header for
the schema.

Order of precedence (lowest → highest):

1. Scraped catalog (release-asset fallback + doc-page parser).
2. Bundle-probe enrichment merged by the render-samples publish step.
3. This overlay.

Family-level defaults apply to every voice whose family matches and that
doesn't already have a value for the field. Per-voice overrides always
win.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

try:
    import yaml  # type: ignore
except ImportError as e:
    raise SystemExit(
        "PyYAML required: pip install pyyaml (or add to tools/catalog/requirements.txt)",
    ) from e


def apply(catalog_path: Path, overlay_path: Path) -> int:
    catalog = json.loads(catalog_path.read_text(encoding="utf-8"))
    overlay = yaml.safe_load(overlay_path.read_text(encoding="utf-8")) or {}
    family_defaults: dict[str, dict] = overlay.get("families", {}) or {}
    per_voice: dict[str, dict] = overlay.get("voices", {}) or {}

    touched = 0
    for v in catalog.get("voices", []):
        family = (v.get("family") or "").lower()
        fam_overrides = family_defaults.get(family) or {}
        # Family defaults only fill *missing* keys — never downgrade
        # anything the scraper or bundle probe produced.
        for k, val in fam_overrides.items():
            if v.get(k) in (None, "", [], {}):
                v[k] = val
                touched += 1
        # Per-voice keys overwrite unconditionally.
        voice_overrides = per_voice.get(v["id"]) or {}
        for k, val in voice_overrides.items():
            v[k] = val
            touched += 1

    catalog_path.write_text(
        json.dumps(catalog, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    print(f"overlay applied: {touched} field write(s) across "
          f"{len(catalog.get('voices', []))} voices")
    return 0


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--catalog", type=Path, default=Path("catalog/v1/models.json"))
    ap.add_argument(
        "--overlay",
        type=Path,
        default=Path("tools/catalog/overlays/voices.yaml"),
    )
    args = ap.parse_args()
    if not args.overlay.is_file():
        print(f"No overlay at {args.overlay} — nothing to apply.")
        return 0
    return apply(args.catalog, args.overlay)


if __name__ == "__main__":
    sys.exit(main())
