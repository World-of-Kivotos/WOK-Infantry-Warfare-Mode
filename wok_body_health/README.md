# Wok步战附属-部位血量

面向 Minecraft 1.20.1 / Forge 47.4.22 的七部位血量模组。

当前版本 `0.1.0-beta.5`；产物为 `build/libs/wok_body_health-0.1.0-beta.5.jar`。

- 头部 35、胸部 85、腹部 70
- 左右手臂各 60、左右腿各 65
- 可分别配置头、胸、腹、左右臂和左右腿的最大血量
- 未启用部位抗性模式时，护甲与其他减伤先结算，本模组使用 `LivingDamageEvent` 的最终净伤
- 与 `wok_infantry_armor` 同装并启用部位抗性模式时，仅护甲列出的部位获得减伤；未覆盖命中不减伤、不耗耐久
- 头部或胸部归零死亡；腹部和四肢归零后产生失能与溢出伤害
- 已损毁的腹部或四肢再次承伤时，默认将 80% 伤害传递给其他未损毁部位
- 玩家击杀玩家时，会按最初命中的部位随机显示枪弹、普通攻击或扩散击杀文本，并保留自定义武器名称；扩散致死不会改记为最终损毁的头部或胸部
- 命中部位：TaCZ 子弹按命中坐标判定；其他弹射物按飞行轨迹与碰撞箱的交点判定；玩家近战按准星位置判定；怪物近战落在胸部（72%）或腹部（28%）
- 虚空与 `/kill` 这类无视无敌的伤害分摊到全身，头或胸归零即死亡
- 断腿降低跳跃高度（一条 65%、两条 35%）在客户端生效，断腿降低移速由服务端属性生效
- 原版红心由七部位 HUD 取代；原版生命恢复与饱食度自然回血不作用于部位血量（有意设计）
- 绝对部位血量接口 `BodyHealthApi.setAllPartsHealth`，供一血救援和新生命重置软联动使用；不受 `bodyHealScale` 影响，原有医疗接口保留
- 可选接入 `wok_trauma`；医疗包一次只治疗选定部位，黄色 Propital 的恢复量由受伤部位共享并优先用于最重伤部位，绿色 eTG-change 的每次恢复脉冲仍同时治疗全部受伤部位，流血伤害分散到全身

## 伤害分类标签

伤害怎样分到各部位由数据包标签决定，整合包可以用数据包增删条目。不在任何标签里的伤害按单个部位结算。

| 标签 | 结算方式 | 默认内容 |
| --- | --- | --- |
| `wok_body_health:explosion_spread` | 按权重分到七个部位 | `#minecraft:is_explosion`、SBW 地雷、SBW 载具爆炸 |
| `wok_body_health:leg_spread` | 双腿各一半 | 摔落、鞘翅撞墙、落在石笋上 |
| `wok_body_health:systemic_spread` | 平分给所有未损毁部位 | `#minecraft:is_fire`、溺水、饥饿、凋零、魔法、冰冻、创伤流血、SBW 白磷与电击 |
| `wok_body_health:head_hit` | 固定打头 | 下落方块、铁砧、滴水石锥、窒息、SBW 枪弹与激光爆头 |

第三方条目都写成可选项，没装对应 MOD 不影响标签加载。PvP 补刀伤害类型同时列入原版 `bypasses_armor`、`bypasses_effects`、`bypasses_enchantments` 和 `bypasses_shield`；若补刀仍被其他模组挡下，玩家会被强制判定死亡（先检查不死图腾，倒地 MOD 照常接管）。

## HUD

HUD 始终位于快捷栏左边缘以左，按屏幕宽度自动选择档位；阈值由配置的最大血量位数算出：

| 档位 | 默认配置下的逻辑宽度 | 显示内容 |
| --- | --- | --- |
| 完整 | ≥ 436 | 人形 + 七个“当前/上限” + 总血量 |
| 精简 | 352–435 | 人形 + 七个当前值 + 总血量 |
| 紧凑 | < 352 | 人形（颜色表示伤势）+ 总血量 |

人形沿用改版前的持枪方块人姿势。`tools/body_health_hud_icon.ps1` 以 `tools/body_health_hud_icon/source` 里的原 256×256 贴图为底，生成 156×192 贴图（每个界面像素 4 个贴图像素，HUD 用线性过滤绘制）：身体保留原图的面和描线；原图的枪被去掉，换成按 M4A1 真实比例绘制的步枪（提把与后照门、伸缩枪托、STANAG 弹匣、带散热孔的护木、三角准星座、鸟笼消焰器），握把在后手、护木压在前手。改图后修改源图或脚本再重跑即可覆盖 `textures/gui/body_hud*.png`。人形是正面视角：右臂、右腿在屏幕左侧，所以这两个读数在左列，左臂、左腿在右列，引线总是指向正在变色的部位。数字放在深色底牌上，不使用文字阴影；创造与旁观模式不显示。

人形下方预留一条 52×11 的伴随栏，供 `WOK步战核心` 的体力条使用。其他客户端 HUD 可通过反射调用 `com.wok.bodyhealth.api.BodyHealthHudApi.companionSlot(int screenWidth, int screenHeight)` 取得 `{left, top, width, height}`；HUD 隐藏时返回 `null`。

## 配置文件

客户端整合包与普通单人测试位于 `config/wok_body_health-common.toml`。服务器以服务器侧该文件为准，最大血量会通过同步包发送给客户端 HUD。

```toml
[part_health]
headMax = 35
chestMax = 85
abdomenMax = 70
leftArmMax = 60
rightArmMax = 60
leftLegMax = 65
rightLegMax = 65

[conversion]
destroyedPartDamageTransferMultiplier = 0.8

[armor_compatibility]
enableArmorBodyPartResistance = true
```

修改最大血量后，已初始化玩家按原血量百分比换算到新上限。`destroyedPartDamageTransferMultiplier` 只作用于命中前已经损毁的非致命部位；首次将部位打至 0 仍使用该部位原有溢出规则。关闭 `enableArmorBodyPartResistance` 或未安装独立护甲时，双方均保持各自原有逻辑并可单独运行。等离子护盾始终是全身能量屏障，不受该开关限制。

## 构建与验收

TaCZ 兼容代码编译需要 TaCZ 1.1.8 JAR（只参与编译，不进入产物，也不进入开发客户端）：

```powershell
.\gradlew.bat -p wok_body_health build "-Ptacz_dev_jar_path=E:\wok\Wok-Project\libs\tacz-1.20.1-1.1.8-hotfix.jar"
```

HUD 真实客户端验收会在 `D:\WOK步战测试\1.20.1-Forge_47.4.22\wok-body-health-acceptance\<批次>` 建隔离超平坦存档，设定固定伤势后按 320×240（紧凑）、427×240（精简）、480×270 与 960×720（完整）截图并校验布局，结果写入该目录的 `hud-test-results/result.txt` 后自动退出。每批次用新的 `hudTestRun`；加 `hudTestCoreJar` 时同时加载 WOK步战核心，检查体力条位置：

```powershell
.\gradlew.bat -p wok_body_health runHudTestClient -PhudTestRun=<批次> "-PhudTestCoreJar=<绝对路径>\wok_infantry\build\libs\wok_infantry-0.3.0-beta.2.jar" "-Ptacz_dev_jar_path=E:\wok\Wok-Project\libs\tacz-1.20.1-1.1.8-hotfix.jar"
```
