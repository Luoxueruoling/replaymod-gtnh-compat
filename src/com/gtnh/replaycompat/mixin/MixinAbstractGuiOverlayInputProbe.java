package com.gtnh.replaycompat.mixin;

import com.gtnh.replaycompat.util.NullWorldGuard;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

/**
 * DIAGNOSTIC ONLY - not part of the shipped patch set.
 *
 * <p>Why: the replay timeline / overlay buttons are unresponsive on GTNH. The overlay's input screen
 * ({@code AbstractGuiOverlay$UserInputGuiScreen}) is fully self-contained - it pumps the vanilla mouse
 * event via {@code GuiScreen.func_146274_d} and dispatches straight into the overlay's jGui tree - so
 * it does not depend on Forge's event bus at all. The only way it can fail to receive input is
 * {@code updateUserInputGui()} declining to install it, which happens exactly when
 * {@code mc.currentScreen != null}:
 *
 * <pre>
 * if (visible) {
 *     if (mouseVisible) { if (mc.currentScreen == null) mc.displayGuiScreen(userInputGuiScreen); }
 *     else              { if (mc.currentScreen == userInputGuiScreen) mc.displayGuiScreen(null); }
 * }
 * </pre>
 *
 * <p>And because that method only runs from the {@code setVisible}/{@code setMouseVisible} setters, one
 * unlucky ordering is permanent. This probe records the state at every such call so the actual culprit
 * screen can be identified instead of guessed.
 */
@Mixin(targets = "com.replaymod.lib.de.johni0702.minecraft.gui.container.AbstractGuiOverlay")
public abstract class MixinAbstractGuiOverlayInputProbe {

    private static int replaycompat$probeCount = 0;

    @Inject(method = "updateUserInputGui", at = @At("HEAD"), remap = false, require = 0)
    private void probe$updateUserInputGui(CallbackInfo ci) {
        replaycompat$probe("updateUserInputGui");
    }

    @Inject(method = "setVisible", at = @At("HEAD"), remap = false, require = 0)
    private void probe$setVisible(boolean value, CallbackInfo ci) {
        replaycompat$probe("setVisible(" + value + ")");
    }

    @Inject(method = "setMouseVisible", at = @At("HEAD"), remap = false, require = 0)
    private void probe$setMouseVisible(boolean value, CallbackInfo ci) {
        replaycompat$probe("setMouseVisible(" + value + ")");
    }

    @Inject(method = "setAllowUserInput", at = @At("HEAD"), remap = false, require = 0)
    private void probe$setAllowUserInput(boolean value, CallbackInfo ci) {
        replaycompat$probe("setAllowUserInput(" + value + ")");
    }

    private void replaycompat$probe(String where) {
        try {
            if (replaycompat$probeCount++ > 80) {
                return;
            }
            Object visible = NullWorldGuard.readField(this, "visible");
            Object mouseVisible = NullWorldGuard.readField(this, "mouseVisible");
            Object userInput = NullWorldGuard.readField(this, "userInputGuiScreen");
            String current = replaycompat$currentScreenName();
            String self = this.getClass().getName();
            System.out.println("[replaycompat-probe] " + self + " " + where
                    + " | visible=" + visible + " mouseVisible=" + mouseVisible
                    + " userInputScreen=" + (userInput == null ? "?" : "set")
                    + " | mc.currentScreen=" + current);
        } catch (Throwable t) {
            try {
                System.out.println("[replaycompat-probe] probe failed: " + t);
            } catch (Throwable ignored) {
                // ignore
            }
        }
    }

    private static String replaycompat$currentScreenName() {
        try {
            Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
            Object mc = null;
            for (String n : new String[] { "getMinecraft", "func_71410_x" }) {
                try {
                    Method m = mcClass.getMethod(n);
                    m.setAccessible(true);
                    mc = m.invoke(null);
                    break;
                } catch (NoSuchMethodException ignored) {
                    // try the next spelling
                }
            }
            if (mc == null) {
                return "<no Minecraft instance>";
            }
            Object current = NullWorldGuard.readField(mc, "currentScreen", "field_71462_r");
            return current == null ? "null" : current.getClass().getName();
        } catch (Throwable t) {
            return "<error: " + t + ">";
        }
    }
}
