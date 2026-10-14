# ReplayMod GTNH 兼容层

> 让 **ReplayMod 1.7.10-2.5.2** 真正跑在 **GT New Horizons 2.9.0-beta-3** 上 ——
> 即 Minecraft 1.7.10，运行于 lwjgl3ify（LWJGL 3 / SDL3，无 GLFW）、Angelica、Mixin 0.8.7
> （unimixins）以及一个约 340 个 mod 的整合包之上。

[English](README.md) | **简体中文**

---

## 这是什么

一个**注入式补丁工程**：它**不包含、不 fork、也不重新编译** ReplayMod。

```
src/      补丁层  com.gtnh.replaycompat
          29 个 mixin + 7 个辅助类 + 1 个 ASM/coremod 入口点
          用裸 javac 编译（无 reobf、无 Gradle）
overlay/  本补丁在官方 jar 内所“替换”的文件
tools/    build.ps1 + apply.ps1 —— 把官方 jar 变成可用的 jar
```

官方 ReplayMod **1.7.10-2.5.2** 的 jar 由你自行提供。本仓库不重新分发任何上游内容。

## 上游与血缘

这是一个**补丁层，不是 fork**。它针对官方的 **ReplayMod 1.7.10-2.5.2** 发布 jar：

* 上游：<https://github.com/ReplayMod/ReplayMod> —— MIT，作者 CrushedPixel 与 johni0702。
* 1.7.10 是 ReplayMod 在 2.6.0 之前支持的最后一个 Minecraft 版本，而上游**特意保留了 1.7.10 的源码**
  （`versions/1.7.10/`）—— 他们 `root.gradle.kts` 里的原话是
  *"We dropped 1.7.10 with the Gradle 7 update but still kept its source in case someone…"*。
* **为什么做补丁而不是 fork？** 上游的 1.7.10 构建链在干净检出上已经跑不起来（2017 年那份要
  Gradle 2.11 与 `ForgeGradle:1.2-SNAPSHOT`，两者早已消失），而本项目**每一处改动都是 Mixin 或小工具类**
  —— fork 之后里面装的也是同一批代码，只是换成另一套工具链编译。补丁+注入的方式让产物**可复现**
  （见 [`docs/BUILDING.md`](docs/BUILDING.md)）、**可审计**：`overlay/` 列出了唯一被替换的上游文件，
  而且**从不删除 jar 里的任何东西**。

完整的血缘记录 —— 精确的上游版本与 jar 的 SHA-256、上游 1.7.10 源码的位置、
以及真要做源码级 fork 需要付出什么 —— 见 [UPSTREAM.md](UPSTREAM.md)。
## 为什么需要它

ReplayMod 的 1.7.10 分支是照着「纯 LWJGL 2 客户端 + 原版主菜单」写的。GTNH 2.9.0 是完全不同的世界：经 lwjgl3ify 接入的 LWJGL 3、Angelica 的 GL 状态管理器、Custom Main Menu，以及数百个 ReplayMod 从未设想过的 mod。原样直接跑，要么启动失败，要么以十几种不同方式出问题。

## 它修了什么

| 范畴 | GTNH 上的现象 | 修法 |
|---|---|---|
| 启动 | 原厂 coremod 的 ASM transformer 会让游戏不可用（物品栏拖不动，或者崩溃循环） | 两个 transformer 都用「具名静态 visitor」重新实现，并改为**可选启用（opt-in）** |
| 主菜单 | 完全没有回放入口（上游挂载的是一个永远不会显示的界面） | `MixinGuiCustomReplayEntry` |
| 主菜单 | “正在保存录像”面板画出来了，但什么都点不动 | `MixinGuiScreenJGuiInputBridge`、`MixinGuiBackgroundProcessesMainMenu` |
| 输入 | 鼠标锁死循环；点过时间轴之后鼠标就卡住 | `MixinGuiReplayOverlayMouseVisible`、`MixinUserInputGuiScreenKeepMouseLock`、`MixinGuiPopupPreserveMouseLock`、`MixinMinecraftShiftEscReset` |
| 输入 | jGui 界面里滚轮失效（SDL3 报 ±1，而 jGui 按 /4 处理） | `MixinGuiScrollableWheelScale` |
| 输入 | 回放相机速度无法用滚轮调整 | 滚轮 → `InputReplayTimer.handleScroll`，每 tick 驱动一次 |
| 输入 | 鼠标解锁时，ReplayMod 自己的快捷键（播放/暂停、**M** 加标记 等）全部失效 | `ReplayKeyBridge` |
| 回放 | 打开回放就掉进第一人称，还渲染出自己的手 | `MixinCameraEntityEnsureView`、`MixinNoFirstPersonHandInReplay` |
| 回放 | 客户端世界尚未创建时加载回放会崩 | `MixinNetHandlerPlayClientNullWorldGuard`、`MixinNetHandlerPlayClientSpawnGuard`（延迟而非丢弃）、`MixinNetworkManagerDeferredFlush` |
| 回放 | 每次切换维度或退出旁观玩家，相机速度都被重置 | `ReplayCameraSpeed` |
| 回放 | 旁观某个玩家 + 夜视会崩 | `MixinEntityRendererNightVisionGuard` |
| 渲染 | 视频导出出来是白屏／空白帧 | `MixinPipelinesNoPbo`、`MixinPboFrameCapturerGlReadPixels` |
| 录制 | 录出来的录像里**没有录制玩家自己的条目** | `MixinS0CPacketSpawnPlayerProfileGuard`、`ReplaySpawnProfile` |
| 录制 | 连接断开后日志被 `RejectedExecutionException` 刷屏 | `MixinPacketListenerSaveGuard` |
| 剪辑 | 保存／应用标记时，遇到一个畸形包就崩 | `MixinSquashFilterSafeBulkRead`、`MixinMarkerProcessorSafeFiltering` |
| 杂项 | AE2 + Waila 集成一点就崩 | `MixinAe2PartAccessorNullMop` |
| 杂项 | 回放期间 Et Futurum Requiem 空指针 | `MixinEtFuturumPlayer` |
| 多人 | 网络握手阶段断连 | `MixinOpenGuiHandlerConnectingGuard` |

每个文件都带有一段很长的类级 javadoc，解释**字节码层面**的成因以及它是如何被验证的 —— 真正的文档在那里。

## 环境要求

* **JDK 8** —— 补丁层面向 Java 8（也就是 1.7.10 的运行时）
* **ReplayMod 1.7.10-2.5.2** 官方 jar（由你提供）
* 一份带 MCP 名称的 1.7.10 Forge **dev** jar，用于编译
  （例如 RetroFuturaGradle / RFG 的产物 —— 本工程是以
  `recompiled_minecraft-1.7.10.jar` 为基准编译的）
* 编译期依赖库：Forge universal、Netty、Gson、Guava、ASM 9.x，
  以及唯一那个 AE2 mixin 所需的 Applied Energistics 2

## 构建与注入

```powershell
# 1) compile the patch layer
.\tools\build.ps1 `
    -JavaHome         "C:\Program Files\Java\jdk-1.8" `
    -MinecraftDevJar  <mcp-named minecraft dev jar> `
    -ReplayModJar     <official replaymod 1.7.10-2.5.2 jar> `
    -ExtraJars        <forge universal>,<netty-all>,<gson>,<guava>,<asm-9.x>,<asm-tree>,<asm-commons>,<ae2>

# 2) turn the official jar into the patched jar
.\tools\apply.ps1 `
    -BaseJar  <official replaymod 1.7.10-2.5.2 jar> `
    -MixinOut .\mixin-out `
    -OutJar   .\dist\replaymod-1.7.10-2.5.2-gtnh.jar
```

`build.ps1` 先编译 `util/`，再编译 `overlay/mixins.replaycompat.json` 里列出的每一个 mixin，
并逐项检查退出码；只要有**任何**一个失败，就在写盘之前中止。
`apply.ps1` 会校验注入的 class 数量，并打印一份清单。

两个脚本都是纯 PowerShell + `javac`；在 Linux/macOS 上请使用
`docs/BUILDING.md` 中给出的等价 `javac`/`jar` 命令。

## 上游 jar 里到底改了什么

*（对照官方 1.7.10-2.5.2 jar 实测，该 jar 共 6101 个条目。）*

**不删除任何内容。** 新增 52 个条目，替换 3 个文件：

### 1. `META-INF/MANIFEST.MF` —— 引导入口点

| 键 | 上游 | 打过补丁后 |
|---|---|---|
| `TweakClass` | `com.replaymod.core.tweaker.ReplayModTweaker` | `org.spongepowered.asm.launch.MixinTweaker` |
| `FMLCorePlugin` | `com.replaymod.core.LoadingPlugin` | `com.gtnh.replaycompat.ReplayModTransformers` |
| `ForceLoadAsMod` | *（无）* | `true` |
| `MixinConfigs` | *（无）* | 一行列出 10 个配置（我们的排在最后） |
| `TweakOrder` | `0` | *（已移除）* |

`mixins.nonmmlauncher.replaymod.json` 是**故意不列**的：它的伴生类没有随发布 jar 一起提供。

### 2. `mixins.recording.replaymod.json` —— 删掉一行

```diff
     //#if MC<=10710
-    "MixinGuiScreen",
     "MixinS26PacketMapChunkBulk",
     //#endif
```

那个上游 mixin 与 GTNH 的输入处理相冲突（正是它导致物品栏拖不动）。
该类仍留在 jar 里，只是不再被应用。

### 3. `assets/replaymod/lang/zh_CN.lang` —— 补上 63 个缺失的键

官方 1.7.10 的中文翻译并不完整；这里补齐了缺口
（渲染设置、按键绑定名称、错误提示 等）。

### 4. 新增：`com/gtnh/replaycompat/**`（51 个 class）+ `mixins.replaycompat.json`

* **29** 个功能 mixin（全部登记在 `mixins.replaycompat.json` 中）
* **9** 个辅助类（`util/`：7 个类 + 2 个内部类）
* **12** 个 ASM transformer 类 + **1** 个 coremod 入口点（`ReplayModTransformers`）

## 如何自行验证以上一切

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
function Ent($p){ $z=[System.IO.Compression.ZipFile]::OpenRead($p); $h=@{}
  foreach($e in $z.Entries){ $h[$e.FullName]=$e.Length }; $z.Dispose(); return $h }

$official = Ent '<official replaymod jar>'
$patched  = Ent '<patched jar>'

$patched.Keys | Where-Object { -not $official.ContainsKey($_) }   # added   → 52
$official.Keys | Where-Object { -not $patched.ContainsKey($_) }    # removed → 0
$patched.Keys | Where-Object { $official.ContainsKey($_) -and $official[$_] -ne $patched[$_] }
                                                                  # changed → the 3 files above
```

## 文档

| 文档 | 内容 |
|---|---|
| [`docs/BUILDING.md`](docs/BUILDING.md) | 分步构建、完整依赖清单、常见报错、以及 Linux/macOS 的等价命令 |
| [`docs/KNOWN_ISSUES.md`](docs/KNOWN_ISSUES.md) | **安装前请先读** —— 哪些还没修、哪些没验证过、哪些其实不是 bug |
| [`docs/IMPLEMENTATION_NOTES.md`](docs/IMPLEMENTATION_NOTES.md) | 代码为什么这么写 —— 约 20 条反直觉的规则，含三次一开始判断错的诊断 |
| [`docs/COMPATIBILITY_LOG.md`](docs/COMPATIBILITY_LOG.md) | 完整现场记录：每个现象、字节码证据、以及修没修 |
| [`docs/WHAT_CHANGED.md`](docs/WHAT_CHANGED.md) | 对上游 jar 改动内容的完整清单 |
| [`docs/PROJECT_HISTORY.md`](docs/PROJECT_HISTORY.md) | 工程状态与版本指针 |
| [`docs/CHANGELOG.md`](docs/CHANGELOG.md) | 逐版变更记录 |
| [`docs/RELEASE_NOTES.md`](docs/RELEASE_NOTES.md) | 当前版本的发布说明 |
| [`docs/VERIFICATION_GUIDE.md`](docs/VERIFICATION_GUIDE.md) | 如何在游戏里复验、要收集哪些证据 |

## 许可与致谢

* **本仓库：MIT** —— 见 [`LICENSE`](LICENSE)。
* **作者 / 维护者：Luoxueruoling。** 所有游戏内实测、驱动每一次修复的现场反馈、以及"哪些值得修"的取舍，都来自这一侧。
* **补丁层本身由一个 AI 编码代理实现**（DeepSeek Harness，`deepseek-flash`），包括字节码考证 ——
  对 MCP 名版与 SRG 名版两个 Minecraft jar 做 `javap`、以及对全部留档构建做逐条目的 jar 差异对比。
  它最初有几次判断是错的、后来靠实测纠正；那些错误被特意保留在笔记里。
* **工具链：** Java 8 `javac` · **Mixin 0.8.7 / UniMixins** · **ASM 9.x** ·
  **RetroFuturaGradle**（1.7.10 开发用 jar）· `javap` 与 JDK 的 zip API 用于验证 · PowerShell 写构建脚本。
* **ReplayMod 同样是 MIT**，作者为 **CrushedPixel** 与 **johni0702** ——
  <https://github.com/ReplayMod/ReplayMod>。上游 jar 自带 `LICENSE.txt`、
  `NOTICE.txt` 以及第三方声明（ASM 等，Apache-2.0）；见 [`NOTICE.md`](NOTICE.md)。
* 上游 jar **不在本仓库重新分发**。如果你要再分发构建出来的 jar，请把上游的声明文件保留在里面。
* 与 Mojang、GTNH 团队以及 ReplayMod 作者均无隶属关系。

## 状态

这是一个**能用但没完工**的适配，目标是一个上游已经放弃 1.7.10 维护的分支。它足以在 GTNH 上录制、
回放、剪辑与导出 —— 但有前提，而且有几件事在客户端侧根本无法解决。要认真依赖它之前，请先读
[`docs/KNOWN_ISSUES.md`](docs/KNOWN_ISSUES.md)。