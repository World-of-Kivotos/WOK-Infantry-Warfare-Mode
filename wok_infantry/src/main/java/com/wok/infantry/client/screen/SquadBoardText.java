package com.wok.infantry.client.screen;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Language keys of the battle terminal's squad, class and deployment pages (0.5.0-beta.1 B8),
 * next to the operation keys of {@link SquadBoardModel}. Every key is registered with its
 * argument count, so the translation contract test can check both languages without the keys
 * having to appear as literals. Sentences that wrap come as a list, longest first, so a narrow
 * column takes a shorter form instead of cutting a sentence in half.
 */
final class SquadBoardText {
    static final String PREFIX = SquadBoardModel.KEY_PREFIX;

    private static final Map<String, Integer> COUNTS = new TreeMap<>();

    // ---- shell ----------------------------------------------------------------------------------
    static final String TITLE = reg("title", 0);
    static final String TITLE_SHORT = reg("title_short", 0);
    static final String HINT_REFRESH = reg("hint.refresh", 0);
    static final String SYNC_TITLE = reg("sync.title", 0);
    static final String SYNC_HINT = reg("sync.hint", 0);
    static final String NOFACTION_TITLE = reg("nofaction.title", 0);
    static final String NOFACTION_HINT = reg("nofaction.hint", 0);
    static final String NOFACTION_OPEN = reg("nofaction.open", 0);

    // ---- squad list / strip -----------------------------------------------------------------------
    static final String LIST_META = reg("list.meta", 2);
    static final String LIST_META_SHORT = reg("list.meta_short", 1);
    static final String VOTE_META_OPEN = reg("vote.meta_open", 0);
    static final String VOTE_META_OPEN_SHORT = reg("vote.meta_open_short", 0);
    static final String VOTE_META_WAIT = reg("vote.meta_wait", 0);
    static final String VOTE_META_WAIT_SHORT = reg("vote.meta_wait_short", 0);
    static final String ROW_TOOLTIP = reg("list.tooltip", 3);

    // ---- roster -----------------------------------------------------------------------------------
    static final String ROSTER_TITLE_OWN = reg("roster.title_own", 1);
    static final String ROSTER_CLOSED_TITLE = reg("roster.closed_title", 1);
    static final String ROSTER_CLOSED_HINT = reg("roster.closed_hint", 2);
    static final String ROSTER_CLOSED_HINT_SHORT = reg("roster.closed_hint_short", 1);
    static final String ROSTER_CLOSED_HINT_TINY = reg("roster.closed_hint_tiny", 0);
    static final String ROSTER_EMPTY_TITLE = reg("roster.empty_title", 1);
    static final String ROSTER_EMPTY_HINT = reg("roster.empty_hint", 0);
    static final String ROSTER_EMPTY_HINT_SHORT = reg("roster.empty_hint_short", 0);
    static final String ROSTER_EMPTY_OTHER = reg("roster.empty_hint_other", 0);
    static final String ROSTER_EMPTY_OTHER_SHORT = reg("roster.empty_hint_other_short", 0);
    static final String ROSTER_EMPTY_OTHER_TINY = reg("roster.empty_hint_other_tiny", 0);
    static final String ROSTER_EMPTY_SLOT = reg("roster.empty_slot", 0);
    static final String ROSTER_YOU = reg("roster.you", 0);
    static final String ROSTER_CLASSES = reg("roster.classes", 0);
    static final String ROSTER_CLASS_CHIP = reg("roster.class_chip", 3);
    static final String ROSTER_TARGET = reg("roster.target", 0);
    static final String ROSTER_TARGET_NONE = reg("roster.target_none", 0);
    static final String LIST_SEPARATOR = reg("list_separator", 0);
    static final String MEMBER_DEAD_WAITING = reg("member.dead_waiting", 0);
    static final String MEMBER_DEPLOYED = reg("member.deployed", 0);
    static final String MEMBER_TOOLTIP = reg("member.tooltip", 3);

    // ---- commander / status / faction ---------------------------------------------------------------
    static final String COMMANDER_TITLE = reg("commander.title", 0);
    static final String COMMANDER_CURRENT = reg("commander.current", 0);
    static final String COMMANDER_SQUAD = reg("commander.squad", 0);
    static final String COMMANDER_VACANT = reg("commander.vacant", 0);
    static final String[] COMMANDER_NOTE = regAll("commander.note", 0, 3);
    static final String MINE_TITLE = reg("mine.title", 0);
    static final String MINE_FACTION = reg("mine.faction", 0);
    static final String MINE_FORMATION = reg("mine.formation", 0);
    static final String MINE_SQUAD = reg("mine.squad", 0);
    static final String MINE_ROLE = reg("mine.role", 0);
    static final String MINE_CLASS = reg("mine.class", 0);
    static final String MINE_DEPLOY = reg("mine.deploy", 0);
    static final String MINE_FORMATION_VALUE = reg("mine.formation_value", 1);
    static final String MINE_VOTE_OPEN = reg("mine.vote_open", 0);
    static final String MINE_VOTE_WAIT = reg("mine.vote_wait", 0);
    static final String MINE_CLASS_AFTER_LOCK = reg("mine.class_after_lock", 0);
    static final String MINE_WAITING = reg("mine.waiting", 1);
    static final String MINE_READY = reg("mine.ready", 0);
    static final String MINE_ACTIVE = reg("mine.active", 0);
    static final String FACTION_TITLE = reg("faction.title", 0);
    static final String FACTION_META = reg("faction.meta", 2);
    static final String FACTION_OWN = reg("faction.own", 1);
    static final String FACTION_OWN_WITH = reg("faction.own_with", 2);

    // ---- vote block --------------------------------------------------------------------------------
    static final String VOTE_TITLE = reg("vote.title", 0);
    static final String VOTE_META_CHANGE = reg("vote.meta_change", 0);
    static final String VOTE_META_RUNNING = reg("vote.meta_running", 0);
    /** Arguments: voted, total, faction. */
    static final String[] VOTE_BLOCK_OPEN_ALL = {reg("vote.block_open", 3),
            reg("vote.block_open_short", 2), reg("vote.block_open_tiny", 2)};
    /** Argument: faction. */
    static final String[] VOTE_BLOCK_WAIT_ALL = {reg("vote.block_wait", 1),
            reg("vote.block_wait_short", 0), reg("vote.block_wait_tiny", 0)};
    static final String[] VOTE_HINT_OPEN = regAll("vote.hint_open", 0, 3);
    static final String[] VOTE_HINT_WAIT = regAll("vote.hint_wait", 0, 3);
    static final String VOTE_OPEN = reg("vote.open", 0);
    static final String VOTE_VIEW = reg("vote.view", 0);
    static final String VOTE_TALLY_OPEN = reg("vote.tally_open", 1);
    static final String VOTE_TALLY_OPEN_SHORT = reg("vote.tally_open_short", 1);
    static final String VOTE_TALLY_WAIT = reg("vote.tally_wait", 1);
    static final String VOTE_TALLY_WAIT_SHORT = reg("vote.tally_wait_short", 1);
    static final String VOTE_VOTES = reg("vote.votes", 1);
    static final String VOTE_VOTES_SHARE = reg("vote.votes_share", 2);
    static final String VOTE_LEADING = reg("vote.leading", 0);
    static final String VOTE_VEHICLES = reg("vote.vehicles", 2);
    static final String VOTE_VEHICLE = reg("vote.vehicle", 2);
    static final String VOTE_NO_VEHICLES = reg("vote.no_vehicles", 0);
    static final String VOTE_MINE = reg("vote.mine", 0);
    static final String VOTE_MINE_NONE = reg("vote.mine_none", 0);
    static final String VOTE_MINE_CLOSED = reg("vote.mine_closed", 0);
    static final String VOTE_DUE = reg("vote.due", 0);
    static final String VOTE_DUE_OPEN = reg("vote.due_open", 0);
    static final String VOTE_DUE_WAIT = reg("vote.due_wait", 0);
    static final String VOTE_LEAD_NONE = reg("vote.lead_none", 0);
    static final String VOTE_LEAD_TIE = reg("vote.lead_tie", 2);
    static final String VOTE_LEAD_ONE = reg("vote.lead_one", 2);
    static final String VOTE_AVAILABLE = reg("vote.available", 0);
    static final String VOTE_AVAILABLE_VALUE = reg("vote.available_value", 1);
    static final String VOTE_SYNC = reg("vote.sync", 0);
    static final String VOTE_SYNC_HINT = reg("vote.sync_hint", 0);
    static final String VOTE_CHANGE = reg("vote.change", 0);
    static final String VOTE_FINAL = reg("vote.final", 0);

    // ---- flow and rules ----------------------------------------------------------------------------
    static final String FLOW_TITLE = reg("flow.title", 0);
    static final String[] FLOW_META = regAll("flow.meta", 0, 2);
    static final String FLOW_JOIN = reg("flow.join", 0);
    static final String FLOW_OPEN = reg("flow.open", 0);
    static final String FLOW_OPEN_DONE = reg("flow.open_done", 0);
    static final String FLOW_OPEN_WAIT = reg("flow.open_wait", 0);
    static final String FLOW_VOTE = reg("flow.vote", 0);
    static final String FLOW_VOTE_OPEN = reg("flow.vote_open", 2);
    static final String FLOW_VOTE_WAIT = reg("flow.vote_wait", 0);
    static final String FLOW_LOCK = reg("flow.lock", 0);
    static final String[] FLOW_LOCK_VALUE = regAll("flow.lock_value", 0, 3);
    static final String FLOW_AFTER = reg("flow.after", 0);
    static final String[] FLOW_AFTER_VALUE = regAll("flow.after_value", 0, 3);
    static final String FLOW_SQUAD = reg("flow.squad", 0);
    static final String[] FLOW_SQUAD_VALUE = regAll("flow.squad_value", 0, 3);
    static final String FLOW_DEPLOY = reg("flow.deploy", 0);
    static final String[] FLOW_DEPLOY_VALUE = regAll("flow.deploy_value", 0, 3);
    static final String RULES_HOW = reg("rules.how", 0);
    static final String[] RULE_HOW_VOTE = regAll("rules.how_vote", 0, 3);
    static final String[] RULE_HOW_CHANGE = regAll("rules.how_change", 0, 2);
    static final String[] RULE_HOW_FINAL = regAll("rules.how_final", 0, 2);
    static final String[] RULE_HOW_OWN = regAll("rules.how_own", 0, 2);
    static final String[] RULE_HOW_SHARED = regAll("rules.how_shared", 0, 3);
    static final String[] RULE_HOW_WAIT_VOTE = regAll("rules.how_wait_vote", 0, 3);
    static final String[] RULE_HOW_WAIT_CANDIDATES = regAll("rules.how_wait_candidates", 0, 2);
    static final String LOCK_RULES_TITLE = reg("rules.lock_title", 0);
    static final String[] LOCK_RULE_RESET = regAll("rules.lock_reset", 0, 3);
    static final String[] LOCK_RULE_QUOTA = regAll("rules.lock_quota", 0, 3);
    static final String[] LOCK_RULE_CHANGE = regAll("rules.lock_change", 0, 3);
    static final String[] LOCK_RULE_LOADOUT = regAll("rules.lock_loadout", 0, 3);
    static final String POINT_RULES_TITLE = reg("rules.point_title", 0);
    static final String[] POINT_RULE_MAIN = regAll("rules.point_main", 0, 3);
    static final String[] POINT_RULE_BEACON = regAll("rules.point_beacon", 0, 3);
    static final String[] POINT_RULE_RALLY = regAll("rules.point_rally", 0, 3);

    // ---- classes ----------------------------------------------------------------------------------
    static final String CLASSES_TITLE = reg("classes.title", 0);
    static final String CLASSES_META = reg("classes.meta", 2);
    static final String CLASSES_VOTE_BLOCK = reg("classes.vote_block", 0);
    static final String[] CLASSES_VOTE_HINT = regAll("classes.vote_hint", 0, 3);
    static final String CLASSES_STATUS_NO_SQUAD = reg("classes.status_no_squad", 0);
    static final String CLASSES_STATUS_ACTIVE = reg("classes.status_active", 0);
    static final String CLASSES_STATUS_PICK = reg("classes.status_pick", 0);
    static final String CLASSES_HINT_CLOSED = reg("classes.hint_closed", 0);
    static final String CLASSES_HINT_CURRENT = reg("classes.hint_current", 0);
    static final String CLASSES_HINT_CURRENT_DEFAULT = reg("classes.hint_current_default", 0);
    static final String CLASSES_HINT_NO_SQUAD = reg("classes.hint_no_squad", 0);
    static final String CLASSES_HINT_ACTIVE = reg("classes.hint_active", 0);
    static final String CLASSES_HINT_FULL = reg("classes.hint_full", 0);
    static final String CLASSES_PER_SQUAD = reg("classes.per_squad", 1);
    static final String CLASSES_WHO_NO_SQUAD = reg("classes.who_no_squad", 0);
    static final String CLASSES_WHO_NONE = reg("classes.who_none", 0);
    static final String CLASSES_RULES_TITLE = reg("classes.rules_title", 0);
    static final String CLASS_RULE_QUOTA = reg("classes.rule_quota", 1);
    static final String CLASS_RULE_QUOTA_SHORT = reg("classes.rule_quota_short", 0);
    static final String CLASS_RULE_QUOTA_TINY = reg("classes.rule_quota_tiny", 0);
    static final String[] CLASS_RULE_CHANGE = regAll("classes.rule_change", 0, 3);
    static final String CLASS_RULE_RESET = reg("classes.rule_reset", 1);
    static final String CLASS_RULE_RESET_SHORT = reg("classes.rule_reset_short", 1);
    static final String CLASS_RULE_RESET_TINY = reg("classes.rule_reset_tiny", 0);
    static final String[] CLASS_RULE_LOADOUT = regAll("classes.rule_loadout", 0, 3);
    static final String[] CLASSES_NOTE = regAll("classes.note", 0, 3);
    static final String SLOTS_TITLE = reg("classes.slots_title", 0);
    static final String[] SLOTS_META = regAll("classes.slots_meta", 0, 2);
    static final String SLOTS_PEOPLE = reg("classes.slots_people", 0);
    static final String CURRENT_TITLE = reg("current.title", 0);
    static final String CURRENT_PENDING = reg("current.pending", 0);
    static final String CURRENT_QUOTA = reg("current.quota", 0);
    static final String CURRENT_CHANGE = reg("current.change", 0);
    static final String CURRENT_CHANGE_NO_SQUAD = reg("current.change_no_squad", 0);
    static final String CURRENT_CHANGE_ACTIVE = reg("current.change_active", 0);
    static final String CURRENT_CHANGE_WAITING = reg("current.change_waiting", 0);
    static final String[] CURRENT_COMPACT_NO_SQUAD = regAll("current.compact_no_squad", 0, 2);
    static final String CURRENT_COMPACT = reg("current.compact", 3);
    static final String CURRENT_COMPACT_ACTIVE = reg("current.compact_active", 0);
    static final String CURRENT_COMPACT_WAITING = reg("current.compact_waiting", 0);
    static final String CURRENT_VOTE_CLASS = reg("current.vote_class", 0);
    static final String CURRENT_VOTE_CHANGE = reg("current.vote_change", 0);
    static final String CURRENT_VOTE_NARROW = reg("current.vote_narrow", 0);
    static final String CURRENT_VOTE_NONE = reg("current.vote_none", 0);
    static final String KIT_TITLE = reg("kit.title", 0);
    static final String KIT_META = reg("kit.meta", 1);
    static final String KIT_SLOT = reg("kit.slot", 2);
    static final String KIT_HOTBAR = reg("kit.hotbar", 1);
    static final String KIT_OFFHAND = reg("kit.offhand", 0);
    static final String KIT_ARMOR = reg("kit.armor", 0);
    static final String KIT_EMPTY = reg("kit.empty", 0);
    static final String KIT_COUNT = reg("kit.count", 2);
    static final String[] NOTE_KIT = regAll("current.note_kit", 0, 3);
    static final String[] NOTE_NO_KIT = regAll("current.note_nokit", 0, 3);

    // ---- deployment -------------------------------------------------------------------------------
    static final String POINTS_META_ACTIVE = reg("points.meta_active", 0);
    static final String POINTS_META_PAGES = reg("points.meta_pages", 3);
    static final String POINTS_META_COUNT = reg("points.meta_count", 1);
    static final String POINTS_TAG_SELECTED = reg("points.tag_selected", 0);
    static final String POINTS_TAG_HERE = reg("points.tag_here", 0);
    static final String POINTS_SUPPLY = reg("points.supply", 0);
    static final String POINTS_DETAIL_MAIN = reg("points.detail_main", 1);
    static final String POINTS_DETAIL_BEACON = reg("points.detail_beacon", 0);
    static final String POINTS_DETAIL_RALLY = reg("points.detail_rally", 0);
    static final String[] POINTS_NOTE_ACTIVE = regAll("points.note_active", 0, 3);
    static final String[] POINTS_NOTE_WAITING = regAll("points.note_waiting", 0, 4);
    static final String POINTS_VOTE_BLOCK = reg("points.vote_block", 0);
    static final String[] POINTS_VOTE_HINT = regAll("points.vote_hint", 0, 3);
    static final String POINTS_TOOLTIP = reg("points.tooltip", 2);
    static final String MAP_TITLE = reg("map.title", 0);
    static final String MAP_META = reg("map.meta", 0);
    static final String MAP_LABEL_SELECTED = reg("map.label_selected", 1);
    static final String MAP_LABEL_HERE = reg("map.label_here", 1);
    static final String MAP_SCALE = reg("map.scale", 1);
    static final String DEPLOY_PHASE_VOTE = reg("deploy.phase_vote", 0);
    static final String DEPLOY_PHASE_ACTIVE = reg("deploy.phase_active", 0);
    static final String DEPLOY_PHASE_WAITING = reg("deploy.phase_waiting", 0);
    static final String DEPLOY_PHASE_READY = reg("deploy.phase_ready", 0);
    static final String DEPLOY_DEPLOYED_AT = reg("deploy.deployed_at", 1);
    static final String DEPLOY_SECONDS = reg("deploy.seconds", 1);
    static final String DEPLOY_COMBAT = reg("deploy.combat", 0);
    static final String DEPLOY_COMBAT_META = reg("deploy.combat_meta", 0);
    static final String[] DEPLOY_WHY_ACTIVE = regAll("deploy.why_active", 1, 3);
    static final String DEPLOY_WHY_WAITING = reg("deploy.why_waiting", 0);
    static final String DEPLOY_COOLDOWN = reg("deploy.cooldown", 0);
    static final String MATES_TITLE = reg("mates.title", 0);
    static final String MATES_META = reg("mates.meta", 2);
    static final String DEPLOY_RULES_TITLE = reg("deploy.rules_title", 0);
    static final String[] RULE_REDEPLOY = regAll("deploy.rule_redeploy", 0, 3);
    static final String[] RULE_RESUPPLY = regAll("deploy.rule_resupply", 1, 3);
    static final String[] RULE_POINT_WAITING = regAll("deploy.rule_point_waiting", 0, 2);
    static final String[] RULE_AFTER_LOCK = regAll("deploy.rule_after_lock", 0, 2);
    static final String[] RULE_ISSUE = regAll("deploy.rule_issue", 0, 3);
    static final String[] RULE_AUTO_CLOSE = regAll("deploy.rule_auto_close", 0, 2);
    static final String DEPLOY_COMPACT_ACTIVE = reg("deploy.compact_active", 1);
    static final String DEPLOY_COMPACT_VOTE = reg("deploy.compact_vote", 0);
    static final String DEPLOY_COMPACT_WAIT = reg("deploy.compact_wait", 0);
    static final String DEPLOY_REASON_ACTIVE = reg("deploy.reason_active", 1);
    static final String[] DEPLOY_REASON_VOTE = regAll("deploy.reason_vote", 0, 2);
    static final String DEPLOY_REASON_WAIT = reg("deploy.reason_wait", 1);
    static final String DEPLOY_REASON_COMBINED = reg("deploy.reason_combined", 1);

    private SquadBoardText() {
    }

    private static String reg(String suffix, int arguments) {
        String key = PREFIX + suffix;
        if (COUNTS.put(key, arguments) != null) {
            throw new IllegalStateException("Squad board text key declared twice: " + key);
        }
        return key;
    }

    /** {@code count} keys {@code base}, {@code base_short}, {@code base_tiny}, {@code base_tiny2}. */
    private static String[] regAll(String base, int arguments, int count) {
        String[] suffixes = {"", SquadBoardModel.SHORT_SUFFIX, SquadBoardModel.TINY_SUFFIX,
                "_tiny2"};
        String[] keys = new String[count];
        for (int index = 0; index < count; index++) {
            keys[index] = reg(base + suffixes[index], arguments);
        }
        return keys;
    }

    static MutableComponent t(String key, Object... args) {
        return Component.translatable(key, args);
    }

    /** Every variant of {@code keys} with the same arguments, longest first. */
    static List<Component> all(String[] keys, Object... args) {
        List<Component> result = new ArrayList<>(keys.length);
        for (String key : keys) {
            result.add(Component.translatable(key, args));
        }
        return List.copyOf(result);
    }

    static List<Component> list(Component... variants) {
        List<Component> result = new ArrayList<>(variants.length);
        for (Component variant : variants) {
            if (variant != null) {
                result.add(variant);
            }
        }
        return List.copyOf(result);
    }

    /** Joins {@code parts} with the language's list separator ("、" / ", "). */
    static MutableComponent joined(List<? extends Component> parts) {
        MutableComponent result = Component.empty();
        for (int index = 0; index < parts.size(); index++) {
            if (index > 0) {
                result.append(Component.translatable(LIST_SEPARATOR));
            }
            result.append(parts.get(index));
        }
        return result;
    }

    /** Every key this class can emit (for the translation contract test). */
    static Set<String> translationKeys() {
        return Collections.unmodifiableSet(new TreeSet<>(COUNTS.keySet()));
    }

    /** Argument count of every parameterized key. */
    static Map<String, Integer> translationArgumentCounts() {
        Map<String, Integer> result = new TreeMap<>();
        COUNTS.forEach((key, count) -> {
            if (count > 0) {
                result.put(key, count);
            }
        });
        return Collections.unmodifiableMap(result);
    }
}
