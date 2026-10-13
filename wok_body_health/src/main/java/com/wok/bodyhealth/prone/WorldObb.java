package com.wok.bodyhealth.prone;

import net.minecraft.world.phys.Vec3;

/**
 * A segment placed in the world: unit axes e1..e3 (from the local u, v, w) and half lengths
 * h1..h3 that already include the configured margin.
 *
 * @param torsoHalfLength |v| without margin; only meaningful for the torso
 */
public record WorldObb(SegmentId id, Vec3 center, Vec3 e1, Vec3 e2, Vec3 e3,
                       double h1, double h2, double h3, double torsoHalfLength) {

    /** World point of the box-local coordinates (a, b, c) along (e1, e2, e3). */
    public Vec3 toWorld(double a, double b, double c) {
        return new Vec3(
                center.x + a * e1.x + b * e2.x + c * e3.x,
                center.y + a * e1.y + b * e2.y + c * e3.y,
                center.z + a * e1.z + b * e2.z + c * e3.z);
    }

    /** Box-local coordinates of a world point. */
    public Vec3 toLocal(Vec3 point) {
        Vec3 d = point.subtract(center);
        return new Vec3(d.dot(e1), d.dot(e2), d.dot(e3));
    }

    /** Box-local components of a world direction. */
    public Vec3 toLocalDirection(Vec3 direction) {
        return new Vec3(direction.dot(e1), direction.dot(e2), direction.dot(e3));
    }

    /** The eight corners, bit 0/1/2 selecting the + side of e1/e2/e3. */
    public Vec3[] corners() {
        Vec3[] out = new Vec3[8];
        for (int i = 0; i < 8; i++) {
            out[i] = toWorld((i & 1) != 0 ? h1 : -h1, (i & 2) != 0 ? h2 : -h2, (i & 4) != 0 ? h3 : -h3);
        }
        return out;
    }
}
