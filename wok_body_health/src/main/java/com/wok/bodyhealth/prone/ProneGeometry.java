package com.wok.bodyhealth.prone;

import net.minecraft.world.phys.Vec3;

/**
 * Body-local to world: {@code P = pos + f*F + s*R + y*Y} with F/R from the body yaw, the same
 * axes as {@code HitLocationResolver}. Uses Math.sin/cos rather than Mth's lookup table.
 */
public final class ProneGeometry {
    private static final double DEGENERATE = 1.0E-9D;
    /** Half the SWIMMING pose height: the centre of the movement box. */
    private static final double CORE_HEIGHT = 0.3D;

    public static Vec3 forward(double yawDeg) {
        double radians = Math.toRadians(yawDeg);
        return new Vec3(-Math.sin(radians), 0.0D, Math.cos(radians));
    }

    public static Vec3 right(double yawDeg) {
        double radians = Math.toRadians(yawDeg);
        return new Vec3(-Math.cos(radians), 0.0D, -Math.sin(radians));
    }

    /** Places every segment of the pose at the entity position, adding the per-segment margin. */
    public static WorldObb[] place(ProneLayouts.BodyPose pose, double x, double y, double z, ProneHitSettings cfg) {
        Vec3 forward = forward(pose.yawDeg());
        Vec3 right = right(pose.yawDeg());
        SegmentId[] ids = SegmentId.values();
        WorldObb[] out = new WorldObb[ids.length];
        for (SegmentId id : ids) {
            LocalObb box = pose.layout().get(id);
            Vec3 c = toWorld(box.c(), forward, right);
            double margin = Math.max(0.0D, cfg.margin(id));
            double lengthU = box.u().length();
            double lengthV = box.v().length();
            double lengthW = box.w().length();
            out[id.ordinal()] = new WorldObb(id, new Vec3(x + c.x, y + c.y, z + c.z),
                    unit(toWorld(box.u(), forward, right), right),
                    unit(toWorld(box.v(), forward, right), forward),
                    unit(toWorld(box.w(), forward, right), new Vec3(0.0D, 1.0D, 0.0D)),
                    lengthU + margin, lengthV + margin, lengthW + margin, lengthV);
        }
        return out;
    }

    /** Centre of the SWIMMING movement box at a snapshot position; it is always in open air. */
    public static Vec3 core(double x, double y, double z) {
        return new Vec3(x, y + CORE_HEIGHT, z);
    }

    private static Vec3 toWorld(Vec3 local, Vec3 forward, Vec3 right) {
        return new Vec3(
                local.x * forward.x + local.y * right.x,
                local.z,
                local.x * forward.z + local.y * right.z);
    }

    private static Vec3 unit(Vec3 vector, Vec3 fallback) {
        double length = vector.length();
        if (!(length >= DEGENERATE) || !Double.isFinite(length)) {
            return fallback;
        }
        return new Vec3(vector.x / length, vector.y / length, vector.z / length);
    }

    private ProneGeometry() {
    }
}
