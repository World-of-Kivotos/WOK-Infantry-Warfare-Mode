# 倒地姿态与动作 MOD 兼容验收

版本追记（2026-09-06）：以下保留当时 alpha.1 的验收记录；本修复现已归入 `0.1.0-alpha.2`，历史“不调整版本号”不再作为交付规则。

## 问题与处理

测试实例的 moveslikemafuyu 1.1.3 在 `ServerEvent.serverSwim` 中发现玩家既不满足浅水游泳、又没有 `craw` 标签时，会清除 `SWIMMING` 强制姿态；它的原始按键事件也不检查倒地效果。原倒地实现仅在服务端 tick 末尾设置强制姿态，缺少客户端姿态与移动输入限制，因此会出现倒地仍站立和倒地期间可切换爬行的问题。

- 在 Forge `Player.setForcedPose` 的参数入口按倒地状态保护姿态，不依赖其他 MOD 的 tick 注册顺序。
- 服务端和本地客户端均立即设置卧倒姿态；服务端的原版实体姿态数据继续向观察者同步。
- 客户端清空移动、潜行、跳跃输入与水平速度，禁止物品交互；保留视角、聊天及放弃救援命令。
- 通过只在客户端生效的可选 Mixin 拦截 Mafuyu 原始动作按键；不依赖其类进行编译，不修改它的 JAR 或全局配置。
- 倒地时清除 Mafuyu 已有 `craw`/`slide` 标签；获救、死亡和重新生成玩家时释放倒地姿态。

## 自动真实客户端测试

测试源码位于 `src/poseTest`，不进入正式 JAR。测试使用真实集成服务端、网络同步、动作事件订阅者和客户端渲染，在步战专用测试实例下的 `wok-downed-acceptance` 隔离目录运行。

准备 `saves/downed_pose_test/level.dat` 测试世界元数据，可使用仓库已有 UI 测试世界的副本；不要使用实际游玩存档。测试会在打开副本前移除非原版维度和玩家数据，并等待原版 60 tick 登录免伤结束后才执行致死测试。独立安装测试只加载 Forge 和倒地模块。组合测试使用测试实例中实际的 Mafuyu 1.1.3 JAR，并将本地依赖副本命名为 `build/compat-inspect/moveslikemafuyu-1.1.3.jar`。

在本模块目录执行：

```powershell
..\gradlew.bat --offline test build
..\gradlew.bat --offline runPoseTestClient
..\gradlew.bat --offline runPoseTestClient '-PposeTestDir=D:/WOK步战测试/1.20.1-Forge_47.4.22/wok-downed-acceptance/mafuyu' '-PposeTestMafuyuJar=build/compat-inspect/moveslikemafuyu-1.1.3.jar'
```

检查对应目录的 `pose-test-results/result.txt`，以及 `downed.png`、`revived.png`。覆盖普通致死转倒地、服务端与客户端拒绝清除姿态、站姿覆盖、倒地时连续按爬行/疾跑/跳跃键、水平位移与碰撞箱、获救同步、姿态解锁及获救后恢复爬行。

## 2026-09-05 验收结果

- `test build` 通过，既有 3 项时序测试通过；生产 JAR 完成 `reobfJar`。
- `verify_mod_independence.ps1` 通过，生产 JAR 仅必需依赖 Forge 和 Minecraft。
- 独立安装的真实客户端测试通过 19 项断言；加入实际 moveslikemafuyu 1.1.3 后通过 23 项断言。
- 组合日志确认姿态保护以及 CrawEvent、SlideEvent、SwimEvent、ClimbEvent 的兼容 Mixin 均成功应用。
- 两组渲染截图已人工查看：倒地时身体卧倒，获救后正常站立。
- [独立安装结果](acceptance/2026-09-05-standalone.txt)、[Mafuyu 组合结果](acceptance/2026-09-05-mafuyu.txt)。截图保存在各隔离副本的 `pose-test-results/`。
- 本轮客户端为 ForgeGradle 开发运行环境；没有宣称完整整合包、生产专用服务器或第二个真实客户端已完成验收。

## 仍需组合联机验收

- 第二名真实客户端观察伤员倒地、拖行、救援和最终死亡，确认模型与碰撞箱同步。
- 倒地前处于爬行/滑铲、载具或水中，倒地时仍不能主动移动。
- 倒地后退出重连，确认继续卧倒；复活后正常行走、潜行和爬行。
- 完整整合包中的枪械动画与伤员拖行，以及 `320×240`/`960×720` HUD 的既有验收。

本次不调整版本号、配置字段或存档结构。
