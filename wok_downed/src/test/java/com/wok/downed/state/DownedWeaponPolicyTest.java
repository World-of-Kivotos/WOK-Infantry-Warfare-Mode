package com.wok.downed.state;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownedWeaponPolicyTest {
    private static final List<String> ALLOWED = List.of("tacz:glock_17", "tacz:m1911");

    @Test
    void listedSidearmsAreAllowed() {
        assertTrue(DownedWeaponPolicy.isAllowedSidearmId("tacz:glock_17", ALLOWED));
        assertTrue(DownedWeaponPolicy.isAllowedSidearmId("tacz:m1911", ALLOWED));
    }

    @Test
    void everythingElseIsBlocked() {
        assertFalse(DownedWeaponPolicy.isAllowedSidearmId("tacz:ak47", ALLOWED));
        assertFalse(DownedWeaponPolicy.isAllowedSidearmId("tacz:glock_18", ALLOWED));
        assertFalse(DownedWeaponPolicy.isAllowedSidearmId("minecraft:stone", ALLOWED));
        assertFalse(DownedWeaponPolicy.isAllowedSidearmId("", ALLOWED));
        assertFalse(DownedWeaponPolicy.isAllowedSidearmId(null, ALLOWED));
    }

    @Test
    void matchingIgnoresCaseAndSurroundingWhitespace() {
        // A hand-edited config must still work.
        assertTrue(DownedWeaponPolicy.isAllowedSidearmId("TACZ:Glock_17", ALLOWED));
        assertTrue(DownedWeaponPolicy.isAllowedSidearmId("  tacz:m1911  ", ALLOWED));
        assertTrue(DownedWeaponPolicy.isAllowedSidearmId(
                "tacz:glock_17", List.of(" TACZ:GLOCK_17 ")));
    }

    @Test
    void emptyOrNullAllowListBlocksEverything() {
        assertFalse(DownedWeaponPolicy.isAllowedSidearmId("tacz:glock_17", List.of()));
        assertFalse(DownedWeaponPolicy.isAllowedSidearmId("tacz:glock_17", null));
    }

    @Test
    void nullEntriesInTheListAreIgnored() {
        java.util.List<String> withNull = new java.util.ArrayList<>();
        withNull.add(null);
        withNull.add("tacz:m1911");
        assertTrue(DownedWeaponPolicy.isAllowedSidearmId("tacz:m1911", withNull));
        assertFalse(DownedWeaponPolicy.isAllowedSidearmId("tacz:glock_17", withNull));
    }

    @Test
    void defaultListCoversEveryPistolOfTheShippedGunPack() {
        // These are the pistol ids registered by TaCZ 1.1.8's default pack.
        List<String> pistols = List.of("tacz:glock_17", "tacz:b93r", "tacz:cz75", "tacz:deagle",
                "tacz:deagle_golden", "tacz:hk_mk23", "tacz:m1911", "tacz:m9a4", "tacz:p320",
                "tacz:rhino357", "tacz:taurus500", "tacz:taurus943", "tacz:timeless50",
                "tacz:uzi");
        for (String pistol : pistols) {
            assertTrue(DownedWeaponPolicy.isAllowedSidearmId(
                            pistol, DownedWeaponPolicy.DEFAULT_SIDEARMS),
                    pistol + " should be fireable while downed");
        }
    }

    @Test
    void defaultListDoesNotLeakLongGuns() {
        for (String longGun : List.of("tacz:ak47", "tacz:m4a1", "tacz:kar98", "tacz:rpg7",
                "tacz:minigun", "tacz:m249", "tacz:fn_fal")) {
            assertFalse(DownedWeaponPolicy.isAllowedSidearmId(
                            longGun, DownedWeaponPolicy.DEFAULT_SIDEARMS),
                    longGun + " must stay blocked while downed");
        }
    }
}
