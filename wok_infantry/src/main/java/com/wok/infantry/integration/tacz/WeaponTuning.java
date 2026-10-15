package com.wok.infantry.integration.tacz;

/** Four independently persisted multipliers for one TaCZ gun ItemStack. */
public record WeaponTuning(float adsSpeedScale, float verticalRecoilScale,
                           float horizontalRecoilScale, float spreadScale) {
    public static final float MIN_SCALE = 0.25F;
    public static final float MAX_STANDARD_SCALE = 15.00F;
    public static final float MAX_RECOIL_SCALE = 15.00F;
    public static final float DEFAULT_SCALE = 1.00F;
    public static final WeaponTuning DEFAULT = new WeaponTuning(
            DEFAULT_SCALE, DEFAULT_SCALE, DEFAULT_SCALE, DEFAULT_SCALE);

    public boolean valid() {
        return validStandardScale(adsSpeedScale) && validRecoilScale(verticalRecoilScale)
                && validRecoilScale(horizontalRecoilScale) && validStandardScale(spreadScale);
    }

    public boolean defaultValues() {
        return adsSpeedScale == DEFAULT_SCALE
                && verticalRecoilScale == DEFAULT_SCALE
                && horizontalRecoilScale == DEFAULT_SCALE
                && spreadScale == DEFAULT_SCALE;
    }

    public static boolean validScale(float scale) {
        return validStandardScale(scale);
    }

    public static boolean validStandardScale(float scale) {
        return validScale(scale, MAX_STANDARD_SCALE);
    }

    public static boolean validRecoilScale(float scale) {
        return validScale(scale, MAX_RECOIL_SCALE);
    }

    private static boolean validScale(float scale, float maximum) {
        return Float.isFinite(scale) && scale >= MIN_SCALE && scale <= maximum;
    }
}
