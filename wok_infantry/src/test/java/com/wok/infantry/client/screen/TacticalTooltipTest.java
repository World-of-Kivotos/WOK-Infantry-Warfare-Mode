package com.wok.infantry.client.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TacticalTooltipTest {
    @Test
    void plateIsTheChromeColourAlmostOpaque() {
        // Preview UIX.tooltip: withAlpha(T.FRAME, 0xF2), whatever the faction's FRAME is.
        assertEquals(0xF214191B, TacticalTooltip.plateColor(0xFF14191B));
        assertEquals(0xF2AEB7BA, TacticalTooltip.plateColor(0xFFAEB7BA), "Neutral: a pale plate");
        assertEquals(0xF226090C, TacticalTooltip.plateColor(0x0026090C), "the alpha is replaced");
        assertEquals(TacticalTooltip.PLATE_ALPHA,
                TacticalTooltip.plateColor(TacticalBoardTheme.FRAME) >>> 24);
    }

    @Test
    void boxFitsTheWrappedText() {
        assertEquals(108, TacticalTooltip.boxWidth(100), "4px padding on both sides");
        assertEquals(25, TacticalTooltip.boxHeight(2), "10px lines plus 5");
    }

    @Test
    void tooltipFlipsLeftAndStaysOnScreen() {
        assertEquals(new TacticalTooltip.Placement(60, 46, 80, 25),
                TacticalTooltip.placeAtMouse(50, 50, 80, 25, 320, 240),
                "right of and slightly above the pointer");
        assertEquals(new TacticalTooltip.Placement(214, 46, 80, 25),
                TacticalTooltip.placeAtMouse(300, 50, 80, 25, 320, 240),
                "flipped to the left of the pointer at the right edge");
        assertEquals(new TacticalTooltip.Placement(60, 213, 80, 25),
                TacticalTooltip.placeAtMouse(50, 235, 80, 25, 320, 240),
                "moved up at the bottom edge");
        assertEquals(new TacticalTooltip.Placement(10, 32, 80, 25),
                TacticalTooltip.placeAtAnchor(new UiRect(10, 10, 60, 29), 80, 25, 320, 240),
                "below a keyboard-focused control");
        assertEquals(new TacticalTooltip.Placement(10, 182, 80, 25),
                TacticalTooltip.placeAtAnchor(new UiRect(10, 210, 60, 230), 80, 25, 320, 240),
                "above it when there is no room below");
    }
}
