package com.gtnh.replaycompat.mixin;

import com.replaymod.lib.de.johni0702.minecraft.gui.container.AbstractGuiOverlay;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Makes the replay overlay's mouse usable, and keeps the normal "mouse grabbed = free camera"
 * behaviour the rest of the time.
 *
 * <h3>Symptom</h3>
 *
 * <p>In a replay the timeline and controls are drawn but nothing reacts to the mouse; and if the mouse
 * is forced visible, the camera can no longer be turned by moving the mouse.
 *
 * <h3>Root cause (measured, HANDOVER 12.22 / 12.23)</h3>
 *
 * <p>{@code AbstractGuiOverlay} only installs its input screen while {@code mouseVisible} is set:
 *
 * <pre>
 * if (visible) {
 *     if (mouseVisible) { if (mc.currentScreen == null) mc.displayGuiScreen(userInputGuiScreen); }
 *     else              { if (mc.currentScreen == userInputGuiScreen) mc.displayGuiScreen(null); }
 * }
 * </pre>
 *
 * <p>Nothing on GTNH ever sets {@code mouseVisible}. Upstream only does so from
 * {@code GuiReplayOverlay$EventHandler}, and that path is measurably dead on this pack (see 12.23).
 *
 * <h3>Two ways to reveal the mouse</h3>
 *
 * <ul>
 *   <li><b>tap Ctrl</b> (the sprint key) - a lock/toggle: the mouse stays visible until you tap Ctrl
 *       again. This is the primary way; it costs no held key and has no side effect in a replay;</li>
 *   <li><b>hold Shift</b> (the sneak key) - temporary: releasing grabs the mouse again. Kept because it
 *       is the behaviour that was validated first.</li>
 * </ul>
 *
 * <p>Both feed one expression, {@code target = ctrlToggle || shiftHeld}, applied only when it changes.
 *
 * <h3>Why Ctrl, and why not a mouse button or another key</h3>
 *
 * <p>Measured on this pack (HANDOVER 12.23): the chat key <b>T</b> is stolen by another mod, <b>B</b> is
 * stolen by JourneyMap, and every mouse-button route is unusable -
 * {@code Mouse.isButtonDown(2)} reads a dead SDL array, polling {@code keyBindPickBlock} loses releases
 * to LWJGL2's event queue, and {@code InputEvent.MouseInputEvent} never reaches a listener at all.
 * Ctrl is unbound by any of that and has no visible effect in a replay.
 *
 * <h3>Why every held key needs a state write-back</h3>
 *
 * <p>{@code setMouseVisible(true)} calls {@code Minecraft.displayGuiScreen(userInputGuiScreen)}, which
 * calls {@code setIngameNotInFocus()} and therefore {@code KeyBinding.unPressAllKeys()} - clearing the
 * very key state that produced the {@code true}. Without a write-back the overlay flips every tick
 * (v39: 400 pairs in 21 s), and a toggle would re-fire endlessly. So after applying a change, every key
 * this patch believes is held is put back with {@code KeyBinding.setKeyBindState}. A real release still
 * wins, because the game sets the state to {@code false} on the actual key-up event.
 *
 * <h3>Why not the high-version approach, and why not {@code InputEvents}</h3>
 *
 * <ul>
 *   <li>The high-version hook is {@code @Mixin(net.minecraft.client.Keyboard)} around a GLFW callback.
 *       This pack has <b>no GLFW at all</b> (lwjgl3ify 3.0.31 drives SDL3) and MC 1.7.10 has no
 *       {@code Keyboard} class - both halves are missing (HANDOVER pit 49).</li>
 *   <li>{@code me.eigenraven.lwjgl3ify.api.InputEvents} only exposes <b>keyboard</b> listeners; it has
 *       <b>no mouse API</b>.</li>
 * </ul>
 *
 * <h3>Why reflection</h3>
 *
 * <p>{@code replaymod-compat} is a plain {@code javac} project with <b>no reobf</b>, so every MCP member
 * name of a Minecraft class is a runtime {@code NoSuchFieldError}/{@code NoSuchMethodError}
 * (HANDOVER pit 29). Both the MCP and the SRG spelling are tried, and every access - including the
 * logging that reports it - lives inside a {@code try} so a failure can never escape as an
 * {@code Error}.
 */
@Mixin(targets = "com.replaymod.lib.de.johni0702.minecraft.gui.container.AbstractGuiOverlay")
public abstract class MixinGuiReplayOverlayMouseVisible {

    private static final String TAG = "[replaycompat] ";

    /** Name of the only overlay we touch - the recording indicator must keep its own behaviour. */
    private static final String REPLAY_OVERLAY = "com.replaymod.replay.gui.overlay.GuiReplayOverlay";

    /** Hard cap on the number of *state change* log lines this patch may print. */
    private static final int LOG_LIMIT = 200;

    /** Separate, much smaller budget for the periodic diagnostic probe. */
    private static final int PROBE_LOG_LIMIT = 20;

    /** If more than this many switches happen inside one second, the patch gives up. */
    private static final int MAX_SWITCHES_PER_WINDOW = 6;

    /** Diagnostic probe cadence. */
    private static final long PROBE_INTERVAL_MS = 5000L;

    /**
     * How many consecutive frames of "locked, but no input screen mounted" we tolerate before releasing
     * the lock by ourselves (HANDOVER 12.45).
     *
     * <p>12.38 taught this patch to keep the player's lock when the input screen closes, which fixed the
     * strobing - but it kept {@code mouseVisible} true even when the screen never came back, so the flag
     * said "mouse visible" while the mouse was actually grabbed (reported as a stuck mouse after using
     * the player list). This grace period makes the state self-consistent: a spectator switch gets a
     * moment to re-mount the screen, and a lock that cannot take effect is dropped instead of lying.
     */
    /**
     * Length of one consistency-check window, and how many consecutive "mostly unmounted" windows are
     * tolerated before the lock is dropped by ourselves (HANDOVER 12.45, rewritten in 12.49).
     *
     * <p>12.45 counted <b>100 consecutive frames</b>. The 2026-10-11 tester logs killed that design:
     * both contain ~185 "locked, but no input screen is mounted" lines and <b>zero</b> grace-period
     * releases, because the input screen kept being mounted and unmounted again and every re-mount
     * reset the counter. A ratio over a fixed time window is immune to that strobing - it catches both
     * "never mounted" and "mounted half the time".
     */
    private static final long UNMOUNT_WINDOW_MS = 1000L;
    private static final int UNMOUNT_BAD_WINDOWS = 2;

    private static Method methodGetMinecraft;
    private static Field fieldGameSettings;
    private static Field fieldKeySneak;
    private static Field fieldKeySprint;
    private static Method methodIsHeld;          // getIsKeyPressed / func_151470_d
    private static Method methodSetKeyBindState; // setKeyBindState / func_74510_a
    private static Method methodGetKeyCode;      // getKeyCode / func_151463_i
    private static Field fieldCurrentScreen;     // currentScreen / field_71462_r
    private static Field fieldVisible;           // AbstractGuiOverlay.visible
    private static boolean resolved;

    private static int switches;
    private static int logCount;
    private static int probeLogCount;
    private static long windowStart;
    private static int windowCount;
    private static boolean runawayReported;
    private static long lastProbe;

    /** Applied state: what {@code mouseVisible} was last set to. */
    private boolean replaycompat$mouseKeyHeld = false;

    /** Lock state driven by tapping Ctrl. */
    private boolean replaycompat$ctrlToggle = false;

    /** Previous Ctrl state, for edge detection. */
    private boolean replaycompat$ctrlWasDown = false;

    /** Per-frame state of the consistency check (12.49): frames and unmounted frames per window. */
    private long replaycompat$statWindowStart = 0L;
    private int replaycompat$statFrames = 0;
    private int replaycompat$statUnmounted = 0;
    private int replaycompat$badWindows = 0;
    /** How many times we already explained an ineffective lock; keeps the log honest but quiet. */
    private int replaycompat$ineffectiveReported = 0;

    @Inject(method = "updateUserInputGui", at = @At("HEAD"), remap = false, require = 0)
    private void replaycompat$pollReplayMouseKey(CallbackInfo ci) {
        try {
            if (!REPLAY_OVERLAY.equals(this.getClass().getName())) {
                return; // not the replay overlay
            }
            AbstractGuiOverlay<?> self = (AbstractGuiOverlay<?>) (Object) this;
            long now = System.currentTimeMillis();

            // ---- Ctrl: tap to lock the mouse visible / grabbed ----
            boolean ctrlDown = replaycompat$isHeld(fieldKeySprint);
            if (ctrlDown && !replaycompat$ctrlWasDown) {   // press edge
                replaycompat$ctrlToggle = !replaycompat$ctrlToggle;
                replaycompat$ctrlWasDown = true;           // stays true until a release is actually seen
                replaycompat$log("replay mouse: Ctrl tap -> "
                        + (replaycompat$ctrlToggle ? "mouse LOCKED VISIBLE" : "mouse LOCKED GRABBED"));
            } else if (!ctrlDown) {
                replaycompat$ctrlWasDown = false;
            }

            // ---- combine with the Shift hold ----
            boolean shiftHeld = replaycompat$isHeld(fieldKeySneak);
            // ★ Shift is deliberately NOT part of the decision any more. Upstream uses keyBindSneak
            // as its "stop spectating" key - SpectatorCameraController#update calls
            // ReplayHandler.spectateCamera() when that binding isPressed(). Occupying it broke
            // "leave the player's view", and writing key state back on every toggle also wiped the
            // movement keys (displayGuiScreen -> unPressAllKeys), which is what scrambled movement.
            // Ctrl alone is enough - it has no other meaning inside a replay.
            // Shift+ESC force reset (HANDOVER 12.44): drop the lock so the mouse is grabbed again and
            // the free camera can be flown. Requested from the Minecraft mixin (ESC's entry point),
            // consumed here because the toggle lives in this class.
            if (com.gtnh.replaycompat.util.ReplayMouseState.consumeResetRequest()) {
                replaycompat$ctrlToggle = false;
                replaycompat$ctrlWasDown = false;
                replaycompat$mouseKeyHeld = false;
                lastProbe = 0L;
            }

            boolean target = replaycompat$ctrlToggle;

            // ★ Consistency check, rewritten in 12.49 from bytecode + two tester logs.
            //
            // Why the lock can be ineffective at all (AbstractGuiOverlay.updateUserInputGui):
            //     if (visible) { if (mouseVisible) { if (mc.currentScreen == null) display(userInput); } }
            // The player list and the keyframe repository hide the overlay while they are open
            // (PlayerOverviewGui.<init>: iconst_0 -> GuiReplayOverlay.setVisible; close(): iconst_1),
            // and with visible == false the whole block is skipped - so "locked" is a lie no matter
            // what we set, and the mouse stays grabbed. Both tester logs show exactly this state:
            // ~185 lines of "no input screen is mounted (currentScreen=null)".
            boolean mounted = replaycompat$inputScreenMounted();
            boolean overlayVisible = replaycompat$overlayVisible();
            boolean ineffective = false;
            String why = "";

            if (target) {
                if (!overlayVisible) {
                    // Give way: whatever hid the overlay manages the mouse itself, so drop the lock
                    // instead of holding a flag that can never take effect.
                    ineffective = true;
                    why = "the overlay is hidden (visible=false, currentScreen="
                            + replaycompat$currentScreenName() + ")";
                } else if (!mounted) {
                    // Ratio over a time window instead of a run of consecutive frames (see constants).
                    if (replaycompat$statWindowStart == 0L) {
                        replaycompat$statWindowStart = now;
                    }
                    replaycompat$statFrames++;
                    replaycompat$statUnmounted++;
                    if (replaycompat$statFrames == 1) {
                        replaycompat$log("replay mouse: locked, but no input screen is mounted"
                                + " (visible=" + overlayVisible + ", currentScreen="
                                + replaycompat$currentScreenName() + ")");
                    }
                    if (now - replaycompat$statWindowStart >= UNMOUNT_WINDOW_MS) {
                        boolean mostlyUnmounted = replaycompat$statUnmounted * 2 > replaycompat$statFrames;
                        replaycompat$badWindows = mostlyUnmounted ? replaycompat$badWindows + 1 : 0;
                        if (replaycompat$badWindows >= UNMOUNT_BAD_WINDOWS) {
                            ineffective = true;
                            why = "no input screen for " + replaycompat$badWindows + " window(s) (visible="
                                    + overlayVisible + ", currentScreen=" + replaycompat$currentScreenName() + ")";
                        }
                        replaycompat$statWindowStart = now;
                        replaycompat$statFrames = 0;
                        replaycompat$statUnmounted = 0;
                    }
                } else {
                    replaycompat$resetUnmountStats();
                }
            } else {
                replaycompat$resetUnmountStats();
            }

            if (ineffective) {
                replaycompat$ctrlToggle = false;
                replaycompat$ctrlWasDown = false;
                target = false;
                if (replaycompat$ineffectiveReported++ < 5) {
                    replaycompat$log("replay mouse: the lock cannot take effect - " + why
                            + ", releasing it (press Ctrl to lock again)");
                }
            }

            // Publish the player's intent every frame, not just on the press edge: the flag is
            // static (the popup mixin lives in another class) while ctrlToggle belongs to this
            // overlay INSTANCE, so a fresh overlay after leaving a replay would otherwise inherit a
            // stale "locked" value. Upstream AbstractGuiPopup snapshots the overlay's mouseVisible
            // and restores it on close -- which is what grabbed the mouse back after every popup
            // (12.36), so it has to see what the player actually asked for.
            com.gtnh.replaycompat.util.ReplayMouseState.lockedVisible = target;
            // The same publish also tells the ESC hook whether the lock actually took effect, so a
            // plain ESC can rescue a session whose lock can never be mounted (12.49).
            com.gtnh.replaycompat.util.ReplayMouseState.lockIneffective = target && !mounted;

            if (now - lastProbe >= PROBE_INTERVAL_MS) {
                lastProbe = now;
                replaycompat$log("replay mouse probe: ctrl=" + ctrlDown + " ctrlWas=" + replaycompat$ctrlWasDown
                        + " toggle=" + replaycompat$ctrlToggle
                        + " shift=" + shiftHeld
                        + " target=" + target
                        + " applied=" + replaycompat$mouseKeyHeld);
            }

            // ★ Decide against the overlay's REAL state, not against the value we last wrote
            // (HANDOVER 12.34). Rendering -- and anything else that ends up calling
            // displayGuiScreen/setIngameFocus -- can flip the overlay's flag behind our back. With
            // the old "compare to our own record" test we would then never correct it again, which
            // is exactly the reported "then rendering stole my mouse". self.isMouseVisible() is the
            // overlay's own accessor (the probe log below already used it as evidence).
            boolean actual = self.isMouseVisible();
            if (target == actual) {
                replaycompat$mouseKeyHeld = target; // keep our record in sync for the probe log
                return; // already where the user wants it - the common case
            }

            if (now - windowStart > 1000L) {
                windowStart = now;
                windowCount = 0;
            }
            if (++windowCount > MAX_SWITCHES_PER_WINDOW) {
                if (!runawayReported) {
                    runawayReported = true;
                    replaycompat$log("replay mouse: input strobing, switching disabled");
                }
                return;
            }

            replaycompat$mouseKeyHeld = target; // set before the call: makes the nested call a no-op
            self.setMouseVisible(target);
            // Break the feedback loop: displayGuiScreen() just wiped every KeyBinding, so put ours back.
            replaycompat$restoreState(fieldKeySprint, replaycompat$ctrlWasDown);
            // Shift is deliberately NOT restored: it belongs to upstream's spectate handling.
            // Evidence, not a claim: the state is read back from the overlay itself.
            replaycompat$log("replay mouse " + (target ? "RELEASED - overlay UI is clickable" : "GRABBED - free camera look")
                    + " (mouseVisible=" + self.isMouseVisible() + ", switch #" + (++switches)
                    + ", toggle=" + replaycompat$ctrlToggle + ", shift=" + shiftHeld + ")");
        } catch (Throwable ignored) {
            // never let this patch break the overlay
        }
    }

    /** Puts a binding this patch believes is held back to "held" after the overlay cleared all of them. */
    private static void replaycompat$restoreState(Field field, boolean held) {
        if (!held) {
            return; // releasing is the game's own job
        }
        try {
            if (field == null || methodSetKeyBindState == null || methodGetKeyCode == null) {
                return;
            }
            Object mc = methodGetMinecraft.invoke(null);
            Object binding = field.get(fieldGameSettings.get(mc));
            Object keyCode = methodGetKeyCode.invoke(binding);
            methodSetKeyBindState.invoke(null, keyCode, Boolean.TRUE);
        } catch (Throwable ignored) {
            // best effort only
        }
    }

    private static boolean replaycompat$isHeld(Field field) {
        try {
            if (!resolved) {
                replaycompat$resolveMembers();
            }
            if (field == null || methodIsHeld == null) {
                return false;
            }
            Object mc = methodGetMinecraft.invoke(null);
            Object binding = field.get(fieldGameSettings.get(mc));
            Object result = methodIsHeld.invoke(binding);
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable t) {
            return false;
        }
    }

    /** One-shot resolution of the members we need, with a single report of what was found. */
    /**
     * True when the overlay's own input screen is the current screen, i.e. the mouse really is free.
     *
     * <p>Returns {@code true} when it cannot tell (fields unresolved): a guard that cannot check must
     * never change behaviour.
     */
    private static boolean replaycompat$inputScreenMounted() {
        try {
            if (fieldCurrentScreen == null || methodGetMinecraft == null) {
                return true;
            }
            Object screen = fieldCurrentScreen.get(methodGetMinecraft.invoke(null));
            if (screen == null) {
                return false;
            }
            return screen.getClass().getName().contains("UserInputGuiScreen");
        } catch (Throwable t) {
            return true;
        }
    }

    /** Class name of the current screen - diagnostics only, so the next report names the culprit. */
    private static String replaycompat$currentScreenName() {        try {
            if (fieldCurrentScreen == null || methodGetMinecraft == null) {
                return "?";
            }
            Object screen = fieldCurrentScreen.get(methodGetMinecraft.invoke(null));
            return screen == null ? "null" : screen.getClass().getName();
        } catch (Throwable t) {
            return "?";
        }
    }

    /**
     * The overlay's own {@code visible} flag. {@code updateUserInputGui()} does nothing at all while
     * it is false, which is how the player list / keyframe repository make our lock ineffective.
     *
     * <p>Returns {@code true} when it cannot tell: a guard that cannot check must not change behaviour.
     */
    private boolean replaycompat$overlayVisible() {
        try {
            if (fieldVisible == null) {
                return true;
            }
            Object value = fieldVisible.get(this);
            return !(value instanceof Boolean) || (Boolean) value;
        } catch (Throwable t) {
            return true;
        }
    }

    private void replaycompat$resetUnmountStats() {
        replaycompat$statWindowStart = 0L;
        replaycompat$statFrames = 0;
        replaycompat$statUnmounted = 0;
        replaycompat$badWindows = 0;
    }

    private static void replaycompat$resolveMembers() {
        resolved = true;
        try {
            Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
            methodGetMinecraft = optionalMethod(mcClass, "getMinecraft", "func_71410_x");
            fieldGameSettings = optionalField(mcClass, "gameSettings", "field_71474_y");
            Class<?> settingsClass = Class.forName("net.minecraft.client.settings.GameSettings");
            fieldKeySneak = optionalField(settingsClass, "keyBindSneak", "field_74311_E");
            fieldKeySprint = optionalField(settingsClass, "keyBindSprint", "field_151444_V");
            Class<?> keyBindingClass = Class.forName("net.minecraft.client.settings.KeyBinding");
            methodIsHeld = optionalMethod(keyBindingClass, "getIsKeyPressed", "func_151470_d");
            methodSetKeyBindState = optionalMethod(keyBindingClass, new Class<?>[] { int.class, boolean.class },
                    "setKeyBindState", "func_74510_a");
            methodGetKeyCode = optionalMethod(keyBindingClass, "getKeyCode", "func_151463_i");
            fieldCurrentScreen = optionalField(mcClass, "currentScreen", "field_71462_r");
            Class<?> selfClass = Class.forName(
                    "com.replaymod.lib.de.johni0702.minecraft.gui.container.AbstractGuiOverlay");
            fieldVisible = optionalFieldDeep(selfClass, "visible");
            replaycompat$log("replay mouse resolver: mc=" + (methodGetMinecraft != null)
                    + " settings=" + (fieldGameSettings != null)
                    + " sneak=" + (fieldKeySneak != null)
                    + " sprint=" + (fieldKeySprint != null)
                    + " isHeld=" + (methodIsHeld != null)
                    + " setState=" + (methodSetKeyBindState != null)
                    + " keyCode=" + (methodGetKeyCode != null));
        } catch (Throwable t) {
            replaycompat$log("replay mouse resolver failed: " + t);
        }
    }

    /**
     * Rate-limited logging. The periodic probe has its own small budget so that it can never crowd out
     * the state-change lines - that mistake cost a round in v44, where the probe consumed the whole
     * {@code LOG_LIMIT} and the actual toggle lines were silently dropped.
     */
    private static void replaycompat$log(String message) {
        try {
            if (message.startsWith("replay mouse probe")) {
                if (probeLogCount >= PROBE_LOG_LIMIT) {
                    return;
                }
                probeLogCount++;
            } else {
                if (logCount >= LOG_LIMIT) {
                    return;
                }
                logCount++;
            }
            System.out.println(TAG + message);
        } catch (Throwable ignored) {
            // logging must never escape
        }
    }

    /** Both the MCP and the SRG spelling are tried; a miss is not an error, it just returns {@code null}. */
    private static Method optionalMethod(Class<?> owner, String... names) {
        return optionalMethod(owner, new Class<?>[0], names);
    }

    private static Method optionalMethod(Class<?> owner, Class<?>[] params, String... names) {
        for (String name : names) {
            try {
                Method method = owner.getDeclaredMethod(name, params);
                method.setAccessible(true);
                return method;
            } catch (Throwable ignored) {
                // try the next spelling
            }
        }
        return null;
    }

    /** Like {@link #optionalField} but walks up the superclass chain ({@code visible} is inherited). */
    private static Field optionalFieldDeep(Class<?> owner, String name) {
        for (Class<?> c = owner; c != null; c = c.getSuperclass()) {
            try {
                Field field = c.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (Throwable ignored) {
                // keep walking up
            }
        }
        return null;
    }

    private static Field optionalField(Class<?> owner, String... names) {
        for (String name : names) {
            try {
                Field field = owner.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (Throwable ignored) {
                // try the next spelling
            }
        }
        return null;
    }
}
