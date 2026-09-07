import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
GEO_DIR = ROOT / "src/main/resources/assets/wok_infantry_armor/geo"
IDENTIFIER = "maska_1sch_heavy"


def cube(origin, size, uv=(0, 0)):
    return {"origin": origin, "size": size, "uv": list(uv)}


def bone(name, cubes, pivot=(0, 30, 0), rotation=None):
    value = {
        "name": name,
        "parent": "armorHead",
        "pivot": list(pivot),
        "cubes": cubes,
    }
    if rotation is not None:
        value["rotation"] = list(rotation)
    return value


def build_bones():
    bones = [{"name": "armorHead", "pivot": [0, 24, 0]}]

    # Closely overlapping stepped volumes form a single heavy hemispherical
    # shell.  The overlap is intentional: it prevents daylight seams at the
    # diagonal corners while retaining the approved block-model language.
    bones.append(bone("maska_shell_crown", [
        cube([-1.35, 33.40, -1.45], [2.70, 0.34, 2.90], (0, 0)),
        cube([-2.30, 33.02, -2.35], [4.60, 0.58, 4.70], (8, 0)),
        cube([-3.10, 32.40, -3.05], [6.20, 0.82, 6.10], (20, 0)),
        cube([-3.70, 31.55, -3.58], [7.40, 1.06, 7.16], (36, 0)),
        cube([-4.12, 30.55, -3.92], [8.24, 1.22, 7.84], (0, 10)),
        cube([-4.38, 29.55, -4.05], [8.76, 1.24, 8.10], (18, 10)),
    ]))

    bones.append(bone("maska_shell_front_curve", [
        cube([-3.92, 30.35, -4.56], [7.84, 0.94, 0.72], (38, 10)),
        cube([-3.45, 31.18, -4.35], [6.90, 0.92, 0.66], (0, 20)),
        cube([-2.80, 31.98, -3.96], [5.60, 0.80, 0.60], (16, 20)),
        cube([-1.92, 32.65, -3.30], [3.84, 0.62, 0.54], (30, 20)),
    ], (0, 31.4, -4.0), (4, 0, 0)))

    bones.append(bone("maska_shell_left_curve", [
        cube([-4.72, 29.45, -3.65], [0.72, 1.36, 7.30], (42, 20)),
        cube([-4.55, 30.60, -3.38], [0.68, 1.26, 6.76], (50, 20)),
        cube([-4.25, 31.66, -2.88], [0.62, 1.06, 5.76], (42, 28)),
        cube([-3.80, 32.52, -2.18], [0.58, 0.76, 4.36], (50, 28)),
    ], (-4.05, 31.0, 0), (0, 0, -5)))
    bones.append(bone("maska_shell_right_curve", [
        cube([4.00, 29.45, -3.65], [0.72, 1.36, 7.30], (42, 20)),
        cube([3.87, 30.60, -3.38], [0.68, 1.26, 6.76], (50, 20)),
        cube([3.63, 31.66, -2.88], [0.62, 1.06, 5.76], (42, 28)),
        cube([3.22, 32.52, -2.18], [0.58, 0.76, 4.36], (50, 28)),
    ], (4.05, 31.0, 0), (0, 0, 5)))
    bones.append(bone("maska_shell_rear_curve", [
        cube([-4.10, 29.40, 3.70], [8.20, 1.42, 0.72], (0, 30)),
        cube([-3.72, 30.62, 3.54], [7.44, 1.28, 0.68], (18, 30)),
        cube([-3.18, 31.70, 3.20], [6.36, 1.08, 0.62], (36, 30)),
        cube([-2.40, 32.58, 2.62], [4.80, 0.72, 0.56], (50, 30)),
    ], (0, 31.0, 3.75), (-4, 0, 0)))

    # The reference does not have a uniform bucket-like lower cylinder.  The
    # dome finishes in a short rear skirt while two large carrier plates extend
    # down the temples and support the face shield.
    bones.append(bone("maska_shell_rear_skirt", [
        cube([-4.30, 26.10, 3.46], [8.60, 3.72, 0.88], (0, 38)),
        cube([-3.78, 25.48, 3.66], [7.56, 0.82, 0.70], (20, 38)),
        cube([-4.48, 26.08, 0.62], [0.76, 3.72, 3.18], (36, 38)),
        cube([3.72, 26.08, 0.62], [0.76, 3.72, 3.18], (42, 38)),
    ]))
    bones.append(bone("maska_left_carrier_plate", [
        cube([-4.92, 24.15, -4.36], [0.78, 5.92, 5.36], (0, 46)),
        cube([-4.82, 23.70, -3.82], [0.70, 0.62, 4.55], (8, 46)),
        cube([-4.80, 29.82, -3.82], [0.72, 0.48, 4.64], (16, 46)),
        cube([-4.98, 24.45, -4.56], [0.34, 5.18, 0.54], (24, 46)),
    ]))
    bones.append(bone("maska_right_carrier_plate", [
        cube([4.14, 24.15, -4.36], [0.78, 5.92, 5.36], (0, 46)),
        cube([4.12, 23.70, -3.82], [0.70, 0.62, 4.55], (8, 46)),
        cube([4.08, 29.82, -3.82], [0.72, 0.48, 4.64], (16, 46)),
        cube([4.64, 24.45, -4.56], [0.34, 5.18, 0.54], (24, 46)),
    ]))

    # Thick circumferential brow band visible behind the hinged face plate.
    bones.append(bone("maska_brow_band", [
        cube([-4.58, 28.82, -4.56], [9.16, 1.18, 0.68], (0, 56)),
        cube([-4.72, 28.72, -3.92], [0.70, 1.30, 7.70], (20, 56)),
        cube([4.02, 28.72, -3.92], [0.70, 1.30, 7.70], (30, 56)),
        cube([-4.16, 28.72, 3.60], [8.32, 1.30, 0.72], (40, 56)),
    ]))

    # The shield is a separate, thick steel plate.  Four corner bolts, the
    # broad horizontal slit and the rounded/angled lower edge follow the new
    # front reference.  The slit remains actual empty geometry.
    bones.append(bone("maska_face_plate_upper", [
        cube([-4.26, 28.36, -5.48], [8.52, 1.70, 0.82], (0, 0)),
        cube([-4.02, 29.90, -5.34], [8.04, 0.40, 0.70], (18, 0)),
        cube([-4.38, 28.18, -5.42], [0.58, 1.46, 0.76], (36, 0)),
        cube([3.80, 28.18, -5.42], [0.58, 1.46, 0.76], (40, 0)),
    ]))
    bones.append(bone("maska_face_plate_lower", [
        cube([-4.26, 26.10, -5.48], [8.52, 1.20, 0.82], (0, 8)),
        cube([-4.05, 25.10, -5.43], [8.10, 1.16, 0.80], (18, 8)),
        cube([-3.66, 24.42, -5.36], [7.32, 0.86, 0.76], (36, 8)),
        cube([-3.02, 23.94, -5.28], [6.04, 0.66, 0.70], (0, 14)),
        cube([-1.94, 23.66, -5.18], [3.88, 0.46, 0.62], (16, 14)),
        cube([-4.38, 26.22, -5.42], [0.58, 1.22, 0.76], (28, 14)),
        cube([3.80, 26.22, -5.42], [0.58, 1.22, 0.76], (32, 14)),
    ]))
    bones.append(bone("maska_face_plate_slit_bevels", [
        cube([-3.62, 28.23, -5.64], [7.24, 0.22, 0.38], (0, 20)),
        cube([-3.62, 27.22, -5.64], [7.24, 0.22, 0.38], (18, 20)),
        cube([-3.88, 27.40, -5.60], [0.30, 0.70, 0.36], (36, 20)),
        cube([3.58, 27.40, -5.60], [0.30, 0.70, 0.36], (40, 20)),
        cube([-3.76, 27.28, -5.60], [0.34, 0.22, 0.36], (44, 20)),
        cube([3.42, 27.28, -5.60], [0.34, 0.22, 0.36], (48, 20)),
    ]))
    bones.append(bone("maska_face_plate_returns", [
        cube([-4.42, 24.85, -5.12], [0.62, 4.70, 1.10], (0, 28)),
        cube([3.80, 24.85, -5.12], [0.62, 4.70, 1.10], (8, 28)),
        cube([-3.50, 23.72, -5.02], [7.00, 0.44, 0.90], (16, 28)),
    ]))

    # Four face-shield bolts and two centered brow rivets reproduce the front
    # image exactly.  Each bolt has a darker cap to remain readable in game.
    bones.append(bone("maska_hardware_face_bolts", [
        cube([-4.12, 29.12, -5.82], [0.40, 0.40, 0.30], (0, 32)),
        cube([3.72, 29.12, -5.82], [0.40, 0.40, 0.30], (4, 32)),
        cube([-4.12, 26.40, -5.82], [0.40, 0.40, 0.30], (8, 32)),
        cube([3.72, 26.40, -5.82], [0.40, 0.40, 0.30], (12, 32)),
        cube([-4.04, 29.20, -5.90], [0.24, 0.24, 0.16], (16, 32)),
        cube([3.80, 29.20, -5.90], [0.24, 0.24, 0.16], (20, 32)),
        cube([-4.04, 26.48, -5.90], [0.24, 0.24, 0.16], (24, 32)),
        cube([3.80, 26.48, -5.90], [0.24, 0.24, 0.16], (28, 32)),
    ]))
    bones.append(bone("maska_hardware_brow_rivets", [
        cube([-0.58, 29.36, -4.98], [0.32, 0.32, 0.28], (32, 32)),
        cube([0.26, 29.36, -4.98], [0.32, 0.32, 0.28], (36, 32)),
    ]))
    bones.append(bone("maska_hardware_hinges", [
        cube([-5.16, 26.44, -0.84], [0.42, 2.10, 1.12], (40, 32)),
        cube([4.74, 26.44, -0.84], [0.42, 2.10, 1.12], (46, 32)),
        cube([-5.30, 27.86, -0.58], [0.28, 0.56, 0.56], (52, 32)),
        cube([-5.30, 26.58, -0.58], [0.28, 0.56, 0.56], (56, 32)),
        cube([5.02, 27.86, -0.58], [0.28, 0.56, 0.56], (52, 32)),
        cube([5.02, 26.58, -0.58], [0.28, 0.56, 0.56], (56, 32)),
    ]))
    return bones


def main():
    document = {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": f"geometry.wok_infantry_armor.helmet_{IDENTIFIER}",
                "texture_width": 64,
                "texture_height": 64,
                "visible_bounds_width": 2.7,
                "visible_bounds_height": 3.2,
                "visible_bounds_offset": [0, 1.45, 0],
            },
            "bones": build_bones(),
        }],
    }
    GEO_DIR.mkdir(parents=True, exist_ok=True)
    target = GEO_DIR / f"helmet_{IDENTIFIER}.geo.json"
    target.write_text(json.dumps(document, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(target.resolve())


if __name__ == "__main__":
    main()
