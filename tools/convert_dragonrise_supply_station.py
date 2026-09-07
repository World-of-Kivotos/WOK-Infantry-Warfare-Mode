"""Convert the authorized DragonRise supply-station Gecko geometry to a Forge OBJ.

The generated files live in the WOK Infantry namespace, so the finished mod does not
load DragonRise classes or resources at runtime. DragonRise and WOK are collaborating
projects; these particular visual assets are included with project authorization.
"""

from __future__ import annotations

import argparse
import json
import math
import shutil
import zipfile
from pathlib import Path


GEO_ENTRY = "assets/dragonrise_reforge/geo/ammo_supply_station.geo.json"
ENTITY_TEXTURE_ENTRY = (
    "assets/dragonrise_reforge/textures/entity/ammo_supply_station.png"
)
ITEM_TEXTURE_ENTRY = "assets/dragonrise_reforge/textures/item/ammo_supply_station.png"


def rotate(point: tuple[float, float, float], pivot: list[float], angles: list[float]):
    x, y, z = (point[index] - pivot[index] for index in range(3))
    rx, ry, rz = (math.radians(value) for value in angles)
    y, z = y * math.cos(rx) - z * math.sin(rx), y * math.sin(rx) + z * math.cos(rx)
    x, z = x * math.cos(ry) + z * math.sin(ry), -x * math.sin(ry) + z * math.cos(ry)
    x, y = x * math.cos(rz) - y * math.sin(rz), x * math.sin(rz) + y * math.cos(rz)
    return x + pivot[0], y + pivot[1], z + pivot[2]


def transform(point, cube, bone, bones):
    """Apply GeckoLib 4's static cube and bone transforms exactly.

    GeckoLib negates Bedrock X/Y rotations and the X pivot, then composes
    X -> Y -> Z rotations around each absolute pivot. Parent bone transforms
    are applied after the child transform.
    """
    value = point
    cube_rotation = cube.get("rotation", [0, 0, 0])
    if any(cube_rotation):
        pivot = cube.get("pivot", cube["origin"])
        value = rotate(value, [-pivot[0] / 16.0, pivot[1] / 16.0,
                               pivot[2] / 16.0],
                       [-cube_rotation[0], -cube_rotation[1], cube_rotation[2]])
    current = bone
    while current is not None:
        rotation = current.get("rotation", [0, 0, 0])
        if any(rotation):
            pivot = current.get("pivot", [0, 0, 0])
            value = rotate(value, [-pivot[0] / 16.0, pivot[1] / 16.0,
                                   pivot[2] / 16.0],
                           [-rotation[0], -rotation[1], rotation[2]])
        current = bones.get(current.get("parent"))
    return value


FACE_VERTICES = {
    # These are GeckoLib BakedModelFactory.VertexSet's exact quad orders.
    "west": ("top_right_back", "top_left_back",
             "bottom_left_back", "bottom_right_back"),
    "east": ("top_left_front", "top_right_front",
             "bottom_right_front", "bottom_left_front"),
    "north": ("top_left_back", "top_left_front",
              "bottom_left_front", "bottom_left_back"),
    "south": ("top_right_front", "top_right_back",
              "bottom_right_back", "bottom_right_front"),
    "up": ("top_right_back", "top_right_front",
           "top_left_front", "top_left_back"),
    "down": ("bottom_left_back", "bottom_left_front",
             "bottom_right_front", "bottom_right_back"),
}


def convert(geometry: dict) -> str:
    definition = geometry["minecraft:geometry"][0]
    description = definition["description"]
    texture_width = float(description["texture_width"])
    texture_height = float(description["texture_height"])
    bones = {bone["name"]: bone for bone in definition["bones"]}
    faces = []
    all_points = []

    for bone in definition["bones"]:
        for cube in bone.get("cubes", []):
            inflate = float(cube.get("inflate", 0.0)) / 16.0
            origin = [float(value) for value in cube["origin"]]
            size = [float(value) for value in cube["size"]]
            # GeckoLib converts Bedrock's X coordinate by negating origin+size.
            x0 = -(origin[0] + size[0]) / 16.0 - inflate
            x1 = -origin[0] / 16.0 + inflate
            y0 = origin[1] / 16.0 - inflate
            y1 = (origin[1] + size[1]) / 16.0 + inflate
            z0 = origin[2] / 16.0 - inflate
            z1 = (origin[2] + size[2]) / 16.0 + inflate
            raw_vertices = {
                "bottom_left_back": (x0, y0, z0),
                "bottom_right_back": (x0, y0, z1),
                "top_left_back": (x0, y1, z0),
                "top_right_back": (x0, y1, z1),
                "top_left_front": (x1, y1, z0),
                "top_right_front": (x1, y1, z1),
                "bottom_left_front": (x1, y0, z0),
                "bottom_right_front": (x1, y0, z1),
            }
            vertices = {name: transform(point, cube, bone, bones)
                        for name, point in raw_vertices.items()}
            for face_name, uv_data in cube.get("uv", {}).items():
                if face_name not in FACE_VERTICES or not isinstance(uv_data, dict):
                    continue
                uv = uv_data["uv"]
                uv_size = uv_data["uv_size"]
                u0 = float(uv[0]) / texture_width
                v0 = float(uv[1]) / texture_height
                u1 = (float(uv[0]) + float(uv_size[0])) / texture_width
                v1 = (float(uv[1]) + float(uv_size[1])) / texture_height
                points = [vertices[key] for key in FACE_VERTICES[face_name]]
                # GeckoLib swaps U for non-mirrored cubes. OBJ V is bottom-origin;
                # Forge's flip_v=true converts these values back to Minecraft UVs.
                texcoords = ((u1, 1.0 - v0), (u0, 1.0 - v0),
                             (u0, 1.0 - v1), (u1, 1.0 - v1))
                faces.append((bone["name"], face_name, points, texcoords))
                all_points.extend(points)

    min_x = min(point[0] for point in all_points)
    max_x = max(point[0] for point in all_points)
    min_y = min(point[1] for point in all_points)
    min_z = min(point[2] for point in all_points)
    max_z = max(point[2] for point in all_points)
    center_x = (min_x + max_x) * 0.5
    center_z = (min_z + max_z) * 0.5

    lines = [
        "# Authorized DragonRise ammo supply station geometry",
        "# Converted for the independent WOK Infantry block",
        "mtllib large_ammo_supply_station.mtl",
        "o large_ammo_supply_station",
        "usemtl station",
    ]
    vertex_index = 1
    texture_index = 1
    current_group = None
    for bone_name, face_name, points, texcoords in faces:
        group = f"{bone_name}_{face_name}"
        if group != current_group:
            lines.append(f"g {group}")
            current_group = group
        for x, y, z in points:
            # Coordinates above already use GeckoLib's 1/16 block scale. Recenter
            # the entity model around this block without altering its proportions.
            lines.append(
                f"v {x - center_x + 0.5:.8f} "
                f"{y - min_y:.8f} "
                f"{z - center_z + 0.5:.8f}"
            )
        for u, v in texcoords:
            lines.append(f"vt {u:.8f} {v:.8f}")
        lines.append(
            "f " + " ".join(
                f"{vertex_index + offset}/{texture_index + offset}"
                for offset in range(4)
            )
        )
        vertex_index += 4
        texture_index += 4
    return "\n".join(lines) + "\n"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("jar", type=Path)
    parser.add_argument("resources", type=Path)
    args = parser.parse_args()
    assets = args.resources / "assets" / "wok_infantry"
    geo_dir = assets / "geo"
    model_dir = assets / "models" / "block"
    block_texture_dir = assets / "textures" / "block"
    item_texture_dir = assets / "textures" / "item"
    for directory in (geo_dir, model_dir, block_texture_dir, item_texture_dir):
        directory.mkdir(parents=True, exist_ok=True)

    with zipfile.ZipFile(args.jar) as archive:
        original_geometry = archive.read(GEO_ENTRY)
        geometry = json.loads(original_geometry)
        (geo_dir / "large_ammo_supply_station.geo.json").write_bytes(
            original_geometry
        )
        (model_dir / "large_ammo_supply_station.obj").write_text(
            convert(geometry), encoding="utf-8", newline="\n"
        )
        with archive.open(ENTITY_TEXTURE_ENTRY) as source, (
            block_texture_dir / "large_ammo_supply_station.png"
        ).open("wb") as target:
            shutil.copyfileobj(source, target)
        with archive.open(ITEM_TEXTURE_ENTRY) as source, (
            item_texture_dir / "large_ammo_supply_station.png"
        ).open("wb") as target:
            shutil.copyfileobj(source, target)

    (model_dir / "large_ammo_supply_station.mtl").write_text(
        "# Authorized DragonRise station material\n"
        "newmtl station\n"
        "Ka 1.0 1.0 1.0\n"
        "Kd 1.0 1.0 1.0\n"
        "map_Kd #texture0\n",
        encoding="utf-8",
        newline="\n",
    )


if __name__ == "__main__":
    main()
