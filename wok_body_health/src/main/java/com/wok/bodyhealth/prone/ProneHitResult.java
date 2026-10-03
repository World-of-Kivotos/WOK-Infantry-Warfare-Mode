package com.wok.bodyhealth.prone;

import com.wok.bodyhealth.health.BodyPart;
import net.minecraft.world.phys.Vec3;

/**
 * Outcome of a segmented prone hit test. NOT_APPLICABLE hands the hit back to the original
 * hitbox; segment, part and point are only set for HIT.
 *
 * @param point entry point on the swept, moved segment box
 * @param note  debug detail such as {@code "already_hit"} or {@code "occluded=2"}, may be null
 */
public record ProneHitResult(Kind kind, SegmentId segment, BodyPart part, Vec3 point, String note) {
    public enum Kind { NOT_APPLICABLE, MISS, HIT }

    public static final ProneHitResult NOT_APPLICABLE = new ProneHitResult(Kind.NOT_APPLICABLE, null, null, null, null);
    public static final ProneHitResult MISS = new ProneHitResult(Kind.MISS, null, null, null, null);

    public ProneHitResult {
        if (kind == null) {
            throw new IllegalArgumentException("kind");
        }
        if (kind == Kind.HIT && (segment == null || part == null || point == null)) {
            throw new IllegalArgumentException("A hit needs a segment, part and point");
        }
    }

    public static ProneHitResult hit(SegmentId segment, BodyPart part, Vec3 point, String note) {
        return new ProneHitResult(Kind.HIT, segment, part, point, note);
    }

    public static ProneHitResult miss(String note) {
        return note == null ? MISS : new ProneHitResult(Kind.MISS, null, null, null, note);
    }

    public static ProneHitResult notApplicable(String note) {
        return note == null ? NOT_APPLICABLE : new ProneHitResult(Kind.NOT_APPLICABLE, null, null, null, note);
    }

    /** Only the head segment is a headshot. */
    public boolean headshot() {
        return part == BodyPart.HEAD;
    }

    public boolean legSegment() {
        return segment == SegmentId.RIGHT_LEG || segment == SegmentId.LEFT_LEG;
    }
}
