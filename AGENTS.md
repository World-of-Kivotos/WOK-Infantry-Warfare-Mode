# WOK 产品线永久命名规则

以下规则适用于本项目及其所有后续开发、文档、构建产物和讨论：

- `WOK` 与 `WOK步战` 是两个不同概念，不得混称。
- `WOK` 指主项目/本体产品线。
- `WOK步战` 指依附于相关生态的附加子模式及其可拆分附属 MOD，不等同于 WOK 本体。
- 已上传 Git、服务于 WOK 本体的护甲实现，正式称为 `WOK-本体护甲`。保留其现有仓库身份与 modId，避免破坏兼容性。
- 从本体拆分、能够单独安装的纯护甲 MOD，正式称为 `WOK步战附属-独立护甲`，建议 modId 为 `wok_infantry_armor`，JAR 前缀为 `wok_infantry_armor-`。
- `WOK步战附属-部位血量` 的 modId 为 `wok_body_health`。
- `WOK步战附属-创伤治疗` 的 modId 为 `wok_trauma`。
- `WOK步战核心` 的 modId 为 `wok_infantry`。
- 未特别说明时，部位防护、部位血量和创伤治疗等新联动只加入 `WOK步战附属-独立护甲`，不得误改或回灌到 `WOK-本体护甲`。
- 所有拆分 MOD 必须保持可独立安装；跨 MOD 功能通过可选依赖或软联动实现。
- `E:\MC针剂\versions\1.20.1-Forge_47.4.22\mods` 是 WOK步战附属专用测试目录；步战核心、创伤、部位血量和独立护甲只在这里测试。
- `D:\WOK测试\versions\1.20.1-Forge_47.4.20\mods` 是 WOK 本体专用测试目录；不得把这里的本体 JAR 当成步战附属或独立护甲。
- `WOK步战附属-独立护甲` 的源码目录是 `D:\Wok\standalone\wok-infantry-armor`；正式 modId 为 `wok_infantry_armor`，Java 包根为 `com.wok.infantryarmor`。
- `WOK步战附属-独立护甲` 的源码目录是 `D:\Wok\standalone\wok-infantry-armor`；正式 modId 为 `wok_infantry_armor`，Java 包根为 `com.wok.infantryarmor`。
- `E:\MC针剂\versions\1.20.1-Forge_47.4.22\mods` 是 WOK步战附属专用测试目录；步战核心、创伤、部位血量和独立护甲只在这里测试。
- `D:\WOK测试\versions\1.20.1-Forge_47.4.20\mods` 是 WOK 本体专用测试目录；不得把这里的本体 JAR 当成步战附属或独立护甲。
