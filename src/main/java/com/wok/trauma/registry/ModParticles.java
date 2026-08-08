package com.wok.trauma.registry;

import com.wok.trauma.WokTraumaMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
    private static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, WokTraumaMod.MOD_ID);

    public static final RegistryObject<SimpleParticleType> BLOOD_SPOT =
            PARTICLES.register("blood_spot", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> BLOOD_POOL =
            PARTICLES.register("blood_pool", () -> new SimpleParticleType(true));

    public static void register(IEventBus bus) {
        PARTICLES.register(bus);
    }

    private ModParticles() {
    }
}
