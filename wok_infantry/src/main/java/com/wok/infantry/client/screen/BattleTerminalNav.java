package com.wok.infantry.client.screen;

import com.wok.infantry.client.BattleClientActions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Navigation of the battle terminal (squads, classes, deployment, loadout, map, formation).
 *
 * <ul>
 *   <li><b>Root return screen.</b> Every terminal screen implements {@link Terminal} and keeps the
 *   screen that was open before the terminal (usually {@code null}, the game).
 *   {@link #returnScreenFor} gives that root for any screen, so a single Esc closes the whole
 *   terminal no matter how many tabs were visited.</li>
 *   <li><b>Replace semantics.</b> {@link #navigate} opens the target tab with the same root and
 *   replaces the current screen; terminal screens never become each other's parent.</li>
 *   <li><b>Reuse.</b> {@link #reuse} hands out the same instance for a tab within one session
 *   (same root), so the tactical map keeps its view when the player comes back to it. The cache is
 *   cleared on logout.</li>
 * </ul>
 * Each tab is opened by an {@link Opener}. Defaults exist for the squad, class and deployment
 * pages (one {@link SquadScreen}, switched in place when it is already open), the loadout and the
 * map; the formation page registers its own with {@link #registerOpener}. Client thread only.
 */
public final class BattleTerminalNav {
    /** Implemented by every battle-terminal screen. */
    public interface Terminal {
        /** Screen the whole terminal returns to; {@code null} returns to the game. */
        Screen terminalReturnScreen();
    }

    /** Opens one tab; {@code root} is the session's return screen, {@code from} the current screen. */
    @FunctionalInterface
    public interface Opener {
        /** @return whether the tab was (or is being) opened */
        boolean open(Screen root, Screen from);
    }

    private static final TerminalNavModel<Screen> MODEL = new TerminalNavModel<>();
    private static final Map<BattleTab, Opener> OPENERS = new EnumMap<>(BattleTab.class);

    static {
        registerOpener(BattleTab.SQUADS, (root, from) -> openSquadPage(root, from,
                BattleTab.SQUADS));
        registerOpener(BattleTab.CLASSES, (root, from) -> openSquadPage(root, from,
                BattleTab.CLASSES));
        registerOpener(BattleTab.DEPLOYMENT, (root, from) -> openSquadPage(root, from,
                BattleTab.DEPLOYMENT));
        registerOpener(BattleTab.MAP, (root, from) -> {
            show(reuse(BattleTab.MAP, root, TacticalMapScreen.class, TacticalMapScreen::new));
            return true;
        });
        registerOpener(BattleTab.LOADOUT, (root, from) -> {
            // The server answers with the loadout snapshot; ClientPacketHandler opens the screen.
            BattleClientActions.openLoadout();
            return true;
        });
    }

    private BattleTerminalNav() {
    }

    /** Root a terminal screen opened from {@code current} must return to. */
    public static Screen returnScreenFor(Screen current) {
        return MODEL.returnScreenFor(current, screen -> screen instanceof Terminal,
                screen -> ((Terminal) screen).terminalReturnScreen());
    }

    /**
     * The squad, class or deployment page: switched in place when {@code from} is already the
     * squad screen, otherwise a new squad screen for {@code root}.
     */
    private static boolean openSquadPage(Screen root, Screen from, BattleTab tab) {
        if (from instanceof SquadScreen squadScreen) {
            squadScreen.showPage(tab);
            if (Minecraft.getInstance().screen != squadScreen) {
                show(squadScreen);
            }
            return true;
        }
        show(SquadScreen.forTab(root, tab));
        return true;
    }

    /** Replaces (or removes, with {@code null}) the opener of {@code tab}. */
    public static void registerOpener(BattleTab tab, Opener opener) {
        Objects.requireNonNull(tab, "tab");
        if (opener == null) {
            OPENERS.remove(tab);
        } else {
            OPENERS.put(tab, opener);
        }
    }

    /**
     * Switches from {@code from} to {@code tab} with replace semantics. Returns {@code false}
     * when no opener is registered for the tab (the caller may handle it in place).
     */
    public static boolean navigate(Screen from, BattleTab tab) {
        Opener opener = tab == null ? null : OPENERS.get(tab);
        return opener != null && opener.open(returnScreenFor(from), from);
    }

    /**
     * The instance of {@code tab} for the session of {@code root}, created with
     * {@code factory.apply(root)} on first use or when the cached one belongs to another root.
     */
    public static <T extends Screen> T reuse(BattleTab tab, Screen root, Class<T> type,
                                             Function<Screen, T> factory) {
        Screen screen = MODEL.reuse(tab, root, () -> factory.apply(root));
        if (type.isInstance(screen)) {
            return type.cast(screen);
        }
        MODEL.forget(tab);
        T created = factory.apply(root);
        MODEL.reuse(tab, root, () -> created);
        return created;
    }

    /** Shows {@code target}, replacing the current screen. */
    public static void show(Screen target) {
        Minecraft.getInstance().setScreen(target);
    }

    /** Closes the whole terminal: back to the root return screen of {@code from}. */
    public static void close(Screen from) {
        show(returnScreenFor(from));
    }

    /** Drops every reusable instance (logout). */
    public static void reset() {
        MODEL.clear();
    }

    /** Clears the reuse cache on logout so no screen of the old world is kept. */
    @Mod.EventBusSubscriber(modid = "wok_infantry", value = Dist.CLIENT)
    public static final class Logout {
        private Logout() {
        }

        @SubscribeEvent
        public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            reset();
        }
    }
}
