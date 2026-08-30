package com.wok.infantry.ammo;

import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Objects;

/** Stable server-side point costs for TaCZ and SBW ammunition descriptors. */
public final class AmmoPointCostPolicy {
    public static final int INTERMEDIATE_ROUND_COST = 1;
    public static final int FULL_POWER_ROUND_COST = 3;
    public static final int EXPLOSIVE_ROUND_COST = 50;

    private AmmoPointCostPolicy() {
    }

    public static int pointsPerRound(ResourceLocation ammoId) {
        Objects.requireNonNull(ammoId, "ammoId");
        return pointsPerRound(ammoId.toString());
    }

    static int pointsPerRound(String ammoId) {
        String normalized = Objects.requireNonNull(ammoId, "ammoId")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (containsAny(normalized, "rpg", "rocket", "missile", "grenade", "40mm",
                "explosive", "warhead", "launcher", "shell", "bomb", "mortar",
                "apfsds", "heatfs", "atgm", "cannon", "20mm", "25mm", "30mm",
                "35mm", "105mm", "120mm", "122mm", "125mm", "155mm", "agm")) {
            return EXPLOSIVE_ROUND_COST;
        }
        if (containsAny(normalized, "762x54", "54r", "308", "762x51", "3006",
                "338", "50bmg", "792x57", "68x51", "heavyammo", "127mm",
                "145mm")) {
            return FULL_POWER_ROUND_COST;
        }
        return INTERMEDIATE_ROUND_COST;
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
