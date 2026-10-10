package com.wok.infantryarmor.armor.client;

import com.wok.infantryarmor.WokInfantryArmorMod;
import com.wok.infantryarmor.armor.settings.ArmorItemSettings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * 客户端断线时丢掉上一个服务器同步来的逐件配置。Forge 只在服务端停止时卸载 SERVER 配置，远程断线不会重置；
 * 换到一个没有 wok-infantry-armor-items.toml 的服务器（例如 1.2.0）时，客户端否则会继续显示上一个服务器的覆盖值。
 */
@Mod.EventBusSubscriber(modid = WokInfantryArmorMod.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ArmorItemSettingsClientLifecycle {

    private ArmorItemSettingsClientLifecycle() {
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        // 单人/局域网主机：内置服务端和客户端共用同一份 spec，由服务端停止时的 Unloading 负责清掉，
        // 这里不能抢先把服务端也切到默认值（LoggingOut 可能在内置服务端还没停下时触发）。
        if (ServerLifecycleHooks.getCurrentServer() == null) {
            ArmorItemSettings.onClientDisconnected();
        }
    }
}
