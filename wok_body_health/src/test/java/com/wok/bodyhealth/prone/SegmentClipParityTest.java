package com.wok.bodyhealth.prone;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An axis-aligned segment at yaw 0 without margin or velocity must clip exactly like the
 * equivalent world AABB. Boxes and the boundary rays use multiples of 1/64 so the frame change
 * is exact and "ends exactly on a face" really does.
 */
final class SegmentClipParityTest {
    private static final int RAYS = 10_000;

    @Test
    void segmentClipMatchesWorldClip() {
        ParityStats stats = run(new Random(0x5EED_2026L), Vec3.ZERO, -2.0D, true);
        assertTrue(stats.hits > 1000, "hits " + stats.hits);
        assertTrue(stats.misses > 1000, "misses " + stats.misses);
        assertTrue(stats.startInside > 1000, "starts inside " + stats.startInside);
        assertTrue(stats.endOnFace > 1000, "ends on a face " + stats.endOnFace);
    }

    /** Shared by {@link SweepParityTest}. */
    static ParityStats run(Random random, Vec3 velocity, double shiftFactor, boolean faceRays) {
        ParityStats stats = new ParityStats();
        for (int n = 0; n < RAYS; n++) {
            double px = dyadic(random, -2048, 2048);
            double py = 64.0D + dyadic(random, -64, 64);
            double pz = dyadic(random, -2048, 2048);
            double cf = dyadic(random, -64, 64);
            double cs = dyadic(random, -64, 64);
            double cy = dyadic(random, -64, 64);
            double hf = dyadic(random, 2, 48);
            double hs = dyadic(random, 2, 48);
            double hy = dyadic(random, 2, 48);
            LocalObb local = LocalObb.ofExtents(cf - hf, cf + hf, cs - hs, cs + hs, cy - hy, cy + hy);
            WorldObb seg = ProneGeometry.place(pose(local), px, py, pz, ProneTestSupport.NO_MARGIN)[0];
            // Yaw 0: f -> +Z, s -> -X, y -> +Y.
            AABB world = new AABB(px - cs - hs, py + cy - hy, pz + cf - hf, px - cs + hs, py + cy + hy, pz + cf + hf);

            Vec3 start;
            Vec3 end;
            int kind = n % 4;
            if (kind == 1) {
                start = inside(random, world);
                end = around(random, world);
                stats.startInside++;
            } else if (kind == 2 && faceRays) {
                Vec3[] ray = endOnFace(random, world);
                start = ray[0];
                end = ray[1];
                stats.endOnFace++;
            } else if (kind == 3) {
                start = around(random, world);
                Vec3 through = inside(random, world);
                end = through.add(through.subtract(start).scale(random.nextDouble()));
            } else {
                start = around(random, world);
                end = around(random, world);
            }

            AABB swept = world.expandTowards(velocity).move(velocity.scale(shiftFactor));
            Optional<Vec3> expected = swept.clip(start, end);
            List<ProneSegmentClip.SegmentHit> hits =
                    ProneSegmentClip.clipAll(new WorldObb[]{seg}, start, end, velocity, shiftFactor);
            String context = "ray " + n + " " + start + " -> " + end + " box " + swept;
            if (expected.isPresent()) {
                assertEquals(1, hits.size(), context);
                Vec3 point = hits.get(0).point();
                assertEquals(expected.get().x, point.x, 1.0E-9D, context);
                assertEquals(expected.get().y, point.y, 1.0E-9D, context);
                assertEquals(expected.get().z, point.z, 1.0E-9D, context);
                stats.hits++;
            } else {
                assertEquals(0, hits.size(), context);
                stats.misses++;
            }
        }
        return stats;
    }

    private static Vec3[] endOnFace(Random random, AABB box) {
        int face = random.nextInt(6);
        double x = between(random, box.minX, box.maxX);
        double y = between(random, box.minY, box.maxY);
        double z = between(random, box.minZ, box.maxZ);
        double out = dyadic(random, 1, 256);
        double sx = x + dyadic(random, -64, 64);
        double sy = y + dyadic(random, -64, 64);
        double sz = z + dyadic(random, -64, 64);
        switch (face) {
            case 0 -> { x = box.minX; sx = x - out; }
            case 1 -> { x = box.maxX; sx = x + out; }
            case 2 -> { y = box.minY; sy = y - out; }
            case 3 -> { y = box.maxY; sy = y + out; }
            case 4 -> { z = box.minZ; sz = z - out; }
            default -> { z = box.maxZ; sz = z + out; }
        }
        return new Vec3[]{new Vec3(sx, sy, sz), new Vec3(x, y, z)};
    }

    /** A multiple of 1/64 strictly inside (min, max), both of which are multiples of 1/64. */
    private static double between(Random random, double min, double max) {
        int steps = (int) Math.round((max - min) * 64.0D);
        return min + (1 + random.nextInt(Math.max(1, steps - 1))) / 64.0D;
    }

    private static Vec3 inside(Random random, AABB box) {
        return new Vec3(lerp(box.minX, box.maxX, 0.02D + 0.96D * random.nextDouble()),
                lerp(box.minY, box.maxY, 0.02D + 0.96D * random.nextDouble()),
                lerp(box.minZ, box.maxZ, 0.02D + 0.96D * random.nextDouble()));
    }

    private static Vec3 around(Random random, AABB box) {
        AABB area = box.inflate(1.5D);
        return new Vec3(lerp(area.minX, area.maxX, random.nextDouble()),
                lerp(area.minY, area.maxY, random.nextDouble()),
                lerp(area.minZ, area.maxZ, random.nextDouble()));
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double dyadic(Random random, int minSteps, int maxSteps) {
        return (minSteps + random.nextInt(maxSteps - minSteps + 1)) / 64.0D;
    }

    private static ProneLayouts.BodyPose pose(LocalObb box) {
        LocalObb[] all = new LocalObb[SegmentId.values().length];
        Arrays.fill(all, box);
        return new ProneLayouts.BodyPose(0.0F, new BodyLayout(all));
    }

    static final class ParityStats {
        int hits;
        int misses;
        int startInside;
        int endOnFace;
    }
}
