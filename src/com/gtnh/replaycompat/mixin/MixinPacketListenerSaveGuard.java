package com.gtnh.replaycompat.mixin;

import com.gtnh.replaycompat.util.ReplayMouseState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops a finished recorder from trying to save more packets (HANDOVER 12.65, v88).
 *
 * <h3>The failure</h3>
 *
 * <p>Connecting to a server <b>while a recording is still running</b> leaves the old
 * {@code PacketListener} alive: its save thread pool has already been shut down (the connection closed,
 * so {@code channelInactive} saved the file), but something keeps feeding it packets. Every one of them
 * produces a full stack trace in the log - hundreds of them, several per tick:
 *
 * <pre>
 * [com.replaymod.recording.packet.PacketListener/]: Writing packet:
 * java.util.concurrent.RejectedExecutionException: Task ... rejected from
 *     java.util.concurrent.ThreadPoolExecutor@...[Terminated, pool size = 0, active threads = 0, ...]
 *     at PacketListener.save(PacketListener.java:202)
 *     at PacketListener.lambda$save$1(PacketListener.java:167)
 *     at Minecraft.func_71407_l(Minecraft.java:1576)
 * </pre>
 *
 * <p>{@code save} is only a submission into {@code saveService}; once that executor is shut down the
 * work can never run, so dropping the call loses nothing - and it keeps a stale recorder from mutating
 * replay state (and the log) long after its session ended.
 *
 * <h3>Layout</h3>
 *
 * <p>No target method parameters are taken (only {@code CallbackInfo}), so this class references no
 * Minecraft type at all (pit 29). The executor lookup lives in
 * {@link ReplayMouseState#packetSaverIsShutDown(Object)} and is fully reflective.
 */
@Mixin(targets = "com.replaymod.recording.packet.PacketListener")
public abstract class MixinPacketListenerSaveGuard {

    private static boolean replaycompat$reported;
    private static int replaycompat$dropped;

    @Inject(method = "save", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void replaycompat$dropSaveAfterShutdown(CallbackInfo ci) {
        try {
            if (ReplayMouseState.packetSaverIsShutDown(this)) {
                replaycompat$dropped++;
                if (!replaycompat$reported) {
                    replaycompat$reported = true;
                    System.out.println("[replaycompat] recording: this recorder's save thread pool was"
                            + " already shut down (the connection closed) - further packets are dropped"
                            + " instead of flooding the log with RejectedExecutionException");
                }
                ci.cancel();
            }
        } catch (Throwable ignored) {
            // Never interfere with a healthy save path.
        }
    }
}
