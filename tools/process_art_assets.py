"""Build Minecraft-sized textures from the generated source artwork."""

from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter


ROOT = Path(__file__).resolve().parents[1]
TRANSPARENT_SOURCES = ROOT / "art_sources" / "transparent_effect_icons"
EFFECT_OUTPUT = ROOT / "src" / "main" / "resources" / "assets" / "wok_trauma" / "textures" / "mob_effect"
PARTICLE_OUTPUT = ROOT / "src" / "main" / "resources" / "assets" / "wok_trauma" / "textures" / "particle"
MISC_OUTPUT = ROOT / "src" / "main" / "resources" / "assets" / "wok_trauma" / "textures" / "misc"
PREVIEW_OUTPUT = ROOT / "art_sources" / "effect-icons-preview.png"


def build_effect_icon(name: str) -> None:
    source = Image.open(TRANSPARENT_SOURCES / f"{name}.png").convert("RGBA")
    sanitized = Image.new("RGBA", source.size, (0, 0, 0, 0))
    source_pixels = source.load()
    sanitized_pixels = sanitized.load()
    for y in range(source.height):
        for x in range(source.width):
            red, green, blue, alpha_value = source_pixels[x, y]
            green_key_residue = (
                name != "regeneration"
                and green > red * 1.12
                and green > blue * 1.08
            )
            if alpha_value < 64 or green_key_residue:
                continue
            if name == "tremor" and max(red, green, blue) < 70:
                red, green, blue = 94, 96, 108
            sanitized_pixels[x, y] = red, green, blue, 255
    source = sanitized

    alpha = source.getchannel("A").point(lambda value: 255 if value > 16 else 0)
    bounds = alpha.getbbox()
    if bounds is None:
        raise ValueError(f"{name} has no visible pixels")

    cropped = source.crop(bounds)
    cropped.thumbnail((16, 16), Image.Resampling.NEAREST)
    output = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    position = ((18 - cropped.width) // 2, (18 - cropped.height) // 2)
    output.alpha_composite(cropped, position)
    output.save(EFFECT_OUTPUT / f"{name}.png", optimize=True)


def build_particle_textures() -> None:
    spot = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(spot)
    draw.polygon(
        [(4, 15), (7, 10), (13, 8), (18, 10), (22, 8), (27, 12),
         (28, 18), (24, 23), (17, 24), (12, 22), (7, 24), (3, 20)],
        fill=(138, 4, 12, 218),
    )
    draw.rectangle((9, 12, 15, 15), fill=(194, 14, 22, 205))
    draw.rectangle((26, 25, 28, 27), fill=(110, 2, 8, 190))
    spot.save(PARTICLE_OUTPUT / "blood_spot.png", optimize=True)

    pool = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(pool)
    draw.polygon(
        [(3, 29), (8, 20), (17, 18), (20, 10), (30, 13), (36, 7),
         (43, 14), (53, 15), (56, 24), (62, 29), (58, 38), (61, 46),
         (51, 49), (45, 57), (34, 53), (24, 59), (18, 51), (8, 49),
         (10, 40), (2, 37)],
        fill=(111, 1, 9, 235),
    )
    draw.polygon(
        [(12, 27), (21, 18), (34, 17), (46, 21), (52, 32),
         (46, 42), (33, 47), (20, 43), (10, 36)],
        fill=(157, 5, 14, 222),
    )
    draw.polygon(
        [(19, 25), (27, 19), (39, 21), (46, 29), (41, 34), (30, 35), (22, 32)],
        fill=(205, 16, 24, 180),
    )
    pool.save(PARTICLE_OUTPUT / "blood_pool.png", optimize=True)


def build_pain_overlay() -> None:
    size = 256
    mask = Image.new("L", (size, size), 0)
    pixels = mask.load()
    center = (size - 1) / 2

    for y in range(size):
        for x in range(size):
            dx = abs(x - center) / center
            dy = abs(y - center) / center
            edge = max(dx, dy)
            radial = math.sqrt(dx * dx + dy * dy)
            strength = max((edge - 0.43) / 0.57, (radial - 0.72) / 0.70)
            pixels[x, y] = int(max(0.0, min(1.0, strength)) * 205)

    mask = mask.filter(ImageFilter.GaussianBlur(radius=7))
    overlay = Image.new("RGBA", (size, size), (78, 4, 17, 0))
    overlay.putalpha(mask)
    overlay.save(MISC_OUTPUT / "pain_blur.png", optimize=True)


def build_preview() -> None:
    scale = 12
    names = (
        "pain",
        "bleeding",
        "major_bleeding",
        "tremor",
        "concussion",
        "analgesia",
        "regeneration",
    )
    preview = Image.new("RGBA", (18 * scale * len(names), 18 * scale), (38, 38, 42, 255))
    for index, name in enumerate(names):
        icon = Image.open(EFFECT_OUTPUT / f"{name}.png").convert("RGBA")
        icon = icon.resize((18 * scale, 18 * scale), Image.Resampling.NEAREST)
        preview.alpha_composite(icon, (index * 18 * scale, 0))
    preview.save(PREVIEW_OUTPUT, optimize=True)


def main() -> None:
    EFFECT_OUTPUT.mkdir(parents=True, exist_ok=True)
    PARTICLE_OUTPUT.mkdir(parents=True, exist_ok=True)
    MISC_OUTPUT.mkdir(parents=True, exist_ok=True)

    for effect in (
        "pain",
        "bleeding",
        "major_bleeding",
        "tremor",
        "concussion",
        "analgesia",
        "regeneration",
    ):
        build_effect_icon(effect)
    propital_icon = Image.open(EFFECT_OUTPUT / "regeneration.png").convert("RGBA")
    propital_icon.save(EFFECT_OUTPUT / "propital_regeneration.png", optimize=True)
    build_particle_textures()
    build_pain_overlay()
    build_preview()


if __name__ == "__main__":
    main()
