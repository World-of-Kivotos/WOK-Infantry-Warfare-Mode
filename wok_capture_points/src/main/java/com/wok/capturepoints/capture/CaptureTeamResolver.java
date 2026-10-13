package com.wok.capturepoints.capture;

import com.wok.capturepoints.config.CaptureConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Team;
import net.minecraftforge.fml.ModList;

public final class CaptureTeamResolver {
    private CaptureTeamResolver() {
    }

    public static CaptureTeam resolve(ServerPlayer player) {
        // When the battle core is present it is the sole authority. An unassigned WOK player
        // must not acquire a capture side from an unrelated scoreboard team or entity tag.
        if (ModList.get().isLoaded("wok_infantry")) {
            return InfantryFactionBridge.resolve(player);
        }

        Team scoreboardTeam = player.getTeam();
        if (scoreboardTeam != null) {
            if (scoreboardTeam.getName().equalsIgnoreCase(CaptureConfig.BLUE_SCOREBOARD_TEAM.get())) {
                return CaptureTeam.BLUE;
            }
            if (scoreboardTeam.getName().equalsIgnoreCase(CaptureConfig.RED_SCOREBOARD_TEAM.get())) {
                return CaptureTeam.RED;
            }
        }
        for (String tag : player.getTags()) {
            if (CaptureConfig.BLUE_TAGS.get().contains(tag)) return CaptureTeam.BLUE;
            if (CaptureConfig.RED_TAGS.get().contains(tag)) return CaptureTeam.RED;
        }
        return CaptureTeam.NEUTRAL;
    }
}
