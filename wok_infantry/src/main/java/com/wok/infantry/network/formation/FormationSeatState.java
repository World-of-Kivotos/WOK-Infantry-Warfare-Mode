package com.wok.infantry.network.formation;

import com.wok.infantry.battle.BattleService;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.PlayerRecord;
import com.wok.infantry.server.FormationService;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * A player's faction, formation and own vote, taken before a faction, vote or assignment request
 * and compared with the state afterwards. Only a real change is pushed to the faction's other
 * members; a repeated or refused request answers its sender alone (NET-1), so a player cannot
 * make the server rebuild every teammate's catalog by repeating a request that changes nothing.
 */
public record FormationSeatState(Faction faction, String formationId, String ownVote) {
    public static final FormationSeatState NONE = new FormationSeatState(null, "", "");

    public FormationSeatState {
        formationId = Objects.requireNonNullElse(formationId, "");
        ownVote = Objects.requireNonNullElse(ownVote, "");
    }

    /** Current state of {@code player} on its server ({@link #NONE} without a battle record). */
    public static FormationSeatState of(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        PlayerRecord record = BattleService.get(player)
                .flatMap(battle -> battle.playerRecord(player.getUUID())).orElse(null);
        if (record == null) {
            return NONE;
        }
        Faction faction = record.faction();
        String ownVote = faction == null ? "" : FormationService.get(player)
                .map(service -> service.voteSnapshot(faction, player.getUUID()).ownVote())
                .orElse("");
        return new FormationSeatState(faction, record.formationId(), ownVote);
    }

    /** The faction or the formation differs (a vote alone does not count). */
    public boolean seatChanged(FormationSeatState after) {
        Objects.requireNonNull(after, "after");
        return faction != after.faction || !formationId.equals(after.formationId);
    }

    /** Anything the faction's other members see differs: seat or own vote. */
    public boolean changed(FormationSeatState after) {
        return seatChanged(after) || !ownVote.equals(after.ownVote);
    }

    /**
     * Factions whose other online members must refresh their catalog: the old and the new
     * faction when something changed, none otherwise.
     */
    public Set<Faction> affectedFactions(FormationSeatState after) {
        EnumSet<Faction> affected = EnumSet.noneOf(Faction.class);
        if (!changed(after)) {
            return affected;
        }
        if (faction != null) {
            affected.add(faction);
        }
        if (after.faction != null) {
            affected.add(after.faction);
        }
        return affected;
    }
}
