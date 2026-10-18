package com.wok.infantry.client.screen;

import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.battle.ClassLimitView;
import com.wok.infantry.battle.ClassQuotaView;
import com.wok.infantry.battle.MemberState;
import com.wok.infantry.battle.MemberView;
import com.wok.infantry.battle.SquadView;
import com.wok.infantry.client.ClientFormationState;
import com.wok.infantry.client.ClientLoadoutState;
import com.wok.infantry.client.hud.SquadRosterModel;
import com.wok.infantry.client.hud.TacticalHud;
import com.wok.infantry.deployment.DeploymentPhase;
import com.wok.infantry.loadout.LoadoutClassDefinition;
import com.wok.infantry.loadout.LoadoutEntry;
import com.wok.infantry.loadout.LoadoutInventoryTarget;
import com.wok.infantry.loadout.LoadoutSlotDefinition;
import com.wok.infantry.loadout.LoadoutSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Class page of the battle terminal (preview {@code 20-squad.js} {@code newClassesPage}).
 *
 * <p>Wide (content ≥ {@value #WIDE_MIN_WIDTH}): the class quota list on the left, the current
 * class card on the right with "配置当前兵种装备" and, when the loadout cache already holds this
 * class, "部署时发放" (the class's slots and items). Narrow (on the D2 device also 960×720 at
 * GUI 1): the list above a compact card. Each class row shows used /
 * quota, who holds it in the own squad, its status (点击更换 / 名额已满 / 作战中不可换 …) and, when
 * tall enough, why; on wide rows each slot is a box with its holder and health. Clicking an
 * available row changes the class at once (the server confirms). Outside a squad the rows say
 * "每队 n" and a table lists which squad still has which class free.
 */
final class ClassPagePainter implements SquadScreen.Painter {
    static final String LIST_UI_ID = "squad.classes";
    static final String LOADOUT_UI_ID = "squad.configure_loadout";
    static final String LIST_BOX = "squad.classes_panel";
    static final String CURRENT_BOX = "squad.current_panel";

    private final SquadScreen host;
    private final Font font;
    private final TacticalShellLayout.Metrics metrics;
    private final boolean vote;
    private final boolean wide;
    private UiRect listPanel = UiRect.EMPTY;
    private UiRect currentPanel = UiRect.EMPTY;
    // list
    private UiRect listContent = UiRect.EMPTY;
    private UiRect listWell = UiRect.EMPTY;
    private int rowHeight;
    private int boxColumns;
    private SquadBoardModel.Page page = SquadBoardModel.Page.of(0, 1, 0);
    private UiRect pager = UiRect.EMPTY;
    private UiRect slotsBlock = UiRect.EMPTY;
    private UiRect listRules = UiRect.EMPTY;
    private SquadBoardBlocks.RulesFit listRulesFit;
    private int noteTop = -1;
    private TacticalList<String> list;
    // current card
    private UiRect currentContent = UiRect.EMPTY;
    private boolean compact;
    private int kitTop = -1;
    private KitMode kitMode = KitMode.NONE;
    private int kitPitch;
    private List<KitSlot> kit;
    private UiRect cardRules = UiRect.EMPTY;
    private SquadBoardBlocks.RulesFit cardRulesFit;
    private int cardNoteTop = -1;
    private int buttonBottom;
    // vote
    private FormationVotePanel.Layout voteLayout;
    private FormationVotePanel.Data voteData;
    private UiRect voteInfo = UiRect.EMPTY;
    private UiRect voteFlow = UiRect.EMPTY;
    private SquadBoardBlocks.FlowFit voteFlowFit;
    private UiRect voteLock = UiRect.EMPTY;
    private SquadBoardBlocks.RulesFit voteLockFit;
    private int voteTextTop;

    private enum KitMode {
        NONE, ROWS, STRIP
    }

    /** One slot of the class's saved loadout. */
    record KitSlot(Component slot, Component target, ItemStack stack, Component item,
                   int count, boolean configured) {
    }

    ClassPagePainter(SquadScreen host, UiRect body, SquadBoardModel model) {
        this.host = host;
        this.font = host.boardFont();
        this.metrics = host.boardMetrics();
        this.vote = model.votePending();
        this.wide = wide(body);
        if (vote) {
            layoutVote(body, model);
            return;
        }
        if (wide) {
            List<UiRect> cols = body.cols(metrics.gap(), UiRect.Size.STAR,
                    UiRect.Size.px(metrics.roomy() ? 280 : 176));
            listPanel = cols.get(0);
            currentPanel = cols.get(1);
            boolean rulesRight = layoutCurrent(model, false,
                    model.authority().inSquad() && model.classRows().size() > 2);
            layoutList(model, !rulesRight);
        } else {
            List<UiRect> rows = body.rows(metrics.gap(), UiRect.Size.STAR,
                    UiRect.Size.px(SquadBoardBlocks.panelHeight(metrics,
                            11 + metrics.buttonHeight(), true)));
            listPanel = rows.get(0);
            currentPanel = rows.get(1);
            layoutCurrent(model, true, false);
            layoutList(model, true);
        }
    }

    /**
     * Narrowest content of the wide layout (preview {@code 20-squad.js}). On the D2 device 960×720
     * at GUI 1 (480×360 logical) gives 416 and is narrow; 480×270 gives 456 and stays wide.
     */
    static final int WIDE_MIN_WIDTH = 440;

    /** Whether {@code body} (the shell's content) gets the list and card side by side. */
    static boolean wide(UiRect body) {
        return body.width() >= WIDE_MIN_WIDTH;
    }

    // ---- class list -------------------------------------------------------------------------------

    private Component listMeta(SquadBoardModel model) {
        BattleSnapshot snapshot = model.snapshot();
        if (!model.authority().inSquad() || snapshot == null) {
            return Component.translatable(SquadLabels.UNASSIGNED_KEY);
        }
        SquadView own = snapshot.squad(snapshot.ownSquad());
        return SquadBoardText.t(SquadBoardText.CLASSES_META,
                SquadLabels.callsign(snapshot.ownSquad()), own == null ? 0 : own.members().size());
    }

    private static List<SquadBoardBlocks.Rule> classRules(SquadBoardModel model) {
        BattleSnapshot snapshot = model.snapshot();
        MutableComponent formation = SquadLabels.formationName(snapshot);
        String defaultClass = snapshot == null ? "" : snapshot.formationContext().defaultClassId();
        List<SquadBoardBlocks.Rule> rules = new ArrayList<>();
        rules.add(SquadBoardBlocks.Rule.of(SquadBoardText.list(
                formation == null ? null : SquadBoardText.t(SquadBoardText.CLASS_RULE_QUOTA,
                        formation),
                SquadBoardText.t(SquadBoardText.CLASS_RULE_QUOTA_SHORT),
                SquadBoardText.t(SquadBoardText.CLASS_RULE_QUOTA_TINY))));
        rules.add(SquadBoardBlocks.Rule.of(SquadBoardText.all(SquadBoardText.CLASS_RULE_CHANGE)));
        Component defaultName = defaultClass.isEmpty() ? null
                : SquadLabels.className(snapshot, defaultClass);
        rules.add(SquadBoardBlocks.Rule.of(SquadBoardText.list(
                defaultName == null ? null : SquadBoardText.t(SquadBoardText.CLASS_RULE_RESET,
                        defaultName),
                defaultName == null ? null : SquadBoardText.t(SquadBoardText.CLASS_RULE_RESET_SHORT,
                        defaultName),
                SquadBoardText.t(SquadBoardText.CLASS_RULE_RESET_TINY))));
        rules.add(SquadBoardBlocks.Rule.of(SquadBoardText.all(SquadBoardText.CLASS_RULE_LOADOUT)));
        return rules;
    }

    private void layoutList(SquadBoardModel model, boolean rulesHere) {
        List<SquadBoardModel.ClassRow> rows = model.classRows();
        listContent = TacticalDraw.panelContent(listPanel, metrics,
                TacticalDraw.PanelStyle.titled(SquadBoardText.t(SquadBoardText.CLASSES_TITLE)));
        UiRect c = listContent;
        int count = Math.max(1, rows.size());
        boolean inSquad = model.authority().inSquad();
        boolean grow = inSquad && rows.size() > 2;
        int maxRow = grow ? (metrics.roomy() ? 120 : 80) : metrics.roomy() ? 60 : 36;
        int minRules = metrics.roomy() ? 44 : 30;
        int slotsHeight = !inSquad && !metrics.tight() && slotsFit(model, c.width())
                ? slotsHeight(model) + metrics.gap() : 0;
        SquadBoardBlocks.RulesFit rules = rulesHere ? SquadBoardBlocks.fitRules(font, metrics,
                c.width(), SquadBoardText.t(SquadBoardText.CLASSES_RULES_TITLE),
                classRules(model), c.height() - 2 - metrics.gap() - count * minRules
                        - slotsHeight, true) : null;
        boolean withSlots = slotsHeight > 0 && count * minRules + 2 + slotsHeight
                + (rules != null ? rules.height() + metrics.gap() : 0) <= c.height();
        int available = c.height() - 2 - (rules != null ? rules.height() + metrics.gap() : 0)
                - (withSlots ? slotsHeight : 0);
        rowHeight = Math.max(metrics.rowHeight(), Math.min(maxRow, available / count));
        int perPage = Math.max(1, (c.height() - 2) / rowHeight);
        int pages = SquadBoardModel.Page.pageCount(rows.size(), perPage);
        if (pages > 1) {
            perPage = Math.max(1, (c.height() - 2 - metrics.buttonHeight() - metrics.gap())
                    / rowHeight);
        }
        page = SquadBoardModel.Page.of(rows.size(), perPage, host.classPage());
        boxColumns = 0;
        for (SquadBoardModel.ClassRow row : rows) {
            boxColumns = Math.max(boxColumns, Math.max(row.quota().limit(), row.holders().size()));
        }
        listWell = new UiRect(c.left(), c.top(), c.right(),
                c.top() + Math.max(1, page.size()) * rowHeight + 2);
        int y = listWell.bottom() + metrics.gap();
        if (!page.multiplePages() && withSlots) {
            slotsBlock = new UiRect(c.left(), y, c.right(), y + slotsHeight - metrics.gap());
            y += slotsHeight;
        }
        if (page.multiplePages()) {
            pager = new UiRect(c.left(), c.bottom() - metrics.buttonHeight(), c.right(),
                    c.bottom());
        } else if (rules != null) {
            listRules = new UiRect(c.left(), y, c.right(), y + rules.height());
            listRulesFit = rules;
        } else if (y + 10 <= c.bottom()) {
            noteTop = y + 1;
        }
        if (rows.isEmpty()) {
            return;
        }
        list = new TacticalList<>(listWell.left(), listWell.top(), listWell.width(),
                listWell.height(), SquadBoardText.t(SquadBoardText.CLASSES_TITLE),
                classId -> classSpec(host.liveModel(), classId));
        list.rowHeight(rowHeight).keyedBy(id -> id)
                .renderer((graphics, unused, bounds, classId, spec, state) -> drawClassRow(
                        graphics, bounds, host.liveModel(), classId, state))
                .onSelect((index, classId) -> {
                    if (!keyboard()) {
                        choose(classId);
                    }
                })
                .onActivate((index, classId) -> choose(classId));
        List<String> ids = new ArrayList<>();
        page.slice(rows).forEach(row -> ids.add(row.classId()));
        list.setItems(ids);
        list.setSelectedIndex(ids.indexOf(model.currentClassId()));
        host.addBoardWidget(list, LIST_UI_ID);
    }

    private static boolean keyboard() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && minecraft.getLastInputType().isKeyboard();
    }

    private void choose(String classId) {
        SquadBoardModel live = host.liveModel();
        SquadBoardModel.ClassRow row = classRow(live, classId);
        if (row != null && row.select().enabled()) {
            host.perform(row.select());
        }
        if (list != null) {
            list.setSelectedIndex(list.items().indexOf(live.currentClassId()));
        }
    }

    private static SquadBoardModel.ClassRow classRow(SquadBoardModel model, String classId) {
        for (SquadBoardModel.ClassRow row : model.classRows()) {
            if (row.classId().equals(classId)) {
                return row;
            }
        }
        return null;
    }

    private TacticalDraw.RowSpec classSpec(SquadBoardModel model, String classId) {
        SquadBoardModel.ClassRow row = classRow(model, classId);
        if (row == null) {
            return TacticalDraw.RowSpec.of(classId);
        }
        Component name = SquadLabels.className(model.snapshot(), classId);
        TacticalDraw.RowSpec spec = TacticalDraw.RowSpec.of(name).withTooltip(
                Component.empty().append(name).append("\n").append(hint(model, row)));
        SquadBoardModel.ActionState select = row.select();
        return select.enabled() ? spec : spec.withDisabledReason(select.reason().full());
    }

    /**
     * Semantic colour of a class row's status word (preview {@code classRows}: its
     * {@code row.color === T.FAINT} test becomes {@code tone() == OFF}). Kept as a tone and
     * resolved by {@link #color()} while drawing, so the "steps back" test never compares colours
     * read in two different palettes.
     */
    enum StatusTone {
        /** Can be picked ("点击更换"): bright green on the dark row. */
        PICK,
        /** The viewer's current class ("当前兵种"): the selection's text colour. */
        CURRENT,
        /** The squad's quota is used up ("名额已满"): bright red. */
        FULL,
        /** Not open, no squad, in combat or any other reason: faint, the row steps back. */
        OFF;

        /** This tone in the palette active now; call while drawing. */
        int color() {
            return switch (this) {
                case PICK -> TacticalBoardTheme.SUCCESS_B;
                case CURRENT -> TacticalBoardTheme.ON_SELECT;
                case FULL -> TacticalBoardTheme.DANGER_B;
                case OFF -> TacticalBoardTheme.FAINT;
            };
        }

        /** Pure: the tone of a row that can be picked, else of one blocked by {@code code}. */
        static StatusTone of(boolean enabled, SquadBoardModel.ReasonCode code) {
            if (enabled) {
                return PICK;
            }
            if (code == null) {
                return OFF;
            }
            return switch (code) {
                case CLASS_CURRENT -> CURRENT;
                case CLASS_FULL -> FULL;
                default -> OFF;
            };
        }
    }

    /** Status word and tone of a class row (preview {@code classRows}). */
    private record RowStatus(Component word, StatusTone tone, boolean enabled) {
    }

    private static RowStatus status(SquadBoardModel model, SquadBoardModel.ClassRow row) {
        SquadBoardModel.ActionState select = row.select();
        if (select.enabled()) {
            return new RowStatus(SquadBoardText.t(SquadBoardText.CLASSES_STATUS_PICK),
                    StatusTone.PICK, true);
        }
        SquadBoardModel.ReasonCode code = select.reason().code();
        Component word = switch (code) {
            case CLASS_NO_SQUAD -> SquadBoardText.t(SquadBoardText.CLASSES_STATUS_NO_SQUAD);
            case CLASS_ACTIVE -> SquadBoardText.t(SquadBoardText.CLASSES_STATUS_ACTIVE);
            default -> select.reason().shortForm();
        };
        return new RowStatus(word, StatusTone.of(false, code), false);
    }

    /** Third line: why, or what a click does. */
    private static Component hint(SquadBoardModel model, SquadBoardModel.ClassRow row) {
        SquadBoardModel.ActionState select = row.select();
        if (select.enabled()) {
            return select.hint() == null ? Component.empty() : select.hint().full();
        }
        return switch (select.reason().code()) {
            case CLASS_NOT_OPEN -> SquadBoardText.t(SquadBoardText.CLASSES_HINT_CLOSED);
            case CLASS_CURRENT -> SquadBoardText.t(model.authority().inSquad()
                    ? SquadBoardText.CLASSES_HINT_CURRENT
                    : SquadBoardText.CLASSES_HINT_CURRENT_DEFAULT);
            case CLASS_NO_SQUAD -> SquadBoardText.t(SquadBoardText.CLASSES_HINT_NO_SQUAD);
            case CLASS_ACTIVE -> SquadBoardText.t(SquadBoardText.CLASSES_HINT_ACTIVE);
            case CLASS_FULL -> SquadBoardText.t(SquadBoardText.CLASSES_HINT_FULL);
            default -> select.reason().full();
        };
    }

    /**
     * One class row (preview {@code classRow}): one, two or three lines, or name / quota boxes /
     * hint on tall wide rows. A row that cannot be clicked steps back (muted text, quiet boxes).
     */
    private void drawClassRow(GuiGraphics graphics, UiRect bounds, SquadBoardModel model,
                              String classId, TacticalDraw.RowState state) {
        SquadBoardModel.ClassRow row = classRow(model, classId);
        if (row == null) {
            return;
        }
        RowStatus status = status(model, row);
        boolean selected = state.selected();
        boolean off = !selected && !status.enabled();
        TacticalDraw.rowBg(graphics, bounds, state.withDisabled(off), 0);
        boolean dim = off && status.tone() == StatusTone.OFF;
        boolean noSquad = !model.authority().inSquad();
        int x = bounds.left() + 6;
        int right = bounds.right() - 5;
        int pitch = bounds.height() + 1;
        int boxHeight = pitch >= 80 ? 32 : pitch >= 58 ? 20 : 13;
        int spacing = pitch >= 80 ? 6 : 4;
        boolean boxes = !noSquad && pitch >= 44 && boxWidth(x, right, row) >= 44;
        int lines = pitch >= 34 ? 3 : pitch >= 21 ? 2 : 1;
        int contentHeight = boxes ? 8 + spacing + boxHeight + spacing + 8 : lines * 11 - 3;
        int y = bounds.top() + Math.floorDiv(bounds.height() - contentHeight, 2);
        ClassQuotaView quota = row.quota();
        Component count = noSquad ? SquadBoardText.t(SquadBoardText.CLASSES_PER_SQUAD,
                quota.limit()) : Component.translatable(SquadLabels.MEMBER_COUNT_KEY,
                quota.used(), quota.limit());
        int countWidth = font.width(count);
        Component name = SquadLabels.className(model.snapshot(), classId);
        int nameColor = selected ? TacticalBoardTheme.ON_SELECT : dim ? TacticalBoardTheme.FAINT
                : TacticalBoardTheme.LIGHT;
        int countColor = selected ? TacticalBoardTheme.ON_SELECT
                : !noSquad && quota.limit() > 0 && quota.used() >= quota.limit()
                ? TacticalBoardTheme.DANGER_B : TacticalBoardTheme.LIGHT_MUTED;
        int statusColor = selected ? TacticalBoardTheme.ON_SELECT : status.tone().color();
        int subColor = selected ? TacticalBoardTheme.SELECT_SUB : dim ? TacticalBoardTheme.FAINT
                : TacticalBoardTheme.LIGHT_MUTED;
        int statusWidth = font.width(status.word());
        if (lines == 1) {
            TextFit.draw(graphics, font, name, x, y,
                    Math.max(0, right - x - statusWidth - countWidth - 16), nameColor,
                    TextFit.Align.LEFT);
            TextFit.draw(graphics, font, count, right - statusWidth - 8 - countWidth, y,
                    countWidth, countColor, TextFit.Align.LEFT);
            TextFit.draw(graphics, font, status.word(), right - statusWidth, y, statusWidth,
                    statusColor, TextFit.Align.LEFT);
            return;
        }
        if (boxes) {
            int nameWidth = TextFit.draw(graphics, font, name, x, y,
                    Math.max(0, right - x - statusWidth - countWidth - 24), nameColor,
                    TextFit.Align.LEFT).width();
            TextFit.draw(graphics, font, count, x + nameWidth + 8, y, countWidth, countColor,
                    TextFit.Align.LEFT);
            TextFit.draw(graphics, font, status.word(), right - statusWidth, y, statusWidth,
                    statusColor, TextFit.Align.LEFT);
            y += 8 + spacing;
            drawQuotaBoxes(graphics, model, row, x, y, right, boxHeight, selected,
                    !selected && !status.enabled());
            y += boxHeight + spacing;
            TextFit.draw(graphics, font, hint(model, row), x, y, right - x, subColor,
                    TextFit.Align.LEFT);
            return;
        }
        int pipsWidth = noSquad ? 0 : Math.min(quota.limit(), 12) * 6;
        TextFit.draw(graphics, font, name, x, y, Math.max(0, right - x - countWidth - pipsWidth
                - 14), nameColor, TextFit.Align.LEFT);
        if (pipsWidth > 0) {
            SquadBoardBlocks.pips(graphics, right - countWidth - 6 - pipsWidth, y + 2,
                    Math.min(quota.used(), pipsWidth / 6), pipsWidth / 6, selected
                            ? TacticalBoardTheme.ON_SELECT : quota.used() >= quota.limit()
                            ? TacticalBoardTheme.DANGER_B : TacticalBoardTheme.NEUTRAL_B,
                    selected ? SquadBoardBlocks.alpha(TacticalBoardTheme.ON_SELECT, 0x80)
                            : TacticalBoardTheme.CELL_EDGE);
        }
        TextFit.draw(graphics, font, count, right - countWidth, y, countWidth, countColor,
                TextFit.Align.LEFT);
        y += 11;
        TextFit.draw(graphics, font, status.word(), right - statusWidth, y, statusWidth,
                statusColor, TextFit.Align.LEFT);
        Component who;
        int whoColor;
        if (noSquad) {
            who = SquadBoardText.t(SquadBoardText.CLASSES_WHO_NO_SQUAD);
            whoColor = selected ? TacticalBoardTheme.SELECT_SUB : TacticalBoardTheme.LIGHT_MUTED;
        } else if (row.holders().isEmpty()) {
            who = SquadBoardText.t(SquadBoardText.CLASSES_WHO_NONE);
            whoColor = selected ? TacticalBoardTheme.SELECT_SUB : TacticalBoardTheme.FAINT;
        } else {
            List<Component> names = new ArrayList<>();
            row.holders().forEach(member -> names.add(Component.literal(member.name())));
            who = SquadBoardText.joined(names);
            whoColor = selected ? TacticalBoardTheme.SELECT_SUB : TacticalBoardTheme.LIGHT_MUTED;
        }
        TextFit.draw(graphics, font, who, x, y, Math.max(0, right - x - statusWidth - 8),
                whoColor, TextFit.Align.LEFT);
        if (lines == 3) {
            TextFit.draw(graphics, font, hint(model, row), x, y + 11, right - x, subColor,
                    TextFit.Align.LEFT);
        }
    }

    private int boxWidth(int x, int right, SquadBoardModel.ClassRow row) {
        int columns = Math.max(boxColumns, Math.max(row.quota().limit(), row.holders().size()));
        if (columns <= 0) {
            return 0;
        }
        return Math.min(150, (right - x - (columns - 1) * 3) / columns);
    }

    /**
     * One box per quota slot with its holder (preview {@code quotaBoxes}); boxes of 30px and more
     * add a second line (health, "阵亡 · 等待重生", "离线"), every box a health strip.
     */
    private void drawQuotaBoxes(GuiGraphics graphics, SquadBoardModel model,
                                SquadBoardModel.ClassRow row, int x, int y, int right, int height,
                                boolean selected, boolean off) {
        int slots = Math.max(row.quota().limit(), row.holders().size());
        int width = boxWidth(x, right, row);
        boolean two = height >= 30;
        BattleSnapshot snapshot = model.snapshot();
        for (int index = 0; index < slots; index++) {
            int left = x + index * (width + 3);
            MemberView member = index < row.holders().size() ? row.holders().get(index) : null;
            int edge = selected ? (member != null ? TacticalBoardTheme.ON_SELECT
                    : SquadBoardBlocks.alpha(TacticalBoardTheme.ON_SELECT, 0x80))
                    : off || member == null ? TacticalBoardTheme.WELL_EDGE
                    : row.quota().used() >= row.quota().limit() ? TacticalBoardTheme.DANGER_B
                    : TacticalBoardTheme.NEUTRAL_B;
            BattleUiTheme.outline(graphics, left, y, left + width, y + height, edge);
            int textY = y + (two ? 4 : height >= 18 ? 3 : (height - 8) / 2);
            if (member == null) {
                TextFit.draw(graphics, font, SquadBoardText.t(SquadBoardText.ROSTER_EMPTY_SLOT),
                        left + 4, textY, width - 8, selected ? TacticalBoardTheme.SELECT_SUB
                                : TacticalBoardTheme.FAINT, TextFit.Align.LEFT);
                continue;
            }
            SquadRosterModel.StatusStyle style = SquadRosterModel.style(member.state());
            boolean self = snapshot != null && member.playerId().equals(snapshot.viewerId());
            Component you = SquadBoardText.t(SquadBoardText.ROSTER_YOU);
            int youWidth = self ? font.width(you) + 4 : 0;
            int nameColor = selected ? TacticalBoardTheme.ON_SELECT
                    : member.state() == MemberState.OFFLINE ? TacticalBoardTheme.OFFLINE
                    : off ? TacticalBoardTheme.LIGHT_MUTED : TacticalBoardTheme.LIGHT;
            int nameWidth = TextFit.draw(graphics, font, member.name(), left + 4, textY,
                    Math.max(0, width - 8 - youWidth), nameColor, TextFit.Align.LEFT).width();
            if (self && width - 8 - youWidth > 0) {
                TextFit.draw(graphics, font, you, left + 4 + nameWidth + 4, textY,
                        font.width(you), selected ? TacticalBoardTheme.SELECT_SUB : off
                                ? TacticalBoardTheme.LIGHT_MUTED : TacticalBoardTheme.NEUTRAL_B,
                        TextFit.Align.LEFT);
            }
            if (height < 18) {
                continue;
            }
            float ratio = member.state().hasVitals() && member.hasHealthRatio()
                    ? member.healthRatio() : 0.0F;
            int tagColor = selected ? TacticalBoardTheme.ON_SELECT : off
                    ? TacticalBoardTheme.LIGHT_MUTED : style.color();
            if (two) {
                Component line;
                int lineColor;
                if (style.hasTag()) {
                    line = member.state() == MemberState.DEAD
                            ? SquadBoardText.t(SquadBoardText.MEMBER_DEAD_WAITING)
                            : Component.translatable(style.tagKey());
                    lineColor = tagColor;
                } else if (member.hasHealthRatio()) {
                    line = Component.translatable(SquadLabels.MEMBER_COUNT_KEY,
                            Math.round(member.health()), Math.round(member.maxHealth()));
                    lineColor = selected ? TacticalBoardTheme.SELECT_SUB
                            : TacticalBoardTheme.LIGHT_MUTED;
                } else {
                    line = SquadBoardText.t(SquadBoardText.MEMBER_DEPLOYED);
                    lineColor = selected ? TacticalBoardTheme.SELECT_SUB
                            : TacticalBoardTheme.LIGHT_MUTED;
                }
                TextFit.draw(graphics, font, line, left + 4, textY + 11, width - 8, lineColor,
                        TextFit.Align.LEFT);
            } else if (style.hasTag()) {
                Component tag = Component.translatable(style.tagKey());
                int tagWidth = font.width(tag);
                if (left + width - 4 - tagWidth > left + 4 + nameWidth + youWidth + 2) {
                    TextFit.draw(graphics, font, tag, left + width - 4 - tagWidth, textY,
                            tagWidth, tagColor, TextFit.Align.LEFT);
                }
            }
            int stripY = y + height - (two ? 6 : 5);
            graphics.fill(left + 4, stripY, left + width - 4, stripY + 2, selected
                    ? SquadBoardBlocks.alpha(TacticalBoardTheme.ON_SELECT, 0x60)
                    : TacticalBoardTheme.HUD_TRACK);
            if (ratio > 0.0F) {
                int barColor = selected ? TacticalBoardTheme.ON_SELECT : off
                        ? SquadBoardBlocks.alpha(TacticalHud.healthColor(ratio), 0x70)
                        : TacticalHud.healthColor(ratio);
                graphics.fill(left + 4, stripY, left + 4 + Math.round((width - 8) * ratio),
                        stripY + 2, barColor);
            }
        }
    }

    // ---- free slots (not in a squad) --------------------------------------------------------------

    private int slotsHeight(SquadBoardModel model) {
        int configured = 0;
        for (SquadBoardModel.SquadRow row : model.squads()) {
            if (row.configured()) {
                configured++;
            }
        }
        return metrics.sectionHeight() + 3 + 11 + configured * (metrics.roomy() ? 13 : 11);
    }

    /** Whether every class name of the table header fits its column. */
    private boolean slotsFit(SquadBoardModel model, int width) {
        List<ClassQuotaView> classes = model.snapshot() == null ? List.of()
                : model.snapshot().classQuotas();
        if (classes.isEmpty()) {
            return false;
        }
        int nameWidth = slotsNameWidth(model, width);
        int cells = classes.size() + 1;
        int cellWidth = (width - nameWidth - 4 * (cells - 1)) / cells;
        for (ClassQuotaView quota : classes) {
            if (font.width(SquadLabels.className(model.snapshot(), quota.classId())) > cellWidth) {
                return false;
            }
        }
        return font.width(SquadBoardText.t(SquadBoardText.SLOTS_PEOPLE)) <= cellWidth;
    }

    private int slotsNameWidth(SquadBoardModel model, int width) {
        int widest = font.width(SquadBoardText.t(SquadBoardText.MINE_SQUAD));
        for (SquadBoardModel.SquadRow row : model.squads()) {
            if (row.configured()) {
                widest = Math.max(widest, font.width(row.name()));
            }
        }
        return Math.min((int) Math.floor(width * 0.3D), widest + 10);
    }

    private void renderSlots(GuiGraphics graphics, SquadBoardModel model, UiRect bounds) {
        BattleSnapshot snapshot = model.snapshot();
        List<ClassQuotaView> classes = snapshot.classQuotas();
        SquadBoardBlocks.subRegion(graphics, "squad.classes.slots", bounds);
        Component meta = FormationDetailPanel.pick(font, SquadBoardText.all(
                SquadBoardText.SLOTS_META), bounds.width() * 2 / 5);
        if (font.width(meta) > bounds.width() * 2 / 5) {
            meta = Component.empty();
        }
        TacticalDraw.section(graphics, font, bounds.topSlice(metrics.sectionHeight()),
                SquadBoardText.t(SquadBoardText.SLOTS_TITLE), meta,
                TacticalBoardTheme.LIGHT_MUTED, TacticalBoardTheme.SECTION);
        int y = bounds.top() + metrics.sectionHeight() + 3;
        int nameWidth = slotsNameWidth(model, bounds.width());
        UiRect header = new UiRect(bounds.left() + nameWidth, y, bounds.right(), y + 9);
        UiRect.Size[] sizes = new UiRect.Size[classes.size() + 1];
        java.util.Arrays.fill(sizes, UiRect.Size.STAR);
        List<UiRect> cells = header.cols(4, sizes);
        TextFit.draw(graphics, font, SquadBoardText.t(SquadBoardText.MINE_SQUAD), bounds.left(),
                y, nameWidth - 6, TacticalBoardTheme.MUTED, TextFit.Align.LEFT);
        for (int index = 0; index < classes.size(); index++) {
            TextFit.draw(graphics, font, SquadLabels.className(snapshot,
                            classes.get(index).classId()), cells.get(index).left(), y,
                    cells.get(index).width(), TacticalBoardTheme.MUTED, TextFit.Align.LEFT);
        }
        UiRect people = cells.get(classes.size());
        TextFit.draw(graphics, font, SquadBoardText.t(SquadBoardText.SLOTS_PEOPLE),
                people.left(), y, people.width(), TacticalBoardTheme.MUTED, TextFit.Align.RIGHT);
        y += 11;
        int pitch = metrics.roomy() ? 13 : 11;
        for (SquadBoardModel.SquadRow row : model.squads()) {
            if (!row.configured()) {
                continue;
            }
            SquadView squad = row.view();
            boolean full = row.full();
            TextFit.draw(graphics, font, row.name(), bounds.left(), y, nameWidth - 6,
                    full ? TacticalBoardTheme.MUTED : TacticalBoardTheme.TEXT,
                    TextFit.Align.LEFT);
            for (int index = 0; index < classes.size(); index++) {
                String classId = classes.get(index).classId();
                ClassLimitView limit = squad.classLimit(classId);
                int used = limit != null ? limit.used() : (int) squad.members().stream()
                        .filter(member -> member.classId().equals(classId)).count();
                int max = limit != null ? limit.limit() : classes.get(index).limit();
                int color = full ? TacticalBoardTheme.MUTED : used < max
                        ? TacticalBoardTheme.TEXT : TacticalBoardTheme.DANGER;
                TextFit.draw(graphics, font, Component.translatable(SquadLabels.MEMBER_COUNT_KEY,
                                used, max), cells.get(index).left(), y, cells.get(index).width(),
                        color, TextFit.Align.LEFT);
            }
            TextFit.draw(graphics, font, row.count(), people.left(), y, people.width(),
                    full ? TacticalBoardTheme.DANGER : TacticalBoardTheme.MUTED,
                    TextFit.Align.RIGHT);
            y += pitch;
        }
        SquadBoardBlocks.endRegion(graphics);
    }

    // ---- current class card -------------------------------------------------------------------------

    /** Lays the card out; returns whether the change rules went into it. */
    private boolean layoutCurrent(SquadBoardModel model, boolean compactCard, boolean wantRules) {
        compact = compactCard;
        currentContent = TacticalDraw.panelContent(currentPanel, metrics,
                TacticalDraw.PanelStyle.titled(SquadBoardText.t(SquadBoardText.CURRENT_TITLE)));
        UiRect c = currentContent;
        int kv = metrics.roomy() ? 13 : 11;
        int y = c.top() + (compact ? 11 : 3 * kv + 3);
        UiRect key = new UiRect(c.left(), y, c.right(), y + metrics.buttonHeight());
        host.addKey(key, Component.translatable("screen.wok_infantry.class.configure_loadout"),
                TacticalIcon.GEAR, model.tabDisabledReason(BattleTab.LOADOUT),
                host::openLoadoutTab, LOADOUT_UI_ID);
        y += metrics.buttonHeight() + 3;
        buttonBottom = y;
        if (compact) {
            return false;
        }
        kit = kit(model.currentClassId());
        kitPitch = kitPitch();
        if (kit != null && !kit.isEmpty()) {
            if (y + metrics.gap() + kitHeight(KitMode.ROWS, kitPitch) <= c.bottom()) {
                kitMode = KitMode.ROWS;
            } else if (y + metrics.gap() + kitHeight(KitMode.STRIP, kitPitch) <= c.bottom()
                    && c.width() >= 20 * Math.min(kit.size(), 8) - 2) {
                kitMode = KitMode.STRIP;
            }
        }
        int below = y + metrics.gap() + kitHeight(kitMode, kitPitch) + 4;
        SquadBoardBlocks.RulesFit rules = wantRules ? SquadBoardBlocks.fitRules(font, metrics,
                c.width(), SquadBoardText.t(SquadBoardText.CLASSES_RULES_TITLE),
                classRules(model), c.bottom() - below - metrics.gap(), true) : null;
        int spare = c.bottom() - below - (rules != null ? rules.height() + metrics.gap() : 20);
        if (kitMode == KitMode.ROWS && metrics.roomy()) {
            kitPitch += Math.max(0, Math.min(8, spare / Math.max(1, kit.size())));
        }
        if (kitMode != KitMode.NONE) {
            kitTop = y + metrics.gap();
            y += metrics.gap() + kitHeight(kitMode, kitPitch) + 4;
        }
        if (rules != null) {
            cardRules = new UiRect(c.left(), y + metrics.gap(), c.right(),
                    y + metrics.gap() + rules.height());
            cardRulesFit = rules;
            return true;
        }
        cardNoteTop = y;
        return false;
    }

    private int kitPitch() {
        return metrics.roomy() ? 28 : metrics.tight() ? 20 : 22;
    }

    private int kitHeight(KitMode mode, int pitch) {
        int slots = kit == null ? 0 : kit.size();
        return switch (mode) {
            case ROWS -> metrics.sectionHeight() + 3 + slots * pitch;
            case STRIP -> metrics.sectionHeight() + 3 + 18;
            case NONE -> 0;
        };
    }

    /**
     * The saved loadout of {@code classId} as the loadout page shows it (selected entry, else the
     * first one), or {@code null} without a cached loadout snapshot for that class (user
     * decision: no silent refresh request).
     */
    static List<KitSlot> kit(String classId) {
        LoadoutSnapshot snapshot = ClientLoadoutState.snapshot();
        if (snapshot == null || classId == null || classId.isEmpty()
                || snapshot.config() == null || snapshot.player() == null) {
            return null;
        }
        Optional<LoadoutClassDefinition> found = snapshot.config().findClass(classId)
                .filter(LoadoutClassDefinition::enabled);
        if (found.isEmpty()) {
            return null;
        }
        LoadoutClassDefinition definition = found.get();
        List<KitSlot> result = new ArrayList<>();
        for (LoadoutSlotDefinition slot : definition.slotDefinitions()) {
            List<LoadoutEntry> entries = definition.entries(slot.id());
            String selectedId = snapshot.player().selectedEntry(classId, slot.id());
            LoadoutEntry entry = entries.stream().filter(candidate ->
                            candidate.id().equals(selectedId)).findFirst()
                    .orElse(entries.isEmpty() ? null : entries.get(0));
            ItemStack stack = entry == null ? ItemStack.EMPTY
                    : LoadoutEntryPreview.resolve(entry).stack();
            Component item = entry == null ? SquadBoardText.t(SquadBoardText.KIT_EMPTY)
                    : Component.literal(entry.displayName());
            result.add(new KitSlot(Component.literal(slot.displayName()), target(slot.target()),
                    stack, item, entry == null ? 0 : entry.count(), entry != null));
        }
        return result;
    }

    private static Component target(LoadoutInventoryTarget target) {
        if (target == null) {
            return Component.empty();
        }
        if (target == LoadoutInventoryTarget.OFFHAND) {
            return SquadBoardText.t(SquadBoardText.KIT_OFFHAND);
        }
        if (target.isArmor()) {
            return SquadBoardText.t(SquadBoardText.KIT_ARMOR);
        }
        return SquadBoardText.t(SquadBoardText.KIT_HOTBAR, target.inventoryIndex() + 1);
    }

    private void renderCurrent(GuiGraphics graphics, SquadBoardModel model) {
        SquadBoardModel.ClassRow row = model.currentClassRow();
        String classId = model.currentClassId();
        Component name = SquadLabels.className(model.snapshot(), classId.isEmpty() ? null
                : classId);
        SquadBoardBlocks.region(graphics, CURRENT_BOX, currentPanel);
        int metaCap = currentPanel.width() * 2 / 5;
        TacticalDraw.panel(graphics, font, currentPanel, metrics,
                TacticalDraw.PanelStyle.titled(SquadBoardText.t(SquadBoardText.CURRENT_TITLE))
                        .withMeta(font.width(name) <= metaCap ? name : Component.empty()));
        UiRect c = currentContent;
        boolean own = model.authority().inSquad();
        boolean active = model.snapshot().deployment().phase() == DeploymentPhase.ACTIVE;
        int kv = metrics.roomy() ? 13 : 11;
        int y = c.top();
        if (!compact) {
            TacticalDraw.kv(graphics, font, c.left(), y, c.width(),
                    SquadBoardText.t(SquadBoardText.MINE_CLASS), name, TacticalBoardTheme.TEXT);
            y += kv;
            Component quota = own && row != null ? Component.translatable(
                    SquadLabels.MEMBER_COUNT_KEY, row.quota().used(), row.quota().limit())
                    : Component.translatable(SquadLabels.UNASSIGNED_KEY);
            TacticalDraw.kv(graphics, font, c.left(), y, c.width(),
                    SquadBoardText.t(SquadBoardText.CURRENT_QUOTA), quota,
                    own ? TacticalBoardTheme.TEXT : TacticalBoardTheme.MUTED);
            y += kv;
            Component change = SquadBoardText.t(!own ? SquadBoardText.CURRENT_CHANGE_NO_SQUAD
                    : active ? SquadBoardText.CURRENT_CHANGE_ACTIVE
                    : SquadBoardText.CURRENT_CHANGE_WAITING);
            TacticalDraw.kv(graphics, font, c.left(), y, c.width(),
                    SquadBoardText.t(SquadBoardText.CURRENT_CHANGE), change,
                    own && !active ? TacticalBoardTheme.SUCCESS : TacticalBoardTheme.MUTED);
        } else {
            List<Component> line = !own ? SquadBoardText.all(
                    SquadBoardText.CURRENT_COMPACT_NO_SQUAD)
                    : List.of(SquadBoardText.t(SquadBoardText.CURRENT_COMPACT,
                    row == null ? 0 : row.quota().used(), row == null ? 0 : row.quota().limit(),
                    SquadBoardText.t(active ? SquadBoardText.CURRENT_COMPACT_ACTIVE
                            : SquadBoardText.CURRENT_COMPACT_WAITING)));
            SquadBoardBlocks.fitted(graphics, font, line, c.left(), y, c.width(),
                    TacticalBoardTheme.MUTED, TextFit.Align.LEFT);
        }
        if (kitMode != KitMode.NONE && kit != null) {
            renderKit(graphics, model, name, new UiRect(c.left(), kitTop, c.right(),
                    kitTop + kitHeight(kitMode, kitPitch)));
        }
        if (cardRulesFit != null) {
            SquadBoardBlocks.rules(graphics, font, cardRules, metrics, cardRulesFit);
        } else if (cardNoteTop >= 0) {
            int lines = (c.bottom() - cardNoteTop) / SquadBoardBlocks.LINE;
            SquadBoardBlocks.paragraph(graphics, font, SquadBoardText.all(
                            kitMode == KitMode.NONE ? SquadBoardText.NOTE_NO_KIT
                                    : SquadBoardText.NOTE_KIT), c.left(), cardNoteTop, c.width(),
                    TacticalBoardTheme.MUTED, lines);
        }
        SquadBoardBlocks.endRegion(graphics);
    }

    private void renderKit(GuiGraphics graphics, SquadBoardModel model, Component className,
                           UiRect bounds) {
        SquadBoardBlocks.subRegion(graphics, "squad.kit", bounds);
        Component meta = SquadBoardText.t(SquadBoardText.KIT_META, className);
        TacticalDraw.section(graphics, font, bounds.topSlice(metrics.sectionHeight()),
                SquadBoardText.t(SquadBoardText.KIT_TITLE),
                font.width(meta) <= bounds.width() * 2 / 5 ? meta : Component.empty(),
                TacticalBoardTheme.LIGHT_MUTED, TacticalBoardTheme.SECTION);
        int y = bounds.top() + metrics.sectionHeight() + 3;
        if (kitMode == KitMode.STRIP) {
            for (int index = 0; index < kit.size() && 20 * index + 18 <= bounds.width(); index++) {
                KitSlot slot = kit.get(index);
                TacticalDraw.slot(graphics, font, bounds.left() + index * 20, y, slot.stack(),
                        false, !slot.configured(), slot.count());
            }
            SquadBoardBlocks.endRegion(graphics);
            return;
        }
        boolean two = kitPitch >= 24;
        int labelWidth = Math.min(52, (int) Math.floor(bounds.width() * 0.3D));
        for (KitSlot slot : kit) {
            TacticalDraw.slot(graphics, font, bounds.left(), y + (kitPitch - 18) / 2 - 1,
                    slot.stack(), false, !slot.configured(), slot.count());
            Component item = slot.count() > 1 ? SquadBoardText.t(SquadBoardText.KIT_COUNT,
                    slot.item(), slot.count()) : slot.item();
            int itemColor = slot.configured() ? TacticalBoardTheme.TEXT : TacticalBoardTheme.MUTED;
            if (two) {
                int textY = y + (kitPitch - 19) / 2 - 1;
                TextFit.draw(graphics, font, item, bounds.left() + 24, textY,
                        bounds.width() - 24, itemColor, TextFit.Align.LEFT);
                TextFit.draw(graphics, font, SquadBoardText.t(SquadBoardText.KIT_SLOT,
                                slot.slot(), slot.target()), bounds.left() + 24, textY + 11,
                        bounds.width() - 24, TacticalBoardTheme.MUTED, TextFit.Align.LEFT);
            } else {
                int textY = y + (kitPitch - 8) / 2 - 1;
                TextFit.draw(graphics, font, slot.slot(), bounds.left() + 22, textY, labelWidth,
                        TacticalBoardTheme.MUTED, TextFit.Align.LEFT);
                TextFit.draw(graphics, font, item, bounds.left() + 26 + labelWidth, textY,
                        bounds.right() - (bounds.left() + 26 + labelWidth), itemColor,
                        TextFit.Align.LEFT);
            }
            y += kitPitch;
        }
        SquadBoardBlocks.endRegion(graphics);
    }

    // ---- vote ---------------------------------------------------------------------------------------

    /** Classes page while voting (preview {@code voteClassesPage}). */
    private void layoutVote(UiRect body, SquadBoardModel model) {
        voteData = FormationVotePanel.of(ClientFormationState.snapshot(), model.snapshot());
        List<UiRect> parts = wide ? body.cols(metrics.gap(), UiRect.Size.STAR,
                UiRect.Size.px(metrics.roomy() ? 280 : 176))
                : body.rows(metrics.gap(), UiRect.Size.STAR, UiRect.Size.px(
                SquadBoardBlocks.panelHeight(metrics, 11 + metrics.buttonHeight(), true)));
        listPanel = parts.get(0);
        currentPanel = parts.get(1);
        currentContent = TacticalDraw.panelContent(currentPanel, metrics,
                TacticalDraw.PanelStyle.titled(SquadBoardText.t(SquadBoardText.CURRENT_TITLE)));
        UiRect p = currentContent;
        int kv = metrics.roomy() ? 13 : 11;
        int y = p.top() + (wide ? 3 * kv + 3 : 11);
        host.addKey(new UiRect(p.left(), y, p.right(), y + metrics.buttonHeight()),
                Component.translatable("screen.wok_infantry.class.configure_loadout"),
                TacticalIcon.GEAR, FormationText.tabLockedReason(), () -> {
                }, LOADOUT_UI_ID);
        y += metrics.buttonHeight() + 3;
        voteTextTop = y;
        boolean infoHere = false;
        boolean rulesHere = false;
        if (wide && voteData.synced()) {
            y += 10 + metrics.gap();
            int infoHeight = metrics.sectionHeight() + 3 + 3 * kv;
            if (y + infoHeight <= p.bottom()) {
                voteInfo = new UiRect(p.left(), y, p.right(), y + infoHeight);
                y += infoHeight + metrics.gap();
                infoHere = true;
            }
            SquadBoardBlocks.FlowFit flow = SquadBoardBlocks.fitFlow(metrics,
                    FormationVotePanel.flowSteps(voteData).size(), p.bottom() - y);
            if (flow != null) {
                voteFlow = new UiRect(p.left(), y, p.right(), y + flow.height());
                voteFlowFit = flow;
            } else {
                SquadBoardBlocks.RulesFit lock = SquadBoardBlocks.fitRules(font, metrics,
                        p.width(), SquadBoardText.t(SquadBoardText.LOCK_RULES_TITLE),
                        lockRules(), p.bottom() - y, false);
                if (lock != null) {
                    voteLock = new UiRect(p.left(), y, p.right(), y + lock.height());
                    voteLockFit = lock;
                    rulesHere = true;
                }
            }
        }
        List<FormationVotePanel.After> after = new ArrayList<>();
        if (!rulesHere) {
            after.add(FormationVotePanel.After.rules(
                    SquadBoardText.t(SquadBoardText.LOCK_RULES_TITLE), lockRules()));
        }
        after.add(FormationVotePanel.howToVote(voteData));
        voteLayout = FormationVotePanel.layout(font, metrics, listPanel,
                new FormationVotePanel.Options(LIST_BOX,
                        SquadBoardText.t(SquadBoardText.CLASSES_TITLE),
                        Component.translatable(SquadBoardText.PREFIX + "sub.pending"),
                        List.of(SquadBoardText.t(SquadBoardText.CLASSES_VOTE_BLOCK)),
                        SquadBoardText.all(SquadBoardText.CLASSES_VOTE_HINT), null, !infoHere,
                        after), voteData);
        host.addKey(voteLayout.block().button(), FormationVotePanel.buttonLabel(voteData),
                FormationVotePanel.buttonIcon(voteData), null, host::openFormationTab,
                SquadScreen.VOTE_UI_ID);
    }

    static List<SquadBoardBlocks.Rule> lockRules() {
        return List.of(
                SquadBoardBlocks.Rule.of(SquadBoardText.all(SquadBoardText.LOCK_RULE_RESET)),
                SquadBoardBlocks.Rule.of(SquadBoardText.all(SquadBoardText.LOCK_RULE_QUOTA)),
                SquadBoardBlocks.Rule.of(SquadBoardText.all(SquadBoardText.LOCK_RULE_CHANGE)),
                SquadBoardBlocks.Rule.of(SquadBoardText.all(SquadBoardText.LOCK_RULE_LOADOUT)));
    }

    private void renderVote(GuiGraphics graphics, SquadBoardModel model) {
        FormationVotePanel.render(graphics, font, metrics, voteLayout, voteData);
        SquadBoardBlocks.region(graphics, CURRENT_BOX, currentPanel);
        TacticalDraw.panel(graphics, font, currentPanel, metrics,
                TacticalDraw.PanelStyle.titled(SquadBoardText.t(SquadBoardText.CURRENT_TITLE))
                        .withMeta(SquadBoardText.t(SquadBoardText.CURRENT_PENDING)));
        UiRect p = currentContent;
        int kv = metrics.roomy() ? 13 : 11;
        int y = p.top();
        if (wide) {
            TacticalDraw.kv(graphics, font, p.left(), y, p.width(),
                    SquadBoardText.t(SquadBoardText.MINE_CLASS),
                    SquadBoardText.t(SquadBoardText.CURRENT_VOTE_CLASS), TacticalBoardTheme.MUTED);
            TacticalDraw.kv(graphics, font, p.left(), y + kv, p.width(),
                    SquadBoardText.t(SquadBoardText.CURRENT_QUOTA),
                    Component.translatable(SquadBoardText.PREFIX + "count.none"),
                    TacticalBoardTheme.MUTED);
            TacticalDraw.kv(graphics, font, p.left(), y + 2 * kv, p.width(),
                    SquadBoardText.t(SquadBoardText.CURRENT_CHANGE),
                    SquadBoardText.t(SquadBoardText.CURRENT_VOTE_CHANGE),
                    TacticalBoardTheme.MUTED);
            SquadBoardBlocks.fitted(graphics, font, List.of(
                            SquadBoardText.t(SquadBoardText.CURRENT_VOTE_NONE)), p.left(),
                    voteTextTop, p.width(), TacticalBoardTheme.MUTED, TextFit.Align.LEFT);
        } else {
            SquadBoardBlocks.fitted(graphics, font, List.of(
                            SquadBoardText.t(SquadBoardText.CURRENT_VOTE_NARROW)), p.left(), y,
                    p.width(), TacticalBoardTheme.MUTED, TextFit.Align.LEFT);
        }
        if (!voteInfo.isEmpty()) {
            SquadBoardBlocks.subRegion(graphics, "squad.vote.info", voteInfo);
            Component meta = SquadBoardText.t(!voteData.open()
                    ? SquadBoardText.VOTE_META_WAIT_SHORT : voteData.changeAllowed()
                    ? SquadBoardText.VOTE_CHANGE : SquadBoardText.VOTE_FINAL);
            TacticalDraw.section(graphics, font, voteInfo.topSlice(metrics.sectionHeight()),
                    SquadBoardText.t(SquadBoardText.VOTE_MINE), meta,
                    TacticalBoardTheme.LIGHT_MUTED, TacticalBoardTheme.SECTION);
            List<FormationVotePanel.Info> info = List.of(FormationVotePanel.mineInfo(voteData),
                    FormationVotePanel.dueInfo(voteData), FormationVotePanel.extraInfo(voteData));
            int top = voteInfo.top() + metrics.sectionHeight() + 3;
            for (int index = 0; index < info.size(); index++) {
                FormationVotePanel.Info row = info.get(index);
                TacticalDraw.kv(graphics, font, voteInfo.left(), top + index * kv,
                        voteInfo.width(), row.key(), row.value(), row.color());
            }
            SquadBoardBlocks.endRegion(graphics);
        }
        if (voteFlowFit != null) {
            SquadBoardBlocks.flow(graphics, font, voteFlow, metrics,
                    FormationVotePanel.flowSteps(voteData), voteFlowFit);
        } else if (voteLockFit != null) {
            SquadBoardBlocks.rules(graphics, font, voteLock, metrics, voteLockFit);
        }
        SquadBoardBlocks.endRegion(graphics);
    }

    // ---- painter ------------------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, SquadBoardModel model, int mouseX, int mouseY) {
        if (vote) {
            renderVote(graphics, model);
            return;
        }
        SquadBoardBlocks.region(graphics, LIST_BOX, listPanel);
        TacticalDraw.panel(graphics, font, listPanel, metrics,
                TacticalDraw.PanelStyle.titled(SquadBoardText.t(SquadBoardText.CLASSES_TITLE))
                        .withMeta(listMeta(model)));
        if (list == null) {
            TacticalDraw.well(graphics, listWell);
        }
        if (!slotsBlock.isEmpty()) {
            renderSlots(graphics, model, slotsBlock);
        }
        if (!pager.isEmpty()) {
            TacticalDraw.pager(graphics, font, pager, page.page(), page.pageCount(),
                    TacticalDraw.pagerHit(pager, mouseX, mouseY));
        } else if (listRulesFit != null) {
            SquadBoardBlocks.rules(graphics, font, listRules, metrics, listRulesFit);
        } else if (noteTop >= 0) {
            int lines = (listContent.bottom() - noteTop) / SquadBoardBlocks.LINE;
            SquadBoardBlocks.paragraph(graphics, font, SquadBoardText.all(
                            SquadBoardText.CLASSES_NOTE), listContent.left(), noteTop,
                    listContent.width(), TacticalBoardTheme.MUTED, lines);
        }
        SquadBoardBlocks.endRegion(graphics);
        renderCurrent(graphics, model);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && !pager.isEmpty()) {
            int step = TacticalDraw.pagerHit(pager, mouseX, mouseY);
            if (step != 0) {
                turn(step);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (page.multiplePages() && listPanel.contains(mouseX, mouseY) && delta != 0.0D) {
            turn(delta > 0.0D ? -1 : 1);
            return true;
        }
        return false;
    }

    private void turn(int step) {
        int target = step < 0 ? page.previousPage() : page.nextPage();
        if (target != page.page()) {
            host.setClassPage(target);
        }
    }

    /** Rows of the shown page (for tests and the probe). */
    SquadBoardModel.Page page() {
        return page;
    }
}
