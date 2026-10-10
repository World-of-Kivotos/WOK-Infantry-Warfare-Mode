package com.wok.infantry.client.screen;

import com.wok.infantry.client.screen.TacticalLivery.Livery;
import com.wok.infantry.client.screen.TacticalShellLayout.Density;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What 0.5.0-beta.4's animation adds to {@link DeviceArt}: the drop shadow runs (faded in with the
 * wake), the clear-glass plate (scheme B's put-away device, preview {@code glass: 'clear'}), the
 * uncached build for the map frame and the faded backdrop. Every default stays pixel-identical.
 */
class DeviceArtAnimationTest {
    @ParameterizedTest(name = "{0}x{1}")
    @CsvSource({"320, 240", "480, 270", "480, 360", "640, 360", "960, 540"})
    void theShadowIsTheFirstRunsOfTheCaseAndNothingElse(int width, int height) {
        for (Livery livery : Livery.values()) {
            DeviceArt.Plate plate = DeviceArt.plate(width, height,
                    TacticalShellLayout.density(width, height), livery);
            int shadow = plate.shadowRuns();
            assertTrue(shadow > 0, "the case has a drop shadow");
            for (int index = shadow; index < plate.under().size(); index++) {
                assertNotEquals(DeviceArt.SHADOW, plate.under().color(index), "under run " + index);
            }
            for (int index = 0; index < plate.over().size(); index++) {
                assertNotEquals(DeviceArt.SHADOW, plate.over().color(index), "over run " + index);
            }
        }
    }

    @ParameterizedTest(name = "{0}x{1}")
    @CsvSource({"320, 240", "480, 270", "480, 360", "640, 360", "960, 540"})
    void theClearGlassPlateIsTheDeviceWithItsGlassOpeningHollowedOut(int width, int height) {
        for (Livery livery : Livery.values()) {
            DeviceArt.Plate plate = DeviceArt.plate(width, height,
                    TacticalShellLayout.density(width, height), livery);
            UiRect s = plate.layout().glass();
            int[] full = new int[width * height];
            plate.under().composite(full, width, height);
            plate.over().composite(full, width, height);
            for (int y = s.top(); y < s.bottom(); y++) {
                for (int x = s.left(); x < s.right(); x++) {
                    full[y * width + x] = 0;
                }
            }
            int[] clear = new int[width * height];
            plate.underClear().composite(clear, width, height);
            plate.overClear().composite(clear, width, height);
            assertArrayEquals(full, clear, livery + " " + width + "x" + height);
            // The recess lip around the opening stays (only S itself turns clear).
            int lip = (s.top() - 1) * width + s.left() + 3;
            assertEquals(0xFF, clear[lip] >>> 24, "lip above the glass");
        }
    }

    @Test
    void cuttingAHoleSplitsRectanglesInPainterOrder() {
        Random random = new Random(20261010L);
        int width = 40;
        int height = 30;
        for (int round = 0; round < 200; round++) {
            DeviceArt.Runs runs = new DeviceArt.Runs();
            for (int index = 0; index < 6; index++) {
                int l = random.nextInt(width);
                int t = random.nextInt(height);
                runs.add(l, t, l + 1 + random.nextInt(width - l), t + 1 + random.nextInt(height - t),
                        (random.nextInt(255) + 1) << 24 | random.nextInt(0x1000000));
            }
            int hl = random.nextInt(width);
            int ht = random.nextInt(height);
            UiRect hole = new UiRect(hl, ht, hl + random.nextInt(width - hl + 1),
                    ht + random.nextInt(height - ht + 1));
            int[] expected = new int[width * height];
            runs.composite(expected, width, height);
            for (int y = hole.top(); y < hole.bottom(); y++) {
                for (int x = hole.left(); x < hole.right(); x++) {
                    expected[y * width + x] = 0;
                }
            }
            int[] actual = new int[width * height];
            runs.minus(hole).composite(actual, width, height);
            assertArrayEquals(expected, actual, "round " + round);
        }
    }

    @Test
    void theUncachedBuildMatchesAndLeavesTheSharedCacheAlone() {
        DeviceArt.Plate cached = DeviceArt.plate(480, 360, Density.STANDARD, Livery.ACADEMY);
        for (int extra = 0; extra < 10; extra++) {
            DeviceArt.Plate frame = DeviceArt.buildUncached(300 + extra * 7, 220 + extra * 5,
                    Density.STANDARD, Livery.ACADEMY);
            assertEquals(300 + extra * 7, frame.layout().width());
        }
        assertSame(cached, DeviceArt.plate(480, 360, Density.STANDARD, Livery.ACADEMY),
                "ten frame sizes did not push the terminal's plate out");
        DeviceArt.Plate built = DeviceArt.buildUncached(480, 360, Density.STANDARD, Livery.ACADEMY);
        int[] a = new int[480 * 360];
        int[] b = new int[480 * 360];
        cached.under().composite(a, 480, 360);
        cached.over().composite(a, 480, 360);
        built.under().composite(b, 480, 360);
        built.over().composite(b, 480, 360);
        assertArrayEquals(a, b);
    }

    @Test
    void fadedShadowAndBackdropScaleTheirAlpha() {
        assertEquals(DeviceArt.SHADOW, DeviceArt.fadedShadow(1.0F));
        assertEquals(0, DeviceArt.fadedShadow(0.0F) >>> 24);
        assertEquals(60, DeviceArt.fadedShadow(0.5F) >>> 24, "0x78 · 0.5");
        assertEquals(0, DeviceArt.fadedShadow(Float.NaN) >>> 24);
        assertEquals(DeviceArt.WORLD_DIM, DeviceArt.backdropDim(1.0F));
        assertEquals(68, DeviceArt.backdropDim(0.5F) >>> 24, "0x88 · 0.5");
        assertEquals(DeviceArt.WORLD_DIM & 0xFFFFFF, DeviceArt.backdropDim(0.5F) & 0xFFFFFF);
        assertEquals(0, DeviceArt.backdropDim(-1.0F) >>> 24);
    }

    @Test
    void blankKeysDefaultToARaisedCapWithoutLed() {
        DeviceArt.BlankKey key = new DeviceArt.BlankKey(null, null, null);
        assertEquals(UiRect.EMPTY, key.cap());
        assertEquals(BezelKey.CapState.RAISED, key.state());
        assertEquals(UiRect.EMPTY, key.led());
    }
}
