package com.wok.infantry.client.tablet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static com.wok.infantry.client.tablet.TabletVectors.near;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Scheme B and the quick setting ({@link TabletPath2D}) against {@code vectors.path2d} (40 groups),
 * {@code mapDeviceTop640}, {@code termVisible} and the per-tier lift numbers, plus the properties
 * of DESIGN 6.6.
 */
class TabletPath2DTest {
    private static final List<String> TIERS = List.of("320x240", "480x270", "640x360", "960x540", "960x720");

    /** The exported p has 13 digits; the preview's inputs were k/12 or short decimals. */
    private static double exactInput(double p) {
        double twelfth = Math.round(p * 12.0D) / 12.0D;
        return Math.abs(twelfth - p) < 1e-11 ? twelfth : p;
    }

    private static void hud(JsonObject expected, boolean hideCrosshair, boolean hideHud, String what) {
        assertEquals(expected.get("hideCrosshair").getAsBoolean(), hideCrosshair, what + " hud");
        assertEquals(expected.get("hideHud").getAsBoolean(), hideHud, what + " hud");
    }

    private static int scan(JsonElement element) {
        return TabletVectors.isNull(element) ? Integer.MIN_VALUE : element.getAsInt();
    }

    @Test
    void framesMatchThePreview() {
        int frames = 0;
        for (JsonElement element : TabletVectors.array("path2d")) {
            JsonObject group = element.getAsJsonObject();
            String tier = group.get("tier").getAsString();
            boolean open = group.get("dir").getAsString().equals("open");
            boolean quick = group.get("quick").getAsBoolean();
            boolean terminal = group.get("kind").getAsString().equals("terminal");
            TabletUnits term = TabletVectors.termUnits(tier);
            TabletUnits map = TabletVectors.mapUnits(tier);
            for (JsonElement fe : group.getAsJsonArray("frames")) {
                JsonObject e = fe.getAsJsonObject();
                double p = exactInput(e.get("p").getAsDouble());
                String what = group.get("kind").getAsString() + " " + tier + " " + (open ? "open" : "close")
                        + (quick ? " quick" : "") + " p=" + p;
                if (terminal) {
                    TabletPath2D.TermFrame f = TabletPath2D.termFrame(p, term, open, quick);
                    near(e.get("p"), f.p(), () -> what + " p");
                    assertEquals(e.get("dy").getAsInt(), f.dy(), what + " dy");
                    assertEquals(e.get("dy0").getAsInt(), f.dy0(), what + " dy0");
                    TabletVectors.box(e.get("D"), f.d(), () -> what + " D");
                    TabletVectors.box(e.get("S"), f.s(), () -> what + " S");
                    TabletVectors.box(e.get("P"), f.display(), () -> what + " P");
                    TabletVectors.box(e.get("E"), f.e(), () -> what + " E");
                    near(e.get("backdrop"), f.backdrop(), () -> what + " backdrop");
                    assertEquals(e.get("glass").getAsString(), f.glass().name().toLowerCase(Locale.ROOT),
                            what + " glass");
                    assertEquals(scan(e.get("scanY")), f.scanY(), what + " scanY");
                    assertEquals(e.get("identity").getAsBoolean(), f.identity(), what + " identity");
                    assertEquals(e.getAsJsonObject("offset").get("x").getAsInt(), f.offsetX(), what);
                    assertEquals(e.getAsJsonObject("offset").get("y").getAsInt(), f.offsetY(), what);
                    assertEquals(e.get("visible").getAsBoolean(), f.visible(), what + " visible");
                    assertEquals(e.get("content").getAsBoolean(), f.content(), what + " content");
                    assertEquals(e.get("phaseName").getAsString(), f.phaseName(), what + " phase");
                    hud(e.getAsJsonObject("hud"), f.hideCrosshair(), f.hideHud(), what);
                } else {
                    TabletPath2D.MapFrame f = TabletPath2D.mapFrame(p, map, term, open, quick);
                    near(e.get("p"), f.p(), () -> what + " p");
                    assertEquals(e.get("cs").getAsInt(), f.cs(), what + " cs");
                    TabletVectors.box(e.get("rest"), f.rest(), () -> what + " rest");
                    TabletVectors.box(e.get("inner"), f.inner(), () -> what + " inner");
                    TabletVectors.box(e.get("D"), f.d(), () -> what + " D");
                    TabletVectors.box(e.get("S"), f.s(), () -> what + " S");
                    TabletVectors.box(e.get("P"), f.display(), () -> what + " P");
                    TabletVectors.box(e.get("E"), f.e(), () -> what + " E");
                    assertEquals(e.get("dy").getAsInt(), f.dy(), what + " dy");
                    near(e.get("k"), f.k(), () -> what + " k");
                    assertEquals(e.get("phase").getAsString(), f.phase().name().toLowerCase(Locale.ROOT),
                            what + " phase");
                    assertEquals(e.getAsJsonObject("offset").get("x").getAsInt(), f.offsetX(), what);
                    assertEquals(e.getAsJsonObject("offset").get("y").getAsInt(), f.offsetY(), what);
                    near(e.get("backdrop"), f.backdrop(), () -> what + " backdrop");
                    near(e.get("sleepAlpha"), f.sleepAlpha(), () -> what + " sleepAlpha");
                    assertEquals(e.get("glass").getAsString(), f.glass().name().toLowerCase(Locale.ROOT),
                            what + " glass");
                    assertEquals(scan(e.get("scanY")), f.scanY(), what + " scanY");
                    assertEquals(e.get("identity").getAsBoolean(), f.identity(), what + " identity");
                    near(e.getAsJsonObject("art").get("pw"), f.artW(), () -> what + " art.pw");
                    near(e.getAsJsonObject("art").get("ph"), f.artH(), () -> what + " art.ph");
                    assertEquals(e.get("visible").getAsBoolean(), f.visible(), what + " visible");
                    assertEquals(e.get("content").getAsBoolean(), f.content(), what + " content");
                    assertEquals(e.get("phaseName").getAsString(), f.phaseName(), what + " phase");
                    hud(e.getAsJsonObject("hud"), f.hideCrosshair(), f.hideHud(), what);
                }
                frames++;
            }
        }
        assertEquals(40 * 19, frames, "40 groups × 19 frames");
    }

    @Test
    void mapDeviceTopAt640x360MatchesDesignTable() {
        TabletUnits term = TabletVectors.termUnits("640x360");
        TabletUnits map = TabletVectors.mapUnits("640x360");
        int[] expected = {281, 164, 89, 46, 27, 22, 20, 4, -12, -14};
        JsonArray rows = TabletVectors.array("mapDeviceTop640");
        assertEquals(expected.length, rows.size());
        for (int i = 0; i < rows.size(); i++) {
            double t = rows.get(i).getAsJsonArray().get(0).getAsDouble();
            int top = TabletPath2D.mapFrame(t / 200.0D, map, term, true, false).e().y();
            assertEquals(rows.get(i).getAsJsonArray().get(1).getAsInt(), top, "t=" + t);
            assertEquals(expected[i], top, "DESIGN 4.2 t=" + t);
        }
    }

    @Test
    void visibilityOfClicksMatchesThePreview() {
        for (JsonElement element : TabletVectors.array("termVisible")) {
            JsonObject rec = element.getAsJsonObject();
            String tier = rec.get("tier").getAsString();
            double p = rec.get("p").getAsDouble();
            boolean terminal = rec.get("kind").getAsString().equals("terminal");
            TabletUnits term = TabletVectors.termUnits(tier);
            TabletUnits map = TabletVectors.mapUnits(tier);
            for (JsonElement pe : rec.getAsJsonArray("points")) {
                JsonArray pt = pe.getAsJsonArray();
                double x = pt.get(0).getAsDouble();
                double y = pt.get(1).getAsDouble();
                boolean visible = terminal
                        ? TabletPath2D.termVisible(TabletPath2D.termFrame(p, term, true, false), x, y)
                        : TabletPath2D.mapVisible(TabletPath2D.mapFrame(p, map, term, true, false), x, y);
                assertEquals(pt.get(2).getAsBoolean(), visible, rec.get("kind") + " " + tier + " p=" + p
                        + " (" + x + ", " + y + ")");
            }
        }
    }

    @Test
    void perTierLiftNumbersMatchThePreview() {
        for (String id : TIERS) {
            JsonObject tier = TabletVectors.tier(id);
            TabletUnits term = TabletVectors.termUnits(id);
            TabletUnits map = TabletVectors.mapUnits(id);
            TabletD2Geometry g = TabletD2Geometry.of(term.lpW(), term.lpH());
            assertEquals(tier.get("termDy0").getAsInt(), TabletPath2D.termDy0(g), id);
            assertEquals(tier.get("liftTravelTermPx").getAsInt(), TabletPath2D.termLiftTravelPx(term), id);
            assertEquals(tier.get("liftTravelMapPx").getAsInt(), TabletPath2D.mapLiftTravelPx(map, term), id);
            int cs = TabletPath2D.mapCs(map, term);
            assertEquals(tier.get("mapCs").getAsInt(), cs, id);
            TabletVectors.box(tier.get("mapRest"), TabletPath2D.rest(map.lpW(), map.lpH(), cs), () -> id + " rest");
            JsonObject bz = tier.getAsJsonObject("mapBezel");
            TabletPath2D.Bezel bezel = TabletPath2D.mapBezel(g, cs);
            assertEquals(bz.get("side").getAsInt(), bezel.side(), id);
            assertEquals(bz.get("top").getAsInt(), bezel.top(), id);
            assertEquals(bz.get("bot").getAsInt(), bezel.bot(), id);
            assertEquals(bz.get("bump").getAsInt(), bezel.bump(), id);
            assertEquals(bz.get("glass").getAsInt(), bezel.glass(), id);
        }
    }

    // ---- DESIGN 6.6 properties ------------------------------------------------------------------

    @Test
    void terminalStartsBelowTheEdgeEndsAsIdentityAndOnlyRises() {
        for (String id : TIERS) {
            TabletUnits term = TabletVectors.termUnits(id);
            for (boolean quick : new boolean[]{false, true}) {
                TabletPath2D.TermFrame first = TabletPath2D.termFrame(quick ? 0.6 : 0.0, term, true, quick);
                assertEquals(term.lpH(), first.e().y(), id + " device top (bumpers) at the bottom edge");
                TabletPath2D.TermFrame last = TabletPath2D.termFrame(1.0D, term, true, quick);
                assertTrue(last.identity() && last.dy() == 0 && last.glass() == TabletPath2D.TermGlass.LIT, id);
                int previous = Integer.MAX_VALUE;
                int previousStep = Integer.MAX_VALUE;
                double step = TabletVectors.DT / (quick ? 80.0D / 0.4D : 200.0D);
                for (double p = quick ? 0.6 : 0.0; p <= 1.0D + 1e-12; p += step) {
                    TabletPath2D.TermFrame f = TabletPath2D.termFrame(p, term, true, quick);
                    assertTrue(f.dy() <= previous, id + " dy never grows (p=" + p + ")");
                    if (previous != Integer.MAX_VALUE && !quick) {
                        int moved = previous - f.dy();
                        assertTrue(previousStep == Integer.MAX_VALUE || moved <= previousStep + 1,
                                id + " frames slow down (p=" + p + ")");
                        previousStep = moved;
                    }
                    if (p >= TabletAnimationModel.B_LIFT_END && !quick) {
                        assertEquals(0, f.dy(), id + " lifted from 0.55");
                    }
                    previous = f.dy();
                }
                if (!quick) {
                    int firstFrame = first.dy() - TabletPath2D.termFrame(step, term, true, false).dy();
                    assertTrue(firstFrame <= 0.30D * term.lpH() + 1, id + " first frame covers ≤ 30 % of the height");
                }
            }
        }
    }

    @Test
    void terminalBackdropScanAndCloseRules() {
        TabletUnits term = TabletVectors.termUnits("480x270");
        assertEquals(0.3D / 0.55D, TabletPath2D.termFrame(0.3, term, true, false).backdrop(), 1e-12);
        assertEquals(1.0D, TabletPath2D.termFrame(0.7, term, true, false).backdrop(), 0.0D);
        assertEquals(0.5D, TabletPath2D.termFrame(0.8, term, true, true).backdrop(), 1e-12);
        assertTrue(TabletPath2D.termFrame(0.7, term, true, false).hasScan(), "scan while opening");
        assertFalse(TabletPath2D.termFrame(0.7, term, false, false).hasScan(), "no scan while closing");
        assertEquals(TabletPath2D.TermGlass.CLEAR, TabletPath2D.termFrame(0.3, term, false, false).glass());
        assertEquals(0.55D, TabletMotion.closeMap(TabletPath.B2D, TabletHand.GUN, true,
                TabletScreenKind.SQUAD).startP(), 0.0D, "the terminal falls from 0.55");
        assertEquals(1.0D, TabletMotion.closeMap(TabletPath.B2D, TabletHand.GUN, true,
                TabletScreenKind.FULLSCREEN).startP(), 0.0D, "the map backs off from 1");
    }

    @Test
    void mapStartsAt78PercentAndEndsFullScreen() {
        for (String id : TIERS) {
            TabletUnits term = TabletVectors.termUnits(id);
            TabletUnits map = TabletVectors.mapUnits(id);
            TabletPath2D.MapFrame start = TabletPath2D.mapFrame(0.0D, map, term, true, false);
            assertEquals((int) Math.round(0.78D * map.lpH()), start.e().y(), id + " device top at 0.78H");
            TabletPath2D.MapFrame end = TabletPath2D.mapFrame(1.0D, map, term, true, false);
            assertTrue(end.identity(), id);
            assertEquals(new TabletPath2D.Box(0, 0, map.lpW(), map.lpH()), end.inner(), id + " P = full screen");
            assertEquals(end.inner(), end.display(), id + " P is the inner screen");
        }
    }

    @Test
    void terminalScreensAreAWhiteList() {
        assertTrue(TabletScreenKind.byPreviewName("squad").isTerminal());
        assertTrue(TabletScreenKind.byPreviewName("formation").isTerminal());
        assertFalse(TabletScreenKind.byPreviewName("map").isTerminal());
        assertFalse(TabletScreenKind.byPreviewName("loadout").isTerminal());
        assertFalse(TabletScreenKind.byPreviewName("anything").isTerminal());
        assertTrue(TabletScreenKind.SQUAD.closesOnTerminalKey());
        assertFalse(TabletScreenKind.TERMINAL.closesOnTerminalKey());
        assertFalse(TabletScreenKind.FULLSCREEN.closesOnTerminalKey());
    }
}
