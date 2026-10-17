import json
import shutil
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/wok_infantry_armor"
ICON_SOURCE = ROOT / "art/helmet_icon_previews/helmet_heavy_slit_visor_pixel_48_preview_v3.png"


def add_chinese_name():
    target = ASSETS / "lang/zh_cn.json"
    data = json.loads(target.read_text(encoding="utf-8"))
    data["item.wok_infantry_armor.helmet_maska_1sch_heavy"] = "Maska-1SCh 重型头盔"
    target.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def install_icon():
    target = ASSETS / "textures/item/helmet_maska_1sch_heavy.png"
    target.parent.mkdir(parents=True, exist_ok=True)
    icon = Image.open(ICON_SOURCE).convert("RGBA")
    if icon.size != (48, 48):
        icon = icon.resize((48, 48), Image.Resampling.NEAREST)
    icon.save(target)


def armor_texture():
    # GeckoLib box UVs require an opaque atlas.  Use restrained, intentionally
    # non-purple olive-metal swatches; hardware and slit bevel ranges receive
    # darker values while the shell remains consistent from every angle.
    image = Image.new("RGBA", (64, 64), (139, 143, 88, 255))
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, 63, 15), fill=(150, 154, 98, 255))
    draw.rectangle((0, 16, 63, 31), fill=(128, 132, 79, 255))
    draw.rectangle((0, 32, 63, 45), fill=(74, 75, 58, 255))
    draw.rectangle((0, 46, 63, 55), fill=(119, 123, 74, 255))
    draw.rectangle((0, 56, 63, 63), fill=(102, 106, 65, 255))
    # Low-frequency wear/edge highlights remain pixel-safe and opaque.
    for x, y in ((6, 4), (22, 9), (44, 5), (11, 20), (37, 27),
                 (4, 50), (28, 52), (55, 49), (16, 59), (48, 61)):
        draw.rectangle((x, y, x + 2, y), fill=(172, 174, 112, 255))
    for x, y in ((14, 7), (32, 13), (51, 22), (8, 38), (26, 41), (46, 36)):
        draw.point((x, y), fill=(80, 83, 53, 255))
    target = ASSETS / "textures/models/armor/helmet_maska_1sch_heavy_layer_1.png"
    target.parent.mkdir(parents=True, exist_ok=True)
    image.save(target)


def main():
    add_chinese_name()
    install_icon()
    armor_texture()
    print("Installed Maska-1SCh item icon, armor texture, and Chinese localization")


if __name__ == "__main__":
    main()
