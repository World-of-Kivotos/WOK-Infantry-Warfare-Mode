package com.wok.infantry.client.tablet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The preview's comparison vectors ({@code src/test/resources/tablet_anim/vectors.json}, written
 * by {@code ui-preview/tablet-anim/tools/export-java-vectors.mjs}; {@code --check} verifies it is
 * still what the preview computes). Numbers are stored with 13 significant digits.
 *
 * <p>Tolerances ({@code meta.tolerance}): {@link #DOUBLE} 1e-9 for every double, relative above
 * magnitude 1 (times in ms, pixels); {@link #PX} 1e-6 for pixel rectangles. Matrices are compared
 * in double, so {@link #DOUBLE} too (the 2e-5 float tolerance is for the JOML float copies of the
 * render batch).
 */
final class TabletVectors {
    static final double DOUBLE = 1e-9;
    static final double PX = 1e-6;
    /** The preview's frame step (1000 / 60 ms). */
    static final double DT = 1000.0D / 60.0D;

    private static JsonObject root;

    private TabletVectors() {
    }

    static synchronized JsonObject root() {
        if (root == null) {
            try (InputStream stream = TabletVectors.class.getClassLoader()
                    .getResourceAsStream("tablet_anim/vectors.json")) {
                assertNotNull(stream, "tablet_anim/vectors.json is missing from the test resources");
                root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                        .getAsJsonObject();
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }
        return root;
    }

    static JsonElement section(String name) {
        JsonElement element = root().get(name);
        assertNotNull(element, () -> "vectors.json has no section " + name);
        return element;
    }

    static JsonArray array(String name) {
        return section(name).getAsJsonArray();
    }

    static JsonObject object(String name) {
        return section(name).getAsJsonObject();
    }

    /** The tier object of {@code tiers[]} with this id. */
    static JsonObject tier(String id) {
        for (JsonElement element : array("tiers")) {
            if (element.getAsJsonObject().get("tier").getAsString().equals(id)) {
                return element.getAsJsonObject();
            }
        }
        fail("no tier " + id);
        return null;
    }

    static TabletUnits units(JsonObject u) {
        return new TabletUnits(u.get("S").getAsInt(), u.get("f").getAsInt(), u.get("lpW").getAsInt(),
                u.get("lpH").getAsInt(), u.get("guiW").getAsInt(), u.get("guiH").getAsInt(),
                u.get("pxW").getAsInt(), u.get("pxH").getAsInt());
    }

    static TabletUnits termUnits(String tier) {
        return units(tier(tier).getAsJsonObject("unitsTerm"));
    }

    static TabletUnits mapUnits(String tier) {
        return units(tier(tier).getAsJsonObject("unitsMap"));
    }

    static boolean isNull(JsonElement element) {
        return element == null || element.isJsonNull();
    }

    static double num(JsonElement element) {
        return isNull(element) ? Double.NaN : element.getAsDouble();
    }

    static double num(JsonObject object, String key) {
        return num(object.get(key));
    }

    static String str(JsonElement element) {
        return isNull(element) ? null : element.getAsString();
    }

    static double[] doubles(JsonElement element) {
        JsonArray array = element.getAsJsonArray();
        double[] out = new double[array.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = array.get(i).getAsDouble();
        }
        return out;
    }

    static double[][] pairs(JsonElement element) {
        JsonArray array = element.getAsJsonArray();
        double[][] out = new double[array.size()][];
        for (int i = 0; i < out.length; i++) {
            out[i] = doubles(array.get(i));
        }
        return out;
    }

    /** {@code |e − a| ≤ tol · max(1, |e|)}; NaN expected means "no value" and must match NaN. */
    static void near(double expected, double actual, double tol, Supplier<String> what) {
        if (Double.isNaN(expected)) {
            assertTrue(Double.isNaN(actual), () -> what.get() + ": expected no value, got " + actual);
            return;
        }
        if (Double.isInfinite(expected)) {
            assertEquals(expected, actual, () -> what.get());
            return;
        }
        double scale = Math.max(1.0D, Math.abs(expected));
        assertTrue(Math.abs(expected - actual) <= tol * scale,
                () -> what.get() + ": expected " + expected + " but was " + actual
                        + " (diff " + Math.abs(expected - actual) + ")");
    }

    static void near(double expected, double actual, Supplier<String> what) {
        near(expected, actual, DOUBLE, what);
    }

    static void near(JsonElement expected, double actual, Supplier<String> what) {
        near(num(expected), actual, DOUBLE, what);
    }

    static void matrix(JsonElement expected, double[] actual, Supplier<String> what) {
        double[] e = doubles(expected);
        assertEquals(16, e.length, what);
        for (int i = 0; i < 16; i++) {
            int index = i;
            near(e[i], actual[i], DOUBLE, () -> what.get() + "[" + index + "]");
        }
    }

    /** {@code {x, y, w, h[, scale]}} against a pixel rectangle (or both null). */
    static void rectPx(JsonElement expected, TabletPose3D.RectPx actual, Supplier<String> what) {
        if (isNull(expected)) {
            assertTrue(actual == null, () -> what.get() + ": expected no rectangle, got " + actual);
            return;
        }
        assertNotNull(actual, () -> what.get() + ": rectangle missing");
        JsonObject o = expected.getAsJsonObject();
        near(num(o, "x"), actual.x(), PX, () -> what.get() + ".x");
        near(num(o, "y"), actual.y(), PX, () -> what.get() + ".y");
        near(num(o, "w"), actual.w(), PX, () -> what.get() + ".w");
        near(num(o, "h"), actual.h(), PX, () -> what.get() + ".h");
        if (o.has("scale")) {
            near(num(o, "scale"), actual.scale(), DOUBLE, () -> what.get() + ".scale");
        }
    }

    /** {@code {x, y, w, h}} against an integer box. */
    static void box(JsonElement expected, TabletPath2D.Box actual, Supplier<String> what) {
        JsonObject o = expected.getAsJsonObject();
        assertEquals(o.get("x").getAsInt(), actual.x(), () -> what.get() + ".x");
        assertEquals(o.get("y").getAsInt(), actual.y(), () -> what.get() + ".y");
        assertEquals(o.get("w").getAsInt(), actual.w(), () -> what.get() + ".w");
        assertEquals(o.get("h").getAsInt(), actual.h(), () -> what.get() + ".h");
    }

    static TabletScreenKind screen(String previewName) {
        return TabletScreenKind.byPreviewName(previewName);
    }

    static TabletPath path(String name) {
        return TabletPath.valueOf(name);
    }
}
