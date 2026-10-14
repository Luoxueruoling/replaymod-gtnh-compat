package com.gtnh.replaycompat.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes ReplayMod's video export work on GTNH by keeping it off the PBO ("pixel buffer object")
 * frame-capture path.
 *
 * <h3>Symptom 1 (338-mod environment, log J7AQ5aF) -- fixed by the reflective bridge</h3>
 *
 * <pre>
 * [com.replaymod.render.ReplayModRender]: Starting ...\.minecraft\ffmpeg\bin\ffmpeg.exe with args: ...
 * [com.replaymod.render.ReplayModRender]: ---- Minecraft Crash Report ----
 * Description: Rendering video
 * java.lang.NoSuchMethodError: 'void com.gtnewhorizons.angelica.glsm.GLStateManager.glReadPixels(int, int, int, int, int, int, long)'
 *     at com.replaymod.render.capturer.PboOpenGlFrameCapturer.captureFrame(PboOpenGlFrameCapturer.java:97)
 * </pre>
 *
 * Angelica's {@code GLSMRedirector} rewrites every {@code org.lwjgl.opengl.*} call site to the
 * matching {@code com.gtnewhorizons.angelica.glsm.GLStateManager} method, and Angelica 2.2.10 does
 * not implement the {@code (IIIIIIJ)V} (long = offset into the bound pack buffer) overload:
 *
 * <pre>
 *   wanted : GL11.glReadPixels(IIIIIIJ)V
 *   present: GLStateManager.glReadPixels(IIIIIILjava/nio/ByteBuffer;)V
 *            GLStateManager.glReadPixels(IIIIIILjava/nio/FloatBuffer;)V
 *            GLStateManager.glReadPixels(IIIIIILjava/nio/IntBuffer;)V
 * </pre>
 *
 * {@code MixinPboFrameCapturerGlReadPixels} bridges that one call by reflection.
 *
 * <h3>Symptom 2 (338-mod environment, 2026-10-11 17:54) -- why this mixin now matters</h3>
 *
 * The reflective bridge removed the crash but the exported video came out <b>blank/white</b>.
 * That is exactly what bypassing the state manager buys you: the {@code long} form of
 * {@code glReadPixels} reads out of the <em>currently bound</em> pack buffer, so it depends on
 * Angelica's {@code glBindBuffer} bookkeeping having actually reached the driver at that moment.
 * A reflective call goes straight to LWJGL and sees the real GL state, which is not guaranteed to
 * agree with Angelica's shadow copy -- so the read can land in the wrong (unwritten) buffer and
 * every frame comes back blank. ReplayMod only ever took that path because
 * {@code PixelBufferObject.SUPPORTED} is true on this pack.
 *
 * <h3>Fix</h3>
 *
 * {@code Pipelines} decides between the PBO and the plain capturer with
 * {@code if (PixelBufferObject.SUPPORTED || renderSettings.isDepthMap())}, always as a static field
 * read. This redirect makes that read report {@code false}, so ordinary exports take upstream's own
 * non-PBO branch ({@code SimpleOpenGlFrameCapturer}, which uses the {@code ByteBuffer} overload that
 * Angelica <em>does</em> provide, and therefore stays entirely inside the state manager). That branch
 * is not a downgrade invented here: it is the path upstream itself uses whenever the driver lacks
 * PBO support. The only cost is that frame read-back is synchronous instead of overlapped, i.e.
 * slightly slower rendering - functionality is identical.
 *
 * <p>{@code depthMap} exports still ask for the PBO capturer on purpose (they need depth data that
 * the simple capturer cannot produce); those stay covered by the reflective bridge.
 *
 * <h3>Injection notes (hard-won)</h3>
 *
 * <ul>
 *   <li><b>{@code value} is the injection point type, not an opcode.</b> The first version of this
 *       mixin said {@code @At(value = "GETSTATIC", ...)}, which throws
 *       {@code InvalidInjectionException: GETSTATIC is not a valid injection point specifier} at
 *       apply time. Because the config is {@code required: false} that only leaves a WARN in the log
 *       and the mixin silently does nothing -- the export then kept running on the PBO path for
 *       months of testing (HANDOVER pit 51). The correct form is
 *       {@code @At(value = "FIELD", opcode = Opcodes.GETSTATIC, target = "...;SUPPORTED:Z")}.</li>
 *   <li><b>Exactly four methods read the field</b> (verified with {@code javap -c} on
 *       {@code 1.7.10-2.5.2}): {@code newDefaultPipeline} (offset 17),
 *       {@code newStereoscopicPipeline} (17), {@code newCubicPipeline} (17) and
 *       {@code newEquirectangularPipeline} (38). {@code newODSPipeline} does <em>not</em> read it and
 *       is deliberately not listed any more.</li>
 *   <li>{@code SUPPORTED} is computed in {@code PixelBufferObject.<clinit>} from
 *       {@code GLContext.getCapabilities().GL_ARB_pixel_buffer_object || OpenGL15}, so it is
 *       <em>not</em> a compile-time constant and the {@code getstatic} really is in the bytecode.</li>
 *   <li>ReplayMod is never remapped by Forge, so the field keeps its real name and the injection uses
 *       {@code remap = false}. {@code require = 0} keeps this a no-op if a future ReplayMod release
 *       changes the code shape.</li>
 * </ul>
 */
@Mixin(targets = "com.replaymod.render.rendering.Pipelines")
public abstract class MixinPipelinesNoPbo {

    private static final String TAG = "[replaycompat] ";
    private static boolean reported;

    @Redirect(method = {
            "newDefaultPipeline",
            "newStereoscopicPipeline",
            "newCubicPipeline",
            "newEquirectangularPipeline"
    }, at = @At(value = "FIELD", opcode = Opcodes.GETSTATIC,
            target = "Lcom/replaymod/render/utils/PixelBufferObject;SUPPORTED:Z"),
            remap = false, require = 0)
    private static boolean replaycompat$forceNonPboCapturer() {
        if (!reported) {
            reported = true;
            try {
                System.out.println(TAG + "video export: PBO frame capture disabled "
                        + "(Angelica has no glReadPixels(long); using the plain capturer)");
            } catch (Throwable ignored) {
                // logging must never escape
            }
        }
        return false;
    }
}
