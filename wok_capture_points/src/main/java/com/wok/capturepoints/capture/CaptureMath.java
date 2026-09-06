package com.wok.capturepoints.capture;

public final class CaptureMath {
    private CaptureMath() {
    }

    public static Result step(double control, int bluePlayers, int redPlayers,
                              boolean blueAllowed, boolean redAllowed,
                              int elapsedTicks, int captureSeconds,
                              int maxSpeedPlayers, boolean advantageMode) {
        int blue = blueAllowed ? Math.max(0, bluePlayers) : 0;
        int red = redAllowed ? Math.max(0, redPlayers) : 0;
        if (blue == red || (!advantageMode && blue > 0 && red > 0)) {
            return new Result(clamp(control), CaptureTeam.NEUTRAL, 0);
        }
        CaptureTeam active = blue > red ? CaptureTeam.BLUE : CaptureTeam.RED;
        int rawSpeed = advantageMode ? Math.abs(blue - red) : Math.max(blue, red);
        int speed = Math.max(1, Math.min(Math.max(1, maxSpeedPlayers), rawSpeed));
        double fullEnemyToOwnedDistance = 2.0D;
        double delta = fullEnemyToOwnedDistance * speed * Math.max(0, elapsedTicks)
                / (Math.max(1, captureSeconds) * 20.0D);
        return new Result(clamp(control + delta * active.direction()), active, speed);
    }

    public static CaptureTeam owner(double control) {
        if (control >= 0.999999D) return CaptureTeam.BLUE;
        if (control <= -0.999999D) return CaptureTeam.RED;
        return CaptureTeam.NEUTRAL;
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) return 0.0D;
        return Math.max(-1.0D, Math.min(1.0D, value));
    }

    public record Result(double control, CaptureTeam activeTeam, int speedMultiplier) {
    }
}
