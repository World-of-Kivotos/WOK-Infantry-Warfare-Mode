package com.wok.infantry.client.hud;

import com.wok.infantry.battle.MemberState;
import com.wok.infantry.battle.MemberView;
import com.wok.infantry.battle.SquadView;
import com.wok.infantry.client.screen.SquadLabels;
import com.wok.infantry.client.screen.TacticalBoardTheme;
import com.wok.infantry.client.screen.TextFit;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Text and columns of the HUD squad roster (preview {@code kit/hud-parts.js} {@code roster}),
 * computed once per battle snapshot instead of every frame. Pure: names, translations and text
 * widths come in as functions, so the column rules are unit-tested.
 *
 * <p>Row: status dot, number 1–8 (the same numbers as the map), role tags, name, then one
 * status column: a healthy member gets a short health bar and the class, a member with a status
 * gets the status word ("倒地", "阵亡", "待部署", "离线") in its place. The bars form one aligned
 * column at least 3px clear of the names. A member without a trusted health ratio (body-health
 * installed, ratio −1) shows the class only.
 */
public final class SquadRosterModel {
    /** Status dot x, relative to the plate's left edge. */
    public static final int DOT_X = 5;
    /** Member number x, relative to the plate's left edge. */
    public static final int NUMBER_X = 10;
    /** Role tags (then the name) start here, relative to the plate's left edge. */
    public static final int NAME_X = 17;
    public static final int ROLE_GAP = 2;
    public static final int RIGHT_INSET = 4;
    public static final int BAR_WIDTH_NARROW = 12;
    public static final int BAR_WIDTH = 20;
    public static final int COLUMN_GAP = 3;
    /** Title starts 5px into the plate; the count keeps 4px from the right edge. */
    public static final int TITLE_X = 5;

    public static final String DOWNED_KEY = "hud.wok_infantry.member.downed";
    public static final String DEAD_KEY = "hud.wok_infantry.member.dead";
    public static final String WAITING_KEY = "hud.wok_infantry.member.waiting";
    public static final String WAITING_SHORT_KEY = "hud.wok_infantry.member.waiting_short";
    public static final String OFFLINE_KEY = "hud.wok_infantry.member.offline";

    private SquadRosterModel() {
    }

    /** Dot colour, hollow dot, and the status word keys of a member state (null = no word). */
    public record StatusStyle(int color, boolean hollow, String tagKey, String shortTagKey) {
        public boolean hasTag() {
            return tagKey != null;
        }
    }

    /**
     * Status dot of each state: deployed green, downed orange, dead red, waiting to deploy a
     * hollow neutral dot, offline gray. Every state but "deployed" also writes its word, so the
     * roster never relies on colour alone.
     */
    public static StatusStyle style(MemberState state) {
        MemberState safe = state == null ? MemberState.OFFLINE : state;
        return switch (safe) {
            case DEPLOYED -> new StatusStyle(TacticalBoardTheme.SUCCESS_B, false, null, null);
            case DOWNED -> new StatusStyle(TacticalBoardTheme.ACCENT_B, false, DOWNED_KEY,
                    DOWNED_KEY);
            case DEAD -> new StatusStyle(TacticalBoardTheme.DANGER_B, false, DEAD_KEY, DEAD_KEY);
            case WAITING -> new StatusStyle(TacticalBoardTheme.NEUTRAL_B, true, WAITING_KEY,
                    WAITING_SHORT_KEY);
            case OFFLINE -> new StatusStyle(TacticalBoardTheme.OFFLINE, false, OFFLINE_KEY,
                    OFFLINE_KEY);
        };
    }

    /** Name colour: offline gray, dead muted red, everyone else light. */
    public static int nameColor(MemberState state) {
        if (state == MemberState.OFFLINE) {
            return TacticalBoardTheme.OFFLINE;
        }
        if (state == MemberState.DEAD) {
            return TacticalHud.mix(TacticalBoardTheme.LIGHT_MUTED, TacticalBoardTheme.DANGER_B, 0.3D);
        }
        return TacticalBoardTheme.LIGHT;
    }

    /** One roster row; x values are relative to the plate's left edge. */
    public record Row(int number, UUID playerId, MemberState state, int dotColor, boolean hollow,
                      String role, int nameX, String name, boolean nameTruncated,
                      String fullName, int nameColor, String tag, int tagColor, String className,
                      boolean healthBar, float healthRatio, boolean self) {
        public boolean hasTag() {
            return !tag.isEmpty();
        }
    }

    /**
     * Roster of one squad as drawn: title (ellipsized), member count over the squad's own
     * capacity, rows and the shared status column (x relative to the plate's left edge).
     */
    public record Roster(String title, boolean titleTruncated, String count, int countX,
                         List<Row> rows, boolean narrow, int width,
                         int columnLeft, int barLeft, int barRight) {
        public Roster {
            rows = List.copyOf(rows);
        }
    }

    /** Translated words the roster writes (resolved once per language). */
    public record Labels(String leader, String commander, String downed, String dead,
                         String waiting, String waitingShort, String offline) {
        public static Labels translated() {
            return new Labels(text(SquadLabels.LEADER_SHORT_KEY),
                    text(SquadLabels.COMMANDER_SHORT_KEY), text(DOWNED_KEY), text(DEAD_KEY),
                    text(WAITING_KEY), text(WAITING_SHORT_KEY), text(OFFLINE_KEY));
        }

        private static String text(String key) {
            return Component.translatable(key).getString();
        }

        String tag(String key) {
            if (key == null) {
                return "";
            }
            return switch (key) {
                case DOWNED_KEY -> downed;
                case DEAD_KEY -> dead;
                case WAITING_KEY -> waiting;
                case WAITING_SHORT_KEY -> waitingShort;
                case OFFLINE_KEY -> offline;
                default -> "";
            };
        }
    }

    /**
     * Builds the roster of {@code squad} (server order kept, at most
     * {@link WokHudLayout#MAX_ROSTER_ROWS} members).
     *
     * @param title     squad name as shown (the full call sign)
     * @param className display class name of a member
     * @param width     text width function of the HUD font
     */
    public static Roster build(SquadView squad, UUID viewerId, boolean narrow, String title,
                               Function<MemberView, String> className, Labels labels,
                               ToIntFunction<String> width) {
        Objects.requireNonNull(squad, "squad");
        int plateWidth = WokHudLayout.rosterWidth(narrow);
        List<MemberView> members = squad.members().stream()
                .limit(WokHudLayout.MAX_ROSTER_ROWS).toList();

        String count = SquadLabels.memberCountText(squad.members().size(), squad.capacity());
        int countWidth = width.applyAsInt(count);
        int countX = plateWidth - RIGHT_INSET - countWidth;
        // preview: title room = width − 10 − count width − 4, i.e. 5px clear of the count
        TextFit.Plain fittedTitle = TextFit.fitPlain(Objects.requireNonNullElse(title, ""),
                countX - TITLE_X - 5, width);

        List<String> classes = new ArrayList<>(members.size());
        List<StatusStyle> styles = new ArrayList<>(members.size());
        int columnWidth = 0;
        int maxColumn = plateWidth / 3;
        for (MemberView member : members) {
            StatusStyle style = style(member.state());
            styles.add(style);
            String name = Objects.requireNonNullElse(className.apply(member), "");
            String shown = narrow ? firstCodePoint(name) : name;
            classes.add(shown);
            if (!style.hasTag()) {
                columnWidth = Math.max(columnWidth, Math.min(maxColumn, width.applyAsInt(shown)));
            }
        }
        int columnLeft = plateWidth - RIGHT_INSET - columnWidth;
        int barRight = columnLeft - COLUMN_GAP;
        int barLeft = barRight - (narrow ? BAR_WIDTH_NARROW : BAR_WIDTH);
        int tagRoom = plateWidth - RIGHT_INSET - barLeft;

        List<Row> rows = new ArrayList<>(members.size());
        for (int index = 0; index < members.size(); index++) {
            MemberView member = members.get(index);
            StatusStyle style = styles.get(index);
            String role = narrow ? SquadLabels.roleSymbols(member.leader(), member.commander())
                    : roleWords(member.leader(), member.commander(), labels);
            int nameX = NAME_X + (role.isEmpty() ? 0 : width.applyAsInt(role) + ROLE_GAP);
            TextFit.Plain name = TextFit.fitPlain(member.name(),
                    Math.max(0, barLeft - COLUMN_GAP - nameX), width);
            String tag = "";
            if (style.hasTag()) {
                String full = labels.tag(style.tagKey());
                tag = width.applyAsInt(full) <= tagRoom ? full : labels.tag(style.shortTagKey());
                tag = TextFit.fitPlain(tag, tagRoom, width).text();
            }
            String shownClass = style.hasTag() ? ""
                    : TextFit.fitPlain(classes.get(index), plateWidth - RIGHT_INSET - columnLeft,
                    width).text();
            boolean bar = !style.hasTag() && member.hasHealthRatio();
            rows.add(new Row(index + 1, member.playerId(), member.state(), style.color(),
                    style.hollow(), role, nameX, name.text(), name.truncated(), member.name(),
                    nameColor(member.state()), tag, style.color(), shownClass, bar,
                    bar ? member.healthRatio() : MemberView.UNKNOWN_HEALTH_RATIO,
                    member.playerId().equals(viewerId)));
        }
        return new Roster(fittedTitle.text(), fittedTitle.truncated(), count, countX, rows,
                narrow, plateWidth, columnLeft, barLeft, barRight);
    }

    /** "队长", "指挥" or "队长·指挥" in the current language. */
    static String roleWords(boolean leader, boolean commander, Labels labels) {
        if (leader && commander) {
            return labels.leader() + SquadLabels.ROLE_SEPARATOR + labels.commander();
        }
        return leader ? labels.leader() : commander ? labels.commander() : "";
    }

    /** First code point (a class initial on narrow screens); empty for an empty name. */
    static String firstCodePoint(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.substring(0, text.offsetByCodePoints(0, 1));
    }
}
