package com.wok.infantry.block.entity;

import com.wok.infantry.config.InfantryServerConfig;
import com.wok.infantry.registry.InfantryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Persistent 500-point supply pool for the physical medium ammunition crate. */
public final class MediumAmmoSupplyCrateBlockEntity extends BlockEntity {
    private static final String REMAINING_POINTS_TAG = "RemainingPoints";
    private int remainingPoints = InfantryServerConfig.MEDIUM_AMMO_SUPPLY_POINTS;

    public MediumAmmoSupplyCrateBlockEntity(BlockPos pos, BlockState state) {
        super(InfantryBlockEntities.MEDIUM_AMMO_SUPPLY_CRATE.get(), pos, state);
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
        if (remainingPoints != InfantryServerConfig.MEDIUM_AMMO_SUPPLY_POINTS) {
            remainingPoints = InfantryServerConfig.MEDIUM_AMMO_SUPPLY_POINTS;
            setChanged();
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        remainingPoints = tag.contains(REMAINING_POINTS_TAG)
                ? Math.max(0, Math.min(InfantryServerConfig.MEDIUM_AMMO_SUPPLY_POINTS,
                tag.getInt(REMAINING_POINTS_TAG)))
                : InfantryServerConfig.MEDIUM_AMMO_SUPPLY_POINTS;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(REMAINING_POINTS_TAG, remainingPoints);
    }
}
