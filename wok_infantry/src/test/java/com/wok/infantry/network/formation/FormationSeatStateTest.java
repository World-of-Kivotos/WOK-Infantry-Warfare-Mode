package com.wok.infantry.network.formation;

import com.wok.infantry.battle.Faction;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a faction, vote or assignment request pushes to the faction's other members (NET-1):
 * only a real change; a repeated request answers its sender alone.
 */
class FormationSeatStateTest {
    private static final FormationSeatState PENDING_BLUE =
            new FormationSeatState(Faction.BLUE, "", "");

    @Test
    void repeatingTheSameRequestRefreshesNobody() {
        FormationSeatState again = new FormationSeatState(Faction.BLUE, null, null);

        assertFalse(PENDING_BLUE.changed(again));
        assertFalse(PENDING_BLUE.seatChanged(again));
        assertEquals(Set.of(), PENDING_BLUE.affectedFactions(again),
                "joining the own faction again must not rebuild every teammate's catalog");
        FormationSeatState voted = new FormationSeatState(Faction.BLUE, "", "mobile");
        assertEquals(Set.of(), voted.affectedFactions(voted), "\"投票未改变\" refreshes nobody");
    }

    @Test
    void aFirstJoinRefreshesTheNewFactionOnly() {
        assertTrue(FormationSeatState.NONE.seatChanged(PENDING_BLUE));
        assertEquals(Set.of(Faction.BLUE), FormationSeatState.NONE.affectedFactions(PENDING_BLUE));
    }

    @Test
    void aVoteChangesTheTallyButNotTheSeat() {
        FormationSeatState voted = new FormationSeatState(Faction.BLUE, "", "mobile");

        assertTrue(PENDING_BLUE.changed(voted));
        assertFalse(PENDING_BLUE.seatChanged(voted),
                "a vote alone never pushes the formation or the deployment page");
        assertEquals(Set.of(Faction.BLUE), PENDING_BLUE.affectedFactions(voted));
    }

    @Test
    void aMoveBetweenFactionsRefreshesBothFactions() {
        FormationSeatState red = new FormationSeatState(Faction.RED, "default", "");

        assertTrue(PENDING_BLUE.seatChanged(red));
        assertEquals(Set.of(Faction.BLUE, Faction.RED), PENDING_BLUE.affectedFactions(red));
        assertEquals(Set.of(Faction.BLUE), PENDING_BLUE.affectedFactions(FormationSeatState.NONE),
                "a release refreshes the faction that was left");
    }

    @Test
    void receivingTheLockedFormationIsASeatChange() {
        FormationSeatState locked = new FormationSeatState(Faction.BLUE, "mobile", "");

        assertTrue(PENDING_BLUE.seatChanged(locked));
        assertEquals(Set.of(Faction.BLUE), PENDING_BLUE.affectedFactions(locked));
    }
}
