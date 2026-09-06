package com.wok.commandersupport.airstrike;

import com.wok.commandersupport.WokCommanderSupportMod;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.support.SupportDefinition;
import com.wok.infantry.support.SupportTarget;
import com.wok.infantry.support.adapter.ProviderAvailability;
import com.wok.infantry.support.adapter.SupportSpawnContext;
import com.wok.infantry.support.adapter.SupportSpawnException;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

/** Production-mapped Forge acceptance test; only runs under the GameTest launch target. */
@GameTestHolder(WokCommanderSupportMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class JdamRuntimeGameTests {
    private JdamRuntimeGameTests() {
    }

    @GameTest(templateNamespace = "wok_infantry", template = "wok_empty",
            timeoutTicks = 100)
    public static void cbcHeShellDropsVerticallyAndDetonatesAfterTwoSeconds(
            GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos targetBlock = helper.absolutePos(new BlockPos(2, 2, 2));
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                targetBlock.getX(), targetBlock.getZ());
        ArmorStand owner = new ArmorStand(level, targetBlock.getX() + 40.5D,
                surfaceY + 0.25D, targetBlock.getZ() + 0.5D);
        helper.assertTrue(level.addFreshEntity(owner), "测试指挥官实体必须成功生成");
        ArmorStand witness = new ArmorStand(level, targetBlock.getX() + 0.5D,
                surfaceY + 0.25D, targetBlock.getZ() + 0.5D);
        helper.assertTrue(level.addFreshEntity(witness), "目标见证实体必须成功生成");
        float witnessHealth = witness.getHealth();

        MillenniumJdamProvider provider = new MillenniumJdamProvider();
        ProviderAvailability availability = provider.availability();
        helper.assertTrue(availability.available(),
                "CBC JDAM 适配器应可用: " + availability.reason());

        UUID callId = UUID.randomUUID();
        SupportDefinition definition = WokCommanderSupportMod.millenniumJdamDefinition();
        SupportTarget target = SupportTarget.point(level.dimension().location(),
                targetBlock.getX() + 0.5D, targetBlock.getZ() + 0.5D);
        execute(helper, provider, new SupportSpawnContext(level, owner, callId,
                definition, target, 0, Faction.BLUE));

        Entity shell = level.getEntity(callId);
        helper.assertTrue(shell != null, "第 0 tick 必须生成 CBC HE 炮弹");
        helper.assertTrue(shell.getType().builtInRegistryHolder().key().location()
                        .equals(MillenniumJdamProvider.CBC_HE_SHELL_ID),
                "生成实体必须是 createbigcannons:he_shell");
        helper.assertTrue(shell.getDeltaMovement().length() > 10.0D,
                "JDAM 从 200 格高空垂直急坠的速度必须高于 10 格/tick");
        helper.assertTrue(Math.abs(shell.getDeltaMovement().x) < 1.0E-9D
                        && shell.getDeltaMovement().y < 0.0D
                        && Math.abs(shell.getDeltaMovement().z) < 1.0E-9D,
                "JDAM 必须从目标正上方垂直急坠，不能斜向滑翔");

        helper.runAfterDelay(WokCommanderSupportMod.MILLENNIUM_JDAM_FLIGHT_TICKS,
                () -> execute(helper, provider,
                        new SupportSpawnContext(level, owner, callId, definition,
                                target, 1, Faction.BLUE)));
        helper.runAfterDelay(WokCommanderSupportMod.MILLENNIUM_JDAM_FLIGHT_TICKS
                        + WokCommanderSupportMod.MILLENNIUM_JDAM_DELAY_FUSE_TICKS / 2,
                () -> {
                    execute(helper, provider,
                            new SupportSpawnContext(level, owner, callId, definition,
                                    target, 2, Faction.BLUE));
                    Entity grounded = level.getEntity(callId);
                    helper.assertTrue(grounded != null && !grounded.isRemoved(),
                            "CBC HE 炮弹必须在延迟引信中点保持落地状态");
                });
        helper.runAfterDelay(WokCommanderSupportMod.MILLENNIUM_JDAM_FLIGHT_TICKS
                + WokCommanderSupportMod.MILLENNIUM_JDAM_DELAY_FUSE_TICKS,
                () -> execute(helper, provider,
                        new SupportSpawnContext(level, owner, callId, definition,
                                target, 3, Faction.BLUE)));
        helper.runAfterDelay(WokCommanderSupportMod.MILLENNIUM_JDAM_FLIGHT_TICKS
                + WokCommanderSupportMod.MILLENNIUM_JDAM_DELAY_FUSE_TICKS + 4, () -> {
            Entity afterFuse = level.getEntity(callId);
            helper.assertTrue(afterFuse == null || afterFuse.isRemoved(),
                    "JDAM 必须在两秒延迟引信后完成爆炸并移除");
            helper.assertTrue(witness.isRemoved() || witness.getHealth() < witnessHealth,
                    "目标点装甲架必须受到真实 CBC HE 爆炸影响，不能只验证弹体消失");
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = "wok_infantry", template = "wok_empty",
            timeoutTicks = 40)
    public static void alliedArtilleryIndicatorTracksMovingEntityAndStopsOnRelease(
            GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 3, 2));
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                anchor.getX(), anchor.getZ());
        ServerPlayer designator = helper.makeMockServerPlayerInLevel();
        designator.setPos(anchor.getX() + 0.5D, surfaceY + 3.0D,
                anchor.getZ() + 0.5D);
        BattleService battle = BattleService.get(level.getServer()).orElseThrow();
        helper.assertTrue(battle.ensurePlayer(designator).success(),
                "测试制导员必须进入战局记录");
        helper.assertTrue(battle.selectFormation(designator, Faction.BLUE, "default",
                        40, 40).success(),
                "测试制导员必须被分配到呼叫方阵营");

        Item indicator = ForgeRegistries.ITEMS.getValue(
                ArtilleryIndicatorDesignation.ITEM_ID);
        helper.assertTrue(indicator != null, "卓越前线火炮指示器必须已注册");
        designator.setItemInHand(InteractionHand.MAIN_HAND,
                new ItemStack(indicator));
        designator.startUsingItem(InteractionHand.MAIN_HAND);

        ArmorStand movingTarget = new ArmorStand(level, anchor.getX() + 9.5D,
                surfaceY + 3.0D, anchor.getZ() + 0.5D);
        helper.assertTrue(level.addFreshEntity(movingTarget),
                "移动制导目标必须生成");
        lookAt(designator, movingTarget.getBoundingBox().getCenter());
        SupportTarget area = SupportTarget.point(level.dimension().location(),
                movingTarget.getX(), movingTarget.getZ());
        ArtilleryIndicatorDesignation.Designation first =
                ArtilleryIndicatorDesignation.acquire(level, Faction.BLUE, area,
                                WokCommanderSupportMod.F16C_PAVEWAY_GUIDANCE_RADIUS)
                        .orElse(null);
        helper.assertTrue(first != null
                        && first.playerId().equals(designator.getUUID()),
                "同阵营其他玩家持续使用指示器时必须取得制导权");

        movingTarget.setPos(movingTarget.getX() + 4.0D, movingTarget.getY(),
                movingTarget.getZ());
        lookAt(designator, movingTarget.getBoundingBox().getCenter());
        ArtilleryIndicatorDesignation.Designation moved =
                ArtilleryIndicatorDesignation.update(level, designator.getUUID(),
                                Faction.BLUE, area,
                                WokCommanderSupportMod.F16C_PAVEWAY_GUIDANCE_RADIUS)
                        .orElse(null);
        helper.assertTrue(moved != null
                        && moved.position().x > first.position().x + 2.0D,
                "持续照射移动实体时服务端光斑必须随目标移动");

        designator.stopUsingItem();
        helper.assertTrue(ArtilleryIndicatorDesignation.update(level,
                        designator.getUUID(), Faction.BLUE, area,
                        WokCommanderSupportMod.F16C_PAVEWAY_GUIDANCE_RADIUS).isEmpty(),
                "松开指示器后必须立即失去制导");
        helper.succeed();
    }

    private static void lookAt(ServerPlayer player, Vec3 target) {
        Vec3 direction = target.subtract(player.getEyePosition()).normalize();
        player.setYRot((float) Math.toDegrees(Math.atan2(-direction.x,
                direction.z)));
        player.setXRot((float) Math.toDegrees(-Math.asin(direction.y)));
        player.setYHeadRot(player.getYRot());
    }

    private static void execute(GameTestHelper helper,
                                MillenniumJdamProvider provider,
                                SupportSpawnContext context) {
        try {
            provider.executeStep(context);
        } catch (SupportSpawnException failure) {
            helper.fail("JDAM provider execution failed: " + failure.getMessage());
        }
    }
}
