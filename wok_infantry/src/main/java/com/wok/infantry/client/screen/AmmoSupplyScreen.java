package com.wok.infantry.client.screen;

import com.wok.infantry.ammo.AmmoSupplyView;
import com.wok.infantry.network.battle.BattleNetwork;
import com.wok.infantry.network.battle.packet.c2s.SelectAmmoSupplyGunPacket;
import com.wok.infantry.network.battle.packet.c2s.SupplyVehicleAmmoPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

/** Responsive tactical-tablet selector for infantry and nearby vehicle ammunition. */
public final class AmmoSupplyScreen extends Screen {
    private static final int GUN_ROW_HEIGHT = 34;
    private static final int VEHICLE_ROW_HEIGHT = 48;
    private final AmmoSupplyView view;
    private final List<VisibleGunRow> visibleGunRows = new ArrayList<>();
    private final List<VisibleVehicleRow> visibleVehicleRows = new ArrayList<>();
    private final Map<String, Integer> selectedRounds = new HashMap<>();
    private TacticalMapLayout.Layout layout;
    private int panelLeft;
    private int panelRight;
    private int listTop;
    private int listBottom;
    private int firstVisible;
    private Mode mode;

    public AmmoSupplyScreen(AmmoSupplyView view) {
        this(view, view.target().isLarge()
                && !view.vehicleAmmunition().isEmpty());
    }

    public AmmoSupplyScreen(AmmoSupplyView view, boolean preferVehicleMode) {
        super(Component.translatable("screen.wok_infantry.ammo_supply"));
        this.view = view;
        this.mode = preferVehicleMode
                && view.target().isLarge()
                ? Mode.VEHICLE : Mode.INFANTRY;
    }

    public boolean vehicleModeSelected() {
        return mode == Mode.VEHICLE;
    }

    @Override
    protected void init() {
        layout = TacticalMapLayout.compute(width, height);
        int margin = layout.rich() ? 16 : 8;
        panelLeft = margin;
        panelRight = width - margin;
        boolean hasModeTabs = view.target().isLarge();
        listTop = layout.rich() ? hasModeTabs ? 117 : 84 : hasModeTabs ? 101 : 68;
        listBottom = Math.max(listTop + rowHeight(), layout.footer().top() - 7);
        rebuildRows();
    }

    private void rebuildRows() {
        clearWidgets();
        visibleGunRows.clear();
        visibleVehicleRows.clear();
        addModeTabs();
        int visibleCount = Math.max(1, (listBottom - listTop) / rowHeight());
        firstVisible = Mth.clamp(firstVisible, 0,
                Math.max(0, optionCount() - visibleCount));
        if (mode == Mode.VEHICLE) {
            addVehicleRows(visibleCount);
        } else {
            addGunRows(visibleCount);
        }
    }

    private void addModeTabs() {
        if (!view.target().isLarge()) {
            return;
        }
        int gap = 4;
        int left = panelLeft + 4;
        int tabWidth = Math.max(50, Math.min(112,
                (panelRight - panelLeft - 12) / 2));
        int top = layout.rich() ? 78 : 62;
        addRenderableWidget(BattleUiButton.builder(Component.translatable(
                        "screen.wok_infantry.ammo_supply.mode.infantry"), ignored -> {
                    mode = Mode.INFANTRY;
                    firstVisible = 0;
                    rebuildRows();
                }).selected(mode == Mode.INFANTRY).kind(BattleUiButton.Kind.CONTROL)
                .bounds(left, top, tabWidth, 20).build());
        addRenderableWidget(BattleUiButton.builder(Component.translatable(
                        "screen.wok_infantry.ammo_supply.mode.vehicle"), ignored -> {
                    mode = Mode.VEHICLE;
                    firstVisible = 0;
                    rebuildRows();
                }).selected(mode == Mode.VEHICLE).kind(BattleUiButton.Kind.CONTROL)
                .bounds(left + tabWidth + gap, top, tabWidth, 20).build());
    }

    private void addGunRows(int visibleCount) {
        int buttonWidth = Math.max(48, Math.min(76,
                (panelRight - panelLeft) / 4));
        int rowRight = panelRight - 4;
        for (int index = firstVisible;
             index < view.guns().size() && visibleGunRows.size() < visibleCount; index++) {
            AmmoSupplyView.GunOption option = view.guns().get(index);
            int y = listTop + visibleGunRows.size() * GUN_ROW_HEIGHT;
            Button button = BattleUiButton.builder(Component.translatable(
                            "gui.wok_infantry.ammo_supply.select"),
                            ignored -> BattleNetwork.sendToServer(
                                    new SelectAmmoSupplyGunPacket(view.target(),
                                            option.inventorySlot())))
                    .kind(option.receivableRounds() > 0
                            ? BattleUiButton.Kind.SUCCESS : BattleUiButton.Kind.NORMAL)
                    .bounds(rowRight - buttonWidth, y + 6, buttonWidth, 22).build();
            button.active = option.receivableRounds() > 0 && view.remainingPoints() > 0;
            addRenderableWidget(button);
            visibleGunRows.add(new VisibleGunRow(option, y,
                    rowRight - buttonWidth - 6));
        }
    }

    private void addVehicleRows(int visibleCount) {
        int rowRight = panelRight - 4;
        int buttonWidth = Math.max(48, Math.min(68,
                (panelRight - panelLeft) / 5));
        int sliderWidth = Math.max(70, Math.min(layout.rich() ? 150 : 100,
                (panelRight - panelLeft) / 3));
        for (int index = firstVisible; index < view.vehicleAmmunition().size()
                && visibleVehicleRows.size() < visibleCount; index++) {
            AmmoSupplyView.VehicleAmmoOption option = view.vehicleAmmunition().get(index);
            int y = listTop + visibleVehicleRows.size() * VEHICLE_ROW_HEIGHT;
            int buttonLeft = rowRight - buttonWidth;
            int sliderLeft = buttonLeft - sliderWidth - 4;
            String selectorKey = selectorKey(option);
            int initial = selectedRounds.getOrDefault(selectorKey,
                    option.maxRounds() > 0 ? option.roundStep() : 0);
            RoundAmountSlider slider = new RoundAmountSlider(sliderLeft, y + 16,
                    sliderWidth, 22, option.roundStep(), option.maxRounds(), initial,
                    value -> selectedRounds.put(selectorKey, value));
            slider.active = option.maxRounds() > 0 && view.remainingPoints() > 0;
            addRenderableWidget(slider);
            Button button = BattleUiButton.builder(Component.translatable(
                            "gui.wok_infantry.ammo_supply.select"), ignored -> {
                        int requested = selectedRounds.getOrDefault(selectorKey,
                                option.roundStep());
                        BattleNetwork.sendToServer(new SupplyVehicleAmmoPacket(
                                view.target(), option.vehicleEntityId(),
                                option.weaponKey(), option.consumerIndex(), requested));
                    }).kind(option.maxRounds() > 0
                            ? BattleUiButton.Kind.SUCCESS : BattleUiButton.Kind.NORMAL)
                    .bounds(buttonLeft, y + 16, buttonWidth, 22).build();
            button.active = option.maxRounds() > 0 && view.remainingPoints() > 0;
            addRenderableWidget(button);
            visibleVehicleRows.add(new VisibleVehicleRow(option, y, sliderLeft - 6));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (optionCount() == 0) {
            return false;
        }
        int before = firstVisible;
        firstVisible -= (int) Math.signum(delta);
        int visibleCount = Math.max(1, (listBottom - listTop) / rowHeight());
        firstVisible = Mth.clamp(firstVisible, 0,
                Math.max(0, optionCount() - visibleCount));
        if (before != firstVisible) {
            rebuildRows();
            return true;
        }
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        TacticalBoardChrome.renderShell(graphics, width, height, layout);
        String stationKey = switch (view.target().kind()) {
            case SMALL_CRATE -> "screen.wok_infantry.ammo_supply.small";
            case MEDIUM_CRATE -> "screen.wok_infantry.ammo_supply.medium";
            case LARGE_STATION, LARGE_BLOCK -> "screen.wok_infantry.ammo_supply.large";
        };
        Component station = Component.translatable(stationKey);
        TacticalBoardChrome.renderHeader(graphics, font, layout,
                Component.translatable("screen.wok_infantry.ammo_supply.board_title"),
                station, true);
        TacticalBoardTheme.raisedPanel(graphics, panelLeft, layout.rich() ? 52 : 43,
                panelRight, listBottom + 1, TacticalBoardTheme.BOARD_ALT);
        renderSupplyMeter(graphics);
        TacticalBoardTheme.sectionHeader(graphics, font, Component.translatable(
                        mode == Mode.VEHICLE
                                ? "screen.wok_infantry.ammo_supply.vehicle_ammunition"
                                : "screen.wok_infantry.ammo_supply.weapons"),
                panelLeft + 4, listTop - 16, panelRight - 4, TacticalBoardTheme.ACCENT);
        if (optionCount() == 0) {
            BattleUiTheme.drawCenteredText(graphics, font, Component.translatable(
                            mode == Mode.VEHICLE
                                    ? "screen.wok_infantry.ammo_supply.no_vehicles"
                                    : "screen.wok_infantry.ammo_supply.no_weapons"),
                    width / 2, listTop + 18, TacticalBoardTheme.MUTED_TEXT);
        }
        visibleGunRows.forEach(row -> renderGunRow(graphics, row));
        visibleVehicleRows.forEach(row -> renderVehicleRow(graphics, row));
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(font, Component.translatable(
                        mode == Mode.VEHICLE
                                ? "screen.wok_infantry.ammo_supply.vehicle_footer"
                                : "screen.wok_infantry.ammo_supply.footer"),
                layout.footer().left() + 5, layout.footer().top() + 4,
                TacticalBoardTheme.MUTED_TEXT, false);
    }

    private void renderSupplyMeter(GuiGraphics graphics) {
        int top = layout.rich() ? 56 : 47;
        int left = panelLeft + 8;
        int right = panelRight - 8;
        int meterLeft = left + Math.min(126, Math.max(80, (right - left) / 3));
        Component points = Component.translatable("screen.wok_infantry.ammo_supply.points",
                view.remainingPoints(), view.capacityPoints());
        graphics.drawString(font, points, left, top + 4,
                view.remainingPoints() > 0 ? TacticalBoardTheme.TEXT
                        : TacticalBoardTheme.DANGER, false);
        graphics.fill(meterLeft, top + 4, right, top + 13, TacticalBoardTheme.INSET);
        int meterWidth = Math.max(0, right - meterLeft - 2);
        int filled = view.capacityPoints() == 0 ? 0
                : meterWidth * view.remainingPoints() / view.capacityPoints();
        graphics.fill(meterLeft + 1, top + 5, meterLeft + 1 + filled, top + 12,
                view.remainingPoints() > view.capacityPoints() / 5
                        ? TacticalBoardTheme.SUCCESS : TacticalBoardTheme.DANGER);
        BattleUiTheme.outline(graphics, meterLeft, top + 4, right, top + 13,
                TacticalBoardTheme.BORDER);
    }

    private void renderGunRow(GuiGraphics graphics, VisibleGunRow row) {
        AmmoSupplyView.GunOption option = row.option();
        renderRowBackground(graphics, row.y(), GUN_ROW_HEIGHT,
                option.receivableRounds() > 0);
        int left = panelLeft + 4;
        int textWidth = Math.max(30, row.textRight() - left - 8);
        graphics.drawString(font, font.plainSubstrByWidth(
                        option.gunName().getString(), textWidth), left + 6, row.y() + 5,
                TacticalBoardTheme.TEXT, false);
        Component detail = Component.translatable("screen.wok_infantry.ammo_supply.detail",
                option.ammunitionName(), option.pointsPerRound(), option.currentRounds(),
                option.reserveLimit(), option.receivableRounds());
        graphics.drawString(font, font.plainSubstrByWidth(detail.getString(), textWidth),
                left + 6, row.y() + 18, option.receivableRounds() > 0
                        ? TacticalBoardTheme.MUTED_TEXT : TacticalBoardTheme.DANGER, false);
    }

    private void renderVehicleRow(GuiGraphics graphics, VisibleVehicleRow row) {
        AmmoSupplyView.VehicleAmmoOption option = row.option();
        renderRowBackground(graphics, row.y(), VEHICLE_ROW_HEIGHT,
                option.maxRounds() > 0);
        int left = panelLeft + 4;
        int textWidth = Math.max(30, row.textRight() - left - 8);
        Component title = Component.translatable(
                "screen.wok_infantry.ammo_supply.vehicle_title",
                option.vehicleName(), option.weaponName());
        graphics.drawString(font, font.plainSubstrByWidth(title.getString(), textWidth),
                left + 6, row.y() + 5, TacticalBoardTheme.TEXT, false);
        Component detail = Component.translatable(
                "screen.wok_infantry.ammo_supply.vehicle_detail",
                option.ammunitionName(), option.pointsPerRound(), option.currentRounds(),
                option.roundStep());
        graphics.drawString(font, font.plainSubstrByWidth(detail.getString(), textWidth),
                left + 6, row.y() + 19, option.maxRounds() > 0
                        ? TacticalBoardTheme.MUTED_TEXT : TacticalBoardTheme.DANGER, false);
        if (option.maxRounds() == 0) {
            Component unavailable = Component.translatable(
                    "screen.wok_infantry.ammo_supply.vehicle_unavailable");
            graphics.drawString(font, font.plainSubstrByWidth(
                            unavailable.getString(), textWidth), left + 6, row.y() + 32,
                    TacticalBoardTheme.DANGER, false);
        }
    }

    private void renderRowBackground(GuiGraphics graphics, int y, int height,
                                     boolean available) {
        int left = panelLeft + 4;
        int right = panelRight - 4;
        graphics.fill(left, y, right, y + height - 2, TacticalBoardTheme.CARD);
        BattleUiTheme.outline(graphics, left, y, right, y + height - 2,
                available ? TacticalBoardTheme.BORDER : TacticalBoardTheme.CARD_DISABLED);
    }

    private int optionCount() {
        return mode == Mode.VEHICLE ? view.vehicleAmmunition().size() : view.guns().size();
    }

    private int rowHeight() {
        return mode == Mode.VEHICLE ? VEHICLE_ROW_HEIGHT : GUN_ROW_HEIGHT;
    }

    private static String selectorKey(AmmoSupplyView.VehicleAmmoOption option) {
        return option.vehicleEntityId() + ":" + option.weaponKey() + ":"
                + option.consumerIndex();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum Mode { INFANTRY, VEHICLE }

    private record VisibleGunRow(AmmoSupplyView.GunOption option, int y, int textRight) {
    }

    private record VisibleVehicleRow(AmmoSupplyView.VehicleAmmoOption option, int y,
                                     int textRight) {
    }

    /** Tactical, shadowless slider whose values are snapped to one real ammo package. */
    private static final class RoundAmountSlider extends AbstractSliderButton {
        private final int step;
        private final int maximum;
        private final IntConsumer onChanged;

        private RoundAmountSlider(int x, int y, int width, int height, int step,
                                  int maximum, int initial, IntConsumer onChanged) {
            super(x, y, width, height, Component.empty(), normalized(step, maximum, initial));
            this.step = Math.max(1, step);
            this.maximum = Math.max(0, maximum - maximum % this.step);
            this.onChanged = onChanged;
            updateMessage();
            if (this.maximum > 0) {
                this.onChanged.accept(amount());
            }
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("screen.wok_infantry.ammo_supply.rounds",
                    amount()));
        }

        @Override
        protected void applyValue() {
            if (maximum > 0) {
                value = (double) (amount() - step) / Math.max(1, maximum - step);
                onChanged.accept(amount());
            }
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (active && (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT)) {
                int direction = keyCode == GLFW.GLFW_KEY_LEFT ? -1 : 1;
                int next = Mth.clamp(amount() + direction * step, step, maximum);
                value = (double) (next - step) / Math.max(1, maximum - step);
                updateMessage();
                onChanged.accept(next);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY,
                                 float partialTick) {
            int left = getX();
            int top = getY();
            int right = left + width;
            int bottom = top + height;
            graphics.fill(left, top, right, bottom, active
                    ? TacticalBoardTheme.CARD : TacticalBoardTheme.CARD_DISABLED);
            BattleUiTheme.outline(graphics, left, top, right, bottom,
                    isHoveredOrFocused() && active
                            ? TacticalBoardTheme.ACCENT : TacticalBoardTheme.BORDER);
            int trackLeft = left + 5;
            int trackRight = right - 5;
            int trackY = bottom - 5;
            graphics.fill(trackLeft, trackY, trackRight, trackY + 2,
                    TacticalBoardTheme.INSET);
            int thumbX = trackLeft + (int) Math.round((trackRight - trackLeft - 3) * value);
            graphics.fill(thumbX, trackY - 2, thumbX + 3, trackY + 4,
                    active ? TacticalBoardTheme.ACCENT : TacticalBoardTheme.MUTED_TEXT);
            int color = active ? TacticalBoardTheme.TEXT : TacticalBoardTheme.MUTED_TEXT;
            BattleUiTheme.drawCenteredText(graphics,
                    net.minecraft.client.Minecraft.getInstance().font, getMessage(),
                    (left + right) / 2, top + 4, color);
        }

        private int amount() {
            if (maximum < step) {
                return 0;
            }
            int packageCount = Math.max(1, maximum / step);
            int index = (int) Math.round(value * (packageCount - 1));
            return Math.min(maximum, (index + 1) * step);
        }

        private static double normalized(int step, int maximum, int initial) {
            int safeStep = Math.max(1, step);
            int safeMaximum = Math.max(0, maximum - maximum % safeStep);
            if (safeMaximum <= safeStep) {
                return 0.0D;
            }
            int snapped = Mth.clamp(initial - initial % safeStep, safeStep, safeMaximum);
            return (double) (snapped - safeStep) / (safeMaximum - safeStep);
        }
    }
}
