package com.wok.infantry.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wok.infantry.formation.selection.FormationSelectionView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Detail panel of the vote page (preview {@code identityBlock}/{@code detailContent}): emblem,
 * name, chips and the capacity check on top; the description and four sections (vehicles, class
 * quotas, squads, capabilities) flowed into one to three columns below.
 *
 * <p>The sections scroll with the mouse wheel and are cut on whole lines only: a line is drawn
 * only when its whole glyph box lies in the view, a wrapped entry is shown whole or not at all and
 * a section title never stands without its first row. When something is hidden, the last line
 * says "还有 n 项：兵种名额 / 小队 · 滚轮查看" next to a scrollbar (no half-cut rows).
 */
final class FormationDetailPanel {
    static final int LINE = 10;
    static final int GLYPH = 8;
    static final int HEAD = 13;
    static final int SECTION_GAP = 6;
    static final int SCROLLBAR = 4;
    private static final Map<String, ResourceLocation> EMBLEMS = new HashMap<>();

    private String formationKey = "";
    private int scroll;
    private int maxScroll;
    private UiRect lastContent = UiRect.EMPTY;

    /** Back to the top (another formation or faction was selected). */
    void reset() {
        scroll = 0;
        formationKey = "";
    }

    /** Draws the whole panel; {@code layout} must have a detail panel. */
    void render(GuiGraphics graphics, Font font, FormationScreenLayout layout,
                FormationVoteModel model) {
        FormationSelectionView formation = model.highlighted();
        TacticalShellLayout.Metrics metrics = layout.shell().metrics();
        TacticalDraw.PanelStyle style = TacticalDraw.PanelStyle.titled(FormationText.detailTitle());
        if (formation != null) {
            style = style.withMeta(FormationText.category(formation));
        }
        TacticalDraw.panel(graphics, font, layout.detailPanel(), metrics, style);
        if (formation == null) {
            TacticalDraw.empty(graphics, font, new UiRect(layout.identity().left(),
                            layout.identity().top(), layout.identity().right(),
                            Math.max(layout.identity().top(), layout.detailAction().bottom())),
                    TacticalIcon.INFO,
                    Component.translatable(FormationText.PREFIX + "list_empty"), null, true);
            return;
        }
        String key = model.browsing().id() + "/" + formation.id();
        if (!key.equals(formationKey)) {
            formationKey = key;
            scroll = 0;
        }
        identity(graphics, font, layout.identity(), model, formation, metrics);
        TacticalDraw.divider(graphics, layout.detailAction().left(), layout.detailAction().right(),
                layout.detailSeparatorY());
        content(graphics, font, layout.content(), model, formation, metrics);
    }

    /** Scrolls the sections when the wheel turns over them; returns whether it was consumed. */
    boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!lastContent.contains(mouseX, mouseY) || maxScroll <= 0 || delta == 0.0D) {
            return false;
        }
        scroll = Math.max(0, Math.min(maxScroll, scroll + (delta > 0.0D ? -LINE : LINE)));
        return true;
    }

    // ---- identity ---------------------------------------------------------------------------------

    /**
     * Emblem (or category glyph) + name and chips + "category · faction · votes" + capacity
     * check. Shared by the detail panel and the narrow list page preview.
     */
    static void identity(GuiGraphics graphics, Font font, UiRect bounds, FormationVoteModel model,
                         FormationSelectionView formation, TacticalShellLayout.Metrics metrics) {
        int size = Math.min(FormationScreenLayout.emblemSize(metrics),
                Math.max(0, bounds.height()));
        UiRect box = new UiRect(bounds.left(), bounds.top(), bounds.left() + size,
                bounds.top() + size);
        graphics.fill(box.left(), box.top(), box.right(), box.bottom(), TacticalBoardTheme.BADGE_BG);
        if (!drawEmblem(graphics, formation.iconId(), box.left(), box.top(), size)) {
            graphics.fill(box.left() + 1, box.top() + 1, box.right() - 1, box.bottom() - 1,
                    TacticalBoardTheme.BOARD);
            FormationText.categoryIcon(formation.categoryId()).drawCentered(graphics, box.left(),
                    box.top(), box.right(), box.bottom(), TacticalBoardTheme.MUTED);
        }
        BattleUiTheme.outline(graphics, box.left(), box.top(), box.right(), box.bottom(),
                TacticalBoardTheme.BORDER);
        int x = box.right() + (metrics.tight() ? 5 : 8);
        int width = Math.max(0, bounds.right() - x);
        int lineHeight = metrics.roomy() ? 14 : 11;
        int y0 = bounds.top() + (metrics.roomy() ? 5 : 0);
        int chipX = bounds.right();
        List<FormationText.Chip> chips = new ArrayList<>(FormationText.chips(model, formation));
        java.util.Collections.reverse(chips);
        for (FormationText.Chip chip : chips) {
            int chipWidth = TacticalDraw.chipWidth(font, chip.text());
            if (chipX - chipWidth < x + 60) {
                break;
            }
            chipX -= chipWidth;
            TacticalDraw.chip(graphics, font, chipX, y0 - 2, chip.text(), chip.color(), false);
            chipX -= 3;
        }
        TextFit.draw(graphics, font, formation.displayName(), x, y0,
                Math.max(0, chipX - x - 4), TacticalBoardTheme.TEXT, TextFit.Align.LEFT);
        TextFit.draw(graphics, font, FormationText.identitySub(model, formation), x,
                y0 + lineHeight, width, TacticalBoardTheme.MUTED, TextFit.Align.LEFT);
        FormationText.FitLine fit = FormationText.fit(model, formation);
        int fitY = y0 + lineHeight * 2;
        if (fitY + GLYPH <= bounds.bottom() + 1) {
            fit.icon().draw(graphics, x, fitY - 1, fit.iconColor());
            drawVariant(graphics, font, fit.text(), x + 12, fitY, width - 12, fit.color());
        }
    }

    /**
     * Draws the formation emblem at an integer size: the pre-scaled {@code _64}/{@code _32}
     * texture when the resource pack has it, otherwise the 256px source scaled down by an
     * integer factor. Returns false when there is no emblem.
     */
    static boolean drawEmblem(GuiGraphics graphics, String iconId, int x, int y, int size) {
        if (iconId == null || iconId.isBlank() || size <= 0) {
            return false;
        }
        ResourceLocation source = ResourceLocation.tryParse(iconId);
        if (source == null) {
            return false;
        }
        ResourceLocation scaled = preScaled(iconId, size);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        if (scaled != null) {
            graphics.blit(scaled, x, y, size, size, 0.0F, 0.0F, size, size, size, size);
        } else {
            graphics.blit(source, x, y, size, size, 0.0F, 0.0F, 256, 256, 256, 256);
        }
        RenderSystem.disableBlend();
        return true;
    }

    /** {@code <name>_<size>.png} next to the emblem, or {@code null} when it does not exist. */
    static synchronized ResourceLocation preScaled(String iconId, int size) {
        String key = iconId + "#" + size;
        if (EMBLEMS.containsKey(key)) {
            return EMBLEMS.get(key);
        }
        ResourceLocation found = null;
        String path = preScaledPath(iconId, size);
        ResourceLocation candidate = path == null ? null : ResourceLocation.tryParse(path);
        Minecraft minecraft = Minecraft.getInstance();
        if (candidate != null && minecraft != null
                && minecraft.getResourceManager().getResource(candidate).isPresent()) {
            found = candidate;
        }
        EMBLEMS.put(key, found);
        return found;
    }

    /** Pure: {@code ns:textures/x.png} → {@code ns:textures/x_32.png}; {@code null} otherwise. */
    static String preScaledPath(String iconId, int size) {
        if (iconId == null || !iconId.endsWith(".png")) {
            return null;
        }
        return iconId.substring(0, iconId.length() - 4) + "_" + size + ".png";
    }

    // ---- content ------------------------------------------------------------------------------------

    private record Line(int column, int y, List<String> lines, String right, int color,
                        boolean indent, int section, boolean first) {
    }

    private record Title(int column, int y, String title, String meta, int section) {
    }

    private void content(GuiGraphics graphics, Font font, UiRect bounds, FormationVoteModel model,
                         FormationSelectionView formation, TacticalShellLayout.Metrics metrics) {
        lastContent = bounds;
        if (bounds.height() < GLYPH || bounds.width() < 24) {
            maxScroll = 0;
            return;
        }
        int columns = bounds.width() >= 480 ? 3 : bounds.width() >= 300 ? 2 : 1;
        int columnGap = metrics.roomy() ? 12 : 8;
        int columnWidth = Math.max(1, (bounds.width() - SCROLLBAR - (columns - 1) * columnGap)
                / columns);
        List<String> description = formation.description().isBlank() ? List.of()
                : TextFit.wrap(font, formation.description(), bounds.width() - SCROLLBAR, 0);
        List<FormationText.Section> sections = FormationText.sections(model.snapshot(), formation);
        int[] columnHeight = new int[columns];
        java.util.Arrays.fill(columnHeight, description.isEmpty() ? 0
                : description.size() * LINE + SECTION_GAP);
        List<Title> titles = new ArrayList<>();
        List<Line> lines = new ArrayList<>();
        for (int index = 0; index < sections.size(); index++) {
            FormationText.Section section = sections.get(index);
            int column = 0;
            for (int c = 1; c < columns; c++) {
                if (columnHeight[c] < columnHeight[column]) {
                    column = c;
                }
            }
            int y = columnHeight[column];
            titles.add(new Title(column, y, section.title(), section.meta(), index));
            int rowY = y + HEAD;
            boolean first = true;
            for (FormationText.SectionRow row : section.rows()) {
                int width = columnWidth - (row.indent() ? 8 : 0);
                List<String> wrapped = row.wrap() ? TextFit.wrap(font, row.left(), width, 0)
                        : List.of(row.left());
                lines.add(new Line(column, rowY, wrapped, row.right(),
                        row.muted() ? TacticalBoardTheme.MUTED : TacticalBoardTheme.TEXT,
                        row.indent(), index, first));
                first = false;
                rowY += wrapped.size() * LINE;
            }
            columnHeight[column] = rowY + SECTION_GAP;
        }
        int total = 0;
        for (int height : columnHeight) {
            total = Math.max(total, height - SECTION_GAP);
        }
        boolean overflow = total > bounds.height();
        int view = overflow ? bounds.height() - (LINE + 2) : bounds.height();
        maxScroll = overflow ? roundUp(Math.max(0, total - view), LINE) : 0;
        scroll = Math.max(0, Math.min(maxScroll, scroll));

        int hidden = 0;
        boolean hiddenBelow = false;
        List<String> hiddenNames = new ArrayList<>();
        UiScale.enableScissor(graphics, bounds);
        try {
            boolean descriptionCut = false;
            for (int index = 0; index < description.size(); index++) {
                int y = index * LINE;
                if (visible(y, 1, scroll, view)) {
                    graphics.drawString(font, description.get(index), bounds.left(),
                            bounds.top() + y - scroll, TacticalBoardTheme.TEXT, false);
                } else {
                    descriptionCut = true;
                    hiddenBelow |= y - scroll >= 0;
                }
            }
            if (descriptionCut) {
                hidden++;
                hiddenNames.add(FormationText.descriptionName());
            }
            boolean[] sectionCut = new boolean[sections.size()];
            boolean[] firstShown = new boolean[sections.size()];
            for (Line line : lines) {
                int x = bounds.left() + line.column() * (columnWidth + columnGap);
                if (!visible(line.y(), line.lines().size(), scroll, view)) {
                    hidden++;
                    sectionCut[line.section()] = true;
                    hiddenBelow |= line.y() - scroll >= 0;
                    continue;
                }
                if (line.first()) {
                    firstShown[line.section()] = true;
                }
                drawLine(graphics, font, line, x, bounds.top() - scroll, columnWidth);
            }
            for (Title title : titles) {
                int y = title.y();
                if (!firstShown[title.section()] || !visible(y, 1, scroll, view)) {
                    continue;
                }
                drawTitle(graphics, font, title, bounds.left() + title.column()
                        * (columnWidth + columnGap), bounds.top() + y - scroll, columnWidth);
            }
            for (int index = 0; index < sections.size(); index++) {
                if (sectionCut[index]) {
                    hiddenNames.add(sections.get(index).title());
                }
            }
        } finally {
            UiScale.disableScissor(graphics);
        }
        if (overflow) {
            int moreY = bounds.bottom() - GLYPH - 1;
            int moreWidth = bounds.width() - SCROLLBAR - 12;
            if (hidden > 0) {
                (hiddenBelow ? TacticalIcon.DOWN : TacticalIcon.UP).draw(graphics, bounds.left(),
                        moreY - 1, TacticalBoardTheme.MUTED);
                drawStringVariant(graphics, font, FormationText.moreLines(hidden, hiddenNames),
                        bounds.left() + 12, moreY, moreWidth, TacticalBoardTheme.MUTED);
            }
            TacticalDraw.scrollbar(graphics, new UiRect(bounds.right() - 2, bounds.top(),
                    bounds.right(), bounds.bottom()), total, view, scroll);
        }
    }

    /** Pure: whether a block of {@code lineCount} lines at {@code y} fits whole in the view. */
    static boolean visible(int y, int lineCount, int scroll, int view) {
        int top = y - scroll;
        return top >= 0 && top + (Math.max(1, lineCount) - 1) * LINE + GLYPH <= view;
    }

    private static int roundUp(int value, int step) {
        return (value + step - 1) / step * step;
    }

    private static void drawTitle(GuiGraphics graphics, Font font, Title title, int x, int y,
                                  int width) {
        graphics.fill(x, y + 1, x + 2, y + 8, TacticalBoardTheme.SECTION);
        int metaWidth = title.meta() == null || title.meta().isEmpty() ? 0
                : font.width(title.meta());
        TextFit.draw(graphics, font, title.title(), x + 5, y,
                Math.max(0, width - 5 - (metaWidth > 0 ? metaWidth + 4 : 0)),
                TacticalBoardTheme.TEXT, TextFit.Align.LEFT);
        if (metaWidth > 0) {
            graphics.drawString(font, title.meta(), x + width - metaWidth, y,
                    TacticalBoardTheme.MUTED, false);
        }
        graphics.fill(x, y + 10, x + width, y + 11, TacticalBoardTheme.EDGE);
    }

    private static void drawLine(GuiGraphics graphics, Font font, Line line, int x, int top,
                                 int width) {
        int left = x + (line.indent() ? 8 : 0);
        int y = top + line.y();
        if (line.indent()) {
            graphics.fill(x + 2, y + 3, x + 4, y + 5, TacticalBoardTheme.MUTED);
        }
        int rightWidth = line.right() == null || line.right().isEmpty() ? 0
                : font.width(line.right());
        for (int index = 0; index < line.lines().size(); index++) {
            int room = x + width - left - (rightWidth > 0 && index == 0 ? rightWidth + 6 : 0);
            TextFit.draw(graphics, font, line.lines().get(index), left, y + index * LINE,
                    Math.max(0, room), line.color(), TextFit.Align.LEFT);
        }
        if (rightWidth > 0) {
            graphics.drawString(font, line.right(), x + width - rightWidth, y,
                    TacticalBoardTheme.MUTED, false);
        }
    }

    // ---- action bar ---------------------------------------------------------------------------------

    /** Reason text with its leading icon, left of the vote key (preview {@code reasonLine}). */
    static void drawReason(GuiGraphics graphics, Font font, int x, int y, int maxWidth,
                           TacticalIcon icon, List<Component> text, int color, int iconColor) {
        if (maxWidth < 24 || text.isEmpty()) {
            return;
        }
        (icon == null ? TacticalIcon.INFO : icon).draw(graphics, x, y - 1, iconColor);
        drawVariant(graphics, font, text, x + 12, y, maxWidth - 12, color);
    }

    /** "你的票" badge in place of the vote key (green, not a key). */
    static void drawMineBadge(GuiGraphics graphics, Font font, UiRect box, Component label) {
        graphics.fill(box.left(), box.top(), box.right(), box.bottom(),
                TacticalBoardTheme.SUCCESS_SOFT);
        BattleUiTheme.outline(graphics, box.left(), box.top(), box.right(), box.bottom(),
                TacticalBoardTheme.SUCCESS);
        int textWidth = TacticalIcon.ADVANCE + font.width(label);
        int x = box.left() + Math.max(3, (box.width() - textWidth) / 2);
        TacticalIcon.CHECK.draw(graphics, x, box.top() + (box.height() - TacticalIcon.SIZE) / 2,
                TacticalBoardTheme.SUCCESS);
        TextFit.draw(graphics, font, label, x + TacticalIcon.ADVANCE,
                box.top() + (box.height() - 8) / 2, Math.max(0, box.right() - x - 14),
                TacticalBoardTheme.SUCCESS, TextFit.Align.LEFT);
    }

    /** Draws the first variant that fits (the last one ellipsized otherwise). */
    static void drawVariant(GuiGraphics graphics, Font font, List<Component> variants, int x,
                            int y, int maxWidth, int color) {
        if (variants.isEmpty() || maxWidth <= 0) {
            return;
        }
        TextFit.draw(graphics, font, pick(font, variants, maxWidth), x, y, maxWidth, color,
                TextFit.Align.LEFT);
    }

    static Component pick(Font font, List<Component> variants, int maxWidth) {
        for (Component variant : variants) {
            if (font.width(variant) <= maxWidth) {
                return variant;
            }
        }
        return variants.get(variants.size() - 1);
    }

    private static void drawStringVariant(GuiGraphics graphics, Font font, List<String> variants,
                                          int x, int y, int maxWidth, int color) {
        String chosen = variants.get(variants.size() - 1);
        for (String variant : variants) {
            if (font.width(variant) <= maxWidth) {
                chosen = variant;
                break;
            }
        }
        TextFit.draw(graphics, font, chosen, x, y, Math.max(0, maxWidth), color,
                TextFit.Align.LEFT);
    }
}
