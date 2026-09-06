package com.wok.downed.state;

import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

public final class DownedPose {
    public static void lock(Player player) {
        // Discard pre-existing crawl/slide state, including requests already in flight when hit.
        // These unnamespaced tags belong to Moves Like Mafuyu; leave them alone without that mod.
        if (ModList.get().isLoaded("moveslikemafuyu")) {
            player.removeTag("craw");
            player.removeTag("slide");
        }
        player.setSprinting(false);
        player.setJumping(false);
        player.setForcedPose(Pose.SWIMMING);
        // Apply immediately as well as on the next updatePlayerPose, for both tracking and camera.
        player.setPose(Pose.SWIMMING);
    }

    private DownedPose() {
    }
}
