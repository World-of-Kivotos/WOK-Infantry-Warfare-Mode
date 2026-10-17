package com.wok.vehiclehealth.balance;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/** Bridges Forge's pre-damage explosion event to SBW's later vehicle hurt calculation. */
public final class VehicleExplosionContexts {
    private static final Map<Level, Map<Integer, PendingExplosion>> PENDING =
            Collections.synchronizedMap(new WeakHashMap<>());

    public static void register(VehicleEntity vehicle,
                                InfantryAntiTankWeapon weapon,
                                Vec3 origin) {
        PENDING.computeIfAbsent(vehicle.level(), ignored -> new HashMap<>())
                .put(vehicle.getId(), new PendingExplosion(
                        weapon, origin, vehicle.level().getGameTime()));
    }

    public static Optional<PendingExplosion> consume(VehicleEntity vehicle) {
        Map<Integer, PendingExplosion> byVehicle = PENDING.get(vehicle.level());
        if (byVehicle == null) {
            return Optional.empty();
        }
        PendingExplosion pending = byVehicle.remove(vehicle.getId());
        if (pending == null
                || Math.abs(vehicle.level().getGameTime() - pending.gameTick()) > 1L) {
            return Optional.empty();
        }
        return Optional.of(pending);
    }

    public record PendingExplosion(InfantryAntiTankWeapon weapon,
                                   Vec3 origin,
                                   long gameTick) {
    }

    private VehicleExplosionContexts() {
    }
}
