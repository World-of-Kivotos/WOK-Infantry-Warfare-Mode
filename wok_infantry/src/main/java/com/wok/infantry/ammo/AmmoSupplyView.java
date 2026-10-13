package com.wok.infantry.ammo;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Objects;

/** Server-authored UI snapshot; every selection is revalidated against live state. */
public record AmmoSupplyView(Target target, int capacityPoints, int remainingPoints,
                             List<GunOption> guns,
                             List<VehicleAmmoOption> vehicleAmmunition) {
    public AmmoSupplyView {
        Objects.requireNonNull(target, "target");
        capacityPoints = Math.max(0, capacityPoints);
        remainingPoints = Math.max(0, Math.min(capacityPoints, remainingPoints));
        guns = List.copyOf(guns == null ? List.of() : guns);
        vehicleAmmunition = List.copyOf(vehicleAmmunition == null
                ? List.of() : vehicleAmmunition);
    }

    /** Append-only wire order: existing small/legacy-large/medium ordinals remain stable. */
    public enum TargetKind { SMALL_CRATE, LARGE_STATION, MEDIUM_CRATE, LARGE_BLOCK }

    public record Target(TargetKind kind, long value) {
        public Target {
            Objects.requireNonNull(kind, "kind");
        }

        public static Target smallCrate(BlockPos pos) {
            return new Target(TargetKind.SMALL_CRATE, Objects.requireNonNull(pos).asLong());
        }

        public static Target largeStation(int entityId) {
            return new Target(TargetKind.LARGE_STATION, entityId);
        }

        public static Target mediumCrate(BlockPos pos) {
            return new Target(TargetKind.MEDIUM_CRATE, Objects.requireNonNull(pos).asLong());
        }

        public static Target largeBlock(BlockPos pos) {
            return new Target(TargetKind.LARGE_BLOCK, Objects.requireNonNull(pos).asLong());
        }

        public boolean isLarge() {
            return kind == TargetKind.LARGE_STATION || kind == TargetKind.LARGE_BLOCK;
        }

        public BlockPos blockPos() {
            if (kind != TargetKind.SMALL_CRATE && kind != TargetKind.MEDIUM_CRATE
                    && kind != TargetKind.LARGE_BLOCK) {
                throw new IllegalStateException("Target is not a block");
            }
            return BlockPos.of(value);
        }

        public int entityId() {
            if (kind != TargetKind.LARGE_STATION || value < Integer.MIN_VALUE
                    || value > Integer.MAX_VALUE) {
                throw new IllegalStateException("Target is not a valid entity id");
            }
            return (int) value;
        }
    }

    public record GunOption(int inventorySlot, Component gunName, Component ammunitionName,
                            String ammunitionId, int pointsPerRound, int currentRounds,
                            int reserveLimit, int receivableRounds) {
        public GunOption {
            if ((inventorySlot < 0 || inventorySlot > 35) && inventorySlot != 40) {
                throw new IllegalArgumentException("Invalid gun inventory slot");
            }
            Objects.requireNonNull(gunName, "gunName");
            Objects.requireNonNull(ammunitionName, "ammunitionName");
            ammunitionId = Objects.requireNonNull(ammunitionId, "ammunitionId");
            pointsPerRound = Math.max(1, pointsPerRound);
            currentRounds = Math.max(0, currentRounds);
            reserveLimit = Math.max(1, reserveLimit);
            receivableRounds = Math.max(0, receivableRounds);
        }
    }

    public record VehicleAmmoOption(int vehicleEntityId, String weaponKey,
                                    int consumerIndex, Component vehicleName,
                                    Component weaponName, Component ammunitionName,
                                    int pointsPerRound, int currentRounds,
                                    int roundStep, int maxRounds) {
        public VehicleAmmoOption {
            if (vehicleEntityId < 0 || weaponKey == null || weaponKey.isBlank()
                    || weaponKey.length() > 128 || consumerIndex < 0
                    || consumerIndex > 255) {
                throw new IllegalArgumentException("Invalid vehicle ammunition selector");
            }
            Objects.requireNonNull(vehicleName, "vehicleName");
            Objects.requireNonNull(weaponName, "weaponName");
            Objects.requireNonNull(ammunitionName, "ammunitionName");
            pointsPerRound = Math.max(1, pointsPerRound);
            currentRounds = Math.max(0, currentRounds);
            roundStep = Math.max(1, roundStep);
            maxRounds = Math.max(0, maxRounds - maxRounds % roundStep);
        }
    }
}
