package com.wok.bodyhealth.prone;

import net.minecraft.util.Mth;

/** Turns a snapshot into the body yaw and body-local layout to test against (spec 7.5). */
public final class ProneLayouts {
    /** Crawl weight from which the limbs switch to the crawl-cycle envelope. */
    private static final float CRAWL_ENVELOPE_WEIGHT = 0.5F;
    private static final double PITCH_LIMIT = 70.0D;

    public record BodyPose(float yawDeg, BodyLayout layout) {
    }

    /**
     * @param t the shared segment table, may be null; TAA modes then resolve to null
     * @return null when the original hitbox should be used
     */
    public static BodyPose resolve(ProneSample s, ProneHitSettings cfg, ProneSegmentTables t) {
        if (s.mode() == ProneMode.NONE) {
            return null;
        }
        if (s.mode() == ProneMode.VANILLA_CRAWL) {
            return new BodyPose(s.bodyYaw(), VanillaCrawlLayout.envelope(cfg.vanillaForwardShift()));
        }
        if (t == null) {
            return null;
        }

        boolean gun = s.gun();
        double pitch = pitch(s);
        switch (s.mode()) {
            case TAA_PRONE:
                return lying(t, s, s.taaHeading(), pitch);
            case TAA_ASSUMED:
                return lying(t, s, s.bodyYaw(), pitch);
            case TAA_REORIENT: {
                double progress = progress(s, cfg);
                float yaw = TaaPhaseMath.reorientYaw(s.taaAnchor(), s.taaTarget(), progress);
                double settle = TaaPhaseMath.reorientPoseWeight(progress);
                BodyLayout layout = BodyLayout.blend(
                        new BodyLayout[]{t.steady(gun, aim(s, yaw), pitch), t.steady(gun, 0.0D, pitch)},
                        new double[]{1.0D - settle, settle});
                return new BodyPose(yaw, layout);
            }
            case TAA_ENTER: {
                double progress = progress(s, cfg);
                ProneSegmentTables.Frame frame = t.enter(gun, progress);
                float yaw = s.taaAnchor();
                double aim = aim(s, yaw) * TaaPhaseMath.enterAimScale(progress);
                return new BodyPose(yaw, withAim(t, frame.layout(), gun, aim, pitch,
                        TaaPhaseMath.enterAimWeight(progress)));
            }
            case TAA_EXIT: {
                double progress = progress(s, cfg);
                ProneSegmentTables.Frame frame = t.exit(gun, progress);
                float yaw = TaaPhaseMath.exitYaw(s.taaAnchor(), s.bodyYaw(), frame.prone());
                return new BodyPose(yaw, withAim(t, frame.layout(), gun, aim(s, s.taaAnchor()), pitch,
                        TaaPhaseMath.exitAimWeight(progress)));
            }
            default:
                return null;
        }
    }

    /**
     * Heading to store in a new snapshot. While lying prone TAA's displayed heading chases the
     * anchor with a low-pass (the anchor itself jumps when crawling changes direction), so
     * PRONE after PRONE steps towards the anchor; every other case takes the mode's own yaw.
     *
     * @param draft the snapshot being captured; its own taaHeading is ignored
     * @param prev  the previous snapshot of the same player, may be null
     */
    public static float heading(ProneSample draft, ProneSample prev, ProneHitSettings cfg, ProneSegmentTables t) {
        if (draft.mode() == ProneMode.TAA_PRONE) {
            return prev != null && prev.mode() == ProneMode.TAA_PRONE
                    ? TaaPhaseMath.headingStep(prev.taaHeading(), draft.taaAnchor())
                    : draft.taaAnchor();
        }
        return yaw(draft, cfg, t);
    }

    /** Body yaw the given snapshot is drawn with (spec 7.5 yaw column). */
    public static float yaw(ProneSample s, ProneHitSettings cfg, ProneSegmentTables t) {
        return switch (s.mode()) {
            case TAA_PRONE -> s.taaHeading();
            case TAA_ENTER -> s.taaAnchor();
            case TAA_REORIENT -> TaaPhaseMath.reorientYaw(s.taaAnchor(), s.taaTarget(), progress(s, cfg));
            case TAA_EXIT -> t == null ? s.taaAnchor()
                    : TaaPhaseMath.exitYaw(s.taaAnchor(), s.bodyYaw(), t.exit(s.gun(), progress(s, cfg)).prone());
            default -> s.bodyYaw();
        };
    }

    /** TAA phase progress of the snapshot with the configured lag. */
    public static double progress(ProneSample s, ProneHitSettings cfg) {
        return TaaPhaseMath.progress(s.gameTime(), cfg.transitionLagTicks(), s.taaStart(), s.taaDuration());
    }

    private static BodyPose lying(ProneSegmentTables t, ProneSample s, float yaw, double pitch) {
        BodyLayout layout = t.steady(s.gun(), aim(s, yaw), pitch);
        if (s.crawlWeight() >= CRAWL_ENVELOPE_WEIGHT) {
            layout = layout.withLimbs(t.crawlEnvelope(s.gun()));
        }
        return new BodyPose(yaw, layout);
    }

    /** {@code base + weight * (steady(aim, pitch) - steady(0, 0))}, the aim offset TAA layers on transitions. */
    private static BodyLayout withAim(ProneSegmentTables t, BodyLayout base, boolean gun,
                                      double aim, double pitch, double weight) {
        if (weight == 0.0D) {
            return base;
        }
        return BodyLayout.blend(
                new BodyLayout[]{base, t.steady(gun, aim, pitch), t.steady(gun, 0.0D, 0.0D)},
                new double[]{1.0D, weight, -weight});
    }

    /** Head yaw relative to the body: TAA's {@code wrapDegrees(yRot - anchor)}. */
    private static double aim(ProneSample s, float yaw) {
        return Mth.wrapDegrees((double) s.yRot() - yaw);
    }

    private static double pitch(ProneSample s) {
        return Mth.clamp(s.xRot(), -PITCH_LIMIT, PITCH_LIMIT);
    }

    private ProneLayouts() {
    }
}
