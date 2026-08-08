package com.wok.trauma.registry;

import com.wok.trauma.WokTraumaMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WokTraumaMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> TRAUMA_TREATMENT = CREATIVE_TABS.register(
            "trauma_treatment",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable(
                            "creativetab.wok_trauma.trauma_treatment"))
                    .icon(() -> ModItems.IFAK.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.CIVILIAN_MEDICAL_KIT.get());
                        output.accept(ModItems.IFAK.get());
                        output.accept(ModItems.AFAK.get());
                        output.accept(ModItems.SALEWA_MEDICAL_KIT.get());
                        output.accept(ModItems.CIVILIAN_BANDAGE.get());
                        output.accept(ModItems.MILITARY_BANDAGE.get());
                        output.accept(ModItems.CAT_TOURNIQUET.get());
                        output.accept(ModItems.CALOK_B.get());
                        output.accept(ModItems.PROPITAL.get());
                        output.accept(ModItems.ETG_CHANGE.get());
                        output.accept(ModItems.MORPHINE.get());
                    })
                    .build());

    public static void register(IEventBus bus) {
        CREATIVE_TABS.register(bus);
    }

    private ModCreativeTabs() {
    }
}
