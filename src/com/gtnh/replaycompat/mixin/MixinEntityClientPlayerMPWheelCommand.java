package com.gtnh.replaycompat.mixin;

import com.gtnh.replaycompat.util.ReplayMouseState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets the player tune the adapter from inside the game (HANDOVER 12.64, v87).
 *
 * <h3>Why this hook</h3>
 *
 * <p>{@code EntityClientPlayerMP#sendChatMessage} ({@code func_71165_d}) is the single funnel every chat
 * line passes through - typed commands included. Cancelling it at HEAD keeps our command <b>entirely
 * client-side</b>: nothing is sent to the server, so it works in singleplayer as well as on any server,
 * and it can never collide with a server-side command of the same name.
 *
 * <p>The method name was verified against this project's own SRG jar
 * ({@code build/rfg/srg_merged_minecraft.jar}) by positional comparison with the MCP-named recompiled
 * jar - <b>the {@code methods.csv} table on this machine disagrees with the project's mappings</b>, so it
 * must never be trusted alone (see the pit list entry added in 12.64).
 *
 * <p>Also note that {@code @Inject} with {@code cancellable = true} is the only thing done here; all
 * parsing and every reflective access live in {@link ReplayMouseState#handleChatCommand(String)}, so this
 * class touches no Minecraft member itself (pit 29).
 */
@Mixin(targets = "net.minecraft.client.entity.EntityClientPlayerMP")
public abstract class MixinEntityClientPlayerMPWheelCommand {

    @Inject(method = "func_71165_d", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void replaycompat$chatCommand(String message, CallbackInfo ci) {
        try {
            if (ReplayMouseState.handleChatCommand(message)) {
                ci.cancel(); // handled locally: never send it on
            }
        } catch (Throwable ignored) {
            // never break chat
        }
    }
}
