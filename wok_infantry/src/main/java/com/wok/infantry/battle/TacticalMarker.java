package com.wok.infantry.battle;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.UUID;

public record TacticalMarker(
        UUID id,
        Faction faction,
        TacticalMarkerType type,
        ResourceLocation dimension,
        double x,
        double y,
        double z,
        double endX,
        double endZ,
        UUID creatorId,
        SquadCallsign creatorSquad,
        long createdAtMillis,
        long expiresAtMillis
) {
    public TacticalMarker {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(faction, "faction");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(creatorId, "creatorId");
        requireCoordinate(x, "x");
        requireCoordinate(y, "y");
        requireCoordinate(z, "z");
        if (createdAtMillis < 0L || expiresAtMillis < createdAtMillis) {
            throw new IllegalArgumentException("Invalid tactical marker lifetime");
        }
        if (type == TacticalMarkerType.ATTACK_DIRECTION) {
            if (!isValidAttackGeometry(x, z, endX, endZ)) {
                throw new IllegalArgumentException("Invalid attack-direction geometry");
            }
        } else {
            // Point markers never retain client-supplied secondary geometry.
            endX = x;
            endZ = z;
        }
    }

    /** Compatibility constructor for version-1 data/callers that only supplied an angle. */
    public TacticalMarker(UUID id, Faction faction, TacticalMarkerType type,
                          ResourceLocation dimension, double x, double y, double z,
                          float directionDegrees, UUID creatorId, SquadCallsign creatorSquad,
                          long createdAtMillis, long expiresAtMillis) {
        this(id, faction, type, dimension, x, y, z,
                legacyEndX(type, x, directionDegrees),
                legacyEndZ(type, z, directionDegrees),
                creatorId, creatorSquad, createdAtMillis, expiresAtMillis);
    }

    public boolean expiredAt(long nowMillis) {
        return expiresAtMillis <= nowMillis;
    }

    public float directionDegrees() {
        if (type != TacticalMarkerType.ATTACK_DIRECTION) {
            return 0.0F;
        }
        return directionDegrees(x, z, endX, endZ);
    }

    public double lengthBlocks() {
        return type == TacticalMarkerType.ATTACK_DIRECTION
                ? Math.hypot(endX - x, endZ - z) : 0.0D;
    }

    public static boolean isValidAttackGeometry(double startX, double startZ,
                                                double endX, double endZ) {
        if (!validCoordinate(startX) || !validCoordinate(startZ)
                || !validCoordinate(endX) || !validCoordinate(endZ)) {
            return false;
        }
        double length = Math.hypot(endX - startX, endZ - startZ);
        return Double.isFinite(length)
                && length >= BattleRules.MIN_ATTACK_DIRECTION_LENGTH_BLOCKS
                && length <= BattleRules.MAX_ATTACK_DIRECTION_LENGTH_BLOCKS;
    }

    private static float directionDegrees(double startX, double startZ,
                                          double endX, double endZ) {
        float degrees = (float) Math.toDegrees(Math.atan2(-(endX - startX), endZ - startZ));
        return degrees < 0.0F ? degrees + 360.0F : degrees;
    }

    private static double legacyEndX(TacticalMarkerType type, double startX,
                                     float directionDegrees) {
        if (type != TacticalMarkerType.ATTACK_DIRECTION || !Float.isFinite(directionDegrees)) {
            return startX;
        }
        return startX - Math.sin(Math.toRadians(directionDegrees))
                * BattleRules.LEGACY_ATTACK_DIRECTION_LENGTH_BLOCKS;
    }

    private static double legacyEndZ(TacticalMarkerType type, double startZ,
                                     float directionDegrees) {
        if (type != TacticalMarkerType.ATTACK_DIRECTION || !Float.isFinite(directionDegrees)) {
            return startZ;
        }
        return startZ + Math.cos(Math.toRadians(directionDegrees))
                * BattleRules.LEGACY_ATTACK_DIRECTION_LENGTH_BLOCKS;
    }

    private static void requireCoordinate(double value, String field) {
        if (!validCoordinate(value)) {
            throw new IllegalArgumentException("Invalid tactical marker " + field + ": " + value);
        }
    }

    private static boolean validCoordinate(double value) {
        return Double.isFinite(value) && Math.abs(value) <= BattleRules.MAX_COORDINATE;
    }
}
