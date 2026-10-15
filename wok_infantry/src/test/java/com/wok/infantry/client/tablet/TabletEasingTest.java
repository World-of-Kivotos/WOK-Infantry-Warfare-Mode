package com.wok.infantry.client.tablet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static com.wok.infantry.client.tablet.TabletVectors.near;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** {@link TabletEasing} against the preview's {@code TA.ease} samples ({@code vectors.easing}). */
class TabletEasingTest {
    @Test
    void everyCurveMatchesThePreviewSamples() {
        Set<String> seen = new HashSet<>();
        for (JsonElement element : TabletVectors.array("easing")) {
            JsonObject curve = element.getAsJsonObject();
            String name = curve.get("name").getAsString();
            TabletEasing easing = TabletEasing.byPreviewName(name);
            assertNotNull(easing, () -> "no Java curve for " + name);
            seen.add(name);
            for (double[] sample : TabletVectors.pairs(curve.get("samples"))) {
                near(sample[1], easing.apply(sample[0]), () -> name + "(" + sample[0] + ")");
            }
        }
        assertEquals(TabletEasing.values().length, seen.size(), "every Java curve has preview samples");
    }

    @Test
    void curvesAreClampedAndPinnedAtBothEnds() {
        for (TabletEasing easing : TabletEasing.values()) {
            assertEquals(0.0D, easing.apply(-0.5D), 1e-15, easing.name());
            assertEquals(0.0D, easing.apply(0.0D), 1e-15, easing.name());
            assertEquals(1.0D, easing.apply(1.0D), 1e-15, easing.name());
            assertEquals(1.0D, easing.apply(2.0D), 1e-15, easing.name());
        }
        // minimum jerk: 10x³ − 15x⁴ + 6x⁵, symmetric around 0.5
        assertEquals(0.5D, TabletEasing.MIN_JERK.apply(0.5D), 1e-15);
        assertEquals(1.0D - TabletEasing.MIN_JERK.apply(0.3D), TabletEasing.MIN_JERK.apply(0.7D), 1e-15);
    }
}
