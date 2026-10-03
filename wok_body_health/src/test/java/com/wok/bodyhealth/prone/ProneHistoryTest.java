package com.wok.bodyhealth.prone;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static com.wok.bodyhealth.prone.ProneTestSupport.assertVec;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ProneHistoryTest {
    private static final ProneSample LIVE = sample(-1L);
    private static final Vec3 LIVE_VELOCITY = new Vec3(0.1D, 0.0D, 0.0D);

    @Test
    void rewindTicksRoundLikeTheGunMods() {
        assertEquals(0, ProneRewind.rewindTicks(0));
        assertEquals(0, ProneRewind.rewindTicks(24));
        assertEquals(1, ProneRewind.rewindTicks(25));
        assertEquals(1, ProneRewind.rewindTicks(74));
        assertEquals(2, ProneRewind.rewindTicks(75));
        assertEquals(20, ProneRewind.rewindTicks(1000));
    }

    @Test
    void ringKeepsTheNewestHundred() {
        ProneHistory history = new ProneHistory();
        assertNull(history.latest());
        for (int i = 1; i <= 105; i++) {
            history.push(sample(i));
        }
        assertEquals(ProneHistory.CAPACITY, history.size());
        assertEquals(105L, history.latest().gameTime());
        assertEquals(105L, history.get(0).gameTime());
        assertEquals(6L, history.get(99).gameTime());
        assertThrows(IndexOutOfBoundsException.class, () -> history.get(100));
        assertThrows(IndexOutOfBoundsException.class, () -> history.get(-1));

        history.clear();
        assertEquals(0, history.size());
        assertNull(history.latest());
    }

    @Test
    void selectRewindsByShooterLatency() {
        ProneHistory history = new ProneHistory();
        for (int i = 1; i <= 5; i++) {
            history.push(sample(i));
        }
        RewindPolicy taczDefault = new RewindPolicy(true, 3.0D, 20);

        ProneRewind.Choice full = ProneRewind.select(history, LIVE, LIVE_VELOCITY, 1000, true, taczDefault);
        assertEquals(4, full.index());
        assertSame(history.get(4), full.sample());
        assertVec(history.get(4).velocity(), full.velocity(), 0.0D);

        ProneRewind.Choice capped = ProneRewind.select(history, LIVE, LIVE_VELOCITY, 1000, true,
                new RewindPolicy(true, 3.0D, 3));
        assertEquals(2, capped.index());

        assertEquals(1, ProneRewind.select(history, LIVE, LIVE_VELOCITY, 50, true, taczDefault).index());
        assertEquals(0, ProneRewind.select(history, LIVE, LIVE_VELOCITY, 0, true, taczDefault).index());
    }

    @Test
    void selectUsesTheLiveSampleWithoutCompensation() {
        ProneHistory history = new ProneHistory();
        history.push(sample(1));
        RewindPolicy policy = new RewindPolicy(true, 3.0D, 20);

        assertLive(ProneRewind.select(history, LIVE, LIVE_VELOCITY, -1, true, policy));
        assertLive(ProneRewind.select(history, LIVE, LIVE_VELOCITY, 200, true,
                new RewindPolicy(false, 3.0D, 20)));
        assertLive(ProneRewind.select(history, LIVE, LIVE_VELOCITY, 200, false, policy));
        assertLive(ProneRewind.select(new ProneHistory(), LIVE, LIVE_VELOCITY, 200, true, policy));
        assertLive(ProneRewind.select(null, LIVE, LIVE_VELOCITY, 200, true, policy));
    }

    @Test
    void velocityIsTheTwoTickDisplacement() {
        Vec3 p0 = new Vec3(1.0D, 64.0D, 1.0D);
        Vec3 p1 = new Vec3(1.2D, 64.0D, 1.1D);
        Vec3 p2 = new Vec3(1.5D, 64.1D, 1.1D);

        assertVec(Vec3.ZERO, ProneRewind.velocity2(p0, null, null), 0.0D);
        assertVec(p1.subtract(p0), ProneRewind.velocity2(p1, p0, null), 0.0D);
        assertVec(p2.subtract(p0), ProneRewind.velocity2(p2, p1, p0), 0.0D);
    }

    @Test
    void policyShiftFactorIsOffsetMinusFive() {
        assertEquals(-2.0D, RewindPolicy.SBW.shiftFactor(), 0.0D);
        assertEquals(-5.0D, new RewindPolicy(true, 0.0D, 20).shiftFactor(), 0.0D);
    }

    private static void assertLive(ProneRewind.Choice choice) {
        assertEquals(-1, choice.index());
        assertSame(LIVE, choice.sample());
        assertSame(LIVE_VELOCITY, choice.velocity());
    }

    private static ProneSample sample(long gameTime) {
        ProneTestSupport.SampleBuilder b = new ProneTestSupport.SampleBuilder();
        b.gameTime = gameTime;
        b.x = gameTime * 0.1D;
        return new ProneSample(b.gameTime, b.x, b.y, b.z, gameTime * 0.01D, 0.0D, 0.0D, b.yRot, b.xRot,
                b.bodyYaw, b.mode, b.taaStart, b.taaDuration, b.anchor, b.target, b.heading, b.gun, b.crawl);
    }
}
