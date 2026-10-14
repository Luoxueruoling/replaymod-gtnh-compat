# Release notes — v93

**Attach to this release:** `replaymod-1.7.10-2.5.2-gtnh.jar`
(`10880577` bytes · `SHA256 670333B416C71BF437C8E7B6CED8300ADC947815EE078497C0DF39C877A275D0`)

---

## What this is

**ReplayMod 1.7.10-2.5.2, made usable on GT New Horizons 2.9.0-beta-3** (Minecraft 1.7.10 running on
lwjgl3ify / LWJGL 3 / SDL3 with no GLFW, Angelica, Mixin 0.8.7 via UniMixins, and a ~340 mod pack).

Stock ReplayMod either fails to start on this stack or breaks in a dozen ways. This build adds a
patch layer (`com.gtnh.replaycompat`: 29 mixins + 7 helper classes + a coremod/ASM entry point) on top
of the **unmodified upstream jar** — nothing is deleted from it, three of its files are replaced, and
52 entries are added.

## ⚠️ Please read this before installing: there are still real problems

This is a **working but unfinished** adaptation of an upstream branch that stopped being maintained
for 1.7.10. The honest summary — the full list is in
[`KNOWN_ISSUES.md`](https://github.com/Luoxueruoling/replaymod-gtnh-compat/blob/main/docs/KNOWN_ISSUES.md):

* **A recording started after the world finished loading contains no terrain** (replays as sky only)
  and **no entry for the recording player**. ReplayMod only records what it sees when it sees it.
  *Workaround: turn on ReplayMod's "auto start recording".* Fixing this properly is unsolved work.
* **The multiplayer handshake guard has never actually been observed firing.** The underlying cause is
  a third-party mod's null-check bug, not ReplayMod; our guard is defensive, not proven.
* **The camera speed cannot survive a dimension change while using the spectator camera** — that
  camera stores no speed at all upstream.
* **Video export has no depth output** and is slower than stock ReplayMod.
* Clicking far along the big timeline **reloads the replay** — that is upstream's design, not a bug.
* With the mouse unlocked, **WASD does not drive the camera** — also upstream's design (press Ctrl).
* Tested **only** against GTNH 2.9.0-beta-3; other 1.7.10 packs may need adjustments.

Things that *look* broken but are not: a disappearing player (that is the camera being in first person
or attached to whoever you spectate), the vanilla health bar during replays, minimap/overlay mods
still drawing, and the wheel doing nothing at the ends of its range.

## Install

1. Delete any previous `replaymod-*.jar` from `mods/`;
2. drop in `replaymod-1.7.10-2.5.2-gtnh.jar`;
3. restart the game.

Requires a GTNH 2.9.0-beta-3 instance. Nothing else needs to be installed.

## What this build fixes

| Area | Symptom | Fixed by |
|---|---|---|
| Startup | stock transformers made GTNH unusable (broken inventory drag / crash loop) | transformers re-implemented, made opt-in |
| Main menu | no Replay entry; "saving replay" panel not clickable | 3 mixins |
| Input | mouse lock loops / stuck mouse / dead hotkeys / broken wheel in jGui | 6 patches + a key bridge |
| Replay | opened in first person with the hand rendered; crashes while loading | 4 mixins |
| Camera | **speed resets on dimension change or when leaving a spectated player** | `ReplayCameraSpeed` |
| Recording | **recording had no entry for the recording player** (skin property with a null signature made the packet unwritable) | `MixinS0CPacketSpawnPlayerProfileGuard` + `ReplaySpawnProfile` |
| Recording | log flooded with 1343 `RejectedExecutionException` after a session ended | `MixinPacketListenerSaveGuard` |
| Editing | saving / applying markers crashed on one malformed packet | 2 mixins |
| Export | **video export rendered a blank frame** | `MixinPipelinesNoPbo`, `MixinPboFrameCapturerGlReadPixels` |
| Misc | AE2+Waila crash; Et Futurum NPE; multiplayer handshake disconnect | 3 mixins |

## Verifying your download

```powershell
Get-FileHash .\replaymod-1.7.10-2.5.2-gtnh.jar -Algorithm SHA256
# expected: 670333B416C71BF437C8E7B6CED8300ADC947815EE078497C0DF39C877A275D0
```

The jar should contain **6153 entries**; the upstream jar it was built from has 6101.

## Building it yourself

See [`BUILDING.md`](https://github.com/Luoxueruoling/replaymod-gtnh-compat/blob/main/docs/BUILDING.md). Rebuilding from the official upstream jar reproduces this build
**entry for entry** (same 6153 entries, same size).

## Credits

**Luoxueruoling** (author, all in-game testing and field reports), an **AI coding agent**
(DeepSeek Harness / `deepseek-flash`, implementation and bytecode analysis), two external testers on
a ~340-mod pack, and the toolchain: Java 8 `javac`, Mixin 0.8.7 / UniMixins, ASM 9.x,
RetroFuturaGradle, `javap`. Upstream ReplayMod is **MIT** by CrushedPixel and johni0702.

## License

MIT — see [`LICENSE`](https://github.com/Luoxueruoling/replaymod-gtnh-compat/blob/main/LICENSE). The upstream jar is not
redistributed in the repository; this release attaches a jar that contains it, with its original
`LICENSE.txt` / `NOTICE.txt` kept inside.
