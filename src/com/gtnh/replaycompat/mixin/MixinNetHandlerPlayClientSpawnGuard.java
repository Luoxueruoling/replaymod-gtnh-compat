package com.gtnh.replaycompat.mixin;

import com.gtnh.replaycompat.util.NullWorldGuard;

import net.minecraft.network.play.server.S0CPacketSpawnPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

/**
 * Keeps ReplayMod's replay-loading phase from crashing on {@code S0CPacketSpawnPlayer} before a world
 * exists - <b>without losing the spawn</b>.
 *
 * <p><b>Symptom (crash-2026-10-10_15.29.18).</b> Clicking "load" in the replay viewer died with:
 *
 * <pre>
 * java.lang.NullPointerException: Cannot read field "field_72995_K" because "p_i45324_1_" is null
 *     at net.minecraft.entity.player.EntityPlayer.&lt;init&gt;(EntityPlayer.java:144)   // world.isRemote
 *     at net.minecraft.client.entity.AbstractClientPlayer.&lt;init&gt;
 *     at net.minecraft.client.entity.EntityOtherPlayerMP.&lt;init&gt;
 *     at net.minecraft.client.network.NetHandlerPlayClient.func_147237_a(NetHandlerPlayClient.java:464)
 *     at net.minecraft.network.play.server.S0CPacketSpawnPlayer.func_148833_a
 *     at com.replaymod.replay.gui.screen.GuiOpeningReplay.func_146269_k(GuiOpeningReplay.java:16)
 * </pre>
 *
 * <p>{@code func_147237_a} builds {@code new EntityOtherPlayerMP(clientWorldController, ...)}, and while
 * {@code GuiOpeningReplay} is still consuming the recording, {@code clientWorldController}
 * ({@code field_147300_g}) is not there yet, so the {@code EntityPlayer} constructor dereferences null.
 *
 * <p><b>Why our own recordings hit this.</b> The server-side recorder injects a synthetic
 * {@code S0CPacketSpawnPlayer} immediately after {@code S01PacketJoinGame} so that a replay contains a
 * player entity to attach the camera to (HANDOVER 2.7(1)) - and that is exactly the packet that
 * arrives while the replay is still opening.
 *
 * <h3>Drop vs defer - why this was changed</h3>
 *
 * <p>The first version of this guard simply dropped the packet. That stopped the crash but had a
 * visible cost reported from the field: <b>the player is not rendered at all until something makes the
 * client spawn it again</b> - e.g. changing dimension inside the replay. "切换维度之后玩家才渲染出来，
 * 没切换之前没有渲染".
 *
 * <p>So the packet is now handed to {@link NullWorldGuard#defer}, which holds it and lets
 * {@code MixinNetworkManagerDeferredFlush} put it back onto the connection's receive queue once the
 * world exists. Re-queueing (rather than reflective dispatch) means vanilla processes it through the
 * normal pipeline, so no exception behaviour changes. The timing is safe because
 * {@code Minecraft.theWorld} - the signal that flush waits for - is only assigned at the very end of
 * loading, long after {@code clientWorldController} has been created by {@code S01PacketJoinGame}.
 *
 * <p>The world reference is read reflectively ({@code field_147300_g} is private) so this patch needs
 * neither a refmap nor an access transformer, and both MCP/SRG spellings are tried.
 */
@Mixin(targets = "net.minecraft.client.network.NetHandlerPlayClient")
public abstract class MixinNetHandlerPlayClientSpawnGuard {

    private static final String TAG = "[replaycompat] ";

    @Inject(method = "func_147237_a", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void replaycompat$deferSpawnWithoutWorld(S0CPacketSpawnPlayer packet, CallbackInfo ci) {
        try {
            Object world = readField(this, "clientWorldController", "field_147300_g");
            if (world == null) {
                // Do NOT drop it: hold it until the world exists, otherwise the replayed player never
                // appears (that is exactly what "the player only shows up after changing dimension" was).
                NullWorldGuard.defer(packet);
                NullWorldGuard.logOnce("replay loading: holding S0CPacketSpawnPlayer until the client world exists"
                        + " (it will be replayed)");
                ci.cancel();
            }
        } catch (Throwable t) {
            // Never let the guard itself break packet handling -- fall through to vanilla behaviour.
            System.out.println(TAG + "spawn guard could not read the client world: " + t);
        }
    }

    private static Object readField(Object target, String... names) throws Exception {
        Field field = findField(target.getClass(), names);
        return field.get(target);
    }

    private static Field findField(Class<?> type, String... names) throws NoSuchFieldException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (String name : names) {
                try {
                    Field f = c.getDeclaredField(name);
                    f.setAccessible(true);
                    return f;
                } catch (NoSuchFieldException ignored) {
                    // try the next spelling / superclass
                }
            }
        }
        throw new NoSuchFieldException(String.valueOf((Object) names[0]) + " (tried "
                + java.util.Arrays.toString(names) + ") on " + type.getName());
    }
}
