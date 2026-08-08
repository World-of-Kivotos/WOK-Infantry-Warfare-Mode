package com.wok.infantry.server;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.loadout.LoadoutClassDefinition;
import com.wok.infantry.loadout.LoadoutConfigData;
import com.wok.infantry.loadout.LoadoutEntry;
import com.wok.infantry.loadout.LoadoutSlot;
import com.wok.infantry.loadout.LoadoutSnapshot;
import com.wok.infantry.loadout.PlayerLoadoutData;
import com.wok.infantry.network.LoadoutNetwork;
import com.wok.infantry.network.clientbound.LoadoutSnapshotPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class LoadoutService {
    public static final int ADMIN_PERMISSION_LEVEL = 2;

    private static final Map<MinecraftServer, LoadoutService> INSTANCES = new ConcurrentHashMap<>();

    private final LoadoutRepository repository;

    private LoadoutService(MinecraftServer server) {
        repository = new LoadoutRepository(server);
        repository.load();
    }

    public static void start(MinecraftServer server) {
        INSTANCES.put(server, new LoadoutService(server));
        WokInfantryMod.LOGGER.info("Wok Infantry loadout service started");
    }

    public static void stop(MinecraftServer server) {
        LoadoutService service = INSTANCES.remove(server);
        if (service != null) {
            service.repository.savePlayers();
            service.repository.saveConfig();
        }
    }

    public static Optional<LoadoutService> get(MinecraftServer server) {
        return Optional.ofNullable(INSTANCES.get(server));
    }

    public static Optional<LoadoutService> get(ServerPlayer player) {
        return get(player.server);
    }

    public void openPlayerScreen(ServerPlayer player) {
        sendSnapshot(player, LoadoutSnapshotPacket.OpenTarget.PLAYER);
    }

    public void openAdminScreen(ServerPlayer player) {
        if (!isAdministrator(player)) {
            player.sendSystemMessage(Component.literal("你没有管理配装的权限"));
            return;
        }
        sendSnapshot(player, LoadoutSnapshotPacket.OpenTarget.ADMIN);
    }

    public void savePlayerSelection(ServerPlayer player, String classId,
                                    Map<String, String> selections, boolean apply) {
        LoadoutClassDefinition definition = repository.config().findClass(classId)
                .filter(LoadoutClassDefinition::enabled).orElse(null);
        if (definition == null) {
            player.sendSystemMessage(Component.literal("该兵种不存在或已停用"));
            return;
        }

        PlayerLoadoutData playerData = repository.player(player.getUUID());
        for (LoadoutSlot slot : LoadoutSlot.values()) {
            String entryId = selections.get(slot.id());
            if (entryId == null || entryId.isBlank()) {
                continue;
            }
            boolean valid = definition.entries(slot).stream().anyMatch(entry -> entry.id().equals(entryId));
            if (!valid) {
                player.sendSystemMessage(Component.literal("无效的配装选项: " + entryId));
                return;
            }
            playerData.select(classId, slot, entryId);
        }
        playerData.setActiveClassId(classId);
        repository.savePlayers();

        if (apply) {
            applyLoadout(player);
        } else {
            player.sendSystemMessage(Component.translatable("message.wok_infantry.saved"));
        }
        sendSnapshot(player, LoadoutSnapshotPacket.OpenTarget.NONE);
    }

    public boolean applyLoadout(ServerPlayer player) {
        PlayerLoadoutData playerData = repository.player(player.getUUID());
        LoadoutClassDefinition definition = repository.config().findClass(playerData.activeClassId())
                .filter(LoadoutClassDefinition::enabled).orElse(null);
        if (definition == null) {
            player.sendSystemMessage(Component.literal("当前兵种不可用"));
            return false;
        }

        for (LoadoutSlot slot : LoadoutSlot.values()) {
            LoadoutEntry entry = resolveSelectedEntry(definition, playerData, slot);
            ItemStack stack = ItemStack.EMPTY;
            if (entry != null) {
                try {
                    stack = LoadoutStackFactory.create(entry);
                } catch (CommandSyntaxException exception) {
                    player.sendSystemMessage(Component.literal(
                            "装备数据损坏: " + entry.displayName()));
                }
            }
            player.getInventory().setItem(slot.hotbarIndex(), stack);
        }
        player.getInventory().selected = 0;
        player.inventoryMenu.broadcastChanges();
        player.sendSystemMessage(Component.translatable("message.wok_infantry.applied"));
        return true;
    }

    public void upsertEntry(ServerPlayer administrator, String classId, String slotId,
                            LoadoutEntry candidate, String originalEntryId) {
        if (!requireAdministrator(administrator)) {
            return;
        }
        LoadoutClassDefinition definition = repository.config().findClass(classId).orElse(null);
        LoadoutSlot slot = LoadoutSlot.byId(slotId).orElse(null);
        if (definition == null || slot == null) {
            administrator.sendSystemMessage(Component.literal("兵种或槽位不存在"));
            return;
        }
        LoadoutStackFactory.ValidationResult validation = LoadoutStackFactory.validate(candidate);
        if (!validation.valid()) {
            administrator.sendSystemMessage(Component.literal(validation.message()));
            return;
        }
        boolean duplicate = definition.entries(slot).stream()
                .anyMatch(entry -> entry.id().equals(candidate.id())
                        && !entry.id().equals(originalEntryId));
        if (duplicate) {
            administrator.sendSystemMessage(Component.literal("同一槽位中装备 ID 必须唯一"));
            return;
        }
        if (originalEntryId != null && !originalEntryId.isBlank()) {
            definition.entries(slot).removeIf(entry -> entry.id().equals(originalEntryId));
        }
        definition.entries(slot).add(candidate.copy());
        repository.saveConfig();
        administrator.sendSystemMessage(Component.translatable("message.wok_infantry.admin_saved"));
        sendSnapshot(administrator, LoadoutSnapshotPacket.OpenTarget.REFRESH_ADMIN);
    }

    public void deleteEntry(ServerPlayer administrator, String classId,
                            String slotId, String entryId) {
        if (!requireAdministrator(administrator)) {
            return;
        }
        LoadoutClassDefinition definition = repository.config().findClass(classId).orElse(null);
        LoadoutSlot slot = LoadoutSlot.byId(slotId).orElse(null);
        if (definition == null || slot == null) {
            return;
        }
        definition.entries(slot).removeIf(entry -> entry.id().equals(entryId));
        repository.saveConfig();
        sendSnapshot(administrator, LoadoutSnapshotPacket.OpenTarget.REFRESH_ADMIN);
    }

    public void updateClass(ServerPlayer administrator, String classId,
                            String displayName, boolean enabled) {
        if (!requireAdministrator(administrator)) {
            return;
        }
        LoadoutClassDefinition definition = repository.config().findClass(classId).orElse(null);
        if (definition == null || displayName == null || displayName.isBlank()
                || displayName.length() > 40) {
            administrator.sendSystemMessage(Component.literal("兵种名称无效"));
            return;
        }
        definition.updateMetadata(displayName, enabled);
        repository.saveConfig();
        sendSnapshot(administrator, LoadoutSnapshotPacket.OpenTarget.REFRESH_ADMIN);
    }

    private LoadoutEntry resolveSelectedEntry(LoadoutClassDefinition definition,
                                               PlayerLoadoutData playerData,
                                               LoadoutSlot slot) {
        String selectedId = playerData.selectedEntry(definition.id(), slot);
        return definition.entries(slot).stream()
                .filter(entry -> entry.id().equals(selectedId))
                .findFirst()
                .orElseGet(() -> definition.entries(slot).stream().findFirst().orElse(null));
    }

    private void sendSnapshot(ServerPlayer player, LoadoutSnapshotPacket.OpenTarget openTarget) {
        LoadoutSnapshot snapshot = new LoadoutSnapshot(
                repository.config().copy(), repository.player(player.getUUID()).copy(),
                isAdministrator(player));
        LoadoutNetwork.sendToPlayer(player, new LoadoutSnapshotPacket(snapshot, openTarget));
    }

    private boolean requireAdministrator(ServerPlayer player) {
        if (isAdministrator(player)) {
            return true;
        }
        player.sendSystemMessage(Component.literal("管理员权限校验失败"));
        return false;
    }

    private boolean isAdministrator(ServerPlayer player) {
        return player.hasPermissions(ADMIN_PERMISSION_LEVEL);
    }
}
