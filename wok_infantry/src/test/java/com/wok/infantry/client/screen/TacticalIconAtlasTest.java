package com.wok.infantry.client.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the generated UI sheets (ui-preview/tools/export-ui-atlas.mjs): the PNGs are the
 * generator's output, every {@link TacticalIcon} UV points at its own glyph, and the hatch tile
 * reproduces the disabled-key hatch.
 */
class TacticalIconAtlasTest {
    static final String TEXTURES = "assets/wok_infantry/textures/gui/";
    static final String MANIFEST = "ui_atlas/ui_atlas_manifest.json";

    @Test
    void sheetsAreTheGeneratorOutput() throws Exception {
        JsonObject sha = manifest().getAsJsonObject("sha256");
        for (String png : new String[]{"ui_icons.png", "ui_hatch.png", "map_icons.png"}) {
            assertEquals(sha.get(png).getAsString(), sha256(TEXTURES + png),
                    png + " must be regenerated with export-ui-atlas.mjs, not edited by hand");
        }
    }

    @Test
    void iconEnumFollowsTheSheetOrder() throws IOException {
        JsonObject icons = manifest().getAsJsonObject("uiIcons");
        JsonArray list = icons.getAsJsonArray("icons");
        assertEquals(TacticalIcon.values().length, list.size(),
                "TacticalIcon must list every MONO_ICONS glyph");
        assertEquals(TacticalIcon.SIZE, icons.get("size").getAsInt());
        assertEquals(TacticalIcon.PITCH, icons.get("pitch").getAsInt());
        assertEquals(TacticalIcon.COLUMNS, icons.get("columns").getAsInt());
        assertEquals(TacticalTextures.UI_ICONS_WIDTH, icons.get("width").getAsInt());
        assertEquals(TacticalTextures.UI_ICONS_HEIGHT, icons.get("height").getAsInt());
        for (TacticalIcon icon : TacticalIcon.values()) {
            JsonObject entry = list.get(icon.ordinal()).getAsJsonObject();
            assertEquals(entry.get("name").getAsString(), icon.id(), "order of " + icon);
            assertEquals(entry.get("u").getAsInt(), icon.u(), "u of " + icon);
            assertEquals(entry.get("v").getAsInt(), icon.v(), "v of " + icon);
        }
    }

    @Test
    void everyIconUvShowsItsOwnGlyph() throws IOException {
        BufferedImage sheet = image(TEXTURES + "ui_icons.png");
        assertEquals(TacticalTextures.UI_ICONS_WIDTH, sheet.getWidth());
        assertEquals(TacticalTextures.UI_ICONS_HEIGHT, sheet.getHeight());
        JsonArray list = manifest().getAsJsonObject("uiIcons").getAsJsonArray("icons");
        boolean[][] used = new boolean[sheet.getWidth()][sheet.getHeight()];
        for (TacticalIcon icon : TacticalIcon.values()) {
            JsonArray rows = list.get(icon.ordinal()).getAsJsonObject().getAsJsonArray("rows");
            for (int y = 0; y < TacticalIcon.SIZE; y++) {
                String row = rows.get(y).getAsString();
                for (int x = 0; x < TacticalIcon.SIZE; x++) {
                    int argb = sheet.getRGB(icon.u() + x, icon.v() + y);
                    used[icon.u() + x][icon.v() + y] = true;
                    if (row.charAt(x) == '#') {
                        assertEquals(0xFFFFFFFF, argb, icon + " at " + x + "," + y);
                    } else {
                        assertEquals(0, argb >>> 24, icon + " must be transparent at " + x + "," + y);
                    }
                }
            }
        }
        for (int x = 0; x < sheet.getWidth(); x++) {
            for (int y = 0; y < sheet.getHeight(); y++) {
                if (!used[x][y]) {
                    assertEquals(0, sheet.getRGB(x, y) >>> 24,
                            "gutters and free cells stay transparent (" + x + "," + y + ")");
                }
            }
        }
    }

    @Test
    void iconsAreLaidOutOnTheTenPixelPitch() {
        assertEquals(0, TacticalIcon.CLOSE.u());
        assertEquals(0, TacticalIcon.CLOSE.v());
        assertEquals(140, TacticalIcon.PERSON.u());
        assertEquals(150, TacticalIcon.LEADER.u());
        assertEquals(0, TacticalIcon.LEADER.v());
        assertEquals(0, TacticalIcon.COMMANDER.u());
        assertEquals(10, TacticalIcon.COMMANDER.v());
        assertEquals(120, TacticalIcon.SHIELD.u());
        assertEquals(20, TacticalIcon.SHIELD.v());
        assertEquals(Optional.of(TacticalIcon.CHECK), TacticalIcon.byId("check"));
        assertEquals(Optional.empty(), TacticalIcon.byId("rifle"));
        assertEquals(Optional.empty(), TacticalIcon.byId(null));
        assertTrue(TacticalIcon.values().length <= TacticalIcon.COLUMNS
                * (TacticalTextures.UI_ICONS_HEIGHT / TacticalIcon.PITCH));
    }

    @Test
    void iconsCentreLikeThePreview() {
        assertEquals(2, TacticalIcon.centeredStart(0, 14));
        assertEquals(5, TacticalIcon.centeredStart(0, 20));
        assertEquals(103, TacticalIcon.centeredStart(100, 115));
        assertEquals(-1, TacticalIcon.centeredStart(0, 8), "floor when the box is narrower");
    }

    @Test
    void hatchTileMatchesTheDisabledKeyHatch() throws IOException {
        BufferedImage tile = image(TEXTURES + "ui_hatch.png");
        assertEquals(TacticalTextures.UI_HATCH_SIZE, tile.getWidth());
        assertEquals(TacticalTextures.UI_HATCH_SIZE, tile.getHeight());
        assertEquals(TacticalButtonStyle.HATCH_SPACING, TacticalTextures.UI_HATCH_SIZE);
        for (int x = 0; x < tile.getWidth(); x++) {
            for (int y = 0; y < tile.getHeight(); y++) {
                int argb = tile.getRGB(x, y);
                if (TacticalButtonStyle.hatchCovers(x, y)) {
                    assertEquals(TacticalBoardTheme.HATCH, argb, "hatch texel " + x + "," + y);
                } else {
                    assertEquals(0, argb >>> 24, "clear texel " + x + "," + y);
                }
            }
        }
    }

    @Test
    void tiledHatchKeepsThePerPixelPattern() {
        // B2a drew one dot at x = left + floorMod(y - top, 4) every 4px; GL_REPEAT tiling from
        // the region's top-left must cover exactly the same pixels for any region size.
        for (int width = 1; width <= 13; width++) {
            for (int height = 1; height <= 9; height++) {
                for (int dy = 0; dy < height; dy++) {
                    for (int dx = 0; dx < width; dx++) {
                        boolean former = Math.floorMod(dx - Math.floorMod(dy, 4), 4) == 0;
                        int tx = dx % TacticalTextures.UI_HATCH_SIZE;
                        int ty = dy % TacticalTextures.UI_HATCH_SIZE;
                        assertEquals(former, TacticalButtonStyle.hatchCovers(tx, ty),
                                width + "x" + height + " at " + dx + "," + dy);
                    }
                }
            }
        }
    }

    @Test
    void hatchRepeatsAndIconSheetsDoNotBlur() throws IOException {
        assertFalse(mcmeta("ui_hatch.png").get("clamp").getAsBoolean(),
                "the hatch tile relies on GL_REPEAT");
        for (String png : new String[]{"ui_icons.png", "ui_hatch.png", "map_icons.png"}) {
            assertFalse(mcmeta(png).get("blur").getAsBoolean(), png + " must stay pixel-sharp");
        }
    }

    static JsonObject manifest() throws IOException {
        return json(MANIFEST);
    }

    static JsonObject json(String path) throws IOException {
        try (InputStream stream = resource(path)) {
            JsonElement root = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            return root.getAsJsonObject();
        }
    }

    static BufferedImage image(String path) throws IOException {
        try (InputStream stream = resource(path)) {
            BufferedImage image = ImageIO.read(stream);
            assertNotNull(image, path + " must be a readable PNG");
            return image;
        }
    }

    static String sha256(String path) throws IOException, NoSuchAlgorithmException {
        try (InputStream stream = resource(path)) {
            return HexFormat.of().withUpperCase().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(stream.readAllBytes()));
        }
    }

    private static JsonObject mcmeta(String png) throws IOException {
        return json(TEXTURES + png + ".mcmeta").getAsJsonObject("texture");
    }

    private static InputStream resource(String path) {
        InputStream stream = TacticalIconAtlasTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, path + " must be on the test classpath");
        return stream;
    }
}
