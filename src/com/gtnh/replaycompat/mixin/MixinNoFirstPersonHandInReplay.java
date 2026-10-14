package com.gtnh.replaycompat.mixin;

import com.replaymod.core.versions.MCVer;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.camera.CameraEntity;

import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

/**
 * Removes the first-person hand from replays - the counterpart of {@link MixinHideHudInReplay}, and for
 * exactly the same reason.
 *
 * <h3>Symptom</h3>
 *
 * <p>A replay still shows the player's own arm/hand in the corner ("手还在"), which also ends up in
 * rendered videos.
 *
 * <h3>Why ReplayMod's own guard does not work here</h3>
 *
 * <p>{@code CameraEntity$EventHandler#onRenderHand} cancels {@code RenderHandEvent} whenever the view
 * entity is not the spectated player - but on this pack that event is not a reliable hook (the same
 * class of problem as the missing {@code HEALTH} overlay event, HANDOVER 12.29). The hand is drawn by
 * {@code EntityRenderer.renderHand} calling into {@code ItemRenderer.renderItemInFirstPerson}, and
 * nothing in that path is guaranteed to raise the Forge event.
 *
 * <h3>What this does</h3>
 *
 * <p>Skips {@code EntityRenderer.renderHand} itself, but only while a replay is open and only when we
 * are <b>not</b> spectating a player - which mirrors upstream's semantics exactly:
 *
 * <pre>
 *   if (mc.renderViewEntity == CameraEntity.this || !(mc.renderViewEntity instanceof EntityPlayer))
 *       event.setCanceled(true);
 * </pre>
 *
 * <p>So watching a player's own first-person view (clicking them / a spectator keyframe) still shows
 * their hand, exactly as intended; the free camera no longer shows yours.
 *
 * <p>"A replay is open" is decided from {@code ReplayModReplay.instance.getReplayHandler() != null}
 * rather than from the view entity, because while spectating the view entity is somebody else.
 *
 * <p>{@code renderHand} is a private Minecraft method, so it is addressed by its SRG name
 * ({@code func_78476_a}); the MCP spelling is included as a fallback and both are advisory
 * ({@code require = 0}).
 */
@Mixin(targets = "net.minecraft.client.renderer.ItemRenderer")
public abstract class MixinNoFirstPersonHandInReplay {

    private static Method getMinecraft;
    private static Method getReplayHandler;
    private static Object replayModReplayInstance;
    private static boolean resolved;
    private static boolean warned;

    /**
     * {@code ItemRenderer.renderItemInFirstPerson(float)} - the method that actually draws the arm and
     * the held item. Its SRG name {@code func_78440_a} was verified to exist (unlike
     * {@code EntityRenderer.renderHand}, whose guessed name silently matched nothing).
     */
    @Inject(method = { "func_78440_a", "renderItemInFirstPerson", "func_78447_b", "renderOverlays" }, at = @At("HEAD"), cancellable = true,
            remap = false, require = 0)
    private void replaycompat$noFirstPersonHandInReplay(float partialTicks, CallbackInfo ci) {
        try {
            if (!replaycompat$resolve()) {
                return;
            }
            Object handler = replayModReplayInstance == null ? null
                    : getReplayHandler.invoke(replayModReplayInstance);
            if (handler == null) {
                return; // not in a replay
            }
            Object mc = getMinecraft.invoke(null);
            if (mc == null) {
                return;
            }
            Object view = MCVer.getRenderViewEntity((net.minecraft.client.Minecraft) mc);
            // Spectating a player on purpose? Their hand is what we want to see - leave it alone.
            if (view instanceof EntityPlayer && !(view instanceof CameraEntity)) {
                return;
            }
            ci.cancel();
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                try {
                    System.out.println("[replaycompat] hand guard failed: " + t);
                } catch (Throwable ignored) {
                    // logging must never escape
                }
            }
        }
    }

    private static boolean replaycompat$resolve() {
        if (resolved) {
            return getMinecraft != null && getReplayHandler != null;
        }
        resolved = true;
        try {
            Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
            for (String name : new String[] { "getMinecraft", "func_71410_x" }) {
                try {
                    getMinecraft = mcClass.getMethod(name);
                    break;
                } catch (NoSuchMethodException ignored) {
                    // try the next spelling
                }
            }
            Class<?> replayModReplay = Class.forName("com.replaymod.replay.ReplayModReplay");
            getReplayHandler = replayModReplay.getMethod("getReplayHandler");
            replayModReplayInstance = replayModReplay.getField("instance").get(null);
        } catch (Throwable t) {
            getMinecraft = null;
            getReplayHandler = null;
            replayModReplayInstance = null;
            return false;
        }
        return getMinecraft != null && getReplayHandler != null;
    }
}
