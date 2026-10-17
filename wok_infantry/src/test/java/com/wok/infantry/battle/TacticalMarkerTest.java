package com.wok.infantry.battle;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TacticalMarkerTest {
    private static final ResourceLocation OVERWORLD =
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");

    @Test
    void attackDirectionRetainsExactEndpointAndUsesMapBearingConvention() {
        TacticalMarker marker = marker(TacticalMarkerType.ATTACK_DIRECTION,
                10.0D, 20.0D, 110.0D, 20.0D);

        assertEquals(110.0D, marker.endX());
        assertEquals(20.0D, marker.endZ());
        assertEquals(100.0D, marker.lengthBlocks(), 1.0E-9D);
        assertEquals(270.0F, marker.directionDegrees(), 1.0E-4F);
    }

    @Test
    void attackDirectionAcceptsInclusiveLengthLimitsAndRejectsOutsideThem() {
        assertTrue(TacticalMarker.isValidAttackGeometry(0.0D, 0.0D,
                BattleRules.MIN_ATTACK_DIRECTION_LENGTH_BLOCKS, 0.0D));
        assertTrue(TacticalMarker.isValidAttackGeometry(0.0D, 0.0D,
                BattleRules.MAX_ATTACK_DIRECTION_LENGTH_BLOCKS, 0.0D));
        assertThrows(IllegalArgumentException.class, () -> marker(
                TacticalMarkerType.ATTACK_DIRECTION, 0.0D, 0.0D,
                BattleRules.MIN_ATTACK_DIRECTION_LENGTH_BLOCKS - 0.001D, 0.0D));
        assertThrows(IllegalArgumentException.class, () -> marker(
                TacticalMarkerType.ATTACK_DIRECTION, 0.0D, 0.0D,
                BattleRules.MAX_ATTACK_DIRECTION_LENGTH_BLOCKS + 0.001D, 0.0D));
    }

    @Test
    void pointMarkerDiscardsSecondaryGeometry() {
        TacticalMarker marker = marker(TacticalMarkerType.TANK,
                12.5D, -8.25D, 9_999.0D, -9_999.0D);

        assertEquals(marker.x(), marker.endX());
        assertEquals(marker.z(), marker.endZ());
        assertEquals(0.0D, marker.lengthBlocks());
        assertEquals(0.0F, marker.directionDegrees());
    }

    @Test
    void legacyAngleConstructorMigratesToFixedLengthEndpoint() {
        TacticalMarker marker = new TacticalMarker(UUID.randomUUID(), Faction.BLUE,
                TacticalMarkerType.ATTACK_DIRECTION, OVERWORLD,
                100.0D, 64.0D, 200.0D, 90.0F,
                UUID.randomUUID(), SquadCallsign.ALPHA, 1_000L, 2_000L);

        assertEquals(BattleRules.LEGACY_ATTACK_DIRECTION_LENGTH_BLOCKS,
                marker.lengthBlocks(), 1.0E-8D);
        assertEquals(90.0F, marker.directionDegrees(), 1.0E-4F);
        assertEquals(100.0D - BattleRules.LEGACY_ATTACK_DIRECTION_LENGTH_BLOCKS,
                marker.endX(), 1.0E-8D);
    }

    @Test
    void rejectsNonFiniteCoordinatesAndInvalidLifetime() {
        assertThrows(IllegalArgumentException.class, () -> marker(
                TacticalMarkerType.INFANTRY, Double.NaN, 0.0D, 0.0D, 0.0D));
        assertThrows(IllegalArgumentException.class, () -> new TacticalMarker(
                UUID.randomUUID(), Faction.RED, TacticalMarkerType.TANK, OVERWORLD,
                0.0D, 64.0D, 0.0D, 0.0D, 0.0D,
                UUID.randomUUID(), SquadCallsign.BRAVO, 2_000L, 1_999L));
    }

    private static TacticalMarker marker(TacticalMarkerType type, double x, double z,
                                         double endX, double endZ) {
        return new TacticalMarker(UUID.randomUUID(), Faction.BLUE, type, OVERWORLD,
                x, 64.0D, z, endX, endZ, UUID.randomUUID(), SquadCallsign.ALPHA,
                1_000L, 2_000L);
    }
}
