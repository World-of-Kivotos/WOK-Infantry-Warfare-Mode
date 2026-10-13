package com.wok.infantry.battle;

import java.util.Locale;
import java.util.Optional;

/**
 * Server-classified roster state of one squad member, shared by the HUD roster and the squad
 * terminal (battle protocol 19). The wire form is {@link #id()}.
 */
public enum MemberState {
    /** Online, deployed and on their feet. */
    DEPLOYED("deployed"),
    /** Online and deployed, but incapacitated by the optional WOK downed module. */
    DOWNED("downed"),
    /** Online, but the player entity is dead (death screen). */
    DEAD("dead"),
    /** Online and alive, but not deployed into the battle (lobby or respawn wait). */
    WAITING("waiting"),
    /** Not connected; the roster slot is reserved. */
    OFFLINE("offline");

    private final String id;

    MemberState(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** Only members with a body in the battle report a health ratio. */
    public boolean hasVitals() {
        return this == DEPLOYED || this == DOWNED;
    }

    public static Optional<MemberState> byId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        for (MemberState state : values()) {
            if (state.id.equals(normalized)) {
                return Optional.of(state);
            }
        }
        return Optional.empty();
    }

    /**
     * Classifies a member in a fixed order: offline, dead, waiting, downed, deployed.
     *
     * <p>Death is checked before deployment because the deployment service leaves ACTIVE in the
     * same tick the player dies; checking deployment first would report every casualty as
     * waiting and the dead state would never reach the roster.
     *
     * @param online      the player is connected
     * @param entityAlive the connected player entity is alive (ignored when offline)
     * @param deployed    the deployment service reports an ACTIVE life
     * @param downed      the optional downed module reports the player as downed; only consulted
     *                    for living, deployed players
     */
    public static MemberState resolve(boolean online, boolean entityAlive, boolean deployed,
                                      boolean downed) {
        if (!online) {
            return OFFLINE;
        }
        if (!entityAlive) {
            return DEAD;
        }
        if (!deployed) {
            return WAITING;
        }
        return downed ? DOWNED : DEPLOYED;
    }

    /**
     * Best-effort state for views built from the pre-protocol-19 booleans, where {@code alive}
     * already meant "online, deployed and alive". Dead and waiting cannot be told apart, so a
     * non-alive online member is reported as waiting.
     */
    public static MemberState fromLegacy(boolean online, boolean alive) {
        if (!online) {
            return OFFLINE;
        }
        return alive ? DEPLOYED : WAITING;
    }
}
