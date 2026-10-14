package com.gtnh.replaycompat.asm;

/**
 * Eagerly prelinks the named nested visitor classes of the rewritten ReplayMod transformers.
 *
 * <p>Why this exists: classes referenced from inside a method body are only loaded/linked the first
 * time that code path executes. For an {@code IClassTransformer} that first execution happens inside
 * {@code LaunchClassLoader.runTransformers()}, where generating a brand new class through the same
 * loader trips the JVM circularity guard ({@code ClassCircularityError}). The rewritten transformers
 * avoid anonymous classes, but their nested visitors would still be cold on the first
 * {@code transform()} call -- so they are force-loaded here instead.
 *
 * <p>{@link #preload()} is called from the coremod class's constructor, i.e. while FML is discovering
 * coremods, long before any transformation is requested. {@code Class.forName(name, true, loader)}
 * performs load + link + initialise, so by the time {@code transform()} runs the nested classes are
 * fully prepared and loading them again is a no-op.
 *
 * <p>It also serves as a smoke test: if the ASM API used by our visitors is not reachable from the
 * coremod loader, the failure surfaces immediately during coremod discovery rather than as a
 * confusing transform-time error.
 */
public final class Launcher {

    private static final String[] PRELOAD = new String[] {
            // our rewritten, registration-safe transformers
            "com.gtnh.replaycompat.asm.GLStateTrackerTransformer",
            "com.gtnh.replaycompat.asm.GLStateTrackerTransformer$Method",
            "com.gtnh.replaycompat.asm.GLStateTrackerTransformer$EligibleMethodCollector",
            "com.gtnh.replaycompat.asm.GLStateTrackerTransformer$InsnScanner",
            "com.gtnh.replaycompat.asm.GLStateTrackerTransformer$RedirectingVisitor",
            "com.gtnh.replaycompat.asm.GLStateTrackerTransformer$HookRedirector",
            "com.gtnh.replaycompat.asm.GLErrorTransformer",
            "com.gtnh.replaycompat.asm.GLErrorTransformer$1", // javac synthetic helper for the static init's try/catch
            "com.gtnh.replaycompat.asm.GLErrorTransformer$ErrorCheckInjector",
            "com.gtnh.replaycompat.asm.GLErrorTransformer$GlCallWatcher",
            "com.gtnh.replaycompat.asm.GLErrorTransformer$GLError",
            // the ASM / LWJGL types they link against
            "org.objectweb.asm.ClassReader",
            "org.objectweb.asm.ClassWriter",
            "org.objectweb.asm.ClassVisitor",
            "org.objectweb.asm.MethodVisitor",
            "org.objectweb.asm.Opcodes"
    };

    private Launcher() {
    }

    /**
     * Force load/link/initialise every class our transformers reference. Safe to call more than once.
     *
     * @return number of classes that were newly loaded (0 when everything was already prepared)
     */
    public static int preload() {
        ClassLoader loader = Launcher.class.getClassLoader();
        int loaded = 0;
        for (String name : PRELOAD) {
            try {
                Class.forName(name, true, loader);
                loaded++;
            } catch (Throwable t) {
                System.err.println("[replaycompat] preload failed for " + name + ": " + t);
            }
        }
        return loaded;
    }
}
