package com.wok.infantry.loadout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LoadoutConfigAmmoCrateTest {
    @Test
    void defaultAssaultRoleOffersOnePortableCrate() {
        LoadoutConfigData data = LoadoutConfigData.defaultConfig();
        assertEquals(1, portableCrateCount(data));
    }

    @Test
    void normalizationMigratesExistingConfigWithoutDuplicates() {
        LoadoutConfigData data = LoadoutConfigData.defaultConfig();
        data.findClass("assault").orElseThrow().entries(LoadoutSlot.GADGET_TWO).clear();
        data.normalize();
        data.normalize();
        assertEquals(1, portableCrateCount(data));
    }

    @Test
    void normalizationDoesNotRecreateADeletedAssaultProfession() {
        LoadoutConfigData data = LoadoutConfigData.defaultConfig();
        data.classes().removeIf(definition -> "assault".equals(definition.id()));

        data.normalize();

        assertTrue(data.findClass("assault").isEmpty());
        assertEquals("support", data.classes().get(0).id());
    }

    private static long portableCrateCount(LoadoutConfigData data) {
        return data.findClass("assault").orElseThrow().entries(LoadoutSlot.GADGET_TWO)
                .stream().filter(entry -> "wok_infantry:ammo_supply_crate"
                        .equals(entry.itemId())).count();
    }
}
