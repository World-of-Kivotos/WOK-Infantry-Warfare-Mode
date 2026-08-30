package com.wok.infantry.formation.vehicle;

import com.wok.infantry.battle.ActionResult;

import java.util.Objects;
import java.util.UUID;

/**
 * Pure server-side decision used by the EntityMountEvent interaction hook. Unmanaged vehicles
 * are deliberately outside this subsystem; managed vehicles fail closed on
 * session/faction/formation drift.
 */
public final class ManagedVehicleAccessPolicy {
    private ManagedVehicleAccessPolicy() {
    }

    public static ActionResult authorize(VehicleOwnership ownership, UUID activeSession,
                                         String actorFactionId, String actorFormationId) {
        if (ownership == null) {
            return ActionResult.ok("非 WOK步战核心载具，不应用编制乘坐限制");
        }
        if (activeSession == null || !ownership.sessionId().equals(activeSession)) {
            return ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具不属于当前战局会话");
        }
        if (actorFactionId == null || actorFactionId.isBlank()) {
            return ActionResult.failure(ActionResult.Code.NOT_ASSIGNED,
                    "玩家尚未选择阵营，不能乘坐编制载具");
        }
        if (!Objects.equals(ownership.factionId(), actorFactionId)) {
            return ActionResult.failure(ActionResult.Code.NOT_SAME_FACTION,
                    "敌方阵营不能乘坐该编制载具");
        }
        if (actorFormationId == null || actorFormationId.isBlank()) {
            return ActionResult.failure(ActionResult.Code.FORMATION_SELECTION_REQUIRED,
                    "玩家尚未选择编制，不能乘坐编制载具");
        }
        if (!Objects.equals(ownership.formationId(), actorFormationId)) {
            return ActionResult.failure(ActionResult.Code.NOT_AUTHORIZED,
                    "玩家不属于该载具的编制");
        }
        return ActionResult.ok("允许乘坐本阵营本编制载具");
    }
}
