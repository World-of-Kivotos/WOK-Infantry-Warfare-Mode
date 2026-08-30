package com.wok.infantry.formation.vehicle;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleReplenishmentSavedDataTest {
    private static final UUID SESSION =
            UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID NEXT_SESSION =
            UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final ResourceLocation DRAGOON =
            ResourceLocation.fromNamespaceAndPath("fcp", "stryker_dragoon");
    private static final ResourceLocation HUMVEE =
            ResourceLocation.fromNamespaceAndPath("fcp", "hmmwv_armored_m2");

    @Test
    void roundTripKeepsPermanentLossAndTimedCooldownSemantics() {
        VehicleReplenishmentSavedData data = new VehicleReplenishmentSavedData();
        VehicleAllocationKey permanent = key("m1296_dragoon_1");
        VehicleAllocationKey timed = key("hmmwv_m2_1");

        assertTrue(data.recordLoss(permanent, DRAGOON,
                VehicleReplenishmentSavedData.NEVER_REPLENISH).success());
        assertTrue(data.recordLoss(timed, HUMVEE, 7_000L).success());

        VehicleReplenishmentSavedData loaded = VehicleReplenishmentSavedData.load(
                data.save(new CompoundTag()));

        assertTrue(loaded.status().success());
        assertEquals(VehicleReplenishmentSavedData.NEVER_REPLENISH,
                loaded.entry(permanent).readyAtTick());
        assertTrue(loaded.ready(6_999L, 8).isEmpty());
        assertEquals(timed, loaded.ready(7_000L, 8).get(0).key());
        assertFalse(loaded.ready(Long.MAX_VALUE, 8).stream()
                .anyMatch(entry -> entry.key().equals(permanent)));
    }

    @Test
    void repeatedLossNeverShortensCooldownAndPermanentLossDominates() {
        VehicleReplenishmentSavedData data = new VehicleReplenishmentSavedData();
        VehicleAllocationKey key = key("hmmwv_m2_1");

        assertTrue(data.recordLoss(key, HUMVEE, 8_000L).success());
        assertTrue(data.recordLoss(key, HUMVEE, 7_000L).success());
        assertEquals(8_000L, data.entry(key).readyAtTick());
        assertTrue(data.recordLoss(key, HUMVEE,
                VehicleReplenishmentSavedData.NEVER_REPLENISH).success());
        assertTrue(data.recordLoss(key, HUMVEE, 9_000L).success());
        assertEquals(VehicleReplenishmentSavedData.NEVER_REPLENISH,
                data.entry(key).readyAtTick());
    }

    @Test
    void reconcileDropsOldSessionsDeletedSlotsAndTypeChanges() {
        VehicleReplenishmentSavedData data = new VehicleReplenishmentSavedData();
        VehicleAllocationKey retained = key("hmmwv_m2_1");
        VehicleAllocationKey deleted = key("hmmwv_m2_2");
        VehicleAllocationKey changed = key("littlebird_armed");
        VehicleAllocationKey oldSession = new VehicleAllocationKey(NEXT_SESSION,
                "academy", "millennium_seminar_mobile", "hmmwv_m2_3");
        assertTrue(data.recordLoss(retained, HUMVEE, 7_000L).success());
        assertTrue(data.recordLoss(deleted, HUMVEE, 7_000L).success());
        assertTrue(data.recordLoss(changed, HUMVEE, 7_000L).success());
        assertTrue(data.recordLoss(oldSession, HUMVEE, 7_000L).success());

        assertTrue(data.reconcile(SESSION, Map.of(
                retained, HUMVEE,
                changed, DRAGOON)).success());

        assertEquals(1, data.entries().size());
        assertTrue(data.contains(retained));
    }

    @Test
    void malformedEntryQuarantinesTheWholeLedger() {
        VehicleReplenishmentSavedData data = new VehicleReplenishmentSavedData();
        assertTrue(data.recordLoss(key("hmmwv_m2_1"), HUMVEE, 7_000L).success());
        CompoundTag root = data.save(new CompoundTag());
        ListTag list = root.getList("Entries", Tag.TAG_COMPOUND);
        list.getCompound(0).putString("EntityType", "minecraft:pig");

        VehicleReplenishmentSavedData loaded =
                VehicleReplenishmentSavedData.load(root);

        assertFalse(loaded.status().success());
        assertTrue(loaded.entries().isEmpty());
        assertTrue(loaded.ready(Long.MAX_VALUE, 8).isEmpty());
    }

    private static VehicleAllocationKey key(String allocationId) {
        return new VehicleAllocationKey(SESSION, "academy",
                "millennium_seminar_mobile", allocationId);
    }
}
