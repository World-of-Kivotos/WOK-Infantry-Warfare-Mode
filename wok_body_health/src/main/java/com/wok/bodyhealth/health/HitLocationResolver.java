package com.wok.bodyhealth.health;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class HitLocationResolver {
    /** Vanilla ProjectileUtil inflates target boxes by this much when testing projectile hits. */
    private static final double PROJECTILE_HIT_MARGIN = 0.3D;

    /**
     * Resolves an exact ray/AABB entry point against seven mutually exclusive
     * player regions. TaCZ supplies the lag-compensated hitbox used here.
     */
    public static BodyPart fromHitbox(Player target, AABB hitbox, Vec3 impact,
                                      boolean headshot) {
        if (headshot) {
            return BodyPart.HEAD;
        }

        Vec3 right = rightVector(target.yBodyRot);
        if (isHorizontal(target.getPose())) {
            Vec3 forward = target.getViewVector(1.0F).normalize();
            if (!isFiniteDirection(forward)) {
                forward = forwardVector(target.yBodyRot);
            }
            return fromHorizontalHitbox(hitbox, impact, forward, right);
        }
        return fromVerticalHitbox(hitbox, impact, right);
    }

    /**
     * Finds where a projectile enters the target. During a hurt call the
     * projectile still sits at its position from before this tick's movement,
     * which may be several blocks away, so its motion is traced into the box.
     */
    public static Vec3 traceImpact(AABB target, Vec3 start, Vec3 motion) {
        if (target.contains(start)) {
            return start;
        }
        if (motion.lengthSqr() > 1.0E-8D) {
            Vec3 end = start.add(motion);
            Optional<Vec3> entry = target.clip(start, end);
            if (entry.isPresent()) {
                return entry.get();
            }
            // Vanilla also accepts rays that only graze the inflated margin.
            Optional<Vec3> grazing = target.inflate(PROJECTILE_HIT_MARGIN).clip(start, end);
            if (grazing.isPresent()) {
                return closestPoint(target, grazing.get());
            }
        }
        return closestPoint(target, start);
    }

    public static Vec3 closestPoint(AABB box, Vec3 point) {
        return new Vec3(
                Mth.clamp(point.x, box.minX, box.maxX),
                Mth.clamp(point.y, box.minY, box.maxY),
                Mth.clamp(point.z, box.minZ, box.maxZ));
    }

    static BodyPart fromVerticalHitbox(AABB hitbox, Vec3 impact, Vec3 right) {
        Vec3 center = hitbox.getCenter();
        double yRatio = Mth.clamp(
                (impact.y - hitbox.minY) / Math.max(0.01D, hitbox.getYsize()),
                0.0D, 1.0D);
        double lateral = normalizedProjection(impact.subtract(center), right, hitbox);

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

    static BodyPart fromHorizontalHitbox(AABB hitbox, Vec3 impact, Vec3 forward, Vec3 right) {
        Vec3 offset = impact.subtract(hitbox.getCenter());
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

    static Vec3 rightVector(float bodyYaw) {
        float yawRadians = bodyYaw * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.cos(yawRadians), 0.0D, -Mth.sin(yawRadians));
    }

    private static Vec3 forwardVector(float bodyYaw) {
        float yawRadians = bodyYaw * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yawRadians), 0.0D, Mth.cos(yawRadians));
    }

    private static boolean isHorizontal(Pose pose) {
        return pose == Pose.SWIMMING
                || pose == Pose.FALL_FLYING
                || pose == Pose.SPIN_ATTACK
                || pose == Pose.SLEEPING;
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
