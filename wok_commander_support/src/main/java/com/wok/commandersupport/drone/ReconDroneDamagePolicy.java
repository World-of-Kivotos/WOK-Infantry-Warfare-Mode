package com.wok.commandersupport.drone;

import com.wok.infantry.battle.Faction;
import net.minecraft.resources.ResourceLocation;

/**
 * Which hits can bring the recon drone down. Only blasts (anything in
 * {@code #minecraft:is_explosion}, which Superb Warfare extends with its own explosion types)
 * and Superb Warfare's direct projectile hit (missiles and anti-air shells) count. Bullets,
 * melee, falling and every other damage type are ignored, as is fire from a player of the
 * drone's own faction. A drone that is already going down takes no further damage.
 *
 * <p>The Superb Warfare damage type is matched by registry id only, so the module does not
 * depend on that mod.</p>
 */
public final class ReconDroneDamagePolicy {
    public static final ResourceLocation SBW_PROJECTILE_HIT =
            ResourceLocation.fromNamespaceAndPath("superbwarfare", "projectile_hit");

    private ReconDroneDamagePolicy() {
    }

    /**
     * @param explosion       the damage type is tagged {@code #minecraft:is_explosion}
     * @param damageType      registry id of the damage type, or null when unknown
     * @param amount          damage amount
     * @param crashing        the drone has already been shot down
     * @param droneFaction    faction that called the drone, or null when unknown
     * @param attackerFaction faction of the attacking player, or null for non-players and
     *                        unassigned players
     */
    public static boolean accepts(boolean explosion, ResourceLocation damageType, float amount,
                                  boolean crashing, Faction droneFaction,
                                  Faction attackerFaction) {
        if (crashing || !Float.isFinite(amount) || amount <= 0.0F) {
            return false;
        }
        if (!explosion && !SBW_PROJECTILE_HIT.equals(damageType)) {
            return false;
        }
        return droneFaction == null || attackerFaction != droneFaction;
    }
}
