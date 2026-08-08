package com.wok.trauma.registry;

import com.wok.trauma.WokTraumaMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, WokTraumaMod.MOD_ID);

    public static final RegistryObject<SoundEvent> CONCUSSION_TINNITUS = SOUNDS.register(
            "concussion_tinnitus",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(
                            WokTraumaMod.MOD_ID, "concussion_tinnitus")));
    public static final RegistryObject<SoundEvent> INJECTOR_PUNCTURE =
            registerSound("injector_puncture");
    public static final RegistryObject<SoundEvent> CANVAS_BAG_OPEN =
            registerSound("canvas_bag_open");
    public static final RegistryObject<SoundEvent> BANDAGE_WRAP =
            registerSound("bandage_wrap");

    private static RegistryObject<SoundEvent> registerSound(String name) {
        return SOUNDS.register(name,
                () -> SoundEvent.createVariableRangeEvent(
                        ResourceLocation.fromNamespaceAndPath(WokTraumaMod.MOD_ID, name)));
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }

    private ModSounds() {
    }
}
