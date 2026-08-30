package com.wok.infantry.network;

import net.minecraft.server.level.ServerPlayer;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Small server-side cooldown gate for client intent packets that can trigger disk writes or
 * snapshot construction. Rejected requests are deliberately silent to avoid response amplification.
 */
public final class ServerRequestLimiter {
    private static final long ENTRY_TTL_NANOS = 10L * 60L * 1_000_000_000L;
    private static final int MAX_TRACKED_PLAYERS = 4_096;
    private static final Map<UUID, PlayerState> PLAYERS = new HashMap<>();
    private static int requestsUntilCleanup = 256;

    private ServerRequestLimiter() {
    }

    public static synchronized boolean allow(ServerPlayer player, Kind kind) {
        if (player == null || kind == null) {
            return false;
        }
        long now = System.nanoTime();
        if (--requestsUntilCleanup <= 0 || PLAYERS.size() >= MAX_TRACKED_PLAYERS) {
            cleanup(now);
            requestsUntilCleanup = 256;
        }
        UUID playerId = player.getUUID();
        PlayerState state = PLAYERS.get(playerId);
        if (state == null) {
            if (PLAYERS.size() >= MAX_TRACKED_PLAYERS) {
                return false;
            }
            state = new PlayerState();
            PLAYERS.put(playerId, state);
        }
        state.lastTouchedNanos = now;
        long nextAllowed = state.nextAllowedNanos.getOrDefault(kind, 0L);
        if (now < nextAllowed) {
            return false;
        }
        state.nextAllowedNanos.put(kind, now + kind.cooldownNanos);
        return true;
    }

    public static synchronized void forget(UUID playerId) {
        if (playerId != null) {
            PLAYERS.remove(playerId);
        }
    }

    private static void cleanup(long now) {
        Iterator<Map.Entry<UUID, PlayerState>> iterator = PLAYERS.entrySet().iterator();
        while (iterator.hasNext()) {
            PlayerState state = iterator.next().getValue();
            if (now - state.lastTouchedNanos > ENTRY_TTL_NANOS) {
                iterator.remove();
            }
        }
    }

    public enum Kind {
        OPEN_UI(1_000),
        SNAPSHOT(1_000),
        // All action/silent-refresh responses share one actor-side rebuild per second; the fixed
        // heartbeat still supplies fresh state to every participant without request amplification.
        SNAPSHOT_RESPONSE(1_000),
        FORMATION_SELECTION(500),
        SQUAD_ACTION(250),
        CLASS_SELECTION(500),
        DEPLOYMENT_SELECTION(200),
        DEPLOYMENT_ACTION(750),
        MAP_MARKER(200),
        SUPPORT_ACTION(750),
        LOADOUT_SAVE(750),
        ADMIN_MUTATION(500),
        CONTAINER_RESYNC(1_000);

        private final long cooldownNanos;

        Kind(long cooldownMillis) {
            cooldownNanos = cooldownMillis * 1_000_000L;
        }
    }

    private static final class PlayerState {
        private final EnumMap<Kind, Long> nextAllowedNanos = new EnumMap<>(Kind.class);
        private long lastTouchedNanos;
    }
}
