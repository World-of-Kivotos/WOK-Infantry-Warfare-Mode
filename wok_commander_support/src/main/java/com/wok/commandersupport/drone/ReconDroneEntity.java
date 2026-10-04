package com.wok.commandersupport.drone;

import com.wok.commandersupport.WokCommanderSupportMod;
import com.wok.commandersupport.client.ReconDroneEngineSound;
import com.wok.commandersupport.registry.CommanderSupportSounds;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.battle.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.UUID;

/**
 * Fixed-wing recon drone that circles a support target while its mission scans for contacts.
 *
 * <p>The path is not simulated: server and clients compute it from the synchronised orbit, the
 * launch tick and the level game time ({@link ReconDroneFlight}), so the drone moves smoothly
 * without position packets. It only takes damage from blasts and Superb Warfare projectile hits
 * ({@link ReconDroneDamagePolicy}); at zero health it bursts, falls with its forward momentum
 * trailing smoke and disappears on impact or after {@link ReconDroneFlight#CRASH_MAX_TICKS}.
 * The wreck never breaks blocks, hurts entities or drops items.</p>
 *
 * <p>The drone is never saved (the entity type is {@code noSave}) and removes itself one
 * departure after its last mission step, so a battle reset or a vanished mission cannot leave it
 * behind. That expiry, and a drone that stopped being ticked at all (its chunk stayed loaded but
 * left the entity-ticking range), are also enforced from outside by {@link ReconDroneWatchdog},
 * which does not rely on this entity's own tick. A drone that was not launched by a mission (for
 * example summoned by command) removes itself on its first tick, or at the watchdog's next sweep
 * if it never ticks. No third-party class is referenced.</p>
 */
public class ReconDroneEntity extends Entity {
    public static final float MAX_HEALTH = 200.0F;
    /** Rendered up to this distance; the tracking range of the entity type still caps it. */
    public static final double RENDER_DISTANCE = 320.0D;
    private static final int UNSET_TICK = -1;
    private static final float DAMAGED_SMOKE_HEALTH = MAX_HEALTH * 0.5F;
    private static final double CRASH_PROBE_STEP = 0.5D;
    /** Distance from the entity position back to the pusher propeller, in blocks. */
    private static final double TAIL_OFFSET = 3.2D;
    /** Half the wingspan plus the wing-tip lights, for culling. */
    private static final double CULLING_HALF_SPAN = 6.5D;

    private static final EntityDataAccessor<Long> DATA_CENTER_X =
            SynchedEntityData.defineId(ReconDroneEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DATA_CENTER_Z =
            SynchedEntityData.defineId(ReconDroneEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Float> DATA_ORBIT_RADIUS =
            SynchedEntityData.defineId(ReconDroneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ALTITUDE =
            SynchedEntityData.defineId(ReconDroneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PHASE =
            SynchedEntityData.defineId(ReconDroneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_CLOCKWISE =
            SynchedEntityData.defineId(ReconDroneEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Long> DATA_LAUNCH_TIME =
            SynchedEntityData.defineId(ReconDroneEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Float> DATA_HEIGHT_ABOVE_TARGET =
            SynchedEntityData.defineId(ReconDroneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEALTH =
            SynchedEntityData.defineId(ReconDroneEntity.class, EntityDataSerializers.FLOAT);
    /** Ticks after launch at which the drone left station, or -1. */
    private static final EntityDataAccessor<Integer> DATA_DEPART_TICK =
            SynchedEntityData.defineId(ReconDroneEntity.class, EntityDataSerializers.INT);
    /** Ticks after launch at which the drone was shot down, or -1. */
    private static final EntityDataAccessor<Integer> DATA_CRASH_TICK =
            SynchedEntityData.defineId(ReconDroneEntity.class, EntityDataSerializers.INT);

    // Server only.
    private Faction faction;
    private long expireGameTime = Long.MIN_VALUE;
    /** Game time of the last server tick, read by {@link ReconDroneWatchdog}. */
    private long lastTickedGameTime = Long.MIN_VALUE;
    private ReconDroneSortie sortie;

    // Both sides: flight clock of the last two ticks, interpolated for rendering.
    private double flightTicks;
    private double flightTicksO;
    private boolean flightClockStarted;
    // Client only: where the falling wreck met the ground, and when.
    private double[] clientImpact;
    private double clientImpactTicks;

    public ReconDroneEntity(EntityType<? extends ReconDroneEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_CENTER_X, 0L);
        entityData.define(DATA_CENTER_Z, 0L);
        entityData.define(DATA_ORBIT_RADIUS, 0.0F);
        entityData.define(DATA_ALTITUDE, 0.0F);
        entityData.define(DATA_PHASE, 0.0F);
        entityData.define(DATA_CLOCKWISE, true);
        entityData.define(DATA_LAUNCH_TIME, -1L);
        entityData.define(DATA_HEIGHT_ABOVE_TARGET, (float) ReconDroneFlight.HEIGHT_ABOVE_TARGET);
        entityData.define(DATA_HEALTH, MAX_HEALTH);
        entityData.define(DATA_DEPART_TICK, UNSET_TICK);
        entityData.define(DATA_CRASH_TICK, UNSET_TICK);
    }

    /**
     * Server side, before the drone is added to the level: fixes its orbit and the mission it
     * reports to. The position is placed on the orbit at launch.
     */
    void launch(ReconDroneFlight.Orbit orbit, double heightAboveTarget, long launchGameTime,
                long expireGameTime, Faction faction, ReconDroneSortie sortie) {
        entityData.set(DATA_CENTER_X, Double.doubleToRawLongBits(orbit.centerX()));
        entityData.set(DATA_CENTER_Z, Double.doubleToRawLongBits(orbit.centerZ()));
        entityData.set(DATA_ORBIT_RADIUS, (float) orbit.radius());
        entityData.set(DATA_ALTITUDE, (float) orbit.altitude());
        entityData.set(DATA_PHASE, (float) orbit.phase());
        entityData.set(DATA_CLOCKWISE, orbit.clockwise());
        entityData.set(DATA_HEIGHT_ABOVE_TARGET, (float) heightAboveTarget);
        entityData.set(DATA_LAUNCH_TIME, launchGameTime);
        this.faction = faction;
        this.expireGameTime = expireGameTime;
        this.lastTickedGameTime = launchGameTime;
        this.sortie = sortie;
        // Read the orbit back: clients only ever see the synchronised (float) values.
        ReconDroneFlight.Orbit synced = orbit();
        ReconDroneFlight.Pose start = ReconDroneFlight.orbitPose(
                synced == null ? orbit : synced, 0.0D);
        moveTo(start.x(), start.y(), start.z(), start.yawDegrees(), 0.0F);
        flightTicks = 0.0D;
        flightTicksO = 0.0D;
        flightClockStarted = true;
    }

    /** The synchronised orbit, or null for a drone that no mission launched. */
    public ReconDroneFlight.Orbit orbit() {
        float radius = entityData.get(DATA_ORBIT_RADIUS);
        if (!(radius > 0.0F) || entityData.get(DATA_LAUNCH_TIME) < 0L) {
            return null;
        }
        double centerX = Double.longBitsToDouble(entityData.get(DATA_CENTER_X));
        double centerZ = Double.longBitsToDouble(entityData.get(DATA_CENTER_Z));
        float altitude = entityData.get(DATA_ALTITUDE);
        float phase = entityData.get(DATA_PHASE);
        if (!Double.isFinite(centerX) || !Double.isFinite(centerZ)
                || !Float.isFinite(altitude) || !Float.isFinite(phase)) {
            return null;
        }
        return new ReconDroneFlight.Orbit(centerX, centerZ, radius, altitude, phase,
                entityData.get(DATA_CLOCKWISE));
    }

    @Override
    public void tick() {
        // The vanilla base tick is skipped on purpose: the path is fixed, and its fluid and
        // portal checks would only read blocks around a drone that never touches them.
        ReconDroneFlight.Orbit orbit = orbit();
        if (orbit == null) {
            if (!level().isClientSide()) {
                discard();
            }
            return;
        }
        long now = level().getGameTime();
        double ticks = Math.max(0.0D, now - entityData.get(DATA_LAUNCH_TIME));
        if (!level().isClientSide()) {
            lastTickedGameTime = now;
            if (expired(now, ticks)) {
                discard();
                return;
            }
        }
        advanceFlightClock(ticks);
        if (isCrashing()) {
            tickCrash(orbit, ticks);
        } else {
            ReconDroneFlight.Pose pose = ReconDroneFlight.flightPose(orbit, departTick(), ticks);
            if (leavesSimulation(pose)) {
                // A drone that stopped ticking would hang in the air while clients keep flying
                // it; its mission reports the lost signal at the next step.
                WokCommanderSupportMod.LOGGER.info(
                        "Recon drone {} left the simulated area and was removed", getUUID());
                discard();
                return;
            }
            setPos(pose.x(), pose.y(), pose.z());
            setYRot(pose.yawDegrees());
        }
        if (level().isClientSide() && !isRemoved()) {
            emitClientEffects();
        }
        firstTick = false;
    }

    /** A falling wreck always ends on its own; otherwise the drone outlives no mission. */
    private boolean expired(long now, double ticks) {
        if (isCrashing()) {
            return false;
        }
        int departTick = departTick();
        return now >= expireGameTime
                || departTick >= 0 && ticks - departTick >= ReconDroneFlight.DEPARTURE_TICKS;
    }

    /**
     * Server side, for {@link ReconDroneWatchdog}: past the expiry tick (a falling wreck
     * excepted), or not ticked by the server for longer than the watchdog's grace.
     */
    boolean overdue(long now) {
        return ReconDroneWatchdog.overdue(now, lastTickedGameTime, expireGameTime, isCrashing());
    }

    private void advanceFlightClock(double ticks) {
        flightTicksO = flightClockStarted ? flightTicks : ticks;
        flightTicks = ticks;
        flightClockStarted = true;
    }

    private void tickCrash(ReconDroneFlight.Orbit orbit, double ticks) {
        int crashTick = crashTick();
        int elapsed = (int) Math.min(ReconDroneFlight.CRASH_MAX_TICKS,
                Math.max(0.0D, Math.floor(ticks) - crashTick));
        ReconDroneFlight.Pose start = ReconDroneFlight.flightPose(orbit, departTick(), crashTick);
        ReconDroneFlight.Pose previous = ReconDroneFlight.crashPose(start, elapsed - 1);
        ReconDroneFlight.Pose current = ReconDroneFlight.crashPose(start, elapsed);
        if (level().isClientSide()) {
            if (clientImpact == null) {
                clientImpact = firstObstacle(previous, current);
                if (clientImpact != null) {
                    clientImpactTicks = ticks;
                }
            }
            if (clientImpact != null) {
                // Rest at the impact point until the server removes the wreck.
                setPos(clientImpact[0], clientImpact[1], clientImpact[2]);
                return;
            }
        } else {
            double[] impact = firstObstacle(previous, current);
            if (impact != null) {
                finishCrash(impact[0], impact[1], impact[2]);
                return;
            }
            if (elapsed >= ReconDroneFlight.CRASH_MAX_TICKS || leavesSimulation(current)) {
                finishCrash(current.x(), current.y(), current.z());
                return;
            }
        }
        setPos(current.x(), current.y(), current.z());
        setYRot(current.yawDegrees());
    }

    /** Server side: the next position lies in a chunk that no longer ticks entities. */
    private boolean leavesSimulation(ReconDroneFlight.Pose pose) {
        return level() instanceof ServerLevel server
                && !server.isPositionEntityTicking(BlockPos.containing(pose.x(), pose.y(),
                pose.z()));
    }

    /** First blocked point between two crash positions, sampled every half block, or null. */
    private double[] firstObstacle(ReconDroneFlight.Pose from, ReconDroneFlight.Pose to) {
        double dx = to.x() - from.x();
        double dy = to.y() - from.y();
        double dz = to.z() - from.z();
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int samples = Math.max(1, (int) Math.ceil(length / CRASH_PROBE_STEP));
        for (int sample = 1; sample <= samples; sample++) {
            double fraction = (double) sample / samples;
            double x = from.x() + dx * fraction;
            double y = from.y() + dy * fraction;
            double z = from.z() + dz * fraction;
            if (blocked(x, y, z)) {
                return new double[]{x, y, z};
            }
        }
        return null;
    }

    /**
     * Solid ground, fluid, the bottom of the world or a column that is not loaded. A missing
     * chunk counts as an impact because looking it up would load it; nothing here waits for a
     * chunk.
     */
    private boolean blocked(double x, double y, double z) {
        Level level = level();
        if (y < level.getMinBuildHeight()) {
            return true;
        }
        if (y >= level.getMaxBuildHeight()) {
            return false;
        }
        BlockPos pos = BlockPos.containing(x, y, z);
        LevelChunk chunk = level.getChunkSource().getChunkNow(
                SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getZ()));
        if (chunk == null) {
            return true;
        }
        BlockState state = chunk.getBlockState(pos);
        return !state.getCollisionShape(chunk, pos).isEmpty()
                || !state.getFluidState().isEmpty();
    }

    private void finishCrash(double x, double y, double z) {
        if (level() instanceof ServerLevel server) {
            impactEffects(server, x, y + 0.5D, z, getUUID());
        }
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide() || isRemoved() || source == null
                || isInvulnerableTo(source)) {
            return false;
        }
        boolean explosion = source.is(DamageTypeTags.IS_EXPLOSION);
        ResourceLocation damageType = source.typeHolder().unwrapKey()
                .map(ResourceKey::location).orElse(null);
        if (!ReconDroneDamagePolicy.accepts(explosion, damageType, amount, isCrashing(),
                faction, attackerFaction(source.getEntity()))) {
            return false;
        }
        float health = Math.max(0.0F, getHealth() - amount);
        entityData.set(DATA_HEALTH, health);
        if (health <= 0.0F) {
            shootDown();
        } else if (level() instanceof ServerLevel server) {
            hitEffects(server, getX(), getY() + ReconDroneAirframe.MODEL_CENTER_HEIGHT, getZ(),
                    getUUID());
        }
        return true;
    }

    /** Faction of the attacking player (directly or piloting a vehicle), or null. */
    private Faction attackerFaction(Entity attacker) {
        Player player = null;
        if (attacker instanceof Player direct) {
            player = direct;
        } else if (attacker != null) {
            LivingEntity pilot = attacker.getControllingPassenger();
            if (pilot instanceof Player controlling) {
                player = controlling;
            }
        }
        if (player == null || !(level() instanceof ServerLevel server)) {
            return null;
        }
        try {
            UUID playerId = player.getUUID();
            return BattleService.get(server.getServer())
                    .flatMap(battle -> battle.factionOf(playerId))
                    .orElse(null);
        } catch (RuntimeException failure) {
            WokCommanderSupportMod.LOGGER.warn(
                    "Recon drone {} could not resolve the faction of attacker {}",
                    getUUID(), player.getUUID(), failure);
            return null;
        }
    }

    private void shootDown() {
        long elapsed = Math.max(0L, level().getGameTime() - entityData.get(DATA_LAUNCH_TIME));
        entityData.set(DATA_HEALTH, 0.0F);
        entityData.set(DATA_CRASH_TICK, (int) Math.min(Integer.MAX_VALUE, elapsed));
        if (sortie != null) {
            sortie.markShotDown();
        }
        if (level() instanceof ServerLevel server) {
            airBurstEffects(server, getX(), getY() + ReconDroneAirframe.MODEL_CENTER_HEIGHT,
                    getZ(), getUUID());
        }
        WokCommanderSupportMod.LOGGER.info("Recon drone {} was shot down", getUUID());
    }

    /** Server side, at the last mission step: roll out of the orbit and fly away. */
    void depart() {
        long launchTime = entityData.get(DATA_LAUNCH_TIME);
        if (isRemoved() || isCrashing() || departTick() >= 0 || launchTime < 0L) {
            return;
        }
        long elapsed = Math.max(0L, level().getGameTime() - launchTime);
        entityData.set(DATA_DEPART_TICK, (int) Math.min(Integer.MAX_VALUE, elapsed));
    }

    public boolean isCrashing() {
        return crashTick() >= 0;
    }

    public float getHealth() {
        return entityData.get(DATA_HEALTH);
    }

    public boolean orbitsClockwise() {
        return entityData.get(DATA_CLOCKWISE);
    }

    /** Flight height over the target surface; the engine is heard this far plus a margin. */
    public double heightAboveTarget() {
        return entityData.get(DATA_HEIGHT_ABOVE_TARGET);
    }

    private int departTick() {
        return entityData.get(DATA_DEPART_TICK);
    }

    private int crashTick() {
        return entityData.get(DATA_CRASH_TICK);
    }

    /** Flight clock interpolated between the last two ticks, in ticks since launch. */
    public double renderFlightTicks(float partialTick) {
        return Mth.lerp(partialTick, flightTicksO, flightTicks);
    }

    public float renderYaw(float partialTick) {
        return Mth.rotLerp(partialTick, yRotO, getYRot());
    }

    /** Bank in degrees, positive with the right wing down. */
    public float renderBank(float partialTick) {
        ReconDroneFlight.Orbit orbit = orbit();
        return orbit == null ? 0.0F : ReconDroneFlight.bankDegrees(orbit, departTick(),
                crashTick(), attitudeTicks(partialTick));
    }

    /** Pitch in degrees, positive nose down. */
    public float renderPitchDown(float partialTick) {
        return orbit() == null ? 0.0F : ReconDroneFlight.pitchDownDegrees(departTick(),
                crashTick(), attitudeTicks(partialTick));
    }

    /** Ticks since the shoot-down, or -1 while the engine still runs. */
    public double crashTicks(float partialTick) {
        int crashTick = crashTick();
        return crashTick < 0 ? -1.0D : Math.max(0.0D, renderFlightTicks(partialTick) - crashTick);
    }

    /** A wreck resting on the ground stops rolling. */
    private double attitudeTicks(float partialTick) {
        double ticks = renderFlightTicks(partialTick);
        return clientImpact == null ? ticks : Math.min(ticks, clientImpactTicks);
    }

    private void emitClientEffects() {
        if (isCrashing()) {
            if (clientImpact != null) {
                return;
            }
            double[] tail = tailPosition();
            Level level = level();
            level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true,
                    tail[0], tail[1], tail[2], 0.0D, 0.03D, 0.0D);
            for (int puff = 0; puff < 2; puff++) {
                level.addAlwaysVisibleParticle(ParticleTypes.LARGE_SMOKE, true,
                        tail[0] + jitter(0.4D), tail[1] + jitter(0.3D), tail[2] + jitter(0.4D),
                        0.0D, 0.02D, 0.0D);
            }
            if (random.nextBoolean()) {
                level.addAlwaysVisibleParticle(ParticleTypes.FLAME, true,
                        tail[0] + jitter(0.3D), tail[1], tail[2] + jitter(0.3D),
                        0.0D, 0.01D, 0.0D);
            }
        } else if (getHealth() < DAMAGED_SMOKE_HEALTH && tickCount % 2 == 0) {
            double[] tail = tailPosition();
            level().addAlwaysVisibleParticle(ParticleTypes.LARGE_SMOKE, true,
                    tail[0] + jitter(0.2D), tail[1], tail[2] + jitter(0.2D),
                    0.0D, 0.015D, 0.0D);
        }
    }

    /** Pusher propeller position behind the fuselage. */
    private double[] tailPosition() {
        float yawRadians = getYRot() * Mth.DEG_TO_RAD;
        return new double[]{getX() + Mth.sin(yawRadians) * TAIL_OFFSET,
                getY() + ReconDroneAirframe.MODEL_CENTER_HEIGHT,
                getZ() - Mth.cos(yawRadians) * TAIL_OFFSET};
    }

    private double jitter(double spread) {
        return (random.nextDouble() - 0.5D) * 2.0D * spread;
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (level().isClientSide()) {
            // The engine loop is client-only; the server never loads the sound class.
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ReconDroneEngineSound.start(this));
        } else {
            // Expiry and stalls are enforced even when this drone is no longer ticked.
            ReconDroneWatchdog.watch(this);
        }
    }

    /**
     * A launched drone follows its deterministic path on the client too; the server's quantised
     * position updates would only make it stutter between computed positions.
     */
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps,
                       boolean teleport) {
        if (orbit() == null) {
            super.lerpTo(x, y, z, yRot, xRot, steps, teleport);
        }
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSquared) {
        double range = RENDER_DISTANCE * getViewScale();
        return distanceSquared < range * range;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(CULLING_HALF_SPAN, 2.0D, CULLING_HALF_SPAN);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Health")) {
            entityData.set(DATA_HEALTH, Mth.clamp(tag.getFloat("Health"), 0.0F, MAX_HEALTH));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Health", getHealth());
    }

    // ---- cosmetic server effects: failures are logged and never change the outcome -------

    private static void hitEffects(ServerLevel level, double x, double y, double z,
                                   UUID droneId) {
        cosmetic(droneId, "hit", () ->
                broadcastParticles(level, ParticleTypes.LARGE_SMOKE, x, y, z, 4, 0.6D, 0.02D));
    }

    private static void airBurstEffects(ServerLevel level, double x, double y, double z,
                                        UUID droneId) {
        cosmetic(droneId, "shoot-down", () -> {
            broadcastParticles(level, ParticleTypes.EXPLOSION, x, y, z, 3, 1.2D, 0.0D);
            broadcastParticles(level, ParticleTypes.FLAME, x, y, z, 16, 0.8D, 0.06D);
            broadcastParticles(level, ParticleTypes.LARGE_SMOKE, x, y, z, 14, 1.0D, 0.04D);
            level.playSound(null, x, y, z, CommanderSupportSounds.RECON_DRONE_DESTROYED.get(),
                    SoundSource.HOSTILE, 1.0F, 1.0F);
        });
    }

    private static void impactEffects(ServerLevel level, double x, double y, double z,
                                      UUID droneId) {
        cosmetic(droneId, "impact", () -> {
            broadcastParticles(level, ParticleTypes.EXPLOSION, x, y, z, 4, 1.0D, 0.0D);
            broadcastParticles(level, ParticleTypes.LAVA, x, y, z, 6, 0.6D, 0.0D);
            broadcastParticles(level, ParticleTypes.LARGE_SMOKE, x, y, z, 20, 1.2D, 0.05D);
            broadcastParticles(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 6, 0.8D,
                    0.01D);
            level.playSound(null, x, y, z, CommanderSupportSounds.RECON_DRONE_DESTROYED.get(),
                    SoundSource.HOSTILE, 0.8F, 0.8F);
        });
    }

    /** Long-distance particles: the drone flies far above the 32-block default cut-off. */
    private static void broadcastParticles(ServerLevel level, ParticleOptions type,
                                           double x, double y, double z, int count,
                                           double spread, double speed) {
        for (ServerPlayer player : level.players()) {
            level.sendParticles(player, type, true, x, y, z, count, spread, spread, spread,
                    speed);
        }
    }

    private static void cosmetic(UUID droneId, String stage, Runnable effect) {
        try {
            effect.run();
        } catch (RuntimeException | LinkageError failure) {
            WokCommanderSupportMod.LOGGER.warn("Recon drone {} skipped its {} effects",
                    droneId, stage, failure);
        }
    }
}
