package com.wok.infantry.block.entity;

import com.wok.infantry.config.InfantryServerConfig;
import com.wok.infantry.registry.InfantryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Persistent 100-point supply pool for the physical small ammunition crate. */
public final class AmmoSupplyCrateBlockEntity extends BlockEntity {
    private static final String REMAINING_POINTS_TAG = "RemainingPoints";
    private int remainingPoints = InfantryServerConfig.SMALL_AMMO_SUPPLY_POINTS;

    public AmmoSupplyCrateBlockEntity(BlockPos pos, BlockState state) {
        super(InfantryBlockEntities.AMMO_SUPPLY_CRATE.get(), pos, state);
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
        if (remainingPoints != InfantryServerConfig.SMALL_AMMO_SUPPLY_POINTS) {
            remainingPoints = InfantryServerConfig.SMALL_AMMO_SUPPLY_POINTS;
            setChanged();
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        remainingPoints = tag.contains(REMAINING_POINTS_TAG)
                ? Math.max(0, Math.min(InfantryServerConfig.SMALL_AMMO_SUPPLY_POINTS,
                tag.getInt(REMAINING_POINTS_TAG)))
                : InfantryServerConfig.SMALL_AMMO_SUPPLY_POINTS;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(REMAINING_POINTS_TAG, remainingPoints);
    }
}
