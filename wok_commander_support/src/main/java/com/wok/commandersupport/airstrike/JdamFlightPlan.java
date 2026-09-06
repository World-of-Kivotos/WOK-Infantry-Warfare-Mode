package com.wok.commandersupport.airstrike;

import net.minecraft.world.phys.Vec3;

/** Deterministic vertical high-altitude drop compensated for CBC HE-shell drag. */
public record JdamFlightPlan(Vec3 spawn, Vec3 velocity, Vec3 impact,
                             int flightTicks) {
    public static final double HORIZONTAL_OFFSET = 0.0D;
    public static final double VERTICAL_OFFSET = 200.0D;
    public static final double CBC_LINEAR_DRAG = 0.01D;

    public static JdamFlightPlan fromImpact(double impactX, double impactY,
                                            double impactZ, int flightTicks) {
        if (!Double.isFinite(impactX) || !Double.isFinite(impactY)
                || !Double.isFinite(impactZ) || flightTicks < 1) {
            throw new IllegalArgumentException("Invalid JDAM impact geometry");
        }
        Vec3 impact = new Vec3(impactX, impactY, impactZ);
        Vec3 spawn = impact.add(-HORIZONTAL_OFFSET, VERTICAL_OFFSET,
                -HORIZONTAL_OFFSET);
        double travelFactor = geometricTravelFactor(CBC_LINEAR_DRAG, flightTicks);
        Vec3 velocity = impact.subtract(spawn).scale(1.0D / travelFactor);
        return new JdamFlightPlan(spawn, velocity, impact, flightTicks);
    }

    static double geometricTravelFactor(double drag, int ticks) {
        if (!Double.isFinite(drag) || drag < 0.0D || drag >= 1.0D || ticks < 1) {
            throw new IllegalArgumentException("Invalid projectile drag model");
        }
        if (drag == 0.0D) {
            return ticks;
        }
        double retained = 1.0D - drag;
        return (1.0D - Math.pow(retained, ticks)) / drag;
    }
}
