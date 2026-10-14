package com.gtnh.replaycompat.asm;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Method;

/**
 * Byte-for-byte equivalent of ReplayMod's {@code com.replaymod.core.asm.GLErrorTransformer},
 * rewritten with named static visitors so it can be registered on GTNH + RFB.
 *
 * <p>See {@link GLStateTrackerTransformer} for the full explanation of the
 * {@code ClassCircularityError} this avoids. In short: the stock transformer creates its
 * {@code ClassVisitor} and {@code MethodVisitor} as anonymous classes inside method bodies, so
 * {@code $1$1} is generated during the very first {@code transform()} call -- from inside
 * {@code LaunchClassLoader.runTransformers()} -- which trips the JVM circularity guard:
 *
 * <pre>
 *   java.lang.ClassCircularityError: com/replaymod/core/asm/GLErrorTransformer$1$1
 *       at com.replaymod.core.asm.GLErrorTransformer.transform(GLErrorTransformer.java:32)
 * </pre>
 *
 * <p>Behaviour is preserved exactly: after every call to a method of any {@code GL*}, {@code ARB*} or
 * {@code EXT*} class, a {@code glErrorCheck(owner, name)} call is appended (and stack growth is
 * accounted for). The runtime checker is deliberately kept in <b>this</b> class rather than reusing
 * {@code GLErrorTransformer.glErrorCheck} so that the rewritten transformer has no dependency on the
 * original {@code com.replaymod.core.asm} classes -- the rewritten one is what gets registered, and
 * keeping it self-contained means the original classes can stay exactly as shipped.
 */
public class GLErrorTransformer implements IClassTransformer {

    private static final String GL_CLASS = "org.lwjgl.opengl.GL";
    private static final String GL = GL_CLASS.replace('.', '/');
    private static final String ARB_CLASS = "org.lwjgl.opengl.ARB";
    private static final String ARB = ARB_CLASS.replace('.', '/');
    private static final String EXT_CLASS = "org.lwjgl.opengl.EXT";
    private static final String EXT = EXT_CLASS.replace('.', '/');

    /** Internal name of this class -- used as the injection target of the appended call. */
    static final String SELF = "com/gtnh/replaycompat/asm/GLErrorTransformer";

    /**
     * Same scope restriction as {@link GLStateTrackerTransformer}: only ReplayMod / jGui code is
     * instrumented. The stock transformer appended a {@code glErrorCheck} after <i>every</i> GL call in
     * <i>every</i> class, which on a 260-mod pack both rewrites other mods' mixin anchors and adds a
     * {@code glGetError()} after thousands of calls. Restricting to our own packages keeps the checker
     * useful (it is a ReplayMod render debug aid) without touching anyone else's bytecode.
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

        String internalName = name == null ? null
                : (name.indexOf('/') >= 0 ? name : name.replace('.', '/'));
        if (internalName == null) return basicClass;
        if (!isInScope(internalName)) return basicClass;
        // Never instrument our own injected checker or the transformer itself.
        if (internalName.startsWith("com/gtnh/replaycompat/")) return basicClass;

        ClassReader reader = new ClassReader(basicClass);
        ClassWriter writer = new ClassWriter(reader, 0);
        reader.accept(new ErrorCheckInjector(writer), 0);
        return writer.toByteArray();
    }

    static final class ErrorCheckInjector extends ClassVisitor {
        ErrorCheckInjector(ClassVisitor delegate) {
            super(Opcodes.ASM5, delegate);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            return new GlCallWatcher(mv);
        }
    }

    static final class GlCallWatcher extends MethodVisitor {
        GlCallWatcher(MethodVisitor delegate) {
            super(Opcodes.ASM5, delegate);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            if (owner != null && (owner.startsWith(GL) || owner.startsWith(ARB) || owner.startsWith(EXT))) {
                visitLdcInsn(owner.replace('/', '.'));
                visitLdcInsn(name);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, SELF, "glErrorCheck",
                        "(Ljava/lang/String;Ljava/lang/String;)V", false);
            }
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            super.visitMaxs(maxStack + 2, maxLocals);
        }
    }

    private static boolean inGlBegin;

    /**
     * {@code GL11.glGetError} / {@code GLU.gluErrorString} resolved reflectively.
     *
     * <p>Not a style choice: GTNH ships lwjgl3ify (LWJGL 3) and the LWJGL <b>2</b> compatibility classes
     * are only injected at game runtime, so a compile-time reference to {@code org.lwjgl.opengl.GL11}
     * cannot even be built against this toolchain. Reflection also keeps this class free of any GL
     * linkage until the checker is genuinely invoked -- and {@code GLErrorTransformer} is opt-in and
     * off by default, so a missing GL class must never be able to break startup.
     */
    private static final Method GL_GET_ERROR;
    private static final Method GLU_ERROR_STRING;

    static {
        Method getError = null;
        Method errorString = null;
        try {
            Class<?> gl11 = Class.forName("org.lwjgl.opengl.GL11");
            getError = gl11.getMethod("glGetError", new Class[0]);
            Class<?> glu = Class.forName("org.lwjgl.util.glu.GLU");
            errorString = glu.getMethod("gluErrorString", new Class[] { int.class });
        } catch (Throwable t) {
            System.err.println("[replaycompat] GL error checker unavailable ("
                    + t.getClass().getSimpleName() + ": " + t.getMessage() + "); GLErrorTransformer will be a no-op");
        }
        GL_GET_ERROR = getError;
        GLU_ERROR_STRING = errorString;
    }

    /** Identical in behaviour to {@code com.replaymod.core.asm.GLErrorTransformer.glErrorCheck}. */
    @SuppressWarnings("unused") // called from injected bytecode
    public static void glErrorCheck(String cls, String method) {
        if (GL_GET_ERROR == null || GLU_ERROR_STRING == null) {
            return; // GL not reachable -- nothing we can check
        }
        if ("glBegin".equals(method) && cls.equals(GL_CLASS + "11")) {
            if (inGlBegin) {
                // glBegin within glBegin
                throw new GLError(GL11_INVALID_OPERATION, cls, method);
            }
            inGlBegin = true;
            return;
        }
        if (inGlBegin && "glEnd".equals(method) && cls.equals(GL_CLASS + "11")) {
            inGlBegin = false;
        }
        if (inGlBegin) {
            return; // Cannot call glGetError in between of glBegin and glEnd
        }
        try {
            int err = ((Integer) GL_GET_ERROR.invoke(null, new Object[0])).intValue();
            if (err == 0) {
                return;
            }
            GLError e = new GLError(err, cls, method);
            if ("true".equals(System.getProperty("replaymod.glerrors.throw", "true"))) {
                throw e;
            } else {
                e.printStackTrace();
            }
        } catch (GLError e) {
            throw e;
        } catch (Throwable t) {
            // Never let the checker itself take the game down.
            System.err.println("[replaycompat] glErrorCheck failed: " + t);
        }
    }

    /** {@code GL11.GL_INVALID_OPERATION}, inlined so no LWJGL class is needed at compile time. */
    private static final int GL11_INVALID_OPERATION = 0x0502;

    public static class GLError extends RuntimeException {
        private GLError(int err, String cls, String method) {
            super(errorString(err) + " (" + err + ")");
            StackTraceElement[] stack = getStackTrace();
            stack[0] = new StackTraceElement(cls, method, stack[0].getFileName(), stack[0].getLineNumber());
            setStackTrace(stack);
        }

        private static String errorString(int err) {
            if (GLU_ERROR_STRING == null) {
                return "GL error";
            }
            try {
                return String.valueOf(GLU_ERROR_STRING.invoke(null, new Object[] { Integer.valueOf(err) }));
            } catch (Throwable t) {
                return "GL error";
            }
        }
    }
}
