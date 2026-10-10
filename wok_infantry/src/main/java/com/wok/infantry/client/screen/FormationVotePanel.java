package com.wok.infantry.client.screen;

import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.client.ui.probe.UiLayoutProbe;
import com.wok.infantry.formation.selection.FactionSelectionView;
import com.wok.infantry.formation.selection.FormationDetailView;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.formation.selection.FormationSelectionView;
import com.wok.infantry.formation.vote.FormationVotePhase;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The formation-vote waiting block that the squad, class and deployment pages show while the
 * viewer's faction has not locked its formation (preview {@code 20-squad.js} {@code votePanel}).
 * A dark well holds the call to action (title, rule text, a key to the formation tab) and a
 * read-only tally of the candidates; below it come "你的票 / 截止" and explanation blocks (the
 * match flow, how voting works, what the lock does). On a tall panel the well hugs its content and
 * the candidate rows grow to two or three lines (category, vehicles, summary); otherwise the well
 * takes the panel and centres its content.
 *
 * <p>Everything here only reads the cached catalog ({@code ClientFormationState}); nothing can be
 * voted from this block. Waiting and to-do states use neutral text, never orange.
 */
final class FormationVotePanel {
    private static final int TALLY_TALL = 40;
    private static final int TALLY_TWO = 29;

    private FormationVotePanel() {
    }

    // ---- data -------------------------------------------------------------------------------------

    /** One candidate row of the tally. */
    record Candidate(String id, Component name, Component category, Component vehicles,
                     String description, int votes, boolean leading) {
    }

    /**
     * What the block shows.
     *
     * @param synced     a catalog is cached (otherwise the block asks to sync and has no tally)
     * @param open       the faction's vote is open (else not started)
     * @param mine       the viewer's vote, or {@code null}
     * @param lead       leader text ("机动部队 4 票", "并列：…", "暂无人投票")
     */
    record Data(boolean synced, boolean open, boolean changeAllowed, Component faction, int voted,
                int total, Component mine, List<Candidate> candidates, Component lead) {
        Data {
            candidates = List.copyOf(candidates);
        }

        static final Data SYNCING = new Data(false, false, true, Component.empty(), 0, 0, null,
                List.of(), Component.empty());
    }

    /** The data of the viewer's own faction in {@code catalog}. */
    static Data of(FormationSelectionSnapshot catalog, BattleSnapshot battle) {
        FormationVoteModel model = FormationVoteModel.of(catalog, "", "", false);
        FactionSelectionView faction = model.joinedFaction();
        if (catalog == null || faction == null) {
            return Data.SYNCING;
        }
        boolean open = catalog.votePhase() == FormationVotePhase.OPEN;
        FormationVoteModel.Leaders leaders = model.leaders();
        List<Candidate> candidates = new ArrayList<>();
        for (FormationSelectionView formation : FormationVoteModel.orderedFormations(faction)) {
            boolean candidate = open ? model.candidate(faction, formation) : formation.available();
            if (!candidate) {
                continue;
            }
            int votes = open ? model.votes(formation.id()) : 0;
            candidates.add(new Candidate(formation.id(), Component.literal(formation.displayName()),
                    Component.literal(formation.categoryDisplayName()), vehicles(formation),
                    formation.description(), votes,
                    open && votes > 0 && leaders.ids().contains(formation.id())));
        }
        Component mine = null;
        if (open && !catalog.ownVoteFormationId().isBlank()) {
            mine = Component.literal(model.formationName(catalog.ownVoteFormationId()));
        }
        Component lead;
        if (leaders.none()) {
            lead = SquadBoardText.t(SquadBoardText.VOTE_LEAD_NONE);
        } else if (leaders.tie()) {
            List<Component> names = new ArrayList<>();
            leaders.ids().forEach(id -> names.add(Component.literal(model.formationName(id))));
            lead = SquadBoardText.t(SquadBoardText.VOTE_LEAD_TIE, SquadBoardText.joined(names),
                    leaders.votes());
        } else {
            lead = SquadBoardText.t(SquadBoardText.VOTE_LEAD_ONE,
                    model.formationName(leaders.ids().get(0)), leaders.votes());
        }
        Component factionName = faction.displayName().isBlank()
                ? SquadLabels.factionName(battle) : Component.literal(faction.displayName());
        return new Data(true, open, catalog.voteChangeAllowed(), factionName,
                model.totalVotes(), Math.max(faction.population(), model.totalVotes()), mine,
                candidates, lead);
    }

    private static Component vehicles(FormationSelectionView formation) {
        List<FormationDetailView.Vehicle> vehicles = formation.detail().vehicles();
        if (vehicles.isEmpty()) {
            return SquadBoardText.t(SquadBoardText.VOTE_NO_VEHICLES);
        }
        List<Component> parts = new ArrayList<>();
        for (FormationDetailView.Vehicle vehicle : vehicles) {
            parts.add(vehicle.count() > 1
                    ? SquadBoardText.t(SquadBoardText.VOTE_VEHICLE, vehicle.displayName(),
                    vehicle.count())
                    : Component.literal(vehicle.displayName()));
        }
        return SquadBoardText.joined(parts);
    }

    // ---- options ----------------------------------------------------------------------------------

    /** A block after the well: the match flow, or a titled rules list. */
    record After(boolean flow, Component title, List<SquadBoardBlocks.Rule> rules) {
        static After flowBlock() {
            return new After(true, null, List.of());
        }

        static After rules(Component title, List<SquadBoardBlocks.Rule> rules) {
            return new After(false, title, rules);
        }
    }

    /**
     * How a page uses the block.
     *
     * @param title      panel title ("编制投票", "兵种名额", "己方部署点")
     * @param meta       panel meta, or {@code null} for the phase ("进行中" / "未开启")
     * @param blockTitle variants of the call to action's title, or {@code null} for the vote's
     * @param hint       variants of the rule text, or {@code null} for the vote's
     * @param icon       icon of the call to action, or {@code null} for the phase's
     * @param info       whether "你的票 / 截止" follow the well
     * @param after      explanation blocks in order ({@code null}: flow, then how to vote)
     */
    record Options(String regionId, Component title, Component meta, List<Component> blockTitle,
                   List<Component> hint, TacticalIcon icon, boolean info, List<After> after) {
    }

    /** "怎么投票" for the data's phase. */
    static After howToVote(Data data) {
        List<SquadBoardBlocks.Rule> rules = new ArrayList<>();
        if (!data.open()) {
            rules.add(SquadBoardBlocks.Rule.of(SquadBoardText.all(
                    SquadBoardText.RULE_HOW_WAIT_VOTE)));
            rules.add(SquadBoardBlocks.Rule.of(SquadBoardText.all(
                    SquadBoardText.RULE_HOW_WAIT_CANDIDATES)));
            rules.add(SquadBoardBlocks.Rule.of(SquadBoardText.all(SquadBoardText.RULE_HOW_OWN)));
        } else {
            rules.add(SquadBoardBlocks.Rule.of(SquadBoardText.all(SquadBoardText.RULE_HOW_VOTE)));
            rules.add(SquadBoardBlocks.Rule.of(SquadBoardText.all(data.changeAllowed()
                    ? SquadBoardText.RULE_HOW_CHANGE : SquadBoardText.RULE_HOW_FINAL)));
            rules.add(SquadBoardBlocks.Rule.of(SquadBoardText.all(SquadBoardText.RULE_HOW_OWN)));
            rules.add(SquadBoardBlocks.Rule.of(SquadBoardText.all(SquadBoardText.RULE_HOW_SHARED)));
        }
        return After.rules(SquadBoardText.t(SquadBoardText.RULES_HOW), rules);
    }

    /** The match flow from joining to deploying, with the faction's current step. */
    static List<SquadBoardBlocks.Step> flowSteps(Data data) {
        List<SquadBoardBlocks.Step> steps = new ArrayList<>();
        steps.add(new SquadBoardBlocks.Step(SquadBoardText.t(SquadBoardText.FLOW_JOIN),
                List.of(data.faction()), SquadBoardBlocks.StepState.DONE));
        steps.add(new SquadBoardBlocks.Step(SquadBoardText.t(SquadBoardText.FLOW_OPEN),
                List.of(SquadBoardText.t(data.open() ? SquadBoardText.FLOW_OPEN_DONE
                        : SquadBoardText.FLOW_OPEN_WAIT)), data.open()
                ? SquadBoardBlocks.StepState.DONE : SquadBoardBlocks.StepState.CURRENT));
        steps.add(new SquadBoardBlocks.Step(SquadBoardText.t(SquadBoardText.FLOW_VOTE),
                List.of(data.open() ? SquadBoardText.t(SquadBoardText.FLOW_VOTE_OPEN, data.voted(),
                        data.total()) : SquadBoardText.t(SquadBoardText.FLOW_VOTE_WAIT)),
                data.open() ? SquadBoardBlocks.StepState.CURRENT
                        : SquadBoardBlocks.StepState.LATER));
        steps.add(new SquadBoardBlocks.Step(SquadBoardText.t(SquadBoardText.FLOW_LOCK),
                SquadBoardText.all(SquadBoardText.FLOW_LOCK_VALUE),
                SquadBoardBlocks.StepState.LATER));
        steps.add(new SquadBoardBlocks.Step(SquadBoardText.t(SquadBoardText.FLOW_AFTER),
                SquadBoardText.all(SquadBoardText.FLOW_AFTER_VALUE),
                SquadBoardBlocks.StepState.LATER));
        steps.add(new SquadBoardBlocks.Step(SquadBoardText.t(SquadBoardText.FLOW_SQUAD),
                SquadBoardText.all(SquadBoardText.FLOW_SQUAD_VALUE),
                SquadBoardBlocks.StepState.LATER));
        steps.add(new SquadBoardBlocks.Step(SquadBoardText.t(SquadBoardText.FLOW_DEPLOY),
                SquadBoardText.all(SquadBoardText.FLOW_DEPLOY_VALUE),
                SquadBoardBlocks.StepState.LATER));
        return steps;
    }

    /**
     * "你的票", "截止" and "领先"/"可用编制" key-value rows (also "我的状态"). The value keeps its
     * {@link SquadBoardBlocks.Ink}, so rows planned by {@link #layout} in {@code init()} are still
     * drawn in the frame's livery.
     */
    record Info(Component key, Component value, SquadBoardBlocks.Ink ink) {
        Info {
            ink = ink == null ? SquadBoardBlocks.Ink.TEXT : ink;
        }

        /** The value colour in the palette active now; call while drawing. */
        int color() {
            return ink.color();
        }
    }

    static Info mineInfo(Data data) {
        return new Info(SquadBoardText.t(SquadBoardText.VOTE_MINE),
                !data.open() ? SquadBoardText.t(SquadBoardText.VOTE_MINE_CLOSED)
                        : data.mine() != null ? data.mine()
                        : SquadBoardText.t(SquadBoardText.VOTE_MINE_NONE),
                data.open() ? SquadBoardBlocks.Ink.TEXT : SquadBoardBlocks.Ink.MUTED);
    }

    static Info dueInfo(Data data) {
        return new Info(SquadBoardText.t(SquadBoardText.VOTE_DUE),
                SquadBoardText.t(data.open() ? SquadBoardText.VOTE_DUE_OPEN
                        : SquadBoardText.VOTE_DUE_WAIT), SquadBoardBlocks.Ink.MUTED);
    }

    static Info extraInfo(Data data) {
        return data.open()
                ? new Info(SquadBoardText.t(SquadBoardText.VOTE_LEADING), data.lead(),
                SquadBoardBlocks.Ink.TEXT)
                : new Info(SquadBoardText.t(SquadBoardText.VOTE_AVAILABLE),
                SquadBoardText.t(SquadBoardText.VOTE_AVAILABLE_VALUE, data.candidates().size()),
                SquadBoardBlocks.Ink.TEXT);
    }

    /** The shortcut key's label: open the ballot, or (not open yet) view the candidates. */
    static Component buttonLabel(Data data) {
        return SquadBoardText.t(data.open() || !data.synced() ? SquadBoardText.VOTE_OPEN
                : SquadBoardText.VOTE_VIEW);
    }

    static TacticalIcon buttonIcon(Data data) {
        return data.open() || !data.synced() ? TacticalIcon.FLAG : TacticalIcon.EYE;
    }

    // ---- layout -----------------------------------------------------------------------------------

    /** The call to action: the empty-state box, its chosen hint and the key below it. */
    record Block(UiRect box, Component title, String hint, int hintLines, UiRect button,
                 TacticalIcon icon) {
    }

    /** One placed explanation block. */
    record PlacedAfter(UiRect bounds, SquadBoardBlocks.FlowFit flow,
                       SquadBoardBlocks.RulesFit rules) {
    }

    /** Planned geometry of the whole panel. */
    record Layout(Options options, UiRect panel, UiRect content, UiRect well, Block block,
                  UiRect tally, int tallyRowHeight, List<Info> info, int infoTop,
                  List<PlacedAfter> after, Component meta) {
    }

    private static int tallyRowHeight(TacticalShellLayout.Metrics metrics) {
        return metrics.tight() ? 13 : metrics.roomy() ? 18 : 15;
    }

    private static int boxWidth(int regionWidth) {
        return Math.max(60, Math.min(regionWidth - 8, 300));
    }

    private static Component blockTitle(Font font, Options options, Data data, int width) {
        List<Component> variants = options.blockTitle();
        if (variants == null) {
            variants = !data.synced() ? List.of(SquadBoardText.t(SquadBoardText.VOTE_SYNC))
                    : data.open() ? SquadBoardText.all(SquadBoardText.VOTE_BLOCK_OPEN_ALL,
                    data.voted(), data.total(), data.faction())
                    : SquadBoardText.all(SquadBoardText.VOTE_BLOCK_WAIT_ALL, data.faction());
        }
        return FormationDetailPanel.pick(font, variants, width);
    }

    private static List<Component> hint(Options options, Data data) {
        if (!data.synced()) {
            return List.of(SquadBoardText.t(SquadBoardText.VOTE_SYNC_HINT));
        }
        if (options.hint() != null) {
            return options.hint();
        }
        return SquadBoardText.all(data.open() ? SquadBoardText.VOTE_HINT_OPEN
                : SquadBoardText.VOTE_HINT_WAIT);
    }

    private static int blockHeight(Font font, TacticalShellLayout.Metrics metrics, UiRect region,
                                   List<Component> hint) {
        int boxWidth = boxWidth(region.width());
        int lines = SquadBoardBlocks.lines(font, hint, boxWidth - 12, 0).size();
        return 12 + 10 + lines * 10 + 4 + metrics.buttonHeight();
    }

    private static Block block(Font font, TacticalShellLayout.Metrics metrics, UiRect region,
                               Options options, Data data, Integer top) {
        int boxWidth = boxWidth(region.width());
        List<Component> hint = hint(options, data);
        int fixed = 12 + 10 + 4 + metrics.buttonHeight();
        int maxLines = Math.max(0, (region.height() - 8 - fixed) / 10);
        TextFit.Wrapped wrapped = maxLines < 1 ? new TextFit.Wrapped("", List.of())
                : TextFit.wrapBest(font, hint, boxWidth - 12, maxLines);
        int lines = wrapped.lines().size();
        int contentHeight = 12 + 10 + lines * 10;
        int blockHeight = contentHeight + 4 + metrics.buttonHeight();
        int y0 = top != null ? top : region.top() + Math.max(4, (region.height() - blockHeight) / 2);
        int left = region.left() + (region.width() - boxWidth) / 2;
        UiRect box = new UiRect(left, y0 - 4, left + boxWidth, y0 + contentHeight + 4);
        Component label = buttonLabel(data);
        int buttonWidth = Math.min(boxWidth, font.width(label) + 11 + 24);
        int buttonLeft = region.left() + (region.width() - buttonWidth) / 2;
        int buttonTop = y0 + contentHeight + 4;
        TacticalIcon icon = options.icon() != null ? options.icon()
                : !data.synced() ? TacticalIcon.REFRESH
                : data.open() ? TacticalIcon.FLAG : TacticalIcon.CLOCK;
        return new Block(box, blockTitle(font, options, data, boxWidth - 12),
                wrapped.text(), lines,
                new UiRect(buttonLeft, buttonTop, buttonLeft + buttonWidth,
                        buttonTop + metrics.buttonHeight()), icon);
    }

    static Component defaultMeta(Data data) {
        if (!data.synced()) {
            return Component.empty();
        }
        return data.open() ? SquadBoardText.t(data.changeAllowed()
                ? SquadBoardText.VOTE_META_CHANGE : SquadBoardText.VOTE_META_RUNNING)
                : SquadBoardText.t(SquadBoardText.VOTE_META_WAIT_SHORT);
    }

    /** Plans the panel in {@code panel}. */
    static Layout layout(Font font, TacticalShellLayout.Metrics metrics, UiRect panel,
                         Options options, Data data) {
        Component meta = options.meta() != null ? options.meta() : defaultMeta(data);
        UiRect content = TacticalDraw.panelContent(panel, metrics,
                TacticalDraw.PanelStyle.titled(options.title()));
        int kv = metrics.roomy() ? 13 : 11;
        int gapH = metrics.roomy() ? 14 : 8;
        int pad = metrics.roomy() ? 8 : 6;
        int count = data.candidates().size();
        int rowHeight = tallyRowHeight(metrics);
        List<Info> hugInfo = !options.info() || !data.synced() ? List.of()
                : List.of(mineInfo(data), dueInfo(data));
        int infoHeight = hugInfo.isEmpty() ? 0 : hugInfo.size() * kv + metrics.gap();
        List<After> after = options.after() == null
                ? List.of(After.flowBlock(), howToVote(data)) : options.after();
        if (!data.synced()) {
            after = List.of();
        }
        // 1) Tall panel: the well hugs the call to action and the tally, blocks follow.
        int[] heights = metrics.roomy() ? new int[]{TALLY_TALL, TALLY_TWO, rowHeight}
                : new int[]{rowHeight};
        for (int tallyHeight : after.isEmpty() ? new int[0] : heights) {
            int listHeight = 11 + count * tallyHeight;
            int wellHeight = pad + blockHeight(font, metrics, content, hint(options, data)) + gapH
                    + listHeight + pad;
            int room = content.height() - wellHeight - infoHeight - metrics.gap();
            List<Object> fits = new ArrayList<>();
            List<After> chosen = new ArrayList<>();
            for (After candidate : after) {
                int height = room - (chosen.isEmpty() ? 0 : metrics.gap());
                Object fit = height <= 0 ? null : candidate.flow()
                        ? SquadBoardBlocks.fitFlow(metrics, flowSteps(data).size(), height)
                        : SquadBoardBlocks.fitRules(font, metrics, content.width(),
                        candidate.title(), candidate.rules(), height, true);
                if (fit != null) {
                    chosen.add(candidate);
                    fits.add(fit);
                    room = height - (fit instanceof SquadBoardBlocks.FlowFit flow ? flow.height()
                            : ((SquadBoardBlocks.RulesFit) fit).height());
                }
            }
            if (chosen.isEmpty()) {
                continue;
            }
            UiRect well = new UiRect(content.left(), content.top(), content.right(),
                    content.top() + wellHeight);
            Block block = block(font, metrics, well, options, data, well.top() + pad);
            int listWidth = Math.min(well.width() - 16, tallyHeight > rowHeight ? 560 : 340);
            int listLeft = well.left() + (well.width() - listWidth) / 2;
            int listTop = well.bottom() - pad - listHeight;
            UiRect tally = new UiRect(listLeft, listTop, listLeft + listWidth,
                    listTop + listHeight);
            int y = well.bottom();
            int infoTop = y + metrics.gap();
            if (!hugInfo.isEmpty()) {
                y += metrics.gap() + hugInfo.size() * kv;
            }
            List<PlacedAfter> placed = new ArrayList<>();
            for (Object fit : fits) {
                y += metrics.gap();
                if (fit instanceof SquadBoardBlocks.FlowFit flow) {
                    placed.add(new PlacedAfter(new UiRect(content.left(), y, content.right(),
                            y + flow.height()), flow, null));
                    y += flow.height();
                } else {
                    SquadBoardBlocks.RulesFit rules = (SquadBoardBlocks.RulesFit) fit;
                    placed.add(new PlacedAfter(new UiRect(content.left(), y, content.right(),
                            y + rules.height()), null, rules));
                    y += rules.height();
                }
            }
            return new Layout(options, panel, content, well, block, tally, tallyHeight,
                    hugInfo, infoTop, placed, meta);
        }
        // 2) Otherwise the well takes the panel and centres its content; info lines under it.
        int listHeight = 11 + count * rowHeight;
        UiRect withListWell = new UiRect(content.left(), content.top(), content.right(),
                content.bottom() - 2 * kv - metrics.gap());
        boolean withList = data.synced() && count > 0
                && blockHeight(font, metrics, withListWell, hint(options, data)) + gapH
                + listHeight + 12 <= withListWell.height();
        List<Info> info = new ArrayList<>();
        if (options.info() && data.synced()) {
            info.add(mineInfo(data));
            if (!withList) {
                info.add(extraInfo(data));
            }
            info.add(dueInfo(data));
        }
        int blockMin = 12 + 10 + 10 + 4 + metrics.buttonHeight() + 8;
        while (!info.isEmpty() && content.height() - info.size() * kv - metrics.gap() < blockMin) {
            info.remove(info.size() == 3 ? 1 : info.size() - 1);
        }
        UiRect well = new UiRect(content.left(), content.top(), content.right(),
                info.isEmpty() ? content.bottom()
                        : content.bottom() - info.size() * kv - metrics.gap());
        Block block;
        UiRect tally = UiRect.EMPTY;
        if (withList) {
            int blockHeight = blockHeight(font, metrics, well, hint(options, data));
            int total = blockHeight + gapH + listHeight;
            int top = well.top() + Math.max(6, (well.height() - total) / 2);
            block = block(font, metrics, well, options, data, top);
            int listWidth = Math.min(well.width() - 16, 340);
            int listLeft = well.left() + (well.width() - listWidth) / 2;
            int listTop = top + blockHeight + gapH;
            tally = new UiRect(listLeft, listTop, listLeft + listWidth, listTop + listHeight);
        } else {
            block = block(font, metrics, well, options, data, null);
        }
        return new Layout(options, panel, content, well, block, tally, rowHeight, List.copyOf(info),
                well.bottom() + metrics.gap(), List.of(), meta);
    }

    // ---- drawing ----------------------------------------------------------------------------------

    static void render(GuiGraphics graphics, Font font, TacticalShellLayout.Metrics metrics,
                       Layout layout, Data data) {
        SquadBoardBlocks.region(graphics, layout.options().regionId(), layout.panel());
        TacticalDraw.panel(graphics, font, layout.panel(), metrics,
                TacticalDraw.PanelStyle.titled(layout.options().title())
                        .withMeta(layout.meta()));
        UiRect well = layout.well();
        SquadBoardBlocks.subRegion(graphics, layout.options().regionId() + ".well", well);
        TacticalDraw.well(graphics, well);
        Block block = layout.block();
        TacticalDraw.empty(graphics, font, block.box(), block.icon(), block.title(),
                Component.literal(block.hint()), false);
        if (!layout.tally().isEmpty()) {
            tally(graphics, font, metrics, layout.tally(), layout.tallyRowHeight(), data);
        }
        SquadBoardBlocks.endRegion(graphics);
        int kv = metrics.roomy() ? 13 : 11;
        UiRect content = layout.content();
        for (int index = 0; index < layout.info().size(); index++) {
            Info info = layout.info().get(index);
            TacticalDraw.kv(graphics, font, content.left(), layout.infoTop() + index * kv,
                    content.width(), info.key(), info.value(), info.color());
        }
        for (PlacedAfter after : layout.after()) {
            if (after.flow() != null) {
                SquadBoardBlocks.flow(graphics, font, after.bounds(), metrics, flowSteps(data),
                        after.flow());
            } else {
                SquadBoardBlocks.rules(graphics, font, after.bounds(), metrics, after.rules());
            }
        }
        SquadBoardBlocks.endRegion(graphics);
    }

    private static void tally(GuiGraphics graphics, Font font, TacticalShellLayout.Metrics metrics,
                              UiRect bounds, int rowHeight, Data data) {
        SquadBoardBlocks.subRegion(graphics, "squad.vote.tally", bounds);
        int count = data.candidates().size();
        List<Component> head = data.open()
                ? List.of(SquadBoardText.t(SquadBoardText.VOTE_TALLY_OPEN, count),
                SquadBoardText.t(SquadBoardText.VOTE_TALLY_OPEN_SHORT, count))
                : List.of(SquadBoardText.t(SquadBoardText.VOTE_TALLY_WAIT, count),
                SquadBoardText.t(SquadBoardText.VOTE_TALLY_WAIT_SHORT, count));
        SquadBoardBlocks.fitted(graphics, font, head, bounds.left(), bounds.top(), bounds.width(),
                TacticalBoardTheme.LIGHT_MUTED, TextFit.Align.LEFT);
        int lines = rowHeight >= TALLY_TALL ? 3 : rowHeight >= TALLY_TWO ? 2 : 1;
        int max = 1;
        for (Candidate candidate : data.candidates()) {
            max = Math.max(max, candidate.votes());
        }
        for (int index = 0; index < count; index++) {
            Candidate candidate = data.candidates().get(index);
            int top = bounds.top() + 11 + index * rowHeight;
            UiRect row = new UiRect(bounds.left(), top, bounds.right(), top + rowHeight);
            TacticalDraw.rowBg(graphics, row, new TacticalDraw.RowState(false, false, true,
                    index % 2 == 1), 0);
            Component right = data.open()
                    ? data.voted() > 0 ? SquadBoardText.t(SquadBoardText.VOTE_VOTES_SHARE,
                    candidate.votes(), Math.round(candidate.votes() * 100.0F / data.voted()))
                    : SquadBoardText.t(SquadBoardText.VOTE_VOTES, candidate.votes())
                    : lines == 1 ? candidate.category() : Component.empty();
            int rightWidth = font.width(right);
            int y = lines > 1 ? row.top() + 4
                    : row.top() + (rowHeight - (data.open() ? 2 : 0) - 8) / 2;
            // The full name wins: first the "领先" tag gives way (the leader keeps its bright
            // name), then the share ("4 票 · 50%" → "4 票").
            int nameNeeds = font.width(candidate.name());
            int nameRoom = row.width() - 10 - (rightWidth > 0 ? rightWidth + 8 : 0);
            if (nameNeeds > nameRoom && data.open() && data.voted() > 0) {
                right = SquadBoardText.t(SquadBoardText.VOTE_VOTES, candidate.votes());
                rightWidth = font.width(right);
                nameRoom = row.width() - 10 - rightWidth - 8;
            }
            Component tag = SquadBoardText.t(SquadBoardText.VOTE_LEADING);
            boolean showTag = candidate.leading()
                    && nameNeeds + font.width(tag) + 5 <= nameRoom;
            int tagWidth = showTag ? font.width(tag) + 5 : 0;
            int room = nameRoom - tagWidth;
            int nameColor = candidate.leading() ? TacticalBoardTheme.LIGHT
                    : TacticalBoardTheme.LIGHT_MUTED;
            int nameWidth = TextFit.draw(graphics, font, candidate.name(), row.left() + 5, y,
                    Math.max(0, room), nameColor, TextFit.Align.LEFT).width();
            if (showTag) {
                TextFit.draw(graphics, font, tag, row.left() + 5 + nameWidth + 5, y,
                        tagWidth - 5, TacticalBoardTheme.NEUTRAL_B, TextFit.Align.LEFT);
            }
            if (rightWidth > 0) {
                TextFit.draw(graphics, font, right, row.right() - 5 - rightWidth, y, rightWidth,
                        nameColor, TextFit.Align.LEFT);
            }
            if (lines >= 2) {
                TextFit.draw(graphics, font, SquadBoardText.t(SquadBoardText.VOTE_VEHICLES,
                                candidate.category(), candidate.vehicles()), row.left() + 5, y + 11,
                        row.width() - 10, TacticalBoardTheme.LIGHT_MUTED, TextFit.Align.LEFT);
            }
            if (lines >= 3 && !candidate.description().isBlank()) {
                TextFit.draw(graphics, font, candidate.description(), row.left() + 5, y + 22,
                        row.width() - 10, TacticalBoardTheme.LIGHT_MUTED, TextFit.Align.LEFT);
            }
            if (data.open()) {
                graphics.fill(row.left() + 5, row.bottom() - 2, row.right() - 5,
                        row.bottom() - 1, TacticalBoardTheme.HUD_TRACK);
                int fill = Math.round((row.width() - 10) * candidate.votes()
                        / (float) Math.max(1, data.voted()));
                graphics.fill(row.left() + 5, row.bottom() - 2, row.left() + 5 + fill,
                        row.bottom() - 1, TacticalBoardTheme.NEUTRAL_B);
            }
        }
        UiLayoutProbe.end(graphics);
    }
}
