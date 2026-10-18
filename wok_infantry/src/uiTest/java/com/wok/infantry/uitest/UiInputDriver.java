package com.wok.infantry.uitest;

import com.wok.infantry.client.screen.TacticalScreen;
import com.wok.infantry.client.ui.probe.UiLayoutFrame;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Input for UI acceptance cases. Controls are addressed by their probe uiId: the driver takes the
 * control's rectangle from the latest recorded frame and acts at its centre, so cases never compute
 * screen coordinates. Coordinates are GUI-scaled (what {@code Screen.mouseClicked} receives);
 * a {@code TacticalScreen} converts them to its 2x layout itself.
 *
 * <p>Hover moves the real mouse position that rendering reads ({@link MouseHandler} xpos/ypos, by
 * reflection on the development names), so hover looks come from the production code paths.
 */
public final class UiInputDriver {
    /** GUI position held off the window (nothing hovered). */
    private static final double PARKED = -1000.0D;
    private static Field xpos;
    private static Field ypos;
    private static boolean held;
    private static double heldX;
    private static double heldY;

    private UiInputDriver() {
    }

    /**
     * Keeps the render mouse at GUI (x, y) until {@link #release()}: re-applied every client tick
     * and before every frame ({@link #applyHeld}), because window events (a real mouse over the
     * window, a resize) would otherwise move it between frames.
     */
    public static void hold(Minecraft minecraft, double guiX, double guiY) {
        held = true;
        heldX = guiX;
        heldY = guiY;
        moveMouse(minecraft, guiX, guiY);
    }

    /** Holds the mouse off the window, so no control is hovered in a capture. */
    public static void holdParked(Minecraft minecraft) {
        hold(minecraft, PARKED, PARKED);
    }

    /** Stops holding the mouse. */
    public static void release() {
        held = false;
    }

    /**
     * Stops holding the mouse and puts it back on the window centre, where Minecraft keeps a
     * grabbed cursor. Use it without a screen (HUD captures): a cursor left parked off the window
     * would turn the camera by the whole distance on the next cursor event.
     */
    public static void releaseToCentre(Minecraft minecraft) {
        held = false;
        Window window = minecraft.getWindow();
        setMouse(minecraft, window.getScreenWidth() / 2.0D, window.getScreenHeight() / 2.0D);
    }

    /** Re-applies the held mouse position (no-op when nothing is held). */
    public static void applyHeld(Minecraft minecraft) {
        if (held && minecraft != null && minecraft.mouseHandler != null) {
            moveMouse(minecraft, heldX, heldY);
        }
    }

    /**
     * Clicks (press and release, left button) at the centre of {@code control}, then parks the
     * held mouse so the screen that follows shows no hover.
     */
    public static void click(Minecraft minecraft, UiLayoutFrame.Control control) {
        Screen screen = minecraft.screen;
        if (screen == null || control == null) {
            throw new IllegalStateException("No screen or control to click");
        }
        UiLayoutFrame.Rect rect = control.rect();
        double x = rect.centerX();
        double y = rect.centerY();
        moveMouse(minecraft, x, y);
        screen.mouseClicked(x, y, GLFW.GLFW_MOUSE_BUTTON_LEFT);
        screen.mouseReleased(x, y, GLFW.GLFW_MOUSE_BUTTON_LEFT);
        if (held) {
            holdParked(minecraft);
        }
    }

    /**
     * Clicks the centre of a widget found without a probe frame (legacy screens). Widget
     * coordinates of a {@code TacticalScreen} are layout coordinates and are scaled back to GUI.
     */
    public static void clickWidget(Minecraft minecraft, AbstractWidget widget) {
        Screen screen = minecraft.screen;
        if (screen == null || widget == null) {
            throw new IllegalStateException("No screen or widget to click");
        }
        int factor = screen instanceof TacticalScreen tactical ? tactical.uiScale() : 1;
        double x = (widget.getX() + widget.getWidth() / 2.0D) * factor;
        double y = (widget.getY() + widget.getHeight() / 2.0D) * factor;
        moveMouse(minecraft, x, y);
        screen.mouseClicked(x, y, GLFW.GLFW_MOUSE_BUTTON_LEFT);
        screen.mouseReleased(x, y, GLFW.GLFW_MOUSE_BUTTON_LEFT);
        if (held) {
            holdParked(minecraft);
        }
    }

    /**
     * Clicks the screen at layout coordinates (a {@code TacticalScreen}'s own coordinates, e.g. a
     * list row's bounds), scaled back to GUI coordinates like {@link #clickWidget}.
     */
    public static void clickLayout(Minecraft minecraft, double layoutX, double layoutY) {
        Screen screen = minecraft.screen;
        if (screen == null) {
            throw new IllegalStateException("No screen to click");
        }
        int factor = screen instanceof TacticalScreen tactical ? tactical.uiScale() : 1;
        double x = layoutX * factor;
        double y = layoutY * factor;
        moveMouse(minecraft, x, y);
        screen.mouseClicked(x, y, GLFW.GLFW_MOUSE_BUTTON_LEFT);
        screen.mouseReleased(x, y, GLFW.GLFW_MOUSE_BUTTON_LEFT);
        if (held) {
            holdParked(minecraft);
        }
    }

    /** Scrolls the wheel at GUI coordinates (x, y). */
    public static boolean scrollAt(Minecraft minecraft, double guiX, double guiY, double delta) {
        Screen screen = minecraft.screen;
        if (screen == null) {
            return false;
        }
        moveMouse(minecraft, guiX, guiY);
        boolean handled = screen.mouseScrolled(guiX, guiY, delta);
        if (held) {
            holdParked(minecraft);
        }
        return handled;
    }

    /** Holds the mouse on the centre of {@code control} (hover, tooltips). */
    public static void hover(Minecraft minecraft, UiLayoutFrame.Control control) {
        UiLayoutFrame.Rect rect = control.rect();
        hold(minecraft, rect.centerX(), rect.centerY());
    }

    /** Sends a key press and release to the open screen. */
    public static boolean key(Minecraft minecraft, int keyCode, int modifiers) {
        Screen screen = minecraft.screen;
        if (screen == null) {
            return false;
        }
        boolean handled = screen.keyPressed(keyCode, 0, modifiers);
        if (minecraft.screen == screen) {
            screen.keyReleased(keyCode, 0, modifiers);
        }
        return handled;
    }

    /** Scrolls the wheel over the centre of {@code control}. */
    public static boolean scroll(Minecraft minecraft, UiLayoutFrame.Control control, double delta) {
        Screen screen = minecraft.screen;
        if (screen == null) {
            return false;
        }
        UiLayoutFrame.Rect rect = control.rect();
        moveMouse(minecraft, rect.centerX(), rect.centerY());
        return screen.mouseScrolled(rect.centerX(), rect.centerY(), delta);
    }

    /** Puts the render mouse position on GUI coordinates (x, y). */
    public static void moveMouse(Minecraft minecraft, double guiX, double guiY) {
        Window window = minecraft.getWindow();
        double toScreenX = (double) window.getScreenWidth() / Math.max(1, window.getGuiScaledWidth());
        double toScreenY = (double) window.getScreenHeight()
                / Math.max(1, window.getGuiScaledHeight());
        setMouse(minecraft, guiX * toScreenX, guiY * toScreenY);
    }

    private static void setMouse(Minecraft minecraft, double screenX, double screenY) {
        try {
            if (xpos == null) {
                xpos = MouseHandler.class.getDeclaredField("xpos");
                xpos.setAccessible(true);
                ypos = MouseHandler.class.getDeclaredField("ypos");
                ypos.setAccessible(true);
            }
            xpos.setDouble(minecraft.mouseHandler, screenX);
            ypos.setDouble(minecraft.mouseHandler, screenY);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot move the development mouse: " + exception,
                    exception);
        }
    }

    // ---- key mappings -----------------------------------------------------------------------------

    /** Outcome of {@link #clickMappedKey}: the mapping used and its conflicts, or a failure. */
    public record KeyPress(KeyMapping mapping, List<String> conflicts, String failure) {
        public boolean ok() {
            return failure == null;
        }

        /** Observation line such as {@code key.wok_infantry.open_terminal@key.keyboard.grave.accent conflicts=none}. */
        public String describe() {
            return mapping.getName() + "@" + mapping.getKey().getName() + " conflicts="
                    + (conflicts.isEmpty() ? "none" : String.join(",", conflicts));
        }
    }

    /**
     * Presses the key a WOK mapping is currently bound to, found by its stable mapping name.
     *
     * <p>With {@code terminalFallback}, a mapping that has no key is replaced by the first bound
     * WOK battle-terminal mapping ({@code key.wok_infantry.*terminal*}): the old squad mapping stays
     * registered but unbound by default, and a player who rebound it still reaches the page.
     *
     * <p>The click goes through Forge's key lookup, so a conflicting binding that would swallow the
     * key in a real client also makes the run fail; the result lists every conflict.
     */
    public static KeyPress clickMappedKey(Minecraft minecraft, String mappingName,
                                          boolean terminalFallback) {
        KeyMapping mapping = findKeyMapping(minecraft, mappingName);
        if (terminalFallback && (mapping == null || mapping.isUnbound())) {
            for (KeyMapping candidate : minecraft.options.keyMappings) {
                if (candidate.getName().startsWith("key.wok_infantry.")
                        && candidate.getName().contains("terminal") && !candidate.isUnbound()) {
                    mapping = candidate;
                    break;
                }
            }
        }
        if (mapping == null) {
            return new KeyPress(null, List.of(), "Key mapping " + mappingName
                    + " is not registered");
        }
        if (mapping.isUnbound()) {
            // prepareUiTestOptions strips every key_key.wok_infantry.* line before each run, so
            // only the mod's default binding counts here.
            return new KeyPress(mapping, List.of(), "Key mapping " + mappingName
                    + " has no default key"
                    + (terminalFallback ? " and no bound key.wok_infantry.*terminal* mapping" : "")
                    + "; the acceptance run resets WOK keys to their defaults");
        }
        if (mapping.getKeyModifier() != KeyModifier.NONE) {
            return new KeyPress(mapping, List.of(), "Key mapping " + mapping.getName()
                    + " needs the " + mapping.getKeyModifier()
                    + " modifier, which the harness cannot hold down");
        }
        List<String> conflicts = new ArrayList<>();
        for (KeyMapping other : minecraft.options.keyMappings) {
            if (other != mapping && !other.isUnbound() && mapping.same(other)) {
                conflicts.add(other.getName());
            }
        }
        KeyMapping.click(mapping.getKey());
        return new KeyPress(mapping, List.copyOf(conflicts), null);
    }

    public static KeyMapping findKeyMapping(Minecraft minecraft, String mappingName) {
        for (KeyMapping candidate : minecraft.options.keyMappings) {
            if (candidate.getName().equals(mappingName)) {
                return candidate;
            }
        }
        return null;
    }
}
