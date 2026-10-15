package com.wok.infantry.integration.tacz;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProneStabilityTest {
    @Test void smoothlyApproachesNativeMultiplierAndMovementLosesStability() {
        float stable = 0;
        assertEquals(1, ProneStability.recoilMultiplier(0.1F, stable), 0.0001);
        for (int tick = 0; tick < 20; tick++) stable = ProneStability.advance(stable, true, false, 40);
        assertEquals(0.55, ProneStability.recoilMultiplier(0.1F, stable), 0.0001);
        for (int tick = 0; tick < 21; tick++) stable = ProneStability.advance(stable, true, false, 40);
        assertEquals(0.1, ProneStability.recoilMultiplier(0.1F, stable), 0.0001);
        stable = ProneStability.advance(stable, true, true, 40);
        assertTrue(ProneStability.recoilMultiplier(0.1F, stable) > 0.1);
        assertEquals(0, ProneStability.advance(stable, false, false, 40));
    }
}
