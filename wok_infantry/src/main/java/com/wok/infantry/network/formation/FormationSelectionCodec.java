package com.wok.infantry.network.formation;

import com.wok.infantry.formation.selection.FactionSelectionView;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.formation.selection.FormationSelectionView;
import com.wok.infantry.formation.vote.FormationVotePhase;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/** Strict bounded codec for the public selection catalog. */
public final class FormationSelectionCodec {
    public static final int MAX_ID = 64;
    public static final int MAX_DISPLAY_NAME = 40;
    public static final int MAX_DESCRIPTION = 512;
    public static final int MAX_REASON = 256;
    public static final int MAX_SUMMARY = 192;
    public static final int MAX_FACTIONS = 16;
    public static final int MAX_FORMATIONS = 32;
    public static final int MAX_SUMMARIES = 64;

    private FormationSelectionCodec() {
    }

    public static void encode(FriendlyByteBuf buffer, FormationSelectionSnapshot snapshot) {
        buffer.writeVarLong(snapshot.generation());
        buffer.writeBoolean(snapshot.selectionRequired());
        buffer.writeUtf(snapshot.selectedFactionId(), MAX_ID);
        buffer.writeUtf(snapshot.selectedFormationId(), MAX_ID);
        buffer.writeUtf(snapshot.votePhase().name(), 16);
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
                buffer.writeUtf(formation.unavailableReason(), MAX_REASON);
                writeStrings(buffer, formation.classes());
                writeStrings(buffer, formation.squads());
                writeStrings(buffer, formation.vehicles());
                writeStrings(buffer, formation.capabilities());
            }
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
        FormationVotePhase votePhase;
        try {
            votePhase = FormationVotePhase.valueOf(buffer.readUtf(16));
        } catch (IllegalArgumentException invalidPhase) {
            throw new IllegalArgumentException("Invalid formation vote phase", invalidPhase);
        }
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
                formations.add(new FormationSelectionView(formationId, formationName,
                        formationDescription, formationIcon, categoryId, categoryDisplayName,
                        formationPopulation, formationCapacity, formationAvailable, reason,
                        classes, squads, vehicles, capabilities));
            }
            factions.add(new FactionSelectionView(factionId, displayName, description,
                    population, capacity, available, formations));
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
                voteTally, factions);
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

    private static void writeStrings(FriendlyByteBuf buffer, List<String> values) {
        writeCount(buffer, values.size(), MAX_SUMMARIES, "summaries");
        values.forEach(value -> buffer.writeUtf(value, MAX_SUMMARY));
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
