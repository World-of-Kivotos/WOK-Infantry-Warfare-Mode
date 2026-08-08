package com.wok.infantryarmor;

import com.wok.infantryarmor.armor.PlateArmorVariant;
import com.wok.infantryarmor.shield.PlasmaShieldVariant;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Creative inventory tab containing the standalone mod's wearable armor. */
public final class ArmorerCreativeTab {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WokInfantryArmorMod.MODID);

    public static final RegistryObject<CreativeModeTab> ARMORER_TAB = TABS.register("armorer",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.wok_infantry_armor"))
                    .icon(() -> new ItemStack(ArmorerItems.plateArmor(PlateArmorVariant.SLICK).get()))
                    .displayItems((params, output) -> {
                        for (PlateArmorVariant variant : PlateArmorVariant.values()) {
                            output.accept(ArmorerItems.plateArmor(variant).get());
                        }
                        for (PlasmaShieldVariant variant : PlasmaShieldVariant.values()) {
                            output.accept(ArmorerItems.plasmaShield(variant).get());
                        }
                    })
                    .build());

    private ArmorerCreativeTab() {
    }

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }
}
