package com.gtnh.replaycompat.mixin;

import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.network.play.server.S05PacketSpawnPosition;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.network.play.server.S0DPacketCollectItem;
import net.minecraft.network.play.server.S0FPacketSpawnMob;
import net.minecraft.network.play.server.S24PacketBlockAction;
import net.minecraft.network.play.server.S25PacketBlockBreakAnim;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.network.play.server.S28PacketEffect;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.network.play.server.S33PacketUpdateSign;
import net.minecraft.network.play.server.S34PacketMaps;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;

import com.gtnh.replaycompat.util.NullWorldGuard;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps ReplayMod's replay-loading phase from crashing on packets whose 1.7.10 handler dereferences
 * {@code Minecraft.theWorld} without a null check.
 *
 * <p><b>Symptom.</b> Opening a replay dies with (crash-2026-10-10_18.04.46 / mclo.gs/uJ0PkXJ):
 *
 * <pre>
 * java.lang.NullPointerException: Cannot invoke "WorldClient.func_147443_d(int,int,int,int,int)"
 *   because "this.field_147299_f.field_71441_e" is null
 *     at net.minecraft.client.network.NetHandlerPlayClient.func_147294_a(NetHandlerPlayClient.java:1182)
 *     at net.minecraft.network.play.server.S25PacketBlockBreakAnim.func_148833_a
 *     at com.replaymod.replay.gui.screen.GuiOpeningReplay.func_146269_k(GuiOpeningReplay.java:16)
 * </pre>
 *
 * <p><b>Why it happens on GTNH specifically.</b> While the loading screen drives the replay's packet
 * stream, {@code Minecraft.theWorld} is not assigned yet - but {@code clientWorldController} was
 * already created by {@code S01PacketJoinGame}. Most handlers use the latter and survive; the 14
 * methods guarded here use the former and are guaranteed NPEs. Any GTNH recording can trigger this,
 * because a server that is being mined emits {@code S25} continuously and the very beginning of a
 * recording therefore contains mining progress.
 *
 * <p><b>Why the existing SpawnGuard did not catch it.</b> That guard reads
 * {@code clientWorldController} ({@code field_147300_g}); this crash is about
 * {@code Minecraft.theWorld} ({@code field_71441_e}). They are different fields with different
 * lifetimes, so the older guard is simply not on this path.
 *
 * <p><b>Treatment.</b> Transient visuals/sounds are dropped. Packets that carry state are handed to
 * {@link NullWorldGuard#defer} and re-queued by {@link MixinNetworkManagerDeferredFlush} once a world
 * exists, so a replay does not silently lose its tile entities, maps or signs.
 *
 * <p>Like the other adapter patches this class avoids {@code @Shadow} (see HANDOVER 5.3): the world is
 * read reflectively, trying MCP and SRG spellings, and every reflective access is wrapped so the guard
 * itself can never be the thing that breaks packet handling.
 */
@Mixin(targets = "net.minecraft.client.network.NetHandlerPlayClient")
public abstract class MixinNetHandlerPlayClientNullWorldGuard {

    // ------------------------------------------------------------------ transient: drop

    /** S25PacketBlockBreakAnim - the packet that actually crashes GTNH replays. */
    @Inject(method = "func_147294_a", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void guard$dropBlockBreakAnim(CallbackInfo ci) {
        if (NullWorldGuard.worldMissing(this)) {
            ci.cancel();
        }
    }

    @Inject(method = "func_147261_a", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void guard$dropBlockAction(S24PacketBlockAction packet, CallbackInfo ci) {
        if (NullWorldGuard.worldMissing(this)) {
            ci.cancel();
        }
    }

    @Inject(method = "func_147277_a", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void guard$dropEffect(S28PacketEffect packet, CallbackInfo ci) {
        if (NullWorldGuard.worldMissing(this)) {
            ci.cancel();
        }
    }

    @Inject(method = "func_147283_a", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void guard$dropExplosion(S27PacketExplosion packet, CallbackInfo ci) {
        if (NullWorldGuard.worldMissing(this)) {
            ci.cancel();
        }
    }

    @Inject(method = "func_147255_a", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void guard$dropSoundEffect(S29PacketSoundEffect packet, CallbackInfo ci) {
        if (NullWorldGuard.worldMissing(this)) {
            ci.cancel();
        }
    }

    @Inject(method = "func_147279_a", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void guard$dropAnimation(S0BPacketAnimation packet, CallbackInfo ci) {
        if (NullWorldGuard.worldMissing(this)) {
            ci.cancel();
        }
    }

    @Inject(method = "func_147246_a", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void guard$dropCollectItem(S0DPacketCollectItem packet, CallbackInfo ci) {
        if (NullWorldGuard.worldMissing(this)) {
            ci.cancel();
        }
    }

    // ------------------------------------------------------------------ stateful: defer

    @Inject(method = "func_147273_a", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void guard$deferUpdateTileEntity(S35PacketUpdateTileEntity packet, CallbackInfo ci) {
        if (NullWorldGuard.worldMissing(this)) {
            NullWorldGuard.defer(packet);
            ci.cancel();
        }
    }

    @Inject(method = "func_147281_a", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void guard$deferSpawnMob(S0FPacketSpawnMob packet, CallbackInfo ci) {
        if (NullWorldGuard.worldMissing(this)) {
            NullWorldGuard.defer(packet);
            ci.cancel();
        }
    }

    @Inject(method = "func_147280_a", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void guard$deferRespawn(S07PacketRespawn packet, CallbackInfo ci) {
        if (NullWorldGuard.worldMissing(this)) {
            NullWorldGuard.defer(packet);
            ci.cancel();
        }
    }

    @Inject(method = "func_147248_a", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void guard$deferUpdateSign(S33PacketUpdateSign packet, CallbackInfo ci) {
        if (NullWorldGuard.worldMissing(this)) {
            NullWorldGuard.defer(packet);
            ci.cancel();
        }
    }

    @Inject(method = "func_147264_a", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void guard$deferMaps(S34PacketMaps packet, CallbackInfo ci) {
        if (NullWorldGuard.worldMissing(this)) {
            NullWorldGuard.defer(packet);
            ci.cancel();
        }
    }

    @Inject(method = "func_147285_a", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void guard$deferTimeUpdate(S03PacketTimeUpdate packet, CallbackInfo ci) {
        if (NullWorldGuard.worldMissing(this)) {
            NullWorldGuard.defer(packet);
            ci.cancel();
        }
    }

    @Inject(method = "func_147271_a", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void guard$deferSpawnPosition(S05PacketSpawnPosition packet, CallbackInfo ci) {
        if (NullWorldGuard.worldMissing(this)) {
            NullWorldGuard.defer(packet);
            ci.cancel();
        }
    }
}
