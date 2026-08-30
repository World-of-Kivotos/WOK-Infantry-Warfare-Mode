# Wok步战附属-部位血量

面向 Minecraft 1.20.1 / Forge 47.4.22 的七部位血量模组。

- 头部 35、胸部 85、腹部 70
- 左右手臂各 60、左右腿各 65
- 可分别配置头、胸、腹、左右臂和左右腿的最大血量
- 未启用部位抗性模式时，护甲与其他减伤先结算，本模组使用 `LivingDamageEvent` 的最终净伤
- 与 `wok_infantry_armor` 同装并启用部位抗性模式时，仅护甲列出的部位获得减伤；未覆盖命中不减伤、不耗耐久
- 头部或胸部归零死亡；腹部和四肢归零后产生失能与溢出伤害
- 已损毁的腹部或四肢再次承伤时，默认将 80% 伤害传递给其他未损毁部位
- 玩家击杀玩家时，会按最初命中的部位随机显示枪弹、普通攻击或扩散击杀文本，并保留自定义武器名称；扩散致死不会改记为最终损毁的头部或胸部
- TaCZ 子弹按命中坐标判定部位，爆炸与环境伤害按规则分配
- 原版红心由七部位 HUD 取代
- 可选接入 `wok_trauma`；医疗包一次只治疗选定部位，黄色 Propital 的恢复量由受伤部位共享并优先用于最重伤部位，绿色 eTG-change 的每次恢复脉冲仍同时治疗全部受伤部位，流血伤害分散到全身

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

在工作区根目录构建：

```powershell
.\gradlew.bat -p wok_body_health build
```
