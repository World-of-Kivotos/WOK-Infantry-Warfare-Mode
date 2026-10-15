package com.wok.infantry.client.screen;

import com.google.gson.JsonObject;
import com.wok.infantry.client.screen.TacticalLivery.Livery;
import com.wok.infantry.client.screen.TacticalShellLayout.Density;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The procedural D2 device ({@link DeviceArt}) apart from the golden images. */
class DeviceArtTest {
    private static final String TEXTURES = TacticalIconAtlasTest.TEXTURES;

    @ParameterizedTest(name = "{0}x{1}")
    @CsvSource({"320, 240", "427, 240", "640, 336", "480, 360", "960, 540", "1920, 1080"})
    void cachedDeviceStaysWithinTheFillBudget(int width, int height) {
        for (Livery livery : Livery.values()) {
            DeviceArt.Plate plate = DeviceArt.plate(width, height,
                    TacticalShellLayout.density(width, height), livery);
            assertTrue(plate.fills() <= 1500, livery + " " + width + "x" + height + ": "
                    + plate.fills() + " fills per frame");
            // The sheen is one or two runs per display row, plus three edge lines.
            assertTrue(plate.glass().size() <= 2 * plate.layout().display().height() + 3,
                    "glass overlay " + plate.glass().size());
        }
    }

    @Test
    void platesAreCachedPerSizeClassAndLivery() {
        DeviceArt.Plate first = DeviceArt.plate(480, 360, Density.STANDARD, Livery.CAESAR);

        assertSame(first, DeviceArt.plate(480, 360, Density.STANDARD, Livery.CAESAR));
        assertNotSame(first, DeviceArt.plate(480, 360, Density.STANDARD, Livery.ACADEMY));
        assertNotSame(first, DeviceArt.plate(480, 360, Density.ROOMY, Livery.CAESAR));
        assertEquals(TacticalShellLayout.compute(480, 360, Density.STANDARD), first.layout());
    }

    @Test
    void rasterizeLeavesTheWorldClearAndTheGlassDark() {
        int[] argb = DeviceArt.rasterize(480, 360, Density.STANDARD, Livery.NEUTRAL);
        TacticalShellLayout layout = TacticalShellLayout.compute(480, 360);
        UiRect display = layout.display();

        assertEquals(0, argb[0] >>> 24, "the world corner stays transparent");
        assertEquals(DeviceArt.GLASS, argb[display.centerY() * 480 + display.centerX()],
                "the lit display is not part of the cached device");
        int caseX = layout.device().left() + 6;
        int caseY = layout.device().centerY();
        assertEquals(DeviceSkin.NEUTRAL.caseColor(), argb[caseY * 480 + caseX], "case face");
    }

    @Test
    void sourceOverMatchesTheCanvasBlend() {
        assertEquals(0xFF102030, DeviceArt.over(0xFFFFFFFF, 0xFF102030), "opaque replaces");
        assertEquals(0xFF102030, DeviceArt.over(0xFF102030, 0x00FFFFFF), "clear keeps");
        assertEquals(0x78000000, DeviceArt.over(0, 0x78000000), "over nothing keeps its alpha");
        // 0x80 over black: 255 * 128 / 255 = 128.
        assertEquals(0xFF808080, DeviceArt.over(0xFF000000, 0x80FFFFFF));
        // 0x48 halo of a white LED on black: 255 * 72 / 255 = 72.
        assertEquals(0xFF484848, DeviceArt.over(0xFF000000, 0x48FFFFFF));
    }

    /** The glass overlay runs equal the preview's per-pixel glassOverlay. */
    @ParameterizedTest(name = "{0}x{1}")
    @CsvSource({"320, 240", "480, 360", "960, 540"})
    void glassOverlayMatchesThePerPixelRule(int width, int height) {
        TacticalShellLayout layout = TacticalShellLayout.compute(width, height);
        UiRect s = layout.glass();
        UiRect p = layout.display();
        int[] runs = new int[width * height];
        int[] rule = new int[width * height];
        java.util.Arrays.fill(runs, 0xFF000000);
        java.util.Arrays.fill(rule, 0xFF000000);
        DeviceArt.glassOverlay(layout).composite(runs, width, height);

        int a = (int) Math.round(p.width() * 0.15D);
        int b = a + Math.max(12, (int) Math.round(p.width() * 0.08D));
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int at = y * width + x;
                if (y == s.top() && x >= s.left() && x < s.right()) {
                    rule[at] = DeviceArt.over(rule[at], DeviceArt.GLASS_HI);
                }
                if (p.contains(x, y)) {
                    if (y == p.top()) {
                        rule[at] = DeviceArt.over(rule[at], 0x30000000);
                    } else if (x == p.left()) {
                        rule[at] = DeviceArt.over(rule[at], 0x24000000);
                    }
                    double d = (x - p.left()) + (y - p.top()) * 1.6D;
                    int sheen = d >= a && d < b ? 0x0AFFFFFF : d >= b + 4 && d < b + 7
                            ? 0x08FFFFFF : 0;
                    rule[at] = DeviceArt.over(rule[at], sheen);
                }
            }
        }
        for (int at = 0; at < rule.length; at++) {
            assertEquals(rule[at], runs[at], "pixel " + (at % width) + "," + (at / width));
        }
    }

    /** Specks cover the inner case face, never the glass recess, and nothing on the compact class. */
    @ParameterizedTest(name = "{0}x{1}")
    @CsvSource({"320, 240", "480, 360", "960, 540"})
    void speckBandsCoverThePreviewSpeckArea(int width, int height) {
        TacticalShellLayout layout = TacticalShellLayout.compute(width, height);
        List<UiRect> bands = DeviceArt.speckBands(layout);
        if (layout.density() == Density.COMPACT) {
            assertTrue(bands.isEmpty(), "no specks on the compact class");
            return;
        }
        UiRect d = layout.device();
        UiRect s = layout.glass();
        UiRect inner = new UiRect(d.left() + 2, d.top() + 2, d.right() - 2, d.bottom() - 2);
        int radius = layout.deviceMetrics().radius() - 2;
        // Opaque pixels of the parts drawn over the specks (bumpers etc.).
        DeviceArt.Plate plate = DeviceArt.plate(width, height, layout.density(), Livery.ACADEMY);
        int[] over = new int[width * height];
        plate.over().composite(over, width, height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean inBand = false;
                for (UiRect band : bands) {
                    inBand |= band.contains(x, y);
                }
                boolean hole = x >= s.left() - 1 && x <= s.right() && y >= s.top() - 1
                        && y <= s.bottom();
                boolean speckArea = inner.contains(x, y) && !hole;
                assertEquals(speckArea, inBand, "pixel " + x + "," + y);
                if (inBand && !DeviceArt.inRoundedRect(inner, radius, x, y)) {
                    // The preview skips the rounded corners; there the bumpers cover the tile.
                    assertEquals(0xFF, over[y * width + x] >>> 24, "corner " + x + "," + y);
                }
            }
        }
        for (int first = 0; first < bands.size(); first++) {
            for (int second = first + 1; second < bands.size(); second++) {
                assertFalse(bands.get(first).intersects(bands.get(second)), "bands overlap");
            }
        }
    }

    @ParameterizedTest(name = "{0}x{1}")
    @CsvSource({"320, 240", "480, 360", "960, 540"})
    void ledsAndSilkscreenSitOnTheTopBezel(int width, int height) {
        TacticalShellLayout layout = TacticalShellLayout.compute(width, height);
        UiRect power = DeviceArt.powerLed(layout);
        UiRect link = DeviceArt.linkLed(layout);
        UiRect bezel = new UiRect(layout.device().left(), layout.device().top(),
                layout.device().right(), layout.glass().top() - 1);

        boolean tight = layout.tight();
        assertEquals(tight ? 2 : 3, power.width());
        assertEquals(tight ? 1 : 2, power.height());
        assertEquals(layout.glass().right() - (tight ? 9 : 24), power.left());
        assertEquals(power.right() + 3, link.left(), "link LED 3px right of the power LED");
        assertEquals(power.top(), link.top());
        assertTrue(bezel.contains(power.inset(-1)), "power LED with its halo");
        assertTrue(bezel.contains(link.inset(-1)), "link LED with its halo");
        assertFalse(power.inset(-1).intersects(link.inset(-1)), "halos stay apart");

        int[] silk = DeviceArt.silkOrigin(layout);
        if (tight) {
            assertNull(silk, "a 6px top bezel has no silkscreen");
        } else {
            int top = layout.deviceMetrics().top();
            assertEquals(layout.glass().left() + 28, silk[0]);
            assertEquals(layout.device().top() + (int) Math.ceil((top - 7) / 2.0D), silk[1]);
            assertTrue(silk[1] + 7 <= layout.glass().top() - 1, "glyphs end above the recess");
        }
    }

    @Test
    void linkLedRunsLightTheLedWithItsHalo() {
        TacticalShellLayout layout = TacticalShellLayout.compute(480, 360);
        DeviceArt.Runs runs = DeviceArt.linkLedRuns(layout, 0xFF8AC4F5);
        UiRect link = DeviceArt.linkLed(layout);

        assertEquals(2, runs.size());
        assertEquals(0x488AC4F5, runs.color(0), "halo");
        assertEquals(link.inset(-1), new UiRect(runs.left(0), runs.top(0), runs.right(0),
                runs.bottom(0)));
        assertEquals(0xFF8AC4F5, runs.color(1));
        assertEquals(link, new UiRect(runs.left(1), runs.top(1), runs.right(1), runs.bottom(1)));
    }

    @Test
    void speckTileIsThePreviewHash() throws Exception {
        BufferedImage tile = TacticalIconAtlasTest.image(TEXTURES + "device_specks.png");
        assertEquals(DeviceArt.SPECK_TILE, tile.getWidth());
        assertEquals(DeviceArt.SPECK_TILE, tile.getHeight());
        int texels = 0;
        for (int y = 0; y < DeviceArt.SPECK_TILE; y++) {
            for (int x = 0; x < DeviceArt.SPECK_TILE; x++) {
                int expected = DeviceArt.speck(x, y);
                int actual = tile.getRGB(x, y);
                if (expected == 0) {
                    assertEquals(0, actual >>> 24, "clear texel " + x + "," + y);
                } else {
                    assertEquals(expected, actual, "speck texel " + x + "," + y);
                    texels++;
                }
            }
        }
        JsonObject specks = TacticalIconAtlasTest.manifest().getAsJsonObject("device")
                .getAsJsonObject("specks");
        assertEquals(texels, specks.get("texels").getAsInt());
        assertTrue(texels > 0, "the tile carries specks");
    }

    @Test
    void vignetteTextureIsThePreviewBackdropShade() throws Exception {
        BufferedImage vignette = TacticalIconAtlasTest.image(TEXTURES + "device_vignette.png");
        int size = DeviceArt.VIGNETTE_SIZE;
        assertEquals(size, vignette.getWidth());
        assertEquals(size, vignette.getHeight());
        int max = 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int alpha = DeviceArt.vignetteAlpha((x + 0.5D) / size, (y + 0.5D) / size);
                int actual = vignette.getRGB(x, y);
                assertEquals(alpha, actual >>> 24, "alpha at " + x + "," + y);
                if (alpha > 0) {
                    assertEquals(0, actual & 0xFFFFFF, "black at " + x + "," + y);
                }
                max = Math.max(max, alpha);
            }
        }
        assertEquals(0, DeviceArt.vignetteAlpha(0.5D, 0.5D), "clear in the middle");
        assertEquals(TacticalIconAtlasTest.manifest().getAsJsonObject("device")
                .getAsJsonObject("vignette").get("maxAlpha").getAsInt(), max);
    }

    @Test
    void deviceTexturesAreTheGeneratorOutput() throws Exception {
        JsonObject sha = TacticalIconAtlasTest.manifest().getAsJsonObject("sha256");
        for (String png : new String[]{"device_specks.png", "device_vignette.png"}) {
            assertEquals(sha.get(png).getAsString(), TacticalIconAtlasTest.sha256(TEXTURES + png),
                    png + " must be regenerated with export-ui-atlas.mjs, not edited by hand");
        }
        JsonObject specks = TacticalIconAtlasTest.json(TEXTURES + "device_specks.png.mcmeta")
                .getAsJsonObject("texture");
        assertFalse(specks.get("clamp").getAsBoolean(), "the speck tile repeats");
        assertFalse(specks.get("blur").getAsBoolean(), "specks stay pixel-sharp");
        JsonObject vignette = TacticalIconAtlasTest.json(TEXTURES + "device_vignette.png.mcmeta")
                .getAsJsonObject("texture");
        assertTrue(vignette.get("blur").getAsBoolean(), "the vignette is stretched smoothly");
        assertTrue(vignette.get("clamp").getAsBoolean());
    }
}
