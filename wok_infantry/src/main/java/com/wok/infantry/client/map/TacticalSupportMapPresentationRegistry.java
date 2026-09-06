package com.wok.infantry.client.map;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Client extension point for support-specific tactical-map presentation. */
public final class TacticalSupportMapPresentationRegistry {
    private static final ConcurrentMap<ResourceLocation, TacticalSupportMapPresentation>
            PRESENTATIONS = new ConcurrentHashMap<>();

    private TacticalSupportMapPresentationRegistry() {
    }

    public static void register(ResourceLocation supportId,
                                TacticalSupportMapPresentation presentation) {
        PRESENTATIONS.put(Objects.requireNonNull(supportId, "supportId"),
                Objects.requireNonNull(presentation, "presentation"));
    }

    public static void unregister(ResourceLocation supportId) {
        PRESENTATIONS.remove(Objects.requireNonNull(supportId, "supportId"));
    }

    public static TacticalSupportMapPresentation presentation(ResourceLocation supportId) {
        return PRESENTATIONS.getOrDefault(Objects.requireNonNull(supportId, "supportId"),
                TacticalSupportMapPresentation.UTILITY);
    }
}
