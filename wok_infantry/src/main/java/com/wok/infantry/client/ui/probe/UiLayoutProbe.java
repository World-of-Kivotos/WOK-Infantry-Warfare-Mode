package com.wok.infantry.client.ui.probe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Layout probe of the WOK步战 UI: shared components report their boxes, texts, controls, clips and
 * map icons here so the development-only UI acceptance can check a real frame for text outside its
 * box, overlapping panels, unreadable CJK, labels shortened without a full-text tooltip and
 * disabled controls without a reason (see {@link UiLayoutReport}).
 *
 * <p><b>No receiver, no work.</b> Production never installs a receiver: {@link #enabled()} stays
 * false and no frame is recording, so every hook is one volatile read and an immediate return —
 * no allocation, no behaviour change. Only the uiTest harness calls {@link #enable()} and installs
 * a {@link UiLayoutFrame} for the single frame it wants to inspect.
 *
 * <p><b>uiId.</b> Screens give controls stable ids with {@link #tag}; the acceptance input driver
 * clicks controls by these ids instead of computing coordinates. Untagged widgets get a derived id:
 * {@code key:<translation key>} for a translatable label, otherwise {@code text:<label>}. Tags are
 * only stored while the probe is enabled.
 *
 * <p>Hooks must be called on the render thread, in the pose the component draws with; they take
 * logical (pose) coordinates and record GUI-scaled screen coordinates.
 */
public final class UiLayoutProbe {
    private static volatile boolean enabled;
    private static volatile UiLayoutFrame frame;
    private static final Map<Object, String> TAGS = new WeakHashMap<>();

    private UiLayoutProbe() {
    }

    // ---- receiver ---------------------------------------------------------------------------------

    /** Turns on uiId tagging and allows frames to be recorded (uiTest only). */
    public static void enable() {
        enabled = true;
    }

    /** Turns the probe off again and forgets every tag. */
    public static void disable() {
        enabled = false;
        frame = null;
        synchronized (TAGS) {
            TAGS.clear();
        }
    }

    public static boolean enabled() {
        return enabled;
    }

    /** Whether a frame is being recorded right now. */
    public static boolean recording() {
        return frame != null;
    }

    /** Starts recording into {@code target} (ignored while the probe is disabled). */
    public static void beginFrame(UiLayoutFrame target) {
        if (enabled) {
            frame = target;
        }
    }

    /** Stops recording and returns the recorded frame, or {@code null}. */
    public static UiLayoutFrame endFrame() {
        UiLayoutFrame recorded = frame;
        frame = null;
        return recorded;
    }

    // ---- ids --------------------------------------------------------------------------------------

    /**
     * Gives {@code control} (a widget or any object a screen reports) the stable id {@code uiId}
     * and returns it, so it can wrap the construction: {@code addRenderableWidget(tag(button,
     * "formation.admin.open"))}. A no-op while the probe is disabled.
     */
    public static <T> T tag(T control, String uiId) {
        if (enabled && control != null && uiId != null && !uiId.isBlank()) {
            synchronized (TAGS) {
                TAGS.put(control, uiId);
            }
        }
        return control;
    }

    /** The tag of {@code control}, or {@code null} (always {@code null} while disabled). */
    public static String tagOf(Object control) {
        if (!enabled || control == null) {
            return null;
        }
        synchronized (TAGS) {
            return TAGS.get(control);
        }
    }

    /** The tag of {@code widget}, or the id derived from its label. */
    public static String uiIdOf(AbstractWidget widget) {
        String tag = tagOf(widget);
        return tag != null ? tag : derivedId(widget.getMessage());
    }

    /** {@code key:<translation key>} for a translatable label, otherwise {@code text:<label>}. */
    public static String derivedId(Component label) {
        if (label == null) {
            return "text:";
        }
        if (label.getContents() instanceof TranslatableContents translatable) {
            return "key:" + translatable.getKey();
        }
        return "text:" + label.getString();
    }

    // ---- boxes ------------------------------------------------------------------------------------

    /**
     * Opens a layout box (preview {@code g.begin}); texts drawn until {@link #end} must stay inside
     * it, and sibling boxes that are both solid must not overlap. Use {@code solid = false} for
     * layers that may cover the board (modal cards, tooltips).
     */
    public static void begin(GuiGraphics graphics, String id, int left, int top, int right,
                             int bottom, boolean solid) {
        UiLayoutFrame target = frame;
        if (target == null) {
            return;
        }
        target.beginBox(id, UiLayoutFrame.transform(pose(graphics), left, top, right, bottom),
                solid);
    }

    /** Closes the box opened by the matching {@link #begin}. */
    public static void end(GuiGraphics graphics) {
        UiLayoutFrame target = frame;
        if (target != null) {
            target.endBox();
        }
    }

    /** Records a solid box without opening it (panels, HUD plates). */
    public static void box(GuiGraphics graphics, String id, int left, int top, int right,
                           int bottom) {
        UiLayoutFrame target = frame;
        if (target == null) {
            return;
        }
        target.addBox(id, UiLayoutFrame.transform(pose(graphics), left, top, right, bottom), true);
    }

    /** {@link #box} named {@code prefix:<label>} (the label is only read while recording). */
    public static void box(GuiGraphics graphics, String prefix, Component label, int left, int top,
                           int right, int bottom) {
        UiLayoutFrame target = frame;
        if (target == null) {
            return;
        }
        String text = label == null ? "" : label.getString();
        target.addBox(text.isEmpty() ? prefix : prefix + ":" + text,
                UiLayoutFrame.transform(pose(graphics), left, top, right, bottom), true);
    }

    // ---- clipping ---------------------------------------------------------------------------------

    /** A scissor was enabled for the GUI rectangle [left, right) x [top, bottom). */
    public static void clipGui(int left, int top, int right, int bottom) {
        UiLayoutFrame target = frame;
        if (target != null) {
            target.pushClip(new UiLayoutFrame.Rect(left, top, right, bottom));
        }
    }

    /** The matching scissor was disabled. */
    public static void unclip() {
        UiLayoutFrame target = frame;
        if (target != null) {
            target.popClip();
        }
    }

    // ---- text -------------------------------------------------------------------------------------

    /**
     * Text drawn by {@code TextFit}: {@code drawn} at ({@code x}, {@code y}) and {@code width}
     * pixels wide; {@code full} (a {@link Component}, a String or {@code null}) is the text before
     * it was fitted.
     */
    public static void fittedText(GuiGraphics graphics, FormattedCharSequence drawn, Object full,
                                  int x, int y, int width, boolean truncated) {
        UiLayoutFrame target = frame;
        if (target == null) {
            return;
        }
        recordText(target, graphics, plain(drawn), x, y, width, true, truncated, plain(full),
                false);
    }

    /** Text drawn directly with {@code GuiGraphics.drawString} (never shortened). */
    public static void rawText(GuiGraphics graphics, Font font, String text, int x, int y) {
        UiLayoutFrame target = frame;
        if (target == null || text == null || font == null) {
            return;
        }
        recordText(target, graphics, text, x, y, font.width(text), false, false, text, false);
    }

    public static void rawText(GuiGraphics graphics, Font font, Component text, int x, int y) {
        UiLayoutFrame target = frame;
        if (target == null || text == null || font == null) {
            return;
        }
        String plain = text.getString();
        recordText(target, graphics, plain, x, y, font.width(text), false, false, plain, false);
    }

    public static void rawText(GuiGraphics graphics, Font font, FormattedCharSequence text, int x,
                               int y) {
        UiLayoutFrame target = frame;
        if (target == null || text == null || font == null) {
            return;
        }
        String plain = plain(text);
        recordText(target, graphics, plain, x, y, font.width(text), false, false, plain, false);
    }

    /**
     * Marks the shortened text that was just drawn at ({@code x}, {@code y}) as offered in full
     * elsewhere (a hover tooltip the caller manages itself), so it is not reported as lost text.
     */
    public static void tipped(GuiGraphics graphics, int x, int y) {
        UiLayoutFrame target = frame;
        if (target == null) {
            return;
        }
        UiLayoutFrame.Rect at = UiLayoutFrame.transform(pose(graphics), x, y, x, y);
        List<UiLayoutFrame.Text> texts = target.texts();
        for (int index = texts.size() - 1; index >= 0; index--) {
            UiLayoutFrame.Text text = texts.get(index);
            if (Math.abs(text.rect().top() - at.top()) < 0.01F
                    && text.rect().left() <= at.left() + 0.01F
                    && text.rect().right() >= at.left() - 0.01F) {
                target.markTipped(index);
                return;
            }
        }
    }

    private static void recordText(UiLayoutFrame target, GuiGraphics graphics, String text, int x,
                                   int y, int width, boolean fitted, boolean truncated,
                                   String full, boolean tipped) {
        Matrix4f pose = pose(graphics);
        target.addText(text, UiLayoutFrame.transform(pose, x, y, x + Math.max(0, width), y + 8),
                UiLayoutFrame.scaleOf(pose), fitted, truncated, full, tipped);
    }

    // ---- controls ---------------------------------------------------------------------------------

    /**
     * A widget finished drawing. {@code state} is the state it was drawn in (for WOK keys the
     * {@code TacticalButtonStyle.State} name); a disabled widget's reason is its tooltip.
     */
    public static void widget(GuiGraphics graphics, AbstractWidget widget, String kind, String state,
                              boolean truncated, boolean focusRing) {
        UiLayoutFrame target = frame;
        if (target == null || widget == null) {
            return;
        }
        String tooltip = tooltipText(widget.getTooltip());
        target.addControl(uiIdOf(widget), kind, state,
                UiLayoutFrame.transform(pose(graphics), widget.getX(), widget.getY(),
                        widget.getX() + widget.getWidth(), widget.getY() + widget.getHeight()),
                widget.active, widget.visible, focusRing, truncated,
                widget.getMessage().getString(), tooltip, widget.active ? "" : tooltip, false);
    }

    /**
     * A part of a widget that acts as its own control: a list row or a tab. Its id is
     * {@code <owner id>/<part>}; {@code reason} (may be {@code null}) explains why it is disabled;
     * {@code tipCoversLabel} says the owner shows the full label on hover.
     */
    public static void part(GuiGraphics graphics, AbstractWidget owner, String part, String kind,
                            String state, int left, int top, int right, int bottom, boolean active,
                            Component label, Component reason, boolean truncated,
                            boolean tipCoversLabel) {
        UiLayoutFrame target = frame;
        if (target == null || owner == null) {
            return;
        }
        target.addControl(uiIdOf(owner) + "/" + part, kind, state,
                UiLayoutFrame.transform(pose(graphics), left, top, right, bottom), active,
                owner.visible, false, truncated, label == null ? "" : label.getString(), "",
                reason == null ? "" : reason.getString(), tipCoversLabel);
    }

    /**
     * A control a screen draws itself (no widget behind it), for example a statically drawn key in
     * a gallery or a custom row whose full text the screen offers on hover.
     */
    public static void control(GuiGraphics graphics, String uiId, String kind, String state,
                               int left, int top, int right, int bottom, boolean active,
                               Component label, Component tooltip, Component reason,
                               boolean truncated, boolean focusRing) {
        UiLayoutFrame target = frame;
        if (target == null) {
            return;
        }
        target.addControl(uiId, kind, state,
                UiLayoutFrame.transform(pose(graphics), left, top, right, bottom), active, true,
                focusRing, truncated, label == null ? "" : label.getString(),
                tooltip == null ? "" : tooltip.getString(),
                reason == null ? "" : reason.getString(), false);
    }

    // ---- map icons --------------------------------------------------------------------------------

    /**
     * A map icon plate was drawn at physical pixels [left, right) x [top, bottom) (physical =
     * framebuffer pixels; {@code guiScale} converts to GUI coordinates).
     */
    public static void icon(String id, String state, int left, int top, int right, int bottom,
                            double guiScale) {
        UiLayoutFrame target = frame;
        if (target == null) {
            return;
        }
        double scale = guiScale > 0.0D ? guiScale : 1.0D;
        target.addIcon(id, state, new UiLayoutFrame.Rect((float) (left / scale),
                        (float) (top / scale), (float) (right / scale), (float) (bottom / scale)),
                right - left, bottom - top);
    }

    /** Adds a remark to the frame being recorded. */
    public static void note(String message) {
        UiLayoutFrame target = frame;
        if (target != null) {
            target.note(message);
        }
    }

    // ---- helpers ----------------------------------------------------------------------------------

    private static Matrix4f pose(GuiGraphics graphics) {
        return graphics == null ? null : graphics.pose().last().pose();
    }

    /** Plain string of a Component, String or FormattedCharSequence ("" for anything else). */
    static String plain(Object text) {
        if (text == null) {
            return "";
        }
        if (text instanceof String string) {
            return string;
        }
        if (text instanceof Component component) {
            return component.getString();
        }
        if (text instanceof FormattedCharSequence sequence) {
            StringBuilder builder = new StringBuilder();
            sequence.accept((index, style, codePoint) -> {
                builder.appendCodePoint(codePoint);
                return true;
            });
            return builder.toString();
        }
        return "";
    }

    /** The lines a tooltip shows, joined with line breaks ("" for none). */
    static String tooltipText(Tooltip tooltip) {
        if (tooltip == null) {
            return "";
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return "";
        }
        List<FormattedCharSequence> lines = tooltip.toCharSequence(minecraft);
        StringBuilder builder = new StringBuilder();
        for (FormattedCharSequence line : lines) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(plain(line));
        }
        return builder.toString();
    }
}
