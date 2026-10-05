package com.wok.infantry.client.screen;

import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.client.BattleClientActions;
import com.wok.infantry.client.ClientBattleState;
import com.wok.infantry.client.ClientBootstrap;
import com.wok.infantry.client.ClientFormationState;
import com.wok.infantry.client.KeyBindingDefaults;
import com.wok.infantry.client.ui.probe.UiLayoutProbe;
import com.wok.infantry.client.ui.probe.UiSurfaceInfo;
import com.wok.infantry.deployment.DeploymentPhase;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.formation.vote.FormationVotePhase;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * The battle terminal's squad, class and deployment pages (preview {@code 20-squad.js}, "new"):
 * one tactical-tablet screen with the shared shell, the six terminal tabs and three pages that
 * switch in place. The loadout, map and formation tabs go through {@link BattleTerminalNav} with
 * replace semantics, so one Esc always closes the whole terminal.
 *
 * <p>What every page shows and allows comes from {@link SquadBoardModel}, rebuilt from the latest
 * battle snapshot each frame; the widgets are rebuilt only when the snapshot's structure changes
 * (its generation), on a page or squad switch and on resize, so a teammate losing health or a
 * ticking countdown never moves the keyboard focus. Destructive or hand-over operations ask for a
 * confirmation inside the screen ({@link TacticalConfirmDialog}; kick, leave, disband and
 * redeploy are dangerous and never accept Enter). Intents go out through
 * {@link BattleClientActions} only; nothing is changed locally before the server answers, but a
 * sent operation waits ("等待回执") until the answer, a new snapshot or a 3-second timeout.
 *
 * <p>While the faction's formation is not locked every page shows the vote waiting block and no
 * control that creates or joins a squad, picks a class or deploys is enabled.
 */
public final class SquadScreen extends TacticalScreen
        implements BattleTerminalNav.Terminal, UiSurfaceInfo {
    /** Preview surface id ({@code ui-preview/surfaces/20-squad.js}). */
    public static final String SURFACE_ID = "squad";
    /** Probe id of the refresh key of the sync placeholder. */
    public static final String REFRESH_UI_ID = "squad.refresh";
    /** Probe id of the "open the formation page" key (vote block, no faction). */
    public static final String VOTE_UI_ID = "squad.vote.open";
    /** Probe ids of the operation keys are {@code squad.action.<action id>}. */
    public static final String ACTION_UI_ID_PREFIX = "squad.action.";

    private static final long PENDING_TIMEOUT_NANOS = 3_000_000_000L;
    private static final String TABS_ROLE = "squad.tabs";

    /** The three pages of this screen. */
    enum Page {
        SQUADS(BattleTab.SQUADS),
        CLASSES(BattleTab.CLASSES),
        DEPLOYMENT(BattleTab.DEPLOYMENT);

        final BattleTab tab;

        Page(BattleTab tab) {
            this.tab = tab;
        }

        static Page of(BattleTab tab) {
            for (Page page : values()) {
                if (page.tab == tab) {
                    return page;
                }
            }
            return null;
        }
    }

    /** What a page draws and how it handles the mouse beyond its widgets. */
    interface Painter {
        void render(GuiGraphics graphics, SquadBoardModel model, int mouseX, int mouseY);

        /** Drawn after the widgets (empty roster slots and the like). */
        default void renderOverlay(GuiGraphics graphics, SquadBoardModel model) {
        }

        default boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false;
        }

        default boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            return false;
        }
    }

    /** An operation key and how to find its current state in a fresh model. */
    private record ActionBinding(Button button, Function<SquadBoardModel,
            SquadBoardModel.ActionState> locator, Component label, boolean labelCut) {
    }

    /** The open confirmation and the operation it asks about. */
    private record OpenConfirm(TacticalConfirmDialog dialog, SquadBoardModel.ActionState state) {
    }

    private final Screen previous;
    private Page page;
    private SquadCallsign viewedSquad;
    private SquadCallsign observedOwnSquad;
    private boolean ownSquadObserved;
    private UUID selectedMember;
    private int classPage;
    private int deploymentPage;
    private long observedGeneration = Long.MIN_VALUE;
    private FormationSelectionSnapshot observedCatalog;
    private ClientBattleState.BattleFeedback observedFeedback;
    private boolean requestedSnapshot;
    private final EnumMap<SquadBoardModel.Action, Long> pending =
            new EnumMap<>(SquadBoardModel.Action.class);
    private SquadBoardModel model;
    private Painter painter;
    /** The stage the painter was built for ({@code null} before the first init). */
    private SquadBoardModel.Stage painterStage;
    private final List<ActionBinding> bindings = new ArrayList<>();
    private final Map<Button, Tooltip> tooltips = new IdentityHashMap<>();
    private final Map<Button, String> tooltipTexts = new IdentityHashMap<>();
    private final Map<GuiEventListener, String> roles = new IdentityHashMap<>();
    /** Roles of the previous build: a click that rebuilt the board finds its control again. */
    private Map<GuiEventListener, String> previousRoles = Map.of();
    /**
     * The model of the frame being drawn, shared by the widget callbacks of that frame (rows,
     * cells, tooltips); {@code null} outside rendering, where callers build a fresh one.
     */
    private SquadBoardModel frameModel;
    /** The confirmation that is open (also gives the probe state {@code kick}). */
    private OpenConfirm openConfirm;
    private long respawnRevision = Long.MIN_VALUE;
    private long respawnTotalTicks;

    public SquadScreen() {
        this(null, false);
    }

    public SquadScreen(Screen previous) {
        this(previous, false);
    }

    /** Used by the server after login/death to open directly on the deployment workflow. */
    public SquadScreen(Screen previous, boolean openDeployment) {
        this(previous, openDeployment ? Page.DEPLOYMENT : Page.SQUADS);
    }

    private SquadScreen(Screen previous, Page page) {
        super(Component.translatable("screen.wok_infantry.squad"));
        // A terminal screen as parent is replaced, never stacked: the whole terminal returns to
        // the screen that was open before it.
        this.previous = BattleTerminalNav.returnScreenFor(previous);
        this.page = page == null ? Page.SQUADS : page;
    }

    /** The terminal page of {@code tab} (squads, classes or deployment) for {@code root}. */
    public static SquadScreen forTab(Screen root, BattleTab tab) {
        Page page = Page.of(tab);
        return new SquadScreen(root, page == null ? Page.SQUADS : page);
    }

    @Override
    public Screen terminalReturnScreen() {
        return previous;
    }

    /** The page shown ({@link BattleTab#SQUADS}, {@link BattleTab#CLASSES} or {@link BattleTab#DEPLOYMENT}). */
    public BattleTab page() {
        return page.tab;
    }

    /**
     * Switches to the squad, class or deployment page in place (other tabs are ignored). Used by
     * the network bridge so a deployment request never opens a second terminal on top.
     */
    public void showPage(BattleTab tab) {
        Page target = Page.of(tab);
        if (target == null || target == page) {
            return;
        }
        page = target;
        if (minecraft != null) {
            rebuildKeepingFocus();
        }
    }

    // ---- state used by the painters ------------------------------------------------------------------

    Font boardFont() {
        return font;
    }

    TacticalShellLayout.Metrics boardMetrics() {
        return shellLayout().metrics();
    }

    /**
     * The model of the frame being drawn (built once per frame and shared by every row, cell and
     * tooltip of that frame), or a fresh one in event handlers.
     */
    SquadBoardModel liveModel() {
        SquadBoardModel frame = frameModel;
        return frame != null ? frame : buildModel();
    }

    SquadCallsign viewedSquad() {
        return viewedSquad;
    }

    UUID selectedMember() {
        return selectedMember;
    }

    int classPage() {
        return classPage;
    }

    int deploymentPage() {
        return deploymentPage;
    }

    void setClassPage(int value) {
        if (value != classPage) {
            classPage = Math.max(0, value);
            rebuildKeepingFocus();
        }
    }

    void setDeploymentPage(int value) {
        if (value != deploymentPage) {
            deploymentPage = Math.max(0, value);
            rebuildKeepingFocus();
        }
    }

    /** Shows the roster of {@code callsign} ({@code null}: the own squad) and clears the target. */
    void viewSquad(SquadCallsign callsign) {
        if (callsign == viewedSquad) {
            return;
        }
        viewedSquad = callsign;
        selectedMember = null;
        rebuildKeepingFocus();
    }

    /** Selects {@code member} as the operation target, or clears it when already selected. */
    void toggleMember(UUID member) {
        selectedMember = member == null || member.equals(selectedMember) ? null : member;
    }

    /** Adds {@code widget} to the board with its probe id. */
    <T extends AbstractWidget> T addBoardWidget(T widget, String uiId) {
        addRenderableWidget(widget);
        roles.put(widget, uiId);
        return UiLayoutProbe.tag(widget, uiId);
    }

    /**
     * Adds an operation key in {@code cell}: full label, or the short one when the full one does
     * not fit; an icon only when there is room for it. Its enabled state, tooltip (the disabled
     * reason, else what it does) follow {@code locator} on a fresh model every frame.
     */
    Button addActionButton(UiRect cell, SquadBoardModel.ActionState initial,
                           Function<SquadBoardModel, SquadBoardModel.ActionState> locator,
                           TacticalIcon icon) {
        SquadBoardModel.Action action = initial.action();
        int room = cell.width() - (action.danger() ? 10 : 8);
        Component full = initial.label();
        Component label = font.width(full) > room ? initial.shortLabel() : full;
        boolean withIcon = icon != null && room >= font.width(label) + TacticalIcon.ADVANCE;
        int labelRoom = cell.width() - 6 - (withIcon ? TacticalIcon.ADVANCE : 0);
        BattleUiButton.Builder builder = BattleUiButton.builder(label,
                        ignored -> perform(locator.apply(liveModel())))
                .kind(kind(action));
        if (withIcon) {
            builder.icon(icon);
        }
        Button button = builder.bounds(cell.left(), cell.top(), cell.width(), cell.height())
                .build();
        addBoardWidget(button, ACTION_UI_ID_PREFIX + action.id());
        ActionBinding binding = new ActionBinding(button, locator, full,
                font.width(label) > labelRoom || label != full);
        bindings.add(binding);
        sync(binding, model);
        return button;
    }

    private static BattleUiButton.Kind kind(SquadBoardModel.Action action) {
        if (action.danger()) {
            return BattleUiButton.Kind.DANGER;
        }
        return switch (action) {
            case CREATE_SQUAD, JOIN_SQUAD, DEPLOY, RESUPPLY -> BattleUiButton.Kind.SUCCESS;
            default -> BattleUiButton.Kind.NORMAL;
        };
    }

    /** A plain key ({@code reason} disables it and becomes its tooltip). */
    Button addKey(UiRect cell, Component label, TacticalIcon icon, Component reason,
                  Runnable onPress, String uiId) {
        BattleUiButton.Builder builder = BattleUiButton.builder(label, ignored -> onPress.run());
        if (icon != null && font.width(label) + TacticalIcon.ADVANCE <= cell.width() - 8) {
            builder.icon(icon);
        }
        Button button = builder.bounds(cell.left(), cell.top(), cell.width(), cell.height())
                .build();
        button.active = reason == null;
        if (reason != null) {
            button.setTooltip(Tooltip.create(font.width(label) > cell.width() - 6
                    ? Component.empty().append(label).append("\n").append(reason) : reason));
        } else if (font.width(label) > cell.width() - 6) {
            button.setTooltip(Tooltip.create(label));
        }
        return addBoardWidget(button, uiId);
    }

    private void sync(ActionBinding binding, SquadBoardModel current) {
        SquadBoardModel.ActionState state = current == null ? null
                : binding.locator().apply(current);
        Button button = binding.button();
        if (state == null) {
            button.active = false;
            setTooltip(button, SquadBoardModel.Reason.of(
                    SquadBoardModel.ReasonCode.PENDING).full(), binding);
            return;
        }
        button.active = state.enabled();
        SquadBoardModel.Reason text = state.explanation();
        setTooltip(button, text == null ? null : text.full(), binding);
    }

    private void setTooltip(Button button, Component text, ActionBinding binding) {
        Component shown = text;
        if (binding.labelCut()) {
            shown = text == null ? binding.label()
                    : Component.empty().append(binding.label()).append("\n").append(text);
        }
        String key = shown == null ? "" : shown.getString();
        if (key.equals(tooltipTexts.get(button))) {
            return;
        }
        tooltipTexts.put(button, key);
        Tooltip tooltip = shown == null ? null : Tooltip.create(shown);
        tooltips.put(button, tooltip);
        button.setTooltip(tooltip);
    }

    /** Runs {@code state} (after its confirmation, if it needs one). */
    void perform(SquadBoardModel.ActionState state) {
        if (state == null || !state.enabled()) {
            return;
        }
        if (state.action() == SquadBoardModel.Action.RETURN_TO_OWN_SQUAD) {
            viewSquad(null);
            return;
        }
        SquadBoardModel.Confirm confirm = state.confirm();
        if (confirm == null) {
            send(state);
            return;
        }
        TacticalConfirmDialog dialog = TacticalConfirmDialog.builder(confirm.title(),
                        confirm.joinedBody())
                .danger(confirm.danger())
                .confirmLabel(confirm.confirmLabel())
                .onConfirm(() -> confirmed(state))
                .onCancel(() -> openConfirm = null)
                .build();
        openConfirm = new OpenConfirm(dialog, state);
        openModal(dialog);
    }

    /**
     * The confirmation was accepted: sends the operation only if a model of the newest snapshot
     * still offers it for the same target, never the intent as it stood when the dialog opened.
     */
    private void confirmed(SquadBoardModel.ActionState asked) {
        openConfirm = null;
        SquadBoardModel.ActionState current = buildModel().stillOffered(asked);
        if (current == null) {
            staleNotice(asked);
            return;
        }
        send(current);
    }

    /**
     * After a newer snapshot: an open confirmation whose operation is still offered for the same
     * target gets the newest wording (a target that just deployed, a different successor); one
     * that no longer applies (the target left, the squad went into combat, the viewer lost the
     * leadership) closes without sending and says so in the footer.
     */
    private void revalidateConfirm() {
        OpenConfirm open = openConfirm;
        if (open == null) {
            return;
        }
        if (open.dialog().closed() || modal() != open.dialog()) {
            openConfirm = null;
            return;
        }
        SquadBoardModel.ActionState current = buildModel().stillOffered(open.state());
        if (current == null || current.confirm() == null) {
            openConfirm = null;
            open.dialog().dismiss();
            staleNotice(open.state());
            return;
        }
        open.dialog().updateBody(current.confirm().joinedBody());
    }

    private void staleNotice(SquadBoardModel.ActionState state) {
        ClientBattleState.showFeedback(false, SquadBoardText.t(SquadBoardText.CONFIRM_STALE,
                state.label()).getString());
        // A local notice is not a server answer: intents still waiting keep waiting.
        observedFeedback = ClientBattleState.feedback();
    }

    private void send(SquadBoardModel.ActionState state) {
        switch (state.action()) {
            case CREATE_SQUAD -> BattleClientActions.createSquad(state.squad());
            case JOIN_SQUAD -> BattleClientActions.joinSquad(state.squad());
            case RETURN_TO_OWN_SQUAD -> {
                viewSquad(null);
                return;
            }
            case LEAVE_SQUAD -> BattleClientActions.leaveSquad();
            case DISBAND_SQUAD -> BattleClientActions.disbandSquad();
            case KICK_MEMBER -> BattleClientActions.kickMember(state.target());
            case TRANSFER_LEADER -> BattleClientActions.transferLeadership(state.target());
            case CLAIM_COMMANDER -> BattleClientActions.claimCommander();
            case RESIGN_COMMANDER -> BattleClientActions.resignCommander();
            case TRANSFER_COMMANDER -> BattleClientActions.transferCommander(state.target());
            case SELECT_CLASS -> BattleClientActions.selectClass(state.classId());
            case SELECT_DEPLOYMENT_POINT -> BattleClientActions.selectDeploymentPoint(
                    state.pointId());
            case DEPLOY -> BattleClientActions.deploy();
            case REDEPLOY -> BattleClientActions.redeploy();
            case RESUPPLY -> BattleClientActions.resupply();
        }
        pending.put(state.action(), System.nanoTime());
    }

    /**
     * Elapsed share of the respawn countdown, from the longest wait seen for the current
     * deployment record (the snapshot carries the remaining ticks, not the total).
     */
    float respawnProgress(SquadBoardModel current) {
        BattleSnapshot snapshot = current.snapshot();
        if (snapshot == null || snapshot.deployment().phase() != DeploymentPhase.WAITING) {
            return 1.0F;
        }
        long waiting = snapshot.deployment().waitingTicks();
        long revision = snapshot.deployment().revision();
        if (revision != respawnRevision) {
            respawnRevision = revision;
            respawnTotalTicks = waiting;
        } else if (waiting > respawnTotalTicks) {
            respawnTotalTicks = waiting;
        }
        return respawnTotalTicks <= 0L ? 1.0F
                : Math.max(0.0F, Math.min(1.0F, 1.0F - waiting / (float) respawnTotalTicks));
    }

    /** Opens the formation tab (vote block, no faction). */
    void openFormationTab() {
        BattleTerminalNav.navigate(this, BattleTab.FORMATION);
    }

    /** Opens the loadout tab (the server answers with the loadout page). */
    void openLoadoutTab() {
        BattleTerminalNav.navigate(this, BattleTab.LOADOUT);
    }

    // ---- model ---------------------------------------------------------------------------------------

    private SquadBoardModel buildModel() {
        BattleSnapshot snapshot = ClientBattleState.snapshot();
        FormationSelectionSnapshot catalog = ClientFormationState.snapshot();
        FormationVotePhase phase = catalog == null ? null : catalog.votePhase();
        return SquadBoardModel.of(SquadBoardModel.Input.of(snapshot)
                .withViewedSquad(viewedSquad)
                .withSelectedMember(selectedMember)
                .withNow(ClientBattleState.estimatedServerTimeMillis())
                .withAdministrator(isAdministrator())
                .withVotePhase(phase)
                .withPending(activePending()));
    }

    private Set<SquadBoardModel.Action> activePending() {
        if (pending.isEmpty()) {
            return Set.of();
        }
        long now = System.nanoTime();
        pending.values().removeIf(sent -> now - sent > PENDING_TIMEOUT_NANOS);
        return pending.isEmpty() ? Set.of() : EnumSet.copyOf(pending.keySet());
    }

    private boolean isAdministrator() {
        return minecraft != null && minecraft.player != null
                && minecraft.player.hasPermissions(BattleRules.ADMIN_PERMISSION_LEVEL);
    }

    // ---- widgets ------------------------------------------------------------------------------------

    @Override
    protected void initTactical() {
        observedGeneration = ClientBattleState.generation();
        observedCatalog = ClientFormationState.snapshot();
        bindings.clear();
        tooltips.clear();
        tooltipTexts.clear();
        previousRoles = roles.isEmpty() ? Map.of() : new IdentityHashMap<>(roles);
        roles.clear();
        BattleSnapshot snapshot = ClientBattleState.snapshot();
        if (snapshot == null && !requestedSnapshot) {
            requestedSnapshot = true;
            BattleClientActions.requestSnapshot();
        }
        normalizeSelection(snapshot);
        model = buildModel();
        BattleTab current = page.tab;
        TacticalTabStrip strip = BattleTab.strip(current,
                tab -> tab == current ? null : model.tabDisabledReason(tab), this::onTab);
        addRenderableWidget(strip);
        roles.put(strip, TABS_ROLE);
        setTabStrip(strip);
        TacticalBoardChrome.placeTabs(font, shellLayout(), title(), strip);

        UiRect content = shellLayout().content();
        painterStage = model.stage();
        painter = switch (model.stage()) {
            case LOADING -> new PlaceholderPainter(this, content, false);
            case NO_FACTION -> new PlaceholderPainter(this, content, true);
            default -> switch (page) {
                case SQUADS -> new SquadPagePainter(this, content, model);
                case CLASSES -> new ClassPagePainter(this, content, model);
                case DEPLOYMENT -> new DeploymentPagePainter(this, content, model);
            };
        };
    }

    /** Follows the own squad when it changes; drops a target that left the viewed squad. */
    private void normalizeSelection(BattleSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        if (!ownSquadObserved || observedOwnSquad != snapshot.ownSquad()) {
            ownSquadObserved = true;
            observedOwnSquad = snapshot.ownSquad();
            viewedSquad = null;
        }
        SquadBoardModel probe = SquadBoardModel.of(SquadBoardModel.Input.of(snapshot)
                .withViewedSquad(viewedSquad).withSelectedMember(selectedMember));
        if (probe.selectedMember() == null) {
            selectedMember = null;
        }
    }

    private void onTab(BattleTab tab) {
        Page target = Page.of(tab);
        if (target != null) {
            showPage(tab);
            return;
        }
        BattleTerminalNav.navigate(this, tab);
    }

    private Component title() {
        return SquadBoardText.t(width < 440 ? SquadBoardText.TITLE_SHORT : SquadBoardText.TITLE);
    }

    /**
     * Rebuilds the widgets and gives the keyboard focus back to the same control (by its role),
     * also to the control that opened an open confirmation.
     */
    private void rebuildKeepingFocus() {
        boolean modalOpen = hasModal();
        GuiEventListener focused = modalOpen ? parkedFocus() : getFocused();
        String role = focused == null ? null : roles.get(focused);
        boolean keyboard = minecraft != null && minecraft.getLastInputType().isKeyboard();
        rebuildWidgets();
        GuiEventListener replacement = role == null || !(keyboard || modalOpen) ? null
                : widgetWithRole(role);
        if (modalOpen) {
            if (replacement != null) {
                reparkFocus(replacement);
            }
            return;
        }
        // Never leave the keyboard focus on a control of the previous build.
        setFocused(replacement);
    }

    /** The enabled control of this build with probe role {@code role}, or {@code null}. */
    private GuiEventListener widgetWithRole(String role) {
        for (Map.Entry<GuiEventListener, String> entry : roles.entrySet()) {
            GuiEventListener widget = entry.getKey();
            if (role.equals(entry.getValue()) && children().contains(widget)
                    && !(widget instanceof AbstractWidget control && !control.active)) {
                return widget;
            }
        }
        return null;
    }

    /**
     * Vanilla focuses the clicked control after its handler returned; when that handler rebuilt
     * the board (a call sign, a page switch) the control is already gone. Its rebuilt counterpart
     * (same probe role) takes the focus instead, else nothing, so later keys never reach a
     * control that is no longer on the screen.
     */
    private void adoptRebuiltFocus() {
        GuiEventListener focused = getFocused();
        if (focused == null || children().contains(focused)) {
            return;
        }
        String role = previousRoles.get(focused);
        setFocused(role == null ? null : widgetWithRole(role));
    }

    @Override
    public void tick() {
        super.tick();
        ClientBattleState.BattleFeedback feedback = ClientBattleState.feedback();
        if (feedback != null && feedback != observedFeedback) {
            // The server answered an intent: whatever it was, stop waiting.
            observedFeedback = feedback;
            pending.clear();
        }
        boolean catalogChanged = ClientFormationState.snapshot() != observedCatalog
                && (model == null || model.stage() != SquadBoardModel.Stage.READY);
        if (ClientBattleState.generation() != observedGeneration || catalogChanged) {
            snapshotChanged();
        }
    }

    /** A structurally new snapshot: stop waiting, rebuild the page, re-check a confirmation. */
    private void snapshotChanged() {
        pending.clear();
        rebuildKeepingFocus();
        revalidateConfirm();
    }

    // ---- rendering ----------------------------------------------------------------------------------

    @Override
    protected void renderTactical(GuiGraphics graphics, int mouseX, int mouseY,
                                  float partialTick) {
        model = buildModel();
        if (painterStage != null && model.stage() != painterStage) {
            // The snapshot changed what the page can show (cleared when the viewer left the
            // battle, the vote reopened, the formation locked) between two ticks: rebuild
            // before drawing, a page is never painted with a model it was not built for (a
            // cleared snapshot would otherwise crash the READY painters).
            snapshotChanged();
            model = buildModel();
        }
        frameModel = model;
        try {
            for (ActionBinding binding : bindings) {
                sync(binding, model);
            }
            BattleSnapshot snapshot = model.snapshot();
            TacticalBoardChrome.ShellSpec spec = TacticalBoardChrome.ShellSpec.of(title())
                    .withIdentity(identity())
                    .withLink(TacticalBoardChrome.LinkState.forBattleSnapshot(snapshot))
                    .withTabs(tabStrip())
                    .withHints(hints())
                    .withFeedback(TacticalBoardChrome.Feedback.fromBattle());
            drawShell(graphics, spec);
            if (painter != null) {
                painter.render(graphics, model, mouseX, mouseY);
            }
            renderWidgets(graphics, mouseX, mouseY, partialTick);
            if (painter != null) {
                painter.renderOverlay(graphics, model);
            }
        } finally {
            frameModel = null;
        }
    }

    /**
     * The longest identity candidate ("阵营 · 编制 · 小队 · 职务" shortened step by step) that
     * the header can show next to the full tab names (preview {@code identityCaps}).
     */
    private Component identity() {
        List<Component> candidates = model.identityCandidates();
        TacticalShellLayout layout = shellLayout();
        TacticalTabStrip strip = tabStrip();
        int tabs = strip == null ? 0 : strip.preferredWidth(font, false, layout.tight());
        int available = layout.header().width() - 14;
        int cap = Math.min(layout.header().width() * 3 / 10,
                available - font.width(title()) - (tabs > 0 ? tabs + 10 : 0) - 10);
        for (Component candidate : candidates) {
            if (font.width(candidate) <= cap) {
                return candidate;
            }
        }
        return candidates.isEmpty() ? null : candidates.get(candidates.size() - 1);
    }

    private List<TacticalBoardChrome.KeyHint> hints() {
        List<TacticalBoardChrome.KeyHint> hints = new ArrayList<>();
        hints.add(TacticalBoardChrome.KeyHint.close());
        TacticalTabStrip strip = tabStrip();
        if (strip != null && strip.canCycle()) {
            hints.add(TacticalBoardChrome.KeyHint.switchTab());
        }
        hints.add(TacticalBoardChrome.KeyHint.literal("R",
                SquadBoardText.t(SquadBoardText.HINT_REFRESH)));
        return hints;
    }

    /** Test seam: pagination of the shown point list (deployment page), or {@code null}. */
    SquadBoardModel.Page shownPointPage() {
        return painter instanceof DeploymentPagePainter deployment ? deployment.page() : null;
    }

    /** Test seam: pagination of the shown class list (class page), or {@code null}. */
    SquadBoardModel.Page shownClassPage() {
        return painter instanceof ClassPagePainter classes ? classes.page() : null;
    }

    // ---- probe state --------------------------------------------------------------------------------

    @Override
    public String uiSurfaceId() {
        return SURFACE_ID;
    }

    /** Preview state ({@code 20-squad.js}) of what the screen shows. */
    @Override
    public String uiStateId() {
        SquadBoardModel current = model == null ? buildModel() : model;
        OpenConfirm open = openConfirm;
        if (hasModal() && open != null && modal() == open.dialog()
                && open.state().action() == SquadBoardModel.Action.KICK_MEMBER) {
            return "kick";
        }
        if (hasModal()) {
            return "confirm";
        }
        return uiStateId(current, page.tab);
    }

    static String uiStateId(SquadBoardModel model, BattleTab page) {
        switch (model.stage()) {
            case LOADING -> {
                return "loading";
            }
            case NO_FACTION -> {
                return "nofaction";
            }
            case VOTE_PENDING -> {
                if (page == BattleTab.CLASSES) {
                    return "voteclasses";
                }
                if (page == BattleTab.DEPLOYMENT) {
                    return "votedeploy";
                }
                return model.input().votePhase() == FormationVotePhase.OPEN ? "vote" : "votewait";
            }
            default -> {
            }
        }
        boolean active = model.snapshot().deployment().phase() == DeploymentPhase.ACTIVE;
        if (page == BattleTab.CLASSES) {
            return !model.authority().inSquad() ? "classesnosquad" : active ? "classesactive"
                    : "classes";
        }
        if (page == BattleTab.DEPLOYMENT) {
            return active ? "active" : "deployment";
        }
        if (!model.authority().inSquad()) {
            return "nosquad";
        }
        return model.viewedSquad() == model.snapshot().ownSquad() ? "squads" : "other";
    }

    // ---- input --------------------------------------------------------------------------------------

    @Override
    protected boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_R && modifiers == 0) {
            BattleClientActions.requestSnapshot();
            return true;
        }
        if (keyCode != GLFW.GLFW_KEY_ESCAPE
                && ClientBootstrap.isKey(KeyBindingDefaults.Binding.TERMINAL, keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.onKeyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected boolean onMouseClicked(double mouseX, double mouseY, int button) {
        if (painter != null && painter.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        boolean handled = super.onMouseClicked(mouseX, mouseY, button);
        adoptRebuiltFocus();
        return handled;
    }

    @Override
    protected boolean onMouseScrolled(double mouseX, double mouseY, double delta) {
        if (super.onMouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        return painter != null && painter.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(previous);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- compatibility --------------------------------------------------------------------------

    /** Delegates to {@link SquadBoardModel.Page#visibleRows} (one paging rule, squad-01). */
    static int visibleDeploymentRows(int listTop, int listBottomExclusive,
                                     int pointHeight, int rowPitch) {
        return SquadBoardModel.Page.visibleRows(listTop, listBottomExclusive, pointHeight,
                rowPitch);
    }

    static int deploymentPageCount(int pointCount, int rowsPerPage) {
        return SquadBoardModel.Page.pageCount(pointCount, rowsPerPage);
    }

    static DeploymentPagination deploymentPagination(int pointCount, int rowsPerPage,
                                                     int requestedPage) {
        SquadBoardModel.Page result = SquadBoardModel.Page.of(pointCount, rowsPerPage,
                requestedPage);
        return new DeploymentPagination(result.page(), result.pageCount(), result.start(),
                result.end());
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

    /** Full call sign; delegates to {@link SquadLabels#callsign} (signature kept for callers). */
    public static MutableComponent callsign(SquadCallsign value) {
        return SquadLabels.callsign(value);
    }

    /** Delegates to {@link SquadLabels#className(String)} (signature kept for callers). */
    public static MutableComponent className(String classId) {
        return SquadLabels.className(classId);
    }

    /** Delegates to {@link SquadLabels#className(BattleSnapshot, String)}. */
    public static MutableComponent className(BattleSnapshot snapshot, String classId) {
        return SquadLabels.className(snapshot, classId);
    }

    /** Delegates to {@link SquadLabels#className(String, String)}. */
    public static MutableComponent className(String classId, String configuredName) {
        return SquadLabels.className(classId, configuredName);
    }

    // ---- placeholders -------------------------------------------------------------------------------

    /**
     * Sync placeholder (no snapshot yet: a refresh key; the snapshot was requested once on
     * opening) or, with a snapshot but no faction, a key to the formation page.
     */
    private static final class PlaceholderPainter implements Painter {
        private final SquadScreen host;
        private final UiRect well;
        private final UiRect empty;
        private final boolean noFaction;

        PlaceholderPainter(SquadScreen host, UiRect content, boolean noFaction) {
            this.host = host;
            this.noFaction = noFaction;
            TacticalShellLayout.Metrics metrics = host.boardMetrics();
            int width = Math.min(content.width() - 20, 260);
            int height = metrics.tight() ? 92 : 104;
            int left = content.left() + (content.width() - width) / 2;
            int top = content.top() + (content.height() - height) / 2;
            this.well = new UiRect(left, top, left + width, top + height);
            this.empty = new UiRect(well.left(), well.top(), well.right(),
                    well.bottom() - metrics.buttonHeight() - 8);
            Component label = noFaction ? SquadBoardText.t(SquadBoardText.NOFACTION_OPEN)
                    : Component.translatable("gui.wok_infantry.refresh");
            int keyWidth = Math.min(width - 12, host.boardFont().width(label) + 36);
            int keyLeft = well.left() + (width - keyWidth) / 2;
            UiRect key = new UiRect(keyLeft, well.bottom() - metrics.buttonHeight() - 6,
                    keyLeft + keyWidth, well.bottom() - 6);
            host.addKey(key, label, noFaction ? TacticalIcon.FLAG : TacticalIcon.REFRESH, null,
                    noFaction ? host::openFormationTab : BattleClientActions::requestSnapshot,
                    noFaction ? VOTE_UI_ID : REFRESH_UI_ID);
        }

        @Override
        public void render(GuiGraphics graphics, SquadBoardModel model, int mouseX, int mouseY) {
            Font font = host.boardFont();
            SquadBoardBlocks.region(graphics, "squad.sync", well);
            TacticalDraw.well(graphics, well);
            TacticalDraw.empty(graphics, font, empty,
                    noFaction ? TacticalIcon.FLAG : TacticalIcon.REFRESH,
                    SquadBoardText.t(noFaction ? SquadBoardText.NOFACTION_TITLE
                            : SquadBoardText.SYNC_TITLE),
                    SquadBoardText.t(noFaction ? SquadBoardText.NOFACTION_HINT
                            : SquadBoardText.SYNC_HINT), false);
            SquadBoardBlocks.endRegion(graphics);
        }
    }
}
