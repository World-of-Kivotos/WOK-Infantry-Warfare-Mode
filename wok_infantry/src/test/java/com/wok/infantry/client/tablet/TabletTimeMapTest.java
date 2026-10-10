package com.wok.infantry.client.tablet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static com.wok.infantry.client.tablet.TabletVectors.near;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every time → p map of the motion (A per hand, opening / 2-second reopen / C1 close / old close;
 * B and quick on terminal and map; OFF) against {@code vectors.timeMaps}: node list, duration,
 * evaluation every frame and the inverse.
 */
class TabletTimeMapTest {
    private static TabletTimeMap javaMap(String id) {
        String[] part = id.split("\\.");
        TabletPath path = TabletVectors.path(part[0]);
        boolean open = part[1].equals("open");
        switch (path) {
            case A3D -> {
                TabletHand hand = TabletHand.byPreviewName(part[2]);
                if (open) {
                    return TabletMotion.openMap(path, hand, part.length > 3 && part[3].equals("fast"));
                }
                return TabletMotion.closeMap(path, hand, part[3].equals("capture"), TabletScreenKind.SQUAD);
            }
            case B2D, QUICK -> {
                TabletScreenKind screen = TabletVectors.screen(part[2]);
                if (open) {
                    return TabletMotion.openMap(path, TabletHand.GUN, part.length > 3 && part[3].equals("fast"));
                }
                return TabletMotion.closeMap(path, TabletHand.GUN, true, screen);
            }
            case OFF -> {
                return TabletMotion.openMap(path, TabletHand.GUN, false);
            }
        }
        throw new IllegalArgumentException(id);
    }

    @Test
    void mapsMatchThePreview() {
        int compared = 0;
        for (JsonElement element : TabletVectors.array("timeMaps")) {
            JsonObject rec = element.getAsJsonObject();
            String id = rec.get("id").getAsString();
            if (id.startsWith("NOW.")) {
                continue; // the preview's "wait for the server" comparison path is not ported
            }
            TabletTimeMap map = javaMap(id);
            double[][] pts = TabletVectors.pairs(rec.get("pts"));
            assertEquals(pts.length, map.size(), () -> id + " nodes");
            for (int i = 0; i < pts.length; i++) {
                int index = i;
                near(pts[i][0], map.nodeMs(i), () -> id + " node " + index + " ms");
                near(pts[i][1], map.nodeP(i), () -> id + " node " + index + " p");
            }
            assertEquals(rec.get("ease").getAsString(), map.ease().previewName(), id + " ease");
            near(rec.get("dur"), map.durMs(), () -> id + " dur");
            for (double[] sample : TabletVectors.pairs(rec.get("samples"))) {
                near(sample[1], map.eval(sample[0]), () -> id + " eval(" + sample[0] + ")");
            }
            for (double[] inverse : TabletVectors.pairs(rec.get("inverse"))) {
                near(inverse[1], map.invert(inverse[0]), () -> id + " invert(" + inverse[0] + ")");
            }
            compared++;
        }
        assertEquals(23, compared, "A 12 + B / quick 10 + OFF 1 maps compared");
    }

    @Test
    void closeStartCapsMatchThePreview() {
        for (JsonElement element : TabletVectors.array("closeStartCap")) {
            JsonObject rec = element.getAsJsonObject();
            TabletPath path = TabletVectors.path(rec.get("path").getAsString());
            boolean capture = rec.get("capture").getAsBoolean();
            near(rec.get("cap"), TabletMotion.closeStartCap(path, capture), () -> path + " " + capture);
        }
    }

    @Test
    void tempoScalesTheV1MapsOnlyAndKeepsTheirNodes() {
        // DESIGN 6.6 round four: A = v1 ÷ 0.3, p nodes unchanged; B, quick and the reopen factor unchanged
        for (boolean empty : new boolean[]{false, true}) {
            double[][] v1 = TabletAnimationModel.v1Open(empty);
            double[][] a = TabletAnimationModel.openPts(empty);
            for (int i = 0; i < v1.length; i++) {
                assertEquals(v1[i][0] / 0.3D, a[i][0], 1e-9);
                assertEquals(v1[i][1], a[i][1], 0.0D);
            }
        }
        assertEquals(1333.3333333333, TabletMotion.openMap(TabletPath.A3D, TabletHand.GUN, false).durMs(), 1e-6);
        assertEquals(1200.0D, TabletMotion.openMap(TabletPath.A3D, TabletHand.EMPTY, false).durMs(), 1e-9);
        assertEquals(800.0D, TabletMotion.openMap(TabletPath.A3D, TabletHand.GUN, true).durMs(), 1e-9);
        assertEquals(720.0D, TabletMotion.openMap(TabletPath.A3D, TabletHand.EMPTY, true).durMs(), 1e-9);
        assertEquals(900.0D, TabletMotion.closeMap(TabletPath.A3D, TabletHand.GUN, true, null).durMs(), 1e-9);
        assertEquals(766.6666666667, TabletMotion.closeMap(TabletPath.A3D, TabletHand.EMPTY, true, null).durMs(), 1e-6);
        assertEquals(733.3333333333, TabletMotion.closeMap(TabletPath.A3D, TabletHand.GUN, false, null).durMs(), 1e-6);
        assertEquals(600.0D, TabletMotion.closeMap(TabletPath.A3D, TabletHand.EMPTY, false, null).durMs(), 1e-9);
        assertEquals(200.0D, TabletMotion.openMap(TabletPath.B2D, TabletHand.GUN, false).durMs(), 0.0D);
        assertEquals(80.0D, TabletMotion.openMap(TabletPath.QUICK, TabletHand.GUN, true).durMs(), 0.0D);
        assertTrue(TabletAnimationModel.KEY_FINISH_MS <= 150.0D, "key finish stays a fast-forward");
        // the C1 close's first segment is 65 v1 ms (review F1), total length unchanged
        double[][] c1 = TabletAnimationModel.v1CloseCapture(false);
        assertEquals(65.0D, c1[1][0], 0.0D);
        assertEquals(270.0D, c1[c1.length - 1][0], 0.0D);
    }

    @Test
    void emptyHandOpenKneeSitsBeforeTheTabletEntersAndBarelyChangesSpeed() {
        double[][] v = TabletAnimationModel.v1Open(true);
        assertTrue(v[1][1] <= TabletAnimationModel.KEYS.get(0).p() + 1e-12, "knee at or before K0");
        double r1 = v[1][1] / v[1][0];
        double r2 = (v[2][1] - v[1][1]) / (v[2][0] - v[1][0]);
        assertTrue(Math.max(r1, r2) / Math.min(r1, r2) < 1.05, "the two speeds differ by < 5 %");
    }

    @Test
    void invertFindsTheFirstTimeOnStepsAndEasedSegments() {
        TabletTimeMap eased = new TabletTimeMap(new double[][]{{0, 0.4}, {100, 1}},
                TabletEasing.EASE_OUT_QUAD, 100);
        for (double t : new double[]{0, 10, 33, 50, 99}) {
            assertEquals(t, eased.invert(eased.eval(t)), 1e-9);
        }
        TabletTimeMap down = TabletMotion.closeMap(TabletPath.A3D, TabletHand.GUN, true, null);
        assertEquals(0.0D, down.invert(1.0D), 0.0D);
        assertEquals(down.durMs(), down.invert(0.0D), 1e-9);
        assertEquals(0.0D, down.invert(2.0D), 0.0D, "clamped to the map's range");
    }
}
