package com.wok.infantry.formation.vote;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.battle.Faction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** World-persistent formation ballots. Match policy remains outside this storage boundary. */
public final class FormationVoteSavedData extends SavedData {
    private static final String DATA_NAME = WokInfantryMod.MOD_ID + "_formation_votes";
    private static final int DATA_VERSION = 1;
    private static final int MAX_CANDIDATES = 32;

    private final FormationVoteLedger ledger = new FormationVoteLedger();

    public static FormationVoteSavedData get(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return server.overworld().getDataStorage().computeIfAbsent(
                FormationVoteSavedData::load, FormationVoteSavedData::new, DATA_NAME);
    }

    public static FormationVoteSavedData load(CompoundTag root) {
        FormationVoteSavedData data = new FormationVoteSavedData();
        if (root == null || root.getInt("Version") != DATA_VERSION
                || !root.contains("Ballots", Tag.TAG_LIST)) {
            return data;
        }
        ListTag ballots = root.getList("Ballots", Tag.TAG_COMPOUND);
        for (int index = 0; index < Math.min(ballots.size(), Faction.values().length); index++) {
            CompoundTag ballot = ballots.getCompound(index);
            Faction faction = Faction.byId(ballot.getString("Faction")).orElse(null);
            if (faction == null
                    || data.ledger.snapshot(faction, null).phase()
                    != FormationVotePhase.NOT_STARTED) {
                continue;
            }
            List<String> candidates = readCandidates(ballot);
            if (!data.ledger.open(faction, candidates,
                    ballot.getBoolean("AllowVoteChange")).success()) {
                continue;
            }
            ListTag votes = ballot.getList("Votes", Tag.TAG_COMPOUND);
            int voteLimit = Math.min(votes.size(), BattleRules.FACTION_CAPACITY);
            for (int voteIndex = 0; voteIndex < voteLimit; voteIndex++) {
                CompoundTag vote = votes.getCompound(voteIndex);
                if (!vote.hasUUID("Voter")) {
                    continue;
                }
                try {
                    data.ledger.cast(faction, vote.getUUID("Voter"),
                            vote.getString("Formation"));
                } catch (RuntimeException ignored) {
                    // Corrupt entries are omitted; no malformed vote can manufacture a candidate.
                }
            }
            FormationVotePhase phase = parsePhase(ballot.getString("Phase"));
            if (phase == FormationVotePhase.LOCKED) {
                data.ledger.lock(faction, ballot.getString("LockedFormation"));
            }
        }
        return data;
    }

    public FormationVoteResult open(Faction faction, List<String> candidates,
                                    boolean allowVoteChange) {
        long before = snapshotRevision(faction);
        FormationVoteResult result = ledger.open(faction, candidates, allowVoteChange);
        markChanged(faction, before);
        return result;
    }

    public FormationVoteResult cast(Faction faction, UUID voterId, String formationId) {
        long before = snapshotRevision(faction);
        FormationVoteResult result = ledger.cast(faction, voterId, formationId);
        markChanged(faction, before);
        return result;
    }

    public FormationVoteResult lock(Faction faction, String formationId) {
        long before = snapshotRevision(faction);
        FormationVoteResult result = ledger.lock(faction, formationId);
        markChanged(faction, before);
        return result;
    }

    public FormationVoteSnapshot snapshot(Faction faction, UUID viewerId) {
        return ledger.snapshot(faction, viewerId);
    }

    public void clear(Faction faction) {
        long before = snapshotRevision(faction);
        ledger.clear(faction);
        markChanged(faction, before);
    }

    public void clearAll() {
        long before = ledger.snapshot(Faction.BLUE, null).revision();
        ledger.clearAll();
        if (ledger.snapshot(Faction.BLUE, null).revision() != before) {
            setDirty();
        }
    }

    public boolean reconcileCandidates(Faction faction, List<String> candidates) {
        boolean changed = ledger.reconcileCandidates(faction, candidates);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        root.putInt("Version", DATA_VERSION);
        ListTag ballots = new ListTag();
        ledger.storedBallots().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> ballots.add(writeBallot(entry.getKey(), entry.getValue())));
        root.put("Ballots", ballots);
        return root;
    }

    private long snapshotRevision(Faction faction) {
        return faction == null ? -1L : ledger.snapshot(faction, null).revision();
    }

    private void markChanged(Faction faction, long before) {
        if (faction != null && ledger.snapshot(faction, null).revision() != before) {
            setDirty();
        }
    }

    private static CompoundTag writeBallot(Faction faction,
                                           FormationVoteLedger.StoredBallot ballot) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Faction", faction.id());
        tag.putString("Phase", ballot.phase().name());
        tag.putString("LockedFormation", ballot.lockedFormationId());
        tag.putBoolean("AllowVoteChange", ballot.allowVoteChange());
        ListTag candidates = new ListTag();
        ballot.candidates().forEach(candidate -> candidates.add(StringTag.valueOf(candidate)));
        tag.put("Candidates", candidates);
        ListTag votes = new ListTag();
        ballot.votes().entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    CompoundTag vote = new CompoundTag();
                    vote.putUUID("Voter", entry.getKey());
                    vote.putString("Formation", entry.getValue());
                    votes.add(vote);
                });
        tag.put("Votes", votes);
        return tag;
    }

    private static List<String> readCandidates(CompoundTag ballot) {
        ListTag tags = ballot.getList("Candidates", Tag.TAG_STRING);
        List<String> candidates = new ArrayList<>();
        for (int index = 0; index < Math.min(tags.size(), MAX_CANDIDATES); index++) {
            candidates.add(tags.getString(index));
        }
        return candidates;
    }

    private static FormationVotePhase parsePhase(String value) {
        try {
            return FormationVotePhase.valueOf(Objects.requireNonNullElse(value, ""));
        } catch (IllegalArgumentException ignored) {
            return FormationVotePhase.OPEN;
        }
    }
}
