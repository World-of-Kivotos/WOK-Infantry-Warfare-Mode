package com.wok.infantry.battle.tickets;

import com.wok.infantry.battle.Faction;
import com.wok.infantry.config.BattleGameplayConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class TicketSavedData extends SavedData {
    private int blue, red;
    private final Set<UUID> chargedLosses = new HashSet<>();

    public TicketSavedData(int initial) { blue = red = Math.max(1, initial); setDirty(); }
    public static TicketSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TicketSavedData::load,
                () -> new TicketSavedData(BattleGameplayConfig.INITIAL_TICKETS.get()), "wok_infantry_tickets");
    }
    public int remaining(Faction side) { return side == Faction.BLUE ? blue : red; }
    public boolean finished() { return blue <= 0 || red <= 0; }
    public Faction winner() { return !finished() ? null : blue <= 0 ? Faction.RED : Faction.BLUE; }

    /** The life issue token makes repeated callbacks and reconnects idempotent. */
    public boolean debit(Faction side, int amount, UUID lossId) {
        if (side == null || amount <= 0 || finished()) return false;
        if (lossId != null && !chargedLosses.add(lossId)) return false;
        if (side == Faction.BLUE) blue = Math.max(0, blue - amount);
        else red = Math.max(0, red - amount);
        setDirty();
        return true;
    }
    public void reset(int initial) {
        blue = red = Math.max(1, Math.min(1_000_000, initial));
        chargedLosses.clear(); setDirty();
    }
    public static TicketSavedData load(CompoundTag root) {
        TicketSavedData data = new TicketSavedData(1);
        data.blue = Math.max(0, Math.min(1_000_000, root.getInt("Blue")));
        data.red = Math.max(0, Math.min(1_000_000, root.getInt("Red")));
        ListTag losses = root.getList("Losses", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(losses.size(), 1_000_000); i++) {
            if (losses.getCompound(i).hasUUID("Id")) data.chargedLosses.add(losses.getCompound(i).getUUID("Id"));
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag root) {
        root.putInt("Blue", blue); root.putInt("Red", red);
        ListTag losses = new ListTag();
        for (UUID id : chargedLosses) { CompoundTag tag = new CompoundTag(); tag.putUUID("Id", id); losses.add(tag); }
        root.put("Losses", losses);
        return root;
    }
}
