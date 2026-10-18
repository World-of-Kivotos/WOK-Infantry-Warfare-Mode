package com.wok.downed.compat;

import com.wok.downed.WokDownedMod;
import com.wok.downed.config.DownedConfig;
import com.wok.downed.state.DownedService;
import com.wok.downed.state.DownedWeaponPolicy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.BusBuilderImpl;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;

import java.util.function.Consumer;

/**
 * Gates TaCZ gunfire for a downed casualty: a listed sidearm fires, everything else is cancelled.
 *
 * <p>TaCZ posts {@code GunShootEvent} on the client immediately before committing a shot; a
 * cancelled event makes TaCZ return {@code FORGE_EVENT_CANCEL} without consuming ammo, playing a
 * shot sound or sending a packet. TaCZ also posts it on the server, so cancelling there too stops
 * a modified client from bypassing the rule.
 *
 * <p>The event is handled through the plain {@link Event} API plus reflection, so this module
 * never links against TaCZ classes: it stays independently installable and cannot break when TaCZ
 * changes.
 *
 * <p><b>Why a dedicated bus and not {@code @SubscribeEvent}?</b> A listener whose parameter is the
 * base {@link Event} type is invoked for <em>every</em> event on the bus, and {@code setCanceled}
 * on an event that is not cancellable throws. Registering a private bus with TaCZ's event class as
 * its marker type keeps every other event — and every event fired before or without TaCZ — out of
 * {@link #onGunShoot}.
 */
public final class TaczSidearmCompat {
    static final String GUN_SHOOT_EVENT = "com.tacz.guns.api.event.common.GunShootEvent";

    private static boolean installed;
    private static boolean reportedShape;
    private static IEventBus bus;

    /** Registers the filtered bus once, using TaCZ's own event class when the mod is present. */
    public static void install() {
        if (installed || !ModList.get().isLoaded("tacz")) {
            return;
        }
        Class<?> gunShootEvent;
        try {
            gunShootEvent = Class.forName(GUN_SHOOT_EVENT);
        } catch (ClassNotFoundException | LinkageError missing) {
            WokDownedMod.LOGGER.warn(
                    "TaCZ is installed but {} is unavailable; the downed sidearm rule is inactive",
                    GUN_SHOOT_EVENT);
            return;
        }
        BusBuilderImpl builder = new BusBuilderImpl();
        builder.setTrackPhases(false);
        builder.markerType(gunShootEvent);
        builder.setExceptionHandler((source, event, listeners, index, throwable) ->
                WokDownedMod.LOGGER.error("Downed sidearm check failed for {}", event, throwable));
        IEventBus filtered = new net.minecraftforge.eventbus.EventBus(builder);
        addFilteredListener(filtered, gunShootEvent);
        bus = filtered;
        installed = true;
        WokDownedMod.LOGGER.info(
                "TaCZ detected: a downed player may only fire the configured sidearms");
    }

    /** Binds the generic {@code Class<T>} parameter so the reflective class can be registered. */
    private static <T extends Event> void addFilteredListener(IEventBus target, Class<?> type) {
        @SuppressWarnings("unchecked")
        Class<T> eventType = (Class<T>) type;
        target.addListener(EventPriority.NORMAL, false, eventType,
                (Consumer<T>) (Consumer<Event>) TaczSidearmCompat::onGunShoot);
    }

    /** Package-visible for tests: whether the filtered bus was wired. */
    static boolean isInstalled() {
        return installed;
    }

    /** Package-visible for tests: the private bus, if any. */
    static IEventBus bus() {
        return bus;
    }

    /**
     * Only ever reached for {@code GunShootEvent} instances (the bus marker type is that class),
     * so a cancel here cannot land on an event that does not support it.
     */
    static void onGunShoot(Event event) {
        Object shooter;
        Object stack;
        try {
            Class<?> type = event.getClass();
            shooter = type.getMethod("getShooter").invoke(event);
            stack = type.getMethod("getGunItemStack").invoke(event);
        } catch (ReflectiveOperationException failure) {
            if (!reportedShape) {
                reportedShape = true;
                WokDownedMod.LOGGER.error(
                        "TaCZ GunShootEvent shape is unexpected; blocking the shot", failure);
            }
            event.setCanceled(true);
            return;
        }
        if (!(shooter instanceof Player player) || !(stack instanceof ItemStack gun)) {
            return;
        }
        if (!DownedService.isDowned(player)) {
            return;
        }
        boolean allowed = DownedConfig.ALLOW_SIDEARM_FIRE.get()
                && DownedWeaponPolicy.isAllowedSidearm(gun, DownedConfig.ALLOWED_SIDEARMS.get());
        if (!allowed) {
            event.setCanceled(true);
        }
    }

    private TaczSidearmCompat() {
    }
}
