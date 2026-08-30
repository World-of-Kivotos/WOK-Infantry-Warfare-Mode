package com.wok.infantry.deployment;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.ParseResults;
import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.battle.ActionResult;
import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.battle.Faction;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Forge coverage for fail-closed administrator deployment-base setup. */
@GameTestHolder(WokInfantryMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DeploymentGameTests {
    private DeploymentGameTests() {
    }

    @GameTest(templateNamespace = WokInfantryMod.MOD_ID,
            template = "wok_empty", timeoutTicks = 200)
    public static void baseCandidatesAreValidatedBeforePersistence(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        DeploymentService service = new DeploymentService(server, new DeploymentSavedData());
        ServerPlayer administrator = administrator(level, server);

        BlockPos safeFeet = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(safeFeet.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(safeFeet, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(safeFeet.above(), Blocks.AIR.defaultBlockState(), 3);
        ActionResult valid = service.setMainBaseAt(administrator, Faction.BLUE, level,
                safeFeet, 37.5F);
        helper.assertTrue(valid.success(),
                "有实心地板和两格净空的主基地必须成功：" + valid.message());
        DeploymentPoint accepted = service.mainBase(Faction.BLUE).orElseThrow();
        helper.assertTrue(accepted.position().equals(safeFeet)
                        && accepted.dimension().equals(level.dimension().location()),
                "成功候选必须按权威维度和位置落盘");

        ServerLevel holding = server.getLevel(DeploymentService.HOLDING_LEVEL);
        helper.assertTrue(holding != null, "GameTest 必须加载 holding 维度");
        ActionResult internal = service.setMainBaseAt(administrator, Faction.BLUE, holding,
                new BlockPos(-4_097, 64, 0), 0.0F);
        helper.assertFalse(internal.success(), "内部等待维度必须被拒绝");
        helper.assertTrue(internal.code() == ActionResult.Code.INVALID_DEPLOYMENT_POINT
                        && internal.message().contains("/battle deployment setbase"),
                "内部维度拒绝必须返回部署点错误和显式 setbase 提示");
        assertUnchanged(helper, service, accepted,
                "内部维度失败候选不得覆盖既有主基地");

        ServerLevel lobby = server.getLevel(DeploymentService.LOBBY_LEVEL);
        helper.assertTrue(lobby != null, "GameTest 必须加载 lobby 维度");
        ActionResult internalLobby = service.setMainBaseAt(administrator, Faction.BLUE, lobby,
                new BlockPos(0, 2, 0), 0.0F);
        helper.assertFalse(internalLobby.success(), "内部大厅维度必须被拒绝");
        helper.assertTrue(internalLobby.code() == ActionResult.Code.INVALID_DEPLOYMENT_POINT
                        && internalLobby.message().contains("/battle deployment setbase"),
                "大厅维度拒绝必须返回部署点错误和显式 setbase 提示");
        assertUnchanged(helper, service, accepted,
                "大厅维度失败候选不得覆盖既有主基地");

        BlockPos unsafe = new BlockPos(safeFeet.getX() + 16,
                level.getMaxBuildHeight() - 1, safeFeet.getZ() + 16);
        ActionResult noLanding = service.setMainBaseAt(administrator, Faction.BLUE, level,
                unsafe, 90.0F);
        helper.assertFalse(noLanding.success(), "无安全落脚位置的候选必须被拒绝");
        helper.assertTrue(noLanding.code() == ActionResult.Code.INVALID_DEPLOYMENT_POINT
                        && noLanding.message().contains("/battle deployment setbase"),
                "无落脚点拒绝必须返回部署点错误和显式 setbase 提示");
        assertUnchanged(helper, service, accepted,
                "无落脚点失败候选不得覆盖既有主基地");

        BlockPos waterFeet = safeFeet.offset(32, 0, 0);
        prepareHazardArea(level, waterFeet, Blocks.STONE.defaultBlockState(),
                Blocks.WATER.defaultBlockState());
        assertUnsafeCandidate(helper, service, administrator, accepted, level, waterFeet,
                "水中的落脚点");

        BlockPos lavaFeet = safeFeet.offset(48, 0, 0);
        prepareHazardArea(level, lavaFeet, Blocks.STONE.defaultBlockState(),
                Blocks.LAVA.defaultBlockState());
        assertUnsafeCandidate(helper, service, administrator, accepted, level, lavaFeet,
                "岩浆中的落脚点");

        BlockPos fireFeet = safeFeet.offset(64, 0, 0);
        prepareHazardArea(level, fireFeet, Blocks.STONE.defaultBlockState(),
                Blocks.FIRE.defaultBlockState());
        assertUnsafeCandidate(helper, service, administrator, accepted, level, fireFeet,
                "着火的落脚点");

        BlockPos magmaFeet = safeFeet.offset(80, 0, 0);
        prepareHazardArea(level, magmaFeet, Blocks.MAGMA_BLOCK.defaultBlockState(),
                Blocks.AIR.defaultBlockState());
        assertUnsafeCandidate(helper, service, administrator, accepted, level, magmaFeet,
                "岩浆块地板");
        helper.succeed();
    }

    @GameTest(templateNamespace = WokInfantryMod.MOD_ID,
            template = "wok_empty", timeoutTicks = 200)
    public static void explicitBaseCommandSupportsPlayerConsoleAndFailClosedValidation(
            GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        DeploymentService service = DeploymentService.get(server).orElseThrow();
        DeploymentSavedData savedData = DeploymentSavedData.get(server);
        DeploymentPoint previousBlue = service.mainBase(Faction.BLUE).orElse(null);
        DeploymentPoint previousRed = service.mainBase(Faction.RED).orElse(null);
        ServerPlayer administrator = administrator(level, server);

        BlockPos legacyFeet = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos playerExplicitFeet = helper.absolutePos(new BlockPos(1, 1, 0));
        BlockPos consoleExplicitFeet = helper.absolutePos(new BlockPos(2, 1, 0));
        BlockPos deniedFeet = helper.absolutePos(new BlockPos(2, 1, 2));
        prepareSafeFeet(level, legacyFeet);
        prepareSafeFeet(level, playerExplicitFeet);
        prepareSafeFeet(level, consoleExplicitFeet);
        prepareSafeFeet(level, deniedFeet);

        float playerYaw = 41.25F;
        CommandSourceStack playerAdmin = playerSource(administrator, level, legacyFeet,
                playerYaw, BattleRules.ADMIN_PERMISSION_LEVEL);
        CommandSourceStack consoleAdmin = server.createCommandSourceStack()
                .withPermission(BattleRules.ADMIN_PERMISSION_LEVEL);

        try {
            int legacyResult = run(server, playerAdmin,
                    "battle deployment setbase blue");
            helper.assertTrue(legacyResult == 1,
                    "旧的玩家当前位置 setbase 语法必须继续成功");
            assertBase(helper, service, Faction.BLUE, level, legacyFeet, playerYaw,
                    "旧当前位置模式必须保存玩家命令源的位置和朝向");

            String dimension = level.dimension().location().toString();
            int playerExplicitResult = run(server, playerAdmin,
                    explicitCommand(Faction.RED, dimension, playerExplicitFeet, null));
            helper.assertTrue(playerExplicitResult == 1,
                    "玩家管理员必须能执行显式维度/坐标且省略 yaw 的 setbase");
            assertBase(helper, service, Faction.RED, level, playerExplicitFeet, playerYaw,
                    "省略 yaw 时必须采用玩家命令源朝向");

            float consoleYaw = 123.5F;
            int consoleResult = run(server, consoleAdmin,
                    explicitCommand(Faction.BLUE, dimension, consoleExplicitFeet,
                            Float.toString(consoleYaw)));
            helper.assertTrue(consoleResult == 1,
                    "控制台管理员必须能执行含显式 yaw 的 setbase");
            DeploymentPoint accepted = assertBase(helper, service, Faction.BLUE, level,
                    consoleExplicitFeet, consoleYaw,
                    "控制台显式模式必须保存目标维度、坐标和 yaw");

            CommandSourceStack deniedConsole = server.createCommandSourceStack()
                    .withPermission(BattleRules.ADMIN_PERMISSION_LEVEL - 1);
            String deniedCommand = explicitCommand(Faction.BLUE, dimension, deniedFeet, "179");
            ParseResults<CommandSourceStack> adminParse = server.getCommands().getDispatcher()
                    .parse(deniedCommand, consoleAdmin);
            helper.assertTrue(!adminParse.getReader().canRead()
                            && adminParse.getExceptions().isEmpty(),
                    "管理员命令源必须完整解析显式维度、三坐标和可选 yaw");
            ParseResults<CommandSourceStack> deniedParse = server.getCommands().getDispatcher()
                    .parse(deniedCommand, deniedConsole);
            helper.assertTrue(deniedParse.getReader().canRead()
                            || !deniedParse.getExceptions().isEmpty(),
                    "低权限命令源不得通过 setbase 命令树解析");
            int deniedResult = run(server, deniedConsole, deniedCommand);
            helper.assertTrue(deniedResult == 0,
                    "低权限控制台命令源必须在命令树权限门被拒绝");
            assertUnchanged(helper, service, accepted,
                    "低权限显式命令不得覆盖既有主基地");

            CommandSourceStack deniedPlayer = playerSource(administrator, level, deniedFeet,
                    0.0F, BattleRules.ADMIN_PERMISSION_LEVEL - 1);
            ActionResult serviceDenied = service.setMainBaseAt(deniedPlayer, Faction.BLUE,
                    level, deniedFeet, 0.0F);
            helper.assertTrue(!serviceDenied.success()
                            && serviceDenied.code() == ActionResult.Code.NOT_AUTHORIZED,
                    "即使绕过命令树，服务入口也必须拒绝低权限玩家命令源");
            assertUnchanged(helper, service, accepted,
                    "服务权限拒绝不得覆盖既有主基地");

            int holdingResult = run(server, consoleAdmin,
                    "battle deployment setbase blue wok_infantry:holding -4097 64 0 0");
            helper.assertTrue(holdingResult == 0,
                    "显式命令不得把 holding 内部维度设为主基地");
            assertUnchanged(helper, service, accepted,
                    "holding 显式命令失败不得覆盖既有主基地");

            BlockPos unsafe = new BlockPos(consoleExplicitFeet.getX() + 16,
                    level.getMaxBuildHeight() - 1, consoleExplicitFeet.getZ() + 16);
            int unsafeResult = run(server, consoleAdmin,
                    explicitCommand(Faction.BLUE, dimension, unsafe, "270"));
            helper.assertTrue(unsafeResult == 0,
                    "显式命令必须复用服务端安全落点预检");
            assertUnchanged(helper, service, accepted,
                    "无安全落点的显式命令不得覆盖既有主基地");

            int clearFromConsole = run(server, consoleAdmin,
                    "battle deployment clearbase red");
            helper.assertTrue(clearFromConsole == 1
                            && service.mainBase(Faction.RED).isEmpty(),
                    "权限2控制台/RCON 必须能清除阵营主基地");
        } finally {
            restoreBase(savedData, Faction.BLUE, previousBlue);
            restoreBase(savedData, Faction.RED, previousRed);
        }
        helper.succeed();
    }

    private static void assertUnchanged(GameTestHelper helper, DeploymentService service,
                                        DeploymentPoint accepted, String message) {
        DeploymentPoint current = service.mainBase(Faction.BLUE).orElse(null);
        helper.assertTrue(current != null && current.id().equals(accepted.id())
                        && current.position().equals(accepted.position())
                        && current.dimension().equals(accepted.dimension()), message);
    }

    private static DeploymentPoint assertBase(GameTestHelper helper,
                                              DeploymentService service,
                                              Faction faction, ServerLevel level,
                                              BlockPos position, float yaw, String message) {
        DeploymentPoint point = service.mainBase(faction).orElse(null);
        helper.assertTrue(point != null
                        && point.dimension().equals(level.dimension().location())
                        && point.position().equals(position)
                        && Math.abs(point.yaw() - yaw) < 0.001F, message);
        return point;
    }

    private static void prepareSafeFeet(ServerLevel level, BlockPos feet) {
        level.setBlock(feet.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(feet, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(feet.above(), Blocks.AIR.defaultBlockState(), 3);
    }

    private static void prepareHazardArea(ServerLevel level, BlockPos origin,
                                          net.minecraft.world.level.block.state.BlockState floor,
                                          net.minecraft.world.level.block.state.BlockState feet) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos candidate = origin.offset(x, 0, z);
                level.setBlock(candidate.below(), floor, 3);
                level.setBlock(candidate, feet, 3);
                level.setBlock(candidate.above(), Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private static void assertUnsafeCandidate(GameTestHelper helper, DeploymentService service,
                                              ServerPlayer administrator,
                                              DeploymentPoint accepted, ServerLevel level,
                                              BlockPos candidate, String scenario) {
        ActionResult result = service.setMainBaseAt(administrator, Faction.BLUE, level,
                candidate, 0.0F);
        helper.assertTrue(!result.success()
                        && result.code() == ActionResult.Code.INVALID_DEPLOYMENT_POINT,
                scenario + "必须被安全落点校验拒绝");
        assertUnchanged(helper, service, accepted,
                scenario + "失败不得覆盖既有主基地");
    }

    private static CommandSourceStack playerSource(ServerPlayer player, ServerLevel level,
                                                   BlockPos position, float yaw,
                                                   int permissionLevel) {
        return new CommandSourceStack(CommandSource.NULL, Vec3.atBottomCenterOf(position),
                new Vec2(0.0F, yaw), level, permissionLevel,
                player.getScoreboardName(), Component.literal(player.getScoreboardName()),
                level.getServer(), player);
    }

    private static String explicitCommand(Faction faction, String dimension, BlockPos position,
                                          String yaw) {
        String command = "battle deployment setbase " + faction.id() + " " + dimension
                + " " + position.getX() + " " + position.getY() + " " + position.getZ();
        return yaw == null ? command : command + " " + yaw;
    }

    private static int run(MinecraftServer server, CommandSourceStack source, String command) {
        return server.getCommands().performPrefixedCommand(source, command);
    }

    private static void restoreBase(DeploymentSavedData savedData, Faction faction,
                                    DeploymentPoint previous) {
        savedData.clearMainBase(faction);
        if (previous != null) {
            savedData.setMainBase(previous);
        }
    }

    private static ServerPlayer administrator(ServerLevel level, MinecraftServer server) {
        String seed = "wok-infantry-deployment-base-validation";
        return new ServerPlayer(server, level, new GameProfile(
                UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)),
                "wok-base-admin")) {
            @Override
            public boolean hasPermissions(int permissionLevel) {
                return true;
            }
        };
    }
}
