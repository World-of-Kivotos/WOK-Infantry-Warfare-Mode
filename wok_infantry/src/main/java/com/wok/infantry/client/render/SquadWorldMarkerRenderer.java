package com.wok.infantry.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.battle.MemberView;
import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.client.ClientBattleState;
import com.wok.infantry.client.screen.SquadLabels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderPlayerEvent;

import java.util.UUID;

/**
 * Local-only squad badge; it never changes vanilla team or global glowing state. Hidden with F1
 * like the rest of the HUD and drawn full-bright, so it reads the same at night and in caves.
 */
public final class SquadWorldMarkerRenderer {
    private static final double MAX_DISTANCE_SQUARED = 96.0D * 96.0D;
    /** Same cyan as the own squad on the tactical map. */
    private static final int LABEL_COLOR = 0xFF7FE8FF;
    private static final int LABEL_BACKGROUND = 0x66081012;

    private SquadWorldMarkerRenderer() {
    }

    public static void onRenderPlayer(RenderPlayerEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player viewer = minecraft.player;
        Player target = event.getEntity();
        BattleSnapshot snapshot = ClientBattleState.snapshot();
        if (viewer == null || snapshot == null || minecraft.options.hideGui) {
            return;
        }

        MemberView member = eligibleMember(snapshot, viewer.getUUID(), target.getUUID(),
                viewer.distanceToSqr(target), target.isAlive(), target.isInvisibleTo(viewer));
        if (member == null) {
            return;
        }

        MutableComponent label = Component.literal("◆ ")
                .append(member.name()).append(" · ")
                .append(SquadLabels.callsign(snapshot.ownSquad()))
                .append(" · ").append(SquadLabels.className(snapshot, member.classId()));
        MutableComponent roles = roleSuffix(member);
        if (roles != null) {
            label.append(" ").append(roles);
        }

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(0.0D, target.getBbHeight() + 0.72D, 0.0D);
        poseStack.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);

        Font font = minecraft.font;
        float x = -font.width(label) / 2.0F;
        font.drawInBatch(label, x, 0.0F, LABEL_COLOR, false,
                poseStack.last().pose(), event.getMultiBufferSource(),
                Font.DisplayMode.NORMAL, LABEL_BACKGROUND, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    /**
     * Role tags after the label: squad leader, faction commander, or both ("队长·指挥") for a
     * member who holds both; null for a plain member.
     */
    static MutableComponent roleSuffix(MemberView member) {
        return member == null ? null : SquadLabels.roleShort(member.leader(), member.commander());
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
