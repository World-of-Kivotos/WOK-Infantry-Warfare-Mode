package com.wok.infantry.client.screen;

import com.wok.infantry.client.screen.TacticalBoardChrome.BezelPlan;
import com.wok.infantry.client.screen.TacticalBoardChrome.LinkState;
import com.wok.infantry.client.screen.TacticalBoardChrome.StatusPlan;
import com.wok.infantry.client.screen.TacticalBoardChrome.StatusText;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure status-bar and bezel plans of the D2 device ({@code 17-device.js statusBar, pageKeys}). */
class TacticalBoardChromeLayoutTest {
    /** A fixed-pitch stand-in for the game font: 6px per code point. */
    private static final ToIntFunction<String> WIDTH = text -> text.codePointCount(0,
            text.length()) * 6;
    private static final String CLOCK = "21:30";

    private static StatusPlan plan(int width, int height, String title, String feedback,
                                   String identity) {
        UiRect bar = TacticalShellLayout.compute(width, height).status();
        return TacticalBoardChrome.planStatus(bar, title, feedback, identity, CLOCK, WIDTH);
    }

    private static UiRect box(StatusPlan plan, StatusText text) {
        return new UiRect(text.x(), plan.textY(), text.right(), plan.textY() + 8);
    }

    @Test
    void rightEndCarriesClockBatteryAndSignal() {
        StatusPlan plan = plan(480, 360, "T", null, null);
        UiRect bar = plan.bar();

        assertEquals(UiRect.of(24, 18, 456, 29), bar);
        assertEquals(19, plan.textY(), "an 11px bar centres the 9px row one pixel down");
        assertEquals(bar.right() - 3 - 30, plan.clock().x());
        assertEquals(CLOCK, plan.clock().text());
        assertEquals(plan.clock().x() - 5 - 11, plan.batteryX());
        assertEquals(plan.batteryX() - 4 - 9, plan.signalX());
        assertEquals(UiRect.of(27, 20, 29, 26), plan.stripe(), "faction stripe 3px in");
        assertEquals(32, plan.title().x());
    }

    @ParameterizedTest(name = "{0}x{1}")
    @CsvSource({"320, 240, 9", "480, 360, 19", "960, 540, 29"})
    void textRowFollowsTheBarHeight(int width, int height, int textY) {
        assertEquals(textY, plan(width, height, "T", null, null).textY());
    }

    @Test
    void titleTakesAtMostFiftyFivePercentOfTheFreeRoom() {
        String longTitle = "X".repeat(80);
        StatusPlan plan = plan(480, 360, longTitle, null, null);
        int free = plan.signalX() - 7 - plan.title().x();

        assertEquals((int) Math.floor(free * 0.55D), plan.title().maxWidth());
        assertTrue(plan.title().truncated());
        assertTrue(plan.title().text().endsWith(TextFit.ELLIPSIS));
        assertTrue(plan.title().width() <= plan.title().maxWidth());
        assertEquals(longTitle, plan.title().full());

        StatusPlan whole = plan(480, 360, "Terminal > Deploy", null, null);
        assertFalse(whole.title().truncated());
        assertEquals(17 * 6, whole.title().width());
    }

    @Test
    void receiptPillHugsItsTextRightOfTheTitle() {
        StatusPlan plan = plan(960, 540, "Terminal > Squads", "Loadout saved", null);
        UiRect pill = plan.pill();

        assertEquals(13 * 6 + 9, pill.width(), "text width + 9");
        assertEquals(plan.signalX() - 7, pill.right(), "right-aligned before the signal");
        assertEquals(plan.bar().top() + 1, pill.top());
        assertEquals(plan.bar().bottom() - 2, pill.bottom());
        assertEquals(pill.left() + 5, plan.feedback().x());
        assertEquals(pill.width() - 7, plan.feedback().maxWidth());
        assertFalse(plan.feedback().truncated());
        assertEquals(UiRect.of(pill.left(), pill.top(), pill.left() + 2, pill.bottom()),
                plan.pillBar());
        assertTrue(plan.title().right() + 8 <= pill.left());
    }

    @Test
    void longReceiptIsEllipsizedInTheRoomThatIsLeft() {
        StatusPlan plan = plan(480, 360, "Terminal > Squads", "R".repeat(200), "Academy · Alpha");

        assertEquals(plan.title().right() + 8, plan.pill().left(), "the pill takes all the room");
        assertTrue(plan.feedback().truncated());
        assertTrue(plan.feedback().width() <= plan.feedback().maxWidth());
        // The identity would end 6px left of the pill, which starts right after the title.
        assertEquals(-6, plan.identityRoom(), "nothing is left for the identity");
        assertEquals(StatusText.NONE, plan.identity());
    }

    @Test
    void narrowBarDropsThePillAndTheIdentity() {
        // 150px: after the right end and a 40px title less than 40px remain.
        UiRect bar = UiRect.of(0, 0, 150, 11);
        StatusPlan plan = TacticalBoardChrome.planStatus(bar, "X".repeat(40), "Saved loadout",
                "Academy · 1st · Alpha · Leader", CLOCK, WIDTH);

        assertEquals(40, plan.title().maxWidth());
        assertEquals(UiRect.EMPTY, plan.pill());
        assertEquals(StatusText.NONE, plan.feedback());
        assertTrue(plan.identityRoom() < 30);
        assertEquals(StatusText.NONE, plan.identity());
    }

    @Test
    void shortTitleStillGetsTwentyFourPixels() {
        UiRect bar = UiRect.of(0, 0, 100, 10);
        StatusPlan plan = TacticalBoardChrome.planStatus(bar, "WOK", null, null, CLOCK, WIDTH);

        assertEquals(24, plan.title().maxWidth());
    }

    @Test
    void identityIsRightAlignedUpToThePill() {
        String identity = "Academy · 1st Battalion · Alpha · Leader";
        StatusPlan withPill = plan(960, 540, "Terminal > Squads", "Loadout saved", identity);
        StatusPlan without = plan(960, 540, "Terminal > Squads", null, identity);

        assertEquals(withPill.pill().left() - 6, withPill.identity().right());
        assertEquals(without.signalX() - 7, without.identity().right());
        assertEquals(identity, without.identity().text());
        assertEquals(without.signalX() - 7 - (without.title().right() + 8),
                without.identityRoom());
        assertFalse(without.identity().truncated());
    }

    @Test
    void fitIdentityDropsPartsBeforeItCuts() {
        String four = "A · BB · CCC · DDDD";

        assertEquals(four, TacticalBoardChrome.fitIdentity(four, 200, WIDTH).text());
        assertEquals("A · CCC · DDDD", TacticalBoardChrome.fitIdentity(four, 100, WIDTH).text(),
                "without the second part");
        assertEquals("A · CCC", TacticalBoardChrome.fitIdentity(four, 50, WIDTH).text(),
                "first and second-to-last part");
        assertEquals("A", TacticalBoardChrome.fitIdentity(four, 30, WIDTH).text());
        assertEquals("", TacticalBoardChrome.fitIdentity(four, 29, WIDTH).text(),
                "below 30px there is no identity");
        assertFalse(TacticalBoardChrome.fitIdentity(four, 50, WIDTH).truncated());

        String three = "A · BB · CCC";
        assertEquals("A · BB", TacticalBoardChrome.fitIdentity(three, 40, WIDTH).text());

        TextFit.Plain cut = TacticalBoardChrome.fitIdentity("ABCDEFGHIJKL", 40, WIDTH);
        assertTrue(cut.truncated(), "a single long part is ellipsized");
        assertTrue(cut.text().endsWith(TextFit.ELLIPSIS));
        assertTrue(cut.width() <= 40);
        assertEquals("", TacticalBoardChrome.fitIdentity(null, 100, WIDTH).text());
    }

    @ParameterizedTest(name = "{0}x{1}")
    @CsvSource({"320, 240", "427, 240", "480, 270", "640, 336", "640, 360", "480, 360",
            "960, 540"})
    void statusPartsStayInsideTheBarAndApart(int width, int height) {
        for (String feedback : new String[]{null, "Saved", "Loadout saved", "R".repeat(90)}) {
            StatusPlan plan = plan(width, height, "WOK Infantry // Battle Terminal > Deployment",
                    feedback, "Academy · 1st Mechanized Battalion · Alpha · Squad leader");
            UiRect bar = plan.bar();
            Map<String, UiRect> parts = new LinkedHashMap<>();
            parts.put("stripe", plan.stripe());
            parts.put("title", box(plan, plan.title()));
            parts.put("pill", plan.pill());
            if (plan.identity().shown()) {
                parts.put("identity", box(plan, plan.identity()));
            }
            parts.put("signal", plan.signal());
            parts.put("battery", plan.battery());
            parts.put("clock", box(plan, plan.clock()));
            String where = width + "x" + height + " feedback " + feedback;
            List<Map.Entry<String, UiRect>> list = List.copyOf(parts.entrySet());
            for (int first = 0; first < list.size(); first++) {
                UiRect a = list.get(first).getValue();
                assertTrue(a.isEmpty() || bar.contains(a), where + ": " + list.get(first).getKey()
                        + " " + a + " leaves " + bar);
                for (int second = first + 1; second < list.size(); second++) {
                    assertFalse(a.intersects(list.get(second).getValue()), where + ": "
                            + list.get(first).getKey() + " overlaps " + list.get(second).getKey());
                }
            }
            if (!plan.pill().isEmpty()) {
                assertTrue(plan.pill().width() >= TacticalBoardChrome.PILL_MIN_WIDTH, where);
            }
            assertTrue(plan.textY() + 8 <= bar.bottom() - 1, where + ": text above the bar rule");
        }
    }

    @Test
    void titleNamesTheCurrentPage() {
        assertEquals("WOK步战 // 战斗终端 › 部署",
                TacticalBoardChrome.statusTitle("WOK步战 // 战斗终端", "部署"));
        assertEquals("部署", TacticalBoardChrome.statusTitle("", "部署"));
        assertEquals("终端", TacticalBoardChrome.statusTitle("终端", null));
        assertEquals("终端", TacticalBoardChrome.statusTitle("终端", " "));

        List<TacticalTabStrip.Tab> tabs = List.of(
                TacticalTabStrip.Tab.of("squads", Component.literal("小队"), Component.literal("队")),
                TacticalTabStrip.Tab.of("deploy", Component.literal("部署地点"),
                        Component.literal("部署")));
        TacticalTabStrip strip = new TacticalTabStrip(TacticalTabStrip.Skin.HEADER, tabs, 1, i -> {
        });
        assertEquals("终端 › 部署地点",
                TacticalBoardChrome.statusTitle(Component.literal("终端"), strip, false));
        assertEquals("终端 › 部署",
                TacticalBoardChrome.statusTitle(Component.literal("终端"), strip, true),
                "the compact class shows the short page name");
        assertEquals("终端", TacticalBoardChrome.statusTitle(Component.literal("终端"), null, true));
    }

    @Test
    void clockIsLocalTimeUnlessFixed() {
        assertEquals("09:05", TacticalBoardChrome.clockText(null, LocalTime.of(9, 5, 59)));
        assertEquals("23:59", TacticalBoardChrome.clockText(" ", LocalTime.of(23, 59)));
        assertEquals("21:30", TacticalBoardChrome.clockText(" 21:30 ", LocalTime.of(9, 5)));
    }

    @Test
    void linkStateLightsTheDeviceLedAndTheBars() {
        assertEquals(DeviceSkin.CAESAR.led(), LinkState.OK.ledColor(DeviceSkin.CAESAR));
        assertEquals(DeviceSkin.NEUTRAL.led(), LinkState.OK.ledColor(DeviceSkin.NEUTRAL));
        assertEquals(TacticalBoardTheme.ACCENT_B, LinkState.WAIT.ledColor(DeviceSkin.ACADEMY));
        assertEquals(TacticalBoardTheme.DANGER_B, LinkState.LOST.ledColor(DeviceSkin.ACADEMY));
        assertEquals(4, LinkState.OK.signalBars());
        assertEquals(2, LinkState.WAIT.signalBars());
        assertEquals(0, LinkState.LOST.signalBars());
        assertEquals(LinkState.WAIT, LinkState.forBattleSnapshot(null));
    }

    @ParameterizedTest(name = "{0}x{1}")
    @CsvSource({"320, 240, 3, 2, 1, 1, 1", "480, 360, 12, 3, 2, 2, 3", "960, 540, 12, 3, 2, 2, 3"})
    void bezelKeyRowFollowsPageKeys(int width, int height, int inset, int ledGap, int ledHeight,
                                    int keyGap, int bottomGap) {
        TacticalShellLayout layout = TacticalShellLayout.compute(width, height);
        BezelPlan plan = TacticalBoardChrome.planBezel(layout);
        UiRect row = plan.keyRow();

        assertEquals(layout.bezel(), plan.bezel());
        assertEquals(layout.glass().left() + inset, row.left());
        assertEquals(layout.glass().right() - inset, row.right());
        assertEquals(layout.bezel().top() + ledGap, plan.ledTop());
        assertEquals(ledHeight, plan.ledHeight());
        assertEquals(plan.ledTop() + ledHeight + keyGap, row.top());
        assertEquals(layout.device().bottom() - bottomGap, row.bottom());
        assertTrue(layout.bezel().contains(row));
        assertTrue(row.height() >= 10, "a readable key on every tier");
    }
}
