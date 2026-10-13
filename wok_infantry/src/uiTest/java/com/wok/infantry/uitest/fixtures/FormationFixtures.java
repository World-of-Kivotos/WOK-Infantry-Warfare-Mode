package com.wok.infantry.uitest.fixtures;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.formation.FactionDefinition;
import com.wok.infantry.formation.FormationCapabilityProfile;
import com.wok.infantry.formation.FormationClassRule;
import com.wok.infantry.formation.FormationConfigData;
import com.wok.infantry.formation.FormationDefinition;
import com.wok.infantry.formation.FormationSupportPolicy;
import com.wok.infantry.formation.selection.FactionSelectionView;
import com.wok.infantry.formation.selection.FormationDetailView;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.formation.selection.FormationSelectionView;
import com.wok.infantry.formation.selection.FormationSupportLabel;
import com.wok.infantry.formation.vote.FormationVotePhase;
import com.wok.infantry.formation.vote.FormationVotePolicy;
import com.wok.infantry.server.FormationService;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Formation selection catalogs (formation protocol 5) built from the core's real default
 * configuration ({@link FormationConfigData#defaultConfig()}) with the preview's populations and
 * votes ({@link MockData}), one {@link Scenario} per state of {@code ui-preview/surfaces/45-formation.js}.
 *
 * <p>Nothing the server words is re-written here: the structured detail, the candidate
 * availability and its reason, and the protocol-4 vehicle and capability lines all come from the
 * server's own builders in {@link FormationService} (called reflectively, they are not public);
 * when one of them is renamed or changes its signature the fixture throws, so the case fails
 * instead of running on stand-in values. The protocol-4 class list carries the real class ids, as the server sends it, so a page that
 * showed internal ids instead of profession names (player-09) would show them here too. Faction,
 * formation and support names are catalog data, as on a real server.
 */
public final class FormationFixtures {
    private static final Set<String> SERVER_WARNINGS = ConcurrentHashMap.newKeySet();
    /** The disabled reserve formation of the {@code full} state (preview {@code RESERVE}). */
    public static final String RESERVE_ID = "millennium_cavalry_reserve";
    /** Formation whose capability block lists five allow-listed supports ({@code longcaps}). */
    public static final String LONG_CAPS_ID = "millennium_seminar_long_caps";
    public static final String MOBILE_ID = "millennium_seminar_mobile";
    public static final String CAVALRY_ID = "millennium_seminar_cavalry_corps";
    public static final String DEFAULT_ID = "default";

    private FormationFixtures() {
    }

    // ---- states of 45-formation -----------------------------------------------------------------

    /** {@code wok_ui_10}: joined the academy faction, vote not opened yet (admin can open it). */
    public static FormationSelectionSnapshot pendingVote() {
        return Scenario.joined(FormationVotePhase.NOT_STARTED).build();
    }

    /** {@code wok_ui_11}: vote open, the viewer voted for the Millennium mobile formation. */
    public static FormationSelectionSnapshot openVote() {
        return Scenario.joined(FormationVotePhase.OPEN).own(MockData.OWN_VOTE).build();
    }

    /**
     * One state of the vote page. Phase, votes and the locked formation describe the academy
     * faction's ballot (the viewer's faction, or the one the viewer looks at before joining);
     * the other faction has not started its ballot.
     */
    public static final class Scenario {
        private String joined = MockData.VIEWER_FACTION;
        private FormationVotePhase phase = FormationVotePhase.OPEN;
        private boolean changeAllowed = true;
        private String own = "";
        private String locked = "";
        private Map<String, Integer> tally;
        private final Map<String, Integer> population = new HashMap<>();
        private final Map<String, Integer> capacity = new HashMap<>();
        private boolean reserve;
        private boolean longCaps;

        private Scenario() {
        }

        /** Not joined; {@code phase} is the academy ballot every viewer can see. */
        public static Scenario unjoined(FormationVotePhase phase) {
            Scenario scenario = new Scenario();
            scenario.joined = "";
            scenario.phase = phase;
            return scenario;
        }

        /** Joined the academy faction at {@code phase}. */
        public static Scenario joined(FormationVotePhase phase) {
            Scenario scenario = new Scenario();
            scenario.phase = phase;
            return scenario;
        }

        public Scenario own(String formationId) {
            this.own = formationId;
            return this;
        }

        public Scenario locked(String formationId) {
            this.locked = formationId;
            return this;
        }

        public Scenario changeAllowed(boolean value) {
            this.changeAllowed = value;
            return this;
        }

        /** Votes per formation id of the academy ballot (default {@link MockData#VOTES}). */
        public Scenario tally(Map<String, Integer> votes) {
            this.tally = new LinkedHashMap<>(votes);
            return this;
        }

        public Scenario population(String factionId, int players) {
            population.put(factionId, players);
            return this;
        }

        /** Overrides a formation's capacity (the {@code full} state's 12-seat cavalry). */
        public Scenario capacity(String formationId, int seats) {
            capacity.put(formationId, seats);
            return this;
        }

        /** Adds the disabled reserve formation to the academy candidates. */
        public Scenario withReserve() {
            this.reserve = true;
            return this;
        }

        /** Adds a formation that allows five named supports (beta.7 capability lines). */
        public Scenario withLongCapabilities() {
            this.longCaps = true;
            return this;
        }

        public FormationSelectionSnapshot build() {
            return FormationFixtures.build(this);
        }
    }

    private static FormationSelectionSnapshot build(Scenario s) {
        FormationConfigData config = FormationConfigData.defaultConfig();
        List<String> registered = MockData.SUPPORT_LABELS.stream()
                .map(MockData.SupportLabelData::id).toList();
        List<FactionSelectionView> factions = new ArrayList<>();
        Map<String, Integer> ownTally = new LinkedHashMap<>();
        for (FactionDefinition source : config.factions()) {
            FactionDefinition faction = adjusted(source, s);
            boolean ballotHere = faction.id().equals(MockData.VIEWER_FACTION);
            MockData.FactionData mock = MockData.FACTIONS.stream()
                    .filter(data -> data.id().equals(faction.id())).findFirst()
                    .orElse(new MockData.FactionData(faction.id(), faction.displayName(), 0,
                            faction.maxPlayers()));
            int members = s.population.getOrDefault(faction.id(), mock.population());
            FormationVotePhase phase = ballotHere ? s.phase : FormationVotePhase.NOT_STARTED;
            String lockedId = phase == FormationVotePhase.LOCKED ? s.locked : "";
            FormationDefinition locked = lockedId.isEmpty() ? null
                    : faction.findFormation(lockedId).orElse(null);
            List<FormationSelectionView> formations = new ArrayList<>();
            for (FormationDefinition formation : faction.formations()) {
                FormationSelectionView view = view(faction, formation,
                        formation == locked ? members : 0, registered);
                formations.add(view);
                if (ballotHere && phase != FormationVotePhase.NOT_STARTED && view.available()) {
                    Map<String, Integer> votes = s.tally != null ? s.tally : MockData.VOTES;
                    ownTally.put(formation.id(), votes.getOrDefault(formation.id(), 0));
                }
            }
            int joinCapacity = FormationVotePolicy.joinCapacity(faction.maxPlayers(), phase,
                    locked == null ? 0 : locked.capacity());
            boolean lockedUsable = locked == null || availability(faction, locked).available();
            boolean available = faction.enabled() && lockedUsable
                    && FormationVotePolicy.canJoin(members, joinCapacity)
                    && formations.stream().anyMatch(FormationSelectionView::available);
            factions.add(new FactionSelectionView(faction.id(), faction.displayName(),
                    faction.description(), members, faction.maxPlayers(), available, formations,
                    phase, lockedId));
        }
        boolean joined = !s.joined.isEmpty();
        boolean lockedPhase = s.phase == FormationVotePhase.LOCKED;
        String selectedFormation = joined && lockedPhase ? s.locked : "";
        return new FormationSelectionSnapshot(1L, !(joined && !selectedFormation.isEmpty()),
                s.joined, selectedFormation,
                joined ? s.phase : FormationVotePhase.NOT_STARTED,
                joined && s.changeAllowed, joined ? s.own : "", joined && lockedPhase ? s.locked : "",
                joined ? ownTally : Map.of(), factions, supportLabels());
    }

    /** Applies the scenario's formation changes to the academy faction. */
    private static FactionDefinition adjusted(FactionDefinition faction, Scenario s) {
        if (!faction.id().equals(MockData.VIEWER_FACTION)) {
            return faction;
        }
        List<FormationDefinition> formations = new ArrayList<>();
        for (FormationDefinition formation : faction.formations()) {
            Integer seats = s.capacity.get(formation.id());
            formations.add(seats == null ? formation
                    : copy(formation, formation.id(), formation.displayName(), formation.enabled(),
                    seats, formation.capabilities()));
        }
        FormationDefinition cavalry = faction.findFormation(CAVALRY_ID).orElse(null);
        if (s.reserve && cavalry != null) {
            formations.add(copy(cavalry, RESERVE_ID, MockData.RESERVE_NAME, false,
                    cavalry.capacity(), cavalry.capabilities()));
        }
        FormationDefinition mobile = faction.findFormation(MOBILE_ID).orElse(null);
        if (s.longCaps && mobile != null) {
            FormationCapabilityProfile base = mobile.capabilities();
            formations.add(copy(mobile, LONG_CAPS_ID, MockData.LONG_CAPS_NAME, true,
                    mobile.capacity(), new FormationCapabilityProfile(base.outpost(),
                            base.rally(), base.respawn(), new FormationSupportPolicy(
                            FormationSupportPolicy.Mode.ALLOW_LIST, MockData.LONG_CAPS_SUPPORTS))));
        }
        return new FactionDefinition(faction.id(), faction.displayName(), faction.description(),
                faction.battleSideId(), faction.enabled(), faction.maxPlayers(), formations);
    }

    private static FormationDefinition copy(FormationDefinition source, String id, String name,
                                            boolean enabled, int capacity,
                                            FormationCapabilityProfile capabilities) {
        return new FormationDefinition(id, name, source.description(), source.icon(),
                source.category(), enabled, capacity, capabilities, source.classes(),
                source.squads(), source.vehicles());
    }

    /** View of one formation as {@code FormationService.formationView} builds it. */
    public static FormationSelectionView view(FactionDefinition faction,
                                              FormationDefinition formation, int population,
                                              List<String> registeredSupports) {
        Availability availability = availability(faction, formation);
        List<String> classes = formation.classes().stream().map(FormationClassRule::classId)
                .toList();
        // The page reads the structured detail; the protocol-4 squad strings are left empty
        // instead of re-implementing the server's wording.
        return new FormationSelectionView(formation.id(), formation.displayName(),
                formation.description(), formation.icon(),
                formation.category() == null ? formation.categoryId() : formation.category().id(),
                formation.category() == null ? formation.categoryId()
                        : formation.category().displayName(),
                population, formation.capacity(), availability.available(),
                availability.reason(), classes, List.of(),
                serverList("vehicleSummaries", formation),
                serverList("capabilitySummaries", formation),
                detail(formation, registeredSupports));
    }

    /** Name table of every support the fixtures mention (support add-on keys and fallbacks). */
    public static List<FormationSupportLabel> supportLabels() {
        return MockData.SUPPORT_LABELS.stream()
                .map(label -> new FormationSupportLabel(label.id(), label.translationKey(),
                        label.fallbackName()))
                .toList();
    }

    // ---- the server's own builders ----------------------------------------------------------------

    /** Candidate availability and its reason, from {@code FormationService.availability}. */
    public record Availability(boolean available, String reason) {
    }

    /**
     * The server's verdict for a server that has the vehicle MODs: the acceptance client loads
     * no SuperbWarfare ecosystem, so the vehicle gate is left out (the formation is judged
     * without its vehicles); every other rule and its wording are the server's.
     */
    static Availability availability(FactionDefinition faction, FormationDefinition formation) {
        FormationDefinition withoutVehicles = new FormationDefinition(formation.id(),
                formation.displayName(), formation.description(), formation.icon(),
                formation.category(), formation.enabled(), formation.capacity(),
                formation.capabilities(), formation.classes(), formation.squads(), List.of());
        try {
            Method method = FormationService.class.getDeclaredMethod("availability",
                    FactionDefinition.class, FormationDefinition.class);
            method.setAccessible(true);
            Object result = method.invoke(null, faction, withoutVehicles);
            Method available = result.getClass().getDeclaredMethod("available");
            Method reason = result.getClass().getDeclaredMethod("reason");
            available.setAccessible(true);
            reason.setAccessible(true);
            return new Availability((Boolean) available.invoke(result),
                    (String) reason.invoke(result));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            throw unavailable("availability", exception);
        }
    }

    /** Structured detail from {@code FormationService.detailView}. */
    static FormationDetailView detail(FormationDefinition formation, List<String> supports) {
        try {
            Method method = FormationService.class.getDeclaredMethod("detailView",
                    FormationDefinition.class, List.class);
            method.setAccessible(true);
            return (FormationDetailView) method.invoke(null, formation, supports);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            throw unavailable("detailView", exception);
        }
    }

    /**
     * Calls the static {@code FormationService.<method>(FormationDefinition)} that builds the
     * protocol-4 summary lines, so the fixture never drifts from the server's wording.
     */
    @SuppressWarnings("unchecked")
    private static List<String> serverList(String method, FormationDefinition formation) {
        try {
            Method summaries = FormationService.class.getDeclaredMethod(method,
                    FormationDefinition.class);
            summaries.setAccessible(true);
            return (List<String>) summaries.invoke(null, formation);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            throw unavailable(method, exception);
        }
    }

    /**
     * A server builder the fixture relies on is gone or changed its signature: the case must fail
     * (its catalog would no longer show the server's wording), not run on stand-in values.
     */
    private static IllegalStateException unavailable(String method, Exception exception) {
        if (SERVER_WARNINGS.add(method)) {
            WokInfantryMod.LOGGER.error("[UI ACCEPTANCE] FormationService.{} unavailable", method,
                    exception);
        }
        return new IllegalStateException("FormationService." + method
                + " is unavailable to the formation fixture: " + exception, exception);
    }
}
