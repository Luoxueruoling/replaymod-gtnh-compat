package com.gtnh.replaycompat.mixin;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Adds the ReplayMod entry point (replay viewer) to GTNH's Custom Main Menu.
 *
 * <p><b>Why this lives in the ReplayMod adapter and not in ServerReplay.</b> It used to be
 * {@code com.gtnh.serverreplay.mixin.MixinGuiCustom}. That made the client's only way into ReplayMod
 * depend on the server-side recording mod: as soon as ServerReplay was moved to
 * {@code mods-disabled/} for a single-variable experiment, the main menu silently lost its replay
 * button. The entry point is ReplayMod's own UI, so it belongs to the ReplayMod adapter.
 *
 * <p><b>Why the target is {@code GuiCustom} and not {@code GuiFakeMain}.</b> Custom Main Menu never
 * displays {@code GuiFakeMain}: {@code CMMEventHandler#openGui} intercepts Forge's {@code GuiOpenEvent}
 * (carrying a {@code GuiMainMenu}) and substitutes its own cached {@code GuiCustom}, so the screen the
 * player actually sees is {@code GuiCustom}. {@code GuiFakeMain} is only constructed and poked into
 * {@code InitGuiEvent.gui} by reflection from {@code initGuiPostEarly} and is never passed to
 * {@code Minecraft#displayGuiScreen}, so its {@code initGui} never runs. Stock ReplayMod bails out on
 * {@code !(event.gui instanceof GuiMainMenu)} and {@code GuiCustom} extends {@code GuiScreen} directly,
 * which is why stock ReplayMod shows no button here.
 *
 * <p><b>★ Everything goes through reflection, and that is load-bearing -- not style.</b>
 * This adapter is built with plain {@code javac} and <b>no {@code reobfJar} step</b>, so a source-level
 * reference to an MCP member name stays in the bytecode and blows up at runtime. The first version of
 * this class used {@code button.id} directly and produced exactly the failure documented in HANDOVER
 * pitfall 28:
 *
 * <pre>
 *   java.lang.NoSuchFieldError: Class net.minecraft.client.gui.GuiButton does not have member field 'int id'
 * </pre>
 *
 * ({@code id} is {@code field_146127_k} at runtime.) Reflection defers member resolution to run time,
 * where each name is tried in both MCP and SRG spelling, so this class needs neither a reobf step nor a
 * refmap. The same reasoning covers {@code GuiCustom#getButtonList} and ReplayMod's own classes.
 */
@Mixin(targets = "lumien.custommainmenu.gui.GuiCustom")
public abstract class MixinGuiCustomReplayEntry {

    /** Same id ReplayMod uses on the vanilla main menu, so nothing else claims it. */
    private static final int REPLAY_BUTTON_ID = 17890234;

    private static final String TAG = "[replaycompat] ";

    @Inject(method = "func_73866_w_", at = @At("RETURN"), remap = false, require = 0)
    private void replaycompat$addReplayModButton(CallbackInfo ci) {
        try {
            ensureReplayButton();
        } catch (Throwable t) {
            reportFailure("initGui", t);
        }
    }

    /**
     * Idempotent add, driven from {@code drawScreen} as well.
     *
     * <p>{@code initGui} alone is not a reliable trigger: the hook is only applied when
     * {@code GuiCustom} is mixed, which happens while the class is being loaded, and if this screen's
     * {@code initGui} already ran before that (or only ever runs once, long before), the RETURN
     * injection never fires. That is exactly what was observed: with {@code initGui} as the only hook
     * there was no output at all, while in an earlier build a mid-session resource reload re-invoked
     * {@code initGui} and the hook did fire. {@code drawScreen} runs every frame, so the button is
     * guaranteed to appear; the guards below make repeated calls cheap and silent.
     */
    @Inject(method = "func_73863_a", at = @At("HEAD"), remap = false, require = 0)
    private void replaycompat$ensureReplayButtonOnDraw(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        try {
            ensureReplayButton();
        } catch (Throwable t) {
            reportFailure("drawScreen", t);
        }
    }

    @Unique
    private static boolean replaycompat$ensureFailedOnce;

    private void ensureReplayButton() throws Exception {
        GuiScreen self = (GuiScreen) (Object) this;
        // MCP: width/height -- SRG: field_146294_l/field_146295_m
        int width = getIntField(self, "width", "field_146294_l");
        int height = getIntField(self, "height", "field_146295_m");

        List<GuiButton> buttons = buttonListOf(self);
        if (buttons == null) {
            reportFailure("getButtonList returned null", null);
            return;
        }
        for (GuiButton existing : buttons) {
            if (existing != null && getIntField(existing, "id", "field_146127_k") == REPLAY_BUTTON_ID) {
                return; // already present -- stay silent, this runs every frame
            }
        }

        String label = localizedViewerLabel();
        GuiButton button = new GuiButton(
                REPLAY_BUTTON_ID,
                width - 110,
                8,
                100,
                20,
                label);
        buttons.add(button);
        log("ReplayMod main menu button added at ("
                + getIntField(button, "xPosition", "field_146128_h") + ", "
                + getIntField(button, "yPosition", "field_146129_i") + ") label='" + label + "' on a "
                + width + "x" + height + " screen (" + buttons.size() + " buttons total)");
    }

    /** Logs a failure at most once (drawScreen would otherwise repeat it every frame). */
    private void reportFailure(String where, Throwable t) {
        if (replaycompat$ensureFailedOnce) {
            return;
        }
        replaycompat$ensureFailedOnce = true;
        log("FAILED in " + where + ": " + t);
        if (t != null) {
            java.io.StringWriter sw = new java.io.StringWriter();
            t.printStackTrace(new java.io.PrintWriter(sw));
            log(sw.toString());
        }
    }

    @Inject(method = "func_146284_a", at = @At("HEAD"), remap = false, require = 0)
    private void replaycompat$onActionPerformed(GuiButton button, CallbackInfo ci) {
        try {
            if (button != null && getIntField(button, "id", "field_146127_k") == REPLAY_BUTTON_ID) {
                log("ReplayMod main menu button clicked, opening the replay viewer");
                openReplayViewer();
            }
        } catch (Throwable t) {
            reportFailure("actionPerformed", t);
        }
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Logs to stdout, which FML routes into {@code latest.log} / {@code dsh-stdout.log}.
     *
     * <p>An earlier version also appended to a file via a <b>relative</b> path
     * ({@code Paths.get("replaycompat-probe.log")}). That file never appeared in the game directory, so
     * the probe silently produced nothing and made it look as though the hook never ran -- the opposite
     * of the truth (bytecode export proved the injection was correct, and the button did appear).
     * Plain stdout is enough and has no such ambiguity; note the log line shows up attributed to the
     * <i>target</i> class ({@code GuiCustom:log:NNN}) because the helper is merged into it.
     */
    private static void log(String message) {
        System.out.println(TAG + message);
    }

    /** {@code GuiCustom#getButtonList()} reflectively, so GuiCustom stays a compile-time stranger. */
    @SuppressWarnings("unchecked")
    private static List<GuiButton> buttonListOf(Object guiCustom) throws Exception {
        Method m = guiCustom.getClass().getMethod("getButtonList");
        return (List<GuiButton>) m.invoke(guiCustom);
    }

    /**
     * Reads a field trying each candidate name in order. Field names differ between the MCP-mapped
     * development jar and the SRG-renamed runtime, and this adapter has no reobf/refmap step to bridge
     * them, so both spellings are attempted.
     */
    private static int getIntField(Object target, String... names) throws Exception {
        Field field = findField(target.getClass(), names);
        return field.getInt(target);
    }

    private static Field findField(Class<?> type, String... names) throws NoSuchFieldException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (String name : names) {
                try {
                    Field f = c.getDeclaredField(name);
                    f.setAccessible(true);
                    return f;
                } catch (NoSuchFieldException ignored) {
                    // try the next spelling / superclass
                }
            }
        }
        throw new NoSuchFieldException(String.valueOf((Object) names[0]) + " (tried "
                + java.util.Arrays.toString(names) + ") on " + type.getName());
    }

    /**
     * {@code I18n.format("replaymod.gui.replayviewer")} reflectively, falling back to a plain label.
     *
     * <p>Two traps here, both hit in turn:
     * <ol>
     *   <li><b>The member name is remapped at runtime.</b> {@code I18n.format} is MCP spelling; the
     *       shipped jar runs SRG, where it is {@code func_135052_a}. Matching only {@code "format"}
     *       never finds a method, so the label silently fell back to the literal "ReplayMod" even
     *       though {@code zh_CN.lang} does contain {@code replaymod.gui.replayviewer=回放中心}.</li>
     *   <li><b>The descriptor has two parameters.</b> {@code format(String, Object...)} compiles to
     *       {@code format(String, Object[])}, so an earlier single-parameter lookup could not match
     *       either.</li>
     * </ol>
     * Both spellings are therefore tried, and {@code I18n} returning the key itself (its
     * missing-translation behaviour) is rejected so we fall back rather than render a raw key.
     */
    private static String localizedViewerLabel() {
        final String key = "replaymod.gui.replayviewer";
        try {
            Class<?> i18n = Class.forName("net.minecraft.client.resources.I18n");
            for (Method m : i18n.getMethods()) {
                // MCP: format  --  SRG (what actually runs): func_135052_a
                if (!"format".equals(m.getName()) && !"func_135052_a".equals(m.getName())) {
                    continue;
                }
                Class<?>[] params = m.getParameterTypes();
                if (params.length != 2 || params[0] != String.class || params[1] != Object[].class) {
                    continue;
                }
                Object localized = m.invoke(null, key, new Object[0]);
                if (localized != null) {
                    String text = String.valueOf(localized);
                    if (text.length() > 0 && !key.equals(text)) {
                        return text;
                    }
                }
            }
        } catch (Throwable ignored) {
            // fall through to the literal label
        }
        return "ReplayMod";
    }

    /** {@code new GuiReplayViewer(ReplayModReplay.instance).display()} reflectively. */
    private static void openReplayViewer() throws Exception {
        Class<?> replayModReplay = Class.forName("com.replaymod.replay.ReplayModReplay");
        Object instance = replayModReplay.getField("instance").get(null);
        Class<?> viewerClass = Class.forName("com.replaymod.replay.gui.screen.GuiReplayViewer");
        Constructor<?> ctor = viewerClass.getConstructor(replayModReplay);
        Object viewer = ctor.newInstance(instance);
        viewerClass.getMethod("display").invoke(viewer);
    }
}
