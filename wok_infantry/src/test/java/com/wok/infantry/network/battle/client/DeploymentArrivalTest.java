package com.wok.infantry.network.battle.client;

import org.junit.jupiter.api.Test;

import static com.wok.infantry.network.battle.client.BattleClientNetworkBridge.DeploymentArrival;
import static com.wok.infantry.network.battle.client.BattleClientNetworkBridge.deploymentArrival;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** text-state-08: a deployment request never opens a second terminal on top of the first. */
class DeploymentArrivalTest {
    @Test
    void anOpenTerminalSwitchesToItsDeploymentPage() {
        assertEquals(DeploymentArrival.SWITCH_PAGE, deploymentArrival(true, false, false));
    }

    @Test
    void loginAndRespawnStillOpenTheDeploymentPage() {
        assertEquals(DeploymentArrival.OPEN, deploymentArrival(false, false, false));
    }

    @Test
    void nothingHappensOnceDeployedOrWhileTypingOrInAnInventory() {
        assertEquals(DeploymentArrival.IGNORE, deploymentArrival(true, true, false));
        assertEquals(DeploymentArrival.IGNORE, deploymentArrival(false, true, false));
        assertEquals(DeploymentArrival.IGNORE, deploymentArrival(false, false, true));
    }
}
