package com.wok.infantry.network.battle.client;

import com.wok.infantry.ammo.AmmoSupplyView;
import com.wok.infantry.client.screen.AmmoSupplyScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class AmmoSupplyClientPacketBridge {
    private AmmoSupplyClientPacketBridge() {
    }

    public static void open(AmmoSupplyView view) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean preferVehicleMode = minecraft.screen instanceof AmmoSupplyScreen screen
                ? screen.vehicleModeSelected()
                : view.target().isLarge()
                && !view.vehicleAmmunition().isEmpty();
        minecraft.setScreen(new AmmoSupplyScreen(view, preferVehicleMode));
    }
}
