package com.wok.infantry.battle;

import com.mojang.authlib.GameProfile;
import com.wok.infantry.WokInfantryMod;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Server-object acceptance coverage for formation-scoped squad identity. */
@GameTestHolder(WokInfantryMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BattleFormationGameTests {
    private BattleFormationGameTests() {
    }

    @GameTest(templateNamespace = WokInfantryMod.MOD_ID,
            template = "wok_empty", timeoutTicks = 200)
    public static void sameCallsignSquadsCannotCrossFormationBoundaries(GameTestHelper helper) {
        BattleSavedData data = new BattleSavedData();
        BattleService service = new BattleService(helper.getLevel().getServer(), data,
                (faction, formationId, callsign) -> faction == Faction.BLUE
                        && callsign == SquadCallsign.ALPHA
                        && ("academy".equals(formationId) || "guards".equals(formationId))
                        ? BattleRules.SQUAD_CAPACITY : 0);
        ServerPlayer academyLeader = player(helper, "academy-leader");
        ServerPlayer academyMember = player(helper, "academy-member");
        ServerPlayer guardsLeader = player(helper, "guards-leader");
        ServerPlayer guardsMember = player(helper, "guards-member");
        enroll(data, academyLeader, "academy");
        enroll(data, academyMember, "academy");
        enroll(data, guardsLeader, "guards");
        enroll(data, guardsMember, "guards");

        helper.assertTrue(service.createSquad(academyLeader, SquadCallsign.ALPHA).success(),
                "学院编制应能创建 Alpha");
        helper.assertTrue(service.joinSquad(academyMember, SquadCallsign.ALPHA).success(),
                "学院编制成员应能加入自己的 Alpha");
        helper.assertTrue(service.createSquad(guardsLeader, SquadCallsign.ALPHA).success(),
                "近卫编制应能独立创建同名 Alpha");
        helper.assertTrue(service.joinSquad(guardsMember, SquadCallsign.ALPHA).success(),
                "近卫编制成员应能加入自己的 Alpha");
        helper.assertTrue(service.squadSize(Faction.BLUE, "academy", SquadCallsign.ALPHA) == 2,
                "学院 Alpha 必须独立计数");
        helper.assertTrue(service.squadSize(Faction.BLUE, "guards", SquadCallsign.ALPHA) == 2,
                "近卫 Alpha 必须独立计数");
        helper.assertTrue(service.squadSize(Faction.BLUE, SquadCallsign.ALPHA) == 0,
                "含歧义的旧二元查询必须失败关闭");

        helper.assertTrue(service.assignClass(academyMember, "support", 1).success(),
                "学院 Alpha 应能占用自己的支援兵名额");
        helper.assertTrue(service.assignClass(guardsMember, "support", 1).success(),
                "近卫 Alpha 不得被学院 Alpha 的兵种占位阻塞");
        helper.assertTrue(service.classUsage(Faction.BLUE, "academy", SquadCallsign.ALPHA,
                "support") == 1, "学院兵种配额必须独立");
        helper.assertTrue(service.classUsage(Faction.BLUE, "guards", SquadCallsign.ALPHA,
                "support") == 1, "近卫兵种配额必须独立");

        ActionResult crossTransfer = service.transferLeadership(academyLeader,
                guardsMember.getUUID());
        helper.assertFalse(crossTransfer.success(), "不得跨编制移交同呼号小队长");
        helper.assertTrue(crossTransfer.code() == ActionResult.Code.INVALID_TARGET,
                "跨编制移交必须返回 INVALID_TARGET");
        ActionResult crossKick = service.kickMember(academyLeader, guardsMember.getUUID());
        helper.assertFalse(crossKick.success(), "不得跨编制踢出同呼号成员");
        helper.assertTrue(crossKick.code() == ActionResult.Code.INVALID_TARGET,
                "跨编制踢出必须返回 INVALID_TARGET");

        BattleSnapshot academySnapshot = service.snapshotFor(academyLeader);
        Set<UUID> visibleAlpha = academySnapshot.squads().stream()
                .filter(squad -> squad.callsign() == SquadCallsign.ALPHA)
                .flatMap(squad -> squad.members().stream())
                .map(MemberView::playerId)
                .collect(Collectors.toSet());
        helper.assertTrue(visibleAlpha.equals(Set.of(academyLeader.getUUID(),
                        academyMember.getUUID())),
                "学院快照不得混入近卫同呼号成员");

        helper.assertTrue(service.kickMember(academyLeader, academyMember.getUUID()).success(),
                "学院队长应能踢出本编制成员");
        data.player(academyMember.getUUID()).formationId = "guards";
        helper.assertTrue(service.joinSquad(academyMember, SquadCallsign.ALPHA).success(),
                "学院 Alpha 的踢出冷却不得污染近卫 Alpha");
        helper.assertTrue(service.disbandSquad(academyLeader).success(),
                "学院队长应能解散自己的 Alpha");
        helper.assertTrue(service.squadSize(Faction.BLUE, "academy", SquadCallsign.ALPHA) == 0,
                "解散学院 Alpha 后本编制必须为空");
        helper.assertTrue(service.squadSize(Faction.BLUE, "guards", SquadCallsign.ALPHA) == 3,
                "解散学院 Alpha 不得影响近卫 Alpha");
        helper.assertTrue(service.squadLeader(Faction.BLUE, "guards", SquadCallsign.ALPHA)
                        .orElseThrow().equals(guardsLeader.getUUID()),
                "近卫 Alpha 队长不得被学院解散操作清除");
        helper.succeed();
    }

    @GameTest(templateNamespace = WokInfantryMod.MOD_ID,
            template = "wok_empty", timeoutTicks = 200)
    public static void sharedFormationResultAppliesToWholeFactionAtomically(
            GameTestHelper helper) {
        BattleSavedData data = new BattleSavedData();
        BattleService service = new BattleService(helper.getLevel().getServer(), data,
                (faction, formationId, callsign) -> faction == Faction.BLUE
                        && callsign == SquadCallsign.ALPHA ? BattleRules.SQUAD_CAPACITY : 0);
        ServerPlayer first = player(helper, "shared-first");
        ServerPlayer second = player(helper, "shared-second");
        ServerPlayer pending = player(helper, "shared-pending");
        ServerPlayer enemy = player(helper, "shared-enemy");
        enroll(data, first, "old_formation");
        enroll(data, second, "old_formation");
        long now = Math.max(1L, System.currentTimeMillis());
        BattleSavedData.StoredPlayer pendingRecord = data.addPlayer(pending.getUUID(),
                pending.getGameProfile().getName(), now);
        pendingRecord.faction = Faction.BLUE;
        pendingRecord.formationId = null;
        BattleSavedData.StoredPlayer enemyRecord = data.addPlayer(enemy.getUUID(),
                enemy.getGameProfile().getName(), now);
        enemyRecord.faction = Faction.RED;
        enemyRecord.formationId = "enemy_formation";

        helper.assertTrue(service.createSquad(first, SquadCallsign.ALPHA).success(),
                "旧编制应能创建小队");
        helper.assertTrue(service.joinSquad(second, SquadCallsign.ALPHA).success(),
                "旧编制成员应能加入小队");

        ActionResult tooSmall = service.applySharedFormation(Faction.BLUE,
                "winning_formation", 2);
        helper.assertFalse(tooSmall.success(), "容量不足时共享结果必须整体失败");
        helper.assertTrue("old_formation".equals(data.player(first.getUUID()).formationId),
                "失败预检不得部分修改现有编制");
        helper.assertTrue(data.player(pending.getUUID()).formationId == null,
                "失败预检不得修改待投票玩家");

        ActionResult applied = service.applySharedFormation(Faction.BLUE,
                "winning_formation", 3);
        helper.assertTrue(applied.success(), "容量足够时应应用阵营共享结果");
        helper.assertTrue(data.players().stream()
                        .filter(record -> record.faction == Faction.BLUE)
                        .allMatch(record -> "winning_formation".equals(record.formationId)),
                "所有蓝方成员必须得到同一胜出编制");
        helper.assertTrue(data.players().stream()
                        .filter(record -> record.faction == Faction.BLUE)
                        .allMatch(record -> record.squad == null),
                "改变编制时旧小队身份必须清除");
        helper.assertTrue("enemy_formation".equals(data.player(enemy.getUUID()).formationId),
                "蓝方共享结果不得影响红方");
        helper.succeed();
    }

    /** vote-01: a player who joins after the lock gets the locked formation in the same step. */
    @GameTest(templateNamespace = WokInfantryMod.MOD_ID,
            template = "wok_empty", timeoutTicks = 200)
    public static void lateJoinAfterLockInheritsTheSharedFormation(GameTestHelper helper) {
        BattleSavedData data = new BattleSavedData();
        BattleService service = new BattleService(helper.getLevel().getServer(), data,
                (faction, formationId, callsign) -> BattleRules.SQUAD_CAPACITY);
        ServerPlayer first = player(helper, "late-first");
        ServerPlayer second = player(helper, "late-second");
        ServerPlayer late = player(helper, "late-joiner");
        ServerPlayer enemy = player(helper, "late-enemy");
        enroll(data, first, "winning_formation");
        enroll(data, second, "winning_formation");
        long now = Math.max(1L, System.currentTimeMillis());
        data.addPlayer(enemy.getUUID(), enemy.getGameProfile().getName(), now).faction =
                Faction.RED;

        ActionResult joined = service.joinFactionWithSharedFormation(late, Faction.BLUE,
                BattleRules.FACTION_CAPACITY, "winning_formation", 40);

        helper.assertTrue(joined.success(), "锁定后加入应直接成功：" + joined.message());
        BattleSavedData.StoredPlayer record = data.player(late.getUUID());
        helper.assertTrue(record.faction == Faction.BLUE, "晚加入者必须进入蓝方");
        helper.assertTrue("winning_formation".equals(record.formationId),
                "晚加入者必须直接拿到锁定编制，不能停在“有阵营无编制”");
        helper.assertTrue(record.assignedClassId != null && !record.assignedClassId.isBlank(),
                "晚加入者必须拿到默认兵种");
        helper.assertTrue(service.factionMemberIds(Faction.BLUE).equals(Set.of(
                        first.getUUID(), second.getUUID(), late.getUUID())),
                "计票成员集合只含本阵营成员");
        helper.assertTrue(service.joinFactionWithSharedFormation(late, Faction.BLUE, 40,
                "winning_formation", 40).success(), "重复请求应幂等成功");
        ActionResult otherSide = service.joinFactionWithSharedFormation(enemy, Faction.BLUE,
                40, "winning_formation", 40);
        helper.assertFalse(otherSide.success(), "已在对方阵营的玩家不能换阵营");
        helper.assertTrue(otherSide.code() == ActionResult.Code.FORMATION_LOCKED,
                "换阵营必须返回 FORMATION_LOCKED");
        helper.assertTrue(data.player(enemy.getUUID()).faction == Faction.RED,
                "被拒绝的请求不得修改对方阵营玩家");
        helper.succeed();
    }

    /** The locked formation's capacity bounds the faction; a join beyond it changes nothing. */
    @GameTest(templateNamespace = WokInfantryMod.MOD_ID,
            template = "wok_empty", timeoutTicks = 200)
    public static void lateJoinBeyondTheLockedCapacityIsRejectedWhole(GameTestHelper helper) {
        BattleSavedData data = new BattleSavedData();
        BattleService service = new BattleService(helper.getLevel().getServer(), data,
                (faction, formationId, callsign) -> BattleRules.SQUAD_CAPACITY);
        ServerPlayer first = player(helper, "full-first");
        ServerPlayer second = player(helper, "full-second");
        ServerPlayer late = player(helper, "full-late");
        enroll(data, first, "small_formation");
        enroll(data, second, "small_formation");

        ActionResult rejected = service.joinFactionWithSharedFormation(late, Faction.BLUE,
                BattleRules.FACTION_CAPACITY, "small_formation", 2);

        helper.assertFalse(rejected.success(), "超出锁定编制容量必须拒绝");
        helper.assertTrue(rejected.code() == ActionResult.Code.FACTION_FULL,
                "超容量必须返回 FACTION_FULL");
        BattleSavedData.StoredPlayer record = data.player(late.getUUID());
        helper.assertTrue(record == null || record.faction == null && record.formationId == null,
                "被拒绝的加入不得留下阵营或编制");
        helper.assertTrue(service.factionSize(Faction.BLUE) == 2, "阵营人数保持不变");
        helper.succeed();
    }

    /**
     * Login after the lock: a member who is in the faction without a formation (joined before
     * the lock and was offline, or reconciled) receives the locked formation; a member with
     * another formation is never silently moved.
     */
    @GameTest(templateNamespace = WokInfantryMod.MOD_ID,
            template = "wok_empty", timeoutTicks = 200)
    public static void loginAfterLockInheritsTheSharedFormation(GameTestHelper helper) {
        BattleSavedData data = new BattleSavedData();
        BattleService service = new BattleService(helper.getLevel().getServer(), data,
                (faction, formationId, callsign) -> BattleRules.SQUAD_CAPACITY);
        ServerPlayer member = player(helper, "login-member");
        ServerPlayer pending = player(helper, "login-pending");
        ServerPlayer other = player(helper, "login-other");
        enroll(data, member, "locked_formation");
        enroll(data, other, "older_formation");
        long now = Math.max(1L, System.currentTimeMillis());
        BattleSavedData.StoredPlayer pendingRecord = data.addPlayer(pending.getUUID(),
                pending.getGameProfile().getName(), now);
        pendingRecord.faction = Faction.BLUE;
        pendingRecord.formationId = null;

        ActionResult inherited = service.joinFactionWithSharedFormation(pending, Faction.BLUE,
                BattleRules.FACTION_CAPACITY, "locked_formation", 40);

        helper.assertTrue(inherited.success(), "登录补继承应成功：" + inherited.message());
        helper.assertTrue("locked_formation".equals(data.player(pending.getUUID()).formationId),
                "有阵营无编制的玩家登录后必须拿到锁定编制");
        helper.assertTrue(service.factionSize(Faction.BLUE) == 3,
                "补继承不得重复占用阵营名额");
        ActionResult moved = service.joinFactionWithSharedFormation(other, Faction.BLUE,
                BattleRules.FACTION_CAPACITY, "locked_formation", 40);
        helper.assertFalse(moved.success(), "已有其他编制的成员不得被补继承改动");
        helper.assertTrue("older_formation".equals(data.player(other.getUUID()).formationId),
                "被拒绝的补继承不得修改原编制");
        helper.succeed();
    }

    private static void enroll(BattleSavedData data, ServerPlayer player, String formationId) {
        long now = Math.max(1L, System.currentTimeMillis());
        BattleSavedData.StoredPlayer stored = data.addPlayer(player.getUUID(),
                player.getGameProfile().getName(), now);
        stored.faction = Faction.BLUE;
        stored.formationId = formationId;
    }

    private static ServerPlayer player(GameTestHelper helper, String seed) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        UUID playerId = UUID.nameUUIDFromBytes(
                ("wok-infantry-formation-" + seed).getBytes(StandardCharsets.UTF_8));
        return new ServerPlayer(server, level, new GameProfile(playerId, seed));
    }
}
