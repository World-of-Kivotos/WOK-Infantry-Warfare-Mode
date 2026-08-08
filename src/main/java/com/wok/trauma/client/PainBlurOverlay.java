package com.wok.trauma.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wok.trauma.WokTraumaMod;
import com.wok.trauma.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public final class PainBlurOverlay {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(WokTraumaMod.MOD_ID, "textures/misc/pain_blur.png");

    public static final IGuiOverlay INSTANCE = (gui, graphics, partialTick, width, height) -> {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.hasEffect(ModEffects.PAIN.get())) {
            return;
        }

        float pulse = 0.5F + 0.5F * Mth.sin((minecraft.player.tickCount + partialTick) * 0.18F);
        float alpha = 0.32F + pulse * 0.10F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(1.0F, 0.82F, 0.86F, alpha);
        graphics.blit(TEXTURE, 0, 0, -90, 0.0F, 0.0F,
                width, height, width, height);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    };

    private PainBlurOverlay() {
    }
}
