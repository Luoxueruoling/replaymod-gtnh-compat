package com.gtnh.replaycompat.mixin;

import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.util.EntityPositionTracker;
import com.replaymod.replaystudio.util.PacketUtils;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.function.Consumer;

/**
 * Keeps a malformed packet in a replay from turning into a scary Crash Report window.
 *
 * <h3>Symptom (reported from the 338-mod environment, log J7AQ5aF)</h3>
 *
 * <pre>
 * [CHAT] [Replay Mod] Error loading entity tracker: readerIndex(1) + length(4) exceeds writerIndex(4)
 *        PooledUnsafeDirectByteBuf(ridx: 1, widx: 4, cap: 4)
 * + a CrashReport-style window:
 *        Description: Failed to load entity tracker, spectator keyframes will be broken.
 *   java.lang.IndexOutOfBoundsException
 *       at com.replaymod.replaystudio.protocol.packets.EntityId.getEntityId(EntityId.java:106)
 *       at com.replaymod.replaystudio.util.PacketUtils.getEntityId(PacketUtils.java:58)
 *       at com.replaymod.replaystudio.util.EntityPositionTracker.loadFromPacketData(EntityPositionTracker.java:165)
 *       at com.replaymod.replaystudio.util.EntityPositionTracker.load(EntityPositionTracker.java:96)
 *       at com.replaymod.simplepathing.gui.GuiPathing.lambda$startLoadingEntityTracker$6(GuiPathing.java:490)
 * </pre>
 *
 * The game itself does <b>not</b> crash (upstream's own log shows it entering the world right
 * afterwards) - ReplayMod deliberately surfaces the failure with {@code Utils.error(...)}. Only
 * simplepathing's spectator keyframes are lost.
 *
 * <h3>Why the exception escapes</h3>
 *
 * {@code EntityPositionTracker.load} wraps only the "read from cache" section in a
 * {@code try/catch (IOException)}. The call to {@code loadFromPacketData(Consumer)} - the fallback
 * that re-parses the recording - sits <em>outside</em> that guard. A truncated/garbled packet makes
 * {@code PacketUtils.getEntityId} read past the end of a Netty buffer, which throws
 * {@code IndexOutOfBoundsException} - an unchecked exception the author never anticipated.
 *
 * <h3>What this does</h3>
 *
 * Both call sites ({@code load} has two: the normal fallback and the "cache was unusable" path) are
 * redirected through here. {@code loadFromPacketData} is private, so it is invoked reflectively (a
 * plain {@code javac} patch project has no reobf and no {@code @Shadow} - see HANDOVER pit 29/42).
 * Any {@code Throwable} it produces is swallowed, which is exactly the degradation upstream already
 * accepts: whatever could be parsed stays, the rest is missing, and nothing pops up.
 *
 * <p>{@code ReplayStudio} does not take part in Forge remapping, so the injection targets the real
 * runtime names and uses {@code remap = false}.
 */
@Mixin(targets = "com.replaymod.replaystudio.util.EntityPositionTracker")
public abstract class MixinEntityPositionTrackerSafeLoad {

    private static final String TAG = "[replaycompat] ";

    /** Hard cap on how many "skipped" lines may reach the log. */
    private static final int LOG_LIMIT = 5;

    private static Method loadFromPacketData;
    private static Field entityPositionsField;
    private static boolean resolved;
    private static int logCount;

    @Redirect(method = "load",
            at = @At(value = "INVOKE",
                    target = "Lcom/replaymod/replaystudio/util/EntityPositionTracker;loadFromPacketData(Ljava/util/function/Consumer;)V"),
            remap = false, require = 0)
    private void replaycompat$safeLoadFromPacketData(EntityPositionTracker self, Consumer<Double> progress) {
        try {
            replaycompat$resolve();
            if (loadFromPacketData == null) {
                return; // cannot invoke it at all - skipping is still better than an unhandled error
            }
            loadFromPacketData.invoke(self, progress);
        } catch (Throwable t) {
            // Unwrap the reflection wrapper so the log names the real cause.
            Throwable cause = t;
            try {
                if (t instanceof java.lang.reflect.InvocationTargetException && t.getCause() != null) {
                    cause = t.getCause();
                }
            } catch (Throwable ignored) {
                // keep the original throwable
            }
            // Make sure the follow-up saveToCache() in load() cannot NPE on a null map.
            replaycompat$ensurePositionsMap(self);
            replaycompat$log("entity tracker: skipping malformed packet data (" + cause + ")");
        }
    }

    /**
     * ★ The actual fix for the entity tracker: tolerate a malformed packet <em>per packet</em> instead
     * of losing the whole tracker.
     *
     * <p>{@code PacketUtils.getEntityId(Packet)} decides from the packet id whether a packet carries
     * an entity id and then reads it out of the payload. A single replay that contains a packet whose
     * payload is too short for that read makes it throw {@code IndexOutOfBoundsException}
     * (measured: {@code readerIndex(1) + length(4) exceeds writerIndex(4)}, i.e. 4 payload bytes),
     * and because that exception is unchecked it aborts the entire {@code loadFromPacketData} loop -
     * so the tracker ends up empty and simplepathing's <b>spectator keyframes</b> stop working
     * ("Failed to load entity tracker, spectator keyframes will be broken.").
     *
     * <p>The call site is right there in the bytecode and already lenient:
     *
     * <pre>
     *   99: invokestatic PacketUtils.getEntityId(Packet)Ljava/lang/Integer;
     *  106: ifnonnull 118        // a null return means "this packet has no entity id"
     *  109: packet.release(); continue;
     * </pre>
     *
     * So returning {@code null} here drops only the offending packet and lets the loop collect every
     * other entity position - the tracker loads completely, and the spectator keyframes that depend on
     * it work again. {@code MixinEntityPositionTrackerSafeLoad#replaycompat$safeLoadFromPacketData}
     * stays as a second line of defence for any other failure.
     */
    @Redirect(method = "loadFromPacketData",
            at = @At(value = "INVOKE",
                    target = "Lcom/replaymod/replaystudio/util/PacketUtils;getEntityId(Lcom/replaymod/replaystudio/protocol/Packet;)Ljava/lang/Integer;"),
            remap = false, require = 0)
    private static Integer replaycompat$safeGetEntityId(Packet packet) throws IOException {
        try {
            return PacketUtils.getEntityId(packet);
        } catch (Throwable t) {
            replaycompat$log("entity tracker: packet with an unreadable entity id was skipped ("
                    + t + ") - the rest of the tracker still loads");
            return null;
        }
    }

    /** Same tolerance for the position update - its call site also treats a null as "skip". */
    @Redirect(method = "loadFromPacketData",
            at = @At(value = "INVOKE",
                    target = "Lcom/replaymod/replaystudio/util/PacketUtils;updateLocation(Lcom/replaymod/replaystudio/util/Location;Lcom/replaymod/replaystudio/protocol/Packet;)Lcom/replaymod/replaystudio/util/Location;"),
            remap = false, require = 0)
    private static com.replaymod.replaystudio.util.Location replaycompat$safeUpdateLocation(
            com.replaymod.replaystudio.util.Location last, Packet packet) throws IOException {
        try {
            return PacketUtils.updateLocation(last, packet);
        } catch (Throwable t) {
            replaycompat$log("entity tracker: unreadable position for a packet was skipped (" + t + ")");
            return null;
        }
    }

    /** {@code saveToCache()} runs right after the call, so the map must never be left null. */
    private static void replaycompat$ensurePositionsMap(EntityPositionTracker self) {
        try {
            replaycompat$resolve();
            if (entityPositionsField == null) {
                return;
            }
            if (entityPositionsField.get(self) == null) {
                entityPositionsField.set(self, new HashMap<Integer, Object>());
            }
        } catch (Throwable ignored) {
            // best effort only
        }
    }

    private static void replaycompat$resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            Method m = EntityPositionTracker.class.getDeclaredMethod("loadFromPacketData", Consumer.class);
            m.setAccessible(true);
            loadFromPacketData = m;
        } catch (Throwable t) {
            loadFromPacketData = null;
        }
        try {
            Field f = EntityPositionTracker.class.getDeclaredField("entityPositions");
            f.setAccessible(true);
            entityPositionsField = f;
        } catch (Throwable t) {
            entityPositionsField = null;
        }
    }

    private static void replaycompat$log(String message) {
        try {
            if (logCount >= LOG_LIMIT) {
                return;
            }
            logCount++;
            System.out.println(TAG + message);
        } catch (Throwable ignored) {
            // logging must never escape
        }
    }
}
