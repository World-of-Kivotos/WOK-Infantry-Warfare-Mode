package com.wok.infantry.client.hud;

import com.wok.infantry.client.ClientFormationState.LockTransition;
import com.wok.infantry.client.screen.TacticalBoardTheme;
import com.wok.infantry.client.screen.TacticalIcon;
import com.wok.infantry.formation.selection.FactionSelectionView;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.formation.selection.FormationSelectionView;
import com.wok.infantry.formation.vote.FormationVotePhase;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

/**
 * Pre-battle formation ballot plate and the 3-second lock notice (preview
 * {@code kit/hud-parts.js} {@code vote}, {@code surfaces/10-hud.js} notes). Each faction plays
 * one formation per match: members vote, an administrator locks it, the whole faction shares
 * it. Until the lock the viewer has a faction but no formation (no squad, so no roster), and the
 * top-centre slot of the battle strip shows this plate instead.
 *
 * <p>Pure: {@link #view} reads the client caches' values, {@link #plate} builds the wording.
 * Waiting and to-do states are neutral (clock icon); orange stays for sections and adjustable
 * controls. The key cap names the terminal key the player has really bound, or is left out when
 * nothing is bound.
 */
public final class FormationVoteHudModel {
    /** How long the lock notice stays. */
    public static final long LOCK_NOTICE_NANOS = 3_000_000_000L;
    /** Lead column width: 16px emblem on its backing, or a 9px icon. */
    public static final int EMBLEM_LEAD_WIDTH = 22;
    public static final int ICON_LEAD_WIDTH = 12;
    /** Plate width beyond lead and text: 6px before the lead, 6px after the text. */
    public static final int HORIZONTAL_PADDING = 12;
    static final String SEPARATOR = " · ";
    static final String PREFIX = "hud.wok_infantry.vote.";

    private FormationVoteHudModel() {
    }

    public enum State {
        /** In a faction, the administrator has not opened the ballot yet. */
        WAITING,
        /** Ballot open, the viewer has not voted. */
        OPEN,
        /** Ballot open, the viewer has voted. */
        VOTED,
        /** Lock notice (3 seconds after the faction's formation was locked). */
        LOCKED
    }

    /**
     * What the plate is about.
     *
     * @param population faction members (the ballot's total)
     * @param voted      votes cast in the faction
     * @param formation  the viewer's vote (VOTED) or the locked formation (LOCKED); "" otherwise
     * @param iconId     emblem texture of the locked formation, "" when none
     * @param remain     share of the lock notice left, 1 → 0 (LOCKED only)
     */
    public record VoteView(State state, String factionName, int population, int voted,
                           String formation, String iconId, boolean changeAllowed, float remain) {
    }

    /** Plate content: accent, lead icon or emblem, two lines and the optional tally meter. */
    public record Plate(State state, int accent, boolean solid, TacticalIcon icon, int iconColor,
                        String emblemId, List<TacticalHud.Segment> firstLine,
                        List<TacticalHud.Segment> secondLine, boolean meter, float meterRatio,
                        float remain) {
        public Plate {
            firstLine = List.copyOf(firstLine);
            secondLine = List.copyOf(secondLine);
        }

        public boolean hasEmblem() {
            return emblemId != null && !emblemId.isEmpty();
        }

        public int leadWidth() {
            return hasEmblem() ? EMBLEM_LEAD_WIDTH : icon != null ? ICON_LEAD_WIDTH : 0;
        }

        /** Natural plate width for the given line width function. */
        public int contentWidth(ToIntFunction<List<TacticalHud.Segment>> lineWidth) {
            return Math.max(lineWidth.applyAsInt(firstLine), lineWidth.applyAsInt(secondLine))
                    + leadWidth() + HORIZONTAL_PADDING;
        }
    }

    /**
     * Ballot state of the viewer, or null when no plate is due: a lock notice younger than
     * {@link #LOCK_NOTICE_NANOS}, otherwise the open or waiting ballot of a viewer who has a
     * faction but no formation yet.
     */
    public static VoteView view(FormationSelectionSnapshot snapshot, Optional<LockTransition> lock,
                                long nowNanos) {
        if (lock != null && lock.isPresent()) {
            LockTransition transition = lock.get();
            long age = nowNanos - transition.lockedAtNanos();
            if (age >= 0L && age < LOCK_NOTICE_NANOS) {
                FactionSelectionView faction = faction(snapshot, transition.factionId());
                FormationSelectionView formation = formation(snapshot, faction,
                        transition.formationId());
                String name = !transition.formationName().isBlank() ? transition.formationName()
                        : formation != null ? formation.displayName() : transition.formationId();
                return new VoteView(State.LOCKED, faction == null ? "" : faction.displayName(),
                        faction == null ? 0 : faction.population(), 0, name,
                        formation == null ? "" : formation.iconId(), false,
                        1.0F - (float) age / LOCK_NOTICE_NANOS);
            }
        }
        if (snapshot == null || !snapshot.selectionRequired()
                || snapshot.selectedFactionId().isEmpty()
                || snapshot.votePhase() == FormationVotePhase.LOCKED) {
            return null;
        }
        FactionSelectionView faction = faction(snapshot, snapshot.selectedFactionId());
        String factionName = faction == null ? "" : faction.displayName();
        int population = faction == null ? 0 : faction.population();
        if (snapshot.votePhase() == FormationVotePhase.NOT_STARTED) {
            return new VoteView(State.WAITING, factionName, population, 0, "", "", false, 0.0F);
        }
        int voted = 0;
        for (int count : snapshot.voteTally().values()) {
            voted += Math.max(0, count);
        }
        String own = snapshot.ownVoteFormationId();
        if (own.isEmpty()) {
            return new VoteView(State.OPEN, factionName, population, voted, "", "",
                    snapshot.voteChangeAllowed(), 0.0F);
        }
        FormationSelectionView mine = formation(snapshot, faction, own);
        return new VoteView(State.VOTED, factionName, population, voted,
                mine == null ? own : mine.displayName(), "", snapshot.voteChangeAllowed(), 0.0F);
    }

    /**
     * Wording of the plate; {@code key} is the terminal key's name (null = unbound: the key cap
     * is left out and the line names the page instead).
     */
    public static Plate plate(VoteView view, boolean tight, Component key) {
        int muted = TacticalBoardTheme.LIGHT_MUTED;
        int light = TacticalBoardTheme.LIGHT;
        String size = tight ? "_short" : "";
        List<TacticalHud.Segment> first = new ArrayList<>();
        List<TacticalHud.Segment> second = new ArrayList<>();
        int total = Math.max(view.population(), view.voted());
        switch (view.state()) {
            case WAITING -> {
                first.add(text("waiting" + size, light));
                Component detail = tight ? tr("waiting_detail_short")
                        : tr("waiting_detail", view.factionName(), view.population());
                Component noKey = tight ? tr("waiting_detail_nokey_short")
                        : tr("waiting_detail_nokey", view.factionName(), view.population());
                keyed(second, key, detail, tr("go_formation"), noKey, muted);
                return new Plate(State.WAITING, TacticalBoardTheme.NEUTRAL_B, false,
                        TacticalIcon.CLOCK, muted, "", first, second, false, 0.0F, 0.0F);
            }
            case OPEN -> {
                first.add(text("open" + size, light));
                first.add(TacticalHud.Segment.text(Component.literal(SEPARATOR), muted));
                first.add(text("not_voted" + size, light));
                Component detail = tight ? tr("tally_short", view.voted(), total)
                        : tr("tally", view.factionName(), view.voted(), total);
                Component noKey = tight ? tr("tally_nokey_short", view.voted(), total)
                        : tr("tally_nokey", view.factionName(), view.voted(), total);
                keyed(second, key, detail, tr("go_formation"), noKey, muted);
                return new Plate(State.OPEN, TacticalBoardTheme.NEUTRAL_B, false,
                        TacticalIcon.CLOCK, light, "", first, second, !tight,
                        ratio(view.voted(), total), 0.0F);
            }
            case VOTED -> {
                first.add(text("voted" + size, muted));
                first.add(TacticalHud.Segment.text(Component.literal(view.formation()), light));
                Component detail = tight ? tr("voted_tally_short", view.voted(), total)
                        : tr("voted_tally", view.factionName(), view.voted(), total);
                String action = view.changeAllowed() ? "change" : "view";
                Component noKey = Component.empty().append(detail)
                        .append(tr(action + "_nokey"));
                keyed(second, key, detail, tr(action + size), noKey, muted);
                return new Plate(State.VOTED, TacticalBoardTheme.NEUTRAL_B, false,
                        TacticalIcon.CHECK, TacticalBoardTheme.SUCCESS_B, "", first, second,
                        !tight, ratio(view.voted(), total), 0.0F);
            }
            default -> {
                Component lockedLabel = tight || view.factionName().isEmpty()
                        ? tr("locked_short") : tr("locked", view.factionName());
                first.add(TacticalHud.Segment.text(lockedLabel, TacticalBoardTheme.SUCCESS_B));
                first.add(TacticalHud.Segment.text(Component.literal(view.formation()), light));
                keyed(second, key, tr("locked_detail" + size), tr("locked_deploy" + size),
                        tr("locked_detail_nokey" + size), muted);
                String emblem = view.iconId() == null || view.iconId().isBlank()
                        || ResourceLocation.tryParse(view.iconId()) == null ? "" : view.iconId();
                return new Plate(State.LOCKED, TacticalBoardTheme.SUCCESS_B, true,
                        TacticalIcon.FLAG, TacticalBoardTheme.SUCCESS_B, emblem, first,
                        second, false, 0.0F, Math.max(0.0F, Math.min(1.0F, view.remain())));
            }
        }
    }

    /** Plate height: 27px, or 32px with the tally meter between the lines. */
    public static int height(Plate plate) {
        return plate.meter() ? WokHudLayout.VOTE_HEIGHT_METER : WokHudLayout.VOTE_HEIGHT;
    }

    private static void keyed(List<TacticalHud.Segment> line, Component key, Component before,
                              Component after, Component withoutKey, int color) {
        if (key == null || key.getString().isEmpty()) {
            line.add(TacticalHud.Segment.text(withoutKey, color));
            return;
        }
        line.add(TacticalHud.Segment.text(before, color));
        line.add(TacticalHud.Segment.key(key));
        line.add(TacticalHud.Segment.text(after, color));
    }

    private static TacticalHud.Segment text(String key, int color) {
        return TacticalHud.Segment.text(tr(key), color);
    }

    private static Component tr(String key, Object... args) {
        return Component.translatable(PREFIX + key, args);
    }

    private static float ratio(int voted, int total) {
        return total <= 0 ? 0.0F : Math.max(0.0F, Math.min(1.0F, voted / (float) total));
    }

    private static FactionSelectionView faction(FormationSelectionSnapshot snapshot, String id) {
        if (snapshot == null || id == null || id.isEmpty()) {
            return null;
        }
        for (FactionSelectionView faction : snapshot.factions()) {
            if (faction.id().equals(id)) {
                return faction;
            }
        }
        return null;
    }

    /**
     * Formation {@code id} of {@code owner} (formation ids are only unique within a faction, so
     * two factions may both have e.g. "default"); any faction's only when the owner is unknown.
     */
    private static FormationSelectionView formation(FormationSelectionSnapshot snapshot,
                                                    FactionSelectionView owner, String id) {
        if (snapshot == null || id == null || id.isEmpty()) {
            return null;
        }
        if (owner != null) {
            for (FormationSelectionView formation : owner.formations()) {
                if (formation.id().equals(id)) {
                    return formation;
                }
            }
            return null;
        }
        for (FactionSelectionView faction : snapshot.factions()) {
            for (FormationSelectionView formation : faction.formations()) {
                if (formation.id().equals(id)) {
                    return formation;
                }
            }
        }
        return null;
    }
}
