package com.wok.infantryarmor.armor.client;

import com.wok.infantryarmor.armor.item.HelmetItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

/** Lazily bakes the head model bound to one registered helmet item. */
public final class HelmetArmorClient implements IClientItemExtensions {

    private final HelmetItem helmetItem;
    private HumanoidModel<?> model;

    private HelmetArmorClient(HelmetItem helmetItem) {
        this.helmetItem = helmetItem;
    }

    public static IClientItemExtensions forItem(HelmetItem helmetItem) {
        return new HelmetArmorClient(helmetItem);
    }

    @Override
    @NotNull
    public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack,
                                                   EquipmentSlot equipmentSlot,
                                                   HumanoidModel<?> original) {
        if (equipmentSlot != EquipmentSlot.HEAD || itemStack.getItem() != helmetItem) {
            return original;
        }
        if (helmetItem.variant().usesGeoModel()) {
            if (!(model instanceof HelmetGeoRenderer)) {
                model = new HelmetGeoRenderer(helmetItem.variant());
            }
            ((HelmetGeoRenderer) model).prepForRender(livingEntity, itemStack, equipmentSlot, original);
            return model;
        }
        if (model == null) {
            ModelPart root = Minecraft.getInstance().getEntityModels()
                    .bakeLayer(HelmetArmorModel.layer(helmetItem.variant()));
            model = new HelmetArmorModel(root);
        }
        return model;
    }
}
