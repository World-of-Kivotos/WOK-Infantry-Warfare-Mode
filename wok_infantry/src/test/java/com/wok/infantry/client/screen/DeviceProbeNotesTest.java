package com.wok.infantry.client.screen;

import com.wok.infantry.client.ui.probe.UiLayoutFrame;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The probe notes the device leaves for the uiTest (plan 6, new semantic checks): the livery and
 * link state a frame was drawn with, and which page LED is lit above which key. The uiTest parses
 * these exact forms; production never records them (no probe receiver).
 */
class DeviceProbeNotesTest {
    @Test
    void shellNoteNamesTheLiveryAndTheLinkState() {
        assertEquals("shell livery=CAESAR link=WAIT",
                TacticalBoardChrome.shellNote(TacticalLivery.Livery.CAESAR,
                        TacticalBoardChrome.LinkState.WAIT));
        assertEquals("shell livery=ACADEMY link=OK",
                TacticalBoardChrome.shellNote(TacticalLivery.Livery.ACADEMY,
                        TacticalBoardChrome.LinkState.OK));
        // Same fallbacks as the shell itself: no livery is Neutral, no link is OK.
        assertEquals("shell livery=NEUTRAL link=OK", TacticalBoardChrome.shellNote(null, null));
        assertTrue(TacticalBoardChrome.shellNote(null, null)
                .startsWith(TacticalBoardChrome.SHELL_NOTE));
    }

    @Test
    void ledNoteCarriesTheKeyRectangleAsTheProbeWritesIt() {
        UiLayoutFrame.Rect key = new UiLayoutFrame.Rect(124.0F, 450.0F, 200.5F, 474.0F);
        assertEquals("bezel.led lit=true key=[124,450,200.5,474]", BezelKey.ledNote(true, key));
        assertEquals("bezel.led lit=false key=" + key, BezelKey.ledNote(false, key));
        assertTrue(BezelKey.ledNote(false, key).startsWith(BezelKey.LED_NOTE));
    }
}
