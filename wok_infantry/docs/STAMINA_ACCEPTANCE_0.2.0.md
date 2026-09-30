# WOK步战核心 0.2.0 体力验收

日期：2026-09-06。体力属于 `wok_infantry`，本次不升级其他附属模块。

## 行为与版本

- 腿部体力归零后立即结束冲刺并移除速度加成；双端均在原版冲刺入口拦截，持续按键、双击前进和重复冲刺请求不能绕过。
- 耗尽锁定随 NBT 保存、玩家实体替换和网络同步保留；默认恢复到 15 才解锁。创造、旁观保持豁免。
- 持 TaCZ 枪械时，手部或腿部低于 50 开始晃动；50 时强度为零，越低幅度越大。手部 40、25、10、0 的疲劳系数为 0.104、0.5、0.896、1，腿部权重保持 35%。
- HUD 在 50 以下变为橙色预警，15 及以下为红色；320×240 下向左避让快捷栏。
- 体力协议由 1 升至 2，按 0.x 兼容性规则升级至 `0.2.0`，两端须一同升级。原消耗/恢复配置保持有效；旧 NBT 无新标记时仍可读取，零体力自动锁定。

## 自动与真实客户端验证

核心 301 项 JUnit（新增 5 项）通过，覆盖渐强曲线、50 点边界、恢复门槛、禁用状态、恢复锁定往返同步以及截断/尾随数据拒绝。`build`、`reobfJar`、版本一致性和独立依赖检查通过，必需依赖仅 Forge 与 Minecraft。

构建输出为 `wok_infantry-0.2.0.jar`（1,447,480 字节）；SHA-256：`CF144C982C8FB54DDD987E4E1AB38865C3124FCDCBFC346123D6AAF5AA1EB610`。

使用实际 Forge 47.4.22 客户端和集成服务端运行验收程序，所有运行目录位于：

`D:\WOK步战测试\1.20.1-Forge_47.4.22\stamina-acceptance\`

| 场景 | 结果目录 | 结果 |
|---|---|---|
| 核心独立安装，无 TaCZ | `20260906-standalone-final` | PASS |
| 核心 + 本地 TaCZ 1.1.8，默认 M4A1 | `20260906-tacz-02` | PASS |

每个目录的 `stamina-acceptance.txt` 保留具体断言和读数，`screenshots/` 保留两档逻辑分辨率（320×240、960×720）各 50、40、25、10、0 五档体力截图。

真实输入/服务端行为检查：

1. 按住原版冲刺键可在体力充足时正常加速；自然冲刺和跳跃均可耗尽腿部体力，并立即锁定。
2. 耗尽后继续按键、双击前进、直接调用冲刺入口，以及发送实际 `START_SPRINTING` 包，均无法恢复冲刺速度加成；仍能正常步行。
3. 恢复到正数但不足 15 时客户端保持锁定；拷贝 NBT 到新玩家实体后锁定仍在；自然恢复至阈值后双端均可再次冲刺。
4. 切换创造后双端豁免生效。
5. TaCZ 客户端实际相机事件在 50 时无疲劳偏移，在 40、25、10、0 时均检测到偏移；无 TaCZ 时保持零偏移。
6. 已目视核对窄屏与大屏的体力条位置、预警色和快捷栏避让；TaCZ 枪械实际渲染正常。

本轮未覆盖完整整合包中的其他 HUD 组合，也未进行两个独立客户端的远程联机测试；网络与服务端输入检查采用真实集成服务端连接。玩家实体替换验证 NBT 锁定保留，不冒称真人退出再登录测试。

## 复验方式

从 `wok_infantry/` 运行（Java 17，Gradle 缓存中需已有 Forge 依赖及游戏资源）：

```powershell
.\gradlew.bat build --offline --no-daemon --console=plain
.\gradlew.bat runClientStaminaTest '-PstaminaTestRoot=D:/WOK步战测试/1.20.1-Forge_47.4.22/stamina-acceptance/新的独立验收目录' --offline --no-daemon --console=plain
```

验收使用现有 `run/world` 的复制件；任务要求全新目录，保留先前结果。临时管理员测试模式只用于隔离部署大厅冻结，实际体力和移动阶段使用生存模式。验收源码位于 `src/staminaTest/`，不会打入生产 JAR。

TaCZ 的发布包使用 SRG 名称，而开发客户端使用 official 名称；为保持 Mixin 的 Shadow 字段和嵌套依赖一致，使用本地映射脚本生成只改 Minecraft 成员名称的测试副本：

```powershell
python dev/remap_stamina_tacz_fixture.py 'D:/WOK步战测试/1.20.1-Forge_47.4.22/mods/tacz-1.20.1-1.1.8-release.jar' build/createSrgToMcp/output.srg ../dist/stamina-0.2.0/tacz-1.1.8-official.jar
.\gradlew.bat runClientStaminaTest '-PstaminaTestRoot=D:/WOK步战测试/1.20.1-Forge_47.4.22/stamina-acceptance/新的TaCZ验收目录' '-PstaminaTestTaczJar=../dist/stamina-0.2.0/tacz-1.1.8-official.jar' --offline --no-daemon --console=plain
```

映射脚本拒绝覆盖已存在的输出；复用已生成的副本时跳过脚本。该副本仅供开发验收，不替换实际安装的 TaCZ，不随核心分发。
