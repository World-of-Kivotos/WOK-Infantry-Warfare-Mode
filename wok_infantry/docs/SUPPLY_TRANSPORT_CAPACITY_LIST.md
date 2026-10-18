# WOK步战车辆运输补给容量清单

本文件是 `WOK步战核心` 车辆运输补给规格与容量的权威清单。今后确认任何新车辆，或调整、停用已有车辆时，必须在这里追加或修改一行，并同步更新服务端默认配置、自动化测试和游戏内验收步骤。

## 口径

- “实体 ID”采用 Minecraft 实体注册 ID，格式为 `namespace:path`。
- “大型补给”对应龙之崛起原生部署器/实体 `dragonrise_reforge:ammo_supply_station`，单站 1500 点；模型、骨骼、动画与渲染完全由龙之崛起原实现负责，避免转换为普通方块后出现部件错位。未安装龙之崛起时核心仍可启动，但大型补给不可发放。
- “中型补给”对应 `wok_infantry:medium_ammo_supply_crate`，单箱 500 点。
- “容量”表示一辆新建立运输数据的满载车辆能够提供的指定规格补给数量；不同规格不得自动互换。
- 车辆当前剩余量持久化到实体 NBT；区块重载、服务端重启或提高配置容量不会自动补满旧车。
- 服务端可在世界配置 `serverconfig/wok_infantry-server.toml` 的 `supplyTransport.vehicles` 中用 `实体ID=large|medium=容量` 覆盖默认清单；旧式 `实体ID=容量` 仅作为大型补给兼容语法。

## 正式容量表

| 序号 | 车辆名称 | 实体 ID | 补给规格 | 容量 | 默认状态 | 备注 |
|---:|---|---|---|---:|---|---|
| 1 | FMTV | `superbwarfare:truck` | 大型 | 3 | 启用 | 2026-08-31 用户确认；作为首个默认运输车辆 |
| 2 | Ural | `fcp:ural` | 大型 | 3 | 启用 | 2026-08-31 用户确认；FCP 普通 Ural，不包含油罐、方舱或火箭炮变体 |
| 3 | HMMWV 无装甲无武装型 | `fcp:hmmwv_unarmored_unarmed` | 中型 | 2 | 启用 | 2026-08-31 用户确认 |
| 4 | HMMWV 装甲无武装型 | `fcp:hmmwv_armored_unarmed` | 中型 | 1 | 启用 | 2026-08-31 用户确认 |

## 变更记录

| 日期 | 变更 |
|---|---|
| 2026-08-31 | 建立容量清单；确认 FMTV（`superbwarfare:truck`）每辆满载 3 个大型补给。 |
| 2026-08-31 | 新增 Ural（`fcp:ural`），每辆满载 3 个大型补给。 |
| 2026-08-31 | 新增无装甲无武装 HMMWV（`fcp:hmmwv_unarmored_unarmed`），每辆满载 2 个中型补给。 |
| 2026-08-31 | 新增装甲无武装 HMMWV（`fcp:hmmwv_armored_unarmed`），每辆满载 1 个中型补给。 |
| 2026-08-31 | 大型补给改用核心自有 `wok_infantry:large_ammo_supply_station`，移除车辆取箱链对龙之崛起物品的依赖。 |
| 2026-08-31 | 按合作使用方案恢复龙之崛起原生 `dragonrise_reforge:ammo_supply_station` 为正式大型补给；核心自有转换方块仅保留旧存档兼容，不再由创造栏或车辆发放。 |
