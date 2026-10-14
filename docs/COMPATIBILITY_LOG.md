# GTNH 2.9.0-beta-3 × ReplayMod / ServerReplay 适配 — 交接文档

> 生成时间：2026-10-10
> 工作区：`<workspace>`
> 目标实例：`<instance>`

---

## ★ 30 秒速览（2026-10-11 深夜更新，**新会话先看这一节就够开工**）

> ⚠️ **本速览表停在 v84**（2026-10-11）。之后的 **v85 ~ v93**、以及"当前部署了什么 / 待测包是哪个"
> **一律以 [`PROJECT_HISTORY.md`](PROJECT_HISTORY.md) 的 §0 为准**；本文件正文已更新到 **§12.74 / v93**。
> 版本链与"改了原始 mod 的什么"另有一份总账：[`WHAT_CHANGED.md`](WHAT_CHANGED.md)
> （其中所有数字都来自留档 jar 的逐条目实测，与本文正文的旧计数冲突时以那份为准）。

> 📄 **想要一份自包含的交接？直接看 [`PROJECT_HISTORY.md`](PROJECT_HISTORY.md)** —— 读完就能接手，**不必先读本文件 4600 行**。
> 本文档继续承担"细节与证据"的角色：每一轮的成因、字节码证据、56 个坑、以及每节的"下一轮要什么"。

> **本会话（2026-10-11 下半场）一路推到 v74**，全部围绕**三位测试者的现场反馈**修。
> 交付链、每一版修什么、哪些已现场验证、哪些仍开着 —— 全在下表。

| 项 | 值 |
|---|---|
| **`mods\` 里实际部署的** | **v65**（`mods\replaymod-1.7.10-2.5.2-gtnh.jar`，`10837941` B / SHA256 `907B72B8…45FCC`）—— **本会话一行未动，随时可回滚** |
| **★ 最新待测包（累积版）** | **v84**（**12.59**：滚轮改用 `totalScrollAmount` 差分 —— 真因是 MouseTweaks 每帧抢读了消费式 `getDWheel()`，坑 67）：`replaymod-1.7.10-2.5.2-gtnh-v84.jar`（`10865116` B / SHA256 `5EB45D668837F342CB58AECF13384E4D3920A9D31C920A779583D63EB7ED13CA`）= **24 mixin + 5 util（31 class）**，在 `release\GTNH-ReplayMod-v84-待测\` + `.zip`（`9763881` B）。**要发就发这一版**（含 v75~v84 全部改动）；历史包 v66~v83 留档在 `release\` |
| **本会话交付链（每版修什么）** | **v66** 非 PBO 抓帧 ⇒ 导出白屏（338 ✅）→ **v67** AE2 判空 ⇒ 点界面不崩（现场 ✅）→ **v68/v69** popup 偷锁 / 输入屏清锁 ⇒ 循环消失（`RELEASED` 136→3）→ **v70~v83** 滚轮**四次失败**（12.43 / 12.49 / 12.51 / 12.55）→ **v71** Shift+ESC 复位 → **v72** 一致性检查（被抖动清零，坑 58）→ **v73** `SquashFilter` 逐包容错 ⇒ 保存不崩 → **v74** 坐标探针 ⇒ **排除**坐标量纲 → **v75** 反射改 `org.lwjglx.*`（坑 57）+ 让位 + ESC 急救 → **v76** ★ 抑制 `runTick → setIngameFocus()` 抢鼠标（12.50）→ **v77** 滚轮读取点 TAIL→HEAD（仍读 0）→ **v78** ★ 鼠标看护搬到每帧 `runTick`、输入屏自己挂回（12.52）→ **v79** 时间轴陈旧 offset 自愈（12.53）→ **v80** 实体普查 ⇒ 「人消失」定性为相机（12.54）→ **v81** 滚轮改 `getDWheel()`（被抢读）→ **v82** ★ 救回 ReplayMod 快捷键 / M 加标记（12.56）→ **v83** 时间轴探针改挂 `onClick` ⇒ **抓到 4 次真实点击**（12.58）→ **v84** 滚轮改 `totalScrollAmount` 差分（12.59） |
| **已现场确认修好** | 导出白屏（v66）· 点界面不崩（v67）· "人回来了" · 无手臂 / UI 正常 · 鼠标锁循环消失（v69）· entity tracker 逐包容错 + 夜视守卫（12.49，真环境 ✅）· ★ **「卡鼠标」已由测试者确认消失**（12.50 / 12.52；`no input screen` **189→15**）· ★ **「大时间轴」结案**（12.60：点击与换算**完全正常**，是上游重放机制）· ★ **「人消失」定性**（12.59：实体一直在，是相机视角）· 零注入失败、零崩溃标记 |
| **★ 仍未解决（各有判据，别凭猜）** | ① **滚轮调速** —— v70→v83 四次失败、**真因已定案**（MouseTweaks 抢读消费式 `getDWheel()`，坑 67），**v84 改用 `totalScrollAmount` 差分，待现场复验**（判据：`wheel now also … from raw dwheel=X`）；② **"回放/渲染只剩天空"** —— 待**两张 F3 截图**（播放头 `00:00` 与出问题处，12.39）；③ **保存录像崩溃** —— v73 已防崩，**根治待新栈**（12.47）；④ **服务端实体包漏录** —— 诊断就绪、**未执行**（12.31 / 12.33）；⑤ `meta` 的 `players`/`singleplayer`（低优先） |
| **测试者（已澄清）** | **两位外部测试者：Tester_A（338 mods）、Tester_B**；**洛洛本人 = 「Luoxueruoling」** ⇒ **每条外部反馈都要带"环境卡片"**（`release\VERIFICATION_GUIDE.md` 第 1 节） |
| **取证铁律** | 要日志一律要 **`logs/fml-client-latest.log`**（我们的输出走 `System.out`，`latest.log` 里没有）；画面问题要 **F3 截图**（看 `E:`）；视频给**原文件**（ffmpeg 抽帧 / `framemd5`） |
| **键位** | **Ctrl** = 锁定 / 放开鼠标可见；**Shift** = 退出旁观（上游）；**ESC（锁失效时）/ Shift+ESC** = 强制复位 |
| **客户端已知不修（已裁定）** | 相机贴人物时近处发黑（12.14）；原版血条仍显示（GTNH 不发 `HEALTH`，12.29）；JourneyMap 小地图 / InGameInfoXML 面板照旧；**鼠标可见时 WASD 不驱动相机**；**往回跳会"飞"一下、人暂时不见**（上游重放机制，12.59 / 12.60） |
| **服务端** | `ServerReplay-1.7.10\` 已按 **12.32 瘦身**为"纯 mixin 服务端录制器"（编译警告 3→0、离线自测通过）；**唯一核心缺口：实体包漏录**（12.31 字节级改写成因）；诊断与编排器 `tools\gtnh-replaymod-serverdiag.ps1` **就绪、未部署**（12.33） |
| **对外发布包 / 分支** | ✅ 主线 **v65**：`release\GTNH-ReplayMod-v65\` + `.zip`（`mods\` 里正在跑的就是它）；🌿 独立分支 **`branches\v82-hotkeys\`**（源码 + 产物 + `FINGERPRINTS.txt` + 重建命令，**已同步到 v84**，见 **12.57** 与 `branches\README.md`） |
| **通用复查方法** | **`release\VERIFICATION_GUIDE.md`**（不绑版本：环境卡片 / 13 项逐项判据 / 取证规范 / 判读表 / 回滚）—— 已复制进 v69 以后每个待测包 |
| **下一步（优先级）** | 1) **把 v84 发出去**（滚轮是**唯一**还没被现场验证的客户端项），回收 `wheel now also …` 那行；2) 三条已结案的可以转告测试者（卡鼠标已消失 / 大时间轴=上游重放 / 人消失=相机视角）；3) 拿**两张 F3**（"只剩天空"定性）；4) 服务端实机诊断（一句话就跑）；5) 让测试者试 **M 加标记**（v82）；6) `meta` 的 `players`/`singleplayer`（12.6-⑦） |

**开工前必读**：**先看本节（版本指针只认本节）与 [`PROJECT_HISTORY.md`](PROJECT_HISTORY.md)（v84 版，含两条新铁律）**，
再读 **勘误区**（含被推翻的旧条目）→ **第 4 节坑列表里的 29 / 47 / 57 / 58 / 59 / 63 / 64 / 65 / 66 / 67**（本轮新踩的都在这几个里）。
本轮（v66~v84）的成因与字节码证据集中在 **12.34 / 12.38 / 12.49~12.60**；
要做客户端构建 / 发布，见 **12.15**（构建命令）、**12.19**（干净重建 + 打包）与 `branches\README.md`（重建 + 部署 / 回滚）。

> **★ 版本指针只认本节**：正文里那些「当前交付 / 现役版本 / 已部署可用 / 应该装在 mods 里的版本」，
> 以及历史快照（v12 / v18 / v19 / v22 / v23 / v24 / v27 / v30-clean）**都只是当时那一轮的记录**
> （按次结案，故意保留以便追溯），**不代表现状**。
> **现状一律以本速览表为准**；同理，各节的「下一步」或「验证步骤」若与本节冲突，也以本节为准。

**本轮（2026-10-10 夜）结案的入口**：**12.14**（回放渲染）/ **12.15**（滚轮）/
**12.16~12.18**（保存录像面板，三层障碍）/ **12.19**（对外发布版：干净重建 + 打包）/
**12.20**（未完成录像恢复 —— **实测本来就可用，零代码改动**）/
**12.21**（**S25 空指针崩溃 —— 复现 + 修复，v32**）/
**12.22~12.23**（**回放界面鼠标/视角 —— 根因 + 修复，v42/v48**）。

**本轮（2026-10-11）结案的入口**：**12.23 补充三**（`Error loading entity tracker` 分析 —— 不是崩溃）/
**12.24**（**「导出/渲染视频」不可用 —— 根因 `NoSuchMethodError: GLStateManager.glReadPixels(long)` + 修复，v49**；
并落地了补充三的防御补丁）。

**环境铁律（别的都可以查，这几条记死）**：
- 测试脚本会 **kill 掉所有 java 进程**；**游戏运行时 `mods\` 里的 jar 被锁，无法覆盖** → 动手前先确认游戏已退出（`Get-Process java`）。
- 构建服务端 mod 必须 `<workspace>\tools\gradle-9.4.1` + `JAVA_HOME=<jdk25>`（**沙箱需 danger-full-access**）。
- **适配包（`replaymod-compat`）是纯 javac 工程，没有 reobf** → 源码里任何 **MCP 成员名**运行时都不存在，**一律反射**（坑 29）；编译**必须加 `-proc:none`**（坑 32）。
- mixin 里**不要用 `@Shadow`**；**不要给 mixin 配置加 priority**。
- `@Mixin(targets = "…")` 的类名必须与 **jar 内真实类名逐字符一致**（含大小写，坑 18/12.13）。
- 读游戏日志必须**共享读取 + 显式 UTF-8**（12.12 末尾有代码）。
- 判断 mixin 有没有注入，看**字节码导出**而不是日志（`-Dmixin.debug.export=true` + `javap`）。

---

## ⚠️ 勘误区（2026-10-10 深夜，由一名只读验证代理实测校出 —— **先看这里再看别的**）

本文档很长、是分阶段写出来的，下列条目**已被实测推翻或自相矛盾**，**以本区为准**：

| # | 位置 | 文档原说法的问题 | 正确说法 |
|---|---|---|---|
| 1 | **第 7 节部署命令** | 让你把**含 shade 的** `ServerReplay-1.7.10-0.1.0.jar` 拷进 `mods\` | **客户端/单人游戏必须装 `-client.jar`**（不含 shade），含 shade 的只给专用服务端；两个都装会踩坑 27（类遮蔽 → 回放画面错乱） |
| 2 | **12.9** | 说实验包"内置 `ReplayModTransformers` 作为 `FMLCorePlugin`" | 当时 manifest 里**根本没有这一行**（`inject-replaymod-transformers.ps1` 的续行判断 bug），所以那一轮 ③ 变量**没生效**；**13:49 已修复并严格正则校验通过** |
| 3 | **第 5 节** | 写"唯一已知缺陷是 EtFuturum NPE，尚待验证" | 该 NPE **已定位并修复**（`MixinEtFuturumPlayer`，见 2.7(3)）；第 5 节整体是较早的快照 |
| 4 | **6.1 第 1 条** | `[x] 已确认…（待做）` 自相矛盾 | **确实未删**：`MixinGuiFakeMain` 仍在源码里、也仍在 `mixins.serverreplay.json` 的 `client` 段 |
| 5 | **6.1 第 2 条** | 读起来像"写个独立 coremod 就行" | **已被 12.4-③ 推翻**：transformer 必须与 coremod **同 jar**，独立 jar 会 `NoClassDefFoundError` |
| 6 | **3.1 / 3.2 表格** | 3.1 说服务端"逻辑待补"；3.2 工具表不全 | 服务端**核心链路已可用**（P1/P2 完成）；`tools/` 实有 **12 个 `.ps1`**（完整清单见 3.2）；另有 `replaymod-compat/` 目录未列 |
| 7 | **坑 28 末句** | 说"没有现成脚本"、让人用 javap 手搓 | **`tools/analyze-mcpr.ps1` 是存在的**（解析 mcpr 的包序列与 custom payload 通道），第 7 节本应给它的用法 |
| 8 | **数字不一致** | 汉化"433/376/439/缺 63"；上游"1623/1625/1600 行"；丢包"4→3 / 6→5 / 7→5" | 汉化以「**zh_CN 439 条、缺失 0**」为准（433−376=57 与"缺 63"本身就不自洽）；上游行数约 1600（无关紧要）；丢包三个数字来自不同批次，**坑 19 已解释成因**（读侧有意过滤 KeepAlive/LoginSuccess），**不是回归** |
| 9 | **12.4 差异清单** | 自称"全部差异"（7 项） | 那是 **v1 时期**的对比；实验包还额外含 `com/gtnh/replaycompat/ReplayModTransformers.class` |
| 10 | **12.9 末段** | 把 `replays/server/` 写成相对路径 | 实际在**游戏实例根目录**：`…\GT New Horizons 2.9.0-beta-3\replays\server\`；`replay_recordings/` 同理也在实例里 |
| 11 | **build.gradle.kts:154** | 注释说"client mixin targets `GuiFakeMain`" | 真正生效的是 `MixinGuiCustom` → **`GuiCustom`**（12.2）；`MixinGuiFakeMain` 是待删死代码 |
| 20 | **12.6-⑥「方块渲染疑似上游 #294」** | 让它看起来像"上游不修、无解" | **已用字节级证据推翻**：同会话客户端/服务端录像的方块数据 **MD5 多重集完全相同**（12.14）。真实症状是「回放刚打开、相机贴人物时近处黑」，**相机离开即正常**；`#294` 的前因是**录制保存失败**，与我们不是同一个病 |
| 21 | **12.6-②「滚轮不流畅」的排查方向** | 建议"摘 `MixinGuiScreen` 做单变量区分" | **方向错误，已作废**。`MixinGuiScreen` 只做事件重放，与滚动幅度无关。真因是 **lwjgl3ify/SDL 的 `dWheel=±1`** 撞上 jGui 的 `/4`（按 LWJGL2 的 ±120 校准）→ 一格只动 1 像素（12.15） |
| 22 | **12.6-③「面板能显示但点不动」** | 旧判读把问题说成"输入路由"一个层面 | **不完整，实际有三层**：① 面板**根本没挂载**（CMM 的 `GuiFakeMain` 让宿主守卫 return）；② 同一 panel **重复挂载**；③ jGui 输入到不了面板。全部已修，见 12.16~12.18 |
| 23 | **12.6-④「恢复未完成录像（未动手）」** | 读起来像"客户端缺这个功能、要自己实现" | **客户端 1.7.10 本来就内置了完整机制**（`RestoreReplayGui` + `zh_CN` 文案 + `raw`/`.no_recover`/`.mcpr.tmp`，在 `ReplayMod.lambda$initClient$2` 里），实测**显示 / 点击 / 恢复执行三段全通、零代码改动**（**12.20**）。真正没实现的只有**服务端侧** `RecorderRecoverer`。另注：`replay_recordings\raw\` 是**故意保留 21 天的调试备份**，不是待恢复物 |
| 24 | **12.22 的「下一步建议 3」（去对照 jGui 高版本的 `AbstractGuiOverlay`）** | 让人以为高版本藏着一套"自动显示鼠标"的逻辑可以抄 | **源码 + 实测双重否定（2026-10-10 夜，12.23）**：高版本与 1.7.10 的 `GuiReplayOverlay$EventHandler`、以及 jGui 的 `updateUserInputGui()` **语义逐字相同**，**jGui 内部从不把 `mouseVisible` 置 true** —— 高版本"鼠标能用"同样只靠"按键 → `setMouseVisible(true)`"。而本环境**根本没有 GLFW**（lwjgl3ify 3.0.31 走 **SDL3**，libraries 里只有 `lwjgl-sdl`），高版本的 GLFW 回调直连**无从照搬**。正解见 **12.23** |
| 25 | **12.22 第 7 点「`KeyBindingEventCallback` 那条链在 GTNH 上根本不触发」的成因** | 早期判断把它归因于"lwjgl3ify 下 LWJGL2 输入路径整条死掉（`Keyboard.next()` 队列没有写入者）" | **只对了一半，别据此下结论**：`org.lwjglx.input.Keyboard.addRawKeyEvent` **是有调用者的**（`Lwjgl3ifyEventLoop.handleKeyEvent → addSdlKeyEvent → addRawKeyEvent → eventQueue(ArrayBlockingQueue 256)`），所以**事件队列活着**（这也解释了"玩家能正常移动"）。真正死的只是 `KeyBindingEventCallback` 那条**回调整链**；而 `KeyBinding.pressed` **确实是会被正常更新的**（12.23 实测 `pressed=true`）。**推论：任何"这个 API 在 lwjgl3ify 下一定死"的判断，都必须落到具体方法上实测，不能整片否定** |
| 26 | **12.22 第 7 点「`KeyBindingEventCallback` 那条链在 GTNH 上根本不触发」** | 早期把它归因成"lwjgl3ify 下 LWJGL2 输入整条死掉"，于是推断"上游做法在本环境不可行" | **要分两层看，两层的结论完全相反（2026-10-10 深夜实测，`tools\shots\f0.png` 等为证）**：<br>① **按键读取层完全正常** —— 按「播放/暂停」键 **P** 前后，相邻两帧（间隔 4 秒）平均像素差从 **`66.878` 掉到 `0.003`**（画面彻底冻结）。而 P 由 `KeyBinding.isPressed()` 触发，它返回 true 的前提是 `pressTime != 0`，而 `pressTime` **只由 `KeyBinding.onTick`(`func_74507_a`) 递增** ⇒ **`onTick` 确实在被调用、`Keyboard.next()` 事件队列是活的**（"队列死了"的说法是错的）。<br>② **但上游 overlay 那条具体路径实测不生效** —— 操作序列：按 **T**（弹出被别的 mod 抢走的书本 GUI）→ 关掉书本 → **回到 `currentScreen == null` 的回放画面** → 点击 overlay 的「暂停」按钮 ⇒ **依然无效**（像素差 0.17 → 0.1，都是静止）⇒ **`mouseVisible` 仍是 false，上游 `EventHandler` 没有把它设起来**。<br>⇒ **"链死了"对 overlay 路径成立、对按键读取层不成立**。`EventHandler` 失效的**确切环节尚未定位**（候选：`afterKeyBindingTick` 因 `onTick` 被 `beforeKeyBindingTick` 的 `ci.cancel()` 取消而跳过 / `EventHandler` 未注册 / `keyChat.isPressed()` 返回 false） |
| 27 | **12.23 补充三整节** | 把 338 mods 环境那份反馈（`latest.log` + `mclo.gs/J7AQ5aF`）**只读到了 entity tracker 那一段**，读起来像"该环境只报了这一个问题" | **同一份日志更靠后还有一次真崩溃被漏看**：第 17658~17668 行 `Description: Rendering video` + `NoSuchMethodError: 'void com.gtnewhorizons.angelica.glsm.GLStateManager.glReadPixels(int,int,int,int,int,int,long)'` —— 那才是"**导出功能不可用**"的根因（见 **12.24**）。**教训：拿到一份 1.8 万行的日志，不要只搜自己当时关心的那个关键词**（这次只搜了 `entity tracker`，而证据在 `Rendering video` / `ffmpeg` 附近） |

**两处「priority」口径不是同一件事**（最容易看晕的地方）：6.1 说的"priority 无效"指的是**注入点争抢**；
12.4-① 说的"`priority: 500` 是头号嫌疑"指的是 **mixin 配置的应用顺序**（数值高者先应用，默认 1000）。
两者结论一致：**不要给 mixin 配置加 priority**。

**实例里还有 17 份录像没有被文档逐条记录**（含 `server-sessionA-1110.mcpr` 和一份 289 B 的失败录像），
都在 `…\GT New Horizons 2.9.0-beta-3\replay_recordings\`。做"哪一轮装了什么、跑没跑"的判读时，
**以文件时间戳为准**（例如 json/tmp 的时间晚于 jar 的 mtime，就说明换包后确实启动过）。

**接手第一件事**：看 `mods/` 与 `mods-disabled/` 的真实内容，对照 12.9 与 12.5，
并**直接问洛洛上一轮「物品栏」实验的结果** —— 磁盘只能看出时序，看不出结果。

---

### ⚠️ 勘误区补充（2026-10-10 夜，本次接手会话实测新增 —— 这几条推翻了 12.5/12.9 的核心判读）

| # | 位置 | 文档原说法的问题 | 正确说法 |
|---|---|---|---|
| 12 | **12.5 / 12.9 整段** | 把「物品栏拖不动」归因于 ReplayMod 的 mixin（①priority / ②TweakClass / ③coremod / ④TweakOrder），并断言"③ 已按 12.4 处理" | **根因是 coremod 的 ASM transformer，与 `MixinGuiScreen`、priority、TweakClass、TweakOrder 全都无关**。真正的缺陷是"**transformer 在方法体内创建匿名类**"，详见下方实测矩阵 |
| 13 | **12.9 末段** | 写"③ 已用 `inject-replaymod-transformers.ps1` 修复并严格正则校验通过" | **那次只是"静态校验通过"，从未真正启动过**。13:49 之后第一、第二次带 `FMLCorePlugin` 的真实启动**都直接崩溃**（v4/v5），第三次（v6，两个 transformer 都 opt-in）才成功 |
| 14 | **12.9 的"下一个单变量是 ②TweakClass / ④TweakOrder"** | 建议做"保留 `ReplayModTweaker` 行为但不抢跑 mixin"的 tweaker | **该实验无意义（零差异）**，见第 13 节的双重论证。真正该做的是第 13 节列出的 transformer 修复 |
| 15 | **12.4-① priority 是"头号嫌疑"** | 把 `"priority": 500` 当成物品栏问题主因 | 已实测排除：v2 去掉 priority 后物品栏**仍然拖不动**；且 v6（无 priority、无 transformer）正常。priority 不是原因 |
| 16 | **12.4-③ "transformer 必须与 coremod 同 jar"** | 该结论本身没错，但**不充分** | 同 jar 只是必要条件。**即使同 jar，这两个 transformer 也注册不了**：`GLErrorTransformer` 与 `GLStateTrackerTransformer` 都在方法体内 `new` 匿名 visitor，导致 `LaunchClassLoader` 重入 → `ClassCircularityError` |
| 17 | **2.5 / 8 / 12.2 说"客户端入口走 `MixinGuiCustom`"** | 读起来像适配包自带的能力 | **那个 mixin 一直住在 ServerReplay 里**（`com.gtnh.serverreplay.mixin.MixinGuiCustom`）。**12.5 起 ServerReplay 被隔离到 `mods-disabled/`，主菜单入口就消失了** —— 这才是"主界面没有回放中心入口"的真实原因。现已搬进 ReplayMod 适配包（**12.12**） |
| 18 | **2.7(3) "Et Futurum NPE 已定位并修复（`MixinEtFuturumPlayer`）"** | 让人以为回放崩溃问题已解决 | **该修复从未真正生效**：`@Mixin(targets = "…storage.EtfuturumPlayer")` 与 jar 内真实类名 `…storage.**EtFuturumPlayer**`（**两个大写 F**）大小写不一致 → mixin 按字符串匹配失败 → 从未应用。而且它同样住在 ServerReplay 里。**崩溃报告打印的是小写拼写，所以这个错误看起来很合理**（**12.13** 结案） |
| 19 | **12.4-③ 之外，关于"适配包怎么编译"** | 正文没写清 | `replaymod-compat` 是**纯 javac 补丁工程，没有 `reobfJar`**：源码里任何 **MCP 成员名**在运行时都不存在（字段→`NoSuchFieldError`，方法→找不到）。**必须全走反射**，且**日志语句本身也要包在 `try` 内**（见 **坑 29**） |

**本轮（第二次接手会话）新增的三条硬教训（比上面的条目更容易再踩）**：

1. **类名字符串必须与 jar 内真实名逐字符一致（含大小写）** —— `@Mixin(targets=...)` 是字符串匹配。
   核对方式：解压 jar 看条目名 / `jar tf`。**不要相信崩溃报告或文档里的拼写**（#18 的教训）。
2. **这个补丁工程没有 reobf → 所有 MCP 成员名都要反射**（坑 29）。而且**异常是 `Error` 而非 `Exception`**，
   写在 `try` 外面（哪怕是拼日志参数）会**既逃逸异常又不打日志**，表现为"hook 根本没执行"，极具误导性。
3. **判断 mixin 有没有真的注入，要看字节码导出，不要看日志**：
   `-Dmixin.debug.export=true` → `<gameDir>\.mixin.out\class\...` → `javap -p -c`。
   我因为用错日志读取方式（ANSI 解码 UTF-8 + 文件独占冲突）连续几轮把"功能正常"误判成"没执行"。

**本轮物品栏问题实验矩阵（全部实测，严格单变量，ServerReplay 始终隔离在 `mods-disabled/`）**：

| 版本 | jar | 与前一版的唯一差异 | 物品栏拖动 | 启动结果 |
|---|---|---|---|---|
| v3 | 13:49 那版 | 基准（**manifest 里没有 `FMLCorePlugin`**，13:49 的修复尚未生效于 13:45 那次启动） | ❌ 拖不动 | ✅ 能进世界 |
| v4 | `…-v4-noguiscreen.jar` | 从 `mixins.recording.replaymod.json` 摘掉 `"MixinGuiScreen"` | — | ❌ **启动崩** `ClassCircularityError: GLErrorTransformer$1$1` |
| v5 | `…-v5-noglerr.jar` | 只注册 `GLStateTrackerTransformer` | — | ❌ **启动崩** `ClassCircularityError: GLStateTrackerTransformer$2$1` |
| **v6** | `…-v6-notransformer.jar` | **两个 transformer 都 opt-in（默认都不注册）** | ✅ **可以拖了** | ✅ 启动成功、进世界正常 |

**结论（洛洛 2026-10-10 实测确认）**：`replaymod-1.7.10-2.5.2-gtnh-v6-notransformer.jar`
（`10791220` B，SHA256 `18954E5D93ADA4A614B26AE1182BB1FA7C01E1332255B41AB1E6B8A1EA97F871`）
是**第一个「物品栏可拖动 + 能正常启动」的版本**。

> 注意一个反直觉之处：**v3 之所以没崩，恰恰是因为它的 `FMLCorePlugin` 行没写进去（coremod 是死代码）；
> 而 13:49 "修好" 之后，coremod 真的生效了，于是必然崩**。也就是说 13:49 那次修复把一个"沉默的缺陷"
> 变成了"必崩"，而两条路都不通——**必须在 coremod 里把两个 transformer 都关掉**。

---

## 0. 任务目标

读取并适配两个上游项目，使它们在 **GTNH 2.9.0-beta-3**（Minecraft 1.7.10）上可用：

- **客户端**：`https://github.com/ReplayMod/ReplayMod`
- **服务端**：`https://github.com/senseiwells/ServerReplay`

---

## 1. 环境事实（全部实测，勿凭记忆改动）

| 项 | 值 |
|---|---|
| 实例目录 | `<instance>` |
| Forge | `1.7.10-10.13.4.1614-1.7.10` |
| 引导 | RetroFuturaBootstrap（RFB，在 `lwjgl3ify-3.0.31-forgePatches.jar` 内） |
| Mixin | **unimixins-all 0.3.1** = UniMix 0.15.6 + **Mixin 0.8.7** |
| 渲染 | lwjgl3ify 3.0.31（LWJGL 3.4.2）+ **Angelica 2.2.10** + NotFine |
| 其他关键 | ArchaicFix 0.8.0、Hodgepodge、CoreTweaks、**Custom Main Menu 1.14.1**、JarJar |
| 游戏用 Java | **JDK 25.0.2**（`<jdk25>`） |
| mods 基线 | 258 个文件（快照见 `tools/mods-baseline.txt`） |
| 本机 JDK | 8（`<jdk8>`）、21、23、24、25；**JDK 17 已下载到** `<workspace>\tools\jdk17\jdk-17.0.20.1+1` |

**GTNH mod 的 manifest 统一约定**（所有 GTNH 系 mod 都这样，适配时必须照做）：

```
TweakClass: org.spongepowered.asm.launch.MixinTweaker
FMLCorePlugin: <自带 coremod>            ← 可选
FMLCorePluginContainsFMLMod: true
MixinConfigs: mixins.<mod>.json
ForceLoadAsMod: true
FMLAT: <mod>_at.cfg
```

---

## 2. 已完成并验证

### 2.1 客户端崩溃根因与修复（核心成果）

**根因**：ReplayMod 官方 jar 的 manifest 声明
`FMLCorePlugin: com.replaymod.core.LoadingPlugin`，而该 coremod **在构造函数里**调用
`MixinBootstrap.init()` + `Mixins.addConfiguration(...)`，**抢在 GTNHMixins 的
`LateMixinOrchestrationMixin`（`mixins.gtnhmixins.json`, `target=@env(PREINIT)`）编排之前**
注册 mixin。后果是 `ItemRenderer` 上 mixin 应用顺序异常 → **Angelica 抢先重写 `renderOverlays`**
→ Galacticraft 的必需注入
`@Inject(method="renderOverlays", at=@At(value="INVOKE", target="Lorg/lwjgl/opengl/GL11;glEnable(I)V", shift=BEFORE))`
扫描到 **0 个目标** → 类转换失败：

```
NoClassDefFoundError: net/minecraft/client/renderer/ItemRenderer
Caused by: InjectionError: Critical injection failure:
    Callback method galacticraft$onRenderOverlays ... failed injection check, (0/1) succeeded. Scanned 0 target(s).
```

**修复**：manifest 改为 GTNH 标准风格——用官方 `MixinTweaker` + `MixinConfigs` 声明，
**移除自有 `FMLCorePlugin`**（不再有 LoadingPlugin 抢跑）。

**实验矩阵**（为什么是这个方案）：

| 变体 | 改动 | 结果 |
|---|---|---|
| 官方原版 | — | ❌ 崩溃 |
| t1 | 移除 `MixinRenderItem` | ❌ |
| t2 | 移除 `FirstPersonRendererAccessor` | ❌ |
| t3 | 全部 `minVersion` → 0.8.7 | ❌ |
| t4 | GTNH 风格 manifest 但**保留** coremod | ❌ |
| **t5** | GTNH 风格 manifest + **移除 coremod** | ✅ **成功** |
| t6 | 只留 MixinConfigs（无 TweakClass） | ❌ |
| t10 | t5 + `priority:500`（实测 priority 对注入点争抢无效，无害） | ✅ **成功** |

**运行验证证据**（`tools/testlogs/replaymod-gtnh-t10.stdout.log` 等）：

```
Identified tweaker org.spongepowered.asm.launch.MixinTweaker from replaymod-...jar
Injecting Mixin Config for replaymod-...jar
Method overwrite conflict ... in mixins.render.replaymod.json:MixinCamera from mod replaymod   ← mixin 生效
[com.replaymod.extras.ReplayModExtras]: Failed to check for OpenEye availability               ← 代码在跑
[Netty Client IO #1] [FML]: Attempting connection ...                                          ← 进服务器成功
[CHAT] [Replay Mod] 开始录制                                                                    ← 录制功能激活
```

**结论：客户端已完全可用（含录制功能）**。

### 2.2 汉化

官方 `zh_CN.lang` 缺 63 个词条（`en_US` 433 / `zh_CN` 376）。已补齐 → **439 条，缺失 0**。
剩 5 条与英文相同的是 `2x/4x/8x`、`Alpha`、品牌名 `Replay Mod`，**不应翻译**。
补丁脚本：`tools/patch-lang-zh.ps1` + 词条表 `server-replay/lang/zh_CN.additions.lang`。

### 2.3 服务端可行性 PoC（已验证）

`server-replay/poc/McprPoc.java` 证明 **ReplayStudio 能为 1.7.10 写出并读回合法 mcpr**：

```
[poc] 1.7.10 protocol = 1.7.6-1.7.10 (id 5)
[poc] registry = 1.7.6-1.7.10(5) / PLAY
[poc] readback meta: mcVersion=1.7.10 rawProtocol=5 fileFormat=MCPR v=14
```

（唯一瑕疵：写 4 个包读回 3 个，第一个 `KeepAlive` 丢失，属 API 用法细节，待修。）

### 2.4 服务端 mod 工程（构建链已打通）

`<workspace>\ServerReplay-1.7.10`，使用 **RetroFuturaGradle**，构建成功产出 jar。
**GTNH 的 RFG 需要：Gradle 9.4.1（8.8 不行）+ JDK 25 运行 Gradle + Java 8 toolchain 编译 + Java 17 toolchain 供反编译。**

### 2.5 客户端主菜单入口：已修复并人眼确认（2026-10-10 晚）

**结论：主菜单右上角出现「回放中心」按钮，点击后回放中心正常打开，回放播放器可在 GTNH 世界里正常播放。**

原 `MixinGuiFakeMain` 之所以永远不生效，是因为**注入目标选错了类**：

- `lumien.custommainmenu.handler.CMMEventHandler#openGui` 收到 Forge 的 `GuiOpenEvent`（gui 是 `GuiMainMenu`）后，
  **把 `event.gui` 直接替换成 CMM 缓存的 `GuiCustom` 实例** → 玩家真正看到的屏幕是 `GuiCustom`。
- `GuiFakeMain` 只是 `initGuiPostEarly` 里通过**反射塞进 `InitGuiEvent.gui` 字段**的一个壳，从不经过
  `Minecraft#displayGuiScreen`，所以它那个空的 `initGui` 永远不会被调用。
- 更糟的是：ReplayMod 自己的 `injectIntoMainMenu` 在事件里看到的是这个 `GuiFakeMain`（它 extends `GuiMainMenu`），
  于是往 `event.buttonList` 加了按钮 —— 而 CMM 的 `initGuiPost` 会把事件里所有非
  `GuiCustomButton` 的按钮**移除丢弃**。所以官方行为下按钮必然消失。

**修复**（`MixinGuiCustom.java`，`@Mixin(targets = "lumien.custommainmenu.gui.GuiCustom")`）：
1. 注入 `func_73866_w_`（`initGui`）的 `RETURN` —— 此时 CMM 的事件处理已全部跑完，
   通过 **public 方法 `getButtonList()`**（返回 `GuiScreen.field_146292_n`）往真实按钮列表加一个普通 `GuiButton`；
2. 注入 `func_146284_a`（`actionPerformed`）的 `HEAD`，按 id 打开 `GuiReplayViewer`。

日志证据：
```
[serverreplay]: ReplayMod main menu button added at (530, 8) on a 640x374 screen (10 buttons total)
[serverreplay]: ReplayMod main menu button clicked, opening the replay viewer
```
`GuiCustom#drawScreen` 会遍历按钮列表对每个 `GuiButton` 调 `drawButton`（字节码偏移 833），所以原生按钮能正常渲染。

### 2.6 服务端录制：端到端已打通（2026-10-10 晚）

`MixinNetworkManager` 拦截 `NetworkManager.func_150725_a`（`scheduleOutboundPacket`），把 S→C 方向的包
序列化后交给 `ReplayRecorder`，**真正写出 .mcpr**。单人游戏的集成服务端即为验证环境。

实测日志（玩家进单人世界约 41 秒）：
```
[Server thread] server replay recording started: ...\replays\server\server-2026-10-10_11-10-21.mcpr
[Netty IO #1]   server replay recording finished: ... (2490 packets, 41646 ms, 903631 bytes)
```
> 上面那条日志来自**早期版本**，当时写在 `replays/server/` 下 —— 那是**错的**，回放中心看不到（坑 25）。
> 现在默认写 ReplayMod 自己的 `replay_recordings/`，文件名带 `server-` 前缀以便与客户端录像区分。
录像内容解析（`recording.tmcpr` = 2491 包，每个包 = `[int32 time][int32 length][varint id + payload]`）：
```
#0  t=0ms     id=0x02  LoginSuccess      ← LOGIN 阶段
#1  t=4ms     id=0x01  JoinGame          ← PLAY 第一个包
#2  t=22ms    id=0x3f  CustomPayload
#3..                           世界数据：0x23(BlockChange)×1288、0x22(MultiBlockChange)×476、0x26(MapChunkBulk)×145
```
**离线自测**（`server-replay/tests/ServerReplaySelfTest.java`，不依赖游戏）已通过：
写 7 包读回 5 包、meta 正确（`mcVersion=1.7.10`、`protocol=5`、`fileFormat=MCPR v=14`）。

### 2.7 回放可用性修复（2026-10-10 深夜，洛洛实测反馈驱动）

洛洛回放我们录的 mcpr 后给出三条反馈，逐条处理如下。

**(1) 回放里看不到人物 —— 已修**

原因：服务端**从不给玩家发他自己的 `S0CPacketSpawnPlayer`**（`EntityTrackerEntry.updatePlayerEntity`
会跳过实体本人的连接），所以服务端录像里没有可挂摄像机的玩家实体。
对比：ReplayMod **客户端**录像里有 1 个 `0x0c`，那是 Forge/集成服务端链路的产物。

修复（`MixinNetworkManager#injectSelfSpawn`）：拦截到 `S01PacketJoinGame` 时，
从 `NetHandlerPlayServer.playerEntity` 取玩家，构造 `new S0CPacketSpawnPlayer(player)` 序列化后
**紧跟在 JoinGame 之后**写进录像，并把 `player.getEntityId()` 通过
`ReplayRecorder.setSelfId` 写进 `metaData.selfId`。

**为什么必须紧跟 JoinGame**：这正是「客户端录像回放必崩、我们的不会崩」的分水岭 ——

| 录像 | LoginSuccess | SpawnPlayer | JoinGame |
|---|---|---|---|
| ReplayMod 客户端录制 | #0 | **#5（t=2.7s）** | **#20（t=26.3s）** |
| 我们的服务端录制 | #0 | （本次注入在 #20 附近） | **#19（t=1.2s）** |

客户端录像的 JoinGame 被 Forge 塞在 `FMLProxyPacket` 里，直到 mod 协商结束才出来（26 秒后），
而 SpawnPlayer 早在 2.7 秒就到了 → 回放时 `mc.theWorld` 仍为 null 就构造 `EntityOtherPlayerMP` →
Et Futurum 的 `EtFuturumPlayer.init` 抛 `NullPointerException`。我们的顺序天然正确。

**(2) 录制时物品栏交互发卡 —— 已修（改为异步写入）**

原因：原实现在 `scheduleOutboundPacket` 的 HEAD 里**当场**把包再序列化一遍并写 zip，
这段开销落在服务端 tick 上（单人游戏时更是落在客户端自己的线程上）。
修复：`ReplayRecorder` 改为 `LinkedBlockingQueue`（容量 20 万）+ 独立守护线程
`serverreplay-writer` 负责编码与写盘；游戏线程只做「打时间戳 + 入队」。
队列满时丢包并告警（不阻塞服务器）；`finish()` 等待队列排空后再收尾。

**(3) 回放里还显示着原版 HUD（血条/饥饿度/物品栏）—— 已定位并修复（与 (1) 同源）**

ReplayMod 1.7.10 隐藏这些 HUD 元素靠的是 `CameraEntity.EventHandler#preRenderGameOverlay`
（`RenderGameOverlayEvent.Pre` 里 `setCanceled(true)` 掉 ARMOR/HEALTH/FOOD/AIR/HOTBAR/EXPERIENCE/...），
而该 handler 是在 **CameraEntity 构造之后**才 `MinecraftForge.EVENT_BUS.register(eventHandler)` 的
（`CameraEntity.java:70-71`）。

于是链条闭合了：**Et Futurum 在 `Entity.<init>` 里注入的
`EtFuturumPlayer.init(Entity, World)` 会在 `world == null` 时 NPE**（crash 第 48 行读 `World.rand`），
回放时构造 `CameraEntity` 正好处于「实体已构造、世界尚未建立」的窗口 →
构造抛异常 → 后面的 HUD 处理器注册永远执行不到 → **既没有摄像机实体（看不到人物），原版 HUD 也没被隐藏**。

**修复**：`MixinEtFuturumPlayer`（client 段）注入 `init` 的 HEAD，
`world == null` 时 `ci.cancel()` 跳过这次初始化。Et Futurum 是普通 Forge mod（非 MC 本体），
所以必须 `remap = false`。
副作用为零：该方法只是把逐玩家状态塞进 `IExtendedEntityProperties`，没有 world 本来也无从初始化；
下一次玩家处于真实 world 时仍会正常执行。

**顺带的大收获**：这一条同时修好了「回放 ReplayMod 客户端自己录的录像必崩」
（那些录像的 `SpawnPlayer` 在 t=2.7s、`JoinGame` 晚到 t=26.3s，正是最恶劣的时序）。

> 编译提示：mixin 注解处理器要求目标类在**编译期 classpath** 上，
> 所以 `build.gradle.kts` 里要用 `fileTree(mods){ include("etfuturum-*.jar") }` 把 Et Futurum 加进 `compileOnly`，
> 否则报 `Mixin target ganymedes01.etfuturum.storage.EtfuturumPlayer could not be found`。

---

## 3. 关键文件清单

### 3.1 交付物

| 路径 | 说明 | 状态 |
|---|---|---|
| **`<workspace>\replaymod-1.7.10-2.5.2-gtnh-v65-handonly.jar`** | **★ 当前适配版客户端 jar**（`10837941` B / `907B72B8…45FCC`）；部署时改名为 `mods\replaymod-1.7.10-2.5.2-gtnh.jar`（**已部署**） | ✅ 全部实测/人眼确认 |
| **`<workspace>\replaymod-1.7.10-2.5.2-gtnh-v48-ctrltoggle.jar`** | **注入基座**（官方 + 全部基础改造，不含 12.24 之后的新 mixin）—— `inject-replaymod-clientmixins.ps1` 固定以它为 `-Jar` | ✅ 保留必需 |
| `<workspace>\replaymod-compat\` | **适配包工程**（纯 javac、无 reobf）：`src\…\mixin\`（mixin 源码，`*.java.disabled` 是已回退/未启用的）、`mixin-out\`（编译产物）、`mixin-res\mixins.replaycompat.json`、`deps\`、`libs\asm-9.9.jar` | ✅ |
| `<workspace>\release\GTNH-ReplayMod-v65\` + `.zip` | **对外发布整包**（jar + 安装说明 + SHA256，zip `9735848` B） | ✅ |
| `<workspace>\replaymod-1.7.10-2.5.2-gtnh-v49…v64*.jar` | 12.24~12.30 的中间版本（v50/v51/v52/v53/v56/v57…v64），**仅供追溯**；其中 v60 有"UI 全消失"缺陷、v57/v58/v63/v64 的 UI 补丁已回退 | 🗄️ 历史 |
| `<workspace>\.selftest\GLCallAudit.java` | **★ 可复用工具**：扫描 jar 内全部 `org/lwjgl/opengl/*` 调用，和 Angelica `GLStateManager` 的方法表比对，**只看"同名不同签名"** ⇒ 一眼找出被 `GLSMRedirector` 改写后必然 `NoSuchMethodError` 的调用点（12.24 靠它一步定位导出崩溃） | ✅ 复用它 |
| `<workspace>\.selftest\DU1IrQc.log`、`J7AQ5aF.log` | 338 环境那两份反馈日志的本地副本（12.24 / 12.25 / 12.27 的证据来源） | ✅ 取证用 |
| `<mc-home>\…\replay_recordings\AAA-s25-first-test.mcpr` | **S25 回归夹具**（开头第 2 个包就是 S25） | ✅ |
| `<mc-home>\…\replay_recordings\AAA-spawndefer-test.mcpr` | **SpawnPlayer 延迟重放夹具**（服务端录像副本，加载期必有合成 SpawnPlayer） | ✅ |
| `<mc-home>\…\replay_recordings\AAA-tracker-badpacket-test.mcpr` | entity tracker 的坏包夹具（⚠️ **没触发**，`0x15` 不在"含实体 ID"判定表里，留着备用，见 12.26 末） | ⚠️ 未触发 |
| `<workspace>\release\GTNH-ReplayMod-v30\` … `v53\` | 更早的对外发布包 | 🗄️ 历史 |
| ⚠️ `<workspace>\replaymod-1.7.10-2.5.2-gtnh-LEGACY-v3.jar` | **旧包 v3**（= `mods-disabled\…-v3.jar`，**物品栏拖不动**那版）**不要拿它测试**（此陷阱由接手验收抓出，见 12.18 末尾） | ❌ 已废弃 |
| `<workspace>\GTNH-ReplayMod-适配报告.md` | 完整适配报告（根因/实验/遗留/服务端设计） | ✅ |
| `<workspace>\tools\make-gtnh-replaymod.ps1` | 一键从官方 jar 生成适配版（`-BypassCustomMainMenuCheck -LocalizeZhCN`），已实测可复现 | ✅ |
| `<workspace>\ServerReplay-1.7.10\` | 服务端录制 mod 工程（RFG） | 🚧 构建链通，逻辑待补 |
| `<workspace>\server-replay\poc\McprPoc.java` | ReplayStudio 1.7.10 mcpr 往返 PoC | ✅ |
| `<workspace>\server-replay\lang\zh_CN.additions.lang` | 63 条补翻译词条表 | ✅ |

### 3.2 工具脚本（`<workspace>\tools\`，**实有 12 个 `.ps1`**）

| 脚本 | 用途 |
|---|---|
| `launch-gtnh.ps1` | **绕过 HMCL 直接启动 GTNH**（classpath 去重等关键修复；`-ExtraJvmArgs` 透传 `-D` 开关；**不会 kill java**） |
| `run-test.ps1` | 自动化「启动→判定崩溃→保存日志」，`-Jar <jar>` 或 `-NoReplayMod`（基线）；**会先 kill 所有 java** |
| `run-batch.ps1` | 批量跑多个 jar |
| `gtnh-window.ps1` | 窗口定位 / 截图 / **鼠标点击拖动** / 按键（**已修 DPI**，见坑 30；`-Find` 带 `exit 0`） |
| `gtnh-printwindow.ps1` | `PrintWindow` 抓窗口内容（不受遮挡影响） |
| `make-gtnh-replaymod.ps1` | 客户端适配**基础包**生成器（manifest / 汉化 / CMM 绕过） |
| `inject-replaymod-transformers.ps1` | 把 coremod transformer 注入基础包 |
| `inject-replaymod-clientmixins.ps1` | 把适配包自有客户端 mixin + 配置注入 jar，并改写 manifest `MixinConfigs`（幂等） |
| `analyze-mcpr.ps1` | 解析 mcpr 的包直方图与 custom payload 通道（`-CustomPayload`） |
| `make-replaymod-asm-compat.ps1` | 早期独立 coremod jar 生成（**已证不可用**，见 12.4-③） |
| `patch-lang-zh.ps1` | 汉化合并 |
| `patch-replaymod.ps1` | 早期实验用的 mixin config 编辑（可弃用） |
| `testlogs/`、`shots/` | 各变体启动日志与崩溃报告 / 自动化截图 |

### 3.3 工作区其他目录

- `ReplayMod-stable/` — stable 分支源码（tarball 解压，56MB）
- `ReplayMod-1.7.10/` — `1.7.10-2.0.1-b1` 老标签源码（221 java 文件，**无 preprocessor 指令**）
- `ServerReplay/` — 上游 ServerReplay 克隆（仅 1623 行 Kotlin，核心在外部 arcade 库）
- `server-replay/` — PoC 与语言文件
- `ServerReplay-1.7.10/` — 自研 1.7.10 服务端 mod（RFG 工程）
- `tools/` — Gradle 8.8/9.4.1、JDK 17、各实验 jar

---

## 4. 已踩的 56 个坑（务必先读，避免重复劳动）

1. **沙箱 schannel 取不到凭据** → `curl`/`git`(schannel) 全报 `SEC_E_NO_CREDENTIALS`。
   解法：`git -c http.sslBackend=openssl`。当前会话已是 danger-full-access，此问题不复现。
2. **网络阻断**：`plugins.gradle.org`、`services.gradle.org`、`downloads.gradle.org`、`raw.githubusercontent.com`、**GitHub Releases 大文件** 均不通或有 SNI 阻断。
   可用替代：`nexus.gtnewhorizons.com`、`repo.spongepowered.org`、`repo1.maven.org`、`jitpack.io`、`api.github.com`、**腾讯云 Gradle 镜像**、**清华 Adoptium 镜像**。
3. **`launch-gtnh.ps1` 的两个关键修复**（不加必崩）：
   - MC 本体 jar **必须放在 classpath 最前**（否则 FML sanity checker NPE）
   - classpath 必须**按 group:artifact 去重取最高版本**（否则 `guava-15.0` 遮蔽 `17.0` → SkinPort `NoSuchMethodError: CharSource.readLines(LineProcessor)`）
4. **HMCL 导出的 bat 不可直接用**（认证 token 过期 + 会静默失败）；用 `launch-gtnh.ps1`。
5. **ReplayMod 的 mixin 配置必须走 manifest `MixinConfigs`**，不能靠自有 coremod 提前 `addConfiguration`。
6. **`mixins.nonmmlauncher.replaymod.json` 必须排除**：其伴生类 `ReplayModNonMMLauncher` 不在发布 jar 里，会 `ClassNotFoundException`。
7. **Custom Main Menu 的 `GuiFakeMain.func_73866_w_()` 是空方法**（`0: return`，不调 super）→ ReplayMod 挂在父类 `GuiScreen.initGui` 的回调**永不触发**。改字符串绕过无效，必须**直接注入 `GuiFakeMain` 自身**。
8. **GTNH 用 JarJar 的 `LoaderV2`**：`@Mod` 类会在 FML 的 mod transition 阶段被解析，若其（间接）引用 MC 网络类 → `NoClassDefFoundError: NetworkManager`。**服务端 mod 应做成纯 mixin mod（无 @Mod 类）**。
9. **RFG 工具链要求**：RFG 2.0.x 的 jar 是 **Java 25（class 69）** 编译的 → 必须 **Gradle 9.4.1 + JDK 25 运行 Gradle**；同时需要 **Java 8 toolchain**（编译）与 **Java 17 toolchain**（fernflower 反编译）。Gradle 8.8 直接报 `Unsupported class file major version 69`。
10. **foojay 自动下载 JDK 会失败**（走 GitHub Releases）→ 手动从清华镜像取 JDK 17 并在 `gradle.properties` 用 `org.gradle.java.installations.paths` 登记。
11. **Mixin 对 Forge patch 成员要 `remap = false`**：`NetworkManager.scheduleOutboundPacket`、字段 `isClientSide` 都不在原版 MCP 映射表里（无 SRG 名）。
12. **refmap 必须显式配置 AP 参数**，否则 jar 里没有 refmap：
    `-AoutRefMapFile=<build>/tmp/mixins/mixins.serverreplay.refmap.json -AdefaultObfuscationEnv=searge`，且用 `tasks.jar { from(mixinRefmap) }` 打进 jar。
13. **1.7.10 的 `NetworkManager` 侧别字段是 `isClientSide`**（服务端 = `!isClientSide`），**没有** `isServerSide`（那是 1.8+）。
14. **ReplayMod 上游状态**：官方自 **2.6.0** 起放弃 1.7.10（`root.gradle.kts:108-110` 有明确注释），`1.7.10-2.5.2` 是最后一版；stable 分支的 1.7.10 源码**不完整**（`ReplayModTweaker` 只在 `versions/1.12.2/`）。

    > ⚠️ **更正（实测）**：上面这句**不准确**。ReplayModTweaker.java 在 stable 树的根 `src/main/java/com/replaymod/core/tweaker/` 里**也有**；`versions/1.7.10/` 放的是 1.7.10 专属的 **14 个覆盖文件**（其中 `GLErrorTransformer`、`GLStateTrackerTransformer`、`MixinGuiScreen` 正是本项目重写/摘除的那几个）。官方是**特意保留** 1.7.10 源码的 —— `root.gradle.kts` 原话：*"We dropped 1.7.10 with the Gradle 7 update but still kept its source in case someone…"*。因此"源码不可得"不能作为不 fork 的理由；真实理由是 1.7.10 的**构建链**（Gradle 2.11 + `ForgeGradle:1.2-SNAPSHOT`）已失效，而本项目改动全是 Mixin。作者 johni0702 明确表示**不会**处理 1.7.10 的 mod 间兼容（issue #294）。
15. **ServerReplay 上游状态**：仅支持 1.17.1~26.3（18 个独立分支），**无 1.7.10**；本体仅 1625 行 Kotlin，**真正的录制逻辑在外部 `arcade` 库**。属于「参照设计重新实现」而非移植。
16. **refmap 为空时，mixin 必须写 SRG 名**（最容易再踩的坑）：
    当 AP 无法解析某个成员的 MCP→SRG 映射时，它会写一个**空 refmap**，而 mixin 运行时**按字面名查找**。此时源码里写 MCP 名会直接失败：
    ```
    InvalidMixinException: @Shadow field isClientSide was not located in the target class
    net.minecraft.network.NetworkManager
    ```
    正确做法是**直接写运行时的 SRG 名**。1.7.10 `NetworkManager` 的对应关系（已从 `build/rfg/srg_merged_minecraft.jar` 实测确认）：

    | MCP 名 | SRG 名（运行时，写这个） |
    |---|---|
    | `isClientSide` | `field_150747_h` |
    | `scheduleOutboundPacket(Packet, GenericFutureListener...)` | `func_150725_a` |
    | `dispatchPacket` | `func_150732_b` |

    查任意成员的 SRG 名：`javap -p -classpath <workspace>\ServerReplay-1.7.10\build\rfg\srg_merged_minecraft.jar <类全名>`
    查 MCP 名对应的源码：`build\rfg\mcp_patched_minecraft-sources.jar` 内。

17. **CMM 真正显示的屏幕是 `GuiCustom`，不是 `GuiFakeMain`**（2026-10-10 实测确认，这是 `MixinGuiFakeMain` 永不触发的原因）：
    `CMMEventHandler#openGui` 把 `GuiOpenEvent.gui` 直接换成 `Config.getGUI("mainmenu")` 返回的 `GuiCustom` 实例；
    `GuiFakeMain` 只在 `initGuiPostEarly` 里被反射塞进 `InitGuiEvent.gui` 字段当壳，从不 `displayGuiScreen`，
    所以它的空 `initGui` 永远不会被调用。ReplayMod 官方逻辑因此把按钮加进了那个事件列表，
    而 `initGuiPost` 会把事件里所有非 `GuiCustomButton` 的按钮删掉 —— 按钮必然消失。
    **正确注入点是 `lumien.custommainmenu.gui.GuiCustom#func_73866_w_` 的 RETURN**（此时 CMM 事件已处理完），
    并用它的 public 方法 `getButtonList()` 拿到 `GuiScreen.field_146292_n`（这样就不需要 `@Shadow` 字段）。
    详见 2.5。

18. **编译期写 MCP 名，mixin 注解里写 SRG 名**（最容易搞混的一处）：
    `compileJava` 用的是 MCP 映射的 MC jar，所以方法体里必须写 `packet.writePacketData(buf)`、`self.getNetHandler()`；
    `reobfJar` 会把它们变成运行时的 `func_148840_b` / `func_150729_e`（已用 `javap` 验证）。
    而 `@Inject(method = "...")` 里的**字符串常量不会被 reobf 处理**，加上空 refmap + `remap = false`，
    那里必须直接写 SRG 名。注意 `EnumConnectionState` 的不少方法**本来就没有 MCP 名**
    （`func_150752_a` / `func_150754_b` 等），源码里就是 SRG 名，照写即可。

19. **mcpr 的读取侧会主动丢包，因此录像必须包含 LOGIN 阶段**（2.3 里"4→3 丢包"之谜的答案）：
    `ReplayInputStream.fillBuffer` 的行为是：
    - **永远丢弃 `KeepAlive`**；
    - 以 PLAY registry 打开文件时（`fileFormatVersion >= 14` 会把它内部换成 LOGIN registry），
      **`LoginSuccess` 本身不会返回给调用方**。
    所以「写 6 读 5」不是丢数据 —— 解析 `recording.tmcpr` 可确认 6 个包全在里面（每个包 17 字节 = `4(time)+4(length)+1(varint id)+8(payload)`）。
    反过来说：**只录 PLAY 阶段的 mcpr 读出来会短**，录制必须从 LOGIN 阶段开始（`ReplayRecorder.STATE_LOGIN`）。
    既然 KeepAlive 必被丢弃，录制时直接跳过它可以省空间。

20. **专用服务端没有 ReplayMod，必须把 ReplayStudio shade 进自己的 jar**：
    从官方 jar 里只取 5 个前缀即可（`build.gradle.kts` 的 `replayStudioPrefixes`）：
    `com/replaymod/replaystudio/**`、`lib/com/github/**`（steveice10 Netty fork）、`lib/it/**`（fastutil）、
    `lib/net/md_5/**`、`lib/org/apache/**`，约 7.4 MB raw / 3.5 MB 压缩。
    **绝不能** shading `org/spongepowered/**`（jar 里内嵌了 mixin 本体，会与 unimixins 冲突）。
    离线验证方式见 2.6 的 `ServerReplaySelfTest`（classpath 只给 shade jar + gson + guava + log4j-api）。
    运行时会刷一批 `Error during mapping loading of Protocol1_16_2To1_16_1 ... NPE`（ViaVersion 缺 mapping 资源），
    **无害**，1.7.10 不需要跨版本转换。

21. **`ZipReplayFile` 的暂存物是一个目录，且不会自动删**：
    录制期间生成 `<name>.mcpr.tmp/`，另有一个 `<name>.mcpr.cache/` 目录（内含 `recording.tmcpr.crc32`）。
    `saveTo()`/`close()` 都不清理 `.cache` 目录 → `ReplayRecorder.deleteCacheFile()` 显式递归删除。
    注意：**用 `ZipReplayFile` 打开已有 mcpr 读回时也会重建该目录**，所以"有 `.cache` 残留"不一定是录制侧的问题。

22. **Forge 的 `FMLProxyPacket` 必须转成 vanilla 包才能录**，否则 GTNH 回放会失败：
    它的类不在 vanilla 包表里（`BiMap.inverse().get()` 返回 null，原实现直接跳过），
    于是 `FML|HS` 的 ModList / RegistryData / HandshakeAck 等**全部没进录像**，
    回放时 FML 报 `Unexpected packet during modded negotiation - assuming vanilla or keepalives : S0CPacketSpawnPlayer`，
    实体生成时序错乱（Et Futurum 会在 `EntityOtherPlayerMP` 构造时因 `worldObj == null` NPE）。
    ReplayMod 官方做法是 `((FMLProxyPacket) packet).toS3FPacket()`（见 1.7.10 源码
    `recording/packet/PacketListener#channelRead`）。该方法只读 `payload().array()`，**不会**破坏正在发送的包。

23. **侧别判断不要用 `@Shadow` 字段**：改用 public 方法
    `NetworkManager#getNetHandler()`（MCP）→ 运行时 `func_150729_e`，
    判断 `instanceof INetHandlerPlayServer`。专用服务端/集成服务端天然只有服务端连接会命中，
    客户端自己那条连接是 `INetHandlerPlayClient`，自动排除。

24. **Gradle 在 `workspace-write` 沙箱下起不来**：报
    `Could not initialize native services / Failed to load native library 'native-platform.dll'`。
    本机实测与 TEMP 位置无关，把文件策略切到 **danger-full-access** 后立刻恢复正常（同一命令、同一会话）。
    另注意 `run-test.ps1` 需要先 `Set-ExecutionPolicy -Scope Process Bypass`（脚本自身的设置生效太晚），
    且 `pwsh` 不在 PATH 里时要直接用 `& <workspace>\tools\run-test.ps1`。

25. **ReplayMod 的录像目录是 `replay_recordings/`，而且回放中心不递归子目录**（很容易白测一轮）：
    `ReplayMod#getReplayFolder()` 解析 `Setting.RECORDING_PATH`，其默认值是 **`./replay_recordings/`**
    （不是 `replays/`）；`GuiReplayViewer` 用 `folder.listFiles(...mcpr)` 列录像，**不会**进子目录。
    所以把服务端录像写在 `replays/server/` 下，玩家在回放中心里**根本看不到**。
    现已改为默认写到 `replay_recordings/`（文件名带 `server-` 前缀以便区分），可用
    `-Dserverreplay.outputDir=<dir>` 覆盖。

26. **服务端录制与客户端录制的内容差异（2026-10-10 实测对比，同一次进世界）**：    客户端录像 6085 包 / 2.08 MB，服务端录像（旧版）2491 包 / 1.19 MB。差异主要在：

    | 包 id | 含义 | 客户端录像 | 旧版服务端录像 |
    |---|---|---|---|
    | `0x3f` | CustomPayload（FML 握手 + 各 mod 网络通信） | **2266** | **1** |
    | `0x12` | EntityVelocity | 578 | 54 |
    | `0x17` | EntityLookMove | 528 | 10 |
    | `0x19` | EntityHeadLook | 54 | 0 |
    | `0x25` | BlockBreakAnim | 203 | 0 |
    | `0x04` / `0x0c` / `0x00` | EntityEquipment / SpawnPlayer / KeepAlive | 5 / 1 / 18 | 0 / 0 / 0（主动跳过 KeepAlive） |
    | `0x23` / `0x22` / `0x26` | 世界数据 | 1288 / 476 / 145 | 1288 / 476 / 145 ✅ 完全一致 |

    世界数据完全一致，说明拦截点与序列化是对的；缺的全是 `FMLProxyPacket` 包裹的包（坑 22 修的就是这个）。
    **另一个重要结论**：那次 `EtFuturumPlayer.init` NPE 是回放 **ReplayMod 客户端自己录的**
    `replay_recordings/2026_10_10_11_10_20.mcpr` 时发生的（当时我们的录像根本不在列表里），
    所以它属于 **GTNH + Et Futurum 的回放兼容问题**，与本 mod 无关。

27. **客户端绝对不能加载本 mod 里 shade 的那份 ReplayStudio（会让回放画面坏掉）**：
    mods 目录里的 jar 会被 Forge 以**不受控的位置**注入 classpath（日志里的
    `FML has found a non-mod file ServerReplay-...jar ... injected into your classpath`），
    于是本 mod shade 的 `com/replaymod/replaystudio/**` 可能**遮蔽 ReplayMod 自带的那一份**。
    而 shade 只挑了 5 个前缀（坑 20），缺的正是 ViaVersion 的 mapping 资源之类 —— 离线自测里
    `Error during mapping loading of Protocol1_16_2To1_16_1 ... NullPointerException` 就是征兆，
    在游戏里则表现为**回放能加载、但方块/注册表映射错乱、画面缺方块错位**。
    **解决方案：产出两个 jar**（`build.gradle.kts` 的 `clientJar` 任务）：
    - `ServerReplay-1.7.10-0.1.0-client.jar`（约 15 KB，**不含** shade）→ **客户端/单人游戏装这个**
      （ReplayMod 提供 ReplayStudio）；
    - `ServerReplay-1.7.10-0.1.0.jar`（约 3.5 MB，**含** shade）→ **专用服务端装这个**（那边没有 ReplayMod）。
    两个都装会重现遮蔽问题。

28. **任何从 `:jar` 复制产物的 Gradle 任务都必须依赖 `:reobfJar` 的*输出*，而不是 `:jar`**：
    `jar` 与 `reobfJar` 写的是**同一个** `build/libs/<name>.jar`，所以
    `from(zipTree(tasks.jar.get().archiveFile))` 可能读到 reobf 之前的内容 —— 客户端于是拿到 MCP 成员名，
    表现为 `NoSuchFieldError: Class net.minecraft.client.gui.GuiButton does not have member field 'int id'`
    （`id` 在运行时叫 `field_146127_k`）。正确写法：
    ```kotlin
    val reobfJarTask = tasks.named("reobfJar")
    val clientJar by tasks.registering(Jar::class) {
        dependsOn(reobfJarTask)
        from({ reobfJarTask.get().outputs.files.map { zipTree(it) } }) { exclude("com/replaymod/**") }
        // ... manifest 同主 jar
    }
    ```
    验证产物是否真的 reobf 过：在 jar 的 class 字节里找 `field_146127_k` / `func_150729_e` 这类 SRG 名
    （`tools/analyze-mcpr.ps1` 附近没有现成脚本，用 `javap -c -p -classpath <jar> <类>` 或按字节搜字符串）。

29. **没有 `reobfJar` 的纯 javac 补丁工程：任何 MCP 成员名都会在运行时炸（同一个病，我连犯两次）**：
    `replaymod-compat` 是**用 javac 直接编译的补丁工程**（没有 Gradle、没有 `reobfJar`），
    所以源码里写 MCP 成员名，字节码里留下的就是 MCP 名，而运行时是 SRG 名：

    | 源码写法 | 运行时错误 | 正确的 SRG 名 |
    |---|---|---|
    | `button.id` | `NoSuchFieldError: ... GuiButton does not have member field 'int id'` | `field_146127_k` |
    | `screen.width` | `NoSuchFieldError: ... GuiScreen does not have member field 'int width'` | `field_146294_l` |
    | `screen.height` | 同上 | `field_146295_m` |

    （`button.id` 那条就是旧坑 28 记录的现象；`screen.width/height` 是 2026-10-10 夜踩到的同一类新变体。）

    **解法（二选一）**：① 所有成员访问走**反射**——本工程采用这种做法，见
    `com.gtnh.replaycompat.mixin.MixinGuiCustomReplayEntry#getIntField`，它按 MCP 名与 SRG 名两种拼写逐个尝试；
    ② 给补丁工程补一个 reobf 步骤（目前没有）。

    **构造函数/方法描述符是安全的**：`new GuiButton(IIIII,Ljava/lang/String;)V` 这类描述符在 MCP 与 SRG 下**完全相同**，
    所以只有**字段名/方法名**需要处理。

    **★ 第二个陷阱（真正坑掉一轮的地方）**：`NoSuchFieldError` 是 `Error` 而**不是** `Exception`，
    会从只 catch `Exception` 的代码里逃出去；更糟的是，如果把 MCP 访问写在 `try` **外面**
    （例如用来拼一条日志的参数时），那么异常既不会被捕获，**那条日志也永远不会打出来** ——
    现象是「日志一条都没有，看起来 hook 根本没执行」，外加探针文件为空，极具误导性。
    **结论：所有 MCP 成员访问都必须包在 `try` 内，日志语句本身也不例外。**

30. **`gtnh-window.ps1` 截图必须 DPI-aware，否则截到的是窗口标题栏**：
    `pwsh` 进程默认 DPI-unaware，`GetClientRect` 返回**虚拟化尺寸**（实测 `569x320`，真值 `854x480`），
    `CopyFromScreen` 于是偏移到标题栏上。**解法**：P/Invoke `SetProcessDPIAware()`，并在**任何窗口操作之前**调用；
    `tools\gtnh-window.ps1` 已按此修复。附带：该脚本 `-Find` 分支带 `exit 0`，
    用 `&`（同进程）调用会把**调用者的脚本一起结束** —— 要客户区尺寸就用 `-Shot`（它本来就会打印 `client=WxH`）。

31. **★「日志打了」不等于「操作成功」（本轮被自己的日志连骗两次）**：
    - 诊断 `println` 写在关键调用**之后**且**不看返回值** → 调用静默失败也照打"成功"
      （本轮的输入桥接就这样 no-op 了一整轮，日志却显示 "forwarded …"）；
    - 改成看返回值后又忘了 **`void` 方法反射调用恒返回 `null`** → 明明是成功，却报成 `NOT_FOUND`。
    **结论：诊断日志必须打印「可判定的证据」**（真实返回值 / 副作用状态 / 异常），
    **最好的办法是去被调用方打探针**——本轮最终正是靠 `AbstractGuiClickable.mouseClick` 的返回值一眼定案。

32. **适配包 mixin 的编译必须加 `-proc:none`**：classpath 上只要有 mixin 的注解处理器，
    `javac` 就会自动运行它，而它需要 gson → `NoClassDefFoundError: com/google/gson/JsonParseException`，
    编译以 **exit 3** 失败。我们不需要 AP（refmap 用的是 ReplayMod 自带的那个）。

33. **适配包 mixin 若引用 MC/Forge 类型，编译 classpath 要用 `recompiled_minecraft-1.7.10.jar`**：
    它含 **378 个 `net/minecraftforge` 条目**（即 Forge 类，如 `GuiScreenEvent`），
    而 **`srg_merged_minecraft.jar` 不含 Forge**，别拿错；另需附 `replaymod-1.7.10-2.5.2.jar`（提供 `MCVer`）。

34. **GTNH 主菜单（CMM `GuiCustom`）上 jGui 收不到鼠标事件**：
    `VanillaGuiScreen.EventHandler.onMouseInput` 是 `@SubscribeEvent(priority = EventPriority.LOWEST)`，
    而 `EventRegistrations.register()`（真正执行 `MinecraftForge.EVENT_BUS.register(this)` 的地方）
    **只在 `wrap()` 新建 wrapper 时**被调用，命中缓存不重注册；`EventHandler.onGuiClosed` 又对
    **每一个** `GuiOpenEvent` **无条件** `unregister()` → 缓存复用的 wrapper 长期处于"已注销"状态，
    LOWEST 优先级又最先被 `receiveCanceled=false` 跳过。
    **解法**：见 12.17 的 `MixinGuiScreenJGuiInputBridge`（在 `GuiScreen.handleMouseInput` 的 HEAD
    直接把事件转给 jGui 的 `MinecraftGuiScreen`，绕开事件系统）。

35. **jGui 点击语义（改 jGui 相关代码前必读）**：
    - 按钮 `onClick()` **在 `mouseClick` 里就触发**，**不依赖 `mouseRelease`**
      （`mouseRelease` 分发的是 `Draggable` 接口，而按钮只实现 `Clickable`——这是设计，不是 bug，别再往这查）；
    - 命中判定 = `container.convertFor(this, point)` 换算成元件内坐标后，
      `0 < x < getLastSize().width && 0 < y < getLastSize().height`；
    - `AbstractComposedGuiElement.forEach(Class)` 返回的是**动态代理**，会把调用**广播到所有层的匹配元件**，
      所以"`forEach` 只取一个元件"的直觉是**错的**。

36. **`RestoreReplayGui` 的 log4j 输出不落进 `logs\` 下任何文件**（2026-10-10 夜实测）：
    `Found partially saved replay`（构造器）与 `Attempting recovery`（「是」按钮）**在 `dsh-stdout.log`
    和 `fml-client-latest.log` 里都搜不到**。→ **判断恢复流程只能靠行为证据**：
    `metaData.json` 的 `duration`/`generator`、`<name>.mcpr.tmp\` 是否被清理、`.replay_cache\` 是否新建。

37. **Windows 会吞掉「点击非前台窗口」的第一下**（那一下用于激活窗口）：
    `gtnh-window.ps1 -Click` 第一次点通常无效，要连点两次；
    **但连点的第二下会落在界面已经变化之后的位置** —— 本轮第二下就误触了主菜单的链接按钮，
    弹出「你确定要打开以下网页？」。
    **正确姿势：点一次 → 用 `gtnh-printwindow.ps1` 确认界面变化 → 再决定补不补点。**

38. **★ `<name>.mcpr.tmp\` 的真实结构（别望文生义）**：
    ```
    <name>.mcpr.tmp\
    ├── source      ← ★ 是「文件」而不是目录！内容是原始 mcpr 的 canonical path（UTF-8）
    ├── changed\    ← 目录
    └── removed\    ← 目录
    ```
    `ZipReplayFile.saveInputFile()`（`ZipReplayFile.java:163`）是 `new FileOutputStream(sourceFile)`
    直接写它。**照 stable 分支 `ReplayFilesService` 里的 `changed/removed/source` 建三个目录 →
    `FileOutputStream` 打在目录上 → `FileNotFoundException: …\source (拒绝访问。)` → 恢复失败并弹 Crash Report 界面。**

39. **读 `fml-client-latest.log` 时 `FileShare.ReadWrite` 仍可能被 log4j 拒绝**：
    报 `The process cannot access the file because another process has locked a portion of the file`。
    → 共享读取要**带重试**。另外 **`dsh-stdout.log` 会被 log4j 轮转**（本轮实测长度 2902243 → 2898321），
    **"长度变小"不能当成"没有新增"**。

40. **`gtnh-window.ps1 -Shot` 抓的是屏幕区域，不是"游戏窗口"**：
    洛洛一旦把 DSH 切到前台，`-Shot` 抓到的就是**聊天窗口**（本轮实际发生，我差点据此误判）。
    → **要游戏画面一律用 `gtnh-printwindow.ps1`**（`PrintWindow`，不受遮挡影响）；
    `-Shot` 只适合"确认游戏确实在前台"。

41. **坐标会随窗口/DPI 变化，旧坐标不可复用**：12.14 记录的是客户区 `1706x996`，
    本轮实测是 **`854x480`** → 12.14 那组"已实测可用坐标"**全部作废**，
    用之前必须按当前窗口重新量。（本轮实测有效的恢复界面「是」按钮客户区坐标：**(274, 299)**。）

42. **★★ Mixin 的包保护是按「包」生效的，不是按 `client` 段列表**：
    把工具类放进 mixin 配置声明的 `package`（`com.gtnh.replaycompat.mixin`）下，
    **即使不登记进 `client` 段**，只要被 mixin 类引用就会炸：
    ```
    IllegalClassLoadError: com.gtnh.replaycompat.mixin.NullWorldGuard is in a defined mixin package
      com.gtnh.replaycompat.mixin.* owned by mixins.replaycompat.json and cannot be referenced directly
    ```
    （v31 就是这样废掉的：一加载就 `NoClassDefFoundError`。）
    **解法**：helper 放到包外（本工程 = **`com.gtnh.replaycompat.util`**），
    **并同步改 `tools/inject-replaymod-clientmixins.ps1`**（它原先只拷 `mixin` 包，现在拷 `mixin` + `util`）。

43. **★★ `clientWorldController`(`field_147300_g`) 与 `Minecraft.theWorld`(`field_71441_e`) 是两个不同步的字段**：
    前者由 `S01PacketJoinGame` 在加载期就建好，后者要等**加载结束**才赋值。
    所以「回放加载期 world 还没建」取决于包用的是哪个字段 —— **查错字段的守卫对另一条路径完全无效**
    （12.13 的 SpawnGuard 查 `field_147300_g`，因此挡不住 12.21 的 S25）。
    用 `javap -c` 扫全类得出：`NetHandlerPlayClient` 里**直接引用 `theWorld` 的 handler 恰好 15 个**。

44. **mcpr 的整数是 int32 大端**：用 PowerShell 的 `[BitConverter]::ToInt32`（**小端**）解析 `recording.tmcpr`
    只会读出 1 个包，看起来像"整段录像只有一个包"（本轮就这样被骗过一次）。
    → 手写大端读取，或直接用 `tools\analyze-mcpr.ps1`。

45. **自造 mcpr 测试夹具时，`recording.tmcpr.crc32` 必须一起重算**：
    它是 **ASCII 十进制文本**，算法 = **标准 CRC32**，范围 = **`recording.tmcpr` 的全部字节**
    （已用真实文件验证：原值 `1065191571` 与重算完全吻合）。只换 `recording.tmcpr` 不更新它，回放可能拒绝加载。

46. **ReplayMod 在「启动扫描阶段」就会预加载录像，不需要玩家点「加载」**：
    栈是 `Minecraft.func_71411_J` → `InputReplayTimer.func_74275_a` → `GuiOpeningReplay.func_146269_k`
    → `NetworkManager.func_74428_b`。所以一段**开头就有 S25** 的录像会让游戏**启动即崩**
    （12.21 实测：什么都没点，启动 2 分 24 秒后崩在主菜单出现之前）。
    → 反过来也是好事：**造一段这样的录像就是一个可靠的回归夹具**（见 12.21）。
47. **★ 编译 `replaymod-compat` 的 mixin 时，classpath 必须包含 `mixin-out` 自己**：
    一旦有 mixin 引用 `com.gtnh.replaycompat.util.*` 里的 helper，只给 `deps` + MC + replaymod 的
    classpath 会报"找不到符号 / 程序包不存在"。本工程实测可用的完整 classpath：
    `"$compat\mixin-out;$compat\deps;<recompiled_minecraft>;<replaymod>;<gson>;$compat\libs\asm-*.jar"`。
    **另外（本轮血的教训）**：`javac` 在 PowerShell 里是原生命令，失败**不会**中断脚本 ——
    我在 v33 / v37 / v38 上连犯三次「编译失败却照旧跑注入脚本、把上一次的旧 class 当新版本部署出去」，
    浪费了整整一轮实测。**务必 `if ($LASTEXITCODE -ne 0) { throw }`，非 0 就中止，绝不"顺手"继续注入。**
48. **★★ `Minecraft.displayGuiScreen()` 会调用 `KeyBinding.unPressAllKeys()`，把「用来触发 GUI 的那个按键状态」自己清掉**（12.23 结案）：
    完整链条 = `setMouseVisible(true)` → `displayGuiScreen(UserInputGuiScreen)` → `setIngameNotInFocus()` → `KeyBinding.unPressAllKeys()`
    → 所有 binding 的 `pressed=false` / `pressTime=0`；下一帧再读就是 false → `displayGuiScreen(null)` → `setIngameFocus()`
    → `inGameHasFocus` 又变回 true → 下次 `notInFocus` 再清一次…… **每 tick 一轮，无限循环**。
    这个现象**极具迷惑性：一个键都没按，日志却在 RELEASED/GRABBED 之间成对翻转**（v39 实测 21 秒 400 次）。
    **解法**：切换后**立刻把状态写回去**（`KeyBinding.setKeyBindState(getKeyCode(), true)`）。
    **通用推论**：任何"根据 `KeyBinding` 状态决定要不要显示 GUI"的代码都会自毁，必须做状态回写。
    另注：**`isPressed()`(`func_151468_f`) 是消费式的**（内部 `--pressTime`），会与 `unPressAllKeys` 相互作用，
    **不要拿它做轮询**；轮询请用 `getIsKeyPressed()`(`func_151470_d` → `pressed` 字段)。
    （另：`KeyBinding` 的 `field_74512_d` 是 **keyCode**、`field_74513_e` 是 **pressed**、`field_151474_i` 才是 **pressTime** —— 探针别读错。）
49. **★★ GTNH 这套环境根本没有 GLFW（lwjgl3ify 3.0.31 用的是 SDL3）—— 别照抄高版本 ReplayMod 的输入方案**（12.23）：
    `Class.forName("org.lwjgl.glfw.GLFW")` 抛 `ClassNotFoundException`，**不是类加载器隔离，而是这一版压根没装 `lwjgl-glfw`**
    （版本 json 的 libraries 里只有 `lwjgl-sdl`，crash report 里 "glfw" 出现 0 次）。
    高版本 ReplayMod 靠 `@Mixin(net.minecraft.client.Keyboard)` 挂 GLFW 回调直连，**在 1.7.10 + lwjgl3ify 上无从照搬**。
    本环境可用的键盘路径（按实测可靠性排序）：
    ① **`KeyBinding.pressed`** —— 实测可用（配坑 48 的状态回写即可，v42 就走这条）；
    ② `me.eigenraven.lwjgl3ify.api.InputEvents.addKeyboardListener` —— SDL 事件直连，签名见 **12.23**，未采用但确认可用；
    ③ `org.lwjgl.input.Keyboard.isKeyDown(int)` —— **方法在、`42→SDL 225` 映射表也对，但此环境下按住时恒 false**，实测排除。
    ⚠️ 同源教训：`org.lwjglx.input.Keyboard.addRawKeyEvent` **是有调用者的**（`handleKeyEvent → addSdlKeyEvent → addRawKeyEvent → eventQueue`），
    事件队列**是活的**（"队列是死的"属早期误判，它本可解释"玩家能正常移动"）。
    **凡是"某 API 在 lwjgl3ify 下一定死"的判断，都必须逐方法实测，不能整片否定。**
50. **★★ `GLSMRedirector` 会把 `org.lwjgl.opengl.*` 调用点无条件改写成 Angelica `GLStateManager` 的同名方法，
    而 Angelica 并未实现全部重载 —— 缺失的那个只会在"真跑到那一行"时炸 `NoSuchMethodError`**（**12.24** 实测）：
    `GLStateManager.glReadPixels` 只有 `ByteBuffer`/`FloatBuffer`/`IntBuffer` 三版，**没有 `(IIIIIIJ)V`（long）**，
    而 ReplayMod 的 `PboOpenGlFrameCapturer` 正是用 long 版 → **一按"渲染"就崩**（`Description: Rendering video`）。
    **排查工具（已留在工作区）**：`.selftest\GLCallAudit.java`（ASM）—— 拿被适配 jar 的**全部** `org/lwjgl/opengl/*` 调用点，
    与 `angelica-*.jar` 里 `com/gtnewhorizons/angelica/glsm/GLStateManager` 的方法表比对，**只看"同名不同签名"那一类**：
    "同名同签名"=安全；"名字都不存在"多为未被重定向的扩展调用（`GL43C`/`KHRDebug`/`ARB*`），基本是假阳性。
    本轮全量扫描 **5381 个类** → **真断链只有 1 处**（就是它）。
    **修法**：① 优先改走**上游自带的降级分支**（此处 = 让 `PixelBufferObject.SUPPORTED` 读成 false，改用非 PBO 的 `ByteBuffer` 重载）；
    ② 兜底用**反射**直连真 LWJGL（普通字节码调用会被同一个 redirector 再改写一次，反射不会）。
51. **★★ Mixin 的 `@At` 里 `value` 是「注入点类型」，不是 opcode —— 字段访问必须写
    `@At(value = "FIELD", opcode = Opcodes.GETSTATIC, target = "L…;字段名:Z")`**（**12.24** 实测）。
    我写成了 `@At(value = "GETSTATIC", target = …)` → Mixin 在**应用该类时**抛
    `InvalidInjectionException: GETSTATIC is not a valid injection point specifier`，
    **整条 mixin 被跳过**；因为 config 是 `required: false`，它**只留一条 WARN、不崩、也不生效**。
    （`PUTSTATIC` / `GETFIELD` / `PUTFIELD` 同理，都是 opcode 而非 value。）
    **教训**：怀疑"某个 mixin 没生效"时，**先去日志里搜 `Mixin apply for mod … failed` /
    `InvalidInjectionException`** —— 这类**解析期**错误不会让游戏崩，极易被"游戏能跑"掩盖。
    本轮正是靠这条 WARN 才发现主方案（禁用 PBO）其实没上，真正救场的是兜底的反射方案。

---

## 5. 当前状态（2026-10-10 晚）

> ⚠️ **本节是当天早一阶段的状态快照，后面又推进了很多。最新状态请看文首「★ 30 秒速览」**
> （尤其是 12.5 的物品栏实验矩阵和 12.9 的交接时实验状态）。本节的 5.2 里
> "唯一的已知缺陷"等描述已被第 12 节取代，保留它只是为了记录当时的判断过程。

### 5.1 客户端：已完成 ✅

- **`replaymod-1.7.10-2.5.2-gtnh.jar` 已部署可用**（**这是当时那一版，非现状**；最新交付见文首速览）：崩溃已修复、录制功能已激活、汉化 439 条 0 缺失。
- **主菜单入口已由人眼确认**：右上角「回放中心」按钮 → 回放中心 → 播放器在 GTNH 世界里正常播放（见 2.5）。
- **未做**：与 ReplayMod 内部相关的可选优化（第 6.1 节）。

### 5.2 服务端录制 mod：核心链路已打通 ✅，回放兼容性待收尾 🚧

**已实测成立的部分**：

- 构建链：RFG 2.0.6 + **Gradle 9.4.1 + JDK 25 运行** + JDK 8 编译 + JDK 17 反编译；`reobfJar` 正常。
- 加载方式：纯 mixin mod（无 `@Mod` 类），manifest 走 `MixinTweaker` + `MixinConfigs`。
  Forge 会提示 `FML has found a non-mod file ServerReplay-...jar`（因为无 `@Mod`），
  但 mixin config 照常注入，功能不受影响。
- 拦截点：`NetworkManager.func_150725_a`（`scheduleOutboundPacket`），HEAD 注入；
  侧别判断改用 `getNetHandler() instanceof INetHandlerPlayServer`（见坑 23）。
- 写出 mcpr：`ReplayRecorder` 逐包写 `ZipReplayFile.writePacketData()`，断开连接时收尾
  （`close()` → `writeMetaData` → `saveTo` → 删 `.cache`）。
  **实测录出 2490 包 / 41.6 秒 / 903 KB 的合法 mcpr**，meta 为 `1.7.10` / protocol 5 / MCPR v14。
- 离线自测 `ServerReplaySelfTest` 通过（不依赖游戏，可在 CI/无游戏环境下回归）。

**当前唯一的已知缺陷**：把录好的 mcpr 放进回放中心播放时，客户端抛
`EtFuturumPlayer.init` 的 `NullPointerException`（`world == null`），伴随 FML 警告
`Unexpected packet during modded negotiation - S0CPacketSpawnPlayer`。
**已修复的成因**：录像缺 Forge 的 `FMLProxyPacket`（FML 握手/mod 网络包），
已按 ReplayMod 官方做法补上 `toS3FPacket()` 转换（坑 22），**新版本尚待一次端到端回放验证**。
若补上 FML 包后仍然 NPE，则需怀疑 Et Futurum 自身在 ReplayMod 回放环境下的兼容性
（对照实验：用 ReplayMod 客户端录制功能录一段 GTNH 录像并回放，看是否同样崩）。

### 5.3 服务端 recorder 的历史遗留说明（保留备查）

- 第一版用 `@Mod` 主类 → `NoClassDefFoundError: NetworkManager`（GTNH JarJar LoaderV2 在 mod transition 阶段解析 @Mod 类）。**已改为纯 mixin mod**。
- 第二版用 MCP 名（`isClientSide`）→ `InvalidMixinException: @Shadow field isClientSide was not located`（空 refmap 下按字面名查找）。
- 第三版改用 SRG 名 + `@Shadow` 字段 → **JVM 在 FML 切换日志配置那一刻静默退出**。
- 隔离验证确认问题在 `MixinNetworkManager`；最终**彻底放弃 `@Shadow`**（改用 public 方法与 SRG 写法的 `@Inject`）后一切正常。
- 因此**不要**在任何 mixin 里使用 `@Shadow`。

### 5.4 服务端 mixin 的备选思路（若以后需要换拦截点）

- 改拦截 **`dispatchPacket`（`func_150732_b`，private）** 而不是 `scheduleOutboundPacket`。
- 改用 **Forge 事件**（`FMLNetworkEvent.ServerCustomPacketEvent` 只能拿到自定义包，不够用）。
- 用 **MixinBooter 的 `IEarlyMixinLoader`** 风格声明（GTNH 原生方式），而非 manifest `MixinConfigs`。
- 录制触发方式（当前是**自动录制所有玩家连接**，可用 `-Dserverreplay.auto=false` 关闭）。

---

## 6. 下一步待办

### 6.1 客户端
- [x] 验证 CMM 主菜单按钮 —— **已人眼确认**（`MixinGuiCustom`，见 2.5）
- [x] 清理：`MixinGuiFakeMain` 已确认永不触发（它只打一行诊断日志），可以从 `mixins.serverreplay.json` 的 `client` 段和源码里删掉（**待做**）
- [x] **拆分客户端版 / 专用服务端版两个 jar**（`clientJar` 任务）—— 客户端绝不能带 shade，见坑 27
- [ ] 可选：补回 `LoadingPlugin` 的 ASM transformer（`GLStateTrackerTransformer`、`GLErrorTransformer`）——需写一个**只注册 transformer、不调用 `MixinBootstrap.init()`** 的 GTNH 风格 coremod
- [ ] 可选：消除 WARN 级注入点抢占（ArchaicFix `MixinMinecraft@Redirect::displayWorkingScreen`、Angelica `MixinEffectRenderer` 粒子剔除、`MixinChunkProviderServer`）——已实测 `priority` 无效，需重新编译相关 mixin

### 6.2 服务端

- [x] 构建链打通（RFG 2.0.6 + **Gradle 9.4.1 + JDK 25 运行** + JDK 8 编译 + JDK 17 反编译）
- [x] mod 能被 Forge 正常加载（**纯 mixin，无 @Mod 类**）
- [x] 拦截点已确定：`NetworkManager.func_150725_a`（MCP: `scheduleOutboundPacket(Packet, GenericFutureListener...)`）
- [x] 崩溃原因定位：**`@Shadow` 在空 refmap 下不可用**
- [x] **侧别判断**：`getNetHandler() instanceof INetHandlerPlayServer`（不用 `@Shadow`，见坑 23）
- [x] **`ReplayRecorder` 真正写 mcpr**：LOGIN + PLAY 全录、跳过 KeepAlive、断开时收尾并清理 `.cache`
- [x] ReplayStudio shade 进 jar（见坑 20），离线自测 `ServerReplaySelfTest` 通过
- [x] 端到端实测（单人游戏集成服务端）：**录出 2490 包 / 41.6 秒 / 903 KB 的合法 mcpr**
- [x] 录制触发方式：**自动录制所有玩家连接**（`-Dserverreplay.auto=false` 关闭）；后续可加命令/玩家过滤
- [x] 澄清「`KeepAlive` 4→3 往返丢失」：**不是 bug**，是 `ReplayInputStream` 有意过滤（坑 19）
- [ ] **回放验证收尾**：确认补上 `FMLProxyPacket` 之后，回放我们的 mcpr 是否还会在 `EtFuturumPlayer.init` NPE
      （若仍崩 → 做对照实验：用 ReplayMod 客户端录制一段 GTNH 录像并回放，判断是 Et Futurum 的回放兼容问题还是我们录像缺包）
- [ ] 专有服务器实测（1.7.10 + GTNH 服务端），多人连接、断线收尾、长时录制（文件体积/性能）
- [ ] 可选：把 ReplayStudio 重定位（relocate）到 `com.gtnh.serverreplay.shaded.*`，彻底避免与 ReplayMod 的类重复
- [ ] 可选：改进 meta（`selfId`、`players`、`serverName`）以便回放界面显示更完整

---

## 7. 常用命令速查

```powershell
# ---- 客户端：启动游戏验证（自动判定崩溃并保存日志）----
# 注意：run-test.ps1 会先 kill 掉所有 java 进程；-KeepRunning 保留游戏进程供人眼确认。
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force   # 必须先设，脚本内的设置生效太晚
& <workspace>\tools\run-test.ps1 -Jar <workspace>\replaymod-1.7.10-2.5.2-gtnh-v30-release.jar -WaitSec 200 -KeepRunning
# ⚠️ 注意：run-test.ps1 会先删掉 mods\replaymod*.jar，再把你传的 -Jar 拷进去（文件名带版本号）！
#    所以跑完测试后 mods 里**就不是正式部署了**，必须按下面这条重新部署。

# ---- 客户端：正式部署（**唯一正确的一条**；回滚也用同一条，换个源文件即可）----
# ⚠️ 游戏运行时 mods\ 里的 jar 被锁 → 部署前先确认游戏已退出（Get-Process java）
Copy-Item <workspace>\replaymod-1.7.10-2.5.2-gtnh-v65-handonly.jar `
          '<instance>\mods\replaymod-1.7.10-2.5.2-gtnh.jar' -Force
& <workspace>\tools\run-test.ps1 -NoReplayMod        # 基线（不带 ReplayMod）

# 从官方 jar 重新生成客户端适配版（含汉化 + CMM 绕过）
pwsh -File <workspace>\tools\make-gtnh-replaymod.ps1 `
     -Source <workspace>\tools\replaymod-1.7.10-2.5.2.jar `
     -Dest   <workspace>\replaymod-1.7.10-2.5.2-gtnh-base.jar `
     -BypassCustomMainMenuCheck -LocalizeZhCN

# ---- 服务端 mod：构建（必须 Gradle 9.4.1 + JDK 25 运行；文件策略需 danger-full-access）----
$env:JAVA_HOME='<jdk25>'
& <workspace>\tools\gradle-9.4.1\bin\gradle.bat --no-daemon build --project-dir <workspace>\ServerReplay-1.7.10
# 产物有两个，别装错（勘误区 #1 / 坑 27）：
#   ServerReplay-1.7.10-0.1.0-client.jar  ← 客户端 / 单人游戏装这个（不含 shade，ReplayMod 提供 ReplayStudio）
#   ServerReplay-1.7.10-0.1.0.jar         ← 专用服务端装这个（含 shade 的 ReplayStudio）
# 两个都装会重现坑 27 的类遮蔽（回放画面错乱）。不要装 -dev.jar。
Copy-Item <workspace>\ServerReplay-1.7.10\build\libs\ServerReplay-1.7.10-0.1.0-client.jar `
          '<instance>\mods\' -Force

# ---- 服务端 mod：离线自测（不需要游戏，验证 shade 依赖闭包 + mcpr 写入）----
$jar='<workspace>\ServerReplay-1.7.10\build\libs\ServerReplay-1.7.10-0.1.0.jar'
$cp="$jar;<libs>\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar;" +
    "<libs>\com\google\guava\guava\17.0\guava-17.0.jar;" +
    "<libs>\org\apache\logging\log4j\log4j-api\2.0-beta9-fixed\log4j-api-2.0-beta9-fixed.jar"
& '<jdk8>\bin\javac.exe' -encoding UTF-8 -nowarn -cp $cp `
     -d <workspace>\.selftest\out <workspace>\server-replay\tests\ServerReplaySelfTest.java
Set-Location <workspace>\.selftest
& '<jdk8>\bin\java.exe' -cp "$cp;<workspace>\.selftest\out" ServerReplaySelfTest

# ---- PoC：验证 ReplayStudio 能写 1.7.10 mcpr ----
$cp="<workspace>\tools\replaymod-1.7.10-2.5.2.jar;<libs>\com\google\guava\guava\17.0\guava-17.0.jar;<libs>\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar"
& '<jdk25>\bin\java.exe' -cp "$cp;<workspace>\server-replay\poc\out" McprPoc <workspace>\server-replay\poc\poc.mcpr

# ---- 查 SRG 名 / MCP 源码（适配必备）----
javap -p -classpath <workspace>\ServerReplay-1.7.10\build\rfg\srg_merged_minecraft.jar net.minecraft.network.NetworkManager
# MCP 源码在 build\rfg\mcp_patched_minecraft-sources.jar 里（可直接读 zip entry）
```

### 运行期可用的开关

| 开关 | 作用 |
|---|---|
| `-Dserverreplay.auto=false` | 服务端 mod 只加载不录制（默认自动录所有连接） |
| 录像输出目录 | ReplayMod 的 `replay_recordings/`（可用 `-Dserverreplay.outputDir=<dir>` 覆盖） |

---

## 8. 重要提醒

- **测试脚本会 kill 掉所有 java 进程**。动手前先确认洛洛不在玩游戏（`Get-Process java`）。
- 服务端录制 mod 走 mixin（`MixinNetworkManager`，拦截 `scheduleOutboundPacket`），
  **不要**加回 `@Mod` 主类；侧别判断用 `getNetHandler() instanceof INetHandlerPlayServer`，
  **不要**用 `@Shadow` 字段（空 refmap 下会静默硬失败）。
- 客户端入口现在住在**适配包**里：`com.gtnh.replaycompat.mixin.MixinGuiCustomReplayEntry`
  （注入 CMM 真正显示的 `GuiCustom`）。**注意它用了 `initGui` + `drawScreen` 双 hook** ——
  只靠 `initGui` 的 RETURN 注入不可靠（实测零输出），原因见 **12.12**。
  **不要**再回头注入 `GuiFakeMain`（它永远不会被 display）；
  **也不要**把它放回 ServerReplay —— 那样一隔离 ServerReplay，主菜单入口就会消失（勘误区 #17）。
- **所有"为打开回放服务"的客户端兼容修复都应留在适配包里**（目前有 3 个：
  入口、`MixinEtFuturumPlayer`、`MixinNetHandlerPlayClientSpawnGuard`，见 12.12/12.13）。
  判断标准：**「打开回放」是纯客户端行为，不该依赖服务端录制 mod。**
- **`@Mixin(targets = "...")` 的类名必须与 jar 内真实类名逐字符一致（含大小写）**；
  `replaymod-compat` 没有 reobf，所以源码里**不要出现 MCP 成员名**——一律反射（坑 29）。
- 服务端 jar 里 **shade 了 ReplayStudio**；改动 `build.gradle.kts` 的 `replayStudioPrefixes` 时
  **绝不能**把 `org/spongepowered/**` 卷进来（会与 unimixins 打架）。
- **部署哪个 jar 别搞错**：客户端/单人游戏装 `ServerReplay-1.7.10-0.1.0-client.jar`（不含 shade），
  专用服务端装 `ServerReplay-1.7.10-0.1.0.jar`（含 shade）。两个都装会重现坑 27 的遮蔽问题。
- 新增 Gradle 任务时注意 **reobf 顺序**：任何从 `:jar` 复制产物出来的任务都必须同时 `dependsOn("reobfJar")`，
  否则会拿到 MCP 名（表现为 `NoSuchFieldError: ... GuiButton does not have member field 'int id'`）。
- 调整 mixin 里的成员名时记住：**方法体写 MCP 名（reobf 会转），注解字符串写 SRG 名**（坑 18）。
- 构建服务端 mod 需要 **danger-full-access**（Gradle native 库，坑 24）。

---

## 9. 新对话开场白（可直接复制粘贴）

> 🔴 **本节内容停留在 v74（已过时）**。**最新的开场白在 [`PROJECT_HISTORY.md`](PROJECT_HISTORY.md)**（v84 版，含两条新铁律与当前唯一未验证项）——
> 新会话请复制那一份。版本指针仍以文首「★ 30 秒速览」为准。

> 继续推进 GTNH 2.9.0-beta-3 × ReplayMod / ServerReplay 的适配，工作区 `<workspace>`。
>
> **请先完整读一遍 `<workspace>\COMPATIBILITY_LOG.md`**，重点这几处（能省掉整轮整轮的试错）：
> **文首「★ 30 秒速览」（一屏就能开工）** →
> **「⚠️ 勘误区」（含 #1~#27，**以该区为准**，它推翻了正文若干旧描述）** →
> **第 4 节：已踩的 56 个坑**（尤其 **29 / 31 / 38 / 42 / 47 / 48 / 50 / 51 / 53 / 55 / 56**）→
> **第 12 节 12.20~12.30**（2026-10-10 那一轮：未完成录像 / S25 / 回放鼠标 / 导出渲染 / 换视角崩溃 /
> 路径关键帧 / 玩家渲染 / 相机 / 手 / UI 回退）→ **以及 12.31~12.48**（本会话这一轮：
> 服务端漏录取证与瘦身 / 导出白屏 / AE2 崩溃 / 鼠标锁循环 / 滚轮调速 / Shift+ESC 复位 /
> 一致性检查 / 保存录像崩溃 / 坐标探针 —— **每一节的结论、判据与"下一轮要什么"都写清了**）。
>
> **当前状态一句话**：
>
> - **`mods\` 里部署的仍是 v65**（`mods\replaymod-1.7.10-2.5.2-gtnh.jar`，`10837941` B / SHA256 `907B72B8…45FCC`）
>   —— **本会话一行未动、随时可回滚**；v65 = 官方 `1.7.10-2.5.2` + GTNH manifest + 汉化 + **14 个功能 mixin**。
> - **最新的（累积版）待测包是 v74**：`release\GTNH-ReplayMod-v74-诊断\` + 同名 `.zip`
>   （`10852986` B / `EF8B05F0…36DA4`，**22 个 mixin + 3 个 util**）= v69~v74 全部改动，**要发就发它**。
> - **本会话已现场确认修好**：导出**白屏**（v66）· 放开鼠标点界面**不崩**（v67）· **「人回来了」** ·
>   **无手臂 / UI 正常** · 鼠标锁**循环消失**（v69，`RELEASED` 136→3）。
> - **仍开着（各有判据）**：点时间轴跳末尾（12.48 探针定位中）· 卡鼠标（12.45 待 `currentScreen=`）·
>   只剩天空（12.39 待 F3）· 保存录像崩溃（12.47 待新栈）· 服务端实体包漏录（12.31）。
> - **已知且已裁定接受**：回放里**原版血条仍会显示**（GTNH 不发 `HEALTH` 事件，见 12.29），
>   JourneyMap 小地图与 InGameInfoXML 信息面板照旧显示（那是那两个 mod 自己画的）。
> - **键位**：`Ctrl` = 锁定鼠标可见；`Shift` = 退出旁观 / 回自由相机；**`Shift+ESC` = 强制复位**（v71 起，回放中生效）。
> - **正式发布包**：`release\GTNH-ReplayMod-v65\` + `.zip`；**通用复查方法**：`release\VERIFICATION_GUIDE.md`。
>
> **硬性注意（每条都踩过，别省）**：
> - 编译 mixin 后**必须检查 javac 退出码，非 0 立即中止**（坑 47）；classpath **必须包含 `mixin-out` 自己**。
> - 适配包是**纯 javac、无 reobf**：**MCP 成员名运行时不存在，一律反射**（坑 29），**日志语句也要包在 try 内**。
> - **判断 mixin 有没有注入：用字节码导出**（`-Dmixin.debug.export=true` + `javap`），**不要看日志**；
>   同时**去日志里搜 `Mixin apply for mod … failed` / `InvalidInjectionException`** —— `require = 0` 的
>   解析期失败是**静默**的（坑 51 / 54，本轮为此白测过两轮）。
> - **改完必须更新 HANDOVER 第 12 节**，并同步文首速览表。
> - 测试脚本会 **kill 所有 java**；**游戏运行中 `mods\` 里的 jar 被锁，无法覆盖** ⇒ 动手前先确认游戏已退出。
> - 抓游戏画面一律用 `tools\gtnh-printwindow.ps1`（坑 40）；本环境**没有 GLFW**（lwjgl3ify 走 SDL3，坑 49）。
> - **加载期守卫要"延迟"不要"丢弃"**（坑 53）；**别接管别人整块渲染**（坑 56）；
>   **UI 类需求先问清"什么时候该显示、什么时候该隐藏"再动手**（坑 56）。
> - **★ 本会话新踩的三条（都返工过，别重复）**：
>   ① **mixin 注入体里绝不能直接引用 MC 成员**（写成 MCP 名编译不过，写成 SRG 名运行时找不到 ——
>      本工程无 reobf；**一律挪进 util 用反射**，12.44 为此当场返工）；
>   ② **兜底返回值要看调用方**：`SquashFilter` 里 `readBulk(...)` 的结果被直接 `.iterator()`，
>      所以只能给**空列表**、不能给 null（12.47）；
>   ③ **坐标/量纲类问题先量数据再改代码**（滚轮 12.15、鼠标坐标 12.48）—— 打几个数比猜三轮便宜。
> - **给测试者要日志一律要 `logs/fml-client-latest.log`**（我们的诊断走 `System.out`，**`latest.log` 里看不到**，
>   12.39 因此白拿过一份）；画面问题要 **F3 截图**；视频给**原文件**（用 ffmpeg 抽帧 / `framemd5` 量化）。
> - **测试者**：**Tester_A（338 mods）、Tester_B** 两位外部环境；**洛洛本人 = 「Luoxueruoling」** ⇒ **每条外部反馈都要"环境卡片"**（`release\VERIFICATION_GUIDE.md` 第 1 节）。
> - `tools\gtnh-window.ps1` 的位置依赖 `logs\dsh-launch.pid`；**自己启动的游戏要把那个文件改成真实 pid**。
>
> **构建 / 注入 / 部署（照抄即可）**：
>
> ```powershell
> # 1) 编译（每个文件单独 javac 并校验退出码；classpath 必须含 mixin-out 自己）
> $compat='<workspace>\replaymod-compat'
> $cp="$compat\mixin-out;$compat\deps;" +
>     "<workspace>\ServerReplay-1.7.10\build\rfg\recompiled_minecraft-1.7.10.jar;" +
>     "<workspace>\tools\replaymod-1.7.10-2.5.2.jar;$compat\libs\asm-9.9.jar"
> javac -proc:none -encoding UTF-8 -cp $cp -d "$compat\mixin-out" <每个 .java>
>
> # 2) 注入（幂等；基座固定用 v48，它已含全部基础改造）
> pwsh -File <workspace>\tools\inject-replaymod-clientmixins.ps1 `
>   -Jar  <workspace>\replaymod-1.7.10-2.5.2-gtnh-v48-ctrltoggle.jar `
>   -Dest <workspace>\replaymod-1.7.10-2.5.2-gtnh-vNN-<名字>.jar
>
> # 3) 部署（先确认游戏已退出）
> Copy-Item <新 jar> `
>   '<instance>\mods\replaymod-1.7.10-2.5.2-gtnh.jar' -Force
> ```
>
> **下一步（优先级）**：
> ① 把 **v65** 发到 338 mods 环境复验（导出 / 路径关键帧 / 换视角不崩 / 回放一开就有玩家 / 相机 / Shift）；
> ② 服务端**实体包漏录**（**12.6-⑤**，唯一剩下的核心功能缺口）；
> ③ `meta` 的 `players` / `singleplayer`（12.6-⑦）；
> ④ 若要做"旁观谁就显示谁的血条"：1.7.10 **结构上做不到**（12.30 三），只能**覆盖式自己画一层**。

## 10. 上游调研结论与重构方案（2026-10-10 深夜）

### 10.1 上游事实

- **ServerReplay（高版本 26.3）本身只有约 1600 行 Kotlin**，真正的录制能力全在外部库
  **`net.casual.arcade:replay`**（版本 `0.14.0-beta.7`，只发布在 `https://maven.supersanta.me/snapshots`，
  通过 Gradle 插件 `net.casualchampionships.joystick` 的 `arcade { version = ... }` 引入，**无公开源码仓库**）。
- 从 ServerReplay 的用法可以读出 arcade 的 API 面貌：
  - `ReplayPlayerRecorders.create(server, profile, connection, path, format, settings)`
    → `recorder.onStart()` / `afterLogin()` / `start()` / `stop()` / `save()`
  - `ReplayChunkRecorders.create(area, path, format, settings, name)`（按区块区域录制）
  - `ReplayFormat` + `ReplayModIO` / `FlashbackIO`（**输出格式抽象**，同一套录制可导出 mcpr 或其它格式）
  - `RecorderSettings`（含 `ChunkRecordingStrategy` 等策略）
  - 事件：`ReplayRecorderStartEvent` / `SaveEvent`（PRE/POST 相位）/ `CloseEvent` /
    `DurationLimitEvent` / `FileSizeLimitEvent` / `SystemStorageLowEvent`
  - 处理器类：`AutomaticRecorders`（ServerStart + PlayerLogin 自动开录）、`RecorderNotifier`（聊天广播）、
    `RecorderRecoverer`（重启后恢复）、`ReplayCleanerUpper`（清理旧录像）、`RecorderWarner`
  - 配置对象 `ReplayConfig`：`automaticallyRecord`、`playerPredicate`、录制路径、默认格式、settings
  - 还有完整的 `/replay` 命令树（start/stop/status/list/players/chunks/…）与 HTTP 下载拦截器
- **ReplayMod 1.7.10 的回放 HUD 处理**在 `com.replaymod.replay.camera.CameraEntity` 的内部事件类里：
  `preRenderGameOverlay(RenderGameOverlayEvent.Pre)` 会 `setCanceled(true)` 掉
  `ARMOR/HEALTH/FOOD/AIR/HOTBAR/EXPERIENCE/HEALTHMOUNT/JUMPBAR`，并临时关掉 `heldItemTooltips`
  （`ALL` 时关、`POST ALL` 时恢复）。**所以回放里"看得到血条/物品栏"本身不是预期行为**，
  值得与洛洛确认具体现象后再定位（待办见 10.3）。

### 10.2 我们的重构方案（分层对齐上游）

```
com.gtnh.serverreplay
├── ServerReplayMod          // 引导：配置加载 + 生命周期协调（无 @Mod 类，由 mixin 触发）
├── config/ReplayConfig      // automaticallyRecord / outputDir / format / 时长上限 / 大小上限 / 玩家过滤
├── recorder/
│   ├── ReplayRecorder       // 抽象：start/stop/save/close + 统计
│   ├── PlayerReplayRecorder // 现有实现（按连接）
│   └── RecorderRegistry     // 活跃录制器注册表（取代静态 ACTIVE map）
├── io/McprWriter            // ReplayStudio 封装（异步写入、cache/crc 清理、meta 组装）
├── processor/
│   ├── AutomaticRecorders   // 连接建立即开录（对应上游同名类）
│   ├── RecorderNotifier     // 开始/结束广播
│   ├── RecorderLimits       // 时长 / 文件大小 / 磁盘空间上限
│   └── ReplayCleanerUpper   // 旧录像清理
├── command/ReplayCommand    // /serverreplay start|stop|status|list
└── mixin/                   // MixinNetworkManager（抓包）、MixinGuiCustom（客户端入口）
```

**1.7.10 约束下的落地方式**（这部分是上游没有的，必须自己解决）：
- 没有 `@Mod` 类（JarJar LoaderV2 会提前解析，见坑 8）→ 配置加载、命令注册、事件订阅
  一律通过 **mixin 注入 `MinecraftServer`** 在做（例如构造器或 `startServer` 的 HEAD）触发一次初始化；
  命令用 **`FMLServerStartingEvent`** 注册（事件总线订阅也在这个初始化里完成）。
- 配置格式用 **JSON + MC 自带的 gson**（不引入新依赖），文件 `config/serverreplay.json`。
- 通知用 `IChatComponent` 广播给有权限的玩家。

**分阶段**（每阶段都要：`gradle build` + 离线自测 + 游戏内实测 + `analyze-mcpr.ps1` 解析取证）：

| 阶段 | 内容 | 风险 |
|---|---|---|
| **P1** | 配置化（自动录制开关、输出目录、上限）；meta 完善（`singleplayer`/`players`/`serverName`）；开始/结束统计日志 | 低 |
| **P2** | `/serverreplay` 命令（start/stop/status/list）；开始/结束广播；时长/文件大小/磁盘上限 | 中 |
| **P3** | 旧录像清理；服务器重启恢复；按上面的包结构整理代码 | 中 |

### 10.3 待确认 / 待办

- [ ] 与洛洛确认"回放里还有玩家界面"的**具体现象**（是原版 HUD 血条/物品栏没被隐藏？还是指 ReplayMod 自己的时间轴界面？
      —— 后者是正常的）。若确实是原版 HUD，则要查 `CameraEntity` 的 `RenderGameOverlayEvent` 处理器为何没生效。
- [ ] P1 开始前先确认：录像的 `metaData.json` 里 `players` 应该填玩家 UUID 列表（客户端录像是
      `["<uuid>"]`，我们现在是空数组），`singleplayer` 在集成服务端应为 `true`。

---

## 11. 重构进展（按上游 1.20.1 / 1.21 落地）

### 11.1 上游资料获取现状（网络受限，务必先看，省得白试）

- **`git clone` / `git ls-remote` 到 github.com 现在会 `Connection was reset` / 连不上 443**
  （ServerReplay 是早前克隆成功的，不要指望现在还能克隆）。
- **`api.github.com` 可用**，并且支持用 **`git/trees/<ref>?recursive=1`** 一次拿到整棵树（比逐个
  `contents` 省得多）；取文件内容用
  `api.github.com/repos/<owner>/<repo>/contents/<path>?ref=<branch>`（返回 base64）。
- **ReplayMod 不走分支**，而是 `versions/<MC 版本>/` 目录；本地 `<workspace>\ReplayMod-stable\versions\`
  已经有 `1.20.1`、`1.21`、`1.21.2`… 但**这些目录里目前只有 `.gitkeep`**（源码在别的分支/子模块里）。
- **ServerReplay 的分支名就是版本号**，实有：`1.17.1 / 1.18.2 / 1.19.4 / 1.20.1 / 1.20.2 / 1.20.4 /
  1.20.6 / 1.21 / 1.21.3 / 1.21.4 / 1.21.5 / 1.21.6 / 1.21.8 / 1.21.9 / 1.21.11 / 26.1 / 26.2 / 26.3`。

### 11.2 ServerReplay 1.20.1 的架构（**自带完整实现，比 26.3 更有参考价值**）

26.3 把录制能力全挪进了私有库 `net.casual.arcade:replay`（只在 `maven.supersanta.me` 发布，无公开源码），
而 **1.20.1 自带实现**，文件清单（已通过 tree API 确认）：

```
src/main/kotlin/me/senseiwells/replay/
├── ServerReplay.kt                      入口
├── recorder/ReplayRecorder.kt          33.6 KB  核心录制器
├── recorder/ChunkSender.kt              7.6 KB
├── recorder/RecorderRecoverer.kt        5.8 KB  重启后恢复未保存录像
├── player/PlayerRecorder.kt             7.0 KB  按玩家录制
├── player/PlayerRecorders.kt            3.7 KB  注册表
├── chunk/ChunkRecorder.kt              13.7 KB  按区块区域录制
├── chunk/ChunkRecorders.kt              7.8 KB
├── util/SizedZipReplayFile.kt           4.1 KB  带大小统计的 mcpr 写入
├── util/CounterOutputStream.kt          359 B   字节计数流
├── util/ReplayOptimizerUtils.kt         6.9 KB  包优化
├── util/FileUtils.kt                    3.1 KB
├── config/ReplayConfig.kt               7.3 KB
├── commands/ReplayCommand.kt           28.0 KB  （Brigadier 命令树）
└── mixin/common/MinecraftServerMixin.java 1.8 KB 生命周期钩子
```

**`MinecraftServerMixin.java` 全文逻辑**（已解码，直接对应我们的实现）：

```java
@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(method = "runServer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;buildServerStatus()...", shift = At.Shift.AFTER))
    void onServerLoaded(CallbackInfo ci) {
        RecorderRecoverer.tryRecover((MinecraftServer) (Object) this);   // 恢复未保存录像
        if (ServerReplay.getConfig().getEnabled()) { /* startChunks */ }
    }
    @Inject(method = "saveAllChunks", at = @At("TAIL"))
    void onSave(..., CallbackInfoReturnable<Boolean> cir) { ReplayConfig.write(...); }
    @Inject(method = "stopServer", at = @At("TAIL"))
    void onServerStopped(CallbackInfo ci) { /* 停掉所有 recorder */ }
}
```

### 11.3 我们已经完成的重构

| 阶段 | 内容 | 证据 |
|---|---|---|
| **P1** | `config/ReplayConfig.java`（gson，`config/serverreplay.json`）：`automaticallyRecord`、`outputDir`、`fileNameFormat`（`{timestamp}`）、`serverName`、`recordingBlacklist`、`maxFileSizeMb`、`maxDurationMinutes`、`systemStorageLowLimitMb`、**`ignore*Packets` 包过滤**、`recoverUnsavedReplays`、`deleteReplaysAfterDays`、`debug` | 离线自测生成了完整配置文件；`ReplayRecorder` 已按配置决定目录/文件名/上限/过滤 |
| **P2** | `MixinMinecraftServer`（对应上游 `MinecraftServerMixin`）：`func_71247_a`(startServer) TAIL → 加载配置 + 注册 `/serverreplay`；`func_71260_j`/`func_71263_m`(stop) → `ReplayRecorder.stopAll()`（**修掉了服务器关停时录像不收尾、留下 `.tmp` 的问题**） | 构建通过；`javap` 确认注入点与 `CommandHandler.func_71560_a` 调用 |
| **P2** | `command/ReplayCommand.java`（`status`/`list`/`stop`/`reload` + 补全），继承 `CommandBase`、权限等级 2 | `javap` 确认 reobf：`func_71517_b`/`func_71514_a`/`func_71518_a`/`func_82362_a`/`func_71515_b`/`func_71516_a` |

**1.7.10 特有注意**：`CommandBase` 已有 `public static getListOfStringsMatchingLastWord(...)`，
自己再写一个同名 private static 会编译失败（"attempting to assign weaker access privileges"）——
我们改名为 `matchingOptions`。另外 `ICommandManager` 接口没有 `registerCommand`，必须强转 `CommandHandler`。

### 11.4 还没做的（P3 及以后）

- [ ] **`meta` 的 `players` / `singleplayer`**：需要把玩家上下文（名/UUID/是否单机）从 mixin 传到 recorder，
      当前签名是 `onOutboundPacket(connection, state, id, data)`，要扩展成带上下文的版本。
- [ ] **`RecorderRecoverer`**（启动时恢复未完成录像）与 **`ReplayCleanerUpper`**（按 `deleteReplaysAfterDays` 清理）——
      配置项已经预留，实现还没写。
- [ ] **包优化**（对应上游 `ReplayOptimizerUtils`：合并/丢弃冗余实体包）。
- [ ] 按第 10.2 节的包结构把 `ReplayRecorder` 拆成 `recorder/` + `io/` + `processor/`。
- [ ] 客户端：删掉永不触发的 `MixinGuiFakeMain`。

---

## 12. 本轮工作总结（2026-10-10 深夜）—— 交接要点

### 12.1 一句话现状

- **服务端录制**：核心链路**可用**（`ReplayRecorder` 能写出合法 mcpr，单人集成服务端实测
  **7042~22466 包 / 77~118 秒**，meta 正确）；**P1 配置层 + P2 生命周期/命令已完成**（见第 11 节）。
  未解决：**实体包漏录**（12.6-**⑤**）。
- **客户端 ReplayMod**：主菜单「**回放中心**」入口（**中文**）、汉化、录制、**回放可正常打开** —— 均已实测通过。
  **上一轮**连续结案四件事：物品栏拖不动（**12.10**，根因是 coremod 的 ASM transformer）、
  「transformer 代价」回收（**12.11**，作用域收窄到 ReplayMod 自己 + 具名静态 visitor）、
  主菜单入口消失（**12.12**）、进入回放崩溃（**12.13**）。
  后两件的共同根因是**架构耦合**：入口与 Et Futurum 回放修复原本都住在 ServerReplay 里，
  一旦为了单变量实验隔离 ServerReplay，它们就集体失效 —— 现已全部搬进适配包并与其解耦。
- **交付物（当前）**：
  - **`replaymod-1.7.10-2.5.2-gtnh-v22-spawnguard.jar`** ← **当前交付**
    （`10809901` B，SHA256 `79BD35ADBBAE16B575960384AC0B7E70F5D127FC6490167191D1039D52AB101A`；
    自带 3 个客户端 mixin：入口 / EtFuturum 构造 / `SpawnPlayer` 加载期保护）
  - `ServerReplay-1.7.10/build/libs/ServerReplay-1.7.10-0.1.0-client.jar`（客户端/单人游戏用，无 shade）
  - `ServerReplay-1.7.10/build/libs/ServerReplay-1.7.10-0.1.0.jar`（专用服务端用，含 shade）
  - `gtnh-replaymod-asm-compat-1.7.10.jar`（**已被证明不可用**，原因见 12.4-③）

### 12.2 客户端入口（已完成，已人眼确认；**归属已于 12.12 变更**）

> ⚠️ 本节的**技术结论仍然成立**，但**归属变了**：入口原先是 ServerReplay 的
> `com.gtnh.serverreplay.mixin.MixinGuiCustom`；**2026-10-10 夜已搬进 ReplayMod 适配包**
> （`com.gtnh.replaycompat.mixin.MixinGuiCustomReplayEntry`，并补了 `drawScreen` 双 hook、
> 全反射实现、中文标签）。详见 **12.12**。**不要把入口再放回 ServerReplay**（勘误区 #17）。

原 `MixinGuiFakeMain` 永不生效，真正要注入的是 **`lumien.custommainmenu.gui.GuiCustom#func_73866_w_`**
（CMM 的 `openGui` 事件把主菜单换成 `GuiCustom`；`GuiFakeMain` 只是被反射塞进 `InitGuiEvent.gui` 的壳，
且 CMM 的 `initGuiPost` 会丢掉事件里所有非 `GuiCustomButton` 的按钮）。
做法是注入 `func_73866_w_`，通过 public 的 `getButtonList()` 加原生 `GuiButton`，
再注入 `func_146284_a` 处理点击。日志证据：`ReplayMod main menu button added at (530, 8) on a 640x374 screen`。

### 12.3 服务端录制（本轮做完的事）

| 事项 | 结果 |
|---|---|
| 真正写 mcpr | ✅ LOGIN+PLAY 全录、跳过 KeepAlive、断开/关服时收尾、清 `.cache` |
| FML 包 | ✅ 按 ReplayMod 官方做法 `FMLProxyPacket.toS3FPacket()` 转换后再录（否则 GTNH 回放缺握手包） |
| 玩家自身实体 | ✅ 收到 `S01PacketJoinGame` 时注入合成 `S0CPacketSpawnPlayer`，并把 entityId 写进 `meta.selfId` |
| 写入线程 | ✅ 序列化仍在游戏线程，**编码+写盘**移到 `serverreplay-writer` 守护线程（有界队列 20 万，满则丢包告警） |
| 录制关闭时开销 | ✅ `isCaptureEnabled()` 让 mixin 在关闭录制时**一进方法就返回**；被 `ignore*Packets` 过滤的包不再先序列化 |
| 关服收尾 | ✅ `MixinMinecraftServer` 注入 `func_71260_j` / `func_71263_m` → `stopAll()`（修掉了留下 `.tmp` 残留的问题） |
| 配置 | ✅ `config/serverreplay.json`（gson），字段见 11.3 |
| 命令 | ✅ `/serverreplay status\|list\|stop\|reload` |
| 离线自测 | ✅ `server-replay/tests/ServerReplaySelfTest.java`（不需要游戏，验证 shade 闭包 + mcpr 往返） |
| 录像目录 | ✅ 写 ReplayMod 的 `replay_recordings/`（回放中心**不递归**，写 `replays/server/` 会看不见） |

### 12.4 ReplayMod 适配包的完整差异清单（★ 最重要）

用「官方 jar vs 适配 jar 全量条目对比」得出的**全部**差异（其余 6000+ 条目逐字节相同）：

| # | 差异 | 官方 | 适配包 | 判断 |
|---|---|---|---|---|
| ① | **11 个 mixin 配置** | 无 | 多了 **`"priority": 500`** | ⚠️ **头号嫌疑**：mixin 是「priority 高者先应用」，默认 1000，改成 500 等于让 ReplayMod 的 `MixinPlayerControllerMP`/`MixinKeyboardListener`（管点击与输入）在争抢里往后排 |
| ② | `TweakClass` | `com.replaymod.core.tweaker.ReplayModTweaker` | `org.spongepowered.asm.launch.MixinTweaker` | ⚠️ 丢失了 `injectIntoClassLoader` 里的 `MixinBootstrap.getPlatform().addContainer(...)` 与 `initModCore`（后者只是安装可选 ModCore，无害） |
| ③ | `FMLCorePlugin` | `com.replaymod.core.LoadingPlugin` | 已移除 | ⚠️ 连带丢失 **`GLErrorTransformer` + `GLStateTrackerTransformer`** |
| ④ | `TweakOrder` | `0` | 已移除 | ⚠️ 影响 tweaker 执行顺序 |
| ⑤ | `ForceLoadAsMod` + `MixinConfigs` | 无 | 有 | ✅ 必要（GTNH 约定，否则 mixin 不生效） |
| ⑥ | `assets/replaymod/lang/zh_CN.lang` | 21805 B | 26395 B（+63 条） | ✅ 汉化 |
| ⑦ | `GuiHandler.class` 一个字符串 | `custommainmenu.gui.GuiFakeMain` | `xustommainmenu…`（等长替换） | ✅ 安全（绕过 CMM 检测） |

**③ 的补法踩了坑**：做成**独立** coremod jar（`gtnh-replaymod-asm-compat-1.7.10.jar`）会启动失败：

```
NoClassDefFoundError: com/replaymod/core/asm/GLErrorTransformer$1$1
NoClassDefFoundError: cpw/mods/fml/common/Optional$Method
```

即 coremod 阶段那个类加载器解析不了 transformer 自己的匿名内部类。**ReplayMod 官方能用，是因为
`LoadingPlugin` 与那两个 transformer 在同一个 jar 里**。所以正确做法是把 coremod 类**塞进 ReplayMod jar
内部**再改 manifest —— 脚本 `tools/inject-replaymod-transformers.ps1` 就是干这个的（对应 12.4-③）。

### 12.5 关键实验结论：物品栏问题的定性

**严格单变量**（每一步只动一个东西，ServerReplay 一直隔离在 `mods-disabled/`）：

| 环境 | 物品栏 |
|---|---|
| ServerReplay + ReplayMod | ❌ 不能拖（洛洛最初反馈） |
| **仅 ReplayMod**（移除 ServerReplay） | ❌ **不能拖** |
| **纯 GTNH**（两者都移除） | ✅ **可以移动** |

**结论**：元凶是 **ReplayMod（我们改造的适配包）**，**与 ServerReplay 完全无关**。
洛洛一开始的判断（"我们的 ReplayMod 本地有问题"）是对的。
剩下的工作就是确定是 ①②③④ 里的哪一个 —— ②③④ 已按 12.4 处理，①priority 已去掉（v2）。

### 12.6 已知未解决问题（按优先级；2026-10-10 夜第二次接手会话后更新）

1. ~~**ReplayMod 适配包的交互/界面问题**（物品栏拖不动、生存界面异常）~~ ✅ **已解决（12.10）**：
   根因是 coremod 的 ASM transformer 触发 `ClassCircularityError`（不是 mixin、不是 priority、
   不是 TweakClass/TweakOrder）。现役版本（v22）已含**作用域收窄**的 transformer（12.11）。
   **残留待观察**：生存界面（血条/饥饿度等 HUD）是否也一并正常——洛洛那次只确认了物品栏。
2. ~~**★ 回放中心滚轮滚动不流畅**~~ ✅ **已解决（见 12.15）**：根因是 jGui 的滚动公式按
   **LWJGL 2 的 `dWheel=±120`** 校准，而 GTNH 跑的是 **lwjgl3ify（LWJGL3 + SDL），实测 `dWheel=-1`**
   → `ceil(1/4) = 1` 像素/格 → 「幅度太小/几乎不动」。
   已用适配包新增的 `MixinGuiScrollableWheelScale` 归一化解决；
   **原来"摘 `MixinGuiScreen` 做单变量"的方向是错的**（那条只负责事件分发，与缩放无关）。
3. ~~**★「保存录像」面板要在主菜单可用**~~ ✅ **已解决（见 12.16 / 12.17 / 12.18）**：
   三层障碍逐一定位并修掉 —— ① 面板**没被挂载**（`GuiBackgroundProcesses.onGuiInit` 的宿主守卫
   因 CMM 的 `GuiFakeMain` 而 return）；② 同一 `panel` 被**重复挂载**；
   ③ jGui 的**鼠标输入到不了面板**（`EventHandler.onMouseInput` 是 `LOWEST` 优先级且 wrapper 被注销）。
   现已在主菜单**完整可用**：显示 + 点击输入框 + 点「完成」→ 文件归位 + 面板关闭（v29 实测）。
4. ~~**★ 恢复未完成录像**（洛洛点名）~~ ✅ **已结案（12.20）—— 无需修复**：
   **1.7.10 客户端本来就内置了完整机制**（`RestoreReplayGui` + 中文文案 + `raw`/`.no_recover`/`.mcpr.tmp`），
   实测在 GTNH 上**显示 / 点击 / 恢复执行三段全通**（`duration` 0→28807、`generator` 打上
   `(+ ReplayMod Replay Recovery)`、`tmp\` 清理、零 crash）。
   **仍然缺的是服务端侧**：ServerReplay 的 `recoverUnsavedReplays=true` 配置预留、实现未写（见 12.6-⑧）。
   注意 `raw\` 是**故意保留 21 天的调试备份**，不是待恢复物。
5. **服务端录像漏录实体包**（实测同会话对比，服务端 vs 客户端）：
   `0x12 EntityVelocity` 96 vs 1332、`0x17 EntityLookMove` 23 vs 1246、`0x25 BlockBreakAnim` **0 vs 441**、
   `0x18` 0 vs 13、`0x0b` 0 vs 3、`0x04` 0 vs 1。时间范围两边一致（1.1s~28.5s），**不是中途断录**；
   已排除「被 Forge 包装成 `0x3f`」（两边 `0x3f` 完全相同，5346 vs 5346）。
   已解出 `0x25` 的 `entityId=1600` = **玩家自己**，说明服务端确实在发 → 是我们的拦截没拿到。
   **下一步**：加运行时诊断，记录所有经过 `scheduleOutboundPacket` 的包 id，确认这些包是否走了别的发送路径。
6. ~~**方块渲染（黄羊毛变白）**~~ ✅ **已结案（见 12.14）**：**录像侧被字节级排除** ——
   同一次会话的客户端/服务端录像在 `0x26`/`0x23`/`0x03`/`0x3f` 上 **MD5 多重集逐字节完全相同**。
   真实现象是「回放**刚打开、相机贴在人物身上**时近处黑」，**相机离开人物后渲染正常**；
   `GLStateTrackerTransformer` 是**加重因素而非根因**（关掉恢复约一半）。洛洛裁定**不修**。
7. **`meta` 的 `players` / `singleplayer`**：ReplayMod 客户端录制会在每次 `SpawnPlayer` 时把 UUID 加进
   `metaData.players`，并把 `singleplayer=local`、`serverName=集成服务端的世界名`；我们目前是空数组 + `false`。
   需要把玩家上下文从 mixin 传进 recorder（要扩 `onOutboundPacket` 签名或引入 context 对象）。
8. **P3 其余项**：`RecorderRecoverer`（恢复未完成录像，与上面第 4 条同源）、
   `ReplayCleanerUpper`（按天数清理）、包优化（对应上游 `ReplayOptimizerUtils`）、
   按 10.2 的包结构拆分代码、删 `MixinGuiFakeMain`（已确认是死代码）。
9. **待优化（低优先）**：`SpawnGuard` 目前是**丢弃**加载期到达的 `SpawnPlayer`（12.13），
   更理想的做法是**缓存到 world 建好再重放**，这样既不会崩、合成出来的"自己"也能出现在回放里。

### 12.7 上游与社区调研结论（省得再查一遍）

- **GTNH 组织里没有任何 ReplayMod 适配仓库**（`search/repositories?q=replay+org:GTNewHorizons` → `total_count: 0`），
  这块工作是独一份的，**没有现成答案可抄**。
- **ReplayMod 在 1.7.10 上的已知未修缺陷**（都可在 api.github.com 查到）：
  - **#294** 与其他 mod 同用 → 录像里**看不到方块**（只有玩家黑轮廓）；johni0702 **明确表示不修**，"PRs welcome"
  - **#388 / #770** 保存录像时 `PacketUtils.getEntityId` 读包越界（`IndexOutOfBoundsException` in `SquashFilter`）
  - **#1044**（2025-01 仍有人报）**回放**时 `FMLProxyPacket.func_148833_a` **NPE** → `GuiOpeningReplay`
  - **#808** 编辑器里交互（按 T 聊天、倒带）疯狂报错、地形界面卡死
  - **#450** 打开物品栏出问题 —— 官方给的唯一手段就是**二分法找冲突 mod**（我们这几轮做的正是这件事）
- **ServerReplay 1.20.1 自带完整实现**（26.3 已挪进私有 arcade 库，无公开源码），
  文件清单与 `MinecraftServerMixin` 全文见第 11.2 节。

### 12.8 网络与环境（下次直接照做）

- `github.com:443` 现在**连不上**（`Connection was reset` / 超时）→ `git clone` / `ls-remote` 都失败。
- **`api.github.com` 可用**：整棵树用 `git/trees/<ref>?recursive=1`；单文件用
  `contents/<path>?ref=<branch>`（返回 base64，注意很占上下文）。
- ReplayMod 的 1.7.10 分支名就是 `1.7.10`（`versions/` 目录是发布结构的产物）；
  ServerReplay 的分支名就是版本号（1.20.1 / 1.21 / …）。
- 构建/测试命令、`-Dserverreplay.*` 开关、`tools/analyze-mcpr.ps1` 用法见第 7 节。

### 12.9 交接时的实验状态（**已于 2026-10-10 夜解决 —— 见文末 12.10**）

> ⚠️ 本节是**上一轮**交接时的状态快照，其中的判读已被推翻，**保留只为追溯**。最新状态看 **12.10**。

交接那一刻正在跑的实验组合（`mods/` 里）：

| 文件 | 说明 |
|---|---|
| `replaymod-1.7.10-2.5.2-gtnh.jar` | = v2（**已去 priority**）+ **同 jar 内置** `ReplayModTransformers` 作为 `FMLCorePlugin` |
| `ServerReplay-*.jar` | **不在 mods 里**，隔离在 `mods-disabled/`（单变量实验需要） |

待测（洛洛）：进世界能否拖动物品栏、生存界面是否正常。
**若好了** → 把这套适配方式固化进 `make-gtnh-replaymod.ps1`，把 ServerReplay 放回去继续做 12.6-②。
**若没好** → 下一个单变量是 ② `TweakClass`/④ `TweakOrder`，做一个"保留 `ReplayModTweaker` 的
`injectIntoClassLoader` 行为、但不让它抢跑 mixin"的 tweaker。

> **实测结果（洛洛）：没好，物品栏仍然拖不动。** 而后续追查证明：上一句的 ②④ 判读是错的
> （见勘误区 #14 与 12.10），真正的原因是 coremod transformer。

**其它交接注意**：
- `replays/server/` 下还留着两份早期录像（`server-2026-10-10_11-10-21.mcpr` 903 KB、
  `11-16-05.mcpr` 634 KB），是旧输出目录时代的产物，**没有删**（里面有洛洛的游戏内容）。
- 中断录制留下的 `.tmp` 目录已清理干净。
- `mods-disabled/` 里的 jar 是**故意的**，不是残留。

---

### 12.10 「物品栏拖不动」结案（2026-10-10 夜，本次会话；洛洛实测确认已修复）

**一句话**：根因是 **coremod 注册的 ASM transformer 在方法体内创建匿名 visitor 类**，
导致 `LaunchClassLoader` 重入并抛 `ClassCircularityError`；把两个 transformer 都改成
opt-in（默认不注册）后，**启动正常 + 物品栏可正常拖动**。

**交付物（当前应该装在 `mods/` 里的版本）**：

| 路径 | 大小 | SHA256 |
|---|---|---|
| `replaymod-1.7.10-2.5.2-gtnh-v6-notransformer.jar` | 10791220 | `18954E5D93ADA4A614B26AE1182BB1FA7C01E1332255B41AB1E6B8A1EA97F871` |

组成：v2（去 priority）+ **同 jar 内置** `com.gtnh.replaycompat.ReplayModTransformers`（`FMLCorePlugin`）
+ 该 coremod 的 `getASMTransformerClass()` **默认返回空数组**。

**修复内容**（`<workspace>\replaymod-compat\src\com\gtnh\replaycompat\ReplayModTransformers.java`）：

```java
@Override
public String[] getASMTransformerClass() {
    if ("true".equals(System.getProperty("replaymod.glerrors", "false"))) {
        return new String[] {
            "com.replaymod.core.asm.GLErrorTransformer",
            "com.replaymod.core.asm.GLStateTrackerTransformer"
        };
    }
    return new String[0];   // 默认：一个都不注册
}
```

**实测证据（v4/v5 的崩溃原文，必须记住）**：

```
Caused by: java.lang.ClassCircularityError: com/replaymod/core/asm/GLErrorTransformer$1$1
    at com.replaymod.core.asm.GLErrorTransformer.transform(GLErrorTransformer.java:32)
Caused by: java.lang.ClassCircularityError: com/replaymod/core/asm/GLStateTrackerTransformer$2$1
    at com.replaymod.core.asm.GLStateTrackerTransformer.findEligibleMethods(GLStateTrackerTransformer.java:69)
→ 连锁 NoClassDefFoundError: openmods/config/simple/ConfigProcessor → Exception in thread "RFB-Main"
```

**为什么"同 jar"还不够**（补强 12.4-③）：两个 transformer 都在**方法体内** `new` 匿名
`ClassVisitor`/`MethodVisitor`（`GLErrorTransformer.java:31/35`、`GLStateTrackerTransformer.java:39/46/68/71`），
所以嵌套类 `$1$1` / `$2$1` 只在**首次 `transform()` 执行时**才生成/加载 —— 那时
`LaunchClassLoader` 正处在 `runTransformers()` 中，递归要求同一个类加载器再生成并加载一个新类，
直接踩中 JVM 的循环保护。**没有任何注册顺序能绕开**，只能不注册，或者把两个 transformer
改写成**具名静态 visitor 类**（在首次 `transform` 之前就热加载好）后再启用。

**代价（可接受，但要知情）**：`GLStateTrackerTransformer` 负责把 `GL11.glEnable/glDisable` 调用点
改写到 `com.replaymod.render.hooks.GLStateTracker` 钩子。**只影响视频渲染的 GL 状态簿记**，
不影响录制、回放播放与输入。若将来真的有渲染缺陷被追到这里，按上面的"具名静态类"方案重写。

**验证方式（本轮已跑通，下次可复用）**：
- 启动：`pwsh -File tools\launch-gtnh.ps1`（需先 `Set-ExecutionPolicy -Scope Process Bypass`）
- 判读主菜单：`dsh-stdout.log` 里出现 `Sound engine started`
- 崩没崩：看 `dsh-stdout.log` 有没有 `ClassCircularity` / `Exception in thread "RFB-Main"`，以及 `crash-reports\`
- **窗口自动化（本轮新建）**：`pwsh -File tools\gtnh-window.ps1 -Find | -Shot <png> | -Click "x,y" | -Drag "x1,y1,x2,y2" | -Key <VK>`
  - 客户区坐标是**物理像素**；游戏窗口 854x480 经 150% DPI 缩放 → 客户区约 `1706x996`，
    主菜单「单人游戏」约在客户区 `(275,470)`，开物品栏用 `-Key 69`（E）
  - 截图存 `tools\shots\`，读图直接看（注意 DSH 会把图放大 2 倍预览）
- 本轮完整链路：主菜单 →「单人游戏」（GTNH 直接续上次的世界）→ `Preparing start region for level 0` → 进世界

**下一步（回到主线）**：把 `ServerReplay-1.7.10-0.1.0-client.jar` 从 `mods-disabled/` 放回 `mods/`，
继续做 **12.6-②：服务端录像漏录实体包**（`0x12`/`0x17`/`0x25` 等），
方法照 12.6-② 说的：**加运行时诊断，记录所有经过 `scheduleOutboundPacket` 的包 id**，
确认那些包是否走了别的发送路径。

**顺手可做的加固**：
1. `tools/inject-replaymod-transformers.ps1` 的注释把"同 jar 就够了"写得太满，应补上 transformer
   匿名类的限制（勘误区 #16）。
2. `make-gtnh-replaymod.ps1` 目前只做 manifest/priority/汉化/CMM 绕过，**不含 transformer 注入**。
   固化"v6 配方"时应把这两步串起来：`make-gtnh-replaymod.ps1` → `inject-replaymod-transformers.ps1`
   （后者用的 coremod 类必须来自已含 opt-in 逻辑的源码）。
3. 客户端上游参考索引见 `upstream-ref\README-上游参考索引.md`（含 1.7.10 / 1.20.1 / 1.21 / jGui /
   Hodgepodge 的对照与结论）。

---

### 12.11 「处理 transformer 代价」结案（2026-10-10 夜，本次会话）—— 根因是作用域过大

**目标**：12.10 为了让游戏能启动，把两个 transformer 都关了；代价是丢掉 `GLStateTracker`
（`GL11.glEnable/glDisable` 的渲染管线状态簿记）。本节记录把它找回来的尝试。

#### 已完成

1. **具名静态 transformer 重写**（`<workspace>\replaymod-compat\src\com\gtnh\replaycompat\asm\`）：
   - `GLStateTrackerTransformer.java` / `GLErrorTransformer.java`：把它们方法体内的匿名
     `ClassVisitor`/`MethodVisitor` 全部改成**具名静态嵌套类**，语义与上游一致；
   - `Launcher.java`：`preload()` 在 coremod 构造函数里把 16 个类（含 ASM / LWJGL 类型）
     提前 `Class.forName(name, true, loader)`，从而**在首次 `transform()` 之前**完成加载链接；
   - `ReplayModTransformers` 构造函数调用 `Launcher.preload()`。
2. **实测注册成功**（v7/v8/v9 都验过）：
   ```
   [replaycompat] preloaded 16 transformer/ASM classes
   Registering transformer com.gtnh.replaycompat.asm.GLStateTrackerTransformer
   ```
   **零 `ClassCircularityError`** —— 12.10 那个致命缺陷确实被这套改法解决了。
3. **离线字节码验证**（`replaymod-compat/probe/TransformProbe.java`，不进生产 jar）：
   对真实 `ItemRenderer.class` 跑 transformer → `METHOD SET PRESERVED: true`、
   `CHECK: parsed ok, structural problems = 0`、字节码合法。
4. **找到真正的新冲突**：**Galacticraft** 的
   `micdoodle8/mods/galacticraft/core/mixins/early/minecraft/ItemRendererMixin` 注入点是
   ```
   @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glEnable(I)V")
   ```
   而 `GLStateTrackerTransformer` 要做的事**正是把那个调用点改写掉** → Galacticraft 注入
   `Scanned 0 target(s)` → `NoClassDefFoundError: ItemRenderer` → 崩溃。
   扫描 `mods/` 全量 mixin class 后，**注解里直接写 `GL11;glEnable/glDisable(I)V` 描述符的共 13 处**，
   真实目标类：`ItemRenderer`（Galacticraft + gtnhlib）、`RenderItem`（Angelica ×2）、
   `EntityRenderer`（DistantHorizons）、`cpw/mods/fml/client/SplashProgress$3`
   （Angelica + ArchaicFix）、`ic2/core/item/RenderLiquidCell`（Angelica）。

#### 结案（2026-10-10 夜，本次会话）—— 根因是「transformer 作用域过大」，洛洛一句话点破

**洛洛的提问**：「是不是只对录制 mod 进行了修改？」——**这一问就是答案**。
原实现（照抄上游语义）对**每一个类**都改写 `GL11.glEnable/glDisable` 调用点，等于拿 ReplayMod 的渲染钩子
去改整个 GTNH 的字节码。由此产生**两个独立的崩溃**：

| # | 现象 | 机理 |
|---|---|---|
| A | `StackOverflowError at GLStateTracker.hook_glEnable(GLStateTracker.java:34)` | `hook_glEnable(int)` 的实现是 `GL11.glEnable(i); getInstance().updateEnabledState(...)` —— 它**必须**调用真正的 `glEnable`。改写它 → `hook_glEnable` 自递归。**按类名跳过 `GLStateTracker` 也没拦住**（jar 内该类仍是原始 2380 B，说明是运行时被改写） |
| B | `InjectionError: galacticraft$onRenderOverlays ... Scanned 0 target(s)` → `NoClassDefFoundError: ItemRenderer` | Galacticraft 的注入点就是 `@At(target = "Lorg/lwjgl/opengl/GL11;glEnable(I)V")`，被我们改写后它的锚点消失 |

**修复**：把两个 transformer 的作用域收窄到 **只处理 ReplayMod / jGui 自己的包**
（`ALLOWED_PREFIXES = { "com/replaymod/", "de/johni0702/" }`），并额外用
`internalName.contains("GLStateTracker")` 兜底。不在作用域内的类**一个字节都不碰**。

这是**结构性修复**，不是继续加黑名单：Galacticraft 的 `ItemRenderer`、Angelica 的 `RenderItem`、
DistantHorizons 的 `EntityRenderer`、IC2 的 `RenderLiquidCell`、Angelica/ArchaicFix 的
`SplashProgress$3` —— 这些之前被扫描出来会冲突的目标，现在**天然不在作用域内**。

**验证（v11，`-Dreplaymod.transformers=true` 开启状态下真机跑完）**：

| 项 | 结果 |
|---|---|
| 启动 | ✅ 主菜单就绪（14:28:33） |
| 进世界 | ✅ 点击「单人游戏」后正常加载世界、进程存活 |
| `StackOverflowError` | ✅ 0 |
| `InjectionError` / `NoClassDefFoundError` / `VerifyError` | ✅ 0（与 transformer 关闭的 v10 段逐段对照） |
| Galacticraft `ItemRendererMixin` | ✅ **注入成功**（`Mixing minecraft.ItemRendererMixin from mixins.GalacticraftCore.early.json into ItemRenderer`） |
| Angelica / gtnhlib / AppleCore / Backhand / Battlegear2 / Hodgepodge / MyCTMLib 的 ItemRenderer 系 mixin | ✅ 全部注入成功 |
| GL 错误 | ✅ 0 |
| 离线探针 | `GLStateTrackerTransformer -> GLStateTracker changed=false`；处理过的类 `METHOD SET PRESERVED: true`、`structural problems = 0` |

**代价（明确记录）**：GL 调用追踪不再覆盖「MC 本体与其他 mod 发出的 GL 调用」，只覆盖
ReplayMod 自己的渲染代码。这是刻意的取舍——该 tracker 的用途是喂 ReplayMod 自己的渲染管线。

**当前开关语义**（`ReplayModTransformers#getASMTransformerClass`）：
```
（默认）                                          # 两个 transformer 都不注册
-Dreplaymod.transformers=true                     # 注册 GLStateTrackerTransformer（已验证可用）
-Dreplaymod.transformers=true -Dreplaymod.glerrors=true   # 外加 GLErrorTransformer（调试用，会在任何 GL 错误时抛异常）
```

**遗留的小疑点（不阻塞，供以后需要时查）**：`transform()` 里按类名做的排除/跳过分支始终没有打出过
日志（`skipping ...`），而离线探针同样条件下是正常的。怀疑 launchwrapper 传入的 `name` 与
`transformedName` 语义与预期不同。**现在作用域门已经让这个问题无害**（不依赖按名跳过也能正确工作），
所以不追了；若将来要恢复「对 Minecraft 本体也追踪」，先查清这一点。

#### 更早一轮（已被上面的结案取代，保留备查）

原「未解之谜」记录如下，其结论（`name` 格式问题）经上面验证**不成立**：

> 为防止冲突加了类型排除表（`EXCLUDED_CLASSES`），但 v8（启用 + 排除表）仍崩在同一个 Galacticraft
> 注入失败，且 stdout 完全没有排除分支日志。最可疑是 `transform()` 收到的 `name` 是点号形式而排除表
> 用斜杠形式。v10 加了格式兼容但未验证。

#### 当前交付状态（v12 —— 代价已真正回收）

- **`replaymod-1.7.10-2.5.2-gtnh-v12-default-on.jar`**，`10803520` B，
  SHA256 `8DCED6833A41E88CAC8966D4EA9BDCFEC0587273E6C9B0F1B0D5540C8BD72D73`
- **`GLStateTrackerTransformer` 现在默认注册**（作用域已收窄到 ReplayMod 自己），
  `GLErrorTransformer` 仍为 opt-in。
- **默认路径真机验证通过**：不带任何 `-D` 开关启动 → 主菜单就绪 → 进世界 → 进程存活 →
  无 crash → `注入/验证错误数 = 0` → `GLStateTrackerTransformer 注册 = True`。
- 附带确认（同一轮）：Galacticraft `ItemRendererMixin`、Angelica / gtnhlib / AppleCore /
  Backhand / Battlegear2 / Hodgepodge / MyCTMLib 的 `ItemRenderer` 系 mixin **全部注入成功**。

**开关语义**：
```
（默认）                                          # 注册 GLStateTrackerTransformer
-Dreplaymod.transformers=false                    # 完全关闭（回到 v6 行为）
-Dreplaymod.glerrors=true                         # 额外注册 GLErrorTransformer（调试）
```

**版本谱系（工作区保留，便于回溯）**：
`v2`（去 priority）→ `v4`（摘 `MixinGuiScreen`，**非必要**）→ `v6`（transformer 全关，物品栏可拖）
→ `v10`（opt-in + 具名静态）→ `v11`（作用域收窄）→ `v12`（transformer 默认开启）
→ `v13`–`v17`（回放中心入口搬迁，见 **12.12**）→ **`v18`（当前交付）**。
`mods-disabled\replaymod-…-v3.jar` 是 13:49 那版备份。

**备用查询**：`transform()` 里按类名跳过分支始终没打出日志（离线探针正常），怀疑 launchwrapper 传入的
`name` 与 `transformedName` 语义与预期不同。**作用域门已让该问题无害**（不依赖按名跳过也正确），
若将来要恢复「对 Minecraft 本体也追踪」再查。

---

### 12.12 回放中心入口搬进适配包（2026-10-10 夜，本次会话）—— 修复架构耦合

**问题（洛洛反馈）**：主界面**没有**回放中心的入口。

**根因**：那个入口从来不是 ReplayMod 适配包提供的，而是 **ServerReplay 客户端 jar 里的
`com.gtnh.serverreplay.mixin.MixinGuiCustom`**。而 ServerReplay 从 12.5 起被隔离到
`mods-disabled/`（单变量实验需要），**入口就跟着消失了**。这是架构错误 —— 入口是 ReplayMod 自己的 UI，
不该依赖服务端录制 mod。实测证据：无 ServerReplay 启动时，v12 的日志里
`MixinGuiCustom` / `main menu button` 全部 0 命中。

**修复**：把入口搬进 ReplayMod 适配包，与 ServerReplay 彻底解耦。

| 产物 | 说明 |
|---|---|
| `replaymod-compat/src/com/gtnh/replaycompat/mixin/MixinGuiCustomReplayEntry.java` | 新 mixin，注入 `lumien.custommainmenu.gui.GuiCustom` |
| `replaymod-compat/mixin-res/mixins.replaycompat.json` | 新 mixin 配置（`package: com.gtnh.replaycompat.mixin`，`client: [MixinGuiCustomReplayEntry]`） |
| `tools/inject-replaymod-clientmixins.ps1` | 新注入脚本：把 mixin 类 + 配置塞进 jar，并在 manifest 的 `MixinConfigs` 追加配置名（会先展开 72 字节续行、再重新折行） |

**三个关键技术点（都是踩出来的）**：

1. **必须同时在 `drawScreen` 里做幂等添加，不能只靠 `initGui`。**
   只注入 `func_73866_w_`(initGui) 的 `RETURN` 时实测**零输出**：`initGui` 可能在本类被 mixin
   之前就已调用过、且此后不再调用（v13 之所以偶然生效，是因为主菜单就绪后发生了一次资源重载，
   把 `initGui` 又触发了一遍）。`func_73863_a`(drawScreen) **每帧调用**，所以在它的 `HEAD`
   做幂等检查（按钮已存在就静默返回），按钮必然出现。**两个 hook 都保留。**
2. **所有 MCP 成员访问一律走反射**（见坑 29）：`screen.width/height`、`button.id`、
   `button.xPosition/yPosition`。
3. **i18n 有两层陷阱（都已修复）**：
   - **成员名在运行时被 remap**：`I18n.format` 是 MCP 拼写，**运行时叫 `func_135052_a`**。
     只匹配 `"format"` 永远找不到方法 —— 这正是按钮长期显示英文 `ReplayMod` 的原因
     （`zh_CN.lang` 里 `replaymod.gui.replayviewer=回放中心` 一直都在）。
   - **描述符是「两个」参数**：`format(String, Object...)` 编译为 `format(String, Object[])`。
   现在 MCP/SRG 两种拼写都试，并且**拒绝 i18n 在缺翻译时返回的 key 本身**（避免显示生 key）。
   **实测已生效**：日志 `... button added at (317, 8) label='回放中心' on a 427x240 screen ...`。
   这条与坑 29 是同一类错误（MCP 名在无 reobf 的运行时不存在），只是换成了**方法名**。

**验证（v18，自动化 + 洛洛目视双确认）**：
- ✅ 按钮出现在主菜单右上角：日志 `ReplayMod main menu button added at (317, 8) on a 427x240 screen (10 buttons total)`
  （`317 = width - 110`，与设计一致）
- ✅ 自动化点击命中并打开回放中心：日志 `ReplayMod main menu button clicked, opening the replay viewer`
- ✅ 与 ServerReplay 解耦：ServerReplay 仍在 `mods-disabled/`，入口照常工作
- ✅ 洛洛截图确认按钮渲染在主菜单右上角

**★ 诊断教训（浪费了好几轮，务必记住）**：
我一度用「`System.out.println` + 写文件探针」判断 hook 是否执行，**两个手段都骗了我**：
- 文件探针用**相对路径** `Paths.get("replaycompat-probe.log")`，文件从未出现在任何预期位置
  （JVM 的 `user.dir` 并不是实例目录）；
- 用 PowerShell 的 `Get-Content`/`Select-String` 搜日志时又受**编码**影响（它按 ANSI 读 UTF-8 文件，
  中文行会乱码、匹配失败），让我误判「日志里没有这一行」。

结果连续几轮把"功能正常"误判成"hook 没执行"。**可靠手段是 Mixin 的字节码导出**：

```powershell
pwsh -File tools\launch-gtnh.ps1 -ExtraJvmArgs '-Dmixin.debug.export=true'
# 导出到 <gameDir>\.mixin.out\class\<包路径>\<类>.class
javap -p -c "<gameDir>\.mixin.out\class\lumien\custommainmenu\gui\GuiCustom.class"
# 可直接看到 handler$…$replaycompat$addReplayModButton 是否被 func_73866_w_ 调用
```
本次正是靠它证明注入一直是对的（`func_73866_w_` 偏移 483、`func_73863_a` 偏移 5、
`func_146284_a` 偏移 3 都有 `invokespecial handler$…$replaycompat$…`）。

**当前交付（v19）**：`replaymod-1.7.10-2.5.2-gtnh-v19-zh.jar`，
size `10807424` B，SHA256 `8F7901455A9E66C6E558BE742B65D435DFE5975760056CC9F5C29AAA88CA37C8`。
= **v12（transformer 默认开启 + 作用域收窄）+ 回放中心入口 mixin + 中文按钮「回放中心」**。

（`v18` 是同一功能但按钮显示英文的上一版；两者只差 `localizedViewerLabel()`。）

**新增/改动的工具**：
- `tools/inject-replaymod-clientmixins.ps1`（新）：注入适配包自有客户端 mixin
- `tools/gtnh-window.ps1`（新）：窗口定位 / 截图 / **鼠标点击拖动** / 按键（已用于自动点击验证入口）
- `tools/gtnh-printwindow.ps1`（新）：`PrintWindow` 抓窗口内容（不受遮挡影响）
- `tools/launch-gtnh.ps1`（改）：新增 `-ExtraJvmArgs`，可透传
  `-Dmixin.debug.export=true` / `-Dreplaymod.transformers=false` 等开关

**★ 读日志的正确姿势（避免重蹈覆辙）**：游戏在写 `dsh-stdout.log` 时，
`[System.IO.File]::ReadAllText` 会因独占冲突直接抛异常；而 PowerShell 的 `Get-Content`/`Select-String`
默认按 **ANSI** 解码 UTF-8 文件，**中文行会乱码并导致匹配失败**（我因此误判过"日志里没有这行"）。
正确做法是**共享读取 + 显式 UTF-8**：
```powershell
$fs=[System.IO.File]::Open($log,[System.IO.FileMode]::Open,[System.IO.FileAccess]::Read,[System.IO.FileShare]::ReadWrite)
$sr=New-Object System.IO.StreamReader($fs,[System.Text.Encoding]::UTF8)
$text=$sr.ReadToEnd(); $sr.Close(); $fs.Close()
```

**下一步（洛洛本次还提了两项，均未动手）**：
1. **「保存录像」面板要能在主菜单用**：上游实现已摸清（`GuiSavingReplay` 用
   `core.getBackgroundProcesses()` 后台面板，再由 `presentRenameDialog` 挂"完成"按钮），
   我们的 `open()/close()` 与高版本**字节码级一致**；缺的是「主菜单上可交互」——
   面板要收鼠标事件需当前屏幕被 jGui 包装（`VanillaGuiScreen.wrap`），
   而 GTNH 主菜单是 CMM 的 `GuiCustom`（原版 `GuiScreen`）。
2. **恢复未完成录像**：上游是 `PacketListener.channelInactive` 里的
   `raw` 文件夹 + `.no_recover` 标记机制（ServerReplay 侧对应 `RecorderRecoverer`）。

高版本对照源码已缓存：`upstream-ref\rm-stable-GuiSavingReplay.java`、
`rm-stable-PacketListener.java`、`rm-stable-GuiRecordingControls.java`、
`rm-stable-ReplayModRecording.java`、`rm-stable-recording-GuiHandler.java`、`rm-stable-ReplayMod.java`
（见该目录 README 第 6 节）。

---

### 12.13 「进入回放崩溃」结案 + 又两个"藏在 ServerReplay 里"的修复（2026-10-10 夜）

**症状**：在回放中心点「加载」→ 崩溃（`crash-2026-10-10_15.19.59` / `15.20.26` / `15.29.18`）。

#### 第 1 层：Et Futurum NPE —— 根因是**类名大小写**

```
java.lang.NullPointerException: Cannot read field "field_73012_v" because "world" is null
    at ganymedes01.etfuturum.storage.EtfuturumPlayer.init(EtfuturumPlayer.java:48)
```

修复 `MixinEtFuturumPlayer` **本来就在 ServerReplay 里**（`mixins.serverreplay.json` 的 client 段），
ServerReplay 一被隔离它就失效 —— 和第 12.12 节的入口按钮是同一种架构耦合。

但搬到适配包后仍然 `@Mixin target ... was not found`。真正的坑是**大小写**：

| | 名字 |
|---|---|
| EtFuturum jar 内实际类名 | `ganymedes01/etfuturum/storage/**EtFuturumPlayer**.class`（**两个大写 F**） |
| 旧代码（ServerReplay 原版 + 我第一版）写的 | `ganymedes01.etfuturum.storage.**EtfuturumPlayer**`（小写 f） |

Mixin 按**字符串**匹配类名，大小写不符 → 永不应用。**所以 HANDOVER 2.7(3) 记的
"EtFuturum NPE 已修复"其实从未真正生效过**（之前能进回放另有原因）。
崩溃报告里打印的是小写拼写，这也是错误拼写看起来合理的原因。

**教训**：`@Mixin(targets = "...")` 的字符串必须与 jar 里的**真实类名逐字符一致**；
核对方法：`jar tf` / 解压 jar 看条目名，**不要相信崩溃报告或文档里的拼写**。

#### 第 2 层：world 未建立时 SpawnPlayer 崩溃 —— 新增 guard

修好 Et Futurum 后暴露出下一层同源问题：

```
java.lang.NullPointerException: Cannot read field "field_72995_K" because "p_i45324_1_" is null
    at net.minecraft.entity.player.EntityPlayer.<init>(EntityPlayer.java:144)    // world.isRemote
    at net.minecraft.client.entity.EntityOtherPlayerMP.<init>
    at net.minecraft.client.network.NetHandlerPlayClient.func_147237_a(NetHandlerPlayClient.java:464)
    at net.minecraft.network.play.server.S0CPacketSpawnPlayer.func_148833_a
    at com.replaymod.replay.gui.screen.GuiOpeningReplay.func_146269_k(GuiOpeningReplay.java:16)
```

`func_147237_a`（handleSpawnPlayer）用 `gameController.theWorld` 构造 `EntityOtherPlayerMP`，
而**回放加载阶段（GuiOpeningReplay）客户端 world 尚未建立** → `world.isRemote` NPE。

**为什么偏偏是我们的录像**：服务端 recorder 会在 `S01PacketJoinGame` 之后紧跟注入一个合成
`S0CPacketSpawnPlayer`（HANDOVER 2.7(1)，为的是回放里有玩家实体可挂摄像机）——
那个包正好在回放还没建好 world 时到达。

**修复**：新增 `MixinNetHandlerPlayClientSpawnGuard`，注入
`NetHandlerPlayClient.func_147237_a` 的 HEAD，反射读取 `clientWorldController`（SRG `field_147300_g`，
private，故用反射而非 `@Shadow`）；world 为 null 时 `ci.cancel()` 丢弃该包并打日志：

```
[replaycompat] replay loading: dropping S0CPacketSpawnPlayer (client world not ready yet)
```

**代价（明确记录）**：这一条 spawn 事件被丢弃，**合成出来的"自己"玩家实体可能不出现在回放里**。
这是刻意的取舍：另一种选择是每次打开我们录的录像都必崩。
若将来要两全，正确做法是**延迟重放**该包（缓存到 world 建立后），而不是丢弃。

#### 验证（v22）

- ✅ 回放中心可打开（洛洛截图：录像列表、加载/重命名/删除/编辑按钮齐全）
- ✅ **点击「加载」进入回放不再崩溃**：回放界面正常（时间轴、速度 1.0x、世界已渲染），进程存活、零 crash
- ✅ 适配包现在自带 3 个客户端 mixin，全部与 ServerReplay 解耦：
  `MixinGuiCustomReplayEntry`（入口）、`MixinEtFuturumPlayer`（回放实体构造）、
  `MixinNetHandlerPlayClientSpawnGuard`（回放加载期保护）

**当前交付（v22）**：`replaymod-1.7.10-2.5.2-gtnh-v22-spawnguard.jar`，
size `10809901` B，SHA256 `79BD35ADBBAE16B575960384AC0B7E70F5D127FC6490167191D1039D52AB101A`。

#### 洛洛新增的待办

- **回放中心滚轮滚动不流畅**（待查。注意：我们新增的入口 mixin 只注入 `GuiCustom`，
  与 `GuiReplayViewer` 无关，所以大概率是 jGui 1.7.10 自身 `GuiScrollable` 的滚动实现问题；
  但也不能完全排除 `MixinGuiScreen` 那条 `handleInput` 鼠标事件重放的副作用 —— 需要实测区分）。
- 「保存录像」面板要在主菜单可用（见 12.12 末尾）。
- 恢复未完成录像（见 12.12 末尾）。

---

### 12.14 「回放渲染有问题」结案（2026-10-10 夜，本次接手会话）—— 录像侧被字节级排除

**洛洛的原始描述**：「目前回放的渲染有问题」；进一步确认是「**人物下方未渲染，远处有渲染**」，
即画面大面积黑、只有轮廓或空洞。

#### 结论（一句话）

**录像内容无罪**（有字节级证据）。黑区出现在**回放刚打开、相机贴在人物实体上**的那一刻，
**相机离开人物后渲染正常** —— 这是回放**初始状态**的问题，不是持续缺陷。**洛洛裁定不修**，回主线 12.6。

#### 证据 1：录像侧被彻底排除（★ 本轮最重要的取证）

HANDOVER 12.6-⑥ 当年只比过**包数量**；本轮补上**逐字节对比** ——
同一次会话（13:19:00）的**客户端录像 vs 服务端录像**，按包 id 做 **MD5 多重集**比较：

| 包 | 含义 | 客户端 | 服务端 | 结论 |
|---|---|---|---|---|
| `0x26` | MapChunkBulk | 125 | 125 | **逐字节一致** |
| `0x23` | BlockChange | 29 | 29 | **逐字节一致** |
| `0x03` | TimeUpdate | 29 | 29 | **逐字节一致** |
| `0x3f` | CustomPayload（FML 握手 / 注册表数据） | 5346 | 5346 | **逐字节一致** |
| `0x12`/`0x17`/`0x25` | 实体运动类 | 1332/1246/441 | 96/23/0 | 已知漏录（12.6-⑤） |

→ **方块数据与 ReplayMod 官方客户端录制完全相同**。所以「看不到方块」**不是录制侧 / ServerReplay 的问题**，
12.6-⑥ 那句「疑似上游 #294」被本证据收窄为「**与录像无关**」。

补充：**#294 原文已核对**（`api.github.com/repos/ReplayMod/ReplayMod/issues/294`），其前因是
`Replay Mod cannot save the replay`（**录制保存失败**），与我们（保存成功、包完整）**不是同一个病**，
**不能直接套用其结论**。

#### 证据 2：`GLStateTrackerTransformer` 是加重因素，不是根因

严格单变量（同一份录像 `2026_10_10_15_28_37.mcpr`）：

| 版本 | 画面 |
|---|---|
| v22 默认（`GLStateTrackerTransformer` 默认注册） | 只有右下角一小块渲染 |
| `-Dreplaymod.transformers=false` | **恢复约一半**，仍有大片黑区 |

→ 关掉它**有帮助但不解决**。**顺带质疑 12.11 的取舍**：把作用域收窄到只看 ReplayMod 之后，
它的 GL 状态影子副本与真实状态不同步，可能确实有害。**留待以后复查**（优先级低）。

#### 证据 3：chunk 已加载，问题在渲染/剔除层

`F3` 调试屏实测（回放中）：

```
MultiplayerChunkCache: 825, 825          ← chunk 全在内存里
Chunk: 57 0 14   XYZ: 918.651 / 5.62000 / -225.600
Angelica 2.2.10
AngelicaShaders - Shadow Pass1
Shadow Maps: half plane = 192.0 meters @ 2048x2048
Shadow Culling Terrain: advanced Frustum Culling enabled
Distant Horizons  Build: 1.1
```

**F3+A（重载所有区块）没有恢复**。

#### 洛洛的裁定与复现

洛洛自己复现后给出关键一句：

> **「刚刚的回放界面默认是与人物重叠的，出来后是正常的渲染」**

即：**回放刚打开时相机贴在玩家实体上，此时近处黑；相机离开人物后渲染正常。**
洛洛决定**不修**，回到主线 12.6。

#### ★ 本轮的工具/环境教训（下次直接用，能省一整轮）

1. **截图必须 DPI-aware**：`pwsh` 进程默认 DPI-unaware，`GetClientRect` 返回**虚拟化尺寸**
   （实测 **569x320**，真值 **854x480**），于是 `CopyFromScreen` 截到的是**窗口标题栏**而不是游戏客户区。
   解法：P/Invoke `SetProcessDPIAware()`，并**在任何窗口操作之前**调用。
   `tools/gtnh-window.ps1` 已按此修复。
2. **`gtnh-window.ps1 -Find` 带 `exit 0`**：用 `&`（同进程）调用会把**调用者的脚本一起结束**。
   要客户区尺寸就用 `-Shot`（它本来就会打印 `client=WxH`）。
3. **已实测可用的客户区坐标**（窗口 854x480、DPI 150%）：
   - 主菜单「**回放中心**」按钮：`(730, 38)`
   - 回放中心**列表第一项**：`(450, 110)`
   - 「**加载**」按钮：`(269, 389)`
4. **`config/shaders.properties` 是 AngelicaShaders 的配置**（`enableShaders` / `shaderPack` /
   `maxShadowRenderDistance`），**每次进回放它的 mtime 都会刷新**。
   本轮曾把它改成 `enableShaders=false` 做实验，**已还原为 `true`**；
   原样备份在 `tools/shaders.properties.gtnh-orig.bak`。
5. `launch-gtnh.ps1` 用 `Start-Process` 启动，**不会 kill 任何 java 进程**（`run-test.ps1` 才会）；
   本机 `pwsh` **不在 PATH**，外部脚本用 `& <路径>`（注意第 2 条的 `exit` 陷阱）或内联 Win32 调用。

---

### 12.15 「回放中心滚轮滚动不流畅」结案（2026-10-10 夜，本次接手会话）—— lwjgl3ify 的滚轮量纲

**洛洛的原始反馈**：「回放中心滚轮滚动不流畅」，追问后的定性是「**幅度太小/几乎不动**」。

#### 根因（已被运行期日志实测证实）

jGui 的 `AbstractGuiScrollable.scroll` 这样换算滚轮增量：

```java
dWheel = (int) Math.copySign(Math.ceil(Math.abs(dWheel) / 4.0), dWheel);
scrollY(dWheel);   // offsetY 立即跳变，无插值
```

这个 **`/ 4` 是按 LWJGL 2 / Windows 语义校准的**：一格 = `WHEEL_DELTA = 120`，于是
`ceil(120/4) = 30` 逻辑像素 ≈ 一整行列表项。

而 **GTNH 客户端跑的是 lwjgl3ify 3.0.31（LWJGL 3 + SDL）**，SDLK 的滚轮增量经兼容层送到
`Mouse.getEventDWheel()` 时是 **`±1`** —— 我们的 patch 打出的实测日志：

```
[replaycompat] gui scroll raw dWheel=-1      （滚 5 格 → 5 条）
```

→ `ceil(1/4) = 1` 像素/格，**滚一格只动 1 像素**，肉眼几乎看不出来。

> **顺带推翻 12.6-② 的旧判读**：那条建议「摘掉 `MixinGuiScreen` 做单变量」。
> 实际上 `MixinGuiScreen` 只负责把鼠标事件重放给 jGui（见 `upstream-ref` 2.1），
> **与缩放无关**，方向是错的。正确定位靠的是读 jGui 源码 + 打印原始 `dWheel`。

#### 修复：`MixinGuiScrollableWheelScale`

在适配包内新增一个 mixin，**在 jGui 自己的算术之前把量纲归一化**（不动 jGui 的 `/4` 与 clamp）：

```java
@Mixin(targets = "com.replaymod.lib.de.johni0702.minecraft.gui.container.AbstractGuiScrollable")
public abstract class MixinGuiScrollableWheelScale {
    @ModifyVariable(method = "scroll", at = @At("HEAD"), argsOnly = true, ordinal = 0,
            remap = false, require = 0)
    private int replaycompat$normalizeWheelDelta(int dWheel) {
        if (dWheel == 0 || Math.abs(dWheel) > 3) return dWheel;   // 已是 LWJGL2 语义（120 的倍数）
        return dWheel * 120;
    }
}
```

**为什么这个 mixin 很安全（几个必须记住的点）**：

1. **目标类名必须用 jar 内 relocate 后的真实名**：jGui 被 ReplayMod 重定位到
   **`com.replaymod.lib.de.johni0702.…`**（不是 `de.johni0702.…`）。核对方式：`jar tf` / 解压看条目名。
2. **jGui 不参与 Forge remap**，所以 `scroll` 就是运行时的真名，注解里写 `"scroll"` + `remap = false` 即可。
3. **打在 `AbstractGuiScrollable` 上必然命中**：`GuiReplayList` 的继承链
   `AbstractGuiResourceLoadingList → AbstractGuiVerticalList → AbstractGuiScrollable`，
   **中间两层都没有覆写 `scroll`**（已用 `javap -p` 逐个确认）。
4. **它不触碰任何 MC 成员**，所以坑 29（无 reobf → MCP 名不存在 → 必须反射）**不适用**，
   既不用 `@Shadow` 也不用反射。`ModifyVariable` 支持 `argsOnly`（Mixin 0.8.7 实测有该属性）。
5. 只在 `|dWheel| <= 3` 时归一化，因此**若某天换回 LWJGL2 语义（±120）也不会被二次放大**。

#### 验证（定量，非人眼）

Win32 注入滚轮（`mouse_event(MOUSEEVENTF_WHEEL)`）向下滚 **5 格**，前后各截一张图：

| | 列表顶部 | 底部可见 |
|---|---|---|
| 滚动前 | `2026_10_10_15_28_37` (3:28 PM) | `14_25_11` |
| 滚 5 格后 | `2026_10_10_14_06_35` (2:06 PM) | `server-2026-10-10_13-19-00` |

**移动约 4.5 项**，正好等于 `5 × 120/4 = 150` 逻辑像素 ÷ 33 像素/项 → **与 ReplayMod 在 LWJGL2 下一致**。
日志同时确认注入生效：`AbstractGuiScrollable:localvar$zdl000$replaymod$replaycompat$normalizeWheelDelta`。

#### 当前交付（v23）

| 路径 | 大小 | SHA256 |
|---|---|---|
| **`replaymod-1.7.10-2.5.2-gtnh-v23-scrollfix.jar`** | `10811052` | `9AB4D63931174827FD888A7BD7EBCBD7D311D30A4239E66B7A9A4B75428C54AE` |

= **v22（三个客户端 mixin）+ 滚轮量纲修复**。适配包现在自带 **4 个客户端 mixin**：
`MixinGuiCustomReplayEntry`（入口）、`MixinEtFuturumPlayer`（回放实体构造）、
`MixinNetHandlerPlayClientSpawnGuard`（回放加载期保护）、**`MixinGuiScrollableWheelScale`（滚轮）**。

#### 复现构建流程（适配包是纯 javac 工程，**没有 reobfJar**）

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force   # 否则 .ps1 直接被拒
# 1) 编译 mixin 到 mixin-out
#    ★ 必须 -proc:none（否则 mixin 的注解处理器被触发、它要 gson，编译以 exit 3 失败，见坑 32）
#    ★ 只要 mixin 引用了 MC/Forge 类型，classpath 就得带上 recompiled_minecraft 与 replaymod jar（坑 33）
$compat   = '<workspace>\replaymod-compat'
$mcpJar   = '<workspace>\ServerReplay-1.7.10\build\rfg\recompiled_minecraft-1.7.10.jar'
$rmJar    = '<workspace>\tools\replaymod-1.7.10-2.5.2.jar'
$gsonJar  = '<libs>\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar'
$cp = "$compat\deps;$mcpJar;$rmJar;$gsonJar;$compat\libs\asm-9.9.jar;$compat\libs\asm-tree-9.9.jar;$compat\libs\asm-commons-9.9.jar"
# 直接编译**全部功能 mixin**（排除 3 个纯诊断探针），最省事且保证 6 个都是最新
Get-ChildItem "$compat\src\com\gtnh\replaycompat\mixin" -Filter '*.java' |
  Where-Object { $_.Name -notmatch 'Probe|EventHandler' } |
  ForEach-Object {
    & '<jdk8>\bin\javac.exe' -proc:none -encoding UTF-8 -nowarn `
       -cp $cp -d "$compat\mixin-out" $_.FullName
  }
# 2) 把 mixin 类 + mixins.replaycompat.json 注入基础 jar，并改写 manifest 的 MixinConfigs（幂等）
& '<workspace>\tools\inject-replaymod-clientmixins.ps1' `
   -Jar '<workspace>\replaymod-1.7.10-2.5.2-gtnh-v22-spawnguard.jar' `
   -Dest '<workspace>\replaymod-1.7.10-2.5.2-gtnh-v23-scrollfix.jar'
```

> 新增 mixin 时**必须同时**把类名加进 `replaymod-compat\mixin-res\mixins.replaycompat.json` 的 `client` 段，
> 否则类被塞进 jar 也不会被应用（这是最容易漏的一步）。

---

### 12.16 「保存录像面板在主菜单不可用」根因定位 + 补丁（2026-10-10 夜，本次接手会话）—— ✅ **已实测并结案（见 12.18 / 12.19）**

**洛洛的诉求**：「保存录像」面板要在**主菜单**可用（12.6-③ / 12.12 末尾）。

#### 根因（★ 字节码级，本轮新发现，推翻了 12.12 末尾"待验证点"的含糊说法）

`GuiBackgroundProcesses.onGuiInit` 在 **1.7.10 的签名与通用源码完全不同**——
通用源码是 `onGuiInit(Screen)` + jGui 的 `InitScreenCallback`，而 **1.7.10 编译版收的是 Forge 事件**：

```
public void onGuiInit(net.minecraftforge.client.event.GuiScreenEvent$InitGuiEvent$Post);
```

它的字节码（`javap -c`，偏移为证）：

```
 0: aload_1
 1: invokestatic  MCVer.getGui(GuiScreenEvent)     // guiScreen = MCVer.getGui(event)
 4: astore_2
 5: aload_2
 6: invokestatic  MCVer.getMinecraft()
 9: getfield      Minecraft.field_71462_r          // mc.currentScreen
12: if_acmpeq     16                               // 相等才继续
15: return                                         ★守卫：不等就直接 return
16: aload_2
17: invokestatic  VanillaGuiScreen.wrap(GuiScreen)
…  getfield GuiBackgroundProcesses.panel → addElements
```

**GTNH 上这两个值永远不相等**：CMM 在 `GuiOpenEvent` 里把真正显示的屏幕换成缓存的
`GuiCustom`（所以 `mc.currentScreen == GuiCustom`），但塞进 `InitGuiEvent.gui` 的是那个**壳**
`GuiFakeMain`（HANDOVER 2.5 / 坑 17 已实测）。于是：

**守卫 return → `VanillaGuiScreen.wrap` 与 `panel` 挂载这两步（让面板"存在"的唯一两步）从未执行**
→ 主菜单上**根本没有这个面板**（所以不是"能看见但点不动"）。

#### 补丁：`MixinGuiBackgroundProcessesMainMenu`

重定向**唯一那一处** `MCVer.getGui(event)` 调用，让它返回**客户端真正在显示的** `mc.currentScreen`：

```java
@Mixin(targets = "com.replaymod.core.gui.GuiBackgroundProcesses")
public abstract class MixinGuiBackgroundProcessesMainMenu {
    @Redirect(method = "onGuiInit", at = @At(value = "INVOKE",
            target = "Lcom/replaymod/core/versions/MCVer;getGui(Lnet/minecraftforge/client/event/GuiScreenEvent;)Lnet/minecraft/client/gui/GuiScreen;"),
            remap = false, require = 0)
    private GuiScreen replaycompat$hostPanelOnTheScreenWeActuallyShow(GuiScreenEvent event) {
        // 反射读 mc.currentScreen（MCP/SRG 双拼写），失败则回退 MCVer.getGui(event)（退化为"面板缺失"而非崩溃）
    }
}
```

**为什么这么写（几条硬约束）**：

1. **不改 `event.gui` 字段，而改"读取"** —— 该字段是 **`public final`**（`javap` 确认），
   客户端跑 **JDK 25**，反射写 final 字段已不可靠。重定向读取完全绕开字段写。
2. **`aload_2` 被 `wrap` 复用**：守卫之后 `wrap(guiScreen)` 用的还是同一个局部变量，
   所以重定向这一处读取后，**整个方法内部是自洽的**（不需要第二处补丁）。
3. **目标类/方法/`MCVer` 都是 ReplayMod 自己的，不做 remap** → 注解里写真实名 + `remap = false`，
   **不需要 `@Shadow`**；唯一反射是 `Minecraft.getMinecraft()/currentScreen`（MCP + SRG 双拼写）。
4. `require = 0` + 异常兜底：补丁失效时退化为原行为。

#### ★ 编译这个 mixin 的两个新坑（本轮踩到，务必记住）

1. **需要 MC + Forge 的编译 classpath**（因为要引用 `GuiScreen` / `GuiScreenEvent`）：
   用 **`<workspace>\ServerReplay-1.7.10\build\rfg\recompiled_minecraft-1.7.10.jar`**
   （实测含 **378 个 `net/minecraftforge` 条目**，即 Forge 类；`srg_merged_minecraft.jar` **不含** Forge，别用错），
   外加 `<workspace>\tools\replaymod-1.7.10-2.5.2.jar`（提供 `MCVer`）。
2. **必须加 `-proc:none`**：一旦 classpath 上出现 mixin 的注解处理器，`javac` 会自动运行它，
   而它要 gson → 报 `NoClassDefFoundError: com/google/gson/JsonParseException`，
   整个编译以 **exit 3** 失败。我们不需要 AP（refmap 用的是 ReplayMod 自带的）。

#### 当前交付（v24）

| 路径 | 大小 | SHA256 |
|---|---|---|
| **`replaymod-1.7.10-2.5.2-gtnh-v24-bgpanel.jar`** | `10813292` | `45F13493615F5090C02C308FC130AF412C087941D6A1DA28E0B0BC0A58B2950F` |

= v23（滚轮修复）+ 保存面板宿主重定向。适配包现在自带 **5 个客户端 mixin**：
入口 / EtFuturum / SpawnGuard / 滚轮 / **后台面板宿主**。已部署进 `mods/`。

#### ✅ 实测结果（已完成，2026-10-10 夜）

**① 补丁确实生效** —— 日志实测：

```
[replaycompat] background panel host: lumien.custommainmenu.gui.GuiFakeMain -> lumien.custommainmenu.gui.GuiCustom
[replaycompat] background panel host: ...AbstractGuiScreen$MinecraftGuiScreen -> lumien.custommainmenu.gui.GuiCustom
[replaycompat] background panel host: net.minecraft.client.gui.GuiSelectWorld -> net.minecraft.client.gui.GuiSelectWorld
```

→ **HANDOVER 2.5 的判读被实测完全证实**（`event.gui` 真是 `GuiFakeMain`，`mc.currentScreen` 真是 `GuiCustom`），
重定向确实发生，`VanillaGuiScreen.wrap(GuiCustom)` 与 `panel` 挂载随之执行。
> ⚠️ 诊断提示：我的日志有 `replaycompat$logged < 6` 的限流，**退出世界那次（真正出面板的那次）没有打日志**，
> 我因此一度误判"补丁没生效"。如果下次只看最后几行日志，会重蹈这个覆辙 ——
> **要么去掉限流，要么按时间点搜全文**。

**② 面板现在能在主菜单显示** —— 退出世界后主菜单右上角出现完整面板：
标题「保存录像文件中...」+ 文件名输入框 + 「完成」按钮（截图 `tools/shots/16-mainmenu-panel.png`）。
录像此时落在 `replay_recordings\recording\<时间戳>.mcpr`（尚未被 `apply()` 移到 `replay_recordings\` 根目录）。

**③ ★ 但点击仍然无效（第二层问题，未解决）**：
- 点「完成」`(650,117)` → 面板毫无变化（`17-after-done.png` 与 `16-mainmenu-panel.png` 几乎一致），
  `replay_recordings` 里没有新 `.mcpr`；
- 点文件名输入框 `(645,73)` 后敲 3 个 `A` → 输入框文本不变（`18-input-test.png`）。

→ **是「jGui 输入事件根本没到达面板」，不是「按钮回调失败」**。
已排除：`GuiCustom` **没有覆写**任何输入方法（`javap` 确认无 `handleInput`/`handleMouseInput`/`mouseClicked`），
所以 `MixinGuiScreen` 的事件桥理论上应该通。

**下一层待查（按嫌疑排序）**：
1. **重复挂载（最可疑）**：每次 `InitGuiEvent.Post` 都会 `addElements(null, panel)` 一次
   （日志显示主菜单每次打开都会走一遍 `onGuiInit`），**同一个 `panel` 被反复加入 `VanillaGuiScreen`**，
   很可能导致命中判定/输入链异常。验证方式：在重定向前先判重或先 `removeElements(panel)`。
2. `MixinGuiScreen.handleJGuiInput` 是否真的在主菜单上被调用（`GuiScreen.handleInput` 是否被别的 mod 取消/绕过）；
3. `VanillaGuiScreen.EventHandler.onMouseInput` 是否收到 `MouseInputEvent`；
4. `getSuperMcGui()`（= `AbstractGuiScreen$MinecraftGuiScreen`）的 `handleMouseInput()` 是否把事件分发进 jGui 树。

**备选方案（若 jGui 这条路走不通）**：绕开 jGui 输入，把「完成保存」做成 **MC 原生按钮**加到
`GuiCustom.getButtonList()`（做法与 `MixinGuiCustomReplayEntry` 的入口按钮完全一致，那条路已实测可用）。

---

### 12.17 保存面板输入链：逐层拆解到底（2026-10-10 夜，本次接手会话）—— ✅ **已结案（见 12.18）**

12.16 把「面板挂载」修好之后暴露了「点不动」。本节是**逐层定位的全过程**，
每层都有**运行期证据**，不要重复走。

#### 已确认的事实链（按因果顺序）

| # | 环节 | 证据 | 结论 |
|---|---|---|---|
| 1 | 面板**挂载** | `background panel host: GuiFakeMain -> GuiCustom` | ✅ 已修（12.16 的 `@Redirect`） |
| 2 | 面板**重复挂载** | 同一次 init 出现 2 条 host 日志（`GuiFakeMain` + jGui 的 `MinecraftGuiScreen`）；加判重后打 `skipping duplicate panel mount (panel already 1x on …)` | ✅ 已修（`@Inject` HEAD + 反射查 `getChildren()`） |
| 3 | 鼠标事件**是否被 post** | `probe: MouseInputEvent constructed/posted #1..#12` | ✅ **被 post**（`MixinGuiScreen` 生效） |
| 4 | `EventHandler` 是否活着 | `probe: EventHandler.onGuiRender reached #1..#3` | ✅ **活着**（渲染走它） |
| 5 | `EventHandler.onMouseInput` 是否收到 | 探针 **0 命中**（全文搜过，非限流问题——限流是 15 次） | ❌ **收不到** ← 断点在此 |
| 6 | 原因 | `onMouseInput`/`onKeyboardInput` 是 `@SubscribeEvent(priority = EventPriority.LOWEST)`；而 `EventRegistrations.register()` 才做 `MinecraftForge.EVENT_BUS.register(this)`，`VanillaGuiScreen.wrap()` **仅在新建 wrapper 时**调 `register()`（缓存命中不重注册），`EventHandler.onGuiClosed` 又对**每个** `GuiOpenEvent` **无条件** `unregister()` | 缓存复用的 wrapper 处于「已注销」状态；LOWEST 优先级又最先被 `receiveCanceled=false` 跳过 |
| 7 | 绕过方案 | 新增 `MixinGuiScreenJGuiInputBridge`：在 `GuiScreen.func_146274_d`(handleMouseInput) 的 HEAD，若当前屏幕不是 jGui 屏幕则直接把事件转给 jGui 树（`VanillaGuiScreen.wrap(cur)` → 反射 `getSuperMcGui()` → `handleMouseInput()`）。递归由「只桥接 `this == mc.currentScreen`」+「跳过 `MinecraftGuiScreen`」双重保证 | ✅ 日志 `input bridge: forwarded mouse event to jGui for lumien.custommainmenu.gui.GuiCustom`；**且截图可见输入框焦点真的变了**（白底→灰底） |
| 8 | **最后一层** | 加 `MixinAbstractGuiClickableProbe` 后实测：`click probe #6 class=…GuiButton pos=(325,58) lastSize=150x20 enabled=true` → **返回 `true`**（即 `onClick()` 执行） | ✅ **已解决**，见下方「根因与修复」 |

> 附带发现：录像保存后并不在 `replay_recordings\` 根目录，而是留在
> **`replay_recordings\recording\<时间戳>.mcpr`**（`apply()` 未执行就不会移走）。
> 之前几轮的录像后来出现在根目录，是因为**下次启动时被 ReplayMod 自行恢复**——
> 所以「根目录有文件」**不能**当作「`apply()` 成功过」的证据。

#### 当前交付（v27）

| 路径 | 大小 | SHA256 |
|---|---|---|
| **`replaymod-1.7.10-2.5.2-gtnh-v27-inputbridge.jar`** | `10819065` | `ED4652B9F380206DCBDE8DFE64668602D968B1CAF218D4F32BDAE97C8DAC8443` |

适配包现在自带 **8 个客户端 mixin**（含 3 个**纯诊断探针**，见下）。
已部署进 `mods/`。

#### ⚠️ 交付前务必做的事

**v27 里带着 3 个诊断探针，正式版应移除**（它们只打日志、无副作用，但会刷日志）：
`MixinMouseInputEventProbe`、`MixinVanillaGuiScreenEventHandler`，
以及 `MixinGuiBackgroundProcessesMainMenu` / `MixinGuiScreenJGuiInputBridge` 里的限流 `println`。
移除方式：从 `mixins.replaycompat.json` 的 `client` 段删掉探针类名并重新注入即可（**探针类可以留在源码里**）。

#### 第 8 层的精确链路（2026-10-10 夜，字节码级，已排除若干嫌疑）

点击路径（全部 `javap -c` 逐步确认，**与鼠标释放无关**）：

```
GuiScreen.func_146274_d()                      // 我们的桥接在这里转发
 └─ AbstractGuiScreen$MinecraftGuiScreen.func_146274_d()
     ├─ super.func_146274_d()                  // 原版 GuiScreen.handleMouseInput
     │   └─ 读 Mouse 事件 → this.mouseClicked(i, j, k)
     │       └─ func_73864_a → this$0.forEach(Clickable.class).mouseClick(new Point(i, j), k)
     │           ├─ forEach 返回 **动态代理**（Proxy.newProxyInstance + 逐层 list），
     │           │  会把调用广播到所有层的匹配元件  → **不是"只取一个"的 bug，已排除**
     │           └─ AbstractGuiClickable.mouseClick(pos, button)
     │               ├─ point = new Point(pos); container.convertFor(this, point)   // 屏幕→元件内坐标
     │               ├─ isMouseHovering(point)  →  0 < x < getLastSize().width && 0 < y < getLastSize().height
     │               ├─ isEnabled()
     │               └─ onClick()               // ★ 点击**立即**触发，**不需要 mouseRelease**
     └─ if (Mouse.hasWheel() && getEventDWheel() != 0)
            this$0.forEach(Scrollable.class).scroll(MouseUtils.getMousePos(), wheel)
```

**已排除的嫌疑**：
- `func_146286_b`(mouseReleased) / `func_146273_a`(mouseClickMove) 分发的接口是 **`Draggable`**，
  而 `AbstractGuiClickable` 只实现 `Clickable` —— 起初以为这是 bug，**但其实无关**：
  按钮的 `onClick()` 在 `mouseClick` 里就发生了，不依赖 release。
  （`VanillaGuiScreen` 自己实现 `Draggable`，其 `mouseRelease` 只是把 `handled` 置回 false。）
- `forEach` 的代理广播机制：正常。

**因此剩余嫌疑只剩两处，都可用一个探针一次定位**：
1. `AbstractGuiClickable.mouseClick` 是否被调用（即 `mouseClicked(i,j,k)` 有没有走到）；
2. 若被调用：`convertFor` 之后的坐标是否落在 `getLastSize()` 内（**注意 `getLastSize()` 来自元件最后一次
   `layout`；`GuiBackgroundProcesses` 是先把 `panel` 加到 `VanillaGuiScreen` 的 layout 里、
   之后才由 `addProcess` 往 `panel` 里塞子元件的**，子元件可能在命中判定时尺寸仍是 0 ——
   **这条最可疑**）。

**建议探针（下一步直接做这个）**：`@Mixin(targets = "…gui.element.AbstractGuiClickable")`，
在 `mouseClick` 的 HEAD/RETURN 打印：入参坐标、`convertFor` 后的坐标、
`getLastSize()` 的宽高、`isEnabled()`、返回值。一次即可判定是「没收到点击」还是「命中了但尺寸为 0」。

#### ★ 第 8 层的根因与修复（**已结案**，是我自己的一个「坑 29 变体」）

桥接类 `MixinGuiScreenJGuiInputBridge` 原本这样调用 jGui 的鼠标泵：

```java
invokeNoArg(jGuiScreen, "handleMouseInput");     // ← 用 MCP 名查找
```

但 `AbstractGuiScreen$MinecraftGuiScreen` 继承自 MC 的 `GuiScreen`，**它的鼠标泵在运行时叫 SRG 名
`func_146274_d`**，`handleMouseInput` 这个名字运行时**根本不存在** → `getDeclaredMethod` 抛
`NoSuchMethodException` → 返回 `null` → **整个桥接是一个静默 no-op**。

**最坑的是我的日志**：`println` 写在调用**之后且不检查返回值**，所以照常打印
`input bridge: forwarded mouse event to jGui for …`，**看起来完全成功**。
（后来又加了 `pump=` 判断，但 `func_146274_d()` 是 **void**，反射调用永远返回 `null`，
于是误报成 `NOT_FOUND` —— **日志在这件事上连骗我两次**。）

**修复**：`invokeNoArg` 改为接受多个候选拼写，调用处传 `"handleMouseInput", "func_146274_d"`。

**验证（v29，完整链路一次通过）**：

```
click probe #5 class=…GuiButton pos=(325,58) lastSize=20x20  -> false   ← 文件名旁的 X 按钮
click probe #6 class=…GuiButton pos=(325,58) lastSize=150x20 -> true    ← ★「完成」按钮，onClick 执行
```

同时目录实测：

| | `replay_recordings` 内容 |
|---|---|
| 点击前 | `recording\<时间戳>.mcpr`（暂存） |
| 点击后 | `2026_10_10_16_56_54….mcpr`（**已移到根目录** = `apply()` 真的执行了） |

截图 `tools/shots/26-after-done-v29.png`：**面板自行消失**，主菜单恢复干净。
→ **「保存录像」面板在主菜单上完全可用（显示 + 点击 + 重命名 + 完成）。**

#### 当时的交付（v30-clean，**已被 12.19 的 release 版取代**）

| 路径 | 大小 | SHA256 |
|---|---|---|
| `replaymod-1.7.10-2.5.2-gtnh-v30-clean.jar`（🗄️ 历史） | `10821168` | `01CF51D5839435C1DBC07967BCC7ECA4B1AACD87E520A2D3B370B292F6CF9BE7` |

= v29 的全部修复 **+ 摘除 3 个纯诊断探针**（**但 jar 内仍残留那 3 个 `.class`**，直到 12.19 才彻底清掉）。
适配包现自带 **6 个客户端 mixin**（全部为功能补丁）：

| mixin | 作用 |
|---|---|
| `MixinGuiCustomReplayEntry` | 主菜单「回放中心」中文字按钮（12.12） |
| `MixinEtFuturumPlayer` | 回放实体构造期 NPE 保护（12.13，**类名大小写**） |
| `MixinNetHandlerPlayClientSpawnGuard` | 回放加载期 `SpawnPlayer` 保护（12.13） |
| `MixinGuiScrollableWheelScale` | 滚轮量纲修复（12.15） |
| `MixinGuiBackgroundProcessesMainMenu` | 保存面板宿主重定向 + 去重挂载（12.16/12.17） |
| `MixinGuiScreenJGuiInputBridge` | 非 jGui 屏幕（CMM 主菜单）的鼠标输入桥接（12.17） |

> 探针源码仍保留在 `replaymod-compat\src\…\mixin\` 下（`MixinMouseInputEventProbe`、
> `MixinVanillaGuiScreenEventHandler`、`MixinAbstractGuiClickableProbe`），
> **只是不在 `mixins.replaycompat.json` 的 `client` 段里**，需要时把类名加回去重新注入即可复现全部取证。

---

### 12.18 「保存录像面板」结案（2026-10-10 夜）—— 一行 MCP/SRG 拼写

**一句话**：面板挂载（12.16）→ 去重（12.17）→ 输入桥接（12.17），
最后卡住的是桥接里 **`handleMouseInput` vs `func_146274_d`** 的拼写——
修掉后「完成」按钮一次通过，文件归位、面板关闭。

**★ 这次最值得记住的教训（比 bug 本身重要）**：
**「日志打了」不等于「操作成功」。** 我连续两次被自己的日志误导：
1. 日志写在调用之后、不看返回值 → 失败也照打"forwarded"；
2. 改成看返回值后又忘了 **void 方法反射调用恒返回 `null`** → 成功也报 `NOT_FOUND`。
**结论：诊断日志必须打印「可判定的证据」**（真实返回值 / 副作用状态 / 异常），
**或者干脆用探针在被调用方打点**——本轮最终正是靠「在被调用方 `AbstractGuiClickable.mouseClick`
打点并打印其返回值」才一眼定案的。

#### 遗留的小尾巴（低优先，下次顺手做）

> ✅ **本节这条小尾巴已由 12.19 处理完毕**（成功路径的 `println` 已删、发布版已从 v22 干净重建）。
> 以下内容保留供追溯：

v30 的 6 个功能 mixin 里，**还有两处带限流的诊断 `println`**（各最多打 5~6 次，对玩家无害）：
- `MixinGuiBackgroundProcessesMainMenu`：`background panel host: …` / `skipping duplicate panel mount …`
- `MixinGuiScreenJGuiInputBridge`：`input bridge: forwarded mouse event … (pump=…)`

清理方式：删掉这两处 `println` → 用 12.15 末尾那条 `javac`（**记得 `-proc:none` 和两个 classpath**）
重新编译这两个类 → `inject-replaymod-clientmixins.ps1` 重新注入。
> 注意：**游戏运行时 `mods\` 里的 jar 被锁住，无法覆盖**，清理前先确认游戏已退出。

**另外**：`MixinGuiScreenJGuiInputBridge` 里 `pump=` 那段判断本身是**错的**（void 反射恒返回 null），
清理时一并把那段日志整个删掉，别留一个永远显示 `NOT_FOUND` 的误导性输出。

---

### 12.19 对外发布版准备（2026-10-10 夜）—— 干净重建 + 打包（**12.18 那条小尾巴已在本节处理完毕**）

**动机**：要把包发给其他人，jar 里不该留调试痕迹。

**v30-clean 的两个不干净之处**：
1. **jar 里残留 3 个探针 `.class`** —— 它们**不在 `mixins.replaycompat.json` 的 `client` 段**里，
   运行时**不会被 Mixin 处理**（零副作用），但别人解包看到会疑惑；
2. **功能 mixin 会往对方日志里打诊断行**（`background panel host …` /
   `skipping duplicate panel mount …` / `input bridge: forwarded …`）。

**★★ 关键陷阱：`inject-replaymod-clientmixins.ps1` 是「添加 / 覆盖」式注入，不会删除旧条目。**
所以「把探针类名从配置里摘掉再重新注入」**并不能**把探针 `.class` 从 jar 里去掉。
**想彻底干净，必须从更早的、不含探针的 jar 重建** —— 本次选 **v22**
（它只含 3 个客户端 mixin，jar 内无探针 class）。

**本次做法**：
1. 从 `MixinGuiBackgroundProcessesMainMenu` / `MixinGuiScreenJGuiInputBridge` 里**删掉成功路径的 `println`**
   （**保留 catch 里的失败日志** —— 那对用户排障有用）；
2. 用 12.15 末尾那条 `javac` 重新编译 6 个功能 mixin（**必须 `-proc:none`**，见坑 32）；
3. `Copy-Item v22 → …-v30-release.jar`，再跑 `inject-replaymod-clientmixins.ps1` 注入 6 个干净 class；
4. 校验 jar 内 `replaycompat/mixin/` **恰好 6 个 class、无任何 Probe**；
5. **重新端到端实测**（改过代码就必须重测）：启动 → 进世界 → 退世界 → 保存面板 → 点「完成」→ 文件归位；零 crash。

**产物**：

| 项 | 值 |
|---|---|
| jar | `replaymod-1.7.10-2.5.2-gtnh-v30-release.jar`，`10816334` B，SHA256 `345EDE0D453FA46F4EC480E68017BB5A7F54AC0A59C79C66FE0F9DD50DDDE978` |
| 发布整包 | `release\GTNH-ReplayMod-v30\`（jar + `安装说明.md` + `SHA256.txt`）与 `GTNH-ReplayMod-v30.zip`（`9712353` B） |
| 日志洁净度 | ✅ 只剩一行 `[replaycompat] ReplayMod main menu button added …`（每次主菜单重建打一次，无害，且能证明入口生效） |

**★ 当前源码 / class 状态（下个会话别被字节数搞混）**：
- `mixin-out` 里的 class **已经是「去日志版」**：`MixinGuiBackgroundProcessesMainMenu` **5590** B、
  `MixinGuiScreenJGuiInputBridge` **4112** B（v30-clean 时代分别是 6058 / 4406，带日志）。
  所以**照 12.15 的命令重编译 + 注入，得到的就是 release 版行为**（不带诊断日志）。
- **探针源码仍在** `replaymod-compat\src\…\mixin\`（`MixinMouseInputEventProbe` /
  `MixinVanillaGuiScreenEventHandler` / `MixinAbstractGuiClickableProbe`）。
  需要复现取证时，把类名加回 `mixins.replaycompat.json` 的 `client` 段再注入即可。

**部署现状**：`mods\replaymod-1.7.10-2.5.2-gtnh.jar` = **release 版**
（`10816334` B / `345EDE0D…`），已实测。

#### 验证步骤（**已按 release 版的实际行为改写**）

1. 启动游戏 → 进任意单人世界（ReplayMod 自动开始录制，`config/replaymod.cfg` 里 `autoStartRecording=true`）；
2. 待 20~30 秒后 **退出世界**（集成服务端断开 → `PacketListener.channelInactive` → `GuiSavingReplay.open()`）；
3. **回到主菜单**，右上角应出现保存面板（标题「保存录像文件中…」+ 文件名输入框 + 「完成」按钮）；
4. **点「完成」** → 面板消失，且暂存文件从 `replay_recordings\recording\<时间戳>.mcpr`
   **被移到** `replay_recordings\<时间戳>.mcpr` —— **这一步才是真正的判据**（说明 `apply()` 确实执行了）；
5. **关于日志（重要，别按旧判据误判）**：release 版按设计**只打**这一行（每次主菜单重建打一次）：
   ```
   [replaycompat] ReplayMod main menu button added at (…) label='回放中心' on a …x… screen
   ```
   **看不到 `background panel host …` / `input bridge …` 是正常的** —— 那些诊断 `println`
   **已在 12.19 随发布版一起删除**，**不代表补丁失效**。
   若连 `main menu button added` 都没有，才是 mixin 没注入，去查 `-Dmixin.debug.export=true` 的字节码导出；
6. （仅作回溯）若面板出现但点不动：那是 jGui 输入路由问题，见 12.17 / 12.18。

---

### 12.20 「恢复未完成录像」结案（2026-10-10 夜，本次接手会话）—— ✅ **无需修复：客户端机制本来就完整可用**

**一句话**：12.6-④ 一直被当成"未动手的功能缺口"，实测证明 **1.7.10 客户端早就内置了全套恢复机制**
（`RestoreReplayGui` + 中文文案 + `raw` / `.no_recover` / `.mcpr.tmp`），在 GTNH 上
**显示 → 点击 → 恢复执行**三段**全部正常**。本轮**没有改动任何代码**，
只用**零存档风险**的人造夹具取证（只启动到主菜单，不进世界、不碰存档）。

#### 机制全貌（字节码级确认）

恢复逻辑在 **`ReplayMod.lambda$initClient$2()`**（= `initClient()` 里 `runPostStartup(...)` 的那个 lambda），
启动时按顺序扫（`javap -p -c` 逐条核对）：

| # | 目标目录 | 动作 |
|---|---|---|
| 1 | `replay_recordings\raw\` | 删掉**创建满 21 天**的文件 —— ★ **`raw\` 是故意保留的调试备份，不是待恢复物**（当前那 2 个文件就是正常产物） |
| 2 | `replay_recordings\recording\` | 把里面所有东西**静默移动**到 `replay_recordings\` 根（目标已存在则跳过）→ 这正是 12.17 观察到的"录像自己跑到根目录" |
| 3 | `.replay_cache\` | 删 7 天未改、或对应录像已不存在的 cache 目录 |
| 4 | `replay_recordings\` 根 | 删 2 天前的 `*.mcpr.del` 目录 |
| 5 | `replay_recordings\` 根 | **`*.mcpr.tmp` 目录**：旁边有 `<name>.mcpr.no_recover` → 删 tmp + 删标记；**否则 `new RestoreReplayGui(core, GuiScreen.wrap(mc.field_71462_r), <name>.mcpr).display()`** |
| 6 | `replay_recordings\` 根 | 删遗留的 `*.no_recover` |

`RestoreReplayGui`（jar 内 `com/replaymod/core/gui/RestoreReplayGui.class`，1.7.10 版**存在且完整**）：
- 三行中文文案 `replaymod.gui.restorereplay1/2/3`（**官方 `zh_CN.lang` 里本来就有**，不是我们补的）；
- 「是」→ `recoverInBackground()` → 后台线程 `tryRecover()`；
- 「否」→ 把 `<name>.mcpr.tmp` 改名成 `<name>.mcpr.del`（交给第 4 步清理）。

`tryRecover()` 的实质动作：

```java
replayFile = ReplayMod.instance.files.open(file);   // 会在 .replay_cache\ 下建 <name>%2Emcpr\
replayFile.save();                                  // 提交 tmp 里未提交的 changed
metaData = replayFile.getMetaData();
if (metaData != null && metaData.getDuration() == 0) {
    // 逐包读 recording.tmcpr（末尾不完整包丢弃）→ 用最后一个完整包的时间戳重建 duration
    metaData.setDuration(...);
    metaData.setGenerator(旧 generator + "(+ ReplayMod Replay Recovery)");
    写回 metaData.json
}
replayFile.save(); replayFile.close();
```

失败时走 `Utils.error(...)` → **弹 Minecraft Crash Report 界面**（上游行为）。

#### 本轮实验（零存档风险）

夹具 = 在 `replay_recordings\` 放一个合法 mcpr + 一个同名 `<name>.mcpr.tmp\` 目录（模拟崩溃残留），
并把 `metaData.json` 的 `duration` 改成 `0`（强制走重写分支）。

| 轮 | 夹具 | 结果 |
|---|---|---|
| 1 | `tmp\` 只有 `changed/removed/source` 三个**空目录**，`duration` = 28807 | 恢复界面**弹出** ✅、点「是」→ 界面回主菜单 ✅、`tmp\` 被清理 ✅、`.replay_cache\<name>%2Emcpr\` 被新建 ✅（`duration ≠ 0`，所以没走重写分支） |
| 2 | `duration` 改成 **0** | 恢复界面**弹出** ✅、点「是」→ `tryRecover` **真的进了 `write()`**，但抛 `FileNotFoundException: …\recoverytest.mcpr.tmp\source (拒绝访问。)` ❌ ← **是我把 `source` 建成了目录**（见坑 38） |
| 3 | 删掉被误建的 `source` **目录** | ★ **完整成功**：`duration` 0 → **28807**、`generator` → `ReplayMod v1.7.10-2.5.2(+ ReplayMod Replay Recovery)`、`tmp\` 清理、cache 新建、**零 crash** |

**完整性旁证**：重建出的 `duration` = **28807**，与我改坏前的原值**完全一致**
→ 包流重写没有破坏数据。

**取证素材**：`tools\shots\30~38-*.png`（恢复界面 / 点击前后 / 第二轮 crash 界面 / 第三轮结果）、
`crash-reports\crash-2026-10-10_17.41.57-client.txt`（第二轮夹具错误的产物）。

#### ★ 结论

**12.6-④ 不需要任何代码改动。** 客户端「未完成录像恢复」在 GTNH 2.9.0-beta-3 + `v30-release` 上：

- 恢复提示**能弹出**（中文完整、按钮可渲染）；
- 「是」**点了就生效**（脚本 `mouse_event` 与真鼠标均可）；
- 恢复**真的执行**（重建时长 + 打恢复标记 + 清理残留）；
- **无 crash**。

#### 遗留观察（都不是缺陷，供以后参考）

1. **`tryRecover` 失败时玩家会看到 Crash Report 界面**（`Utils.error`）。上游行为；
   真实崩溃残留不会触发（第二轮那次纯属我夹具的 `source` 结构错）。
2. **`raw\` 会一直攒文件**（保留 21 天）。这是**设计如此**，不用清。
3. 本轮唯一的新 crash report 是 **`crash-2026-10-10_17.41.57-client.txt`**，
   **是第二轮夹具错误造成的测试产物**，不是产品缺陷（原文见下）。
4. **服务端侧仍缺**：ServerReplay 的 `recoverUnsavedReplays=true` **配置项预留、实现未写**。
   但注意 —— ServerReplay 写录像用的**也是** ReplayStudio 的 `ZipReplayFile`，
   所以它留下的 `<name>.mcpr.tmp` **理论上能被客户端这套机制直接救回**，值得以后实测验一次。

**第二轮 crash 原文（留档）**：

```
Description: Recovering replay file
java.io.FileNotFoundException: …\replay_recordings\recoverytest.mcpr.tmp\source (拒绝访问。)
    at java.io.FileOutputStream.open0(Native Method)
    at com.replaymod.replaystudio.replay.ZipReplayFile.saveInputFile(ZipReplayFile.java:163)
    at com.replaymod.replaystudio.replay.ZipReplayFile.write(ZipReplayFile.java:223)
    at com.replaymod.core.gui.RestoreReplayGui.tryRecover(RestoreReplayGui.java:145)
    at com.replaymod.core.gui.RestoreReplayGui.lambda$recoverInBackground$6(RestoreReplayGui.java:99)
    at java.lang.Thread.run(Thread.java:1474)
```

#### ★★ 本轮新增的 6 个坑（**坑 36~41**，正式条目见第 4 节，这里给要点）

1. **坑 36**：`RestoreReplayGui` 的 log4j 输出（`Found partially saved replay` / `Attempting recovery`）
   **不落进 `logs\` 下任何文件**。→ 判断恢复流程**只能靠行为证据**：`metaData.json` 的
   `duration`/`generator`、`tmp\` 是否被清理、`.replay_cache\` 是否新建。
2. **坑 37**：**Windows 会吞掉"点击非前台窗口"的第一下**（用于激活）。→ `-Click` 要连点两次，
   但**连点的第二下会落在界面已经变化之后的位置**（本轮第二下就误触了主菜单的链接按钮，
   弹出「你确定要打开以下网页？」）。**正确姿势：点一次 → 用 PrintWindow 确认界面变化 → 再决定补不补点。**
3. **坑 38（本轮最坑）**：`<name>.mcpr.tmp\` 的真实结构是
   ```
   <name>.mcpr.tmp\
   ├── source      ← ★ 是「文件」不是目录！内容是原始 mcpr 的 canonical path
   ├── changed\    ← 目录
   └── removed\    ← 目录
   ```
   （`saveInputFile()` 就是 `new FileOutputStream(sourceFile)` 直接写它。）
   **别照 stable 分支的 `changed/removed/source` 望文生义建三个目录** —— `FileOutputStream`
   打在目录上会抛 `FileNotFoundException: …\source (拒绝访问。)`。
4. **坑 39**：读 `fml-client-latest.log` 时 **`FileShare.ReadWrite` 仍可能被 log4j 拒绝**
   （`…another process has locked a portion of the file`）→ 共享读取要带重试。
   另外 **`dsh-stdout.log` 会被 log4j 轮转**（实测长度 2902243 → 2898321），
   **"长度变小"不能当成"没有新增"**。
5. **坑 40**：`gtnh-window.ps1 -Shot` 抓的是**屏幕区域**，洛洛把 DSH 切到前台时抓到的就是聊天窗口
   （本轮实际发生）。→ **要游戏内容一律用 `gtnh-printwindow.ps1`**（`PrintWindow`，不受遮挡）。
6. **坑 41**：本轮客户区实测为 **854x480**（12.14 记录的 1706x996 已不适用）→
   12.14 那组"已实测可用坐标"**全部作废**，用之前必须按当前窗口重量。
   （本轮实测有效的恢复界面「是」按钮客户区坐标：**(274, 299)**。）

#### 收工状态（2026-10-10 傍晚，洛洛去吃晚饭前）

- **无遗留进程**：`Get-Process java` 为空；夹具（`recoverytest.mcpr` 及其 `.tmp`、`.replay_cache` 缓存）已全删；
  `replay_recordings\` 根目录 **29 个 mcpr**，与开工前一致（洛洛的录像一个没动）。
- **零代码改动**：本轮**没有碰任何 jar / 源码**；唯一的落盘改动就是本文档。
- **下一轮入口（洛洛离线时未定，两个候选）**：
  1. **服务端实体包漏录（12.6-⑤）** —— 现在速览表的**第一优先**；需要把
     `ServerReplay-1.7.10-0.1.0-client.jar` 从 `mods-disabled\` **放回 `mods\`**，然后**进世界**实测
     （会占屏幕几分钟；`run-test.ps1` 会 kill 所有 java，动手前先确认洛洛不在玩）。
  2. **服务端 `RecorderRecoverer`（P3）** —— 纯代码 + 离线自测（`ServerReplaySelfTest`），**不占屏幕**；
     顺带验证本节遗留观察 4：「服务端留下的 `<name>.mcpr.tmp` 能否被客户端这套机制救回」。
- **可复用的取证素材**：`tools\shots\30~38-*.png`（恢复界面 / 点击前后 / 第二轮 crash / 第三轮结果）、
  `<workspace>\.selftest\dump\{ReplayMod,RestoreReplayGui-members,RestoreReplayGui,ZipReplayFile}.txt`（字节码 dump）。

---

### 12.21 「打开回放必崩（S25 空指针）」结案 —— ✅ **已修复 + 双向实测（v32）**

**来源**：洛洛给的 `https://mclo.gs/uJ0PkXJ` + `<home>\Downloads\ReplayMod-GTNH-S25空指针-PROJECT_HISTORY.md`。

**症状**：
```
java.lang.NullPointerException: Cannot invoke "WorldClient.func_147443_d(int,int,int,int,int)"
  because "this.field_147299_f.field_71441_e" is null
    at NetHandlerPlayClient.func_147294_a(NetHandlerPlayClient.java:1182)   // handleBlockBreakAnim
    at S25PacketBlockBreakAnim.func_148833_a
    at NetworkManager.func_74428_b
    at GuiOpeningReplay.func_146269_k
```

**根因（★ 两个不同步的 world 字段）**：
- `NetHandlerPlayClient.clientWorldController`（`field_147300_g`）：由 `S01PacketJoinGame` 在加载期就建好 → 大多数包用它，**不崩**
- `Minecraft.theWorld`（`field_71441_e`）：要等加载结束才赋值 → **恰好 15 个 handler 直接解引用它**，加载期必 NPE
- 这 15 个（`javap -c` 扫全类）：`func_147237_a`(S0C) / `147246`(S0D) / `147248`(S33) / `147255`(S29) /
  `147261`(S24) / `147264`(S34) / `147271`(S05) / `147273`(S35) / `147277`(S28) / `147279`(S0B) /
  `147280`(S07) / `147281`(S0F) / `147283`(S27) / `147285`(S03) / `147294`(S25)
- **这就解释了 12.13 的 SpawnGuard 为什么挡不住**：它查 `field_147300_g`，崩溃点在 `field_71441_e`。洛洛文档里猜的「(b)」方向对，但要说到字段这一层。

**复现（零存档风险，方法可复用）**：夹具 `replay_recordings\AAA-s25-first-test.mcpr`
= 复制 `2026_10_10_17_19_22.mcpr`，在 **LoginSuccess 之后**插入一个 S25 包，并**重算 `recording.tmcpr.crc32`**。
**关键**：ReplayMod 在**启动扫描阶段**就会预加载录像（`InputReplayTimer` → `GuiOpeningReplay`），
所以坏录像让游戏**启动即崩**，根本不用点「加载」。
- v30-release（未修）+ 该录像 → **崩溃**，与 mclo.gs/uJ0PkXJ **逐行相同**（含行号 1182）
- v32 + 同一录像 → **不崩**，回放界面正常打开，**零新 crash**

**修复（v32）**：
| 组件 | 作用 |
|---|---|
| `MixinNetHandlerPlayClientNullWorldGuard` | 14 个 handler 各注入 HEAD；`Minecraft.theWorld == null` 时拦截 |
| `MixinNetworkManagerDeferredFlush` | 注入 `NetworkManager.func_74428_b` HEAD：世界就绪后把缓存的包**放回接收队列**，交给 vanilla 派发 |
| `com.gtnh.replaycompat.util.NullWorldGuard` | 共享反射/判定工具（**必须在 mixin 包外**，见坑 42） |

**丢弃 vs 延迟（刻意区分）**：
- **丢弃**（纯瞬时视觉/音效）：S25 / S24 / S28 / S27 / S29 / S0B / S0D
- **延迟重放**（带状态）：S35(TE) / S0F(SpawnMob) / S07(Respawn) / S33(Sign) / S34(Maps) / S03(Time) / S05(SpawnPos)
- 延迟**不用反射派发**（放回队列即可）→ 完全不改变异常行为。

**产物**：`replaymod-1.7.10-2.5.2-gtnh-v32-nullworld.jar`（`10821617` B，
SHA256 `D6E466876AFDECA2F788F77A9162B81B6171E3E84C3429C8031E0EF0931F6F9E`）= v30-release + 上述 2 个 mixin。
**已部署、已实测，是本轮唯一可用的交付版。**

**遗留**：洛洛给的崩溃环境是 **338 mods / Java 25.0.1(Microsoft) / Windows 11**，本机 **334 mods** ——
机理与行号一致，但**那个环境需要拿 v32 复测**。

---

### 12.22 「回放界面鼠标点不动 / 视角转不动」—— 根因定位（**✅ 已结案 → 见 12.23**）

**症状（v32 上）**：进回放后时间轴与控件画得出来，但**鼠标点不动**；强行让鼠标可见后又**拖不动画面、无法转视角**。

**已查清的事实（探针实测，勿重复）**：

1. 回放界面**不是 GuiScreen，是 overlay**：`com/replaymod/replay/gui/overlay/GuiReplayOverlay`。
2. overlay 的输入屏是内部类 **`AbstractGuiOverlay$UserInputGuiScreen extends GuiScreen`**，它**自包含、不碰 Forge 事件总线**：
   - `func_146274_d()` → `super.func_146274_d()`（读 Mouse 事件）+ 滚轮
   - `func_73864_a(x,y,btn)` → `this$0.forEach(Clickable).mouseClick(...)`
   - → 所以**与坑 34（jGui 事件被注销）无关**；`MinecraftGuiScreen` 也覆写了全套输入方法，那条路是好的。
3. **overlay 只在 `mouseVisible == true` 时才挂这个输入屏**：
   ```java
   if (visible) {
       if (mouseVisible) { if (mc.currentScreen == null) mc.displayGuiScreen(userInputGuiScreen); }
       else              { if (mc.currentScreen == userInputGuiScreen) mc.displayGuiScreen(null); }
   }
   ```
4. **探针实测（v34）**：`visible=true, mouseVisible=false, mc.currentScreen=null`，`updateUserInputGui` 每帧刷几百次。
   → **`currentScreen` 条件满足，但 `mouseVisible` 永远是 false** → overlay 主动保证"没有输入屏" → 鼠标事件无人处理。
5. **谁设 `mouseVisible`**：扫全 jar 只有 `GuiReplayOverlay$EventHandler`（另加 `simplepathing/GuiPathing$10`）：
   ```java
   while (settings.keyChat.wasPressed() || settings.keyCommand.wasPressed()) {
       if (!isMouseVisible()) setMouseVisible(true);
   }
   ```
   → 上游语义：**按住 T 或 `/` 才显示鼠标**。
6. **高版本（`ReplayMod-stable` 的 1.20.1 源码）逐字相同** → **不是版本差异**。
7. **但这条链在 GTNH 上根本不触发**（洛洛实测"所有按键都一样"）：`KeyBindingEventCallback` 由
   `com/replaymod/core/mixin/MixinKeyboardListener` 发出，而它读的是
   **`org.lwjgl.input.Keyboard.getEventKey()/getEventCharacter()/getEventKeyState()`**，挂在
   `KeyBinding.func_74507_a` 前后 —— **lwjgl3ify（LWJGL3+SDL）下游戏不走这条 LWJGL2 输入路径**。

**走过的弯路（别重复）**：
- **v35**：在 `setVisible` 后强制 `setMouseVisible(true)` → **UI 能用了，但视角拖不动** ❌
  原因：上游语义是「**鼠标被抓取时才能自由转视角**」，永久放开鼠标 = 视角回到"由录像输入驱动"。
- **v36**：改成轮询 `keyChat`/`keyCommand` 的 `isPressed()` → **和聊天栏冲突**（按 T 会弹消息栏）❌
- **v37/v38**：改用 `keyBindSneak`（Shift，`field_74311_E`；已确认 ReplayMod 那 18 个 `replaymod.input.*`
  快捷键**没占用 Shift**）→ **代码没编译成功**。
  ⚠️ **`v33`/`v34`/`v37`/`v38` 这几个 jar 都不可用**：v33/v34 是探针版，v37/v38 是**编译失败后误部署**的产物
  （里面的 `MixinGuiReplayOverlayMouseVisible.class` 实际还是 v36 的 T 键版）。**已全部废弃，别用。**

**当前源码状态**：`MixinGuiReplayOverlayMouseVisible.java` 已改成 **Shift 版**
（字段声明 / 反射解析 / 使用三处都已替换成 `keyBindSneak` + `field_74311_E`），
但**最后一次编译没通过就被我误注入了**，需要**重新编译验证**。

**下一步建议（给下个会话，按优先级）**：
1. **先编译 + 注入（基于 v32）→ 实测「按住 Shift = 操作 UI，松开 = 抓取转视角」**。
   编译命令见 12.15，**必须 `-proc:none`**，且 **classpath 必须包含 `mixin-out` 自己**（见坑 47）。
   **务必检查 `javac` 退出码，非 0 就中止，不要继续注入。**
2. 若仍不行 → **换触发方式**（鼠标中键 / ReplayMod 自己的键位），别再在按键上打转。
3. **★ 洛洛的提示「高版本没这问题」值得深挖**：高版本 `GuiReplayOverlay` 源码相同，差异**不在这个类**，
   可能在：
   - **jGui 的 `AbstractGuiOverlay` 版本差异** —— 本仓库只有 `VanillaGuiScreen` 的对照（`upstream-ref\jgui-*`），
     **jGui 高版本的 `AbstractGuiOverlay` 源码还没拿到** → 用 `api.github.com` 拉一份对照，很可能一眼看出正确做法；
   - 或高版本 ReplayMod 在回放中**拦截了聊天键**（所以按 T 不会开消息栏）。
4. 另有一个**尚未定位**的独立问题：洛洛提到「回放会重新加载 / 加载后恢复之前的」—— 信息不足，下次先问清现象。

**本轮沉淀的素材（勿删）**：
- `replay_recordings\AAA-s25-first-test.mcpr` —— **回归夹具**：开头第 2 个包就是 S25，用于验证"加载期不再崩"。
- `.selftest\Crc32Tool.java` —— 标准 CRC32（重建 `recording.tmcpr.crc32` 用）。
- `.selftest\dump\*.txt` —— `NetHandlerPlayClient` / `ReplayMod` / `RestoreReplayGui` / `ZipReplayFile` /
  `NetworkManager` / `AbstractGuiOverlay` / `UserInputGuiScreen` 的字节码 dump。
- `replaymod-compat\src\...\mixin\MixinAbstractGuiOverlayInputProbe.java` —— **诊断探针**
  （记录 `visible`/`mouseVisible`/`currentScreen`）；需要时加回 `mixins.replaycompat.json` 的 `client` 段再注入。

---

### 12.23 「回放界面鼠标点不动 / 视角转不动」结案 —— ✅ **已修复 + 双向实测（v42）**

> **12.22 已结案。** 本节给出根因的最终形态、修复、以及**四种被排除的路径**（下一位别重复）。

#### 交付

| 项 | 值 |
|---|---|
| 文件 | **`replaymod-1.7.10-2.5.2-gtnh-v42-replaymouse.jar`** |
| 大小 | `10825170` B |
| SHA256 | `4C0BFA7819683FB3C8F5051514DC04A4F504564A2C5FE2AFAED641582329CC90` |
| 构成 | **v32 + 1 个新 mixin**（`MixinGuiReplayOverlayMouseVisible`） |
| 单变量验证 | 与 v32 **逐 entry SHA256 比对**：只多这 1 个 class，只有 `mixins.replaycompat.json` 内容不同 ✅ |

#### 一句话方案

`AbstractGuiOverlay` 只在 `mouseVisible == true` 时才挂输入屏（`UserInputGuiScreen`），而 GTNH 上它**永远是 false**（上游唯一的赋值点走 `KeyBindingEventCallback`，那条链在 lwjgl3ify 下不触发）。

**修复 = 按住 Shift（`keyBindSneak`）时轮询它的 `pressed` 字段 → `setMouseVisible(true)`；松手 → `setMouseVisible(false)`。**
于是：**按住 Shift = 鼠标松开、可以点时间轴和按钮；松开 Shift = 鼠标重新被抓取、可以自由转视角**（与上游"按 T 出鼠标"语义一致，只是换了个不会弹聊天栏的键）。

#### ★★ 核心陷阱：`displayGuiScreen` 会清空**所有** KeyBinding —— v39 每 tick 抖动 400 次的根因

只做"轮询 + `setMouseVisible`"**必然抖动**（v39 实测：21 秒内 400 次 RELEASED/GRABBED 成对翻转，**而我一个键都没按**）。反馈循环是：

```
pressed=true  →  setMouseVisible(true)
                     →  Minecraft.displayGuiScreen(UserInputGuiScreen)
                        →  Minecraft.setIngameNotInFocus()
                           →  KeyBinding.unPressAllKeys()      ← ★ 把所有 binding 的 pressed 清成 false
                              →  下一帧读 pressed=false  →  setMouseVisible(false)
                                 →  displayGuiScreen(null) → setIngameFocus()
                                    →  inGameHasFocus=true（于是下一次 notInFocus 又会清）
                                       →  游戏重新填入按键状态 → 回到第一步（每 tick 一次）
```

**「显示鼠标」这个动作本身把「显示鼠标」所依赖的按键状态清掉了。**

**解法：切换后立刻把状态写回去** —— 当 applied 状态是"按住"时，调用
`KeyBinding.setKeyBindState(getKeyCode(), true)`（反射 `func_74510_a`）。下一帧读到的仍是 `true`，循环被打断；而**松开时由游戏自己在真实 key-up 事件里设 false**，所以不影响松手。

> 这也是为什么 **v41/v42 只读 `getIsKeyPressed()`（`func_151470_d` → `pressed` 字段）**，
> **绝不能用 `isPressed()`（`func_151468_f`）**：后者是"消费式"的（`if (pressTime == 0) return false; else { --pressTime; return true; }`），
> 会与 `unPressAllKeys` 相互作用，是 v39 抖动的另一个来源。

#### 实测证据（全部为可判定证据，不是"日志说成功"）

1. **注入**：`-Dmixin.debug.export=true` → `.mixin.out\class\...\AbstractGuiOverlay.class` → `javap -p -c` 里
   `updateUserInputGui()` 的字节码第 2 条指令就是 `invokespecial handler$zea000$replaymod$replaycompat$pollReplayMouseKey` ✅
   （**不要靠日志判断注入**，见文首「环境铁律」最后一条。）
2. **运行时**（v42，回放界面稳定后按一次 Shift）—— **日志恰好 3 条**：
   ```
   replay mouse resolver: mc=true settings=true sneak=true isHeld=true setState=true keyCode=true
   replay mouse RELEASED - overlay UI is clickable (mouseVisible=true,  switch #1)
   replay mouse GRABBED  - free camera look       (mouseVisible=false, switch #2)
   ```
   `mouseVisible=` 是**从 overlay 自己读回来**的（`isMouseVisible()`），属坑 31 要求的"可判定证据"。
   多次按键后累计 **7 对干净切换、零次额外翻转**。
3. **A/B 点击实验**（Win32 注入鼠标，客户区 854x480）：
   - 不按 Shift 点「暂停」按钮（客户区 `37,39`）→ 连续两张截图 **SHA256 完全相同** ⇒ 点击**打不到 UI** ✅
   - 按住 Shift 点同一个位置 → 画面/时间轴状态**发生变化** ⇒ 点击**生效** ✅
4. `keyCode=42`（`KEY_LSHIFT`）实测确认 `field_74311_E` **就是** `keyBindSneak`（不是猜的）。

#### 被排除的四条路（★ 下一位别再试）

| 路径 | 实测结果 | 依据 |
|---|---|---|
| `org.lwjgl.glfw.GLFW.glfwGetKey`（**高版本 ReplayMod 的正统做法**） | ❌ `ClassNotFoundException` | **这套环境根本没有 GLFW**：lwjgl3ify 3.0.31 走的是 **SDL3**（`org.lwjgl.sdl.*`），版本 json 的 libraries 里有 `lwjgl-sdl`、**没有 `lwjgl-glfw`**。高版本的"GLFW 回调直连"在这里**无从照搬** |
| `org.lwjgl.input.Keyboard.isKeyDown(int)` | ❌ 方法能解析，但**按住时恒 false** | 实现本身是对的（读 `sdlKeyPressedArray`，`42→SDL 225` 映射正确），但此环境下拿不到有效值 |
| `me.eigenraven.lwjgl3ify.api.InputEvents`（**"高版本方案"在本环境的等价物**） | ⏸ **未采用（不需要）** | v42 已完整复现上游行为；签名见下方备查 |
| 按 **T**（上游的键） | ❌ **实测完全无变化** | 用 v41 探针做了巧妙判据：若上游 `EventHandler` 真被触发，`mouseVisible` 会被设 true，v41 下一帧就会打出**意外的** `GRABBED`。实测 switch 数 **14 → 14 不变** ⇒ **12.22「按 T 也无效」的结论成立** |

**`InputEvents` 完整签名（备查，万一将来要改成事件驱动）**：

```
me.eigenraven.lwjgl3ify.api.InputEvents  (public class, 方法全 public static)
  static void addKeyboardListener(InputEvents$KeyboardListener)
  static void removeKeyboardListener(InputEvents$KeyboardListener)
  static void addWeakKeyboardListener(...) / removeWeakKeyboardListener(...)
  static void injectKeyEvent(InputEvents$KeyEvent) / injectTextEvent(...)
  static void beginTextInput() / endTextInput()

InputEvents$KeyboardListener (public interface)
  void onKeyEvent(InputEvents$KeyEvent)     // 抽象
  void onTextEvent(InputEvents$TextEvent)   // default 空实现

InputEvents$KeyAction  (enum): PRESSED / REPEATED / RELEASED
InputEvents$KeyEvent   (public class, final 字段):
  int lwjgl2KeyCode; int lwjgl2ScanCode; int sdlKeyCode; int sdlScanCode;
  int sdlRawKeyCode; KeyAction action; short sdlKeyModifiers; long keyNamePointer;
```
- **推荐用 `lwjgl2KeyCode`**（可与 `Keyboard.KEY_LSHIFT == 42` 直接比较）。
- 事件链（字节码确认）：`Display.update() → Lwjgl3ifyEventLoop.pumpEvents → SDL_PumpEvents/SDL_PeepEvents → handleKeyEvent → InputEvents.injectKeyEvent`，**回调稳定发生在渲染主线程**。
- 两个监听器列表是**非同步 `ArrayList`**，注册/注销要放主线程。
- 从 Mixin 反射可达（`InputEvents` 自己引用了 `net.minecraft.client.Minecraft`，说明它与游戏代码同一加载器）。
- **注意**：`org.lwjglx.input.Keyboard.addRawKeyEvent` **是有调用者的**（`handleKeyEvent → addSdlKeyEvent → addRawKeyEvent → eventQueue`），所以
  **`Keyboard.next()` 事件队列是活的**（这也解释了"玩家能正常移动"）—— 「队列是死的」是**早期误判**，别据此下结论。

#### 走过的弯路（本轮，按版本）

| 版本 | 改了什么 | 结果 |
|---|---|---|
| v33 / v34 | 纯诊断探针 | 确立了 `visible=true / mouseVisible=false / currentScreen=null` |
| v35 | `setVisible` 后强制 `setMouseVisible(true)` | UI 能用，但**视角拖不动**（鼠标永久释放） |
| v36 | 轮询 `keyChat` | **和聊天栏冲突**（按 T 弹消息栏） |
| v37 / v38 | 改用 `keyBindSneak` | **编译失败后被误注入**（源码引用了未声明的 `fieldKeyCommand`）→ 坏包，已废弃 |
| **v39** | 修好编译，`getIsKeyPressed() \|\| isPressed()` 轮询 | 注入成功、resolver 全绿，但 **每 tick 抖动 400 次**（见上文反馈循环） |
| v40 | 改读 GLFW `glfwGetKey` + `isKeyDown` | 两条路都不出值（本环境无 GLFW；`isKeyDown` 恒 false），静默降级 |
| v41 | 回退到 `KeyBinding`，**加状态恢复** + 全套原始值探针 | **成功**：切换干净、`pressed=true` 稳定保持 |
| **v42** | v41 去掉探针/降噪，保留失控保护与日志上限 | **交付版**：日志恰好 3 条 |

> **坑 47 在这轮又应验了一次**：v37/v38 就是"javac 失败却照旧注入"的产物。本轮从 v39 起一律
> `if ($LASTEXITCODE -ne 0) { throw }`，非 0 立即中止。

#### 复现构建流程（与 12.15 相同，**注意 classpath 必须含 `mixin-out` 自己**）

```powershell
$compat = '<workspace>\replaymod-compat'
$javac  = '<jdk8>\bin\javac.exe'
$cp = "$compat\mixin-out;$compat\deps;<workspace>\ServerReplay-1.7.10\build\rfg\recompiled_minecraft-1.7.10.jar;" +
      "<workspace>\tools\replaymod-1.7.10-2.5.2.jar;<libs>\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar;" +
      "$compat\libs\asm-9.9.jar;$compat\libs\asm-tree-9.9.jar;$compat\libs\asm-commons-9.9.jar"
# 编译全部功能 mixin（排除探针），逐个检查退出码
Get-ChildItem "$compat\src\com\gtnh\replaycompat\mixin" -Filter '*.java' |
  Where-Object { $_.Name -notmatch 'Probe|EventHandler' } | ForEach-Object {
    & $javac -proc:none -encoding UTF-8 -nowarn -cp $cp -d "$compat\mixin-out" $_.FullName
    if ($LASTEXITCODE -ne 0) { throw "compile failed: $($_.Name)" }        # ★ 坑 47
  }
# 以 v30-release 为基注入（v42 = v30-release + 9 个 mixin）
& '<workspace>\tools\inject-replaymod-clientmixins.ps1' `
   -Jar '<workspace>\replaymod-1.7.10-2.5.2-gtnh-v30-release.jar' `
   -Dest '<workspace>\replaymod-1.7.10-2.5.2-gtnh-v42-replaymouse.jar'
```

#### 自动化测试要点（省得下次踩）

- **注入按键**：`keybd_event(bVk, bScan, 0, 0)` 必须带**真实 scancode**（LSHIFT: `0xA0, 0x2A`；T: `0x54, 0x14`），
  否则游戏读不到。**本轮的 keybd_event 是有效的**（v40 失败纯属路径死，不是注入无效）。
- **点击前先切前台**：`gtnh-window.ps1 -Shot` 会 `SetForegroundWindow`；**直接连点会被 Windows 吞掉第一下（坑 37）**
  —— 本轮因此白跑了一轮（三次点击全落空，界面停在主菜单）。
- **`-Shot` 抓客户区、坐标即客户区像素**；日志里的 "427x240 screen" 是 **GUI scale=2 后的逻辑坐标**，
  换算成像素要 **×2**（回放中心按钮：逻辑 `(317,8)` → 像素 `(634,16)`，中心约 `(734,36)`）。
- **回放加载期间主线程阻塞**，`updateUserInputGui` 根本不被调用 → **在加载界面按 Shift 什么都不会发生**。
  必须等世界加载完（本机约 60~100 秒，画面会从纯色背景变成带时间轴的回放画面）再按键。
- **别用截图 SHA256 判断"视角有没有转"**：若回放正在播放（或相机在水下，水有动画），任何两帧都不同。
  先用日志确认状态，或先把回放暂停。
  → **正确的做法是定量**：用 `System.Drawing` 每隔 4 像素采样 R/G/B 求**平均绝对差**。
  本轮实测：播放中两帧（间隔 4 秒）平均差 ≈ **66.9**，暂停后 ≈ **0.003** —— 差三个数量级，判据非常干净。

---

#### ★ 补充（2026-10-10 深夜）：12.22 的归因修正 + 上游 T 键为什么在这台环境失败

**先纠正一件事**：12.22 把"按 T 也无效"归因为"`KeyBindingEventCallback` 那条回调链根本不触发"，
并进一步推断成"lwjgl3ify 下 LWJGL2 输入整条死掉"。**这个归因不准确**（勘误区 #26）。实测证据如下。

**（1）ReplayMod 自己的快捷键是能用的**（定量实测）

| 类别 | 代表键 | 驱动机制 | 结论 |
|---|---|---|---|
| **普通快捷键** | **P** 播放/暂停、M 加标记、Z 全亮、N 缩略图、B 玩家总览 | Forge `InputEvent.KeyInputEvent` → `KeyBindingRegistry.handleKeyBindings()` → MC 原生 `isPressed()`（消费式） | ✅ **P 实测生效** |
| **重复快捷键** | V 同步时间轴 | `TickEvent.RenderTickEvent`（每帧）→ `getIsKeyPressed()` 轮询 | ✅ 机制上必然工作 |
| **相机翻滚** | L / J / K | `CameraEntity` 直接轮询 `getIsKeyPressed()` | ✅ 同上 |

`P` 的判据是**定量**的（避免"截图 hash 不同"被播放/水动画污染的假阳性）：

```
回放播放中，相邻两帧（间隔 4 秒）平均像素差  = 66.878
按一下 P 之后，同样间隔            平均像素差  = 0.003    ← 画面彻底冻结 => P 生效
```

**（2）由此反推：`KeyBindingEventCallback` 一直是通的**

`P` 的 handler 由 `KeyBinding.isPressed()` 触发，而它返回 true 的前提是 **`pressTime != 0`**，
`pressTime` 只由 **`KeyBinding.onTick`(`func_74507_a`) 递增**。既然 P 能触发 → `onTick` 在被调用；
而 `MixinKeyboardListener.afterKeyBindingTick` 注入在 `onTick` 的 **RETURN**，且 `javap` 确认其字节码里
**没有任何前置条件**（直接 `KeyBindingEventCallback.EVENT.invoker().onKeybindingEvent()`）
→ **`KeyBindingEventCallback` 一直在正常发出**。

**（3）上游 T 键方案在这台环境失败的真正原因：T 被别的 mod 抢走了**

实测按 T 弹出的**不是聊天栏**，而是 **Hardcore Ender Expansion 的「Ender Compendium」书本 GUI**
（截图 `tools\shots\t2.png`）。于是：

```
按 T → 别的 mod 用 KeyInputEvent 打开书本 GUI → mc.currentScreen = 那个 GuiScreen
     → AbstractGuiOverlay.updateUserInputGui() 里 if (mc.currentScreen == null) 不成立
        → UserInputGuiScreen 挂不上
```

**即：不是"链断了"，而是「键位被抢」+「overlay 硬要求 `currentScreen == null`」这两条一起把上游方案堵死了。**

> ⚠️ **一处仍是推测（未验证）**：ReplayMod 的 `EventHandler` 是否**确实**在按 T 时执行了 `setMouseVisible(true)`
> （即"哪怕设了 true 也因 `currentScreen != null` 而无效"）——本轮**没能干净验证**：按 T 后被书本 GUI 挡住，
> 后续的点击判据失效。要确认的话，干净做法是：进回放 → 先点一次暂停按钮（应当无效）→ 按 T 打开书本 → ESC 关回回放
> → 再点暂停按钮（若生效，则说明上游确实设了 `mouseVisible`）。

**（4）这就是 v42 选 Shift 的原因，也是为什么不建议改用 T**

`keyBindSneak`（Shift）在这台环境**没有绑定任何 GUI**，按下它时 `currentScreen` 保持 `null`，overlay 才挂得上输入屏。
若要换键，**必须先确认该键没被任何 mod 抢**。本轮实测的占用情况：

| 键 | 状态 |
|---|---|
| **T** | ❌ **被抢**（Hardcore Ender Expansion 书本） |
| **B** | ❌ **被抢**（JourneyMap「新建导航点」） |
| P / M / Z / N / V / L / J / K | ✅ 可用（ReplayMod 实际生效） |
| **Shift（潜行）** | ✅ 可用，且不绑定任何 GUI —— **v42 采用** |

> **⚠️ 更正（2026-10-10 深夜，紧接上方写完后做的复验）**：本补充节（2）里"`KeyBindingEventCallback` 一直是通的"
> 只是**推断**，已被紧接着的**干净复验推翻**，特此更正（勘误区 #26 是最终版）：
>
> ```
> 回放暂停中，前后两帧平均像素差 = 0.17        ← 静止基线（截图 f0.png 确认在回放画面）
> 按 T（弹出被抢走的书本）→ 关掉书本 → 回到 currentScreen == null 的回放画面
> 点击 overlay「暂停」按钮 → 前后两帧平均像素差 = 0.1   ← 仍然无效
> ```
>
> ⇒ **`mouseVisible` 仍是 false，上游 `EventHandler` 并没有把它设起来。**
> 所以本补充节（3）里"哪怕设了 true 也因 `currentScreen != null` 而挂不上"这个解释**不成立**；
> 真相是**这条路径本身就没跑通**。正确的分层结论是：
> **按键读取层 ✅ 正常（P 实测）／ReplayMod 普通快捷键 ✅ 正常／上游 overlay 路径 ❌ 不生效**，
> 而**失效的确切环节仍未定位**（候选：`afterKeyBindingTick` 被 `beforeKeyBindingTick` 的 `ci.cancel()` 跳过 /
> `EventHandler` 未注册 / `keyChat.isPressed()` 为 false）。
> 本节（4）"**T 被别的 mod 抢走**"这一条**仍然成立**（独立实测），所以即使把 overlay 路径修好，用 T 依然会被抢。

---

#### ★ 补充二（2026-10-10 深夜）：换触发键 → 左 Ctrl（v48），以及鼠标中键为什么走不通

**交付升级**：`replaymod-1.7.10-2.5.2-gtnh-v48-ctrltoggle.jar`（`10827526` B，SHA256 `6DE20297…`）。
触发方式从"只能按住 Shift"扩展为两种：

| 操作 | 语义 |
|---|---|
| **点一下左 Ctrl**（`keyBindSprint` → `field_151444_V`） | **锁定**：鼠标保持可见，直到再点一下 |
| **按住 Shift**（`keyBindSneak` → `field_74311_E`） | **临时**：松开即抓取（v42 原有行为，保留） |

`target = ctrlToggle || shiftHeld`，只在变化时应用；应用之后把"补丁认为仍被按住的键"用
`KeyBinding.setKeyBindState` 写回（原因即上文那条反馈循环）。Ctrl 的按下沿用 `ctrlWasDown` 做边缘检测，
并且**在写回时保持它**，所以按住 Ctrl 不会连续触发、真松手才能复位。

**为什么最后是 Ctrl**：本包内**没有任何 mod 占用它**、在回放里**没有任何副作用**（不像 Shift 会让相机潜行、
更不像 T/B 会被抢走）、键盘左下角位置顺手，而且**不需要一直按着**。

**★ 鼠标中键：四条路全部实测排除（这一节省掉下一位一整轮）**

| 路径 | 实测结果 |
|---|---|
| `org.lwjgl.input.Mouse.isButtonDown(2)` | ❌ **恒 false** —— 它读的 SDL 状态数组在本环境没人维护（与 `Keyboard.isKeyDown` 同源，见坑 49） |
| 轮询 `keyBindPickBlock`（中键 = keyCode `-98`，即 `button - 100`，这正是 MC 把鼠标按钮映射成绑定键码的方式） | ⚠️ **确实能工作**（v44 首次点击真的切换了），但**反复切换会漏**：LWJGL2 的 `Mouse.next()` 永远不处理队列里最后剩下的那个事件，**释放事件被吞掉** → 下一次按下检测不到 |
| Forge `InputEvent.MouseInputEvent` | ❌ 注册到 `MinecraftForge.EVENT_BUS` 后**一个事件都收不到**（v45/v46，日志只打了 "registered" 然后什么都没有）。**原因**：1.7.10 里 `InputEvent` 是 **FML 的 game event**，挂在 **`FMLCommonHandler.instance().bus()`** 上，与 Forge 总线**是两条不同的总线**。改成两条都注册（v47）后**未测完**（构建中途被中止） |
| `me.eigenraven.lwjgl3ify.api.InputEvents` | ❌ **只有键盘监听器**，没有鼠标 API |

**v43~v48 弯路一览**：v43（`Mouse.isButtonDown`）→ v44（`keyBindPickBlock` 轮询：能工作但不稳）→
v45/v46（Forge 鼠标事件：总线注册错）→ v47（修正总线：未测完）→ **v48（改用键盘键 Ctrl + 沿用 v42 那套已
验证的轮询机制：成功）**。

**遗留（小）**：v48 的 jar 里还带着 `com/gtnh/replaycompat/util/ReplayMouseInput.class`——它是 v45~v47
那段探索的产物，**当前没有任何 mixin 引用它，因此不会被加载、也没有副作用**；想彻底清干净就在编译前删掉
`replaymod-compat\src\com\gtnh\replaycompat\util\ReplayMouseInput.java`。

**★ 一条诊断纪律（本轮真踩到，值得单独记）**：v48 首次实测时，日志里冒出十几条连续的
`Ctrl tap -> LOCKED VISIBLE/GRABBED`，我据此判定"边缘检测被 lwjgl3ify 的电平抖动反复触发"，
并且**把已经可用的版本回滚掉了**。实际上那是**洛洛本人在按键**——3 次脚本注入 + 12 次手按 = 15 次，
与 `switch #1..#15` **严格一一对应**（若是抖动，一次按键会产生多条 tap，switch 数会远超按键数）。
**教训：断言"抖动/异常触发"之前，先确认旁边有没有人在操作；而"次数能不能对上"是最便宜、最有效的判据。**

---

#### ★ 补充三（2026-10-11）：`Error loading entity tracker` 分析 —— **不是游戏崩溃**

**来源**：洛洛在 **338 mods 反馈环境**（PrismLauncher + Java 25.0.1 + Win11 25H2）实测：
`mclo.gs/J7AQ5aF` + 两张截图 + `latest.log`。
**该环境装的是 `replaymod-1.7.10-2.5.2-gtnh-v48`（旧版 v30 已正确禁用）**
⇒ v48 在 338 mods 环境能正常加载，**本问题与适配包无关，是上游 ReplayStudio 的鲁棒性缺陷**。

**两个症状（同一根因）**：

1. 聊天栏红字：`[Replay Mod] Error loading entity tracker: readerIndex(1) + length(4) exceeds writerIndex(4): PooledUnsafeDirectByteBuf(ridx: 1, widx: 4, cap: 4)`
2. 一个 CrashReport 样式的窗口：`Failed to load entity tracker, spectator keyframes will be broken.`

> **★ 先澄清：游戏没有崩溃。** `latest.log` 显示 09:45:19 报错后，09:45:21 正常进世界、09:48:36 正常 `Stopping!`。
> 那个窗口是 **ReplayMod 自己弹的**，不是 MC 的崩溃处理。

**根因链（三层，字节码 + 源码双证据）**

```
GuiPathing.startLoadingEntityTracker() 的加载线程
 └─ EntityPositionTracker.load(Consumer)          [EntityPositionTracker.java:96]
     └─ loadFromPacketData(Consumer)              [:80]  ← private
         └─ PacketUtils.getEntityId(Packet)       [PacketUtils.java:580]
             └─ EntityId.getEntityId(ByteBuf)     [EntityId.java:106]
                 └─ ByteBuf.readInt() → IndexOutOfBoundsException
```

**① 抛点**：某个包被判定为"含实体 ID"，但它的负载只有 **4 字节**；读掉 1 字节包 ID 后只剩 3 字节，却要读 4 字节 int。

**② 异常为何会逃逸**（`javap -c` 的 Exception table）：`EntityPositionTracker.load` 的异常表里
`81 178 181 any` 保护的是"从缓存读"那一段，而 **`loadFromPacketData` 的调用点在 183 —— 落在保护区之外**
（fallback 路径在 229，同样在保护外）。
⇒ 作者只预期了 `EOFException`（录像截断）并已处理，**没预期 Netty 的 `IndexOutOfBoundsException`**，于是它直接冲出 `load()`。

**③ 为何最终变成"弹窗"**（`GuiPathing.java:488-515`）：

```java
} catch (Throwable e) {
    logger.error("Loading entity tracker:", e);
    mod.getCore().runLater(() -> {
        mod.getCore().printWarningToChat("Error loading entity tracker: %s", e.getLocalizedMessage()); // ← 症状 1
        entityTrackerFuture.setException(e);                                                           // ← 异常挂到 future
    });
```

随后 `entityTrackerFuture` 的消费方用
`Utils.error(..., CrashReport.makeCrashReport(t, "Failed to load entity tracker, spectator keyframes will be broken."), ...)`
（`GuiPathing.java:569` 一带）**刻意弹出报告窗口**（症状 2）。
⇒ **这是上游有意的错误上报设计，不是崩溃。**

**影响范围**：**只有 simplepathing 的「位置关键帧 / spectator keyframes」失效**（上游自己就这么写的）；
**回放播放、时间轴、渲染、滚轮、鼠标全部不受影响**（截图里回放正在正常播放）。

**本机复现不了**：本机 `dsh-stdout.log` / `fml-client-latest.log` 对该错误 **0 命中**
⇒ 触发条件是"录像里出现特定长度的包"，**属录像内容相关，不是版本或环境问题**。

**★ 建议的防御性修复（尚未实施）**：新增一个 mixin，把 `load()` 里对 `loadFromPacketData` 的调用包进 try/catch：

```java
@Redirect(method = "load",
          at = @At(value = "INVOKE",
                   target = "Lcom/replaymod/replaystudio/util/EntityPositionTracker;loadFromPacketData(Ljava/util/function/Consumer;)V"),
          remap = false, require = 0)
private void replaycompat$safeLoad(EntityPositionTracker self, Consumer<Double> progress) {
    try {
        Method m = EntityPositionTracker.class.getDeclaredMethod("loadFromPacketData", Consumer.class);
        m.setAccessible(true);
        m.invoke(self, progress);
    } catch (Throwable t) {          // 坏包/解析失败 → 跳过，而不是交给 GuiPathing 去公开
        System.out.println("[replaycompat] entity tracker load failed (ignored): " + t);
    }
}
```

**要点**：`load` 里有**两处**调用点（183 正常 / 229 fallback）→ `@Redirect` 一次覆盖两个；
`loadFromPacketData` 是 **private** → 反射调用（`setAccessible`，本工程已有大量同类先例）；
ReplayStudio **不参与 Forge remap** → `remap = false`；
结果：entity tracker 可能不完整（与现状同等的降级），但**不再弹窗、不再刷红字**，能解析出的部分照常可用。

**验证方式**：本机复现不了 ⇒ **需要在那台 338 mods 环境用同一个录像复验**（预期：不再出现红字与窗口）。

**★ 顺带纠正一个常见误判**：看到 "Crash Report" 样式的界面**不等于游戏崩溃** ——
ReplayMod 会用它展示"非致命但需要告知"的错误。判断依据应是**游戏随后是否还能继续**，
以及 `latest.log` 里除正常 `Stopping!` 之外**有没有异常终止的痕迹**。

---

### 12.24 「导出功能不可用」结案 + `Error loading entity tracker` 防御补丁 —— 交付 **v49**（2026-10-11）

> 本轮由洛洛给出的**同一份 338 mods 环境反馈**驱动：`latest.log` + `mclo.gs/J7AQ5aF`（18756 行）。
> 上一轮（12.23 补充三）只分析了其中的 entity tracker 报错，**漏看了同一份日志里更靠后的一段崩溃** —— 那段才是"导出功能不可用"。

#### 一、`Error loading entity tracker` 防御补丁（12.23 补充三的方案，**已实施**）

新增 `MixinEntityPositionTrackerSafeLoad`，`@Redirect` 掉 `EntityPositionTracker.load` 里**两处**
（字节码偏移 184 / 229）对 private `loadFromPacketData(Consumer)` 的调用，改由反射调用并吞掉 `Throwable`；
失败时额外把 `entityPositions` 字段兜底成空 `HashMap`，**防止紧随其后的 `saveToCache()` 再 NPE**（那会让异常二次逃逸）。

- 目标类 `com.replaymod.replaystudio.util.EntityPositionTracker`（ReplayStudio，**不参与 Forge remap** → `remap = false`）。
- **注入已用字节码导出证实**（`-Dmixin.debug.export=true`）：
  `load` 的两处调用都变成 `redirect$zeb000$replaymod$replaycompat$safeLoadFromPacketData`，handler 体完整。
- ⚠️ **效果仍需 338 mods 环境用同一份录像复验**（本机复现不了，见 12.23 补充三）。

#### 二、「导出功能不可用」—— 根因：**Angelica 的 `GLStateManager` 少了 `glReadPixels(long)`**

**证据（338 mods 环境日志 `J7AQ5aF` 第 17658~17668 行，本机副本 `<workspace>\.selftest\J7AQ5aF.log`）**：

```
[09:47:43] [com.replaymod.render.ReplayModRender]: Starting ...\.minecraft\ffmpeg\bin\ffmpeg.exe with args: -y -f rawvideo ...
[09:47:44] [com.replaymod.render.ReplayModRender]: ---- Minecraft Crash Report ----
Description: Rendering video
java.lang.NoSuchMethodError: 'void com.gtnewhorizons.angelica.glsm.GLStateManager.glReadPixels(int, int, int, int, int, int, long)'
    at com.replaymod.render.capturer.PboOpenGlFrameCapturer.captureFrame(PboOpenGlFrameCapturer.java:97)
```

**链条**：ffmpeg 找到了（`\.minecraft\ffmpeg\bin\ffmpeg.exe`）→ 真正开始渲染 → 读回帧时崩。

- ReplayMod 的字节码请求的是 **`GL11.glReadPixels(IIIIIIJ)V`**（long = PBO 内偏移，LWJGL3 语义）；
- Angelica 的 **`GLSMRedirector`** 会把所有 `org.lwjgl.opengl.*` 调用点改写到 `GLStateManager` 的同名方法；
- 而 **Angelica 2.2.10 的 `GLStateManager.glReadPixels` 只有 3 个重载**
  （`ByteBuffer` / `FloatBuffer` / `IntBuffer`，`javap` 实测，**没有 long 版**）→ 运行时 `NoSuchMethodError`。

**★ 字节码级全量审计（新增可复用工具）**：`.selftest\GLCallAudit.java`
（ASM，扫描 jar 内 **5381 个类**的全部 `org/lwjgl/opengl/*` 调用，逐个与 Angelica `GLStateManager` 的 685 个方法比对）
→ **整个 ReplayMod 里"名字能对上、签名对不上"的调用点只有这一处**（其余全是 exact match 或根本不被重定向）。
即：**导出失败的唯一根因就是它，不是 ffmpeg、不是权限、也不是我们的其它补丁。**

#### 三、修复（两个新 mixin）

| mixin | 做什么 |
|---|---|
| **`MixinPipelinesNoPbo`** | `@Redirect`（GETSTATIC）`Pipelines` 里 4 处 `PixelBufferObject.SUPPORTED` 读取 → 返回 `false`，于是 `if (SUPPORTED \|\| isDepthMap())` 走**上游自带的非 PBO 降级分支**（`SimpleOpenGlFrameCapturer`，用 `ByteBuffer` 重载，Angelica 有）→ 普通导出从此不碰那个缺失的方法 |
| **`MixinPboFrameCapturerGlReadPixels`** | 兜底：把 `PboOpenGlFrameCapturer.captureFrame` 里的调用**反射**转发到真正的 LWJGL3 `GL11.glReadPixels(...,long)`。`@Redirect` 的 target **同时写 GL11 与 `GLStateManager` 两个 owner**（谁先改写字节码都能命中，另一个 `require = 0` 静默跳过）。**深度图导出**（`isDepthMap()` 强制 PBO）靠它 |

**为什么用反射**：普通（字节码）调用会被**同一个 `GLSMRedirector`** 再次改写，反射走运行期查找、直达真 LWJGL 绑定。
`glReadPixels` 是纯读取、不改 GL 状态，绕过状态管理器是安全的。

**代价（明确记录）**：普通导出从 PBO 异步读回变成同步读回 → 渲染**略慢**；功能等价（这条分支本来就是上游在"驱动不支持 PBO"时用的）。

#### 四、交付与验证状态（**如实记录**）

| 项 | 值 |
|---|---|
| 文件 | **`replaymod-1.7.10-2.5.2-gtnh-v49-exportfix.jar`** |
| 大小 / SHA256 | `10832367` B / `5A85C18AFBA8859CC3A61988FFD4D58A1373290413F3CC4FCADA8EB91F0EFD7A` |
| 构成 | **v48 + 3 个新 mixin**（entity tracker / Pipelines-NoPBO / PBO-glReadPixels），共 **12 个功能 mixin** |
| 基座 | 以 **v48** 为基注入（`inject-replaymod-clientmixins.ps1` 是添加/覆盖式，不删旧条目） |
| 已部署 | ✅ 覆盖为 `mods\replaymod-1.7.10-2.5.2-gtnh.jar`（SHA256 一致），**已实测启动 + 回放** |

**本轮已实测通过的**（`-Dmixin.debug.export=true`，2026-10-11 15:00 前后）：

- 启动 ✅ 主菜单就绪（`main menu button added at (530, 8) … 640x374 screen`），**零 mixin 报错、零 crash**；
- 回放 ✅ 加载 `AAA-s25-first-test.mcpr`（S25 夹具）**不崩**、回放界面/时间轴正常、`dropping S0CPacketSpawnPlayer` 照常；
- 鼠标 ✅ `Ctrl tap -> mouse LOCKED VISIBLE` / `RELEASED - overlay UI is clickable`（v48 行为未回归）；
- **`MixinEntityPositionTrackerSafeLoad` 注入 = 已字节码导出证实** ✅（见上）；
- 退出 ✅ `Stopping!` 正常收尾，**无新 crash report**（`crash-reports\` 最新仍是 10-10 的旧文件）。

#### 五、★★ 真机实测结果（2026-10-11 15:12，**洛洛亲手点的「渲染」**）—— ✅ **导出成功**

洛洛自己在回放里点了左起第 2 个图标按钮（= `renderButton`，「渲染录像」），日志与产物如下（`logs\fml-client-latest.log`）：

```
[15:12:00] [com.replaymod.render.ReplayModRender/]: Opening render queue popup
[15:12:04] [com.replaymod.render.ReplayModRender/]: Using default FFmpeg executable
[15:12:04] [com.replaymod.render.ReplayModRender/]: Starting ffmpeg with args: -y -f rawvideo -pix_fmt bgra -s 1920x1080 -r 60 -i - -an -c:v libx264 -preset ultrafast -pix_fmt yuv420p "2026_10_11_15_12_00.mp4"
[15:12:05] [mixin/]: Mixing MixinPboFrameCapturerGlReadPixels from mixins.replaycompat.json into com.replaymod.render.capturer.PboOpenGlFrameCapturer
[15:12:10] [PboOpenGlFrameCapturer:replaycompat$log:609]: [replaycompat] video export: glReadPixels(long) resolved on org.lwjgl.opengl.GL11
```

**产物**（`ffprobe` 实测，不是"看起来像成功"）：

| 项 | 值 |
|---|---|
| 文件 | `replay_videos\2026_10_11_15_12_00.mp4`（`4016006` B，15:12:32 收尾） |
| 编码 / 分辨率 / 帧率 | **h264 / 1920x1080 / 60 fps** |
| 帧数 / 时长 / 码率 | **1230 帧 / 20.5 s / 1.57 Mbps** |
| `NoSuchMethodError` | **0（一次都没有）** |
| 新增 crash-report | **0**（`crash-reports\` 最新仍是 10-10 的旧文件） |

⇒ **「导出功能不可用」结案**，根因判断正确、修复有效。

**★ 但实测同时抓出我自己写错的一处（见坑 51）**：日志里还有一条 WARN ——

```
[15:12:05] [mixin/]: Mixin apply for mod replaymod failed mixins.replaycompat.json:MixinPipelinesNoPbo
    -> com.replaymod.render.rendering.Pipelines:
    org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException:
    GETSTATIC is not a valid injection point specifier
```

即 **`MixinPipelinesNoPbo`（"改走非 PBO 路径"那个）根本没注入成功**，真正救活导出的是**兜底的
`MixinPboFrameCapturerGlReadPixels`（反射直连真 GL11）** —— 日志第 5 行就是它在工作。
它没有把游戏搞崩，是因为 mixin config 是 `required: false`，Mixin 对解析失败的 mixin 只记 WARN 然后跳过。

**所以最终形态是**：导出**继续走上游的 PBO 路径**（性能更好、行为与上游一致），只是把那个
Angelica 缺失的 `glReadPixels(long)` 调用**反射转发给真 LWJGL3**。这比"禁用 PBO"更小、更好。

#### 六、v50（清理版）—— 与已实测的 v49 **行为等价**

**v50 = v49 去掉那个从未生效的 `MixinPipelinesNoPbo`**（源码保留为
`replaymod-compat\src\…\mixin\MixinPipelinesNoPbo.java.disabled`，`*.java` 过滤不会再编译它，
`mixins.replaycompat.json` 也已摘掉它）。目的只有一个：**让日志不再出现那条误导性的 "Mixin apply … failed" WARN**。

| 项 | 值 |
|---|---|
| 文件 | **`replaymod-1.7.10-2.5.2-gtnh-v50-exportfix.jar`** |
| 大小 / SHA256 | `10831328` B / `4C2C8BBBB50B6C544F67438CEC9A4F3D6AC11774FA25D0B2EB6E649CC3AA62FD` |
| 构成 | v48 + 2 个新 mixin（`MixinEntityPositionTrackerSafeLoad` + `MixinPboFrameCapturerGlReadPixels`），共 **11 个功能 mixin** |
| 逻辑上还需重测吗 | **不需要**：v49 里那个失败的 mixin 从未生效，所以 v50 与"洛洛刚实测通过的那一版"**实际执行的代码完全相同**。⚠️ 但严格起见，下次进游戏时**看一眼日志有没有那条 WARN 消失**即可（一秒的事） |

**部署状态**：洛洛实测时 `mods\replaymod-1.7.10-2.5.2-gtnh.jar` = **v49**（`5A85C18A…`），
**游戏运行中 jar 被锁，v50 还没放进去** —— 等游戏退出后覆盖部署即可（命令见第 7 节）。

#### 七、仍未做的

- ⚠️ 338 mods 环境用**同一份录像**复验 entity tracker 弹窗是否消失（`replaymod-1.7.10-2.5.2-gtnh-v49/50` 都行）。
- 可选：把 `MixinPipelinesNoPbo` 按坑 51 的正确写法修好后**再决定要不要用**（当前不需要 —— PBO 路径已实测通过且更快）。

---

### 12.25 「回放里点自己换视角 → 崩」结案 + 「渲染出手臂 / 看不到玩家」的解释（2026-10-11）—— 交付 **v51**

**来源**：洛洛转达 **338 mods 环境（Tester_A喵）** 的三条反馈，附两份日志
（`<home>\Downloads\latest (1).log`、`mclo.gs/DU1IrQc`，后者本机副本 `<workspace>\.selftest\DU1IrQc.log`）：

1. 「你看渲染还把我手渲染上了（）」
2. 「它到底能不能渲染到玩家」／「我在单人存档试的时候渲染不到」
3. **「我点了自己，就是 replay 的那个换视角的功能 → 崩了」**

#### 〇、先确认好消息：**v50 在那个环境导出是好的**

`DU1IrQc` 显示该环境装的就是 **`replaymod-1.7.10-2.5.2-gtnh-v50`**，并且：

```
[15:24:55] Examining replaymod-1.7.10-2.5.2-gtnh-v50.jar to load
[15:33:45] [com.replaymod.render.ReplayModRender]: Starting …\.minecraft\ffmpeg\bin\ffmpeg.exe with args: … -s 1920x1080 -r 60 …
[15:33:47] [PboOpenGlFrameCapturer]: [replaycompat] video export: glReadPixels(long) resolved on org.lwjgl.opengl.GL11
[15:34:47] [com.replaymod.render.ReplayModRender]: Starting …ffmpeg.exe …        ← 第二次渲染也成功
```

**零 `NoSuchMethodError`、零 `Rendering video` 崩溃** ⇒ 12.24 的导出修复在 338 mods 环境复现成功 ✅

#### 一、★ 崩溃根因：MC 1.7.10 的 `updateFogColor`「检查用 A、取值用 B」，而 `func_82830_a` 没有 null 检查

崩溃原文（`latest (1).log` 最后 15:39:12；正是"点自己换视角"那一刻）：

```
[Client thread/FATAL]: Unreported exception thrown!
java.lang.NullPointerException: Cannot invoke "net.minecraft.potion.PotionEffect.func_76459_b()"
  because the return value of "net.minecraft.entity.player.EntityPlayer.func_70660_b(Potion)" is null
    at net.minecraft.client.renderer.EntityRenderer.func_82830_a(EntityRenderer.java:13580)
    at net.minecraft.client.renderer.EntityRenderer.func_78466_h(EntityRenderer.java:1802)   ← updateFogColor
    at net.minecraft.client.renderer.EntityRenderer.func_78471_a(EntityRenderer.java:1154)   ← renderWorld
    at net.minecraft.client.renderer.EntityRenderer.func_78480_b(EntityRenderer.java:1015)
```

**字节码证据（`srg_merged_minecraft.jar`，两处调用只有第二处是这条路）**：

```
EntityRenderer.func_78466_h:                       // updateFogColor(float)
   8: getfield Minecraft.field_71451_h ; astore_3  // local3 = mc.renderViewEntity
 918: aload_3 ; isPotionActive(Potion.nightVision) // ★ 守卫检查的是 renderViewEntity
 925: ifeq 1062
 933: getfield Minecraft.field_71439_g            // ★ 取值却拿 mc.thePlayer
 937: invokespecial func_82830_a(EntityPlayer,F)
```

```
EntityRenderer.func_82830_a(EntityPlayer,F):        // 该方法体内【没有】null 检查
   0: aload_1 ; getstatic Potion.field_76439_r     // Potion.nightVision
   4: invokevirtual EntityPlayer.func_70660_b      // getActivePotionEffect  → 可以是 null
   7: invokevirtual PotionEffect.func_76459_b()I   // getDuration            → NPE
```

⇒ **只要 `renderViewEntity != mc.thePlayer` 且前者有夜视、后者没有，就必崩。**
普通游戏里两者永远相同（第一人称），所以从不出事；
**而 ReplayMod 的「旁观某个玩家」正是唯一会把它俩拆开的场景** ——
GTNH 里"有夜视"太常见了（夜视护目镜 / 夜视药水 / IC2 头盔），被旁观的那位有、自己没 → 一点就崩。
**这不是 Angelica 的锅，也不在 ReplayMod 里，是 MC/Forge 自带缺陷**（另注：`mcpatcher` 的 `Lightmap.getNightVisionStrength`
与 Iris 的 `CommonUniforms` 也有同样的调用，但它们**各自都做了 `isPotionActive` 检查**，所以不是它们）。

#### 二、修复：`MixinEntityRendererNightVisionGuard`（v51）

在 private `func_82830_a` 的 **HEAD** 注入（`remap = false, require = 0`）：反射问一次
「传进来的这个 player 到底有没有夜视」，**没有就 `cir.setReturnValue(1.0F)`**，否则原样放行。

- 反射访问 `Potion.field_76439_r` 与 `EntityPlayer.func_70660_b`（本工程无 reobf → 坑 29；日志语句也在 try 内）。
- 为什么返回 **`1.0F`**：它是该方法**自己**在"夜视满效"时返回的合法值；**不用 0.0F 是因为后面存在除法**（`fconst_1 … fdiv`），0 会变成除零。
- 反射失败时**返回 true（不干预）**，即退化为原行为，绝不引入新问题。

#### 三、交付 v51（**取代 v50**）

| 项 | 值 |
|---|---|
| 文件 | **`replaymod-1.7.10-2.5.2-gtnh-v51-spectateguard.jar`** |
| 大小 / SHA256 | `10833255` B / `46B44B189042477F49445D15C26BDC6AFE07941E988C3E88E66D31C9E079CD2B` |
| 构成 | v48 + **3** 个新 mixin（entity tracker 防御 / PBO-glReadPixels / **夜视守卫**），共 **12 个功能 mixin** |
| 部署 | ⚠️ **尚未部署**（改包时洛洛的游戏还在跑、jar 被锁）—— 退出游戏后按第 7 节命令覆盖即可 |

**待验证**（我没法在本机"点自己换视角"而不打扰当前会话）：338 环境装 v51 后再点一次「换视角」，
判据 = **不再出现那行 `[Client thread/FATAL]: Unreported exception thrown!`**，
且日志里可能出现 `[replaycompat] night-vision guard: …`（限 3 条）。

#### 四、「渲染出手臂」与「看不到玩家」—— **不是 bug，是"旁观玩家视角"的上游设计**

Tester_A喵说的"换视角"= ReplayMod 的 **spectate** 功能：把 `mc.renderViewEntity` 指向**被旁观的玩家实体**。于是：

| 现象 | 上游代码 | 解释 |
|---|---|---|
| **渲染/回放里出现"手"** | `com.replaymod.render.mixin.MixinEntityRenderer#replayModRender_renderSpectatorHand`：**只有** `renderViewModRender_handler.omnidirectional`（360° 全景）时才 `ci.cancel()`；若 `renderViewEntity instanceof EntityPlayer && !(instanceof CameraEntity)` → **故意让手正常渲染** | 你在看"那个玩家的第一人称"，所以拍到的就是**他拿着东西的手**。ReplayMod 的语义如此 |
| **看不到玩家（人物模型）** | 同上：相机就在那个玩家**眼睛**位置 | 第一人称当然看不到自己的模型。**想拍到人 = 把机位拉到第三人称** |

**正确用法（给Tester_A喵）**：
1. 先在回放里用**鼠标/按键**把相机移到想要的位置（**相机实体的自由视角**，也就是不点任何实体、直接用 WASD/自由飞行），
2. 再在时间轴上**打位置关键帧**（`replaymod.gui.ingame.menu.addposkeyframe`「添加位置关键帧」），
3. 这样渲染出来的视频就是**第三人称**机位，能拍到人物、也没有手。

> 反过来：**只要你用了"旁观玩家"这个视角，手和"看不到自己"就是必然的**（上游行为，不是我们的补丁造成的）。
> 注意本机日志里 render 模块的 `MixinEntityRenderer` **确实成功注入**了
> （`Mixing MixinEntityRenderer from mixins.render.replaymod.json into net.minecraft.client.renderer.EntityRenderer`），
> 所以这不是"mixin 没生效"。

#### 五、★ 新增坑 52

**52. ★★ MC/Forge 1.7.10 的 `EntityRenderer.func_78466_h`（updateFogColor）里，
夜视守卫检查的是 `mc.renderViewEntity`（`field_71451_h`，字节码偏移 918），
取值却用 `mc.thePlayer`（`field_71439_g`，偏移 933）—— 两个字段一旦不同就可能 NPE。**
被调用的 private `func_82830_a` **内部没有 null 检查**（直接 `.getDuration()`）。
普通游戏里两者恒等所以永不触发；**只有"把 renderViewEntity 指向别的实体"的功能（ReplayMod 的旁观玩家）才会踩中**。
**教训**：排查"只有某个功能才崩"时，先怀疑**这种"检查对象 ≠ 使用对象"的不一致**，
而不是先怀疑 mod 冲突 —— 本轮从收到反馈到锁定根因只用了字节码比对，没有做任何二分法实验。
**另外**：`func_82830_a` 是 Forge patch 添加的方法（MCP 源码里没有），注入时用 SRG 名 + `remap = false`。

---

### 12.26 「路径关键帧（旁观者关键帧）真正修好」—— entity tracker 逐包容错（2026-10-11）—— 交付 **v52**

**背景**：12.24 给 `EntityPositionTracker` 加的补丁只是"**不再弹窗**"：它把整个 `loadFromPacketData`
包进 try/catch，坏包一来就**整段放弃** → tracker 是空的 → simplepathing 的**旁观者关键帧仍然失效**
（上游原话就是 "spectator keyframes will be broken"）。洛洛指出要**真正修好路径关键帧**。

**★ 关键发现：调用点本来就允许"这个包没有实体 ID"**

```
EntityPositionTracker.loadFromPacketData:
   99: invokestatic PacketUtils.getEntityId(Packet)Ljava/lang/Integer;
  106: ifnonnull 118        ← ★ 返回 null 是【合法】语义
  109: packet.release(); continue;   ← null 就跳过这一个包、继续下一个
```

所以**逐包**容错就能既保住数据又保住流程。

**修复（v52）**：`MixinEntityPositionTrackerSafeLoad` 新增两处 `@Redirect`：
- `PacketUtils.getEntityId(Packet)` → catch 任何 `Throwable`，**返回 null**（跳过该包，循环继续）
- `PacketUtils.updateLocation(Location, Packet)` → 同样 catch → 返回 null（调用点同样判空）

**已实测（本机，字节码导出，`-Dmixin.debug.export=true`）**：

```
EntityPositionTracker.loadFromPacketData:
   97: invokestatic redirect$zeb000$replaymod$replaycompat$safeGetEntityId:(Lcom/replaymod/replaystudio/protocol/Packet;)Ljava/lang/Integer;
  196: invokestatic redirect$zeb000$replaymod$replaycompat$safeUpdateLocation:(...Location;...Packet;)...Location;
EntityPositionTracker.load: 187 / 238 → safeLoadFromPacketData   （12.24 的那层兜底仍在）
```

并且正常录像不受影响：`[16:00:25] [GuiPathing]: Loaded entity tracker in 4ms` ✅

> ⚠️ **坏包本身在本机复现不了**（12.23 补充三已说明：与录像内容相关）。
> 我尝试用 `0x15`(EntityRelativeMove) 造了一个夹具 `replay_recordings\AAA-tracker-badpacket-test.mcpr`
> （插了 `[id][3 字节]` 的 4 字节包、重算了 crc32），**但它没有触发** —— 说明 `0x15` 并不在
> ReplayMod 的"含实体 ID"判定表里。夹具留着备用；**真正的验收要用 338 环境那条原录像**，
> 判据 = 不再出现 `Loading entity tracker:` + 栈，而是出现我们的
> `entity tracker: packet with an unreadable entity id was skipped (...)` 且 tracker 正常加载完。

---

### 12.27 「玩家不渲染，切换维度之后才出现」结案 —— SpawnPlayer 由**丢弃**改为**延迟重放**（2026-10-11）—— 交付 **v53**

**来源（Tester_A喵，338 环境）**：「**切换纬度之后玩家才渲染出来／没切换之前没有渲染**」。

**根因 = 12.13 那个"丢弃"的代价，被现场准确踩中了。**
`MixinNetHandlerPlayClientSpawnGuard` 在 `NetHandlerPlayClient.func_147237_a` 的 HEAD 检查
`clientWorldController`（`field_147300_g`）；回放**加载期**它是 null，于是 `ci.cancel()` **把包扔掉**。
服务端录像里那个"合成 SpawnPlayer"（2.7(1)）恰好就在加载期到达 ⇒ **玩家实体从来没被创建**；
直到录像里出现下一次 spawn（例如切换维度）才终于建出来 —— 与反馈逐字吻合。
这正是 **12.6-⑨** 早就写下的"更理想做法"。

**修复（v53）**：不再丢弃，改为交给 `NullWorldGuard.defer(packet)` 暂存；
已有的 `MixinNetworkManagerDeferredFlush`（注入 `NetworkManager.func_74428_b`）会在
`Minecraft.theWorld` 出现后把这些包**放回接收队列**，由 vanilla 正常派发。

- 为什么时机安全：flush 等的信号是 `Minecraft.theWorld`（`field_71441_e`，**加载结束时**才赋值），
  而 SpawnPlayer 需要的是 `clientWorldController`（由 `S01PacketJoinGame` 在**加载早期**创建）
  ⇒ 条件满足时前者已存在。
- 为什么"放回队列"而不是反射派发：不改变任何异常行为（12.21 的同一套机制）。

**已实测（本机，服务端录像 `server-2026-10-10_13-19-00.mcpr` 的副本 `AAA-spawndefer-test.mcpr`）**：

```
[16:00:27] [replaycompat] replay loading: holding S0CPacketSpawnPlayer until the client world exists (it will be replayed)
[16:00:28] [replaycompat] re-queued 1 deferred packet(s) now that the world exists
```

**并且截图确认：回放一开始（没切维度）画面里就能看到玩家实体**（`tools\shots\v53-04-f3.png`）✅
—— 之前担心的"`func_74428_b` 在加载完成后不再被调用、requeue 永不发生"**没有发生**。

#### 交付 v53（**取代 v51/v52**）

| 项 | 值 |
|---|---|
| 文件 | **`replaymod-1.7.10-2.5.2-gtnh-v53-spawndefer.jar`** |
| 大小 / SHA256 | `10833703` B / `274D2FD28B4A9CAB429790B92379DB4EAFC53E9D3044F0E63FF2AE410B170A6E` |
| 构成 | v48 + 4 个新 mixin（entity tracker 逐包容错 / PBO-glReadPixels / 夜视守卫 / **SpawnPlayer 延迟重放**），共 **12 个功能 mixin** |

#### ★ 新增坑 53

**53. ★★ "丢弃（drop）"和"延迟（defer）"在回放加载期**是完全不同的两件事，能延迟就**绝对不要丢弃**。
丢弃的问题不是"少一个包"，而是**它的后果要很久以后才暴露**：12.13 丢的是加载期的 `SpawnPlayer`，
表现却是"回放里看不到玩家，**换个维度**才出现"——从现象几乎不可能反推到"某个包被扔了"。
本项目已经有一套现成的延迟设施（`NullWorldGuard.defer` + `MixinNetworkManagerDeferredFlush` 的 requeue），
**新写加载期守卫时应默认走 defer**，只有"纯瞬时视觉/音效"（`DROP_WHEN_WORLD_MISSING` 那 7 类）才允许丢。
另注：`require = 0` 的注入失败是静默的，所以**改完守卫一定要去看那两条日志**
（`holding … (it will be replayed)` 与 `re-queued N deferred packet(s)`）来确认它真的在工作。

#### 五、★ 新增坑 50（本次踩到，性质与坑 29 同源但发生在**别人**的兼容层里）

**50. ★★ GTNH 的 `GLSMRedirector` 会把 `org.lwjgl.opengl.*` 调用点无条件改写成 Angelica `GLStateManager` 的同名方法，
而 Angelica 并未实现全部重载 —— 缺失的那个就会变成运行期 `NoSuchMethodError`（不是编译期、不报在启动，只在你真跑到那行时才炸）。**
实测缺口：`GLStateManager.glReadPixels` 只有 `ByteBuffer`/`FloatBuffer`/`IntBuffer` 三版，**没有 `(IIIIIIJ)V`（long）**。
**排查姿势**（本轮已验证有效，工具留在 `.selftest\GLCallAudit.java`）：
拿被适配 jar 的**全部** `org/lwjgl/opengl/*` 调用点去和 `angelica-*.jar` 里
`com/gtnewhorizons/angelica/glsm/GLStateManager` 的方法表比对，**只看"同名不同签名"**那一类
（"完全同名同签名"=安全；"名字都不存在"多是没被重定向的扩展调用，如 `GL43C`/`KHRDebug`，多为假阳性）。
**推论**：以后任何 ReplayMod 模块"点了没反应/跑到某处必崩"，先跑一遍这个审计，比人肉猜快得多。

---

### 12.28 「打开回放就是玩家视角 + 前进方向错乱 + 无法退出旁观」结案（2026-10-11）—— 交付 **v56**

**来源（洛洛）**：三条反馈 —— ① **打开回放、什么都不点就已经是玩家视角**；② **有一次前进变成了其他方向**；③ **无法退出旁观**。

**结论：这是两个独立缺陷**，一个在我们自己的补丁里，一个在上游的初始化缺口里。

#### 一、★ 我们 12.23 的鼠标补丁把上游的「退出旁观」键抢走了（→ 症状 ②③）

上游的"退出旁观、回到自由相机"就是 **Shift**（`keyBindSneak` / `field_74311_E`）：

```
SpectatorCameraController.update(float):
     4: mc.gameSettings.field_74311_E          // keyBindSneak
    11: KeyBinding.func_151468_f()              // isPressed()
    14: ifeq 26
    23: ReplayHandler.spectateCamera()          // ★ Shift = 退出旁观
```

而 12.23 我们把 Shift 拿来当"临时放开鼠标"，并且在每次切换后**写回按键状态**。两个后果正好对上反馈：

| 反馈 | 机制 |
|---|---|
| **无法退出旁观** | `isPressed()`(`func_151468_f`) 是**消费式**的（`pressTime`），我们的写回/清键让它读不到 ⇒ `spectateCamera()` 永不触发 |
| **前进方向错乱** | `setMouseVisible(true)` → `displayGuiScreen` → **`unPressAllKeys()` 清空所有 KeyBinding（含 WASD）**（坑 48），那一下的移动输入就乱了 |

**修复（v56）**：Shift **完全还给上游** —— 鼠标切换只看 `ctrlToggle`，不再把 Shift 计入判断，也**不再写回 Shift 状态**。
Ctrl 单独就够（回放里它没有别的含义，而且不用一直按着）。

**修好后的键位（写进发布说明）**：

| 键 | 作用 |
|---|---|
| **Ctrl**（点一下） | 锁定/放开鼠标可见（我们的功能） |
| **Shift** | **退出旁观、回到自由相机**（上游行为，现在恢复可用） |

#### 二、回放打开时没人把 `renderViewEntity` 设成回放相机（→ 症状 ①）

**字节码证据（整个 jar）**：
- 对 `Minecraft.field_71439_g`（thePlayer）**只有 `getfield`，没有任何 `putfield`**；`MCVer` 里也**没有** `setThePlayer`
  ⇒ `ReplayHandler#isCameraView()`（要求 `mc.thePlayer instanceof CameraEntity`）**在 1.7.10 上永远是 false**；
- 对 `Minecraft.field_71451_h`（renderViewEntity）的写入者只有 `ReplayHandler.spectateEntity` 与 `CameraEntity` 的旁观逻辑，
  而 `spectateEntity` **只被"点击实体 / 玩家总览 / 旁观者关键帧"触发**，`ReplayHandler` 的构造函数里**没有任何**设置相机的代码。
⇒ 回放刚打开时 `renderViewEntity` 保持 MC 上次赋的值（本地玩家），于是第一人称、有手、看不到自己。

**修复（v55 起）**：新增 `MixinCameraEntityEnsureView` —— 在 `CameraEntity.func_70071_h_` 的 TAIL 每 tick 检查一次：
**不处于旁观状态（`spectatedUUID == null`）且 `renderViewEntity != 相机实体` 时，把它指回相机实体**
（语义等价于上游的 `spectateCamera()`，只是改成自动）。旁观状态**完全不干预**，所以"点实体换视角"照旧可用。

> ⚠️ 记一个坑：`CameraEntity` 的方法名是 **`func_70071_h_`（带尾随下划线）**，不是 `func_70071_h`。
> 我第一版写错了，`require = 0` 让它**静默不生效**（v54 白测一轮）。现在两个拼写都写进 `method = {...}`。

#### 三、排除项：**不是**录像里的关键帧

一度怀疑是"旁观者关键帧"（`SpectatorProperty`）导致打开即旁观。实测**排除**：本机唯一两条带关键帧数据的录像
（`2026_10_10_17_19_22.mcpr` / `2026_10_10_16_56_54.mcpr` 里的 `timelines.json`）中，
**位置/旁观轴的关键帧列表都是空的**（`"keyframes":[]`），只有 `16_56_54` 里有一个 `timestamp` 时间同步关键帧。

#### 四、v56 实测（本机，2026-10-11 16:22）

- `Loaded entity tracker in 6ms` ✅
- `holding S0CPacketSpawnPlayer … (it will be replayed)` → `re-queued 1 deferred packet(s)` ✅（12.27 仍有效）
- **`camera view` 一条日志都没有** ⇒ `renderViewEntity == 回放相机`，**相机本来就是对的**（12.28 二的修复已生效）✅

| 项 | 值 |
|---|---|
| 文件 | **`replaymod-1.7.10-2.5.2-gtnh-v56-freeshift.jar`** |
| 大小 / SHA256 | `10836006` B / `9A0B2AB3D385E290A1BD0613EB91634E5C47FC55B4D4A0025B765052DB0F197A` |
| 构成 | v48 + **6** 个新 mixin（tracker 逐包容错 / PBO-glReadPixels / 夜视守卫 / SpawnPlayer 延迟重放 / e**相机实体确保 / 鼠标补丁只留 Ctrl**），共 **13 个功能 mixin** |

#### ★ 新增坑 54

**54. ★★ 给回放做输入补丁前，先查上游拿这个键干什么。**
我们为了让鼠标可用，把 **Shift（`keyBindSneak`）** 抢来做开关 —— 而它正是上游
`SpectatorCameraController.update` 里"**退出旁观**"的触发键（`isPressed()`）。抢键 + 状态写回的结果是：
**玩家永远退不出旁观**，而且每次切换鼠标都会经 `displayGuiScreen → unPressAllKeys()` **清空所有按键（含 WASD）**，
表现为"**前进方向/移动偶尔错乱**"。**选键三原则**：① 上游没用；② 别的 mod 没占用；③ 不涉及"一直按住"。
**通用推论**：任何调用 `displayGuiScreen`（从而 `unPressAllKeys`）的补丁都会**顺手清掉 WASD/跳跃**，
所以**绝不要在"玩家可能正在走路/飞行"的时刻频繁切换 GUI**；也要避免为此写回其它键的状态。

**★ 另一个同类坑**：`CameraEntity` 的 `onUpdate` 在 jar 里叫 **`func_70071_h_`（尾下划线）**；
写成 `func_70071_h` 时 `require = 0` **静默不生效**，看起来"补丁没写一样"。**改完 mixin 一定要去日志里找它自己的输出**（或做字节码导出）确认真的跑到了。

---

### 12.29 「回放里还显示原版 HUD（血条）」结案（2026-10-11）—— 交付 **v60**（⚠️ HUD 部分**尚未实测**）

**来源（洛洛）**：截图 + "还是要界面问题" —— 回放里底部**一条原版血条**（❤×10）还在。

#### 一、★ 根因：GTNH 上 **`RenderGameOverlayEvent.HEALTH` 根本不会被发出**

ReplayMod 隐藏 HUD 的方式是取消 `RenderGameOverlayEvent.Pre`：

```java
// CameraEntity$EventHandler#preRenderGameOverlay
case ARMOR: case HEALTH: case FOOD: case AIR: case HOTBAR:
case EXPERIENCE: case HEALTHMOUNT: case JUMPBAR:
    event.setCanceled(true);
```

**探针实测（v59-diag，注入 `CameraEntity$EventHandler#preRenderGameOverlay` 的 HEAD 打印 `event.type`）**：

```
reached: ALL, HELMET, PORTAL, BOSSHEALTH, FOOD, AIR, EXPERIENCE, TEXT, CHAT, CROSSHAIRS
缺：     HEALTH        ← ★ 从来没有出现过
```

⇒ handler **活着、也确实在取消**（FOOD/AIR/EXPERIENCE 等），但**血条的 `HEALTH` 事件在这套整合包里不会被 post**。
本机 `GuiIngameForge` 的血条由它自己那组
`public static boolean renderHealth / renderArmor / renderFood / renderAir / renderExperiance / renderHotbar / renderHealthMount / renderJumpBar`
开关驱动，**没有 ReplayMod 可以取消的东西**。

顺带**排除**了两条早期猜测（都实测过）：
- 不是 Angelica 的 HUD 缓存把事件吞了 —— 虽然 `HUDCaching.renderCachedHud` 确实会走"非缓存分支 vs blit 缓存"
  （`AngelicaConfig.hudCachingActive` 默认 `false`），而且 `HUDCachingEarlyReturnTransformer` 会给**所有**该事件的接收者注入
  `if (HUDCachingHooks.shouldReturnEarly()) return;`；但探针证明 handler 其实收得到事件（说明这层已经通了）。
  仍然保留两个补丁（`MixinHudCachingReplayPassthrough` 让 `shouldReturnEarly()` 在回放中为 false、
  `MixinHudCachingReplayNoCache` 让 `AngelicaConfig.hudCachingActive` 在回放中读成 false），
  它们是**让事件能到达 handler 的前提**，去掉就会退回"FOOD/AIR 也拦不住"。

#### 二、修复：回放中直接不渲染原版 HUD（`MixinHideHudInReplay`）

`@Mixin(targets = "net.minecraftforge.client.GuiIngameForge")`，
在 `func_73830_a`（= `GuiIngameForge.renderGameOverlay`，Forge 类不 remap、方法名是 SRG）的 HEAD：
**当 `renderViewEntity instanceof CameraEntity` 时 `ci.cancel()`** —— 整个原版 HUD 不渲染
（血条/饥饿/护甲/氧气/经验/热键栏/准星/聊天/F3 文本）。

- **ReplayMod 自己的 UI（时间轴、按钮、速度）是 jGui 独立 overlay，完全不受影响**；
- 非回放状态一切照旧；
- 代价：这一步比上游"逐元素取消"更狠 —— **聊天和 F3 调试屏在回放中也会一起消失**。
  若洛洛希望保留聊天/F3，改用"只关 `GuiIngameForge.renderHealth/renderArmor/renderFood/renderAir/renderExperiance/renderHotbar/renderHealthMount/renderJumpBar`"即可（同一注入点，改几行），
  但那几个字段在 `renderGameOverlay` 里**有些是被每帧重新赋值的**（例如 `renderHealthMount = ridingEntity instanceof EntityLivingBase`），
  所以"关开关"未必可靠 —— **cancel 整个方法才是稳的那条**。

#### 三、交付与验证状态（**如实**）

| 项 | 值 |
|---|---|
| 文件 | **`replaymod-1.7.10-2.5.2-gtnh-v60-hudhidden.jar`** |
| 大小 / SHA256 | `10840248` B / `EC01410C1E069A765AAF2B669EC916594A68A62923231F9576135A5D1FB1D01D` |
| 构成 | v48 + **8** 个新 mixin（共 **16 个功能 mixin**） |
| 部署 | ✅ 已覆盖 `mods\replaymod-1.7.10-2.5.2-gtnh.jar` |
| 验证 | **手：已由洛洛确认「手消失了」** ✅（HUD 部分按洛洛裁定回退，见 12.30 二） | ⚠️ 原始备注：**HUD 部分未实测** —— v60 的构建/部署都完成了，但测试那一轮游戏被关掉了（日志 16:48:14 `SoundSystem shutting down`，**无 crash、无 mixin 报错**），所以"血条是否消失"还没有人眼确认。**其余 7 项修复都是已验证过的。** |

#### ★ 新增坑 55

**55. ★★ 不要假设"某个 Forge 事件一定会被发出"。**
ReplayMod 靠 `RenderGameOverlayEvent.Pre(HEALTH)` 隐藏血条，逻辑本身没问题、handler 也确实在跑，
但**这套整合包里 `HEALTH` 事件从不出现**（实测：同一 handler 收到 ALL/HELMET/PORTAL/BOSSHEALTH/
FOOD/AIR/EXPERIENCE/TEXT/CHAT/CROSSHAIRS，**独独没有 HEALTH**），于是"取消血条"这件事**永远做不到**。
**排查姿势**：不要停在"handler 没被调用 vs 被调用了"这一层，而要**把事件类型打出来**
（本轮就是靠给 ReplayMod 自己的 handler 打 `event.type` 才一步定位）。
**兜底原则**：当某个事件驱动的隐藏/取消不生效时，**直接控制渲染入口**（这里是 `GuiIngameForge.renderGameOverlay` 的 `ci.cancel()`）
比继续追事件链更可靠；代价要如实写在文档里（本例会连聊天/F3 一起隐藏）。

---

### 12.30 「手」的修复 + **UI 那一堆尝试全部回退**（2026-10-11）—— 交付 **v65**

#### 一、手：改打**确定存在**的入口（`ItemRenderer.renderItemInFirstPerson`）

ReplayMod 用 `CameraEntity$EventHandler#onRenderHand` 取消 `RenderHandEvent` 来藏手，但在这套包上同 `HEALTH` 一样不可靠。
第一次尝试注入 `EntityRenderer.renderHand`，**方法名是我猜的 `func_78476_a`，jar 里根本不存在** ⇒ `require = 0` 静默失效（"手还在"）。
**正确入口**（由 MCP 源码参数名反推、并在 srg jar 中核对）：

| MCP | SRG |
|---|---|
| `ItemRenderer.renderItemInFirstPerson(float p_78440_1_)` | **`func_78440_a`** |
| `ItemRenderer.renderOverlays(float p_78447_1_)` | **`func_78447_b`** |

`MixinNoFirstPersonHandInReplay` 现在注入这两个，条件与上游语义一致：
**自由相机 → 取消；正在旁观某个玩家 → 放行**（那时该看到他的手/手持物）。

#### 二、★ UI 的整段弯路（v57→v64）与**回退决定**

起因是"回放里还显示原版 HUD（血条）"。过程与结论：

1. **`HEALTH` 事件不发**（12.29 的探针结论）⇒ ReplayMod 的 cancel 拿不到它；
2. **v57/v58**：让 `HUDCaching.shouldReturnEarly()` 与 `AngelicaConfig.hudCachingActive` 在回放中失效 —— 这两条**确实让事件能到 handler**（有益），但**治不了 HEALTH**；
3. **v60**：改成 `ci.cancel()` 整个 `GuiIngameForge.renderGameOverlay` ⇒ **连 ReplayMod 自己的时间轴/按钮也没了**（"UI 全部消失"）。
   原因：jGui 的 overlay 也从 `renderGameOverlay` 内部那个 `RenderGameOverlayEvent` 分发里画；
4. **v61~v64**：改成逐元素取消（血条/饥饿/护甲/氧气/经验/热键栏/跳条/Boss血条/F3文本），并试图按"自由相机 vs 旁观玩家"区分 —— 但**方向与用户预期相反了一半**：
   真正要的是「**旁观某个玩家时，显示那个玩家的血条/快捷栏/UI**」，而不是"回放里一律隐藏"；
5. **用户裁定：回退** —— "回退到处理 ui 之前，把手的那个保留"。

**v65 因此 = 12.28 的 v56（相机 + Shift 归还）+ 手的修复**，UI 相关的三个补丁
（`MixinHideHudElementsInReplay` / `MixinHudCachingReplayNoCache` / `MixinHudCachingReplayPassthrough`）
**全部摘除**（源码留在 `*.java.disabled` 备查）。

> **UI 的现状（回退后的诚实描述）**：回到上游原始行为 —— ReplayMod 会取消它能收到的那几项
> （FOOD / AIR / EXPERIENCE 等），**血条仍然显示**（GTNH 不发 `HEALTH`）。**这一点是已知且被接受的**。
> 另外 **JourneyMap 的小地图**与 **InGameInfoXML 的左上角信息**是那两个 mod 自己画的，
> 上游与我们都不碰它们（要关请用它们自己的开关）。

#### 三、1.7.10 的结构性限制（**别再重复尝试**）

"旁观谁就显示谁的血条"在 **1.7.10 版 ReplayMod 上做不到**，因为：
**整个 jar 里对 `mc.thePlayer` 只有 `getfield`，没有任何 `putfield`，`MCVer` 也没有 `setThePlayer`**
⇒ 血条/饥饿/氧气读的永远是**回放客户端那个玩家**，而不是被旁观者。
（快捷栏例外：`onRenderHandMonitor` 会把被旁观者的 `inventory.getCurrentItem()` 推进 `ItemRenderer`，所以**快捷栏是真的**。）
若将来真要"那个玩家的血条"，唯一干净的路子是**自己覆盖式渲染一层**（只读被旁观者数据，不改 `mc.thePlayer`）。

| 项 | 值 |
|---|---|
| 文件 | **`replaymod-1.7.10-2.5.2-gtnh-v65-handonly.jar`** |
| 大小 / SHA256 | `10837941` B / `907B72B8B8D3EA638B24C5CD0C4E50F7481C4A919A7F97C66E774012CD545FCC` |
| 构成 | v48 + 5 个新 mixin（tracker 逐包容错 / PBO-glReadPixels / 夜视守卫 / SpawnPlayer 延迟重放 / 相机实体确保 / **手**）—— 共 **14 个功能 mixin** |
| 部署 | ✅ **已覆盖 `mods\replaymod-1.7.10-2.5.2-gtnh.jar`**（SHA256 `907B72B8…`，已核对一致） |

#### ★ 新增坑 56

**56. ★★ 不要"接管"别人的整块渲染，也不要替用户决定界面该有什么。**
两次教训叠在一起：① v60 直接 `ci.cancel()` 掉 `GuiIngameForge.renderGameOverlay`，
以为只关掉原版 HUD，结果**把 ReplayMod 自己的时间轴也一起干掉了** —— 因为 jGui 的 overlay 就挂在那个方法内部的事件分发上。
**推论**：在 `renderGameOverlay` 这一层，"关掉一个元素"和"关掉整个界面"只差一行，**必须先确认谁还依赖这个入口**
（`grep` 一下谁监听 `RenderGameOverlayEvent`）。
② 更难的一项：**动手前先问清"你要什么"**。用户要的是"旁观某个玩家时显示他的 UI"，
我却实现成"回放里一律隐藏 UI" —— 代码每一版都能编译、日志也没报错，但**方向错了**，白改五轮。
**UI 类需求，先问一句"什么时候该显示、什么时候该隐藏"，再动手。**

---

### 12.31 服务端「实体包漏录」：旧判读被字节级证据推翻 + 诊断已就绪（2026-10-11，**洛洛指示暂停于此**）

> 本轮**没有改动任何 jar、没有部署、没有动 `mods\`**；唯一落盘的代码改动是**服务端 mod 的诊断代码**（已编译通过）。
> 交付版本仍是 **v65**（`907B72B8…`），`mods\replaymod-1.7.10-2.5.2-gtnh.jar` 与其一致。

#### 一、★ 先用同一份录像把现象钉死（可复现，命令见第五节）

取**同一次会话**的两份录像（`date` 只差 57 ms）重新解析：

| 包 | 客户端 `2026_10_10_13_19_00.mcpr` | 服务端 `server-2026-10-10_13-19-00.mcpr` | 判定 |
|---|---|---|---|
| `0x15` EntityRelativeMove | 673 | **673** | **payload 多重集逐字节完全一致** ⇒ 一个没丢 |
| `0x26` / `0x23` / `0x03` / `0x3f` | 125 / 29 / 29 / 5346 | 完全相同 | ✅（与 12.14 的结论一致） |
| `0x19` EntityHeadLook | 551 | 498 | 少 53 |
| `0x12` EntityVelocity | 1332 | **96** | 真丢 |
| `0x17` EntityLookMove | 1246 | **23** | 真丢 |
| `0x25` BlockBreakAnim | 441 | **0** | 真丢 |

**两条独立现象（这才是真成因，12.6-⑤ 的"服务端在发、我们没拿到"过于笼统）**：

1. **服务端录像在 `t=5s~20s` 整整 15 秒零包**（`mcpr-census.ps1` 实测三个 5 秒桶 `total=0`），
   而客户端录像在同一区间有 **4322** 个包；两边时间轴**已用 `0x0f`/`0x20` 校准过**（都在 `t≈1.1s`，差约 70 ms）
   ⇒ 不是时间轴错位。
2. **丢的是"三类高频实体包"**（`0x12`/`0x17`/`0x25`，外加 53 个 `0x19`），
   而 `0x15`/`0x26`/`0x3f` 等**完全一致**；服务端录到的**全部包**（除我们合成的 `SpawnPlayer`）
   都能在客户端录像里找到同 payload 副本 ⇒ **服务端录像是客户端的真子集，是真丢包**。
   总数也对得上：客户端 9990 − 服务端 7008 = **2982** ≈ 1236 + 1223 + 441 + 53 + 零头。

#### 二、★ 离线穷举排除（全部有字节码证据，下一位别再查这些）

| 假设 | 结论 | 证据 |
|---|---|---|
| 1.7.10 服务端还有"另一条发包路径" | ❌ **排除** | 字节码全扫：服务端侧引用 `func_150725_a` 的只有 `NetHandlerPlayServer`，而 `func_147359_a` 的唯一出口就是偏移 44~48 的 `field_147371_a.scheduleOutboundPacket` |
| 配置把实体包过滤掉了 | ❌ **排除** | `config/serverreplay.json` 全部 `ignore*Packets=false`；`ReplayConfig#isIgnoredPacket` 只认 5 个显式开关（0x02/0x28-0x2A/0x3B-0x3E/0x3F） |
| ReplayStudio 写入端会丢包 | ❌ **排除** | `ReplayOutputStream.doWrite` 只更新 `duration`，无任何丢弃分支 |
| 我们查错了包表（`func_150754_b` 语义反了） | ❌ **排除** | `func_150754_b(false)` → `func_150755_b()` → `field_150770_i`，而 `func_150756_b`（`EnumConnectionState$4` 里 89 次注册）填的正是它；`S12/S17/S25` 各注册一次 |
| Hodgepodge 有"本地直投"优化 | ❌ **排除** | `hodgepodge-2.7.196.jar` 内 Network/Packet 相关类逐个看过，无此类 |

⇒ **唯一自洽的剩余解释：`func_150725_a` 的 HEAD 注入在那段时间（以及其它时段）没有执行 / 提前 return**，
而"为什么"必须靠**实机带诊断**一次问清楚（离线已到极限）。

#### 三、已完成（**未部署**）：服务端 mod 加了三层诊断

`ServerReplay-1.7.10`（Gradle `BUILD SUCCESSFUL`，1m04s；改的是源码，`build/libs` 已更新但**没有拷进任何 mods**）：

| 诊断 | 位置 | 作用 |
|---|---|---|
| `ReplayRecorder.noteCapturePoint()` | `MixinNetworkManager` 的 HEAD（`isCaptureEnabled()` 之后） | 采集点 **≥1500 ms** 无包就 `[diag] capture gap: no outbound packet reached the recorder for N ms` ⇒ **直接区分"注入没跑"与"包被过滤"** |
| `ReplayRecorder.noteUnmappedPacket(String)` | 三处静默 return：`state == null` / 状态非 LOGIN·PLAY / 类不在包表 | `[diag] outbound packet had no replay packet id: …`（限流：每类前 3 条 + 每 500 条一次） |
| 逐 id 普查 | `write()` 计数，`finish()` 打印 | `[diag] stored packet ids: 0x12=… 0x17=… 0x25=…` + 未映射类汇总 |

> 日志**默认就开**（都是异常信号 + 限流），实测时不需要额外开关；`ReplayConfig.debug` 字段仍未使用。

#### 四、★ 下次继续的入口（**就一步：实机复现**）

1. 确认游戏已退出；把 `ServerReplay-1.7.10-0.1.0-client.jar`（**无 shade**，见勘误区 #1）放回 `mods\`；
2. 启动单人世界，**正常走动/打怪 40~60 秒**（要让 `0x12`/`0x17`/`0x25` 真的产生），退出世界；
3. 判据（按顺序看）：
   - 有没有 `[diag] capture gap: …` ⇒ **有** = 注入在那段时间根本没被调用（问题在 mixin/加载器层）；
     **没有** = 注入在跑，问题在"包没到达 `func_150725_a`"或"被静默 return"；
   - 有没有 `[diag] outbound packet had no replay packet id: …` ⇒ 直接指出是哪个类、哪张表；
   - `[diag] stored packet ids:` 与服务端录像直方图互相印证；
4. 与**同一次会话**的客户端录像对比（用下面三个脚本），确认是否复现"15 秒空窗 + 三类包丢失"。

#### 五、新增的可复用工具（`.selftest\`，纯离线）

| 脚本 | 用途 |
|---|---|
| `mcpr-timeline.ps1` | 指定 id 的**每桶（默认 5 s）时间分布** + 首末时间戳 ⇒ 用来发现"空窗" |
| `mcpr-census.ps1` | **全 id 每桶普查**（总数 + top id）⇒ 一眼看出"整段空白 vs 选择性缺失" |
| `mcpr-dump.ps1` | 逐包导出 `timestamp<TAB>id<TAB>md5(payload)` ⇒ 两份录像做**多重集比对**，判定"真丢包 vs 时间戳错位" |

> 踩坑记录：`$pid` 在 PowerShell 里是**只读自动变量**，脚本里别拿它当变量名（本轮连炸一屏）。

#### 六、待洛洛决策（已问，洛洛选择"先暂停"）

- **①** v65 发到 338 mods 环境：zip 在 `<workspace>\release\GTNH-ReplayMod-v65.zip`（`9735848` B），
  **交付方式未定**（我无法直推文件到那台机器）；
- **②** 是否允许实机诊断（占屏 3~5 分钟，`run-test` 类脚本会 kill 所有 java）。

---

### 12.32 服务端 mod 瘦身重构：删掉"不必要的功能"（2026-10-11，洛洛提议「参考本地的、去掉服务端不必要的功能」）

> **动机**：服务端 mod 从 10-10 那几轮实验里积下了两坨包袱 ——
> ① **客户端的活早就搬进 ReplayMod 适配包了**（12.12 的入口、12.13 的 EtFuturum），ServerReplay 里那三份是**重复 + 死代码**；
> ② 配置里有一批**"模板里写着、代码里从来没读"的开关**，等于对使用者说谎。
> 洛洛一句话点题，本轮只做"减法"，**不新增任何功能**。

#### 一、删了什么（每一项都写明"为什么安全"）

| # | 删除项 | 为什么安全 |
|---|---|---|
| 1 | `mixin/MixinGuiCustom.java` | 主菜单「回放中心」入口现在由适配包的 `MixinGuiCustomReplayEntry` 提供（12.12）。**两份都在时反而可能重复加按钮**，而且服务端机上根本没装 CMM |
| 2 | `mixin/MixinGuiFakeMain.java` | **早就实测确认永不触发**（6.1 / 勘误区 #17：CMM 真正显示的是 `GuiCustom`），一直挂在配置的 `client` 段里当死代码 |
| 3 | `mixin/MixinEtFuturumPlayer.java` | 适配包里那份才是生效的；**服务端这份连类名大小写都是错的**（`EtfuturumPlayer`，真名 `EtFuturumPlayer`，勘误区 #18），从未应用过 |
| 4 | `mixins.serverreplay.json` 的整个 `client` 段 | 上面三个的宿主；删掉后服务端 mod 变成**纯服务端 mixin mod**，装到专用服务端上不会再带客户端语义 |
| 5 | `ReplayConfig`：`recordingBlacklist` + `isBlacklisted()` | **`isBlacklisted` 从未被任何代码调用** ⇒ 黑名单功能根本没实现 |
| 6 | `ReplayConfig`：`systemStorageLowLimitMb` | 磁盘空间守卫**未实现**（`ReplayRecorder` 里没有任何引用） |
| 7 | `ReplayConfig`：`recoverUnsavedReplays` / `deleteReplaysAfterDays` / `logDeletedReplays` | 三个 housekeeping 开关**全部未实现**。另注：半截的 `<name>.mcpr.tmp` 客户端本来就能自己救回（**12.20**），清旧文件交给运维脚本更合适 |
| 8 | `ReplayConfig`：`debug` | 声明了但**没有任何读取者**（12.31 的诊断故意做成默认开启 + 限流，不依赖它） |
| 9 | `ReplayRecorder`：`isRecording()` / `getPacketCount()` / `getConnection()` / `getFile()` + `connection` 字段 | 全仓库**零调用者**的公开成员；`connection` 字段只剩构造器赋值、无人读 |
| 10 | `ReplayConfig.fileNameFormat` 的错误 javadoc | 原文写"`{timestamp}`、`{username}`、`{uuid}` 会被替换"，实际**只实现了 `{timestamp}`**（`ReplayRecorder#newFile`）⇒ 改成诚实描述 |

**保留不动**（都是活的、或有意为之）：`automaticallyRecord` / `outputDir` / `fileNameFormat` / `serverName` /
`maxFileSizeMb` / `maxDurationMinutes`（`exceedsLimits` 真的在用）/ 四个 `ignore*Packets`（`isIgnoredPacket` 在用）/
`/serverreplay status|list|stop|reload` 四个子命令（都实现）/ 两个 jar 的 `clientJar` 任务（见第三节）。

#### 二、实测结果（全部实测，不是推断）

| 项 | 结果 |
|---|---|
| 构建 | `gradle build` **BUILD SUCCESSFUL in 13 s**（Gradle 9.4.1 + JDK 25 运行） |
| **编译警告** | 从 **3 条降到 0 条** —— 那 3 条全是这三个客户端 mixin 带来的（`EtfuturumPlayer could not be fully resolved` + 两条 `target is public and should be specified in value`）。**这本身就是"它们不该在这儿"的直接证据** |
| `reobfJar` / `clientJar` | 都正常产出 |
| jar 内 `com/gtnh/serverreplay/**` | 只剩 **5 个类**：`ReplayRecorder`（+2 匿名 +`PendingPacket`）、`ReplayCommand`、`ReplayConfig`、`MixinMinecraftServer`、`MixinNetworkManager`；**客户端 mixin 残留 = 0** |
| `mixins.serverreplay.json`（jar 内） | 只剩 `MixinNetworkManager` + `MixinMinecraftServer` |
| 体积 | 含 shade 版 `3553980 → 3547398` B；client 版 `28940 → 22438` B（各 −6.5 KB） |
| **离线自测** | `ServerReplaySelfTest` **SUCCESS**：`meta mcVersion=1.7.10 protocol=5 fileFormat=MCPR v=14`、`readback 5 packets`、staging 目录被清干净 ⇒ shade 闭包与 mcpr 往返**未被瘦身破坏** |
| 部署 | **没有任何 jar 被拷进 `mods\` 或 `mods-disabled\`**；v65 客户端交付物与 `mods\` 一点没动 |

#### 三、留着没动的（含理由，下一位别再纠结）

1. **`com/gtnh/serverreplay/Tags.class`** —— RFG 的 `injectTags` 任务按 `build.gradle.kts` 生成，
   注释原本写着"供 `@Mod` 注解引用版本号"，而**本 mod 没有 `@Mod` 类** ⇒ 它是**无人引用的死类**。
   **不动**：只有几百字节，而 `injectTags` 是 RFG 构建链的一环，动它的收益不值得冒构建链风险。
   已把那行注释改成诚实描述（含"没有调用者"这一实测结论）。
2. **两个 jar（含 shade 3.5 MB / 不含 shade 22 KB）** —— 见勘误区 #1 / 坑 27：专用服务端必须用含 shade 的那个
   （那边没有 ReplayMod 提供 ReplayStudio），**单人游戏/集成服务端**则必须用不含 shade 的，否则类遮蔽会让回放画面错乱。
   **都留**：client 版是 12.27 / 12.31 所有单人环境实测的前提。
3. **上游有、我们没有的**：`RecorderNotifier`（聊天广播）/ `RecorderRecoverer` / `ReplayCleanerUpper` /
   `RecorderWarner` / HTTP 下载拦截器 / player-predicate 树。**本轮一律不补**：
   1.7.10 上收益低，而**唯一的核心缺口仍是 12.31 的漏录**（先把录制做对，再谈锦上添花）。
4. `mcmod.info` —— 纯元数据；FML 因为没有 `@Mod` 类本来就会把它当 "non-mod file"（坑 8 的已知现象），留着无害。

#### 四、★ 一句总结给下一位

服务端 mod 现在的定位很干净：**"纯 mixin 的服务端录制器"** —— 抓 `NetworkManager.func_150725_a` 写 mcpr（`MixinNetworkManager`）、
管生命周期与命令注册（`MixinMinecraftServer`）、配置只有真正生效的 9 个字段。
**它不再承担任何客户端 UI 职责**（那些全在 ReplayMod 适配包里）。下一步仍是 **12.31 的实机诊断**。

---

### 12.33 两件事都已"排好"，等一句话就跑（2026-10-11，**洛洛说"可以排好"**）

> **状态：全部就绪，全部未执行。** 本节的产出是**工具与文件**，不是实验结果。
> `mods\` 里的 v65 与 `mods-disabled\` 都没被碰过。

#### 一、① 实机诊断：编排器 `tools\gtnh-replaymod-serverdiag.ps1`

把 12.31 的"实机复现 + 读诊断日志"做成分阶段、可中断、有人眼兜底的脚本（**不替你决定**，每步一个开关）。

```powershell
# 只看计划（只读 preflight，不部署不启动）——本轮已干跑验证：exit 0
powershell -NoProfile -ExecutionPolicy Bypass -File <workspace>\tools\gtnh-replaymod-serverdiag.ps1

# 正式一轮（部署诊断版 → 启动 → 你手动进世界玩 45 秒 → 你手动退出 → 收集判读 → 恢复隔离）
powershell -NoProfile -ExecutionPolicy Bypass -File <workspace>\tools\gtnh-replaymod-serverdiag.ps1 `
    -Deploy -Launch -Collect -Restore
```

| 开关 | 作用 |
|---|---|
| （无） | 只读 preflight：java 进程 / `mods` 里的 ReplayMod 是不是 v65 / 诊断版 jar 在不在 / `mods` 里有没有残留 ServerReplay |
| `-Deploy` | 把 `ServerReplay-1.7.10-0.1.0-client.jar`（**无 shade**，勘误区 #1）拷进 `mods`；已存在则先改名成 `.previous` |
| `-Launch` | `launch-gtnh.ps1` 启动（**不 kill java**）→ 自动把 `logs\dsh-launch.pid` 写成真实 pid → 等 `Sound engine started` → PrintWindow 抓主菜单 |
| `-AutoEnterWorld` | 自动点一次「单人游戏」→ 等 8 秒 → **按日志判定** → 没动静才补点第二下（避开坑 37 的"连点误触"） |
| `-AutoWalk` | 计时期间自动按住 `W`/`S` 走动（`keybd_event` **带真实 scancode**，12.23）。默认关：**挖方块/打怪必须手动**，否则 `0x25`/`0x12` 不产生 |
| `-Collect` | 抓三类 `[diag]` 行 → 找同会话两份录像 → 跑 `analyze-mcpr.ps1` → 落盘 `\.selftest\diag-<时间戳>.txt` → **打印判读（结论 A/B/C）** |
| `-Restore` | 把 ServerReplay 挪回 `mods-disabled\`（恢复隔离态） |
| `-All` | `-Deploy -Launch -Collect -Restore` |

**判读三条（脚本会自动打出来）**：有 `capture gap` ⇒ **结论 A**（注入那段时间根本没被调用，问题在 mixin/加载器层）；
只有 `no replay packet id` ⇒ **结论 B**（包到了、换不到 id，日志直接点名类与状态）；
两类都没有而录像仍缺 `0x12/0x17/0x25` ⇒ **结论 C**（包没走到 `func_150725_a`，要换方向）。

**★ 排这一轮时踩到的三件事（下一位直接照做，别重踩）**：

1. **执行器是 Windows PowerShell 5.1**（`C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe`），**不是 pwsh 7**，
   而且本会话的**进程执行策略是 `Restricted`** ⇒ 必须 `-ExecutionPolicy Bypass`（坑 24 的同一件事）。
2. **`.ps1` 带中文时一定要存成 UTF-8 *带 BOM***：无 BOM 的 UTF-8 在 5.1 下按 ANSI 解码，中文变乱码后**连引号定界都会崩**
   （本轮第一版就是这个症状，`Parser::ParseFile` 报出一屏"意外的标记"）。加 BOM 后 0 错误。
3. **`tools\analyze-mcpr.ps1` 用 `Write-Host` 输出**（信息流），所以 `2>&1` **抓不到**它——要 `*>&1`。
   另外 `gtnh-window.ps1` 的 `-Find` 分支带 `exit 0`，同进程调用会**连调用者一起结束**（坑 30）；本脚本只用 `-Shot`/`-Click`。

**验证到哪一步**：语法 `Parser::ParseFile` **0 错误**；**干跑 preflight exit 0**（输出中文正常、四项检查全 `[ok]`）。
**真正跑游戏那一轮没有执行** —— 等洛洛一句话。

#### 二、① 同时交付给 338 环境的一页复验清单

`<workspace>\release\GTNH-ReplayMod-v65\338复验指引.md`（**可直接转发**，面向外部使用者）：

- 先做两件事：**确认 mods 里只有一个 ReplayMod**；日志里全文搜 `Mixin apply for mod replaymod failed` / `InvalidInjectionException` / `NoSuchMethodError`，命中就报告（补丁没生效的信号，坑 51）。
- **7 项逐条验**，每项都给了「怎么做 / 期望 / 日志判据」：
  ① 导出渲染（★ 最关键，附 ffmpeg 前置）② 路径关键帧（用**上次会弹窗的那份录像**）③ 换视角不崩（**必须挑一个有夜视的目标**，否则测不出）④ 回放一开就有玩家（看 `holding … / re-queued …`）⑤ 打开回放即自由相机 ⑥ Ctrl 鼠标 / Shift 退出旁观 ⑦ 自由相机下没有自己的手。
- **已知现象不用报**：原版血条仍显示（GTNH 不发那个事件，已裁定接受）、JourneyMap 小地图 / InGameInfoXML 面板、打开回放瞬间近处黑、不支持深度图导出。
- 回收清单：7 项结果 + 没过项的 `logs/latest.log` 与截图 +（若第 1/2 项没过）出问题的 `.mcpr` + 环境信息。

> `release\GTNH-ReplayMod-v65.zip`（`9735848` B）**没有重打包** —— 指引是独立的一个 `.md`，
> 转发时"zip + 这个 md"两个文件即可。若洛洛想把指引一起塞进 zip，重打包一次即可（jar 与 `SHA256.txt` 不受影响）。
> **交付方式仍待洛洛确认**（我这边无法直推文件到 338 那台机器）。

#### 三、下一位的入口

1. 洛洛说"跑" ⇒ 执行第一节那条 `-Deploy -Launch -Collect -Restore`，然后**只看判读结论**：
   A ⇒ 去查"那段时间服务端走了哪条发包路径"；B ⇒ 按日志点名的类去查表；C ⇒ 换方向。
2. 洛洛给 338 的交付方式 ⇒ 把 `release\GTNH-ReplayMod-v65.zip` + `338复验指引.md` 送过去。
3. 两条都不动的话，**保持现状**：客户端 v65 已部署可用，服务端 mod 处于隔离态 + 已瘦身 + 带诊断。

---

### 12.34 338 环境反馈「导出的视频白屏」—— 根因与修复（v66，**✅ 338 环境已验证通过**）

> **来源**：Tester_A（338 mods 环境），2026-10-11 17:54，两条：
> ①「渲染出来的视频白屏」②「然后渲染结束给我鼠标抢了」。
> ①是**功能性缺陷**（本轮的验收阻塞项），②更像体验/状态问题，需要先问清现象（见第四节）。

#### 一、★ 白屏的根因：反射直连真 GL11 绕过了 Angelica 的状态管理器

12.24 的修复让导出**不崩**了，但那条路是"兜底"：`MixinPboFrameCapturerGlReadPixels` 把
`glReadPixels(..., long)` 用**反射直连真 LWJGL3**（因为 Angelica 的 `GLStateManager` 没实现这个重载）。

而 `glReadPixels` 的 **long 形式是"从当前绑定的 `GL_PIXEL_PACK_BUFFER` 的指定偏移处读回"**——
它**完全依赖 PBO 正处于绑定状态**。PBO 的绑定/解绑是**通过 Angelica 的 `GLStateManager` 做的**，
也就是说：**读的时候绕过了状态管理器，而绑的时候经过了它**。只要 Angelica 的状态影子副本
与真实 GL 状态不同步，读回就会落在**错误的（没被写入的）缓冲区**上 ⇒ **每帧都是空白 ⇒ 白屏**。

**这解释了两个现象**：
- 为什么 12.24 本机实测"导出成功"（1080p/60fps/1230 帧）—— 那次状态恰好同步；
- 为什么 338 环境白屏 —— 那边（不同驱动 / AngelicaShaders 路径 / 334 vs 338 mods）影子副本与真实状态分叉。

⇒ **正解不是继续修那条反射路，而是根本不走 PBO 路径**：上游自己就有非 PBO 分支
（`SimpleOpenGlFrameCapturer`，用 **`ByteBuffer` 重载**），而那个重载 Angelica **是实现了的** ⇒
整条读回路径都留在状态管理器内部，不存在影子副本不同步的问题。

#### 二、修复：`MixinPipelinesNoPbo` 按**坑 51** 重写并启用（v66）

12.24 就写了这个 mixin，但它写的是 `@At(value = "GETSTATIC", ...)` ——
**`value` 是"注入点类型"不是 opcode**，于是运行期抛
`InvalidInjectionException: GETSTATIC is not a valid injection point specifier`；
因为配置是 `required: false`，它**只在日志里留一条 WARN 然后静默不生效**。
所以从 v49 一直到 v65，导出**一直吊在 PBO 路径上**，靠那条反射兜着 —— 白屏正是这个历史遗留的账。

**修好后的三处关键取值（离线 `javap -v` 验死，不再是"看不出来的失败"）**：

```
org.spongepowered.asm.mixin.injection.Redirect(
  method=["newDefaultPipeline","newStereoscopicPipeline","newCubicPipeline","newEquirectangularPipeline"]
  at=@At( value="FIELD"                       ← ★ 必须是 FIELD
          opcode=178                          ← ★ 178 = GETSTATIC
          target="Lcom/replaymod/render/utils/PixelBufferObject;SUPPORTED:Z"
          remap=0, require=0 )
```

| 核对项 | 证据 | 结果 |
|---|---|---|
| 目标 `getstatic` 真实存在吗 | `javap -c Pipelines` → `getstatic #95 // Field …PixelBufferObject.SUPPORTED:Z`，出现在 `newDefaultPipeline`(偏移 17) / `newStereoscopicPipeline`(17) / `newCubicPipeline`(17) / `newEquirectangularPipeline`(38) | ✅ **4 处，全部匹配** |
| `SUPPORTED` 会不会是编译期常量（那样读取点被内联、永远 redirect 不到） | `javap -c PixelBufferObject` 的 `<clinit>`：由 `GLContext.getCapabilities().GL_ARB_pixel_buffer_object \|\| OpenGL15` **运行时计算** | ✅ **不是常量，getstatic 真实在字节码里** |
| 方法名对不对 | `javap -p Pipelines` 的方法表 | ✅ 5 个 `new*Pipeline` 全在；**原 mixin 多写了一个 `newODSPipeline`，它根本不读 `SUPPORTED`** —— 已从列表里去掉（`require = 0` 下多写无害，但没必要） |
| 注解取值对不对 | `javap -v` 的 `RuntimeVisibleAnnotations` | ✅ `value="FIELD"` / `opcode=178` / `target=…;SUPPORTED:Z` |

**代价（如实记录）**：普通导出的帧读回从"异步重叠"变成"同步"，**渲染会略慢**；功能等价 ——
这条分支本来就是上游在"驱动不支持 PBO"时走的。**深度图导出**仍然强制走 PBO（那时用得上那张反射桥）。

#### 二·补、★ 非 PBO 路径的离线验证（这是"跳过本机实测"的前提）

洛洛决定**不在本机实测、直接把 v66 发给 338 环境**（本机原本就不白屏，跑了也证明不了修好）。
因为这条非 PBO 分支**从 12.24 起就没有真正跑过一次**，发出去之前必须离线证明"它本身是通的"：

| 核对 | 证据 | 结果 |
|---|---|---|
| `Pipelines` 的非 PBO 分支到底 new 哪个类 | `javap -c`：偏移 44 正是 `ifeq 44` 的跳转目标，那里是 `new com/replaymod/render/capturer/SimpleOpenGlFrameCapturer` | ✅ 类**存在**（先前一次 javap "查不到"是我把 grep 写错，不是类不存在） |
| 它自己怎么读像素 | 它只有构造器 + `process()`，**没有任何 GL 调用** ⇒ 读回在基类 | `com/replaymod/render/capturer/OpenGlFrameCapturer` |
| **基类用的哪个重载** | `OpenGlFrameCapturer` 偏移 39：`invokestatic org/lwjgl/opengl/GL11.glReadPixels:(IIIIIILjava/nio/ByteBuffer;)V` | ✅ **ByteBuffer 版 —— 正是 Angelica 实现了的那三个之一** |
| 这条路上有没有 long 版残留 | 全类扫描 `glReadPixels` 只有上面**一处** | ✅ **一个都没有** |

⇒ **禁用 PBO 之后，整条读回链路都留在 Angelica 的状态管理器内部，不可能再碰到那个缺失的方法。**
⚠️ 离线能证明的是"不会崩、不会走错方法"；**"视频内容正常"仍要靠Tester_A那台实测**。

> 顺带记一笔：PBO 分支用的其实是 **`SimplePboOpenGlFrameCapturer`**（不是 12.24 崩溃栈里那个
> `PboOpenGlFrameCapturer`）—— 两者都在 jar 里，别混。

#### 三、产物 v66（**未部署**）

| 项 | 值 |
|---|---|
| 文件 | **`<workspace>\replaymod-1.7.10-2.5.2-gtnh-v66-nopbo.jar`** |
| 大小 / SHA256 | `10838977` B / `4B7940E9EE607FF64A01D3BD4C4DB0631E801BC77EDF9C7D5812DF50D3C3B479`（**含本节的导出修复 + 第五节的鼠标修复，因此重建过一次**） |
| 构成 | **v48 基座 + 15 个功能 mixin**（= v65 的 14 个 + `MixinPipelinesNoPbo`）；本次改动了其中 2 个 |
| 待测包（给 338 环境） | `<workspace>\release\GTNH-ReplayMod-v66-待测\`（`replaymod-1.7.10-2.5.2-gtnh-v66.jar` + `SHA256.txt` + **`本次要测什么.md`**）+ 同名 `.zip` |
| 编译 | 15 个 mixin **逐个 `javac`，退出码全 0**（坑 47 的纪律） |
| jar 内容 | `com/gtnh/replaycompat/mixin/*.class` **15 个**；jar 内 `mixins.replaycompat.json` 的 `client` 段含 `MixinPipelinesNoPbo` |
| 部署 | ❌ **没有任何 jar 进 `mods\`**；`mods\replaymod-…-gtnh.jar` 仍是 v65（`907B72B8…`） |

#### 四、验证计划（**洛洛决定：跳过本机实测，直接发 338 环境**）

> 理由：本机原本就不白屏，跑一轮只能证明"没弄坏"，证明不了"修好了"；而这条非 PBO 分支的可用性
> 已在「二·补」用字节码离线证明（读回全程走 Angelica 实现了的 `ByteBuffer` 重载）。

1. **338 环境（Tester_A）**：装 v66 重渲一次。**主判据 = 视频不再白屏**；
   辅助判据 = 日志出现 `[replaycompat] video export: PBO frame capture disabled (…)`（直接证明新路径生效）。
   已提醒他：**渲染会略慢**，这是预期代价，不是 bug。
2. **本机**：等 338 回话后再决定要不要补跑一轮 —— 若那边说"还是白"，本机立刻成为必要的对照面。

#### 五、「渲染结束鼠标被抢了」—— 已按"以真实状态为准"修复（v66）

`MixinGuiReplayOverlayMouseVisible`（12.23/12.28 的产物）的状态是
`ctrlToggle`（Ctrl 点一下锁定）/ `ctrlWasDown`（边缘检测）/ `mouseKeyHeld`（**我们上次设进去的值**），
每帧在 `updateUserInputGui` 的 HEAD 轮询，**原来只在 `target != mouseKeyHeld` 时才应用**。

**病灶就是那个比较对象**：它比的是"我们上次设的值"，而不是 overlay 的**真实状态**。
渲染过程中只要别的代码（渲染 GUI 的开关、`displayGuiScreen(null)` → `setIngameFocus()`）把真实的
`mouseVisible` 改回 `false`，我们的 `mouseKeyHeld` 仍然记着 `true` ⇒ **永远不再纠正** ⇒ 鼠标保持被抓。

**修法（已落地）**：改成**读回 overlay 自己的 `isMouseVisible()` 再决策** ——

```java
boolean actual = self.isMouseVisible();
if (target == actual) {
    replaycompat$mouseKeyHeld = target;   // 同步我们的记录（probe 日志用）
    return;                               // 已经在用户要的状态，什么都不做
}
```

于是"外部把状态改了"这种情况下一帧就会被纠正回来；本来就一致时仍然什么都不做（常见路径零开销）。
**失控保护保留**：每秒最多 6 次切换（`MAX_SWITCHES_PER_WINDOW`），若真有"我们设一次、别处改一次"的
拉扯，最多 6 次后就会被禁用并留一条 `input strobing` 日志 —— 宁可停手也不抖动（坑 48 的教训）。

**仍需Tester_A确认**；如果他还是觉得"鼠标被抢"，那句追问依然有效：
> **渲染结束后，再点一下左 Ctrl，鼠标能回来吗？** 能 ⇒ 本次修法对路（只是还有别的时序）；不能 ⇒ 是另一个病。

#### 六、状态

- v65 仍是 `mods\` 里的交付版；**v66 已打包成待测包先发给 338 环境验**（洛洛定的：本机不跑，因为本机原本就不白屏、跑了也证明不了修好），等那边回话再决定是否部署/发布。
- 服务端那边（12.31/12.33）不受影响，保持"诊断就绪、未执行"。

---

### 12.35 338 反馈「点时间轴的时候崩」—— 是 **AE2 的 Waila 集成缺判空**（v67）

> **来源**：`https://mclo.gs/mfORdGF`（洛洛转达），本机副本 `<workspace>\.selftest\mfORdGF.log`（`4605905` B / 24924 行）。
> 日志显示装的是 **v65**（`replaymod-1.7.10-2.5.2-gtnh-v65.jar`），崩溃时间 **18:01:11**。

#### 一、崩因：AE2 把 null 的"鼠标指向目标"直接解引用

```
[Client thread/FATAL]: Unreported exception thrown!
java.lang.NullPointerException: Cannot read field "field_72307_f" because "mop" is null
    at appeng.integration.modules.waila.part.PartAccessor.getMaybePart(PartAccessor.java:43)
    at appeng.integration.modules.waila.PartWailaDataProvider.getWailaStack(PartWailaDataProvider.java:98)
    at mcp.mobius.waila.overlay.RayTracing.getIdentifierItems(RayTracing.java:152)
    at mcp.mobius.waila.overlay.RayTracing.getIdentifierStack(RayTracing.java:88)
    at mcp.mobius.waila.overlay.RayTracing.getTargetStack(RayTracing.java:69)
    at mcp.mobius.waila.overlay.DecoratorRenderer.onRenderWorldLast(DecoratorRenderer.java:28)
    at net.minecraftforge.client.ForgeHooksClient.dispatchRenderLast(ForgeHooksClient.java:261)
```

**字节码证据**（`javap -c` on `appliedenergistics2-rv3-beta-1050-GTNH.jar`）：
`getMaybePart(TileEntity, MovingObjectPosition)` 的**头两条指令**就是
`aload_2; getfield MovingObjectPosition.field_72307_f` —— **零判空**，所以 `mop == null` 时必崩在 `PartAccessor.java:43`。

#### 二、★ 为什么"点时间轴"只是**时机**，不是原因

崩溃报告里本地玩家实体是 **`CameraEntity['CHSheep']`** ⇒ 这是**回放中**。
Waila 每帧都从 `RenderWorldLastEvent` 跑一遍"鼠标指着什么方块"以显示提示框；
而**回放里一旦用 Ctrl 放开鼠标**（这正是点时间轴、点按钮的前提，见 12.23/12.34 的鼠标补丁），
游戏的 `objectMouseOver` 就可能是 **null** —— 那是完全正常的状态。
⇒ **AE2 拿到这个正常状态却没判空 ⇒ 整局崩掉。**
⇒ 换句话说：**不是"点时间轴"崩，而是"回放里放开鼠标"随时都可能崩**，点时间轴只是那一下。

**责任归属**：这是 **AE2 自身的缺陷**（`PartAccessor` 缺 null 检查），既不是 ReplayMod 的、也不是适配包的。
但既然我们的 Ctrl 补丁让"回放里放开鼠标"变成常用操作，就该替它兜住 —— 这与 12.25（夜视守卫）、
12.31（entity tracker）是同一类"给第三方缺陷打防御补丁"。

#### 三、修复：`MixinAe2PartAccessorNullMop`（v67）

在 `getMaybePart` 的 **HEAD** 注入，参数 `MovingObjectPosition` 为 null 时返回 **`Optional.absent()`**：

```java
@Inject(method = "getMaybePart", at = @At("HEAD"), remap = false, require = 0, cancellable = true)
private void replaycompat$guardNullMouseOver(TileEntity te, MovingObjectPosition mop,
                                             CallbackInfoReturnable<Optional<IPart>> cir) {
    if (mop == null) cir.setReturnValue(Optional.absent());
}
```

**为什么返回 absent 是干净降级（这是动手前必须验的一步）**：`getMaybePart` 的调用方**只有两个**，
而它们**全都先 `isPresent()` 再 `get()`**：

| 调用方 | 证据 |
|---|---|
| `appeng.integration.modules.waila.PartWailaDataProvider` | 偏移 +31 `isPresent()`，`ifeq` 跳过 |
| `com.glodblock.github.crossmod.waila.PartWailaDataProvider`（**ae2fc**） | **5 处**调用点，逐处都是 `getMaybePart → astore → isPresent() → ifeq` |

⇒ 唯一可见差异 = "没有指向目标时安静跳过 AE2 部件提示"，而不是换一种崩法。

**注入点取值已离线验死**（`javap -v`）：
`method=["getMaybePart"]` / `value="HEAD"` / `cancellable=true` / `require=0` /
`targets=["appeng.integration.modules.waila.part.PartAccessor"]`。
AE2 是普通 Forge mod、从不被 remap ⇒ 方法名保持源码名 + `remap = false`。
这条 mixin 放在配置的 **client 段**（Waila 的提示路径是纯客户端的）。

#### 四、产物 v67（**未部署**）

| 项 | 值 |
|---|---|
| 文件 | **`<workspace>\replaymod-1.7.10-2.5.2-gtnh-v67-ae2guard.jar`** |
| 大小 / SHA256 | `10839879` B / `296283697EDB866AC3B13833B40381530F170DB4EFAA011957B69E8B4C3FD214` |
| 构成 | **v48 基座 + 16 个功能 mixin** = **v66 的 15 个 + `MixinAe2PartAccessorNullMop`** |
| 编译 | 16 个 mixin 逐个 `javac`，退出码全 0（AE2 + Guava 已挂进编译 classpath） |
| 待测包 | `<workspace>\release\GTNH-ReplayMod-v67-待测\`（jar + `SHA256.txt` + `本次要测什么.md`）+ 同名 `.zip` |
| 部署 | ❌ 未部署；`mods\` 仍是 v65（`907B72B8…`） |

> **v67 取代 v66**：v66（`4B7940E9…`）只含"白屏 + 鼠标"两项，v67 在它之上加了这一针。
> 给 338 环境的包**发 v67 即可**；v66 的待测目录/zip 保留仅作追溯（别混发）。

#### 五、本轮的方法论（值得沿用）

1. **别信用户的现象描述，先看崩溃栈 + 上下文**：他说"点时间轴崩"，而崩溃报告里玩家是 `CameraEntity`
   ⇒ 一眼就知道这是**回放**场景，"时间轴"只是触发时机。
2. **判空补丁必须先确认调用方**：`Optional.absent()` 只有在调用方检查 `isPresent()` 时才是降级，
   否则就是把 `NullPointerException` 换成 `IllegalStateException`。本轮把 AE2 与 ae2fc 的
   **全部 6 处调用点**都用 `javap -c` 看过才动手。
3. **注入点写法照旧要离线验**（`value` 是注入点类型、`cancellable=true` 才能 `setReturnValue`）。

---

### 12.36 338 反馈「长按 ESC 才能操作 / WASD 没用 / 点时间轴跳末尾」—— 上游 popup 覆盖了玩家锁定（v68）

> **来源**：`https://mclo.gs/wuAqOTX`（本机副本 `<workspace>\.selftest\wuAqOTX.log`，`4206054` B / 22883 行）。
> 日志显示装的是 **v66**。Tester_A原话：「我需要一直长按 esc 才能偶尔操作」「无法调时间轴」「点了的话直接跑到时间轴最后了」「wasd 没有用」。

#### 一、★ 日志铁证（一眼定性）

| 统计 | 值 | 含义 |
|---|---|---|
| `mouse LOCKED VISIBLE` | **1** | 玩家只按了一次 Ctrl |
| `replay mouse RELEASED - overlay UI is clickable` | **112** | 我们**设了 112 次** `mouseVisible=true` |
| `replay mouse GRABBED - free camera look` | **0** | 一次都没成功进入"抓取" |
| `input strobing, switching disabled` | **1** | 已撞上每秒 6 次的防抖动保护 |
| `toggle=` | 恒 **true** | **按键检测没问题**（所以"改成事件驱动"的方向是错的） |

⇒ **`setMouseVisible(true)` 之后读回来是 true，但下一帧再读又变 false** ⇒ 我们（v66 的"以真实状态为准"）每帧重试 ⇒ 抖动。

#### 二、机理链（字节码级，四步闭合）

1. `AbstractGuiOverlay.setMouseVisible(b)` 的实现是 **`putfield mouseVisible; updateUserInputGui();`** —— 设完**立刻**跑挂屏逻辑：
   `if (visible) { if (mouseVisible) { if (mc.currentScreen == null) mc.displayGuiScreen(userInputGuiScreen); } }`
2. 全类扫描 `mouseVisible` 的写入者**只有两处**：`setMouseVisible` 自己，和合成访问器 `access$202`；
   而 `access$202` 的**唯一**调用点是 **`AbstractGuiOverlay$UserInputGuiScreen.func_146281_b`（= `onGuiClosed`）**
   ⇒ **输入屏一被关闭，就把 `mouseVisible` 清成 false**。
3. **`AbstractGuiPopup`**（渲染设置 / 渲染队列那类面板的基类）：
   ```java
   protected void open()  { wasMouseVisible = overlay.isMouseVisible(); overlay.setMouseVisible(true); }
   protected void close() { overlay.setMouseVisible(wasMouseVisible); }
   ```
   上游语义是"弹出时放开鼠标、关闭时恢复原状"。但**在这套整合包里那个 flag 从来不稳定为 true**
   （12.22 的结论 + 第 2 步的清理）⇒ `wasMouseVisible` 几乎总记成 `false`
   ⇒ **每次 popup 关闭都把玩家用 Ctrl 锁定的可见状态覆盖回"抓取"**。
4. 我们的补丁于是每帧重试 ⇒ 每次都走 `displayGuiScreen` ⇒ `unPressAllKeys()`
   ⇒ **WASD 被反复清空**；输入屏反复重建 ⇒ jGui 把一次点击当成拖动 ⇒ **时间轴一下跳到末尾**。

⇒ **三个症状（长按 ESC / WASD 没用 / 时间轴跳末尾）是同一个根因。**

> **如实记一笔：v66 那处"以真实状态为准"是本轮我引入的放大器。** v65 只是"设一次就失败"（鼠标闪一下没了，
> 所以玩家只能靠 ESC），v66 把它变成"每帧重试"，于是多出抖动、WASD 被反复清空两个后果。
> 修的方向没错（确实该以真实状态为准），错在**没有配套的退避与前置条件**。

#### 三、修复（v68）

| 组件 | 作用 |
|---|---|
| **`MixinGuiPopupPreserveMouseLock`**（新） | `@Redirect` `AbstractGuiPopup.open()` 里那次 `overlay.isMouseVisible()`，返回 **`overlay.isMouseVisible() \|\| ReplayMouseState.lockedVisible`** ⇒ popup 快照的是**玩家意图**，关闭时恢复"可见"而不是覆盖玩家。没有 Ctrl 锁定时返回值与原逻辑逐位相同，**上游行为零改变** |
| **`com.gtnh.replaycompat.util.ReplayMouseState`**（新） | 两个 mixin 之间的共享标志。**必须放 mixin 包外**（坑 42：mixin 包内的类不能互相引用） |
| `MixinGuiReplayOverlayMouseVisible`（改） | **每帧**把当前 overlay 实例的 `ctrlToggle` 同步进 `ReplayMouseState` —— 不能只在按键边缘同步，因为标志是静态的、而 `ctrlToggle` 属于 overlay 实例，**退出回放再进另一个回放**时会残留上一次的值 |

**注入点离线验证**（`javap -v`）：
`method=["open"]` / `value="INVOKE"` /
`target="Lcom/replaymod/lib/de/johni0702/minecraft/gui/container/AbstractGuiOverlay;isMouseVisible()Z"` /
`remap=false` / `require=0` —— 且 `AbstractGuiPopup.open` 里**确实存在**该调用（`javap -c` 已确认）。

#### 四、产物 v68（**未部署**）

| 项 | 值 |
|---|---|
| 文件 | **`<workspace>\replaymod-1.7.10-2.5.2-gtnh-v68-mouselock.jar`** |
| 大小 / SHA256 | `10841184` B / `78FA48A2FCEDEEDCA9E277AF676C03FAC61642FCFC71F1EA8F3C7A1F53584BDA` |
| 构成 | v48 基座 + **17 个功能 mixin** + 3 个 util class = **v67 的 16 个 + `MixinGuiPopupPreserveMouseLock`** |
| 编译 | util 3 个 + mixin 17 个逐个 `javac`，退出码全 0（**util 必须先编**，mixin 依赖它） |
| 待测包 | `<workspace>\release\GTNH-ReplayMod-v68-待测\`（jar + `SHA256.txt` + `本次要测什么.md`）+ 同名 `.zip` |
| 部署 | ❌ 未部署；`mods\` 仍是 v65（`907B72B8…`） |

> **v68 取代 v66 与 v67**（一次测三件事：导出白屏 / AE2 崩溃 / 鼠标·WASD·时间轴）。
> v66、v67 的待测目录与 zip 保留仅作追溯，**别混发**。

#### 五、方法论（本轮新增）

1. **同一份日志里的计数就是证据**：`RELEASED 112` vs `GRABBED 0` 一眼把"抖动"和"按键检测坏了"区分开 ——
   后者会表现为 `toggle` 不翻转，而日志显示 `toggle` 恒为 true。
2. **"谁改了那个字段"要用全量字节码扫描回答**：先列出字段的**所有写入者**（只两处），再找合成访问器
   `access$XX` 的**唯一调用点**，一步就落到 `onGuiClosed` 上。
3. **改上游从未走过的路径要当心**：`mouseVisible=true` 这条路径在 GTNH 上以前从未稳定成立过，
   所以上游围绕它的 `wasMouseVisible` 快照逻辑从来没有被验证过 —— 我们把它打开，就得替它兜住。

---

### 12.37 338 新反馈：「渲染没问题、没手臂、UI 也没问题，但人好像又没了」（2026-10-11 晚）

> Tester_A原话：**「渲染是没有问题的」**「也没有手臂，ui什么的」**「但是人好像又没了」**。
> ⇒ **一半是重大好消息，一半是新回归。**

#### 一、✅ 好消息（已可结案）：12.34 的白屏修复在 338 环境**验证通过**

「渲染是没有问题的」= 导出的视频**不再白屏**；「也没有手臂」= 12.30 的手部修复正常；
「ui什么的」也没问题 ⇒ **非 PBO 路径（`MixinPipelinesNoPbo`）在 338 环境成立**，
12.34 的根因判断（白屏 = 反射直连真 GL11 绕过了 Angelica 的状态管理器）**得到现场证实**。
唯一代价（渲染略慢）他也没抱怨 ⇒ 这个权衡是对的。

#### 二、⚠️「人没了」—— 先用已有日志排除了最可疑的那条链

**判据来自 `NullWorldGuard` 自己的两行日志**，我在**已有的两份 v66 日志**里各查了一遍：

| 判据 | `wuAqOTX.log`（18:08:02） | `mfORdGF.log`（17:58:21） |
|---|---|---|
| `replay loading: holding S0CPacketSpawnPlayer until the client world exists (it will be replayed)` | ✅ 1 次 | ✅ 1 次 |
| `replaycompat re-queued 1 deferred packet(s) now that the world exists` | ✅ 1 次 | ✅ 1 次 |
| `dropping S0CPacketSpawnPlayer` | ❌ 0 | ❌ 0 |
| `deferred packet queue is full` | ❌ 0 | ❌ 0 |

⇒ **defer → requeue 全链路正常工作，SpawnPlayer 确实被重放了。**
⇒ **「人没了」不是 12.27 那条链坏了**（那次回归的判据恰恰是"只有 holding、没有 re-queued"）。
⇒ 也顺手说明 `MixinPipelinesNoPbo` / `MixinAe2PartAccessorNullMop` / popup 修复**都没有破坏这条链**。

#### 三、剩下的三个可能（按可能性排序，**需要问清再动手**，坑 56）

1. **相机位置**（最可能）：12.28 二让 `renderViewEntity` 每 tick 自动指回 `CameraEntity`；
   而回放**刚打开**时相机恰好与人物重合（12.14 的原话就是"相机贴在人物身上、近处黑"）
   ⇒ 表现为"看不到人"，**移动一下相机就能看到**。需要一个判据区分它和"实体真的不存在"。
2. **这份录像本身没有玩家实体**：客户端录的录像里 `S0CPacketSpawnPlayer` 时序不同
   （12.13：客户端录像的 SpawnPlayer 在 t=2.7s、JoinGame 在 t=26.3s），
   而且**我们的服务端录像才会注入合成 SpawnPlayer**（2.7(1)）⇒ 换一份录像结果会不同。
3. 别的渲染层面问题（此时才需要新证据）。

**要问的三件事（一次问清，省一轮）**：
- **「人没了」的确切含义**：完全看不到任何玩家模型？／要**切换维度**才出现（12.27 的老症状）？／**移动相机或走一下就能看到**？
- **是哪一份录像**：文件名带 `server-` 前缀的（服务端录的）还是别的？
- **一张截图**（或 F3 里的实体数 `E:`）—— 图比描述可靠。

> **不要先动代码**：如果是第 1 种（相机初始机位），"修法"是改变回放初始机位，那属于**行为变更**，要先确认洛洛是否要。
> 如果是第 2 种，则不是缺陷，只需在说明里写清"哪些录像里有人"。

---

### 12.38 「点时间轴后 WASD 又没了」结案方向 + **★ 导出视频的画面问题浮出水面**（v69）

> **来源**：`https://mclo.gs/qufYbYC`（本机副本 `<workspace>\.selftest\qufYbYC.log`，`3992081` B / 24924 行，装的是 **v68**）
> 加上洛洛转来的两个视频：`QQ20261011-181519.mp4`（17.5s / 32fps = 录屏）、
> `2026_10_11_18_20_34.mp4`（10s / 1920x1080 / 60fps = **ReplayMod 渲染输出**）。
> Tester_A原话：「没有崩」「人回来了」「但是一点时间轴就和之前一样」「但是点了暂停时间轴的话 wasd 又没了」
> 「人的视角渲染出来的视频有问题」。

#### 一、✅ 已确认修好的（三条）

「**没有崩**」= 12.35 的 AE2 判空生效；「**人回来了**」= 12.37 的疑问自行消失（换录像/相机后可见）；
「导出的视频不白了」= 12.34 在 v66 已被验证，v68 延续。

#### 二、「点时间轴后 WASD 又没了」—— 12.36 的 popup 修复**不够**，真凶是输入屏的 `onGuiClosed`

**v68 的日志计数**（对照 v66）：

| 判据 | v66 | **v68** |
|---|---|---|
| `RELEASED`（我们设"鼠标可见"） | 112 | **136** |
| `GRABBED`（抓到"抓取"） | 0 | **0** |
| `mouse LOCKED GRABBED`（玩家按 Ctrl 抓回） | 0 | **0** |
| `input strobing` | 1 | 0 |
| `replay mouse probe:`（每 5 秒一条） | 20 | **3** |

⇒ **12.36 的 popup 快照修复没有降低重试次数** ⇒ popup 不是（唯一）元凶。
⇒ `probe` 只有 3 条说明 `updateUserInputGui` 被调得**很稀疏**（几秒~几十秒一次），**不是每帧**。

**这次把范围缩到一句话能说清**：`mouseVisible` 的写入者只有 `setMouseVisible` 和合成访问器 `access$202`；
`access$202` 全仓库只有**一个**调用点 —— **`AbstractGuiOverlay$UserInputGuiScreen.func_146281_b`（= `onGuiClosed`）**。
⇒ **输入屏每次被关闭，都会把"鼠标可见"清成 false**；而这个标志又是"要不要继续挂着输入屏"的依据
（`updateUserInputGui`：`if (mouseVisible && mc.currentScreen == null) displayGuiScreen(userInputGuiScreen)`）
⇒ **玩家一点时间轴 ⇒ 输入屏关一次 ⇒ 标志被清 ⇒ 鼠标被抓回 ⇒ 我们下一轮再放开**；
而每次放开都要走 `displayGuiScreen` → `unPressAllKeys()` ⇒ **WASD 被反复清空**（正是"点暂停后 WASD 没了"）。

**修复 `MixinUserInputGuiScreenKeepMouseLock`（v69）**：`@Redirect` 掉 `func_146281_b` 里那次
`access$202(overlay, false)` —— **只在"玩家仍按 Ctrl 锁定"时跳过这次写入**。
标志于是保持 true，上游自己的 `updateUserInputGui()` 会在下一轮把输入屏重新挂上；
玩家再按一次 Ctrl（意图变 false）时该写入照常发生，抓回行为与原来一致；
**没有锁定时这个 redirect 是纯透传，上游行为逐位不变**。
`access$202` 是 private synthetic，透传路径用**反射**调用它（本工程无 reobf，不能直接调）。

**注入点离线验证**（`javap -v`）：`method=["func_146281_b"]` / `value="INVOKE"` /
`target="Lcom/replaymod/lib/de/johni0702/minecraft/gui/container/AbstractGuiOverlay;access$202(Lcom/replaymod/lib/de/johni0702/minecraft/gui/container/AbstractGuiOverlay;Z)Z"` / `remap=false` / `require=0`。

> **给用户的说明里必须写**：**鼠标可见时 WASD 本来就不驱动相机**（上游设计）。
> 想飞行要先按一次 Ctrl 把鼠标抓回 —— 这一条不写清，任何修复都会被当成"WASD 坏了"。

#### 三、★ 导出视频的**画面**问题（本轮最重要的意外发现）

**用 ffmpeg 抽帧对比**（系统里的 `ffmpeg 9.0.2`，实例目录里没有）：

| | 本机（12.24 渲染） | Tester_A（338，v68 渲染） |
|---|---|---|
| 画面 | **有完整世界**：一大片绿色机械结构、方块细节清晰 | **几乎只有天空**：云 + 一块孤立方块 + 一条品红色斜线 |
| 参数 | 20.5 s / 1080p / 60fps / 1567 kbps | 10 s / 1080p / 60fps / 992 kbps |

**⇒ 两个结论**：
1. **12.24 那次"导出成功"的验收只看了文件合法性**（`ffprobe`：1230 帧 / h264 / 1080p60），
   **没有人看过画面** —— 所以"渲染导出"的**画面质量**从未被真正验收过。
2. 「只有右下角一小块渲染」这个描述与 **12.14** 记录的、**v22 默认注册 `GLStateTrackerTransformer` 时**的症状
   **一字不差**（当时：v22 默认 = 只有右下角一小块；`-Dreplaymod.transformers=false` = 恢复约一半）。
   ⇒ 高度怀疑 338 环境上影响渲染画面的就是那个 transformer。

**已经发给Tester_A的单变量实验**（写进 v69 的说明）：在 PrismLauncher 的 JVM 参数里加
**`-Dreplaymod.transformers=false`**，用同一段录像再渲染一次，看世界是否出来。
判据：出来 ⇒ 把 `GLStateTracker` 改成"渲染导出时自动关闭"；还是一样 ⇒ 换方向，
并且需要他说明"视频里不对的**具体**是什么"（看不到方块？看不到人？颜色/贴图异常？）。

**对照图**：`release\GTNH-ReplayMod-v69-待测\对照截图\本机-渲染有世界.png`
与 `…\Tester_A-渲染几乎没世界.png`；抽帧原图在 `tools\shots\vidframes\`。

#### 四、产物 v69（**未部署**）

| 项 | 值 |
|---|---|
| 文件 | **`<workspace>\replaymod-1.7.10-2.5.2-gtnh-v69-inputscreen.jar`** |
| 大小 / SHA256 | `10842922` B / `EC9C01B1CD32C8DFC6F57810677853A159EAB82CD4EB72B03EBB8CEB86F4277E` |
| 构成 | v48 基座 + **18 个功能 mixin** + 3 个 util class（= v68 的 17 个 + `MixinUserInputGuiScreenKeepMouseLock`） |
| 待测包 | `<workspace>\release\GTNH-ReplayMod-v69-待测\`（jar + `SHA256.txt` + `本次要测什么.md` + **`对照截图\`**）+ 同名 `.zip` |
| 部署 | ❌ 未部署；`mods\` 仍是 v65（`907B72B8…`） |

#### 五、工具沉淀（本轮新增）

**用 ffmpeg 抽帧做"渲染画面"的客观验收**（本机 ffmpeg 在
`<home>\AppData\Local\Microsoft\WinGet\Packages\Gyan.FFmpeg_…\ffmpeg-9.0.2-full_build\bin\ffmpeg.exe`）：

```powershell
& $ff -ss 5 -i <video.mp4> -frames:v 1 -y <frame.png>     # 抽第 5 秒的一帧，再用 read_image 看
& $ff -i <video.mp4> 2>&1 | Select-String 'Duration|Stream #0.*Video'   # 时长/分辨率/帧率
```
⇒ **以后任何"渲染导出"的验收都必须包含"看画面"这一步**，不能只看文件能不能播。

#### 六、★「视频里的循环 bug」定位：不是帧重复，而是**画面冻结 ⇒ 时间轴停在末尾**（2026-10-11 晚追加）

洛洛追问"录制的视频是在哪里出现了循环 bug"，于是**逐帧量化**了Tester_A那段 10 秒的渲染输出：

| 指标 | Tester_A（v68 渲染） | 本机对照（**v49** 渲染，12.24） |
|---|---|---|
| 帧数 / 不同帧哈希 | 603 / **602**（只有 1 组重复 2 帧） | 1229 |
| 帧间差分 平均 | **0.055** | 0.083 |
| 帧间差分 **中位数** | **0.003** | — |
| 帧间差分 **最大** | 8.102 | **58.865** |

命令（可复用）：
```powershell
& $ff -i <v> -f framemd5 -            # 逐帧哈希 ⇒ 有没有完全重复的帧
& $ff -i <v> -vf "tblend=all_mode=difference,signalstats,metadata=print:file=-" -f null -   # 每帧 YAVG = 与上一帧的平均亮度差
```

**结论**：
1. **视频里没有"帧循环"**（603 帧里 602 个不同哈希）—— "循环"不在视频里；
2. 真正的问题是**画面完全冻结**（中位数 `0.003` ≈ 相邻帧无任何可见变化），而本机对照最大帧差 `58.9`（相机明显在动）；
3. **相机位置的唯一驱动源是录像时间轴**：`renderViewEntity`（`field_71451_h`）在整个 ReplayMod 里只有 4 个写入者，
   而渲染路径的 `EntityRendererHandler.renderWorld` **只读 `thePlayer`、不写 `renderViewEntity`** ⇒ 时间轴不动 ⇒ 渲染出来的每一帧都一样；
4. **⇒ 这与Tester_A早前那句「点了时间轴直接跑到时间轴最后」是同一件事**：
   **点击被当成"拖动到末尾"** ⇒ 播放位置停在录像结尾 ⇒ 渲染 ⇒ 冻结。
   而"点击被当拖动"的原因正是 12.38 第二节那个**输入屏被反复重建**的循环（jClick/jDrag 状态被搅乱）。

**⇒ 所以「跳到最后」与「视频冻结」同源，而 v69 修的正是那个循环。**

**已加进 v69 说明的验证步骤**（零成本）：进回放后**手动把时间轴拖回最左（0%）**、确认时间在走，再渲染这一小段 ⇒
画面动起来即确认"时间轴跑到末尾"这条因果；仍冻结则说明是渲染管线本身的问题，那时需要**那份 `.mcpr` 录像文件**。

#### 七、★「陷入循环」的定量证据（Tester_A的 17.5 秒录屏 + 两份日志，2026-10-11 晚追加）

Tester_A第二次反馈原话：**「陷入循环了说是」**，并给了录屏 `QQ20261011-181519.mp4`（17.5 s / 1920x1080 / 32 fps）。

**录屏侧的客观测量**（`ffmpeg -f framemd5`，1342 帧）：

| 现象 | 数据 |
|---|---|
| 像素完全相同的帧 | 一组画面**重复 820 次**（占 61%） |
| 短周期重复 | 另有 25 组（11 次 / 10 次 / 9 次 / 6 次 / 5 次…）⇒ **反复回到同一画面** |
| 画面里的硬指标 | 第 9 秒那一帧右上角 **`FPS: 8`**；右下角 `00:08 / 12:00`（录像总长 12 分钟） |

**日志侧的定量证据**（「放开鼠标」= `replay mouse RELEASED`，每次都会 `displayGuiScreen`）：

| | v66 `wuAqOTX`（18:04–18:10） | v68 `qufYbYC`（18:23–18:37） |
|---|---|---|
| 「放开」记录 | **112** 条 | **136** 条 |
| 「抓回」记录 | **0** 条 | **0** 条 |
| **同一秒内最多** | **10 次** | **11 次** |
| 密集段 | 18:10:04–18:10:27（24 秒 43 次） | 18:31:48–18:32:00、18:33:30–18:33:31 |

**⇒ 结论（"循环"到底是什么）**：**循环发生在回放的输入屏上，不在视频里** ——
输入屏每秒被"撤下又挂上"10~11 次 ⇒ **帧率掉到 8**（录屏可见）、**GUI 反复重建**（录屏里"反复回到同一画面"）、
每次重建都 `unPressAllKeys()` ⇒ **WASD 被反复清空**；而点击/拖动状态被搅乱 ⇒ **时间轴跳到末尾** ⇒ 渲染出来的视频**冻结**。
**⇒ v69 修的正是这个源头**（输入屏关闭时不再清掉玩家的鼠标锁）。
**判据（给Tester_A v69 复测用）**：日志里**同一秒的 `RELEASED` 次数应从 10~11 降到 1**、FPS 恢复正常、WASD 不再丢。

> **⚠️ 一个取证缺口要记住**：录屏是 **18:15:19** 开始的，而手上三份日志分别是
> `mfORdGF`（17:55–18:01）、`wuAqOTX`（18:04–18:10）、`qufYbYC`（18:23–18:37）——
> **录屏那一轮的日志我们没有**。要精确复盘"循环"当场，需要Tester_A把那一轮的 `logs/latest.log` 发来。

---

### 12.39 ★ v69 实测反馈：「按 ESC 就到中间去了」—— 循环已消失，但暴露了两件新事（2026-10-11 21:19，**反馈者：Tester_B**）

> **★ 测试者归属说明（2026-10-11 21:2x，洛洛更正）**：本文档 12.34~12.38 一路把反馈者统称为"Tester_A"，
> 但洛洛指出**本轮（12.39）的反馈来自「Tester_B」** ⇒ **至少存在两位测试者，且环境可能不同**
> （Tester_A：338 mods，实例在 `D:/MC/minecraft/.minecraft/…`；Tester_B：本轮实例路径相同但需单独确认）。
> **⇒ 以后每条反馈都必须标注来源**；本文档早前各节的归属**未经逐一确认，引用时以"某位测试者"看待**。
> 这一条对判读很关键：像"渲染有世界 / 只有天空"这类差异，**有可能是两台机器，而不是同一个问题的两个阶段**。

> **来源**：`<home>\Downloads\latest (2).log`（测试者实例 `D:/MC/minecraft/.minecraft/…`）
> + `屏幕录制 2026-10-11 211918.mp4`（3.2 s / 2560x1600 / 30 fps）。

#### 一、✅ v69 的第一个效果被现场确认：**循环没了**

| 观察 | v66 / v68 | **v69（本次录屏）** |
|---|---|---|
| 帧率 | `FPS: 8`（录屏可见） | **`FPS: 116` / `115`** |
| 日志 | 同一秒 `RELEASED` 最多 10~11 次 | （本次日志是 `latest.log`，不含 STDOUT，见第四节） |
| 崩溃 / 注入错误 | — | **0 / 0** ✅ |

⇒ 「每秒重建输入屏十几次」这个循环**确实被 12.38 的修复治住了**（帧率从 8 回到 116）。

#### 二、★ 新观察 1：「导出视频只有天空」的根源**不在渲染管线**，而在**回放播放本身**

录屏画面（播放头停在 `10:00`）**只有天空和云**，与 12.38 里那段渲染视频（`2026_10_11_18_20_34.mp4`）**一模一样**。
⇒ **回放里那一帧的画面本来就只有天空** ⇒ 12.38 第三节"GLStateTracker 影响渲染画面"的怀疑**需要修正**：
渲染只是把「回放里看到的东西」录了下来，**问题在回放播放/机位，不在导出路径**。
（✅ **归属已确认（洛洛）**：12.38 那段渲染视频（`2026_10_11_18_20_34.mp4`）与 `qufYbYC` 日志是**Tester_A**的；
本轮是**Tester_B**的 ⇒ **两台不同环境都出现"只剩天空"** ⇒ 这是**普遍现象，不是某台机器的偶发**，
⇒ 应作为**真问题**追，且优先怀疑**两台共有的东西**（我们的适配包本身 / Angelica 的雾与渲染距离 / 回放相机机位），
而不是某一台的环境差异。）
（仍需一条判据：**把播放头拖到 00:00 时画面里有没有世界** —— 若开头有世界、只有中段是天空，
那就只是"那一帧的机位在高空"，根本不是缺陷。这条已写在 v69 说明的 ②补 里。）

#### 三、★ 新观察 2：「按 ESC 就到中间去了」

时间轴刻度是 `00:00 / 05:00 / 10:00 / 15:00 / 20:00 / 25:00 / 30:00`，而播放头停在中段的 `10:00`。

**机理假设（待证）**：按 ESC 打开游戏菜单时，overlay 的输入屏被卸下（`mc.currentScreen != null` ⇒
`updateUserInputGui` 不挂它）；再按 ESC 关掉菜单后，`mouseVisible` 仍为 true ⇒ **输入屏被重新挂载**，
而 jGui 在挂载/首次输入时可能把**鼠标当前位置**当成一次点击 —— **鼠标恰好停在画面正中**（录屏可见），
于是时间轴被"点"到 **10:00**。这与 12.36/12.38 那条链同源（**输入屏生命周期被搅动 ⇒ 产生假的点击/拖动**），
只是 v69 修掉的是"反复重建"，而这一次是"ESC 造成的一次重建"。

**下一步需要**：`fml-client-latest.log`（我们的补丁走 `System.out`，**不在 `latest.log` 里**）
+ 让他描述"按 ESC 前后"鼠标在哪、是否每次都在中间。

#### 四、⚠️ 取证要点（教训）

**`latest.log` 里看不到我们的补丁输出** —— 我们的诊断走 `System.out.println`，
只会出现在 **`fml-client-latest.log`**（带 `[STDOUT]` 前缀）或 `dsh-stdout.log` 里。
⇒ **以后要日志时直接要 `fml-client-latest.log`**，别要 `latest.log`（这次白拿一份）。


---

### 12.40 新增「通用复查方法」文档（2026-10-11，洛洛：「准备一下复查方法文档」）

**动机**：这一轮反馈来自**两位测试者**（Tester_A 338 / Tester_B），且是十几条零散消息 ——
已经出现过两次"环境与归属混淆"，还白拿过一份看不到任何判据的 `latest.log`。
⇒ 把"怎么装、逐项怎么验、要给我什么、我怎么判读"固化成一份**不绑版本**的文档。

| 项 | 值 |
|---|---|
| 位置 | **`<workspace>\release\VERIFICATION_GUIDE.md`**（通用版，以后每轮复用） |
| 分发 | 已复制进 `release\GTNH-ReplayMod-v69-待测\`，并在 `本次要测什么.md` 顶部加了指引 |

**内容骨架**（八节）：
1. **环境卡片（强制填写）** —— 测试者 / 实例路径 / mods 数 / 包名与 SHA256 / **JVM 参数（尤其 `-Dreplaymod.transformers=`）** / Java 版本 / 显卡 / 被测录像；
2. 装包 3 步（删旧 → 放新 → 重启）与校验；
3. **★ 13 项逐项复查表** —— 每项给"怎么做 / 期望 / **硬判据**"（主菜单入口、打开回放、看到玩家、自由相机、Ctrl 鼠标锁不循环、点时间轴不跳末尾、**ESC 后时间轴不动**、放开鼠标不崩、WASD、**导出渲染画面正确**、路径关键帧、换视角、保存面板）；
4. **★ 取证规范** —— **日志要 `fml-client-latest.log` 而不是 `latest.log`**（我们的输出走 `System.out`，12.39 已因此白跑一次）、画面要 **F3 截图**（XYZ / chunk 数 / 观察实体 / 雾）、视频要原文件并附三条 ffmpeg 量化命令（时长信息 / `framemd5` 找重复帧 / `tblend` 帧间差分看画面动没动）；
5. **判读表** —— 9 条"日志里看到什么 ⇒ 什么结论 ⇒ 下一步"（含 `re-queued`/`PBO frame capture disabled`/`input screen closed while locked`/`Mixin apply … failed` 等关键行）；
6. **已知不是 bug 的 5 条**（血条、鼠标可见时 WASD 不驱动相机、初始机位发黑、不支持深度图、导出略慢）；
7. **已知未决问题 + 专用判据**（"只剩天空"要 `00:00` 与出问题处各一张 F3；"ESC 跳中间"要鼠标位置 + 日志；"时间轴跳末尾"要 `RELEASED`/`GRABBED` 计数）；
8. 回滚（本包不改配置，唯一产物是 `replay_recordings\` 与 `replay_videos\`）。

**⇒ 以后要反馈时，先要环境卡片 + `fml-client-latest.log` + F3 截图，这三样凑齐再动手，能省掉整轮返工。**

---

### 12.41 「滚轮没办法调速度」—— 取证结论：**这是上游从来没有的功能**（2026-10-11，Tester_B反馈）

> **来源**（Tester_B）：「然后还有」「不知道为毛」「**滚轮没办法调速度**」。

#### 一、取证（三层，全部离线可证）

1. **滚轮在 jGui 里只发给 `Scrollable`**：`AbstractGuiOverlay$UserInputGuiScreen.func_146274_d()` 的字节码是
   `Mouse.hasWheel() → Mouse.getEventDWheel() → forEach(Scrollable.class).scroll(mousePos, dWheel)` —— **只此一条路**。
2. **速度是滑块，不是 Scrollable**：`GuiReplayOverlay` 里 `public final GuiSlider speedSlider`（100x20、37 档），
   而 `AbstractGuiSlider implements Clickable, Draggable, IGuiSlider` —— **没有 `Scrollable`**。
   jGui 内实现 `Scrollable` 的只有 `AbstractGuiScrollable` 与 3 个屏幕包装类（全 jar 扫描，共 5 个）。
   ⇒ **滚轮事件永远到不了速度滑块。**
3. **上游（含高版本）也没有这个功能**：本地 `ReplayMod-stable`（1.20.1/1.21）与 `ReplayMod-1.7.10` 的
   `GuiReplayOverlay.java` 逐行对照 —— 两者在速度上一模一样（都是 `GuiSlider` + `getSpeedSliderValue()`），
   **都没有任何滚轮处理**（`getEventDWheel`/`dWheel` 命中 0 处，那些命中全是 `speedSlider`）。

⇒ **结论：「滚轮调速」不是 bug、也不是我们改坏的，而是 1.7.10 与高版本都不存在的功能。**
（12.15 修的 `MixinGuiScrollableWheelScale` 只作用于 `AbstractGuiScrollable`，与滑块无关，**没有互相干扰**。）

#### 二、但它可以安全地加（滚轮目前在回放界面里是闲置的）

`GuiReplayOverlay` 里**一个 `Scrollable` 都没有**（字节码扫描：`Scrollable = 0`）⇒ **回放界面里的滚轮现在完全没用**，
再加一条"滚轮调速"不会与任何既有行为冲突。可行做法：
在 overlay 每帧（`updateUserInputGui` 的 HEAD）读 `Mouse.getEventDWheel()`，
非 0 且回放 overlay 可见时，**直接改写 `speedSlider` 的值并触发它自己的 `onValueChanged`**（即复用上游的滑块逻辑，
不自己算速度），并把当前速度显示同步过去。

**⇒ 这是"新增功能"，需洛洛决定是否要做**（不属修复范畴）。

#### 三、本条的可复用判据（写进复查方法）

| 现象 | 结论 |
|---|---|
| 回放里滚轮"没反应" | **正常** —— 上游在回放界面里没有绑定滚轮（列表里的滚轮是另一条路，12.15 已修） |
| 回放中心列表里滚轮一格只动 1 像素 | 12.15 的量纲问题；若复现请报（说明 `MixinGuiScrollableWheelScale` 没生效） |

> **⚠️ 12.41 方向更正（洛洛澄清）**：Tester_B说的"调速度"是**回放里自由相机的移动速度**，**不是播放速度滑块**。
> 12.41 的取证（滚轮到不了 `speedSlider`）本身没错，但**不是本条问题的成因** —— 真正的功能在下面 12.42。

---

### 12.42 ★「滚轮调不了相机移动速度」—— 上游有这功能，被"输入屏常驻"踩掉了（2026-10-11，Tester_B）

#### 一、上游确实有这个功能（1.7.10 与高版本都有）

`ReplayMod-1.7.10` 的 `InputReplayTimer#handleMouseEvent()`（第 54~83 行）：

```java
int wheel = Mouse.getEventDWheel();
if (wheel != 0) {
    CameraEntity cameraEntity = mod.getReplayHandler().getCameraEntity();
    CameraController controller = cameraEntity.getCameraController();
    while (wheel > 0) { controller.increaseSpeed(); wheel--; }
    while (wheel < 0) { controller.decreaseSpeed(); wheel++; }
}
```

高版本把这段抽成了 `InputReplayTimer#handleScroll(int wheel)`，**逻辑相同**。
速度范围由 `ClassicCameraController` 决定（1.7.10：`SPEED_CHANGE = 0.5`，`LOWER_SPEED = 2`，`UPPER_SPEED = 20`）。

#### 二、★ 为什么它失效：滚轮被"交给当前屏幕"了

同一个 `updateTimer()` 的第 40~50 行是**条件分支**：

```java
if (mc.currentScreen == null || mc.currentScreen.allowUserInput) {
    while (Mouse.next()) { handleMouseEvent(); }   // ← 上面那段滚轮调速只在这一支里跑
} else {
    mc.currentScreen.handleInput();                // ← 另一支：滚轮交给屏幕，相机收不到
}
```

- **鼠标被抓取**（`mc.currentScreen == null`）⇒ 走第一支 ⇒ **滚轮能调速** ✅
- **jGui 的输入屏挂着**（`currentScreen = AbstractGuiOverlay$UserInputGuiScreen`，且其
  `allowUserInput`(`field_146291_p`) 为 `false`）⇒ 走第二支 ⇒ **滚轮进了 UI，相机收不到** ❌

**⇒ 而 12.23/12.28 的 Ctrl 鼠标锁让输入屏"常驻"**（这正是让鼠标可点、时间轴可拖的前提），
**⇒ 用户几乎总是停在"调速失效"的那一侧** ⇒ 表现为"滚轮没办法调速度"。
**⇒ 这不是 12.41 那条"滑块不吃滚轮"，而是"滚轮压根没进相机控制器"。**

#### 三、两个可选修法（需洛洛定，**本轮未动代码**）

| 方案 | 做法 | 代价 / 风险 |
|---|---|---|
| **A. 只作说明** | 告诉使用者：**想调速先按一次 Ctrl 把鼠标抓回**（抓取状态下滚轮即可调速，符合上游设计） | 零风险；但"锁定可见"时仍不能调速 |
| **B. 增强：锁定可见时也能调速** | 在输入屏的滚轮路径上加一针 —— 例如 mixin `UserInputGuiScreen#func_146274_d` 的 HEAD/TAIL，读 `Mouse.getEventDWheel()`，非 0 时**调用与上游相同的 `controller.increaseSpeed()/decreaseSpeed()`** | 低风险（该滚轮路径在回放 overlay 里本来只转发给 `Scrollable`，而 overlay 里**没有任何 `Scrollable`**，所以没有既有行为会被抢）；但属于**新增行为**，需明确同意 |

> **取证要点**：`Mouse.getEventDWheel()` 返回的是"当前事件"的值，`Mouse.next()` 消费后**在下一帧之前仍然可读**
> ⇒ 方案 B 无论打在 `func_146274_d` 的 HEAD 还是 TAIL 都读得到。

---

### 12.43 实现「滚轮调相机移动速度」（v70，洛洛选方案 B）

**决策**：洛洛选 **B（增强）** —— 让"鼠标锁定可见"时滚轮也能调相机飞行速度。

**实现**：新 mixin `MixinUserInputGuiScreenWheelCameraSpeed`
（`@Mixin(targets = "…gui.container.AbstractGuiOverlay$UserInputGuiScreen")`）：

| 项 | 做法 |
|---|---|
| 注入点 | `func_146274_d`（`handleMouseInput`）的 **TAIL** —— 等 `super` 把 `Mouse.next()` 消费完之后再读；`getEventDWheel()` 在下一帧前仍返回当前事件的值 |
| 读滚轮 | **反射** `org.lwjgl.input.Mouse.getEventDWheel()`（零编译期依赖；该值在 lwjgl3ify/SDL 下是 ±1，LWJGL2 下是 ±120，见 12.15） |
| 调速链（全反射） | `ReplayModReplay.instance` → `getReplayHandler()` → `getCameraEntity()` → `getCameraController()` → **`increaseSpeed()` / `decreaseSpeed()`**（与上游 `InputReplayTimer#handleMouseEvent` 用**同一对方法**，速度范围与步长因此与上游完全一致，不自己算） |
| 防护 ① | `overlay.isAllowUserInput()` 为 true 时**直接让位** —— 那种情况下上游自己那条支路已经在处理鼠标，我们再做一遍会让一档变成两档 |
| 防护 ② | 每个滚轮事件最多应用 `MAX_STEPS = 10` 步，防止驱动给出异常大的值 |

**离线验证**（`javap -v`）：`method=["func_146274_d"]` / `value="TAIL"` / `remap=false` / `require=0` /
`targets=["…AbstractGuiOverlay$UserInputGuiScreen"]`；编译 18 个 mixin 退出码全 0；jar 内 **19 个 mixin class**。

**产物 v70**：

| 项 | 值 |
|---|---|
| 文件 | **`<workspace>\replaymod-1.7.10-2.5.2-gtnh-v70-wheelspeed.jar`** |
| 大小 / SHA256 | `10845203` B / `C91B61AB36D89D93C57C976E815D6301B9EB4A12804DBC1CB5E2AF5C411C0F46` |
| 待测包 | `release\GTNH-ReplayMod-v70-待测\`（jar + SHA256 + `本次要测什么.md` + `VERIFICATION_GUIDE.md` + `对照截图\`）+ 同名 `.zip` |
| 部署 | ❌ 未部署；`mods\` 仍是 v65 |

**判据（给测试者）**：日志出现一次
`[replaycompat] replay mouse: wheel now also adjusts the camera speed while the mouse is unlocked (N step(s) applied)`
⇒ 说明这一针生效；没有这行则要 `fml-client-latest.log` 看反射链（本工程无 reobf，该功能靠反射）。

> **注意**：本条只影响**相机飞行速度**；**播放速度**滑块（左上角 `速度: 1.0x`）上游本来就不吃滚轮（12.41 已取证）。

---

### 12.44 「玩家列表切到玩家视角后卡鼠标」⇒ 实现 Shift+ESC 强制复位（v71）

> **来源**（Tester_B）：**`fml-client-latest.log`（72.4 MB / 631211 行，21:32–21:41，装的是 v69）** + 两条描述：
> 「观察者视角没问题」「但是一用玩家列表切换到玩家视角就会出现卡鼠标的情况」，
> 并建议：**「你可以试试加个 shift+ESC 键的强制复位，就相当于将状态恢复到文件刚加载出来的时候」**。

#### 一、日志先说三件好事（v69 的修复被现场验证）

| 判据 | v66 / v68 | **v69（本次）** |
|---|---|---|
| `RELEASED - overlay UI` | 112 / 136 | **3** |
| `input strobing` | 1 | **0** |
| `input screen closed while locked` | — | **1**（修复在工作） |
| 注入失败 / `InvalidInjectionException` | — | **0** |

⇒ **"每秒重建十几次"的循环确实治住了**（频次掉了两个数量级）。

#### 二、★「卡鼠标」的机理（时序是日志自己讲的）

```
21:40:33  Ctrl tap -> LOCKED VISIBLE
21:40:35  probe: toggle=true target=true applied=true      ← 锁定稳定
21:40:36  input screen closed while locked - keeping the mouse visible   ← 输入屏被关
21:40:37  night-vision guard ×3                            ← 切到"有夜视的玩家"视角
21:40:40… probe 一直 toggle=true applied=true              ← 我们始终认为"鼠标可见"
```

**问题 = 状态不自洽**：12.38 的修复**保住了 `mouseVisible` 标志**，但**输入屏已被关闭**；
而上游只在 `mc.currentScreen == null` 时才重挂输入屏 ⇒ **一旦没挂回来，标志说"可见"、实际鼠标被抓** ⇒ 表现为"卡鼠标"。
**⇒ 这是我 12.38 那针的副作用**（保锁太用力，缺了一致性检查）。

#### 三、修复：Shift+ESC 强制复位（`MixinMinecraftShiftEscReset` + `ReplayMouseState.performReset`）

| 项 | 做法 |
|---|---|
| 触发点 | `Minecraft#displayInGameMenu`（**SRG `func_71385_j`**）的 HEAD，`cancellable` —— 这是 **ESC 的唯一入口**（ESC 没有 KeyBinding，而 `Keyboard.isKeyDown` 在本环境恒 false，坑 49） |
| 组合判定 | 反射读 `keyBindSneak.pressed`（`GameSettings.field_74311_E` → `KeyBinding.func_151470_d`） |
| 复位动作 | ① `ReplayMouseState.requestReset()`（由鼠标 mixin 下一帧消费，把 `ctrlToggle` 归零）② `overlay.setMouseVisible(false)`（鼠标抓回 ⇒ 可自由飞）③ **`ReplayHandler.spectateCamera()`**（退出旁观，上游自己的方法）④ `mc.displayGuiScreen(null)`（关掉任何界面）⑤ `ci.cancel()`（Shift 按住时不弹菜单） |
| 仅在回放中生效 | `Minecraft.theWorld == null` 或 `getReplayHandler() == null` 时直接放行，ESC 行为完全不变 |

**★ 这一针踩到并修正了本工程的经典坑**：注入体最初**直接写了 MC 成员名**（`field_71441_e` 等）——
编译 classpath 是 MCP 名的 `recompiled_minecraft`，所以编译就失败了；**更要命的是本工程没有 reobf，
即便编译通过，运行时也找不到**（坑 29）。现已改成：**mixin 注入体里零 MC 成员访问**，
只留 `ReplayMouseState.performReset(this)`，所有字段/方法（SRG+MCP 双拼写）都在 util 里反射解析。
另修正一处：`ReplayModReplay` **没有 `getInstance()`**，只有 **public static 字段 `instance`**。

**离线验证**：注解 `method=["func_71385_j"]` / `value="HEAD"` / `remap=false` / `require=0` / `cancellable`；
20 个 mixin + util 编译退出码全 0；jar 内 **20 个 mixin class**。

#### 四、产物 v71（**未部署**）

| 项 | 值 |
|---|---|
| 文件 | **`<workspace>\replaymod-1.7.10-2.5.2-gtnh-v71-resetkey.jar`** |
| 大小 / SHA256 | `10848602` B / `A31CA1AD06ADEF416EC98A7BF92BF205139B115106A6665D8A6226D5B341C11F` |
| 构成 | v48 基座 + **20 个功能 mixin** + 3 个 util class（= v70 的 19 个 + `MixinMinecraftShiftEscReset`） |
| 待测包 | `release\GTNH-ReplayMod-v71-待测\`（jar + SHA256 + `本次要测什么.md` + `VERIFICATION_GUIDE.md`）+ 同名 `.zip` |
| 部署 | ❌ 未部署；`mods\` 仍是 v65 |

**判据**：日志出现一次 `replay reset (Shift+ESC): mouse released, spectatorship cleared, GUI closed - back to the freshly loaded state`。

> **遗留（仍未解）**：为什么旁观切换后输入屏**没有**被重挂 —— 这属于"根治"（让标志与实际自洽），
> 本轮只做了"急救键"。若 Shift+ESC 成为常用操作，下一步应查 `currentScreen` 在旁观后到底是什么。

---

### 12.45 修「保锁太用力」：加一致性检查（v72，洛洛点名要求根治）

> **起因**：洛洛指出 12.44 的遗留项 —— **「对保锁太用力，缺了一致性检查修复」**。
> 12.44 只给了急救键（Shift+ESC），而"输入屏挂不上却声称鼠标可见"这个**状态不自洽**仍在。

#### 一、病灶（12.38 的副作用）

12.38 为了不让玩家的 Ctrl 锁被 `onGuiClosed` 偷走，改成**"输入屏关闭时跳过那次写入"**。
这治好了循环（实测 `RELEASED` 136 → 3），但**没有配套检查"锁到底有没有生效"**：
一旦输入屏之后没能重挂（上游只在 `mc.currentScreen == null` 时才重挂它），
`mouseVisible` 仍是 `true` 而鼠标实际被抓 ⇒ **玩家看到的是"卡鼠标"**。

#### 二、修复：宽限期 + 自动放锁 + 记录元凶（`MixinGuiReplayOverlayMouseVisible`）

每帧（`updateUserInputGui` 的 HEAD，本来就在轮询）加一段检查：

| 条件 | 行为 |
|---|---|
| `target == true`（玩家要锁着）且**输入屏确实是 `currentScreen`** | 正常，`unmountedFrames` 归零 |
| `target == true` 但**输入屏没挂** | 计数；**第 1 帧**打一条含 `currentScreen` 类名的日志 |
| 连续 **`UNMOUNT_GRACE_FRAMES = 100`** 帧（约 2 秒）仍未挂回 | **自动放锁**：`ctrlToggle/ctrlWasDown/mouseKeyHeld` 归零，并记录"输入屏没回来、已放锁、按 Ctrl 可重新锁定" |
| 字段解析失败（无法判断） | **返回 true ⇒ 不干预**（"判断不了的守卫绝不该改变行为"） |

**新增反射件**：`Minecraft.currentScreen`（`field_71462_r`，MCP+SRG 双拼写，走已有的 `optionalField`）。
"输入屏是否挂着"的判定用**类名包含 `UserInputGuiScreen`**（与既有的 `REPLAY_OVERLAY` 判定同一风格，不依赖编译期类型）。

**为什么是 100 帧而不是"立刻放锁"**：切换旁观需要一点时间重挂输入屏，立刻放锁会把正常操作误判成故障 ——
给宽限期才能同时满足"不误伤"与"不撒谎"。

#### 三、产物 v72（**未部署**）

| 项 | 值 |
|---|---|
| 文件 | **`<workspace>\replaymod-1.7.10-2.5.2-gtnh-v72-consistency.jar`** |
| 大小 / SHA256 | `10849200` B / `CECF84BD22B5E1E81F55903E2E545794511D2C84A8A17FC161014CF523424ACD` |
| 构成 | v48 基座 + **20 个功能 mixin** + 3 个 util（与 v71 同数量，本次只改 `MixinGuiReplayOverlayMouseVisible`） |
| 待测包 | `release\GTNH-ReplayMod-v72-待测\`（jar + SHA256 + `本次要测什么.md` + `VERIFICATION_GUIDE.md`）+ 同名 `.zip` |
| 部署 | ❌ 未部署；`mods\` 仍是 v65 |

#### 四、下一步（根治的最后一块拼图）

新日志会告诉我们**旁观切换时 `currentScreen` 究竟是哪个类**：
- 若是 `GuiPlayerOverview`（玩家列表没关）⇒ 根因是"切视角时没关列表"，可以在那一步修；
- 若是 `UserInputGuiScreen` 自己（挂上了但状态不同步）⇒ 说明判定条件要改；
- 若两行日志都**没出现**（即锁一直有效）⇒ 说明 v72 已经不需要补救了。

⇒ **拿到那条 `currentScreen=…` 就能收口。**

---

### 12.46 又一份日志 + 录屏：**没能覆盖"卡鼠标"现场**（2026-10-11 21:44–21:51，Tester_B）

> **来源**：`fml-client-latest (1).log`（29.7 MB / 250072 行，21:44:37–21:51:20）
> + `屏幕录制 2026-10-11 215007.mp4`（**3 分 15 秒** / 2560x1600 / 30fps）。

#### 一、为什么这一轮用不上（两条硬原因）

1. **日志里装的是 `replaymod-1.7.10-2.5.2-gtnh-v69.jar`** —— **不是 v72**，所以 12.45 新加的
   `currentScreen=…` 诊断**一行都没有**；
2. **录屏 21:50:07 才开始**，而现场是 **21:48:58–21:49:33** ⇒ 等他开始录时**已经脱离回放**。
   **画面证据**（第 30 秒那一帧）：有血条、物品栏、JourneyMap 小地图、Waila「石头 #0001」、左上角"录制中"，
   **没有时间轴 UI** ⇒ 那是**正常游戏世界**，不是回放。

#### 二、但时间线被完整复现（与 12.44 的模式一模一样）

```
21:48:14  Ctrl tap -> LOCKED VISIBLE (switch #1) → 21:48:15 GRABBED (switch #2)
21:48:55  Ctrl tap -> LOCKED VISIBLE (switch #3)
21:48:55  probe: toggle=true target=true applied=true
21:48:58  input screen closed while locked - keeping the mouse visible
21:48:59  night-vision guard ×3                              ← 切到"有夜视的玩家"
21:49:08  camera view: spectating 1070b099-…, leaving the view entity alone   ← 12.28 相机补丁正确地不干预
21:49:33  （最后一条 replay mouse probe）
之后      ★ 再无任何 [replaycompat] 输出 ⇒ overlay 停止更新 ⇒ 玩家已退出回放
```

⇒ **`probe` 消失 = overlay 不再更新**，即他在这前后**离开了回放**；而日志里**没有任何"退出回放"的辅助线索** ——
结合他早前那句「长按 ESC 才能操作」，**他是靠 ESC 菜单硬退出来的**（v69 没有 Shift+ESC）。

#### 三、顺手发现的新噪音（未定责）

`journeymap` 在旁观期间**疯狂报错**，从 21:49:21 持续到 21:50:11 之后，每秒数条，**连 `Client thread` 都在报**：

```
[ERROR] [journeymap/]: Couldn't get service name: java.lang.NullPointerException:
        Cannot invoke "String.replace(char, char)" because … is null
```
⇒ 多半只是 JourneyMap 在回放/旁观下取不到服务名（与"卡鼠标"未必相关），
但它**会淹没日志**（这一份 29.7 MB 里很大一部分是它 + `SkinPort` 的 DEBUG 刷屏）。

#### 四、下一轮要什么（**照这个来一次就能收口**）

1. **装 v72**（`release\GTNH-ReplayMod-v72-待测\`）；
2. **录屏从"点开玩家列表之前"就开始**，一直录到"脱离回放"；
3. 复现顺序：进回放 → Ctrl 放开鼠标 → **用玩家列表点玩家切换视角** → 等 5 秒 → 试鼠标 / 试 WASD；
4. 把 **`fml-client-latest.log`** 发来 —— v72 会打这两行中的至少一行：
   - `replay mouse: locked, but no input screen is mounted (currentScreen=……)`
   - `replay mouse: input screen did not come back within 100 frames - releasing the lock (currentScreen=……)`

**⇒ 那条 `currentScreen=……` 就是 12.45 说的"最后一块拼图"。**

---

### 12.47 「保存录像时崩溃」—— `SquashFilter` 批量读取越界（v73）

> **来源**：洛洛转的 `debug.log`（46.2 KB，崩溃报告，`Time: 2026-10-11 22:13:11`）+ 一张回放动图。
> **崩溃报告里的 mod 列表显示装的是 `replaymod-1.7.10-2.5.2-gtnh-v72.jar`**（336 mods，Java 25.0.3）
> ⇒ **Tester_B已经拿到 v72 了**，但这份是崩溃报告，**不含我们的补丁输出**（那些只在 `fml-client-latest.log`）。

#### 一、崩溃原文与定性

```
Description: Saving replay file
java.lang.IndexOutOfBoundsException: readerIndex(8) + length(103086) exceeds writerIndex(27768): PooledUnsafeDirectByteBuf(...)
    at ...AbstractByteBuf.readBytes
    at ...ByteBufNetInput.readBytes(ByteBufNetInput.java:106)
    at com.replaymod.replaystudio.protocol.packets.PacketChunkData.readBulkV1_7(PacketChunkData.java:131)
    at com.replaymod.replaystudio.protocol.packets.PacketChunkData.readBulk(PacketChunkData.java:92)
    at com.replaymod.replaystudio.filter.SquashFilter.onPacket(SquashFilter.java:370)
    at com.replaymod.editor.gui.MarkerProcessor.apply(MarkerProcessor.java:212)
    at com.replaymod.recording.packet.PacketListener.lambda$channelInactive$6(PacketListener.java:296)
    at java.lang.Thread.run(Thread.java:1474)
```

**人话**：保存录像时 `SquashFilter` 要压缩一个"批量区块数据"包，而该包**声明的长度（103086）超过了它实际携带的字节（27768）**
⇒ ReplayStudio 读越界 ⇒ 异常**从保存线程逃逸**（没有 catch）⇒ **录像丢失 + 游戏崩**。
**定性：上游鲁棒性缺口**，与 ReplayMod issue **#388 / #770**（同样在 `SquashFilter` 抛 `IndexOutOfBoundsException`）同一族。

#### 二、修复 `MixinSquashFilterSafeBulkRead`（v73）

`SquashFilter#onPacket` 里**恰好两处**批量读取（`javap -c` 确认偏移 1016 / 1080）：

| 调用 | 签名 |
|---|---|
| `PacketChunkData.readBulk(Packet)` | `public static List<Column> readBulk(Packet) throws IOException` |
| `PacketBlockChange.readBulk(Packet)` | `public static List<PacketBlockChange> readBulk(Packet) throws IOException` |

两处都用 `@Redirect` 包上 try/catch，**失败时返回 `Collections.emptyList()`**：

- **为什么是空列表而不是 null**：上游紧接着就是 `readBulk(packet).iterator()`（偏移 1019），返回 null 只会把崩溃挪成 `NullPointerException`；
- **取舍**：与 12.26（entity tracker 逐包容错）一致 —— **丢一个坏包，保住录像与游戏**；
- 日志限流 5 条：`replay saving: skipped an unreadable … packet instead of failing the whole save (…)`。

**离线验证**（`javap -v`）：两处 `@Redirect` 的 `method=["onPacket"]` / `value="INVOKE"` /
`target="…PacketChunkData;readBulk(…)Ljava/util/List;"` 与 `…PacketBlockChange;readBulk(…)Ljava/util/List;"` / `remap=false` / `require=0`。

#### 三、产物 v73（**未部署**）

| 项 | 值 |
|---|---|
| 文件 | **`<workspace>\replaymod-1.7.10-2.5.2-gtnh-v73-savesafe.jar`** |
| 大小 / SHA256 | `10850542` B / `A8D948D62AF0297A97478AA89C59D57CD31E541DF35BA78CBD6D95847B664DD8` |
| 构成 | v48 基座 + **21 个功能 mixin** + 3 个 util（= v72 的 20 个 + `MixinSquashFilterSafeBulkRead`） |
| 待测包 | `release\GTNH-ReplayMod-v73-待测\`（jar + SHA256 + `本次要测什么.md` + `VERIFICATION_GUIDE.md`）+ 同名 `.zip` |
| 部署 | ❌ 未部署；`mods\` 仍是 v65 |

> ⚠️ **仍然缺 v72 的现场日志**：那条 `currentScreen=……` 只有在 **`fml-client-latest.log`** 里才有
> （崩溃报告 `debug.log` 不含 `System.out`）。拿不到它，"卡鼠标"的根因就只能停在 12.45 的"自动放锁"缓解层。

#### 四、★ 触发条件（Tester_B 2026-10-11 22:14 补充）

> 原话：**「情况是游戏内暂停/停止录制后再度录制保存的时候出现的」**

**这让它从一个偶发崩溃变成一条可复现序列。** 两个机理假设（都指向"这个坏包不是随机产生的"）：

1. **停止/暂停录制留下的"未完成文件"**：ReplayMod 停止录制时可能留下没写完的文件
   （这正是 12.20 那套 `replay_recordings\raw\` / `.mcpr.tmp` / `.no_recover` 机制存在的原因）；
   再度录制后保存时若也去处理它，就容易遇到"声明长度 > 实际字节"的包；
2. **`SquashFilter` 是重状态的流式压缩器**：字段里是
   `chunks` / `unloadedChunks` / `entities` / `teams` / `maps` / `currentWorld` / `currentWindow` / `loginPhase` …
   而 `MarkerProcessor` 里既有 `new SquashFilter()`（字节码偏移 161、727），也有 `copy()`（567、591）与
   `release()`（562、717、1624、1634）—— **状态在"停止 / 再开始"之间若被复用或复制不同步**，
   后续包的长度就会对不上。

**⇒ 无论哪一条，v73 的修复都覆盖**：`SquashFilter#onPacket` 里那**两处**批量读取都被包住，
长度不对只会**跳过该包**，异常再也不会逃出保存线程。**代价**：那一个区块包丢失（画面可能缺一小块），
换回**录像与游戏都保住** —— 与 12.26（entity tracker 逐包容错）同一取舍。

**⇒ 下一步**：让Tester_B用 v73 **照这条序列复现一次**；若不崩即修复生效；
若仍崩，则说明还有**别的**读包点（v73 只覆盖了 `SquashFilter` 的两处），那时需要 `fml-client-latest.log` 里的新栈。

---

### 12.48 「点时间轴直接跳到最后」**依然存在** ⇒ 转向坐标换算 + 加诊断探针（v74）

> **来源**：Tester_A（转述）：「**但是点时间轴直接跳到最后的问题还在**」（并 @ 了「Luoxueruoling」——**那就是洛洛本人**，Tester_A是在向洛洛报告）。
> 同轮Tester_B回答上一轮的问题：**「没产生文件」**。

#### 一、★ 12.36 的解释被推翻

12.36 把"点时间轴跳末尾"归因为**输入屏被反复重建 ⇒ 点击被当拖动**；12.38/12.45 已把那个循环治掉
（实测 `RELEASED` 从 136 降到 3），**但症状依旧** ⇒ **说明与输入屏无关**，得换方向。

#### 二、新方向：jGui 的鼠标坐标换算（与 12.15 的滚轮量纲同一族）

`MouseUtils.getMousePos()`（`javap -c` 全文）：

```java
Point scaled = getScaledDimensions();            // ScaledResolution 宽高
int x = Mouse.getX() * scaled.getX() / mc.field_71443_c;   // field_71443_c = displayWidth
int y = scaled.getY() - Mouse.getY() * scaled.getY() / mc.field_71440_d;
```

**它假设 `Mouse.getX()` 与 `Minecraft.displayWidth` 是同一套像素。**
而 `org.lwjgl.input.Mouse.getX()` → `org.lwjglx.input.Mouse.getX()` 只是 `return x`，
那个 `x` 由 **SDL 鼠标移动事件**写入（`addMoveEvent` / `poll()` 里 `x = latestX`）；
**同一个类里还用到 `Display.getPixelScaleFactor()`** ⇒ lwjgl3ify 自己就有 DPI 缩放的概念。

**本环境是 150% DPI**（12.14 实测：`GetClientRect` 的虚拟化尺寸 569x320 vs 真值 854x480，正好 1.5 倍）。
⇒ **若一边是物理像素、一边是逻辑像素，算出的 x 就被整体放大** ⇒ 点击位置被推到最右端
⇒ **"点哪都跳到最后"** —— 这解释了为什么它与输入屏修补无关。

#### 三、不猜方向：加只读探针（v74）

新 mixin `MixinMouseUtilsCoordinateProbe`（`@Inject` 到 `MouseUtils.getMousePos` 的 RETURN），
每 40 次调用打一条、最多 24 条，**不改变任何行为**：

```
[replaycompat] mouse pos probe: raw=(…) display=(…) scaled=(…) gui=(…)
```
| 字段 | 来源 |
|---|---|
| `raw` | `Mouse.getX()/getY()`（反射，`org.lwjgl.input.Mouse`） |
| `display` | `Minecraft.displayWidth/displayHeight`（`field_71443_c` / `field_71440_d`） |
| `scaled` | `MouseUtils.getScaledDimensions()` |
| `gui` | `getMousePos()` 的返回值（**时间轴就是按它算位置**） |

**判据**：比较 `raw.x / display.x` 与 `gui.x / scaled.x` ——
一致 ⇒ 坐标无问题，换方向；差同一系数（如 1.5）⇒ **DPI 像素空间不一致**，据此修换算。

#### 四、Tester_B那条回话的价值：**机理假设 ① 被否定**

问"崩溃后 `replay_recordings\` 有没有留下 `.tmp`/多余文件"，答：**「没产生文件」**
⇒ 12.47 的第一个机理（"处理停止录制留下的未完成文件"）**不成立**；
嫌疑集中到第二个：**`SquashFilter` 那套重状态在"停止/再开始"之间被复用或复制不同步**
（v73 的跳过坏包已经能让它不崩，但要根治得看那份 `fml-client-latest.log` 的新栈）。

#### 五、产物 v74（**未部署**）

| 项 | 值 |
|---|---|
| 文件 | **`<workspace>\replaymod-1.7.10-2.5.2-gtnh-v74-posprobe.jar`** |
| 大小 / SHA256 | `10852986` B / `EF8B05F06A0682886728F7BE6A55246AE69F8DE8EADFF24B5C6735B01E636DA4` |
| 构成 | v48 基座 + **22 个功能 mixin** + 3 个 util（= v73 的 21 个 + `MixinMouseUtilsCoordinateProbe`） |
| 待测包 | `release\GTNH-ReplayMod-v74-诊断\` + 同名 `.zip` |
| 部署 | ❌ 未部署；`mods\` 仍是 v65 |

> **这一版的价值全在那几行日志**：拿到 `mouse pos probe` 之后，"点时间轴跳末尾"要么当场结案（修换算），
> 要么被干净地排除（换方向），不会再像 12.36 那样靠推测白走一轮。

---

### 12.49 两位测试者的新日志：三个根因全部定案 + 交付 **v75**（2026-10-11 深夜）

> **来源**：洛洛转来的两份 `fml-client-latest.log` + 两条文字反馈：
> Tester_B = `fml-client-latest (2).log`（`90973171` B / 746289 行 / 22:09–22:27 / 装的是 **v72**）；
> Tester_A = `fml-client-latest (3).log`（`39049809` B / 281987 行 / 22:23–22:29 / 装的是 **v74**）。
> 「依旧会像之前一样卡住，然后急救键没用，但是在 replay 设置里调整一个开关就不会卡住了，然后就是滑动滚轮依旧不能加速旁观者视角」
> ／「人物不渲染，多点了几下时间轴就无法移动了」「以及还是时间轴无法调」。
> **本轮没有跑游戏**：全部结论来自这两份日志 + 字节码（`javap`，外加本轮新写的 ASM 调用点审计器）。

#### 〇、先记两个好消息：两条旧修复在真环境**验收通过**

| 判据 | 证据（Tester_B v72 日志） |
|---|---|
| **12.26 entity tracker 逐包容错** | **5 条** `entity tracker: packet with an unreadable entity id was skipped (IndexOutOfBoundsException: readerIndex(1) + length(4) exceeds writerIndex(4) ...)` ⇒ 12.26 写下的验收判据（"出现我们这行且 tracker 正常加载完"）**达成** —— 那份本机复现不了的坏录像，在他这里复现了，而且不再弹窗 |
| **12.25 夜视守卫** | 3 条 `night-vision guard: renderViewEntity had the effect but the local player does not ...` |
| 注入健康 | 两份日志 `Mixin apply for mod replaymod failed` = **0**、`InvalidInjectionException` = **0**；v74 的 22 个 mixin 逐条 `Mixing …` 成功，包括 `MixinMouseUtilsCoordinateProbe` |

#### 一、v74 探针结案：**「点时间轴跳到最后」的坐标量纲假设被排除**

24 条探针全部长这样（Tester_A，2560×1406 窗口）：

```
[replaycompat] mouse pos probe: raw=(0,0) display=(2560,1406) scaled=(640,352) gui=(515,142)
... gui=(234,163) (23,333) (273,137) (143,112) (13,26) (395,38) (461,20) ...
```

两条判读：

1. **`raw` 恒为 `(0,0)`** ⇒ 探针反射读错了类（**坑 57**，21.49 二节）；`gui` 却在 13…515 之间正常移动
   ⇒ jGui 实际用的鼠标值**是好的**。
2. **`gui` 全程落在 `scaled=(640,352)` 之内，没有任何越界** ⇒ **换算链路自洽**。
   而且 `AbstractGuiTimeline.getTimeAt`（`javap -c` 核实）里自己会
   `getContainer().convertFor(this, point)`，**越界时返回 -1**（= 点不着，不会"跳到末尾"）——
   若真存在 1.5 倍放大，表现会是"点时间轴没反应"，而不是"跳到最后"。

⇒ **12.48 的 DPI 像素空间假设可以排除**，症状要换方向（候选：jGui 的 `convertFor`/`getLastSize` 与布局
不同步；或输入屏生命周期造成的假点击/拖动）。探针本身已按坑 57 修好，v75 起能同时给出真实 `raw`。

#### 二、「滚轮不能加速旁观者视角」—— **v70 那个补丁从来没有生效过一次**（坑 57）

lwjgl3ify 的 redirector 把 `org.lwjgl.input.Mouse` 的**字节码调用点**改写到真实现
`org.lwjglx.input.Mouse`，**但反射不受改写影响**；v70 的滚轮补丁恰恰是
`Class.forName("org.lwjgl.input.Mouse").getMethod("getEventDWheel")` ⇒ 落在**兼容壳**上，读到的**恒为 0**。

两处独立证据互相印证：v72/v74 日志里 `wheel now also …` **0 条**（尽管
`MixinUserInputGuiScreenWheelCameraSpeed` 的 `Mixing …` 明确注入成功），以及探针的 `raw=(0,0)`。

**修法（v75）**：新增 `com.gtnh.replaycompat.util.LwjglMouseBridge` —— 依次尝试
`org.lwjglx.input.Mouse` → `org.lwjgl.input.Mouse`；滚轮补丁与坐标探针都改走它。

#### 三、「卡鼠标 / 急救键没用」—— 玩家列表把**整个 overlay** 隐藏了（`visible=false`）

**字节码证据**（`iconst_0` / `iconst_1` 都核对过）：

```
PlayerOverviewGui.<init>()   -> GuiReplayOverlay.setVisible(false)   // 玩家列表：隐藏 overlay
PlayerOverviewGui.close()    -> GuiReplayOverlay.setVisible(true)
GuiKeyframeRepository.display()/close()                          同样
```

而 overlay 挂输入屏的逻辑（`AbstractGuiOverlay.updateUserInputGui`，`javap -c` 全文）是：

```java
if (visible) {                                  // ★ 12.45/12.72 的检查漏掉的第三个条件
    if (mouseVisible) { if (mc.currentScreen == null) mc.displayGuiScreen(userInputGuiScreen); }
    else              { if (mc.currentScreen == userInputGuiScreen) mc.displayGuiScreen(null); }
}
```

`visible == false` 时**整段被跳过** ⇒ 我们设的"鼠标可见"是空话、输入屏永远挂不上、鼠标保持被抓 = **卡住**。
两份日志各有 **186 / 189 行** `locked, but no input screen is mounted (currentScreen=null)` 与之吻合；
`GuiCompressedChest` 那几次则是另一个 mod 的界面（那时报"没挂"是**正确**的）。

**为什么 v72 的"2 秒自动放锁"一次都没触发**（两份日志 `did not come back` = 0）：旧实现数的是
**连续 100 帧**，而输入屏一挂一回就把计数清零 —— **抖动型故障下连续计数永远到不了阈值**（**坑 58**）。

**修法（v75，两处）**：
① `visible == false` 时**立刻释放我们的锁**（让位：玩家列表/关键帧仓库自己管鼠标），日志说明原因；
② 宽限期改为**按 1 秒窗口的未挂载比例**判定（`>1/2` 记一个坏窗口，连续 2 个坏窗口 ⇒ 放锁），抖动也能抓住。

#### 四、「急救键没用」—— Shift 的按键状态被游戏自己清掉了

`displayGuiScreen → setIngameNotInFocus → KeyBinding.unPressAllKeys()`（**坑 48**）会把 Shift 的
`pressed` 清成 false，于是"按住 Shift+ESC"经常判定不到 —— 两份日志里 `replay reset (Shift+ESC)` **0 次**。

**修法（v75）**：`ReplayMouseState` 增 `lockIneffective`（由鼠标补丁每帧发布"锁是否真的生效"），
`performReset` 的条件改成 `Shift 按住 || 锁失效` ⇒ **锁失效时普通 ESC 就直接复位**（不需要 Shift）；
锁正常时 ESC 行为逐位不变（照旧打开菜单）。

#### 五、交付 **v75**（**未部署**）

| 项 | 值 |
|---|---|
| 文件 | **`<workspace>\replaymod-1.7.10-2.5.2-gtnh-v75-mousefix.jar`** |
| 大小 / SHA256 | `10854976` B / `1B033B2AE10C14B22F174BEAC85E2D41844207C32475B1031FE09049C8A1B59E` |
| 构成 | v48 基座 + **22 个功能 mixin** + **4 个 util**（新增 `LwjglMouseBridge`）；改动了 `MixinGuiReplayOverlayMouseVisible` / `MixinUserInputGuiScreenWheelCameraSpeed` / `MixinMouseUtilsCoordinateProbe` / `ReplayMouseState` |
| 编译 | 4 个 util + 22 个 mixin **逐个 `javac`，退出码全 0**（坑 47 的纪律） |
| 注入后校验 | `javap -c`：滚轮补丁的字节码里出现 `LwjglMouseBridge.isAvailable/getEventDWheel`；overlay 补丁里出现 `fieldVisible` / `overlayVisible()` / `resetUnmountStats()`；jar 内 **26 个 class** |
| 待测包 | `release\GTNH-ReplayMod-v75-待测\`（jar + `SHA256.txt` + `本次要测什么.md` + `VERIFICATION_GUIDE.md`）+ 同名 `.zip`（`9754878` B） |
| 部署 | ❌ **未部署**；`mods\replaymod-…-gtnh.jar` 仍是 **v65**（`907B72B8…45FCC`，已复核未变） |

#### ★ 新增坑 57 / 58

**57. ★★ lwjgl3ify 的 redirector 只改写「字节码调用点」，**反射会落到兼容壳**（值恒 0 / 永远过期）。**
本环境里 `org.lwjgl.input.Mouse` **这个类存在**、方法也**能解析**，所以反射不报错 —— 它只是永远返回 0。
⇒ **凡是"绕开 redirector"的反射写法（本工程为了避开 GLSMRedirector 大量使用反射），都必须先确认目标类
是不是真实现**：`org.lwjgl.input.*` → 改读 **`org.lwjglx.input.*`**。本坑同时废掉了 v70 的滚轮功能
（`getEventDWheel` 恒 0）与 v74 探针的 `raw`。判据：**探针打出来的值全 0 或恒定，就先怀疑反射目标**。

**58. ★★ 判定"某个状态一直没生效"要用「时间窗口比例」，不要用「连续帧数」。**
v72 的自动放锁数的是连续 100 帧，而故障是"挂上—掉下"反复 ⇒ 每次短暂挂上都把计数清零，
**阈值永远到不了**（两份日志 186/189 条告警、0 次放锁）。改成"每 1 秒窗口里未生效帧占比 > 1/2"累计 2 个窗口
即可同时抓住"完全不生效"和"半数时间不生效"。**推论：所有"超时即自愈"的设计都要问一句"它会不会被抖动重置"。**

#### 六、下一步

1. **发 v75**（`release\GTNH-ReplayMod-v75-待测\` + `.zip`），回收三样判据：
   滚轮那行 `wheel now also …` ·让位/窗口判定那两行 `the lock cannot take effect - …` ·`replay reset (Shift+ESC) …`；
2. **v65 或 v75 发到 338 环境**复验（导出 / 路径关键帧 / 换视角 / 相机 / Shift 的 7 项，见 `338复验指引.md`）；
3. 仍开着：**只剩天空**（要两张 F3：`00:00` 与出问题处）、**保存录像崩溃**（v73 后按"暂停/停止→再录→保存"复现）、
   **服务端实体包漏录**（12.31/12.33 的实机诊断，一句话就跑）；
4. **「点时间轴跳到最后」换方向**（坐标量纲已排除）：优先怀疑 jGui 的 `convertFor`/`getLastSize` 与布局不同步，
   或输入屏生命周期造成的假点击/拖动。

---

### 12.50 v75 日志到手：坐标换算被硬证据排除 +「点几下时间轴就不能动」真凶定案 —— 交付 **v76**（2026-10-11）

> **来源**：`fml-client-latest (4).log`（`42393398` B / 319043 行 / 22:52–22:59 / 装的是 **v75**，反馈者同前）。
> **本轮同样没有跑游戏**：结论全部来自日志 + 字节码。

#### 一、探针修好后的**硬证据**：坐标换算完全正确 ⇒「点时间轴跳最后」的坐标假设**彻底出局**

```
[replaycompat] mouse pos probe: raw=(1280,703) [org.lwjglx.input.Mouse] display=(2560,1406) scaled=(640,352) gui=(320,176)
```

逐条验算（`gui.x = raw.x × scaled.x / display.x`）：`1280→320` ✔ `393→98` ✔ `71→17` ✔ `825→206` ✔，
24 条样本里**没有一条越界**。⇒ **12.48 的「150% DPI 像素空间不一致」假设可以正式结案为"排除"**；
症状必须往别处查（jGui 的 `convertFor`/`getLastSize` 与布局不同步，或输入屏生命周期造成的假点击/拖动）。

#### 二、v75 那套一致性检查**一条都没触发** —— 两个设计错误（→ 坑 59）

`the lock cannot take effect` **0 次**、而 `locked, but no input screen is mounted` 仍有 **189 条**（全挤在 22:57–22:58）。
两条原因，都是我的实现问题：

1. **检查寄生在错误的位置**：`updateUserInputGui()` 的自动调用者只有合成访问器 **`access$100`**，
   而它由 **`UserInputGuiScreen` 自己**（输入屏的 tick 路径）调用 ⇒ **输入屏一被拆掉，就不会再有人调
   `updateUserInputGui()`**，我们的 HEAD 检查也跟着停摆（日志里 1.5 次/秒的稀疏调用正是
   `setMouseVisible` 偶发触发的结果）。而 189 条告警正是"拆掉之后、下一次稀疏调用"时才被看见的。
2. **判定仍会被抖动重置**：窗内计数只在 `!mounted` 时递增，而 `mounted == true` 的帧走
   `resetUnmountStats()` 把 `badWindows` 一起清零 ⇒ 阈值永远到不了（与 12.45 的"连续 100 帧"是**同一个病**，
   我只是把病灶从"帧计数"搬到了"另一个计数"上）。

#### 三、真凶：`runTick` 的鼠标循环会调 `setIngameFocus()`（字节码铁证）

`Minecraft.func_71407_l`（runTick）里：

```
552: getfield Minecraft.field_71462_r      // currentScreen
556: ifnonnull 579                          // == null 才继续
559: getfield Minecraft.field_71415_G      // inGameHasFocus
563: ifne 593                               // == false 才继续
566: invokestatic org/lwjgl/input/Mouse.getEventButtonState()
569: ifeq 593                               // 有按键按下才继续
572: invokevirtual Minecraft.func_71381_h   // ★ setIngameFocus()
```

而 `func_71381_h` 内部是 `grabMouseCursor()` + **`displayGuiScreen(null)`**。

⇒ **只要玩家按左键**（点时间轴、点按钮），而那一刻 `currentScreen == null` 且未聚焦，
游戏就会**抢走鼠标并关掉回放输入屏**。这正是测试者那句
**「多点了几下时间轴就无法移动了」**，也是那 ~190 条告警的来源（每次点击拆一次屏）。

#### 四、交付 **v76**（**未部署**）

| 项 | 值 |
|---|---|
| 文件 | **`<workspace>\replaymod-1.7.10-2.5.2-gtnh-v76-nofocusgrab.jar`** |
| 大小 / SHA256 | `10855176` B / `41DD2D0FD636C4D93D15F1AB655150E3615280D3DA77BEA6C7A7E07C5BD30F8E` |
| 改动 | `ReplayMouseState.shouldSuppressIngameFocus()`（"回放中 + 玩家 Ctrl 锁着鼠标"才为真）+ `MixinMinecraftShiftEscReset` 新增 `@Inject(method = "func_71381_h", HEAD, cancellable)` ⇒ **锁定时抑制 setIngameFocus**；其余同 v75 |
| 编译 / 校验 | 22 mixin + 4 util 逐个 `javac` **退出码全 0**；`javap` 确认 `keepReplayMouseVisible` 与 `shouldSuppressIngameFocus` 都在 jar 里（26 class） |
| 待测包 | `release\GTNH-ReplayMod-v76-待测\`（jar + `SHA256.txt` + `本次要测什么.md` + `VERIFICATION_GUIDE.md`）+ `.zip`（`9753918` B） |
| 部署 | ❌ 未部署；`mods\` 仍是 **v65**（已复核），无 java 进程 |

**判据**（现场复验）：`locked, but no input screen is mounted` 的条数应从 ~190 **大幅下降**（个位数或 0），
且"点时间轴/点按钮之后鼠标依然可用"。

#### ★ 新增坑 59

**59. ★★ 不要把"每帧检查"寄生在一个**只在特定状态下才被调用**的方法上。**
`AbstractGuiOverlay.updateUserInputGui()` 的唯一自动调用者是合成访问器 `access$100`，由**输入屏自己**调用
⇒ 输入屏一被拆掉，所有寄生在它上面的逻辑（包括"把输入屏挂回来"的逻辑）**一起消失** ——
本工程为此白测了 v72/v75 两版。**凡是要"每帧兜底"的守卫，都必须挂在每帧必然执行的位置**
（`Minecraft.runTick` / `EntityRenderer`），而不是挂在"故障发生后就不会再被调用"的地方。

**同源推论（真凶）**：回放里抢鼠标的不是我们的补丁，而是 **`runTick → setIngameFocus()`**（左键点击触发）；
`setIngameFocus` 内部会 `displayGuiScreen(null)`，因此它同时也是"输入屏被拆掉"的元凶。

---

### 12.51 滚轮的真障碍（事件队列时序）+ v75 效果实测 —— 交付 **v77**（2026-10-11 深夜）

> **来源**：`fml-client-latest (5).log`（`66738871` B / 533595 行 / 23:00–23:10 / 装的是 **v75**）
> + 反馈「现在卡鼠标现象**偶而发生**……但是**有所好转**，然后**依旧没办法滚轮加速**」
> + 测试者的推测「滚轮没法调怀疑是回放的游戏模式问题导致的，判断理由：回放时会出现血条」。

#### 一、v75 的效果**实测有效**（"有所好转"是真的）

| 判据 | v75 上一份日志 | **本次（v75）** |
|---|---|---|
| `locked, but no input screen is mounted` | 189 | **94** |
| `the lock cannot take effect - …`（让位/放锁） | **0** | **5** |
| `LOCKED VISIBLE` / `LOCKED GRABBED`（玩家操作） | 3 / 2 | 23 / 16 |
| `input screen closed while locked` | 1 | 1 |

⇒ 让位与"放锁"逻辑**确实在工作**（5 次），告警减半；**但频繁按 Ctrl 的症状仍在** ⇒ 说明
"左键点击 ⇒ `setIngameFocus()` 抢鼠标"（12.50 的 v76）才是主路径 —— 而这份日志证明**测试者当时拿的还是 v75，v76 尚未被测**。

#### 二、滚轮的真障碍：**事件队列被 `super` 消费掉了**（字节码）

`AbstractGuiOverlay$UserInputGuiScreen.func_146274_d`：

```
0: invokespecial GuiScreen.func_146274_d()      // ★ while (Mouse.next()) 把整个鼠标队列消费掉
4: invokestatic Mouse.hasWheel()
10: invokestatic Mouse.getEventDWheel()          // ← 此时读到的已是【队尾】
```

而 lwjgl3ify 的实现是 `getEventDWheel() { return wheelEvents[queue.getCurrentPos()]; }` ——
**`super` 循环结束后 `getCurrentPos()` 停在队尾，值恒为 0**。

> LWJGL2 会把最后一个事件保留在 `currentEvent` 里，所以上游这么写没问题；**换到 lwjgl3ify 上就永远是 0**。
> 这也顺带说明：jGui 的"滚轮分发给列表"（12.15 修过的那条）在这套环境里同样收不到东西。

**修法（v77）**：把我们的读取点从 **TAIL 挪到 HEAD**（`super` 之前）—— 那是整条路径上唯一还能读到真实值的位置；
jGui 自身行为一字未改。

> **回应测试者的推测**：血条与滚轮无关。血条的原因是 **GTNH 不发 `RenderGameOverlayEvent.HEALTH`**（12.29 实测，
> 同一 handler 收得到 ALL/FOOD/AIR/EXPERIENCE/CROSSHAIRS，**独独没有 HEALTH**）⇒ ReplayMod 没有东西可取消。
> 这一条已裁定接受，**不是游戏模式问题**，也与滚轮无关；滚轮的障碍是上面这条时序。

#### 三、交付 **v77**（**未部署**）

| 项 | 值 |
|---|---|
| 文件 | **`<workspace>\replaymod-1.7.10-2.5.2-gtnh-v77-wheelhead.jar`** |
| 大小 / SHA256 | `10855327` B / `30ED4C0D1F5E47558BCDF781DD8F5F767E9189630E36E1798B4A959B1EBB0FF7` |
| 改动 | `MixinUserInputGuiScreenWheelCameraSpeed` 注入点 **TAIL → HEAD** + 新增"读到滚轮却应用不了"的限流诊断（`replaycompat$explainOnce`）；其余同 v76 |
| 编译 | 22 mixin + 4 util 逐个 `javac` **退出码全 0** |
| 待测包 | `release\GTNH-ReplayMod-v77-待测\` + `.zip`（`9754559` B） |
| 部署 | ❌ 未部署；`mods\` 仍是 **v65**（已复核），无 java 进程 |

**判据**：日志出现 `replay mouse: wheel now also adjusts the camera speed …`；
若出现新加的 `wheel camera speed: the wheel was seen but could not be applied - …`，则按它给的原因继续查。

#### ★ 新增坑 60

**60. ★★ lwjgl3ify 下读「当前鼠标事件」必须在**消费者把队列读完之前**。**
`Mouse.getEventDWheel()` = `wheelEvents[queue.getCurrentPos()]`，而 `GuiScreen.handleMouseInput` 内部
`while (Mouse.next())` 会把队列**整个消费掉** —— 循环之后 `getCurrentPos()` 停在队尾，读出恒 0。
LWJGL2 的"最后一个事件会留在 `currentEvent` 里"这条语义在这里**不成立**，
所以所有"在 super 之后读鼠标事件"的写法（包括上游自己的 jGui 分发）都要改成**在 super 之前读**。

---

### 12.52 时间轴的新症状「只能往后调，往前直接飞走」+ 每帧兜底 —— 交付 **v78**（2026-10-11 深夜）

> **来源**：`fml-client-latest (6).log`（`79546071` B / 625505 行 / 23:10–23:22 / 装的是 **v76**）
> + 两位测试者的截图：
> Tester_A「单人存档没出问题」「唯一问题就是还是调不了上面的大时间轴」「**只能往后调**」「**往前就直接飞到最（前/后）了**」；
> Tester_B「我单人时间轴能调吗喵~」「我不行」「**上面的条压根不动的**」。
> 另有「服务里多人我角色又成滚木了（= 人物消失）」。

#### 一、v76 的效果（继续改善，未归零）

| 判据 | v75 前 | v75 | **v76** |
|---|---|---|---|
| `locked, but no input screen is mounted` | 189 | 94 | **85** |
| `the lock cannot take effect - …` | 0 | 5 | 1 |

#### 二、时间轴：**方向性症状**把范围缩到"视口状态"（`offset` / `zoom` / `size`）

`AbstractGuiTimeline.getTimeAt`（字节码）：

```java
if (getLastSize() == null) return -1;
container.convertFor(this, point);
if (x < 0 || y < 0 || x > size.width || y > size.height) return -1;
int w = size.width - 8;
double rel = (length * zoom) * (x - 4) / w;
return max(0, min(length, round(offset + rel)));      // ★ offset 是绝对时间偏移
```

- **"只能往后调"** ⇒ 最像 **`offset` 停在很大的值**（视口滚到后段没复位）：点左端算出的是 `offset` 而不是 0；
- **"往前直接飞到最前/最后"** ⇒ 像 **`size`（时间轴宽高）过期或异常**，`(x-4)/w` 爆掉后被 clamp 到两端；
- 坐标换算本身已由 12.50 的数值验证**排除**（`gui = raw × scaled / display` 逐条吻合）。

⇒ **v78 加只读探针**（`MixinGuiTimelineClickProbe`，最多 20 条）打印
`point=(x,y) size=(WxH) offset=… zoom=… length=… -> time=…`，一轮即可定案（不再靠推测）。

#### 三、顺带修掉「调完时间轴鼠标就不听话」的根

**往回跳必须重放录像**（上游设计）：`FullReplaySender.sendPacketsTill` → **`ReplayHandler.restartedReplay()`**，
而它会 `displayGuiScreen(new ReplayHandler$1(this))` —— 把回放的**输入屏顶掉**。顶掉之后没有任何东西会把它挂回来
（唯一自动调用者是输入屏自己，**坑 59**）⇒ "跳完就卡"。

**修法（v78）**：把鼠标状态看护**搬到每帧必然执行的 `Minecraft.runTick`（`func_71407_l`）**——
不再寄生在 `updateUserInputGui` 上；输入屏不在时**自己挂回去**（每秒最多 1 次；连续 3 次仍不行则**放锁**，
绝不反复抢鼠标，避免坑 48 的 `unPressAllKeys` 抖动）。

#### 四、交付 **v78**（**未部署**）

| 项 | 值 |
|---|---|
| 文件 | **`<workspace>\replaymod-1.7.10-2.5.2-gtnh-v78-timelineprobe.jar`** |
| 大小 / SHA256 | `10858505` B / `2CA3BC6E227854BD7A3916896794DA4C0601FEEE61DBC0AEDC7DCE4022BDD546` |
| 构成 | v48 基座 + **23 个功能 mixin** + 4 util（新增 `MixinGuiTimelineClickProbe`；`ReplayMouseState` 加 `tick()`；`MixinMinecraftShiftEscReset` 加 `func_71407_l` 注入） |
| 编译 | **23 mixin + 4 util 逐个 `javac` 退出码全 0**（第一次编译失败：把 `Method` 误写成 `Field`；**坑 47 的纪律生效 —— 没有注入任何东西**） |
| 待测包 | `release\GTNH-ReplayMod-v78-待测\` + `.zip`（`9757769` B） |
| 部署 | ❌ 未部署；`mods\` 仍是 **v65**（已复核），无 java 进程 |

**判据**：`timeline probe: … offset=… zoom=… -> time=…`（点左/中/右各一次）；
若 `offset != 0` ⇒ 视口未复位（据此修 `ensureCursorVisible`/点击换算）；
若 `size` 异常 ⇒ 布局不同步；若两者正常 ⇒ 换方向（查 `doJump` 与重放路径）。
另：`replay mouse: mounting the overlay input screen myself …` = 每帧兜底生效。

#### ★ 新增坑 61

**61. ★★ 回放里"往回跳"必然触发**重放**，而重放会顶掉 overlay 的输入屏。**
`FullReplaySender.sendPacketsTill` → `ReplayHandler.restartedReplay()` 里有
`mc.func_71364_i()`（setIngameNotInFocus）+ `mc.displayGuiScreen(new ReplayHandler$1(this))`，
于是 `mc.currentScreen` 变成了那个临时屏幕 ⇒ 回放输入屏丢失 ⇒ 鼠标抓不回。
**任何"跳转/重放"路径之后都必须重新挂载输入屏**；而"挂载"这件事**不能寄生在输入屏自己身上**（坑 59）
⇒ 必须由每帧驱动（v78 的 `ReplayMouseState.tick()` 就是干这个的）。

---

### 12.53 大时间轴结案：**陈旧的 `offset`** —— 由测试者那句「重跑一遍就回去了」定案，交付 **v79**（2026-10-11 深夜）

> **来源**：洛洛转来的Tester_A回复（附 `GTNH-ReplayMod-v76-待测.zip`）：
> 「**这一版可以用了其实**」「大时间轴调不了问题不大（）」「**时间轴重跑一遍就回去了**」。
> ⇒ ① **v76 达成主要目标**（12.50 的 `setIngameFocus` 抑制方向正确）；② 第三句是**决定性线索**。

#### 一、为什么第三句就够定案

"重载录像"**不可能改变坐标换算**（12.50 已用 24 组数字逐条验算 `gui = raw × scaled / display` 全部吻合）
⇒ 只能是**会话状态**被重置。时间轴的会话状态只有 `offset` 与 `zoom`（`size` 是渲染产物），而：

```java
// AbstractGuiTimeline.getTimeAt
double rel = (length * zoom) * (x - 4) / (size.width - 8);
return max(0, min(length, round(offset + rel)));      // ★ offset 一大，点哪都落在后面
```

```java
// AbstractGuiTimeline.checkOffset —— 唯一把它夹回合法范围的地方
int visible = (int)(length * zoom);
if (visible + offset > length) offset = length - visible;
```

**而 `checkOffset()` 只被 `setOffset()` / `setZoom()` 调用**（`javap -c` 核实）——
录像长度变化（切换 / 重载 / 重编码）之后**没有任何人再夹一次** ⇒ `offset` 可以一直停在很大的值上，
直到重载录像把它一并重置 ⇒ **"重跑一遍就回去了"** ✔
（另注：`getTimeAt` 开头检查的是 `getLastSize()`，而越界判断与除法用的是字段 `size` —— 两者不同步时也会异常，
这一点留给 v78 的只读探针去证实。）

#### 二、修复：`MixinGuiTimelineViewportGuard`（v79）

在 `getTimeAt` 的 **HEAD**：**`zoom >= 1` 且 `offset != 0` ⇒ 把 `offset` 归零**（最多记 5 条日志）。

**安全性论证（这是动手的前提）**：`zoom == 1` 时 `visible == length`，`checkOffset()` 自己算出的合法值就是
`length - visible == 0` ⇒ **"`zoom >= 1` 且 `offset != 0`"本身就违反它自己的不变式**。
守卫**不写** `size`、**不写** `zoom` ⇒ 正常时间轴逐位不变。
（**不做** `size` 的兜底：`size` 与 `lastSize` 是否应当相同无法离线判定，误改会伤到正常路径。）

#### 三、交付 **v79**（**未部署**）

| 项 | 值 |
|---|---|
| 文件 | **`<workspace>\replaymod-1.7.10-2.5.2-gtnh-v79-timelinefix.jar`** |
| 大小 / SHA256 | `10860504` B / `A62020D31162AA0FDBF6FA23270C024072135CE93CC8C280D379B1F16ED6D5BE` |
| 构成 | v48 基座 + **24 个功能 mixin** + 4 util（新增 `MixinGuiTimelineViewportGuard`；v78 的只读探针保留） |
| 编译 | 24 mixin + 4 util 逐个 `javac` **退出码全 0** |
| 待测包 | `release\GTNH-ReplayMod-v79-待测\` + `.zip`（`9759656` B） |
| 部署 | ❌ 未部署；`mods\` 仍是 **v65**（已复核），无 java 进程 |

**判据**：日志出现 `timeline: stale viewport offset N ms with zoom=… - reset to 0` ⇒ 命中；
点大时间轴的左/中/右应该"点哪跳哪"。

#### ★ 新增坑 62

**62. ★★ "只夹上界、且只在 setter 里夹"的视口状态，在它依赖的量（这里是录像 `length`）变化时会静默越界。**
`checkOffset()` 里只有 `if (visible + offset > length)`（没有下界），而且只由 `setOffset`/`setZoom` 触发；
`length` 一变，旧 `offset` 就成了非法值，但没有任何路径去纠正它 ⇒ 表现为"点哪都跳到后面"。
**通用推论**：凡是"派生状态 + 只在 setter 里校正"的写法，都要问一句"它的输入量变了谁来补一次"；
而**用户可用的临时解法（这里是"重载录像"）往往就是根因的形状** —— 它能重置什么，问题就在什么上。

---

### 12.54 「单人也滚木了」—— 先定性再动手：加实体普查探针，交付 **v80**（2026-10-11 深夜）

> **来源**：`fml-client-latest (7).log`（`24066451` B / 173217 行 / 23:24–23:29 / 装的是 **v77**）
> + Tester_A：「**单人也滚木了**」（滚木 = 人物消失；此前他在 v76 时说的是"单人存档没出问题"）。

#### 一、这份日志的读数（有喜有疑）

| 判据 | v76 | **v77** | 说明 |
|---|---|---|---|
| `locked, but no input screen is mounted` | 85 | **15** | ★ **12.50 的 `setIngameFocus` 抑制大幅奏效**（189→94→85→15） |
| FATAL / 注入失败 | 0 | **0** | 无崩溃 |
| `holding S0CPacketSpawnPlayer` | 1 | **0** | 这次 SpawnPlayer 没走 12.27 的 defer 路径（`re-queued` 另有 1 次，来自别的延迟包） |
| `wheel now also …` | 0 | **0** | v77 把读取点挪到 `super` 之前**仍未读到** ⇒ 下一步要看 v77 新加的 `wheel camera speed: … could not be applied - <原因>` |

#### 二、「滚木」为什么必须先用数据定性

12.37 早就列过两种完全不同的可能，而现有日志分不开：

| | 现象 | 老账 |
|---|---|---|
| **A** | **实体真的不在世界里**（合成 SpawnPlayer 被丢/延迟没重放） | 12.13 / 12.27 |
| **B** | **实体在，只是没渲染 / 不在相机前**（相机贴在身上、机位在天上、或渲染层） | 12.37 |

**v80**：每 5 秒一行只读普查（最多 20 行）

```
[replaycompat] replay entity census: players=N entities=M renderView=… currentScreen=…
```

判据：`players=0` ⇒ **A**（查 defer/requeue 路径）；`players>=1` 而画面里没人 ⇒ **B**（要 **F3 截图**：
`E:` 实体数 + XYZ + 朝向）。同时还要问清"是切维度才出现 / 移动相机就好 / 一直都没有"——
这三种在 12.27 / 12.37 里分别是不同的病。

#### 三、交付 **v80**（**未部署**）

| 项 | 值 |
|---|---|
| 文件 | **`<workspace>\replaymod-1.7.10-2.5.2-gtnh-v80-entitycensus.jar`** |
| 大小 / SHA256 | `10861300` B / `AAFF7AC976C08205FE8C700751BD9C9EA0DA29CED20CA25728A683DB617CE785` |
| 构成 | v48 基座 + 24 个功能 mixin + 4 util（`ReplayMouseState.census()`，由 `runTick` 注入低频调用） |
| 编译 | 24 mixin + 4 util 逐个 `javac` **退出码全 0** |
| 待测包 | `release\GTNH-ReplayMod-v80-待测\` + `.zip`（`9760330` B） |
| 部署 | ❌ 未部署；`mods\` 仍是 **v65**（已复核） |

---

### 12.55 滚轮终于有解：**API 用错了**（`getDWheel` vs `getEventDWheel`）+ 卡鼠标结案 —— 交付 **v81**（2026-10-11 深夜）

> **来源**：`fml-client-latest (8).log`（`82659677` B / 696046 行 / 23:20–23:30 / 装的是 **v77**）
> + Tester_B：「**卡鼠标已经没发生了**，但是滚轮依旧是滚木（= 不行）」。

#### 一、✅ 卡鼠标结案

测试者明确"没发生了"；数字也一致：`locked, but no input screen is mounted` **189 → 94 → 85 → 15**。
⇒ 12.50 的 `setIngameFocus` 抑制 + 12.52 的每帧兜底是正确组合。

#### 二、滚轮的最终答案：`getEventDWheel()` 在这套环境**天然读不到**

v77 那份日志里，**连 v77 新加的 `wheel camera speed: … could not be applied` 都没有** ⇒ 说明代码在
"wheel == 0"处静默返回了。翻 lwjgl3ify 的字节码：

```java
public static int getEventDWheel() { return wheelEvents[queue.getCurrentPos()]; }   // 只在"当前事件是滚轮"时非 0
public static int getDWheel()      { int d = dwheel; dwheel = 0; return d; }        // 累积 + 消费
// dwheel 由滚轮事件累加： addMoveEvent 里  dwheel += (wheel - lastWheel)；只有 <clinit> 清零过
```

**鼠标移动事件也走同一个队列**，`getCurrentPos()` 绝大多数时候落在移动事件上 ⇒ `getEventDWheel()` **恒为 0**——
这就是为什么把它从 TAIL 挪到 HEAD（v77）依然无效。
而 `getDWheel()` 与"当前是哪条事件"无关 ⇒ **任何时机读都是真值**。

**修法（v81）**：`LwjglMouseBridge` 新增 `getDWheel()`，滚轮补丁改用它；日志同时打出原始值
（`… from raw dwheel=X`）以便下一轮核对量纲（预期 ±1/格）。

#### 三、交付 **v81**（**未部署**）

| 项 | 值 |
|---|---|
| 文件 | **`<workspace>\replaymod-1.7.10-2.5.2-gtnh-v81-dwheel.jar`** |
| 大小 / SHA256 | `10861410` B / `38924FAE3F87358B354158BB3480656594DDAE73671F3908872191325ECEAD11` |
| 构成 | v48 基座 + 24 mixin + 4 util（含 v78 时间轴探针、v79 视口自愈、v80 实体普查） |
| 编译 | 24 mixin + 4 util 逐个 `javac` **退出码全 0** |
| 待测包 | `release\GTNH-ReplayMod-v81-待测\` + `.zip`（`9760357` B） |
| 部署 | ❌ 未部署；`mods\` 仍是 **v65**（已复核） |

**判据**：`replay mouse: wheel now also adjusts the camera speed … (N step(s) applied from raw dwheel=X)`。

#### ★ 新增坑 63

**63. ★★ LWJGL2 的鼠标 API 有两套轮询语义，lwjgl3ify 下只有"累积消费式"那套可用。**
`Mouse.getEventDWheel()`（以及 `getEventX/Y`）读的是 `wheelEvents[queue.getCurrentPos()]` ——
**只在"当前正在处理的那条事件"恰是滚轮时才非 0**；鼠标移动事件同样排队，所以正常操作下它**永远读 0**。
要读滚轮必须用 `Mouse.getDWheel()`（`dwheel` 由滚轮事件累加、读取即清零），它与"当前事件"无关。
**推论**：凡是要在"事件循环之外"读鼠标状态的代码，都要先确认那个 getter 是"当前事件视图"还是"累积视图"；
本工程为此在 v70→v81 之间白试了四次（反射读错类 → TAIL → HEAD → 才换对 API）。

---

### 12.56 ReplayMod 快捷键「基本全是摆设」= 输入屏常驻的副作用 —— 交付 **v82**（2026-10-11 深夜）

> **来源**：Tester_A（截图）：「然后就是，**快捷键能不能修复一下**」「**现在基本全是摆设**」
> 「其他的都还好，**标记事件这个是要的**」「一段录像基本很长，**没有标记的话不好找关键时间**」。

#### 一、根因：`KeyBinding` 在锁定鼠标期间**被冻结**

1.7.10 `Minecraft.runTick` 里，键盘事件只在

```java
currentScreen == null || currentScreen.allowUserInput
```

成立时才被喂进 `KeyBinding`（`setKeyBindState` / `onTick`）。而 **ReplayMod 的快捷键全部基于 `KeyBinding`**
（Forge `InputEvent.KeyInputEvent` → 读绑定状态）。我们的 Ctrl 鼠标锁让回放**输入屏常驻**
（`currentScreen` 恒为 `UserInputGuiScreen`，其 `allowUserInput = false`）⇒ **此后 `KeyBinding` 再没被更新过**
⇒ `P`（播放/暂停）/ `M`（加标记）/ `Z` / `N` / `B` **统统失效**。

> ★ 这同时**修正 12.23 的一条旧结论**：当时实测"`P` 是好的"，是因为**那时还没有鼠标锁**；
> 有了常驻输入屏之后，这一整类快捷键就都死了。**"输入屏常驻"是有代价的**（见坑 65）。

#### 二、修复（v82）：用 `InputEvents` **旁路补喂**，只补键盘

`me.eigenraven.lwjgl3ify.api.InputEvents` 的键盘监听（12.23 已确认回调在主线程）是**监听**而非消费，
所以聊天/ESC 等 GUI 键盘输入**完全不受影响**。转发规则：

| 状态 | 行为 |
|---|---|
| **锁着鼠标**（玩家按过 Ctrl） | 我们补喂：`PRESSED → setKeyBindState(true) + onTick(key)`（`onTick` 是 `isPressed()` 所需 `pressTime` 的来源）；`RELEASED → setKeyBindState(false)` |
| **没锁**（鼠标被抓） | **一个字都不做** —— 游戏自己喂，避免双重触发 |
| **Ctrl 本身** | 不转发（那是本补丁的锁定键） |

实现：`com.gtnh.replaycompat.util.ReplayKeyBridge`（用 `Proxy` 反射实现 `InputEvents$KeyboardListener`，
本工程无 reobf、该接口也不在编译 classpath），由 `ReplayMouseState.tick()` 在首帧装一次。

#### 三、交付 **v82**（**未部署**）

| 项 | 值 |
|---|---|
| 文件 | **`<workspace>\replaymod-1.7.10-2.5.2-gtnh-v82-hotkeys.jar`** |
| 大小 / SHA256 | `10864854` B / `3C44050058C89CAAA64191C476E9E30CD6237DCD739A82CA49087695757BAFB0` |
| 构成 | v48 基座 + 24 mixin + **5 util**（新增 `ReplayKeyBridge`）→ jar 内 **31 个 class**（+2 个内部类），已逐个核对 |
| 待测包 | `release\GTNH-ReplayMod-v82-待测\` + `.zip`（`9763490` B） |
| 部署 | ❌ 未部署；`mods\` 仍是 **v65**（已复核） |

**判据**：日志一次性出现 `replay key bridge installed (ReplayMod hotkeys stay alive while the mouse is unlocked)`；
按 `M` 应在时间轴上**加出标记**。

#### ★ 新增坑 64 / 65

**64. ★★ PowerShell 里 `ForEach-Object { … if ($LASTEXITCODE -ne 0) { throw } }` 的 `throw` 不会中止脚本。**
它在管道内被吞掉，后面的注入脚本**照样执行** —— 于是"编译失败却注入了旧 class"，正是坑 47 的变体（v82 首刷命中）。
**正确写法**：用 `foreach ($x in …) { … if (...) { $fail = ... } }` 收集失败，循环结束后统一 `throw`；
**判据也要从"编译命令返回了什么"改成"产物里有没有那个新 class"**（本轮正是靠 `jar tf` 数出 31 个 class 才敢用）。

**65. ★★ 让回放输入屏「常驻」是有代价的：`KeyBinding` 会被冻结。**
只要 `currentScreen` 是一个 `allowUserInput == false` 的屏幕，1.7.10 就不再更新任何 `KeyBinding`
⇒ 所有基于绑定的快捷键（ReplayMod 的 P/M/Z/N/B…）静默失效。
**推论**：凡是"把某个屏幕长期挂着"的补丁，都必须同时考虑**键盘绑定是否还需要更新**；
本工程的正解是用 `InputEvents` 旁路补喂（只补键盘、且只在需要接管时）。

---

### 12.57 v82 固化为独立分支快照（2026-10-11 深夜，洛洛：「v82 记录并独立分支」）

**背景**：这个工作区**不是 git 仓库**（`.git` 不存在），而这条线已经推进到 v82、需要与主线 v65
**并行保留、各自可重建**。因此用"自包含的目录快照"承担分支职责。

| 分支 | 位置 | 状态 | 指纹 |
|---|---|---|---|
| **main**（正式发布版） | `release\GTNH-ReplayMod-v65\` + `mods\` 里正在跑的 | ✅ 稳定，可回滚 | `10837941` B / `907B72B8…45FCC` |
| **v82-hotkeys**（独立改进线） | **`branches\v82-hotkeys\`** | 🚧 待现场复验 | `10864854` B / `3C440500…AFB0` |

**快照构成**（45 个文件）：`src\`（41 个：24 mixin + 5 util + asm/coremod + `mixin-res\mixins.replaycompat.json`）、
`dist\`（jar + `SHA256.txt` + `FINGERPRINTS.txt`）、`本次要测什么.md`；
另有 **`branches\README.md`**：两条线的总览、**从零重建命令**（含坑 47/64 的硬校验写法）、
部署/回滚（切换分支）命令。

两条线的关系：**同一基座（v48）上的两条独立演进** —— main = v48 + 14 mixin（v65），
v82 = v48 + 24 mixin + 5 util（累积 v49~v82）；都**不含** v65 就摘掉的 HUD 补丁（12.30 的裁定）。

> **可选（等洛洛定）**：要不要把 `<workspace>` 初始化成 **git 仓库**、用真分支管理？
> 可行，但要注意：① 工作区里有 `tools\*.zip`（gradle/JDK 约 450 MB）与 `tools\replaymod-stable.tar.gz`（57 MB）、
> 上百个历史 jar（每个 ~10 MB），必须先写 `.gitignore`；② **v65 的源码已不在磁盘上**（当前 src 是 v82 线），
> 所以"从 v65 拉一条分支"只能靠 `release\GTNH-ReplayMod-v65\` 的产物，**无法还原源码** ——
> 若要做 git 分支，建议**从现在（v82）起**建立历史，v65 仅作为 tag 记录指纹。

---

### 12.58 时间轴探针的结论 + 探针本身的修正 —— 交付 **v83**（2026-10-11 深夜）

> **来源**：洛洛转的 `fml-client-latest (9).log`（`32538467` B / 248228 行 / 23:31–23:38 / 装的是 **v78**）。

#### 一、v78 的兜底**被实测证明在工作**

`mounting the overlay input screen myself` **10 次** ⇒ 12.52 那条"每帧把输入屏挂回来"的兜底**真的在救场**
（输入屏确实会反复被顶掉，而它每次都被挂回来了）。`locked, but no input screen` 24 条。

#### 二、那 20 条 `timeline probe` 逐条验算的结果（**三条结论**）

| 结论 | 证据 |
|---|---|
| **换算公式完全正确** | `1800000 × 0.1 × (96−4) ÷ (509−8) = 33054`，与打印的 `time=33054` 分毫不差；另一组 `55491 × 1.0 × 32 ÷ 482 = 3684` 同样吻合 |
| **`offset` 恒为 0** ⇒ 12.53 的"陈旧 offset"**不是这个病**（该修复无害，保留） | 20 条全部 `offset=0` |
| ★ **数据混了两种时间轴** | `zoom=0.1/length=1800000(30min)` 8 条 vs `zoom=1.0/length=55491(55s)` 12 条 —— `AbstractGuiTimeline` 同时是**回放**（`GuiMarkerTimeline`）与**路径**（`GuiKeyframeTimeline`）的父类 |

**并且探针本身设计错了**：`getTimeAt` 在**鼠标移动**（悬停/提示）时也会被调用 ⇒ 那 20 条抓的是**移动**而非点击。
⇒ **v83 把探针改挂到 `onClick(I)V`（真正的跳转入口），并打印 `this.getClass().getSimpleName()`**：

```
[replaycompat] timeline click: GuiMarkerTimeline -> jump to 33054 ms size=(509x20) offset=0 zoom=0.1 length=1800000
```

#### 三、顺带定案一条：**「往前调直接飞走」不是缺陷**

回放**往回跳必须重放录像**（`FullReplaySender.sendPacketsTill → ReplayHandler.restartedReplay`：从头重新喂包、
`displayGuiScreen(临时屏)`、重建世界）⇒ 视觉上"唰"地回去；**往后跳只需继续喂包** ⇒ 流畅。
**这是上游机制**；我们唯一的责任是"重放顶掉输入屏 ⇒ 跳完鼠标不好使"，而 **12.52 的每帧兜底已经兜住**（↑第一节 10 次）。

#### 四、交付 **v83**（**未部署**）

| 项 | 值 |
|---|---|
| 文件 | **`<workspace>\replaymod-1.7.10-2.5.2-gtnh-v83-timelineclick.jar`** |
| 大小 / SHA256 | `10864815` B / `5EE30F40CA60E1F6055D7BDA960A539DECF308BF4C8FA691A8876D9B5A9C119A` |
| 构成 | v48 基座 + 24 mixin + 5 util（含 v82 的快捷键桥）→ jar 内 **31 class**（已核对）；探针字符串已确认在字节码里 |
| 待测包 | `release\GTNH-ReplayMod-v83-待测\` + `.zip`（`9763607` B） |
| 分支快照 | `branches\v82-hotkeys\` 已同步到 v83（46 个文件） |
| 部署 | ❌ 未部署；`mods\` 仍是 **v65**（已复核） |

**判据**：点大时间轴后日志出现 `timeline click: <类名> -> jump to N ms …`；
**若一行都没有**，那就是"点击根本没到时间轴"（同样是有价值的结论）。

#### ★ 新增坑 66

**66. ★★ 给父类混入写探针时，必须把「子类实例」打出来，否则日志里会混进别的功能的时间轴。**
`AbstractGuiTimeline` 被回放 overlay（`GuiMarkerTimeline`）与 simplepathing（`GuiKeyframeTimeline`）共用，
两类实例的参数完全不同（`0.1 / 30min` vs `1.0 / 55s`），混在一张日志里会**看起来像同一个东西在乱跳**。
**推论**：探针的选点也要挑"语义唯一"的方法 —— `getTimeAt` 会被悬停调用（抓到的是鼠标移动），
`onClick(int)` 才是真正的点击。

---

### 12.59 滚轮的最终真因：**MouseTweaks 每帧把 `getDWheel()` 抢读了** + 「人消失」定性为相机视角 —— 交付 **v84**（2026-10-11 深夜）

> **来源**：洛洛转的 `fml-client-latest (10).log`（`129176020` B / 924154 行 / 23:37–23:49 / 装的是 **v81**）
> + Tester_B「滚轮依旧是滚木」、Tester_A「**往回到时间轴人也会消失**」「**让时间轴走完重新开始倒是又会回来**」。

#### 一、「人消失」**定性为相机视角，不是实体丢失**（v80 的普查探针立功）

```
players=1 entities=1  renderView=CameraEntity
players=2 entities=95 renderView=CameraEntity
players=2 entities=91 renderView=EntityOtherPlayerMP     ← 旁观某人：相机就在他眼睛上
... 20 条里 players 恒为 1 或 2，从未出现 0
```

⇒ **实体一直在世界里**（所以 `holding`/`dropping` 只有 1/0 次，12.27 那条老路没被触发）。
看不到人是**视角**问题：`renderView=EntityOtherPlayerMP` 即"旁观某个玩家" ⇒ **第一人称当然看不到他自己的模型**；
「走完重开又回来」= 重放结束后相机被重置回初始机位。**与 12.25 的解释一致**，属**上游行为**。

#### 二、滚轮折腾 v70→v84 的**最终真因**（ASM 扫实例 mods 抓到）

```
yalter/mousetweaks/Runtime#onUpdateInGui(GuiScreen)               -> org/lwjgl/input/Mouse#getDWheel()I
yalter/mousetweaks/handlers/ContainerContext#refresh(GuiScreen,Z) -> org/lwjgl/input/Mouse#getDWheel()I
```

**MouseTweaks 每帧都读 `Mouse.getDWheel()`**，而它是**消费式**（`d = dwheel; dwheel = 0; return d;`）
⇒ 我们去看时**永远是 0**。**这才是 v81 那次"改用 getDWheel"也失效的原因**。

**修法（v84）**：改用同类的

```java
public static double totalScrollAmount;   // public、单调累积、【不消费】
```

**自己记上次值、两次相减**得到本帧滚动量（谁都不抢不走）；字段拿不到时回退旧路径。

> 附带排除：`Config.INPUT_SCROLL_SPEED` 默认 **1.0**（范围 0.05~20），`addWheelEvent` 的"整数刻度门控"
> 因此不是问题；`GuiScreen.func_146274_d` 里**没有** `getEventDWheel` 调用（只有 X/Y/Button），
> 所以"在 GuiScreen 拦读取"这条备用路本来就不存在。

#### 三、交付 **v84**（**未部署**）

| 项 | 值 |
|---|---|
| 文件 | **`<workspace>\replaymod-1.7.10-2.5.2-gtnh-v84-scroll.jar`** |
| 大小 / SHA256 | `10865116` B / `5EB45D668837F342CB58AECF13384E4D3920A9D31C920A779583D63EB7ED13CA` |
| 构成 | v48 基座 + 24 mixin + 5 util → **31 class**；`javap` 确认 `totalScrollAmount` 已在字节码里 |
| 待测包 | `release\GTNH-ReplayMod-v84-待测\` + `.zip`（`9763881` B） |
| 分支快照 | `branches\v82-hotkeys\` 已同步到 v84（47 个文件） |
| 部署 | ❌ 未部署；`mods\` 仍是 **v65**（已复核） |

#### ★ 新增坑 67

**67. ★★ 用"消费式"API 之前，先在实例 mods 里扫一遍谁在抢读它。**
`Mouse.getDWheel()` 读一次即清零，而 GTNH 自带的 **MouseTweaks** 每帧（`Runtime#onUpdateInGui`）都在读它
⇒ 任何后来者拿到的都是 0。**排查手段**：拿到实例的 `mods\` 目录，用本工程的 ASM 调用点审计器
（`.selftest\MethodCallAudit.java`）按**方法名**扫 `#getDWheel` —— 本轮就是这样一击命中的。
**推论**：凡是"全局单值 + 消费式"的环境 API（鼠标滚轮、按键队列、事件队列），
跨 mod 使用时都要**优先找非消费的那一个**（或自己计差分），而不是假设自己先到。

---

### 12.60 「大时间轴调不了」**结案：点击完全正常，体验来自上游重放机制**（2026-10-11 深夜）

> **来源**：`fml-client-latest (11).log`（`22737533` B / 167942 行 / 23:48–23:52 / 装的是 **v83**）。
> v83 把探针改挂 `onClick` 之后，**第一次抓到了真正的点击**（`timeline click` 4 条）。

#### 一、四条点击数据（**逐项正常**）

```
timeline click: -> jump to 14942 ms  size=(490x20) offset=0 zoom=1.0 length=38310
timeline click: -> jump to  1669 ms  size=(490x20) offset=0 zoom=1.0 length=38310
timeline click: -> jump to 25116 ms  size=(490x20) offset=0 zoom=1.0 length=38310
timeline click: -> jump to 15499 ms  size=(490x20) offset=0 zoom=1.0 length=38310
```

| 判据 | 结果 |
|---|---|
| 时间轴换算（`getTimeAt`） | ✅ 请求时间全部落在 `length=38310` 内，与点击位置相符 |
| 视口 `offset` / `zoom` | ✅ `0` / `1.0` ⇒ **12.48 的"坐标量纲"与 12.53 的"陈旧 offset"确认都无关** |
| `onClick` 是否被触发 | ✅ 4/4 命中 |
| **点击之后发生了什么** | ★ **`Client disconnected from server, clearing skin customization flags`** —— 每点一次就**断开并重建连接 = 重放一次** |

⇒ **「大时间轴调不了」= 上游的"往回跳必须重放"机制**（`sendPacketsTill → restartedReplay`：从头重喂数据、
`displayGuiScreen(临时屏)`、重建世界）+ **重放期间相机被重置**（12.59 定性为视角问题）。**不是我们的缺陷。**

我们唯一的责任（重放顶掉 overlay 输入屏 ⇒ 跳完鼠标不好使）**已被 12.52 的每帧兜底接住**：
本条日志里 `input screen closed while locked` 紧跟点击之后，而 `mounting the overlay input screen myself` **5 次**。

#### 二、探针的小瑕疵（不影响结论）

`this.getClass().getSimpleName()` 在混入后的类上返回**空串**（日志里 `timeline click:  -> …`），
所以"哪一个时间轴"这次没打出来；但 `size=(490x20)` / `length=38310` / `zoom=1.0` 已足以确认是**回放时间轴**。

#### 三、给使用者的正解（写进说明，不再改代码）

1. 往回跳之后**等重放跑完**（画面会"唰"过去一次），再按 Ctrl 重新锁鼠标；
2. 想精确定位：**先暂停**，用时间轴定位再播放；
3. 要"拍人/看人"：别点实体，用**自由相机**（WASD）移到第三人称机位（12.25）；
4. 长录像找关键时间：用 **M 加标记**（v82 已把快捷键救回来）—— 这正是Tester_A要的功能。

#### 四、本轮日志其它读数

`replay key bridge installed` ✅ · `entity tracker … skipped` ×5 ✅（12.26 在真环境继续工作）·
`entity census` 13 条 · `mounting …` 5 次 · `holding`/`re-queued` 1/1 · **零 FATAL、零注入失败**。

---

### 12.61 v84「判据行一行都没有」定案（观测盲区）+ **标记处理器崩溃**的真身 —— 交付 **v85**（2026-10-13）

> **来源**：Tester_A（338 mods / PrismLauncher `[官包]GTNH2.9.xbeta3` / `<home>`）两份新日志 ——
> `fml-client-latest (12).log`（`27662774` B / 206825 行 / 00:02–00:05 / **v84**：回放里 Ctrl 切换 + 点时间轴，**无滚轮痕迹**）
> 与 `fml-client-latest (13).log`（`7825675` B / 48551 行 / 16:07–16:10 / **v84**：编辑器里「应用标记」⇒ **崩溃报告**）。

#### 一、v84 的滚轮判据行为什么一行都没有（**观测盲区**，不能推出"没滚"）

`MixinUserInputGuiScreenWheelCameraSpeed` 挂在 `AbstractGuiOverlay$UserInputGuiScreen#func_146274_d`
（= `GuiScreen#handleMouseInput`），而**这个方法不保证被调用**：

- 字节码证据（`recompiled_minecraft` + `MethodCallAudit`）：`Minecraft#runTick → GuiScreen#handleMouseInput`
  确实存在，**但**回放中驱动输入的是 `InputReplayTimer#updateTimer`，它输入屏分支调的是
  `GuiScreen#func_146269_k`（`handleInput`）；
- 而 `GuiScreen#handleInput` 只在 `while (Mouse.next())` **还有事件**时才转发给 `handleMouseInput` ——
  任何更早的队列消费者（本包自带 MouseTweaks：`Runtime#onUpdateInGui` + `ContainerContext#refresh`，坑 67）
  都能让这个钩子**一次都不执行**；
- v84 的注入体在 `wheel == 0` 时直接 `return`：**"钩子没被调用"与"滚轮读成 0"在日志里完全同形**。
  上一轮把"有没有判据行"当判据，是观测设计上的失误（坑 68）。

上游那条路也不是我们早先以为的"被输入屏抢走"。`com.replaymod.core.mixin.MixinMinecraft#scroll()`
是 `@Redirect(method="runTick", target="Lorg/lwjgl/input/Mouse;getEventDWheel()I", remap=false)`，
方法体**无任何条件**：

```
0: invokestatic Mouse.getEventDWheel()I
3: istore_1
4: iload_1
5: invokestatic InputReplayTimer.handleScroll(I)V
8: iload_1
9: ireturn
```

`InputReplayTimer#handleScroll(int)` 自带两级判空（`getReplayHandler()` / `getCameraEntity()` 为 null 即
`return`）⇒ **复用它是安全的**，步长与速度范围也与上游一致（优于 v70~v84 自己调
`increase/decreaseSpeed`）。另注：`CameraController` 接口**只有** `update/increase/decreaseSpeed`，
**没有 getSpeed** ⇒ 无法用"读速度"做去重。

#### 二、修法（v85，`25 mixin + 5 util = 32 class`）

| # | 改动 | 位置 |
|---|---|---|
| 1 | 滚轮处理搬进**每帧 `Minecraft.runTick`**（与 12.52 同一个教训：兜底不能寄生在可能不被调用的方法上） | `ReplayMouseState#wheelTick`，由 `tick()` 调用 |
| 2 | 读数仍用**不消费**的 `org.lwjglx.input.Mouse.totalScrollAmount` 差分 + **小数累积**（高精度滚轮/触摸板一格 < 1） | 同上 |
| 3 | 应用直接调**上游 `InputReplayTimer.handleScroll(int)`** | 同上（反射，失败打印原因） |
| 4 | 旧注入点**降级为一次性探针**：`wheel probe: UserInputGuiScreen.func_146274_d was reached` —— 留着它就能永久回答"这个方法到底会不会被调" | `MixinUserInputGuiScreenWheelCameraSpeed` |
| 5 | 新增**无条件探针**：`wheel probe: per-tick wheel hook is alive (totalScrollAmount=…, handleScroll=…)` | 同上 |

> `totalScrollAmount` 的类型已在 **lwjgl3ify 3.0.31** 字节码里核实：`public static double`，且
> `addWheelEvent(double)` 在**整数刻度门控之前**无条件 `dadd`/`putstatic` ⇒ 只要 SDL 送来滚轮就一定会变
> （所以"读不到"只能来自"钩子没跑"，不可能来自"字段不存在/类型不对"）。

#### 三、★ 标记处理器崩溃（本节真正的收获）

Tester_A 10-12 16:09 的崩溃报告 `Description: Running marker processor`：

```
java.lang.IndexOutOfBoundsException: readerIndex(18) + length(44957696) exceeds writerIndex(132)
    at ...ByteBufNetInput.readBytes(ByteBufNetInput.java:106)
    at ...PacketChunkData.readLoad(PacketChunkData.java:258)
    at ...PacketChunkData.read(PacketChunkData.java:58)
    at ...SquashFilter.onPacket(SquashFilter.java:362)
    at ...MarkerProcessor.apply(MarkerProcessor.java:212)
    at ...GuiEditReplay.lambda$apply$10(GuiEditReplay.java:158)
```

**同一份日志里 v73 的保护成功生效 5 次**（`skipped an unreadable chunk data packet`，全是
`readerIndex(8)`），第 6 个坏包却走了**另一条入口** —— `SquashFilter#onPacket` 有**三条**读取路径：

```
offset  964: invokestatic PacketChunkData.read(Packet)     -> PacketChunkData    ← v73 漏了这条
offset 1015: invokestatic PacketChunkData.readBulk(Packet) -> List<Column>       ← v73 已护
             invokestatic PacketBlockChange.readBulk(...)   -> List<BlockChange>  ← v73 已护
```

**v85 修法（两层）**：

1. `MixinSquashFilterSafeBulkRead` 补第三条 redirect，失败时返回 **`PacketChunkData.unload(0, 0)`**
   （上游自己的工厂）而**不是 null** —— `onPacket` 紧接着就调
   `isUnload()/getUnloadX()/getUnloadZ()`，null 只会把崩溃换成 NPE；`unload(0,0)` 退化成
   "卸载一个从未加载的区块"，语义安全；
2. 新增 `MixinMarkerProcessorSafeFiltering`：redirect `MarkerProcessor#apply` 里**两处**
   `SquashFilter#onPacket(PacketStream, PacketData)Z` 调用（`invokevirtual`，返回值随后被 `pop`），
   任何异常只丢这一个包 ⇒ 从根上堵住"编辑器读到坏录像就崩游戏"这一整类问题
   （比逐个补读取点耐用：上游再加读取路径也不会漏）。

**交付 v85**：`replaymod-1.7.10-2.5.2-gtnh-v85-editorfix.jar`（`10865772` B /
`764A29DD7E48AE14583B7A032EE560C5764ABD39E2F973649DFE4A7300119BB8`），发布包
`release\GTNH-ReplayMod-v85-待测\` + `.zip`（`9765477` B，含 `本次要测什么.md`）；
`mods\` 仍为 **v65**（已复核 `907B72B8…45FCC`，未部署）。

#### ★ 新增坑 68

**68. ★★ 不要把"日志里没有判据行"当作"功能没触发"的证据，除非那条判据所在的注入点保证被调用。**
v84 在 `wheel == 0` 时静默 `return`，于是"钩子没被调用"和"滚轮读成 0"**同形**，白折腾一轮。
**推论**：① 每个关键注入点在**第一次执行**时都打一条**无条件**"hook alive"行；
② 凡是依赖"事件队列 / 输入泵"的方法（`handleInput`、`handleMouseInput`、`Mouse.next()` 系），
回放里都可能被上游整条换掉调用链 ⇒ **兜底只挂 `runTick`**（与坑 59 同源）。

---

### 12.62 v85 **现场双验证成功**（滚轮 + 标记处理器崩溃）+ 滚轮灵敏度根因 + 「只剩天空」的硬证据 —— 交付 **v86**（2026-10-13 下午）

> **来源**：Tester_B（自己的实例 `D:/MC/minecraft/.minecraft`）`fml-client-latest (14).log`
> （`121580815` B / **1004439** 行 / 12:15–12:30 / 装的是 **v85**）+ 他在群里给的两条反馈
> （「滚轮的灵敏度有点低了 / 有点太低了」、「我在游戏内开始录制就会发生这种加载不出来的情况」+ 回放截图）。

#### 一、v85 的两项修复**都拿到了现场证据**

```
[12:16:47] wheel probe: per-tick wheel hook is alive (totalScrollAmount=available, handleScroll=available)
[12:18:14] wheel: adjusted the camera speed by -1 step(s) from the wheel (event #1)
[12:18:15] wheel: adjusted the camera speed by  1 step(s) from the wheel (event #2)
...                                                              (共 10 条，步长 -1/+1/-3)
[12:21:09] wheel probe: UserInputGuiScreen.func_146274_d was reached
[12:20:32] replay editing: skipped a packet the marker processor could not read instead of crashing the game (...)
```

| 项 | 结论 |
|---|---|
| 每帧钩子 | ✅ 活着，`totalScrollAmount` 与 `handleScroll` **都 available**（12.61 的两条类推全部落实） |
| 滚轮调速 | ✅ **10 次真实调速** —— v70 起折腾五版的功能终于生效 |
| 旧注入点 `func_146274_d` | ⚠️ 整场**只被调到一次**（12:21:09）。这正面解释了 v84 的判据行为什么一行都没有（**它确实经常不被调用**），也证明 12.61 换入口是对的（坑 68） |
| **标记处理器崩溃** | ✅ **修好了**：同一批坏包（v85 之前 = 崩溃报告）现在只打 4 条 skip 日志，游戏跑到 12:30，**零崩溃报告、零 ERROR、零未处理异常** |

#### 二、滚轮灵敏度：Tester_B说「太低」的根因

```
SDL3/lwjgl3ify 每次滚轮事件 -> addWheelEvent(delta = 1.0) -> totalScrollAmount += 1.0
我们的差分 = 1 格 = 1 步 -> InputReplayTimer.handleScroll(1) -> increaseSpeed() 一次
```

`increaseSpeed()` **一次只调一档**（约 ×1.1）⇒ 滚一格只涨 10%，要滚二十几格才翻倍，手感自然像"没动"。
（上游在 LWJGL2 时代 `getEventDWheel()` 是**每格 ±120**，所以原版手感是"一滚一大截"。）

**v86 修法**：`WHEEL_STEPS_PER_NOTCH = 5`（每格 5 步），并提供 `-Dreplaycompat.wheelSteps=N`（1~120）
免重建调参；探针行加打 `stepsPerNotch=5`。`WHEEL_MAX_STEPS` 由 20 提到 **120**（= 上游一格的量级）。

#### 三、★「只剩天空」第一次拿到**量化的硬证据**

Tester_B的日志里，我们的实体普查探针连续 20 条几乎都是：

```
replay entity census: players=2 entities=2       ← 世界里有且仅有两个玩家实体
```

对照Tester_A正常回放的同一探针：`entities=95 / 151 / 316`（12.59）。
`entities=2` ⇒ **区块根本没加载**（没有方块 ⇒ 没有方块实体/生物），所以画面只剩天空和云；
而**整份 100 万行日志零 ERROR、零崩溃报告** ⇒ 它是**静默**发生的。

⇒ 「只剩天空」= **chunk 数据缺失/未被应用**，与"渲染没画出来"无关（12.39 的老猜测可以划掉）。
下一步判定"录制时没录进 chunk 包"还是"回放时没应用"，**必须要录像本体**：
① `.mcpr` 原文件（解析包类型，看有无 `S21PacketChunkData` / `S26PacketMapChunkBulk`）；
② 录制当下他能否看到地形；③ `.mcpr` 文件大小。

> 顺手排除：本工程的 `NullWorldGuard` defer/requeue **只处理 7 种 stateful 包**（S03/S05/S07/S0F/S33/S34/S35），
> **完全不碰 chunk 包**；而且 (14) 日志里**没有** `re-queued N deferred packet(s)` ⇒ 这条路这次根本没被触发，
> 不可能是它导致地形缺失。

**交付 v86**：`replaymod-1.7.10-2.5.2-gtnh-v86-wheelspeed.jar`（`10865980` B /
`D5163D48C14D7D007ECE13EB6BA2CF189356B7C3D2DD22EFC59371581A063FE8`，仍 **32 class**），
发布包 `release\GTNH-ReplayMod-v86-待测\` + `.zip`；`mods\` 仍为 **v65**（未部署）。

---

### 12.63 「只剩天空」根因锁定：**录像里没有世界初始区块** —— mcpr 包流分析（2026-10-13 下午）

> **材料**：Tester_B两份**原始 `.mcpr`**（`2026_10_13_12_48_17.mcpr` =「世界内录制」`430260` B / 27.01 s；
> `2026_10_13_12_49_38.mcpr` =「自动录制」`5700776` B / 34.04 s）+ `fml-client-latest (15).log`
> （`30.65` MB / 257067 行 / 12:46–12:51 / 装的是 **v86**）。

#### 一、`.mcpr` 其实就是 **ZIP**（可直接解包，不必写 Java）

```
2026_10_13_12_48_17.mcpr
  metaData.json           299 B   ← 纯 JSON（不是 protobuf）
  mods.json             14001 B
  recording.tmcpr     1476634 B   ← 真正的包流
  recording.tmcpr.crc32    10 B   ← crc32 的十进制 ASCII（如 "2902572109"）
  markers.json              2 B
metaData: {"singleplayer":true,"duration":27010,...,"generator":"ReplayMod v1.7.10-2.5.2",...}
```

`recording.tmcpr` 的记录格式（本轮反推并验证）—— 全部大端：

```
重复 { int32 时间增量(ms) ; int32 payload 长度 ; byte 包 id ; payload[len-1] }
```

首包 `00 00 00 00 | 00 00 00 2d | 02 | 24 <36 字节 UUID> | 06 "Player"` ⇒ len = 1+1+36+1+6 = **45** ✓。

#### 二、★ 包类型直方图（**决定性证据**）

| | 世界内手动录制（**只剩天空**） | 自动录制（**正常**） |
|---|---|---|
| 时长 | 27.01 s | 34.04 s |
| `recording.tmcpr` | 1 476 634 B | 7 017 187 B |
| 总包数 | 11 009 | 17 030 |
| **`CHUNK_DATA` (0x21)** | **125** | **1 449** |
| **`MAP_CHUNK_BULK` (0x26)** | **25** | **373** |
| **chunk 合计** | **150**（5.6/s） | **1 822**（53.6/s） |
| `CustomPayload` (0x3f) | 6 991 | 7 364 |
| `SpawnPlayer` (0x0c) | 70 | 82 |
| `AttachEntity` (0x19) | 203 | 1 515 |

⇒ **正常录像的区块包是"只剩天空"那份的 12 倍**。坏的那份只录到了后续零星的区块更新，
**没有"进入世界时服务端一次性推送的全量区块"** ⇒ 回放时世界是空的 ⇒ **画面只剩天空**，
与 12.62 从 `entity census` 看到的 `players=2 entities=2` 完全吻合。

#### 三、录制器自己的 NPE（同源线索，但不是 chunk 缺失的主因）

```
[12:49:38] [com.replaymod.recording.packet.PacketListener/]: Writing packet:
java.lang.NullPointerException: Cannot invoke "String.getBytes(java.nio.charset.Charset)"
        because "<parameter1>" is null
    at net.minecraft.network.PacketBuffer.func_150785_a(SourceFile:141)          (writeString)
    at net.minecraft.network.play.server.S0CPacketSpawnPlayer.func_148840_b(SourceFile:81)
    at com.replaymod.recording.packet.PacketListener.getPacketData(PacketListener.java:465)
    at com.replaymod.recording.packet.PacketListener.save(PacketListener.java:201)
    at com.replaymod.recording.handler.RecordingEventHandler.spawnRecordingPlayer(RecordingEventHandler.java:111)
    at net.minecraft.client.network.NetHandlerPlayClient.handler$zbp000$replaymod$recordOwnJoin
    at net.minecraft.network.play.server.S01PacketJoinGame.func_148833_a            ← 进世界
```

`spawnRecordingPlayer` 是 ReplayMod「在录像里补一个自己的 spawn 包」的机制（客户端收不到自己的
`S0CPacketSpawnPlayer`）。它构造的那个包在**这个环境**里有一个字符串字段是 `null` ——
**1.7.10 原版 `S0CPacketSpawnPlayer.writePacketData` 并不写字符串**，所以这个类被
**skinport / CustomSkinLoader 一类改写过**（本包两者都在），假包没填那个字段 ⇒ 写包 NPE。

⚠️ 它对「自动录制」那份**同样发生**，而那份是**正常**的 ⇒ **不是 chunk 缺失的原因**；
但它会让录像里缺"玩家自己的 spawn 条目"，**Tester_B报的「私人维度里渲染不出玩家」值得从这个方向复查**。

#### 四、结论与下一步

1. **「只剩天空」= 录像里没有世界初始区块** ⇒ 属**录制时机**问题：中途开始录制时，
   客户端已经持有的区块不会经由网络重发，录制器自然录不到；
2. 保存时那 5 个"坏 chunk 包"（v73/v85 容错接住、保存因此没崩）是**次要**因素 ——
   丢 5 个包不足以造成整片天空缺地形；
3. **决定性实验**（彻底分清"录制漏"与"保存丢"）：让Tester_B关掉 ReplayMod 的
   **`Setting.AUTO_POST_PROCESS`（自动后处理）**，分别录一份"进世界就开始"和一份"中途开始"，
   把两份**原始 `.mcpr`** 交回 ⇒ 若中途那份 `CHUNK_DATA` 仍只有百来个，即可定案为录制漏包。
   据此二选一：**(a)** 在适配层"开始录制"时**强制重发区块**（单人可走 `IntegratedServer`，
   多人需服务端配合 —— 与 `ServerReplay` 的"实体包漏录"是同一族问题，§12.31）；
   **(b)** 在说明里写清**录制请从"进入世界"那一刻开始**（上游本来就有的使用约束）。

---

### 12.65 录制器在连接切换后往已关闭的线程池塞包（1343 条刷屏）—— 交付 **v88**（2026-10-13 下午）

> **来源**：洛洛本机日志（v86，13:07–13:22）：`RejectedExecutionException` **1343 次**。

```
[com.replaymod.recording.packet.PacketListener/]: Writing packet:
java.util.concurrent.RejectedExecutionException: Task ... rejected from
    java.util.concurrent.ThreadPoolExecutor@...[Terminated, pool size = 0, active threads = 0, ...]
    at PacketListener.save(PacketListener.java:202)
    at PacketListener.lambda$save$1(PacketListener.java:167)
    at Minecraft.func_71407_l(Minecraft.java:1576)
```

**成因**：连接关闭时 `PacketListener.channelInactive` 已经保存完录像并 shutdown 了 `saveService`，
但**旧的 recorder 仍被持续喂包**（典型场景：录着像就直接去连服务器）。提交永远不可能执行，只产生日志。

**修法（v88）**：新增 `MixinPacketListenerSaveGuard` —— `save` 的 HEAD 处（`@Inject` 只收 `CallbackInfo`，
不引用任何 MC 类型，坑 29）反射读 `saveService`（`ReplayMouseState#packetSaverIsShutDown`），
一旦 `isShutdown()` 就 `ci.cancel()`，并只打**一条**说明日志。

---

### 12.66 ★「进服务器就断连」的真身：**Salis Arcana 在握手期打开焦点操纵器 GUI**（与录制无关）—— 交付 **v89**

> **来源**：洛洛截图（`A fatal error has occurred during the network handshake`）+ 本机日志
> （v86，13:13:25–13:13:33）。

#### 一、完整栈（两次抛出**完全相同**，FML 只是在两个 handler 里各记一次）

```
GuiConnecting.func_73876_c(SourceFile:78)                 ← 玩家还在"连接中"界面
  → NetworkManager.func_74428_b → FMLProxyPacket.func_148833_a
    → OpenGuiHandler.channelRead0
      → OpenGuiHandler.redirect$clc000$hodgepodge$openGui(:1039)     (hodgepodge 的 redirect)
        → NetworkRegistry.getLocalGuiContainer(:265)
          → thaumcraft.client.ClientProxy.getClientGuiElement(:383)
            → new GuiFocalManipulator(:42)
              → new ContainerFocalManipulator(:36)
                → ContainerFocalManipulator.handler$eno000$salisarcana$attachPlayer(:526)
                  → tileEntity.salisArcana$connectPlayer(player)
java.lang.NullPointerException: ... because "tileEntity" is null
```

FML 的两次记录分别是 `[FML/]: OpenGuiHandler exception` 与
`[FML/]: HandshakeCompletionHandler exception` —— **栈一模一样**，后者才让它变成"握手致命错误"⇒ 断连
（新增**坑 70**）。

#### 二、根因与责任

`ContainerFocalManipulator` 的构造在**第 36 行**就调用了 Salis Arcana 注入的 `attachPlayer`，
而 `tileEntity` 要到构造函数后面才赋值 ⇒ **Salis Arcana 1.1.71 的判空缺陷**。
只要服务端在客户端**仍在连接/加载时**发来「打开神秘焦点操纵器」的包就会踩到。

- ⚠️ **与本工程无关**：全量日志确认我们的 `defer/requeue` **整场零触发**；
- ⚠️ 也**与录制无关**：之前"每次录制时都遇到"是巧合（录制拉长了连接/加载时间窗）。

#### 三、修法（v89）

`MixinOpenGuiHandlerConnectingGuard`：`@Inject` 到
`cpw.mods.fml.common.network.internal.OpenGuiHandler#channelRead0` 的 HEAD（`cancellable`），
客户端 `Minecraft.theWorld == null`（= 还在连接/加载）时**丢弃这个 OpenGui 包**
⇒ 不会进入那个构造函数 ⇒ 不再 NPE ⇒ 不再断连；世界正常存在时行为不变。

**可行性的关键**：`channelRead0` 的两个参数
（`io.netty.channel.ChannelHandlerContext`、`cpw.mods.fml.common.network.internal.FMLMessage$OpenGui`）
**都是非混淆类**，可以直接在源码里写；而同一条链上的
`NetworkRegistry.getLocalGuiContainer` 参数是**混淆名**（`yz`=EntityPlayer、`ahb`=World），源码里**写不出来** ⇒
只能选这个注入点。

**立刻可用的规避法（无需补丁）**：在服务器上**开着焦点操纵器界面**时断开/换服务器 ⇒ Salis Arcana 会尝试
恢复它 ⇒ 崩。**断开前先按 ESC 退出该界面**。

#### ★ 新增坑 70

**70. ★★ 日志里两个不同标题的异常若栈完全相同，那是同一个异常被两个 catch 各记了一次——别当成两个问题。**
本轮 `OpenGuiHandler exception` 与 `HandshakeCompletionHandler exception` 栈逐帧相同，
一开始差点被当成"两个 bug"，正是"后者才是致命"这一点决定了修哪个漏斗。

---

### 12.67 ★「回放只剩天空」**结案：录像里没有世界初始区块（录制时机问题，非缺陷）**

> **证据**：Tester_B两份 `.mcpr` + 洛洛本机三份（A/B 实验）+ 洛洛回放反馈「这个的地图没有加载」。

#### 一、量化对照（同一解析器；`recording.tmcpr` = `[i32 时间增量][i32 长度][1B 包id][payload]`，大端）

| 录像 | 环境 | 时长 | `0x21 CHUNK_DATA` | `0x26 MAP_CHUNK_BULK` | 合计 | 回放结果 |
|---|---|---|---|---|---|---|
| Tester_B「一进世界就录」 | 单人 | 34 s | 1449 | 373 | **1822** | ✅ 正常 |
| Tester_B「中途才录」 | 单人 | 27 s | 125 | 25 | **150** | ❌ 只剩天空 |
| 洛洛「先玩再录」 | 单人 | 58.6 s | 749 | 275 | **1024** | （取决于走动范围） |
| 洛洛「服务器第 1 段」 | 多人 | 40.9 s | 325 | 111 | **436** | 部分 |
| 洛洛「服务器第 2 段」 | 多人 | 9.2 s | 91 | 18 | **109** | ❌ **地图没加载**（他确认） |

⇒ 区块包**少一个数量级**的录像就是"只剩天空"；而且**密度**（包/秒）比总量更能说明问题：
一进世界就录 **53.6/s**，"先玩再录" **17.5/s**，服务器中途 **11.8/s**。

#### 二、机制（为什么必然如此）

ReplayMod 只在**收到区块包的那一刻**把它写进录像。世界初始加载时服务端一次性推送视野内全部区块
（单人 ≈1500–1800 个包，1–2 秒内完成）；**录制若在那之后才启动**，这些包早已发完、**不会再发一遍**
⇒ 录像里只剩后续零星的区块更新 ⇒ 回放时没有区块可建。

- ⚠️ **1.7.10 客户端不会主动请求区块**（区块由服务端推）⇒ 多人服务器下**客户端侧无法补偿**；
- 单人理论可走 `IntegratedServer` 强制重发，但属高风险改造，**不值得**；
- **上游 ReplayMod 本来的使用约束就是"从进入世界开始录"**。

#### 三、对策（写进说明，不改代码）

1. **打开 ReplayMod 的 `AUTO_START_RECORDING`（自动开始录制）** ⇒ 每次进入世界/服务器即开录，
   从此不会缺初始区块（`Setting.AUTO_START_RECORDING`，见 12.64 的 `Setting` 类清单）；
2. 已经录坏的录像**无法补救**（区块数据根本不在文件里）；
3. 需要"看地形"的素材，一律用自动录制产出的那份。

> 顺手确认解析器可信：四份文件全部 `badLen=0 / tailLeft=0 / badId=1`（仅一个收尾包）⇒ 结构自洽。

---

### 12.68 让录像不再缺地形的两条补丁路线（记录 + 可行性评估）

> **缘起**：洛洛提议「**在服务端 mod 加入推送功能**」（服务端在录制开始时把已加载区块重推一遍）。
> 承接 12.67 的结案（缺地形 = 录像里没有世界初始区块），这里把两条路线记下来并做技术评估。

#### 方案 S —— 服务端推送（洛洛的提议）

服务端在"录制开始"时，把该玩家视野内已加载的区块**重新发送**一遍。

- **优点**：客户端只是正常收包 ⇒ **零额外序列化开销、无卡顿**；包顺序天然正确；
  `ServerReplay-1.7.10` 已经是"纯 mixin 服务端录制器"，加这个功能是**自然扩展**。
- **致命前提**：**服务器必须装这个 mod**。公共服务器（测试者常玩的 `<server>`）
  装不了 ⇒ **只适用于单人世界和自己开的服务器**。
- **实现要点（1.7.10）**：
  1. **触发信号**：需要"录制开始了"这一事件。客户端发一个自定义包（`C17PacketCustomPayload`
     或 `SimpleNetworkWrapper`）通知服务端；单人世界则可直接调用；
  2. **重发**：清掉该玩家的"已发送区块"记录让 `PlayerInstance` 重推，**或**直接从
     `ChunkProviderServer` 取区块 → 构造 `S26PacketMapChunkBulk`（**公开构造** `S26PacketMapChunkBulk(List)`）
     → 经 `EntityPlayerMP.playerNetServerHandler.sendPacket` 发出；
  3. **限速**：分批发送，避免几百个包一次性打爆连接。

#### 前置核实：**ReplayMod 本身不是纯客户端 mod**（2026-10-13 查证）

方案 S 的可行性取决于"服务端能不能装东西"，所以先把这条查实（洛洛提问）：

- `mcmod.info` 里 ReplayMod 是多模块包，除 6 个客户端模块（recording / replay / render / editor /
  simplepathing / extras）外，还有一个 **`replaymod-restrictions`**，描述原文是
  **"Server-side Restrictions Module of the ReplayMod - Restrict what clients can do in replays"**
  ⇒ 官方自带**服务端模块**（用途：限制客户端在回放里的行为）；
- `com.replaymod.core.ReplayMod` 的 `@Mod` 注解里**既无 `clientSideOnly` 也无 `serverSideOnly`**
  ⇒ **服务端可以加载**；并带 `acceptableRemoteVersions = "*"` ⇒ 服务端不装也能连；
- 本机日志也可佐证：`[Server thread] ... Sending event FMLServerStartedEvent to mod replaymod`；
- ⚠️ **1.7.10 版没有"服务端录制"** —— 这正是要自研 `ServerReplay-1.7.10` 的原因（12.31~12.33）。

**对方案 S 的结论**：服务端装 mod 属**官方支持**的路径，推送功能做成独立服务端补丁与
`replaymod-restrictions` **并存不冲突**；公共服务器能否使用只取决于服主是否愿意装。
⚠️ 另注：我们发布的 v8x jar 是"改过的 ReplayMod 整包"，虽然 `mixins.replaycompat.json` 走
`"client": [...]` 列表（专用服务端不会应用），但 `ReplayModTransformers` coremod 属客户端 GL 范畴
⇒ 真上服务端应**单独出服务端构建**，不要把客户端整包丢过去。

#### 方案 C —— 客户端补写区块（**不需要服务端**）

录制器一开工，就用**客户端内存里已有的区块**自行构造区块包写进录像。

- **可行性已核实**：
  - `S21PacketChunkData` 有**公开构造** `S21PacketChunkData(Chunk, boolean, int)` ——
    **构造方法没有名字**，重命名不影响，可以**直接 `new`**；
  - `PacketListener.save(Packet)` 是 public ⇒ 可以把我们构造的包直接喂给录制器；
  - 注入点：`PacketListener.<init>(ReplayMod, Path, ReplayFile, ReplayMetaData)` 的 **TAIL**
    （每个录制会话开始都会走这里）；
  - 数据来源：`ChunkProviderClient.field_73236_b`（`chunkMapping`，`LongHashMap`）。
- **优点**：**任何服务器都能用**（含公共服务器），不需要任何一方配合；客户端持有的正是
  "自己视野内"的区块 —— 而录像需要的恰好就是这些。
- **代价**：要自己序列化（每区块 20–100 KB，视野内 200–400 个 ⇒ 数 MB～数十 MB），会有**一次明显卡顿**
  （必须分帧摊开）；`LongHashMap` 的遍历需反射其内部数组与 `LongHashMapEntry`。
- **风险**：`PacketListener.save` 只在 `connectionState == PLAY` 时才真正落盘，注入时机必须落在游戏阶段
  （最稳妥是"进世界后的第一帧"，而不是构造函数里立刻写）。

#### 建议

| 场景 | 选哪条 |
|---|---|
| 单人世界 / 自己的服务器 | **方案 S**（干净、零卡顿、改动最小） |
| 公共服务器（测试者现状） | **方案 C**（唯一可行） |

两者可共存并互相去重。**优先级：先 S 后 C** —— S 改动小、风险低、`ServerReplay` 已在手；
C 作为"通用兜底"排在其后，用来覆盖公共服务器这个真实场景。

---

### 12.64 滚轮灵敏度**游戏内可调**（`/replaycompat wheel <n>`）+ ★ **映射表陷阱** —— 交付 **v87**（2026-10-13 下午）

> **缘起**：Tester_B在 v86 上的反馈「灵敏度还可以调高一点」「如果可以在游戏内调是最好的」。

#### 一、★ 新增坑 69：**别信 `methods.csv`，以项目自己的 SRG jar 为准**

本机 `%USERPROFILE%\.gradle\caches\minecraft\de\oceanlabs\mcp\mcp_stable\12\methods.csv`
与**本项目实际使用的映射不一致**：

```
methods.csv          : addChatMessage -> func_145747_a
srg_merged_minecraft : EntityPlayer 里没有 func_145747_a，只有 func_146105_b(IChatComponent)
```

⇒ 只看 csv 会把方法名写错（运行期 `NoSuchMethodException`，或被 `require = 0` 静默吞掉）。

**正确做法（本项目可复用的通用手法）**：`build\rfg\` 下同时有 **MCP 名版**
（`recompiled_minecraft-1.7.10.jar`）与 **SRG 名版**（`srg_merged_minecraft.jar`），
它们是**同一份 class 的两种命名**（RFG 只重命名）⇒ 用 `javap -p` 取两边的方法列表**按位置逐条对照**，
并**要求目标类里唯一匹配**。本次据此确认：

| MCP | SRG | 用途 |
|---|---|---|
| `EntityClientPlayerMP.sendChatMessage(String)` | **`func_71165_d`** | 指令拦截点 |
| `GuiIngame.getChatGUI()` | **`func_146158_b`** | 回显链路 |
| `GuiNewChat.printChatMessage(IChatComponent)` | **`func_146227_a`** | 回显 |
| `Minecraft.getMinecraft()` | `getMinecraft` / `func_71410_x` | 反射**双拼写**兜底 |

⚠️ `EntityPlayer` 两边方法数 **188 vs 173 不一致** ⇒ 位置对照**并非处处安全**，必须落在具体类里核对。

#### 二、v87 的指令实现

- **拦截点**：`@Mixin(EntityClientPlayerMP)` + `@Inject(method = "func_71165_d", at = HEAD, cancellable = true)`
  —— 聊天框所有消息（含指令）的唯一漏斗；处理了就 `ci.cancel()` ⇒ **纯客户端**，不发往服务器
  （单机/任何服务器可用，也不会撞服务器同名指令）。
- **逻辑全在** `ReplayMouseState#handleChatCommand`（mixin 注入体只做一次调用，坑 29 同源纪律）；
- **回显**用反射：`Minecraft.getMinecraft()`（双拼写）→ `ingameGUI`(`field_71456_v`) →
  `func_146158_b()` → `new ChatComponentText(...)`（**构造可直接写**——重命名不改 `<init>`）→ `func_146227_a`；
  反射失败退回 `System.out`；
- **语法**：`/replaycompat wheel`（查看）、`/replaycompat wheel <1-120>`（设置）、`/replaycompat <n>`（简写）；
- **默认值 5 → 10**（Tester_B要求再高一点），仍可 `-Dreplaycompat.wheelSteps=N` 覆盖。

#### 三、Tester_B这一轮的三条新反馈

| # | 反馈 | 状态 |
|---|---|---|
| 1 | 「灵敏度还可以调高一点 / 最好游戏内调」 | ✅ **v87**：默认 10 步 + `/replaycompat wheel <n>` |
| 2 | 「**跨维度之后已经调整过的速度会重置**」 | ⏳ 待办：跨维度（`S07PacketRespawn`）后相机实体被重建 ⇒ 需在重建后**恢复用户设好的速度** |
| 3 | 「私人维度（`PERSONAL_DIM_180`）里**渲染不出玩家**」 | ⏳ 待办：疑与 12.63 的 `S0CPacketSpawnPlayer.writeString(null)` NPE 同源（录像里缺玩家 spawn 条目） |

#### 四、洛洛本机 A/B 实验（进行中，勿动 `mods\`）

v86 已部署到本机 `mods\`（`D5163D48…`；v65 回滚 jar 在手），按 12.63 的判据录两组：
**A = 先玩 30 s～1 min 再开始录**、**B = 重进世界立刻开始录**，随后解析两份 `.mcpr` 对比区块包数。
⚠️ 本机旧录像**不可用作对照**（多为多人服务器 `singleplayer:false`，且经 `Replay Recovery` 重写，
包型分布与Tester_B的单人录像不同）。

---

### 12.69 Tester_B v87 后两条现场反馈：**跨维度速度重置**定案 + **玩家 spawn 条目写不进录像**的真身 —— 交付 **v90**（2026-10-13 傍晚）

> **来源**：Tester_B在 v86/v87 上的两条反馈（12.64 第三节表 #2 / #3）。
> **产出**：`replaymod-1.7.10-2.5.2-gtnh-v90-speedspawn.jar`（`10877672` B /
> `SHA256 4679D378170B65EE9B14D2736BE4D0E1319DA2676B12DFFA72BD65F1FBEC0F75`），
> 包在 `release\GTNH-ReplayMod-v90-待测\` + `.zip`。
> 产物类数：**38**（29 mixin + 9 util）= v89 的 35 再加 `MixinS0CPacketSpawnPlayerProfileGuard`
> 与 `ReplayCameraSpeed` / `ReplaySpawnProfile`；jar 内 `com/gtnh/replaycompat/**` 由 48 → **51**。

#### 一、反馈 #2「跨维度之后已经调整过的速度会重置」

**速度不在相机实体上**。`javap -c` 上游 `replaymod-1.7.10-2.5.2.jar`：

```
ClassicCameraController.<init>   MAX_SPEED = 10.0; THRESHOLD = MAX_SPEED/20; DECAY = MAX_SPEED/3
increaseSpeed()                  setCameraMaximumSpeed(MAX_SPEED + 0.5)
decreaseSpeed()                  setCameraMaximumSpeed(MAX_SPEED - 0.5)
setCameraMaximumSpeed(d)         if (d < 2.0d || d > 20.0d) return;   // 范围外静默忽略
                                 MAX_SPEED = d; THRESHOLD = MAX_SPEED/20; DECAY = 5.0
InputReplayTimer.handleScroll(i) ReplayHandler.getCameraEntity().getCameraController()
                                   → increaseSpeed()/decreaseSpeed()  （我们自己滚轮调的就是它）
SpectatorCameraController        increaseSpeed()/decreaseSpeed() 都是 `return`（空方法）
                                 且**没有** MAX_SPEED 字段
```

跨维度会替换客户端世界，ReplayMod 随之重建相机实体与控制器 ⇒ `MAX_SPEED` 回到 10.0。

**修法（v90）**：新增 `util\ReplayCameraSpeed`，由 `ReplayMouseState.tick` 每帧驱动（与滚轮同一个
"保证被调用"的钩子，坑 59）：

- **世界对象或控制器实例变化** ⇒ 用**上游自己的** `increaseSpeed()/decreaseSpeed()` 逐步走回用户值。
  ⚠️ **不能直接反射写 `MAX_SPEED`**：`THRESHOLD`（=MAX/20）与 `DECAY`（`setCameraMaximumSpeed`
  里固定 5.0，构造函数里却是 MAX/3）是派生值，直接写字段会让它们不同步；
- **实例没变而值变了** ⇒ 判为用户自己调的（滚轮 / 上游按键 / v87 指令），记住新值，供下次跨维度恢复；
- 用户没调过之前**什么都不做**，绝不覆盖上游默认值。

判据行（每次会话最多 5 条）：
`[replaycompat] camera speed: the world changed and the speed had been reset to 10.0 - restored the wheel setting 15.0`

**顺带定量的一个体验事实**：v87 默认"每格 10 步"= 每格 **+5.0**，而范围只有 **2.0~20.0**（默认 10.0）
⇒ **往上两格到顶、往下三格到底**，到顶后再滚上游静默忽略（看起来像"失灵"）。已写进给测试者的说明。

#### 二、反馈 #3「私人维度渲染不出玩家」—— 12.63 指的方向对，但**成因猜错了**

**`RecordingEventHandler.spawnRecordingPlayer()` 的字节码**（上游 jar）：

```
EntityClientPlayerMP p = mc.thePlayer;   // field_71439_g
assert p != null;
packetListener.save(new S0CPacketSpawnPlayer(p));
```

**`S0CPacketSpawnPlayer.func_148840_b`（= `writePacketData`）的字节码**（MCP 版与 SRG 版逐条对照，
`field_148955_b` 两版**同名** ⇒ 运行时就是它；`func_148840_b` 与崩溃栈一致）：

```
writeVarIntToBuffer(field_148957_a);                              // entity id
UUID id = field_148955_b.getId();
writeStringToBuffer(id == null ? "" : id.toString());             // ← 有 null 兜底
writeStringToBuffer(field_148955_b.getName());                    // ← ★ 没有兜底 ⇒ NPE
writeVarIntToBuffer(field_148955_b.getProperties().size());
... 每个 property 一组 name/value/signature
```

⇒ 崩溃栈里的 `because "<parameter1>" is null` 指的是 **`GameProfile` 的 name 为 null**，
**不是** profile 本身为 null（那样会在上一句就 NPE，且措辞会是 `field_148955_b is null`）。
`PacketListener.getPacketData` 抛出的异常被上层吞掉并打日志 ⇒ **整包没进录像** ⇒
回放里没有"玩家自己"这个实体。

**★ 勘误 12.63（它猜"该类被 skinport / CustomSkinLoader 改写"）**：本轮**全量扫描实例的 338 个 mod jar**
（解压每个 class 条目，在常量池里按 ASCII 搜 `S0CPacketSpawnPlayer`）：

```
archaicfix-0.8.0.jar          :: org/embeddedt/archaicfix/mixins/common/core/MixinEntityTrackerEntry.class
replaymod-1.7.10-2.5.2-gtnh.jar :: com/replaymod/replay/FullReplaySender.class
```

**只有这两个**，且皮肤相关 mod（skinport / CustomSkinLoader）**根本没引用它** ⇒ 那段 `writeString`
就是**1.7.10 原版代码**，不需要任何 ASM 改写来解释（新增**坑 71**）。

**修法（v90）**：`util\ReplaySpawnProfile` + `mixin\MixinS0CPacketSpawnPlayerProfileGuard`
（`@Inject(method = "func_148840_b", at = HEAD, remap = false, require = 0)`）：名字缺失/为空时换成
一个可写的 `GameProfile`（名字取玩家自己的名，取不到用 `"Player"`；**UUID 与皮肤 properties 原样保留**）。
本来正常的包一个字节都不改。判据行：
`[replaycompat] spawn packet: the player profile had no name (so the recording could not store the player's spawn entry) - wrote it with name '…' and kept the properties`

**⚠️ 只修了一半**（**v91 候选**）：`spawnRecordingPlayer` 的唯一调用者是
`NetHandlerPlayClient.handler$zbp000$replaymod$recordOwnJoin`（挂在 `S01PacketJoinGame` 上）
⇒ **进世界之后才手动开始录的录像，压根不会走这段代码**，仍然没有玩家自己的条目。
补法与方案 C 同源（在 `PacketListener` 构造之后补一次），但前提要先按下面的勘误重新核实。

#### 三、★ 勘误 12.68：「`save` 只在 `connectionState == PLAY` 时才落盘」**不准确**

`PacketListener.save(Packet)` 的字节码开头（上游 jar）：

```
0:  invokestatic  MCVer.isOnMainThread()
3:  ifne          17                       // 不在主线程 → scheduleOnMainThread 后 return
17: aload_1 instanceof S0CPacketSpawnPlayer // 是玩家 spawn 包 → metaData.players += uuid; saveMetaData()
93: invokestatic  System.currentTimeMillis()   // 之后直接算时间戳并写盘
```

⇒ `save` 自己**没有** `connectionState == PLAY` 的门。12.68 里"注入时机必须落在游戏阶段、
不能放在构造函数里立刻写"这条前提**需要重新核实**（真正可能拦截的地方在 `getPacketData` /
`channelRead` 的状态机推进里，还没看完）。方案 C 的注入点选择必须以
`release\方案S与C-分析.md` 的证据为准，不要沿用 12.68 的措辞。

#### 四、★ 新增坑 71

**71. ★★ 崩溃栈里出现某个原版类的方法，不等于有 mod 用 ASM 改写了它——先"全量扫谁引用了这个类"，再判断。**

12.63 由 `<parameter1> is null` + "1.7.10 原版不写字符串"推断"S0CPacketSpawnPlayer 被
skinport / CustomSkinLoader 改写过"，据此把方向定成"皮肤 mod 的锅"。本轮把 338 个 mod jar 的
每个 class 常量池扫了一遍，引用者只有 archaicfix 与 replaymod 两个 ⇒ **就是原版行为**
（`GameProfile#getName()` 返回 null），**猜测把一整轮的方向带偏了**。
代价极小的正确顺序：先 `S0CPacketSpawnPlayer` 全量引用扫描（几秒），再决定要不要假设有改写。

---

### 12.70 本轮环境复核（2026-10-13 傍晚，接手会话）

- `mods\replaymod-1.7.10-2.5.2-gtnh.jar` 实测 = **v89**
  （`FD3E03B040C99CAE57FAD410E0A4746B520922657C0D9B04DD66C0F64FCE2A89` / `10870800` B）
  —— ⚠️ **`PROJECT_HISTORY.md` §0 与 `HANDOVER` 速览表里"mods 里是 v65 / v86"都已过期**，以本行为准；
- 无 java 进程运行；`release\` 最新待测包 = **v90**（本轮的），v89 留档；
- `<home>\Downloads\fml-client-latest (15).log`（`32141894` B / 12:46–12:51）= **v86 的日志**
  （`mixins.replaycompat.json source=…-v86.jar`、`stepsPerNotch=5`、10 次 5 步调速）
  = **12.62 已消费的那一份**，**不是** v89 的反馈；其中
  `dropped an OpenGui packet` **0 次**（v86 还没有这个补丁），
  `String.getBytes` NPE **1 次**（12:49:38，进世界那一刻，见 12.69 第二节）。
  ⇒ **v89 的现场反馈尚未到达。**

#### ★ v89 的现场结果（本机 13:37–13:52 会话，`logs\fml-client-latest.log` = **33 MB**）

> ⚠️ 踩到一个新变体：`Get-ChildItem` 对**正在被写入的** `fml-client-latest.log` 报 **0 字节**，
> 而 `[System.IO.FileInfo]::new(...)` 报 **32999393** 字节 —— **先入为主地信 `Length` 会得出
> "没有日志"的错误结论**（本轮差点据此回复"v89 反馈没到"）。

| 判据 | 结果 | 判读 |
|---|---|---|
| 崩溃报告（13:30 后） | **0** | 无新崩溃 |
| `Mixin apply for mod … failed` / `InvalidInjectionException` | **0 / 0** | 零注入失败 |
| `RejectedExecutionException` | **0** | ★ **v88 确认生效**（此前 1343 条刷屏） |
| `A fatal error … network handshake` / `HandshakeCompletionHandler` / `OpenGuiHandler exception` | **0 / 0 / 0** | **没有断连** |
| `dropped an OpenGui packet` | **0** | ⚠️ **补丁一次都没触发** ⇒ 这次是"**没踩到**那时序"，**不能算握手兜底验证通过** |
| `wheel: adjusted the camera speed by 10/-10/-10 step(s)`（13:41:26–27） | **3 次** | 滚轮正常（v87 默认 10 步/格） |
| `replay key bridge installed` / `wheel probe: per-tick wheel hook is alive` | **1 / 1** | v82 / v85 的钩子都在 |
| `replay entity census: players=1`（13:42:33） | 1 | 回放里有玩家实体 |
| `Writing packet: … String.getBytes … "<parameter1>" is null`（13:41:49、13:41:54） | **2 次**（均 join game 那一刻） | ★ **12.69 第二节在洛洛本机复现**（两次连接 <server> 各一次） |

**录像结果：4 份新录像的区块包全部严重不足**（解析器与大端口径同 12.67，`duration` 与 `metaData.duration` 吻合）：

| 录像 | 环境 | duration | `0x21` | `0x26` | 合计 | 密度 |
|---|---|---|---|---|---|---|
| `2026_10_13_13_39_45_0.mcpr` | 单人 | 102.8 s | 125 | 150 | **275** | 2.7/s |
| `2026_10_13_13_41_49_0.mcpr` | 服务器 a18 | 25.5 s | 15 | 48 | **63** | 2.5/s |
| `2026_10_13_13_41_49_1.mcpr` | 服务器 a18 | 7.7 s | 60 | 12 | **72** | 9.4/s |
| （12.67 基线）Tester_B一进世界就录 | 单人 | 34 s | 1449 | 373 | **1822** | 53.6/s |
| （12.67 基线）Tester_B中途才录 | 单人 | 27 s | 125 | 25 | **150** | 5.6/s |

⇒ **比"只剩天空"的基线（150 / 5.6/s）还低** ⇒ 这几份回放同样只剩天空，
**属 12.67 已结案的录制时机问题，不是 v89 引入的回归**（v89 没有改动录制时机）。

#### ★ 顺手拿到的"铁证"：元数据说有玩家、录像里一个都没有

`2026_10_13_13_41_49_0.mcpr` 的 `metaData.json`：

```
{"singleplayer":false,"serverName":"<server>","customServerName":"zako~",
 "duration":25492,"players":["<uuid>",
                             "<uuid>"]}
```

而同一份录像里 **`0x0c S0CPacketSpawnPlayer` = 0 个**（单人那份没 NPE，有 1 个）。
这正是 12.69 第二节推断的机制的现场对照：`PacketListener.save` **先**把 UUID 记进
`metaData.players`（字节码 17–89 行）**再**去 `getPacketData` 序列化包（93 行起），
包因 profile 里的字符串为 null 而抛 NPE、被上层吞掉 ⇒ **元数据记了 2 个玩家，录像里 0 个 spawn 条目**。
⇒ **v90 修的就是这个**（`MixinS0CPacketSpawnPlayerProfileGuard`）。
> ⚠️ **这句当时把 null 猜成了 `GameProfile.getName()` —— 已被 12.71 推翻**：真凶是
> **皮肤属性（`Property`）的 name/value/signature 之一**，v90 因此**没有生效**，改由 **v91** 修好。

#### ★ 这份录像同时补上了方案 C 分析报告所缺的基线样本

`release\方案S与C-分析.md` 的"最不确定项 3"是「工作区内没有『客户端中途开始录制』的 `.mcpr` 样本」。
`…\replay_recordings\2026_10_13_13_39_45_0.mcpr` 就是一份**单人、中途开始录制、duration 102.8 s、
区块包仅 275 个（2.7/s）**的现成样本，动方案 C 之前先用它核对基线。

---

### 12.71 ★「玩家条目写不进录像」的真凶（**两度猜错**）：不是玩家名，是**皮肤属性里的 null 字符串** —— 交付 **v91**（2026-10-13 傍晚）

> **材料**：Tester_B v90 现场反馈（「这个是开了自动录制的，还是**一开始看不到人**，但是**出入维度后正常**」）
> + 两份原始 `.mcpr` + `fml-client-latest (16).log`（`50296591` B / 14:09–14:34）。
> **产出**：`replaymod-1.7.10-2.5.2-gtnh-v91-spawnprops.jar`
> （`10878888` B / `SHA256 C5D627BC33A7EDC82622C7828DEA1A9BB87D8B505D3326AF0F5E9F803C0CD820`）。
> **与 v90 的差异**：**只有 `util/ReplaySpawnProfile.class`**（6030 → 8971 B），零其他改动。

#### 一、决定性现场数据（Tester_B两份录像的 `0x0c` 逐条解析）

| 录像 | Tester_B标注 | `0x0c` 出现时刻 | 每条带的 properties |
|---|---|---|---|
| `2026_10_13_14_10_36.mcpr` | 没开自动录制 | **t = 0 ms** | **0** |
| `2026_10_13_14_12_05.mcpr` | 开了自动录制 | **t = 24168 ms / 32470 ms** | **0** |

⇒ 第二份**前 24 秒录像里没有任何玩家条目** ⇒ 回放一开始看不到人；后来**进出维度**重新触发了一次
「进世界」，ReplayMod 才补上 ⇒「出入维度后正常」。**与Tester_B的原话逐字吻合。**
⇒ 反过来也说明：**凡是写成功的条目，properties 都是 0 个** —— 只有带皮肤属性的档案会写失败。

#### 二、v90 为什么没救：**mixin 注入成功了，但代码判错了字段**

`fml-client-latest (16).log` 里：

```
[14:09:46] [mixin/CoFHCore]: Mixing MixinS0CPacketSpawnPlayerProfileGuard from mixins.replaycompat.json
                            into net.minecraft.network.play.server.S0CPacketSpawnPlayer      ← 注入成功
--- [spawn packet:] count=0        ← 我们的修复日志一次都没打
--- [String.getBytes] count=2      ← NPE 照旧（14:11:50 / 14:12:05，各自紧跟 CustomSkinLoader 加载完档案那一行）
```

v90 的 `ensureWritable` 只检查 `GameProfile.getName()`；Tester_B的名字是好的（`'Tester_B'`）
⇒ 判定 "healthy"、直接 return ⇒ **什么都没修**。

#### 三、真凶：`writePacketData` 的 **5 处** `writeStringToBuffer`

`javap -c` 逐个偏移（MCP 版与 SRG 版一致）：

| 偏移 | 内容 | 兜底 |
|---|---|---|
| 30 | UUID | **有**（`id == null ? "" : id.toString()`） |
| 41 | `GameProfile#getName()` | 无 ← 12.63 与 v90 都以为在这里 |
| 100 | `Property#getName()` | 无 |
| 109 | `Property#getValue()` | 无 |
| 118 | **`Property#getSignature()`** | 无 ← **最可能的真凶** |

`RecordingEventHandler#spawnRecordingPlayer` 的字节码是
`packetListener.save(new S0CPacketSpawnPlayer(mc.thePlayer))` ⇒ profile 就是
`mc.thePlayer.getGameProfile()`，而 **CustomSkinLoader 会往它上面挂 `textures` 属性**
（日志里 NPE 紧跟在 `Tester_B's profile loaded` 之后）⇒ 该属性的某个字段为 null。

⚠️ **行号不可作为证据**：崩溃栈写 `func_148840_b(SourceFile:81)`，而 MCP 版 `LineNumberTable` 里
`writePacketData` 从 **83** 行才开始 ⇒ **两个 1.7.10 jar 的源行号并不一致**，只能按**偏移 + 调用序列**定位。

#### 四、读侧证据：写**空串**是合法的

`readPacketData` 对每个属性**无条件读三个字符串**（`readStringFromBuffer(32767)` × 3），
然后 `new Property(name, value, signature)` —— 没有任何 null 分支
⇒（长度 0 的串读得回来）⇒ 把 null 换成 `""` **既合法也不丢东西**（回放不校验皮肤签名）。

#### 五、v91 的修法与判据

`ReplaySpawnProfile#ensureWritable` 改为：profile 为 null / 名字为 null 或空 / **任一属性的
name·value·signature 为 null** ⇒ 换一份**净化过的副本**（同 UUID、可用名字、null 字符串换成 `""`；
属性名本身为 null 的那一条只能丢弃，但**绝不动 spawn 条目本身**）。本来能写的包一字节不动。

日志（每次会话各一条，★ 这次能直接读出真凶字段）：

```
[replaycompat] spawn packet: the player profile could not be written (name='Tester_B', property
 'textures' had a null signature) - wrote a sanitised copy (properties=1, 1 with empty strings)
 so the recording can store the player's spawn entry
[replaycompat] spawn packet: profile is writable as-is (name='Tester_B', properties=1) - nothing to repair
```

**离线预验证**：`.selftest\SpawnProfileReflectCheck.java` 已扩到 v91 的 API 面
（`PropertyMap.values/put`、`Property.getName/getValue/getSignature`、`Property(String,String,String)`），
并在真实 authlib 1.5.21 上实测「null signature 的 `Property` 可构造 → 净化后 `signatureLength=0`」。

#### 六、★ 新增坑 72

**72. ★★「mixin 注入成功」不等于「你的代码做了事」。**

v90 的 `ensureWritable` 只在**修复时**打日志，于是「日志里没有 `spawn packet:`」被读成
"这段代码没被调用"；**实际是它被调用了、但判断条件盯错了字段、静默变成了 no-op**。
`require = 0` 让 mixin 层面也毫无声响，两个"静默"叠在一起就骗过了整整一轮。

⇒ **守卫/补丁类代码必须在"该动手却决定不动手"时也留一条可判读的日志**
（v91 因此补了 `profile is writable as-is (…)` 那一行）。
这与**坑 68**（"没有判据行"不能推断"功能没触发"）是同一族，方向相反：
坑 68 是**注入点不保证被调用**，坑 72 是**判据行本身设计得无法区分"没调用"与"调用了但判错"**。

---

### 12.72 v91 **现场确认**（★ 真凶逐字命中）+「换世界重置速度」为何 v90 一直没生效 —— 交付 **v92**（2026-10-13 夜）

> **材料**：Tester_B v91 反馈（「目前看来没啥太大问题了」+「**换世界依旧会重置旁观者的速度**」
> +「局内开始录制的话得先换维度才能显示（这个问题不大）」）+ `fml-client-latest (17).log`
> （`50903185` B，18:46–18:57）+ 两份新录像。
> **产出**：`replaymod-1.7.10-2.5.2-gtnh-v92-camspeed2.jar`
> （`10879821` B / `SHA256 6C1EE32C9DC4D94F8C74F7F53795E2A58A3C9A4519C391DDCF6F0718E5AFB47E`）
> = v91 + `ReplayCameraSpeed` 重写 + `ReplayMouseState` 接线，**只差这两个 class**。

#### 一、★ v91 的判定**逐字命中**（12.71 的推断被现场确认）

日志里就是我设计的那条判据：

```
[replaycompat] spawn packet: the player profile could not be written
 (name='Tester_B', property 'textures' has a null signature)
 - wrote a sanitised copy (properties=1, 1 with empty strings) so the recording can store the player's spawn entry
```

| 判据 | v90 | v91 |
|---|---|---|
| `String.getBytes` NPE | 2 次 | **0 次** |
| 录像里的玩家条目 | 全部 `props=0` | `2026_10_13_18_49_27.mcpr`：**`t=147ms props=1 [textures(val=444, sig=0)]`** |

⇒ **真凶 = `Property#getSignature()` 为 null**。三个版本、三次判定：
**12.63 猜"皮肤 mod 改写了这个类"**（错，见坑 71）→ **12.69/v90 猜 `GameProfile.getName()`**（错，静默 no-op）
→ **12.71/v91 由"成功条目全是 props=0"反推出是属性字段**（对）。**前后错了两轮**。

#### 二、「换世界重置速度」为什么 v90 没生效（**坑 72 一轮之内第二次现形**）

| 判据（同一次会话） | 次数 |
|---|---|
| `wheel: adjusted the camera speed by 10 step(s)`（滚轮确实调过） | **10** |
| `camera speed: … restored …`（v90 的恢复日志） | **0** |
| `camera speed persistence unavailable`（反射初始化失败） | **0** |
| `ClassicCameraController` / `SpectatorCameraController`（类名从不出现在日志里） | 0 |

反射没失败、滚轮调过、恢复却一次没跑 ⇒ 代码**被调用了，但停在某个静默 `return` 上**。
v90 的 `tick` 里唯一的静默出口就是：

```java
Field maxSpeed = findField(controller.getClass(), "MAX_SPEED");
if (maxSpeed == null) {
    return;   // e.g. SpectatorCameraController: no speed of its own, leave it alone
}
```

⇒ **最可能：Tester_B用的是旁观者相机**（`SpectatorCameraController` **没有** `MAX_SPEED` 字段，
且它的 `increaseSpeed()/decreaseSpeed()` 是**空方法** ⇒ 那种相机自己**不保存速度**）。
同一个坑（"该动手却决定不动手"却不留日志）**一轮之内犯了两次**：一次判错字段（spawn），一次悄悄 return（速度）。

#### 三、v92 的改法

1. `MAX_SPEED` 读不到**不再放弃**：改用「**重放净滚轮格数**」兜底 ——
   把玩家滚过的净格数记下来，世界/控制器重建后**把同样的输入再喂一遍**（`noteWheelSteps`）；
2. 加**必须出现的诊断行**（进回放即打一次）：
   `camera speed: controller=<类名> maxSpeedReadable=<t/f> speed=<值> wheelSteps=<n>`；
   若该 controller 无速度字段，再补一行 `… has no MAX_SPEED field …`；
3. 恢复日志改为同时报 controller 类名、恢复后的值、跨越的格数。

**判据（下一轮）**：
- `camera speed: controller=… maxSpeedReadable=…` **必须出现**（不出现 ⇒ 钩子没进回放分支）；
- 跨世界后 `camera speed: the world changed (controller=…) … restored …`；
- 若出现 `has no MAX_SPEED field` ⇒ Tester_B确用旁观者相机，需按"那种相机不存速度"另设计（**v93 候选**）。

#### 四、仍开着的两条（下一轮）

1. **「局内开始录制得先换维度才能显示」**（Tester_B自评"问题不大"）：机制已明确 ——
   `spawnRecordingPlayer` 的唯一调用者是 `S01PacketJoinGame` 上的 `recordOwnJoin` ⇒
   **中途开录的录像压根不走那段代码**；补法 = 在 `PacketListener` 构造后、若已在世界里就补一次
   （v91 已把 `writePacketData` 修好，补的包现在能写进去了）；
2. v92 的速度诊断结果。

---

### 12.73 「换世界 / 从人物身上下来 速度重置」的真因：**前两版读的是 `static` 上限常量** —— 交付 **v93**（2026-10-13 深夜）

> **材料**：Tester_B v92 反馈（「换世界**和从人物身上下来**相机速度都会被重置」+「依旧没解决」）
> + `fml-client-latest (18).log`（`61415655` B，19:07–19:20）+ 两份新录像。
> **产出**：`replaymod-1.7.10-2.5.2-gtnh-v93-camspeed3.jar`
> （`10880577` B / `SHA256 670333B416C71BF437C8E7B6CED8300ADC947815EE078497C0DF39C877A275D0`）
> = v92 重写 `ReplayCameraSpeed`，**只差这一个 class**。

#### 一、v92 的诊断行**自己把根因招了**（这就是加"必须出现的判据"的价值）

```
[19:12:16] camera speed: controller=com.replaymod.replay.camera.VanillaCameraController
                          maxSpeedReadable=true  speed=1000.0  wheelSteps=0
[19:12:35] camera speed: the world changed (controller=VanillaCameraController,
                          speed had been reset to 1000.0) - restored the wheel setting 1000.0 across 64 wheel step(s)
[19:13:02] camera speed: SpectatorCameraController has no MAX_SPEED field - …
[19:13:02] camera speed: the camera controller changed (controller=SpectatorCameraController) - re-applied 64 notch(es)
[19:13:04] camera speed: the camera controller changed (controller=VanillaCameraController, speed had been reset to 1000.0) - …
```

两条信息：**①** Tester_B用的相机是 **`VanillaCameraController`**（既不是我一直假设的 Classic，也不是
Spectator；"从人物身上下来"那条链路 = Spectator ↔ Vanilla 来回切，v92 确实检测到了）；
**②** 那个 `speed=1000.0` **是个恒定常量**。

#### 二、字节码：三种相机的速度存储**完全不同**（`javap -c`）

| 相机 | 字段 | 语义 |
|---|---|---|
| `ClassicCameraController` | `private double MAX_SPEED = 10.0`（**实例**） | `increase/decrease` = ±0.5，范围 [2,20]；`setCameraMaximumSpeed` 还会重算 THRESHOLD/DECAY |
| `VanillaCameraController` | `private static final int MAX_SPEED = 1000`（**static 常量**）<br>`private static final int MIN_SPEED = -1000`<br>**`private int speed`**（实例，默认 0） | `increaseSpeed(): speed = Math.min(1000, speed+1)`<br>`decreaseSpeed(): speed = Math.max(-1000, speed-1)`<br>`update()` 里位移 ∝ `Math.pow(1 + speed/300.0, …)` |
| `SpectatorCameraController` | 无 | `increase/decreaseSpeed` 是空方法 |

⇒ **v90/v92 用 `getDeclaredField("MAX_SPEED")` 按名字找**，在 Vanilla 上命中 **static final 上限 1000**
⇒ 读到的"当前速度"永远不变 ⇒ `userSpeed` 永远是 null ⇒ 只能靠 `netSteps` 兜底，
而 `netSteps` 被自己的 `MAX_STEPS = 64` 夹住（她实际调得更多）⇒ **恢复不完整**，看起来"在跑却没用"。

#### 三、v93 的改法

1. `findSpeedField`：**跳过 `static` 成员**，优先 `MAX_SPEED`（Classic，double），
   否则取数值型的 `speed`（Vanilla，int）；
2. 步长按字段类型取（double ⇒ 0.5，int ⇒ 1），仍用**上游自己的** `increaseSpeed()/decreaseSpeed()`
   逐档走回 —— 不用 `putfield` 直接写，避免 Classic 的 THRESHOLD/DECAY 失配；
3. `MAX_STEPS` 64 → **2000**（覆盖 Vanilla 的 ±1000）；
4. 诊断行改为打印**字段名 + 类型**（`speedField=speed(int)`），并在玩家第一次设定速度时多打一行
   `the player set the camera speed to … (field speed) - it will be kept across dimension changes …`。

#### 四、★ 新增坑 73

**73. ★★ `getDeclaredField(name)` 拿到的可能是 `static` 常量——反射读"当前值"之前必须查 `Modifier.isStatic`。**

`MAX_SPEED` 在 `ClassicCameraController` 里是**实例**值（当前速度），在 `VanillaCameraController` 里
是**同名的 static 上限**；`increaseSpeed()` 改的是另一个叫 `speed` 的实例字段。
**同名不同义 + static/实例混淆**，两版（v90/v92）都被它骗过，而且**日志看起来一切正常**（"读到了 1000.0"、
"恢复了"）。这一轮的方法论收获：**v92 那条"必须出现的诊断行"是唯一能揭穿它的东西** ——
如果只按"有没有恢复日志"判断，v92 会一直被当成已经修好。

> **同类错误一轮内连犯三次的完整账**（都在本轮）：
> ① 12.63 猜"皮肤 mod 改写了 `S0CPacketSpawnPlayer`"（错，坑 71）；
> ② 12.69/v90 猜"`GameProfile.getName()` 为 null"（错，坑 72）；
> ③ 12.70~12.72 按名字读速度字段、拿到 static 常量（错，坑 73）。
> **共同点：都拿"看起来合理的解释"当结论，而没有先做一次廉价的实测**（全量引用扫描 / 打印全部
> 5 个字符串字段 / 检查 static）。

---

### 12.74 v93 **现场通过**（相机速度那条终于结案）+ Tester_B收工、转交Tester_A（多人与粗剪）（2026-10-13 深夜）

> **材料**：Tester_B v93 反馈（「**目前没问题了**」+「至于**粗剪**和**多人**让**Tester_A**测吧」）
> + `fml-client-latest (19).log`（`18671596` B，19:34–19:39）。

#### 一、v93 的判据行**逐条按设计出现**（这是"读对字段"的直接证据）

```
[19:36:21] camera speed: controller=com.replaymod.replay.camera.VanillaCameraController speedField=speed(int) speed=0.0 wheelSteps=0
[19:36:26] camera speed: the player set the camera speed to 10.0 (field speed) - it will be kept across dimension changes and camera switches
[19:36:37] camera speed: SpectatorCameraController has no instance speed field - …
[19:36:37] camera speed: the camera controller changed (controller=SpectatorCameraController) - restored the wheel setting (re-applied 800 notch(es))
[19:36:38] camera speed: the camera controller changed (controller=VanillaCameraController, speed was 0.0) - restored the wheel setting 843.0 across 800 wheel step(s)
[19:36:46] camera speed: the world changed (controller=VanillaCameraController, speed was 0.0) - restored the wheel setting 744.0 across 710 wheel step(s)
[19:36:53] camera speed: the camera controller changed (controller=SpectatorCameraController) - restored the wheel setting (re-applied 710 notch(es))
[19:37:05] camera speed: the world changed (controller=SpectatorCameraController) - restored the wheel setting (re-applied 710 notch(es))
[19:37:10] camera speed: the camera controller changed (controller=VanillaCameraController, speed was 0.0) - restored the wheel setting 744.0 across 710 wheel step(s)
```

要点：
- **`speedField=speed(int) speed=0.0`** ⇒ 终于读到**实例字段**（默认 0），而不是那个 static 常量 1000；
- `the player set the camera speed to 10.0` ⇒ 玩家的改动**被感知**（v90/v92 从未感知到）；
- **换世界**（`the world changed`）与**从人物身上下来**（`Spectator ↔ Vanilla` 切换）**两条链路都恢复成功**；
- 旁观者相机那段仍走 notch 重放（那种相机确实不存速度）。

**同一次会话其余判据全 0**：`String.getBytes` / `Writing packet` / `RejectedExecutionException` /
`Mixin apply … failed` / `InvalidInjectionException` / `A fatal error` **均为 0** ⇒ **无回归**。
⇒ **Tester_B这条线（滚轮 / 速度 / 玩家条目 / 地形）到此收工。**

#### 二、测试者分工调整（Tester_B提出）

- **Tester_B**：收工（她的环境与项目均已复验通过）；
- **Tester_A（338 mods）**：接**多人服务器** + **粗剪/导出**两项。
  已写好交接给他的专项清单：`release\GTNH-ReplayMod-v93-待测\给Tester_A-多人与粗剪复验.md`
  （覆盖 ① 滚轮速度与换世界保持 ② 玩家条目判据 ③ 保存/编辑录像的 skip 容错
  ④ v89 握手兜底**至今未被真实触发**，需要他在多人下踩 ⑤ **A/B 两份多人录像**作为"补地形"开发的基线
  ⑥ `entity census` 定量"看不到人" ⑦ 快捷键/M 标记 ⑧ 导出渲染 与 编辑器两条"剪"的入口）。

  ⚠️ "粗剪"具体指哪个入口尚未确认（ReplayMod 1.7.10 里 `Render video` 与 `Replay Editor` 两个界面），
  清单里**两条都写了**并要求他注明点的是哪个。

#### 三、下一步（按优先级）

1. 等Tester_A的多人 + 粗剪反馈；
2. **v94 候选**：**中途开始录制时补一次玩家 spawn 条目**（Tester_B自评"问题不大"的那条）——
   机制完全明确、v91 已把写包修好，只需在 `PacketListener` 构造后若已在世界里补一次；
3. **方案 C**：`release\方案S与C-分析.md` 已就绪（结论**先 C 后 S**），基线样本也齐了
   （单人中途开录 275 chunk 包 / Tester_B A 组 2024、B 组无 —— 见 12.67 与 12.71）。
