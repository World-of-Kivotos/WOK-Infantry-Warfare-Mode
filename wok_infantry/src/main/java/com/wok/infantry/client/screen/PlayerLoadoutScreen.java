package com.wok.infantry.client.screen;

import com.wok.infantry.loadout.LoadoutClassDefinition;
import com.wok.infantry.loadout.LoadoutEntry;
import com.wok.infantry.loadout.LoadoutSlot;
import com.wok.infantry.loadout.LoadoutSnapshot;
import com.wok.infantry.network.LoadoutNetwork;
import com.wok.infantry.network.serverbound.OpenLoadoutPacket;
import com.wok.infantry.network.serverbound.SavePlayerLoadoutPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlayerLoadoutScreen extends Screen {
    private final LoadoutSnapshot snapshot;
    private String selectedClassId;
    private LoadoutSlot selectedSlot = LoadoutSlot.PRIMARY;
    private int page;

    public PlayerLoadoutScreen(LoadoutSnapshot snapshot) {
        super(Component.translatable("screen.wok_infantry.loadout"));
        this.snapshot = snapshot;
        this.selectedClassId = resolveInitialClass(snapshot);
    }

    @Override
    protected void init() {
        List<LoadoutClassDefinition> classes = enabledClasses();
        int tabWidth = Math.max(48, Math.min(110,
                (width - 20) / Math.max(1, classes.size())));
        int tabsWidth = classes.size() * tabWidth;
        int tabX = Math.max(10, (width - tabsWidth) / 2);
        for (int index = 0; index < classes.size(); index++) {
            LoadoutClassDefinition definition = classes.get(index);
            Button button = Button.builder(Component.literal(definition.displayName()), ignored -> {
                selectedClassId = definition.id();
                page = 0;
                refreshWidgets();
            }).bounds(tabX + index * tabWidth, 26, tabWidth - 4, 20).build();
            button.active = !definition.id().equals(selectedClassId);
            addRenderableWidget(button);
        }

        boolean compact = width < 520;
        int slotX = 10;
        int slotWidth = compact ? 102 : 124;
        int slotY = 62;
        for (LoadoutSlot slot : LoadoutSlot.values()) {
            Button button = Button.builder(Component.literal(slot.displayName()), ignored -> {
                selectedSlot = slot;
                page = 0;
                refreshWidgets();
            }).bounds(slotX, slotY, slotWidth, 22).build();
            button.active = slot != selectedSlot;
            addRenderableWidget(button);
            slotY += 25;
        }

        LoadoutClassDefinition definition = selectedClass();
        if (definition != null) {
            List<LoadoutEntry> entries = definition.entries(selectedSlot);
            int columns = compact ? 1 : 2;
            int pageSize = compact ? 4 : 8;
            int start = Math.min(page * pageSize, Math.max(0, entries.size() - 1));
            int end = Math.min(entries.size(), start + pageSize);
            int choiceX = slotX + slotWidth + 12;
            int availableWidth = Math.max(80, width - choiceX - 10);
            int choiceWidth = Math.max(40, (availableWidth - (columns - 1) * 4) / columns);
            for (int index = start; index < end; index++) {
                LoadoutEntry entry = entries.get(index);
                int local = index - start;
                int x = choiceX + local % columns * (choiceWidth + 4);
                int y = 65 + local / columns * 29;
                boolean selected = entry.id().equals(
                        snapshot.player().selectedEntry(selectedClassId, selectedSlot));
                Component label = Component.literal((selected ? "✓ " : "") + entry.displayName());
                addRenderableWidget(Button.builder(label, ignored -> {
                    snapshot.player().select(selectedClassId, selectedSlot, entry.id());
                    refreshWidgets();
                }).bounds(x, y, choiceWidth, 24).build());
            }

            int pageCount = Math.max(1, (entries.size() + pageSize - 1) / pageSize);
            Button previous = Button.builder(Component.literal("<"), ignored -> {
                page--;
                refreshWidgets();
            }).bounds(choiceX, height - 54, 34, 20).build();
            previous.active = page > 0;
            addRenderableWidget(previous);
            Button next = Button.builder(Component.literal(">"), ignored -> {
                page++;
                refreshWidgets();
            }).bounds(choiceX + 38, height - 54, 34, 20).build();
            next.active = page + 1 < pageCount;
            addRenderableWidget(next);
        }

        int bottomY = height - 28;
        addRenderableWidget(Button.builder(Component.literal("保存"), ignored -> save(false))
                .bounds(width / 2 - 154, bottomY, 98, 20).build());
        addRenderableWidget(Button.builder(Component.literal("保存并发放"), ignored -> save(true))
                .bounds(width / 2 - 50, bottomY, 120, 20).build());
        addRenderableWidget(Button.builder(Component.literal("刷新"), ignored ->
                        LoadoutNetwork.sendToServer(new OpenLoadoutPacket(false)))
                .bounds(width / 2 + 76, bottomY, 78, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(8, 18, width - 8, 50, 0xB0181D26);
        boolean compact = width < 520;
        int slotWidth = compact ? 102 : 124;
        int choiceX = 10 + slotWidth + 12;
        graphics.fill(6, 54, choiceX - 6, height - 34, 0xA8141820);
        graphics.fill(choiceX - 2, 54, width - 6, height - 34, 0xA8141820);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        graphics.drawString(font, selectedSlot.displayName() + "候选装备", choiceX, 54, 0xFFD66B, false);

        LoadoutEntry selected = selectedEntry();
        if (selected != null && !compact) {
            graphics.drawString(font, "物品: " + selected.itemId(), choiceX + 80,
                    height - 51, 0xA8C7E8, false);
            graphics.drawString(font, "数量: " + selected.count()
                            + (selected.snbt().isBlank() ? "" : "  含自定义数据"),
                    choiceX + 80, height - 40, 0x9AA4B2, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void save(boolean apply) {
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
        return snapshot.config().classes().stream().filter(LoadoutClassDefinition::enabled).toList();
    }

    private LoadoutClassDefinition selectedClass() {
        return snapshot.config().findClass(selectedClassId).orElse(null);
    }

    private LoadoutEntry selectedEntry() {
        LoadoutClassDefinition definition = selectedClass();
        if (definition == null) {
            return null;
        }
        String selectedId = snapshot.player().selectedEntry(selectedClassId, selectedSlot);
        return definition.entries(selectedSlot).stream()
                .filter(entry -> entry.id().equals(selectedId)).findFirst().orElse(null);
    }

    private static String resolveInitialClass(LoadoutSnapshot snapshot) {
        return snapshot.config().findClass(snapshot.player().activeClassId())
                .filter(LoadoutClassDefinition::enabled)
                .map(LoadoutClassDefinition::id)
                .orElseGet(() -> snapshot.config().classes().stream()
                        .filter(LoadoutClassDefinition::enabled)
                        .map(LoadoutClassDefinition::id)
                        .findFirst().orElse("assault"));
    }
}
