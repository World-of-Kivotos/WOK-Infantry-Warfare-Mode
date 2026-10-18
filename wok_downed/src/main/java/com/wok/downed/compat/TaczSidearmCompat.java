package com.wok.downed.compat;

import com.wok.downed.WokDownedMod;
import com.wok.downed.config.DownedConfig;
import com.wok.downed.state.DownedService;
import com.wok.downed.state.DownedWeaponPolicy;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

/**
 * Gates TaCZ gunfire for a downed casualty: a listed sidearm fires, everything else is cancelled.
 *
 * <p>TaCZ posts {@code GunShootEvent} on the client immediately before committing a shot; a
 * cancelled event makes TaCZ return {@code FORGE_EVENT_CANCEL} without consuming ammo, playing a
 * shot sound or sending a packet. TaCZ also posts it on the server, so cancelling there too stops
 * a modified client from bypassing the rule.
 *
 * <p>The listener is registered only when TaCZ is installed, and the event is handled through the
 * plain {@link Event} API plus reflection, so this module never links against TaCZ classes: it
 * stays independently installable and cannot break when TaCZ changes.
 */
public final class TaczSidearmCompat {
    private static boolean registered;
    private static boolean reportedShape;

    /** Registers the listener once, if TaCZ is present. Safe to call more than once. */
    public static void install() {
        if (registered || !ModList.get().isLoaded("tacz")) {
            return;
        }
        registered = true;
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(TaczSidearmCompat.class);
        WokDownedMod.LOGGER.info(
                "TaCZ detected: a downed player may only fire the configured sidearms");
    }

    /** Package-visible for the unit test seam: whether the listener was wired. */
    static boolean isInstalled() {
        return registered;
    }

    @SubscribeEvent
    public static void onGunShoot(Event event) {
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
