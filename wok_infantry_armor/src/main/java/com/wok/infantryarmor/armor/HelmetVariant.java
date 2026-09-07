package com.wok.infantryarmor.armor;

/** Stable identities and protection profiles for wearable head armor. */
public enum HelmetVariant {
    RIOT("riot", PlateArmorTier.II, PlateArmorWeight.MEDIUM,
            PlateArmorConstructionMaterial.ARAMID),
    LIGHTWEIGHT_BALLISTIC_MASK("lightweight_ballistic_mask", PlateArmorTier.II,
            PlateArmorWeight.LIGHT, PlateArmorConstructionMaterial.ARAMID),

    CQCM_BALLISTIC_MASK("cqcm_ballistic_mask", PlateArmorTier.III,
            PlateArmorWeight.MEDIUM, PlateArmorConstructionMaterial.ARMOR_STEEL),
    LZSH_LIGHT("lzsh_light", PlateArmorTier.III, PlateArmorWeight.LIGHT,
            PlateArmorConstructionMaterial.UHMWPE),
    B6B47("6b47", PlateArmorTier.III, PlateArmorWeight.MEDIUM,
            PlateArmorConstructionMaterial.ARAMID),
    B6B47_DIGITAL_COVER("6b47_digital_cover", PlateArmorTier.III,
            PlateArmorWeight.MEDIUM, PlateArmorConstructionMaterial.ARAMID),
    CAIMAN_COMPOSITE("caiman_composite", PlateArmorTier.III,
            PlateArmorWeight.LIGHT, PlateArmorConstructionMaterial.COMBINED),
    KIVER_M_HEAVY("kiver_m_heavy", PlateArmorTier.III,
            PlateArmorWeight.HEAVY, PlateArmorConstructionMaterial.TITANIUM),

    ACHHC_LIGHT("achhc_light", PlateArmorTier.IV, PlateArmorWeight.LIGHT,
            PlateArmorConstructionMaterial.ARAMID),
    STRIKE("strike", PlateArmorTier.IV, PlateArmorWeight.MEDIUM,
            PlateArmorConstructionMaterial.COMBINED),
    FAST_MT_SUPER_HIGH_CUT("fast_mt_super_high_cut", PlateArmorTier.IV,
            PlateArmorWeight.LIGHT, PlateArmorConstructionMaterial.UHMWPE),
    AIRFRAME("airframe", PlateArmorTier.IV, PlateArmorWeight.LIGHT,
            PlateArmorConstructionMaterial.COMBINED),
    FLUX("flux", PlateArmorTier.IV, PlateArmorWeight.LIGHT,
            PlateArmorConstructionMaterial.UHMWPE),
    VULKAN_5_HEAVY("vulkan_5_heavy", PlateArmorTier.IV, PlateArmorWeight.HEAVY,
            PlateArmorConstructionMaterial.TITANIUM),

    ALTYN_HEAVY("altyn_heavy", PlateArmorTier.V, PlateArmorWeight.HEAVY,
            PlateArmorConstructionMaterial.TITANIUM),
    FAST_HEAVY_PROTECTION_KIT("fast_heavy_protection_kit", PlateArmorTier.V,
            PlateArmorWeight.HEAVY, PlateArmorConstructionMaterial.COMBINED),

    MASKA_1SCH_HEAVY("maska_1sch_heavy", PlateArmorTier.VI,
            PlateArmorWeight.HEAVY, PlateArmorConstructionMaterial.TITANIUM);

    private final String id;
    private final PlateArmorTier tier;
    private final PlateArmorWeight weight;
    private final PlateArmorConstructionMaterial material;

    HelmetVariant(String id, PlateArmorTier tier, PlateArmorWeight weight,
                  PlateArmorConstructionMaterial material) {
        this.id = id;
        this.tier = tier;
        this.weight = weight;
        this.material = material;
    }

    public String id() {
        return id;
    }

    public String itemId() {
        return "helmet_" + id;
    }

    public PlateArmorTier tier() {
        return tier;
    }

    public PlateArmorWeight weight() {
        return weight;
    }

    public PlateArmorConstructionMaterial material() {
        return material;
    }

    public boolean usesGeoModel() {
        return true;
    }
}
