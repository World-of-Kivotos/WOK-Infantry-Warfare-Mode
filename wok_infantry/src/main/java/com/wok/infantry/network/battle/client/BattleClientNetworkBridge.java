package com.wok.infantry.network.battle.client;

import com.wok.infantry.battle.BattleFeedbackMessages;
import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.battle.TacticalMarkerType;
import com.wok.infantry.client.BattleClientActions;
import com.wok.infantry.client.ClientBattleState;
import com.wok.infantry.client.screen.SquadScreen;
import com.wok.infantry.client.screen.TacticalMapScreen;
import com.wok.infantry.network.LoadoutNetwork;
import com.wok.infantry.network.battle.BattleNetwork;
import com.wok.infantry.network.battle.BattleOpenTarget;
import com.wok.infantry.network.battle.packet.c2s.AssignClassPacket;
import com.wok.infantry.network.battle.packet.c2s.BattleOpenPacket;
import com.wok.infantry.network.battle.packet.c2s.BattleSnapshotRequestPacket;
import com.wok.infantry.network.battle.packet.c2s.ClaimCommanderPacket;
import com.wok.infantry.network.battle.packet.c2s.CommanderActionPacket;
import com.wok.infantry.network.battle.packet.c2s.CreateMarkerPacket;
import com.wok.infantry.network.battle.packet.c2s.DeployPacket;
import com.wok.infantry.network.battle.packet.c2s.RedeployPacket;
import com.wok.infantry.network.battle.packet.c2s.RemoveMarkerPacket;
import com.wok.infantry.network.battle.packet.c2s.ResupplyPacket;
import com.wok.infantry.network.battle.packet.c2s.RequestSupportPacket;
import com.wok.infantry.network.battle.packet.c2s.SelectDeploymentPointPacket;
import com.wok.infantry.network.battle.packet.c2s.SquadActionPacket;
import com.wok.infantry.network.serverbound.OpenLoadoutPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/** Installs both directions of the network/UI bridge during client bootstrap. */
public final class BattleClientNetworkBridge {
    private static boolean installed;

    private BattleClientNetworkBridge() {
    }

    public static synchronized void install() {
        if (installed) {
            return;
        }
        BattleClientPacketBridge.install(new IncomingHandler());
        BattleClientActions.install(new OutgoingHandler());
        installed = true;
    }

    public static synchronized void reset() {
        if (!installed) {
            return;
        }
        BattleClientActions.reset();
        BattleClientPacketBridge.reset();
        installed = false;
    }

    public static void openSquadScreen() {
        BattleNetwork.sendToServer(new BattleOpenPacket(BattleOpenTarget.SQUAD));
    }

    public static void openMapScreen() {
        BattleNetwork.sendToServer(new BattleOpenPacket(BattleOpenTarget.MAP));
    }

    public static void openDeploymentScreen() {
        BattleNetwork.sendToServer(new BattleOpenPacket(BattleOpenTarget.DEPLOYMENT));
    }

    private static final class IncomingHandler implements BattleClientPacketBridge.Handler {
        @Override
        public void applySnapshot(BattleSnapshot snapshot, BattleOpenTarget openTarget) {
            BattleSnapshot previousSnapshot = ClientBattleState.snapshot();
            ClientBattleState.update(snapshot);
            Minecraft minecraft = Minecraft.getInstance();
            Screen current = minecraft.screen;
            boolean deploymentCompleted = previousSnapshot != null
                    && previousSnapshot.viewerId().equals(snapshot.viewerId())
                    && previousSnapshot.deployment().phase()
                    != com.wok.infantry.deployment.DeploymentPhase.ACTIVE
                    && snapshot.deployment().phase()
                    == com.wok.infantry.deployment.DeploymentPhase.ACTIVE;
            if (deploymentCompleted && current instanceof SquadScreen
                    && openTarget == BattleOpenTarget.NONE) {
                minecraft.setScreen(null);
                return;
            }
            switch (openTarget) {
                case NONE -> {
                }
                case SQUAD -> {
                    if (!(current instanceof SquadScreen)) {
                        minecraft.setScreen(new SquadScreen(current));
                    }
                }
                case DEPLOYMENT -> minecraft.setScreen(new SquadScreen(current, true));
                case MAP -> {
                    if (!(current instanceof TacticalMapScreen)) {
                        minecraft.setScreen(new TacticalMapScreen(current));
                    }
                }
            }
        }

        @Override
        public void clear() {
            ClientBattleState.clear();
        }

        @Override
        public void feedback(boolean success, String message) {
            ClientBattleState.showFeedback(success,
                    BattleFeedbackMessages.resolve(message).getString());
        }
    }

    private static final class OutgoingHandler implements BattleClientActions.Handler {
        @Override
        public void requestSnapshot() {
            BattleNetwork.sendToServer(new BattleSnapshotRequestPacket());
        }

        @Override
        public void createSquad(SquadCallsign callsign) {
            BattleNetwork.sendToServer(SquadActionPacket.create(callsign));
        }

        @Override
        public void joinSquad(SquadCallsign callsign) {
            BattleNetwork.sendToServer(SquadActionPacket.join(callsign));
        }

        @Override
        public void leaveSquad() {
            BattleNetwork.sendToServer(SquadActionPacket.leave());
        }

        @Override
        public void disbandSquad() {
            BattleNetwork.sendToServer(SquadActionPacket.disband());
        }

        @Override
        public void transferLeadership(UUID playerId) {
            BattleNetwork.sendToServer(SquadActionPacket.promote(playerId));
        }

        @Override
        public void kickMember(UUID playerId) {
            BattleNetwork.sendToServer(SquadActionPacket.kick(playerId));
        }

        @Override
        public void claimCommander() {
            BattleNetwork.sendToServer(new ClaimCommanderPacket());
        }

        @Override
        public void resignCommander() {
            BattleNetwork.sendToServer(CommanderActionPacket.resign());
        }

        @Override
        public void transferCommander(UUID playerId) {
            BattleNetwork.sendToServer(CommanderActionPacket.transfer(playerId));
        }

        @Override
        public void selectClass(String classId) {
            BattleNetwork.sendToServer(new AssignClassPacket(classId));
        }

        @Override
        public void openLoadout() {
            LoadoutNetwork.sendToServer(new OpenLoadoutPacket(false));
        }

        @Override
        public void selectDeploymentPoint(UUID pointId) {
            BattleNetwork.sendToServer(new SelectDeploymentPointPacket(pointId));
        }

        @Override
        public void deploy() {
            BattleNetwork.sendToServer(new DeployPacket());
        }

        @Override
        public void redeploy() {
            BattleNetwork.sendToServer(new RedeployPacket());
        }

        @Override
        public void resupply() {
            BattleNetwork.sendToServer(new ResupplyPacket());
        }

        @Override
        public void createMarker(BattleClientActions.MarkerDraft marker) {
            if (!validCoordinate(marker.startX()) || !validCoordinate(marker.startZ())
                    || !validCoordinate(marker.endX()) || !validCoordinate(marker.endZ())) {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.player != null) {
                    minecraft.player.displayClientMessage(
                            Component.translatable("message.wok_infantry.invalid_marker"), false);
                }
                return;
            }
            TacticalMarkerType type = TacticalMarkerType.valueOf(marker.type().name());
            BattleNetwork.sendToServer(new CreateMarkerPacket(type, marker.dimension(),
                    marker.startX(), marker.startZ(), marker.endX(), marker.endZ(),
                    BattleRules.DEFAULT_MARKER_TTL_MILLIS));
        }

        @Override
        public void removeMarker(UUID markerId) {
            BattleNetwork.sendToServer(new RemoveMarkerPacket(markerId));
        }

        @Override
        public void requestSupport(BattleClientActions.SupportDraft support) {
            if (!validCoordinate(support.startX()) || !validCoordinate(support.startZ())
                    || !validCoordinate(support.endX()) || !validCoordinate(support.endZ())) {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.player != null) {
                    minecraft.player.displayClientMessage(
                            Component.translatable("message.wok_infantry.invalid_support_target"),
                            false);
                }
                return;
            }
            BattleNetwork.sendToServer(new RequestSupportPacket(UUID.randomUUID(),
                    support.supportId(), support.dimension(), support.startX(), support.startZ(),
                    support.endX(), support.endZ()));
        }

        private static boolean validCoordinate(double value) {
            return Double.isFinite(value) && Math.abs(value) <= BattleRules.MAX_COORDINATE;
        }
    }
}
