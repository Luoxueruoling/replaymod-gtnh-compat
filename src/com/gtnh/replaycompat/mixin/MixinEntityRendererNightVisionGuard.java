package com.gtnh.replaycompat.mixin;

import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Stops "spectate a player" in a replay from crashing the game with a NullPointerException.
 *
 * <h3>Symptom (reported from the 338-mod environment, log DU1IrQc / latest (1).log)</h3>
 *
 * <pre>
 * [Client thread/FATAL]: Unreported exception thrown!
 * java.lang.NullPointerException: Cannot invoke "net.minecraft.potion.PotionEffect.func_76459_b()"
 *   because the return value of "net.minecraft.entity.player.EntityPlayer.func_70660_b(Potion)" is null
 *     at net.minecraft.client.renderer.EntityRenderer.func_82830_a(EntityRenderer.java:13580)
 *     at net.minecraft.client.renderer.EntityRenderer.func_78466_h(EntityRenderer.java:1802)
 *     at net.minecraft.client.renderer.EntityRenderer.func_78471_a(EntityRenderer.java:1154)
 *     at net.minecraft.client.renderer.EntityRenderer.func_78480_b(EntityRenderer.java:1015)
 *     at net.minecraft.client.Minecraft.func_71411_J(Minecraft.java:1001)
 * </pre>
 *
 * The user just clicked "spectate" (换视角) on a player entity in the replay.
 *
 * <h3>Root cause - a check/fetch mismatch in {@code updateFogColor} itself</h3>
 *
 * Bytecode of {@code EntityRenderer.func_78466_h} (the fog-colour update, from the RFG merged jar):
 *
 * <pre>
 *   15: astore_3                                  // local 3 = mc.renderViewEntity   (field_71451_h)
 *  918: aload_3 ; isPotionActive(nightVision)    // &lt;- the guard tests the RENDER VIEW ENTITY
 *  925: ifeq 1062                                 //    ("no night vision" -&gt; skip)
 *  933: getfield Minecraft.field_71439_g          // &lt;- but the value is fetched from mc.thePlayer !
 *  937: invokespecial func_82830_a(EntityPlayer,F)
 * </pre>
 *
 * and {@code func_82830_a} dereferences the effect without any null check:
 *
 * <pre>
 *   0: aload_1 ; getstatic Potion.field_76439_r        // nightVision
 *   4: invokevirtual EntityPlayer.func_70660_b(Potion) // getActivePotionEffect
 *   7: invokevirtual PotionEffect.func_76459_b()I      // getDuration  &lt;- NPE when the above is null
 * </pre>
 *
 * So the code is only safe while {@code renderViewEntity == mc.thePlayer}. In ordinary play that is
 * always true - but ReplayMod's "spectate a player" is precisely the feature that points
 * {@code renderViewEntity} at <em>another</em> player (the replayed one) while {@code mc.thePlayer}
 * stays the local player. If the spectated entity has night vision (very common in GTNH: night-vision
 * goggles, potions, IC2 helmets) and the local player does not, the guard passes and the fetch
 * returns null -&gt; crash. That is why this only ever happens in a replay.
 *
 * <h3>What this does</h3>
 *
 * Injects at the head of the private {@code func_82830_a}. When the passed player really has no
 * night-vision effect, it returns {@code 1.0F} instead of dereferencing null.
 * {@code 1.0F} is a value this method itself returns (for a fully active effect) and, unlike 0.0F,
 * it cannot turn a later division into a division by zero. Otherwise the original body runs
 * untouched, so normal play (player does have night vision) behaves exactly as before.
 *
 * <p>{@code func_82830_a} is a Forge-added method that only exists in the MCP/SRG naming; the
 * injection therefore uses the SRG name with {@code remap = false}. Everything else is reflective
 * because this project has no reobf (HANDOVER pit 29) - and every reflective step, logging included,
 * lives inside a {@code try} so a failure degrades to vanilla behaviour instead of a new crash.
 */
@Mixin(targets = "net.minecraft.client.renderer.EntityRenderer")
public abstract class MixinEntityRendererNightVisionGuard {

    private static final String TAG = "[replaycompat] ";

    /** Hard cap on log lines - this runs several times per frame while the bug is being hit. */
    private static final int LOG_LIMIT = 3;

    private static Method getActivePotionEffect;
    private static Object nightVisionPotion;
    private static boolean resolved;
    private static int logCount;

    @Inject(method = "func_82830_a", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void replaycompat$guardNightVisionBrightness(EntityPlayer player, float partialTicks,
                                                         CallbackInfoReturnable<Float> cir) {
        try {
            if (replaycompat$hasNightVision(player)) {
                return; // the original body is what we want in the normal case
            }
            cir.setReturnValue(1.0F);
            replaycompat$log("night-vision guard: renderViewEntity had the effect but the local player "
                    + "does not - returning a neutral brightness instead of crashing");
        } catch (Throwable ignored) {
            // never make things worse: on any failure the original method runs unchanged
        }
    }

    private static boolean replaycompat$hasNightVision(EntityPlayer player) throws Exception {
        replaycompat$resolve(player);
        if (getActivePotionEffect == null || nightVisionPotion == null) {
            return true; // cannot tell - do not interfere
        }
        return getActivePotionEffect.invoke(player, nightVisionPotion) != null;
    }

    private static void replaycompat$resolve(EntityPlayer player) {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            Class<?> potionClass = Class.forName("net.minecraft.potion.Potion");
            Field field = potionClass.getDeclaredField("field_76439_r"); // Potion.nightVision
            field.setAccessible(true);
            nightVisionPotion = field.get(null);

            // getActivePotionEffect(Potion) - public, so getMethod finds it on any EntityPlayer subclass
            getActivePotionEffect = (player == null ? EntityPlayer.class : player.getClass())
                    .getMethod("func_70660_b", potionClass);
        } catch (Throwable t) {
            getActivePotionEffect = null;
            nightVisionPotion = null;
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
