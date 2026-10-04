package com.wok.infantry.deployment;

import com.mojang.authlib.GameProfile;
import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.formation.FactionDefinition;
import com.wok.infantry.formation.vote.FormationVotePhase;
import com.wok.infantry.formation.vote.FormationVoteSavedData;
import com.wok.infantry.loadout.LoadoutClassDefinition;
import com.wok.infantry.loadout.LoadoutConfigData;
import com.wok.infantry.loadout.LoadoutSlotDefinition;
import com.wok.infantry.server.FormationService;
import com.wok.infantry.server.LoadoutService;
import com.wok.infantry.testmode.TestModeRules;
import com.wok.infantry.testmode.TestModeSavedData;
import com.wok.infantry.testmode.TestModeService;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Forge coverage of the server-wide test mode (0.4.0-beta.2): main bases for both sides, the
 * boss bar, the one-click test start from the black waiting space and the countdown coming back
 * once the mode is off. The tests run on the shared services of the GameTest world (its
 * {@code run/world} is also the UI acceptance seed), so everything they change is put back.
 */
@GameTestHolder(WokInfantryMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TestModeGameTests {
    private static final String ACTOR = "gametest";

    private TestModeGameTests() {
    }

    @GameTest(templateNamespace = WokInfantryMod.MOD_ID,
            template = "wok_empty", timeoutTicks = 200)
    public static void testModeAddsBothMainBasesAndShowsItsBossBarUntilTurnedOff(
            GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        TestModeService testMode = TestModeService.get(server).orElseThrow();
        DeploymentService deployment = DeploymentService.get(server).orElseThrow();
        DeploymentSavedData savedData = DeploymentSavedData.get(server);
        DeploymentPoint previousBlue = deployment.mainBase(Faction.BLUE).orElse(null);
        DeploymentPoint previousRed = deployment.mainBase(Faction.RED).orElse(null);
        boolean wasEnabled = testMode.enabled();
        try {
            if (wasEnabled) {
                testMode.setEnabled(false, ACTOR);
            }
            savedData.clearMainBase(Faction.BLUE);
            savedData.clearMainBase(Faction.RED);

            TestModeService.Report on = testMode.setEnabled(true, ACTOR);
            helper.assertTrue(on.success() && testMode.enabled()
                            && TestModeService.isEnabled(server),
                    "开启命令必须打开全服测试模式：" + on.lines());
            helper.assertTrue(TestModeSavedData.get(server).enabled(),
                    "测试模式开关必须写进存档，重启后保留");
            helper.assertTrue(testMode.bossBarShown(), "开启后必须显示测试模式 Boss 条");

            ServerLevel overworld = server.overworld();
            BlockPos spawn = overworld.getSharedSpawnPos();
            DeploymentPoint blue = deployment.mainBase(Faction.BLUE).orElse(null);
            DeploymentPoint red = deployment.mainBase(Faction.RED).orElse(null);
            helper.assertTrue(blue != null && red != null,
                    "没有主基地的两方都必须补出主基地：" + on.lines());
            helper.assertTrue(blue.dimension().equals(Level.OVERWORLD.location())
                            && red.dimension().equals(Level.OVERWORLD.location()),
                    "补出的主基地必须在主世界");
            helper.assertTrue(overworld.getWorldBorder().isWithinBounds(blue.position())
                            && overworld.getWorldBorder().isWithinBounds(red.position()),
                    "补出的主基地必须在世界边界内");
            // The landing search may move 2 blocks; an ocean spawn moves to the nearest shore
            // within the 24-block search square (the GameTest world spawns in water).
            int reach = TestModeRules.BASE_SEARCH_RADIUS + 2;
            helper.assertTrue(Math.abs(blue.position().getX() - spawn.getX()) <= reach
                            && Math.abs(blue.position().getZ() - spawn.getZ()) <= reach,
                    "蓝方主基地必须在出生点附近的安全落脚处：" + blue.position() + " / " + spawn);
            helper.assertTrue(Math.abs(Math.abs(red.position().getX() - spawn.getX()) - 64)
                            <= reach && Math.abs(red.position().getZ() - spawn.getZ()) <= reach,
                    "红方主基地必须在出生点 +64 X（东边不行时 -64 X）附近的安全落脚处："
                            + red.position() + " / " + spawn);
            helper.assertTrue(on.lines().stream().anyMatch(line -> line.contains("出生点 +64 X")
                            || line.contains("出生点 -64 X")),
                    "回执必须写明红方主基地相对出生点的位置：" + on.lines());

            TestModeService.Report again = testMode.setEnabled(true, ACTOR);
            helper.assertTrue(again.success() && testMode.enabled()
                            && deployment.mainBase(Faction.BLUE).orElseThrow().id().equals(blue.id())
                            && deployment.mainBase(Faction.RED).orElseThrow().id().equals(red.id()),
                    "重复开启不得改动已有主基地");

            TestModeService.Report off = testMode.setEnabled(false, ACTOR);
            helper.assertTrue(off.success() && !testMode.enabled()
                            && !TestModeSavedData.get(server).enabled(),
                    "关闭命令必须关掉测试模式并写进存档");
            helper.assertFalse(testMode.bossBarShown(), "关闭后必须移除测试模式 Boss 条");
            helper.assertTrue(testMode.bossBarViewers().isEmpty(), "关闭后 Boss 条不得留给任何人");
            helper.assertTrue(deployment.mainBase(Faction.BLUE).orElseThrow().id().equals(blue.id())
                            && deployment.mainBase(Faction.RED).orElseThrow().id().equals(red.id()),
                    "测试模式补出的主基地关闭后必须保留");
            helper.assertTrue(off.lines().stream().anyMatch(line -> line.contains("保留")),
                    "关闭回执必须说明补出的主基地保留");
        } finally {
            if (testMode.enabled() != wasEnabled) {
                testMode.setEnabled(wasEnabled, ACTOR);
            }
            restoreBase(savedData, Faction.BLUE, previousBlue);
            restoreBase(savedData, Faction.RED, previousRed);
        }
        helper.succeed();
    }

    /**
     * An administrator in the black waiting space (lobby dimension) runs the test start: the
     * faction's vote is locked, the administrator joins it, leads a new squad and is deployed
     * from the main base in survival. The loadout catalog is swapped in memory for one without
     * slots so the deployment does not depend on the GameTest world's loadout file (issuing a
     * real kit is covered elsewhere).
     */
    @GameTest(templateNamespace = WokInfantryMod.MOD_ID,
            template = "wok_empty", timeoutTicks = 200)
    public static void testStartDeploysAWaitingAdministratorAndOffRestoresTheCountdown(
            GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        TestModeService testMode = TestModeService.get(server).orElseThrow();
        DeploymentService deployment = DeploymentService.get(server).orElseThrow();
        DeploymentSavedData savedData = DeploymentSavedData.get(server);
        BattleService battle = BattleService.get(server).orElseThrow();
        FormationService formations = FormationService.get(server).orElseThrow();
        LoadoutService loadouts = LoadoutService.get(server).orElseThrow();
        ServerLevel lobby = server.getLevel(DeploymentService.LOBBY_LEVEL);
        helper.assertTrue(lobby != null, "GameTest 必须加载 lobby 维度");

        FactionDefinition faction = formations.catalog().factions().stream()
                .filter(FactionDefinition::enabled).findFirst().orElse(null);
        helper.assertTrue(faction != null, "GameTest 目录必须至少有一个启用的公开阵营");
        Faction side = faction.battleSide();
        helper.assertTrue(formations.voteSnapshot(side, null).phase()
                        == FormationVotePhase.NOT_STARTED,
                faction.id() + " 的编制投票必须未开始（上次运行没有清理？）");
        DeploymentPoint previousBlue = deployment.mainBase(Faction.BLUE).orElse(null);
        DeploymentPoint previousRed = deployment.mainBase(Faction.RED).orElse(null);
        boolean wasEnabled = testMode.enabled();
        LoadoutConfigData slotless = loadouts.catalogForGameTest().copy();
        for (LoadoutClassDefinition definition : slotless.classes()) {
            for (LoadoutSlotDefinition slot : List.copyOf(definition.slotDefinitions())) {
                definition.deleteSlot(slot.id());
            }
        }
        LoadoutConfigData previousLoadouts = loadouts.swapCatalogForGameTest(slotless);
        UUID playerId = UUID.nameUUIDFromBytes(
                "wok-infantry-test-start".getBytes(StandardCharsets.UTF_8));
        ServerPlayer administrator = new FakePlayer(server.overworld(),
                new GameProfile(playerId, "wok-test-start")) {
            @Override
            public boolean hasPermissions(int permissionLevel) {
                return true;
            }
        };
        try {
            if (wasEnabled) {
                testMode.setEnabled(false, ACTOR);
            }
            // The black space where a player without a faction waits.
            administrator.teleportTo(lobby, 0.5D, 2.0D, 0.5D, 0.0F, 0.0F);
            helper.assertTrue(administrator.serverLevel() == lobby,
                    "测试前管理员必须在黑色等待空间");

            TestModeService.Report report = testMode.testStart(administrator, null,
                    faction.id(), null);
            helper.assertTrue(report.success(), "测试开局必须成功：" + report.lines());
            helper.assertTrue(testMode.enabled(), "测试开局必须先开启全服测试模式");
            helper.assertTrue(formations.voteSnapshot(side, null).phase()
                            == FormationVotePhase.LOCKED,
                    "未锁定的阵营必须被锁定为所选编制");
            String formationId = battle.formationOf(playerId).orElse("");
            helper.assertTrue(battle.factionOf(playerId).orElse(null) == side
                            && formationId.equals(formations.voteSnapshot(side, null)
                            .lockedFormationId()),
                    "管理员必须加入该阵营并使用锁定编制");
            helper.assertTrue(battle.squadOf(playerId).isPresent()
                            && battle.isSquadLeader(playerId),
                    "管理员必须在新建的小队里当队长");
            helper.assertTrue(deployment.isActive(playerId), "管理员必须已部署");
            helper.assertTrue(administrator.serverLevel().dimension().equals(Level.OVERWORLD),
                    "部署后必须离开等待空间回到主世界");
            helper.assertTrue(administrator.gameMode.getGameModeForPlayer() == GameType.SURVIVAL,
                    "测试开局必须以生存模式部署");
            DeploymentPoint base = deployment.mainBase(side).orElse(null);
            helper.assertTrue(base != null
                            && administrator.blockPosition().distManhattan(base.position()) <= 6,
                    "必须从本方主基地部署：" + administrator.blockPosition() + " / "
                            + (base == null ? "无主基地" : base.position()));

            // A new waiting life (what a death or "重新部署" starts) is checked through a fresh
            // deployment record, so the test charges no manpower to the GameTest world.
            deployment.forget(playerId);
            DeploymentView on = deployment.viewFor(administrator);
            helper.assertTrue(on.phase() == DeploymentPhase.READY && on.waitingTicks() == 0L,
                    "测试模式下部署倒计时与重生等待必须为 0：" + on.phase() + " "
                            + on.waitingTicks());

            testMode.setEnabled(false, ACTOR);
            helper.assertFalse(testMode.bossBarShown(), "关闭后必须移除测试模式 Boss 条");
            deployment.forget(playerId);
            DeploymentView off = deployment.viewFor(administrator);
            helper.assertTrue(off.phase() == DeploymentPhase.WAITING && off.waitingTicks() > 0L,
                    "关闭测试模式后部署倒计时必须恢复：" + off.phase() + " " + off.waitingTicks());

            // Turning it on again ends the running countdown at once.
            testMode.setEnabled(true, ACTOR);
            DeploymentView skipped = deployment.viewFor(administrator);
            helper.assertTrue(skipped.phase() == DeploymentPhase.READY
                            && skipped.waitingTicks() == 0L,
                    "开启测试模式必须结束正在进行的部署倒计时");
            testMode.setEnabled(false, ACTOR);
        } finally {
            if (testMode.enabled() != wasEnabled) {
                testMode.setEnabled(wasEnabled, ACTOR);
            }
            deployment.forget(playerId);
            battle.removeFromBattle(administrator, playerId);
            FormationVoteSavedData.get(server).clear(side);
            loadouts.swapCatalogForGameTest(previousLoadouts);
            restoreBase(savedData, Faction.BLUE, previousBlue);
            restoreBase(savedData, Faction.RED, previousRed);
            administrator.serverLevel().removePlayerImmediately(administrator,
                    Entity.RemovalReason.DISCARDED);
        }
        helper.succeed();
    }

    private static void restoreBase(DeploymentSavedData savedData, Faction faction,
                                    DeploymentPoint previous) {
        savedData.clearMainBase(faction);
        if (previous != null) {
            savedData.setMainBase(previous);
        }
    }
}
