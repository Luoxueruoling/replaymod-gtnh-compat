package com.gtnh.replaycompat;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

/**
 * Restores the ASM transformers that ReplayMod's own {@code com.replaymod.core.LoadingPlugin}
 * registers, without repeating the mistake that made the stock jar unusable on GTNH.
 *
 * <p>Background (see the workspace HANDOVER, sections 2.1 and 6.1): ReplayMod's coremod both
 * registers transformers <em>and</em> calls {@code MixinBootstrap.init()} /
 * {@code Mixins.addConfiguration(...)} from its constructor. Doing the latter before GTNHMixins'
 * {@code LateMixinOrchestrationMixin} has done its own orchestration reorders mixin application on
 * {@code ItemRenderer}, which makes Angelica rewrite {@code renderOverlays} first and Galacticraft's
 * required injection fail with {@code InjectionError: ... Scanned 0 target(s)}. The fix at the time
 * was to drop the coremod entirely -- which also silently dropped the transformers:
 *
 * <pre>
 *   LoadingPlugin#getASMTransformerClass() -&gt; {
 *       "com.replaymod.core.asm.GLErrorTransformer",
 *       "com.replaymod.core.asm.GLStateTrackerTransformer"
 *   }
 * </pre>
 *
 * <p>This coremod does only the transformer half. It deliberately:
 * <ul>
 *   <li>does <b>not</b> touch {@code MixinBootstrap} or {@code Mixins} in any way, so mixin
 *       orchestration stays entirely with GTNHMixins (the manifest still declares
 *       {@code TweakClass: org.spongepowered.asm.launch.MixinTweaker} for ReplayMod itself);</li>
 *   <li>returns {@code null} for the mod container, setup and access-transformer hooks, so FML treats
 *       it purely as a transformer provider;</li>
 *   <li>keeps {@code injectData} empty -- no early class loading, which is what previously made the
 *       JVM die silently during FML's log reconfiguration;</li>
 *   <li>registers <b>only</b> {@code GLStateTrackerTransformer} by default, exactly like stock
 *       ReplayMod -- see {@link #getASMTransformerClass()} for why {@code GLErrorTransformer} must stay
 *       opt-in.</li>
 * </ul>
 *
 * <p>The transformer class names are returned as strings rather than class literals on purpose: it
 * keeps this class loadable during coremod discovery (when only this jar is guaranteed to be on the
 * classpath) and defers resolving {@code com.replaymod.core.asm.*} to the transformer phase, by which
 * point the ReplayMod jar is present.
 */
@IFMLLoadingPlugin.MCVersion("1.7.10")
public class ReplayModTransformers implements IFMLLoadingPlugin {

    public ReplayModTransformers() {
        // Force load/link the named visitor classes of our transformers NOW -- during coremod
        // discovery, i.e. strictly before FML asks anyone to transform a class. Doing this lazily
        // (on the first transform) is exactly what triggered ClassCircularityError with the stock,
        // anonymous-class transformers.
        int n = com.gtnh.replaycompat.asm.Launcher.preload();
        System.out.println("[replaycompat] preloaded " + n + " transformer/ASM classes");
    }

    /**
     * {@code GLStateTrackerTransformer} is registered by default; {@code GLErrorTransformer} stays opt-in
     * behind {@code -Dreplaymod.glerrors=true} (matching stock ReplayMod -- it is a debug assertion that
     * throws on any GL error, far too aggressive for a modpack).
     *
     * <p>History, because this was wrong twice before it was right:
     * <ul>
     *   <li>The <b>stock</b> transformers (the ones living in {@code com.replaymod.core.asm}) are
     *       unconditionally fatal here: their visitors are anonymous classes created inside method
     *       bodies, so the nested class is generated during the first {@code transform()} call -- from
     *       inside {@code LaunchClassLoader.runTransformers()} -- and the JVM dies with
     *       {@code ClassCircularityError} (<code>GLErrorTransformer$1$1</code> /
     *       <code>GLStateTrackerTransformer$2$1</code>). They are never registered.</li>
     *   <li>Our first rewrite fixed that (named static visitors + {@code Launcher.preload()}) but kept
     *       the upstream behaviour of rewriting {@code GL11.glEnable/glDisable} call sites in
     *       <b>every</b> class, which broke two things: (a) {@code GLStateTracker.hook_glEnable} itself
     *       calls {@code GL11.glEnable}, so rewriting it caused infinite self-recursion
     *       ({@code StackOverflowError}); (b) Galacticraft injects at
     *       {@code @At(target = "Lorg/lwjgl/opengl/GL11;glEnable(I)V")}, so rewriting that call site
     *       made its injection find zero targets ({@code Scanned 0 target(s)} -&gt;
     *       {@code NoClassDefFoundError: ItemRenderer}).</li>
     *   <li>Current form restricts the scope to {@code com/replaymod/} + {@code de/johni0702/} only, so
     *       no other mod's bytecode is ever inspected. Verified in-game with transformers enabled:
     *       starts, enters world, zero injection errors, Galacticraft's {@code ItemRendererMixin} applies
     *       fine.</li>
     * </ul>
     */
    @Override
    public String[] getASMTransformerClass() {
        boolean disable = "false".equals(System.getProperty("replaymod.transformers", "true"));
        if (disable) {
            return new String[0];
        }
        if ("true".equals(System.getProperty("replaymod.glerrors", "false"))) {
            return new String[] {
                    "com.gtnh.replaycompat.asm.GLErrorTransformer",
                    "com.gtnh.replaycompat.asm.GLStateTrackerTransformer"
            };
        }
        return new String[] {
                "com.gtnh.replaycompat.asm.GLStateTrackerTransformer"
        };
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        // Intentionally empty: see the class comment.
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
