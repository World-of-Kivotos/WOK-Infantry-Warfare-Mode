package com.wok.bodyhealth.prone;

import com.wok.bodyhealth.health.BodyPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Ray test against placed segments. Each segment is handled in its own frame with vanilla
 * {@link AABB#clip}, swept and moved by the target velocity exactly like TaCZ/SBW treat the
 * whole box, so the entry-face-only semantics stay identical to the original check.
 */
public final class ProneSegmentClip {
    private static final double TIE_EPSILON = 1.0E-9D;
    private static final double REACH_PADDING = 1.0E-4D;
    /** Torso lambda below this (neck = -1, hips = +1) is chest: the upper 8 of 12 model pixels. */
    private static final double CHEST_LIMIT = 1.0D / 3.0D;

    /**
     * @param t           entry parameter along start..end
     * @param torsoLambda torso only: entry position along the neck-to-hip axis relative to the swept
     *                    box centre, -1 at the neck and +1 at the hips; NaN for other segments
     */
    public record SegmentHit(SegmentId id, Vec3 point, double t, double torsoLambda) {
    }

    /** @param hit the first entry not hidden behind a block, or null */
    public record Selection(SegmentHit hit, int occluded) {
    }

    /**
     * Clips every segment independently. Sorted by t; entries closer than 1e-9 are ordered by
     * {@link SegmentId#TIE_ORDER}. A start inside a segment misses that segment only.
     *
     * @param velocity    the target velocity the gun mod sweeps its box with
     * @param shiftFactor {@link RewindPolicy#shiftFactor()}
     */
    public static List<SegmentHit> clipAll(WorldObb[] segs, Vec3 start, Vec3 end, Vec3 velocity, double shiftFactor) {
        List<SegmentHit> hits = new ArrayList<>(segs.length);
        for (WorldObb seg : segs) {
            Vec3 a = seg.toLocal(start);
            Vec3 b = seg.toLocal(end);
            Vec3 localVelocity = seg.toLocalDirection(velocity);
            Optional<Vec3> entry = sweptBox(seg, localVelocity, shiftFactor).clip(a, b);
            if (entry.isEmpty()) {
                continue;
            }
            Vec3 q = entry.get();
            Vec3 ray = b.subtract(a);
            double t = q.subtract(a).dot(ray) / ray.lengthSqr();
            double lambda = Double.NaN;
            if (seg.id() == SegmentId.TORSO) {
                lambda = seg.torsoHalfLength() > TIE_EPSILON
                        ? (q.y - (shiftFactor + 0.5D) * localVelocity.y) / seg.torsoHalfLength() : 0.0D;
            }
            hits.add(new SegmentHit(seg.id(), seg.toWorld(q.x, q.y, q.z), t, lambda));
        }
        sortByEntry(hits);
        return hits;
    }

    /**
     * First hit whose point can see the body core. A hidden hit is dropped and the ray carries on
     * to the next segment, so a head poking through a door cannot be shot from the other side.
     */
    public static Selection select(List<SegmentHit> hits, Vec3 core, OcclusionTest occ) {
        int occluded = 0;
        for (SegmentHit hit : hits) {
            if (occ != null && occ.blocked(hit.point(), core)) {
                occluded++;
                continue;
            }
            return new Selection(hit, occluded);
        }
        return new Selection(null, occluded);
    }

    /** HIT for a selected segment, otherwise MISS; the note records how many segments were hidden. */
    public static ProneHitResult toResult(Selection selection) {
        String note = selection.occluded() > 0 ? "occluded=" + selection.occluded() : null;
        SegmentHit hit = selection.hit();
        if (hit == null) {
            return ProneHitResult.miss(note);
        }
        return ProneHitResult.hit(hit.id(), partOf(hit), hit.point(), note);
    }

    /**
     * World AABB containing every point any segment can be hit at with this velocity: the
     * corners of each swept and moved box in its own frame, mapped to the world. Segments must
     * not be empty.
     */
    public static AABB reach(WorldObb[] segs, Vec3 velocity, double shiftFactor) {
        if (segs.length == 0) {
            throw new IllegalArgumentException("No segments");
        }
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (WorldObb seg : segs) {
            AABB box = sweptBox(seg, seg.toLocalDirection(velocity), shiftFactor);
            for (int i = 0; i < 8; i++) {
                Vec3 corner = seg.toWorld(
                        (i & 1) != 0 ? box.maxX : box.minX,
                        (i & 2) != 0 ? box.maxY : box.minY,
                        (i & 4) != 0 ? box.maxZ : box.minZ);
                minX = Math.min(minX, corner.x);
                minY = Math.min(minY, corner.y);
                minZ = Math.min(minZ, corner.z);
                maxX = Math.max(maxX, corner.x);
                maxY = Math.max(maxY, corner.y);
                maxZ = Math.max(maxZ, corner.z);
            }
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ).inflate(REACH_PADDING);
    }

    public static BodyPart partOf(SegmentHit hit) {
        return switch (hit.id()) {
            case HEAD -> BodyPart.HEAD;
            case TORSO -> hit.torsoLambda() < CHEST_LIMIT ? BodyPart.CHEST : BodyPart.ABDOMEN;
            case RIGHT_ARM -> BodyPart.RIGHT_ARM;
            case LEFT_ARM -> BodyPart.LEFT_ARM;
            case RIGHT_LEG -> BodyPart.RIGHT_LEG;
            case LEFT_LEG -> BodyPart.LEFT_LEG;
        };
    }

    /** {@code AABB(-h..h).expandTowards(v).move(v * k)} in the segment's own frame. */
    private static AABB sweptBox(WorldObb seg, Vec3 localVelocity, double shiftFactor) {
        return new AABB(-seg.h1(), -seg.h2(), -seg.h3(), seg.h1(), seg.h2(), seg.h3())
                .expandTowards(localVelocity)
                .move(localVelocity.scale(shiftFactor));
    }

    /** Sort by t, then order each run of entries within 1e-9 of its first entry by tie rank. */
    private static void sortByEntry(List<SegmentHit> hits) {
        hits.sort(Comparator.comparingDouble(SegmentHit::t));
        int runStart = 0;
        while (runStart < hits.size()) {
            int runEnd = runStart + 1;
            double first = hits.get(runStart).t();
            while (runEnd < hits.size() && hits.get(runEnd).t() - first < TIE_EPSILON) {
                runEnd++;
            }
            if (runEnd - runStart > 1) {
                hits.subList(runStart, runEnd).sort(Comparator.comparingInt(hit -> hit.id().tieRank()));
            }
            runStart = runEnd;
        }
    }

    private ProneSegmentClip() {
    }
}
