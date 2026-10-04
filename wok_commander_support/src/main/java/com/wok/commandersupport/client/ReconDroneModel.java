package com.wok.commandersupport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wok.commandersupport.WokCommanderSupportMod;
import com.wok.commandersupport.drone.ReconDroneAirframe;
import com.wok.commandersupport.drone.ReconDroneEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Hand-built vanilla model of the recon drone, generated from {@link ReconDroneAirframe}. The
 * propeller spins, the electro-optical ball looks down into the turn towards the target, and
 * the wing-tip, strobe and beacon lights are drawn in a separate full-bright pass.
 */
public class ReconDroneModel extends EntityModel<ReconDroneEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(WokCommanderSupportMod.MOD_ID, "recon_drone"),
            "main");
    private static final float PROPELLER_RADIANS_PER_TICK = 1.3F;
    /** The sensor ball looks this far below the wing plane, into the orbit. */
    private static final float SENSOR_DEPRESSION_RADIANS = 0.85F;

    private final List<ModelPart> litParts = new ArrayList<>();
    private final ModelPart propeller;
    private final ModelPart sensor;
    private final ModelPart navPort;
    private final ModelPart navStarboard;
    private final ModelPart strobe;
    private final ModelPart beacon;

    public ReconDroneModel(ModelPart root) {
        for (String name : ReconDroneAirframe.LIT_PARTS) {
            litParts.add(root.getChild(name));
        }
        this.propeller = root.getChild(ReconDroneAirframe.PROPELLER);
        this.sensor = root.getChild(ReconDroneAirframe.SENSOR);
        this.navPort = root.getChild(ReconDroneAirframe.NAV_PORT);
        this.navStarboard = root.getChild(ReconDroneAirframe.NAV_STARBOARD);
        this.strobe = root.getChild(ReconDroneAirframe.STROBE);
        this.beacon = root.getChild(ReconDroneAirframe.BEACON);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (ReconDroneAirframe.Part part : ReconDroneAirframe.parts()) {
            CubeListBuilder cubes = CubeListBuilder.create();
            for (ReconDroneAirframe.Box box : part.boxes()) {
                cubes.texOffs(box.u(), box.v()).mirror(box.mirror())
                        .addBox(box.x(), box.y(), box.z(),
                                box.width(), box.height(), box.depth());
            }
            root.addOrReplaceChild(part.name(), cubes, PartPose.offsetAndRotation(
                    part.pivotX(), part.pivotY(), part.pivotZ(),
                    part.xRot(), part.yRot(), part.zRot()));
        }
        return LayerDefinition.create(mesh, ReconDroneAirframe.TEXTURE_WIDTH,
                ReconDroneAirframe.TEXTURE_HEIGHT);
    }

    @Override
    public void setupAnim(ReconDroneEntity drone, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // A wreck's propeller windmills to a stop instead of spinning at full power.
        float spin = drone.isCrashing() ? 0.35F : 1.0F;
        propeller.zRot = (ageInTicks * PROPELLER_RADIANS_PER_TICK * spin) % Mth.TWO_PI;
        // Model -X is the starboard side: a clockwise orbit turns right, towards the target.
        sensor.yRot = (drone.orbitsClockwise() ? 1.0F : -1.0F) * Mth.HALF_PI;
        sensor.xRot = SENSOR_DEPRESSION_RADIANS;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                               int packedOverlay, float red, float green, float blue,
                               float alpha) {
        for (ModelPart part : litParts) {
            part.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        }
    }

    /**
     * Steady red and green wing-tip lights plus the flashing strobe and beacon, full bright
     * while lit. A shot-down drone's lights go dark and take the world light instead.
     */
    public void renderLights(PoseStack poseStack, VertexConsumer consumer, int worldLight,
                             int fullBright, int packedOverlay, float ageInTicks,
                             boolean powered) {
        int steady = powered ? fullBright : worldLight;
        navPort.render(poseStack, consumer, steady, packedOverlay);
        navStarboard.render(poseStack, consumer, steady, packedOverlay);
        strobe.render(poseStack, consumer,
                powered && ReconDroneAirframe.strobeOn(ageInTicks) ? fullBright : worldLight,
                packedOverlay);
        beacon.render(poseStack, consumer,
                powered && ReconDroneAirframe.beaconOn(ageInTicks) ? fullBright : worldLight,
                packedOverlay);
    }
}
