package com.wok.infantry.deployment;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class KitProvenanceAmmoLimitTest {
    @Test
    void issuedWeaponCarriesItsOwnServerAuthoredReserveLimit() {
        CompoundTag provenance = new CompoundTag();
        KitProvenance.writeAmmoReserveLimit(provenance, 210);

        assertTrue(KitProvenance.readAmmoReserveLimit(provenance).isPresent());
        assertEquals(210, KitProvenance.readAmmoReserveLimit(provenance).orElseThrow());
    }
}
