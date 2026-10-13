package com.wok.vehiclehealth.client;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Keeps local Superb Warfare prediction from outrunning the damaged server turret. */
public final class ClientTurretAuthority {
    private static final long STATE_TIMEOUT_TICKS = 5L;
    private static final Map<Integer, TurretState> STATES = new HashMap<>();

    public static void accept(int entityId, float yaw, float pitch) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        long expiresAt = minecraft.level.getGameTime() + STATE_TIMEOUT_TICKS;
        STATES.put(entityId, new TurretState(yaw, pitch, expiresAt));
        apply(entityId, yaw, pitch, true);
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            STATES.clear();
            return;
        }
        long now = minecraft.level.getGameTime();
        Iterator<Map.Entry<Integer, TurretState>> iterator = STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, TurretState> entry = iterator.next();
            TurretState state = entry.getValue();
            Entity entity = minecraft.level.getEntity(entry.getKey());
            if (!(entity instanceof VehicleEntity vehicle)
                    || vehicle.isRemoved() || state.expiresAt < now) {
                iterator.remove();
                continue;
            }
            vehicle.setTurretYRot(state.yaw);
            vehicle.setTurretXRot(state.pitch);
        }
    }

    private static void apply(int entityId, float yaw, float pitch, boolean preservePrevious) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(entityId);
        if (!(entity instanceof VehicleEntity vehicle)) {
            return;
        }
        if (preservePrevious) {
            vehicle.setTurretYRotO(vehicle.getTurretYRot());
            vehicle.setTurretXRotO(vehicle.getTurretXRot());
        }
        vehicle.setTurretYRot(yaw);
        vehicle.setTurretXRot(pitch);
    }

    private record TurretState(float yaw, float pitch, long expiresAt) {
    }

    private ClientTurretAuthority() {
    }
}
