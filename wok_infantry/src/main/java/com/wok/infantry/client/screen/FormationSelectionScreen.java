package com.wok.infantry.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wok.infantry.client.ClientFormationState;
import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.formation.FormationCategory;
import com.wok.infantry.formation.selection.FactionSelectionView;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.formation.selection.FormationSelectionView;
import com.wok.infantry.formation.vote.FormationVotePhase;
import com.wok.infantry.network.formation.client.FormationClientNetworkBridge;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** Responsive tactical-board surface for faction -> category -> concrete formation. */
public final class FormationSelectionScreen extends Screen {
    private FormationSelectionSnapshot snapshot;
    private final Screen returnScreen;
    private String selectedFactionId;
    private String selectedCategoryId;
    private String highlightedFormationId = "";
    private int formationPage;
    private TacticalMapLayout.Layout tabletLayout;
    private SelectionLayout selectionLayout;

    public FormationSelectionScreen(FormationSelectionSnapshot snapshot, Screen returnScreen) {
        super(Component.literal("WOK步战 · 阵营与编制"));
        this.snapshot = snapshot;
        this.returnScreen = returnScreen;
        this.selectedFactionId = preferredFaction(snapshot);
        selectFirstCategoryAndFormation();
    }

    public Screen returnScreen() {
        return returnScreen;
    }

    public void replaceSnapshot(FormationSelectionSnapshot replacement) {
        snapshot = replacement;
        if (snapshot.factions().stream().noneMatch(faction ->
                faction.id().equals(selectedFactionId))) {
            selectedFactionId = preferredFaction(snapshot);
        }
        if (formationsForSelectedCategory().isEmpty()) {
            selectFirstCategoryAndFormation();
        } else if (formationsForSelectedCategory().stream().noneMatch(formation ->
                formation.id().equals(highlightedFormationId))) {
            highlightedFormationId = formationsForSelectedCategory().get(0).id();
        }
        formationPage = boundedPage(formationPage, formationsForSelectedCategory().size(),
                visibleFormationRows());
        rebuildWidgets();
    }

    @Override
    protected void init() {
        tabletLayout = TacticalMapLayout.compute(width, height);
        selectionLayout = SelectionLayout.compute(width, height, tabletLayout);
        addFactionButtons();
        addCategoryButtons();
        addFormationButtons();
        addAdministratorVoteActionButton();
    }

    private void addFactionButtons() {
        List<FactionSelectionView> factions = snapshot.factions();
        if (factions.isEmpty()) {
            return;
        }
        int gap = 3;
        int available = selectionLayout.right - selectionLayout.left
                - gap * (factions.size() - 1);
        int buttonWidth = Math.max(1, available / factions.size());
        for (int index = 0; index < factions.size(); index++) {
            FactionSelectionView faction = factions.get(index);
            int left = selectionLayout.left + index * (buttonWidth + gap);
            int right = index == factions.size() - 1 ? selectionLayout.right
                    : left + buttonWidth;
            String label = faction.displayName() + "  " + faction.population()
                    + "/" + faction.capacity();
            TacticalBoardButton button = new TacticalBoardButton(left,
                    selectionLayout.factionTop, right - left, 20, Component.literal(label),
                    ignored -> {
                        selectedFactionId = faction.id();
                        formationPage = 0;
                        selectFirstCategoryAndFormation();
                        if (snapshot.selectedFactionId().isBlank()) {
                            FormationClientNetworkBridge.selectFaction(snapshot.generation(),
                                    faction.id());
                        }
                        rebuildWidgets();
                    }, TacticalBoardButton.Kind.NAVIGATION,
                    faction.id().equals(selectedFactionId), TacticalBoardTheme.FRIENDLY);
            button.active = snapshot.selectedFactionId().isBlank()
                    ? faction.available() : faction.id().equals(snapshot.selectedFactionId());
            addRenderableWidget(button);
        }
    }

    private void addCategoryButtons() {
        FormationCategory[] categories = FormationCategory.values();
        int gap = 2;
        int available = selectionLayout.right - selectionLayout.left
                - gap * (categories.length - 1);
        int buttonWidth = Math.max(1, available / categories.length);
        FactionSelectionView faction = selectedFaction();
        for (int index = 0; index < categories.length; index++) {
            FormationCategory category = categories[index];
            int left = selectionLayout.left + index * (buttonWidth + gap);
            int right = index == categories.length - 1 ? selectionLayout.right
                    : left + buttonWidth;
            boolean present = faction != null && faction.formations().stream().anyMatch(
                    formation -> category.id().equals(formation.categoryId()));
            TacticalBoardButton button = new TacticalBoardButton(left,
                    selectionLayout.categoryTop, right - left, 19,
                    Component.literal(category.displayName()), ignored -> {
                        selectedCategoryId = category.id();
                        formationPage = 0;
                        highlightedFormationId = formationsForSelectedCategory().stream()
                                .map(FormationSelectionView::id).findFirst().orElse("");
                        rebuildWidgets();
                    }, TacticalBoardButton.Kind.TOGGLE,
                    category.id().equals(selectedCategoryId), TacticalBoardTheme.ACCENT);
            button.active = present;
            addRenderableWidget(button);
        }
    }

    private void addFormationButtons() {
        List<FormationSelectionView> formations = formationsForSelectedCategory();
        int rows = visibleFormationRows();
        formationPage = boundedPage(formationPage, formations.size(), rows);
        int start = formationPage * rows;
        int end = Math.min(formations.size(), start + rows);
        FactionSelectionView faction = selectedFaction();
        for (int index = start; index < end; index++) {
            FormationSelectionView formation = formations.get(index);
            int y = selectionLayout.listTop + (index - start) * 23;
            String label = formation.displayName() + "  " + formation.population()
                    + "/" + formation.capacity()
                    + (snapshot.votePhase() == FormationVotePhase.NOT_STARTED ? ""
                    : "  票 " + snapshot.voteTally().getOrDefault(formation.id(), 0));
            TacticalBoardButton button = new TacticalBoardButton(selectionLayout.listLeft, y,
                    selectionLayout.listRight - selectionLayout.listLeft, 20,
                    Component.literal(label), ignored -> {
                        highlightedFormationId = formation.id();
                        if (faction != null) {
                            if (snapshot.votePhase() == FormationVotePhase.OPEN
                                    && faction.id().equals(snapshot.selectedFactionId())) {
                                FormationClientNetworkBridge.vote(snapshot.generation(),
                                        formation.id());
                            }
                        }
                    }, TacticalBoardButton.Kind.CONTROL,
                    formation.id().equals(highlightedFormationId), TacticalBoardTheme.SELECTED);
            button.active = faction != null && faction.available() && formation.available()
                    && snapshot.votePhase() == FormationVotePhase.OPEN
                    && faction.id().equals(snapshot.selectedFactionId())
                    && (snapshot.ownVoteFormationId().isBlank()
                    || snapshot.voteChangeAllowed()
                    || formation.id().equals(snapshot.ownVoteFormationId()));
            addRenderableWidget(button);
        }
        if (pageCount(formations.size(), rows) > 1) {
            int pagerY = selectionLayout.bodyBottom - 20;
            TacticalBoardButton previous = new TacticalBoardButton(selectionLayout.listLeft,
                    pagerY, 24, 17, Component.literal("‹"), ignored -> {
                        formationPage = Math.max(0, formationPage - 1);
                        rebuildWidgets();
                    }, TacticalBoardButton.Kind.NAVIGATION, false,
                    TacticalBoardTheme.ACCENT);
            previous.active = formationPage > 0;
            addRenderableWidget(previous);
            TacticalBoardButton next = new TacticalBoardButton(selectionLayout.listRight - 24,
                    pagerY, 24, 17, Component.literal("›"), ignored -> {
                        formationPage = Math.min(pageCount(formations.size(), rows) - 1,
                                formationPage + 1);
                        rebuildWidgets();
                    }, TacticalBoardButton.Kind.NAVIGATION, false,
                    TacticalBoardTheme.ACCENT);
            next.active = formationPage + 1 < pageCount(formations.size(), rows);
            addRenderableWidget(next);
        }
    }

    private void addAdministratorVoteActionButton() {
        if (!administratorVoteActionVisible()) {
            return;
        }
        FormationSelectionView formation = highlightedFormation();
        TacticalMapLayout.Rect footer = tabletLayout.footer();
        int buttonWidth = Math.min(132, Math.max(92, footer.width() / 3));
        int buttonHeight = Math.max(12, footer.height() - 2);
        boolean opening = snapshot.votePhase() == FormationVotePhase.NOT_STARTED;
        TacticalBoardButton action = new TacticalBoardButton(footer.right() - buttonWidth - 2,
                footer.top() + 1, buttonWidth, buttonHeight,
                Component.literal(opening ? "管理员开启投票" : "管理员锁定"),
                ignored -> runAdministratorVoteAction(),
                TacticalBoardButton.Kind.CONTROL, false, TacticalBoardTheme.ACCENT);
        action.active = opening || formation != null && formation.available();
        addRenderableWidget(action);
    }

    private boolean administratorVoteActionVisible() {
        return minecraft != null && minecraft.player != null
                && minecraft.player.hasPermissions(BattleRules.ADMIN_PERMISSION_LEVEL)
                && snapshot.votePhase() != FormationVotePhase.LOCKED
                && !snapshot.selectedFactionId().isBlank();
    }

    private void runAdministratorVoteAction() {
        if (!administratorVoteActionVisible() || minecraft == null
                || minecraft.player == null || minecraft.player.connection == null) {
            ClientFormationState.feedback(false, "当前无法管理编制投票");
            return;
        }
        if (snapshot.votePhase() == FormationVotePhase.NOT_STARTED) {
            ClientFormationState.feedback(true, "正在开启编制投票");
            minecraft.player.connection.sendCommand(administratorOpenVoteCommand(
                    snapshot.selectedFactionId()));
            return;
        }
        FormationSelectionView formation = highlightedFormation();
        if (formation == null || !formation.available()) {
            ClientFormationState.feedback(false, "当前编制无法锁定");
            return;
        }
        ClientFormationState.feedback(true, "正在锁定：" + formation.displayName());
        minecraft.player.connection.sendCommand(administratorLockCommand(
                snapshot.selectedFactionId(), formation.id()));
    }

    static String administratorOpenVoteCommand(String factionId) {
        return "battle admin formation vote open " + factionId + " true";
    }

    static String administratorLockCommand(String factionId, String formationId) {
        return "battle admin formation vote lock " + factionId + " " + formationId;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (tabletLayout == null || selectionLayout == null) {
            tabletLayout = TacticalMapLayout.compute(width, height);
            selectionLayout = SelectionLayout.compute(width, height, tabletLayout);
        }
        TacticalBoardChrome.renderShell(graphics, width, height, tabletLayout);
        TacticalBoardChrome.renderHeader(graphics, font, tabletLayout, title,
                Component.literal(identityText()), true);

        TacticalBoardTheme.raisedPanel(graphics, selectionLayout.left,
                selectionLayout.bodyTop, selectionLayout.listRight,
                selectionLayout.bodyBottom, TacticalBoardTheme.BOARD_ALT);
        TacticalBoardTheme.sectionHeader(graphics, font, "具体编制",
                selectionLayout.listLeft, selectionLayout.bodyTop + 3,
                selectionLayout.listRight, TacticalBoardTheme.SELECTED);

        if (!selectionLayout.compact) {
            TacticalBoardTheme.raisedPanel(graphics, selectionLayout.detailLeft,
                    selectionLayout.bodyTop, selectionLayout.right,
                    selectionLayout.bodyBottom, TacticalBoardTheme.BOARD_ALT);
            renderDetails(graphics, selectionLayout.detailLeft + 6,
                    selectionLayout.bodyTop + 4,
                    selectionLayout.right - selectionLayout.detailLeft - 12,
                    selectionLayout.bodyBottom - 6);
        } else {
            int detailsTop = selectionLayout.listTop
                    + visibleFormationRows() * 23 + 2;
            renderDetails(graphics, selectionLayout.listLeft + 4, detailsTop,
                    selectionLayout.listRight - selectionLayout.listLeft - 8,
                    selectionLayout.bodyBottom - 3);
        }

        String feedback = ClientFormationState.feedback();
        int footerLeft = tabletLayout.footer().left() + 4;
        int footerRight = administratorVoteActionVisible()
                ? tabletLayout.footer().right()
                - Math.min(132, Math.max(92, tabletLayout.footer().width() / 3)) - 6
                : tabletLayout.footer().right() - 4;
        int footerCenter = footerLeft + Math.max(1, footerRight - footerLeft) / 2;
        int footerTextWidth = Math.max(1, footerRight - footerLeft);
        if (!feedback.isBlank()) {
            graphics.drawCenteredString(font, Component.literal(
                            font.plainSubstrByWidth(feedback, footerTextWidth)), footerCenter,
                    tabletLayout.footer().top() + 7,
                    ClientFormationState.feedbackSuccess()
                            ? TacticalBoardTheme.SUCCESS : TacticalBoardTheme.DANGER);
        } else {
            graphics.drawCenteredString(font,
                    Component.literal(font.plainSubstrByWidth(voteStatusText(), footerTextWidth)),
                    footerCenter,
                    tabletLayout.footer().top() + 7, TacticalBoardTheme.MUTED_TEXT);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private String voteStatusText() {
        return switch (snapshot.votePhase()) {
            case NOT_STARTED -> snapshot.selectedFactionId().isBlank()
                    ? "阵营 → 大类别 → 具体编制"
                    : "等待管理员开启编制投票 · Esc 暂时关闭";
            case OPEN -> snapshot.ownVoteFormationId().isBlank()
                    ? "编制投票进行中 · Esc 暂时关闭"
                    : "已投：" + formationDisplayName(snapshot.ownVoteFormationId())
                    + (snapshot.voteChangeAllowed() ? " · 可改票" : " · 不可改票")
                    + " · Esc 暂时关闭";
            case LOCKED -> "阵营共享编制："
                    + formationDisplayName(snapshot.lockedFormationId());
        };
    }

    private String formationDisplayName(String formationId) {
        return snapshot.factions().stream()
                .flatMap(faction -> faction.formations().stream())
                .filter(formation -> formation.id().equals(formationId))
                .map(FormationSelectionView::displayName)
                .findFirst().orElse(formationId);
    }

    private void renderDetails(GuiGraphics graphics, int x, int y, int maxWidth,
                               int bottom) {
        FormationSelectionView formation = highlightedFormation();
        if (formation == null || maxWidth <= 8 || bottom <= y) {
            return;
        }
        TacticalBoardTheme.sectionHeader(graphics, font, formation.displayName(), x, y,
                x + maxWidth, formation.available()
                        ? TacticalBoardTheme.ACCENT : TacticalBoardTheme.DANGER);
        ResourceLocation icon = formation.iconId().isBlank() ? null
                : ResourceLocation.tryParse(formation.iconId());
        boolean compact = selectionLayout != null && selectionLayout.compact;
        if (icon != null && compact) {
            renderFormationIcon(graphics, icon, x + maxWidth - 13, y + 1, 12);
        }
        y += 18;
        int iconSize = icon == null || compact ? 0 : Math.min(48,
                Math.max(24, maxWidth / 4));
        int descriptionWidth = iconSize == 0 ? maxWidth - 4
                : Math.max(1, maxWidth - iconSize - 10);
        int descriptionBottom = y;
        if (iconSize > 0) {
            renderFormationIcon(graphics, icon, x + maxWidth - iconSize, y, iconSize);
            descriptionBottom = y + iconSize;
        }
        y = drawWrapped(graphics, formation.description(), x + 2, y,
                descriptionWidth, TacticalBoardTheme.TEXT, bottom, 3);
        if (iconSize > 0) {
            y = Math.max(y, descriptionBottom);
        }
        if (!formation.unavailableReason().isBlank()) {
            y = drawWrapped(graphics, "不可用：" + formation.unavailableReason(), x + 2,
                    y + 2, maxWidth - 4, TacticalBoardTheme.DANGER, bottom, 2);
        }
        y = drawList(graphics, "能力", formation.capabilities(), x + 2, y + 3,
                maxWidth - 4, TacticalBoardTheme.ACCENT, bottom, 3);
        y = drawList(graphics, "兵种", formation.classes(), x + 2, y + 2,
                maxWidth - 4, TacticalBoardTheme.TEXT, bottom, 2);
        y = drawList(graphics, "小队", formation.squads(), x + 2, y + 2,
                maxWidth - 4, TacticalBoardTheme.MUTED_TEXT, bottom, 2);
        drawList(graphics, "载具", formation.vehicles(), x + 2, y + 2,
                maxWidth - 4, TacticalBoardTheme.MUTED_TEXT, bottom, 2);
    }

    private static void renderFormationIcon(GuiGraphics graphics, ResourceLocation icon,
                                            int x, int y, int size) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(icon, x, y, size, size, 0.0F, 0.0F,
                256, 256, 256, 256);
        RenderSystem.disableBlend();
    }

    private int drawList(GuiGraphics graphics, String label, List<String> entries,
                         int x, int y, int maxWidth, int color, int bottom, int maxLines) {
        String value = entries.isEmpty() ? "无" : String.join("、", entries);
        return drawWrapped(graphics, label + "：" + value, x, y, maxWidth, color,
                bottom, maxLines);
    }

    private int drawWrapped(GuiGraphics graphics, String value, int x, int y,
                            int maxWidth, int color, int bottom, int maxLines) {
        if (value == null || value.isBlank() || y >= bottom) {
            return y;
        }
        List<FormattedCharSequence> lines = font.split(Component.literal(value),
                Math.max(1, maxWidth));
        int visible = Math.min(Math.min(lines.size(), maxLines),
                Math.max(0, (bottom - y) / 10));
        for (int index = 0; index < visible; index++) {
            graphics.drawString(font, lines.get(index), x, y + index * 10, color, false);
        }
        return y + visible * 10;
    }

    private String identityText() {
        FactionSelectionView faction = selectedFaction();
        FormationCategory category = FormationCategory.byId(selectedCategoryId).orElse(null);
        String factionText = faction == null ? "未选择阵营" : faction.displayName();
        return category == null ? factionText : factionText + " · " + category.displayName();
    }

    private FactionSelectionView selectedFaction() {
        return snapshot.factions().stream()
                .filter(faction -> faction.id().equals(selectedFactionId)).findFirst()
                .orElse(null);
    }

    private List<FormationSelectionView> formationsForSelectedCategory() {
        FactionSelectionView faction = selectedFaction();
        return faction == null ? List.of() : faction.formations().stream()
                .filter(formation -> formation.categoryId().equals(selectedCategoryId)).toList();
    }

    private FormationSelectionView highlightedFormation() {
        List<FormationSelectionView> formations = formationsForSelectedCategory();
        return formations.stream().filter(formation ->
                        formation.id().equals(highlightedFormationId)).findFirst()
                .orElseGet(() -> formations.stream().findFirst().orElse(null));
    }

    private void selectFirstCategoryAndFormation() {
        FactionSelectionView faction = selectedFaction();
        FormationSelectionView first = faction == null ? null : faction.formations().stream()
                .filter(FormationSelectionView::available).findFirst()
                .orElseGet(() -> faction.formations().stream().findFirst().orElse(null));
        selectedCategoryId = first == null ? FormationCategory.INFANTRY.id()
                : first.categoryId();
        highlightedFormationId = first == null ? "" : first.id();
    }

    private int visibleFormationRows() {
        if (selectionLayout == null) {
            return 4;
        }
        int availableHeight = selectionLayout.bodyBottom - selectionLayout.listTop
                - (selectionLayout.compact ? 58 : 22);
        return Math.max(1, Math.min(7, availableHeight / 23));
    }

    private static String preferredFaction(FormationSelectionSnapshot snapshot) {
        if (!snapshot.selectedFactionId().isBlank()) {
            return snapshot.selectedFactionId();
        }
        return snapshot.factions().stream().filter(FactionSelectionView::available)
                .map(FactionSelectionView::id).findFirst()
                .orElseGet(() -> snapshot.factions().stream().map(FactionSelectionView::id)
                        .findFirst().orElse(""));
    }

    private static int boundedPage(int requested, int itemCount, int pageSize) {
        return Math.max(0, Math.min(requested, pageCount(itemCount, pageSize) - 1));
    }

    private static int pageCount(int itemCount, int pageSize) {
        return Math.max(1, (itemCount + Math.max(1, pageSize) - 1)
                / Math.max(1, pageSize));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !snapshot.selectionRequired() || !snapshot.selectedFactionId().isBlank();
    }

    @Override
    public void onClose() {
        if (shouldCloseOnEsc() && minecraft != null) {
            minecraft.setScreen(returnScreen);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record SelectionLayout(boolean compact, int left, int right,
                                   int factionTop, int categoryTop,
                                   int bodyTop, int bodyBottom,
                                   int listLeft, int listRight, int listTop,
                                   int detailLeft) {
        private static SelectionLayout compute(int width, int height,
                                               TacticalMapLayout.Layout tablet) {
            int left = tablet.header().left() + 5;
            int right = tablet.header().right() - 5;
            int factionTop = tablet.header().bottom() + 4;
            int categoryTop = factionTop + 23;
            int bodyTop = categoryTop + 23;
            int bodyBottom = Math.max(bodyTop + 40, tablet.footer().top() - 4);
            boolean compact = width < 600 || height < 360;
            int listLeft = left + 5;
            int listRight = compact ? right - 5
                    : left + Math.max(150, (right - left) * 42 / 100);
            int detailLeft = compact ? right : listRight + 5;
            return new SelectionLayout(compact, left, right, factionTop, categoryTop,
                    bodyTop, bodyBottom, listLeft, listRight, bodyTop + 20, detailLeft);
        }
    }
}
