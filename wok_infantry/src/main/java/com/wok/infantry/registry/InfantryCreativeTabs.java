package com.wok.infantry.registry;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.ammo.transport.SupplyCargoType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Dedicated inventory page for administrator-owned WOK Infantry setup blocks. */
public final class InfantryCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WokInfantryMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> SPECIAL_BLOCKS = TABS.register(
            "special_blocks",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable(
                            "itemGroup.wok_infantry.special_blocks"))
                    .icon(() -> new ItemStack(InfantryItems.VEHICLE_DEPLOYMENT.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(InfantryItems.DEPLOYMENT_BEACON.get());
                        output.accept(InfantryItems.VEHICLE_DEPLOYMENT.get());
                        output.accept(InfantryItems.AMMO_SUPPLY_CRATE.get());
                        output.accept(InfantryItems.MEDIUM_AMMO_SUPPLY_CRATE.get());
                        Item nativeLargeStation = ForgeRegistries.ITEMS.getValue(
                                SupplyCargoType.LARGE.itemId());
                        if (nativeLargeStation != null && nativeLargeStation != Items.AIR) {
                            output.accept(nativeLargeStation);
                        }
                        output.accept(InfantryItems.RALLY_RADIO.get());
                    })
                    .build());

    private InfantryCreativeTabs() {
    }

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }
}
