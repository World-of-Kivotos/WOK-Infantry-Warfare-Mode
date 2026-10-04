package com.wok.commandersupport.drone;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The texture, model table and data-pack tag the client and other mods rely on. */
class ReconDroneAirframeTest {
    private static final String TEXTURE =
            "/assets/wok_commander_support/textures/entity/recon_drone.png";
    private static final String PANTSIR_TAG = "/data/vvp/tags/entity_types/pantsir_air_target.json";
    /** Hit box of the entity type (CommanderSupportEntities). */
    private static final double HIT_BOX_WIDTH = 4.0D;
    private static final double HIT_BOX_HEIGHT = 1.2D;

    @Test
    void theAirframeLooksLikeAMaleFixedWingDrone() {
        double span = ReconDroneAirframe.wingspanBlocks();
        double length = ReconDroneAirframe.lengthBlocks();
        assertEquals(12.0D, span, 0.01D, "wingspan of about 12 blocks");
        assertTrue(length > 6.0D && length < 7.0D, "fuselage length " + length);
        // Long, slender straight wing: span about twice the length, as on MQ-1 / TB2.
        assertTrue(span / length > 1.7D && span / length < 2.1D);
        // The hit box covers the fuselage and the inner wing around the entity position.
        assertTrue(HIT_BOX_WIDTH < span && HIT_BOX_WIDTH < length);
        assertEquals(HIT_BOX_HEIGHT / 2.0D, ReconDroneAirframe.MODEL_CENTER_HEIGHT, 1.0E-6D);
    }

    @Test
    void everyPartIsEitherLitByTheWorldOrALight() {
        Set<String> names = new HashSet<>();
        for (ReconDroneAirframe.Part part : ReconDroneAirframe.parts()) {
            assertTrue(names.add(part.name()), "duplicate part " + part.name());
            assertFalse(part.boxes().isEmpty(), part.name());
        }
        Set<String> grouped = new HashSet<>(ReconDroneAirframe.LIT_PARTS);
        grouped.addAll(ReconDroneAirframe.LIGHT_PARTS);
        assertEquals(names, grouped);
        assertEquals(ReconDroneAirframe.LIT_PARTS.size() + ReconDroneAirframe.LIGHT_PARTS.size(),
                grouped.size());
    }

    @Test
    void navigationLightsSitOnTheRightWingTips() {
        // Model +X is the port (left) side: red on the left, green on the right.
        ReconDroneAirframe.Box port = ReconDroneAirframe.part(ReconDroneAirframe.NAV_PORT)
                .boxes().get(0);
        ReconDroneAirframe.Box starboard = ReconDroneAirframe.part(
                ReconDroneAirframe.NAV_STARBOARD).boxes().get(0);
        assertEquals(ReconDroneAirframe.Material.LIGHT_RED, port.material());
        assertEquals(ReconDroneAirframe.Material.LIGHT_GREEN, starboard.material());
        assertEquals(48.0F, port.x());
        assertEquals(-48.0F, starboard.x() + starboard.width());
    }

    @Test
    void strobeAndBeaconFlashOutOfPhase() {
        int strobe = 0;
        int beacon = 0;
        int both = 0;
        for (int tick = 0; tick < 240; tick++) {
            boolean strobeOn = ReconDroneAirframe.strobeOn(tick + 0.5F);
            boolean beaconOn = ReconDroneAirframe.beaconOn(tick + 0.5F);
            strobe += strobeOn ? 1 : 0;
            beacon += beaconOn ? 1 : 0;
            both += strobeOn && beaconOn ? 1 : 0;
        }
        assertTrue(strobe > 0 && strobe < 60, "strobe lit " + strobe + " of 240 ticks");
        assertTrue(beacon > 0 && beacon < 60, "beacon lit " + beacon + " of 240 ticks");
        assertEquals(0, both);
    }

    @Test
    void uvFootprintsFitTheTextureAndNeverOverlap() {
        List<ReconDroneAirframe.Box> unique = new ArrayList<>();
        for (ReconDroneAirframe.Part part : ReconDroneAirframe.parts()) {
            for (ReconDroneAirframe.Box box : part.boxes()) {
                assertTrue(box.u() >= 0 && box.v() >= 0, box.toString());
                assertTrue(box.u() + box.footprintWidth() <= ReconDroneAirframe.TEXTURE_WIDTH,
                        box.toString());
                assertTrue(box.v() + box.footprintHeight() <= ReconDroneAirframe.TEXTURE_HEIGHT,
                        box.toString());
                ReconDroneAirframe.Box shared = null;
                for (ReconDroneAirframe.Box other : unique) {
                    if (other.u() == box.u() && other.v() == box.v()) {
                        shared = other;
                    }
                }
                if (shared != null) {
                    // Only identical boxes (the mirrored wing panels and tail surfaces) share.
                    assertEquals(shared.width(), box.width(), box.toString());
                    assertEquals(shared.height(), box.height(), box.toString());
                    assertEquals(shared.depth(), box.depth(), box.toString());
                    continue;
                }
                for (ReconDroneAirframe.Box other : unique) {
                    boolean separate = box.u() + box.footprintWidth() <= other.u()
                            || other.u() + other.footprintWidth() <= box.u()
                            || box.v() + box.footprintHeight() <= other.v()
                            || other.v() + other.footprintHeight() <= box.v();
                    assertTrue(separate, box + " overlaps " + other);
                }
                unique.add(box);
            }
        }
    }

    @Test
    void textureIsPaintedOpaqueOnEveryModelFace() throws IOException {
        BufferedImage image = texture();
        assertEquals(ReconDroneAirframe.TEXTURE_WIDTH, image.getWidth());
        assertEquals(ReconDroneAirframe.TEXTURE_HEIGHT, image.getHeight());
        for (ReconDroneAirframe.Part part : ReconDroneAirframe.parts()) {
            for (ReconDroneAirframe.Box box : part.boxes()) {
                for (int[] face : faces(box)) {
                    for (int x = face[0]; x < face[0] + face[2]; x++) {
                        for (int y = face[1]; y < face[1] + face[3]; y++) {
                            int alpha = image.getRGB(x, y) >>> 24;
                            assertEquals(255, alpha, part.name() + " texel " + x + "," + y);
                        }
                    }
                }
            }
        }
    }

    @Test
    void theAirframeIsGreyAndTheLightsAreColoured() throws IOException {
        BufferedImage image = texture();
        ReconDroneAirframe.Box fuselage = ReconDroneAirframe.part(ReconDroneAirframe.AIRFRAME)
                .boxes().get(0);
        assertEquals(ReconDroneAirframe.Material.FUSELAGE, fuselage.material());
        int[] top = faces(fuselage).get(0);
        int rgb = image.getRGB(top[0] + 1, top[1] + 1);
        int red = (rgb >> 16) & 255;
        int green = (rgb >> 8) & 255;
        int blue = rgb & 255;
        assertTrue(Math.abs(red - green) < 16 && Math.abs(green - blue) < 16,
                "fuselage should be grey: " + Integer.toHexString(rgb));

        int portLight = centre(image, ReconDroneAirframe.part(ReconDroneAirframe.NAV_PORT));
        int starboardLight = centre(image,
                ReconDroneAirframe.part(ReconDroneAirframe.NAV_STARBOARD));
        assertTrue(((portLight >> 16) & 255) > 200 && ((portLight >> 8) & 255) < 100);
        assertTrue(((starboardLight >> 8) & 255) > 180 && ((starboardLight >> 16) & 255) < 100);
    }

    @Test
    void pantsirAirDefenceMayTargetTheDrone() throws IOException {
        JsonObject tag;
        try (InputStream stream = ReconDroneAirframeTest.class.getResourceAsStream(PANTSIR_TAG)) {
            assertNotNull(stream, "missing " + PANTSIR_TAG);
            tag = JsonParser.parseString(new String(stream.readAllBytes(),
                    StandardCharsets.UTF_8)).getAsJsonObject();
        }
        assertFalse(tag.get("replace").getAsBoolean(), "must extend, not replace, the tag");
        JsonArray values = tag.getAsJsonArray("values");
        assertEquals(1, values.size());
        assertEquals("wok_commander_support:recon_drone", values.get(0).getAsString());
    }

    /** Face rectangles {x, y, width, height} in the vanilla box-UV layout, top face first. */
    private static List<int[]> faces(ReconDroneAirframe.Box box) {
        int u = box.u();
        int v = box.v();
        int w = box.width();
        int h = box.height();
        int d = box.depth();
        return List.of(
                new int[]{u + d, v, w, d},
                new int[]{u + d + w, v, w, d},
                new int[]{u, v + d, d, h},
                new int[]{u + d, v + d, w, h},
                new int[]{u + d + w, v + d, d, h},
                new int[]{u + 2 * d + w, v + d, w, h});
    }

    private static int centre(BufferedImage image, ReconDroneAirframe.Part part) {
        int[] front = faces(part.boxes().get(0)).get(3);
        return image.getRGB(front[0], front[1]);
    }

    private static BufferedImage texture() throws IOException {
        try (InputStream stream = ReconDroneAirframeTest.class.getResourceAsStream(TEXTURE)) {
            assertNotNull(stream, "missing " + TEXTURE);
            BufferedImage image = ImageIO.read(stream);
            assertNotNull(image, "unreadable " + TEXTURE);
            return image;
        }
    }
}
