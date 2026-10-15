# WOK步战附属-独立护甲

从 World of Kivotos 主项目独立出来的纯护甲 Forge MOD。

当前版本 `1.3.0-beta.1`。新增逐件配置文件 `wok-infantry-armor-items.toml`：54 件插板和 17 件头盔可以分别覆盖等级/类型/材质、防护数值（缓冲 R、抗穿 Q、防护率 G、抗压 T）、耐久上限、磨损倍率、移速和额外属性（增益减益），没有覆盖的项继续沿用主配置 `wok-infantry-armor.toml` 的整类默认值；全部取默认时游戏行为与 1.2.0-beta.2 相同。配套管理命令 `/wokarmor inspect`、`/wokarmor problems`。用法、公式和已知限制见 [CONFIG.md](CONFIG.md)。

17 件头部防具（`tools/helmet-models` 生成）和 54 件插板护甲（`tools/plate-armor` 生成，与 WOK-本体护甲的新模型一致）的穿戴模型按《逃离塔科夫》原物重做（1.2.0 系列），统一为折中 2× 精度，游戏内外观验收仍待完成。默认护甲移速仍为轻甲 0%、中甲 -5%、重甲 -12%（再减材料惩罚）。

## 内容

- 54 件六档插板护甲，保留独立模型、贴图、防弹/穿甲/物理防护、承压、机动与耐久逻辑。
- 17 件 II–VI 级头部防具，具备独立物品图标、可穿戴模型、耐久和头部命中防护。
- 18 件六档电浆护盾，保留能量、过热、散热、充能、HUD、受击反馈和音效。
- 3 个旧电浆护盾 ID 兼容物品，仅用于旧存档/命令兼容，不显示在创造栏。
- 可选 TaCZ 弹种识别与统一损甲兼容。
- 可选 `WOK步战附属-部位血量` 软联动：头盔仅保护头部，胸甲按各自覆盖区域保护，单次命中不会叠加两件防具。
- 头盔防护随剩余耐久连续衰减：高耐久保持接近标称防护，低耐久阶段加速下降，耗尽后头盔损坏并立即失去保护。
- 头盔结构耐久由子弹实际普通伤害段与穿甲伤害段共同消耗，穿甲段默认按 2 倍磨损计入；高威力、高穿甲弹会更快击毁头盔。
- TaCZ 穿甲比例会换算为 I–VI 级穿透等级；子弹穿透等级高于头盔等级时，其穿甲伤害段直接穿透，不再享受 Q 缓冲。
- 头盔固定分为轻型头盔、中型头盔和重型头盔三类，默认结构耐久分别为 60、80、180；整类数值与穿甲磨损倍率（默认 2 倍）可在主配置 `wok-infantry-armor.toml` 中调整，单件耐久可在逐件配置中覆盖。
- 逐件配置 `wok-infantry-armor-items.toml`（服务端配置，位于 `saves/<世界>/serverconfig/`）：每件护甲一节，节名是完整注册名（如 `[plates.plate_armor_slick]`、`[helmets.helmet_6b47]`），写 `"default"` 的项沿用主配置；数字、百分数可以写在引号里；非法值回退默认并在日志与 `/wokarmor problems` 中列出。详见 [CONFIG.md](CONFIG.md)。

不包含生产台、方块实体、校准 QTE、职业经验、纳米护甲板、配方生产链或主 MOD 的其他系统。

## 环境

- Minecraft 1.20.1
- Forge 47.3.0
- Java 17
- GeckoLib 4.8.4+（头部防具穿戴模型的必需运行依赖）
- TaCZ 1.1.8 hotfix（可选运行依赖；仅编译 API）

## 构建

```powershell
.\gradlew.bat build
```

开发编译需要 ForgeGradle 映射后的 TaCZ 1.1.8-hotfix JAR。默认从本机 ForgeGradle
缓存查找，也可通过 `-Ptacz_dev_jar_path=<path>` 指定；TaCZ 不会被打包进成品 JAR。

成品位于 `build/libs/wok_infantry_armor-1.20.1-1.3.0-beta.1.jar`。

## 接入别的项目

最简单的方式是把成品 JAR 放进目标整合包的 `mods`。若目标也是 ForgeGradle 工程，可把 JAR 放进目标工程的
`libs`，再按本地依赖方式加入；本 MOD 使用独立 `wok_infantry_armor` 命名空间，不要求加载原 `miningdim` MOD。
