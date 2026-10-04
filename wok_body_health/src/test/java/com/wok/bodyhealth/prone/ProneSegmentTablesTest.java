package com.wok.bodyhealth.prone;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import static com.wok.bodyhealth.prone.ProneTestSupport.assertLayout;
import static com.wok.bodyhealth.prone.ProneTestSupport.extents;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProneSegmentTablesTest {
    private static final double TABLE_TOLERANCE = 2.0E-3D;
    private static ProneSegmentTables tables;
    private static JsonObject raw;

    @BeforeAll
    static void load() throws IOException {
        tables = ProneTestSupport.tables();
        try (InputStream in = ProneSegmentTables.class.getResourceAsStream(ProneSegmentTables.RESOURCE)) {
            assertNotNull(in);
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                raw = JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
    }

    @Test
    void resourceHoldsEveryPoseFrameAndEnvelope() {
        for (String variant : new String[]{"gun", "empty"}) {
            assertEquals(333, raw.getAsJsonObject("steady").getAsJsonObject(variant).size());
            assertEquals(17, raw.getAsJsonObject("enter").getAsJsonArray(variant).size());
            assertEquals(17, raw.getAsJsonObject("exit").getAsJsonArray(variant).size());
            assertEquals(6, raw.getAsJsonObject("crawlEnvelopeAABB").getAsJsonObject(variant).size());
        }
        for (int i = 0; i < ProneSegmentTables.AIM_COUNT; i++) {
            for (int j = 0; j < ProneSegmentTables.PITCHES.length; j++) {
                assertTrue(raw.getAsJsonObject("steady").getAsJsonObject("gun")
                        .has(ProneSegmentTables.steadyKey(i, j)), ProneSegmentTables.steadyKey(i, j));
            }
        }
        assertNotNull(ProneSegmentTables.shared());
        assertSame(ProneSegmentTables.shared(), ProneSegmentTables.shared());
    }

    @Test
    void malformedTablesAreRejected() {
        assertThrows(IOException.class, () -> ProneSegmentTables.load(stream("{}")));
        assertThrows(IOException.class, () -> ProneSegmentTables.load(stream("not json")));
    }

    @Test
    void everyStoredBoxIsOrthogonal() {
        for (boolean gun : new boolean[]{true, false}) {
            for (int i = 0; i < ProneSegmentTables.AIM_COUNT; i++) {
                for (int j = 0; j < ProneSegmentTables.PITCHES.length; j++) {
                    assertOrthogonal(tables.grid(gun, i, j));
                }
            }
            for (int k = 0; k < ProneSegmentTables.FRAME_COUNT; k++) {
                assertOrthogonal(tables.enterFrame(gun, k).layout());
                assertOrthogonal(tables.exitFrame(gun, k).layout());
            }
        }
    }

    @Test
    void steadyGunPoseMatchesTheReferenceExtents() {
        BodyLayout pose = tables.steady(true, 0.0D, 0.0D);
        assertExtents(pose.get(SegmentId.HEAD), 0.173, 0.642, -0.234, 0.234, 0.150, 0.619);
        assertExtents(pose.get(SegmentId.TORSO), -0.296, 0.407, -0.234, 0.234, 0.033, 0.267);
        assertExtents(pose.get(SegmentId.RIGHT_ARM), 0.173, 0.876, 0.234, 0.469, 0.033, 0.267);
        assertExtents(pose.get(SegmentId.LEFT_ARM), 0.158, 0.809, -0.508, 0.211, 0.005, 0.298);
        assertExtents(pose.get(SegmentId.RIGHT_LEG), -1.007, -0.285, -0.005, 0.291, 0.033, 0.267);
        assertExtents(pose.get(SegmentId.LEFT_LEG), -1.007, -0.285, -0.291, 0.005, 0.033, 0.267);

        // The torso's v runs neck to hips (backwards), which the chest/abdomen split relies on.
        assertTrue(pose.get(SegmentId.TORSO).v().x < -0.35D);
    }

    @Test
    void steadyEmptyHandArmsAreSymmetric() {
        BodyLayout pose = tables.steady(false, 0.0D, 0.0D);
        assertExtents(pose.get(SegmentId.RIGHT_ARM), 0.145, 0.885, 0.221, 0.538, 0.003, 0.287);
        assertExtents(pose.get(SegmentId.LEFT_ARM), 0.145, 0.885, -0.538, -0.221, 0.003, 0.287);
    }

    @Test
    void gridPointsReturnTheStoredPose() {
        for (boolean gun : new boolean[]{true, false}) {
            for (int i : new int[]{0, 5, 18, 27, 35}) {
                for (int j = 0; j < ProneSegmentTables.PITCHES.length; j++) {
                    assertSame(tables.grid(gun, i, j),
                            tables.steady(gun, i * 10 - 180, ProneSegmentTables.PITCHES[j]), "aim index " + i);
                }
            }
            // +180 wraps onto the -180 column; both columns hold the same seam pose.
            assertLayout(tables.grid(gun, 36, 4), tables.steady(gun, 180.0D, 0.0D), TABLE_TOLERANCE);
            // Pitch beyond TAA's +/-70 clamp stays on the edge row.
            assertSame(tables.grid(gun, 18, 8), tables.steady(gun, 0.0D, 85.0D));
            assertSame(tables.grid(gun, 18, 0), tables.steady(gun, 0.0D, -85.0D));
        }
    }

    @Test
    void offGridPosesLieBetweenTheirNeighbours() {
        BodyLayout mid = tables.steady(true, 5.0D, -17.5D);
        // aim 0 / 10 are columns 18 / 19, pitch -25 / -10 rows 2 / 3.
        BodyLayout[] corners = {tables.grid(true, 18, 2), tables.grid(true, 19, 2),
                tables.grid(true, 18, 3), tables.grid(true, 19, 3)};
        boolean differs = false;
        for (SegmentId id : SegmentId.values()) {
            double[] value = {mid.get(id).c().x, mid.get(id).c().y, mid.get(id).c().z};
            for (int axis = 0; axis < 3; axis++) {
                double min = Double.POSITIVE_INFINITY;
                double max = Double.NEGATIVE_INFINITY;
                for (BodyLayout corner : corners) {
                    double[] c = {corner.get(id).c().x, corner.get(id).c().y, corner.get(id).c().z};
                    min = Math.min(min, c[axis]);
                    max = Math.max(max, c[axis]);
                }
                assertTrue(value[axis] >= min - 1.0E-12D && value[axis] <= max + 1.0E-12D, id + " axis " + axis);
                differs |= max - min > 1.0E-6D;
            }
        }
        assertTrue(differs, "the four neighbours should not all be equal");
        for (BodyLayout corner : corners) {
            assertNotSame(corner, mid);
        }
    }

    @Test
    void transitionEndsMeetTheSteadyPose() {
        BodyLayout steady = tables.steady(true, 0.0D, 0.0D);
        assertLayout(steady, tables.enter(true, 1.0D).layout(), TABLE_TOLERANCE);
        assertLayout(steady, tables.exit(true, 0.0D).layout(), TABLE_TOLERANCE);

        // Empty-handed, TAA's transition ground clamp also counts the arms as contacts and lifts
        // the whole body by about 0.013 compared with the steady pose; k7 keeps that as rendered.
        BodyLayout empty = tables.steady(false, 0.0D, 0.0D);
        assertLayout(empty, tables.enter(false, 1.0D).layout(), 0.015D);
        assertLayout(empty, tables.exit(false, 0.0D).layout(), 0.015D);

        for (boolean gun : new boolean[]{true, false}) {
            assertEquals(1.0D, tables.enter(gun, 1.0D).prone(), 1.0E-9D);
            assertEquals(1.0D, tables.exit(gun, 0.0D).prone(), 1.0E-9D);
        }
    }

    @Test
    void transitionsStartAndEndStanding() {
        for (boolean gun : new boolean[]{true, false}) {
            assertEquals(1.646D, tables.enter(gun, 0.0D).layout().get(SegmentId.HEAD).c().z, TABLE_TOLERANCE);
            assertEquals(1.646D, tables.exit(gun, 1.0D).layout().get(SegmentId.HEAD).c().z, TABLE_TOLERANCE);
            assertEquals(0.0D, tables.enter(gun, 0.0D).prone(), 1.0E-9D);
            assertEquals(0.0D, tables.exit(gun, 1.0D).prone(), 1.0E-9D);
        }
    }

    @Test
    void keyframesInterpolateLinearly() {
        ProneSegmentTables.Frame a = tables.enterFrame(true, 4);
        ProneSegmentTables.Frame b = tables.enterFrame(true, 5);
        ProneSegmentTables.Frame mid = tables.enter(true, 4.25D / 16.0D);
        assertEquals(a.prone() * 0.75D + b.prone() * 0.25D, mid.prone(), 1.0E-12D);
        for (SegmentId id : SegmentId.values()) {
            assertEquals(a.layout().get(id).c().z * 0.75D + b.layout().get(id).c().z * 0.25D,
                    mid.layout().get(id).c().z, 1.0E-12D);
        }
        assertSame(tables.enterFrame(true, 8), tables.enter(true, 0.5D));
        assertSame(tables.enterFrame(true, 0), tables.enter(true, -3.0D));
        assertSame(tables.exitFrame(true, 16), tables.exit(true, 7.0D));
    }

    private static void assertOrthogonal(BodyLayout layout) {
        for (SegmentId id : SegmentId.values()) {
            LocalObb box = layout.get(id);
            assertTrue(Math.abs(box.u().dot(box.v())) < 1.0E-6D, id + " u.v");
            assertTrue(Math.abs(box.u().dot(box.w())) < 1.0E-6D, id + " u.w");
            assertTrue(Math.abs(box.v().dot(box.w())) < 1.0E-6D, id + " v.w");
        }
    }

    private static void assertExtents(LocalObb box, double f0, double f1, double s0, double s1, double y0, double y1) {
        double[] e = extents(box);
        double[] expected = {f0, f1, s0, s1, y0, y1};
        String[] names = {"fMin", "fMax", "sMin", "sMax", "yMin", "yMax"};
        for (int i = 0; i < 6; i++) {
            assertEquals(expected[i], e[i], TABLE_TOLERANCE, names[i]);
        }
    }

    private static InputStream stream(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }
}
