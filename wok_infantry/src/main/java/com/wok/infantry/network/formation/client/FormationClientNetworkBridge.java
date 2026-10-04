package com.wok.infantry.network.formation.client;

import com.wok.infantry.client.ClientBattleState;
import com.wok.infantry.client.ClientFormationState;
import com.wok.infantry.client.screen.BattleTab;
import com.wok.infantry.client.screen.BattleTerminalNav;
import com.wok.infantry.client.screen.FormationSelectionScreen;
import com.wok.infantry.client.screen.FormationText;
import com.wok.infantry.client.screen.FormationVoteModel;
import com.wok.infantry.client.screen.SquadScreen;
import com.wok.infantry.client.screen.TacticalMapScreen;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.network.formation.FormationNetwork;
import com.wok.infantry.network.formation.packet.c2s.RequestFormationCatalogPacket;
import com.wok.infantry.network.formation.packet.c2s.SelectFormationPacket;
import com.wok.infantry.network.formation.packet.c2s.CastFormationVotePacket;
import com.wok.infantry.network.formation.packet.c2s.SelectFactionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Installs the client side of the public faction/formation protocol. */
public final class FormationClientNetworkBridge {
    private static boolean installed;

    private FormationClientNetworkBridge() {
    }

    public static synchronized void install() {
        if (installed) {
            return;
        }
        FormationClientPacketBridge.install(new IncomingHandler());
        // The battle terminal's "编制" tab (player-08: the vote page can always be reopened).
        BattleTerminalNav.registerOpener(BattleTab.FORMATION, (root, from) -> {
            openFromTerminal(root);
            return true;
        });
        installed = true;
    }

    public static void requestCatalog() {
        FormationNetwork.sendToServer(new RequestFormationCatalogPacket());
    }

    /**
     * Opens the vote page from the terminal (its "编制" tab): at once with the cached catalog,
     * or in the waiting state until the server answers; the catalog is requested either way.
     * The page stays open after the lock to show the result.
     */
    public static void openFromTerminal(Screen returnScreen) {
        open(returnScreen, FormationSelectionScreen.Entry.TERMINAL);
    }

    /**
     * Opens the vote page for the terminal key while the player has no formation. When the
     * server answers that nothing needs to be chosen any more (the cached catalog was stale),
     * the page gives way to the squad page instead of staying open.
     */
    public static void openFromTerminalKey(Screen returnScreen) {
        open(returnScreen, FormationSelectionScreen.Entry.KEY);
    }

    private static void open(Screen returnScreen, FormationSelectionScreen.Entry entry) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof FormationSelectionScreen) {
            return;
        }
        minecraft.setScreen(new FormationSelectionScreen(ClientFormationState.snapshot(),
                returnScreen, entry));
        requestCatalog();
    }

    public static void select(long generation, String factionId, String formationId) {
        FormationNetwork.sendToServer(new SelectFormationPacket(generation, factionId,
                formationId));
    }

    public static void vote(long generation, String formationId) {
        FormationNetwork.sendToServer(new CastFormationVotePacket(generation, formationId));
    }

    public static void selectFaction(long generation, String factionId) {
        FormationNetwork.sendToServer(new SelectFactionPacket(generation, factionId));
    }

    /** No screen, or a battle-terminal screen without drafts, may give way to the deployment page. */
    static boolean replaceable(Screen screen) {
        return screen == null || screen instanceof FormationSelectionScreen
                || screen instanceof SquadScreen || screen instanceof TacticalMapScreen
                || screen instanceof BattleTerminalNav.Terminal;
    }

    private static final class IncomingHandler implements FormationClientPacketBridge.Handler {
        @Override
        public void apply(FormationSelectionSnapshot snapshot, boolean openScreen) {
            apply(snapshot, openScreen, false);
        }

        @Override
        public void apply(FormationSelectionSnapshot snapshot, boolean openScreen,
                          boolean lockNotice) {
            ClientFormationState.update(snapshot);
            Minecraft minecraft = Minecraft.getInstance();
            Screen current = minecraft.screen;
            FormationSelectionScreen selectionScreen =
                    current instanceof FormationSelectionScreen screen ? screen : null;
            switch (FormationVoteModel.arrival(snapshot, openScreen, lockNotice,
                    selectionScreen == null ? null : selectionScreen.entry(),
                    replaceable(current))) {
                case DEPLOYMENT -> {
                    recordLock(snapshot);
                    // Replaces the terminal: the vote page is never the deployment page's parent.
                    minecraft.setScreen(new SquadScreen(BattleTerminalNav.returnScreenFor(current),
                            true));
                }
                case NOTICE_ONLY -> recordLock(snapshot);
                case CLOSE -> minecraft.setScreen(selectionScreen.returnScreen());
                case SQUADS -> minecraft.setScreen(new SquadScreen(selectionScreen.returnScreen()));
                case REPLACE -> selectionScreen.replaceSnapshot(snapshot);
                case OPEN -> minecraft.setScreen(new FormationSelectionScreen(snapshot, current));
                case IGNORE -> {
                }
            }
        }

        /** HUD lock notice (read through lockTransition) and one deployment-page receipt. */
        private static void recordLock(FormationSelectionSnapshot snapshot) {
            String name = FormationVoteModel.of(snapshot, "", "", false)
                    .formationName(snapshot.selectedFormationId());
            ClientFormationState.recordLock(snapshot.selectedFactionId(),
                    snapshot.selectedFormationId(), name);
            ClientBattleState.showFeedback(true, FormationText.lockedNotice(name));
        }

        @Override
        public void feedback(boolean success, String message) {
            ClientFormationState.feedback(success, message);
        }
    }
}
