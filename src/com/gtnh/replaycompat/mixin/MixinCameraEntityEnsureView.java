package com.gtnh.replaycompat.mixin;

import com.replaymod.core.versions.MCVer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Makes the replay actually use ReplayMod's own camera entity as the view entity.
 *
 * <h3>Symptom (reported from the field)</h3>
 *
 * <p>Opening a replay drops you straight into the <b>player's first-person view</b>: you see your own
 * hand, you cannot see your character, and rendering a video therefore contains a floating arm and no
 * player model. "好像默认就进入了玩家视角" / "渲染还把我手渲染上了".
 *
 * <h3>Root cause</h3>
 *
 * <p>Upstream sets the view entity only in {@code ReplayHandler.spectateEntity(EntityLivingBase)}:
 *
 * <pre>
 * if (mc.renderViewEntity != e) {
 *     mc.renderViewEntity = e;
 *     cameraEntity.setCameraPosRot(e);
 * }
 * </pre>
 *
 * and that method is called <em>only</em> when something asks to spectate (clicking an entity,
 * {@code SpectatorProperty} keyframes, the player overview). <b>Nothing sets it when a replay is
 * opened.</b> Verified against the shipped jar: every reference to {@code Minecraft.field_71451_h}
 * (renderViewEntity) and {@code field_71439_g} (thePlayer) is a {@code getfield} - the only writers
 * are {@code spectateEntity} and {@code CameraEntity}'s own spectating logic. There is also no
 * {@code setThePlayer} anywhere, so {@code ReplayHandler#isCameraView()} - which requires
 * {@code mc.thePlayer instanceof CameraEntity} - is <b>permanently false on 1.7.10</b>.
 *
 * <p>Consequence: {@code renderViewEntity} keeps whatever {@code Minecraft} last assigned (the local
 * player), so the game renders in first person and {@code CameraEntity}'s own hand guard
 * (<code>if (mc.renderViewEntity == CameraEntity.this || !(mc.renderViewEntity instanceof EntityPlayer))
 * event.setCanceled(true);</code>) never fires either - hence the visible arm.
 *
 * <h3>Fix</h3>
 *
 * <p>Once per tick, if we are <b>not</b> spectating (the replay handler's {@code spectatedUUID} is
 * null) and the view entity is not this camera, point it back at the camera - exactly what
 * {@code spectateCamera()} (= {@code spectateEntity(null)}) does, just applied automatically after
 * loading. Spectating is left completely alone, so clicking a player still works.
 *
 * <p>{@code MCVer.getRenderViewEntity/setRenderViewEntity} are ReplayMod's own accessors (the
 * Minecraft field is private and this project has no reobf), so no reflection is needed for the swap
 * itself; the reflection here is only for {@code Minecraft.getMinecraft} (an SRG name) and
 * {@code ReplayModReplay.instance.getReplayHandler().getSpectatedUUID()}.
 */
@Mixin(targets = "com.replaymod.replay.camera.CameraEntity")
public abstract class MixinCameraEntityEnsureView {

    private static final String TAG = "[replaycompat] ";

    private static Method getMinecraft;      // Minecraft.getMinecraft / func_71410_x
    private static Method getReplayHandler;  // ReplayModReplay#getReplayHandler
    private static Method getSpectatedUUID;  // ReplayHandler#getSpectatedUUID
    private static boolean resolved;
    private static boolean warned;
    private static int logCount;

    @Inject(method = { "func_70071_h_", "func_70071_h" }, at = @At("TAIL"), remap = false, require = 0)
    private void replaycompat$ensureCameraIsViewEntity(CallbackInfo ci) {
        try {
            if (!replaycompat$resolve()) {
                return;
            }
            Object mc = getMinecraft.invoke(null);
            if (mc == null) {
                return;
            }
            Object handler = replaycompat$instance == null ? null
                    : getReplayHandler.invoke(replaycompat$instance);
            Object spectating = handler == null ? null : getSpectatedUUID.invoke(handler);
            Object camera = this;

            Object view = MCVer.getRenderViewEntity((net.minecraft.client.Minecraft) mc);
            if (view == camera) {
                return; // already correct
            }
            if (spectating != null) {
                replaycompat$log("camera view: spectating " + spectating + ", leaving the view entity alone");
                return;
            }
            MCVer.setRenderViewEntity((net.minecraft.client.Minecraft) mc,
                    (net.minecraft.entity.EntityLivingBase) camera);
            replaycompat$log("camera view: renderViewEntity was "
                    + (view == null ? "null" : view.getClass().getName())
                    + " instead of the replay camera - restored it (this is why the first-person hand"
                    + " was rendered and the player was invisible)");
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                try {
                    System.out.println(TAG + "camera view check failed: " + t);
                } catch (Throwable ignored) {
                    // logging must never escape
                }
            }
        }
    }

    private static boolean replaycompat$resolve() {
        if (resolved) {
            return getMinecraft != null && getReplayHandler != null && getSpectatedUUID != null;
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
            Field instance = replayModReplay.getField("instance");
            replaycompat$instance = instance.get(null);

            Class<?> handlerClass = Class.forName("com.replaymod.replay.ReplayHandler");
            getSpectatedUUID = handlerClass.getMethod("getSpectatedUUID");
        } catch (Throwable t) {
            getMinecraft = null;
            getReplayHandler = null;
            getSpectatedUUID = null;
            return false;
        }
        return true;
    }

    /** {@code ReplayModReplay.instance} - stored so the static handler lookup does not need the field again. */
    private static Object replaycompat$instance;

    private static void replaycompat$log(String message) {
        try {
            if (logCount >= 5) {
                return;
            }
            logCount++;
            System.out.println(TAG + message);
        } catch (Throwable ignored) {
            // logging must never escape
        }
    }
}
