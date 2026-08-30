package com.wok.infantry.registry;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.block.DeploymentBeaconBlock;
import com.wok.infantry.block.AmmoSupplyCrateBlock;
import com.wok.infantry.block.VehicleDeploymentBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class InfantryBlocks {
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, WokInfantryMod.MOD_ID);

    public static final RegistryObject<Block> DEPLOYMENT_BEACON = BLOCKS.register(
             "deployment_beacon",
             () -> new DeploymentBeaconBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                     .strength(4.0F, 3_600_000.0F)
                     .pushReaction(PushReaction.BLOCK)));

    public static final RegistryObject<Block> VEHICLE_DEPLOYMENT = BLOCKS.register(
            "vehicle_deployment",
            () -> new VehicleDeploymentBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(4.0F, 3_600_000.0F)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));

    public static final RegistryObject<Block> AMMO_SUPPLY_CRATE = BLOCKS.register(
            "ammo_supply_crate",
            () -> new AmmoSupplyCrateBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(4.0F, 3_600_000.0F)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));

    private InfantryBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
