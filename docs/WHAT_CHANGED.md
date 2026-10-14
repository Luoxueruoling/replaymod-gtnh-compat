# 对原始 mod 的修改总账（v1 → v93）

> 工作区 `<workspace>`。目标：让 **ReplayMod 1.7.10-2.5.2（GTNH 整合版）** 在 **GTNH 2.9.0-beta-3** 上可用。
> 本文只回答一件事：**我们到底改了原始 mod 的什么**。每一版的来龙去脉在 `COMPATIBILITY_LOG.md`（§12.x）与 `PROJECT_HISTORY.md`。
> 文中所有数字都由脚本从 jar 里逐条目数出来，复核方法见 **§7**。

---

## 0. 一页速览

| 维度 | 数字 / 事实 |
|---|---|
| 被改的原始 jar | `replaymod-1.7.10-2.5.2-gtnh.jar`（上游 1.7.10-2.5.2 基线共 **6101** 个条目） |
| **删除的条目** | **0** —— 上游内容一个都没删 |
| **新增的条目** | **52** = **51 个 class**（`com/gtnh/replaycompat/**`）+ **1 个 mixin 配置**（`mixins.replaycompat.json`） |
| **改写的上游文件** | **3 个**：`META-INF/MANIFEST.MF`、`mixins.recording.replaymod.json`、`assets/replaymod/lang/zh_CN.lang` |
| 51 个新 class 的构成 | **29** 功能 mixin + **9** util（7 个类 + 2 个内部类）+ **12** ASM transformer + **1** coremod 入口 |
| 当前版本 | **v93**：`replaymod-1.7.10-2.5.2-gtnh-v93-camspeed3.jar`，`10880577` B，`SHA256 670333B416C71BF437C8E7B6CED8300ADC947815EE078497C0DF39C877A275D0` |

**一句话**：**没有 fork 上游源码重新编译**。做法是——保留上游 jar 的全部内容，用脚本把一层
`com.gtnh.replaycompat` 适配包**注入**进去，并把引导入口（manifest）、上游的一处 mixin 列表、
以及汉化文件改掉。

---

## 1. jar 结构层面的修改（最要紧的一处）

上游与 v93 的 `META-INF/MANIFEST.MF` 对比（逐行，取自 jar）：

| 键 | 上游（原始） | v93（我们） | 为什么 |
|---|---|---|---|
| `TweakClass` | `com.replaymod.core.tweaker.ReplayModTweaker` | **`org.spongepowered.asm.launch.MixinTweaker`** | GTNH 用 unimixins + Mixin 0.8.7；上游那个 tweaker 在 GTNH 上不可用（HANDOVER 坑 5、12.9） |
| `FMLCorePlugin` | `com.replaymod.core.LoadingPlugin` | **`com.gtnh.replaycompat.ReplayModTransformers`** | 上游 coremod 注册的两个 transformer 在 GTNH + RFB 上会 `ClassCircularityError`（见 §3.3） |
| `ForceLoadAsMod` | *（无）* | **`true`** | 强制按 mod 加载（GTNH 系 mod 的统一约定） |
| `MixinConfigs` | *（无）* | **一整行新增**（10 个配置，末尾追加 `mixins.replaycompat.json`） | 上游靠自家 tweaker 拉配置；现在必须显式声明 |
| `TweakOrder` | `0` | *（已移除）* | 与 MixinTweaker 组合后不再需要 |
| `FMLCorePluginContainsFMLMod` | `true` | `true` | 未改 |
| `FMLAT` | `replaymod_at.cfg` | `replaymod_at.cfg` | 未改（上游的访问转换器照旧生效） |

`MixinConfigs` 里列的 10 个（**顺序即加载顺序**）：

```
mixins.core.replaymod.json, mixins.recording.replaymod.json, mixins.render.replaymod.json,
mixins.replay.replaymod.json, mixins.compat.shaders.replaymod.json,
mixins.extras.playeroverview.replaymod.json, mixins.jgui.json,
mixins.compat.mapwriter.replaymod.json, mixins.render.blend.replaymod.json,
mixins.replaycompat.json          ← 我们的（最后加载）
```

⚠️ **故意不列** `mixins.nonmmlauncher.replaymod.json`（jar 里有这个文件，但它的伴生类
`ReplayModNonMMLauncher` 不在发布 jar 里，列了会 `ClassNotFoundException` —— HANDOVER 坑 6）。
另一个不进列表的是 `mixins.replaymod.refmap.json`（那是 refmap，不是配置）。

---

## 2. 被改写的 3 个上游文件

### 2.1 `META-INF/MANIFEST.MF`（206 B → 561 B）
见 §1。

### 2.2 `mixins.recording.replaymod.json`（909 B → 892 B）★ 唯一一处改动上游 mixin 列表

**唯一的变化：从 `client` 列表里摘掉了一行 `"MixinGuiScreen"`。**

```diff
     //#if MC<=10710
-    "MixinGuiScreen",
     "MixinS26PacketMapChunkBulk",
     //#endif
```

原因：那个 mixin 与 GTNH 的输入/物品栏行为冲突（「物品栏拖不动」那条主诉）。
注意这是**上游自带的** mixin，我们只是**不让它应用**，源码与 class 都还在 jar 里（HANDOVER 12.5 / 勘误区 #12）。

### 2.3 `assets/replaymod/lang/zh_CN.lang`（21805 B → 26395 B，407 行 → 470 行，**+63 个键**）

补全官方 1.7.10 汉化里缺失的条目（菜单/渲染设置/快捷键名等），例如：

```
replaymod.gui.rendersettings.addtoqueue=添加到队列
replaymod.gui.rendersettings.sphericalmetadata=注入球形元数据
replaymod.input.timekeyframe=时间关键帧
replaymod.gui.noquickmode=快速模式仅在 Minecraft %1$s 及以上版本可用
```

---

## 3. 新增的适配层：`com/gtnh/replaycompat/**`（51 个 class）

### 3.1 功能 mixin（29 个，全部登记在 `mixins.replaycompat.json`）

| Mixin | 目标类 | 注入点 | 解决什么 |
|---|---|---|---|
| `MixinGuiCustomReplayEntry` | `lumien.custommainmenu.gui.GuiCustom` | `func_73866_w_`@RETURN、`func_73863_a`@HEAD、`func_146284_a`@HEAD | 在 GTNH 的 Custom Main Menu 上**补回回放中心入口**（上游逻辑挂在永不被调用的 `GuiFakeMain` 上） |
| `MixinGuiScreenJGuiInputBridge` | `net.minecraft.client.gui.GuiScreen` | `func_146274_d`@HEAD | 把鼠标事件桥进 jGui，让主菜单上的「保存录像」面板**可点** |
| `MixinGuiBackgroundProcessesMainMenu` | `com.replaymod.core.gui.GuiBackgroundProcesses` | `onGuiInit`@HEAD(cancel) + `@Redirect` | ①让 CMM 环境下那个 guard 通过 ②**阻止面板重复挂载** |
| `MixinGuiScrollableWheelScale` | jGui `AbstractGuiScrollable` | `@ModifyVariable scroll` | 修正 lwjgl3ify(SDL3) 下滚轮量纲（±1 撞上 jGui 的 /4） |
| `MixinGuiReplayOverlayMouseVisible` | jGui `AbstractGuiOverlay` | `updateUserInputGui`@HEAD | 回放 overlay 的**鼠标可用性**（Ctrl 锁 + 每帧看护，12.50/12.52） |
| `MixinUserInputGuiScreenKeepMouseLock` | jGui `AbstractGuiOverlay$UserInputGuiScreen` | `@Redirect func_146281_b` | 输入屏关闭时**不再清掉鼠标锁**（治「鼠标锁循环」） |
| `MixinGuiPopupPreserveMouseLock` | jGui `AbstractGuiPopup` | `@Redirect open` | 弹窗打开时**保住**玩家的 Ctrl 锁 |
| `MixinMinecraftShiftEscReset` | `net.minecraft.client.Minecraft` | `func_71385_j`@HEAD、`func_71381_h`@HEAD、**`func_71407_l`@HEAD** | Shift+ESC 强制复位；★最后那个是**每帧看护的宿主**（滚轮/速度/普查都挂在它上面） |
| `MixinUserInputGuiScreenWheelCameraSpeed` | 同上 UserInputGuiScreen | `func_146274_d`@HEAD | **v85 起降级为探针**（真正的滚轮逻辑已搬到每帧 `runTick`） |
| `MixinEntityClientPlayerMPWheelCommand` | `net.minecraft.client.entity.EntityClientPlayerMP` | `func_71165_d`@HEAD(cancel) | 游戏内指令 `/replaycompat wheel <1-120>`（纯客户端，消息不发服务器） |
| `MixinCameraEntityEnsureView` | `com.replaymod.replay.camera.CameraEntity` | `func_70071_h_`@TAIL | 让回放**真的用 ReplayMod 的相机实体**当视角（治「一进回放就是第一人称/看见自己的手」） |
| `MixinNoFirstPersonHandInReplay` | `net.minecraft.client.renderer.ItemRenderer` | `func_78440_a` 等 4 个@HEAD(cancel) | 回放里**不渲染第一人称的手** |
| `MixinEntityRendererNightVisionGuard` | `net.minecraft.client.renderer.EntityRenderer` | `func_82830_a`@HEAD(cancel) | 修「换视角 + 夜视」崩溃 |
| `MixinPacketListenerSaveGuard` | `com.replaymod.recording.packet.PacketListener` | `save`@HEAD(cancel) | 录制器已关闭的线程池**不再被塞包**（干掉 1343 条 `RejectedExecutionException`） |
| `MixinS0CPacketSpawnPlayerProfileGuard` | `net.minecraft.network.play.server.S0CPacketSpawnPlayer` | `func_148840_b`@HEAD | 修**玩家条目写不进录像**（皮肤属性 `signature` 为 null ⇒ 写包 NPE ⇒ 整包丢失） |
| `MixinOpenGuiHandlerConnectingGuard` | `cpw.mods.fml.common.network.internal.OpenGuiHandler` | `channelRead0`@HEAD(cancel) | 世界还没建好时**丢弃"打开 GUI"包**（治「进服务器就断连」= Salis Arcana 判空缺陷） |
| `MixinNetHandlerPlayClientNullWorldGuard` | `net.minecraft.client.network.NetHandlerPlayClient` | **14 个** `func_1472xx_a`@HEAD(cancel) | 回放加载期 `theWorld == null` 时**暂扣**会 NPE 的包（不丢，交给下一个 mixin） |
| `MixinNetHandlerPlayClientSpawnGuard` | 同上 | `func_147237_a`@HEAD(cancel) | `S0CPacketSpawnPlayer` 在无世界时**延迟**（不是丢弃，否则回放里玩家永远不出现） |
| `MixinNetworkManagerDeferredFlush` | `net.minecraft.network.NetworkManager` | `func_74428_b`@HEAD | 世界就绪后把上面暂扣的包**重新塞回接收队列** |
| `MixinEtFuturumPlayer` | `ganymedes01.etfuturum.storage.EtFuturumPlayer` | `init`@HEAD(cancel) | Et Futurum Requiem 的 NPE（注意类名大小写 `EtFuturum`，曾因此静默失效） |
| `MixinAe2PartAccessorNullMop` | `appeng.integration.modules.waila.part.PartAccessor` | `getMaybePart`@HEAD(cancel) | AE2 的 Waila 集成**没判空** ⇒ 点界面就崩 |
| `MixinPipelinesNoPbo` | `com.replaymod.render.rendering.Pipelines` | `@Redirect`（构造抓帧器） | **导出白屏**：绕开 PBO 抓帧路径 |
| `MixinPboFrameCapturerGlReadPixels` | `com.replaymod.render.capturer.PboOpenGlFrameCapturer` | `@Redirect` ×2 | 修 Angelica 改写后的 `glReadPixels(..., long)` 反射桥 |
| `MixinSquashFilterSafeBulkRead` | `com.replaymod.replaystudio.filter.SquashFilter` | `onPacket` `@Redirect` ×3 | 保存/编辑录像时**逐包容错**（三条读取入口都要护住） |
| `MixinMarkerProcessorSafeFiltering` | `com.replaymod.editor.gui.MarkerProcessor` | `apply` `@Redirect` | 编辑器最后一道防线：坏包不许杀游戏 |
| `MixinEntityPositionTrackerSafeLoad` | `com.replaymod.replaystudio.util.EntityPositionTracker` | `load`/`loadFromPacketData` `@Redirect` ×3 | 畸形包不再弹崩溃报告 |
| `MixinGuiTimelineViewportGuard` | jGui `AbstractGuiTimeline` | `getTimeAt`@HEAD | 时间轴视口**自愈**（陈旧 offset） |
| `MixinGuiTimelineClickProbe` | 同上 | `onClick`@HEAD | **只读探针**（已结案：大时间轴 = 上游重放机制） |
| `MixinMouseUtilsCoordinateProbe` | jGui `MouseUtils` | `getMousePos`@RETURN | **只读探针**（已结案：排除坐标量纲） |

### 3.2 util（7 个类 + 2 个内部类）

| util | 作用 |
|---|---|
| `ReplayMouseState` | 回放鼠标补丁之间的共享状态：Ctrl 锁、每帧看护、Shift+ESC 复位、`/replaycompat` 指令、实体普查、滚轮→速度换算、录制器存活判断 |
| `NullWorldGuard` | 「世界还没建好」的暂扣队列 + 反射助手 |
| `LwjglMouseBridge` | 在 lwjgl3ify(SDL3) 客户端上**按游戏自己的方式读鼠标**（必须指 `org.lwjglx.*`） |
| `ReplayMouseInput` | 把中键点击送进回放 overlay |
| `ReplayKeyBridge`（+`$1`/`$Handler`） | 鼠标解锁时**旁路补喂 `KeyBinding`**，救回 ReplayMod 自己的快捷键（含 M 加标记） |
| `ReplayCameraSpeed` | 记住玩家调好的相机速度，**跨维度 / 退出旁观后恢复** |
| `ReplaySpawnProfile` | 把 ReplayMod 自造的玩家 spawn 包**净化成可写**（null 字符串 → 空串） |

### 3.3 coremod 与 ASM transformer（13 个 class）

| 类 | 作用 |
|---|---|
| `ReplayModTransformers`（manifest 的 `FMLCorePlugin`） | 重新注册上游那两个 transformer，但**避开**让原厂 jar 在 GTNH 上不可用的写法 |
| `asm/GLErrorTransformer`（+4 内部类） | 上游同名类的「具名静态 visitor」重写版，能在 GTNH + RFB 上注册 |
| `asm/GLStateTrackerTransformer`（+5 内部类） | 同上：把 GL 开关调用重定向到 `GLStateTracker.hook_glEnable/hook_glDisable` |
| `asm/Launcher` | 提前预链接上面那些内部类，避免 transform 期的 `ClassCircularityError` |

> ⚠️ 这两个 transformer 的**注入本身在 GTNH 上仍然不可用**（会让物品栏拖不动 / 启动崩），
> 所以它们的注册是 **opt-in**：默认不注册，只保留代码与入口。这是「物品栏拖不动」那轮
> 单变量实验的结论（HANDOVER 勘误区补充 12~16）。

### 3.4 mixin 配置：`mixins.replaycompat.json`（新增文件）

- `package: com.gtnh.replaycompat.mixin`、`refmap: mixins.replaymod.refmap.json`（复用上游 refmap）、`minVersion 0.8.7`、`required: false`
- `client` 列表 **29 项**（= §3.1 的 29 个）
- **不含** 4 个只读探针（`MixinAbstractGuiClickableProbe`、`MixinAbstractGuiOverlayInputProbe`、`MixinMouseInputEventProbe`、`MixinVanillaGuiScreenEventHandler`）—— 它们的源码留在工程里当工具，**不进产物**

---

## 4. 版本链（改了哪些版本、每版动的是哪一块）

> 完整成因/证据在 `COMPATIBILITY_LOG.md`；这里只给"改了原始 mod 的什么"。

### 4.1 起步期（v1 ~ v12）：coremod 与 ASM transformer

把仓库里留档的**每一个** jar 两两对比（脚本见 §7），实测结果是：

| 版本 | 适配层的变化 |
|---|---|
| v2 | **适配层还是空的**（jar 里 0 个 `com/gtnh/replaycompat` 条目）|
| **v4** | ★ **第一个 class = `ReplayModTransformers`**（manifest 的 `FMLCorePlugin` 开始指向它）|
| v5 / v6 | 只改它 —— 就是"transformer 一注册游戏就崩"的那一轮反复 |
| **v7** | ★ **一次性加入 12 个 asm class**：两个 transformer 的「具名静态 visitor」重写版（含 9 个内部类）+ `Launcher` |
| v8 ~ v11 | 反复调 `GLStateTrackerTransformer` / `GLErrorTransformer` 的字节码（`4040 → 3639 → 3724 → 2723` 一次收缩）|
| v12 | 无变化 |

这一期的结论（已写进勘误区）：**「物品栏拖不动」的根因是 coremod 的 ASM transformer**，
与 mixin priority / TweakClass / TweakOrder 全都无关；修法是**把两个 transformer 变成 opt-in**
（默认不注册）+ **从上游配置里摘掉 `MixinGuiScreen`** —— v6 是第一个「能拖 + 能启动」的版本。

### 4.2 功能铺开期（v13 ~ v48）：从第一个 mixin 到"基座"

| 版本 | 新增到适配层的 |
|---|---|
| **v13** | ★ **第一个功能 mixin：`MixinGuiCustomReplayEntry`**（主菜单回放入口）|
| v14 ~ v19 | 只改它（**6 次**迭代 —— CMM 那个入口反反复复）|
| v20 | `MixinEtFuturumPlayer` |
| v22 | `MixinNetHandlerPlayClientSpawnGuard` |
| v23 | `MixinGuiScrollableWheelScale` |
| v24 | `MixinGuiBackgroundProcessesMainMenu` |
| v25 | `MixinVanillaGuiScreenEventHandler`（探针）|
| v26 | `MixinMouseInputEventProbe`（探针）|
| v27 | `MixinGuiScreenJGuiInputBridge` |
| v28 | `MixinAbstractGuiClickableProbe`（探针）|
| **v30-clean** | **摘掉 3 个探针**（`AbstractGuiClickableProbe` / `MouseInputEventProbe` / `VanillaGuiScreenEventHandler`）|
| v31 | `MixinNetHandlerPlayClientNullWorldGuard`、`MixinNetworkManagerDeferredFlush`、`NullWorldGuard`（★ 当时放进了 **mixin 包**）|
| **v32** | ★ `NullWorldGuard` **从 `mixin/` 搬到 `util/`** —— mixin 包里的类不能被直接引用（HANDOVER 坑 42），这是那条坑的原始现场 |
| v34 / v35 | 加过又撤掉 `MixinAbstractGuiOverlayInputProbe`；v35 起 `MixinGuiReplayOverlayMouseVisible` 进产物 |
| v36 ~ v44 | 只改 `MixinGuiReplayOverlayMouseVisible`（**9 次**迭代 —— 鼠标可用性那一整轮）|
| v45 | `ReplayMouseInput` |
| **v47** | ⚠️ **一次回退版**：摘掉 5 个（`MixinGuiReplayOverlayMouseVisible`、`MixinNetHandlerPlayClientNullWorldGuard`、`MixinNetworkManagerDeferredFlush`、`NullWorldGuard`、`ReplayMouseInput`）|
| **v48** | 全部恢复 ⇒ **v48 就是此后一直沿用的"基座"**（`25` 个适配条目）|

### 4.3 v49 ~ v65：导出、夜视、相机、HUD 的反复（v65 = 正式发布版）

| 版本 | 新增/变化 |
|---|---|
| **v49** | `MixinEntityPositionTrackerSafeLoad`、`MixinPboFrameCapturerGlReadPixels`、`MixinPipelinesNoPbo`（导出/渲染那一轮）|
| v50 | **摘掉** `MixinPipelinesNoPbo`（那一版没成）|
| v51 | `MixinEntityRendererNightVisionGuard` |
| v52 | 改 `EntityPositionTrackerSafeLoad` |
| **v53** | 改 `NetHandlerPlayClientSpawnGuard`：**延迟**而不是丢弃（"切维度后才出现玩家"的修法）|
| v54 / v55 | `MixinCameraEntityEnsureView`（相机视角）|
| v56 | 改 `GuiReplayOverlayMouseVisible` |
| v57 / v58 | `MixinHudCachingReplayPassthrough`、`MixinHudCachingReplayNoCache` |
| v59 | `MixinDiagOverlayReached`（诊断）|
| v60 | `MixinHideHudInReplay`（并撤掉 v59 那个诊断）|
| v61 | `MixinHideHudElementsInReplay`、`MixinNoFirstPersonHandInReplay`（并撤掉 `MixinHideHudInReplay`）|
| v62 ~ v64 | 只改上面两个 |
| **v65** | ★ **摘掉 3 个 HUD 补丁**（`HideHudElementsInReplay`、`HudCachingReplayNoCache`、`HudCachingReplayPassthrough`），只留 `MixinNoFirstPersonHandInReplay`。**v65 = 本线的正式发布版**：`release\GTNH-ReplayMod-v65\`，`10837941` B / `907B72B8…45FCC` |

> 完整的逐版差异（v2 ~ v93，共 92 个留档 jar）已存成
> `.selftest\jar-version-changelog.txt`，本文只摘关键节点。

### 4.4 v66 ~ v93（正式发布之后的改动，逐版实测）

> `+` = 新增 class，`~` = 只改了已有 class；全部由 §7 的脚本从留档 jar 实测得出。

| 版本 | 适配层的变化 | 解决什么 / 现场状态 |
|---|---|---|
| **v66** | **+** `MixinPipelinesNoPbo`（v49 加过、v50 摘掉，这版重新加回）｜**~** `MixinGuiReplayOverlayMouseVisible` | **导出白屏** ✅ 338 环境「渲染没问题」 |
| **v67** | **+** `MixinAe2PartAccessorNullMop` | 点界面崩溃 ✅「没有崩」 |
| **v68** | **+** `MixinGuiPopupPreserveMouseLock`、**+** `util/ReplayMouseState` | 鼠标锁循环（前半）|
| **v69** | **+** `MixinUserInputGuiScreenKeepMouseLock` | 鼠标锁循环 ✅ `RELEASED` 136→3 |
| **v70** | **+** `MixinUserInputGuiScreenWheelCameraSpeed` | 滚轮第 1 次尝试 ❌ |
| **v71** | **+** `MixinMinecraftShiftEscReset` | Shift+ESC 复位 |
| **v72** | ~ `MixinGuiReplayOverlayMouseVisible` | 一致性检查 ❌（抖动清零）|
| **v73** | **+** `MixinSquashFilterSafeBulkRead` | 保存录像崩溃（第一版容错）|
| **v74** | **+** `MixinMouseUtilsCoordinateProbe` | 只读探针（排除坐标量纲）|
| **v75** | **+** `util/LwjglMouseBridge` | 反射改 `org.lwjglx.*`（坑 57）|
| **v76** | ~ `MixinMinecraftShiftEscReset`、~ `ReplayMouseState` | 抑制抢鼠标 ✅ `no input screen` 189→15 |
| **v77 / v78** | ~ `WheelCameraSpeed`；**+** `MixinGuiTimelineClickProbe`（v78）| 滚轮仍无效；时间轴探针 |
| **v79** | **+** `MixinGuiTimelineViewportGuard` | 视口自愈（无害保留）|
| **v80** | ~ `MixinMinecraftShiftEscReset`、~ `ReplayMouseState` | 实体普查 ✅ 定案「人消失」= 相机视角 |
| **v81** | ~ `WheelCameraSpeed`、~ `LwjglMouseBridge` | 滚轮改 `getDWheel()` ❌（被 MouseTweaks 抢读）|
| **v82** | **+** `util/ReplayKeyBridge`（+2 内部类）| 救回 ReplayMod 快捷键（含 M 标记）✅ |
| **v83** | ~ `MixinGuiTimelineClickProbe`（改挂 `onClick`）| ✅ 抓到 4 次真实点击 |
| **v84** | ~ `MixinUserInputGuiScreenWheelCameraSpeed` | 改用 `totalScrollAmount` 差分 ❌ |
| **v85** | **+** `MixinMarkerProcessorSafeFiltering`；~ `SquashFilterSafeBulkRead`（补第三条读取入口）；~ `ReplayMouseState`（滚轮搬进每帧 `runTick`）；**~ `WheelCameraSpeed` 4739 → 1350（降级为纯探针）** | ✅ 现场双验证（滚轮 + 标记崩溃）|
| **v86** | ~ `ReplayMouseState`（灵敏度 ×5）| ✅ Tester_B使用 |
| **v87** | **+** `MixinEntityClientPlayerMPWheelCommand` | 游戏内指令 `/replaycompat wheel <n>` |
| **v88** | **+** `MixinPacketListenerSaveGuard` | 1343 条 `RejectedExecutionException` → **0** ✅ |
| **v89** | **+** `MixinOpenGuiHandlerConnectingGuard`；~ `util/NullWorldGuard` | 「进服务器就断连」⚠️ 补丁**至今未被真实触发** |
| **v90** | **+** `MixinS0CPacketSpawnPlayerProfileGuard`、**+** `util/ReplayCameraSpeed`、**+** `util/ReplaySpawnProfile` | 两个新功能 ⚠️ **都没生效** |
| **v91** | ~ `util/ReplaySpawnProfile`（6030 → 8971：真凶是皮肤属性 `signature` 为 null）| ✅ 现场逐字命中 |
| **v92** | ~ `util/ReplayCameraSpeed`、~ `ReplayMouseState`（加"必须出现"的诊断行）| ⚠️ 诊断行抓出真因 |
| **v93** | ~ `util/ReplayCameraSpeed`（跳过 `static`、按字段类型定步长）| ✅ Tester_B「目前没问题了」 |

**产物规模**：v48 = **25** 个适配条目 → **v93 = 52**（`6126 → 6153`）。

#### ⚠️ 关于 HANDOVER 正文里 v1~v65 的"mixin 计数"：**以 jar 实测为准**

HANDOVER 正文出现过至少两套互相矛盾的计数（例如 §12.23 说 v42 = v30-release **+9 个 mixin**，
而 §12.19 说 v30-release 只有 **6 个**；其后 v53 / v56 / v60 / v65 又各有 "v48 + N" 的写法且互不吻合）。
**这些口径后来对不上**（清单见 `.selftest\v1-v65-changelog.md` 第 5 节）。
本文 §4.1~§4.4 的数字**全部来自留档 jar 的逐条目实测**；与正文计数冲突时，**以本文为准**。

---

## 5. 我们**没有**改的东西（边界）

- 上游 6101 个条目**零删除**；
- 上游自带的 **9 个 mixin 配置**全部保留、且仍按上游列表加载（只从中摘掉 1 个 mixin 名）；
- 上游自己的 `ReplayModTweaker` / `LoadingPlugin` **代码仍在 jar 里**（只是 manifest 不再指向它们）；
- `mixins.nonmmlauncher.replaymod.json` 与伴生类的关系保持原样（只是不进 manifest 列表）；
- **没有**动上游的 `replaymod_at.cfg`；
- 已裁定**不做/接受**的：导出不支持深度图、比原版略慢、鼠标可见时 WASD 不驱动相机、
  回放里显示原版血条、往回跳会「飞」一下（上游机制）。

---

## 6. 服务端那条线（独立的另一份修改）

`<workspace>\ServerReplay-1.7.10\` 是**另一条独立工程**（不是改 ReplayMod，而是参考 `senseiwells/ServerReplay`
的设计，为 1.7.10 **重新实现**服务端录制器）：

- 形态：**纯 mixin 服务端 mod**（刻意不做 `@Mod` 类 —— GTNH 的 JarJar `LoaderV2` 会让引用了
  网络类的 mod 类在 transition 阶段 `NoClassDefFoundError`）；
- 产物两版：`-client.jar`（不含 shade，客户端/单人用）与含 shade 的主 jar（专用服务端用）——
  **两个都装会类遮蔽**（回放画面错乱）；
- 当前状态：**未部署**；核心缺口 = **实体包漏录**（采集点空窗 + 三类高频包丢失）。

---

## 7. 怎么复核这份总账（照抄即可）

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
function Ent($p){ $z=[System.IO.Compression.ZipFile]::OpenRead($p); $h=@{}
  foreach($e in $z.Entries){ $h[$e.FullName]=$e.Length }; $z.Dispose(); return $h }
$up = Ent '<workspace>\tools\replaymod-1.7.10-2.5.2.jar'                    # 上游原始
$v93= Ent '<workspace>\replaymod-1.7.10-2.5.2-gtnh-v93-camspeed3.jar'       # 我们的最新版

# 新增了什么
$v93.Keys | Where-Object { -not $up.ContainsKey($_) } | Sort-Object
# 删除了什么（应为空）
$up.Keys  | Where-Object { -not $v93.ContainsKey($_) } | Sort-Object
# 改写了什么（应为 3 个）
$v93.Keys | Where-Object { $up.ContainsKey($_) -and $up[$_] -ne $v93[$_] }
```

再配两条：
- manifest 对比：解压两个 jar 的 `META-INF/MANIFEST.MF` 直接看（§1 的表就是这么来的）；
- 上游 mixin 列表的改动：`mixins.recording.replaymod.json` 里搜 `MixinGuiScreen`（v93 里**没有**）。
