package com.wok.infantry.uitest;

import com.wok.infantry.client.ui.probe.UiLayoutProbe;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.Optional;

/**
 * Finds widgets of a screen without depending on the display language or on coordinates.
 *
 * <p>A widget matches, in this order: its probe tag ({@link UiLayoutProbe#tag}) equals the uiId;
 * its label is one of the given translation keys; its label text equals the current translation of
 * one of those keys; its label text equals one of the given literal texts. Labels are given as
 * {@code key:<translation key>} or as plain text. Screens that still use literal Chinese labels
 * keep working, and once a batch moves them to language keys or tags them, the same lookup keeps
 * finding them in every language.
 */
public final class UiFind {
    private UiFind() {
    }

    /**
     * First visible widget of {@code screen} that matches {@code uiId} or one of {@code labels}.
     */
    public static Optional<AbstractWidget> widget(Screen screen, String uiId, String... labels) {
        if (screen == null) {
            return Optional.empty();
        }
        if (uiId != null) {
            for (GuiEventListener child : screen.children()) {
                if (child instanceof AbstractWidget widget && widget.visible
                        && uiId.equals(UiLayoutProbe.tagOf(widget))) {
                    return Optional.of(widget);
                }
            }
        }
        for (String label : labels) {
            for (GuiEventListener child : screen.children()) {
                if (child instanceof AbstractWidget widget && widget.visible
                        && matches(widget.getMessage(), label)) {
                    return Optional.of(widget);
                }
            }
        }
        return Optional.empty();
    }

    /** Whether {@code message} is the label described by {@code label}. */
    static boolean matches(Component message, String label) {
        if (message == null || label == null || label.isEmpty()) {
            return false;
        }
        if (label.startsWith("key:")) {
            String key = label.substring("key:".length());
            if (message.getContents() instanceof TranslatableContents translatable
                    && translatable.getKey().equals(key)) {
                return true;
            }
            Language language = Language.getInstance();
            return language.has(key) && language.getOrDefault(key).equals(message.getString());
        }
        return label.equals(message.getString());
    }

    /** Short description of what was searched, for failure messages. */
    public static String describe(String uiId, String... labels) {
        return "uiId=" + uiId + " labels=" + String.join("|", labels);
    }
}
