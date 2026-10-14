package com.gtnh.replaycompat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Diagnostic only since v85 (HANDOVER 12.61): reports whether the replay input screen's own mouse
 * handler is reached at all.
 *
 * <h3>Why the camera-speed work moved out of here</h3>
 *
 * <p>v70-v84 all hung the wheel to camera-speed conversion on this method
 * ({@code AbstractGuiOverlay$UserInputGuiScreen#func_146274_d} = {@code GuiScreen#handleMouseInput}).
 * The tester logs then showed the expected line never appearing, without telling us <em>which</em> of
 * two very different reasons applied: the handler was never called, or it was called and the wheel read
 * as 0. Both look identical in a log.
 *
 * <p>{@code Minecraft.runTick} does call {@code GuiScreen#handleMouseInput} directly, but during a
 * replay the input pump is {@code InputReplayTimer#updateTimer}, whose input-screen branch calls
 * {@code GuiScreen#handleInput} instead - and that one only forwards to {@code handleMouseInput} while
 * {@code Mouse#next()} still yields events, so any earlier consumer of the mouse queue silently removes
 * this hook. The wheel handling now lives in {@code ReplayMouseState#wheelTick}, which is driven from
 * {@code Minecraft.runTick} every frame (pit 59: never park a fallback on a method that a fault can stop
 * calling).
 *
 * <p>This hook stays because it is the cheapest possible answer to "was this method reached?": it prints
 * exactly once per session, and its absence from a log is itself the answer.
 */
@Mixin(targets = "com.replaymod.lib.de.johni0702.minecraft.gui.container.AbstractGuiOverlay$UserInputGuiScreen")
public abstract class MixinUserInputGuiScreenWheelCameraSpeed {

    private static final String TAG = "[replaycompat] ";
    private static boolean reported;

    @Inject(method = "func_146274_d", at = @At("HEAD"), remap = false, require = 0)
    private void replaycompat$wheelProbe(CallbackInfo ci) {
        if (reported) {
            return;
        }
        reported = true;
        try {
            System.out.println(TAG + "wheel probe: UserInputGuiScreen.func_146274_d was reached"
                    + " (the per-frame hook in ReplayMouseState is the one that applies the wheel)");
        } catch (Throwable ignored) {
            // diagnostics only
        }
    }
}
