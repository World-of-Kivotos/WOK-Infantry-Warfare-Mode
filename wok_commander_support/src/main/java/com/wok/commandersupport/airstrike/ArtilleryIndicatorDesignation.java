package com.wok.commandersupport.airstrike;

import com.wok.infantry.battle.BattleService;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.support.SupportTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

/**
 * Server-authoritative continuous designations from Superb Warfare's indicator item.
 *
 * <p>The laser never loads chunks: its horizontal travel is capped just beyond the authorized
 * area, and it stops at the first block whose chunk is not loaded. Only a real block strike or an
 * entity in front of the ray is a spot; open sky and a ray cut by an unloaded chunk are not.</p>
 */
final class ArtilleryIndicatorDesignation {
    static final String MOD_ID = "superbwarfare";
    static final ResourceLocation ITEM_ID = ResourceLocation.fromNamespaceAndPath(
            MOD_ID, "artillery_indicator");
    static final double MAX_RANGE = 512.0D;
    /** Horizontal slack past the far edge of the authorized area. */
    static final double RAY_MARGIN = 16.0D;

    private ArtilleryIndicatorDesignation() {
    }

    static boolean available() {
        return ModList.get().isLoaded(MOD_ID)
                && ForgeRegistries.ITEMS.containsKey(ITEM_ID);
    }

    static Optional<Designation> acquire(ServerLevel level, Faction faction,
                                         SupportTarget authorizedArea,
                                         double authorizedRadius) {
        if (level == null || faction == null || authorizedArea == null
                || !Double.isFinite(authorizedRadius) || authorizedRadius <= 0.0D) {
            return Optional.empty();
        }
        BattleService battle = BattleService.get(level.getServer()).orElse(null);
        if (battle == null) {
            return Optional.empty();
        }
        return level.players().stream()
                .filter(ArtilleryIndicatorDesignation::eligiblePlayer)
                .filter(player -> battle.factionOf(player.getUUID())
                        .filter(playerFaction -> playerFaction == faction).isPresent())
                .map(player -> designate(player, authorizedArea, authorizedRadius)
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .min(Comparator
                        .comparingDouble((Designation designation) -> horizontalDistanceSquared(
                                designation.position(), authorizedArea.startX(),
                                authorizedArea.startZ()))
                        .thenComparing(designation -> designation.playerId().toString()));
    }

    static Optional<Designation> update(ServerLevel level, UUID playerId,
                                        Faction faction,
                                        SupportTarget authorizedArea,
                                        double authorizedRadius) {
        if (level == null || playerId == null || faction == null || authorizedArea == null
                || !Double.isFinite(authorizedRadius) || authorizedRadius <= 0.0D) {
            return Optional.empty();
        }
        net.minecraft.world.entity.player.Player found = level.getPlayerByUUID(playerId);
        if (!(found instanceof ServerPlayer player) || player.serverLevel() != level
                || !eligiblePlayer(player)) {
            return Optional.empty();
        }
        return BattleService.get(level.getServer())
                .flatMap(battle -> battle.factionOf(playerId))
                .filter(playerFaction -> playerFaction == faction)
                .flatMap(ignored -> designate(player, authorizedArea,
                        authorizedRadius));
    }

    private static boolean eligiblePlayer(ServerPlayer player) {
        if (player == null || !player.isAlive() || player.isSpectator()
                || !player.isUsingItem()
                || player.getUsedItemHand() != InteractionHand.MAIN_HAND) {
            return false;
        }
        ItemStack used = player.getUseItem();
        return !used.isEmpty() && ITEM_ID.equals(ForgeRegistries.ITEMS.getKey(
                used.getItem()));
    }

    private static Optional<Designation> designate(ServerPlayer player,
                                                   SupportTarget authorizedArea,
                                                   double authorizedRadius) {
        ServerLevel level = player.serverLevel();
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 direction = player.getViewVector(1.0F);
        double length = rayLength(direction, horizontalReach(start.x, start.z,
                authorizedArea.startX(), authorizedArea.startZ(), authorizedRadius));
        if (!(length > 0.0D)) {
            return Optional.empty();
        }
        Vec3 end = start.add(direction.normalize().scale(length));
        BlockRay blockRay = clipLoaded(level, new ClipContext(start, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        Vec3 rayEnd = blockRay.end();
        AABB searchBox = player.getBoundingBox().expandTowards(
                rayEnd.subtract(start)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player,
                start, rayEnd, searchBox,
                entity -> validRayTarget(player, entity), start.distanceToSqr(rayEnd));
        Optional<Vec3> spot = laserSpot(blockRay,
                entityHit == null ? null : entityHit.getLocation());
        if (spot.isEmpty() || !validPosition(level, spot.get(), authorizedArea,
                authorizedRadius)) {
            return Optional.empty();
        }
        return Optional.of(new Designation(player.getUUID(),
                player.getGameProfile().getName(), spot.get()));
    }

    /**
     * Same as {@link BlockGetter#clip} with OUTLINE shapes and no fluids, except that it checks
     * every block's chunk first and stops at the first one that is not loaded instead of loading
     * it.
     */
    private static BlockRay clipLoaded(ServerLevel level, ClipContext clip) {
        Vec3 from = clip.getFrom();
        Vec3 to = clip.getTo();
        return BlockGetter.traverseBlocks(from, to, clip, (context, pos) -> {
            LevelChunk chunk = loadedChunk(level, pos);
            if (chunk == null) {
                return new BlockRay(RayEnd.UNLOADED,
                        new AABB(pos).clip(from, to).orElse(from));
            }
            // Read from the chunk already in hand: level.getBlockState would wait for a chunk
            // that is ticketed but still loading.
            BlockState state = chunk.getBlockState(pos);
            BlockHitResult hit = level.clipWithInteractionOverride(from, to, pos,
                    context.getBlockShape(state, level, pos), state);
            return hit == null ? null : new BlockRay(RayEnd.BLOCK, hit.getLocation());
        }, context -> new BlockRay(RayEnd.OPEN, to));
    }

    /**
     * Horizontal distance the laser may travel: the far edge of the authorized area seen from the
     * designator plus {@link #RAY_MARGIN}, never more than {@link #MAX_RANGE}. Invalid input
     * yields 0, meaning no ray.
     */
    static double horizontalReach(double fromX, double fromZ, double centerX,
                                  double centerZ, double authorizedRadius) {
        if (!Double.isFinite(authorizedRadius) || authorizedRadius <= 0.0D) {
            return 0.0D;
        }
        double dx = centerX - fromX;
        double dz = centerZ - fromZ;
        double reach = Math.sqrt(dx * dx + dz * dz) + authorizedRadius + RAY_MARGIN;
        return Double.isFinite(reach) ? Math.min(MAX_RANGE, reach) : 0.0D;
    }

    /**
     * Ray length along {@code direction} whose horizontal travel equals
     * {@code horizontalReach}. Looking steeply up or down stays in the same chunk columns, so the
     * length may grow up to {@link #MAX_RANGE} and still reach the ground under a high designator.
     * Invalid input yields 0, meaning no ray.
     */
    static double rayLength(Vec3 direction, double horizontalReach) {
        if (direction == null || !Double.isFinite(horizontalReach)
                || horizontalReach <= 0.0D) {
            return 0.0D;
        }
        double length = direction.length();
        if (!Double.isFinite(length) || length <= 0.0D) {
            return 0.0D;
        }
        double horizontalPerBlock = Math.sqrt(direction.x * direction.x
                + direction.z * direction.z) / length;
        if (horizontalPerBlock * MAX_RANGE <= horizontalReach) {
            return MAX_RANGE;
        }
        return horizontalReach / horizontalPerBlock;
    }

    /** An entity in front of the block ray wins; otherwise only a real block strike is a spot. */
    static Optional<Vec3> laserSpot(BlockRay blockRay, Vec3 entityHit) {
        if (entityHit != null) {
            return Optional.of(entityHit);
        }
        if (blockRay != null && blockRay.kind() == RayEnd.BLOCK) {
            return Optional.of(blockRay.end());
        }
        return Optional.empty();
    }

    private static boolean validRayTarget(ServerPlayer player, Entity entity) {
        return entity != player && entity.isAlive() && !entity.isSpectator()
                && entity.isPickable();
    }

    private static boolean validPosition(ServerLevel level, Vec3 position,
                                         SupportTarget area, double radius) {
        if (position == null || !Double.isFinite(position.x)
                || !Double.isFinite(position.y) || !Double.isFinite(position.z)
                || horizontalDistanceSquared(position, area.startX(), area.startZ())
                > radius * radius
                || !level.getWorldBorder().isWithinBounds(
                        BlockPos.containing(position))) {
            return false;
        }
        return chunkLoaded(level, BlockPos.containing(position));
    }

    /**
     * True only when the chunk column holding {@code pos} has finished loading; never loads it
     * and never waits. {@code ServerLevel.hasChunk} only checks the ticket level, so it also
     * accepts a chunk that is still being read or generated.
     */
    static boolean chunkLoaded(ServerLevel level, BlockPos pos) {
        return loadedChunk(level, pos) != null;
    }

    private static LevelChunk loadedChunk(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getZ()));
    }

    private static double horizontalDistanceSquared(Vec3 position,
                                                    double x, double z) {
        double dx = position.x - x;
        double dz = position.z - z;
        return dx * dx + dz * dz;
    }

    /** How the block part of the laser ended. */
    enum RayEnd {
        /** Struck a block outline. */
        BLOCK,
        /** Reached its length without striking a block. */
        OPEN,
        /** Reached a chunk that is not loaded and stopped at its edge. */
        UNLOADED
    }

    /** Where the block part of the laser ended; the entity search never looks past {@code end}. */
    record BlockRay(RayEnd kind, Vec3 end) {
    }

    record Designation(UUID playerId, String playerName, Vec3 position) {
    }
}
