package com.wok.bodyhealth.prone;

import net.minecraft.util.Mth;

/**
 * Re-runs vanilla's body-yaw rule ({@code LivingEntity.tick} 182-334 and {@code tickHeadTurn}
 * 0-69) on the server. The server never sees a player's own movement in that rule because the
 * packet handler resets xo/zo first, so its yBodyRot can be up to 50 degrees off what observers
 * see; feeding the real per-tick displacement here reproduces the observers' body yaw.
 */
public final class BodyYawSim {
    /** One tick of body yaw; float arithmetic matches vanilla. */
    public static float step(float body, float yRot, double dx, double dz, boolean swinging) {
        float target = body;
        float distanceSqr = (float) (dx * dx + dz * dz);
        if (distanceSqr > 0.0025000002F) {
            float heading = (float) Mth.atan2(dz, dx) * (180.0F / (float) Math.PI) - 90.0F;
            float offset = Mth.abs(Mth.wrapDegrees(yRot) - heading);
            // Walking backwards keeps the body facing forwards.
            target = 95.0F < offset && offset < 265.0F ? heading - 180.0F : heading;
        }
        if (swinging) {
            target = yRot;
        }

        float next = body + Mth.wrapDegrees(target - body) * 0.3F;
        float slack = Mth.wrapDegrees(yRot - next);
        if (Math.abs(slack) > 50.0F) {
            next += slack - (float) (Mth.sign(slack) * 50);
        }
        return next;
    }

    private BodyYawSim() {
    }
}
