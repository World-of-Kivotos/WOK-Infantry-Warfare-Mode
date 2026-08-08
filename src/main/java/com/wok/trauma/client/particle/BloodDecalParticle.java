package com.wok.trauma.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class BloodDecalParticle extends TextureSheetParticle {
    private final float rotation;

    private BloodDecalParticle(ClientLevel level, double x, double y, double z,
                               SpriteSet sprites, float size, int minLifetime, int maxLifetime) {
        super(level, x, y, z);
        this.pickSprite(sprites);
        this.quadSize = size * (0.82F + random.nextFloat() * 0.36F);
        this.rotation = random.nextFloat() * Mth.TWO_PI;
        this.lifetime = minLifetime + random.nextInt(maxLifetime - minLifetime + 1);
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.rCol = 0.68F + random.nextFloat() * 0.12F;
        this.gCol = 0.025F;
        this.bCol = 0.035F;
        this.alpha = 0.92F;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        Vec3 cameraPosition = camera.getPosition();
        float x = (float) (Mth.lerp(partialTick, xo, this.x) - cameraPosition.x());
        float y = (float) (Mth.lerp(partialTick, yo, this.y) - cameraPosition.y());
        float z = (float) (Mth.lerp(partialTick, zo, this.z) - cameraPosition.z());
        float size = getQuadSize(partialTick);
        Quaternionf rotationQuaternion = new Quaternionf().rotationY(rotation);

        Vector3f[] vertices = {
                new Vector3f(-1.0F, 0.0F, -1.0F),
                new Vector3f(-1.0F, 0.0F, 1.0F),
                new Vector3f(1.0F, 0.0F, 1.0F),
                new Vector3f(1.0F, 0.0F, -1.0F)
        };
        for (Vector3f vertex : vertices) {
            vertex.rotate(rotationQuaternion);
            vertex.mul(size);
            vertex.add(x, y, z);
        }

        float fadeStart = Math.max(0.0F, lifetime - 200.0F);
        float fade = age <= fadeStart ? 1.0F
                : 1.0F - (age - fadeStart) / Math.max(1.0F, lifetime - fadeStart);
        int light = getLightColor(partialTick);

        buffer.vertex(vertices[0].x(), vertices[0].y(), vertices[0].z())
                .uv(getU1(), getV1()).color(rCol, gCol, bCol, alpha * fade).uv2(light).endVertex();
        buffer.vertex(vertices[1].x(), vertices[1].y(), vertices[1].z())
                .uv(getU1(), getV0()).color(rCol, gCol, bCol, alpha * fade).uv2(light).endVertex();
        buffer.vertex(vertices[2].x(), vertices[2].y(), vertices[2].z())
                .uv(getU0(), getV0()).color(rCol, gCol, bCol, alpha * fade).uv2(light).endVertex();
        buffer.vertex(vertices[3].x(), vertices[3].y(), vertices[3].z())
                .uv(getU0(), getV1()).color(rCol, gCol, bCol, alpha * fade).uv2(light).endVertex();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final float size;
        private final int minLifetime;
        private final int maxLifetime;

        public Provider(SpriteSet sprites, float size, int minLifetime, int maxLifetime) {
            this.sprites = sprites;
            this.size = size;
            this.minLifetime = minLifetime;
            this.maxLifetime = maxLifetime;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ) {
            return new BloodDecalParticle(
                    level, x, y, z, sprites, size, minLifetime, maxLifetime);
        }
    }
}
