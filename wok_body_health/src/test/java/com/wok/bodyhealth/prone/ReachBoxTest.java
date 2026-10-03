package com.wok.bodyhealth.prone;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReachBoxTest {
    private static final double K = -2.0D;

    @Test
    void reachContainsEverySweptCornerOfAnAngledPose() {
        // Yaw 45 turns every segment off the world axes; the gun pose's left arm is oblique on its own.
        WorldObb[] segs = ProneGeometry.place(new ProneLayouts.BodyPose(45.0F,
                ProneTestSupport.tables().steady(true, 30.0D, -10.0D)), 12.5D, 70.0D, -3.0D, ProneHitSettings.DEFAULTS);
        for (Vec3 velocity : new Vec3[]{new Vec3(0.4D, 0.0D, 0.0D), new Vec3(-0.15D, 0.05D, 0.3D), Vec3.ZERO}) {
            AABB reach = ProneSegmentClip.reach(segs, velocity, K);
            for (WorldObb seg : segs) {
                Vec3 v = seg.toLocalDirection(velocity);
                AABB local = new AABB(-seg.h1(), -seg.h2(), -seg.h3(), seg.h1(), seg.h2(), seg.h3())
                        .expandTowards(v).move(v.scale(K));
                for (int i = 0; i < 8; i++) {
                    Vec3 corner = seg.toWorld(
                            (i & 1) != 0 ? local.maxX : local.minX,
                            (i & 2) != 0 ? local.maxY : local.minY,
                            (i & 4) != 0 ? local.maxZ : local.minZ);
                    assertTrue(within(reach, corner), seg.id() + " corner " + i + " " + corner + " outside " + reach);
                }
            }
        }
    }

    @Test
    void everyHitPointLiesInsideTheReach() {
        WorldObb[] segs = ProneGeometry.place(new ProneLayouts.BodyPose(45.0F,
                ProneTestSupport.tables().steady(true, 0.0D, 0.0D)), 0.0D, 64.0D, 0.0D, ProneHitSettings.DEFAULTS);
        Vec3 velocity = new Vec3(0.4D, 0.0D, 0.0D);
        AABB reach = ProneSegmentClip.reach(segs, velocity, K);
        Random random = new Random(11L);
        int hits = 0;
        for (int n = 0; n < 3000; n++) {
            Vec3 start = new Vec3(random.nextDouble() * 8.0D - 4.0D, 62.0D + random.nextDouble() * 4.0D,
                    random.nextDouble() * 8.0D - 4.0D);
            Vec3 aim = new Vec3(random.nextDouble() * 3.0D - 1.5D, 63.8D + random.nextDouble() * 1.2D,
                    random.nextDouble() * 3.0D - 1.5D);
            for (ProneSegmentClip.SegmentHit hit : ProneSegmentClip.clipAll(segs, start,
                    start.add(aim.subtract(start).scale(2.0D)), velocity, K)) {
                hits++;
                assertTrue(within(reach, hit.point()), hit.id() + " " + hit.point());
            }
        }
        assertTrue(hits > 500, "hits " + hits);
    }

    @Test
    void sweptReachGrowsWithTheVelocity() {
        WorldObb[] segs = ProneTestSupport.steadyGunAtOrigin(ProneHitSettings.DEFAULTS);
        AABB still = ProneSegmentClip.reach(segs, Vec3.ZERO, K);
        AABB moving = ProneSegmentClip.reach(segs, new Vec3(0.5D, 0.0D, 0.0D), K);
        // Swept by +0.5 and moved by -1.0: the box now spans [min - 1.0, max - 0.5] in x.
        assertTrue(Math.abs(moving.minX - (still.minX - 1.0D)) < 1.0E-9D);
        assertTrue(Math.abs(moving.maxX - (still.maxX - 0.5D)) < 1.0E-9D);
    }

    private static boolean within(AABB box, Vec3 p) {
        return p.x >= box.minX && p.x <= box.maxX
                && p.y >= box.minY && p.y <= box.maxY
                && p.z >= box.minZ && p.z <= box.maxZ;
    }
}
