# WOK步战核心 UI 自动验收（0.4.0-beta.1 档 1，0.4.0-beta.2 补测试开局，0.4.0-beta.3 地图标点，0.5.0-beta.1 战斗终端三页与体力条 A4，0.5.0-beta.2 战况条据点小牌，0.5.0-beta.3 平板外壳 D2 与阵营涂装 P3）

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
| 只跑某组 / 某界面 / 某用例 | `-PuiCases=formation`、`-PuiCases=hud+kit`、`-PuiCases=formation.full-academy`（不含 `legacy` 时跳过实时流程和 14 张旧截图，约 1–2 分钟）。`-PuiCases=kit.default` 选中这个状态的全部涂装，`-PuiCases=caesar`（或 `academy`、`neutral`）选中该涂装的全部用例 |
| 只跑某些档位 | `-PuiTiers=320x240+640x336`（14 张旧截图不受影响）；`-PuiTiers=960x540` 另外打开只出报告的宽松档（见第 3 节） |
| 已迁移界面也只出报告 | `-PuiLayoutStrict=false` |
| 预览图目录 | `-PuiPreviewShots=<目录>`，默认向上查找相邻的 `ui-preview/shots`（index.html 并排对照用） |

列表参数可用 `,`、`;` 或 `+` 分隔。整轮（实时流程 + 87 个用例，424 张截图：0.5.0-beta.3 加上凯撒 / 中立涂装的 15 个用例和 1 个 HUD 回归用例后；0.5.0-beta.2 为 71 个用例、344 张，在本机约 6 分钟；0.5.0-beta.1 并入体力条 A4 后为 66 个用例、319 张；只有战斗终端时为 58 个用例、279 张，只有体力条时为 48 个用例、229 张，0.4.0-beta.2 / 0.4.0-beta.3 时为 40 个用例、189 张，0.4.0-beta.1 时为 38 个用例、179 张）在本机约 8 分钟。

**固定的运行属性**（`build.gradle` 的 `uiTestClient` 设定，0.5.0-beta.3 起）：

| 属性 | 值 | 作用 |
|---|---|---|
| `wok.ui.clock` | `21:30` | 平板状态栏的时钟（`TacticalBoardChrome.CLOCK_PROPERTY`）固定，探针文字和截图不随时间变 |
| `wok.ui.tabletAnimation` | `OFF` | 全部用例在“掏平板动画”关闭时跑。动画属于核心 0.5.0-beta.4，现在还没有代码读这个属性，先留好 |

结果文件开头记一行 `uiClock=21:30 (wok.ui.clock=21:30) tabletAnimation=OFF`。

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
| 960×540（选开） | 1920×1080，GUI 2 | 960×540：唯一用到 D2 宽松档机身（ROOMY）的档位 | 只出报告；默认不跑，`-PuiTiers=960x540` 写全名时才加到跑满 5 档的用例上（窗口在 1080p 显示器上放不下时记为 REPORT） |

语言：zh_cn 必过；en_us 只出报告（`-PuiLayoutStrict=false` 跑一轮）。

**D2 外壳下各档的内容区**（`TacticalShellLayout.content()`，页面实际拿到的宽高）：320×240 为 296×192，427×240 为 403×192，480×270 为 456×222，480×360（960×720 档）为 416×284，640×336 为 576×260，960×540 为 846×434。640×336 下小队页变窄版、480×360 下兵种页和部署页变窄版（方案 4.8），截图里能看到。

**涂装**：平板界面的用例都写明涂装，截图名带后缀 `-academy`（学院军，海军蓝）、`-caesar`（凯撒，正红）、`-neutral`（中立，浅钢灰）。

| 用例 | 涂装怎么来 |
|---|---|
| squad（战斗终端） | 不固定：终端按夹具自己判定（蓝方 → 学院军，红方 → 凯撒；加载中没有战局快照时看编制目录里加入的阵营），检查判定结果与用例写明的涂装一致 |
| formation、kit、mapicons、hud.terminal | 用 `TacticalLivery.pinForAcceptance` 固定：验收玩家在实时流程里已经有真实战局快照（本方），不固定的话未加入阵营的编制页也会跟着本方变色 |
| HUD 其余用例、旧界面、14 张旧截图 | 没有涂装（HUD 永远是 A 配色，旧界面档 1 不换壳） |

固定涂装只在该用例里有效，用例结束（包括清理失败）时由运行器释放。

**960×720 档的覆盖范围**：自动化的“960×720”档是 960×720 窗口、GUI 1。已迁移界面和 HUD 在客户端配置 `ui.minimumScale2x`（默认开）下按 2× 排成 480×360，所以这一档实际截图验收的是 480×360 排版；旧界面不受该配置影响，仍按 1× 的 960×720 排版截图（只出报告）。已迁移界面真正 1× 的 960×720 逻辑布局（例如 1920×1440 窗口 GUI 2，或关掉 `ui.minimumScale2x`）没有真实客户端截图，只由纯布局单测 `FormationScreenLayoutTest`（“960, 720, WIDE”）和 `WokHudLayoutTest`（960×720、绘制倍率 1）覆盖。AGENTS 要求的 960×720 档对已迁移界面目前就是这两部分合起来的结果，需要时可再加一个 1920×1440 GUI 2 的报告档。

## 4. 检查规则

每张截图对应的那一帧都由主源码的布局探针 `client/ui/probe` 记录框、文字、控件、裁剪和地图标点（生产环境没有接收端，不做任何事），再按 `UiLayoutReport` 检查：

- 文字出屏、出了所在框；框出父框；同级实体框重叠；begin/end 不配对；
- 文字被缩小（相对布局 < 1×）；含中文的文字物理字号 < 2（GUI 1 下 1× 中文看不清）；
- 文字或按键被省略号截断却没有完整内容提示；禁用控件没有写原因；控件出屏、按键文字出键；
- 中文界面出现 READY / LOCKED / INBOUND / COOLDOWN / OFFLINE / [SL] / [CO] 之类英文占位。

**已迁移界面**（陈列页、地图标点陈列、编制页、HUD，0.5.0-beta.1 起加上战斗终端的小队 / 兵种 / 部署三页）在必过档上有任何违规即失败；**未迁移的旧界面**（地图、配装、管理员、补给）只出报告，等各自的批次迁移后再转为严格。每个用例还有自己的语义检查（下表），语义检查在必过档上失败同样判整轮失败。

**平板外壳检查**（0.5.0-beta.3，`UiDeviceChecks`）：凡是写明涂装、截到 `TacticalScreen` 的用例，每张截图都要通过：

- 外壳在探针备注里写下这一帧判定的涂装和链路状态（`shell livery=CAESAR link=OK`），涂装与用例写明的一致；结果页和 `wok_ui_layout.json` 都显示它；
- 外壳区域 `shell.device`、`shell.status`、`shell.bezel` 都报给探针，而且都是非实心区域；
- 底框两端的 Esc（`shell.key.esc`）和 R（`shell.key.refresh`）硬件键都在、都在底框里；
- 有页面键的界面，当前页的键是按下状态（`CURRENT`）、在底框里，并且它上方的 LED 是唯一亮着的一盏（探针备注 `bezel.led lit=true key=[…]` 与这个键的矩形相同；底框太矮没有 LED 时不亮任何一盏）；
- 每个危险键（`DANGER` / `DANGER_ARMED`）都带警示斜纹，键上的文字从斜纹右边开始；
- 回执只在状态栏胶囊里：底框上没有不属于任何键的文字（原来的页脚回执已经没有了）；
- 文字和页面控件都不出玻璃区（S），只有底框上的实体键和浮动提示例外。

结果文件里每张截图记一行 `device[<用例>@<档位>]=livery=… link=… esc=… refresh=… page=<当前页>+led dangerKeys=<n>`。

## 5. 用例清单

截图名 `wok_ui_<界面>_<状态>[-<涂装>]_<档位>.png`（例如 `wok_ui_squad_kick-caesar_320x240.png`），用例 id 同样带涂装后缀（`squad.kick-caesar`）；14 张旧截图保留原名和顺序，HUD 与旧界面没有涂装后缀。

| 组 / 界面 | 状态 | 档位 | 迁移 | 主要语义检查 |
|---|---|---|---|---|
| 实时流程（旧图 01–09） | deployment、squads、classes、map ×2、squads 960、commander、loadout ×2 | 各自原档位 | 否 | 终端键/地图键走真实按键与网络；兵种页真实鼠标点击；JM 小地图隐藏、全屏重定向、4/4 与 16/16 地形块；支援按钮与 R80/R64；两张地图截图记录 `mapIcons[...]`（按种类计数、按方案 B 尺寸），一个新标点都没画出来即失败（0.4.0-beta.3） |
| formation.legacy（旧图 10、11） | 320 待开启、960 投票中 | 320、960 | 否 | 管理员“开启编制投票 / 锁定投票结果”按 uiId `formation.admin.open` / `formation.admin.lock` 找到 |
| admin.legacy_*（旧图 12–14） | 列表 320、职业管理 320、列表 960 | 320、960 | 否 | “+槽位”“设置”“职业管理”存在 |
| kit（组件陈列页） | 学院军：default、confirm、inputs、cards、hud、icons；凯撒与中立：default、confirm、cards（0.5.0-beta.3，涂装固定） | 全部 5 档 | 是 | 按钮七态 + 焦点；超长键省略号带完整提示；危险确认默认焦点在“取消”、Enter 不确认、Esc 取消；48 个界面图标（0.5.0-beta.1 追加体力条的手掌、靴子、跳跃箭头）；10 种标点物理尺寸；0.5.0-beta.3 起页面是底框页面键、R 键重画本页，回执“配装已保存”在状态栏胶囊里，状态栏身份随涂装（学院军 / 凯撒 / 未加入阵营），HUD 页的样品始终按 A 配色画 |
| mapicons（地图标点陈列） | dark（深色地形）、paper（浅色纸图），学院军涂装固定 | 全部 5 档 | 是 | 10 种标点 + 名称；普通/悬停/选中/即将过期；宽屏另有 0.75×/1.25×/1.75× 旋钮；每个标点物理尺寸 = 15×15（定位针 15×18）美术像素 × 旋钮对应的整物理像素（0.4.0-beta.3 起与地图同用 `TacticalMapIcons.mapArtPx`：GUI 1–3 不变，GUI 4 放大 4/3 后取整），与战术地图上一致；0.5.0-beta.3 起两种底图是底框页面键，R 键禁用并写原因（截到斜线禁用的硬件键），标点名称始终是深色底板上的 A 浅色字 |
| formation（编制页，预览 45-formation） | join、confirm、facfull、vote、detail、locked、latejoin、lateconfirm、waiting、waitover（目录请求 3 秒没有回应）、longcaps（玩家视角）；pending、full、admintie、admin，以及 0.4.0-beta.2 的 testmode（未加入阵营时的“测试开局”键，页面状态为 join）、testconfirm（测试开局确认层）（管理员视角）。0.5.0-beta.3 起未加入的状态（join、confirm、facfull、latejoin、lateconfirm、waiting、waitover、testmode、testconfirm）为中立，已加入的为学院军；另有凯撒视角的 vote、locked（锁定 `caesar_234_mechanized`）、admin（危险确认）。涂装都固定 | 全部 5 档 | 是 | 页面自报预览状态；凯撒 locked 页确实是凯撒阵营并锁定了 `caesar_234_mechanized`；管理员在未加入阵营时看得到可用的“测试开局”键且它单独在列表面板的紧凑管理员区，pending 状态下它与“开启编制投票”同一行、不重叠，玩家视角看不到它；测试开局为普通确认、正文写出所加入的阵营、Esc 取消后留在本页；waitover 写出“编制目录没有送达”并保留重试键；夹具未被服务端目录替换；详情区没有内部 ID（player-09）；裁剪区内没有半行；浏览中的阵营是描边而非实心蓝；未加入时 Esc 能关页；加入为普通确认、锁定为危险确认且默认“取消”；阵营满员/容量不足时按键禁用并写原因；锁定后加入仍可加入；5 个白名单支援全部按名称显示；已加入阵营时逐档记录页头身份是否显示（`formationHeaderIdentity[...]`），中文 427×240 档必须显示（页面在不足 440 宽时用短标题“编制投票”；427 档只出报告，不显示记为 REPORT） |
| admin.noclass（admin-01 回归） | 编制没有任何职业规则 | 320、960、640 | 否 | “职业管理”禁用并写原因；真实点击和强行 onPress 都不崩溃、不离开列表页 |
| ammo.small320（player-01 回归） | 小型弹药箱 | 320（480 报告） | 否 | 剩余点数和点数条不在分区标题下面，位于面板顶和分区标题之间 |
| squad（战斗终端，预览 20-squad，0.5.0-beta.1） | squads、other、nosquad、kick、classes、classesnosquad、classesactive、deployment、active、loading、votewait、vote、voteclasses、votedeploy，以及 2 / 8 / 15 / 16 个部署点（页面状态为 deployment），都是学院军；0.5.0-beta.3 起另有凯撒视角的 squads、kick、classes、deployment、active、loading（同一套夹具换到红方：己方 / 敌方阵营对调、编制 `caesar_234_mechanized`、红方部署点）。涂装都不固定，由终端自己判定 | 全部 5 档 | 是 | 页面自报预览状态；凯撒 kick 的确认键是红色的确认态危险键（`DANGER_ARMED`），压在同样是红色的名单选中行（`SELECTED`）上，靠斜纹和竖条区分；loading 的链路状态是 `WAIT`；客户端夹具 `SquadFixtures` 在截图时没被服务端快照换掉；裁剪区内没有半行；查看别队与踢出经真实点击（呼号条或小队列表、名单行、踢出键）到达；踢出为红色危险确认、焦点在“取消”、Tab 到红键后 Enter 和 Space 都不确认、Esc 取消且终端还在；部署点列表的页数等于实际页数、首页每一行都画出、标题写出同一页码；编制锁定前没有可点的建队 / 兵种 / 部署控件，且有通往编制页的键 |
| hud（战斗 HUD，预览 10-hud） | battle、chat、downed、roster8、votewait、vote、voted、locked、boss（8 人名单 + 客户端放入的原版 Boss 条） | 全部 5 档 | 是 | 核心部件不碰快捷栏和状态行；体力条凹槽正好在原版经验条那一行 `[h−30, h−23)`、与 `StaminaBarLayout` 算出的位置一致，各部件不碰快捷栏、选中框、副手格、状态行、TaCZ 读数禁区和聊天（2× 耳朵在聊天最后一行之上时必须在聊天背景右边），布局宽 ≥ 640 时两只耳朵各有一个百分比、否则没有，原版经验条和跳跃条的覆盖层没有运行；boss 状态下移、右移后的 Boss 条区域不碰名单、不出屏；320 档名单底边 ≤ y95；窄屏开聊天时名单收成一行；投票阶段投票条占战况条槽位、没有名单、写出终端键；锁定通知 3 秒内截到；倒地时本人行写“倒地”，替身附属面板拿到 `center_low` 槽位 |
| hud.capture*（站在占点据点内，预览 10-hud 新版，0.5.0-beta.2） | capture（己方 ×2 占领中 62%，剩余 0:09）、capture-contested（争夺，进度冻结）、capture-secured（己方已控制 100%）、capture-disabled（据点停用）、capture-locked（敌方占领中、本方暂无资格） | 全部 5 档 | 是 | 核心验收不装占点附属，由 `CaptureHudBridge.pinForAcceptance` 按本人阵营注入据点（与占点 0.1.0-alpha.4 `CaptureHudApi.currentPoint()` 同一套字段）；hud 的全部检查之外：据点小牌 `hud.objective` 在战况条内，探针记录的外观、贴边色（己方蓝 / 敌方红 / 争夺橙 / 停用灰）、数字或“停用”、锁、实心与否都符合该状态；非紧凑屏战况条 30 高且画出第二行“据点名 · 状态 · 剩余”，紧凑屏 17 高且只留小牌；兵力数字不压小牌 |
| hud.terminal-caesar（HUD 回归，0.5.0-beta.3） | 战斗 HUD 上用凯撒涂装打开战斗终端，再按 Esc 关掉，然后截 HUD | 全部 5 档 | 是 | 终端开着时：终端是凯撒红，核心 HUD（名单、战况条、投票条、体力条）一个探针框都不画，`HudFrame` 记着终端开着（附属 HUD 的槽位照常给）；关掉之后：所有可换肤令牌都回到 A 值、当前调色板是 A，体力条写给探针的颜色与 A 配色下算出的一致；其余同 hud |
| stamina-a4（体力条，预览 16-stamina 的 A4；组 `hud`） | full、sprint、aim、legsout、recover、unlock、vehicle、horse（与预览状态同名，结果页并排显示预览图） | 全部 5 档 | 是 | 战斗 HUD 上把体力条固定为预览的演示数据（满体力；疾跑腿 64% 残影 69%；开镜手 42% 橙；腿耗尽锁定；双池恢复中；腿 16% 刚解锁；乘坐载具腿灰；骑马蓄力 55%），上一行 hud 的全部体力条检查之外，再核对体力条写给探针的状态说明：剪影/锁/勾/箭头、贴边颜色、填充色、残影、恢复中、锁定空槽暗红、坐骑状态。验收客户端里玩家并没有真的骑乘，所以没有预览里的坐骑血量行 |

用例数据对齐预览 `data/mock.js`：编制目录由核心默认配置 `FormationConfigData.defaultConfig()` 生成，详情、候选可用性及原因、旧版摘要行都调用服务端 `FormationService` 自己的方法（反射），夹具不重写任何服务端措辞；验收客户端没有卓越前线载具 MOD，可用性按“装了载具 MOD 的服务端”判定。HUD 状态只改客户端缓存（战局快照、兵力、体力、编制目录），每 tick 和每帧 HUD 前重装，服务端心跳不会混进截图；用例结束后恢复。管理员视角的用例临时授予 OP，玩家视角的用例收回，结束时由验收器统一撤销。

## 6. 结果在哪

| 文件 | 内容 |
|---|---|
| `run/ui-test/ui-test-results/wok_ui_acceptance.txt` | 首行 `status=PASS/FAIL`；失败时 `failure=`；随后是实时流程观测项和每个用例每个档位一行 `case[...]=PASS/FAIL/REPORT` |
| `run/ui-test/ui-test-results/wok_ui_layout.json` | 每张截图的档位、布局尺寸、违规（规则、文字/框/uiId、矩形）、控件和框；平板界面另有 `livery`（用例写明的涂装）和 `shell`（外壳实际画出的 `livery=… link=…`），探针备注在 `notes` |
| `run/ui-test/ui-test-results/wok_ui_manifest.json` | 截图清单与状态（平板界面带 `livery`） |
| `run/ui-test/ui-test-results/index.html` | Java 截图与预览图并排，下面列违规；标题旁的色块是涂装，说明行写外壳备注。预览图先找同名涂装导出 `<界面>__new__<预览档>__<状态>-<涂装>.png`，没有就用同一状态的旧版预览图并注明“无某涂装预览”，再没有写“无预览” |
| `run/ui-test/ui-test-results/wok_ui_progress.txt` | 进度；客户端没写结果就退出时，Gradle 报出最后到达的阶段 |
| `run/ui-test/screenshots/` | 截图 |

### 0.5.0-beta.3 验收（2026-10-10，平板外壳 D2 与阵营涂装 P3：final-zh、final-en）

本版的自动验收以本节 `final-zh`、`final-en` 两份归档为准。

- 代码：分支 `claude/平板外壳` 的 `c52530d`（本版最后一个源码提交，整体审查修正；之后只改文档）。限流脚本跑 `cleanTest test` 加六个附加测试源集编译：JUnit 1267 项，0 失败、0 跳过（`:test` 重新执行）；`runGameTestServer`：22 项必需 GameTest 全部通过。8 个模块逐个 `clean build` 全部成功。产物 `wok_infantry-0.5.0-beta.3.jar`，2,437,934 字节，SHA-256 `0EAC020C721E82800F3A116D8FF5D2B2E3BB7BA49DB0919F2381B501B5B5D80D`，已部署到测试端（0.5.0-beta.2 改名为 `.bak`，见 CHANGELOG）。
- 用例与档位：87 个用例、424 张截图（0.5.0-beta.2 的 71 个用例 + 凯撒战斗终端 6 + 凯撒编制页 3 + 组件陈列页凯撒 / 中立各 3 + HUD 回归 `hud.terminal-caesar` 1）；默认 5 档：320×240、960×720（GUI 1，按 2× 排成 480×360）、640×336 必过，480×270、427×240 只出报告；960×540 宽松档是选开的，本轮没跑。整轮在本机约 8 分钟。
- 涂装：58 个平板界面用例 × 5 档 = 290 张截图都写明涂装并跑了平板外壳检查——学院军 34 个用例（战斗终端 18、已加入的编制页 8、组件陈列页 6、地图标点陈列页 2），凯撒 12 个（战斗终端 6、编制页 3、组件陈列页 3），中立 12 个（未加入的编制页 9、组件陈列页 3）。外壳记录：学院军 OK 165、WAIT 5，凯撒 OK 55、WAIT 5，中立 OK 50、WAIT 10（WAIT 是战斗终端 `loading` 与编制页 `waiting` / `waitover`）。战斗终端的涂装由终端按夹具自己判定，其余固定。
- zh_cn 严格轮：`status=PASS`，8678 tick，424 张截图，`strictLayoutViolations=0`，`layoutViolations=121`，`uiClock=21:30`，`tabletAnimation=OFF`，`finalActiveMarkers=1`，`temporaryOperatorCleanup=revoked`。用例行 415 = 410 PASS + 5 REPORT（`admin.legacy_list@320x240`、`admin.legacy_list_large@960x720`、`admin.noclass` 的 320 / 960 / 640 三档）+ 0 FAIL。121 条违规全在只出报告的旧界面或实时流程截图上（管理员配装 64、玩家配装 30、战术地图 26、实时流程 `wok_ui_01_deployment_320x240` 的“先加入一个未满员小队”1 条，与 0.5.0-beta.1、0.5.0-beta.2 相同），已迁移界面 0 条。04 / 06 两张地图截图的 `mapIcons` 不变（8 种标点各 1 个，牌 30 物理像素）；`formationHeaderIdentity` 在中文 427×240 下已加入的状态全部显示（960×720 档的 detail、longcaps、admintie、admin 学院军因 D2 内容区变窄收起，只要求 427 档）。与 `8abd040` 那一轮 `device-r1-zh` 比：用例行、外壳记录、违规逐条相同。归档 `D:\WOK步战测试\1.20.1-Forge_47.4.22\ui-acceptance\20261010-0.5.0-beta.3\final-zh\`（424 张截图与 5 个结果文件）。
- en_us 报告轮（`-PuiLang=en_us -PuiLayoutStrict=false`）：`status=PASS`，8672 tick，424 张截图，`strictLayoutViolations=0`，`layoutViolations=223`（0.5.0-beta.2 为 229）；用例行 415 = 336 PASS + 79 REPORT + 0 FAIL。已迁移界面上的 121 条（小队终端 66、编制页 49、HUD 4、地图标点陈列 2）全是英文太长被省略号截断，留给 i18n 收尾（“Administrator lock (no timer)”×23、“Resupply needs you deployed inside the main base”×14、“3 (the faction picks one)”×8 等）；其余 102 条是只出报告的旧界面与实时流程截图。与 `device-r1-en` 比用例行、外壳记录、违规全部相同。归档 `D:\WOK步战测试\1.20.1-Forge_47.4.22\ui-acceptance\20261010-0.5.0-beta.3\final-en\`。
- 同一日期目录下：`device-r1-zh` / `device-r1-en` 是 `8abd040`（实机截图对照修正）那一轮，结果与最终一轮相同；`device-zh`、`device-en`、`device-en-recheck`、`device-en-liveflow` 是更早的几轮（zh 严格轮 320×240 有 6 项严格违规、英文 `kit.default-caesar@960x720` 被手动最大化的窗口跑坏、英文实时流程找不到“兵种”键），都由 `8abd040` 修正，只作对照。
- 截图目视与像素比对（对照预览 `device-2-livery` 的 3 档 × 3 涂装）：960×720 凯撒底框与预览 0 个像素不同（容差 30）；中立左右与顶边框、学院军状态栏与小队列表面板（含玻璃反光）在容差 3 内相同，差别只有时钟数字、个别丝印像素、禁用斜纹相位和字体字形。宽窄翻转与方案 4.8 一致（640×336 小队页、480×360 兵种 / 部署页和编制详情变窄或单列）。
- 与预览有意不同、已决定保持的两处：
  1. 未加入阵营时编制页正在浏览的阵营键画描边 + 选中色竖条 + 眼睛图标，不画成预览里的实心选中（用户 10-04 反馈 1，`FormationVoteModelTest` 固定）。
  2. 部署页重生倒计时“N 秒”和它的指示灯仍用正文色 / 灰色（TEXT / MUTED），不用预览里的橙色：橙色只表示分区与可调控件。
- 另外接受的外观差别：机身四周透出实时世界（截图不再逐字节相同），640×336、960×720 下原版快捷栏在底框下方压暗地露出一条（方案 4.9）；未加入时编制页底框有禁用的“R 重试”和“Esc 关闭（～ 键可重开）”，预览这里没有 R 键；身份比预览长（Java 没有编制短名）。
- 仍未做：真实客户端（测试端 PCL）人工验收，由用户来做，清单如下（方案 5.2 第 4 步，细项见第 7 节第 13 条）：
  - 三种涂装都要看：没加入阵营时看中立；`/battle admin test start academy` 看学院军；`/battle admin test start caesar` 看凯撒。
  - 窗口：960×720 GUI 1、320×240、640×336。
  - 检查项：文字是否溢出、控件是否重叠；按键七态，外加确认态危险键；字体清晰度；底框键：点击、Ctrl+Tab、←/→、Esc、R；踢人确认弹窗；反馈胶囊、信号格；HUD 没有变色；切到地图或配装页签时的过渡外观；帧率。
  - 顺带核对整体审查留下的一项：在战斗终端里用 Tab 把焦点移到“R 刷新”，等一个新快照（或按 R），焦点框应仍在 R 上。

### 0.5.0-beta.2 验收（2026-10-05，战况条据点小牌：zh_cn-final2、en_us）

- 最终（独立审查修正后）：代码 `565a16f`（本版最后一个源码提交；审查修正只改了顶部没有底板时 `top_center_next` 槽位的起点）。限流脚本 `clean build`（连同六个附加测试源集的编译）：JUnit 1007 项，0 失败、0 跳过；`runGameTestServer`：22 项必需 GameTest 全部通过。产物 `wok_infantry-0.5.0-beta.2.jar`，2,342,088 字节，SHA-256 `4727A2CBB64B24C34079FD185DA9E79A6DB7A0C74073C2764178A4E6583551A9`。zh_cn 严格轮：`status=PASS`，7177 tick，71 个用例、344 张截图，`strictLayoutViolations=0`，`layoutViolations=121`，335 行用例结果与下面审查前的 `zh_cn-final` 逐行相同。归档 `D:\WOK步战测试\1.20.1-Forge_47.4.22\ui-acceptance\20261005-0.5.0-beta.2\zh_cn-final2\`。en_us 报告轮没有重跑（见下面审查前的数字）。占点附属的 `runUiTestClient` 审查后每张 HUD 图带一条原版 Boss 条，对本版与 0.5.0-beta.1 都 PASS（截图在 `wok_capture_points/run/ui-acceptance/20261005-0.1.0-alpha.4/review-core-0.5.0-beta.*/`）。
- 审查前：分支 `claude/占点HUD` 的 `4934b45`。核心用限流脚本 `clean build`（连同六个附加测试源集的编译）：JUnit 1006 项，0 失败、0 跳过（0.5.0-beta.1 的 989 + 据点小牌 17）；`runGameTestServer`：22 项必需 GameTest 全部通过。产物（已作废）2,342,092 字节，SHA-256 `847D700262BA9F80BF371340ABDB5FDFCB6C4A658C3AAFE03E5E714C6FEF1E66`。
- 审查前 zh_cn 严格轮：`status=PASS`，7174 tick，71 个用例、344 张截图，`strictLayoutViolations=0`，`layoutViolations=121`（与 0.5.0-beta.1 相同，全是只出报告的旧界面）。新增 `hud.capture*` 5 个用例 × 5 档 = 25 张全部 PASS、0 违规；其余 310 行与 `20261005-0.5.0-beta.1\zh_cn-merged` 逐行相同。归档 `D:\WOK步战测试\1.20.1-Forge_47.4.22\ui-acceptance\20261005-0.5.0-beta.2\zh_cn-final\`。同目录的 `zh_cn\` 是第二行加“先省据点名”之前的首轮，用例结果与 `zh_cn-final` 逐行相同，只作对照。
- en_us 报告轮（`-PuiLang=en_us -PuiLayoutStrict=false`）：`status=PASS`，7173 tick，344 张截图，`strictLayoutViolations=0`，`layoutViolations=229`（与 `en_us-merged` 相同）；`hud.capture*` 25 张 0 违规；其余 310 行与 `en_us-merged` 逐行相同。归档 `…\20261005-0.5.0-beta.2\en_us\`。
- 占点附属自己的 `runUiTestClient`（共用 `run/ui-test`）对本版与 0.5.0-beta.1 各跑一轮，都 PASS：本版读到据点就画小牌、占点不画；关闭 `hud.showBattleStrip` 后占点薄条进 `top_center_next`；0.5.0-beta.1 下占点画薄条。截图在 `wok_capture_points/run/ui-acceptance/20261005-0.1.0-alpha.4/`（不在核心清单里，不进 `archiveUiAcceptance`）。

### 0.5.0-beta.1 最终验收（2026-10-05，并入体力条 A4 后：zh_cn-merged、en_us-merged）

本版的自动验收以本节两份归档为准。本版由 `claude/新版界面` 依次并入 0.4.0-beta.3（地图标点）、`claude/战斗终端`（战斗终端三页）和 `claude/体力条`（体力条 A4）而成；两个分支合并前各自的结果见下一节，保留为历史。

- 代码：`claude/新版界面` 的 `91f0c93`（合并体力条后的最后一个源码提交，之后只改文档）。核心用限流脚本 `clean build`（连同六个附加测试源集的编译）：JUnit 989 项，0 失败、0 跳过（0.4.0-beta.3 的 922 + 战斗终端 46 + 体力条 21）；`runGameTestServer`：22 项必需 GameTest 全部通过。产物 `wok_infantry-0.5.0-beta.1.jar`，2,326,073 字节，SHA-256 `A930A26E5BBD92F0B35BFBB43BCDA1714D361650AC3BD27A9393403AF2FA0BFC`（两轮跑完后重新核对，不变）；独立安装检查与 `verify_versions.ps1 -Release` 都 PASS。尚未部署到测试端。
- zh_cn 严格轮（`runUiTestClient`）：`status=PASS`，6744 tick，66 个用例、319 张截图，`strictLayoutViolations=0`。`squad.*` 18 个用例、`stamina-a4.*` 8 个用例在 5 个档位（含 480×270、427×240 报告档）共 90 + 40 张截图全部 PASS、0 违规。`layoutViolations=121`，与战斗终端分支的 `zh_cn-final3` 逐张相同：管理员终端 64，实时流程截图 57（配装 30、战术地图 26、部署页 1：`wok_ui_01` 里 320 档“先加入一个未满员小队”被截断、没有完整提示，实时流程截图只出报告）。310 行用例结果：270 行与 `zh_cn-final3` 逐行相同，220 行与体力条分支的 `stamina-a4-review-zh_cn` 逐行相同（共有的 180 行两边一致），没有缺少、多出或改变的行。两张地图截图的 `mapIcons[...]` 都画出 8 种标点各 1 个、牌 30 物理像素，960 档另有 6 个工具键图标；`finalActiveMarkers=1`。归档 `D:\WOK步战测试\1.20.1-Forge_47.4.22\ui-acceptance\20261005-0.5.0-beta.1\zh_cn-merged\`。
- en_us 报告轮（`-PuiLang=en_us -PuiLayoutStrict=false`）：`status=PASS`，6736 tick，66 个用例、319 张截图，`strictLayoutViolations=0`，`layoutViolations=229`，与 `en_us-final` 逐张相同；体力条 40 张 0 违规（只有百分比数字，没有可翻译文字）；270 行用例结果与 `en_us-final` 逐行相同。归档 `D:\WOK步战测试\1.20.1-Forge_47.4.22\ui-acceptance\20261005-0.5.0-beta.1\en_us-merged\`。
- 代表截图（`zh_cn-merged\screenshots\`）：战斗终端 `wok_ui_squad_squads_640x336.png`、体力条腿锁定 `wok_ui_stamina-a4_legsout_640x336.png`、地图 `wok_ui_04_map_320x240.png`。
- 仍未做：第 7 节第 10、11 条的真实客户端人工验收（含需要两个号的项目）。

### 0.5.0-beta.1 合并前（分支）验收（2026-10-05，历史）

战斗终端分支（审查修正后：zh_cn-final3、en_us-final）。同一日期目录下的 `zh_cn-final`（界面提交 `e1c44d6`，产物已作废）和 `zh_cn-final2`（审查途中的 `0cd6377`）保留作对照，三轮 zh_cn 的 270 行用例结果逐行相同。这一节的产物已被合并后的构建取代，作废、未部署。

- 代码：分支 `claude/战斗终端` 的 `60abe6d`（分支最后一个源码提交，在 0.4.0-beta.2 之上，不含 0.4.0-beta.3 的地图标点）。核心先用限流脚本 `clean build`（连同六个附加测试源集的编译）：JUnit 953 项，0 失败、0 跳过；`runGameTestServer`：22 项必需 GameTest 全部通过。产物 `wok_infantry-0.5.0-beta.1.jar`，2,292,921 字节，SHA-256 `5423FADFF84A2FFA4742EC41079252ED35B471258A79B3791DB4E75CC94C3D5F`（两轮跑完后重新核对，不变）。
- zh_cn 严格轮（`runUiTestClient`）：`status=PASS`，6029 tick，58 个用例、279 张截图，`strictLayoutViolations=0`，`layoutViolations=121`（全是未迁移的旧界面；0.4.0-beta.2 为 169，少掉的是旧小队页）。新迁移的 `squad.*` 18 个用例在 5 个档位（含 480×270、427×240 报告档）共 90 张截图全部 PASS、0 违规；14 张旧截图名称和顺序不变，01–09 里的小队 / 兵种 / 部署画面换成新界面（预期）；`finalActiveMarkers=1`。归档 `D:\WOK步战测试\1.20.1-Forge_47.4.22\ui-acceptance\20261005-0.5.0-beta.1\zh_cn-final3\`。
- en_us 报告轮（`-PuiLang=en_us -PuiLayoutStrict=false`）：`status=PASS`，6042 tick，279 张截图，`strictLayoutViolations=0`，`layoutViolations=229`。新三页 77 条，全部是英文较长被省略号截断、没有完整提示（部署页作战区说明 “Resupply needs you deployed inside the main base” 一句占 18 条，其余是投票等待区、部署清单、320 档指挥官名、640 档“我的状态”编制名和 320 档 “Promote” 键）；其余 152 条不在新三页：已迁移界面原有的英文截断 51 条（编制页 37、陈列页 8、HUD 4、标点陈列 2），只出报告的旧界面 101 条（管理员终端 64、实时流程截图 32、旧图 10/11 编制页 3、弹药补给 2）。留给 i18n 收尾。归档 `D:\WOK步战测试\1.20.1-Forge_47.4.22\ui-acceptance\20261005-0.5.0-beta.1\en_us-final\`。

体力条分支（审查修正后：stamina-a4-review-zh_cn）。分支在 0.4.0-beta.3 之上、没有改版本号，所以归档在 0.4.0-beta.3 的日期目录下；产物仍叫 `wok_infantry-0.4.0-beta.3.jar`，作废、未部署。

- 代码：分支 `claude/体力条` 的 `b2dd63f`。`clean build`（连同六个附加测试源集的编译）：JUnit 943 项全过（比 0.4.0-beta.3 多 21 项）；`runGameTestServer`：22 项必需 GameTest 全部通过。
- zh_cn 严格轮：`status=PASS`，5033 tick，48 个用例、229 张截图，`strictLayoutViolations=0`，`layoutViolations=169`（全是当时的旧界面，含旧小队页）；原有 180 行与 0.4.0-beta.3 逐行相同，新增的 stamina-a4 40 行全部 PASS、0 违规。归档 `D:\WOK步战测试\1.20.1-Forge_47.4.22\ui-acceptance\20261005-0.4.0-beta.3\stamina-a4-review-zh_cn\`（实现者那一轮 `stamina-a4-pre0.5.0-zh_cn` 的 220 行与它逐行相同）。en_us 轮没有跑。

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
5. 附属联动：部位血量的人形（0.5.0-beta.1 起体力不再画进人形下的“手/腿”伴随槽，旧部位血量仍预留那条槽、只是空着）、占点据点（0.5.0-beta.2 起由核心画进战况条，自动化只注入了数据；装占点 0.1.0-alpha.4 进真实据点看小牌颜色随占领方变化、剩余时间每次同步跳动是否可接受、走出据点小牌和第二行消失；关闭 `hud.showBattleStrip` 后看占点薄条；旧占点 alpha.3 仍画大面板时核心的避让）、倒地附属的真实面板（自动化只用替身占槽位）、指挥官支援的真实技能目录、TaCZ 枪械剪影。
6. HUD 叠放：Boss 条在据点面板和状态效果图标同时出现时的让位（自动化的 boss 状态只有名单和战况条，Boss 条是客户端放入的）、副手物品、F1 / F3 / 按住 Tab、夜间头顶标记亮度；锁定通知 3 秒内底边细线的缩短动画。
7. 按键：控制设置里“WOK步战核心”分类的键不标红、页脚和 HUD 键帽显示实际绑定的键（终端键是反引号时显示全角“～”，看是否认得出）、普通玩家按管理员键不出红字、装了 Xaero 世界地图时进入战局（编制已锁定）后按 M 直接进战术地图，投票中按 M 打开 Xaero 自己的地图。
8. 旧界面（地图、配装、管理员、补给）换新配色后的过渡外观，以及 en_us 下的中英混排。
9. 测试开局（0.4.0-beta.2）：在黑色等待空间里点编制页的“测试开局”并确认，看聊天里的逐步回报、顶部黄色 Boss 条、部署后编制页自动关闭、出生在主世界本方主基地且为生存模式；阵亡后点“部署”立即出发；`/battle admin test mode off` 后 Boss 条消失、倒计时恢复、主基地仍在；退出重进与重启服务端后测试模式保持开启。局域网双人时用 `/battle admin test start <另一阵营> <编制> <玩家>` 送第二个号。

10. 战斗终端（0.5.0-beta.1）：按 `` ` `` 打开后小队 / 兵种 / 部署三页在窄屏（320×240）与大屏（640×336 实际 GUI 3）的文字、按键状态和确认层；真实鼠标点呼号、名单行、兵种行和部署点（部署页方位图上的点也能点选）；踢出、退出、解散、重新部署的红色确认只能用鼠标点、Enter 不确认，移交队长、移交指挥权为普通确认，卸任指挥官不弹；确认层开着时对方离队或小队进入作战，确认层自动关闭并在页脚写“情况已变化，未执行”；部署成功后终端自动关闭，登录与重生自动打开部署页（终端已开着时原地切到部署页）；投票锁定前三页都是投票等待区。需要两个号的：踢人与 60 秒冷却倒计时、移交队长、把指挥权移交给别队在线队长、作战中踢人。

11. 体力条 A4（0.5.0-beta.1）：联网时疾跑、开镜的残影和恢复节奏；腿耗尽锁疾跑、回到 15 时的绿勾；真骑马的跳跃蓄力与骆驼冲刺冷却变灰、坐卓越前线载具时腿段变灰（uiTest 里体力状态是固定注入的，玩家没有真的骑乘，截图里没有坐骑血量行）；和部位血量人形、TaCZ 弹药读数同屏；854×480 GUI 1 窗口的耳朵是否太小（左耳会碰聊天时退回 1×）；`hud.staminaBar` 开关切换后原版经验条与跳跃条是否回来；卓越前线自带的体力线 `stamina_hud` 关掉后凹槽底边是否干净（开着时它压在凹槽底边；测试端 2026-10-05 已改为 `false`，原文件备份为 `superbwarfare-client.toml.backup-20261005-203838-before-stamina-hud-off.bak`，整合包也应关）。

12. 战况条据点小牌（0.5.0-beta.2）：多人对局里两边轮流进出据点、争夺、顺序占点未解锁时的锁图标；据点名很长时小牌省略名字的样子；GUI 1 下 2× 小牌与第二行是否清晰；编制投票条显示期间站进据点（据点条在投票条下方）。

13. 平板外壳 D2 与阵营涂装 P3（0.5.0-beta.3）：三种涂装都要看——没加入阵营时看中立，`/battle admin test start academy` 看学院军，`/battle admin test start caesar` 看凯撒；窗口 960×720 GUI 1、320×240、640×336。看机身浮在压暗的世界上（世界每轮不同，截图不再逐字节相同）、玻璃深度与反光、2× 下斑点和暗角、LED 与焦点框在三种涂装下是否看得见；底框键：点击、Ctrl+Tab、←/→、Esc、R；按键七态加确认态危险键（凯撒的“悬停在已选上”与“确认态危险”只靠斜纹和色相区分）；反馈胶囊太长时的省略号与悬停全文、信号格；终端开着时核心 HUD 不在机身边上露出来、关掉后 HUD 颜色不变（附属 MOD 的 HUD 这一轮不隐藏，可能在边缘露一点）；中立浅色井里的输入框光标（0.5.0-beta.3 起用深色字的颜色，不再是原版浅灰）；切到地图或配装页签时回到旧 A 外框的过渡外观；帧率。宽松档（960×540 逻辑）的机身自动验收默认不截，需要时 `-PuiTiers=960x540` 只出报告。

## 8. 维护

- 新界面批次只新增 `cases/<界面>Cases.java`，在 `UiCaseCatalog` 加一行；迁移完成的界面把用例设为 `migrated(true)`，并让界面实现 `UiSurfaceInfo`、用 `UiLayoutProbe.tag` 给按键打 uiId、用 `UiLayoutProbe.begin/end` 包住各区域。
- 平板界面（`TacticalScreen`）的用例必须写明涂装：界面自己按夹具判定的用 `livery(...)`，要固定的用 `pinLivery(...)`（运行器在用例开始时固定、结束时释放，不要在用例里自己调 `pinForAcceptance`）。这样截图名带涂装后缀，并自动跑平板外壳检查。同一状态要看另一种涂装就再加一个用例。
- 夹具按阵营方参数化：`MockData.Side`（ACADEMY / CAESAR），`SquadFixtures.Scenario.withSide(...)`、`FormationFixtures.Scenario.joined(side, phase)`。
- HUD 部件要用 `TacticalHud.plate/readout` 或 `HudPaint.probeBox` 画，直接 `drawString` 的文字探针看不到。
- 管理员视角的用例用 `FormationCases` 里的 `asAdministrator()`，玩家视角用 `asPlayer()`；不要直接改服务端的 OP 状态。
- 纯验收工具和文档的改动不单独提升版本，跟随所属核心版本。
