package com.wok.infantry.battle;

import java.util.Objects;

/** Stable result codes let packets and commands translate errors without trusting client text. */
public record ActionResult(boolean success, Code code, String message) {
    public ActionResult {
        Objects.requireNonNull(code, "code");
        message = Objects.requireNonNullElse(message, "");
    }

    public static ActionResult ok(String message) {
        return new ActionResult(true, Code.OK, message);
    }

    public static ActionResult failure(Code code, String message) {
        if (code == Code.OK) {
            throw new IllegalArgumentException("A failed action cannot use the OK code");
        }
        return new ActionResult(false, code, message);
    }

    public enum Code {
        OK,
        BATTLE_FULL,
        FACTION_FULL,
        FACTION_SELECTION_REQUIRED,
        FORMATION_SELECTION_REQUIRED,
        FORMATION_NOT_FOUND,
        FORMATION_FULL,
        FORMATION_UNAVAILABLE,
        FORMATION_LOCKED,
        NOT_ASSIGNED,
        ALREADY_IN_SQUAD,
        NOT_IN_SQUAD,
        SQUAD_EXISTS,
        SQUAD_NOT_FOUND,
        SQUAD_FULL,
        SQUAD_REJOIN_COOLDOWN,
        NOT_AUTHORIZED,
        INVALID_TARGET,
        TARGET_NOT_FOUND,
        NOT_SAME_FACTION,
        NOT_SAME_SQUAD,
        COMMANDER_EXISTS,
        NOT_COMMANDER,
        INVALID_CLASS_ID,
        INVALID_CLASS_LIMIT,
        CLASS_LIMIT_REACHED,
        CLASS_REQUIRES_SQUAD,
        CLASS_CHANGE_REQUIRES_DEPLOYMENT,
        SQUAD_CHANGE_REQUIRES_DEPLOYMENT,
        NOT_WAITING_FOR_DEPLOYMENT,
        RESPAWN_COOLDOWN,
        DEPLOYMENT_POINT_REQUIRED,
        INVALID_DEPLOYMENT_POINT,
        LOADOUT_INCOMPLETE,
        INVENTORY_BLOCKED,
        KIT_ALREADY_ISSUED,
        RESUPPLY_COOLDOWN,
        NOT_AT_SUPPLY,
        INVALID_MARKER,
        MARKER_LIMIT_REACHED,
        MARKER_RATE_LIMITED,
        MARKER_NOT_FOUND
    }
}
