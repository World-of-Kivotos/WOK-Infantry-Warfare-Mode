package com.wok.infantry.client.tablet;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * First login without a battle snapshot (DESIGN 3.6, review S9 c): "search" every 500 ms, at most
 * six, "ready" once the link is up while the tablet is out, nothing after a close.
 */
class TabletLinkChimeTest {
    private static final double FRAME = 1000.0D / 60.0D;

    @Test
    void constantsAreThePreviews() {
        JsonObject search = TabletVectors.object("spec").getAsJsonObject("a").getAsJsonObject("sfx")
                .getAsJsonObject("search");
        assertEquals(search.get("name").getAsString(), TabletAnimationModel.SFX_SEARCH);
        assertEquals(search.get("everyMs").getAsDouble(), TabletAnimationModel.SFX_SEARCH_EVERY_MS, 0.0D);
        assertEquals(search.get("maxTicks").getAsInt(), TabletAnimationModel.SFX_SEARCH_MAX_TICKS);
    }

    @Test
    void searchesEveryHalfSecondAtMostSixTimes() {
        TabletLinkChime chime = new TabletLinkChime();
        chime.start(1000.0D);
        List<Double> ticks = new ArrayList<>();
        for (int frame = 0; frame < 600; frame++) {
            double t = 1000.0D + frame * FRAME;
            for (String name : chime.update(t, false, TabletMotion.State.SHOWN)) {
                assertEquals("search", name);
                ticks.add(t);
            }
        }
        assertEquals(6, ticks.size(), "at most six ticks (3 s): " + ticks);
        assertEquals(1000.0D, ticks.get(0), 1e-9, "the first tick at once");
        for (int i = 1; i < ticks.size(); i++) {
            double gap = ticks.get(i) - ticks.get(i - 1);
            assertTrue(gap >= 500.0D - 1e-6 && gap < 500.0D + FRAME, "gap " + gap);
        }
        assertTrue(chime.active(), "still waiting for the link after the last tick");
        assertEquals(6, chime.ticks());
        assertEquals(List.of("ready"), chime.update(20000.0D, true, TabletMotion.State.SHOWN),
                "the link still plays ready after the ticks ran out");
        assertFalse(chime.active());
    }

    @Test
    void readyOnceTheLinkComesUpWhileOpeningOrShown() {
        for (TabletMotion.State state : TabletMotion.State.values()) {
            TabletLinkChime chime = new TabletLinkChime();
            chime.start(0.0D);
            assertEquals(List.of("search"), chime.update(0.0D, false, state));
            List<String> sounds = chime.update(300.0D, true, state);
            boolean out = state == TabletMotion.State.OPENING || state == TabletMotion.State.SHOWN;
            assertEquals(out ? List.of("ready") : List.of(), sounds, state.name());
            assertFalse(chime.active());
            assertEquals(List.of(), chime.update(400.0D, true, state), "ready plays once");
        }
    }

    @Test
    void aLongFrameDoesNotBunchTicks() {
        TabletLinkChime chime = new TabletLinkChime();
        chime.start(0.0D);
        assertEquals(List.of("search"), chime.update(0.0D, false, TabletMotion.State.SHOWN));
        // a 1.4 s hitch: one tick, and the next one half an interval later at the earliest
        assertEquals(List.of("search"), chime.update(1400.0D, false, TabletMotion.State.SHOWN));
        assertEquals(List.of(), chime.update(1500.0D, false, TabletMotion.State.SHOWN));
        assertEquals(List.of(), chime.update(1640.0D, false, TabletMotion.State.SHOWN));
        assertEquals(List.of("search"), chime.update(1650.0D, false, TabletMotion.State.SHOWN));
    }

    @Test
    void stopAndRestart() {
        TabletLinkChime chime = new TabletLinkChime();
        assertEquals(List.of(), chime.update(0.0D, true, TabletMotion.State.SHOWN), "idle chime is silent");
        chime.start(0.0D);
        chime.update(0.0D, false, TabletMotion.State.OPENING);
        chime.start(100.0D);
        assertEquals(1, chime.ticks(), "start while searching keeps the running chime");
        chime.stop();
        assertEquals(List.of(), chime.update(600.0D, true, TabletMotion.State.SHOWN));
        chime.start(700.0D);
        assertEquals(0, chime.ticks());
        assertEquals(List.of("search"), chime.update(700.0D, false, TabletMotion.State.SHOWN));
    }
}
