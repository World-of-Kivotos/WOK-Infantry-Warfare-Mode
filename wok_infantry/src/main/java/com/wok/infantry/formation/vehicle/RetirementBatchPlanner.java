package com.wok.infantry.formation.vehicle;

import com.wok.infantry.battle.ActionResult;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** Pure two-phase retirement preflight shared by session switching and formation reloads. */
final class RetirementBatchPlanner {
    private RetirementBatchPlanner() {
    }

    static <K, V> Plan<K, V> preflight(List<K> targets,
                                       Function<K, Inspection<V>> inspector) {
        if (targets == null || inspector == null) {
            return Plan.failure(ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                    "载具退役预检参数缺失"));
        }
        Map<K, V> loaded = new LinkedHashMap<>();
        java.util.ArrayList<K> unloaded = new java.util.ArrayList<>();
        for (K target : targets) {
            Inspection<V> inspection;
            try {
                inspection = inspector.apply(target);
            } catch (RuntimeException | LinkageError exception) {
                return Plan.failure(ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                        "载具退役预检异常: "
                                + exception.getClass().getSimpleName()));
            }
            if (inspection == null || inspection.result() == null) {
                return Plan.failure(ActionResult.failure(ActionResult.Code.INVALID_TARGET,
                        "载具退役预检未返回有效结果"));
            }
            if (!inspection.result().success()) {
                // Discard every earlier provisional candidate. Callers cannot enter commit with
                // only a prefix of a batch that failed predictable validation.
                return Plan.failure(inspection.result());
            }
            if (inspection.loadedValue() == null) {
                unloaded.add(target);
            } else {
                loaded.put(target, inspection.loadedValue());
            }
        }
        return new Plan<>(ActionResult.ok("载具退役整批预检通过"), loaded,
                unloaded);
    }

    record Inspection<V>(ActionResult result, V loadedValue) {
        Inspection {
            Objects.requireNonNull(result, "result");
        }

        static <V> Inspection<V> loaded(V value) {
            return new Inspection<>(ActionResult.ok("已加载载具预检通过"),
                    Objects.requireNonNull(value, "value"));
        }

        static <V> Inspection<V> unloaded() {
            return new Inspection<>(ActionResult.ok("未加载载具规划 tombstone"), null);
        }

        static <V> Inspection<V> failure(ActionResult result) {
            if (result == null || result.success()) {
                throw new IllegalArgumentException("Failure inspection requires failed result");
            }
            return new Inspection<>(result, null);
        }
    }

    record Plan<K, V>(ActionResult result, Map<K, V> loaded, List<K> unloaded) {
        Plan {
            Objects.requireNonNull(result, "result");
            loaded = Collections.unmodifiableMap(new LinkedHashMap<>(loaded));
            unloaded = List.copyOf(unloaded);
        }

        static <K, V> Plan<K, V> failure(ActionResult result) {
            return new Plan<>(result, Map.of(), List.of());
        }
    }
}
