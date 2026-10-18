package com.wok.downed.compat;

import com.wok.downed.WokDownedMod;
import com.wok.downed.config.DownedConfig;
import com.wok.downed.state.DownedService;
import com.wok.downed.state.DownedWeaponPolicy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
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
 * <p>Everything about TaCZ is reached reflectively, so this module never links against TaCZ
 * classes: it stays independently installable and cannot break when TaCZ changes.
 *
 * <h2>Why not {@code @SubscribeEvent}</h2>
 * A listener whose parameter is the base {@link Event} type receives <em>every</em> event on the
 * bus, including events that are not cancellable and events fired before TaCZ registers anything.
 * Calling {@code setCanceled} on such an event throws and takes the game down during loading.
 *
 * <p>Forge's own {@code BusBuilder.markerType} would restrict a private bus to one type, but it
 * hard-rejects anything that is not an interface
 * ({@code IllegalArgumentException: Cannot specify a class marker type}), and
 * {@code GunShootEvent} is a class. This listener is therefore added with the generic
 * {@code addListener(Consumer)} overload — which subscribes to the {@link Event} base type — and
 * {@link #onAnyEvent} returns immediately unless the event really is TaCZ's, both by class and by
 * the {@code isCancelable} contract. That keeps the registration free of TaCZ classes while never
 * touching an event the rule does not own.
 */
public final class TaczSidearmCompat {
    static final String GUN_SHOOT_EVENT = "com.tacz.guns.api.event.common.GunShootEvent";

    private static boolean installed;
    private static boolean reportedShape;
    private static Class<?> gunShootEventType;

    /** Registers the listener once, binding TaCZ's event class by name when the mod is present. */
    public static void install() {
        if (installed || !ModList.get().isLoaded("tacz")) {
            return;
        }
        try {
            gunShootEventType = Class.forName(GUN_SHOOT_EVENT);
        } catch (ClassNotFoundException | LinkageError missing) {
            WokDownedMod.LOGGER.warn(
                    "TaCZ is installed but {} is unavailable; the downed sidearm rule is inactive",
                    GUN_SHOOT_EVENT);
            return;
        }
        IEventBus bus = MinecraftForge.EVENT_BUS;
        addEventConsumer(bus, TaczSidearmCompat::onAnyEvent);
        installed = true;
        WokDownedMod.LOGGER.info(
                "TaCZ detected: a downed player may only fire the configured sidearms");
    }

    /** Keeps the generic {@code Consumer<? super T>} bound to {@link Event} explicitly. */
    private static void addEventConsumer(IEventBus bus, Consumer<Event> listener) {
        bus.addListener(EventPriority.NORMAL, false, listener);
    }

    /** Package-visible for tests: whether the listener was wired. */
    static boolean isInstalled() {
        return installed;
    }

    /**
     * Forwards only TaCZ's shoot event. Everything else returns before any state is touched, so an
     * unrelated event can never be cancelled and a pre-TaCZ event can never throw.
     */
    static void onAnyEvent(Event event) {
        if (event == null || gunShootEventType == null
                || !gunShootEventType.isInstance(event) || !event.isCancelable()) {
            return;
        }
        onGunShoot(event);
    }

    /** Only reached for a cancelable {@code GunShootEvent}. */
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
