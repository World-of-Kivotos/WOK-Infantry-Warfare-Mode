package com.wok.infantry.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.battle.MemberView;
import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.client.ClientBattleState;
import com.wok.infantry.client.screen.SquadScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderPlayerEvent;

import java.util.UUID;

/** Local-only squad badge; it never changes vanilla team or global glowing state. */
public final class SquadWorldMarkerRenderer {
    private static final double MAX_DISTANCE_SQUARED = 96.0D * 96.0D;

    private SquadWorldMarkerRenderer() {
    }

    public static void onRenderPlayer(RenderPlayerEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player viewer = minecraft.player;
        Player target = event.getEntity();
        BattleSnapshot snapshot = ClientBattleState.snapshot();
        if (viewer == null || snapshot == null) {
            return;
        }

        MemberView member = eligibleMember(snapshot, viewer.getUUID(), target.getUUID(),
                viewer.distanceToSqr(target), target.isAlive(), target.isInvisibleTo(viewer));
        if (member == null) {
            return;
        }

        MutableComponent label = Component.literal("◆ ")
                .append(member.name()).append(" · ")
                .append(SquadScreen.callsign(snapshot.ownSquad()))
                .append(" · ").append(SquadScreen.className(snapshot, member.classId()));
        if (member.commander()) {
            label.append(" ").append(Component.translatable(
                    "hud.wok_infantry.role.commander_short"));
        } else if (member.leader()) {
            label.append(" ").append(Component.translatable("hud.wok_infantry.role.leader_short"));
        }

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(0.0D, target.getBbHeight() + 0.72D, 0.0D);
        poseStack.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);

        Font font = minecraft.font;
        float x = -font.width(label) / 2.0F;
        font.drawInBatch(label, x, 0.0F, 0xFF7FE8FF, false,
                poseStack.last().pose(), event.getMultiBufferSource(),
                Font.DisplayMode.NORMAL, 0x66081012, event.getPackedLight());
        poseStack.popPose();
    }

    /**
     * Resolves visibility and the label source from one captured snapshot, avoiding a mixed-frame
     * read if a network update replaces {@link ClientBattleState}'s volatile snapshot mid-render.
     */
    static MemberView eligibleMember(BattleSnapshot snapshot, UUID viewerId, UUID targetId,
                                     double distanceSquared, boolean targetEntityAlive,
                                     boolean invisibleToViewer) {
        // RenderPlayerEvent only visits entities in the current client level, so dimension
        // equality is already guaranteed at the event boundary.
        if (snapshot == null || viewerId == null || targetId == null
                || !snapshot.viewerId().equals(viewerId) || viewerId.equals(targetId)
                || snapshot.ownSquad() == null || !targetEntityAlive || invisibleToViewer
                || !Double.isFinite(distanceSquared) || distanceSquared < 0.0D
                || distanceSquared > MAX_DISTANCE_SQUARED) {
            return null;
        }
        SquadCallsign ownSquad = snapshot.ownSquad();
        MemberView member = snapshot.squads().stream()
                .filter(squad -> squad.callsign() == ownSquad)
                .flatMap(squad -> squad.members().stream())
                .filter(candidate -> candidate.playerId().equals(targetId))
                .findFirst().orElse(null);
        // WAITING/READY players are intentionally absent from the battlefield even if another
        // spectator client can still see their entity.
        return member != null && member.squad() == ownSquad
                && member.online() && member.alive() ? member : null;
    }
}
