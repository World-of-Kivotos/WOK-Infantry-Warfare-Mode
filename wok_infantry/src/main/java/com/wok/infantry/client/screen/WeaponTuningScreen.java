package com.wok.infantry.client.screen;

import com.wok.infantry.integration.tacz.WeaponTuning;
import com.wok.infantry.network.battle.BattleNetwork;
import com.wok.infantry.network.battle.packet.c2s.ApplyWeaponTuningPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.UUID;

/** Responsive tactical-tablet editor whose values belong to one physical gun ItemStack. */
public final class WeaponTuningScreen extends Screen {
    private static final int SLIDER_HEIGHT = 20;
    private static final int RESET_WIDTH = 52;

    private final Screen parent;
    private final UUID weaponId;
    private final Component weaponName;
    private final WeaponTuning initial;
    private TacticalMapLayout.Layout boardLayout;
    private EditorLayout editorLayout;
    private TacticalBoardSlider adsSpeed;
    private TacticalBoardSlider verticalRecoil;
    private TacticalBoardSlider horizontalRecoil;
    private TacticalBoardSlider spread;

    public WeaponTuningScreen(Screen parent, UUID weaponId, Component weaponName,
                              WeaponTuning initial) {
        super(Component.translatable("screen.wok_infantry.weapon_tuning"));
        this.parent = parent;
        this.weaponId = Objects.requireNonNull(weaponId, "weaponId");
        this.weaponName = Objects.requireNonNull(weaponName, "weaponName");
        this.initial = Objects.requireNonNull(initial, "initial");
    }

    @Override
    protected void init() {
        boardLayout = TacticalMapLayout.compute(width, height);
        editorLayout = computeEditorLayout(width, height, boardLayout);
        int row = 0;
        adsSpeed = addTuningRow(row++, Component.translatable(
                        "screen.wok_infantry.weapon_tuning.ads_speed"), initial.adsSpeedScale(),
                WeaponTuning.MAX_STANDARD_SCALE);
        verticalRecoil = addTuningRow(row++, Component.translatable(
                        "screen.wok_infantry.weapon_tuning.vertical_recoil"),
                initial.verticalRecoilScale(), WeaponTuning.MAX_RECOIL_SCALE);
        horizontalRecoil = addTuningRow(row++, Component.translatable(
                        "screen.wok_infantry.weapon_tuning.horizontal_recoil"),
                initial.horizontalRecoilScale(), WeaponTuning.MAX_RECOIL_SCALE);
        spread = addTuningRow(row, Component.translatable(
                        "screen.wok_infantry.weapon_tuning.spread"), initial.spreadScale(),
                WeaponTuning.MAX_STANDARD_SCALE);
        addActionButtons();
    }

    private TacticalBoardSlider addTuningRow(int row, Component label, float value,
                                              float maximum) {
        int y = editorLayout.rowsTop() + row * editorLayout.rowHeight();
        int sliderLeft = editorLayout.left() + 8;
        int resetLeft = editorLayout.right() - RESET_WIDTH - 8;
        TacticalBoardSlider slider = new TacticalBoardSlider(sliderLeft, y,
                Math.max(80, resetLeft - sliderLeft - 4), SLIDER_HEIGHT, label,
                WeaponTuning.MIN_SCALE, maximum, 0.05D, value,
                ignored -> {
                });
        addRenderableWidget(slider);
        addRenderableWidget(BattleUiButton.builder(Component.translatable(
                        "screen.wok_infantry.weapon_tuning.reset_one"),
                ignored -> slider.setSelectedValue(WeaponTuning.DEFAULT_SCALE))
                .kind(BattleUiButton.Kind.CONTROL)
                .bounds(resetLeft, y, RESET_WIDTH, SLIDER_HEIGHT).build());
        return slider;
    }

    private void addActionButtons() {
        int gap = 4;
        int available = editorLayout.right() - editorLayout.left() - 16 - gap * 2;
        int buttonWidth = Math.max(56, available / 3);
        int left = editorLayout.left() + 8;
        int y = editorLayout.actionsTop();
        addRenderableWidget(BattleUiButton.builder(Component.translatable(
                        "screen.wok_infantry.weapon_tuning.apply"), ignored -> applyAndClose())
                .kind(BattleUiButton.Kind.SUCCESS)
                .bounds(left, y, buttonWidth, 22).build());
        addRenderableWidget(BattleUiButton.builder(Component.translatable(
                        "screen.wok_infantry.weapon_tuning.reset_all"), ignored -> {
                    adsSpeed.setSelectedValue(WeaponTuning.DEFAULT_SCALE);
                    verticalRecoil.setSelectedValue(WeaponTuning.DEFAULT_SCALE);
                    horizontalRecoil.setSelectedValue(WeaponTuning.DEFAULT_SCALE);
                    spread.setSelectedValue(WeaponTuning.DEFAULT_SCALE);
                }).kind(BattleUiButton.Kind.DANGER)
                .bounds(left + buttonWidth + gap, y, buttonWidth, 22).build());
        addRenderableWidget(BattleUiButton.builder(Component.translatable(
                        "screen.wok_infantry.weapon_tuning.cancel"), ignored -> onClose())
                .bounds(left + (buttonWidth + gap) * 2, y,
                        editorLayout.right() - 8 - left - (buttonWidth + gap) * 2, 22)
                .build());
    }

    private void applyAndClose() {
        WeaponTuning tuning = new WeaponTuning((float) adsSpeed.selectedValue(),
                (float) verticalRecoil.selectedValue(),
                (float) horizontalRecoil.selectedValue(),
                (float) spread.selectedValue());
        BattleNetwork.sendToServer(new ApplyWeaponTuningPacket(weaponId, tuning));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        TacticalBoardChrome.renderShell(graphics, width, height, boardLayout);
        TacticalBoardChrome.renderHeader(graphics, font, boardLayout,
                Component.translatable("screen.wok_infantry.weapon_tuning.board_title"),
                Component.translatable("screen.wok_infantry.weapon_tuning.identity"), true);
        TacticalBoardTheme.raisedPanel(graphics, editorLayout.left(), editorLayout.top(),
                editorLayout.right(), editorLayout.bottom(), TacticalBoardTheme.BOARD_ALT);
        TacticalBoardTheme.sectionHeader(graphics, font, weaponName,
                editorLayout.left() + 4, editorLayout.top() + 4,
                editorLayout.right() - 4, TacticalBoardTheme.ACCENT);
        Component hint = Component.translatable("screen.wok_infantry.weapon_tuning.hint");
        graphics.drawString(font, font.plainSubstrByWidth(hint.getString(),
                        editorLayout.right() - editorLayout.left() - 18),
                editorLayout.left() + 9, editorLayout.top() + 21,
                TacticalBoardTheme.MUTED_TEXT, false);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(font, Component.translatable(
                        "screen.wok_infantry.weapon_tuning.footer"),
                boardLayout.footer().left() + 5, boardLayout.footer().top() + 4,
                TacticalBoardTheme.MUTED_TEXT, false);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    static EditorLayout computeEditorLayout(int width, int height,
                                            TacticalMapLayout.Layout board) {
        int panelWidth = Math.min(680, Math.max(240, width - (width < 420 ? 16 : 32)));
        int left = Math.max(4, (width - panelWidth) / 2);
        int right = Math.min(width - 4, left + panelWidth);
        int top = board.header().bottom() + 4;
        int bottom = board.footer().top() - 4;
        int actionsTop = bottom - 27;
        int rowsTop = top + 40;
        int availableRows = Math.max(96, actionsTop - rowsTop - 3);
        int rowHeight = Math.max(24, Math.min(40, availableRows / 4));
        return new EditorLayout(left, top, right, bottom, rowsTop, rowHeight,
                actionsTop);
    }

    record EditorLayout(int left, int top, int right, int bottom,
                        int rowsTop, int rowHeight, int actionsTop) {
    }
}
