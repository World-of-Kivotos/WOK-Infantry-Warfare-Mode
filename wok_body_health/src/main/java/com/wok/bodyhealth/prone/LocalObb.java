package com.wok.bodyhealth.prone;

import net.minecraft.world.phys.Vec3;

/**
 * Body-local oriented box. Every vector holds (f, s, y) in its (x, y, z) slots: f runs along the
 * body's forward axis, s to the body's right and y up from the feet, all in blocks with the origin
 * at the entity position. u/v/w are half-axis vectors whose length is half the edge length.
 *
 * <p>v is the cube's own Y axis; for the torso it points from the neck towards the hips, which
 * is what the chest/abdomen split relies on.
 */
public record LocalObb(Vec3 c, Vec3 u, Vec3 v, Vec3 w) {
    /** Blended half lengths never collapse below this. */
    private static final double MIN_HALF_LENGTH = 0.001D;
    private static final double DEGENERATE = 1.0E-9D;

    /** Axis-aligned box from body-local extents; v points backwards so a torso keeps its neck-to-hip axis. */
    public static LocalObb ofExtents(double f0, double f1, double s0, double s1, double y0, double y1) {
        return new LocalObb(
                new Vec3((f0 + f1) * 0.5D, (s0 + s1) * 0.5D, (y0 + y1) * 0.5D),
                new Vec3(0.0D, (s1 - s0) * 0.5D, 0.0D),
                new Vec3(-(f1 - f0) * 0.5D, 0.0D, 0.0D),
                new Vec3(0.0D, 0.0D, (y1 - y0) * 0.5D));
    }

    /**
     * Weighted blend of boxes (weights may be negative for delta layering). Centres and axis vectors
     * are mixed linearly, half lengths separately, then the axes are re-orthogonalised in u, v, w
     * order. If any step degenerates the most heavily weighted input is returned unchanged.
     */
    public static LocalObb blend(LocalObb[] parts, double[] weights) {
        checkWeights(parts.length, weights);
        int sole = soleUnitWeight(weights);
        if (sole >= 0) {
            return parts[sole];
        }

        double cx = 0.0D, cy = 0.0D, cz = 0.0D;
        double ux = 0.0D, uy = 0.0D, uz = 0.0D;
        double vx = 0.0D, vy = 0.0D, vz = 0.0D;
        double wx = 0.0D, wy = 0.0D, wz = 0.0D;
        double lu = 0.0D, lv = 0.0D, lw = 0.0D;
        for (int i = 0; i < parts.length; i++) {
            double k = weights[i];
            if (k == 0.0D) {
                continue;
            }
            LocalObb p = parts[i];
            cx += k * p.c.x;
            cy += k * p.c.y;
            cz += k * p.c.z;
            ux += k * p.u.x;
            uy += k * p.u.y;
            uz += k * p.u.z;
            vx += k * p.v.x;
            vy += k * p.v.y;
            vz += k * p.v.z;
            wx += k * p.w.x;
            wy += k * p.w.y;
            wz += k * p.w.z;
            lu += k * p.u.length();
            lv += k * p.v.length();
            lw += k * p.w.length();
        }
        LocalObb blended = orthogonalize(new Vec3(cx, cy, cz),
                new Vec3(ux, uy, uz), new Vec3(vx, vy, vz), new Vec3(wx, wy, wz),
                Math.max(MIN_HALF_LENGTH, lu), Math.max(MIN_HALF_LENGTH, lv), Math.max(MIN_HALF_LENGTH, lw));
        return blended != null ? blended : parts[heaviest(weights)];
    }

    /** Gram-Schmidt in u, v, w order keeping each half length; returns this box if it is degenerate. */
    public LocalObb orthonormalized() {
        LocalObb result = orthogonalize(c, u, v, w,
                Math.max(MIN_HALF_LENGTH, u.length()),
                Math.max(MIN_HALF_LENGTH, v.length()),
                Math.max(MIN_HALF_LENGTH, w.length()));
        return result != null ? result : this;
    }

    /**
     * {@code |c| + |u| + |v| + |w|}: no point of the box is further from the origin, even if its
     * axes end up as unit vectors that are not quite orthogonal.
     */
    public double reach() {
        return c.length() + u.length() + v.length() + w.length();
    }

    /** Moves the box along the body's forward axis. */
    public LocalObb shiftF(double df) {
        return new LocalObb(c.add(df, 0.0D, 0.0D), u, v, w);
    }

    private static LocalObb orthogonalize(Vec3 c, Vec3 u, Vec3 v, Vec3 w,
                                          double lengthU, double lengthV, double lengthW) {
        Vec3 e1 = unitOrNull(u);
        if (e1 == null) {
            return null;
        }
        Vec3 e2 = unitOrNull(v.subtract(e1.scale(v.dot(e1))));
        if (e2 == null) {
            return null;
        }
        Vec3 e3 = unitOrNull(w.subtract(e1.scale(w.dot(e1))).subtract(e2.scale(w.dot(e2))));
        if (e3 == null) {
            return null;
        }
        return new LocalObb(c, e1.scale(lengthU), e2.scale(lengthV), e3.scale(lengthW));
    }

    private static Vec3 unitOrNull(Vec3 vector) {
        double length = vector.length();
        if (!(length >= DEGENERATE) || !Double.isFinite(length)) {
            return null;
        }
        return new Vec3(vector.x / length, vector.y / length, vector.z / length);
    }

    static void checkWeights(int parts, double[] weights) {
        if (parts == 0 || parts != weights.length) {
            throw new IllegalArgumentException("Need one weight per part, got " + parts + " parts and "
                    + weights.length + " weights");
        }
    }

    /** Index of the only non-zero weight when that weight is exactly 1, otherwise -1. */
    static int soleUnitWeight(double[] weights) {
        int found = -1;
        for (int i = 0; i < weights.length; i++) {
            if (weights[i] != 0.0D) {
                if (found >= 0 || weights[i] != 1.0D) {
                    return -1;
                }
                found = i;
            }
        }
        return found;
    }

    static int heaviest(double[] weights) {
        int best = 0;
        for (int i = 1; i < weights.length; i++) {
            if (weights[i] > weights[best]) {
                best = i;
            }
        }
        return best;
    }
}
