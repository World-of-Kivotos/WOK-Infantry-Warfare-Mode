# WOK步战核心：阵营与编制框架

本文记录 `WOK步战核心`（modId：`wok_infantry`）当前已经落地的阵营—编制框架。它描述的是代码现状与扩展边界；除已经确认的“千禧年研讨会机动部队”和源码安全默认项外，尚未定案的编制、枪械和载具名称不应写死在代码或本文中。

## 1. 三级选择与阵营共享编制模型

玩家进入服务器后按以下顺序选择：

```text
公开阵营（学院军 / 凯撒）
    -> 编制大类别（步兵 / 装甲 / 摩步 / 机械化 / 特种）
        -> 该阵营下的具体编制候选
            -> 阵营投票锁定一个共享结果
                -> 小队、兵种、配装、部署、载具与支援规则
```

- 第一级是公开阵营 `FactionDefinition.id`，默认骨架为 `academy` 与 `caesar`。
- 每个公开阵营映射到一个既有内部战斗侧 `battleSide`。当前只接受 `blue` 或 `red`，且同一战斗侧只能有一个公开阵营。
- 第二级是固定的 `FormationCategory`：`infantry`、`armored`、`motorized`、`mechanized`、`special`。类别只负责组织目录，实际规则全部写在具体编制上。
- 第三级是该公开阵营下的 `FormationDefinition.id`。同名具体编制可以分别出现在不同阵营下；查找时始终携带所属战斗侧。
- 阵营投票框架按 blue/red 分开保存候选、选票、票数、是否允许改票与锁定结果；胜出结果属于整个阵营。底层 ledger 不擅自决定截止、最高票或同票规则，而由之后确认的比赛流程显式触发。
- 玩家选择阵营后可以保留“已有阵营、尚无编制”的存档状态；此时不能加入小队、选择配装或部署。管理员锁定结果后，服务端一次性给当时该阵营的所有成员应用同一个具体编制。锁定后晚加入者如何处理仍等待产品规则确认。
- 投票候选、个人选票和锁定结果保存在主世界 `SavedData` 中，服务器重启不丢失；目录热重载会移除失效候选和选票，锁定结果失效时整张票失败关闭；战局重置清空双方投票。
- 客户端只提交目录代数、公开阵营 ID 与编制 ID。服务端重新查目录、依赖、人数和锁定状态，客户端不能提交内部战斗侧、容量、兵种配额或物品。
- 现有兼容选择入口仍保留到投票结算策略接线完成；最终玩家只选择阵营并为具体编制投票，不再各自持有不同的生效编制。
- 玩家状态由世界存档保存。正常重连、死亡与换维度不会清空阵营/编制；尚在投票等待、旧存档缺失编制或目录调和清除编制的玩家会回到选择流程。管理员移出玩家会清除其本局归属，战局重置会清空全局战局状态。
- 小队的完整键是 `(battleSide, formationId, callsign)`。不同编制即使都使用 `alpha`，成员、队长、容量、兵种占位、权限、快照与踢出冷却也彼此隔离。指挥官仍是战斗侧级别，而不是编制级别。

目录每次重载都会增加 `generation`。携带旧代数的选择请求会被拒绝并要求刷新，避免客户端根据过期配置入队。

## 2. 配置文件与 ID 边界

编制目录位于服务端实例的：

```text
config/wok_infantry/formations.json
```

它是 Forge 服务端配置文件，不是数据包文件。文件第一次不存在时，服务端生成安全默认骨架并通过临时文件原子落盘。已有文件若读取/JSON 解析失败或最终没有有效阵营，重载会失败关闭：保留原文件、当前内存目录和 generation，不安装默认回退，也不清理玩家 roster。解析成功后的规范化结果只在内存中生效，不自动重写已有配置；管理员应根据日志中的 diagnostics 手工修正原文件后再次重载。

ID 分为两类：

- `faction.id`、`formation.id`、`classId`、`callsign` 和 `vehicle.id` 是 `wok_infantry` 内部稳定 ID，不是资源位置，不能带冒号。除 callsign 外，格式为最多 64 个小写字母、数字、`_`、`-`、`.`；显示文字放在 `displayName`。
- `vehicle.entityId`、编制 `icon` 和 TaCZ 枪械 NBT 中的 `GunId` 才是 Minecraft 资源位置，必须采用 `namespace:path`。载具当前支持 `superbwarfare` 与 FCP 的 `fcp` namespace。

已经进入存档或被其他配置引用的稳定 ID 不宜改名。只修改 `displayName` 或 `description` 不会改变身份。

## 3. `formations.json` schema

### 根对象

| 字段 | 类型 | 当前约束 |
| --- | --- | --- |
| `version` | integer | 当前规范版本固定为 `2`；旧 version 1 目录在内存中迁移 |
| `factions` | array | 最多读取 16 项；因 `battleSide` 不得重复且只支持 blue/red，当前最多有两个有效公开阵营 |

### 阵营对象

| 字段 | 类型 | 当前约束 |
| --- | --- | --- |
| `id` | string | 公开阵营稳定 ID |
| `displayName` | string | 最长 40 字符；空值回退到 ID |
| `description` | string | 最长 512 字符 |
| `battleSide` | string | 仅 `blue` 或 `red`，同侧重复项会被移除 |
| `enabled` | boolean | 没有任何有效且启用的编制时会自动停用 |
| `maxPlayers` | integer | 1–40 |
| `formations` | array | 最多 32 个，阵营内 `id` 唯一 |

### 编制对象

| 字段 | 类型 | 当前约束 |
| --- | --- | --- |
| `id` | string | 编制稳定 ID |
| `displayName` | string | 最长 40 字符 |
| `description` | string | 最长 512 字符 |
| `icon` | string | 可选 GUI 纹理资源位置；空值表示不显示编制徽标 |
| `category` | string | 五种固定大类别 ID 之一 |
| `enabled` | boolean | `classes` 或 `squads` 没有有效项时自动停用 |
| `capacity` | integer | 1 到所属阵营 `maxPlayers`，且全局上限 40 |
| `capabilities` | object | 兵站、队包、重生与支援策略；具体数值按具体编制填写 |
| `classes` | array | 最多 64 条，`classId` 唯一 |
| `squads` | array | 当前最多 5 条，callsign 唯一 |
| `vehicles` | array | 最多 64 条，`id` 唯一；空数组不要求安装卓越前线 |

### 编制能力规则

`capabilities.outpost` 与 `capabilities.rally` 使用同一套可销毁部署物 schema：

- `enabled`：是否允许该部署物。
- `maxActive`：阵营兵站上限或每小队队包上限，范围 0–64。
- `squadLeaderCanPlace`、`commanderCanPlace`：队长与指挥官放置权限。已确认兵站允许两者放置；队包由队长放置。
- `maxHealth`：方块权威血量；0–1,000,000。伤害来源和友伤规则尚未确认。
- `placementCooldownSeconds`、`replacementCooldownSeconds`、`destructionCooldownSeconds`：放置、替换及被毁后恢复 CD，范围 0–86,400 秒。轻步兵营可使用较短值，但当前不写死数值。

`capabilities.respawn`：

- `delaySeconds: -1` 继承现有全局 15 秒；0–3,600 覆盖为具体编制等待时间。服务端部署状态机已经读取此值。
- `mobileSpawnVehicleIds` 引用同一具体编制的 `vehicles[].id`；空数组表示只能从固定部署点重生。摩步/机械化的默认规则保持一个兵站加车辆运输，只有未来明确配置的具体载具才成为移动重生点。

`capabilities.support`：

- `mode` 为 `all`、`none` 或 `allow_list`。
- `allowList` 使用完整支援资源 ID（例如 `wok_infantry:some_support`），只在 `allow_list` 模式生效。
- 支援目录和请求入口都会执行该编制策略；`none` 可明确表达某个具体编制完全没有支援技能。

### 兵种规则

```json
{
  "classId": "assault",
  "displayName": "突破手",
  "squadLimit": 8,
  "allowedEntries": {
    "primary": ["entry_id_defined_in_loadouts"]
  }
}
```

- `displayName` 是当前编制独有的职业名称，最多 40 个字符；相同 `classId` 在另一个编制中可以使用完全不同的名称。
- `squadLimit` 为 1–8，是该职业在单个小队中的上限；管理员终端保存时会同步当前编制各小队对此职业的名额。
- 每个可选编制必须至少包含一个职业；`classes` 列表第一项就是该编制的默认职业。玩家入队、离队、旧职业被删除或名额调和时会回到这个职业，因此它在每个小队中的有效限额必须至少等于该小队容量，否则整个编制会失败关闭。
- `allowedEntries` 的键应使用实际配装槽：`primary`、`secondary`、`melee`、`gadget_one`、`gadget_two`、`throwable`。
- 槽位键缺失或对应数组为空表示该槽允许 `loadouts.json` 中此兵种的全部已配置条目；要做严格限制，必须填写非空白名单。
- 每个兵种最多 16 个槽位规则，每个槽位最多 64 个条目 ID。非空白名单若规范化后没有任何有效 ID，整条兵种规则会被移除，避免意外退化成“全部允许”。
- 权限等级 2 管理员可按 `U` 或执行 `/loadoutadmin`，在终端顶部选择阵营与编制后进入“职业管理”，新建、重命名、调整名额或删除当前编制职业。新职业具有独立的底层装备池，只会出现在当前编制的小队职业页和配装页。包括 `assault` 在内的任意职业都可删除，但每个编制至少保留一个；删除列表第一项时，下一项自动接任默认职业并获得覆盖小队容量的安全名额。
- 选择职业与槽位后可用“仅此 / 加入 / 移出 / 全部开放”维护白名单。“读取主手”会保留手中成品枪的 TaCZ 配件 NBT，并把新条目直接加入当前编制；从空白名单首次读取会自动建立严格白名单，而不是继续允许全部条目。

上面的 `entry_id_defined_in_loadouts` 只是 schema 占位符，不是当前正式枪械条目。

### 小队规则

```json
{
  "callsign": "alpha",
  "displayName": "Alpha",
  "capacity": 8,
  "classLimits": {
    "assault": 8,
    "support": 2
  }
}
```

- 当前 callsign 只支持 `alpha`、`bravo`、`charlie`、`delta`、`echo`，容量为 1–8。
- `classLimits` 的键必须引用同一编制 `classes` 中的 `classId`，值为 0 到该小队容量。值为 0 可禁用该小队中的某兵种。
- 未写入 `classLimits` 的兵种回退到其 `squadLimit`，最终有效值仍不超过小队容量和全局小队上限 8。
- 未列在当前编制 `squads` 中的 callsign 不能创建或加入。

### 载具规则

```json
{
  "id": "vehicle_slot_id",
  "displayName": "载具显示名",
  "entityId": "fcp:actual_registered_entity_path",
  "offsetX": 0.0,
  "offsetY": 0.0,
  "offsetZ": 0.0,
  "yaw": 0.0,
  "replenishmentCooldownSeconds": -1
}
```

- `id` 是该编制内部的稳定分配 ID；`entityId` 必须是实际已注册的卓越前线实体资源位置。
- 三个偏移必须是有限数值且绝对值不超过 256；`yaw` 会规范化到 `[0, 360)`。
- `replenishmentCooldownSeconds` 是实体真正损毁后的补充时间；`-1` 表示本战局该槽位损毁后永久耗尽，非负值按服务端秒计时。冷却使用主世界 SavedData 持久化，重启不会重置；管理员重复执行部署命令也不能绕过永久损失或冷却。
- 冷却到期后，核心在该阵营当前实体载具部署箭头重建该单一槽位。方块缺失、区块未加载、阵营/箭头状态与存档不一致、空间不安全或 provider 失败时保留台账并在后续周期重试，不会提前消费补充资格，也不会退回玩家主基地生成。
- 任何一个载具条目依赖缺失、namespace 不受支持或实体未注册，都会让普通玩家无法选择整个编制，而不是跳过坏条目。
- `vehicles: []` 时不触发卓越前线依赖，因此 `wok_infantry` 仍可独立安装。

## 4. 源码生成的默认目录

首次启动会为双方生成 `default` 常规安全编制，并在学院军中加入已经确认的首个正式编制 `millennium_seminar_mobile`。下面 JSON 展示双方共用的 `default` 骨架；正式机动部队的差异项紧随其后列出，未确认的枪械与其他编制仍不写入默认目录。

```json
{
  "version": 2,
  "factions": [
    {
      "id": "academy",
      "displayName": "学院军",
      "description": "学院军战场阵营",
      "battleSide": "blue",
      "enabled": true,
      "maxPlayers": 40,
      "formations": [
        {
          "id": "default",
          "displayName": "常规编制",
          "description": "默认步兵常规编制",
          "category": "infantry",
          "enabled": true,
          "capacity": 40,
          "capabilities": {
            "outpost": { "enabled": false, "maxActive": 0, "squadLeaderCanPlace": false, "commanderCanPlace": false, "maxHealth": 0, "placementCooldownSeconds": 0, "replacementCooldownSeconds": 0, "destructionCooldownSeconds": 0 },
            "rally": { "enabled": false, "maxActive": 0, "squadLeaderCanPlace": false, "commanderCanPlace": false, "maxHealth": 0, "placementCooldownSeconds": 0, "replacementCooldownSeconds": 0, "destructionCooldownSeconds": 0 },
            "respawn": { "delaySeconds": -1, "mobileSpawnVehicleIds": [] },
            "support": { "mode": "none", "allowList": [] }
          },
          "classes": [
            { "classId": "assault", "squadLimit": 8, "allowedEntries": {} },
            { "classId": "support", "squadLimit": 2, "allowedEntries": {} },
            { "classId": "engineer", "squadLimit": 2, "allowedEntries": {} },
            { "classId": "recon", "squadLimit": 1, "allowedEntries": {} }
          ],
          "squads": [
            { "callsign": "alpha", "displayName": "Alpha", "capacity": 8, "classLimits": { "assault": 8, "support": 2, "engineer": 2, "recon": 1 } },
            { "callsign": "bravo", "displayName": "Bravo", "capacity": 8, "classLimits": { "assault": 8, "support": 2, "engineer": 2, "recon": 1 } },
            { "callsign": "charlie", "displayName": "Charlie", "capacity": 8, "classLimits": { "assault": 8, "support": 2, "engineer": 2, "recon": 1 } },
            { "callsign": "delta", "displayName": "Delta", "capacity": 8, "classLimits": { "assault": 8, "support": 2, "engineer": 2, "recon": 1 } },
            { "callsign": "echo", "displayName": "Echo", "capacity": 8, "classLimits": { "assault": 8, "support": 2, "engineer": 2, "recon": 1 } }
          ],
          "vehicles": []
        }
      ]
    },
    {
      "id": "caesar",
      "displayName": "凯撒",
      "description": "凯撒战场阵营",
      "battleSide": "red",
      "enabled": true,
      "maxPlayers": 40,
      "formations": [
        {
          "id": "default",
          "displayName": "常规编制",
          "description": "默认步兵常规编制",
          "category": "infantry",
          "enabled": true,
          "capacity": 40,
          "capabilities": {
            "outpost": { "enabled": false, "maxActive": 0, "squadLeaderCanPlace": false, "commanderCanPlace": false, "maxHealth": 0, "placementCooldownSeconds": 0, "replacementCooldownSeconds": 0, "destructionCooldownSeconds": 0 },
            "rally": { "enabled": false, "maxActive": 0, "squadLeaderCanPlace": false, "commanderCanPlace": false, "maxHealth": 0, "placementCooldownSeconds": 0, "replacementCooldownSeconds": 0, "destructionCooldownSeconds": 0 },
            "respawn": { "delaySeconds": -1, "mobileSpawnVehicleIds": [] },
            "support": { "mode": "none", "allowList": [] }
          },
          "classes": [
            { "classId": "assault", "squadLimit": 8, "allowedEntries": {} },
            { "classId": "support", "squadLimit": 2, "allowedEntries": {} },
            { "classId": "engineer", "squadLimit": 2, "allowedEntries": {} },
            { "classId": "recon", "squadLimit": 1, "allowedEntries": {} }
          ],
          "squads": [
            { "callsign": "alpha", "displayName": "Alpha", "capacity": 8, "classLimits": { "assault": 8, "support": 2, "engineer": 2, "recon": 1 } },
            { "callsign": "bravo", "displayName": "Bravo", "capacity": 8, "classLimits": { "assault": 8, "support": 2, "engineer": 2, "recon": 1 } },
            { "callsign": "charlie", "displayName": "Charlie", "capacity": 8, "classLimits": { "assault": 8, "support": 2, "engineer": 2, "recon": 1 } },
            { "callsign": "delta", "displayName": "Delta", "capacity": 8, "classLimits": { "assault": 8, "support": 2, "engineer": 2, "recon": 1 } },
            { "callsign": "echo", "displayName": "Echo", "capacity": 8, "classLimits": { "assault": 8, "support": 2, "engineer": 2, "recon": 1 } }
          ],
          "vehicles": []
        }
      ]
    }
  ]
}
```

学院军的 `millennium_seminar_mobile` 显示为“千禧年研讨会机动部队”，类别为 `mechanized`，徽标为 `wok_infantry:textures/gui/formations/millennium_seminar_mobile.png`，并配置以下八个独立载具槽位：

| 显示名 | 实体资源 ID | 数量 | 损毁后补充 |
| --- | --- | ---: | --- |
| M1296 龙骑兵 | `fcp:stryker_dragoon` | 3 | 不可再生 |
| M1128 MGS | `fcp:stryker_mgs` | 2 | 不可再生 |
| 悍马 M2 | `fcp:hmmwv_armored_m2` | 2 | 300 秒 |
| 小鸟 机枪版 | `fcp:littlebird_armed` | 1 | 600 秒 |

## 5. 编制如何约束小队、兵种与装备

### 小队和兵种

编制是服务端规则源。创建/加入小队时，服务端先确认 callsign 在玩家当前编制中存在，再使用该编制配置的容量；选择兵种时，再按当前 `(战斗侧, 编制, 小队)` 原子预留配额。跨编制的队长操作、加入、踢人、转交、解散、快照和配额统计都会拒绝或隔离。

编制中的 `classId` 应与 `config/wok_infantry/loadouts.json` 中的兵种 ID 对齐。二者不一致时不会自动创建配装；玩家最终只能看到并保存“该编制存在、且配装目录也存在”的交集。

### TaCZ 枪械

`formations.json` 不直接保存枪械物品或 `GunId`。正确的数据链是：

```text
loadouts.json 的 LoadoutEntry.id
    -> formations.json 的 classes[].allowedEntries[slot]
        -> 玩家保存选择
            -> 部署/补给时服务端再次验证并发放
```

TaCZ 枪械条目本身放在 `loadouts.json`：

- `itemId` 必须精确为 `tacz:modern_kinetic_gun`。
- `snbt` 必须是合法复合标签并含有有效 `GunId` 资源位置。
- `GunId` 必须能由当前 TaCZ 注册表找到。
- `ammoReserveLimit` 为该单独枪械条目的携带/补给弹药上限，范围 1–4096 发，默认 180 发。它随兵种配装深复制，并在部署时由服务端写入实际枪械的 WOK步战发放标记；小型弹药箱和大型弹药站按玩家点选的那把枪读取，而不是按整个职业共用一个数值。
- TaCZ 未安装、枪械不存在、NBT 无效或反射 API 失败时，该枪械生成空物品并失败关闭，不会退回为其他枪。
- `wok_infantry` 对 TaCZ 使用可选依赖与反射边界；未配置 TaCZ 枪械时可独立安装。安全默认配装只是 `minecraft:air` 占位，不是正式武器表。
- 客户端候选卡和详情从配置 SNBT 重建枪械，再通过可选反射读取 TaCZ HUD 的原生 `39×13` 枪械剪影；普通物品和不可解析枪械退回物品图标。剪影只负责显示，服务端发装校验仍以上述 `GunId` 与完整 SNBT 为准。

### 卓越前线载具

卓越前线与 FCP 均为可选依赖。当前已经实现配置校验、管理员显式部署、整批规划与失败关闭、事务式生成、所有权标签、持久化分配台账、持久化损失/补充台账、延迟清理 tombstone、实体生命周期观察、会话轮换和受管载具乘坐授权。

当前行为边界如下：

- 玩家选择编制时只会校验并展示 `vehicles`，不会触发首次生成。首批载具必须由管理员执行载具部署命令；此后实体真正损毁才按该槽位配置自动补充。
- 管理员部署会把所选公开阵营映射到内部战斗侧，并以该侧唯一的实体载具部署箭头为批次原点；箭头指向决定整个批次的基础 yaw，配置中的相对偏移和单车 yaw 再据此旋转。方块不存在、未加载、阵营颜色或朝向与存档不一致、空间碰撞、液体、地面不稳或任何实体解析失败时，整批失败；玩家主基地不参与载具生成。
- 批次 allocation 由 `(sessionId, publicFactionId, formationId, vehicle.id)` 唯一标识。同一批次重复部署会幂等返回仍有效的现有实体；台账已占用但实体未加载时会拒绝补发，避免复制载具。
- 运行时台账同步到主世界 `SavedData`。载具实体同时携带 ownership 标签；台账损坏、重复 allocation、重复实体 UUID 或 ownership 不一致时均失败关闭，而不是把未知记录当作空位。
- `EntityJoinLevelEvent` 会恢复或核对加载实体；有待清理 tombstone 的旧载具会在重新加载前被丢弃。`EntityLeaveLevelEvent` 会在实体真正销毁时释放 allocation，普通区块卸载则保留台账。
- 真正损毁会写入 `wok_infantry_vehicle_replenishment` SavedData；管理重置、热重载退役、战局 session 轮换和延迟 tombstone 清理不会伪造战损或触发补充。
- `EntityMountEvent` 已接入服务端授权。WOK步战核心管理的载具只允许当前 session、同一公开阵营且同一编制的玩家乘坐；未选阵营/编制、其他编制、敌方阵营、旧 session、已从当前目录删除/改型的 allocation 或损坏 ownership 都会被拒绝。未受 WOK步战核心管理的载具不应用这套限制。
- 管理员载具 reset 会清理当前战局 session 的已加载载具；暂未加载的实体会留下持久化 tombstone，待其之后进入世界时完成清理。
- 全局战局重置会生成新的 session，并退役旧 session 的所有 allocation；服务器启动时也会从 SavedData 恢复台账并清理非当前 session 的载具记录。
- 编制目录热重载会调和当前 session 台账：已删除、改名或更换实体类型的 allocation 会清理已加载实体；未加载实体写入持久化 tombstone，且不会因同名配置再次出现而取消既定清理。
- session 轮换和热重载退役会先对整批 allocation 做零写入 ownership/UUID/类型/维度预检；任一可预测冲突都会整批中止。全部通过后才进入 Minecraft 实体 `discard` 的不可回滚提交阶段，运行时异常不会发布新的 active session，并会尽力持久化已完成的删除前缀。

生命周期接线已经落地，但真实卓越前线 + FCP 环境下的实体工厂、碰撞箱、乘坐事件兼容性、区块卸载/重载、计时补充和服务器重启恢复仍需运行时联调，当前不能将纯 Java 测试等同于真实 MOD 验收。

## 6. 命令边界

玩家命令：

- `/battle formation catalog`：显示当前 generation 和可用阵营/编制 ID；普通 catalog 不暴露服务端配置文件的绝对路径。
- `/battle formation join <publicFactionId>`：只选择学院军或凯撒军并占用阵营名额。
- `/battle formation vote <formationId>`：为当前阵营正在进行的一个具体编制投票。
- `/battle formation select <publicFactionId> <formationId>`：兼容入口；现在等价于先加入阵营、再提交投票，不再直接形成个人编制。
- `/battle status`：查看内部战斗侧、编制、小队与兵种状态。
- `/battle squad create <alpha|bravo|charlie|delta|echo>`
- `/battle squad join <alpha|bravo|charlie|delta|echo>`
- `/battle squad leave`
- `/battle squad disband`
- `/battle squad transfer <player>`
- `/battle squad kick <player>`
- `/loadout`：打开玩家配装界面；可见项仍由当前编制在服务端过滤。

管理员命令（权限等级 2）：

- `/battle admin formation reload`：重读并规范化 `formations.json`、提高 generation，调和失效/超容量选择、已删除或缩容小队、兵种删除/降额以及当前 session 的旧载具 allocation，并向在线玩家推送新目录；被清理选择的玩家回到三级选择/投票等待，其他 roster 变化会使存活玩家撤回旧配装并回到部署等待。解析失败时不提高 generation，也不改文件或生效状态。
- `/battle admin formation assign <player> <publicFactionId> <formationId>`：通过与普通选择相同的目录、启用状态和可选 MOD availability 校验恢复或强制改派指定玩家。
- `/battle admin formation vote open <publicFactionId> <allowVoteChange>`：使用当前全部有效具体编制开启或重开该阵营投票；布尔参数显式决定本轮能否改票，避免框架猜测。
- `/battle admin formation vote lock <publicFactionId> <formationId>`：显式锁定由比赛流程选出的结果，并原子地把同阵营全部玩家同步到该编制。当前不会自行猜测截止时机、最高票或同票处理。
- `/battle deployment vehicle place <blue|red> <dimension> <x> <y> <z> [yaw]`：原子放置/重定向并绑定该内部战斗侧唯一的载具部署箭头；省略 yaw 时采用命令源朝向。
- `/battle deployment vehicle remove <dimension> <x> <y> <z>`：移除实体方块及其绑定。游戏内也可用蓝/红染料绑定、空手查询、潜行空手顺时针旋转。
- `/battle admin formation vehicles deploy <publicFactionId> <formationId>`：在该阵营内部战斗侧的载具部署箭头处显式部署当前 session 的编制载具批次；重复执行遵守 allocation 幂等规则。
- `/battle admin formation vehicles reset`：清理当前 session 的全部编制载具；未加载载具写入延迟清理 tombstone。
- `/battle admin formation vehicles activate`：让载具 provider 重新加载台账并对齐当前部署 session，用于启动失败后的受控恢复；失败时仍保持原 authority，不发布分裂会话。
- `/battle admin remove <player>`：将玩家移出当前战局并清除其阵营/编制/小队状态。
- `/battle admin reset`：清空整个战局、小队、兵种占位、标记及相关部署/支援状态，同时轮换载具 session 并退役旧 session allocation；这是全局破坏性操作。
- `/loadoutadmin`：打开配装配置界面；顶部选择阵营与编制后，可管理该编制独有的职业、读取手持成品装备，并编辑职业各槽位的严格白名单。默认快捷键为 `U`，可在按键设置中重新绑定。

兼容命令 `/battle admin assign <player> <blue|red>` 会先把内部战斗侧解析回当前公开阵营，再选择该阵营下字面量为 `default` 的编制，最后统一调用 `FormationService.forceAssign`。因此它也执行与普通选择相同的 availability 校验，不再绕过目录或可选 MOD 检查。该命令仍依赖 `default` 这个兼容 ID；新配置和日常管理应优先使用 `admin formation assign`。

普通选择、管理员 `formation assign` 和旧 blue/red 兼容指派都会检查默认兵种不变量、卓越前线依赖与载具注册。热重载不会迁移或猜测已改名的 ID：缺失、停用、依赖失效以及超过新阵营/编制容量的选择会被服务端清除；仍有效编制中的小队和兵种则按队长、入队时间与 UUID 的稳定顺序调和。

## 7. 自动化测试与验收边界

当前自动化覆盖：

| 范围 | 测试 |
| --- | --- |
| 配置默认值、JSON 往返、规范化、去重、容量边界与失败关闭 | `FormationConfigDataTest` |
| 配置首次原子生成、畸形 JSON/空目录不覆盖原文件或当前目录 | `FormationRepositoryTest` |
| 选择网络编解码、畸形/超限数据拒绝、客户端视图不泄露内部战斗侧 | `FormationSelectionCodecTest` |
| 双方独立投票、改票策略、候选调和、畸形/超限投票存档拒绝 | `FormationVoteLedgerTest`、`FormationVoteSavedDataTest` |
| 同 callsign 跨编制存档隔离、旧 leader 修复、小队删除/缩容与兵种删除/降额的确定性调和 | `BattleSavedDataFormationTest`、`FormationServiceRosterReconciliationTest` |
| 创建/加入/配额/转交/踢出/冷却/快照/解散的跨编制隔离，以及共享结果的全阵营原子应用 | `BattleFormationGameTests` |
| 卓越前线批量规划与旋转、退役整批零写入预检、依赖/注册表失败关闭、当前 session allocation 调和、阵营+编制乘坐授权 | `VehicleBatchPlannerTest`、`RetirementBatchPlannerTest`、`SuperbWarfareVehicleGateTest`、`ManagedVehicleAccessPolicyTest` |
| 载具 SavedData 往返、tombstone 恢复、损坏/重复记录隔离与同步原子性 | `VehicleAllocationSavedDataTest` |

在 `wok_infantry` 模块内运行：

```powershell
.\gradlew.bat test --no-daemon --console=plain
.\gradlew.bat runGameTestServer --no-daemon --console=plain
```

发布前仍需在 WOK步战附属专用测试目录 `E:\MC针剂\versions\1.20.1-Forge_47.4.22\mods` 做真实多人验收，至少包括：

1. 两方独立开启投票，验证入阵营、改票开关、票数同步、显式锁定与全阵营共享结果；过期 generation 请求必须被拒绝。
2. 同阵营两个编制各自创建同名 `alpha`，成员、队长、踢出冷却、兵种配额和快照互不出现。
3. 重连、死亡、维度切换后保留选择；管理员移出后重新打开选择；降低容量、删除小队、降低兵种限额、停用/删除编制或令载具依赖失效后热重载，选择、roster 与旧装备必须按新目录调和；全局重置后所有在线玩家都回到未选择状态。
4. 使用实际 TaCZ 枪械注册 ID 验证允许/拒绝列表、部署与补给二次校验；移除 TaCZ 后相关枪械失败关闭但核心仍能启动。
5. 配置卓越前线实体后验证缺 MOD、错误 namespace、未注册实体都会阻止普通选择；空 `vehicles` 时核心可独立运行。
6. 在真实卓越前线环境设置双方载具部署箭头，验证四向批次朝向、管理员 deploy 的整批生成与幂等返回、方块缺失/错色/错向失败关闭、预检失败回滚、热重载删除/改名/改型 allocation、reset 对已加载/未加载载具的清理、重启后的 SavedData 恢复、实体 join/leave 台账同步、本编制乘坐、其他编制/敌方/旧 session 拒绝及全局重置后的 session 轮换。接线已经存在，但在完成这组运行时联调前不能宣称真实卓越前线兼容通过。

任何正式编制表进入配置前，还应独立确认稳定 ID、人数总和、各小队兵种配额、TaCZ 条目 ID 与实际 `GunId`、卓越前线实体 ID，以及缺少可选 MOD 时期望采用“禁用该编制”还是提供无载具替代编制。
