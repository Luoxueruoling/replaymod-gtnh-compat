package com.gtnh.replaycompat.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Keeps {@code KeyBinding} state alive while a replay is driven with the mouse unlocked, so ReplayMod's
 * own hotkeys (play/pause, add marker, night vision, ...) work again (HANDOVER 12.56).
 *
 * <h3>Why the hotkeys die</h3>
 *
 * <p>1.7.10's {@code Minecraft.runTick} only feeds keyboard events into {@code KeyBinding} while
 * {@code currentScreen == null || currentScreen.allowUserInput}. Our Ctrl lock makes the replay
 * overlay's input screen <b>permanently</b> the current screen (that is what makes the mouse usable),
 * so from that moment on {@code KeyBinding.setKeyBindState} is never called again and every hotkey that
 * polls a binding - which is how ReplayMod registers them - is dead. Tester report:
 * "the hotkeys are basically all decoration now", "the add-marker one is the one I need".
 *
 * <h3>Why this bridge, and why only while locked</h3>
 *
 * <p>{@code me.eigenraven.lwjgl3ify.api.InputEvents} delivers SDL key events on the render thread
 * (verified in 12.23) and, unlike {@code Keyboard.next()}, it is a <b>listener</b>: reading it does not
 * consume anything, so the GUI keeps receiving its own keys (chat, ESC). We only forward while the
 * player's Ctrl lock is on - with the lock off the game updates {@code KeyBinding} itself and forwarding
 * would double-fire.
 *
 * <p>Our own lock key (sprint / left Ctrl) is deliberately never forwarded: this patch owns it.
 */
public final class ReplayKeyBridge {

    private static final String TAG = "[replaycompat] ";
    /** LWJGL2 {@code Keyboard.KEY_LCONTROL}; the lock key, kept out of the bridge. */
    private static final int SPRINT_KEY_CODE = 29;

    private static boolean installed;
    private static boolean failed;
    private static Method methodSetKeyBindState;
    private static Method methodOnTick;

    private ReplayKeyBridge() {
    }

    /** Installs the listener once, on the first tick (which is the render thread). */
    public static void ensureInstalled() {
        if (installed || failed) {
            return;
        }
        try {
            Class<?> keyBinding = Class.forName("net.minecraft.client.settings.KeyBinding");
            methodSetKeyBindState = find(keyBinding, new Class<?>[]{int.class, boolean.class},
                    "setKeyBindState", "func_74510_a");
            methodOnTick = find(keyBinding, new Class<?>[]{int.class}, "onTick", "func_74507_a");

            Class<?> events = Class.forName("me.eigenraven.lwjgl3ify.api.InputEvents");
            Class<?> listener = Class.forName("me.eigenraven.lwjgl3ify.api.InputEvents$KeyboardListener");
            Method add = events.getMethod("addKeyboardListener", listener);
            Object proxy = Proxy.newProxyInstance(listener.getClassLoader(), new Class<?>[]{listener},
                    new Handler());
            add.invoke(null, proxy);
            installed = true;
            System.out.println(TAG + "replay key bridge installed (ReplayMod hotkeys stay alive while the"
                    + " mouse is unlocked)");
        } catch (Throwable t) {
            failed = true;
            System.out.println(TAG + "replay key bridge unavailable: " + t);
        }
    }

    private static final class Handler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            try {
                if ("onKeyEvent".equals(method.getName()) && args != null && args.length == 1) {
                    forward(args[0]);
                }
            } catch (Throwable ignored) {
                // never break input
            }
            return null;
        }
    }

    private static void forward(Object event) throws Exception {
        // With the lock off the game feeds KeyBinding itself - forwarding would double-fire.
        if (!ReplayMouseState.lockedVisible) {
            return;
        }
        Class<?> type = event.getClass();
        Field actionField = type.getField("action");
        Field keyField = type.getField("lwjgl2KeyCode");
        String action = String.valueOf(actionField.get(event));
        int key = keyField.getInt(event);
        if (key == SPRINT_KEY_CODE) {
            return; // our own lock key
        }
        if ("PRESSED".equals(action)) {
            methodSetKeyBindState.invoke(null, key, Boolean.TRUE);
            methodOnTick.invoke(null, key); // ReplayMod polls isPressed(), which needs pressTime
        } else if ("RELEASED".equals(action)) {
            methodSetKeyBindState.invoke(null, key, Boolean.FALSE);
        }
    }

    private static Method find(Class<?> owner, Class<?>[] params, String... names) throws NoSuchMethodException {
        for (Class<?> c = owner; c != null; c = c.getSuperclass()) {
            for (String name : names) {
                try {
                    Method m = c.getDeclaredMethod(name, params);
                    m.setAccessible(true);
                    return m;
                } catch (NoSuchMethodException ignored) {
                    // try the next spelling / superclass
                }
            }
        }
        throw new NoSuchMethodException(java.util.Arrays.toString(names) + " on " + owner.getName());
    }
}
