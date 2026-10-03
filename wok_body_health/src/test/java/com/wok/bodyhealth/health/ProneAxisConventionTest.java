package com.wok.bodyhealth.health;

import com.wok.bodyhealth.prone.ProneGeometry;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The prone segments must use the same body axes as the upright resolver. */
final class ProneAxisConventionTest {
    @Test
    void proneRightVectorMatchesTheResolver() {
        for (float yaw : new float[]{0.0F, 33.3F, 45.0F, 90.0F, 180.0F, -135.0F, 271.5F}) {
            Vec3 expected = HitLocationResolver.rightVector(yaw);
            Vec3 actual = ProneGeometry.right(yaw);
            // Mth.cos/sin use a lookup table, ProneGeometry the exact functions.
            assertEquals(expected.x, actual.x, 1.0E-3D, "yaw " + yaw);
            assertEquals(expected.y, actual.y, 1.0E-3D, "yaw " + yaw);
            assertEquals(expected.z, actual.z, 1.0E-3D, "yaw " + yaw);
        }
    }
}
