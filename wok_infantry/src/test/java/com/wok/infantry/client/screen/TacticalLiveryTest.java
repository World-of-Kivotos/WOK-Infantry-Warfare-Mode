package com.wok.infantry.client.screen;

import com.wok.infantry.client.screen.TacticalLivery.Livery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TacticalLiveryTest {
    @AfterEach
    void unpin() {
        TacticalLivery.pinForAcceptance(null);
    }

    @Test
    void aViewerWithoutBattleStateIsNeutral() {
        assertEquals(Livery.NEUTRAL, TacticalLivery.current());
    }

    @Test
    void acceptancePinOverridesAndReleases() {
        TacticalLivery.pinForAcceptance(Livery.CAESAR);
        assertEquals(Livery.CAESAR, TacticalLivery.current());
        TacticalLivery.pinForAcceptance(null);
        assertEquals(Livery.NEUTRAL, TacticalLivery.current());
    }
}
