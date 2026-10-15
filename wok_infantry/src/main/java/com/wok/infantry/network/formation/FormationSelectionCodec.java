package com.wok.infantry.network.formation;

import com.wok.infantry.formation.FormationSupportPolicy;
import com.wok.infantry.formation.selection.FactionSelectionView;
import com.wok.infantry.formation.selection.FormationDetailView;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.formation.selection.FormationSelectionView;
import com.wok.infantry.formation.selection.FormationSupportLabel;
import com.wok.infantry.formation.vote.FormationVotePhase;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * Strict bounded codec for the public selection catalog.
 *
 * <p>Formation protocol 5 appends, per faction, its ballot phase and locked formation (sent to
 * every viewer, without the tally), per formation the structured {@link FormationDetailView}, and
 * at the end the support name table. Display text is clipped with {@link #clip} like every other
 * catalog string, so an overlong admin-authored name can never make the snapshot unencodable.
 */
public final class FormationSelectionCodec {
    public static final int MAX_ID = 64;
    public static final int MAX_DISPLAY_NAME = 40;
    public static final int MAX_DESCRIPTION = 512;
    public static final int MAX_REASON = 256;
    public static final int MAX_SUMMARY = 192;
    public static final int MAX_FACTIONS = 16;
    public static final int MAX_FORMATIONS = 32;
    public static final int MAX_SUMMARIES = 64;
    /** Entries per structured detail list (classes, vehicles, squads, mobile spawns, supports). */
    public static final int MAX_DETAIL_ENTRIES = 64;
    /** Support name table size (the support registry itself holds at most 32). */
    public static final int MAX_SUPPORT_LABELS = 64;
    /** Upper bound of a vehicle replenishment cooldown on the wire (one week). */
    public static final int MAX_COOLDOWN_SECONDS = 604_800;
    /** Upper bound of a formation respawn delay on the wire. */
    public static final int MAX_RESPAWN_SECONDS = 3_600;
    /** Upper bound of outposts and rally packs on the wire. */
    public static final int MAX_DEPLOYABLES = 64;
    /** Upper bound of a class quota, a vehicle group's count and a squad capacity on the wire. */
    public static final int MAX_DETAIL_COUNT = 128;
    private static final int MAX_PHASE = 16;

    private FormationSelectionCodec() {
    }

    public static void encode(FriendlyByteBuf buffer, FormationSelectionSnapshot snapshot) {
        buffer.writeVarLong(snapshot.generation());
        buffer.writeBoolean(snapshot.selectionRequired());
        buffer.writeUtf(snapshot.selectedFactionId(), MAX_ID);
        buffer.writeUtf(snapshot.selectedFormationId(), MAX_ID);
        buffer.writeUtf(snapshot.votePhase().name(), MAX_PHASE);
        buffer.writeBoolean(snapshot.voteChangeAllowed());
        buffer.writeUtf(snapshot.ownVoteFormationId(), MAX_ID);
        buffer.writeUtf(snapshot.lockedFormationId(), MAX_ID);
        writeCount(buffer, snapshot.voteTally().size(), MAX_FORMATIONS, "vote tally");
        snapshot.voteTally().forEach((formationId, votes) -> {
            buffer.writeUtf(requireId(formationId, true), MAX_ID);
            if (votes < 0 || votes > 128) {
                throw new IllegalArgumentException("Invalid vote count: " + votes);
            }
            buffer.writeVarInt(votes);
        });
        writeCount(buffer, snapshot.factions().size(), MAX_FACTIONS, "factions");
        for (FactionSelectionView faction : snapshot.factions()) {
            buffer.writeUtf(faction.id(), MAX_ID);
            buffer.writeUtf(faction.displayName(), MAX_DISPLAY_NAME);
            buffer.writeUtf(faction.description(), MAX_DESCRIPTION);
            buffer.writeVarInt(faction.population());
            buffer.writeVarInt(faction.capacity());
            buffer.writeBoolean(faction.available());
            if ((faction.votePhase() == FormationVotePhase.LOCKED)
                    == faction.lockedFormationId().isEmpty()) {
                throw new IllegalArgumentException(
                        "Faction ballot phase and locked formation disagree");
            }
            buffer.writeUtf(faction.votePhase().name(), MAX_PHASE);
            buffer.writeUtf(requireId(faction.lockedFormationId(), false), MAX_ID);
            writeCount(buffer, faction.formations().size(), MAX_FORMATIONS, "formations");
            for (FormationSelectionView formation : faction.formations()) {
                buffer.writeUtf(formation.id(), MAX_ID);
                buffer.writeUtf(formation.displayName(), MAX_DISPLAY_NAME);
                buffer.writeUtf(formation.description(), MAX_DESCRIPTION);
                buffer.writeUtf(formation.iconId(), MAX_SUMMARY);
                buffer.writeUtf(formation.categoryId(), MAX_ID);
                buffer.writeUtf(formation.categoryDisplayName(), MAX_DISPLAY_NAME);
                buffer.writeVarInt(formation.population());
                buffer.writeVarInt(formation.capacity());
                buffer.writeBoolean(formation.available());
                buffer.writeUtf(clip(formation.unavailableReason(), MAX_REASON), MAX_REASON);
                writeStrings(buffer, formation.classes());
                writeStrings(buffer, formation.squads());
                writeStrings(buffer, formation.vehicles());
                writeStrings(buffer, formation.capabilities());
                writeDetail(buffer, formation.detail());
            }
        }
        writeCount(buffer, snapshot.supportLabels().size(), MAX_SUPPORT_LABELS, "support labels");
        for (FormationSupportLabel label : snapshot.supportLabels()) {
            buffer.writeUtf(clip(label.id(), MAX_SUMMARY), MAX_SUMMARY);
            buffer.writeUtf(clip(label.translationKey(), MAX_SUMMARY), MAX_SUMMARY);
            buffer.writeUtf(clip(label.fallbackName(), MAX_SUMMARY), MAX_SUMMARY);
        }
    }

    public static FormationSelectionSnapshot decode(FriendlyByteBuf buffer) {
        long generation = buffer.readVarLong();
        if (generation < 0L) {
            throw new IllegalArgumentException("Negative formation catalog generation");
        }
        boolean required = buffer.readBoolean();
        String selectedFaction = buffer.readUtf(MAX_ID);
        String selectedFormation = buffer.readUtf(MAX_ID);
        FormationVotePhase votePhase = readPhase(buffer);
        boolean voteChangeAllowed = buffer.readBoolean();
        String ownVote = buffer.readUtf(MAX_ID);
        String lockedFormation = buffer.readUtf(MAX_ID);
        int tallyCount = readCount(buffer, MAX_FORMATIONS, "vote tally");
        java.util.LinkedHashMap<String, Integer> voteTally = new java.util.LinkedHashMap<>();
        for (int index = 0; index < tallyCount; index++) {
            String formationId = requireId(buffer.readUtf(MAX_ID), true);
            int votes = readBoundedPopulation(buffer, "formation votes");
            if (voteTally.putIfAbsent(formationId, votes) != null) {
                throw new IllegalArgumentException("Duplicate formation vote tally id");
            }
        }
        int factionCount = readCount(buffer, MAX_FACTIONS, "factions");
        List<FactionSelectionView> factions = new ArrayList<>(factionCount);
        for (int factionIndex = 0; factionIndex < factionCount; factionIndex++) {
            String factionId = requireId(buffer.readUtf(MAX_ID), true);
            String displayName = buffer.readUtf(MAX_DISPLAY_NAME);
            String description = buffer.readUtf(MAX_DESCRIPTION);
            int population = readBoundedPopulation(buffer, "faction population");
            int capacity = readBoundedCapacity(buffer, "faction capacity");
            boolean available = buffer.readBoolean();
            FormationVotePhase factionPhase = readPhase(buffer);
            String factionLocked = requireId(buffer.readUtf(MAX_ID), false);
            if ((factionPhase == FormationVotePhase.LOCKED) == factionLocked.isEmpty()) {
                throw new IllegalArgumentException(
                        "Faction ballot phase and locked formation disagree");
            }
            int formationCount = readCount(buffer, MAX_FORMATIONS, "formations");
            List<FormationSelectionView> formations = new ArrayList<>(formationCount);
            for (int formationIndex = 0; formationIndex < formationCount; formationIndex++) {
                String formationId = requireId(buffer.readUtf(MAX_ID), true);
                String formationName = buffer.readUtf(MAX_DISPLAY_NAME);
                String formationDescription = buffer.readUtf(MAX_DESCRIPTION);
                String formationIcon = buffer.readUtf(MAX_SUMMARY);
                String categoryId = requireId(buffer.readUtf(MAX_ID), true);
                String categoryDisplayName = buffer.readUtf(MAX_DISPLAY_NAME);
                int formationPopulation = readBoundedPopulation(buffer,
                        "formation population");
                int formationCapacity = readBoundedCapacity(buffer, "formation capacity");
                boolean formationAvailable = buffer.readBoolean();
                String reason = buffer.readUtf(MAX_REASON);
                List<String> classes = readStrings(buffer);
                List<String> squads = readStrings(buffer);
                List<String> vehicles = readStrings(buffer);
                List<String> capabilities = readStrings(buffer);
                FormationDetailView detail = readDetail(buffer);
                formations.add(new FormationSelectionView(formationId, formationName,
                        formationDescription, formationIcon, categoryId, categoryDisplayName,
                        formationPopulation, formationCapacity, formationAvailable, reason,
                        classes, squads, vehicles, capabilities, detail));
            }
            factions.add(new FactionSelectionView(factionId, displayName, description,
                    population, capacity, available, formations, factionPhase, factionLocked));
        }
        int labelCount = readCount(buffer, MAX_SUPPORT_LABELS, "support labels");
        List<FormationSupportLabel> labels = new ArrayList<>(labelCount);
        for (int index = 0; index < labelCount; index++) {
            labels.add(new FormationSupportLabel(buffer.readUtf(MAX_SUMMARY),
                    buffer.readUtf(MAX_SUMMARY), buffer.readUtf(MAX_SUMMARY)));
        }
        if (!selectedFaction.isEmpty()) {
            requireId(selectedFaction, false);
        }
        if (!selectedFormation.isEmpty()) {
            requireId(selectedFormation, false);
        }
        if (!ownVote.isEmpty()) {
            requireId(ownVote, false);
        }
        if (!lockedFormation.isEmpty()) {
            requireId(lockedFormation, false);
        }
        return new FormationSelectionSnapshot(generation, required, selectedFaction,
                selectedFormation, votePhase, voteChangeAllowed, ownVote, lockedFormation,
                voteTally, factions, labels);
    }

    public static String requireId(String value, boolean required) {
        if (value == null || value.isEmpty()) {
            if (required) {
                throw new IllegalArgumentException("Missing formation identifier");
            }
            return "";
        }
        if (value.length() > MAX_ID) {
            throw new IllegalArgumentException("Formation identifier is too long");
        }
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (!(current >= 'a' && current <= 'z')
                    && !(current >= '0' && current <= '9')
                    && current != '_' && current != '-' && current != '.') {
                throw new IllegalArgumentException("Invalid formation identifier");
            }
        }
        return value;
    }

    private static void writeDetail(FriendlyByteBuf buffer, FormationDetailView detail) {
        writeCount(buffer, detail.classQuotas().size(), MAX_DETAIL_ENTRIES, "class quotas");
        for (FormationDetailView.ClassQuota quota : detail.classQuotas()) {
            buffer.writeUtf(clip(quota.displayName(), MAX_DISPLAY_NAME), MAX_DISPLAY_NAME);
            writeBounded(buffer, quota.squadLimit(), 0, MAX_DETAIL_COUNT, "class squad limit");
        }
        writeCount(buffer, detail.vehicles().size(), MAX_DETAIL_ENTRIES, "vehicles");
        for (FormationDetailView.Vehicle vehicle : detail.vehicles()) {
            buffer.writeUtf(clip(vehicle.displayName(), MAX_DISPLAY_NAME), MAX_DISPLAY_NAME);
            writeBounded(buffer, vehicle.count(), 1, MAX_DETAIL_COUNT, "vehicle count");
            writeBounded(buffer, vehicle.cooldownSeconds(), FormationDetailView.Vehicle.NEVER,
                    MAX_COOLDOWN_SECONDS, "vehicle cooldown");
        }
        writeCount(buffer, detail.squads().size(), MAX_DETAIL_ENTRIES, "squads");
        for (FormationDetailView.Squad squad : detail.squads()) {
            buffer.writeUtf(clip(squad.displayName(), MAX_DISPLAY_NAME), MAX_DISPLAY_NAME);
            writeBounded(buffer, squad.capacity(), 1, MAX_DETAIL_COUNT, "squad capacity");
        }
        writeBounded(buffer, detail.outpostMax(), 0, MAX_DEPLOYABLES, "outposts");
        writeBounded(buffer, detail.rallyMax(), 0, MAX_DEPLOYABLES, "rally packs");
        writeBounded(buffer, detail.respawnDelaySeconds(), FormationDetailView.INHERIT_RESPAWN,
                MAX_RESPAWN_SECONDS, "respawn delay");
        writeCount(buffer, detail.mobileSpawnVehicles().size(), MAX_DETAIL_ENTRIES,
                "mobile spawn vehicles");
        detail.mobileSpawnVehicles().forEach(name ->
                buffer.writeUtf(clip(name, MAX_DISPLAY_NAME), MAX_DISPLAY_NAME));
        buffer.writeUtf(detail.supportMode().name(), MAX_PHASE);
        writeCount(buffer, detail.supportIds().size(), MAX_DETAIL_ENTRIES, "support ids");
        detail.supportIds().forEach(id -> buffer.writeUtf(clip(id, MAX_SUMMARY), MAX_SUMMARY));
    }

    private static FormationDetailView readDetail(FriendlyByteBuf buffer) {
        int classCount = readCount(buffer, MAX_DETAIL_ENTRIES, "class quotas");
        List<FormationDetailView.ClassQuota> classes = new ArrayList<>(classCount);
        for (int index = 0; index < classCount; index++) {
            classes.add(new FormationDetailView.ClassQuota(buffer.readUtf(MAX_DISPLAY_NAME),
                    readBounded(buffer, 0, MAX_DETAIL_COUNT, "class squad limit")));
        }
        int vehicleCount = readCount(buffer, MAX_DETAIL_ENTRIES, "vehicles");
        List<FormationDetailView.Vehicle> vehicles = new ArrayList<>(vehicleCount);
        for (int index = 0; index < vehicleCount; index++) {
            vehicles.add(new FormationDetailView.Vehicle(buffer.readUtf(MAX_DISPLAY_NAME),
                    readBounded(buffer, 1, MAX_DETAIL_COUNT, "vehicle count"),
                    readBounded(buffer, FormationDetailView.Vehicle.NEVER, MAX_COOLDOWN_SECONDS,
                            "vehicle cooldown")));
        }
        int squadCount = readCount(buffer, MAX_DETAIL_ENTRIES, "squads");
        List<FormationDetailView.Squad> squads = new ArrayList<>(squadCount);
        for (int index = 0; index < squadCount; index++) {
            squads.add(new FormationDetailView.Squad(buffer.readUtf(MAX_DISPLAY_NAME),
                    readBounded(buffer, 1, MAX_DETAIL_COUNT, "squad capacity")));
        }
        int outposts = readBounded(buffer, 0, MAX_DEPLOYABLES, "outposts");
        int rally = readBounded(buffer, 0, MAX_DEPLOYABLES, "rally packs");
        int respawn = readBounded(buffer, FormationDetailView.INHERIT_RESPAWN,
                MAX_RESPAWN_SECONDS, "respawn delay");
        int mobileCount = readCount(buffer, MAX_DETAIL_ENTRIES, "mobile spawn vehicles");
        List<String> mobile = new ArrayList<>(mobileCount);
        for (int index = 0; index < mobileCount; index++) {
            mobile.add(buffer.readUtf(MAX_DISPLAY_NAME));
        }
        FormationSupportPolicy.Mode mode;
        try {
            mode = FormationSupportPolicy.Mode.valueOf(buffer.readUtf(MAX_PHASE));
        } catch (IllegalArgumentException invalidMode) {
            throw new IllegalArgumentException("Invalid formation support mode", invalidMode);
        }
        int supportCount = readCount(buffer, MAX_DETAIL_ENTRIES, "support ids");
        List<String> supports = new ArrayList<>(supportCount);
        for (int index = 0; index < supportCount; index++) {
            supports.add(buffer.readUtf(MAX_SUMMARY));
        }
        return new FormationDetailView(classes, vehicles, squads, outposts, rally, respawn,
                mobile, mode, supports);
    }

    private static FormationVotePhase readPhase(FriendlyByteBuf buffer) {
        try {
            return FormationVotePhase.valueOf(buffer.readUtf(MAX_PHASE));
        } catch (IllegalArgumentException invalidPhase) {
            throw new IllegalArgumentException("Invalid formation vote phase", invalidPhase);
        }
    }

    private static void writeStrings(FriendlyByteBuf buffer, List<String> values) {
        writeCount(buffer, values.size(), MAX_SUMMARIES, "summaries");
        values.forEach(value -> buffer.writeUtf(clip(value, MAX_SUMMARY), MAX_SUMMARY));
    }

    /**
     * Display-only text is built from admin config at runtime; an overlong line must not make
     * the snapshot unencodable, because that kicks the player during login.
     */
    static String clip(String value, int maximum) {
        if (value == null || value.length() <= maximum) {
            return value;
        }
        int end = maximum - 1;
        if (Character.isHighSurrogate(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(0, end) + "…";
    }

    private static List<String> readStrings(FriendlyByteBuf buffer) {
        int count = readCount(buffer, MAX_SUMMARIES, "summaries");
        List<String> values = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            values.add(buffer.readUtf(MAX_SUMMARY));
        }
        return List.copyOf(values);
    }

    private static void writeCount(FriendlyByteBuf buffer, int value, int maximum, String name) {
        if (value < 0 || value > maximum) {
            throw new IllegalArgumentException("Too many " + name + ": " + value);
        }
        buffer.writeVarInt(value);
    }

    private static int readCount(FriendlyByteBuf buffer, int maximum, String name) {
        int count = buffer.readVarInt();
        if (count < 0 || count > maximum) {
            throw new IllegalArgumentException("Invalid " + name + " count: " + count);
        }
        return count;
    }

    private static void writeBounded(FriendlyByteBuf buffer, int value, int minimum, int maximum,
                                     String name) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException("Invalid " + name + ": " + value);
        }
        buffer.writeVarInt(value);
    }

    private static int readBounded(FriendlyByteBuf buffer, int minimum, int maximum,
                                   String name) {
        int value = buffer.readVarInt();
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException("Invalid " + name + ": " + value);
        }
        return value;
    }

    private static int readBoundedPopulation(FriendlyByteBuf buffer, String name) {
        int value = buffer.readVarInt();
        if (value < 0 || value > 128) {
            throw new IllegalArgumentException("Invalid " + name + ": " + value);
        }
        return value;
    }

    private static int readBoundedCapacity(FriendlyByteBuf buffer, String name) {
        int value = buffer.readVarInt();
        if (value < 1 || value > 128) {
            throw new IllegalArgumentException("Invalid " + name + ": " + value);
        }
        return value;
    }
}
