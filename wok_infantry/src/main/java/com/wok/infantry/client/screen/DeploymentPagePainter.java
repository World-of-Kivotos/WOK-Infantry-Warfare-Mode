package com.wok.infantry.client.screen;

import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.battle.MemberState;
import com.wok.infantry.battle.MemberView;
import com.wok.infantry.battle.SquadView;
import com.wok.infantry.client.ClientFormationState;
import com.wok.infantry.client.hud.SquadRosterModel;
import com.wok.infantry.deployment.DeploymentPhase;
import com.wok.infantry.deployment.DeploymentPoint;
import com.wok.infantry.deployment.DeploymentPointKind;
import com.wok.infantry.formation.vote.FormationVotePhase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Deployment page of the battle terminal (preview {@code 20-squad.js} {@code newDeploymentPage}).
 *
 * <p>Wide (content ≥ {@value #WIDE_MIN_WIDTH}): the viewer's deployment points on the left,
 * hugging their rows when the "部署点方位" schematic fits below; on the right the deployment
 * status: phase and respawn countdown (neutral, not orange), the checklist, the deploy key and its
 * reason, the in-combat group (resupply, redeploy with a confirmation), the squad mates and the
 * notes. Narrow (320 wide, and on the D2 device also 960×720 at GUI 1): status strip, points, and
 * an action bar whose single reason line covers its three keys.
 * Title, rows and pager of the point list share one pagination. While voting the list gives way
 * to the vote block and nothing can be deployed.
 */
final class DeploymentPagePainter implements SquadScreen.Painter {
    static final String POINTS_UI_ID = "squad.points";
    static final String POINTS_BOX = "squad.points_panel";
    static final String MAP_BOX = "squad.map_panel";
    static final String STATUS_BOX = "squad.deploy_panel";
    static final String ACTIONS_BOX = "squad.deploy_actions";

    private static final int POINT_ROW_MID = 24;
    /**
     * Narrowest content of the wide layout (preview {@code 20-squad.js}). On the D2 device 960×720
     * at GUI 1 (480×360 logical) gives 416 and is narrow; 480×270 gives 456 and stays wide.
     */
    static final int WIDE_MIN_WIDTH = 440;

    private final SquadScreen host;
    private final Font font;
    private final TacticalShellLayout.Metrics metrics;
    private final boolean vote;
    private final boolean wide;
    private final boolean active;
    private UiRect pointsPanel = UiRect.EMPTY;
    private UiRect mapPanel = UiRect.EMPTY;
    private UiRect statusPanel = UiRect.EMPTY;
    private UiRect actionsBar = UiRect.EMPTY;
    // points
    private UiRect pointsContent = UiRect.EMPTY;
    private UiRect pointsWell = UiRect.EMPTY;
    private SquadBoardModel.Page page = SquadBoardModel.Page.of(0, 1, 0);
    private UiRect pager = UiRect.EMPTY;
    private int noteTop = -1;
    private TacticalList<UUID> pointList;
    private DeploymentPointMap.Plan mapPlan;
    // status
    private StatusLayout status;
    // vote
    private FormationVotePanel.Layout voteLayout;
    private FormationVotePanel.Data voteData;

    /** Planned rows of the wide status panel. */
    private record StatusLayout(UiRect content, boolean meter, int checklistTop, int deployTop,
                                int deployReasonY, int combatTop, int combatKeysTop,
                                int whyY, UiRect flow, SquadBoardBlocks.FlowFit flowFit,
                                UiRect mates, int matePitch, UiRect rules,
                                SquadBoardBlocks.RulesFit rulesFit, int fallbackTop) {
    }

    DeploymentPagePainter(SquadScreen host, UiRect body, SquadBoardModel model) {
        this.host = host;
        this.font = host.boardFont();
        this.metrics = host.boardMetrics();
        this.vote = model.votePending();
        this.wide = wide(body);
        this.active = model.snapshot().deployment().phase() == DeploymentPhase.ACTIVE;
        if (vote) {
            voteData = FormationVotePanel.of(ClientFormationState.snapshot(), model.snapshot());
        }
        int count = model.pointRows().size();
        int tall = pointRowTall();
        if (wide) {
            List<UiRect> cols = body.cols(metrics.gap(), UiRect.Size.STAR,
                    UiRect.Size.px(metrics.roomy() ? 280 : 176));
            UiRect left = cols.get(0);
            statusPanel = cols.get(1);
            int[][] fits = {{tall, 64}, {POINT_ROW_MID, 48}};
            int fixedRow = 0;
            for (int[] fit : fits) {
                int listHeight = SquadBoardBlocks.panelHeight(metrics, count * fit[0] + 2, true);
                if (!vote && count > 0 && left.height() - listHeight - metrics.gap()
                        >= SquadBoardBlocks.panelHeight(metrics, fit[1], true)) {
                    List<UiRect> rows = left.rows(metrics.gap(), UiRect.Size.px(listHeight),
                            UiRect.Size.STAR);
                    pointsPanel = rows.get(0);
                    mapPanel = rows.get(1);
                    fixedRow = fit[0];
                    break;
                }
            }
            if (pointsPanel.isEmpty()) {
                pointsPanel = left;
            }
            layoutPoints(model, fixedRow);
            layoutStatus(model);
        } else {
            boolean meter = showsMeter(model);
            int topHeight = SquadBoardBlocks.panelHeight(metrics, 11 + (meter ? 7 : 0) + 10,
                    true);
            int actionsHeight = metrics.buttonHeight() + 12;
            List<UiRect> rows = body.rows(metrics.gap(), UiRect.Size.px(topHeight),
                    UiRect.Size.STAR, UiRect.Size.px(actionsHeight));
            statusPanel = rows.get(0);
            UiRect middle = rows.get(1);
            actionsBar = rows.get(2);
            int tallHeight = SquadBoardBlocks.panelHeight(metrics, count * tall + 2, true);
            if (!vote && count > 0 && middle.height() - tallHeight - metrics.gap()
                    >= SquadBoardBlocks.panelHeight(metrics, 70, true)) {
                List<UiRect> parts = middle.rows(metrics.gap(), UiRect.Size.px(tallHeight),
                        UiRect.Size.STAR);
                pointsPanel = parts.get(0);
                mapPanel = parts.get(1);
                layoutPoints(model, tall);
            } else {
                pointsPanel = middle;
                layoutPoints(model, 0);
            }
            layoutActionBar(model);
        }
        if (!mapPanel.isEmpty()) {
            UiRect content = TacticalDraw.panelContent(mapPanel, metrics,
                    TacticalDraw.PanelStyle.titled(SquadBoardText.t(SquadBoardText.MAP_TITLE)));
            List<DeploymentPointMap.Point> points = new ArrayList<>();
            for (SquadBoardModel.PointRow row : model.pointRows()) {
                points.add(DeploymentPointMap.point(row,
                        DeploymentPointMap.label(row, active).getString()));
            }
            // The grid step only depends on the scale; plan again with its scale-bar text.
            int step = DeploymentPointMap.plan(content, points, font::width, "").step();
            mapPlan = DeploymentPointMap.plan(content, points, font::width, scaleText(step));
        }
    }

    /** Whether {@code body} (the shell's content) gets the two-column layout. */
    static boolean wide(UiRect body) {
        return body.width() >= WIDE_MIN_WIDTH;
    }

    private String scaleText(int blocks) {
        return SquadBoardText.t(SquadBoardText.MAP_SCALE, blocks).getString();
    }

    private int pointRowTall() {
        return metrics.roomy() ? 40 : 35;
    }

    private static boolean showsMeter(SquadBoardModel model) {
        return !model.votePending() && model.snapshot().deployment().phase()
                == DeploymentPhase.WAITING;
    }

    // ---- points -----------------------------------------------------------------------------------

    private void layoutPoints(SquadBoardModel model, int fixedRow) {
        if (vote) {
            List<FormationVotePanel.After> after = List.of(
                    FormationVotePanel.After.rules(SquadBoardText.t(
                            SquadBoardText.POINT_RULES_TITLE), pointRules()),
                    FormationVotePanel.howToVote(voteData));
            voteLayout = FormationVotePanel.layout(font, metrics, pointsPanel,
                    new FormationVotePanel.Options(POINTS_BOX,
                            Component.translatable("screen.wok_infantry.deployment.points"),
                            Component.translatable(SquadBoardText.PREFIX + "sub.pending"),
                            List.of(SquadBoardText.t(SquadBoardText.POINTS_VOTE_BLOCK)),
                            SquadBoardText.all(SquadBoardText.POINTS_VOTE_HINT), TacticalIcon.LOCK,
                            true, after), voteData);
            host.addKey(voteLayout.block().button(), FormationVotePanel.buttonLabel(voteData),
                    FormationVotePanel.buttonIcon(voteData), null, host::openFormationTab,
                    SquadScreen.VOTE_UI_ID);
            return;
        }
        List<SquadBoardModel.PointRow> rows = model.pointRows();
        int count = rows.size();
        pointsContent = TacticalDraw.panelContent(pointsPanel, metrics,
                TacticalDraw.PanelStyle.titled(Component.translatable(
                        "screen.wok_infantry.deployment.points")));
        UiRect c = pointsContent;
        int natural = (c.height() - 2) / Math.max(1, count);
        int tall = pointRowTall();
        int rowHeight = fixedRow > 0 ? fixedRow : natural >= tall ? tall
                : Math.max(metrics.rowHeight(), Math.min(metrics.roomy() ? 32 : 24, natural));
        int perPage = Math.max(1, (c.height() - 2) / rowHeight);
        if (SquadBoardModel.Page.pageCount(count, perPage) > 1) {
            perPage = Math.max(1, (c.height() - 2 - metrics.buttonHeight() - metrics.gap())
                    / rowHeight);
        }
        page = SquadBoardModel.Page.of(count, perPage, host.deploymentPage());
        pointsWell = new UiRect(c.left(), c.top(), c.right(),
                Math.min(c.bottom(), c.top() + Math.max(1, page.size()) * rowHeight + 2));
        if (count == 0) {
            pointsWell = new UiRect(c.left(), c.top(), c.right(), c.bottom());
            return;
        }
        if (page.multiplePages()) {
            pager = new UiRect(c.left(), c.bottom() - metrics.buttonHeight(), c.right(),
                    c.bottom());
        } else if (pointsWell.bottom() + metrics.gap() + 10 <= c.bottom()) {
            noteTop = pointsWell.bottom() + metrics.gap() + 1;
        }
        pointList = new TacticalList<>(pointsWell.left(), pointsWell.top(), pointsWell.width(),
                pointsWell.height(), Component.translatable(
                "screen.wok_infantry.deployment.points"),
                id -> pointSpec(host.liveModel(), id));
        pointList.rowHeight(rowHeight).keyedBy(id -> id)
                .renderer((graphics, unused, bounds, id, spec, state) -> drawPoint(graphics,
                        bounds, host.liveModel(), id, state))
                .onSelect((index, id) -> {
                    if (!keyboard()) {
                        choose(id);
                    }
                })
                .onActivate((index, id) -> choose(id));
        List<UUID> ids = new ArrayList<>();
        page.slice(rows).forEach(row -> ids.add(row.point().id()));
        pointList.setItems(ids);
        SquadBoardModel.PointRow selected = model.selectedPoint();
        pointList.setSelectedIndex(selected == null ? -1 : ids.indexOf(selected.point().id()));
        host.addBoardWidget(pointList, POINTS_UI_ID);
    }

    static List<SquadBoardBlocks.Rule> pointRules() {
        return List.of(
                SquadBoardBlocks.Rule.of(SquadBoardText.all(SquadBoardText.POINT_RULE_MAIN)),
                SquadBoardBlocks.Rule.of(SquadBoardText.all(SquadBoardText.POINT_RULE_BEACON)),
                SquadBoardBlocks.Rule.of(SquadBoardText.all(SquadBoardText.POINT_RULE_RALLY)));
    }

    private static boolean keyboard() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && minecraft.getLastInputType().isKeyboard();
    }

    private void choose(UUID id) {
        SquadBoardModel live = host.liveModel();
        SquadBoardModel.PointRow row = pointRow(live, id);
        if (row != null && row.select().enabled()) {
            host.perform(row.select());
        }
        if (pointList != null) {
            SquadBoardModel.PointRow selected = live.selectedPoint();
            pointList.setSelectedIndex(selected == null ? -1
                    : pointList.items().indexOf(selected.point().id()));
        }
    }

    private static SquadBoardModel.PointRow pointRow(SquadBoardModel model, UUID id) {
        for (SquadBoardModel.PointRow row : model.pointRows()) {
            if (row.point().id().equals(id)) {
                return row;
            }
        }
        return null;
    }

    private TacticalDraw.RowSpec pointSpec(SquadBoardModel model, UUID id) {
        SquadBoardModel.PointRow row = pointRow(model, id);
        if (row == null) {
            return TacticalDraw.RowSpec.of("");
        }
        TacticalDraw.RowSpec spec = TacticalDraw.RowSpec.of(row.title()).withTooltip(
                SquadBoardText.t(SquadBoardText.POINTS_TOOLTIP, row.title(),
                        row.coordinatesWithHeight()));
        SquadBoardModel.ActionState select = row.select();
        return select.enabled() ? spec : spec.withDisabledReason(select.reason().full());
    }

    private static Component detail(SquadBoardModel.PointRow row) {
        DeploymentPoint point = row.point();
        return switch (point.kind()) {
            case MAIN_BASE -> SquadBoardText.t(SquadBoardText.POINTS_DETAIL_MAIN,
                    point.supplyRadius());
            case FIELD_BEACON -> SquadBoardText.t(SquadBoardText.POINTS_DETAIL_BEACON);
            case RALLY -> SquadBoardText.t(SquadBoardText.POINTS_DETAIL_RALLY);
        };
    }

    /** One point row (preview {@code pointRow}): icon, title, dimension and coordinates. */
    private void drawPoint(GuiGraphics graphics, UiRect bounds, SquadBoardModel model, UUID id,
                           TacticalDraw.RowState state) {
        SquadBoardModel.PointRow row = pointRow(model, id);
        if (row == null) {
            return;
        }
        boolean selected = row.selected();
        boolean locked = model.snapshot().deployment().phase() == DeploymentPhase.ACTIVE;
        TacticalDraw.rowBg(graphics, bounds, state.withSelected(selected || state.selected())
                .withDisabled(locked && !selected), 0);
        boolean highlighted = selected || state.selected();
        int pitch = bounds.height() + 1;
        int lines = pitch >= 34 ? 3 : pitch >= 21 ? 2 : 1;
        int y = bounds.top() + Math.floorDiv(bounds.height() - (lines * 11 - 3), 2);
        int iconColor = highlighted ? TacticalBoardTheme.ON_SELECT : locked
                ? TacticalBoardTheme.FAINT : TacticalBoardTheme.LIGHT_MUTED;
        DeploymentPointMap.icon(row.point().kind()).draw(graphics, bounds.left() + 6,
                bounds.top() + Math.floorDiv(bounds.height() - TacticalIcon.SIZE, 2), iconColor);
        int x = bounds.left() + 20;
        int right = bounds.right() - 5;
        Component tag = selected ? SquadBoardText.t(locked ? SquadBoardText.POINTS_TAG_HERE
                : SquadBoardText.POINTS_TAG_SELECTED) : null;
        int tagWidth = tag == null ? 0 : font.width(tag);
        Component coordinates = lines == 3 ? row.coordinatesWithHeight() : row.coordinates();
        int nameColor = highlighted ? TacticalBoardTheme.ON_SELECT : locked
                ? TacticalBoardTheme.FAINT : TacticalBoardTheme.LIGHT;
        int subColor = highlighted ? TacticalBoardTheme.SELECT_SUB : locked
                ? TacticalBoardTheme.FAINT : TacticalBoardTheme.LIGHT_MUTED;
        if (lines == 1) {
            int nameWidth = TextFit.draw(graphics, font, row.title(), x, y,
                    Math.max(0, right - x - tagWidth - 8), nameColor, TextFit.Align.LEFT).width();
            int coordinatesLeft = x + nameWidth + 8;
            int room = right - coordinatesLeft - (tagWidth > 0 ? tagWidth + 8 : 0);
            if (room > 16) {
                TextFit.draw(graphics, font, coordinates, coordinatesLeft, y, room, subColor,
                        TextFit.Align.LEFT);
            }
        } else {
            TextFit.draw(graphics, font, row.title(), x, y, Math.max(0, right - x - tagWidth - 8),
                    nameColor, TextFit.Align.LEFT);
            Component extra = lines == 2 && row.point().kind() == DeploymentPointKind.MAIN_BASE
                    ? SquadBoardText.t(SquadBoardText.POINTS_SUPPLY) : null;
            int extraWidth = extra == null ? 0 : font.width(extra);
            TextFit.draw(graphics, font, coordinates, x, y + 11,
                    Math.max(0, right - x - (extraWidth > 0 ? extraWidth + 8 : 0)), subColor,
                    TextFit.Align.LEFT);
            if (extra != null) {
                TextFit.draw(graphics, font, extra, right - extraWidth, y + 11, extraWidth,
                        highlighted ? TacticalBoardTheme.ON_SELECT : locked
                                ? TacticalBoardTheme.FAINT : TacticalBoardTheme.SUCCESS_B,
                        TextFit.Align.LEFT);
            }
            if (lines == 3) {
                TextFit.draw(graphics, font, detail(row), x, y + 22, right - x,
                        highlighted ? TacticalBoardTheme.SELECT_SUB : locked
                                ? TacticalBoardTheme.FAINT
                                : row.point().kind() == DeploymentPointKind.MAIN_BASE
                                ? TacticalBoardTheme.SUCCESS_B : TacticalBoardTheme.LIGHT_MUTED,
                        TextFit.Align.LEFT);
            }
        }
        if (tag != null) {
            TextFit.draw(graphics, font, tag, right - tagWidth, y, tagWidth,
                    TacticalBoardTheme.ON_SELECT, TextFit.Align.LEFT);
        }
    }

    private Component pointsMeta(SquadBoardModel model) {
        int count = model.pointRows().size();
        if (active) {
            return SquadBoardText.t(SquadBoardText.POINTS_META_ACTIVE);
        }
        return page.multiplePages() ? SquadBoardText.t(SquadBoardText.POINTS_META_PAGES, count,
                page.page() + 1, page.pageCount())
                : SquadBoardText.t(SquadBoardText.POINTS_META_COUNT, count);
    }

    private void renderPoints(GuiGraphics graphics, SquadBoardModel model, int mouseX,
                              int mouseY) {
        if (vote) {
            FormationVotePanel.render(graphics, font, metrics, voteLayout, voteData);
            return;
        }
        SquadBoardBlocks.region(graphics, POINTS_BOX, pointsPanel);
        Component meta = pointsMeta(model);
        TacticalDraw.panel(graphics, font, pointsPanel, metrics,
                TacticalDraw.PanelStyle.titled(Component.translatable(
                        "screen.wok_infantry.deployment.points")).withMeta(
                        font.width(meta) <= pointsPanel.width() * 2 / 5 ? meta
                                : Component.literal(page.label())));
        if (pointList == null) {
            TacticalDraw.well(graphics, pointsWell);
            TacticalDraw.empty(graphics, font, pointsWell.inset(2), TacticalIcon.FLAG,
                    SquadBoardModel.Reason.of(SquadBoardModel.ReasonCode.DEPLOY_NO_POINTS)
                            .shortForm(),
                    SquadBoardModel.Reason.of(SquadBoardModel.ReasonCode.DEPLOY_NO_POINTS).full(),
                    false);
        }
        if (!pager.isEmpty()) {
            TacticalDraw.pager(graphics, font, pager, page.page(), page.pageCount(),
                    TacticalDraw.pagerHit(pager, mouseX, mouseY));
        } else if (noteTop >= 0) {
            int lines = (pointsContent.bottom() - noteTop + 1) / SquadBoardBlocks.LINE;
            SquadBoardBlocks.paragraph(graphics, font, SquadBoardText.all(active
                            ? SquadBoardText.POINTS_NOTE_ACTIVE : SquadBoardText.POINTS_NOTE_WAITING),
                    pointsContent.left(), noteTop, pointsContent.width(), TacticalBoardTheme.MUTED,
                    lines);
        }
        SquadBoardBlocks.endRegion(graphics);
        if (!mapPanel.isEmpty() && mapPlan != null) {
            SquadBoardBlocks.region(graphics, MAP_BOX, mapPanel);
            TacticalDraw.panel(graphics, font, mapPanel, metrics,
                    TacticalDraw.PanelStyle.titled(SquadBoardText.t(SquadBoardText.MAP_TITLE))
                            .withMeta(SquadBoardText.t(SquadBoardText.MAP_META)));
            DeploymentPointMap.render(graphics, font, mapPlan, active);
            SquadBoardBlocks.endRegion(graphics);
        }
    }

    // ---- status (wide) ------------------------------------------------------------------------------

    private List<SquadBoardBlocks.Rule> statusRules(SquadBoardModel model) {
        List<SquadBoardBlocks.Rule> rules = new ArrayList<>();
        if (active) {
            int radius = mainBaseRadius(model);
            rules.add(SquadBoardBlocks.Rule.of(SquadBoardText.all(SquadBoardText.RULE_REDEPLOY)));
            rules.add(SquadBoardBlocks.Rule.of(SquadBoardText.all(SquadBoardText.RULE_RESUPPLY,
                    radius)));
            rules.add(SquadBoardBlocks.Rule.of(SquadBoardText.all(
                    SquadBoardText.RULE_POINT_WAITING)));
        } else if (vote) {
            rules.add(SquadBoardBlocks.Rule.of(SquadBoardText.all(SquadBoardText.LOCK_RULE_RESET)));
            rules.add(SquadBoardBlocks.Rule.of(SquadBoardText.all(SquadBoardText.RULE_AFTER_LOCK)));
            rules.add(SquadBoardBlocks.Rule.of(SquadBoardText.all(SquadBoardText.RULE_ISSUE)));
        } else {
            rules.add(SquadBoardBlocks.Rule.of(SquadBoardText.all(SquadBoardText.RULE_ISSUE)));
            rules.add(SquadBoardBlocks.Rule.of(SquadBoardText.all(SquadBoardText.RULE_REDEPLOY)));
            rules.add(SquadBoardBlocks.Rule.of(SquadBoardText.all(SquadBoardText.RULE_AUTO_CLOSE)));
        }
        return rules;
    }

    private static int mainBaseRadius(SquadBoardModel model) {
        for (SquadBoardModel.PointRow row : model.pointRows()) {
            if (row.point().kind() == DeploymentPointKind.MAIN_BASE) {
                return row.point().supplyRadius();
            }
        }
        return DeploymentPoint.DEFAULT_SUPPLY_RADIUS;
    }

    private static List<MemberView> mates(SquadBoardModel model) {
        BattleSnapshot snapshot = model.snapshot();
        if (model.votePending() || snapshot == null || snapshot.ownSquad() == null) {
            return List.of();
        }
        SquadView own = snapshot.squad(snapshot.ownSquad());
        if (own == null) {
            return List.of();
        }
        return own.members().stream().filter(member ->
                !member.playerId().equals(snapshot.viewerId())).toList();
    }

    private void layoutStatus(SquadBoardModel model) {
        UiRect c = TacticalDraw.panelContent(statusPanel, metrics,
                TacticalDraw.PanelStyle.titled(Component.translatable(
                        "screen.wok_infantry.deployment_status")));
        int y = c.top() + 11;
        boolean meter = showsMeter(model);
        if (meter) {
            y += 7;
        }
        int checklistTop = y;
        if (!active) {
            y += model.checklist().size() * 11;
        } else {
            y += 3 * 11;
        }
        y += 3;
        int deployTop = y;
        host.addActionButton(new UiRect(c.left(), y, c.right(), y + metrics.buttonHeight()),
                model.deploy(), SquadBoardModel::deploy, TacticalIcon.DEPLOY);
        y += metrics.buttonHeight() + 2;
        int deployReasonY = y;
        y += 12;
        int combatTop = y;
        y += metrics.sectionHeight() + 3;
        int combatKeysTop = y;
        List<UiRect> cells = new UiRect(c.left(), y, c.right(), y + metrics.buttonHeight())
                .cols(metrics.gap(), UiRect.Size.STAR, UiRect.Size.STAR);
        host.addActionButton(cells.get(0), model.resupply(), SquadBoardModel::resupply,
                TacticalIcon.BOX);
        host.addActionButton(cells.get(1), model.redeploy(), SquadBoardModel::redeploy, null);
        y += metrics.buttonHeight() + 2;
        int whyY = y;
        y += 12;
        int available = c.bottom() - y - metrics.gap();
        int one = metrics.roomy() ? 13 : 11;
        List<SquadBoardBlocks.Rule> rules = statusRules(model);
        List<MemberView> mates = mates(model);
        UiRect flow = UiRect.EMPTY;
        SquadBoardBlocks.FlowFit flowFit = null;
        UiRect matesBox = UiRect.EMPTY;
        int matePitch = 0;
        UiRect rulesBox = UiRect.EMPTY;
        SquadBoardBlocks.RulesFit rulesFit = null;
        Component rulesTitle = SquadBoardText.t(SquadBoardText.DEPLOY_RULES_TITLE);
        boolean planned = false;
        if (vote && voteData != null && voteData.synced()) {
            SquadBoardBlocks.RulesFit fit = SquadBoardBlocks.fitRules(font, metrics, c.width(),
                    rulesTitle, rules.subList(2, rules.size()), available, true);
            SquadBoardBlocks.FlowFit flowFitted = fit == null ? null : SquadBoardBlocks.fitFlow(
                    metrics, FormationVotePanel.flowSteps(voteData).size(),
                    available - fit.height() - metrics.gap());
            if (flowFitted != null) {
                y += metrics.gap();
                flow = new UiRect(c.left(), y, c.right(), y + flowFitted.height());
                flowFit = flowFitted;
                y += flowFitted.height();
                rulesBox = new UiRect(c.left(), y + metrics.gap(), c.right(),
                        y + metrics.gap() + fit.height());
                rulesFit = fit;
                planned = true;
            }
        }
        if (!planned) {
            int[] pitches = mates.isEmpty() ? new int[]{0} : new int[]{one + 11, one, 0};
            for (int pitch : pitches) {
                int matesHeight = pitch > 0 ? metrics.sectionHeight() + 3 + mates.size() * pitch
                        + metrics.gap() : 0;
                SquadBoardBlocks.RulesFit fit = matesHeight < available
                        ? SquadBoardBlocks.fitRules(font, metrics, c.width(), rulesTitle, rules,
                        available - matesHeight, true) : null;
                if (fit != null) {
                    if (pitch > 0) {
                        y += metrics.gap();
                        matesBox = new UiRect(c.left(), y, c.right(),
                                y + metrics.sectionHeight() + 3 + mates.size() * pitch);
                        matePitch = pitch;
                        y = matesBox.bottom();
                    }
                    rulesBox = new UiRect(c.left(), y + metrics.gap(), c.right(),
                            y + metrics.gap() + fit.height());
                    rulesFit = fit;
                    planned = true;
                    break;
                }
            }
        }
        if (!planned) {
            SquadBoardBlocks.RulesFit fit = SquadBoardBlocks.fitRules(font, metrics, c.width(),
                    rulesTitle, rules, available, false);
            if (fit != null) {
                rulesBox = new UiRect(c.left(), y + metrics.gap(), c.right(),
                        y + metrics.gap() + fit.height());
                rulesFit = fit;
                planned = true;
            }
        }
        status = new StatusLayout(c, meter, checklistTop, deployTop, deployReasonY, combatTop,
                combatKeysTop, whyY, flow, flowFit, matesBox, matePitch, rulesBox, rulesFit,
                planned ? -1 : y);
    }

    private Component phaseWord(SquadBoardModel model) {
        if (active) {
            return SquadBoardText.t(SquadBoardText.DEPLOY_PHASE_ACTIVE);
        }
        if (vote) {
            return SquadBoardText.t(SquadBoardText.DEPLOY_PHASE_VOTE);
        }
        return SquadBoardText.t(model.snapshot().deployment().phase() == DeploymentPhase.WAITING
                ? SquadBoardText.DEPLOY_PHASE_WAITING : SquadBoardText.DEPLOY_PHASE_READY);
    }

    private Component phaseValue(SquadBoardModel model) {
        if (active) {
            SquadBoardModel.PointRow point = model.selectedPoint();
            return point == null ? Component.empty()
                    : SquadBoardText.t(SquadBoardText.DEPLOY_DEPLOYED_AT, point.title());
        }
        if (vote) {
            return SquadBoardText.t(model.input().votePhase() == FormationVotePhase.OPEN
                    ? SquadBoardText.VOTE_META_OPEN_SHORT : SquadBoardText.VOTE_META_WAIT_SHORT);
        }
        int seconds = model.respawnSeconds();
        return seconds > 0 ? SquadBoardText.t(SquadBoardText.DEPLOY_SECONDS, seconds)
                : Component.empty();
    }

    private static int checkColor(SquadBoardModel.CheckState state) {
        return switch (state) {
            case DONE -> TacticalBoardTheme.SUCCESS;
            case TODO -> TacticalBoardTheme.DANGER;
            case WAITING -> TacticalBoardTheme.TEXT;
            case LOCKED -> TacticalBoardTheme.MUTED;
        };
    }

    private static TacticalIcon checkIcon(SquadBoardModel.CheckState state) {
        return switch (state) {
            case DONE -> TacticalIcon.CHECK;
            case TODO -> TacticalIcon.CLOSE;
            case WAITING -> TacticalIcon.CLOCK;
            case LOCKED -> TacticalIcon.LOCK;
        };
    }

    private void renderStatus(GuiGraphics graphics, SquadBoardModel model) {
        SquadBoardBlocks.region(graphics, STATUS_BOX, statusPanel);
        TacticalDraw.panel(graphics, font, statusPanel, metrics,
                TacticalDraw.PanelStyle.titled(Component.translatable(
                        "screen.wok_infantry.deployment_status")));
        StatusLayout layout = status;
        UiRect c = layout.content();
        int y = c.top();
        renderPhaseLine(graphics, model, c, y);
        y += 11;
        if (layout.meter()) {
            TacticalDraw.meter(graphics, new UiRect(c.left(), y, c.right(), y + 3),
                    host.respawnProgress(model), TacticalBoardTheme.MUTED, TacticalBoardTheme.WELL,
                    0);
        }
        y = layout.checklistTop();
        if (!active) {
            int labelWidth = 12;
            for (SquadBoardModel.ChecklistItem item : model.checklist()) {
                labelWidth = Math.max(labelWidth, font.width(item.label()) + 6);
            }
            labelWidth = Math.min(labelWidth, c.width() / 2);
            for (SquadBoardModel.ChecklistItem item : model.checklist()) {
                checkIcon(item.state()).draw(graphics, c.left(), y - 1, checkColor(item.state()));
                TextFit.draw(graphics, font, item.label(), c.left() + 12, y, labelWidth - 4,
                        TacticalBoardTheme.MUTED, TextFit.Align.LEFT);
                TextFit.draw(graphics, font, item.value(), c.left() + 12 + labelWidth, y,
                        Math.max(0, c.width() - 12 - labelWidth),
                        item.state() == SquadBoardModel.CheckState.DONE ? TacticalBoardTheme.TEXT
                                : TacticalBoardTheme.MUTED, TextFit.Align.LEFT);
                y += 11;
            }
        } else {
            BattleSnapshot snapshot = model.snapshot();
            TacticalDraw.kv(graphics, font, c.left(), y, c.width(),
                    SquadBoardText.t(SquadBoardText.MINE_SQUAD), snapshot.ownSquad() == null
                            ? Component.translatable(SquadLabels.UNASSIGNED_KEY)
                            : SquadLabels.callsign(snapshot.ownSquad()), TacticalBoardTheme.TEXT);
            String classId = model.currentClassId();
            TacticalDraw.kv(graphics, font, c.left(), y + 11, c.width(),
                    SquadBoardText.t(SquadBoardText.MINE_CLASS), SquadLabels.className(snapshot,
                            classId.isEmpty() ? null : classId), TacticalBoardTheme.TEXT);
            int resupply = model.resupplySeconds();
            TacticalDraw.kv(graphics, font, c.left(), y + 22, c.width(),
                    SquadBoardText.t(SquadBoardText.DEPLOY_COOLDOWN), resupply > 0
                            ? SquadBoardText.t(SquadBoardText.DEPLOY_SECONDS, resupply)
                            : model.resupply().explanation().shortForm(), TacticalBoardTheme.TEXT);
        }
        SquadBoardModel.Reason deployReason = model.deploy().explanation();
        if (deployReason != null) {
            SquadBoardBlocks.fitted(graphics, font, deployReason.candidates(), c.left(),
                    layout.deployReasonY(), c.width(), TacticalBoardTheme.MUTED,
                    TextFit.Align.LEFT);
        }
        Component combatMeta = active ? Component.empty()
                : SquadBoardText.t(SquadBoardText.DEPLOY_COMBAT_META);
        TacticalDraw.section(graphics, font, new UiRect(c.left(), layout.combatTop(), c.right(),
                        layout.combatTop() + metrics.sectionHeight()),
                SquadBoardText.t(SquadBoardText.DEPLOY_COMBAT),
                font.width(combatMeta) <= c.width() * 2 / 5 ? combatMeta : Component.empty(),
                TacticalBoardTheme.LIGHT_MUTED, TacticalBoardTheme.SECTION);
        SquadBoardBlocks.fitted(graphics, font, whyLine(model), c.left(), layout.whyY(),
                c.width(), TacticalBoardTheme.MUTED, TextFit.Align.LEFT);
        if (layout.flowFit() != null && voteData != null) {
            SquadBoardBlocks.flow(graphics, font, layout.flow(), metrics,
                    FormationVotePanel.flowSteps(voteData), layout.flowFit());
        }
        if (layout.matePitch() > 0) {
            renderMates(graphics, model, layout.mates(), layout.matePitch());
        }
        if (layout.rulesFit() != null) {
            SquadBoardBlocks.rules(graphics, font, layout.rules(), metrics, layout.rulesFit());
        } else if (layout.fallbackTop() >= 0 && layout.fallbackTop() + 9 <= c.bottom()) {
            SquadBoardBlocks.paragraph(graphics, font, statusRules(model).get(0).variants(),
                    c.left(), layout.fallbackTop(), c.width(), TacticalBoardTheme.FAINT,
                    (c.bottom() - layout.fallbackTop() + 1) / SquadBoardBlocks.LINE);
        }
        SquadBoardBlocks.endRegion(graphics);
    }

    private void renderPhaseLine(GuiGraphics graphics, SquadBoardModel model, UiRect c, int y) {
        // Waiting (respawn countdown, vote) is neutral; only "作战中" is green. The preview draws
        // the "N 秒" countdown orange (20-squad.js:1581); the Java port made it TEXT on purpose
        // (JAVA_PORT_PLAN 6.2: orange is for sections and adjustable controls), so there is no
        // orange text here that would need ACCENT_TEXT.
        TacticalDraw.led(graphics, c.left(), y + 2, active ? TacticalBoardTheme.SUCCESS
                : TacticalBoardTheme.MUTED);
        Component phase = phaseWord(model);
        int phaseWidth = TextFit.draw(graphics, font, phase, c.left() + 7, y,
                c.width() - 7, TacticalBoardTheme.TEXT, TextFit.Align.LEFT).width();
        int x = c.left() + 7 + phaseWidth + 6;
        for (Component value : phaseValues(model)) {
            if (!value.getString().isEmpty() && font.width(value) <= c.right() - x) {
                TextFit.draw(graphics, font, value, x, y, c.right() - x,
                        active || vote ? TacticalBoardTheme.MUTED : TacticalBoardTheme.TEXT,
                        TextFit.Align.RIGHT);
                return;
            }
        }
    }

    /** The value after the phase word, longest first (left out when none fits). */
    private List<Component> phaseValues(SquadBoardModel model) {
        Component value = phaseValue(model);
        SquadBoardModel.PointRow point = model.selectedPoint();
        if (active && point != null) {
            return List.of(value, point.title());
        }
        return List.of(value);
    }

    private List<Component> whyLine(SquadBoardModel model) {
        if (!active) {
            return List.of(SquadBoardText.t(SquadBoardText.DEPLOY_WHY_WAITING));
        }
        int seconds = model.resupplySeconds();
        if (seconds > 0) {
            return SquadBoardText.all(SquadBoardText.DEPLOY_WHY_ACTIVE, seconds);
        }
        SquadBoardModel.Reason reason = model.resupply().explanation();
        return reason == null ? List.of() : reason.candidates();
    }

    private void renderMates(GuiGraphics graphics, SquadBoardModel model, UiRect box, int pitch) {
        List<MemberView> mates = mates(model);
        long alive = mates.stream().filter(member -> member.state().hasVitals()).count();
        SquadBoardBlocks.subRegion(graphics, "squad.mates", box);
        Component meta = SquadBoardText.t(SquadBoardText.MATES_META, alive, mates.size());
        TacticalDraw.section(graphics, font, box.topSlice(metrics.sectionHeight()),
                SquadBoardText.t(SquadBoardText.MATES_TITLE),
                font.width(meta) <= box.width() * 2 / 5 ? meta : Component.empty(),
                TacticalBoardTheme.LIGHT_MUTED, TacticalBoardTheme.SECTION);
        int one = metrics.roomy() ? 13 : 11;
        boolean two = pitch > one;
        int y = box.top() + metrics.sectionHeight() + 3;
        BattleSnapshot snapshot = model.snapshot();
        for (MemberView mate : mates) {
            if (y + pitch > box.bottom() + 1) {
                break;
            }
            SquadRosterModel.StatusStyle style = SquadRosterModel.style(mate.state());
            int textY = y + (two ? 1 : (pitch - 9) / 2);
            int stateColor = SquadPagePainter.boardColor(mate.state());
            graphics.fill(box.left() + 1, textY + 2, box.left() + 5, textY + 6, stateColor);
            Component right;
            if (mate.state() == MemberState.DEAD) {
                right = SquadBoardText.t(SquadBoardText.MEMBER_DEAD_WAITING);
            } else if (style.hasTag()) {
                right = Component.translatable(style.tagKey());
            } else if (mate.hasHealthRatio()) {
                right = Component.translatable(SquadLabels.MEMBER_COUNT_KEY,
                        Math.round(mate.health()), Math.round(mate.maxHealth()));
            } else {
                right = SquadBoardText.t(SquadBoardText.MEMBER_DEPLOYED);
            }
            int rightWidth = font.width(right);
            TextFit.draw(graphics, font, mate.name(), box.left() + 9, textY,
                    Math.max(0, box.width() - 9 - rightWidth - 6),
                    mate.state() == MemberState.OFFLINE ? TacticalBoardTheme.MUTED
                            : TacticalBoardTheme.TEXT, TextFit.Align.LEFT);
            TextFit.draw(graphics, font, right, box.right() - rightWidth, textY, rightWidth,
                    style.hasTag() ? SquadPagePainter.boardTextColor(mate.state())
                            : TacticalBoardTheme.MUTED, TextFit.Align.LEFT);
            if (two) {
                Component className = SquadLabels.className(snapshot, mate.classId());
                int classWidth = TextFit.draw(graphics, font, className, box.left() + 9,
                        textY + 11, box.width() - 9, TacticalBoardTheme.MUTED,
                        TextFit.Align.LEFT).width();
                int barLeft = box.left() + 9 + classWidth + 6;
                if (mate.state().hasVitals() && mate.hasHealthRatio()
                        && barLeft < box.right() - 10) {
                    float ratio = mate.healthRatio();
                    TacticalDraw.meter(graphics, new UiRect(barLeft, textY + 14, box.right(),
                                    textY + 16), ratio, SquadBoardBlocks.boardHealthColor(ratio),
                            TacticalBoardTheme.WELL, 0);
                }
            }
            y += pitch;
        }
        SquadBoardBlocks.endRegion(graphics);
    }

    // ---- compact (320 wide) -------------------------------------------------------------------------

    private void layoutActionBar(SquadBoardModel model) {
        UiRect keys = new UiRect(actionsBar.left(), actionsBar.top(), actionsBar.right(),
                actionsBar.top() + metrics.buttonHeight());
        List<UiRect> cells = keys.cols(metrics.gap(), UiRect.Size.STAR, UiRect.Size.STAR,
                UiRect.Size.STAR);
        host.addActionButton(cells.get(0), model.deploy(), SquadBoardModel::deploy,
                TacticalIcon.DEPLOY);
        host.addActionButton(cells.get(1), model.resupply(), SquadBoardModel::resupply,
                TacticalIcon.BOX);
        host.addActionButton(cells.get(2), model.redeploy(), SquadBoardModel::redeploy, null);
    }

    private void renderCompactStatus(GuiGraphics graphics, SquadBoardModel model) {
        SquadBoardBlocks.region(graphics, STATUS_BOX, statusPanel);
        Component meta = active ? SquadBoardText.t(SquadBoardText.DEPLOY_PHASE_ACTIVE)
                : vote ? SquadBoardText.t(SquadBoardText.DEPLOY_PHASE_VOTE)
                : model.snapshot().deployment().phase() == DeploymentPhase.WAITING
                ? SquadBoardText.t(SquadBoardText.MINE_WAITING, model.respawnSeconds())
                : SquadBoardText.t(SquadBoardText.DEPLOY_PHASE_READY);
        UiRect c = TacticalDraw.panel(graphics, font, statusPanel, metrics,
                TacticalDraw.PanelStyle.titled(Component.translatable(
                        "screen.wok_infantry.deployment_status")).withMeta(meta));
        int y = c.top();
        int x = c.left();
        List<SquadBoardModel.ChecklistItem> items = active ? deployedItems(model)
                : model.checklist();
        int slot = c.width() / 3;
        for (int index = 0; index < Math.min(3, items.size()); index++) {
            SquadBoardModel.ChecklistItem item = items.get(index);
            checkIcon(item.state()).draw(graphics, x, y - 1, checkColor(item.state()));
            int width = TextFit.draw(graphics, font, item.value(), x + 11, y,
                    Math.max(0, slot - 14), item.state() == SquadBoardModel.CheckState.DONE
                            ? TacticalBoardTheme.TEXT : vote ? TacticalBoardTheme.MUTED
                            : TacticalBoardTheme.TEXT, TextFit.Align.LEFT).width();
            x += 11 + Math.max(width, 0) + 8;
            x = Math.max(x, c.left() + slot * (index + 1));
        }
        y += 11;
        if (showsMeter(model)) {
            TacticalDraw.meter(graphics, new UiRect(c.left(), y, c.right(), y + 3),
                    host.respawnProgress(model), TacticalBoardTheme.MUTED, TacticalBoardTheme.WELL,
                    0);
            y += 7;
        }
        Component line = active ? SquadBoardText.t(SquadBoardText.DEPLOY_COMPACT_ACTIVE,
                model.resupplySeconds())
                : vote ? SquadBoardText.t(SquadBoardText.DEPLOY_COMPACT_VOTE)
                : SquadBoardText.t(SquadBoardText.DEPLOY_COMPACT_WAIT);
        SquadBoardBlocks.fitted(graphics, font, List.of(line), c.left(), y, c.width(),
                TacticalBoardTheme.MUTED, TextFit.Align.LEFT);
        SquadBoardBlocks.endRegion(graphics);
    }

    /**
     * The compact status strip in combat (preview {@code deployCompact}): squad, class and the
     * point deployed at, where waiting shows the checklist (it is empty in combat).
     */
    static List<SquadBoardModel.ChecklistItem> deployedItems(SquadBoardModel model) {
        BattleSnapshot snapshot = model.snapshot();
        if (snapshot == null) {
            return List.of();
        }
        List<SquadBoardModel.ChecklistItem> items = new ArrayList<>(3);
        items.add(snapshot.ownSquad() == null
                ? new SquadBoardModel.ChecklistItem(SquadBoardModel.CheckItem.SQUAD,
                SquadBoardModel.CheckState.TODO,
                Component.translatable(SquadLabels.UNASSIGNED_KEY))
                : new SquadBoardModel.ChecklistItem(SquadBoardModel.CheckItem.SQUAD,
                SquadBoardModel.CheckState.DONE, SquadLabels.callsign(snapshot.ownSquad())));
        String classId = model.currentClassId();
        items.add(new SquadBoardModel.ChecklistItem(SquadBoardModel.CheckItem.CLASS,
                SquadBoardModel.CheckState.DONE, SquadLabels.className(snapshot,
                classId.isEmpty() ? null : classId)));
        SquadBoardModel.PointRow point = model.selectedPoint();
        if (point != null) {
            items.add(new SquadBoardModel.ChecklistItem(SquadBoardModel.CheckItem.POINT,
                    SquadBoardModel.CheckState.DONE, point.title()));
        }
        return List.copyOf(items);
    }

    private void renderActionReason(GuiGraphics graphics, SquadBoardModel model) {
        SquadBoardBlocks.region(graphics, ACTIONS_BOX, actionsBar);
        List<Component> reason = new ArrayList<>();
        if (active) {
            int seconds = model.resupplySeconds();
            if (seconds > 0) {
                reason.add(SquadBoardText.t(SquadBoardText.DEPLOY_REASON_ACTIVE, seconds));
            }
            SquadBoardModel.Reason resupply = model.resupply().explanation();
            if (resupply != null) {
                reason.addAll(resupply.candidates());
            }
        } else if (vote) {
            reason.addAll(SquadBoardText.all(SquadBoardText.DEPLOY_REASON_VOTE));
        } else {
            int seconds = model.respawnSeconds();
            if (seconds > 0) {
                reason.add(SquadBoardText.t(SquadBoardText.DEPLOY_REASON_WAIT, seconds));
            }
            SquadBoardModel.Reason deploy = model.deploy().explanation();
            if (deploy != null) {
                reason.add(SquadBoardText.t(SquadBoardText.DEPLOY_REASON_COMBINED,
                        deploy.shortForm()));
                reason.addAll(deploy.candidates());
            }
        }
        SquadBoardBlocks.fitted(graphics, font, reason, actionsBar.left(),
                actionsBar.bottom() - 9, actionsBar.width(), TacticalBoardTheme.MUTED,
                TextFit.Align.LEFT);
        SquadBoardBlocks.endRegion(graphics);
    }

    // ---- painter ------------------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, SquadBoardModel model, int mouseX, int mouseY) {
        renderPoints(graphics, model, mouseX, mouseY);
        if (wide) {
            renderStatus(graphics, model);
        } else {
            renderCompactStatus(graphics, model);
            renderActionReason(graphics, model);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (!pager.isEmpty()) {
            int step = TacticalDraw.pagerHit(pager, mouseX, mouseY);
            if (step != 0) {
                turn(step);
                return true;
            }
        }
        if (mapPlan != null && !active) {
            UUID hit = DeploymentPointMap.hit(mapPlan, mouseX, mouseY);
            if (hit != null) {
                SquadBoardModel.PointRow row = pointRow(host.liveModel(), hit);
                if (row != null && row.select().enabled()) {
                    host.perform(row.select());
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (page.multiplePages() && pointsPanel.contains(mouseX, mouseY) && delta != 0.0D) {
            turn(delta > 0.0D ? -1 : 1);
            return true;
        }
        return false;
    }

    private void turn(int step) {
        int target = step < 0 ? page.previousPage() : page.nextPage();
        if (target != page.page()) {
            host.setDeploymentPage(target);
        }
    }

    /** The shown page of the point list (title, rows and pager share it). */
    SquadBoardModel.Page page() {
        return page;
    }
}
