# 2026-09-06 Git 内容与版本核对

## 审计结论与统计口径

以本地最新提交 `6944d76` 为界，工作区包含 **25 项 MOD 新增功能组**，分布于核心 13 项、指挥官支援 3 项、占点 3 项、倒地救援 3 项、载具部位血量 3 项；另有 **3 项配套工具**。文件、测试、语言资源和同一功能的 UI/网络层不重复算作新功能。平衡调整与修复单列，不冒充新增。

这不是全部功能都已通过游戏验收，也不是 25 次提交。阵营框架等大量早期工作已经提交，仍被旧“未发布”日志混在一起；本次将内容分清并给漏升版本的模块补齐版本。

## 审计开始时的本地 Git 状态

本节保存上一轮版本审计时点；后续提交与推送状态以 Git 历史和 [本次更新说明](UPDATE_NOTES_2026-09-06.md) 为准。

- 当前分支：`codex/体力`；HEAD：`6944d76`，2026-08-31，`Update WOK infantry modules and test baseline`。
- 本地 `main` 在 `ddeba45`（2026-08-08）；当前分支比本地 main 多 3 次提交：`9e0a60a`、`de22a34`、`6944d76`。该范围累计影响 513 个文件，增加 77,448 行、删除 413 行；这不是本次工作区新增数量。
- 本地没有版本标签，当前分支没有上游跟踪。本轮没有 fetch，不据此判断远程是否有更新。
- `wok_commander_support/`、`wok_capture_points/`、`wok_downed/` 三个完整模块及部分核心/载具新增文件仍未加入 Git；它们不能仅靠现有提交恢复。
- 本轮只整理本地源码、构建与说明，没有提交、推送、创建标签或替换专用测试实例的活动 `mods`。历史 ZIP/JAR 保留；新旧同名内容的歧义由新版本及 SHA-256 清单消除。

## 最新提交之后的 25 项新增功能组

| 编号 | 模块 | 新增内容 | 主要源码/文档证据（仓库相对路径） |
| --- | --- | --- | --- |
| N01 | 核心 | 全阵营与管理员预设配装的全量导入/导出、预览、备份与恢复 | `wok_infantry/src/main/java/com/wok/infantry/configtransfer/`、`wok_infantry/docs/CATALOG_IMPORT_EXPORT.md` |
| N02 | 核心 | 四种车辆的大/中型运输补给、余量持久化、签名取箱与负重减速 | `wok_infantry/docs/SUPPLY_TRANSPORT_CAPACITY_LIST.md`、核心 `ammo/transport/` |
| N03 | 核心 | 500 点中型弹药箱 | 核心 `block/MediumAmmoSupplyCrateBlock.java` |
| N04 | 核心 | 大型补给站原生实体接入、7 秒预燃烧/主爆炸/余燃流程及旧模型兼容 | 核心 `ammo/LargeSupplyStationDetonation.java`、`LargeSupplyDetonationTimeline.java` |
| N05 | 核心 | 小队队包，生命值、权限、小队专属部署点与 8 分钟持久冷却 | 核心 `block/RallyRadioBlock.java`、`deployment/RallyDeploymentPoint.java` |
| N06 | 核心 | 每战局首次部署免费备用弹药，同弹种取最高上限且死亡不重置 | 核心 `ammo/AmmoSupplyService.java`、`AmmoSupplyPlanner.java` |
| N07 | 核心 | 凯撒 234机械化作战单元：豹2A4、CV90、无武装装甲 HMMWV | 核心 `formation/FormationConfigData.java` |
| N08 | 核心 | 研讨会骑兵军团：M1A2 SEP TUSK II 与 M3A3 编成 | 同上，`millenniumCavalryFormation()` |
| N09 | 核心 | 阵营过滤的临时侦察情报接口与红点显示 | 核心 `support/adapter/SupportIntelPublisher.java`、`battle/BattleService.java` |
| N10 | 核心 | 管理员按阵营与技能 ID 清除单项支援冷却 | 核心 `server/BattleCommands.java`、`support/SupportService.java` |
| N11 | 核心 | 附属地图区域覆盖接口，用于占点区域/名称/进度 | 核心 `client/map/TacticalMapAreaOverlayRegistry.java` |
| N12 | 核心 | 支援表现扩展、真实攻击范围与在途倒计时 | 核心 `client/map/TacticalSupportMapPresentationRegistry.java`、`client/screen/TacticalMapScreen.java` |
| N13 | 核心 | 逐枪伤害、穿甲与射速倍率接入 TaCZ 最终计算 | 核心 `integration/tacz/TaczAdsSpeedAdapter.java` |
| N14 | 指挥官支援 | 侦察卫星：150 格半径、30 秒扫描与阵营情报 | `wok_commander_support/README.md`、该模块 `src/main/` |
| N15 | 指挥官支援 | F-15EX JDAM 1000 磅空袭、在途/落地引信与声音 | 同上 |
| N16 | 指挥官支援 | F-16C GBU-12 500 磅空袭、同阵营光斑锁定与逐 tick 制导 | 同上 |
| N17 | 占点 | 木棍圈地、据点管理指令、区域与规则持久化 | `wok_capture_points/README.md`、该模块 `src/main/` |
| N18 | 占点 | 双方顺序解锁、连续控制与 FREEZE/ADVANTAGE 算法 | 同上 |
| N19 | 占点 | 点内 HUD、独立队伍/标签识别与可选核心地图显示 | 同上 |
| N20 | 倒地救援 | 致死转倒地、失血/补枪/放弃与倒地 HUD | `wok_downed/README.md`、该模块 `src/main/` |
| N21 | 倒地救援 | 队友救援、动作中断与医疗软联动 | 同上 |
| N22 | 倒地救援 | 拖行、速度限制与远端玩家同步 | 同上、`wok_downed/docs/DRAG_SYNC_ACCEPTANCE.md` |
| N23 | 载具部位血量 | 按车辆种类划分 T1/T2/T3 车体与部件模板 | `wok_vehicle_health/BALANCE_TIERS.md`、该模块 `balance/` |
| N24 | 载具部位血量 | 方向装甲与坦克炮伤害归一化 | 同上 |
| N25 | 载具部位血量 | TaCZ HEAT/温压弹/M72 分开结算，直击/爆炸去重 | 同上 |

此表的“核心”源码短路径均相对于 `wok_infantry/src/main/java/com/wok/infantry/`。

## 配套工具与已有内容调整

三项配套工具：T01 `tools/loadout-editor/` 离线编制配装编辑器；T02 `tools/apply_infantry_weapon_armor_balance.ps1` 枪械/护甲平衡配置应用；T03 `tools/apply_m249_stance_recoil.py` M249 姿态后坐力试调。它们不计入 25 项 MOD 功能，也不代表其外部配置已内置到 JAR。

另有以下调整与修复：地图字体/图层/友军图标，支援换阵营取消及冷却指令解析，合法装备 ID 保留，部位伤害默认换算 10→8，轻甲/中甲默认移速调整，观瞄恢复及炮塔受损转速，倒地姿态与延迟确认下的拖行位置修复。具体分类见根 `CHANGELOG.md` 当前版本章节。

已提交但此前版本未充分区分的内容包括：核心的阵营/编制与管理员装备池、职业/小队/指挥官、部署/补给/载具台账、体力、战术平板地图与支援扩展框架。它们已经在 `6944d76` 中，不算本表 N01–N25 的新增。

创伤治疗尤其需要补号：`git diff 9e0a60a HEAD -- src` 显示 0.0.2 记录后已有 7 个文件、139 行新增/21 行删除，包括黄针共享慢回和延迟副作用，但 `mod_version` 一直未变。部位血量在 `de22a34` 后也已提交 HUD 底部避让调整。

## 整理后的版本

| 模块 | 整理前源码版本 | 当前版本 | 原因 |
| --- | --- | --- | --- |
| WOK步战核心 | 0.0.1 | **0.1.0** | 大批累积功能与本次目录导入导出，配装协议 11、战局协议 18 |
| 创伤治疗 | 0.0.2 | **0.1.0-beta.1** | 为已提交的慢回/延迟副作用新增内容补号，游戏验证未齐 |
| 部位血量 | 0.0.3-beta.1 | **0.0.3-beta.2** | 伤害默认值与 HUD 调整，继续同一测试版本线 |
| 独立护甲 | 1.1.0 | **1.1.1-beta.1** | 平衡修正用补丁版本，保留 1.1 系列；修正根说明错写的 0.0.1 |
| 载具部位血量 | 0.0.1 | **0.1.0-beta.1** | T 级、方向伤害与反坦克新增功能，实射矩阵待验收 |
| 指挥官支援 | 0.0.1 | **0.1.0-beta.1** | 三种具体支援，完整组合服制导/伤害仍待验收 |
| 占点 | 0.1.0-alpha.1 | **0.1.0-alpha.2** | 纠正可选核心的最低兼容版本为 0.1.0，保留早期测试阶段 |
| 倒地救援 | 0.1.0-alpha.1 | **0.1.0-alpha.2** | 姿态/拖行同步修复从旧同号测试包中明确区分 |

离线编辑器另设 `0.1.0-beta.1`，配套核心 `0.1.0`；包名、`VERSION.json` 和 SHA-256 同时记录两者，不再附带混淆的 0.0.1 同名核心。

当前源码版本不是 GitHub Release 状态。正式版本与预发行后缀均保留各模块既有序列，不把所有模块强行改成同一个版本。

## 构建与验收结果

2026-09-06，JDK 17、离线 Gradle：八个模块的 `build` 和 `reobfJar` 全部通过。创伤首次构建因旧默认 TaCZ 缓存路径不存在而失败；改用已有的 `dist/test-export-20260905-buildlogs/tacz-1.1.8-mapped.jar` 后通过，未改动第三方依赖。

| 模块 | 本次 JUnit | 本次版本核对/依赖静态检查 | 游戏内证据与仍待完成项 |
| --- | ---: | --- | --- |
| 核心 | 296 | 通过 | 当前 0.1.0 真实独立 Forge 客户端导入导出 PASS，双分辨率 8 图；真实枪包/载具/运输及新编制整合联调未在本轮补测 |
| 创伤治疗 | 无测试源 | 通过 | 旧恢复计时检查已记录；注射、延迟副作用、完整医疗链待游戏验收 |
| 部位血量 | 无测试源 | 通过 | 伤害默认值、医疗数值与 HUD 组合需游戏确认 |
| 独立护甲 | 无测试源 | 通过 | 穿戴移速与实弹平衡需游戏确认 |
| 载具部位血量 | 18 | 通过 | 真实第三方组合射击矩阵、旧车迁移需游戏确认 |
| 指挥官支援 | 18 | 通过，但有下述架构限制 | 历史三技能 UI 验收通过；新 JDAM 爆炸链、双人移动制导与断照待正式组合服验收 |
| 占点 | 9 | 通过 | 历史 320×240/960×720 HUD/地图验收通过；多人推进、圈地、重启恢复待测 |
| 倒地救援 | 3 | 通过 | 历史姿态 19/23 项、带 150ms 延迟拖行双方 19/15 项通过；完整医疗/死亡部署链待测 |

总计 **344 项 JUnit，0 失败/错误**；无测试源的三个模块不计为有自动化功能测试。编辑器现有 **25 项 Node 测试通过**，新 ZIP CRC、文件内容与配套 JAR 校验通过；本轮没有修改编辑器界面功能或重复做其全套浏览器验收。

本次核心真实客户端结果：`D:/WOK步战测试/1.20.1-Forge_47.4.22/catalog-transfer-acceptance/20260906-v0.1.0/catalog-acceptance.txt`，`status=PASS`。覆盖服务端管理员权限、预览令牌、预览后文件变化拒绝、真实配置修改导入、备份、玩家记录保留和在线同步；未把玩家个人自定义配装引入系统。

依赖静态检查只证明 JAR 声明与打包边界符合检查器允许列表，不等同于各模块真实独立启动。指挥官支援现有实现强制依赖核心，是“所有附属可独立安装”规则尚未解决的既有例外；检查器本来就允许它，不能据 PASS 宣称完全独立。本轮修正其最低核心版本为 0.1.0，未擅自重写其架构。

## 后续版本操作

1. 新内容开始时确定所属模块的新版本；已交付后任何行为、资源或配置调整不得覆盖旧号测试包。纯文档不加号。
2. 同步模块 README、根 README、`docs/VERSIONING.md` 与根 `CHANGELOG.md` 六类说明；配置包和编辑器各自带版本。
3. 先构建核心再构建引用它的附属。占点与指挥官支援现在默认从核心 `gradle.properties` 解析准确开发 JAR，生产 GameTest 脚本亦不再写死旧版本。
4. 构建后运行 `tools/verify_versions.ps1 -ManifestPath outputs/current-versions.json`，核对文件名与真实 JAR 元数据。清单包含源码 Git HEAD、工作区是否有改动、准确产物路径与 SHA-256。
5. 完成该模块的独立启动、游戏功能和联动验收后再发布；未完成项必须保留测试说明。实际安装时一个 modId 只留一个活动 JAR。

本轮构建清单为 `outputs/current-versions-20260906.json`。因为源码尚未提交，不能只用 HEAD 认定构建内容；以该清单的每个 JAR 哈希识别当前产物。
