package com.gtnh.replaycompat.mixin;

import com.gtnh.replaycompat.util.ReplayMouseState;
import com.replaymod.lib.de.johni0702.minecraft.gui.container.AbstractGuiOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Method;

/**
 * Stops the replay input screen from clearing the mouse lock every time it closes (HANDOVER 12.38).
 *
 * <h3>Why 12.36's popup fix was not enough</h3>
 *
 * The 338 environment's v68 log still showed {@code RELEASED} 136x against {@code GRABBED} 0x: the
 * overlay's {@code mouseVisible} flag keeps being reset, so the Ctrl lock has to be re-applied again
 * and again (and every re-application runs {@code Minecraft.displayGuiScreen} →
 * {@code unPressAllKeys()}, which is what wipes WASD).
 *
 * A full scan of the writers of that field leaves exactly two: {@code setMouseVisible} itself and the
 * synthetic accessor {@code AbstractGuiOverlay.access$202} - and {@code access$202} has exactly one
 * caller, {@code AbstractGuiOverlay$UserInputGuiScreen.func_146281_b} (= {@code GuiScreen.onGuiClosed}).
 * So: <b>closing the input screen clears the flag</b>, and since the flag is what keeps the input
 * screen mounted, the lock can never survive a single timeline click or popup close.
 *
 * <h3>Fix</h3>
 *
 * Redirect that one write: when it would store {@code false} <em>and</em> the player still wants the
 * mouse visible ({@link ReplayMouseState#lockedVisible}), skip the store. The flag then stays true,
 * so upstream's own {@code updateUserInputGui()} re-mounts the input screen on the next pass
 * ({@code if (mouseVisible && mc.currentScreen == null) displayGuiScreen(userInputGuiScreen)}).
 *
 * <p>When the player presses Ctrl again (intent false) the store happens normally, so the mouse is
 * grabbed back exactly as before. With no lock at all this redirect is a pass-through and upstream
 * behaviour is bit-for-bit unchanged.
 *
 * <p>{@code access$202} is a private synthetic accessor, so the pass-through path reaches the original
 * reflectively (this compat project has no reobf and cannot call it directly). jGui is relocated
 * inside the ReplayMod jar and never remapped, hence {@code remap = false}; {@code require = 0} keeps
 * this a no-op if the code shape changes.
 */
@Mixin(targets = "com.replaymod.lib.de.johni0702.minecraft.gui.container.AbstractGuiOverlay$UserInputGuiScreen")
public abstract class MixinUserInputGuiScreenKeepMouseLock {

    private static final String ACCESSOR =
            "com.replaymod.lib.de.johni0702.minecraft.gui.container.AbstractGuiOverlay";
    private static final String ACCESSOR_METHOD = "access$202";

    private static Method accessor;
    private static boolean accessorResolved;

    @Redirect(method = "func_146281_b",
            at = @At(value = "INVOKE",
                    target = "Lcom/replaymod/lib/de/johni0702/minecraft/gui/container/AbstractGuiOverlay;access$202(Lcom/replaymod/lib/de/johni0702/minecraft/gui/container/AbstractGuiOverlay;Z)Z"),
            remap = false, require = 0)
    private static boolean replaycompat$keepMouseLockAcrossClose(AbstractGuiOverlay overlay, boolean value) {
        if (!value && ReplayMouseState.lockedVisible) {
            // Do NOT store false: the player asked for a visible mouse, and the flag is what keeps the
            // input screen mounted. Upstream re-mounts it by itself on the next updateUserInputGui().
            ReplayMouseState.noteKeptLockOnce();
            return true;
        }
        try {
            Method m = replaycompat$accessor();
            if (m == null) {
                return value;
            }
            Object result = m.invoke(null, overlay, Boolean.valueOf(value));
            return (result instanceof Boolean) ? ((Boolean) result).booleanValue() : value;
        } catch (Throwable ignored) {
            // Never let the guard break GUI closing.
            return value;
        }
    }

    private static Method replaycompat$accessor() {
        if (accessorResolved) {
            return accessor;
        }
        accessorResolved = true;
        try {
            Class<?> owner = Class.forName(ACCESSOR);
            Method m = owner.getDeclaredMethod(ACCESSOR_METHOD, owner, boolean.class);
            m.setAccessible(true);
            accessor = m;
        } catch (Throwable ignored) {
            accessor = null;
        }
        return accessor;
    }
}
