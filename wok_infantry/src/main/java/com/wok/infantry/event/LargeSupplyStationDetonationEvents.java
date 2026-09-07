package com.wok.infantry.event;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.ammo.LargeSupplyStationDetonation;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Drives and clears the staged station-destruction timeline on the logical server. */
@Mod.EventBusSubscriber(modid = WokInfantryMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LargeSupplyStationDetonationEvents {
    private LargeSupplyStationDetonationEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            LargeSupplyStationDetonation.tick(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            LargeSupplyStationDetonation.resumeIfInterrupted(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        LargeSupplyStationDetonation.clear();
    }
}
