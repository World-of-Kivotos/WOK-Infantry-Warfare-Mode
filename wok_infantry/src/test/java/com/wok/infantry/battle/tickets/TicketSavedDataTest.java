package com.wok.infantry.battle.tickets;

import com.wok.infantry.battle.Faction;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class TicketSavedDataTest {
    @Test void unspentNewRoundIsSavedBeforeFirstCasualty() {
        TicketSavedData data = new TicketSavedData(500);
        assertTrue(data.isDirty());
        TicketSavedData loaded = TicketSavedData.load(data.save(new CompoundTag()));
        assertEquals(500, loaded.remaining(Faction.BLUE));
        assertFalse(loaded.finished());
    }
    @Test void lossesAreDeduplicatedAcrossSaveAndReload() {
        TicketSavedData data = new TicketSavedData(500);
        UUID loss = UUID.randomUUID();
        assertTrue(data.debit(Faction.BLUE, 20, loss));
        assertFalse(data.debit(Faction.BLUE, 20, loss));
        TicketSavedData loaded = TicketSavedData.load(data.save(new CompoundTag()));
        assertFalse(loaded.debit(Faction.BLUE, 20, loss));
        assertEquals(480, loaded.remaining(Faction.BLUE));
        assertEquals(500, loaded.remaining(Faction.RED));
    }
    @Test void zeroEndsRoundAndResetStartsFresh() {
        TicketSavedData data = new TicketSavedData(2);
        assertTrue(data.debit(Faction.RED, 100, UUID.randomUUID()));
        assertEquals(0, data.remaining(Faction.RED));
        assertTrue(data.finished());
        assertEquals(Faction.BLUE, data.winner());
        assertFalse(data.debit(Faction.BLUE, 100, null));
        assertTrue(TicketSavedData.load(data.save(new CompoundTag())).finished());
        data.reset(500);
        assertFalse(data.finished());
        assertEquals(500, data.remaining(Faction.RED));
    }
}
