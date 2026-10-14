package com.gtnh.replaycompat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Method;

/**
 * Reflection bridge for the one GL call that Angelica's GLSMRedirector rewrites to a method that
 * does not exist: glReadPixels(..., long).
 *
 * Angelica 2.2.10's GLSMRedirector turns every org.lwjgl.opengl.GL11 call site into the matching
 * com.gtnewhorizons.angelica.glsm.GLStateManager call. For glReadPixels it only implements the three
 * java.nio.Buffer overloads - there is no (IIIIIIJ)V variant - so a call site using the LWJGL3
 * "offset into the bound pixel pack buffer" form dies with NoSuchMethodError (measured in the
 * 338-mod environment's log, crash "Rendering video").
 *
 * MixinPipelinesNoPbo removes that call site for ordinary exports by taking upstream's non-PBO
 * branch. This mixin is the belt-and-braces for cases that still force PBO (depth-map exports): it
 * replaces the call with a reflective call to the real LWJGL3
 * GL11.glReadPixels(int,int,int,int,int,int,long).
 *
 * Reflection is the point: a normal (bytecode) call would be rewritten by the very same
 * GLSMRedirector that caused the problem, whereas a reflective lookup happens at runtime and reaches
 * the genuine LWJGL binding. glReadPixels is a pure read and changes no GL state, so bypassing the
 * state manager is safe.
 *
 * Both plausible owners of the call site are targeted, because the order in which this mixin and
 * Angelica's redirector see the class is not guaranteed: org/lwjgl/opengl/GL11 (mixin first) and
 * com/gtnewhorizons/angelica/glsm/GLStateManager (Angelica first). Both use require = 0, so whichever
 * one does not match is silently skipped.
 */
@Mixin(targets = "com.replaymod.render.capturer.PboOpenGlFrameCapturer")
public abstract class MixinPboFrameCapturerGlReadPixels {

    /** Only the real implementation exists (the other captureFrame is a bridge method). */
    private static final String CAPTURE_FRAME =
            "captureFrame(ILjava/lang/Enum;)Lcom/replaymod/render/frame/OpenGlFrame;";

    private static final String TAG = "[replaycompat] ";

    private static Method readPixels;
    private static boolean resolved;
    private static int logCount;

    @Redirect(method = CAPTURE_FRAME,
            at = @At(value = "INVOKE",
                    target = "Lorg/lwjgl/opengl/GL11;glReadPixels(IIIIIIJ)V"),
            remap = false, require = 0)
    private static void replaycompat$readPixelsViaGl11(int x, int y, int width, int height,
                                                       int format, int type, long offset) {
        replaycompat$readPixels(x, y, width, height, format, type, offset);
    }

    @Redirect(method = CAPTURE_FRAME,
            at = @At(value = "INVOKE",
                    target = "Lcom/gtnewhorizons/angelica/glsm/GLStateManager;glReadPixels(IIIIIIJ)V"),
            remap = false, require = 0)
    private static void replaycompat$readPixelsViaGlStateManager(int x, int y, int width, int height,
                                                                 int format, int type, long offset) {
        replaycompat$readPixels(x, y, width, height, format, type, offset);
    }

    private static void replaycompat$readPixels(int x, int y, int width, int height,
                                                int format, int type, long offset) {
        try {
            Method m = replaycompat$resolve();
            if (m == null) {
                replaycompat$log("video export: no usable glReadPixels(long) to reach the driver - "
                        + "the captured frames would be wrong");
                return;
            }
            m.invoke(null, x, y, width, height, format, type, offset);
        } catch (Throwable t) {
            replaycompat$log("video export: glReadPixels(long) failed: " + t);
        }
    }

    private static Method replaycompat$resolve() {
        if (resolved) {
            return readPixels;
        }
        resolved = true;
        // GL11 is the LWJGL3 core binding on this pack; GL11C is the raw entry point it delegates to
        // and is tried as a fallback in case a shim ever replaces GL11 itself.
        String[] owners = { "org.lwjgl.opengl.GL11", "org.lwjgl.opengl.GL11C" };
        for (String owner : owners) {
            try {
                Class<?> type = Class.forName(owner);
                readPixels = type.getMethod("glReadPixels",
                        int.class, int.class, int.class, int.class, int.class, int.class, long.class);
                replaycompat$log("video export: glReadPixels(long) resolved on " + owner);
                return readPixels;
            } catch (Throwable ignored) {
                // try the next owner
            }
        }
        readPixels = null;
        return null;
    }

    private static void replaycompat$log(String message) {
        try {
            if (logCount >= 5) {
                return;
            }
            logCount++;
            System.out.println(TAG + message);
        } catch (Throwable ignored) {
            // logging must never escape
        }
    }
}