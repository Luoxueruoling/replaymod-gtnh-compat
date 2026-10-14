package com.gtnh.replaycompat.mixin;

import com.gtnh.replaycompat.util.ReplayMouseState;
import com.replaymod.lib.de.johni0702.minecraft.gui.container.AbstractGuiOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Keeps the player's Ctrl mouse lock alive across popups (HANDOVER 12.36).
 *
 * <h3>The loop this breaks</h3>
 *
 * Upstream {@code AbstractGuiPopup} does:
 *
 * <pre>
 *   open()  : wasMouseVisible = overlay.isMouseVisible();  overlay.setMouseVisible(true);
 *   close() : overlay.setMouseVisible(wasMouseVisible);
 * </pre>
 *
 * i.e. a popup makes the mouse visible while it is open and restores the previous state on close.
 * On GTNH the overlay's {@code mouseVisible} flag never stays true on its own -- the input screen's
 * {@code onGuiClosed} clears it (verified: {@code AbstractGuiOverlay$UserInputGuiScreen.func_146281_b}
 * contains the only other write to that field). So the snapshot is almost always {@code false}, and
 * every popup close <b>grabs the mouse back</b> even though the player had locked it visible with
 * Ctrl. The mouse mixin then re-applies the lock, another popup closes, and so on -- the strobing
 * seen in the 338 environment's log ({@code RELEASED} 112x, {@code GRABBED} 0x, strobing guard
 * tripped). Each of those reapplications runs {@code Minecraft.displayGuiScreen}, whose
 * {@code unPressAllKeys()} wipes WASD, which is why movement stopped working and the timeline
 * jumped to the end (a click was being reinterpreted while the input screen kept being rebuilt).
 *
 * <h3>Fix</h3>
 *
 * Take the snapshot from what the player asked for: return
 * {@code overlay.isMouseVisible() || ReplayMouseState.lockedVisible}. With no Ctrl lock the value is
 * unchanged, so upstream behaviour is preserved exactly; with a lock the popup restores
 * "visible" instead of overriding the player.
 *
 * <p>jGui is relocated inside the ReplayMod jar and never remapped by Forge, so both the method name
 * and the call target keep their real names and the injection uses {@code remap = false}.
 * {@code require = 0} degrades to upstream behaviour if the code shape ever changes.
 */
@Mixin(targets = "com.replaymod.lib.de.johni0702.minecraft.gui.popup.AbstractGuiPopup")
public abstract class MixinGuiPopupPreserveMouseLock {

    @Redirect(method = "open",
            at = @At(value = "INVOKE",
                    target = "Lcom/replaymod/lib/de/johni0702/minecraft/gui/container/AbstractGuiOverlay;isMouseVisible()Z"),
            remap = false, require = 0)
    private boolean replaycompat$snapshotPlayerIntent(AbstractGuiOverlay overlay) {
        try {
            return overlay.isMouseVisible() || ReplayMouseState.lockedVisible;
        } catch (Throwable ignored) {
            return true;
        }
    }
}
