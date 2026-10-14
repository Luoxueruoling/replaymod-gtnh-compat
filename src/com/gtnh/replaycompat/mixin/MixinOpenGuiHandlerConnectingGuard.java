package com.gtnh.replaycompat.mixin;

import com.gtnh.replaycompat.util.NullWorldGuard;

import cpw.mods.fml.common.network.internal.FMLMessage;
import io.netty.channel.ChannelHandlerContext;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drops an "open this GUI" packet that arrives before the client world exists (HANDOVER 12.66, v89).
 *
 * <h3>The crash this prevents</h3>
 *
 * <p>Tester log (v86, 2026-10-13 13:13:30) - the player was still on the <b>connecting</b> screen when a
 * Focal Manipulator GUI packet arrived:
 *
 * <pre>
 * GuiConnecting.updateScreen
 *   -&gt; NetworkManager.processReceivedPackets -&gt; FMLProxyPacket
 *     -&gt; OpenGuiHandler.channelRead0
 *       -&gt; NetworkRegistry.getLocalGuiContainer(:265)
 *         -&gt; ClientProxy.getClientGuiElement(:383) -&gt; new GuiFocalManipulator
 *           -&gt; new ContainerFocalManipulator(:36) -&gt; Salis Arcana's attachPlayer(:526)
 *             -&gt; tileEntity.salisArcana$connectPlayer(player)   &lt;- tileEntity not assigned yet
 * java.lang.NullPointerException ... because "tileEntity" is null
 * </pre>
 *
 * <p>FML reports that NPE from two of its handlers ("OpenGuiHandler exception" and
 * "HandshakeCompletionHandler exception", identical stacks) and treats it as a fatal handshake error, so
 * the connection is terminated - the tester saw "A fatal error has occurred during the network
 * handshake". <b>Neither the recording nor this adapter has anything to do with it</b>: any OpenGui
 * packet that arrives too early hits the missing null check in Salis Arcana's container patch.
 *
 * <h3>Why this hook is safe</h3>
 *
 * <p>Both parameter types ({@code io.netty.channel.ChannelHandlerContext},
 * {@code cpw.mods.fml.common.network.internal.FMLMessage$OpenGui}) are non-obfuscated library classes, so
 * unlike {@code NetworkRegistry.getLocalGuiContainer} - whose Minecraft parameters are obfuscated
 * ({@code yz}, {@code ahb}) and therefore cannot be named in source - this target is writable directly.
 *
 * <p>Cancelling while {@code Minecraft.theWorld == null} only suppresses a GUI that could not be built
 * correctly anyway; the player is still on the connecting/loading screen. Once the world exists nothing
 * changes.
 */
@Mixin(targets = "cpw.mods.fml.common.network.internal.OpenGuiHandler")
public abstract class MixinOpenGuiHandlerConnectingGuard {

    @Inject(method = "channelRead0", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void replaycompat$skipGuiBeforeWorld(ChannelHandlerContext ctx, FMLMessage.OpenGui msg,
            CallbackInfo ci) {
        try {
            if (NullWorldGuard.clientWorldMissing()) {
                NullWorldGuard.logOnce("join/connect: dropped an OpenGui packet that arrived before the"
                        + " client world existed - this is the packet that made Thaumcraft's Focal"
                        + " Manipulator container crash the handshake (HANDOVER 12.66)");
                ci.cancel();
            }
        } catch (Throwable ignored) {
            // never interfere with a normal GUI open
        }
    }
}
