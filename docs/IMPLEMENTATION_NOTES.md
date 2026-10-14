# Implementation notes

Why this layer is written the way it is. Every item below cost at least one field round —
they are the non-obvious rules that make the difference between "the game starts" and
"the game starts but something silently does nothing".

## Build-time rules

1. **`-proc:none` is mandatory.** Mixin's annotation processor sits on the classpath; without
   this flag `javac` runs it, and it needs gson → the compile dies with **exit 3**.

2. **Check the exit code of every `javac` invocation, and throw outside the loop.**
   `throw` inside a PowerShell pipeline is swallowed — that once produced a jar built from
   stale classes after a failed compile. The scripts here collect failures and throw at the end.

3. **`getDeclaredField(name)` may return a `static` member.** The most expensive bug in this
   project: two different camera controllers both had a field named `MAX_SPEED` — in one it is
   the *current speed* (instance `double`), in the other it is a *static upper bound*
   (`static final int`, always 1000) while the real value lives in a field called `speed`.
   Reading it by name and assuming it is the value produced code that ran, logged plausible
   numbers, and did nothing. **Always check `Modifier.isStatic`.**

## Environment rules (1.7.10 on a modern stack)

4. **Reflect on `org.lwjglx.*`, not `org.lwjgl.input.*`.** lwjgl3ify only rewrites bytecode
   *call sites*; the old `org.lwjgl.input` classes remain as hollow shells with stale state.

5. **The mouse wheel arrives as ±1, not ±120.** SDL3 delivers one notch as `1`, while jGui's
   widgets were calibrated for LWJGL2's `±120` and divide by 4 — so one notch moves one pixel.
   Anything wheel-driven has to scale explicitly.

6. **Consumable input APIs have thieves.** `Mouse.getDWheel()` is polled every frame by
   MouseTweaks in a typical GTNH pack, so whoever reads first wins and everyone else measures 0.
   Prefer a monotonic, non-consuming source (`org.lwjglx.input.Mouse#totalScrollAmount`) and
   take differences.

7. **There is no GLFW** — the client runs on SDL3, so any technique that hooks GLFW callbacks is
   unavailable by construction.

## Mixin rules

8. **`@Mixin(targets = "...")` is a string match.** The spelling must equal the name inside the
   jar **character for character, including case**. A single lowercase letter once made a fix
   silently never apply (and the crash report printed the wrong spelling, which made it look
   plausible).

9. **Classes in the mixin package cannot be referenced directly** from other classes in that
   package — put shared helpers outside it (`util/` here).

10. **`require = 0` failures are silent.** Never conclude "the mixin is fine" from the absence of
    an error. Verify by exporting bytecode (see 12).

11. **Don't hang per-frame upkeep on a method that stops being called once things go wrong.**
    A guard that only runs while the problematic screen exists disappears exactly when it is
    needed. `Minecraft#runTick` is the only hook guaranteed to run.

12. **Prefer injecting where the parameter types are already unobfuscated.** For one fix the only
    usable injection point was `OpenGuiHandler#channelRead0`, whose parameters (a Netty context
    and FML's own `OpenGui` message class) are not obfuscated at all, while the next method down
    the chain takes Minecraft types whose names are only SRG at runtime.

## Verification rules

13. **Check the exported bytecode, not the log.** `-Dmixin.debug.export=true` plus `javap -c` is
    the only proof that an injection landed.

14. **"No log line" does not distinguish "did not run" from "ran and decided not to act".**
    Guard code must log the *decision to do nothing* as well, otherwise a wrong field name (see 3)
    looks identical to a hook that was never called.

15. **Two differently-titled exceptions with identical stacks are one exception caught twice.**
    That distinction decided which funnel had to be fixed.

16. **Measure the data before changing the code.** Several "obvious" fixes here were wrong; what
    settled them was counting: packets in a recording, wheel deltas per frame, field values per
    controller.

## ReplayMod-specific knowledge

17. **`PacketListener#save` has no play-state gate.** It checks the main thread and writes; the
    real failure mode is an unreadable packet throwing inside packet serialisation, which the
    caller swallows and logs. So a packet that fails to serialise is simply *absent* from the
    recording.

18. **`S0CPacketSpawnPlayer#writePacketData` writes five strings without null checks** — the UUID
    (which does have a fallback), `GameProfile#getName()`, and each property's name/value/signature.
    A skin mod that attaches a `textures` property without a signature makes the whole packet
    unwritable, and the recording silently loses the recording player's own entity.

19. **The camera speed is not stored on the camera entity.** It lives in whichever
    `CameraController` is active, and the three implementations store it completely differently
    (see 3). Spectator's `increaseSpeed`/`decreaseSpeed` are literally empty methods.

20. **ReplayMod adds its own synthetic player-spawn packet**, because 1.7.10 never sends a client
    its own spawn — and it does so only on join. A recording started after the world loaded has no
    such packet and therefore no player of its own.

21. **Recordings only contain what was seen when it was seen.** World chunks arrive when the
    server sends them; a recording started later cannot contain the initial chunk burst. Enable
    ReplayMod's auto-start-recording.
