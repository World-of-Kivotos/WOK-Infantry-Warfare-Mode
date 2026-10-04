package com.wok.infantry.client.hud;

import com.wok.infantry.battle.MemberState;
import com.wok.infantry.battle.MemberView;
import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.battle.SquadView;
import com.wok.infantry.client.hud.SquadRosterModel.Labels;
import com.wok.infantry.client.hud.SquadRosterModel.Roster;
import com.wok.infantry.client.hud.SquadRosterModel.Row;
import com.wok.infantry.client.hud.SquadRosterModel.StatusStyle;
import com.wok.infantry.client.screen.TacticalBoardTheme;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SquadRosterModelTest {
    private static final Labels ZH = new Labels("队长", "指挥", "倒地", "阵亡", "待部署", "待命", "离线");
    private static final UUID VIEWER = new UUID(0L, 1L);
    private static final Map<String, String> CLASSES = Map.of("assault", "突击兵",
            "medic", "医疗兵", "recon", "侦察兵");

    @Test
    void everyMemberStateHasItsOwnDotAndWord() {
        StatusStyle deployed = SquadRosterModel.style(MemberState.DEPLOYED);
        assertEquals(TacticalBoardTheme.SUCCESS_B, deployed.color());
        assertFalse(deployed.hollow());
        assertFalse(deployed.hasTag(), "a deployed member shows health and class instead");

        StatusStyle downed = SquadRosterModel.style(MemberState.DOWNED);
        assertEquals(TacticalBoardTheme.ACCENT_B, downed.color());
        assertEquals(SquadRosterModel.DOWNED_KEY, downed.tagKey());

        StatusStyle dead = SquadRosterModel.style(MemberState.DEAD);
        assertEquals(TacticalBoardTheme.DANGER_B, dead.color());
        assertEquals(SquadRosterModel.DEAD_KEY, dead.tagKey());

        StatusStyle waiting = SquadRosterModel.style(MemberState.WAITING);
        assertEquals(TacticalBoardTheme.NEUTRAL_B, waiting.color());
        assertTrue(waiting.hollow(), "waiting to deploy is the only hollow dot");
        assertEquals(SquadRosterModel.WAITING_SHORT_KEY, waiting.shortTagKey());

        StatusStyle offline = SquadRosterModel.style(MemberState.OFFLINE);
        assertEquals(TacticalBoardTheme.OFFLINE, offline.color());
        assertEquals(SquadRosterModel.OFFLINE_KEY, offline.tagKey());

        assertEquals(5, List.of(deployed.color(), downed.color(), dead.color(), waiting.color(),
                offline.color()).stream().distinct().count(), "five distinct dot colours");
    }

    @Test
    void namesAreGrayOfflineAndReddishWhenDead() {
        assertEquals(TacticalBoardTheme.OFFLINE, SquadRosterModel.nameColor(MemberState.OFFLINE));
        assertEquals(0xFFB49792, SquadRosterModel.nameColor(MemberState.DEAD));
        assertEquals(TacticalBoardTheme.LIGHT, SquadRosterModel.nameColor(MemberState.DOWNED));
        assertEquals(TacticalBoardTheme.LIGHT, SquadRosterModel.nameColor(MemberState.WAITING));
    }

    @Test
    void countUsesTheSquadsOwnCapacity() {
        SquadView squad = squad(6, member(1, "Hoshino", MemberState.DEPLOYED, 1.0F, true, false),
                member(2, "Shiroko", MemberState.DEPLOYED, 0.5F, false, false));
        Roster roster = build(squad, false);
        assertEquals("2/6", roster.count());
        assertEquals(172 - 4 - HudTestSupport.width("2/6"), roster.countX());
    }

    @Test
    void wideRosterAlignsOneStatusColumn() {
        SquadView squad = squad(8,
                member(1, "Hoshino", MemberState.DEPLOYED, 0.9F, true, true),
                member(2, "Shiroko_Sunaookami_Long", MemberState.DOWNED, 0.3F, false, false),
                member(3, "Nonomi", MemberState.DEAD, -1.0F, false, false),
                member(4, "Ayane", MemberState.WAITING, -1.0F, false, false),
                member(5, "Serika", MemberState.OFFLINE, -1.0F, false, false),
                member(6, "Hina", MemberState.DEPLOYED, 0.2F, false, false));
        Roster roster = build(squad, false);
        assertEquals(172, roster.width());
        int column = 172 - 4 - HudTestSupport.width("突击兵");
        assertEquals(column, roster.columnLeft());
        assertEquals(column - 3, roster.barRight());
        assertEquals(column - 3 - 20, roster.barLeft());

        Row leader = roster.rows().get(0);
        assertEquals("队长·指挥", leader.role(), "both roles of a member who holds both");
        assertTrue(leader.healthBar());
        assertEquals("突击兵", leader.className());
        assertTrue(leader.self(), "the viewer's own row is marked");

        Row downed = roster.rows().get(1);
        assertEquals("倒地", downed.tag());
        assertTrue(downed.nameTruncated(), "long names end in an ellipsis");
        assertTrue(downed.name().endsWith("…"));
        assertTrue(downed.nameX() + HudTestSupport.width(downed.name())
                <= roster.barLeft() - 3, "names stay 3px clear of the status column");
        assertFalse(downed.healthBar(), "a status word replaces the bar");

        assertEquals("阵亡", roster.rows().get(2).tag());
        assertEquals("待部署", roster.rows().get(3).tag(), "the full word fits the wide column");
        assertEquals("离线", roster.rows().get(4).tag());
        assertEquals(List.of(1, 2, 3, 4, 5, 6),
                roster.rows().stream().map(Row::number).toList(), "map numbers in server order");
    }

    @Test
    void narrowRosterUsesSymbolsInitialsAndShortWords() {
        SquadView squad = squad(8,
                member(1, "Hoshino", MemberState.DEPLOYED, 0.9F, true, true),
                member(2, "Ayane", MemberState.WAITING, -1.0F, false, false),
                member(3, "Hina", MemberState.DEPLOYED, 0.6F, false, true));
        Roster roster = build(squad, true);
        assertEquals(116, roster.width());
        assertEquals("★◆", roster.rows().get(0).role());
        assertEquals("◆", roster.rows().get(2).role());
        assertEquals("突", roster.rows().get(0).className(), "class initial");
        assertEquals(12, roster.barRight() - roster.barLeft());
        assertEquals("待命", roster.rows().get(1).tag(), "the short word where 待部署 does not fit");
        assertTrue(roster.rows().get(1).hollow());
    }

    @Test
    void unknownHealthRatioDrawsNoBarButKeepsTheClass() {
        SquadView squad = squad(8, member(1, "Hoshino", MemberState.DEPLOYED, -1.0F, true, false),
                member(2, "Hina", MemberState.DEPLOYED, 0.4F, false, false));
        Roster roster = build(squad, false);
        Row unknown = roster.rows().get(0);
        assertFalse(unknown.healthBar(), "body-health installed: no vanilla bar");
        assertEquals(MemberView.UNKNOWN_HEALTH_RATIO, unknown.healthRatio());
        assertEquals("突击兵", unknown.className());
        assertTrue(roster.rows().get(1).healthBar());
        assertEquals(0.4F, roster.rows().get(1).healthRatio());
    }

    @Test
    void atMostEightRowsAndLongTitlesEndInAnEllipsis() {
        List<MemberView> members = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            members.add(member(i, "M" + i, MemberState.DEPLOYED, 1.0F, false, false));
        }
        SquadView squad = new SquadView(SquadCallsign.ALPHA, members.get(0).playerId(), members, 10);
        Roster roster = SquadRosterModel.build(squad, VIEWER, true,
                "一个非常非常非常非常长的小队名字", m -> CLASSES.get(m.classId()), ZH,
                HudTestSupport::width);
        assertEquals(8, roster.rows().size());
        assertEquals("10/10", roster.count());
        assertTrue(roster.titleTruncated());
        assertTrue(SquadRosterModel.TITLE_X + HudTestSupport.width(roster.title())
                <= roster.countX() - 5, "title keeps 5px from the count");
    }

    @Test
    void roleWordsAndInitialsHandleEdgeCases() {
        assertEquals("", SquadRosterModel.roleWords(false, false, ZH));
        assertEquals("指挥", SquadRosterModel.roleWords(false, true, ZH));
        assertEquals("", SquadRosterModel.firstCodePoint(""));
        assertEquals("𝔸", SquadRosterModel.firstCodePoint("𝔸bc"), "surrogate pairs stay whole");
        assertNotEquals(SquadRosterModel.style(MemberState.WAITING).tagKey(),
                SquadRosterModel.style(MemberState.WAITING).shortTagKey());
    }

    private static Roster build(SquadView squad, boolean narrow) {
        return SquadRosterModel.build(squad, VIEWER, narrow, "阿尔法小队",
                member -> CLASSES.get(member.classId()), ZH, HudTestSupport::width);
    }

    private static SquadView squad(int capacity, MemberView... members) {
        return new SquadView(SquadCallsign.ALPHA, members[0].playerId(), List.of(members), capacity);
    }

    private static MemberView member(long id, String name, MemberState state, float ratio,
                                     boolean leader, boolean commander) {
        boolean online = state != MemberState.OFFLINE;
        boolean alive = state == MemberState.DEPLOYED || state == MemberState.DOWNED;
        return new MemberView(new UUID(0L, id), name, online, alive, 20.0F, 20.0F, leader,
                commander, SquadCallsign.ALPHA, "assault", state, ratio);
    }
}
