"""Render Android launcher / splash assets from the master Hayai SVGs.

Run with:  python .github/branding/build_branding.py
Requires:  Pillow, resvg-py  (pip install --user Pillow resvg-py)
"""

from __future__ import annotations

import re
from io import BytesIO
from pathlib import Path

from PIL import Image
import resvg_py

ROOT = Path(__file__).resolve().parents[2]
BRANDING = ROOT / ".github" / "branding"
RES = ROOT / "app" / "src" / "main" / "res"
DEBUG_RES = ROOT / "app" / "src" / "debug" / "res"
NIGHTLY_RES = ROOT / "app" / "src" / "nightly" / "res"
README_IMAGES = ROOT / ".github" / "readme-images"
APP_ROOT = ROOT / "app" / "src" / "main"

ICON_SVG = (BRANDING / "app-icon.svg").read_text(encoding="utf-8")
ROUND_SVG = (BRANDING / "app-icon-round.svg").read_text(encoding="utf-8")
FG_SVG = (BRANDING / "app-icon-foreground.svg").read_text(encoding="utf-8")

# Master SVG canvas size (square + round + foreground all share the same viewport).
ICON_VIEWPORT = 2201

# Extra padding applied around the logo silhouette inside the icon canvas.
# 1.0 = paths fill the master SVG as-authored; smaller values shrink the logo
# toward the canvas center so launcher / splash renders aren't visually
# cropped or "zoomed in." The brand background rect stays full-bleed.
LOGO_SCALE = 0.72

# Notification small-icon (status bar) wants the silhouette to fill more of
# its 24dp canvas — at LOGO_SCALE it disappears against the status bar. Keep
# this close to 1.0; a touch of padding only so it doesn't kiss the edges.
NOTIFICATION_LOGO_SCALE = 0.92

DENSITY_SIZES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

# Adaptive-icon background-color variants (matches values/colors.xml).
# "classic" reuses the brand rosé so the legacy raster matches the default adaptive icon.
LAUNCHER_COLOR_VARIANTS = {
    "classic": "#0E7C86",
    "blue":    "#1E3A5F",
    "gray":    "#374151",
    "orange":  "#5C2E0E",
}

# Original brand teal in the master SVG — what we swap out for variants.
BRAND_BG_HEX = "#0E7C86"


def render(svg: str, size: int) -> Image.Image:
    png_bytes = bytes(
        resvg_py.svg_to_bytes(svg_string=svg, width=size, height=size)
    )
    return Image.open(BytesIO(png_bytes)).convert("RGBA")


def save_png(svg: str, path: Path, size: int) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img = render(svg, size)
    img.save(path, "PNG", optimize=True)
    print(f"  wrote {path.relative_to(ROOT)}  ({size}x{size})")


def save_webp(svg: str, path: Path, size: int, *, lossless: bool = True) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img = render(svg, size)
    img.save(path, "WEBP", lossless=lossless, quality=100, method=6)
    print(f"  wrote {path.relative_to(ROOT)}  ({size}x{size})")


def desaturate_to_white(svg: str) -> str:
    """Return svg with every fill color forced to #FFFFFF (for monochrome adaptive icon)."""
    return re.sub(r'fill="#[0-9A-Fa-f]{3,8}"', 'fill="#FFFFFF"', svg)


def with_backdrop(svg: str, color: str, viewport: int = ICON_VIEWPORT) -> str:
    """Inject a full-bleed background <rect> right after the opening <svg> tag.

    Used for the monochrome preview so the white silhouette renders against a
    dark surface (otherwise it's white-on-transparent → invisible on white UIs).
    """
    backdrop = f'<rect width="{viewport}" height="{viewport}" fill="{color}"/>'
    return re.sub(r"(<svg\b[^>]*>)", r"\1" + backdrop, svg, count=1)


PATH_RE = re.compile(
    r'<path\s+d="(?P<d>[^"]+)"\s+fill="(?P<fill>#[0-9A-Fa-f]{3,8})"\s*/>',
    re.DOTALL,
)


def extract_paths(svg: str) -> list[tuple[str, str]]:
    """Return [(pathData, fillColor)] from an SVG, in document order."""
    out: list[tuple[str, str]] = []
    for m in PATH_RE.finditer(svg):
        d = " ".join(m.group("d").split())
        out.append((d, m.group("fill").upper()))
    return out


def pad_paths(svg: str, scale: float = LOGO_SCALE, viewport: int = ICON_VIEWPORT) -> str:
    """Wrap every <path/> in a <g transform> that scales the logo around the canvas center.

    Leaves any <rect> (the brand background) untouched so it keeps full bleed.
    """
    if scale == 1.0:
        return svg
    matches = list(PATH_RE.finditer(svg))
    if not matches:
        return svg
    pivot = viewport / 2.0
    first_start = matches[0].start()
    last_end = matches[-1].end()
    transform = (
        f'<g transform="translate({pivot:g} {pivot:g}) '
        f'scale({scale:g}) translate({-pivot:g} {-pivot:g})">'
    )
    return svg[:first_start] + transform + svg[first_start:last_end] + "</g>" + svg[last_end:]


def vector_drawable(
    paths: list[tuple[str, str]],
    *,
    width_dp: int,
    height_dp: int,
    viewport_w: float,
    viewport_h: float,
    scale: float = LOGO_SCALE,
) -> str:
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        f'    android:width="{width_dp}dp"',
        f'    android:height="{height_dp}dp"',
        f'    android:viewportWidth="{viewport_w:g}"',
        f'    android:viewportHeight="{viewport_h:g}">',
    ]
    scaled = scale != 1.0
    if scaled:
        pivot_x = viewport_w / 2.0
        pivot_y = viewport_h / 2.0
        lines.append("    <group")
        lines.append(f'        android:scaleX="{scale:g}"')
        lines.append(f'        android:scaleY="{scale:g}"')
        lines.append(f'        android:pivotX="{pivot_x:g}"')
        lines.append(f'        android:pivotY="{pivot_y:g}">')
    path_indent = "        " if scaled else "    "
    attr_indent = "            " if scaled else "        "
    for d, fill in paths:
        lines.append(f"{path_indent}<path")
        lines.append(f'{attr_indent}android:pathData="{d}"')
        lines.append(f'{attr_indent}android:fillColor="{fill}" />')
    if scaled:
        lines.append("    </group>")
    lines.append("</vector>")
    lines.append("")
    return "\n".join(lines)


def write_text(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    print(f"  wrote {path.relative_to(ROOT)}")


def write_vector_drawables() -> None:
    """Regenerate the Android vector drawables that mirror the master SVGs."""
    fg_paths = extract_paths(FG_SVG)
    icon_paths = extract_paths(ICON_SVG)

    print("== vector drawables ==")

    # Adaptive-icon foreground: white silhouette inside 108dp safe canvas.
    # The new master SVG already builds in generous padding (logo bbox sits
    # well inside 192dp / 288dp safe area), so we keep the launcher pieces at
    # the standard 108dp adaptive size.
    fg_xml = vector_drawable(
        fg_paths,
        width_dp=108,
        height_dp=108,
        viewport_w=ICON_VIEWPORT,
        viewport_h=ICON_VIEWPORT,
    )
    write_text(RES / "drawable" / "ic_launcher_foreground.xml", fg_xml)

    # Themed-icon (monochrome) layer for adaptive icons — same silhouette
    write_text(RES / "drawable" / "ic_hayai_monochrome_launcher.xml", fg_xml)

    # Legacy/full ic_launcher drawable (with rosé background) used as drawable resource
    write_text(
        RES / "drawable" / "ic_launcher.xml",
        vector_drawable(
            icon_paths,
            width_dp=108,
            height_dp=108,
            viewport_w=ICON_VIEWPORT,
            viewport_h=ICON_VIEWPORT,
        ),
    )

    # Splash-screen vector — silhouette only (windowSplashScreenBackground
    # sets the rosé fill). 288dp is the Android 12 spec for foreground-only
    # splash icons; the LOGO_SCALE padding matches the launcher look.
    write_text(
        RES / "drawable" / "ic_hayai_splash.xml",
        vector_drawable(
            fg_paths,
            width_dp=288,
            height_dp=288,
            viewport_w=ICON_VIEWPORT,
            viewport_h=ICON_VIEWPORT,
        ),
    )

    # Notification small-icon (status bar). Status-bar real estate is tiny, so
    # we use a much milder padding than the launcher — otherwise the silhouette
    # is barely visible in the 24dp tray slot.
    write_text(
        RES / "drawable" / "ic_hayai.xml",
        vector_drawable(
            fg_paths,
            width_dp=24,
            height_dp=24,
            viewport_w=ICON_VIEWPORT,
            viewport_h=ICON_VIEWPORT,
            scale=NOTIFICATION_LOGO_SCALE,
        ),
    )


def main() -> None:
    write_vector_drawables()

    # Padded variants share the LOGO_SCALE shrink so PNG/webp rasters match
    # the in-app vector drawables (less zoomed-in launcher / notification logo).
    icon_svg_padded = pad_paths(ICON_SVG)
    round_svg_padded = pad_paths(ROUND_SVG)
    fg_svg_padded = pad_paths(FG_SVG)

    print("== mipmap launchers (PNG) ==")
    # Square master → ic_launcher.png, rounded master → ic_launcher_round.png.
    # Pre-Oreo launchers fall back to these rasters as-is (no adaptive mask),
    # so the file's shape needs to match the launcher's expectation.
    raster_sources = {
        "ic_launcher": icon_svg_padded,
        "ic_launcher_round": round_svg_padded,
    }
    for variant, source_svg in raster_sources.items():
        for density, size in DENSITY_SIZES.items():
            for res_root in (RES, DEBUG_RES, NIGHTLY_RES):
                target = res_root / f"mipmap-{density}" / f"{variant}.png"
                if not target.parent.exists():
                    continue
                save_png(source_svg, target, size)

    print("== alternate-color launcher rasters (.webp) ==")
    for variant_name, color in LAUNCHER_COLOR_VARIANTS.items():
        square_variant = icon_svg_padded.replace(f'"{BRAND_BG_HEX}"', f'"{color}"')
        round_variant = round_svg_padded.replace(f'"{BRAND_BG_HEX}"', f'"{color}"')
        # Sanity: every variant must have actually swapped the bg color.
        assert color in square_variant, f"bg fill swap failed (square) for {variant_name}"
        assert color in round_variant, f"bg fill swap failed (round) for {variant_name}"
        for density, size in DENSITY_SIZES.items():
            save_webp(square_variant, RES / f"mipmap-{density}" / f"ic_launcher_{variant_name}.webp", size, lossless=True)
            save_webp(round_variant, RES / f"mipmap-{density}" / f"ic_launcher_round_{variant_name}.webp", size, lossless=True)
        # Branding preview at 512 for design reviews (square version).
        save_png(square_variant, BRANDING / f"app-icon-{variant_name}.png", 512)

    print("== Play Store / web icons ==")
    # Play Store requires a 512×512 square; Google rounds it for the listing.
    save_png(icon_svg_padded, APP_ROOT / "ic_launcher-playstore.png", 512)
    save_png(icon_svg_padded, APP_ROOT / "ic_launcher-web.png", 512)

    print("== README marketplace asset ==")
    # README badge looks better as a circle since it's displayed standalone.
    save_webp(round_svg_padded, README_IMAGES / "app-icon.webp", 1024, lossless=True)

    print("== monochrome preview (debug aid only) ==")
    # Themed-icon layer is white silhouette on transparent — preview it on a
    # dark backdrop so designers can actually see the shape.
    monochrome_preview = with_backdrop(desaturate_to_white(fg_svg_padded), "#1F1F1F")
    save_png(monochrome_preview, BRANDING / "app-icon-monochrome.png", 512)

    print("done.")


if __name__ == "__main__":
    main()
