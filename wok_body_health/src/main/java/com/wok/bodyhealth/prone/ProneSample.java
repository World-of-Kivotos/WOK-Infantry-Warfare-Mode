package com.wok.bodyhealth.prone;

import net.minecraft.world.phys.Vec3;

/**
 * Server-side snapshot of everything the prone hit layout depends on, taken at
 * PlayerTickEvent END (history) or at hit time (live sample).
 *
 * @param vx          TaCZ-style velocity: the 2-tick displacement for history entries,
 *                    {@code pos - (xOld, yOld, zOld)} for the live sample
 * @param bodyYaw     simulated on-screen body yaw ({@link BodyYawSim}), not the server's yBodyRot
 * @param taaHeading  low-passed heading while TAA_PRONE or TAA_EXIT, otherwise the mode's own yaw
 *                    (see {@link ProneLayouts#heading})
 * @param taaAim      smoothed head-to-body yaw TAA draws TAA_ENTER and TAA_PRONE with, otherwise the
 *                    raw one against taaHeading (see {@link ProneLayouts#smoothedAim})
 * @param crawlWeight 0..1, rises while the player keeps moving
 */
public record ProneSample(long gameTime, double x, double y, double z,
                          double vx, double vy, double vz,
                          float yRot, float xRot,
                          float bodyYaw,
                          ProneMode mode, long taaStart, int taaDuration, float taaAnchor, float taaTarget,
                          float taaHeading, float taaAim,
                          boolean gun, float crawlWeight) {

    public Vec3 velocity() {
        return new Vec3(vx, vy, vz);
    }

    public ProneSample withTaaHeading(float heading) {
        return new ProneSample(gameTime, x, y, z, vx, vy, vz, yRot, xRot, bodyYaw,
                mode, taaStart, taaDuration, taaAnchor, taaTarget, heading, taaAim, gun, crawlWeight);
    }

    public ProneSample withTaaAim(float aim) {
        return new ProneSample(gameTime, x, y, z, vx, vy, vz, yRot, xRot, bodyYaw,
                mode, taaStart, taaDuration, taaAnchor, taaTarget, taaHeading, aim, gun, crawlWeight);
    }
}
