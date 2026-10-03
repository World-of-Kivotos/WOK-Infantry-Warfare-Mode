package com.wok.infantry.support.adapter;

import net.minecraft.resources.ResourceLocation;

/**
 * Provider-neutral execution boundary. Implementations must not expose optional-mod classes in
 * public signatures and are bound to one namespaced support definition id.
 */
public interface SupportProvider {
    ResourceLocation supportId();

    ProviderAvailability availability();

    /**
     * Executes one bounded logical step of an accepted mission.
     *
     * <p>Throw {@link SupportSpawnException#endMission} to stop the mission and keep the
     * cooldown, {@link SupportSpawnException#notDelivered} when nothing reached the battlefield
     * and the cooldown should be returned (the scheduler honours this only before the first
     * step has completed), or {@link SupportSpawnException#providerBroken} when the integration
     * itself is defective and must stay disabled until restart. Unchecked runtime and linkage
     * faults are treated as provider-broken without refund.</p>
     */
    void executeStep(SupportSpawnContext context) throws SupportSpawnException;

    /**
     * Called at most once when the scheduler itself ends a mission after at least one step has
     * run (requester left the faction, footprint unloaded or outside the border, provider
     * disabled). {@code context.stepIndex()} is the step that will no longer run. Implementations
     * remove whatever the mission left in the world, using only already-loaded state; this is
     * called even after the provider's circuit has tripped. Failures are logged and ignored.
     */
    default void abandon(SupportSpawnContext context) {
    }
}
