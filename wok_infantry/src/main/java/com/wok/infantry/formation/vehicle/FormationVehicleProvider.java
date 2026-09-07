package com.wok.infantry.formation.vehicle;

import com.wok.infantry.battle.ActionResult;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.UUID;

/** Stable wiring surface for the formation lifecycle and optional vehicle providers. */
public interface FormationVehicleProvider {
    ActionResult start(MinecraftServer server);

    ActionResult start(MinecraftServer server, UUID activeSession);

    ActionResult retireSessionsExcept(MinecraftServer server, UUID activeSession);

    /**
     * Reconciles already allocated vehicles after a formation-catalog reload. The expected map
     * describes allocation keys that remain valid in the active session and their configured
     * entity types; it does not spawn missing vehicles.
     */
    ActionResult reconcileActiveSession(MinecraftServer server, UUID activeSession,
                                        Map<VehicleAllocationKey, ResourceLocation>
                                                expectedAllocations);

    VehicleDeploymentResult deployBatch(MinecraftServer server,
                                        VehicleDeploymentRequest request);

    ActionResult resetAllocation(MinecraftServer server, VehicleAllocationKey key);

    ActionResult resetSession(MinecraftServer server, UUID sessionId);

    ActionResult observeLoadedVehicle(Entity entity);

    VehicleRemovalObservation observeRemovedVehicle(Entity entity);

    ActionResult authorizeMount(Entity vehicle, UUID activeSession, String actorFactionId,
                                String actorFormationId);

    /** Compatibility bridge that fails closed for managed vehicles until the caller supplies a
     * formation identity. */
    default ActionResult authorizeMount(Entity vehicle, UUID activeSession,
                                        String actorFactionId) {
        return authorizeMount(vehicle, activeSession, actorFactionId, null);
    }

    ActionResult stop(MinecraftServer server);
}
