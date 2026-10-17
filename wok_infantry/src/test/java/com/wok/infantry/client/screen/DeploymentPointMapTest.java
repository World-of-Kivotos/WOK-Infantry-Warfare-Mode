package com.wok.infantry.client.screen;

import com.wok.infantry.deployment.DeploymentPointKind;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeploymentPointMapTest {
    private static final ToIntFunction<String> WIDTH = text -> text.codePoints()
            .map(point -> point < 0x80 ? 6 : 9).sum();
    private static final ResourceLocation OVERWORLD =
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
    private static final ResourceLocation NETHER =
            ResourceLocation.fromNamespaceAndPath("minecraft", "the_nether");

    private static DeploymentPointMap.Point point(int index, DeploymentPointKind kind, int x,
                                                  int z, boolean selected, String label) {
        return new DeploymentPointMap.Point(new UUID(7L, index), kind, OVERWORLD, x, z, 8,
                selected, label);
    }

    private static List<DeploymentPointMap.Point> preview() {
        return List.of(point(0, DeploymentPointKind.MAIN_BASE, -160, 110, true, "主基地 · 已选"),
                point(1, DeploymentPointKind.FIELD_BEACON, -40, 20, false, "部署信标 1"),
                point(2, DeploymentPointKind.RALLY, -30, 60, false, "小队队包"));
    }

    @Test
    void pointsLabelsAndScaleBarStayInsideTheWellAndNeverOverlap() {
        for (UiRect well : List.of(new UiRect(10, 10, 270, 130), new UiRect(0, 0, 150, 70),
                new UiRect(5, 5, 455, 205))) {
            DeploymentPointMap.Plan plan = DeploymentPointMap.plan(well, preview(), WIDTH, "50 格");
            assertEquals(3, plan.points().size());
            List<UiRect> labels = new ArrayList<>();
            for (DeploymentPointMap.Placed placed : plan.points()) {
                assertTrue(well.contains(placed.x(), placed.y()),
                        placed + " lies outside " + well);
                if (!placed.label().isEmpty()) {
                    assertTrue(well.contains(placed.label()), placed.label() + " leaves " + well);
                    for (UiRect other : labels) {
                        assertFalse(other.intersects(placed.label()),
                                "labels overlap: " + other + " / " + placed.label());
                    }
                    labels.add(placed.label());
                    assertEquals(WIDTH.applyAsInt(placed.point().label()),
                            placed.label().width(), "a label is drawn at its full width");
                }
            }
            if (!plan.scaleBar().isEmpty()) {
                assertTrue(well.contains(plan.scaleBar()));
                for (UiRect label : labels) {
                    assertFalse(label.intersects(plan.scaleBar()));
                }
            }
            assertTrue(plan.step() * plan.scale() >= 26 || plan.step() == 1000,
                    "the grid step is at least 26px wide");
        }
    }

    @Test
    void theSelectedPointsLabelWinsAndTheMainBaseGetsItsRing() {
        DeploymentPointMap.Plan plan = DeploymentPointMap.plan(new UiRect(0, 0, 300, 160),
                preview(), WIDTH, "50 格");
        DeploymentPointMap.Placed main = plan.points().get(0);
        assertFalse(main.label().isEmpty(), "the selected point is always labelled when it fits");
        assertTrue(main.ring() >= 10, "the main base shows its supply radius");
        assertEquals(0, plan.points().get(1).ring());
        assertEquals(main.point().id(), DeploymentPointMap.hit(plan, main.x() + 3, main.y() - 3));
        assertNull(DeploymentPointMap.hit(plan, -50, -50));
    }

    @Test
    void onlyTheSelectedPointsDimensionIsPlotted() {
        List<DeploymentPointMap.Point> points = new ArrayList<>(preview());
        points.add(new DeploymentPointMap.Point(new UUID(7L, 9L), DeploymentPointKind.FIELD_BEACON,
                NETHER, 4000, -4000, 8, false, "部署信标 2"));
        DeploymentPointMap.Plan plan = DeploymentPointMap.plan(new UiRect(0, 0, 300, 160),
                points, WIDTH, "50 格");
        assertEquals(3, plan.points().size());
        assertTrue(plan.points().stream().allMatch(placed ->
                placed.point().dimension().equals(OVERWORLD)));
        assertTrue(DeploymentPointMap.plan(new UiRect(0, 0, 300, 160), List.of(), WIDTH, "")
                .points().isEmpty());
    }

    @Test
    void denseBeaconsDropLabelsInsteadOfPrintingOverEachOther() {
        List<DeploymentPointMap.Point> points = new ArrayList<>();
        points.add(point(0, DeploymentPointKind.MAIN_BASE, 0, 0, true, "主基地 · 已选"));
        for (int index = 1; index < 16; index++) {
            points.add(point(index, DeploymentPointKind.FIELD_BEACON, index * 3, index * 2, false,
                    "部署信标 " + index));
        }
        DeploymentPointMap.Plan plan = DeploymentPointMap.plan(new UiRect(0, 0, 220, 90),
                points, WIDTH, "10 格");
        List<UiRect> labels = plan.points().stream().map(DeploymentPointMap.Placed::label)
                .filter(label -> !label.isEmpty()).toList();
        for (int first = 0; first < labels.size(); first++) {
            for (int second = first + 1; second < labels.size(); second++) {
                assertFalse(labels.get(first).intersects(labels.get(second)));
            }
        }
        assertTrue(labels.size() < points.size(), "some labels must give way");
    }
}
