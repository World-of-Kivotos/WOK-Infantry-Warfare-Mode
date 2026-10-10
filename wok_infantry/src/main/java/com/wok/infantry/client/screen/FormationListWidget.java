package com.wok.infantry.client.screen;

import com.wok.infantry.client.hud.TacticalHud;
import com.wok.infantry.formation.selection.FactionSelectionView;
import com.wok.infantry.formation.selection.FormationSelectionView;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * Formation list of the vote page (preview {@code formationList}/{@code formationRow}): a dark
 * {@link TacticalList} grouped by category, one row per candidate. A row never shows a head count
 * (formations hold nobody before the lock); it shows votes and their share, my vote (green bar
 * and check), the locked result (green lock), "未入选", "停用" and "容量不足".
 *
 * <p>Clicking a row, in any phase and also when it is unavailable, only moves the highlight; it
 * never sends a vote (vote-09, player-06). The list instance lives as long as the screen, so the
 * scroll position and keyboard focus survive snapshot updates.
 */
final class FormationListWidget {
    private final TacticalList<FormationSelectionView> list;
    private FormationVoteModel model;

    FormationListWidget(Consumer<FormationSelectionView> onHighlight,
                        Consumer<FormationSelectionView> onActivate) {
        this.list = new TacticalList<FormationSelectionView>(0, 0, 0, 0,
                Component.translatable(FormationText.PREFIX + "panel.list"), this::spec)
                .keyedBy(FormationSelectionView::id)
                .groupBy(FormationText::category)
                .renderer(this::renderRow)
                .onSelect((index, formation) -> onHighlight.accept(formation))
                .onActivate((index, formation) -> onActivate.accept(formation))
                .emptyState(TacticalIcon.INFO,
                        Component.translatable(FormationText.PREFIX + "list_empty"), null);
    }

    TacticalList<FormationSelectionView> widget() {
        return list;
    }

    /**
     * Updates rows, geometry and the highlighted row. The selection follows the formation id and
     * the scroll position is kept (and clamped when the list got shorter).
     */
    void update(FormationVoteModel model, TacticalShellLayout.Metrics metrics, UiRect bounds) {
        this.model = model;
        list.rowHeight(FormationScreenLayout.rowHeight(metrics))
                .headerHeight(FormationScreenLayout.groupHeight(metrics));
        list.setBounds(bounds.left(), bounds.top(), bounds.width(), bounds.height());
        list.visible = !bounds.isEmpty();
        list.setItems(FormationVoteModel.orderedFormations(model.browsing()));
        FormationSelectionView highlighted = model.highlighted();
        int index = highlighted == null ? -1 : list.items().indexOf(highlighted);
        if (index != list.selectedIndex()) {
            list.setSelectedIndex(index);
        }
    }

    /** Natural height of the well: every group title and row plus the well edges. */
    static int naturalHeight(FactionSelectionView faction, TacticalShellLayout.Metrics metrics) {
        List<FormationSelectionView> ordered = FormationVoteModel.orderedFormations(faction);
        if (ordered.isEmpty()) {
            // Room for the list's empty state instead of a 2px sliver.
            return 3 * FormationScreenLayout.rowHeight(metrics) + 2;
        }
        int groups = 0;
        String previous = null;
        for (FormationSelectionView formation : ordered) {
            if (!formation.categoryId().equals(previous)) {
                groups++;
                previous = formation.categoryId();
            }
        }
        return groups * FormationScreenLayout.groupHeight(metrics)
                + ordered.size() * FormationScreenLayout.rowHeight(metrics) + 2;
    }

    private TacticalDraw.RowSpec spec(FormationSelectionView formation) {
        TacticalDraw.RowSpec spec = TacticalDraw.RowSpec.of(formation.displayName());
        if (model == null) {
            return spec;
        }
        FormationVoteModel.RowStatus status = model.rowStatus(formation);
        spec = spec.withSub(FormationText.rowSub(model, formation, status));
        if (status.dimmed()) {
            // Still selectable: hovering explains why it cannot be voted for (player-06).
            spec = spec.withDisabledReason(FormationText.rowSub(model, formation, status));
        }
        return spec;
    }

    private void renderRow(GuiGraphics graphics, Font font, UiRect row,
                           FormationSelectionView formation, TacticalDraw.RowSpec spec,
                           TacticalDraw.RowState state) {
        if (model == null) {
            TacticalDraw.row(graphics, font, row, spec, state);
            return;
        }
        FormationVoteModel.RowStatus status = model.rowStatus(formation);
        boolean selected = state.selected();
        boolean locked = status.mark() == FormationVoteModel.RowMark.LOCKED;
        boolean off = status.mark() == FormationVoteModel.RowMark.DISABLED
                || status.mark() == FormationVoteModel.RowMark.SHORTFALL;
        int lead = status.mine() || locked ? TacticalBoardTheme.SUCCESS_B : 0;
        TacticalDraw.RowState background = background(state);
        TacticalDraw.rowBg(graphics, row, background, lead);
        int nameColor = selected ? TacticalBoardTheme.ON_SELECT
                : status.dimmed() ? TacticalBoardTheme.OFFLINE : TacticalBoardTheme.LIGHT;
        int mute = selected ? TacticalBoardTheme.SELECT_SUB : TacticalBoardTheme.LIGHT_MUTED;
        int mineColor = selected ? TacticalBoardTheme.ON_SELECT : TacticalBoardTheme.SUCCESS_B;
        boolean twoLines = row.height() >= 20;
        int y1 = twoLines ? row.top() + 3 : row.top() + Math.floorDiv(row.height() - 8, 2);
        Component mark = FormationText.rowMark(status, twoLines);
        int markWidth = mark == null ? 0 : font.width(mark);
        int x = row.right() - 4 - markWidth;
        if (mark != null) {
            graphics.drawString(font, mark, x, y1, locked
                    ? (selected ? TacticalBoardTheme.ON_SELECT : TacticalBoardTheme.SUCCESS_B)
                    : mute, false);
        }
        if (off || locked) {
            x -= TacticalIcon.ADVANCE;
            TacticalIcon.LOCK.draw(graphics, x, y1 - 1, locked && !selected
                    ? TacticalBoardTheme.SUCCESS_B : mute);
        }
        if (status.mine() && !twoLines) {
            x -= TacticalIcon.ADVANCE;
            TacticalIcon.CHECK.draw(graphics, x, y1 - 1, mineColor);
        }
        int nameLeft = row.left() + 6;
        TextFit.draw(graphics, font, formation.displayName(), nameLeft, y1,
                Math.max(0, x - 4 - nameLeft), nameColor, TextFit.Align.LEFT);
        if (twoLines) {
            int y2 = row.top() + 13;
            int subRight = row.right() - 4;
            if (status.mine()) {
                Component mine = FormationText.mine();
                int mineWidth = font.width(mine);
                graphics.drawString(font, mine, subRight - mineWidth, y2, mineColor, false);
                TacticalIcon.CHECK.draw(graphics, subRight - mineWidth - TacticalIcon.ADVANCE,
                        y2 - 1, mineColor);
                subRight -= mineWidth + 15;
            }
            TextFit.draw(graphics, font, FormationText.rowSub(model, formation, status),
                    nameLeft, y2, Math.max(0, subRight - nameLeft), mute, TextFit.Align.LEFT);
        }
        if (status.showVotes()) {
            int total = Math.max(1, model.totalVotes());
            int share = Math.round((row.width() - 8) * status.votes() / (float) total);
            graphics.fill(nameLeft, row.bottom() - 1, row.right() - 2, row.bottom(),
                    shareTrack(background));
            graphics.fill(nameLeft, row.bottom() - 1, Math.min(row.right() - 2, nameLeft + share),
                    row.bottom(), selected ? TacticalBoardTheme.SELECT_BAR
                            : status.mine() || locked ? TacticalBoardTheme.SUCCESS_B
                            : TacticalBoardTheme.NEUTRAL_B);
        }
    }

    /**
     * Background state of a row: every row stays selectable (an unavailable one only explains
     * itself), and a hovered selected row lights up one step like keys and cards (P3,
     * {@link TacticalDraw.RowState#brightensSelection}).
     */
    static TacticalDraw.RowState background(TacticalDraw.RowState state) {
        return (state == null ? TacticalDraw.RowState.NORMAL : state).withDisabled(false);
    }

    /**
     * Track of the vote-share hairline: on the selection a wash of its light bar over the row's own
     * fill (the brighter one while hovered, {@link TacticalDraw#rowFill}), otherwise the well.
     */
    static int shareTrack(TacticalDraw.RowState background) {
        return background.selected()
                ? TacticalHud.mix(TacticalDraw.rowFill(background, false),
                TacticalBoardTheme.SELECT_BAR, 0.3D)
                : TacticalBoardTheme.WELL;
    }
}
