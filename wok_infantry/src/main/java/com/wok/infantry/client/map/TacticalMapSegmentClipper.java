package com.wok.infantry.client.map;

/** Pure Liang-Barsky clipping for tactical-map screen-space line segments. */
public final class TacticalMapSegmentClipper {
    private TacticalMapSegmentClipper() {
    }

    /**
     * Clips a directed segment to an inclusive rectangle while preserving endpoint order.
     *
     * @return the visible directed segment, or {@code null} when it does not intersect the
     * rectangle or any input is non-finite
     */
    public static ClippedSegment clip(double startX, double startY,
                                      double endX, double endY,
                                      double minimumX, double minimumY,
                                      double maximumX, double maximumY) {
        if (!Double.isFinite(startX) || !Double.isFinite(startY)
                || !Double.isFinite(endX) || !Double.isFinite(endY)
                || !Double.isFinite(minimumX) || !Double.isFinite(minimumY)
                || !Double.isFinite(maximumX) || !Double.isFinite(maximumY)
                || minimumX > maximumX || minimumY > maximumY) {
            return null;
        }

        double deltaX = endX - startX;
        double deltaY = endY - startY;
        if (deltaX == 0.0D && deltaY == 0.0D) {
            return inside(startX, startY, minimumX, minimumY, maximumX, maximumY)
                    ? new ClippedSegment(startX, startY, endX, endY) : null;
        }

        double entering = 0.0D;
        double leaving = 1.0D;
        if (deltaX == 0.0D) {
            if (startX < minimumX || startX > maximumX) {
                return null;
            }
        } else {
            double first = (minimumX - startX) / deltaX;
            double second = (maximumX - startX) / deltaX;
            if (first > second) {
                double swap = first;
                first = second;
                second = swap;
            }
            entering = Math.max(entering, first);
            leaving = Math.min(leaving, second);
            if (entering > leaving) {
                return null;
            }
        }

        if (deltaY == 0.0D) {
            if (startY < minimumY || startY > maximumY) {
                return null;
            }
        } else {
            double first = (minimumY - startY) / deltaY;
            double second = (maximumY - startY) / deltaY;
            if (first > second) {
                double swap = first;
                first = second;
                second = swap;
            }
            entering = Math.max(entering, first);
            leaving = Math.min(leaving, second);
            if (entering > leaving) {
                return null;
            }
        }

        return new ClippedSegment(
                startX + deltaX * entering,
                startY + deltaY * entering,
                startX + deltaX * leaving,
                startY + deltaY * leaving);
    }

    private static boolean inside(double x, double y,
                                  double minimumX, double minimumY,
                                  double maximumX, double maximumY) {
        return x >= minimumX && x <= maximumX && y >= minimumY && y <= maximumY;
    }

    public record ClippedSegment(double startX, double startY, double endX, double endY) {
    }
}
