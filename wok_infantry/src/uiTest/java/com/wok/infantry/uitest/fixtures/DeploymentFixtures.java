package com.wok.infantry.uitest.fixtures;

import com.wok.infantry.deployment.DeploymentPoint;
import com.wok.infantry.deployment.KitProvenance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.UUID;

/** Server-side deployment fixtures of the live flow (run on the server thread). */
public final class DeploymentFixtures {
    private DeploymentFixtures() {
    }

    /**
     * The UI seed is copied from the latest GameTest world and therefore must not assume that the
     * shared spawn still has solid terrain. Builds a tiny deterministic platform in the isolated
     * copy so the acceptance validates the real safe-spawn check instead of stale geometry.
     */
    public static BlockPos prepareSafeBase(ServerLevel level) {
        BlockPos sharedSpawn = level.getSharedSpawnPos();
        int minimumFeetY = level.getMinBuildHeight() + 2;
        int maximumFeetY = level.getMaxBuildHeight() - 2;
        // The copied GameTest seed can place shared spawn below sea level. Clearing only two
        // water blocks creates a momentary air pocket that refills before the deploy packet is
        // processed. Put the deterministic fixture above sea level so the second safety scan
        // validates stable terrain rather than a transient fluid update.
        int requestedFeetY = Math.max(sharedSpawn.getY(), level.getSeaLevel() + 4);
        int feetY = Math.max(minimumFeetY, Math.min(maximumFeetY, requestedFeetY));
        BlockPos feet = new BlockPos(sharedSpawn.getX(), feetY, sharedSpawn.getZ());
        level.getChunkAt(feet);
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos column = feet.offset(x, 0, z);
                level.setBlockAndUpdate(column.below(), Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(column, Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(column.above(), Blocks.AIR.defaultBlockState());
            }
        }
        return feet;
    }

    /** Blocks and fluids at a deployment point, for the result file. */
    public static String describe(MinecraftServer server, DeploymentPoint point) {
        ServerLevel level = server.getLevel(Level.OVERWORLD);
        if (level == null || !Level.OVERWORLD.location().equals(point.dimension())) {
            return "ERROR:unexpectedDimension=" + point.dimension();
        }
        BlockPos feet = point.position();
        BlockPos floor = feet.below();
        return "floor=" + level.getBlockState(floor)
                + ",feet=" + level.getBlockState(feet)
                + ",head=" + level.getBlockState(feet.above())
                + ",floorFluid=" + level.getFluidState(floor)
                + ",feetFluid=" + level.getFluidState(feet)
                + ",headFluid=" + level.getFluidState(feet.above());
    }

    /** Puts personal items on the body and a sentinel into an emptied Ender Chest. */
    public static String seedInventory(MinecraftServer server, UUID playerId) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            return "ERROR: deployment inventory fixture player is offline";
        }
        player.getInventory().setItem(20, new ItemStack(Items.DIRT, 7));
        player.getInventory().setItem(36, new ItemStack(Items.IRON_BOOTS));
        player.getInventory().setItem(40, new ItemStack(Items.STICK, 3));
        player.getEnderChestInventory().clearContent();
        player.getEnderChestInventory().setItem(0, new ItemStack(Items.DIAMOND, 2));
        player.getInventory().setChanged();
        player.getEnderChestInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        return "OK: seeded body items and an isolated Ender Chest sentinel";
    }

    /** Body items are gone after deploying and the Ender Chest is untouched. */
    public static String verifyInventoryPolicy(MinecraftServer server, UUID playerId) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            return "ERROR: deployment inventory verification player is offline";
        }
        for (int slot : new int[]{20, 36, 40}) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && !KitProvenance.isIssued(stack)) {
                return "ERROR: personal body item survived deployment at inventory slot " + slot;
            }
        }
        ItemStack sentinel = player.getEnderChestInventory().getItem(0);
        if (!sentinel.is(Items.DIAMOND) || sentinel.getCount() != 2) {
            return "ERROR: deployment changed the Ender Chest sentinel";
        }
        for (int slot = 1; slot < player.getEnderChestInventory().getContainerSize(); slot++) {
            if (!player.getEnderChestInventory().getItem(slot).isEmpty()) {
                return "ERROR: deployment inserted a body item into Ender Chest slot " + slot;
            }
        }
        return "OK: body items cleared and Ender Chest remained untouched";
    }
}
