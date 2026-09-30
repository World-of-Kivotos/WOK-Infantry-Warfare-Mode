package com.wok.bodyhealth.health;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class HitLocationResolverTest {
    /** Standing player box; body yaw 0 faces +Z, so the player's right side is -X. */
    private static final AABB STANDING = new AABB(-0.3D, 64.0D, -0.3D, 0.3D, 65.8D, 0.3D);
    private static final Vec3 RIGHT = HitLocationResolver.rightVector(0.0F);

    @Test
    void bodyYawZeroPutsTheRightSideOnNegativeX() {
        assertEquals(-1.0D, RIGHT.x, 1.0E-6D);
        assertEquals(0.0D, RIGHT.z, 1.0E-6D);
    }

    @Test
    void partitionsTheUprightBoxIntoSevenParts() {
        assertEquals(BodyPart.HEAD, vertical(0.0D, 65.6D, 0.3D));
        assertEquals(BodyPart.CHEST, vertical(0.0D, 65.2D, 0.3D));
        assertEquals(BodyPart.ABDOMEN, vertical(0.0D, 64.8D, 0.3D));
        assertEquals(BodyPart.RIGHT_ARM, vertical(-0.3D, 65.2D, 0.0D));
        assertEquals(BodyPart.LEFT_ARM, vertical(0.3D, 65.2D, 0.0D));
        assertEquals(BodyPart.RIGHT_LEG, vertical(-0.1D, 64.3D, 0.3D));
        assertEquals(BodyPart.LEFT_LEG, vertical(0.1D, 64.3D, 0.3D));
    }

    @Test
    void tracesAnAngledArrowIntoTheChestInsteadOfTheArm() {
        // The arrow is still two blocks away and 0.6 to the side when hurt runs.
        Vec3 start = new Vec3(0.6D, 65.2D, 2.3D);
        Vec3 motion = new Vec3(-0.9D, 0.0D, -3.0D);
        Vec3 impact = HitLocationResolver.traceImpact(STANDING, start, motion);

        assertEquals(0.3D, impact.z, 1.0E-6D);
        assertEquals(0.0D, impact.x, 1.0E-6D);
        assertEquals(BodyPart.CHEST, HitLocationResolver.fromVerticalHitbox(STANDING, impact, RIGHT));
    }

    @Test
    void grazingHitsInsideTheVanillaMarginSnapToTheNearestSurface() {
        Vec3 impact = HitLocationResolver.traceImpact(
                STANDING, new Vec3(0.45D, 65.2D, 3.0D), new Vec3(0.0D, 0.0D, -6.0D));

        assertEquals(new Vec3(0.3D, 65.2D, 0.3D), impact);
        assertEquals(BodyPart.LEFT_ARM, HitLocationResolver.fromVerticalHitbox(STANDING, impact, RIGHT));
    }

    @Test
    void stationarySourcesUseTheClosestPointOfTheBox() {
        // A ground-level mine or evoker fang next to the left foot.
        Vec3 impact = HitLocationResolver.traceImpact(
                STANDING, new Vec3(1.5D, 64.05D, 0.0D), Vec3.ZERO);

        assertEquals(new Vec3(0.3D, 64.05D, 0.0D), impact);
        assertEquals(BodyPart.LEFT_LEG, HitLocationResolver.fromVerticalHitbox(STANDING, impact, RIGHT));
    }

    @Test
    void aProjectileAlreadyInsideTheBoxHitsWhereItIs() {
        Vec3 inside = new Vec3(0.0D, 65.6D, 0.0D);
        assertEquals(inside, HitLocationResolver.traceImpact(STANDING, inside, new Vec3(0.0D, 0.0D, 3.0D)));
    }

    @Test
    void partitionsTheHorizontalBoxAlongTheBody() {
        AABB swimming = new AABB(-0.3D, 64.0D, -0.3D, 0.3D, 64.6D, 0.3D);
        Vec3 forward = new Vec3(0.0D, 0.0D, 1.0D);

        assertEquals(BodyPart.HEAD, HitLocationResolver.fromHorizontalHitbox(
                swimming, new Vec3(0.0D, 64.3D, 0.3D), forward, RIGHT));
        assertEquals(BodyPart.ABDOMEN, HitLocationResolver.fromHorizontalHitbox(
                swimming, new Vec3(0.0D, 64.6D, 0.0D), forward, RIGHT));
        assertEquals(BodyPart.LEFT_LEG, HitLocationResolver.fromHorizontalHitbox(
                swimming, new Vec3(0.1D, 64.3D, -0.3D), forward, RIGHT));
    }

    private static BodyPart vertical(double x, double y, double z) {
        return HitLocationResolver.fromVerticalHitbox(STANDING, new Vec3(x, y, z), RIGHT);
    }
}
