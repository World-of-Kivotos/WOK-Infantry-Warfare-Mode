package com.wok.infantry.client.screen;

import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.battle.ClassQuotaView;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.FormationContextView;
import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.battle.SquadView;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * Shared squad wording for the HUD, the squad terminal and the tactical map: call signs, short
 * role tags, member count over the squad's own capacity, class, faction, formation and
 * dimension names. One place, so the roster, the squad page and the map always say the same
 * thing (hud-core-02, squad-21). {@code SquadScreen.callsign/className} delegate here.
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
    /** Full role names ("指挥官" / "小队长" / "队员" / "未入队"). */
    public static final String COMMANDER_KEY = "role.wok_infantry.commander";
    public static final String LEADER_KEY = "role.wok_infantry.squad_leader";
    public static final String MEMBER_KEY = "role.wok_infantry.member";
    public static final String UNASSIGNED_KEY = "role.wok_infantry.unassigned";
    /** Short role name of a plain member ("队员"); leader/commander reuse the HUD tags. */
    public static final String MEMBER_SHORT_KEY = "role.wok_infantry.member_short";
    /** Fallback class when a class id is missing. */
    public static final String FALLBACK_CLASS_ID = "assault";

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

    /**
     * The viewer's single role for status rows and the page identity, commander first:
     * "指挥官", "小队长", "队员", or "未入队" outside a squad.
     */
    public static MutableComponent roleName(boolean inSquad, boolean leader, boolean commander) {
        return Component.translatable(roleKey(inSquad, leader, commander));
    }

    /** {@link #roleName} in its short form: "指挥", "队长", "队员", "未入队". */
    public static MutableComponent roleNameShort(boolean inSquad, boolean leader,
                                                 boolean commander) {
        if (commander) {
            return Component.translatable(COMMANDER_SHORT_KEY);
        }
        if (inSquad && leader) {
            return Component.translatable(LEADER_SHORT_KEY);
        }
        return Component.translatable(inSquad ? MEMBER_SHORT_KEY : UNASSIGNED_KEY);
    }

    static String roleKey(boolean inSquad, boolean leader, boolean commander) {
        if (commander) {
            return COMMANDER_KEY;
        }
        if (!inSquad) {
            return UNASSIGNED_KEY;
        }
        return leader ? LEADER_KEY : MEMBER_KEY;
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
        return memberCount(members, capacity);
    }

    /** "members/capacity" as a translatable label. */
    public static MutableComponent memberCount(int members, int capacity) {
        return Component.translatable(MEMBER_COUNT_KEY, Math.max(0, members),
                Math.max(0, capacity));
    }

    /** Translated default class name ({@code class.wok_infantry.<id>}), falling back to the id. */
    public static MutableComponent className(String classId) {
        String safeId = classId == null || classId.isBlank() ? FALLBACK_CLASS_ID : classId;
        return Component.translatableWithFallback("class.wok_infantry." + safeId, safeId);
    }

    /** Configured class name from the snapshot quotas, else the translated default class name. */
    public static MutableComponent className(BattleSnapshot snapshot, String classId) {
        String configuredName = snapshot == null ? "" : snapshot.classQuotas().stream()
                .filter(quota -> quota.classId().equals(classId))
                .map(ClassQuotaView::displayName)
                .findFirst().orElse("");
        return className(classId, configuredName);
    }

    /** {@code configuredName} when it survives sanitizing, else the translated default name. */
    public static MutableComponent className(String classId, String configuredName) {
        String safeConfiguredName = ClassQuotaView.sanitizeDisplayName(configuredName);
        return safeConfiguredName.isBlank()
                ? className(classId) : Component.literal(safeConfiguredName);
    }

    /**
     * The viewer's public faction name (protocol 20 context), else the battle side's translated
     * name, else "未加入阵营".
     */
    public static MutableComponent factionName(BattleSnapshot snapshot) {
        if (snapshot == null) {
            return Component.translatable("faction.wok_infantry.unassigned");
        }
        String publicName = snapshot.formationContext().factionName();
        if (!publicName.isEmpty()) {
            return Component.literal(publicName);
        }
        return battleSideName(snapshot.faction());
    }

    /** The opposing public faction name, else the opposite battle side's translated name. */
    public static MutableComponent enemyFactionName(BattleSnapshot snapshot) {
        if (snapshot == null || snapshot.faction() == null) {
            return Component.translatable("faction.wok_infantry.unassigned");
        }
        String publicName = snapshot.formationContext().enemyFactionName();
        return publicName.isEmpty() ? battleSideName(snapshot.faction().opposite())
                : Component.literal(publicName);
    }

    /** Translated battle side ("蓝方"/"红方"), or "未加入阵营" for {@code null}. */
    public static MutableComponent battleSideName(Faction faction) {
        return Component.translatable(faction == null ? "faction.wok_infantry.unassigned"
                : "faction.wok_infantry." + faction.id());
    }

    /**
     * The shared formation's public name, or {@code null} while the faction has none (vote not
     * locked) or the server sent no context.
     */
    public static MutableComponent formationName(BattleSnapshot snapshot) {
        FormationContextView context = snapshot == null ? FormationContextView.EMPTY
                : snapshot.formationContext();
        if (!context.hasFormation()) {
            return null;
        }
        return Component.literal(context.formationName().isEmpty()
                ? context.formationId() : context.formationName());
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
