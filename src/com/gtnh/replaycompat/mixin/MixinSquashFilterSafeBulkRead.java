package com.gtnh.replaycompat.mixin;

import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.packets.PacketChunkData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.List;

/**
 * Keeps saving <b>and editing</b> a replay from dying on a malformed chunk packet (HANDOVER 12.47,
 * completed in 12.61 after the tester's v84 crash).
 *
 * <h3>The two crashes this fixes</h3>
 *
 * <pre>
 * Description: Saving replay file                                  (12.47, fixed in v73)
 * java.lang.IndexOutOfBoundsException: readerIndex(8) + length(103086) exceeds writerIndex(27768)
 *     at ...AbstractByteBuf.readBytes(AbstractByteBuf.java:896)
 *     at ...PacketChunkData.readBulkV1_7(PacketChunkData.java:131)
 *     at ...PacketChunkData.readBulk(PacketChunkData.java:92)
 *     at ...SquashFilter.onPacket(SquashFilter.java:370)
 * </pre>
 *
 * <pre>
 * Description: Running marker processor                           (tester log 2026-10-12 16:09, v84)
 * java.lang.IndexOutOfBoundsException: readerIndex(18) + length(44957696) exceeds writerIndex(132)
 *     at ...ByteBufNetInput.readBytes(ByteBufNetInput.java:106)
 *     at ...PacketChunkData.readLoad(PacketChunkData.java:258)
 *     at ...PacketChunkData.read(PacketChunkData.java:58)      &lt;-- third entry point, was unprotected
 *     at ...SquashFilter.onPacket(SquashFilter.java:362)
 *     at ...MarkerProcessor.apply(MarkerProcessor.java:212)
 *     at ...GuiEditReplay.lambda$apply$10(GuiEditReplay.java:158)
 * </pre>
 *
 * <p>In that v84 log the <b>v73 guards did fire five times</b> ("skipped an unreadable chunk data
 * packet", all with {@code readerIndex(8)}) and then the very next bad packet killed the game with
 * {@code readerIndex(18)}. The reason is that {@code SquashFilter#onPacket} reads chunk payloads
 * through three different calls, and {@code PacketChunkData#read(Packet)} - the {@code PacketType.CHUNK_DATA}
 * branch - is not one of the two bulk calls that were redirected:
 *
 * <pre>
 *   offset  964: invokestatic PacketChunkData.read(Packet)     -&gt; PacketChunkData       (new guard)
 *   offset 1015: invokestatic PacketChunkData.readBulk(Packet) -&gt; List&lt;Column&gt;         (v73 guard)
 *                invokestatic PacketBlockChange.readBulk(Packet) -&gt; List&lt;BlockChange&gt;   (v73 guard)
 * </pre>
 *
 * <h3>Fix</h3>
 *
 * <p>All three call sites are redirected so a read failure loses that one packet instead of escaping
 * the thread and taking the game with it - the same trade-off already accepted in 12.26 for the entity
 * tracker and in 12.47 for saving. The return values are chosen so the <b>caller cannot trip over
 * them</b>:
 *
 * <ul>
 *   <li>the two bulk reads are used as {@code readBulk(packet).iterator()}, so they return <b>an empty
 *       list</b> (null would only move the crash to a {@code NullPointerException});</li>
 *   <li>{@code PacketChunkData#read} is used directly - {@code onPacket} immediately calls
 *       {@code isUnload()}, {@code getUnloadX()} and {@code getUnloadZ()} on it - so it returns
 *       {@code PacketChunkData.unload(0, 0)}, upstream's own factory for "this chunk is gone". The bogus
 *       packet therefore degrades into unloading a chunk that was never loaded, and the recording (or
 *       the marker process) continues.</li>
 * </ul>
 *
 * <p>ReplayStudio lives inside the ReplayMod jar and is never remapped, hence {@code remap = false}
 * with the real class/method names; {@code require = 0} keeps this a no-op if the shape changes.
 * Logging is rate limited so a recording full of bad packets cannot flood the log.
 */
@Mixin(targets = "com.replaymod.replaystudio.filter.SquashFilter")
public abstract class MixinSquashFilterSafeBulkRead {

    private static final String TAG = "[replaycompat] ";
    private static int logged;

    /** {@code PacketType.CHUNK_DATA} - the branch that crashed the marker processor in v84 (12.61). */
    @Redirect(method = "onPacket",
            at = @At(value = "INVOKE",
                    target = "Lcom/replaymod/replaystudio/protocol/packets/PacketChunkData;read(Lcom/replaymod/replaystudio/protocol/Packet;)Lcom/replaymod/replaystudio/protocol/packets/PacketChunkData;"),
            remap = false, require = 0)
    private static PacketChunkData replaycompat$safeChunkDataRead(Packet packet) {
        try {
            return PacketChunkData.read(packet);
        } catch (Throwable t) {
            replaycompat$log("chunk data (load)", t);
            // Never null: onPacket calls isUnload()/getUnloadX()/getUnloadZ() on the result at once.
            return PacketChunkData.unload(0, 0);
        }
    }

    @Redirect(method = "onPacket",
            at = @At(value = "INVOKE",
                    target = "Lcom/replaymod/replaystudio/protocol/packets/PacketChunkData;readBulk(Lcom/replaymod/replaystudio/protocol/Packet;)Ljava/util/List;"),
            remap = false, require = 0)
    private static List<?> replaycompat$safeChunkBulkRead(Packet packet) {
        try {
            return PacketChunkData.readBulk(packet);
        } catch (Throwable t) {
            replaycompat$log("chunk data", t);
            return Collections.emptyList();
        }
    }

    @Redirect(method = "onPacket",
            at = @At(value = "INVOKE",
                    target = "Lcom/replaymod/replaystudio/protocol/packets/PacketBlockChange;readBulk(Lcom/replaymod/replaystudio/protocol/Packet;)Ljava/util/List;"),
            remap = false, require = 0)
    private static List<?> replaycompat$safeBlockBulkRead(Packet packet) {
        try {
            return com.replaymod.replaystudio.protocol.packets.PacketBlockChange.readBulk(packet);
        } catch (Throwable t) {
            replaycompat$log("block change", t);
            return Collections.emptyList();
        }
    }

    private static void replaycompat$log(String what, Throwable t) {
        try {
            if (logged >= 5) {
                return;
            }
            logged++;
            System.out.println(TAG + "replay saving: skipped an unreadable " + what
                    + " packet instead of failing the whole save (" + t + ")");
        } catch (Throwable ignored) {
            // logging must never escape
        }
    }
}
