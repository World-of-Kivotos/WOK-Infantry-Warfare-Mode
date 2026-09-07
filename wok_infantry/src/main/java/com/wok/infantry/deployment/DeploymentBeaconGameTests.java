package com.wok.infantry.deployment;

import com.mojang.authlib.GameProfile;
import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.battle.ActionResult;
import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.block.DeploymentBeaconBlock;
import com.wok.infantry.block.DeploymentBeaconMode;
import com.wok.infantry.registry.InfantryBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Physical-block acceptance coverage for faction field deployment beacons. */
@GameTestHolder(WokInfantryMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DeploymentBeaconGameTests {
    private DeploymentBeaconGameTests() {
    }

    @GameTest(templateNamespace = WokInfantryMod.MOD_ID,
            template = "wok_empty", timeoutTicks = 400)
    public static void physicalBindingIsIdempotentFactionFilteredAndRemovedWithBlock(
            GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        DeploymentService deployment = DeploymentService.get(server).orElseThrow();
        DeploymentSavedData savedData = DeploymentSavedData.get(server);
        BattleService battle = BattleService.get(server).orElseThrow();
        WorldPatch patch = new WorldPatch();
        BlockPos anchor = helper.absolutePos(new BlockPos(1, 1, 1));
        FieldDeploymentPoint previous = savedData.fieldPointAt(
                level.dimension().location(), anchor).orElse(null);
        ServerPlayer administrator = player(level, "beacon-admin", true);
        ServerPlayer blueViewer = player(level, "beacon-blue-view", false);
        ServerPlayer redViewer = player(level, "beacon-red-view", false);

        try {
            if (previous != null) {
                deployment.onBeaconRemoved(level, anchor);
            }
            prepareBeacon(patch, level, anchor);
            admitAs(helper, battle, administrator, blueViewer, Faction.BLUE);
            admitAs(helper, battle, administrator, redViewer, Faction.RED);

            InteractionResult firstUse = useBeacon(level, anchor, administrator,
                    new ItemStack(Items.BLUE_DYE));
            helper.assertTrue(firstUse.consumesAction(),
                    "蓝色染料右键部署信标必须由服务端消费");
            FieldDeploymentPoint blue = savedData.fieldPointAt(
                    level.dimension().location(), anchor).orElseThrow();
            helper.assertTrue(blue.faction() == Faction.BLUE
                            && blue.anchorPosition().equals(anchor),
                    "首次绑定必须把真实方块坐标保存为蓝方 anchor");
            assertMode(helper, level, anchor, DeploymentBeaconMode.BLUE,
                    "成功绑定后方块状态必须切换为 BLUE");
            helper.assertTrue(deployment.beaconFactionAt(level, anchor)
                            .orElse(null) == Faction.BLUE,
                    "物理 MODE 与持久化记录一致时必须查询到蓝方归属");

            DeploymentPoint blueProjection = deployment.pointsFor(blueViewer).stream()
                    .filter(point -> point.id().equals(blue.id())).findFirst().orElseThrow();
            helper.assertTrue(blueProjection.kind() == DeploymentPointKind.FIELD_BEACON,
                    "信标投影必须显式标记为 FIELD_BEACON");
            helper.assertTrue(deployment.pointsFor(redViewer).stream()
                            .noneMatch(point -> point.id().equals(blue.id())),
                    "红方客户端不得收到蓝方信标");
            helper.assertTrue(deployment.selectPoint(blueViewer, blue.id()).success(),
                    "蓝方等待玩家必须能选择己方信标");

            useBeacon(level, anchor, administrator, new ItemStack(Items.BLUE_DYE));
            FieldDeploymentPoint repeated = savedData.fieldPointAt(
                    level.dimension().location(), anchor).orElseThrow();
            helper.assertTrue(repeated.id().equals(blue.id()),
                    "同阵营重复绑定必须幂等并保留稳定 UUID");
            helper.assertTrue(blue.id().equals(
                            deployment.viewFor(blueViewer).selectedPointId()),
                    "幂等绑定不得清除仍有效的己方选择");

            useBeacon(level, anchor, administrator, new ItemStack(Items.RED_DYE));
            FieldDeploymentPoint red = savedData.fieldPointAt(
                    level.dimension().location(), anchor).orElseThrow();
            helper.assertTrue(red.faction() == Faction.RED,
                    "改阵营绑定必须建立红方归属");
            helper.assertTrue(savedData.fieldPoints(Faction.BLUE).stream()
                            .noneMatch(point -> point.anchorPosition().equals(anchor)
                                    && point.dimension().equals(level.dimension().location())),
                    "改阵营绑定必须彻底清除旧蓝方归属和空间索引");
            helper.assertTrue(deployment.viewFor(blueViewer).selectedPointId() == null,
                    "改阵营必须清除旧方玩家对该 anchor 的选择");
            helper.assertTrue(deployment.pointsFor(blueViewer).stream()
                            .noneMatch(point -> point.id().equals(red.id()))
                            && deployment.pointsFor(redViewer).stream()
                            .anyMatch(point -> point.id().equals(red.id())
                                    && point.kind() == DeploymentPointKind.FIELD_BEACON),
                    "重新绑定后信标只能投影给新阵营");
            assertMode(helper, level, anchor, DeploymentBeaconMode.RED,
                    "改阵营成功后方块状态必须切换为 RED");
            helper.assertTrue(deployment.beaconFactionAt(level, anchor)
                            .orElse(null) == Faction.RED,
                    "改阵营后物理 MODE 与持久化归属必须同时更新");
            helper.assertTrue(deployment.selectPoint(redViewer, red.id()).success(),
                    "红方等待玩家必须能选择重新绑定后的信标");

            helper.assertTrue(level.destroyBlock(anchor, false, administrator),
                    "真实 destroyBlock 必须能拆除部署信标");
            helper.assertTrue(savedData.fieldPointAt(level.dimension().location(), anchor)
                            .isEmpty(),
                    "真实拆除必须从持久化 registry 注销 anchor");
            helper.assertTrue(deployment.pointsFor(redViewer).stream()
                            .noneMatch(point -> point.id().equals(red.id())),
                    "拆除后不得继续向阵营快照暴露旧信标");
            helper.assertTrue(deployment.viewFor(redViewer).selectedPointId() == null,
                    "拆除必须清除所有玩家对旧 UUID 的选择");
        } finally {
            deployment.onBeaconRemoved(level, anchor);
            patch.restore();
            if (previous != null) {
                savedData.putFieldPoint(previous);
            }
            deployment.forget(blueViewer.getUUID());
            deployment.forget(redViewer.getUUID());
            battle.removeFromBattle(administrator, blueViewer.getUUID());
            battle.removeFromBattle(administrator, redViewer.getUUID());
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = WokInfantryMod.MOD_ID,
            template = "wok_empty", timeoutTicks = 400)
    public static void failedBindingsRejectPermissionInternalDimensionsAndUnsafeLanding(
            GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        DeploymentSavedData isolatedData = new DeploymentSavedData();
        DeploymentService isolated = new DeploymentService(server, isolatedData);
        WorldPatch patch = new WorldPatch();
        ServerPlayer administrator = player(level, "beacon-valid-admin", true);
        ServerPlayer denied = player(level, "beacon-denied", false);
        BlockPos acceptedAnchor = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos deniedAnchor = helper.absolutePos(new BlockPos(10, 1, 1));
        BlockPos limitAnchor = helper.absolutePos(new BlockPos(20, 1, 1));
        ServerLevel holding = server.getLevel(DeploymentService.HOLDING_LEVEL);
        ServerLevel lobby = server.getLevel(DeploymentService.LOBBY_LEVEL);
        helper.assertTrue(holding != null && lobby != null,
                "GameTest 必须加载 holding 和 lobby 内部维度");
        BlockPos holdingAnchor = new BlockPos(120, 64, 120);
        BlockPos lobbyAnchor = new BlockPos(-120, 64, 120);
        BlockPos unsafeAnchor = new BlockPos(acceptedAnchor.getX() + 32,
                level.getMaxBuildHeight() - 1, acceptedAnchor.getZ() + 32);

        try {
            isolatedData.setMainBase(new DeploymentPoint(UUID.randomUUID(), Faction.BLUE,
                    level.dimension().location(), acceptedAnchor.offset(0, 0, -8), 0.0F,
                    DeploymentPoint.DEFAULT_SUPPLY_RADIUS));
            prepareBeacon(patch, level, acceptedAnchor);
            ActionResult accepted = isolated.bindBeacon(administrator, Faction.BLUE, level,
                    acceptedAnchor, 15.0F);
            helper.assertTrue(accepted.success(),
                    "安全位置的真实信标必须可由权限2管理员绑定：" + accepted.message());
            FieldDeploymentPoint original = isolatedData.fieldPointAt(
                    level.dimension().location(), acceptedAnchor).orElseThrow();
            CompoundTag baseline = isolatedData.save(new CompoundTag());
            assertMode(helper, level, acceptedAnchor, DeploymentBeaconMode.BLUE,
                    "基准信标必须保持 BLUE 状态");

            prepareBeacon(patch, level, deniedAnchor);
            ActionResult noPermission = isolated.bindBeacon(denied, Faction.RED, level,
                    deniedAnchor, 0.0F);
            assertRejectedWithoutMutation(helper, noPermission, ActionResult.Code.NOT_AUTHORIZED,
                    isolatedData, baseline, original, level, acceptedAnchor, deniedAnchor,
                    "低于权限2的玩家");
            assertMode(helper, level, deniedAnchor, DeploymentBeaconMode.UNBOUND,
                    "无权限绑定失败后候选方块必须保持 UNBOUND");

            prepareBeacon(patch, holding, holdingAnchor);
            ServerPlayer holdingAdmin = player(holding, "beacon-holding-admin", true);
            ActionResult internalHolding = isolated.bindBeacon(holdingAdmin, Faction.RED,
                    holding, holdingAnchor, 0.0F);
            assertRejectedWithoutMutation(helper, internalHolding,
                    ActionResult.Code.INVALID_DEPLOYMENT_POINT, isolatedData, baseline, original,
                    level, acceptedAnchor, holdingAnchor, "holding 内部维度");
            assertMode(helper, holding, holdingAnchor, DeploymentBeaconMode.UNBOUND,
                    "holding 拒绝后真实方块必须保持 UNBOUND");

            prepareBeacon(patch, lobby, lobbyAnchor);
            ServerPlayer lobbyAdmin = player(lobby, "beacon-lobby-admin", true);
            ActionResult internalLobby = isolated.bindBeacon(lobbyAdmin, Faction.BLUE,
                    lobby, lobbyAnchor, 0.0F);
            assertRejectedWithoutMutation(helper, internalLobby,
                    ActionResult.Code.INVALID_DEPLOYMENT_POINT, isolatedData, baseline, original,
                    level, acceptedAnchor, lobbyAnchor, "lobby 内部维度");
            assertMode(helper, lobby, lobbyAnchor, DeploymentBeaconMode.UNBOUND,
                    "lobby 拒绝后真实方块必须保持 UNBOUND");

            patch.set(level, unsafeAnchor,
                    InfantryBlocks.DEPLOYMENT_BEACON.get().defaultBlockState());
            ActionResult unsafe = isolated.bindBeacon(administrator, Faction.RED, level,
                    unsafeAnchor, 0.0F);
            assertRejectedWithoutMutation(helper, unsafe,
                    ActionResult.Code.INVALID_DEPLOYMENT_POINT, isolatedData, baseline, original,
                    level, acceptedAnchor, unsafeAnchor, "没有安全落脚位置的信标");
            assertMode(helper, level, unsafeAnchor, DeploymentBeaconMode.UNBOUND,
                    "无安全落脚点拒绝后候选方块必须保持 UNBOUND");

            for (int index = 1; index < DeploymentSavedData.MAX_FIELD_POINTS_PER_FACTION;
                 index++) {
                BlockPos seededAnchor = new BlockPos(1_000 + index * 4, 64, 1_000);
                boolean inserted = isolatedData.putFieldPoint(new FieldDeploymentPoint(
                        UUID.randomUUID(), Faction.BLUE, level.dimension().location(),
                        seededAnchor, seededAnchor.east().above(), 0.0F));
                helper.assertTrue(inserted, "前15个蓝方信标记录必须都能占用独立名额");
            }
            helper.assertTrue(isolatedData.mainBase(Faction.BLUE).isPresent()
                            && isolatedData.fieldPoints(Faction.BLUE).size() == 15,
                    "蓝方必须能同时拥有1个主基地和15个前线信标");
            CompoundTag atLimit = isolatedData.save(new CompoundTag());
            prepareBeacon(patch, level, limitAnchor);
            ActionResult overLimit = isolated.bindBeacon(administrator, Faction.BLUE, level,
                    limitAnchor, 0.0F);
            assertRejectedWithoutMutation(helper, overLimit,
                    ActionResult.Code.INVALID_DEPLOYMENT_POINT, isolatedData, atLimit, original,
                    level, acceptedAnchor, limitAnchor, "第16个蓝方信标");
            assertMode(helper, level, limitAnchor, DeploymentBeaconMode.UNBOUND,
                    "达到15个信标上限后候选方块必须保持 UNBOUND");
        } finally {
            patch.restore();
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = WokInfantryMod.MOD_ID,
            template = "wok_empty", timeoutTicks = 400)
    public static void consoleCommandPlacesBindsProtectsAndRemovesBeacon(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        DeploymentSavedData savedData = DeploymentSavedData.get(server);
        DeploymentService deployment = DeploymentService.get(server).orElseThrow();
        WorldPatch patch = new WorldPatch();
        BlockPos anchor = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos deniedAnchor = anchor.offset(8, 0, 0);
        FieldDeploymentPoint previous = savedData.fieldPointAt(
                level.dimension().location(), anchor).orElse(null);

        try {
            if (previous != null) {
                deployment.onBeaconRemoved(level, anchor);
            }
            prepareBeacon(patch, level, anchor);
            patch.set(level, anchor, Blocks.AIR.defaultBlockState());
            prepareBeacon(patch, level, deniedAnchor);
            patch.set(level, deniedAnchor, Blocks.AIR.defaultBlockState());

            String dimension = level.dimension().location().toString();
            String place = "battle deployment beacon place blue " + dimension + " "
                    + anchor.getX() + " " + anchor.getY() + " " + anchor.getZ() + " 135";
            int placed = server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack().withPermission(
                            BattleRules.ADMIN_PERMISSION_LEVEL), place);
            helper.assertTrue(placed == 1,
                    "权限2控制台必须能原子放置并绑定蓝方部署信标");
            FieldDeploymentPoint point = savedData.fieldPointAt(
                    level.dimension().location(), anchor).orElseThrow();
            helper.assertTrue(point.faction() == Faction.BLUE
                            && Float.compare(point.yaw(), 135.0F) == 0,
                    "控制台指令必须保存显式阵营和 yaw");
            assertMode(helper, level, anchor, DeploymentBeaconMode.BLUE,
                    "控制台放置成功后物理方块必须呈蓝方模式");

            ServerPlayer deniedBreaker = player(level, "beacon-break-no", false);
            BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(level, anchor,
                    level.getBlockState(anchor), deniedBreaker);
            MinecraftForge.EVENT_BUS.post(breakEvent);
            helper.assertTrue(breakEvent.isCanceled()
                            && level.getBlockState(anchor).is(
                            InfantryBlocks.DEPLOYMENT_BEACON.get()),
                    "非管理员的破坏事件必须被取消，信标不得注销");

            String denied = "battle deployment beacon place red " + dimension + " "
                    + deniedAnchor.getX() + " " + deniedAnchor.getY() + " "
                    + deniedAnchor.getZ();
            int deniedResult = server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack().withPermission(
                            BattleRules.ADMIN_PERMISSION_LEVEL - 1), denied);
            helper.assertTrue(deniedResult == 0 && level.getBlockState(deniedAnchor).isAir()
                            && savedData.fieldPointAt(level.dimension().location(), deniedAnchor)
                            .isEmpty(),
                    "低权限命令不得放下方块或污染部署点存档");

            String remove = "battle deployment beacon remove " + dimension + " "
                    + anchor.getX() + " " + anchor.getY() + " " + anchor.getZ();
            int removed = server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack().withPermission(
                            BattleRules.ADMIN_PERMISSION_LEVEL), remove);
            helper.assertTrue(removed == 1 && level.getBlockState(anchor).isAir()
                            && savedData.fieldPointAt(level.dimension().location(), anchor)
                            .isEmpty(),
                    "控制台移除必须同时删除物理信标和持久化部署点");
        } finally {
            deployment.onBeaconRemoved(level, anchor);
            deployment.onBeaconRemoved(level, deniedAnchor);
            patch.restore();
            if (previous != null) {
                savedData.putFieldPoint(previous);
            }
        }
        helper.succeed();
    }

    private static void assertRejectedWithoutMutation(GameTestHelper helper, ActionResult result,
                                                       ActionResult.Code expectedCode,
                                                       DeploymentSavedData data,
                                                       CompoundTag baseline,
                                                       FieldDeploymentPoint original,
                                                       ServerLevel acceptedLevel,
                                                       BlockPos acceptedAnchor,
                                                       BlockPos rejectedAnchor,
                                                       String scenario) {
        helper.assertTrue(!result.success() && result.code() == expectedCode,
                scenario + "必须稳定拒绝，实际为 " + result.code() + ": " + result.message());
        helper.assertTrue(baseline.equals(data.save(new CompoundTag())),
                scenario + "失败后 SavedData 必须逐字节等价，不能污染任何索引");
        helper.assertTrue(original.equals(data.fieldPoint(original.id()).orElseThrow())
                        && original.equals(data.fieldPointAt(original.dimension(),
                        original.anchorPosition()).orElseThrow()),
                scenario + "失败后旧部署点及 UUID/anchor 索引必须完整保留");
        assertMode(helper, acceptedLevel, acceptedAnchor, DeploymentBeaconMode.BLUE,
                scenario + "失败不得修改既有信标的可视归属");
        if (acceptedLevel.dimension().location().equals(original.dimension())
                && rejectedAnchor.equals(original.anchorPosition())) {
            helper.fail(scenario + "测试候选不得与基准 anchor 重合");
        }
    }

    private static void admitAs(GameTestHelper helper, BattleService battle,
                                ServerPlayer administrator, ServerPlayer player,
                                Faction faction) {
        ActionResult admission = battle.ensurePlayer(player);
        helper.assertTrue(admission.success(),
                "测试 viewer 加入战局失败：" + admission.code());
        ActionResult assignment = battle.forceAssignFaction(administrator, player.getUUID(),
                faction);
        helper.assertTrue(assignment.success(),
                "测试 viewer 阵营分配失败：" + assignment.code());
    }

    private static InteractionResult useBeacon(ServerLevel level, BlockPos anchor,
                                               ServerPlayer player, ItemStack held) {
        ItemStack previous = player.getItemInHand(InteractionHand.MAIN_HAND).copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        try {
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(anchor), Direction.UP,
                    anchor, false);
            return level.getBlockState(anchor).use(level, player,
                    InteractionHand.MAIN_HAND, hit);
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, previous);
        }
    }

    private static void prepareBeacon(WorldPatch patch, ServerLevel level, BlockPos anchor) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos feet = anchor.offset(x, 0, z);
                patch.set(level, feet.below(), Blocks.STONE.defaultBlockState());
                patch.set(level, feet, Blocks.AIR.defaultBlockState());
                patch.set(level, feet.above(), Blocks.AIR.defaultBlockState());
            }
        }
        patch.set(level, anchor, InfantryBlocks.DEPLOYMENT_BEACON.get().defaultBlockState());
    }

    private static void assertMode(GameTestHelper helper, ServerLevel level, BlockPos anchor,
                                   DeploymentBeaconMode expected, String message) {
        BlockState state = level.getBlockState(anchor);
        helper.assertTrue(state.is(InfantryBlocks.DEPLOYMENT_BEACON.get())
                        && state.getValue(DeploymentBeaconBlock.MODE) == expected,
                message);
    }

    private static ServerPlayer player(ServerLevel level, String name, boolean administrator) {
        MinecraftServer server = level.getServer();
        String safeName = name.length() > 16 ? name.substring(0, 16) : name;
        return new ServerPlayer(server, level,
                new GameProfile(UUID.randomUUID(), safeName)) {
            @Override
            public boolean hasPermissions(int permissionLevel) {
                return administrator && permissionLevel <= BattleRules.ADMIN_PERMISSION_LEVEL;
            }

            @Override
            public void sendSystemMessage(Component message) {
                // GameTest players intentionally have no network connection. Physical block use
                // still exercises production feedback, but its delivery is irrelevant here.
            }
        };
    }

    /** Records every touched block so tests also clean changes made outside their tiny template. */
    private static final class WorldPatch {
        private final Map<ServerLevel, LinkedHashMap<BlockPos, BlockState>> originals =
                new IdentityHashMap<>();

        void set(ServerLevel level, BlockPos position, BlockState state) {
            BlockPos immutable = position.immutable();
            originals.computeIfAbsent(level, ignored -> new LinkedHashMap<>())
                    .putIfAbsent(immutable, level.getBlockState(immutable));
            level.setBlock(immutable, state, 3);
        }

        void restore() {
            originals.forEach((level, states) -> {
                List<Map.Entry<BlockPos, BlockState>> entries =
                        new ArrayList<>(states.entrySet());
                for (int index = entries.size() - 1; index >= 0; index--) {
                    Map.Entry<BlockPos, BlockState> entry = entries.get(index);
                    level.setBlock(entry.getKey(), entry.getValue(), 3);
                }
            });
            originals.clear();
        }
    }
}
