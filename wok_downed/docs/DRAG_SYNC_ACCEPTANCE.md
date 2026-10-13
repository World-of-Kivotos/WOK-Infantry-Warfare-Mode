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
