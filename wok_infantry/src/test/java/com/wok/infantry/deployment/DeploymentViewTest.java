package com.wok.infantry.deployment;

import com.wok.infantry.battle.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeploymentViewTest {
    private static final ResourceLocation OVERWORLD =
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");

    @Test
    void countdownsClampAtZeroAndPointListIsDefensivelyCopied() {
        List<DeploymentPoint> source = new ArrayList<>();
        source.add(point());
        DeploymentView view = new DeploymentView(DeploymentPhase.READY,
                4L, 100L, 90L, 160L, source.get(0).id(),
                true, true, true, false, source);
        source.clear();

        assertEquals(0L, view.waitingTicks());
        assertEquals(60L, view.resupplyTicks());
        assertEquals(1, view.points().size());
        assertThrows(UnsupportedOperationException.class, () -> view.points().clear());
    }

    @Test
    void nullPointListBecomesEmptyAndNegativeTicksAreRejected() {
        DeploymentView view = new DeploymentView(DeploymentPhase.WAITING,
                0L, 0L, 0L, 0L, null,
                false, false, false, false, null);

        assertEquals(List.of(), view.points());
        assertThrows(IllegalArgumentException.class, () -> new DeploymentView(
                DeploymentPhase.WAITING, 0L, -1L, 0L, 0L, null,
                false, false, false, false, List.of()));
    }

    private static DeploymentPoint point() {
        return new DeploymentPoint(UUID.randomUUID(), Faction.BLUE, OVERWORLD,
                new BlockPos(10, 64, 20), 90.0F, DeploymentPoint.DEFAULT_SUPPLY_RADIUS);
    }
}
