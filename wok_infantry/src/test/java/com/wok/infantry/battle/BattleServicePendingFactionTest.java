package com.wok.infantry.battle;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Faction-only roster changes of {@link BattleService} without a server: the administrator's
 * pending assignment never resets a member of the same faction (NET-5), and a member left without
 * a formation in a full locked faction can be released to choose again (NET-8).
 */
class BattleServicePendingFactionTest {
    @Test
    void aPendingAssignmentKeepsAMemberOfTheSameFactionAsItIs() {
        BattleSavedData data = new BattleSavedData();
        BattleSavedData.StoredPlayer legacy = member(data, Faction.BLUE, "default");
        BattleSavedData.StoredPlayer pending = member(data, Faction.BLUE, null);
        BattleSavedData.StoredPlayer enemy = member(data, Faction.RED, "default");
        BattleSavedData.StoredPlayer removed = member(data, Faction.BLUE, null);
        removed.admitted = false;

        assertTrue(BattleService.keepsPendingAssignment(legacy, Faction.BLUE),
                "an old save's formation, squad and command survive a same-faction assignment");
        assertTrue(BattleService.keepsPendingAssignment(pending, Faction.BLUE));
        assertFalse(BattleService.keepsPendingAssignment(enemy, Faction.BLUE),
                "a move between factions still clears the old faction's state");
        assertFalse(BattleService.keepsPendingAssignment(removed, Faction.BLUE),
                "a removed player is admitted again");
        assertFalse(BattleService.keepsPendingAssignment(null, Faction.BLUE));
        assertFalse(BattleService.keepsPendingAssignment(pending, null));
    }

    @Test
    void aFactionOnlyMemberIsReleasedAndLosesItsCommand() {
        BattleSavedData data = new BattleSavedData();
        BattleSavedData.StoredPlayer pending = member(data, Faction.BLUE, null);
        pending.assignedClassId = "medic";
        data.setCommander(Faction.BLUE, pending.playerId);

        assertTrue(BattleService.releasePendingFactionState(data, pending.playerId,
                Faction.BLUE));

        assertNull(pending.faction);
        assertNull(pending.formationId);
        assertNull(pending.squad);
        assertEquals(BattleRules.DEFAULT_CLASS_ID, pending.assignedClassId);
        assertTrue(pending.admitted, "released members may choose a faction again at once");
        assertNull(data.commander(Faction.BLUE));
    }

    @Test
    void membersWithAFormationOrInAnotherFactionAreNeverReleased() {
        BattleSavedData data = new BattleSavedData();
        BattleSavedData.StoredPlayer deployed = member(data, Faction.BLUE, "mobile");
        BattleSavedData.StoredPlayer otherSide = member(data, Faction.RED, null);
        UUID commander = deployed.playerId;
        data.setCommander(Faction.BLUE, commander);

        assertFalse(BattleService.releasePendingFactionState(data, deployed.playerId,
                Faction.BLUE));
        assertFalse(BattleService.releasePendingFactionState(data, otherSide.playerId,
                Faction.BLUE));
        assertFalse(BattleService.releasePendingFactionState(data, UUID.randomUUID(),
                Faction.BLUE));
        assertFalse(BattleService.releasePendingFactionState(data, null, Faction.BLUE));
        assertFalse(BattleService.releasePendingFactionState(data, otherSide.playerId, null));

        assertEquals(Faction.BLUE, deployed.faction);
        assertEquals("mobile", deployed.formationId);
        assertEquals(Faction.RED, otherSide.faction);
        assertEquals(commander, data.commander(Faction.BLUE));
    }

    private static BattleSavedData.StoredPlayer member(BattleSavedData data, Faction faction,
                                                       String formationId) {
        UUID playerId = UUID.randomUUID();
        BattleSavedData.StoredPlayer player = data.addPlayer(playerId,
                "player-" + playerId.toString().substring(0, 8), 1_000L);
        player.faction = faction;
        player.formationId = formationId;
        return player;
    }
}
