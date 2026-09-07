package com.wok.infantry.block;

import com.wok.infantry.ammo.AmmoSupplyService;
import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.block.entity.LargeAmmoSupplyStationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Independent 1500-point large supply station owned entirely by WOK Infantry. */
public final class LargeAmmoSupplyStationBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape NORTH_SOUTH = Block.box(-22, 0, -9, 38, 41, 25);
    private static final VoxelShape EAST_WEST = Block.box(-9, 0, -22, 25, 41, 38);

    public LargeAmmoSupplyStationBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING,
                context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                               CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.X
                ? EAST_WEST : NORTH_SOUTH;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LargeAmmoSupplyStationBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(level.getBlockEntity(pos)
                instanceof LargeAmmoSupplyStationBlockEntity station)) {
            return InteractionResult.CONSUME;
        }
        if (serverPlayer.isShiftKeyDown()
                && serverPlayer.hasPermissions(BattleRules.ADMIN_PERMISSION_LEVEL)) {
            station.refill();
            serverPlayer.sendSystemMessage(Component.translatable(
                    "message.wok_infantry.ammo_supply.refilled", station.remainingPoints()));
            return InteractionResult.CONSUME;
        }
        AmmoSupplyService.openLargeBlock(serverPlayer, pos, station);
        return InteractionResult.CONSUME;
    }
}
