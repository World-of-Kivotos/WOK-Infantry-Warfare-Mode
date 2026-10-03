package com.wok.bodyhealth.prone;

import com.wok.bodyhealth.health.BodyPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Steady TAA prone pose with a TaCZ gun, aim 0, pitch 0, facing +Z at (0, 64, 0). */
final class ProneSegmentHitTest {
    private static final Vec3 CORE = ProneGeometry.core(0.0D, 64.0D, 0.0D);
    /** The original SWIMMING box TaCZ tests against, with its +0.0625 top for non-sneaking players. */
    private static final AABB OLD_BOX = new AABB(-0.3D, 64.0D, -0.3D, 0.3D, 64.6625D, 0.3D);

    @Test
    void shotFromTheSideHitsTheLeftLegOutsideTheOldBox() {
        Vec3 start = new Vec3(3.0D, 64.15D, -0.8D);
        Vec3 end = new Vec3(-3.0D, 64.15D, -0.8D);
        ProneHitResult result = shoot(noMargin(), start, end, OcclusionTest.NONE);

        assertEquals(ProneHitResult.Kind.HIT, result.kind());
        assertEquals(SegmentId.LEFT_LEG, result.segment());
        assertEquals(BodyPart.LEFT_LEG, result.part());
        assertTrue(result.legSegment());
        assertFalse(result.headshot());
        assertTrue(result.point().x > 0.25D && result.point().x < 0.30D, "x " + result.point().x);
        assertTrue(OLD_BOX.clip(start, end).isEmpty());
    }

    @Test
    void airAboveTheBackMissesWithDefaultMargins() {
        Vec3 start = new Vec3(3.0D, 64.45D, -0.1D);
        Vec3 end = new Vec3(-3.0D, 64.45D, -0.1D);
        WorldObb[] segs = ProneTestSupport.steadyGunAtOrigin(ProneHitSettings.DEFAULTS);

        assertSame(ProneHitResult.MISS, shoot(segs, start, end, OcclusionTest.NONE));
        assertTrue(OLD_BOX.clip(start, end).isPresent());
    }

    @Test
    void frontalShotHitsTheHeadFace() {
        ProneHitResult result = shoot(noMargin(),
                new Vec3(0.0D, 64.4D, 3.0D), new Vec3(0.0D, 64.4D, -3.0D), OcclusionTest.NONE);

        assertEquals(SegmentId.HEAD, result.segment());
        assertEquals(BodyPart.HEAD, result.part());
        assertTrue(result.headshot());
        assertEquals(0.641D, result.point().z, 2.0E-3D);
    }

    @Test
    void verticalShotsSplitTheTorsoIntoChestAndAbdomen() {
        assertEquals(BodyPart.CHEST, downAt(0.1D));
        assertEquals(BodyPart.ABDOMEN, downAt(-0.2D));
        // The split sits 8 of 12 model pixels below the neck: f = 0.056 - 0.352 / 3 = -0.0613.
        assertEquals(BodyPart.CHEST, downAt(-0.055D));
        assertEquals(BodyPart.ABDOMEN, downAt(-0.068D));
    }

    @Test
    void selectionEqualsTheNearestSingleSegmentClip() {
        WorldObb[] segs = ProneTestSupport.steadyGunAtOrigin(ProneHitSettings.DEFAULTS);
        Random random = new Random(7L);
        int hits = 0;
        for (int n = 0; n < 4000; n++) {
            Vec3 start = new Vec3(random.nextDouble() * 6.0D - 3.0D, 63.5D + random.nextDouble() * 2.0D,
                    random.nextDouble() * 6.0D - 3.0D);
            Vec3 aim = new Vec3(random.nextDouble() * 2.4D - 1.2D, 64.0D + random.nextDouble() * 0.8D,
                    random.nextDouble() * 2.4D - 1.2D);
            Vec3 end = start.add(aim.subtract(start).scale(2.0D));

            ProneSegmentClip.SegmentHit nearest = null;
            for (WorldObb seg : segs) {
                List<ProneSegmentClip.SegmentHit> single =
                        ProneSegmentClip.clipAll(new WorldObb[]{seg}, start, end, Vec3.ZERO, -2.0D);
                if (!single.isEmpty() && (nearest == null || single.get(0).t() < nearest.t())) {
                    nearest = single.get(0);
                }
            }
            ProneSegmentClip.Selection selection = ProneSegmentClip.select(
                    ProneSegmentClip.clipAll(segs, start, end, Vec3.ZERO, -2.0D), CORE, OcclusionTest.NONE);
            if (nearest == null) {
                assertNull(selection.hit());
            } else {
                hits++;
                assertEquals(nearest.t(), selection.hit().t(), 1.0E-9D);
                assertEquals(0, selection.occluded());
            }
        }
        assertTrue(hits > 500, "hits " + hits);
    }

    @Test
    void aStartInsideTheTorsoStillHitsTheHead() {
        ProneHitResult result = shoot(noMargin(),
                new Vec3(0.0D, 64.2D, 0.0D), new Vec3(0.0D, 64.2D, 3.0D), OcclusionTest.NONE);
        assertEquals(SegmentId.HEAD, result.segment());
    }

    @Test
    void aRayEndingShortOfTheFeetMisses() {
        WorldObb[] segs = noMargin();
        Vec3 start = new Vec3(0.1D, 64.15D, -3.0D);
        assertSame(ProneHitResult.MISS, shoot(segs, start, new Vec3(0.1D, 64.15D, -1.05D), OcclusionTest.NONE));
        assertEquals(SegmentId.LEFT_LEG,
                shoot(segs, start, new Vec3(0.1D, 64.15D, -0.2D), OcclusionTest.NONE).segment());
    }

    @Test
    void anOccludedHeadHitIsDropped() {
        WorldObb[] segs = noMargin();
        Vec3 start = new Vec3(0.0D, 64.4D, 3.0D);
        Vec3 end = new Vec3(0.0D, 64.4D, -3.0D);
        List<ProneSegmentClip.SegmentHit> hits = ProneSegmentClip.clipAll(segs, start, end, Vec3.ZERO, -2.0D);
        Vec3 headPoint = hits.get(0).point();
        OcclusionTest wallAtHead = (from, to) -> from.equals(headPoint);

        ProneSegmentClip.Selection selection = ProneSegmentClip.select(hits, CORE, wallAtHead);
        assertEquals(1, selection.occluded());
        ProneHitResult result = ProneSegmentClip.toResult(selection);
        if (hits.size() > 1) {
            assertSame(hits.get(1), selection.hit());
        } else {
            assertEquals(ProneHitResult.Kind.MISS, result.kind());
        }
        assertEquals("occluded=1", result.note());

        // Lower down the ray passes the supporting left arm, the head and the torso; hidden hits
        // hand over to the next segment along the ray.
        List<ProneSegmentClip.SegmentHit> low = ProneSegmentClip.clipAll(segs,
                new Vec3(0.0D, 64.2D, 3.0D), new Vec3(0.0D, 64.2D, -3.0D), Vec3.ZERO, -2.0D);
        assertTrue(low.size() >= 3, "hits " + low);
        Vec3 first = low.get(0).point();
        Vec3 second = low.get(1).point();
        ProneSegmentClip.Selection next = ProneSegmentClip.select(low, CORE, (from, to) -> from.equals(first));
        assertSame(low.get(1), next.hit());
        assertEquals(1, next.occluded());
        ProneSegmentClip.Selection third = ProneSegmentClip.select(low, CORE,
                (from, to) -> from.equals(first) || from.equals(second));
        assertSame(low.get(2), third.hit());
        assertNotEquals(low.get(0).id(), third.hit().id());
        assertEquals(2, third.occluded());
    }

    @Test
    void hitsAreOrderedByEntryWithTiesInTieOrder() {
        WorldObb[] segs = ProneTestSupport.steadyGunAtOrigin(ProneHitSettings.DEFAULTS);
        List<ProneSegmentClip.SegmentHit> along = ProneSegmentClip.clipAll(segs,
                new Vec3(0.0D, 64.2D, 3.0D), new Vec3(0.0D, 64.2D, -3.0D), Vec3.ZERO, -2.0D);
        assertTrue(along.size() >= 3);
        for (int i = 1; i < along.size(); i++) {
            assertTrue(along.get(i - 1).t() <= along.get(i).t());
        }

        WorldObb head = segs[SegmentId.HEAD.ordinal()];
        WorldObb[] twins = {
                copyAs(head, SegmentId.LEFT_LEG), copyAs(head, SegmentId.HEAD),
                copyAs(head, SegmentId.RIGHT_ARM), copyAs(head, SegmentId.TORSO)};
        List<ProneSegmentClip.SegmentHit> tied = ProneSegmentClip.clipAll(twins,
                new Vec3(0.0D, 64.4D, 3.0D), new Vec3(0.0D, 64.4D, -3.0D), Vec3.ZERO, -2.0D);
        assertEquals(List.of(SegmentId.TORSO, SegmentId.HEAD, SegmentId.RIGHT_ARM, SegmentId.LEFT_LEG),
                tied.stream().map(ProneSegmentClip.SegmentHit::id).toList());
    }

    @Test
    void partsFollowTheSegments() {
        Vec3 p = Vec3.ZERO;
        assertEquals(BodyPart.RIGHT_ARM, ProneSegmentClip.partOf(new ProneSegmentClip.SegmentHit(SegmentId.RIGHT_ARM, p, 0, Double.NaN)));
        assertEquals(BodyPart.LEFT_ARM, ProneSegmentClip.partOf(new ProneSegmentClip.SegmentHit(SegmentId.LEFT_ARM, p, 0, Double.NaN)));
        assertEquals(BodyPart.RIGHT_LEG, ProneSegmentClip.partOf(new ProneSegmentClip.SegmentHit(SegmentId.RIGHT_LEG, p, 0, Double.NaN)));
        assertEquals(BodyPart.CHEST, ProneSegmentClip.partOf(new ProneSegmentClip.SegmentHit(SegmentId.TORSO, p, 0, 0.33D)));
        assertEquals(BodyPart.ABDOMEN, ProneSegmentClip.partOf(new ProneSegmentClip.SegmentHit(SegmentId.TORSO, p, 0, 0.34D)));
    }

    private static BodyPart downAt(double f) {
        ProneHitResult result = shoot(noMargin(),
                new Vec3(0.0D, 66.0D, f), new Vec3(0.0D, 63.0D, f), OcclusionTest.NONE);
        assertEquals(SegmentId.TORSO, result.segment(), "f " + f);
        return result.part();
    }

    private static ProneHitResult shoot(WorldObb[] segs, Vec3 start, Vec3 end, OcclusionTest occ) {
        return ProneSegmentClip.toResult(ProneSegmentClip.select(
                ProneSegmentClip.clipAll(segs, start, end, Vec3.ZERO, -2.0D), CORE, occ));
    }

    private static WorldObb[] noMargin() {
        return ProneTestSupport.steadyGunAtOrigin(ProneTestSupport.NO_MARGIN);
    }

    private static WorldObb copyAs(WorldObb seg, SegmentId id) {
        return new WorldObb(id, seg.center(), seg.e1(), seg.e2(), seg.e3(), seg.h1(), seg.h2(), seg.h3(),
                seg.torsoHalfLength());
    }
}
