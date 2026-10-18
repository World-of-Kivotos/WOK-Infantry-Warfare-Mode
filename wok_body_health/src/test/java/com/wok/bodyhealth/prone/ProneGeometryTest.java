package com.wok.bodyhealth.prone;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static com.wok.bodyhealth.prone.ProneTestSupport.assertVec;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class ProneGeometryTest {
    @Test
    void yawZeroFacesPositiveZWithTheRightSideOnNegativeX() {
        assertVec(new Vec3(0.0D, 0.0D, 1.0D), ProneGeometry.forward(0.0D), 1.0E-12D);
        assertVec(new Vec3(-1.0D, 0.0D, 0.0D), ProneGeometry.right(0.0D), 1.0E-12D);
    }

    @Test
    void yawNinetyFacesNegativeX() {
        assertVec(new Vec3(-1.0D, 0.0D, 0.0D), ProneGeometry.forward(90.0D), 1.0E-12D);
        assertVec(new Vec3(0.0D, 0.0D, -1.0D), ProneGeometry.right(90.0D), 1.0E-12D);
    }

    @Test
    void localForwardMapsAlongTheBodyYaw() {
        LocalObb unit = LocalObb.ofExtents(0.75D, 1.25D, -0.25D, 0.25D, -0.25D, 0.25D);
        WorldObb[] placed = ProneGeometry.place(pose(0.0F, unit), 0.0D, 64.0D, 0.0D, ProneTestSupport.NO_MARGIN);
        assertVec(new Vec3(0.0D, 64.0D, 1.0D), placed[SegmentId.HEAD.ordinal()].center(), 1.0E-12D);

        WorldObb turned = ProneGeometry.place(pose(90.0F, unit), 10.0D, 64.0D, 5.0D,
                ProneTestSupport.NO_MARGIN)[SegmentId.HEAD.ordinal()];
        assertVec(new Vec3(9.0D, 64.0D, 5.0D), turned.center(), 1.0E-12D);
    }

    @Test
    void placementAddsTheSegmentMarginButNotToTheTorsoLength() {
        LocalObb box = LocalObb.ofExtents(-0.25D, 0.25D, -0.125D, 0.125D, 0.0D, 0.5D);
        WorldObb[] placed = ProneGeometry.place(pose(30.0F, box), 0.0D, 0.0D, 0.0D, ProneHitSettings.DEFAULTS);

        WorldObb head = placed[SegmentId.HEAD.ordinal()];
        assertEquals(0.125D + 0.0625D, head.h1(), 1.0E-12D);
        assertEquals(0.25D + 0.0625D, head.h2(), 1.0E-12D);
        assertEquals(0.25D + 0.0625D, head.h3(), 1.0E-12D);
        WorldObb torso = placed[SegmentId.TORSO.ordinal()];
        assertEquals(0.25D + 0.03125D, torso.h2(), 1.0E-12D);
        assertEquals(0.25D, torso.torsoHalfLength(), 1.0E-12D);
        assertEquals(0.125D + 0.015625D, placed[SegmentId.LEFT_LEG.ordinal()].h1(), 1.0E-12D);

        // Axes stay orthonormal, e2 points backwards along the body.
        assertEquals(1.0D, torso.e1().length(), 1.0E-12D);
        assertEquals(0.0D, torso.e1().dot(torso.e2()), 1.0E-12D);
        assertVec(ProneGeometry.forward(30.0D).scale(-1.0D), torso.e2(), 1.0E-12D);
        assertVec(new Vec3(0.0D, 1.0D, 0.0D), torso.e3(), 1.0E-12D);
    }

    @Test
    void coreIsTheSwimmingBoxCentre() {
        assertVec(new Vec3(0.0D, 64.3D, 0.0D), ProneGeometry.core(0.0D, 64.0D, 0.0D), 1.0E-12D);
    }

    @Test
    void localAndWorldCoordinatesRoundTrip() {
        WorldObb seg = ProneTestSupport.steadyGunAtOrigin(ProneHitSettings.DEFAULTS)[SegmentId.LEFT_ARM.ordinal()];
        Vec3 world = new Vec3(0.3D, 64.2D, 0.6D);
        Vec3 local = seg.toLocal(world);
        assertVec(world, seg.toWorld(local.x, local.y, local.z), 1.0E-12D);
        Vec3[] corners = seg.corners();
        assertEquals(8, corners.length);
        for (Vec3 corner : corners) {
            Vec3 c = seg.toLocal(corner);
            assertEquals(seg.h1(), Math.abs(c.x), 1.0E-12D);
            assertEquals(seg.h2(), Math.abs(c.y), 1.0E-12D);
            assertEquals(seg.h3(), Math.abs(c.z), 1.0E-12D);
        }
    }

    private static ProneLayouts.BodyPose pose(float yaw, LocalObb box) {
        LocalObb[] all = new LocalObb[SegmentId.values().length];
        Arrays.fill(all, box);
        return new ProneLayouts.BodyPose(yaw, new BodyLayout(all));
    }
}
