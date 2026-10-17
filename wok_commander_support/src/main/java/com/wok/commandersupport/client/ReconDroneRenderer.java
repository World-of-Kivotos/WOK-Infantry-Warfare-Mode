package com.wok.commandersupport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wok.commandersupport.WokCommanderSupportMod;
import com.wok.commandersupport.drone.ReconDroneAirframe;
import com.wok.commandersupport.drone.ReconDroneEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws the recon drone along its heading with the synchronised bank and pitch: banked into the
 * orbit, rolling out on departure and spiralling down once shot down. No shadow is cast from
 * cruise altitude.
 */
public class ReconDroneRenderer extends EntityRenderer<ReconDroneEntity> {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            WokCommanderSupportMod.MOD_ID, "textures/entity/recon_drone.png");

    private final ReconDroneModel model;

    public ReconDroneRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new ReconDroneModel(context.bakeLayer(ReconDroneModel.LAYER_LOCATION));
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(ReconDroneEntity drone, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, ReconDroneAirframe.MODEL_CENTER_HEIGHT, 0.0D);
        // Yaw, then pitch about the wing axis, then roll about the fuselage axis.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - drone.renderYaw(partialTick)));
        poseStack.mulPose(Axis.XP.rotationDegrees(-drone.renderPitchDown(partialTick)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-drone.renderBank(partialTick)));
        float scale = ReconDroneAirframe.RENDER_SCALE;
        poseStack.scale(-scale, -scale, scale);

        float ageInTicks = drone.tickCount + partialTick;
        model.setupAnim(drone, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);
        VertexConsumer consumer = buffers.getBuffer(model.renderType(TEXTURE));
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
        model.renderLights(poseStack, consumer, packedLight, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, ageInTicks, !drone.isCrashing());
        poseStack.popPose();
        super.render(drone, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ReconDroneEntity drone) {
        return TEXTURE;
    }
}
