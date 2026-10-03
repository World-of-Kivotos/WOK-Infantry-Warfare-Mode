package com.wok.commandersupport.recon;

import com.wok.commandersupport.WokCommanderSupportMod;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.support.adapter.SupportSpawnException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReconSatelliteProviderTest {
    @Test
    void scansPublishOnlyWhileTheRequesterStaysInTheAcceptedFaction() {
        assertFalse(ReconSatelliteProvider.requesterLeftMissionFaction(Faction.BLUE,
                Faction.BLUE));
        assertFalse(ReconSatelliteProvider.requesterLeftMissionFaction(Faction.RED,
                Faction.RED));
        assertTrue(ReconSatelliteProvider.requesterLeftMissionFaction(Faction.BLUE,
                Faction.RED));
        assertTrue(ReconSatelliteProvider.requesterLeftMissionFaction(Faction.RED,
                Faction.BLUE));
        // Losing the assignment also leaves the faction that owns the intel.
        assertTrue(ReconSatelliteProvider.requesterLeftMissionFaction(Faction.BLUE, null));
        assertTrue(ReconSatelliteProvider.requesterLeftMissionFaction(null, Faction.BLUE));
    }

    @Test
    void missingContextEndsTheMissionWithoutTrippingTheCircuit() {
        ReconSatelliteProvider provider = new ReconSatelliteProvider();
        assertEquals(WokCommanderSupportMod.RECON_SATELLITE_ID, provider.supportId());
        assertTrue(provider.availability().available());

        SupportSpawnException failure = assertThrows(SupportSpawnException.class,
                () -> provider.executeStep(null));
        assertFalse(failure.providerBroken());
        assertFalse(failure.refundCooldown());
    }
}
