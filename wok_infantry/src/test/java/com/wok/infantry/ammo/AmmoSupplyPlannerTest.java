package com.wok.infantry.ammo;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AmmoSupplyPlannerTest {
    @Test
    void sharedAmmoTypeUsesOneCapInsteadOfMultiplyingPerWeapon() {
        Map<String, Integer> result = AmmoSupplyPlanner.deficits(
                List.of("tacz:556x45", "tacz:556x45"), ignored -> 35, 180);

        assertEquals(Map.of("tacz:556x45", 145), result);
    }

    @Test
    void eachUniqueTypeReceivesItsOwnReserveCap() {
        Map<String, Integer> current = Map.of("tacz:556x45", 120, "tacz:9mm", 12);

        Map<String, Integer> result = AmmoSupplyPlanner.deficits(
                List.of("tacz:556x45", "tacz:9mm"), current::get, 180);

        assertEquals(Map.of("tacz:556x45", 60, "tacz:9mm", 168), result);
    }

    @Test
    void ammunitionAboveTheCapIsNeverRemoved() {
        Map<String, Integer> result = AmmoSupplyPlanner.deficits(
                List.of("tacz:762x51"), ignored -> 240, 180);

        assertEquals(Map.of("tacz:762x51", 0), result);
    }

    @Test
    void rejectsNonPositiveLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> AmmoSupplyPlanner.deficits(List.of("tacz:9mm"), ignored -> 0, 0));
    }

    @Test
    void independentPerTypeLimitsProduceIndependentInitialDeficits() {
        Map<String, Integer> result = AmmoSupplyPlanner.deficitsByType(
                Map.of("tacz:556x45", 180, "tacz:9mm", 54),
                ammo -> ammo.endsWith("9mm") ? 17 : 30);

        assertEquals(150, result.get("tacz:556x45"));
        assertEquals(37, result.get("tacz:9mm"));
    }

    @Test
    void perTypeDeficitDoesNotRemoveRoundsAboveTheConfiguredLimit() {
        Map<String, Integer> result = AmmoSupplyPlanner.deficitsByType(
                Map.of("tacz:556x45", 180), ignored -> 240);

        assertEquals(0, result.get("tacz:556x45"));
    }

    @Test
    void perTypeDeficitRejectsInvalidLimits() {
        assertThrows(IllegalArgumentException.class,
                () -> AmmoSupplyPlanner.deficitsByType(Map.of("tacz:9mm", 0),
                        ignored -> 0));
    }
}
