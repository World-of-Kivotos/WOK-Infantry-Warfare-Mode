package com.wok.capturepoints.selection;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SelectionStore {
    private static final Map<UUID, Selection> SELECTIONS = new ConcurrentHashMap<>();

    private SelectionStore() {
    }

    public static Selection first(UUID playerId, ResourceLocation dimension, BlockPos position) {
        return SELECTIONS.compute(playerId, (ignored, existing) ->
                new Selection(dimension, position.immutable(), existing != null
                        && existing.dimension().equals(dimension) ? existing.second() : null));
    }

    public static Selection second(UUID playerId, ResourceLocation dimension, BlockPos position) {
        return SELECTIONS.compute(playerId, (ignored, existing) ->
                new Selection(dimension, existing != null && existing.dimension().equals(dimension)
                        ? existing.first() : null, position.immutable()));
    }

    public static Selection get(UUID playerId) {
        return SELECTIONS.get(playerId);
    }

    public static void clear(UUID playerId) {
        SELECTIONS.remove(playerId);
    }

    public record Selection(ResourceLocation dimension, BlockPos first, BlockPos second) {
        public boolean complete() { return first != null && second != null; }
    }
}
