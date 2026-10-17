package com.wok.infantry.stamina;

import com.mojang.authlib.GameProfile;
import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.config.InfantryServerConfig;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Server-object coverage for jump drain and persistence across a replacement player entity. */
@GameTestHolder(WokInfantryMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StaminaGameTests {
    private StaminaGameTests() {
    }

    @GameTest(templateNamespace = WokInfantryMod.MOD_ID,
            template = "wok_empty", timeoutTicks = 100)
    public static void jumpDrainPersistsAndCopiesAcrossRespawn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UUID playerId = UUID.nameUUIDFromBytes("stamina-jump-player".getBytes(
                java.nio.charset.StandardCharsets.UTF_8));
        ServerPlayer original = new ServerPlayer(level.getServer(), level,
                new GameProfile(playerId, "stamina-jump-player"));
        original.setGameMode(GameType.SURVIVAL);
        StaminaState.full().save(original);

        StaminaState afterJump = StaminaEvents.consumeJump(original);
        float expectedLegs = StaminaMath.drain(StaminaRules.MAX_STAMINA,
                InfantryServerConfig.legJumpCost());
        helper.assertTrue(Float.compare(afterJump.arms(), StaminaRules.MAX_STAMINA) == 0,
                "跳跃不得消耗手部体力");
        helper.assertTrue(Float.compare(afterJump.legs(), expectedLegs) == 0,
                "跳跃必须按服务端配置消耗腿部体力");
        helper.assertTrue(StaminaState.load(original).equals(afterJump),
                "跳跃后的双池必须立即写入玩家持久化 NBT");

        ServerPlayer replacement = new ServerPlayer(level.getServer(), level,
                new GameProfile(playerId, "stamina-respawn-player"));
        StaminaState.copy(original, replacement);
        helper.assertTrue(StaminaState.load(replacement).equals(afterJump),
                "玩家实体替换时必须保留手部和腿部体力");
        helper.succeed();
    }

    @GameTest(templateNamespace = WokInfantryMod.MOD_ID,
            template = "wok_empty", timeoutTicks = 100)
    public static void sprintDrainUsesServerPositionSamples(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UUID playerId = UUID.nameUUIDFromBytes("stamina-sprint-player".getBytes(
                java.nio.charset.StandardCharsets.UTF_8));
        ServerPlayer player = new ServerPlayer(level.getServer(), level,
                new GameProfile(playerId, "stamina-sprint-player"));
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(0.0D, 1.0D, 0.0D);
        player.setSprinting(true);
        StaminaState.full().save(player);

        StaminaEvents.onPlayerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(
                net.minecraftforge.event.TickEvent.Phase.END, player));
        player.setPos(0.14D, 1.0D, 0.0D);
        player.setSprinting(true);
        StaminaEvents.onPlayerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(
                net.minecraftforge.event.TickEvent.Phase.END, player));

        StaminaState afterSprintTick = StaminaState.load(player);
        float expectedLegs = StaminaMath.drain(StaminaRules.MAX_STAMINA,
                InfantryServerConfig.legSprintDrainPerTick());
        helper.assertTrue(Float.compare(afterSprintTick.arms(), StaminaRules.MAX_STAMINA) == 0,
                "疾跑不得消耗手部体力");
        helper.assertTrue(Float.compare(afterSprintTick.legs(), expectedLegs) == 0,
                "服务端位置采样检测到疾跑后必须消耗腿部体力");
        helper.succeed();
    }
}
