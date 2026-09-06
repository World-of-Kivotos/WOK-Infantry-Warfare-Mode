# WOK步战附属-指挥官支援

当前产物 `build/libs/wok_commander_support-0.1.0-beta.1.jar`，最低核心版本为 `0.1.0`。开发构建自动读取核心 `gradle.properties`，不再固定使用旧同名包。

Minecraft 1.20.1 / Forge 47.4.22 独立附属模块，modId 为
`wok_commander_support`，当前版本 `0.1.0-beta.1`。本模块强制依赖
`WOK步战核心`（`wok_infantry`），但不依赖创伤治疗、部位血量、独立护甲、
TaCZ 或任何具体载具 MOD。Create Big Cannons 只作为空袭弹体的可选软依赖，
卓越前线只作为爆炸实现的可选软依赖；缺失时模块和侦察卫星仍可启动，
CBC 缺失会令两种空袭条目明确显示不可用。卓越前线缺失时 F-15EX 安全回退
CBC HE 爆炸，必须使用火炮指示器的 F-16C 宝石路支援则显示不可用。

## 侦察卫星

- 仅当前阵营指挥官能在战术地图的“支援”页呼叫。
- 点选一个扫描中心；服务端校验其所在维度、世界边界和半径 150 格范围内的全部区块已经加载。
- 冷却 10 分钟，按阵营和技能 ID `wok_commander_support:recon_satellite` 持久化；更换指挥官或重启服务器不会刷新冷却。
- 呼叫后立即扫描，此后每 5 秒刷新一次，共覆盖 30 秒（6 个刷新窗口）。
- 扫描同维度、圆形水平范围内已部署且存活的敌方玩家；玩家正在乘坐实体时只显示所乘载具的接触点，避免两个红点重叠。
- 同时扫描带有 WOK 编制 ownership 的敌方载具，包括当前无人乘坐的已部署载具。
- 结果仅出现在调用方阵营的战术地图中，统一显示为不可手动选择/删除的红点；目标离开区域后，旧点会在一个刷新间隔后自然消失。
- 红点是运行期临时情报，不占玩家手工标记额度、不写世界存档，服务器重启后不会残留。

## 千禧年 F-15EX 杰达姆 1000磅空袭

- 稳定技能 ID 为 `wok_commander_support:millennium_f15ex_jdam_1000lb`；新生成配置仅向学院军的 `millennium_seminar_mobile`（“千禧年研讨会机动部队”）开放。
- 现任阵营指挥官在战术地图选择一个点即可呼叫；服务端重新校验维度、世界边界、以目标点为中心的 32 格执行区和区块加载状态。
- 冷却暂定 15 分钟，按阵营和技能 ID持久化；更换指挥官或重启服务器不会刷新冷却。
- 呼叫成功后先进入 200 tick（10 秒）服务端在途倒计时；战术地图显示支援正在接近，倒计时期间不生成航弹实体。
- 第 10 秒生成真实的 Create Big Cannons `createbigcannons:he_shell`，位置精确位于目标正上方 200 格；飞行阶段关闭物理碰撞偏航，不会再擦着地形斜向滑翔。
- 飞行计划按 CBC 线性阻力补偿；20 tick（1 秒）后把同一枚炮弹精确收束到地表并静止，播放沉重触地声，再启动 40 tick（2 秒）延迟引信。
- 呼叫后约第 13 秒优先调用卓越前线 0.8.9 的 `CustomExplosion.Builder`：1000 点爆炸伤害、32 格半径、`GIANT` 爆炸粒子，并沿用卓越前线服务端的方块破坏配置和 Forge 爆炸事件链；调用成功后移除 CBC 弹体，绝不叠加第二次爆炸。
- 卓越前线未安装或运行时接口不兼容时，才把同一枚 CBC HE 炮弹的倒计时归零作为保底爆炸，避免 JDAM 变成哑弹。飞行与爆炸阶段继续叠加 DVIDS 公有领域 F-15E 靶场实录，来源和变换记录见 `THIRD_PARTY_NOTICES.md`。
- 弹体需要 Create Big Cannons `5.11.4` 至 `6.0.0` 之前版本；卓越前线 `0.8.9+` 为可选爆炸后端。两者都只通过注册 ID或反射软联动，不把第三方类打进成品。

## F-16C GBU-12 宝石路 II 500磅精准空袭

- 稳定技能 ID 为 `wok_commander_support:f16c_gbu12_paveway_500lb`；新生成的普通编制与千禧年编制均默认开放，冷却暂定 15 分钟。
- 指挥官只在战术终端点选许可区中心，服务端校验同维度、世界边界、已加载区块以及中心周围 64 格完整许可区；地图点击本身不是激光目标。
- 呼叫后有 10 秒在途时间。释放瞬间，服务端从同阵营在线玩家中选取一名正在主手持续使用卓越前线 `superbwarfare:artillery_indicator` 的制导员；敌方、旁观者、死亡玩家和照射点在许可区外的玩家均不参与。
- 多名友军同时照射时，选取落点最接近许可区中心的一名并锁定其 UUID，后续不会在不同光斑间跳转。制导员可以不是指挥官，也不需要站在许可区内，只要求同阵营、同维度且 512 格服务端射线确实命中许可区。
- 航弹使用真实 CBC `createbigcannons:he_shell`，从斜上方进入并飞行 60 tick（3 秒）。服务器每 tick 重做方块/实体射线，持续照射移动玩家或载具时，航弹会按最新命中点修正弹道。
- 制导员松开使用键、切换物品、离线、死亡、换维度、变更阵营或把照射点移出许可区后，制导立即永久断开；航弹保持最后一次合法修正完成余下飞行，不会自动换人或重新捕获。
- 最终在最后一个合法光斑触发卓越前线 `CustomExplosion`：500 点伤害、16 格半径和 `LARGE` 粒子。卓越前线爆炸接口临时失败时归零同一枚 CBC HE 的倒计时保底，不生成第二枚航弹。
- 释放时没有有效友军持续照射则不生成航弹，并向指挥官明确提示；这属于已受理呼叫，已提交的阵营冷却不会返还。

## 编制权限

新生成的普通 WOK步战默认编制开放侦察卫星与 F-16C 宝石路空袭；新生成的
`millennium_seminar_mobile` 还开放其专属 F-15EX JDAM 空袭。已有服务器的
`config/wok_infantry/formations.json` 不会被强制改写；千禧年编制应配置为：

```json
"support": {
  "mode": "allow_list",
  "allowList": [
    "wok_commander_support:recon_satellite",
    "wok_commander_support:millennium_f15ex_jdam_1000lb",
    "wok_commander_support:f16c_gbu12_paveway_500lb"
  ]
}
```

`mode: all` 会按管理员意图绕过默认专属白名单并允许该编制使用服务器注册的全部支援；要保持千禧年专属，请让其他编制维持 `allow_list` 且不要加入 F-15EX JDAM ID。

## 管理员冷却指令

权限等级 2 的管理员可清除一个阵营、一个稳定支援 ID 的持久冷却：

```text
/battle admin support cooldown clear <blue|red> <支援ID>
```

例如立即恢复蓝方 JDAM：

```text
/battle admin support cooldown clear blue wok_commander_support:millennium_f15ex_jdam_1000lb
```

该指令只清除冷却，不取消正在执行的 30 秒侦察或其他在途任务。

## 构建

先构建同仓库核心，再构建本模块：

```powershell
cd ..\wok_infantry
.\gradlew.bat jar
cd ..\wok_commander_support
..\gradlew.bat build
```

构建只把核心 JAR 作为开发依赖，不会将其类打进本模块成品。

## 验证

- JDK 17 单元测试通过，模块 18 项 JUnit 与核心 278 项 JUnit 测试全部通过；新增覆盖 F-16C 稳定 ID、200 tick 在途、60 tick 逐 tick 制导、64 格许可区、512 格精确指示器 ID/射线合同、移动光斑修正、CBC 阻力补偿及卓越前线 500/16/`LARGE` 爆炸合同。
- 独立依赖检查通过；成品仅包含本模块类，强制依赖 `wok_infantry`，CBC 保持可选依赖。
- 既有官方生产组合验收覆盖同一枚 `createbigcannons:he_shell` 的落地静止和 CBC 伤害链；本次改为 200 tick 在途延迟、200 格高度、20 tick 垂直急坠、40 tick 引信及卓越前线爆炸后端，仍需在正式 Create 6.0.8 + Create Big Cannons 5.11.4 + 卓越前线 0.8.9 组合服重新确认完整视听与伤害效果。
- Forge 47.4.22 + JourneyMap 6.0.2 真实客户端验收通过：320×240 显示紧凑 JDAM 按钮，960×720 显示完整名称；两档均无文字越界或控件重叠。
- 侦察证据保存在 `outputs/recon_satellite_validation/`，JDAM 生产运行与 UI 证据保存在 `outputs/jdam_airstrike_validation/`。
- Forge 47.4.22 + JourneyMap 6.0.2 真实客户端三技能 UI 验收通过：320×240 下 F-16C 按钮为 `56×17`，960×720 下为 `249×24`，均未越界或遮挡。
- F-16C 的实际双人持续照射、移动目标及断照保持末次修正仍需在正式组合服完成；userdev GameTest 因 Create 生产混淆 Mixin 与官方映射环境不兼容而未进入测试，正式服务端验收脚本又因本机缺少隔离 Forge 服务端运行库在启动前退出，因此当前不得把单元、构建或 UI 结果表述为已完成游戏内制导验收。
