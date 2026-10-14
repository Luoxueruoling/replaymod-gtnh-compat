# Upstream lineage

Everything needed to relate this repository to the original project, and to reproduce the exact jar
this patch is applied to.

## The upstream project

| | |
|---|---|
| Repository | <https://github.com/ReplayMod/ReplayMod> |
| License | **MIT** — `LICENSE.md` in the upstream repo, `LICENSE.txt` inside the jar |
| Authors | **CrushedPixel**, **johni0702** and contributors |
| Website | <https://replaymod.com> |

## The exact build this patch targets

| | |
|---|---|
| Version | **`1.7.10-2.5.2`** — the last ReplayMod release supporting Minecraft 1.7.10 |
| File | `replaymod-1.7.10-2.5.2.jar` |
| Size | `10788790` bytes |
| SHA-256 | `E8B0379ED3B03B3028145C71962AA141757844F2EB7B82CE5390A9CEE5A8CE96` |
| Entries | `6101` |

Confirm you have the same input before patching:

```powershell
Get-FileHash .\replaymod-1.7.10-2.5.2.jar -Algorithm SHA256
# -> E8B0379ED3B03B3028145C71962AA141757844F2EB7B82CE5390A9CEE5A8CE96
```

## Where the 1.7.10 sources live upstream

Upstream dropped 1.7.10 with 2.6.0 but **kept the sources on purpose**. From their `root.gradle.kts`:

```
// We dropped 1.7.10 with the Gradle 7 update but still kept its source in case someone…
.filterNot { it == "1.7.10" && versionComparator.compare(version, "2.6.0") >= 0 }
```

The layout is *shared `src/` + per-version overrides*. The 1.7.10-specific files are:

```
versions/1.7.10/src/main/java/com/replaymod/
├── core/asm/GLErrorTransformer.java            <- this project re-implements it (see below)
├── core/asm/GLStateTrackerTransformer.java     <- and this one
├── core/mixin/ResourcePackRepositoryAccessor.java
├── core/utils/ModInfoGetter.java
├── core/versions/BufferBuilder.java
├── recording/mixin/MixinGuiScreen.java         <- this project disables it
├── recording/mixin/MixinS26PacketMapChunkBulk.java
├── render/hooks/ForceChunkLoadingHook.java
├── render/hooks/GLStateTracker.java
├── render/mixin/MixinOpenGlHelper.java
├── render/mixin/Mixin_ForceChunkLoading.java
├── replay/mixin/MixinEntityRenderer.java
├── replay/mixin/MixinFMLClientHandler.java
└── compat/shaders/mixin/MixinShaders.java
```

There is also a historical single-commit backport, tagged **`1.7.10-2.0.1-b1`** (`1283a39`,
2017-09-14). It is **five minor versions behind** the build this patch targets — do not use it as a
base.

## Why this is a patch layer and not a fork

1. **The 1.7.10 build chain no longer works from a clean checkout.** The 2017 tree's wrapper asks for
   **Gradle 2.11** (2015) over plain HTTP from `services.gradle.org`, and its build script needs
   `net.minecraftforge.gradle:ForgeGradle:1.2-SNAPSHOT` — a snapshot long since purged from Sonatype.
   The modern tree expects `gg.essential.multi-version` plus a Gradle 9.x toolchain.
2. **Every change in this project is a Mixin or a small helper class.** A fork would ship the same
   code in a different container.
3. **Patching is more auditable.** [`overlay/`](overlay) lists the only three upstream files that get
   replaced, `src/` is the complete list of added code, and **nothing is ever deleted** from the jar.

The trade-offs, stated plainly: a patch layer cannot follow upstream improvements (there are none for
1.7.10) and it is tied to one exact upstream jar — hence the SHA-256 above. If the 1.7.10 build chain
is ever revived, the contents of `src/` and `overlay/` transplant directly into a real fork.

## Relationship to the upstream repository

This repository is **not a Git fork** of the upstream project, and it contains **no upstream code** —
which keeps the licensing trivial. It links to upstream rather than duplicating it. See
[`NOTICE.md`](NOTICE.md) for the third-party notices, and [`docs/WHAT_CHANGED.md`](docs/WHAT_CHANGED.md)
for the complete inventory of what gets changed inside the jar.
