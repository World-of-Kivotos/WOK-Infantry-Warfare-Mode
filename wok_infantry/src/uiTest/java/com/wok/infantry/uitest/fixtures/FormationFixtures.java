package com.wok.infantry.uitest.fixtures;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.formation.FactionDefinition;
import com.wok.infantry.formation.FormationClassRule;
import com.wok.infantry.formation.FormationConfigData;
import com.wok.infantry.formation.FormationDefinition;
import com.wok.infantry.formation.FormationVehicleDefinition;
import com.wok.infantry.formation.selection.FactionSelectionView;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.formation.selection.FormationSelectionView;
import com.wok.infantry.formation.vote.FormationVotePhase;
import com.wok.infantry.server.FormationService;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Formation selection catalogs built from the core's real default configuration
 * ({@link FormationConfigData#defaultConfig()}), with the preview's populations and votes
 * ({@link MockData}). Class lists carry the real class ids exactly as the server sends them, so a
 * screen that shows internal ids instead of class names (player-09) is visible in the captures.
 */
public final class FormationFixtures {
    private static boolean capabilityWarningLogged;

    private FormationFixtures() {
    }

    /**
     * Catalog as the server sends it to a member of the academy faction.
     *
     * @param phase    vote phase of the viewer's faction
     * @param ownVote  the viewer's vote ("" for none)
     * @param locked   locked formation id ("" while not locked)
     * @param required whether the viewer still has to pick a formation
     */
    public static FormationSelectionSnapshot catalog(FormationVotePhase phase, String ownVote,
                                                     String locked, boolean required) {
        FormationConfigData config = FormationConfigData.defaultConfig();
        List<FactionSelectionView> factions = new ArrayList<>();
        Map<String, Integer> tally = new LinkedHashMap<>();
        for (FactionDefinition faction : config.factions()) {
            MockData.FactionData mock = MockData.FACTIONS.stream()
                    .filter(data -> data.id().equals(faction.id())).findFirst()
                    .orElse(new MockData.FactionData(faction.id(), faction.displayName(), 0,
                            faction.maxPlayers()));
            List<FormationSelectionView> formations = new ArrayList<>();
            for (FormationDefinition formation : faction.formations()) {
                formations.add(view(formation, mock.population()));
                if (faction.id().equals(MockData.VIEWER_FACTION)
                        && phase != FormationVotePhase.NOT_STARTED) {
                    tally.put(formation.id(), MockData.VOTES.getOrDefault(formation.id(), 0));
                }
            }
            factions.add(new FactionSelectionView(faction.id(), faction.displayName(),
                    faction.description(), mock.population(), mock.capacity(), true, formations));
        }
        return new FormationSelectionSnapshot(1L, required, MockData.VIEWER_FACTION,
                locked.isEmpty() ? "" : locked, phase, true, ownVote, locked, tally, factions);
    }

    /** {@code wok_ui_10}: joined the academy faction, vote not opened yet (admin can open it). */
    public static FormationSelectionSnapshot pendingVote() {
        return catalog(FormationVotePhase.NOT_STARTED, "", "", true);
    }

    /** {@code wok_ui_11}: vote open, the viewer voted for the Millennium mobile formation. */
    public static FormationSelectionSnapshot openVote() {
        return catalog(FormationVotePhase.OPEN, MockData.OWN_VOTE, "", true);
    }

    /** View of one formation as {@code FormationService.formationView} builds it. */
    public static FormationSelectionView view(FormationDefinition formation, int population) {
        List<String> classes = formation.classes().stream().map(FormationClassRule::classId)
                .toList();
        List<String> squads = formation.squads().stream()
                .map(squad -> squad.displayName() + " (" + squad.capacity() + ")").toList();
        return new FormationSelectionView(formation.id(), formation.displayName(),
                formation.description(), formation.icon(), formation.category().id(),
                formation.category().displayName(), 0, formation.capacity(),
                population < formation.capacity(), "", classes, squads, vehicles(formation),
                capabilities(formation));
    }

    private static List<String> vehicles(FormationDefinition formation) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, Integer> cooldowns = new LinkedHashMap<>();
        for (FormationVehicleDefinition vehicle : formation.vehicles()) {
            counts.merge(vehicle.displayName(), 1, Integer::sum);
            cooldowns.putIfAbsent(vehicle.displayName(), vehicle.replenishmentCooldownSeconds());
        }
        List<String> summaries = new ArrayList<>();
        counts.forEach((name, count) -> {
            int seconds = cooldowns.get(name);
            String replenishment = seconds < 0 ? "不可再生"
                    : seconds % 60 == 0 ? seconds / 60 + "分钟" : seconds + "秒";
            summaries.add(name + (count > 1 ? " ×" + count : "") + "（" + replenishment + "）");
        });
        return summaries;
    }

    /** The server's own capability lines (package-private there, read reflectively here). */
    @SuppressWarnings("unchecked")
    private static List<String> capabilities(FormationDefinition formation) {
        try {
            Method method = FormationService.class.getDeclaredMethod("capabilitySummaries",
                    FormationDefinition.class);
            method.setAccessible(true);
            return (List<String>) method.invoke(null, formation);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            if (!capabilityWarningLogged) {
                capabilityWarningLogged = true;
                WokInfantryMod.LOGGER.warn("[UI ACCEPTANCE] Formation capability lines unavailable",
                        exception);
            }
            return List.of();
        }
    }
}
