package com.wok.commandersupport.airstrike;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Pure flight geometry for the visible three-second diagonal Paveway approach. */
record PavewayGuidancePlan(Vec3 spawn, int flightTicks) {
    static final double CBC_LINEAR_DRAG = 0.01D;
    static final double SPAWN_OFFSET_X = -96.0D;
    static final double SPAWN_OFFSET_Y = 120.0D;
    static final double SPAWN_OFFSET_Z = -96.0D;
    static final double GUIDANCE_APPROACH_HEIGHT = 4.0D;

    PavewayGuidancePlan {
        if (spawn == null || !finite(spawn) || flightTicks <= 0) {
            throw new IllegalArgumentException("Invalid Paveway flight plan");
        }
    }

    static PavewayGuidancePlan fromDesignation(Vec3 designation, int flightTicks) {
        if (designation == null || !finite(designation) || flightTicks <= 0) {
            throw new IllegalArgumentException("Invalid Paveway designation");
        }
        return new PavewayGuidancePlan(designation.add(SPAWN_OFFSET_X,
                SPAWN_OFFSET_Y, SPAWN_OFFSET_Z), flightTicks);
    }

    /**
     * The four diagonal release points around a designation, the one horizontally nearest to
     * {@code preferNear} first (the designator: chunks around a player are the likeliest to be
     * loaded). Ties, and a missing or non-finite {@code preferNear}, keep the original
     * north-west approach first, then north-east, south-west and south-east.
     */
    static List<PavewayGuidancePlan> releaseCandidates(Vec3 designation, Vec3 preferNear,
                                                       int flightTicks) {
        if (designation == null || !finite(designation) || flightTicks <= 0) {
            throw new IllegalArgumentException("Invalid Paveway designation");
        }
        List<PavewayGuidancePlan> candidates = new ArrayList<>(4);
        for (double signZ : new double[]{1.0D, -1.0D}) {
            for (double signX : new double[]{1.0D, -1.0D}) {
                candidates.add(new PavewayGuidancePlan(designation.add(
                        SPAWN_OFFSET_X * signX, SPAWN_OFFSET_Y, SPAWN_OFFSET_Z * signZ),
                        flightTicks));
            }
        }
        if (preferNear != null && finite(preferNear)) {
            // List.sort is stable, so equal distances keep the fixed order above.
            candidates.sort(Comparator.comparingDouble(plan -> {
                double dx = plan.spawn().x - preferNear.x;
                double dz = plan.spawn().z - preferNear.z;
                return dx * dx + dz * dz;
            }));
        }
        return List.copyOf(candidates);
    }

    static Vec3 guidancePoint(Vec3 designation) {
        if (designation == null || !finite(designation)) {
            throw new IllegalArgumentException("Invalid Paveway designation");
        }
        return designation.add(0.0D, GUIDANCE_APPROACH_HEIGHT, 0.0D);
    }

    static Vec3 velocityTo(Vec3 current, Vec3 destination, int remainingTicks) {
        if (current == null || destination == null || !finite(current)
                || !finite(destination) || remainingTicks <= 0) {
            throw new IllegalArgumentException("Invalid Paveway guidance correction");
        }
        double travelFactor = geometricTravelFactor(CBC_LINEAR_DRAG,
                remainingTicks);
        return destination.subtract(current).scale(1.0D / travelFactor);
    }

    static double geometricTravelFactor(double drag, int ticks) {
        if (!Double.isFinite(drag) || drag < 0.0D || drag >= 1.0D
                || ticks <= 0) {
            throw new IllegalArgumentException("Invalid drag flight parameters");
        }
        return drag == 0.0D ? ticks
                : (1.0D - Math.pow(1.0D - drag, ticks)) / drag;
    }

    private static boolean finite(Vec3 vector) {
        return Double.isFinite(vector.x) && Double.isFinite(vector.y)
                && Double.isFinite(vector.z);
    }
}
