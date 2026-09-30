"""Map a local SRG TaCZ distribution (including mixin shadows) for official-name userdev tests.

Only Minecraft member identifiers are renamed. No gameplay code is replaced. The output is a
test fixture, never a distribution MOD. Pass ForgeGradle's build/createSrgToMcp/output.srg.
"""
import argparse
import io
import re
import struct
import zipfile
from pathlib import Path


def member_names(mapping):
    names = {}
    for line in mapping.read_text(encoding="utf-8").splitlines():
        parts = line.split()
        if parts[0] not in ("FD:", "MD:"):
            continue
        old = parts[1].rsplit("/", 1)[1].encode()
        new = parts[2 if parts[0] == "FD:" else 3].rsplit("/", 1)[1].encode()
        if old in names and names[old] != new:
            raise ValueError(f"Ambiguous SRG identifier {old!r}")
        names[old] = new
    return names


def class_members(data, names):
    if data[:4] != b"\xca\xfe\xba\xbe":
        raise ValueError("Not a class file")
    result = bytearray(data[:10])
    count = struct.unpack_from(">H", data, 8)[0]
    offset, index = 10, 1
    sizes = {3: 4, 4: 4, 5: 8, 6: 8, 7: 2, 8: 2, 9: 4, 10: 4, 11: 4,
             12: 4, 15: 3, 16: 2, 17: 4, 18: 4, 19: 2, 20: 2}
    while index < count:
        tag = data[offset]
        result.append(tag)
        offset += 1
        if tag == 1:
            length = struct.unpack_from(">H", data, offset)[0]
            value = data[offset + 2:offset + 2 + length]
            mapped = re.sub(rb"\b[fm]_\d+_\b", lambda match: names.get(match[0], match[0]), value)
            result.extend(struct.pack(">H", len(mapped)))
            result.extend(mapped)
            offset += 2 + length
        else:
            size = sizes[tag]
            result.extend(data[offset:offset + size])
            offset += size
            if tag in (5, 6):
                index += 1
        index += 1
    result.extend(data[offset:])
    return bytes(result)


def map_jar(data, names):
    output = io.BytesIO()
    with zipfile.ZipFile(io.BytesIO(data)) as source, zipfile.ZipFile(output, "w") as target:
        for info in source.infolist():
            content = source.read(info)
            if info.filename.endswith(".class"):
                content = class_members(content, names)
            elif info.filename.endswith(".jar"):
                content = map_jar(content, names)
            target.writestr(info, content)
    return output.getvalue()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input", type=Path)
    parser.add_argument("mapping", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError("Use a fresh output path; never replace the installed TaCZ MOD")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_bytes(map_jar(args.input.read_bytes(), member_names(args.mapping)))
    print(f"Created official-name test fixture: {args.output}")
