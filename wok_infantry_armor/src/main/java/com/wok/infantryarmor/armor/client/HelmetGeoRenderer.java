package com.wok.infantryarmor.armor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wok.infantryarmor.armor.HelmetVariant;
import com.wok.infantryarmor.armor.item.HelmetItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * GeckoLib armor renderer for the generated headgear models (wok_infantry_armor/tools/helmet-models).
 *
 * <p>The models are built at true size with every visible surface at least 0.25 px outside the skin's
 * hat layer, so no extra scaling is applied. Visor glass and lenses live in their own {@value #GLASS_BONE}
 * bone and are drawn in a second, translucent pass after the opaque shell. Switching render types only
 * after the opaque pass has finished keeps the translucent state from leaking into the shell (drawing
 * glass bones in the middle of the recursion made the whole helmet translucent on GeckoLib 4.8.4).
 */
public final class HelmetGeoRenderer extends GeoArmorRenderer<HelmetItem> {

    private static final String SHELL_BONE = "helmet_shell";
    private static final String GLASS_BONE = "visor_glass";
    private static final float GLASS_ALPHA = 0.45F;

    public HelmetGeoRenderer(HelmetVariant variant) {
        super(new HelmetGeoModel(variant));
    }

    @Override
    public void actuallyRender(PoseStack poseStack, HelmetItem animatable, BakedGeoModel model,
                               RenderType renderType, MultiBufferSource bufferSource,
                               VertexConsumer buffer, boolean isReRender, float partialTick,
                               int packedLight, int packedOverlay, float red, float green,
                               float blue, float alpha) {
        GeoBone glass = model.getBone(GLASS_BONE).orElse(null);
        if (glass == null || isReRender || bufferSource == null) {
            super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
                    isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
            return;
        }
        GeoBone shell = model.getBone(SHELL_BONE).orElse(null);

        glass.setHidden(true);
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
                false, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        glass.setHidden(false);

        if (shell != null) {
            shell.setHidden(true);
        }
        try {
            RenderType glassType = RenderType.entityTranslucent(getTextureLocation(animatable));
            super.actuallyRender(poseStack, animatable, model, glassType, bufferSource,
                    bufferSource.getBuffer(glassType), true, partialTick, packedLight, packedOverlay,
                    red, green, blue, alpha * GLASS_ALPHA);
        } finally {
            if (shell != null) {
                shell.setHidden(false);
            }
        }
    }
}
