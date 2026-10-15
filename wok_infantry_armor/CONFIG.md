# WOK步战附属-独立护甲：配置说明

适用版本：独立护甲 `1.3.0-beta.1` 起。本文只讲 `WOK步战附属-独立护甲`（modId `wok_infantry_armor`），不涉及 WOK-本体护甲。

本文随模组打包在 JAR 根目录（用压缩软件打开 `wok_infantry_armor-*.jar` 即可看到 `CONFIG.md`），源码位于 GitHub 仓库 `World-of-Kivotos/WOK-Infantry-Warfare-Mode` 的 `wok_infantry_armor/CONFIG.md`。

## 1. 两个配置文件

| 文件 | 作用 |
| --- | --- |
| `wok-infantry-armor.toml`（主配置） | 按“等级 × 轻中重”矩阵和材质整类调：R/Q/G/T 四张 18 格矩阵、构型移速、材质的耐久与泄漏系数、头盔整类耐久与子弹磨损倍率、等离子护盾（`plasmaShield.balanceV4.<型号>`）。 |
| `wok-infantry-armor-items.toml`（逐件配置，1.3.0 新增） | 54 件插板 + 17 件头盔，每件一节，可以单独覆盖等级/类型/材质、防护数值、耐久、磨损倍率、移速和额外属性。写 `"default"` 的项沿用主配置算出的值。 |

- 两个文件都是服务端配置，在 `saves/<世界>/serverconfig/`（专用服务器是 `world/serverconfig/`）。每个世界各一份，进入世界时自动生成；联机时服务器把它们发给客户端，用于提示框和耐久条。
- 主配置的键名在 1.3.0 中一个都没有改动，已有世界的主配置原样保留。
- `defaultconfigs/wok-infantry-armor-items.toml`（整合包根目录下的 `defaultconfigs`）会被复制进**所有还没有该文件的世界**——包括已有世界第一次升级到 1.3.0 时——之后不再覆盖。整合包作者可以用它分发平衡方案。
- 修改后保存即可：服务器运行中会自动热重载，服务端判定在下一 tick 内生效。已联机的客户端提示框/耐久条不会刷新，需要重新进入服务器。
- **在运行中的服务器上改之前，先把文件复制一份备份**（两个文件都一样）。运行中保存了 TOML 语法错误的文件（例如漏了引号），热重载会失败；这时必须在关服或退出单人世界**之前**改好，否则 Forge 关服时会把文件里坏行以下的内容全部删掉。详见第 2 节规则 2。不想冒这个险，就关服后再改。

## 2. 怎么改逐件配置

每节的节名是完整注册名（游戏里按 F3+H 打开高级提示框就能看到物品 ID）：

```toml
	#LBT 6094A Slick 插板背心（wok_infantry_armor:plate_armor_slick） | VI级 中型 装甲钢 | 主配置矩阵第17项 | 缓冲R=0.9648 抗穿Q=0.56 防护率G=0.8944 抗压T=147.2 耐久=760 机动=-8% | 部位=胸部
	[plates.plate_armor_slick]
		tier = "default"
		weight = "default"
		material = "default"
		ballisticProtectionR = "default"
		armorPiercingBufferQ = "default"
		generalProtectionG = "default"
		pressureCapacityT = "default"
		coverage = "default"
		durability = "default"
		wearMultiplier = 1.0
		movementModifier = "default"
		attributeModifiers = []
```

规则：

1. **只改英文双引号里面的内容，引号不要删。** 数字和百分数也可以写在引号里，例如 `"0.9"`、`"85%"`、`"-10%"`；直接写不带引号的数字（`0.9`）也可以。
2. 文字一定要带引号：`tier = "IV"`、`material = "ceramic"`、`coverage = ["chest", "abdomen"]`。写成 `tier = IV` 是 TOML 语法错误，后果看服务器当时是否在运行：
   - **服务器没在运行时改坏**（带着坏文件启动服务器或进入单人世界）：**整个世界无法加载**。把引号补上，或者删掉这个文件让它重新生成。
   - **服务器运行中改坏**：这次热重载失败，日志里只有 Forge 的英文报错 `Failed loading config file wok-infantry-armor-items.toml`（主配置则是 `wok-infantry-armor.toml`），`/wokarmor problems` 里看不到。服务器暂时继续用改之前的值（例外：如果上次加载之后还没有用到过护甲配置，例如专用服务器上没有玩家在线，坏行以下的护甲会立刻按 `"default"` 处理）。**必须在关服或退出单人世界之前把文件改好并保存。** Forge 读文件时会先清空内存里的配置，读到坏行就停下，所以内存里只剩坏行以上的部分；关服/退出世界时 Forge 会把这份残缺的配置写回文件。坏行以下所有节的覆盖（连同坏掉的那一行）都会从文件里消失，下次启动全部变回 `"default"`，这时已经没有可以补的引号了，只能用备份恢复。另外，文件坏着的期间**新玩家进不了服务器**：服务器把文件原样发给客户端，客户端解析失败，进服就会失败。
   - 所以：在运行中的服务器上修改前先备份文件；保存后看一眼服务端日志有没有 `Failed loading config file`，有就立刻改好；或者干脆关服后再改。主配置 `wok-infantry-armor.toml` 同样如此。
   - **在已有的节里原地改那一行。** 生成的文件里每件护甲的节（`[plates.<id>]`、`[helmets.<id>]`）和每个键都已经存在：找到那一节，把对应行 `= ` 后面的值换掉。不要在文件末尾再写一个同名的节，也不要在同一节里再加一行同名的键——两者都是 TOML 语法错误，后果同上（服务器没运行时世界无法加载；运行中改坏时热重载失败，必须在关服前改好，否则坏行以下的内容会被 Forge 删掉）。日志里的报错是英文的 `Table with path [plates, plate_armor_slick] has been declared twice`（节重复）或 `entry "[tier]" defined twice in its table`（键重复）；看到它们就删掉重复的那一节或那一行。
3. `"default"`（不区分大小写）= 沿用主配置按等级/类型/材质算出的值。
4. 默认没有引号的两个键 `wearMultiplier` 和头盔的 `movementModifier` 直接写数字最省事（`wearMultiplier = 0.5`、`movementModifier = -0.05`）；写成带引号的数字（`"0.5"`）也可以，头盔的 `movementModifier` 还接受百分数（`"-5%"`），`wearMultiplier` 不接受百分数。这两个键**超出范围时夹到边界**（例如 `wearMultiplier = 500` 按 100 处理），其余键超出范围时回退默认值。
5. 全角符号可以用：`，`、`％`、全角空格、全角数字会先换成半角。
6. 只要写的是文字或数字，写错的值（不是数字、`nan`/`inf`、超出范围、未知等级/材质/部位……）都不会被 Forge 改掉，文件保持原样；游戏按该项的默认值处理（第 4 条的两个键越界时夹到边界），并在服务端日志里打一条 WARN，写明完整键路径、原值和原因。用 `/wokarmor problems` 可以随时列出。这条对逐件配置的所有键都成立，包括 `wearMultiplier` 和头盔的 `movementModifier`；`attributeModifiers` 另有要求：整体必须是方括号列表，列表里每条是带引号的文字（列表里写错的文字条目同样只是被忽略并列入 `/wokarmor problems`）。
   - 例外是下面几种情况，由 Forge 自己处理并重写文件，**不会**出现在 `/wokarmor problems` 里：
     - 键名或节名拼错（和 Forge 生成的不一样，例如把 `wearMultiplier` 写成 `wearMultipler`，或把 `[helmets.helmet_altyn_heavy]` 写成 `[helmets.helmet_altyn]`）：Forge 认不出这个键或这一节，会把这一行（或整节）删掉，再把正确的键按默认值补回去，你写的值就丢了。所以只改 `=` 后面的值，不要重新输入键名或节名。
     - 单个键写成其他类型（例如 `tier = true`）：Forge 把这个键改回默认值。
     - `attributeModifiers` 列表里有不带引号的条目（例如 `["generic.luck add 1", 0.5]`）：Forge 只删掉这些条目，其余带引号的条目保留。
     - `attributeModifiers` 没写方括号（例如 `attributeModifiers = "generic.luck add 1"`）：它不是列表，Forge 把整个键改成 `[]`，这条属性就没了。`attributeModifiers` 只有一条也必须写成 `["generic.luck add 1"]`，不像 `coverage` 那样接受单个字符串。
   - Forge 改写时留下的痕迹取决于当时服务器是否在运行：
     - **服务器启动或进入世界时**（例如关服后改坏再启动）：日志逐条 WARN `Incorrect key <键路径> was corrected from <原值> to its default, <默认值>`，文件直接被改写，**不生成备份**，原值只留在这行日志里。
     - **服务器运行中热重载时**：日志只有一条 `Configuration file <路径> is not correct. Correcting`，**不逐条列出**是哪个键被改；同时在同一目录生成 `.bak` 备份（例如 `wok-infantry-armor-items-1.toml.bak`），保存改写前的文件。
     - 因为启动时的改写不留备份，修改前请自己先复制一份文件。同目录里已有的 `.bak` 可能是更早某次热重载留下的，用它恢复前先核对内容，免得丢掉之后的其他修改。
7. **不要在文件里写自己的注释**，Forge 加载/重载时会删掉。平衡记录请写在别处。
8. 每节第一行注释是按“代码默认主配置”算出的默认值；如果本世界的主配置改过，以主配置为准（看游戏内提示框或 `/wokarmor inspect`）。头盔数值是满耐久时的标称值。

### 键 ↔ 提示框

| 键 | 提示框 | 取值 |
| --- | --- | --- |
| `tier` | 等级 | `I`~`VI`（不区分大小写，也接受 `1`~`6`） |
| `weight` | 类型 | `light` / `medium` / `heavy`，也接受 轻/中/重、轻型/中型/重型 |
| `material` | 材质 | `uhmwpe` `aramid` `armor_steel` `combined` `aluminum` `titanium` `ceramic`，也接受中文材质名（超高分子聚乙烯、芳纶、装甲钢、复合材料、铝、钛、陶瓷） |
| `ballisticProtectionR` | 缓冲（普通弹道段） | 0 ~ 0.99，或 `"0%"`~`"99%"` |
| `armorPiercingBufferQ` | 抗穿（穿甲段，仅子弹穿甲等级 ≤ tier 时生效） | 同上 |
| `generalProtectionG` | 防护率（非 TaCZ 物理伤害） | 同上 |
| `pressureCapacityT` | 抗压 | 0 ~ 10000，不接受百分数 |
| `coverage`（仅插板） | 防护部位 | `"default"`、`["chest", "abdomen"]` 或 `"chest,abdomen"`；部位 `chest` `abdomen` `left_arm` `right_arm` `left_leg` `right_leg` 或中文名（胸部、腹部、左臂、右臂、左腿、右腿）；不能写 `head`，不能为空 |
| `durability` | 耐久上限 | 1 ~ 100000 的整数（`"250.0"` 可以，`"250.5"` 不行） |
| `wearMultiplier` | （无） | 0 ~ 100 的数字（可加引号，不接受百分数），磨损倍率，1 = 不变；越界夹到边界 |
| `movementModifier`（插板） | 机动修正 | `"default"`、数字或百分数：-0.95 ~ 1.0（`-0.08`、`"-0.08"` 或 `"-8%"` = 机动修正 -8%）；越界回退默认 |
| `movementModifier`（头盔） | 机动修正 | 数字或百分数：-0.95 ~ 1.0（`-0.08` 或 `"-8%"`），默认 `0.0`（写 `"default"` 也是 0）；越界夹到边界 |
| `attributeModifiers` | 额外属性 | 字符串列表，只有一条也要写方括号（`["generic.luck add 1"]`），不接受单个字符串；见第 5 节 |

比率键遇到大于 1 的数字（例如把 85% 写成 `"85"`）视为写错，不会自动当成百分数。

### 分类键影响什么

数值覆盖优先于分类：某一项写了数字，分类键就不再影响这一项。

| 键 | 插板 | 头盔 |
| --- | --- | --- |
| `tier` | 选主配置矩阵的行；子弹穿甲判定 | 选矩阵行；子弹穿甲判定 |
| `weight` | 选矩阵的列；基础移速 | 选矩阵列；默认耐久（轻 60 / 中 80 / 重 180） |
| `material` | 泄漏系数、抗压系数、默认耐久、移速惩罚 | 泄漏系数、抗压系数（不影响耐久和移速） |

注册时绑定的原版护甲材质（穿戴音效、原版图层名）不随 `weight` 改变。

## 3. 计算公式

### 防护数值

没有覆盖时（i = 等级序号 × 3 + 类型序号，即“主配置矩阵第 i+1 项”）：

- R = max(0, 1 − (1 − 主配置R[i]) × 材质.ballisticLeak)；Q、G 同理分别用 `armorPiercingLeak`、`generalLeak`。基础值为 0 时保持 0。
- T = 主配置T[i] × 材质.pressureCapacity。
- 覆盖时，写的数字就是最终有效值，不再乘材质系数。
- 头盔的 R/Q/G/T 还要乘耐久状态效率 c × (2 − c)，c = 剩余耐久 / 耐久上限（插板始终为 1）。

一次伤害如何减免：

| 伤害类型 | 结果 |
| --- | --- |
| TaCZ 子弹普通段（`tacz:bullet`、`tacz:bullet_void`） | X × (1 − R) |
| TaCZ 子弹穿甲段（`tacz:bullet_ignore_armor`、`tacz:bullet_void_ignore_armor`） | 子弹穿透等级 > 护甲 `tier`：X 全额；否则 X × (1 − Q) |
| 生物/玩家近战、原版弹射物（箭、三叉戟、雪球等）、爆炸（`mob_attack`、`player_attack`、`#is_projectile`、`#is_explosion`） | X − min(X, T) × G |
| 其他（摔落、火焰（含带 `#is_fire` 的弹射物，即烈焰人小火球、恶魂火球的直接命中）、凋零（含凋零之首 `wither_skull` 的直接命中）、魔法、龙息、荆棘、仙人掌、带 `#bypasses_armor` 的伤害、没有上述原版标签的其他模组枪械等） | 不减免 |

“不减免”这一行的排除项**先于**上面几行判定：`minecraft:fireball`、`minecraft:unattributed_fireball` 虽然也带 `#is_projectile`，但同时带 `#is_fire`，`minecraft:wither_skull` 被单独排除，所以这三种直接命中不管 G/T 调多高都是全额伤害。只有恶魂火球、凋零之首的爆炸部分（`minecraft:explosion`）按爆炸行减免。

穿透等级由 TaCZ 穿甲比例换算：≥0.60 为 VI，≥0.50 为 V，≥0.35 为 IV，≥0.20 为 III，≥0.10 为 II，其余 I。VI 级护甲不会被击穿。

### 磨损

先算出现行的整数磨损 legacy：

- 插板，普通物理伤害：max(1, floor(X / 4))；TaCZ 子弹（一弹结算一次）：同一公式作用于“普通段 + 穿甲段”。
- 头盔，普通物理伤害：max(1, ceil(X))；TaCZ 子弹：max(1, ceil((普通段 + 穿甲段 × armorPiercingWearMultiplier) × ballisticWearScale))，两个倍率在主配置 `plateArmor.helmetIntegrity` 里（默认 2 和 1）。

再乘逐件 `wearMultiplier`（记作 m）：m = 1 时就是 legacy；m = 0 时不磨损；其余情况 s = legacy × m，磨损 = floor(s)，再以 s 的小数部分为概率多扣 1 点。所以 m = 0.5 时平均磨损正好减半，即使每发只扣 1 点也一样（一半概率扣 1、一半概率不扣）。

耐久是存在物品上的“已损耗值”，上限是实时读取的：调低上限会让已经超过新上限的护甲在下一 tick 碎掉（穿在身上的立刻碎，放在背包里的穿上时碎）；调高上限等于白送耐久。

### 移速

- 插板没有覆盖时：主配置 `plateArmor.movement.<类型>` − 材质 `movementPenalty`，不夹取（与 1.2.0 相同）。覆盖后用写的数字。
- 头盔默认 0，不沿用类型或材质；写了数字才有移速修正。
- 所有移速修正都是 MULTIPLY_TOTAL，**彼此相乘**：最终速度 = 基础 × (1 + 胸甲) × (1 + 头盔) × (1 + 部位血量腿伤) × (1 + 补给搬运) × ……。为了不把人钉在原地，若 (1 + 胸甲) × (1 + 头盔) < 0.05（胸甲槽是电浆护盾时按护盾配置的移速算），会自动把头盔这一项收紧到乘积正好 0.05（胸甲自身已低于 0.05 时，头盔减速归零）；头盔的加速不受影响。

## 4. 覆盖部位什么时候生效

只有同时满足下面三条才按部位判定：安装了 `WOK步战附属-部位血量`、它的护甲部位开关开启、本次是局部命中（子弹、近战等能确定部位的伤害）。此时头部命中只看头盔，其余部位只看胸甲的 `coverage`。

否则（没装部位血量、开关关闭、爆炸/摔落等非局部伤害）由胸甲保护全身；没有胸甲时由头盔保护全身——此时头盔的 R/Q/G/T 和磨损也作用于躯干和腿部命中。平衡时请同时考虑这两种情况。

## 5. 额外属性（增益减益）

`attributeModifiers` 每条写 `"<属性 id> <操作> <数值>"`，三段之间可以用半角/全角逗号或空格分隔：

```toml
		attributeModifiers = ["generic.knockback_resistance add 0.1", "generic.attack_damage multiply_total -5%"]
```

- 必须是方括号列表，每条都带引号；只有一条也写成 `["generic.luck add 1"]`。这里不像 `coverage` 那样接受单个字符串：写成 `attributeModifiers = "generic.luck add 1"` 会被 Forge 改成 `[]`，列表里不带引号的条目会被 Forge 删掉，这两种情况都不会出现在 `/wokarmor problems` 里（见第 2 节规则 6）。
- 属性 id 不写命名空间时默认 `minecraft:`。
- 操作：`add` / `addition` / `+` = 加法；`multiply_base` / `base` = 乘基础值；`multiply_total` / `total` / `mul` = 乘总值。不区分大小写。
- 数值：乘法可以写百分数（`-5%` = -0.05），加法不行。乘法不能低于 -0.95，加法绝对值不超过 1024。
- 同一属性、同一操作写多条会相加合并，合并后也要在上面的范围内。
- 被拒绝的属性：`generic.movement_speed`（请改用 `movementModifier`）、`generic.armor`、`generic.armor_toughness`（穿插板时原版护甲值和韧性会被清零，写了也没用）。
- 属性必须存在、而且玩家身上有这个属性，否则忽略。被忽略的条目不显示在提示框里，会写进日志和 `/wokarmor problems`。
- 插板的额外属性在胸甲槽穿着可用插板时生效，头盔的在头盔槽穿着可用头盔时生效；护甲耗尽、脱下即移除。

可用的玩家属性：

| 属性 id | 含义 |
| --- | --- |
| `generic.knockback_resistance` | 击退抗性（1 = 完全不被击退） |
| `generic.attack_damage` | 近战攻击伤害 |
| `generic.attack_speed` | 攻击速度 |
| `generic.attack_knockback` | 攻击击退 |
| `generic.luck` | 幸运 |
| `generic.max_health` | 最大生命值（见下方限制） |
| `forge:swim_speed` | 游泳速度 |
| `forge:step_height_addition` | 额外跨越高度 |
| `forge:entity_gravity` | 重力（坠落加速度） |
| `forge:block_reach` | 方块交互距离 |
| `forge:entity_reach` | 实体交互距离 |

`generic.max_health` 在安装部位血量时无效（部位血量每 tick 都把原版血量顶满）；没有安装时，增加的上限在重新进入世界或复活后不会自动回满。想调生存能力请用 R/Q/G/T。

## 6. 平衡示例

**下面的示例都不是要粘贴进文件的新内容。** 每件护甲的节和所有键在生成的文件里都已经有了：先在文件里找到示例标出的那一节（例如搜索 `[plates.plate_armor_slick]`），再把那一节里**已有的**对应行改成“改后”的样子，其余行不动。不要把节名（方括号那一行）或这些行再写一遍——重复的节或键是 TOML 语法错误，服务器没运行时会让世界无法加载，运行中改坏则必须在关服前改好（见第 2 节规则 2）。

**削弱 Slick 的普通弹道缓冲（96.48% → 93%），其余不动。** 在 `[plates.plate_armor_slick]` 节里：

```toml
# 改前
		ballisticProtectionR = "default"
# 改后
		ballisticProtectionR = "93%"
```

**PACA 软甲磨损减半，并且只护胸。** 在 `[plates.plate_armor_paca]` 节里：

```toml
# 改前
		coverage = "default"
		wearMultiplier = 1.0
# 改后
		coverage = ["chest"]
		wearMultiplier = 0.5
```

**把 Gladiator-S 维京改成轻型（与 WOK-本体的调整一致）。** 只改分类，其余仍写 `"default"`，数值会自动换到“V 级轻型陶瓷”那一格：缓冲 92.48% → 90.6%，抗穿 42.8% → 34%，防护率 84% → 78%，抗压 110.4 → 96.6，机动 -5.5% → -0.5%；耐久仍按陶瓷 420。在 `[plates.plate_armor_gladiator_s_viking]` 节里：

```toml
# 改前
		weight = "default"
# 改后
		weight = "light"
```

**阿尔金重型头盔：加 10% 击退抗性，戴着慢 5%，耐久提到 240。** 在 `[helmets.helmet_altyn_heavy]` 节里：

```toml
# 改前
		durability = "default"
		movementModifier = 0.0
		attributeModifiers = []
# 改后
		durability = "240"
		movementModifier = -0.05
		attributeModifiers = ["generic.knockback_resistance add 0.1"]
```

（`# 改前`/`# 改后` 只是本文的标注，不要写进配置文件；写进去也会在下次加载时被 Forge 删掉，见规则 7。）

## 7. 管理命令

需要权限等级 2（OP），只在服务端执行、读服务端配置：

- `/wokarmor inspect`：显示自己胸甲槽、头盔槽护甲的解析后设置——中文名、ID、等级/类型/材质（覆盖过的标“（覆盖）”）、R/Q/G/T、耐久上限、磨损倍率、机动修正、覆盖部位、额外属性（含被忽略的条目和原因）。
- `/wokarmor inspect <物品>`：显示指定护甲，例如 `/wokarmor inspect wok_infantry_armor:helmet_6b47`；控制台也能用。
- `/wokarmor problems`：列出逐件配置里所有无效值（键路径、原值、原因），没有就显示“没有问题”。

## 8. 已知限制

- 等离子护盾不在逐件配置里，仍用主配置 `plasmaShield.balanceV4.<型号>` 逐型号调。
- 头盔“耐久 → 防护效率”曲线、子弹穿透等级阈值、伤害类型白名单仍是代码常量，不可配置。
- 重新分类 `weight` 不会改变穿戴音效和原版图层名（注册时绑定）。
- 已有的属性修正 UUID 与 WOK-本体护甲相同，两者仍不能装在同一个整合包里；1.3.0 新增的修正都用新的 UUID。
- 服务器热重载后，已联机客户端的提示框和耐久条不刷新，需要重新进入服务器；服务端判定即时正确。
- 客户端断开连接后会丢掉上一个服务器的逐件配置；之后进入没有逐件配置的服务器（例如 1.2.0）时，提示框按默认值显示。
- 网络协议版本没有变，所以 1.2.x 的客户端仍能进入 1.3.0 的服务器，不会报错；但 1.2.x 不认识逐件配置文件，收不到它，提示框和耐久条都按整类默认值显示（例如服务器把阿尔金重型头盔耐久改成 240，1.2.x 客户端仍显示 /180，耐久条在 180 处就显示见底），而服务端判定按 240 走。客户端请和服务器一起升级到 1.3.0。
- 运行中的服务器上保存了带 TOML 语法错误的配置文件时，热重载失败、服务器暂时沿用旧值，但 Forge 在关服或退出世界时会把只剩坏行以上内容的配置写回文件，坏行以下的覆盖全部丢失、下次启动变回 `"default"`，本模组不会另行报告；文件坏着的期间新玩家无法进服。运行中修改前请先备份，改坏了要在关服前改好（见第 2 节规则 2）。
- 拼错的键名或节名会被 Forge 删掉并恢复默认值，不会出现在 `/wokarmor problems` 里（见第 2 节规则 6）。
- 同物品合成/铁砧修复、经验修补附魔书、保护附魔书仍能绕过耐久和防护调整。
- 耐久是绝对损耗值：调低上限会让超过新上限的已穿戴护甲在下一 tick 碎掉，调高上限等于修复。
- `generic.max_health` 在安装部位血量时无效；未安装时，增加的上限在重进或复活后不会自动回满。
- `/config showfile wok_infantry_armor SERVER` 只会显示最后注册的逐件配置文件；两个文件都在 `saves/<世界>/serverconfig/`。
- `defaultconfigs/wok-infantry-armor-items.toml` 会被复制进所有还没有该文件的世界（包括已有世界第一次升级到 1.3.0 时），之后不再覆盖。
- 主配置四张矩阵（R/Q/G/T）必须正好 18 个数；R/Q/G 每个数须满足 0 ≤ x < 1，T ≥ 0。超出范围的数（例如 `1.0`、`-0.1`、`nan`、`inf`）会被 Forge 直接从列表里删掉并改写文件，列表因此不足 18 个时，**整张矩阵的 18 格全部**改用代码默认值（不只是写错的那一格，你对这张矩阵其他格的修改也暂时失效），并在每次加载/重载后打一次 ERROR；把缺的数补回 18 个后保存即可恢复。
