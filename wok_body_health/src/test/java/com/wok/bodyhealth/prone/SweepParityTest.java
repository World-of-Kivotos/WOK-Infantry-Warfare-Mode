package com.wok.bodyhealth.prone;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Velocity sweep and shift in the segment frame equal TaCZ's {@code expandTowards(v).move(k * v)}. */
final class SweepParityTest {
    @Test
    void sweptSegmentMatchesSweptWorldBox() {
        // 0.3 is not a multiple of 1/64, so exact face landings are left to SegmentClipParityTest.
        SegmentClipParityTest.ParityStats stats =
                SegmentClipParityTest.run(new Random(0xC0FFEEL), new Vec3(0.0D, 0.0D, 0.3D), -2.0D, false);
        assertTrue(stats.hits > 1000, "hits " + stats.hits);
        assertTrue(stats.misses > 1000, "misses " + stats.misses);
    }

    @Test
    void diagonalSweepMatchesToo() {
        SegmentClipParityTest.ParityStats stats =
                SegmentClipParityTest.run(new Random(42L), new Vec3(-0.25D, 0.125D, 0.5D), -2.0D, true);
        assertTrue(stats.hits > 1000, "hits " + stats.hits);
    }
}
