package com.wok.infantry.formation.vehicle;

import com.wok.infantry.battle.ActionResult;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Fail-closed optional-mod and registry gate shared by production code and pure tests. */
public final class SuperbWarfareVehicleGate {
    public static final String MOD_ID = "superbwarfare";
    public static final String FCP_MOD_ID = "fcp";
    private static final Set<String> SUPPORTED_ENTITY_NAMESPACES =
            Set.of(MOD_ID, FCP_MOD_ID);

    private SuperbWarfareVehicleGate() {
    }

    public static <T> Resolution<T> resolve(boolean modLoaded, List<ResourceLocation> entityIds,
                                             Function<ResourceLocation, T> resolver) {
        if (entityIds == null || resolver == null) {
            return Resolution.failure("卓越前线载具解析参数缺失");
        }
        if (entityIds.isEmpty()) {
            return Resolution.success(Map.of());
        }
        if (!modLoaded) {
            return Resolution.failure("编制需要卓越前线 MOD（modId=superbwarfare），但服务端未加载");
        }

        for (ResourceLocation entityId : entityIds) {
            if (!supportsEntity(entityId)) {
                return Resolution.failure("载具实体必须属于已支持的卓越前线生态 namespace "
                        + SUPPORTED_ENTITY_NAMESPACES + ": "
                        + String.valueOf(entityId));
            }
        }

        Map<ResourceLocation, T> resolved = new LinkedHashMap<>();
        for (ResourceLocation entityId : entityIds) {
            if (resolved.containsKey(entityId)) {
                continue;
            }
            T value;
            try {
                value = resolver.apply(entityId);
            } catch (RuntimeException exception) {
                return Resolution.failure("解析卓越前线载具实体失败: " + entityId
                        + " (" + exception.getClass().getSimpleName() + ")");
            }
            if (value == null) {
                return Resolution.failure("卓越前线载具实体未注册: " + entityId);
            }
            resolved.put(entityId, value);
        }
        return Resolution.success(resolved);
    }

    public static boolean supportsEntity(ResourceLocation entityId) {
        return entityId != null
                && SUPPORTED_ENTITY_NAMESPACES.contains(entityId.getNamespace());
    }

    public record Resolution<T>(ActionResult result, Map<ResourceLocation, T> values) {
        public Resolution {
            values = values == null ? Map.of() : Map.copyOf(values);
        }

        static <T> Resolution<T> success(Map<ResourceLocation, T> values) {
            return new Resolution<>(ActionResult.ok("卓越前线载具配置校验通过"), values);
        }

        static <T> Resolution<T> failure(String message) {
            return new Resolution<>(ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    message), Map.of());
        }
    }
}
