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
        return switch (s.mode()) {
            case TAA_PRONE -> lying(t, s, s.taaHeading(), s.taaAim(), pitch);
            case TAA_ASSUMED -> lying(t, s, s.bodyYaw(), aim(s, s.bodyYaw()), pitch);
            case TAA_REORIENT -> {
                double progress = progress(s, cfg);
                float yaw = TaaPhaseMath.reorientYaw(s.taaAnchor(), s.taaTarget(), progress);
                double settle = TaaPhaseMath.reorientPoseWeight(progress);
                BodyLayout layout = BodyLayout.blend(
                        new BodyLayout[]{t.steady(gun, aim(s, yaw), pitch), t.steady(gun, 0.0D, pitch)},
                        new double[]{1.0D - settle, settle});
                yield new BodyPose(yaw, layout);
            }
            case TAA_ENTER -> {
                double progress = progress(s, cfg);
                ProneSegmentTables.Frame frame = t.enter(gun, progress);
                // taaAim already carries enterAimScale: TAA smooths the scaled angle.
                yield new BodyPose(s.taaAnchor(), withAim(t, frame.layout(), gun, s.taaAim(), pitch,
                        TaaPhaseMath.enterAimWeight(progress)));
            }
            case TAA_EXIT -> {
                double progress = progress(s, cfg);
                ProneSegmentTables.Frame frame = t.exit(gun, progress);
                yield new BodyPose(s.taaHeading(), withAim(t, frame.layout(), gun, aim(s, s.taaAnchor()), pitch,
                        TaaPhaseMath.exitAimWeight(progress)));
            }
            default -> null;
        };
    }

    /**
     * Heading to store in a new snapshot. TAA's displayed heading chases the mode's yaw with a
     * low-pass ({@code Motion.heading}) while lying prone (the anchor itself jumps when crawling
     * changes direction) and while getting up, so PRONE after PRONE and EXIT after ENTER, PRONE or
     * EXIT step towards it; every other case takes the mode's own yaw.
     *
     * @param draft the snapshot being captured; its own taaHeading is ignored
     * @param prev  the previous snapshot of the same player, may be null
     */
    public static float heading(ProneSample draft, ProneSample prev, ProneHitSettings cfg, ProneSegmentTables t) {
        float yaw = modeYaw(draft, cfg, t);
        boolean filtered = prev != null && switch (draft.mode()) {
            case TAA_PRONE -> prev.mode() == ProneMode.TAA_PRONE;
            // ENTER sets the heading every frame and PRONE and EXIT filter it, so all three keep it warm.
            case TAA_EXIT -> prev.mode() == ProneMode.TAA_ENTER || prev.mode() == ProneMode.TAA_PRONE
                    || prev.mode() == ProneMode.TAA_EXIT;
            default -> false;
        };
        return filtered ? TaaPhaseMath.headingStep(prev.taaHeading(), yaw) : yaw;
    }

    /**
     * Aim to store in a new snapshot whose heading is already set. TAA smooths the head-to-body
     * yaw with {@code Motion.angle} while entering (after scaling it by enterAimScale) and while
     * lying prone, so ENTER and PRONE step from the previous snapshot's aim when that one was
     * entering or prone too. Anything else starts at the target, as a fresh or long idle
     * {@code Motion} does; the other modes store the unsmoothed aim against their heading.
     *
     * @param draft the snapshot being captured, taaHeading set; its own taaAim is ignored
     * @param prev  the previous snapshot of the same player, may be null
     */
    public static float smoothedAim(ProneSample draft, ProneSample prev, ProneHitSettings cfg) {
        ProneMode mode = draft.mode();
        if (mode != ProneMode.TAA_ENTER && mode != ProneMode.TAA_PRONE) {
            return (float) aim(draft, draft.taaHeading());
        }
        double target = mode == ProneMode.TAA_ENTER
                ? aim(draft, draft.taaAnchor()) * TaaPhaseMath.enterAimScale(progress(draft, cfg))
                : aim(draft, draft.taaHeading());
        boolean filtered = prev != null
                && (prev.mode() == ProneMode.TAA_ENTER || prev.mode() == ProneMode.TAA_PRONE);
        return (float) (filtered ? TaaPhaseMath.aimStep(prev.taaAim(), target) : target);
    }

    /** Unfiltered body yaw of the snapshot's mode (spec 7.5 yaw column) that the heading chases. */
    private static float modeYaw(ProneSample s, ProneHitSettings cfg, ProneSegmentTables t) {
        return switch (s.mode()) {
            case TAA_PRONE, TAA_ENTER -> s.taaAnchor();
            case TAA_REORIENT -> TaaPhaseMath.reorientYaw(s.taaAnchor(), s.taaTarget(), progress(s, cfg));
            case TAA_EXIT -> t == null ? s.taaAnchor()
                    : TaaPhaseMath.exitYaw(s.taaAnchor(), s.bodyYaw(), t.exit(s.gun(), progress(s, cfg)).prone());
            default -> s.bodyYaw();
        };
    }

    /**
     * Bound on {@link LocalObb#reach} of every box {@link #resolve} can return with these settings,
     * for {@link ProneSegmentClip#reachRadius}.
     *
     * @param t the shared segment table, may be null
     */
    public static double localReach(ProneHitSettings cfg, ProneSegmentTables t) {
        double vanilla = VanillaCrawlLayout.reach(cfg.vanillaForwardShift());
        return t == null ? vanilla : Math.max(vanilla, t.reachBound());
    }

    /** TAA phase progress of the snapshot with the configured lag. */
    public static double progress(ProneSample s, ProneHitSettings cfg) {
        return TaaPhaseMath.progress(s.gameTime(), cfg.transitionLagTicks(), s.taaStart(), s.taaDuration());
    }

    private static BodyPose lying(ProneSegmentTables t, ProneSample s, float yaw, double aim, double pitch) {
        BodyLayout layout = t.steady(s.gun(), aim, pitch);
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
