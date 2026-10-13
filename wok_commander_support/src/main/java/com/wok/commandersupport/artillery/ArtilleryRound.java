package com.wok.commandersupport.artillery;

/**
 * One planned shell: where it lands horizontally and on which mission steps it whistles in and
 * lands. The height is read from the terrain when the step runs, so earlier craters count.
 */
record ArtilleryRound(int wave, int round, double x, double z,
                      int whistleStep, int impactStep) {
    ArtilleryRound {
        if (wave < 0 || round < 0 || !Double.isFinite(x) || !Double.isFinite(z)
                || whistleStep < 0
                || impactStep != whistleStep + ArtilleryProfile.WHISTLE_LEAD_STEPS) {
            throw new IllegalArgumentException("Invalid artillery round");
        }
    }

    /** Horizontal distance from a point, used to check the scatter envelope. */
    double distanceTo(double centerX, double centerZ) {
        return Math.hypot(x - centerX, z - centerZ);
    }
}
