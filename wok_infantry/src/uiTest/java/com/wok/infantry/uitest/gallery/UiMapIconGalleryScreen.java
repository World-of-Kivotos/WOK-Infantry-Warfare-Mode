package com.wok.infantry.uitest.gallery;

import com.wok.infantry.client.map.TacticalMapIcons;
import com.wok.infantry.client.screen.PaletteToken;
import com.wok.infantry.client.screen.TacticalBoardChrome;
import com.wok.infantry.client.screen.TacticalBoardTheme;
import com.wok.infantry.client.screen.TacticalDraw;
import com.wok.infantry.client.screen.TacticalPalette;
import com.wok.infantry.client.screen.TacticalScreen;
import com.wok.infantry.client.screen.TacticalShellLayout;
import com.wok.infantry.client.screen.TacticalTabStrip;
import com.wok.infantry.client.screen.TextFit;
import com.wok.infantry.client.screen.UiRect;
import com.wok.infantry.client.ui.probe.UiLayoutProbe;
import com.wok.infantry.client.ui.probe.UiSurfaceInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

/**
 * Map marker sheet (preview surface {@code 36-map-icons}, states {@code dark} and {@code paper}):
 * the ten Squad-style markers with their names on dark terrain or on the pale paper map, then a
 * strip with the interaction states (normal, hover, selected, about to expire) and, where there is
 * room, the icon-size knob at 0.75×, 1.25× and 1.75×. Markers keep their physical size whatever
 * the GUI scale (15×15 art pixels × whole physical pixels per art pixel), which the acceptance
 * checks from the probe's icon records. uiTest only; texts come from {@code wok_uitest} and the
 * core's marker names.
 *
 * <p>D2 device (0.5.0-beta.3): the sheet sits on the tablet's glass; the two backgrounds are the
 * bezel's page keys ({@link #PAGES_UI_ID}) between the hardware Esc key and an R key that is
 * disabled with its reason (nothing on a static sheet to refresh), so the hatched hardware key is
 * captured too. Marker names stay light on their fixed dark plates in every livery: the map's
 * own colours never follow the livery in this round.
 */
public final class UiMapIconGalleryScreen extends TacticalScreen implements UiSurfaceInfo {
    public static final String SURFACE_ID = "mapicons";
    /** Bezel page keys: dark terrain and paper map. */
    public static final String PAGES_UI_ID = "mapicons.pages";
    /** Knob values of the size row (the preview's 0.75×, 1.25×, 1.75×). */
    public static final double[] KNOBS = {0.75D, 1.25D, 1.75D};
    private static final int LABEL_PAD = 3;
    /** Fixed dark plate under a marker name (the map's label plate). */
    private static final int LABEL_PLATE = 0xC0101417;
    private static final String REFRESH_KEY = "screen.wok_infantry.squad_board.hint.refresh";

    private boolean paper;
    private TacticalShellLayout.Metrics metrics = TacticalShellLayout.Metrics.of(
            TacticalShellLayout.Density.COMPACT);
    private UiRect sheet = UiRect.EMPTY;
    private UiRect strip = UiRect.EMPTY;
    private TacticalTabStrip pages;
    private Component statusTitle;

    public UiMapIconGalleryScreen(boolean paper) {
        super(tr("title"));
        this.paper = paper;
        this.statusTitle = title;
    }

    @Override
    public String uiSurfaceId() {
        return SURFACE_ID;
    }

    @Override
    public String uiStateId() {
        return paper ? "paper" : "dark";
    }

    /** Whether the size row is drawn (not on tight screens, as the preview). */
    public boolean showsSizes() {
        return !metrics.tight();
    }

    private static MutableComponent tr(String key, Object... args) {
        return Component.translatable("uitest.wok_infantry.mapicons." + key, args);
    }

    @Override
    protected void initTactical() {
        TacticalShellLayout shell = shellLayout();
        metrics = shell.metrics();
        List<UiRect> rows = shell.content().rows(metrics.gap(), UiRect.Size.STAR,
                UiRect.Size.px(metrics.tight() ? 44 : 70));
        sheet = rows.get(0);
        strip = rows.get(1);

        // Bezel: Esc closes, R is disabled with its reason, the two backgrounds are page keys.
        setBezelKeys(TacticalBoardChrome.KeyHint.close(), TacticalBoardChrome.KeyHint.literal("R",
                Component.translatable(REFRESH_KEY)), () -> { }, () -> tr("refresh_reason"));
        pages = UiLayoutProbe.tag(new TacticalTabStrip(TacticalTabStrip.Skin.BEZEL, List.of(
                TacticalTabStrip.Tab.of("dark", tr("page.dark")),
                TacticalTabStrip.Tab.of("paper", tr("page.paper"))), paper ? 1 : 0,
                index -> {
                    paper = index == 1;
                    rebuildWidgets();
                }), PAGES_UI_ID);
        addRenderableWidget(pages);
        setTabStrip(pages);
        TacticalBoardChrome.placeBezel(font, shell, pages, bezelHints());
        // Short title on narrow bars, as the component gallery and the terminals.
        statusTitle = TacticalBoardChrome.shellTitle(font, shell, title, tr("title_short"), pages,
                UiKitGalleryScreen.SHORT_TITLE_BELOW);
    }

    private TacticalDraw.PanelStyle sheetStyle() {
        TacticalDraw.PanelStyle style = TacticalDraw.PanelStyle.titled(
                tr(paper ? "sheet.paper" : "sheet.dark"));
        return metrics.tight() ? style : style.withMeta(tr("sheet.meta"));
    }

    @Override
    protected void renderTactical(GuiGraphics graphics, int mouseX, int mouseY,
                                  float partialTick) {
        drawShell(graphics, TacticalBoardChrome.ShellSpec.of(statusTitle)
                .withIdentity(tr("identity"))
                .withTabs(pages));
        renderSheet(graphics);
        renderStrip(graphics);
        renderWidgets(graphics, mouseX, mouseY, partialTick);
    }

    private void field(GuiGraphics graphics, UiRect area) {
        if (paper) {
            graphics.fill(area.left(), area.top(), area.right(), area.bottom(),
                    TacticalBoardTheme.CARD);
            // Faint contour lines, as on the paper map.
            for (int y = area.top() + 7; y < area.bottom(); y += 11) {
                graphics.fill(area.left(), y, area.right(), y + 1, 0x226D6A62);
            }
        } else {
            TacticalDraw.well(graphics, area);
            for (int y = area.top() + 9; y < area.bottom(); y += 13) {
                graphics.fill(area.left() + 1, y, area.right() - 1, y + 1,
                        TacticalBoardTheme.MAP_WASH);
            }
        }
    }

    /**
     * Ten markers with their names: as few rows as the widest name allows, the markers spread
     * evenly over them.
     */
    private void renderSheet(GuiGraphics graphics) {
        UiLayoutProbe.begin(graphics, "mapicons.sheet", sheet.left(), sheet.top(), sheet.right(),
                sheet.bottom(), true);
        UiRect c = TacticalDraw.panel(graphics, font, sheet, metrics, sheetStyle());
        field(graphics, c);
        TacticalMapIcons.MapIcon[] icons = TacticalMapIcons.MapIcon.values();
        int widest = 0;
        for (TacticalMapIcons.MapIcon icon : icons) {
            widest = Math.max(widest, font.width(icon.label()));
        }
        int columns = Math.max(1, Math.min(metrics.tight() ? 5 : 10,
                c.width() / (widest + 2 * LABEL_PAD + 4)));
        int rows = (icons.length + columns - 1) / columns;
        // Spread the markers evenly over those rows (5 + 5 rather than 9 + 1).
        columns = (icons.length + rows - 1) / rows;
        int cellWidth = c.width() / columns;
        int cellHeight = c.height() / rows;
        int artPx = TacticalMapIcons.mapArtPx(1.0D);
        double physicalPerLogical = effectiveGuiScale();
        int plate = (int) Math.ceil(15 * artPx / physicalPerLogical);
        int pin = (int) Math.ceil(18 * artPx / physicalPerLogical);
        for (int index = 0; index < icons.length; index++) {
            TacticalMapIcons.MapIcon icon = icons[index];
            int cellLeft = c.left() + (index % columns) * cellWidth;
            int cellTop = c.top() + (index / columns) * cellHeight;
            int iconTop = cellTop + Math.max(2, (cellHeight - pin - 3 - 10) / 2);
            float x = cellLeft + cellWidth / 2.0F;
            float y = icon.plate() == TacticalMapIcons.Plate.PIN ? iconTop + pin
                    : iconTop + plate / 2.0F;
            TacticalMapIcons.draw(graphics, icon, x, y, artPx, TacticalMapIcons.IconState.NORMAL);
            label(graphics, icon.label(), x, iconTop + pin + 3, cellWidth - 4);
        }
        UiLayoutProbe.end(graphics);
    }

    /** States on the tank marker, then (wide screens) the size knob on the defend marker. */
    private void renderStrip(GuiGraphics graphics) {
        UiLayoutProbe.begin(graphics, "mapicons.states", strip.left(), strip.top(), strip.right(),
                strip.bottom(), true);
        UiRect c = TacticalDraw.panel(graphics, font, strip, metrics,
                TacticalDraw.PanelStyle.titled(tr("states.title")));
        field(graphics, c);
        Component[] stateLabels = {tr("state.normal"), tr("state.hover"), tr("state.selected"),
                tr("state.expiring")};
        TacticalMapIcons.IconState[] states = {TacticalMapIcons.IconState.NORMAL,
                TacticalMapIcons.IconState.HOVER, TacticalMapIcons.IconState.SELECTED,
                TacticalMapIcons.IconState.NORMAL};
        int items = states.length + (showsSizes() ? KNOBS.length : 0);
        int width = c.width() / items;
        int labelTop = c.bottom() - 9;
        for (int index = 0; index < states.length; index++) {
            float x = c.left() + index * width + width / 2.0F;
            int artPx = TacticalMapIcons.mapArtPx(1.0D);
            float y = (c.top() + labelTop - 2) / 2.0F;
            TacticalMapIcons.draw(graphics, TacticalMapIcons.MapIcon.TANK, x, y, artPx,
                    states[index], index == 3 ? TacticalMapIcons.EXPIRING_ALPHA : 1.0F);
            label(graphics, stateLabels[index], x, labelTop, width - 4);
        }
        if (showsSizes()) {
            for (int knob = 0; knob < KNOBS.length; knob++) {
                int index = states.length + knob;
                float x = c.left() + index * width + width / 2.0F;
                int artPx = TacticalMapIcons.mapArtPx(KNOBS[knob]);
                float y = (c.top() + labelTop - 2) / 2.0F;
                TacticalMapIcons.draw(graphics, TacticalMapIcons.MapIcon.DEFEND, x, y, artPx,
                        TacticalMapIcons.IconState.NORMAL);
                label(graphics, tr("size", KNOBS[knob]), x, labelTop, width - 4);
            }
        }
        UiLayoutProbe.end(graphics);
    }

    /**
     * A name centred under a marker on a dark label plate (never wider than {@code room}). Plate
     * and name are the map's fixed colours: the A light ink, never the livery's (the Neutral
     * "light" is dark ink and would vanish on the plate).
     */
    private void label(GuiGraphics graphics, Component text, float centreX, int top, int room) {
        int textWidth = Math.min(font.width(text), Math.max(0, room - 2 * LABEL_PAD));
        int plateWidth = textWidth + 2 * LABEL_PAD;
        int left = Math.round(centreX - plateWidth / 2.0F);
        graphics.fill(left, top - 1, left + plateWidth, top + 9, LABEL_PLATE);
        TextFit.draw(graphics, font, text, left + LABEL_PAD, top, textWidth,
                TacticalPalette.A.get(PaletteToken.LIGHT), TextFit.Align.LEFT);
    }
}
