package com.wok.infantry.uitest.gallery;

import com.wok.infantry.client.ClientBootstrap;
import com.wok.infantry.client.KeyBindingDefaults;
import com.wok.infantry.client.hud.TacticalHud;
import com.wok.infantry.client.map.TacticalMapIcons;
import com.wok.infantry.client.screen.TacticalBoardChrome;
import com.wok.infantry.client.screen.TacticalBoardSlider;
import com.wok.infantry.client.screen.TacticalBoardTheme;
import com.wok.infantry.client.screen.TacticalButtonStyle;
import com.wok.infantry.client.screen.TacticalConfirmDialog;
import com.wok.infantry.client.screen.TacticalDraw;
import com.wok.infantry.client.screen.TacticalIcon;
import com.wok.infantry.client.screen.TacticalList;
import com.wok.infantry.client.screen.TacticalScreen;
import com.wok.infantry.client.screen.TacticalShellLayout;
import com.wok.infantry.client.screen.TacticalSliderScale;
import com.wok.infantry.client.screen.TacticalStepper;
import com.wok.infantry.client.screen.TacticalTabStrip;
import com.wok.infantry.client.screen.TacticalTextField;
import com.wok.infantry.client.screen.TextFit;
import com.wok.infantry.client.screen.UiRect;
import com.wok.infantry.client.screen.UiTestWidgets;
import com.wok.infantry.client.ui.probe.UiLayoutProbe;
import com.wok.infantry.client.ui.probe.UiSurfaceInfo;
import com.wok.infantry.formation.FormationCategory;
import com.wok.infantry.uitest.fixtures.MockData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Component gallery of the WOK步战 tablet style (preview surface {@code 00-kit}, merged with the
 * shared-component sheet): every shared component in its states on one terminal, captured by the
 * UI acceptance on all tiers. The first page is the preview's {@code default} state; the danger
 * key opens the {@code confirm} state; the other pages show what does not fit beside it.
 *
 * <ul>
 *   <li>{@link Page#CONTROLS} — key states (normal, hover, selected, disabled, danger, success,
 *       adjustable, keyboard focus), an ellipsized key, an icon-only key, board tabs (full and
 *       pager), a well list, a slider, a stepper, a text field, chips, a meter, an empty state;</li>
 *   <li>{@link Page#INPUTS} — a grouped list that overflows ("还有 n 项"), slider variants, a
 *       stepper at its minimum, text fields (placeholder, typing, error);</li>
 *   <li>{@link Page#CARDS} — cards, item slots, a pager, key-value lines, LEDs, a paragraph and
 *       an empty well;</li>
 *   <li>{@link Page#HUD} — HUD plates, readouts, meters, status dots, labelled bars, key caps;</li>
 *   <li>{@link Page#ICONS} — the 45 interface icons and the ten map markers in their states on
 *       dark terrain and on the paper map.</li>
 * </ul>
 *
 * <p>Statically drawn samples (hover, focus, cards) report themselves to the layout probe; every
 * other part is a real widget, so the probe hooks of the shared components are what is tested.
 * Interface texts come from the uiTest-only {@code wok_uitest} language files.
 */
public final class UiKitGalleryScreen extends TacticalScreen implements UiSurfaceInfo {
    /** Gallery pages, in tab order. */
    public enum Page {
        CONTROLS("default", "controls"),
        INPUTS("inputs", "inputs"),
        CARDS("cards", "cards"),
        HUD("hud", "hud"),
        ICONS("icons", "icons");

        private final String stateId;
        private final String tabId;

        Page(String stateId, String tabId) {
            this.stateId = stateId;
            this.tabId = tabId;
        }

        public String stateId() {
            return stateId;
        }

        public String tabId() {
            return tabId;
        }
    }

    public static final String SURFACE_ID = "kit";
    public static final String PAGES_UI_ID = "kit.pages";
    /** Ellipsized demonstration key; its full label is offered as a tooltip. */
    public static final String LONG_KEY_UI_ID = "kit.button.long";
    public static final String DANGER_KEY_UI_ID = "kit.button.danger";
    private static final int LONG_KEY_WIDTH = 140;
    private static final int PAGER_TABS_WIDTH = 120;

    /** A key drawn in a forced state (hover and focus cannot be held by a real widget). */
    private record ForcedKey(String uiId, UiRect rect, Component label,
                             TacticalButtonStyle.Look look, boolean focus) {
    }

    /** Row of the squad list (preview {@code 00-kit} list well). */
    private record SquadRow(String id, Component right, int rightColor, TacticalIcon icon,
                            int lead, Component disabledReason) {
    }

    /** Row of the formation candidate list. */
    private record Candidate(String name, FormationCategory category, int votes,
                             Component disabledReason, TacticalIcon icon, Component sub,
                             ItemStack item) {
    }

    private Page page;
    private TacticalShellLayout.Metrics metrics = TacticalShellLayout.Metrics.of(
            TacticalShellLayout.Density.COMPACT);
    private TacticalTabStrip pages;
    private final List<ForcedKey> forcedKeys = new ArrayList<>();
    private UiRect regionA = UiRect.EMPTY;
    private UiRect regionB = UiRect.EMPTY;
    private UiRect regionC = UiRect.EMPTY;
    private UiRect chipsRow = UiRect.EMPTY;
    private UiRect meterRow = UiRect.EMPTY;
    private UiRect emptyWell = UiRect.EMPTY;
    private TacticalConfirmDialog dialog;
    private boolean confirmed;

    public UiKitGalleryScreen(Page page) {
        super(tr("title"));
        this.page = page == null ? Page.CONTROLS : page;
    }

    // ---- surface info / test access -------------------------------------------------------------

    @Override
    public String uiSurfaceId() {
        return SURFACE_ID;
    }

    @Override
    public String uiStateId() {
        return hasModal() ? "confirm" : page.stateId();
    }

    public Page page() {
        return page;
    }

    /** The last confirmation opened by the danger key, or {@code null}. */
    public TacticalConfirmDialog dialog() {
        return dialog;
    }

    /** Whether the danger confirmation ran its confirm callback. */
    public boolean confirmed() {
        return confirmed;
    }

    private static MutableComponent tr(String key, Object... args) {
        return Component.translatable("uitest.wok_infantry.kit." + key, args);
    }

    // ---- layout ---------------------------------------------------------------------------------

    @Override
    protected void initTactical() {
        TacticalShellLayout shell = shellLayout();
        metrics = shell.metrics();
        forcedKeys.clear();
        regionA = UiRect.EMPTY;
        regionB = UiRect.EMPTY;
        regionC = UiRect.EMPTY;
        chipsRow = UiRect.EMPTY;
        meterRow = UiRect.EMPTY;
        emptyWell = UiRect.EMPTY;

        List<TacticalTabStrip.Tab> tabs = new ArrayList<>();
        for (Page each : Page.values()) {
            tabs.add(TacticalTabStrip.Tab.of(each.tabId(), tr("page." + each.tabId())));
        }
        pages = UiLayoutProbe.tag(new TacticalTabStrip(TacticalTabStrip.Skin.HEADER, tabs,
                page.ordinal(), index -> switchPage(Page.values()[index])), PAGES_UI_ID);
        addRenderableWidget(pages);
        setTabStrip(pages);
        TacticalBoardChrome.placeTabs(font, shell, title, pages);

        UiRect content = shell.content();
        switch (page) {
            case CONTROLS -> initControls(content);
            case INPUTS -> initInputs(content);
            case CARDS -> initCards(content);
            case HUD -> regionA = content;
            case ICONS -> initIcons(content);
        }
    }

    private void switchPage(Page next) {
        page = next;
        rebuildWidgets();
    }

    private TacticalBoardChrome.ShellSpec spec() {
        return TacticalBoardChrome.ShellSpec.of(title)
                .withIdentity(tr("identity"))
                .withTabs(pages)
                .withHints(TacticalBoardChrome.KeyHint.close(),
                        TacticalBoardChrome.KeyHint.switchTab(),
                        ClientBootstrap.keyHint(KeyBindingDefaults.Binding.TERMINAL,
                                tr("hint.terminal")))
                .withFeedback(TacticalBoardChrome.Feedback.success(tr("feedback")));
    }

    private static boolean fits(int y, int height, int bottom) {
        return y + height <= bottom;
    }

    // ---- page: controls (preview state "default") -----------------------------------------------

    private TacticalDraw.PanelStyle buttonsStyle() {
        TacticalDraw.PanelStyle style = TacticalDraw.PanelStyle.titled(tr("buttons.title"));
        return metrics.tight() ? style : style.withMeta(tr("buttons.meta"));
    }

    private TacticalDraw.PanelStyle inputsStyle() {
        return TacticalDraw.PanelStyle.titled(tr("inputs.title"));
    }

    private void initControls(UiRect content) {
        int gap = metrics.gap();
        int keyHeight = metrics.buttonHeight();
        List<UiRect> halves = content.width() >= 520
                ? content.cols(gap, UiRect.Size.STAR, UiRect.Size.STAR)
                : content.rows(gap, UiRect.Size.STAR, UiRect.Size.STAR);
        regionA = halves.get(0);
        regionB = halves.get(1);

        UiRect keys = TacticalDraw.panelContent(regionA, metrics, buttonsStyle());
        int perRow = keys.width() >= 300 ? 4 : keys.width() >= 180 ? 3 : 2;
        int keyWidth = (keys.width() - (perRow - 1) * gap) / perRow;
        for (int index = 0; index < 8; index++) {
            UiRect cell = UiRect.ofSize(keys.left() + (index % perRow) * (keyWidth + gap),
                    keys.top() + (index / perRow) * (keyHeight + gap), keyWidth, keyHeight);
            switch (index) {
                case 0 -> key("kit.button.normal", tr("state.normal"), cell,
                        UiTestWidgets.Kind.NORMAL, false);
                case 1 -> forcedKeys.add(new ForcedKey("kit.button.hover", cell,
                        tr("state.hover"), new TacticalButtonStyle.Look(
                        TacticalButtonStyle.State.HOVER, true), false));
                case 2 -> key("kit.button.selected", tr("state.selected"), cell,
                        UiTestWidgets.Kind.NORMAL, true);
                case 3 -> {
                    Button disabled = key("kit.button.disabled", tr("state.disabled"), cell,
                            UiTestWidgets.Kind.NORMAL, false);
                    disabled.active = false;
                    disabled.setTooltip(Tooltip.create(tr("state.disabled_reason")));
                }
                case 4 -> addRenderableWidget(UiLayoutProbe.tag(UiTestWidgets.key(
                        tr("state.danger"), ignored -> openConfirm(), cell,
                        UiTestWidgets.Kind.DANGER, false, null, false), DANGER_KEY_UI_ID));
                case 5 -> key("kit.button.success", tr("state.success"), cell,
                        UiTestWidgets.Kind.SUCCESS, false);
                case 6 -> key("kit.button.control", tr("state.control"), cell,
                        UiTestWidgets.Kind.CONTROL, false);
                default -> forcedKeys.add(new ForcedKey("kit.button.focus", cell.inset(2),
                        tr("state.focus"), new TacticalButtonStyle.Look(
                        TacticalButtonStyle.State.NORMAL, false), true));
            }
        }
        int y = keys.top() + (8 + perRow - 1) / perRow * (keyHeight + gap);
        if (fits(y, keyHeight, keys.bottom())) {
            int longWidth = Math.min(keys.width() - keyHeight - gap, LONG_KEY_WIDTH);
            addRenderableWidget(UiLayoutProbe.tag(UiTestWidgets.key(tr("long_label"),
                    ignored -> { }, UiRect.ofSize(keys.left(), y, longWidth, keyHeight),
                    UiTestWidgets.Kind.NORMAL, false, TacticalIcon.GEAR, false), LONG_KEY_UI_ID));
            addRenderableWidget(UiLayoutProbe.tag(UiTestWidgets.key(tr("settings"),
                    ignored -> { }, UiRect.ofSize(keys.left() + longWidth + gap, y, keyHeight,
                            keyHeight), UiTestWidgets.Kind.CONTROL, false, TacticalIcon.GEAR,
                    true), "kit.button.icon"));
            y += keyHeight + gap;
        }
        if (fits(y, keyHeight, keys.bottom())) {
            addRenderableWidget(UiLayoutProbe.tag(new TacticalTabStrip(keys.left(), y,
                    keys.width(), keyHeight, TacticalTabStrip.Skin.BOARD, List.of(
                    TacticalTabStrip.Tab.of("primary", tr("tab.primary")),
                    TacticalTabStrip.Tab.of("secondary", tr("tab.secondary")),
                    TacticalTabStrip.Tab.of("melee", tr("tab.melee")),
                    TacticalTabStrip.Tab.of("gadget", tr("tab.gadget")),
                    TacticalTabStrip.Tab.of("throwable", tr("tab.throwable"))
                            .withDisabledReason(tr("tab.throwable_reason"))), 1,
                    index -> { }), "kit.tabs"));
            y += keyHeight + gap;
        }
        if (fits(y, keyHeight, keys.bottom())) {
            addRenderableWidget(UiLayoutProbe.tag(new TacticalTabStrip(keys.left(), y,
                    Math.min(keys.width(), PAGER_TABS_WIDTH), keyHeight,
                    TacticalTabStrip.Skin.BOARD, List.of(
                    TacticalTabStrip.Tab.of("primary", tr("tab.primary")),
                    TacticalTabStrip.Tab.of("secondary", tr("tab.secondary")),
                    TacticalTabStrip.Tab.of("melee", tr("tab.melee")),
                    TacticalTabStrip.Tab.of("gadget", tr("tab.gadget"))), 2,
                    index -> { }), "kit.tabs_pager"));
        }

        UiRect inputs = TacticalDraw.panelContent(regionB, metrics, inputsStyle());
        int listHeight = metrics.rowHeight() * 4 + 2;
        TacticalList<SquadRow> squads = new TacticalList<SquadRow>(inputs.left(), inputs.top(),
                inputs.width(), listHeight, tr("list.squads"), UiKitGalleryScreen::squadSpec)
                .metrics(metrics)
                .keyedBy(SquadRow::id);
        squads.setItems(List.of(
                new SquadRow("alpha", Component.translatable("hud.wok_infantry.squad_count", 6, 8),
                        0, TacticalIcon.SQUAD, TacticalBoardTheme.SUCCESS_B, null),
                new SquadRow("bravo", Component.translatable("hud.wok_infantry.squad_count", 3, 8),
                        0, TacticalIcon.SQUAD, 0, null),
                new SquadRow("charlie", tr("row.locked"), TacticalBoardTheme.ACCENT_B,
                        TacticalIcon.LOCK, 0, null),
                new SquadRow("delta", tr("row.empty"), 0, TacticalIcon.SQUAD, 0,
                        tr("row.empty_reason"))));
        squads.setSelectedIndex(1);
        addRenderableWidget(UiLayoutProbe.tag(squads, "kit.list"));
        int yy = inputs.top() + listHeight + gap;
        if (fits(yy, keyHeight, inputs.bottom())) {
            addRenderableWidget(UiLayoutProbe.tag(TacticalBoardSlider.builder(
                            Component.translatable("screen.wok_infantry.map.icon_scale"), value -> { })
                    .bounds(inputs.left(), yy, inputs.width(), keyHeight)
                    .range(0.75D, 1.75D, 0.05D).value(1.25D).ticks(0.75D, 1.25D, 1.75D)
                    .build(), "kit.slider"));
            yy += keyHeight + gap;
        }
        if (fits(yy, keyHeight, inputs.bottom())) {
            List<UiRect> pair = UiRect.ofSize(inputs.left(), yy, inputs.width(), keyHeight)
                    .cols(gap, UiRect.Size.STAR, UiRect.Size.STAR);
            UiRect stepper = pair.get(0);
            addRenderableWidget(UiLayoutProbe.tag(new TacticalStepper(stepper.left(),
                    stepper.top(), stepper.width(), stepper.height(),
                    tr("slider.rounds"), TacticalSliderScale.linear(10.0D, 200.0D, 10.0D), 40.0D,
                    TacticalBoardSlider.translated("screen.wok_infantry.ammo_supply.rounds"),
                    value -> { }), "kit.stepper"));
            UiRect field = pair.get(1);
            TacticalTextField text = new TacticalTextField(font, field.left(), field.top(),
                    field.width(), field.height(), tr("text.name"));
            text.setMaxLength(64);
            text.setValue("before-import-20260906.json");
            addRenderableWidget(UiLayoutProbe.tag(text, "kit.text"));
            setFocused(text);
            yy += keyHeight + gap;
        }
        if (fits(yy, TacticalDraw.CHIP_HEIGHT, inputs.bottom())) {
            chipsRow = new UiRect(inputs.left(), yy, inputs.right(), yy + TacticalDraw.CHIP_HEIGHT);
            yy += TacticalDraw.CHIP_HEIGHT + gap;
        }
        if (fits(yy, 4, inputs.bottom())) {
            meterRow = new UiRect(inputs.left(), yy, inputs.right(), yy + 4);
            yy += 4 + gap;
        }
        if (fits(yy, 30, inputs.bottom())) {
            emptyWell = new UiRect(inputs.left(), yy, inputs.right(), inputs.bottom());
        }
    }

    private Button key(String uiId, Component label, UiRect bounds, UiTestWidgets.Kind kind,
                       boolean selected) {
        return addRenderableWidget(UiLayoutProbe.tag(UiTestWidgets.key(label, ignored -> { },
                bounds, kind, selected, null, false), uiId));
    }

    private static TacticalDraw.RowSpec squadSpec(SquadRow row) {
        TacticalDraw.RowSpec spec = TacticalDraw.RowSpec.of(
                        Component.translatable("squad.wok_infantry." + row.id()))
                .withRight(row.right(), row.rightColor())
                .withIcon(row.icon());
        if (row.lead() != 0) {
            spec = spec.withLead(row.lead());
        }
        return row.disabledReason() == null ? spec : spec.withDisabledReason(row.disabledReason());
    }

    private void openConfirm() {
        confirmed = false;
        dialog = TacticalConfirmDialog.builder(tr("confirm.title"), tr("confirm.body"))
                .danger(true)
                .confirmLabel(tr("confirm.ok"))
                .onConfirm(() -> confirmed = true)
                .build();
        openModal(dialog);
    }

    private void renderControls(GuiGraphics graphics) {
        UiLayoutProbe.begin(graphics, "kit.buttons", regionA.left(), regionA.top(),
                regionA.right(), regionA.bottom(), true);
        TacticalDraw.panel(graphics, font, regionA, metrics, buttonsStyle());
        for (ForcedKey key : forcedKeys) {
            UiRect rect = key.rect();
            TextFit.Fitted fitted = TacticalButtonStyle.render(graphics, font, rect.left(),
                    rect.top(), rect.right(), rect.bottom(), key.label(), key.look(),
                    TacticalButtonStyle.Options.DEFAULT.withFocusRing(key.focus()));
            UiLayoutProbe.control(graphics, key.uiId(), "button", key.look().state().name(),
                    rect.left(), rect.top(), rect.right(), rect.bottom(), true, key.label(), null,
                    null, fitted.truncated(), key.focus());
        }
        UiLayoutProbe.end(graphics);

        UiLayoutProbe.begin(graphics, "kit.inputs", regionB.left(), regionB.top(),
                regionB.right(), regionB.bottom(), true);
        TacticalDraw.panel(graphics, font, regionB, metrics, inputsStyle());
        renderChips(graphics, chipsRow);
        if (!meterRow.isEmpty()) {
            TacticalDraw.meter(graphics, meterRow, 0.62F, TacticalBoardTheme.SELECT,
                    TacticalBoardTheme.WELL, 10);
        }
        if (!emptyWell.isEmpty()) {
            TacticalDraw.well(graphics, emptyWell);
            TacticalDraw.empty(graphics, font, TacticalDraw.wellInner(emptyWell),
                    TacticalIcon.SATELLITE, tr("empty.title"), tr("empty.hint"), false);
        }
        UiLayoutProbe.end(graphics);
    }

    /** Chips that fit into {@code row}, left to right (preview {@code UI.chip}). */
    private void renderChips(GuiGraphics graphics, UiRect row) {
        if (row.isEmpty()) {
            return;
        }
        Component[] texts = {
                Component.translatable("screen.wok_infantry.class.quota", 2, 4),
                tr("chip.full"), tr("chip.available"), tr("chip.cooldown", 184)};
        int[] colors = {TacticalBoardTheme.SELECT, TacticalBoardTheme.DANGER,
                TacticalBoardTheme.SUCCESS, TacticalBoardTheme.ACCENT};
        int x = row.left();
        for (int index = 0; index < texts.length; index++) {
            if (x + TacticalDraw.chipWidth(font, texts[index]) > row.right()) {
                break;
            }
            x = TacticalDraw.chip(graphics, font, x, row.top(), texts[index], colors[index], false)
                    + 3;
        }
    }

    // ---- page: inputs ---------------------------------------------------------------------------

    private void initInputs(UiRect content) {
        int gap = metrics.gap();
        int keyHeight = metrics.buttonHeight();
        List<UiRect> columns = content.cols("2*,3*", gap);
        regionA = columns.get(0);
        regionB = columns.get(1);

        List<Candidate> candidates = candidates();
        UiRect listArea = TacticalDraw.panelContent(regionA, metrics, candidatesStyle());
        TacticalList<Candidate> list = new TacticalList<Candidate>(listArea.left(),
                listArea.top(), listArea.width(), listArea.height(), tr("candidates.list"),
                UiKitGalleryScreen::candidateSpec)
                .metrics(metrics)
                .keyedBy(Candidate::name)
                .groupBy(candidate -> Component.literal(candidate.category().displayName()));
        list.setItems(candidates);
        list.setSelectedIndex(4);
        addRenderableWidget(UiLayoutProbe.tag(list, "kit.formations"));

        UiRect side = TacticalDraw.panelContent(regionB, metrics, controlsStyle());
        int y = side.top();
        if (fits(y, keyHeight, side.bottom())) {
            addRenderableWidget(UiLayoutProbe.tag(TacticalBoardSlider.builder(
                            tr("slider.rounds"), value -> { })
                    .bounds(side.left(), y, side.width(), keyHeight)
                    .scale(TacticalSliderScale.roundDetents(30, 300)).value(120)
                    .formatter(TacticalBoardSlider.translated(
                            "screen.wok_infantry.ammo_supply.rounds"))
                    .detentTicks(true).build(), "kit.slider.rounds"));
            y += keyHeight + gap;
        }
        if (fits(y, keyHeight, side.bottom())) {
            addRenderableWidget(UiLayoutProbe.tag(TacticalBoardSlider.builder(
                            Component.translatable("screen.wok_infantry.map.icon_scale"), value -> { })
                    .bounds(side.left(), y, side.width(), keyHeight)
                    .range(0.75D, 1.75D, 0.05D).value(1.0D).ticks(1.0D).build(),
                    "kit.slider.scale"));
            y += keyHeight + gap;
        }
        if (fits(y, keyHeight, side.bottom())) {
            TacticalBoardSlider disabled = TacticalBoardSlider.builder(tr("slider.disabled"),
                            value -> { })
                    .bounds(side.left(), y, side.width(), keyHeight).value(0.4D).build();
            disabled.active = false;
            disabled.setTooltip(Tooltip.create(tr("slider.disabled_reason")));
            addRenderableWidget(UiLayoutProbe.tag(disabled, "kit.slider.disabled"));
            y += keyHeight + gap;
        }
        if (fits(y, keyHeight, side.bottom())) {
            List<UiRect> pair = UiRect.ofSize(side.left(), y, side.width(), keyHeight)
                    .cols(gap, UiRect.Size.STAR, UiRect.Size.STAR);
            UiRect stepper = pair.get(0);
            addRenderableWidget(UiLayoutProbe.tag(new TacticalStepper(stepper.left(),
                    stepper.top(), stepper.width(), stepper.height(), tr("stepper.count"),
                    TacticalSliderScale.linear(1.0D, 8.0D, 1.0D), 1.0D,
                    TacticalBoardSlider.INTEGER, value -> { }), "kit.stepper.min"));
            UiRect field = pair.get(1);
            addRenderableWidget(UiLayoutProbe.tag(new TacticalTextField(font, field.left(),
                    field.top(), field.width(), field.height(), tr("text.placeholder"))
                    .placeholder(tr("text.placeholder")), "kit.text.placeholder"));
            y += keyHeight + gap;
        }
        if (fits(y, keyHeight, side.bottom())) {
            List<UiRect> pair = UiRect.ofSize(side.left(), y, side.width(), keyHeight)
                    .cols(gap, UiRect.Size.STAR, UiRect.Size.STAR);
            UiRect typedBounds = pair.get(0);
            TacticalTextField typed = new TacticalTextField(font, typedBounds.left(),
                    typedBounds.top(), typedBounds.width(), typedBounds.height(), tr("text.slot"));
            typed.setValue("assault_rifle_01");
            addRenderableWidget(UiLayoutProbe.tag(typed, "kit.text.typed"));
            setFocused(typed);
            UiRect errorBounds = pair.get(1);
            TacticalTextField error = new TacticalTextField(font, errorBounds.left(),
                    errorBounds.top(), errorBounds.width(), errorBounds.height(), tr("text.slot"));
            error.setValue("bad id!");
            error.setError(tr("text.error"));
            addRenderableWidget(UiLayoutProbe.tag(error, "kit.text.error"));
            y += keyHeight + gap;
        }
        if (fits(y, TacticalDraw.CHIP_HEIGHT, side.bottom())) {
            chipsRow = new UiRect(side.left(), y, side.right(), y + TacticalDraw.CHIP_HEIGHT);
            y += TacticalDraw.CHIP_HEIGHT + gap;
        }
        if (fits(y, 4, side.bottom())) {
            meterRow = new UiRect(side.left(), y, side.right(), y + 4);
        }
    }

    private TacticalDraw.PanelStyle candidatesStyle() {
        return TacticalDraw.PanelStyle.titled(tr("candidates.title"))
                .withMeta(tr("candidates.meta", candidates().size()));
    }

    private TacticalDraw.PanelStyle controlsStyle() {
        return TacticalDraw.PanelStyle.titled(tr("inputs.controls"));
    }

    /** Sixteen formation candidates in four categories (names are server data). */
    private static List<Candidate> candidates() {
        return List.of(
                new Candidate("常规编制", FormationCategory.INFANTRY, 3, null, null, null, null),
                new Candidate("学院轻步兵营", FormationCategory.INFANTRY, 0, null, null, null,
                        null),
                new Candidate("凯撒步兵营", FormationCategory.INFANTRY, 0, null, null, null, null),
                new Candidate("学院预备役", FormationCategory.INFANTRY, 0,
                        tr("candidate.full_reason"), null, null, null),
                new Candidate("千禧年研讨会机动部队", FormationCategory.MECHANIZED,
                        MockData.VOTES.get("millennium_seminar_mobile"), null, null, null, null),
                new Candidate("234机械化作战单元", FormationCategory.MECHANIZED,
                        MockData.VOTES.get("caesar_234_mechanized"), null, null, null, null),
                new Candidate("学院摩步营", FormationCategory.MECHANIZED, 0, null, null, null, null),
                new Candidate("凯撒摩步营", FormationCategory.MECHANIZED, 0, null, null, null, null),
                new Candidate("研讨会骑兵军团", FormationCategory.ARMORED,
                        MockData.VOTES.get("millennium_seminar_cavalry_corps"), null, null, null,
                        null),
                new Candidate("学院装甲营", FormationCategory.ARMORED, 0, null, null, null, null),
                new Candidate("凯撒装甲营", FormationCategory.ARMORED, 0, null, null, null, null),
                new Candidate("凯撒重装连", FormationCategory.ARMORED, 0, null, null, null, null),
                new Candidate("研讨会侦察分队", FormationCategory.SPECIAL, 0, null, TacticalIcon.EYE,
                        tr("candidate.sub"), null),
                new Candidate("学院工兵连", FormationCategory.SPECIAL, 0, null, null, null,
                        new ItemStack(Items.IRON_PICKAXE)),
                new Candidate("学院特种编制", FormationCategory.SPECIAL, 0, null, null, null, null),
                new Candidate("凯撒特种编制", FormationCategory.SPECIAL, 0, null, null, null,
                        null));
    }

    private static TacticalDraw.RowSpec candidateSpec(Candidate candidate) {
        TacticalDraw.RowSpec spec = TacticalDraw.RowSpec.of(candidate.name())
                .withRight(tr("votes", candidate.votes()));
        if (candidate.icon() != null) {
            spec = spec.withIcon(candidate.icon());
        }
        if (candidate.sub() != null) {
            spec = spec.withSub(candidate.sub());
        }
        if (candidate.item() != null) {
            spec = spec.withItem(candidate.item());
        }
        if (candidate.votes() >= 5) {
            spec = spec.withLead(TacticalBoardTheme.SUCCESS_B);
        }
        return candidate.disabledReason() == null ? spec
                : spec.withDisabledReason(candidate.disabledReason());
    }

    private void renderInputs(GuiGraphics graphics) {
        UiLayoutProbe.begin(graphics, "kit.candidates", regionA.left(), regionA.top(),
                regionA.right(), regionA.bottom(), true);
        TacticalDraw.panel(graphics, font, regionA, metrics, candidatesStyle());
        UiLayoutProbe.end(graphics);
        UiLayoutProbe.begin(graphics, "kit.controls", regionB.left(), regionB.top(),
                regionB.right(), regionB.bottom(), true);
        TacticalDraw.panel(graphics, font, regionB, metrics, controlsStyle());
        renderChips(graphics, chipsRow);
        if (!meterRow.isEmpty()) {
            TacticalDraw.meter(graphics, meterRow, 0.3F, TacticalBoardTheme.ACCENT,
                    TacticalBoardTheme.WELL, 4);
        }
        UiLayoutProbe.end(graphics);
    }

    // ---- page: cards ----------------------------------------------------------------------------

    private void initCards(UiRect content) {
        int gap = metrics.gap();
        List<UiRect> halves = content.width() >= 400
                ? content.cols(gap, UiRect.Size.STAR, UiRect.Size.STAR)
                : content.rows(gap, UiRect.Size.STAR, UiRect.Size.STAR);
        regionA = halves.get(0);
        regionB = halves.get(1);
    }

    private TacticalDraw.PanelStyle cardsStyle() {
        return TacticalDraw.PanelStyle.titled(tr("cards.title"));
    }

    private TacticalDraw.PanelStyle statusStyle() {
        return TacticalDraw.PanelStyle.titled(tr("status.title"));
    }

    private void renderCards(GuiGraphics graphics) {
        int gap = metrics.gap();
        UiLayoutProbe.begin(graphics, "kit.cards", regionA.left(), regionA.top(),
                regionA.right(), regionA.bottom(), true);
        UiRect c = TacticalDraw.panel(graphics, font, regionA, metrics, cardsStyle());
        int cardHeight = Math.min(metrics.buttonHeight() * 2, 28);
        int cardWidth = (c.width() - 3 * gap) / 4;
        TacticalButtonStyle.CardState[] states = TacticalButtonStyle.CardState.values();
        Component[] labels = {tr("state.normal"), tr("state.hover"), tr("state.selected_short"),
                tr("state.disabled")};
        int y = c.top();
        if (fits(y, cardHeight, c.bottom())) {
            for (int index = 0; index < 4; index++) {
                UiRect card = UiRect.ofSize(c.left() + index * (cardWidth + gap), y, cardWidth,
                        cardHeight);
                TacticalDraw.card(graphics, card, states[index]);
                int color = states[index] == TacticalButtonStyle.CardState.SELECTED
                        ? TacticalBoardTheme.ON_SELECT
                        : states[index] == TacticalButtonStyle.CardState.DISABLED
                        ? TacticalBoardTheme.DISABLED_TEXT : TacticalBoardTheme.TEXT;
                TextFit.draw(graphics, font, labels[index], card.left() + 4,
                        card.top() + (card.height() - 8) / 2, card.width() - 8, color,
                        TextFit.Align.CENTER);
            }
            y += cardHeight + gap;
        }
        if (fits(y, TacticalDraw.SLOT_SIZE, c.bottom())) {
            TacticalDraw.slot(graphics, font, c.left(), y, new ItemStack(Items.IRON_SWORD), false,
                    false, 1);
            TacticalDraw.slot(graphics, font, c.left() + 20, y, new ItemStack(Items.ARROW), true,
                    false, 64);
            TacticalDraw.slot(graphics, font, c.left() + 40, y, ItemStack.EMPTY, false, true, 0);
            int pagerWidth = Math.min(60, c.width() - 62);
            if (pagerWidth >= 40) {
                TacticalDraw.pager(graphics, font, UiRect.ofSize(c.right() - pagerWidth, y + 2,
                        pagerWidth, 14), 1, 3, 0);
            }
            y += TacticalDraw.SLOT_SIZE + gap;
        }
        if (fits(y, 8, c.bottom())) {
            TacticalDraw.kv(graphics, font, c.left(), y, c.width(), tr("kv.respawn"),
                    tr("kv.respawn_value"), 0);
            y += TacticalDraw.LINE_HEIGHT;
        }
        if (fits(y, 8, c.bottom())) {
            TacticalDraw.kv(graphics, font, c.left(), y, c.width(), tr("kv.capacity"),
                    Component.literal("40"), TacticalBoardTheme.SUCCESS);
        }
        UiLayoutProbe.end(graphics);

        UiLayoutProbe.begin(graphics, "kit.status", regionB.left(), regionB.top(),
                regionB.right(), regionB.bottom(), true);
        UiRect d = TacticalDraw.panel(graphics, font, regionB, metrics, statusStyle());
        int yy = d.top();
        if (fits(yy, 8, d.bottom())) {
            int x = d.left();
            Component[] leds = {tr("led.ok"), tr("led.wait"), tr("led.lost")};
            int[] colors = {TacticalBoardTheme.SUCCESS_B, TacticalBoardTheme.ACCENT_B,
                    TacticalBoardTheme.DANGER_B};
            for (int index = 0; index < leds.length; index++) {
                int width = TacticalDraw.LED_SIZE + 3 + font.width(leds[index]);
                if (x + width > d.right()) {
                    break;
                }
                TacticalDraw.led(graphics, x, yy + 2, colors[index]);
                TextFit.draw(graphics, font, leds[index], x + TacticalDraw.LED_SIZE + 3, yy,
                        font.width(leds[index]), TacticalBoardTheme.TEXT, TextFit.Align.LEFT);
                x += width + 8;
            }
            yy += TacticalDraw.LINE_HEIGHT + gap;
        }
        int lines = TextFit.wrap(font, tr("paragraph").getString(), d.width(), 0).size();
        int maxLines = metrics.tight() ? 2 : 3;
        if (fits(yy, Math.min(lines, maxLines) * TacticalDraw.LINE_HEIGHT, d.bottom())
                && lines <= maxLines) {
            yy += TacticalDraw.paragraph(graphics, font, tr("paragraph").getString(), d.left(), yy,
                    d.width(), TacticalBoardTheme.TEXT, maxLines) * TacticalDraw.LINE_HEIGHT + gap;
        }
        if (fits(yy, 30, d.bottom())) {
            UiRect well = new UiRect(d.left(), yy, d.right(), d.bottom());
            TacticalDraw.well(graphics, well);
            TacticalDraw.empty(graphics, font, TacticalDraw.wellInner(well), TacticalIcon.PIN,
                    tr("markers.empty"), tr("markers.hint"), false);
        }
        UiLayoutProbe.end(graphics);
    }

    // ---- page: HUD ------------------------------------------------------------------------------

    private void renderHud(GuiGraphics graphics) {
        UiRect world = regionA;
        UiLayoutProbe.begin(graphics, "kit.hud.world", world.left(), world.top(), world.right(),
                world.bottom(), true);
        TacticalDraw.well(graphics, world);
        int inset = 4;
        UiRect roster = renderRoster(graphics, world.left() + inset, world.top() + inset,
                world.bottom() - inset);
        int stripLeft = roster.right() + 6;
        UiRect strip = renderStrip(graphics, stripLeft, world.top() + inset,
                world.right() - inset);
        if (!strip.isEmpty()) {
            renderVoteBar(graphics, stripLeft, strip.bottom() + 4, world.right() - inset);
        }
        renderVitals(graphics, world.left() + inset, world.bottom() - inset, roster.bottom() + 4);
        UiLayoutProbe.end(graphics);
    }

    private UiRect renderRoster(GuiGraphics graphics, int left, int top, int limit) {
        MockData.SquadData squad = MockData.viewerSquad();
        int members = metrics.tight() ? 4 : squad.members().size();
        int nameWidth = 0;
        for (int index = 0; index < members; index++) {
            nameWidth = Math.max(nameWidth, font.width(squad.members().get(index).name()));
        }
        Component header = Component.translatable("squad.wok_infantry." + squad.id());
        String count = squad.members().size() + "/" + squad.capacity();
        int meterWidth = 24;
        int width = Math.max(4 + 6 + nameWidth + 6 + meterWidth + 4,
                4 + font.width(header) + 6 + font.width(count) + 4);
        int height = 4 + 10 + members * 10 + 2;
        if (top + height > limit) {
            members = Math.max(1, (limit - top - 16) / 10);
            height = 4 + 10 + members * 10 + 2;
        }
        UiRect plate = UiRect.ofSize(left, top, width, height);
        UiLayoutProbe.begin(graphics, "kit.hud.roster", plate.left(), plate.top(), plate.right(),
                plate.bottom(), true);
        TacticalHud.plate(graphics, plate.left(), plate.top(), plate.right(), plate.bottom(),
                TacticalHud.Edge.LEFT, TacticalBoardTheme.HUD_FRIENDLY, false);
        TacticalHud.readout(graphics, font, header, plate.left() + 4, plate.top() + 4,
                TacticalBoardTheme.LIGHT);
        TacticalHud.readout(graphics, font, count, plate.right() - 4 - font.width(count),
                plate.top() + 4, TacticalBoardTheme.LIGHT_MUTED);
        for (int index = 0; index < members; index++) {
            MockData.MemberData member = squad.members().get(index);
            int rowTop = plate.top() + 14 + index * 10;
            if (index == 0) {
                graphics.fill(plate.left() + 2, rowTop, plate.right() - 1, rowTop + 10,
                        TacticalHud.SELF_ROW_TINT);
            }
            int dotColor = !member.online() ? TacticalBoardTheme.FAINT
                    : member.alive() ? TacticalBoardTheme.SUCCESS_B : TacticalBoardTheme.DANGER_B;
            TacticalHud.statusDot(graphics, plate.left() + 5, rowTop + 3, dotColor,
                    !member.online());
            TacticalHud.readout(graphics, font, member.name(), plate.left() + 10, rowTop + 1,
                    member.alive() ? TacticalBoardTheme.LIGHT : TacticalBoardTheme.LIGHT_MUTED);
            TacticalHud.meter(graphics, plate.right() - 4 - meterWidth, rowTop + 4,
                    plate.right() - 4, rowTop + 6, member.healthRatio(),
                    TacticalHud.healthColor(member.healthRatio()));
        }
        UiLayoutProbe.end(graphics);
        return plate;
    }

    private UiRect renderStrip(GuiGraphics graphics, int left, int top, int right) {
        String blue = String.valueOf(MockData.TICKETS_BLUE);
        String red = String.valueOf(MockData.TICKETS_RED);
        Component objective = tr("hud.objective", MockData.OBJECTIVE,
                Math.round(MockData.OBJECTIVE_PROGRESS * 100));
        int fixed = 4 + font.width(blue) + 4 + 6 + font.width(objective) + 6 + 4
                + font.width(red) + 4;
        int meterWidth = Math.min(60, (right - left - fixed) / 2);
        if (meterWidth < 16) {
            return UiRect.EMPTY;
        }
        UiRect plate = UiRect.ofSize(left, top, fixed + 2 * meterWidth, 18);
        UiLayoutProbe.begin(graphics, "kit.hud.strip", plate.left(), plate.top(), plate.right(),
                plate.bottom(), true);
        TacticalHud.plate(graphics, plate.left(), plate.top(), plate.right(), plate.bottom(),
                TacticalHud.Edge.TOP, TacticalBoardTheme.HUD_FRIENDLY, true);
        int textY = plate.top() + 5;
        int x = TacticalHud.readout(graphics, font, blue, plate.left() + 4, textY,
                TacticalBoardTheme.HUD_FRIENDLY) + 4;
        TacticalHud.meter(graphics, x, textY + 2, x + meterWidth, textY + 5,
                (float) MockData.TICKETS_BLUE / MockData.TICKETS_MAX, TacticalBoardTheme.HUD_FRIENDLY,
                TacticalBoardTheme.HUD_TRACK, 0, true);
        x += meterWidth + 6;
        x = TacticalHud.readout(graphics, font, objective, x, textY, TacticalBoardTheme.LIGHT) + 6;
        TacticalHud.meter(graphics, x, textY + 2, x + meterWidth, textY + 5,
                (float) MockData.TICKETS_RED / MockData.TICKETS_MAX, TacticalBoardTheme.HUD_HOSTILE,
                TacticalBoardTheme.HUD_TRACK, 0, false);
        x += meterWidth + 4;
        TacticalHud.readout(graphics, font, red, x, textY, TacticalBoardTheme.HUD_HOSTILE);
        UiLayoutProbe.end(graphics);
        return plate;
    }

    private void renderVoteBar(GuiGraphics graphics, int left, int top, int right) {
        Component key = ClientBootstrap.keyLabel(KeyBindingDefaults.Binding.TERMINAL);
        List<TacticalHud.Segment> full = segments(tr("hud.vote", MockData.VOTED,
                MockData.FACTIONS.get(0).population()), key);
        List<TacticalHud.Segment> brief = segments(tr("hud.vote_short"), key);
        List<TacticalHud.Segment> chosen = TacticalHud.segmentsWidth(font, full) + 8 <= right - left
                ? full : TacticalHud.segmentsWidth(font, brief) + 8 <= right - left ? brief : null;
        if (chosen == null) {
            return;
        }
        int width = TacticalHud.segmentsWidth(font, chosen) + 8;
        UiRect plate = UiRect.ofSize(left, top, width, 14);
        UiLayoutProbe.begin(graphics, "kit.hud.vote", plate.left(), plate.top(), plate.right(),
                plate.bottom(), true);
        TacticalHud.plate(graphics, plate.left(), plate.top(), plate.right(), plate.bottom(),
                TacticalHud.Edge.LEFT, TacticalBoardTheme.ACCENT_B, false);
        TacticalHud.drawSegments(graphics, font, chosen, plate.left() + 4, plate.top() + 3,
                plate.right() - 4);
        UiLayoutProbe.end(graphics);
    }

    private List<TacticalHud.Segment> segments(Component text, Component key) {
        List<TacticalHud.Segment> segments = new ArrayList<>();
        segments.add(TacticalHud.Segment.text(text, TacticalBoardTheme.LIGHT));
        if (key != null) {
            segments.add(TacticalHud.Segment.key(key));
            segments.add(TacticalHud.Segment.text(tr("hud.vote_action"),
                    TacticalBoardTheme.LIGHT_MUTED));
        }
        return segments;
    }

    private void renderVitals(GuiGraphics graphics, int left, int bottom, int minTop) {
        Component arms = Component.translatable("hud.wok_infantry.stamina.arms");
        Component legs = Component.translatable("hud.wok_infantry.stamina.legs");
        int labelWidth = Math.max(font.width(arms), font.width(legs));
        int width = 4 + labelWidth + 2 + 50 + 4;
        int height = 4 + 20 + 2;
        if (bottom - height < minTop) {
            return;
        }
        UiRect plate = new UiRect(left, bottom - height, left + width, bottom);
        UiLayoutProbe.begin(graphics, "kit.hud.vitals", plate.left(), plate.top(), plate.right(),
                plate.bottom(), true);
        TacticalHud.plate(graphics, plate.left(), plate.top(), plate.right(), plate.bottom(),
                TacticalHud.Edge.LEFT, TacticalBoardTheme.NEUTRAL_B, false);
        TacticalHud.labelledBar(graphics, font, arms, MockData.STAMINA_ARMS / 100.0F,
                TacticalHud.staminaColor(MockData.STAMINA_ARMS), plate.left() + 4,
                plate.top() + 4, plate.right() - 4);
        TacticalHud.labelledBar(graphics, font, legs, MockData.STAMINA_LEGS / 100.0F,
                TacticalHud.staminaColor(MockData.STAMINA_LEGS), plate.left() + 4,
                plate.top() + 14, plate.right() - 4);
        UiLayoutProbe.end(graphics);
    }

    // ---- page: icons ----------------------------------------------------------------------------

    private void initIcons(UiRect content) {
        int gap = metrics.gap();
        UiRect probe = TacticalDraw.panelContent(content, metrics, iconsStyle());
        int perRow = Math.max(1, probe.width() / TacticalIcon.ADVANCE);
        int rows = (TacticalIcon.values().length + perRow - 1) / perRow;
        int chrome = (probe.top() - content.top()) + (content.bottom() - probe.bottom());
        int height = chrome + rows * TacticalIcon.ADVANCE;
        regionA = new UiRect(content.left(), content.top(), content.right(),
                content.top() + height);
        List<UiRect> maps = new UiRect(content.left(), regionA.bottom() + gap, content.right(),
                content.bottom()).cols(gap, UiRect.Size.STAR, UiRect.Size.STAR);
        regionB = maps.get(0);
        regionC = maps.get(1);
    }

    private TacticalDraw.PanelStyle iconsStyle() {
        return TacticalDraw.PanelStyle.titled(tr("icons.title"))
                .withMeta(tr("icons.meta", TacticalIcon.values().length));
    }

    private void renderIcons(GuiGraphics graphics) {
        UiLayoutProbe.begin(graphics, "kit.icons", regionA.left(), regionA.top(), regionA.right(),
                regionA.bottom(), true);
        UiRect c = TacticalDraw.panel(graphics, font, regionA, metrics, iconsStyle());
        int perRow = Math.max(1, c.width() / TacticalIcon.ADVANCE);
        TacticalIcon[] icons = TacticalIcon.values();
        for (int index = 0; index < icons.length; index++) {
            icons[index].draw(graphics, c.left() + (index % perRow) * TacticalIcon.ADVANCE,
                    c.top() + (index / perRow) * TacticalIcon.ADVANCE, TacticalBoardTheme.TEXT);
        }
        UiLayoutProbe.end(graphics);
        renderMapIcons(graphics, regionB, "kit.mapicons.dark", tr("mapicons.dark"), false);
        renderMapIcons(graphics, regionC, "kit.mapicons.paper", tr("mapicons.paper"), true);
    }

    /** Ten markers in four rows (normal, hover, selected, about to expire). */
    private void renderMapIcons(GuiGraphics graphics, UiRect region, String id, Component title,
                                boolean paper) {
        UiLayoutProbe.begin(graphics, id, region.left(), region.top(), region.right(),
                region.bottom(), true);
        UiRect c = TacticalDraw.panel(graphics, font, region, metrics,
                TacticalDraw.PanelStyle.titled(title));
        if (paper) {
            graphics.fill(c.left(), c.top(), c.right(), c.bottom(), TacticalBoardTheme.CARD);
        } else {
            TacticalDraw.well(graphics, c);
        }
        int artPx = TacticalMapIcons.mapArtPx(1.0D);
        double physicalPerLogical = effectiveGuiScale();
        int plate = (int) Math.ceil(15 * artPx / physicalPerLogical);
        int pin = (int) Math.ceil(18 * artPx / physicalPerLogical);
        int pitchX = plate + 4;
        int pitchY = pin + 4;
        TacticalMapIcons.IconState[] states = {TacticalMapIcons.IconState.NORMAL,
                TacticalMapIcons.IconState.HOVER, TacticalMapIcons.IconState.SELECTED,
                TacticalMapIcons.IconState.NORMAL};
        TacticalMapIcons.MapIcon[] markers = TacticalMapIcons.MapIcon.values();
        for (int row = 0; row < states.length; row++) {
            int top = c.top() + 3 + row * pitchY;
            if (top + pin > c.bottom()) {
                break;
            }
            for (int column = 0; column < markers.length; column++) {
                int left = c.left() + 3 + column * pitchX;
                if (left + plate > c.right()) {
                    break;
                }
                TacticalMapIcons.MapIcon marker = markers[column];
                float x = left + plate / 2.0F;
                float y = marker.plate() == TacticalMapIcons.Plate.PIN ? top + pin
                        : top + plate / 2.0F;
                TacticalMapIcons.draw(graphics, marker, x, y, artPx, states[row],
                        row == 3 ? TacticalMapIcons.EXPIRING_ALPHA : 1.0F);
            }
        }
        UiLayoutProbe.end(graphics);
    }

    // ---- rendering ------------------------------------------------------------------------------

    @Override
    protected void renderTactical(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        drawShell(graphics, spec());
        switch (page) {
            case CONTROLS -> renderControls(graphics);
            case INPUTS -> renderInputs(graphics);
            case CARDS -> renderCards(graphics);
            case HUD -> renderHud(graphics);
            case ICONS -> renderIcons(graphics);
        }
        renderWidgets(graphics, mouseX, mouseY, partialTick);
    }
}
