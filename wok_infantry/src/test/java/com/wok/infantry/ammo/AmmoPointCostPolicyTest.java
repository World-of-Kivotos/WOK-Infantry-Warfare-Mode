package com.wok.infantry.ammo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AmmoPointCostPolicyTest {
    @Test
    void intermediateRoundsCostOnePoint() {
        assertEquals(1, AmmoPointCostPolicy.pointsPerRound("pack:556x45"));
        assertEquals(1, AmmoPointCostPolicy.pointsPerRound("pack:545x39"));
        assertEquals(1, AmmoPointCostPolicy.pointsPerRound("pack:762x39"));
    }

    @Test
    void fullPowerRoundsCostThreePoints() {
        assertEquals(3, AmmoPointCostPolicy.pointsPerRound("pack:762x54r"));
        assertEquals(3, AmmoPointCostPolicy.pointsPerRound("pack:308_winchester"));
        assertEquals(3, AmmoPointCostPolicy.pointsPerRound("pack:762x51"));
        assertEquals(3, AmmoPointCostPolicy.pointsPerRound("vvp:item_12_7mm:@HeavyAmmo"));
    }

    @Test
    void explosiveRoundsCostFiftyPoints() {
        assertEquals(50, AmmoPointCostPolicy.pointsPerRound("pack:rpg_rocket"));
        assertEquals(50, AmmoPointCostPolicy.pointsPerRound("pack:40mm_grenade"));
        assertEquals(50, AmmoPointCostPolicy.pointsPerRound("superbwarfare:large_shell_he"));
        assertEquals(50, AmmoPointCostPolicy.pointsPerRound("vvp:shell_122mm"));
    }

    @Test
    void explosiveClassificationTakesPrecedence() {
        assertEquals(50, AmmoPointCostPolicy.pointsPerRound("pack:762x51_launcher_grenade"));
    }
}
