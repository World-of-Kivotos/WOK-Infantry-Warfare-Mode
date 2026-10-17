package com.wok.infantry.integration.tacz;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class AdsSpeedPolicyTest {
    @Test
    void thirtyPercentSlowerSpeedUsesSeventyPercentScale() {
        assertEquals(0.70F, AdsSpeedPolicy.speedScaleForSlowdown(30), 0.00001F);
    }

    @Test
    void thirtyPercentSlowerSpeedMakesAimTimeOnePointFourThreeTimesLonger() {
        float scale = AdsSpeedPolicy.speedScaleForSlowdown(30);
        assertEquals(0.2857143F, AdsSpeedPolicy.adjustedAimTime(0.20F, scale), 0.00001F);
    }

    @Test
    void rejectsACompleteOrNegativeSpeedReduction() {
        assertThrows(IllegalArgumentException.class,
                () -> AdsSpeedPolicy.speedScaleForSlowdown(100));
        assertThrows(IllegalArgumentException.class,
                () -> AdsSpeedPolicy.speedScaleForSlowdown(-1));
    }
}
