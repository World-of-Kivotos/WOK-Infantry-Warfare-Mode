package com.wok.infantry.client.map;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Client extension point for support-specific tactical-map presentation. */
public final class TacticalSupportMapPresentationRegistry {
    private static final ConcurrentMap<ResourceLocation, TacticalSupportMapPresentation>
            PRESENTATIONS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<ResourceLocation, Double>
            GUIDANCE_RADII = new ConcurrentHashMap<>();

    private TacticalSupportMapPresentationRegistry() {
    }

    public static void register(ResourceLocation supportId,
                                TacticalSupportMapPresentation presentation) {
        PRESENTATIONS.put(Objects.requireNonNull(supportId, "supportId"),
                Objects.requireNonNull(presentation, "presentation"));
    }

    /**
     * Registers an inner designation/guidance zone, in blocks, drawn inside the support area.
     *
     * <p>Like the presentation hint this is client-only: the server keeps validating the actual
     * designation radius. Supports without a registered zone keep their previous map drawing.</p>
     */
    public static void registerGuidanceRadius(ResourceLocation supportId, double radius) {
        Objects.requireNonNull(supportId, "supportId");
        if (!Double.isFinite(radius) || radius <= 0.0D) {
            throw new IllegalArgumentException(
                    "guidance radius must be finite and positive: " + radius);
        }
        GUIDANCE_RADII.put(supportId, radius);
    }

    public static void unregisterGuidanceRadius(ResourceLocation supportId) {
        GUIDANCE_RADII.remove(Objects.requireNonNull(supportId, "supportId"));
    }

    /** Removes every client presentation hint of the support, including its guidance zone. */
    public static void unregister(ResourceLocation supportId) {
        Objects.requireNonNull(supportId, "supportId");
        PRESENTATIONS.remove(supportId);
        GUIDANCE_RADII.remove(supportId);
    }

    public static TacticalSupportMapPresentation presentation(ResourceLocation supportId) {
        return PRESENTATIONS.getOrDefault(Objects.requireNonNull(supportId, "supportId"),
                TacticalSupportMapPresentation.UTILITY);
    }

    public static OptionalDouble guidanceRadius(ResourceLocation supportId) {
        Double radius = GUIDANCE_RADII.get(Objects.requireNonNull(supportId, "supportId"));
        return radius == null ? OptionalDouble.empty() : OptionalDouble.of(radius);
    }
}
