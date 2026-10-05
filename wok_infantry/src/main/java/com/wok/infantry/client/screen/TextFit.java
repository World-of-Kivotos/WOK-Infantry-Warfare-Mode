package com.wok.infantry.client.screen;

import com.wok.infantry.client.ui.probe.UiLayoutProbe;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

/**
 * Width-bounded text for every WOK步战 surface: text that does not fit ends in an ellipsis
 * ("…") instead of being hard-cut or scrolled, and is always drawn without a shadow.
 *
 * <p>The decisions live in pure {@code *Plain} methods that take any width function, so they
 * can be unit-tested without a Minecraft font; the {@link Font} overloads only adapt them.
 * Callers that truncate should offer the full text elsewhere (for buttons: a tooltip).
 */
public final class TextFit {
    public static final String ELLIPSIS = "…";
    /** Closing punctuation that never starts a wrapped line (避头尾). */
    static final String NO_LINE_START = "。，、；：？！）」』》〉】’”…·%,.;:?!)]}";
    /** Opening brackets and quotes never end a line (they go down with the text they open). */
    static final String NO_LINE_END = "（「『《〈【‘“([{";

    public enum Align {
        LEFT,
        CENTER,
        RIGHT
    }

    /** Fitted text ready to draw, its drawn width and whether anything was cut off. */
    public record Fitted(FormattedCharSequence text, int width, boolean truncated) {
        static final Fitted EMPTY = new Fitted(FormattedCharSequence.EMPTY, 0, false);
    }

    /** Pure counterpart of {@link Fitted}. */
    public record Plain(String text, int width, boolean truncated) {
    }

    private TextFit() {
    }

    // ---- pure core --------------------------------------------------------------------------------

    /** Returns {@code text} unchanged when it fits, otherwise its longest fitting prefix plus "…". */
    public static Plain fitPlain(String text, int maxWidth, ToIntFunction<String> width) {
        String safe = text == null ? "" : text;
        int full = width.applyAsInt(safe);
        if (full <= maxWidth || safe.isEmpty()) {
            return new Plain(safe, full, false);
        }
        int ellipsisWidth = width.applyAsInt(ELLIPSIS);
        if (maxWidth < ellipsisWidth) {
            return new Plain("", 0, true);
        }
        String head = stripTrailingWhitespace(prefixByWidth(safe, maxWidth - ellipsisWidth, width));
        String fitted = head + ELLIPSIS;
        return new Plain(fitted, width.applyAsInt(fitted), true);
    }

    /**
     * Two-part label such as "支援名称  12秒": the trailing status stays complete and only the
     * name is shortened. When even the status alone does not fit, the status is ellipsized.
     */
    public static Plain fitPlainWithTrailing(String name, String trailing, int maxWidth,
                                             ToIntFunction<String> width) {
        String safeName = name == null ? "" : name;
        String safeTrailing = trailing == null ? "" : trailing;
        String whole = safeName + safeTrailing;
        int wholeWidth = width.applyAsInt(whole);
        if (wholeWidth <= maxWidth) {
            return new Plain(whole, wholeWidth, false);
        }
        String status = safeTrailing.strip();
        int trailingWidth = width.applyAsInt(safeTrailing);
        if (trailingWidth > maxWidth || safeName.isEmpty()) {
            Plain alone = fitPlain(status, maxWidth, width);
            return new Plain(alone.text(), alone.width(), true);
        }
        Plain head = fitPlain(safeName, maxWidth - trailingWidth, width);
        if (head.text().isEmpty()) {
            Plain alone = fitPlain(status, maxWidth, width);
            return new Plain(alone.text(), alone.width(), true);
        }
        String fitted = head.text() + safeTrailing;
        return new Plain(fitted, width.applyAsInt(fitted), true);
    }

    /**
     * Wraps latin text at spaces and CJK text between any two characters, never starting a line
     * with closing punctuation and hard-breaking words that are wider than a line. With
     * {@code maxLines > 0} the result is limited to that many lines and the last kept line ends
     * in "…". Explicit {@code \n} always starts a new line.
     */
    public static List<String> wrapPlain(String text, int maxWidth, int maxLines,
                                         ToIntFunction<String> width) {
        List<String> lines = new ArrayList<>();
        String safe = text == null ? "" : text;
        int lineWidth = Math.max(1, maxWidth);
        for (String paragraph : safe.split("\n", -1)) {
            wrapParagraph(paragraph, lineWidth, width, lines);
        }
        if (maxLines > 0 && lines.size() > maxLines) {
            List<String> kept = new ArrayList<>(lines.subList(0, maxLines));
            int last = kept.size() - 1;
            kept.set(last, ellipsizeForced(kept.get(last), lineWidth, width));
            return kept;
        }
        return lines;
    }

    /** A wrapped paragraph: the candidate that was chosen and its lines. */
    public record Wrapped(String text, List<String> lines) {
        public Wrapped {
            text = text == null ? "" : text;
            lines = List.copyOf(lines == null ? List.of() : lines);
        }
    }

    /**
     * Picks the first of {@code candidates} (longest first) that wraps into at most
     * {@code maxLines} lines (0 = no limit) without an orphan, i.e. a last line of only one or two
     * characters ("…发放配" / "装"); failing that the first that fits the line limit; failing
     * that the last candidate cut to {@code maxLines} with "…" (preview {@code wrapBest}).
     * Punctuation and spaces are not counted as characters.
     */
    public static Wrapped wrapBestPlain(List<String> candidates, int maxWidth, int maxLines,
                                        ToIntFunction<String> width) {
        if (candidates == null || candidates.isEmpty()) {
            return new Wrapped("", List.of());
        }
        List<List<String>> wrapped = new ArrayList<>(candidates.size());
        for (String candidate : candidates) {
            wrapped.add(wrapPlain(candidate, maxWidth, 0, width));
        }
        for (int index = 0; index < candidates.size(); index++) {
            List<String> lines = wrapped.get(index);
            if ((maxLines <= 0 || lines.size() <= maxLines) && !orphaned(lines)) {
                return new Wrapped(candidates.get(index), lines);
            }
        }
        for (int index = 0; index < candidates.size(); index++) {
            List<String> lines = wrapped.get(index);
            if (maxLines <= 0 || lines.size() <= maxLines) {
                return new Wrapped(candidates.get(index), lines);
            }
        }
        String last = candidates.get(candidates.size() - 1);
        return new Wrapped(last, wrapPlain(last, maxWidth, Math.max(1, maxLines), width));
    }

    /** {@link #wrapBestPlain} for components, measured with {@code font}. */
    public static Wrapped wrapBest(Font font, List<? extends Component> candidates, int maxWidth,
                                   int maxLines) {
        List<String> plain = new ArrayList<>(candidates == null ? 0 : candidates.size());
        if (candidates != null) {
            for (Component candidate : candidates) {
                plain.add(candidate == null ? "" : candidate.getString());
            }
        }
        return wrapBestPlain(plain, maxWidth, maxLines, font::width);
    }

    /** Whether a wrapped paragraph ends in a line of one or two counted characters. */
    static boolean orphaned(List<String> lines) {
        if (lines.size() < 2) {
            return false;
        }
        String last = lines.get(lines.size() - 1);
        int counted = 0;
        for (int offset = 0; offset < last.length(); ) {
            int codePoint = last.codePointAt(offset);
            if (!Character.isWhitespace(codePoint) && NOT_COUNTED.indexOf(codePoint) < 0) {
                counted++;
            }
            offset += Character.charCount(codePoint);
        }
        return counted <= 2;
    }

    /** Punctuation that does not count as an orphaned character (preview {@code NOT_COUNTED}). */
    static final String NOT_COUNTED = "。，、；：！？…（）()·,.;:!?";

    /** Left edge of a text of {@code textWidth} placed in [x, x + maxWidth). */
    public static int alignedX(int x, int maxWidth, int textWidth, Align align) {
        int room = Math.max(0, maxWidth);
        return switch (align == null ? Align.LEFT : align) {
            case LEFT -> x;
            case CENTER -> x + (room - textWidth) / 2;
            case RIGHT -> x + room - textWidth;
        };
    }

    // ---- Minecraft font adapters --------------------------------------------------------------------

    public static Fitted fit(Font font, String text, int maxWidth) {
        Plain plain = fitPlain(text, maxWidth, font::width);
        return new Fitted(Component.literal(plain.text()).getVisualOrderText(),
                plain.width(), plain.truncated());
    }

    /** Fits a styled component; the kept prefix keeps its styles. */
    public static Fitted fit(Font font, Component text, int maxWidth) {
        if (text == null) {
            return Fitted.EMPTY;
        }
        int full = font.width(text);
        if (full <= maxWidth || full == 0) {
            return new Fitted(text.getVisualOrderText(), full, false);
        }
        int ellipsisWidth = font.width(ELLIPSIS);
        if (maxWidth < ellipsisWidth) {
            return new Fitted(FormattedCharSequence.EMPTY, 0, true);
        }
        FormattedText head = font.substrByWidth(text, maxWidth - ellipsisWidth);
        String headText = head.getString();
        String trimmed = stripTrailingWhitespace(headText);
        if (trimmed.length() != headText.length()) {
            // Cut by length, not by re-measuring the plain string: bold parts are wider than
            // their plain text, so a width-based re-cut would drop visible characters.
            head = prefixByLength(head, trimmed.length());
        }
        FormattedCharSequence sequence = Language.getInstance().getVisualOrder(
                FormattedText.composite(head, FormattedText.of(ELLIPSIS)));
        return new Fitted(sequence, font.width(sequence), true);
    }

    public static Fitted fitWithTrailing(Font font, String name, String trailing, int maxWidth) {
        Plain plain = fitPlainWithTrailing(name, trailing, maxWidth, font::width);
        return new Fitted(Component.literal(plain.text()).getVisualOrderText(),
                plain.width(), plain.truncated());
    }

    public static Fitted fitWithTrailing(Font font, Component name, Component trailing,
                                         int maxWidth) {
        return fitWithTrailing(font, name == null ? "" : name.getString(),
                trailing == null ? "" : trailing.getString(), maxWidth);
    }

    public static List<String> wrap(Font font, String text, int maxWidth, int maxLines) {
        return wrapPlain(text, maxWidth, maxLines, font::width);
    }

    /** Draws {@code text} fitted into [x, x + maxWidth) without a shadow. */
    public static Fitted draw(GuiGraphics graphics, Font font, Component text, int x, int y,
                              int maxWidth, int color, Align align) {
        return drawProbed(graphics, font, fit(font, text, maxWidth), text, x, y, maxWidth, color,
                align);
    }

    public static Fitted draw(GuiGraphics graphics, Font font, String text, int x, int y,
                              int maxWidth, int color, Align align) {
        return drawProbed(graphics, font, fit(font, text, maxWidth), text, x, y, maxWidth, color,
                align);
    }

    /** Draws an already fitted text without a shadow. */
    public static Fitted drawFitted(GuiGraphics graphics, Font font, Fitted fitted, int x, int y,
                                    int maxWidth, int color, Align align) {
        return drawProbed(graphics, font, fitted, null, x, y, maxWidth, color, align);
    }

    /**
     * Draws and reports the text to the layout probe ({@code full} is the unfitted text when the
     * caller has it); the report is a no-op unless the uiTest probe is recording.
     */
    private static Fitted drawProbed(GuiGraphics graphics, Font font, Fitted fitted, Object full,
                                     int x, int y, int maxWidth, int color, Align align) {
        int drawX = alignedX(x, maxWidth, fitted.width(), align);
        if (fitted.width() > 0) {
            graphics.drawString(font, fitted.text(), drawX, y, color, false);
        }
        if (UiLayoutProbe.recording()) {
            UiLayoutProbe.fittedText(graphics, fitted.text(), full, drawX, y, fitted.width(),
                    fitted.truncated());
        }
        return fitted;
    }

    // ---- helpers -------------------------------------------------------------------------------------

    /** Longest prefix (whole code points) whose width is at most {@code maxWidth}. */
    static String prefixByWidth(String text, int maxWidth, ToIntFunction<String> width) {
        if (maxWidth <= 0 || text.isEmpty()) {
            return "";
        }
        int[] ends = codePointEnds(text);
        int low = 0;
        int high = ends.length;
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            if (width.applyAsInt(text.substring(0, ends[middle - 1])) <= maxWidth) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low == 0 ? "" : text.substring(0, ends[low - 1]);
    }

    /** The first {@code length} chars of {@code text}; every kept part keeps its own style. */
    static FormattedText prefixByLength(FormattedText text, int length) {
        List<FormattedText> parts = new ArrayList<>();
        int[] remaining = {Math.max(0, length)};
        text.visit((style, content) -> {
            if (remaining[0] <= 0) {
                return FormattedText.STOP_ITERATION;
            }
            String piece = content.length() <= remaining[0]
                    ? content : content.substring(0, remaining[0]);
            remaining[0] -= piece.length();
            if (!piece.isEmpty()) {
                parts.add(FormattedText.of(piece, style));
            }
            if (remaining[0] <= 0) {
                return FormattedText.STOP_ITERATION;
            }
            return Optional.empty();
        }, Style.EMPTY);
        return FormattedText.composite(parts);
    }

    private static int[] codePointEnds(String text) {
        int[] ends = new int[text.codePointCount(0, text.length())];
        int index = 0;
        for (int offset = 0; offset < text.length(); ) {
            offset += Character.charCount(text.codePointAt(offset));
            ends[index++] = offset;
        }
        return ends;
    }

    private static String stripTrailingWhitespace(String text) {
        int end = text.length();
        while (end > 0 && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(0, end);
    }

    private static String ellipsizeForced(String line, int maxWidth, ToIntFunction<String> width) {
        String body = stripTrailingWhitespace(line);
        if (width.applyAsInt(body + ELLIPSIS) <= maxWidth) {
            return body + ELLIPSIS;
        }
        String head = stripTrailingWhitespace(prefixByWidth(body,
                maxWidth - width.applyAsInt(ELLIPSIS), width));
        return head + ELLIPSIS;
    }

    private static void wrapParagraph(String paragraph, int maxWidth,
                                      ToIntFunction<String> width, List<String> lines) {
        StringBuilder line = new StringBuilder();
        for (String token : tokens(paragraph)) {
            if (token.isBlank()) {
                if (line.length() > 0) {
                    line.append(token);
                }
                continue;
            }
            String piece = token;
            if (width.applyAsInt(line + piece) > maxWidth && !line.toString().isBlank()) {
                String done = stripTrailingWhitespace(line.toString());
                line.setLength(0);
                if ((startsWithClosingPunctuation(piece) || endsWithOpeningPunctuation(done))
                        && done.codePointCount(0, done.length()) > 1) {
                    // 避头尾: carry the previous character down together with the closing
                    // punctuation, or an opening bracket down to the text it opens.
                    int cut = done.offsetByCodePoints(done.length(), -1);
                    lines.add(stripTrailingWhitespace(done.substring(0, cut)));
                    line.append(done.substring(cut));
                } else {
                    lines.add(done);
                }
            }
            while (width.applyAsInt(line + piece) > maxWidth) {
                int room = maxWidth - width.applyAsInt(line.toString());
                String head = prefixByWidth(piece, room, width);
                if (head.isEmpty()) {
                    if (line.length() > 0) {
                        lines.add(stripTrailingWhitespace(line.toString()));
                        line.setLength(0);
                        continue;
                    }
                    // A single code point wider than the line: emit it on its own line.
                    head = piece.substring(0, Character.charCount(piece.codePointAt(0)));
                }
                lines.add(line + head);
                line.setLength(0);
                piece = piece.substring(head.length());
            }
            line.append(piece);
        }
        lines.add(stripTrailingWhitespace(line.toString()));
    }

    private static boolean startsWithClosingPunctuation(String token) {
        return !token.isEmpty() && NO_LINE_START.indexOf(token.codePointAt(0)) >= 0;
    }

    private static boolean endsWithOpeningPunctuation(String line) {
        return !line.isEmpty()
                && NO_LINE_END.indexOf(line.codePointBefore(line.length())) >= 0;
    }

    /** Splits into single CJK characters, runs of other non-space characters and space runs. */
    private static List<String> tokens(String paragraph) {
        List<String> tokens = new ArrayList<>();
        int offset = 0;
        while (offset < paragraph.length()) {
            int codePoint = paragraph.codePointAt(offset);
            int next = offset + Character.charCount(codePoint);
            if (isBreakAnywhere(codePoint)) {
                tokens.add(paragraph.substring(offset, next));
                offset = next;
                continue;
            }
            boolean space = Character.isWhitespace(codePoint);
            int end = next;
            while (end < paragraph.length()) {
                int following = paragraph.codePointAt(end);
                if (isBreakAnywhere(following) || Character.isWhitespace(following) != space) {
                    break;
                }
                end += Character.charCount(following);
            }
            tokens.add(paragraph.substring(offset, end));
            offset = end;
        }
        return tokens;
    }

    /**
     * CJK ideographs, kana, CJK punctuation and full-width forms may break on either side. Curly
     * quotes, "…" and "·" are deliberately not in this set (as in the preview's {@code font.wrap}):
     * inside a latin word ("don’t") they stay part of the word, and next to CJK text they still
     * form their own token, so 避头尾 keeps them off the start of a line.
     */
    private static boolean isBreakAnywhere(int codePoint) {
        return (codePoint >= 0x2E80 && codePoint <= 0x9FFF)
                || (codePoint >= 0xF900 && codePoint <= 0xFAFF)
                || (codePoint >= 0xFF00 && codePoint <= 0xFFEF);
    }
}
