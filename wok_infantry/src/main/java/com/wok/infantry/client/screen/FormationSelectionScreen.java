package com.wok.infantry.client.screen;

import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.client.ClientBattleState;
import com.wok.infantry.client.ClientBootstrap;
import com.wok.infantry.client.ClientFormationState;
import com.wok.infantry.client.KeyBindingDefaults;
import com.wok.infantry.client.hud.TacticalHud;
import com.wok.infantry.client.ui.probe.UiLayoutProbe;
import com.wok.infantry.client.ui.probe.UiSurfaceInfo;
import com.wok.infantry.deployment.DeploymentPhase;
import com.wok.infantry.formation.selection.FactionSelectionView;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.formation.selection.FormationSelectionView;
import com.wok.infantry.formation.vote.FormationVotePhase;
import com.wok.infantry.network.battle.BattleOpenTarget;
import com.wok.infantry.network.battle.client.BattleClientNetworkBridge;
import com.wok.infantry.network.formation.client.FormationClientNetworkBridge;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Faction and formation vote page of the battle terminal (preview {@code 45-formation.js} "new").
 *
 * <p>Flow: join a faction (confirmed) → the administrator opens the vote → vote → the
 * administrator locks one formation (danger confirmation) → deployment. Faction keys and formation
 * rows only change what is looked at; the join, vote, open and lock keys are separate and every
 * disabled key says why next to it. The footer always carries the next step
 * ({@link FormationVoteModel#step()}) unless a 3-second receipt replaces it.
 *
 * <p>Layouts ({@link FormationScreenLayout}): wide three areas (strip, list, detail) or, below
 * 440 logical pixels, a list page and a detail page. Esc: an open dialog is cancelled first, the
 * narrow detail page goes back to the list, otherwise the page closes; without a faction the
 * action bar then says which key reopens it (user report 4).
 *
 * <p>For the UI acceptance the page reports its preview surface and state ({@link UiSurfaceInfo})
 * and tags its keys with the {@code *_UI_ID} probe ids; both are inert outside the uiTest probe.
 */
public final class FormationSelectionScreen extends TacticalScreen
        implements BattleTerminalNav.Terminal, UiSurfaceInfo {
    /** Preview surface id ({@code ui-preview/surfaces/45-formation.js}). */
    public static final String SURFACE_ID = "formation";
    /** Probe ids of the page's keys (the uiTest input driver clicks them by these ids). */
    public static final String ADMIN_OPEN_UI_ID = "formation.admin.open";
    public static final String ADMIN_LOCK_UI_ID = "formation.admin.lock";
    /** Administrator test-start key (0.4.0-beta.2). */
    public static final String TEST_START_UI_ID = "formation.admin.test";
    public static final String JOIN_UI_ID = "formation.join";
    public static final String VOTE_UI_ID = "formation.vote";
    public static final String DETAILS_UI_ID = "formation.details";
    public static final String BACK_UI_ID = "formation.back";
    public static final String RETRY_UI_ID = "formation.retry";
    public static final String LIST_UI_ID = "formation.list";
    /** Faction keys are {@code formation.faction/<faction id>}. */
    public static final String FACTION_UI_ID_PREFIX = "formation.faction/";
    /** Probe boxes of the page's regions (texts must stay inside them). */
    public static final String STRIP_BOX = "formation.strip";
    public static final String LIST_BOX = "formation.list_panel";
    public static final String DETAIL_BOX = "formation.detail";
    public static final String CRUMB_BOX = "formation.crumb";
    public static final String WAITING_BOX = "formation.waiting";

    /** How the page was opened, which decides what a settled snapshot does to it. */
    public enum Entry {
        /** Pushed by the server: closes once nothing is left to choose. */
        SERVER,
        /** Opened from the terminal's formation tab: stays open to show the locked result. */
        TERMINAL,
        /** Opened by the terminal key without a formation: falls back to the squad page. */
        KEY
    }

    private static final int RETRY_KEY_WIDTH = 116;

    private FormationSelectionSnapshot snapshot;
    private final Screen returnScreen;
    private final Entry entry;
    private String browseFactionId = "";
    private String highlightedFormationId = "";
    private boolean detailPage;
    private boolean rebuildPending;
    private final FormationListWidget list = new FormationListWidget(this::highlight,
            ignored -> castVote());
    private final FormationDetailPanel detail = new FormationDetailPanel();
    private FormationScreenLayout layout;
    private UiRect actionKey = UiRect.EMPTY;
    private int reasonLeft;
    private UiRect actionBar = UiRect.EMPTY;
    /**
     * Which page control each focusable widget of the current build is (its probe id), so a
     * rebuild (a teammate's vote, a new highlight) gives the keyboard focus back to the same
     * control instead of dropping it.
     */
    private final Map<GuiEventListener, String> roles = new IdentityHashMap<>();
    /** The join or lock confirmation this page opened, while it is open. */
    private OpenDialog openDialog;
    /**
     * The administrator confirmed a test start from this page (0.4.0-beta.2 审查修正): the
     * battle snapshot that reports the deployment closes the page whichever way it was opened.
     */
    private boolean awaitingTestStart;

    private enum DialogKind {
        JOIN,
        LOCK,
        /** Administrator test start (0.4.0-beta.2). */
        TEST
    }

    /** A confirmation and what it is about, to re-check it when a newer catalog arrives. */
    private record OpenDialog(DialogKind kind, String factionId, String formationId,
                              TacticalConfirmDialog dialog) {
    }

    /** Role used for the tab strip in {@link #roles} (it has no probe id). */
    private static final String TABS_ROLE = "formation.tabs";

    public FormationSelectionScreen(FormationSelectionSnapshot snapshot, Screen returnScreen) {
        this(snapshot, returnScreen, Entry.SERVER);
    }

    /**
     * @param snapshot     the catalog, or {@code null} to show the waiting state until it arrives
     * @param returnScreen screen the whole terminal returns to
     */
    public FormationSelectionScreen(FormationSelectionSnapshot snapshot, Screen returnScreen,
                                    Entry entry) {
        super(Component.translatable("screen.wok_infantry.formation.title"));
        this.snapshot = snapshot;
        this.returnScreen = returnScreen;
        this.entry = entry == null ? Entry.SERVER : entry;
        normalizeSelection();
    }

    public Screen returnScreen() {
        return returnScreen;
    }

    @Override
    public Screen terminalReturnScreen() {
        return returnScreen;
    }

    public Entry entry() {
        return entry;
    }

    /** Whether this page sent a test start that has not been answered by a deployment yet. */
    public boolean awaitingTestStart() {
        return awaitingTestStart;
    }

    /**
     * Whether an arriving battle snapshot closes this page (0.4.0-beta.2 审查修正): only after
     * this page sent a test start, once the snapshot reports the viewer deployed without asking
     * for another page. Before, the page closed only when the server had pushed it; opened by
     * the terminal key it turned into the squad page and from the terminal tab it stayed open
     * over the battlefield. A refused deployment asks for the deployment page instead.
     */
    public static boolean closesOnTestStartDeployment(boolean awaitingTestStart,
                                                      DeploymentPhase phase,
                                                      BattleOpenTarget openTarget) {
        return awaitingTestStart && phase == DeploymentPhase.ACTIVE
                && openTarget == BattleOpenTarget.NONE;
    }

    /** The catalog shown, or {@code null} while waiting for it. */
    public FormationSelectionSnapshot snapshot() {
        return snapshot;
    }

    @Override
    public String uiSurfaceId() {
        return SURFACE_ID;
    }

    @Override
    public String uiStateId() {
        Boolean dangerDialog = modal() instanceof TacticalConfirmDialog dialog
                ? dialog.danger() : null;
        boolean testDialog = openDialog != null && openDialog.kind() == DialogKind.TEST
                && modal() == openDialog.dialog();
        return uiStateId(model(), detailPage && layout != null
                && layout.mode() == FormationScreenLayout.Mode.NARROW_DETAIL, dangerDialog,
                testDialog);
    }

    /**
     * {@link #uiStateId(FormationVoteModel, boolean, Boolean)} with the administrator's
     * test-start confirmation, which reports {@code testconfirm} (0.4.0-beta.2).
     */
    static String uiStateId(FormationVoteModel model, boolean detailPage, Boolean dangerDialog,
                            boolean testDialog) {
        if (testDialog && model.stage() != FormationVoteModel.Stage.WAITING
                && model.stage() != FormationVoteModel.Stage.EMPTY) {
            return "testconfirm";
        }
        return uiStateId(model, detailPage, dangerDialog);
    }

    /**
     * Preview state ({@code 45-formation.js} states) of what the page shows: {@code waiting},
     * {@code join}, {@code confirm}, {@code facfull}, {@code latejoin}, {@code lateconfirm},
     * {@code pending}, {@code vote}, {@code detail}, {@code full}, {@code admintie},
     * {@code admin} (lock confirmation) or {@code locked}.
     *
     * @param detailPage   the narrow detail page is shown
     * @param dangerDialog {@code null} without a dialog, else whether it is the (dangerous) lock
     *                     confirmation rather than the join confirmation
     */
    static String uiStateId(FormationVoteModel model, boolean detailPage, Boolean dangerDialog) {
        FormationVoteModel.Stage stage = model.stage();
        if (stage == FormationVoteModel.Stage.WAITING || stage == FormationVoteModel.Stage.EMPTY) {
            return "waiting";
        }
        boolean browsingLocked = model.phase(model.browsing()) == FormationVotePhase.LOCKED;
        if (dangerDialog != null) {
            return dangerDialog ? "admin" : browsingLocked ? "lateconfirm" : "confirm";
        }
        switch (stage) {
            case UNJOINED -> {
                FormationVoteModel.JoinBlock block = model.joinAction().block();
                if (block == FormationVoteModel.JoinBlock.FACTION_FULL
                        || block == FormationVoteModel.JoinBlock.LOCKED_FULL) {
                    return "facfull";
                }
                return browsingLocked ? "latejoin" : "join";
            }
            case NOT_STARTED -> {
                return "pending";
            }
            case LOCKED -> {
                return "locked";
            }
            default -> {
                FormationSelectionView highlighted = model.highlighted();
                if (highlighted != null && (!highlighted.available()
                        || model.shortfall(model.browsing(), highlighted))) {
                    return "full";
                }
                if (model.admin().visible() && model.leaders().tie()) {
                    return "admintie";
                }
                return detailPage || !model.snapshot().ownVoteFormationId().isBlank()
                        ? "detail" : "vote";
            }
        }
    }

    /**
     * Shows a newer catalog in place: highlight, scroll position and the keyboard focus are kept,
     * and an open join or lock confirmation is rewritten from it (or closed when its target no
     * longer applies).
     */
    public void replaceSnapshot(FormationSelectionSnapshot replacement) {
        snapshot = replacement;
        normalizeSelection();
        if (minecraft != null) {
            rebuildKeepingFocus();
            refreshOpenDialog();
        }
    }

    static String administratorOpenVoteCommand(String factionId) {
        return "battle admin formation vote open " + factionId + " true";
    }

    static String administratorLockCommand(String factionId, String formationId) {
        return "battle admin formation vote lock " + factionId + " " + formationId;
    }

    /** The test-start key's command (no new packet: the command tree checks the permission). */
    static String administratorTestStartCommand(String factionId, String formationId) {
        return "battle admin test start " + factionId + " " + formationId;
    }

    /** Current page state (recomputed on demand; cheap). */
    FormationVoteModel model() {
        return FormationVoteModel.of(snapshot, browseFactionId, highlightedFormationId,
                isAdministrator());
    }

    private void normalizeSelection() {
        if (snapshot == null) {
            return;
        }
        FormationVoteModel probe = FormationVoteModel.of(snapshot, browseFactionId,
                highlightedFormationId, false);
        String previousFaction = browseFactionId;
        browseFactionId = probe.browsing() == null ? "" : probe.browsing().id();
        FormationSelectionView highlighted = probe.highlighted();
        highlightedFormationId = highlighted == null ? "" : highlighted.id();
        if (!previousFaction.equals(browseFactionId)) {
            detail.reset();
        }
    }

    private boolean isAdministrator() {
        return minecraft != null && minecraft.player != null
                && minecraft.player.hasPermissions(BattleRules.ADMIN_PERMISSION_LEVEL);
    }

    // ---- widgets ------------------------------------------------------------------------------------

    @Override
    protected void initTactical() {
        rebuildPending = false;
        roles.clear();
        FormationVoteModel model = model();
        TacticalShellLayout.Metrics metrics = TacticalShellLayout.Metrics.forSize(width, height);
        boolean waiting = model.stage() == FormationVoteModel.Stage.WAITING
                || model.stage() == FormationVoteModel.Stage.EMPTY;
        if (!FormationScreenLayout.narrow(width, height)) {
            detailPage = false;
        }
        int listNeed = waiting ? 0
                : FormationListWidget.naturalHeight(model.browsing(), metrics);
        int joinWidth = waiting || model.joined() ? 0
                : font.width(FormationText.joinLabel(model).get(0)) + 34;
        int testKeyWidth = waiting || !model.testStart().visible() ? 0
                : font.width(FormationText.testStartKey()) + 30;
        layout = FormationScreenLayout.compute(width, height,
                waiting ? 0 : model.snapshot().factions().size(), model.joined(),
                model.admin().visible(), detailPage, waiting, listNeed, joinWidth,
                testKeyWidth);
        actionKey = UiRect.EMPTY;
        actionBar = UiRect.EMPTY;
        switch (layout.mode()) {
            case WAITING -> addRetryKey();
            case WIDE -> {
                addFactionKeys(model);
                addJoinKey(model, layout.joinKey(), true);
                addList(model, metrics);
                addAdminKey(model);
                addTestStartKey(model);
                addVoteKey(model, layout.detailAction(), layout.detailAction().left());
            }
            case NARROW_LIST -> {
                addFactionKeys(model);
                addList(model, metrics);
                addAdminKey(model);
                addTestStartKey(model);
                addDetailsKey(model);
                if (model.joined()) {
                    addVoteKey(model, layout.actionBar(), layout.detailsKey().right() + 4);
                } else {
                    addJoinKey(model, layout.mainKey(), false);
                }
            }
            case NARROW_DETAIL -> {
                addRenderableWidget(role(BattleUiButton.builder(
                                FormationText.listKey(), ignored -> {
                            detailPage = false;
                            requestRebuild();
                        }).icon(TacticalIcon.BACK)
                        .bounds(layout.crumbBack().left(), layout.crumbBack().top(),
                                layout.crumbBack().width(), layout.crumbBack().height())
                        .build(), BACK_UI_ID));
                addVoteKey(model, layout.detailAction(), layout.detailAction().left());
            }
        }
        if (model.joined()) {
            // The bottom bezel comes last, so plain Tab reaches it after every control of the page.
            TacticalTabStrip strip = BattleTab.strip(BattleTab.FORMATION,
                    tab -> tab == BattleTab.FORMATION || model.hasFormation() ? null
                            : FormationText.tabLockedReason(), this::navigate);
            addRenderableWidget(strip);
            roles.put(strip, TABS_ROLE);
            setTabStrip(strip);
            TacticalBoardChrome.placeBezel(font, shellLayout(), strip, bezelHints());
        }
    }

    /** Tags {@code widget} with its probe id and remembers it as that page control. */
    private <T extends GuiEventListener> T role(T widget, String uiId) {
        roles.put(widget, uiId);
        return UiLayoutProbe.tag(widget, uiId);
    }

    private void addFactionKeys(FormationVoteModel model) {
        List<FactionSelectionView> factions = model.snapshot().factions();
        for (int index = 0; index < factions.size() && index < layout.factionKeys().size();
             index++) {
            FactionSelectionView faction = factions.get(index);
            UiRect rect = layout.factionKeys().get(index);
            FormationFactionButton key = new FormationFactionButton(rect.left(), rect.top(),
                    rect.width(), rect.height(), Component.literal(faction.displayName()),
                    FormationText.factionBadge(model, faction), model.factionLook(faction),
                    ignored -> browse(faction.id()));
            key.setTooltip(Tooltip.create(FormationText.factionTooltip(model, faction)));
            addRenderableWidget(role(key, FACTION_UI_ID_PREFIX + faction.id()));
        }
    }

    private void addJoinKey(FormationVoteModel model, UiRect rect, boolean wide) {
        if (rect.isEmpty() || model.joined()) {
            return;
        }
        FormationVoteModel.JoinAction join = model.joinAction();
        List<Component> labels = FormationText.joinLabel(model);
        Component label = FormationDetailPanel.pick(font, labels, Math.max(0,
                rect.width() - 20));
        Button key = BattleUiButton.builder(label, ignored -> confirmJoin())
                .kind(BattleUiButton.Kind.SUCCESS)
                .icon(join.enabled() ? TacticalIcon.CHECK : TacticalIcon.LOCK)
                .bounds(rect.left(), rect.top(), rect.width(), rect.height()).build();
        key.active = join.enabled();
        key.setTooltip(Tooltip.create(join.enabled() ? labels.get(0)
                : FormationText.step(model).get(0)));
        addRenderableWidget(role(key, JOIN_UI_ID));
    }

    private void addList(FormationVoteModel model, TacticalShellLayout.Metrics metrics) {
        list.update(model, metrics, layout.well());
        addRenderableWidget(role(list.widget(), LIST_UI_ID));
    }

    private void addAdminKey(FormationVoteModel model) {
        FormationVoteModel.AdminState admin = model.admin();
        if (!admin.visible() || layout.adminKey().isEmpty()) {
            return;
        }
        UiRect rect = layout.adminKey();
        Button key;
        if (admin.opening()) {
            key = role(BattleUiButton.builder(FormationText.adminOpenKey(),
                            ignored -> openVote())
                    .icon(TacticalIcon.UNLOCK)
                    .bounds(rect.left(), rect.top(), rect.width(), rect.height()).build(),
                    ADMIN_OPEN_UI_ID);
            key.active = admin.openEnabled();
        } else {
            key = role(BattleUiButton.builder(FormationText.adminLockKey(),
                            ignored -> confirmLock())
                    .kind(BattleUiButton.Kind.DANGER).icon(TacticalIcon.LOCK)
                    .bounds(rect.left(), rect.top(), rect.width(), rect.height()).build(),
                    ADMIN_LOCK_UI_ID);
            key.active = admin.lockEnabled();
            if (!admin.lockEnabled()) {
                key.setTooltip(Tooltip.create(FormationText.adminDetail(model).get(0)));
            }
        }
        addRenderableWidget(key);
    }

    /**
     * Administrator test-start key (0.4.0-beta.2): an adjustable (orange) control beside the
     * vote key, or alone in the compact administrator area before joining or after the lock.
     */
    private void addTestStartKey(FormationVoteModel model) {
        FormationVoteModel.TestStart test = model.testStart();
        if (!test.visible() || layout.testKey().isEmpty()) {
            return;
        }
        UiRect rect = layout.testKey();
        Button key = BattleUiButton.builder(FormationText.testStartKey(),
                        ignored -> confirmTestStart())
                .kind(BattleUiButton.Kind.CONTROL).icon(TacticalIcon.WRENCH)
                .bounds(rect.left(), rect.top(), rect.width(), rect.height()).build();
        key.active = test.enabled();
        key.setTooltip(Tooltip.create(FormationText.testStartTooltip(model)));
        addRenderableWidget(role(key, TEST_START_UI_ID));
    }

    private void addDetailsKey(FormationVoteModel model) {
        UiRect rect = layout.detailsKey();
        if (rect.isEmpty()) {
            return;
        }
        Button key = BattleUiButton.builder(FormationText.detailsKey(), ignored -> {
                    detailPage = true;
                    requestRebuild();
                }).icon(TacticalIcon.EYE)
                .bounds(rect.left(), rect.top(), rect.width(), rect.height()).build();
        key.active = model.highlighted() != null;
        addRenderableWidget(role(key, DETAILS_UI_ID));
    }

    /** Vote key at the right of {@code bar}; the reason line takes the room left of it. */
    private void addVoteKey(FormationVoteModel model, UiRect bar, int left) {
        if (bar.isEmpty() || model.highlighted() == null) {
            return;
        }
        FormationVoteModel.VoteAction action = model.voteAction();
        Component label = FormationText.voteLabel(action);
        boolean icon = action.buttonIcon() != null || action.mine();
        int minimum = layout.shell().metrics().tight() ? 84 : 104;
        int width = Math.min(Math.max(font.width(label) + (icon ? 30 : 18), minimum),
                (int) Math.floor((bar.right() - left) * 0.6D));
        actionBar = bar;
        reasonLeft = left;
        actionKey = new UiRect(bar.right() - width, bar.top(), bar.right(), bar.bottom());
        if (action.mine()) {
            return;
        }
        BattleUiButton.Builder builder = BattleUiButton.builder(label, ignored -> onVoteKey())
                .kind(action.label() == FormationVoteModel.VoteLabel.DEPLOY
                        ? BattleUiButton.Kind.NORMAL : BattleUiButton.Kind.SUCCESS);
        if (action.buttonIcon() != null) {
            builder.icon(action.buttonIcon());
        }
        builder.bounds(actionKey.left(), actionKey.top(), actionKey.width(), actionKey.height());
        Button key = builder.build();
        key.active = action.enabled();
        if (!action.enabled()) {
            // The reason is written beside the key; hovering the key says it too.
            key.setTooltip(Tooltip.create(FormationText.reason(model, action).get(0)));
        }
        addRenderableWidget(role(key, VOTE_UI_ID));
    }

    private void addRetryKey() {
        UiRect retry = waitingGeometry().retry();
        addRenderableWidget(role(BattleUiButton.builder(FormationText.retryKey(),
                        ignored -> retry())
                .kind(BattleUiButton.Kind.CONTROL).icon(TacticalIcon.REFRESH)
                .bounds(retry.left(), retry.top(), retry.width(), retry.height()).build(),
                RETRY_UI_ID));
    }

    // ---- actions ------------------------------------------------------------------------------------

    private void browse(String factionId) {
        FormationVoteModel model = model();
        FactionSelectionView faction = model.snapshot() == null ? null
                : model.snapshot().faction(factionId);
        if (faction == null || !model.factionClickable(faction)
                || factionId.equals(browseFactionId)) {
            return;
        }
        browseFactionId = factionId;
        highlightedFormationId = FormationVoteModel.initialHighlight(snapshot, faction);
        detail.reset();
        requestRebuild();
    }

    private void highlight(FormationSelectionView formation) {
        if (formation == null || formation.id().equals(highlightedFormationId)) {
            return;
        }
        highlightedFormationId = formation.id();
        requestRebuild();
    }

    private void confirmJoin() {
        FormationVoteModel model = model();
        FactionSelectionView faction = model.browsing();
        if (faction == null || model.joined() || !model.joinAction().enabled()) {
            return;
        }
        String factionId = faction.id();
        TacticalConfirmDialog dialog = TacticalConfirmDialog.builder(
                        FormationText.joinConfirmTitle(faction),
                        FormationText.joinConfirmBody(model, faction))
                .confirmLabel(FormationText.joinConfirmOk())
                .cancelLabel(FormationText.joinConfirmCancel())
                .onConfirm(() -> {
                    if (snapshot == null) {
                        return;
                    }
                    FormationClientNetworkBridge.selectFaction(snapshot.generation(), factionId);
                    ClientFormationState.feedback(ClientFormationState.FeedbackKind.PENDING,
                            FormationText.pendingJoin(faction));
                })
                .build();
        openDialog = new OpenDialog(DialogKind.JOIN, factionId, "", dialog);
        openModal(dialog);
    }

    private void onVoteKey() {
        FormationVoteModel model = model();
        FormationVoteModel.VoteAction action = model.voteAction();
        if (action.label() == FormationVoteModel.VoteLabel.DEPLOY && action.enabled()) {
            if (minecraft != null) {
                minecraft.setScreen(returnScreen);
            }
            BattleClientNetworkBridge.openDeploymentScreen();
            return;
        }
        castVote();
    }

    /** Casts the vote for the highlighted formation when its key is live (Enter does the same). */
    private void castVote() {
        FormationVoteModel model = model();
        FormationSelectionView formation = model.highlighted();
        if (formation == null || snapshot == null || !model.enterVotes()) {
            return;
        }
        FormationClientNetworkBridge.vote(snapshot.generation(), formation.id());
        ClientFormationState.feedback(ClientFormationState.FeedbackKind.PENDING,
                FormationText.pendingVote(formation));
    }

    private void openVote() {
        FormationVoteModel model = model();
        FactionSelectionView own = model.joinedFaction();
        if (own == null || !model.admin().openEnabled() || !sendCommand(
                administratorOpenVoteCommand(own.id()))) {
            ClientFormationState.feedback(false, FormationText.cannotManage());
            return;
        }
        ClientFormationState.feedback(ClientFormationState.FeedbackKind.PENDING,
                FormationText.pendingOpen());
    }

    private void confirmLock() {
        FormationVoteModel model = model();
        FactionSelectionView own = model.joinedFaction();
        FormationSelectionView target = model.highlighted();
        if (own == null || target == null || !model.admin().lockEnabled()) {
            ClientFormationState.feedback(false, FormationText.cannotLock());
            return;
        }
        String factionId = own.id();
        String formationId = target.id();
        TacticalConfirmDialog dialog = TacticalConfirmDialog.builder(
                        FormationText.lockConfirmTitle(own),
                        FormationText.lockConfirmBody(model, target))
                .danger(true)
                .confirmLabel(FormationText.lockConfirmOk())
                .onConfirm(() -> {
                    if (!sendCommand(administratorLockCommand(factionId, formationId))) {
                        ClientFormationState.feedback(false, FormationText.cannotManage());
                        return;
                    }
                    ClientFormationState.feedback(ClientFormationState.FeedbackKind.PENDING,
                            FormationText.pendingLock(target));
                })
                .build();
        openDialog = new OpenDialog(DialogKind.LOCK, factionId, formationId, dialog);
        openModal(dialog);
    }

    /**
     * Administrator test start (0.4.0-beta.2): an ordinary confirmation naming the faction and
     * formation, then {@code battle admin test start <faction> <formation>} as a chat command;
     * the server checks the permission, reports every step in chat and answers in the footer.
     * A successful start deploys the administrator, which closes this page.
     */
    private void confirmTestStart() {
        FormationVoteModel model = model();
        FormationVoteModel.TestStart test = model.testStart();
        if (!test.enabled()) {
            ClientFormationState.feedback(false, FormationText.cannotTestStart());
            return;
        }
        String factionId = test.factionId();
        String formationId = test.formationId();
        String pending = FormationText.pendingTestStart(model);
        TacticalConfirmDialog dialog = TacticalConfirmDialog.builder(
                        FormationText.testConfirmTitle(), FormationText.testConfirmBody(model))
                .confirmLabel(FormationText.testConfirmOk())
                .onConfirm(() -> {
                    if (!sendCommand(administratorTestStartCommand(factionId, formationId))) {
                        ClientFormationState.feedback(false, FormationText.cannotTestStart());
                        return;
                    }
                    awaitingTestStart = true;
                    ClientFormationState.feedback(ClientFormationState.FeedbackKind.PENDING,
                            pending);
                })
                .build();
        openDialog = new OpenDialog(DialogKind.TEST, factionId, formationId, dialog);
        openModal(dialog);
    }

    /**
     * A newer catalog arrived while the join or lock confirmation is open: the dialog was written
     * from the old one (population, votes, the leader). It is rewritten from the new catalog, or
     * closed with a receipt when its target can no longer be joined or locked, so nobody confirms
     * a decision on stale numbers.
     */
    private void refreshOpenDialog() {
        OpenDialog open = openDialog;
        if (open == null) {
            return;
        }
        if (modal() != open.dialog() || open.dialog().closed()) {
            openDialog = null;
            return;
        }
        FormationVoteModel model = model();
        if (open.kind() == DialogKind.TEST) {
            // The test start names what the page shows; it is rewritten while that still
            // applies (a teammate's vote, the lock) and closed silently when it no longer does.
            FormationVoteModel.TestStart test = model.testStart();
            if (test.enabled() && test.factionId().equals(open.factionId())) {
                open.dialog().updateBody(FormationText.testConfirmBody(model));
                return;
            }
            openDialog = null;
            open.dialog().dismiss();
            return;
        }
        boolean join = open.kind() == DialogKind.JOIN;
        FormationVoteModel.DialogFate fate = join ? model.joinDialogFate(open.factionId())
                : model.lockDialogFate(open.factionId(), open.formationId());
        if (fate == FormationVoteModel.DialogFate.KEEP) {
            open.dialog().updateBody(join ? FormationText.joinConfirmBody(model, model.browsing())
                    : FormationText.lockConfirmBody(model, model.highlighted()));
            return;
        }
        openDialog = null;
        open.dialog().dismiss();
        if (fate == FormationVoteModel.DialogFate.CLOSE_WITH_NOTICE) {
            ClientFormationState.feedback(false, join ? FormationText.joinDialogClosed()
                    : FormationText.lockDialogClosed());
        }
    }

    /** Administrator actions stay chat commands: the command tree checks the permission. */
    private boolean sendCommand(String command) {
        if (!isAdministrator() || minecraft.player.connection == null) {
            return false;
        }
        minecraft.player.connection.sendCommand(command);
        return true;
    }

    private void retry() {
        FormationClientNetworkBridge.requestCatalog();
        ClientFormationState.feedback(ClientFormationState.FeedbackKind.PENDING,
                net.minecraft.client.resources.language.I18n.get(
                        FormationText.PREFIX + "feedback.retrying"));
    }

    private void navigate(BattleTab tab) {
        if (tab == null || tab == BattleTab.FORMATION) {
            return;
        }
        if (!BattleTerminalNav.navigate(this, tab) && tab == BattleTab.CLASSES) {
            BattleTerminalNav.show(new SquadScreen(returnScreen));
        }
    }

    private void requestRebuild() {
        rebuildPending = true;
    }

    /**
     * Rebuilds the widgets and gives the focus back to the same page control (found by its
     * role): the list always (its scroll-bar drag continues), a key while the keyboard drives the
     * page, and the control that opened an open modal (so closing it still returns the focus).
     */
    private void rebuildKeepingFocus() {
        boolean modalOpen = hasModal();
        GuiEventListener focused = modalOpen ? parkedFocus() : getFocused();
        String role = focused == null ? null : roles.get(focused);
        boolean keyboard = minecraft != null && minecraft.getLastInputType().isKeyboard();
        rebuildWidgets();
        GuiEventListener restored = focusTarget(role, LIST_UI_ID.equals(role) || keyboard
                || modalOpen);
        if (restored == null) {
            return;
        }
        if (modalOpen) {
            reparkFocus(restored);
        } else {
            setFocused(restored);
        }
    }

    /** The widget of this build playing {@code role}, or {@code null}. */
    private GuiEventListener focusTarget(String role, boolean wanted) {
        if (role == null || !wanted) {
            return null;
        }
        for (Map.Entry<GuiEventListener, String> entry : roles.entrySet()) {
            GuiEventListener widget = entry.getKey();
            if (role.equals(entry.getValue()) && children().contains(widget)
                    && !(widget instanceof AbstractWidget control && !control.active)) {
                return widget;
            }
        }
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        if (rebuildPending && minecraft != null) {
            rebuildKeepingFocus();
        }
    }

    // ---- rendering ----------------------------------------------------------------------------------

    @Override
    protected void renderTactical(GuiGraphics graphics, int mouseX, int mouseY,
                                  float partialTick) {
        if (layout == null) {
            renderWidgets(graphics, mouseX, mouseY, partialTick);
            return;
        }
        FormationVoteModel model = model();
        TacticalBoardChrome.ShellSpec spec = TacticalBoardChrome.ShellSpec
                .of(FormationText.title(width))
                .withIdentity(FormationText.identity(model, ClientBattleState.snapshot()))
                .withLink(model.waiting() ? TacticalBoardChrome.LinkState.WAIT
                        : TacticalBoardChrome.LinkState.OK)
                .withTabs(tabStrip())
                .withHints(hints(model))
                .withFeedback(footer(model));
        drawShell(graphics, spec);
        switch (layout.mode()) {
            case WAITING -> {
                region(graphics, WAITING_BOX, layout.waitingPanel());
                renderWaiting(graphics, model);
                UiLayoutProbe.end(graphics);
            }
            case WIDE -> {
                region(graphics, STRIP_BOX, layout.strip());
                renderStrip(graphics, model);
                UiLayoutProbe.end(graphics);
                region(graphics, LIST_BOX, layout.listPanel());
                renderListPanel(graphics, model, false);
                UiLayoutProbe.end(graphics);
                region(graphics, DETAIL_BOX, layout.detailPanel());
                detail.render(graphics, font, layout, model);
                renderAction(graphics, model);
                UiLayoutProbe.end(graphics);
            }
            case NARROW_LIST -> {
                region(graphics, LIST_BOX, layout.listPanel());
                renderListPanel(graphics, model, true);
                if (model.joined()) {
                    renderAction(graphics, model);
                }
                UiLayoutProbe.end(graphics);
            }
            case NARROW_DETAIL -> {
                region(graphics, CRUMB_BOX, layout.crumb());
                renderCrumb(graphics, model, mouseX, mouseY);
                UiLayoutProbe.end(graphics);
                region(graphics, DETAIL_BOX, layout.detailPanel());
                detail.render(graphics, font, layout, model);
                renderAction(graphics, model);
                UiLayoutProbe.end(graphics);
            }
        }
        renderWidgets(graphics, mouseX, mouseY, partialTick);
    }

    /** Opens the layout-probe box of a page region (no-op outside the uiTest probe). */
    private static void region(GuiGraphics graphics, String id, UiRect rect) {
        UiLayoutProbe.begin(graphics, id, rect.left(), rect.top(), rect.right(), rect.bottom(),
                true);
    }

    private List<TacticalBoardChrome.KeyHint> hints(FormationVoteModel model) {
        Component terminalKey = ClientBootstrap.keyLabel(KeyBindingDefaults.Binding.TERMINAL);
        List<TacticalBoardChrome.KeyHint> hints = new ArrayList<>();
        Component esc = FormationText.escClose(!unjoined(model), terminalKey);
        if (layout.mode() == FormationScreenLayout.Mode.WAITING) {
            hints.add(TacticalBoardChrome.KeyHint.literal("R", FormationText.hintRetry()));
            hints.add(TacticalBoardChrome.KeyHint.literal("Esc", esc));
            return hints;
        }
        boolean detailView = layout.mode() == FormationScreenLayout.Mode.NARROW_DETAIL;
        hints.add(TacticalBoardChrome.KeyHint.literal("Esc",
                detailView ? FormationText.hintBackToList() : esc));
        if (model.enterVotes()) {
            hints.add(TacticalBoardChrome.KeyHint.literal("Enter",
                    FormationText.voteLabel(model.voteAction())));
        }
        hints.add(TacticalBoardChrome.KeyHint.of(FormationText.hintWheel(),
                detailView ? FormationText.hintScroll() : FormationText.hintBrowse()));
        return hints;
    }

    /**
     * Footer receipt: the 3-second server/pending receipt, otherwise the next step, in the neutral
     * guide colour like the HUD's waiting and to-do plates (orange is for sections and controls).
     */
    private TacticalBoardChrome.Feedback footer(FormationVoteModel model) {
        TacticalBoardChrome.Feedback receipt = TacticalBoardChrome.Feedback.fromFormation();
        if (receipt != null) {
            return receipt;
        }
        UiRect footer = layout.shell().footer();
        int room = (int) Math.floor(footer.width() * (layout.shell().tight() ? 0.62D : 0.5D))
                - 16;
        return TacticalBoardChrome.Feedback.guide(FormationDetailPanel.pick(font,
                FormationText.step(model), room));
    }

    private void renderStrip(GuiGraphics graphics, FormationVoteModel model) {
        UiRect area = layout.status();
        if (area.width() < 24) {
            return;
        }
        int textY = area.top() + Math.floorDiv(area.height() - 8, 2);
        if (!model.joined()) {
            Component note = FormationDetailPanel.pick(font, FormationText.joinNote(model),
                    area.width());
            TextFit.draw(graphics, font, note, area.left(), textY, area.width(),
                    TacticalBoardTheme.TEXT, TextFit.Align.RIGHT);
            return;
        }
        statusLine(graphics, area, textY, FormationText.statusLed(model),
                FormationText.joinedStatus(model));
    }

    /** LED + main + muted sub, right-aligned; variants are tried from long to short. */
    private void statusLine(GuiGraphics graphics, UiRect area, int textY, int led,
                            List<Component[]> variants) {
        if (variants.isEmpty()) {
            return;
        }
        Component[] chosen = variants.get(variants.size() - 1);
        for (Component[] variant : variants) {
            if (7 + font.width(variant[0]) + 8 + font.width(variant[1]) <= area.width()) {
                chosen = variant;
                break;
            }
        }
        int needed = 7 + font.width(chosen[0]) + 8 + font.width(chosen[1]);
        int x = area.right() - Math.min(needed, area.width());
        TacticalDraw.led(graphics, x, textY + 2, led);
        TextFit.Fitted main = TextFit.draw(graphics, font, chosen[0], x + 7, textY,
                Math.max(0, area.right() - x - 7), TacticalBoardTheme.TEXT, TextFit.Align.LEFT);
        int subX = x + 7 + main.width() + 8;
        if (area.right() - subX > 24) {
            TextFit.draw(graphics, font, chosen[1], subX, textY, area.right() - subX,
                    TacticalBoardTheme.MUTED, TextFit.Align.LEFT);
        }
    }

    private void renderListPanel(GuiGraphics graphics, FormationVoteModel model, boolean narrow) {
        TacticalShellLayout.Metrics metrics = layout.shell().metrics();
        TacticalDraw.panel(graphics, font, layout.listPanel(), metrics,
                TacticalDraw.PanelStyle.titled(FormationText.listTitle(model, narrow))
                        .withMeta(FormationText.listMeta(model, narrow)));
        if (!layout.summary().isEmpty()) {
            renderSummary(graphics, model, layout.summary());
        }
        if (!layout.preview().isEmpty() && model.highlighted() != null) {
            UiRect preview = layout.preview();
            FormationDetailPanel.identity(graphics, font, preview, model, model.highlighted(),
                    metrics);
            int y = preview.top() + FormationScreenLayout.emblemSize(metrics) + 4;
            int lines = (preview.bottom() - y + 2) / TacticalDraw.LINE_HEIGHT;
            if (lines > 0) {
                TacticalDraw.paragraph(graphics, font, model.highlighted().description(),
                        preview.left(), y, preview.width(), TacticalBoardTheme.MUTED, lines);
            }
        }
        if (!layout.admin().isEmpty()) {
            renderAdmin(graphics, model, layout.admin());
        }
    }

    private void renderSummary(GuiGraphics graphics, FormationVoteModel model, UiRect area) {
        graphics.fill(area.left(), area.top() + 1, area.left() + 2, area.top() + 8,
                TacticalBoardTheme.SECTION);
        TextFit.draw(graphics, font, FormationText.summaryTitle(model), area.left() + 5,
                area.top(), area.width() - 5, TacticalBoardTheme.TEXT, TextFit.Align.LEFT);
        TacticalDraw.divider(graphics, area.left(), area.right(), area.top() + 10);
        int y = area.top() + 14;
        for (FormationText.SummaryRow row : FormationText.summary(model)) {
            if (y + 8 > area.bottom()) {
                break;
            }
            if (row.key() == null) {
                int lines = Math.min(3, Math.max(1, (area.bottom() - y + 2) / 10));
                y += TacticalDraw.paragraph(graphics, font, row.value().getString(), area.left(),
                        y, area.width(), TacticalBoardTheme.MUTED, lines) * 10;
                continue;
            }
            if (row.key().getString().isEmpty()) {
                TextFit.draw(graphics, font, row.value(), area.left(), y, area.width(),
                        row.color() == 0 ? TacticalBoardTheme.TEXT : row.color(),
                        TextFit.Align.RIGHT);
            } else {
                TacticalDraw.kv(graphics, font, area.left(), y, area.width(), row.key(),
                        row.value(), row.color());
            }
            y += 10;
        }
    }

    private void renderAdmin(GuiGraphics graphics, FormationVoteModel model, UiRect area) {
        TacticalDraw.divider(graphics, area.left(), area.right(), area.top());
        int y1 = area.top() + 3;
        int y2 = y1 + 11;
        graphics.fill(area.left(), y1 + 1, area.left() + 2, y1 + 8, TacticalBoardTheme.SECTION);
        TextFit.draw(graphics, font, FormationText.adminTitle(), area.left() + 5, y1, 40,
                TacticalBoardTheme.TEXT, TextFit.Align.LEFT);
        if (!model.admin().visible()) {
            // Compact area (not joined yet, or locked): only the test-start key and its target.
            FormationDetailPanel.drawVariant(graphics, font, FormationText.testStartHeadline(model),
                    area.left() + 46, y1, area.width() - 46, TacticalBoardTheme.MUTED);
            return;
        }
        FormationDetailPanel.drawVariant(graphics, font, FormationText.adminHeadline(model),
                area.left() + 46, y1, area.width() - 46, TacticalBoardTheme.MUTED);
        FormationVoteModel.AdminState admin = model.admin();
        List<Component> detailText = FormationText.adminDetail(model);
        if (admin.opening()) {
            FormationDetailPanel.drawReason(graphics, font, area.left(), y2, area.width(),
                    TacticalIcon.INFO, detailText, TacticalBoardTheme.MUTED,
                    TacticalBoardTheme.MUTED);
        } else if (admin.lockBlock() != FormationVoteModel.LockBlock.NONE) {
            FormationDetailPanel.drawReason(graphics, font, area.left(), y2, area.width(),
                    TacticalIcon.LOCK, detailText, TacticalBoardTheme.TEXT,
                    TacticalBoardTheme.MUTED);
        } else if (admin.relation() == FormationVoteModel.AdminRelation.SOLE_LEADER) {
            FormationDetailPanel.drawReason(graphics, font, area.left(), y2, area.width(),
                    TacticalIcon.CHECK, detailText, TacticalBoardTheme.MUTED,
                    TacticalBoardTheme.SUCCESS);
        } else {
            // Orange attention strip: locking something other than the sole leader.
            graphics.fill(area.left(), y2 - 2, area.right(), y2 + 9,
                    TacticalHud.withAlpha(TacticalBoardTheme.ACCENT, 0x40));
            graphics.fill(area.left(), y2 - 2, area.left() + 2, y2 + 9,
                    TacticalBoardTheme.ACCENT);
            FormationDetailPanel.drawReason(graphics, font, area.left() + 4, y2,
                    area.width() - 6, TacticalIcon.WARN, detailText, TacticalBoardTheme.TEXT,
                    TacticalBoardTheme.ACCENT);
        }
    }

    private void renderAction(GuiGraphics graphics, FormationVoteModel model) {
        if (actionKey.isEmpty() || model.highlighted() == null) {
            return;
        }
        FormationVoteModel.VoteAction action = model.voteAction();
        if (action.mine()) {
            FormationDetailPanel.drawMineBadge(graphics, font, actionKey,
                    FormationText.voteLabel(action));
        }
        int textY = actionBar.top() + Math.floorDiv(actionBar.height() - 8, 2);
        int reasonWidth = actionKey.left() - 6 - (reasonLeft + 2);
        FormationDetailPanel.drawReason(graphics, font, reasonLeft + 2, textY, reasonWidth,
                action.icon(), FormationText.reason(model, action),
                action.enabled() || action.mine() ? TacticalBoardTheme.MUTED
                        : TacticalBoardTheme.TEXT, TacticalBoardTheme.MUTED);
        if (!action.enabled() && !action.mine() && reasonWidth >= 24) {
            // A shortened reason is offered in full by the disabled vote key's tooltip.
            UiLayoutProbe.tipped(graphics, reasonLeft + 2 + 12, textY);
        }
    }

    private void renderCrumb(GuiGraphics graphics, FormationVoteModel model, int mouseX,
                             int mouseY) {
        FormationSelectionView formation = model.highlighted();
        if (formation == null) {
            return;
        }
        UiRect text = layout.crumbText();
        TextFit.draw(graphics, font, FormationText.crumb(model, formation), text.left(),
                text.top() + Math.floorDiv(text.height() - 8, 2), text.width(),
                TacticalBoardTheme.MUTED, TextFit.Align.LEFT);
        List<FormationSelectionView> ordered =
                FormationVoteModel.orderedFormations(model.browsing());
        TacticalDraw.pager(graphics, font, layout.crumbPager(), ordered.indexOf(formation),
                ordered.size(), TacticalDraw.pagerHit(layout.crumbPager(), mouseX, mouseY));
    }

    private record WaitingGeometry(UiRect content, UiRect empty, UiRect retry) {
    }

    private WaitingGeometry waitingGeometry() {
        TacticalShellLayout.Metrics metrics = layout.shell().metrics();
        UiRect c = TacticalDraw.panelContent(layout.waitingPanel(), metrics,
                TacticalDraw.PanelStyle.titled(FormationText.waitingTitle()));
        boolean empty = model().stage() == FormationVoteModel.Stage.EMPTY;
        int emptyWidth = Math.max(40, Math.min(c.width() - 24, 300));
        // Room for the longer waiting hint, so the retry key keeps its place when the hint turns
        // into the "no answer" variant after the timeout.
        int lines = empty
                ? TextFit.wrap(font, FormationText.emptyHint().getString(), emptyWidth - 12, 0)
                .size()
                : Math.max(TextFit.wrap(font, FormationText.waitingHint(false).getString(),
                        emptyWidth - 12, 0).size(),
                TextFit.wrap(font, FormationText.waitingHint(true).getString(),
                        emptyWidth - 12, 0).size());
        int blockHeight = 22 + lines * 10 + 8;
        int total = blockHeight + 6 + metrics.buttonHeight();
        int top = c.top() + Math.max(2, (c.height() - total) / 2);
        int emptyLeft = c.left() + (c.width() - emptyWidth) / 2;
        int retryWidth = Math.min(RETRY_KEY_WIDTH, c.width());
        int retryLeft = c.left() + (c.width() - retryWidth) / 2;
        int retryTop = Math.min(c.bottom() - metrics.buttonHeight(), top + blockHeight + 6);
        return new WaitingGeometry(c, new UiRect(emptyLeft, top - 4, emptyLeft + emptyWidth,
                Math.min(retryTop - 2, top + blockHeight - 4)),
                new UiRect(retryLeft, retryTop, retryLeft + retryWidth,
                        retryTop + metrics.buttonHeight()));
    }

    private void renderWaiting(GuiGraphics graphics, FormationVoteModel model) {
        boolean empty = model.stage() == FormationVoteModel.Stage.EMPTY;
        TacticalDraw.panel(graphics, font, layout.waitingPanel(), layout.shell().metrics(),
                TacticalDraw.PanelStyle.titled(FormationText.waitingTitle())
                        .withMeta(empty ? Component.empty() : FormationText.waitingMeta(),
                                TacticalBoardTheme.ACCENT_B));
        WaitingGeometry geometry = waitingGeometry();
        TacticalDraw.well(graphics, geometry.content());
        TacticalDraw.empty(graphics, font, geometry.empty(),
                empty ? TacticalIcon.INFO : TacticalIcon.REFRESH,
                empty ? FormationText.emptyHeading() : FormationText.waitingHeading(),
                empty ? FormationText.emptyHint() : FormationText.waitingHint(), false);
    }

    // ---- input --------------------------------------------------------------------------------------

    @Override
    protected boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (layout != null && layout.mode() == FormationScreenLayout.Mode.NARROW_DETAIL
                    && FormationVoteModel.escAction(true)
                    == FormationVoteModel.EscAction.BACK_TO_LIST) {
                detailPage = false;
                requestRebuild();
                return true;
            }
            onClose();
            return true;
        }
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
                && (getFocused() == null || getFocused() == list.widget())
                && model().enterVotes()) {
            castVote();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_R && layout != null
                && layout.mode() == FormationScreenLayout.Mode.WAITING) {
            retry();
            return true;
        }
        return super.onKeyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected boolean onMouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && layout != null
                && layout.mode() == FormationScreenLayout.Mode.NARROW_DETAIL) {
            int step = TacticalDraw.pagerHit(layout.crumbPager(), mouseX, mouseY);
            if (step != 0) {
                page(step);
                return true;
            }
        }
        return super.onMouseClicked(mouseX, mouseY, button);
    }

    private void page(int step) {
        FormationVoteModel model = model();
        List<FormationSelectionView> ordered =
                FormationVoteModel.orderedFormations(model.browsing());
        int index = ordered.indexOf(model.highlighted());
        int next = index + step;
        if (index >= 0 && next >= 0 && next < ordered.size()) {
            highlight(ordered.get(next));
        }
    }

    @Override
    protected boolean onMouseScrolled(double mouseX, double mouseY, double delta) {
        if (layout != null && layout.mode() != FormationScreenLayout.Mode.WAITING
                && layout.mode() != FormationScreenLayout.Mode.NARROW_LIST
                && detail.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        return super.onMouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    /**
     * Closes the page. Without a faction the action bar then says which key opens it again, so a
     * player who is not ready to join is never stuck on it (user report 4).
     */
    @Override
    public void onClose() {
        if (minecraft == null) {
            return;
        }
        boolean unjoined = unjoined(model());
        minecraft.setScreen(returnScreen);
        if (unjoined && minecraft.gui != null) {
            minecraft.gui.setOverlayMessage(FormationText.reopenNotice(
                    ClientBootstrap.keyLabel(KeyBindingDefaults.Binding.TERMINAL)), false);
        }
    }

    /**
     * Whether the viewer has no faction. While the catalog is still on its way the battle
     * snapshot decides, so a participant who opened the page from the squad terminal is not told
     * "关闭阵营选择" or shown the unjoined Esc hint.
     */
    private static boolean unjoined(FormationVoteModel model) {
        return model.waiting() ? !KeyBindingDefaults.inBattle(ClientBattleState.snapshot())
                : !model.joined();
    }
}
