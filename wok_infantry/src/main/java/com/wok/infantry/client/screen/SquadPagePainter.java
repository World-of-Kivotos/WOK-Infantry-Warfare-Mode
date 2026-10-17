package com.wok.infantry.client.screen;

import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.battle.ClassLimitView;
import com.wok.infantry.battle.MemberState;
import com.wok.infantry.battle.MemberView;
import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.battle.SquadView;
import com.wok.infantry.client.ClientFormationState;
import com.wok.infantry.client.hud.SquadRosterModel;
import com.wok.infantry.deployment.DeploymentPhase;
import com.wok.infantry.formation.vote.FormationVotePhase;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Squad page of the battle terminal (preview {@code 20-squad.js} {@code newSquadsPage}).
 *
 * <p>Wide (content ≥ {@value #WIDE_MIN_WIDTH}): the five call signs as a strip on top, the viewed
 * squad's roster below and a right column with "我的状态", "指挥官" and "阵营兵力". Narrow (on the
 * D2 device also 640×336 and 640×360): the call-sign list and the commander on the left (plus the
 * faction strength when it fits), the roster on the right. Under
 * the roster the operations of the viewed squad: on the own squad the target line and the 2+2
 * group (hand over / kick | leave / disband); on another squad only join (or create) and back to
 * the own squad. One line under the keys says why a key is disabled or what it does. Before the
 * formation is locked the roster gives way to the vote block and no call sign can be opened.
 */
final class SquadPagePainter implements SquadScreen.Painter {
    static final String STRIP_UI_ID_PREFIX = "squad.strip/";
    static final String LIST_UI_ID = "squad.list";
    static final String ROSTER_UI_ID = "squad.roster";
    static final String STRIP_BOX = "squad.strip_panel";
    static final String LIST_BOX = "squad.list_panel";
    static final String ROSTER_BOX = "squad.roster_panel";
    static final String STATUS_BOX = "squad.status_panel";
    static final String COMMANDER_BOX = "squad.commander_panel";
    static final String FACTION_BOX = "squad.faction_panel";
    static final String VOTE_BOX = "squad.vote_panel";
    /**
     * Narrowest content of the wide layout (preview {@code 20-squad.js}). On the D2 device
     * 640×336 and 640×360 give 576 and are narrow (the strip becomes the list column); 960×540
     * gives 846 and stays wide.
     */
    static final int WIDE_MIN_WIDTH = 600;

    private final SquadScreen host;
    private final Font font;
    private final TacticalShellLayout.Metrics metrics;
    private final boolean wide;
    private final boolean vote;
    private UiRect strip = UiRect.EMPTY;
    private UiRect listPanel = UiRect.EMPTY;
    private UiRect statusPanel = UiRect.EMPTY;
    private UiRect commanderPanel = UiRect.EMPTY;
    private UiRect factionPanel = UiRect.EMPTY;
    private UiRect mainPanel = UiRect.EMPTY;
    private FormationVotePanel.Layout voteLayout;
    private FormationVotePanel.Data voteData;
    private RosterLayout roster;
    private TacticalList<UUID> rosterList;
    private CommanderLayout commander;

    /** Planned geometry of the roster panel. */
    private record RosterLayout(UiRect content, UiRect well, int rowHeight, int slots,
                                int fitRows, UiRect chips, int targetY, UiRect buttons,
                                int reasonY, boolean targetRow) {
    }

    /** Planned geometry of the commander panel. */
    private record CommanderLayout(UiRect content, int firstRow, boolean squadRow, UiRect keys,
                                   int reasonY) {
    }

    SquadPagePainter(SquadScreen host, UiRect body, SquadBoardModel model) {
        this.host = host;
        this.font = host.boardFont();
        this.metrics = host.boardMetrics();
        this.vote = model.votePending();
        this.wide = wide(body);
        int gap = metrics.gap();
        int kv = metrics.roomy() ? 13 : 11;
        if (wide) {
            List<UiRect> rows = body.rows(gap, UiRect.Size.px(metrics.roomy() ? 44 : 34),
                    UiRect.Size.STAR);
            strip = rows.get(0);
            List<UiRect> cols = rows.get(1).cols(gap, UiRect.Size.STAR,
                    UiRect.Size.px(metrics.roomy() ? 280 : 176));
            mainPanel = cols.get(0);
            int statusHeight = SquadBoardBlocks.panelHeight(metrics, 6 * kv - 2, true);
            int factionHeight = SquadBoardBlocks.panelHeight(metrics, 29, true);
            List<UiRect> right = cols.get(1).rows(gap, UiRect.Size.px(statusHeight),
                    UiRect.Size.STAR, UiRect.Size.px(factionHeight));
            statusPanel = right.get(0);
            commanderPanel = right.get(1);
            factionPanel = right.get(2);
        } else {
            int leftWidth = body.width() < 400 ? 118 : (int) Math.floor(body.width() * 0.36D);
            List<UiRect> cols = body.cols(gap, UiRect.Size.px(leftWidth), UiRect.Size.STAR);
            UiRect left = cols.get(0);
            mainPanel = cols.get(1);
            int commanderHeight = SquadBoardBlocks.panelHeight(metrics,
                    kv * (metrics.tight() ? 1 : 2) + metrics.buttonHeight() + 3 + 9, true);
            int factionHeight = SquadBoardBlocks.panelHeight(metrics, 29, true);
            int minimumList = SquadBoardBlocks.panelHeight(metrics,
                    5 * (metrics.tight() ? 22 : 26) + 1, true);
            boolean withFaction = left.height() - commanderHeight - factionHeight - 2 * gap
                    >= minimumList;
            List<UiRect> parts = withFaction
                    ? left.rows(gap, UiRect.Size.STAR, UiRect.Size.px(commanderHeight),
                    UiRect.Size.px(factionHeight))
                    : left.rows(gap, UiRect.Size.STAR, UiRect.Size.px(commanderHeight));
            listPanel = parts.get(0);
            commanderPanel = parts.get(1);
            factionPanel = withFaction ? parts.get(2) : UiRect.EMPTY;
        }
        if (wide) {
            addStrip(model);
        } else {
            addSquadList(model);
        }
        if (vote) {
            voteData = FormationVotePanel.of(ClientFormationState.snapshot(), model.snapshot());
            voteLayout = FormationVotePanel.layout(font, metrics, mainPanel,
                    new FormationVotePanel.Options(VOTE_BOX,
                            SquadBoardText.t(SquadBoardText.VOTE_TITLE), null, null, null, null,
                            true, null), voteData);
            FormationVotePanel.Block block = voteLayout.block();
            host.addKey(block.button(), FormationVotePanel.buttonLabel(voteData),
                    FormationVotePanel.buttonIcon(voteData), null, host::openFormationTab,
                    SquadScreen.VOTE_UI_ID);
        } else {
            addRoster(model);
        }
        addCommander(model);
    }

    /** Whether {@code body} (the shell's content) gets the call-sign strip and right column. */
    static boolean wide(UiRect body) {
        return body.width() >= WIDE_MIN_WIDTH;
    }

    // ---- call signs -------------------------------------------------------------------------------

    private void addStrip(SquadBoardModel model) {
        UiRect inner = strip.inset(1);
        List<UiRect> cells = inner.cols(1, UiRect.Size.STAR, UiRect.Size.STAR, UiRect.Size.STAR,
                UiRect.Size.STAR, UiRect.Size.STAR);
        List<SquadBoardModel.SquadRow> rows = model.squads();
        for (int index = 0; index < rows.size() && index < cells.size(); index++) {
            SquadBoardModel.SquadRow row = rows.get(index);
            SquadCallsign callsign = row.callsign();
            BoardCellButton cell = new BoardCellButton(cells.get(index), row.name(),
                    !vote && row.viewed(),
                    (graphics, bounds, state, keyboard) -> drawSquadRow(graphics, bounds,
                            host.liveModel(), callsign, state, 0, true),
                    () -> host.viewSquad(callsign));
            Component info = rowTooltip(model, row);
            if (vote) {
                cell.withDisabledReason(Component.empty().append(info).append("\n").append(
                        SquadBoardModel.Reason.of(SquadBoardModel.ReasonCode.VOTE_PENDING)
                                .full()));
            } else {
                cell.setTooltip(net.minecraft.client.gui.components.Tooltip.create(info));
            }
            host.addBoardWidget(cell, STRIP_UI_ID_PREFIX + callsign.id());
        }
    }

    private void addSquadList(SquadBoardModel model) {
        UiRect content = TacticalDraw.panelContent(listPanel, metrics,
                TacticalDraw.PanelStyle.titled(listTitle()));
        int rows = SquadCallsign.values().length;
        int rowHeight = Math.max(metrics.rowHeight(), Math.min(metrics.tight() ? 26 : 30,
                (content.height() - 2) / rows));
        UiRect well = new UiRect(content.left(), content.top(), content.right(),
                Math.min(content.bottom(), content.top() + rows * rowHeight + 2));
        TacticalList<SquadCallsign> list = new TacticalList<>(well.left(), well.top(),
                well.width(), well.height(), Component.translatable("screen.wok_infantry.squad_list"),
                callsign -> squadSpec(host.liveModel(), callsign));
        list.rowHeight(rowHeight).keyedBy(SquadCallsign::id)
                .renderer((graphics, font, bounds, callsign, spec, state) -> drawSquadRow(graphics,
                        bounds, host.liveModel(), callsign, state,
                        callsign.ordinal(), false))
                .onSelect((index, callsign) -> {
                    if (vote) {
                        return;
                    }
                    host.viewSquad(callsign);
                })
                .onActivate((index, callsign) -> {
                    if (!vote) {
                        host.viewSquad(callsign);
                    }
                });
        List<SquadCallsign> items = new ArrayList<>();
        for (SquadBoardModel.SquadRow row : model.squads()) {
            items.add(row.callsign());
        }
        list.setItems(items);
        if (!vote) {
            list.setSelectedIndex(items.indexOf(model.viewedSquad()));
        }
        host.addBoardWidget(list, LIST_UI_ID);
    }

    private TacticalDraw.RowSpec squadSpec(SquadBoardModel model, SquadCallsign callsign) {
        SquadBoardModel.SquadRow row = model.squad(callsign);
        TacticalDraw.RowSpec spec = TacticalDraw.RowSpec.of(row == null
                ? SquadLabels.callsign(callsign) : row.name());
        if (row == null) {
            return spec;
        }
        spec = spec.withTooltip(rowTooltip(model, row));
        return vote ? spec.withDisabledReason(SquadBoardModel.Reason.of(
                SquadBoardModel.ReasonCode.VOTE_PENDING).full()) : spec;
    }

    private static Component rowTooltip(SquadBoardModel model, SquadBoardModel.SquadRow row) {
        Component state = row.own() || !row.status().hasLabel() ? row.subCandidates().get(0)
                : row.status().label();
        return SquadBoardText.t(SquadBoardText.ROW_TOOLTIP, row.name(), row.count(), state);
    }

    private Component listTitle() {
        return Component.translatable("screen.wok_infantry.squad_list");
    }

    private Component listMeta(SquadBoardModel model, int width) {
        int cap = (int) Math.floor((width - 2) * 0.4D);
        if (vote) {
            boolean open = model.input().votePhase() == FormationVotePhase.OPEN;
            return fitOrEmpty(open
                    ? List.of(SquadBoardText.t(SquadBoardText.VOTE_META_OPEN),
                    SquadBoardText.t(SquadBoardText.VOTE_META_OPEN_SHORT))
                    : List.of(SquadBoardText.t(SquadBoardText.VOTE_META_WAIT),
                    SquadBoardText.t(SquadBoardText.VOTE_META_WAIT_SHORT)), cap);
        }
        long open = model.squads().stream().filter(SquadBoardModel.SquadRow::configured).count();
        MutableComponent formation = SquadLabels.formationName(model.snapshot());
        List<Component> variants = new ArrayList<>();
        if (formation != null) {
            variants.add(SquadBoardText.t(SquadBoardText.LIST_META, formation, open));
        }
        variants.add(SquadBoardText.t(SquadBoardText.LIST_META_SHORT, open));
        return fitOrEmpty(variants, cap);
    }

    private Component fitOrEmpty(List<Component> variants, int width) {
        for (Component variant : variants) {
            if (font.width(variant) <= width) {
                return variant;
            }
        }
        return Component.empty();
    }

    /** Tone of a status word on a dark row. */
    private static int toneColor(SquadBoardModel.Tone tone) {
        return switch (tone) {
            case SUCCESS -> TacticalBoardTheme.SUCCESS_B;
            case DANGER -> TacticalBoardTheme.DANGER_B;
            case NEUTRAL, MUTED -> TacticalBoardTheme.LIGHT_MUTED;
            case FAINT -> TacticalBoardTheme.FAINT;
        };
    }

    /**
     * One call sign (preview {@code squadRow}): one line (name, count or status), two lines (name,
     * own tag and count; leader or state and status word) or three (plus capacity pips and the
     * class spread). Text on the selected row uses the selection colours.
     */
    private void drawSquadRow(GuiGraphics graphics, UiRect bounds, SquadBoardModel model,
                              SquadCallsign callsign, TacticalDraw.RowState state, int index,
                              boolean horizontal) {
        SquadBoardModel.SquadRow row = model.squad(callsign);
        if (row == null) {
            return;
        }
        boolean selected = state.selected();
        boolean dim = !row.configured();
        TacticalDraw.rowBg(graphics, bounds, horizontal ? state.withAlt(false) : state,
                row.own() ? TacticalBoardTheme.SUCCESS_B : 0);
        int pitch = bounds.height() + (horizontal ? 0 : 1);
        int lines = pitch >= 40 ? 3 : pitch >= 21 ? 2 : 1;
        int x = bounds.left() + 6;
        int right = bounds.right() - 4;
        int y = bounds.top() + Math.floorDiv(bounds.height() - (lines * 11 - 3), 2);
        int nameColor = selected ? TacticalBoardTheme.ON_SELECT : dim ? TacticalBoardTheme.FAINT
                : TacticalBoardTheme.LIGHT;
        int subColor = selected ? TacticalBoardTheme.SELECT_SUB : dim ? TacticalBoardTheme.FAINT
                : TacticalBoardTheme.LIGHT_MUTED;
        boolean voting = row.status() == SquadBoardModel.SquadStatus.VOTE_PENDING;
        Component ownTag = SquadBoardModel.SquadStatus.OWN.label();
        int ownWidth = row.own() ? font.width(ownTag) + 4 : 0;
        Component count = row.count();
        if (lines == 1) {
            boolean status = !row.own() && row.status().hasLabel() && !row.full();
            Component value = status ? row.status().label() : count;
            int valueColor = selected ? TacticalBoardTheme.ON_SELECT : row.full()
                    ? TacticalBoardTheme.DANGER_B : row.own() ? TacticalBoardTheme.LIGHT_MUTED
                    : toneColor(row.status().tone());
            int valueWidth = font.width(value);
            int nameWidth = drawName(graphics, row, x, y,
                    right - valueWidth - 6 - ownWidth - x, nameColor);
            if (row.own()) {
                TextFit.draw(graphics, font, ownTag, x + nameWidth + 4, y, ownWidth,
                        selected ? TacticalBoardTheme.ON_SELECT : TacticalBoardTheme.SUCCESS_B,
                        TextFit.Align.LEFT);
            }
            TextFit.draw(graphics, font, value, right - valueWidth, y, valueWidth, valueColor,
                    TextFit.Align.LEFT);
            return;
        }
        int countWidth = font.width(count);
        int nameWidth = drawName(graphics, row, x, y, right - countWidth - 6 - ownWidth - x,
                nameColor);
        if (row.own()) {
            TextFit.draw(graphics, font, ownTag, x + nameWidth + 4, y, ownWidth,
                    selected ? TacticalBoardTheme.ON_SELECT : TacticalBoardTheme.SUCCESS_B,
                    TextFit.Align.LEFT);
        }
        TextFit.draw(graphics, font, count, right - countWidth, y, countWidth,
                selected ? TacticalBoardTheme.ON_SELECT : row.full() ? TacticalBoardTheme.DANGER_B
                        : dim ? TacticalBoardTheme.FAINT : TacticalBoardTheme.LIGHT_MUTED,
                TextFit.Align.LEFT);
        y += 11;
        Component status = row.own() || voting || !row.status().hasLabel() ? null
                : row.status().label();
        List<Component> sub = row.subCandidates();
        int all = right - x;
        if (!row.configured() && status != null) {
            // "本阵营编制无此呼号" beats the redundant "未开放" (line 1 already shows "—").
            if (font.width(sub.get(0)) + 6 + font.width(status) > all
                    && font.width(sub.get(0)) <= all) {
                status = null;
            }
        }
        int statusWidth = status == null ? 0 : font.width(status);
        Component subText = FormationDetailPanel.pick(font, sub,
                all - (statusWidth > 0 ? statusWidth + 6 : 0));
        TextFit.draw(graphics, font, subText, x, y, all - (statusWidth > 0 ? statusWidth + 6 : 0),
                subColor, TextFit.Align.LEFT);
        if (status != null) {
            TextFit.draw(graphics, font, status, right - statusWidth, y, statusWidth,
                    selected ? TacticalBoardTheme.ON_SELECT : toneColor(row.status().tone()),
                    TextFit.Align.LEFT);
        }
        if (lines == 3 && row.configured()) {
            y += 11;
            int pipColor = selected ? TacticalBoardTheme.ON_SELECT : row.full()
                    ? TacticalBoardTheme.DANGER_B : row.own() ? TacticalBoardTheme.SUCCESS_B
                    : TacticalBoardTheme.NEUTRAL_B;
            int capacity = Math.min(row.capacity(), Math.max(0, (right - x) / 6));
            int end = SquadBoardBlocks.pips(graphics, x, y + 2, Math.min(row.members(), capacity),
                    capacity, pipColor, selected ? SquadBoardBlocks.alpha(
                            TacticalBoardTheme.ON_SELECT, 0x80) : TacticalBoardTheme.CELL_EDGE);
            Component spread = classSpread(model.snapshot(), row.view());
            if (!spread.getString().isEmpty() && right - end - 6 > 12) {
                TextFit.draw(graphics, font, spread, end + 6, y, right - end - 6, subColor,
                        TextFit.Align.LEFT);
            }
        }
    }

    /** Full call sign, or the short one when the full one does not fit; returns the width. */
    private int drawName(GuiGraphics graphics, SquadBoardModel.SquadRow row, int x, int y,
                         int room, int color) {
        Component name = font.width(row.name()) <= room ? row.name() : row.shortName();
        return TextFit.draw(graphics, font, name, x, y, Math.max(0, room), color,
                TextFit.Align.LEFT).width();
    }

    /** "突击兵2 医疗兵1": the classes held in a squad, in class order. */
    private static Component classSpread(BattleSnapshot snapshot, SquadView squad) {
        if (squad == null || squad.members().isEmpty()) {
            return Component.empty();
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (snapshot != null) {
            snapshot.classQuotas().forEach(quota -> counts.put(quota.classId(), 0));
        }
        for (MemberView member : squad.members()) {
            counts.merge(member.classId(), 1, Integer::sum);
        }
        MutableComponent result = Component.empty();
        boolean first = true;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() <= 0) {
                continue;
            }
            if (!first) {
                result.append(" ");
            }
            first = false;
            result.append(SquadLabels.className(snapshot, entry.getKey()))
                    .append(String.valueOf(entry.getValue()));
        }
        return result;
    }

    // ---- roster -----------------------------------------------------------------------------------

    private void addRoster(SquadBoardModel model) {
        SquadBoardModel.SquadRow row = model.viewedRow();
        UiRect content = TacticalDraw.panelContent(mainPanel, metrics,
                TacticalDraw.PanelStyle.titled(rosterTitle(row)));
        SquadBoardModel.RosterActions actions = model.rosterActions();
        int line = 11;
        boolean targetRow = actions.targetRow();
        boolean hasButtons = !actions.buttons().isEmpty();
        int actionsHeight = (targetRow ? line : 0) + (hasButtons ? metrics.buttonHeight() : 0)
                + (actions.reason() != null ? line + 1 : 0);
        int members = row == null ? 0 : row.members();
        boolean chips = row != null && row.configured() && members > 0 && content.width() >= 240
                && row.view() != null && !row.view().classLimits().isEmpty();
        int chipsHeight = chips ? 11 + metrics.gap() : 0;
        UiRect well = new UiRect(content.left(), content.top(), content.right(),
                Math.max(content.top(), content.bottom() - actionsHeight - metrics.gap()
                        - chipsHeight));
        int capacity = row == null ? 0 : row.capacity();
        int slots = Math.max(capacity, members);
        int rowHeight = slots == 0 ? metrics.rowHeight() : Math.max(metrics.rowHeight(),
                Math.min(metrics.roomy() ? 34 : metrics.tight() ? 20 : 26,
                        (well.height() - 2) / slots));
        int fitRows = slots == 0 ? 0 : Math.min(slots, Math.max(0, (well.height() - 2)
                / rowHeight));
        UiRect chipRow = chips ? new UiRect(content.left(), well.bottom() + metrics.gap(),
                content.right(), well.bottom() + metrics.gap() + 11) : UiRect.EMPTY;
        int actionsTop = content.bottom() - actionsHeight;
        int targetY = actionsTop;
        int buttonsTop = actionsTop + (targetRow ? line : 0);
        UiRect buttons = hasButtons ? new UiRect(content.left(), buttonsTop, content.right(),
                buttonsTop + metrics.buttonHeight()) : UiRect.EMPTY;
        roster = new RosterLayout(content, well, rowHeight, slots, fitRows, chipRow, targetY,
                buttons, content.bottom() - 9, targetRow);
        if (row != null && row.configured() && members > 0) {
            rosterList = new TacticalList<>(well.left(), well.top(), well.width(),
                    well.height(), row.name(), id -> memberSpec(host.liveModel(), id));
            rosterList.rowHeight(rowHeight).keyedBy(id -> id)
                    .renderer((graphics, font, bounds, id, spec, state) -> drawMember(graphics,
                            bounds, host.liveModel(), id, state, rosterIndex(id) + 1))
                    .onSelect((index, id) -> selectMember(id))
                    .onActivate((index, id) -> selectMember(id));
            List<UUID> ids = new ArrayList<>();
            row.view().members().forEach(member -> ids.add(member.playerId()));
            rosterList.setItems(ids);
            rosterList.setSelectedIndex(ids.indexOf(host.selectedMember()));
            host.addBoardWidget(rosterList, ROSTER_UI_ID);
        }
        if (hasButtons) {
            addRosterButtons(actions, buttons);
        }
    }

    private int rosterIndex(UUID id) {
        return rosterList == null ? 0 : Math.max(0, rosterList.items().indexOf(id));
    }

    private void selectMember(UUID id) {
        host.toggleMember(id);
        if (rosterList != null) {
            rosterList.setSelectedIndex(host.selectedMember() == null ? -1
                    : rosterList.items().indexOf(host.selectedMember()));
        }
    }

    private void addRosterButtons(SquadBoardModel.RosterActions actions, UiRect row) {
        List<UiRect.Size> sizes = new ArrayList<>();
        List<SquadBoardModel.ActionState> buttons = actions.buttons();
        for (int index = 0; index < buttons.size(); index++) {
            if (actions.split() > 0 && index == actions.split()) {
                sizes.add(UiRect.Size.px(metrics.gap() * 2));
            }
            SquadBoardModel.Action action = buttons.get(index).action();
            sizes.add(action == SquadBoardModel.Action.CREATE_SQUAD
                    || action == SquadBoardModel.Action.JOIN_SQUAD
                    ? UiRect.Size.star(2.0D) : UiRect.Size.STAR);
        }
        List<UiRect> cells = row.cols(metrics.gap(), sizes.toArray(UiRect.Size[]::new));
        int cell = 0;
        for (int index = 0; index < buttons.size(); index++) {
            if (actions.split() > 0 && index == actions.split()) {
                cell++;
            }
            SquadBoardModel.ActionState state = buttons.get(index);
            SquadBoardModel.Action action = state.action();
            TacticalIcon icon = switch (action) {
                case TRANSFER_LEADER -> TacticalIcon.LEADER;
                case CREATE_SQUAD -> TacticalIcon.PLUS;
                case JOIN_SQUAD -> state.enabled() ? TacticalIcon.CHECK : null;
                case RETURN_TO_OWN_SQUAD -> TacticalIcon.BACK;
                default -> null;
            };
            host.addActionButton(cells.get(cell++), state,
                    live -> live.rosterActions().button(action), icon);
        }
    }

    private Component rosterTitle(SquadBoardModel.SquadRow row) {
        if (row == null) {
            return Component.empty();
        }
        return row.own() ? SquadBoardText.t(SquadBoardText.ROSTER_TITLE_OWN, row.name())
                : row.name();
    }

    private TacticalDraw.RowSpec memberSpec(SquadBoardModel model, UUID id) {
        MemberView member = member(model, id);
        if (member == null) {
            return TacticalDraw.RowSpec.of("");
        }
        return TacticalDraw.RowSpec.of(member.name()).withTooltip(SquadBoardText.t(
                SquadBoardText.MEMBER_TOOLTIP, member.name(),
                SquadLabels.className(model.snapshot(), member.classId()), stateText(member)));
    }

    private static MemberView member(SquadBoardModel model, UUID id) {
        BattleSnapshot snapshot = model.snapshot();
        if (snapshot == null || id == null) {
            return null;
        }
        for (SquadView squad : snapshot.squads()) {
            for (MemberView member : squad.members()) {
                if (member.playerId().equals(id)) {
                    return member;
                }
            }
        }
        return null;
    }

    private static Component stateText(MemberView member) {
        SquadRosterModel.StatusStyle style = SquadRosterModel.style(member.state());
        if (member.state() == MemberState.DEAD) {
            return SquadBoardText.t(SquadBoardText.MEMBER_DEAD_WAITING);
        }
        if (style.hasTag()) {
            return Component.translatable(style.tagKey());
        }
        return SquadBoardText.t(SquadBoardText.MEMBER_DEPLOYED);
    }

    /**
     * One roster row (preview {@code memberRow}): state dot, number, role tags, name, "你", class;
     * with room a second line with the health bar, or the state ("阵亡 · 等待重生", "离线").
     */
    private void drawMember(GuiGraphics graphics, UiRect bounds, SquadBoardModel model, UUID id,
                            TacticalDraw.RowState state, int number) {
        MemberView member = member(model, id);
        TacticalDraw.rowBg(graphics, bounds, state, 0);
        if (member == null) {
            return;
        }
        BattleSnapshot snapshot = model.snapshot();
        boolean selected = state.selected();
        boolean self = snapshot != null && member.playerId().equals(snapshot.viewerId());
        boolean two = bounds.height() + 1 >= 24;
        int y = two ? bounds.top() + Math.floorDiv(bounds.height() - 19, 2)
                : bounds.top() + Math.floorDiv(bounds.height() - 8, 2);
        SquadRosterModel.StatusStyle style = SquadRosterModel.style(member.state());
        int dotColor = selected ? TacticalBoardTheme.ON_SELECT : style.color();
        if (style.hollow()) {
            BattleUiTheme.outline(graphics, bounds.left() + 5, y + 2, bounds.left() + 9, y + 6,
                    dotColor);
        } else {
            graphics.fill(bounds.left() + 5, y + 2, bounds.left() + 9, y + 6, dotColor);
        }
        String numberText = String.valueOf(number);
        TextFit.draw(graphics, font, numberText, bounds.left() + 12, y, font.width(numberText),
                selected ? TacticalBoardTheme.ON_SELECT : TacticalBoardTheme.LIGHT_MUTED,
                TextFit.Align.LEFT);
        int x = bounds.left() + 12 + font.width(numberText) + 4;
        Component role = SquadLabels.roleShort(member.leader(), member.commander());
        if (role != null) {
            int roleWidth = font.width(role);
            TextFit.draw(graphics, font, role, x, y, roleWidth, selected
                    ? TacticalBoardTheme.ON_SELECT : TacticalBoardTheme.SECTION_B,
                    TextFit.Align.LEFT);
            x += roleWidth + 4;
        }
        Component className = SquadLabels.className(snapshot, member.classId());
        int classWidth = font.width(className);
        Component tag = !two && style.hasTag() ? Component.translatable(style.tagKey()) : null;
        int tagWidth = tag == null ? 0 : font.width(tag) + 6;
        Component you = SquadBoardText.t(SquadBoardText.ROSTER_YOU);
        int youWidth = self ? TacticalDraw.chipWidth(font, you) + 3 : 0;
        int nameRoom = bounds.right() - 6 - classWidth - 8 - tagWidth - youWidth - x;
        int nameColor = selected ? TacticalBoardTheme.ON_SELECT
                : SquadRosterModel.nameColor(member.state());
        int nameWidth = TextFit.draw(graphics, font, member.name(), x, y, Math.max(0, nameRoom),
                nameColor, TextFit.Align.LEFT).width();
        int end = x + nameWidth;
        if (self && nameRoom > 0) {
            end = TacticalDraw.chip(graphics, font, end + 3, y - 2, you, selected
                    ? TacticalBoardTheme.ON_SELECT : TacticalBoardTheme.NEUTRAL_B, true);
        }
        if (tag != null) {
            TextFit.draw(graphics, font, tag, end + 5, y, font.width(tag), selected
                    ? TacticalBoardTheme.ON_SELECT : style.color(), TextFit.Align.LEFT);
        }
        if (bounds.right() - 6 - classWidth > end + 4) {
            TextFit.draw(graphics, font, className, bounds.right() - 6 - classWidth, y, classWidth,
                    selected ? TacticalBoardTheme.ON_SELECT : TacticalBoardTheme.LIGHT_MUTED,
                    TextFit.Align.LEFT);
        }
        if (!two) {
            return;
        }
        int lineY = y + 11;
        int barLeft = bounds.left() + 21;
        if (style.hasTag() && member.state() != MemberState.DOWNED) {
            TextFit.draw(graphics, font, stateText(member), barLeft, lineY,
                    bounds.right() - 6 - barLeft, selected ? TacticalBoardTheme.ON_SELECT
                            : style.color(), TextFit.Align.LEFT);
            return;
        }
        if (!member.hasHealthRatio()) {
            TextFit.draw(graphics, font, stateText(member), barLeft, lineY,
                    bounds.right() - 6 - barLeft, selected ? TacticalBoardTheme.ON_SELECT
                            : member.state() == MemberState.DOWNED ? style.color()
                            : TacticalBoardTheme.LIGHT_MUTED, TextFit.Align.LEFT);
            return;
        }
        Component health = Component.translatable(SquadLabels.MEMBER_COUNT_KEY,
                Math.round(member.health()), Math.round(member.maxHealth()));
        int healthWidth = font.width(health);
        int barRight = bounds.right() - 6 - healthWidth - 6;
        float ratio = member.healthRatio();
        if (member.state() == MemberState.DOWNED) {
            Component downed = Component.translatable(style.tagKey());
            int downedWidth = font.width(downed);
            TextFit.draw(graphics, font, downed, barLeft, lineY, downedWidth,
                    selected ? TacticalBoardTheme.ON_SELECT : style.color(), TextFit.Align.LEFT);
            barLeft += downedWidth + 4;
        }
        if (barRight > barLeft + 4) {
            graphics.fill(barLeft, lineY + 3, barRight, lineY + 5, selected
                    ? SquadBoardBlocks.alpha(TacticalBoardTheme.ON_SELECT, 0x60)
                    : TacticalBoardTheme.HUD_TRACK);
            graphics.fill(barLeft, lineY + 3, barLeft + Math.round((barRight - barLeft) * ratio),
                    lineY + 5, selected ? TacticalBoardTheme.ON_SELECT
                            : com.wok.infantry.client.hud.TacticalHud.healthColor(ratio));
        }
        TextFit.draw(graphics, font, health, bounds.right() - 6 - healthWidth, lineY,
                healthWidth, selected ? TacticalBoardTheme.ON_SELECT
                        : TacticalBoardTheme.LIGHT_MUTED, TextFit.Align.LEFT);
    }

    // ---- commander --------------------------------------------------------------------------------

    private void addCommander(SquadBoardModel model) {
        UiRect content = TacticalDraw.panelContent(commanderPanel, metrics,
                TacticalDraw.PanelStyle.titled(SquadBoardText.t(SquadBoardText.COMMANDER_TITLE)));
        int kv = metrics.roomy() ? 13 : 11;
        boolean squadRow = !metrics.tight();
        int y = content.top() + kv + (squadRow ? kv : 0);
        UiRect keys = new UiRect(content.left(), y, content.right(), y + metrics.buttonHeight());
        commander = new CommanderLayout(content, content.top(), squadRow, keys,
                y + metrics.buttonHeight() + 3);
        SquadBoardModel.CommanderPanel panel = model.commanderPanel();
        List<SquadBoardModel.ActionState> buttons = panel.buttons();
        if (buttons.size() == 2) {
            List<UiRect> cells = keys.cols(metrics.gap(), UiRect.Size.STAR, UiRect.Size.STAR);
            for (int index = 0; index < 2; index++) {
                SquadBoardModel.Action action = buttons.get(index).action();
                host.addActionButton(cells.get(index), buttons.get(index),
                        live -> live.commanderPanel().button(action), null);
            }
        } else if (buttons.size() == 1) {
            SquadBoardModel.Action action = buttons.get(0).action();
            host.addActionButton(keys, buttons.get(0),
                    live -> live.commanderPanel().button(action),
                    action == SquadBoardModel.Action.CLAIM_COMMANDER ? TacticalIcon.COMMANDER
                            : null);
        }
    }

    // ---- drawing ----------------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, SquadBoardModel model, int mouseX, int mouseY) {
        if (wide) {
            SquadBoardBlocks.region(graphics, STRIP_BOX, strip);
            TacticalDraw.well(graphics, strip);
            SquadBoardBlocks.endRegion(graphics);
        } else {
            SquadBoardBlocks.region(graphics, LIST_BOX, listPanel);
            TacticalDraw.panel(graphics, font, listPanel, metrics,
                    TacticalDraw.PanelStyle.titled(listTitle())
                            .withMeta(listMeta(model, listPanel.width())));
            SquadBoardBlocks.endRegion(graphics);
        }
        if (vote) {
            FormationVotePanel.render(graphics, font, metrics, voteLayout, voteData);
        } else {
            renderRoster(graphics, model);
        }
        if (!statusPanel.isEmpty()) {
            renderStatus(graphics, model);
        }
        renderCommander(graphics, model);
        if (!factionPanel.isEmpty()) {
            renderFaction(graphics, model);
        }
    }

    private void renderRoster(GuiGraphics graphics, SquadBoardModel model) {
        SquadBoardModel.SquadRow row = model.viewedRow();
        SquadBoardBlocks.region(graphics, ROSTER_BOX, mainPanel);
        Component meta = row == null || !row.configured()
                ? SquadBoardModel.SquadStatus.NOT_CONFIGURED.label()
                : Component.translatable("screen.wok_infantry.squad.member_count", row.members(),
                row.capacity());
        TacticalDraw.panel(graphics, font, mainPanel, metrics,
                TacticalDraw.PanelStyle.titled(rosterTitle(row)).withMeta(meta));
        RosterLayout layout = roster;
        if (row == null || !row.configured() || row.members() == 0) {
            TacticalDraw.well(graphics, layout.well());
            renderEmptyRoster(graphics, model, row, layout.well().inset(2));
        }
        if (!layout.chips().isEmpty() && row != null && row.view() != null) {
            renderChips(graphics, model, row.view(), layout.chips());
        }
        SquadBoardModel.RosterActions actions = model.rosterActions();
        UiRect content = layout.content();
        if (layout.targetRow()) {
            Component label = SquadBoardText.t(SquadBoardText.ROSTER_TARGET);
            int x = content.left() + TextFit.draw(graphics, font, label, content.left(),
                    layout.targetY(), content.width(), TacticalBoardTheme.MUTED,
                    TextFit.Align.LEFT).width() + 5;
            MemberView target = actions.target();
            if (target == null) {
                TextFit.draw(graphics, font, SquadBoardText.t(SquadBoardText.ROSTER_TARGET_NONE),
                        x, layout.targetY(), content.right() - x, TacticalBoardTheme.FAINT,
                        TextFit.Align.LEFT);
            } else {
                SquadRosterModel.StatusStyle style = SquadRosterModel.style(target.state());
                Component tag = style.hasTag() ? Component.literal(" · ")
                        .append(Component.translatable(style.tagKey())) : Component.empty();
                int tagWidth = font.width(tag);
                int nameWidth = TextFit.draw(graphics, font, target.name(), x, layout.targetY(),
                        Math.max(0, content.right() - x - tagWidth), TacticalBoardTheme.TEXT,
                        TextFit.Align.LEFT).width();
                if (tagWidth > 0) {
                    TextFit.draw(graphics, font, tag, x + nameWidth, layout.targetY(), tagWidth,
                            boardTextColor(target.state()), TextFit.Align.LEFT);
                }
            }
        }
        if (actions.reason() != null) {
            reasonLine(graphics, actions.reason(), content.left(), layout.reasonY(),
                    content.width());
        }
        SquadBoardBlocks.endRegion(graphics);
    }

    /**
     * Status mark colour (dots, fills) on the light board, read from the palette active now. A
     * word in this colour goes through {@link #boardTextColor} instead.
     */
    static int boardColor(MemberState state) {
        return switch (state == null ? MemberState.OFFLINE : state) {
            case DEPLOYED -> TacticalBoardTheme.SUCCESS;
            case DOWNED -> TacticalBoardTheme.ACCENT;
            case DEAD -> TacticalBoardTheme.DANGER;
            case WAITING, OFFLINE -> TacticalBoardTheme.MUTED;
        };
    }

    /**
     * Status word colour on the light board ("倒地", "阵亡"): {@link #boardColor}, except that
     * the downed orange is written in {@link TacticalBoardTheme#ACCENT_TEXT} (plan 4.7: ACCENT as
     * text is only 1.7–2.8:1 on the light panels; ACCENT stays for marks and fills).
     */
    static int boardTextColor(MemberState state) {
        return state == MemberState.DOWNED ? TacticalBoardTheme.ACCENT_TEXT : boardColor(state);
    }

    /** Draws the longest form of {@code reason} that fits in one line. */
    void reasonLine(GuiGraphics graphics, SquadBoardModel.Reason reason, int x, int y,
                    int width) {
        SquadBoardBlocks.fitted(graphics, font, reason.candidates(), x, y, width,
                TacticalBoardTheme.MUTED, TextFit.Align.LEFT);
    }

    private void renderEmptyRoster(GuiGraphics graphics, SquadBoardModel model,
                                   SquadBoardModel.SquadRow row, UiRect area) {
        if (row == null) {
            return;
        }
        int hintWidth = area.width() - 12;
        if (!row.configured()) {
            List<Component> open = new ArrayList<>();
            for (SquadBoardModel.SquadRow other : model.squads()) {
                if (other.configured()) {
                    open.add(other.shortName());
                }
            }
            MutableComponent formation = SquadLabels.formationName(model.snapshot());
            List<Component> hints = new ArrayList<>();
            if (formation != null && !open.isEmpty()) {
                hints.add(SquadBoardText.t(SquadBoardText.ROSTER_CLOSED_HINT, formation,
                        SquadBoardText.joined(open)));
            }
            if (!open.isEmpty()) {
                hints.add(SquadBoardText.t(SquadBoardText.ROSTER_CLOSED_HINT_SHORT,
                        SquadBoardText.joined(open)));
            }
            hints.add(SquadBoardText.t(SquadBoardText.ROSTER_CLOSED_HINT_TINY));
            TacticalDraw.empty(graphics, font, area, TacticalIcon.LOCK,
                    FormationDetailPanel.pick(font, List.of(
                            SquadBoardText.t(SquadBoardText.ROSTER_CLOSED_TITLE, row.name()),
                            SquadBoardText.t(SquadBoardText.ROSTER_CLOSED_TITLE,
                                    row.shortName())), hintWidth),
                    Component.literal(TextFit.wrapBest(font, hints, hintWidth,
                            TacticalDraw.emptyHintCapacity(area)).text()), false);
            return;
        }
        List<Component> hints = model.authority().inSquad()
                ? SquadBoardText.list(SquadBoardText.t(SquadBoardText.ROSTER_EMPTY_OTHER),
                SquadBoardText.t(SquadBoardText.ROSTER_EMPTY_OTHER_SHORT),
                SquadBoardText.t(SquadBoardText.ROSTER_EMPTY_OTHER_TINY))
                : SquadBoardText.list(SquadBoardText.t(SquadBoardText.ROSTER_EMPTY_HINT),
                SquadBoardText.t(SquadBoardText.ROSTER_EMPTY_HINT_SHORT));
        TacticalDraw.empty(graphics, font, area, TacticalIcon.SQUAD,
                FormationDetailPanel.pick(font, List.of(
                        SquadBoardText.t(SquadBoardText.ROSTER_EMPTY_TITLE, row.name()),
                        SquadBoardText.t(SquadBoardText.ROSTER_EMPTY_TITLE, row.shortName())),
                        hintWidth),
                Component.literal(TextFit.wrapBest(font, hints, hintWidth,
                        TacticalDraw.emptyHintCapacity(area)).text()), false);
    }

    private void renderChips(GuiGraphics graphics, SquadBoardModel model, SquadView squad,
                             UiRect row) {
        SquadBoardBlocks.subRegion(graphics, "squad.roster.classes", row);
        Component label = SquadBoardText.t(SquadBoardText.ROSTER_CLASSES);
        int x = row.left() + TextFit.draw(graphics, font, label, row.left(), row.top() + 2,
                row.width(), TacticalBoardTheme.MUTED, TextFit.Align.LEFT).width() + 5;
        for (ClassLimitView limit : squad.classLimits()) {
            if (limit.closed()) {
                continue;
            }
            Component chip = SquadBoardText.t(SquadBoardText.ROSTER_CLASS_CHIP,
                    SquadLabels.className(model.snapshot(), limit.classId()), limit.used(),
                    limit.limit());
            if (x + TacticalDraw.chipWidth(font, chip) > row.right()) {
                break;
            }
            x = TacticalDraw.chip(graphics, font, x, row.top(), chip, limit.full()
                    ? TacticalBoardTheme.DANGER : TacticalBoardTheme.MUTED, false) + 3;
        }
        SquadBoardBlocks.endRegion(graphics);
    }

    @Override
    public void renderOverlay(GuiGraphics graphics, SquadBoardModel model) {
        if (roster == null || rosterList == null) {
            return;
        }
        // Open slots below the members: plain well, a hollow dot and "空位" (no hover, no stripe).
        UiRect inner = TacticalDraw.wellInner(roster.well());
        int members = rosterList.items().size();
        if (members >= roster.fitRows()) {
            return;
        }
        SquadBoardBlocks.subRegion(graphics, "squad.roster.slots", inner);
        Component empty = SquadBoardText.t(SquadBoardText.ROSTER_EMPTY_SLOT);
        for (int index = members; index < roster.fitRows(); index++) {
            int top = inner.top() + index * roster.rowHeight();
            int bottom = top + roster.rowHeight() - 1;
            if (bottom > inner.bottom()) {
                break;
            }
            int dotTop = top + (roster.rowHeight() - 1 - 4) / 2;
            BattleUiTheme.outline(graphics, inner.left() + 5, dotTop, inner.left() + 9, dotTop + 4,
                    TacticalBoardTheme.WELL_EDGE);
            TextFit.draw(graphics, font, empty, inner.left() + 21,
                    top + (roster.rowHeight() - 1 - 8) / 2, inner.width() - 27,
                    TacticalBoardTheme.FAINT, TextFit.Align.LEFT);
        }
        SquadBoardBlocks.endRegion(graphics);
    }

    private void renderStatus(GuiGraphics graphics, SquadBoardModel model) {
        SquadBoardBlocks.region(graphics, STATUS_BOX, statusPanel);
        UiRect content = TacticalDraw.panel(graphics, font, statusPanel, metrics,
                TacticalDraw.PanelStyle.titled(SquadBoardText.t(SquadBoardText.MINE_TITLE)));
        int kv = metrics.roomy() ? 13 : 11;
        List<FormationVotePanel.Info> rows = statusRows(model);
        for (int index = 0; index < rows.size(); index++) {
            FormationVotePanel.Info row = rows.get(index);
            if (content.top() + index * kv + 8 > content.bottom()) {
                break;
            }
            TacticalDraw.kv(graphics, font, content.left(), content.top() + index * kv,
                    content.width(), row.key(), row.value(), row.color());
        }
        SquadBoardBlocks.endRegion(graphics);
    }

    /**
     * "我的状态": faction, formation, squad, role, class, deployment. The respawn wait stays
     * neutral text (JAVA_PORT_PLAN 6.2: orange is for sections and adjustable controls).
     */
    static List<FormationVotePanel.Info> statusRows(SquadBoardModel model) {
        BattleSnapshot snapshot = model.snapshot();
        List<FormationVotePanel.Info> rows = new ArrayList<>();
        rows.add(new FormationVotePanel.Info(SquadBoardText.t(SquadBoardText.MINE_FACTION),
                SquadLabels.factionName(snapshot), SquadBoardBlocks.Ink.TEXT));
        if (model.votePending()) {
            boolean open = model.input().votePhase() == FormationVotePhase.OPEN;
            rows.add(new FormationVotePanel.Info(SquadBoardText.t(SquadBoardText.MINE_FORMATION),
                    SquadBoardText.t(open ? SquadBoardText.MINE_VOTE_OPEN
                            : SquadBoardText.MINE_VOTE_WAIT), SquadBoardBlocks.Ink.TEXT));
            rows.add(new FormationVotePanel.Info(SquadBoardText.t(SquadBoardText.MINE_SQUAD),
                    Component.translatable(SquadBoardText.PREFIX + "sub.pending"),
                    SquadBoardBlocks.Ink.MUTED));
            rows.add(new FormationVotePanel.Info(SquadBoardText.t(SquadBoardText.MINE_ROLE),
                    Component.translatable(SquadLabels.UNASSIGNED_KEY),
                    SquadBoardBlocks.Ink.MUTED));
            rows.add(new FormationVotePanel.Info(SquadBoardText.t(SquadBoardText.MINE_CLASS),
                    SquadBoardText.t(SquadBoardText.MINE_CLASS_AFTER_LOCK),
                    SquadBoardBlocks.Ink.MUTED));
            rows.add(new FormationVotePanel.Info(SquadBoardText.t(SquadBoardText.MINE_DEPLOY),
                    Component.translatable(SquadBoardText.PREFIX + "check.after_lock"),
                    SquadBoardBlocks.Ink.MUTED));
            return rows;
        }
        MutableComponent formation = SquadLabels.formationName(snapshot);
        rows.add(new FormationVotePanel.Info(SquadBoardText.t(SquadBoardText.MINE_FORMATION),
                formation == null ? Component.translatable(SquadBoardText.PREFIX + "count.none")
                        : SquadBoardText.t(SquadBoardText.MINE_FORMATION_VALUE, formation),
                SquadBoardBlocks.Ink.TEXT));
        SquadBoardModel.Authority authority = model.authority();
        rows.add(new FormationVotePanel.Info(SquadBoardText.t(SquadBoardText.MINE_SQUAD),
                authority.inSquad() ? SquadLabels.callsign(snapshot.ownSquad())
                        : Component.translatable(SquadLabels.UNASSIGNED_KEY),
                authority.inSquad() ? SquadBoardBlocks.Ink.TEXT : SquadBoardBlocks.Ink.MUTED));
        rows.add(new FormationVotePanel.Info(SquadBoardText.t(SquadBoardText.MINE_ROLE),
                authority.roleName(), authority.inSquad() || authority.commander()
                ? SquadBoardBlocks.Ink.TEXT : SquadBoardBlocks.Ink.MUTED));
        String classId = model.currentClassId();
        MutableComponent className = SquadLabels.className(snapshot,
                classId.isEmpty() ? null : classId);
        rows.add(new FormationVotePanel.Info(SquadBoardText.t(SquadBoardText.MINE_CLASS),
                authority.inSquad() ? className : Component.translatable(
                        SquadBoardText.PREFIX + "check.class_default", className),
                SquadBoardBlocks.Ink.TEXT));
        DeploymentPhase phase = snapshot.deployment().phase();
        rows.add(new FormationVotePanel.Info(SquadBoardText.t(SquadBoardText.MINE_DEPLOY),
                switch (phase) {
                    case ACTIVE -> SquadBoardText.t(SquadBoardText.MINE_ACTIVE);
                    case WAITING -> SquadBoardText.t(SquadBoardText.MINE_WAITING,
                            model.respawnSeconds());
                    case READY -> SquadBoardText.t(SquadBoardText.MINE_READY);
                }, phase == DeploymentPhase.ACTIVE ? SquadBoardBlocks.Ink.SUCCESS
                : SquadBoardBlocks.Ink.TEXT));
        return rows;
    }

    private void renderCommander(GuiGraphics graphics, SquadBoardModel model) {
        SquadBoardBlocks.region(graphics, COMMANDER_BOX, commanderPanel);
        TacticalDraw.panel(graphics, font, commanderPanel, metrics,
                TacticalDraw.PanelStyle.titled(SquadBoardText.t(SquadBoardText.COMMANDER_TITLE)));
        CommanderLayout layout = commander;
        UiRect content = layout.content();
        int kv = metrics.roomy() ? 13 : 11;
        SquadBoardModel.CommanderPanel panel = model.commanderPanel();
        MemberView current = panel.commander();
        TacticalDraw.kv(graphics, font, content.left(), layout.firstRow(), content.width(),
                SquadBoardText.t(SquadBoardText.COMMANDER_CURRENT), current == null
                        ? SquadBoardText.t(SquadBoardText.COMMANDER_VACANT)
                        : Component.literal(current.name()),
                current == null ? TacticalBoardTheme.MUTED : TacticalBoardTheme.TEXT);
        if (layout.squadRow()) {
            TacticalDraw.kv(graphics, font, content.left(), layout.firstRow() + kv,
                    content.width(), SquadBoardText.t(SquadBoardText.COMMANDER_SQUAD),
                    panel.commanderSquad() == null
                            ? Component.translatable(SquadBoardText.PREFIX + "count.none")
                            : SquadLabels.callsign(panel.commanderSquad()),
                    TacticalBoardTheme.MUTED);
        }
        int y = layout.reasonY();
        if (panel.reason() != null && y + 8 <= content.bottom()) {
            reasonLine(graphics, panel.reason(), content.left(), y, content.width());
        }
        y += 12;
        int lines = (content.bottom() - y) / SquadBoardBlocks.LINE;
        if (lines >= 1) {
            SquadBoardBlocks.paragraph(graphics, font,
                    SquadBoardText.all(SquadBoardText.COMMANDER_NOTE), content.left(), y,
                    content.width(), TacticalBoardTheme.FAINT, lines);
        }
        SquadBoardBlocks.endRegion(graphics);
    }

    private void renderFaction(GuiGraphics graphics, SquadBoardModel model) {
        BattleSnapshot snapshot = model.snapshot();
        Component own = SquadLabels.factionName(snapshot);
        Component enemy = SquadLabels.enemyFactionName(snapshot);
        int metaCap = (int) Math.floor((factionPanel.width() - 2) * 0.4D);
        List<Component> tags = new ArrayList<>();
        if (model.votePending()) {
            boolean open = model.input().votePhase() == FormationVotePhase.OPEN;
            tags.add(SquadBoardText.t(open ? SquadBoardText.VOTE_META_OPEN
                    : SquadBoardText.VOTE_META_WAIT));
            tags.add(SquadBoardText.t(open ? SquadBoardText.VOTE_META_OPEN_SHORT
                    : SquadBoardText.VOTE_META_WAIT_SHORT));
        } else {
            MutableComponent formation = SquadLabels.formationName(snapshot);
            if (formation != null) {
                tags.add(formation);
            }
        }
        Component meta = Component.empty();
        for (Component tag : tags) {
            Component candidate = SquadBoardText.t(SquadBoardText.FACTION_META, own, tag);
            if (font.width(candidate) <= metaCap) {
                meta = candidate;
                break;
            }
        }
        SquadBoardBlocks.region(graphics, FACTION_BOX, factionPanel);
        UiRect content = TacticalDraw.panel(graphics, font, factionPanel, metrics,
                TacticalDraw.PanelStyle.titled(SquadBoardText.t(SquadBoardText.FACTION_TITLE))
                        .withMeta(meta));
        int[] counts = {snapshot.factionMemberCount(), snapshot.enemyFactionMemberCount()};
        int[] capacities = {model.factionCapacity(), model.enemyFactionCapacity()};
        // Own side blue, enemy red: fixed for every livery, like the map and the HUD.
        int[] colors = {TacticalBoardTheme.HUD_FRIENDLY, TacticalBoardTheme.HUD_HOSTILE};
        for (int index = 0; index < 2; index++) {
            int y = content.top() + index * 16;
            Component value = Component.translatable(SquadLabels.MEMBER_COUNT_KEY, counts[index],
                    capacities[index]);
            int valueWidth = font.width(value);
            int room = content.width() - valueWidth - 6;
            Component label;
            if (index > 0) {
                label = enemy;
            } else if (!meta.getString().isEmpty() || tags.isEmpty()) {
                label = FormationDetailPanel.pick(font, List.of(
                        SquadBoardText.t(SquadBoardText.FACTION_OWN, own), own), room);
            } else {
                label = FormationDetailPanel.pick(font, List.of(
                        SquadBoardText.t(SquadBoardText.FACTION_OWN_WITH, own,
                                tags.get(tags.size() - 1)),
                        SquadBoardText.t(SquadBoardText.FACTION_OWN, own), own), room);
            }
            TextFit.draw(graphics, font, label, content.left(), y, Math.max(0, room),
                    TacticalBoardTheme.TEXT, TextFit.Align.LEFT);
            TextFit.draw(graphics, font, value, content.right() - valueWidth, y, valueWidth,
                    TacticalBoardTheme.MUTED, TextFit.Align.LEFT);
            float ratio = capacities[index] <= 0 ? 0.0F
                    : Math.min(1.0F, counts[index] / (float) capacities[index]);
            TacticalDraw.meter(graphics, new UiRect(content.left(), y + 10, content.right(),
                    y + 13), ratio, colors[index], TacticalBoardTheme.WELL, 0);
        }
        SquadBoardBlocks.endRegion(graphics);
    }
}
