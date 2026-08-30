package com.wok.infantry.client.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AdminLoadoutSessionStateTest {
    @Test
    void lastAdministratorEditContextSurvivesScreenRecreation() {
        AdminLoadoutSessionState.remember("academy", "millennium_seminar_mobile",
                "squad_leader", "primary");

        AdminLoadoutSessionState.Selection restored = AdminLoadoutSessionState.load();

        assertEquals("academy", restored.factionId());
        assertEquals("millennium_seminar_mobile", restored.formationId());
        assertEquals("squad_leader", restored.classId());
        assertEquals("primary", restored.slotId());
    }
}
