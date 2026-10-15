package com.wok.infantry.client.ui.probe;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Layout checker of one {@link UiLayoutFrame}. It ports the preview's self-check
 * ({@code ui-preview/kit/mcgui.js} {@code check()}: text off screen or outside its box, boxes
 * outside their parent, overlapping solid siblings, shrunken text) and adds the rules of the WOK步战
 * UI baseline: CJK text below 2 physical pixels per text pixel, shortened text without a full-text
 * tooltip, disabled controls without a reason, English status placeholders on a Chinese client,
 * controls and their labels outside the screen or their key.
 *
 * <p>Pure logic without a client; the uiTest harness decides whether violations fail a case
 * (migrated surfaces) or are only reported (surfaces that still wait for their batch).
 */
public final class UiLayoutReport {
    /** Slack for float rounding of transformed rectangles (same as the preview). */
    static final float EPSILON = 0.01F;

    /** Status words that must not reach a Chinese client untranslated (and HUD role tags). */
    public static final Pattern PLACEHOLDER = Pattern.compile(
            "(?<![A-Za-z])(READY|LOCKED|INBOUND|COOLDOWN|OFFLINE)(?![A-Za-z])|\\[(SL|CO)]");

    /** One rule of the checker; {@link #id()} is the stable name used in reports. */
    public enum Rule {
        TEXT_OFFSCREEN("text-offscreen"),
        TEXT_OVERFLOW("text-overflow"),
        TEXT_SCALED_DOWN("text-scaled-down"),
        CJK_TOO_SMALL("cjk-too-small"),
        TEXT_TRUNCATED_NO_TIP("text-truncated-no-tip"),
        PLACEHOLDER_TEXT("placeholder-text"),
        BOX_NEGATIVE("box-negative"),
        BOX_OUTSIDE("box-outside"),
        BOX_OVERLAP("box-overlap"),
        UNBALANCED_BOXES("unbalanced-boxes"),
        CONTROL_OFFSCREEN("control-offscreen"),
        CONTROL_TEXT_OVERFLOW("control-text-overflow"),
        CONTROL_TRUNCATED_NO_TIP("control-truncated-no-tip"),
        CONTROL_DISABLED_NO_REASON("control-disabled-no-reason");

        private final String id;

        Rule(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    /**
     * One finding. {@code subject} is the text, box id or uiId concerned, {@code at} its rectangle
     * and {@code with} the box, parent or sibling it collides with ({@code null} when none).
     */
    public record Violation(Rule rule, String subject, String detail, UiLayoutFrame.Rect at,
                            UiLayoutFrame.Rect with) {
        @Override
        public String toString() {
            return rule.id() + " '" + subject + "' " + detail + " at " + at
                    + (with == null ? "" : " vs " + with);
        }
    }

    /**
     * @param language client language code ({@code zh_cn}); placeholders are only checked for
     *                 Chinese
     */
    public record Options(String language) {
        public Options {
            language = language == null ? "" : language.toLowerCase(Locale.ROOT);
        }

        public static Options of(String language) {
            return new Options(language);
        }

        public boolean chinese() {
            return language.startsWith("zh");
        }
    }

    private UiLayoutReport() {
    }

    /** All violations of {@code frame}, in a stable order (texts, boxes, controls). */
    public static List<Violation> check(UiLayoutFrame frame, Options options) {
        Options safe = options == null ? Options.of("") : options;
        List<Violation> violations = new ArrayList<>();
        UiLayoutFrame.Rect screen = frame.screen();
        List<UiLayoutFrame.Box> boxes = frame.boxes();
        List<UiLayoutFrame.Control> controls = frame.controls();
        Map<UiLayoutFrame.Control, Boolean> ownedTruncation = new IdentityHashMap<>();

        for (UiLayoutFrame.Text text : frame.texts()) {
            UiLayoutFrame.Rect rect = text.rect();
            boolean drawn = rect.width() > 0.0F && !text.text().isEmpty();
            UiLayoutFrame.Control owner = owner(controls, text);
            if (drawn && !text.clipped()) {
                if (!rect.within(screen, EPSILON)) {
                    violations.add(new Violation(Rule.TEXT_OFFSCREEN, text.text(),
                            "text leaves the screen", rect, screen));
                } else if (text.box() >= 0 && !rect.within(boxes.get(text.box()).rect(), EPSILON)) {
                    UiLayoutFrame.Box box = boxes.get(text.box());
                    violations.add(new Violation(Rule.TEXT_OVERFLOW, text.text(),
                            "text leaves box " + box.id(), rect, box.rect()));
                }
                if (owner != null && !rect.within(owner.rect(), EPSILON)) {
                    violations.add(new Violation(Rule.CONTROL_TEXT_OVERFLOW, text.text(),
                            "label leaves control " + owner.uiId(), rect, owner.rect()));
                }
            }
            if (drawn && text.scale() / frame.baseScale() < 0.999F) {
                violations.add(new Violation(Rule.TEXT_SCALED_DOWN, text.text(),
                        "text drawn at " + round(text.scale() / frame.baseScale())
                                + "x of its layout", rect, null));
            }
            double physical = text.scale() * frame.guiScale();
            if (drawn && containsCjk(text.text()) && physical < 1.999D) {
                violations.add(new Violation(Rule.CJK_TOO_SMALL, text.text(),
                        "CJK text at " + round(physical)
                                + " physical px per text px (needs 2)", rect, null));
            }
            if (drawn && safe.chinese() && PLACEHOLDER.matcher(text.text()).find()) {
                violations.add(new Violation(Rule.PLACEHOLDER_TEXT, text.text(),
                        "untranslated status placeholder on a " + safe.language() + " client",
                        rect, null));
            }
            if (text.truncated()) {
                if (owner != null) {
                    ownedTruncation.put(owner, Boolean.TRUE);
                } else if (!text.tipped()) {
                    violations.add(new Violation(Rule.TEXT_TRUNCATED_NO_TIP,
                            text.fullText().isEmpty() ? text.text() : text.fullText(),
                            "shortened text has no full-text tooltip", rect, null));
                }
            }
        }

        for (UiLayoutFrame.Box box : boxes) {
            UiLayoutFrame.Rect rect = box.rect();
            if (rect.width() < 0.0F || rect.height() < 0.0F) {
                violations.add(new Violation(Rule.BOX_NEGATIVE, box.id(), "negative size", rect,
                        null));
            }
            UiLayoutFrame.Rect parent = box.parent() >= 0 ? boxes.get(box.parent()).rect() : screen;
            if (!rect.within(parent, EPSILON)) {
                violations.add(new Violation(Rule.BOX_OUTSIDE, box.id(), "box leaves "
                        + (box.parent() >= 0 ? boxes.get(box.parent()).id() : "screen"), rect,
                        parent));
            }
        }
        for (int first = 0; first < boxes.size(); first++) {
            UiLayoutFrame.Box a = boxes.get(first);
            for (int second = first + 1; second < boxes.size(); second++) {
                UiLayoutFrame.Box b = boxes.get(second);
                if (a.parent() == b.parent() && a.solid() && b.solid()
                        && a.rect().overlaps(b.rect())) {
                    violations.add(new Violation(Rule.BOX_OVERLAP, a.id() + " / " + b.id(),
                            "solid sibling boxes overlap", a.rect(), b.rect()));
                }
            }
        }
        if (frame.unbalanced() != 0) {
            violations.add(new Violation(Rule.UNBALANCED_BOXES, "frame",
                    frame.unbalanced() + " box(es) not closed or closed twice", screen, null));
        }

        for (UiLayoutFrame.Control control : controls) {
            if (!control.visible()) {
                continue;
            }
            if (control.rect().width() > 0.0F && control.clip() == null
                    && !control.rect().within(screen, EPSILON)) {
                violations.add(new Violation(Rule.CONTROL_OFFSCREEN, control.uiId(),
                        "control leaves the screen", control.rect(), screen));
            }
            boolean truncated = control.truncated()
                    || ownedTruncation.getOrDefault(control, Boolean.FALSE);
            if (truncated && !covered(control)) {
                violations.add(new Violation(Rule.CONTROL_TRUNCATED_NO_TIP, control.uiId(),
                        "label '" + control.label() + "' is shortened without a full-text tooltip",
                        control.rect(), null));
            }
            if (!control.active() && !"CURRENT".equals(control.state())
                    && control.disabledReason().isBlank()) {
                violations.add(new Violation(Rule.CONTROL_DISABLED_NO_REASON, control.uiId(),
                        "disabled without a reason", control.rect(), null));
            }
        }
        return violations;
    }

    /** Whether the control offers its full label while hovered. */
    static boolean covered(UiLayoutFrame.Control control) {
        return control.tipCoversLabel() || covers(control.tooltip(), control.label());
    }

    /**
     * Whether {@code tooltip} contains {@code full} ignoring whitespace (tooltips wrap lines).
     * An empty {@code full} is covered by any non-empty tooltip.
     */
    public static boolean covers(String tooltip, String full) {
        String tip = squash(tooltip);
        if (tip.isEmpty()) {
            return false;
        }
        String text = squash(full);
        return text.isEmpty() || tip.contains(text);
    }

    /** CJK ideographs, kana and full-width forms (same ranges as the preview's font). */
    public static boolean containsCjk(String text) {
        if (text == null) {
            return false;
        }
        for (int offset = 0; offset < text.length(); ) {
            int codePoint = text.codePointAt(offset);
            if (codePoint >= 0x3001 && codePoint <= 0x30FF
                    || codePoint >= 0x3200 && codePoint <= 0x9FFF
                    || codePoint >= 0xF900 && codePoint <= 0xFAFF
                    || codePoint >= 0xFF01 && codePoint <= 0xFF5E) {
                return true;
            }
            offset += Character.charCount(codePoint);
        }
        return false;
    }

    /**
     * The control that drew the text: the control recorded next after it (widgets and their parts
     * report themselves right after drawing their own texts), provided its rectangle holds the
     * text's centre (its left edge for an empty text); otherwise {@code null}. Texts drawn later
     * on top of a control (tooltips, modal cards) are therefore never attributed to it.
     */
    static UiLayoutFrame.Control owner(List<UiLayoutFrame.Control> controls,
                                       UiLayoutFrame.Text text) {
        if (text.nextControl() < 0 || text.nextControl() >= controls.size()) {
            return null;
        }
        UiLayoutFrame.Control next = controls.get(text.nextControl());
        UiLayoutFrame.Rect rect = text.rect();
        float x = rect.width() > 0.0F ? rect.centerX() : rect.left();
        float y = rect.centerY();
        return next.visible() && next.rect().contains(x, y) ? next : null;
    }

    private static String squash(String text) {
        return text == null ? "" : text.replaceAll("\\s+", "");
    }

    private static String round(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
