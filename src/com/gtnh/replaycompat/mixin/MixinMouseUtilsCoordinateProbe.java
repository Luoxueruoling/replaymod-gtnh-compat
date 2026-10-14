package com.gtnh.replaycompat.mixin;

import com.gtnh.replaycompat.util.LwjglMouseBridge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Diagnostic probe for "clicking the timeline jumps to the end" (HANDOVER 12.48).
 *
 * <p>jGui converts the raw mouse position with
 * {@code x = Mouse.getX() * scaledWidth / Minecraft.displayWidth} - a formula that assumes
 * {@code Mouse.getX()} and the Display width are the same kind of pixel. On this pack the mouse comes
 * from SDL while the Display values come through lwjgl3ify, and the environment is 150% DPI (12.14
 * measured exactly this: virtualised client size 569x320 vs real 854x480). If the two disagree, the
 * computed x is stretched and every click lands past the end of the timeline - which is exactly what
 * is reported ("clicking the timeline jumps straight to the end", and it survived the 12.38/12.45
 * mouse-lock fixes, so it is not about the input screen being rebuilt).
 *
 * <p>Rather than guess the direction, this prints all four numbers once per interval:
 *
 * <pre>
 *   [replaycompat] mouse pos probe: raw=(…,…) display=(…,…) scaled=(…,…) gui=(…,…)
 * </pre>
 *
 * From that, {@code gui.x / scaled.x} versus {@code raw.x / display.x} immediately shows whether the
 * two pixel spaces agree, and by what factor they differ.
 *
 * <p>Read-only and rate limited; it changes no behaviour whatsoever.
 */
@Mixin(targets = "com.replaymod.lib.de.johni0702.minecraft.gui.utils.MouseUtils")
public abstract class MixinMouseUtilsCoordinateProbe {

    private static final String TAG = "[replaycompat] ";
    private static final int LOG_LIMIT = 24;
    /** Only report every Nth call so a moving mouse cannot flood the log. */
    private static final int EVERY = 40;

    private static Field mcField;
    private static Field displayWidthField;
    private static Field displayHeightField;
    private static Method getScaledDimensions;
    private static Method pointGetX;
    private static Method pointGetY;
    private static boolean resolved;
    private static boolean broken;
    private static int calls;
    private static int logged;

    @Inject(method = "getMousePos", at = @At("RETURN"), remap = false, require = 0)
    private static void replaycompat$probeMouseCoordinates(CallbackInfoReturnable<Object> cir) {
        try {
            if (broken || logged >= LOG_LIMIT) {
                return;
            }
            if (++calls % EVERY != 0) {
                return;
            }
            replaycompat$resolve();
            if (broken) {
                return;
            }

            Object mc = mcField.get(null);
            Object scaled = getScaledDimensions.invoke(null);
            Object point = cir.getReturnValue();

            logged++;
            System.out.println(TAG + "mouse pos probe: raw=(" + LwjglMouseBridge.getX() + ","
                    + LwjglMouseBridge.getY() + ") [" + LwjglMouseBridge.resolvedOn() + "]"
                    + " display=(" + displayWidthField.getInt(mc) + "," + displayHeightField.getInt(mc) + ")"
                    + " scaled=(" + pointGetX.invoke(scaled) + "," + pointGetY.invoke(scaled) + ")"
                    + " gui=(" + pointGetX.invoke(point) + "," + pointGetY.invoke(point) + ")");
        } catch (Throwable ignored) {
            // never disturb the real code path
        }
    }

    private static void replaycompat$resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            Class<?> mouseUtils = Class.forName("com.replaymod.lib.de.johni0702.minecraft.gui.utils.MouseUtils");
            mcField = mouseUtils.getDeclaredField("mc");
            mcField.setAccessible(true);
            getScaledDimensions = mouseUtils.getMethod("getScaledDimensions");

            Class<?> mc = Class.forName("net.minecraft.client.Minecraft");
            displayWidthField = findField(mc, "displayWidth", "field_71443_c");
            displayHeightField = findField(mc, "displayHeight", "field_71440_d");

            // Mouse state comes from LwjglMouseBridge: a reflective Class.forName("org.lwjgl.input.Mouse")
            // lands on lwjgl3ify's compatibility shim, whose getX() is always 0 (12.49).
            Class<?> point = Class.forName("com.replaymod.lib.de.johni0702.minecraft.gui.utils.lwjgl.Point");
            pointGetX = point.getMethod("getX");
            pointGetY = point.getMethod("getY");
        } catch (Throwable t) {
            broken = true;
            System.out.println(TAG + "mouse pos probe unavailable: " + t);
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
}
