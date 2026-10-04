package com.wok.infantry.client.screen;

import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.formation.FormationCategory;
import com.wok.infantry.formation.selection.FactionSelectionView;
import com.wok.infantry.formation.selection.FormationDetailView;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.formation.selection.FormationSelectionView;
import com.wok.infantry.formation.selection.FormationSupportLabel;
import com.wok.infantry.formation.vote.FormationVotePhase;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Every text of the formation vote page in one place. All of it comes from language keys
 * ({@code screen.wok_infantry.formation.*}); only server-authored names (factions, formations,
 * classes, vehicles, squads, unavailable reasons) are inserted as they are. Methods returning a
 * list give the variants from long to short, the caller draws the first that fits.
 */
public final class FormationText {
    static final String PREFIX = "screen.wok_infantry.formation.";

    private FormationText() {
    }

    static MutableComponent key(String suffix, Object... args) {
        return Component.translatable(PREFIX + suffix, args);
    }

    // ---- shell ----------------------------------------------------------------------------------

    public static Component title(int width) {
        return key(width < 400 ? "title_short" : "title");
    }

    /** Header identity: "未加入阵营", or the public faction · squad · role. */
    public static Component identity(FormationVoteModel model, BattleSnapshot battle) {
        FactionSelectionView faction = model.joinedFaction();
        if (faction == null) {
            return key("identity.unjoined");
        }
        MutableComponent identity = Component.literal(faction.displayName());
        if (battle != null && battle.ownSquad() != null) {
            identity.append(" · ").append(SquadScreen.callsign(battle.ownSquad()));
            identity.append(" · ").append(Component.translatable(battle.commander()
                    ? "role.wok_infantry.commander" : battle.squadLeader()
                    ? "role.wok_infantry.squad_leader" : "role.wok_infantry.member"));
        } else {
            identity.append(" · ").append(key("identity.no_squad"));
        }
        return identity;
    }

    /** Reason shown on the greyed battle-terminal tabs before the formation is locked. */
    public static Component tabLockedReason() {
        return key("tab_locked");
    }

    // ---- step guide -------------------------------------------------------------------------

    /** Step-by-step instruction (footer guide), long to short. */
    public static List<Component> step(FormationVoteModel model) {
        FactionSelectionView browsing = model.browsing();
        String faction = browsing == null ? "" : browsing.displayName();
        return switch (model.step()) {
            case SYNC -> List.of(key("step.sync"));
            case EMPTY -> List.of(key("step.empty"), key("step.empty_short"));
            case JOIN -> List.of(key("step.join", faction), key("step.join_short"));
            case JOIN_LOCKED -> List.of(key("step.join_locked", faction),
                    key("step.join_short"));
            case BROWSE_OTHER -> List.of(key("step.browse_other", faction),
                    key("step.browse_other_short"));
            case NO_JOINABLE -> List.of(key("step.no_joinable"), key("step.no_joinable_short"));
            case WAIT_OPEN -> List.of(key("step.wait_open"), key("step.wait_open_short"));
            case ADMIN_OPEN -> List.of(key("step.admin_open"), key("step.admin_open_short"));
            case VOTE -> List.of(key("step.vote"), key("step.vote_short"));
            case WAIT_LOCK -> List.of(key("step.wait_lock"), key("step.wait_lock_short"));
            case ADMIN_LOCK -> List.of(key("step.admin_lock"), key("step.admin_lock_short"));
            case DEPLOY -> List.of(key("step.deploy", lockedName(model)),
                    key("step.deploy_short"));
            case LOCKED_NO_FORMATION -> List.of(key("step.no_formation"),
                    key("step.no_formation_short"));
        };
    }

    /**
     * Status line of the joined faction (LED + main + muted sub): gray before the vote, orange
     * while voting, green once locked. Pairs are long to short.
     */
    public static List<Component[]> joinedStatus(FormationVoteModel model) {
        FactionSelectionView own = model.joinedFaction();
        if (own == null) {
            return List.of();
        }
        String tally = model.totalVotes() + "/" + own.population();
        return switch (model.snapshot().votePhase()) {
            case OPEN -> List.of(
                    new Component[]{key("status.open"), key(model.snapshot().voteChangeAllowed()
                            ? "status.open_sub_change" : "status.open_sub_fixed", tally)},
                    new Component[]{key("status.open_short"), key("status.open_sub_short", tally)});
            case LOCKED -> List.of(
                    new Component[]{key("status.locked"), key("status.locked_sub",
                            lockedName(model))},
                    new Component[]{key("status.locked_short"), Component.literal(
                            lockedName(model))});
            case NOT_STARTED -> List.of(
                    new Component[]{key("status.pending"), key("status.pending_sub")},
                    new Component[]{key("status.pending_short"), key("status.pending_sub_short")});
        };
    }

    /** LED colour of the joined status line. */
    public static int statusLed(FormationVoteModel model) {
        if (!model.joined()) {
            return TacticalBoardTheme.MUTED;
        }
        return switch (model.snapshot().votePhase()) {
            case OPEN -> TacticalBoardTheme.ACCENT;
            case LOCKED -> TacticalBoardTheme.SUCCESS;
            case NOT_STARTED -> TacticalBoardTheme.MUTED;
        };
    }

    /** Note next to the join key (not joined), long to short. */
    public static List<Component> joinNote(FormationVoteModel model) {
        FormationVoteModel.JoinAction join = model.joinAction();
        if (!join.enabled()) {
            return List.of(key("note.full"), key("note.full_short"));
        }
        if (join.locked()) {
            return List.of(key("note.locked", lockedName(model)), key("note.locked_short"));
        }
        return List.of(key("note.join"), key("note.join_short"));
    }

    /** Join key label, long to short. */
    public static List<Component> joinLabel(FormationVoteModel model) {
        FactionSelectionView faction = model.browsing();
        FormationVoteModel.JoinAction join = model.joinAction();
        String name = faction == null ? "" : faction.displayName();
        if (!join.enabled()) {
            return switch (join.block()) {
                case FACTION_FULL, LOCKED_FULL -> List.of(key("join.full", name,
                        join.population(), join.capacity()), key("join.full_short", name));
                default -> List.of(key("join.unavailable", name), key("join.unavailable_short"));
            };
        }
        if (join.locked()) {
            return List.of(key("join.locked", name, lockedName(model)),
                    key("join.locked_short", name), key("join", name));
        }
        return List.of(key("join.fixed", name), key("join", name));
    }

    // ---- faction keys --------------------------------------------------------------------------

    public static Component factionBadge(FormationVoteModel model, FactionSelectionView faction) {
        return key("faction.population", faction.population(),
                model.effectiveCapacity(faction));
    }

    /** Hover text of a faction key. */
    public static Component factionTooltip(FormationVoteModel model, FactionSelectionView faction) {
        FactionSelectionView own = model.joinedFaction();
        MutableComponent text;
        if (own != null && !own.id().equals(faction.id())) {
            text = key("faction.tooltip_other", faction.displayName(), faction.population(),
                    model.effectiveCapacity(faction));
            text.append("\n").append(key("faction.tooltip_fixed", own.displayName()));
            return text;
        }
        text = key("faction.tooltip", faction.displayName(), faction.population(),
                model.effectiveCapacity(faction));
        if (!faction.description().isBlank()) {
            text.append("\n").append(faction.description());
        }
        if (own == null) {
            text.append("\n").append(key("faction.tooltip_browse"));
        }
        return text;
    }

    // ---- list ------------------------------------------------------------------------------------

    /** Title and meta of the list panel. */
    public static Component listTitle(FormationVoteModel model, boolean narrow) {
        FactionSelectionView faction = model.browsing();
        return narrow && faction != null ? key("panel.list_narrow", faction.displayName())
                : key("panel.list");
    }

    public static Component listMeta(FormationVoteModel model, boolean narrow) {
        FactionSelectionView faction = model.browsing();
        if (faction == null) {
            return Component.empty();
        }
        if (!narrow) {
            return key("panel.list_meta", faction.displayName(), faction.formations().size());
        }
        if (!model.joined()) {
            if (!model.joinAction().enabled()) {
                return key("meta.unjoined_full");
            }
            return key(model.phase(faction) == FormationVotePhase.LOCKED
                    ? "meta.unjoined_locked" : "meta.unjoined");
        }
        return switch (model.snapshot().votePhase()) {
            case OPEN -> key("meta.open");
            case LOCKED -> key("meta.locked");
            case NOT_STARTED -> key("meta.pending");
        };
    }

    /** Group title of a category: translated for the built-in categories. */
    public static Component category(FormationSelectionView formation) {
        return FormationCategory.byId(formation.categoryId())
                .<Component>map(category -> key("category." + category.id()))
                .orElseGet(() -> Component.literal(formation.categoryDisplayName()));
    }

    /** 9×9 glyph of a category (used when a formation has no emblem). */
    public static TacticalIcon categoryIcon(String categoryId) {
        return switch (FormationCategory.byId(categoryId).orElse(FormationCategory.INFANTRY)) {
            case INFANTRY -> TacticalIcon.SQUAD;
            case ARMORED -> TacticalIcon.TANK;
            case MOTORIZED -> TacticalIcon.BOX;
            case MECHANIZED -> TacticalIcon.SHIELD;
            case SPECIAL -> TacticalIcon.CROSSHAIR;
        };
    }

    /** Short status mark at the right of a row, or {@code null}. */
    public static Component rowMark(FormationVoteModel.RowStatus status, boolean twoLines) {
        return switch (status.mark()) {
            case DISABLED -> key("row.mark.disabled");
            case SHORTFALL -> key("row.mark.shortfall");
            case LOCKED -> key("row.mark.locked");
            case NOT_CHOSEN -> key("row.mark.not_chosen");
            case NONE -> !twoLines && status.showVotes() ? key("row.votes_short", status.votes())
                    : null;
        };
    }

    /** Second line of a row (rows of 20px and more). */
    public static Component rowSub(FormationVoteModel model, FormationSelectionView formation,
                                   FormationVoteModel.RowStatus status) {
        FactionSelectionView faction = model.browsing();
        int population = faction == null ? 0 : faction.population();
        return switch (status.mark()) {
            case DISABLED -> Component.literal(formation.unavailableReason().isBlank()
                    ? I18n.get(PREFIX + "reason.unavailable") : formation.unavailableReason());
            case SHORTFALL -> key("row.shortfall", formation.capacity(), population);
            case LOCKED -> status.showVotes()
                    ? key("row.locked_votes", population, status.votes())
                    : key("row.locked", population);
            case NOT_CHOSEN -> key("row.not_chosen", lockedName(model));
            case NONE -> status.showVotes() ? key("row.votes", status.votes(), status.percent())
                    : key("row.capacity", formation.capacity());
        };
    }

    public static Component mine() {
        return key("row.mine");
    }

    // ---- summary ---------------------------------------------------------------------------------

    /** One line of the summary: key/value (null key = muted paragraph, "" key = full line). */
    public record SummaryRow(Component key, Component value, int color) {
    }

    public static Component summaryTitle(FormationVoteModel model) {
        return key(model.joined() ? "summary.vote" : "summary.faction");
    }

    public static List<SummaryRow> summary(FormationVoteModel model) {
        FactionSelectionView faction = model.browsing();
        if (faction == null) {
            return List.of();
        }
        long candidates = faction.formations().stream()
                .filter(FormationSelectionView::available).count();
        int capacity = model.effectiveCapacity(faction);
        if (!model.joined()) {
            FormationVoteModel.JoinAction join = model.joinAction();
            SummaryRow population = new SummaryRow(key("summary.population"), key(join.enabled()
                    ? "summary.population_value" : "summary.population_full",
                    faction.population(), capacity), 0);
            if (!join.enabled()) {
                return List.of(population,
                        new SummaryRow(key("summary.candidates"),
                                key("summary.candidates_count", candidates), 0),
                        new SummaryRow(null, key("summary.note_full"), 0));
            }
            if (join.locked()) {
                return List.of(population,
                        new SummaryRow(key("summary.locked_formation"),
                                Component.literal(lockedName(model)), TacticalBoardTheme.SUCCESS),
                        new SummaryRow(null, key("summary.note_locked", lockedName(model)), 0));
            }
            return List.of(population,
                    new SummaryRow(key("summary.candidates"),
                            key("summary.candidates_value", candidates), 0),
                    new SummaryRow(null, key("summary.note_unjoined"), 0));
        }
        FormationSelectionSnapshot snapshot = model.snapshot();
        SummaryRow deadline = new SummaryRow(key("summary.deadline"),
                key("summary.deadline_value"), 0);
        return switch (snapshot.votePhase()) {
            case NOT_STARTED -> List.of(
                    new SummaryRow(key("summary.status"), key("summary.status_not_started"), 0),
                    deadline, new SummaryRow(null, key("summary.note_pending"), 0));
            case LOCKED -> List.of(
                    new SummaryRow(key("summary.status"), key("summary.status_locked"), 0),
                    new SummaryRow(key("summary.locked_formation"),
                            Component.literal(lockedName(model)), TacticalBoardTheme.SUCCESS),
                    new SummaryRow(key("summary.users"), key("summary.users_value",
                            faction.population()), 0),
                    new SummaryRow(key("summary.my_vote"), ownVote(model), 0),
                    new SummaryRow(null, key("summary.note_locked_joined"), 0));
            case OPEN -> {
                FormationVoteModel.Leaders leaders = model.leaders();
                List<SummaryRow> rows = new java.util.ArrayList<>();
                rows.add(new SummaryRow(key("summary.voted"), key("summary.voted_value",
                        model.totalVotes(), faction.population()), 0));
                rows.add(new SummaryRow(key("summary.my_vote"), ownVote(model),
                        snapshot.ownVoteFormationId().isBlank() ? TacticalBoardTheme.MUTED
                                : TacticalBoardTheme.SUCCESS));
                if (leaders.none()) {
                    rows.add(new SummaryRow(key("summary.leader"), key("summary.no_votes"), 0));
                } else if (leaders.tie()) {
                    rows.add(new SummaryRow(key("summary.tie"), key("summary.tie_value",
                            leaders.votes()), 0));
                    rows.add(new SummaryRow(Component.empty(), Component.literal(
                            names(model, leaders.ids())), 0));
                } else {
                    rows.add(new SummaryRow(key("summary.leader"), key("summary.leader_value",
                            model.formationName(leaders.ids().get(0)), leaders.votes()), 0));
                }
                rows.add(deadline);
                rows.add(new SummaryRow(key("summary.change"), key(snapshot.voteChangeAllowed()
                        ? "summary.change_allowed" : "summary.change_denied"), 0));
                yield List.copyOf(rows);
            }
        };
    }

    private static Component ownVote(FormationVoteModel model) {
        String own = model.snapshot().ownVoteFormationId();
        return own.isBlank() ? key("summary.not_voted")
                : Component.literal(model.formationName(own));
    }

    // ---- administrator ----------------------------------------------------------------------------

    public static Component adminTitle() {
        return key("admin.title");
    }

    public static Component adminOpenKey() {
        return key("admin.open");
    }

    public static Component adminLockKey() {
        return key("admin.lock");
    }

    /** Text right of "管理员" on the first line, long to short. */
    public static List<Component> adminHeadline(FormationVoteModel model) {
        FormationVoteModel.AdminState admin = model.admin();
        if (admin.opening()) {
            return List.of(key("admin.open_hint"));
        }
        FormationSelectionView target = model.highlighted();
        if (target == null) {
            return List.of(key("admin.no_target"));
        }
        return List.of(key("admin.target", target.displayName(), model.votes(target.id())),
                key("admin.target_short", target.displayName()));
    }

    /** Second line: candidates, the capacity check or the leader comparison, long to short. */
    public static List<Component> adminDetail(FormationVoteModel model) {
        FormationVoteModel.AdminState admin = model.admin();
        if (admin.opening()) {
            return List.of(key("admin.candidates", admin.candidates()),
                    key("admin.candidates_short", admin.candidates()),
                    key("admin.candidates_tiny", admin.candidates()));
        }
        FormationSelectionView target = model.highlighted();
        FactionSelectionView own = model.joinedFaction();
        int population = own == null ? 0 : own.population();
        switch (admin.lockBlock()) {
            case SHORTFALL -> {
                return List.of(key("admin.shortfall", target.capacity(), population),
                        key("admin.shortfall_short", target.capacity(), population));
            }
            case UNAVAILABLE -> {
                return List.of(Component.literal(target.unavailableReason().isBlank()
                                ? I18n.get(PREFIX + "admin.disabled")
                                : target.unavailableReason()), key("admin.disabled"));
            }
            case NOT_CANDIDATE -> {
                return List.of(key("reason.not_candidate"));
            }
            case NO_TARGET -> {
                return List.of(key("admin.no_target"));
            }
            case NONE -> {
            }
        }
        FormationVoteModel.Leaders leaders = model.leaders();
        return switch (admin.relation()) {
            case SOLE_LEADER -> List.of(key("admin.sole_leader"));
            case NO_VOTES -> List.of(key("admin.no_votes"), key("admin.no_votes_short"));
            case TIE_INCLUDED, TIE_EXCLUDED -> List.of(
                    key("admin.tie", names(model, leaders.ids()), leaders.votes()),
                    key("admin.tie_short", leaders.ids().size(), leaders.votes()));
            case NOT_LEADER -> List.of(
                    key("admin.leader", model.formationName(leaders.ids().get(0)),
                            leaders.votes()),
                    key("admin.leader_short", leaders.votes()));
        };
    }

    // ---- vote key ------------------------------------------------------------------------------

    public static Component voteLabel(FormationVoteModel.VoteAction action) {
        return switch (action.label()) {
            case VOTE -> key("action.vote");
            case CHANGE -> key("action.change");
            case MINE -> key("action.mine");
            case DEPLOY -> key("action.deploy");
            case CANNOT_JOIN -> key("action.cannot_join");
            case USE_AFTER_JOIN -> key("action.use_after_join");
            case NOT_CHOSEN -> key("action.not_chosen");
        };
    }

    public static Component detailsKey() {
        return key("action.details");
    }

    public static Component listKey() {
        return key("action.list");
    }

    /** Text left of the vote key, long to short. */
    public static List<Component> reason(FormationVoteModel model,
                                         FormationVoteModel.VoteAction action) {
        FormationSelectionView formation = model.highlighted();
        FactionSelectionView faction = model.browsing();
        String lockedName = lockedName(model);
        int population = faction == null ? 0 : faction.population();
        return switch (action.reason()) {
            case CHANGE_ALLOWED_AFTER -> List.of(key("reason.change_allowed_after"));
            case NO_CHANGE_AFTER -> List.of(key("reason.no_change_after"));
            case CURRENT_VOTE -> List.of(key("reason.current_vote",
                    model.formationName(model.snapshot().ownVoteFormationId())));
            case MINE_CAN_CHANGE -> List.of(key("reason.mine_can_change"));
            case MINE_FIXED -> List.of(key("reason.mine_fixed"));
            case LOCKED_USES_THIS -> List.of(key("reason.locked_uses_this"),
                    key("reason.locked_uses_this_short"));
            case LOCKED_NOT_CHOSEN -> List.of(key("reason.locked_not_chosen", lockedName),
                    key("reason.locked_not_chosen_short", lockedName));
            case JOIN_FIRST -> List.of(key("reason.join_first"));
            case FACTION_FULL -> faction == null ? List.of(key("reason.faction_full_short"))
                    : List.of(key("reason.faction_full", faction.displayName(), population,
                    model.effectiveCapacity(faction)), key("reason.faction_full_short"));
            case LATE_USE_THIS -> List.of(key("reason.late_use_this"),
                    key("reason.late_use_this_short"));
            case LATE_NOT_CHOSEN -> List.of(key("reason.late_not_chosen", lockedName),
                    key("reason.late_not_chosen_short", lockedName));
            case WAIT_OPEN -> List.of(key("reason.wait_open"));
            case UNAVAILABLE -> List.of(formation == null
                    || formation.unavailableReason().isBlank() ? key("reason.unavailable")
                    : Component.literal(formation.unavailableReason()), key("reason.unavailable"));
            case SHORTFALL -> List.of(
                    key("reason.shortfall", formation.capacity(),
                            faction == null ? "" : faction.displayName(), population),
                    key("reason.shortfall_short", formation.capacity(), population));
            case NOT_CANDIDATE -> List.of(key("reason.not_candidate"));
            case NO_CHANGE -> List.of(key("reason.no_change"));
            case NO_FORMATION -> List.of(key("reason.no_formation"));
        };
    }

    // ---- detail identity -------------------------------------------------------------------------

    /** Chips right of the formation name, in drawing order. */
    public record Chip(Component text, int color) {
    }

    public static List<Chip> chips(FormationVoteModel model, FormationSelectionView formation) {
        FormationVoteModel.RowStatus status = model.rowStatus(formation);
        List<Chip> chips = new java.util.ArrayList<>();
        if (status.mark() == FormationVoteModel.RowMark.LOCKED) {
            chips.add(new Chip(key("chip.locked"), TacticalBoardTheme.SUCCESS));
        } else if (status.mine()) {
            chips.add(new Chip(key("chip.mine"), TacticalBoardTheme.SUCCESS));
        }
        switch (status.mark()) {
            case DISABLED -> chips.add(new Chip(key("chip.disabled"), TacticalBoardTheme.MUTED));
            case SHORTFALL -> chips.add(new Chip(key("chip.shortfall"), TacticalBoardTheme.MUTED));
            case NOT_CHOSEN -> chips.add(new Chip(key("chip.not_chosen"),
                    TacticalBoardTheme.MUTED));
            default -> {
            }
        }
        return chips;
    }

    /** "类别 · 阵营 · N 票（P%）". */
    public static Component identitySub(FormationVoteModel model,
                                        FormationSelectionView formation) {
        FactionSelectionView faction = model.browsing();
        String factionName = faction == null ? "" : faction.displayName();
        if (model.showVotes(faction)) {
            return key("detail.sub_votes", category(formation), factionName,
                    model.votes(formation.id()), model.percent(formation.id()));
        }
        return key("detail.sub", category(formation), factionName);
    }

    /** Capacity check line: icon, colour and text variants. */
    public record FitLine(TacticalIcon icon, int iconColor, int color, List<Component> text) {
    }

    public static FitLine fit(FormationVoteModel model, FormationSelectionView formation) {
        FactionSelectionView faction = model.browsing();
        String factionName = faction == null ? "" : faction.displayName();
        int population = faction == null ? 0 : faction.population();
        FormationVoteModel.RowStatus status = model.rowStatus(formation);
        return switch (status.mark()) {
            case LOCKED -> new FitLine(TacticalIcon.FLAG, TacticalBoardTheme.SUCCESS,
                    TacticalBoardTheme.TEXT, List.of(key("fit.locked", population),
                    key("fit.locked_short", population)));
            case NOT_CHOSEN -> new FitLine(TacticalIcon.MINUS, TacticalBoardTheme.MUTED,
                    TacticalBoardTheme.MUTED, List.of(key("fit.not_chosen", lockedName(model)),
                    key("fit.not_chosen_short")));
            case SHORTFALL -> new FitLine(TacticalIcon.LOCK, TacticalBoardTheme.MUTED,
                    TacticalBoardTheme.MUTED, List.of(key("fit.shortfall", formation.capacity(),
                    factionName, population), key("fit.shortfall_short", formation.capacity())));
            case DISABLED -> new FitLine(TacticalIcon.LOCK, TacticalBoardTheme.MUTED,
                    TacticalBoardTheme.MUTED, List.of(key("fit.disabled"),
                    key("fit.disabled_short")));
            case NONE -> new FitLine(TacticalIcon.CHECK, TacticalBoardTheme.SUCCESS,
                    TacticalBoardTheme.TEXT, List.of(key("fit.capacity", formation.capacity(),
                    factionName, population), key("fit.capacity_short", formation.capacity(),
                    population)));
        };
    }

    // ---- detail sections ---------------------------------------------------------------------------

    /** One row of a detail section: left text, optional right text, muted, indented, wrapped. */
    public record SectionRow(String left, String right, boolean muted, boolean indent,
                             boolean wrap) {
    }

    /** One detail section with its title, meta and rows. */
    public record Section(String title, String meta, List<SectionRow> rows) {
    }

    /** The four detail sections (vehicles, class quotas, squads, capabilities). */
    public static List<Section> sections(FormationSelectionSnapshot snapshot,
                                         FormationSelectionView formation) {
        FormationDetailView detail = formation.detail();
        List<Section> sections = new java.util.ArrayList<>(4);
        List<SectionRow> vehicles = new java.util.ArrayList<>();
        for (FormationDetailView.Vehicle vehicle : detail.vehicles()) {
            String name = vehicle.count() > 1
                    ? I18n.get(PREFIX + "detail.vehicle_count", vehicle.displayName(),
                    vehicle.count()) : vehicle.displayName();
            vehicles.add(new SectionRow(name, replenishment(vehicle), false, false, false));
        }
        if (vehicles.isEmpty()) {
            vehicles.add(new SectionRow(I18n.get(PREFIX + "detail.vehicles_none"), null, true,
                    false, false));
        }
        int vehicleCount = detail.vehicleCount();
        sections.add(new Section(I18n.get(PREFIX + "detail.vehicles"), vehicleCount == 0 ? ""
                : I18n.get(PREFIX + "detail.vehicles_meta", vehicleCount), vehicles));

        List<SectionRow> classes = detail.classQuotas().stream()
                .map(quota -> new SectionRow(quota.displayName(),
                        I18n.get(PREFIX + "detail.people", quota.squadLimit()), false, false,
                        false)).collect(Collectors.toList());
        if (classes.isEmpty()) {
            classes.add(new SectionRow(I18n.get(PREFIX + "detail.none"), null, true, false,
                    false));
        }
        sections.add(new Section(I18n.get(PREFIX + "detail.classes"),
                I18n.get(PREFIX + "detail.classes_meta"), classes));

        List<SectionRow> squads = detail.squads().stream()
                .map(squad -> new SectionRow(squad.displayName(),
                        I18n.get(PREFIX + "detail.people", squad.capacity()), false, false,
                        false)).collect(Collectors.toList());
        if (squads.isEmpty()) {
            squads.add(new SectionRow(I18n.get(PREFIX + "detail.none"), null, true, false,
                    false));
        }
        sections.add(new Section(I18n.get(PREFIX + "detail.squads"),
                I18n.get(PREFIX + "detail.squads_meta", detail.squads().size()), squads));

        List<SectionRow> capabilities = new java.util.ArrayList<>();
        capabilities.add(new SectionRow(I18n.get(PREFIX + "detail.outpost"),
                detail.outpostMax() == 0 ? I18n.get(PREFIX + "detail.none")
                        : I18n.get(PREFIX + "detail.outpost_max", detail.outpostMax()),
                false, false, false));
        capabilities.add(new SectionRow(I18n.get(PREFIX + "detail.rally"),
                detail.rallyMax() == 0 ? I18n.get(PREFIX + "detail.none")
                        : I18n.get(PREFIX + "detail.rally_value", detail.rallyMax()),
                false, false, false));
        capabilities.add(new SectionRow(I18n.get(PREFIX + "detail.respawn"),
                detail.respawnDelaySeconds() == FormationDetailView.INHERIT_RESPAWN
                        ? I18n.get(PREFIX + "detail.respawn_global")
                        : I18n.get(PREFIX + "detail.seconds", detail.respawnDelaySeconds()),
                false, false, false));
        if (!detail.mobileSpawnVehicles().isEmpty()) {
            capabilities.add(new SectionRow(I18n.get(PREFIX + "detail.mobile"),
                    I18n.get(PREFIX + "detail.mobile_value", detail.mobileSpawnVehicles().size()),
                    false, false, false));
            for (String vehicle : detail.mobileSpawnVehicles()) {
                capabilities.add(new SectionRow(vehicle, null, true, true, false));
            }
        }
        String supportValue = switch (detail.supportMode()) {
            case NONE -> I18n.get(PREFIX + "detail.none");
            case ALL -> I18n.get(PREFIX + "detail.support_all");
            case ALLOW_LIST -> I18n.get(PREFIX + "detail.support_count",
                    detail.supportIds().size());
        };
        capabilities.add(new SectionRow(I18n.get(PREFIX + "detail.support"), supportValue,
                false, false, false));
        for (String supportId : detail.supportIds()) {
            capabilities.add(new SectionRow(supportName(snapshot, supportId), null, true, true,
                    false));
        }
        sections.add(new Section(I18n.get(PREFIX + "detail.capabilities"), "", capabilities));
        return sections;
    }

    private static String replenishment(FormationDetailView.Vehicle vehicle) {
        if (!vehicle.replenishes()) {
            return I18n.get(PREFIX + "detail.replenish_never");
        }
        int seconds = vehicle.cooldownSeconds();
        return seconds % 60 == 0 ? I18n.get(PREFIX + "detail.replenish_minutes", seconds / 60)
                : I18n.get(PREFIX + "detail.replenish_seconds", seconds);
    }

    /**
     * Display name of a support: its translation when the client has the key (the support add-on
     * ships it), otherwise the server's fallback name, otherwise the id.
     */
    public static String supportName(FormationSelectionSnapshot snapshot, String supportId) {
        FormationSupportLabel label = snapshot == null ? null : snapshot.supportLabel(supportId);
        if (label == null) {
            return supportId;
        }
        if (!label.translationKey().isBlank() && I18n.exists(label.translationKey())) {
            return I18n.get(label.translationKey());
        }
        return label.fallbackName().isBlank() ? supportId : label.fallbackName();
    }

    /** "还有 n 项：…" line of the detail view, long to short. */
    public static List<String> moreLines(int hidden, List<String> sectionNames) {
        String names = String.join(" / ", sectionNames);
        return List.of(I18n.get(PREFIX + "detail.more_named_scroll", hidden, names),
                I18n.get(PREFIX + "detail.more_named", hidden, names),
                I18n.get(PREFIX + "detail.more_scroll", hidden),
                I18n.get(PREFIX + "detail.more", hidden));
    }

    public static String descriptionName() {
        return I18n.get(PREFIX + "detail.description");
    }

    public static Component detailTitle() {
        return key("detail.title");
    }

    /** Crumb of the narrow detail page: "阵营 › 类别". */
    public static Component crumb(FormationVoteModel model, FormationSelectionView formation) {
        FactionSelectionView faction = model.browsing();
        return key("crumb", faction == null ? "" : faction.displayName(), category(formation));
    }

    // ---- dialogs ---------------------------------------------------------------------------------

    public static Component joinConfirmTitle(FactionSelectionView faction) {
        return key("confirm.join_title", faction.displayName());
    }

    public static Component joinConfirmBody(FormationVoteModel model, FactionSelectionView faction) {
        int capacity = model.effectiveCapacity(faction);
        if (model.phase(faction) == FormationVotePhase.LOCKED) {
            return key("confirm.join_body_locked", faction.displayName(), lockedName(model),
                    faction.population(), capacity);
        }
        long candidates = faction.formations().stream()
                .filter(FormationSelectionView::available).count();
        String description = faction.description().isBlank() ? faction.displayName()
                : faction.description();
        return key("confirm.join_body", description, faction.population(), capacity, candidates);
    }

    public static Component joinConfirmOk() {
        return key("confirm.join_ok");
    }

    public static Component joinConfirmCancel() {
        return key("confirm.join_cancel");
    }

    public static Component lockConfirmTitle(FactionSelectionView faction) {
        return key("confirm.lock_title", faction.displayName());
    }

    /** Lock confirmation: whole-faction effect, votes, leader comparison and consequences. */
    public static Component lockConfirmBody(FormationVoteModel model,
                                            FormationSelectionView target) {
        FactionSelectionView own = model.joinedFaction();
        MutableComponent body = key("confirm.lock_body", own == null ? "" : own.displayName(),
                own == null ? 0 : own.population(), target.displayName(),
                model.votes(target.id()));
        FormationVoteModel.Leaders leaders = model.leaders();
        switch (model.relation(target)) {
            case NO_VOTES -> body.append(key("confirm.lock_warn_none"));
            case TIE_INCLUDED -> body.append(key("confirm.lock_warn_tie_in",
                    quoted(model, leaders.ids()), leaders.votes()));
            case TIE_EXCLUDED -> body.append(key("confirm.lock_warn_tie_out",
                    quoted(model, leaders.ids()), leaders.votes()));
            case NOT_LEADER -> body.append(key("confirm.lock_warn_leader",
                    model.formationName(leaders.ids().get(0)), leaders.votes()));
            case SOLE_LEADER -> {
            }
        }
        return body.append(key("confirm.lock_consequence"));
    }

    public static Component lockConfirmOk() {
        return key("confirm.lock_ok");
    }

    // ---- receipts, hints and notices -------------------------------------------------------------

    public static String pendingJoin(FactionSelectionView faction) {
        return I18n.get(PREFIX + "feedback.joining", faction.displayName());
    }

    public static String pendingVote(FormationSelectionView formation) {
        return I18n.get(PREFIX + "feedback.voting", formation.displayName());
    }

    public static String pendingOpen() {
        return I18n.get(PREFIX + "feedback.opening");
    }

    public static String pendingLock(FormationSelectionView formation) {
        return I18n.get(PREFIX + "feedback.locking", formation.displayName());
    }

    public static String cannotManage() {
        return I18n.get(PREFIX + "feedback.cannot_manage");
    }

    public static String cannotLock() {
        return I18n.get(PREFIX + "feedback.cannot_lock");
    }

    /** Footer Esc action: "关闭（` 键可重开）" while not joined, else "暂时关闭". */
    public static Component escClose(boolean joined, Component terminalKey) {
        if (joined) {
            return key("hint.close");
        }
        return terminalKey == null ? key("hint.close_unbound")
                : key("hint.close_reopen", terminalKey);
    }

    public static Component hintBackToList() {
        return key("hint.back_list");
    }

    public static Component hintWheel() {
        return key("hint.wheel");
    }

    public static Component hintBrowse() {
        return key("hint.browse");
    }

    public static Component hintScroll() {
        return key("hint.scroll");
    }

    public static Component hintRetry() {
        return key("hint.retry");
    }

    /** Action-bar message after closing the page without a faction (user report 4). */
    public static Component reopenNotice(Component terminalKey) {
        return terminalKey == null ? key("reopen_unbound") : key("reopen", terminalKey);
    }

    /** Receipt shown once in the deployment page footer when the formation was applied. */
    public static String lockedNotice(String formationName) {
        return I18n.get(PREFIX + "locked_notice", formationName);
    }

    // ---- waiting and empty ------------------------------------------------------------------------

    public static Component waitingTitle() {
        return key("waiting.title");
    }

    public static Component waitingMeta() {
        return key("waiting.meta");
    }

    public static Component waitingHeading() {
        return key("waiting.heading");
    }

    public static Component waitingHint() {
        return key("waiting.hint");
    }

    public static Component retryKey() {
        return key("waiting.retry");
    }

    public static Component emptyHeading() {
        return key("empty.heading");
    }

    public static Component emptyHint() {
        return key("empty.hint");
    }

    // ---- helpers ---------------------------------------------------------------------------------

    private static String lockedName(FormationVoteModel model) {
        FactionSelectionView faction = model.browsing();
        FormationSelectionView locked = model.lockedFormation(faction);
        return locked == null ? model.formationName(model.lockedFormationId(faction))
                : locked.displayName();
    }

    private static String names(FormationVoteModel model, List<String> ids) {
        return ids.stream().map(model::formationName)
                .collect(Collectors.joining(I18n.get(PREFIX + "list_separator")));
    }

    private static String quoted(FormationVoteModel model, List<String> ids) {
        return ids.stream().map(id -> I18n.get(PREFIX + "quoted", model.formationName(id)))
                .collect(Collectors.joining(I18n.get(PREFIX + "and_separator")));
    }
}
