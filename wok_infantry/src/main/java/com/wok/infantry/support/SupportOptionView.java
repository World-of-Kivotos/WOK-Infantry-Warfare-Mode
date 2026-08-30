package com.wok.infantry.support;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/** Client-safe definition and faction-specific availability for one registered support. */
public record SupportOptionView(
        ResourceLocation id,
        String translationKey,
        String fallbackName,
        String shortName,
        SupportTargetMode targetMode,
        double radius,
        boolean providerAvailable,
        String availabilityReason,
        long readyAtGameTick,
        boolean active
) {
    public static final int MAX_AVAILABILITY_REASON_LENGTH = 160;

    public SupportOptionView {
        SupportDefinition.requireValidId(id);
        translationKey = requireText(translationKey,
                SupportDefinition.MAX_TRANSLATION_KEY_LENGTH, "translation key");
        fallbackName = requireText(fallbackName,
                SupportDefinition.MAX_FALLBACK_NAME_LENGTH, "fallback name");
        shortName = requireText(shortName,
                SupportDefinition.MAX_SHORT_NAME_LENGTH, "short name");
        Objects.requireNonNull(targetMode, "targetMode");
        if (!Double.isFinite(radius) || radius < 0.0D
                || radius > SupportDefinition.MAX_RADIUS) {
            throw new IllegalArgumentException("Invalid support option radius");
        }
        availabilityReason = sanitizeAvailabilityReason(availabilityReason);
        if (readyAtGameTick < 0L) {
            throw new IllegalArgumentException("Support ready tick cannot be negative");
        }
        if (providerAvailable && !availabilityReason.isBlank()) {
            availabilityReason = "";
        }
    }

    public SupportOptionView(SupportDefinition definition, boolean providerAvailable,
                             String availabilityReason, long readyAtGameTick, boolean active) {
        this(definition.id(), definition.translationKey(), definition.fallbackName(),
                definition.shortName(), definition.targetMode(), definition.radius(),
                providerAvailable, availabilityReason, readyAtGameTick, active);
    }

    public boolean directional() {
        return targetMode == SupportTargetMode.DIRECTIONAL;
    }

    public long cooldownRemainingTicks(long serverGameTick) {
        return Math.max(0L, readyAtGameTick - Math.max(0L, serverGameTick));
    }

    public boolean ready(long serverGameTick) {
        return providerAvailable && !active && cooldownRemainingTicks(serverGameTick) == 0L;
    }

    public static String sanitizeAvailabilityReason(String reason) {
        String safe = Objects.requireNonNullElse(reason, "");
        if (safe.length() <= MAX_AVAILABILITY_REASON_LENGTH) {
            return safe;
        }
        int end = MAX_AVAILABILITY_REASON_LENGTH;
        if (Character.isHighSurrogate(safe.charAt(end - 1))
                && Character.isLowSurrogate(safe.charAt(end))) {
            end--;
        }
        return safe.substring(0, end);
    }

    private static String requireText(String value, int maximumLength, String field) {
        String checked = Objects.requireNonNull(value, field).trim();
        if (checked.isEmpty() || checked.length() > maximumLength) {
            throw new IllegalArgumentException("Invalid support option " + field);
        }
        return checked;
    }
}
