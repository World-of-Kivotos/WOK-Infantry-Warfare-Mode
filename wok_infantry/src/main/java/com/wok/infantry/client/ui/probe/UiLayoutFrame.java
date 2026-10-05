package com.wok.infantry.client.ui.probe;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Layout record of one rendered frame: declared boxes (with their nesting), drawn texts, reported
 * controls, map icons and the clip state each of them was drawn under. It is the Java counterpart
 * of the preview's {@code Gui} instrumentation ({@code ui-preview/kit/mcgui.js}: {@code begin},
 * {@code end}, {@code _recordText}).
 *
 * <p>All rectangles are in GUI-scaled screen coordinates (Minecraft's logical screen, the space of
 * mouse events), whatever pose the component drew under: {@link UiLayoutProbe} transforms them with
 * the current pose. {@link #guiScale()} turns them into physical pixels, {@link #baseScale()} is the
 * layout factor of the surface (2 for a {@code TacticalScreen} at the minimum 2x), so a text's
 * {@code scale / baseScale} is its size relative to the layout it was designed for.
 *
 * <p>A frame is filled by {@link UiLayoutProbe} on the render thread only while it is installed with
 * {@link UiLayoutProbe#beginFrame}; production code never creates one. Tests may build frames by
 * hand through the public {@code add*} methods.
 */
public final class UiLayoutFrame {
    /** Rectangle {@code [left, right) x [top, bottom)} in GUI-scaled screen coordinates. */
    public record Rect(float left, float top, float right, float bottom) {
        public float width() {
            return right - left;
        }

        public float height() {
            return bottom - top;
        }

        public float centerX() {
            return (left + right) / 2.0F;
        }

        public float centerY() {
            return (top + bottom) / 2.0F;
        }

        public boolean contains(float x, float y) {
            return x >= left && x < right && y >= top && y < bottom;
        }

        /** Whether this rectangle lies inside {@code outer}, allowing {@code epsilon} slack. */
        public boolean within(Rect outer, float epsilon) {
            return left >= outer.left - epsilon && top >= outer.top - epsilon
                    && right <= outer.right + epsilon && bottom <= outer.bottom + epsilon;
        }

        /** Strict overlap of the interiors (touching edges do not overlap). */
        public boolean overlaps(Rect other) {
            return left < other.right && right > other.left && top < other.bottom
                    && bottom > other.top;
        }

        public Rect intersection(Rect other) {
            return new Rect(Math.max(left, other.left), Math.max(top, other.top),
                    Math.min(right, other.right), Math.min(bottom, other.bottom));
        }

        @Override
        public String toString() {
            return "[" + format(left) + "," + format(top) + "," + format(right) + ","
                    + format(bottom) + "]";
        }

        private static String format(float value) {
            float rounded = Math.round(value * 10.0F) / 10.0F;
            return rounded == (int) rounded ? Integer.toString((int) rounded)
                    : Float.toString(rounded);
        }
    }

    /**
     * Declared layout box. {@code parent} is the index of the enclosing box or -1 for the screen;
     * non-solid boxes (modal cards, tooltips, overlays) may cover their siblings.
     */
    public record Box(int index, String id, Rect rect, boolean solid, int parent) {
    }

    /**
     * Drawn text. {@code scale} is the pose scale it was drawn under (GUI pixels per text pixel),
     * {@code box} the innermost open box or -1, {@code clip} the active clip or {@code null}.
     * {@code fitted} texts went through {@code TextFit}: {@code truncated} says whether they were
     * ellipsized and {@code fullText} is the original text when the caller knew it (else "").
     * {@code tipped} marks a truncated text whose caller offers the full text elsewhere.
     * {@code nextControl} is the index the next recorded control gets: widgets report themselves
     * right after drawing their own texts, so only that control can own the text (a text drawn on
     * a later layer, such as a tooltip or a modal card, never belongs to a control below it).
     */
    public record Text(String text, Rect rect, float scale, int box, Rect clip, boolean fitted,
                       boolean truncated, String fullText, boolean tipped, int nextControl) {
        public boolean clipped() {
            return clip != null;
        }
    }

    /**
     * Reported control: a widget or a part of one (list row, tab). {@code state} is the drawn state
     * ({@code NORMAL}, {@code HOVER}, {@code SELECTED}, {@code CURRENT}, {@code DISABLED},
     * {@code DANGER}, {@code DANGER_ARMED}, {@code SUCCESS}, {@code CONTROL}, {@code ERROR} …).
     * {@code tooltip} is the text the control currently offers, {@code disabledReason} why it is
     * disabled, and {@code tipCoversLabel} says that the control shows its full label whenever it
     * is hovered (lists and tab strips do), so a shortened label is never lost.
     */
    public record Control(String uiId, String kind, String state, Rect rect, boolean active,
                          boolean visible, boolean focusRing, boolean truncated, String label,
                          String tooltip, String disabledReason, boolean tipCoversLabel,
                          Rect clip) {
    }

    /**
     * Map icon. {@code rect} is the plate in GUI coordinates; {@code physicalWidth} and
     * {@code physicalHeight} are its size in physical (framebuffer) pixels.
     */
    public record Icon(String id, String state, Rect rect, int physicalWidth, int physicalHeight) {
    }

    private final int guiWidth;
    private final int guiHeight;
    private final double guiScale;
    private final int baseScale;
    private final List<Box> boxes = new ArrayList<>();
    private final List<Text> texts = new ArrayList<>();
    private final List<Control> controls = new ArrayList<>();
    private final List<Icon> icons = new ArrayList<>();
    private final List<String> notes = new ArrayList<>();
    private final Deque<Integer> boxStack = new ArrayDeque<>();
    private final Deque<Rect> clipStack = new ArrayDeque<>();
    private int unbalancedEnds;

    /**
     * @param guiWidth  GUI-scaled screen width (window.getGuiScaledWidth())
     * @param guiHeight GUI-scaled screen height
     * @param guiScale  window GUI scale (physical pixels per GUI pixel)
     * @param baseScale layout factor of the surface (1, or 2 at the minimum 2x)
     */
    public UiLayoutFrame(int guiWidth, int guiHeight, double guiScale, int baseScale) {
        this.guiWidth = Math.max(0, guiWidth);
        this.guiHeight = Math.max(0, guiHeight);
        this.guiScale = guiScale > 0.0D ? guiScale : 1.0D;
        this.baseScale = Math.max(1, baseScale);
    }

    public int guiWidth() {
        return guiWidth;
    }

    public int guiHeight() {
        return guiHeight;
    }

    public double guiScale() {
        return guiScale;
    }

    public int baseScale() {
        return baseScale;
    }

    /** The screen as a rectangle in GUI coordinates. */
    public Rect screen() {
        return new Rect(0.0F, 0.0F, guiWidth, guiHeight);
    }

    public List<Box> boxes() {
        return Collections.unmodifiableList(boxes);
    }

    public List<Text> texts() {
        return Collections.unmodifiableList(texts);
    }

    public List<Control> controls() {
        return Collections.unmodifiableList(controls);
    }

    public List<Icon> icons() {
        return Collections.unmodifiableList(icons);
    }

    public List<String> notes() {
        return Collections.unmodifiableList(notes);
    }

    /** Boxes still open when the frame ended plus {@code end()} calls without a box. */
    public int unbalanced() {
        return boxStack.size() + unbalancedEnds;
    }

    /** First control with {@code uiId}, or {@code null}. */
    public Control control(String uiId) {
        for (Control control : controls) {
            if (control.uiId().equals(uiId)) {
                return control;
            }
        }
        return null;
    }

    /** First box with {@code id}, or {@code null}. */
    public Box box(String id) {
        for (Box box : boxes) {
            if (box.id().equals(id)) {
                return box;
            }
        }
        return null;
    }

    // ---- recording (GUI coordinates) ------------------------------------------------------------

    /** Opens a box; texts and boxes recorded until {@link #endBox()} belong to it. */
    public int beginBox(String id, Rect rect, boolean solid) {
        int index = addBox(id, rect, solid);
        boxStack.push(index);
        return index;
    }

    /** Closes the innermost open box. */
    public void endBox() {
        if (boxStack.isEmpty()) {
            unbalancedEnds++;
        } else {
            boxStack.pop();
        }
    }

    /** Records a box inside the current one without opening it. */
    public int addBox(String id, Rect rect, boolean solid) {
        int index = boxes.size();
        boxes.add(new Box(index, id == null ? "" : id, rect, solid, currentBox()));
        return index;
    }

    public void addText(String text, Rect rect, float scale, boolean fitted, boolean truncated,
                        String fullText, boolean tipped) {
        texts.add(new Text(text == null ? "" : text, rect, scale, currentBox(), currentClip(),
                fitted, truncated, fullText == null ? "" : fullText, tipped, controls.size()));
    }

    /**
     * A new layer (a modal card) starts: the texts drawn so far that no control has claimed yet
     * stay ownerless, so a control of the new layer never adopts a label of the layer below
     * (for example a faint page label under a dialog key).
     */
    public void layer() {
        int next = controls.size();
        for (int index = texts.size() - 1; index >= 0; index--) {
            Text text = texts.get(index);
            if (text.nextControl() != next) {
                break;
            }
            texts.set(index, new Text(text.text(), text.rect(), text.scale(), text.box(),
                    text.clip(), text.fitted(), text.truncated(), text.fullText(), text.tipped(),
                    -1));
        }
    }

    /** Marks text {@code index} as offered in full elsewhere (see {@link UiLayoutProbe#tipped}). */
    public void markTipped(int index) {
        if (index >= 0 && index < texts.size()) {
            Text text = texts.get(index);
            texts.set(index, new Text(text.text(), text.rect(), text.scale(), text.box(),
                    text.clip(), text.fitted(), text.truncated(), text.fullText(), true,
                    text.nextControl()));
        }
    }

    public void addControl(String uiId, String kind, String state, Rect rect, boolean active,
                           boolean visible, boolean focusRing, boolean truncated, String label,
                           String tooltip, String disabledReason, boolean tipCoversLabel) {
        controls.add(new Control(uiId == null ? "" : uiId, kind == null ? "" : kind,
                state == null ? "" : state, rect, active, visible, focusRing, truncated,
                label == null ? "" : label, tooltip == null ? "" : tooltip,
                disabledReason == null ? "" : disabledReason, tipCoversLabel, currentClip()));
    }

    public void addIcon(String id, String state, Rect rect, int physicalWidth, int physicalHeight) {
        icons.add(new Icon(id == null ? "" : id, state == null ? "" : state, rect, physicalWidth,
                physicalHeight));
    }

    /** Free-form remark that ends up in the layout report. */
    public void note(String message) {
        if (message != null && !message.isBlank()) {
            notes.add(message);
        }
    }

    /** Pushes a clip; it is intersected with the enclosing one, as GuiGraphics does. */
    public void pushClip(Rect rect) {
        Rect enclosing = clipStack.peek();
        clipStack.push(enclosing == null ? rect : enclosing.intersection(rect));
    }

    public void popClip() {
        if (!clipStack.isEmpty()) {
            clipStack.pop();
        }
    }

    /** The active clip rectangle, or {@code null}. */
    public Rect currentClip() {
        return clipStack.peek();
    }

    private int currentBox() {
        Integer top = boxStack.peek();
        return top == null ? -1 : top;
    }

    // ---- pose helpers ---------------------------------------------------------------------------

    /** Transforms a pose-space rectangle to GUI coordinates (scale and translation only). */
    public static Rect transform(Matrix4f pose, float left, float top, float right, float bottom) {
        if (pose == null) {
            return new Rect(Math.min(left, right), Math.min(top, bottom), Math.max(left, right),
                    Math.max(top, bottom));
        }
        Vector3f a = pose.transformPosition(left, top, 0.0F, new Vector3f());
        Vector3f b = pose.transformPosition(right, bottom, 0.0F, new Vector3f());
        return new Rect(Math.min(a.x(), b.x()), Math.min(a.y(), b.y()), Math.max(a.x(), b.x()),
                Math.max(a.y(), b.y()));
    }

    /** GUI pixels per pose unit along x (the pose's scale; WOK UI never rotates). */
    public static float scaleOf(Matrix4f pose) {
        return pose == null ? 1.0F : Math.abs(pose.m00());
    }
}
