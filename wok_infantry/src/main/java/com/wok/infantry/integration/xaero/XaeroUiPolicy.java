package com.wok.infantry.integration.xaero;

import com.wok.infantry.WokInfantryMod;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Keeps Xaero's native minimap hidden while retaining its mapping/session services. */
final class XaeroUiPolicy {
    private static volatile ReflectionBridge bridge;
    private static volatile boolean reflectionUnavailable;
    private static volatile boolean hidden;

    private XaeroUiPolicy() {
    }

    static void install() {
        enforceMinimapHidden();
    }

    static void enforceMinimapHidden() {
        if (reflectionUnavailable) {
            return;
        }
        try {
            ReflectionBridge current = bridge;
            if (current == null) {
                current = ReflectionBridge.load();
                bridge = current;
            }
            hidden = current.hideMinimapIfReady();
        } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException
                 | IllegalAccessException exception) {
            reflectionUnavailable = true;
            WokInfantryMod.LOGGER.warn(
                    "Xaero minimap UI could not be suppressed; tactical terrain remains available",
                    exception);
        } catch (InvocationTargetException exception) {
            // Xaero may still be constructing its settings/session during early client ticks.
            hidden = false;
            Throwable cause = exception.getCause();
            if (cause instanceof LinkageError) {
                reflectionUnavailable = true;
                WokInfantryMod.LOGGER.warn(
                        "Xaero minimap UI bridge is incompatible with this Xaero build", cause);
            }
        } catch (LinkageError error) {
            reflectionUnavailable = true;
            WokInfantryMod.LOGGER.warn(
                    "Xaero minimap UI bridge is incompatible with this Xaero build", error);
        }
    }

    static boolean nativeMinimapHidden() {
        return hidden;
    }

    private record ReflectionBridge(Field instanceField, Method getSettings,
                                    Field minimapOptionField, Method getBooleanValue,
                                    Method setOptionValue) {
        private static ReflectionBridge load() throws ClassNotFoundException,
                NoSuchFieldException, NoSuchMethodException {
            ClassLoader loader = XaeroUiPolicy.class.getClassLoader();
            Class<?> hudMod = Class.forName("xaero.common.HudMod", false, loader);
            Class<?> settings = Class.forName("xaero.common.settings.ModSettings", false, loader);
            Class<?> options = Class.forName("xaero.common.settings.ModOptions", false, loader);
            return new ReflectionBridge(
                    hudMod.getField("INSTANCE"),
                    hudMod.getMethod("getSettings"),
                    options.getField("MINIMAP"),
                    settings.getMethod("getBooleanValue", options),
                    settings.getMethod("setOptionValue", options, Object.class));
        }

        private boolean hideMinimapIfReady()
                throws IllegalAccessException, InvocationTargetException {
            Object instance = instanceField.get(null);
            Object minimapOption = minimapOptionField.get(null);
            if (instance == null || minimapOption == null) {
                return false;
            }
            Object settings = getSettings.invoke(instance);
            if (settings == null) {
                return false;
            }
            if (Boolean.TRUE.equals(getBooleanValue.invoke(settings, minimapOption))) {
                // This changes only the live HUD-module state. WOK does not rewrite Xaero's
                // config file, so temporarily removing WOK restores the user's own preference.
                setOptionValue.invoke(settings, minimapOption, Boolean.FALSE);
            }
            return !Boolean.TRUE.equals(getBooleanValue.invoke(settings, minimapOption));
        }
    }
}
