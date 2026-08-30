package com.wok.infantry.ammo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToIntFunction;

/** Pure calculation boundary: one reserve cap is applied once per unique ammunition type. */
public final class AmmoSupplyPlanner {
    private AmmoSupplyPlanner() {
    }

    public static <T> Map<T, Integer> deficits(Iterable<T> requiredTypes,
                                               ToIntFunction<T> currentCount,
                                               int reserveLimit) {
        Objects.requireNonNull(requiredTypes, "requiredTypes");
        Objects.requireNonNull(currentCount, "currentCount");
        if (reserveLimit < 1) {
            throw new IllegalArgumentException("Reserve limit must be positive");
        }
        LinkedHashMap<T, Integer> result = new LinkedHashMap<>();
        for (T type : requiredTypes) {
            Objects.requireNonNull(type, "Ammunition type cannot be null");
            if (result.containsKey(type)) {
                continue;
            }
            int existing = Math.max(0, currentCount.applyAsInt(type));
            result.put(type, Math.max(0, reserveLimit - existing));
        }
        return Map.copyOf(result);
    }
}
