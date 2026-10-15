package com.wok.infantry.client.tablet;

import com.wok.infantry.integration.tacz.TaczStaminaAdapter;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * What the animation reads from the game (IMPL_PLAN D16, D17; DESIGN 3.4, 3.7): the held item's
 * kind, whether the first-person hand pass can be expected, and the signals that interrupt it.
 * The rules are pure static methods; {@link #hand}, {@link #predictNoHands} and {@link Watch#of}
 * read the client.
 */
public final class TabletEnvironment {
    /** Superb Warfare's namespace; its guns are recognised by name only (never linked). */
    static final String SBW_NAMESPACE = "superbwarfare";

    private TabletEnvironment() {
    }

    // ---- held item (D16) ----------------------------------------------------------------------

    /** The main hand of {@code player} when the tablet comes out; unknown kinds count as items. */
    public static TabletHand hand(LocalPlayer player) {
        if (player == null) {
            return TabletHand.ITEM;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            return TabletHand.EMPTY;
        }
        boolean tacz = false;
        try {
            tacz = TaczStaminaAdapter.isHoldingGun(player);
        } catch (RuntimeException | LinkageError ignored) {
            // An unexpected TaCZ build: the item is judged by its name below.
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        boolean sbw = id != null && sbwGun(id.getNamespace(), stack.getItem().getClass());
        return TabletHand.classify(false, tacz, sbw);
    }

    /**
     * A Superb Warfare gun: an item of its namespace whose class or a superclass is named
     * {@code *GunItem} (by name, so nothing of SBW is linked).
     */
    static boolean sbwGun(String namespace, Class<?> type) {
        if (!SBW_NAMESPACE.equals(namespace)) {
            return false;
        }
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            if (c.getSimpleName().endsWith("GunItem")) {
                return true;
            }
        }
        return false;
    }

    // ---- hand pass prediction (D17) --------------------------------------------------------------

    /** Whether the first-person hand pass will not run, read from the client. */
    public static boolean predictNoHands(Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null) {
            return true;
        }
        LocalPlayer player = minecraft.player;
        return predictNoHands(minecraft.options.hideGui,
                minecraft.options.getCameraType() == CameraType.FIRST_PERSON,
                player.isSpectator(), player.isSleeping(),
                minecraft.getCameraEntity() == player, player.isPassenger());
    }

    /**
     * D17: scheme A needs the first-person hand pass; it does not run with F1, in third person,
     * for a spectator, while sleeping, through another entity's eyes (a drone) or in a vehicle
     * (Superb Warfare cancels it; vanilla boats and horses only lose a little).
     */
    public static boolean predictNoHands(boolean hideGui, boolean firstPerson, boolean spectator,
                                         boolean sleeping, boolean cameraIsPlayer,
                                         boolean passenger) {
        return hideGui || !firstPerson || spectator || sleeping || !cameraIsPlayer || passenger;
    }

    // ---- interrupts (DESIGN 3.4) ------------------------------------------------------------------

    /**
     * What the interrupt rules compare from tick to tick (identities only).
     *
     * @param level   the client level
     * @param vehicle what the player rides, or {@code null}
     * @param camera  the camera entity
     * @param player  the local player
     * @param slot    the selected hotbar slot
     * @param alive   the player is alive
     */
    public record Watch(Object level, Object vehicle, Object camera, Object player, int slot,
                        boolean alive) {
        /** The current state, or {@code null} without a player. */
        public static Watch of(Minecraft minecraft) {
            if (minecraft == null || minecraft.player == null) {
                return null;
            }
            LocalPlayer player = minecraft.player;
            return new Watch(minecraft.level, player.getVehicle(), minecraft.getCameraEntity(),
                    player, player.getInventory().selected, player.isAlive());
        }
    }

    /**
     * The interrupt between two ticks, or {@code null}: death, a new level (dimension), getting on
     * a vehicle, a new camera entity; while closing also a new hotbar slot, firing (attack held
     * with no screen) and aiming (use held, or TaCZ aiming). {@link TabletMotion#interrupt} then
     * decides whether it ends the animation (an opening only ends for the first four).
     */
    public static TabletMotion.Interrupt detect(Watch before, Watch now, boolean closing,
                                                boolean attackDown, boolean useDown,
                                                boolean aiming) {
        if (now == null) {
            return null;
        }
        if (!now.alive()) {
            return TabletMotion.Interrupt.DEATH;
        }
        if (before == null) {
            return null;
        }
        if (before.level() != now.level() || before.player() != now.player()) {
            return TabletMotion.Interrupt.DIMENSION;
        }
        if (now.vehicle() != null && before.vehicle() != now.vehicle()) {
            return TabletMotion.Interrupt.VEHICLE;
        }
        if (before.camera() != now.camera()) {
            return TabletMotion.Interrupt.CAMERA;
        }
        if (!closing) {
            return null;
        }
        if (before.slot() != now.slot()) {
            return TabletMotion.Interrupt.HOTBAR;
        }
        if (attackDown) {
            return TabletMotion.Interrupt.FIRE;
        }
        if (useDown || aiming) {
            return TabletMotion.Interrupt.AIM;
        }
        return null;
    }
}
