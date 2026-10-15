package com.wok.infantry.client.screen;

import com.wok.infantry.client.screen.TacticalLivery.Livery;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Pixel-exact comparison of {@link DeviceArt#rasterize} with the D2 device the preview draws
 * ({@code ui-preview/tools/export-device-golden.mjs}: {@code 17-device.js} at level 2 in the P3
 * {@code SKINS}, without the specks, the link LED, the silkscreen and the lit display). Re-run
 * the exporter with {@code --module} after a preview change; never edit the goldens by hand.
 */
class DeviceArtGoldenTest {
    private static final String GOLDEN = "device_golden/";
    /** Mismatches listed in a failure message. */
    private static final int REPORTED = 12;

    static Stream<Arguments> goldens() {
        List<Arguments> cases = new ArrayList<>();
        for (Livery livery : Livery.values()) {
            for (int[] size : new int[][]{{320, 240}, {480, 360}, {960, 540}}) {
                cases.add(Arguments.of(livery, size[0], size[1]));
            }
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "{0} {1}x{2}")
    @MethodSource("goldens")
    void rasterizedDeviceMatchesThePreview(Livery livery, int width, int height)
            throws IOException {
        String name = livery.name().toLowerCase(Locale.ROOT) + "_" + width + "x" + height + ".png";
        BufferedImage golden = image(GOLDEN + name);
        assertEquals(width, golden.getWidth(), name);
        assertEquals(height, golden.getHeight(), name);

        int[] argb = DeviceArt.rasterize(width, height, TacticalShellLayout.density(width, height),
                livery);
        assertEquals(width * height, argb.length);
        List<String> mismatches = new ArrayList<>();
        int count = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int expected = golden.getRGB(x, y);
                int actual = argb[y * width + x];
                // Fully transparent pixels may carry any colour.
                boolean same = (expected >>> 24) == 0 ? (actual >>> 24) == 0 : expected == actual;
                if (!same) {
                    count++;
                    if (mismatches.size() < REPORTED) {
                        mismatches.add(String.format(Locale.ROOT, "(%d,%d) want %08X got %08X", x,
                                y, expected, actual));
                    }
                }
            }
        }
        if (count > 0) {
            fail(name + ": " + count + " pixels differ, e.g. " + String.join(", ", mismatches));
        }
    }

    private static BufferedImage image(String path) throws IOException {
        try (InputStream stream = DeviceArtGoldenTest.class.getClassLoader()
                .getResourceAsStream(path)) {
            assertNotNull(stream, path + " must be on the test classpath");
            BufferedImage image = ImageIO.read(stream);
            assertNotNull(image, path + " must be a readable PNG");
            return image;
        }
    }
}
