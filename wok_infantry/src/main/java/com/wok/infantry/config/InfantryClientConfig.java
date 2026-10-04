package com.wok.infantry.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client-only presentation preferences for WOK Infantry. */
public final class InfantryClientConfig {
    public static final double MIN_MAP_MARKER_SCALE = 0.75D;
    public static final double MAX_MAP_MARKER_SCALE = 1.75D;
    public static final double DEFAULT_MAP_MARKER_SCALE = 1.0D;
    public static final boolean DEFAULT_MINIMUM_SCALE_2X = true;
    public static final boolean DEFAULT_REDIRECT_XAERO_WORLD_MAP = true;

    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.DoubleValue MAP_MARKER_SCALE;
    private static final ForgeConfigSpec.BooleanValue REDIRECT_XAERO_WORLD_MAP;
    private static final ForgeConfigSpec.BooleanValue MINIMUM_SCALE_2X;
    private static final ForgeConfigSpec.IntValue KEY_DEFAULTS_REVISION;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Tactical map presentation settings")
                .push("tacticalMap");
        MAP_MARKER_SCALE = builder
                .comment("Scale for infantry, tank, and IFV tactical-map markers")
                .defineInRange("intelMarkerScale", DEFAULT_MAP_MARKER_SCALE,
                        MIN_MAP_MARKER_SCALE, MAX_MAP_MARKER_SCALE);
        REDIRECT_XAERO_WORLD_MAP = builder
                .comment("With Xaero's World Map installed: while you are in a battle (have a "
                        + "faction), opening Xaero's world map (its key, M by default) opens the "
                        + "WOK tactical map instead. Outside a battle Xaero's map opens as usual. "
                        + "Set to false to always open Xaero's map; then bind the WOK tactical "
                        + "map key to a different key in the controls screen.")
                .define("redirectXaeroWorldMap", DEFAULT_REDIRECT_XAERO_WORLD_MAP);
        builder.pop();
        builder.comment("Screen and HUD presentation settings")
                .push("ui");
        MINIMUM_SCALE_2X = builder
                .comment("At GUI scale 1 with a window of at least 640x480, lay out and draw WOK "
                        + "Infantry screens and HUD at 2x (960x720 is laid out as 480x360) so CJK "
                        + "text stays readable. Set to false to keep the 1x size.")
                .define("minimumScale2x", DEFAULT_MINIMUM_SCALE_2X);
        builder.pop();
        builder.comment("Key binding bookkeeping")
                .push("keys");
        KEY_DEFAULTS_REVISION = builder
                .comment("Revision of the WOK default key table already applied to options.txt "
                        + "(written by the mod, do not edit). While it is lower than the current "
                        + "revision, WOK keys still on their old default (K, L, U, O, and M with "
                        + "Xaero's World Map installed) are moved once to the new default; keys "
                        + "you chose yourself are kept.")
                .defineInRange("defaultsRevision", 0, 0, Integer.MAX_VALUE);
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

    /**
     * {@code tacticalMap.redirectXaeroWorldMap}: whether Xaero's world map screen is replaced by
     * the tactical map while the player is in a battle.
     */
    public static boolean redirectXaeroWorldMap() {
        if (!SPEC.isLoaded()) {
            return DEFAULT_REDIRECT_XAERO_WORLD_MAP;
        }
        return REDIRECT_XAERO_WORLD_MAP.get();
    }

    /** {@code ui.minimumScale2x}: whether GUI scale 1 draws WOK screens and HUD at 2x. */
    public static boolean minimumScale2x() {
        if (!SPEC.isLoaded()) {
            return DEFAULT_MINIMUM_SCALE_2X;
        }
        return MINIMUM_SCALE_2X.get();
    }

    /**
     * {@code keys.defaultsRevision}: default key table revision already applied to options.txt,
     * or {@code -1} while the client config is not loaded (nothing may be migrated yet).
     */
    public static int keyDefaultsRevision() {
        if (!SPEC.isLoaded()) {
            return -1;
        }
        return KEY_DEFAULTS_REVISION.get();
    }

    /** Records that the default key table {@code revision} has been applied (saved at once). */
    public static void setKeyDefaultsRevision(int revision) {
        if (!SPEC.isLoaded() || KEY_DEFAULTS_REVISION.get() == revision) {
            return;
        }
        KEY_DEFAULTS_REVISION.set(Math.max(0, revision));
        KEY_DEFAULTS_REVISION.save();
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) {
            return DEFAULT_MAP_MARKER_SCALE;
        }
        return Math.max(MIN_MAP_MARKER_SCALE,
                Math.min(MAX_MAP_MARKER_SCALE, value));
    }
}
