package com.wok.infantryarmor.armor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wok.infantryarmor.armor.HelmetVariant;
import com.wok.infantryarmor.armor.item.HelmetItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/** GeckoLib armor renderer for Blockbench-authored headgear geometry. */
public final class HelmetGeoRenderer extends GeoArmorRenderer<HelmetItem> {

    /**
     * Player head overlays extend beyond the base 8x8 head. This is especially visible when a
     * client turns the skin's second layer into real voxel geometry. Keep the helmet outside that
     * envelope instead of hiding the player's complete head overlay (which may also contain
     * glasses, facial hair, or other details).
     */
    private static final float HEAD_OVERLAY_CLEARANCE_WIDTH = 1.10F;
    private static final float HEAD_OVERLAY_CLEARANCE_HEIGHT = 1.06F;

    private static final ResourceLocation VISOR_GLASS_TEXTURE = new ResourceLocation(
            "minecraft", "textures/block/tinted_glass.png");

    private final HelmetVariant variant;

    public HelmetGeoRenderer(HelmetVariant variant) {
        super(new HelmetGeoModel(variant));
        this.variant = variant;
        withScale(HEAD_OVERLAY_CLEARANCE_WIDTH, HEAD_OVERLAY_CLEARANCE_HEIGHT);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, HelmetItem animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource,
                                  VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, float red, float green,
                                  float blue, float alpha) {
        boolean vulkanGlass = variant == HelmetVariant.VULKAN_5_HEAVY
                && "vulkan_visor_glass".equals(bone.getName());
        // Altyn uses opaque smoked armored glass. Rendering several curved
        // child bones through entityTranslucent leaks the translucent render
        // state into the complete Gecko armor pass on Forge/GeckoLib 4.8.4,
        // making the green titanium shell appear transparent. Keep only the
        // single-plane Vulkan visor on the translucent pass.
        if (vulkanGlass) {
            RenderType glassRenderType = RenderType.entityTranslucent(VISOR_GLASS_TEXTURE);
            VertexConsumer glassBuffer = bufferSource.getBuffer(glassRenderType);
            super.renderRecursively(poseStack, animatable, bone, glassRenderType, bufferSource,
                    glassBuffer, isReRender, partialTick, packedLight, packedOverlay,
                    0.58F, 0.66F, 0.70F, 0.42F);
            return;
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
