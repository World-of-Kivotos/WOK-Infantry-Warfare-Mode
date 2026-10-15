package com.wok.infantry.client.tablet;

import com.google.gson.JsonObject;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fade-out of a cut sound (DESIGN 3.6 S5, IMPL_PLAN D5): a 12 ms time constant sampled once
 * per sound-engine tick, stopped on the first tick from 60 ms on that follows an inaudible one —
 * never straight from an audible volume. {@link TabletSoundInstance} only forwards to the
 * {@link TabletSoundFade.Fader} (its sound event needs a bootstrapped game, so it is not built
 * here).
 */
class TabletSoundFadeTest {
    private static final double TICK = 50.0D;
    private static final double START = TabletAnimationModel.SFX_VOLUME;

    @Test
    void constantsAreThePreviews() {
        JsonObject sfx = TabletVectors.object("spec").getAsJsonObject("a").getAsJsonObject("sfx");
        assertEquals(sfx.getAsJsonObject("cut").get("tauMs").getAsDouble(), TabletAnimationModel.SFX_CUT_TAU_MS, 0.0D);
        assertEquals(sfx.getAsJsonObject("cut").get("stopMs").getAsDouble(), TabletAnimationModel.SFX_CUT_STOP_MS, 0.0D);
        assertEquals(sfx.get("volume").getAsDouble(), TabletAnimationModel.SFX_VOLUME, 0.0D);
        assertEquals(sfx.get("noRepeatMs").getAsDouble(), TabletAnimationModel.SFX_NO_REPEAT_MS, 0.0D);
    }

    @Test
    void theSoundClassIsTheTickableOne() {
        assertSame(AbstractTickableSoundInstance.class, TabletSoundInstance.class.getSuperclass(),
                "one tickable sound class for every tablet sound (D5)");
    }

    @Test
    void fadeCurve() {
        assertEquals(1.0D, TabletSoundFade.gain(0.0D), 0.0D);
        assertEquals(Math.exp(-1.0D), TabletSoundFade.gain(12.0D), 1e-12);
        assertEquals(new TabletSoundFade.Step(Math.exp(-2.5D), false), TabletSoundFade.step(30.0D, 1.0D));
        assertEquals(new TabletSoundFade.Step(Math.exp(-50.0D / 12.0D), false), TabletSoundFade.step(50.0D, 0.5D));
        assertEquals(TabletSoundFade.Step.STOP, TabletSoundFade.step(60.0D, 0.0155D),
                "stops from 60 ms on once the last tick left it inaudible");
        assertEquals(new TabletSoundFade.Step(0.0D, false), TabletSoundFade.step(60.0D, 0.43D),
                "still audible: silent first, stop on the next tick");
        assertEquals(new TabletSoundFade.Step(0.0D, false), TabletSoundFade.step(75.0D, 1.0D),
                "late first tick: silent first, stop on the next");
        assertEquals(1.0D, TabletSoundFade.step(-5.0D, 1.0D).gain(), 0.0D, "clock skew counts as 0");
    }

    @Test
    void anUncutSoundKeepsItsVolume() {
        TabletSoundFade.Fader fader = new TabletSoundFade.Fader(START);
        for (int i = 0; i < 20; i++) {
            assertFalse(fader.tick(i * TICK));
        }
        assertFalse(fader.fading());
        assertEquals(START, fader.volume(), 0.0D);
    }

    @Test
    void aCutFadesThenStops() {
        TabletSoundFade.Fader fader = new TabletSoundFade.Fader(START);
        fader.fadeOut(1000.0D);
        assertTrue(fader.fading());
        assertFalse(fader.tick(1010.0D));
        assertEquals(START * Math.exp(-10.0D / 12.0D), fader.volume(), 1e-12);
        fader.fadeOut(1040.0D);
        assertFalse(fader.tick(1060.0D), "43% on the last tick: silent first (a second cut keeps the first time)");
        assertEquals(0.0D, fader.volume(), 0.0D);
        assertTrue(fader.tick(1110.0D));
        assertTrue(fader.stopped());
        assertTrue(fader.tick(1160.0D), "stays stopped");
    }

    @Test
    void aCutRightBeforeATickStopsOnTheThird() {
        TabletSoundFade.Fader fader = new TabletSoundFade.Fader(START);
        fader.fadeOut(0.0D);
        assertFalse(fader.tick(2.0D));
        assertFalse(fader.tick(52.0D));
        assertEquals(START * Math.exp(-52.0D / 12.0D), fader.volume(), 1e-12);
        assertTrue(fader.tick(102.0D), "1.3% on the last tick: stopped");
    }

    @Test
    void aLateFirstTickGoesSilentBeforeStopping() {
        TabletSoundFade.Fader fader = new TabletSoundFade.Fader(START);
        fader.fadeOut(0.0D);
        assertFalse(fader.tick(90.0D));
        assertEquals(0.0D, fader.volume(), 0.0D);
        assertTrue(fader.tick(140.0D));
    }

    @Test
    void everyCutPhaseStopsWithinThreeTicksAndNeverFromAnAudibleVolume() {
        for (double phase = 0.0D; phase < TICK; phase += 0.5D) {
            TabletSoundFade.Fader fader = new TabletSoundFade.Fader(START);
            fader.fadeOut(0.0D);
            double before = START;
            int ticks = 0;
            double at = phase;
            while (true) {
                ticks++;
                boolean stop = fader.tick(at);
                if (stop) {
                    assertTrue(before < START * TabletSoundFade.SILENT,
                            "phase " + phase + ": stopped from " + before);
                    assertTrue(at >= TabletAnimationModel.SFX_CUT_STOP_MS, "phase " + phase + " stopped at " + at);
                    break;
                }
                assertTrue(fader.volume() < before || ticks == 1 && phase == 0.0D,
                        "phase " + phase + " tick " + ticks + " did not lower the volume");
                before = fader.volume();
                at += TICK;
                assertTrue(ticks < 3, "phase " + phase + " still playing after " + ticks + " ticks");
            }
            assertTrue(ticks <= 3, "phase " + phase);
        }
    }
}
