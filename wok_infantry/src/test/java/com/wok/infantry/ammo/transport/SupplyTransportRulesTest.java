package com.wok.infantry.ammo.transport;

import com.wok.infantry.config.InfantryServerConfig;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplyTransportRulesTest {
    @Test
    void largeCargoUsesTheNativeDragonRiseStationDeployer() {
        assertEquals(ResourceLocation.fromNamespaceAndPath(
                        "dragonrise_reforge", "ammo_supply_station"),
                SupplyCargoType.LARGE.itemId());
    }

    @Test
    void defaultVehicleListKeepsConfirmedCargoTypesAndCapacities() {
        assertEquals(Map.of(
                        ResourceLocation.fromNamespaceAndPath(
                                "superbwarfare", "truck"),
                        new SupplyTransportProfile(SupplyCargoType.LARGE, 3),
                        ResourceLocation.fromNamespaceAndPath(
                                "fcp", "ural"),
                        new SupplyTransportProfile(SupplyCargoType.LARGE, 3),
                        ResourceLocation.fromNamespaceAndPath(
                                "fcp", "hmmwv_unarmored_unarmed"),
                        new SupplyTransportProfile(SupplyCargoType.MEDIUM, 2),
                        ResourceLocation.fromNamespaceAndPath(
                                "fcp", "hmmwv_armored_unarmed"),
                        new SupplyTransportProfile(SupplyCargoType.MEDIUM, 1)),
                SupplyTransportRules.parse(
                        InfantryServerConfig.DEFAULT_SUPPLY_TRANSPORT_VEHICLES));
    }

    @Test
    void parsesVehicleProfilesAndUsesTheLastDuplicateEntry() {
        Map<ResourceLocation, SupplyTransportProfile> profiles =
                SupplyTransportRules.parse(List.of(
                        "superbwarfare:truck=large=3",
                        " addon:light_truck = medium = 2 ",
                        "superbwarfare:truck=medium=6"));

        assertEquals(2, profiles.size());
        assertEquals(new SupplyTransportProfile(SupplyCargoType.MEDIUM, 6),
                profiles.get(ResourceLocation.fromNamespaceAndPath(
                        "superbwarfare", "truck")));
        assertEquals(new SupplyTransportProfile(SupplyCargoType.MEDIUM, 2),
                profiles.get(ResourceLocation.fromNamespaceAndPath(
                        "addon", "light_truck")));
    }

    @Test
    void acceptsLegacyTwoPartEntriesAsLargeCargo() {
        assertEquals(Map.of(
                        ResourceLocation.fromNamespaceAndPath(
                                "legacy", "supply_truck"),
                        new SupplyTransportProfile(SupplyCargoType.LARGE, 4)),
                SupplyTransportRules.parse(List.of("legacy:supply_truck=4")));
    }

    @Test
    void rejectsMalformedIdsAndUnsafeCapacities() {
        assertFalse(SupplyTransportRules.isValidEntry("not an id=4"));
        assertFalse(SupplyTransportRules.isValidEntry(
                "superbwarfare:truck=medium=0"));
        assertFalse(SupplyTransportRules.isValidEntry(
                "superbwarfare:truck=medium=65"));
        assertFalse(SupplyTransportRules.isValidEntry(
                "superbwarfare:truck=small=2"));
        assertFalse(SupplyTransportRules.isValidEntry(
                "superbwarfare:truck=medium=many"));
        assertTrue(SupplyTransportRules.isValidEntry(
                "superbwarfare:truck=large=64"));

        assertEquals(Map.of(), SupplyTransportRules.parse(List.of(
                "not an id=4", "superbwarfare:truck=medium=0")));
    }
}
