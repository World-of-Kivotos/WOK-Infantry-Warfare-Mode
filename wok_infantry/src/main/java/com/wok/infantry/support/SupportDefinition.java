package com.wok.infantry.support;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * Immutable, provider-neutral definition of one commander support capability.
 *
 * <p>The namespaced id is the only durable identity used by packets, cooldown data and active
 * missions. Optional integrations may register definitions without adding enum constants to the
 * core.</p>
 */
public record SupportDefinition(
        ResourceLocation id,
        String translationKey,
        String fallbackName,
        String shortName,
        SupportTargetMode targetMode,
        long cooldownTicks,
        long inboundTicks,
        int stepCount,
        int stepIntervalTicks,
        double radius
) {
    /** Must stay aligned with the bounded battle protocol support-id field. */
    public static final int MAX_ID_LENGTH = 128;
    public static final int MAX_TRANSLATION_KEY_LENGTH = 128;
    public static final int MAX_FALLBACK_NAME_LENGTH = 64;
    public static final int MAX_SHORT_NAME_LENGTH = 24;
    public static final int MAX_STEPS = 64;
    public static final double MAX_RADIUS = 512.0D;

    public SupportDefinition {
        requireValidId(id);
        translationKey = requireText(translationKey, MAX_TRANSLATION_KEY_LENGTH,
                "translation key");
        fallbackName = requireText(fallbackName, MAX_FALLBACK_NAME_LENGTH,
                "fallback name");
        shortName = requireText(shortName, MAX_SHORT_NAME_LENGTH, "short name");
        Objects.requireNonNull(targetMode, "targetMode");
        if (cooldownTicks < 0L || inboundTicks < 0L) {
            throw new IllegalArgumentException("Support timing cannot be negative");
        }
        if (stepCount < 1 || stepCount > MAX_STEPS) {
            throw new IllegalArgumentException("Support step count must be between 1 and "
                    + MAX_STEPS);
        }
        if (stepIntervalTicks < 1) {
            throw new IllegalArgumentException("Support step interval must be positive");
        }
        if (!Double.isFinite(radius) || radius < 0.0D || radius > MAX_RADIUS) {
            throw new IllegalArgumentException("Support radius must be finite and between 0 and "
                    + MAX_RADIUS);
        }
    }

    public boolean directional() {
        return targetMode == SupportTargetMode.DIRECTIONAL;
    }

    public static ResourceLocation requireValidId(ResourceLocation id) {
        ResourceLocation checked = Objects.requireNonNull(id, "id");
        if (checked.toString().length() > MAX_ID_LENGTH) {
            throw new IllegalArgumentException("Support id exceeds " + MAX_ID_LENGTH
                    + " characters: " + checked);
        }
        return checked;
    }

    private static String requireText(String value, int maximumLength, String field) {
        String checked = Objects.requireNonNull(value, field).trim();
        if (checked.isEmpty() || checked.length() > maximumLength) {
            throw new IllegalArgumentException("Support " + field
                    + " must contain 1 to " + maximumLength + " characters");
        }
        return checked;
    }
}
