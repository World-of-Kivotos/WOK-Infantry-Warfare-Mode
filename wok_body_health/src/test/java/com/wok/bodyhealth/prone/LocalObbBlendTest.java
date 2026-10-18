package com.wok.bodyhealth.prone;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static com.wok.bodyhealth.prone.ProneTestSupport.assertBox;
import static com.wok.bodyhealth.prone.ProneTestSupport.assertVec;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LocalObbBlendTest {
    private static final LocalObb A = LocalObb.ofExtents(-0.3D, 0.4D, -0.2D, 0.2D, 0.0D, 0.25D);
    /** A smaller box turned 40 degrees about the up axis and moved. */
    private static final LocalObb B = new LocalObb(new Vec3(0.5D, -0.1D, 0.3D),
            turn(new Vec3(0.0D, 0.15D, 0.0D), 40.0D),
            turn(new Vec3(-0.25D, 0.0D, 0.0D), 40.0D),
            new Vec3(0.0D, 0.0D, 0.1D));

    @Test
    void endpointWeightsReturnTheEndpoints() {
        assertSame(A, LocalObb.blend(new LocalObb[]{A, B}, new double[]{1.0D, 0.0D}));
        assertSame(B, LocalObb.blend(new LocalObb[]{A, B}, new double[]{0.0D, 1.0D}));
        // The general path reproduces an already orthogonal box.
        assertBox(A, LocalObb.blend(new LocalObb[]{A, A}, new double[]{0.5D, 0.5D}), 1.0E-12D);
    }

    @Test
    void blendIsOrthogonalWithBlendedLengths() {
        LocalObb mixed = LocalObb.blend(new LocalObb[]{A, B}, new double[]{0.7D, 0.3D});

        assertTrue(Math.abs(mixed.u().dot(mixed.v())) < 1.0E-12D);
        assertTrue(Math.abs(mixed.u().dot(mixed.w())) < 1.0E-12D);
        assertTrue(Math.abs(mixed.v().dot(mixed.w())) < 1.0E-12D);
        assertEquals(0.7D * A.u().length() + 0.3D * B.u().length(), mixed.u().length(), 1.0E-12D);
        assertEquals(0.7D * A.v().length() + 0.3D * B.v().length(), mixed.v().length(), 1.0E-12D);
        assertEquals(0.7D * A.w().length() + 0.3D * B.w().length(), mixed.w().length(), 1.0E-12D);
        assertVec(A.c().scale(0.7D).add(B.c().scale(0.3D)), mixed.c(), 1.0E-12D);
        // The first axis follows the blended u direction.
        Vec3 direction = A.u().scale(0.7D).add(B.u().scale(0.3D)).normalize();
        assertVec(direction, mixed.u().normalize(), 1.0E-12D);
    }

    @Test
    void negativeWeightsLayerADelta() {
        // base + (B - A) moves the centre by B - A.
        LocalObb base = A.shiftF(0.2D);
        LocalObb layered = LocalObb.blend(new LocalObb[]{base, B, A}, new double[]{1.0D, 1.0D, -1.0D});
        assertVec(B.c().add(0.2D, 0.0D, 0.0D), layered.c(), 1.0E-12D);
        assertEquals(B.u().length(), layered.u().length(), 1.0E-12D);
    }

    @Test
    void degenerateBlendFallsBackToTheHeaviestPart() {
        LocalObb wide = boxWithU(new Vec3(0.0D, 0.6D, 0.0D));
        LocalObb flipped = boxWithU(new Vec3(0.0D, -0.4D, 0.0D));
        // 0.4 * 0.6 - 0.6 * 0.4 cancels u exactly.
        assertSame(flipped, LocalObb.blend(new LocalObb[]{wide, flipped}, new double[]{0.4D, 0.6D}));

        LocalObb narrow = boxWithU(new Vec3(0.0D, 0.4D, 0.0D));
        LocalObb wideFlipped = boxWithU(new Vec3(0.0D, -0.6D, 0.0D));
        assertSame(narrow, LocalObb.blend(new LocalObb[]{narrow, wideFlipped}, new double[]{0.6D, 0.4D}));
    }

    @Test
    void orthonormalizedRemovesSkewAndKeepsLengths() {
        LocalObb skewed = new LocalObb(Vec3.ZERO, new Vec3(0.0D, 0.234D, 0.001D),
                new Vec3(-0.352D, 0.001D, 0.0D), new Vec3(0.001D, 0.0D, 0.117D));
        LocalObb fixed = skewed.orthonormalized();
        assertTrue(Math.abs(fixed.u().dot(fixed.v())) < 1.0E-12D);
        assertTrue(Math.abs(fixed.v().dot(fixed.w())) < 1.0E-12D);
        assertEquals(skewed.u().length(), fixed.u().length(), 1.0E-12D);
        assertEquals(skewed.v().length(), fixed.v().length(), 1.0E-12D);
        assertEquals(skewed.w().length(), fixed.w().length(), 1.0E-12D);
    }

    @Test
    void extentsBoxKeepsTheTorsoAxisBackwards() {
        LocalObb torso = LocalObb.ofExtents(-0.296D, 0.407D, -0.234D, 0.234D, 0.183D, 0.417D);
        assertVec(new Vec3(0.0555D, 0.0D, 0.3D), torso.c(), 1.0E-12D);
        assertVec(new Vec3(0.0D, 0.234D, 0.0D), torso.u(), 1.0E-12D);
        assertVec(new Vec3(-0.3515D, 0.0D, 0.0D), torso.v(), 1.0E-12D);
        assertVec(new Vec3(0.0D, 0.0D, 0.117D), torso.w(), 1.0E-12D);
    }

    @Test
    void mismatchedWeightsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> LocalObb.blend(new LocalObb[]{A, B}, new double[]{1.0D}));
        assertThrows(IllegalArgumentException.class, () -> LocalObb.blend(new LocalObb[0], new double[0]));
    }

    private static LocalObb boxWithU(Vec3 u) {
        return new LocalObb(Vec3.ZERO, u, new Vec3(-0.2D, 0.0D, 0.0D), new Vec3(0.0D, 0.0D, 0.1D));
    }

    /** Turns a body-local vector about the up (y) axis. */
    private static Vec3 turn(Vec3 v, double degrees) {
        double r = Math.toRadians(degrees);
        return new Vec3(v.x * Math.cos(r) - v.y * Math.sin(r), v.x * Math.sin(r) + v.y * Math.cos(r), v.z);
    }
}
