package com.gtnh.replaycompat.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Makes ReplayMod's synthetic "spawn the recording player" packet writable again.
 *
 * <h3>Symptom (field logs)</h3>
 *
 * <pre>
 * [com.replaymod.recording.packet.PacketListener/]: Writing packet:
 * java.lang.NullPointerException: Cannot invoke "String.getBytes(java.nio.charset.Charset)"
 *         because "&lt;parameter1&gt;" is null
 *     at net.minecraft.network.PacketBuffer.func_150785_a(PacketBuffer)          (writeString)
 *     at net.minecraft.network.play.server.S0CPacketSpawnPlayer.func_148840_b    (writePacketData)
 *     at com.replaymod.recording.packet.PacketListener.getPacketData(PacketListener.java:465)
 *     at com.replaymod.recording.handler.RecordingEventHandler.spawnRecordingPlayer(RecordingEventHandler.java:111)
 *     at net.minecraft.client.network.NetHandlerPlayClient.handler$zbp000$replaymod$recordOwnJoin
 *     at net.minecraft.network.play.server.S01PacketJoinGame.func_148833_a
 * </pre>
 *
 * <p>{@code PacketListener} swallows that exception and prints it, so the packet is simply never
 * stored: <b>the recording ends up with no entry for the recording player</b>, and a replay shows
 * nobody until something makes the client spawn that player again - which is exactly the reported
 * "一开始看不到人，但是出入维度后正常" (a dimension change re-triggers a join, and therefore another
 * synthetic spawn packet).
 *
 * <h3>Which string is actually null (two wrong guesses before this one - see HANDOVER 12.71)</h3>
 *
 * <p>{@code writePacketData} has <b>five</b> {@code writeStringToBuffer} call sites, verified with
 * {@code javap -c}:
 *
 * <pre>
 * offset  30   UUID                    (has a null fallback: id == null ? "" : id.toString())
 * offset  41   GameProfile#getName()
 * offset 100   Property#getName()
 * offset 109   Property#getValue()
 * offset 118   Property#getSignature()
 * </pre>
 *
 * <p>HANDOVER 12.63 - and the first version of this class (v90) - assumed the second one. Field
 * evidence says otherwise: <b>every spawn packet that actually made it into a recording has
 * {@code properties = 0}</b>, while the failing ones happen right after CustomSkinLoader finishes
 * loading the local profile (and therefore attaches a {@code textures} property). The read side reads
 * all three property strings unconditionally ({@code readStringFromBuffer} x3, then
 * {@code new Property(name, value, signature)}), so writing an <b>empty string</b> where a property
 * field is null is both legal and lossless for replays.
 *
 * <h3>What this does</h3>
 *
 * <p>Called from the HEAD of {@code func_148840_b} (see
 * {@code MixinS0CPacketSpawnPlayerProfileGuard}). If the profile is null, its name is null/empty, or
 * any of its properties has a null name/value/signature, the packet gets a <b>sanitised
 * replacement</b> profile: same UUID, a usable name, and the same properties with every null string
 * replaced by {@code ""}. A property whose <em>name</em> is null cannot be represented in the packet
 * at all and is dropped - only that property, never the spawn entry itself. Packets that are already
 * writable are left completely untouched.
 *
 * <p>It also prints one line when it repairs something (naming the exact null field) and one line the
 * first time it sees a writable profile that actually carries properties, so the next field round can
 * confirm or falsify the diagnosis from a log instead of from another guess.
 *
 * <p>Everything reflective on purpose: plain javac, no reobf, so no MCP member name would survive to
 * runtime (HANDOVER pit 29). The field is spelled {@code field_148955_b} in <b>both</b> the MCP and
 * the SRG jar, and {@code func_148840_b} is the name the crash stack itself reports.
 */
public final class ReplaySpawnProfile {

    private ReplaySpawnProfile() {
    }

    private static final String TAG = "[replaycompat] ";

    /** Last resort so the packet can always be written. */
    private static final String DEFAULT_NAME = "Player";

    private static boolean resolved;
    private static boolean unavailable;

    private static Field fProfile;
    private static Field fThePlayer;
    private static Method mGetMinecraft;
    private static Method mGetUniqueID;
    private static Method mProfileGetName;
    private static Method mPlayerGetName;
    private static Method mGetProperties;
    private static Method mPropertyValues;
    private static Method mPropGetName;
    private static Method mPropGetValue;
    private static Method mPropGetSignature;
    private static Constructor<?> mPropCtor;
    private static Method mPut;

    private static boolean loggedFix;
    private static boolean loggedHealthy;

    /**
     * Ensures {@code S0CPacketSpawnPlayer#writePacketData} can serialise this packet.
     *
     * @param packet the {@code S0CPacketSpawnPlayer} instance (passed as {@code Object} so the mixin
     *               body stays free of foreign member names)
     */
    public static void ensureWritable(Object packet) {
        try {
            if (unavailable || packet == null) {
                return;
            }
            if (!resolve()) {
                return;
            }
            Object profile = fProfile.get(packet);
            String name = nameOf(profile);
            List<Object> properties = propertiesOf(profile);

            String bad = firstBadPropertyField(properties);
            boolean badName = name == null || name.isEmpty();
            if (profile != null && !badName && bad == null) {
                if (!loggedHealthy && !properties.isEmpty()) {
                    loggedHealthy = true;
                    System.out.println(TAG + "spawn packet: profile is writable as-is (name='" + name
                            + "', properties=" + properties.size() + ") - nothing to repair");
                }
                return; // already writable: never touch it
            }

            String useName = badName ? playerName() : name;
            Object fixed = newProfile(idOf(profile), useName);
            Object dstProps = mGetProperties.invoke(fixed);

            int kept = 0;
            int sanitised = 0;
            String detail = bad == null ? "" : bad;
            for (Object property : properties) {
                String pn = str(mPropGetName.invoke(property));
                if (pn == null) {
                    continue; // a property with no name cannot be represented in the packet at all
                }
                String pv = str(mPropGetValue.invoke(property));
                String ps = str(mPropGetSignature.invoke(property));
                if (pv == null || ps == null) {
                    sanitised++;
                    if (detail.isEmpty()) {
                        detail = "property '" + pn + "' had a null "
                                + (pv == null ? "value" : "signature");
                    }
                }
                mPut.invoke(dstProps, pn, mPropCtor.newInstance(pn, pv == null ? "" : pv,
                        ps == null ? "" : ps));
                kept++;
            }
            fProfile.set(packet, fixed);

            if (!loggedFix) {
                loggedFix = true;
                System.out.println(TAG + "spawn packet: the player profile could not be written"
                        + " (name=" + (badName ? "missing" : "'" + useName + "'")
                        + (detail.isEmpty() ? "" : ", " + detail)
                        + ") - wrote a sanitised copy (properties=" + kept
                        + (sanitised > 0 ? ", " + sanitised + " with empty strings" : "")
                        + ") so the recording can store the player's spawn entry");
            }
        } catch (Throwable ignored) {
            // This only ever turns a guaranteed write failure into a successful one; never make it worse.
        }
    }

    // ------------------------------------------------------------------ reflection helpers

    private static String str(Object value) {
        return value instanceof String ? (String) value : null;
    }

    private static String nameOf(Object profile) throws Exception {
        return profile == null ? null : str(mProfileGetName.invoke(profile));
    }

    /** All {@code Property} objects of the profile, or an empty list. */
    private static List<Object> propertiesOf(Object profile) throws Exception {
        List<Object> out = new ArrayList<Object>();
        if (profile == null) {
            return out;
        }
        Object map = mGetProperties.invoke(profile);
        if (map == null) {
            return out;
        }
        Object values = mPropertyValues.invoke(map);
        if (values instanceof Collection) {
            for (Object value : (Collection<?>) values) {
                out.add(value);
            }
        }
        return out;
    }

    /** Describes the first null property field, or null when every property is writable. */
    private static String firstBadPropertyField(List<Object> properties) throws Exception {
        for (Object property : properties) {
            String pn = str(mPropGetName.invoke(property));
            if (pn == null) {
                return "a property has a null name";
            }
            if (str(mPropGetValue.invoke(property)) == null) {
                return "property '" + pn + "' has a null value";
            }
            if (str(mPropGetSignature.invoke(property)) == null) {
                return "property '" + pn + "' has a null signature";
            }
        }
        return null;
    }

    private static UUID idOf(Object profile) throws Exception {
        if (profile != null) {
            Object id = profile.getClass().getMethod("getId").invoke(profile);
            if (id instanceof UUID) {
                return (UUID) id;
            }
        }
        return playerUuid();
    }

    /** The local player's own name, used when the profile lost it. */
    private static String playerName() {
        try {
            Object player = thePlayer();
            if (player != null) {
                Object name = mPlayerGetName.invoke(player);
                if (name instanceof String && !((String) name).isEmpty()) {
                    return (String) name;
                }
            }
        } catch (Throwable ignored) {
            // fall through to the literal
        }
        return DEFAULT_NAME;
    }

    private static UUID playerUuid() {
        try {
            Object player = thePlayer();
            if (player != null) {
                Object id = mGetUniqueID.invoke(player);
                if (id instanceof UUID) {
                    return (UUID) id;
                }
            }
        } catch (Throwable ignored) {
            // fall through
        }
        return new UUID(0L, 0L);
    }

    private static Object thePlayer() throws Exception {
        Object mc = mGetMinecraft.invoke(null);
        return mc == null ? null : fThePlayer.get(mc);
    }

    private static Object newProfile(UUID id, String name) throws Exception {
        Class<?> gameProfile = Class.forName("com.mojang.authlib.GameProfile");
        return gameProfile.getConstructor(UUID.class, String.class).newInstance(id, name);
    }

    private static boolean resolve() {
        if (resolved) {
            return !unavailable;
        }
        resolved = true;
        try {
            Class<?> spawnPacket = Class.forName("net.minecraft.network.play.server.S0CPacketSpawnPlayer");
            fProfile = spawnPacket.getDeclaredField("field_148955_b");
            fProfile.setAccessible(true);

            Class<?> profileClass = Class.forName("com.mojang.authlib.GameProfile");
            mProfileGetName = profileClass.getMethod("getName");
            mGetProperties = profileClass.getMethod("getProperties");
            mGetUniqueID = findMethod(Class.forName("net.minecraft.entity.Entity"),
                    "getUniqueID", "func_110124_au");
            mPlayerGetName = findMethod(Class.forName("net.minecraft.entity.player.EntityPlayer"),
                    "getName", "func_70005_c_");

            Class<?> propertyMap = Class.forName("com.mojang.authlib.properties.PropertyMap");
            mPropertyValues = propertyMap.getMethod("values");
            mPut = propertyMap.getMethod("put", Object.class, Object.class);

            Class<?> property = Class.forName("com.mojang.authlib.properties.Property");
            mPropGetName = property.getMethod("getName");
            mPropGetValue = property.getMethod("getValue");
            mPropGetSignature = property.getMethod("getSignature");
            mPropCtor = property.getConstructor(String.class, String.class, String.class);

            Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
            for (String spelling : new String[] { "getMinecraft", "func_71410_x" }) {
                try {
                    mGetMinecraft = mcClass.getMethod(spelling);
                    break;
                } catch (NoSuchMethodException ignored) {
                    // try the next spelling
                }
            }
            fThePlayer = findField(mcClass, "thePlayer", "field_71439_g");

            if (mGetMinecraft == null || fThePlayer == null || mGetUniqueID == null
                    || mPlayerGetName == null) {
                throw new NoSuchMethodException("Minecraft.thePlayer / getMinecraft / getUniqueID");
            }
        } catch (Throwable t) {
            unavailable = true;
            System.out.println(TAG + "spawn profile guard unavailable: " + t);
            return false;
        }
        return true;
    }

    private static Method findMethod(Class<?> type, String... names) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (String name : names) {
                try {
                    Method m = c.getDeclaredMethod(name);
                    m.setAccessible(true);
                    return m;
                } catch (NoSuchMethodException ignored) {
                    // next spelling / superclass
                } catch (Throwable ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private static Field findField(Class<?> type, String... names) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (String name : names) {
                try {
                    Field f = c.getDeclaredField(name);
                    f.setAccessible(true);
                    return f;
                } catch (NoSuchFieldException ignored) {
                    // next spelling / superclass
                } catch (Throwable ignored) {
                    return null;
                }
            }
        }
        return null;
    }
}
