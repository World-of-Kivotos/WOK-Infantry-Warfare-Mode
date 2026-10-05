package com.wok.infantry.client.screen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.battle.ClassLimitView;
import com.wok.infantry.battle.ClassQuotaView;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.FormationContextView;
import com.wok.infantry.battle.KickCooldownView;
import com.wok.infantry.battle.MemberState;
import com.wok.infantry.battle.MemberView;
import com.wok.infantry.battle.PermissionView;
import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.battle.SquadView;
import com.wok.infantry.client.screen.SquadBoardModel.Action;
import com.wok.infantry.client.screen.SquadBoardModel.ActionState;
import com.wok.infantry.client.screen.SquadBoardModel.CheckItem;
import com.wok.infantry.client.screen.SquadBoardModel.CheckState;
import com.wok.infantry.client.screen.SquadBoardModel.Page;
import com.wok.infantry.client.screen.SquadBoardModel.ReasonCode;
import com.wok.infantry.client.screen.SquadBoardModel.Role;
import com.wok.infantry.client.screen.SquadBoardModel.SquadRow;
import com.wok.infantry.client.screen.SquadBoardModel.SquadStatus;
import com.wok.infantry.client.screen.SquadBoardModel.Stage;
import com.wok.infantry.deployment.DeploymentPhase;
import com.wok.infantry.deployment.DeploymentPoint;
import com.wok.infantry.deployment.DeploymentPointKind;
import com.wok.infantry.deployment.DeploymentView;
import com.wok.infantry.formation.vote.FormationVotePhase;
import com.wok.infantry.network.battle.BattleNetworkLimits;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SquadBoardModelTest {
    private static final UUID VIEWER = new UUID(0L, 1L);
    private static final long NOW = 100_000L;
    private static final ResourceLocation OVERWORLD =
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
    private static final FormationContextView CONTEXT = new FormationContextView(
            "millennium_seminar_mobile", "千禧年研讨会机动部队", "assault", "academy", "学院军",
            40, "kaiser", "凯撒", 40);
    private static final List<ClassQuotaView> QUOTAS = List.of(
            new ClassQuotaView("support", "支援兵", 1, 1),
            new ClassQuotaView("assault", "突击兵", 6, 2),
            new ClassQuotaView("medic", "医疗兵", 2, 2),
            new ClassQuotaView("sniper", "狙击手", 0, 0));
    private static final PermissionView LEADER = new PermissionView(false, false, true, true,
            true, false, true);
    private static final PermissionView MEMBER = new PermissionView(false, false, true, false,
            false, false, false);
    private static final PermissionView UNASSIGNED = new PermissionView(true, true, false,
            false, false, false, false);
    private static final PermissionView NOTHING = new PermissionView(false, false, false,
            false, false, false, false);

    // ------------------------------------------------------------------------- squads

    @Test
    void formationWithOnlyAlphaAndBravoClosesTheOtherCallSigns() {
        BattleSnapshot snapshot = unassignedSnapshot(List.of(
                squad(SquadCallsign.ALPHA, 8, 10, 3), squad(SquadCallsign.BRAVO, 4, 20, 0)));
        SquadBoardModel model = SquadBoardModel.of(snapshot, SquadCallsign.CHARLIE, null, NOW);

        assertEquals(Stage.READY, model.stage());
        assertEquals(List.of(SquadStatus.JOINABLE, SquadStatus.CREATABLE,
                        SquadStatus.NOT_CONFIGURED, SquadStatus.NOT_CONFIGURED,
                        SquadStatus.NOT_CONFIGURED),
                model.squads().stream().map(SquadRow::status).toList());
        SquadRow charlie = model.viewedRow();
        assertFalse(charlie.configured());
        assertEquals(KEY("count.none"), key(charlie.count()));
        assertEquals(KEY("sub.closed"), key(charlie.subCandidates().get(0)));
        // no create entry for a call sign the formation does not open (squad-02)
        assertTrue(model.rosterActions().buttons().isEmpty());
        assertEquals(ReasonCode.NOT_CONFIGURED, model.rosterActions().reason().code());
        assertNull(model.action(Action.CREATE_SQUAD));
    }

    @Test
    void countsUseEachSquadsOwnCapacity() {
        BattleSnapshot snapshot = unassignedSnapshot(List.of(
                squad(SquadCallsign.ALPHA, 8, 10, 3), squad(SquadCallsign.BRAVO, 4, 20, 4)));
        SquadBoardModel model = SquadBoardModel.of(snapshot, SquadCallsign.BRAVO, null, NOW);

        assertEquals(List.of(3, 8), args(model.squad(SquadCallsign.ALPHA).count()));
        assertEquals(List.of(4, 4), args(model.squad(SquadCallsign.BRAVO).count()),
                "the denominator is the squad's own 4, never the formation maximum 8");
        assertEquals(SquadStatus.FULL, model.squad(SquadCallsign.BRAVO).status());
        ActionState join = model.rosterActions().button(Action.JOIN_SQUAD);
        assertFalse(join.enabled());
        assertEquals(ReasonCode.SQUAD_FULL, join.reason().code());
        assertEquals(KEY("sub.leader"), key(model.squad(SquadCallsign.ALPHA)
                .subCandidates().get(0)));
    }

    @Test
    void viewingAnotherSquadNeverOffersLeaveOrDisband() {
        BattleSnapshot snapshot = leaderSnapshot(defaultDeployment(), List.of());
        SquadBoardModel model = SquadBoardModel.of(snapshot, SquadCallsign.BRAVO, null, NOW);

        SquadBoardModel.RosterActions actions = model.rosterActions();
        assertFalse(actions.ownSquad());
        assertEquals(List.of(Action.JOIN_SQUAD, Action.RETURN_TO_OWN_SQUAD),
                actions.buttons().stream().map(ActionState::action).toList());
        ActionState join = actions.button(Action.JOIN_SQUAD);
        assertFalse(join.enabled());
        assertEquals(ReasonCode.IN_SQUAD_JOIN, join.reason().code());
        assertTrue(actions.button(Action.RETURN_TO_OWN_SQUAD).enabled());
        assertEquals(SquadCallsign.ALPHA,
                actions.button(Action.RETURN_TO_OWN_SQUAD).squad());
        assertNull(actions.button(Action.LEAVE_SQUAD));
        assertNull(actions.button(Action.DISBAND_SQUAD));

        SquadBoardModel empty = SquadBoardModel.of(snapshot, SquadCallsign.CHARLIE, null, NOW);
        ActionState create = empty.rosterActions().button(Action.CREATE_SQUAD);
        assertFalse(create.enabled());
        assertEquals(ReasonCode.IN_SQUAD_CREATE, create.reason().code());
        assertEquals(SquadStatus.NOT_CREATED, empty.squad(SquadCallsign.CHARLIE).status());
    }

    @Test
    void unassignedPlayerJoinsWithTheDefaultClassHintAndCreatesAsLeader() {
        BattleSnapshot snapshot = unassignedSnapshot(List.of(
                squad(SquadCallsign.ALPHA, 8, 10, 3), squad(SquadCallsign.BRAVO, 8, 20, 0)));
        SquadBoardModel model = SquadBoardModel.of(snapshot, SquadCallsign.ALPHA, null, NOW);

        assertEquals(Role.UNASSIGNED, model.authority().role());
        assertEquals("assault", model.currentClassId(), "outside a squad: viewerClassId");
        ActionState join = model.rosterActions().button(Action.JOIN_SQUAD);
        assertTrue(join.enabled());
        assertNull(join.reason());
        assertEquals(ReasonCode.JOIN_HINT, join.hint().code());
        assertEquals("突击兵", join.hint().args().get(0) instanceof Component component
                ? component.getString() : "");
        assertEquals(KEY("action.join"), key(join.label()));
        assertEquals(SquadCallsign.ALPHA, join.squad());
        assertNull(join.confirm(), "joining needs no confirmation");

        SquadBoardModel bravo = SquadBoardModel.of(snapshot, SquadCallsign.BRAVO, null, NOW);
        ActionState create = bravo.rosterActions().button(Action.CREATE_SQUAD);
        assertTrue(create.enabled());
        assertEquals(ReasonCode.CREATE_HINT, create.hint().code());
        assertEquals(List.of(Action.CREATE_SQUAD),
                bravo.rosterActions().buttons().stream().map(ActionState::action).toList(),
                "no 'back to own squad' without a squad");

        // class rows: the current class is the default even outside a squad (squad-16)
        SquadBoardModel.ClassRow current = model.currentClassRow();
        assertNotNull(current);
        assertEquals("assault", current.classId());
        assertEquals(ReasonCode.CLASS_CURRENT, current.select().reason().code());
        assertEquals(ReasonCode.CLASS_NO_SQUAD, model.classRows().get(0).select().reason()
                .code());
        assertEquals(ReasonCode.CLASS_NOT_OPEN, model.classRows().get(3).select().reason()
                .code());
    }

    @Test
    void kickCooldownDisablesJoinAndCreateUntilItExpires() {
        SquadView alpha = squad(SquadCallsign.ALPHA, 8, 10, 3);
        SquadView bravo = squad(SquadCallsign.BRAVO, 8, 20, 0);
        BattleSnapshot snapshot = unassignedSnapshot(List.of(alpha, bravo))
                .withViewerContext(CONTEXT, "assault", List.of(
                        new KickCooldownView(SquadCallsign.ALPHA, NOW + 42_500L),
                        new KickCooldownView(SquadCallsign.BRAVO, NOW + 1_000L)));

        SquadBoardModel model = SquadBoardModel.of(snapshot, SquadCallsign.ALPHA, null, NOW);
        assertEquals(43, model.kickCooldownSeconds(SquadCallsign.ALPHA), "rounded up");
        assertEquals(SquadStatus.COOLDOWN, model.squad(SquadCallsign.ALPHA).status());
        ActionState join = model.rosterActions().button(Action.JOIN_SQUAD);
        assertFalse(join.enabled());
        assertEquals(ReasonCode.KICK_COOLDOWN, join.reason().code());
        assertEquals(List.of(43), join.reason().args());

        SquadBoardModel bravoView = SquadBoardModel.of(snapshot, SquadCallsign.BRAVO, null,
                NOW);
        assertEquals(ReasonCode.KICK_COOLDOWN, bravoView.rosterActions()
                .button(Action.CREATE_SQUAD).reason().code(),
                "the server checks the cooldown on create as well");

        SquadBoardModel later = SquadBoardModel.of(snapshot, SquadCallsign.ALPHA, null,
                NOW + 42_500L);
        assertEquals(0, later.kickCooldownSeconds(SquadCallsign.ALPHA));
        assertTrue(later.rosterActions().button(Action.JOIN_SQUAD).enabled(),
                "a countdown at zero no longer blocks, even before the next snapshot");
    }

    // ---------------------------------------------------------------------- own squad

    @Test
    void leaderManagesTheOwnSquadWithConfirmationsAndCanStillKickInCombat() {
        BattleSnapshot waiting = leaderSnapshot(defaultDeployment(), List.of());
        UUID offline = new UUID(0L, 13L);
        UUID online = new UUID(0L, 12L);

        SquadBoardModel none = SquadBoardModel.of(waiting, SquadCallsign.ALPHA, null, NOW);
        SquadBoardModel.RosterActions actions = none.rosterActions();
        assertTrue(actions.ownSquad());
        assertTrue(actions.targetRow());
        assertEquals(2, actions.split());
        assertEquals(List.of(Action.TRANSFER_LEADER, Action.KICK_MEMBER, Action.LEAVE_SQUAD,
                        Action.DISBAND_SQUAD),
                actions.buttons().stream().map(ActionState::action).toList());
        assertEquals(ReasonCode.SELECT_MEMBER, actions.reason().code());
        assertFalse(actions.button(Action.KICK_MEMBER).enabled());
        assertTrue(actions.button(Action.LEAVE_SQUAD).enabled());
        assertTrue(actions.button(Action.DISBAND_SQUAD).enabled());

        SquadBoardModel self = SquadBoardModel.of(waiting, SquadCallsign.ALPHA, VIEWER, NOW);
        assertEquals(ReasonCode.SELECT_OTHER_MEMBER,
                self.rosterActions().button(Action.KICK_MEMBER).reason().code());

        SquadBoardModel away = SquadBoardModel.of(waiting, SquadCallsign.ALPHA, offline, NOW);
        assertEquals(ReasonCode.TARGET_OFFLINE,
                away.rosterActions().button(Action.TRANSFER_LEADER).reason().code());
        assertTrue(away.rosterActions().button(Action.KICK_MEMBER).enabled(),
                "an offline member can still be kicked");

        SquadBoardModel picked = SquadBoardModel.of(waiting, SquadCallsign.ALPHA, online, NOW);
        ActionState transfer = picked.rosterActions().button(Action.TRANSFER_LEADER);
        assertTrue(transfer.enabled());
        assertEquals(online, transfer.target());
        assertNotNull(transfer.confirm());
        assertFalse(transfer.confirm().danger());
        ActionState kick = picked.rosterActions().button(Action.KICK_MEMBER);
        assertTrue(kick.confirm().danger());
        assertEquals(KEY("confirm.kick.title"), key(kick.confirm().title()));
        assertEquals(List.of("Rifleman12", SquadLabels.callsign(SquadCallsign.ALPHA),
                        SquadBoardModel.KICK_COOLDOWN_SECONDS),
                args(kick.confirm().body().get(0)));
        assertEquals(KEY("confirm.kick.active"), key(kick.confirm().body().get(1)),
                "a deployed target is warned about the pull-back");

        BattleSnapshot combat = leaderSnapshot(activeDeployment(), List.of());
        SquadBoardModel fighting = SquadBoardModel.of(combat, SquadCallsign.ALPHA, online, NOW);
        assertTrue(fighting.rosterActions().button(Action.KICK_MEMBER).enabled(),
                "the server allows kicking during combat");
        assertTrue(fighting.rosterActions().button(Action.TRANSFER_LEADER).enabled());
        assertEquals(ReasonCode.LEAVE_ACTIVE,
                fighting.rosterActions().button(Action.LEAVE_SQUAD).reason().code());
        assertEquals(ReasonCode.LEAVE_ACTIVE,
                fighting.rosterActions().button(Action.DISBAND_SQUAD).reason().code());
        assertEquals(ReasonCode.LEAVE_ACTIVE, fighting.rosterActions().reason().code());
    }

    @Test
    void plainMemberOnlyLeavesAndLeaveConfirmNamesTheConsequences() {
        SquadView alpha = new SquadView(SquadCallsign.ALPHA, new UUID(0L, 11L), List.of(
                member(new UUID(0L, 11L), "Leader11", SquadCallsign.ALPHA, true, false, true,
                        "support"),
                member(VIEWER, "Viewer", SquadCallsign.ALPHA, false, false, true, "assault")),
                8, List.of());
        BattleSnapshot snapshot = snapshot(SquadCallsign.ALPHA, false, false, List.of(alpha),
                MEMBER, defaultDeployment(), "assault", List.of());
        SquadBoardModel model = SquadBoardModel.of(snapshot, null, new UUID(0L, 11L), NOW);

        assertEquals(Role.MEMBER, model.authority().role());
        assertEquals(List.of(Action.LEAVE_SQUAD), model.rosterActions().buttons().stream()
                .map(ActionState::action).toList());
        assertFalse(model.rosterActions().targetRow());
        assertEquals(ReasonCode.LEADER_ONLY, model.rosterActions().reason().code());
        ActionState leave = model.rosterActions().button(Action.LEAVE_SQUAD);
        assertTrue(leave.danger());
        assertEquals(1, leave.confirm().body().size(), "a plain member: class reset only");
        assertEquals(KEY("confirm.leave.body"), key(leave.confirm().body().get(0)));

        // leader + commander leaving: resigns command and hands the squad to the first
        // online member (the offline one joined earlier but is skipped)
        SquadView led = new SquadView(SquadCallsign.ALPHA, VIEWER, List.of(
                member(VIEWER, "Viewer", SquadCallsign.ALPHA, true, true, true, "support"),
                member(new UUID(0L, 21L), "Away", SquadCallsign.ALPHA, false, false, false,
                        "assault"),
                member(new UUID(0L, 22L), "Here", SquadCallsign.ALPHA, false, false, true,
                        "assault")), 8, List.of());
        BattleSnapshot leaderSnapshot = snapshot(SquadCallsign.ALPHA, true, true, List.of(led),
                LEADER, defaultDeployment(), "support", List.of());
        SquadBoardModel leaderModel = SquadBoardModel.of(leaderSnapshot, null, null, NOW);
        List<Component> body = leaderModel.rosterActions().button(Action.LEAVE_SQUAD)
                .confirm().body();
        assertEquals(List.of(KEY("confirm.leave.body"), KEY("confirm.leave.commander"),
                KEY("confirm.leave.successor")), body.stream().map(SquadBoardModelTest::key)
                .toList());
        assertEquals(List.of("Here"), args(body.get(2)));
    }

    @Test
    void administratorWhoIsNotLeaderStillManagesTheOwnSquad() {
        SquadView alpha = new SquadView(SquadCallsign.ALPHA, new UUID(0L, 11L), List.of(
                member(new UUID(0L, 11L), "Leader11", SquadCallsign.ALPHA, true, false, true,
                        "support"),
                member(VIEWER, "Viewer", SquadCallsign.ALPHA, false, false, true, "assault")),
                8, List.of());
        PermissionView admin = new PermissionView(false, false, true, true, false, true, false);
        BattleSnapshot snapshot = snapshot(SquadCallsign.ALPHA, false, false, List.of(alpha),
                admin, defaultDeployment(), "assault", List.of());
        SquadBoardModel model = SquadBoardModel.of(snapshot, null, new UUID(0L, 11L), NOW);

        assertTrue(model.authority().administrator(),
                "canRemoveAnyMarker without command reveals the administrator");
        assertTrue(model.authority().manageOwnSquad());
        assertTrue(model.rosterActions().button(Action.KICK_MEMBER).enabled());
    }

    // ---------------------------------------------------------------------- commander

    @Test
    void commanderHandsCommandToAnOnlineLeaderOfAnotherSquad() {
        SquadView alpha = new SquadView(SquadCallsign.ALPHA, VIEWER, List.of(
                member(VIEWER, "Viewer", SquadCallsign.ALPHA, true, true, true, "support")),
                8, List.of());
        UUID bravoLeader = new UUID(0L, 21L);
        UUID bravoMember = new UUID(0L, 22L);
        SquadView bravo = new SquadView(SquadCallsign.BRAVO, bravoLeader, List.of(
                member(bravoLeader, "BravoLead", SquadCallsign.BRAVO, true, false, true,
                        "support"),
                member(bravoMember, "BravoMember", SquadCallsign.BRAVO, false, false, true,
                        "assault")), 8, List.of());
        BattleSnapshot snapshot = snapshot(SquadCallsign.ALPHA, true, true,
                List.of(alpha, bravo), new PermissionView(false, false, true, true, true, true,
                        false), defaultDeployment(), "support", List.of());

        SquadBoardModel noTarget = SquadBoardModel.of(snapshot, SquadCallsign.BRAVO,
                bravoMember, NOW);
        SquadBoardModel.CommanderPanel panel = noTarget.commanderPanel();
        assertEquals(VIEWER, panel.commander().playerId());
        assertEquals(SquadCallsign.ALPHA, panel.commanderSquad());
        assertTrue(panel.button(Action.RESIGN_COMMANDER).enabled());
        assertNull(panel.button(Action.RESIGN_COMMANDER).confirm(),
                "resigning does not ask (user decision)");
        assertEquals(ReasonCode.COMMANDER_TARGET,
                panel.button(Action.TRANSFER_COMMANDER).reason().code());

        SquadBoardModel target = SquadBoardModel.of(snapshot, SquadCallsign.BRAVO,
                bravoLeader, NOW);
        ActionState transfer = target.commanderPanel().button(Action.TRANSFER_COMMANDER);
        assertTrue(transfer.enabled());
        assertEquals(bravoLeader, transfer.target());
        assertEquals(KEY("confirm.transfer_commander.body"),
                key(transfer.confirm().body().get(0)));
        assertFalse(transfer.confirm().danger());
        assertEquals(ReasonCode.COMMANDER_TRANSFER_HINT,
                target.commanderPanel().reason().code());
        assertFalse(target.authority().administrator(),
                "a commander's canRemoveAnyMarker says nothing about admin rights");
    }

    @Test
    void claimReasonsFollowTheServerOrder() {
        BattleSnapshot unassigned = unassignedSnapshot(List.of(
                squad(SquadCallsign.ALPHA, 8, 10, 2)));
        assertEquals(ReasonCode.CLAIM_NO_SQUAD, SquadBoardModel.of(unassigned, null, null, NOW)
                .commanderPanel().reason().code());

        BattleSnapshot claimable = leaderSnapshot(defaultDeployment(), List.of());
        SquadBoardModel model = SquadBoardModel.of(claimable, null, null, NOW);
        assertTrue(model.commanderPanel().button(Action.CLAIM_COMMANDER).enabled());
        assertEquals(ReasonCode.CLAIM_HINT, model.commanderPanel().reason().code());

        SquadBoardModel waitingReceipt = SquadBoardModel.of(SquadBoardModel.Input.of(claimable)
                .withPending(EnumSet.of(Action.CLAIM_COMMANDER)));
        assertEquals(ReasonCode.PENDING, waitingReceipt.commanderPanel()
                .button(Action.CLAIM_COMMANDER).reason().code());
    }

    // ------------------------------------------------------------------- confirmations

    @Test
    void exactlyTheDecidedActionsAskForConfirmation() {
        assertEquals(EnumSet.of(Action.KICK_MEMBER, Action.LEAVE_SQUAD, Action.DISBAND_SQUAD,
                        Action.REDEPLOY, Action.TRANSFER_LEADER, Action.TRANSFER_COMMANDER),
                SquadBoardModel.confirmedActions());
        assertFalse(Action.RESIGN_COMMANDER.requiresConfirm());
        for (Action action : List.of(Action.KICK_MEMBER, Action.LEAVE_SQUAD,
                Action.DISBAND_SQUAD, Action.REDEPLOY)) {
            assertTrue(action.danger(), action + " is red and Enter does not confirm it");
        }
        assertThrows(IllegalArgumentException.class, () -> new ActionState(Action.DEPLOY,
                false, null, null, null, null, null, null, null));
    }

    /** 审查修正: an open confirmation is checked again against the newest snapshot. */
    @Test
    void anOpenConfirmationOnlySendsWhatTheNewestSnapshotStillOffers() {
        UUID rifleman = new UUID(0L, 12L);
        UUID medic = new UUID(0L, 14L);
        BattleSnapshot waiting = leaderSnapshot(defaultDeployment(), List.of());
        SquadBoardModel opened = SquadBoardModel.of(waiting, SquadCallsign.ALPHA, rifleman, NOW);
        ActionState kick = opened.rosterActions().button(Action.KICK_MEMBER);
        ActionState disband = opened.rosterActions().button(Action.DISBAND_SQUAD);
        ActionState leave = opened.rosterActions().button(Action.LEAVE_SQUAD);
        ActionState transfer = opened.rosterActions().button(Action.TRANSFER_LEADER);

        ActionState again = opened.stillOffered(kick);
        assertNotNull(again, "nothing changed: the kick is still offered");
        assertEquals(rifleman, again.target());
        assertNotNull(again.confirm());

        // the target left (the screen's selection is dropped) or another member is selected
        assertNull(SquadBoardModel.of(waiting, SquadCallsign.ALPHA, null, NOW)
                .stillOffered(kick));
        assertNull(SquadBoardModel.of(waiting, SquadCallsign.ALPHA, medic, NOW)
                .stillOffered(kick), "a different target is never kicked instead");

        // the squad went into combat: kicking and handing over stay, leave / disband do not
        SquadBoardModel combat = SquadBoardModel.of(leaderSnapshot(activeDeployment(),
                List.of()), SquadCallsign.ALPHA, rifleman, NOW);
        assertNotNull(combat.stillOffered(kick), "the server allows kicking in combat");
        assertNotNull(combat.stillOffered(transfer));
        assertNull(combat.stillOffered(disband));
        assertNull(combat.stillOffered(leave));

        // the viewer is no longer the leader: no management left, leaving still is
        SquadView alpha = new SquadView(SquadCallsign.ALPHA, rifleman, List.of(
                member(rifleman, "Rifleman12", SquadCallsign.ALPHA, true, false, true,
                        "assault"),
                member(VIEWER, "Viewer", SquadCallsign.ALPHA, false, false, true, "support")),
                8, List.of());
        SquadBoardModel demoted = SquadBoardModel.of(snapshot(SquadCallsign.ALPHA, false, false,
                        List.of(alpha), MEMBER, defaultDeployment(), "support", List.of()),
                SquadCallsign.ALPHA, rifleman, NOW);
        assertNull(demoted.stillOffered(kick));
        assertNull(demoted.stillOffered(transfer));
        assertNull(demoted.stillOffered(disband));
        assertNotNull(demoted.stillOffered(leave));

        // waiting for the server's answer to the same operation also stops it
        SquadBoardModel pending = SquadBoardModel.of(SquadBoardModel.Input.of(waiting)
                .withViewedSquad(SquadCallsign.ALPHA).withSelectedMember(rifleman)
                .withNow(NOW).withPending(EnumSet.of(Action.KICK_MEMBER)));
        assertNull(pending.stillOffered(kick));
        assertNull(opened.stillOffered(null));
        ActionState noTarget = SquadBoardModel.of(waiting, SquadCallsign.ALPHA, null, NOW)
                .rosterActions().button(Action.KICK_MEMBER);
        assertFalse(noTarget.enabled());
        assertNull(opened.stillOffered(noTarget), "a disabled state is never re-offered");
    }

    /** 审查修正: the 320-wide status strip in combat is not left empty. */
    @Test
    void compactCombatStripNamesSquadClassAndPoint() {
        SquadBoardModel combat = SquadBoardModel.of(leaderSnapshot(activeDeployment(),
                List.of()), null, null, NOW);
        assertTrue(combat.checklist().isEmpty(), "the waiting checklist is empty in combat");
        List<SquadBoardModel.ChecklistItem> items = DeploymentPagePainter.deployedItems(combat);
        assertEquals(List.of(CheckItem.SQUAD, CheckItem.CLASS, CheckItem.POINT),
                items.stream().map(SquadBoardModel.ChecklistItem::item).toList());
        assertTrue(items.stream().allMatch(item -> item.state() == CheckState.DONE));
        assertEquals("支援兵", items.get(1).value().getString());
        assertEquals(KEY("point.main_base"), key(items.get(2).value()));
    }

    /** 审查修正: a redeploy confirmation lapses once the viewer is no longer in combat. */
    @Test
    void aRedeployConfirmationLapsesOutsideCombat() {
        ActionState redeploy = SquadBoardModel.of(leaderSnapshot(activeDeployment(), List.of()),
                null, null, NOW).redeploy();
        assertTrue(redeploy.enabled());
        assertNotNull(SquadBoardModel.of(leaderSnapshot(activeDeployment(), List.of()), null,
                null, NOW).stillOffered(redeploy));
        assertNull(SquadBoardModel.of(leaderSnapshot(defaultDeployment(), List.of()), null,
                null, NOW).stillOffered(redeploy));
    }

    // ---------------------------------------------------------------------- classes

    @Test
    void classRowsFollowTheQuotaRulesInOrder() {
        BattleSnapshot waiting = leaderSnapshot(defaultDeployment(), List.of());
        SquadBoardModel model = SquadBoardModel.of(waiting, null, null, NOW);

        List<SquadBoardModel.ClassRow> rows = model.classRows();
        assertEquals(4, rows.size());
        assertEquals(ReasonCode.CLASS_CURRENT, rows.get(0).select().reason().code());
        assertTrue(rows.get(1).select().enabled());
        assertEquals(ReasonCode.CLASS_HINT, rows.get(1).select().hint().code());
        assertEquals(List.of(4), rows.get(1).select().hint().args());
        assertEquals("assault", rows.get(1).select().classId());
        assertEquals(ReasonCode.CLASS_FULL, rows.get(2).select().reason().code());
        assertEquals(ReasonCode.CLASS_NOT_OPEN, rows.get(3).select().reason().code());
        assertEquals(new ClassLimitView("assault", 6, 2), rows.get(1).squadLimit());
        assertEquals(2, rows.get(1).holders().size());

        BattleSnapshot combat = leaderSnapshot(activeDeployment(), List.of());
        assertEquals(ReasonCode.CLASS_ACTIVE, SquadBoardModel.of(combat, null, null, NOW)
                .classRows().get(1).select().reason().code());
    }

    // ------------------------------------------------------------------- deployment

    @Test
    void deployReasonsAreInferredFromTheSnapshot() {
        DeploymentPoint main = point(31, DeploymentPointKind.MAIN_BASE);
        DeploymentPoint beacon = point(32, DeploymentPointKind.FIELD_BEACON);
        DeploymentPoint beacon2 = point(33, DeploymentPointKind.FIELD_BEACON);

        DeploymentView waiting = new DeploymentView(DeploymentPhase.WAITING, 1L, 100L, 241L,
                100L, null, true, true, false, false, List.of(main, beacon, beacon2));
        SquadBoardModel model = SquadBoardModel.of(leaderSnapshot(waiting, List.of()), null,
                null, NOW);
        assertEquals(ReasonCode.DEPLOY_WAITING, model.deploy().reason().code());
        assertEquals(List.of(8), model.deploy().reason().args(), "141 ticks round up");
        assertEquals(8, model.respawnSeconds());
        assertEquals(List.of(1, 1, 2), model.pointRows().stream()
                .map(SquadBoardModel.PointRow::number).toList());
        assertEquals(List.of(2), args(model.pointRows().get(2).title()));
        assertEquals(List.of(CheckState.DONE, CheckState.DONE, CheckState.TODO,
                CheckState.WAITING), model.checklist().stream()
                .map(SquadBoardModel.ChecklistItem::state).toList());

        DeploymentView noPoint = new DeploymentView(DeploymentPhase.READY, 1L, 100L, 100L,
                100L, null, true, true, false, false, List.of(main));
        assertEquals(ReasonCode.DEPLOY_SELECT_POINT, SquadBoardModel.of(
                leaderSnapshot(noPoint, List.of()), null, null, NOW).deploy().reason().code());

        DeploymentView empty = new DeploymentView(DeploymentPhase.READY, 1L, 100L, 100L,
                100L, null, true, true, false, false, List.of());
        assertEquals(ReasonCode.DEPLOY_NO_POINTS, SquadBoardModel.of(
                leaderSnapshot(empty, List.of()), null, null, NOW).deploy().reason().code());

        DeploymentView ready = new DeploymentView(DeploymentPhase.READY, 1L, 100L, 100L,
                100L, main.id(), true, true, true, false, List.of(main, beacon));
        SquadBoardModel readyModel = SquadBoardModel.of(leaderSnapshot(ready, List.of()), null,
                null, NOW);
        assertTrue(readyModel.deploy().enabled());
        assertEquals(main.id(), readyModel.deploy().pointId());
        assertEquals(ReasonCode.POINT_SELECTED,
                readyModel.pointRows().get(0).select().reason().code());
        assertTrue(readyModel.pointRows().get(1).select().enabled());
        assertEquals(ReasonCode.REDEPLOY_NOT_ACTIVE, readyModel.redeploy().reason().code());
        assertEquals(ReasonCode.RESUPPLY_NOT_ACTIVE, readyModel.resupply().reason().code());

        BattleSnapshot unassigned = unassignedSnapshot(List.of(
                squad(SquadCallsign.ALPHA, 8, 10, 2))).withDeployment(noPoint);
        assertEquals(ReasonCode.DEPLOY_NO_SQUAD, SquadBoardModel.of(unassigned, null, null,
                NOW).deploy().reason().code());
    }

    @Test
    void combatOffersRedeployWithConfirmAndExplainsResupply() {
        SquadBoardModel model = SquadBoardModel.of(leaderSnapshot(activeDeployment(),
                List.of()), null, null, NOW);
        assertEquals(ReasonCode.DEPLOY_ACTIVE, model.deploy().reason().code());
        assertTrue(model.redeploy().enabled());
        assertTrue(model.redeploy().confirm().danger());
        assertEquals(ReasonCode.RESUPPLY_AWAY, model.resupply().reason().code());
        assertEquals(ReasonCode.POINT_SELECTED,
                model.pointRows().get(0).select().reason().code());
        assertEquals(ReasonCode.POINT_ACTIVE,
                model.pointRows().get(1).select().reason().code());
        assertTrue(model.checklist().isEmpty(), "no to-do list while in combat");
    }

    @Test
    void oneSharedPaginationForTwoThreeAndSixteenPoints() {
        for (int points : new int[]{2, 3, BattleNetworkLimits.MAX_DEPLOYMENT_POINTS}) {
            for (int rows : new int[]{1, 2, 5, 12}) {
                List<Integer> reached = new ArrayList<>();
                int requested = 0;
                int pages = 0;
                while (true) {
                    Page page = Page.of(points, rows, requested);
                    pages++;
                    assertEquals(Page.pageCount(points, rows), page.pageCount());
                    assertEquals((page.page() + 1) + "/" + page.pageCount(), page.label());
                    assertTrue(page.size() <= rows);
                    for (int index = page.start(); index < page.end(); index++) {
                        reached.add(index);
                    }
                    if (!page.hasNext()) {
                        assertEquals(page.page(), page.nextPage());
                        break;
                    }
                    requested = page.nextPage();
                }
                assertEquals(points, reached.size(), points + " points at " + rows + " rows");
                assertEquals(pages, Page.pageCount(points, rows),
                        "the title meta and the pager agree on the page count");
            }
        }
        Page clamped = Page.of(16, 5, 99);
        assertEquals(3, clamped.page());
        assertEquals(List.of(15), clamped.slice(java.util.stream.IntStream.range(0, 16)
                .boxed().toList()));
        assertEquals(1, Page.visibleRows(138, 156, 18, 20));
        assertEquals(0, Page.visibleRows(138, 155, 18, 20));
        assertThrows(IllegalArgumentException.class, () -> Page.visibleRows(0, 10, 18, 10));
        assertEquals(1, Page.pageCount(0, 3));
    }

    // ------------------------------------------------------------------- vote pending

    @Test
    void beforeTheLockNothingCreatesJoinsPicksOrDeploys() {
        DeploymentPoint main = point(31, DeploymentPointKind.MAIN_BASE);
        BattleSnapshot voting = new BattleSnapshot(VIEWER, Faction.BLUE, null, false, false,
                18, 12, BattleRules.FACTION_CAPACITY, 1, List.of(), List.of(), List.of(),
                NOTHING, QUOTAS, com.wok.infantry.support.SupportView.unavailable(),
                new DeploymentView(DeploymentPhase.READY, 1L, 100L, 100L, 100L, main.id(),
                        true, true, true, true, List.of(main)), NOW, 1L,
                new FormationContextView("", "", "", "academy", "学院军", 40, "kaiser", "凯撒",
                        40), "", List.of());
        SquadBoardModel model = SquadBoardModel.of(SquadBoardModel.Input.of(voting)
                .withVotePhase(FormationVotePhase.OPEN));

        assertEquals(Stage.VOTE_PENDING, model.stage());
        assertTrue(model.squads().stream().allMatch(row ->
                row.status() == SquadStatus.VOTE_PENDING));
        for (ActionState state : model.actions()) {
            assertFalse(state.enabled(), state.action() + " must stay disabled before the lock");
            assertEquals(ReasonCode.VOTE_PENDING, state.reason().code());
        }
        assertEquals("", model.currentClassId());
        assertNull(model.currentClassRow());
        assertEquals(KEY("identity.vote_open"), key(model.identityCandidates().get(0)));
        assertEquals(List.of(CheckItem.FORMATION, CheckItem.SQUAD, CheckItem.CLASS,
                CheckItem.POINT), model.checklist().stream()
                .map(SquadBoardModel.ChecklistItem::item).toList());
        // Same rule as the formation page's strip (6.2): every tab but the formation tab waits.
        for (BattleTab tab : BattleTab.values()) {
            if (tab == BattleTab.FORMATION) {
                assertNull(model.tabDisabledReason(tab));
            } else {
                assertNotNull(model.tabDisabledReason(tab), tab + " must wait for the lock");
            }
        }
        assertNull(SquadBoardModel.of(leaderSnapshot(defaultDeployment(), List.of()), null,
                null, NOW).tabDisabledReason(BattleTab.MAP));
    }

    @Test
    void loadingAndIdentityCandidatesShortenStepByStep() {
        SquadBoardModel loading = SquadBoardModel.of(null, null, null, 0L);
        assertEquals(Stage.LOADING, loading.stage());
        assertTrue(loading.squads().stream().allMatch(row -> !row.configured()));
        assertTrue(loading.actions().stream().noneMatch(ActionState::enabled));
        assertEquals("screen.wok_infantry.map.link_connecting",
                key(loading.identityCandidates().get(0)));

        SquadBoardModel leader = SquadBoardModel.of(leaderSnapshot(defaultDeployment(),
                List.of()), null, null, NOW);
        assertEquals(40, leader.factionCapacity());
        assertEquals(40, leader.enemyFactionCapacity());
        List<Component> identity = leader.identityCandidates();
        assertEquals(5, identity.size());
        assertEquals("学院军 · 千禧年研讨会机动部队 · squad.wok_infantry.alpha · "
                + "role.wok_infantry.squad_leader", flat(identity.get(0)));
        assertEquals("学院军 · squad.wok_infantry.alpha.short", flat(identity.get(4)));
    }

    @Test
    void everyKeyTheModelCanEmitExistsInBothLanguages() throws IOException {
        Set<String> keys = SquadBoardModel.translationKeys();
        assertTrue(keys.size() > 200);
        for (String resource : List.of("assets/wok_infantry/lang/zh_cn.json",
                "assets/wok_infantry/lang/en_us.json")) {
            JsonObject bundle = bundle(resource);
            for (String key : keys) {
                assertTrue(bundle.has(key), resource + " is missing " + key);
            }
            for (String key : List.of(SquadLabels.UNASSIGNED_KEY, SquadLabels.MEMBER_SHORT_KEY,
                    "dimension.minecraft.overworld")) {
                assertTrue(bundle.has(key), resource + " is missing " + key);
            }
        }
    }

    // ----------------------------------------------------------------------- fixtures

    private static String KEY(String suffix) {
        return SquadBoardModel.KEY_PREFIX + suffix;
    }

    /** Viewer leads ALPHA (support) with 4 members; BRAVO has 2 members; CHARLIE is empty. */
    private static BattleSnapshot leaderSnapshot(DeploymentView deployment,
                                                 List<KickCooldownView> cooldowns) {
        SquadView alpha = new SquadView(SquadCallsign.ALPHA, VIEWER, List.of(
                member(VIEWER, "Viewer", SquadCallsign.ALPHA, true, false, true, "support"),
                member(new UUID(0L, 12L), "Rifleman12", SquadCallsign.ALPHA, false, false,
                        true, "assault"),
                member(new UUID(0L, 13L), "Offline13", SquadCallsign.ALPHA, false, false,
                        false, "assault"),
                member(new UUID(0L, 14L), "Medic14", SquadCallsign.ALPHA, false, false, true,
                        "medic")), 8,
                List.of(new ClassLimitView("support", 1, 1), new ClassLimitView("assault", 6, 2),
                        new ClassLimitView("medic", 1, 1), new ClassLimitView("sniper", 0, 0)));
        SquadView bravo = squad(SquadCallsign.BRAVO, 8, 20, 2);
        SquadView charlie = squad(SquadCallsign.CHARLIE, 6, 30, 0);
        return snapshot(SquadCallsign.ALPHA, true, false, List.of(alpha, bravo, charlie),
                LEADER, deployment, "support", cooldowns);
    }

    private static BattleSnapshot unassignedSnapshot(List<SquadView> squads) {
        return snapshot(null, false, false, squads, UNASSIGNED, defaultDeployment(),
                "assault", List.of());
    }

    private static BattleSnapshot snapshot(SquadCallsign ownSquad, boolean leader,
                                           boolean commander, List<SquadView> squads,
                                           PermissionView permissions,
                                           DeploymentView deployment, String viewerClassId,
                                           List<KickCooldownView> cooldowns) {
        int members = squads.stream().mapToInt(squad -> squad.members().size()).sum();
        return new BattleSnapshot(VIEWER, Faction.BLUE, ownSquad, leader, commander,
                members + (ownSquad == null ? 1 : 0), 12, BattleRules.FACTION_CAPACITY, 8,
                squads, List.of(), List.of(), permissions,
                ownSquad == null ? List.of(new ClassQuotaView("support", "支援兵", 1, 0),
                        new ClassQuotaView("assault", "突击兵", 6, 1),
                        new ClassQuotaView("medic", "医疗兵", 2, 0),
                        new ClassQuotaView("sniper", "狙击手", 0, 0)) : QUOTAS,
                com.wok.infantry.support.SupportView.unavailable(), deployment, NOW, 1L,
                CONTEXT, viewerClassId, cooldowns);
    }

    /** Squad of {@code size} members starting at player index {@code first}; first leads. */
    private static SquadView squad(SquadCallsign callsign, int capacity, int first, int size) {
        List<MemberView> members = new ArrayList<>();
        for (int offset = 0; offset < size; offset++) {
            UUID id = new UUID(0L, first + offset);
            members.add(member(id, "Player" + (first + offset), callsign, offset == 0, false,
                    true, offset == 0 ? "support" : "assault"));
        }
        return new SquadView(callsign, size == 0 ? null : members.get(0).playerId(), members,
                capacity, List.of());
    }

    private static MemberView member(UUID id, String name, SquadCallsign squad, boolean leader,
                                     boolean commander, boolean online, String classId) {
        MemberState state = online ? MemberState.DEPLOYED : MemberState.OFFLINE;
        return new MemberView(id, name, online, online, online ? 20.0F : 0.0F, 20.0F, leader,
                commander, squad, classId, state, online ? 1.0F : MemberView.UNKNOWN_HEALTH_RATIO);
    }

    private static DeploymentPoint point(int index, DeploymentPointKind kind) {
        return new DeploymentPoint(new UUID(1L, index), Faction.BLUE, OVERWORLD,
                new BlockPos(index, 64, -index), 0.0F, DeploymentPoint.DEFAULT_SUPPLY_RADIUS,
                kind);
    }

    private static DeploymentView defaultDeployment() {
        DeploymentPoint main = point(31, DeploymentPointKind.MAIN_BASE);
        return new DeploymentView(DeploymentPhase.READY, 1L, 100L, 100L, 100L, main.id(),
                true, true, true, false, List.of(main, point(32, DeploymentPointKind.RALLY)));
    }

    private static DeploymentView activeDeployment() {
        DeploymentPoint main = point(31, DeploymentPointKind.MAIN_BASE);
        return new DeploymentView(DeploymentPhase.ACTIVE, 1L, 100L, 100L, 100L, main.id(),
                false, false, false, false, List.of(main, point(32, DeploymentPointKind.RALLY)));
    }

    private static String key(Component component) {
        return ((TranslatableContents) component.getContents()).getKey();
    }

    private static List<Object> args(Component component) {
        return List.of(((TranslatableContents) component.getContents()).getArgs());
    }

    /** Literal text and raw translation keys of a component tree, in order. */
    private static String flat(Component component) {
        StringBuilder result = new StringBuilder();
        component.visit(text -> {
            result.append(text);
            return java.util.Optional.empty();
        });
        return result.toString();
    }

    private static JsonObject bundle(String resource) throws IOException {
        try (InputStream stream = SquadBoardModelTest.class.getClassLoader()
                .getResourceAsStream(resource)) {
            assertNotNull(stream, resource);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }
}
