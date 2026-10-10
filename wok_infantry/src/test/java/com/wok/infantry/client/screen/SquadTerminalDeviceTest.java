package com.wok.infantry.client.screen;

import com.wok.infantry.battle.MemberState;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The battle terminal's squad, class and deployment pages on the D2 device: the status-bar title
 * and identity rules of {@link SquadScreen}, the wide / narrow flips of the three painters on the
 * device's content (plan 4.8), and the page colours that must follow the frame's livery (status
 * tones, planned key-value inks, the downed word in {@code ACCENT_TEXT}).
 */
class SquadTerminalDeviceTest {
    /** Minecraft-like advances: space 4, ASCII 6, everything else (CJK, "›", "·") 9. */
    private static final ToIntFunction<String> WIDTH = text -> text.codePoints()
            .map(codePoint -> codePoint == ' ' ? 4 : codePoint < 0x80 ? 6 : 9).sum();
    private static final String CLOCK = "21:30";
    private static final String TITLE_ZH = "WOK步战 // 战斗终端";
    private static final String TITLE_EN = "WOK INFANTRY // BATTLE TERMINAL";
    private static final List<String> PAGES_ZH = List.of("小队", "兵种", "部署");
    private static final List<String> PAGES_EN = List.of("Squads", "Roles", "Deploy");
    /** {@code SquadBoardModel.identityCandidates} of a squad leader, longest first. */
    private static final List<String> IDENTITY = List.of(
            "学院军 · 千禧年研讨会机动部队 · 阿尔法小队 · 小队长",
            "学院军 · 阿尔法小队 · 小队长",
            "学院军 · 阿尔法 · 小队长",
            "学院军 · 阿尔法 · 队长",
            "学院军 · 阿尔法");

    @AfterEach
    void backToA() {
        TacticalPalette.A.apply();
        assertEquals(0, TacticalPalette.openScopes(), "a test left a palette scope open");
    }

    private static TacticalBoardChrome.StatusPlan status(int width, int height, String title,
                                                         String feedback, String identity) {
        UiRect bar = TacticalShellLayout.compute(width, height).status();
        return TacticalBoardChrome.planStatus(bar, title, feedback, identity, CLOCK, WIDTH);
    }

    /** The status bar's title room on a {@code width}×{@code height} layout. */
    private static int titleRoom(int width, int height) {
        return status(width, height, "", null, null).title().maxWidth();
    }

    private static String title(int width, int height, String full, String shortTitle,
                                List<String> pages) {
        return SquadScreen.useFullTitle(width, titleRoom(width, height), full, pages, WIDTH)
                ? full : shortTitle;
    }

    // ---- status-bar title ---------------------------------------------------------------------

    @ParameterizedTest(name = "{0}x{1}")
    @CsvSource({"320, 240, false", "427, 240, true", "480, 270, true", "480, 360, true",
            "640, 336, true", "960, 540, true"})
    void chineseTitleIsShortOnlyBelowFourHundredLikeThePreview(int width, int height,
                                                               boolean full) {
        assertEquals(full ? TITLE_ZH : "战斗终端",
                title(width, height, TITLE_ZH, "战斗终端", PAGES_ZH));
    }

    @Test
    void aLongTranslationFallsBackInsteadOfBeingCut() {
        // "WOK INFANTRY // BATTLE TERMINAL › Squads" would end in "…" at 480×360.
        assertEquals("BATTLE TERMINAL",
                title(480, 360, TITLE_EN, "BATTLE TERMINAL", PAGES_EN));
        assertEquals(TITLE_EN, title(960, 540, TITLE_EN, "BATTLE TERMINAL", PAGES_EN));
    }

    @Test
    void theChineseTitleIsNeverEllipsizedOnAnyPage() {
        for (int[] size : new int[][]{{320, 240}, {427, 240}, {480, 270}, {480, 360},
                {640, 336}, {640, 360}, {960, 540}}) {
            String chosen = title(size[0], size[1], TITLE_ZH, "战斗终端", PAGES_ZH);
            for (String page : PAGES_ZH) {
                TacticalBoardChrome.StatusPlan plan = status(size[0], size[1],
                        TacticalBoardChrome.statusTitle(chosen, page), null, null);
                assertFalse(plan.title().truncated(), size[0] + "x" + size[1] + " " + chosen
                        + " › " + page);
            }
        }
    }

    @Test
    void oneTitleForAllThreePages() {
        // A page name that only fits on its own must not make the title jump between pages.
        int room = WIDTH.applyAsInt(TacticalBoardChrome.statusTitle("TITLE", "AB"));
        assertTrue(SquadScreen.useFullTitle(640, room, "TITLE", List.of("AB", "CD"), WIDTH));
        assertFalse(SquadScreen.useFullTitle(640, room, "TITLE", List.of("AB", "CDE"), WIDTH));
        assertFalse(SquadScreen.useFullTitle(399, 1000, "TITLE", List.of("AB"), WIDTH));
    }

    // ---- status-bar identity ------------------------------------------------------------------

    @Test
    void pickIdentityTakesTheLongestThatFitsElseTheShortest() {
        List<String> candidates = List.of("AAAA", "AAA", "A");

        assertEquals("AAAA", SquadScreen.pickIdentity(candidates, 24, WIDTH));
        assertEquals("AAA", SquadScreen.pickIdentity(candidates, 23, WIDTH));
        assertEquals("A", SquadScreen.pickIdentity(candidates, 6, WIDTH));
        assertEquals("A", SquadScreen.pickIdentity(candidates, 2, WIDTH),
                "nothing fits: the shortest goes in and the shell shortens it");
        assertNull(SquadScreen.pickIdentity(List.<String>of(), 100, WIDTH));
        assertNull(SquadScreen.pickIdentity(null, 100, WIDTH));
    }

    @ParameterizedTest(name = "{0}x{1}")
    @CsvSource({"320, 240", "427, 240", "480, 360", "640, 336", "960, 540"})
    void theStatusBarDrawsTheChosenIdentityUnchanged(int width, int height) {
        String title = TacticalBoardChrome.statusTitle(
                title(width, height, TITLE_ZH, "战斗终端", PAGES_ZH), "小队");
        for (String feedback : new String[]{null, "已加入 阿尔法小队"}) {
            int room = status(width, height, title, feedback, null).identityRoom();
            String chosen = SquadScreen.pickIdentity(IDENTITY, room, WIDTH);
            TacticalBoardChrome.StatusPlan plan = status(width, height, title, feedback, chosen);

            assertEquals(room, plan.identityRoom(), "the identity does not move the room");
            if (WIDTH.applyAsInt(chosen) <= room) {
                assertEquals(chosen, plan.identity().text(),
                        width + "x" + height + " " + feedback);
                assertFalse(plan.identity().truncated());
            } else {
                // Not even the shortest fits (320×240 with a receipt): the shell shortens it.
                assertEquals(TacticalBoardChrome.fitIdentity(chosen, room, WIDTH).text(),
                        plan.identity().text());
            }
        }
    }

    @Test
    void identityUsesTheWholeStatusBarRoomNotTheOldHeaderCap() {
        // The old cap (30% of the header, minus the six tab widths) chose "学院军 · 阿尔法 · 队长"
        // at 640×336; the device's status bar has room for the formation as well.
        String title = TacticalBoardChrome.statusTitle(TITLE_ZH, "小队");
        int room = status(640, 336, title, null, null).identityRoom();

        assertEquals(IDENTITY.get(0), SquadScreen.pickIdentity(IDENTITY, room, WIDTH));
    }

    @Test
    void aReceiptPillShortensTheIdentity() {
        String title = TacticalBoardChrome.statusTitle("战斗终端", "小队");
        int quiet = status(320, 240, title, null, null).identityRoom();
        int busy = status(320, 240, title, "已加入 阿尔法小队", null).identityRoom();

        assertTrue(busy < quiet, "the pill takes room from the identity");
        int quietIndex = IDENTITY.indexOf(SquadScreen.pickIdentity(IDENTITY, quiet, WIDTH));
        int busyIndex = IDENTITY.indexOf(SquadScreen.pickIdentity(IDENTITY, busy, WIDTH));
        assertTrue(busyIndex > quietIndex, "a shorter candidate while the receipt shows");
    }

    // ---- wide / narrow flips (plan 4.8) -------------------------------------------------------

    @ParameterizedTest(name = "{0}x{1}")
    @CsvSource({
            // width, height, content width, squad wide, classes / deployment wide
            "320, 240, 296, false, false",
            "427, 240, 403, false, false",
            "480, 270, 456, false, true",
            "480, 360, 416, false, false",
            "640, 336, 576, false, true",
            "640, 360, 576, false, true",
            "960, 540, 846, true, true"})
    void pagesFlipOnTheDeviceContent(int width, int height, int contentWidth, boolean squadWide,
                                     boolean twoColumns) {
        UiRect content = TacticalShellLayout.compute(width, height).content();

        assertEquals(contentWidth, content.width());
        assertEquals(squadWide, SquadPagePainter.wide(content), "squad strip and right column");
        assertEquals(twoColumns, ClassPagePainter.wide(content), "class list beside the card");
        assertEquals(twoColumns, DeploymentPagePainter.wide(content), "points beside the status");
    }

    @Test
    void strictTiersKeepTheirContentHeights() {
        assertEquals(192, TacticalShellLayout.compute(320, 240).content().height());
        assertEquals(260, TacticalShellLayout.compute(640, 336).content().height());
        assertEquals(284, TacticalShellLayout.compute(480, 360).content().height());
    }

    // ---- colours of the frame's livery --------------------------------------------------------

    @Test
    void classStatusTonesFollowTheReasonNotAColour() {
        assertEquals(ClassPagePainter.StatusTone.PICK,
                ClassPagePainter.StatusTone.of(true, null));
        assertEquals(ClassPagePainter.StatusTone.CURRENT, ClassPagePainter.StatusTone.of(false,
                SquadBoardModel.ReasonCode.CLASS_CURRENT));
        assertEquals(ClassPagePainter.StatusTone.FULL, ClassPagePainter.StatusTone.of(false,
                SquadBoardModel.ReasonCode.CLASS_FULL));
        for (SquadBoardModel.ReasonCode code : new SquadBoardModel.ReasonCode[]{
                SquadBoardModel.ReasonCode.CLASS_NOT_OPEN, SquadBoardModel.ReasonCode.CLASS_NO_SQUAD,
                SquadBoardModel.ReasonCode.CLASS_ACTIVE, SquadBoardModel.ReasonCode.PENDING, null}) {
            assertEquals(ClassPagePainter.StatusTone.OFF,
                    ClassPagePainter.StatusTone.of(false, code), String.valueOf(code));
        }
    }

    @Test
    void classStatusTonesResolveInThePaletteBeingDrawn() {
        for (TacticalPalette palette : List.of(TacticalPalette.ACADEMY, TacticalPalette.CAESAR,
                TacticalPalette.NEUTRAL)) {
            try (TacticalPalette.Applied ignored = TacticalPalette.push(palette)) {
                assertEquals(palette.get(PaletteToken.SUCCESS_B),
                        ClassPagePainter.StatusTone.PICK.color(), palette.name());
                assertEquals(palette.get(PaletteToken.ON_SELECT),
                        ClassPagePainter.StatusTone.CURRENT.color(), palette.name());
                assertEquals(palette.get(PaletteToken.DANGER_B),
                        ClassPagePainter.StatusTone.FULL.color(), palette.name());
                assertEquals(palette.get(PaletteToken.FAINT),
                        ClassPagePainter.StatusTone.OFF.color(), palette.name());
            }
        }
        assertEquals(TacticalPalette.A.get(PaletteToken.FAINT),
                ClassPagePainter.StatusTone.OFF.color(), "back to A outside the scope");
    }

    @Test
    void keyValueRowsPlannedBeforeTheFrameStillDrawInItsLivery() {
        // FormationVotePanel.layout plans its info rows in init(), outside any palette scope.
        FormationVotePanel.Info planned = new FormationVotePanel.Info(Component.literal("截止"),
                Component.literal("管理员关闭投票时"), SquadBoardBlocks.Ink.MUTED);
        FormationVotePanel.Info active = new FormationVotePanel.Info(Component.literal("部署"),
                Component.literal("作战中"), SquadBoardBlocks.Ink.SUCCESS);

        try (TacticalPalette.Applied ignored = TacticalPalette.push(TacticalPalette.CAESAR)) {
            assertEquals(TacticalPalette.CAESAR.get(PaletteToken.MUTED), planned.color());
            assertEquals(TacticalPalette.CAESAR.get(PaletteToken.SUCCESS), active.color());
        }
        assertEquals(TacticalPalette.A.get(PaletteToken.MUTED), planned.color());
        assertEquals(SquadBoardBlocks.Ink.TEXT, new FormationVotePanel.Info(Component.empty(),
                Component.empty(), null).ink(), "no ink is plain text");
    }

    @Test
    void theDownedWordUsesAccentTextWhileItsDotKeepsAccent() {
        try (TacticalPalette.Applied ignored = TacticalPalette.push(TacticalPalette.ACADEMY)) {
            assertEquals(TacticalPalette.ACADEMY.get(PaletteToken.ACCENT_TEXT),
                    SquadPagePainter.boardTextColor(MemberState.DOWNED));
            assertEquals(TacticalPalette.ACADEMY.get(PaletteToken.ACCENT),
                    SquadPagePainter.boardColor(MemberState.DOWNED));
            assertEquals(TacticalPalette.ACADEMY.get(PaletteToken.DANGER),
                    SquadPagePainter.boardTextColor(MemberState.DEAD));
            assertEquals(TacticalPalette.ACADEMY.get(PaletteToken.SUCCESS),
                    SquadPagePainter.boardTextColor(MemberState.DEPLOYED));
            assertEquals(TacticalPalette.ACADEMY.get(PaletteToken.MUTED),
                    SquadPagePainter.boardTextColor(null));
        }
    }
}
