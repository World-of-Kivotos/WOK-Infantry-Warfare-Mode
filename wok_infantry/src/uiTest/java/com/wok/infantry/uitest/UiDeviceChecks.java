package com.wok.infantry.uitest;

import com.wok.infantry.client.screen.BezelKey;
import com.wok.infantry.client.screen.TacticalBezelPlan;
import com.wok.infantry.client.screen.TacticalBoardChrome;
import com.wok.infantry.client.screen.TacticalButtonStyle;
import com.wok.infantry.client.screen.TacticalLivery;
import com.wok.infantry.client.screen.TacticalScreen;
import com.wok.infantry.client.screen.TacticalShellLayout;
import com.wok.infantry.client.screen.TacticalTabStrip;
import com.wok.infantry.client.screen.UiRect;
import com.wok.infantry.client.ui.probe.UiLayoutFrame;
import com.wok.infantry.client.ui.probe.UiLayoutProbe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Semantic checks of the D2 tablet device (plan 6, "新增语义检查"), run by {@link UiCaseRunner}
 * on every capture of a case with a livery whose screen is a {@link TacticalScreen}:
 * <ul>
 *   <li>the shell's probe note records the livery and link state the frame was drawn with, and
 *       the livery is the one the case expects;</li>
 *   <li>the shell regions {@code shell.device}, {@code shell.status} and {@code shell.bezel} are
 *       reported, all non-solid;</li>
 *   <li>the hardware Esc and R keys are on the bezel;</li>
 *   <li>a screen with page keys shows the current page's key pressed ({@code CURRENT}) on the
 *       bezel, under the only lit LED;</li>
 *   <li>every danger key carries the hazard stripes ({@link TacticalButtonStyle.Palette#hazard()})
 *       and its label starts clear of them;</li>
 *   <li>receipts live in the status-bar pill: no text on the bezel that no key owns (the old
 *       footer receipt is gone);</li>
 *   <li>no text and no page control outside the glass, except the bezel keys and tooltips.</li>
 * </ul>
 * Every capture also leaves one {@code device[...]} observation line in the acceptance result.
 */
public final class UiDeviceChecks {
    /** Rounding slack of transformed rectangles (as the layout report). */
    private static final float EPSILON = 0.01F;
    /** Probe box id of the tactical tooltip. */
    private static final String TOOLTIP_BOX = "tooltip";

    private UiDeviceChecks() {
    }

    /** Runs the device checks on {@code capture}; a capture of another screen is left alone. */
    static void check(UiCaseContext context, UiCapture.Result capture,
                      TacticalLivery.Livery expected) {
        if (!(capture.screen() instanceof TacticalScreen screen) || capture.frame() == null) {
            return;
        }
        UiLayoutFrame frame = capture.frame();
        int scale = Math.max(1, capture.baseScale());
        String where = context.uiCase().id() + "@" + (context.tier() == null ? "-"
                : context.tier().id());

        // The resolved livery and link state, as the shell recorded them.
        Map<String, String> shell = shellNote(frame);
        context.require(!shell.isEmpty(), "the tablet shell left no probe note (device not drawn)");
        context.require(expected.name().equals(shell.get("livery")),
                "the device is painted " + shell.get("livery") + ", the case expects "
                        + expected.name());

        // Shell regions: reported, and never solid (texts and keys may sit on them).
        for (String id : List.of(TacticalBoardChrome.DEVICE_UI_ID,
                TacticalBoardChrome.STATUS_UI_ID, TacticalBoardChrome.BEZEL_UI_ID)) {
            UiLayoutFrame.Box box = frame.box(id);
            context.require(box != null, "the shell region " + id + " was not reported");
            context.require(!box.solid(), "the shell region " + id + " is reported as solid");
        }
        UiLayoutFrame.Rect bezel = frame.box(TacticalBoardChrome.BEZEL_UI_ID).rect();

        // Hardware keys at both ends of the bezel.
        UiLayoutFrame.Control esc = bezelKey(context, frame, bezel,
                TacticalBoardChrome.ESC_KEY_UI_ID);
        UiLayoutFrame.Control refresh = bezelKey(context, frame, bezel,
                TacticalBoardChrome.REFRESH_KEY_UI_ID);

        // The current page key: pressed, on the bezel, under the only lit LED.
        TacticalTabStrip strip = screen.tabStrip();
        String stripId = strip == null ? null : UiLayoutProbe.uiIdOf(strip);
        String page = "-";
        if (strip != null && strip.skin() == TacticalTabStrip.Skin.BEZEL
                && strip.current() >= 0 && strip.current() < strip.tabs().size()) {
            String currentId = stripId + "/" + strip.tabs().get(strip.current()).id();
            UiLayoutFrame.Control current = frame.control(currentId);
            context.require(current != null && current.visible(),
                    "the current page key " + currentId + " is not on screen");
            context.require("CURRENT".equals(current.state()),
                    "the current page key " + currentId + " is drawn " + current.state()
                            + ", not pressed");
            context.require(current.rect().within(bezel, EPSILON),
                    "the page key " + currentId + " " + current.rect() + " leaves the bezel "
                            + bezel);
            List<String> lit = ledNotes(frame, true);
            TacticalBezelPlan plan = TacticalBezelPlan.plan(context.minecraft().font,
                    screen.shellLayout(), screen.bezelHints());
            if (plan.hasLeds()) {
                String expectedNote = BezelKey.ledNote(true, current.rect());
                context.require(lit.size() == 1 && lit.get(0).equals(expectedNote),
                        "the LED above the current page key is not the only lit one: " + lit
                                + " (expected " + expectedNote + ")");
            } else {
                context.require(lit.isEmpty(), "LEDs lit on a bezel without LEDs: " + lit);
            }
            page = strip.tabs().get(strip.current()).id() + (plan.hasLeds() ? "+led" : "");
        }

        // Danger keys: hazard stripes, label clear of them.
        List<UiLayoutFrame.Control> controls = frame.controls();
        int dangerKeys = 0;
        for (int index = 0; index < controls.size(); index++) {
            UiLayoutFrame.Control control = controls.get(index);
            if (!control.visible() || !isDangerState(control.state())) {
                continue;
            }
            dangerKeys++;
            TacticalButtonStyle.Palette palette = TacticalButtonStyle.palette(
                    new TacticalButtonStyle.Look(TacticalButtonStyle.State.valueOf(
                            control.state()), false));
            context.require(palette.hazard(), control.uiId() + " is a danger key without the"
                    + " hazard stripes");
            int height = Math.round(control.rect().height() / scale);
            float clear = control.rect().left()
                    + (TacticalButtonStyle.hazardWidth(height) + 1) * scale - EPSILON;
            for (UiLayoutFrame.Text text : ownedTexts(frame, index)) {
                context.require(text.rect().left() >= clear, "the label '" + text.text()
                        + "' of danger key " + control.uiId() + " starts on its hazard stripes ("
                        + text.rect() + " in " + control.rect() + ")");
            }
        }

        // Receipts: no text on the bezel that no key owns (no footer receipt any more).
        TacticalShellLayout layout = screen.shellLayout();
        UiRect glassLayout = layout.glass();
        UiLayoutFrame.Rect glass = new UiLayoutFrame.Rect(glassLayout.left() * (float) scale,
                glassLayout.top() * (float) scale, glassLayout.right() * (float) scale,
                glassLayout.bottom() * (float) scale);
        for (UiLayoutFrame.Text text : frame.texts()) {
            UiLayoutFrame.Rect shown = shown(text);
            if (shown == null || inTooltip(frame, text)) {
                continue;
            }
            boolean onBezel = bezel.contains(shown.centerX(), shown.centerY());
            boolean onKey = insideControl(controls, shown);
            if (onBezel && !onKey) {
                context.fail("text '" + text.text() + "' " + shown + " sits on the bezel outside"
                        + " every key (receipts belong in the status-bar pill)");
            }
            if (!shown.within(glass, EPSILON) && !(onBezel && onKey)) {
                context.fail("text '" + text.text() + "' " + shown + " leaves the glass " + glass);
            }
        }
        // Page controls stay on the glass; only the bezel keys sit below it.
        for (UiLayoutFrame.Control control : controls) {
            if (!control.visible() || control.rect().width() <= 0.0F
                    || control.rect().height() <= 0.0F
                    || isBezelControl(control, stripId, bezel)) {
                continue;
            }
            UiLayoutFrame.Rect shown = control.clip() == null ? control.rect()
                    : control.rect().intersection(control.clip());
            if (shown.width() <= 0.0F || shown.height() <= 0.0F) {
                continue;
            }
            context.require(shown.within(glass, EPSILON), "control " + control.uiId() + " "
                    + shown + " leaves the glass " + glass);
        }

        context.observe("device[" + where + "]=livery=" + shell.get("livery") + " link="
                + shell.get("link") + " esc=" + esc.state() + " refresh=" + refresh.state()
                + (refresh.disabledReason().isBlank() ? "" : "(reason)") + " page=" + page
                + " dangerKeys=" + dangerKeys);
    }

    /**
     * The fields of the shell's probe note ({@code livery}, {@code link}), or an empty map when
     * the frame has none.
     */
    public static Map<String, String> shellNote(UiLayoutFrame frame) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (frame == null) {
            return fields;
        }
        for (String note : frame.notes()) {
            if (note.startsWith(TacticalBoardChrome.SHELL_NOTE)) {
                for (String token : note.substring(TacticalBoardChrome.SHELL_NOTE.length())
                        .trim().split("\\s+")) {
                    int equals = token.indexOf('=');
                    if (equals > 0) {
                        fields.put(token.substring(0, equals), token.substring(equals + 1));
                    }
                }
                break;
            }
        }
        return fields;
    }

    /** The LED notes of the frame that are lit ({@code lit}) or unlit. */
    static List<String> ledNotes(UiLayoutFrame frame, boolean lit) {
        List<String> notes = new ArrayList<>();
        String marker = "lit=" + lit + " ";
        for (String note : frame.notes()) {
            if (note.startsWith(BezelKey.LED_NOTE)
                    && note.startsWith(marker, BezelKey.LED_NOTE.length())) {
                notes.add(note);
            }
        }
        return notes;
    }

    private static UiLayoutFrame.Control bezelKey(UiCaseContext context, UiLayoutFrame frame,
                                                  UiLayoutFrame.Rect bezel, String uiId) {
        UiLayoutFrame.Control key = frame.control(uiId);
        context.require(key != null && key.visible() && key.rect().width() > 0.0F
                && key.rect().height() > 0.0F, "the hardware key " + uiId + " is not on screen");
        context.require(key.rect().within(bezel, EPSILON), "the hardware key " + uiId + " "
                + key.rect() + " leaves the bezel " + bezel);
        return key;
    }

    private static boolean isDangerState(String state) {
        return TacticalButtonStyle.State.DANGER.name().equals(state)
                || TacticalButtonStyle.State.DANGER_ARMED.name().equals(state);
    }

    /**
     * Whether {@code control} is a key of the bezel: Esc, R, the registered page strip or one of
     * its keys, or any key or tab strip lying wholly on the bezel (a page strip a screen did not
     * register with {@code setTabStrip}).
     */
    private static boolean isBezelControl(UiLayoutFrame.Control control, String stripId,
                                          UiLayoutFrame.Rect bezel) {
        String uiId = control.uiId();
        if (uiId.startsWith("shell.key.") || stripId != null
                && (uiId.equals(stripId) || uiId.startsWith(stripId + "/"))) {
            return true;
        }
        String kind = control.kind();
        return ("key".equals(kind) || "tab".equals(kind) || "tabs".equals(kind))
                && control.rect().within(bezel, EPSILON);
    }

    /**
     * Texts the control at {@code index} drew (the layout report's ownership rule: recorded
     * right before it, centred inside it).
     */
    private static List<UiLayoutFrame.Text> ownedTexts(UiLayoutFrame frame, int index) {
        UiLayoutFrame.Control control = frame.controls().get(index);
        List<UiLayoutFrame.Text> owned = new ArrayList<>();
        for (UiLayoutFrame.Text text : frame.texts()) {
            UiLayoutFrame.Rect rect = text.rect();
            if (text.nextControl() == index && rect.width() > 0.0F && !text.text().isBlank()
                    && control.rect().contains(rect.centerX(), rect.centerY())) {
                owned.add(text);
            }
        }
        return owned;
    }

    /** The part of a drawn text that is visible (inside its clip), or {@code null} for none. */
    private static UiLayoutFrame.Rect shown(UiLayoutFrame.Text text) {
        UiLayoutFrame.Rect rect = text.rect();
        if (rect.width() <= 0.0F || text.text().isBlank()) {
            return null;
        }
        if (text.clipped()) {
            rect = rect.intersection(text.clip());
        }
        return rect.width() > 0.0F && rect.height() > 0.0F ? rect : null;
    }

    private static boolean insideControl(List<UiLayoutFrame.Control> controls,
                                         UiLayoutFrame.Rect rect) {
        for (UiLayoutFrame.Control control : controls) {
            if (control.visible() && rect.within(control.rect(), EPSILON)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the text was drawn inside a tooltip (tooltips may cover the whole screen). */
    private static boolean inTooltip(UiLayoutFrame frame, UiLayoutFrame.Text text) {
        List<UiLayoutFrame.Box> boxes = frame.boxes();
        for (int at = text.box(); at >= 0 && at < boxes.size(); at = boxes.get(at).parent()) {
            if (TOOLTIP_BOX.equals(boxes.get(at).id())) {
                return true;
            }
        }
        return false;
    }
}
