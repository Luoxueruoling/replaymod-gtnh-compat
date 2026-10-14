package com.gtnh.replaycompat.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Shared state between the replay-mouse patches, plus the Shift+ESC force reset.
 *
 * <p><b>Why this class exists instead of one mixin reading the other's field:</b> classes inside a
 * mixin package may not be referenced directly -- the mixin config owns that package and any such
 * reference dies with {@code IllegalClassLoadError: ... is in a defined mixin package ... and cannot be
 * referenced directly} (HANDOVER pit 42). Helpers therefore live outside {@code
 * com.gtnh.replaycompat.mixin}, exactly like {@link NullWorldGuard} does.
 *
 * <p><b>Why the reset is reflective:</b> this compat project is compiled with plain {@code javac}
 * against MCP-named Minecraft and has <em>no reobf step</em>, so any MCP member name would compile and
 * then be absent at runtime (HANDOVER pit 29 -- this bit me twice in one session). Everything touching
 * Minecraft or ReplayMod classes is therefore resolved reflectively here, trying both spellings.
 */
public final class ReplayMouseState {

    private ReplayMouseState() {
    }

    /**
     * True while the player has locked the mouse visible with Ctrl (the toggle in
     * {@code MixinGuiReplayOverlayMouseVisible}).
     */
    public static volatile boolean lockedVisible;

    /**
     * True while the player's Ctrl lock is demonstrably <b>not</b> in effect - the overlay is hidden
     * (player list / keyframe repository call {@code setVisible(false)}), or its input screen is not
     * mounted. Written every frame by {@code MixinGuiReplayOverlayMouseVisible}.
     *
     * <p>Measured 2026-10-11 (HANDOVER 12.49): both tester logs contain ~185
     * "locked, but no input screen is mounted (currentScreen=null)" lines and <b>zero</b> Shift+ESC
     * resets - i.e. the emergency key was pressed exactly while the lock was ineffective, and the
     * sneak binding had been cleared by {@code unPressAllKeys} (pit 48) so the combination never
     * matched. With this flag a plain ESC is enough in that state, while a healthy lock leaves ESC
     * alone (it still opens the game menu).
     */
    public static volatile boolean lockIneffective;

    private static volatile boolean keptLockReported;
    private static volatile boolean resetRequested;
    private static volatile boolean resetReported;

    /** One-shot diagnostic: the input screen closed while the player still had the lock on. */
    public static void noteKeptLockOnce() {
        if (keptLockReported) {
            return;
        }
        keptLockReported = true;
        System.out.println("[replaycompat] replay mouse: input screen closed while locked - keeping"
                + " the mouse visible (otherwise the lock gets stolen back)");
    }

    /** Asks the replay-mouse patch to drop its lock so the mouse is grabbed again. */
    public static void requestReset() {
        resetRequested = true;
    }

    /** True exactly once per {@link #requestReset()} call. */
    public static boolean consumeResetRequest() {
        if (!resetRequested) {
            return false;
        }
        resetRequested = false;
        return true;
    }

    private static void noteResetOnce() {
        if (resetReported) {
            return;
        }
        resetReported = true;
        System.out.println("[replaycompat] replay reset (Shift+ESC): mouse released, spectatorship"
                + " cleared, GUI closed - back to the freshly loaded state");
    }

    // ------------------------------------------------------------------ Shift+ESC force reset

    private static boolean resolved;
    private static boolean broken;
    private static Field fTheWorld;
    private static Field fGameSettings;
    private static Field fKeyBindSneak;
    private static Field fPressed;
    private static Method mDisplayGuiScreen;
    private static Method mGetIsKeyPressed;
    private static Field fReplayModInstance;
    private static Method mGetReplayHandler;
    private static Method mGetOverlay;
    private static Method mSetMouseVisible;
    private static Method mSpectateCamera;

    /**
     * Shift+ESC inside a replay: restore the state a freshly loaded recording is in.
     *
     * <p>Needed because switching to a player's view closes the overlay's input screen while the 12.38
     * lock-preserving patch keeps {@code mouseVisible} true; if the input screen is not re-mounted
     * (it only is while {@code mc.currentScreen == null}) the flag and reality disagree and the mouse
     * feels stuck. This drops our lock, clears spectatorship via upstream's own
     * {@code ReplayHandler#spectateCamera} and closes any open screen.
     *
     * @param minecraft the {@code Minecraft} instance (passed in so this class needs no MC types)
     * @return true when the reset ran and ESC should not open the menu
     */
    /**
     * True when {@code Minecraft.setIngameFocus()} has to be suppressed this tick: a replay is running
     * and the player asked for a visible mouse.
     *
     * <h3>Why (HANDOVER 12.50 - bytecode + tester logs)</h3>
     *
     * <p>{@code Minecraft.runTick} ({@code func_71407_l}) contains, inside its mouse-event loop:
     *
     * <pre>
     * if (mc.currentScreen == null &amp;&amp; !mc.inGameHasFocus &amp;&amp; Mouse.getEventButtonState()) {
     *     mc.setIngameFocus();     // func_71381_h: grabMouseCursor() + displayGuiScreen(null)
     * }
     * </pre>
     *
     * <p>So <b>every left click</b> that arrives while the input screen is momentarily down re-grabs the
     * mouse <i>and</i> closes the input screen - measured as the cause of the tester's
     * "after clicking the timeline a few times I cannot move any more" (and of the ~190
     * "locked, but no input screen is mounted" lines). Suppressing it while the player's Ctrl lock is on
     * keeps the mouse usable; with the lock off nothing changes, so clicking the game window still
     * focuses it as before.
     */
    public static boolean shouldSuppressIngameFocus(Object minecraft) {
        try {
            resolve();
            if (broken || minecraft == null || !lockedVisible) {
                return false;
            }
            if (read(fTheWorld, minecraft) == null) {
                return false; // not in a world, so not in a replay
            }
            Object mod = fReplayModInstance.get(null);
            if (mod == null) {
                return false;
            }
            return mGetReplayHandler.invoke(mod) != null;
        } catch (Throwable t) {
            return false; // never break normal focusing
        }
    }

    // ------------------------------------------------------- per-frame upkeep (HANDOVER 12.52)

    private static Field fOverlayVisible;
    private static Field fUserInputGuiScreen;
    private static Field fCurrentScreen;
    private static Field fWorldPlayerEntities;
    private static Field fWorldLoadedEntities;
    private static Field fRenderViewEntity;
    private static long lastCensus;
    private static int censusLogs;
    private static boolean tickResolved;
    private static boolean tickResolvedFailed;
    private static long lastMountAttempt;
    private static int mountTries;

    /**
     * Per-frame replay-mouse upkeep, driven from {@code Minecraft.runTick} ({@code func_71407_l}).
     *
     * <h3>Why it cannot live in the overlay (pit 59, HANDOVER 12.52)</h3>
     *
     * <p>{@code AbstractGuiOverlay.updateUserInputGui()} is only ever called automatically by the
     * overlay's <b>own input screen</b> (synthetic accessor {@code access$100}). The moment that screen
     * is torn down - by {@code setIngameFocus}, by a replay restart, by the player list - nothing mounts
     * it again, and every guard piggybacking on that method disappears with it. That is exactly why the
     * checks added in v72 and v75 never fired in the field. This runs every tick instead, and mounts the
     * input screen back itself while the player's Ctrl lock is on.
     *
     * <p>Safety: at most one attempt per second, and after three attempts the lock is dropped instead.
     * A state we cannot keep honest must not be held, and calling {@code displayGuiScreen} every frame
     * would wipe the movement keys on every frame (pit 48).
     */
    public static void tick(Object minecraft) {
        try {
            resolve();
            // Install the hotkey bridge on the first tick (render thread). It forwards only while the
            // lock is on, because with the lock off the game feeds KeyBinding itself (12.56).
            ReplayKeyBridge.ensureInstalled();
            // The wheel -> camera-speed conversion has to run on every frame no matter what the input
            // screen is doing, so it is driven from here like the rest of the upkeep (HANDOVER 12.61).
            wheelTick();
            // Keeping the speed the player dialled in alive across dimension changes is driven from here
            // too (12.64 feedback #2), and deliberately before the lock check below: the speed has to
            // survive even while the mouse is not locked.
            ReplayCameraSpeed.tick(minecraft);
            if (broken || minecraft == null || !lockedVisible) {
                return;
            }
            if (read(fTheWorld, minecraft) == null) {
                return; // not in a world -> not in a replay
            }
            Object mod = fReplayModInstance.get(null);
            if (mod == null) {
                return;
            }
            Object handler = mGetReplayHandler.invoke(mod);
            if (handler == null) {
                return;
            }
            Object overlay = mGetOverlay.invoke(handler);
            if (overlay == null) {
                return;
            }
            resolveForTick();
            if (tickResolvedFailed) {
                return;
            }
            // A hidden overlay manages the mouse itself (player list / keyframe repository): stand down.
            if (!Boolean.TRUE.equals(fOverlayVisible.get(overlay))) {
                lockedVisible = false;
                return;
            }
            Object screen = fCurrentScreen.get(minecraft);
            if (screen != null && screen.getClass().getName().contains("UserInputGuiScreen")) {
                mountTries = 0;
                return; // healthy
            }
            long now = System.currentTimeMillis();
            if (now - lastMountAttempt < 1000L) {
                return;
            }
            lastMountAttempt = now;
            if (++mountTries > 3) {
                mountTries = 0;
                lockedVisible = false;
                System.out.println("[replaycompat] replay mouse: the input screen keeps vanishing - "
                        + "releasing the lock instead of fighting it");
                return;
            }
            Object input = fUserInputGuiScreen.get(overlay);
            if (input == null) {
                return;
            }
            if (mountTries == 1) {
                System.out.println("[replaycompat] replay mouse: mounting the overlay input screen myself "
                        + "(the overlay only does that while its own screen is up)");
            }
            mDisplayGuiScreen.invoke(minecraft, input);
        } catch (Throwable ignored) {
            // never disturb the game loop
        }
    }

    /**
     * Low-frequency, read-only census of the replay world (HANDOVER 12.54).
     *
     * <p>Reported as "the character turned into a rolling log" (= the player entity is gone). That
     * symptom has two very different causes and the log cannot tell them apart: the entity may really
     * be missing from the world (the 12.27 {@code S0CPacketSpawnPlayer} path), or it may exist and
     * simply not be rendered / not be in front of the camera (12.37). Printing the counts, the view
     * entity and whether a screen is up settles it in one round.
     */
    public static void census(Object minecraft) {
        try {
            resolve();
            if (broken || minecraft == null) {
                return;
            }
            if (read(fTheWorld, minecraft) == null) {
                return;
            }
            Object mod = fReplayModInstance.get(null);
            if (mod == null) {
                return;
            }
            if (mGetReplayHandler.invoke(mod) == null) {
                return; // not replaying
            }
            long now = System.currentTimeMillis();
            if (now - lastCensus < 5000L || censusLogs >= 20) {
                return;
            }
            lastCensus = now;
            resolveForTick();
            if (tickResolvedFailed) {
                return;
            }
            Object world = fTheWorld.get(minecraft);
            Object view = fRenderViewEntity.get(minecraft);
            Object screen = fCurrentScreen.get(minecraft);
            censusLogs++;
            System.out.println("[replaycompat] replay entity census: players=" + size(fWorldPlayerEntities, world)
                    + " entities=" + size(fWorldLoadedEntities, world)
                    + " renderView=" + (view == null ? "null" : view.getClass().getSimpleName())
                    + " currentScreen=" + (screen == null ? "null" : screen.getClass().getSimpleName()));
        } catch (Throwable ignored) {
            // diagnostics only
        }
    }

    // ------------------------------------------------- wheel -> camera speed (HANDOVER 12.61)

    /** At most this many speed steps per frame; the raw delta is device dependent (HANDOVER 12.15). */
    private static final int WHEEL_MAX_STEPS = 120;

    /**
     * Speed steps applied per wheel notch.
     *
     * <p>lwjgl3ify/SDL delivers one notch as a delta of 1.0 (measured in the v85 tester log:
     * "adjusted the camera speed by 1 step(s)"), while ReplayMod's {@code increaseSpeed()} moves the
     * camera speed by a single step. One step per notch was measured as far too coarse by the testers
     * on 2026-10-13 ("滚轮的灵敏度有点低了 / 有点太低了"), so a notch is scaled. Upstream on LWJGL2 saw
     * ±120 per notch, which is the upper bound used here.
     *
     * <p><b>Runtime tunable (v87)</b> - three ways, in increasing convenience:
     * <ol>
     *   <li>{@code -Dreplaycompat.wheelSteps=N} (1..120);</li>
     *   <li>the in-game command {@code /replaycompat wheel <n>} - handled client-side, never sent to
     *       the server (see {@link #handleChatCommand});</li>
     *   <li>{@code /replaycompat wheel} with no value reports the current setting.</li>
     * </ol>
     */
    private static final int WHEEL_STEPS_DEFAULT = 10;
    private static final int WHEEL_STEPS_MIN = 1;
    private static final int WHEEL_STEPS_MAX = 120;

    private static volatile int wheelStepsPerNotch = replaycompat$initialSteps();

    private static int replaycompat$initialSteps() {
        try {
            return replaycompat$clamp(Integer.getInteger("replaycompat.wheelSteps", WHEEL_STEPS_DEFAULT)
                    .intValue());
        } catch (Throwable t) {
            return WHEEL_STEPS_DEFAULT;
        }
    }

    private static int replaycompat$clamp(int n) {
        if (n < WHEEL_STEPS_MIN) {
            return WHEEL_STEPS_MIN;
        }
        return n > WHEEL_STEPS_MAX ? WHEEL_STEPS_MAX : n;
    }

    /** Current steps per notch (read by the diagnostics line and the command). */
    public static int getWheelStepsPerNotch() {
        return wheelStepsPerNotch;
    }

    public static void setWheelStepsPerNotch(int n) {
        wheelStepsPerNotch = replaycompat$clamp(n);
    }

    private static boolean wheelResolved;
    private static boolean wheelResolveFailed;
    private static boolean wheelProbeLogged;
    private static Field fTotalScrollAmount;
    private static Method mHandleScroll;
    private static double pendingScroll;
    private static double lastScrollAmount = Double.MIN_VALUE;
    private static int wheelApplied;
    private static int wheelLogs;

    /**
     * Turns the mouse wheel into camera-speed steps while a replay is running (HANDOVER 12.42/12.59,
     * reworked in 12.61).
     *
     * <h3>Why it is driven from {@code runTick} and not from the overlay</h3>
     *
     * <p>Every attempt from v70 to v84 hung off
     * {@code AbstractGuiOverlay$UserInputGuiScreen#func_146274_d}, which is <b>not guaranteed to run</b>:
     * during a replay the input pump is {@code InputReplayTimer#updateTimer}, whose input-screen branch
     * calls {@code GuiScreen#handleInput}; that method only forwards to {@code handleMouseInput} while
     * {@code Mouse#next()} still yields events, so any earlier consumer of the mouse queue silently
     * removes the hook. {@code Minecraft.runTick} cannot be skipped, so the conversion lives here
     * (pit 59 - the same lesson as the mouse upkeep in 12.52).
     *
     * <h3>Why the wheel is read as a difference</h3>
     *
     * <p>{@code Mouse#getDWheel()} and {@code Mouse#getEventDWheel()} go to whoever reads them first -
     * GTNH ships MouseTweaks, which polls {@code getDWheel()} every frame (pit 67), which is why v81's
     * switch to it still measured 0. {@code org.lwjglx.input.Mouse#totalScrollAmount} is public,
     * monotonic and <b>never consumed</b> (verified in the lwjgl3ify 3.0.31 bytecode: {@code
     * addWheelEvent} adds to it before its integer-notch gate), so the delta is taken here. The
     * fractional remainder is carried over because precise wheels and touchpads report less than one
     * notch per event.
     *
     * <h3>Why upstream's own method is called</h3>
     *
     * <p>{@code InputReplayTimer#handleScroll} is what ReplayMod's own {@code @Redirect} on
     * {@code Mouse#getEventDWheel} calls; it re-checks the replay handler and the camera entity for null
     * and returns quietly otherwise, so running it outside a replay is a no-op. Reusing it keeps the
     * step size and the speed range identical to upstream instead of inventing our own maths (12.43).
     */
    public static void wheelTick() {
        try {
            resolveWheel();
            if (wheelResolveFailed) {
                return;
            }
            if (!wheelProbeLogged) {
                wheelProbeLogged = true;
                System.out.println("[replaycompat] wheel probe: per-tick wheel hook is alive"
                        + " (totalScrollAmount=" + (fTotalScrollAmount != null ? "available" : "unavailable")
                        + ", handleScroll=" + (mHandleScroll != null ? "available" : "unavailable")
                        + ", stepsPerNotch=" + wheelStepsPerNotch + ")");
            }
            if (fTotalScrollAmount == null) {
                return;
            }
            double current = ((Double) fTotalScrollAmount.get(null)).doubleValue();
            if (lastScrollAmount == Double.MIN_VALUE) {
                lastScrollAmount = current; // first sighting: no delta to apply yet
                return;
            }
            pendingScroll += current - lastScrollAmount;
            lastScrollAmount = current;

            int notches = (int) pendingScroll; // truncates toward zero, the remainder stays pending
            if (notches == 0) {
                return;
            }
            pendingScroll -= notches;
            // One notch is one upstream speed step, which the testers measured as far too coarse
            // (2026-10-13). Scale it, then clamp so a fast spin cannot jump straight to the maximum.
            int steps = notches * wheelStepsPerNotch;
            if (steps > WHEEL_MAX_STEPS) {
                steps = WHEEL_MAX_STEPS;
            } else if (steps < -WHEEL_MAX_STEPS) {
                steps = -WHEEL_MAX_STEPS;
            }
            mHandleScroll.invoke(null, Integer.valueOf(steps));
            // Tell the speed keeper how many notches the player applied: a controller that keeps no
            // readable speed of its own (SpectatorCameraController) can then still be given the same
            // input again after a world change (HANDOVER 12.72).
            ReplayCameraSpeed.noteWheelSteps(steps);
            wheelApplied++;
            if (wheelLogs < 10) {
                wheelLogs++;
                System.out.println("[replaycompat] wheel: adjusted the camera speed by " + steps
                        + " step(s) from the wheel (event #" + wheelApplied + ")");
            }
        } catch (Throwable ignored) {
            // This is an enhancement: it must never disturb the game loop.
        }
    }

    // ------------------------------------------------- in-game command (HANDOVER 12.64)

    /**
     * Handles {@code /replaycompat ...} typed into the chat box, entirely client-side (v87).
     *
     * <p>Called from {@code EntityClientPlayerMP#sendChatMessage} ({@code func_71165_d}) - the single
     * funnel every chat line passes through - and returning {@code true} makes the mixin cancel the
     * vanilla call, so the command never reaches the server (no "unknown command" reply, works on any
     * server / in singleplayer).
     *
     * <p>Syntax: {@code /replaycompat wheel [1-120]} - without a number it reports the current value.
     *
     * @return true when the message was our command and must not be sent on
     */
    public static boolean handleChatCommand(String message) {
        try {
            if (message == null) {
                return false;
            }
            String trimmed = message.trim();
            String lower = trimmed.toLowerCase();
            if (!lower.equals("/replaycompat") && !lower.startsWith("/replaycompat ")) {
                return false;
            }
            String[] parts = trimmed.split("\\s+");
            String value = null;
            if (parts.length >= 2 && "wheel".equalsIgnoreCase(parts[1])) {
                if (parts.length >= 3) {
                    value = parts[2];
                }
            } else if (parts.length >= 2) {
                value = parts[1]; // "/replaycompat <n>" shorthand
            }

            if (value == null) {
                replaycompat$feedback("wheel sensitivity: " + wheelStepsPerNotch + " speed step(s) per"
                        + " notch (range " + WHEEL_STEPS_MIN + "-" + WHEEL_STEPS_MAX
                        + ") - use /replaycompat wheel <n>");
                return true;
            }
            int n;
            try {
                n = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                replaycompat$feedback("'" + value + "' is not a number - use /replaycompat wheel <"
                        + WHEEL_STEPS_MIN + "-" + WHEEL_STEPS_MAX + ">");
                return true;
            }
            int before = wheelStepsPerNotch;
            setWheelStepsPerNotch(n);
            replaycompat$feedback("wheel sensitivity: " + before + " -> " + wheelStepsPerNotch
                    + " speed step(s) per notch" + (n != wheelStepsPerNotch ? " (out of range, clamped)" : ""));
            return true;
        } catch (Throwable t) {
            return false; // never swallow a chat line because of our own failure
        }
    }

    /** Prints a line into the local chat box. Reflective: without reobf no MCP member name survives. */
    private static void replaycompat$feedback(String text) {
        try {
            Class<?> minecraft = Class.forName("net.minecraft.client.Minecraft");
            Method get = null;
            for (String name : new String[]{"getMinecraft", "func_71410_x"}) {
                try {
                    get = minecraft.getMethod(name);
                    break;
                } catch (NoSuchMethodException ignored) {
                    // try the next spelling
                }
            }
            Object mc = get == null ? null : get.invoke(null);
            if (mc == null) {
                throw new IllegalStateException("no Minecraft instance");
            }
            Object ingame = findField(mc.getClass(), "ingameGUI", "field_71456_v").get(mc);
            Object chat = ingame.getClass().getMethod("func_146158_b").invoke(ingame);
            Object line = Class.forName("net.minecraft.util.ChatComponentText")
                    .getConstructor(String.class).newInstance("[replaycompat] " + text);
            chat.getClass()
                    .getMethod("func_146227_a", Class.forName("net.minecraft.util.IChatComponent"))
                    .invoke(chat, line);
        } catch (Throwable t) {
            // Chat feedback is a nicety; the log line is the fallback.
            System.out.println("[replaycompat] " + text);
        }
    }

    // ------------------------------------------------- recorder lifecycle (HANDOVER 12.65, v88)

    /**
     * True when a ReplayMod {@code PacketListener}'s save thread pool has already been shut down.
     *
     * <p>Used by {@code MixinPacketListenerSaveGuard}: after the connection closes, the recorder's
     * {@code saveService} executor is terminated but packets keep arriving (the classic case is starting a
     * recording and then connecting to a server without stopping it), so every packet produced a
     * {@code RejectedExecutionException} stack trace in the log.
     *
     * <p>Reflective on purpose: the field belongs to ReplayMod, which ships unobfuscated, but this project
     * never hard-references foreign members from an injection body (pit 29).
     */
    public static boolean packetSaverIsShutDown(Object packetListener) {
        try {
            if (packetListener == null) {
                return false;
            }
            Object service = findField(packetListener.getClass(), "saveService").get(packetListener);
            if (service instanceof java.util.concurrent.ExecutorService) {
                return ((java.util.concurrent.ExecutorService) service).isShutdown();
            }
        } catch (Throwable ignored) {
            // A guard that cannot tell must never change behaviour.
        }
        return false;
    }

    private static void resolveWheel() {
        if (wheelResolved || wheelResolveFailed) {
            return;
        }
        wheelResolved = true;
        try {
            // org.lwjglx.* on purpose: lwjgl3ify only rewrites bytecode call sites, so the
            // org.lwjgl.input.* shim keeps stale state (pit 57).
            fTotalScrollAmount = Class.forName("org.lwjglx.input.Mouse").getField("totalScrollAmount");
        } catch (Throwable t) {
            fTotalScrollAmount = null;
        }
        try {
            Class<?> timer = Class.forName("com.replaymod.replay.InputReplayTimer");
            mHandleScroll = timer.getMethod("handleScroll", int.class);
        } catch (Throwable t) {
            mHandleScroll = null;
        }
        if (fTotalScrollAmount == null || mHandleScroll == null) {
            wheelResolveFailed = true;
            System.out.println("[replaycompat] wheel camera speed unavailable: totalScrollAmount="
                    + (fTotalScrollAmount != null) + ", handleScroll=" + (mHandleScroll != null));
        }
    }

    private static String size(Field field, Object target) {
        try {
            Object value = field == null ? null : field.get(target);
            if (value instanceof java.util.List) {
                return String.valueOf(((java.util.List<?>) value).size());
            }
            return value == null ? "?" : "n/a";
        } catch (Throwable t) {
            return "?";
        }
    }

    private static void resolveForTick() {
        if (tickResolved || tickResolvedFailed) {
            return;
        }
        try {
            Class<?> overlay = Class.forName(
                    "com.replaymod.lib.de.johni0702.minecraft.gui.container.AbstractGuiOverlay");
            fOverlayVisible = findField(overlay, "visible");
            fUserInputGuiScreen = findField(overlay, "userInputGuiScreen");
            Class<?> mc = Class.forName("net.minecraft.client.Minecraft");
            fCurrentScreen = findField(mc, "currentScreen", "field_71462_r");
            fRenderViewEntity = findField(mc, "renderViewEntity", "field_71451_h");
            Class<?> world = Class.forName("net.minecraft.world.World");
            fWorldPlayerEntities = findField(world, "playerEntities", "field_73010_i");
            fWorldLoadedEntities = findField(world, "loadedEntityList", "field_72996_f");
            tickResolved = true;
        } catch (Throwable t) {
            tickResolvedFailed = true;
            System.out.println("[replaycompat] replay mouse tick unavailable: " + t);
        }
    }

    public static boolean performReset(Object minecraft) {
        try {
            resolve();
            if (broken || minecraft == null) {
                return false;
            }
            if (read(fTheWorld, minecraft) == null) {
                return false; // not in a world, so not in a replay
            }
            // Shift+ESC as documented - plus: while the lock is measurably ineffective, a plain ESC
            // already means "get me out of this stuck state" (HANDOVER 12.49).
            if (!isShiftHeld(minecraft) && !lockIneffective) {
                return false;
            }
            Object mod = fReplayModInstance.get(null);
            if (mod == null) {
                return false;
            }
            Object handler = mGetReplayHandler.invoke(mod);
            if (handler == null) {
                return false; // not replaying: let the normal menu open
            }

            requestReset(); // consumed by MixinGuiReplayOverlayMouseVisible on its next poll
            Object overlay = mGetOverlay.invoke(handler);
            if (overlay != null) {
                mSetMouseVisible.invoke(overlay, Boolean.FALSE); // mouse grabbed again -> free flight
            }
            mSpectateCamera.invoke(handler); // leave the player's view (upstream's own method)
            mDisplayGuiScreen.invoke(minecraft, new Object[]{null}); // close whatever is open
            noteResetOnce();
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** {@code keyBindSneak.pressed}: ESC has no KeyBinding, so the combination is read from sneak. */
    private static boolean isShiftHeld(Object minecraft) {
        try {
            Object settings = read(fGameSettings, minecraft);
            if (settings == null) {
                return false;
            }
            Object sneak = read(fKeyBindSneak, settings);
            if (sneak == null) {
                return false;
            }
            if (mGetIsKeyPressed != null) {
                return Boolean.TRUE.equals(mGetIsKeyPressed.invoke(sneak));
            }
            Object pressed = read(fPressed, sneak);
            return Boolean.TRUE.equals(pressed);
        } catch (Throwable t) {
            return false;
        }
    }

    private static Object read(Field field, Object target) throws IllegalAccessException {
        return field == null ? null : field.get(target);
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            Class<?> mc = Class.forName("net.minecraft.client.Minecraft");
            fTheWorld = findField(mc, "theWorld", "field_71441_e");
            fGameSettings = findField(mc, "gameSettings", "field_71474_y");
            mDisplayGuiScreen = findMethod(mc, new Class<?>[]{Class.forName("net.minecraft.client.gui.GuiScreen")},
                    "displayGuiScreen", "func_147108_a");

            Class<?> settings = Class.forName("net.minecraft.client.settings.GameSettings");
            fKeyBindSneak = findField(settings, "keyBindSneak", "field_74311_E");

            Class<?> keyBinding = Class.forName("net.minecraft.client.settings.KeyBinding");
            fPressed = findField(keyBinding, "pressed", "field_74513_e");
            try {
                mGetIsKeyPressed = keyBinding.getMethod("func_151470_d");
            } catch (Throwable ignored) {
                try {
                    mGetIsKeyPressed = keyBinding.getMethod("getIsKeyPressed");
                } catch (Throwable ignored2) {
                    mGetIsKeyPressed = null;
                }
            }

            Class<?> replayModReplay = Class.forName("com.replaymod.replay.ReplayModReplay");
            // public static FIELD "instance"; there is no getInstance() on this class.
            fReplayModInstance = replayModReplay.getField("instance");
            mGetReplayHandler = replayModReplay.getMethod("getReplayHandler");

            Class<?> handler = Class.forName("com.replaymod.replay.ReplayHandler");
            mGetOverlay = handler.getMethod("getOverlay");
            mSpectateCamera = handler.getMethod("spectateCamera");

            Class<?> overlay = Class.forName(
                    "com.replaymod.lib.de.johni0702.minecraft.gui.container.AbstractGuiOverlay");
            mSetMouseVisible = overlay.getMethod("setMouseVisible", boolean.class);
        } catch (Throwable t) {
            broken = true;
            System.out.println("[replaycompat] Shift+ESC reset unavailable (reflection setup): " + t);
        }
    }

    private static Field findField(Class<?> type, String... names) throws NoSuchFieldException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (String name : names) {
                try {
                    Field f = c.getDeclaredField(name);
                    f.setAccessible(true);
                    return f;
                } catch (NoSuchFieldException ignored) {
                    // try the next spelling / superclass
                }
            }
        }
        throw new NoSuchFieldException(java.util.Arrays.toString(names) + " on " + type.getName());
    }

    private static Method findMethod(Class<?> type, Class<?>[] params, String... names) throws NoSuchMethodException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (String name : names) {
                try {
                    Method m = c.getDeclaredMethod(name, params);
                    m.setAccessible(true);
                    return m;
                } catch (NoSuchMethodException ignored) {
                    // try the next spelling / superclass
                }
            }
        }
        throw new NoSuchMethodException(java.util.Arrays.toString(names) + " on " + type.getName());
    }
}
