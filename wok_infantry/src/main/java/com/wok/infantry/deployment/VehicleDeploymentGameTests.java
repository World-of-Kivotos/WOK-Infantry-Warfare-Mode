package com.wok.infantry.deployment;

import com.mojang.authlib.GameProfile;
import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.battle.ActionResult;
import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.block.DeploymentBeaconMode;
import com.wok.infantry.block.VehicleDeploymentBlock;
import com.wok.infantry.registry.InfantryBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Physical acceptance coverage for cardinal faction vehicle origins. */
@GameTestHolder(WokInfantryMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VehicleDeploymentGameTests {
    private VehicleDeploymentGameTests() {
    }

    @GameTest(templateNamespace = WokInfantryMod.MOD_ID,
            template = "wok_empty", timeoutTicks = 400)
    public static void arrowBindingRotationValidationAndRemoval(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        DeploymentService deployment = DeploymentService.get(server).orElseThrow();
        DeploymentSavedData data = DeploymentSavedData.get(server);
        VehicleDeploymentPoint previousBlue = data.vehiclePoint(Faction.BLUE).orElse(null);
        VehicleDeploymentPoint previousRed = data.vehiclePoint(Faction.RED).orElse(null);
        BlockPos anchor = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos duplicate = helper.absolutePos(new BlockPos(5, 1, 1));
        WorldPatch patch = new WorldPatch();
        ServerPlayer administrator = player(level, "vehicle-admin", true);

        try {
            remove(data, previousBlue);
            remove(data, previousRed);
            prepare(patch, level, anchor, Direction.EAST);
            prepare(patch, level, duplicate, Direction.WEST);

            useBlock(level, anchor, administrator, new ItemStack(Items.BLUE_DYE));
            VehicleDeploymentPoint bound = data.vehiclePoint(Faction.BLUE).orElseThrow();
            helper.assertTrue(bound.anchorPosition().equals(anchor)
                            && bound.dimension().equals(level.dimension().location())
                            && bound.facing() == Direction.EAST
                            && Float.compare(bound.yaw(), 270.0F) == 0,
                    "蓝色染料必须保存真实方块坐标及向东箭头 yaw");
            assertState(helper, level, anchor, DeploymentBeaconMode.BLUE, Direction.EAST,
                    "绑定成功后必须呈蓝方且保留箭头方向");
            helper.assertTrue(deployment.vehicleDeploymentPoint(Faction.BLUE)
                            .filter(bound::equals).isPresent(),
                    "物理状态与持久化记录一致时必须暴露载具生成原点");

            ActionResult duplicateResult = deployment.bindVehicleDeployment(
                    administrator, Faction.BLUE, level, duplicate);
            helper.assertTrue(!duplicateResult.success()
                            && data.vehiclePoint(Faction.BLUE).filter(bound::equals).isPresent(),
                    "同一阵营的第二个载具部署方块必须拒绝且不替换原点");
            assertState(helper, level, duplicate, DeploymentBeaconMode.UNBOUND,
                    Direction.WEST, "拒绝第二原点时候选方块必须保持未绑定");

            ActionResult rotated = deployment.rotateVehicleDeployment(
                    administrator, level, anchor);
            helper.assertTrue(rotated.success(), "管理员潜行旋转的服务端操作必须成功");
            VehicleDeploymentPoint south = data.vehiclePoint(Faction.BLUE).orElseThrow();
            helper.assertTrue(south.facing() == Direction.SOUTH
                            && Float.compare(south.yaw(), 0.0F) == 0,
                    "顺时针旋转必须把向东箭头及持久化 yaw 同步改为向南");
            assertState(helper, level, anchor, DeploymentBeaconMode.BLUE, Direction.SOUTH,
                    "旋转必须原子更新方块状态");

            patch.set(level, anchor, level.getBlockState(anchor)
                    .setValue(VehicleDeploymentBlock.FACING, Direction.NORTH));
            helper.assertTrue(deployment.vehicleDeploymentPoint(Faction.BLUE).isEmpty(),
                    "物理箭头与记录不一致时载具生成原点必须失败关闭");
            patch.set(level, anchor, level.getBlockState(anchor)
                    .setValue(VehicleDeploymentBlock.FACING, Direction.SOUTH));

            ServerPlayer deniedBreaker = player(level, "vehicle-break-no", false);
            BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(level, anchor,
                    level.getBlockState(anchor), deniedBreaker);
            MinecraftForge.EVENT_BUS.post(breakEvent);
            helper.assertTrue(breakEvent.isCanceled(),
                    "非管理员不得拆除载具部署方块");

            helper.assertTrue(level.destroyBlock(anchor, false, administrator),
                    "管理员必须能拆除载具部署方块");
            helper.assertTrue(data.vehiclePoint(Faction.BLUE).isEmpty()
                            && deployment.vehicleDeploymentPoint(Faction.BLUE).isEmpty(),
                    "拆除实体方块必须注销阵营载具生成原点");
        } finally {
            data.removeVehiclePoint(level.dimension().location(), anchor);
            data.removeVehiclePoint(level.dimension().location(), duplicate);
            patch.restore();
            restore(data, previousBlue);
            restore(data, previousRed);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = WokInfantryMod.MOD_ID,
            template = "wok_empty", timeoutTicks = 400)
    public static void consoleCommandPlacesReorientsAndRemovesArrow(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        DeploymentSavedData data = DeploymentSavedData.get(server);
        VehicleDeploymentPoint previousRed = data.vehiclePoint(Faction.RED).orElse(null);
        BlockPos anchor = helper.absolutePos(new BlockPos(1, 1, 1));
        WorldPatch patch = new WorldPatch();

        try {
            remove(data, previousRed);
            patch.set(level, anchor.below(), Blocks.STONE.defaultBlockState());
            patch.set(level, anchor, Blocks.AIR.defaultBlockState());
            String dimension = level.dimension().location().toString();
            String position = anchor.getX() + " " + anchor.getY() + " " + anchor.getZ();
            int placed = command(server, BattleRules.ADMIN_PERMISSION_LEVEL,
                    "battle deployment vehicle place red " + dimension + " "
                            + position + " 270");
            helper.assertTrue(placed == 1, "权限2控制台必须能放置并绑定载具箭头");
            assertState(helper, level, anchor, DeploymentBeaconMode.RED, Direction.EAST,
                    "yaw 270 必须生成向东的红方箭头");
            helper.assertTrue(data.vehiclePoint(Faction.RED)
                            .filter(point -> point.facing() == Direction.EAST).isPresent(),
                    "控制台显式 yaw 必须写入持久化记录");

            int reoriented = command(server, BattleRules.ADMIN_PERMISSION_LEVEL,
                    "battle deployment vehicle place red " + dimension + " "
                            + position + " 180");
            helper.assertTrue(reoriented == 1, "重复 place 必须能原子重定向已有箭头");
            assertState(helper, level, anchor, DeploymentBeaconMode.RED, Direction.NORTH,
                    "yaw 180 必须把已有箭头改为向北");
            helper.assertTrue(data.vehiclePoint(Faction.RED)
                            .filter(point -> point.facing() == Direction.NORTH).isPresent(),
                    "重定向已有箭头必须同步更新持久化方向");

            int removed = command(server, BattleRules.ADMIN_PERMISSION_LEVEL,
                    "battle deployment vehicle remove " + dimension + " " + position);
            helper.assertTrue(removed == 1 && level.getBlockState(anchor).isAir()
                            && data.vehiclePoint(Faction.RED).isEmpty(),
                    "控制台移除必须同时删除实体方块和阵营绑定");
        } finally {
            data.removeVehiclePoint(level.dimension().location(), anchor);
            patch.restore();
            restore(data, previousRed);
        }
        helper.succeed();
    }

    private static int command(MinecraftServer server, int permission, String command) {
        return server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack().withPermission(permission), command);
    }

    private static void prepare(WorldPatch patch, ServerLevel level, BlockPos position,
                                Direction facing) {
        patch.set(level, position.below(), Blocks.STONE.defaultBlockState());
        patch.set(level, position, InfantryBlocks.VEHICLE_DEPLOYMENT.get()
                .defaultBlockState().setValue(VehicleDeploymentBlock.FACING, facing));
    }

    private static void assertState(GameTestHelper helper, ServerLevel level,
                                    BlockPos position, DeploymentBeaconMode mode,
                                    Direction facing, String message) {
        BlockState state = level.getBlockState(position);
        helper.assertTrue(state.is(InfantryBlocks.VEHICLE_DEPLOYMENT.get())
                        && state.getValue(VehicleDeploymentBlock.MODE) == mode
                        && state.getValue(VehicleDeploymentBlock.FACING) == facing,
                message);
    }

    private static void useBlock(ServerLevel level, BlockPos position, ServerPlayer player,
                                 ItemStack held) {
        ItemStack previous = player.getItemInHand(InteractionHand.MAIN_HAND).copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        try {
            level.getBlockState(position).use(level, player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(position), Direction.UP,
                            position, false));
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, previous);
        }
    }

    private static ServerPlayer player(ServerLevel level, String name, boolean administrator) {
        MinecraftServer server = level.getServer();
        return new ServerPlayer(server, level,
                new GameProfile(UUID.randomUUID(), name.substring(0, Math.min(16,
                        name.length())))) {
            @Override
            public boolean hasPermissions(int permissionLevel) {
                return administrator && permissionLevel <= BattleRules.ADMIN_PERMISSION_LEVEL;
            }

            @Override
            public void sendSystemMessage(Component message) {
                // GameTest players intentionally have no network connection.
            }
        };
    }

    private static void remove(DeploymentSavedData data, VehicleDeploymentPoint point) {
        if (point != null) {
            data.removeVehiclePoint(point.dimension(), point.anchorPosition());
        }
    }

    private static void restore(DeploymentSavedData data, VehicleDeploymentPoint point) {
        if (point != null) {
            data.bindVehiclePoint(point);
        }
    }

    private static final class WorldPatch {
        private final Map<BlockPos, BlockState> originals = new LinkedHashMap<>();
        private ServerLevel level;

        void set(ServerLevel target, BlockPos position, BlockState state) {
            if (level == null) {
                level = target;
            }
            BlockPos immutable = position.immutable();
            originals.putIfAbsent(immutable, target.getBlockState(immutable));
            target.setBlock(immutable, state, 3);
        }

        void restore() {
            if (level == null) {
                return;
            }
            BlockPos[] positions = originals.keySet().toArray(BlockPos[]::new);
            for (int index = positions.length - 1; index >= 0; index--) {
                BlockPos position = positions[index];
                level.setBlock(position, originals.get(position), 3);
            }
            originals.clear();
        }
    }
}
