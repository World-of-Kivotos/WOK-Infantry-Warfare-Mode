package com.wok.infantry.formation.vehicle;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleAllocationSavedDataTest {
    private static final UUID SESSION =
            UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ENTITY_A =
            UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID ENTITY_B =
            UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final ResourceLocation TYPE =
            ResourceLocation.fromNamespaceAndPath("superbwarfare", "m_1a_2");
    private static final ResourceLocation DIMENSION =
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");

    @Test
    void roundTripRetainsPendingRetirementAndCanRestoreWithoutOptionalMod() {
        VehicleAllocationSavedData original = new VehicleAllocationSavedData();
        SuperbWarfareVehicleService.VehicleLedgerEntry pending =
                entry("academy_mbt", ENTITY_A, true);
        SuperbWarfareVehicleService.VehicleLedgerEntry active =
                entry("academy_ifv", ENTITY_B, false);

        assertTrue(original.synchronize(List.of(active, pending)).success());
        VehicleAllocationSavedData loaded = VehicleAllocationSavedData.load(
                original.save(new CompoundTag()));

        assertTrue(loaded.status().success());
        assertEquals(2, loaded.entries().size());
        assertEquals("academy_ifv", loaded.entries().get(0).allocation().allocationId());
        assertFalse(loaded.entries().get(0).pendingRetirement());
        assertEquals("academy_mbt", loaded.entries().get(1).allocation().allocationId());
        assertTrue(loaded.entries().get(1).pendingRetirement());

        SuperbWarfareVehicleService service = new SuperbWarfareVehicleService(
                () -> false, ignored -> null);
        for (SuperbWarfareVehicleService.VehicleLedgerEntry persisted : loaded.entries()) {
            VehicleAllocationKey key = persisted.allocation();
            VehicleOwnership ownership = new VehicleOwnership(key.sessionId(), key.factionId(),
                    key.formationId(), key.allocationId(), persisted.entityTypeId(),
                    persisted.dimension());
            assertTrue(service.restoreTrackedAllocation(ownership, persisted.entityId(),
                    persisted.pendingRetirement()).success());
        }
        assertEquals(2, service.trackedAllocations().size());
        assertTrue(service.ledgerSnapshot().stream()
                .filter(value -> value.allocation().allocationId().equals("academy_mbt"))
                .findFirst().orElseThrow().pendingRetirement());
    }

    @Test
    void unsupportedOrMissingVersionStaysQuarantinedAcrossCanonicalSave() {
        CompoundTag unsupported = new CompoundTag();
        unsupported.putInt("Version", VehicleAllocationSavedData.DATA_VERSION + 1);
        unsupported.putBoolean("Quarantined", false);
        unsupported.put("Allocations", new ListTag());

        VehicleAllocationSavedData future = VehicleAllocationSavedData.load(unsupported);
        assertTrue(future.quarantined());
        assertFalse(future.status().success());
        VehicleAllocationSavedData persistedQuarantine = VehicleAllocationSavedData.load(
                future.save(new CompoundTag()));
        assertTrue(persistedQuarantine.quarantined());

        CompoundTag missingVersion = new CompoundTag();
        missingVersion.putBoolean("Quarantined", false);
        missingVersion.put("Allocations", new ListTag());
        assertTrue(VehicleAllocationSavedData.load(missingVersion).quarantined());
    }

    @Test
    void malformedEntryQuarantinesWholeLedgerInsteadOfDroppingIt() {
        VehicleAllocationSavedData original = new VehicleAllocationSavedData();
        assertTrue(original.synchronize(List.of(entry("academy_mbt", ENTITY_A, false)))
                .success());
        CompoundTag root = original.save(new CompoundTag());
        ListTag list = root.getList("Allocations", Tag.TAG_COMPOUND);
        list.getCompound(0).remove("EntityType");

        VehicleAllocationSavedData loaded = VehicleAllocationSavedData.load(root);

        assertTrue(loaded.quarantined());
        assertTrue(loaded.entries().isEmpty());
        assertFalse(loaded.status().success());
    }

    @Test
    void duplicateAllocationOrEntityUuidQuarantinesWholeLedger() {
        VehicleAllocationSavedData one = new VehicleAllocationSavedData();
        assertTrue(one.synchronize(List.of(entry("academy_mbt", ENTITY_A, false)))
                .success());
        CompoundTag duplicateAllocation = one.save(new CompoundTag());
        ListTag duplicated = duplicateAllocation.getList("Allocations", Tag.TAG_COMPOUND);
        duplicated.add(duplicated.getCompound(0).copy());
        assertTrue(VehicleAllocationSavedData.load(duplicateAllocation).quarantined());

        VehicleAllocationSavedData two = new VehicleAllocationSavedData();
        assertTrue(two.synchronize(List.of(entry("academy_mbt", ENTITY_A, false),
                entry("academy_ifv", ENTITY_B, false))).success());
        CompoundTag duplicateEntity = two.save(new CompoundTag());
        ListTag twoEntries = duplicateEntity.getList("Allocations", Tag.TAG_COMPOUND);
        twoEntries.getCompound(1).putUUID("Entity",
                twoEntries.getCompound(0).getUUID("Entity"));
        assertTrue(VehicleAllocationSavedData.load(duplicateEntity).quarantined());
    }

    @Test
    void rejectedRuntimeSnapshotNeverWeakensPreviousLedger() {
        VehicleAllocationSavedData data = new VehicleAllocationSavedData();
        SuperbWarfareVehicleService.VehicleLedgerEntry original =
                entry("academy_mbt", ENTITY_A, true);
        assertTrue(data.synchronize(List.of(original)).success());

        assertFalse(data.synchronize(List.of(original,
                entry("academy_ifv", ENTITY_A, false))).success());
        assertEquals(List.of(original), data.entries());
        assertFalse(data.quarantined());
    }

    @Test
    void nonSuperbWarfareEntryIsRejectedWithoutMutatingLedger() {
        VehicleAllocationSavedData data = new VehicleAllocationSavedData();
        SuperbWarfareVehicleService.VehicleLedgerEntry invalid =
                new SuperbWarfareVehicleService.VehicleLedgerEntry(
                        new VehicleAllocationKey(SESSION, "wok_infantry:academy",
                                "wok_infantry:armored", "academy_mbt"), ENTITY_A,
                        ResourceLocation.fromNamespaceAndPath("minecraft", "pig"), DIMENSION,
                        false);

        assertFalse(data.synchronize(List.of(invalid)).success());
        assertTrue(data.entries().isEmpty());
        assertFalse(data.quarantined());
    }

    private static SuperbWarfareVehicleService.VehicleLedgerEntry entry(
            String allocationId, UUID entityId, boolean pending) {
        return new SuperbWarfareVehicleService.VehicleLedgerEntry(
                new VehicleAllocationKey(SESSION, "wok_infantry:academy",
                        "wok_infantry:armored", allocationId), entityId, TYPE, DIMENSION,
                pending);
    }
}
