package com.wok.infantry.client.screen;

import com.wok.infantry.loadout.LoadoutClassDefinition;
import com.wok.infantry.loadout.LoadoutEntry;
import com.wok.infantry.loadout.LoadoutSlot;
import com.wok.infantry.loadout.LoadoutSnapshot;
import com.wok.infantry.network.LoadoutNetwork;
import com.wok.infantry.network.serverbound.AdminClassPacket;
import com.wok.infantry.network.serverbound.AdminEntryPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class AdminLoadoutScreen extends Screen {
    private enum PageMode { LIST, ENTRY_EDITOR, CLASS_SETTINGS }

    private LoadoutSnapshot snapshot;
    private String selectedClassId;
    private LoadoutSlot selectedSlot = LoadoutSlot.PRIMARY;
    private PageMode mode = PageMode.LIST;
    private int page;
    private String originalEntryId = "";
    private boolean enabledDraft;

    private EditBox classNameField;
    private EditBox entryIdField;
    private EditBox displayNameField;
    private EditBox itemIdField;
    private EditBox countField;
    private EditBox snbtField;

    public AdminLoadoutScreen(LoadoutSnapshot snapshot) {
        super(Component.translatable("screen.wok_infantry.admin"));
        this.snapshot = snapshot;
        this.selectedClassId = snapshot.config().classes().stream()
                .findFirst().map(LoadoutClassDefinition::id).orElse("assault");
        LoadoutClassDefinition definition = selectedClass();
        this.enabledDraft = definition != null && definition.enabled();
    }

    public void replaceSnapshot(LoadoutSnapshot replacement) {
        snapshot = replacement;
        if (replacement.config().findClass(selectedClassId).isEmpty()) {
            selectedClassId = replacement.config().classes().stream()
                    .findFirst().map(LoadoutClassDefinition::id).orElse("assault");
        }
        LoadoutClassDefinition definition = selectedClass();
        enabledDraft = definition != null && definition.enabled();
        originalEntryId = "";
        mode = PageMode.LIST;
        rebuildAll();
    }

    @Override
    protected void init() {
        switch (mode) {
            case LIST -> initListPage();
            case ENTRY_EDITOR -> initEntryEditorPage();
            case CLASS_SETTINGS -> initClassSettingsPage();
        }
    }

    private void initListPage() {
        LoadoutClassDefinition definition = selectedClass();
        if (definition == null) {
            return;
        }

        int classCount = Math.max(1, snapshot.config().classes().size());
        int classWidth = Math.max(44, Math.min(100, (width - 16) / classCount));
        int classX = Math.max(8, (width - classWidth * classCount) / 2);
        for (int index = 0; index < snapshot.config().classes().size(); index++) {
            LoadoutClassDefinition classDefinition = snapshot.config().classes().get(index);
            Button button = Button.builder(Component.literal(classDefinition.displayName()), ignored -> {
                selectedClassId = classDefinition.id();
                enabledDraft = classDefinition.enabled();
                selectedSlot = LoadoutSlot.PRIMARY;
                page = 0;
                rebuildAll();
            }).bounds(classX + index * classWidth, 24, classWidth - 3, 20).build();
            button.active = !classDefinition.id().equals(selectedClassId);
            addRenderableWidget(button);
        }

        int slotWidth = Math.max(34, (width - 16) / LoadoutSlot.values().length);
        for (int index = 0; index < LoadoutSlot.values().length; index++) {
            LoadoutSlot slot = LoadoutSlot.values()[index];
            Button button = Button.builder(Component.literal(shortSlotName(slot)), ignored -> {
                selectedSlot = slot;
                page = 0;
                rebuildAll();
            }).bounds(8 + index * slotWidth, 49, slotWidth - 3, 20).build();
            button.active = slot != selectedSlot;
            addRenderableWidget(button);
        }

        List<LoadoutEntry> entries = definition.entries(selectedSlot);
        int pageSize = listPageSize();
        int pageCount = Math.max(1, (entries.size() + pageSize - 1) / pageSize);
        page = Math.max(0, Math.min(page, pageCount - 1));
        int start = page * pageSize;
        int end = Math.min(entries.size(), start + pageSize);
        for (int index = start; index < end; index++) {
            LoadoutEntry entry = entries.get(index);
            int y = 82 + (index - start) * 23;
            String label = entry.displayName() + "  §8" + entry.itemId();
            addRenderableWidget(Button.builder(Component.literal(label), ignored -> openEntry(entry))
                    .bounds(12, y, width - 24, 20).build());
        }

        int bottomY = height - 28;
        Button previous = Button.builder(Component.literal("<"), ignored -> {
            page--;
            rebuildAll();
        }).bounds(10, bottomY, 34, 20).build();
        previous.active = page > 0;
        addRenderableWidget(previous);
        Button next = Button.builder(Component.literal(">"), ignored -> {
            page++;
            rebuildAll();
        }).bounds(48, bottomY, 34, 20).build();
        next.active = page + 1 < pageCount;
        addRenderableWidget(next);

        int remaining = width - 94;
        int actionWidth = Math.max(70, (remaining - 8) / 2);
        addRenderableWidget(Button.builder(Component.literal("新建装备"), ignored -> openNewEntry())
                .bounds(90, bottomY, actionWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("兵种设置"), ignored -> openClassSettings())
                .bounds(94 + actionWidth, bottomY,
                        Math.max(70, width - (104 + actionWidth)), 20).build());
    }

    private void initEntryEditorPage() {
        int fieldX = Math.max(12, width / 2 - Math.min(210, width / 2 - 12));
        int fieldWidth = width - fieldX * 2;
        LoadoutEntry selected = findOriginalEntry();

        entryIdField = editBox(fieldX, 47, fieldWidth, "装备 ID", 64,
                selected == null ? "" : selected.id());
        displayNameField = editBox(fieldX, 75, fieldWidth, "显示名称", 80,
                selected == null ? "" : selected.displayName());
        itemIdField = editBox(fieldX, 103, fieldWidth, "物品注册名", 256,
                selected == null ? "minecraft:air" : selected.itemId());
        countField = editBox(fieldX, 131, Math.min(90, fieldWidth), "数量", 2,
                selected == null ? "1" : Integer.toString(selected.count()));
        snbtField = editBox(fieldX, 159, fieldWidth, "可选 SNBT", 32_767,
                selected == null ? "" : selected.snbt());
        addRenderableWidget(entryIdField);
        addRenderableWidget(displayNameField);
        addRenderableWidget(itemIdField);
        addRenderableWidget(countField);
        addRenderableWidget(snbtField);

        int bottomY = height - 28;
        int buttonWidth = Math.max(70, (width - 32) / 3);
        addRenderableWidget(Button.builder(Component.literal("保存"), ignored -> saveEntry())
                .bounds(10, bottomY, buttonWidth, 20).build());
        Button delete = Button.builder(Component.literal("删除"), ignored -> deleteEntry())
                .bounds(16 + buttonWidth, bottomY, buttonWidth, 20).build();
        delete.active = selected != null;
        addRenderableWidget(delete);
        addRenderableWidget(Button.builder(Component.literal("返回列表"), ignored -> backToList())
                .bounds(22 + buttonWidth * 2, bottomY,
                        Math.max(70, width - (32 + buttonWidth * 2)), 20).build());
    }

    private void initClassSettingsPage() {
        LoadoutClassDefinition definition = selectedClass();
        if (definition == null) {
            return;
        }
        int fieldWidth = Math.min(300, width - 40);
        int fieldX = (width - fieldWidth) / 2;
        classNameField = editBox(fieldX, 76, fieldWidth, "兵种显示名称", 40,
                definition.displayName());
        addRenderableWidget(classNameField);
        addRenderableWidget(Button.builder(enabledLabel(), button -> {
            enabledDraft = !enabledDraft;
            button.setMessage(enabledLabel());
        }).bounds(fieldX, 106, fieldWidth, 20).build());

        int bottomY = height - 28;
        int buttonWidth = Math.max(100, (width - 28) / 2);
        addRenderableWidget(Button.builder(Component.literal("保存兵种设置"), ignored -> saveClass())
                .bounds(10, bottomY, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("返回列表"), ignored -> backToList())
                .bounds(18 + buttonWidth, bottomY,
                        Math.max(100, width - (28 + buttonWidth)), 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(6, 5, width - 6, height - 5, 0xB0141922);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);

        switch (mode) {
            case LIST -> renderListPage(graphics);
            case ENTRY_EDITOR -> renderEntryEditorPage(graphics);
            case CLASS_SETTINGS -> renderClassSettingsPage(graphics);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderListPage(GuiGraphics graphics) {
        LoadoutClassDefinition definition = selectedClass();
        String className = definition == null ? "" : definition.displayName();
        graphics.drawString(font, className + " / " + selectedSlot.displayName(),
                12, 71, 0xFFD66B, false);
        if (definition != null && definition.entries(selectedSlot).isEmpty()) {
            graphics.drawCenteredString(font, "此槽位还没有候选装备",
                    width / 2, 105, 0x9AA4B2);
        }
    }

    private void renderEntryEditorPage(GuiGraphics graphics) {
        graphics.drawCenteredString(font,
                selectedClassName() + " / " + selectedSlot.displayName()
                        + (originalEntryId.isBlank() ? " / 新建" : " / 编辑"),
                width / 2, 24, 0xFFD66B);
        int fieldX = entryIdField == null ? 12 : entryIdField.getX();
        graphics.drawString(font, "装备 ID", fieldX, 37, 0xA8C7E8, false);
        graphics.drawString(font, "显示名称", fieldX, 65, 0xA8C7E8, false);
        graphics.drawString(font, "物品注册名", fieldX, 93, 0xA8C7E8, false);
        graphics.drawString(font, "数量", fieldX, 121, 0xA8C7E8, false);
        graphics.drawString(font, "可选 SNBT / TaCZ 数据", fieldX, 149, 0xA8C7E8, false);
    }

    private void renderClassSettingsPage(GuiGraphics graphics) {
        graphics.drawCenteredString(font, selectedClassName() + " / 兵种设置",
                width / 2, 28, 0xFFD66B);
        graphics.drawCenteredString(font, "内部 ID: " + selectedClassId,
                width / 2, 45, 0x9AA4B2);
        graphics.drawString(font, "兵种显示名称", classNameField.getX(), 66, 0xA8C7E8, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private EditBox editBox(int x, int y, int boxWidth, String narration,
                            int maxLength, String value) {
        EditBox box = new EditBox(font, x, y, Math.max(40, boxWidth), 20,
                Component.literal(narration));
        box.setMaxLength(maxLength);
        box.setValue(value);
        return box;
    }

    private void openEntry(LoadoutEntry entry) {
        originalEntryId = entry.id();
        mode = PageMode.ENTRY_EDITOR;
        rebuildAll();
    }

    private void openNewEntry() {
        originalEntryId = "";
        mode = PageMode.ENTRY_EDITOR;
        rebuildAll();
    }

    private void openClassSettings() {
        LoadoutClassDefinition definition = selectedClass();
        enabledDraft = definition != null && definition.enabled();
        mode = PageMode.CLASS_SETTINGS;
        rebuildAll();
    }

    private void backToList() {
        mode = PageMode.LIST;
        originalEntryId = "";
        rebuildAll();
    }

    private void saveEntry() {
        int count;
        try {
            count = Integer.parseInt(countField.getValue());
        } catch (NumberFormatException exception) {
            count = 0;
        }
        LoadoutEntry entry = new LoadoutEntry(entryIdField.getValue().trim(),
                displayNameField.getValue().trim(), itemIdField.getValue().trim(),
                count, snbtField.getValue().trim());
        LoadoutNetwork.sendToServer(AdminEntryPacket.upsert(
                selectedClassId, selectedSlot.id(), originalEntryId, entry));
    }

    private void deleteEntry() {
        if (!originalEntryId.isBlank()) {
            LoadoutNetwork.sendToServer(AdminEntryPacket.delete(
                    selectedClassId, selectedSlot.id(), originalEntryId));
        }
    }

    private void saveClass() {
        LoadoutNetwork.sendToServer(new AdminClassPacket(selectedClassId,
                classNameField.getValue().trim(), enabledDraft));
    }

    private Component enabledLabel() {
        return Component.literal(enabledDraft ? "状态：已启用" : "状态：已停用");
    }

    private int listPageSize() {
        return Math.max(3, Math.min(6, (height - 115) / 23));
    }

    private LoadoutClassDefinition selectedClass() {
        return snapshot.config().findClass(selectedClassId).orElse(null);
    }

    private String selectedClassName() {
        LoadoutClassDefinition definition = selectedClass();
        return definition == null ? selectedClassId : definition.displayName();
    }

    private LoadoutEntry findOriginalEntry() {
        LoadoutClassDefinition definition = selectedClass();
        if (definition == null || originalEntryId.isBlank()) {
            return null;
        }
        return definition.entries(selectedSlot).stream()
                .filter(entry -> entry.id().equals(originalEntryId))
                .findFirst().orElse(null);
    }

    private void rebuildAll() {
        clearWidgets();
        init();
    }

    private static String shortSlotName(LoadoutSlot slot) {
        return switch (slot) {
            case PRIMARY -> "主武器";
            case SECONDARY -> "副武器";
            case MELEE -> "近战";
            case GADGET_ONE -> "道具一";
            case GADGET_TWO -> "道具二";
            case THROWABLE -> "投掷物";
        };
    }
}
