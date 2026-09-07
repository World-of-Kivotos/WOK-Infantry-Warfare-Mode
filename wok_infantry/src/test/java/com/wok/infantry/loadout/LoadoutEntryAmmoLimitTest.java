package com.wok.infantry.loadout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LoadoutEntryAmmoLimitTest {
    @Test
    void eachWeaponEntryOwnsItsIndependentReserveLimit() {
        LoadoutEntry rifle = new LoadoutEntry("rifle", "步枪", "minecraft:stick",
                1, "", 210);
        LoadoutEntry pistol = new LoadoutEntry("pistol", "手枪", "minecraft:stick",
                1, "", 48);

        assertEquals(210, rifle.ammoReserveLimit());
        assertEquals(48, pistol.ammoReserveLimit());
        assertEquals(210, rifle.copy().ammoReserveLimit());
    }

    @Test
    void legacyEntryDefaultsToOneHundredEightyAndInvalidInputIsVisibleToValidation() {
        LoadoutEntry legacy = new LoadoutEntry("legacy", "旧枪", "minecraft:stick", 1, "");
        LoadoutEntry invalid = new LoadoutEntry("invalid", "错误", "minecraft:stick",
                1, "", 4_097);

        assertEquals(LoadoutEntry.DEFAULT_AMMO_RESERVE_LIMIT, legacy.ammoReserveLimit());
        assertTrue(legacy.hasValidAmmoReserveLimit());
        assertFalse(invalid.hasValidAmmoReserveLimit());
        assertEquals(4_097, invalid.configuredAmmoReserveLimit());
    }
}
