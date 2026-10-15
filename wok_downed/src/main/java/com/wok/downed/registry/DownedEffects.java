package com.wok.downed.registry;

import com.wok.downed.WokDownedMod;
import com.wok.downed.effect.DownedEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class DownedEffects {
    private static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, WokDownedMod.MOD_ID);

    public static final RegistryObject<MobEffect> DOWNED =
            EFFECTS.register("downed", DownedEffect::new);

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }

    private DownedEffects() {
    }
}
