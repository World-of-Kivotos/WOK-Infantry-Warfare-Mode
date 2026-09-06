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
                access = new Access(serviceClass.getMethod("get", ServerPlayer.class),
                        serviceClass.getMethod("factionOf", UUID.class),
                        factionClass.getMethod("id"));
            } catch (ReflectiveOperationException ignored) {
                access = null;
            }
            probed = true;
            return access;
        }
    }

    private record Access(Method getService, Method factionOf, Method factionId) {
    }
}
