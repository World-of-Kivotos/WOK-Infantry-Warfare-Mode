package com.wok.infantry.battle;

public record PermissionView(
        boolean canCreateSquad,
        boolean canJoinSquad,
        boolean canLeaveSquad,
        boolean canManageSquad,
        boolean canCreateMarkers,
        boolean canRemoveAnyMarker,
        boolean canClaimCommander
) {
}
