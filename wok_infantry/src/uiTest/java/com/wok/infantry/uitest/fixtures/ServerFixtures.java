package com.wok.infantry.uitest.fixtures;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Server-side fixture state shared by the live flow and the cases: operator rights the acceptance
 * granted temporarily, so the harness cleanup can take them back (a player who already was an
 * operator keeps the status). Call on the server thread.
 */
public final class ServerFixtures {
    private static volatile boolean temporaryOperator;

    private ServerFixtures() {
    }

    /**
     * Grants operator rights to {@code playerId} unless it already has them; returns
     * {@code OK…} or {@code ERROR…} for {@code UiStep.server}.
     */
    public static String grantOperator(MinecraftServer server, UUID playerId) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            return "ERROR: fixture player is offline";
        }
        if (!server.getPlayerList().isOp(player.getGameProfile())) {
            temporaryOperator = true;
            server.getPlayerList().op(player.getGameProfile());
        }
        return "OK: operator";
    }

    /**
     * Takes back the operator rights a case granted, so the next screens show the ordinary
     * player's view; refuses (ERROR) when the player was an operator before the acceptance, whose
     * status the run never changes. The grant stays recorded, so the harness cleanup reports it.
     */
    public static String revokeTemporaryOperator(MinecraftServer server, UUID playerId) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            return "ERROR: fixture player is offline";
        }
        if (!server.getPlayerList().isOp(player.getGameProfile())) {
            return "OK: not an operator";
        }
        if (!temporaryOperator) {
            return "ERROR: the fixture player is a permanent operator; the player view cannot be"
                    + " captured";
        }
        server.getPlayerList().deop(player.getGameProfile());
        return "OK: operator revoked";
    }

    /** Whether a case granted operator rights that the cleanup must revoke. */
    public static boolean temporaryOperatorGranted() {
        return temporaryOperator;
    }

    /** Forgets the grant after the cleanup revoked it. */
    public static void clearTemporaryOperator() {
        temporaryOperator = false;
    }
}
