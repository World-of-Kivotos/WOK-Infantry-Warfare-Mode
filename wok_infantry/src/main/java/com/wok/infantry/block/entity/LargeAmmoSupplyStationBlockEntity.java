package com.wok.infantry.block.entity;

import com.wok.infantry.config.InfantryServerConfig;
import com.wok.infantry.registry.InfantryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Persistent 1500-point supply pool for WOK Infantry's own large station block. */
public final class LargeAmmoSupplyStationBlockEntity extends BlockEntity {
    private static final String REMAINING_POINTS_TAG = "RemainingPoints";
    private int remainingPoints = InfantryServerConfig.LARGE_AMMO_SUPPLY_POINTS;

    public LargeAmmoSupplyStationBlockEntity(BlockPos pos, BlockState state) {
        super(InfantryBlockEntities.LARGE_AMMO_SUPPLY_STATION.get(), pos, state);
    }

    public int remainingPoints() {
        return remainingPoints;
    }

    public int consumePoints(int requested) {
        int consumed = Math.min(Math.max(0, requested), remainingPoints);
        if (consumed > 0) {
            remainingPoints -= consumed;
            setChanged();
        }
        return consumed;
    }

    public void refill() {
        if (remainingPoints != InfantryServerConfig.LARGE_AMMO_SUPPLY_POINTS) {
            remainingPoints = InfantryServerConfig.LARGE_AMMO_SUPPLY_POINTS;
            setChanged();
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        remainingPoints = tag.contains(REMAINING_POINTS_TAG)
                ? Math.max(0, Math.min(InfantryServerConfig.LARGE_AMMO_SUPPLY_POINTS,
                tag.getInt(REMAINING_POINTS_TAG)))
                : InfantryServerConfig.LARGE_AMMO_SUPPLY_POINTS;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(REMAINING_POINTS_TAG, remainingPoints);
    }
}
