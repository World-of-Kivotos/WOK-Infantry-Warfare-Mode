package com.wok.infantry.client.tablet;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A real {@link TabletMotion} drained into a {@link TabletSoundBook} frame by frame, the way the
 * controller does it: which sounds each path and hand plays, when the stow lands (review leftover
 * ①), the switch {@code ui.tabletSounds}, and the first-login chime.
 */
class TabletSoundRouteTest {
    private static final double FRAME = 1000.0D / 60.0D;

    /** Opens at 0, closes {@code holdMs} after it is shown; returns what played, with times. */
    private static List<TabletSoundBookTest.Fake> run(TabletMotion.Config config, boolean enabled,
                                                      boolean link, double holdMs) {
        TabletSoundBookTest.FakeOutput out = new TabletSoundBookTest.FakeOutput();
        TabletSoundBook<TabletSoundBookTest.Fake> book = new TabletSoundBook<>(out);
        TabletMotion motion = new TabletMotion(config);
        double t = 0.0D;
        out.now = t;
        motion.open(t);
        drain(motion, book, t, enabled, link);
        double shownAt = Double.NaN;
        boolean closed = false;
        for (int frame = 1; frame < 600; frame++) {
            t = frame * FRAME;
            out.now = t;
            if (motion.state() == TabletMotion.State.SHOWN && Double.isNaN(shownAt)) {
                shownAt = t;
            }
            if (!closed && !Double.isNaN(shownAt) && t - shownAt >= holdMs) {
                motion.close(t);
                closed = true;
            }
            motion.update(t);
            drain(motion, book, t, enabled, link);
            book.tick(t, enabled, () -> link, motion.state());
            if (closed && motion.state() == TabletMotion.State.IDLE) {
                break;
            }
        }
        assertTrue(closed && motion.state() == TabletMotion.State.IDLE, "the round trip ends");
        return out.played;
    }

    private static void drain(TabletMotion motion, TabletSoundBook<TabletSoundBookTest.Fake> book,
                              double now, boolean enabled, boolean link) {
        for (TabletMotion.Event event : motion.drainEvents()) {
            book.onEvent(event, now, enabled, () -> link);
        }
    }

    private static List<String> names(List<TabletSoundBookTest.Fake> played) {
        List<String> names = new ArrayList<>();
        for (TabletSoundBookTest.Fake fake : played) {
            names.add(fake.name + (fake.dir == TabletCue.Dir.CLOSE ? "<" : ">"));
        }
        return names;
    }

    private static TabletMotion.Config config(TabletMode mode, boolean degrade, TabletHand hand,
                                              TabletScreenKind screen) {
        return new TabletMotion.Config(mode, degrade, hand, true, screen);
    }

    @Test
    void schemeAPlaysTheWholeSetForEachHand() {
        assertEquals(List.of("holster>", "draw>", "grip>", "power>", "boot>", "ready>", "zoom>",
                        "power<", "sleep<", "stow<", "raise<"),
                names(run(config(TabletMode.FULL, false, TabletHand.GUN, TabletScreenKind.SQUAD), true, true, 500)));
        assertEquals(List.of("rustle>", "draw>", "grip>", "power>", "boot>", "ready>", "zoom>",
                        "power<", "sleep<", "stow<", "rustle<"),
                names(run(config(TabletMode.FULL, false, TabletHand.EMPTY, TabletScreenKind.SQUAD), true, true, 500)));
        assertEquals(List.of("rustle>", "draw>", "grip>", "power>", "boot>", "ready>", "zoom>",
                        "power<", "sleep<", "stow<", "rustle<"),
                names(run(config(TabletMode.FULL, false, TabletHand.ITEM, TabletScreenKind.SQUAD), true, true, 500)));
    }

    @Test
    void schemeBPlaysElectronicSoundsOnly() {
        assertEquals(List.of("boot>", "ready>", "sleep<"),
                names(run(config(TabletMode.FULL, true, TabletHand.GUN, TabletScreenKind.SQUAD), true, true, 500)));
        assertEquals(List.of("boot>", "ready>", "sleep<"),
                names(run(config(TabletMode.FULL, true, TabletHand.GUN, TabletScreenKind.FULLSCREEN), true, true, 500)));
    }

    @Test
    void quickAndOffAreSilent() {
        for (TabletMode mode : List.of(TabletMode.QUICK, TabletMode.OFF)) {
            assertEquals(List.of(), names(run(config(mode, false, TabletHand.GUN, TabletScreenKind.SQUAD),
                    true, true, 500)), mode.name());
        }
    }

    @Test
    void soundsOffPlaysNothing() {
        assertEquals(List.of(), names(run(config(TabletMode.FULL, false, TabletHand.GUN,
                TabletScreenKind.SQUAD), false, true, 500)));
    }

    @Test
    void stowLandsOnK0ForEveryHand() {
        // review leftover ①: the stow's main hit (350 ms into the file) on the frame the tablet
        // leaves the picture (K0), within one frame, for guns and items (0.52) and empty hands (0.60)
        double hit = TabletVectors.object("sounds").getAsJsonObject("hitMs").get("stow").getAsDouble();
        double k0 = TabletAnimationModel.KEYS.get(0).p();
        for (TabletHand hand : TabletHand.values()) {
            List<TabletSoundBookTest.Fake> played = run(config(TabletMode.FULL, false, hand,
                    TabletScreenKind.SQUAD), true, true, 500);
            TabletSoundBookTest.Fake power = null;
            TabletSoundBookTest.Fake stow = null;
            for (TabletSoundBookTest.Fake fake : played) {
                if (fake.dir == TabletCue.Dir.CLOSE && fake.name.equals("power")) {
                    power = fake;
                }
                if (fake.name.equals("stow")) {
                    stow = fake;
                }
            }
            assertTrue(power != null && stow != null, hand.name());
            TabletTimeMap close = TabletMotion.closeMap(TabletPath.A3D, hand, true, TabletScreenKind.SQUAD);
            double k0At = power.startedAt + close.invert(k0);
            double error = stow.startedAt + hit - k0At;
            // the cue fires on the first frame past its p (up to one frame late) plus the table's
            // own 0.8 / 2.0 ms; before the fix the empty hand was 72 ms late
            assertTrue(error >= -3.0D && error <= FRAME + 3.0D, hand + ": stow hit " + error + " ms from K0");
        }
    }

    @Test
    void firstLoginSearchesThenStaysSilentWhenPutAway() {
        List<TabletSoundBookTest.Fake> played = run(config(TabletMode.FULL, false, TabletHand.GUN,
                TabletScreenKind.SQUAD), true, false, 1600);
        List<String> names = names(played);
        assertEquals(List.of("holster>", "draw>", "grip>", "power>", "boot>", "search>", "zoom>",
                "search>", "search>", "search>", "power<", "sleep<", "stow<", "raise<"), names,
                "no ready without a snapshot; the search stops when the tablet goes away");
    }
}
