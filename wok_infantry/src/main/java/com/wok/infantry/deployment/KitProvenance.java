package com.wok.infantry.deployment;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

/** Namespaced, server-authored provenance carried by every issued deployment stack. */
public final class KitProvenance {
    private static final String ROOT_TAG = "wok_infantry_kit";
    private static final String SESSION_TAG = "Session";
    private static final String OWNER_TAG = "Owner";
    private static final String ISSUE_TOKEN_TAG = "IssueToken";
    private static final String INVENTORY_SLOT_TAG = "InventorySlot";
    private static final String AMMO_RESERVE_LIMIT_TAG = "AmmoReserveLimit";
    private static final String TRANSPORT_CARGO_TAG = "TransportCargo";
    private static final int MAX_NESTED_TAG_DEPTH = 16;
    private static final int MAX_NESTED_TAG_NODES = 4_096;

    public enum PayloadScanResult {
        CLEAR,
        ISSUED,
        INDETERMINATE
    }

    private KitProvenance() {
    }

    public static void stamp(ItemStack stack, UUID sessionId, UUID ownerId, UUID issueToken,
                             int inventorySlot) {
        stamp(stack, sessionId, ownerId, issueToken, inventorySlot, 0);
    }

    public static void stamp(ItemStack stack, UUID sessionId, UUID ownerId, UUID issueToken,
                             int inventorySlot, int ammoReserveLimit) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(issueToken, "issueToken");
        if (inventorySlot < 0 || inventorySlot > 40) {
            throw new IllegalArgumentException(
                    "Deployment stack must be bound to a player-inventory slot");
        }
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Cannot stamp an empty deployment stack");
        }
        CompoundTag provenance = new CompoundTag();
        provenance.putUUID(SESSION_TAG, sessionId);
        provenance.putUUID(OWNER_TAG, ownerId);
        provenance.putUUID(ISSUE_TOKEN_TAG, issueToken);
        provenance.putInt(INVENTORY_SLOT_TAG, inventorySlot);
        writeAmmoReserveLimit(provenance, ammoReserveLimit);
        stack.getOrCreateTag().put(ROOT_TAG, provenance);
    }

    /**
     * Marks runtime logistics cargo as belonging to this life without binding it to one slot.
     * The player must be able to move the deployer into either hand before placing it, while all
     * ordinary combat-kit stacks remain fixed to their issued slots.
     */
    public static void stampTransportCargo(ItemStack stack, UUID sessionId, UUID ownerId,
                                           UUID issueToken) {
        stamp(stack, sessionId, ownerId, issueToken, 0);
        markTransportCargo(stack.getTag().getCompound(ROOT_TAG));
    }

    public static java.util.OptionalInt ammoReserveLimit(ItemStack stack) {
        if (!isIssued(stack)) {
            return java.util.OptionalInt.empty();
        }
        CompoundTag provenance = stack.getTag().getCompound(ROOT_TAG);
        return readAmmoReserveLimit(provenance);
    }

    static void writeAmmoReserveLimit(CompoundTag provenance, int ammoReserveLimit) {
        Objects.requireNonNull(provenance, "provenance");
        if (ammoReserveLimit > 0) {
            provenance.putInt(AMMO_RESERVE_LIMIT_TAG, ammoReserveLimit);
        }
    }

    static java.util.OptionalInt readAmmoReserveLimit(CompoundTag provenance) {
        if (provenance == null
                || !provenance.contains(AMMO_RESERVE_LIMIT_TAG, Tag.TAG_INT)) {
            return java.util.OptionalInt.empty();
        }
        int value = provenance.getInt(AMMO_RESERVE_LIMIT_TAG);
        return value > 0 ? java.util.OptionalInt.of(value) : java.util.OptionalInt.empty();
    }

    public static boolean isIssued(ItemStack stack) {
        CompoundTag root = stack == null ? null : stack.getTag();
        return root != null && root.contains(ROOT_TAG, Tag.TAG_COMPOUND);
    }

    /** Removes a prior battle issuance stamp before an administrator saves a stack as a template. */
    public static void clearStamp(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getTag() == null) {
            return;
        }
        CompoundTag tag = stack.getTag();
        tag.remove(ROOT_TAG);
        if (tag.isEmpty()) {
            stack.setTag(null);
        }
    }

    public static boolean isValid(ItemStack stack, UUID sessionId,
                                  UUID ownerId, UUID issueToken) {
        if (!isIssued(stack) || sessionId == null || ownerId == null || issueToken == null) {
            return false;
        }
        CompoundTag provenance = stack.getTag().getCompound(ROOT_TAG);
        return provenance.hasUUID(SESSION_TAG) && sessionId.equals(provenance.getUUID(SESSION_TAG))
                && provenance.hasUUID(OWNER_TAG) && ownerId.equals(provenance.getUUID(OWNER_TAG))
                && provenance.hasUUID(ISSUE_TOKEN_TAG)
                && issueToken.equals(provenance.getUUID(ISSUE_TOKEN_TAG));
    }

    public static boolean isValidAtSlot(ItemStack stack, UUID sessionId,
                                        UUID ownerId, UUID issueToken, int inventorySlot) {
        if (!isValid(stack, sessionId, ownerId, issueToken)) {
            return false;
        }
        CompoundTag provenance = stack.getTag().getCompound(ROOT_TAG);
        return inventorySlotMatches(provenance, inventorySlot);
    }

    static void markTransportCargo(CompoundTag provenance) {
        Objects.requireNonNull(provenance, "provenance");
        provenance.putBoolean(TRANSPORT_CARGO_TAG, true);
    }

    static boolean inventorySlotMatches(CompoundTag provenance, int inventorySlot) {
        return provenance != null && (provenance.getBoolean(TRANSPORT_CARGO_TAG)
                || provenance.contains(INVENTORY_SLOT_TAG, Tag.TAG_INT)
                && provenance.getInt(INVENTORY_SLOT_TAG) == inventorySlot);
    }

    /** Detects provenance hidden inside vanilla NBT or a Forge capability-backed container. */
    public static boolean containsIssuedPayload(ItemStack stack) {
        return scanPayload(stack) == PayloadScanResult.ISSUED;
    }

    /** Budget exhaustion is explicit so callers can reject safely without deleting user data. */
    public static PayloadScanResult scanPayload(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return PayloadScanResult.CLEAR;
        }
        // The authoritative top-level marker must win before any complex third-party capability
        // can exhaust the bounded deep-scan budget.
        if (isIssued(stack)) {
            return PayloadScanResult.ISSUED;
        }
        try {
            // ItemStack#getTag omits ForgeCaps. Full serialization includes capability-backed
            // backpacks/item handlers, closing a path that could otherwise persist issued kit.
            CompoundTag serialized = stack.save(new CompoundTag());
            return scanPayload(serialized, 0, new int[]{0});
        } catch (RuntimeException exception) {
            // Never delete user data when a third-party capability cannot serialize safely.
            return PayloadScanResult.INDETERMINATE;
        }
    }

    /** Removes all previous WOK deployment batches before an atomic install. */
    public static int purgeAllIssued(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        int removed = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (scanPayload(stack) == PayloadScanResult.ISSUED) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
                removed++;
            }
        }
        removed += purgeMenu(player, null, null, false, true);
        broadcast(player);
        return removed;
    }

    /** Removes foreign, stale-session and stale-issuance stacks from a player's inventory. */
    public static int purgeInvalid(ServerPlayer player, UUID sessionId, UUID issueToken) {
        Objects.requireNonNull(player, "player");
        int removed = 0;
        UUID ownerId = player.getUUID();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            // This runs every player tick: only inspect the authoritative top-level stamp here.
            // Deep container scans are reserved for transfer/craft/lifecycle boundaries.
            boolean issuedPayload = isIssued(stack);
            boolean validBoundStack = isIssued(stack)
                    && isValidAtSlot(stack, sessionId, ownerId, issueToken, slot);
            if (issuedPayload && !validBoundStack) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
                removed++;
            }
        }
        removed += purgeMenu(player, sessionId, issueToken, true, false);
        if (removed > 0) {
            broadcast(player);
        }
        return removed;
    }

    /** Issued combat kits never become normal death loot. */
    public static int removeIssuedDrops(Collection<ItemEntity> drops) {
        if (drops == null || drops.isEmpty()) {
            return 0;
        }
        int before = drops.size();
        drops.removeIf(entity -> entity != null
                && scanPayload(entity.getItem()) == PayloadScanResult.ISSUED);
        return before - drops.size();
    }

    private static int purgeMenu(ServerPlayer player, UUID sessionId, UUID issueToken,
                                 boolean allowBoundInventoryStacks, boolean deepScan) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null) {
            return 0;
        }
        int removed = 0;
        ItemStack carried = menu.getCarried();
        if (isIssuedForScan(carried, deepScan)) {
            menu.setCarried(ItemStack.EMPTY);
            removed++;
        }
        UUID ownerId = player.getUUID();
        for (Slot menuSlot : menu.slots) {
            ItemStack stack = menuSlot.getItem();
            if (!isIssuedForScan(stack, deepScan)) {
                continue;
            }
            int inventorySlot = menuSlot.getContainerSlot();
            boolean validPlayerInventory = allowBoundInventoryStacks
                    && menuSlot.container == player.getInventory()
                    && inventorySlot >= 0 && inventorySlot <= 40
                    && isIssued(stack)
                    && isValidAtSlot(stack, sessionId, ownerId, issueToken, inventorySlot);
            if (!validPlayerInventory) {
                menuSlot.set(ItemStack.EMPTY);
                removed++;
            }
        }
        return removed;
    }

    private static void broadcast(ServerPlayer player) {
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) {
            player.containerMenu.broadcastChanges();
        }
    }

    private static boolean isIssuedForScan(ItemStack stack, boolean deepScan) {
        return deepScan ? scanPayload(stack) == PayloadScanResult.ISSUED : isIssued(stack);
    }

    private static PayloadScanResult scanPayload(Tag tag, int depth, int[] visited) {
        if (tag == null) {
            return PayloadScanResult.CLEAR;
        }
        if (depth > MAX_NESTED_TAG_DEPTH
                || visited[0]++ >= MAX_NESTED_TAG_NODES) {
            return PayloadScanResult.INDETERMINATE;
        }
        if (tag instanceof CompoundTag compound) {
            if (compound.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
                return PayloadScanResult.ISSUED;
            }
            for (String key : compound.getAllKeys()) {
                PayloadScanResult child = scanPayload(compound.get(key), depth + 1, visited);
                if (child == PayloadScanResult.ISSUED) {
                    return child;
                }
                if (child == PayloadScanResult.INDETERMINATE) {
                    return child;
                }
            }
            return PayloadScanResult.CLEAR;
        } else if (tag instanceof net.minecraft.nbt.ListTag list) {
            for (int index = 0; index < list.size(); index++) {
                PayloadScanResult child = scanPayload(list.get(index), depth + 1, visited);
                if (child == PayloadScanResult.ISSUED) {
                    return child;
                }
                if (child == PayloadScanResult.INDETERMINATE) {
                    return child;
                }
            }
            return PayloadScanResult.CLEAR;
        }
        return PayloadScanResult.CLEAR;
    }
}
