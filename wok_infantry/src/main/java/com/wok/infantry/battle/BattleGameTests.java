package com.wok.infantry.battle;

import com.mojang.authlib.GameProfile;
import com.wok.infantry.WokInfantryMod;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Forge server-object acceptance tests for the core Squad/WARDOGS-style battle loop. */
@GameTestHolder(WokInfantryMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BattleGameTests {
    private BattleGameTests() {
    }

    @GameTest(templateNamespace = WokInfantryMod.MOD_ID,
            template = "wok_empty", timeoutTicks = 200)
    public static void fortyVsFortyCapacityAndExplicitSelection(GameTestHelper helper) {
        BattleService service = isolatedService(helper);
        EnumMap<Faction, Integer> factionCounts = new EnumMap<>(Faction.class);

        for (int index = 0; index < BattleRules.FACTION_CAPACITY * Faction.values().length;
             index++) {
            ServerPlayer player = player(helper, index);
            Faction selectedFaction = index < BattleRules.FACTION_CAPACITY
                    ? Faction.BLUE : Faction.RED;
            ActionResult admission = assignDefaultFormation(service, player, selectedFaction);
            helper.assertTrue(admission.success(), "前80名玩家都应成功选择阵营与编制："
                    + index + " / " + admission.code());
            Faction faction = service.factionOf(player.getUUID()).orElse(null);
            helper.assertTrue(faction != null, "已接纳玩家必须拥有阵营");
            factionCounts.merge(faction, 1, Integer::sum);
        }

        helper.assertTrue(factionCounts.getOrDefault(Faction.BLUE, 0)
                        == BattleRules.FACTION_CAPACITY,
                "蓝方必须恰好40人");
        helper.assertTrue(factionCounts.getOrDefault(Faction.RED, 0)
                        == BattleRules.FACTION_CAPACITY,
                "红方必须恰好40人");

        ServerPlayer overflowPlayer = player(helper, 80);
        helper.assertTrue(service.ensurePlayer(overflowPlayer).success(),
                "满员时仍应允许创建未分配的玩家身份记录");
        ActionResult overflow = service.selectFormation(overflowPlayer, Faction.BLUE,
                "default", BattleRules.FACTION_CAPACITY, BattleRules.FACTION_CAPACITY);
        helper.assertFalse(overflow.success(), "蓝方第41名玩家必须被拒绝");
        helper.assertTrue(overflow.code() == ActionResult.Code.FACTION_FULL,
                "阵营满员拒绝必须返回 FACTION_FULL，而不是其他失败");
        helper.succeed();
    }

    @GameTest(templateNamespace = WokInfantryMod.MOD_ID,
            template = "wok_empty", timeoutTicks = 200)
    public static void squadClassAndFactionMapContract(GameTestHelper helper) {
        BattleService service = isolatedService(helper);
        List<ServerPlayer> bluePlayers = new ArrayList<>();
        List<ServerPlayer> redPlayers = new ArrayList<>();

        // Explicitly select nine BLUE and eight RED records. Nine BLUE actors let us prove the
        // 8+1 squad boundary without relying on the retired automatic assignment fixture.
        for (int index = 0; index < 17; index++) {
            ServerPlayer player = player(helper, 100 + index);
            Faction selectedFaction = index < 9 ? Faction.BLUE : Faction.RED;
            ActionResult admission = assignDefaultFormation(service, player, selectedFaction);
            helper.assertTrue(admission.success(),
                    "测试玩家阵营/编制选择失败：" + admission.code());
            Faction faction = service.factionOf(player.getUUID()).orElseThrow();
            (faction == Faction.BLUE ? bluePlayers : redPlayers).add(player);
        }
        helper.assertTrue(bluePlayers.size() == 9 && redPlayers.size() == 8,
                "测试夹具应显式分配为蓝9红8");

        ServerPlayer leader = bluePlayers.get(0);
        helper.assertTrue(service.createSquad(leader, SquadCallsign.ALPHA).success(),
                "首名蓝方玩家应能创建 Alpha 小队");
        for (int index = 1; index < BattleRules.SQUAD_CAPACITY; index++) {
            ActionResult joined = service.joinSquad(bluePlayers.get(index),
                    SquadCallsign.ALPHA);
            helper.assertTrue(joined.success(), "Alpha 第" + (index + 1)
                    + "名成员应能加入：" + joined.code());
        }
        ActionResult ninthMember = service.joinSquad(bluePlayers.get(8),
                SquadCallsign.ALPHA);
        helper.assertFalse(ninthMember.success(), "Alpha 第9名成员必须被拒绝");
        helper.assertTrue(ninthMember.code() == ActionResult.Code.SQUAD_FULL,
                "第9名成员必须返回 SQUAD_FULL");
        helper.assertTrue(service.squadSize(Faction.BLUE, SquadCallsign.ALPHA)
                        == BattleRules.SQUAD_CAPACITY,
                "Alpha 必须稳定保持8人");

        helper.assertTrue(service.assignClass(bluePlayers.get(1), "support", 2).success(),
                "第一名支援兵应成功占位");
        helper.assertTrue(service.assignClass(bluePlayers.get(2), "support", 2).success(),
                "第二名支援兵应成功占位");
        ActionResult thirdSupport = service.assignClass(bluePlayers.get(3), "support", 2);
        helper.assertFalse(thirdSupport.success(), "第三名支援兵必须被名额拒绝");
        helper.assertTrue(thirdSupport.code() == ActionResult.Code.CLASS_LIMIT_REACHED,
                "支援兵超额必须返回 CLASS_LIMIT_REACHED");
        helper.assertTrue(service.classUsage(Faction.BLUE, SquadCallsign.ALPHA, "support") == 2,
                "Alpha 支援兵占位必须保持2人");

        helper.assertTrue(service.claimCommander(leader).success(),
                "Alpha 队长应能担任蓝方指挥官");
        Vec3 origin = Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
        Set<TacticalMarkerType> requiredTypes = EnumSet.of(
                TacticalMarkerType.INFANTRY,
                TacticalMarkerType.TANK,
                TacticalMarkerType.IFV,
                TacticalMarkerType.ATTACK_DIRECTION);
        int offset = 0;
        for (TacticalMarkerType type : requiredTypes) {
            Vec3 start = origin.add(offset++, 0.0D, 0.0D);
            Vec3 end = type == TacticalMarkerType.ATTACK_DIRECTION
                    ? start.add(32.0D, 0.0D, 0.0D) : start;
            ActionResult marker = service.createMarker(leader, type,
                    leader.serverLevel().dimension(), start, end,
                    BattleRules.DEFAULT_MARKER_TTL_MILLIS);
            helper.assertTrue(marker.success(), "必需标记创建失败："
                    + type + " / " + marker.code());
        }

        BattleSnapshot blueSnapshot = service.snapshotFor(leader, Map.of(
                "assault", 8,
                "support", 2,
                "engineer", 2,
                "recon", 1));
        SquadView alpha = blueSnapshot.squads().stream()
                .filter(squad -> squad.callsign() == SquadCallsign.ALPHA)
                .findFirst().orElseThrow();
        helper.assertTrue(alpha.members().size() == BattleRules.SQUAD_CAPACITY,
                "蓝方快照必须显示 Alpha 全部8名成员");
        helper.assertTrue(blueSnapshot.markers().stream()
                        .map(TacticalMarker::type).collect(java.util.stream.Collectors.toSet())
                        .equals(requiredTypes),
                "蓝方快照必须包含四类必需战术标记");
        helper.assertTrue(blueSnapshot.permissions().canCreateMarkers(),
                "队长/指挥官快照必须开放标记工具");

        BattleSnapshot redSnapshot = service.snapshotFor(redPlayers.get(0));
        helper.assertTrue(redSnapshot.markers().isEmpty(),
                "敌对阵营不得接收蓝方战术标记");
        helper.assertTrue(redSnapshot.squads().stream()
                        .flatMap(squad -> squad.members().stream())
                        .noneMatch(member -> service.factionOf(member.playerId())
                                .filter(Faction.BLUE::equals).isPresent()),
                "红方快照不得包含任何蓝方小队成员");

        UUID blueMarkerId = blueSnapshot.markers().get(0).id();
        ActionResult enemyRemoval = service.removeMarker(redPlayers.get(0), blueMarkerId);
        helper.assertFalse(enemyRemoval.success(), "敌方不得删除蓝方战术标记");
        helper.assertTrue(enemyRemoval.code() == ActionResult.Code.MARKER_NOT_FOUND,
                "跨阵营删除必须与未知标记返回相同结果，不能泄漏标记是否存在");
        helper.assertTrue(service.snapshotFor(leader).markers().size() == requiredTypes.size(),
                "敌方删除请求不得改变蓝方标记");
        helper.succeed();
    }

    @GameTest(templateNamespace = WokInfantryMod.MOD_ID,
            template = "wok_empty", timeoutTicks = 200)
    public static void tacticalMarkerRoleAndRemovalPermissions(GameTestHelper helper) {
        BattleSavedData data = new BattleSavedData();
        BattleService service = isolatedService(helper, data);
        List<ServerPlayer> bluePlayers = new ArrayList<>();
        List<ServerPlayer> redPlayers = new ArrayList<>();

        for (int index = 0; index < 9; index++) {
            ServerPlayer candidate = player(helper, 500 + index);
            Faction selectedFaction = index < 5 ? Faction.BLUE : Faction.RED;
            ActionResult admission = assignDefaultFormation(service, candidate,
                    selectedFaction);
            helper.assertTrue(admission.success(),
                    "标记权限测试玩家阵营/编制选择失败：" + admission.code());
            Faction faction = service.factionOf(candidate.getUUID()).orElseThrow();
            (faction == Faction.BLUE ? bluePlayers : redPlayers).add(candidate);
        }
        helper.assertTrue(bluePlayers.size() == 5 && redPlayers.size() == 4,
                "测试夹具应显式分配为蓝5红4");

        ServerPlayer commanderOnly = bluePlayers.get(0);
        ServerPlayer alphaLeaderOnly = bluePlayers.get(1);
        ServerPlayer alphaMember = bluePlayers.get(2);
        ServerPlayer bravoCreator = bluePlayers.get(3);
        ServerPlayer bravoLeader = bluePlayers.get(4);
        helper.assertTrue(service.createSquad(commanderOnly, SquadCallsign.ALPHA).success(),
                "应能创建蓝方 Alpha");
        helper.assertTrue(service.joinSquad(alphaLeaderOnly, SquadCallsign.ALPHA).success(),
                "未来 Alpha 队长应能先加入小队");
        helper.assertTrue(service.joinSquad(alphaMember, SquadCallsign.ALPHA).success(),
                "蓝方普通成员应能加入 Alpha");
        helper.assertTrue(service.createSquad(bravoCreator, SquadCallsign.BRAVO).success(),
                "应能创建蓝方 Bravo");
        helper.assertTrue(service.joinSquad(bravoLeader, SquadCallsign.BRAVO).success(),
                "未来 Bravo 队长应能先加入小队");

        ServerPlayer redLeader = redPlayers.get(0);
        ServerPlayer redMember = redPlayers.get(1);
        helper.assertTrue(service.createSquad(redLeader, SquadCallsign.ALPHA).success(),
                "应能创建红方 Alpha");
        helper.assertTrue(service.joinSquad(redMember, SquadCallsign.ALPHA).success(),
                "红方普通成员应能加入 Alpha");

        helper.assertTrue(service.claimCommander(commanderOnly).success(),
                "蓝方 Alpha 初始队长应能申请指挥官");
        // A successful online leadership transfer is covered by the production service's
        // PlayerList gate. Synthetic GameTest players are deliberately not registered online,
        // so mutate only the isolated backing state to exercise the downstream role policy.
        data.setLeader(Faction.BLUE, SquadCallsign.ALPHA, alphaLeaderOnly.getUUID());
        data.changed();
        helper.assertTrue(service.isCommander(commanderOnly.getUUID())
                        && !service.isSquadLeader(commanderOnly.getUUID()),
                "测试角色必须是纯指挥官而非兼任队长");
        helper.assertTrue(service.isSquadLeader(alphaLeaderOnly.getUUID())
                        && !service.isCommander(alphaLeaderOnly.getUUID()),
                "测试角色必须是纯小队长而非兼任指挥官");

        BattleSnapshot commanderView = service.snapshotFor(commanderOnly);
        BattleSnapshot leaderView = service.snapshotFor(alphaLeaderOnly);
        BattleSnapshot memberView = service.snapshotFor(alphaMember);
        helper.assertTrue(commanderView.permissions().canCreateMarkers()
                        && commanderView.permissions().canRemoveAnyMarker(),
                "纯指挥官必须能发布并管理本阵营标记");
        helper.assertTrue(leaderView.permissions().canCreateMarkers()
                        && !leaderView.permissions().canRemoveAnyMarker(),
                "纯小队长必须能发布，但不能任意删除其他小队标记");
        helper.assertFalse(memberView.permissions().canCreateMarkers(),
                "普通成员快照不得开放标记工具");

        Vec3 origin = Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
        ActionResult blueMemberCreate = service.createMarker(alphaMember,
                TacticalMarkerType.IFV, alphaMember.serverLevel().dimension(),
                origin, origin, BattleRules.DEFAULT_MARKER_TTL_MILLIS);
        helper.assertFalse(blueMemberCreate.success(), "蓝方普通成员不得发布战术标记");
        helper.assertTrue(blueMemberCreate.code() == ActionResult.Code.NOT_AUTHORIZED,
                "普通成员发布必须返回 NOT_AUTHORIZED");
        ActionResult redMemberCreate = service.createMarker(redMember,
                TacticalMarkerType.IFV, redMember.serverLevel().dimension(),
                origin, origin, BattleRules.DEFAULT_MARKER_TTL_MILLIS);
        helper.assertFalse(redMemberCreate.success(), "红方普通成员同样不得发布战术标记");
        helper.assertTrue(redMemberCreate.code() == ActionResult.Code.NOT_AUTHORIZED,
                "双方普通成员必须受同一权限约束");

        helper.assertTrue(service.createMarker(commanderOnly, TacticalMarkerType.INFANTRY,
                commanderOnly.serverLevel().dimension(), origin, origin,
                BattleRules.DEFAULT_MARKER_TTL_MILLIS).success(),
                "纯指挥官应能发布步兵标记");
        helper.assertTrue(service.createMarker(alphaLeaderOnly, TacticalMarkerType.TANK,
                alphaLeaderOnly.serverLevel().dimension(), origin.add(1.0D, 0.0D, 0.0D),
                origin.add(1.0D, 0.0D, 0.0D),
                BattleRules.DEFAULT_MARKER_TTL_MILLIS).success(),
                "纯小队长应能发布坦克标记");
        helper.assertTrue(service.createMarker(bravoCreator, TacticalMarkerType.IFV,
                bravoCreator.serverLevel().dimension(), origin.add(2.0D, 0.0D, 0.0D),
                origin.add(2.0D, 0.0D, 0.0D),
                BattleRules.DEFAULT_MARKER_TTL_MILLIS).success(),
                "Bravo 初始队长应能发布步战车标记");
        helper.assertTrue(service.createMarker(bravoCreator, TacticalMarkerType.RALLY,
                bravoCreator.serverLevel().dimension(), origin.add(3.0D, 0.0D, 0.0D),
                origin.add(3.0D, 0.0D, 0.0D),
                BattleRules.DEFAULT_MARKER_TTL_MILLIS).success(),
                "Bravo 初始队长应能发布集结点标记");
        helper.assertTrue(service.createMarker(bravoCreator, TacticalMarkerType.DEFEND,
                bravoCreator.serverLevel().dimension(), origin.add(4.0D, 0.0D, 0.0D),
                origin.add(4.0D, 0.0D, 0.0D),
                BattleRules.DEFAULT_MARKER_TTL_MILLIS).success(),
                "Bravo 初始队长应能发布防守标记");
        helper.assertTrue(service.createMarker(redLeader, TacticalMarkerType.DEFEND,
                redLeader.serverLevel().dimension(), origin.add(5.0D, 0.0D, 0.0D),
                origin.add(5.0D, 0.0D, 0.0D),
                BattleRules.DEFAULT_MARKER_TTL_MILLIS).success(),
                "红方小队长应能发布红方标记");

        BattleSnapshot blueSnapshot = service.snapshotFor(alphaMember);
        BattleSnapshot redSnapshot = service.snapshotFor(redMember);
        helper.assertTrue(blueSnapshot.markers().size() == 5,
                "蓝方只能收到5个蓝方标记");
        helper.assertTrue(redSnapshot.markers().size() == 1,
                "红方只能收到自己的1个标记");
        TacticalMarker commanderMarker = blueSnapshot.markers().stream()
                .filter(marker -> marker.creatorId().equals(commanderOnly.getUUID()))
                .findFirst().orElseThrow();
        TacticalMarker alphaLeaderMarker = blueSnapshot.markers().stream()
                .filter(marker -> marker.creatorId().equals(alphaLeaderOnly.getUUID()))
                .findFirst().orElseThrow();
        TacticalMarker bravoIfvMarker = blueSnapshot.markers().stream()
                .filter(marker -> marker.creatorId().equals(bravoCreator.getUUID()))
                .filter(marker -> marker.type() == TacticalMarkerType.IFV)
                .findFirst().orElseThrow();
        TacticalMarker bravoRallyMarker = blueSnapshot.markers().stream()
                .filter(marker -> marker.creatorId().equals(bravoCreator.getUUID()))
                .filter(marker -> marker.type() == TacticalMarkerType.RALLY)
                .findFirst().orElseThrow();
        TacticalMarker bravoDefendMarker = blueSnapshot.markers().stream()
                .filter(marker -> marker.creatorId().equals(bravoCreator.getUUID()))
                .filter(marker -> marker.type() == TacticalMarkerType.DEFEND)
                .findFirst().orElseThrow();
        TacticalMarker redMarker = redSnapshot.markers().get(0);

        // Emulate the successfully transferred role after creation so each deletion authority
        // branch is isolated: creator, current creator-squad leader, commander and unrelated
        // leader are now four distinct actors.
        data.setLeader(Faction.BLUE, SquadCallsign.BRAVO, bravoLeader.getUUID());
        data.changed();
        helper.assertTrue(!service.isSquadLeader(bravoCreator.getUUID())
                        && service.isSquadLeader(bravoLeader.getUUID()),
                "Bravo 标记创建者必须已降为普通成员，接任者必须是当前队长");
        for (ServerPlayer actor : List.of(commanderOnly, alphaLeaderOnly, alphaMember,
                bravoCreator, bravoLeader, redLeader)) {
            helper.assertFalse(actor.hasPermissions(BattleRules.ADMIN_PERMISSION_LEVEL),
                    "权限矩阵参与者不得意外拥有管理员旁路");
        }

        ActionResult unrelatedLeaderRemoval = service.removeMarker(alphaLeaderOnly,
                bravoIfvMarker.id());
        helper.assertFalse(unrelatedLeaderRemoval.success(),
                "无关小队长不得删除 Bravo 标记");
        helper.assertTrue(unrelatedLeaderRemoval.code() == ActionResult.Code.NOT_AUTHORIZED,
                "无关小队长删除必须返回 NOT_AUTHORIZED");
        ActionResult ordinaryMemberRemoval = service.removeMarker(alphaMember,
                bravoIfvMarker.id());
        helper.assertFalse(ordinaryMemberRemoval.success(), "普通成员不得删除标记");
        helper.assertTrue(ordinaryMemberRemoval.code() == ActionResult.Code.NOT_AUTHORIZED,
                "普通成员删除必须返回 NOT_AUTHORIZED");
        ActionResult enemyRemoval = service.removeMarker(redLeader, bravoIfvMarker.id());
        helper.assertFalse(enemyRemoval.success(), "敌方不得删除蓝方标记");
        helper.assertTrue(enemyRemoval.code() == ActionResult.Code.MARKER_NOT_FOUND,
                "敌方删除不得泄漏蓝方标记是否存在");
        helper.assertTrue(data.marker(bravoIfvMarker.id()) != null,
                "失败的删除请求不得移除目标标记");
        helper.assertTrue(service.snapshotFor(alphaMember).markers().size() == 5,
                "所有未授权删除请求都不得修改蓝方标记");

        helper.assertTrue(service.removeMarker(commanderOnly, bravoIfvMarker.id()).success(),
                "纯指挥官应能删除其他蓝方小队的标记");
        helper.assertTrue(data.marker(bravoIfvMarker.id()) == null,
                "指挥官成功删除后目标标记必须消失");
        helper.assertTrue(service.removeMarker(bravoLeader, bravoRallyMarker.id()).success(),
                "创建者所属小队的当前队长应能删除标记");
        helper.assertTrue(data.marker(bravoRallyMarker.id()) == null,
                "当前所属小队长成功删除后目标标记必须消失");
        helper.assertTrue(service.removeMarker(bravoCreator, bravoDefendMarker.id()).success(),
                "已不再担任队长的创建者仍应能删除自己的标记");
        helper.assertTrue(data.marker(bravoDefendMarker.id()) == null,
                "创建者成功删除后目标标记必须消失");
        helper.assertTrue(service.removeMarker(alphaLeaderOnly, commanderMarker.id()).success(),
                "创建者所属小队的当前队长应能删除该标记");
        helper.assertTrue(data.marker(commanderMarker.id()) == null,
                "Alpha 当前队长成功删除后指挥官标记必须消失");
        helper.assertTrue(service.removeMarker(alphaLeaderOnly, alphaLeaderMarker.id()).success(),
                "标记创建者应能删除自己的标记");
        helper.assertTrue(data.marker(alphaLeaderMarker.id()) == null,
                "Alpha 创建者成功删除后自己的标记必须消失");
        helper.assertTrue(service.snapshotFor(alphaMember).markers().isEmpty(),
                "合法删除后蓝方标记应全部清空");
        helper.assertTrue(service.removeMarker(redLeader, redMarker.id()).success(),
                "红方创建者应能删除自己的标记");
        helper.assertTrue(service.snapshotFor(redMember).markers().isEmpty(),
                "红方合法删除后标记应清空");
        helper.succeed();
    }

    @GameTest(templateNamespace = WokInfantryMod.MOD_ID,
            template = "wok_empty", timeoutTicks = 200)
    public static void squadLifecycleReleasesAuthorityAndClassSlots(GameTestHelper helper) {
        BattleService service = isolatedService(helper);
        List<ServerPlayer> bluePlayers = new ArrayList<>();

        for (int index = 0; index < 5; index++) {
            ServerPlayer candidate = player(helper, 300 + index);
            ActionResult admission = assignDefaultFormation(service, candidate, Faction.BLUE);
            helper.assertTrue(admission.success(),
                    "生命周期测试玩家阵营/编制选择失败：" + admission.code());
            bluePlayers.add(candidate);
        }
        helper.assertTrue(bluePlayers.size() == 5, "测试夹具应产生5名蓝方成员");

        ServerPlayer alphaLeader = bluePlayers.get(0);
        List<ServerPlayer> alphaMembers = bluePlayers.subList(1, 4);
        ServerPlayer bravoLeader = bluePlayers.get(4);
        helper.assertTrue(service.createSquad(alphaLeader, SquadCallsign.ALPHA).success(),
                "应能创建 Alpha 小队");
        for (ServerPlayer member : alphaMembers) {
            helper.assertTrue(service.joinSquad(member, SquadCallsign.ALPHA).success(),
                    "Alpha 成员加入失败");
            helper.assertTrue(service.assignClass(member, "support", 8).success(),
                    "生命周期测试需要先占用支援兵名额");
        }
        helper.assertTrue(service.assignClass(alphaLeader, "engineer", 8).success(),
                "原队长必须先占用非默认兵种，才能验证离队清理");
        helper.assertTrue(service.createSquad(bravoLeader, SquadCallsign.BRAVO).success(),
                "应能创建 Bravo 小队");

        ActionResult unauthorizedKick = service.kickMember(alphaMembers.get(0),
                alphaMembers.get(1).getUUID());
        helper.assertFalse(unauthorizedKick.success(), "普通队员不得踢出同队成员");
        helper.assertTrue(unauthorizedKick.code() == ActionResult.Code.NOT_AUTHORIZED,
                "非队长踢出必须返回 NOT_AUTHORIZED");

        helper.assertTrue(service.claimCommander(alphaLeader).success(),
                "Alpha 队长应能申请蓝方指挥官");
        ActionResult duplicateCommander = service.claimCommander(bravoLeader);
        helper.assertFalse(duplicateCommander.success(), "同阵营不得同时存在两名指挥官");
        helper.assertTrue(duplicateCommander.code() == ActionResult.Code.COMMANDER_EXISTS,
                "第二名指挥官申请必须返回 COMMANDER_EXISTS");

        helper.assertTrue(service.leaveSquad(alphaLeader).success(),
                "队长应能主动退出 Alpha");
        helper.assertTrue(service.squadOf(alphaLeader.getUUID()).isEmpty(),
                "退出后原队长不得仍属于 Alpha");
        helper.assertTrue(BattleRules.DEFAULT_CLASS_ID.equals(
                        service.assignedClass(alphaLeader.getUUID())),
                "退出小队必须重置为默认突击兵");
        helper.assertFalse(service.isCommander(alphaLeader.getUUID()),
                "退出小队必须同时释放指挥官身份");
        helper.assertTrue(service.squadSize(Faction.BLUE, SquadCallsign.ALPHA) == 3,
                "队长退出后 Alpha 应保留3名成员");

        UUID successorId = service.squadLeader(Faction.BLUE, SquadCallsign.ALPHA)
                .orElseThrow();
        helper.assertFalse(successorId.equals(alphaLeader.getUUID()),
                "离队的原队长不得继续担任队长");
        ServerPlayer successor = alphaMembers.stream()
                .filter(member -> member.getUUID().equals(successorId))
                .findFirst().orElseThrow();
        ServerPlayer kickTarget = alphaMembers.stream()
                .filter(member -> !member.getUUID().equals(successorId))
                .findFirst().orElseThrow();
        helper.assertTrue(service.kickMember(successor, kickTarget.getUUID()).success(),
                "继任队长应能踢出同队成员");
        helper.assertTrue(service.squadOf(kickTarget.getUUID()).isEmpty(),
                "被踢成员必须脱离小队");
        helper.assertTrue(BattleRules.DEFAULT_CLASS_ID.equals(
                        service.assignedClass(kickTarget.getUUID())),
                "被踢成员必须释放兵种占位并回到突击兵");
        ActionResult immediateRejoin = service.joinSquad(kickTarget, SquadCallsign.ALPHA);
        helper.assertFalse(immediateRejoin.success(),
                "被踢成员不得立即重新加入原小队");
        helper.assertTrue(immediateRejoin.code()
                        == ActionResult.Code.SQUAD_REJOIN_COOLDOWN,
                "立即重加必须返回专用踢出冷却错误，而不是依赖客户端按钮状态");
        helper.assertTrue(service.joinSquad(kickTarget, SquadCallsign.BRAVO).success(),
                "踢出冷却只能限制原小队，不得阻止成员加入其他小队");
        helper.assertTrue(service.leaveSquad(kickTarget).success(),
                "加入其他小队后应能正常退出并继续生命周期测试");
        helper.assertTrue(service.classUsage(Faction.BLUE, SquadCallsign.ALPHA,
                        "support") == 2,
                "踢出一名支援兵后 Alpha 只能剩2个支援兵占位");

        ServerPlayer remainingMember = alphaMembers.stream()
                .filter(member -> !member.getUUID().equals(successorId))
                .filter(member -> !member.getUUID().equals(kickTarget.getUUID()))
                .findFirst().orElseThrow();
        ActionResult unauthorizedDisband = service.disbandSquad(remainingMember);
        helper.assertFalse(unauthorizedDisband.success(), "普通队员不得解散小队");
        helper.assertTrue(unauthorizedDisband.code() == ActionResult.Code.NOT_AUTHORIZED,
                "非队长解散必须返回 NOT_AUTHORIZED");

        helper.assertTrue(service.claimCommander(bravoLeader).success(),
                "原指挥官离队后 Bravo 队长应能接任");
        helper.assertTrue(service.resignCommander(bravoLeader).success(),
                "现任指挥官应能主动卸任");
        helper.assertTrue(service.claimCommander(successor).success(),
                "Alpha 继任队长应能在解散前担任指挥官");
        helper.assertTrue(service.disbandSquad(successor).success(),
                "继任队长应能解散 Alpha");
        helper.assertFalse(service.isCommander(successor.getUUID()),
                "解散小队必须清除其中现任指挥官");
        helper.assertTrue(service.squadSize(Faction.BLUE, SquadCallsign.ALPHA) == 0,
                "解散后 Alpha 必须为空");
        helper.assertTrue(service.squadLeader(Faction.BLUE, SquadCallsign.ALPHA).isEmpty(),
                "解散后 Alpha 不得保留队长引用");
        helper.assertTrue(service.classUsage(Faction.BLUE, SquadCallsign.ALPHA,
                        "support") == 0,
                "解散后 Alpha 的兵种占位必须全部释放");
        helper.assertTrue(service.claimCommander(bravoLeader).success(),
                "Alpha 解散后释放的指挥官席位必须可由 Bravo 重新占用");
        helper.assertTrue(service.resignCommander(bravoLeader).success(),
                "最终应能干净释放蓝方指挥官席位");
        for (int index = 0; index < 4; index++) {
            ServerPlayer formerAlpha = bluePlayers.get(index);
            helper.assertTrue(service.squadOf(formerAlpha.getUUID()).isEmpty(),
                    "解散后所有 Alpha 成员都必须脱队");
            helper.assertTrue(BattleRules.DEFAULT_CLASS_ID.equals(
                            service.assignedClass(formerAlpha.getUUID())),
                    "解散后所有 Alpha 成员都必须回到突击兵");
        }

        ServerPlayer recreatedLeader = successor;
        helper.assertTrue(service.createSquad(recreatedLeader, SquadCallsign.ALPHA).success(),
                "解散后 Alpha 呼号必须可重新创建");
        helper.assertTrue(service.assignClass(recreatedLeader, "support", 1).success(),
                "释放后的支援兵名额必须可再次占用");
        helper.succeed();
    }

    private static BattleService isolatedService(GameTestHelper helper) {
        return isolatedService(helper, new BattleSavedData());
    }

    private static BattleService isolatedService(GameTestHelper helper, BattleSavedData data) {
        MinecraftServer server = helper.getLevel().getServer();
        return new BattleService(server, data);
    }

    private static ActionResult assignDefaultFormation(BattleService service,
                                                       ServerPlayer player,
                                                       Faction faction) {
        ActionResult admission = service.ensurePlayer(player);
        if (!admission.success()) {
            return admission;
        }
        return service.selectFormation(player, faction, "default",
                BattleRules.FACTION_CAPACITY, BattleRules.FACTION_CAPACITY);
    }

    private static ServerPlayer player(GameTestHelper helper, int index) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        return new ServerPlayer(server, level, new GameProfile(
                UUID.nameUUIDFromBytes(("wok-infantry-gametest-" + index)
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                "wok-test-" + index));
    }
}
