# 版本变更记录

本文件记录本兼容层从第一版到当前版本的改动，以及每一版**实际解决了什么现象**。

阅读方式：

* 版本从新到旧排列（v93 → v1）。
* **v66 及以后**逐版列出，因为这一区间是面向用户的正式交付链。
* **v1 ~ v65** 属于内部迭代期，按三个阶段合并叙述；阶段内只保留对后续版本有影响的节点。
* 表中的版本指 jar 文件名里的版本号，命名格式为
  `replaymod-1.7.10-2.5.2-gtnh-vNN-<后缀>.jar`，后缀是构建时自加的语义标签。
* 所有「新增了什么 class」的数字都来自对留档 jar 的**逐条目实测对比**，不是文档里的估算。
  机制层面的成因与验证方法见各源文件的类级 javadoc，以及
  [IMPLEMENTATION_NOTES.md](IMPLEMENTATION_NOTES.md)。

---

## v93

**改动**：重写 `util/ReplayCameraSpeed` —— 遍历字段时跳过 `static`，并按字段类型决定步长。

**解决的现象**：相机速度的「记住并恢复」在部分场景下取值不正确。这是相机速度记忆功能的最终定型版本，测试者确认正常。

## v92

**改动**：重写 `util/ReplayCameraSpeed`、`util/ReplayMouseState`，新增一条「必须出现」的诊断日志行。

**解决的现象**：本身不改变最终行为，属于诊断版本 —— 该诊断行定位出了 v93 所修的真实成因。

## v91

**改动**：重写 `util/ReplaySpawnProfile`（类体积 6030 → 8971 字节）。真凶是玩家 spawn 包中皮肤属性 `signature` 为 `null`。

**解决的现象**：录像里**没有录制玩家自己的条目**。改写后在测试现场逐字命中根因。

## v90

**改动**：新增 `MixinS0CPacketSpawnPlayerProfileGuard`；新增 `util/ReplayCameraSpeed`；新增 `util/ReplaySpawnProfile`。

**解决的现象**：两个新功能（录像玩家条目、相机速度记忆）首次落地，但在本版本中**均未真正生效**，随后分别由 v91 与 v93 修正。

## v89

**改动**：新增 `MixinOpenGuiHandlerConnectingGuard`；重写 `util/NullWorldGuard`。

**解决的现象**：进入服务器时在网络握手阶段断连（「打开 GUI」包在世界尚未创建时被处理）。该场景在后续测试中未被复现，因此这个补丁未经现场验证，作为防御性修复保留。

## v88

**改动**：新增 `MixinPacketListenerSaveGuard`（`PacketListener.save` HEAD 取消）。

**解决的现象**：连接结束后日志被 `RejectedExecutionException` 刷屏，实测 **1343 条 → 0 条**：录制器已关闭的线程池不再被投递数据包。

## v87

**改动**：新增 `MixinEntityClientPlayerMPWheelCommand`，提供游戏内指令 `/replaycompat wheel <1-120>`。纯客户端实现，消息不会发送到服务器。

**解决的现象**：滚轮灵敏度此前只能靠改代码调整，现在可以游戏内直接设定。

## v86

**改动**：重写 `util/ReplayMouseState`（滚轮灵敏度 ×5）。

**解决的现象**：滚轮调速手感过慢。测试者反馈可用。

## v85

**改动**：新增 `MixinMarkerProcessorSafeFiltering`；重写 `MixinSquashFilterSafeBulkRead`（补上第三条读取入口）、`util/ReplayMouseState`（滚轮处理搬进每 tick 的 `runTick`）；`MixinUserInputGuiScreenWheelCameraSpeed` 由功能补丁**降级为纯探针**（4739 → 1350 字节）。

**解决的现象**：两件事在同一次现场测试中双双确认 —— 滚轮可以改变回放相机速度，保存／应用标记不再因坏包崩溃。

## v84

**改动**：重写 `MixinUserInputGuiScreenWheelCameraSpeed`，改用 `totalScrollAmount` 差分读取滚轮量。

**解决的现象**：滚轮调速仍未生效（方向记录，用于排除该读取路径）。

## v83

**改动**：重写 `MixinGuiTimelineClickProbe`，探针改挂在 `onClick` 上。

**解决的现象**：成功捕获 4 次真实点击，为时间轴点击行为定性提供了判据。

## v82

**改动**：新增 `util/ReplayKeyBridge`（+2 个内部类）。

**解决的现象**：鼠标解锁时 ReplayMod 自己的快捷键全部失效（播放/暂停、**M** 加标记 等）。现在会在鼠标解锁状态下旁路补喂 `KeyBinding`，快捷键恢复可用。

## v81

**改动**：重写 `MixinUserInputGuiScreenWheelCameraSpeed`、`util/LwjglMouseBridge`，改读 `getDWheel()`。

**解决的现象**：滚轮调速仍未生效 —— 原始滚轮量被其它 mod 抢先读取。

## v80

**改动**：重写 `MixinMinecraftShiftEscReset`、`util/ReplayMouseState`，加入每帧实体普查。

**解决的现象**：回放中「看不到其它玩家」被定性为**相机视角问题**，而不是实体数据缺失 —— 避免了继续朝错误方向修补。

## v79

**改动**：新增 `MixinGuiTimelineViewportGuard`（`getTimeAt` HEAD）。

**解决的现象**：时间轴视口因陈旧的偏移量而错位。补丁无害，长期保留。

## v78

**改动**：新增 `MixinGuiTimelineClickProbe`（只读探针）。

**解决的现象**：用于定位时间轴点击行为，不改动游戏逻辑。

## v77

**改动**：重写 `MixinUserInputGuiScreenWheelCameraSpeed`。

**解决的现象**：滚轮调速仍无效（方向记录）。

## v76

**改动**：重写 `MixinMinecraftShiftEscReset`、`util/ReplayMouseState`。

**解决的现象**：补丁与上游争抢鼠标控制权。修正后日志中 `no input screen` 记录由 189 条降至 15 条。

## v75

**改动**：新增 `util/LwjglMouseBridge`。

**解决的现象**：鼠标读取的反射路径指向 `org.lwjgl.*`，而本环境经 lwjgl3ify 走 SDL3，该路径不可用；改为按游戏自身的方式读取 `org.lwjglx.*`。

## v74

**改动**：新增 `MixinMouseUtilsCoordinateProbe`（只读探针）。

**解决的现象**：用于排除「鼠标坐标量纲错误」这一嫌疑，不改动游戏逻辑。

## v73

**改动**：新增 `MixinSquashFilterSafeBulkRead`。

**解决的现象**：保存录像时遇到畸形包直接崩溃 —— 第一版容错实现。

## v72

**改动**：重写 `MixinGuiReplayOverlayMouseVisible`（一致性检查，抖动清零）。

**解决的现象**：鼠标状态每 tick 反复抖动。

## v71

**改动**：新增 `MixinMinecraftShiftEscReset`。

**解决的现象**：鼠标状态卡死时无法强制复位，现在 Shift+ESC 可以复位。

## v70

**改动**：新增 `MixinUserInputGuiScreenWheelCameraSpeed`。

**解决的现象**：滚轮调整相机速度的第一次尝试（未成功，最终由 v85 的每帧方案取代）。

## v69

**改动**：新增 `MixinUserInputGuiScreenKeepMouseLock`（重定向输入屏关闭时的鼠标锁清理）。

**解决的现象**：鼠标锁死循环。日志中 `RELEASED` 相关记录由 136 条降至 3 条。

## v68

**改动**：新增 `MixinGuiPopupPreserveMouseLock`；新增 `util/ReplayMouseState`（此后多个鼠标补丁共享的状态中心）。

**解决的现象**：弹窗打开时丢失玩家手动锁定的鼠标状态（鼠标锁循环问题的前半部分）。

## v67

**改动**：新增 `MixinAe2PartAccessorNullMop`。

**解决的现象**：点击 AE2 界面即崩溃 —— AE2 的 Waila 集成没有判空。

## v66

**改动**：新增 `MixinPipelinesNoPbo`（v49 曾加入、v50 摘掉，本版重新加回）；重写 `MixinGuiReplayOverlayMouseVisible`。

**解决的现象**：视频导出得到白屏／空白帧。本版本在测试环境确认「渲染没问题」。

---

## v1 ~ v65（内部迭代期，按阶段合并）

这一区间是功能从零搭建、反复试错的过程，逐版罗列意义不大，因此按三个阶段归纳。

### 阶段一：起步期（v1 ~ v12）—— coremod 与 ASM transformer

* **v1**：官方 jar 的 GTNH 化首版 —— GTNH 风格的 manifest（`MixinTweaker` + `MixinConfigs` + `ForceLoadAsMod`）、汉化 +63 个键、`GuiHandler` 字符串的等长替换。
* **v2**：去掉 mixin 配置上的 `"priority": 500`。实测排除了「物品栏拖不动」与 mixin 应用顺序有关的假设。
* **v4 / v5**：适配层第一个 class `ReplayModTransformers` 出现，manifest 的 `FMLCorePlugin` 开始指向它。结果是**一注册 transformer 就崩**：`ClassCircularityError`（`GLErrorTransformer$1$1`、`GLStateTrackerTransformer$2$1`）并连锁 `NoClassDefFoundError`。
* **v6**：新增 `ReplayModTransformers#getASMTransformerClass()` 默认返回空数组，**两个 transformer 都改为 opt-in（默认不注册）**，同时从上游 mixin 列表里摘掉 `"MixinGuiScreen"`。这是**第一个「物品栏可拖 + 能正常启动」的版本**，也是「物品栏拖不动」问题定性的分界点 —— 根因是 coremod 的 ASM transformer，与 mixin priority、`TweakClass`、`TweakOrder` 均无关。
* **v7 ~ v9**：新增 12 个 asm class —— 两个 transformer 的「具名静态 visitor」重写版（含 9 个内部类）+ `asm/Launcher` 预热；`ClassCircularityError` 归零，注册成功。
* **v10 ~ v12**：把 transformer 作用域收窄到 `com/replaymod/` 与 `de/johni0702/`（`ALLOWED_PREFIXES`），根治了 `StackOverflowError at GLStateTracker.hook_glEnable` 以及「改写消掉其它 mod mixin 的 `@At` 锚点」导致的 `InjectionError`；v12 起 `GLStateTrackerTransformer` 默认注册。

### 阶段二：功能铺开期（v13 ~ v48）—— 从第一个 mixin 到固定基座

* **v13**：★ **第一个功能 mixin `MixinGuiCustomReplayEntry`** —— 上游把回放中心入口挂在 GTNH 上永远不会显示的界面上，导致主菜单完全没有回放入口。
* **v14 ~ v19**：只改这一个 mixin，共 6 次迭代；v19 起按钮显示中文「回放中心」（此前是英文 `ReplayMod`）。
* **v20**：新增 `MixinEtFuturumPlayer` —— 修进入回放时 Et Futurum Requiem 的空指针（类名大小写写错会静默失效）。
* **v22**：新增 `MixinNetHandlerPlayClientSpawnGuard` —— 修「点加载进入回放即崩」。
* **v23**：新增 `MixinGuiScrollableWheelScale` —— 修「回放中心滚轮几乎不动」（SDL3 报 ±1，而 jGui 的算术按 ±120 校准）。
* **v24**：新增 `MixinGuiBackgroundProcessesMainMenu` —— 修「保存录像面板在主菜单根本不挂载」。
* **v25 / v26**：新增两个只读探针 `MixinVanillaGuiScreenEventHandler`、`MixinMouseInputEventProbe`。
* **v27**：新增 `MixinGuiScreenJGuiInputBridge` —— 面板能显示但点不动、输入框打不进字。
* **v29**：修正桥接方法名拼写（`handleMouseInput` → `func_146274_d`），完整链路一次通过：点击生效、面板自行消失、文件归位。
* **v30**：摘掉 3 个诊断探针，得到一个干净可发布的版本。
* **v31**：`NullWorldGuard` 一度被放进 mixin 包，结果一加载就 `NoClassDefFoundError`（mixin 包保护按「包」生效）。
* **v32**：把 `NullWorldGuard` 从 `mixin/` 搬到 `util/`；新增 `MixinNetHandlerPlayClientNullWorldGuard`（14 个 handler 各注入 HEAD）与 `MixinNetworkManagerDeferredFlush`（世界就绪后回灌队列）—— 修「打开回放必崩」（客户端世界尚未创建时收到会解引用 `theWorld` 的包）。
* **v33 ~ v42**：`MixinGuiReplayOverlayMouseVisible` 的 **9 次迭代**：强制 `setMouseVisible(true)` → 轮询 `keyChat`/`keyCommand` → 改用 Shift → 状态写回。期间确认「必须把按键状态写回」，否则每 tick 抖动数百次；终形态为「按住 Shift 放开鼠标」。
* **v43 ~ v47**：鼠标中键探路，`Mouse.isButtonDown`、`keyBindPickBlock`、Forge 两类 `InputEvent` 总线四条路径全部实测排除；v47 是一次回退版本（摘掉 5 项）。
* **v48**：全部恢复，并改为用键盘 **左 Ctrl** 做「点一下锁定鼠标」，Shift 临时放开保留。**v48 成为此后所有版本固定的注入基座**（25 个适配条目）。

### 阶段三：导出与 HUD 反复期（v49 ~ v65）

* **v49**：新增 `MixinEntityPositionTrackerSafeLoad`、`MixinPipelinesNoPbo`、`MixinPboFrameCapturerGlReadPixels` —— 修「导出功能不可用」，根因是 `NoSuchMethodError: GLStateManager.glReadPixels(IIIIIIJ)V`（Angelica 改写后的 `GLStateManager` 没有 long 版重载）；导出获得成功验证（h264 / 1920×1080 / 60fps、1230 帧、零 `NoSuchMethodError`）。
* **v50**：摘掉从未生效的 `MixinPipelinesNoPbo`，只为消除误导性的 WARN；与 v49 实际执行的代码完全相同。
* **v51**：新增 `MixinEntityRendererNightVisionGuard` —— 修「回放中给自己换视角 + 夜视崩溃」（`getActivePotionEffect` 返回 `null`；返回 `1.0F` 而非 `0.0F`，因为下游有除法）。
* **v52**：重写 `MixinEntityPositionTrackerSafeLoad`，由「整段放弃」改为逐包 `@Redirect` 容错，真正修好关键帧。
* **v53**：重写 `MixinNetHandlerPlayClientSpawnGuard` —— `S0CPacketSpawnPlayer` 由**丢弃**改为**延迟重放**，修「玩家不渲染，切换维度之后才出现」。
* **v54 / v55**：新增 `MixinCameraEntityEnsureView` —— 每 tick 校正 `renderViewEntity`，让回放真正使用 ReplayMod 的相机实体视角（旁观玩家时不干预）。
* **v56**：重写 `MixinGuiReplayOverlayMouseVisible`，把 **Shift 完全还给上游**（退出旁观），鼠标切换只看 Ctrl —— 修「退不出旁观 + 前进方向错乱」。
* **v57 / v58**：新增 `MixinHudCachingReplayPassthrough`、`MixinHudCachingReplayNoCache`，让 HUD 事件能到达 handler（有益，但不解决血条问题，其后已回退）。
* **v59**：诊断探针，实证 GTNH 根本不发送 `RenderGameOverlayEvent.HEALTH`，因此原版血条藏不掉。
* **v60**：新增 `MixinHideHudInReplay`（整体取消 `renderGameOverlay`）—— 手确实消失了，但连带 ReplayMod 自己的时间轴一起消失。
* **v61 ~ v64**：改为按元素取消（血条／饥饿／护甲／氧气／经验／热键栏／Boss 血条／F3 文本），方向与预期不符，其后全部回退。
* **v65**：新增 `MixinNoFirstPersonHandInReplay`，改打确定存在的 `ItemRenderer.renderItemInFirstPerson`（自由相机取消、旁观玩家放行）；同时**摘掉 3 个 HUD 补丁**（`MixinHideHudElementsInReplay`、`MixinHudCachingReplayNoCache`、`MixinHudCachingReplayPassthrough`），只保留手部补丁。**v65 是本线的正式发布版本。**

---

## 产物形态

本工程的产物**不是**重新编译的 ReplayMod，而是：**官方 jar + 一层注入**。

| 项目 | 值 |
|---|---|
| 上游基线 | 官方 ReplayMod **1.7.10-2.5.2**，6101 个条目 |
| 新增条目 | **52** = **51 个 class**（`com/gtnh/replaycompat/**`）+ **1 个 mixin 配置**（`mixins.replaycompat.json`） |
| 替换的上游文件 | **3** —— `META-INF/MANIFEST.MF`、`mixins.recording.replaymod.json`、`assets/replaymod/lang/zh_CN.lang` |
| 删除的条目 | **0** |
| 产物条目总数 | 6101 → **6153**（v48 时期为 6126） |

51 个新增 class 的构成：

* **29** 个功能 mixin（全部登记在 `mixins.replaycompat.json`，另有若干只读探针源码留在工程内、不进产物）
* **9** 个辅助类（`util/`：7 个类 + 2 个内部类）
* **12** 个 ASM transformer 类 + **1** 个 coremod 入口点（`ReplayModTransformers`）

当前版本指纹：

| 版本 | 文件名 | 字节数 | SHA256 |
|---|---|---|---|
| **v93** | `replaymod-1.7.10-2.5.2-gtnh-v93-camspeed3.jar` | 10880577 | `670333B416C71BF437C8E7B6CED8300ADC947815EE078497C0DF39C877A275D0` |

复核方法（PowerShell，逐条目对比官方 jar 与产物 jar）：

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
function Ent($p){ $z=[System.IO.Compression.ZipFile]::OpenRead($p); $h=@{}
  foreach($e in $z.Entries){ $h[$e.FullName]=$e.Length }; $z.Dispose(); return $h }
$up = Ent '<official replaymod 1.7.10-2.5.2 jar>'
$new= Ent '<patched jar>'

$new.Keys | Where-Object { -not $up.ContainsKey($_) } | Sort-Object   # 新增 → 52
$up.Keys  | Where-Object { -not $new.ContainsKey($_) } | Sort-Object  # 删除 → 空
$new.Keys | Where-Object { $up.ContainsKey($_) -and $up[$_] -ne $new[$_] }  # 替换 → 3
```
