package com.wok.infantry.uitest.fixtures;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.formation.FactionDefinition;
import com.wok.infantry.formation.FormationClassRule;
import com.wok.infantry.formation.FormationConfigData;
import com.wok.infantry.formation.FormationDefinition;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Formation selection catalogs built from the core's real default configuration
 * ({@link FormationConfigData#defaultConfig()}), with the preview's populations and votes
 * ({@link MockData}). Class lists carry the real class ids exactly as the server sends them, so a
 * screen that shows internal ids instead of class names (player-09) is visible in the captures.
 */
public final class FormationFixtures {
    private static final Set<String> SUMMARY_WARNINGS = ConcurrentHashMap.newKeySet();

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

    /** The server's own vehicle lines (private there, read reflectively here). */
    private static List<String> vehicles(FormationDefinition formation) {
        return serverSummaries("vehicleSummaries", formation);
    }

    /** The server's own capability lines (package-private there, read reflectively here). */
    private static List<String> capabilities(FormationDefinition formation) {
        return serverSummaries("capabilitySummaries", formation);
    }

    /**
     * Calls the static {@code FormationService.<method>(FormationDefinition)} that builds the
     * summary lines of the real catalog, so the fixture never drifts from the server's wording.
     */
    @SuppressWarnings("unchecked")
    private static List<String> serverSummaries(String method, FormationDefinition formation) {
        try {
            Method summaries = FormationService.class.getDeclaredMethod(method,
                    FormationDefinition.class);
            summaries.setAccessible(true);
            return (List<String>) summaries.invoke(null, formation);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            if (SUMMARY_WARNINGS.add(method)) {
                WokInfantryMod.LOGGER.warn("[UI ACCEPTANCE] FormationService.{} unavailable",
                        method, exception);
            }
            return List.of();
        }
    }
}
