package com.wok.bodyhealth.prone;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

/** Builds a {@link ProneSample} from a live server player (spec 10). */
public final class ProneSampler {
    /**
     * @param velocity    TaCZ-style velocity to store with the snapshot
     * @param crawlWeight tracker's crawl weight, 0..1
     * @param bodyYaw     simulated on-screen body yaw
     * @param prev        the player's previous snapshot, may be null; drives the PRONE heading filter
     */
    public static ProneSample capture(ServerPlayer p, long gameTime, Vec3 velocity, float crawlWeight,
                                      float bodyYaw, ProneSample prev) {
        ProneMode mode = ProneMode.NONE;
        long taaStart = 0L;
        int taaDuration = 0;
        float taaAnchor = 0.0F;
        float taaTarget = 0.0F;
        if (isLandProne(p)) {
            if (TaaProneBridge.available()) {
                TaaProneBridge.State state = TaaProneBridge.read(p);
                if (state != null) {
                    mode = ProneMode.fromTaaPhase(state.phase());
                    taaStart = state.start();
                    taaDuration = state.duration();
                    taaAnchor = state.anchor();
                    taaTarget = state.target();
                } else {
                    // No TAA state: a one-block gap crawl drawn by vanilla, unless the read broke.
                    mode = TaaProneBridge.available() ? ProneMode.VANILLA_CRAWL : ProneMode.TAA_ASSUMED;
                }
            } else {
                mode = TaaProneBridge.installed() ? ProneMode.TAA_ASSUMED : ProneMode.VANILLA_CRAWL;
            }
        }

        boolean gun = ProneHitService.isGun(p.getMainHandItem());
        ProneSample draft = new ProneSample(gameTime, p.getX(), p.getY(), p.getZ(),
                velocity.x, velocity.y, velocity.z, p.getYRot(), p.getXRot(), bodyYaw,
                mode, taaStart, taaDuration, taaAnchor, taaTarget, bodyYaw, gun, crawlWeight);
        if (mode == ProneMode.NONE) {
            return draft;
        }
        return draft.withTaaHeading(ProneLayouts.heading(
                draft, prev, ProneHitService.settings(), ProneSegmentTables.shared()));
    }

    /** Lying on land: the SWIMMING pose outside water, not riding, gliding or spectating. */
    static boolean isLandProne(ServerPlayer p) {
        return p.getPose() == Pose.SWIMMING
                && !p.isPassenger()
                && !p.isInWaterOrBubble()
                && !p.isFallFlying()
                && p.isAlive()
                && !p.isSpectator();
    }

    private ProneSampler() {
    }
}
