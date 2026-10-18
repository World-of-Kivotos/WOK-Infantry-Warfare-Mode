package com.wok.infantry.stamina;

/**
 * Tracks authoritative server positions because network players are moved by position packets;
 * their {@code deltaMovement} is not a reliable measure of horizontal travel.
 */
final class StaminaMovementTracker {
    private static final double MIN_HORIZONTAL_DISTANCE_SQR = 0.0004D;
    private static final double MAX_HORIZONTAL_DISTANCE_SQR = 2.25D;

    private boolean initialized;
    private double lastX;
    private double lastZ;

    boolean sample(double x, double z) {
        if (!initialized) {
            initialized = true;
            lastX = x;
            lastZ = z;
            return false;
        }
        double deltaX = x - lastX;
        double deltaZ = z - lastZ;
        lastX = x;
        lastZ = z;
        double distanceSqr = deltaX * deltaX + deltaZ * deltaZ;
        // Ignore packet jitter and teleports; sprint drain is only for continuous locomotion.
        return distanceSqr >= MIN_HORIZONTAL_DISTANCE_SQR
                && distanceSqr <= MAX_HORIZONTAL_DISTANCE_SQR;
    }
}
