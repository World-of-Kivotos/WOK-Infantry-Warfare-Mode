package com.wok.infantry.client.map;

import com.wok.infantry.battle.TacticalMarkerType;
import com.wok.infantry.client.map.TacticalMapIcons.MapIcon;
import com.wok.infantry.client.map.TacticalMapIcons.Placement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Map marker size scheme B, whole-physical-pixel rounding and the fade thresholds the tactical
 * map uses (0.4.0-beta.3).
 */
class TacticalMapIconsSizingTest {
    @Test
    void symbolScaleIsOneUpToGuiThreeAndGuiOverThreeAbove() {
        assertEquals(1.0D, TacticalMapIcons.symbolScale(1.0D));
        assertEquals(1.0D, TacticalMapIcons.symbolScale(2.0D));
        assertEquals(1.0D, TacticalMapIcons.symbolScale(3.0D));
        assertEquals(4.0D / 3.0D, TacticalMapIcons.symbolScale(4.0D), 1.0E-12);
        assertEquals(2.0D, TacticalMapIcons.symbolScale(6.0D), 1.0E-12);
        assertEquals(1.0D, TacticalMapIcons.symbolScale(Double.NaN));
        assertEquals(1.0D, TacticalMapIcons.symbolScale(0.0D));
    }

    @Test
    void markersKeepOnePhysicalSizeAtGuiOneToThree() {
        for (int step = 0; step <= 20; step++) {
            double knob = 0.75D + step * 0.05D;
            int expected = TacticalMapIcons.physicalPerArt(knob);
            for (int gui = 1; gui <= 3; gui++) {
                assertEquals(expected, TacticalMapIcons.mapArtPx(knob, gui),
                        "knob " + knob + " at GUI " + gui);
            }
        }
        // 1.0: a 30 physical px plate (10 GUI px at GUI 3, 15 at GUI 2, 30 at GUI 1).
        assertEquals(2, TacticalMapIcons.mapArtPx(1.0D, 1.0D));
        assertEquals(2, TacticalMapIcons.mapArtPx(1.0D, 3.0D));
    }

    @Test
    void guiFourGrowsByFourThirdsRoundedToWholePhysicalPixels() {
        // 2 × 4/3 = 2.67 rounds to 3 physical px per art px: a 45 px plate instead of 30.
        assertEquals(3, TacticalMapIcons.mapArtPx(1.0D, 4.0D));
        assertEquals(2, TacticalMapIcons.mapArtPx(0.75D, 4.0D));
        assertEquals(3, TacticalMapIcons.mapArtPx(1.25D, 4.0D));
        assertEquals(4, TacticalMapIcons.mapArtPx(1.5D, 4.0D));
        assertEquals(5, TacticalMapIcons.mapArtPx(1.75D, 4.0D));
        for (int step = 0; step <= 20; step++) {
            double knob = 0.75D + step * 0.05D;
            assertTrue(TacticalMapIcons.mapArtPx(knob, 4.0D)
                            >= TacticalMapIcons.mapArtPx(knob, 3.0D),
                    "GUI 4 never draws a marker smaller than GUI 3 at knob " + knob);
        }
    }

    @Test
    void knobOrdersTheSizesAndNeverDropsBelowOnePixel() {
        int previous = 0;
        for (int step = 0; step <= 20; step++) {
            int artPx = TacticalMapIcons.mapArtPx(0.75D + step * 0.05D, 2.0D);
            assertTrue(artPx >= previous, "a larger knob never shrinks the markers");
            previous = artPx;
        }
        assertEquals(4, previous, "1.75 draws 4 physical px per art px");
        assertEquals(1, TacticalMapIcons.mapArtPx(0.1D, 1.0D));
        assertEquals(2, TacticalMapIcons.mapArtPx(Double.NaN, 2.0D));
    }

    @Test
    void everyPlacementSitsOnWholePhysicalPixelsWithTheScaledPlate() {
        for (int gui = 1; gui <= 4; gui++) {
            for (double knob : new double[]{0.75D, 1.0D, 1.25D, 1.5D, 1.75D}) {
                int artPx = TacticalMapIcons.mapArtPx(knob, gui);
                for (MapIcon icon : MapIcon.values()) {
                    // An anchor between GUI pixels and between physical pixels.
                    Placement placement = TacticalMapIcons.placeGui(icon, 101.37D, 57.61D,
                            artPx, gui);
                    assertEquals(icon.plate().width() * artPx,
                            placement.plateRight() - placement.plateLeft());
                    assertEquals(icon.plate().height() * artPx,
                            placement.plateBottom() - placement.plateTop());
                    assertEquals(Math.round(57.61D * gui), icon.plate() == TacticalMapIcons.Plate.PIN
                            ? placement.plateBottom()
                            : placement.plateTop() + Math.floorDiv(
                                    icon.plate().anchorHalfY() * artPx, 2),
                            icon + " anchor row at GUI " + gui + ", " + artPx + " px");
                }
            }
        }
    }

    @Test
    void placedMarkersFadeForTheirLastThirtySeconds() {
        long created = 1_000_000L;
        long expires = created + 120_000L;
        for (TacticalMarkerType type : TacticalMarkerType.values()) {
            if (type == TacticalMarkerType.RECON_CONTACT) {
                continue;
            }
            assertFalse(TacticalMapIcons.expiring(type, created, expires, expires - 30_001L),
                    type + " 30.001 s left");
            assertTrue(TacticalMapIcons.expiring(type, created, expires, expires - 30_000L),
                    type + " 30 s left");
            assertEquals(1.0F, TacticalMapIcons.alpha(type, created, expires, created));
            assertEquals(TacticalMapIcons.EXPIRING_ALPHA,
                    TacticalMapIcons.alpha(type, created, expires, expires - 1_000L));
        }
    }

    @Test
    void contactsFadeForTheLastQuarterOfTheirLife() {
        // A 12 s drone contact: faded only for its last 3 s, not from the start.
        long created = 5_000L;
        long drone = created + 12_000L;
        assertFalse(TacticalMapIcons.expiring(TacticalMarkerType.RECON_CONTACT, created, drone,
                created));
        assertFalse(TacticalMapIcons.expiring(TacticalMarkerType.RECON_CONTACT, created, drone,
                drone - 3_001L));
        assertTrue(TacticalMapIcons.expiring(TacticalMarkerType.RECON_CONTACT, created, drone,
                drone - 3_000L));
        // A 10 min satellite contact: faded for its last 150 s.
        long satellite = created + 600_000L;
        assertFalse(TacticalMapIcons.expiring(TacticalMarkerType.RECON_CONTACT, created,
                satellite, satellite - 150_001L));
        assertTrue(TacticalMapIcons.expiring(TacticalMarkerType.RECON_CONTACT, created,
                satellite, satellite - 150_000L));
        assertEquals(TacticalMapIcons.EXPIRING_ALPHA, TacticalMapIcons.alpha(
                TacticalMarkerType.RECON_CONTACT, created, drone, drone - 1L));
        // A contact without a life span (created == expires) is faded, not divided by zero.
        assertTrue(TacticalMapIcons.expiring(TacticalMarkerType.RECON_CONTACT, created, created,
                created));
    }
}
