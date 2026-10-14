package com.gtnh.replaycompat.mixin;

import com.replaymod.lib.de.johni0702.minecraft.gui.container.VanillaGuiScreen;

import net.minecraftforge.client.event.GuiScreenEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Diagnostic probe on jGui's {@code VanillaGuiScreen.EventHandler}.
 *
 * <p>It logs three different callbacks so the result is self-checking:
 *
 * <ul>
 *   <li>{@code onGuiRender} - <b>control</b>. The saving panel is visibly rendered on the GTNH main
 *       menu, and that rendering goes through this handler, so this line <em>must</em> appear. If it
 *       does not, the probe itself failed to apply and no conclusion can be drawn from the others.</li>
 *   <li>{@code onMouseInput} - the actual question. These events are posted only by ReplayMod's own
 *       1.7.10 {@code MixinGuiScreen}, which hijacks {@code GuiScreen.handleInput}
 *       ({@code func_146269_k}) and re-walks the LWJGL mouse queue.</li>
 *   <li>{@code onKeyboardInput} - same mechanism for keys (typing into the rename field).</li>
 * </ul>
 *
 * <p>Observation so far: the panel appears but does not react to clicks or typing, and no
 * {@code onMouseInput} line was produced. Comparing the control line against it tells us whether the
 * break is upstream (events never posted) or downstream (posted but not delivered here).
 *
 * <p>This class only observes - nothing is cancelled or modified.
 */
@Mixin(targets = "com.replaymod.lib.de.johni0702.minecraft.gui.container.VanillaGuiScreen$EventHandler")
public abstract class MixinVanillaGuiScreenEventHandler {

    private static int replaycompat$renderLog = 0;
    private static int replaycompat$mouseLog = 0;
    private static int replaycompat$keyLog = 0;

    /** Control probe: proves the mixin applied, because the panel does render. */
    @Inject(method = "onGuiRender", at = @At("HEAD"), remap = false, require = 0)
    private void replaycompat$traceGuiRender(GuiScreenEvent.DrawScreenEvent.Post event, CallbackInfo ci) {
        try {
            if (replaycompat$renderLog < 3) {
                replaycompat$renderLog++;
                System.out.println("[replaycompat] probe: EventHandler.onGuiRender reached #"
                        + replaycompat$renderLog);
            }
        } catch (Throwable ignored) {
            // logging must never break rendering
        }
    }

    @Inject(method = "onMouseInput", at = @At("HEAD"), remap = false, require = 0)
    private void replaycompat$traceMouseInput(VanillaGuiScreen.MouseInputEvent event, CallbackInfo ci) {
        try {
            if (replaycompat$mouseLog < 15) {
                replaycompat$mouseLog++;
                System.out.println("[replaycompat] probe: EventHandler.onMouseInput reached #"
                        + replaycompat$mouseLog);
            }
        } catch (Throwable ignored) {
            // logging must never break input handling
        }
    }

    @Inject(method = "onKeyboardInput", at = @At("HEAD"), remap = false, require = 0)
    private void replaycompat$traceKeyboardInput(VanillaGuiScreen.KeyboardInputEvent event, CallbackInfo ci) {
        try {
            if (replaycompat$keyLog < 15) {
                replaycompat$keyLog++;
                System.out.println("[replaycompat] probe: EventHandler.onKeyboardInput reached #"
                        + replaycompat$keyLog);
            }
        } catch (Throwable ignored) {
            // logging must never break input handling
        }
    }
}
