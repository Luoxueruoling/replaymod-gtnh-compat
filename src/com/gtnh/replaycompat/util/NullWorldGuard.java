package com.gtnh.replaycompat.util;

import net.minecraft.network.Packet;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared state and reflection helpers for the "replay is loading, the client world does not exist yet"
 * guards.
 *
 * <p>Not a mixin itself (it is deliberately absent from {@code mixins.replaycompat.json}), it just
 * lives in the mixin package so that {@code inject-replaymod-clientmixins.ps1} ships it alongside.
 *
 * <h3>Why this exists</h3>
 *
 * <p>While {@code GuiOpeningReplay} drives the replay's packet stream, {@code NetHandlerPlayClient}
 * holds two different world references that are <b>not</b> set at the same time:
 *
 * <ul>
 *   <li>{@code clientWorldController} ({@code field_147300_g}) - created by {@code S01PacketJoinGame},
 *       so it exists for most of the loading phase;</li>
 *   <li>{@code Minecraft.theWorld} ({@code field_71441_e}) - only assigned once loading finishes.</li>
 * </ul>
 *
 * <p>Exactly 15 handlers in 1.7.10 dereference {@code theWorld} directly, with no null check; each of
 * them is a guaranteed {@code NullPointerException} if its packet arrives during loading. The one that
 * actually bites GTNH is {@code S25PacketBlockBreakAnim} ({@code func_147294_a}): GTNH servers emit it
 * constantly whenever anything is being mined, so it is present in the first moments of a recording.
 *
 * <h3>Two different treatments</h3>
 *
 * <p>Purely cosmetic/instantaneous packets are dropped; packets that carry state are deferred and
 * re-queued once the world exists, so a replay does not silently lose e.g. its tile entities.
 */
public final class NullWorldGuard {

    private NullWorldGuard() {
    }

    public static final String TAG = "[replaycompat] ";

    /** Packets whose 1.7.10 handler dereferences {@code Minecraft.theWorld} without a null check. */
    public static final Set<String> NEEDS_WORLD = new HashSet<String>(Arrays.asList(
            "net.minecraft.network.play.server.S03PacketTimeUpdate",
            "net.minecraft.network.play.server.S05PacketSpawnPosition",
            "net.minecraft.network.play.server.S07PacketRespawn",
            "net.minecraft.network.play.server.S0BPacketAnimation",
            "net.minecraft.network.play.server.S0DPacketCollectItem",
            "net.minecraft.network.play.server.S0FPacketSpawnMob",
            "net.minecraft.network.play.server.S24PacketBlockAction",
            "net.minecraft.network.play.server.S25PacketBlockBreakAnim",
            "net.minecraft.network.play.server.S27PacketExplosion",
            "net.minecraft.network.play.server.S28PacketEffect",
            "net.minecraft.network.play.server.S29PacketSoundEffect",
            "net.minecraft.network.play.server.S33PacketUpdateSign",
            "net.minecraft.network.play.server.S34PacketMaps",
            "net.minecraft.network.play.server.S35PacketUpdateTileEntity"
    ));

    /**
     * The subset that is safe to simply throw away: transient visuals and sounds which the server
     * repeats anyway, and which carry no state a replay needs.
     */
    public static final Set<String> DROP_WHEN_WORLD_MISSING = new HashSet<String>(Arrays.asList(
            "net.minecraft.network.play.server.S25PacketBlockBreakAnim",
            "net.minecraft.network.play.server.S24PacketBlockAction",
            "net.minecraft.network.play.server.S28PacketEffect",
            "net.minecraft.network.play.server.S27PacketExplosion",
            "net.minecraft.network.play.server.S29PacketSoundEffect",
            "net.minecraft.network.play.server.S0BPacketAnimation",
            "net.minecraft.network.play.server.S0DPacketCollectItem"
    ));

    /** Packets held back until a world exists. Bounded so a broken state cannot grow without limit. */
    private static final Deque<Packet> DEFERRED = new ArrayDeque<Packet>();
    private static final int DEFERRED_LIMIT = 4000;

    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();

    // ---------------------------------------------------------------- classification

    public static boolean needsWorld(Packet packet) {
        return packet != null && NEEDS_WORLD.contains(packet.getClass().getName());
    }

    public static boolean isTransient(Packet packet) {
        return packet != null && DROP_WHEN_WORLD_MISSING.contains(packet.getClass().getName());
    }

    /** Logs each distinct message once, so a long loading phase cannot flood the log. */
    public static void logOnce(String message) {
        try {
            if (LOGGED.add(message)) {
                System.out.println(TAG + message);
            }
        } catch (Throwable ignored) {
            // logging must never be the thing that breaks
        }
    }

    // ---------------------------------------------------------------- deferred packets

    public static void defer(Packet packet) {
        if (DEFERRED.size() < DEFERRED_LIMIT) {
            DEFERRED.add(packet);
        } else {
            logOnce("deferred packet queue is full, dropping " + packet.getClass().getName());
        }
    }

    public static boolean hasDeferred() {
        return !DEFERRED.isEmpty();
    }

    /**
     * Moves every deferred packet back onto the connection's receive queue, so the normal
     * {@code processReceivedPackets} loop dispatches them with the world now in place.
     *
     * @return how many packets were re-queued (0 when the queue could not be reached)
     */
    @SuppressWarnings("unchecked")
    public static int requeueDeferred(Object networkManager) {
        try {
            Queue<Packet> queue = (Queue<Packet>) readField(networkManager, "receivedPackets", "field_150748_i");
            if (queue == null || DEFERRED.isEmpty()) {
                return 0;
            }
            int n = 0;
            while (!DEFERRED.isEmpty()) {
                queue.add(DEFERRED.poll());
                n++;
            }
            return n;
        } catch (Throwable t) {
            return 0;
        }
    }

    // ---------------------------------------------------------------- world lookup

    /**
     * {@code Minecraft.theWorld} as seen from a {@code NetHandlerPlayClient}.
     *
     * @throws Exception when the fields cannot be resolved (callers then leave the packet alone)
     */
    public static Object worldOfHandler(Object handler) throws Exception {
        Object minecraft = readField(handler, "gameController", "field_147299_f");
        return readField(minecraft, "theWorld", "field_71441_e");
    }

    /**
     * {@code true} when {@code Minecraft.theWorld} is still absent, i.e. the packet must not be handed
     * to its (unchecked) handler.
     *
     * <p>Any reflection failure reports {@code false}: a guard that cannot tell must never change
     * vanilla behaviour.
     */
    public static boolean worldMissing(Object handler) {
        try {
            if (worldOfHandler(handler) == null) {
                logOnce("replay loading: Minecraft.theWorld is still null, guarding world-dependent packets");
                return true;
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Same, but starting from a {@code NetworkManager} (via its packet listener). */
    public static Object worldOfNetworkManager(Object networkManager) throws Exception {
        Object handler = readField(networkManager, "packetListener", "field_150744_m");
        if (handler == null) {
            return null;
        }
        return worldOfHandler(handler);
    }

    /**
     * True while the client has no world yet - i.e. it is still connecting or loading (HANDOVER 12.66).
     *
     * <p>Used by {@code MixinOpenGuiHandlerConnectingGuard} to drop an {@code OpenGui} packet that arrives
     * before the world exists: Thaumcraft's {@code ContainerFocalManipulator} resolves its tile entity
     * from the world, and the Salis Arcana patch dereferences it without a null check, which turns into a
     * fatal handshake error.
     *
     * <p>Reflective for the usual reason (no reobf, pit 29): the caller is a mixin into a Forge class and
     * must not reference Minecraft members directly. A failure to resolve reports {@code false} when a
     * Minecraft instance exists - a guard that cannot tell must not change behaviour.
     */
    public static boolean clientWorldMissing() {
        try {
            Class<?> minecraft = Class.forName("net.minecraft.client.Minecraft");
            Method get = null;
            for (String name : new String[]{"getMinecraft", "func_71410_x"}) {
                try {
                    get = minecraft.getMethod(name);
                    break;
                } catch (NoSuchMethodException ignored) {
                    // try the next spelling
                }
            }
            Object instance = get == null ? null : get.invoke(null);
            if (instance == null) {
                return true; // still starting up: nothing can be opened yet
            }
            return readField(instance, "theWorld", "field_71441_e") == null;
        } catch (Throwable t) {
            return false;
        }
    }

    // ---------------------------------------------------------------- reflection

    /** Reads the first field that resolves, trying MCP and SRG spellings at every superclass. */
    public static Object readField(Object target, String... names) throws Exception {
        if (target == null) {
            throw new NullPointerException("cannot read " + Arrays.toString(names) + " from null");
        }
        return findField(target.getClass(), names).get(target);
    }

    public static Field findField(Class<?> type, String... names) throws NoSuchFieldException {
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
        throw new NoSuchFieldException(Arrays.toString(names) + " on " + type.getName());
    }
}
