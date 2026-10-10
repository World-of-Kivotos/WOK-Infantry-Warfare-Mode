package com.wok.infantry.client.screen;

import com.wok.infantry.client.screen.TacticalLivery.Livery;
import com.wok.infantry.client.screen.TacticalShellLayout.Density;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DeviceArt#rasterize(int, int, Density, Livery, DeviceArt.RasterOptions)}, the front face
 * of the animation's 3D device (IMPL_PLAN 4.3 {@code TabletFaceRasterTest}): the default options
 * are the plain image; without the shadow the image is cut-out clean (alpha 0 or 255) and the case
 * is unchanged; the specks touch only the speck bands and follow the screen's tiling; keys and the
 * link LED stay where they belong.
 */
class DeviceArtRasterOptionsTest {
    private static final int[][] SIZES = {{320, 240}, {480, 270}, {640, 360}, {960, 540}, {480, 360}};

    private static Density density(int w, int h) {
        return TacticalShellLayout.density(w, h);
    }

    @Test
    void defaultOptionsAreThePlainImage() {
        for (int[] size : SIZES) {
            for (Livery livery : Livery.values()) {
                Density d = density(size[0], size[1]);
                assertArrayEquals(DeviceArt.rasterize(size[0], size[1], d, livery),
                        DeviceArt.rasterize(size[0], size[1], d, livery, DeviceArt.RasterOptions.DEFAULT),
                        livery + " " + size[0] + "x" + size[1]);
            }
        }
    }

    @Test
    void withoutTheShadowEveryPixelIsClearOrOpaqueAndTheCaseIsUnchanged() {
        for (int[] size : SIZES) {
            int w = size[0];
            int h = size[1];
            Density d = density(w, h);
            int[] plain = DeviceArt.rasterize(w, h, d, Livery.ACADEMY);
            int[] face = DeviceArt.rasterize(w, h, d, Livery.ACADEMY,
                    new DeviceArt.RasterOptions(false, false, List.of(), 0));
            int opaque = 0;
            for (int i = 0; i < face.length; i++) {
                int a = face[i] >>> 24;
                assertTrue(a == 0 || a == 255, w + "x" + h + " pixel " + i + " alpha " + a);
                if ((plain[i] >>> 24) == 255) {
                    assertEquals(plain[i], face[i], w + "x" + h + " case pixel " + i);
                    opaque++;
                } else {
                    assertEquals(0, a, w + "x" + h + " shadow-only pixel " + i + " is clear");
                }
            }
            assertTrue(opaque > w * h / 2, "the case covers most of the plane");
        }
    }

    @Test
    void specksTouchOnlyTheSpeckBandsAsTheScreenTilesThem() {
        int w = 640;
        int h = 360;
        Density d = density(w, h);
        DeviceArt.Plate plate = DeviceArt.plate(w, h, d, Livery.CAESAR);
        int[] bare = DeviceArt.rasterize(w, h, d, Livery.CAESAR,
                new DeviceArt.RasterOptions(false, false, List.of(), 0));
        int[] specked = DeviceArt.rasterize(w, h, d, Livery.CAESAR,
                new DeviceArt.RasterOptions(false, true, List.of(), 0));
        // The expected image: under (no shadow), specks tiled from (0, 0), then over.
        int[] expected = new int[w * h];
        plate.under().composite(expected, w, h, plate.shadowRuns());
        int changed = 0;
        for (UiRect band : plate.speckBands()) {
            for (int y = band.top(); y < band.bottom(); y++) {
                for (int x = band.left(); x < band.right(); x++) {
                    int speck = DeviceArt.speck(Math.floorMod(x, DeviceArt.SPECK_TILE),
                            Math.floorMod(y, DeviceArt.SPECK_TILE));
                    if (speck != 0) {
                        expected[y * w + x] = DeviceArt.over(expected[y * w + x], speck);
                    }
                }
            }
        }
        plate.over().composite(expected, w, h);
        assertArrayEquals(expected, specked);
        for (int i = 0; i < bare.length; i++) {
            if (bare[i] != specked[i]) {
                int x = i % w;
                int y = i / w;
                boolean inBand = plate.speckBands().stream().anyMatch(b -> x >= b.left()
                        && x < b.right() && y >= b.top() && y < b.bottom());
                assertTrue(inBand, "speck outside the bands at " + x + "," + y);
                changed++;
            }
        }
        assertTrue(changed > 100, "specks were drawn: " + changed);
    }

    @Test
    void compactDevicesHaveNoSpecks() {
        int[] bare = DeviceArt.rasterize(480, 270, Density.COMPACT, Livery.NEUTRAL,
                new DeviceArt.RasterOptions(false, false, List.of(), 0));
        int[] specked = DeviceArt.rasterize(480, 270, Density.COMPACT, Livery.NEUTRAL,
                new DeviceArt.RasterOptions(false, true, List.of(), 0));
        assertArrayEquals(bare, specked);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"ACADEMY", "CAESAR", "NEUTRAL"})
    void linkLedChangesOnlyTheLedAndItsHalo(Livery livery) {
        int w = 640;
        int h = 360;
        Density d = density(w, h);
        TacticalShellLayout layout = TacticalShellLayout.compute(w, h, d);
        UiRect led = DeviceArt.linkLed(layout);
        int[] bare = DeviceArt.rasterize(w, h, d, livery,
                new DeviceArt.RasterOptions(false, true, List.of(), 0));
        int[] lit = DeviceArt.rasterize(w, h, d, livery,
                new DeviceArt.RasterOptions(false, true, List.of(), 0xFFF0A63A));
        int changed = 0;
        for (int i = 0; i < bare.length; i++) {
            if (bare[i] != lit[i]) {
                int x = i % w;
                int y = i / w;
                assertTrue(x >= led.left() - 1 && x < led.right() + 1 && y >= led.top() - 1
                        && y < led.bottom() + 1, "LED pixel outside its halo at " + x + "," + y);
                changed++;
            }
        }
        assertEquals((led.width() + 2) * (led.height() + 2), changed, "LED and its 1px halo");
        assertEquals(0xFFF0A63A, lit[led.top() * w + led.left()]);
    }

    @Test
    void blankKeysAreTheCapsTheScreenDrawsWithoutLabelsOrHatch() {
        int w = 640;
        int h = 360;
        Density d = density(w, h);
        DeviceSkin skin = Livery.ACADEMY.skin();
        UiRect cap = new UiRect(40, 336, 90, 350);
        UiRect led = UiRect.ofSize(62, 333, 6, 1);
        List<DeviceArt.BlankKey> keys = List.of(
                new DeviceArt.BlankKey(cap, BezelKey.CapState.DOWN, led),
                new DeviceArt.BlankKey(new UiRect(100, 336, 150, 350), BezelKey.CapState.DISABLED,
                        UiRect.EMPTY));
        int[] bare = DeviceArt.rasterize(w, h, d, Livery.ACADEMY,
                new DeviceArt.RasterOptions(false, true, List.of(), 0));
        int[] keyed = DeviceArt.rasterize(w, h, d, Livery.ACADEMY,
                new DeviceArt.RasterOptions(false, true, keys, 0));
        BezelKey.Cap down = BezelKey.cap(skin, BezelKey.CapState.DOWN);
        BezelKey.Cap off = BezelKey.cap(skin, BezelKey.CapState.DISABLED);
        assertEquals(down.face(), keyed[(cap.top() + 4) * w + cap.left() + 5], "pressed face");
        assertEquals(down.topLip(), keyed[(cap.top() + 1) * w + cap.left() + 5], "pressed upper lip");
        assertEquals(down.edge(), keyed[cap.top() * w + cap.left() + 5], "outline");
        assertEquals(bare[cap.top() * w + cap.left()], keyed[cap.top() * w + cap.left()],
                "clipped corner keeps the case");
        assertEquals(off.face(), keyed[340 * w + 120], "disabled face is flat (no hatch)");
        assertEquals(DeviceArt.LED_OFF, keyed[led.top() * w + led.left()], "page LED unlit");
        for (int i = 0; i < bare.length; i++) {
            if (bare[i] != keyed[i]) {
                int x = i % w;
                int y = i / w;
                boolean inKey = keys.stream().anyMatch(k -> k.cap().contains(x, y)
                        || k.led().contains(x, y));
                assertTrue(inKey, "key pixel outside its cap or LED at " + x + "," + y);
            }
        }
        // The 2D put-away device draws the same rectangles.
        DeviceArt.Runs runs = DeviceArt.blankKeyRuns(keys, skin);
        int[] fromRuns = bare.clone();
        runs.composite(fromRuns, w, h);
        assertArrayEquals(keyed, fromRuns);
    }
}
