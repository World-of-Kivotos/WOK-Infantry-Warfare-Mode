"""Apply the M249 standing/prone handling profile without reserializing the loadout."""

import argparse
from datetime import datetime
import json
from pathlib import Path
import re
import tempfile
import os
import sys


DEFAULT_ROOT = Path(r"D:\WOK步战测试\1.20.1-Forge_47.4.22")
SCALES = {
    "wok_infantry_vertical_recoil_scale": "8.0",
    "wok_infantry_horizontal_recoil_scale": "6.0",
}
GUN_ID = re.compile(r'(?:^\{|,)\s*GunId\s*:\s*"tacz:m249"\s*(?=,|})')


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--test-root", type=Path, default=DEFAULT_ROOT)
    parser.add_argument("--apply", action="store_true", help="Write after validation; otherwise preview only")
    args = parser.parse_args()
    root = args.test_root.resolve(strict=True)
    loadout = root / "config/wok_infantry/loadouts.json"
    gun_data = root / "tacz/tacz_default_gun/data/tacz/data/guns/m249_data.json"

    # The installed TaCZ gun pack already supplies the required posture difference.
    crawl = re.findall(r'"crawl_recoil_multiplier"\s*:\s*([0-9.]+)', gun_data.read_text(encoding="utf-8-sig"))
    if len(crawl) != 1 or float(crawl[0]) != 0.1:
        raise ValueError("Expected M249 crawl_recoil_multiplier=0.1; review this gun pack before applying")

    original_bytes = loadout.read_bytes()
    original = original_bytes.decode("utf-8-sig")
    expected = json.loads(original)
    replacements = {}
    changes = []
    matched = 0
    for cls in expected["classes"]:
        for slot_id, entries in cls["slots"].items():
            for entry in entries:
                old = entry.get("snbt", "")
                if entry.get("itemId") != "tacz:modern_kinetic_gun" or not GUN_ID.search(old):
                    continue
                matched += 1
                new = old
                previous = {}
                for tag, value in SCALES.items():
                    pattern = re.compile(r'(?P<prefix>(?:^\{|,)\s*' + re.escape(tag)
                                         + r'\s*:\s*)(?P<value>[-+0-9.eE]+)[fF](?=\s*[,}])')
                    matches = list(pattern.finditer(new))
                    if len(matches) != 1:
                        raise ValueError(f"Expected one {tag} in {cls['id']}/{slot_id}/{entry['id']}")
                    previous[tag] = float(matches[0].group("value"))
                    new = pattern.sub(lambda m: m.group("prefix") + value + "f", new)
                if new != old:
                    entry["snbt"] = new
                    replacements[old] = new
                    changes.append({"classId": cls["id"], "slotId": slot_id,
                                    "entryId": entry["id"], "before": previous,
                                    "after": {k: float(v) for k, v in SCALES.items()}})
    if matched == 0:
        raise ValueError("No existing M249 loadout entries found")

    updated = original
    for old, new in replacements.items():
        old_literal = json.dumps(old, ensure_ascii=False)
        if old_literal not in updated:
            raise ValueError("SNBT JSON escaping differs; refusing to rewrite the whole file")
        updated = updated.replace(old_literal, json.dumps(new, ensure_ascii=False))
    if json.loads(updated) != expected:
        raise ValueError("Candidate differs outside the selected M249 recoil fields")

    report = {"mode": "apply" if args.apply else "preview", "loadout": str(loadout),
              "matchedEntries": matched, "changes": changes, "proneMultiplier": 0.1}
    if args.apply and updated != original:
        if loadout.read_bytes() != original_bytes:
            raise ValueError("Loadout changed during preparation; retry after reviewing the new file")
        backup = loadout.with_name(loadout.name + ".m249-stance-"
                                   + datetime.now().strftime("%Y%m%d-%H%M%S-%f") + ".bak")
        with backup.open("xb") as handle:
            handle.write(original_bytes)
        encoded = updated.encode("utf-8")
        if original_bytes.startswith(b"\xef\xbb\xbf"):
            encoded = b"\xef\xbb\xbf" + encoded
        with tempfile.NamedTemporaryFile(dir=loadout.parent, prefix="m249-recoil-", suffix=".tmp", delete=False) as handle:
            temporary = Path(handle.name)
            handle.write(encoded)
        try:
            os.replace(temporary, loadout)
        finally:
            if temporary.exists():
                temporary.unlink()
        report["backup"] = str(backup)
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
