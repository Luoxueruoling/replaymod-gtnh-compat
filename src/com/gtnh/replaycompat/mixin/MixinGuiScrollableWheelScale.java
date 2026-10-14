package com.gtnh.replaycompat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Makes the mouse wheel usable in ReplayMod's jGui screens (most visibly the replay viewer's
 * recording list) on GTNH, where the client runs on lwjgl3ify instead of plain LWJGL 2.
 *
 * <p><b>Symptom.</b> Scrolling the replay list barely moves it: one wheel notch shifts the list by
 * about one pixel, so the user has to spam the wheel. Reported by 洛洛 as
 * "回放中心滚轮滚动不流畅" / "幅度太小/几乎不动".
 *
 * <p><b>Root cause.</b> jGui's {@code AbstractGuiScrollable.scroll} converts the raw wheel delta like
 * this:
 *
 * <pre>
 * dWheel = (int) Math.copySign(Math.ceil(Math.abs(dWheel) / 4.0), dWheel);
 * scrollY(dWheel);
 * </pre>
 *
 * That {@code / 4} is calibrated for LWJGL 2 / Windows semantics, where one wheel notch is
 * {@code WHEEL_DELTA = 120} and therefore moves {@code 120 / 4 = 30} GUI pixels - roughly one list
 * row. lwjgl3ify feeds the same {@code Mouse.getEventDWheel()} from SDL, where one wheel notch is
 * {@code 1}, so the expression floors to {@code ceil(1 / 4) = 1} pixel per notch.
 *
 * <p><b>Fix.</b> Normalise the value back to LWJGL 2 units before jGui's own arithmetic runs, leaving
 * jGui's scrolling behaviour (including its {@code / 4} and its clamping) untouched. Values that are
 * already in LWJGL 2 units are multiples of 120, so anything with a magnitude of 3 or less must have
 * come from SDL.
 *
 * <p><b>Why this is a plain {@code @ModifyVariable} and needs no reflection.</b> The target is jGui,
 * which ReplayMod relocates into its own jar and which is <em>not</em> remapped by Forge, so the
 * method name {@code scroll} is the real runtime name. This patch never touches a member of a
 * Minecraft class, so the "no reobf / MCP names do not exist at runtime" hazard (HANDOVER pit 29)
 * does not apply here.
 */
@Mixin(targets = "com.replaymod.lib.de.johni0702.minecraft.gui.container.AbstractGuiScrollable")
public abstract class MixinGuiScrollableWheelScale {

    /** Wheel notches, in LWJGL 2 units, that one SDL notch is worth. */
    private static final int LWJGL2_WHEEL_DELTA = 120;

    /** Values larger than this can only have come from LWJGL 2 / WHEEL_DELTA already. */
    private static final int SDL_MAX_MAGNITUDE = 3;

    /** Diagnostics: log only the first few deltas so we can confirm the raw value without spamming. */
    private static int replaycompat$loggedDeltas = 0;

    @ModifyVariable(method = "scroll", at = @At("HEAD"), argsOnly = true, ordinal = 0,
            remap = false, require = 0)
    private int replaycompat$normalizeWheelDelta(int dWheel) {
        try {
            if (replaycompat$loggedDeltas < 8 && dWheel != 0) {
                replaycompat$loggedDeltas++;
                System.out.println("[replaycompat] gui scroll raw dWheel=" + dWheel);
            }
        } catch (Throwable ignored) {
            // Logging must never be able to break scrolling.
        }

        if (dWheel == 0 || Math.abs(dWheel) > SDL_MAX_MAGNITUDE) {
            return dWheel;
        }
        return dWheel * LWJGL2_WHEEL_DELTA;
    }
}
