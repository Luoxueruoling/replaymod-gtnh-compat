package com.gtnh.replaycompat.mixin;

import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Bridges mouse input into jGui for vanilla screens that jGui only <em>wraps</em> instead of owning
 * - in practice the Custom Main Menu screen, so that ReplayMod's "saving replay" panel becomes
 * clickable there.
 *
 * <h3>Why this is needed</h3>
 *
 * Normally jGui receives mouse input like this:
 *
 * <pre>
 * ReplayMod MixinGuiScreen.handleInput()      // hijacks GuiScreen.func_146269_k
 *   -&gt; while (Mouse.next()) post(new VanillaGuiScreen.MouseInputEvent())
 *        -&gt; VanillaGuiScreen.EventHandler.onMouseInput()   {@code @SubscribeEvent(priority = LOWEST)}
 *             -&gt; getSuperMcGui().handleMouseInput()        // the jGui screen tree
 * </pre>
 *
 * On GTNH that middle step never fires. Measured with probes (see HANDOVER 12.16):
 * <ul>
 *   <li>the event <b>is</b> constructed/posted - {@code MouseInputEvent} ctor hit 12+ times;</li>
 *   <li>{@code EventHandler} <b>is</b> alive - {@code onGuiRender} fires;</li>
 *   <li>but {@code onMouseInput} <b>never</b> fires - 0 hits, while the panel is drawn and dead.</li>
 * </ul>
 *
 * The cause is in jGui's own registration lifecycle: {@code EventRegistrations.register()} is what
 * performs {@code MinecraftForge.EVENT_BUS.register(this)}, but
 * {@code VanillaGuiScreen.wrap()} calls {@code register()} <em>only when it has to create a new
 * wrapper</em> - a cache hit returns the cached wrapper without re-registering - while
 * {@code EventHandler.onGuiClosed} unconditionally calls {@code unregister()} on every
 * {@code GuiOpenEvent}. A cached wrapper therefore ends up deregistered, and being
 * {@code EventPriority.LOWEST} it is also the first to be skipped once anything cancels the event.
 *
 * <h3>What this does</h3>
 *
 * Instead of relying on that event hop, call the jGui screen tree directly from vanilla's own mouse
 * pump: whenever the <em>currently displayed</em> screen handles a mouse event and that screen is not
 * itself a jGui screen, hand the same event to the jGui screen that wraps it. Vanilla behaviour is
 * left untouched (the method body still runs), so both the vanilla buttons and the jGui panel work.
 *
 * <h3>Recursion safety</h3>
 *
 * jGui's own MC screen ({@code AbstractGuiScreen$MinecraftGuiScreen}) also extends {@code GuiScreen},
 * so this injection would fire for it as well. That is excluded twice over: only the screen that
 * equals {@code mc.currentScreen} is bridged, and jGui's own screens are skipped by name. If jGui's
 * screen ever did become the current screen, the bridge would simply do nothing, because the event
 * path already works for real jGui screens (verified: the replay viewer's wheel scrolling works).
 */
@Mixin(targets = "net.minecraft.client.gui.GuiScreen")
public abstract class MixinGuiScreenJGuiInputBridge {

    private static final String TAG = "[replaycompat] ";
    private static int replaycompat$bridgeLog = 0;

    @Inject(method = "func_146274_d", at = @At("HEAD"), remap = false, require = 0)
    private void replaycompat$forwardMouseToJGui(CallbackInfo ci) {
        try {
            Object current = readCurrentScreen();
            // Only the screen actually on display, and only when *this* is that screen.
            if (current == null || current != (Object) this) {
                return;
            }
            String name = current.getClass().getName();
            // jGui's own screens already receive input through the normal path.
            if (name.contains("MinecraftGuiScreen")) {
                return;
            }

            Object wrapper = invokeWrap(current);
            if (wrapper == null) {
                return;
            }
            // VanillaGuiScreen.getSuperMcGui() returns jGui's own MC screen (private method).
            // Both are jGui methods, which Forge never remaps - the literal names are correct.
            Object jGuiScreen = invokeNoArg(wrapper, "getSuperMcGui");
            if (jGuiScreen == null) {
                return;
            }
            // jGui's screen extends Minecraft's GuiScreen, so its mouse pump carries an SRG name at
            // runtime: func_146274_d. Looking it up as "handleMouseInput" (the MCP/dev name) silently
            // finds nothing and the whole bridge becomes a no-op - try both spellings (pit 29).
            invokeNoArg(jGuiScreen, "handleMouseInput", "func_146274_d");
        } catch (Throwable t) {
            try {
                System.out.println(TAG + "input bridge failed: " + t);
            } catch (Throwable ignored) {
                // never let the bridge break vanilla input
            }
        }
    }

    private static Object readCurrentScreen() throws Exception {
        Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
        Object mc = invokeStatic(mcClass, "getMinecraft", "func_71410_x");
        if (mc == null) {
            return null;
        }
        return readField(mc, "currentScreen", "field_71462_r");
    }

    private static Object invokeWrap(Object screen) throws Exception {
        Class<?> type = Class.forName(
                "com.replaymod.lib.de.johni0702.minecraft.gui.container.VanillaGuiScreen");
        Method wrap = type.getMethod("wrap", GuiScreen.class);
        wrap.setAccessible(true);
        return wrap.invoke(null, (GuiScreen) screen);
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

    private static Object invokeNoArg(Object target, String... names) throws Exception {
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            for (String name : names) {
                try {
                    Method m = c.getDeclaredMethod(name);
                    m.setAccessible(true);
                    return m.invoke(target);
                } catch (NoSuchMethodException ignored) {
                    // try the next spelling / superclass
                }
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
