package com.wok.commandersupport.registry;

import com.wok.commandersupport.WokCommanderSupportMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CommanderSupportSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,
                    WokCommanderSupportMod.MOD_ID);

    public static final RegistryObject<SoundEvent> JDAM_F15_APPROACH = register(
            "jdam_f15_approach", 256.0F);
    public static final RegistryObject<SoundEvent> JDAM_BOMB_TAIL = register(
            "jdam_bomb_tail", 256.0F);

    private CommanderSupportSounds() {
    }

    private static RegistryObject<SoundEvent> register(String path, float range) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                WokCommanderSupportMod.MOD_ID, path);
        return SOUND_EVENTS.register(path,
                () -> SoundEvent.createFixedRangeEvent(id, range));
    }
}
