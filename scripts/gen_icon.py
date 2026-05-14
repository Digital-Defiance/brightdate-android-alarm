#!/usr/bin/env python3
"""Generate BrightDate launcher icon PNGs at standard Android mipmap densities.

Run from anywhere:
    python3 scripts/gen_icon.py
"""
from PIL import Image, ImageDraw, ImageFont
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "brightdate-android-alarm" / "app" / "src" / "main" / "res"

# dp size of an adaptive-icon foreground/background canvas
DP = 108
# pixel sizes for each density bucket (= DP * scale)
DENSITIES = {
    "mdpi": 1.0,
    "hdpi": 1.5,
    "xhdpi": 2.0,
    "xxhdpi": 3.0,
    "xxxhdpi": 4.0,
}

LABEL = "BRIGHTDATE"
NUMBER = "9627.47168"

YELLOW = (255, 213, 0, 255)
GREY = (160, 160, 160, 255)
BLACK = (0, 0, 0, 255)

FONTS_DIR = ROOT / "scripts" / "fonts"
FONT_CANDIDATES_BOLD = [
    str(FONTS_DIR / "Roboto-Bold.ttf"),
    "/System/Library/Fonts/Helvetica.ttc",
    "/System/Library/Fonts/Supplemental/Arial Bold.ttf",
]
FONT_CANDIDATES_REGULAR = [
    str(FONTS_DIR / "Roboto-Regular.ttf"),
    "/System/Library/Fonts/HelveticaNeue.ttc",
    "/System/Library/Fonts/Helvetica.ttc",
    "/System/Library/Fonts/Supplemental/Arial.ttf",
]

def load_font(candidates, size, index_for_ttc=None):
    for path in candidates:
        try:
            if path.endswith(".ttc") and index_for_ttc is not None:
                return ImageFont.truetype(path, size, index=index_for_ttc)
            return ImageFont.truetype(path, size)
        except (OSError, IOError):
            continue
    return ImageFont.load_default()

def render_foreground(px):
    """Adaptive-icon foreground (transparent BG, content inside the safe zone)."""
    img = Image.new("RGBA", (px, px), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Safe zone: center 66dp of 108dp canvas (Material guideline)
    safe = int(px * 66 / 108)
    safe_origin = (px - safe) // 2

    label_font = load_font(FONT_CANDIDATES_REGULAR, max(8, int(safe * 0.11)), index_for_ttc=0)
    number_font = load_font(FONT_CANDIDATES_BOLD, max(10, int(safe * 0.21)), index_for_ttc=1)

    cx = px // 2

    tracking = max(1, int(safe * 0.012))
    label_width = sum(draw.textlength(ch, font=label_font) for ch in LABEL) + tracking * (len(LABEL) - 1)
    number_width = draw.textlength(NUMBER, font=number_font)

    label_bbox = label_font.getbbox(LABEL)
    label_h = label_bbox[3] - label_bbox[1]
    num_bbox = number_font.getbbox(NUMBER)
    num_h = num_bbox[3] - num_bbox[1]
    gap = int(safe * 0.04)
    total_h = label_h + gap + num_h
    top = safe_origin + (safe - total_h) // 2

    # Label with extra letter-spacing
    x = cx - label_width / 2
    y = top - label_bbox[1]
    for ch in LABEL:
        draw.text((x, y), ch, font=label_font, fill=GREY)
        x += draw.textlength(ch, font=label_font) + tracking

    # Number
    nx = cx - number_width / 2
    ny = top + label_h + gap - num_bbox[1]
    draw.text((nx, ny), NUMBER, font=number_font, fill=YELLOW)

    return img

def render_legacy(px):
    img = Image.new("RGBA", (px, px), BLACK)
    img.alpha_composite(render_foreground(px))
    return img

def main():
    for bucket, scale in DENSITIES.items():
        px = int(DP * scale)
        out_dir = RES / f"mipmap-{bucket}"
        out_dir.mkdir(exist_ok=True)

        render_foreground(px).save(out_dir / "ic_launcher_foreground.png", "PNG", optimize=True)

        legacy = render_legacy(px)
        legacy.save(out_dir / "ic_launcher.png", "PNG", optimize=True)

        round_img = Image.new("RGBA", (px, px), (0, 0, 0, 0))
        mask = Image.new("L", (px, px), 0)
        ImageDraw.Draw(mask).ellipse((0, 0, px - 1, px - 1), fill=255)
        round_img.paste(legacy, (0, 0), mask)
        round_img.save(out_dir / "ic_launcher_round.png", "PNG", optimize=True)

        print(f"  wrote mipmap-{bucket} ({px}px)")

    # Play Store / preview asset
    render_legacy(512).save(ROOT / "scripts" / "ic_launcher_512.png", "PNG")
    print(f"  preview written to scripts/ic_launcher_512.png")

if __name__ == "__main__":
    main()
