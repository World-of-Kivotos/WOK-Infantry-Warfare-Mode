package com.wok.infantry.client.screen;

import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.battle.ClassQuotaView;
import com.wok.infantry.battle.MemberView;
import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.battle.SquadView;
import com.wok.infantry.client.BattleClientActions;
import com.wok.infantry.client.ClientBattleState;
import com.wok.infantry.deployment.DeploymentPhase;
import com.wok.infantry.deployment.DeploymentPoint;
import com.wok.infantry.deployment.DeploymentPointKind;
import com.wok.infantry.deployment.DeploymentView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Deployment/squad management surface modelled after large-team tactical shooters. */
public final class SquadScreen extends Screen {
    private enum Page {
        SQUADS,
        CLASSES,
        DEPLOYMENT
    }

    private final Screen previous;
    private Page page = Page.SQUADS;
    private int classPage;
    private int deploymentPage;
    private SquadCallsign selectedSquad;
    private SquadCallsign observedOwnSquad;
    private boolean ownSquadObserved;
    private UUID selectedMember;
    private long redeployConfirmUntilNanos;
    private long disbandConfirmUntilNanos;
    private long observedGeneration = -1L;
    private boolean requestedSnapshot;

    private int contentTop;
    private int contentBottom;
    private int leftX;
    private int leftWidth;
    private int centerX;
    private int centerWidth;
    private int rightX;
    private int rightWidth;
    private boolean compact;
    private TacticalMapLayout.Layout tabletLayout;

    public SquadScreen() {
        this(null, false);
    }

    public SquadScreen(Screen previous) {
        this(previous, false);
    }

    /** Used by the server after login/death to open directly on the deployment workflow. */
    public SquadScreen(Screen previous, boolean openDeployment) {
        super(Component.translatable("screen.wok_infantry.squad"));
        this.previous = previous;
        this.page = openDeployment ? Page.DEPLOYMENT : Page.SQUADS;
    }

    @Override
    protected void init() {
        observedGeneration = ClientBattleState.generation();
        BattleSnapshot snapshot = ClientBattleState.snapshot();
        if (snapshot == null && !requestedSnapshot) {
            requestedSnapshot = true;
            BattleClientActions.requestSnapshot();
        }

        layoutPanels();
        initNavigation();
        if (snapshot == null) {
            addRenderableWidget(BattleUiButton.builder(
                            Component.translatable("gui.wok_infantry.refresh"),
                            ignored -> BattleClientActions.requestSnapshot())
                    .bounds(width / 2 - 55, height / 2 + 12, 110, 20).build());
            return;
        }

        normalizeSelection(snapshot);
        switch (page) {
            case SQUADS -> initSquadWidgets(snapshot);
            case CLASSES -> initClassWidgets(snapshot);
            case DEPLOYMENT -> initDeploymentWidgets(snapshot);
        }
    }

    private void layoutPanels() {
        tabletLayout = TacticalMapLayout.compute(width, height);
        contentTop = tabletLayout.rich() ? 58 : 48;
        contentBottom = Math.max(contentTop + 100, tabletLayout.footer().top() - 8);
        compact = width < 690;
        int gap = 6;
        int horizontalMargin = tabletLayout.rich() ? 16 : 8;
        int contentRight = width - horizontalMargin;
        leftX = horizontalMargin;
        if (compact) {
            leftWidth = Math.max(132, Math.min(210, (width - 22) * 2 / 5));
            centerX = leftX + leftWidth + gap;
            centerWidth = Math.max(110, contentRight - centerX);
            rightX = width;
            rightWidth = 0;
        } else {
            leftWidth = Math.max(170, Math.min(230, width / 4));
            rightWidth = Math.max(155, Math.min(205, width / 5));
            centerX = leftX + leftWidth + gap;
            rightX = contentRight - rightWidth;
            centerWidth = Math.max(180, rightX - gap - centerX);
        }
    }

    private void initNavigation() {
        int buttonY = tabletLayout.topBarY();
        int utilityWidth = 22;
        int tabCount = 5;
        int tabGap = 3;
        int refreshX = tabletLayout.header().right() - 4 - utilityWidth;
        int available = Math.max(170, refreshX - leftX - 6);
        int tabWidth = Math.max(34, Math.min(96,
                (available - tabGap * (tabCount - 1)) / tabCount));
        int x = leftX;

        Component squadsLabel = fittedButtonLabel("screen.wok_infantry.tab.squads",
                "screen.wok_infantry.tab.squads_short", tabWidth);
        Button squads = BattleUiButton.builder(squadsLabel, ignored -> {
            page = Page.SQUADS;
            rebuildWidgets();
        }).selected(page == Page.SQUADS)
                .tooltip(Tooltip.create(Component.translatable("screen.wok_infantry.tab.squads")))
                .bounds(x, buttonY, tabWidth, 20).build();
        squads.active = page != Page.SQUADS;
        addRenderableWidget(squads);
        x += tabWidth + 3;

        Component classesLabel = fittedButtonLabel("screen.wok_infantry.tab.classes",
                "screen.wok_infantry.tab.classes_short", tabWidth);
        Button classes = BattleUiButton.builder(classesLabel, ignored -> {
            page = Page.CLASSES;
            rebuildWidgets();
        }).selected(page == Page.CLASSES)
                .tooltip(Tooltip.create(Component.translatable("screen.wok_infantry.tab.classes")))
                .bounds(x, buttonY, tabWidth, 20).build();
        classes.active = page != Page.CLASSES;
        addRenderableWidget(classes);
        x += tabWidth + 3;

        Component deploymentLabel = fittedButtonLabel("screen.wok_infantry.tab.deployment",
                "screen.wok_infantry.tab.deployment_short", tabWidth);
        Button deployment = BattleUiButton.builder(deploymentLabel, ignored -> {
                    page = Page.DEPLOYMENT;
                    rebuildWidgets();
                }).selected(page == Page.DEPLOYMENT)
                .tooltip(Tooltip.create(Component.translatable(
                        "screen.wok_infantry.tab.deployment")))
                .bounds(x, buttonY, tabWidth, 20).build();
        deployment.active = page != Page.DEPLOYMENT;
        addRenderableWidget(deployment);
        x += tabWidth + 3;

        Component loadoutLabel = fittedButtonLabel("screen.wok_infantry.tab.loadout",
                "screen.wok_infantry.tab.loadout_short", tabWidth);
        addRenderableWidget(BattleUiButton.builder(loadoutLabel,
                        ignored -> BattleClientActions.openLoadout())
                .tooltip(Tooltip.create(Component.translatable("screen.wok_infantry.tab.loadout")))
                .bounds(x, buttonY, tabWidth, 20).build());
        x += tabWidth + 3;

        Component mapLabel = fittedButtonLabel("screen.wok_infantry.tab.map",
                "screen.wok_infantry.tab.map_short", tabWidth);
        addRenderableWidget(BattleUiButton.builder(mapLabel, ignored ->
                        Minecraft.getInstance().setScreen(new TacticalMapScreen(this)))
                .tooltip(Tooltip.create(Component.translatable("screen.wok_infantry.tab.map")))
                .bounds(x, buttonY, tabWidth, 20).build());

        addRenderableWidget(BattleUiButton.builder(Component.literal("R"), ignored ->
                        BattleClientActions.requestSnapshot())
                .kind(BattleUiButton.Kind.CONTROL)
                .tooltip(Tooltip.create(Component.translatable("gui.wok_infantry.refresh")))
                .bounds(refreshX, buttonY, utilityWidth, 20).build());
    }

    private void initSquadWidgets(BattleSnapshot snapshot) {
        int rowTop = contentTop + 25;
        int rowHeight = Math.max(14, Math.min(34,
                (contentBottom - rowTop - 29) / SquadCallsign.values().length));
        for (int index = 0; index < SquadCallsign.values().length; index++) {
            SquadCallsign callsign = SquadCallsign.values()[index];
            SquadView squad = ClientBattleState.squad(callsign);
            int y = rowTop + index * rowHeight;
            int actionWidth = Math.min(54, leftWidth / 3);
            int selectWidth = leftWidth - actionWidth - 10;
            int rowButtonHeight = Math.max(12, rowHeight - 2);
            int squadButtonWidth = Math.max(55, selectWidth);
            boolean ownSquad = snapshot.ownSquad() == callsign;
            boolean selected = selectedSquad == callsign;
            Component fullSquadLabel = squadButtonLabel(callsign, squad, ownSquad, selected);
            Component visibleSquadLabel = fittedSquadButtonLabel(callsign, squad, ownSquad,
                    selected, fullSquadLabel, squadButtonWidth);
            addRenderableWidget(BattleUiButton.builder(visibleSquadLabel, ignored -> {
                selectedSquad = callsign;
                selectedMember = null;
                rebuildWidgets();
            }).selected(selected)
                    .tooltip(Tooltip.create(fullSquadLabel))
                    .bounds(leftX + 5, y, squadButtonWidth, rowButtonHeight).build());

            if (squad == null || !squad.active()) {
                Button create = BattleUiButton.builder(Component.translatable("gui.wok_infantry.create"), ignored -> {
                            selectedSquad = callsign;
                            BattleClientActions.createSquad(callsign);
                        })
                        .bounds(leftX + leftWidth - actionWidth - 4, y,
                                actionWidth, rowButtonHeight).build();
                create.active = snapshot.permissions().canCreateSquad()
                        && snapshot.deployment().canChangeSquad();
                addRenderableWidget(create);
            } else if (snapshot.ownSquad() != callsign) {
                Button join = BattleUiButton.builder(Component.translatable("gui.wok_infantry.join"), ignored -> {
                            selectedSquad = callsign;
                            BattleClientActions.joinSquad(callsign);
                        })
                        .bounds(leftX + leftWidth - actionWidth - 4, y,
                                actionWidth, rowButtonHeight).build();
                join.active = snapshot.permissions().canJoinSquad()
                        && snapshot.deployment().canChangeSquad()
                        && squad.members().size() < Math.max(1, squad.capacity());
                addRenderableWidget(join);
            }
        }

        boolean canDisband = snapshot.squadLeader()
                && snapshot.ownSquad() != null
                && snapshot.deployment().canChangeSquad();
        SquadView selected = ClientBattleState.squad(selectedSquad);
        if (selected != null) {
            int memberTop = contentTop + 28;
            int actionY = contentBottom - 26;
            int memberActionTop = canDisband ? actionY - 24 : actionY;
            int minimumMemberHeight = canDisband ? 9 : 12;
            int memberHeight = Math.max(minimumMemberHeight, Math.min(compact ? 25 : 28,
                    Math.max(1, memberActionTop - memberTop - 3) / 8));
            List<MemberView> members = selected.members().stream()
                    .sorted(Comparator.comparing(MemberView::leader).reversed())
                    .limit(8).toList();
            for (int index = 0; index < members.size(); index++) {
                MemberView member = members.get(index);
                int y = memberTop + index * memberHeight;
                int memberButtonWidth = centerWidth - 12;
                Component fullMemberLabel = memberButtonLabel(snapshot, index + 1, member);
                Component visibleMemberLabel = ellipsizedButtonLabel(
                        fullMemberLabel, memberButtonWidth);
                addRenderableWidget(BattleUiButton.builder(visibleMemberLabel, ignored -> {
                    selectedMember = member.playerId().equals(selectedMember)
                            ? null : member.playerId();
                    rebuildWidgets();
                }).selected(member.playerId().equals(selectedMember))
                        .tooltip(Tooltip.create(fullMemberLabel))
                        .bounds(centerX + 6, y, memberButtonWidth,
                                Math.max(9, memberHeight - 3)).build());
            }
        }

        int actionY = contentBottom - 26;
        int buttonWidth = Math.max(36, (centerWidth - 18) / 3);
        Component leaveLabel = fittedButtonLabel("gui.wok_infantry.leave_squad",
                "gui.wok_infantry.leave_squad_short", buttonWidth);
        Button leave = BattleUiButton.builder(leaveLabel, ignored ->
                        BattleClientActions.leaveSquad())
                .tooltip(Tooltip.create(Component.translatable("gui.wok_infantry.leave_squad")))
                .bounds(centerX + 5, actionY, buttonWidth, 20).build();
        leave.active = snapshot.permissions().canLeaveSquad()
                && snapshot.deployment().canChangeSquad();
        addRenderableWidget(leave);

        MemberView target = ClientBattleState.member(selectedMember);
        boolean canManageTarget = snapshot.permissions().canManageSquad()
                && target != null
                && !target.playerId().equals(snapshot.viewerId())
                && target.squad() == snapshot.ownSquad();
        Component transferFullLabel = targetActionLabel(
                "gui.wok_infantry.transfer_leader", target);
        Component transferLabel = fittedButtonLabel(transferFullLabel,
                "gui.wok_infantry.transfer_leader_short", buttonWidth);
        Button transfer = BattleUiButton.builder(transferLabel, ignored -> {
            if (selectedMember != null) {
                BattleClientActions.transferLeadership(selectedMember);
            }
        }).tooltip(Tooltip.create(transferFullLabel))
                .bounds(centerX + 9 + buttonWidth, actionY, buttonWidth, 20).build();
        transfer.active = canManageTarget && target.online();
        addRenderableWidget(transfer);

        Component kickFullLabel = targetActionLabel("gui.wok_infantry.kick", target);
        int kickWidth = Math.max(36, centerWidth - (18 + buttonWidth * 2));
        Component kickLabel = fittedButtonLabel(kickFullLabel,
                "gui.wok_infantry.kick_short", kickWidth);
        Button kick = BattleUiButton.builder(kickLabel, ignored -> {
            if (selectedMember != null) {
                BattleClientActions.kickMember(selectedMember);
            }
        }).kind(BattleUiButton.Kind.DANGER)
                .tooltip(Tooltip.create(kickFullLabel))
                .bounds(centerX + 13 + buttonWidth * 2, actionY, kickWidth, 20).build();
        kick.active = canManageTarget;
        addRenderableWidget(kick);

        if (canDisband) {
            boolean confirming = disbandConfirmUntilNanos > System.nanoTime();
            Button disband = BattleUiButton.builder(Component.translatable(confirming
                            ? "gui.wok_infantry.disband_squad_confirm"
                            : "gui.wok_infantry.disband_squad"), ignored -> {
                        long now = System.nanoTime();
                        if (disbandConfirmUntilNanos > now) {
                            disbandConfirmUntilNanos = 0L;
                            BattleClientActions.disbandSquad();
                        } else {
                            disbandConfirmUntilNanos = now + 3_000_000_000L;
                            rebuildWidgets();
                        }
                    }).kind(BattleUiButton.Kind.DANGER)
                    .bounds(centerX + 5, actionY - 24, centerWidth - 10, 20).build();
            addRenderableWidget(disband);
        }

        int commanderX = compact ? leftX + 5 : rightX + 6;
        int commanderWidth = compact ? leftWidth - 10 : rightWidth - 12;
        if (snapshot.commander()) {
            int gap = 4;
            int resignWidth = Math.max(40, (commanderWidth - gap) / 2);
            Component resignFullLabel = Component.translatable(
                    "gui.wok_infantry.resign_commander");
            Component resignLabel = fittedButtonLabel(resignFullLabel,
                    "gui.wok_infantry.resign_commander_short", resignWidth);
            addRenderableWidget(BattleUiButton.builder(resignLabel, ignored ->
                            BattleClientActions.resignCommander())
                    .tooltip(Tooltip.create(resignFullLabel))
                    .bounds(commanderX, contentBottom - 26, resignWidth, 20).build());
            boolean validCommanderTarget = target != null && target.leader() && target.online()
                    && !target.playerId().equals(snapshot.viewerId());
            int commanderTransferWidth = Math.max(40,
                    commanderWidth - resignWidth - gap);
            Component commanderTransferFullLabel = targetActionLabel(
                    "gui.wok_infantry.transfer_commander", target);
            Component commanderTransferLabel = fittedButtonLabel(commanderTransferFullLabel,
                    "gui.wok_infantry.transfer_commander_short", commanderTransferWidth);
            Button commanderTransfer = BattleUiButton.builder(commanderTransferLabel, ignored -> {
                        if (selectedMember != null) {
                            BattleClientActions.transferCommander(selectedMember);
                        }
                    }).tooltip(Tooltip.create(commanderTransferFullLabel))
                    .bounds(commanderX + resignWidth + gap, contentBottom - 26,
                            commanderTransferWidth, 20).build();
            commanderTransfer.active = validCommanderTarget;
            addRenderableWidget(commanderTransfer);
        } else if (snapshot.permissions().canClaimCommander()) {
            Component claimFullLabel = Component.translatable(
                    "gui.wok_infantry.claim_commander");
            Component claimLabel = fittedButtonLabel(claimFullLabel,
                    "gui.wok_infantry.claim_commander_short", commanderWidth);
            addRenderableWidget(BattleUiButton.builder(claimLabel, ignored ->
                            BattleClientActions.claimCommander())
                    .tooltip(Tooltip.create(claimFullLabel))
                    .bounds(commanderX, contentBottom - 26, commanderWidth, 20).build());
        }
    }

    private void initClassWidgets(BattleSnapshot snapshot) {
        int rowTop = contentTop + 29;
        int listX = compact ? leftX : centerX;
        int listWidth = compact ? leftWidth + 6 + centerWidth : centerWidth;
        String currentClass = currentClass(snapshot);
        List<ClassQuotaView> quotas = snapshot.classQuotas();
        int bottomY = contentBottom - 26;
        int availableHeight = Math.max(1, bottomY - rowTop - 3);
        int rowsPerPage = Math.max(1, availableHeight / 24);
        int pageCount = Math.max(1, (quotas.size() + rowsPerPage - 1) / rowsPerPage);
        classPage = Math.max(0, Math.min(classPage, pageCount - 1));
        int start = classPage * rowsPerPage;
        int end = Math.min(quotas.size(), start + rowsPerPage);
        int rowHeight = Math.max(14, Math.min(34,
                availableHeight / Math.max(1, end - start)));
        for (int index = start; index < end; index++) {
            ClassQuotaView quota = quotas.get(index);
            int y = rowTop + (index - start) * rowHeight;
            if (y + Math.max(11, rowHeight - 4) > bottomY) {
                break;
            }
            MutableComponent label = className(quota.classId(), quota.displayName()).append("  ")
                    .append(Component.translatable("screen.wok_infantry.class.quota",
                            quota.used(), quota.limit()));
            if (quota.classId().equals(currentClass)) {
                label.append("  ").append(Component.translatable("screen.wok_infantry.class.selected"));
            }
            Button choose = BattleUiButton.builder(label, ignored ->
                            BattleClientActions.selectClass(quota.classId()))
                    .selected(quota.classId().equals(currentClass))
                    .bounds(listX + 6, y, listWidth - 12,
                            Math.max(11, rowHeight - 4)).build();
            choose.active = snapshot.ownSquad() != null
                    && snapshot.deployment().canChangeClass()
                    && !quota.classId().equals(currentClass)
                    && quota.remaining() > 0;
            Component reason;
            if (snapshot.ownSquad() == null) {
                reason = Component.translatable("screen.wok_infantry.class.reason.no_squad");
            } else if (!snapshot.deployment().canChangeClass()) {
                reason = Component.translatable("screen.wok_infantry.class.reason.active");
            } else if (quota.classId().equals(currentClass)) {
                reason = Component.translatable("screen.wok_infantry.class.reason.current");
            } else if (quota.remaining() <= 0) {
                reason = Component.translatable("screen.wok_infantry.class.reason.full");
            } else {
                reason = Component.translatable("screen.wok_infantry.class.reason.available");
            }
            choose.setTooltip(Tooltip.create(reason));
            addRenderableWidget(choose);
        }

        int pagerWidth = 30;
        Button previous = BattleUiButton.builder(Component.literal("<"), ignored -> {
            classPage--;
            rebuildWidgets();
        }).bounds(listX + 6, bottomY, pagerWidth, 20).build();
        previous.active = classPage > 0;
        addRenderableWidget(previous);
        Button next = BattleUiButton.builder(Component.literal(">"), ignored -> {
            classPage++;
            rebuildWidgets();
        }).bounds(listX + listWidth - 6 - pagerWidth, bottomY, pagerWidth, 20).build();
        next.active = classPage + 1 < pageCount;
        addRenderableWidget(next);
        addRenderableWidget(BattleUiButton.builder(Component.translatable("screen.wok_infantry.class.configure_loadout"),
                        ignored -> BattleClientActions.openLoadout())
                .bounds(listX + 40, bottomY, Math.max(40, listWidth - 80), 20).build());
    }

    private void initDeploymentWidgets(BattleSnapshot snapshot) {
        DeploymentView deployment = snapshot.deployment();
        int panelX = leftX;
        int panelWidth = Math.max(120, width - panelX
                - (tabletLayout.rich() ? 16 : 8));
        int actionY = contentBottom - 26;
        DeploymentLayout layout = deploymentLayout();
        int rowsPerPage = visibleDeploymentRows(layout.listTop(), actionY - 2,
                layout.pointHeight(), layout.rowPitch());
        DeploymentPagination pagination = deploymentPagination(
                deployment.points().size(), rowsPerPage, deploymentPage);
        deploymentPage = pagination.page();
        for (int index = pagination.startInclusive();
             index < pagination.endExclusive(); index++) {
            DeploymentPoint point = deployment.points().get(index);
            int localIndex = index - pagination.startInclusive();
            int y = layout.listTop() + localIndex * layout.rowPitch();
            boolean selected = point.id().equals(deployment.selectedPointId());
            MutableComponent label = selected ? Component.literal("✓ ") : Component.empty();
            String pointKey = point.kind() == DeploymentPointKind.MAIN_BASE
                    ? "screen.wok_infantry.deployment.main_base"
                    : "screen.wok_infantry.deployment.field_beacon";
            label.append(Component.translatable(pointKey,
                    point.dimension(), point.position().getX(), point.position().getY(),
                    point.position().getZ()));

            int pointX = panelX + 7;
            int pointWidth = panelWidth - 14;
            if (pagination.multiplePages() && localIndex == 0) {
                int pagerWidth = compact ? 24 : 28;
                int pagerGap = 3;
                Button previousPage = BattleUiButton.builder(Component.literal("<"), ignored -> {
                    deploymentPage = pagination.previousPage();
                    rebuildWidgets();
                }).bounds(pointX, y, pagerWidth, layout.pointHeight()).build();
                previousPage.active = pagination.hasPrevious();
                addRenderableWidget(previousPage);

                Button nextPage = BattleUiButton.builder(Component.literal(">"), ignored -> {
                    deploymentPage = pagination.nextPage();
                    rebuildWidgets();
                }).bounds(pointX + pointWidth - pagerWidth, y,
                        pagerWidth, layout.pointHeight()).build();
                nextPage.active = pagination.hasNext();
                addRenderableWidget(nextPage);

                pointX += pagerWidth + pagerGap;
                pointWidth -= (pagerWidth + pagerGap) * 2;
            }
            Button pointButton = BattleUiButton.builder(label, ignored ->
                            BattleClientActions.selectDeploymentPoint(point.id()))
                    .selected(selected)
                    .bounds(pointX, y, Math.max(40, pointWidth), layout.pointHeight()).build();
            pointButton.active = deployment.phase() != DeploymentPhase.ACTIVE && !selected;
            addRenderableWidget(pointButton);
        }

        int gap = 4;
        int actionWidth = Math.max(48, (panelWidth - 14 - gap * 2) / 3);
        int actionX = panelX + 7;
        Button deploy = BattleUiButton.builder(Component.translatable("gui.wok_infantry.deploy"), ignored ->
                        BattleClientActions.deploy())
                .kind(BattleUiButton.Kind.SUCCESS)
                .bounds(actionX, actionY, actionWidth, 20).build();
        deploy.active = deployment.canDeploy();
        addRenderableWidget(deploy);
        actionX += actionWidth + gap;

        boolean confirmingRedeploy = redeployConfirmUntilNanos > System.nanoTime();
        Button redeploy = BattleUiButton.builder(Component.translatable(confirmingRedeploy
                        ? "gui.wok_infantry.redeploy_confirm" : "gui.wok_infantry.redeploy"), ignored -> {
                    long now = System.nanoTime();
                    if (redeployConfirmUntilNanos > now) {
                        redeployConfirmUntilNanos = 0L;
                        BattleClientActions.redeploy();
                    } else {
                        redeployConfirmUntilNanos = now + 3_000_000_000L;
                        rebuildWidgets();
                    }
                })
                .kind(BattleUiButton.Kind.DANGER)
                .bounds(actionX, actionY, actionWidth, 20).build();
        redeploy.active = deployment.phase() == DeploymentPhase.ACTIVE;
        addRenderableWidget(redeploy);
        actionX += actionWidth + gap;

        Button resupply = BattleUiButton.builder(Component.translatable("gui.wok_infantry.resupply"), ignored ->
                        BattleClientActions.resupply())
                .kind(BattleUiButton.Kind.SUCCESS)
                .bounds(actionX, actionY,
                        Math.max(48, panelX + panelWidth - 7 - actionX), 20).build();
        resupply.active = deployment.canResupply();
        addRenderableWidget(resupply);
    }

    @Override
    public void tick() {
        super.tick();
        if (redeployConfirmUntilNanos != 0L
                && redeployConfirmUntilNanos <= System.nanoTime()) {
            redeployConfirmUntilNanos = 0L;
            rebuildWidgets();
            return;
        }
        if (disbandConfirmUntilNanos != 0L
                && disbandConfirmUntilNanos <= System.nanoTime()) {
            disbandConfirmUntilNanos = 0L;
            rebuildWidgets();
            return;
        }
        if (observedGeneration != ClientBattleState.generation()) {
            rebuildWidgets();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        BattleSnapshot snapshot = ClientBattleState.snapshot();
        TacticalBoardChrome.renderShell(graphics, width, height, tabletLayout);
        TacticalBoardChrome.renderHeader(graphics, font, tabletLayout,
                Component.translatable("screen.wok_infantry.squad.board_title"),
                TacticalBoardChrome.battleIdentity(snapshot), snapshot != null);
        if (snapshot == null) {
            BattleUiTheme.drawCenteredText(graphics, font,
                    Component.translatable("screen.wok_infantry.waiting_snapshot"),
                    width / 2, height / 2 - 10, TacticalBoardTheme.MUTED_TEXT);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        switch (page) {
            case SQUADS -> renderSquadPage(graphics, snapshot);
            case CLASSES -> renderClassPage(graphics, snapshot);
            case DEPLOYMENT -> renderDeploymentPage(graphics, snapshot);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        TacticalMapLayout.Rect footer = tabletLayout.footer();
        BattleUiTheme.feedback(graphics, font, footer.left(), footer.right(),
                footer.top() + 5);
    }

    private void renderSquadPage(GuiGraphics graphics, BattleSnapshot snapshot) {
        TacticalBoardTheme.raisedPanel(graphics, leftX, contentTop,
                leftX + leftWidth, contentBottom, TacticalBoardTheme.BOARD_ALT);
        TacticalBoardTheme.raisedPanel(graphics, centerX, contentTop,
                centerX + centerWidth, contentBottom, TacticalBoardTheme.BOARD_ALT);
        TacticalBoardTheme.sectionHeader(graphics, font,
                Component.translatable("screen.wok_infantry.squad_list"),
                leftX + 4, contentTop + 4, leftX + leftWidth - 4,
                TacticalBoardTheme.SELECTED);

        SquadView selected = ClientBattleState.squad(selectedSquad);
        MutableComponent heading = selectedSquad == null
                ? Component.translatable("screen.wok_infantry.no_squad_selected")
                : callsign(selectedSquad).copy();
        if (selected != null) {
            heading.append("  ").append(Component.translatable("screen.wok_infantry.squad.member_count",
                    selected.members().size(), Math.max(snapshot.squadCapacity(), selected.capacity())));
        }
        TacticalBoardTheme.sectionHeader(graphics, font, heading,
                centerX + 4, contentTop + 4, centerX + centerWidth - 4,
                TacticalBoardTheme.ACCENT);

        if (selected == null || selected.members().isEmpty()) {
            BattleUiTheme.drawCenteredText(graphics, font,
                    Component.translatable("screen.wok_infantry.squad.empty"),
                    centerX + centerWidth / 2, contentTop + 55,
                    TacticalBoardTheme.MUTED_TEXT);
        }

        if (!compact) {
            renderCommandPanel(graphics, snapshot);
        }
    }

    private void renderCommandPanel(GuiGraphics graphics, BattleSnapshot snapshot) {
        TacticalBoardTheme.raisedPanel(graphics, rightX, contentTop,
                rightX + rightWidth, contentBottom, TacticalBoardTheme.BOARD_ALT);
        TacticalBoardTheme.sectionHeader(graphics, font,
                Component.translatable("screen.wok_infantry.command_panel"),
                rightX + 4, contentTop + 4, rightX + rightWidth - 4,
                TacticalBoardTheme.ACCENT);
        int y = contentTop + 29;
        renderStatusLine(graphics, rightX + 7, y,
                "screen.wok_infantry.status.squad",
                snapshot.ownSquad() == null
                        ? Component.translatable("screen.wok_infantry.status.none")
                        : callsign(snapshot.ownSquad()),
                snapshot.ownSquad() == null ? TacticalBoardTheme.MUTED_TEXT
                        : TacticalBoardTheme.FRIENDLY);
        y += 19;
        renderStatusLine(graphics, rightX + 7, y,
                "screen.wok_infantry.status.role",
                role(snapshot), TacticalBoardTheme.TEXT);
        y += 30;
        int maxHintLines = Math.max(1, (contentBottom - 32 - y) / 10);
        drawWrappedText(graphics, Component.translatable("screen.wok_infantry.command_hint"),
                rightX + 7, y, rightWidth - 14, maxHintLines,
                TacticalBoardTheme.MUTED_TEXT);
    }

    private void renderClassPage(GuiGraphics graphics, BattleSnapshot snapshot) {
        int listX = compact ? leftX : centerX;
        int listWidth = compact ? leftWidth + 6 + centerWidth : centerWidth;
        TacticalBoardTheme.raisedPanel(graphics, listX, contentTop,
                listX + listWidth, contentBottom, TacticalBoardTheme.BOARD_ALT);
        TacticalBoardTheme.sectionHeader(graphics, font,
                Component.translatable("screen.wok_infantry.class.title"),
                listX + 4, contentTop + 4, listX + listWidth - 4,
                TacticalBoardTheme.ACCENT);
        if (!compact) {
            TacticalBoardTheme.raisedPanel(graphics, leftX, contentTop,
                    leftX + leftWidth, contentBottom, TacticalBoardTheme.BOARD_ALT);
            TacticalBoardTheme.sectionHeader(graphics, font,
                    Component.translatable("screen.wok_infantry.deployment_status"),
                    leftX + 4, contentTop + 4, leftX + leftWidth - 4,
                    TacticalBoardTheme.SELECTED);
            int y = contentTop + 32;
            MutableComponent squad = snapshot.ownSquad() == null
                    ? Component.translatable("screen.wok_infantry.status.none")
                    : callsign(snapshot.ownSquad()).copy();
            renderStatusLine(graphics, leftX + 7, y,
                    "screen.wok_infantry.status.squad", squad,
                    TacticalBoardTheme.FRIENDLY);
            y += 20;
            renderStatusLine(graphics, leftX + 7, y,
                    "screen.wok_infantry.status.class",
                    className(snapshot, currentClass(snapshot)),
                    TacticalBoardTheme.TEXT);
            renderCommandPanel(graphics, snapshot);
        }
    }

    private void renderDeploymentPage(GuiGraphics graphics, BattleSnapshot snapshot) {
        DeploymentView deployment = snapshot.deployment();
        int panelX = leftX;
        int panelWidth = Math.max(120, width - panelX
                - (tabletLayout.rich() ? 16 : 8));
        DeploymentLayout layout = deploymentLayout();
        TacticalBoardTheme.raisedPanel(graphics, panelX, contentTop,
                panelX + panelWidth, contentBottom, TacticalBoardTheme.BOARD_ALT);
        TacticalBoardTheme.sectionHeader(graphics, font,
                Component.translatable("screen.wok_infantry.deployment.title"),
                panelX + 4, contentTop + 4, panelX + panelWidth - 4,
                TacticalBoardTheme.ACCENT);

        Component phase = Component.translatable("screen.wok_infantry.deployment.phase."
                + deployment.phase().name().toLowerCase(java.util.Locale.ROOT));
        renderStatusLine(graphics, panelX + 7, layout.phaseY(),
                "screen.wok_infantry.deployment.phase", phase,
                deployment.phase() == DeploymentPhase.ACTIVE
                        ? TacticalBoardTheme.FRIENDLY : TacticalBoardTheme.TEXT);

        MutableComponent squad = snapshot.ownSquad() == null
                ? Component.translatable("screen.wok_infantry.status.none")
                : callsign(snapshot.ownSquad()).copy();
        int squadX = compact ? panelX + 7
                : panelX + Math.max(118, panelWidth / 3);
        renderStatusLine(graphics, squadX, layout.squadY(),
                "screen.wok_infantry.status.squad", squad,
                snapshot.ownSquad() == null ? TacticalBoardTheme.MUTED_TEXT
                        : TacticalBoardTheme.FRIENDLY);

        Component hint = deploymentHint(snapshot, deployment);
        drawWrappedText(graphics, hint, panelX + 7, layout.hintY(), panelWidth - 14,
                layout.maxHintLines(), TacticalBoardTheme.MUTED_TEXT);

        int rowsPerPage = visibleDeploymentRows(layout.listTop(), contentBottom - 52,
                layout.pointHeight(), layout.rowPitch());
        DeploymentPagination pagination = deploymentPagination(
                deployment.points().size(), rowsPerPage, deploymentPage);
        MutableComponent pointsHeading = Component.translatable(
                "screen.wok_infantry.deployment.points");
        if (pagination.multiplePages()) {
            pointsHeading.append(Component.literal("  " + (pagination.page() + 1)
                    + "/" + pagination.pageCount()));
        }
        graphics.drawString(font, pointsHeading, panelX + 7, layout.pointsHeadingY(),
                TacticalBoardTheme.TEXT, false);
    }

    private DeploymentLayout deploymentLayout() {
        if (compact) {
            return new DeploymentLayout(contentTop + 26, contentTop + 38,
                    contentTop + 51, 2, contentTop + 76, contentTop + 90,
                    18, 20);
        }
        return new DeploymentLayout(contentTop + 27, contentTop + 27,
                contentTop + 47, 1, contentTop + 61, contentTop + 72,
                20, 23);
    }

    static int visibleDeploymentRows(int listTop, int listBottomExclusive,
                                     int pointHeight, int rowPitch) {
        if (pointHeight <= 0 || rowPitch < pointHeight) {
            throw new IllegalArgumentException("Invalid deployment row geometry");
        }
        int availableHeight = Math.max(0, listBottomExclusive - listTop);
        if (availableHeight < pointHeight) {
            return 0;
        }
        return 1 + (availableHeight - pointHeight) / rowPitch;
    }

    static int deploymentPageCount(int pointCount, int rowsPerPage) {
        if (pointCount <= 0 || rowsPerPage <= 0) {
            return 1;
        }
        return 1 + (pointCount - 1) / rowsPerPage;
    }

    static DeploymentPagination deploymentPagination(int pointCount, int rowsPerPage,
                                                       int requestedPage) {
        int safePointCount = Math.max(0, pointCount);
        int pageCount = deploymentPageCount(safePointCount, rowsPerPage);
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
        int start = rowsPerPage <= 0 ? 0 : Math.min(safePointCount, page * rowsPerPage);
        int end = rowsPerPage <= 0 ? 0
                : Math.min(safePointCount, start + rowsPerPage);
        return new DeploymentPagination(page, pageCount, start, end);
    }

    private void drawWrappedText(GuiGraphics graphics, Component text, int x, int y,
                                 int maxWidth, int maxLines, int color) {
        List<FormattedCharSequence> lines = font.split(text, Math.max(1, maxWidth));
        int visibleLines = Math.min(Math.max(0, maxLines), lines.size());
        for (int index = 0; index < visibleLines; index++) {
            graphics.drawString(font, lines.get(index), x, y + index * 10, color, false);
        }
    }

    private record DeploymentLayout(int phaseY, int squadY, int hintY, int maxHintLines,
                                    int pointsHeadingY, int listTop,
                                    int pointHeight, int rowPitch) {
    }

    record DeploymentPagination(int page, int pageCount,
                                int startInclusive, int endExclusive) {
        boolean multiplePages() {
            return pageCount > 1;
        }

        boolean hasPrevious() {
            return page > 0;
        }

        boolean hasNext() {
            return page + 1 < pageCount;
        }

        int previousPage() {
            return hasPrevious() ? page - 1 : page;
        }

        int nextPage() {
            return hasNext() ? page + 1 : page;
        }
    }

    private static Component deploymentHint(BattleSnapshot snapshot, DeploymentView deployment) {
        if (deployment.phase() == DeploymentPhase.WAITING) {
            long seconds = (deployment.waitingTicks() + 19L) / 20L;
            return Component.translatable("screen.wok_infantry.deployment.waiting", seconds);
        }
        if (deployment.phase() == DeploymentPhase.ACTIVE) {
            if (deployment.canResupply()) {
                return Component.translatable("screen.wok_infantry.deployment.resupply_ready");
            }
            if (deployment.resupplyTicks() > 0L) {
                long seconds = (deployment.resupplyTicks() + 19L) / 20L;
                return Component.translatable("screen.wok_infantry.deployment.resupply_cooldown",
                        seconds);
            }
            return Component.translatable("screen.wok_infantry.deployment.return_to_base");
        }
        if (snapshot.ownSquad() == null) {
            return Component.translatable("screen.wok_infantry.deployment.join_squad_first");
        }
        if (deployment.points().isEmpty()) {
            return Component.translatable("screen.wok_infantry.deployment.no_base");
        }
        if (deployment.selectedPointId() == null) {
            return Component.translatable("screen.wok_infantry.deployment.select_point");
        }
        if (!deployment.canDeploy()) {
            return Component.translatable("screen.wok_infantry.deployment.not_ready");
        }
        return Component.translatable("screen.wok_infantry.deployment.ready");
    }

    private void renderStatusLine(GuiGraphics graphics, int x, int y, String labelKey,
                                  Component value, int valueColor) {
        Component label = Component.translatable(labelKey);
        graphics.drawString(font, label, x, y, TacticalBoardTheme.MUTED_TEXT, false);
        graphics.drawString(font, value, x + font.width(label) + 4, y, valueColor, false);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(previous);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void normalizeSelection(BattleSnapshot snapshot) {
        if (!ownSquadObserved || observedOwnSquad != snapshot.ownSquad()) {
            observedOwnSquad = snapshot.ownSquad();
            ownSquadObserved = true;
            if (snapshot.ownSquad() != null) {
                selectedSquad = snapshot.ownSquad();
            }
        }
        if (selectedSquad == null) {
            selectedSquad = snapshot.ownSquad();
        }
        if (selectedSquad == null && !snapshot.squads().isEmpty()) {
            selectedSquad = snapshot.squads().get(0).callsign();
        }
        if (selectedSquad == null) {
            selectedSquad = SquadCallsign.ALPHA;
        }
        MemberView member = ClientBattleState.member(selectedMember);
        if (member == null || member.squad() != selectedSquad) {
            selectedMember = null;
        }
    }

    @Override
    protected void rebuildWidgets() {
        clearWidgets();
        init();
    }

    private String currentClass(BattleSnapshot snapshot) {
        MemberView viewer = ClientBattleState.member(snapshot.viewerId());
        return viewer == null ? "assault" : viewer.classId();
    }

    private MutableComponent memberButtonLabel(BattleSnapshot snapshot, int index,
                                               MemberView member) {
        MutableComponent result = Component.literal(
                        member.playerId().equals(selectedMember) ? "✓ " : "")
                .append(Component.literal(index + "  "))
                .append(member.online() ? "● " : "○ ");
        if (member.commander()) {
            result.append(Component.translatable("hud.wok_infantry.role.commander_short")).append(" ");
        } else if (member.leader()) {
            result.append(Component.translatable("hud.wok_infantry.role.leader_short")).append(" ");
        }
        result.append(Component.literal(member.name())).append("  ")
                .append(className(snapshot, member.classId()));
        return result;
    }

    private static MutableComponent role(BattleSnapshot snapshot) {
        if (snapshot.commander()) {
            return Component.translatable("role.wok_infantry.commander");
        }
        if (snapshot.squadLeader()) {
            return Component.translatable("role.wok_infantry.squad_leader");
        }
        return Component.translatable("role.wok_infantry.member");
    }

    public static MutableComponent callsign(SquadCallsign value) {
        return Component.translatable("squad.wok_infantry." + value.id());
    }

    private static MutableComponent squadButtonLabel(SquadCallsign callsign, SquadView squad,
                                                      boolean own, boolean selected) {
        MutableComponent label = Component.literal(selected ? "› " : "")
                .append(callsign(callsign));
        if (own) {
            label.append(Component.literal(" ★"));
        }
        if (squad != null && squad.active()) {
            label.append(Component.literal("  " + squad.members().size() + "/" + squad.capacity()));
        }
        return label;
    }

    private Component fittedSquadButtonLabel(SquadCallsign callsign, SquadView squad,
                                             boolean own, boolean selected,
                                             Component regular, int buttonWidth) {
        if (font.width(regular) <= Math.max(1, buttonWidth - 6)) {
            return regular;
        }
        String abbreviation = callsign.id().substring(0, 1).toUpperCase(Locale.ROOT);
        MutableComponent compactLabel = Component.literal(selected ? "› " : "")
                .append(Component.literal(abbreviation));
        if (own) {
            compactLabel.append(Component.literal(" ★"));
        }
        if (squad != null && squad.active()) {
            compactLabel.append(Component.literal(" " + squad.members().size()
                    + "/" + squad.capacity()));
        }
        return ellipsizedButtonLabel(compactLabel, buttonWidth);
    }

    private static Component targetActionLabel(String key, MemberView target) {
        MutableComponent label = Component.translatable(key);
        if (target != null) {
            label.append(Component.literal(" · " + target.name()));
        }
        return label;
    }

    private Component fittedButtonLabel(String regularKey, String shortKey, int buttonWidth) {
        return fittedButtonLabel(Component.translatable(regularKey), shortKey, buttonWidth);
    }

    private Component fittedButtonLabel(Component regular, String shortKey, int buttonWidth) {
        Component candidate = font.width(regular) <= Math.max(1, buttonWidth - 6)
                ? regular : Component.translatable(shortKey);
        return ellipsizedButtonLabel(candidate, buttonWidth);
    }

    private Component ellipsizedButtonLabel(Component label, int buttonWidth) {
        int available = Math.max(1, buttonWidth - 6);
        if (font.width(label) <= available) {
            return label;
        }
        String ellipsis = "…";
        int bodyWidth = Math.max(1, available - font.width(ellipsis));
        return Component.literal(font.plainSubstrByWidth(label.getString(), bodyWidth)
                + ellipsis);
    }

    public static MutableComponent className(String classId) {
        String safeId = classId == null || classId.isBlank() ? "assault" : classId;
        return Component.translatableWithFallback("class.wok_infantry." + safeId, safeId);
    }

    public static MutableComponent className(BattleSnapshot snapshot, String classId) {
        String configuredName = snapshot == null ? "" : snapshot.classQuotas().stream()
                .filter(quota -> quota.classId().equals(classId))
                .map(ClassQuotaView::displayName)
                .findFirst().orElse("");
        return className(classId, configuredName);
    }

    public static MutableComponent className(String classId, String configuredName) {
        String safeConfiguredName = ClassQuotaView.sanitizeDisplayName(configuredName);
        return safeConfiguredName.isBlank()
                ? className(classId) : Component.literal(safeConfiguredName);
    }
}
