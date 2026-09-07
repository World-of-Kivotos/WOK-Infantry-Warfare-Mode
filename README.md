# WOK步兵战争模式

> [!WARNING]
> **当前仓库仅为早期测试版本，不是正式发布版。** 功能、配置格式、存档结构、物品行为和联机协议仍可能发生不兼容调整；请仅用于开发与测试，并在更新前备份世界、配置和测试实例。

独立于“WOK计划”的 Minecraft 1.20.1 / Forge 47.4.22 步兵战争模式项目。

当前仓库包含以下模块；除指挥官支援需要步战核心外，其他附属按各自 README 声明的基础依赖安装：

| 模块 | 正式名称 | 当前版本 | 源码目录 |
| --- | --- | --- | --- |
| `wok_infantry` | WOK步战核心 | `0.1.0` | `wok_infantry/` |
| `wok_trauma` | WOK步战附属-创伤治疗 | `0.1.0-beta.1` | 仓库根目录 |
| `wok_body_health` | WOK步战附属-部位血量 | `0.0.3-beta.2` | `wok_body_health/` |
| `wok_infantry_armor` | WOK步战附属-独立护甲 | `1.1.1-beta.1` | `wok_infantry_armor/` |
| `wok_vehicle_health` | WOK步战附属-载具部位血量 | `0.1.0-beta.1` | `wok_vehicle_health/` |
| `wok_commander_support` | WOK步战附属-指挥官支援 | `0.1.0-beta.1` | `wok_commander_support/` |
| `wok_capture_points` | WOK步战附属-占点 | `0.1.0-alpha.2` | `wok_capture_points/` |
| `wok_downed` | WOK步战附属-倒地救援 | `0.1.0-alpha.2` | `wok_downed/` |

`WOK` 与 `WOK步战` 是两个不同产品线；本仓库不属于 WOK 本体项目。

版本号由各模块独立维护，每次交付新增内容或改变已有内容都必须换号，测试包同样适用；纯文档修改不升级。发布规则见
[`docs/VERSIONING.md`](docs/VERSIONING.md)，历史更新见 [`CHANGELOG.md`](CHANGELOG.md)。
Git 内容清单、版本归属及未完成验收见 [`2026-09-06 版本核对`](docs/CONTENT_VERSION_AUDIT_2026-09-06.md)。
本次 Git 递交的更新内容见 [`2026-09-06 更新说明`](docs/UPDATE_NOTES_2026-09-06.md)。
WOK步战附属专用客户端的当前依赖、地图后端和回滚基线见
[`docs/WOK_INFANTRY_TEST_ENVIRONMENT.md`](docs/WOK_INFANTRY_TEST_ENVIRONMENT.md)。

## 当前效果

- `wok_trauma:pain`（疼痛）：显示脉动式视野失焦叠层。
- `wok_trauma:bleeding`（流血）：每秒损失 0.5 颗心，约每 5 秒在脚边生成一处小型血迹。
- `wok_trauma:major_bleeding`（大出血）：每秒损失 1 颗心，约每 3 秒生成一处大型血泊。
- `wok_trauma:tremor`（颤栗）：连续疼痛 20 秒后触发；TaCZ 枪械散布和开镜耗时都会增加，并产生纯视觉的第一人称手部颤抖。

小血迹会存在 30–50 秒，大型血泊会存在 45–60 秒；两者均在消失前 10 秒逐渐淡出，
且不会保存到世界存档。

TaCZ 枪弹命中默认必定造成 30 秒疼痛，并互斥判定 10% 大出血与 35% 普通流血。
同一攻击者在同一游戏刻内的重复命中只判定一次，避免霰弹瞬间重复触发。

## 医疗包部位选择

与 `wok_body_health` 同时安装时，手持任意医疗包，按住蹲下键并滚动鼠标滚轮，可在“自动、头部、胸部、腹部、左臂、右臂、左腿、右腿”之间循环选择。选择时快捷栏不会滚动，当前目标显示在动作栏。

- 手动模式严格治疗所选部位；目标满血时不会转移到其他部位，也不会因回血消耗耐久。
- 自动模式依次检查头部、胸部、腹部，最后治疗四肢中血量比例最低的部位。
- 一次持续使用始终锁定开始使用时的目标，必须松开并再次使用才能治疗另一个部位。
- 未安装 `wok_body_health` 时不接管滚轮，医疗包保留原逻辑。

## 回血针剂联动

与 `wok_body_health` 同时安装时，黄色 Propital 的每次恢复使用一份共享治疗量，优先用于当前伤势最重的部位；参照《逃离塔科夫》的 2 秒注射、`+1 HP/s`、持续 300 秒设计，并按本项目默认 `bodyHealScale = 10` 换算为每秒恢复 0.1 点原版治疗量（即 1 点部位血量），全程共恢复 300 点部位血量。注射 270 秒后会产生 30 秒颤栗与视野收缩。绿色 eTG-change 继续在每次脉冲中分别治疗全部受伤部位，保持紧急全身恢复定位。这套逻辑不占用医疗包的单部位治疗选择。未安装 `wok_body_health` 时，两种针剂继续恢复原版生命值。

## 配置

首次启动后生成 `config/wok_trauma-common.toml`，可调整：

- 枪伤疼痛时长
- 普通流血概率
- 大出血概率
- 颤栗散布倍率
- 颤栗开镜耗时倍率

## 测试命令

```mcfunction
/effect give @s wok_trauma:pain 30
/effect give @s wok_trauma:bleeding 30
/effect give @s wok_trauma:major_bleeding 30
/effect give @s wok_trauma:tremor 30
```

## 构建

需要 64 位 JDK 17：

```powershell
$env:JAVA_HOME='C:\path\to\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat build
```

开发运行需要一份 ForgeGradle 已映射的 TaCZ 1.1.8-hotfix JAR。默认从 ForgeGradle
缓存中寻找，也可以通过 `-Ptacz_dev_jar_path=<path>` 指定。

构建产物位于 `build/libs/wok_trauma-0.1.0-beta.1.jar`。
