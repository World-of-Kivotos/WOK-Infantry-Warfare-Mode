package com.wok.infantry.client;

import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Catalog request timeout of the formation vote page's waiting state (B11a). */
class ClientFormationStateTest {
    private static final long TIMEOUT = ClientFormationState.CATALOG_TIMEOUT_NANOS;

    @AfterEach
    void clear() {
        ClientFormationState.clear();
    }

    @Test
    void aRequestIsOverdueOnlyAfterTheTimeout() {
        long sent = 5_000_000_000L;

        assertFalse(ClientFormationState.catalogOverdue(0L, sent + TIMEOUT * 10),
                "no request pending, nothing is overdue");
        assertFalse(ClientFormationState.catalogOverdue(sent, sent));
        assertFalse(ClientFormationState.catalogOverdue(sent, sent + TIMEOUT - 1));
        assertTrue(ClientFormationState.catalogOverdue(sent, sent + TIMEOUT));
    }

    @Test
    void anArrivingCatalogAnswersTheRequest() {
        ClientFormationState.catalogRequested();
        assertFalse(ClientFormationState.catalogOverdue(), "just sent");

        ClientFormationState.update(new FormationSelectionSnapshot(1L, false, "", "",
                List.of()));

        assertFalse(ClientFormationState.catalogOverdue(),
                "an arrived catalog leaves no request pending");
    }
}
