# WOK步战核心第三方素材说明

本说明有两份：仓库内的 `wok_infantry/THIRD_PARTY_NOTICES.md` 与 JAR 内的
`META-INF/THIRD_PARTY_NOTICES.md`，两份内容保持一致。

## 龙之崛起大型弹药补给站

`WOK步战核心` 的独立大型弹药补给站使用了龙之崛起项目提供的以下视觉素材：

- 大型弹药补给站几何造型
- 实体贴图
- 物品图标

WOK 与龙之崛起为合作关系，上述素材经项目合作授权用于
`wok_infantry:large_ammo_supply_station`。几何在构建资源中转换为 Forge 原生 OBJ，
运行时不需要安装龙之崛起；方块注册、碰撞、1500 点补给存档和交互逻辑均由
`WOK步战核心` 自行实现。

## 原创素材

以下素材都由本项目自行绘制或生成，不含第三方素材，不在本说明的第三方授权范围内
（路径都在 `assets/wok_infantry/` 下）：

- 千禧年研讨会编制徽标 `textures/gui/formations/millennium_seminar_mobile.png`
  （256×256，2026-08-31 加入）是项目自绘的。同目录的预缩图
  `millennium_seminar_mobile_64.png` 与 `millennium_seminar_mobile_32.png`
  （0.4.0-beta.1 新增）由它按面积平均缩小得到，同属项目自绘素材。
- 三张界面图集（0.4.0-beta.1 新增）是项目原创像素图，由本地预览工具生成：
  - `textures/gui/ui_icons.png`（界面图标图集，45 个图标）
  - `textures/gui/ui_hatch.png`（禁用斜纹）
  - `textures/gui/map_icons.png`（战术地图标点图集，10 种标点）

三张图集由界面预览工具 `ui-preview` 的 `tools/export-ui-atlas.mjs` 从 `kit/icons.js`
（`MONO_ICONS`）和 `kit/map-icons.js` 中手写的字符网格像素数据导出。`ui-preview` 是本地预览工具，
不在本 Git 仓库内（本机位置 `E:\WOK步战\ui-preview`）；需要改图时在那里改源数据并重新导出，
不要直接手改 PNG。仓库内的 `wok_infantry/src/test/resources/ui_atlas/ui_atlas_manifest.json`
记录了生成器、来源说明和三张 PNG 的 SHA-256，图集测试据此核对。地图标点只参考《Squad》等作品的
信息组织方式（底板形状表示类别、白色剪影表示种类），图形本身没有复制任何作品的图标。
