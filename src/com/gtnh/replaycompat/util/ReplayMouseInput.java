package com.gtnh.replaycompat.util;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Delivers middle-click presses to the replay overlay.
 *
 * <h3>Why this class exists (HANDOVER 12.23)</h3>
 *
 * <p>Polling for the middle button does not work reliably on this pack:
 *
 * <ul>
 *   <li>{@code org.lwjgl.input.Mouse.isButtonDown(2)} always reads {@code false} - the SDL state array
 *       is not maintained here (same problem as {@code Keyboard.isKeyDown}, pit 49);</li>
 *   <li>polling {@code keyBindPickBlock} (key code {@code -98} = {@code middle - 100}, which is how the
 *       game maps mouse buttons onto bindings) does work, but its press/release state is delivered
 *       through LWJGL2's event queue, which never processes the queue's last remaining event - so a
 *       release can be swallowed and the next press goes unnoticed.</li>
 * </ul>
 *
 * <p>Forge's {@code InputEvent.MouseInputEvent} is fired from inside the game's mouse event loop, i.e.
 * exactly while {@code Mouse.getEventButton()/getEventButtonState()} are valid, and only when there is
 * a real event. Consuming it through an edge-triggered listener therefore needs no polling, no
 * edge detection and no de-bouncing.
 *
 * <p>This class lives in {@code com.gtnh.replaycompat.util}, <b>not</b> in the mixin package: helper
 * classes inside a declared mixin package cannot be referenced directly (HANDOVER pit 42).
 *
 * <p>The listener is registered lazily (once) through {@code MinecraftForge.EVENT_BUS} by reflection,
 * so this project keeps its "no compile-time dependency beyond what the classpath already offers"
 * property and cannot break if the bus moves.
 */
public class ReplayMouseInput {

    private static final String TAG = "[replaycompat] ";

    /** LWJGL2 button index of the middle mouse button. */
    private static final int MIDDLE_BUTTON = 2;

    /** Set from the event handler (may run on the render thread), read once per frame. */
    private static volatile boolean pending;

    private static boolean registered;

    /** Diagnostic counter - proves whether the Forge mouse event reaches us at all. */
    private static int eventLogCount;

    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseInputEvent event) {
        try {
            Class<?> mouseClass = Class.forName("org.lwjgl.input.Mouse");
            Object button = mouseClass.getMethod("getEventButton").invoke(null);
            Object state = mouseClass.getMethod("getEventButtonState").invoke(null);
            if (eventLogCount < 25) {
                eventLogCount++;
                System.out.println(TAG + "mouse event reached listener: button=" + button + " state=" + state);
            }
            if (button instanceof Integer && ((Integer) button) == MIDDLE_BUTTON
                    && state instanceof Boolean && (Boolean) state) {
                pending = true;
            }
        } catch (Throwable ignored) {
            // an input listener must never be able to break the game
        }
    }

    /** Returns true once for every middle-click; the flag is cleared on read. */
    public static boolean consumeMiddleClick() {
        if (pending) {
            pending = false;
            return true;
        }
        return false;
    }

    /** Idempotently registers this listener on the event buses. */
    public static void ensureRegistered() {
        if (registered) {
            return;
        }
        registered = true;
        Object listener = new ReplayMouseInput();
        // InputEvent (KeyInputEvent / MouseInputEvent) are FML *game* events: on 1.7.10 they are
        // dispatched from FMLCommonHandler.instance().bus(), which is a different bus from
        // MinecraftForge.EVENT_BUS. Registering only on the Forge bus is exactly why v45/v46 logged
        // "registered" and then never received a single event. Both are registered here so this keeps
        // working regardless of which bus the pack dispatches from.
        replaycompat$registerOnForgeBus(listener);
        replaycompat$registerOnFmlBus(listener);
    }

    private static void replaycompat$registerOnForgeBus(Object listener) {
        try {
            Class<?> forgeClass = Class.forName("net.minecraftforge.common.MinecraftForge");
            Object bus = forgeClass.getField("EVENT_BUS").get(null);
            bus.getClass().getMethod("register", Object.class).invoke(bus, listener);
            System.out.println(TAG + "replay mouse: registered on MinecraftForge.EVENT_BUS");
        } catch (Throwable t) {
            System.out.println(TAG + "replay mouse: MinecraftForge.EVENT_BUS registration failed: " + t);
        }
    }

    private static void replaycompat$registerOnFmlBus(Object listener) {
        try {
            Class<?> handlerClass = Class.forName("cpw.mods.fml.common.FMLCommonHandler");
            Object handler = handlerClass.getMethod("instance").invoke(null);
            Object bus = handler.getClass().getMethod("bus").invoke(handler);
            bus.getClass().getMethod("register", Object.class).invoke(bus, listener);
            System.out.println(TAG + "replay mouse: registered on FMLCommonHandler.bus() (" + bus.getClass().getName() + ")");
        } catch (Throwable t) {
            System.out.println(TAG + "replay mouse: FMLCommonHandler.bus() registration failed: " + t);
        }
    }
}
