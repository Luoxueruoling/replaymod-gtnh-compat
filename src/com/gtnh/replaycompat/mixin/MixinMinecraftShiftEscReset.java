package com.gtnh.replaycompat.mixin;

import com.gtnh.replaycompat.util.ReplayMouseState;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shift+ESC = "force reset" inside a replay: put the session back into the state it has right after a
 * recording is loaded (HANDOVER 12.44, suggested by the tester after a player-list spectator switch
 * left the mouse unusable).
 *
 * <h3>Why this is needed</h3>
 *
 * Switching to a player's view through the player list makes the overlay close its input screen. The
 * 12.38 lock-preserving patch then keeps {@code mouseVisible} true (so the player's Ctrl lock is not
 * stolen), but the input screen is only re-mounted while {@code mc.currentScreen == null}. If it does
 * not come back, the flag says "mouse visible" while the mouse is actually grabbed - it feels stuck.
 * Shift+ESC is the emergency exit: drop the lock, clear spectatorship, close any open screen.
 *
 * <h3>Why hook {@code displayInGameMenu}</h3>
 *
 * ESC has no {@code KeyBinding}, and {@code org.lwjgl.input.Keyboard.isKeyDown} is unreliable on this
 * pack (HANDOVER pit 49: it returns false while the key is held). {@code Minecraft#displayInGameMenu}
 * ({@code func_71385_j}) is where ESC lands, so the combination is detected reliably (by checking the
 * sneak binding inside {@link ReplayMouseState#performReset}) and the menu is cancelled while Shift is
 * held.
 *
 * <p><b>The injection body deliberately touches no Minecraft members.</b> This project has no refmap
 * and no reobf, so a compiled reference to e.g. {@code theWorld} would resolve at compile time and then
 * be missing at runtime (pit 29). All of that lives in the util class, reflectively.
 */
@Mixin(Minecraft.class)
public abstract class MixinMinecraftShiftEscReset {

    @Inject(method = "func_71385_j", at = @At("HEAD"), remap = false, require = 0, cancellable = true)
    private void replaycompat$shiftEscForceReset(CallbackInfo ci) {
        try {
            if (ReplayMouseState.performReset(this)) {
                ci.cancel(); // Shift is held: reset instead of opening the game menu
            }
        } catch (Throwable ignored) {
            // Never break ESC handling.
        }
    }

    /**
     * Keeps the mouse in the player's hands while their Ctrl lock is on (HANDOVER 12.50).
     *
     * <p>{@code Minecraft.runTick} calls {@code setIngameFocus} ({@code func_71381_h}) from its mouse
     * loop on <b>every left click</b> while {@code currentScreen == null &amp;&amp; !inGameHasFocus} - and
     * that method grabs the mouse and runs {@code displayGuiScreen(null)}, which tears the overlay's
     * input screen down again. Measured symptom: "after clicking the timeline a few times I cannot move
     * any more". With the lock off the suppression never triggers, so normal focusing is untouched.
     */
    @Inject(method = "func_71381_h", at = @At("HEAD"), remap = false, require = 0, cancellable = true)
    private void replaycompat$keepReplayMouseVisible(CallbackInfo ci) {
        try {
            if (ReplayMouseState.shouldSuppressIngameFocus(this)) {
                ci.cancel();
            }
        } catch (Throwable ignored) {
            // Never break focusing.
        }
    }

    /**
     * Per-frame replay-mouse upkeep (HANDOVER 12.52, pit 59). It lives here on purpose: the overlay's
     * own {@code updateUserInputGui} is only reached while its input screen is up, so a guard placed
     * there can never recover from that screen being torn down. {@code runTick} always runs.
     */
    @Inject(method = "func_71407_l", at = @At("HEAD"), remap = false, require = 0)
    private void replaycompat$replayMouseTick(CallbackInfo ci) {
        try {
            ReplayMouseState.tick(this);
            ReplayMouseState.census(this);
        } catch (Throwable ignored) {
            // never disturb the game loop
        }
    }
}
