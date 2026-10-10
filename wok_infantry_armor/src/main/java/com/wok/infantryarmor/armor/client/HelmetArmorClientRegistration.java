package com.wok.infantryarmor.armor.client;

import com.wok.infantryarmor.WokInfantryArmorMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 在客户端模型烘焙阶段注册头盔的原生模型层（未改用 GeckoLib 的头盔走这套）。
 * 插板护甲改为烘焙网格后不再需要模型层，见 {@link PlateArmorBakedModel}。
 */
@Mod.EventBusSubscriber(modid = WokInfantryArmorMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class HelmetArmorClientRegistration {

    private HelmetArmorClientRegistration() {
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        HelmetArmorModel.registerLayers(event);
    }
}
