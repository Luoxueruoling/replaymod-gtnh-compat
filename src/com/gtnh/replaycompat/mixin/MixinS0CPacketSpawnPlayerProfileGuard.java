package com.gtnh.replaycompat.mixin;

import com.gtnh.replaycompat.util.ReplaySpawnProfile;

import net.minecraft.network.PacketBuffer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops ReplayMod's synthetic player-spawn packet from failing to serialise, so a recording actually
 * contains an entry for the recording player (HANDOVER 12.63 section three / 12.64 feedback #3).
 *
 * <p>{@code RecordingEventHandler#spawnRecordingPlayer} calls
 * {@code PacketListener#save(new S0CPacketSpawnPlayer(mc.thePlayer))}, and
 * {@code S0CPacketSpawnPlayer#func_148840_b} (writePacketData) writes
 * {@code field_148955_b.getName()} into the buffer <b>without</b> a null check - unlike the UUID two
 * instructions earlier, which does have one. In this pack the profile name can be null, and then
 * {@code PacketBuffer#func_150785_a} (writeStringToBuffer) throws:
 *
 * <pre>
 * java.lang.NullPointerException: Cannot invoke "String.getBytes(java.nio.charset.Charset)"
 *         because "&lt;parameter1&gt;" is null
 *     at net.minecraft.network.PacketBuffer.func_150785_a(PacketBuffer)
 *     at net.minecraft.network.play.server.S0CPacketSpawnPlayer.func_148840_b
 *     at com.replaymod.recording.packet.PacketListener.getPacketData(PacketListener.java:465)
 *     at com.replaymod.recording.handler.RecordingEventHandler.spawnRecordingPlayer(RecordingEventHandler.java:111)
 * </pre>
 *
 * <p>{@code PacketListener} swallows that exception and prints it, so the packet is simply never stored:
 * the replay ends up with no player of its own (reported as "the player is not rendered" in some
 * dimensions). {@link ReplaySpawnProfile} makes the profile writable at the HEAD of the method, which
 * leaves well-formed packets completely untouched.
 *
 * <p>{@code remap = false} with the runtime (SRG) name on purpose - the mixin has no refmap of its own
 * and this project has no reobf step. Both spellings were read straight off the shipped jars:
 * {@code func_148840_b} = {@code writePacketData}, {@code field_148955_b} = the {@code GameProfile}
 * field (the same name in the MCP jar, so the mapping has no friendly name for it).
 * {@code require = 0} keeps a rename from being fatal, and the payload class
 * {@code net.minecraft.network.PacketBuffer} is one of the few names that are identical before and after
 * remapping.
 */
@Mixin(targets = "net.minecraft.network.play.server.S0CPacketSpawnPlayer")
public abstract class MixinS0CPacketSpawnPlayerProfileGuard {

    @Inject(method = "func_148840_b", at = @At("HEAD"), remap = false, require = 0)
    private void replaycompat$ensureWritableProfile(PacketBuffer buffer, CallbackInfo ci) {
        ReplaySpawnProfile.ensureWritable(this);
    }
}
