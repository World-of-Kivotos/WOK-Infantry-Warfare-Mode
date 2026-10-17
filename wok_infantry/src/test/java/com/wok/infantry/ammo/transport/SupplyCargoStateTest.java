package com.wok.infantry.ammo.transport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplyCargoStateTest {
    @Test
    void takingCargoIsFiniteAndNeverUnderflows() {
        SupplyCargoState state = SupplyCargoState.full(2).takeOne().takeOne().takeOne();

        assertEquals(2, state.capacity());
        assertEquals(0, state.remaining());
        assertTrue(state.empty());
    }

    @Test
    void capacityChangesNeverRefillSavedCargo() {
        SupplyCargoState partiallyUsed = new SupplyCargoState(4, 2);

        assertEquals(new SupplyCargoState(8, 2), partiallyUsed.withCapacity(8));
        assertEquals(new SupplyCargoState(1, 1), partiallyUsed.withCapacity(1));
    }

    @Test
    void corruptCountsAreBounded() {
        assertEquals(new SupplyCargoState(64, 64),
                new SupplyCargoState(Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(new SupplyCargoState(4, 0), new SupplyCargoState(4, -20));
    }
}
