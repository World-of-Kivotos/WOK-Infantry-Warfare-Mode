package com.wok.vehiclehealth.client;

import com.wok.vehiclehealth.module.VehicleModuleMath;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public final class ClientOpticsInterference {
    private static UUID vehicleId;
    private static int remainingTicks;
    private static int totalTicks;
    private static float strength;

    public static void accept(UUID affectedVehicleId, int durationTicks, float effectStrength) {
        vehicleId = affectedVehicleId;
        int duration = Math.max(1, durationTicks);
        remainingTicks = Math.max(remainingTicks, duration);
        totalTicks = Math.max(totalTicks, duration);
        strength = Math.max(strength, VehicleModuleMath.clamp(effectStrength, 0.0F, 1.0F));
    }

    public static void tick() {
        if (remainingTicks > 0) {
            remainingTicks--;
        }
        if (remainingTicks <= 0) {
            clear();
        }
    }

    public static boolean activeForLocalPlayer() {
        Minecraft minecraft = Minecraft.getInstance();
        if (remainingTicks <= 0 || vehicleId == null || minecraft.player == null) {
            return false;
        }
        Entity riding = minecraft.player.getVehicle();
        while (riding != null) {
            if (vehicleId.equals(riding.getUUID())) {
                return true;
            }
            riding = riding.getVehicle();
        }
        return false;
    }

    public static float alpha() {
        if (remainingTicks <= 0 || totalTicks <= 0) {
            return 0.0F;
        }
        float life = remainingTicks / (float) totalTicks;
        return VehicleModuleMath.clamp(strength * (0.25F + 0.75F * life), 0.0F, 1.0F);
    }

    public static int remainingTicks() {
        return remainingTicks;
    }

    private static void clear() {
        vehicleId = null;
        remainingTicks = 0;
        totalTicks = 0;
        strength = 0.0F;
    }

    private ClientOpticsInterference() {
    }
}
