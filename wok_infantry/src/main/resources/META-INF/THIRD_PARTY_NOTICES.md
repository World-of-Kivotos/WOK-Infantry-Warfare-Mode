# WOK步战核心第三方素材说明

本说明有两份：仓库内的 `wok_infantry/THIRD_PARTY_NOTICES.md` 与 JAR 内的
`META-INF/THIRD_PARTY_NOTICES.md`，两份内容保持一致。

## 非官方同人作品声明

本项目（WOK步战及其附属 MOD）是非官方的粉丝同人作品。其中阵营、编制、组织、地名等名称以及相关标志取材自《蔚蓝档案》（Blue Archive），相关权利归原权利人所有。本项目与原作及其权利方没有任何关联，也未获得其授权或认可。如权利人提出要求，我们会更换或移除相关名称与图标，这不影响 MOD 的主要玩法。

## 龙之崛起大型弹药补给站

`WOK步战核心` 的独立大型弹药补给站使用了龙之崛起项目提供的以下视觉素材：

- 大型弹药补给站几何造型
- 实体贴图
- 物品图标

WOK 与龙之崛起为合作关系，上述素材经项目合作授权用于
`wok_infantry:large_ammo_supply_station`。几何在构建资源中转换为 Forge 原生 OBJ，
运行时不需要安装龙之崛起；方块注册、碰撞、1500 点补给存档和交互逻辑均由
`WOK步战核心` 自行实现。

## 千禧年研讨会编制徽标

千禧年研讨会编制徽标（wok_infantry 资源 textures/gui/formations/millennium_seminar_mobile*.png，含 64/32 预缩图）由本项目成员参照《蔚蓝档案》（Blue Archive）中“千禧年研讨会”的标志自行像素化重绘，属于非官方二次创作。原标志及相关名称的权利归原权利人所有；本项目与其没有关联，也未获得授权或认可。如权利人提出要求，我们会移除或更换该素材。

文件（路径都在 `assets/wok_infantry/` 下）：

- `textures/gui/formations/millennium_seminar_mobile.png`：重绘的原图，256×256，2026-08-31 加入。
- `textures/gui/formations/millennium_seminar_mobile_64.png`、`millennium_seminar_mobile_32.png`：
  0.4.0-beta.1 新增的预缩图，由上面的原图按面积平均缩小得到，来源与上面相同。

## 界面图集

三张界面图集（ui_icons.png、ui_hatch.png、map_icons.png）是本项目原创像素图，由本地预览工具生成，不含第三方素材。

文件（0.4.0-beta.1 新增，路径都在 `assets/wok_infantry/` 下）：

- `textures/gui/ui_icons.png`（界面图标图集，45 个图标）
- `textures/gui/ui_hatch.png`（禁用斜纹）
- `textures/gui/map_icons.png`（战术地图标点图集，10 种标点）

三张图集由界面预览工具 `ui-preview` 的 `tools/export-ui-atlas.mjs` 从 `kit/icons.js`
（`MONO_ICONS`）和 `kit/map-icons.js` 中手写的字符网格像素数据导出。`ui-preview` 是本地预览工具，
不在本 Git 仓库内（本机位置 `E:\WOK步战\ui-preview`）；需要改图时在那里改源数据并重新导出，
不要直接手改 PNG。仓库内的 `wok_infantry/src/test/resources/ui_atlas/ui_atlas_manifest.json`
记录了生成器、来源说明和三张 PNG 的 SHA-256，图集测试据此核对。地图标点只参考《Squad》等作品的
信息组织方式（底板形状表示类别、白色剪影表示种类），图形本身没有复制任何作品的图标。
