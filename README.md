# ReplayMod GTNH Compatibility Layer

> Makes **ReplayMod 1.7.10-2.5.2** actually work on **GT New Horizons 2.9.0-beta-3** —
> Minecraft 1.7.10 running on lwjgl3ify (LWJGL 3 / SDL3, no GLFW), Angelica, Mixin 0.8.7
> (unimixins) and a ~340 mod pack.

**English** | [简体中文](README.zh_CN.md)

---

## What this is

An **injection-style patch project**: it does **not** contain, fork, or recompile ReplayMod.

```
src/       the patch layer  com.gtnh.replaycompat
           29 mixins + 7 helper classes + an ASM/coremod entry point
           compiled with plain javac (no reobf, no Gradle)
overlay/   the files this patch REPLACES inside the official jar
tools/     build.ps1 + apply.ps1 — turn the official jar into a working one
```

You supply the official ReplayMod **1.7.10-2.5.2** jar. Nothing upstream is redistributed here.

## Upstream & lineage

This is a **patch layer, not a fork**. It targets the official **ReplayMod 1.7.10-2.5.2** release jar:

* Upstream: <https://github.com/ReplayMod/ReplayMod> — MIT, by CrushedPixel and johni0702.
* 1.7.10 was the last Minecraft version ReplayMod supported before 2.6.0, and upstream **deliberately
  kept the 1.7.10 sources** in `versions/1.7.10/` — the comment in their `root.gradle.kts` reads
  *"We dropped 1.7.10 with the Gradle 7 update but still kept its source in case someone…"*.
* **Why patch instead of fork?** Upstream's own 1.7.10 build chain no longer works from a clean
  checkout (the 2017 tree wants Gradle 2.11 and `ForgeGradle:1.2-SNAPSHOT`, both long gone), while
  **every change in this project is a Mixin or a small helper class** — a fork would contain the same
  code, just compiled by a different toolchain. Patch-then-inject keeps the result **reproducible**
  (see [`docs/BUILDING.md`](docs/BUILDING.md)) and **auditable**: `overlay/` lists the only upstream
  files that are replaced, and nothing is ever deleted from the jar.

The full lineage record — the exact upstream version and jar SHA-256, where the 1.7.10 sources
live upstream, and what a real source fork would actually take — is in [UPSTREAM.md](UPSTREAM.md).
## Why it is needed

ReplayMod's 1.7.10 branch was written for a plain LWJGL 2 client with the vanilla main menu.
GTNH 2.9.0 is a different world: LWJGL 3 through lwjgl3ify, Angelica's GL state manager,
Custom Main Menu, and hundreds of mods ReplayMod never anticipated. Out of the box it either
fails to start or breaks in a dozen different ways.

## What it fixes

| Area | Symptom on GTNH | Fix |
|---|---|---|
| Startup | the stock coremod's ASM transformers make the game unusable (broken inventory drag, or a crash loop) | both transformers re-implemented with named static visitors and made **opt-in** |
| Main menu | no replay entry at all (upstream hooks a screen that is never displayed) | `MixinGuiCustomReplayEntry` |
| Main menu | the "saving replay" panel is drawn but nothing is clickable | `MixinGuiScreenJGuiInputBridge`, `MixinGuiBackgroundProcessesMainMenu` |
| Input | mouse lock loops; the mouse gets stuck after clicking the timeline | `MixinGuiReplayOverlayMouseVisible`, `MixinUserInputGuiScreenKeepMouseLock`, `MixinGuiPopupPreserveMouseLock`, `MixinMinecraftShiftEscReset` |
| Input | mouse wheel broken in jGui screens (SDL3 reports ±1, jGui divides by 4) | `MixinGuiScrollableWheelScale` |
| Input | the replay camera speed cannot be changed with the wheel | wheel → `InputReplayTimer.handleScroll`, driven every tick |
| Input | ReplayMod's own hotkeys (play/pause, **M** for marker, ...) dead while the mouse is unlocked | `ReplayKeyBridge` |
| Replay | opening a replay drops you into first person, your own hand is rendered | `MixinCameraEntityEnsureView`, `MixinNoFirstPersonHandInReplay` |
| Replay | crashes while loading, before a client world exists | `MixinNetHandlerPlayClientNullWorldGuard`, `MixinNetHandlerPlayClientSpawnGuard` (defers instead of dropping), `MixinNetworkManagerDeferredFlush` |
| Replay | camera speed resets whenever you change dimension or leave a spectated player | `ReplayCameraSpeed` |
| Replay | spectating a player + night vision crashes | `MixinEntityRendererNightVisionGuard` |
| Rendering | video export produces a blank/white frame | `MixinPipelinesNoPbo`, `MixinPboFrameCapturerGlReadPixels` |
| Recording | the recording ends up with **no entry for the recording player** | `MixinS0CPacketSpawnPlayerProfileGuard`, `ReplaySpawnProfile` |
| Recording | log flooded with `RejectedExecutionException` after the connection ends | `MixinPacketListenerSaveGuard` |
| Editing | saving / applying markers crashes on one malformed packet | `MixinSquashFilterSafeBulkRead`, `MixinMarkerProcessorSafeFiltering` |
| Misc | AE2 + Waila integration crashes on click | `MixinAe2PartAccessorNullMop` |
| Misc | Et Futurum Requiem NPE during replay | `MixinEtFuturumPlayer` |
| Multiplayer | disconnect during the network handshake | `MixinOpenGuiHandlerConnectingGuard` |

Every file carries a long class-level javadoc explaining the *bytecode-level* reason and how it
was verified — that is where the real documentation lives.

## Requirements

* **JDK 8** — the patch layer targets Java 8 (1.7.10's runtime)
* **ReplayMod 1.7.10-2.5.2** official jar (you supply it)
* A 1.7.10 Forge **dev** jar with MCP names, for compilation
  (e.g. what RetroFuturaGradle / RFG produces — this project was built against
  `recompiled_minecraft-1.7.10.jar`)
* Compile-time libraries — all obtainable from Maven Central or your Forge dev environment:
  * Forge universal 1.7.10 (`net.minecraftforge:forge:1.7.10-10.13.4.1614-1.7.10:universal`)
  * **launchwrapper 1.12** (`net.minecraft:launchwrapper:1.12`) — only the coremod/ASM classes need it
  * Netty 4.0.10, Gson, Guava 17
  * ASM 9.x (`asm`, `asm-tree`, `asm-commons`)
  * Applied Energistics 2 (one mixin targets it) — any GTNH-era AE2 build

## Build & apply

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

`build.ps1` compiles `util/` first, then every mixin listed in `overlay/mixins.replaycompat.json`,
checks **each** file's exit code, and aborts before anything is written if one fails.
`apply.ps1` verifies the injected class count and prints an inventory.

Both scripts are plain PowerShell + `javac`; on Linux/macOS use the equivalent `javac`/`jar`
commands shown in `docs/BUILDING.md`.

## What exactly changes inside the upstream jar

*(Measured against the official 1.7.10-2.5.2 jar, 6101 entries.)*

**Nothing is deleted.** 52 entries are added, 3 files are replaced:

### 1. `META-INF/MANIFEST.MF` — the boot entry points

| Key | Upstream | Patched |
|---|---|---|
| `TweakClass` | `com.replaymod.core.tweaker.ReplayModTweaker` | `org.spongepowered.asm.launch.MixinTweaker` |
| `FMLCorePlugin` | `com.replaymod.core.LoadingPlugin` | `com.gtnh.replaycompat.ReplayModTransformers` |
| `ForceLoadAsMod` | *(absent)* | `true` |
| `MixinConfigs` | *(absent)* | one line listing 10 configs (ours last) |
| `TweakOrder` | `0` | *(removed)* |

`mixins.nonmmlauncher.replaymod.json` is deliberately **not** listed: its companion class is not
shipped in the release jar.

### 2. `mixins.recording.replaymod.json` — one line removed

```diff
     //#if MC<=10710
-    "MixinGuiScreen",
     "MixinS26PacketMapChunkBulk",
     //#endif
```

That upstream mixin conflicts with GTNH's input handling (it is what breaks inventory dragging).
The class stays in the jar; it is simply not applied.

### 3. `assets/replaymod/lang/zh_CN.lang` — 63 missing keys added

The official 1.7.10 Chinese translation is incomplete; this fills in the gaps
(render settings, key binding names, error texts, ...).

### 4. Added: `com/gtnh/replaycompat/**` (51 classes) + `mixins.replaycompat.json`

* **29** functional mixins (all listed in `mixins.replaycompat.json`)
* **9** helper classes (`util/`: 7 classes + 2 inner classes)
* **12** ASM transformer classes + **1** coremod entry point (`ReplayModTransformers`)

## How to verify all of the above yourself

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

## Documentation

| Document | What is in it |
|---|---|
| [`docs/BUILDING.md`](docs/BUILDING.md) | step-by-step build, exact dependency list, troubleshooting, and Linux/macOS equivalents |
| [`docs/KNOWN_ISSUES.md`](docs/KNOWN_ISSUES.md) | **read before installing** — what is still broken, what is unverified, and what is *not* a bug |
| [`docs/IMPLEMENTATION_NOTES.md`](docs/IMPLEMENTATION_NOTES.md) | why the code is written the way it is — the ~20 non-obvious rules, including three diagnoses that were wrong first |
| [`docs/COMPATIBILITY_LOG.md`](docs/COMPATIBILITY_LOG.md) | the full field log: every symptom, its bytecode evidence, and how it was (or was not) fixed |
| [`docs/WHAT_CHANGED.md`](docs/WHAT_CHANGED.md) | complete inventory of what this project changes inside the upstream jar |
| [`docs/PROJECT_HISTORY.md`](docs/PROJECT_HISTORY.md) | project state and version pointers |
| [`docs/CHANGELOG.md`](docs/CHANGELOG.md) | per-build changelog |
| [`docs/RELEASE_NOTES.md`](docs/RELEASE_NOTES.md) | release notes for the current build |
| [`docs/VERIFICATION_GUIDE.md`](docs/VERIFICATION_GUIDE.md) | how to verify a build in-game and what evidence to collect |

## License & credits

* **This repository: MIT** — see [`LICENSE`](LICENSE).
* **Author / maintainer: Luoxueruoling.** All in-game testing, the field reports that drove the
  fixes, and the decisions about what was worth fixing came from this side of the project.
* **The patch layer was implemented by an AI coding agent** (DeepSeek Harness, `deepseek-flash`),
  including the bytecode archaeology — `javap` against both the MCP-named and the SRG-named Minecraft
  jars, and a jar-by-jar diff of every archived build. Several of its first diagnoses were wrong and
  had to be corrected by measurement; those mistakes are kept in the notes on purpose.
* **Toolchain:** Java 8 `javac` · **Mixin 0.8.7 / UniMixins** · **ASM 9.x** ·
  **RetroFuturaGradle** (the 1.7.10 dev jars) · `javap` and the JDK zip APIs for verification ·
  PowerShell for the build scripts.
* **ReplayMod is MIT as well**, by **CrushedPixel** and **johni0702** —
  <https://github.com/ReplayMod/ReplayMod>. The upstream jar ships its own `LICENSE.txt`,
  `NOTICE.txt` and third-party notices (ASM and friends, Apache-2.0); see [`NOTICE.md`](NOTICE.md).
* The upstream jar is **not** redistributed in this repository. If you redistribute a built jar,
  keep the upstream notices inside it.
* Not affiliated with Mojang, the GTNH team, or the ReplayMod authors.

## Status

This is a **working but unfinished** adaptation, targeting a ReplayMod branch that upstream stopped
maintaining for 1.7.10. It is good enough to record, replay, edit and export on GTNH — with caveats,
and a few things are simply not fixable from the client side. Read
[`docs/KNOWN_ISSUES.md`](docs/KNOWN_ISSUES.md) before relying on it for anything important.