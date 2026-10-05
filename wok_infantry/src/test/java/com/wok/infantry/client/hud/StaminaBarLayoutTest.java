package com.wok.infantry.client.hud;

import com.wok.infantry.client.hud.StaminaBarLayout.Layout;
import com.wok.infantry.client.screen.UiRect;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The stamina bar's geometry against the A4 pixel spec of the preview
 * ({@code surfaces/16-stamina.js} stamina-a4 notes, "像素规格" per tier), and its clearances from
 * the vanilla hotbar, status rows, chat and TaCZ's ammo readout on every width.
 */
class StaminaBarLayoutTest {
    private static final int NUMBER_WIDTH = HudTestSupport.width("100%");

    private static Layout layout(int width, int height, int factor) {
        return StaminaBarLayout.compute(width, height, factor, NUMBER_WIDTH,
                StaminaBarLayout.DEFAULT_CHAT_RIGHT);
    }

    @Test
    void narrowestTierMatchesThePreview() {
        // preview: 凹槽 [69,201)×[210,217)，左耳 [59,69)×[204,215)、中缝凸台 [154,166)×[200,210)，
        // 手槽 [70,133)×[212,215)、腿槽 [137,198)×[212,215)，不显示数字
        Layout bar = layout(320, 240, 1);
        assertTrue(bar.narrow());
        assertFalse(bar.numbers());
        assertEquals(1, bar.earScale());
        assertEquals(UiRect.of(69, 210, 201, 217), bar.band());
        assertEquals(UiRect.of(59, 204, 69, 215), bar.earLeft());
        assertNull(bar.earRight(), "the right ear would reach TaCZ's readout");
        assertEquals(UiRect.of(154, 200, 166, 210), bar.tab());
        assertEquals(UiRect.of(70, 212, 133, 215), bar.arms());
        assertEquals(UiRect.of(137, 212, 198, 215), bar.legs());
        assertEquals(UiRect.of(134, 211, 136, 216), bar.separator());
        assertEquals(UiRect.of(59, 204, 60, 215), bar.armsAccent(), "1px accent in the 10px ear");
        assertEquals(UiRect.of(199, 210, 201, 217), bar.legsAccent(), "legs accent: plate's last 2px");
        assertEquals(UiRect.of(60, 205, 69, 214), bar.armsIcon());
        assertEquals(UiRect.of(156, 201, 165, 210), bar.legsIcon(), "boot in the centre tab");
        assertNull(bar.armsNumber());
        assertNull(bar.legsNumber());
    }

    @Test
    void narrowRuleAt427() {
        Layout bar = layout(427, 240, 1);
        assertTrue(bar.narrow());
        assertEquals(UiRect.of(122, 210, 304, 217), bar.band(), "full row: 304 < 427 − 119");
        assertEquals(UiRect.of(112, 204, 122, 215), bar.earLeft());
        assertEquals(UiRect.of(207, 200, 219, 210), bar.tab());
        assertEquals(UiRect.of(123, 212, 211, 215), bar.arms());
        assertEquals(UiRect.of(215, 212, 301, 215), bar.legs());
    }

    @Test
    void wideTiersWithoutNumbersMatchThePreview() {
        // preview 480×270：凹槽 [149,331)×[240,247)，左耳 [134,149)×[234,247)、右耳 [331,346)×[234,247)，
        // 手槽 [150,238)×[242,245)、腿槽 [242,330)×[242,245)，不显示数字
        Layout bar = layout(480, 270, 1);
        assertFalse(bar.narrow());
        assertFalse(bar.numbers());
        assertEquals(StaminaBarLayout.EAR_WIDTH, bar.earWidth());
        assertEquals(UiRect.of(149, 240, 331, 247), bar.band());
        assertEquals(UiRect.of(134, 234, 149, 247), bar.earLeft());
        assertEquals(UiRect.of(331, 234, 346, 247), bar.earRight());
        assertNull(bar.tab());
        assertEquals(UiRect.of(150, 242, 238, 245), bar.arms());
        assertEquals(UiRect.of(242, 242, 330, 245), bar.legs());
        assertEquals(UiRect.of(239, 241, 241, 246), bar.separator());
        assertEquals(UiRect.of(134, 234, 136, 247), bar.armsAccent());
        assertEquals(UiRect.of(344, 234, 346, 247), bar.legsAccent());
        assertEquals(UiRect.of(138, 236, 147, 245), bar.armsIcon(), "icon at (earL.l + 4, h − 34)");
        assertEquals(UiRect.of(333, 236, 342, 245), bar.legsIcon(), "icon at (earR.r − 13, h − 34)");
    }

    @Test
    void wideTiersWithNumbersMatchThePreview() {
        // preview 640×360：凹槽 [229,411)×[330,337)，左耳 [187,229)×[324,337)、右耳 [411,453)×[324,337)，
        // 手槽 [230,318)×[332,335)、腿槽 [322,410)×[332,335)，数字（"100%" 宽 24）在耳朵里贴着槽的外端
        Layout bar = layout(640, 360, 1);
        assertTrue(bar.numbers());
        assertEquals(42, bar.earWidth(), "2 + 2 + 9 + 3 + 24 + 2");
        assertEquals(UiRect.of(229, 330, 411, 337), bar.band());
        assertEquals(UiRect.of(187, 324, 229, 337), bar.earLeft());
        assertEquals(UiRect.of(411, 324, 453, 337), bar.earRight());
        assertEquals(UiRect.of(230, 332, 318, 335), bar.arms());
        assertEquals(UiRect.of(322, 332, 410, 335), bar.legs());
        assertEquals(UiRect.of(191, 326, 200, 335), bar.armsIcon());
        assertEquals(UiRect.of(440, 326, 449, 335), bar.legsIcon());
        // numbers in row h − 33; "100%" right-aligned ends at x = 227 (drawn from 204 as the
        // preview's P.r − 2 − width + 1), the legs' starts at 413 (P.l + 2)
        assertEquals(UiRect.of(203, 327, 227, 335), bar.armsNumber());
        assertEquals(UiRect.of(413, 327, 437, 335), bar.legsNumber());

        Layout tall = layout(640, 336, 1);
        assertEquals(UiRect.of(229, 306, 411, 313), tall.band(), "640×336: 24 rows higher");
        assertEquals(UiRect.of(187, 300, 229, 313), tall.earLeft());
        assertEquals(UiRect.of(411, 300, 453, 313), tall.earRight());

        // preview 960×540：凹槽 [389,571)×[510,517)，左耳 [347,389)×[504,517)、右耳 [571,613)×[504,517)，
        // 手槽 [390,478)×[512,515)、腿槽 [482,570)×[512,515)
        Layout wide = layout(960, 540, 1);
        assertEquals(UiRect.of(389, 510, 571, 517), wide.band());
        assertEquals(UiRect.of(347, 504, 389, 517), wide.earLeft());
        assertEquals(UiRect.of(571, 504, 613, 517), wide.earRight());
        assertEquals(UiRect.of(390, 512, 478, 515), wide.arms());
        assertEquals(UiRect.of(482, 512, 570, 515), wide.legs());
        assertEquals(UiRect.of(389, 690, 571, 697), layout(960, 720, 1).band());
    }

    @Test
    void guiScaleOneKeepsTheGroovesAtOneXAndDoublesTheEars() {
        // 960×720 at GUI 1 with the 2× HUD (480×360 layout): no numbers (as the preview's
        // 480×360 page); grooves 1× in the vanilla row, ears 2× [h − 49, h − 23)
        Layout bar = layout(960, 720, 2);
        assertEquals(2, bar.earScale());
        assertFalse(bar.numbers());
        assertFalse(bar.narrow());
        assertEquals(UiRect.of(389, 690, 571, 697), bar.band());
        assertEquals(UiRect.of(359, 671, 389, 697), bar.earLeft());
        assertEquals(UiRect.of(571, 671, 601, 697), bar.earRight());
        assertEquals(UiRect.of(390, 692, 478, 695), bar.arms());
        assertEquals(UiRect.of(482, 692, 570, 695), bar.legs());
        assertEquals(UiRect.of(367, 675, 385, 693), bar.armsIcon(), "18×18 silhouette");
        assertEquals(UiRect.of(575, 675, 593, 693), bar.legsIcon());
        assertEquals(UiRect.of(359, 671, 363, 697), bar.armsAccent(), "2 ear units = 4px");
        assertTrue(bar.earLeft().left() >= StaminaBarLayout.DEFAULT_CHAT_RIGHT,
                "the 2× ear reaches above the chat's last line but stays right of its columns");

        // 1920×1080 at GUI 1: a 960×540 layout, wide enough for the 2× numbers
        Layout large = layout(1920, 1080, 2);
        assertTrue(large.numbers());
        assertEquals(UiRect.of(785, 1031, 869, 1057), large.earLeft());
        assertEquals(UiRect.of(817, 1037, 865, 1053), large.armsNumber());
        assertEquals(UiRect.of(1055, 1037, 1103, 1053), large.legsNumber());
    }

    @Test
    void guiScaleOneEarsStayOneXWhereTheyWouldEnterTheChat() {
        // the acceptance's 427×240 tier: 854×480 at GUI 1; a 2× left ear [306, 336) would reach
        // into the chat's background [0, 332) above its last line h − 40
        Layout bar = layout(854, 480, 2);
        assertEquals(1, bar.earScale());
        assertFalse(bar.narrow());
        assertEquals(UiRect.of(321, 444, 336, 457), bar.earLeft());
        assertEquals(UiRect.of(518, 444, 533, 457), bar.earRight());
        // a narrower chat leaves room for the 2× ears
        Layout narrowChat = StaminaBarLayout.compute(854, 480, 2, NUMBER_WIDTH,
                StaminaBarLayout.chatRight(200, 1.0D));
        assertEquals(2, narrowChat.earScale());
        assertEquals(UiRect.of(306, 431, 336, 457), narrowChat.earLeft());
        assertEquals(906, firstWidthWithDoubleEars(), "default chat: 2× ears from 906 wide on");
    }

    private static int firstWidthWithDoubleEars() {
        for (int width = 640; width <= 1920; width++) {
            if (layout(width, 480, 2).earScale() == 2) {
                return width;
            }
        }
        return -1;
    }

    @Test
    void narrowFormStartsWhereTheRightEarWouldReachTaczs() {
        assertTrue(layout(444, 250, 1).narrow());
        assertFalse(layout(445, 250, 1).narrow(), "ear [313, 328) ends at TaCZ's 328");
        assertTrue(layout(440, 250, 1).narrow(), "the preview's 440 rule plus the 4px it missed");
        assertEquals(UiRect.of(69, 210, 201, 217), layout(320, 240, 1).band());
        assertEquals(201, layout(320, 240, 1).band().right(), "cut 2px before w − 117");
        assertEquals(UiRect.of(102, 211, 267, 218), layout(386, 241, 1).band(),
                "below 420 wide the row ends 2px before TaCZ's keep-out");
    }

    @Test
    void chatRightFollowsTheChatWidthAndScale() {
        assertEquals(332, StaminaBarLayout.chatRight(320, 1.0D), "default: [0, 332)");
        assertEquals(172, StaminaBarLayout.chatRight(160, 1.0D));
        assertEquals(326, StaminaBarLayout.chatRight(320, 0.5D), "(640 + 12) × 0.5");
        assertEquals(332, StaminaBarLayout.chatRight(320, Double.NaN), "bad scale: 1");
        assertEquals(42, StaminaBarLayout.earWidth(24));
        assertEquals(15, StaminaBarLayout.EAR_WIDTH);
    }

    /** Every width and the clearances that keep the bar from touching anything vanilla or TaCZ. */
    @Test
    void everyWidthKeepsClearOfHotbarStatusRowsChatAndTacz() {
        for (int factor = 1; factor <= 2; factor++) {
            for (int width = 320; width <= 1920; width++) {
                for (int height : new int[]{240, 271, 336, 480, 720}) {
                    if (factor == 2 && (width / 2 < 320 || height / 2 < 240)) {
                        continue;
                    }
                    assertClear(layout(width, height, factor), factor,
                            width + "x" + height + "@" + factor);
                }
            }
        }
    }

    private static void assertClear(Layout bar, int factor, String where) {
        int w = bar.guiWidth();
        int h = bar.guiHeight();
        int cx = w / 2;
        UiRect band = bar.band();
        assertEquals(h - 30, band.top(), where);
        assertEquals(h - 23, band.bottom(), where + ": ends on the selection frame's top row");
        assertTrue(band.left() >= cx - 91 && band.right() <= cx + 91, where + ": hotbar column");
        UiRect tacz = UiRect.of(w - 117, h - 48, w - 5, h - 22);
        List<UiRect> obstacles = List.of(
                // the hotbar, its selection frame, the off-hand slots and the attack indicators
                UiRect.of(0, h - 23, w, h),
                // vanilla status rows above the experience row (hearts, armour, food, air, mount)
                UiRect.of(cx - 91, 0, cx - 10, h - 30), UiRect.of(cx + 10, 0, cx + 91, h - 30),
                tacz);
        List<UiRect> pieces = bar.pieces();
        for (UiRect piece : pieces) {
            for (UiRect obstacle : obstacles) {
                assertFalse(piece.intersects(obstacle), where + ": " + piece + " vs " + obstacle);
            }
            if (piece.top() < h - 40) {
                assertTrue(piece.left() >= StaminaBarLayout.DEFAULT_CHAT_RIGHT,
                        where + ": above the chat's last line only right of its columns " + piece);
            }
            assertTrue(piece.left() >= 0 && piece.right() <= w, where + ": on screen " + piece);
        }
        for (int i = 0; i < pieces.size(); i++) {
            for (int j = i + 1; j < pieces.size(); j++) {
                assertFalse(pieces.get(i).intersects(pieces.get(j)), where + ": pieces only touch");
            }
        }
        if (bar.narrow()) {
            assertNotNull(bar.tab(), where);
            assertTrue(bar.tab().left() >= cx - 10 && bar.tab().right() <= cx + 10,
                    where + ": the tab sits in the centre gap");
            assertTrue(bar.earLeft().right() <= cx - 91, where);
        } else {
            assertTrue(bar.earLeft().right() <= cx - 91 && bar.earRight().left() >= cx + 91,
                    where + ": ears outside the hotbar column");
            assertEquals(bar.earLeft().width(), bar.earRight().width(), where + ": symmetric");
        }
        assertEquals(bar.numbers(), w / factor >= 640 && bar.earScale() == factor, where);
        if (bar.numbers()) {
            assertTrue(bar.armsNumber().width() >= NUMBER_WIDTH * bar.earScale(), where);
            assertTrue(bar.earLeft().contains(bar.armsNumber())
                    && bar.earRight().contains(bar.legsNumber()), where);
        }
        assertTrue(bar.arms().width() > 0 && bar.legs().width() > 0, where);
        assertTrue(band.contains(bar.arms()) && band.contains(bar.legs())
                && band.contains(bar.separator()), where);
        assertTrue(bar.earLeft().contains(bar.armsIcon()), where);
        assertTrue((bar.earRight() != null ? bar.earRight() : bar.tab()).contains(bar.legsIcon()),
                where);
        if (w % 47 == 0) {
            assertOutline(bar, where);
        }
    }

    @Test
    void outlineIsTheUnionsBorderDrawnOnce() {
        for (Layout bar : List.of(layout(320, 240, 1), layout(480, 270, 1),
                layout(640, 336, 1), layout(960, 720, 2), layout(854, 480, 2))) {
            assertOutline(bar, bar.guiWidth() + "x" + bar.guiHeight());
        }
        // wide: band top and bottom, each ear's four sides minus where it meets the band
        Layout wide = layout(480, 270, 1);
        assertTrue(wide.outline().contains(UiRect.of(149, 240, 331, 241)), "band top row");
        assertTrue(wide.outline().contains(UiRect.of(148, 235, 149, 240)),
                "left ear's inner side only above the band");
        assertEquals(10, wide.outline().size());
    }

    /** Brute force: a pixel is outline iff it is in the union with a 4-neighbour outside it. */
    private static void assertOutline(Layout bar, String where) {
        List<UiRect> pieces = bar.pieces();
        UiRect bounds = bar.bounds();
        Set<Long> drawn = new HashSet<>();
        for (UiRect segment : bar.outline()) {
            for (int y = segment.top(); y < segment.bottom(); y++) {
                for (int x = segment.left(); x < segment.right(); x++) {
                    assertTrue(drawn.add(key(x, y)), where + ": pixel " + x + "," + y
                            + " drawn twice");
                }
            }
        }
        List<Long> missing = new ArrayList<>();
        for (int y = bounds.top(); y < bounds.bottom(); y++) {
            for (int x = bounds.left(); x < bounds.right(); x++) {
                boolean border = inside(pieces, x, y) && (!inside(pieces, x - 1, y)
                        || !inside(pieces, x + 1, y) || !inside(pieces, x, y - 1)
                        || !inside(pieces, x, y + 1));
                if (border != drawn.contains(key(x, y))) {
                    missing.add(key(x, y));
                }
            }
        }
        assertTrue(missing.isEmpty(), where + ": outline differs at " + missing.size()
                + " pixel(s), first " + (missing.isEmpty() ? "" : decode(missing.get(0))));
    }

    private static boolean inside(List<UiRect> pieces, int x, int y) {
        for (UiRect piece : pieces) {
            if (piece.contains(x + 0.5D, y + 0.5D)) {
                return true;
            }
        }
        return false;
    }

    private static long key(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    private static String decode(long key) {
        return (int) (key >> 32) + "," + (int) key;
    }
}
