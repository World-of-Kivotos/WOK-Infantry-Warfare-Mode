package com.wok.infantry.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextFitTest {
    /** Minecraft-like advances: space 4, ASCII 6, "…" 8, everything else (CJK) 9. */
    private static final ToIntFunction<String> WIDTH = text -> text.codePoints()
            .map(TextFitTest::advance).sum();

    private static int advance(int codePoint) {
        if (codePoint == ' ') {
            return 4;
        }
        if (codePoint == 0x2026) {
            return 8;
        }
        return codePoint < 0x80 ? 6 : 9;
    }

    @Test
    void textThatFitsIsUnchanged() {
        TextFit.Plain fitted = TextFit.fitPlain("ABC", 18, WIDTH);

        assertEquals("ABC", fitted.text());
        assertEquals(18, fitted.width());
        assertFalse(fitted.truncated());
    }

    @Test
    void overlongTextEndsInEllipsisWithinWidth() {
        TextFit.Plain fitted = TextFit.fitPlain("ABCDEFGH", 30, WIDTH);

        assertEquals("ABC…", fitted.text());
        assertEquals(26, fitted.width());
        assertTrue(fitted.truncated());
    }

    @Test
    void chineseTextIsCutBetweenCharacters() {
        TextFit.Plain fitted = TextFit.fitPlain("编制投票进行中", 40, WIDTH);

        assertEquals("编制投…", fitted.text());
        assertTrue(fitted.width() <= 40);
        assertTrue(fitted.truncated());
    }

    @Test
    void widthTooSmallForEllipsisDrawsNothing() {
        TextFit.Plain fitted = TextFit.fitPlain("ABC", 5, WIDTH);

        assertEquals("", fitted.text());
        assertEquals(0, fitted.width());
        assertTrue(fitted.truncated());
    }

    @Test
    void emptyTextIsNeverReportedAsTruncated() {
        TextFit.Plain fitted = TextFit.fitPlain("", -4, WIDTH);

        assertEquals("", fitted.text());
        assertFalse(fitted.truncated(), "an empty label needs no full-text tooltip");
    }

    @Test
    void neverSplitsSurrogatePairs() {
        String clef = new String(Character.toChars(0x1D11E));
        TextFit.Plain fitted = TextFit.fitPlain("A" + clef + "B" + clef + "C", 25, WIDTH);

        assertEquals("A" + clef + "…", fitted.text());
    }

    @Test
    void trailingSpacesBeforeEllipsisAreDropped() {
        assertEquals("AB…", TextFit.fitPlain("AB  CD", 26, WIDTH).text());
    }

    @Test
    void leadingSpacesReservedForIconsAreKept() {
        assertEquals("   Hostile", TextFit.fitPlain("   Hostile", 100, WIDTH).text());
    }

    @Test
    void trailingStatusStaysCompleteWhileNameIsShortened() {
        // name 58px + status 26px = 84px; only the name gives way.
        TextFit.Plain fitted = TextFit.fitPlainWithTrailing("F-16C JDAM", "  12s", 66, WIDTH);

        assertEquals("F-16C…  12s", fitted.text());
        assertEquals(64, fitted.width());
        assertTrue(fitted.truncated());
    }

    @Test
    void twoPartLabelThatFitsIsUnchanged() {
        TextFit.Plain fitted = TextFit.fitPlainWithTrailing("F-16C", "  12s", 100, WIDTH);

        assertEquals("F-16C  12s", fitted.text());
        assertFalse(fitted.truncated());
    }

    @Test
    void statusAloneIsEllipsizedWhenEvenItDoesNotFit() {
        TextFit.Plain fitted = TextFit.fitPlainWithTrailing("F-16C JDAM", "  12s", 15, WIDTH);

        assertEquals("1…", fitted.text());
        assertTrue(fitted.width() <= 15);
        assertTrue(fitted.truncated());
    }

    @Test
    void chineseWrapsBetweenAnyCharacters() {
        assertEquals(List.of("一二三", "四五六"),
                TextFit.wrapPlain("一二三四五六", 27, 0, WIDTH));
    }

    @Test
    void latinWrapsAtSpaces() {
        assertEquals(List.of("alpha beta", "gamma"),
                TextFit.wrapPlain("alpha beta gamma", 60, 0, WIDTH));
    }

    @Test
    void closingPunctuationNeverStartsALine() {
        List<String> lines = TextFit.wrapPlain("一二三，四", 27, 0, WIDTH);

        assertEquals(List.of("一二", "三，四"), lines);
        lines.forEach(line -> assertFalse(line.startsWith("，"), line));
    }

    @Test
    void curlyApostropheStaysInsideItsLatinWord() {
        // Splitting at "’" would carry "n" down and print "we do" / "n’t" (review fix).
        assertEquals(List.of("we", "don’t", "stop"),
                TextFit.wrapPlain("we don’t stop", 40, 0, WIDTH));
    }

    @Test
    void openingBracketNeverEndsALine() {
        // "共享编制（" / "千禧年…）" read as a dangling bracket (squad terminal rules, 640×336).
        assertEquals(List.of("一二", "（三四"), TextFit.wrapPlain("一二（三四", 27, 0, WIDTH));
        assertEquals(List.of("一二", "“三”"), TextFit.wrapPlain("一二“三”", 27, 0, WIDTH));
    }

    @Test
    void closingQuoteAfterChineseStillNeverStartsALine() {
        assertEquals(List.of("一二", "三”四"), TextFit.wrapPlain("一二三”四", 27, 0, WIDTH));
    }

    @Test
    void closingBracketAlreadyOnTheLineGoesDownWithTheNextMark() {
        // "（已有小队解散），部署页…" at formation.admin-caesar@320x240 started a line with "），".
        List<String> lines = TextFit.wrapPlain("一（二三），四", 45, 0, WIDTH);

        assertEquals(List.of("一（二", "三），四"), lines);
        lines.forEach(line -> assertFalse(TextFit.NO_LINE_START.indexOf(line.codePointAt(0)) >= 0,
                line));
    }

    @Test
    void aLatinWordGoesDownWholeWithItsClosingMark() {
        assertEquals(List.of("一", "Caesar），"), TextFit.wrapPlain("一Caesar），", 60, 0, WIDTH));
        assertEquals(List.of("ab", "cd，"), TextFit.wrapPlain("ab cd，", 30, 0, WIDTH));
    }

    @Test
    void ellipsisRunAfterChineseNeverStartsALine() {
        assertEquals(List.of("一二", "三……"), TextFit.wrapPlain("一二三……", 27, 0, WIDTH));
    }

    @Test
    void lengthPrefixKeepsEveryPartsStyle() {
        Component text = Component.empty()
                .append(Component.literal("AB ").withStyle(ChatFormatting.BOLD))
                .append(Component.literal("CD").withStyle(ChatFormatting.ITALIC));
        List<String> parts = new ArrayList<>();
        TextFit.prefixByLength(text, 4).visit((style, content) -> {
            parts.add((style.isBold() ? "b:" : "") + (style.isItalic() ? "i:" : "") + content);
            return Optional.empty();
        }, Style.EMPTY);

        assertEquals(List.of("b:AB ", "i:C"), parts);
        assertEquals("AB", TextFit.prefixByLength(text, 2).getString());
        assertEquals("", TextFit.prefixByLength(text, 0).getString());
        assertEquals("AB CD", TextFit.prefixByLength(text, 99).getString());
    }

    @Test
    void wordsWiderThanALineAreHardBroken() {
        assertEquals(List.of("ABCD", "EFGH", "IJ"),
                TextFit.wrapPlain("ABCDEFGHIJ", 25, 0, WIDTH));
    }

    @Test
    void lineLimitEndsTheLastKeptLineInEllipsis() {
        List<String> lines = TextFit.wrapPlain("一二三四五六七八九", 27, 2, WIDTH);

        assertEquals(List.of("一二三", "四五…"), lines);
        assertTrue(WIDTH.applyAsInt(lines.get(1)) <= 27);
    }

    @Test
    void explicitNewlinesStartNewLines() {
        assertEquals(List.of("ab", "cd"), TextFit.wrapPlain("ab\ncd", 100, 0, WIDTH));
    }

    @Test
    void alignmentPlacesTextInsideTheBox() {
        assertEquals(10, TextFit.alignedX(10, 100, 40, TextFit.Align.LEFT));
        assertEquals(40, TextFit.alignedX(10, 100, 40, TextFit.Align.CENTER));
        assertEquals(70, TextFit.alignedX(10, 100, 40, TextFit.Align.RIGHT));
    }

    @Test
    void wrapBestSkipsAVariantThatLeavesAnOrphan() {
        // 7 CJK characters per 63px line: the long form ends with "装" alone on its last line.
        String orphan = "部署时清空随身物品按兵种发放配装";
        String clean = "部署时按兵种发放配装";
        assertTrue(TextFit.orphaned(TextFit.wrapPlain(orphan, 63, 0, WIDTH)));
        TextFit.Wrapped wrapped = TextFit.wrapBestPlain(List.of(orphan, clean), 63, 0, WIDTH);

        assertEquals(clean, wrapped.text());
        assertFalse(TextFit.orphaned(wrapped.lines()));
    }

    @Test
    void wrapBestKeepsTheLineLimitAndFallsBackToTheShortestForm() {
        String longForm = "一二三四五六七八九十一二三四五六七八九十";
        String shortForm = "一二三四五六七";
        assertEquals(shortForm, TextFit.wrapBestPlain(List.of(longForm, shortForm), 63, 1, WIDTH)
                .text());
        TextFit.Wrapped cut = TextFit.wrapBestPlain(List.of(longForm, longForm + "十"), 63, 2,
                WIDTH);
        assertEquals(2, cut.lines().size());
        assertTrue(cut.lines().get(1).endsWith(TextFit.ELLIPSIS));
        // Punctuation does not count: "配装。" alone on a line is still an orphan.
        assertTrue(TextFit.orphaned(List.of("一二三四五六七", "配装。")));
        assertFalse(TextFit.orphaned(List.of("一二三四五六七", "发放配装")));
        assertEquals("", TextFit.wrapBestPlain(List.of(), 63, 0, WIDTH).text());
    }
}
