package com.gtnh.replaycompat.mixin;

import com.gtnh.replaycompat.util.NullWorldGuard;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Re-queues packets that {@link MixinNetHandlerPlayClientNullWorldGuard} had to hold back because
 * {@code Minecraft.theWorld} did not exist yet.
 *
 * <p>Hooked onto {@code NetworkManager.func_74428_b} (the receive loop) rather than onto a packet
 * handler, because the whole point is to act on the first frame after a world appears - which may well
 * be a frame in which none of the guarded packet types arrive.
 *
 * <p>Re-queueing (instead of invoking the handler reflectively) keeps this patch free of any reflective
 * dispatch: the packets go back onto the very queue the vanilla loop is about to drain, so vanilla
 * dispatches them exactly as it always would, and nothing here can alter exception behaviour.
 *
 * <p>On a dedicated server the packet listener has no {@code gameController} field, so the world lookup
 * throws and the hook quietly does nothing.
 */
@Mixin(targets = "net.minecraft.network.NetworkManager")
public abstract class MixinNetworkManagerDeferredFlush {

    @Inject(method = "func_74428_b", at = @At("HEAD"), remap = false, require = 0)
    private void guard$requeueDeferredPackets(CallbackInfo ci) {
        try {
            if (!NullWorldGuard.hasDeferred()) {
                return;
            }
            if (NullWorldGuard.worldOfNetworkManager(this) == null) {
                return; // still loading - keep holding them
            }
            int n = NullWorldGuard.requeueDeferred(this);
            if (n > 0) {
                NullWorldGuard.logOnce("re-queued " + n + " deferred packet(s) now that the world exists");
            }
        } catch (Throwable ignored) {
            // Never interfere with the real packet loop.
        }
    }
}
