package com.wok.downed.registry;

import com.wok.downed.WokDownedMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Sound events of the downed module. The tinnitus loop is registered (unlike the core module's
 * runtime-only tablet cues) because it is a networked sound the client plays on its own behalf;
 * registering it here keeps the id stable for both sides without a protocol of its own.
 */
public final class DownedSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, WokDownedMod.MOD_ID);

    public static final RegistryObject<SoundEvent> TINNITUS = SOUNDS.register("downed.tinnitus",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(WokDownedMod.MOD_ID, "downed.tinnitus")));

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }

    private DownedSounds() {
    }
}
