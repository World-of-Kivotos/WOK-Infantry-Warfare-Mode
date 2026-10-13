package com.wok.infantry.client.screen;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure placement rules that keep support labels clear of their rings and of the map chrome. */
class TacticalMapLabelPlacementTest {
    // 320x240 at GUI scale 3: map viewport 8..186 x 48..190, chrome measured from the screen.
    private static final TacticalMapLayout.Rect COMPACT_SCALE =
            new TacticalMapLayout.Rect(15, 172, 71, 186);
    private static final TacticalMapLayout.Rect COMPACT_COMPASS =
            new TacticalMapLayout.Rect(159, 80, 178, 94);
    private static final List<TacticalMapLayout.Rect> COMPACT_CHROME =
            List.of(COMPACT_SCALE, COMPACT_COMPASS);

    @Test
    void aLabelAwayFromTheChromeKeepsItsPlace() {
        TacticalMapLayout.Rect tag = new TacticalMapLayout.Rect(40, 120, 119, 130);
        assertEquals(40, TacticalMapScreen.clearOfMapChrome(tag, 10, 105, 2, COMPACT_CHROME));
    }

    @Test
    void theZoneTagSlidesOffTheScaleBarAlongItsOwnRow() {
        // Reported case: the F-16C "Designation zone R64m" tag sat under the 100 m scale bar.
        TacticalMapLayout.Rect tag = new TacticalMapLayout.Rect(28, 170, 107, 180);
        int left = TacticalMapScreen.clearOfMapChrome(tag, 10, 105, 2, COMPACT_CHROME);
        assertEquals(COMPACT_SCALE.right() + 2, left);
        TacticalMapLayout.Rect moved = new TacticalMapLayout.Rect(left, tag.top(),
                left + tag.width(), tag.bottom());
        assertFalse(moved.intersects(COMPACT_SCALE));
        assertTrue(moved.right() <= 186 - 2, "the tag must stay inside the map viewport");
    }

    @Test
    void aCardUnderTheCompassSlidesToTheNearerClearSide() {
        TacticalMapLayout.Rect card = new TacticalMapLayout.Rect(40, 82, 172, 99);
        int left = TacticalMapScreen.clearOfMapChrome(card, 10, 52, 2, COMPACT_CHROME);
        assertEquals(COMPACT_COMPASS.left() - 2 - card.width(), left);
    }

    @Test
    void aLabelWithNoClearRowPositionKeepsItsPlace() {
        // A 170px card on a 178px map cannot leave the compass row; drawing it in place beats
        // pushing it outside the viewport.
        TacticalMapLayout.Rect card = new TacticalMapLayout.Rect(12, 82, 182, 99);
        assertEquals(12, TacticalMapScreen.clearOfMapChrome(card, 10, 14, 2, COMPACT_CHROME));
    }

    @Test
    void aSlideThatWouldLandOnOtherChromeIsSkipped() {
        TacticalMapLayout.Rect left = new TacticalMapLayout.Rect(10, 100, 30, 110);
        TacticalMapLayout.Rect right = new TacticalMapLayout.Rect(48, 100, 70, 110);
        TacticalMapLayout.Rect label = new TacticalMapLayout.Rect(25, 102, 45, 108);
        int placed = TacticalMapScreen.clearOfMapChrome(label, 0, 200, 2,
                List.of(left, right));
        assertEquals(right.right() + 2, placed,
                "the gap between the two panels is too narrow, so the label goes past both");
    }

    @Test
    void missionCardsSitWhollyAboveTheAreaOutlineAndTheZone() {
        // 960x720, GUI scale 1, default zoom: R80 area 36px, R64 zone 29px with a 2px casing
        // half-width, 4px gap and a 47px mission card.
        int cardHalf = (47 + 1) / 2;
        int offset = TacticalMapScreen.supportGuidanceLabelOffset(36, 29, 2, 4, cardHalf);
        int cardBottom = -offset + cardHalf;
        assertTrue(cardBottom <= -(36 + 4), "the card must stay above the R80 outline");
        assertTrue(cardBottom <= -(29 + 2 + 4), "the card must stay above the R64 casing");

        // 320x240, GUI scale 3: 17px card, 1px casing half-width, 2px gap.
        int compactHalf = (17 + 1) / 2;
        int compactOffset = TacticalMapScreen.supportGuidanceLabelOffset(36, 29, 1, 2,
                compactHalf);
        assertTrue(-compactOffset + compactHalf <= -(36 + 2));
    }
}
