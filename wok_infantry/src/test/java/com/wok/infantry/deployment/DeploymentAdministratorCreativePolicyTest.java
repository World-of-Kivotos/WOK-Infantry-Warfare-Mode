package com.wok.infantry.deployment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeploymentAdministratorCreativePolicyTest {
    @Test
    void administratorAlreadyInCreativeKeepsCreativeDuringManagedDeployment() {
        assertTrue(AdministratorCreativePolicy.preservesCreative(
                true, true));
    }

    @Test
    void ordinaryPlayersAndOtherModesRemainManaged() {
        assertFalse(AdministratorCreativePolicy.preservesCreative(
                false, true));
        assertFalse(AdministratorCreativePolicy.preservesCreative(
                true, false));
    }
}
