package com.gtnh.replaycompat.mixin;

import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.filter.SquashFilter;
import com.replaymod.replaystudio.stream.PacketStream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Last line of defence for the replay editor: one unreadable packet must not kill the game
 * (HANDOVER 12.61, from the tester's 2026-10-12 crash report "Running marker processor").
 *
 * <h3>Why this exists on top of {@link MixinSquashFilterSafeBulkRead}</h3>
 *
 * <p>That mixin guards the three packet reads <b>we know about</b> inside
 * {@code SquashFilter#onPacket}. The v84 crash proved the list was incomplete once already - the
 * third entry point ({@code PacketChunkData.read}) only surfaced when a tester's recording contained
 * a bad {@code CHUNK_DATA} packet, and every read site that gets added upstream is a new hole.
 *
 * <p>{@code MarkerProcessor.apply} is where the editor feeds packets to the squash filter
 * ({@code GuiEditReplay}'s "apply markers" button, crash report section "Running marker processor") and
 * it calls {@code SquashFilter.onPacket} twice, in a loop over the whole recording. Wrapping those
 * calls costs one redirect and removes the whole class of "the editor dies on a corrupt recording"
 * failures, whatever the internal read path looks like.
 *
 * <h3>Semantics</h3>
 *
 * <p>On failure the packet is dropped and the loop continues; the returned {@code boolean} is discarded
 * by the caller ({@code pop} right after the call), so returning {@code false} changes nothing.
 * {@code require = 0} and a reflective-free body: ReplayStudio classes keep their real names, so this
 * is compiled against the jar directly like the other ReplayStudio patches.
 */
@Mixin(targets = "com.replaymod.editor.gui.MarkerProcessor")
public abstract class MixinMarkerProcessorSafeFiltering {

    private static final String TAG = "[replaycompat] ";
    private static int logged;

    @Redirect(method = "apply",
            at = @At(value = "INVOKE",
                    target = "Lcom/replaymod/replaystudio/filter/SquashFilter;onPacket(Lcom/replaymod/replaystudio/stream/PacketStream;Lcom/replaymod/replaystudio/PacketData;)Z"),
            remap = false, require = 0)
    private static boolean replaycompat$safeOnPacket(SquashFilter filter, PacketStream stream, PacketData data) {
        try {
            return filter.onPacket(stream, data);
        } catch (Throwable t) {
            replaycompat$log(t);
            return false;
        }
    }

    private static void replaycompat$log(Throwable t) {
        try {
            if (logged >= 5) {
                return;
            }
            logged++;
            System.out.println(TAG + "replay editing: skipped a packet the marker processor could not"
                    + " read instead of crashing the game (" + t + ")");
        } catch (Throwable ignored) {
            // logging must never escape
        }
    }
}
