package com.wok.bodyhealth.health;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class HitLocationResolver {
    public static BodyPart fromImpact(Player target, Vec3 impact, boolean headshot) {
        if (headshot) {
            return BodyPart.HEAD;
        }

        AABB bounds = target.getBoundingBox();
        double height = Math.max(0.01D, bounds.getYsize());
        double yRatio = Mth.clamp((impact.y - bounds.minY) / height, 0.0D, 1.0D);

        float yawRadians = target.getYRot() * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Mth.cos(yawRadians), 0.0D, -Mth.sin(yawRadians));
        Vec3 center = bounds.getCenter();
        double lateral = impact.subtract(center).dot(right);
        boolean rightSide = lateral >= 0.0D;
        boolean outsideTorso = Math.abs(lateral) > bounds.getXsize() * 0.22D;

        if (yRatio >= 0.78D) {
            return BodyPart.HEAD;
        }
        if (yRatio >= 0.52D) {
            if (outsideTorso) {
                return rightSide ? BodyPart.RIGHT_ARM : BodyPart.LEFT_ARM;
            }
            return BodyPart.CHEST;
        }
        if (yRatio >= 0.36D) {
            return BodyPart.ABDOMEN;
        }
        return rightSide ? BodyPart.RIGHT_LEG : BodyPart.LEFT_LEG;
    }

    /**
     * Resolves an exact ray/AABB entry point against seven mutually exclusive
     * player regions. TaCZ supplies the lag-compensated hitbox used here.
     */
    public static BodyPart fromHitbox(Player target, AABB hitbox, Vec3 impact,
                                      boolean headshot) {
        if (headshot) {
            return BodyPart.HEAD;
        }

        Pose pose = target.getPose();
        if (pose == Pose.SWIMMING
                || pose == Pose.FALL_FLYING
                || pose == Pose.SPIN_ATTACK
                || pose == Pose.SLEEPING) {
            return fromHorizontalHitbox(target, hitbox, impact);
        }
        return fromVerticalHitbox(target, hitbox, impact);
    }

    private static BodyPart fromVerticalHitbox(Player target, AABB hitbox, Vec3 impact) {
        Vec3 center = hitbox.getCenter();
        double yRatio = Mth.clamp(
                (impact.y - hitbox.minY) / Math.max(0.01D, hitbox.getYsize()),
                0.0D, 1.0D);
        double lateral = normalizedProjection(
                impact.subtract(center), rightVector(target), hitbox);

        // Minecraft's 16-pixel-wide player model consists of an 8-pixel torso
        // with 4-pixel arms on both sides. The outer quarters of TaCZ's player
        // hitbox therefore belong to the arms while the middle half is torso.
        if (yRatio >= 0.78D) {
            return BodyPart.HEAD;
        }
        if (yRatio >= 0.34D && Math.abs(lateral) >= 0.50D) {
            return sidePart(lateral, BodyPart.RIGHT_ARM, BodyPart.LEFT_ARM);
        }
        if (yRatio >= 0.52D) {
            return BodyPart.CHEST;
        }
        if (yRatio >= 0.34D) {
            return BodyPart.ABDOMEN;
        }
        return sidePart(lateral, BodyPart.RIGHT_LEG, BodyPart.LEFT_LEG);
    }

    private static BodyPart fromHorizontalHitbox(Player target, AABB hitbox, Vec3 impact) {
        Vec3 center = hitbox.getCenter();
        Vec3 offset = impact.subtract(center);
        Vec3 forward = target.getViewVector(1.0F).normalize();
        if (!isFiniteDirection(forward)) {
            forward = forwardVector(target);
        }
        Vec3 right = rightVector(target);
        double longitudinal = normalizedProjection(offset, forward, hitbox);
        double lateral = normalizedProjection(offset, right, hitbox);

        // Swimming, elytra flight and spin attack use a short horizontal TaCZ
        // AABB. Partition its forward axis from head to feet, then split arms
        // and legs by the same left/right model geometry as the upright box.
        if (longitudinal >= 0.58D) {
            return BodyPart.HEAD;
        }
        if (longitudinal >= -0.30D && Math.abs(lateral) >= 0.50D) {
            return sidePart(lateral, BodyPart.RIGHT_ARM, BodyPart.LEFT_ARM);
        }
        if (longitudinal >= 0.10D) {
            return BodyPart.CHEST;
        }
        if (longitudinal >= -0.30D) {
            return BodyPart.ABDOMEN;
        }
        return sidePart(lateral, BodyPart.RIGHT_LEG, BodyPart.LEFT_LEG);
    }

    private static double normalizedProjection(Vec3 offset, Vec3 axis, AABB hitbox) {
        double halfX = hitbox.getXsize() * 0.5D;
        double halfY = hitbox.getYsize() * 0.5D;
        double halfZ = hitbox.getZsize() * 0.5D;
        double extent = Math.abs(axis.x) * halfX
                + Math.abs(axis.y) * halfY
                + Math.abs(axis.z) * halfZ;
        if (extent <= 1.0E-6D) {
            return 0.0D;
        }
        return Mth.clamp(offset.dot(axis) / extent, -1.0D, 1.0D);
    }

    private static Vec3 rightVector(Player target) {
        float yawRadians = target.yBodyRot * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.cos(yawRadians), 0.0D, -Mth.sin(yawRadians));
    }

    private static Vec3 forwardVector(Player target) {
        float yawRadians = target.yBodyRot * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yawRadians), 0.0D, Mth.cos(yawRadians));
    }

    private static boolean isFiniteDirection(Vec3 direction) {
        return Double.isFinite(direction.x)
                && Double.isFinite(direction.y)
                && Double.isFinite(direction.z)
                && direction.lengthSqr() > 1.0E-8D;
    }

    private static BodyPart sidePart(double lateral, BodyPart right, BodyPart left) {
        return lateral >= 0.0D ? right : left;
    }

    private HitLocationResolver() {
    }
}
