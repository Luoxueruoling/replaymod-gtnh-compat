# Known issues and limitations

**Read this before installing.** This compatibility layer makes ReplayMod usable on GTNH, but it is
an adaptation of a mod whose 1.7.10 branch was abandoned upstream in 2021 — several things are still
broken, and a few things that *look* broken are actually by design (of ReplayMod, of GTNH, or of
1.7.10 itself). This page is the honest list.

Last updated for the **v93** build.

---

## 1. Not verified in the field

| Thing | Status |
|---|---|
| **Disconnect during the network handshake** (multiplayer) | A guard exists (`MixinOpenGuiHandlerConnectingGuard`) that drops a "open this GUI" packet arriving before the client world exists. The underlying cause is a **third-party mod's null-check bug**, not ReplayMod and not this patch. ⚠️ The guard has **never actually been observed firing** — the timing window is narrow. It is defensive, not proven. |
| Other 1.7.10 packs | Developed and tested against **GT New Horizons 2.9.0-beta-3** only (lwjgl3ify + Angelica + a ~340 mod pack). Other packs may need adjustments. |
| Video export edge cases | Export was fixed for the common path; unusual resolutions/encoders are untested. |

## 2. Known broken or imperfect

### Replay content

* **A recording started after the world finished loading contains no terrain.** ReplayMod records
  chunks when they arrive; the initial burst is sent once, at join. A late-started recording therefore
  replays as sky only. **Workaround: enable ReplayMod's `AUTO_START_RECORDING`.**
  Making late recordings complete again is an open design question (server-side resend vs. client-side
  backfill) and is **not implemented**.
* **A recording started after join has no entry for the recording player**, so you may not see your own
  character in that replay until you change dimension inside it. This is a second, independent
  consequence of the same "recorded only what it saw when it saw it" behaviour.
* **"The player disappears"** is usually *not* a missing entity: the camera is simply co-located with
  the player (first person) or attached to another player you are spectating. Use the free camera.
* **Jumping backwards in the timeline "flies"** for a moment, and the player can briefly vanish — this
  is upstream's replay mechanism plus a camera reset, not a regression.

### Camera & input

* **The camera speed cannot be kept across dimension changes while using the *spectator* camera
  controller** — that controller stores no speed at all (its `increaseSpeed`/`decreaseSpeed` are empty
  methods upstream). It works for the default (vanilla) camera and the classic one.
* **With the mouse unlocked, WASD does not drive the replay camera.** Upstream design — press Ctrl to
  grab the mouse back.
* **The wheel sensitivity scale differs per camera type.** The default camera uses a ±1000 "gear"
  scale, so the default 10 steps per notch is coarse; `/replaycompat wheel 1` makes it fine-grained.

### Interface & rendering

* **Clicking far along the big timeline reloads the replay** (you see `Client disconnected from
  server`). Verified to be upstream's replay mechanism, not an input bug — jumping is a reload by design.
* **Export does not support depth output**, and is somewhat slower than stock ReplayMod.
* **The vanilla health bar is still drawn in replays** — GTNH never sends `HEALTH` events.
* **JourneyMap's minimap and InGameInfoXML panels keep drawing** during replays; those mods draw them
  themselves.
* Close to a character in a replay, the near field can look **dark** until the camera moves away
  (upstream renderer behaviour).

## 3. Explicitly out of scope

* **Server-side recording.** There is a separate, unfinished server recorder project. It is **not
  deployed** and its core gap (some entity packets are missed) is unsolved.
* **Making old, already-recorded replays complete.** If the chunks are not in the file, nothing can
  bring them back.
* Client-side terrain backfill for late-started recordings (see §2).

## 4. Things that are *not* bugs (please don't report them)

`F` = first-person hand hidden in replays · health bar visible · backwards jump "flies" ·
timeline click reloads · minimap/overlay mods still draw · wheel stops responding at the ends of its
range (upstream silently ignores out-of-range values).

---

## Reporting an issue

Please include:

1. GTNH version, and your mod count (or `mods/` listing) — this patch is environment-sensitive;
2. **`logs/fml-client-latest.log`** — *not* `latest.log`: the patch's diagnostics go through
   `System.out` and only appear in the FML log;
3. For anything visual, an **F3 screenshot**;
4. For replay problems, the **original `.mcpr`** file (not a re-encoded video);
5. What you did, and roughly when (`hh:mm`).
