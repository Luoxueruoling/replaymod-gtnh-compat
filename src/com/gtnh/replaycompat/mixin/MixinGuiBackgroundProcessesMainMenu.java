package com.gtnh.replaycompat.mixin;

import com.replaymod.core.versions.MCVer;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Two related fixes for ReplayMod's background-process panel (the "saving replay" panel) on GTNH:
 *
 * <ol>
 *   <li><b>Host the panel on the screen the client actually shows.</b>
 *       {@code onGuiInit} bails out unless {@code MCVer.getGui(event) == mc.currentScreen}, but on
 *       GTNH the main menu is Custom Main Menu's {@code GuiCustom} while CMM passes a throwaway
 *       {@code GuiFakeMain} shell through {@code InitGuiEvent.gui} (HANDOVER 2.5 / pit 17). The
 *       {@code @Redirect} below returns {@code mc.currentScreen} instead, so the guard passes,
 *       {@code VanillaGuiScreen.wrap} targets {@code GuiCustom} and the panel gets attached.</li>
 *   <li><b>Do not attach the same panel twice.</b> {@code onGuiInit} fires once per
 *       {@code InitGuiEvent.Post}, and GTNH/CMM emits two of them per main-menu initialisation
 *       (one carrying {@code GuiFakeMain}, one carrying jGui's own
 *       {@code AbstractGuiScreen$MinecraftGuiScreen}). Each call ends in
 *       {@code addElements(null, panel)} against the <em>same</em> cached
 *       {@code VanillaGuiScreen} wrapper, so the panel accumulates duplicate children. The
 *       {@code @Inject} at HEAD detects an already-attached panel and cancels the duplicate.</li>
 * </ol>
 *
 * <p><b>Why no {@code @Shadow}:</b> the target, {@code onGuiInit} and {@code MCVer} all belong to
 * ReplayMod, which Forge never remaps, so their names are the real runtime names (pit 29 only bites
 * Minecraft members). Everything else ({@code Minecraft.getMinecraft/currentScreen}, the private
 * {@code panel} field, {@code VanillaGuiScreen.wrap}, {@code getChildren}) is reached reflectively,
 * trying both MCP and SRG spellings where relevant. Every reflective step is wrapped so a failure
 * degrades to upstream behaviour rather than crashing.
 */
@Mixin(targets = "com.replaymod.core.gui.GuiBackgroundProcesses")
public abstract class MixinGuiBackgroundProcessesMainMenu {

    private static final String TAG = "[replaycompat] ";

    /** Diagnostics: log only the first few occurrences so we do not spam the log every GUI open. */
    private static int replaycompat$hostLog = 0;
    private static int replaycompat$dupLog = 0;

    // ------------------------------------------------------------------ 1. host on the real screen

    @Redirect(method = "onGuiInit", at = @At(value = "INVOKE",
            target = "Lcom/replaymod/core/versions/MCVer;getGui(Lnet/minecraftforge/client/event/GuiScreenEvent;)Lnet/minecraft/client/gui/GuiScreen;"),
            remap = false, require = 0)
    private GuiScreen replaycompat$hostPanelOnTheScreenWeActuallyShow(GuiScreenEvent event) {
        GuiScreen real = null;
        try {
            real = readCurrentScreen();
        } catch (Throwable t) {
            try {
                System.out.println(TAG + "background panel: could not read mc.currentScreen: " + t);
            } catch (Throwable ignored) {
                // logging must never break the injection
            }
        }

        if (real != null) {
            return real;
        }

        // Fall back to upstream behaviour so a failure here degrades to "panel missing", not a crash.
        return MCVer.getGui(event);
    }

    // ------------------------------------------------------------------ 2. no duplicate attachment

    @Inject(method = "onGuiInit", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void replaycompat$avoidDuplicatePanelMount(GuiScreenEvent.InitGuiEvent.Post event, CallbackInfo ci) {
        try {
            Object panel = readField(this, "panel");
            GuiScreen current = readCurrentScreen();
            if (panel == null || current == null) {
                return;
            }
            Object wrapper = invokeWrap(current);
            int copies = countChildCopies(wrapper, panel);
            if (copies > 0) {
                ci.cancel();
            }
        } catch (Throwable t) {
            try {
                System.out.println(TAG + "duplicate-mount check failed: " + t);
            } catch (Throwable ignored) {
                // logging must never break the injection
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private static String name(Object o) {
        return o == null ? "null" : o.getClass().getName();
    }

    private static GuiScreen readCurrentScreen() throws Exception {
        Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
        Object mc = invokeStatic(mcClass, "getMinecraft", "func_71410_x");
        if (mc == null) {
            return null;
        }
        Object screen = readField(mc, "currentScreen", "field_71462_r");
        return screen instanceof GuiScreen ? (GuiScreen) screen : null;
    }

    private static Object invokeWrap(GuiScreen screen) throws Exception {
        Class<?> type = Class.forName(
                "com.replaymod.lib.de.johni0702.minecraft.gui.container.VanillaGuiScreen");
        Method wrap = type.getMethod("wrap", GuiScreen.class);
        wrap.setAccessible(true);
        return wrap.invoke(null, screen);
    }

    private static int countChildCopies(Object wrapper, Object child) throws Exception {
        if (wrapper == null) {
            return 0;
        }
        Method getChildren = null;
        for (Class<?> c = wrapper.getClass(); c != null && getChildren == null; c = c.getSuperclass()) {
            try {
                getChildren = c.getDeclaredMethod("getChildren");
                getChildren.setAccessible(true);
            } catch (NoSuchMethodException ignored) {
                // keep walking up
            }
        }
        if (getChildren == null) {
            return -1;
        }
        Object result = getChildren.invoke(wrapper);
        if (!(result instanceof Iterable)) {
            return -1;
        }
        int n = 0;
        for (Object o : (Iterable<?>) result) {
            if (o == child) {
                n++;
            }
        }
        return n;
    }

    private static Object invokeStatic(Class<?> type, String... names) throws Exception {
        for (String n : names) {
            try {
                Method m = type.getMethod(n);
                m.setAccessible(true);
                return m.invoke(null);
            } catch (NoSuchMethodException ignored) {
                // try the next spelling
            }
        }
        return null;
    }

    private static Object readField(Object target, String... names) throws Exception {
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            for (String n : names) {
                try {
                    Field f = c.getDeclaredField(n);
                    f.setAccessible(true);
                    return f.get(target);
                } catch (NoSuchFieldException ignored) {
                    // try the next spelling / superclass
                }
            }
        }
        return null;
    }
}
