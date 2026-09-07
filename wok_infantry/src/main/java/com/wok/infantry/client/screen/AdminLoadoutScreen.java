package com.wok.infantry.client.screen;

import com.wok.infantry.formation.FactionDefinition;
import com.wok.infantry.formation.FormationClassEditAction;
import com.wok.infantry.formation.FormationClassRule;
import com.wok.infantry.formation.FormationDefinition;
import com.wok.infantry.formation.FormationLoadoutEditAction;
import com.wok.infantry.loadout.LoadoutClassDefinition;
import com.wok.infantry.loadout.LoadoutEntry;
import com.wok.infantry.loadout.LoadoutInventoryTarget;
import com.wok.infantry.loadout.LoadoutSlotDefinition;
import com.wok.infantry.loadout.LoadoutSnapshot;
import com.wok.infantry.network.LoadoutNetwork;
import com.wok.infantry.network.serverbound.AdminClassPacket;
import com.wok.infantry.network.serverbound.AdminEntryPacket;
import com.wok.infantry.network.serverbound.AdminFormationLoadoutPacket;
import com.wok.infantry.network.serverbound.AdminClassLoadoutCopyPacket;
import com.wok.infantry.network.serverbound.AdminSlotPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;

public final class AdminLoadoutScreen extends Screen {
    private enum PageMode { LIST, ENTRY_EDITOR, SLOT_EDITOR, CLASS_SETTINGS }

    private static ClassLoadoutClipboard classLoadoutClipboard;

    private LoadoutSnapshot snapshot;
    private String selectedFactionId = "";
    private String selectedFormationId = "";
    private String selectedClassId;
    private String selectedSlotId = "primary";
    private PageMode mode = PageMode.LIST;
    private int page;
    private int slotPage;
    private String originalEntryId = "";
    private boolean creatingClass;
    private boolean creatingSlot;
    private boolean reopenClassSettingsAfterRefresh;
    private int pendingPastedClassIndex = -1;
    private LoadoutInventoryTarget editedTarget = LoadoutInventoryTarget.HOTBAR_1;
    private boolean editedRequired = true;

    private EditBox classNameField;
    private EditBox classLimitField;
    private EditBox entryAmmoLimitField;
    private EditBox entryIdField;
    private EditBox displayNameField;
    private EditBox itemIdField;
    private EditBox countField;
    private EditBox snbtField;
    private EditBox slotIdField;
    private EditBox slotNameField;

    public AdminLoadoutScreen(LoadoutSnapshot snapshot) {
        super(Component.translatable("screen.wok_infantry.admin"));
        this.snapshot = snapshot;
        AdminLoadoutSessionState.Selection remembered = AdminLoadoutSessionState.load();
        this.selectedFactionId = remembered.factionId();
        this.selectedFormationId = remembered.formationId();
        this.selectedClassId = remembered.classId();
        this.selectedSlotId = remembered.slotId();
        reconcileFormationSelection();
        reconcileSelectedClass();
        reconcileSelectedSlot();
        rememberSelection();
    }

    public void replaceSnapshot(LoadoutSnapshot replacement) {
        boolean reopenClassSettings = reopenClassSettingsAfterRefresh;
        reopenClassSettingsAfterRefresh = false;
        snapshot = replacement;
        reconcileFormationSelection();
        if (pendingPastedClassIndex >= 0) {
            FormationDefinition refreshedTarget = selectedFormation();
            if (refreshedTarget != null
                    && pendingPastedClassIndex < refreshedTarget.classes().size()) {
                selectedClassId = refreshedTarget.classes()
                        .get(pendingPastedClassIndex).classId();
            }
            pendingPastedClassIndex = -1;
        }
        if (replacement.config().findClass(selectedClassId).isEmpty()) {
            selectedClassId = replacement.config().classes().stream()
                    .findFirst().map(LoadoutClassDefinition::id).orElse("assault");
        }
        reconcileSelectedClass();
        reconcileSelectedSlot();
        originalEntryId = "";
        creatingClass = false;
        creatingSlot = false;
        mode = reopenClassSettings && selectedFormationRule() != null
                ? PageMode.CLASS_SETTINGS : PageMode.LIST;
        rebuildAll();
    }

    @Override
    protected void init() {
        switch (mode) {
            case LIST -> initListPage();
            case ENTRY_EDITOR -> initEntryEditorPage();
            case SLOT_EDITOR -> initSlotEditorPage();
            case CLASS_SETTINGS -> initClassSettingsPage();
        }
    }

    private void initListPage() {
        addRenderableWidget(BattleUiButton.builder(Component.literal("导入 / 导出"), ignored -> {
            if (minecraft != null) minecraft.setScreen(new CatalogTransferScreen(this));
        }).kind(BattleUiButton.Kind.CONTROL).bounds(width - 88, 4, 80, 17).build());
        LoadoutClassDefinition definition = selectedClass();
        if (definition == null) {
            return;
        }

        FactionDefinition faction = selectedFaction();
        FormationDefinition formation = selectedFormation();
        int utilityWidth = 24;
        int contextGap = 3;
        int pasteX = width - 8 - utilityWidth;
        int copyX = pasteX - contextGap - utilityWidth;
        int contextWidth = Math.max(70,
                (copyX - contextGap * 2 - 8) / 2);
        Button factionButton = BattleUiButton.builder(
                        fittedButtonLabel(Component.literal("阵营："
                                + (faction == null ? "未选择" : faction.displayName())),
                                contextWidth), ignored -> cycleFaction())
                .bounds(8, 24, contextWidth, 20).build();
        factionButton.active = snapshot.formations().factions().size() > 1;
        factionButton.setTooltip(Tooltip.create(Component.literal(
                "当前阵营：" + (faction == null ? "未选择" : faction.displayName())
                        + "\n点击切换阵营")));
        addRenderableWidget(factionButton);
        int formationX = 11 + contextWidth;
        int formationWidth = Math.max(70, copyX - contextGap - formationX);
        Button formationButton = BattleUiButton.builder(
                        fittedButtonLabel(Component.literal("编制："
                                + (formation == null ? "未选择" : formation.displayName())),
                                formationWidth), ignored -> cycleFormation())
                .bounds(formationX, 24, formationWidth, 20).build();
        formationButton.active = faction != null && faction.formations().size() > 1;
        formationButton.setTooltip(Tooltip.create(Component.literal(
                "当前编制：" + (formation == null ? "未选择" : formation.displayName())
                        + "\n点击切换编制")));
        addRenderableWidget(formationButton);
        Button copyClass = BattleUiButton.builder(Component.literal("复"),
                        ignored -> copyCurrentClassLoadout())
                .kind(BattleUiButton.Kind.CONTROL)
                .bounds(copyX, 24, utilityWidth, 20).build();
        copyClass.active = formation != null && selectedFormationRule() != null;
        copyClass.setTooltip(Tooltip.create(Component.literal(
                !copyClass.active ? "当前没有可复制的兵种配装"
                        : "复制当前兵种的装备槽位、物品、SNBT 与白名单\n当前："
                        + faction.displayName() + " / " + formation.displayName()
                        + " / " + selectedClassName())));
        addRenderableWidget(copyClass);
        Button pasteClass = BattleUiButton.builder(Component.literal("贴"),
                        ignored -> confirmPasteClassLoadout())
                .kind(BattleUiButton.Kind.DANGER)
                .bounds(pasteX, 24, utilityWidth, 20).build();
        boolean sameClass = classLoadoutClipboard != null
                && classLoadoutClipboard.factionId().equals(selectedFactionId)
                && classLoadoutClipboard.formationId().equals(selectedFormationId)
                && classLoadoutClipboard.classId().equals(selectedClassId);
        pasteClass.active = formation != null && selectedFormationRule() != null
                && classLoadoutClipboard != null && !sameClass;
        pasteClass.setTooltip(Tooltip.create(Component.literal(
                classLoadoutClipboard == null ? "请先复制一个兵种配装"
                        : sameClass ? "源兵种与目标兵种相同，不能粘贴"
                        : "把“" + classLoadoutClipboard.displayName()
                        + "”的独立配装副本粘贴到当前兵种\n粘贴前会再次确认")));
        addRenderableWidget(pasteClass);

        List<LoadoutClassDefinition> availableClasses = availableClasses();
        initClassSelector(availableClasses);

        List<LoadoutSlotDefinition> slots = definition.slotDefinitions();
        int previousX = 8;
        int tabX = 36;
        int settingsX = width - 50;
        int addX = settingsX - 46;
        int nextX = addX - 28;
        int tabAreaWidth = Math.max(44, nextX - tabX - 4);
        int slotsPerPage = Math.max(1, tabAreaWidth / 52);
        int slotPageCount = Math.max(1, (slots.size() + slotsPerPage - 1) / slotsPerPage);
        slotPage = Math.max(0, Math.min(slotPage, slotPageCount - 1));
        int slotStart = slotPage * slotsPerPage;
        int slotEnd = Math.min(slots.size(), slotStart + slotsPerPage);
        int visibleSlots = Math.max(1, slotEnd - slotStart);
        int slotWidth = Math.max(40, Math.min(96, tabAreaWidth / visibleSlots));
        Button previousSlotPage = BattleUiButton.builder(Component.literal("‹"), ignored -> {
            slotPage--;
            if (!slots.isEmpty()) {
                selectedSlotId = slots.get(Math.max(0, slotPage * slotsPerPage)).id();
                page = 0;
            }
            rebuildAll();
        }).bounds(previousX, 74, 24, 20).build();
        previousSlotPage.active = slotPage > 0;
        addRenderableWidget(previousSlotPage);
        for (int index = slotStart; index < slotEnd; index++) {
            LoadoutSlotDefinition slot = slots.get(index);
            Button button = BattleUiButton.builder(
                    fittedButtonLabel(Component.literal(slot.displayName()), slotWidth - 3), ignored -> {
                        selectedSlotId = slot.id();
                        page = 0;
                        rebuildAll();
                    }).bounds(tabX + (index - slotStart) * slotWidth, 74,
                    slotWidth - 3, 20).build();
            button.active = !slot.id().equals(selectedSlotId);
            button.setTooltip(Tooltip.create(Component.literal(slot.displayName() + "\n目标："
                    + slot.target().displayName() + (slot.required() ? "\n必需槽位" : "\n可选槽位"))));
            addRenderableWidget(button);
        }
        Button nextSlotPage = BattleUiButton.builder(Component.literal("›"), ignored -> {
            slotPage++;
            if (!slots.isEmpty()) {
                selectedSlotId = slots.get(Math.min(slots.size() - 1,
                        slotPage * slotsPerPage)).id();
                page = 0;
            }
            rebuildAll();
        }).bounds(nextX, 74, 24, 20).build();
        nextSlotPage.active = slotPage + 1 < slotPageCount;
        addRenderableWidget(nextSlotPage);
        Button addSlot = BattleUiButton.builder(Component.literal("+槽位"), ignored -> openNewSlot())
                .bounds(addX, 74, 42, 20).build();
        addSlot.active = slots.size() < LoadoutClassDefinition.MAX_SLOTS;
        addRenderableWidget(addSlot);
        Button editSlot = BattleUiButton.builder(Component.literal("设置"), ignored -> openSlotEditor())
                .bounds(settingsX, 74, 42, 20).build();
        editSlot.active = selectedSlot() != null;
        addRenderableWidget(editSlot);

        LoadoutSlotDefinition selectedSlot = selectedSlot();
        List<LoadoutEntry> entries = selectedSlot == null
                ? List.of() : definition.entries(selectedSlot.id());
        FormationClassRule rule = selectedFormationRule();
        List<String> strictEntries = rule == null
                || selectedSlot == null ? List.of() : rule.allowedEntriesFor(selectedSlot.id());
        boolean strict = !strictEntries.isEmpty();
        int pageSize = listPageSize();
        int pageCount = Math.max(1, (entries.size() + pageSize - 1) / pageSize);
        page = Math.max(0, Math.min(page, pageCount - 1));
        int start = page * pageSize;
        int end = Math.min(entries.size(), start + pageSize);
        for (int index = start; index < end; index++) {
            LoadoutEntry entry = entries.get(index);
            int y = 107 + (index - start) * 23;
            LoadoutPreviewButton button = new LoadoutPreviewButton(
                    12, y, Math.max(80, width - 82), 20, entry, false,
                    ignored -> openEntry(entry));
            button.setTooltip(Tooltip.create(Component.literal(
                    entry.displayName() + "\n" + entry.itemId() + " ×" + entry.count())));
            addRenderableWidget(button);
            boolean allowed = rule != null && selectedSlot != null
                    && rule.allowsEntry(selectedSlot.id(), entry.id());
            FormationLoadoutEditAction action = !strict
                    ? FormationLoadoutEditAction.EXCLUSIVE
                    : allowed ? FormationLoadoutEditAction.REMOVE
                    : FormationLoadoutEditAction.ADD;
            String actionName = !strict ? "仅此" : allowed ? "移出" : "加入";
            Button scopeButton = BattleUiButton.builder(Component.literal(actionName),
                            ignored -> editFormationRule(entry.id(), action))
                    .bounds(width - 66, y, 54, 20).build();
            scopeButton.active = rule != null
                    && !(action == FormationLoadoutEditAction.REMOVE
                    && strictEntries.size() == 1);
            scopeButton.setTooltip(Tooltip.create(Component.literal(switch (action) {
                case EXCLUSIVE -> "将当前编制的此槽位限定为该装备";
                case ADD -> "把该装备加入当前编制白名单";
                case REMOVE -> strictEntries.size() == 1
                        ? "受限槽位至少保留一个装备；请先加入其他装备"
                        : "从当前编制白名单移除该装备";
                default -> "调整当前编制白名单";
            })));
            addRenderableWidget(scopeButton);
        }

        int bottomY = height - 28;
        Button previous = BattleUiButton.builder(Component.literal("<"), ignored -> {
            page--;
            rebuildAll();
        }).bounds(10, bottomY, 34, 20).build();
        previous.active = page > 0;
        addRenderableWidget(previous);
        Button next = BattleUiButton.builder(Component.literal(">"), ignored -> {
            page++;
            rebuildAll();
        }).bounds(48, bottomY, 34, 20).build();
        next.active = page + 1 < pageCount;
        addRenderableWidget(next);

        int remaining = width - 94;
        int actionWidth = Math.max(48, (remaining - 12) / 4);
        Button newEntry = BattleUiButton.builder(
                        fittedButtonLabel(Component.literal("新建装备"), actionWidth),
                        ignored -> openNewEntry())
                .bounds(90, bottomY, actionWidth, 20).build();
        newEntry.active = selectedSlot != null;
        addRenderableWidget(newEntry);
        Button capture = BattleUiButton.builder(
                        fittedButtonLabel(Component.literal("读取主手"), actionWidth),
                        ignored -> captureMainHand("", "", ""))
                .bounds(94 + actionWidth, bottomY, actionWidth, 20).build();
        capture.setTooltip(Tooltip.create(Component.literal(
                "将主手物品及 TaCZ 配件数据新增到当前槽位，\n并加入当前编制的严格白名单")));
        capture.active = selectedSlot != null;
        addRenderableWidget(capture);
        Button allowAll = BattleUiButton.builder(
                        fittedButtonLabel(Component.literal("全部开放"), actionWidth),
                        ignored -> editFormationRule("",
                                FormationLoadoutEditAction.ALLOW_ALL))
                .bounds(98 + actionWidth * 2, bottomY, actionWidth, 20).build();
        allowAll.active = rule != null && strict;
        allowAll.setTooltip(Tooltip.create(Component.literal(
                "取消当前编制对此槽位的限制，允许全局装备池中的全部条目")));
        addRenderableWidget(allowAll);
        int classSettingsX = 102 + actionWidth * 3;
        addRenderableWidget(BattleUiButton.builder(
                        fittedButtonLabel(Component.literal("职业管理"),
                                Math.max(48, width - classSettingsX - 10)),
                        ignored -> openClassSettings())
                .bounds(classSettingsX, bottomY,
                        Math.max(48, width - classSettingsX - 10), 20).build());
    }

    private void initClassSelector(List<LoadoutClassDefinition> classes) {
        if (classes.isEmpty()) {
            return;
        }
        int maxVisible = Math.max(1, (width - 16) / 44);
        if (classes.size() <= maxVisible) {
            int classWidth = Math.max(44, Math.min(100, (width - 16) / classes.size()));
            int classX = Math.max(8, (width - classWidth * classes.size()) / 2);
            for (int index = 0; index < classes.size(); index++) {
                LoadoutClassDefinition definition = classes.get(index);
                Button button = BattleUiButton.builder(
                                Component.literal(classDisplayName(definition)),
                                ignored -> selectClass(definition.id()))
                        .bounds(classX + index * classWidth, 49,
                                classWidth - 3, 20).build();
                button.active = !definition.id().equals(selectedClassId);
                addRenderableWidget(button);
            }
            return;
        }
        addRenderableWidget(BattleUiButton.builder(Component.literal("<"),
                        ignored -> cycleClass(-1))
                .bounds(8, 49, 34, 20).build());
        Button selected = BattleUiButton.builder(
                        fittedButtonLabel(Component.literal("职业：" + selectedClassName()),
                                Math.max(44, width - 100)), ignored -> cycleClass(1))
                .bounds(47, 49, Math.max(44, width - 100), 20).build();
        selected.setTooltip(Tooltip.create(Component.literal(
                "当前编制共有 " + classes.size() + " 个职业；点击切换")));
        addRenderableWidget(selected);
        addRenderableWidget(BattleUiButton.builder(Component.literal(">"),
                        ignored -> cycleClass(1))
                .bounds(width - 48, 49, 40, 20).build());
    }

    private void selectClass(String classId) {
        selectedClassId = classId;
        selectedSlotId = "";
        reconcileSelectedSlot();
        slotPage = 0;
        page = 0;
        rebuildAll();
    }

    private void cycleClass(int direction) {
        List<LoadoutClassDefinition> classes = availableClasses();
        if (classes.isEmpty()) {
            return;
        }
        int current = 0;
        for (int index = 0; index < classes.size(); index++) {
            if (classes.get(index).id().equals(selectedClassId)) {
                current = index;
                break;
            }
        }
        int next = Math.floorMod(current + direction, classes.size());
        selectClass(classes.get(next).id());
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
        entryAmmoLimitField = editBox(fieldX + Math.min(90, fieldWidth) + 10, 131,
                Math.max(80, fieldWidth - Math.min(90, fieldWidth) - 10),
                "该枪械弹药上限", 4, Integer.toString(selected == null
                        ? LoadoutEntry.DEFAULT_AMMO_RESERVE_LIMIT
                        : selected.ammoReserveLimit()));
        snbtField = editBox(fieldX, 159, fieldWidth, "可选 SNBT", 32_767,
                selected == null ? "" : selected.snbt());
        addRenderableWidget(entryIdField);
        addRenderableWidget(displayNameField);
        addRenderableWidget(itemIdField);
        addRenderableWidget(countField);
        addRenderableWidget(entryAmmoLimitField);
        addRenderableWidget(snbtField);

        int bottomY = height - 28;
        int buttonWidth = Math.max(56, (width - 38) / 4);
        Button capture = BattleUiButton.builder(Component.literal("读取主手"),
                        ignored -> captureMainHand(originalEntryId,
                                entryIdField.getValue().trim(),
                                displayNameField.getValue().trim()))
                .bounds(10, bottomY, buttonWidth, 20).build();
        capture.setTooltip(Tooltip.create(Component.literal(
                "使用当前主手物品覆盖物品、数量与 TaCZ 配件数据并保存")));
        addRenderableWidget(capture);
        addRenderableWidget(BattleUiButton.builder(Component.literal("保存"), ignored -> saveEntry())
                .bounds(16 + buttonWidth, bottomY, buttonWidth, 20).build());
        Button delete = BattleUiButton.builder(Component.literal("删除"), ignored -> deleteEntry())
                .bounds(22 + buttonWidth * 2, bottomY, buttonWidth, 20).build();
        delete.active = selected != null;
        addRenderableWidget(delete);
        addRenderableWidget(BattleUiButton.builder(Component.literal("返回"), ignored -> backToList())
                .bounds(28 + buttonWidth * 3, bottomY,
                        Math.max(56, width - (38 + buttonWidth * 3)), 20).build());
    }

    private void initSlotEditorPage() {
        LoadoutSlotDefinition selected = selectedSlot();
        if (!creatingSlot && selected == null) {
            return;
        }
        int fieldWidth = Math.min(320, width - 40);
        int fieldX = (width - fieldWidth) / 2;
        String proposedId = creatingSlot ? "custom_"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                : selected.id();
        slotIdField = editBox(fieldX, 72, fieldWidth, "槽位内部 ID", 64, proposedId);
        slotIdField.active = creatingSlot;
        slotNameField = editBox(fieldX, 102, fieldWidth, "槽位显示名称", 40,
                creatingSlot ? "新装备槽位" : selected.displayName());
        addRenderableWidget(slotIdField);
        addRenderableWidget(slotNameField);

        Button target = BattleUiButton.builder(Component.literal("目标栏位："
                        + editedTarget.displayName()), button -> {
                            cycleEditedTarget();
                            button.setMessage(Component.literal("目标栏位："
                                    + editedTarget.displayName()));
                        })
                .bounds(fieldX, 134, fieldWidth, 20).build();
        target.setTooltip(Tooltip.create(Component.literal(
                "每个职业内，同一个物理栏位只能分配给一个装备槽位")));
        addRenderableWidget(target);
        Button required = BattleUiButton.builder(Component.literal(
                        editedRequired ? "必需：是（缺少装备时禁止部署）" : "必需：否（空槽会跳过）"),
                        button -> {
                            editedRequired = !editedRequired;
                            button.setMessage(Component.literal(editedRequired
                                    ? "必需：是（缺少装备时禁止部署）"
                                    : "必需：否（空槽会跳过）"));
                        }).bounds(fieldX, 160, fieldWidth, 20).build();
        addRenderableWidget(required);

        int bottomY = height - 28;
        if (creatingSlot) {
            int buttonWidth = Math.max(70, (width - 26) / 2);
            addRenderableWidget(BattleUiButton.builder(Component.literal("创建槽位"),
                            ignored -> saveSlot())
                    .bounds(8, bottomY, buttonWidth, 20).build());
            addRenderableWidget(BattleUiButton.builder(Component.literal("返回"),
                            ignored -> backToList())
                    .bounds(18 + buttonWidth, bottomY,
                            Math.max(70, width - buttonWidth - 26), 20).build());
            return;
        }

        int buttonWidth = Math.max(44, (width - 48) / 5);
        addRenderableWidget(BattleUiButton.builder(Component.literal("保存"),
                        ignored -> saveSlot())
                .bounds(8, bottomY, buttonWidth, 20).build());
        addRenderableWidget(BattleUiButton.builder(Component.literal("左移"),
                        ignored -> moveSlot(AdminSlotPacket.Action.MOVE_LEFT))
                .bounds(16 + buttonWidth, bottomY, buttonWidth, 20).build());
        addRenderableWidget(BattleUiButton.builder(Component.literal("右移"),
                        ignored -> moveSlot(AdminSlotPacket.Action.MOVE_RIGHT))
                .bounds(24 + buttonWidth * 2, bottomY, buttonWidth, 20).build());
        addRenderableWidget(BattleUiButton.builder(Component.literal("删除槽位"),
                        ignored -> deleteSlot())
                .bounds(32 + buttonWidth * 3, bottomY, buttonWidth, 20).build());
        addRenderableWidget(BattleUiButton.builder(Component.literal("返回"),
                        ignored -> backToList())
                .bounds(40 + buttonWidth * 4, bottomY,
                        Math.max(44, width - 48 - buttonWidth * 4), 20).build());
    }

    private void initClassSettingsPage() {
        LoadoutClassDefinition definition = selectedClass();
        FormationClassRule rule = selectedFormationRule();
        if (!creatingClass && (definition == null || rule == null)) {
            return;
        }
        int fieldWidth = Math.min(300, width - 40);
        int fieldX = (width - fieldWidth) / 2;
        classNameField = editBox(fieldX, 76, fieldWidth, "职业显示名称", 40,
                creatingClass ? "" : classDisplayName(definition));
        addRenderableWidget(classNameField);
        int limitWidth = Math.min(100, fieldWidth);
        classLimitField = editBox(fieldX, 106, limitWidth,
                "每小队名额", 1, Integer.toString(creatingClass ? 1 : rule.squadLimit()));
        addRenderableWidget(classLimitField);
        FormationDefinition formation = selectedFormation();
        int classIndex = formation == null ? -1 : java.util.stream.IntStream
                .range(0, formation.classes().size())
                .filter(index -> formation.classes().get(index).classId().equals(selectedClassId))
                .findFirst().orElse(-1);
        int orderX = fieldX + limitWidth + 8;
        int orderAreaWidth = Math.max(80, fieldX + fieldWidth - orderX);
        int orderButtonWidth = Math.max(38, (orderAreaWidth - 6) / 2);
        Button moveUp = BattleUiButton.builder(
                        fittedButtonLabel(Component.literal("↑ 上移"), orderButtonWidth),
                        ignored -> moveClass(FormationClassEditAction.MOVE_UP))
                .bounds(orderX, 106, orderButtonWidth, 20).build();
        moveUp.active = !creatingClass && classIndex > 0;
        moveUp.setTooltip(Tooltip.create(Component.literal(
                "将该职业向列表前方移动；只改变显示顺序，不会放宽职业名额")));
        addRenderableWidget(moveUp);
        Button moveDown = BattleUiButton.builder(
                        fittedButtonLabel(Component.literal("↓ 下移"), orderButtonWidth),
                        ignored -> moveClass(FormationClassEditAction.MOVE_DOWN))
                .bounds(orderX + orderButtonWidth + 6, 106,
                        orderButtonWidth, 20).build();
        moveDown.active = !creatingClass && formation != null
                && classIndex >= 0 && classIndex + 1 < formation.classes().size();
        moveDown.setTooltip(Tooltip.create(Component.literal(
                "将该职业向列表后方移动；玩家职业选择页会使用相同顺序")));
        addRenderableWidget(moveDown);

        int bottomY = height - 28;
        int buttonWidth = Math.max(52, (width - 50) / 4);
        Button create = BattleUiButton.builder(
                        fittedButtonLabel(Component.literal("新建职业"), buttonWidth),
                        ignored -> startCreatingClass())
                .bounds(10, bottomY, buttonWidth, 20).build();
        create.active = !creatingClass && selectedFormation() != null
                && selectedFormation().classes().size() < FormationDefinition.MAX_CLASSES;
        addRenderableWidget(create);
        addRenderableWidget(BattleUiButton.builder(
                        fittedButtonLabel(Component.literal(creatingClass ? "创建" : "保存"),
                                buttonWidth), ignored -> saveClass())
                .bounds(20 + buttonWidth, bottomY, buttonWidth, 20).build());
        Button delete = BattleUiButton.builder(
                        fittedButtonLabel(Component.literal("删除职业"), buttonWidth),
                        ignored -> deleteClass())
                .bounds(30 + buttonWidth * 2, bottomY, buttonWidth, 20).build();
        delete.active = !creatingClass && formation != null && formation.classes().size() > 1;
        delete.setTooltip(Tooltip.create(Component.literal(
                delete.active
                        ? "从当前编制彻底删除该职业；系统会保留一个可覆盖全队的兜底职业"
                        : "每个编制至少保留一个职业；请先新建其他职业")));
        addRenderableWidget(delete);
        addRenderableWidget(BattleUiButton.builder(
                        fittedButtonLabel(Component.literal("返回"), buttonWidth),
                        ignored -> backToList())
                .bounds(40 + buttonWidth * 3, bottomY,
                        Math.max(52, width - (50 + buttonWidth * 3)), 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(6, 5, width - 6, height - 5, 0xB0141922);
        if (mode == PageMode.LIST) {
            graphics.drawString(font, font.plainSubstrByWidth(title.getString(), width - 108),
                    12, 8, 0xFFFFFF, false);
        } else {
            BattleUiTheme.drawCenteredText(graphics, font, title, width / 2, 8, 0xFFFFFF);
        }

        switch (mode) {
            case LIST -> renderListPage(graphics);
            case ENTRY_EDITOR -> renderEntryEditorPage(graphics);
            case SLOT_EDITOR -> renderSlotEditorPage(graphics);
            case CLASS_SETTINGS -> renderClassSettingsPage(graphics);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderListPage(GuiGraphics graphics) {
        LoadoutClassDefinition definition = selectedClass();
        LoadoutSlotDefinition selectedSlot = selectedSlot();
        String className = definition == null ? "" : classDisplayName(definition);
        FormationDefinition formation = selectedFormation();
        String context = (formation == null ? "未选择编制" : formation.displayName())
                + " / " + className + " / "
                + (selectedSlot == null ? "请新增装备槽位" : selectedSlot.displayName()
                + " → " + selectedSlot.target().displayName());
        graphics.drawString(font, fittedText(context, width - 24),
                12, 96, 0xFFD66B, false);
        if (definition != null && selectedSlot != null
                && definition.entries(selectedSlot.id()).isEmpty()) {
            BattleUiTheme.drawCenteredText(graphics, font, "此槽位还没有候选装备",
                    width / 2, 130, 0x9AA4B2);
        }
    }

    private void renderEntryEditorPage(GuiGraphics graphics) {
        LoadoutSlotDefinition selectedSlot = selectedSlot();
        BattleUiTheme.drawCenteredText(graphics, font,
                selectedClassName() + " / "
                        + (selectedSlot == null ? "未选择槽位" : selectedSlot.displayName())
                        + (originalEntryId.isBlank() ? " / 新建" : " / 编辑"),
                width / 2, 24, 0xFFD66B);
        int fieldX = entryIdField == null ? 12 : entryIdField.getX();
        graphics.drawString(font, "装备 ID", fieldX, 37, 0xA8C7E8, false);
        graphics.drawString(font, "显示名称", fieldX, 65, 0xA8C7E8, false);
        graphics.drawString(font, "物品注册名", fieldX, 93, 0xA8C7E8, false);
        graphics.drawString(font, "数量", fieldX, 121, 0xA8C7E8, false);
        graphics.drawString(font, "该枪械携带/补给弹药上限（1–4096 发）",
                entryAmmoLimitField.getX(), 121, 0xA8C7E8, false);
        graphics.drawString(font, "可选 SNBT / TaCZ 数据", fieldX, 149, 0xA8C7E8, false);
    }

    private void renderSlotEditorPage(GuiGraphics graphics) {
        BattleUiTheme.drawCenteredText(graphics, font,
                creatingSlot ? selectedClassName() + " / 新建装备槽位"
                        : selectedClassName() + " / 编辑装备槽位",
                width / 2, 26, 0xFFD66B);
        BattleUiTheme.drawCenteredText(graphics, font,
                "可自定义名称、顺序、必需状态及快捷栏/护甲目标",
                width / 2, 44, 0x9AA4B2);
        graphics.drawString(font, "槽位内部 ID（创建后固定）", slotIdField.getX(),
                62, 0xA8C7E8, false);
        graphics.drawString(font, "槽位显示名称", slotNameField.getX(),
                92, 0xA8C7E8, false);
    }

    private void renderClassSettingsPage(GuiGraphics graphics) {
        BattleUiTheme.drawCenteredText(graphics, font, creatingClass
                        ? "新建编制职业" : selectedClassName() + " / 职业设置",
                width / 2, 28, 0xFFD66B);
        BattleUiTheme.drawCenteredText(graphics, font, creatingClass
                        ? "新职业只属于当前所选编制"
                        : "内部 ID: " + selectedClassId,
                width / 2, 45, 0x9AA4B2);
        graphics.drawString(font, "职业显示名称", classNameField.getX(), 66, 0xA8C7E8, false);
        graphics.drawString(font, "每小队名额（1–8）", classLimitField.getX(), 96, 0xA8C7E8, false);
        graphics.drawString(font, "显示顺序", classLimitField.getX()
                + classLimitField.getWidth() + 8, 96, 0xA8C7E8, false);
        FormationDefinition formation = selectedFormation();
        if (!creatingClass && formation != null) {
            int position = java.util.stream.IntStream.range(0, formation.classes().size())
                    .filter(index -> formation.classes().get(index).classId()
                            .equals(selectedClassId))
                    .findFirst().orElse(-1);
            if (position >= 0) {
                BattleUiTheme.drawCenteredText(graphics, font,
                        "当前第 " + (position + 1) + " / " + formation.classes().size()
                                + "；排序不改变职业人数上限",
                        width / 2, 166, 0x9AA4B2);
            }
        }
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
        if (selectedSlot() == null) {
            return;
        }
        originalEntryId = "";
        mode = PageMode.ENTRY_EDITOR;
        rebuildAll();
    }

    private void openNewSlot() {
        LoadoutInventoryTarget free = firstFreeTarget();
        if (free == null) {
            return;
        }
        creatingSlot = true;
        editedTarget = free;
        editedRequired = true;
        mode = PageMode.SLOT_EDITOR;
        rebuildAll();
    }

    private void openSlotEditor() {
        LoadoutSlotDefinition slot = selectedSlot();
        if (slot == null) {
            return;
        }
        creatingSlot = false;
        editedTarget = slot.target();
        editedRequired = slot.required();
        mode = PageMode.SLOT_EDITOR;
        rebuildAll();
    }

    private void openClassSettings() {
        creatingClass = false;
        mode = PageMode.CLASS_SETTINGS;
        rebuildAll();
    }

    private void startCreatingClass() {
        creatingClass = true;
        rebuildAll();
    }

    private void backToList() {
        mode = PageMode.LIST;
        originalEntryId = "";
        creatingSlot = false;
        rebuildAll();
    }

    private void saveEntry() {
        int count;
        try {
            count = Integer.parseInt(countField.getValue());
        } catch (NumberFormatException exception) {
            count = 0;
        }
        int ammoReserveLimit = parsedEntryAmmoLimit();
        LoadoutEntry entry = new LoadoutEntry(entryIdField.getValue().trim(),
                displayNameField.getValue().trim(), itemIdField.getValue().trim(),
                count, snbtField.getValue().trim(), ammoReserveLimit);
        LoadoutNetwork.sendToServer(AdminEntryPacket.upsert(
                selectedClassId, selectedSlotId, originalEntryId, entry,
                selectedFactionId, selectedFormationId));
    }

    private void deleteEntry() {
        if (!originalEntryId.isBlank()) {
            LoadoutNetwork.sendToServer(AdminEntryPacket.delete(
                    selectedClassId, selectedSlotId, originalEntryId));
        }
    }

    private void captureMainHand(String originalId, String requestedId,
                                 String requestedDisplayName) {
        LoadoutNetwork.sendToServer(AdminEntryPacket.captureMainHand(
                selectedClassId, selectedSlotId, originalId,
                requestedId, requestedDisplayName,
                selectedFactionId, selectedFormationId, parsedEntryAmmoLimit()));
    }

    private int parsedEntryAmmoLimit() {
        if (entryAmmoLimitField == null) {
            return LoadoutEntry.DEFAULT_AMMO_RESERVE_LIMIT;
        }
        try {
            return Integer.parseInt(entryAmmoLimitField.getValue());
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private void editFormationRule(String entryId, FormationLoadoutEditAction action) {
        if (selectedFaction() == null || selectedFormation() == null) {
            return;
        }
        LoadoutNetwork.sendToServer(new AdminFormationLoadoutPacket(action,
                selectedFactionId, selectedFormationId, selectedClassId,
                selectedSlotId, entryId));
    }

    private void saveSlot() {
        String slotId = slotIdField.getValue().trim();
        selectedSlotId = slotId;
        LoadoutNetwork.sendToServer(new AdminSlotPacket(
                creatingSlot ? AdminSlotPacket.Action.CREATE : AdminSlotPacket.Action.UPDATE,
                selectedClassId, slotId, slotNameField.getValue().trim(),
                editedTarget, editedRequired));
    }

    private void moveSlot(AdminSlotPacket.Action action) {
        LoadoutSlotDefinition slot = selectedSlot();
        if (slot != null) {
            LoadoutNetwork.sendToServer(new AdminSlotPacket(action, selectedClassId,
                    slot.id(), slot.displayName(), slot.target(), slot.required()));
        }
    }

    private void deleteSlot() {
        LoadoutSlotDefinition slot = selectedSlot();
        if (slot != null) {
            LoadoutNetwork.sendToServer(new AdminSlotPacket(AdminSlotPacket.Action.DELETE,
                    selectedClassId, slot.id(), slot.displayName(), slot.target(),
                    slot.required()));
        }
    }

    private void cycleEditedTarget() {
        LoadoutInventoryTarget[] targets = LoadoutInventoryTarget.values();
        int start = editedTarget.ordinal();
        for (int step = 1; step <= targets.length; step++) {
            LoadoutInventoryTarget candidate = targets[(start + step) % targets.length];
            if (!targetUsedByOtherSlot(candidate)) {
                editedTarget = candidate;
                return;
            }
        }
    }

    private void saveClass() {
        int squadLimit;
        try {
            squadLimit = Integer.parseInt(classLimitField.getValue());
        } catch (NumberFormatException exception) {
            squadLimit = 0;
        }
        String classId = creatingClass
                ? "custom_" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12)
                : selectedClassId;
        selectedClassId = classId;
        LoadoutNetwork.sendToServer(new AdminClassPacket(
                creatingClass ? FormationClassEditAction.CREATE
                        : FormationClassEditAction.UPDATE,
                selectedFactionId, selectedFormationId, classId,
                classNameField.getValue().trim(), squadLimit));
    }

    private void deleteClass() {
        FormationDefinition formation = selectedFormation();
        if (!creatingClass && formation != null && formation.classes().size() > 1) {
            LoadoutNetwork.sendToServer(new AdminClassPacket(
                    FormationClassEditAction.DELETE, selectedFactionId,
                    selectedFormationId, selectedClassId, "", 1));
        }
    }

    private void moveClass(FormationClassEditAction action) {
        FormationClassRule rule = selectedFormationRule();
        if (creatingClass || rule == null || (action != FormationClassEditAction.MOVE_UP
                && action != FormationClassEditAction.MOVE_DOWN)) {
            return;
        }
        reopenClassSettingsAfterRefresh = true;
        LoadoutNetwork.sendToServer(new AdminClassPacket(action, selectedFactionId,
                selectedFormationId, selectedClassId, rule.displayName(), rule.squadLimit()));
    }

    private void copyCurrentClassLoadout() {
        FactionDefinition faction = selectedFaction();
        FormationDefinition formation = selectedFormation();
        FormationClassRule rule = selectedFormationRule();
        if (faction == null || formation == null || rule == null) {
            return;
        }
        classLoadoutClipboard = new ClassLoadoutClipboard(faction.id(), formation.id(),
                rule.classId(), faction.displayName() + " / " + formation.displayName()
                + " / " + selectedClassName());
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.literal(
                    "已复制兵种配装：" + classLoadoutClipboard.displayName()), false);
        }
        rebuildAll();
    }

    private void confirmPasteClassLoadout() {
        FormationDefinition target = selectedFormation();
        FormationClassRule targetRule = selectedFormationRule();
        ClassLoadoutClipboard copied = classLoadoutClipboard;
        if (minecraft == null || target == null || targetRule == null || copied == null
                || copied.factionId().equals(selectedFactionId)
                && copied.formationId().equals(selectedFormationId)
                && copied.classId().equals(selectedClassId)) {
            return;
        }
        String targetName = selectedFaction().displayName() + " / " + target.displayName()
                + " / " + selectedClassName();
        Component title = Component.literal("确认替换当前兵种配装？");
        Component message = Component.literal("源：" + copied.displayName()
                + "\n目标：" + targetName
                + "\n只替换目标兵种的槽位、装备、SNBT 和白名单；名称、顺序与名额不变。");
        minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (minecraft != null) {
                minecraft.setScreen(this);
            }
            if (confirmed) {
                pendingPastedClassIndex = Math.max(0, target.classes().indexOf(targetRule));
                LoadoutNetwork.sendToServer(new AdminClassLoadoutCopyPacket(
                        copied.factionId(), copied.formationId(), copied.classId(),
                        selectedFactionId, selectedFormationId, selectedClassId));
            }
        }, title, message));
    }

    private int listPageSize() {
        return Math.max(2, Math.min(6, (height - 140) / 23));
    }

    private void reconcileFormationSelection() {
        List<FactionDefinition> factions = snapshot.formations().factions();
        FactionDefinition faction = factions.stream()
                .filter(candidate -> candidate.id().equals(selectedFactionId))
                .findFirst().orElseGet(() -> factions.stream().findFirst().orElse(null));
        selectedFactionId = faction == null ? "" : faction.id();
        FormationDefinition formation = faction == null ? null : faction.formations().stream()
                .filter(candidate -> candidate.id().equals(selectedFormationId))
                .findFirst().orElseGet(() -> faction.formations().stream()
                        .findFirst().orElse(null));
        selectedFormationId = formation == null ? "" : formation.id();
    }

    private void reconcileSelectedClass() {
        List<LoadoutClassDefinition> classes = availableClasses();
        if (classes.stream().noneMatch(definition -> definition.id().equals(selectedClassId))) {
            selectedClassId = classes.stream().findFirst()
                    .map(LoadoutClassDefinition::id).orElse("assault");
        }
    }

    private void cycleFaction() {
        List<FactionDefinition> factions = snapshot.formations().factions();
        if (factions.isEmpty()) {
            return;
        }
        int current = 0;
        for (int index = 0; index < factions.size(); index++) {
            if (factions.get(index).id().equals(selectedFactionId)) {
                current = index;
                break;
            }
        }
        FactionDefinition next = factions.get((current + 1) % factions.size());
        selectedFactionId = next.id();
        selectedFormationId = next.formations().stream().findFirst()
                .map(FormationDefinition::id).orElse("");
        reconcileSelectedClass();
        selectedSlotId = "";
        reconcileSelectedSlot();
        slotPage = 0;
        page = 0;
        rebuildAll();
    }

    private void cycleFormation() {
        FactionDefinition faction = selectedFaction();
        if (faction == null || faction.formations().isEmpty()) {
            return;
        }
        List<FormationDefinition> formations = faction.formations();
        int current = 0;
        for (int index = 0; index < formations.size(); index++) {
            if (formations.get(index).id().equals(selectedFormationId)) {
                current = index;
                break;
            }
        }
        selectedFormationId = formations.get((current + 1) % formations.size()).id();
        reconcileSelectedClass();
        selectedSlotId = "";
        reconcileSelectedSlot();
        slotPage = 0;
        page = 0;
        rebuildAll();
    }

    private List<LoadoutClassDefinition> availableClasses() {
        FormationDefinition formation = selectedFormation();
        if (formation == null) {
            return snapshot.config().classes();
        }
        return formation.classes().stream()
                .map(rule -> snapshot.config().findClass(rule.classId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private FactionDefinition selectedFaction() {
        return snapshot.formations().findFaction(selectedFactionId).orElse(null);
    }

    private FormationDefinition selectedFormation() {
        return snapshot.formations().findFormation(selectedFactionId,
                selectedFormationId).orElse(null);
    }

    private FormationClassRule selectedFormationRule() {
        FormationDefinition formation = selectedFormation();
        return formation == null ? null : formation.findClass(selectedClassId).orElse(null);
    }

    private LoadoutClassDefinition selectedClass() {
        return snapshot.config().findClass(selectedClassId).orElse(null);
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

    private LoadoutInventoryTarget firstFreeTarget() {
        for (LoadoutInventoryTarget target : LoadoutInventoryTarget.values()) {
            LoadoutClassDefinition definition = selectedClass();
            if (definition == null || definition.slotDefinitions().stream()
                    .noneMatch(slot -> slot.target() == target)) {
                return target;
            }
        }
        return null;
    }

    private boolean targetUsedByOtherSlot(LoadoutInventoryTarget target) {
        LoadoutClassDefinition definition = selectedClass();
        if (definition == null) {
            return false;
        }
        return definition.slotDefinitions().stream().anyMatch(slot ->
                (creatingSlot || !slot.id().equals(selectedSlotId)) && slot.target() == target);
    }

    private String selectedClassName() {
        LoadoutClassDefinition definition = selectedClass();
        return definition == null ? selectedClassId : classDisplayName(definition);
    }

    private String classDisplayName(LoadoutClassDefinition definition) {
        FormationDefinition formation = selectedFormation();
        String scoped = formation == null ? "" : formation.findClass(definition.id())
                .map(FormationClassRule::displayName).orElse("");
        return scoped.isBlank() ? definition.displayName() : scoped;
    }

    private LoadoutEntry findOriginalEntry() {
        LoadoutClassDefinition definition = selectedClass();
        if (definition == null || originalEntryId.isBlank()) {
            return null;
        }
        return definition.entries(selectedSlotId).stream()
                .filter(entry -> entry.id().equals(originalEntryId))
                .findFirst().orElse(null);
    }

    private void rebuildAll() {
        rememberSelection();
        clearWidgets();
        init();
    }

    private void rememberSelection() {
        AdminLoadoutSessionState.remember(selectedFactionId, selectedFormationId,
                selectedClassId, selectedSlotId);
    }

    @Override
    public void onClose() {
        rememberSelection();
        super.onClose();
    }

    private record ClassLoadoutClipboard(String factionId, String formationId,
                                         String classId, String displayName) {
    }

    private Component fittedButtonLabel(Component label, int buttonWidth) {
        return Component.literal(fittedText(label.getString(), Math.max(1, buttonWidth - 8)));
    }

    private String fittedText(String value, int maxWidth) {
        if (font.width(value) <= maxWidth) {
            return value;
        }
        String ellipsis = "…";
        return font.plainSubstrByWidth(value,
                Math.max(1, maxWidth - font.width(ellipsis))) + ellipsis;
    }

}
