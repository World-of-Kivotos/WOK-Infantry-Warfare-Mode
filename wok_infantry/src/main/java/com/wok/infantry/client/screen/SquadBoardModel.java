package com.wok.infantry.client.screen;

import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.battle.ClassLimitView;
import com.wok.infantry.battle.ClassQuotaView;
import com.wok.infantry.battle.KickCooldownView;
import com.wok.infantry.battle.MemberView;
import com.wok.infantry.battle.PermissionView;
import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.battle.SquadView;
import com.wok.infantry.deployment.DeploymentPhase;
import com.wok.infantry.deployment.DeploymentPoint;
import com.wok.infantry.deployment.DeploymentPointKind;
import com.wok.infantry.deployment.DeploymentView;
import com.wok.infantry.formation.vote.FormationVotePhase;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Pure logic of the battle terminal's squad, class and deployment pages (no drawing): who the
 * viewer is (authority), what every squad row says, which operations exist where, whether each
 * one is available and why not (as language keys with long / short / tiny forms), which ones
 * ask for confirmation and with what text, the page identity, and one shared pagination rule.
 *
 * <p>Everything is derived from one {@link BattleSnapshot} plus the screen's own selection, so
 * a model is cheap to rebuild every frame; build the widgets from the model taken at
 * {@code init()} and read countdowns from a fresh model at render time. The client only hints:
 * the server still validates every intent, and no rule here is stricter than the server's
 * (kicking is allowed in combat, the commander may hand over to any online squad leader).
 * While the faction's formation is not locked, nothing that creates or joins a squad, picks a
 * class or deploys is ever enabled.
 */
public final class SquadBoardModel {
    /** Language key prefix of everything this model names. */
    public static final String KEY_PREFIX = "screen.wok_infantry.squad_board.";
    /** Suffixes of the shorter reason forms ("…_short", "…_tiny"). */
    public static final String SHORT_SUFFIX = "_short";
    public static final String TINY_SUFFIX = "_tiny";
    /** Separator of identity parts ("学院军 · 阿尔法小队 · 小队长"). */
    public static final String IDENTITY_SEPARATOR = " · ";
    /** Rejoin cooldown after a kick, as the confirmation states it (seconds). */
    public static final int KICK_COOLDOWN_SECONDS = (int) (
            BattleRules.SQUAD_KICK_REJOIN_COOLDOWN_MILLIS / 1_000L);

    // ------------------------------------------------------------------------------- enums

    /** What the pages can show at all. */
    public enum Stage {
        /** No snapshot yet: the sync placeholder with a refresh key. */
        LOADING,
        /** A snapshot without a faction (the server normally clears instead). */
        NO_FACTION,
        /** In a faction whose formation vote is not locked: every page shows the vote block. */
        VOTE_PENDING,
        /** Formation known: squads, classes and deployment work normally. */
        READY
    }

    /** The viewer's single displayed role, commander first. */
    public enum Role {
        UNASSIGNED, MEMBER, LEADER, COMMANDER
    }

    /** Semantic colour of a status word (drawing maps it onto the theme tokens). */
    public enum Tone {
        SUCCESS, DANGER, NEUTRAL, MUTED, FAINT
    }

    /** Every operation of the three pages, with its label keys and confirmation rule. */
    public enum Action {
        CREATE_SQUAD("create", false, false),
        JOIN_SQUAD("join", false, false),
        RETURN_TO_OWN_SQUAD("return", false, false),
        LEAVE_SQUAD("leave", true, true),
        DISBAND_SQUAD("disband", true, true),
        KICK_MEMBER("kick", true, true),
        TRANSFER_LEADER("transfer_leader", true, false),
        CLAIM_COMMANDER("claim_commander", false, false),
        RESIGN_COMMANDER("resign_commander", false, true),
        TRANSFER_COMMANDER("transfer_commander", true, false),
        SELECT_CLASS("select_class", false, false),
        SELECT_DEPLOYMENT_POINT("select_point", false, false),
        DEPLOY("deploy", false, false),
        REDEPLOY("redeploy", true, true),
        RESUPPLY("resupply", false, false);

        private final String id;
        private final boolean confirmed;
        private final boolean danger;

        Action(String id, boolean confirmed, boolean danger) {
            this.id = id;
            this.confirmed = confirmed;
            this.danger = danger;
        }

        public String id() {
            return id;
        }

        /**
         * Asks for a confirmation first (user decision 2026-10-05: kick, leave, disband,
         * redeploy, hand over leadership and hand over command; resigning does not ask).
         */
        public boolean requiresConfirm() {
            return confirmed;
        }

        /** Red, destructive styling (and a danger confirmation that Enter does not accept). */
        public boolean danger() {
            return danger;
        }

        /** Full label key; create/join take the call sign as their only argument. */
        public String labelKey() {
            return KEY_PREFIX + "action." + id;
        }

        /** Short label for narrow cells ("退队", "移交"), never an ellipsized full label. */
        public String shortLabelKey() {
            return labelKey() + SHORT_SUFFIX;
        }
    }

    /** Status word of a squad row (preview squadInfo). */
    public enum SquadStatus {
        VOTE_PENDING("pending", Tone.FAINT),
        NOT_CONFIGURED("closed", Tone.FAINT),
        OWN("own", Tone.SUCCESS),
        CREATABLE("creatable", Tone.SUCCESS),
        NOT_CREATED("empty", Tone.MUTED),
        FULL("full", Tone.DANGER),
        JOINABLE("joinable", Tone.SUCCESS),
        COOLDOWN("cooldown", Tone.MUTED),
        /** Established squad the viewer cannot join from another squad: no word, the count says it. */
        NONE("", Tone.MUTED);

        private final String id;
        private final Tone tone;

        SquadStatus(String id, Tone tone) {
            this.id = id;
            this.tone = tone;
        }

        public Tone tone() {
            return tone;
        }

        public boolean hasLabel() {
            return !id.isEmpty();
        }

        /** Label key, or {@code null} for {@link #NONE}. */
        public String key() {
            return id.isEmpty() ? null : KEY_PREFIX + "status." + id;
        }

        public Component label() {
            return id.isEmpty() ? Component.empty() : Component.translatable(key());
        }
    }

    /** Why an operation is unavailable, or what it does (hints). Each has three lengths. */
    public enum ReasonCode {
        VOTE_PENDING("vote_pending", 0, 0, 0),
        NOT_CONFIGURED("not_configured", 0, 0, 0),
        IN_SQUAD_CREATE("in_squad_create", 1, 1, 0),
        IN_SQUAD_JOIN("in_squad_join", 1, 1, 0),
        SQUAD_CHANGE_ACTIVE("squad_change_active", 0, 0, 0),
        SQUAD_FULL("squad_full", 0, 0, 0),
        KICK_COOLDOWN("kick_cooldown", 1, 1, 1),
        CREATE_UNAVAILABLE("create_unavailable", 0, 0, 0),
        JOIN_UNAVAILABLE("join_unavailable", 0, 0, 0),
        PENDING("pending", 0, 0, 0),
        SELECT_MEMBER("select_member", 0, 0, 0),
        SELECT_OTHER_MEMBER("select_other_member", 0, 0, 0),
        TARGET_OFFLINE("target_offline", 1, 1, 0),
        LEADER_ONLY("leader_only", 0, 0, 0),
        LEAVE_ACTIVE("leave_active", 0, 0, 0),
        LEAVE_UNAVAILABLE("leave_unavailable", 0, 0, 0),
        CLAIM_NO_SQUAD("claim_no_squad", 0, 0, 0),
        CLAIM_NOT_LEADER("claim_not_leader", 0, 0, 0),
        COMMANDER_EXISTS("commander_exists", 0, 0, 0),
        CLAIM_UNAVAILABLE("claim_unavailable", 0, 0, 0),
        COMMANDER_TARGET("commander_target", 0, 0, 0),
        CLASS_NO_SQUAD("class_no_squad", 0, 0, 0),
        CLASS_ACTIVE("class_active", 0, 0, 0),
        CLASS_CURRENT("class_current", 0, 0, 0),
        CLASS_NOT_OPEN("class_not_open", 0, 0, 0),
        CLASS_FULL("class_full", 0, 0, 0),
        DEPLOY_ACTIVE("deploy_active", 0, 0, 0),
        DEPLOY_WAITING("deploy_waiting", 1, 1, 1),
        DEPLOY_NO_SQUAD("deploy_no_squad", 0, 0, 0),
        DEPLOY_NO_POINTS("deploy_no_points", 0, 0, 0),
        DEPLOY_SELECT_POINT("deploy_select_point", 0, 0, 0),
        DEPLOY_POINT_GONE("deploy_point_gone", 0, 0, 0),
        DEPLOY_UNAVAILABLE("deploy_unavailable", 0, 0, 0),
        POINT_ACTIVE("point_active", 0, 0, 0),
        POINT_SELECTED("point_selected", 0, 0, 0),
        REDEPLOY_NOT_ACTIVE("redeploy_not_active", 0, 0, 0),
        RESUPPLY_NOT_ACTIVE("resupply_not_active", 0, 0, 0),
        RESUPPLY_COOLDOWN("resupply_cooldown", 1, 1, 1),
        RESUPPLY_AWAY("resupply_away", 0, 0, 0),
        CREATE_HINT("create_hint", 0, 0, 0),
        JOIN_HINT("join_hint", 1, 1, 0),
        JOIN_HINT_PLAIN("join_hint_plain", 0, 0, 0),
        MANAGE_HINT("manage_hint", 0, 0, 0),
        CLAIM_HINT("claim_hint", 0, 0, 0),
        COMMANDER_TRANSFER_HINT("commander_transfer_hint", 1, 1, 0),
        CLASS_HINT("class_hint", 1, 1, 0),
        DEPLOY_HINT("deploy_hint", 0, 0, 0),
        REDEPLOY_HINT("redeploy_hint", 0, 0, 0),
        RESUPPLY_HINT("resupply_hint", 0, 0, 0);

        private final String id;
        private final int[] argumentCounts;

        ReasonCode(String id, int full, int shortForm, int tiny) {
            this.id = id;
            this.argumentCounts = new int[]{full, shortForm, tiny};
        }

        /** Full-length key; {@code key() + "_short"} and {@code key() + "_tiny"} also exist. */
        public String key() {
            return KEY_PREFIX + "reason." + id;
        }

        /** The three keys, longest form first. */
        public List<String> keys() {
            return List.of(key(), key() + SHORT_SUFFIX, key() + TINY_SUFFIX);
        }

        /** Format arguments each of {@link #keys()} uses, in the same order. */
        public int argumentCount(int variant) {
            return argumentCounts[variant];
        }
    }

    /** Rows of the deployment checklist. */
    public enum CheckItem {
        FORMATION, SQUAD, CLASS, POINT, RESPAWN;

        public String labelKey() {
            return KEY_PREFIX + "check." + name().toLowerCase(Locale.ROOT);
        }
    }

    /** State of one checklist row: done (check), to do (cross), waiting (clock), locked. */
    public enum CheckState {
        DONE, TODO, WAITING, LOCKED
    }

    // ----------------------------------------------------------------------------- records

    /**
     * A reason or hint with its arguments. Arguments may be components (names, call signs);
     * the three lengths share them, a shorter form simply uses fewer.
     */
    public record Reason(ReasonCode code, List<Object> args) {
        public Reason {
            Objects.requireNonNull(code, "code");
            args = args == null ? List.of() : args.stream()
                    .map(argument -> argument == null ? "" : argument).toList();
        }

        public static Reason of(ReasonCode code, Object... args) {
            return new Reason(code, Arrays.asList(args));
        }

        public String key() {
            return code.key();
        }

        public MutableComponent full() {
            return variant(0);
        }

        public MutableComponent shortForm() {
            return variant(1);
        }

        public MutableComponent tiny() {
            return variant(2);
        }

        /** Full, short and tiny forms, for a "first that fits" choice. */
        public List<Component> candidates() {
            return List.of(full(), shortForm(), tiny());
        }

        private MutableComponent variant(int index) {
            int count = Math.min(code.argumentCount(index), args.size());
            return Component.translatable(code.keys().get(index),
                    args.subList(0, count).toArray());
        }
    }

    /** A confirmation layer's text. Body paragraphs are shown in order. */
    public record Confirm(Action action, Component title, List<Component> body,
                          Component confirmLabel, boolean danger) {
        public Confirm {
            Objects.requireNonNull(action, "action");
            Objects.requireNonNull(title, "title");
            body = List.copyOf(Objects.requireNonNullElse(body, List.of()));
            Objects.requireNonNull(confirmLabel, "confirmLabel");
        }

        /** The paragraphs joined by a blank line, for a single-body dialog. */
        public Component joinedBody() {
            MutableComponent result = Component.empty();
            for (int index = 0; index < body.size(); index++) {
                if (index > 0) {
                    result.append("\n\n");
                }
                result.append(body.get(index));
            }
            return result;
        }
    }

    /**
     * One operation as offered right now.
     *
     * @param reason  why it is disabled; {@code null} exactly when {@code enabled}
     * @param hint    what it does when enabled (may be {@code null})
     * @param confirm the confirmation to show before sending; {@code null} when the action
     *                needs none or is disabled
     * @param squad   call sign the intent targets (create/join/return), else {@code null}
     * @param target  member the intent targets (kick, hand-overs), else {@code null}
     * @param classId class the intent selects, else {@code null}
     * @param pointId deployment point the intent selects, else {@code null}
     */
    public record ActionState(Action action, boolean enabled, Reason reason, Reason hint,
                              Confirm confirm, SquadCallsign squad, UUID target, String classId,
                              UUID pointId) {
        public ActionState {
            Objects.requireNonNull(action, "action");
            if (enabled == (reason != null)) {
                throw new IllegalArgumentException("A disabled action needs exactly one reason");
            }
            if (!enabled) {
                confirm = null;
            }
        }

        public boolean disabled() {
            return !enabled;
        }

        /** The line to show under / beside the control: the reason, else the hint. */
        public Reason explanation() {
            return reason != null ? reason : hint;
        }

        public boolean requiresConfirm() {
            return action.requiresConfirm();
        }

        public boolean danger() {
            return action.danger();
        }

        /** Full label ("加入布拉沃小队", "退出小队"). */
        public MutableComponent label() {
            return action == Action.CREATE_SQUAD || action == Action.JOIN_SQUAD
                    ? Component.translatable(action.labelKey(), SquadLabels.callsign(squad))
                    : Component.translatable(action.labelKey());
        }

        /** Short label for a narrow cell ("加入", "退队"). */
        public MutableComponent shortLabel() {
            return Component.translatable(action.shortLabelKey());
        }
    }

    /** The viewer's standing; {@code administrator} is a client hint only. */
    public record Authority(boolean inFaction, boolean inSquad, boolean leader,
                            boolean commander, boolean administrator, boolean manageOwnSquad) {
        public Role role() {
            if (commander) {
                return Role.COMMANDER;
            }
            if (!inSquad) {
                return Role.UNASSIGNED;
            }
            return leader ? Role.LEADER : Role.MEMBER;
        }

        public MutableComponent roleName() {
            return SquadLabels.roleName(inSquad, leader, commander);
        }

        public MutableComponent roleNameShort() {
            return SquadLabels.roleNameShort(inSquad, leader, commander);
        }
    }

    /**
     * One call sign of the faction's formation.
     *
     * @param view                the squad as sent, {@code null} when the formation has no such
     *                            call sign (or before the vote is locked)
     * @param leader              the squad's leader, {@code null} when not established
     * @param kickCooldownSeconds seconds before the viewer may create or join it again (0 = free)
     */
    public record SquadRow(SquadCallsign callsign, SquadView view, SquadStatus status,
                           boolean own, boolean viewed, MemberView leader,
                           int kickCooldownSeconds) {
        public boolean configured() {
            return view != null;
        }

        public int members() {
            return view == null ? 0 : view.members().size();
        }

        public int capacity() {
            return view == null ? 0 : view.capacity();
        }

        public boolean full() {
            return view != null && view.full();
        }

        public MutableComponent name() {
            return SquadLabels.callsign(callsign);
        }

        public MutableComponent shortName() {
            return SquadLabels.callsignShort(callsign);
        }

        /** "n/capacity", or "—" for a call sign the formation does not open. */
        public MutableComponent count() {
            return view == null ? Component.translatable(KEY_PREFIX + "count.none")
                    : SquadLabels.memberCount(view);
        }

        /** Second line, longest first: "队长 X", "尚未建立", "本阵营编制无此呼号"… */
        public List<Component> subCandidates() {
            if (status == SquadStatus.VOTE_PENDING) {
                return List.of(Component.translatable(KEY_PREFIX + "sub.pending"));
            }
            if (view == null) {
                return List.of(Component.translatable(KEY_PREFIX + "sub.closed"),
                        Component.translatable(KEY_PREFIX + "sub.closed" + SHORT_SUFFIX));
            }
            if (leader != null) {
                return List.of(Component.translatable(KEY_PREFIX + "sub.leader",
                        leader.name()));
            }
            return List.of(Component.translatable(KEY_PREFIX + "sub.empty"));
        }
    }

    /**
     * The operations under the viewed squad's roster.
     *
     * @param ownSquad  the viewed squad is the viewer's own
     * @param targetRow show the "操作对象" line (own squad, viewer may manage it)
     * @param target    the selected other member of the own squad, or {@code null}
     * @param buttons   in display order
     * @param split     index where the second button group starts (0 = one group)
     * @param reason    the single line under the buttons (a reason, or a hint)
     */
    public record RosterActions(boolean ownSquad, boolean targetRow, MemberView target,
                                List<ActionState> buttons, int split, Reason reason) {
        public RosterActions {
            buttons = List.copyOf(buttons);
        }

        public ActionState button(Action action) {
            return buttons.stream().filter(button -> button.action() == action)
                    .findFirst().orElse(null);
        }
    }

    /**
     * The commander block: who commands, and the viewer's own commander operations.
     *
     * @param commander      the faction commander, {@code null} when vacant
     * @param commanderSquad the commander's squad, {@code null} when vacant
     */
    public record CommanderPanel(MemberView commander, SquadCallsign commanderSquad,
                                 List<ActionState> buttons, Reason reason) {
        public CommanderPanel {
            buttons = List.copyOf(buttons);
        }

        public ActionState button(Action action) {
            return buttons.stream().filter(button -> button.action() == action)
                    .findFirst().orElse(null);
        }
    }

    /**
     * One class of the viewer's quota list.
     *
     * @param squadLimit the own squad's per-squad entry (protocol 20), {@code null} outside a squad
     * @param holders    own-squad members currently holding the class, in roster order
     */
    public record ClassRow(ClassQuotaView quota, boolean current, ClassLimitView squadLimit,
                           List<MemberView> holders, ActionState select) {
        public ClassRow {
            holders = List.copyOf(holders);
        }

        public String classId() {
            return quota.classId();
        }

        public boolean closed() {
            return quota.limit() <= 0;
        }
    }

    /**
     * One deployment point.
     *
     * @param number 1-based number among points of the same kind (beacons are numbered)
     */
    public record PointRow(DeploymentPoint point, int number, boolean selected,
                           ActionState select) {
        /** "主基地", "部署信标 2", "小队队包". */
        public MutableComponent title() {
            return switch (point.kind()) {
                case MAIN_BASE -> Component.translatable(KEY_PREFIX + "point.main_base");
                case FIELD_BEACON -> Component.translatable(KEY_PREFIX + "point.field_beacon",
                        number);
                case RALLY -> Component.translatable(KEY_PREFIX + "point.rally");
            };
        }

        /** "主世界 · X 12 · Z -40" (two-line rows). */
        public MutableComponent coordinates() {
            return Component.translatable(KEY_PREFIX + "point.coords",
                    SquadLabels.dimensionName(point.dimension()), point.position().getX(),
                    point.position().getZ());
        }

        /** "主世界 · X 12 · Y 64 · Z -40" (three-line rows). */
        public MutableComponent coordinatesWithHeight() {
            return Component.translatable(KEY_PREFIX + "point.coords_full",
                    SquadLabels.dimensionName(point.dimension()), point.position().getX(),
                    point.position().getY(), point.position().getZ());
        }
    }

    /** One checklist row of the deployment status panel. */
    public record ChecklistItem(CheckItem item, CheckState state, Component value) {
        public Component label() {
            return Component.translatable(item.labelKey());
        }
    }

    /**
     * One pagination result, shared by a list's title meta, its rows and its pager so they
     * can never disagree (squad-01). Compute it once per layout.
     */
    public record Page(int page, int pageCount, int start, int end) {
        /** Rows of {@code rowHeight} at {@code pitch} spacing that fit in [top, bottom). */
        public static int visibleRows(int top, int bottomExclusive, int rowHeight, int pitch) {
            if (rowHeight <= 0 || pitch < rowHeight) {
                throw new IllegalArgumentException("Invalid row geometry");
            }
            int available = Math.max(0, bottomExclusive - top);
            return available < rowHeight ? 0 : 1 + (available - rowHeight) / pitch;
        }

        public static int pageCount(int itemCount, int rowsPerPage) {
            if (itemCount <= 0 || rowsPerPage <= 0) {
                return 1;
            }
            return 1 + (itemCount - 1) / rowsPerPage;
        }

        /** The page {@code requestedPage} clamped into range. */
        public static Page of(int itemCount, int rowsPerPage, int requestedPage) {
            int count = Math.max(0, itemCount);
            int pages = pageCount(count, rowsPerPage);
            int page = Math.max(0, Math.min(requestedPage, pages - 1));
            int start = rowsPerPage <= 0 ? 0 : Math.min(count, page * rowsPerPage);
            int end = rowsPerPage <= 0 ? 0 : Math.min(count, start + rowsPerPage);
            return new Page(page, pages, start, end);
        }

        public int size() {
            return end - start;
        }

        public boolean multiplePages() {
            return pageCount > 1;
        }

        public boolean hasPrevious() {
            return page > 0;
        }

        public boolean hasNext() {
            return page + 1 < pageCount;
        }

        public int previousPage() {
            return hasPrevious() ? page - 1 : page;
        }

        public int nextPage() {
            return hasNext() ? page + 1 : page;
        }

        /** The page's slice of {@code items} (which must be the list the page was made for). */
        public <T> List<T> slice(List<T> items) {
            return items.subList(Math.min(start, items.size()), Math.min(end, items.size()));
        }

        /** "1/3" for a title meta or pager label. */
        public String label() {
            return (page + 1) + "/" + pageCount;
        }
    }

    /**
     * Everything the model reads besides the snapshot: the screen's selection, the clock used
     * for the kick countdown ({@code ClientBattleState.estimatedServerTimeMillis()}), the
     * client-side administrator hint, the faction's vote phase (from the formation catalog, may
     * be {@code null}) and intents sent but not yet answered (their buttons wait).
     */
    public record Input(BattleSnapshot snapshot, SquadCallsign viewedSquad, UUID selectedMember,
                        long nowServerMillis, boolean administrator,
                        FormationVotePhase votePhase, Set<Action> pending) {
        public Input {
            pending = pending == null || pending.isEmpty() ? Set.of()
                    : Set.copyOf(pending);
        }

        public static Input of(BattleSnapshot snapshot) {
            return new Input(snapshot, null, null,
                    snapshot == null ? 0L : snapshot.serverTimeMillis(), false, null, Set.of());
        }

        public Input withViewedSquad(SquadCallsign squad) {
            return new Input(snapshot, squad, selectedMember, nowServerMillis, administrator,
                    votePhase, pending);
        }

        public Input withSelectedMember(UUID member) {
            return new Input(snapshot, viewedSquad, member, nowServerMillis, administrator,
                    votePhase, pending);
        }

        public Input withNow(long serverMillis) {
            return new Input(snapshot, viewedSquad, selectedMember, serverMillis, administrator,
                    votePhase, pending);
        }

        public Input withAdministrator(boolean value) {
            return new Input(snapshot, viewedSquad, selectedMember, nowServerMillis, value,
                    votePhase, pending);
        }

        public Input withVotePhase(FormationVotePhase phase) {
            return new Input(snapshot, viewedSquad, selectedMember, nowServerMillis,
                    administrator, phase, pending);
        }

        public Input withPending(Collection<Action> actions) {
            return new Input(snapshot, viewedSquad, selectedMember, nowServerMillis,
                    administrator, votePhase,
                    actions == null ? Set.of() : Set.copyOf(actions));
        }
    }

    // ------------------------------------------------------------------------------ state

    private final Input input;
    private final BattleSnapshot snapshot;
    private final Stage stage;
    private final Authority authority;
    private final List<SquadRow> squads;
    private final SquadCallsign viewedSquad;
    private final MemberView selectedMember;
    private final RosterActions rosterActions;
    private final CommanderPanel commanderPanel;
    private final List<ClassRow> classRows;
    private final List<PointRow> pointRows;
    private final ActionState deploy;
    private final ActionState redeploy;
    private final ActionState resupply;
    private final List<ChecklistItem> checklist;

    private SquadBoardModel(Input input) {
        this.input = Objects.requireNonNull(input, "input");
        this.snapshot = input.snapshot();
        this.stage = stageOf(snapshot);
        this.authority = authorityOf(snapshot, input.administrator());
        this.viewedSquad = normalizeViewedSquad();
        this.selectedMember = normalizeSelectedMember();
        this.squads = buildSquadRows();
        this.rosterActions = buildRosterActions();
        this.commanderPanel = buildCommanderPanel();
        this.classRows = buildClassRows();
        this.pointRows = buildPointRows();
        this.deploy = buildDeploy();
        this.redeploy = buildRedeploy();
        this.resupply = buildResupply();
        this.checklist = buildChecklist();
    }

    public static SquadBoardModel of(Input input) {
        return new SquadBoardModel(input);
    }

    /** Convenience for the common case (no admin hint, vote phase or pending intents). */
    public static SquadBoardModel of(BattleSnapshot snapshot, SquadCallsign viewedSquad,
                                     UUID selectedMember, long nowServerMillis) {
        return of(Input.of(snapshot).withViewedSquad(viewedSquad)
                .withSelectedMember(selectedMember).withNow(nowServerMillis));
    }

    public Input input() {
        return input;
    }

    /** The snapshot the model was built from (may be {@code null} while loading). */
    public BattleSnapshot snapshot() {
        return snapshot;
    }

    public Stage stage() {
        return stage;
    }

    public boolean votePending() {
        return stage == Stage.VOTE_PENDING;
    }

    public boolean ready() {
        return stage == Stage.READY;
    }

    public Authority authority() {
        return authority;
    }

    /** All five call signs in order. */
    public List<SquadRow> squads() {
        return squads;
    }

    public SquadRow squad(SquadCallsign callsign) {
        return squads.stream().filter(row -> row.callsign() == callsign).findFirst()
                .orElse(null);
    }

    /** The squad whose roster is shown: the requested one, else own, else first configured. */
    public SquadCallsign viewedSquad() {
        return viewedSquad;
    }

    public SquadRow viewedRow() {
        return squad(viewedSquad);
    }

    /** The selected member if they belong to the viewed squad, else {@code null}. */
    public MemberView selectedMember() {
        return selectedMember;
    }

    public RosterActions rosterActions() {
        return rosterActions;
    }

    public CommanderPanel commanderPanel() {
        return commanderPanel;
    }

    /** The class the viewer has reserved ("" when unknown). */
    public String currentClassId() {
        if (snapshot == null) {
            return "";
        }
        if (!snapshot.viewerClassId().isEmpty()) {
            return snapshot.viewerClassId();
        }
        MemberView viewer = viewerMember();
        return viewer == null ? "" : viewer.classId();
    }

    public List<ClassRow> classRows() {
        return classRows;
    }

    public ClassRow currentClassRow() {
        return classRows.stream().filter(ClassRow::current).findFirst().orElse(null);
    }

    public List<PointRow> pointRows() {
        return pointRows;
    }

    public PointRow selectedPoint() {
        return pointRows.stream().filter(PointRow::selected).findFirst().orElse(null);
    }

    public ActionState deploy() {
        return deploy;
    }

    public ActionState redeploy() {
        return redeploy;
    }

    public ActionState resupply() {
        return resupply;
    }

    /** Waiting-for-deployment checklist (empty while in combat or loading). */
    public List<ChecklistItem> checklist() {
        return checklist;
    }

    /** Seconds of the respawn countdown (rounded up), 0 when not waiting. */
    public int respawnSeconds() {
        if (snapshot == null || snapshot.deployment().phase() != DeploymentPhase.WAITING) {
            return 0;
        }
        return ticksToSeconds(snapshot.deployment().waitingTicks());
    }

    /** Seconds until the next resupply (rounded up), 0 when free. */
    public int resupplySeconds() {
        return snapshot == null ? 0 : ticksToSeconds(snapshot.deployment().resupplyTicks());
    }

    /** Own public faction's player cap (context), else the battle-wide faction capacity. */
    public int factionCapacity() {
        if (snapshot == null) {
            return 0;
        }
        int capacity = snapshot.formationContext().factionCapacity();
        return capacity > 0 ? capacity : snapshot.factionCapacity();
    }

    /** Opposing public faction's player cap (context), else the battle-wide faction capacity. */
    public int enemyFactionCapacity() {
        if (snapshot == null) {
            return 0;
        }
        int capacity = snapshot.formationContext().enemyFactionCapacity();
        return capacity > 0 ? capacity : snapshot.factionCapacity();
    }

    /** Seconds before the viewer may create or join {@code squad} again, 0 when free. */
    public int kickCooldownSeconds(SquadCallsign squad) {
        if (snapshot == null) {
            return 0;
        }
        KickCooldownView cooldown = snapshot.kickCooldown(squad);
        return cooldown == null ? 0 : cooldown.remainingSeconds(input.nowServerMillis());
    }

    /** Every action state the model offers (roster, commander, classes, points, deployment). */
    public List<ActionState> actions() {
        List<ActionState> result = new ArrayList<>(rosterActions.buttons());
        result.addAll(commanderPanel.buttons());
        classRows.forEach(row -> result.add(row.select()));
        pointRows.forEach(row -> result.add(row.select()));
        result.add(deploy);
        result.add(redeploy);
        result.add(resupply);
        return List.copyOf(result);
    }

    /** First offered state of {@code action}, or {@code null}. */
    public ActionState action(Action action) {
        return actions().stream().filter(state -> state.action() == action).findFirst()
                .orElse(null);
    }

    /**
     * The enabled operation this model still offers with the same intent as {@code original}
     * (same action, call sign, target, class and deployment point), or {@code null} when it no
     * longer applies. A confirmation opened for {@code original} is checked against every newer
     * snapshot with this (its text follows the returned state) and once more before it is sent:
     * a kick whose target has left, a disband after the squad went into combat or a hand-over
     * after the viewer lost the leadership is dropped instead of being sent stale.
     */
    public ActionState stillOffered(ActionState original) {
        if (original == null || !original.enabled()) {
            return null;
        }
        for (ActionState candidate : actions()) {
            if (candidate.enabled() && sameIntent(candidate, original)) {
                return candidate;
            }
        }
        return null;
    }

    /** Same operation on the same call sign, member, class and point. */
    static boolean sameIntent(ActionState left, ActionState right) {
        return left.action() == right.action()
                && left.squad() == right.squad()
                && Objects.equals(left.target(), right.target())
                && Objects.equals(left.classId(), right.classId())
                && Objects.equals(left.pointId(), right.pointId());
    }

    /**
     * Page identity, longest first ("阵营 · 编制 · 小队 · 职务" shortened step by step); the
     * shell shows the first that fits.
     */
    public List<Component> identityCandidates() {
        if (stage == Stage.LOADING) {
            return List.of(Component.translatable("screen.wok_infantry.map.link_connecting"));
        }
        MutableComponent faction = SquadLabels.factionName(snapshot);
        if (stage == Stage.NO_FACTION) {
            return List.of(faction);
        }
        if (stage == Stage.VOTE_PENDING) {
            String state = input.votePhase() == FormationVotePhase.OPEN ? "vote_open"
                    : "vote_waiting";
            return List.of(
                    Component.translatable(KEY_PREFIX + "identity." + state, faction),
                    Component.translatable(KEY_PREFIX + "identity." + state + SHORT_SUFFIX,
                            faction));
        }
        MutableComponent formation = SquadLabels.formationName(snapshot);
        List<Component> result = new ArrayList<>();
        if (!authority.inSquad()) {
            MutableComponent unassigned = SquadLabels.roleName(false, false,
                    authority.commander());
            if (formation != null) {
                result.add(join(faction, formation, unassigned));
            }
            result.add(join(faction, unassigned));
            return List.copyOf(result);
        }
        SquadCallsign own = snapshot.ownSquad();
        if (formation != null) {
            result.add(join(faction, formation, SquadLabels.callsign(own),
                    authority.roleName()));
        }
        result.add(join(faction, SquadLabels.callsign(own), authority.roleName()));
        result.add(join(faction, SquadLabels.callsignShort(own), authority.roleName()));
        result.add(join(faction, SquadLabels.callsignShort(own), authority.roleNameShort()));
        result.add(join(faction, SquadLabels.callsignShort(own)));
        return List.copyOf(result);
    }

    /**
     * Why a terminal tab cannot be opened, or {@code null}. Before the formation is locked every
     * tab but the formation tab waits for it ("编制锁定后开放"), the same rule as the formation
     * page's strip (user decision 6.2); the page that is already shown is never disabled by the
     * screen, and it shows the vote block.
     */
    public Component tabDisabledReason(BattleTab tab) {
        if (tab == null || stage == Stage.READY || stage == Stage.LOADING) {
            return null;
        }
        return tab == BattleTab.FORMATION ? null : FormationText.tabLockedReason();
    }

    // ------------------------------------------------------------------------ derivation

    static Stage stageOf(BattleSnapshot snapshot) {
        if (snapshot == null) {
            return Stage.LOADING;
        }
        if (snapshot.faction() == null) {
            return Stage.NO_FACTION;
        }
        return snapshot.formationLocked() ? Stage.READY : Stage.VOTE_PENDING;
    }

    static Authority authorityOf(BattleSnapshot snapshot, boolean administratorHint) {
        if (snapshot == null) {
            return new Authority(false, false, false, false, administratorHint, false);
        }
        PermissionView permissions = snapshot.permissions();
        boolean inSquad = snapshot.ownSquad() != null;
        // canRemoveAnyMarker = commander || administrator on the server, so it reveals the
        // administrator for every non-commander; the explicit hint covers the rest.
        boolean administrator = administratorHint
                || (permissions.canRemoveAnyMarker() && !snapshot.commander());
        boolean manage = inSquad && permissions.canManageSquad();
        return new Authority(snapshot.faction() != null, inSquad, snapshot.squadLeader(),
                snapshot.commander(), administrator, manage);
    }

    private SquadCallsign normalizeViewedSquad() {
        if (input.viewedSquad() != null) {
            return input.viewedSquad();
        }
        if (snapshot != null && snapshot.ownSquad() != null) {
            return snapshot.ownSquad();
        }
        if (snapshot != null && !snapshot.squads().isEmpty()) {
            return snapshot.squads().get(0).callsign();
        }
        return SquadCallsign.ALPHA;
    }

    private MemberView normalizeSelectedMember() {
        if (snapshot == null || input.selectedMember() == null) {
            return null;
        }
        SquadView viewed = snapshot.squad(viewedSquad);
        if (viewed == null) {
            return null;
        }
        return viewed.members().stream()
                .filter(member -> member.playerId().equals(input.selectedMember()))
                .findFirst().orElse(null);
    }

    private MemberView viewerMember() {
        if (snapshot == null) {
            return null;
        }
        for (SquadView squad : snapshot.squads()) {
            for (MemberView member : squad.members()) {
                if (member.playerId().equals(snapshot.viewerId())) {
                    return member;
                }
            }
        }
        return null;
    }

    private List<SquadRow> buildSquadRows() {
        List<SquadRow> rows = new ArrayList<>(SquadCallsign.values().length);
        for (SquadCallsign callsign : SquadCallsign.values()) {
            SquadView view = snapshot == null || stage != Stage.READY ? null
                    : snapshot.squad(callsign);
            boolean own = snapshot != null && snapshot.ownSquad() == callsign;
            int cooldown = kickCooldownSeconds(callsign);
            MemberView leader = view == null ? null : view.members().stream()
                    .filter(MemberView::leader).findFirst().orElse(null);
            rows.add(new SquadRow(callsign, view, squadStatus(view, own, cooldown),
                    own, callsign == viewedSquad, leader, cooldown));
        }
        return List.copyOf(rows);
    }

    private SquadStatus squadStatus(SquadView view, boolean own, int cooldown) {
        if (stage != Stage.READY) {
            return SquadStatus.VOTE_PENDING;
        }
        if (view == null) {
            return SquadStatus.NOT_CONFIGURED;
        }
        if (own) {
            return SquadStatus.OWN;
        }
        boolean inSquad = authority.inSquad();
        if (view.members().isEmpty()) {
            if (inSquad) {
                return SquadStatus.NOT_CREATED;
            }
            return cooldown > 0 ? SquadStatus.COOLDOWN : SquadStatus.CREATABLE;
        }
        if (view.full()) {
            return SquadStatus.FULL;
        }
        if (!inSquad) {
            return cooldown > 0 ? SquadStatus.COOLDOWN : SquadStatus.JOINABLE;
        }
        return SquadStatus.NONE;
    }

    private boolean pending(Action action) {
        return input.pending().contains(action);
    }

    private boolean canChangeSquad() {
        return snapshot != null && snapshot.deployment().canChangeSquad();
    }

    private RosterActions buildRosterActions() {
        if (stage != Stage.READY) {
            return new RosterActions(false, false, null, List.of(), 0,
                    Reason.of(ReasonCode.VOTE_PENDING));
        }
        SquadView viewed = snapshot.squad(viewedSquad);
        boolean ownSquad = authority.inSquad() && snapshot.ownSquad() == viewedSquad
                && viewed != null;
        if (ownSquad) {
            return ownSquadActions(viewed);
        }
        List<ActionState> buttons = new ArrayList<>();
        Reason reason;
        if (viewed == null) {
            reason = Reason.of(ReasonCode.NOT_CONFIGURED);
        } else if (viewed.members().isEmpty()) {
            ActionState create = createState(viewed);
            buttons.add(create);
            reason = create.explanation();
        } else {
            ActionState join = joinState(viewed);
            buttons.add(join);
            reason = join.explanation();
        }
        if (authority.inSquad()) {
            buttons.add(new ActionState(Action.RETURN_TO_OWN_SQUAD, true, null, null, null,
                    snapshot.ownSquad(), null, null, null));
        }
        return new RosterActions(false, false, null, buttons, 0, reason);
    }

    private RosterActions ownSquadActions(SquadView viewed) {
        MemberView selected = selectedMember;
        boolean selectedSelf = selected != null
                && selected.playerId().equals(snapshot.viewerId());
        MemberView target = selectedSelf ? null : selected;
        ActionState leave = leaveState(viewed);
        if (!authority.manageOwnSquad()) {
            Reason reason = canChangeSquad() ? Reason.of(ReasonCode.LEADER_ONLY)
                    : Reason.of(ReasonCode.LEAVE_ACTIVE);
            return new RosterActions(true, false, null, List.of(leave), 0, reason);
        }
        Reason noTarget = Reason.of(selectedSelf ? ReasonCode.SELECT_OTHER_MEMBER
                : ReasonCode.SELECT_MEMBER);
        ActionState transfer;
        if (target == null) {
            transfer = disabled(Action.TRANSFER_LEADER, noTarget);
        } else if (!target.online()) {
            transfer = disabled(Action.TRANSFER_LEADER,
                    Reason.of(ReasonCode.TARGET_OFFLINE, target.name()));
        } else if (pending(Action.TRANSFER_LEADER)) {
            transfer = disabled(Action.TRANSFER_LEADER, Reason.of(ReasonCode.PENDING));
        } else {
            transfer = new ActionState(Action.TRANSFER_LEADER, true, null,
                    Reason.of(ReasonCode.MANAGE_HINT),
                    transferLeaderConfirm(target), null, target.playerId(), null, null);
        }
        ActionState kick;
        if (target == null) {
            kick = disabled(Action.KICK_MEMBER, noTarget);
        } else if (pending(Action.KICK_MEMBER)) {
            kick = disabled(Action.KICK_MEMBER, Reason.of(ReasonCode.PENDING));
        } else {
            kick = new ActionState(Action.KICK_MEMBER, true, null,
                    Reason.of(ReasonCode.MANAGE_HINT), kickConfirm(target, viewed), null,
                    target.playerId(), null, null);
        }
        ActionState disband;
        if (!canChangeSquad()) {
            disband = disabled(Action.DISBAND_SQUAD, Reason.of(ReasonCode.LEAVE_ACTIVE));
        } else if (pending(Action.DISBAND_SQUAD)) {
            disband = disabled(Action.DISBAND_SQUAD, Reason.of(ReasonCode.PENDING));
        } else {
            disband = new ActionState(Action.DISBAND_SQUAD, true, null,
                    Reason.of(ReasonCode.MANAGE_HINT), disbandConfirm(viewed), null, null,
                    null, null);
        }
        Reason reason;
        if (!canChangeSquad()) {
            reason = Reason.of(ReasonCode.LEAVE_ACTIVE);
        } else if (target == null) {
            reason = noTarget;
        } else if (!target.online()) {
            reason = Reason.of(ReasonCode.TARGET_OFFLINE, target.name());
        } else {
            reason = Reason.of(ReasonCode.MANAGE_HINT);
        }
        return new RosterActions(true, true, target, List.of(transfer, kick, leave, disband),
                2, reason);
    }

    private ActionState leaveState(SquadView viewed) {
        if (!canChangeSquad()) {
            return disabled(Action.LEAVE_SQUAD, Reason.of(ReasonCode.LEAVE_ACTIVE));
        }
        if (!snapshot.permissions().canLeaveSquad()) {
            return disabled(Action.LEAVE_SQUAD, Reason.of(ReasonCode.LEAVE_UNAVAILABLE));
        }
        if (pending(Action.LEAVE_SQUAD)) {
            return disabled(Action.LEAVE_SQUAD, Reason.of(ReasonCode.PENDING));
        }
        return new ActionState(Action.LEAVE_SQUAD, true, null, null, leaveConfirm(viewed),
                null, null, null, null);
    }

    private ActionState createState(SquadView viewed) {
        SquadCallsign callsign = viewed.callsign();
        int cooldown = kickCooldownSeconds(callsign);
        Reason reason = null;
        if (authority.inSquad()) {
            reason = Reason.of(ReasonCode.IN_SQUAD_CREATE,
                    SquadLabels.callsign(snapshot.ownSquad()));
        } else if (!canChangeSquad()) {
            reason = Reason.of(ReasonCode.SQUAD_CHANGE_ACTIVE);
        } else if (cooldown > 0) {
            reason = Reason.of(ReasonCode.KICK_COOLDOWN, cooldown);
        } else if (!snapshot.permissions().canCreateSquad()) {
            reason = Reason.of(ReasonCode.CREATE_UNAVAILABLE);
        } else if (pending(Action.CREATE_SQUAD)) {
            reason = Reason.of(ReasonCode.PENDING);
        }
        return reason != null
                ? new ActionState(Action.CREATE_SQUAD, false, reason, null, null, callsign,
                null, null, null)
                : new ActionState(Action.CREATE_SQUAD, true, null,
                Reason.of(ReasonCode.CREATE_HINT), null, callsign, null, null, null);
    }

    private ActionState joinState(SquadView viewed) {
        SquadCallsign callsign = viewed.callsign();
        int cooldown = kickCooldownSeconds(callsign);
        Reason reason = null;
        if (authority.inSquad()) {
            reason = Reason.of(ReasonCode.IN_SQUAD_JOIN,
                    SquadLabels.callsign(snapshot.ownSquad()));
        } else if (viewed.full()) {
            reason = Reason.of(ReasonCode.SQUAD_FULL);
        } else if (!canChangeSquad()) {
            reason = Reason.of(ReasonCode.SQUAD_CHANGE_ACTIVE);
        } else if (cooldown > 0) {
            reason = Reason.of(ReasonCode.KICK_COOLDOWN, cooldown);
        } else if (!snapshot.permissions().canJoinSquad()) {
            reason = Reason.of(ReasonCode.JOIN_UNAVAILABLE);
        } else if (pending(Action.JOIN_SQUAD)) {
            reason = Reason.of(ReasonCode.PENDING);
        }
        if (reason != null) {
            return new ActionState(Action.JOIN_SQUAD, false, reason, null, null, callsign,
                    null, null, null);
        }
        String defaultClass = defaultClassId();
        Reason hint = defaultClass.isEmpty() ? Reason.of(ReasonCode.JOIN_HINT_PLAIN)
                : Reason.of(ReasonCode.JOIN_HINT,
                SquadLabels.className(snapshot, defaultClass));
        return new ActionState(Action.JOIN_SQUAD, true, null, hint, null, callsign, null,
                null, null);
    }

    /** The formation's default class: from the context, else the class held outside a squad. */
    String defaultClassId() {
        if (snapshot == null) {
            return "";
        }
        String contextDefault = snapshot.formationContext().defaultClassId();
        if (!contextDefault.isEmpty()) {
            return contextDefault;
        }
        return authority.inSquad() ? "" : snapshot.viewerClassId();
    }

    private CommanderPanel buildCommanderPanel() {
        MemberView commander = null;
        SquadCallsign commanderSquad = null;
        if (snapshot != null && stage == Stage.READY) {
            for (SquadView squad : snapshot.squads()) {
                for (MemberView member : squad.members()) {
                    if (member.commander()) {
                        commander = member;
                        commanderSquad = squad.callsign();
                    }
                }
            }
        }
        if (stage != Stage.READY) {
            Reason reason = Reason.of(ReasonCode.VOTE_PENDING);
            return new CommanderPanel(null, null,
                    List.of(disabled(Action.CLAIM_COMMANDER, reason)), reason);
        }
        if (authority.commander()) {
            ActionState resign = pending(Action.RESIGN_COMMANDER)
                    ? disabled(Action.RESIGN_COMMANDER, Reason.of(ReasonCode.PENDING))
                    : new ActionState(Action.RESIGN_COMMANDER, true, null, null, null, null,
                    null, null, null);
            MemberView target = selectedMember;
            boolean validTarget = target != null && target.leader() && target.online()
                    && !target.playerId().equals(snapshot.viewerId());
            ActionState transfer;
            if (!validTarget) {
                transfer = disabled(Action.TRANSFER_COMMANDER,
                        Reason.of(ReasonCode.COMMANDER_TARGET));
            } else if (pending(Action.TRANSFER_COMMANDER)) {
                transfer = disabled(Action.TRANSFER_COMMANDER, Reason.of(ReasonCode.PENDING));
            } else {
                transfer = new ActionState(Action.TRANSFER_COMMANDER, true, null,
                        Reason.of(ReasonCode.COMMANDER_TRANSFER_HINT, target.name()),
                        transferCommanderConfirm(target), null, target.playerId(), null, null);
            }
            return new CommanderPanel(commander, commanderSquad, List.of(resign, transfer),
                    transfer.explanation());
        }
        Reason reason = null;
        if (!authority.inSquad()) {
            reason = Reason.of(ReasonCode.CLAIM_NO_SQUAD);
        } else if (!authority.leader()) {
            reason = Reason.of(ReasonCode.CLAIM_NOT_LEADER);
        } else if (commander != null) {
            reason = Reason.of(ReasonCode.COMMANDER_EXISTS);
        } else if (!snapshot.permissions().canClaimCommander()) {
            reason = Reason.of(ReasonCode.CLAIM_UNAVAILABLE);
        } else if (pending(Action.CLAIM_COMMANDER)) {
            reason = Reason.of(ReasonCode.PENDING);
        }
        ActionState claim = reason != null ? disabled(Action.CLAIM_COMMANDER, reason)
                : new ActionState(Action.CLAIM_COMMANDER, true, null,
                Reason.of(ReasonCode.CLAIM_HINT), null, null, null, null, null);
        return new CommanderPanel(commander, commanderSquad, List.of(claim),
                claim.explanation());
    }

    private List<ClassRow> buildClassRows() {
        if (snapshot == null || stage == Stage.NO_FACTION) {
            return List.of();
        }
        String current = currentClassId();
        SquadView own = authority.inSquad() ? snapshot.squad(snapshot.ownSquad()) : null;
        boolean canChangeClass = snapshot.deployment().canChangeClass();
        List<ClassRow> rows = new ArrayList<>(snapshot.classQuotas().size());
        for (ClassQuotaView quota : snapshot.classQuotas()) {
            boolean isCurrent = !current.isEmpty() && quota.classId().equals(current);
            List<MemberView> holders = own == null ? List.of() : own.members().stream()
                    .filter(member -> member.classId().equals(quota.classId())).toList();
            Reason reason = null;
            if (stage != Stage.READY) {
                reason = Reason.of(ReasonCode.VOTE_PENDING);
            } else if (quota.limit() <= 0) {
                reason = Reason.of(ReasonCode.CLASS_NOT_OPEN);
            } else if (isCurrent) {
                reason = Reason.of(ReasonCode.CLASS_CURRENT);
            } else if (!authority.inSquad()) {
                reason = Reason.of(ReasonCode.CLASS_NO_SQUAD);
            } else if (!canChangeClass) {
                reason = Reason.of(ReasonCode.CLASS_ACTIVE);
            } else if (quota.remaining() <= 0) {
                reason = Reason.of(ReasonCode.CLASS_FULL);
            } else if (pending(Action.SELECT_CLASS)) {
                reason = Reason.of(ReasonCode.PENDING);
            }
            ActionState select = reason != null
                    ? new ActionState(Action.SELECT_CLASS, false, reason, null, null, null,
                    null, quota.classId(), null)
                    : new ActionState(Action.SELECT_CLASS, true, null,
                    Reason.of(ReasonCode.CLASS_HINT, quota.remaining()), null, null, null,
                    quota.classId(), null);
            rows.add(new ClassRow(quota, isCurrent,
                    own == null ? null : own.classLimit(quota.classId()), holders, select));
        }
        return List.copyOf(rows);
    }

    private List<PointRow> buildPointRows() {
        if (snapshot == null) {
            return List.of();
        }
        DeploymentView deployment = snapshot.deployment();
        boolean active = deployment.phase() == DeploymentPhase.ACTIVE;
        int[] numbers = new int[DeploymentPointKind.values().length];
        List<PointRow> rows = new ArrayList<>(deployment.points().size());
        for (DeploymentPoint point : deployment.points()) {
            int number = ++numbers[point.kind().ordinal()];
            boolean selected = point.id().equals(deployment.selectedPointId());
            Reason reason = null;
            if (stage != Stage.READY) {
                reason = Reason.of(ReasonCode.VOTE_PENDING);
            } else if (selected) {
                reason = Reason.of(ReasonCode.POINT_SELECTED);
            } else if (active) {
                reason = Reason.of(ReasonCode.POINT_ACTIVE);
            } else if (pending(Action.SELECT_DEPLOYMENT_POINT)) {
                reason = Reason.of(ReasonCode.PENDING);
            }
            ActionState select = new ActionState(Action.SELECT_DEPLOYMENT_POINT,
                    reason == null, reason, null, null, null, null, null, point.id());
            rows.add(new PointRow(point, number, selected, select));
        }
        return List.copyOf(rows);
    }

    private ActionState buildDeploy() {
        if (snapshot == null || stage != Stage.READY) {
            return disabled(Action.DEPLOY, Reason.of(ReasonCode.VOTE_PENDING));
        }
        DeploymentView deployment = snapshot.deployment();
        if (deployment.canDeploy() && !pending(Action.DEPLOY)) {
            return new ActionState(Action.DEPLOY, true, null, Reason.of(ReasonCode.DEPLOY_HINT),
                    null, null, null, null, deployment.selectedPointId());
        }
        Reason reason;
        if (pending(Action.DEPLOY)) {
            reason = Reason.of(ReasonCode.PENDING);
        } else if (deployment.phase() == DeploymentPhase.ACTIVE) {
            reason = Reason.of(ReasonCode.DEPLOY_ACTIVE);
        } else if (deployment.phase() == DeploymentPhase.WAITING) {
            reason = Reason.of(ReasonCode.DEPLOY_WAITING, respawnSeconds());
        } else if (!authority.inSquad()) {
            reason = Reason.of(ReasonCode.DEPLOY_NO_SQUAD);
        } else if (deployment.points().isEmpty()) {
            reason = Reason.of(ReasonCode.DEPLOY_NO_POINTS);
        } else if (deployment.selectedPointId() == null) {
            reason = Reason.of(ReasonCode.DEPLOY_SELECT_POINT);
        } else if (deployment.points().stream()
                .noneMatch(point -> point.id().equals(deployment.selectedPointId()))) {
            reason = Reason.of(ReasonCode.DEPLOY_POINT_GONE);
        } else {
            reason = Reason.of(ReasonCode.DEPLOY_UNAVAILABLE);
        }
        return disabled(Action.DEPLOY, reason);
    }

    private ActionState buildRedeploy() {
        if (snapshot == null || stage != Stage.READY) {
            return disabled(Action.REDEPLOY, Reason.of(ReasonCode.VOTE_PENDING));
        }
        if (snapshot.deployment().phase() != DeploymentPhase.ACTIVE) {
            return disabled(Action.REDEPLOY, Reason.of(ReasonCode.REDEPLOY_NOT_ACTIVE));
        }
        if (pending(Action.REDEPLOY)) {
            return disabled(Action.REDEPLOY, Reason.of(ReasonCode.PENDING));
        }
        return new ActionState(Action.REDEPLOY, true, null,
                Reason.of(ReasonCode.REDEPLOY_HINT), redeployConfirm(), null, null, null, null);
    }

    private ActionState buildResupply() {
        if (snapshot == null || stage != Stage.READY) {
            return disabled(Action.RESUPPLY, Reason.of(ReasonCode.VOTE_PENDING));
        }
        DeploymentView deployment = snapshot.deployment();
        if (deployment.canResupply() && !pending(Action.RESUPPLY)) {
            return new ActionState(Action.RESUPPLY, true, null,
                    Reason.of(ReasonCode.RESUPPLY_HINT), null, null, null, null, null);
        }
        Reason reason;
        if (pending(Action.RESUPPLY)) {
            reason = Reason.of(ReasonCode.PENDING);
        } else if (deployment.phase() != DeploymentPhase.ACTIVE) {
            reason = Reason.of(ReasonCode.RESUPPLY_NOT_ACTIVE);
        } else if (deployment.resupplyTicks() > 0L) {
            reason = Reason.of(ReasonCode.RESUPPLY_COOLDOWN, resupplySeconds());
        } else {
            reason = Reason.of(ReasonCode.RESUPPLY_AWAY);
        }
        return disabled(Action.RESUPPLY, reason);
    }

    private List<ChecklistItem> buildChecklist() {
        if (snapshot == null || stage == Stage.NO_FACTION
                || snapshot.deployment().phase() == DeploymentPhase.ACTIVE) {
            return List.of();
        }
        if (stage == Stage.VOTE_PENDING) {
            boolean open = input.votePhase() == FormationVotePhase.OPEN;
            Component locked = Component.translatable(KEY_PREFIX + "check.after_lock");
            return List.of(
                    new ChecklistItem(CheckItem.FORMATION, CheckState.WAITING,
                            Component.translatable(KEY_PREFIX + (open ? "check.vote_open"
                                    : "check.vote_waiting"))),
                    new ChecklistItem(CheckItem.SQUAD, CheckState.LOCKED, locked),
                    new ChecklistItem(CheckItem.CLASS, CheckState.LOCKED,
                            Component.translatable(KEY_PREFIX + "check.class_after_lock")),
                    new ChecklistItem(CheckItem.POINT, CheckState.LOCKED, locked));
        }
        List<ChecklistItem> items = new ArrayList<>(4);
        SquadCallsign own = snapshot.ownSquad();
        items.add(own != null
                ? new ChecklistItem(CheckItem.SQUAD, CheckState.DONE, SquadLabels.callsign(own))
                : new ChecklistItem(CheckItem.SQUAD, CheckState.TODO,
                Component.translatable(KEY_PREFIX + "check.squad_todo")));
        String classId = currentClassId();
        MutableComponent className = SquadLabels.className(snapshot,
                classId.isEmpty() ? null : classId);
        items.add(new ChecklistItem(CheckItem.CLASS, CheckState.DONE, own != null ? className
                : Component.translatable(KEY_PREFIX + "check.class_default", className)));
        PointRow point = pointRows.stream().filter(PointRow::selected).findFirst().orElse(null);
        items.add(point != null
                ? new ChecklistItem(CheckItem.POINT, CheckState.DONE, point.title())
                : new ChecklistItem(CheckItem.POINT, CheckState.TODO,
                Component.translatable(KEY_PREFIX + "check.point_todo")));
        int seconds = respawnSeconds();
        items.add(seconds > 0
                ? new ChecklistItem(CheckItem.RESPAWN, CheckState.WAITING,
                Component.translatable(KEY_PREFIX + "check.respawn_wait", seconds))
                : new ChecklistItem(CheckItem.RESPAWN, CheckState.DONE,
                Component.translatable(KEY_PREFIX + "check.ready")));
        return List.copyOf(items);
    }

    // -------------------------------------------------------------------- confirmations

    private Confirm kickConfirm(MemberView target, SquadView squad) {
        List<Component> body = new ArrayList<>();
        body.add(Component.translatable(KEY_PREFIX + "confirm.kick.body", target.name(),
                SquadLabels.callsign(squad.callsign()), KICK_COOLDOWN_SECONDS));
        if (target.state().hasVitals()) {
            body.add(Component.translatable(KEY_PREFIX + "confirm.kick.active", target.name()));
        }
        return new Confirm(Action.KICK_MEMBER,
                Component.translatable(KEY_PREFIX + "confirm.kick.title", target.name()), body,
                Component.translatable(KEY_PREFIX + "confirm.kick.ok"), true);
    }

    private Confirm leaveConfirm(SquadView squad) {
        List<Component> body = new ArrayList<>();
        String defaultClass = snapshot.formationContext().defaultClassId();
        body.add(defaultClass.isEmpty()
                ? Component.translatable(KEY_PREFIX + "confirm.leave.body_plain",
                SquadLabels.callsign(squad.callsign()))
                : Component.translatable(KEY_PREFIX + "confirm.leave.body",
                SquadLabels.callsign(squad.callsign()),
                SquadLabels.className(snapshot, defaultClass)));
        if (authority.commander()) {
            body.add(Component.translatable(KEY_PREFIX + "confirm.leave.commander"));
        }
        if (authority.leader()) {
            MemberView successor = successorAfterLeaving(squad);
            body.add(successor == null
                    ? Component.translatable(KEY_PREFIX + "confirm.leave.last")
                    : Component.translatable(KEY_PREFIX + "confirm.leave.successor",
                    successor.name()));
        }
        return new Confirm(Action.LEAVE_SQUAD,
                Component.translatable(KEY_PREFIX + "confirm.leave.title",
                        SquadLabels.callsign(squad.callsign())), body,
                Component.translatable(KEY_PREFIX + "confirm.leave.ok"), true);
    }

    /**
     * Who the server makes leader when the viewer (the leader) leaves: the first online member
     * by join order, else the first member (BattleService.detachFromSquad). The roster arrives
     * leader first, then by join time.
     */
    MemberView successorAfterLeaving(SquadView squad) {
        List<MemberView> others = squad.members().stream()
                .filter(member -> !member.playerId().equals(snapshot.viewerId())).toList();
        return others.stream().filter(MemberView::online).findFirst()
                .orElse(others.isEmpty() ? null : others.get(0));
    }

    private Confirm disbandConfirm(SquadView squad) {
        List<MemberView> others = squad.members().stream()
                .filter(member -> !member.playerId().equals(snapshot.viewerId())).toList();
        List<Component> body = new ArrayList<>();
        body.add(Component.translatable(KEY_PREFIX + "confirm.disband.body",
                SquadLabels.callsign(squad.callsign()), others.size()));
        if (others.stream().anyMatch(member -> member.state().hasVitals())) {
            body.add(Component.translatable(KEY_PREFIX + "confirm.disband.active"));
        }
        squad.members().stream().filter(MemberView::commander).findFirst().ifPresent(
                commander -> body.add(Component.translatable(
                        KEY_PREFIX + "confirm.disband.commander", commander.name())));
        return new Confirm(Action.DISBAND_SQUAD,
                Component.translatable(KEY_PREFIX + "confirm.disband.title",
                        SquadLabels.callsign(squad.callsign())), body,
                Component.translatable(KEY_PREFIX + "confirm.disband.ok"), true);
    }

    private Confirm redeployConfirm() {
        return new Confirm(Action.REDEPLOY,
                Component.translatable(KEY_PREFIX + "confirm.redeploy.title"),
                List.of(Component.translatable(KEY_PREFIX + "confirm.redeploy.body")),
                Component.translatable(KEY_PREFIX + "confirm.redeploy.ok"), true);
    }

    private Confirm transferLeaderConfirm(MemberView target) {
        return new Confirm(Action.TRANSFER_LEADER,
                Component.translatable(KEY_PREFIX + "confirm.transfer_leader.title"),
                List.of(Component.translatable(KEY_PREFIX + "confirm.transfer_leader.body",
                        target.name(), SquadLabels.callsign(target.squad()))),
                Component.translatable(KEY_PREFIX + "confirm.transfer.ok"), false);
    }

    private Confirm transferCommanderConfirm(MemberView target) {
        return new Confirm(Action.TRANSFER_COMMANDER,
                Component.translatable(KEY_PREFIX + "confirm.transfer_commander.title"),
                List.of(Component.translatable(KEY_PREFIX + "confirm.transfer_commander.body",
                        target.name(), SquadLabels.callsign(target.squad()))),
                Component.translatable(KEY_PREFIX + "confirm.transfer.ok"), false);
    }

    // ------------------------------------------------------------------------- helpers

    private static ActionState disabled(Action action, Reason reason) {
        return new ActionState(action, false, reason, null, null, null, null, null, null);
    }

    private static int ticksToSeconds(long ticks) {
        return ticks <= 0L ? 0 : (int) Math.min(Integer.MAX_VALUE, (ticks + 19L) / 20L);
    }

    private static MutableComponent join(Component... parts) {
        MutableComponent result = Component.empty();
        for (int index = 0; index < parts.length; index++) {
            if (index > 0) {
                result.append(IDENTITY_SEPARATOR);
            }
            result.append(parts[index]);
        }
        return result;
    }

    /** Every language key this model can emit (for the translation contract test). */
    public static Set<String> translationKeys() {
        Set<String> keys = new TreeSet<>();
        for (Action action : Action.values()) {
            keys.add(action.labelKey());
            keys.add(action.shortLabelKey());
        }
        for (SquadStatus status : SquadStatus.values()) {
            if (status.hasLabel()) {
                keys.add(status.key());
            }
        }
        for (ReasonCode code : ReasonCode.values()) {
            keys.addAll(code.keys());
        }
        for (CheckItem item : CheckItem.values()) {
            keys.add(item.labelKey());
        }
        for (String suffix : List.of("count.none", "sub.pending", "sub.closed",
                "sub.closed" + SHORT_SUFFIX, "sub.leader", "sub.empty",
                "point.main_base", "point.field_beacon", "point.rally", "point.coords",
                "point.coords_full",
                "check.vote_open", "check.vote_waiting", "check.after_lock",
                "check.class_after_lock", "check.squad_todo", "check.class_default",
                "check.point_todo", "check.respawn_wait", "check.ready",
                "identity.vote_open", "identity.vote_open" + SHORT_SUFFIX,
                "identity.vote_waiting", "identity.vote_waiting" + SHORT_SUFFIX,
                "confirm.kick.title", "confirm.kick.body", "confirm.kick.active",
                "confirm.kick.ok", "confirm.leave.title", "confirm.leave.body",
                "confirm.leave.body_plain", "confirm.leave.commander", "confirm.leave.successor",
                "confirm.leave.last", "confirm.leave.ok", "confirm.disband.title",
                "confirm.disband.body", "confirm.disband.active", "confirm.disband.commander",
                "confirm.disband.ok", "confirm.redeploy.title", "confirm.redeploy.body",
                "confirm.redeploy.ok", "confirm.transfer_leader.title",
                "confirm.transfer_leader.body", "confirm.transfer_commander.title",
                "confirm.transfer_commander.body", "confirm.transfer.ok")) {
            keys.add(KEY_PREFIX + suffix);
        }
        // The pages' own texts (0.5.0-beta.1 B8 rendering).
        keys.addAll(SquadBoardText.translationKeys());
        return Set.copyOf(keys);
    }

    /** Argument count of each parameterized key in {@link #translationKeys()}. */
    public static Map<String, Integer> translationArgumentCounts() {
        Map<String, Integer> counts = new TreeMap<>();
        counts.put(Action.CREATE_SQUAD.labelKey(), 1);
        counts.put(Action.JOIN_SQUAD.labelKey(), 1);
        for (ReasonCode code : ReasonCode.values()) {
            for (int variant = 0; variant < 3; variant++) {
                if (code.argumentCount(variant) > 0) {
                    counts.put(code.keys().get(variant), code.argumentCount(variant));
                }
            }
        }
        counts.put(KEY_PREFIX + "sub.leader", 1);
        counts.put(KEY_PREFIX + "point.field_beacon", 1);
        counts.put(KEY_PREFIX + "point.coords", 3);
        counts.put(KEY_PREFIX + "point.coords_full", 4);
        counts.put(KEY_PREFIX + "check.class_default", 1);
        counts.put(KEY_PREFIX + "check.respawn_wait", 1);
        counts.put(KEY_PREFIX + "identity.vote_open", 1);
        counts.put(KEY_PREFIX + "identity.vote_open" + SHORT_SUFFIX, 1);
        counts.put(KEY_PREFIX + "identity.vote_waiting", 1);
        counts.put(KEY_PREFIX + "identity.vote_waiting" + SHORT_SUFFIX, 1);
        counts.put(KEY_PREFIX + "confirm.kick.title", 1);
        counts.put(KEY_PREFIX + "confirm.kick.body", 3);
        counts.put(KEY_PREFIX + "confirm.kick.active", 1);
        counts.put(KEY_PREFIX + "confirm.leave.title", 1);
        counts.put(KEY_PREFIX + "confirm.leave.body", 2);
        counts.put(KEY_PREFIX + "confirm.leave.body_plain", 1);
        counts.put(KEY_PREFIX + "confirm.leave.successor", 1);
        counts.put(KEY_PREFIX + "confirm.disband.title", 1);
        counts.put(KEY_PREFIX + "confirm.disband.body", 2);
        counts.put(KEY_PREFIX + "confirm.disband.commander", 1);
        counts.put(KEY_PREFIX + "confirm.transfer_leader.body", 2);
        counts.put(KEY_PREFIX + "confirm.transfer_commander.body", 2);
        SquadBoardText.translationArgumentCounts().forEach((key, count) -> {
            if (counts.put(key, count) != null) {
                throw new IllegalStateException("Squad board key declared twice: " + key);
            }
        });
        return Collections.unmodifiableMap(counts);
    }

    static EnumSet<Action> confirmedActions() {
        EnumSet<Action> result = EnumSet.noneOf(Action.class);
        for (Action action : Action.values()) {
            if (action.requiresConfirm()) {
                result.add(action);
            }
        }
        return result;
    }
}
