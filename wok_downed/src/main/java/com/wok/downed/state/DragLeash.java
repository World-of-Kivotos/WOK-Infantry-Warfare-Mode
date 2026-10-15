package com.wok.downed.state;

/** Horizontal rope between carrier and casualty: slack inside its length, pulled taut beyond it. */
public final class DragLeash {
    /**
     * Returns the casualty's new horizontal position {x, z}, or {@code null} while the rope is slack.
     * The casualty stays on the carrier-to-casualty line, so turning around never swings it behind
     * the carrier's back and the carrier can face (and aim at) the casualty again.
     */
    public static double[] follow(double carrierX, double carrierZ,
                                  double casualtyX, double casualtyZ, double length) {
        double safeLength = Math.max(0.0D, length);
        double dx = casualtyX - carrierX;
        double dz = casualtyZ - carrierZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance <= safeLength) {
            return null;
        }
        double scale = safeLength / distance;
        return new double[]{carrierX + dx * scale, carrierZ + dz * scale};
    }

    /** Yaw in degrees that makes an entity at (fromX, fromZ) face (toX, toZ). */
    public static float yawToward(double fromX, double fromZ, double toX, double toZ) {
        return (float) (Math.toDegrees(Math.atan2(toZ - fromZ, toX - fromX)) - 90.0D);
    }

    /** True when the carrier's 3D separation from the casualty means the rope can no longer hold. */
    public static boolean snapped(double distanceSquared, double length, double rescueDistance) {
        double limit = Math.max(Math.max(0.0D, length), Math.max(0.0D, rescueDistance)) + 1.0D;
        return distanceSquared > limit * limit;
    }

    private DragLeash() {
    }
}
