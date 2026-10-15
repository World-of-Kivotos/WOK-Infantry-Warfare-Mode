package com.wok.infantry.client.tablet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the motion's events do to the sounds ({@link TabletSoundBook}): every scenario of
 * {@code vectors.scenarios} replayed through the book (each sound starts once, each cut fades
 * exactly the sounds of its direction that are still in the book), the 150 ms no-repeat rule, the
 * {@code ui.tabletSounds} switch, the link chime hand-off and clean-up.
 */
class TabletSoundBookTest {
    /** A fake playing sound. */
    static final class Fake {
        final String name;
        final TabletCue.Dir dir;
        final double startedAt;
        double fadedAt = Double.NaN;

        Fake(String name, TabletCue.Dir dir, double startedAt) {
            this.name = name;
            this.dir = dir;
            this.startedAt = startedAt;
        }

        boolean faded() {
            return !Double.isNaN(fadedAt);
        }

        @Override
        public String toString() {
            return name + "/" + dir + "@" + startedAt + (faded() ? " faded@" + fadedAt : "");
        }
    }

    /** Records every play and fade; the clock is set by the test. */
    static final class FakeOutput implements TabletSoundBook.Output<Fake> {
        final List<Fake> played = new ArrayList<>();
        double now;

        @Override
        public Fake play(String name, TabletCue.Dir dir) {
            Fake fake = new Fake(name, dir, now);
            played.add(fake);
            return fake;
        }

        @Override
        public void fadeOut(Fake sound, double at) {
            assertFalse(sound.faded(), () -> sound + " faded twice");
            sound.fadedAt = at;
        }
    }

    private static TabletMotion.Event sound(String name, TabletCue.Dir dir, double at, boolean link) {
        return new TabletMotion.Event(TabletMotion.Event.Type.SOUND, name, at, dir, link);
    }

    private static TabletMotion.Event cut(String which, double at) {
        return new TabletMotion.Event(TabletMotion.Event.Type.CUT, which, at, null, false);
    }

    private static TabletMotion.Event state(TabletMotion.State state, double at) {
        return new TabletMotion.Event(TabletMotion.Event.Type.STATE, state.name(), at, null, false);
    }

    private static TabletMotion.Event event(JsonObject e) {
        String type = e.get("type").getAsString();
        double at = e.get("at").getAsDouble();
        return switch (type) {
            case "sound" -> sound(e.get("name").getAsString(),
                    "close".equals(e.get("dir").getAsString()) ? TabletCue.Dir.CLOSE : TabletCue.Dir.OPEN,
                    at, !TabletVectors.isNull(e.get("link")) && e.get("link").getAsBoolean());
            case "cut" -> cut(e.get("name").getAsString(), at);
            case "state" -> state(TabletMotion.State.valueOf(e.get("name").getAsString()), at);
            default -> throw new AssertionError("event type " + type);
        };
    }

    @Test
    void everyScenarioPlaysEachSoundOnceAndCutsOnlyItsDirection() {
        int scenarios = 0;
        int cuts = 0;
        for (JsonElement element : TabletVectors.array("scenarios")) {
            JsonObject scenario = element.getAsJsonObject();
            String id = scenario.get("id").getAsString();
            FakeOutput out = new FakeOutput();
            TabletSoundBook<Fake> book = new TabletSoundBook<>(out);
            List<String> expectedPlays = new ArrayList<>();
            for (JsonElement ev : scenario.getAsJsonArray("events")) {
                JsonObject e = ev.getAsJsonObject();
                TabletMotion.Event event = event(e);
                out.now = event.atMs();
                if (event.type() == TabletMotion.Event.Type.SOUND) {
                    expectedPlays.add(event.name() + "/" + event.dir());
                }
                List<Fake> before = new ArrayList<>(out.played);
                book.onEvent(event, event.atMs(), true, () -> true);
                if (event.type() != TabletMotion.Event.Type.CUT) {
                    continue;
                }
                cuts++;
                TabletMotion.Cut which = TabletMotion.Cut.valueOf(event.name().toUpperCase(Locale.ROOT));
                for (Fake fake : before) {
                    boolean matches = which == TabletMotion.Cut.ALL || fake.dir.name().equals(which.name());
                    boolean recent = event.atMs() - fake.startedAt <= TabletSoundBook.LIFE_MS;
                    if (matches && recent) {
                        assertTrue(fake.faded(), () -> id + ": " + which + " cut at " + event.atMs()
                                + " left " + fake + " playing");
                    } else if (!matches) {
                        assertTrue(!fake.faded() || fake.fadedAt < event.atMs(), () -> id + ": " + which
                                + " cut at " + event.atMs() + " faded " + fake + " of the other direction");
                    }
                }
                for (Fake fake : book.activeSounds()) {
                    assertFalse(fake.faded(), () -> id + ": faded " + fake + " still in the book");
                    assertTrue(which != TabletMotion.Cut.ALL && !fake.dir.name().equals(which.name()),
                            () -> id + ": " + fake + " still in the book after " + which);
                }
            }
            List<String> actualPlays = new ArrayList<>();
            for (Fake fake : out.played) {
                actualPlays.add(fake.name + "/" + fake.dir);
            }
            assertEquals(expectedPlays, actualPlays, id + ": each motion sound starts exactly once");
            scenarios++;
        }
        assertEquals(26, scenarios);
        assertEquals(11, cuts, "the scenarios' cuts (reverse both ways, Esc in three phases, key finish, click jump, interrupts)");
    }

    @Test
    void escapeThenReopenCutsTheRightSounds() {
        // A-gun-esc-raise-reopen: open 0, Esc 500, reopen 800 (vectors.scenarios)
        JsonObject scenario = null;
        for (JsonElement element : TabletVectors.array("scenarios")) {
            if (element.getAsJsonObject().get("id").getAsString().equals("A-gun-esc-raise-reopen")) {
                scenario = element.getAsJsonObject();
            }
        }
        assertNotNull(scenario);
        FakeOutput out = new FakeOutput();
        TabletSoundBook<Fake> book = new TabletSoundBook<>(out);
        for (JsonElement ev : scenario.getAsJsonArray("events")) {
            TabletMotion.Event event = event(ev.getAsJsonObject());
            out.now = event.atMs();
            book.onEvent(event, event.atMs(), true, () -> true);
        }
        Fake holster = find(out, "holster", 0);
        Fake draw = find(out, "draw", 200);
        Fake stow = find(out, "stow", 500);
        Fake raise = find(out, "raise", 733.3333333333);
        Fake reDraw = find(out, "draw", 833.3333333333);
        assertEquals(500.0D, holster.fadedAt, 1e-9, "Esc while opening fades the opening sounds");
        assertEquals(500.0D, draw.fadedAt, 1e-9);
        assertEquals(800.0D, stow.fadedAt, 1e-9, "reopening while closing fades the closing sounds");
        assertEquals(800.0D, raise.fadedAt, 1e-9);
        assertFalse(reDraw.faded(), "the new opening keeps playing");
        assertEquals(List.of("draw", "grip", "power", "boot", "ready", "zoom"), book.activeNames());
    }

    private static Fake find(FakeOutput out, String name, double at) {
        for (Fake fake : out.played) {
            if (fake.name.equals(name) && Math.abs(fake.startedAt - at) < 1e-6) {
                return fake;
            }
        }
        throw new AssertionError("no " + name + " at " + at + " in " + out.played);
    }

    @Test
    void theSameSoundDoesNotRepeatWithin150Ms() {
        FakeOutput out = new FakeOutput();
        TabletSoundBook<Fake> book = new TabletSoundBook<>(out);
        assertTrue(book.play("boot", TabletCue.Dir.OPEN, 1000.0D, true));
        assertFalse(book.play("boot", TabletCue.Dir.OPEN, 1149.0D, true), "149 ms later");
        assertTrue(book.play("sleep", TabletCue.Dir.CLOSE, 1149.0D, true), "another sound plays");
        assertTrue(book.play("boot", TabletCue.Dir.OPEN, 1150.0D, true), "150 ms later");
        assertEquals(3, out.played.size());
        TabletSoundBook.Entry repeat = book.log().get(1);
        assertEquals(TabletSoundBook.Entry.Kind.REPEAT, repeat.kind());
        assertEquals("boot", repeat.name());
        // a reversal back over the same cue within 150 ms (preview a.sfx.noRepeatMs)
        book.onEvent(sound("power", TabletCue.Dir.OPEN, 2000, false), 2000.0D, true, () -> true);
        book.onEvent(cut("open", 2050), 2050.0D, true, () -> true);
        book.onEvent(sound("power", TabletCue.Dir.CLOSE, 2100, false), 2100.0D, true, () -> true);
        assertEquals(4, out.played.size(), "the close's power key is dropped 100 ms after the open's");
    }

    @Test
    void soundsOffPlaysNothingButKeepsTheLog() {
        FakeOutput out = new FakeOutput();
        TabletSoundBook<Fake> book = new TabletSoundBook<>(out);
        book.onEvent(sound("holster", TabletCue.Dir.OPEN, 0, false), 0.0D, false, () -> true);
        book.onEvent(sound("ready", TabletCue.Dir.OPEN, 1000, true), 1000.0D, false, () -> true);
        assertEquals(List.of(), out.played);
        assertEquals(List.of(), book.activeNames());
        List<TabletSoundBook.Entry> log = book.log();
        assertEquals(2, log.size());
        assertEquals(TabletSoundBook.Entry.Kind.PLAY, log.get(0).kind());
        assertFalse(log.get(0).audible());
        assertEquals("ready", log.get(1).name());
        assertEquals(0, book.cut(TabletMotion.Cut.ALL, 1100.0D));
    }

    @Test
    void readyWithoutALinkStartsTheChimeUntilTheLinkComesUp() {
        FakeOutput out = new FakeOutput();
        TabletSoundBook<Fake> book = new TabletSoundBook<>(out);
        boolean[] link = {false};
        book.onEvent(sound("ready", TabletCue.Dir.OPEN, 987, true), 987.0D, true, () -> link[0]);
        assertEquals(List.of(), out.played, "no ready without a snapshot");
        assertTrue(book.chime().active());
        assertEquals(TabletSoundBook.Entry.Kind.WAIT, book.log().get(0).kind());
        for (int frame = 0; 987 + frame * 20 < 2500; frame++) {
            double t = 987.0D + frame * 20.0D;
            out.now = t;
            book.tick(t, true, () -> link[0], TabletMotion.State.OPENING);
        }
        List<Double> ticks = new ArrayList<>();
        for (Fake fake : out.played) {
            assertEquals("search", fake.name);
            assertEquals(TabletCue.Dir.OPEN, fake.dir);
            ticks.add(fake.startedAt);
        }
        assertEquals(4, ticks.size(), "a tick at once, then every 500 ms: " + ticks);
        assertEquals(987.0D, ticks.get(0), 1e-9);
        link[0] = true;
        out.now = 2600.0D;
        book.tick(2600.0D, true, () -> link[0], TabletMotion.State.SHOWN);
        assertEquals("ready", out.played.get(out.played.size() - 1).name);
        assertFalse(book.chime().active());
        int count = out.played.size();
        book.tick(3200.0D, true, () -> link[0], TabletMotion.State.SHOWN);
        assertEquals(count, out.played.size(), "ready plays once");
    }

    @Test
    void closingStopsTheChimeWithoutReady() {
        FakeOutput out = new FakeOutput();
        TabletSoundBook<Fake> book = new TabletSoundBook<>(out);
        book.onEvent(sound("ready", TabletCue.Dir.OPEN, 0, true), 0.0D, true, () -> false);
        book.tick(0.0D, true, () -> false, TabletMotion.State.OPENING);
        assertEquals(1, out.played.size());
        book.onEvent(state(TabletMotion.State.CLOSING, 100), 100.0D, true, () -> false);
        assertFalse(book.chime().active(), "a close stops the search");
        book.tick(700.0D, true, () -> true, TabletMotion.State.CLOSING);
        assertEquals(1, out.played.size(), "no ready after the tablet went away");
        // a reopen with the link up plays ready straight away
        book.onEvent(sound("ready", TabletCue.Dir.OPEN, 900, true), 900.0D, true, () -> true);
        assertEquals("ready", out.played.get(1).name);
        assertFalse(book.chime().active());
    }

    @Test
    void linkIsOnlyAskedForReady() {
        TabletSoundBook<Fake> book = new TabletSoundBook<>(new FakeOutput());
        book.onEvent(sound("boot", TabletCue.Dir.OPEN, 0, false), 0.0D, true, () -> {
            throw new AssertionError("asked for the link of a plain cue");
        });
        book.tick(10.0D, true, () -> {
            throw new AssertionError("asked for the link with no chime running");
        }, TabletMotion.State.OPENING);
    }

    @Test
    void finishedSoundsLeaveTheBookAndStopAllFadesTheRest() {
        FakeOutput out = new FakeOutput();
        TabletSoundBook<Fake> book = new TabletSoundBook<>(out);
        out.now = 0.0D;
        book.play("holster", TabletCue.Dir.OPEN, 0.0D, true);
        out.now = 800.0D;
        book.play("boot", TabletCue.Dir.OPEN, 800.0D, true);
        book.tick(1001.0D, true, () -> true, TabletMotion.State.OPENING);
        assertEquals(List.of("boot"), book.activeNames(), "holster is long over");
        book.onEvent(sound("ready", TabletCue.Dir.OPEN, 1001, true), 1001.0D, true, () -> false);
        assertTrue(book.chime().active());
        book.stopAll(1100.0D);
        assertFalse(book.chime().active());
        assertEquals(List.of(), book.activeNames());
        assertTrue(out.played.get(1).faded());
        assertFalse(out.played.get(0).faded(), "a finished sound is not touched");
        TabletSoundBook.Entry last = book.log().get(book.log().size() - 1);
        assertEquals(TabletSoundBook.Entry.Kind.CUT, last.kind());
        assertEquals("all", last.name());
        assertNull(last.dir());
    }

    @Test
    void theLogKeepsTheLastLines() {
        TabletSoundBook<Fake> book = new TabletSoundBook<>(new FakeOutput());
        for (int i = 0; i < TabletSoundBook.LOG_SIZE + 10; i++) {
            book.play("search", TabletCue.Dir.OPEN, i * 200.0D, true);
        }
        List<TabletSoundBook.Entry> log = book.log();
        assertEquals(TabletSoundBook.LOG_SIZE, log.size());
        assertEquals((TabletSoundBook.LOG_SIZE + 9) * 200.0D, log.get(log.size() - 1).atMs(), 1e-9);
    }
}
