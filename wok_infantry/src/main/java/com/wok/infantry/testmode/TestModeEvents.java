package com.wok.infantry.testmode;

import com.wok.infantry.WokInfantryMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge lifecycle of the server-wide test mode: restore after a restart, boss bar upkeep. */
@Mod.EventBusSubscriber(modid = WokInfantryMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TestModeEvents {
    private TestModeEvents() {
    }

    /** After the battle services started (they are created on demand if they have not yet). */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onServerStarted(ServerStartedEvent event) {
        TestModeService.start(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TestModeService.stop(event.getServer());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TestModeService.get(player.server).ifPresent(service ->
                    service.onPlayerLoggedIn(player));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TestModeService.get(player.server).ifPresent(service ->
                    service.onPlayerLoggedOut(player));
        }
    }

    /** Once a second: the boss bar follows respawned player entities and late joiners. */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 20 != 10) {
            return;
        }
        TestModeService.get(event.getServer()).ifPresent(TestModeService::syncBossBar);
    }
}
