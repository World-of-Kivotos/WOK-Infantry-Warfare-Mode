package com.wok.infantry.ammo;

/** Pure package/round/point calculation shared by the vehicle supply transaction and tests. */
public final class VehicleAmmoSupplyMath {
    private VehicleAmmoSupplyMath() {
    }

    public static Plan plan(int requestedRounds, int roundsPerPackage, int pointsPerRound,
                            int remainingPoints, int packageCapacity) {
        if (roundsPerPackage < 1 || pointsPerRound < 1 || requestedRounds < 1
                || remainingPoints < 0 || packageCapacity < 0) {
            return new Plan(0, 0, 0);
        }
        int requestedPackages = Math.max(1,
                Math.floorDiv(requestedRounds + roundsPerPackage - 1, roundsPerPackage));
        long pointsPerPackage = (long) roundsPerPackage * pointsPerRound;
        int affordablePackages = pointsPerPackage > Integer.MAX_VALUE ? 0
                : (int) Math.min(Integer.MAX_VALUE, remainingPoints / pointsPerPackage);
        int packages = Math.min(requestedPackages,
                Math.min(affordablePackages, packageCapacity));
        long rounds = (long) packages * roundsPerPackage;
        long points = rounds * pointsPerRound;
        return new Plan(packages, (int) Math.min(Integer.MAX_VALUE, rounds),
                (int) Math.min(Integer.MAX_VALUE, points));
    }

    public record Plan(int packages, int rounds, int points) {
    }
}
