package com.wok.infantry.formation.vehicle;

import com.wok.infantry.battle.ActionResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Pure configuration and coordinate planning; it never touches Forge registries or a world. */
public final class VehicleBatchPlanner {
    public static final int MAX_VEHICLES_PER_BATCH = 64;
    public static final double MAX_ABSOLUTE_OFFSET = 256.0D;
    private static final int MAX_ALLOCATION_ID_LENGTH = 96;
    private static final Pattern ALLOCATION_ID = Pattern.compile("[a-z0-9._/-]+");

    private VehicleBatchPlanner() {
    }

    public static PlanResult plan(ResourceLocation dimension, Vec3 basePosition, float baseYaw,
                                  List<? extends VehicleDefinitionView> definitions) {
        if (dimension == null) {
            return PlanResult.failure(invalidTarget("载具部署目标维度缺失"));
        }
        if (basePosition == null || !finite(basePosition.x)
                || !finite(basePosition.y) || !finite(basePosition.z)
                || !Float.isFinite(baseYaw)) {
            return PlanResult.failure(invalidTarget("载具主基地坐标或朝向不合法"));
        }
        if (definitions == null) {
            return PlanResult.failure(invalidTarget("载具编制列表缺失"));
        }
        if (definitions.size() > MAX_VEHICLES_PER_BATCH) {
            return PlanResult.failure(invalidTarget("单批载具数量超过上限 "
                    + MAX_VEHICLES_PER_BATCH));
        }

        double radians = Math.toRadians(baseYaw);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        Set<String> allocations = new HashSet<>();
        List<VehicleSpawnPlan> plans = new ArrayList<>(definitions.size());

        for (int index = 0; index < definitions.size(); index++) {
            VehicleDefinitionView definition = definitions.get(index);
            if (definition == null) {
                return PlanResult.failure(invalidTarget("载具编制第 " + index + " 项为空"));
            }
            String allocation = normalizedAllocation(definition.allocationId());
            if (allocation == null) {
                return PlanResult.failure(invalidTarget("载具编制第 " + index
                        + " 项 allocationId 不合法"));
            }
            if (!allocations.add(allocation)) {
                return PlanResult.failure(invalidTarget("载具 allocationId 重复: "
                        + allocation));
            }

            ResourceLocation entityId = definition.entityId() == null ? null
                    : ResourceLocation.tryParse(definition.entityId());
            if (entityId == null) {
                return PlanResult.failure(invalidTarget("载具实体 ID 不合法: "
                        + String.valueOf(definition.entityId())));
            }
            if (!finiteOffset(definition.offsetX()) || !finiteOffset(definition.offsetY())
                    || !finiteOffset(definition.offsetZ())
                    || !Float.isFinite(definition.yawOffset())) {
                return PlanResult.failure(invalidTarget("载具 " + allocation
                        + " 的相对坐标或朝向不合法"));
            }

            // Local +X/+Z align with world +X/+Z at yaw 0. Positive Minecraft yaw rotates
            // local +Z towards world -X, which is this standard X/Z rotation matrix.
            double rotatedX = definition.offsetX() * cosine - definition.offsetZ() * sine;
            double rotatedZ = definition.offsetX() * sine + definition.offsetZ() * cosine;
            Vec3 target = basePosition.add(rotatedX, definition.offsetY(), rotatedZ);
            if (!finite(target.x) || !finite(target.y) || !finite(target.z)) {
                return PlanResult.failure(invalidTarget("载具 " + allocation
                        + " 的目标坐标溢出"));
            }
            plans.add(new VehicleSpawnPlan(allocation, entityId, dimension, target,
                    wrapDegrees(baseYaw + definition.yawOffset())));
        }
        return PlanResult.success(plans);
    }

    private static String normalizedAllocation(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.length() > MAX_ALLOCATION_ID_LENGTH
                || !ALLOCATION_ID.matcher(normalized).matches()) {
            return null;
        }
        return normalized;
    }

    private static boolean finiteOffset(double value) {
        return finite(value) && Math.abs(value) <= MAX_ABSOLUTE_OFFSET;
    }

    private static boolean finite(double value) {
        return Double.isFinite(value);
    }

    static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0F;
        if (wrapped >= 180.0F) {
            wrapped -= 360.0F;
        }
        if (wrapped < -180.0F) {
            wrapped += 360.0F;
        }
        return wrapped;
    }

    private static ActionResult invalidTarget(String message) {
        return ActionResult.failure(ActionResult.Code.INVALID_TARGET, message);
    }

    public record PlanResult(ActionResult result, List<VehicleSpawnPlan> plans) {
        public PlanResult {
            plans = plans == null ? List.of() : List.copyOf(plans);
        }

        static PlanResult success(List<VehicleSpawnPlan> plans) {
            return new PlanResult(ActionResult.ok("载具批次规划完成"), plans);
        }

        static PlanResult failure(ActionResult result) {
            return new PlanResult(result, List.of());
        }
    }
}
