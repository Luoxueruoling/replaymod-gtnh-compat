package com.gtnh.replaycompat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

/**
 * Self-heals the timeline's viewport offset, i.e. "the big timeline can only be moved forward /
 * jumping backwards flies to the end" (HANDOVER 12.53).
 *
 * <h3>Evidence</h3>
 *
 * <p>The tester's own workaround pinned it down: <b>"reloading the replay once puts it back"</b> -
 * only session state can behave like that (the coordinate conversion cannot change by reloading, and
 * 12.50 already verified it numerically). The two pieces of session state are {@code offset} and
 * {@code zoom}, and {@code getTimeAt} computes:
 *
 * <pre>
 * int w = size.width - 8;
 * double rel = (length * zoom) * (x - 4) / w;
 * return max(0, min(length, round(offset + rel)));
 * </pre>
 *
 * so a stale {@code offset} makes every click land late.
 *
 * <p>{@code AbstractGuiTimeline.checkOffset()} is the only thing that clamps it:
 *
 * <pre>
 * int visible = (int)(length * zoom);
 * if (visible + offset &gt; length) offset = length - visible;
 * </pre>
 *
 * and it is called from <b>setOffset / setZoom only</b> - nothing re-clamps after {@code length}
 * changes (replay switch, restart, re-encode).
 *
 * <h3>Why zeroing while {@code zoom >= 1} is safe</h3>
 *
 * <p>With {@code zoom == 1} the whole recording is visible, so the only legal offset is
 * {@code length - visible == 0}; {@code checkOffset()} itself would produce 0. Therefore
 * {@code offset != 0} with {@code zoom >= 1} is an inconsistent state by construction, and forcing it
 * back cannot change any legitimate behaviour. The guard touches nothing else - in particular it never
 * writes {@code size} or {@code zoom}, so a healthy timeline behaves bit-for-bit as before.
 */
@Mixin(targets = "com.replaymod.lib.de.johni0702.minecraft.gui.element.advanced.AbstractGuiTimeline")
public abstract class MixinGuiTimelineViewportGuard {

    private static final String TAG = "[replaycompat] ";
    private static final int LOG_LIMIT = 5;

    private static Field fieldOffset;
    private static Field fieldZoom;
    private static Field fieldLength;
    private static boolean resolved;
    private static boolean broken;
    private static int healed;

    @Inject(method = "getTimeAt", at = @At("HEAD"), remap = false, require = 0)
    private void replaycompat$healStaleOffset(int x, int y, CallbackInfoReturnable<Integer> cir) {
        try {
            if (broken) {
                return;
            }
            replaycompat$resolve();
            if (broken) {
                return;
            }
            double zoom = ((Double) fieldZoom.get(this)).doubleValue();
            int offset = ((Integer) fieldOffset.get(this)).intValue();
            if (zoom < 1.0D || offset == 0) {
                return;
            }
            fieldOffset.setInt(this, 0);
            if (healed++ < LOG_LIMIT) {
                System.out.println(TAG + "timeline: stale viewport offset " + offset
                        + " ms with zoom=" + zoom + " (length=" + fieldLength.get(this)
                        + ") - reset to 0, the whole recording is visible anyway");
            }
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
            fieldOffset = field(timeline, "offset");
            fieldZoom = field(timeline, "zoom");
            fieldLength = field(timeline, "length");
        } catch (Throwable t) {
            broken = true;
            System.out.println(TAG + "timeline viewport guard unavailable: " + t);
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
