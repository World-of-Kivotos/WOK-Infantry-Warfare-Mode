package com.wok.trauma.compat.tacz;

import com.tacz.guns.api.client.event.BeforeRenderHandEvent;
import com.wok.trauma.client.TremorHandShake;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;

final class TaczClientCompat {
    static void register() {
        MinecraftForge.EVENT_BUS.addListener(
                EventPriority.LOWEST, false, TaczClientCompat::onBeforeRenderHand);
    }

    private static void onBeforeRenderHand(BeforeRenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            TremorHandShake.apply(
                    event.getPoseStack(), minecraft.player, minecraft.getFrameTime());
        }
    }

    private TaczClientCompat() {
    }
}
