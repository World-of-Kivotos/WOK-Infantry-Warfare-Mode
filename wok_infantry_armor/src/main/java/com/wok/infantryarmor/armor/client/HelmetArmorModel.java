package com.wok.infantryarmor.armor.client;

import com.wok.infantryarmor.WokInfantryArmorMod;
import com.wok.infantryarmor.armor.HelmetVariant;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.EntityRenderersEvent;

/** Procedural head-slot geometry for the standalone helmet collection. */
public final class HelmetArmorModel extends HumanoidModel<LivingEntity> {

    public HelmetArmorModel(ModelPart root) {
        super(root);
    }

    public static ModelLayerLocation layer(HelmetVariant variant) {
        return new ModelLayerLocation(new ResourceLocation(
                WokInfantryArmorMod.MODID, variant.itemId()), "main");
    }

    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (HelmetVariant variant : HelmetVariant.values()) {
            event.registerLayerDefinition(layer(variant), () -> createLayer(variant));
        }
    }

    public static LayerDefinition createLayer(HelmetVariant variant) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", geometry(variant), PartPose.ZERO);
        addVariantDetails(head, variant);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(),
                PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void addVariantDetails(PartDefinition head, HelmetVariant variant) {
        switch (variant) {
            case RIOT -> addRiotDetails(head);
            case CQCM_BALLISTIC_MASK -> addCqcmDetails(head);
            case LZSH_LIGHT -> addLzshDetails(head);
            case B6B47 -> addB6b47Details(head, false);
            case B6B47_DIGITAL_COVER -> addB6b47Details(head, true);
            case CAIMAN_COMPOSITE -> addCaimanDetails(head);
            case KIVER_M_HEAVY -> addKiverDetails(head);
            default -> {
            }
        }
    }

    private static CubeListBuilder geometry(HelmetVariant variant) {
        return switch (variant) {
            case RIOT -> riot();
            case LIGHTWEIGHT_BALLISTIC_MASK -> lightweightMask();
            case CQCM_BALLISTIC_MASK -> cqcmMask();
            case LZSH_LIGHT -> lzsh();
            case B6B47 -> b6b47(false);
            case B6B47_DIGITAL_COVER -> b6b47(true);
            case CAIMAN_COMPOSITE -> caiman();
            case KIVER_M_HEAVY -> kiver();
            case ACHHC_LIGHT -> achhc();
            case STRIKE -> strike();
            case FAST_MT_SUPER_HIGH_CUT -> fastMt();
            case AIRFRAME -> airframe();
            case FLUX -> flux();
            case VULKAN_5_HEAVY -> kiver();
            case ALTYN_HEAVY -> kiver();
            case FAST_HEAVY_PROTECTION_KIT -> fastMt();
            case MASKA_1SCH_HEAVY -> kiver();
        };
    }

    private static CubeListBuilder crown(CubeListBuilder b) {
        return b.texOffs(0, 0).addBox(-3.55F, -9.00F, -3.55F, 7.10F, 0.70F, 7.10F)
                .texOffs(0, 8).addBox(-4.05F, -8.30F, -4.05F, 8.10F, 1.15F, 8.10F)
                .texOffs(0, 18).addBox(-4.35F, -7.15F, -4.35F, 8.70F, 2.20F, 8.70F);
    }

    private static CubeListBuilder lowSides(CubeListBuilder b, float height) {
        return b.texOffs(0, 32).addBox(-4.48F, -4.95F, -4.10F, 0.72F, height, 8.20F)
                .texOffs(0, 32).addBox(3.76F, -4.95F, -4.10F, 0.72F, height, 8.20F)
                .texOffs(20, 32).addBox(-3.76F, -4.95F, 3.55F, 7.52F, height, 0.70F);
    }

    private static CubeListBuilder frontMount(CubeListBuilder b, float y) {
        return b.texOffs(0, 48).addBox(-1.65F, y, -4.82F, 3.30F, 2.05F, 0.48F)
                .texOffs(8, 48).addBox(-1.05F, y + 0.35F, -5.05F, 2.10F, 1.25F, 0.28F);
    }

    private static CubeListBuilder sideRails(CubeListBuilder b, float y) {
        return b.texOffs(16, 48).addBox(-4.78F, y, -1.25F, 0.46F, 1.55F, 4.45F)
                .texOffs(16, 48).addBox(4.32F, y, -1.25F, 0.46F, 1.55F, 4.45F)
                .texOffs(28, 48).addBox(-4.94F, y + 0.35F, -0.80F, 0.30F, 0.26F, 3.55F)
                .texOffs(28, 48).addBox(4.64F, y + 0.35F, -0.80F, 0.30F, 0.26F, 3.55F);
    }

    private static CubeListBuilder chinStraps(CubeListBuilder b) {
        return b.texOffs(38, 48).addBox(-3.55F, -3.20F, -4.48F, 0.34F, 4.60F, 0.30F)
                .texOffs(38, 48).addBox(3.21F, -3.20F, -4.48F, 0.34F, 4.60F, 0.30F)
                .texOffs(42, 48).addBox(-3.25F, 1.05F, -4.56F, 6.50F, 0.34F, 0.34F)
                .texOffs(42, 48).addBox(-1.25F, 1.32F, -4.58F, 2.50F, 0.28F, 0.34F);
    }

    private static CubeListBuilder earPads(CubeListBuilder b) {
        return b.texOffs(0, 32).addBox(-4.72F, -4.35F, -1.35F, 0.72F, 3.25F, 3.80F)
                .texOffs(0, 32).addBox(4.00F, -4.35F, -1.35F, 0.72F, 3.25F, 3.80F);
    }

    private static CubeListBuilder riot() {
        return CubeListBuilder.create()
                // Stepped shell: each ring expands toward the brow to retain a readable dome.
                .texOffs(0, 16).addBox(-2.75F, -9.15F, -2.90F, 5.50F, 0.55F, 5.80F)
                .texOffs(16, 16).addBox(-3.30F, -8.60F, -3.45F, 6.60F, 0.65F, 6.90F)
                .texOffs(32, 16).addBox(-3.80F, -7.95F, -3.90F, 7.60F, 0.80F, 7.80F)
                .texOffs(0, 16).addBox(-4.25F, -7.15F, -4.25F, 8.50F, 2.25F, 8.50F)
                // Open-face lower shell: side cheeks and rear skirt only.
                .texOffs(0, 16).addBox(-4.48F, -4.90F, -4.08F, 0.72F, 4.05F, 8.16F)
                .texOffs(0, 16).addBox(3.76F, -4.90F, -4.08F, 0.72F, 4.05F, 8.16F)
                .texOffs(18, 16).addBox(-3.76F, -4.90F, 3.48F, 7.52F, 4.05F, 0.72F)
                // Dark rubber rim around the face opening.
                .texOffs(0, 32).addBox(-4.16F, -5.08F, -4.62F, 8.32F, 0.54F, 0.54F)
                .texOffs(16, 32).addBox(-4.62F, -4.62F, -4.32F, 0.42F, 3.85F, 0.46F)
                .texOffs(16, 32).addBox(4.20F, -4.62F, -4.32F, 0.42F, 3.85F, 0.46F)
                // Right-side adjustment plate and fasteners.
                .texOffs(22, 32).addBox(4.42F, -4.30F, -0.75F, 0.52F, 1.65F, 2.55F)
                .texOffs(30, 32).addBox(4.82F, -4.02F, -0.40F, 0.22F, 0.42F, 0.42F)
                .texOffs(30, 32).addBox(4.82F, -3.36F, 0.82F, 0.22F, 0.42F, 0.42F)
                .texOffs(34, 32).addBox(4.78F, -2.15F, 1.36F, 0.18F, 0.48F, 0.48F);
    }

    private static void addRiotDetails(PartDefinition head) {
        // Olive padding remains visible inside the open face without covering the player skin.
        head.addOrReplaceChild("riot_brow_padding", CubeListBuilder.create()
                        .texOffs(0, 48).addBox(-3.65F, -0.28F, -0.28F, 7.30F, 0.56F, 0.56F),
                PartPose.offset(0.0F, -4.42F, -4.18F));
        head.addOrReplaceChild("riot_left_padding", CubeListBuilder.create()
                        .texOffs(16, 48).addBox(-0.30F, 0.0F, -0.28F, 0.60F, 3.15F, 0.56F),
                PartPose.offset(-3.82F, -4.30F, -4.12F));
        head.addOrReplaceChild("riot_right_padding", CubeListBuilder.create()
                        .texOffs(16, 48).addBox(-0.30F, 0.0F, -0.28F, 0.60F, 3.15F, 0.56F),
                PartPose.offset(3.82F, -4.30F, -4.12F));

        // Two articulated straps converge below the chin like the approved preview.
        head.addOrReplaceChild("riot_left_chin_strap", CubeListBuilder.create()
                        .texOffs(22, 48).addBox(-0.18F, 0.0F, -0.18F, 0.36F, 4.75F, 0.36F),
                PartPose.offsetAndRotation(-3.55F, -1.65F, -4.40F, 0.0F, 0.0F, -0.28F));
        head.addOrReplaceChild("riot_right_chin_strap", CubeListBuilder.create()
                        .texOffs(22, 48).addBox(-0.18F, 0.0F, -0.18F, 0.36F, 4.75F, 0.36F),
                PartPose.offsetAndRotation(3.55F, -1.65F, -4.40F, 0.0F, 0.0F, 0.28F));
        head.addOrReplaceChild("riot_chin_cup", CubeListBuilder.create()
                        .texOffs(28, 32).addBox(-1.20F, -0.38F, -0.36F, 2.40F, 0.76F, 0.72F)
                        .texOffs(38, 32).addBox(-0.72F, 0.38F, -0.30F, 1.44F, 0.32F, 0.60F),
                PartPose.offset(0.0F, 2.55F, -4.52F));
    }

    private static void addCqcmDetails(PartDefinition head) {
        // Recessed rims sharpen the two eye openings and stop the mask reading as a flat slab.
        head.addOrReplaceChild("cqcm_left_eye_rim", CubeListBuilder.create()
                        .texOffs(20, 32).addBox(-2.55F, -0.18F, -0.16F, 2.55F, 0.36F, 0.32F)
                        .texOffs(20, 32).addBox(-2.55F, 1.42F, -0.16F, 2.55F, 0.36F, 0.32F)
                        .texOffs(28, 32).addBox(-2.72F, 0.0F, -0.16F, 0.34F, 1.42F, 0.32F),
                PartPose.offset(-0.66F, -6.28F, -4.65F));
        head.addOrReplaceChild("cqcm_right_eye_rim", CubeListBuilder.create()
                        .texOffs(20, 32).addBox(0.0F, -0.18F, -0.16F, 2.55F, 0.36F, 0.32F)
                        .texOffs(20, 32).addBox(0.0F, 1.42F, -0.16F, 2.55F, 0.36F, 0.32F)
                        .texOffs(28, 32).addBox(2.38F, 0.0F, -0.16F, 0.34F, 1.42F, 0.32F),
                PartPose.offset(0.66F, -6.28F, -4.65F));

        addMaskSideStrap(head, "cqcm_upper", -6.95F, 0.38F);
        addMaskSideStrap(head, "cqcm_middle", -4.05F, 0.34F);
        addMaskSideStrap(head, "cqcm_lower", -1.30F, 0.34F);

        head.addOrReplaceChild("cqcm_left_top_strap", CubeListBuilder.create()
                        .texOffs(0, 48).addBox(-0.24F, -0.18F, -2.90F, 0.48F, 0.36F, 4.45F)
                        .texOffs(10, 32).addBox(-0.42F, -0.32F, -3.18F, 0.84F, 0.64F, 0.72F),
                PartPose.offsetAndRotation(-2.35F, -8.62F, 0.15F, -0.10F, 0.0F, 0.0F));
        head.addOrReplaceChild("cqcm_right_top_strap", CubeListBuilder.create()
                        .texOffs(0, 48).addBox(-0.24F, -0.18F, -2.90F, 0.48F, 0.36F, 4.45F)
                        .texOffs(10, 32).addBox(-0.42F, -0.32F, -3.18F, 0.84F, 0.64F, 0.72F),
                PartPose.offsetAndRotation(2.35F, -8.62F, 0.15F, -0.10F, 0.0F, 0.0F));
    }

    private static void addMaskSideStrap(PartDefinition head, String name, float y, float thickness) {
        head.addOrReplaceChild(name + "_left", CubeListBuilder.create()
                        .texOffs(14, 48).addBox(-0.18F, -0.18F, -0.15F, thickness, 0.36F, 4.75F)
                        .texOffs(26, 32).addBox(-0.30F, -0.42F, 0.08F, 0.60F, 0.84F, 0.78F),
                PartPose.offset(-4.35F, y, -0.10F));
        head.addOrReplaceChild(name + "_right", CubeListBuilder.create()
                        .texOffs(14, 48).addBox(-thickness + 0.18F, -0.18F, -0.15F,
                                thickness, 0.36F, 4.75F)
                        .texOffs(26, 32).addBox(-0.30F, -0.42F, 0.08F, 0.60F, 0.84F, 0.78F),
                PartPose.offset(4.35F, y, -0.10F));
    }

    private static void addLzshDetails(PartDefinition head) {
        addHelmetHarness(head, "lzsh", 3.62F, -3.58F, -4.28F, 4.95F, 0.27F, 2.12F);
        head.addOrReplaceChild("lzsh_crown_patch", CubeListBuilder.create()
                        .texOffs(40, 32).addBox(-1.15F, -0.12F, -1.82F, 2.30F, 0.24F, 3.64F),
                PartPose.offsetAndRotation(1.70F, -8.88F, -0.35F, 0.0F, 0.0F, -0.10F));
        head.addOrReplaceChild("lzsh_rail_teeth", CubeListBuilder.create()
                        .texOffs(50, 32).addBox(-0.12F, -0.10F, -1.45F, 0.24F, 0.20F, 0.45F)
                        .texOffs(50, 32).addBox(-0.12F, -0.10F, -0.55F, 0.24F, 0.20F, 0.45F)
                        .texOffs(50, 32).addBox(-0.12F, -0.10F, 0.35F, 0.24F, 0.20F, 0.45F)
                        .texOffs(50, 32).addBox(-0.12F, -0.10F, 1.25F, 0.24F, 0.20F, 0.45F),
                PartPose.offset(4.82F, -4.73F, 0.18F));
    }

    private static void addB6b47Details(PartDefinition head, boolean covered) {
        String prefix = covered ? "b6b47_cover" : "b6b47";
        addHelmetHarness(head, prefix, 3.56F, -3.55F, -4.26F, 5.05F, 0.28F, 2.18F);
        head.addOrReplaceChild(prefix + "_rear_harness", CubeListBuilder.create()
                        .texOffs(28, 48).addBox(-3.12F, -0.17F, -0.17F, 6.24F, 0.34F, 0.34F),
                PartPose.offset(0.0F, -2.65F, 4.18F));
        if (covered) {
            head.addOrReplaceChild("b6b47_cover_side_tab", CubeListBuilder.create()
                            .texOffs(50, 32).addBox(-0.16F, -0.42F, -1.20F, 0.32F, 0.84F, 2.40F),
                    PartPose.offset(4.42F, -4.40F, 2.30F));
        }
    }

    private static void addCaimanDetails(PartDefinition head) {
        addHelmetHarness(head, "caiman", 3.46F, -3.62F, -4.27F, 4.92F, 0.29F, 2.08F);
        head.addOrReplaceChild("caiman_left_ear_pad", CubeListBuilder.create()
                        .texOffs(0, 32).addBox(-0.34F, -1.20F, -1.30F, 0.68F, 2.40F, 2.60F),
                PartPose.offset(-4.26F, -2.72F, 0.10F));
        head.addOrReplaceChild("caiman_right_ear_pad", CubeListBuilder.create()
                        .texOffs(0, 32).addBox(-0.34F, -1.20F, -1.30F, 0.68F, 2.40F, 2.60F),
                PartPose.offset(4.26F, -2.72F, 0.10F));
        head.addOrReplaceChild("caiman_rear_dial", CubeListBuilder.create()
                        .texOffs(10, 32).addBox(-0.72F, -0.72F, -0.20F, 1.44F, 1.44F, 0.40F)
                        .texOffs(16, 32).addBox(-0.36F, -0.36F, -0.36F, 0.72F, 0.72F, 0.32F),
                PartPose.offset(0.0F, -3.62F, 4.32F));
    }

    private static void addKiverDetails(PartDefinition head) {
        addHelmetHarness(head, "kiver", 3.72F, -2.82F, -4.38F, 4.88F, 0.24F, 2.05F);
        head.addOrReplaceChild("kiver_chin_pad", CubeListBuilder.create()
                        .texOffs(20, 32).addBox(-1.38F, -0.42F, -0.42F, 2.76F, 0.84F, 0.84F)
                        .texOffs(30, 32).addBox(-0.92F, 0.42F, -0.34F, 1.84F, 0.34F, 0.68F),
                PartPose.offset(0.0F, 2.22F, -4.48F));
        head.addOrReplaceChild("kiver_left_side_clip", CubeListBuilder.create()
                        .texOffs(38, 32).addBox(-0.20F, -0.52F, -0.36F, 0.40F, 1.04F, 0.72F),
                PartPose.offset(-4.86F, -3.55F, 0.58F));
        head.addOrReplaceChild("kiver_right_side_clip", CubeListBuilder.create()
                        .texOffs(38, 32).addBox(-0.20F, -0.52F, -0.36F, 0.40F, 1.04F, 0.72F),
                PartPose.offset(4.86F, -3.55F, 0.58F));
    }

    private static void addHelmetHarness(PartDefinition head, String prefix, float attachX,
                                         float attachY, float attachZ, float length,
                                         float angle, float chinY) {
        head.addOrReplaceChild(prefix + "_left_front_strap", CubeListBuilder.create()
                        .texOffs(0, 48).addBox(-0.18F, 0.0F, -0.18F, 0.36F, length, 0.36F),
                PartPose.offsetAndRotation(-attachX, attachY, attachZ, 0.0F, 0.0F, -angle));
        head.addOrReplaceChild(prefix + "_right_front_strap", CubeListBuilder.create()
                        .texOffs(0, 48).addBox(-0.18F, 0.0F, -0.18F, 0.36F, length, 0.36F),
                PartPose.offsetAndRotation(attachX, attachY, attachZ, 0.0F, 0.0F, angle));
        head.addOrReplaceChild(prefix + "_left_rear_strap", CubeListBuilder.create()
                        .texOffs(8, 48).addBox(-0.17F, 0.0F, -0.17F, 0.34F, length - 0.55F, 0.34F),
                PartPose.offsetAndRotation(-attachX, attachY + 0.28F, 2.75F,
                        -0.12F, 0.0F, -angle * 0.72F));
        head.addOrReplaceChild(prefix + "_right_rear_strap", CubeListBuilder.create()
                        .texOffs(8, 48).addBox(-0.17F, 0.0F, -0.17F, 0.34F, length - 0.55F, 0.34F),
                PartPose.offsetAndRotation(attachX, attachY + 0.28F, 2.75F,
                        -0.12F, 0.0F, angle * 0.72F));
        head.addOrReplaceChild(prefix + "_chin_band", CubeListBuilder.create()
                        .texOffs(16, 48).addBox(-2.18F, -0.18F, -0.18F, 4.36F, 0.36F, 0.36F)
                        .texOffs(28, 48).addBox(-0.55F, -0.34F, -0.26F, 1.10F, 0.68F, 0.52F),
                PartPose.offset(0.0F, chinY, -4.46F));
    }

    private static CubeListBuilder lightweightMask() {
        CubeListBuilder b = CubeListBuilder.create()
                .texOffs(0, 32).addBox(-3.95F, -7.35F, -4.62F, 7.90F, 1.30F, 0.55F)
                .texOffs(0, 32).addBox(-3.95F, -6.05F, -4.62F, 2.65F, 1.65F, 0.55F)
                .texOffs(0, 32).addBox(1.30F, -6.05F, -4.62F, 2.65F, 1.65F, 0.55F)
                .texOffs(20, 32).addBox(-1.25F, -6.05F, -4.78F, 2.50F, 3.85F, 0.72F)
                .texOffs(26, 32).addBox(-3.70F, -4.15F, -4.58F, 2.45F, 3.55F, 0.52F)
                .texOffs(26, 32).addBox(1.25F, -4.15F, -4.58F, 2.45F, 3.55F, 0.52F)
                .texOffs(36, 32).addBox(-2.95F, -1.20F, -4.70F, 5.90F, 2.15F, 0.66F)
                .texOffs(0, 48).addBox(-3.25F, -8.35F, -0.75F, 1.10F, 0.34F, 5.30F)
                .texOffs(0, 48).addBox(2.15F, -8.35F, -0.75F, 1.10F, 0.34F, 5.30F)
                .texOffs(12, 48).addBox(-0.42F, -9.05F, -3.40F, 0.84F, 0.34F, 7.35F)
                .texOffs(22, 48).addBox(-4.55F, -5.90F, -0.25F, 0.34F, 4.80F, 1.00F)
                .texOffs(22, 48).addBox(4.21F, -5.90F, -0.25F, 0.34F, 4.80F, 1.00F);
        return b;
    }

    private static CubeListBuilder cqcmMask() {
        return CubeListBuilder.create()
                // Brow and temples form a rounded upper plate without filling the eye openings.
                .texOffs(0, 16).addBox(-3.15F, -8.35F, -4.46F, 6.30F, 0.55F, 0.48F)
                .texOffs(14, 16).addBox(-3.62F, -7.80F, -4.50F, 7.24F, 1.25F, 0.52F)
                .texOffs(0, 16).addBox(-3.82F, -6.55F, -4.54F, 1.10F, 2.05F, 0.56F)
                .texOffs(0, 16).addBox(2.72F, -6.55F, -4.54F, 1.10F, 2.05F, 0.56F)
                // Narrow bridge separates two real openings; cheek plates taper toward the jaw.
                .texOffs(8, 16).addBox(-0.58F, -6.55F, -4.72F, 1.16F, 4.55F, 0.72F)
                .texOffs(14, 16).addBox(-3.64F, -4.50F, -4.58F, 2.72F, 3.55F, 0.60F)
                .texOffs(14, 16).addBox(0.92F, -4.50F, -4.58F, 2.72F, 3.55F, 0.60F)
                .texOffs(28, 16).addBox(-2.92F, -2.15F, -4.66F, 5.84F, 2.55F, 0.68F)
                .texOffs(42, 16).addBox(-2.18F, 0.40F, -4.60F, 4.36F, 0.62F, 0.56F)
                // Thin side wings follow the head and carry the strap mounts.
                .texOffs(0, 32).addBox(-4.38F, -7.55F, -2.95F, 0.52F, 6.85F, 3.15F)
                .texOffs(0, 32).addBox(3.86F, -7.55F, -2.95F, 0.52F, 6.85F, 3.15F)
                .texOffs(10, 32).addBox(-3.86F, -8.10F, 1.80F, 7.72F, 0.48F, 1.70F);
    }

    private static CubeListBuilder lzsh() {
        return CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.90F, -9.00F, -3.05F, 5.80F, 0.58F, 6.10F)
                .texOffs(14, 0).addBox(-3.55F, -8.42F, -3.65F, 7.10F, 0.78F, 7.30F)
                .texOffs(30, 0).addBox(-4.12F, -7.64F, -4.08F, 8.24F, 2.20F, 8.16F)
                .texOffs(0, 16).addBox(-4.38F, -5.44F, -3.92F, 0.74F, 2.45F, 7.35F)
                .texOffs(0, 16).addBox(3.64F, -5.44F, -3.92F, 0.74F, 2.45F, 7.35F)
                .texOffs(16, 16).addBox(-3.64F, -5.44F, 3.42F, 7.28F, 2.70F, 0.74F)
                .texOffs(30, 16).addBox(-4.18F, -5.62F, -4.52F, 8.36F, 0.40F, 0.52F)
                // Rectangular front shroud and deep side rail match the reference silhouette.
                .texOffs(0, 32).addBox(-3.05F, -7.30F, -4.66F, 1.55F, 2.55F, 0.44F)
                .texOffs(8, 32).addBox(-2.68F, -6.95F, -4.90F, 0.82F, 1.70F, 0.28F)
                .texOffs(14, 32).addBox(4.12F, -5.35F, -1.92F, 0.58F, 1.62F, 4.95F)
                .texOffs(26, 32).addBox(4.56F, -5.05F, -1.42F, 0.24F, 0.26F, 3.70F)
                .texOffs(26, 32).addBox(4.56F, -4.43F, -1.42F, 0.24F, 0.26F, 3.70F)
                .texOffs(36, 32).addBox(-4.74F, -4.48F, -0.82F, 0.56F, 2.82F, 3.72F)
                .texOffs(36, 32).addBox(4.18F, -4.48F, -0.82F, 0.56F, 2.82F, 3.72F);
    }

    private static CubeListBuilder b6b47(boolean covered) {
        CubeListBuilder b = CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.80F, -9.05F, -2.95F, 5.60F, 0.55F, 5.90F)
                .texOffs(14, 0).addBox(-3.48F, -8.50F, -3.60F, 6.96F, 0.75F, 7.20F)
                .texOffs(30, 0).addBox(-4.08F, -7.75F, -4.08F, 8.16F, 2.42F, 8.16F)
                .texOffs(0, 16).addBox(-4.35F, -5.33F, -3.92F, 0.72F, 3.00F, 7.45F)
                .texOffs(0, 16).addBox(3.63F, -5.33F, -3.92F, 0.72F, 3.00F, 7.45F)
                .texOffs(16, 16).addBox(-3.63F, -5.33F, 3.50F, 7.26F, 3.22F, 0.66F)
                .texOffs(30, 16).addBox(-4.12F, -5.52F, -4.50F, 8.24F, 0.38F, 0.50F)
                .texOffs(0, 32).addBox(-1.36F, -7.45F, -4.62F, 2.72F, 2.20F, 0.42F)
                .texOffs(8, 32).addBox(-0.82F, -7.06F, -4.86F, 1.64F, 1.42F, 0.25F)
                .texOffs(16, 32).addBox(-1.12F, -6.15F, -4.96F, 0.34F, 0.34F, 0.20F)
                .texOffs(16, 32).addBox(0.78F, -6.15F, -4.96F, 0.34F, 0.34F, 0.20F);
        if (covered) {
            // Raised cloth panels, crown seam and front flap make the covered model unmistakable.
            b.texOffs(0, 32).addBox(-3.58F, -8.68F, -3.76F, 7.16F, 0.20F, 7.52F)
                    .texOffs(18, 32).addBox(-4.22F, -7.88F, -4.20F, 8.44F, 2.18F, 8.40F)
                    .texOffs(36, 32).addBox(-0.18F, -8.90F, -4.05F, 0.36F, 0.24F, 8.10F)
                    .texOffs(42, 32).addBox(-3.18F, -7.55F, -4.45F, 2.90F, 1.55F, 0.28F)
                    .texOffs(42, 32).addBox(-3.00F, -7.77F, -4.58F, 2.55F, 0.40F, 1.52F)
                    .texOffs(50, 32).addBox(-2.68F, -6.72F, 4.15F, 5.36F, 0.64F, 0.24F);
        }
        return b;
    }

    private static CubeListBuilder caiman() {
        return CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.82F, -9.05F, -3.00F, 5.64F, 0.58F, 6.00F)
                .texOffs(14, 0).addBox(-3.52F, -8.47F, -3.65F, 7.04F, 0.76F, 7.30F)
                .texOffs(30, 0).addBox(-4.10F, -7.71F, -4.05F, 8.20F, 2.20F, 8.10F)
                // High-cut shell exposes the ear while retaining a rear nape extension.
                .texOffs(0, 16).addBox(-4.34F, -5.51F, -3.90F, 0.66F, 1.55F, 3.20F)
                .texOffs(0, 16).addBox(3.68F, -5.51F, -3.90F, 0.66F, 1.55F, 3.20F)
                .texOffs(10, 16).addBox(-4.30F, -5.51F, 1.30F, 0.68F, 2.72F, 2.65F)
                .texOffs(10, 16).addBox(3.62F, -5.51F, 1.30F, 0.68F, 2.72F, 2.65F)
                .texOffs(20, 16).addBox(-3.62F, -5.51F, 3.46F, 7.24F, 2.90F, 0.68F)
                .texOffs(34, 16).addBox(-4.05F, -5.68F, -4.54F, 8.10F, 0.36F, 0.54F)
                // Front shroud, side rails, hook panels and vent slots.
                .texOffs(0, 32).addBox(-1.82F, -7.68F, -4.64F, 3.64F, 2.30F, 0.42F)
                .texOffs(10, 32).addBox(-1.18F, -7.30F, -4.90F, 2.36F, 1.45F, 0.26F)
                .texOffs(20, 32).addBox(-4.58F, -5.24F, -1.38F, 0.50F, 1.42F, 4.18F)
                .texOffs(20, 32).addBox(4.08F, -5.24F, -1.38F, 0.50F, 1.42F, 4.18F)
                .texOffs(30, 32).addBox(-4.76F, -4.90F, -0.88F, 0.26F, 0.22F, 3.18F)
                .texOffs(30, 32).addBox(4.50F, -4.90F, -0.88F, 0.26F, 0.22F, 3.18F)
                .texOffs(38, 32).addBox(-3.22F, -8.58F, -3.55F, 2.22F, 0.22F, 3.05F)
                .texOffs(38, 32).addBox(1.00F, -8.58F, -3.55F, 2.22F, 0.22F, 3.05F)
                .texOffs(48, 32).addBox(-3.00F, -6.92F, 3.94F, 6.00F, 0.92F, 0.26F)
                .texOffs(54, 32).addBox(-3.42F, -6.68F, -4.22F, 0.72F, 0.28F, 0.24F)
                .texOffs(54, 32).addBox(2.70F, -6.68F, -4.22F, 0.72F, 0.28F, 0.24F);
    }

    private static CubeListBuilder kiver() {
        return CubeListBuilder.create()
                // Cloth-covered padded crown with visible center and side panel seams.
                .texOffs(0, 0).addBox(-2.92F, -9.08F, -3.08F, 5.84F, 0.62F, 6.16F)
                .texOffs(14, 0).addBox(-3.60F, -8.46F, -3.70F, 7.20F, 0.82F, 7.40F)
                .texOffs(30, 0).addBox(-4.18F, -7.64F, -4.12F, 8.36F, 2.50F, 8.24F)
                .texOffs(0, 16).addBox(-0.15F, -9.18F, -4.02F, 0.30F, 0.20F, 8.04F)
                .texOffs(8, 16).addBox(-4.30F, -6.95F, -0.22F, 0.26F, 1.00F, 3.90F)
                .texOffs(8, 16).addBox(4.04F, -6.95F, -0.22F, 0.26F, 1.00F, 3.90F)
                // Thick ear flaps and rear skirt define the Kiver-M silhouette.
                .texOffs(16, 16).addBox(-4.62F, -5.25F, -1.72F, 0.92F, 5.10F, 5.52F)
                .texOffs(16, 16).addBox(3.70F, -5.25F, -1.72F, 0.92F, 5.10F, 5.52F)
                .texOffs(30, 16).addBox(-3.70F, -5.22F, 3.42F, 7.40F, 4.85F, 0.86F)
                .texOffs(46, 16).addBox(-4.08F, -5.46F, -4.54F, 8.16F, 0.54F, 0.52F)
                .texOffs(0, 32).addBox(-4.86F, -4.42F, 0.04F, 0.32F, 1.25F, 1.08F)
                .texOffs(0, 32).addBox(4.54F, -4.42F, 0.04F, 0.32F, 1.25F, 1.08F)
                .texOffs(6, 32).addBox(-3.02F, -1.22F, -4.68F, 6.04F, 0.95F, 0.54F);
    }

    private static CubeListBuilder achhc() {
        CubeListBuilder b = crown(CubeListBuilder.create());
        lowSides(b, 3.80F);
        b.texOffs(36, 32).addBox(-3.60F, -5.20F, -4.62F, 7.20F, 0.48F, 0.48F)
                .texOffs(0, 48).addBox(-4.62F, -3.55F, 0.40F, 0.54F, 2.55F, 3.10F)
                .texOffs(0, 48).addBox(4.08F, -3.55F, 0.40F, 0.54F, 2.55F, 3.10F);
        return chinStraps(b);
    }

    private static CubeListBuilder strike() {
        CubeListBuilder b = crown(CubeListBuilder.create());
        frontMount(b, -7.35F);
        earPads(b);
        b.texOffs(36, 32).addBox(-3.45F, -8.48F, -3.55F, 2.20F, 0.28F, 3.25F)
                .texOffs(36, 32).addBox(1.25F, -8.48F, -3.55F, 2.20F, 0.28F, 3.25F)
                .texOffs(46, 32).addBox(-3.25F, -6.95F, 3.95F, 6.50F, 1.25F, 0.32F);
        return chinStraps(b);
    }

    private static CubeListBuilder fastMt() {
        CubeListBuilder b = crown(CubeListBuilder.create());
        frontMount(b, -7.45F);
        sideRails(b, -5.05F);
        b.texOffs(0, 32).addBox(-4.60F, -4.00F, 1.20F, 0.54F, 2.20F, 2.65F)
                .texOffs(0, 32).addBox(4.06F, -4.00F, 1.20F, 0.54F, 2.20F, 2.65F)
                .texOffs(36, 32).addBox(-3.95F, -5.00F, -4.62F, 7.90F, 0.22F, 0.28F);
        return chinStraps(b);
    }

    private static CubeListBuilder airframe() {
        CubeListBuilder b = CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.55F, -9.00F, -3.55F, 7.10F, 0.70F, 7.10F)
                .texOffs(0, 8).addBox(-4.05F, -8.30F, -4.05F, 3.55F, 2.85F, 8.10F)
                .texOffs(0, 8).addBox(0.50F, -8.30F, -4.05F, 3.55F, 2.85F, 8.10F)
                .texOffs(0, 18).addBox(-4.30F, -5.45F, 1.30F, 1.10F, 2.80F, 2.85F)
                .texOffs(0, 18).addBox(3.20F, -5.45F, 1.30F, 1.10F, 2.80F, 2.85F);
        frontMount(b, -7.45F);
        sideRails(b, -5.30F);
        earPads(b);
        return chinStraps(b);
    }

    private static CubeListBuilder flux() {
        CubeListBuilder b = crown(CubeListBuilder.create());
        frontMount(b, -7.45F);
        sideRails(b, -5.05F);
        earPads(b);
        b.texOffs(36, 32).addBox(-3.15F, -8.48F, -4.15F, 2.20F, 0.28F, 3.10F)
                .texOffs(36, 32).addBox(0.95F, -8.48F, -4.15F, 2.20F, 0.28F, 3.10F)
                .texOffs(46, 32).addBox(-3.30F, -6.75F, 3.95F, 6.60F, 1.15F, 0.32F);
        return chinStraps(b);
    }
}
