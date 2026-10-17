package com.wok.infantry.network.battle.client;

import com.wok.infantry.client.screen.WeaponTuningScreen;
import com.wok.infantry.integration.tacz.WeaponTuning;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public final class WeaponTuningClientPacketBridge {
    private WeaponTuningClientPacketBridge() {
    }

    public static void open(UUID weaponId, Component weaponName, WeaponTuning tuning) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new WeaponTuningScreen(
                minecraft.screen, weaponId, weaponName, tuning));
    }
}
