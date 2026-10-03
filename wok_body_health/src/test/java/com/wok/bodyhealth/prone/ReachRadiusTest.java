package com.wok.bodyhealth.prone;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The broad-phase pre-check (spec 9) may only skip a target whose reach box misses the query, so
 * its radius has to hold the reach box of every pose, margin and sweep the hit test can build.
 */
final class ReachRadiusTest {
    private static final ProneMode[] MODES = {ProneMode.TAA_ENTER, ProneMode.TAA_PRONE, ProneMode.TAA_REORIENT,
            ProneMode.TAA_EXIT, ProneMode.TAA_ASSUMED, ProneMode.VANILLA_CRAWL};
    /** Per-axis velocity ranges: lying still, crawling and far faster than any crawl. */
    private static final double[] SPEEDS = {0.0D, 0.15D, 1.5D};

    @Test
    void radiusHoldsTheReachOfRandomPosesMarginsAndSweeps() {
        ProneSegmentTables t = ProneTestSupport.tables();
        Random random = new Random(41L);
        int touching = 0;
        int skipped = 0;
        for (int n = 0; n < 20_000; n++) {
            ProneHitSettings cfg = new ProneHitSettings(true, margin(random), margin(random), margin(random),
                    random.nextInt(6), random.nextBoolean() ? -0.4D : 0.0D, true, false);
            ProneSample s = sample(random);
            WorldObb[] segs = ProneGeometry.place(ProneLayouts.resolve(s, cfg, t), s.x(), s.y(), s.z(), cfg);
            double speed = SPEEDS[random.nextInt(SPEEDS.length)];
            Vec3 velocity = new Vec3(signed(random, speed), signed(random, speed), signed(random, speed));
            double k = -6.0D + random.nextDouble() * 8.0D;
            AABB reach = ProneSegmentClip.reach(segs, velocity, k);
            double radius = ProneSegmentClip.reachRadius(ProneLayouts.localReach(cfg, t), cfg.maxMargin(), velocity, k);

            assertTrue(reach.minX >= s.x() - radius && reach.maxX <= s.x() + radius
                            && reach.minY >= s.y() - radius && reach.maxY <= s.y() + radius
                            && reach.minZ >= s.z() - radius && reach.maxZ <= s.z() + radius,
                    s.mode() + " reach " + reach + " is not within " + radius + " of the position");

            AABB query = random.nextBoolean() ? near(random, reach) : near(random, new AABB(
                    s.x() - radius, s.y() - radius, s.z() - radius, s.x() + radius, s.y() + radius, s.z() + radius));
            boolean may = ProneSegmentClip.mayReach(query, s.x(), s.y(), s.z(), radius);
            if (reach.intersects(query)) {
                touching++;
                assertTrue(may, s.mode() + " reach " + reach + " touches " + query + " but was ruled out");
            }
            if (!may) {
                skipped++;
            }
        }
        // Both branches must actually be exercised for the check above to mean anything.
        assertTrue(touching > 1_000, "queries touching the reach: " + touching);
        assertTrue(skipped > 1_000, "queries ruled out: " + skipped);
    }

    @Test
    void localReachBoundsEveryTransitionPoseOnTheTableGrid() {
        ProneSegmentTables t = ProneTestSupport.tables();
        ProneHitSettings cfg = ProneHitSettings.DEFAULTS;
        double bound = ProneLayouts.localReach(cfg, t);
        for (boolean gun : new boolean[]{true, false}) {
            for (ProneMode mode : new ProneMode[]{ProneMode.TAA_ENTER, ProneMode.TAA_EXIT}) {
                for (int frame = 0; frame < ProneSegmentTables.FRAME_COUNT; frame++) {
                    for (int aim = -180; aim <= 180; aim += 10) {
                        for (double pitch : ProneSegmentTables.PITCHES) {
                            ProneTestSupport.SampleBuilder b = new ProneTestSupport.SampleBuilder();
                            b.mode = mode;
                            b.gun = gun;
                            b.taaDuration = ProneSegmentTables.FRAME_COUNT - 1;
                            b.taaStart = b.gameTime - frame;
                            b.yRot = aim;
                            b.aim = aim;
                            b.xRot = (float) pitch;
                            ProneLayouts.BodyPose pose = ProneLayouts.resolve(b.build(), cfg, t);
                            assertTrue(pose.layout().reach() <= bound,
                                    mode + " gun=" + gun + " frame " + frame + " aim " + aim + " pitch " + pitch
                                            + ": " + pose.layout().reach() + " > " + bound);
                        }
                    }
                }
            }
        }
    }

    @Test
    void farTargetsAreRuledOutAndEdgesAreNot() {
        AABB query = new AABB(0.0D, 60.0D, 0.0D, 1.0D, 61.0D, 1.0D);
        assertFalse(ProneSegmentClip.mayReach(query, 50.0D, 64.0D, 0.5D, 4.0D));
        assertFalse(ProneSegmentClip.mayReach(query, 0.5D, 66.0D, 0.5D, 4.0D));
        // A cube that only touches the query must still be tested in full.
        assertTrue(ProneSegmentClip.mayReach(query, 5.0D, 60.5D, 0.5D, 4.0D));
        assertTrue(ProneSegmentClip.mayReach(query, 0.5D, 60.5D, 0.5D, 0.0D));
        // A NaN velocity makes the radius NaN; that must fall through to the full test.
        double nan = ProneSegmentClip.reachRadius(2.0D, 0.1D, new Vec3(Double.NaN, 0.0D, 0.0D), -2.0D);
        assertTrue(ProneSegmentClip.mayReach(query, 50.0D, 64.0D, 0.5D, nan));
    }

    private static ProneSample sample(Random random) {
        ProneTestSupport.SampleBuilder b = new ProneTestSupport.SampleBuilder();
        b.mode = MODES[random.nextInt(MODES.length)];
        b.x = signed(random, 30_000.0D);
        b.y = -64.0D + random.nextDouble() * 384.0D;
        b.z = signed(random, 30_000.0D);
        b.yRot = angle(random);
        b.xRot = (float) signed(random, 90.0D);
        b.bodyYaw = angle(random);
        b.anchor = angle(random);
        b.target = angle(random);
        b.heading = angle(random);
        b.aim = angle(random);
        b.taaDuration = 2 + random.nextInt(20);
        b.taaStart = b.gameTime - random.nextInt(b.taaDuration + 4);
        b.gun = random.nextBoolean();
        b.crawl = random.nextFloat();
        return b.build();
    }

    /** A box of up to 2 blocks a side, centred anywhere in {@code around} grown by 2 blocks. */
    private static AABB near(Random random, AABB around) {
        Vec3 centre = new Vec3(
                around.minX - 2.0D + random.nextDouble() * (around.getXsize() + 4.0D),
                around.minY - 2.0D + random.nextDouble() * (around.getYsize() + 4.0D),
                around.minZ - 2.0D + random.nextDouble() * (around.getZsize() + 4.0D));
        return new AABB(centre, centre).inflate(random.nextDouble(), random.nextDouble(), random.nextDouble());
    }

    private static double margin(Random random) {
        return random.nextDouble() * 0.25D;
    }

    private static float angle(Random random) {
        return (float) signed(random, 180.0D);
    }

    private static double signed(Random random, double range) {
        return (random.nextDouble() * 2.0D - 1.0D) * range;
    }
}
