package com.wok.infantry.support.adapter;

import net.minecraft.resources.ResourceLocation;

/**
 * Provider-neutral execution boundary. Implementations must not expose optional-mod classes in
 * public signatures and are bound to one namespaced support definition id.
 */
public interface SupportProvider {
    ResourceLocation supportId();

    ProviderAvailability availability();

    /** Executes one bounded logical step of an accepted mission. */
    void executeStep(SupportSpawnContext context) throws SupportSpawnException;
}
