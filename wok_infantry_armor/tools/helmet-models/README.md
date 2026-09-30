# 头盔模型生成工具

17 件头部防具的穿戴模型由这里的脚本生成，不手改 `geo/helmet_*.geo.json` 和 `textures/models/armor/helmet_*_layer_1.png`。
精度固定为折中 2×：几何在 ¼ 像素网格上，贴图每像素 2×2 texel，按半像素格上色。
（同目录上一级的 `generate_*_headgear.py`、`install_maska_1sch_assets.py` 生成的是 1.1.x 的旧模型，已被本工具取代。）

需要 Node.js 18+，不依赖任何 npm 包。

## 文件

- `helmets/<id>.js`：每件头盔的设计，顶层只有一条 `ARMORS['<id>'] = function (mode) { ... }`，返回 `{ S, px, cell, boxes }`。
  所有设计会拼进同一个模块，辅助函数必须写在函数体里。
- `helmets/<id>.meta.json`：名称、等级、塔科夫原物图片链接、给人看的说明。
- `helmets/_kit.js`：共用盔壳生成器（`shell`、`rimAlong`、`colBottom`、`sideStrap`）。`designs.js`：贴图工具（`kit`、`hex`、`fbm`、`box` 等）。
- `export.mjs`：导出到 `../../src/main/resources/assets/wok_infantry_armor`。
- `check.mjs <id>`：无头自检（颜色、离皮肤帽子层的距离、是否挡住眼睛、方块数）。
- `viewer.js`、`template.html`、`build.mjs`、`serve.mjs`、`geo_embed.mjs`：可交互预览页（左边读现有 geo.json，右边是设计）。

## 常用命令

```
node check.mjs 6b47
node export.mjs                       # 全部导出
node export.mjs --only 6b47           # 只导出一件
node geo_embed.mjs ../../src/main/resources/assets/wok_infantry_armor
node build.mjs                        # 生成 helmet-preview.html
node serve.mjs 5182                   # 本地预览
```

## 约定

- 坐标：MC 头部空间，头 x −4..4、y −8..0（y 向下）、z −4..4，前方 −z。皮肤帽子层是 ±4.5、y −8.5..0.5，所有朝外的可见面离它至少 0.25。
- 覆盖范围：MC 的头大、额头高，盔沿前沿在 y −4.5，两侧约 −2.5，后面到 −1 ~ −1.5；只保留型号自己的特征切口。
- 眼睛在 x ±1..3、y −4..−2。普通头盔不能挡眼睛；带面罩的要留观察窗/观察缝。
- 标签为 `visor glass` 的方块导出到 `visor_glass` 骨骼，`HelmetGeoRenderer` 在盔壳画完后以半透明单独绘制；其余方块在 `helmet_shell` 骨骼。
- 导出换算：文件坐标 = (x, 24 − y, z)，方块旋转角原样写入，旋转中心同样换算；每个面的顶点顺序和 UV 角与原版 ModelPart 相同（已对照 GeckoLib 4.8.4 的 `VertexSet`/`GeoQuad` 核对），没有不透明像素的面不写入文件。

参考图来自 tarkov.dev（`https://assets.tarkov.dev/<id>-512.webp`，id 见各 meta.json），只作建模参考，不放进仓库。
