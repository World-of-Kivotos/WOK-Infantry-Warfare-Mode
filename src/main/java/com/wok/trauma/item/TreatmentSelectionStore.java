package com.wok.trauma.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Session-scoped server authority for manual medical-kit targets. */
public final class TreatmentSelectionStore {
    private static final Map<UUID, TreatmentSelection> SELECTIONS = new ConcurrentHashMap<>();

    public static TreatmentSelection get(Player player) {
        return SELECTIONS.getOrDefault(player.getUUID(), TreatmentSelection.AUTO);
    }

    public static void set(ServerPlayer player, TreatmentSelection selection) {
        if (selection.isAutomatic()) {
            SELECTIONS.remove(player.getUUID());
        } else {
            SELECTIONS.put(player.getUUID(), selection);
        }
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SELECTIONS.remove(event.getEntity().getUUID());
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        SELECTIONS.clear();
    }

    private TreatmentSelectionStore() {
    }
}
