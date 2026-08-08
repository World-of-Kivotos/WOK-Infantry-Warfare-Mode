package com.wok.trauma.client;

import com.wok.trauma.WokTraumaMod;
import com.wok.trauma.client.particle.BloodDecalParticle;
import com.wok.trauma.registry.ModParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WokTraumaMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.BLOOD_SPOT.get(),
                sprites -> new BloodDecalParticle.Provider(sprites, 0.18F, 600, 1000));
        event.registerSpriteSet(ModParticles.BLOOD_POOL.get(),
                sprites -> new BloodDecalParticle.Provider(sprites, 0.58F, 900, 1200));
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("pain_blur", PainBlurOverlay.INSTANCE);
    }

    private ClientModEvents() {
    }
}
