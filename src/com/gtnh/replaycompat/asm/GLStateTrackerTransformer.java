package com.gtnh.replaycompat.asm;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashSet;
import java.util.Set;

/**
 * Byte-for-byte equivalent of ReplayMod's {@code com.replaymod.core.asm.GLStateTrackerTransformer},
 * rewritten so that it can actually be registered on GTNH + RFB.
 *
 * <p><b>Why a rewrite is required.</b> The stock transformer instantiates its
 * {@code ClassVisitor}/{@code MethodVisitor} as <i>anonymous classes inside method bodies</i>. Those
 * nested classes ({@code $2}, {@code $2$1}) are therefore only generated and loaded the first time
 * {@code transform()} runs -- from inside {@code LaunchClassLoader.runTransformers()}. Recursively
 * asking the class loader that is mid-load to generate a brand new class trips the JVM circularity
 * guard:
 *
 * <pre>
 *   java.lang.ClassCircularityError: com/replaymod/core/asm/GLStateTrackerTransformer$2$1
 *       at GLStateTrackerTransformer.findEligibleMethods(GLStateTrackerTransformer.java:69)
 * </pre>
 *
 * and every subsequent class then fails to transform, killing {@code RFB-Main}.
 *
 * <p><b>The fix.</b> Every visitor here is a <i>named static nested class</i>. Named nested classes
 * are compiled as ordinary {@code .class} files and are eagerly prelinked by
 * {@link Launcher#preload()} (invoked from the coremod's constructor, i.e. <i>before</i> FML ever
 * asks us to transform anything). No class is ever created during a transform.
 *
 * <p>Semantics are kept identical to the original: rewrite
 * {@code GL11.glEnable}/{@code GL11.glDisable} call sites (only in methods that reference them, and
 * never inside {@code GLStateTracker} itself) onto {@code GLStateTracker.hook_glEnable/hook_glDisable}.
 */
public class GLStateTrackerTransformer implements IClassTransformer {

    private static final String GL11_CLASS = "org.lwjgl.opengl.GL11";
    private static final String GL11 = GL11_CLASS.replace('.', '/');
    private static final String GL_ENABLE = "glEnable";
    private static final String GL_DISABLE = "glDisable";
    private static final String STATE_TRACKER_CLASS = "com.replaymod.render.hooks.GLStateTracker";
    private static final String STATE_TRACKER = STATE_TRACKER_CLASS.replace('.', '/');
    private static final String HOOK_ENABLE = "hook_glEnable";
    private static final String HOOK_DISABLE = "hook_glDisable";

    /**
     * Classes this transformer is allowed to touch: <b>only ReplayMod / jGui's own code</b>.
     *
     * <p>This scope restriction is the fix for two separate crashes, both caused by the original
     * "rewrite GL11.glEnable/glDisable call sites in <i>every</i> class" behaviour:
     *
     * <ol>
     *   <li><b>Self-recursion.</b> {@code com.replaymod.render.hooks.GLStateTracker.hook_glEnable(int)}
     *       is implemented as {@code GL11.glEnable(i); getInstance().updateEnabledState(...)} -- it
     *       <i>must</i> call the real {@code glEnable}. Rewriting that call site turns it into
     *       {@code hook_glEnable -> hook_glEnable -> ...} and the game dies with
     *       {@code StackOverflowError at GLStateTracker.hook_glEnable(GLStateTracker.java:34)}.
     *       Skipping the class by name did not prevent this.</li>
     *   <li><b>Killing other mods' mixin anchors.</b> Galacticraft's {@code ItemRendererMixin} injects at
     *       {@code @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glEnable(I)V")}. Rewriting that
     *       call site makes its injection find zero targets:
     *       {@code InjectionError: ... Scanned 0 target(s)} -&gt;
     *       {@code NoClassDefFoundError: net/minecraft/client/renderer/ItemRenderer} -&gt; crash.
     *       (Same class of hazard for Angelica's {@code RenderItem}, DistantHorizons'
     *       {@code EntityRenderer}, Angelica/ArchaicFix's {@code SplashProgress$3}, IC2's
     *       {@code RenderLiquidCell}.)</li>
     * </ol>
     *
     * <p>By restricting to ReplayMod's own packages, both hazards disappear structurally rather than via
     * an ever-growing blacklist: we never look at another mod's bytecode at all. What is given up is
     * state tracking for GL calls made from outside ReplayMod (Minecraft's own renderer, other mods) --
     * acceptable, because the tracker exists to feed ReplayMod's own render pipeline.
     */
    private static final String[] ALLOWED_PREFIXES = new String[] {
            "com/replaymod/",
            "de/johni0702/"
    };

    private static boolean isInScope(String internalName) {
        for (String prefix : ALLOWED_PREFIXES) {
            if (internalName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;

        // LaunchClassLoader hands transformers the transformed (dotted) name in `name` on 1.7.10, but
        // other call sites pass the internal (slashed) form. Normalise before comparing.
        String internalName = name == null ? null
                : (name.indexOf('/') >= 0 ? name : name.replace('.', '/'));
        if (internalName == null) return basicClass;

        // Scope gate: never touch anything outside ReplayMod / jGui. This is what keeps us from
        // rewriting GLStateTracker's own hook bodies (self-recursion -> StackOverflowError) and from
        // destroying other mods' GL11 mixin anchors (Galacticraft's ItemRenderer, Angelica, DH, IC2).
        if (!isInScope(internalName)) {
            return basicClass;
        }

        // Belt and braces: never rewrite the tracker itself, however it is named/relocated.
        if (internalName.equals(STATE_TRACKER_CLASS) || internalName.contains("GLStateTracker")) {
            return basicClass;
        }

        ClassReader reader = new ClassReader(basicClass);
        Set<Method> eligible = new HashSet<Method>();
        reader.accept(new EligibleMethodCollector(eligible), ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        if (eligible.isEmpty()) {
            return basicClass;
        }

        ClassWriter writer = new ClassWriter(reader, 0);
        reader.accept(new RedirectingVisitor(writer, eligible), 0);
        return writer.toByteArray();
    }

    /** Value type used to remember which methods actually touch GL11.glEnable/glDisable. */
    static final class Method {
        final String name;
        final String desc;

        Method(String name, String desc) {
            this.name = name;
            this.desc = desc;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Method other = (Method) o;
            return name.equals(other.name) && desc.equals(other.desc);
        }

        @Override
        public int hashCode() {
            return name.hashCode() ^ desc.hashCode();
        }
    }

    /** Pass 1: collect the methods that call GL11.glEnable / GL11.glDisable. */
    static final class EligibleMethodCollector extends ClassVisitor {
        private final Set<Method> eligible;

        EligibleMethodCollector(Set<Method> eligible) {
            super(Opcodes.ASM5);
            this.eligible = eligible;
        }

        @Override
        public MethodVisitor visitMethod(int access, String methodName, String methodDesc,
                                         String signature, String[] exceptions) {
            return new InsnScanner(methodName, methodDesc, eligible);
        }
    }

    static final class InsnScanner extends MethodVisitor {
        private final String methodName;
        private final String methodDesc;
        private final Set<Method> eligible;

        InsnScanner(String methodName, String methodDesc, Set<Method> eligible) {
            super(Opcodes.ASM5);
            this.methodName = methodName;
            this.methodDesc = methodDesc;
            this.eligible = eligible;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (GL11.equals(owner) && (GL_ENABLE.equals(name) || GL_DISABLE.equals(name))) {
                eligible.add(new Method(methodName, methodDesc));
            }
        }
    }

    /** Pass 2: rewrite the matching call sites onto GLStateTracker hooks. */
    static final class RedirectingVisitor extends ClassVisitor {
        private final Set<Method> eligible;

        RedirectingVisitor(ClassVisitor delegate, Set<Method> eligible) {
            super(Opcodes.ASM5, delegate);
            this.eligible = eligible;
        }

        @Override
        public MethodVisitor visitMethod(int access, String methodName, String methodDesc,
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, methodName, methodDesc, signature, exceptions);
            if (!eligible.contains(new Method(methodName, methodDesc))) {
                return mv;
            }
            return new HookRedirector(mv);
        }
    }

    static final class HookRedirector extends MethodVisitor {
        HookRedirector(MethodVisitor delegate) {
            super(Opcodes.ASM5, delegate);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (GL11.equals(owner)) {
                if (GL_ENABLE.equals(name)) {
                    owner = STATE_TRACKER;
                    name = HOOK_ENABLE;
                } else if (GL_DISABLE.equals(name)) {
                    owner = STATE_TRACKER;
                    name = HOOK_DISABLE;
                }
            }
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }
}
