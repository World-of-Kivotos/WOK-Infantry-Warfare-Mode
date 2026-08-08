"""Build crisp 64px Minecraft item textures from the redrawn injector artwork."""

from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
SOURCE_DIR = ROOT / "art_sources" / "transparent_injector_icons"
OUTPUT_DIR = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "wok_trauma"
    / "textures"
    / "item"
)
ITEMS = ("propital", "etg_change", "morphine")


def build_item_texture(name: str) -> None:
    source = Image.open(SOURCE_DIR / f"{name}.png").convert("RGBA")
    source.putalpha(source.getchannel("A").point(lambda value: 255 if value >= 48 else 0))
    bounds = source.getchannel("A").getbbox()
    if bounds is None:
        raise ValueError(f"{name} has no visible pixels")

    cropped = source.crop(bounds)
    cropped.thumbnail((60, 60), Image.Resampling.NEAREST)
    output = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    position = ((64 - cropped.width) // 2, (64 - cropped.height) // 2)
    output.alpha_composite(cropped, position)
    output.save(OUTPUT_DIR / f"{name}.png", optimize=True)


def main() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    for item in ITEMS:
        build_item_texture(item)


if __name__ == "__main__":
    main()
