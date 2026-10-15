package com.wok.infantry.network.battle.packet.c2s;

import com.wok.infantry.battle.ActionResult;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.network.battle.BattleNetwork;
import com.wok.infantry.network.battle.BattleNetworkLimits;
import com.wok.infantry.network.battle.BattleOpenTarget;
import com.wok.infantry.network.battle.packet.s2c.BattleActionFeedbackPacket;
import com.wok.infantry.network.ServerRequestLimiter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

final class BattleServerPacketSupport {
    private BattleServerPacketSupport() {
    }

    static BattleService serviceFor(ServerPlayer sender) {
        return BattleService.get(sender).orElseGet(() -> {
            BattleNetwork.sendToPlayer(sender, new com.wok.infantry.network.battle.packet.s2c.BattleClearPacket());
            sender.displayClientMessage(Component.literal("Battle service is not running"), false);
            return null;
        });
    }

    static boolean ensurePlayer(BattleService service, ServerPlayer sender) {
        ActionResult result = service.ensurePlayer(sender);
        if (result.success()) {
            return true;
        }
        finish(sender, service, result);
        return false;
    }

    static void finish(ServerPlayer sender, BattleService service, ActionResult result) {
        finish(sender, service, result, result.success());
    }

    static void finish(ServerPlayer sender, BattleService service, ActionResult result,
                       boolean stateChanged) {
        if (result.success()) {
            sendFeedback(sender, result);
            // The actor gets an immediate authoritative result. Faction/global fan-out is
            // coalesced by the fixed one-second server heartbeat instead of multiplying every
            // click into up to 80 independently rebuilt snapshots.
            if (stateChanged && ServerRequestLimiter.allow(sender,
                    ServerRequestLimiter.Kind.SNAPSHOT_RESPONSE)) {
                BattleNetwork.sendSnapshotToPlayer(service, sender, BattleOpenTarget.NONE);
            }
            return;
        }
        showResult(sender, result);
    }

    static void reject(ServerPlayer sender, BattleService service,
                       ActionResult.Code code, String message) {
        finish(sender, service, ActionResult.failure(code, message));
    }

    private static void showResult(ServerPlayer sender, ActionResult result) {
        String message = result.message();
        if (message.isBlank()) {
            message = result.code().name();
        }
        if (message.length() > BattleNetworkLimits.MAX_ACTION_MESSAGE_LENGTH) {
            message = message.substring(0, BattleNetworkLimits.MAX_ACTION_MESSAGE_LENGTH);
        }
        BattleNetwork.sendToPlayer(sender, new BattleActionFeedbackPacket(false, message));
        sender.displayClientMessage(Component.literal(message), false);
    }

    private static void sendFeedback(ServerPlayer sender, ActionResult result) {
        String message = result.message().isBlank() ? result.code().name() : result.message();
        if (message.length() > BattleNetworkLimits.MAX_ACTION_MESSAGE_LENGTH) {
            message = message.substring(0, BattleNetworkLimits.MAX_ACTION_MESSAGE_LENGTH);
        }
        BattleNetwork.sendToPlayer(sender, new BattleActionFeedbackPacket(true, message));
    }
}
