# Wok步战核心

Minecraft 1.20.1 / Forge 47.4.22 的独立步战小游戏核心。当前阶段实现服务端权威的配装系统。

## 已实现

- 四个基础兵种：突击兵、支援兵、工程兵、侦察兵
- 六个配装槽位：主武器、副武器、近战、战术道具一、战术道具二、投掷物
- 玩家配装 UI：按 `L` 或执行 `/loadout`
- 管理员编辑 UI：执行 `/loadoutadmin`，需要权限等级 2
- 管理员可修改兵种显示名称和启用状态
- 管理员可在每个兵种/槽位下新增、修改、删除候选装备
- 配置物品注册名、数量和可选 SNBT
- 玩家选择按世界与 UUID 持久化
- 配置原子写入，所有管理员操作均由服务端重新校验权限和数据
- `/loadout apply` 可重新发放玩家已保存的配装

## 数据文件

- 管理员配置：`config/wok_infantry/loadouts.json`
- 玩家选择：`<世界>/data/wok_infantry/player_loadouts.json`

第一次启动服务器时会自动生成默认配置。

## TaCZ

本 MOD 不强制依赖 TaCZ。管理员可以把 TaCZ 物品注册名和枪械 SNBT 填入编辑界面，
服务端会在实际发放前解析并校验数据。这样配装系统也可以兼容其他武器 MOD。

## 构建

需要 JDK 17：

```powershell
.\gradlew.bat build
```

产物位于 `build/libs/wok_infantry-0.1.0.jar`。
