package com.wok.trauma.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.wok.trauma.registry.ModEffects;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public final class TremorHandShake {
    public static void apply(PoseStack poseStack, Player player, float partialTick) {
        if (!player.hasEffect(ModEffects.TREMOR.get())) {
            return;
        }

        float time = player.tickCount + partialTick;
        float rapid = Mth.sin(time * 2.37F);
        float uneven = Mth.sin(time * 1.61F + 1.35F);
        float vertical = Mth.sin(time * 2.91F + 0.62F);

        poseStack.translate(
                (rapid * 0.0008F + uneven * 0.0004F),
                vertical * 0.0006F,
                0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(vertical * 0.08F));
        poseStack.mulPose(Axis.YP.rotationDegrees(uneven * 0.10F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rapid * 0.12F));
    }

    private TremorHandShake() {
    }
}
