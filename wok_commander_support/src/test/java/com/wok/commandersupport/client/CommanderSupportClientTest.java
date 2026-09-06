package com.wok.commandersupport.client;

import com.wok.commandersupport.WokCommanderSupportMod;
import com.wok.infantry.client.map.TacticalSupportMapPresentation;
import com.wok.infantry.client.map.TacticalSupportMapPresentationRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
