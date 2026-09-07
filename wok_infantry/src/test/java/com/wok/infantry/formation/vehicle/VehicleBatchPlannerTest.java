package com.wok.infantry.formation.vehicle;

import com.wok.infantry.formation.FormationVehicleDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleBatchPlannerTest {
    private static final ResourceLocation OVERWORLD =
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");

    @Test
    void rotatesEveryRelativeOffsetAroundBaseYawAndAddsYawOffset() {
        VehicleAllocationSpec tank = new VehicleAllocationSpec("tank_one",
                "superbwarfare:t_90a", 2.0D, 1.0D, 10.0D, 45.0F);

        VehicleBatchPlanner.PlanResult result = VehicleBatchPlanner.plan(OVERWORLD,
                new Vec3(100.0D, 64.0D, 200.0D), 90.0F, List.of(tank));

        assertTrue(result.result().success());
        assertEquals(1, result.plans().size());
        VehicleSpawnPlan plan = result.plans().get(0);
        assertEquals(90.0D, plan.position().x, 1.0E-9D);
        assertEquals(65.0D, plan.position().y, 1.0E-9D);
        assertEquals(202.0D, plan.position().z, 1.0E-9D);
        assertEquals(135.0F, plan.yaw(), 1.0E-6F);
    }

    @Test
    void plansWholeBatchInDefinitionOrderWithoutLoadingVehicleMod() {
        List<VehicleAllocationSpec> vehicles = List.of(
                new VehicleAllocationSpec("mbt", "superbwarfare:m_1a_2",
                        0.0D, 0.0D, 8.0D, 0.0F),
                new VehicleAllocationSpec("ifv", "superbwarfare:bradley",
                        -6.0D, 0.0D, 4.0D, -30.0F));

        VehicleBatchPlanner.PlanResult result = VehicleBatchPlanner.plan(OVERWORLD,
                Vec3.ZERO, 180.0F, vehicles);

        assertTrue(result.result().success());
        assertEquals(List.of("mbt", "ifv"), result.plans().stream()
                .map(VehicleSpawnPlan::allocationId).toList());
        assertEquals(0.0D, result.plans().get(0).position().x, 1.0E-9D);
        assertEquals(-8.0D, result.plans().get(0).position().z, 1.0E-9D);
        assertEquals(-180.0F, result.plans().get(0).yaw(), 1.0E-6F);
        assertEquals(150.0F, result.plans().get(1).yaw(), 1.0E-6F);
    }

    @Test
    void duplicateAllocationRejectsTheEntirePlan() {
        VehicleBatchPlanner.PlanResult result = VehicleBatchPlanner.plan(OVERWORLD,
                Vec3.ZERO, 0.0F, List.of(
                        new VehicleAllocationSpec("tank", "superbwarfare:t_90a",
                                0.0D, 0.0D, 0.0D, 0.0F),
                        new VehicleAllocationSpec("TANK", "superbwarfare:m_1a_2",
                                20.0D, 0.0D, 0.0D, 0.0F)));

        assertFalse(result.result().success());
        assertTrue(result.result().message().contains("重复"));
        assertTrue(result.plans().isEmpty());
    }

    @Test
    void formationDefinitionAdapterPreservesServerOwnedCoordinates() {
        FormationVehicleDefinition definition = new FormationVehicleDefinition(
                "academy_mbt", "主战坦克", "superbwarfare:m_1a_2",
                12.0D, 0.5D, -4.0D, 270.0F);

        VehicleDeploymentRequest request = VehicleDeploymentRequest.fromFormationDefinitions(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "wok_infantry:academy", "wok_infantry:armored",
                OVERWORLD, new Vec3(1.0D, 64.0D, 1.0D), 0.0F,
                List.of(definition));

        assertEquals(List.of(new VehicleAllocationSpec("academy_mbt",
                "superbwarfare:m_1a_2", 12.0D, 0.5D, -4.0D, 270.0F)),
                request.vehicles());
    }
}
