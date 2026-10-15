package com.wok.infantry.client.map;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the deployment-point pins go on the tactical map (preview {@code surfaces/30-map.js},
 * {@code pointPlan}). A pin's tip sits on its deployment point. When that spot is taken — by a
 * marker icon (a squad rally pack under the rally flag), the start icon of an attack order, its
 * arrow head, or a pin placed before it — the whole pin steps aside and a leader runs from its tip
 * back to the true spot.
 *
 * <p>Candidates, around everything the pin's own spot hits: left, right, the same two raised so the
 * tip is level with the top of it (upper left, upper right), straight above it, then the four side
 * spots once more one pin width further out. Each candidate is scored by the area it covers of
 * every obstacle times the obstacle's weight (marker icons and pins 1, attack-order shafts
 * {@link #SHAFT_WEIGHT}); the least-covering candidate whose pin stays inside the viewport wins and
 * ties keep the earlier one. Covering up to {@link #GRAZE_ART_PX2} art pixels² (a drop shadow, or
 * the box corners a round head leaves empty) counts as clear, so a pin that only grazes stays put.
 * When even the chosen spot covers a marker icon the pin is flagged {@link Plan#over()} and drawn
 * above the icons, so its head is never hidden.
 *
 * <p>Pins are planned in order; every placed pin, its leader and its spawn badge become obstacles
 * for the later ones. A further list of boxes to avoid (labels the old map draws under its
 * symbols, the compass and the scale bar) only scores the candidates of a pin that has to move
 * anyway; it never moves a pin off its true spot by itself. All coordinates are physical pixels;
 * the class is pure logic.
 */
public final class TacticalMapPinPlanner {
    /** Covering this many art pixels² or fewer counts as clear. */
    public static final int GRAZE_ART_PX2 = 6;
    /** Weight of an attack-order shaft or a pin leader: a pin may cross a line rather than an icon. */
    public static final double SHAFT_WEIGHT = 0.25D;
    /** Art pixels between a pin and the obstacle it steps beside. */
    static final int SIDE_GAP_ART = 1;
    /** One pin width (15) plus its shadow and a gap: how much further out the second ring sits. */
    static final int FAR_STEP_ART = 17;

    private TacticalMapPinPlanner() {
    }

    /**
     * Axis-aligned box in physical pixels with a placement weight; {@code icon} marks the boxes of
     * marker icons (a pin covering one is drawn above the icons).
     */
    public record Box(double left, double top, double right, double bottom, double weight,
                      boolean icon) {
        /** Box of half size {@code half} centred on (x, y). */
        public static Box around(double x, double y, double half, double weight, boolean icon) {
            return new Box(x - half, y - half, x + half, y + half, weight, icon);
        }

        /** Overlapping area with {@code other}, 0 when they do not overlap. */
        public double overlap(Box other) {
            double width = Math.min(right, other.right) - Math.max(left, other.left);
            double height = Math.min(bottom, other.bottom) - Math.max(top, other.top);
            return width <= 0.0D || height <= 0.0D ? 0.0D : width * height;
        }

        boolean contains(Box inner) {
            return inner.left >= left && inner.top >= top && inner.right <= right
                    && inner.bottom <= bottom;
        }
    }

    /**
     * A deployment point to place: its pin icon, the true spot (the pin tip) and whether it is the
     * spawn point the player picked (it carries the check badge).
     */
    public record Pin(TacticalMapIcons.MapIcon icon, double x, double y, boolean spawn) {
    }

    /** The spawn check badge: centre and half size (the green square, without its casing). */
    public record Badge(double x, double y, int half) {
        Box box() {
            return Box.around(x, y, half + 1.0D, 1.0D, false);
        }
    }

    /**
     * Where a pin is drawn: its tip at (tipX, tipY); {@code moved} pins draw a leader to the true
     * spot; {@code over} pins are drawn above the marker icons; {@code badge} is null unless the pin
     * is the spawn point.
     */
    public record Plan(Pin pin, double tipX, double tipY, boolean moved, boolean over,
                       Badge badge) {
    }

    /**
     * Box of a marker icon anchored at (x, y) — the plate centre, or a pin's tip — grown by
     * {@code padArt} art pixels: 1 for the drop shadow, 3 for a hover / selection ring.
     */
    public static Box iconBox(TacticalMapIcons.MapIcon icon, double x, double y, int artPx,
                              int padArt, double weight, boolean isIcon) {
        TacticalMapIcons.Plate plate = icon.plate();
        int px = Math.max(1, artPx);
        double anchorX = plate.anchorHalfX() / 2.0D;
        double anchorY = plate.anchorHalfY() / 2.0D;
        return new Box(x - (anchorX + padArt) * px, y - (anchorY + padArt) * px,
                x + (plate.width() - anchorX + padArt) * px,
                y + (plate.height() - anchorY + padArt) * px, weight, isIcon);
    }

    /**
     * Small boxes of half size {@code half} every {@code step} pixels along a segment, keeping only
     * the ones whose centre lies inside {@code clip}.
     */
    public static List<Box> lineBoxes(double x0, double y0, double x1, double y1, double step,
                                      double half, double weight, Box clip) {
        List<Box> boxes = new ArrayList<>();
        double length = Math.hypot(x1 - x0, y1 - y0);
        int segments = Math.max(1, (int) Math.ceil(length / Math.max(1.0D, step)));
        for (int index = 0; index <= segments; index++) {
            double x = x0 + (x1 - x0) * index / segments;
            double y = y0 + (y1 - y0) * index / segments;
            if (clip == null || x >= clip.left && x < clip.right && y >= clip.top
                    && y < clip.bottom) {
                boxes.add(Box.around(x, y, half, weight, false));
            }
        }
        return boxes;
    }

    /**
     * The two boxes a pin with its tip at (tipX, tipY) covers, grown by {@code padArt} art pixels:
     * the round head (art rows 0–14, 15 wide) and the narrow tip (rows 15–17, 5 wide). The whole
     * art box would count the empty corners beside the tip as covered.
     */
    static List<Box> footprint(double tipX, double tipY, int artPx, int padArt) {
        int px = Math.max(1, artPx);
        double pad = padArt * (double) px;
        Box head = new Box(tipX - 7.5D * px - pad, tipY - 18.0D * px - pad,
                tipX + 7.5D * px + pad, tipY - 3.0D * px + pad, 1.0D, false);
        Box tip = new Box(tipX - 2.5D * px - pad, head.bottom(), tipX + 2.5D * px + pad,
                tipY + pad, 1.0D, false);
        return List.of(head, tip);
    }

    /** Weighted area of {@code footprint} covered by {@code obstacles}. */
    static double cost(List<Box> footprint, List<Box> obstacles) {
        double total = 0.0D;
        for (Box part : footprint) {
            for (Box obstacle : obstacles) {
                total += part.overlap(obstacle) * obstacle.weight();
            }
        }
        return total;
    }

    /** Unweighted area of {@code footprint} covered by marker icons only. */
    static double iconCost(List<Box> footprint, List<Box> obstacles) {
        double total = 0.0D;
        for (Box part : footprint) {
            for (Box obstacle : obstacles) {
                if (obstacle.icon()) {
                    total += part.overlap(obstacle);
                }
            }
        }
        return total;
    }

    /** {@link #plan(List, List, List, int, Box, double, double, int)} with nothing else to avoid. */
    public static List<Plan> plan(List<Pin> pins, List<Box> obstacles, int artPx, Box viewport,
                                  double lineStep, double lineHalf, int badgeHalf) {
        return plan(pins, obstacles, List.of(), artPx, viewport, lineStep, lineHalf, badgeHalf);
    }

    /**
     * Places the pins in order.
     *
     * @param pins        pins whose true spot lies inside the viewport, in drawing order
     * @param obstacles   marker icons, attack-order shafts and heads (see the class comment)
     * @param avoid       what a pin that has to move should rather not land on, without moving a
     *                    pin off its true spot by itself: labels the map has already drawn below
     *                    the pins (support cards and tags, area notes) and the compass and scale
     *                    bar drawn above them
     * @param artPx       physical pixels per art pixel of the pins
     * @param viewport    the map viewport; a moved pin (with its shadow) must stay inside it
     * @param lineStep    spacing of the obstacle boxes laid along a leader
     * @param lineHalf    half size of those boxes
     * @param badgeHalf   half size of the spawn check badge (without its 1 px casing)
     */
    public static List<Plan> plan(List<Pin> pins, List<Box> obstacles, List<Box> avoid,
                                  int artPx, Box viewport, double lineStep, double lineHalf,
                                  int badgeHalf) {
        int px = Math.max(1, artPx);
        double graze = GRAZE_ART_PX2 * (double) px * px;
        double halfWidth = 7.5D * px;
        List<Box> taken = new ArrayList<>(obstacles);
        List<Plan> plans = new ArrayList<>(pins.size());
        for (Pin pin : pins) {
            double x = pin.x();
            double y = pin.y();
            double tipX = x;
            double tipY = y;
            List<Box> home = footprint(x, y, px, 0);
            if (cost(home, taken) > graze) {
                double[] blocker = blocker(home, taken);
                double left = blocker[0] - px * SIDE_GAP_ART - halfWidth;
                double right = blocker[2] + px * SIDE_GAP_ART + halfWidth;
                double top = blocker[1];
                double far = FAR_STEP_ART * (double) px;
                double[][] candidates = {
                        {left, y}, {right, y}, {left, top}, {right, top}, {x, top - px},
                        {left - far, y}, {right + far, y}, {left - far, top}, {right + far, top}
                };
                double best = Double.POSITIVE_INFINITY;
                for (int index = 0; index < candidates.length; index++) {
                    double candidateX = Math.round(candidates[index][0]);
                    double candidateY = Math.round(candidates[index][1]);
                    Box drawn = iconBox(pin.icon(), candidateX, candidateY, px, 1, 1.0D, false);
                    if (viewport != null && !viewport.contains(drawn)) {
                        continue;
                    }
                    List<Box> foot = footprint(candidateX, candidateY, px, 0);
                    double score = Math.max(0.0D, cost(foot, taken) - graze)
                            + cost(foot, avoid) + index * 1.0E-6D;
                    if (score < best) {
                        best = score;
                        tipX = candidateX;
                        tipY = candidateY;
                    }
                }
            }
            boolean moved = tipX != x || tipY != y;
            List<Box> placed = footprint(tipX, tipY, px, 0);
            boolean over = moved && iconCost(placed, taken) > graze;
            Badge badge = pin.spawn() ? badge(x, tipX, tipY, px, badgeHalf, viewport) : null;
            // Later pins steer clear of this one, its leader and its badge.
            taken.addAll(footprint(tipX, tipY, px, 1));
            if (moved) {
                taken.addAll(lineBoxes(x, y, tipX, tipY, lineStep, lineHalf, SHAFT_WEIGHT,
                        viewport));
            }
            if (badge != null) {
                taken.add(badge.box());
            }
            plans.add(new Plan(pin, tipX, tipY, moved, over, badge));
        }
        return plans;
    }

    /**
     * Bounds {left, top, right, bottom} of what the pin's home spot hits: the obstacles of weight 1
     * or more when there are any (icons, heads, pins), otherwise every hit (shafts, leaders).
     */
    private static double[] blocker(List<Box> home, List<Box> taken) {
        List<Box> hits = new ArrayList<>();
        List<Box> big = new ArrayList<>();
        for (Box obstacle : taken) {
            boolean hit = false;
            for (Box part : home) {
                hit |= part.overlap(obstacle) > 0.0D;
            }
            if (hit) {
                hits.add(obstacle);
                if (obstacle.weight() >= 1.0D) {
                    big.add(obstacle);
                }
            }
        }
        List<Box> union = big.isEmpty() ? hits : big;
        double[] bounds = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (Box box : union) {
            bounds[0] = Math.min(bounds[0], box.left());
            bounds[1] = Math.min(bounds[1], box.top());
            bounds[2] = Math.max(bounds[2], box.right());
            bounds[3] = Math.max(bounds[3], box.bottom());
        }
        return bounds;
    }

    /**
     * The spawn check sits on the pin head's upper corner, which the round head leaves empty: upper
     * right, or upper left when the pin stepped left or the badge would leave the viewport, so it
     * never covers the silhouette.
     */
    static Badge badge(double trueX, double tipX, double tipY, int artPx, int half, Box viewport) {
        int px = Math.max(1, artPx);
        double offset = 5.5D * px + half + 1.0D;
        boolean left = tipX < trueX
                || viewport != null && tipX + offset + half + 2.0D > viewport.right() - 1.0D;
        double x = Math.round(tipX + (left ? -offset : offset));
        double y = Math.round(tipY - 18.0D * px + half + 1.0D - px);
        return new Badge(x, y, half);
    }
}
