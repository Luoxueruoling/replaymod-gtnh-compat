package com.gtnh.replaycompat.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Keeps the camera speed the player dialled in alive across dimension changes and camera switches.
 *
 * <h3>Symptom (three field rounds: v87, v91, v92)</h3>
 *
 * <p>"跨维度之后已经调整过的速度会重置" / "换世界和从人物身上上去相机速度都会被重置".
 *
 * <h3>Where the speed actually lives (this took three wrong guesses - see HANDOVER 12.73)</h3>
 *
 * <p>There are <b>three</b> camera controllers and they store the speed differently. Everything below
 * is from {@code javap -c} on the shipped jar:
 *
 * <pre>
 * ClassicCameraController     private double MAX_SPEED = 10.0      increase/decrease: +/- 0.5, range [2, 20]
 *                             (setCameraMaximumSpeed also recomputes THRESHOLD and DECAY)
 * VanillaCameraController     private static final int MAX_SPEED = 1000   &lt;-- an UPPER BOUND, static
 *                             private static final int MIN_SPEED = -1000
 *                             private int speed;               &lt;-- the real value, default 0
 *                             increaseSpeed(): speed = min(1000, speed + 1)
 *                             decreaseSpeed(): speed = max(-1000, speed - 1)
 * SpectatorCameraController   neither - its increase/decreaseSpeed are literally {@code return}
 * </pre>
 *
 * <p><b>v90 and v92 read {@code MAX_SPEED} by name only.</b> With the Vanilla controller - which is
 * what the tester actually uses - that resolves to the <em>static constant</em> 1000, so the observed
 * "speed" never changed, the player's own edits were never noticed, and the fallback could only replay
 * wheel notches (clamped too tightly) - i.e. it looked like it ran and still did nothing. The v92
 * diagnostic line is the fossil of exactly that:
 * {@code controller=VanillaCameraController maxSpeedReadable=true speed=1000.0}.
 *
 * <h3>What this does (v93)</h3>
 *
 * <p>It finds the controller's <b>instance</b> speed field, skipping static members, preferring
 * {@code MAX_SPEED} (Classic) and otherwise a numeric {@code speed} (Vanilla). Then:
 *
 * <ol>
 *   <li>while nothing is rebuilt, any change of that field is taken as the player editing the speed
 *       (wheel, upstream's own keys) and remembered;</li>
 *   <li>when the world object or the controller instance changes (dimension change, leaving a
 *       spectated player, replay restart), the remembered value is stepped back in using upstream's
 *       own {@code increaseSpeed()/decreaseSpeed()} - stepping rather than writing the field directly,
 *       so {@code ClassicCameraController}'s derived THRESHOLD/DECAY stay consistent. The step size is
 *       taken from the field type (0.5 for the double field, 1 for the int one);</li>
 *   <li>a controller with no instance speed field at all (Spectator) falls back to replaying the net
 *       wheel notches, and says so instead of returning silently (pit 72).</li>
 * </ol>
 *
 * <p>The probe line now names the field it picked, so the next log tells the whole story:
 * {@code camera speed: controller=… speedField=speed speed=12.0 …}.
 *
 * <p>Everything reflective on purpose: plain javac, no reobf (HANDOVER pit 29). Driven from
 * {@code Minecraft.runTick}, the only hook guaranteed to run during a replay (pit 59).
 */
public final class ReplayCameraSpeed {

    private ReplayCameraSpeed() {
    }

    /** Only differences above this count as a change. */
    private static final double EPSILON = 0.001d;

    /** Hard bound for the notch-replay fallback and for stepping (Vanilla's own range is +/-1000). */
    private static final int MAX_STEPS = 2000;

    private static boolean resolved;
    private static boolean unavailable;

    private static Field fTheWorld;
    private static Field fModInstance;
    private static Method mGetReplayHandler;
    private static Method mGetCameraEntity;
    private static Method mGetCameraController;
    private static Method mIncreaseSpeed;
    private static Method mDecreaseSpeed;
    private static Object modInstance;

    private static Object lastWorld;
    private static Object lastController;

    /** The speed the player picked, in the units of whichever field the controller uses. */
    private static Double userSpeed;
    private static double lastSeen = Double.NaN;

    /** Net wheel notches applied while replaying - the fallback recipe for controllers without a field. */
    private static int netSteps;

    private static int restoreLogs;
    private static boolean probed;
    private static boolean notedNoSpeedField;
    private static boolean notedUserSpeed;

    /**
     * Per-tick upkeep, called from {@link ReplayMouseState#tick(Object)} before any of the mouse-lock
     * checks (the speed has to survive even while the mouse is not locked).
     *
     * @param minecraft the {@code Minecraft} instance (passed in so this class needs no MC types)
     */
    public static void tick(Object minecraft) {
        try {
            if (unavailable || minecraft == null) {
                return;
            }
            if (!resolve()) {
                return;
            }
            Object world = fTheWorld.get(minecraft);
            if (world == null) {
                lastWorld = null; // left the world: nothing to remember against
                return;
            }
            Object handler = modInstance == null ? null : mGetReplayHandler.invoke(modInstance);
            if (handler == null) {
                lastWorld = world;
                lastController = null;
                return; // not replaying
            }
            Object camera = mGetCameraEntity.invoke(handler);
            if (camera == null) {
                return;
            }
            Object controller = mGetCameraController.invoke(camera);
            if (controller == null) {
                return;
            }

            // The instance field, NOT the static upper bound that shares the MAX_SPEED name.
            Field speedField = findSpeedField(controller.getClass());
            double speed = speedField == null ? Double.NaN : read(speedField, controller);

            if (!probed) {
                probed = true;
                System.out.println("[replaycompat] camera speed: controller="
                        + controller.getClass().getName()
                        + " speedField=" + (speedField == null ? "none" : speedField.getName()
                                + "(" + speedField.getType().getSimpleName() + ")")
                        + " speed=" + (speedField == null ? "n/a" : String.valueOf(speed))
                        + " wheelSteps=" + netSteps);
            }
            if (speedField == null && !notedNoSpeedField) {
                notedNoSpeedField = true;
                System.out.println("[replaycompat] camera speed: " + controller.getClass().getSimpleName()
                        + " has no instance speed field - that controller keeps no speed of its own, so"
                        + " the wheel setting can only be re-applied as raw notches after a rebuild"
                        + " (upstream's own increase/decreaseSpeed are empty methods there)");
            }

            boolean worldChanged = world != lastWorld;
            boolean controllerChanged = controller != lastController;
            boolean rebuilt = worldChanged || controllerChanged;

            if (rebuilt && (speedField != null ? userSpeed != null : netSteps != 0)) {
                double restored = Double.NaN;
                if (speedField != null && userSpeed != null) {
                    restored = stepTo(controller, speedField, userSpeed.doubleValue(), speed);
                } else if (netSteps != 0) {
                    applyNotches(controller, netSteps);
                    if (speedField != null) {
                        restored = read(speedField, controller);
                    }
                }
                if (restoreLogs < 6) {
                    restoreLogs++;
                    System.out.println("[replaycompat] camera speed: the "
                            + (worldChanged ? "world" : "camera controller")
                            + " changed (controller=" + controller.getClass().getSimpleName()
                            + (Double.isNaN(speed) ? "" : ", speed was " + speed)
                            + ") - restored the wheel setting"
                            + (Double.isNaN(restored) ? " (re-applied " + netSteps + " notch(es))"
                                    : " " + restored)
                            + " across " + netSteps + " wheel step(s)");
                }
                if (speedField != null) {
                    speed = read(speedField, controller);
                }
            } else if (!rebuilt && speedField != null && !Double.isNaN(lastSeen)
                    && Math.abs(speed - lastSeen) > EPSILON) {
                // Nothing was rebuilt, so the player (wheel or upstream's own keys) changed it: remember.
                userSpeed = Double.valueOf(speed);
                if (!notedUserSpeed) {
                    notedUserSpeed = true;
                    System.out.println("[replaycompat] camera speed: the player set the camera speed to "
                            + speed + " (field " + speedField.getName() + ") - it will be kept across"
                            + " dimension changes and camera switches");
                }
            }

            lastWorld = world;
            lastController = controller;
            lastSeen = speed;
        } catch (Throwable ignored) {
            // An enhancement: it must never disturb the game loop.
        }
    }

    /**
     * Records that the wheel patch just handed {@code steps} notches to upstream. Only counted while a
     * replay is running, so ordinary gameplay wheel use cannot pollute the fallback recipe.
     */
    public static void noteWheelSteps(int steps) {
        try {
            if (steps == 0 || unavailable || !resolve() || !isReplaying()) {
                return;
            }
            int total = netSteps + steps;
            if (total > MAX_STEPS) {
                total = MAX_STEPS;
            } else if (total < -MAX_STEPS) {
                total = -MAX_STEPS;
            }
            netSteps = total;
        } catch (Throwable ignored) {
            // diagnostics only
        }
    }

    /** Current net wheel notches (diagnostics / future commands). */
    public static int getWheelSteps() {
        return netSteps;
    }

    /** The speed the player last chose, if one has been observed. */
    public static double getUserSpeedOrNaN() {
        return userSpeed == null ? Double.NaN : userSpeed.doubleValue();
    }

    private static boolean isReplaying() {
        try {
            return modInstance != null && mGetReplayHandler.invoke(modInstance) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    // ------------------------------------------------------------------ speed field plumbing

    /**
     * The controller's <b>instance</b> speed field: {@code MAX_SPEED} when present (ClassicCameraController),
     * otherwise a numeric field called {@code speed} (VanillaCameraController). Static members are skipped
     * on purpose - {@code VanillaCameraController.MAX_SPEED} is a static upper bound and reading it was
     * exactly the v90/v92 bug.
     */
    private static Field findSpeedField(Class<?> type) {
        Field named = null;
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            Field[] fields;
            try {
                fields = c.getDeclaredFields();
            } catch (Throwable t) {
                continue;
            }
            for (Field f : fields) {
                if (Modifier.isStatic(f.getModifiers())) {
                    continue; // the upper bound, not the value
                }
                Class<?> t = f.getType();
                if (t != int.class && t != double.class && t != float.class && t != long.class) {
                    continue;
                }
                String name = f.getName();
                if ("MAX_SPEED".equals(name)) {
                    f.setAccessible(true);
                    return f;
                }
                if (named == null && "speed".equalsIgnoreCase(name)) {
                    named = f;
                }
            }
        }
        if (named != null) {
            named.setAccessible(true);
        }
        return named;
    }

    private static double read(Field field, Object controller) throws Exception {
        return ((Number) field.get(controller)).doubleValue();
    }

    /** One {@code increaseSpeed()} moves a double field by 0.5 and the int field by 1. */
    private static double stepSize(Field field) {
        Class<?> t = field.getType();
        return (t == double.class || t == float.class) ? 0.5d : 1.0d;
    }

    /**
     * Steps the controller's speed to {@code target} using upstream's own methods, and returns what it
     * actually reached. Stepping (instead of writing the field) keeps ClassicCameraController's derived
     * THRESHOLD/DECAY in sync; upstream clamps the range itself.
     */
    private static double stepTo(Object controller, Field field, double target, double current)
            throws Exception {
        if (!Double.isNaN(current)) {
            int steps = (int) Math.round((target - current) / stepSize(field));
            applyNotches(controller, steps);
        }
        return read(field, controller);
    }

    /** Replays a number of wheel notches on the controller (upstream clamps the range itself). */
    private static void applyNotches(Object controller, int steps) throws Exception {
        int n = steps;
        if (n > MAX_STEPS) {
            n = MAX_STEPS;
        } else if (n < -MAX_STEPS) {
            n = -MAX_STEPS;
        }
        for (int i = 0; i < n; i++) {
            mIncreaseSpeed.invoke(controller);
        }
        for (int i = 0; i > n; i--) {
            mDecreaseSpeed.invoke(controller);
        }
    }

    private static boolean resolve() {
        if (resolved) {
            return !unavailable;
        }
        resolved = true;
        try {
            Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
            fTheWorld = findField(mcClass, "theWorld", "field_71441_e");

            Class<?> replayModReplay = Class.forName("com.replaymod.replay.ReplayModReplay");
            fModInstance = replayModReplay.getField("instance");
            mGetReplayHandler = replayModReplay.getMethod("getReplayHandler");
            modInstance = fModInstance.get(null);

            Class<?> handlerClass = Class.forName("com.replaymod.replay.ReplayHandler");
            mGetCameraEntity = handlerClass.getMethod("getCameraEntity");

            Class<?> cameraClass = Class.forName("com.replaymod.replay.camera.CameraEntity");
            mGetCameraController = cameraClass.getMethod("getCameraController");

            Class<?> controllerClass = Class.forName("com.replaymod.replay.camera.CameraController");
            mIncreaseSpeed = controllerClass.getMethod("increaseSpeed");
            mDecreaseSpeed = controllerClass.getMethod("decreaseSpeed");
        } catch (Throwable t) {
            unavailable = true;
            System.out.println("[replaycompat] camera speed persistence unavailable: " + t);
            return false;
        }
        return true;
    }

    private static Field findField(Class<?> type, String... names) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (String name : names) {
                try {
                    Field f = c.getDeclaredField(name);
                    f.setAccessible(true);
                    return f;
                } catch (NoSuchFieldException ignored) {
                    // next spelling / superclass
                } catch (Throwable ignored) {
                    return null;
                }
            }
        }
        return null;
    }
}
