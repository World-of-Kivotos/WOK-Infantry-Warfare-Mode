package com.wok.commandersupport.drone;

import com.wok.commandersupport.WokCommanderSupportMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Server-side backstop that removes recon drones even when they no longer tick.
 *
 * <p>A drone normally ends itself in {@link ReconDroneEntity#tick()}: at its expiry tick, at the
 * end of its departure, or when its next position leaves the simulated area. That only works
 * while the server ticks it. A drone whose chunk stays loaded but drops out of the entity-ticking
 * range (between the simulation and the view distance), or whose dimension stopped ticking
 * entities because nobody is there, is frozen in the air while clients keep flying it along its
 * computed path. The mission notices that at its next step, but a battle reset or a crashed
 * mission never calls the provider again, and a departing or falling drone has no mission left.</p>
 *
 * <p>Every drone added to a server level is therefore registered here, by weak reference, and
 * checked once per server tick on its own level clock: past its expiry (unless it is a falling
 * wreck, which ends by itself), or not ticked for more than {@link #STALL_GRACE_TICKS}, it is
 * discarded on the spot. Discarding an entity that does not tick is safe. Nothing is persisted;
 * the list is cleared whenever a server starts or stops. Failures are only logged.</p>
 *
 * <p>Registers itself on the Forge event bus through {@link Mod.EventBusSubscriber}; nothing else
 * may hook {@link #onServerTick} again.</p>
 */
@Mod.EventBusSubscriber(modid = WokCommanderSupportMod.MOD_ID)
public final class ReconDroneWatchdog {
    /**
     * Ticks a drone may go without a server tick before it counts as frozen. Well inside one
     * 40-tick mission step, so a frozen drone is gone before the mission checks on it twice.
     */
    static final long STALL_GRACE_TICKS = 20L;

    private static final List<WeakReference<ReconDroneEntity>> WATCHED = new ArrayList<>();

    private ReconDroneWatchdog() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            sweep();
        }
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        clear();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        clear();
    }

    /** Server side, when a drone joins a level. */
    static synchronized void watch(ReconDroneEntity drone) {
        if (drone != null) {
            WATCHED.add(new WeakReference<>(drone));
        }
    }

    static synchronized int watched() {
        return WATCHED.size();
    }

    private static synchronized void clear() {
        WATCHED.clear();
    }

    /** Removes every overdue drone. Never throws into the server tick. */
    static void sweep() {
        List<ReconDroneEntity> overdue = collectOverdue();
        for (ReconDroneEntity drone : overdue) {
            try {
                WokCommanderSupportMod.LOGGER.info(
                        "Recon drone {} stopped ticking or outlived its mission and was removed",
                        drone.getUUID());
                drone.discard();
            } catch (RuntimeException | LinkageError failure) {
                WokCommanderSupportMod.LOGGER.warn("Recon drone {} could not be removed",
                        drone.getUUID(), failure);
            }
        }
    }

    /** Drops removed and collected drones from the list and returns the overdue ones. */
    private static synchronized List<ReconDroneEntity> collectOverdue() {
        if (WATCHED.isEmpty()) {
            return List.of();
        }
        List<ReconDroneEntity> overdue = new ArrayList<>();
        Iterator<WeakReference<ReconDroneEntity>> iterator = WATCHED.iterator();
        while (iterator.hasNext()) {
            ReconDroneEntity drone = iterator.next().get();
            if (drone == null || drone.isRemoved()) {
                iterator.remove();
                continue;
            }
            try {
                if (drone.overdue(drone.level().getGameTime())) {
                    iterator.remove();
                    overdue.add(drone);
                }
            } catch (RuntimeException | LinkageError failure) {
                iterator.remove();
                overdue.add(drone);
                WokCommanderSupportMod.LOGGER.warn("Recon drone {} could not be checked",
                        drone.getUUID(), failure);
            }
        }
        return overdue;
    }

    /**
     * Pure rule: a drone is overdue once its expiry tick has passed (a falling wreck excepted, it
     * ends on its own within {@link ReconDroneFlight#CRASH_MAX_TICKS}), or once the server has
     * not ticked it for more than {@link #STALL_GRACE_TICKS}. A drone that was never launched by
     * a mission carries no expiry and no tick, so it is overdue at once.
     */
    static boolean overdue(long now, long lastTickedGameTime, long expireGameTime,
                           boolean crashing) {
        if (!crashing && now >= expireGameTime) {
            return true;
        }
        if (lastTickedGameTime >= now) {
            return false;
        }
        long sinceTick = now - lastTickedGameTime;
        // A negative difference with an earlier last tick means the subtraction overflowed.
        return sinceTick < 0L || sinceTick > STALL_GRACE_TICKS;
    }
}
