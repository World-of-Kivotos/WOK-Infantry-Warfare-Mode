package com.wok.infantry.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client-only presentation preferences for WOK Infantry. */
public final class InfantryClientConfig {
    public static final double MIN_MAP_MARKER_SCALE = 0.75D;
    public static final double MAX_MAP_MARKER_SCALE = 1.75D;
    public static final double DEFAULT_MAP_MARKER_SCALE = 1.0D;

    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.DoubleValue MAP_MARKER_SCALE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Tactical map presentation settings")
                .push("tacticalMap");
        MAP_MARKER_SCALE = builder
                .comment("Scale for infantry, tank, and IFV tactical-map markers")
                .defineInRange("intelMarkerScale", DEFAULT_MAP_MARKER_SCALE,
                        MIN_MAP_MARKER_SCALE, MAX_MAP_MARKER_SCALE);
        builder.pop();
        SPEC = builder.build();
    }

    private InfantryClientConfig() {
    }

    public static double markerScale() {
        if (!SPEC.isLoaded()) {
            return DEFAULT_MAP_MARKER_SCALE;
        }
        return clamp(MAP_MARKER_SCALE.get());
    }

    public static void setMarkerScale(double value) {
        if (!SPEC.isLoaded()) {
            return;
        }
        double clamped = clamp(value);
        if (Double.compare(MAP_MARKER_SCALE.get(), clamped) != 0) {
            MAP_MARKER_SCALE.set(clamped);
        }
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) {
            return DEFAULT_MAP_MARKER_SCALE;
        }
        return Math.max(MIN_MAP_MARKER_SCALE,
                Math.min(MAX_MAP_MARKER_SCALE, value));
    }
}
