package com.wok.infantry.server;

import com.wok.infantry.battle.ActionResult;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.formation.FactionDefinition;
import com.wok.infantry.formation.FormationCategory;
import com.wok.infantry.formation.FormationClassRule;
import com.wok.infantry.formation.FormationDefinition;
import com.wok.infantry.formation.FormationSquadDefinition;
import com.wok.infantry.formation.vote.FormationVotePhase;
import com.wok.infantry.formation.vote.FormationVotePolicy;
import com.wok.infantry.formation.vote.FormationVoteSnapshot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Routing of {@link FormationService}'s join paths (B11a): the faction choice
 * ({@code selectFaction}), the login inheritance ({@code inheritLockedFormation}) and the
 * administrator assignment ({@code adminAssign}) decide here which battle-service operation runs;
 * the GameTests cover those operations themselves on real server objects.
 */
class FormationServiceRoutingTest {
    private static final FormationDefinition DEFAULT = formation("default", "常规编制", 40);
    private static final FormationDefinition MOBILE = formation("mobile", "机动部队", 30);
    private static final FactionDefinition ACADEMY = new FactionDefinition("academy", "学院军",
            "学院军战场阵营", Faction.BLUE, true, 40, List.of(DEFAULT, MOBILE));

    /** Records which seat operation ran and answers with a preset result. */
    private static final class RecordingSeat implements FormationService.FactionSeat,
            FormationService.AdminSeat {
        private final List<String> calls = new ArrayList<>();
        private final ActionResult answer;

        RecordingSeat(ActionResult answer) {
            this.answer = answer;
        }

        @Override
        public ActionResult joinFaction(Faction side, int factionCapacity) {
            calls.add("faction:" + side.id() + ":" + factionCapacity);
            return answer;
        }

        @Override
        public ActionResult joinShared(Faction side, int factionCapacity, String formationId,
                                       int formationCapacity) {
            calls.add("shared:" + side.id() + ":" + factionCapacity + ":" + formationId + ":"
                    + formationCapacity);
            return answer;
        }

        @Override
        public ActionResult assignFormation(String lockedFormationId) {
            calls.add("assign:" + lockedFormationId);
            return answer;
        }

        @Override
        public ActionResult assignFactionOnly(Faction side, int factionCapacity) {
            calls.add("faction-only:" + side.id() + ":" + factionCapacity);
            return answer;
        }
    }

    // ---- faction choice (selectFaction) -------------------------------------------------------

    @Test
    void beforeTheLockAFactionChoiceOnlyReservesTheFactionSlot() {
        for (FormationVoteSnapshot vote : new FormationVoteSnapshot[]{null,
                vote(FormationVotePhase.NOT_STARTED, ""), vote(FormationVotePhase.OPEN, "")}) {
            RecordingSeat seat = new RecordingSeat(ActionResult.ok(""));

            ActionResult result = FormationService.routeFactionChoice(ACADEMY, vote,
                    formation -> true, seat);

            assertTrue(result.success());
            assertEquals("已加入学院军，等待编制投票", result.message());
            assertEquals(List.of("faction:blue:40"), seat.calls,
                    "no formation before the faction's lock");
        }
    }

    @Test
    void afterTheLockAFactionChoiceJoinsTheLockedFormation() {
        RecordingSeat seat = new RecordingSeat(ActionResult.ok(""));

        ActionResult result = FormationService.routeFactionChoice(ACADEMY,
                vote(FormationVotePhase.LOCKED, "mobile"), formation -> true, seat);

        assertTrue(result.success());
        assertEquals("已加入学院军，本局编制：机动部队", result.message());
        assertEquals(List.of("shared:blue:40:mobile:30"), seat.calls,
                "vote-01: faction maximum and the locked formation's capacity both bound the join");
    }

    @Test
    void anUnusableLockedFormationRefusesTheJoinWithoutTouchingTheRoster() {
        RecordingSeat unavailable = new RecordingSeat(ActionResult.ok(""));
        ActionResult disabled = FormationService.routeFactionChoice(ACADEMY,
                vote(FormationVotePhase.LOCKED, "mobile"),
                formation -> !formation.id().equals("mobile"), unavailable);
        RecordingSeat missing = new RecordingSeat(ActionResult.ok(""));
        ActionResult removed = FormationService.routeFactionChoice(ACADEMY,
                vote(FormationVotePhase.LOCKED, "deleted"), formation -> true, missing);

        assertEquals(ActionResult.Code.FORMATION_UNAVAILABLE, disabled.code());
        assertEquals(ActionResult.Code.FORMATION_UNAVAILABLE, removed.code());
        assertTrue(unavailable.calls.isEmpty());
        assertTrue(missing.calls.isEmpty());
    }

    @Test
    void aFullLockedFactionNamesTheLockedCapacityAndOtherFailuresPassThrough() {
        RecordingSeat full = new RecordingSeat(ActionResult.failure(
                ActionResult.Code.FACTION_FULL, "目标阵营已满"));
        ActionResult refused = FormationService.routeFactionChoice(ACADEMY,
                vote(FormationVotePhase.LOCKED, "mobile"), formation -> true, full);
        RecordingSeat other = new RecordingSeat(ActionResult.failure(
                ActionResult.Code.FORMATION_LOCKED, "本轮不能更换阵营"));
        ActionResult otherSide = FormationService.routeFactionChoice(ACADEMY,
                vote(FormationVotePhase.LOCKED, "mobile"), formation -> true, other);

        assertEquals(ActionResult.Code.FACTION_FULL, refused.code());
        assertEquals("学院军本局已锁定编制“机动部队”，最多 30 人", refused.message());
        assertEquals(ActionResult.Code.FORMATION_LOCKED, otherSide.code());
        assertEquals("本轮不能更换阵营", otherSide.message());
    }

    // ---- login inheritance (inheritLockedFormation) ------------------------------------------

    @Test
    void loginGivesTheLockedFormationOnlyToMembersWithoutOne() {
        RecordingSeat inherits = new RecordingSeat(ActionResult.ok(""));
        Optional<ActionResult> inherited = FormationService.routeInheritance(ACADEMY,
                vote(FormationVotePhase.LOCKED, "mobile"), false, formation -> true, inherits);

        assertTrue(inherited.isPresent() && inherited.get().success());
        assertEquals(List.of("shared:blue:40:mobile:30"), inherits.calls);

        RecordingSeat untouched = new RecordingSeat(ActionResult.ok(""));
        assertTrue(FormationService.routeInheritance(ACADEMY,
                vote(FormationVotePhase.LOCKED, "mobile"), true, formation -> true, untouched)
                .isEmpty(), "a member with a formation is never moved");
        assertTrue(FormationService.routeInheritance(ACADEMY,
                vote(FormationVotePhase.OPEN, ""), false, formation -> true, untouched)
                .isEmpty(), "nothing to inherit before the lock");
        assertTrue(FormationService.routeInheritance(ACADEMY, null, false, formation -> true,
                untouched).isEmpty());
        assertTrue(FormationService.routeInheritance(ACADEMY,
                vote(FormationVotePhase.LOCKED, "mobile"), false, formation -> false, untouched)
                .isEmpty(), "an unusable locked formation is not handed out");
        assertTrue(untouched.calls.isEmpty());
    }

    // ---- administrator assignment (adminAssign, vote-02/03) ----------------------------------

    @Test
    void afterTheLockAdministratorsAssignOnlyTheLockedFormation() {
        RecordingSeat blank = new RecordingSeat(ActionResult.ok("ok"));
        RecordingSeat same = new RecordingSeat(ActionResult.ok("ok"));
        RecordingSeat other = new RecordingSeat(ActionResult.ok("ok"));
        FormationVoteSnapshot locked = vote(FormationVotePhase.LOCKED, "mobile");

        assertTrue(FormationService.routeAdminAssignment(ACADEMY, locked, "", "Alice", blank)
                .success());
        assertTrue(FormationService.routeAdminAssignment(ACADEMY, locked, "MOBILE", "Alice",
                same).success());
        ActionResult refused = FormationService.routeAdminAssignment(ACADEMY, locked,
                "default", "Alice", other);

        assertEquals(List.of("assign:mobile"), blank.calls, "blank = whatever is locked");
        assertEquals(List.of("assign:mobile"), same.calls);
        assertEquals(ActionResult.Code.FORMATION_LOCKED, refused.code());
        assertEquals("学院军本局已锁定编制“机动部队”，只能分配到该编制", refused.message());
        assertTrue(other.calls.isEmpty());
    }

    @Test
    void beforeTheLockAdministratorsAssignOnlyTheFaction() {
        RecordingSeat seat = new RecordingSeat(ActionResult.ok(""));

        ActionResult result = FormationService.routeAdminAssignment(ACADEMY,
                vote(FormationVotePhase.OPEN, ""), "default", "Alice", seat);

        assertTrue(result.success());
        assertEquals("已将 Alice 分配到学院军；本阵营编制尚未锁定，锁定后统一下发",
                result.message());
        assertEquals(List.of("faction-only:blue:40"), seat.calls,
                "nobody is placed into a formation the faction did not vote for");

        RecordingSeat failing = new RecordingSeat(ActionResult.failure(
                ActionResult.Code.FACTION_FULL, "目标阵营已满"));
        assertEquals("目标阵营已满", FormationService.routeAdminAssignment(ACADEMY, null, null,
                "Alice", failing).message());
    }

    @Test
    void anUnknownFormationIsRefusedBeforeAnyAssignment() {
        RecordingSeat seat = new RecordingSeat(ActionResult.ok(""));

        ActionResult result = FormationService.routeAdminAssignment(ACADEMY,
                vote(FormationVotePhase.LOCKED, "mobile"), "armor", "Alice", seat);

        assertEquals(ActionResult.Code.FORMATION_NOT_FOUND, result.code());
        assertTrue(seat.calls.isEmpty());
    }

    // ---- vote refusal (castVote through FormationVotePolicy.voteBlock) -----------------------

    @Test
    void voteRefusalsKeepTheLedgerCodesAndSayWhy() {
        assertNull(FormationService.voteRefusal(FormationVotePolicy.VoteBlock.NONE,
                FormationVotePhase.OPEN, ACADEMY, MOBILE, 12));
        ActionResult notOpen = FormationService.voteRefusal(
                FormationVotePolicy.VoteBlock.NOT_OPEN, FormationVotePhase.NOT_STARTED, ACADEMY,
                MOBILE, 12);
        ActionResult locked = FormationService.voteRefusal(
                FormationVotePolicy.VoteBlock.NOT_OPEN, FormationVotePhase.LOCKED, ACADEMY,
                MOBILE, 12);
        ActionResult shortfall = FormationService.voteRefusal(
                FormationVotePolicy.VoteBlock.CAPACITY_SHORTFALL, FormationVotePhase.OPEN,
                ACADEMY, MOBILE, 31);
        ActionResult change = FormationService.voteRefusal(
                FormationVotePolicy.VoteBlock.CHANGE_NOT_ALLOWED, FormationVotePhase.OPEN,
                ACADEMY, MOBILE, 12);
        ActionResult candidate = FormationService.voteRefusal(
                FormationVotePolicy.VoteBlock.NOT_CANDIDATE, FormationVotePhase.OPEN, ACADEMY,
                MOBILE, 12);

        assertEquals(ActionResult.Code.FORMATION_LOCKED, notOpen.code());
        assertEquals("该阵营的编制投票尚未开启", notOpen.message());
        assertEquals(ActionResult.Code.FORMATION_LOCKED, locked.code());
        assertEquals("学院军本局编制已锁定，投票已结束", locked.message());
        assertEquals(ActionResult.Code.FORMATION_FULL, shortfall.code());
        assertEquals("“机动部队”最多容纳 30 人，学院军已有 31 人，不能作为共享编制",
                shortfall.message());
        assertEquals(ActionResult.Code.NOT_AUTHORIZED, change.code());
        assertEquals(ActionResult.Code.FORMATION_NOT_FOUND, candidate.code());
        assertFalse(change.success());
    }

    private static FormationVoteSnapshot vote(FormationVotePhase phase, String lockedId) {
        return new FormationVoteSnapshot(Faction.BLUE, 1L, phase, true, lockedId, "",
                List.of("default", "mobile"), Map.of());
    }

    private static FormationDefinition formation(String id, String name, int capacity) {
        return new FormationDefinition(id, name, "", FormationCategory.INFANTRY, true, capacity,
                null, List.of(new FormationClassRule("assault", "突击兵", 8, Map.of())),
                List.of(new FormationSquadDefinition("alpha", "Alpha", 8, Map.of())), List.of());
    }
}
