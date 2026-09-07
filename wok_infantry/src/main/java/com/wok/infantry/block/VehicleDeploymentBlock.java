package com.wok.infantry.block;

import com.wok.infantry.battle.ActionResult;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.deployment.DeploymentService;
import com.wok.infantry.deployment.VehicleDeploymentPoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;

/** Physical faction vehicle-batch origin; its arrow facing is authoritative spawn yaw. */
public final class VehicleDeploymentBlock extends Block {
    public static final EnumProperty<DeploymentBeaconMode> MODE =
            EnumProperty.create("mode", DeploymentBeaconMode.class);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D,
            16.0D, 5.0D, 16.0D);

    public VehicleDeploymentBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(MODE, DeploymentBeaconMode.UNBOUND)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                               CollisionContext context) {
        return SHAPE;
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
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        Faction requestedFaction = held.is(Items.BLUE_DYE) ? Faction.BLUE
                : held.is(Items.RED_DYE) ? Faction.RED : null;
        boolean emptyHand = held.isEmpty();
        if (requestedFaction == null && !emptyHand) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        Optional<DeploymentService> service = DeploymentService.get(serverLevel.getServer());
        if (service.isEmpty()) {
            serverPlayer.sendSystemMessage(Component.translatable(
                    "message.wok_infantry.vehicle_deployment.unavailable"));
            return InteractionResult.CONSUME;
        }

        if (requestedFaction != null) {
            send(serverPlayer, service.get().bindVehicleDeployment(serverPlayer,
                    requestedFaction, serverLevel, pos));
        } else if (serverPlayer.isShiftKeyDown()) {
            send(serverPlayer, service.get().rotateVehicleDeployment(serverPlayer,
                    serverLevel, pos));
        } else {
            Optional<VehicleDeploymentPoint> point =
                    service.get().vehicleDeploymentPointAt(serverLevel, pos);
            Component feedback = point.<Component>map(value -> Component.translatable(
                            "message.wok_infantry.vehicle_deployment.bound",
                            Component.translatable("faction.wok_infantry."
                                    + value.faction().id()), value.facing().getName()))
                    .orElseGet(() -> Component.translatable(
                            "message.wok_infantry.vehicle_deployment.unbound",
                            state.getValue(FACING).getName()));
            serverPlayer.sendSystemMessage(feedback);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                         boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock() && level instanceof ServerLevel serverLevel) {
            DeploymentService.get(serverLevel.getServer())
                    .ifPresent(service -> service.onVehicleDeploymentRemoved(serverLevel, pos));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static void send(ServerPlayer player, ActionResult result) {
        if (result != null && !result.message().isBlank()) {
            player.sendSystemMessage(Component.literal(result.message()));
        }
    }
}
