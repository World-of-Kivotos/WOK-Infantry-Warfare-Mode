package com.wok.infantry.battle;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleSavedDataFormationTest {
    @Test
    void sameCallsignInTwoFormationsKeepsIndependentLeadersAndCapacity() {
        BattleSavedData original = new BattleSavedData();
        UUID academyLeader = null;
        UUID guardsLeader = null;
        long joinedAt = Math.max(1L, System.currentTimeMillis() - 10_000L);
        for (int index = 0; index < BattleRules.SQUAD_CAPACITY + 1; index++) {
            UUID playerId = id("academy-" + index);
            addSquadMember(original, playerId, "academy", joinedAt + index);
            if (academyLeader == null) {
                academyLeader = playerId;
            }
        }
        for (int index = 0; index < BattleRules.SQUAD_CAPACITY; index++) {
            UUID playerId = id("guards-" + index);
            addSquadMember(original, playerId, "guards", joinedAt + 100 + index);
            if (guardsLeader == null) {
                guardsLeader = playerId;
            }
        }
        original.setLeader(Faction.BLUE, "academy", SquadCallsign.ALPHA, academyLeader);
        original.setLeader(Faction.BLUE, "guards", SquadCallsign.ALPHA, guardsLeader);

        BattleSavedData decoded = BattleSavedData.load(original.save(new CompoundTag()));

        assertEquals(BattleRules.SQUAD_CAPACITY,
                countMembers(decoded, "academy", SquadCallsign.ALPHA));
        assertEquals(BattleRules.SQUAD_CAPACITY,
                countMembers(decoded, "guards", SquadCallsign.ALPHA));
        assertEquals(academyLeader,
                decoded.leader(Faction.BLUE, "academy", SquadCallsign.ALPHA));
        assertEquals(guardsLeader,
                decoded.leader(Faction.BLUE, "guards", SquadCallsign.ALPHA));
        assertNull(decoded.leader(Faction.BLUE, SquadCallsign.ALPHA),
                "legacy two-part lookup must fail closed when formations are ambiguous");

        CompoundTag encoded = decoded.save(new CompoundTag());
        assertEquals(6, encoded.getInt("Version"));
        ListTag leaders = encoded.getList("Leaders", Tag.TAG_COMPOUND);
        assertEquals(2, leaders.size());
        assertTrue(leaders.stream().map(CompoundTag.class::cast)
                .allMatch(tag -> tag.contains("Formation", Tag.TAG_STRING)));
    }

    @Test
    void factionOnlySelectionSurvivesSaveWhileVotingAndCannotKeepSquadState() {
        BattleSavedData original = new BattleSavedData();
        long now = Math.max(1L, System.currentTimeMillis() - 1_000L);
        UUID playerId = id("faction-only-voter");
        BattleSavedData.StoredPlayer player = original.addPlayer(playerId, "Voter", now);
        player.faction = Faction.BLUE;
        player.formationId = null;
        player.squad = SquadCallsign.ALPHA;
        player.squadJoinedAtMillis = now;

        BattleSavedData decoded = BattleSavedData.load(original.save(new CompoundTag()));
        PlayerRecord restored = decoded.player(playerId).view();

        assertEquals(Faction.BLUE, restored.faction());
        assertEquals("", restored.formationId());
        assertNull(restored.squad());
        assertEquals(BattleRules.DEFAULT_CLASS_ID, restored.assignedClassId());
    }

    @Test
    void legacyLeaderWithoutFormationIsDiscardedAndRepairedFromScopedMembers() {
        BattleSavedData writer = new BattleSavedData();
        long joinedAt = Math.max(1L, System.currentTimeMillis() - 10_000L);
        UUID firstMember = id("legacy-first");
        UUID legacyLeader = id("legacy-selected-leader");
        addSquadMember(writer, firstMember, "academy", joinedAt);
        addSquadMember(writer, legacyLeader, "academy", joinedAt + 1L);
        writer.setLeader(Faction.BLUE, "academy", SquadCallsign.ALPHA, legacyLeader);

        CompoundTag legacyRoot = writer.save(new CompoundTag());
        ListTag leaders = legacyRoot.getList("Leaders", Tag.TAG_COMPOUND);
        leaders.getCompound(0).remove("Formation");

        BattleSavedData decoded = BattleSavedData.load(legacyRoot);

        assertEquals(firstMember,
                decoded.leader(Faction.BLUE, "academy", SquadCallsign.ALPHA),
                "missing formation must not preserve the untrusted legacy leader choice");
        CompoundTag canonical = decoded.save(new CompoundTag());
        ListTag canonicalLeaders = canonical.getList("Leaders", Tag.TAG_COMPOUND);
        assertEquals(1, canonicalLeaders.size());
        assertEquals("academy", canonicalLeaders.getCompound(0).getString("Formation"));
        assertEquals(firstMember, canonicalLeaders.getCompound(0).getUUID("Player"));
    }

    @Test
    void rosterReloadRemovesDeletedSquadsAndShrinksCapacityInStableOrder() {
        BattleSavedData data = new BattleSavedData();
        long joinedAt = 10_000L;
        UUID earliest = addRosterMember(data, "alpha-earliest", SquadCallsign.ALPHA,
                "assault", joinedAt);
        UUID second = addRosterMember(data, "alpha-second", SquadCallsign.ALPHA,
                "assault", joinedAt + 1L);
        UUID third = addRosterMember(data, "alpha-third", SquadCallsign.ALPHA,
                "assault", joinedAt + 2L);
        UUID leader = addRosterMember(data, "alpha-leader", SquadCallsign.ALPHA,
                "assault", joinedAt + 3L);
        UUID deletedA = addRosterMember(data, "bravo-a", SquadCallsign.BRAVO,
                "assault", joinedAt);
        UUID deletedB = addRosterMember(data, "bravo-b", SquadCallsign.BRAVO,
                "assault", joinedAt + 1L);
        data.setLeader(Faction.BLUE, "academy", SquadCallsign.ALPHA, leader);
        data.setLeader(Faction.BLUE, "academy", SquadCallsign.BRAVO, deletedA);

        List<UUID> affected = BattleService.reconcileFormationRosterState(data,
                (faction, formationId, callsign) -> callsign == SquadCallsign.ALPHA
                        ? Optional.of(new BattleService.FormationRosterRule(2,
                        Map.of("assault", 2)))
                        : Optional.empty());

        assertEquals(List.of(second, third, deletedA, deletedB), affected);
        assertEquals(SquadCallsign.ALPHA, data.player(leader).squad,
                "the current leader is retained before earlier ordinary members");
        assertEquals(SquadCallsign.ALPHA, data.player(earliest).squad);
        assertNull(data.player(second).squad);
        assertNull(data.player(third).squad);
        assertNull(data.player(deletedA).squad);
        assertNull(data.player(deletedB).squad);
        assertEquals(leader, data.leader(Faction.BLUE, "academy", SquadCallsign.ALPHA));
        assertNull(data.leader(Faction.BLUE, "academy", SquadCallsign.BRAVO));
    }

    @Test
    void rosterReloadKeepsExistingClassesThenReassignsOrEvictsDeterministically() {
        BattleSavedData data = new BattleSavedData();
        long joinedAt = 20_000L;
        UUID leader = addRosterMember(data, "class-leader", SquadCallsign.ALPHA,
                "support", joinedAt);
        UUID excessSupport = addRosterMember(data, "class-second-support",
                SquadCallsign.ALPHA, "support", joinedAt + 1L);
        UUID deletedClass = addRosterMember(data, "class-deleted", SquadCallsign.ALPHA,
                "medic", joinedAt + 2L);
        UUID rifleman = addRosterMember(data, "class-rifleman", SquadCallsign.ALPHA,
                "rifleman", joinedAt + 3L);
        UUID evicted = addRosterMember(data, "class-evicted", SquadCallsign.ALPHA,
                "medic", joinedAt + 4L);
        data.setLeader(Faction.BLUE, "academy", SquadCallsign.ALPHA, leader);
        LinkedHashMap<String, Integer> limits = new LinkedHashMap<>();
        limits.put("support", 1);
        limits.put("engineer", 1);
        limits.put("assault", 1);
        limits.put("rifleman", 1);

        List<UUID> affected = BattleService.reconcileFormationRosterState(data,
                (faction, formationId, callsign) -> Optional.of(
                        new BattleService.FormationRosterRule(5, limits)));

        assertEquals(List.of(excessSupport, deletedClass, evicted), affected);
        assertEquals("support", data.player(leader).assignedClassId,
                "stable order keeps the leader's existing limited reservation");
        assertEquals("engineer", data.player(excessSupport).assignedClassId,
                "fallback follows the configured profession order without a hard-coded ID");
        assertEquals("assault", data.player(deletedClass).assignedClassId,
                "later displaced members use the first remaining configured class");
        assertEquals("rifleman", data.player(rifleman).assignedClassId);
        assertNull(data.player(evicted).squad,
                "a member is removed when aggregate class capacity is below squad size");
        assertEquals("support", data.player(evicted).assignedClassId,
                "an evicted member returns to the formation's first/default profession");
        assertEquals(leader, data.leader(Faction.BLUE, "academy", SquadCallsign.ALPHA));
    }

    @Test
    void privateLoadoutBackingReplacementKeepsOnlyTargetFormationPlayersOnTheirRole() {
        BattleSavedData data = new BattleSavedData();
        UUID targetSupport = addRosterMember(data, "target-support", SquadCallsign.ALPHA,
                "support", 30_000L);
        UUID targetAssault = addRosterMember(data, "target-assault", SquadCallsign.ALPHA,
                "assault", 30_001L);
        UUID otherFormationSupport = id("other-formation-support");
        addSquadMember(data, otherFormationSupport, "guards", 30_002L);
        data.player(otherFormationSupport).assignedClassId = "support";

        List<UUID> affected = BattleService.remapFormationClassState(data, Faction.BLUE,
                "academy", "support", "copy_support");

        assertEquals(List.of(targetSupport), affected);
        assertEquals("copy_support", data.player(targetSupport).assignedClassId);
        assertEquals("assault", data.player(targetAssault).assignedClassId);
        assertEquals("support", data.player(otherFormationSupport).assignedClassId);
    }

    private static void addSquadMember(BattleSavedData data, UUID playerId,
                                       String formationId, long joinedAt) {
        BattleSavedData.StoredPlayer player = data.addPlayer(playerId, playerId.toString(), joinedAt);
        player.faction = Faction.BLUE;
        player.formationId = formationId;
        player.squad = SquadCallsign.ALPHA;
        player.squadJoinedAtMillis = joinedAt;
    }

    private static UUID addRosterMember(BattleSavedData data, String seed,
                                        SquadCallsign squad, String classId, long joinedAt) {
        UUID playerId = id(seed);
        addSquadMember(data, playerId, "academy", joinedAt);
        BattleSavedData.StoredPlayer player = data.player(playerId);
        player.squad = squad;
        player.assignedClassId = classId;
        return playerId;
    }

    private static long countMembers(BattleSavedData data, String formationId,
                                     SquadCallsign squad) {
        return data.players().stream()
                .filter(player -> player.faction == Faction.BLUE)
                .filter(player -> formationId.equals(player.formationId))
                .filter(player -> player.squad == squad)
                .count();
    }

    private static UUID id(String seed) {
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}
