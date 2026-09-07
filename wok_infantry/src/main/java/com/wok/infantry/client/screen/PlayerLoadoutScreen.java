package com.wok.infantry.client.screen;

import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.client.ClientBattleState;
import com.wok.infantry.loadout.LoadoutClassDefinition;
import com.wok.infantry.loadout.LoadoutEntry;
import com.wok.infantry.loadout.LoadoutSlotDefinition;
import com.wok.infantry.loadout.LoadoutSnapshot;
import com.wok.infantry.network.LoadoutNetwork;
import com.wok.infantry.network.serverbound.OpenLoadoutPacket;
import com.wok.infantry.network.serverbound.SavePlayerLoadoutPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Player-facing tactical tablet for role and equipment selection. */
public final class PlayerLoadoutScreen extends Screen {
    private final LoadoutSnapshot snapshot;
    private final Screen previous;
    private String selectedClassId;
    private String selectedSlotId = "";
    private int page;
    private int slotPage;
    private int classPage;
    private boolean classPageInitialized;
    private int lastClassesPerPage = -1;

    private TacticalMapLayout.Layout tabletLayout;
    private boolean compact;
    private boolean rich;
    private int margin;
    private int contentTop;
    private int panelsTop;
    private int panelsBottom;
    private int slotX;
    private int slotWidth;
    private int choiceX;
    private int choiceWidth;
    private int detailX;
    private int detailWidth;
    private int candidatePageCount = 1;

    public PlayerLoadoutScreen(LoadoutSnapshot snapshot) {
        this(snapshot, null);
    }

    public PlayerLoadoutScreen(LoadoutSnapshot snapshot, Screen previous) {
        super(Component.translatable("screen.wok_infantry.loadout"));
        this.snapshot = snapshot;
        this.previous = previous;
        this.selectedClassId = resolveInitialClass(snapshot);
        reconcileSelectedSlot();
    }

    @Override
    protected void init() {
        layoutBoard();
        initPrimaryNavigation();
        initClassStrip();
        initSlotButtons();
        initCandidateButtons();
        initCommitButtons();
    }

    private void layoutBoard() {
        tabletLayout = TacticalMapLayout.compute(width, height);
        compact = width < 520 || height < 350;
        rich = tabletLayout.rich();
        margin = rich ? 16 : 8;
        contentTop = rich ? 58 : 48;
        int classStripHeight = rich ? 32 : 27;
        panelsTop = contentTop + classStripHeight + 5;
        panelsBottom = Math.max(panelsTop + 90, height - 34);

        slotX = margin;
        slotWidth = compact ? 96 : rich ? 176 : 128;
        choiceX = slotX + slotWidth + 6;
        if (rich) {
            detailWidth = Math.max(205, Math.min(260, width / 4));
            detailX = width - margin - detailWidth;
            choiceWidth = Math.max(140, detailX - 6 - choiceX);
        } else {
            detailX = width - margin;
            detailWidth = 0;
            choiceWidth = Math.max(100, detailX - choiceX);
        }
    }

    private void initPrimaryNavigation() {
        int y = tabletLayout.topBarY();
        int utilityWidth = 22;
        int refreshX = tabletLayout.header().right() - 4 - utilityWidth;
        int gap = 3;
        int tabWidth = Math.max(48, Math.min(96,
                (refreshX - margin - 8 - gap * 2) / 3));
        int x = margin;

        addRenderableWidget(BattleUiButton.builder(
                        Component.translatable("screen.wok_infantry.tab.squads"),
                        ignored -> openSquads())
                .bounds(x, y, tabWidth, 20).build());
        x += tabWidth + gap;

        Button loadout = BattleUiButton.builder(
                        Component.translatable("screen.wok_infantry.tab.loadout"), ignored -> {
                        })
                .selected(true)
                .bounds(x, y, tabWidth, 20).build();
        loadout.active = false;
        addRenderableWidget(loadout);
        x += tabWidth + gap;

        addRenderableWidget(BattleUiButton.builder(
                        Component.translatable("screen.wok_infantry.tab.map"), ignored -> {
                            if (minecraft != null) {
                                minecraft.setScreen(new TacticalMapScreen(this));
                            }
                        })
                .bounds(x, y, tabWidth, 20).build());

        addRenderableWidget(BattleUiButton.builder(Component.literal("R"), ignored -> refresh())
                .kind(BattleUiButton.Kind.CONTROL)
                .tooltip(Tooltip.create(Component.translatable("gui.wok_infantry.refresh")))
                .bounds(refreshX, y, utilityWidth, 20).build());
    }

    private void initClassStrip() {
        List<LoadoutClassDefinition> classes = enabledClasses();
        int stripLeft = margin;
        int stripRight = width - margin;
        int buttonY = contentTop + (rich ? 6 : 3);
        int buttonHeight = 20;
        int pagerWidth = 30;
        int classAreaWidth = Math.max(48, stripRight - stripLeft - pagerWidth * 2 - 16);
        int desiredTabWidth = rich ? 108 : 72;
        int classesPerPage = Math.max(1, classAreaWidth / desiredTabWidth);
        int classPageCount = Math.max(1,
                (classes.size() + classesPerPage - 1) / classesPerPage);
        if (!classPageInitialized || lastClassesPerPage != classesPerPage) {
            int selectedIndex = Math.max(0, java.util.stream.IntStream.range(0, classes.size())
                    .filter(index -> classes.get(index).id().equals(selectedClassId))
                    .findFirst().orElse(0));
            classPage = selectedIndex / classesPerPage;
            classPageInitialized = true;
            lastClassesPerPage = classesPerPage;
        }
        classPage = Math.max(0, Math.min(classPage, classPageCount - 1));
        int start = classPage * classesPerPage;
        int end = Math.min(classes.size(), start + classesPerPage);
        int visibleClasses = Math.max(1, end - start);
        int tabWidth = Math.max(40, classAreaWidth / visibleClasses);
        int tabX = stripLeft + pagerWidth + 8;

        Button previousClass = BattleUiButton.builder(Component.literal("‹"), ignored -> {
            classPage--;
            refreshWidgets();
        }).kind(BattleUiButton.Kind.CONTROL)
                .bounds(stripLeft + 4, buttonY, pagerWidth, buttonHeight).build();
        previousClass.active = classPage > 0;
        addRenderableWidget(previousClass);

        for (int index = start; index < end; index++) {
            LoadoutClassDefinition definition = classes.get(index);
            Component fullLabel = Component.literal(definition.displayName());
            Component visibleLabel = fittedButtonLabel(fullLabel, tabWidth - 4);
            boolean selected = definition.id().equals(selectedClassId);
            Button button = BattleUiButton.builder(visibleLabel, ignored -> {
                selectedClassId = definition.id();
                selectedSlotId = "";
                reconcileSelectedSlot();
                slotPage = 0;
                page = 0;
                refreshWidgets();
            }).selected(selected)
                    .tooltip(Tooltip.create(fullLabel))
                    .bounds(tabX + (index - start) * tabWidth, buttonY,
                            Math.max(36, tabWidth - 4), buttonHeight).build();
            button.active = !selected;
            addRenderableWidget(button);
        }

        Button nextClass = BattleUiButton.builder(Component.literal("›"), ignored -> {
            classPage++;
            refreshWidgets();
        }).kind(BattleUiButton.Kind.CONTROL)
                .bounds(stripRight - 4 - pagerWidth, buttonY,
                        pagerWidth, buttonHeight).build();
        nextClass.active = classPage + 1 < classPageCount;
        addRenderableWidget(nextClass);
    }

    private void initSlotButtons() {
        LoadoutClassDefinition definition = selectedClass();
        List<LoadoutSlotDefinition> slots = definition == null
                ? List.of() : definition.slotDefinitions();
        if (slots.isEmpty()) {
            slotPage = 0;
            return;
        }
        int buttonTop = panelsTop + 20;
        int availableHeight = Math.max(1, panelsBottom - buttonTop - 4);
        int pitch = rich ? 31 : 19;
        int withoutPager = Math.max(1, availableHeight / pitch);
        boolean needsPager = slots.size() > withoutPager;
        int visibleCount = Math.max(1, (availableHeight - (needsPager ? 23 : 0)) / pitch);
        int slotPageCount = Math.max(1, (slots.size() + visibleCount - 1) / visibleCount);
        slotPage = Math.max(0, Math.min(slotPage, slotPageCount - 1));
        int start = slotPage * visibleCount;
        int end = Math.min(slots.size(), start + visibleCount);
        int buttonHeight = Math.max(15, Math.min(rich ? 26 : 17, pitch - 2));
        for (int index = start; index < end; index++) {
            LoadoutSlotDefinition slot = slots.get(index);
            Component fullLabel = slotName(slot);
            Component visibleLabel = fittedButtonLabel(fullLabel, slotWidth - 8);
            boolean selected = slot.id().equals(selectedSlotId);
            Button button = BattleUiButton.builder(visibleLabel, ignored -> {
                selectedSlotId = slot.id();
                page = 0;
                refreshWidgets();
            }).selected(selected)
                    .tooltip(Tooltip.create(fullLabel.copy().append("\n")
                            .append(Component.literal(slot.target().displayName()))))
                    .bounds(slotX + 4, buttonTop + (index - start) * pitch,
                            slotWidth - 8, buttonHeight).build();
            button.active = !selected;
            addRenderableWidget(button);
        }
        if (slotPageCount > 1) {
            int pagerY = panelsBottom - 22;
            int pagerWidth = Math.max(24, (slotWidth - 12) / 2);
            Button previous = BattleUiButton.builder(Component.literal("‹"), ignored -> {
                slotPage--;
                selectedSlotId = slots.get(Math.max(0, slotPage * visibleCount)).id();
                page = 0;
                refreshWidgets();
            }).kind(BattleUiButton.Kind.CONTROL)
                    .bounds(slotX + 4, pagerY, pagerWidth, 18).build();
            previous.active = slotPage > 0;
            addRenderableWidget(previous);
            Button next = BattleUiButton.builder(Component.literal("›"), ignored -> {
                slotPage++;
                selectedSlotId = slots.get(Math.min(slots.size() - 1,
                        slotPage * visibleCount)).id();
                page = 0;
                refreshWidgets();
            }).kind(BattleUiButton.Kind.CONTROL)
                    .bounds(slotX + slotWidth - 4 - pagerWidth, pagerY,
                            pagerWidth, 18).build();
            next.active = slotPage + 1 < slotPageCount;
            addRenderableWidget(next);
        }
    }

    private void initCandidateButtons() {
        LoadoutClassDefinition definition = selectedClass();
        if (definition == null) {
            candidatePageCount = 1;
            page = 0;
            return;
        }

        LoadoutSlotDefinition selectedSlot = selectedSlot();
        if (selectedSlot == null) {
            candidatePageCount = 1;
            page = 0;
            return;
        }
        List<LoadoutEntry> entries = definition.entries(selectedSlot.id());
        int columns = compact ? 1 : 2;
        int rows = 4;
        int pageSize = columns * rows;
        candidatePageCount = Math.max(1, (entries.size() + pageSize - 1) / pageSize);
        page = Math.max(0, Math.min(page, candidatePageCount - 1));
        int start = page * pageSize;
        int end = Math.min(entries.size(), start + pageSize);

        int listTop = panelsTop + 20;
        int pagerY = panelsBottom - 23;
        int availableHeight = Math.max(1, pagerY - listTop - 3);
        int maxPitch = rich ? Math.max(48, Math.min(70, height / 12)) : 25;
        int pitch = Math.max(20, Math.min(maxPitch, availableHeight / rows));
        int buttonHeight = Math.max(18, Math.min(rich ? 58 : 20, pitch - 3));
        int gap = 4;
        int cardWidth = Math.max(44,
                (choiceWidth - 8 - gap * (columns - 1)) / columns);

        for (int index = start; index < end; index++) {
            LoadoutEntry entry = entries.get(index);
            int local = index - start;
            int x = choiceX + 4 + local % columns * (cardWidth + gap);
            int y = listTop + local / columns * pitch;
            boolean selected = entry.id().equals(
                    effectiveSelectedEntryId(definition, selectedSlot));
            Component fullLabel = Component.literal(entry.displayName());
            MutableComponent tooltip = fullLabel.copy().append("\n")
                    .append(Component.literal(entry.itemId() + " ×" + entry.count()));
            LoadoutPreviewButton button = new LoadoutPreviewButton(
                    x, y, cardWidth, buttonHeight, entry, selected, ignored -> {
                snapshot.player().select(selectedClassId, selectedSlot.id(), entry.id());
                refreshWidgets();
            });
            button.setTooltip(Tooltip.create(tooltip));
            addRenderableWidget(button);
        }

        int pagerWidth = 30;
        Button previousPage = BattleUiButton.builder(Component.literal("<"), ignored -> {
            page--;
            refreshWidgets();
        }).kind(BattleUiButton.Kind.CONTROL)
                .bounds(choiceX + 4, pagerY, pagerWidth, 20).build();
        previousPage.active = page > 0;
        addRenderableWidget(previousPage);

        Button nextPage = BattleUiButton.builder(Component.literal(">"), ignored -> {
            page++;
            refreshWidgets();
        }).kind(BattleUiButton.Kind.CONTROL)
                .bounds(choiceX + choiceWidth - 4 - pagerWidth,
                        pagerY, pagerWidth, 20).build();
        nextPage.active = page + 1 < candidatePageCount;
        addRenderableWidget(nextPage);
    }

    private void initCommitButtons() {
        int gap = 4;
        int x;
        int y;
        int availableWidth;
        int buttonHeight;
        if (rich) {
            x = detailX + 5;
            y = panelsBottom - 25;
            availableWidth = detailWidth - 10;
            buttonHeight = 20;
        } else {
            TacticalMapLayout.Rect footer = tabletLayout.footer();
            x = footer.left() + 4;
            y = footer.top() + 2;
            availableWidth = footer.width() - 8;
            buttonHeight = Math.max(14, footer.height() - 4);
        }
        int buttonWidth = Math.max(44, (availableWidth - gap) / 2);
        Button save = BattleUiButton.builder(
                        Component.translatable("gui.wok_infantry.loadout.save"),
                        ignored -> save(false))
                .kind(BattleUiButton.Kind.SUCCESS)
                .bounds(x, y, buttonWidth, buttonHeight).build();
        save.active = selectedClass() != null;
        addRenderableWidget(save);
        addRenderableWidget(BattleUiButton.builder(
                        Component.translatable("gui.wok_infantry.refresh"), ignored -> refresh())
                .kind(BattleUiButton.Kind.CONTROL)
                .bounds(x + buttonWidth + gap, y,
                        Math.max(44, availableWidth - buttonWidth - gap),
                        buttonHeight).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        TacticalBoardChrome.renderShell(graphics, width, height, tabletLayout);
        BattleSnapshot battle = ClientBattleState.snapshot();
        TacticalBoardChrome.renderHeader(graphics, font, tabletLayout,
                Component.translatable("screen.wok_infantry.loadout.board_title"),
                battle == null ? loadoutIdentity() : TacticalBoardChrome.battleIdentity(battle),
                true);

        int stripBottom = panelsTop - 5;
        TacticalBoardTheme.raisedPanel(graphics, margin, contentTop,
                width - margin, stripBottom, TacticalBoardTheme.BOARD_ALT);
        TacticalBoardTheme.raisedPanel(graphics, slotX, panelsTop,
                slotX + slotWidth, panelsBottom, TacticalBoardTheme.BOARD_ALT);
        TacticalBoardTheme.raisedPanel(graphics, choiceX, panelsTop,
                choiceX + choiceWidth, panelsBottom, TacticalBoardTheme.BOARD_ALT);
        TacticalBoardTheme.sectionHeader(graphics, font,
                Component.translatable("screen.wok_infantry.loadout.slots"),
                slotX + 4, panelsTop + 4, slotX + slotWidth - 4,
                TacticalBoardTheme.SELECTED);
        TacticalBoardTheme.sectionHeader(graphics, font,
                Component.translatable("screen.wok_infantry.loadout.choices",
                        selectedSlot() == null ? Component.literal("未配置槽位")
                                : slotName(selectedSlot())),
                choiceX + 4, panelsTop + 4, choiceX + choiceWidth - 4,
                TacticalBoardTheme.ACCENT);

        if (rich) {
            renderDetailPanel(graphics);
        }

        LoadoutClassDefinition definition = selectedClass();
        if (definition == null) {
            BattleUiTheme.drawCenteredText(graphics, font,
                    Component.translatable("screen.wok_infantry.loadout.no_classes"),
                    choiceX + choiceWidth / 2, panelsTop + 48,
                    TacticalBoardTheme.MUTED_TEXT);
        } else if (selectedSlot() == null) {
            BattleUiTheme.drawCenteredText(graphics, font,
                    Component.literal("该职业尚未配置装备槽位"),
                    choiceX + choiceWidth / 2, panelsTop + 48,
                    TacticalBoardTheme.MUTED_TEXT);
        } else if (definition.entries(selectedSlotId).isEmpty()) {
            BattleUiTheme.drawCenteredText(graphics, font,
                    Component.translatable("screen.wok_infantry.loadout.no_entries"),
                    choiceX + choiceWidth / 2, panelsTop + 48,
                    TacticalBoardTheme.MUTED_TEXT);
        }

        int pagerY = panelsBottom - 23;
        BattleUiTheme.drawCenteredText(graphics, font,
                Component.translatable("screen.wok_infantry.loadout.page",
                        page + 1, candidatePageCount),
                choiceX + choiceWidth / 2, pagerY + 6,
                TacticalBoardTheme.MUTED_TEXT);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderDetailPanel(GuiGraphics graphics) {
        TacticalBoardTheme.raisedPanel(graphics, detailX, panelsTop,
                detailX + detailWidth, panelsBottom, TacticalBoardTheme.BOARD_ALT);
        TacticalBoardTheme.sectionHeader(graphics, font,
                Component.translatable("screen.wok_infantry.loadout.current"),
                detailX + 4, panelsTop + 4, detailX + detailWidth - 4,
                TacticalBoardTheme.SELECTED);

        int y = panelsTop + 24;
        LoadoutClassDefinition definition = selectedClass();
        List<LoadoutSlotDefinition> slots = definition == null
                ? List.of() : definition.slotDefinitions();
        for (LoadoutSlotDefinition slot : slots) {
            Component slotLabel = slotName(slot);
            LoadoutEntry selected = selectedEntry(slot);
            Component value = selected == null
                    ? Component.translatable("screen.wok_infantry.status.none")
                    : Component.literal(selected.displayName());
            graphics.drawString(font, slotLabel, detailX + 7, y,
                    TacticalBoardTheme.MUTED_TEXT, false);
            int valueX = detailX + Math.min(detailWidth / 2,
                    font.width(slotLabel) + 13);
            graphics.drawString(font, fittedButtonLabel(value,
                            detailX + detailWidth - valueX - 7),
                    valueX, y, selected == null
                            ? TacticalBoardTheme.MUTED_TEXT : TacticalBoardTheme.TEXT, false);
            y += 18;
        }

        LoadoutEntry selected = selectedEntry();
        int detailTop = Math.max(y + 8, panelsTop + 150);
        TacticalBoardTheme.sectionHeader(graphics, font,
                Component.translatable("screen.wok_infantry.loadout.selected"),
                detailX + 4, detailTop, detailX + detailWidth - 4,
                TacticalBoardTheme.ACCENT);
        if (selected != null) {
            int previewTop = detailTop + 18;
            graphics.fill(detailX + 7, previewTop,
                    detailX + detailWidth - 7, previewTop + 44,
                    TacticalBoardTheme.DEVICE_FRAME);
            BattleUiTheme.outline(graphics, detailX + 7, previewTop,
                    detailX + detailWidth - 7, previewTop + 44,
                    TacticalBoardTheme.BORDER);
            LoadoutEntryPreview.render(graphics, LoadoutEntryPreview.resolve(selected),
                    detailX + 11, previewTop + 3, detailWidth - 22, 38);
            graphics.drawString(font, fittedButtonLabel(Component.literal(selected.itemId()),
                            detailWidth - 14),
                    detailX + 7, previewTop + 48, TacticalBoardTheme.TEXT, false);
            graphics.drawString(font, Component.translatable(
                            "screen.wok_infantry.loadout.quantity", selected.count()),
                    detailX + 7, previewTop + 64, TacticalBoardTheme.MUTED_TEXT, false);
            graphics.drawString(font, Component.translatable(
                            selected.snbt().isBlank()
                                    ? "screen.wok_infantry.loadout.custom_data.none"
                                    : "screen.wok_infantry.loadout.custom_data.present"),
                    detailX + 7, previewTop + 80, TacticalBoardTheme.MUTED_TEXT, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(previous);
        }
    }

    public Screen returnScreen() {
        return previous;
    }

    private void openSquads() {
        if (minecraft == null) {
            return;
        }
        minecraft.setScreen(previous instanceof SquadScreen
                ? previous : new SquadScreen(previous));
    }

    private void refresh() {
        LoadoutNetwork.sendToServer(new OpenLoadoutPacket(false));
    }

    private void save(boolean apply) {
        if (selectedClass() == null) {
            return;
        }
        Map<String, String> selections = new LinkedHashMap<>(
                snapshot.player().selectionsFor(selectedClassId));
        LoadoutNetwork.sendToServer(new SavePlayerLoadoutPacket(
                selectedClassId, selections, apply));
        onClose();
    }

    private void refreshWidgets() {
        clearWidgets();
        init();
    }

    private List<LoadoutClassDefinition> enabledClasses() {
        return snapshot.config().classes().stream()
                .filter(LoadoutClassDefinition::enabled).toList();
    }

    private LoadoutClassDefinition selectedClass() {
        return snapshot.config().findClass(selectedClassId)
                .filter(LoadoutClassDefinition::enabled).orElse(null);
    }

    private LoadoutEntry selectedEntry() {
        return selectedEntry(selectedSlot());
    }

    private LoadoutEntry selectedEntry(LoadoutSlotDefinition slot) {
        LoadoutClassDefinition definition = selectedClass();
        if (definition == null || slot == null) {
            return null;
        }
        String selectedId = effectiveSelectedEntryId(definition, slot);
        return definition.entries(slot.id()).stream()
                .filter(entry -> entry.id().equals(selectedId)).findFirst().orElse(null);
    }

    private Component loadoutIdentity() {
        LoadoutClassDefinition definition = selectedClass();
        MutableComponent identity = definition == null
                ? Component.translatable("screen.wok_infantry.loadout.no_classes")
                : Component.literal(definition.displayName());
        LoadoutSlotDefinition slot = selectedSlot();
        return slot == null ? identity : identity.append(" · ").append(slotName(slot));
    }

    private Component fittedButtonLabel(Component label, int buttonWidth) {
        int available = Math.max(1, buttonWidth - 6);
        if (font.width(label) <= available) {
            return label;
        }
        String ellipsis = "…";
        int bodyWidth = Math.max(1, available - font.width(ellipsis));
        return Component.literal(font.plainSubstrByWidth(label.getString(), bodyWidth)
                + ellipsis);
    }

    private static Component slotName(LoadoutSlotDefinition slot) {
        return Component.literal(slot.displayName());
    }

    private LoadoutSlotDefinition selectedSlot() {
        LoadoutClassDefinition definition = selectedClass();
        return definition == null ? null : definition.findSlot(selectedSlotId).orElse(null);
    }

    private void reconcileSelectedSlot() {
        LoadoutClassDefinition definition = selectedClass();
        if (definition == null) {
            selectedSlotId = "";
            return;
        }
        if (definition.findSlot(selectedSlotId).isEmpty()) {
            selectedSlotId = definition.slotDefinitions().stream().findFirst()
                    .map(LoadoutSlotDefinition::id).orElse("");
        }
    }

    private String effectiveSelectedEntryId(LoadoutClassDefinition definition,
                                            LoadoutSlotDefinition slot) {
        String selectedId = snapshot.player().selectedEntry(selectedClassId, slot.id());
        if (definition.entries(slot.id()).stream().anyMatch(entry -> entry.id().equals(selectedId))) {
            return selectedId;
        }
        return definition.entries(slot.id()).stream().findFirst()
                .map(LoadoutEntry::id).orElse("");
    }

    private static String resolveInitialClass(LoadoutSnapshot snapshot) {
        return snapshot.config().findClass(snapshot.player().activeClassId())
                .filter(LoadoutClassDefinition::enabled)
                .map(LoadoutClassDefinition::id)
                .orElseGet(() -> snapshot.config().classes().stream()
                        .filter(LoadoutClassDefinition::enabled)
                        .map(LoadoutClassDefinition::id)
                        .findFirst().orElse(""));
    }
}
