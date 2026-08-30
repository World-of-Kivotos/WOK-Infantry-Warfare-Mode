package com.wok.infantry.client.screen;

import java.util.Objects;

/** Client-session memory so closing the administrator terminal does not lose edit context. */
final class AdminLoadoutSessionState {
    private static Selection remembered = new Selection("", "", "", "primary");

    private AdminLoadoutSessionState() {
    }

    static synchronized Selection load() {
        return remembered;
    }

    static synchronized void remember(String factionId, String formationId,
                                      String classId, String slotId) {
        remembered = new Selection(factionId, formationId, classId, slotId);
    }

    record Selection(String factionId, String formationId,
                     String classId, String slotId) {
        Selection {
            factionId = Objects.requireNonNullElse(factionId, "");
            formationId = Objects.requireNonNullElse(formationId, "");
            classId = Objects.requireNonNullElse(classId, "");
            slotId = Objects.requireNonNullElse(slotId, "");
        }
    }
}
