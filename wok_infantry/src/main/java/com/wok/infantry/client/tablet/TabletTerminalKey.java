package com.wok.infantry.client.tablet;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * The client's terminal-key and map-key latches (DESIGN 2.2): since 0.5.0-beta.4 the terminal key
 * opens the squad page and the map key the tactical map in the same frame, so a held key would
 * close and reopen them at the key repeat rate (the server's one-second cooldown used to hide this
 * for the map). Opening or closing with a key latches it; the latch clears on the key's release
 * ({@code ScreenEvent.KeyReleased.Pre} with a screen open, {@code InputEvent.Key} release without
 * one) and on every client tick in which the key is not physically held (IMPL_PLAN D7).
 *
 * <p>Client thread only. {@code ClientBootstrap} installs the release hooks.
 */
public final class TabletTerminalKey {
    private static final TabletKeyLatch LATCH = new TabletKeyLatch();
    private static final TabletKeyLatch MAP_LATCH = new TabletKeyLatch();
    private static Supplier<List<KeyMapping>> mappings = List::of;
    private static Supplier<List<KeyMapping>> mapMappings = List::of;
    private static boolean installed;

    private TabletTerminalKey() {
    }

    /**
     * Installs the release hooks once; {@code terminalMappings} lists the key mappings that open the
     * terminal (the terminal key and the former squad key).
     */
    public static void install(Supplier<List<KeyMapping>> terminalMappings) {
        install(terminalMappings, List::of);
    }

    /** {@link #install(Supplier)} with the map key mappings as well. */
    public static void install(Supplier<List<KeyMapping>> terminalMappings,
                               Supplier<List<KeyMapping>> mapKeyMappings) {
        mappings = Objects.requireNonNull(terminalMappings, "terminalMappings");
        mapMappings = Objects.requireNonNull(mapKeyMappings, "mapKeyMappings");
        if (installed) {
            return;
        }
        installed = true;
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true,
                ScreenEvent.KeyReleased.Pre.class,
                event -> onRelease(event.getKeyCode(), event.getScanCode()));
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, true, InputEvent.Key.class,
                event -> {
                    if (event.getAction() == GLFW.GLFW_RELEASE) {
                        onRelease(event.getKey(), event.getScanCode());
                    }
                });
    }

    /** Whether the terminal key is still held from the press that last opened or closed a screen. */
    public static boolean latched() {
        return LATCH.latched();
    }

    /** The key just opened or closed something: ignore it until it is released. */
    public static void latch() {
        LATCH.latch();
    }

    /** Whether the map key is still held from the press that last opened the map. */
    public static boolean mapLatched() {
        return MAP_LATCH.latched();
    }

    /** The map key just opened a screen: ignore it until it is released. */
    public static void latchMap() {
        MAP_LATCH.latch();
    }

    /** Clears both latches (logout). */
    public static void reset() {
        LATCH.release();
        MAP_LATCH.release();
    }

    /** Per client tick, before the key presses are handled: clears a latch once nothing is held. */
    public static void poll() {
        poll(LATCH, mappings);
        poll(MAP_LATCH, mapMappings);
    }

    private static void poll(TabletKeyLatch latch, Supplier<List<KeyMapping>> source) {
        if (!latch.latched()) {
            return;
        }
        boolean down = false;
        for (KeyMapping mapping : source.get()) {
            if (mapping != null && physicallyDown(mapping)) {
                down = true;
                break;
            }
        }
        latch.poll(down);
    }

    private static void onRelease(int keyCode, int scanCode) {
        release(LATCH, mappings, keyCode, scanCode);
        release(MAP_LATCH, mapMappings, keyCode, scanCode);
    }

    private static void release(TabletKeyLatch latch, Supplier<List<KeyMapping>> source,
                                int keyCode, int scanCode) {
        if (!latch.latched()) {
            return;
        }
        for (KeyMapping mapping : source.get()) {
            if (mapping != null && !mapping.isUnbound() && mapping.matches(keyCode, scanCode)) {
                latch.release();
                return;
            }
        }
    }

    /** Whether the key (or mouse button) of {@code mapping} is held right now. */
    static boolean physicallyDown(KeyMapping mapping) {
        if (mapping.isUnbound()) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return false;
        }
        long window = minecraft.getWindow().getWindow();
        InputConstants.Key key = mapping.getKey();
        return switch (key.getType()) {
            case KEYSYM -> InputConstants.isKeyDown(window, key.getValue());
            case MOUSE -> GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
            // A scancode-bound key cannot be polled; its release events still clear the latch.
            case SCANCODE -> true;
        };
    }
}
