package com.wok.bodyhealth.prone;

/** One body-local box per {@link SegmentId}, indexed by ordinal. Treated as immutable. */
public record BodyLayout(LocalObb[] seg) {
    private static final SegmentId[] LIMBS = {
            SegmentId.RIGHT_ARM, SegmentId.LEFT_ARM, SegmentId.RIGHT_LEG, SegmentId.LEFT_LEG};

    public BodyLayout {
        if (seg.length != SegmentId.values().length) {
            throw new IllegalArgumentException("A body layout needs " + SegmentId.values().length
                    + " segments, got " + seg.length);
        }
        for (LocalObb box : seg) {
            if (box == null) {
                throw new IllegalArgumentException("Missing segment box");
            }
        }
    }

    public LocalObb get(SegmentId id) {
        return seg[id.ordinal()];
    }

    /** Copy of this layout with both arms and both legs taken from {@code limbs}. */
    public BodyLayout withLimbs(BodyLayout limbs) {
        LocalObb[] out = seg.clone();
        for (SegmentId id : LIMBS) {
            out[id.ordinal()] = limbs.get(id);
        }
        return new BodyLayout(out);
    }

    /** Copy of this layout moved along the body's forward axis. */
    public BodyLayout shiftF(double df) {
        if (df == 0.0D) {
            return this;
        }
        LocalObb[] out = new LocalObb[seg.length];
        for (int i = 0; i < seg.length; i++) {
            out[i] = seg[i].shiftF(df);
        }
        return new BodyLayout(out);
    }

    /** Segment-wise {@link LocalObb#blend}; a single weight of exactly 1 returns that layout as is. */
    public static BodyLayout blend(BodyLayout[] ls, double[] w) {
        LocalObb.checkWeights(ls.length, w);
        int sole = LocalObb.soleUnitWeight(w);
        if (sole >= 0) {
            return ls[sole];
        }
        LocalObb[] out = new LocalObb[SegmentId.values().length];
        LocalObb[] parts = new LocalObb[ls.length];
        for (int i = 0; i < out.length; i++) {
            for (int j = 0; j < ls.length; j++) {
                parts[j] = ls[j].seg[i];
            }
            out[i] = LocalObb.blend(parts, w);
        }
        return new BodyLayout(out);
    }
}
