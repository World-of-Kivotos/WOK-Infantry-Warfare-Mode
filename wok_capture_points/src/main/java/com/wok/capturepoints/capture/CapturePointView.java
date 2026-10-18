package com.wok.capturepoints.capture;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public record CapturePointView(
        String id,
        String displayName,
        ResourceLocation dimension,
        BlockPos min,
        BlockPos max,
        int order,
        int captureSeconds,
        boolean enabled,
        double control,
        CaptureTeam owner,
        CaptureTeam activeTeam,
        int bluePlayers,
        int redPlayers,
        int speedMultiplier,
        boolean blueAllowed,
        boolean redAllowed) {
}
