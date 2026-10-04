package com.wok.infantry.integration.xaero;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.client.ClientBattleState;
import com.wok.infantry.client.KeyBindingDefaults;
import com.wok.infantry.client.screen.BattleTab;
import com.wok.infantry.client.screen.BattleTerminalNav;
import com.wok.infantry.client.screen.TacticalMapScreen;
import com.wok.infantry.config.InfantryClientConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;

/**
 * Sends Xaero's World Map ({@code xaeroworldmap}) to the WOK tactical map while the player is in
 * a battle, the way {@code JourneyMapUiPolicy} does for JourneyMap's fullscreen map.
 *
 * <p>Xaero is an optional soft dependency: its map screen and its open-map key are recognised by
 * class name and mapping name only, never through a compile-time reference. If the expected
 * screen class is missing (another Xaero build), the redirect stays off, one warning is logged
 * and Xaero keeps its own behaviour.</p>
 *
 * <ul>
 *   <li>In a battle (the cached battle snapshot has a faction, see
 *   {@link KeyBindingDefaults#inBattle}) opening
 *   {@code xaero.map.gui.GuiMap} opens the tactical map instead. The map instance and its return
 *   screen come from {@link BattleTerminalNav}, so the view is kept between openings and Esc
 *   goes back to the game rather than into a Xaero screen.</li>
 *   <li>Outside a battle, or with {@code tacticalMap.redirectXaeroWorldMap = false}, Xaero's map
 *   opens as usual.</li>
 *   <li>When the WOK tactical-map key and Xaero's open-map key are the same key, WOK leaves the
 *   press to Xaero ({@link #handlesSamePress}): the redirect turns it into the tactical map in a
 *   battle, so the Xaero map never flashes up first and Esc never returns to it.</li>
 * </ul>
 * Client thread only.
 */
public final class XaeroWorldMapPolicy {
    public static final String MOD_ID = "xaeroworldmap";
    /** Xaero's World Map screen (checked by name against the class and its superclasses). */
    static final String WORLD_MAP_SCREEN_CLASS = "xaero.map.gui.GuiMap";
    /** Package of every Xaero World Map screen; never kept as the tactical map's return screen. */
    static final String WORLD_MAP_PACKAGE = "xaero.map.";
    /** Xaero's open-map {@code KeyMapping} name (default M). */
    public static final String OPEN_MAP_KEY_NAME = "gui.xaero_open_map";

    /** What a screen opening becomes. */
    enum Decision {
        /** Not Xaero's map, redirect off, or not in a battle: leave the opening alone. */
        KEEP,
        /** Replace Xaero's map with the tactical map. */
        REDIRECT,
        /** The tactical map is already open: keep it instead of opening a second map. */
        CANCEL
    }

    private static volatile boolean installed;
    private static KeyMapping openMapKey;
    /** The key-mapping array {@link #openMapKey} was looked up in (Forge replaces it on load). */
    private static KeyMapping[] openMapKeyScanned;

    private XaeroWorldMapPolicy() {
    }

    /** Whether Xaero's World Map is installed (whether or not the redirect could be enabled). */
    public static boolean isModLoaded() {
        ModList mods = ModList.get();
        return mods != null && mods.isLoaded(MOD_ID);
    }

    /** Hooks the redirect when Xaero's World Map is installed; safe to call more than once. */
    public static synchronized void install() {
        if (installed || !isModLoaded()) {
            return;
        }
        try {
            Class.forName(WORLD_MAP_SCREEN_CLASS, false, XaeroWorldMapPolicy.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError exception) {
            WokInfantryMod.LOGGER.warn("Xaero's World Map is installed but {} was not found; its "
                    + "world map is not redirected to the WOK tactical map", WORLD_MAP_SCREEN_CLASS,
                    exception);
            return;
        }
        installed = true;
        MinecraftForge.EVENT_BUS.addListener(XaeroWorldMapPolicy::onScreenOpening);
        WokInfantryMod.LOGGER.info("Xaero's World Map opens the WOK tactical map while in a battle "
                + "(client config tacticalMap.redirectXaeroWorldMap)");
    }

    /** Whether Xaero's map is currently redirected (installed, hooked and enabled in config). */
    public static boolean redirectActive() {
        return installed && InfantryClientConfig.redirectXaeroWorldMap();
    }

    public static void onScreenOpening(ScreenEvent.Opening event) {
        Screen requested = event.getNewScreen();
        if (requested == null || !isWorldMapScreen(requested.getClass())) {
            return;
        }
        Screen current = event.getCurrentScreen();
        switch (decide(true, InfantryClientConfig.redirectXaeroWorldMap(),
                KeyBindingDefaults.inBattle(ClientBattleState.snapshot()),
                current instanceof TacticalMapScreen)) {
            case KEEP -> {
            }
            case CANCEL -> event.setCanceled(true);
            case REDIRECT -> {
                Screen root = returnScreen(current);
                event.setNewScreen(BattleTerminalNav.reuse(BattleTab.MAP, root,
                        TacticalMapScreen.class, TacticalMapScreen::new));
            }
        }
    }

    /**
     * Xaero's open-map key, looked up by name among the game's key mappings (again only when the
     * mapping array changes), or {@code null} when Xaero's World Map (or that mapping) is not
     * present. Client thread only.
     */
    public static KeyMapping openMapKey() {
        if (!installed) {
            return null;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null) {
            return null;
        }
        KeyMapping[] mappings = minecraft.options.keyMappings;
        if (mappings != openMapKeyScanned) {
            openMapKey = findByName(mappings, OPEN_MAP_KEY_NAME);
            openMapKeyScanned = mappings;
        }
        return openMapKey;
    }

    /**
     * Whether a press of {@code wokMapKey} also fires Xaero's open-map key while the redirect is
     * active: same key and same modifier. Then WOK must not open its own map for that press.
     */
    public static boolean handlesSamePress(KeyMapping wokMapKey) {
        if (wokMapKey == null || wokMapKey.isUnbound() || !redirectActive()) {
            return false;
        }
        KeyMapping xaero = openMapKey();
        return xaero != null && !xaero.isUnbound()
                && xaero.getKey().equals(wokMapKey.getKey())
                && xaero.getKeyModifier() == wokMapKey.getKeyModifier();
    }

    static KeyMapping findByName(KeyMapping[] mappings, String name) {
        if (mappings == null) {
            return null;
        }
        for (KeyMapping mapping : mappings) {
            if (mapping != null && name.equals(mapping.getName())) {
                return mapping;
            }
        }
        return null;
    }

    /** Pure redirect rule, see the class comment. */
    static Decision decide(boolean worldMapScreen, boolean redirectEnabled, boolean inBattle,
                           boolean tacticalMapOpen) {
        if (!worldMapScreen || !redirectEnabled || !inBattle) {
            return Decision.KEEP;
        }
        return tacticalMapOpen ? Decision.CANCEL : Decision.REDIRECT;
    }

    /** Whether {@code type} is Xaero's world map screen or a subclass of it (by class name). */
    static boolean isWorldMapScreen(Class<?> type) {
        for (Class<?> candidate = type; candidate != null; candidate = candidate.getSuperclass()) {
            if (isWorldMapScreenName(candidate.getName())) {
                return true;
            }
        }
        return false;
    }

    static boolean isWorldMapScreenName(String className) {
        return WORLD_MAP_SCREEN_CLASS.equals(className);
    }

    /**
     * Whether a screen may be the tactical map's return screen: anything except a Xaero World
     * Map screen, whose Esc chain could lead back into Xaero's map.
     */
    static boolean keepAsReturnScreen(String currentClassName) {
        return currentClassName == null || !currentClassName.startsWith(WORLD_MAP_PACKAGE);
    }

    private static Screen returnScreen(Screen current) {
        if (current != null && !keepAsReturnScreen(current.getClass().getName())) {
            return null;
        }
        return BattleTerminalNav.returnScreenFor(current);
    }
}
