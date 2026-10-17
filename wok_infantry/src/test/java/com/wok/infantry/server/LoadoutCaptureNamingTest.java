package com.wok.infantry.server;

import com.wok.infantry.loadout.LoadoutEntry;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class LoadoutCaptureNamingTest {
    @Test
    void taczGunIdBecomesStableEntryId() {
        CompoundTag tag = new CompoundTag();
        tag.putString("GunId", "tacz:m4a1/millennium-kit");

        assertEquals("m4a1_millennium-kit",
                LoadoutService.captureIdBase("modern_kinetic_gun", tag));
    }

    @Test
    void duplicateCapturedIdsReceiveDeterministicSuffix() {
        List<LoadoutEntry> existing = List.of(
                entry("m4a1"), entry("m4a1_2"), entry("other"));

        assertEquals("m4a1_3",
                LoadoutService.uniqueCapturedEntryId("m4a1", existing, ""));
        assertEquals("m4a1",
                LoadoutService.uniqueCapturedEntryId("m4a1", existing, "m4a1"));
    }

    private static LoadoutEntry entry(String id) {
        return new LoadoutEntry(id, id, "minecraft:stone", 1, "");
    }
}
