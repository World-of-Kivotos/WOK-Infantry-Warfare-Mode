"""Package only delivery files; exclude browser test projects and screenshots."""
from pathlib import Path
import hashlib
import re
import zipfile

root = Path(__file__).resolve().parents[2]
editor_version = (root / 'tools/loadout-editor/VERSION').read_text(encoding='utf-8').strip()
core_version = re.search(r'^mod_version=(.+)$', (root / 'wok_infantry/gradle.properties').read_text(encoding='utf-8'), re.M)[1].strip()
out = root / 'outputs' / f'loadout-editor-{editor_version}-core-{core_version}'
names = ['WOK步战编制配装编辑器.html', '使用说明.md', '验收说明.md',
         '示例数据包-仅供学习.json', f'游戏端MOD/wok_infantry-{core_version}.jar', 'VERSION.json', 'SHA256SUMS.txt']
package = out.parent / f'WOK步战编制配装编辑器-{editor_version}-核心{core_version}.zip'
with zipfile.ZipFile(package, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as z:
    for name in names:
        z.write(out / name, 'WOK步战编制配装编辑器/' + name)
with zipfile.ZipFile(package) as z:
    assert z.testzip() is None
    assert len(z.infolist()) == len(names)
    for name in names:
        assert z.read('WOK步战编制配装编辑器/' + name) == (out / name).read_bytes()
digest = hashlib.sha256(package.read_bytes()).hexdigest()
package.with_suffix('.zip.sha256').write_text(digest+'  '+package.name+'\n', encoding='utf-8')
print(f'{package}\n{package.stat().st_size} bytes\nSHA256 {digest}')
