package com.wok.infantry.support;

import com.wok.infantry.battle.Faction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupportSavedDataTest {
    private static final ResourceLocation FUTURE =
            ResourceLocation.fromNamespaceAndPath("future_mod", "orbital_scan");
    private static final ResourceLocation OTHER =
            ResourceLocation.fromNamespaceAndPath("other_mod", "supply_drop");

    @Test
    void dynamicCooldownsRoundTripWithoutARegisteredDefinition() {
        SupportSavedData data = new SupportSavedData();
        assertTrue(data.setReadyAt(Faction.BLUE, FUTURE, 1_000L));
        assertTrue(data.setReadyAt(Faction.RED, FUTURE, 2_000L));
        assertTrue(data.setReadyAt(Faction.BLUE, OTHER, 3_000L));
        long blueRevision = data.structuralRevision(Faction.BLUE);
        long redRevision = data.structuralRevision(Faction.RED);

        SupportSavedData loaded = SupportSavedData.load(data.save(new CompoundTag()).copy());

        assertEquals(1_000L, loaded.readyAt(Faction.BLUE, FUTURE));
        assertEquals(2_000L, loaded.readyAt(Faction.RED, FUTURE));
        assertEquals(3_000L, loaded.readyAt(Faction.BLUE, OTHER));
        assertEquals(0L, loaded.readyAt(Faction.RED, OTHER));
        assertEquals(blueRevision, loaded.structuralRevision(Faction.BLUE));
        assertEquals(redRevision, loaded.structuralRevision(Faction.RED));
        assertEquals(data.save(new CompoundTag()), loaded.save(new CompoundTag()));
    }

    @Test
    void duplicateEntriesKeepLongestCooldownAndMalformedEntriesAreIgnored() {
        CompoundTag root = new CompoundTag();
        root.putInt("Version", SupportSavedData.DATA_VERSION);
        ListTag cooldowns = new ListTag();
        cooldowns.add(cooldown("blue", FUTURE.toString(), 80L));
        cooldowns.add(cooldown("blue", FUTURE.toString(), 120L));
        cooldowns.add(cooldown("blue", FUTURE.toString(), 90L));
        cooldowns.add(cooldown("unknown", FUTURE.toString(), 5_000L));
        cooldowns.add(cooldown("red", "not valid:???", 5_000L));
        cooldowns.add(cooldown("red", FUTURE.toString(), 0L));
        cooldowns.add(cooldown("red", OTHER.toString(), -1L));
        root.put("Cooldowns", cooldowns);

        SupportSavedData loaded = SupportSavedData.load(root);

        assertEquals(120L, loaded.readyAt(Faction.BLUE, FUTURE));
        assertTrue(loaded.cooldowns(Faction.RED).isEmpty());
        assertEquals(1, loaded.save(new CompoundTag())
                .getList("Cooldowns", Tag.TAG_COMPOUND).size());
    }

    @Test
    void versionOnePrototypeIdsAreDiscardedRatherThanMisnamespaced() {
        CompoundTag root = new CompoundTag();
        root.putInt("Version", 1);
        ListTag cooldowns = new ListTag();
        CompoundTag legacy = new CompoundTag();
        legacy.putString("Faction", "blue");
        legacy.putString("Type", "prototype_skill");
        legacy.putLong("ReadyAt", 500L);
        cooldowns.add(legacy);
        root.put("Cooldowns", cooldowns);

        assertTrue(SupportSavedData.load(root).cooldowns(Faction.BLUE).isEmpty());
    }

    @Test
    void mutationsAreIdempotentAndResetDoesNotCrossContaminateKeys() {
        SupportSavedData data = new SupportSavedData();
        SupportSavedData.CooldownKey blue = new SupportSavedData.CooldownKey(
                Faction.BLUE, FUTURE);
        SupportSavedData.CooldownKey red = new SupportSavedData.CooldownKey(
                Faction.RED, FUTURE);
        assertNotEquals(blue, red);
        assertThrows(NullPointerException.class,
                () -> new SupportSavedData.CooldownKey(null, FUTURE));

        assertTrue(data.setReadyAt(blue.faction(), blue.supportId(), 400L));
        long revision = data.structuralRevision(Faction.BLUE);
        assertFalse(data.setReadyAt(blue.faction(), blue.supportId(), 400L));
        assertEquals(revision, data.structuralRevision(Faction.BLUE));
        assertFalse(data.setReadyAt(Faction.BLUE, OTHER, -1L));
        assertTrue(data.setReadyAt(Faction.RED, OTHER, 500L));
        assertTrue(data.setReadyAt(Faction.RED, OTHER, 0L));
        assertEquals(0L, data.readyAt(Faction.RED, OTHER));

        data.resetAll();
        assertTrue(data.cooldowns(Faction.BLUE).isEmpty());
        assertTrue(data.cooldowns(Faction.RED).isEmpty());
        assertTrue(data.structuralRevision(Faction.BLUE) > revision);
    }

    @Test
    void persistenceRevisionIsIndependentForEachFaction() {
        SupportSavedData data = new SupportSavedData();
        long redBefore = data.structuralRevision(Faction.RED);

        assertTrue(data.setReadyAt(Faction.BLUE, FUTURE, 100L));

        assertTrue(data.structuralRevision(Faction.BLUE) > 0L);
        assertEquals(redBefore, data.structuralRevision(Faction.RED));
    }

    @Test
    void pruningReclaimsOnlyExpiredEntriesIncludingUnknownIds() {
        SupportSavedData data = new SupportSavedData();
        assertTrue(data.setReadyAt(Faction.BLUE, FUTURE, 100L));
        assertTrue(data.setReadyAt(Faction.RED, FUTURE, 101L));
        assertTrue(data.setReadyAt(Faction.BLUE, OTHER, 50L));

        assertEquals(2, data.pruneExpired(100L));
        assertTrue(data.cooldowns(Faction.BLUE).isEmpty());
        assertEquals(101L, data.readyAt(Faction.RED, FUTURE));
        assertEquals(0, data.pruneExpired(100L));
    }

    @Test
    void capacityFailureIsRecoverableAfterExpiredEntriesArePruned() {
        SupportSavedData data = new SupportSavedData();
        for (int index = 0; index < SupportSavedData.MAX_COOLDOWN_ENTRIES; index++) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    "capacity_test", "support_" + index);
            assertTrue(data.setReadyAt(Faction.BLUE, id, 100L));
        }
        assertThrows(IllegalStateException.class,
                () -> data.setReadyAt(Faction.RED, FUTURE, 200L));

        assertEquals(SupportSavedData.MAX_COOLDOWN_ENTRIES, data.pruneExpired(100L));
        assertTrue(data.setReadyAt(Faction.RED, FUTURE, 200L));
    }

    private static CompoundTag cooldown(String faction, String supportId, long readyAt) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Faction", faction);
        tag.putString("SupportId", supportId);
        tag.putLong("ReadyAt", readyAt);
        return tag;
    }
}
