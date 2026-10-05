# WOK步战核心 UI 自动验收（0.4.0-beta.1 档 1，0.4.0-beta.2 补测试开局，0.4.0-beta.3 地图标点）

`runUiTestClient` 启动一个开发用真实客户端，进入隔离存档，先走一遍实时流程（部署页、终端键、地图键、队长/指挥官、部署、标记、JourneyMap 地形），截下旧截图 01–09；再由用例运行器 `UiCaseRunner` 先跑 `legacy` 组补齐旧截图 10–14（编制 2 张、管理员 3 张，保留原名和顺序），然后把 `src/uiTest/.../cases/` 里登记的其余用例逐个档位截图、检查，最后写结果并自动退出。验收代码全部在 `src/uiTest`，不会进入生产 JAR。

本文件说明怎么跑、跑哪些档位和用例、结果放在哪，以及哪些东西只能真人看。

## 1. 准备

| 需要 | 说明 |
|---|---|
| JourneyMap 6.0.2 | 必需。默认读 `run/compat-cache/journeymap-forge-1.20.1-6.0.2.jar`，可从步战测试端 `mods` 只读复制一份，或用 `-PuiJourneyMapJar=<路径>` 指定。 |
| 种子存档 | `run/world/level.dat`：先跑一次 `runGameTestServer`，或从其他工作区的 `run/world` 复制（不带 `session.lock`）。 |
| TaCZ | 不需要。 |

每次运行都会用种子重建 `run/ui-test/saves/wok_ui_test`，按 `-PuiLang` 改写 `run/ui-test/options.txt` 的语言，并删掉其中核心的按键行 `key_key.wok_infantry.*`（按键回到 MOD 默认值）。

## 2. 怎么跑

| 目的 | 命令（在 `wok_infantry` 目录） |
|---|---|
| 正式一轮（zh_cn，必过） | `.\gradlew.bat runUiTestClient --no-daemon --console=plain` |
| 英文报告轮（只出报告） | `.\gradlew.bat runUiTestClient -PuiLang=en_us -PuiLayoutStrict=false --no-daemon --console=plain` |
| 归档刚跑完的一轮 | `.\gradlew.bat archiveUiAcceptance --no-daemon --console=plain` |
| 只跑某组 / 某界面 / 某用例 | `-PuiCases=formation`、`-PuiCases=hud+kit`、`-PuiCases=formation.full`（不含 `legacy` 时跳过实时流程和 14 张旧截图，约 1–2 分钟） |
| 只跑某些档位 | `-PuiTiers=320x240+640x336`（14 张旧截图不受影响） |
| 已迁移界面也只出报告 | `-PuiLayoutStrict=false` |
| 预览图目录 | `-PuiPreviewShots=<目录>`，默认向上查找相邻的 `ui-preview/shots`（index.html 并排对照用） |

列表参数可用 `,`、`;` 或 `+` 分隔。整轮（实时流程 + 48 个用例，229 张截图；加体力条 8 个状态之前为 40 个用例、189 张，0.4.0-beta.1 时为 38 个用例、179 张）在本机约 5 分钟。

**判定**：结果文件首行 `status=PASS` 才算通过，Gradle 据此决定任务成败；通过时还检查 `wok_ui_manifest.json` 存在且列出的截图都在。

**归档**：`archiveUiAcceptance` 只复制最近一轮 manifest 列出的截图和结果文件，目标是

```
D:\WOK步战测试\1.20.1-Forge_47.4.22\ui-acceptance\<yyyyMMdd>-<版本>\<标签>\
    screenshots\        本轮截图
    ui-test-results\    wok_ui_acceptance.txt、wok_ui_layout.json、wok_ui_manifest.json、index.html、wok_ui_progress.txt
```

标签默认取这一轮实际用的语言（结果文件里的 `languageCode`，即 `zh_cn` / `en_us`），所以同一天的中英两轮并排放；`-PuiArchiveLabel=<名字>`、`-PuiArchiveDate=yyyyMMdd` 可改。任务只写 `ui-acceptance` 下的新目录，目录已存在就拒绝，不会覆盖，也不碰测试端的 config、mods、saves。每跑完一轮就归档一次（下一轮会清掉 `run/ui-test` 里的结果）。

## 3. 档位

| 档位 | 自动化窗口 | 布局尺寸 | 判定 |
|---|---|---|---|
| 320×240 | 960×720，GUI 3 | 320×240 | 必过 |
| 960×720 | 960×720，GUI 1 | 已迁移界面和 HUD 按最低 2× 排成 480×360 | 必过；运行器另外断言 480×360 |
| 640×336 | 1280×672，GUI 2 | 640×336 | 必过（你实际用的 1920×1008 GUI 3，逻辑尺寸相同，物理字号是 2 而不是 3） |
| 480×270 | 960×540，GUI 2 | 480×270 | 只出报告 |
| 427×240 | 854×480（游戏默认窗口），GUI 1 | 按最低 2× 排成 427×240：编制页走窄屏布局，HUD 的 Boss 条要右移 | 只出报告 |

语言：zh_cn 必过；en_us 只出报告（`-PuiLayoutStrict=false` 跑一轮）。

**960×720 档的覆盖范围**：自动化的“960×720”档是 960×720 窗口、GUI 1。已迁移界面和 HUD 在客户端配置 `ui.minimumScale2x`（默认开）下按 2× 排成 480×360，所以这一档实际截图验收的是 480×360 排版；旧界面不受该配置影响，仍按 1× 的 960×720 排版截图（只出报告）。已迁移界面真正 1× 的 960×720 逻辑布局（例如 1920×1440 窗口 GUI 2，或关掉 `ui.minimumScale2x`）没有真实客户端截图，只由纯布局单测 `FormationScreenLayoutTest`（“960, 720, WIDE”）和 `WokHudLayoutTest`（960×720、绘制倍率 1）覆盖。AGENTS 要求的 960×720 档对已迁移界面目前就是这两部分合起来的结果，需要时可再加一个 1920×1440 GUI 2 的报告档。

## 4. 检查规则

每张截图对应的那一帧都由主源码的布局探针 `client/ui/probe` 记录框、文字、控件、裁剪和地图标点（生产环境没有接收端，不做任何事），再按 `UiLayoutReport` 检查：

- 文字出屏、出了所在框；框出父框；同级实体框重叠；begin/end 不配对；
- 文字被缩小（相对布局 < 1×）；含中文的文字物理字号 < 2（GUI 1 下 1× 中文看不清）；
- 文字或按键被省略号截断却没有完整内容提示；禁用控件没有写原因；控件出屏、按键文字出键；
- 中文界面出现 READY / LOCKED / INBOUND / COOLDOWN / OFFLINE / [SL] / [CO] 之类英文占位。

**已迁移界面**（陈列页、地图标点陈列、编制页、HUD）在必过档上有任何违规即失败；**未迁移的旧界面**（小队、地图、配装、管理员、补给）只出报告，等各自的批次迁移后再转为严格。每个用例还有自己的语义检查（下表），语义检查在必过档上失败同样判整轮失败。

## 5. 用例清单

截图名 `wok_ui_<界面>_<状态>_<档位>.png`；14 张旧截图保留原名和顺序。

| 组 / 界面 | 状态 | 档位 | 迁移 | 主要语义检查 |
|---|---|---|---|---|
| 实时流程（旧图 01–09） | deployment、squads、classes、map ×2、squads 960、commander、loadout ×2 | 各自原档位 | 否 | 终端键/地图键走真实按键与网络；兵种页真实鼠标点击；JM 小地图隐藏、全屏重定向、4/4 与 16/16 地形块；支援按钮与 R80/R64；两张地图截图记录 `mapIcons[...]`（按种类计数、按方案 B 尺寸），一个新标点都没画出来即失败（0.4.0-beta.3） |
| formation.legacy（旧图 10、11） | 320 待开启、960 投票中 | 320、960 | 否 | 管理员“开启编制投票 / 锁定投票结果”按 uiId `formation.admin.open` / `formation.admin.lock` 找到 |
| admin.legacy_*（旧图 12–14） | 列表 320、职业管理 320、列表 960 | 320、960 | 否 | “+槽位”“设置”“职业管理”存在 |
| kit（组件陈列页） | default、confirm、inputs、cards、hud、icons | 全部 5 档 | 是 | 按钮七态 + 焦点；超长键省略号带完整提示；危险确认默认焦点在“取消”、Enter 不确认、Esc 取消；48 个界面图标（0.5.0-beta.1 追加体力条的手掌、靴子、跳跃箭头）；10 种标点物理尺寸 |
| mapicons（地图标点陈列） | dark（深色地形）、paper（浅色纸图） | 全部 5 档 | 是 | 10 种标点 + 名称；普通/悬停/选中/即将过期；宽屏另有 0.75×/1.25×/1.75× 旋钮；每个标点物理尺寸 = 15×15（定位针 15×18）美术像素 × 旋钮对应的整物理像素（0.4.0-beta.3 起与地图同用 `TacticalMapIcons.mapArtPx`：GUI 1–3 不变，GUI 4 放大 4/3 后取整），与战术地图上一致 |
| formation（编制页，预览 45-formation） | join、confirm、facfull、vote、detail、locked、latejoin、lateconfirm、waiting、waitover（目录请求 3 秒没有回应）、longcaps（玩家视角）；pending、full、admintie、admin，以及 0.4.0-beta.2 的 testmode（未加入阵营时的“测试开局”键，页面状态为 join）、testconfirm（测试开局确认层）（管理员视角） | 全部 5 档 | 是 | 页面自报预览状态；管理员在未加入阵营时看得到可用的“测试开局”键且它单独在列表面板的紧凑管理员区，pending 状态下它与“开启编制投票”同一行、不重叠，玩家视角看不到它；测试开局为普通确认、正文写出所加入的阵营、Esc 取消后留在本页；waitover 写出“编制目录没有送达”并保留重试键；夹具未被服务端目录替换；详情区没有内部 ID（player-09）；裁剪区内没有半行；浏览中的阵营是描边而非实心蓝；未加入时 Esc 能关页；加入为普通确认、锁定为危险确认且默认“取消”；阵营满员/容量不足时按键禁用并写原因；锁定后加入仍可加入；5 个白名单支援全部按名称显示；已加入阵营时逐档记录页头身份是否显示（`formationHeaderIdentity[...]`），中文 427×240 档必须显示（页面在不足 440 宽时用短标题“编制投票”；427 档只出报告，不显示记为 REPORT） |
| admin.noclass（admin-01 回归） | 编制没有任何职业规则 | 320、960、640 | 否 | “职业管理”禁用并写原因；真实点击和强行 onPress 都不崩溃、不离开列表页 |
| ammo.small320（player-01 回归） | 小型弹药箱 | 320（480 报告） | 否 | 剩余点数和点数条不在分区标题下面，位于面板顶和分区标题之间 |
| hud（战斗 HUD，预览 10-hud） | battle、chat、downed、roster8、votewait、vote、voted、locked、boss（8 人名单 + 客户端放入的原版 Boss 条） | 全部 5 档 | 是 | 核心部件不碰快捷栏和状态行；体力条凹槽正好在原版经验条那一行 `[h−30, h−23)`、与 `StaminaBarLayout` 算出的位置一致，各部件不碰快捷栏、选中框、副手格、状态行、TaCZ 读数禁区和聊天（2× 耳朵在聊天最后一行之上时必须在聊天背景右边），布局宽 ≥ 640 时两只耳朵各有一个百分比、否则没有，原版经验条和跳跃条的覆盖层没有运行；boss 状态下移、右移后的 Boss 条区域不碰名单、不出屏；320 档名单底边 ≤ y95；窄屏开聊天时名单收成一行；投票阶段投票条占战况条槽位、没有名单、写出终端键；锁定通知 3 秒内截到；倒地时本人行写“倒地”，替身附属面板拿到 `center_low` 槽位 |
| stamina-a4（体力条，预览 16-stamina 的 A4；组 `hud`） | full、sprint、aim、legsout、recover、unlock、vehicle、horse（与预览状态同名，结果页并排显示预览图） | 全部 5 档 | 是 | 战斗 HUD 上把体力条固定为预览的演示数据（满体力；疾跑腿 64% 残影 69%；开镜手 42% 橙；腿耗尽锁定；双池恢复中；腿 16% 刚解锁；乘坐载具腿灰；骑马蓄力 55%），上一行 hud 的全部体力条检查之外，再核对体力条写给探针的状态说明：剪影/锁/勾/箭头、贴边颜色、填充色、残影、恢复中、锁定空槽暗红、坐骑状态。验收客户端里玩家并没有真的骑乘，所以没有预览里的坐骑血量行 |

用例数据对齐预览 `data/mock.js`：编制目录由核心默认配置 `FormationConfigData.defaultConfig()` 生成，详情、候选可用性及原因、旧版摘要行都调用服务端 `FormationService` 自己的方法（反射），夹具不重写任何服务端措辞；验收客户端没有卓越前线载具 MOD，可用性按“装了载具 MOD 的服务端”判定。HUD 状态只改客户端缓存（战局快照、兵力、体力、编制目录），每 tick 和每帧 HUD 前重装，服务端心跳不会混进截图；用例结束后恢复。管理员视角的用例临时授予 OP，玩家视角的用例收回，结束时由验收器统一撤销。

## 6. 结果在哪

| 文件 | 内容 |
|---|---|
| `run/ui-test/ui-test-results/wok_ui_acceptance.txt` | 首行 `status=PASS/FAIL`；失败时 `failure=`；随后是实时流程观测项和每个用例每个档位一行 `case[...]=PASS/FAIL/REPORT` |
| `run/ui-test/ui-test-results/wok_ui_layout.json` | 每张截图的档位、布局尺寸、违规（规则、文字/框/uiId、矩形）、控件和框 |
| `run/ui-test/ui-test-results/wok_ui_manifest.json` | 截图清单与状态 |
| `run/ui-test/ui-test-results/index.html` | Java 截图与预览图并排，下面列违规 |
| `run/ui-test/ui-test-results/wok_ui_progress.txt` | 进度；客户端没写结果就退出时，Gradle 报出最后到达的阶段 |
| `run/ui-test/screenshots/` | 截图 |

### 0.4.0-beta.3 最终验收（2026-10-05，final3：审查修正后）

本版的自动验收以本节的 `zh_cn-final3` 归档为准；下一节首轮 `final`（`1814e74`）与第一次审查修正的 `zh_cn-final2`（`bec2438`，JUnit 920 项，SHA-256 `4DFFBABF…5C3B12`）的产物已作废，归档留作对照，三轮的 180 行用例结果逐行相同。

- 代码：分支 `claude/地图标点` 的 `9db2698`（本版最后一个源码提交）。两处审查修正：悬停按光标的亚像素位置判定、光标下的图标优先于只压到进攻线身的命中（`bec2438`）；卫星/无人机红点在所属支援任务还会再扫描时不变淡，任务扫描完后最后一批照旧在租期最后 25% 变淡（`9db2698`，以前卫星红点每次扫描前会淡下去约 1 秒）。核心用限流脚本 `clean build`（连同六个附加测试源集的编译）：JUnit 922 项，0 失败、0 跳过；`runGameTestServer`：22 项必需 GameTest 全部通过。产物 `wok_infantry-0.4.0-beta.3.jar`，2,076,666 字节，SHA-256 `9B7C2890170CB7414DDFCF11DDFDFBC6E37877A017D0DE4D6EB91960B609360D`（跑完后重新核对，不变）。
- zh_cn 严格轮（`runUiTestClient`）：`status=PASS`，4338 tick，40 个用例、189 张截图，`strictLayoutViolations=0`，`layoutViolations=169`；180 行用例结果与首轮 `zh_cn-final` 逐行相同。两张地图截图的 `mapIcons[...]` 与首轮相同（8 种标点各 1 个，牌 30 物理像素；960 档另有 6 个工具键图标）。归档 `D:\WOK步战测试\1.20.1-Forge_47.4.22\ui-acceptance\20261005-0.4.0-beta.3\zh_cn-final3\`（`zh_cn-final2` 已被第一次审查修正那一轮占用，归档任务不覆盖已有目录）。
- 本版没跑 en_us 报告轮（文案只改了一条滑杆提示）。

### 0.4.0-beta.3 首轮验收（2026-10-05，final，产物已作废）

- 代码：分支 `claude/地图标点` 的 `1814e74`（本版最后一个源码提交）。本版把战术地图的标点换成 Squad 式剪影图标（`TacticalMapIcons` + `TacticalMapPinPlanner`），地图布局与侧栏不变。核心先用限流脚本 `clean build`（连同六个附加测试源集的编译）：JUnit 919 项，0 失败、0 跳过；`runGameTestServer`：22 项必需 GameTest 全部通过。产物 `wok_infantry-0.4.0-beta.3.jar`，2,075,575 字节，SHA-256 `EDBC3FC186DDE7C8BD09533093F63F7305F93EF299EC654949B1F9390D0C4A70`（跑完后重新核对，不变）。
- zh_cn 严格轮（`runUiTestClient`）：`status=PASS`，4336 tick，40 个用例、189 张截图，`strictLayoutViolations=0`，`layoutViolations=169`；180 行用例结果与 0.4.0-beta.2 的 `zh_cn-final2` 逐行相同。`mapIcons[wok_ui_04_map_320x240.png]`、`mapIcons[wok_ui_06_map_960x720.png]` 都画出 8 种标点（敌方步兵、坦克、步战车、卫星侦察目标、进攻方向、防守、集结点、主基地定位针），牌 30 物理像素；960 档另有 6 个侧栏工具键图标（1 物理像素一个美术像素）。归档 `D:\WOK步战测试\1.20.1-Forge_47.4.22\ui-acceptance\20261005-0.4.0-beta.3\zh_cn-final\`。
- 新旧对比：旧图看 `20261005-0.4.0-beta.2\zh_cn-final2\screenshots\wok_ui_04_map_320x240.png`、`wok_ui_06_map_960x720.png`（11×11 白线小图、坦克/步战车俯视贴图、橙色 B 方块），新图看本节归档的同名文件。
- 本版没跑 en_us 报告轮（文案只改了一条滑杆提示）。

### 0.4.0-beta.2 最终验收（2026-10-05，final2：审查修正后）

本版的自动验收以本节的 `zh_cn-final2`、`en_us-final2` 两份归档为准；下一节首轮 `final`（`c40e4d9`，产物已作废）保留为对照，两轮的 180 行用例结果在中英两种语言下都逐行相同。

- 代码：分支 `claude/新版界面` 的 `19d2114`（本版最后一个源码提交）。它在 `c40e4d9` 之上做了审查修正：测试开局在任何改动之前先核对阵营、编制和阵营空位；锁定或开票之后某一步失败也照常推送；发出过测试开局的编制页在部署成功的战局快照到达时关闭（不论它是怎么打开的）；补主基地跳过地面是树叶的落脚处，红方退到出生点 -64 X 时朝东。编制页的布局、按键和文字都没有变。核心先用限流脚本 `clean build`（连同六个附加测试源集的编译）：JUnit 907 项，0 失败、0 跳过；`runGameTestServer`：22 项必需 GameTest 全部通过（含本版新增 3 项）。产物 `wok_infantry-0.4.0-beta.2.jar`，2,066,819 字节，SHA-256 `60829BC47C86F4AF894CA32C1A210B5CA5E2D824B52A495892003BC6BAF50582`（两轮跑完后重新核对，不变）。
- zh_cn 严格轮（`runUiTestClient`）：`status=PASS`，4358 tick，40 个用例、189 张截图，`strictLayoutViolations=0`，`layoutViolations=169`；用例结果与首轮 `zh_cn-final` 逐行相同，“测试开局”键尺寸与 `finalActiveMarkers=1` 也相同。归档 `D:\WOK步战测试\1.20.1-Forge_47.4.22\ui-acceptance\20261005-0.4.0-beta.2\zh_cn-final2\`。
- en_us 报告轮（`-PuiLang=en_us -PuiLayoutStrict=false`）：`status=PASS`，4335 tick，189 张截图，`layoutViolations=172`、`strictLayoutViolations=0`；用例结果与首轮 `en_us-final` 逐行相同（比 0.4.0-beta.1 多的 4 条见下一节）。归档 `D:\WOK步战测试\1.20.1-Forge_47.4.22\ui-acceptance\20261005-0.4.0-beta.2\en_us-final2\`。
- 仍未做：真实客户端（测试端 PCL）里点“测试开局”的游戏内验收（含部署成功后编制页按服务端推出、战斗终端键、终端“编制”页签三种打开方式都自动关闭），以及局域网双人用 `/battle admin test start <阵营> <编制> <玩家>` 送第二个号。

### 0.4.0-beta.2 首轮验收（2026-10-05，final，已被 final2 取代）

- 代码：分支 `claude/新版界面` 的 `c40e4d9`（首轮最后一个源码提交，产物已作废）。核心先用限流脚本 `clean build`（连同六个附加测试源集的编译）：JUnit 905 项，0 失败、0 跳过；`runGameTestServer`：21 项必需 GameTest 全部通过（含本版新增 2 项）。产物 `wok_infantry-0.4.0-beta.2.jar`，2,062,775 字节，SHA-256 `1230DE869343EED83C3D83920662639A78C5F78FBA21468396890153A53AD324`（两轮跑完后重新核对，不变）。
- zh_cn 严格轮（`runUiTestClient`）：`status=PASS`，4336 tick，40 个用例、189 张截图，`strictLayoutViolations=0`。新增的 `formation.testmode`、`formation.testconfirm` 在 5 个档位（含 427×240）都是 0 违规；`layoutViolations=169` 与 0.4.0-beta.1 的 `zh_cn-final2` 逐条相同（都是旧界面），原有 179 张截图各档结果不变。“测试开局”键尺寸（屏幕像素）：320×240 为 303×14，427×240 为 820×28（2× 排版），480×270 为 170×14，640×336 为 200×18，960×720 为 332×36（2× 排版）。Boss 条位移与页头身份记录与 `zh_cn-final2` 相同。实时流程末尾的 `finalActiveMarkers` 由 3 变为 1：标记按存活时间过期，本轮多了 10 张截图、整轮长约 180 tick，与测试模式无关。归档 `D:\WOK步战测试\1.20.1-Forge_47.4.22\ui-acceptance\20261005-0.4.0-beta.2\zh_cn-final\`。
- en_us 报告轮（`-PuiLang=en_us -PuiLayoutStrict=false`）：`status=PASS`，4339 tick，189 张截图，`layoutViolations=172`、`strictLayoutViolations=0`。比 `en_us-final2`（168）多 4 条，都在新增截图里：testmode、testconfirm 两个状态的 960、480 档左侧阵营概况里的“3 (the faction picks one)”被省略号截断——与原有 join、confirm 状态同一行文字、同一位置的已知英文截断（留给 i18n 收尾），不是新按键造成的；其余 168 条与 `en_us-final2` 逐条相同。英文“Test start”键在 320 档与“Open formation vote”同一行完整显示。归档 `D:\WOK步战测试\1.20.1-Forge_47.4.22\ui-acceptance\20261005-0.4.0-beta.2\en_us-final\`。
- 仍未做：真实客户端（测试端 PCL）里点“测试开局”的游戏内验收，以及局域网双人用 `/battle admin test start <阵营> <编制> <玩家>` 送第二个号。

### 0.4.0-beta.1 最终验收（2026-10-05，final2）

0.4.0-beta.1 的自动验收以本节的 `zh_cn-final2`、`en_us-final2` 两份归档为准；下一节的 `final` 一轮保留为历史。

- 代码：分支 `claude/新版界面` 的 `883fcd6`。它在 `final` 一轮的 `3a24aec` 之上改了两件事：素材声明改正（`b102e43`：仓库与 JAR 内两份 `THIRD_PARTY_NOTICES.md` 开头加非官方同人作品声明，千禧年研讨会编制徽标改写为本项目成员参照《蔚蓝档案》（Blue Archive）中“千禧年研讨会”的标志自行像素化重绘的非官方二次创作，三张界面图集仍为原创；根 README 加“声明”一节），以及编制投票页在布局宽度不足 440 时用短标题“编制投票”（`883fcd6`，原为不足 400），让 427×240 下页头身份恢复显示；同一提交给编制页用例加了页头身份记录 `formationHeaderIdentity[...]`（见第 5 节）。核心先用限流脚本 `clean build`（连同六个附加测试源集的编译）：JUnit 876 项，0 失败、0 跳过；再跑 `runGameTestServer`：19 项必需 GameTest 全部通过；然后在同一工作区跑下面两轮。产物 `wok_infantry-0.4.0-beta.1.jar`，2,009,894 字节，SHA-256 `8324F63A34AAAB59A5A39444FC4DD0D10C1AB653DF8CC9F5FEED43C0EB178C73`（两轮跑完后重新核对，不变）；JAR 内 `META-INF/THIRD_PARTY_NOTICES.md` 与仓库内两份逐字节相同，是改正后的新版。
- zh_cn 严格轮（`runUiTestClient`，JourneyMap 6.0.2，不需要 TaCZ）：`status=PASS`，4158 tick，38 个用例、179 张截图，`strictLayoutViolations=0`。已迁移界面（kit、mapicons、formation、hud）在 5 个档位都没有违规；`layoutViolations=169` 全部来自未迁移的旧界面，只出报告（管理员终端 64、小队终端 49、配装 30、战术地图 26；按规则分为 GUI 1 下 1× 中文 114、禁用键没写原因 51、截断没有完整提示 3、“READY”占位 1），与 `final` 一轮逐条相同，各用例各档的结果也相同。Boss 条位移（GUI 像素）：320×240 右 53 下 35、427×240 右 16 下 69、480×270 右 29 下 35、640×336 下 39、960×720 下 77，与 `final` 一轮相同。页头身份：已加入阵营的 8 个编制页状态（vote、detail、locked、longcaps、pending、full、admin、admintie）在 427×240、480×270、640×336、960×720 四档都显示“学院军 · 阿尔法小队 · 指挥官”，320×240 照旧收起（40 条 `formationHeaderIdentity`）。归档 `D:\WOK步战测试\1.20.1-Forge_47.4.22\ui-acceptance\20261005-0.4.0-beta.1\zh_cn-final2\`。
- en_us 报告轮（`-PuiLang=en_us -PuiLayoutStrict=false`）：`status=PASS`（语义检查全部通过），4155 tick，179 张截图，`layoutViolations=168`、`strictLayoutViolations=0`，与 `final` 一轮逐条相同（构成见下一节），各用例各档的结果也相同。427 档编制页 15 个状态改用短标题“FORMATION VOTE”，页签因此放得下全名（`final` 一轮是“SQD / Role / DEP / Kit / Map / FMN”短名）；英文页签较长，页头身份在 5 个档位都仍收起（40 条 `formationHeaderIdentity` 全为 hidden；427 以外各档的编制页截图与 `final` 一轮逐字节相同）。归档 `D:\WOK步战测试\1.20.1-Forge_47.4.22\ui-acceptance\20261005-0.4.0-beta.1\en_us-final2\`。
- 427×240 修正前后（zh_cn，同名截图 `wok_ui_formation_vote_427x240.png`）：
  - 修正前 `...\20261005-0.4.0-beta.1\zh_cn-final\screenshots\wok_ui_formation_vote_427x240.png`：页头是“WOK步战 // 编制投票”加六个页签，右侧没有身份。
  - 修正后 `...\20261005-0.4.0-beta.1\zh_cn-final2\screenshots\wok_ui_formation_vote_427x240.png`：页头是“编制投票”加六个页签，右侧显示“学院军 · 阿尔法小队 · 指挥官”。管理员锁定确认 `wok_ui_formation_admin_427x240.png` 等其余 7 个已加入状态相同。
  - 与 `final` 一轮逐张比对 179 张：111 张逐字节相同；427 档编制页 15 个状态都不同，像素差异只在页头（标题改为短标题，已加入的 8 个状态多出身份）；其余 53 张（HUD 45 张、实时流程 01/03、管理员 12/13/14 与 `admin.noclass` 3 张）透出实时世界背景，水面、云等每轮不同，布局报告逐条相同。
- 标题前缀检查（在 `final` 一轮逐张核对的基础上，只看本轮变了的截图）：
  - 新外壳（编制投票页、组件陈列页、地图标点陈列页）：zh_cn 下五档的标题都完整显示、没有省略号，也不与页签、身份重叠；编制页 320、427 档用短标题“编制投票”，480 档起用完整标题“WOK步战 // 编制投票”。
  - 旧外壳（小队、配装、战术地图、弹药补给、武器调校）与英文其余各处的标题情况同 `final` 一轮（见下一节），本轮没有改动。
- 仍未做：第 7 节的真实客户端人工验收（测试端 PCL）和需要两个号的多人项目。部署到测试端由部署方完成（应部署上面 SHA-256 的 JAR）。

### 历史：0.4.0-beta.1 第一轮最终验收 final（2026-10-05，`3a24aec`）

本节的 JAR（SHA-256 `2BB57175…`）早于素材声明改正和 427×240 标题修正，已作废、没有部署；结论以上一节为准。下文的“改号前”指本版仍编号 0.3.0-beta.8 时。

- 代码：分支 `claude/新版界面` 的 `3a24aec`。它在本版仍编号 0.3.0-beta.8 时的最终提交 `8aaa570` 之上只改了四件事：界面标题前缀“WOK //”改为“WOK步战 //”（英文“WOK INFANTRY //”），版本号改为 0.4.0-beta.1，版本规范补 0.x 不兼容升次版本号的规则，两份素材声明统一。核心先用限流脚本 clean build（JUnit 875 项，0 失败、0 跳过）并跑 `runGameTestServer`（19 项全部通过），再在同一工作区跑下面两轮。产物 `wok_infantry-0.4.0-beta.1.jar`，SHA-256 `2BB571757469A689B7526BAAC7E6D07D018A9AC84EDFDF7129BC93FEB39B9106`。
- zh_cn 严格轮（`runUiTestClient`，JourneyMap 6.0.2，不需要 TaCZ）：`status=PASS`，4143 tick，38 个用例、179 张截图，`strictLayoutViolations=0`。已迁移界面（kit、mapicons、formation、hud）在 5 个档位都没有违规；`layoutViolations=169` 全部来自未迁移的旧界面，只出报告（管理员终端 64、小队终端 49、配装 30、战术地图 26），与改号前的 `zh_cn-final` 逐条相同，只有 960 档小队页、配装页两条“GUI 1 下 1× 中文”报告里的标题文字变了。Boss 条位移（GUI 像素）：320×240 右 53 下 35、427×240 右 16 下 69、480×270 右 29 下 35、640×336 下 39、960×720 下 77，与改号前相同。归档 `D:\WOK步战测试\1.20.1-Forge_47.4.22\ui-acceptance\20261005-0.4.0-beta.1\zh_cn-final\`。
- en_us 报告轮（`-PuiLang=en_us -PuiLayoutStrict=false`）：`status=PASS`（语义检查全部通过），4160 tick，179 张截图，`layoutViolations=168`、`strictLayoutViolations=0`。比改号前（161）多 7 条，都是“WOK INFANTRY //”前缀变长后 320 档标题被省略号截断且没有完整提示（`text-truncated-no-tip`）：组件陈列页 6 个状态、配装终端 1 张；小队终端 4 张和弹药补给 1 张的标题改号前就被截断，只是文字变了。已迁移界面合计 47 条（编制页 33、HUD 4、陈列页 8、标点陈列 2）；其余 121 条中 118 条是旧界面（管理员 64、小队 24、配装 22、地图 6、补给 2），3 条是只出报告的旧图 11（编制页 960 档）。归档 `D:\WOK步战测试\1.20.1-Forge_47.4.22\ui-acceptance\20261005-0.4.0-beta.1\en_us-final\`。
- 标题前缀检查（逐张看截图，并把新外壳 115 张截图页头右侧的身份区与改号前的归档逐张比对）：
  - 新外壳（编制投票页、组件陈列页、地图标点陈列页）：zh_cn 下 320×240、427×240、480×270、640×336、960×720 五档的标题都完整显示、没有省略号，也不与页签、身份重叠；编制页 320 档照旧用短标题“编制投票”。
  - 427×240 档已加入阵营、页头有页签的 8 个编制页状态（vote、detail、locked、longcaps、pending、full、admin、admintie）放不下“学院军 · 阿尔法小队 · 指挥官”这类长身份，身份按外壳规则收起（改号前这 8 张都显示）；其余 107 张的身份显示与改号前相同。示例：`zh_cn-final\screenshots\wok_ui_formation_vote_427x240.png`（收起）、`wok_ui_formation_vote_640x336.png`、`wok_ui_formation_vote_960x720.png`（都显示）。
  - 旧外壳（小队、配装、战术地图、弹药补给、武器调校）：标题宽度按“页头宽 − 身份宽 − 32”裁切，与身份之间固定留 12px，任何宽度都不会重叠；zh_cn 下 320 宽也放得下完整标题（`wok_ui_02_squads_320x240.png`、`wok_ui_04_map_320x240.png`、`wok_ui_08_loadout_320x240.png`、`wok_ui_ammo_small320_320x240.png`；武器调校没有自动截图，按公式核对）。427、640 档时旧界面按 1× 的 854×480、640×336 排版，比 320 宽松。没有改代码。
  - en_us：编制页 427 档起用完整标题“WOK INFANTRY // FORMATION VOTE”，页签用短名、身份收起，与改号前相同（改号前英文 427 档也不显示身份）；320 档组件陈列页标题带省略号，页签收成“‹ Keys 1/5 ›”翻页条（改号前是完整标题和五个完整页签；即 en_us 轮多出的报告）。组件陈列页 427×240、960×720 两档（6 个状态共 12 张）的页头身份“Academy · Alpha · Leader”也因标题变长收起（改号前显示；外壳规则，不算违规）。旧外壳在 320 档：配装终端标题新带省略号（报告多出的 1 条）；战术地图标题由完整变为“WOK INFANTRY // TACTI…”，它用 `drawString` 直接绘制、不经布局探针，所以不在报告数里（`wok_ui_04_map_320x240.png` 可见）；小队、补给改号前就带省略号。
- 改号前的归档（`20261004-0.3.0-beta.8\`、`20261005-0.3.0-beta.8\`）保留原目录名，只作参考。
- 仍未做：第 7 节的真实客户端人工验收（测试端 PCL）和需要两个号的多人项目。

以下三节是本版仍编号 0.3.0-beta.8 时的验收记录，保留作历史：其中的“本版”指当时的编号，归档目录名也保留 `0.3.0-beta.8`。本版结论以“0.4.0-beta.1 最终验收（2026-10-05，final2）”一节为准。

### 历史：本轮结果（2026-10-04，当时编号 0.3.0-beta.8）

**来源说明**：本节下面三轮（`zh_cn`、`en_us`、`zh_cn-review`）都跑在 B11b 分支上（`zh_cn-review` 跑的是其末端 `e5efe49` 的代码，之后的 `cbd2c46` 只改文档；另两轮更早），不含并行的 B11a 改动（编制页等待态与目录超时、Boss 条右移、服务端 `voteBlock` 与管理员第三步 `ADMIN_VOTE` 文案、登录推送目录、`FORMATION_CATALOG` 限流等，主源码 17 个文件）。两条分支在 `8ad1b74`/`41c03a4` 合并，合并后的代码没有用这三轮验收过；`en_us` 轮还早于审查修正提交 `e5efe49`。在最终提交上重新构建、重跑并用新标签归档这一步已经完成，见本节末尾“最终提交 0306a7b 的验收”，本版以那两份归档为准，CHANGELOG 的测试结果也已按它更新。

- zh_cn：`status=PASS`，139 张截图，严格违规 0；已迁移界面（kit 24、mapicons 8、formation 56、hud 32）全部 PASS，两条回归 PASS；旧图 10/11（`formation.legacy`）没有设为已迁移，只出报告（这一轮 0 条）。归档在 `D:\WOK步战测试\1.20.1-Forge_47.4.22\ui-acceptance\20261004-0.3.0-beta.8\zh_cn\`；审查修正（HUD 各状态先清空聊天、标点陈列页均分行、编制夹具调不到服务端方法即失败）后重跑一轮，结果相同（PASS，139 张，严格违规 0，报告 169 条），归档在同一日期目录的 `zh_cn-review\`，是合并前最后一轮。
- 合并后复跑（2026-10-05，整体审查工作区，代码等于 `41c03a4`，审查提交只改文档、注释和工具脚本）：zh_cn `status=PASS`，139 张截图，严格违规 0，报告 169 条，130 个“用例 × 档位”结果与 `zh_cn-review` 逐条相同（B11a 改过的 waiting、admintie、admin 等编制页状态在必过档都是 0 违规）；en_us 报告轮 `status=PASS`，报告 158 条，比合并前多 1 条：320 档等待态页头 “Reading formation info”（B11a 新文案）被省略号截断、没有完整提示，留给 i18n 收尾。这两轮没有归档到测试端，只说明合并本身没有引入严格违规；最终提交上的重跑与归档已完成（见本节末尾）。
- 旧界面只出报告的违规共 169 条：管理员终端 64（GUI 1 下 1× 中文 44、禁用键没写原因 20）、小队终端 49（1× 中文 26、禁用键没写原因 23：部署/重新部署/补给、退出/移交/踢出、创建）、配装 30（1× 中文 22、翻页键没写原因 8）、战术地图 26（1× 中文 22、支援键截断没有完整提示 3、“READY”占位 1）。它们分别留给地图（B7）、小队（B8）、配装/补给（B9）、管理员（B10）批次。
- en_us 报告轮（`-PuiLayoutStrict=false`，跑在审查修正 `e5efe49` 之前）：`status=PASS`（语义检查全部通过），139 张截图，共报告 157 条违规，归档在同一日期目录的 `en_us\`。其中已迁移界面 40 条，全部是英文文案比中文长而被省略号截断、没有完整提示：编制页投票摘要（“Administrator lock (no timer)”“3 (the faction picks one)”、领先编制名 + 票数）、管理员区说明、320 档详情的载具/能力行；HUD 投票条第二行在 960/640 档（“→ Formation to change”“to pick a squad and deploy”）；标点陈列页页头身份。其余 117 条是旧界面。英文措辞缩短或改成可换行留给后续的 i18n 收尾。

### 历史：整体审查后的重跑（2026-10-05，合并结果 41c03a4 + 整体审查修正）

- 新增 427×240 报告档（854×480 GUI 1）和两个用例：`formation.waitover`（B11a 的目录超时态）、`hud.boss`（B11a 的 Boss 条下移和右移）。
- zh_cn：`status=PASS`，179 张截图，严格违规 0，报告 169 条（全是旧界面，与上一轮相同）；38 个用例中已迁移界面在 5 个档位全部 PASS，427×240 档没有任何违规。Boss 条位移（GUI 像素）：320×240 右 53 下 35、427×240 右 16 下 69、480×270 右 29 下 35、640×336 与 960×720 不右移，名单与 Boss 条区域都不重叠。
- 这一轮在审查工作区 `ui-rv2` 的 `run/ui-test` 里跑，审查期间测试端只读，没有归档。合并后的重跑与归档已完成，见下一节。

### 历史：编号 0.3.0-beta.8 时最终提交 0306a7b 的验收（2026-10-05）

- 代码：分支 `claude/新版界面` 的 `0306a7b`（B11a/B11b 合并结果，再并入整体审查修正 rv1–rv4）。核心先在这个提交上用限流脚本 clean build，再在同一工作区跑开发客户端验收；部署到测试端的 `wok_infantry-0.3.0-beta.8.jar`（SHA-256 前缀 `61624BC3818F6ADA`）是同一提交的构建产物（后来在 `8aaa570` 改正客户端配置注释后重新构建并替换，测试端现存的这个文件 SHA-256 前缀为 `B5CA2792B984E55C`，代码行为不变，见 CHANGELOG）。
- zh_cn 严格轮（`runUiTestClient`，JourneyMap 6.0.2，不需要 TaCZ）：`status=PASS`，4163 tick，38 个用例、179 张截图。已迁移界面（kit、mapicons、formation、hud）在 5 个档位都没有违规，`strictLayoutViolations=0`；`layoutViolations=169` 全部来自未迁移的旧界面，只出报告（管理员终端 64、小队终端 49、配装 30、战术地图 26；按规则分为 GUI 1 下 1× 中文 114、禁用键没写原因 51、截断没有完整提示 3、“READY”占位 1），与上一轮相同。Boss 条位移（GUI 像素）：320×240 右 53 下 35、427×240 右 16 下 69、480×270 右 29 下 35、640×336 下 39、960×720 下 77，后两档不右移。归档 `D:\WOK步战测试\1.20.1-Forge_47.4.22\ui-acceptance\20261005-0.3.0-beta.8\zh_cn-final\`。
- en_us 报告轮（`-PuiLang=en_us -PuiLayoutStrict=false`）：`status=PASS`（语义检查全部通过），4160 tick，179 张截图，`layoutViolations=161`、`strictLayoutViolations=0`。已迁移界面 41 条（编制页 33、HUD 4、陈列页 2、标点陈列 2），全部是英文文案较长被省略号截断且没有完整提示（`text-truncated-no-tip`）；其余 120 条中 117 条是旧界面，3 条是只出报告的旧图 11（编制页 960 档，`formation.legacy`）。归档 `D:\WOK步战测试\1.20.1-Forge_47.4.22\ui-acceptance\20261005-0.3.0-beta.8\en_us-final\`。
- 编号 0.3.0-beta.8 时的自动验收以这两份归档为准（0.4.0-beta.1 以上面“0.4.0-beta.1 最终验收（2026-10-05，final2）”一节的 `zh_cn-final2`、`en_us-final2` 为准）；`20261004-0.3.0-beta.8\` 下的 `zh_cn`、`en_us`、`zh_cn-review` 三份早于合并，只作参考。
- 仍未做：第 7 节的真实客户端人工验收（测试端 PCL），以及需要两个号的多人项目（真实计票与锁定后进部署页、锁定后第二个号加入、PvP 下名单的倒地/阵亡状态）。

## 7. 只能真人看的

自动验收看不到、或只能近似的，需要在测试端 `D:\WOK步战测试\1.20.1-Forge_47.4.22` 用真实客户端看：

1. 真实显示器上的清晰度：GUI 1 的 2× 有没有重影；640×336 实际是 GUI 3（自动化用 GUI 2）；1080p GUI 4 的 480×270。
2. 和预览 PNG 并排看风格观感：配色语义、斜纹、焦点框、按钮悬停（自动化只截“停放鼠标”的画面，悬停只在陈列页和个别用例里出现）。
3. 真实鼠标和键盘手感：悬停过渡、滚轮、Tab 焦点、Enter/Space 不重复触发、Ctrl+Tab 切页。
4. 多人流程：真实计票、管理员开启与锁定后本阵营自动进部署页而对方不弹页、第二个号锁定后加入直接拿到编制、开着背包等非 WOK步战界面时锁定只出 HUD 通知。
5. 附属联动：部位血量的人形与“手/腿”伴随槽、占点面板在据点里的避让、倒地附属的真实面板（自动化只用替身占槽位）、指挥官支援的真实技能目录、TaCZ 枪械剪影。
6. HUD 叠放：Boss 条在据点面板和状态效果图标同时出现时的让位（自动化的 boss 状态只有名单和战况条，Boss 条是客户端放入的）、副手物品、F1 / F3 / 按住 Tab、夜间头顶标记亮度；锁定通知 3 秒内底边细线的缩短动画。
7. 按键：控制设置里“WOK步战核心”分类的键不标红、页脚和 HUD 键帽显示实际绑定的键（终端键是反引号时显示全角“～”，看是否认得出）、普通玩家按管理员键不出红字、装了 Xaero 世界地图时进入战局（编制已锁定）后按 M 直接进战术地图，投票中按 M 打开 Xaero 自己的地图。
8. 旧界面（小队、地图、配装、管理员、补给）换新配色后的过渡外观，以及 en_us 下的中英混排。
9. 测试开局（0.4.0-beta.2）：在黑色等待空间里点编制页的“测试开局”并确认，看聊天里的逐步回报、顶部黄色 Boss 条、部署后编制页自动关闭、出生在主世界本方主基地且为生存模式；阵亡后点“部署”立即出发；`/battle admin test mode off` 后 Boss 条消失、倒计时恢复、主基地仍在；退出重进与重启服务端后测试模式保持开启。局域网双人时用 `/battle admin test start <另一阵营> <编制> <玩家>` 送第二个号。

## 8. 维护

- 新界面批次只新增 `cases/<界面>Cases.java`，在 `UiCaseCatalog` 加一行；迁移完成的界面把用例设为 `migrated(true)`，并让界面实现 `UiSurfaceInfo`、用 `UiLayoutProbe.tag` 给按键打 uiId、用 `UiLayoutProbe.begin/end` 包住各区域。
- HUD 部件要用 `TacticalHud.plate/readout` 或 `HudPaint.probeBox` 画，直接 `drawString` 的文字探针看不到。
- 管理员视角的用例用 `FormationCases` 里的 `asAdministrator()`，玩家视角用 `asPlayer()`；不要直接改服务端的 OP 状态。
- 纯验收工具和文档的改动不单独提升版本，跟随所属核心版本。
