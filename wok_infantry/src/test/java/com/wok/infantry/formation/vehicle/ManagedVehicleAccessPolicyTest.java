package com.wok.infantry.formation.vehicle;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagedVehicleAccessPolicyTest {
    private static final UUID SESSION =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final VehicleOwnership OWNERSHIP = new VehicleOwnership(SESSION,
            "wok_infantry:academy", "wok_infantry:armored", "mbt",
            ResourceLocation.fromNamespaceAndPath("superbwarfare", "m_1a_2"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"));

    @Test
    void sameSessionFactionAndFormationCanMount() {
        assertTrue(ManagedVehicleAccessPolicy.authorize(OWNERSHIP, SESSION,
                "wok_infantry:academy", "wok_infantry:armored").success());
    }

    @Test
    void enemyOrStaleSessionFailsClosed() {
        assertFalse(ManagedVehicleAccessPolicy.authorize(OWNERSHIP, SESSION,
                "wok_infantry:caesar", "wok_infantry:armored").success());
        assertFalse(ManagedVehicleAccessPolicy.authorize(OWNERSHIP, UUID.randomUUID(),
                "wok_infantry:academy", "wok_infantry:armored").success());
    }

    @Test
    void otherOrMissingFormationFailsClosed() {
        assertFalse(ManagedVehicleAccessPolicy.authorize(OWNERSHIP, SESSION,
                "wok_infantry:academy", "wok_infantry:airborne").success());
        assertFalse(ManagedVehicleAccessPolicy.authorize(OWNERSHIP, SESSION,
                "wok_infantry:academy", null).success());
    }

    @Test
    void unmanagedVehicleIsOutsideThisPolicy() {
        assertTrue(ManagedVehicleAccessPolicy.authorize(null, SESSION,
                "wok_infantry:academy", null).success());
    }

    @Test
    void savedLedgerCanRestoreUnloadedAllocationAndPendingResetWithoutMod() {
        SuperbWarfareVehicleService service = new SuperbWarfareVehicleService(
                () -> false, id -> null);
        UUID entityId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        assertTrue(service.restoreTrackedAllocation(OWNERSHIP, entityId, true).success());
        assertEquals(entityId, service.trackedAllocations()
                .get(OWNERSHIP.allocationKey()));
        SuperbWarfareVehicleService.VehicleLedgerEntry entry =
                service.ledgerSnapshot().get(0);
        assertEquals(OWNERSHIP.allocationKey(), entry.allocation());
        assertEquals(OWNERSHIP.entityTypeId(), entry.entityTypeId());
        assertTrue(entry.pendingRetirement());
    }

    @Test
    void savedLedgerRejectsSecondEntityForSameAllocation() {
        SuperbWarfareVehicleService service = new SuperbWarfareVehicleService(
                () -> false, id -> null);
        assertTrue(service.restoreTrackedAllocation(OWNERSHIP,
                UUID.fromString("00000000-0000-0000-0000-000000000002")).success());

        assertFalse(service.restoreTrackedAllocation(OWNERSHIP,
                UUID.fromString("00000000-0000-0000-0000-000000000003")).success());
        assertEquals(1, service.ledgerSnapshot().size());
    }

    @Test
    void startupRetirementSelectsEverySessionExceptTheNewActiveSession() {
        SuperbWarfareVehicleService service = new SuperbWarfareVehicleService(
                () -> false, id -> null);
        UUID oldSession = UUID.fromString("00000000-0000-0000-0000-000000000099");
        VehicleOwnership oldOwnership = new VehicleOwnership(oldSession,
                OWNERSHIP.factionId(), OWNERSHIP.formationId(), "old_mbt",
                OWNERSHIP.entityTypeId(), OWNERSHIP.dimension());
        assertTrue(service.restoreTrackedAllocation(OWNERSHIP,
                UUID.fromString("00000000-0000-0000-0000-000000000002")).success());
        assertTrue(service.restoreTrackedAllocation(oldOwnership,
                UUID.fromString("00000000-0000-0000-0000-000000000003")).success());

        assertEquals(List.of(oldOwnership.allocationKey()),
                service.retirementCandidates(SESSION));
        assertTrue(service.retirementCandidates(oldSession).contains(
                OWNERSHIP.allocationKey()));
    }

    @Test
    void hotReloadReconciliationFindsRemovedRenamedAndTypeChangedAllocationsOnly() {
        SuperbWarfareVehicleService service = new SuperbWarfareVehicleService(
                () -> false, id -> null);
        VehicleOwnership removed = ownership("removed_mbt", "m_1a_2");
        VehicleOwnership typeChanged = ownership("type_changed", "m_1a_2");
        VehicleOwnership retained = ownership("retained", "m_1a_2");
        VehicleOwnership otherSession = new VehicleOwnership(UUID.randomUUID(),
                OWNERSHIP.factionId(), OWNERSHIP.formationId(), "old_session",
                OWNERSHIP.entityTypeId(), OWNERSHIP.dimension());
        assertTrue(service.restoreTrackedAllocation(removed, UUID.randomUUID()).success());
        assertTrue(service.restoreTrackedAllocation(typeChanged, UUID.randomUUID()).success());
        assertTrue(service.restoreTrackedAllocation(retained, UUID.randomUUID()).success());
        assertTrue(service.restoreTrackedAllocation(otherSession, UUID.randomUUID()).success());

        ResourceLocation replacement = ResourceLocation.fromNamespaceAndPath(
                "superbwarfare", "a_10");
        var expected = java.util.Map.of(
                typeChanged.allocationKey(), replacement,
                retained.allocationKey(), retained.entityTypeId(),
                new VehicleAllocationKey(SESSION, OWNERSHIP.factionId(),
                        OWNERSHIP.formationId(), "renamed_mbt"), removed.entityTypeId());

        assertEquals(List.of(removed.allocationKey(), typeChanged.allocationKey()),
                service.reconciliationCandidates(SESSION, expected));
    }

    @Test
    void pendingTombstoneRemainsRetiredEvenWhenAllocationReturnsToConfig() {
        SuperbWarfareVehicleService service = new SuperbWarfareVehicleService(
                () -> false, id -> null);
        assertTrue(service.restoreTrackedAllocation(OWNERSHIP, UUID.randomUUID(), true)
                .success());

        assertEquals(List.of(OWNERSHIP.allocationKey()),
                service.reconciliationCandidates(SESSION,
                        java.util.Map.of(OWNERSHIP.allocationKey(),
                                OWNERSHIP.entityTypeId())));
    }

    @Test
    void hotReloadExpectedLedgerRejectsForeignSessionAndNonProviderEntityType() {
        VehicleAllocationKey foreignSession = new VehicleAllocationKey(UUID.randomUUID(),
                OWNERSHIP.factionId(), OWNERSHIP.formationId(), "foreign_session");
        assertFalse(SuperbWarfareVehicleService.validateExpectedAllocations(SESSION,
                java.util.Map.of(foreignSession, OWNERSHIP.entityTypeId())).success());
        assertFalse(SuperbWarfareVehicleService.validateExpectedAllocations(SESSION,
                java.util.Map.of(OWNERSHIP.allocationKey(),
                        ResourceLocation.fromNamespaceAndPath("minecraft", "pig"))).success());
        assertTrue(SuperbWarfareVehicleService.validateExpectedAllocations(SESSION,
                java.util.Map.of(OWNERSHIP.allocationKey(), OWNERSHIP.entityTypeId()))
                .success());
    }

    private static VehicleOwnership ownership(String allocationId, String entityPath) {
        return new VehicleOwnership(SESSION, OWNERSHIP.factionId(), OWNERSHIP.formationId(),
                allocationId, ResourceLocation.fromNamespaceAndPath("superbwarfare", entityPath),
                OWNERSHIP.dimension());
    }
}
