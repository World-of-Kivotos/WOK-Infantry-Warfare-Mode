# WOK步战附属-占点

Minecraft 1.20.1 / Forge 47.4.22 的独立占点附属，modId 为 `wok_capture_points`。当前开发版本为 `0.1.0-alpha.2`。可选核心联动要求 `wok_infantry` 0.1.0 或更高版本，未安装核心时仍可独立运行。

## 快速设置 A/B/C/D

1. 管理员手持木棍，左击区域第一角、右击区域第二角。
2. 依次执行 `/wokcapture point set a A点`、`/wokcapture point set b B点` 等命令。
3. 用 `/wokcapture point order a 0` 至 `/wokcapture point order d 3` 调整顺序。
4. 用 `/wokcapture rules sequential true` 开启顺序占点；蓝方按低到高、红方按高到低推进。
5. 用 `/wokcapture rules duration 90` 设置全局占领时长，或用 `/wokcapture point duration a 60` 覆盖单点时长。

重新圈地后再次执行 `point set` 会移动已有据点，同时保留其顺序、时长和归属。所有据点、世界规则覆盖项和占领进度写入世界存档。

## 阵营识别

- 安装 `WOK步战核心` 时，优先采用核心权威的 BLUE/RED 阵营。
- 单独安装时，默认识别记分板队伍 `blue`、`red`。
- 也可给玩家添加 `wok_capture_blue` 或 `wok_capture_red` 实体标签。
- 队伍名和标签列表可在服务端配置中修改。

## UI 与地图

玩家进入据点后，屏幕顶部显示战术拼板风格的据点名、蓝红人数比、连续控制条、争夺/锁定/占领状态和人数速度倍率。安装 `WOK步战核心` 时，据点区域、名称、归属颜色和进度会显示在 WOK 战术地图中；未安装核心时占点与 HUD 仍可独立工作。

## 主要指令

- `/wokcapture point list|info <id>|remove <id>`
- `/wokcapture point owner <id> neutral|blue|red`
- `/wokcapture point enabled <id> true|false`
- `/wokcapture point duration <id> <秒>|default`
- `/wokcapture point reset <id>`
- `/wokcapture rules status|duration <秒>|sequential <布尔值>|defaults`
- `/wokcapture selection clear`
- `/wokcapture sync`

所有管理指令和木棍圈地均要求权限等级 2。

## 服务端配置

配置项包括默认用时、是否顺序占点、计算/同步频率、最大人数速度倍率、争夺模式 `FREEZE`/`ADVANTAGE`、选择工具、据点数量与区域轴长上限、创造模式玩家是否计数、全服播报以及独立运行时的队伍映射。

## 构建

先构建仓库内的 `wok_infantry` 开发 JAR（仅用于编译可选地图接口），再从本目录使用仓库根 Gradle Wrapper：

```powershell
cd wok_infantry
..\gradlew.bat jar
cd ..\wok_capture_points
..\gradlew.bat test jar
```

成品位于 `wok_capture_points/build/libs/wok_capture_points-0.1.0-alpha.2.jar`；核心 class 不会被打进该 JAR，运行时不安装核心也可加载。
