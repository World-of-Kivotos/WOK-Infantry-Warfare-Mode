package com.wok.infantry.deployment;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class KitProvenanceAmmoLimitTest {
    @Test
    void issuedWeaponCarriesItsOwnServerAuthoredReserveLimit() {
        CompoundTag provenance = new CompoundTag();
        KitProvenance.writeAmmoReserveLimit(provenance, 210);

        assertTrue(KitProvenance.readAmmoReserveLimit(provenance).isPresent());
        assertEquals(210, KitProvenance.readAmmoReserveLimit(provenance).orElseThrow());
    }

    @Test
    void transportCargoCanMoveSlotsWithoutMakingOrdinaryKitMovable() {
        CompoundTag ordinary = new CompoundTag();
        ordinary.putInt("InventorySlot", 7);
        assertTrue(KitProvenance.inventorySlotMatches(ordinary, 7));
        assertFalse(KitProvenance.inventorySlotMatches(ordinary, 8));

        CompoundTag cargo = new CompoundTag();
        cargo.putInt("InventorySlot", 0);
        KitProvenance.markTransportCargo(cargo);
        assertTrue(KitProvenance.inventorySlotMatches(cargo, 0));
        assertTrue(KitProvenance.inventorySlotMatches(cargo, 8));
        assertTrue(KitProvenance.inventorySlotMatches(cargo, 40));
    }
}
