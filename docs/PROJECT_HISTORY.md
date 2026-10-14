# GTNH 2.9.0-beta-3 × ReplayMod 适配 —— 交接（2026-10-13 深夜，更新到 v93）

> 🚀 **要在新对话里接手？直接复制 [`PROJECT_HISTORY.md`](PROJECT_HISTORY.md) 的内容当第一条消息**（已在 v91 校准）。
>
> **这份文档是自包含的**：读完它就能接手，不必先读 5400 行的 `COMPATIBILITY_LOG.md`。
> 要成因、字节码证据、67 个坑的细节时，按文中「§节号」回查 [COMPATIBILITY_LOG.md](COMPATIBILITY_LOG.md)（§12.1~12.60）。
> 通用复验方法在 [VERIFICATION_GUIDE.md](release/VERIFICATION_GUIDE.md)；分支与重建在 [branches/README.md](branches/README.md)。

---

## 0. 一句话现状

| | |
|---|---|
| **`mods\` 里跑着的** | ⚠️ **2026-10-14 复查：ReplayMod 当前未加载** —— `mods\replaymod-1.7.10-2.5.2-gtnh.jar` 已被改名为 **`…-gtnh.jar.disabled`**（`10870800` B = 正是 **v89** 的字节数）。此前记录：v89（`FD3E03B040C99CAE57FAD410E0A4746B520922657C0D9B04DD66C0F64FCE2A89`，2026-10-13 13:26 实测）；回滚 jar `replaymod-1.7.10-2.5.2-gtnh-v65-handonly.jar` 仍在手 |
| **最新待测包** | **v93**：`release\GTNH-ReplayMod-v93-待测\` + `.zip`（jar `10880577` B / `670333B416C71BF437C8E7B6CED8300ADC947815EE078497C0DF39C877A275D0`）= **29 mixin + 9 util（38 class）** ⇒ **要发就发它**（= v92 + 速度字段读对；与 v92 **只差 `ReplayCameraSpeed`**）。Tester_B已在 v93 上复验通过（「目前没问题了」），Tester_A待发 |
| **独立分支快照** | `branches\v82-hotkeys\`（源码 + 产物 + `FINGERPRINTS.txt`）—— ✅ **已同步到 v93**（2026-10-13 深夜） |
| **修改总账** | `WHAT_CHANGED.md` —— 一页看清"改了原始 mod 的什么"（新增 52 个条目 / 改写 3 个上游文件 / 删除 0） |
| **客户端结案进度** | 主诉问题 **基本全部结案**（§3）；**滚轮**已现场验证、并可按需调（v87 的 `/replaycompat wheel <n>`）；**「回放只剩天空」已结案**（§4 ② / 12.67 = 录制时机，非缺陷）；**握手期 OpenGui 崩溃**（"进服务器就断连"）已修待测（§12.66） |
| **服务端** | `ServerReplay-1.7.10\` 已瘦身为「纯 mixin 服务端录制器」+ 诊断；**未部署**；**实体包漏录**是唯一核心缺口（§12.31） |
| **测试者** | **Tester_A（338 mods）、Tester_B**；**洛洛本人 = 「Luoxueruoling」**。⚠️ **分工（2026-10-13 夜起）**：**Tester_B收工**（她的环境与项目均已复验通过）；**Tester_A接「多人服务器 + 粗剪/导出」**，专项清单见 `release\GTNH-ReplayMod-v93-待测\给Tester_A-多人与粗剪复验.md` |

---

## 1. 交付物清单（全在 `<workspace>`）

| 类型 | 路径 | 用途 |
|---|---|---|
| 正式发布版 | `release\GTNH-ReplayMod-v65\` + `.zip` | 在 `mods\` 里跑着；含 `安装说明.md`、`338复验指引.md` |
| **最新待测包** | `release\GTNH-ReplayMod-v89-待测\` + `.zip` | **转发用这个**（含 v75~v89 全部改动） |
| 分支快照 | `branches\v82-hotkeys\`（+ `branches\README.md`） | 源码/产物/重建命令；两条线总览 |
| 历史待测包 | `release\GTNH-ReplayMod-v66~v88-*\` | 留档 |
| **通用复验方法** | `release\VERIFICATION_GUIDE.md` | 不绑版本；已复制进每个待测包 |
| **交接文档（本文）** | `PROJECT_HISTORY.md` | 自包含交接 |
| 细节文档 | `COMPATIBILITY_LOG.md`（约 5400 行 / §12.1~12.60 / 坑 1~67） | 成因、证据、每轮结论 |
| 客户端适配源码 | `replaymod-compat\`（`src\` + `mixin-res\mixins.replaycompat.json`） | 纯 javac、**无 reobf**（§5） |
| 服务端工程 | `ServerReplay-1.7.10\` | RFG 工程；`build\libs\` 有 shaded / client 两版 |
| 服务端诊断编排器 | `tools\gtnh-replaymod-serverdiag.ps1` | 干跑通过（exit 0），**未执行** |
| 取证工具 | `.selftest\mcpr-*.ps1`、`.selftest\MethodCallAudit.java` | mcpr 时间桶/普查/比对；**ASM 调用点审计（谁在调这个方法）** |

---

## 2. 交付链（v66 → v89，每版修什么）

| 版本 | 修的问题 | 现场验证 | 节 |
|---|---|---|---|
| **v66** | 导出**白屏**（改走非 PBO 抓帧，根因是反射绕过了 Angelica 状态管理器） | ✅ 338「渲染没问题」 | 12.34 |
| **v67** | 放开鼠标点界面**崩**（AE2 的 Waila 集成缺判空） | ✅ 现场「没有崩」 | 12.35 |
| **v68/v69** | popup 偷锁 → 输入屏关闭不再清锁（治**循环**） | ✅ `RELEASED` **136 → 3** | 12.36/12.38 |
| **v70** | 滚轮调相机速度（复用上游控制器） | ❌ **从未生效**（反射读错类 → 12.59 定案） | 12.43 |
| **v71** | Shift+ESC 强制复位 | 部分（12.49 起 ESC 也能救） | 12.44 |
| **v72** | 一致性检查（锁定但没挂 ⇒ 自动放锁） | ❌ 抖动下永不触发（坑 58） | 12.45 |
| **v73** | **保存录像崩溃**（`SquashFilter` 逐包容错） | 部分（触发序列已明确） | 12.47 |
| **v74** | 只读坐标探针 | ✅ 数据到手，**排除坐标量纲** | 12.48/12.50 |
| **v75** | 滚轮改读 `org.lwjglx.*`（坑 57）· 让位（玩家列表隐藏 overlay）· ESC 急救 | 部分 | 12.49 |
| **v76** | ★ **抑制 `runTick → setIngameFocus()` 抢鼠标** | ✅ `no input screen` **189→15** | 12.50 |
| **v77** | 滚轮读取点 TAIL→HEAD | ❌ 仍读 0（12.59 定案） | 12.51 |
| **v78** | ★ **把鼠标看护搬到每帧 `runTick`**，输入屏不在时自己挂回（坑 59） | ✅ 实测兜底触发 10 次 | 12.52 |
| **v79** | 时间轴"陈旧 offset"自愈 | 无害保留（数据证明不是它） | 12.53 |
| **v80** | **实体普查探针** | ✅ 定案「人消失」= 相机视角 | 12.54 |
| **v81** | 滚轮改 `Mouse.getDWheel()` | ❌ 被 MouseTweaks 抢读 | 12.55 |
| **v82** | ★ **救回 ReplayMod 快捷键（含 M 加标记）**：输入屏常驻会冻结 `KeyBinding`（坑 65） | ✅ 日志 `key bridge installed` | 12.56 |
| **v83** | 时间轴探针改挂 `onClick` + 打印实例类名（坑 66） | ✅ 抓到 4 次真实点击 | 12.58 |
| **v84** | 滚轮改用 **`totalScrollAmount` 差分**（真因：MouseTweaks 抢读，坑 67） | ❌ 仍无判据行 ⇒ **12.61 定案为观测盲区**（旧注入点不保证被调用） | 12.59 |
| **v85** | ★ **滚轮搬进每帧 `runTick`** + 直接复用上游 `InputReplayTimer.handleScroll`（旧注入点降级为探针）· ★ **修「标记处理器崩溃」**：`SquashFilter.onPacket` 的**第三条**读取入口（`PacketChunkData.read`）+ `MarkerProcessor.apply` 兜底 | ✅ **现场双验证**（Tester_B `(14)`）：滚轮 10 次真实调速；那批坏包被接住 4 次、**没崩** | 12.61 |
| **v86** | 滚轮**灵敏度 ×5**（每格 1 步 → 5 步；SDL 每格 delta=1，而 `increaseSpeed()` 只调一档 ≈×1.1 ⇒ Tester_B「有点太低」）；可用 `-Dreplaycompat.wheelSteps=N` 覆盖 | ✅ Tester_B已用（`stepsPerNotch=5` + 10 次 5 步调速）；反馈「还可以再高一点、最好游戏内调」 | 12.62 |
| **v87** | 滚轮**游戏内可调**：客户端指令 `/replaycompat wheel <1-120>`（拦 `func_71165_d`，纯客户端不发服务器）+ 默认 5 → **10** | 待测 | 12.64 |
| **v88** | 录制器**已关闭的保存线程池**不再被塞包（`MixinPacketListenerSaveGuard`）⇒ 干掉 1343 条 `RejectedExecutionException` 刷屏 | 待测 | 12.65 |
| **v89** | ★ **握手期 OpenGui 兜底**：客户端世界还没建立时丢弃"打开 GUI"包 ⇒ 不再触发 Salis Arcana 在 `ContainerFocalManipulator` 构造里的 `tileEntity` 判空缺陷（**"进服务器就断连"的真因**，与录制无关） | 待测 | 12.66 |
| **v90** | Tester_B两条：① **跨维度后相机速度不再重置**（`ReplayCameraSpeed`，每帧记住/走回用户值）· ② 玩家 spawn 条目写包崩溃（`ReplaySpawnProfile` 补名字并保留 UUID/皮肤属性）—— ⚠️ **② 盯错了字段、静默无效**，由 v91 改正 | ① 待测；② **无效** | 12.69 |
| **v91** | ★ **Tester_B「一开始看不到人」的真凶**：为 null 的**不是玩家名**，而是**皮肤属性（`Property`）的 signature**（`writePacketData` 里 `Property#getSignature()` 那处同样没有兜底）⇒ 带皮肤属性的档案整包写失败 ⇒ 那个玩家条目丢失。v91 净化属性（null → 空串，读侧本就读三个串，合法） | ✅ **现场逐字命中**（18:46 会话）：`property 'textures' has a null signature`；NPE **2→0**；录像 `props=1 [textures(val=444, sig=0)]` | 12.71 |
| **v92** | Tester_B「**换世界依旧会重置旁观者的速度**」：v90 的保持逻辑**静默失效**（最可能是旁观者相机 `SpectatorCameraController` **没有 `MAX_SPEED` 字段**，v90 遇到它直接 `return`，无日志）。v92 读不到字段**不再放弃** ⇒ 改用「重放净滚轮格数」兜底 + 强制诊断行 | ⚠️ **诊断行立功**：抓出真实相机是 `VanillaCameraController`、我读的是 **static 常量 1000** | 12.72 |
| **v93** | ★ **速度重置的真因**：三种相机存速度的方式**完全不同**；`VanillaCameraController` 里 **`MAX_SPEED` 是 `static final` 上限常量（=1000）**，真值是 **`private int speed`**（每步 ±1、范围 ±1000）。v90/v92 按名字读 `MAX_SPEED` ⇒ 永远读到常量 ⇒ 察觉不到玩家的改动。v93 **跳过 static**、优先 `MAX_SPEED`（Classic/double）否则 `speed`（Vanilla/int），步长按字段类型取，仍用上游步进 | 待现场复验：`speedField=speed(int)` + `the player set the camera speed to …` | 12.73 |

**键位**：`Ctrl` = 锁定/放开鼠标可见 · `Shift` = 退出旁观（上游） · **`ESC`（锁失效时）/ `Shift+ESC` = 强制复位**。

---

## 3. 已结案（客户端）

**早期**：主菜单入口（12.12）· 物品栏拖不动（12.10）· 回放中心滚轮量纲（12.15）· 保存录像面板（12.16~18）·
恢复未完成录像**本就可用，零改动**（12.20）· 打开回放必崩 S25（12.21，v32）· 回放界面鼠标/视角（12.23，v42/v48）·
导出不可用（12.24）· 换视角夜视崩溃（12.25，v51）· `entity tracker` 弹窗（12.26，**真环境验收 ✅**）·
玩家不渲染（12.27，v53）· 打开即旁观/方向错乱/退不出旁观（12.28，v56）· 手（12.30，v65）。

**本轮（v66~v89）**：

| 问题 | 结论 | 证据 |
|---|---|---|
| 导出白屏 | ✅ v66 | 338「渲染没问题」 |
| 点界面崩溃 | ✅ v67 | 现场「没有崩」 |
| 鼠标锁循环 | ✅ v69 | `RELEASED` 136→3 |
| **卡鼠标 / 点几下时间轴就不能动** | ✅ **v76 + v78**（测试者确认「已经没发生了」） | `locked, but no input screen` **189→94→85→15**；`mounting … myself` 实测触发 |
| **大时间轴调不了** | ✅ **结案 = 上游机制**（12.60）：点击与换算**完全正常**（请求时间全落在 `length` 内、`offset=0/zoom=1.0`），而每点一次就 **`Client disconnected from server`** = 重放一次 | v83 的 `onClick` 探针 4/4 |
| **快捷键「基本全是摆设」** | ✅ v82：输入屏常驻冻结 `KeyBinding`（坑 65）⇒ 用 `InputEvents` 旁路补喂（只在锁定时） | 日志 `replay key bridge installed` |
| **「人消失 / 滚木」** | ✅ **定性 = 相机视角，不是实体丢失**（12.59）：普查 20 条 `players` 恒 1~2、**从无 0**；`renderView=EntityOtherPlayerMP` 即"旁观某人=第一人称" | v80 普查 |
| **滚轮调速** | ✅ **v85 现场生效**（12.62：Tester_B日志 **10 次真实调速**）；灵敏度 v86 ×5 → **v87 默认每格 10 步**，且可 `/replaycompat wheel <n>` 游戏内现场调 | `wheel: adjusted the camera speed by N step(s) from the wheel` |
| **标记处理器崩溃** | ✅ **v85 修好**（12.61/12.62）：`SquashFilter.onPacket` **第三条**读取入口 + `MarkerProcessor.apply` 兜底；v84 里原来必崩的坏包现在只打 skip 日志 | `replay editing: skipped a packet …`，**零崩溃报告** |
| **回放只剩天空** | ✅ **结案 = 录制时机**（12.67）：录像里没有世界初始区块；对策 = 打开 ReplayMod 的**自动开始录制** | 区块包 **1822**（一进世界就录）vs **109~150**（中途录） |
| **「进服务器就断连」** | ✅ v89 兜底（12.66）：真因是 **Salis Arcana** 在 `ContainerFocalManipulator` 构造里对 `tileEntity` 判空缺失，**与录制和本工程无关** | 待现场复验：`dropped an OpenGui packet …` |
| **跨维度 / 从人物身上下来 后速度重置**（Tester_B v87 / v91 / v92 三次反馈） | ✅ **v93 现场通过**（12.74）：真因是 `VanillaCameraController` 里 **`MAX_SPEED` 是 `static final` 上限常量（1000）**、真值叫 `private int speed`；v93 跳过 static 读对字段。日志逐条命中：`speedField=speed(int) speed=0.0` → `the player set the camera speed to 10.0` → 换世界/下人物后 `restored … 744.0` | ✅ 已闭环（Tester_B「目前没问题了」） |
| **玩家 spawn 条目写不进录像**（Tester_B「一开始看不到人」） | ✅ **v91 现场确认修好**（12.71/12.72）：真凶是**皮肤属性 `Property#getSignature()` 为 null**（12.63 猜错了类、v90 猜错了字段）；v91 净化属性。Tester_B 18:46 会话 `String.getBytes` NPE **2→0**，录像条目变成 `props=1 [textures(val=444, sig=0)]` | ✅ 已闭环 |
| **局内开始录制要先换维度才显示玩家**（Tester_B自评"问题不大"） | ⏳ 机制已明确：`spawnRecordingPlayer` 只在 `S01PacketJoinGame` 被调用 ⇒ 中途开录的录像压根不走那段代码 | **v93 候选**：在 `PacketListener` 构造后補一次（v91 已把写包修好，补的包能落盘） |
| 夜视 / entity tracker / 相机 / 手 / 导出路径 | ✅ 均在真环境复现通过 | 12.25~12.34 |

---

## 4. 仍未解决（**各有判据，别凭猜**）

| # | 问题 | 现状 | 需要什么 |
|---|---|---|---|
| ① | **滚轮调速** | v70→v84 **五次失败**，12.61 定案：旧注入点 `UserInputGuiScreen.func_146274_d` **不保证被调用**（回放中鼠标泵走 `InputReplayTimer.updateTimer` → `GuiScreen.handleInput` → 只有 `Mouse.next()` 还有事件时才转发），所以"无判据行"无法区分"钩子没被调用"与"读成 0"；v85 搬到每帧 `runTick` + 复用上游 `handleScroll` + 无条件探针 | **v85 现场复验**：`wheel probe: per-tick wheel hook is alive (…)`（必定出现）→ 滚轮后 `wheel: adjusted the camera speed by N step(s)…`；旧注入点若被调用会额外出现 `wheel probe: UserInputGuiScreen.func_146274_d was reached` |
| ② | **回放只剩天空** | ✅ **结案（12.67）**：= **录像里没有世界初始区块**，属**录制时机**问题、非缺陷。量化：一进世界就录 **1822** 个区块包（53.6/s）→ 正常；中途才录 **150 / 109** 个（5.6~11.8/s）→ 只剩天空（洛洛回放 `2026_10_13_13_20_14_1` 亲口确认"地图没加载"）。**已录坏的文件无法补救**（区块数据不在文件里） | **对策**：打开 ReplayMod 的 **`AUTO_START_RECORDING`（自动开始录制）** ⇒ 以后每次都从进世界开录；1.7.10 客户端不会主动请求区块，多人下客户端侧无法补偿 |
| ③ | **保存/编辑录像崩溃** | **已拿到新栈（12.61）**：Tester_A 10-12 16:09 `Description: Running marker processor`，`SquashFilter.onPacket:362` → `PacketChunkData.read:58` → `readLoad:258`（**第三条读取入口**，v73 只护了 `readBulk` 两条；同日志里 v73 已成功跳过 5 个坏包）；v85 已修（该入口 + `MarkerProcessor.apply` 兜底） | **v85 复验**：同一份录像再点「应用标记」⇒ 只出现 `replay saving: skipped an unreadable chunk data (load) packet…` / `replay editing: skipped a packet…`，**不再弹崩溃报告** |
| ④ | **服务端实体包漏录** | 12.31 字节级改写成因（采集点空窗 + 三类高频包丢失）；诊断与编排器就绪 | 跑一次实机诊断（§6 末） |
| ⑤ | `meta` 的 `players`/`singleplayer` | 低优先（要扩 `onOutboundPacket` 签名） | 排期 |
| ⑥ | 往回跳"飞走"/人消失 | **不修**：上游重放机制 + 相机重置（12.59/12.60） | —— |
| ⑦ | **中途才开始录的录像没有玩家条目** | v90 只修了"写包 NPE"这一半；`spawnRecordingPlayer` 的唯一调用者是 `S01PacketJoinGame` 上的 `recordOwnJoin` ⇒ 进世界之后才开录的录像压根不走那段代码（12.69 二） | **v91 候选**：在 `PacketListener` 构造后补一次（与方案 C 同源注入点）；先按 **12.69 三** 的勘误核实落盘条件 |

---

## 5. ★ 环境与铁律（每条都踩过）

**环境**：实例 `<instance>` · 本机是 **Windows PowerShell 5.1**（不是 pwsh 7）·
**DPI 150%** · **没有 GLFW**（lwjgl3ify 3.0.31 走 SDL3）· 游戏运行时 `mods\` 的 jar 被锁（先查 `Get-Process java`）·
跑测试脚本会 kill 所有 java · ffmpeg 在 `…\Gyan.FFmpeg_…\ffmpeg-9.0.2-full_build\bin\ffmpeg.exe`。

**写代码铁律（★ 是本次新踩的）**
1. **纯 javac、无 reobf** ⇒ **MCP 成员名运行时不存在**；**mixin 注入体里连 SRG 名都不能直接写**，一律挪进 util 反射（坑 29）；
2. ★ **lwjgl3ify 的 redirector 只改写字节码调用点** ⇒ **反射必须指向 `org.lwjglx.*`**，读 `org.lwjgl.input.*` 会拿到空壳（坑 57）；
3. ★ **消费式 API 先扫谁在抢读**：`Mouse.getDWheel()` 被 **MouseTweaks** 每帧读走（坑 67）；
   LWJGL2 的鼠标 getter 有"当前事件视图"与"累积视图"两套语义（坑 63）；
4. ★ **每帧兜底不能寄生在"故障后就不会被调用"的方法上**（`updateUserInputGui` 的唯一自动调用者是输入屏自己，坑 59）；
5. ★ **超时自愈要用时间窗口比例，不要用连续帧数**（抖动会把计数清零，坑 58）；
6. ★ **加载期守卫要"延迟"不要"丢弃"**（坑 53）；读鼠标事件要在消费者读完队列**之前**（坑 60）；
7. **判断 mixin 是否注入：看字节码导出**（`-Dmixin.debug.export=true` + `javap`），**不要看日志**；
   同时搜 `Mixin apply for mod … failed` / `InvalidInjectionException`（`require = 0` 的解析失败是静默的，坑 51/54）；
8. ★ **PowerShell 里 `ForEach-Object { … throw }` 不会中止脚本** ⇒ 编译失败照样注入（坑 64）：
   **用 `foreach` 收集失败、循环外统一 throw**，并**事后数产物 class 数**（v89 起应为 **35** = 28 mixin + 5 util + 2 内部类）；
9. **兜底返回值要看调用方**（`readBulk(...)` 被直接 `.iterator()` ⇒ 只能给空列表，12.47）；
10. **坐标/量纲类问题先量数据再改代码**（滚轮 12.15、鼠标坐标 12.48、时间轴 12.53/12.58 都是量出来的）；
11. ★ **不要用"日志里没有判据行"推断"功能没触发"**，除非那条判据所在的注入点保证被调用（坑 68）；
    ★ **两个不同标题的异常若栈逐帧相同，就是同一个异常被两处 catch**（坑 70）；
12. ★ **`methods.csv` 不可信**：MCP→SRG 必须拿 `build\rfg\` 的 **MCP 版（`recompiled_minecraft-1.7.10.jar`）
    与 SRG 版（`srg_merged_minecraft.jar`）javap 位置对照**，并在目标类里确认唯一（坑 69）；
13. ★ **`.mcpr` 是 ZIP**（`metaData.json` / `recording.tmcpr` / `mods.json` / crc32）；
    `recording.tmcpr` = `[i32 时间增量][i32 长度][1B 包id][payload]`（**全大端**）⇒ 直接统计包型即可判断"录像有没有地形"（12.67）。
14. ★ **崩溃栈里出现某个原版类的方法 ≠ 有 mod 用 ASM 改写了它** —— 先全量扫"谁引用了这个类"
    （解压每个 jar 的 class、在常量池里按 ASCII 搜类名，338 个 jar 几秒跑完），再决定要不要假设有改写。
    **坑 71**：12.63 由 `<parameter1> is null` 推断"`S0CPacketSpawnPlayer` 被 skinport / CustomSkinLoader
    改写过"，把方向定成"皮肤 mod 的锅"；本轮实测引用者只有 `archaicfix` 与 `replaymod` 两个
    ⇒ **就是 1.7.10 原版行为**（`GameProfile#getName()` 为 null），**猜测把一整轮方向带偏了**。
    （⚠️ 12.71 又进一步推翻了"就是 `getName()`"这个结论本身：真凶是**皮肤属性的字段**。）
15. ★★ **「mixin 注入成功」≠「你的代码做了事」**（**坑 72**，v90 栽在这）：`require = 0` 让注入层毫无声响，
    而守卫代码若**只在"动手修"时打日志**，那"日志里没有判据行"就同时兼容"没被调用"与"调用了但判错字段"两种
    情况 —— 必须像 v91 那样在**"该动手却决定不动手"**时也留一条可判读的日志。
16. ★★ **`getDeclaredField(name)` 可能是 `static` 常量**（**坑 73**）：反射读"当前值"前必须查
    `Modifier.isStatic`。`MAX_SPEED` 在 `ClassicCameraController` 是**实例**当前速度、在
    `VanillaCameraController` 是**同名的 static 上限**（真值叫 `speed`）⇒ v90/v92 一直读常量，
    "日志看起来一切正常"却什么都没做。**只有一条"必须出现、且内容可判读"的诊断行能揭穿它。**

**取证规范**：日志一律要 **`logs/fml-client-latest.log`**（我们的输出走 `System.out`，`latest.log` 里看不到，12.39 白拿过一份）·
画面问题要 **F3 截图**（看 `E:` 实体数）· 视频给**原文件**（ffmpeg 抽帧 / `framemd5`）· 每条反馈都要"环境卡片"。

---

## 6. 常用命令（照抄）

```powershell
$compat='<workspace>\replaymod-compat'
$javac ='<jdk8>\bin\javac.exe'
$cp = "$compat\mixin-out;$compat\deps;" +
      "<workspace>\ServerReplay-1.7.10\build\rfg\recompiled_minecraft-1.7.10.jar;" +
      "<workspace>\tools\replaymod-1.7.10-2.5.2.jar;" +
      "<libs>\net\minecraftforge\forge\1.7.10-10.13.4.1614-1.7.10\forge-1.7.10-10.13.4.1614-1.7.10-universal.jar;" +
      "<libs>\io\netty\netty-all\4.0.10.Final\netty-all-4.0.10.Final.jar;" +
      "<libs>\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar;" +
      "<libs>\com\google\guava\guava\17.0\guava-17.0.jar;" +
      "<instance>\mods\appliedenergistics2-rv3-beta-1050-GTNH.jar;" +
      "$compat\libs\asm-9.9.jar;$compat\libs\asm-tree-9.9.jar;$compat\libs\asm-commons-9.9.jar"

# 1) util 先编，2) 再按 mixins.replaycompat.json 的清单编 mixin；★ 失败必须中止（坑 47/64）
$fail=$null
foreach ($f in (Get-ChildItem "$compat\src\com\gtnh\replaycompat\util" -Filter '*.java')) {
  & $javac -proc:none -encoding UTF-8 -nowarn -cp $cp -d "$compat\mixin-out" $f.FullName
  if ($LASTEXITCODE -ne 0) { $fail = "UTIL: $($f.Name)" } }
$json=Get-Content "$compat\mixin-res\mixins.replaycompat.json" -Raw
$names=[regex]::Matches($json,'"Mixin([A-Za-z0-9]+)"') | ForEach-Object { $_.Groups[1].Value }
foreach ($n in $names) {
  & $javac -proc:none -encoding UTF-8 -nowarn -cp $cp -d "$compat\mixin-out" "$compat\src\com\gtnh\replaycompat\mixin\Mixin$n.java"
  if ($LASTEXITCODE -ne 0) { $fail = "MIXIN: Mixin$n" } }
if ($fail) { throw "COMPILE FAILED -> $fail (aborting before injection)" }

# 3) 注入（幂等；基座固定 v48）；注意执行策略要 Bypass
& powershell -NoProfile -ExecutionPolicy Bypass -File <workspace>\tools\inject-replaymod-clientmixins.ps1 `
  -Jar <workspace>\replaymod-1.7.10-2.5.2-gtnh-v48-ctrltoggle.jar `
  -Dest <workspace>\replaymod-1.7.10-2.5.2-gtnh-vNN-<名字>.jar
# 4) ★ 事后校验：class 数应为 38（v90 起：29 mixin + 9 util）；jar 内 com/gtnh/replaycompat/** 应为 51
#    （v89 是 35 / 48；新增 MixinS0CPacketSpawnPlayerProfileGuard + ReplayCameraSpeed + ReplaySpawnProfile）

# 部署 / 回滚（先确认游戏已退出）
Copy-Item <新jar> '<instance>\mods\replaymod-1.7.10-2.5.2-gtnh.jar' -Force
Copy-Item '<workspace>\replaymod-1.7.10-2.5.2-gtnh-v65-handonly.jar' <同上> -Force   # 回滚 v65

# 服务端构建（Gradle 9.4.1 + JDK 25）
$env:JAVA_HOME='<jdk25>'
& '<workspace>\tools\gradle-9.4.1\bin\gradle.bat' --no-daemon build --project-dir '<workspace>\ServerReplay-1.7.10'
# 服务端实机诊断（占屏 3~5 分钟）
& powershell -NoProfile -ExecutionPolicy Bypass -File <workspace>\tools\gtnh-replaymod-serverdiag.ps1 -Deploy -Launch -Collect -Restore

# ASM 调用点审计（"谁在调这个方法"）—— 本轮靠它抓出 MouseTweaks 抢读
& java -cp '<workspace>\.selftest;<workspace>\replaymod-compat\libs\asm-9.9.jar' MethodCallAudit <jar> '#方法名'
```

---

## 7. 下一步（按优先级）

1. **把 v90 发出去**（= v89 + Tester_B两条，见 §12.69）⇒ 三项判据：跨维度速度保持（`camera speed: the world changed …`）·
   spawn 条目补名（`spawn packet: the player profile had no name …`）· **v89 的握手兜底还没复验过**
   （`dropped an OpenGui packet …`，连 `<server>` 看是否还断连）；
2. **v91 候选**：**中途开始录时补一次玩家 spawn 条目**（与方案 C 同源注入点）；动手前先按 **§12.69 三**
   的勘误重新核实 `PacketListener.save` 的落盘条件（12.68 说的"只在 PLAY 落盘"**不准确**）；
3. **已结案的可转告测试者**：卡鼠标（已消失）· 大时间轴（上游重放机制，给了 4 条正解）· 人消失（相机视角，用自由相机）·
   **跨维度速度 / 玩家条目（v90）**；
4. **「只剩天空」已结案**（12.67）：让测试者打开 ReplayMod 的**自动开始录制**即可根治；
5. **服务端实机诊断**（一句话就跑，§6 末）；
6. **让录像不再缺地形**（12.68 / 12.69 三）：**洛洛要求先出分析结果再定路线** ⇒ 见
   `release\方案S与C-分析.md`（方案 S 服务端推送 / 方案 C 客户端补写区块）；
7. 长录像找关键时间 ⇒ 让测试者试 **M 加标记**（v82 的功能，等确认）；
8. 更远：`meta` 的 `players`/`singleplayer`（12.6-⑦）。

---

## 8. 已裁定"不修 / 接受"的（别重复投入）

- 回放刚打开、相机贴人物时**近处发黑**（12.14）；
- 回放里**原版血条仍显示**（GTNH 不发 `HEALTH` 事件，12.29/12.30）；
- **JourneyMap 小地图 / InGameInfoXML 面板**照旧（那两个 mod 自己画的）；
- 导出**不支持深度图**、**比原版略慢**（12.34）；
- **鼠标可见时 WASD 不驱动相机**（上游设计；想飞先按 Ctrl 抓回）；
- **往回跳会"飞"一下 / 人暂时消失**（上游重放机制 + 相机重置，12.59/12.60）。
