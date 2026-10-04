package com.wok.bodyhealth.prone;

import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class ProneTestSupport {
    static final ProneHitSettings NO_MARGIN = new ProneHitSettings(true, 0.0D, 0.0D, 0.0D, 0, -0.4D, true, false);

    private static ProneSegmentTables tables;

    static synchronized ProneSegmentTables tables() {
        if (tables == null) {
            try (InputStream in = ProneSegmentTables.class.getResourceAsStream(ProneSegmentTables.RESOURCE)) {
                assertNotNull(in, "segment table resource");
                tables = ProneSegmentTables.load(in);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return tables;
    }

    /** Steady prone pose, TaCZ gun, aim 0, pitch 0, facing +Z at (0, 64, 0). */
    static WorldObb[] steadyGunAtOrigin(ProneHitSettings cfg) {
        return ProneGeometry.place(
                new ProneLayouts.BodyPose(0.0F, tables().steady(true, 0.0D, 0.0D)), 0.0D, 64.0D, 0.0D, cfg);
    }

    static void assertVec(Vec3 expected, Vec3 actual, double tolerance) {
        assertEquals(expected.x, actual.x, tolerance, "x");
        assertEquals(expected.y, actual.y, tolerance, "y");
        assertEquals(expected.z, actual.z, tolerance, "z");
    }

    static void assertBox(LocalObb expected, LocalObb actual, double tolerance) {
        assertVec(expected.c(), actual.c(), tolerance);
        assertVec(expected.u(), actual.u(), tolerance);
        assertVec(expected.v(), actual.v(), tolerance);
        assertVec(expected.w(), actual.w(), tolerance);
    }

    static void assertLayout(BodyLayout expected, BodyLayout actual, double tolerance) {
        for (SegmentId id : SegmentId.values()) {
            LocalObb e = expected.get(id);
            LocalObb a = actual.get(id);
            try {
                assertBox(e, a, tolerance);
            } catch (AssertionError error) {
                throw new AssertionError(id + ": " + error.getMessage(), error);
            }
        }
    }

    /** Body-local AABB of a box as {fMin, fMax, sMin, sMax, yMin, yMax}. */
    static double[] extents(LocalObb box) {
        double ef = Math.abs(box.u().x) + Math.abs(box.v().x) + Math.abs(box.w().x);
        double es = Math.abs(box.u().y) + Math.abs(box.v().y) + Math.abs(box.w().y);
        double ey = Math.abs(box.u().z) + Math.abs(box.v().z) + Math.abs(box.w().z);
        return new double[]{box.c().x - ef, box.c().x + ef, box.c().y - es, box.c().y + es,
                box.c().z - ey, box.c().z + ey};
    }

    static final class SampleBuilder {
        long gameTime = 1000L;
        double x;
        double y = 64.0D;
        double z;
        float yRot;
        float xRot;
        float bodyYaw;
        ProneMode mode = ProneMode.TAA_PRONE;
        long taaStart = 900L;
        int taaDuration = 17;
        float anchor;
        float target;
        float heading;
        float aim;
        boolean gun = true;
        float crawl;

        ProneSample build() {
            return new ProneSample(gameTime, x, y, z, 0.0D, 0.0D, 0.0D, yRot, xRot, bodyYaw,
                    mode, taaStart, taaDuration, anchor, target, heading, aim, gun, crawl);
        }
    }

    private ProneTestSupport() {
    }
}
