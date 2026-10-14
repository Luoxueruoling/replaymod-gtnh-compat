package com.gtnh.replaycompat.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * GTNH replay-compatibility patch for Et Futurum Requiem.
 *
 * <p><b>Why this lives in the ReplayMod adapter.</b> It used to be
 * {@code com.gtnh.serverreplay.mixin.MixinEtFuturumPlayer}, registered from
 * {@code mixins.serverreplay.json}. That made "can a GTNH replay open at all" depend on the
 * <i>server-side recording mod</i>: as soon as ServerReplay was moved to {@code mods-disabled/} for a
 * single-variable experiment, this patch disappeared with it and every replay died on the loading
 * screen with the NPE below. Opening replays is pure client behaviour, so the patch belongs here.
 *
 * <p><b>The defect.</b> {@code EtFuturumPlayer.init(Entity, World)} is called from Et Futurum's
 * injection into {@code Entity.<init>} and immediately dereferences its {@code world} argument
 * (crash line 48: {@code Cannot read field "field_73012_v" because "world" is null}). During a
 * ReplayMod replay the world is legitimately absent while entities are still being constructed, so
 * every replayed entity construction throws:
 *
 * <pre>
 * java.lang.NullPointerException: Cannot read field "field_73012_v" because "world" is null
 *     at ganymedes01.etfuturum.storage.EtFuturumPlayer.init(EtfuturumPlayer.java:48)
 *     at net.minecraft.entity.Entity.&lt;init&gt;(Entity.java:183)
 *     at net.minecraft.client.entity.EntityOtherPlayerMP.&lt;init&gt;
 *     at net.minecraft.network.play.server.S0CPacketSpawnPlayer.func_148833_a
 *     at com.replaymod.replay.gui.screen.GuiOpeningReplay.func_146269_k
 * </pre>
 *
 * <p>This is not merely a crash: ReplayMod's own camera entity registers its HUD suppression handler
 * ({@code RenderGameOverlayEvent.Pre} cancelling HEALTH/FOOD/HOTBAR/...) <em>after</em> the entity is
 * constructed. When the constructor throws, that registration never happens — which is why a GTNH
 * replay both shows the vanilla HUD and appears to contain no player.
 *
 * <p>ReplayMod's own client recordings hit this too: Forge keeps their {@code JoinGame} inside an
 * {@code FMLProxyPacket} until mod negotiation finishes (observed at 26 s), while {@code SpawnPlayer}
 * arrives at 2.7 s. Skipping the initialisation when there is no world is therefore the difference
 * between a replay that opens and one that dies on the loading screen.
 *
 * <p>Et Futurum is a regular Forge mod, not Minecraft itself, so {@code remap = false} is required:
 * the target method and its descriptor must be matched literally.
 *
 * <p><b>★ Class-name casing matters, and the original got it wrong.</b> The class in
 * {@code etfuturum-2.6.58-GTNH.jar} is {@code ganymedes01/etfuturum/storage/EtFuturumPlayer.class} —
 * capital {@code F} in {@code EtFuturumPlayer}. Both this patch's predecessor (ServerReplay's
 * {@code MixinEtFuturumPlayer}) and the first version of this file targeted
 * {@code ...storage.EtfuturumPlayer} (lowercase {@code f}). Mixin matches target names as strings, so
 * the mismatch meant the mixin silently never applied:
 *
 * <pre>
 *   [RFB-Main/WARN] [mixin/]: @Mixin target ganymedes01.etfuturum.storage.EtfuturumPlayer was not found
 *                            mixins.replaycompat.json:MixinEtFuturumPlayer
 * </pre>
 *
 * and every replay kept dying with the NPE the patch was supposed to prevent. (Crash reports show the
 * lowercase spelling, which is why the wrong casing looked plausible.)
 */
@Mixin(targets = "ganymedes01.etfuturum.storage.EtFuturumPlayer")
public abstract class MixinEtFuturumPlayer {

    @Inject(method = "init", at = @At("HEAD"), cancellable = true, remap = false)
    private void replaycompat$skipInitWithoutWorld(Entity entity, World world, CallbackInfo ci) {
        if (world == null) {
            // Nothing to initialise against; Et Futurum only stores per-player state here, and the
            // real initialisation happens again once the player exists in a world.
            ci.cancel();
        }
    }
}
