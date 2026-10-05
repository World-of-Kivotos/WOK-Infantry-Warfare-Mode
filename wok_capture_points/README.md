# WOK步战附属-占点

Minecraft 1.20.1 / Forge 47.4.22 的独立占点附属，modId 为 `wok_capture_points`。当前开发版本为 `0.1.0-alpha.4`。可选核心联动要求 `wok_infantry` 0.1.0 或更高版本，未安装核心时仍可独立运行。

## 快速设置 A/B/C/D

1. 管理员手持木棍，左击区域第一角、右击区域第二角。
2. 依次执行 `/wokcapture point set a A点`、`/wokcapture point set b B点` 等命令。
3. 用 `/wokcapture point order a 0` 至 `/wokcapture point order d 3` 调整顺序。
4. 用 `/wokcapture rules sequential true` 开启顺序占点；蓝方按低到高、红方按高到低推进。
5. 用 `/wokcapture rules duration 90` 设置全局占领时长，或用 `/wokcapture point duration a 60` 覆盖单点时长。

重新圈地后再次执行 `point set` 会移动已有据点，同时保留其顺序、时长和归属。所有据点、世界规则覆盖项和占领进度写入世界存档。

## 阵营识别

仅存活、未倒地且非旁观玩家参与占点；安装核心时还要求 ACTIVE 部署。与核心 `0.3.0-beta.1` 或更新版本联合使用时，新局统一重置进度，本局结束后暂停占领。占点和全占均不扣兵力；未安装核心时仍可独立占点。

- 安装 `WOK步战核心` 时，优先采用核心权威的 BLUE/RED 阵营。
- 单独安装时，默认识别记分板队伍 `blue`、`red`。
- 也可给玩家添加 `wok_capture_blue` 或 `wok_capture_red` 实体标签。
- 队伍名和标签列表可在服务端配置中修改。

## UI 与地图

玩家站进据点后才显示据点 HUD（0.1.0-alpha.4 起不再画 alpha.3 那块 330×52 的大面板）：

- 与核心 `0.5.0-beta.2` 或更新版本同装、且核心客户端配置 `hud.showBattleStrip` 开着（默认）时，据点由核心画进顶部兵力战况条：中间一个“B 62%”小牌（左贴边颜色表示正在占领的一方，争夺、停用、本方无资格各有样式），宽屏第二行写“据点名 · 状态 · 剩余 m:ss”。本附属自己什么都不画。
- 其余情况（未装核心、核心早于 0.5.0-beta.2、或核心关闭了战况条）由本附属画一条薄条：顶部居中、宽不超过 200、一行“据点名 │ 蓝方人数 ▬|▬ 红方人数”（进度条从中线向领先一方伸出，蓝左红右），宽屏（布局宽 ≥400 且高 ≥280）第二行写状态与剩余时间。有核心时放进核心 `InfantryHudApi.slot("top_center_next", 宽, 高)` 给的位置（核心早于 0.4.0-beta.1 时按 alpha.3 面板的位置：宽屏居中于 GUI y 28，GUI 宽 ≤360 时在名单右侧 x 140 起、y 26），单独安装时贴顶；原版 Boss 条真的压到它时移到 Boss 条下面（核心已把 Boss 条挪到它下方时不跟着动，避免两边互相推；核心 0.5.0-beta.2 起，薄条起点在第一条 Boss 条那一行时由核心把 Boss 条挪到它下面，薄条不动）；GUI 缩放 1 的大窗口按 2× 画。文字无阴影，底板配色与核心 HUD 一致（颜色常量在本附属内定义，不依赖核心）。

安装 `WOK步战核心` 时，据点区域、名称、归属颜色和进度还会显示在 WOK 战术地图中；未安装核心时占点与 HUD 仍可独立工作。

### 给核心的只读接口（0.1.0-alpha.4）

`com.wok.capturepoints.api.CaptureHudApi`（仅客户端、按类名反射调用，不需要链接本附属的类）：

- `currentPoint()`：玩家所在据点的只读 `Map<String, Object>`，不在据点里时为 `null`。键：`version`（1）、`id`、`name`、`shortName`（不超过 3 个字符的 id 转大写，否则为显示名）、`control`（−1 红方占满 … 1 蓝方占满）、`percent`（`|control|` 百分比，未真正占满前最多 99）、`leading` / `owner` / `capturing`（`neutral|blue|red`）、`bluePlayers`、`redPlayers`、`enabled`、`blueAllowed`、`redAllowed`、`speed`、`captureSeconds`、`remainingSeconds`（正在占领时到占满的秒数，否则 −1）、`state`（`disabled|contested|locked|capturing|secured|neutral`）、`status`（本附属语言文件翻译的状态 `Component`）。同一次同步内返回同一个 Map 实例。
- `panelRect(int, int)`：本附属薄条的 `{left, top, width, height}`（GUI 像素），不画时（含核心接管时）为 `null`。核心 0.4.0-beta.1 起据此让开名单、战况条和 Boss 条。
- 本附属反射调用核心的 `InfantryHudApi.rendersCapturePoints()`（核心 0.5.0-beta.2 新增）：为 true 时不画薄条；方法不存在或调用失败时只记一次日志并改画薄条。

兼容：新核心 + 本版＝据点并入战况条；核心 0.5.0-beta.1 或更早 + 本版＝本附属薄条（放进核心槽位，核心照 `panelRect` 让开）；只装本附属＝薄条；核心关闭战况条＝薄条。新核心 + 占点 alpha.3＝alpha.3 的大面板 + 核心照旧避让。

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

成品位于 `wok_capture_points/build/libs/wok_capture_points-0.1.0-alpha.4.jar`；核心 class 不会被打进该 JAR，运行时不安装核心也可加载。

`..\gradlew.bat runUiTestClient` 在核心的 `run/ui-test` 里截 6 张图（HUD 与战术地图各两档，再加关闭核心战况条后的两张薄条图；HUD 图都带一条只在客户端的原版 Boss 条），并核对据点由谁画、薄条四个 tick 内不再移动且在屏幕上半部；加 `-Pinfantry_dev_jar_path=<旧核心 JAR>` 可对旧核心跑同一套验收。
