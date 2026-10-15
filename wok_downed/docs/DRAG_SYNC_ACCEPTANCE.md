# 拖行位置同步回归

版本追记（2026-09-06）：以下保留当时 alpha.1 的验收记录；本修复现已归入 `0.1.0-alpha.2`，历史“不调整版本号”不再作为交付规则。

## 原因与修复

原拖行逻辑在 `PlayerTickEvent.END` 内调用 `ServerPlayer.teleportTo`。这个 API 会向伤员发送位置包，但事件仍处于 `ServerGamePacketListenerImpl.tick` 的 `player.doTick()` 内部，随后原版会用 `firstGoodXYZ` 把玩家位置恢复到 tick 开始的记录。

这会使实体跟踪器读到旧位置。拖行每 tick 都产生新的传送确认编号，在较高延迟下，旧确认又无法及时更新服务端记录，因此拖人者看到伤员模型停在原地，即使伤员本人已经收到了移动包。

修复在拖行设置位置后调用原版公开的 `connection.resetPosition()`，同步更新 `firstGoodXYZ` 和 `lastGoodXYZ`。服务端实体跟踪与伤员自己的传送包使用同一位置；保留原有倒地姿态、移动限制、拖行速度、距离与停止条件。

## 真实双客户端测试

`src/dragTest` 是独立测试源码集，不进入正式 JAR。Host 是拖人者，同时观察远端伤员模型；Guest 是被拖伤员，通过真实 TCP 连接到 `127.0.0.1:25576`。只延迟 Guest 的传送确认包 150ms，模拟确认跨越多个服务端 tick 的情况，不修改生产网络协议。

测试覆盖：直线拖行、转弯、两端可见位置、卧倒姿态、放下后拖人者继续走动、伤员停留原地，以及获救后恢复站姿。两端分别保存截图与断言结果。

在步战专用测试实例 `wok-downed-acceptance/<run>/host/saves/downed_drag_test/` 准备仅含原版维度的测试世界副本，创建同级 `guest` 工作目录；两个目录的 `options.txt` 均设置 `pauseOnLostFocus:false`。每轮使用新的 `<run>` 名称，避免沿用旧结果。

在本模块目录分别启动两个终端：

```powershell
..\gradlew.bat --offline runDragTestHost '-PdragTestRun=<run>' '-PposeTestMafuyuJar=build/compat-inspect/moveslikemafuyu-1.1.3.jar'
..\gradlew.bat --offline runDragTestGuest '-PdragTestRun=<run>' '-PposeTestMafuyuJar=build/compat-inspect/moveslikemafuyu-1.1.3.jar'
```

省略 `poseTestMafuyuJar` 可运行只安装倒地模块的版本。完成后检查 `<run>/host-result.txt`、`guest-result.txt` 和两端 `*-phase-*.png`。进程退出码或单纯构建成功不足以判定通过；两份结果均须以 `status=PASS` 开头。

本轮修复不调整版本号、配置、存档结构或必需依赖。

## 2026-09-05 原实现复现证据

在同时安装 moveslikemafuyu 1.1.3、伤员确认包延迟 150ms 的双客户端中，原实现出现稳定的不一致：伤员本人的位置已到 `(0.5021, 101.05, 8.9627)`，而拖人者看到的远端模型仍在 `(0.5, 101.0, -0.65)`，相差约 9.6 格。

[原实现拖人者结果](acceptance/2026-09-05-drag-baseline-host.txt) 与 [原实现伤员结果](acceptance/2026-09-05-drag-baseline-guest.txt) 保留同一时段的具体坐标及失败断言。

## 修复后验收结果

- 同样安装 moveslikemafuyu 1.1.3，并延迟传送确认 150ms，两个真实 Forge 客户端全部通过：拖人者侧 19 项断言，伤员侧 15 项断言。
- 直线拖行后，拖人者看到的伤员为 `(0.5, 101.05005, 8.96265)`，伤员本人为 `(0.5, 101.05, 8.96274)`，差异仅为原版跟踪包量化误差。
- 转弯后的双方位置、放下后留在原地、卧倒姿态和救起后的站姿均通过；已查看对应客户端渲染截图。
- [修复后拖人者结果](acceptance/2026-09-05-drag-fixed-host.txt)、[修复后伤员结果](acceptance/2026-09-05-drag-fixed-guest.txt)。完整截图位于步战测试实例 `wok-downed-acceptance/drag-verified-20260905/`。
- 本轮使用 ForgeGradle 开发客户端和真实回环 TCP；完整整合包及跨机器专用服务器测试仍需在对应环境复测。

## 0.1.0-alpha.4：拖起伤员后放不下来（2026-10-10）

用户反馈“人会黏在人物屁股上，放不下来”。

### 原因

原实现每 tick 把伤员传送到拖拽者**视线方向**正后方 `dragFollowDistance` 格。拖拽者转身时伤员同步转到新的背后，准星永远落不到伤员身上；而放下只能靠“潜行右键点击伤员”，所以拖起后既放不下，也无法开始救治。上面 2026-09-05 的测试直接调用 `stopDraggingByParticipant` 放下伤员，没有经过玩家输入，所以没有暴露这个问题。

### 修复

- 绳索式跟随（`DragLeash`）：超过跟随距离才沿两人连线把伤员拉回该距离，距离以内不动；转身不再甩动伤员。
- 轻按潜行放下（`SneakTap`）：开始手势的潜行松开后，按下并在 10 tick 内松开潜行即放下；松开时才放下，潜行右键点击伤员仍是开关。
- 拉动时检查伤员碰撞箱：被方块占住就依次抬高 0.55、1.05 格，再缩短一半绳长，最后放在拖拽者脚下。
- 三维距离超过“救援距离与跟随距离中较大者 + 1 格”时自动解除拖行。

### 测试改动

`src/dragTest` 改为全程使用拖拽者客户端的真实输入，上面 2026-09-05 一节描述的是旧流程：

1. 拖拽者面朝伤员，对准后按住潜行 6 tick，期间用 `KeyMapping.click` 触发一次右键，开始拖行；松开潜行 40 tick 后拖行仍在（开始手势的潜行不算轻按）。
2. 向南走 80 tick、再向西走 80 tick：两端看到的伤员与服务端位置一致，伤员在身后，与拖拽者水平距离保持 1.15 格（±0.05）。
3. 拖拽者原地转身，准星固定对准伤员 40 tick：服务端伤员位置不变（≤0.01 格），拖拽者客户端 `hitResult` 仍是伤员。准星在本阶段开始时算好后不再跟随，原实现会把伤员移出这个准星。
4. 轻按潜行 4 tick：拖行解除，拖拽者移速修正被移除。
5. 拖拽者走开 60 tick：伤员留在原地；最后救起伤员，两端看到站姿。

运行方法同上一节（每轮新的 `<run>` 目录、先启动 Host 再启动 Guest）。

### 结果

- 修复版（最终代码 `release-final2-20261010`，未装 moveslikemafuyu，测试端实例也已停用它）：拖拽者 33 项、伤员 18 项全部 PASS；已查看两端第 4、6、8、11、12 阶段截图。[拖拽者结果](acceptance/2026-10-10-release-fixed-host.txt)、[伤员结果](acceptance/2026-10-10-release-fixed-guest.txt)。改配置注释与版本号之前的同一逻辑（`release-fixed-20261010`）也全部通过；`release-final-20261010` 一轮伤员客户端没收到服务端已发出的任何 Forge 握手包，30 秒登录超时，没有执行到检查，换新目录重跑即通过。
- 原实现对照（`release-baseline-20261010`，同一测试，只把 `isDragged` 临时改为公开）：第 8 阶段 FAIL。拖拽者转身面向东后，伤员被从 x=-7.96 传送到 x=-10.27（拖拽者身后），准星落在地面（`BlockHitResult`），即用户看到的现象。[拖拽者结果](acceptance/2026-10-10-release-baseline-host.txt)、[伤员结果](acceptance/2026-10-10-release-baseline-guest.txt)。
- 截图位于步战测试实例 `wok-downed-acceptance/` 下的同名目录。整合包与多人联机下的楼梯、墙角、梯子、手持 TaCZ 枪械等情形仍需在测试端真实客户端验收。
