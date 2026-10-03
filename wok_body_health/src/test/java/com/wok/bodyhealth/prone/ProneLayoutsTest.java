package com.wok.bodyhealth.prone;

import org.junit.jupiter.api.Test;

import static com.wok.bodyhealth.prone.ProneTestSupport.assertLayout;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

final class ProneLayoutsTest {
    private static final ProneHitSettings CFG = ProneHitSettings.DEFAULTS;

    @Test
    void lyingProneUsesTheFilteredHeading() {
        ProneTestSupport.SampleBuilder b = builder(ProneMode.TAA_PRONE);
        b.heading = 30.0F;
        b.anchor = 10.0F;
        b.bodyYaw = -50.0F;
        b.yRot = 70.0F;
        ProneLayouts.BodyPose pose = ProneLayouts.resolve(b.build(), CFG, tables());

        assertEquals(30.0F, pose.yawDeg(), 1.0E-6F);
        // aim = wrap(yRot - heading) = 40, column 22; pitch 0 is row 4.
        assertSame(tables().grid(true, 22, 4), pose.layout());
    }

    @Test
    void assumedAndVanillaPosesUseTheSimulatedBodyYaw() {
        ProneTestSupport.SampleBuilder assumed = builder(ProneMode.TAA_ASSUMED);
        assumed.bodyYaw = -50.0F;
        assumed.heading = 30.0F;
        assertEquals(-50.0F, ProneLayouts.resolve(assumed.build(), CFG, tables()).yawDeg(), 1.0E-6F);

        ProneTestSupport.SampleBuilder vanilla = builder(ProneMode.VANILLA_CRAWL);
        vanilla.bodyYaw = 120.0F;
        vanilla.heading = 30.0F;
        assertEquals(120.0F, ProneLayouts.resolve(vanilla.build(), CFG, tables()).yawDeg(), 1.0E-6F);
    }

    @Test
    void crawlingSwapsTheLimbsForTheCrawlEnvelope() {
        ProneTestSupport.SampleBuilder b = builder(ProneMode.TAA_PRONE);
        b.crawl = 0.5F;
        BodyLayout layout = ProneLayouts.resolve(b.build(), CFG, tables()).layout();
        BodyLayout envelope = tables().crawlEnvelope(true);
        BodyLayout steady = tables().steady(true, 0.0D, 0.0D);
        for (SegmentId id : new SegmentId[]{SegmentId.RIGHT_ARM, SegmentId.LEFT_ARM,
                SegmentId.RIGHT_LEG, SegmentId.LEFT_LEG}) {
            assertSame(envelope.get(id), layout.get(id), id.name());
        }
        assertSame(steady.get(SegmentId.HEAD), layout.get(SegmentId.HEAD));
        assertSame(steady.get(SegmentId.TORSO), layout.get(SegmentId.TORSO));

        b.crawl = 0.49F;
        assertSame(steady, ProneLayouts.resolve(b.build(), CFG, tables()).layout());
    }

    @Test
    void vanillaCrawlIsShiftedForTaczTweaks() {
        ProneLayouts.BodyPose pose = ProneLayouts.resolve(builder(ProneMode.VANILLA_CRAWL).build(), CFG, null);
        assertNotNull(pose, "the vanilla layout does not need the TAA table");
        BodyLayout plain = VanillaCrawlLayout.envelope(0.0D);
        for (SegmentId id : SegmentId.values()) {
            assertEquals(plain.get(id).c().x - 0.4D, pose.layout().get(id).c().x, 1.0E-12D);
            assertEquals(plain.get(id).c().z, pose.layout().get(id).c().z, 1.0E-12D);
        }
        assertEquals(0.5725D, plain.get(SegmentId.HEAD).c().x, 1.0E-12D);
    }

    @Test
    void enterEndsInTheAimedSteadyPose() {
        ProneTestSupport.SampleBuilder b = builder(ProneMode.TAA_ENTER);
        b.anchor = 20.0F;
        b.yRot = 50.0F;
        b.xRot = 10.0F;
        b.gameTime = b.taaStart + b.taaDuration;
        ProneLayouts.BodyPose pose = ProneLayouts.resolve(b.build(), CFG, tables());

        assertEquals(20.0F, pose.yawDeg(), 1.0E-6F);
        assertLayout(tables().steady(true, 30.0D, 10.0D), pose.layout(), 3.0E-3D);
    }

    @Test
    void enterStartsStanding() {
        ProneTestSupport.SampleBuilder b = builder(ProneMode.TAA_ENTER);
        b.yRot = 60.0F;
        b.gameTime = b.taaStart;
        BodyLayout layout = ProneLayouts.resolve(b.build(), CFG, tables()).layout();
        assertSame(tables().enterFrame(true, 0).layout(), layout);
    }

    @Test
    void reorientSettlesOnTheNeutralPose() {
        ProneTestSupport.SampleBuilder b = builder(ProneMode.TAA_REORIENT);
        b.anchor = 0.0F;
        b.target = 90.0F;
        b.yRot = 120.0F;
        b.xRot = -25.0F;
        b.taaDuration = 11;
        b.gameTime = b.taaStart + 10;
        ProneLayouts.BodyPose pose = ProneLayouts.resolve(b.build(), CFG, tables());

        assertSame(tables().steady(true, 0.0D, -25.0D), pose.layout());
        double t = 10.0D / 11.0D;
        assertEquals(TaaPhaseMath.reorientYaw(0.0F, 90.0F, t), pose.yawDeg(), 1.0E-6F);

        // At the start the turn has not begun and the pose still carries the full aim.
        b.gameTime = b.taaStart;
        ProneLayouts.BodyPose start = ProneLayouts.resolve(b.build(), CFG, tables());
        assertEquals(0.0F, start.yawDeg(), 1.0E-6F);
        assertSame(tables().steady(true, 120.0D, -25.0D), start.layout());
    }

    @Test
    void exitEndsFacingTheBodyYaw() {
        ProneTestSupport.SampleBuilder b = builder(ProneMode.TAA_EXIT);
        b.anchor = 10.0F;
        b.bodyYaw = 70.0F;
        b.taaDuration = 15;
        b.gameTime = b.taaStart + 15;
        ProneLayouts.BodyPose end = ProneLayouts.resolve(b.build(), CFG, tables());
        assertEquals(70.0F, end.yawDeg(), 1.0E-4F);
        assertSame(tables().exitFrame(true, 16).layout(), end.layout());

        b.gameTime = b.taaStart;
        assertEquals(10.0F, ProneLayouts.resolve(b.build(), CFG, tables()).yawDeg(), 1.0E-4F);
    }

    @Test
    void transitionLagDelaysProgress() {
        ProneTestSupport.SampleBuilder b = builder(ProneMode.TAA_ENTER);
        b.gameTime = b.taaStart + 1;
        ProneHitSettings lagged = new ProneHitSettings(true, 0.0625D, 0.03125D, 0.015625D, 1, -0.4D, true, false);
        assertSame(tables().enterFrame(true, 0).layout(), ProneLayouts.resolve(b.build(), lagged, tables()).layout());
    }

    @Test
    void notProneOrNoTableFallsBack() {
        assertNull(ProneLayouts.resolve(builder(ProneMode.NONE).build(), CFG, tables()));
        for (ProneMode mode : new ProneMode[]{ProneMode.TAA_PRONE, ProneMode.TAA_ASSUMED, ProneMode.TAA_ENTER,
                ProneMode.TAA_EXIT, ProneMode.TAA_REORIENT}) {
            assertNull(ProneLayouts.resolve(builder(mode).build(), CFG, null), mode.name());
        }
    }

    @Test
    void headingLowPassesOnlyWhileStayingProne() {
        ProneTestSupport.SampleBuilder prev = builder(ProneMode.TAA_PRONE);
        prev.heading = 0.0F;
        ProneTestSupport.SampleBuilder draft = builder(ProneMode.TAA_PRONE);
        draft.anchor = 60.0F;
        draft.heading = 999.0F;

        assertEquals(TaaPhaseMath.headingStep(0.0F, 60.0F),
                ProneLayouts.heading(draft.build(), prev.build(), CFG, tables()), 1.0E-6F);
        assertEquals(60.0F, ProneLayouts.heading(draft.build(), null, CFG, tables()), 1.0E-6F);
        prev.mode = ProneMode.TAA_ENTER;
        assertEquals(60.0F, ProneLayouts.heading(draft.build(), prev.build(), CFG, tables()), 1.0E-6F);

        ProneTestSupport.SampleBuilder crawl = builder(ProneMode.VANILLA_CRAWL);
        crawl.bodyYaw = -15.0F;
        assertEquals(-15.0F, ProneLayouts.heading(crawl.build(), prev.build(), CFG, tables()), 1.0E-6F);
    }

    @Test
    void taaPhasesMapToModes() {
        assertEquals(ProneMode.TAA_ENTER, ProneMode.fromTaaPhase(1));
        assertEquals(ProneMode.TAA_PRONE, ProneMode.fromTaaPhase(2));
        assertEquals(ProneMode.TAA_EXIT, ProneMode.fromTaaPhase(3));
        assertEquals(ProneMode.TAA_REORIENT, ProneMode.fromTaaPhase(4));
        assertEquals(ProneMode.TAA_PRONE, ProneMode.fromTaaPhase(9));
    }

    private static ProneTestSupport.SampleBuilder builder(ProneMode mode) {
        ProneTestSupport.SampleBuilder b = new ProneTestSupport.SampleBuilder();
        b.mode = mode;
        return b;
    }

    private static ProneSegmentTables tables() {
        return ProneTestSupport.tables();
    }
}
