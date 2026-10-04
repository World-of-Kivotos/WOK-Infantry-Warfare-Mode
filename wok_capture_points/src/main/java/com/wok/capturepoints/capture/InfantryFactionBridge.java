package com.wok.capturepoints.capture;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/** Reflection keeps WOK Infantry an optional runtime dependency. */
final class InfantryFactionBridge {
    private static volatile Access access;
    private static volatile boolean probed;

    private InfantryFactionBridge() {
    }

    static boolean isActive(ServerPlayer player) {
        if (!ModList.get().isLoaded("wok_infantry")) return true;
        Access current = access();
        if (current == null) return false;
        try {
            Object value = current.getDeployment.invoke(null, player);
            return value instanceof Optional<?> service && service.isPresent()
                    && Boolean.TRUE.equals(current.isActive.invoke(service.get(), player.getUUID()));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    static CaptureTeam resolve(ServerPlayer player) {
        if (!ModList.get().isLoaded("wok_infantry")) return CaptureTeam.NEUTRAL;
        Access current = access();
        if (current == null) return CaptureTeam.NEUTRAL;
        try {
            Object serviceOptional = current.getService.invoke(null, player);
            if (!(serviceOptional instanceof Optional<?> service) || service.isEmpty()) {
                return CaptureTeam.NEUTRAL;
            }
            Object factionOptional = current.factionOf.invoke(service.get(), player.getUUID());
            if (!(factionOptional instanceof Optional<?> faction) || faction.isEmpty()) {
                return CaptureTeam.NEUTRAL;
            }
            return CaptureTeam.byId(String.valueOf(current.factionId.invoke(faction.get())))
                    .orElse(CaptureTeam.NEUTRAL);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return CaptureTeam.NEUTRAL;
        }
    }

    private static Access access() {
        if (probed) return access;
        synchronized (InfantryFactionBridge.class) {
            if (probed) return access;
            try {
                Class<?> serviceClass = Class.forName("com.wok.infantry.battle.BattleService");
                Class<?> factionClass = Class.forName("com.wok.infantry.battle.Faction");
                Class<?> deploymentClass = Class.forName("com.wok.infantry.deployment.DeploymentService");
                access = new Access(serviceClass.getMethod("get", ServerPlayer.class),
                        serviceClass.getMethod("factionOf", UUID.class),
                        factionClass.getMethod("id"),
                        deploymentClass.getMethod("get", ServerPlayer.class),
                        deploymentClass.getMethod("isActive", UUID.class));
            } catch (ReflectiveOperationException ignored) {
                access = null;
            }
            probed = true;
            return access;
        }
    }

    private record Access(Method getService, Method factionOf, Method factionId,
                          Method getDeployment, Method isActive) {
    }
}
