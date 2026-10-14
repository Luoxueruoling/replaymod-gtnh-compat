package com.gtnh.replaycompat.mixin;

import com.replaymod.lib.de.johni0702.minecraft.gui.utils.lwjgl.ReadablePoint;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;

/**
 * Diagnostic probe on jGui's clickable element, to settle the last layer of the "saving panel is
 * drawn but does not react" problem.
 *
 * <p>jGui's click semantics were confirmed from bytecode:
 *
 * <pre>
 * AbstractGuiClickable.mouseClick(pos, button) {
 *     Point p = new Point(pos);
 *     if (getContainer() != null) getContainer().convertFor(this, p);   // screen -&gt; element-local
 *     if (isMouseHovering(p) &amp;&amp; isEnabled()) { onClick(); return true; }
 *     return false;
 * }
 * // isMouseHovering: 0 &lt; x &lt; getLastSize().width &amp;&amp; 0 &lt; y &lt; getLastSize().height
 * </pre>
 *
 * So a button fires purely on {@code mouseClick} (never on release), and the only two ways it can
 * stay silent are: this method is never reached, or it is reached but the element-local coordinates
 * fall outside {@code getLastSize()} (e.g. the element has not been laid out yet, so its size is 0).
 *
 * <p>This probe therefore prints, for each call: the raw position, the element class,
 * {@code isEnabled()} and {@code getLastSize()} (both reflectively - they are jGui methods, which
 * Forge does not remap, but this mixin does not extend the target so it cannot call them directly),
 * plus the eventual return value. Observes only.
 */
@Mixin(targets = "com.replaymod.lib.de.johni0702.minecraft.gui.element.AbstractGuiClickable")
public abstract class MixinAbstractGuiClickableProbe {

    private static int replaycompat$calls = 0;

    @Inject(method = "mouseClick", at = @At("HEAD"), remap = false, require = 0)
    private void replaycompat$traceClickHead(ReadablePoint pos, int button, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (replaycompat$calls >= 25) {
                return;
            }
            replaycompat$calls++;
            System.out.println("[replaycompat] click probe #" + replaycompat$calls
                    + " class=" + this.getClass().getName()
                    + " pos=(" + safeInt(pos, "getX") + "," + safeInt(pos, "getY") + ")"
                    + " button=" + button
                    + " lastSize=" + describeSize(this)
                    + " enabled=" + invoke(this, "isEnabled"));
        } catch (Throwable t) {
            try {
                System.out.println("[replaycompat] click probe head failed: " + t);
            } catch (Throwable ignored) {
                // logging must never break input
            }
        }
    }

    @Inject(method = "mouseClick", at = @At("RETURN"), remap = false, require = 0)
    private void replaycompat$traceClickReturn(ReadablePoint pos, int button, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (replaycompat$calls <= 25) {
                System.out.println("[replaycompat] click probe RETURN class=" + this.getClass().getName()
                        + " -> " + cir.getReturnValue());
            }
        } catch (Throwable ignored) {
            // logging must never break input
        }
    }

    private static String describeSize(Object element) {
        try {
            Object size = invoke(element, "getLastSize");
            if (size == null) {
                return "null";
            }
            return safeInt(size, "getWidth") + "x" + safeInt(size, "getHeight");
        } catch (Throwable t) {
            return "err(" + t + ")";
        }
    }

    private static Object invoke(Object target, String name) {
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            try {
                Method m = c.getDeclaredMethod(name);
                m.setAccessible(true);
                return m.invoke(target);
            } catch (NoSuchMethodException ignored) {
                // keep walking up
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }

    private static String safeInt(Object target, String name) {
        Object v = invoke(target, name);
        return v == null ? "?" : String.valueOf(v);
    }
}
