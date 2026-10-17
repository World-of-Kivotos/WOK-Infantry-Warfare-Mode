package com.wok.infantry.battle;

import com.wok.infantry.network.battle.BattleNetworkLimits;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberStateTest {
    @Test
    void resolveChecksOfflineThenDeadThenWaitingThenDowned() {
        for (boolean entityAlive : new boolean[]{false, true}) {
            for (boolean deployed : new boolean[]{false, true}) {
                for (boolean downed : new boolean[]{false, true}) {
                    assertEquals(MemberState.OFFLINE,
                            MemberState.resolve(false, entityAlive, deployed, downed),
                            "offline wins over every other flag");
                }
            }
        }
        for (boolean deployed : new boolean[]{false, true}) {
            for (boolean downed : new boolean[]{false, true}) {
                assertEquals(MemberState.DEAD,
                        MemberState.resolve(true, false, deployed, downed),
                        "a dead entity is dead even though deployment already left ACTIVE");
            }
        }
        for (boolean downed : new boolean[]{false, true}) {
            assertEquals(MemberState.WAITING, MemberState.resolve(true, true, false, downed),
                    "a living player outside the battle is waiting, never downed");
        }
        assertEquals(MemberState.DOWNED, MemberState.resolve(true, true, true, true));
        assertEquals(MemberState.DEPLOYED, MemberState.resolve(true, true, true, false));
    }

    @Test
    void onlyBodiesInTheBattleHaveVitals() {
        assertTrue(MemberState.DEPLOYED.hasVitals());
        assertTrue(MemberState.DOWNED.hasVitals());
        assertFalse(MemberState.DEAD.hasVitals());
        assertFalse(MemberState.WAITING.hasVitals());
        assertFalse(MemberState.OFFLINE.hasVitals());
    }

    @Test
    void wireIdsRoundTripAndFitTheEnumLimit() {
        for (MemberState state : MemberState.values()) {
            assertEquals(Optional.of(state), MemberState.byId(state.id()));
            assertEquals(Optional.of(state), MemberState.byId(" " + state.id().toUpperCase(Locale.ROOT) + " "));
            assertTrue(state.id().length() <= BattleNetworkLimits.MAX_ENUM_ID_LENGTH);
        }
        assertEquals(Optional.empty(), MemberState.byId(null));
        assertEquals(Optional.empty(), MemberState.byId(""));
        assertEquals(Optional.empty(), MemberState.byId("alive"));
    }

    @Test
    void legacyBooleansMapToOfflineDeployedOrWaiting() {
        assertEquals(MemberState.OFFLINE, MemberState.fromLegacy(false, false));
        assertEquals(MemberState.OFFLINE, MemberState.fromLegacy(false, true));
        assertEquals(MemberState.WAITING, MemberState.fromLegacy(true, false));
        assertEquals(MemberState.DEPLOYED, MemberState.fromLegacy(true, true));
    }

    @Test
    void compatibilityConstructorDerivesStateAndVanillaRatio() {
        UUID id = new UUID(0L, 1L);
        MemberView deployed = new MemberView(id, "A", true, true, 15.0F, 20.0F,
                false, false, SquadCallsign.ALPHA, "assault");
        assertEquals(MemberState.DEPLOYED, deployed.state());
        assertEquals(0.75F, deployed.healthRatio());
        assertTrue(deployed.hasHealthRatio());

        MemberView waiting = new MemberView(id, "A", true, false, 0.0F, 20.0F,
                false, false, SquadCallsign.ALPHA, "assault");
        assertEquals(MemberState.WAITING, waiting.state());
        assertEquals(MemberView.UNKNOWN_HEALTH_RATIO, waiting.healthRatio());
        assertFalse(waiting.hasHealthRatio());

        MemberView offline = new MemberView(id, "A", false, false, 0.0F, 20.0F,
                false, false, SquadCallsign.ALPHA, "assault");
        assertEquals(MemberState.OFFLINE, offline.state());
        assertEquals(MemberView.UNKNOWN_HEALTH_RATIO, offline.healthRatio());

        MemberView overhealed = new MemberView(id, "A", true, true, 30.0F, 20.0F,
                false, false, SquadCallsign.ALPHA, "assault");
        assertEquals(1.0F, overhealed.healthRatio());
    }

    @Test
    void canonicalConstructorNormalizesRatioAndDefaultsMissingState() {
        UUID id = new UUID(0L, 2L);
        MemberView nullState = new MemberView(id, "B", true, true, 20.0F, 20.0F,
                false, false, SquadCallsign.BRAVO, "assault", null, 0.5F);
        assertEquals(MemberState.DEPLOYED, nullState.state());
        assertEquals(0.5F, nullState.healthRatio());

        assertEquals(MemberView.UNKNOWN_HEALTH_RATIO, MemberView.normalizeHealthRatio(Float.NaN));
        assertEquals(MemberView.UNKNOWN_HEALTH_RATIO,
                MemberView.normalizeHealthRatio(Float.POSITIVE_INFINITY));
        assertEquals(MemberView.UNKNOWN_HEALTH_RATIO, MemberView.normalizeHealthRatio(-0.25F));
        assertEquals(1.0F, MemberView.normalizeHealthRatio(4.0F));
        assertEquals(0.0F, MemberView.normalizeHealthRatio(0.0F));

        assertEquals(0.5F, MemberView.vanillaHealthRatio(10.0F, 20.0F));
        assertEquals(0.0F, MemberView.vanillaHealthRatio(-3.0F, 20.0F));
        assertEquals(MemberView.UNKNOWN_HEALTH_RATIO, MemberView.vanillaHealthRatio(10.0F, 0.0F));
        assertEquals(MemberView.UNKNOWN_HEALTH_RATIO,
                MemberView.vanillaHealthRatio(Float.NaN, 20.0F));
    }
}
