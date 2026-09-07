package com.wok.infantryarmor;

import com.wok.infantryarmor.armor.HelmetVariant;
import com.wok.infantryarmor.armor.PlateArmorVariant;
import com.wok.infantryarmor.armor.item.HelmetItem;
import com.wok.infantryarmor.armor.item.PlateArmorItem;
import com.wok.infantryarmor.shield.PlasmaShieldType;
import com.wok.infantryarmor.shield.PlasmaShieldVariant;
import com.wok.infantryarmor.shield.item.PlasmaShieldItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

/** Registers only wearable armor. Production tables and crafting materials are intentionally absent. */
public final class ArmorerItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, WokInfantryArmorMod.MODID);

    private static final Map<PlateArmorVariant, RegistryObject<Item>> PLATE_ARMORS =
            new EnumMap<>(PlateArmorVariant.class);
    private static final Map<HelmetVariant, RegistryObject<Item>> HELMETS =
            new EnumMap<>(HelmetVariant.class);
    private static final Map<PlasmaShieldVariant, RegistryObject<Item>> PLASMA_SHIELDS =
            new EnumMap<>(PlasmaShieldVariant.class);
    private static final Map<PlasmaShieldType, RegistryObject<Item>> LEGACY_PLASMA_SHIELDS =
            new EnumMap<>(PlasmaShieldType.class);

    static {
        for (PlateArmorVariant variant : PlateArmorVariant.values()) {
            PLATE_ARMORS.put(variant,
                    ITEMS.register(variant.itemId(), () -> new PlateArmorItem(variant)));
        }
        for (HelmetVariant variant : HelmetVariant.values()) {
            HELMETS.put(variant,
                    ITEMS.register(variant.itemId(), () -> new HelmetItem(variant)));
        }
        for (PlasmaShieldVariant variant : PlasmaShieldVariant.values()) {
            PLASMA_SHIELDS.put(variant,
                    ITEMS.register(variant.itemId(), () -> new PlasmaShieldItem(variant)));
        }
        // Preserve the three original shield registry IDs for old worlds and commands.
        for (PlasmaShieldType legacyType : PlasmaShieldType.values()) {
            LEGACY_PLASMA_SHIELDS.put(legacyType,
                    ITEMS.register(legacyType.itemId(), () -> new PlasmaShieldItem(legacyType.variant())));
        }
    }

    private ArmorerItems() {
    }

    public static RegistryObject<Item> plateArmor(PlateArmorVariant variant) {
        return PLATE_ARMORS.get(variant);
    }

    public static RegistryObject<Item> helmet(HelmetVariant variant) {
        return HELMETS.get(variant);
    }

    public static RegistryObject<Item> plasmaShield(PlasmaShieldVariant variant) {
        return PLASMA_SHIELDS.get(variant);
    }

    public static RegistryObject<Item> legacyPlasmaShield(PlasmaShieldType legacyType) {
        return LEGACY_PLASMA_SHIELDS.get(legacyType);
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
