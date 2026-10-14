package com.gtnh.replaycompat.mixin;

import appeng.api.parts.IPart;
import com.google.common.base.Optional;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops AE2's Waila integration from crashing the game when there is no mouse-over target.
 *
 * <h3>Reported symptom (338-mod environment, 2026-10-11 18:01, log mfORdGF)</h3>
 *
 * <pre>
 * [Client thread/FATAL]: Unreported exception thrown!
 * java.lang.NullPointerException: Cannot read field "field_72307_f" because "mop" is null
 *     at appeng.integration.modules.waila.part.PartAccessor.getMaybePart(PartAccessor.java:43)
 *     at appeng.integration.modules.waila.PartWailaDataProvider.getWailaStack(PartWailaDataProvider.java:98)
 *     at mcp.mobius.waila.overlay.RayTracing.getIdentifierItems(RayTracing.java:152)
 *     at mcp.mobius.waila.overlay.RayTracing.getIdentifierStack(RayTracing.java:88)
 *     at mcp.mobius.waila.overlay.DecoratorRenderer.onRenderWorldLast(DecoratorRenderer.java:28)
 * </pre>
 *
 * The player reported it as "crashed when I clicked the timeline", but the timeline is not the
 * cause: the log shows the local player entity was {@code CameraEntity['CHSheep']}, i.e. this
 * happened <b>inside a replay</b>. Waila runs every frame from {@code RenderWorldLastEvent} and asks
 * the mod that owns the block for a tooltip; with the mouse released to the jGui overlay (which is
 * how you click the timeline at all -- see the Ctrl toggle in
 * {@code MixinGuiReplayOverlayMouseVisible}) the game's {@code objectMouseOver} can be null, and
 * AE2 hands that null straight to {@code PartAccessor}.
 *
 * <h3>Why it is a genuine bug in AE2, not a replay problem</h3>
 *
 * {@code PartAccessor.getMaybePart(TileEntity, MovingObjectPosition)} begins with
 * {@code aload_2; getfield MovingObjectPosition.field_72307_f} -- it dereferences the
 * {@code MovingObjectPosition} without any null check (verified with {@code javap -c} on
 * {@code appliedenergistics2-rv3-beta-1050-GTNH.jar}). The crash is simply the first frame in which
 * that argument is null; a replay with the mouse released just makes that state common.
 *
 * <h3>Fix</h3>
 *
 * Inject at the HEAD of {@code getMaybePart} and answer {@code Optional.absent()} when the
 * {@code MovingObjectPosition} is null. That is a clean degradation, not a behaviour change: both
 * callers check {@code isPresent()} before {@code get()} --
 *
 * <pre>
 *   appeng.integration.modules.waila.PartWailaDataProvider  (isPresent at +31, ifeq on absent)
 *   com.glodblock.github.crossmod.waila.PartWailaDataProvider  (ae2fc; 5 call sites, all guarded)
 * </pre>
 *
 * so the only visible difference is that AE2 part tooltips are skipped while there is no target,
 * instead of the game dying.
 *
 * <p>AE2 is a normal Forge mod and is never remapped by Forge, so the method keeps its source name
 * and the injection uses {@code remap = false}. {@code require = 0} turns a miss into a no-op (and
 * keeps the patch harmless on installs without AE2). This mixin lives in the <b>client</b> section
 * of the config, because Waila's tooltip path is client-only.
 */
@Mixin(targets = "appeng.integration.modules.waila.part.PartAccessor")
public abstract class MixinAe2PartAccessorNullMop {

    @Inject(method = "getMaybePart", at = @At("HEAD"), remap = false, require = 0, cancellable = true)
    private void replaycompat$guardNullMouseOver(TileEntity tileEntity, MovingObjectPosition mop,
                                                 CallbackInfoReturnable<Optional<IPart>> cir) {
        if (mop == null) {
            cir.setReturnValue(Optional.absent());
        }
    }
}
