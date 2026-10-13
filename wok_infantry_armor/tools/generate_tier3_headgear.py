import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
GEO_DIR = ROOT / "src/main/resources/assets/wok_infantry_armor/geo"


def cube(origin, size, uv=(0, 0)):
    return {"origin": origin, "size": size, "uv": list(uv)}


def bone(name, cubes=None, pivot=(0, 30, 0), rotation=None, parent="armorHead"):
    value = {"name": name, "parent": parent, "pivot": list(pivot)}
    if rotation is not None:
        value["rotation"] = list(rotation)
    if cubes:
        value["cubes"] = cubes
    return value


def document(identifier, bones, height=2.8):
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": f"geometry.wok_infantry_armor.helmet_{identifier}",
                "texture_width": 64,
                "texture_height": 64,
                "visible_bounds_width": 2.5,
                "visible_bounds_height": height,
                "visible_bounds_offset": [0, 1.5, 0],
            },
            "bones": bones,
        }],
    }


def dome(identifier, *, high_cut=False, heavy=False, mount=True, rails=True):
    radius = 4.28 if heavy else 4.12
    outer = radius + 0.48
    bones = [{"name": "armorHead", "pivot": [0, 24, 0]}]
    # The two lowest backing layers sit behind the vanilla face plane. They close
    # side seams without drawing a front surface across the player's eyes.
    bones.append(bone("seamless_backing", [
        cube([-radius, 28.95, -3.84], [radius * 2, 1.34, 7.68], (0, 0)),
        cube([-3.88, 30.02, -3.70], [7.76, 1.42, 7.40], (18, 0)),
        cube([-3.52, 31.18, -3.42], [7.04, 1.28, 6.84], (36, 0)),
        cube([-2.98, 32.18, -2.92], [5.96, 1.02, 5.84], (0, 18)),
        cube([-2.18, 32.94, -2.10], [4.36, 0.70, 4.20], (14, 18)),
        cube([-1.12, 33.42, -1.02], [2.24, 0.34, 2.04], (26, 18)),
    ]))
    bones.append(bone("front_curve", [
        cube([-4.20, 29.02, -4.62], [8.40, 0.82, 0.82], (34, 18)),
        cube([-3.90, 29.66, -4.52], [7.80, 1.08, 0.76], (0, 30)),
        cube([-3.50, 30.54, -4.34], [7.00, 1.18, 0.70], (18, 30)),
        cube([-2.96, 31.52, -4.00], [5.92, 1.06, 0.64], (36, 30)),
        cube([-2.22, 32.42, -3.46], [4.44, 0.80, 0.58], (50, 30)),
    ], (0, 30.7, -3.9), (6, 0, 0)))
    bones.append(bone("rear_curve", [
        cube([-4.08, 28.10, 3.72], [8.16, 1.66, 0.82], (0, 40)),
        cube([-3.84, 29.52, 3.62], [7.68, 1.40, 0.76], (18, 40)),
        cube([-3.42, 30.70, 3.44], [6.84, 1.28, 0.70], (36, 40)),
        cube([-2.84, 31.78, 3.12], [5.68, 1.04, 0.62], (50, 40)),
    ], (0, 30.2, 3.76), (-7, 0, 0)))
    bones.append(bone("left_curve", [
        cube([-outer, 28.50, -4.00], [0.78, 1.36, 8.00], (0, 48)),
        cube([-outer + 0.14, 29.66, -3.82], [0.74, 1.36, 7.64], (10, 48)),
        cube([-outer + 0.34, 30.82, -3.42], [0.68, 1.24, 6.84], (20, 48)),
        cube([-outer + 0.64, 31.86, -2.84], [0.60, 1.02, 5.68], (30, 48)),
    ], (-3.82, 30.4, 0), (0, 0, -7)))
    bones.append(bone("right_curve", [
        cube([outer - 0.78, 28.50, -4.00], [0.78, 1.36, 8.00], (0, 48)),
        cube([outer - 0.88, 29.66, -3.82], [0.74, 1.36, 7.64], (10, 48)),
        cube([outer - 1.02, 30.82, -3.42], [0.68, 1.24, 6.84], (20, 48)),
        cube([outer - 1.24, 31.86, -2.84], [0.60, 1.02, 5.68], (30, 48)),
    ], (3.82, 30.4, 0), (0, 0, 7)))
    lower_y = 27.75 if high_cut else 26.15
    lower_h = 1.45 if high_cut else 2.95
    bones.append(bone("lower_side_shells", [
        cube([-outer - 0.06, lower_y, -3.54], [0.72, lower_h, 7.10], (40, 48)),
        cube([outer - 0.66, lower_y, -3.54], [0.72, lower_h, 7.10], (40, 48)),
        cube([-outer - 0.12, 28.76, -3.86], [0.84, 0.42, 7.74], (52, 48)),
        cube([outer - 0.72, 28.76, -3.86], [0.84, 0.42, 7.74], (52, 48)),
    ]))
    bones.append(bone("rear_skirt", [
        cube([-3.86, 26.42 if not high_cut else 27.44, 3.68], [7.72, 2.42 if not high_cut else 1.40, 0.76], (0, 56)),
        cube([-3.22, 25.70 if not high_cut else 27.08, 3.76], [6.44, 0.94 if not high_cut else 0.58, 0.68], (18, 56)),
    ], (0, 28, 3.9), (-4, 0, 0)))
    bones.append(bone("continuous_rim", [
        cube([-4.56, 28.82, -4.70], [9.12, 0.48, 0.66], (0, 24)),
        cube([-outer - 0.12, lower_y - 0.18, -4.02], [0.46, lower_h + 0.42, 3.64], (20, 24)),
        cube([outer - 0.34, lower_y - 0.18, -4.02], [0.46, lower_h + 0.42, 3.64], (20, 24)),
        cube([-outer - 0.10, lower_y - 0.24, 0.36], [0.46, lower_h + 0.46, 3.76], (28, 24)),
        cube([outer - 0.36, lower_y - 0.24, 0.36], [0.46, lower_h + 0.46, 3.76], (28, 24)),
        cube([-3.54, (25.58 if not high_cut else 27.18), 3.86], [7.08, 0.48, 0.52], (36, 24)),
    ]))
    if mount:
        bones.append(bone("front_mount", [
            cube([-1.30, 29.52, -5.02], [2.60, 2.52, 0.44], (0, 32)),
            cube([-1.02, 29.82, -5.28], [2.04, 1.90, 0.32], (8, 32)),
            cube([-1.18, 31.88, -5.14], [2.36, 0.42, 0.36], (14, 32)),
            cube([-1.40, 29.46, -5.14], [0.30, 2.52, 0.30], (20, 32)),
            cube([1.10, 29.46, -5.14], [0.30, 2.52, 0.30], (20, 32)),
            cube([-0.22, 30.16, -5.48], [0.44, 1.06, 0.28], (24, 32)),
        ], (0, 30.76, -4.72), (4, 0, 0)))
    if rails:
        rail_x = outer + 0.18
        bones.append(bone("side_rails", [
            cube([-rail_x, 28.44, -1.32], [0.30, 0.72, 2.64], (34, 32)),
            cube([rail_x - 0.30, 28.44, -1.32], [0.30, 0.72, 2.64], (34, 32)),
            cube([-rail_x - 0.14, 28.56, -1.04], [0.18, 0.28, 2.08], (42, 32)),
            cube([rail_x - 0.04, 28.56, -1.04], [0.18, 0.28, 2.08], (42, 32)),
        ]))
    bones.append(bone("shell_fasteners", [
        cube([-outer - 0.08, 29.10, -2.46], [0.22, 0.30, 0.30], (52, 32)),
        cube([outer - 0.14, 29.10, -2.46], [0.22, 0.30, 0.30], (52, 32)),
        cube([-outer - 0.08, 28.66, 2.08], [0.22, 0.30, 0.30], (52, 32)),
        cube([outer - 0.14, 28.66, 2.08], [0.22, 0.30, 0.30], (52, 32)),
    ]))
    return bones


def cqcm():
    bones = [{"name": "armorHead", "pivot": [0, 24, 0]}]
    bones.append(bone("mask_forehead", [
        cube([-4.02, 29.42, -4.60], [8.04, 2.10, 0.62], (0, 0)),
        cube([-3.54, 31.30, -4.40], [7.08, 0.76, 0.58], (18, 0)),
    ], (0, 30.2, -4.2), (5, 0, 0)))
    bones.append(bone("eye_frame", [
        cube([-4.18, 27.30, -4.66], [0.86, 2.34, 0.68], (0, 16)),
        cube([3.32, 27.30, -4.66], [0.86, 2.34, 0.68], (0, 16)),
        cube([-3.34, 29.12, -4.70], [2.82, 0.52, 0.66], (8, 16)),
        cube([0.52, 29.12, -4.70], [2.82, 0.52, 0.66], (8, 16)),
        cube([-3.34, 27.18, -4.70], [2.82, 0.48, 0.66], (16, 16)),
        cube([0.52, 27.18, -4.70], [2.82, 0.48, 0.66], (16, 16)),
        cube([-0.52, 27.24, -4.78], [1.04, 2.30, 0.72], (24, 16)),
    ]))
    bones.append(bone("mask_cheeks", [
        cube([-4.02, 24.72, -4.58], [3.52, 2.70, 0.68], (0, 28)),
        cube([0.50, 24.72, -4.58], [3.52, 2.70, 0.68], (10, 28)),
        cube([-3.64, 23.20, -4.46], [7.28, 1.82, 0.72], (20, 28)),
        cube([-2.82, 22.22, -4.30], [5.64, 1.24, 0.72], (36, 28)),
        cube([-1.02, 25.12, -4.82], [2.04, 2.22, 0.78], (50, 28)),
    ], (0, 25.5, -4.1), (-3, 0, 0)))
    bones.append(bone("mask_side_wrap", [
        cube([-4.62, 24.60, -3.90], [0.74, 4.96, 3.36], (0, 40)),
        cube([3.88, 24.60, -3.90], [0.74, 4.96, 3.36], (8, 40)),
        cube([-4.50, 22.92, -3.18], [0.64, 2.06, 2.40], (16, 40)),
        cube([3.86, 22.92, -3.18], [0.64, 2.06, 2.40], (22, 40)),
    ]))
    bones.append(bone("mask_vents", [
        cube([-1.54, 23.56, -4.86], [0.28, 0.28, 0.20], (30, 40)),
        cube([-0.86, 23.42, -4.90], [0.28, 0.28, 0.20], (30, 40)),
        cube([-0.18, 23.36, -4.92], [0.36, 0.28, 0.20], (30, 40)),
        cube([0.58, 23.42, -4.90], [0.28, 0.28, 0.20], (30, 40)),
        cube([1.26, 23.56, -4.86], [0.28, 0.28, 0.20], (30, 40)),
    ]))
    return bones


def lightweight_mask():
    bones = [{"name": "armorHead", "pivot": [0, 24, 0]}]
    bones.append(bone("mask_brow", [
        cube([-3.72, 29.18, -4.56], [7.44, 1.90, 0.58], (0, 0)),
        cube([-3.20, 30.86, -4.34], [6.40, 0.72, 0.54], (16, 0)),
    ], (0, 30.0, -4.2), (5, 0, 0)))
    bones.append(bone("mask_eye_frame", [
        cube([-3.86, 27.32, -4.66], [0.72, 2.08, 0.64], (0, 16)),
        cube([3.14, 27.32, -4.66], [0.72, 2.08, 0.64], (0, 16)),
        cube([-3.18, 29.02, -4.70], [2.62, 0.48, 0.64], (8, 16)),
        cube([0.56, 29.02, -4.70], [2.62, 0.48, 0.64], (8, 16)),
        cube([-3.18, 27.18, -4.70], [2.62, 0.44, 0.64], (16, 16)),
        cube([0.56, 27.18, -4.70], [2.62, 0.44, 0.64], (16, 16)),
        cube([-0.50, 27.26, -4.78], [1.00, 2.12, 0.70], (24, 16)),
    ]))
    bones.append(bone("skull_cheeks", [
        cube([-3.74, 24.60, -4.56], [3.24, 2.82, 0.66], (0, 28)),
        cube([0.50, 24.60, -4.56], [3.24, 2.82, 0.66], (10, 28)),
        cube([-2.96, 23.12, -4.44], [5.92, 1.72, 0.70], (20, 28)),
        cube([-2.46, 22.24, -4.30], [4.92, 1.10, 0.66], (34, 28)),
        cube([-0.92, 25.02, -4.86], [1.84, 2.40, 0.78], (46, 28)),
    ], (0, 25.5, -4.1), (-3, 0, 0)))
    teeth = []
    for index in range(7):
        teeth.append(cube([-1.75 + index * 0.50, 23.42, -4.88], [0.32, 0.58, 0.20], (54, 28)))
    bones.append(bone("skull_teeth", teeth))
    bones.append(bone("mask_side_wrap", [
        cube([-4.32, 24.42, -3.76], [0.62, 4.82, 3.12], (0, 40)),
        cube([3.70, 24.42, -3.76], [0.62, 4.82, 3.12], (8, 40)),
    ]))
    return bones


def add_riot_details(bones):
    bones.append(bone("riot_side_hardware", [
        cube([-4.86, 28.24, -1.48], [0.34, 1.02, 2.30], (0, 56)),
        cube([4.52, 28.24, -1.48], [0.34, 1.02, 2.30], (6, 56)),
        cube([4.78, 28.48, -0.96], [0.22, 0.48, 0.84], (12, 56)),
        cube([4.78, 28.48, 0.18], [0.22, 0.48, 0.84], (12, 56)),
        cube([-4.98, 27.52, 1.28], [0.24, 0.32, 0.82], (18, 56)),
        cube([4.74, 27.52, 1.28], [0.24, 0.32, 0.82], (18, 56)),
    ]))


def add_strike_details(bones):
    bones.append(bone("strike_velcro", [
        cube([-3.18, 32.22, -3.58], [2.42, 0.22, 1.30], (0, 56)),
        cube([0.76, 32.22, -3.58], [2.42, 0.22, 1.30], (6, 56)),
        cube([-2.58, 32.66, 2.18], [2.02, 0.22, 1.18], (12, 56)),
        cube([0.56, 32.66, 2.18], [2.02, 0.22, 1.18], (18, 56)),
    ]))


def add_fast_details(bones):
    bones.append(bone("fast_brow_cable", [
        cube([-3.70, 29.18, -4.86], [7.40, 0.20, 0.22], (24, 56)),
        cube([-3.88, 29.06, -4.78], [0.22, 0.42, 0.24], (42, 56)),
        cube([3.66, 29.06, -4.78], [0.22, 0.42, 0.24], (42, 56)),
    ]))


def add_airframe_details(bones):
    bones.append(bone("airframe_overlap_spine", [
        cube([-0.26, 29.18, -4.68], [0.52, 4.10, 0.26], (0, 56)),
        cube([-0.26, 29.18, 3.92], [0.52, 4.10, 0.26], (4, 56)),
        cube([-2.34, 33.30, -0.22], [4.68, 0.28, 0.44], (8, 56)),
        cube([-4.76, 30.18, -0.24], [0.28, 1.32, 0.48], (18, 56)),
        cube([4.48, 30.18, -0.24], [0.28, 1.32, 0.48], (18, 56)),
    ]))


def add_flux_details(bones):
    bones.append(bone("flux_panels", [
        cube([-2.92, 32.26, -3.54], [2.20, 0.24, 1.28], (0, 56)),
        cube([0.72, 32.26, -3.54], [2.20, 0.24, 1.28], (6, 56)),
        cube([-4.82, 29.74, -2.16], [0.26, 0.76, 1.46], (12, 56)),
        cube([4.56, 29.74, -2.16], [0.26, 0.76, 1.46], (12, 56)),
        cube([-4.82, 29.74, 0.70], [0.26, 0.76, 1.46], (18, 56)),
        cube([4.56, 29.74, 0.70], [0.26, 0.76, 1.46], (18, 56)),
    ]))


def add_vulkan_details(bones):
    bones.append(bone("vulkan_reinforced_band", [
        cube([-4.72, 29.44, -4.86], [9.44, 0.92, 0.84], (0, 56)),
        cube([-4.84, 29.36, -4.02], [0.84, 1.08, 8.04], (20, 56)),
        cube([4.00, 29.36, -4.02], [0.84, 1.08, 8.04], (20, 56)),
        cube([-4.24, 29.42, 3.78], [8.48, 0.94, 0.76], (38, 56)),
        cube([-3.42, 30.22, -4.64], [6.84, 0.46, 0.54], (54, 56)),
    ]))
    bones.append(bone("vulkan_ear_armor", [
        cube([-5.18, 25.18, -2.86], [1.12, 4.58, 5.92], (0, 44)),
        cube([4.06, 25.18, -2.86], [1.12, 4.58, 5.92], (14, 44)),
        cube([-5.34, 26.02, -1.86], [0.34, 2.92, 3.72], (28, 44)),
        cube([5.00, 26.02, -1.86], [0.34, 2.92, 3.72], (36, 44)),
        cube([-4.84, 24.72, 1.32], [0.74, 1.12, 2.12], (44, 44)),
        cube([4.10, 24.72, 1.32], [0.74, 1.12, 2.12], (50, 44)),
    ]))
    bones.append(bone("vulkan_visor_frame", [
        cube([-4.54, 29.02, -5.18], [9.08, 0.62, 0.48], (0, 32)),
        cube([-4.64, 25.30, -5.12], [0.72, 4.18, 0.46], (20, 32)),
        cube([3.92, 25.30, -5.12], [0.72, 4.18, 0.46], (20, 32)),
        cube([-3.90, 24.94, -5.08], [7.80, 0.48, 0.42], (36, 32)),
        cube([-4.10, 28.62, -5.32], [0.62, 0.62, 0.44], (52, 32)),
        cube([3.48, 28.62, -5.32], [0.62, 0.62, 0.44], (52, 32)),
    ]))
    bones.append(bone("vulkan_visor_glass", [
        cube([-3.90, 25.42, -5.28], [7.80, 3.50, 0.10], (0, 0)),
    ]))
    bones.append(bone("vulkan_hinges", [
        cube([-5.42, 28.20, -2.66], [0.42, 0.92, 1.18], (0, 52)),
        cube([5.00, 28.20, -2.66], [0.42, 0.92, 1.18], (8, 52)),
        cube([-5.52, 28.42, -2.36], [0.24, 0.48, 0.48], (16, 52)),
        cube([5.28, 28.42, -2.36], [0.24, 0.48, 0.48], (16, 52)),
        cube([-5.24, 28.46, 1.64], [0.32, 0.52, 0.82], (22, 52)),
        cube([4.92, 28.46, 1.64], [0.32, 0.52, 0.82], (22, 52)),
    ]))


def add_altyn_details(bones):
    # Altyn is a full shell rather than a high-cut helmet.  The lower apron is
    # deliberately built from overlapping steps so the front, side and rear
    # views read as one thick pressed-steel piece instead of separate plates.
    bones.append(bone("altyn_shell_apron", [
        cube([-4.18, 24.28, -4.62], [8.36, 1.10, 0.72], (0, 44)),
        cube([-4.54, 24.72, -4.48], [0.74, 3.18, 1.42], (18, 44)),
        cube([3.80, 24.72, -4.48], [0.74, 3.18, 1.42], (26, 44)),
        cube([-4.82, 24.10, -3.56], [0.92, 3.28, 3.06], (34, 44)),
        cube([3.90, 24.10, -3.56], [0.92, 3.28, 3.06], (44, 44)),
        cube([-4.76, 24.18, -0.70], [0.82, 2.92, 4.20], (0, 52)),
        cube([3.94, 24.18, -0.70], [0.82, 2.92, 4.20], (10, 52)),
        cube([-3.86, 23.96, 3.42], [7.72, 1.26, 0.82], (20, 52)),
        cube([-3.34, 23.58, 3.54], [6.68, 0.62, 0.66], (38, 52)),
    ]))
    bones.append(bone("altyn_shell_edge_binding", [
        cube([-4.28, 24.08, -4.78], [8.56, 0.32, 0.36], (0, 60)),
        cube([-4.98, 23.90, -3.62], [0.28, 3.50, 3.16], (18, 60)),
        cube([4.70, 23.90, -3.62], [0.28, 3.50, 3.16], (24, 60)),
        cube([-4.92, 23.88, -0.62], [0.28, 3.20, 4.26], (30, 60)),
        cube([4.64, 23.88, -0.62], [0.28, 3.20, 4.26], (36, 60)),
        cube([-3.92, 23.68, 3.62], [7.84, 0.30, 0.34], (42, 60)),
    ]))
    bones.append(bone("altyn_opaque_left_side_shell", [
        cube([-5.08, 27.46, -3.90], [0.62, 1.52, 7.42], (0, 0)),
        cube([-5.02, 25.88, -3.54], [0.66, 1.76, 6.94], (10, 0)),
        cube([-4.92, 24.52, -2.90], [0.72, 1.58, 6.12], (20, 0)),
        cube([-4.76, 23.94, -1.82], [0.70, 0.80, 4.72], (30, 0)),
    ]))
    bones.append(bone("altyn_opaque_right_side_shell", [
        cube([4.46, 27.46, -3.90], [0.62, 1.52, 7.42], (0, 0)),
        cube([4.36, 25.88, -3.54], [0.66, 1.76, 6.94], (10, 0)),
        cube([4.20, 24.52, -2.90], [0.72, 1.58, 6.12], (20, 0)),
        cube([4.06, 23.94, -1.82], [0.70, 0.80, 4.72], (30, 0)),
    ]))

    # The real Altyn visor is horizontally convex.  The central third projects
    # furthest forward while the left and right thirds sweep back toward the
    # hinge arms.  Separate rotated bones preserve that curve in game instead
    # of representing the armored glass as one flat Minecraft plane.
    bones.append(bone("altyn_visor_carrier", [
        cube([-5.10, 28.82, -4.74], [10.20, 0.66, 0.60], (0, 32)),
        cube([-5.18, 26.02, -4.62], [0.72, 3.18, 0.64], (22, 32)),
        cube([4.46, 26.02, -4.62], [0.72, 3.18, 0.64], (30, 32)),
        cube([-5.06, 25.46, -4.54], [1.18, 0.74, 0.62], (38, 32)),
        cube([3.88, 25.46, -4.54], [1.18, 0.74, 0.62], (46, 32)),
    ]))
    bones.append(bone("altyn_visor_frame_center", [
        cube([-1.52, 28.72, -5.86], [3.04, 0.72, 0.78], (0, 34)),
        cube([-1.48, 24.70, -5.84], [2.96, 0.74, 0.78], (8, 34)),
    ]))
    bones.append(bone("altyn_visor_frame_left_curve", [
        cube([-4.66, 28.72, -5.70], [3.24, 0.72, 0.78], (16, 34)),
        cube([-4.62, 24.70, -5.68], [3.20, 0.74, 0.78], (26, 34)),
    ], (-1.42, 27.08, -5.70), (0, 10, 0)))
    bones.append(bone("altyn_visor_frame_right_curve", [
        cube([1.42, 28.72, -5.70], [3.24, 0.72, 0.78], (16, 34)),
        cube([1.42, 24.70, -5.68], [3.20, 0.74, 0.78], (26, 34)),
    ], (1.42, 27.08, -5.70), (0, -10, 0)))
    bones.append(bone("altyn_visor_frame_side_posts", [
        cube([-4.92, 25.30, -5.20], [0.82, 3.62, 0.78], (36, 34)),
        cube([4.10, 25.30, -5.20], [0.82, 3.62, 0.78], (46, 34)),
        cube([-4.72, 28.48, -5.34], [0.84, 0.54, 0.72], (56, 34)),
        cube([3.88, 28.48, -5.34], [0.84, 0.54, 0.72], (56, 38)),
        cube([-4.70, 25.12, -5.30], [0.88, 0.54, 0.72], (0, 40)),
        cube([3.82, 25.12, -5.30], [0.88, 0.54, 0.72], (8, 40)),
    ]))
    # Thick side returns connect the convex visor to the carrier arms.  The
    # earlier curved pane stopped at the front posts and left a visible draft
    # gap from oblique/side angles.  These opaque frame and gasket layers sit
    # behind the glass edge and overlap both the post and hinge carrier.
    bones.append(bone("altyn_visor_left_side_return", [
        cube([-5.24, 25.38, -5.02], [0.58, 3.48, 1.12], (20, 40)),
        cube([-5.18, 25.58, -4.02], [0.52, 3.08, 1.06], (28, 40)),
        cube([-5.12, 25.82, -3.08], [0.46, 2.60, 0.82], (36, 40)),
        cube([-5.08, 28.54, -4.86], [0.46, 0.42, 2.66], (44, 40)),
        cube([-5.06, 25.18, -4.84], [0.44, 0.42, 2.58], (52, 40)),
    ], (-4.76, 27.10, -4.18), (0, 5, 0)))
    bones.append(bone("altyn_visor_right_side_return", [
        cube([4.66, 25.38, -5.02], [0.58, 3.48, 1.12], (20, 40)),
        cube([4.66, 25.58, -4.02], [0.52, 3.08, 1.06], (28, 40)),
        cube([4.66, 25.82, -3.08], [0.46, 2.60, 0.82], (36, 40)),
        cube([4.62, 28.54, -4.86], [0.46, 0.42, 2.66], (44, 40)),
        cube([4.62, 25.18, -4.84], [0.44, 0.42, 2.58], (52, 40)),
    ], (4.76, 27.10, -4.18), (0, -5, 0)))
    bones.append(bone("altyn_visor_side_gaskets", [
        cube([-4.74, 25.72, -5.44], [0.24, 2.82, 1.58], (0, 46)),
        cube([4.50, 25.72, -5.44], [0.24, 2.82, 1.58], (6, 46)),
        cube([-4.68, 25.46, -4.10], [0.22, 0.34, 1.20], (12, 46)),
        cube([4.46, 25.46, -4.10], [0.22, 0.34, 1.20], (18, 46)),
        cube([-4.68, 28.48, -4.10], [0.22, 0.34, 1.20], (24, 46)),
        cube([4.46, 28.48, -4.10], [0.22, 0.34, 1.20], (30, 46)),
    ]))
    bones.append(bone("altyn_visor_bezel_center", [
        cube([-1.46, 28.32, -6.10], [2.92, 0.30, 0.30], (18, 40)),
        cube([-1.44, 25.50, -6.08], [2.88, 0.30, 0.30], (26, 40)),
    ]))
    bones.append(bone("altyn_visor_bezel_left_curve", [
        cube([-4.10, 28.32, -5.94], [2.70, 0.30, 0.30], (34, 40)),
        cube([-4.06, 25.50, -5.92], [2.66, 0.30, 0.30], (42, 40)),
    ], (-1.40, 27.06, -5.94), (0, 10, 0)))
    bones.append(bone("altyn_visor_bezel_right_curve", [
        cube([1.40, 28.32, -5.94], [2.70, 0.30, 0.30], (34, 40)),
        cube([1.40, 25.50, -5.92], [2.66, 0.30, 0.30], (42, 40)),
    ], (1.40, 27.06, -5.94), (0, -10, 0)))
    bones.append(bone("altyn_visor_glass_center", [
        cube([-1.44, 25.80, -6.04], [2.88, 2.52, 0.10], (0, 32)),
    ]))
    bones.append(bone("altyn_visor_glass_left", [
        cube([-3.82, 25.80, -5.88], [2.42, 2.52, 0.10], (8, 32)),
    ], (-1.40, 27.06, -5.88), (0, 10, 0)))
    bones.append(bone("altyn_visor_glass_right", [
        cube([1.40, 25.80, -5.88], [2.42, 2.52, 0.10], (16, 32)),
    ], (1.40, 27.06, -5.88), (0, -10, 0)))
    # A continuous recessed glazing bed sits behind all three curved panes.
    # Its edges extend underneath the inner bezel, so fractional gaps caused
    # by rotating the left/right pane bones can never expose the background.
    bones.append(bone("altyn_visor_continuous_glazing_bed", [
        cube([-4.24, 25.62, -5.48], [8.48, 2.88, 0.30], (0, 32)),
        cube([-4.42, 25.48, -5.42], [0.44, 3.16, 0.34], (18, 32)),
        cube([3.98, 25.48, -5.42], [0.44, 3.16, 0.34], (24, 32)),
        cube([-4.18, 28.42, -5.44], [8.36, 0.36, 0.34], (30, 32)),
        cube([-4.18, 25.34, -5.42], [8.36, 0.36, 0.34], (48, 32)),
    ]))
    bones.append(bone("altyn_visor_glass_edge_overlap", [
        cube([-4.08, 28.22, -5.98], [2.76, 0.28, 0.24], (0, 38)),
        cube([-1.50, 28.34, -6.14], [3.00, 0.28, 0.24], (8, 38)),
        cube([1.32, 28.22, -5.98], [2.76, 0.28, 0.24], (16, 38)),
        cube([-4.08, 25.62, -5.96], [2.76, 0.28, 0.24], (24, 38)),
        cube([-1.50, 25.54, -6.12], [3.00, 0.28, 0.24], (32, 38)),
        cube([1.32, 25.62, -5.96], [2.76, 0.28, 0.24], (40, 38)),
        cube([-4.18, 25.70, -5.82], [0.30, 2.72, 0.28], (48, 38)),
        cube([3.88, 25.70, -5.82], [0.30, 2.72, 0.28], (54, 38)),
    ]))

    bones.append(bone("altyn_left_visor_arm", [
        cube([-5.36, 27.82, -4.28], [0.34, 1.18, 2.18], (0, 48)),
        cube([-5.38, 27.52, -2.30], [0.36, 1.34, 1.30], (6, 48)),
        cube([-5.34, 27.26, -1.20], [0.36, 1.34, 1.08], (12, 48)),
    ], (-5.14, 28.10, -2.40), (0, 0, -3)))
    bones.append(bone("altyn_right_visor_arm", [
        cube([5.02, 27.82, -4.28], [0.34, 1.18, 2.18], (0, 48)),
        cube([5.02, 27.52, -2.30], [0.36, 1.34, 1.30], (6, 48)),
        cube([4.98, 27.26, -1.20], [0.36, 1.34, 1.08], (12, 48)),
    ], (5.14, 28.10, -2.40), (0, 0, 3)))
    bones.append(bone("altyn_pivot_housings", [
        cube([-5.34, 26.72, -0.92], [0.60, 1.78, 1.54], (20, 48)),
        cube([4.74, 26.72, -0.92], [0.60, 1.78, 1.54], (28, 48)),
        cube([-5.54, 27.14, -0.60], [0.24, 0.86, 0.86], (36, 48)),
        cube([5.30, 27.14, -0.60], [0.24, 0.86, 0.86], (42, 48)),
        cube([-5.46, 27.34, -0.40], [0.18, 0.46, 0.46], (48, 48)),
        cube([5.28, 27.34, -0.40], [0.18, 0.46, 0.46], (52, 48)),
    ]))
    bones.append(bone("altyn_side_boxes", [
        cube([-5.18, 25.26, 0.72], [0.64, 1.72, 1.42], (0, 54)),
        cube([4.54, 25.26, 0.72], [0.64, 1.72, 1.42], (8, 54)),
        cube([-5.32, 25.62, 0.92], [0.20, 0.98, 0.98], (16, 54)),
        cube([5.12, 25.62, 0.92], [0.20, 0.98, 0.98], (22, 54)),
    ]))
    bones.append(bone("altyn_rear_comms", [
        cube([-1.12, 25.16, 4.58], [2.24, 1.76, 0.72], (28, 54)),
        cube([-0.90, 24.84, 4.74], [1.80, 0.42, 0.46], (38, 54)),
        cube([-0.72, 26.86, 4.66], [1.44, 0.30, 0.42], (44, 54)),
        cube([1.52, 25.30, 4.62], [0.38, 0.62, 0.78], (48, 54)),
        cube([1.84, 25.44, 4.68], [1.42, 0.26, 0.30], (52, 54)),
        cube([3.18, 25.30, 4.62], [0.40, 0.54, 0.72], (58, 54)),
    ]))
    bones.append(bone("altyn_rear_cable", [
        cube([1.74, 25.52, 4.98], [0.48, 0.28, 0.26], (0, 62)),
        cube([2.12, 25.44, 4.98], [0.74, 0.24, 0.24], (4, 62)),
        cube([2.76, 25.30, 4.96], [0.72, 0.24, 0.24], (8, 62)),
        cube([3.34, 25.16, 4.88], [0.28, 0.40, 0.30], (12, 62)),
    ]))
    bones.append(bone("altyn_wide_side_carriers", [
        cube([-5.28, 27.84, -4.48], [0.46, 1.02, 2.64], (16, 62)),
        cube([-5.22, 27.48, -2.00], [0.44, 1.16, 1.58], (22, 62)),
        cube([-5.16, 26.98, -0.58], [0.42, 1.48, 1.16], (28, 62)),
        cube([4.82, 27.84, -4.48], [0.46, 1.02, 2.64], (16, 62)),
        cube([4.78, 27.48, -2.00], [0.44, 1.16, 1.58], (22, 62)),
        cube([4.74, 26.98, -0.58], [0.42, 1.48, 1.16], (28, 62)),
    ]))
    bones.append(bone("altyn_visible_fasteners", [
        cube([-5.54, 27.42, -0.34], [0.20, 0.34, 0.34], (34, 62)),
        cube([5.34, 27.42, -0.34], [0.20, 0.34, 0.34], (38, 62)),
        cube([-5.12, 25.02, 1.34], [0.22, 0.30, 0.30], (42, 62)),
        cube([4.90, 25.02, 1.34], [0.22, 0.30, 0.30], (46, 62)),
        cube([-0.14, 24.66, 5.30], [0.28, 0.28, 0.16], (50, 62)),
    ]))
    bones.append(bone("altyn_crown_lug", [
        cube([-0.42, 33.62, -0.38], [0.84, 0.30, 0.76], (0, 62)),
        cube([-0.30, 33.88, -0.26], [0.60, 0.34, 0.52], (4, 62)),
    ]))


def add_fast_heavy_kit_details(bones):
    # Velocity Systems SLAAP rifle plate.  It is one horseshoe-shaped shell:
    # two front arms leave a deep opening for the NVG shroud, join over the
    # crown, then continue into the broad rear arc visible in the supplied
    # three-view reference.  It is not the later two-piece Ops-Core LPBA.
    bones.append(bone("fast_slaap_left_arm", [
        cube([-4.24, 29.70, -4.90], [2.66, 0.72, 0.72], (0, 0)),
        cube([-4.08, 30.34, -4.68], [2.48, 0.82, 1.28], (8, 0)),
        cube([-3.84, 31.08, -4.34], [2.24, 0.88, 2.02], (16, 0)),
        cube([-3.48, 31.86, -3.84], [1.92, 0.86, 2.72], (24, 0)),
        cube([-3.00, 32.58, -3.18], [1.46, 0.68, 3.62], (32, 0)),
        cube([-2.48, 33.10, -2.24], [0.92, 0.40, 4.34], (40, 0)),
    ]))
    bones.append(bone("fast_slaap_right_arm", [
        cube([1.58, 29.70, -4.90], [2.66, 0.72, 0.72], (0, 0)),
        cube([1.60, 30.34, -4.68], [2.48, 0.82, 1.28], (8, 0)),
        cube([1.60, 31.08, -4.34], [2.24, 0.88, 2.02], (16, 0)),
        cube([1.56, 31.86, -3.84], [1.92, 0.86, 2.72], (24, 0)),
        cube([1.54, 32.58, -3.18], [1.46, 0.68, 3.62], (32, 0)),
        cube([1.56, 33.10, -2.24], [0.92, 0.40, 4.34], (40, 0)),
    ]))
    bones.append(bone("fast_slaap_crown_bridge", [
        cube([-1.62, 31.62, -4.04], [3.24, 0.74, 2.08], (42, 0)),
        cube([-1.60, 32.28, -3.54], [3.20, 0.72, 2.84], (48, 0)),
        cube([-2.48, 33.10, -2.24], [4.96, 0.42, 4.56], (48, 0)),
        cube([-3.00, 32.58, 0.42], [6.00, 0.52, 2.28], (0, 8)),
    ]))
    bones.append(bone("fast_slaap_rear_arc", [
        cube([-4.06, 29.54, 3.90], [8.12, 0.70, 0.62], (48, 0)),
        cube([-3.78, 30.18, 3.82], [7.56, 0.82, 0.60], (0, 8)),
        cube([-3.40, 30.94, 3.62], [6.80, 0.86, 0.58], (16, 8)),
        cube([-2.92, 31.74, 3.28], [5.84, 0.82, 0.54], (32, 8)),
        cube([-2.30, 32.48, 2.72], [4.60, 0.68, 0.50], (46, 8)),
    ]))
    bones.append(bone("fast_slaap_edge_binding", [
        cube([-4.34, 29.48, -5.02], [2.86, 0.24, 0.28], (0, 14)),
        cube([1.48, 29.48, -5.02], [2.86, 0.24, 0.28], (8, 14)),
        cube([-4.16, 29.34, 4.06], [8.32, 0.24, 0.28], (16, 14)),
        cube([-4.46, 29.24, 2.92], [0.26, 0.44, 1.18], (30, 14)),
        cube([4.20, 29.24, 2.92], [0.26, 0.44, 1.18], (36, 14)),
        cube([-2.56, 33.50, -2.32], [5.12, 0.22, 4.72], (42, 14)),
    ]))
    bones.append(bone("fast_heavy_exposed_nvg_cage", [
        cube([-1.42, 29.64, -5.34], [0.30, 2.44, 0.30], (0, 20)),
        cube([1.12, 29.64, -5.34], [0.30, 2.44, 0.30], (4, 20)),
        cube([-1.42, 31.86, -5.34], [2.84, 0.30, 0.30], (8, 20)),
        cube([-1.14, 29.46, -5.42], [2.28, 0.34, 0.32], (16, 20)),
        cube([-0.26, 30.10, -5.56], [0.52, 1.10, 0.26], (24, 20)),
    ]))
    # Rear anchors and rail adapters are visible through the gap between shell
    # and face kit.  Keeping them separate gives the side view real depth.
    bones.append(bone("fast_shc_arc_shims", [
        cube([-5.12, 27.36, -2.14], [0.50, 1.22, 1.72], (0, 40)),
        cube([4.62, 27.36, -2.14], [0.50, 1.22, 1.72], (8, 40)),
        cube([-5.30, 27.58, -1.70], [0.22, 0.68, 0.86], (16, 40)),
        cube([5.08, 27.58, -1.70], [0.22, 0.68, 0.86], (22, 40)),
        cube([-5.02, 26.56, 0.58], [0.44, 1.10, 1.28], (28, 40)),
        cube([4.58, 26.56, 0.58], [0.44, 1.10, 1.28], (34, 40)),
    ]))
    bones.append(bone("fast_mandible_rear_edges", [
        cube([-4.84, 23.88, 1.20], [0.86, 3.20, 2.30], (40, 40)),
        cube([3.98, 23.88, 1.20], [0.86, 3.20, 2.30], (48, 40)),
        cube([-4.46, 22.94, 1.62], [0.92, 1.22, 1.64], (56, 40)),
        cube([3.54, 22.94, 1.62], [0.92, 1.22, 1.64], (56, 44)),
    ]))

    # Left and right textile/composite panels descend diagonally from the rail
    # adapters.  Several shallow layers create the stitched, padded wrap seen
    # in the reference without closing the central viewing aperture.
    bones.append(bone("fast_mandible_left_flexible_armor", [
        cube([-4.78, 24.18, -3.98], [1.10, 3.34, 5.18], (14, 46)),
        cube([-4.48, 23.38, -3.72], [1.34, 1.18, 4.76], (26, 46)),
        cube([-4.16, 22.82, -3.34], [1.42, 0.82, 4.06], (38, 46)),
        cube([-4.92, 25.66, -3.12], [0.34, 1.30, 3.36], (48, 46)),
    ], (-4.18, 25.28, -2.06), (0, -3, -5)))
    bones.append(bone("fast_mandible_right_flexible_armor", [
        cube([3.68, 24.18, -3.98], [1.10, 3.34, 5.18], (14, 46)),
        cube([3.14, 23.38, -3.72], [1.34, 1.18, 4.76], (26, 46)),
        cube([2.74, 22.82, -3.34], [1.42, 0.82, 4.06], (38, 46)),
        cube([4.58, 25.66, -3.12], [0.34, 1.30, 3.36], (48, 46)),
    ], (4.18, 25.28, -2.06), (0, 3, 5)))
    bones.append(bone("fast_mandible_left_front_panel", [
        cube([-4.20, 24.42, -4.72], [2.02, 2.58, 0.76], (0, 52)),
        cube([-3.92, 23.52, -4.70], [2.38, 1.14, 0.78], (8, 52)),
        cube([-3.54, 22.74, -4.58], [2.46, 0.98, 0.80], (18, 52)),
        cube([-2.88, 22.26, -4.48], [1.88, 0.70, 0.82], (28, 52)),
        cube([-4.46, 25.62, -4.34], [0.56, 1.54, 1.52], (36, 52)),
    ], (-3.34, 25.10, -4.20), (0, -4, -7)))
    bones.append(bone("fast_mandible_right_front_panel", [
        cube([2.18, 24.42, -4.72], [2.02, 2.58, 0.76], (0, 52)),
        cube([1.54, 23.52, -4.70], [2.38, 1.14, 0.78], (8, 52)),
        cube([1.08, 22.74, -4.58], [2.46, 0.98, 0.80], (18, 52)),
        cube([1.00, 22.26, -4.48], [1.88, 0.70, 0.82], (28, 52)),
        cube([3.90, 25.62, -4.34], [0.56, 1.54, 1.52], (36, 52)),
    ], (3.34, 25.10, -4.20), (0, 4, 7)))
    bones.append(bone("fast_mandible_panel_stitching", [
        cube([-3.98, 24.58, -5.02], [1.54, 1.92, 0.26], (44, 52)),
        cube([2.44, 24.58, -5.02], [1.54, 1.92, 0.26], (52, 52)),
        cube([-3.46, 23.18, -4.90], [1.72, 0.38, 0.28], (0, 58)),
        cube([1.74, 23.18, -4.90], [1.72, 0.38, 0.28], (8, 58)),
    ]))
    # Continuous soft-armor backing closes the seam created where the rotated
    # left/right panels meet the detachable centre plate.  It sits behind the
    # visible faces, so the camouflage plates and black spine retain their
    # outline while no sky/background can show through at any viewing angle.
    bones.append(bone("fast_mandible_continuous_inner_liner", [
        cube([-2.34, 25.94, -4.54], [4.68, 1.18, 0.44], (0, 32)),
        cube([-2.70, 24.82, -4.50], [5.40, 1.26, 0.46], (12, 32)),
        cube([-2.58, 23.66, -4.46], [5.16, 1.28, 0.48], (26, 32)),
        cube([-2.24, 22.56, -4.40], [4.48, 1.22, 0.50], (40, 32)),
        cube([-1.68, 21.90, -4.34], [3.36, 0.80, 0.52], (52, 32)),
    ]))
    bones.append(bone("fast_mandible_center_overlap_flaps", [
        cube([-1.36, 25.84, -4.92], [0.72, 1.16, 0.38], (0, 38)),
        cube([0.64, 25.84, -4.92], [0.72, 1.16, 0.38], (6, 38)),
        cube([-1.48, 24.20, -4.94], [0.70, 1.78, 0.40], (12, 38)),
        cube([0.78, 24.20, -4.94], [0.70, 1.78, 0.40], (18, 38)),
        cube([-1.36, 22.54, -4.92], [0.68, 1.78, 0.42], (24, 38)),
        cube([0.68, 22.54, -4.92], [0.68, 1.78, 0.42], (30, 38)),
    ]))
    bones.append(bone("fast_mandible_upper_binding", [
        cube([-4.18, 26.86, -4.70], [2.44, 0.30, 0.34], (18, 58)),
        cube([1.74, 26.86, -4.70], [2.44, 0.30, 0.34], (28, 58)),
        cube([-4.74, 26.88, -3.94], [0.30, 0.34, 3.72], (38, 58)),
        cube([4.44, 26.88, -3.94], [0.30, 0.34, 3.72], (46, 58)),
    ]))

    # The black centre spine projects beyond the camouflaged cheek panels and
    # terminates in a narrow chin cap.  Stepped widths approximate its faceted
    # pentagonal cross-section from the front and side views.
    bones.append(bone("fast_mandible_detachable_front_plate", [
        cube([-0.72, 26.12, -5.28], [1.44, 0.98, 0.80], (0, 16)),
        cube([-0.86, 24.18, -5.38], [1.72, 2.02, 0.92], (8, 16)),
        cube([-0.76, 22.40, -5.42], [1.52, 1.90, 0.96], (18, 16)),
        cube([-0.58, 21.94, -5.34], [1.16, 0.56, 0.88], (28, 16)),
        cube([-0.44, 21.72, -5.22], [0.88, 0.28, 0.68], (34, 16)),
    ]))
    bones.append(bone("fast_mandible_front_plate_facets", [
        cube([-0.98, 24.38, -5.12], [0.24, 1.72, 0.68], (40, 16)),
        cube([0.74, 24.38, -5.12], [0.24, 1.72, 0.68], (44, 16)),
        cube([-0.88, 22.56, -5.14], [0.22, 1.62, 0.70], (48, 16)),
        cube([0.66, 22.56, -5.14], [0.22, 1.62, 0.70], (52, 16)),
        cube([-0.34, 23.16, -5.56], [0.68, 1.52, 0.18], (56, 16)),
    ]))
    bones.append(bone("fast_heavy_attachment_hardware", [
        cube([-4.94, 26.78, -3.44], [0.50, 0.72, 0.92], (0, 22)),
        cube([4.44, 26.78, -3.44], [0.50, 0.72, 0.92], (6, 22)),
        cube([-5.12, 27.02, -3.14], [0.22, 0.28, 0.34], (12, 22)),
        cube([4.90, 27.02, -3.14], [0.22, 0.28, 0.34], (16, 22)),
        cube([-4.98, 24.06, 0.82], [0.36, 0.50, 0.82], (20, 22)),
        cube([4.62, 24.06, 0.82], [0.36, 0.50, 0.82], (26, 22)),
    ]))
    bones.append(bone("fast_heavy_full_side_rails", [
        cube([-5.34, 28.32, -1.98], [0.32, 0.78, 3.74], (30, 22)),
        cube([5.02, 28.32, -1.98], [0.32, 0.78, 3.74], (40, 22)),
        cube([-5.48, 28.44, -1.62], [0.18, 0.26, 3.02], (50, 22)),
        cube([5.30, 28.44, -1.62], [0.18, 0.26, 3.02], (58, 22)),
        cube([-5.40, 28.34, -2.16], [0.24, 0.34, 0.44], (0, 26)),
        cube([5.16, 28.34, -2.16], [0.24, 0.34, 0.44], (4, 26)),
        cube([-5.40, 28.34, 1.72], [0.24, 0.34, 0.44], (8, 26)),
        cube([5.16, 28.34, 1.72], [0.24, 0.34, 0.44], (12, 26)),
    ]))
    bones.append(bone("fast_heavy_rear_suspension", [
        cube([-3.64, 25.22, 4.42], [1.24, 0.28, 0.34], (16, 26)),
        cube([2.40, 25.22, 4.42], [1.24, 0.28, 0.34], (22, 26)),
        cube([-2.52, 24.58, 4.48], [0.28, 0.84, 0.34], (28, 26)),
        cube([2.24, 24.58, 4.48], [0.28, 0.84, 0.34], (32, 26)),
        cube([-2.38, 24.38, 4.48], [4.76, 0.28, 0.34], (36, 26)),
        cube([-0.20, 23.44, 4.54], [0.40, 1.12, 0.36], (48, 26)),
        cube([-1.14, 23.24, 4.56], [2.28, 0.28, 0.34], (52, 26)),
    ]))


def add_digital_cover(bones):
    bones.append(bone("digital_cover", [
        cube([-0.18, 29.20, -4.78], [0.36, 4.12, 0.22], (0, 56)),
        cube([-0.18, 29.20, 4.02], [0.36, 4.12, 0.22], (4, 56)),
        cube([-2.22, 33.42, -0.18], [4.44, 0.26, 0.36], (8, 56)),
        cube([-1.66, 30.02, -4.86], [3.32, 0.70, 0.24], (18, 56)),
        cube([1.72, 29.62, -4.72], [1.34, 0.46, 0.22], (28, 56)),
    ]))


def add_caiman_details(bones):
    bones.append(bone("caiman_velcro", [
        cube([-2.92, 32.34, -3.68], [2.24, 0.24, 1.34], (0, 56)),
        cube([0.68, 32.34, -3.68], [2.24, 0.24, 1.34], (6, 56)),
        cube([-2.56, 32.72, 2.22], [2.00, 0.24, 1.18], (12, 56)),
        cube([0.56, 32.72, 2.22], [2.00, 0.24, 1.18], (18, 56)),
    ]))
    bones.append(bone("caiman_vents", [
        cube([-4.78, 30.22, -2.10], [0.26, 0.30, 0.74], (24, 56)),
        cube([-4.78, 30.22, 1.36], [0.26, 0.30, 0.74], (24, 56)),
        cube([4.52, 30.22, -2.10], [0.26, 0.30, 0.74], (24, 56)),
        cube([4.52, 30.22, 1.36], [0.26, 0.30, 0.74], (24, 56)),
    ]))


def add_kiver_details(bones):
    bones.append(bone("fabric_seams", [
        cube([-0.16, 29.04, -4.76], [0.32, 4.42, 0.24], (0, 56)),
        cube([-0.16, 29.04, 4.00], [0.32, 4.42, 0.24], (4, 56)),
        cube([-2.60, 33.48, -0.16], [5.20, 0.24, 0.32], (8, 56)),
    ]))
    bones.append(bone("ear_padding", [
        cube([-5.02, 24.72, -2.72], [0.84, 4.50, 5.60], (18, 56)),
        cube([4.18, 24.72, -2.72], [0.84, 4.50, 5.60], (30, 56)),
        cube([-5.18, 25.54, -1.62], [0.28, 2.74, 3.24], (42, 56)),
        cube([4.90, 25.54, -1.62], [0.28, 2.74, 3.24], (48, 56)),
    ]))


def write(identifier, bones):
    GEO_DIR.mkdir(parents=True, exist_ok=True)
    target = GEO_DIR / f"helmet_{identifier}.geo.json"
    target.write_text(json.dumps(document(identifier, bones), indent=2) + "\n", encoding="utf-8")
    print(target.relative_to(ROOT))


def clone_bones(identifier):
    source = json.loads((GEO_DIR / f"helmet_{identifier}.geo.json").read_text(encoding="utf-8"))
    return source["minecraft:geometry"][0]["bones"]


def main():
    riot = dome("riot", high_cut=False, mount=False, rails=False)
    add_riot_details(riot)
    write("riot", riot)
    write("lightweight_ballistic_mask", lightweight_mask())
    write("cqcm_ballistic_mask", cqcm())
    write("lzsh_light", dome("lzsh_light", high_cut=True, mount=True, rails=True))
    approved_6b47 = json.loads((GEO_DIR / "helmet_6b47.geo.json").read_text(encoding="utf-8"))
    digital = approved_6b47["minecraft:geometry"][0]["bones"]
    add_digital_cover(digital)
    write("6b47_digital_cover", digital)
    caiman = dome("caiman_composite", high_cut=True, mount=True, rails=True)
    add_caiman_details(caiman)
    write("caiman_composite", caiman)
    kiver = dome("kiver_m_heavy", high_cut=False, heavy=True, mount=False, rails=False)
    add_kiver_details(kiver)
    write("kiver_m_heavy", kiver)
    achhc = clone_bones("6b47")
    achhc = [bone_data for bone_data in achhc if bone_data["name"] not in {"front_mount", "side_rails"}]
    write("achhc_light", achhc)
    strike = clone_bones("6b47")
    add_strike_details(strike)
    write("strike", strike)
    fast = clone_bones("6b47")
    add_fast_details(fast)
    write("fast_mt_super_high_cut", fast)
    airframe = clone_bones("6b47")
    add_airframe_details(airframe)
    write("airframe", airframe)
    flux = clone_bones("6b47")
    add_flux_details(flux)
    write("flux", flux)
    vulkan = clone_bones("6b47")
    vulkan = [bone_data for bone_data in vulkan if bone_data["name"] not in {"front_mount", "side_rails"}]
    add_vulkan_details(vulkan)
    write("vulkan_5_heavy", vulkan)
    altyn = clone_bones("kiver_m_heavy")
    altyn = [bone_data for bone_data in altyn if bone_data["name"] not in {"fabric_seams", "ear_padding"}]
    add_altyn_details(altyn)
    write("altyn_heavy", altyn)
    fast_heavy = clone_bones("fast_mt_super_high_cut")
    add_fast_heavy_kit_details(fast_heavy)
    write("fast_heavy_protection_kit", fast_heavy)


if __name__ == "__main__":
    main()
