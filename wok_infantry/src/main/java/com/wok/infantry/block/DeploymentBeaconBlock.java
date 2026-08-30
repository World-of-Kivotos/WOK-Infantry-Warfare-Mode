package com.wok.infantry.block;

import com.wok.infantry.battle.ActionResult;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.deployment.DeploymentService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Optional;

/** Administrator-placed physical anchor for a faction deployment point. */
public final class DeploymentBeaconBlock extends Block {
    public static final EnumProperty<DeploymentBeaconMode> MODE =
            EnumProperty.create("mode", DeploymentBeaconMode.class);

    public DeploymentBeaconBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(MODE, DeploymentBeaconMode.UNBOUND));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        Faction requestedFaction = held.is(Items.BLUE_DYE) ? Faction.BLUE
                : held.is(Items.RED_DYE) ? Faction.RED : null;
        boolean query = held.isEmpty();
        if (requestedFaction == null && !query) {
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
                    "message.wok_infantry.deployment_beacon.unavailable"));
            return InteractionResult.CONSUME;
        }
        if (requestedFaction != null) {
            ActionResult result = service.get().bindBeacon(serverPlayer, requestedFaction,
                    serverLevel, pos, serverPlayer.getYRot());
            if (!result.message().isBlank()) {
                serverPlayer.sendSystemMessage(Component.literal(result.message()));
            }
        } else {
            Optional<Faction> faction = service.get().beaconFactionAt(serverLevel, pos);
            Component feedback = faction.<Component>map(value -> Component.translatable(
                            "message.wok_infantry.deployment_beacon.bound",
                            Component.translatable("faction.wok_infantry." + value.id())))
                    .orElseGet(() -> Component.translatable(
                            "message.wok_infantry.deployment_beacon.unbound"));
            serverPlayer.sendSystemMessage(feedback);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                         boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock() && level instanceof ServerLevel serverLevel) {
            DeploymentService.get(serverLevel.getServer())
                    .ifPresent(service -> service.onBeaconRemoved(serverLevel, pos));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
