package com.wok.infantry.client.screen;

import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.client.ClientBattleState;
import com.wok.infantry.client.ClientBootstrap;
import com.wok.infantry.client.ClientFormationState;
import com.wok.infantry.client.KeyBindingDefaults;
import com.wok.infantry.client.hud.TacticalHud;
import com.wok.infantry.formation.selection.FactionSelectionView;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.formation.selection.FormationSelectionView;
import com.wok.infantry.network.battle.client.BattleClientNetworkBridge;
import com.wok.infantry.network.formation.client.FormationClientNetworkBridge;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

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
 */
public final class FormationSelectionScreen extends TacticalScreen
        implements BattleTerminalNav.Terminal {
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

    /** The catalog shown, or {@code null} while waiting for it. */
    public FormationSelectionSnapshot snapshot() {
        return snapshot;
    }

    /** Shows a newer catalog in place: highlight, scroll position and list focus are kept. */
    public void replaceSnapshot(FormationSelectionSnapshot replacement) {
        snapshot = replacement;
        normalizeSelection();
        if (minecraft != null) {
            rebuildKeepingFocus();
        }
    }

    static String administratorOpenVoteCommand(String factionId) {
        return "battle admin formation vote open " + factionId + " true";
    }

    static String administratorLockCommand(String factionId, String formationId) {
        return "battle admin formation vote lock " + factionId + " " + formationId;
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
        layout = FormationScreenLayout.compute(width, height,
                waiting ? 0 : model.snapshot().factions().size(), model.joined(),
                model.admin().visible(), detailPage, waiting, listNeed, joinWidth);
        actionKey = UiRect.EMPTY;
        actionBar = UiRect.EMPTY;
        if (model.joined()) {
            TacticalTabStrip strip = BattleTab.strip(BattleTab.FORMATION,
                    tab -> tab == BattleTab.FORMATION || model.hasFormation() ? null
                            : FormationText.tabLockedReason(), this::navigate);
            addRenderableWidget(strip);
            setTabStrip(strip);
            TacticalBoardChrome.placeTabs(font, shellLayout(), FormationText.title(width), strip);
        }
        switch (layout.mode()) {
            case WAITING -> addRetryKey();
            case WIDE -> {
                addFactionKeys(model);
                addJoinKey(model, layout.joinKey(), true);
                addList(model, metrics);
                addAdminKey(model);
                addVoteKey(model, layout.detailAction(), layout.detailAction().left());
            }
            case NARROW_LIST -> {
                addFactionKeys(model);
                addList(model, metrics);
                addAdminKey(model);
                addDetailsKey(model);
                if (model.joined()) {
                    addVoteKey(model, layout.actionBar(), layout.detailsKey().right() + 4);
                } else {
                    addJoinKey(model, layout.mainKey(), false);
                }
            }
            case NARROW_DETAIL -> {
                addRenderableWidget(BattleUiButton.builder(FormationText.listKey(), ignored -> {
                            detailPage = false;
                            requestRebuild();
                        }).icon(TacticalIcon.BACK)
                        .bounds(layout.crumbBack().left(), layout.crumbBack().top(),
                                layout.crumbBack().width(), layout.crumbBack().height())
                        .build());
                addVoteKey(model, layout.detailAction(), layout.detailAction().left());
            }
        }
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
            addRenderableWidget(key);
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
        addRenderableWidget(key);
    }

    private void addList(FormationVoteModel model, TacticalShellLayout.Metrics metrics) {
        list.update(model, metrics, layout.well());
        addRenderableWidget(list.widget());
    }

    private void addAdminKey(FormationVoteModel model) {
        FormationVoteModel.AdminState admin = model.admin();
        if (!admin.visible() || layout.adminKey().isEmpty()) {
            return;
        }
        UiRect rect = layout.adminKey();
        Button key;
        if (admin.opening()) {
            key = BattleUiButton.builder(FormationText.adminOpenKey(), ignored -> openVote())
                    .icon(TacticalIcon.UNLOCK)
                    .bounds(rect.left(), rect.top(), rect.width(), rect.height()).build();
            key.active = admin.openEnabled();
        } else {
            key = BattleUiButton.builder(FormationText.adminLockKey(), ignored -> confirmLock())
                    .kind(BattleUiButton.Kind.DANGER).icon(TacticalIcon.LOCK)
                    .bounds(rect.left(), rect.top(), rect.width(), rect.height()).build();
            key.active = admin.lockEnabled();
            if (!admin.lockEnabled()) {
                key.setTooltip(Tooltip.create(FormationText.adminDetail(model).get(0)));
            }
        }
        addRenderableWidget(key);
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
        addRenderableWidget(key);
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
        addRenderableWidget(key);
    }

    private void addRetryKey() {
        UiRect retry = waitingGeometry().retry();
        addRenderableWidget(BattleUiButton.builder(FormationText.retryKey(), ignored -> retry())
                .kind(BattleUiButton.Kind.CONTROL).icon(TacticalIcon.REFRESH)
                .bounds(retry.left(), retry.top(), retry.width(), retry.height()).build());
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
        openModal(TacticalConfirmDialog.builder(FormationText.joinConfirmTitle(faction),
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
                .build());
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
        openModal(TacticalConfirmDialog.builder(FormationText.lockConfirmTitle(own),
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
                .build());
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

    private void rebuildKeepingFocus() {
        boolean listFocused = getFocused() == list.widget();
        rebuildWidgets();
        if (listFocused && children().contains(list.widget())) {
            setFocused(list.widget());
        }
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
            case WAITING -> renderWaiting(graphics, model);
            case WIDE -> {
                renderStrip(graphics, model);
                renderListPanel(graphics, model, false);
                detail.render(graphics, font, layout, model);
                renderAction(graphics, model);
            }
            case NARROW_LIST -> {
                renderListPanel(graphics, model, true);
                if (model.joined()) {
                    renderAction(graphics, model);
                }
            }
            case NARROW_DETAIL -> {
                renderCrumb(graphics, model, mouseX, mouseY);
                detail.render(graphics, font, layout, model);
                renderAction(graphics, model);
            }
        }
        renderWidgets(graphics, mouseX, mouseY, partialTick);
    }

    private List<TacticalBoardChrome.KeyHint> hints(FormationVoteModel model) {
        Component terminalKey = ClientBootstrap.keyLabel(KeyBindingDefaults.Binding.TERMINAL);
        List<TacticalBoardChrome.KeyHint> hints = new ArrayList<>();
        Component esc = FormationText.escClose(model.joined(), terminalKey);
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
        if (tabStrip() != null) {
            hints.add(TacticalBoardChrome.KeyHint.switchTab());
        }
        return hints;
    }

    /** Footer receipt: the 3-second server/pending receipt, otherwise the next step. */
    private TacticalBoardChrome.Feedback footer(FormationVoteModel model) {
        TacticalBoardChrome.Feedback receipt = TacticalBoardChrome.Feedback.fromFormation();
        if (receipt != null) {
            return receipt;
        }
        UiRect footer = layout.shell().footer();
        int room = (int) Math.floor(footer.width() * (layout.shell().tight() ? 0.62D : 0.5D))
                - 16;
        return TacticalBoardChrome.Feedback.notice(FormationDetailPanel.pick(font,
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
        FormationDetailPanel.drawReason(graphics, font, reasonLeft + 2, textY,
                actionKey.left() - 6 - (reasonLeft + 2), action.icon(),
                FormationText.reason(model, action),
                action.enabled() || action.mine() ? TacticalBoardTheme.MUTED
                        : TacticalBoardTheme.TEXT, TacticalBoardTheme.MUTED);
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
        int lines = TextFit.wrap(font, (empty ? FormationText.emptyHint()
                : FormationText.waitingHint()).getString(), emptyWidth - 12, 0).size();
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
        boolean joined = snapshot != null && model().joined();
        minecraft.setScreen(returnScreen);
        if (!joined && minecraft.gui != null) {
            minecraft.gui.setOverlayMessage(FormationText.reopenNotice(
                    ClientBootstrap.keyLabel(KeyBindingDefaults.Binding.TERMINAL)), false);
        }
    }
}
