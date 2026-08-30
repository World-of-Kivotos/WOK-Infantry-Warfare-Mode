package com.wok.infantryarmor.armor.client;

import com.wok.infantryarmor.WokInfantryArmorMod;
import com.wok.infantryarmor.armor.HelmetVariant;
import com.wok.infantryarmor.armor.item.HelmetItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Resource-backed Blockbench/GeckoLib model used by detailed headgear. */
public final class HelmetGeoModel extends GeoModel<HelmetItem> {

    private static final ResourceLocation STATIC_ANIMATION = new ResourceLocation(
            WokInfantryArmorMod.MODID, "animations/helmet_static.animation.json");

    private final HelmetVariant variant;

    public HelmetGeoModel(HelmetVariant variant) {
        this.variant = variant;
    }

    @Override
    public ResourceLocation getModelResource(HelmetItem animatable) {
        return new ResourceLocation(WokInfantryArmorMod.MODID,
                "geo/helmet_" + variant.id() + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HelmetItem animatable) {
        return new ResourceLocation(WokInfantryArmorMod.MODID,
                "textures/models/armor/helmet_" + variant.id() + "_layer_1.png");
    }

    @Override
    public ResourceLocation getAnimationResource(HelmetItem animatable) {
        return STATIC_ANIMATION;
    }
}
