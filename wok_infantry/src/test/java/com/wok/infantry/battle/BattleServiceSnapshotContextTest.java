package com.wok.infantry.battle;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Protocol 20 snapshot fields built by {@link BattleService} without a server: each squad's
 * class limits and the viewer's own kick cooldowns.
 */
class BattleServiceSnapshotContextTest {
    private static final long NOW = 1_000_000L;

    @Test
    void squadClassLimitsKeepFormationOrderCapAtCapacityAndCountHolders() {
        BattleSavedData data = new BattleSavedData();
        BattleSavedData.StoredPlayer leader = member(data, "support");
        BattleSavedData.StoredPlayer first = member(data, "assault");
        BattleSavedData.StoredPlayer second = member(data, "assault");
        BattleSavedData.StoredPlayer legacy = member(data, null);
        LinkedHashMap<String, Integer> limits = new LinkedHashMap<>();
        limits.put("support", 1);
        limits.put("Assault", 12);
        limits.put("bad id!", 3);
        limits.put("medic", 0);
        limits.put("assault", 2);
        limits.put("sniper", null);

        List<ClassLimitView> views = BattleService.squadClassLimitViews(limits, 6,
                List.of(leader, first, second, legacy));

        assertEquals(List.of(new ClassLimitView("support", 1, 1),
                        new ClassLimitView("assault", 6, 3),
                        new ClassLimitView("medic", 0, 0)), views,
                "ids are normalized once, a limit never exceeds the squad capacity, malformed "
                        + "or empty entries are skipped and a missing class counts as default");
        assertEquals(List.of(), BattleService.squadClassLimitViews(null, 6, List.of(leader)));
        assertEquals(List.of(), BattleService.squadClassLimitViews(Map.of(), 6, List.of()));

        LinkedHashMap<String, Integer> many = new LinkedHashMap<>();
        for (int index = 0; index < 80; index++) {
            many.put("class_" + index, 1);
        }
        assertEquals(64, BattleService.squadClassLimitViews(many, 8, List.of()).size(),
                "the list stops at the wire bound");
    }

    @Test
    void onlyTheViewersUnexpiredCooldownsInTheCurrentFormationAreSent() {
        BattleSavedData data = new BattleSavedData();
        BattleSavedData.StoredPlayer viewer = member(data, "assault");
        UUID other = UUID.randomUUID();
        Map<BattleService.SquadKickKey, Long> cooldowns = new LinkedHashMap<>();
        cooldowns.put(key(viewer.playerId, Faction.BLUE, "mobile", SquadCallsign.BRAVO),
                NOW + 30_000L);
        cooldowns.put(key(viewer.playerId, Faction.BLUE, "mobile", SquadCallsign.ALPHA),
                NOW + 5_000L);
        cooldowns.put(key(viewer.playerId, Faction.BLUE, "mobile", SquadCallsign.CHARLIE),
                NOW);
        cooldowns.put(key(viewer.playerId, Faction.BLUE, "other_formation",
                SquadCallsign.DELTA), NOW + 30_000L);
        cooldowns.put(key(viewer.playerId, Faction.RED, "mobile", SquadCallsign.ECHO),
                NOW + 30_000L);
        cooldowns.put(key(other, Faction.BLUE, "mobile", SquadCallsign.DELTA), NOW + 30_000L);
        // a wall clock that stepped back can never push an expiry past the 60 s rule
        cooldowns.put(key(viewer.playerId, Faction.BLUE, "mobile", SquadCallsign.ECHO),
                NOW + 10L * BattleRules.SQUAD_KICK_REJOIN_COOLDOWN_MILLIS);

        List<KickCooldownView> views = BattleService.kickCooldownViews(cooldowns, viewer, NOW);

        assertEquals(List.of(new KickCooldownView(SquadCallsign.ALPHA, NOW + 5_000L),
                new KickCooldownView(SquadCallsign.BRAVO, NOW + 30_000L),
                new KickCooldownView(SquadCallsign.ECHO,
                        NOW + BattleRules.SQUAD_KICK_REJOIN_COOLDOWN_MILLIS)), views);

        viewer.formationId = null;
        assertEquals(List.of(), BattleService.kickCooldownViews(cooldowns, viewer, NOW),
                "no formation, no squads to rejoin");
        assertEquals(List.of(), BattleService.kickCooldownViews(cooldowns, null, NOW));
    }

    private static BattleService.SquadKickKey key(UUID player, Faction faction,
                                                  String formation, SquadCallsign squad) {
        return new BattleService.SquadKickKey(player, faction, formation, squad);
    }

    private static BattleSavedData.StoredPlayer member(BattleSavedData data, String classId) {
        UUID playerId = UUID.randomUUID();
        BattleSavedData.StoredPlayer player = data.addPlayer(playerId,
                "player-" + playerId.toString().substring(0, 8), 1_000L);
        player.faction = Faction.BLUE;
        player.formationId = "mobile";
        player.squad = SquadCallsign.ALPHA;
        player.assignedClassId = classId;
        return player;
    }
}
