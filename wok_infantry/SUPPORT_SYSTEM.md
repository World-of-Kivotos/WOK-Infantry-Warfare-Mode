# WOK步战指挥支援框架

本文件描述 `WOK步战核心`（modId `wok_infantry`）的第三个核心功能。它不是 `WOK` 本体功能，也不修改或回灌 `WOK-本体护甲`。

## 当前交付范围

核心本身只交付通用支援框架，不内置任何具体支援技能、执行 provider 或第三方 MOD 实体绑定。仅安装核心时，`SupportProviders.createDefault()` 返回冻结的空注册表，因此战术平板显示“支援框架已就绪 / 暂无已注册支援”是正常状态，而不是组件加载失败。安装独立的 `WOK步战附属-指挥官支援`（0.1.0-beta.3）后，扩展会注册 7 个技能：

| 技能 ID（命名空间 `wok_commander_support`） | 名称 | 地图表现 |
| --- | --- | --- |
| `recon_satellite` | 侦察卫星 | 情报 |
| `recon_drone` | 无人机侦察 | 情报 |
| `millennium_f15ex_jdam_1000lb` | 千禧年 F-15EX 杰达姆 1000磅空袭 | 攻击 |
| `f16c_gbu12_paveway_500lb` | F-16C GBU-12 宝石路 II 500磅精准空袭 | 攻击（带照射许可内圈） |
| `howitzer_3round_barrage` | 三连发榴弹炮击 | 攻击 |
| `howitzer_105mm_rapid_3round_barrage` | 快速三连发105毫米榴弹炮打击 | 攻击 |
| `howitzer_105mm_5round_barrage` | 五连发105毫米榴弹炮打击 | 攻击 |

空袭与炮击用到的 CBC、卓越前线依赖，无人机实体，以及全部执行逻辑都留在扩展模块内；各技能的数值、时序和失败处理见该附属的 README。

支援目录由服务端注册表动态生成。安装 `WOK步战核心` 本身不会自动获得炮击、空袭、扫射或其他伤害能力，也不会因发现任意第三方 MOD 而自动注册支援。

## 扩展注册 API

扩展应在 Forge common setup 完成、首个服务端启动之前，通过以下框架类型组装目录：

1. 创建一个经过校验的 `SupportDefinition`，包含稳定的命名空间 ID、翻译键与回退名称、短名称、`POINT` 或 `DIRECTIONAL` 目标模式、冷却、来袭时间、执行步数/间隔、作用半径，以及最后一个组件 `requiresLoadedFootprint`（见下文“覆盖范围与区块加载”）。原 10 参数构造器保留，等同于 `requiresLoadedFootprint=true`。
2. 实现 `SupportProvider`；其 `supportId()` 必须与定义 ID 一致，执行入口只接收核心提供的 `SupportSpawnContext`。
3. 调用 `SupportProviders.register(definition, provider)` 成对注册。若定义和 provider 由不同初始化步骤提供，也可分别调用 `registerDefinition` 与 `registerProvider`。
4. 首个 `SupportService` 启动时，`SupportProviders.createDefault()` 会自动校验、冻结并缓存整个进程的目录；此后迟到注册会立即失败，所有服务端实例读取同一份不可变快照。

`SupportRegistry.builder()` 仍可用于单元测试、独立目录校验和显式依赖注入，但生产扩展必须经 `SupportProviders` 引导入口注册，否则不会进入默认 `SupportService`。

重复 ID、定义/provider ID 不一致、游离 provider、越界参数和冻结后的再次写入都会失败关闭。命名空间 ID 最长 128 个字符，是网络包、冷却存档和运行中任务的持久身份；扩展不得依赖列表位置或客户端显示文本作为身份。

`SupportProvider` 的回调一览（除 `supportId`、`availability`、`executeStep` 外都有默认空实现）：

| 回调 | 何时调用 | 用途 |
| --- | --- | --- |
| `accepted(context)` | 受理成功后一次（核心 0.3.0-beta.6 起） | 只做受理时的声音和提示，见下文“受理回调” |
| `executeStep(context)` | 在途时间结束后，每步一次 | 执行任务；失败按下文“失败处理与冷却返还”分类 |
| `abandon(context)` | 第 0 步执行过之后核心自己取消任务时一次 | 撤回已经放进世界的东西 |

扩展 provider 负责自己的可选依赖、加载顺序、实体构造和实际效果。核心公开边界不得在签名中暴露可选 MOD 的类。所有注册项都会经过核心熔断包装（`GuardedSupportProvider`）；熔断只针对适配器本身损坏，规则见下文“失败处理与冷却返还”。熔断后只关闭对应注册项，并由 `GuardedSupportProvider` 对每个注册项记录一次带完整堆栈的错误（`AbstractSoftSupportProvider` 只记录熔断状态、不另打日志），不生成原版爆炸或其他替代伤害。

无伤害侦察类 provider 可通过 `SupportIntelPublisher` 发布最多 64 个短期接触点。接口会再次校验调用方、支援圆形 footprint、坐标、高度、TTL 和数量；接触点仅合并到调用方阵营快照，不占手工标记额度、不落盘，也不能在客户端手动选择或删除。批次本身不合法（数量、TTL、空目标、超出扫描区或高度）视为适配器缺陷，按不返还的 `providerBroken` 处理；发布者离线、战局服务不可用或发布被拒只结束本次任务。

临时情报属于阵营而不是指挥官本人：`BattleService.publishSupportIntel` 只要求受理阵营没变、目标维度存在且不是 WOK步战等待区或大厅，不要求指挥官当前所在维度等于目标维度。指挥官在侦察期间阵亡进入等待区或换维度，已受理的任务继续刷新目标；不检查发布者是否仍是指挥官，与调度器执行期间只校验阵营一致。`SupportIntelPublisher` 只要求发布者是呼叫者当前在线的会话（玩家列表里该 UUID 对应的那个实例）：停在死亡界面仍算在线（原版在死亡 1 秒后移除尸体实体，所以不能只看 `isRemoved`）；已下线，或调度器拿到的是重生前的旧实体，都按“只结束本次”处理。

## 受理回调 `accepted`

核心 0.3.0-beta.6 起，`SupportProvider` 新增默认空实现的 `accepted(SupportSpawnContext context)`。它让扩展在受理那一刻就给出反馈，而不必等到在途时间结束、第 0 步执行时。例如榴弹炮击在受理时就响第一波炮声，并在这里排好其余各波炮声。

- 调用时机：`SupportService.requestSupport` 在任务放进任务表、受理回执记下之后，返回受理结果之前，调用一次；此时仍持有 `SupportService` 的锁，与调度器调用 `executeStep` 时相同。同一请求 ID 重发（幂等重放）、校验失败、冷却中都不会调用。
- 上下文：`context.stepIndex()` 为 0；`owner` 是受理时的指挥官；`level` 是目标维度（受理校验已保证与指挥官所在维度相同）；`callId` 与之后 `executeStep` / `abandon` 收到的相同；`faction` 是受理阵营。
- 只做表现：只用于声音、提示这类受理时的反馈。不得放实体、改方块，也不得读未加载区块（读高度只能用 `getChunkNow` 拿到的已加载区块）；任务本身从 `executeStep` 的第 0 步开始。
- 异常吞掉：`GuardedSupportProvider.accepted` 在熔断前转发给扩展，`Exception`（含直接实现偷偷抛出的受检异常）和 `LinkageError` 只记 warn，不熔断、不外抛；`SupportService` 自己再兜一层，构造上下文失败同样只记日志。`VirtualMachineError` 照常抛出。
- 不影响冷却和任务：回调失败不会撤销受理、不返还冷却、不取消任务，也不会让技能停用。
- 已熔断的 provider 不会收到这个回调。
- 第 0 步之前核心取消任务（会返还冷却）时不调用 `abandon`，核心目前也没有按 callId 查询任务是否仍在执行的接口。扩展若在 `accepted` 里排了延迟的声音，要自己设过期时间。指挥官支援的炮击给这些炮声加了租约：过了预期的第 0 步时刻 20 tick 还没执行第 0 步，就丢弃剩余炮声；在途期内本该响的炮声仍会照常响。

旧扩展不覆写这个方法时行为不变。

## 失败处理与冷却返还

`SupportSpawnException` 是 provider 向核心报告失败的唯一受检通道。message 是玩家可见的中文短句，会出现在聊天提示里；cause 只写入服务端日志。三类工厂方法：

| 方法 | 本次任务 | 冷却 | 技能 |
| --- | --- | --- | --- |
| `endMission(message[, cause])` | 结束 | 不返还 | 照常可用 |
| `notDelivered(message[, cause])` | 结束 | 返还（第 0 步完成前） | 照常可用 |
| `providerBroken(message, cause, refundCooldown)` | 结束 | 由调用方声明（第 0 步完成前才生效） | 熔断到服务器重启 |

- 原有的 `new SupportSpawnException(message)` 与 `(message, cause)` 构造器保留，语义等同 `endMission`。
- provider 抛出的 `RuntimeException`、`LinkageError` 和反射异常（`ReflectiveOperationException`，包括直接实现偷偷抛出的受检异常）统一转换为不返还的 `providerBroken`。`ThreadDeath` 与 `VirtualMachineError` 照旧不吞。
- `AbstractSoftSupportProvider` 的抽象方法为 `doExecuteStep(SupportSpawnContext) throws ReflectiveOperationException, SupportSpawnException`，分类规则与 `GuardedSupportProvider` 相同。
- 已熔断的注册项再被调用时只抛 `endMission`；调度器每步执行前都会先检查可用性，因此还没执行第 0 步的任务按下面的“核心取消”返还。

返还条件：只有任务的第 0 步还没有成功完成时才返还。两种来源：

1. provider 在第 0 步抛出 `notDelivered`，或 `refundCooldown=true` 的 `providerBroken`。
2. 核心自己取消任务：呼叫者已不在受理阵营、provider 不可用或未注册、目标维度已不可用、覆盖范围超出世界坐标或世界边界、要求加载的 footprint 已卸载。

第 0 步执行过之后，无论哪种失败一律不返还。返还是比对写回：受理时记下写入存档的到期 tick，返还时只有存档里该（阵营, 支援 ID）的值仍等于它才清零；管理员中途用命令清除或重置过冷却，就保持管理员的结果。返还后可以立刻再次呼叫，核心不另加防刷限制。

第 0 步执行过之后核心自己取消任务（上面第 2 条列出的原因）时，世界里可能已经有这次任务放出的东西。核心在结束任务前调用一次 `SupportProvider.abandon(context)`（默认空实现），`context.stepIndex()` 是不再执行的那一步；适配器已熔断时照样调用，目标维度已不可用时不调用。provider 应只用已加载的状态找回并移除自己放出的实体（例如按 callId 查找弹体），不得加载区块；`GuardedSupportProvider` 透传该调用，清理抛出的运行时异常或链接错误只记警告日志。第 0 步之前取消的任务（会返还冷却）不调用 `abandon`。

任务没有完成就结束时，核心按呼叫者 UUID 重新查找在线玩家，发一条聊天提示（原因经截断处理），正常完成不发：

- 已返还：`[支援] {名称} 未能投送：{原因}，冷却已返还`
- 要求返还但冷却已被改过：`[支援] {名称} 未能投送：{原因}`
- 不返还：`[支援] {名称} 任务中止：{原因}`

调度器遍历任务表的快照，每个任务外再兜住调度器自身的 `RuntimeException` / `LinkageError`（记错误日志，不返还地结束该任务，异常不会抛进服务端 tick）；provider 回调里重入修改任务表（例如战局重置）不会打断整轮调度。每 tick 4 次的执行预算统计全部 provider 回调，失败的也算。

## 覆盖范围与区块加载

`requiresLoadedFootprint=true`（默认）时，受理和每一步执行前都要求覆盖范围内的全部区块已经加载，区块数不超过 1024；执行期间区块卸载会按“核心取消”结束任务。

`requiresLoadedFootprint=false` 供从不读取覆盖范围内方块或高度的 provider 使用，例如只扫描已加载实体的侦察。受理和执行期间只校验坐标范围、世界边界和目标维度存在，不检查区块数上限（半径已受定义上限约束，避免误拒大范围扫描）。受理时未加载的列不读高度图、地表 Y 取世界最低高度，核心不会因此同步加载或生成区块；这类 provider 自己也不得读取未加载区块。

## 玩家流程与空目录

1. 按 `M` 打开 WOK步战战术平板。
2. 宽屏布局在右侧目标卡下方显示支援模块；普通和紧凑布局通过“标记 / 支援”切换进入。
3. 空目录显示框架就绪提示，不绘制虚假的固定技能按钮。
4. 有扩展注册后，平板按服务端快照动态列出支援。点目标在地图上选择一个位置；方向目标依次选择起点和终点；右键取消当前选点。
5. 客户端只提交注册 ID、维度和 X/Z 目标意图；是否受理、按钮状态、阵营冷却和在途任务都以服务端权威快照为准。

普通队员可以查看锁定、扩展不可用、冷却和执行中状态，但客户端显示不构成权限依据。

客户端扩展可通过 `TacticalSupportMapPresentationRegistry` 按同一稳定支援 ID 注册 `UTILITY`、`INTELLIGENCE` 或 `OFFENSIVE` 地图表现。攻击性点目标使用服务端快照中的定义半径绘制完整危险区，方向型目标绘制完整扫掠走廊；该注册表只决定视觉语义，不能覆盖服务端范围、冷却、权限或执行逻辑。未注册的支援安全回退为 `UTILITY`。

需要友军照射的支援可再调用 `registerGuidanceRadius(supportId, radius)` 声明“照射许可”内圈，`guidanceRadius(supportId)` 返回 `OptionalDouble`，`unregisterGuidanceRadius(supportId)` 单独移除；`unregister(supportId)` 会同时清掉内圈。半径必须是有限正数，否则抛 `IllegalArgumentException`；注册表线程安全，只在物理客户端使用。地图在选点预览、在途和执行中三种状态下，于危险区内画一圈琥珀色虚线（圆心为目标起点，与服务端照射判定一致），像素半径按“已画出的外圈 × 内圈/外圈半径”换算，外圈被截断时仍保持比例；两圈下方标注“照射许可 R{n}m”（语言键 `screen.wok_infantry.map.support.guidance_radius`），标签整体放在外圈与内圈（含深色描边）中更靠外的一圈之下，留出间隙，不压住任一圈。内圈小到不超过准星臂长时不画；标签会压住准星时，文字并入原有的波及半径标签或任务卡片。内圈同样只是显示，照射判定半径由扩展在服务端自行执行。

## 服务端权威边界

- 请求者必须是当前服务器已分配玩家、已经进入作战阶段，并且是当前阵营指挥官。
- 只接受玩家当前战场维度；WOK步战等待维度和大厅维度始终拒绝。
- 服务端按注册定义重新校验 ID、目标模式、有限坐标、作用范围、世界坐标上限、世界边界和完整 footprint。
- 默认要求 footprint 涉及的区块已经加载；定义声明 `requiresLoadedFootprint=false` 时只校验坐标、世界边界和维度。两种情况下客户端请求都不得强制生成或长期占用任意远端区块。
- Y 坐标由服务端按地形计算，客户端不能指定执行高度。
- 每个阵营同一注册 ID 同时最多一个任务；全服队列、单任务步骤和每 tick 执行量都有硬上限。
- 每个确认请求携带 UUID 幂等键，网络重发不会重复受理；该键只用于回执幂等。任务 callId 由服务端 `UUID.randomUUID()` 生成，provider 可以放心把它用作实体 UUID。
- 冷却按阵营和稳定注册 ID 保存到 `<世界>/data/wok_infantry_support.dat`；指挥权移交或服务器重启不会刷新已有冷却。只有“失败处理与冷却返还”一节列出的情况会按比对写回返还。
- 权限等级 2 的管理员可执行 `/battle admin support cooldown clear <blue|red> <支援ID>` 清除一个阵营、一个支援 ID 的持久冷却；该操作不取消当前在途任务。冷却存的是到期 tick，扩展调短定义冷却后，升级前已开始的冷却仍按旧到期时间走完，需要时用这条命令清除。
- 执行期间传给 provider 的 `owner` 优先使用呼叫者当前在线的会话，离线时退回受理时的实体（可能是重生前的旧实例，只适合作归属；发布临时情报时另行校验）。
- 在途任务只保留在当前服务端运行期；崩溃或重启后不重放，已经提交的持久冷却仍然保留。

## 网络、存档与阵营隔离

- 战局网络协议为 `18`（`BattleNetwork.PROTOCOL_VERSION`），版本严格匹配；旧协议客户端不会错解支援定义、任务或临时侦察接触点。
- C2S 支援请求使用独立意图包，不复用战术标记包，并对长度、枚举、坐标和尾随数据执行有界解码。
- 卫星红点（`TacticalMarkerType.RECON_CONTACT`）只能由侦察支援生成：`BattleService.createMarker` 拒绝该类型，`CreateMarkerPacket` 构造与解码时同样拒绝；`wok_infantry_battle.dat` 读档时丢弃该类型的标记。
- `BattleSnapshot` 只向观察者发送己方可见的注册定义、己方冷却和己方在途任务，不向敌方泄漏目标或调用者身份。
- 快照里卫星红点与手工标记各保底 32 个显示名额（`BattleRules.RESERVED_SUPPORT_INTEL_SNAPSHOT_SLOTS`），一方用不完的让给另一方，合计不超过 64（`BattleRules.mergeSnapshotMarkers`）。红点按发布顺序排列（呼叫首次发布的先后、批次内顺序），名额不够时保留较早发布的；手工标记按创建时间排列，名额不够时保留最新的，服务端回复创建成功的标记一定出现在快照里，最早的先让位。
- 目录为空时快照携带空列表；默认安装不会写入任何具体技能冷却记录。
- 目录结构、provider 可用性、冷却或任务结构变化会触发 UI 更新；纯倒计时刷新不会持续重建控件。

## 兼容性

支援框架本身不声明 Create Big Cannons、Superb Warfare 或 TaCZ 依赖，也不包含它们的具体支援实现。TaCZ 与 Superb Warfare 在 `mods.toml` 中保留的可选依赖只服务于现有的配装和编制载具软联动，与默认空支援目录无关。未安装这些 MOD 时，WOK步战核心的支援框架仍可独立启动。

成品不内嵌或重打包任何关联 MOD 类。扩展若要使用第三方内容，必须自行声明兼容版本并完成组合测试。

WOK步战附属的指定游戏内测试目录是：

```text
D:\WOK步战测试\1.20.1-Forge_47.4.22\mods
```

不得把 `D:\WOK测试\...\mods` 中的 WOK 本体 JAR 复制到该目录充当步战附属。

## 验收建议

- 仅安装核心必需依赖时，确认服务端启动、空目录快照、平板空态和其他战局功能正常。
- 使用测试扩展注册点目标与方向目标，覆盖注册、排序、权限、目标校验、冷却、幂等、任务预算和存档。
- 覆盖重复 ID、畸形定义、游离 provider、provider 不可用和执行异常，确认均失败关闭且不产生替代伤害。
- 分别触发 `endMission`、`notDelivered` 与 `providerBroken`，确认只有后者熔断；第 0 步前返还冷却并收到聊天提示，第 0 步后不返还；管理员中途清除冷却后返还不覆盖管理员结果。
- 受理回调：受理后立刻有反馈（例如炮击的第一波炮声）；让 `accepted` 抛异常时只有 warn 日志，受理结果、冷却和任务都不受影响，技能也不停用。`SupportService` 中“入表 → 记回执 → `accepted` → 返回”的顺序没有端到端单元测试，需在游戏内确认。
- 侦察期间让指挥官阵亡进入等待区或换维度，确认红点继续刷新；同时检查红点与手工标记各自至少显示 32 个。
- 在 320×240 与 960×720 下检查照射许可内圈虚线和标签的清晰度、溢出与重叠。
- 在完整 MOD 生态中确认 TaCZ 配装和 Superb Warfare 编制载具仍正常，同时确认核心不会自动注册任何支援技能。
