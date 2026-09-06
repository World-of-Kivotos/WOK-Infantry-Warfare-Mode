package com.wok.commandersupport.airstrike;

import com.wok.infantry.battle.BattleService;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.support.SupportTarget;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

/** Server-authoritative continuous designations from Superb Warfare's indicator item. */
final class ArtilleryIndicatorDesignation {
    static final String MOD_ID = "superbwarfare";
    static final ResourceLocation ITEM_ID = ResourceLocation.fromNamespaceAndPath(
            MOD_ID, "artillery_indicator");
    static final double MAX_RANGE = 512.0D;

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
        if (level == null || playerId == null || faction == null) {
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
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 end = start.add(player.getViewVector(1.0F).scale(MAX_RANGE));
        BlockHitResult blockHit = player.serverLevel().clip(new ClipContext(start,
                end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        Vec3 clippedEnd = blockHit.getLocation();
        double blockDistanceSquared = start.distanceToSqr(clippedEnd);
        AABB searchBox = player.getBoundingBox().expandTowards(
                clippedEnd.subtract(start)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player,
                start, clippedEnd, searchBox,
                entity -> validRayTarget(player, entity), blockDistanceSquared);
        Vec3 position = entityHit == null ? clippedEnd : entityHit.getLocation();
        if (!validPosition(player.serverLevel(), position, authorizedArea,
                authorizedRadius)) {
            return Optional.empty();
        }
        return Optional.of(new Designation(player.getUUID(),
                player.getGameProfile().getName(), position));
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
                        net.minecraft.core.BlockPos.containing(position))) {
            return false;
        }
        return level.hasChunkAt(net.minecraft.core.BlockPos.containing(position));
    }

    private static double horizontalDistanceSquared(Vec3 position,
                                                    double x, double z) {
        double dx = position.x - x;
        double dz = position.z - z;
        return dx * dx + dz * dz;
    }

    record Designation(UUID playerId, String playerName, Vec3 position) {
    }
}
