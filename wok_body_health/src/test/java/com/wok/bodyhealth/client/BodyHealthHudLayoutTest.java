package com.wok.bodyhealth.client;

import com.wok.bodyhealth.client.BodyHealthHudLayout.Layout;
import com.wok.bodyhealth.client.BodyHealthHudLayout.Tier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BodyHealthHudLayoutTest {
    /** Vanilla font widths of "85/85" and "85" (six pixels per digit or slash). */
    private static final int FULL_LABEL = 30;
    private static final int SHORT_LABEL = 12;

    @Test
    void picksTheRichestTierThatFitsLeftOfTheHotbar() {
        assertEquals(Tier.COMPACT, layout(320).tier());
        assertEquals(Tier.COMPACT, layout(342).tier());
        assertEquals(Tier.COMPACT, layout(351).tier());
        assertEquals(Tier.SHORT, layout(352).tier());
        assertEquals(Tier.SHORT, layout(384).tier());
        assertEquals(Tier.SHORT, layout(427).tier());
        assertEquals(Tier.SHORT, layout(435).tier());
        assertEquals(Tier.FULL, layout(436).tier());
        assertEquals(Tier.FULL, layout(456).tier());
        assertEquals(Tier.FULL, layout(960).tier());
    }

    @Test
    void keepsEveryTierLeftOfTheHotbarAndInsideTheScreen() {
        for (int width = 320; width <= 1920; width++) {
            Layout layout = layout(width);
            int labelExtent = switch (layout.tier()) {
                case FULL -> Tier.FULL.labelGap() + FULL_LABEL + BodyHealthHudLayout.CHIP_PADDING;
                case SHORT -> Tier.SHORT.labelGap() + SHORT_LABEL + BodyHealthHudLayout.CHIP_PADDING;
                case COMPACT -> 0;
            };
            int left = Math.min(layout.figureX() - labelExtent, layout.companionLeft());
            int right = Math.max(layout.figureX() + BodyHealthHudLayout.FIGURE_WIDTH + labelExtent,
                    layout.companionLeft() + BodyHealthHudLayout.COMPANION_WIDTH);
            assertTrue(left >= 2, "left edge at width " + width);
            assertTrue(right <= BodyHealthHudLayout.hotbarLeft(width) - 4, "right edge at width " + width);
        }
    }

    @Test
    void staysWhereTheOldFigureWasOnWideScreens() {
        Layout wide = BodyHealthHudLayout.compute(960, 720, FULL_LABEL, SHORT_LABEL);
        assertEquals(69, wide.figureCenterX());
        assertEquals(43, wide.companionLeft());
        assertEquals(720 - 13, wide.companionTop());
    }

    @Test
    void compactColumnCentresTheStaminaStripUnderTheFigure() {
        Layout compact = layout(320);
        assertEquals(20, compact.figureX());
        assertEquals(13, compact.companionLeft());
        assertEquals(compact.figureCenterX(),
                compact.companionLeft() + BodyHealthHudLayout.COMPANION_WIDTH / 2);
    }

    @Test
    void centredChipsShiftLeftInsteadOfCrossingTheHotbar() {
        Layout compact = layout(320);
        int chipWidth = 42 + BodyHealthHudLayout.CHIP_PADDING;
        int chipLeft = compact.centredChipLeft(chipWidth);
        assertTrue(chipLeft >= 2);
        assertTrue(chipLeft + chipWidth <= compact.rightLimit());
        assertEquals(-1, compact.centredChipLeft(200));
    }

    private static Layout layout(int width) {
        return BodyHealthHudLayout.compute(width, 240, FULL_LABEL, SHORT_LABEL);
    }
}
