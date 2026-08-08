package com.wok.infantry.client;

import com.wok.infantry.client.screen.AdminLoadoutScreen;
import com.wok.infantry.client.screen.PlayerLoadoutScreen;
import com.wok.infantry.loadout.LoadoutSnapshot;
import com.wok.infantry.network.clientbound.LoadoutSnapshotPacket;
import net.minecraft.client.Minecraft;

public final class ClientPacketHandler {
    private ClientPacketHandler() {
    }

    public static void handleSnapshot(LoadoutSnapshot snapshot,
                                      LoadoutSnapshotPacket.OpenTarget target) {
        if (snapshot == null || snapshot.config() == null || snapshot.player() == null) {
            return;
        }
        ClientLoadoutState.update(snapshot);
        Minecraft minecraft = Minecraft.getInstance();
        switch (target) {
            case PLAYER -> minecraft.setScreen(new PlayerLoadoutScreen(snapshot));
            case ADMIN -> {
                if (snapshot.administrator()) {
                    minecraft.setScreen(new AdminLoadoutScreen(snapshot));
                }
            }
            case REFRESH_ADMIN -> {
                if (snapshot.administrator()
                        && minecraft.screen instanceof AdminLoadoutScreen adminScreen) {
                    adminScreen.replaceSnapshot(snapshot);
                }
            }
            case NONE -> {
            }
        }
    }
}
