package com.wok.commandersupport.recon;

import com.wok.commandersupport.WokCommanderSupportMod;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.support.SupportDefinition;
import com.wok.infantry.support.SupportTargetMode;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReconSatelliteScannerTest {
    @Test
    void specificationUsesTenMinuteCooldownAndThirtySecondScanWindow() {
        assertEquals(12_000L, WokCommanderSupportMod.RECON_COOLDOWN_TICKS);
        assertEquals(6, WokCommanderSupportMod.RECON_SCAN_STEPS);
        assertEquals(100, WokCommanderSupportMod.RECON_SCAN_INTERVAL_TICKS);
        assertEquals(30 * 20,
                WokCommanderSupportMod.RECON_SCAN_STEPS
                        * WokCommanderSupportMod.RECON_SCAN_INTERVAL_TICKS);
        assertEquals(150.0D, WokCommanderSupportMod.RECON_RADIUS);
        SupportDefinition definition = WokCommanderSupportMod.reconSatelliteDefinition();
        assertEquals(WokCommanderSupportMod.RECON_SATELLITE_ID, definition.id());
        assertEquals(SupportTargetMode.POINT, definition.targetMode());
        assertEquals(12_000L, definition.cooldownTicks());
        assertEquals(6, definition.stepCount());
        assertEquals(100, definition.stepIntervalTicks());
        assertEquals(150.0D, definition.radius());
    }

    @Test
    void scanUsesAHorizontalInclusiveRadius() {
        assertTrue(ReconSatelliteScanner.insideRadius(0, 0, 150, 0, 150));
        assertTrue(ReconSatelliteScanner.insideRadius(10, -20, 100, 100, 150));
        assertFalse(ReconSatelliteScanner.insideRadius(0, 0, 150.001D, 0, 150));
        assertFalse(ReconSatelliteScanner.insideRadius(0, 0, 0, 0, Double.NaN));
    }

    @Test
    void contactLeaseBridgesUpdatesButExpiresAtEndOfWindow() {
        assertTrue(ReconSatelliteProvider.CONTACT_TTL_TICKS
                > WokCommanderSupportMod.RECON_SCAN_INTERVAL_TICKS);
        assertTrue(ReconSatelliteProvider.CONTACT_TTL_TICKS
                < WokCommanderSupportMod.RECON_SCAN_INTERVAL_TICKS * 2);
    }

    @Test
    void onlyDeployedLivingEnemyPlayersInsideRadiusAreScanned() {
        assertTrue(ReconSatelliteScanner.shouldScanPlayer(Faction.BLUE, Faction.RED,
                true, true, false, 0, 0, 100, 0, 150));
        assertFalse(ReconSatelliteScanner.shouldScanPlayer(Faction.BLUE, Faction.BLUE,
                true, true, false, 0, 0, 100, 0, 150));
        assertFalse(ReconSatelliteScanner.shouldScanPlayer(Faction.BLUE, Faction.RED,
                false, true, false, 0, 0, 100, 0, 150));
        assertFalse(ReconSatelliteScanner.shouldScanPlayer(Faction.BLUE, Faction.RED,
                true, false, false, 0, 0, 100, 0, 150));
        assertFalse(ReconSatelliteScanner.shouldScanPlayer(Faction.BLUE, Faction.RED,
                true, true, true, 0, 0, 100, 0, 150));
        assertFalse(ReconSatelliteScanner.shouldScanPlayer(Faction.BLUE, Faction.RED,
                true, true, false, 0, 0, 151, 0, 150));
    }

    @Test
    void enemyVehiclesAreScannedAndMountedPlayersCollapseToVehicleIdentity() {
        assertTrue(ReconSatelliteScanner.shouldScanVehicle(Faction.BLUE, Faction.RED,
                0, 0, 149, 0, 150));
        assertFalse(ReconSatelliteScanner.shouldScanVehicle(Faction.BLUE, Faction.BLUE,
                0, 0, 100, 0, 150));
        assertFalse(ReconSatelliteScanner.shouldScanVehicle(Faction.BLUE, Faction.RED,
                0, 0, 151, 0, 150));

        UUID playerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        assertEquals(vehicleId,
                ReconSatelliteScanner.resolvedPlayerContactId(playerId, vehicleId));
        assertEquals(playerId,
                ReconSatelliteScanner.resolvedPlayerContactId(playerId, null));
    }
}
