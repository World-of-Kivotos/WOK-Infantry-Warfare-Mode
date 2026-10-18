package com.wok.infantry.client.screen;

import com.wok.infantry.client.ClientLoadoutState;
import com.wok.infantry.configtransfer.CatalogTransferAction;
import com.wok.infantry.configtransfer.CatalogTransferResult;
import com.wok.infantry.network.LoadoutNetwork;
import com.wok.infantry.network.serverbound.AdminCatalogTransferPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Compact tablet with a server-validated preview before replacement. */
public final class CatalogTransferScreen extends Screen {
    private static final AtomicInteger REQUESTS = new AtomicInteger();
    private final Screen parent;
    private final List<Button> actions = new ArrayList<>();
    private TacticalMapLayout.Layout board;
    private EditBox nameField;
    private Button confirm;
    private String fileName = "catalog-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    private String message = "导出所有管理员阵营与配装。导入时先预览，再确认整体替换。";
    private String token = "";
    private List<String> files = List.of();
    private int fileIndex = -1;
    private int filePage;
    private int filePages = 1;
    private int requestId;
    private int scroll;
    private int ticksPending;
    private boolean busy;
    private boolean success = true;
    private CatalogTransferAction pendingAction;
    private Layout layout;

    public CatalogTransferScreen(Screen parent) {
        super(Component.literal("阵营与配装 · 导入导出"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        board = TacticalMapLayout.compute(width, height);
        layout = computeLayout(width, height);
        actions.clear();
        int left = layout.left();
        int right = layout.right();
        nameField = new EditBox(font, left + 8, layout.fieldY(), right - left - 76, 20,
                Component.literal("数据包文件名"));
        nameField.setMaxLength(69);
        nameField.setValue(fileName);
        nameField.setResponder(value -> {
            fileName = value;
            token = "";
            updateButtons();
        });
        addRenderableWidget(nameField);
        button("‹", right - 64, layout.fieldY(), 25, BattleUiButton.Kind.CONTROL, () -> browse(-1));
        button("›", right - 35, layout.fieldY(), 25, BattleUiButton.Kind.CONTROL, () -> browse(1));
        int actionWidth = (right - left - 24) / 3;
        button("导出全部", left + 8, layout.buttonsY(), actionWidth, BattleUiButton.Kind.SUCCESS,
                () -> request(CatalogTransferAction.EXPORT));
        button("预览导入", left + 12 + actionWidth, layout.buttonsY(), actionWidth, BattleUiButton.Kind.CONTROL,
                () -> request(CatalogTransferAction.PREVIEW));
        button("文件列表", left + 16 + actionWidth * 2, layout.buttonsY(), actionWidth,
                BattleUiButton.Kind.CONTROL, () -> { filePage = 0; request(CatalogTransferAction.LIST); });
        confirm = button("确认整体替换", left + 8, layout.actionsY(), (right - left - 20) / 2,
                BattleUiButton.Kind.DANGER, () -> request(CatalogTransferAction.IMPORT));
        confirm.setTooltip(Tooltip.create(Component.literal("先预览数据包。确认后自动备份当前配置，并整体替换阵营与配装。")));
        addRenderableWidget(BattleUiButton.builder(Component.literal("返回配装管理"), ignored -> onClose())
                .bounds(left + 12 + (right - left - 20) / 2, layout.actionsY(),
                        (right - left - 20) / 2, 20).build());
        updateButtons();
    }

    private Button button(String text, int x, int y, int width, BattleUiButton.Kind kind, Runnable action) {
        Button button = BattleUiButton.builder(Component.literal(text), ignored -> action.run())
                .kind(kind).bounds(x, y, width, 20).build();
        actions.add(button);
        addRenderableWidget(button);
        return button;
    }

    private void browse(int direction) {
        token = "";
        if (files.isEmpty()) { request(CatalogTransferAction.LIST); return; }
        int next = fileIndex + direction;
        if (next >= 0 && next < files.size()) {
            fileIndex = next;
            nameField.setValue(files.get(fileIndex));
        } else {
            filePage = Math.floorMod(filePage + direction, filePages);
            request(CatalogTransferAction.LIST);
        }
    }

    private void request(CatalogTransferAction action) {
        if (busy) return;
        String confirmation = token;
        token = "";
        busy = true;
        ticksPending = 0;
        success = true;
        message = "正在处理服务端数据包…";
        scroll = 0;
        pendingAction = action;
        requestId = REQUESTS.incrementAndGet();
        updateButtons();
        LoadoutNetwork.sendToServer(new AdminCatalogTransferPacket(requestId, action, fileName,
                action == CatalogTransferAction.IMPORT ? confirmation : "", filePage));
    }

    public void acceptResult(int id, CatalogTransferResult result) {
        if (id != requestId || !busy) return;
        busy = false;
        success = result.success();
        message = result.message();
        scroll = 0;
        if (pendingAction == CatalogTransferAction.LIST && result.success()) {
            files = result.files();
            filePage = result.page();
            filePages = Math.max(1, result.pages());
            fileIndex = files.isEmpty() ? -1 : 0;
            if (fileIndex >= 0) nameField.setValue(files.get(fileIndex));
            message = files.isEmpty() ? "服务端暂无数据包；先导出或放入 catalogs 目录。"
                    : "文件第 " + (filePage + 1) + "/" + filePages + " 页；使用 ‹ › 切换文件。\n" + message;
        }
        token = result.success() && pendingAction == CatalogTransferAction.PREVIEW ? result.token() : "";
        updateButtons();
    }

    private void updateButtons() {
        if (nameField == null) return;
        nameField.setEditable(!busy);
        for (Button button : actions) button.active = !busy;
        if (confirm != null) confirm.active = !busy && !token.isEmpty();
    }

    @Override
    public void tick() {
        nameField.tick();
        if (busy && ++ticksPending >= 600) {
            busy = false;
            token = "";
            success = false;
            message = "服务端响应超时。操作可能已完成，请检查文件或重新打开管理界面后再预览。";
            updateButtons();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        TacticalBoardChrome.renderShell(graphics, width, height, board);
        TacticalBoardChrome.renderHeader(graphics, font, board, title, Component.literal("ADMIN"), true);
        TacticalBoardTheme.raisedPanel(graphics, layout.left(), layout.top(), layout.right(),
                layout.bottom(), TacticalBoardTheme.BOARD_ALT);
        line(graphics, "全量：阵营 / 编制 / 兵种 / 槽位 / 装备 / 载具规则", layout.top() + 7, TacticalBoardTheme.TEXT);
        line(graphics, "服务端：config/wok_infantry/catalogs", layout.top() + 20, TacticalBoardTheme.MUTED_TEXT);
        line(graphics, "文件名（.json 可省略；‹ › 浏览服务端文件）", layout.fieldY() - 12, TacticalBoardTheme.TEXT);
        int textWidth = layout.right() - layout.left() - 16;
        var lines = font.split(Component.literal(message), textWidth);
        int visible = Math.max(1, (layout.actionsY() - 7 - layout.statusY()) / 11);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, lines.size() - visible)));
        graphics.enableScissor(layout.left() + 6, layout.statusY(), layout.right() - 6, layout.actionsY() - 5);
        for (int index = scroll; index < Math.min(lines.size(), scroll + visible); index++) {
            graphics.drawString(font, lines.get(index), layout.left() + 8,
                    layout.statusY() + (index - scroll) * 11,
                    success ? TacticalBoardTheme.TEXT : TacticalBoardTheme.DANGER, false);
        }
        graphics.disableScissor();
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(font, font.plainSubstrByWidth("管理员预设 · 不包含玩家记录 · 长消息可滚动",
                        board.footer().width() - 10), board.footer().left() + 5, board.footer().top() + 4,
                TacticalBoardTheme.MUTED_TEXT, false);
    }

    private void line(GuiGraphics graphics, String value, int y, int color) {
        graphics.drawString(font, font.plainSubstrByWidth(value, layout.right() - layout.left() - 16),
                layout.left() + 8, y, color, false);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        scroll += delta < 0 ? 1 : -1;
        return true;
    }

    @Override
    public void onClose() {
        if (minecraft == null) return;
        var snapshot = ClientLoadoutState.snapshot();
        minecraft.setScreen(snapshot != null && snapshot.administrator() ? new AdminLoadoutScreen(snapshot) : parent);
    }

    @Override public boolean isPauseScreen() { return false; }

    static Layout computeLayout(int width, int height) {
        var board = TacticalMapLayout.compute(width, height);
        int panelWidth = Math.min(680, width - 16);
        int left = (width - panelWidth) / 2;
        int top = board.header().bottom() + 4;
        int bottom = board.footer().top() - 4;
        return new Layout(left, left + panelWidth, top, bottom, top + 48,
                top + 73, top + 101, bottom - 26);
    }

    record Layout(int left, int right, int top, int bottom, int fieldY, int buttonsY, int statusY, int actionsY) {}
}
