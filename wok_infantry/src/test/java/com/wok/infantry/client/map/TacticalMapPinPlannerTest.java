package com.wok.infantry.client.map;

import com.wok.infantry.client.map.TacticalMapIcons.MapIcon;
import com.wok.infantry.client.map.TacticalMapPinPlanner.Box;
import com.wok.infantry.client.map.TacticalMapPinPlanner.Pin;
import com.wok.infantry.client.map.TacticalMapPinPlanner.Plan;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Deployment pins stepping aside on the tactical map (preview pointPlan). */
class TacticalMapPinPlannerTest {
    private static final int PX = 2;
    private static final Box VIEWPORT = new Box(0, 0, 600, 400, 0, false);
    private static final double LINE_STEP = 12.0D;
    private static final double LINE_HALF = 3.0D;
    private static final int BADGE_HALF = 7;

    private static List<Plan> plan(List<Pin> pins, List<Box> obstacles) {
        return TacticalMapPinPlanner.plan(pins, obstacles, PX, VIEWPORT, LINE_STEP, LINE_HALF,
                BADGE_HALF);
    }

    private static Box icon(MapIcon icon, double x, double y) {
        return TacticalMapPinPlanner.iconBox(icon, x, y, PX, 1, 1.0D, true);
    }

    @Test
    void aPinOnAClearSpotStaysOnItsPoint() {
        Plan plan = plan(List.of(new Pin(MapIcon.MAIN_BASE, 300, 200, false)), List.of()).get(0);
        assertEquals(300.0D, plan.tipX());
        assertEquals(200.0D, plan.tipY());
        assertFalse(plan.moved());
        assertFalse(plan.over());
        assertNull(plan.badge());
    }

    @Test
    void aRallyPackUnderTheRallyFlagStepsLeftAndDrawsALeader() {
        Box rally = icon(MapIcon.RALLY, 300, 200);
        Plan plan = plan(List.of(new Pin(MapIcon.RALLY_PACK, 300, 200, false)),
                List.of(rally)).get(0);
        assertTrue(plan.moved(), "the pack's head would cover the rally flag");
        assertFalse(plan.over(), "the left side is clear");
        // First candidate: one art pixel left of the flag, pin half width further, same tip row.
        assertEquals(Math.round(rally.left() - PX - 7.5D * PX), plan.tipX());
        assertEquals(200.0D, plan.tipY());
        double covered = TacticalMapPinPlanner.cost(
                TacticalMapPinPlanner.footprint(plan.tipX(), plan.tipY(), PX, 0), List.of(rally));
        assertEquals(0.0D, covered, "the moved pin no longer covers the flag");
    }

    @Test
    void aPinThatOnlyGrazesAnIconStaysPut() {
        // The pin head spans x 285–315, y 164–194: this box covers its top-right 2 × 2 art px
        // (4 × 4 = 16 physical px², within the 6 art px² = 24 px² allowance).
        Box grazed = new Box(311, 160, 330, 168, 1.0D, true);
        double area = TacticalMapPinPlanner.cost(
                TacticalMapPinPlanner.footprint(300, 200, PX, 0), List.of(grazed));
        assertEquals(16.0D, area);
        Plan plan = plan(List.of(new Pin(MapIcon.FIELD_BEACON, 300, 200, false)),
                List.of(grazed)).get(0);
        assertFalse(plan.moved());
        assertFalse(plan.over());

        // Two art px wider (8 × 4 = 32 px²) is no longer a graze.
        Box covering = new Box(307, 160, 330, 168, 1.0D, true);
        assertTrue(plan(List.of(new Pin(MapIcon.FIELD_BEACON, 300, 200, false)),
                List.of(covering)).get(0).moved());
    }

    @Test
    void candidatesThatLeaveTheViewportAreSkipped() {
        // At the left edge the left candidates would put the pin off the map: it goes right.
        Box rally = icon(MapIcon.RALLY, 20, 200);
        Plan plan = plan(List.of(new Pin(MapIcon.RALLY_PACK, 20, 200, false)),
                List.of(rally)).get(0);
        assertTrue(plan.moved());
        assertEquals(Math.round(rally.right() + PX + 7.5D * PX), plan.tipX());
        Box drawn = TacticalMapPinPlanner.iconBox(MapIcon.RALLY_PACK, plan.tipX(), plan.tipY(),
                PX, 1, 1.0D, false);
        assertTrue(VIEWPORT.contains(drawn));
    }

    @Test
    void labelsOnlySteerAPinThatHasToMove() {
        // A support card right where the pack would go first (left of the flag).
        Box rally = icon(MapIcon.RALLY, 300, 200);
        Box card = new Box(200, 150, 282, 210, 1.0D, false);
        Plan moved = TacticalMapPinPlanner.plan(
                List.of(new Pin(MapIcon.RALLY_PACK, 300, 200, false)), List.of(rally),
                List.of(card), PX, VIEWPORT, LINE_STEP, LINE_HALF, BADGE_HALF).get(0);
        assertTrue(moved.moved());
        assertEquals(Math.round(rally.right() + PX + 7.5D * PX), moved.tipX(),
                "the next candidate, right of the flag, is clear of the card");
        assertEquals(0.0D, TacticalMapPinPlanner.cost(
                TacticalMapPinPlanner.footprint(moved.tipX(), moved.tipY(), PX, 0),
                List.of(card)));

        // A card under a pin's true spot does not move the pin.
        Plan home = TacticalMapPinPlanner.plan(
                List.of(new Pin(MapIcon.MAIN_BASE, 240, 200, false)), List.of(),
                List.of(card), PX, VIEWPORT, LINE_STEP, LINE_HALF, BADGE_HALF).get(0);
        assertFalse(home.moved());
    }

    @Test
    void aPinWithNoClearSpotIsDrawnAboveTheIcons() {
        // The rally flag decides where the candidates go; a field of light icons (weight 0.5,
        // so it does not widen that blocker) covers every candidate.
        Box rally = icon(MapIcon.RALLY, 300, 200);
        Box everywhere = new Box(-1000, -1000, 2000, 2000, 0.5D, true);
        Plan plan = plan(List.of(new Pin(MapIcon.MAIN_BASE, 300, 200, false)),
                List.of(rally, everywhere)).get(0);
        assertTrue(plan.moved(), "the spot off the flag covers less; ties keep the first");
        assertEquals(Math.round(rally.left() - PX - 7.5D * PX), plan.tipX());
        assertTrue(plan.over(), "its head must not be hidden under the icons");
    }

    @Test
    void aPinThatCannotMoveStaysBelowTheIcons() {
        // A viewport too small for any candidate: the pin keeps its spot and its layer.
        Box tiny = new Box(280, 150, 320, 210, 0, false);
        Box rally = icon(MapIcon.RALLY, 300, 200);
        Plan plan = TacticalMapPinPlanner.plan(List.of(new Pin(MapIcon.RALLY_PACK, 300, 200,
                false)), List.of(rally), PX, tiny, LINE_STEP, LINE_HALF, BADGE_HALF).get(0);
        assertFalse(plan.moved());
        assertFalse(plan.over());
    }

    @Test
    void laterPinsAvoidEarlierPinsAndTheirLeaders() {
        List<Plan> plans = plan(List.of(
                new Pin(MapIcon.MAIN_BASE, 300, 200, false),
                new Pin(MapIcon.FIELD_BEACON, 300, 200, false),
                new Pin(MapIcon.RALLY_PACK, 300, 200, false)), List.of());
        assertFalse(plans.get(0).moved(), "the first pin keeps its spot");
        assertTrue(plans.get(1).moved());
        assertTrue(plans.get(2).moved());
        List<Box> first = TacticalMapPinPlanner.footprint(plans.get(0).tipX(),
                plans.get(0).tipY(), PX, 0);
        for (int index = 1; index < plans.size(); index++) {
            List<Box> later = TacticalMapPinPlanner.footprint(plans.get(index).tipX(),
                    plans.get(index).tipY(), PX, 0);
            assertEquals(0.0D, TacticalMapPinPlanner.cost(later, first), "pin " + index);
            assertFalse(plans.get(index).over(), "pins are not marker icons");
        }
        List<Box> second = TacticalMapPinPlanner.footprint(plans.get(1).tipX(),
                plans.get(1).tipY(), PX, 0);
        List<Box> third = TacticalMapPinPlanner.footprint(plans.get(2).tipX(),
                plans.get(2).tipY(), PX, 0);
        assertEquals(0.0D, TacticalMapPinPlanner.cost(third, second));
    }

    @Test
    void attackShaftsWeighAQuarterOfAnIcon() {
        List<Box> shaft = TacticalMapPinPlanner.lineBoxes(100, 200, 500, 200, 12, 4,
                TacticalMapPinPlanner.SHAFT_WEIGHT, VIEWPORT);
        assertEquals(35, shaft.size(), "400 px in 12 px steps: 34 segments, 35 boxes");
        double weighted = TacticalMapPinPlanner.cost(
                TacticalMapPinPlanner.footprint(300, 210, PX, 0), shaft);
        double unweighted = TacticalMapPinPlanner.iconCost(
                TacticalMapPinPlanner.footprint(300, 210, PX, 0), shaft);
        assertTrue(weighted > 0.0D);
        assertEquals(0.0D, unweighted, "a shaft is not an icon");
        List<Box> clipped = TacticalMapPinPlanner.lineBoxes(-100, 200, 100, 200, 10, 3, 1.0D,
                VIEWPORT);
        assertTrue(clipped.stream().allMatch(box -> box.left() + 3 >= 0),
                "boxes off the viewport are dropped");
    }

    @Test
    void theSpawnBadgeSitsOnTheHeadsUpperCornerAwayFromTheMove() {
        Plan clear = plan(List.of(new Pin(MapIcon.MAIN_BASE, 300, 200, true)), List.of()).get(0);
        assertNotNull(clear.badge());
        double offset = 5.5D * PX + BADGE_HALF + 1;
        assertEquals(Math.round(300 + offset), clear.badge().x(), "upper right by default");
        assertEquals(Math.round(200 - 18.0D * PX + BADGE_HALF + 1 - PX), clear.badge().y());
        assertEquals(BADGE_HALF, clear.badge().half());

        Box rally = icon(MapIcon.RALLY, 300, 200);
        Plan moved = plan(List.of(new Pin(MapIcon.RALLY_PACK, 300, 200, true)),
                List.of(rally)).get(0);
        assertTrue(moved.tipX() < 300);
        assertEquals(Math.round(moved.tipX() - offset), moved.badge().x(),
                "a pin that stepped left carries its badge on the left");

        Plan edge = TacticalMapPinPlanner.plan(List.of(new Pin(MapIcon.MAIN_BASE, 590, 200,
                true)), List.of(), PX, VIEWPORT, LINE_STEP, LINE_HALF, BADGE_HALF).get(0);
        assertEquals(Math.round(590 - offset), edge.badge().x(),
                "at the right edge the badge goes left");
    }

    @Test
    void aBadgeAndALeaderAlsoKeepLaterPinsAway() {
        List<Box> obstacles = new ArrayList<>();
        obstacles.add(icon(MapIcon.RALLY, 300, 200));
        List<Plan> plans = plan(List.of(
                new Pin(MapIcon.RALLY_PACK, 300, 200, true),
                new Pin(MapIcon.FIELD_BEACON, Math.round(300 - 17 - 7.5D * PX - PX), 200,
                        false)), obstacles);
        Plan pack = plans.get(0);
        Plan beacon = plans.get(1);
        assertTrue(pack.moved());
        Box badge = Box.around(pack.badge().x(), pack.badge().y(), BADGE_HALF + 1, 1, false);
        List<Box> beaconFoot = TacticalMapPinPlanner.footprint(beacon.tipX(), beacon.tipY(),
                PX, 0);
        assertEquals(0.0D, TacticalMapPinPlanner.cost(beaconFoot, List.of(badge)),
                "the beacon does not cover the pack's spawn badge");
        assertEquals(0.0D, TacticalMapPinPlanner.cost(beaconFoot,
                TacticalMapPinPlanner.footprint(pack.tipX(), pack.tipY(), PX, 0)));
    }
}
