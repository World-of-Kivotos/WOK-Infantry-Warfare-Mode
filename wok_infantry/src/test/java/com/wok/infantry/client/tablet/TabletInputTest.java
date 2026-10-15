package com.wok.infantry.client.tablet;

import com.wok.infantry.client.tablet.TabletInput.Delivery;
import com.wok.infantry.client.tablet.TabletMotion.Input;
import com.wok.infantry.client.tablet.TabletMotion.State;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Input while the tablet comes out on scheme B / the quick setting (DESIGN 2.5, 4.1–4.3). */
class TabletInputTest {
    private static final TabletUnits T640 = TabletVectors.termUnits("640x360");
    private static final TabletUnits T960X720 = TabletVectors.termUnits("960x720");

    @Test
    void keysAreClassifiedEscFirst() {
        assertEquals(Input.ESC, TabletInput.classifyKey(true, true, true, true));
        assertEquals(Input.TERMINAL, TabletInput.classifyKey(false, true, true, true));
        assertEquals(Input.MAP_KEY, TabletInput.classifyKey(false, false, true, true));
        assertEquals(Input.HOTBAR, TabletInput.classifyKey(false, false, false, true));
        assertEquals(Input.KEY, TabletInput.classifyKey(false, false, false, false));
    }

    @Test
    void otherKeysFinishTheOpeningUnlessHeldSinceItStarted() {
        for (Input input : new Input[]{Input.KEY, Input.HOTBAR, Input.MAP_KEY}) {
            assertTrue(TabletInput.finishes(TabletPath.B2D, State.OPENING, 0.3D, input,
                    TabletScreenKind.SQUAD, false), input + " finishes");
            assertFalse(TabletInput.finishes(TabletPath.B2D, State.OPENING, 0.3D, input,
                    TabletScreenKind.SQUAD, true), input + " held since the open (repeats)");
        }
        assertFalse(TabletInput.finishes(TabletPath.B2D, State.OPENING, 0.3D, Input.ESC,
                TabletScreenKind.SQUAD, false), "Esc closes instead");
        assertFalse(TabletInput.finishes(TabletPath.B2D, State.OPENING, 0.3D, Input.TERMINAL,
                TabletScreenKind.SQUAD, false), "the terminal key closes the squad page instead");
        assertFalse(TabletInput.finishes(TabletPath.B2D, State.OPENING, 0.3D, Input.TERMINAL,
                TabletScreenKind.TERMINAL, false), "the formation page ignores it");
        assertFalse(TabletInput.finishes(TabletPath.B2D, State.SHOWN, 1.0D, Input.KEY,
                TabletScreenKind.SQUAD, false), "nothing to finish");
        assertFalse(TabletInput.finishes(TabletPath.B2D, State.CLOSING, 0.3D, Input.KEY,
                TabletScreenKind.SQUAD, false), "never while closing");
    }

    @Test
    void onlyAnOpeningIsEverIntercepted() {
        TabletPath2D.TermFrame frame = TabletPath2D.termFrame(0.3D, T640, true, false);
        for (State state : new State[]{State.IDLE, State.SHOWN, State.CLOSING}) {
            for (Input input : new Input[]{Input.MOUSE_DOWN, Input.SCROLL, Input.DRAG}) {
                assertEquals(Delivery.PASS, TabletInput.decide(TabletPath.B2D, state, 0.3D, input,
                        TabletScreenKind.SQUAD, frame, null, 300, 200, 1, false), state + " " + input);
            }
        }
    }

    @Test
    void aPressBeforeTheScreenLightsUpIsSwallowedAndJumpsToTheEnd() {
        for (double p : new double[]{0.0D, 0.2D, 0.54D}) {
            TabletPath2D.TermFrame frame = TabletPath2D.termFrame(p, T640, true, false);
            for (Input input : new Input[]{Input.MOUSE_DOWN, Input.SCROLL, Input.DRAG}) {
                Delivery d = TabletInput.decide(TabletPath.B2D, State.OPENING, p, input,
                        TabletScreenKind.SQUAD, frame, null, 300, 200, 1, false);
                assertEquals(Delivery.BLOCK, d, "p=" + p + " " + input);
            }
        }
    }

    @Test
    void duringTheScanOnlyTheLitPartAndTheBezelReachTheScreen() {
        double p = 0.775D; // scan line half way down the glass
        TabletPath2D.TermFrame frame = TabletPath2D.termFrame(p, T640, true, false);
        assertEquals(TabletPath2D.TermGlass.SCAN, frame.glass());
        TabletPath2D.Box s = frame.s();
        int x = s.x() + s.w() / 2;
        // Above the scan line: lit, handed over unmoved (the device rests in place, dy 0).
        Delivery above = TabletInput.decide(TabletPath.B2D, State.OPENING, p, Input.MOUSE_DOWN,
                TabletScreenKind.SQUAD, frame, null, x, frame.scanY() - 1, 1, false);
        assertTrue(above.cancel() && above.deliver() && above.jump(), above.toString());
        assertEquals(x, above.x(), 1e-9);
        assertEquals(frame.scanY() - 1, above.y(), 1e-9);
        // Below the scan line: still dark glass, swallowed.
        assertEquals(Delivery.BLOCK, TabletInput.decide(TabletPath.B2D, State.OPENING, p,
                Input.MOUSE_DOWN, TabletScreenKind.SQUAD, frame, null, x, frame.scanY() + 1, 1,
                false));
        // The bezel keys below the glass are visible.
        Delivery bezel = TabletInput.decide(TabletPath.B2D, State.OPENING, p, Input.MOUSE_DOWN,
                TabletScreenKind.SQUAD, frame, null, x, s.bottom() + 3, 1, false);
        assertTrue(bezel.deliver(), "a bezel key under the glass is lit hardware");
    }

    @Test
    void theQuickSettingMovesThePressByTheDeviceOffsetInLayoutPixels() {
        double p = 0.7D;
        // GUI 1 with the minimum 2x: one layout pixel is two GUI pixels.
        TabletPath2D.TermFrame frame = TabletPath2D.termFrame(p, T960X720, true, true);
        assertTrue(frame.dy() > 0, "still sliding up");
        int f = T960X720.factor();
        assertEquals(2, f);
        double guiX = 400.0D;
        double guiY = 300.0D;
        Delivery d = TabletInput.decide(TabletPath.QUICK, State.OPENING, p, Input.MOUSE_DOWN,
                TabletScreenKind.SQUAD, frame, null, guiX, guiY, f, false);
        assertTrue(d.cancel() && d.deliver() && d.jump());
        assertEquals(guiX, d.x(), 1e-9);
        assertEquals(guiY - frame.dy() * 2.0D, d.y(), 1e-9, "dy layout pixels = 2·dy GUI pixels");
        double[] shifted = TabletInput.shift(10.0D, 20.0D, 3, 4, 2);
        assertEquals(4.0D, shifted[0], 1e-9);
        assertEquals(12.0D, shifted[1], 1e-9);
    }

    @Test
    void theMapPinsItsTopLeftToTheInnerScreenAndKeepsPressesOnTheFrame() {
        TabletUnits map = TabletVectors.mapUnits("640x360");
        double p = 0.57D; // hold phase: inner screen at rest (0.8 × 0.8)
        TabletPath2D.MapFrame frame = TabletPath2D.mapFrame(p, map, T640, true, false);
        TabletPath2D.Box inner = frame.inner();
        assertTrue(inner.x() > 0 && inner.y() > 0);
        double x = inner.x() + 10.5D;
        double y = inner.y() + 7.25D;
        Delivery in = TabletInput.decide(TabletPath.B2D, State.OPENING, p, Input.MOUSE_DOWN,
                TabletScreenKind.FULLSCREEN, null, frame, x, y, 2, false);
        assertTrue(in.cancel() && in.deliver() && in.jump());
        assertEquals(10.5D, in.x(), 1e-9, "the map is in GUI pixels, never 2x");
        assertEquals(7.25D, in.y(), 1e-9);
        Delivery out = TabletInput.decide(TabletPath.B2D, State.OPENING, p, Input.MOUSE_DOWN,
                TabletScreenKind.FULLSCREEN, null, frame, inner.x() - 2, y, 2, false);
        assertEquals(Delivery.BLOCK, out, "the frame and the world around it");
        // While the sleep layer still covers the inner screen, the press is swallowed.
        TabletPath2D.MapFrame early = TabletPath2D.mapFrame(0.35D, map, T640, true, false);
        assertEquals(Delivery.BLOCK, TabletInput.decide(TabletPath.B2D, State.OPENING, 0.35D,
                Input.MOUSE_DOWN, TabletScreenKind.FULLSCREEN, null, early,
                early.inner().x() + 5, early.inner().y() + 5, 1, false));
    }

    @Test
    void aDragOfAButtonHeldBeforeTheOpenIsSwallowedWithoutAJump() {
        TabletPath2D.TermFrame frame = TabletPath2D.termFrame(0.3D, T640, true, false);
        assertEquals(Delivery.SWALLOW, TabletInput.decide(TabletPath.B2D, State.OPENING, 0.3D,
                Input.DRAG, TabletScreenKind.SQUAD, frame, null, 10, 10, 1, true));
        assertFalse(Delivery.SWALLOW.jump());
        assertEquals(Delivery.BLOCK, TabletInput.decide(TabletPath.B2D, State.OPENING, 0.3D,
                Input.DRAG, TabletScreenKind.SQUAD, frame, null, 10, 10, 1, false));
    }

    @Test
    void aReleaseIsNeverIntercepted() {
        TabletPath2D.TermFrame frame = TabletPath2D.termFrame(0.3D, T640, true, false);
        assertEquals(Delivery.PASS, TabletInput.decide(TabletPath.B2D, State.OPENING, 0.3D,
                Input.MOUSE_UP, TabletScreenKind.SQUAD, frame, null, 10, 10, 1, false));
    }
}
