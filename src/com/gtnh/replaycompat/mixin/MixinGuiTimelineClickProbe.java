package com.gtnh.replaycompat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Read-only probe for "the big timeline cannot be moved / only moves forward" (HANDOVER 12.52).
 *
 * <p>Reported 2026-10-11 by both testers: "the bar on top does not move at all", "only forward works",
 * "going backwards jumps straight to the end/beginning". The conversion itself is known to be correct
 * (12.50 verified {@code gui = raw * scaled / display} numerically), so the suspicion falls on the
 * timeline's own viewport state - {@code getTimeAt} is:
 *
 * <pre>
 * if (getLastSize() == null) return -1;
 * container.convertFor(this, point);
 * if (x &lt; 0 || y &lt; 0 || x &gt; size.width || y &gt; size.height) return -1;
 * int w = size.width - 4 - 4;
 * double t = length * zoom;
 * double rel = t * (x - 4) / w;
 * return max(0, min(length, round(offset + rel)));
 * </pre>
 *
 * <p>If {@code offset} sits near {@code length} (a scrolled viewport that was never reset) then a click
 * near the left edge rounds to {@code offset} instead of an early time - which matches "only forward
 * works". If {@code size} is stale or tiny, the division explodes - which matches "jumps straight to
 * the end". Printing all of them settles it in one round instead of guessing.
 */
@Mixin(targets = "com.replaymod.lib.de.johni0702.minecraft.gui.element.advanced.AbstractGuiTimeline")
public abstract class MixinGuiTimelineClickProbe {

    private static final String TAG = "[replaycompat] ";
    private static final int LOG_LIMIT = 20;

    private static Field fieldSize;
    private static Field fieldOffset;
    private static Field fieldZoom;
    private static Field fieldLength;
    private static Method methodGetWidth;
    private static Method methodGetHeight;
    private static boolean resolved;
    private static boolean broken;
    private static int logged;

    /**
     * Hooks the <b>real click</b> ({@code onClick(int)}) rather than {@code getTimeAt}: measured in 12.58,
     * {@code getTimeAt} is also called while merely moving the mouse (hover/tooltip), so the first version
     * of this probe only ever captured mouse movement and never a click. {@code onClick} is what actually
     * asks the replay to jump, and it is only reached from {@code mouseClick}.
     *
     * <p>Also prints the concrete timeline class: {@code AbstractGuiTimeline} is the parent of both the
     * replay overlay's {@code GuiMarkerTimeline} and simplepathing's {@code GuiKeyframeTimeline}, and the
     * v78 samples mixed both (zoom=0.1/length=30min vs zoom=1.0/length=55s) - which made them look like a
     * bug. Naming the class removes that ambiguity for good.
     */
    @Inject(method = "onClick(I)V", at = @At("HEAD"), remap = false, require = 0)
    private void replaycompat$probeTimelineClick(int time, CallbackInfo ci) {
        try {
            if (broken || logged >= LOG_LIMIT) {
                return;
            }
            replaycompat$resolve();
            if (broken) {
                return;
            }
            Object size = fieldSize.get(this);
            int width = size == null ? -1 : ((Integer) methodGetWidth.invoke(size)).intValue();
            int height = size == null ? -1 : ((Integer) methodGetHeight.invoke(size)).intValue();

            logged++;
            System.out.println(TAG + "timeline click: " + this.getClass().getSimpleName()
                    + " -> jump to " + time + " ms"
                    + " size=(" + width + "x" + height + ")"
                    + " offset=" + fieldOffset.get(this)
                    + " zoom=" + fieldZoom.get(this)
                    + " length=" + fieldLength.get(this));
        } catch (Throwable ignored) {
            // never disturb the timeline
        }
    }

    private static void replaycompat$resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            Class<?> timeline = Class.forName(
                    "com.replaymod.lib.de.johni0702.minecraft.gui.element.advanced.AbstractGuiTimeline");
            fieldSize = field(timeline, "size");
            fieldOffset = field(timeline, "offset");
            fieldZoom = field(timeline, "zoom");
            fieldLength = field(timeline, "length");

            Class<?> dimension = Class.forName(
                    "com.replaymod.lib.de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension");
            methodGetWidth = dimension.getMethod("getWidth");
            methodGetHeight = dimension.getMethod("getHeight");
        } catch (Throwable t) {
            broken = true;
            System.out.println(TAG + "timeline probe unavailable: " + t);
        }
    }

    private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
        for (Class<?> c = owner; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
                // keep walking up
            }
        }
        throw new NoSuchFieldException(name + " on " + owner.getName());
    }
}
