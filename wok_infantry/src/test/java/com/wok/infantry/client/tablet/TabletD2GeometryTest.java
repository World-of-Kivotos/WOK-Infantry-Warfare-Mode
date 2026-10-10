package com.wok.infantry.client.tablet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wok.infantry.client.screen.UiRect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The D2 geometry the animation uses is {@code TacticalShellLayout.compute} and matches the
 * preview's {@code d2geom} on all five tiers ({@code vectors.tiers[].geom}).
 */
class TabletD2GeometryTest {
    private static void rect(JsonElement expected, UiRect actual, String what) {
        JsonObject o = expected.getAsJsonObject();
        assertEquals(o.get("l").getAsInt(), actual.left(), what + ".l");
        assertEquals(o.get("t").getAsInt(), actual.top(), what + ".t");
        assertEquals(o.get("r").getAsInt(), actual.right(), what + ".r");
        assertEquals(o.get("b").getAsInt(), actual.bottom(), what + ".b");
        assertEquals(o.get("w").getAsInt(), actual.width(), what + ".w");
        assertEquals(o.get("h").getAsInt(), actual.height(), what + ".h");
    }

    @Test
    void fiveTiersMatchThePreview() {
        for (JsonElement element : TabletVectors.array("tiers")) {
            JsonObject tier = element.getAsJsonObject();
            String id = tier.get("tier").getAsString();
            JsonObject geom = tier.getAsJsonObject("geom");
            TabletD2Geometry g = TabletD2Geometry.of(geom.get("w").getAsInt(), geom.get("h").getAsInt());
            assertEquals(geom.get("density").getAsString(), g.density().name(), id);
            rect(geom.get("D"), g.d(), id + " D");
            rect(geom.get("S"), g.s(), id + " S");
            rect(geom.get("P"), g.p(), id + " P");
            rect(geom.get("E"), g.e(), id + " E");
            assertEquals(geom.getAsJsonObject("k").get("bump").getAsInt(), g.k().bump(), id);
        }
    }

    @Test
    void designTableSpotChecks() {
        // DESIGN 6.6 round two: 480×270 COMPACT D (2,2,478,268) S (7,8,473,250) P (8,9,472,249);
        // 640×360 STANDARD D (12,6,628,355); 960×540 ROOMY D (28,12,932,530)
        TabletD2Geometry a = TabletD2Geometry.of(480, 270);
        assertEquals(UiRect.of(2, 2, 478, 268), a.d());
        assertEquals(UiRect.of(7, 8, 473, 250), a.s());
        assertEquals(UiRect.of(8, 9, 472, 249), a.p());
        assertEquals(UiRect.of(12, 6, 628, 355), TabletD2Geometry.of(640, 360).d());
        assertEquals(UiRect.of(28, 12, 932, 530), TabletD2Geometry.of(960, 540).d());
    }
}
