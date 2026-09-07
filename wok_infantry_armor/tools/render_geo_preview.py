import json
import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont


def translation(v):
    matrix = np.eye(4)
    matrix[:3, 3] = v
    return matrix


def rotation_xyz(degrees):
    x, y, z = np.radians(degrees)
    rx = np.array([[1, 0, 0, 0], [0, math.cos(x), -math.sin(x), 0], [0, math.sin(x), math.cos(x), 0], [0, 0, 0, 1]])
    ry = np.array([[math.cos(y), 0, math.sin(y), 0], [0, 1, 0, 0], [-math.sin(y), 0, math.cos(y), 0], [0, 0, 0, 1]])
    rz = np.array([[math.cos(z), -math.sin(z), 0, 0], [math.sin(z), math.cos(z), 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]])
    return rz @ ry @ rx


def transform_point(matrix, point):
    value = matrix @ np.array([point[0], point[1], point[2], 1.0])
    return value[:3]


def cube_vertices(origin, size):
    x, y, z = origin
    dx, dy, dz = size
    return np.array([
        [x, y, z], [x + dx, y, z], [x + dx, y + dy, z], [x, y + dy, z],
        [x, y, z + dz], [x + dx, y, z + dz], [x + dx, y + dy, z + dz], [x, y + dy, z + dz],
    ])


FACES = [
    (0, 3, 2, 1), (4, 5, 6, 7), (0, 4, 7, 3),
    (1, 2, 6, 5), (0, 1, 5, 4), (3, 7, 6, 2),
]


def base_color(name):
    if "maska_hardware" in name:
        return np.array([69, 70, 54])
    if "maska_face_plate_slit_bevels" in name:
        return np.array([81, 84, 51])
    if "maska_face_plate" in name:
        return np.array([132, 134, 82])
    if "maska_brow_band" in name:
        return np.array([111, 115, 71])
    if "maska_left_carrier" in name or "maska_right_carrier" in name:
        return np.array([119, 123, 76])
    if "maska_shell_rear_skirt" in name:
        return np.array([128, 133, 84])
    if "maska" in name:
        return np.array([151, 156, 103])
    if "glass" in name:
        return np.array([54, 71, 70])
    if "visor" in name and "carrier" not in name:
        return np.array([37, 40, 38])
    if "spine" in name or "bezel" in name:
        return np.array([39, 42, 39])
    if "slaap" in name or "mandible" in name and "front_plate" not in name:
        return np.array([126, 119, 88])
    if "hardware" in name or "adapter" in name or "housing" in name or "box" in name or "comms" in name or "cable" in name:
        return np.array([67, 70, 63])
    if "strap" in name or "chin" in name or "liner" in name:
        return np.array([40, 43, 39])
    if "mount" in name or "rail" in name or "fastener" in name:
        return np.array([77, 80, 66])
    if "rim" in name:
        return np.array([82, 88, 67])
    return np.array([104, 112, 82])


def build_faces(geometry):
    bones = {bone["name"]: bone for bone in geometry["bones"]}
    matrices = {}

    def bone_matrix(name):
        if name in matrices:
            return matrices[name]
        bone = bones[name]
        parent = bone_matrix(bone["parent"]) if bone.get("parent") else np.eye(4)
        pivot = np.array(bone.get("pivot", [0, 0, 0]), dtype=float)
        rotation = bone.get("rotation", [0, 0, 0])
        local = translation(pivot) @ rotation_xyz(rotation) @ translation(-pivot)
        matrices[name] = parent @ local
        return matrices[name]

    result = []
    for name, bone in bones.items():
        matrix = bone_matrix(name)
        for cube in bone.get("cubes", []):
            vertices = cube_vertices(cube["origin"], cube["size"])
            vertices = np.array([transform_point(matrix, vertex) for vertex in vertices])
            color = base_color(name)
            for face in FACES:
                points = vertices[list(face)]
                normal = np.cross(points[1] - points[0], points[2] - points[0])
                length = np.linalg.norm(normal)
                if length > 0:
                    normal /= length
                result.append((points, normal, color))
    return result


def render_view(canvas, faces, box, mode, label):
    left, top, right, bottom = box
    width, height = right - left, bottom - top
    if mode == "front":
        camera = np.array([0.0, 0.0, -1.0])
        project = lambda p: (p[0], p[1])
    elif mode == "left":
        camera = np.array([-1.0, 0.0, 0.0])
        project = lambda p: (-p[2], p[1])
    elif mode == "front_left":
        camera = np.array([-0.62, 0.0, -0.78])
        camera /= np.linalg.norm(camera)
        horizontal = np.array([camera[2], 0.0, -camera[0]])
        project = lambda p: (float(np.dot(p, horizontal)), p[1])
    else:
        camera = np.array([0.0, 0.0, 1.0])
        project = lambda p: (-p[0], p[1])

    visible = []
    for points, normal, color in faces:
        facing = float(np.dot(normal, camera))
        if facing <= 0.015:
            continue
        projected = [project(point) for point in points]
        depth = float(np.mean(points @ camera))
        light = max(0.0, float(np.dot(normal, np.array([-0.35, 0.78, -0.52]))))
        shade = 0.62 + 0.34 * light + 0.10 * facing
        shaded = tuple(np.clip(color * shade, 0, 255).astype(int))
        visible.append((depth, projected, shaded))

    all_points = [point for _, projected, _ in visible for point in projected]
    min_x = min(p[0] for p in all_points)
    max_x = max(p[0] for p in all_points)
    min_y = min(p[1] for p in all_points)
    max_y = max(p[1] for p in all_points)
    scale = min((width - 44) / (max_x - min_x), (height - 62) / (max_y - min_y))
    center_x = (min_x + max_x) / 2
    center_y = (min_y + max_y) / 2
    screen_center_x = (left + right) / 2
    screen_center_y = (top + bottom) / 2 + 8

    draw = ImageDraw.Draw(canvas)
    draw.rounded_rectangle(box, radius=18, fill=(18, 21, 19), outline=(53, 59, 51), width=2)
    for _, projected, color in sorted(visible, key=lambda item: item[0]):
        polygon = [
            (screen_center_x + (x - center_x) * scale, screen_center_y - (y - center_y) * scale)
            for x, y in projected
        ]
        draw.polygon(polygon, fill=color)
    draw.text((left + 18, top + 14), label, fill=(214, 218, 205), font=ImageFont.load_default())


def main():
    source = Path(sys.argv[1])
    target = Path(sys.argv[2])
    document = json.loads(source.read_text(encoding="utf-8"))
    geometry = document["minecraft:geometry"][0]
    faces = build_faces(geometry)
    canvas = Image.new("RGB", (1600, 560), (10, 12, 11))
    render_view(canvas, faces, (20, 20, 520, 540), "front", "FRONT")
    render_view(canvas, faces, (550, 20, 1050, 540), "front_left", "FRONT-LEFT")
    render_view(canvas, faces, (1080, 20, 1580, 540), "left", "LEFT")
    target.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(target)
    print(target.resolve())


if __name__ == "__main__":
    main()
