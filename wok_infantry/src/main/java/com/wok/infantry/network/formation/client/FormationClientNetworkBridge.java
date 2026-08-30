package com.wok.infantry.network.formation.client;

import com.wok.infantry.client.ClientFormationState;
import com.wok.infantry.client.screen.FormationSelectionScreen;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.network.formation.FormationNetwork;
import com.wok.infantry.network.formation.packet.c2s.RequestFormationCatalogPacket;
import com.wok.infantry.network.formation.packet.c2s.SelectFormationPacket;
import com.wok.infantry.network.formation.packet.c2s.CastFormationVotePacket;
import com.wok.infantry.network.formation.packet.c2s.SelectFactionPacket;
import com.wok.infantry.formation.vote.FormationVotePhase;
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
        installed = true;
    }

    public static void requestCatalog() {
        FormationNetwork.sendToServer(new RequestFormationCatalogPacket());
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

    private static final class IncomingHandler implements FormationClientPacketBridge.Handler {
        @Override
        public void apply(FormationSelectionSnapshot snapshot, boolean openScreen) {
            ClientFormationState.update(snapshot);
            Minecraft minecraft = Minecraft.getInstance();
            Screen current = minecraft.screen;
            if (current instanceof FormationSelectionScreen selectionScreen) {
                if (!snapshot.selectionRequired()
                        && snapshot.votePhase() != FormationVotePhase.OPEN) {
                    minecraft.setScreen(selectionScreen.returnScreen());
                } else {
                    selectionScreen.replaceSnapshot(snapshot);
                }
                return;
            }
            if (openScreen && (snapshot.selectionRequired()
                    || snapshot.votePhase() == FormationVotePhase.OPEN)) {
                minecraft.setScreen(new FormationSelectionScreen(snapshot, current));
            }
        }

        @Override
        public void feedback(boolean success, String message) {
            ClientFormationState.feedback(success, message);
        }
    }
}
