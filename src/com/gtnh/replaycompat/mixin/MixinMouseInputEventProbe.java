package com.gtnh.replaycompat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Diagnostic probe that counts how often jGui's {@code MouseInputEvent} is actually constructed.
 *
 * <p>This is the cleanest possible question to ask about the broken input path: that event class is
 * instantiated in exactly one place in the whole mod - ReplayMod's 1.7.10
 * {@code recording/mixin/MixinGuiScreen}, inside its {@code while (Mouse.next())} replay loop:
 *
 * <pre>
 * while (Mouse.next()) {
 *     if (MinecraftForge.EVENT_BUS.post(new VanillaGuiScreen.MouseInputEvent())) continue;
 *     handleMouseInput();
 * }
 * </pre>
 *
 * <p>So a line from here means "the replay loop ran at least once and posted an event"; silence
 * means either that mixin did not apply, or {@code Mouse.next()} returned false there (the queue had
 * already been drained by {@code Minecraft.runTick}'s own mouse handling, which is a second,
 * independent consumer in 1.7.10).
 *
 * <p>Observes only.
 */
@Mixin(targets = "com.replaymod.lib.de.johni0702.minecraft.gui.container.VanillaGuiScreen$MouseInputEvent")
public abstract class MixinMouseInputEventProbe {

    private static int replaycompat$posted = 0;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false, require = 0)
    private void replaycompat$traceConstruction(CallbackInfo ci) {
        try {
            if (replaycompat$posted < 15) {
                replaycompat$posted++;
                System.out.println("[replaycompat] probe: MouseInputEvent constructed/posted #"
                        + replaycompat$posted);
            }
        } catch (Throwable ignored) {
            // logging must never break event dispatch
        }
    }
}
