package com.wok.infantryarmor;

import com.wok.infantryarmor.armor.PlateArmorConfig;
import com.wok.infantryarmor.shield.PlasmaShieldConfig;
import net.minecraftforge.common.ForgeConfigSpec;

/** Server configuration containing armor and plasma-shield balance values only. */
public final class ArmorerConfig {

    public static final ForgeConfigSpec SPEC;
    public static final PlateArmorConfig PLATE_ARMOR;
    public static final PlasmaShieldConfig PLASMA_SHIELD;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        PLATE_ARMOR = PlateArmorConfig.define(builder);
        PLASMA_SHIELD = PlasmaShieldConfig.define(builder);
        SPEC = builder.build();
    }

    private ArmorerConfig() {
    }
}
