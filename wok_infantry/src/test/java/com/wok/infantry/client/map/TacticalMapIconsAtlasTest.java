package com.wok.infantry.client.map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wok.infantry.battle.TacticalMarkerType;
import com.wok.infantry.client.map.TacticalMapIcons.IconState;
import com.wok.infantry.client.map.TacticalMapIcons.MapIcon;
import com.wok.infantry.client.map.TacticalMapIcons.Placement;
import com.wok.infantry.client.map.TacticalMapIcons.Plate;
import com.wok.infantry.client.screen.TacticalBoardTheme;
import com.wok.infantry.client.screen.TacticalTextures;
import com.wok.infantry.deployment.DeploymentPointKind;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the generated map marker sheet, its UVs and the physical-pixel placement. */
class TacticalMapIconsAtlasTest {
    private static final String SHEET = "assets/wok_infantry/textures/gui/map_icons.png";
    private static final String MANIFEST = "ui_atlas/ui_atlas_manifest.json";

    private static BufferedImage sheet;
    private static JsonObject map;
    private static final Map<String, JsonObject> cells = new HashMap<>();

    @BeforeAll
    static void load() throws IOException {
        try (InputStream stream = resource(SHEET)) {
            sheet = ImageIO.read(stream);
        }
        assertNotNull(sheet, "map_icons.png must be readable");
        try (InputStream stream = resource(MANIFEST)) {
            JsonElement root = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            map = root.getAsJsonObject().getAsJsonObject("mapIcons");
        }
        for (JsonElement element : map.getAsJsonArray("cells")) {
            JsonObject cell = element.getAsJsonObject();
            cells.put(cell.get("name").getAsString(), cell);
        }
    }

    @Test
    void sheetGeometryMatchesTheGenerator() {
        assertEquals(TacticalTextures.MAP_ICONS_SIZE, sheet.getWidth());
        assertEquals(TacticalTextures.MAP_ICONS_SIZE, sheet.getHeight());
        assertEquals(TacticalMapIcons.CELL, map.get("cell").getAsInt());
        assertEquals(TacticalMapIcons.COLUMNS, map.get("columns").getAsInt());
        assertEquals(TacticalMapIcons.ORIGIN, map.get("origin").getAsInt());
        assertEquals(TacticalMapIcons.SHADOW_ALPHA, map.get("shadowAlpha").getAsInt());
        assertEquals(TacticalMapIcons.HOVER_RING, color(map.get("hoverRing").getAsString()));
        assertEquals(TacticalMapIcons.SELECTED_RING, color(map.get("selectedRing").getAsString()));
        assertEquals(TacticalMapIcons.OUTLINE, color(map.get("outline").getAsString()));
        for (JsonObject cell : cells.values()) {
            int index = cell.get("index").getAsInt();
            assertEquals(cell.get("u").getAsInt(), TacticalMapIcons.cellU(index));
            assertEquals(cell.get("v").getAsInt(), TacticalMapIcons.cellV(index));
            assertTrue(TacticalMapIcons.cellU(index) + TacticalMapIcons.CELL <= sheet.getWidth());
            assertTrue(TacticalMapIcons.cellV(index) + TacticalMapIcons.CELL <= sheet.getHeight());
        }
    }

    @Test
    void platesPointAtTheirOwnCells() {
        for (Plate plate : Plate.values()) {
            String id = plate.name().toLowerCase(Locale.ROOT);
            String ring = plate == Plate.CIRCLE_DASHED ? "circle" : id;
            assertEquals(index("plate_" + id + "_fill"), plate.fillCell(), plate + " fill");
            assertEquals(index("plate_" + id + "_edge"), plate.edgeCell(), plate + " edge");
            assertEquals(index("ring_" + ring), plate.ringCell(), plate + " ring");
            JsonObject size = map.getAsJsonObject("plates").getAsJsonObject(
                    plate == Plate.CIRCLE_DASHED ? "circle" : id);
            assertEquals(size.get("w").getAsInt(), plate.width());
            assertEquals(size.get("h").getAsInt(), plate.height());
            assertEquals(Math.round(size.get("ax").getAsDouble() * 2), plate.anchorHalfX());
            assertEquals(Math.round(size.get("ay").getAsDouble() * 2), plate.anchorHalfY());
        }
    }

    @Test
    void markerKindsFollowTheSheetAndThePreviewColours() {
        JsonArray icons = map.getAsJsonArray("icons");
        assertEquals(MapIcon.values().length, icons.size());
        for (MapIcon icon : MapIcon.values()) {
            JsonObject entry = icons.get(icon.ordinal()).getAsJsonObject();
            assertEquals(entry.get("name").getAsString(), icon.id(), "order of " + icon);
            assertEquals(entry.get("dashed").getAsBoolean(), icon.dashed(), icon + " dashed");
            String plate = entry.get("plate").getAsString();
            assertEquals(plate, (icon.plate() == Plate.CIRCLE_DASHED ? Plate.CIRCLE : icon.plate())
                    .name().toLowerCase(Locale.ROOT), icon + " plate");
            assertEquals(color(entry.get("color").getAsString()), icon.color(), icon + " colour");
            assertEquals(index(entry.get("glyph").getAsString()), icon.glyphCell(), icon + " glyph");
            assertTrue(icon.labelKey().startsWith("marker.wok_infantry."));
        }
        assertEquals(TacticalBoardTheme.MAP_ICON_RECON, MapIcon.RECON_CONTACT.color());
        assertTrue(MapIcon.RECON_CONTACT.dashed(), "an unconfirmed contact has a dashed outline");
    }

    @Test
    void everyMarkerTypeAndDeploymentPointHasAnIcon() {
        Set<MapIcon> used = EnumSet.noneOf(MapIcon.class);
        for (TacticalMarkerType type : TacticalMarkerType.values()) {
            MapIcon icon = MapIcon.of(type);
            assertEquals(type.id(), icon.id(), "marker " + type);
            used.add(icon);
        }
        assertEquals(MapIcon.MAIN_BASE, MapIcon.of(DeploymentPointKind.MAIN_BASE));
        assertEquals(MapIcon.FIELD_BEACON, MapIcon.of(DeploymentPointKind.FIELD_BEACON));
        assertEquals(MapIcon.RALLY_PACK, MapIcon.of(DeploymentPointKind.RALLY));
        for (DeploymentPointKind kind : DeploymentPointKind.values()) {
            assertEquals(Plate.PIN, MapIcon.of(kind).plate(), "deployment points are pins");
            used.add(MapIcon.of(kind));
        }
        assertEquals(EnumSet.allOf(MapIcon.class), used);
    }

    @Test
    void plateLayersFormTheWholePlateWithAShadowOnly() {
        for (Plate plate : Plate.values()) {
            int[][] fill = layer(plate.fillCell());
            int[][] edge = layer(plate.edgeCell());
            int plateTexels = 0;
            for (int y = 0; y < TacticalMapIcons.CELL; y++) {
                for (int x = 0; x < TacticalMapIcons.CELL; x++) {
                    int f = fill[x][y];
                    int e = edge[x][y];
                    boolean inPlate = x >= TacticalMapIcons.ORIGIN && y >= TacticalMapIcons.ORIGIN
                            && x < TacticalMapIcons.ORIGIN + plate.width()
                            && y < TacticalMapIcons.ORIGIN + plate.height();
                    if (f != 0) {
                        assertEquals(0xFFFFFFFF, f, plate + " fill is white");
                        assertTrue(inPlate, plate + " fill stays inside the plate");
                        assertFalse(e == 0xFFFFFFFF, plate + " fill and outline never overlap");
                    }
                    if (e == 0xFFFFFFFF) {
                        assertTrue(inPlate, plate + " outline stays inside the plate");
                    } else if (e != 0) {
                        assertEquals(TacticalMapIcons.SHADOW_ALPHA << 24, e,
                                plate + " shadow texel at " + x + "," + y);
                    }
                    if (f != 0 || e == 0xFFFFFFFF) {
                        plateTexels++;
                    }
                }
            }
            assertTrue(plateTexels > plate.width() * plate.height() / 2, plate + " is solid");
            assertBorderClear(plate.fillCell());
            assertBorderClear(plate.edgeCell());
        }
    }

    @Test
    void dashedContactIsTheCircleWithAnInterruptedOutline() {
        int[][] fill = layer(Plate.CIRCLE.fillCell());
        int[][] edge = layer(Plate.CIRCLE.edgeCell());
        int[][] dashedFill = layer(Plate.CIRCLE_DASHED.fillCell());
        int[][] dashedEdge = layer(Plate.CIRCLE_DASHED.edgeCell());
        int outline = 0;
        int dashed = 0;
        for (int y = 0; y < TacticalMapIcons.CELL; y++) {
            for (int x = 0; x < TacticalMapIcons.CELL; x++) {
                boolean solid = fill[x][y] != 0 || edge[x][y] == 0xFFFFFFFF;
                boolean dash = dashedFill[x][y] != 0 || dashedEdge[x][y] == 0xFFFFFFFF;
                assertEquals(solid, dash, "same plate shape at " + x + "," + y);
                outline += edge[x][y] == 0xFFFFFFFF ? 1 : 0;
                dashed += dashedEdge[x][y] == 0xFFFFFFFF ? 1 : 0;
            }
        }
        assertTrue(dashed > outline / 3 && dashed < outline * 2 / 3,
                "about half of the outline is dashed away: " + dashed + " of " + outline);
    }

    @Test
    void ringsSurroundThePlateWithoutTouchingIt() {
        for (Plate plate : new Plate[]{Plate.CIRCLE, Plate.SQUARE, Plate.PIN}) {
            int[][] ring = layer(plate.ringCell());
            int[][] fill = layer(plate.fillCell());
            int[][] edge = layer(plate.edgeCell());
            int count = 0;
            for (int y = 0; y < TacticalMapIcons.CELL; y++) {
                for (int x = 0; x < TacticalMapIcons.CELL; x++) {
                    if (ring[x][y] == 0) {
                        continue;
                    }
                    count++;
                    assertEquals(0xFFFFFFFF, ring[x][y], plate + " ring is white");
                    assertEquals(0, fill[x][y], plate + " ring stays outside the plate");
                    assertFalse(edge[x][y] == 0xFFFFFFFF, plate + " ring misses the outline");
                    assertTrue(x >= TacticalMapIcons.ORIGIN - 2 && y >= TacticalMapIcons.ORIGIN - 2
                                    && x < TacticalMapIcons.ORIGIN + plate.width() + 2
                                    && y < TacticalMapIcons.ORIGIN + plate.height() + 2,
                            plate + " ring is at most 2 art pixels out");
                }
            }
            assertTrue(count > 0, plate + " has a ring");
            assertBorderClear(plate.ringCell());
        }
    }

    @Test
    void silhouettesAreWhiteInsideTheHead() {
        for (MapIcon icon : MapIcon.values()) {
            int[][] glyph = layer(icon.glyphCell());
            int count = 0;
            for (int y = 0; y < TacticalMapIcons.CELL; y++) {
                for (int x = 0; x < TacticalMapIcons.CELL; x++) {
                    if (glyph[x][y] == 0) {
                        continue;
                    }
                    count++;
                    assertEquals(0xFFFFFFFF, glyph[x][y], icon + " silhouette is white");
                    int artX = x - TacticalMapIcons.ORIGIN;
                    int artY = y - TacticalMapIcons.ORIGIN;
                    assertTrue(artX >= 2 && artX <= 12 && artY >= 2 && artY <= 12,
                            icon + " silhouette inside the 11×11 head area");
                }
            }
            assertTrue(count >= 20, icon + " silhouette is drawn");
        }
    }

    @Test
    void iconScaleKeepsWholePhysicalPixels() {
        assertEquals(2, TacticalMapIcons.physicalPerArt(0.75D));
        assertEquals(2, TacticalMapIcons.physicalPerArt(1.0D));
        assertEquals(3, TacticalMapIcons.physicalPerArt(1.25D));
        assertEquals(3, TacticalMapIcons.physicalPerArt(1.5D));
        assertEquals(4, TacticalMapIcons.physicalPerArt(1.75D));
        assertEquals(1, TacticalMapIcons.physicalPerArt(0.1D));
        assertEquals(2, TacticalMapIcons.physicalPerArt(Double.NaN));
    }

    @Test
    void circlesAndSquaresCentreOnTheAnchor() {
        Placement even = TacticalMapIcons.place(MapIcon.TANK, 100.0D, 100.0D, 2);
        assertEquals(85, even.plateLeft());
        assertEquals(85, even.plateTop());
        assertEquals(115, even.plateRight());
        assertEquals(115, even.plateBottom());
        assertEquals(30, even.plateRight() - even.plateLeft(), "30 px plate at scale 1");
        assertEquals(85 - 6, even.cellLeft());
        assertEquals(48, even.cellSize());

        Placement odd = TacticalMapIcons.place(MapIcon.DEFEND, 100.0D, 100.0D, 3);
        assertEquals(78, odd.plateLeft(), "half-pixel offset rounds towards the top-left");
        assertEquals(123, odd.plateRight());
        assertEquals(45, odd.plateRight() - odd.plateLeft());
        assertEquals(78 - 9, odd.cellLeft());
    }

    @Test
    void anchorIsRoundedToAWholePhysicalPixelFirst() {
        Placement fractional = TacticalMapIcons.place(MapIcon.INFANTRY, 100.4D, 99.6D, 3);
        Placement whole = TacticalMapIcons.place(MapIcon.INFANTRY, 100.0D, 100.0D, 3);

        assertEquals(whole, fractional);
        assertEquals(TacticalMapIcons.place(MapIcon.INFANTRY, 101.0D, 101.0D, 3),
                TacticalMapIcons.place(MapIcon.INFANTRY, 100.5D, 100.5D, 3));
    }

    @Test
    void pinTipEndsOnTheAnchor() {
        for (int px = 1; px <= 4; px++) {
            Placement pin = TacticalMapIcons.place(MapIcon.RALLY_PACK, 50.0D, 80.0D, px);
            assertEquals(80, pin.plateBottom(), "tip at the anchor for " + px + " px");
            assertEquals(80 - 18 * px, pin.plateTop());
        }
    }

    @Test
    void guiCoordinatesConvertThroughTheFullPhysicalScale() {
        // A tactical screen at GUI 1 drawn at the minimum 2×: pose scale 2, window scale 1.
        assertEquals(20.5D, TacticalMapIcons.physical(10.25F, 2.0F, 0.0F, 1.0D), 1.0E-9);
        assertEquals(61.5D, TacticalMapIcons.physical(10.0F, 2.0F, 0.5F, 3.0D), 1.0E-9);
        Placement gui = TacticalMapIcons.placeGui(MapIcon.TANK, 50.0D, 50.0D, 2, 2.0D);
        assertEquals(TacticalMapIcons.place(MapIcon.TANK, 100.0D, 100.0D, 2), gui);
    }

    @Test
    void hitBoxIsThePlatePlusOneArtPixel() {
        Placement tank = TacticalMapIcons.place(MapIcon.TANK, 100.0D, 100.0D, 2);
        assertTrue(tank.hit(100.0D, 100.0D));
        assertTrue(tank.hit(83.0D, 100.0D), "one art pixel outside the plate still hits");
        assertFalse(tank.hit(82.9D, 100.0D));
        assertFalse(tank.hit(100.0D, 117.0D));
        assertTrue(TacticalMapIcons.hitGui(MapIcon.TANK, 50.0D, 50.0D, 2, 2.0D, 42.0D, 50.0D));
        assertFalse(TacticalMapIcons.hitGui(MapIcon.TANK, 50.0D, 50.0D, 2, 2.0D, 41.0D, 50.0D));
    }

    @Test
    void ringsFollowTheInteractionState() {
        assertEquals(0, TacticalMapIcons.ringColor(IconState.NORMAL));
        assertEquals(0xB0FFFFFF, TacticalMapIcons.ringColor(IconState.HOVER));
        assertEquals(0xFFFFE36E, TacticalMapIcons.ringColor(IconState.SELECTED));
        assertEquals(0, TacticalMapIcons.ringColor(null));
    }

    private static int index(String cell) {
        JsonObject entry = cells.get(cell);
        assertNotNull(entry, "manifest cell " + cell);
        return entry.get("index").getAsInt();
    }

    private static int[][] layer(int cell) {
        int u = TacticalMapIcons.cellU(cell);
        int v = TacticalMapIcons.cellV(cell);
        int[][] texels = new int[TacticalMapIcons.CELL][TacticalMapIcons.CELL];
        for (int y = 0; y < TacticalMapIcons.CELL; y++) {
            for (int x = 0; x < TacticalMapIcons.CELL; x++) {
                int argb = sheet.getRGB(u + x, v + y);
                texels[x][y] = (argb >>> 24) == 0 ? 0 : argb;
            }
        }
        return texels;
    }

    private static void assertBorderClear(int cell) {
        int[][] texels = layer(cell);
        int last = TacticalMapIcons.CELL - 1;
        for (int i = 0; i <= last; i++) {
            assertEquals(0, texels[i][0], "cell " + cell + " top border");
            assertEquals(0, texels[i][last], "cell " + cell + " bottom border");
            assertEquals(0, texels[0][i], "cell " + cell + " left border");
            assertEquals(0, texels[last][i], "cell " + cell + " right border");
        }
    }

    private static int color(String hex) {
        return (int) Long.parseLong(hex.substring(2), 16);
    }

    private static InputStream resource(String path) {
        InputStream stream = TacticalMapIconsAtlasTest.class.getClassLoader()
                .getResourceAsStream(path);
        assertNotNull(stream, path + " must be on the test classpath");
        return stream;
    }
}
