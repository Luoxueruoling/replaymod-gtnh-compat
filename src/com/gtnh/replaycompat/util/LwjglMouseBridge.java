package com.gtnh.replaycompat.util;

import java.lang.reflect.Method;

/**
 * Reads the mouse the way the game itself reads it, on a lwjgl3ify (SDL3) client.
 *
 * <h3>Why this exists (HANDOVER 12.49)</h3>
 *
 * <p>lwjgl3ify ships an {@code org.lwjgl.input.*} compatibility shim, and its {@code GLSMRedirector}-style
 * transformer rewrites <b>bytecode call sites</b> from {@code org.lwjgl.input.Mouse} to the real
 * implementation {@code org.lwjglx.input.Mouse}. A <b>reflective</b> lookup is not a call site, so it
 * keeps landing on the shim - whose state nobody updates.
 *
 * <p>Measured consequence, from a 74 MB tester log with the v74 probe:
 *
 * <pre>
 *   [replaycompat] mouse pos probe: raw=(0,0) display=(2560,1406) scaled=(640,352) gui=(515,142)
 * </pre>
 *
 * <p>{@code raw} was 0 in every one of the 24 samples while {@code gui} moved across the whole screen:
 * the shim's {@code getX()} returns 0. The very same mistake sat in the wheel-speed patch introduced in
 * v70 - it read {@code org.lwjgl.input.Mouse.getEventDWheel()} reflectively, so it always saw 0 and the
 * feature never worked once (the tester reported "scrolling still does not speed up the camera").
 *
 * <p>So: try the real class first and keep the shim only as a fallback. {@code org.lwjglx.input.Mouse}
 * returns framebuffer pixels, which is the same space as {@code Minecraft.displayWidth} - the two have
 * to agree for jGui's {@code MouseUtils.getMousePos()} conversion to be correct.
 *
 * <p>Lives in {@code util} on purpose: classes inside the mixin package cannot reference each other
 * (HANDOVER pit 42).
 */
public final class LwjglMouseBridge {

    /** Real implementation first (lwjgl3ify), shim second (plain LWJGL2 / other packs). */
    private static final String[] CANDIDATES = {
            "org.lwjglx.input.Mouse",
            "org.lwjgl.input.Mouse",
    };

    private static boolean resolved;
    private static Method methodGetX;
    private static Method methodGetY;
    private static Method methodGetEventDWheel;
    private static Method methodGetDWheel;

    private LwjglMouseBridge() {
    }

    /** Framebuffer-space X of the cursor, or 0 when unavailable. */
    public static int getX() {
        return invokeInt(methodGetX);
    }

    /** Framebuffer-space Y of the cursor, or 0 when unavailable. */
    public static int getY() {
        return invokeInt(methodGetY);
    }

    /**
     * Accumulated wheel delta since the last call, <b>consuming</b> it - this is the API to use.
     *
     * <p>Verified 2026-10-11 (HANDOVER 12.55) from lwjgl3ify's bytecode:
     * {@code addMoveEvent} does {@code dwheel += (wheel - lastWheel)} and only {@code <clinit>} zeroes
     * it, so the value is always a truthful "wheel moved by N since you last asked".
     * {@link #getEventDWheel()} instead reads {@code wheelEvents[queue.getCurrentPos()]}, i.e. it is
     * non-zero <b>only while the current event happens to be the wheel event</b> - and since every mouse
     * move also goes through that queue, in practice it reads 0. That is why both the old TAIL hook and
     * the HEAD hook (v77) never saw a wheel.
     */
    public static int getDWheel() {
        return invokeInt(methodGetDWheel);
    }

    /**
     * Wheel delta of the current mouse event (0 when there is none). Kept for diagnostics; prefer
     * {@link #getDWheel()} - see there for why this one is not usable here.
     */
    public static int getEventDWheel() {
        return invokeInt(methodGetEventDWheel);
    }

    /** Which class the reflection actually bound to - for diagnostics only. */
    public static String resolvedOn() {
        resolve();
        return methodGetX == null ? "none" : methodGetX.getDeclaringClass().getName();
    }

    public static boolean isAvailable() {
        resolve();
        return methodGetX != null;
    }

    private static int invokeInt(Method method) {
        resolve();
        if (method == null) {
            return 0;
        }
        try {
            Object value = method.invoke(null);
            return value instanceof Integer ? (Integer) value : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        for (String name : CANDIDATES) {
            try {
                Class<?> type = Class.forName(name);
                Method x = type.getMethod("getX");
                Method y = type.getMethod("getY");
                Method wheel = type.getMethod("getEventDWheel");
                x.setAccessible(true);
                y.setAccessible(true);
                wheel.setAccessible(true);
                methodGetX = x;
                methodGetY = y;
                methodGetEventDWheel = wheel;
                try {
                    methodGetDWheel = type.getMethod("getDWheel");
                    methodGetDWheel.setAccessible(true);
                } catch (Throwable ignored) {
                    methodGetDWheel = null; // older shims may not have it
                }
                return;
            } catch (Throwable ignored) {
                // try the next candidate
            }
        }
    }
}
