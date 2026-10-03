package com.wok.commandersupport.client;

import com.wok.commandersupport.WokCommanderSupportMod;
import com.wok.infantry.client.map.TacticalSupportMapPresentation;
import com.wok.infantry.client.map.TacticalSupportMapPresentationRegistry;
import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommanderSupportClientTest {
    @Test
    void registersReconAndJdamWithDifferentMapSemantics() {
        CommanderSupportClient.registerMapPresentations();

        assertEquals(TacticalSupportMapPresentation.INTELLIGENCE,
                TacticalSupportMapPresentationRegistry.presentation(
                        WokCommanderSupportMod.RECON_SATELLITE_ID));
        assertEquals(TacticalSupportMapPresentation.OFFENSIVE,
                TacticalSupportMapPresentationRegistry.presentation(
                        WokCommanderSupportMod.MILLENNIUM_JDAM_ID));
        assertEquals(TacticalSupportMapPresentation.OFFENSIVE,
                TacticalSupportMapPresentationRegistry.presentation(
                        WokCommanderSupportMod.F16C_PAVEWAY_ID));
    }

    @Test
    void onlyPavewayDrawsADesignationPermitInsideItsDangerArea() {
        CommanderSupportClient.registerMapPresentations();

        OptionalDouble paveway = TacticalSupportMapPresentationRegistry.guidanceRadius(
                WokCommanderSupportMod.F16C_PAVEWAY_ID);
        assertTrue(paveway.isPresent());
        assertEquals(64.0D, paveway.getAsDouble());
        assertTrue(paveway.getAsDouble()
                < WokCommanderSupportMod.f16cPavewayDefinition().radius());
        assertFalse(TacticalSupportMapPresentationRegistry.guidanceRadius(
                WokCommanderSupportMod.RECON_SATELLITE_ID).isPresent());
        assertFalse(TacticalSupportMapPresentationRegistry.guidanceRadius(
                WokCommanderSupportMod.MILLENNIUM_JDAM_ID).isPresent());
    }
}
