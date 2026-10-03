package com.wok.bodyhealth.prone;

import net.minecraft.util.Mth;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TaaPhaseMathTest {
    @Test
    void progressCountsTicksSinceTheStart() {
        assertEquals(8.0D / 17.0D, TaaPhaseMath.progress(108L, 0, 100L, 17), 1.0E-12D);
        assertEquals(7.0D / 17.0D, TaaPhaseMath.progress(108L, 1, 100L, 17), 1.0E-12D);
        // TAA never divides by less than two ticks.
        assertEquals(0.5D, TaaPhaseMath.progress(101L, 0, 100L, 0), 1.0E-12D);
        assertEquals(0.0D, TaaPhaseMath.progress(90L, 0, 100L, 17), 1.0E-12D);
        assertEquals(1.0D, TaaPhaseMath.progress(500L, 0, 100L, 17), 1.0E-12D);
    }

    @Test
    void reorientTurnsAfterAShortHold() {
        assertEquals(0.0F, TaaPhaseMath.reorientYaw(0.0F, 90.0F, 0.0D), 1.0E-4F);
        assertEquals(0.0F, TaaPhaseMath.reorientYaw(0.0F, 90.0F, 0.16D), 1.0E-4F);
        assertEquals(45.0F, TaaPhaseMath.reorientYaw(0.0F, 90.0F, 0.58D), 1.0E-4F);
        assertEquals(90.0F, TaaPhaseMath.reorientYaw(0.0F, 90.0F, 1.0D), 1.0E-4F);
        // The short way round across the seam.
        assertEquals(190.0F, TaaPhaseMath.reorientYaw(170.0F, -170.0F, 1.0D), 1.0E-4F);
        assertEquals(-170.0F, Mth.wrapDegrees(TaaPhaseMath.reorientYaw(170.0F, -170.0F, 1.0D)), 1.0E-4F);
    }

    @Test
    void reorientPoseDecaysByEightyTwoPercent() {
        assertEquals(0.0D, TaaPhaseMath.reorientPoseWeight(0.0D), 1.0E-12D);
        assertEquals(1.0D, TaaPhaseMath.reorientPoseWeight(0.82D), 1.0E-12D);
        assertEquals(1.0D, TaaPhaseMath.reorientPoseWeight(0.95D), 1.0E-12D);
    }

    @Test
    void enterAimScaleRampsFromFortyPercent() {
        assertEquals(0.0D, TaaPhaseMath.enterAimScale(0.4D), 1.0E-12D);
        assertEquals(1.0D, TaaPhaseMath.enterAimScale(1.0D), 1.0E-12D);
        assertEquals(0.0D, TaaPhaseMath.enterAimWeight(0.65D), 1.0E-12D);
        assertEquals(1.0D, TaaPhaseMath.enterAimWeight(1.0D), 1.0E-12D);
        assertEquals(1.0D, TaaPhaseMath.exitAimWeight(0.0D), 1.0E-12D);
        assertEquals(0.0D, TaaPhaseMath.exitAimWeight(0.24D), 1.0E-12D);
    }

    @Test
    void exitYawHandsOverToTheBody() {
        assertEquals(0.0F, TaaPhaseMath.exitYaw(0.0F, 60.0F, 1.0D), 1.0E-4F);
        assertEquals(60.0F, TaaPhaseMath.exitYaw(0.0F, 60.0F, 0.0D), 1.0E-4F);
    }

    @Test
    void headingChasesTheAnchor() {
        assertEquals(0.5654D, TaaPhaseMath.HEADING_ALPHA, 1.0E-4D);
        assertEquals(33.92F, TaaPhaseMath.headingStep(0.0F, 60.0F), 0.01F);
        assertEquals(-33.92F, TaaPhaseMath.headingStep(0.0F, -60.0F), 0.01F);
        assertEquals(60.0F, TaaPhaseMath.headingStep(60.0F, 60.0F), 1.0E-4F);
    }

    @Test
    void smoothstepIsClamped() {
        assertEquals(0.0D, TaaPhaseMath.smoothstep(-1.0D), 1.0E-12D);
        assertEquals(0.5D, TaaPhaseMath.smoothstep(0.5D), 1.0E-12D);
        assertEquals(1.0D, TaaPhaseMath.smoothstep(2.0D), 1.0E-12D);
    }
}
