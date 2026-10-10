package com.wok.infantry.client.tablet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wok.infantry.client.tablet.TabletMotion.Event;
import com.wok.infantry.client.tablet.TabletMotion.State;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.wok.infantry.client.tablet.TabletVectors.near;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link TabletMotion} against the preview: phase names (42 groups), the input table (1170 rows),
 * enter / leave tables, remap, and 26 state-machine scenarios replayed frame by frame with their
 * state, sound and cut events. Plus DESIGN 6.6: the reopen window, open / close spam and
 * continuity of reversals.
 */
class TabletMotionTest {
    private static final double DT = TabletVectors.DT;

    @Test
    void phaseNamesMatchThePreview() {
        int groups = 0;
        for (JsonElement element : TabletVectors.array("phases")) {
            JsonObject rec = element.getAsJsonObject();
            String pathName = rec.get("path").getAsString();
            if (pathName.equals("NOW")) {
                continue;
            }
            TabletPath path = TabletVectors.path(pathName);
            TabletScreenKind screen = TabletVectors.screen(rec.get("screen").getAsString());
            int dir = rec.get("dir").getAsInt();
            TabletHand hand = TabletHand.byPreviewName(rec.get("hand").getAsString());
            for (JsonElement ne : rec.getAsJsonArray("names")) {
                JsonArray pair = ne.getAsJsonArray();
                double p = pair.get(0).getAsDouble();
                assertEquals(pair.get(1).getAsString(), TabletMotion.phaseName(path, dir, p, hand, screen),
                        path + " " + screen + " dir " + dir + " " + hand + " p=" + p);
            }
            groups++;
        }
        assertEquals(36, groups, "42 groups minus the 6 of the preview-only NOW path");
    }

    private static TabletMotion.Input input(String name) {
        return switch (name) {
            case "esc" -> TabletMotion.Input.ESC;
            case "terminal" -> TabletMotion.Input.TERMINAL;
            case "mapKey" -> TabletMotion.Input.MAP_KEY;
            case "key" -> TabletMotion.Input.KEY;
            case "hotbar" -> TabletMotion.Input.HOTBAR;
            case "mouseDown" -> TabletMotion.Input.MOUSE_DOWN;
            case "scroll" -> TabletMotion.Input.SCROLL;
            case "drag" -> TabletMotion.Input.DRAG;
            case "mouseUp" -> TabletMotion.Input.MOUSE_UP;
            case "mouseMove" -> TabletMotion.Input.MOUSE_MOVE;
            default -> throw new IllegalArgumentException(name);
        };
    }

    private static final Map<String, TabletMotion.Effect> EFFECTS = Map.of(
            "none", TabletMotion.Effect.NONE, "close", TabletMotion.Effect.CLOSE,
            "finish", TabletMotion.Effect.FINISH, "jumpShown", TabletMotion.Effect.JUMP_SHOWN,
            "interrupt", TabletMotion.Effect.INTERRUPT, "reopen", TabletMotion.Effect.REOPEN,
            "pause", TabletMotion.Effect.PAUSE, "open", TabletMotion.Effect.OPEN);

    @Test
    void inputTableMatchesThePreview() {
        JsonArray table = TabletVectors.object("input").getAsJsonArray("table");
        assertEquals(1170, table.size());
        for (JsonElement element : table) {
            JsonArray row = element.getAsJsonArray();
            TabletPath path = TabletVectors.path(row.get(0).getAsString());
            TabletScreenKind screen = TabletVectors.screen(row.get(1).getAsString());
            State state = State.valueOf(row.get(2).getAsString());
            TabletMotion.Input in = input(row.get(3).getAsString());
            double p = row.get(4).getAsDouble();
            boolean visible = row.get(5).getAsBoolean();
            TabletMotion.InputDecision d = TabletMotion.inputAction(path, state, p, in, screen, visible);
            String what = row.toString();
            assertEquals(row.get(6).getAsBoolean(), d.intercept(), what + " intercept");
            assertEquals(row.get(7).getAsString(), d.deliver().name().toLowerCase(Locale.ROOT), what + " deliver");
            assertEquals(EFFECTS.get(row.get(8).getAsString()), d.effect(), what + " effect");
        }
    }

    private static TabletMotion.Kind kind(JsonElement element) {
        if (TabletVectors.isNull(element)) {
            return null;
        }
        return element.getAsString().equals("tablet") ? TabletMotion.Kind.TABLET : TabletMotion.Kind.OTHER;
    }

    @Test
    void enterLeaveAndRemapMatchThePreview() {
        JsonObject input = TabletVectors.object("input");
        for (JsonElement element : input.getAsJsonArray("enter")) {
            JsonArray row = element.getAsJsonArray();
            assertEquals(TabletMotion.Enter.valueOf(row.get(3).getAsString()),
                    TabletMotion.enterAction(kind(row.get(0)), kind(row.get(1)), row.get(2).getAsBoolean()),
                    row.toString());
        }
        for (JsonElement element : input.getAsJsonArray("leave")) {
            JsonArray row = element.getAsJsonArray();
            assertEquals(row.get(2).getAsString().equals("CLOSE"),
                    TabletMotion.leaveAction(kind(row.get(0)), kind(row.get(1))), row.toString());
        }
        for (JsonElement element : input.getAsJsonArray("remap")) {
            JsonObject rec = element.getAsJsonObject();
            JsonObject rect = rec.getAsJsonObject("rect");
            TabletMotion.Remap r = TabletMotion.remapPoint(rec.get("mx").getAsDouble(), rec.get("my").getAsDouble(),
                    rect.get("x").getAsDouble(), rect.get("y").getAsDouble(), rect.get("scale").getAsDouble(),
                    rec.get("W").getAsDouble(), rec.get("H").getAsDouble());
            JsonObject out = rec.getAsJsonObject("out");
            near(out.get("x"), r.x(), () -> "remap x");
            near(out.get("y"), r.y(), () -> "remap y");
            assertEquals(out.get("inside").getAsBoolean(), r.inside());
        }
    }

    // ---- scenarios ------------------------------------------------------------------------------

    static TabletMotion.Config config(JsonObject cfg) {
        return new TabletMotion.Config(TabletMode.valueOf(cfg.get("mode").getAsString()),
                cfg.get("degrade").getAsBoolean(), TabletHand.byPreviewName(cfg.get("hand").getAsString()),
                cfg.get("capture").getAsBoolean(), TabletVectors.screen(cfg.get("screen").getAsString()));
    }

    private static TabletMotion.Interrupt cause(String name) {
        return TabletMotion.Interrupt.valueOf(name.toUpperCase(Locale.ROOT));
    }

    /** Replays one scenario: at every frame, actions with at ≤ t first, then update(t). */
    static List<Object[]> replay(JsonObject scenario, List<Event> events, List<TabletMotion.PoseContext> poses,
                                 TabletMotion motion) {
        List<JsonObject> script = new ArrayList<>();
        scenario.getAsJsonArray("script").forEach(e -> script.add(e.getAsJsonObject()));
        script.sort((a, b) -> Double.compare(a.get("at").getAsDouble(), b.get("at").getAsDouble()));
        double endMs = scenario.get("endMs").getAsDouble();
        List<Object[]> rows = new ArrayList<>();
        int si = 0;
        for (int i = 0; ; i++) {
            double t = i * DT;
            if (t > endMs + 1e-9) {
                break;
            }
            while (si < script.size() && script.get(si).get("at").getAsDouble() <= t + 1e-9) {
                JsonObject a = script.get(si++);
                switch (a.get("do").getAsString()) {
                    case "open" -> motion.open(t);
                    case "close" -> motion.close(t);
                    case "finish" -> motion.finish(t);
                    case "jumpShown" -> motion.jumpShown(t);
                    case "interrupt" -> motion.interrupt(t, cause(a.get("cause").getAsString()));
                    case "loseHand" -> motion.loseHandRender(t);
                    case "enter" -> motion.onScreenOpening(kind(a.get("from")), kind(a.get("to")),
                            !a.has("inWorld") || a.get("inWorld").getAsBoolean(), t);
                    case "reset" -> motion.reset(t, a.has("completed") && a.get("completed").getAsBoolean());
                    default -> throw new IllegalArgumentException(a.toString());
                }
            }
            motion.update(t);
            events.addAll(motion.drainEvents());
            poses.add(motion.poseContext());
            rows.add(new Object[]{i, t, motion.state(), motion.p(), motion.dir(), motion.curve(),
                    motion.path(), motion.fast(), motion.phaseName()});
        }
        return rows;
    }

    @Test
    void scenariosMatchThePreviewFrameByFrame() {
        int scenarios = 0;
        for (JsonElement element : TabletVectors.array("scenarios")) {
            JsonObject sc = element.getAsJsonObject();
            String id = sc.get("id").getAsString();
            TabletMotion motion = new TabletMotion(config(sc.getAsJsonObject("cfg")));
            List<Event> events = new ArrayList<>();
            List<TabletMotion.PoseContext> poses = new ArrayList<>();
            List<Object[]> rows = replay(sc, events, poses, motion);
            JsonArray expectedRows = sc.getAsJsonArray("rows");
            assertEquals(expectedRows.size(), rows.size(), id + " frames");
            for (int i = 0; i < rows.size(); i++) {
                JsonArray e = expectedRows.get(i).getAsJsonArray();
                Object[] a = rows.get(i);
                String what = id + " frame " + i;
                near(e.get(1), (double) a[1], () -> what + " t");
                assertEquals(e.get(2).getAsString(), ((State) a[2]).name(), what + " state");
                near(e.get(3), (double) a[3], () -> what + " p");
                assertEquals(e.get(4).getAsInt(), (int) a[4], what + " dir");
                assertEquals(e.get(5).getAsString(), ((TabletMotion.Curve) a[5]).name().toLowerCase(Locale.ROOT),
                        what + " curve");
                assertEquals(TabletVectors.str(e.get(6)), a[6] == null ? null : ((TabletPath) a[6]).name(),
                        what + " path");
                assertEquals(e.get(7).getAsBoolean(), (boolean) a[7], what + " fast");
                assertEquals(e.get(8).getAsString(), a[8], what + " phase");
                TabletMotion.PoseContext pc = poses.get(i);
                if (TabletVectors.isNull(e.get(9))) {
                    assertTrue(Double.isNaN(pc.captureFromP()), what + " no capture start");
                } else {
                    near(e.get(9), pc.captureFromP(), () -> what + " captureFromP");
                    assertEquals(e.get(10).getAsBoolean(), pc.capture(), what + " capture");
                }
            }
            JsonArray expectedEvents = sc.getAsJsonArray("events");
            assertEquals(expectedEvents.size(), events.size(), () -> id + " events " + describe(events));
            for (int i = 0; i < events.size(); i++) {
                JsonObject e = expectedEvents.get(i).getAsJsonObject();
                Event a = events.get(i);
                String what = id + " event " + i + " " + a;
                assertEquals(e.get("type").getAsString(), a.type().name().toLowerCase(Locale.ROOT), what);
                assertEquals(e.get("name").getAsString(), a.name(), what);
                near(e.get("at"), a.atMs(), () -> what + " at");
                assertEquals(TabletVectors.str(e.get("dir")), a.dir() == null ? null : a.dir().previewName(), what);
                assertEquals(!TabletVectors.isNull(e.get("link")) && e.get("link").getAsBoolean(), a.link(), what);
            }
            scenarios++;
        }
        assertEquals(26, scenarios);
    }

    private static String describe(List<Event> events) {
        StringBuilder out = new StringBuilder();
        for (Event e : events) {
            out.append(e.type()).append(':').append(e.name()).append('@')
                    .append(String.format(Locale.ROOT, "%.1f", e.atMs())).append(' ');
        }
        return out.toString();
    }

    // ---- DESIGN 6.6 -----------------------------------------------------------------------------

    private static void runUntil(TabletMotion m, double from, double to) {
        for (double t = from; t <= to + 1e-9; t += DT) {
            m.update(t);
        }
    }

    @Test
    void reopeningWithinTwoSecondsIsFasterAndLaterIsNot() {
        TabletMotion m = new TabletMotion();
        m.open(0);
        runUntil(m, 0, 1400);
        assertEquals(State.SHOWN, m.state());
        m.close(1500);
        runUntil(m, 1500, 2500);
        assertEquals(State.IDLE, m.state());
        // the close ended at 1500 + 900 = 2400
        m.open(2400 + 1999);
        assertTrue(m.fast(), "reopened 1999 ms after the close finished");
        assertEquals(800.0D, m.durMs(), 1e-9);
        m.reset(5000, true);
        m.open(5000 + 2000);
        assertFalse(m.fast(), "2 s after a completed reset");
        assertEquals(1333.3333333333, m.durMs(), 1e-6);
        // an interrupted opening does not count as a completed close
        TabletMotion n = new TabletMotion();
        n.open(0);
        n.reset(100, false);
        n.open(200);
        assertFalse(n.fast());
    }

    @Test
    void openCloseSpamKeepsOrderAndProgressContinuous() {
        TabletMotion m = new TabletMotion();
        double previous = 0.0D;
        double t = 0.0D;
        for (int round = 0; round < 5; round++) {
            m.open(t);
            for (int i = 0; i < 15; i++, t += DT) {
                m.update(t);
                assertTrue(Math.abs(m.p() - previous) <= 0.06D, "no jump while opening (round " + round + ")");
                previous = m.p();
            }
            m.close(t);
            boolean wake = previous > 0.61D + 1e-9 && previous < 0.74D - 1e-9;
            assertEquals(wake ? 0.61D : previous, m.p(), 1e-9,
                    "a reversal starts from the current p (the wake band from READ)");
            previous = m.p();
            for (int i = 0; i < 6; i++, t += DT) {
                m.update(t);
                assertTrue(Math.abs(m.p() - previous) <= 0.06D, "no jump while closing (round " + round + ")");
                previous = m.p();
            }
        }
        List<String> states = new ArrayList<>();
        for (Event e : m.drainEvents()) {
            if (e.type() == Event.Type.STATE) {
                states.add(e.name());
            }
        }
        for (int i = 0; i < states.size(); i++) {
            assertEquals(i % 2 == 0 ? "OPENING" : "CLOSING", states.get(i), "alternating states " + states);
        }
    }

    @Test
    void reversalsContinueFromTheCurrentProgressOutsideTheWakeBand() {
        for (TabletHand hand : TabletHand.values()) {
            for (double at = DT; at < 1300; at += 3 * DT) {
                TabletMotion m = new TabletMotion(TabletMotion.Config.DEFAULT.withHand(hand));
                m.open(0);
                runUntil(m, 0, at);
                if (m.state() != State.OPENING) {
                    break;
                }
                double before = m.p();
                TabletMotion.Curve curve = m.curve();
                m.close(at + DT);
                boolean wake = before > 0.61 + 1e-9 && before < 0.74 - 1e-9;
                assertEquals(wake ? 0.61D : before, m.p(), 1e-9, hand + " close at p=" + before);
                assertEquals(curve, m.curve(), "a reversal keeps the curve");
                assertEquals(!wake, m.closeFrom().capture(), "C1 captures unless closed in the wake band");
                // reopening mid-close continues forward from there, fast
                double mid = m.p();
                m.update(at + 2 * DT);
                double p2 = m.p();
                m.open(at + 2 * DT);
                assertEquals(p2, m.p(), 1e-9, "reopen from p=" + mid);
                assertTrue(m.fast());
                assertEquals(curve, m.curve());
            }
        }
    }

    @Test
    void fromShownTheCloseUsesTheCloseCurveAndCapturesTheWholeScreen() {
        TabletMotion m = new TabletMotion();
        m.open(0);
        runUntil(m, 0, 1400);
        m.close(1500);
        assertEquals(TabletMotion.Curve.CLOSE, m.curve());
        assertEquals(1.0D, m.closeFrom().p(), 0.0D);
        assertTrue(m.poseContext().capture());
        // without C1 the old close starts at READ
        TabletMotion old = new TabletMotion(TabletMotion.Config.DEFAULT.withCapture(false));
        old.open(0);
        runUntil(old, 0, 1400);
        old.close(1500);
        assertEquals(0.61D, old.p(), 1e-12);
        assertFalse(old.poseContext().capture());
    }

    @Test
    void cutsFollowReversalsInterruptsAndFastForwards() {
        // open → close → open: cut open, then cut close; an interrupted close: cut all; finish / jump: cut open
        TabletMotion m = new TabletMotion();
        m.open(0);
        m.update(300);
        m.close(316);
        m.update(400);
        m.open(416);
        m.update(500);
        m.close(516);
        m.interrupt(600, TabletMotion.Interrupt.FIRE);
        List<String> cuts = new ArrayList<>();
        for (Event e : m.drainEvents()) {
            if (e.type() == Event.Type.CUT) {
                cuts.add(e.name());
            }
        }
        assertEquals(List.of("open", "close", "open", "all"), cuts);
        assertEquals(State.IDLE, m.state());
        TabletMotion f = new TabletMotion();
        f.open(0);
        f.update(100);
        f.finish(116);
        f.update(250);
        assertEquals(State.SHOWN, f.state(), "finished within 100 ms");
        TabletMotion j = new TabletMotion();
        j.open(0);
        j.update(100);
        j.jumpShown(116);
        assertEquals(State.SHOWN, j.state());
        List<String> sounds = new ArrayList<>();
        List<String> jumpCuts = new ArrayList<>();
        for (Event e : j.drainEvents()) {
            if (e.type() == Event.Type.SOUND && e.atMs() == 116) {
                sounds.add(e.name());
            }
            if (e.type() == Event.Type.CUT) {
                jumpCuts.add(e.name());
            }
        }
        assertEquals(List.of("ready"), sounds, "a jump plays only ready");
        assertEquals(List.of("open"), jumpCuts);
    }

    @Test
    void quickAndOffPlayNoSoundsAndOpeningIgnoresWeakInterrupts() {
        for (TabletMode mode : new TabletMode[]{TabletMode.QUICK, TabletMode.OFF}) {
            TabletMotion m = new TabletMotion(TabletMotion.Config.DEFAULT.withMode(mode));
            m.open(0);
            runUntil(m, 0, 200);
            m.close(300);
            runUntil(m, 300, 500);
            for (Event e : m.drainEvents()) {
                assertTrue(e.type() != Event.Type.SOUND, mode + " plays no sounds");
            }
            assertEquals(State.IDLE, m.state(), mode.name());
        }
        TabletMotion m = new TabletMotion();
        m.open(0);
        assertFalse(m.interrupt(10, TabletMotion.Interrupt.FIRE));
        assertFalse(m.interrupt(10, TabletMotion.Interrupt.HOTBAR));
        assertTrue(m.interrupt(10, TabletMotion.Interrupt.VEHICLE));
        assertEquals(State.IDLE, m.state());
    }

    @Test
    void switchPathRestartsAFreshOpeningOnBOnly() {
        TabletMotion m = new TabletMotion();
        m.open(0);
        m.update(DT);
        assertTrue(m.switchPath(TabletPath.B2D, DT));
        assertEquals(TabletPath.B2D, m.path());
        assertEquals(0.0D, m.p(), 0.0D);
        assertTrue(m.config().degrade());
        m.update(DT + 200);
        assertEquals(State.SHOWN, m.state(), "B opens in 200 ms");
        TabletMotion late = new TabletMotion();
        late.open(0);
        late.update(5 * DT);
        assertFalse(late.switchPath(TabletPath.B2D, 5 * DT), "too late: lose the hand pass instead");
        assertTrue(late.loseHandRender(5 * DT));
        assertEquals(State.SHOWN, late.state());
        assertNull(new TabletMotion().closeFrom());
    }
}
