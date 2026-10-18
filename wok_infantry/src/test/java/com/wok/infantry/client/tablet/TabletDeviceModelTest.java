package com.wok.infantry.client.tablet;

import com.google.gson.JsonObject;
import com.wok.infantry.client.screen.DeviceArt;
import com.wok.infantry.client.screen.TacticalLivery;
import com.wok.infantry.client.screen.UiRect;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.wok.infantry.client.tablet.TabletVectors.near;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The 3D device's geometry ({@link TabletDeviceModel}, IMPL_PLAN 4.3 {@code TabletOutlineTest}):
 * the front face over E against the preview ({@code vectors.tiers[].face3D}); the walls traced
 * from the front-face image form closed outlines with outward normals and the right material;
 * the back plate covers exactly the solid pixels.
 */
class TabletDeviceModelTest {
    private static final List<String> TIERS = List.of("320x240", "480x270", "640x360", "960x540", "960x720");

    @Test
    void faceQuadMatchesThePreview() {
        for (String id : TIERS) {
            TabletUnits term = TabletVectors.termUnits(id);
            TabletPose3D.Context c = TabletPose3D.Context.of(term, term, TabletScreenKind.SQUAD,
                    TabletPose3D.P11_HAND, false);
            TabletDeviceModel.FaceQuad q = TabletDeviceModel.faceQuad(c.face(), c.geom());
            JsonObject e = TabletVectors.tier(id).getAsJsonObject("face3D");
            near(e.get("x0"), q.x0(), () -> id + " x0");
            near(e.get("x1"), q.x1(), () -> id + " x1");
            near(e.get("yTop"), q.yTop(), () -> id + " yTop");
            near(e.get("yBottom"), q.yBottom(), () -> id + " yBottom");
            near(e.get("u0"), q.u0(), () -> id + " u0");
            near(e.get("u1"), q.u1(), () -> id + " u1");
            near(e.get("v0"), q.v0(), () -> id + " v0");
            near(e.get("v1"), q.v1(), () -> id + " v1");
            double[] center = TabletVectors.doubles(e.get("sleepCenter"));
            near(center[0], q.sleepCenterX(), () -> id + " sleep x");
            near(center[1], q.sleepCenterY(), () -> id + " sleep y");
            near(e.get("sleepWidth"), q.sleepWidth(), () -> id + " sleep width");
            near(e.get("thick"), q.thick(), () -> id + " thick");
        }
    }

    private static int[] faceImage(TabletD2Geometry g) {
        return DeviceArt.rasterize(g.w(), g.h(), g.density(), TacticalLivery.Livery.ACADEMY,
                new DeviceArt.RasterOptions(false, true, List.of(), 0xFF7CCB8F));
    }

    private static boolean solid(int[] argb, int w, int h, int x, int y) {
        return x >= 0 && y >= 0 && x < w && y < h
                && (argb[y * w + x] >>> 24) >= TabletDeviceModel.SOLID_ALPHA;
    }

    @Test
    void wallsCloseAroundTheCaseWithOutwardNormalsAndTheBackPlateCoversIt() {
        for (String id : TIERS) {
            TabletUnits term = TabletVectors.termUnits(id);
            TabletD2Geometry g = TabletD2Geometry.of(term.lpW(), term.lpH());
            int w = g.w();
            int h = g.h();
            int[] argb = faceImage(g);
            TabletDeviceModel.Mesh mesh = TabletDeviceModel.mesh(argb, w, h, g.d());

            // Back plate: exactly the solid pixels, no overlap.
            int area = 0;
            boolean[] covered = new boolean[w * h];
            for (TabletDeviceModel.BackRect r : mesh.back()) {
                area += r.area();
                for (int y = r.top(); y < r.bottom(); y++) {
                    for (int x = r.left(); x < r.right(); x++) {
                        assertTrue(solid(argb, w, h, x, y), id + " back plate over an empty pixel");
                        assertFalse(covered[y * w + x], id + " back plate overlaps itself");
                        covered[y * w + x] = true;
                    }
                }
            }
            assertEquals(mesh.solid(), area, id + " back plate area");

            // Walls: every exposed pixel side once; closed (in = out at every grid point).
            int exposed = 0;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (!solid(argb, w, h, x, y)) {
                        continue;
                    }
                    exposed += solid(argb, w, h, x, y - 1) ? 0 : 1;
                    exposed += solid(argb, w, h, x, y + 1) ? 0 : 1;
                    exposed += solid(argb, w, h, x - 1, y) ? 0 : 1;
                    exposed += solid(argb, w, h, x + 1, y) ? 0 : 1;
                }
            }
            Map<Long, Integer> balance = new HashMap<>();
            int length = 0;
            for (TabletDeviceModel.Edge e : mesh.edges()) {
                length += e.length();
                assertTrue(e.dx() == 0 || e.dy() == 0, id + " walls are axis-aligned");
                balance.merge(key(e.x0(), e.y0()), 1, Integer::sum);
                balance.merge(key(e.x1(), e.y1()), -1, Integer::sum);
                // Outward: the pixel left of the direction (y down) is empty, right of it solid.
                int sx = e.dx() != 0 ? Math.min(e.x0(), e.x1()) : e.x0() - (e.dy() > 0 ? 1 : 0);
                int sy = e.dy() != 0 ? Math.min(e.y0(), e.y1()) : e.y0() - (e.dx() < 0 ? 1 : 0);
                // (sx, sy): the solid pixel along the wall's first step.
                assertTrue(solid(argb, w, h, sx, sy), id + " solid side of " + e);
                int ox = sx + e.dy() * 1;
                int oy = sy - e.dx() * 1;
                assertFalse(solid(argb, w, h, ox, oy), id + " outward side of " + e + " is empty");
                // Material: case inside D, rubber outside.
                boolean inD = sx >= g.d().left() && sx < g.d().right() && sy >= g.d().top()
                        && sy < g.d().bottom();
                assertEquals(inD ? TabletDeviceModel.Wall.CASE : TabletDeviceModel.Wall.RUBBER,
                        e.wall(), id + " material of " + e);
            }
            assertEquals(exposed, length, id + " wall length = exposed pixel sides");
            for (Map.Entry<Long, Integer> entry : balance.entrySet()) {
                assertEquals(0, entry.getValue(), id + " outline closed at " + entry.getKey());
            }
            assertTrue(mesh.edges().size() < length, id + " walls are merged");
            assertTrue(mesh.edges().stream().anyMatch(e -> e.wall() == TabletDeviceModel.Wall.RUBBER),
                    id + " bumpers are rubber");
        }
    }

    private static long key(int x, int y) {
        return ((long) x << 32) ^ (y & 0xFFFFFFFFL);
    }

    @Test
    void wallNormalsFollowThePreviewRule() {
        // A 3×2 block: top runs left → right (normal +y), right runs down (+x), bottom runs
        // right → left (−y), left runs up (−x); preview n = [dy, dx, 0].
        int w = 5;
        int h = 4;
        int[] argb = new int[w * h];
        for (int y = 1; y < 3; y++) {
            for (int x = 1; x < 4; x++) {
                argb[y * w + x] = 0xFF000000;
            }
        }
        TabletDeviceModel.Mesh mesh = TabletDeviceModel.mesh(argb, w, h, new UiRect(0, 0, 5, 4));
        assertEquals(4, mesh.edges().size());
        assertEquals(1, mesh.back().size());
        assertEquals(6, mesh.solid());
        for (TabletDeviceModel.Edge e : mesh.edges()) {
            if (e.y0() == 1 && e.y1() == 1) {
                assertEquals(new TabletDeviceModel.Edge(1, 1, 4, 1, TabletDeviceModel.Wall.CASE), e);
                assertEquals(1, e.dx());
            } else if (e.x0() == 4 && e.x1() == 4) {
                assertEquals(new TabletDeviceModel.Edge(4, 1, 4, 3, TabletDeviceModel.Wall.CASE), e);
            } else if (e.y0() == 3 && e.y1() == 3) {
                assertEquals(new TabletDeviceModel.Edge(4, 3, 1, 3, TabletDeviceModel.Wall.CASE), e);
            } else {
                assertEquals(new TabletDeviceModel.Edge(1, 3, 1, 1, TabletDeviceModel.Wall.CASE), e);
            }
        }
    }
}
