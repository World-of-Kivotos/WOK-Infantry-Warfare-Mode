"""Prepare a narrowly scoped repair of the three legacy test formations; never edit the source."""
import copy
import hashlib
import json
import re
import sys
from pathlib import Path

source, destination = map(Path, sys.argv[1:3])
raw = source.read_bytes()
text = raw.decode("utf-8-sig")
before = json.loads(text)
after = copy.deepcopy(before)
targets = {"academy/default", "academy/millennium_seminar_mobile", "caesar/default"}
seen = set()
policy = dict(enabled=True, maxActive=1, squadLeaderCanPlace=True, commanderCanPlace=True,
              maxHealth=100, placementCooldownSeconds=480, replacementCooldownSeconds=480,
              destructionCooldownSeconds=480)
for faction in after["factions"]:
    for formation in faction["formations"]:
        key = f'{faction["id"]}/{formation["id"]}'
        if key not in targets:
            continue
        old = formation["capabilities"]["rally"]
        expected = {k: False if isinstance(v, bool) else 0 for k, v in policy.items()}
        if old != expected:
            raise ValueError(f"{key}: not the known disabled legacy placeholder; preserve for review")
        formation["capabilities"]["rally"] = policy.copy()
        seen.add(key)
if seen != targets:
    raise ValueError(f"Missing exact target formations: {targets - seen}")

# Keep the original JSON text intact outside the rally objects, including weapon SNBT and spacing.
matches = list(re.finditer(r'"rally"\s*:\s*\{[^{}]*\}', text))
if len(matches) != len(targets):
    raise ValueError("Unexpected additional rally objects; refusing a broad replacement")
newline = "\r\n" if "\r\n" in text else "\n"
def replace(match):
    indent = re.search(r'\n([ \t]*)"', match[0])[1]
    fields = [f'{indent}"{k}": {json.dumps(v)}' for k, v in policy.items()]
    return '"rally": {' + newline + ("," + newline).join(fields) + newline + indent[:-2] + "}"
updated = re.sub(r'"rally"\s*:\s*\{[^{}]*\}', replace, text)
if json.loads(updated) != after:
    raise ValueError("Text patch changed data outside the intended rally policies")
if destination.exists():
    raise FileExistsError("Use a fresh prepared configuration path")
destination.parent.mkdir(parents=True, exist_ok=True)
destination.write_bytes(updated.encode("utf-8"))
manifest = dict(configVersion="0.1.0", minimumCoreVersion="0.2.2", release="0.2.2",
                source=str(source), targets=sorted(targets),
                sourceSha256=hashlib.sha256(raw).hexdigest(),
                preparedSha256=hashlib.sha256(destination.read_bytes()).hexdigest(), policy=policy)
destination.with_suffix(".manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps(manifest, ensure_ascii=False))
