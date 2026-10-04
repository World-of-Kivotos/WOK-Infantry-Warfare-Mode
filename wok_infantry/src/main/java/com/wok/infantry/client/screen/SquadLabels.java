package com.wok.infantry.client.screen;

import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.battle.SquadView;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * Shared squad wording for the HUD, the squad terminal and the tactical map: call signs, short
 * role tags, member count over the squad's own capacity, class and dimension names. One place,
 * so the roster, the squad page and the map always say the same thing (hud-core-02, squad-21).
 */
public final class SquadLabels {
    /** Squad leader symbol on narrow layouts. */
    public static final String LEADER_SYMBOL = "★";
    /** Faction commander symbol on narrow layouts. */
    public static final String COMMANDER_SYMBOL = "◆";
    /** Joins two role tags held by one member ("队长·指挥"). */
    public static final String ROLE_SEPARATOR = "·";
    public static final String LEADER_SHORT_KEY = "hud.wok_infantry.role.leader_short";
    public static final String COMMANDER_SHORT_KEY = "hud.wok_infantry.role.commander_short";
    public static final String MEMBER_COUNT_KEY = "hud.wok_infantry.squad_count";

    private SquadLabels() {
    }

    /** Full call sign ("阿尔法小队"). */
    public static MutableComponent callsign(SquadCallsign callsign) {
        return Component.translatable(callsignKey(callsign));
    }

    /** Short call sign ("阿尔法"), for narrow rows and tags. */
    public static MutableComponent callsignShort(SquadCallsign callsign) {
        return Component.translatable(callsignKey(callsign) + ".short");
    }

    public static String callsignKey(SquadCallsign callsign) {
        return "squad.wok_infantry." + (callsign == null ? SquadCallsign.ALPHA : callsign).id();
    }

    /**
     * Short role tags of a member: "队长", "指挥", or both joined ("队长·指挥"); {@code null} for
     * a plain member. A member who is squad leader and faction commander shows both.
     */
    public static MutableComponent roleShort(boolean leader, boolean commander) {
        if (!leader && !commander) {
            return null;
        }
        MutableComponent result = leader ? Component.translatable(LEADER_SHORT_KEY)
                : Component.translatable(COMMANDER_SHORT_KEY);
        if (leader && commander) {
            result.append(ROLE_SEPARATOR).append(Component.translatable(COMMANDER_SHORT_KEY));
        }
        return result;
    }

    /** Role symbols for narrow layouts: "★" leader, "◆" commander, "★◆" both, "" none. */
    public static String roleSymbols(boolean leader, boolean commander) {
        return (leader ? LEADER_SYMBOL : "") + (commander ? COMMANDER_SYMBOL : "");
    }

    /** "members/capacity" with the squad's own capacity as denominator. */
    public static String memberCountText(int members, int capacity) {
        return Math.max(0, members) + "/" + Math.max(0, capacity);
    }

    /** Member count of {@code squad} over its own capacity (never the formation-wide maximum). */
    public static MutableComponent memberCount(SquadView squad) {
        int members = squad == null ? 0 : squad.members().size();
        int capacity = squad == null ? 0 : squad.capacity();
        return Component.translatable(MEMBER_COUNT_KEY, members, capacity);
    }

    /** Configured class name from the snapshot quotas, else the translated default class name. */
    public static MutableComponent className(BattleSnapshot snapshot, String classId) {
        return SquadScreen.className(snapshot, classId);
    }

    /** Translated dimension name ({@code dimension.<namespace>.<path>}), falling back to its path. */
    public static MutableComponent dimensionName(ResourceLocation dimension) {
        if (dimension == null) {
            return Component.empty();
        }
        return Component.translatableWithFallback(
                "dimension." + dimension.getNamespace() + "." + dimension.getPath(),
                dimension.getPath());
    }
}
