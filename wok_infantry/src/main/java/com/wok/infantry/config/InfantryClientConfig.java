package com.wok.infantry.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client-only presentation preferences for WOK Infantry. */
public final class InfantryClientConfig {
    public static final double MIN_MAP_MARKER_SCALE = 0.75D;
    public static final double MAX_MAP_MARKER_SCALE = 1.75D;
    public static final double DEFAULT_MAP_MARKER_SCALE = 1.0D;
    public static final boolean DEFAULT_MINIMUM_SCALE_2X = true;
    public static final boolean DEFAULT_REDIRECT_XAERO_WORLD_MAP = true;

    public static final HudRosterMode DEFAULT_HUD_ROSTER_MODE = HudRosterMode.AUTO;
    public static final boolean DEFAULT_SHOW_BATTLE_STRIP = true;

    /** {@code hud.rosterMode}: how the in-battle squad roster is shown. */
    public enum HudRosterMode {
        /** Full roster; title row only on a tight screen with chat open, with F3, or while Tab is held. */
        AUTO,
        /** Always the full roster. */
        FULL,
        /** Always only the title row (squad name and member count). */
        COLLAPSED,
        /** No roster. */
        HIDDEN
    }

    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.DoubleValue MAP_MARKER_SCALE;
    private static final ForgeConfigSpec.BooleanValue REDIRECT_XAERO_WORLD_MAP;
    private static final ForgeConfigSpec.BooleanValue MINIMUM_SCALE_2X;
    private static final ForgeConfigSpec.IntValue KEY_DEFAULTS_REVISION;
    private static final ForgeConfigSpec.EnumValue<HudRosterMode> HUD_ROSTER_MODE;
    private static final ForgeConfigSpec.BooleanValue SHOW_BATTLE_STRIP;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Tactical map presentation settings")
                .push("tacticalMap");
        MAP_MARKER_SCALE = builder
                .comment("Scale for every tactical-map marker (intel, orders and deployment "
                        + "points); markers snap to whole physical pixels")
                .defineInRange("intelMarkerScale", DEFAULT_MAP_MARKER_SCALE,
                        MIN_MAP_MARKER_SCALE, MAX_MAP_MARKER_SCALE);
        REDIRECT_XAERO_WORLD_MAP = builder
                .comment("With Xaero's World Map installed: once you are in a battle (have a "
                        + "faction and its formation is locked), opening Xaero's world map (its key, "
                        + "M by default) opens the WOK tactical map instead. Before that Xaero's map "
                        + "opens as usual. "
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
                        + "revision, WOK keys still on their old default (K, L, U, O, and M while "
                        + "Xaero's World Map is redirected and its open-map key is on M as well) are "
                        + "moved once to the new default; keys you chose yourself are kept.")
                .defineInRange("defaultsRevision", 0, 0, Integer.MAX_VALUE);
        builder.pop();
        builder.comment("In-battle HUD settings (the GUI scale 1 size follows ui.minimumScale2x)")
                .push("hud");
        HUD_ROSTER_MODE = builder
                .comment("Squad roster: AUTO shows every member and shrinks to the title row on "
                        + "small screens while chat is open, with F3 or while the player list key "
                        + "is held; FULL always shows every member; COLLAPSED always shows only the "
                        + "title row; HIDDEN hides the roster.")
                .defineEnum("rosterMode", DEFAULT_HUD_ROSTER_MODE);
        SHOW_BATTLE_STRIP = builder
                .comment("Show the top battle strip with both sides' manpower. Round results and "
                        + "base supply notices are shown either way.")
                .define("showBattleStrip", DEFAULT_SHOW_BATTLE_STRIP);
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

    /** {@code hud.rosterMode}; {@link #DEFAULT_HUD_ROSTER_MODE} while the config is not loaded. */
    public static HudRosterMode hudRosterMode() {
        if (!SPEC.isLoaded()) {
            return DEFAULT_HUD_ROSTER_MODE;
        }
        HudRosterMode mode = HUD_ROSTER_MODE.get();
        return mode == null ? DEFAULT_HUD_ROSTER_MODE : mode;
    }

    /** {@code hud.showBattleStrip}: whether the manpower strip is drawn at the top. */
    public static boolean showBattleStrip() {
        if (!SPEC.isLoaded()) {
            return DEFAULT_SHOW_BATTLE_STRIP;
        }
        return SHOW_BATTLE_STRIP.get();
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) {
            return DEFAULT_MAP_MARKER_SCALE;
        }
        return Math.max(MIN_MAP_MARKER_SCALE,
                Math.min(MAX_MAP_MARKER_SCALE, value));
    }
}
