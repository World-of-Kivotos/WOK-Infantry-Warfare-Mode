package com.wok.infantry.server;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.battle.ActionResult;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.battle.ClassQuotaView;
import com.wok.infantry.deployment.DeploymentService;
import com.wok.infantry.deployment.KitProvenance;
import com.wok.infantry.formation.FormationClassEditAction;
import com.wok.infantry.formation.ClassLoadoutCopyPlanner;
import com.wok.infantry.formation.FormationConfigData;
import com.wok.infantry.formation.FormationDefinition;
import com.wok.infantry.formation.FormationLoadoutEditAction;
import com.wok.infantry.loadout.LoadoutClassDefinition;
import com.wok.infantry.loadout.LoadoutConfigData;
import com.wok.infantry.loadout.LoadoutEntry;
import com.wok.infantry.loadout.LoadoutInventoryTarget;
import com.wok.infantry.loadout.LoadoutSlotDefinition;
import com.wok.infantry.loadout.LoadoutSnapshot;
import com.wok.infantry.loadout.PlayerLoadoutData;
import com.wok.infantry.network.LoadoutNetwork;
import com.wok.infantry.network.clientbound.LoadoutSnapshotPacket;
import com.wok.infantry.network.serverbound.AdminSlotPacket;
import com.wok.infantry.registry.InfantryItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LoadoutService {
    public static final int ADMIN_PERMISSION_LEVEL = 2;
    private static final long PLAYER_SAVE_DEBOUNCE_TICKS = 5L * 20L;

    private static final Map<MinecraftServer, LoadoutService> INSTANCES = new ConcurrentHashMap<>();

    private final LoadoutRepository repository;
    private final MinecraftServer server;
    private boolean playersDirty;
    private long nextPlayerSaveTick;

    private LoadoutService(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
        repository = new LoadoutRepository(server);
        repository.load();
    }

    public static void start(MinecraftServer server) {
        INSTANCES.put(server, new LoadoutService(server));
        WokInfantryMod.LOGGER.info("WOK Infantry loadout service started");
    }

    public static void stop(MinecraftServer server) {
        LoadoutService service = INSTANCES.remove(server);
        if (service != null) {
            service.flushPlayerData();
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
        FormationService formations = FormationService.get(player).orElse(null);
        if (formations == null || formations.classRule(player.getUUID(), classId).isEmpty()) {
            player.sendSystemMessage(Component.literal("当前编制不允许该兵种"));
            return;
        }

        Map<String, String> validatedSelections = new LinkedHashMap<>();
        for (LoadoutSlotDefinition slot : definition.slotDefinitions()) {
            String entryId = selections.get(slot.id());
            if (entryId == null || entryId.isBlank()) {
                continue;
            }
            boolean valid = definition.entries(slot.id()).stream()
                    .anyMatch(entry -> entry.id().equals(entryId));
            if (!valid) {
                player.sendSystemMessage(Component.literal("无效的配装选项: " + entryId));
                return;
            }
            if (!formations.allowsLoadoutEntry(player.getUUID(), classId, slot.id(), entryId)) {
                player.sendSystemMessage(Component.literal(
                        "当前编制不允许该配装选项: " + entryId));
                return;
            }
            validatedSelections.put(slot.id(), entryId);
        }

        ActionResult classResult = reserveBattleClass(player, definition);
        if (!classResult.success()) {
            player.sendSystemMessage(Component.literal(classResult.message()));
            return;
        }

        PlayerLoadoutData playerData = repository.player(player.getUUID());
        boolean changed = !playerData.activeClassId().equals(classId);
        for (Map.Entry<String, String> selection : validatedSelections.entrySet()) {
            changed |= !Objects.equals(playerData.selectedEntry(classId, selection.getKey()),
                    selection.getValue());
            playerData.select(classId, selection.getKey(), selection.getValue());
        }
        playerData.setActiveClassId(classId);
        if (changed) {
            markPlayersDirty();
        }

        if (apply) {
            applyLoadout(player);
        } else {
            player.sendSystemMessage(Component.translatable("message.wok_infantry.saved"));
        }
        sendSnapshot(player, LoadoutSnapshotPacket.OpenTarget.NONE);
    }

    /**
     * Reserves a server-authoritative squad class slot without issuing equipment. This is used by
     * the Squad/WARDOGS-style roster UI; the loadout screen remains responsible for item choices.
     */
    public ActionResult assignBattleClass(ServerPlayer player, String classId) {
        LoadoutClassDefinition definition = repository.config().findClass(classId).orElse(null);
        if (definition == null) {
            return ActionResult.failure(ActionResult.Code.INVALID_CLASS_ID,
                    "该兵种不存在或已停用");
        }
        ActionResult result = reserveBattleClass(player, definition);
        if (result.success()) {
            PlayerLoadoutData playerData = repository.player(player.getUUID());
            if (!definition.id().equals(playerData.activeClassId())) {
                playerData.setActiveClassId(definition.id());
                markPlayersDirty();
                sendSnapshot(player, LoadoutSnapshotPacket.OpenTarget.NONE);
            }
        }
        return result;
    }

    /** Snapshot-safe class limits, preserving administrator configuration order. */
    public Map<String, Integer> classLimits() {
        LinkedHashMap<String, Integer> limits = new LinkedHashMap<>();
        repository.config().classes().stream()
                .filter(LoadoutClassDefinition::enabled)
                .forEach(definition -> limits.put(definition.id(), definition.squadLimit()));
        return Collections.unmodifiableMap(limits);
    }

    /** Formation-scoped class limits for one player; global limits are only a safe fallback. */
    public Map<String, Integer> classLimits(ServerPlayer player) {
        FormationService formations = FormationService.get(player).orElse(null);
        String callsign = BattleService.get(player).flatMap(service ->
                service.squadOf(player.getUUID())).map(callsignValue -> callsignValue.id())
                .orElse("");
        Map<String, Integer> formationLimits = formations == null ? Map.of()
                : formations.classLimits(player.getUUID(), callsign);
        if (formationLimits.isEmpty()) {
            return classLimits();
        }
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        formationLimits.forEach((classId, limit) -> repository.config().findClass(classId)
                .filter(LoadoutClassDefinition::enabled)
                .ifPresent(definition -> result.put(definition.id(), limit)));
        return Collections.unmodifiableMap(result);
    }

    /** Bounded, Unicode-safe class labels in the same order and enabled set as classLimits(). */
    public Map<String, String> classDisplayNames() {
        LinkedHashMap<String, String> displayNames = new LinkedHashMap<>();
        repository.config().classes().stream()
                .filter(LoadoutClassDefinition::enabled)
                .forEach(definition -> displayNames.put(definition.id(),
                        ClassQuotaView.sanitizeDisplayName(definition.displayName())));
        return Collections.unmodifiableMap(displayNames);
    }

    public Map<String, String> classDisplayNames(ServerPlayer player) {
        Map<String, Integer> limits = classLimits(player);
        FormationService formations = FormationService.get(player).orElse(null);
        LinkedHashMap<String, String> displayNames = new LinkedHashMap<>();
        limits.keySet().forEach(classId -> repository.config().findClass(classId)
                .ifPresent(definition -> displayNames.put(definition.id(),
                        ClassQuotaView.sanitizeDisplayName(formations == null
                                ? definition.displayName()
                                : scopedClassName(formations, player.getUUID(), definition)))));
        return Collections.unmodifiableMap(displayNames);
    }

    public boolean applyLoadout(ServerPlayer player) {
        if (!isAdministrator(player)) {
            player.sendSystemMessage(Component.literal(
                    "装备发放已改由部署/补给流程控制；当前界面只保存兵种与配装选择"));
            return false;
        }
        return DeploymentService.get(player)
                .map(service -> service.resupply(player).success()).orElse(false);
    }

    /** Coalesces up to 80 players' rapid UI saves into at most one full-store write per 5s. */
    public void tick() {
        if (playersDirty && server.getTickCount() >= nextPlayerSaveTick) {
            flushPlayerData();
        }
    }

    private void markPlayersDirty() {
        if (!playersDirty) {
            playersDirty = true;
            nextPlayerSaveTick = server.getTickCount() + PLAYER_SAVE_DEBOUNCE_TICKS;
        }
    }

    private void flushPlayerData() {
        if (playersDirty) {
            repository.savePlayers();
            playersDirty = false;
            nextPlayerSaveTick = 0L;
        }
    }

    /**
     * Resolves every configured slot before any inventory mutation. Deployment and resupply use
     * this as the prepare half of a two-phase issue transaction.
     */
    public PreparedLoadout prepareDeploymentLoadout(ServerPlayer player) {
        BattleService battleService = BattleService.get(player).orElse(null);
        if (battleService == null || battleService.factionOf(player.getUUID()).isEmpty()
                || battleService.formationOf(player.getUUID()).isEmpty()
                || battleService.squadOf(player.getUUID()).isEmpty()) {
            return PreparedLoadout.failure(ActionResult.failure(ActionResult.Code.NOT_IN_SQUAD,
                    "必须先加入战局小队才能发放装备"));
        }
        PlayerLoadoutData playerData = repository.player(player.getUUID());
        String assignedClassId = battleService.assignedClass(player.getUUID());
        LoadoutClassDefinition definition = repository.config().findClass(assignedClassId)
                .orElse(null);
        if (definition == null) {
            return PreparedLoadout.failure(ActionResult.failure(ActionResult.Code.INVALID_CLASS_ID,
                    "当前兵种不可用"));
        }
        FormationService formations = FormationService.get(player).orElse(null);
        if (formations == null
                || formations.classRule(player.getUUID(), assignedClassId).isEmpty()) {
            return PreparedLoadout.failure(ActionResult.failure(ActionResult.Code.INVALID_CLASS_ID,
                    "当前编制不允许该兵种"));
        }

        List<PreparedStack> resolvedStacks = new ArrayList<>();
        for (LoadoutSlotDefinition slot : definition.slotDefinitions()) {
            LoadoutEntry entry = resolveSelectedEntry(definition, playerData, slot);
            if (entry == null) {
                if (slot.required()) {
                    return PreparedLoadout.failure(ActionResult.failure(
                            ActionResult.Code.LOADOUT_INCOMPLETE,
                            "兵种 " + scopedClassName(formations, player.getUUID(), definition)
                                    + " 的必需槽位“" + slot.displayName()
                                    + "”尚未配置，未发放任何装备"));
                }
                continue;
            }
            if (!formations.allowsLoadoutEntry(player.getUUID(), assignedClassId,
                    slot.id(), entry.id())) {
                return PreparedLoadout.failure(ActionResult.failure(
                        ActionResult.Code.LOADOUT_INCOMPLETE,
                        "当前编制不允许 " + slot.displayName() + " 配装："
                                + entry.displayName()));
            }
            ItemStack stack;
            try {
                stack = LoadoutStackFactory.create(entry);
            } catch (CommandSyntaxException exception) {
                return PreparedLoadout.failure(ActionResult.failure(
                        ActionResult.Code.LOADOUT_INCOMPLETE,
                        "装备数据损坏: " + entry.displayName()));
            }
            if (stack.isEmpty()) {
                if (slot.required()) {
                    return PreparedLoadout.failure(ActionResult.failure(
                            ActionResult.Code.LOADOUT_INCOMPLETE,
                            "兵种 " + scopedClassName(formations, player.getUUID(), definition)
                                    + " 的必需槽位“" + slot.displayName()
                                    + "”没有有效装备，未发放任何装备"));
                }
                continue;
            }
            if (!slot.target().accepts(stack, player)) {
                return PreparedLoadout.failure(ActionResult.failure(
                        ActionResult.Code.LOADOUT_INCOMPLETE,
                        slot.displayName() + "中的“" + entry.displayName() + "”不能装备到"
                                + slot.target().displayName()));
            }
            resolvedStacks.add(new PreparedStack(slot.id(), slot.displayName(),
                    slot.target(), stack, entry.ammoReserveLimit()));
        }
        return PreparedLoadout.success(resolvedStacks);
    }

    /**
     * Refuses to overwrite ordinary player items. Existing WOK deployment stacks are safe to
     * replace because their old issuance token is invalidated by the caller's transaction.
     */
    public ActionResult validateInstallSlots(ServerPlayer player, PreparedLoadout prepared) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(prepared, "prepared");
        if (!prepared.result().success()) {
            return ActionResult.failure(ActionResult.Code.LOADOUT_INCOMPLETE,
                    "配装尚未完整解析");
        }
        ActionResult inventoryPolicy = validateDeploymentInventory(player);
        if (!inventoryPolicy.success()) {
            return inventoryPolicy;
        }
        Set<Integer> targets = new HashSet<>();
        for (PreparedStack resolved : prepared.stacks()) {
            ItemStack preparedStack = resolved.stack();
            if (preparedStack.isEmpty() || !targets.add(resolved.target().inventoryIndex())) {
                return ActionResult.failure(ActionResult.Code.LOADOUT_INCOMPLETE,
                        "配装槽位目标重复或为空: " + resolved.displayName());
            }
            int inventoryIndex = resolved.target().inventoryIndex();
            ItemStack existing = player.getInventory().getItem(inventoryIndex);
            if (!existing.isEmpty() && !KitProvenance.isIssued(existing)) {
                return ActionResult.failure(ActionResult.Code.INVENTORY_BLOCKED,
                        "请先清空" + resolved.target().displayName()
                                + "（" + resolved.displayName() + "），不会覆盖普通物品");
            }
        }
        return ActionResult.ok("装备栏位可安全写入");
    }

    /**
     * A deployment is a managed per-life inventory, not six optional bonus stacks. Equipped armor
     * remains available for the independent armor module; every general/offhand slot must be clear.
     */
    public ActionResult validateDeploymentInventory(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (!player.containerMenu.getCarried().isEmpty()) {
            return ActionResult.failure(ActionResult.Code.INVENTORY_BLOCKED,
                    "请先放下鼠标光标上的物品");
        }
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty() || KitProvenance.isIssued(stack)) {
                continue;
            }
            KitProvenance.PayloadScanResult payload = KitProvenance.scanPayload(stack);
            if (payload != KitProvenance.PayloadScanResult.CLEAR) {
                return ActionResult.failure(ActionResult.Code.INVENTORY_BLOCKED,
                        "背包物品含无法安全验证的战局装备数据，请先移出背包");
            }
            String location = slot < 9 ? "快捷栏 " + (slot + 1) : "普通背包";
            return ActionResult.failure(ActionResult.Code.INVENTORY_BLOCKED,
                    "部署前请清空" + location + "；战局装备不是额外赠送物品");
        }
        ItemStack offhand = player.getInventory().getItem(40);
        if (!offhand.isEmpty() && !KitProvenance.isIssued(offhand)) {
            return ActionResult.failure(ActionResult.Code.INVENTORY_BLOCKED,
                    "部署前请清空副手栏");
        }
        // Personal armor remains available in targets the profession does not manage.
        for (int slot = 36; slot < 40; slot++) {
            ItemStack armor = player.getInventory().getItem(slot);
            if (!armor.isEmpty() && !KitProvenance.isIssued(armor)
                    && KitProvenance.scanPayload(armor)
                    != KitProvenance.PayloadScanResult.CLEAR) {
                return ActionResult.failure(ActionResult.Code.INVENTORY_BLOCKED,
                        "护甲槽含战局装备数据，请先卸下该物品");
            }
        }
        return ActionResult.ok("部署物品栏已清理");
    }

    /** Builds a zero-mutation clear plan, including armor, personal crafting slots and cursor. */
    public InventoryClearPlan prepareInventoryClear(ServerPlayer player, PreparedLoadout prepared) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(prepared, "prepared");
        if (!prepared.result().success()) {
            return InventoryClearPlan.failure(prepared.result());
        }
        if (player.containerMenu != player.inventoryMenu) {
            return InventoryClearPlan.failure(ActionResult.failure(
                    ActionResult.Code.INVENTORY_BLOCKED, "请先关闭外部容器再部署"));
        }
        Map<Integer, ItemStack> inventoryBefore = new LinkedHashMap<>();
        Map<Integer, ItemStack> craftingBefore = new LinkedHashMap<>();
        boolean clearedAny = false;

        for (int slot = 0; slot <= 40; slot++) {
            ItemStack stack = player.getInventory().getItem(slot).copy();
            inventoryBefore.put(slot, stack);
            clearedAny |= !stack.isEmpty();
        }

        for (int menuSlot = 0; menuSlot <= 4; menuSlot++) {
            ItemStack stack = player.inventoryMenu.getSlot(menuSlot).getItem().copy();
            craftingBefore.put(menuSlot, stack);
            clearedAny |= !stack.isEmpty();
        }

        ItemStack carriedBefore = player.inventoryMenu.getCarried().copy();
        clearedAny |= !carriedBefore.isEmpty();
        return InventoryClearPlan.success(inventoryBefore, craftingBefore,
                carriedBefore, clearedAny);
    }

    /** Commits the destructive body clear and issued kit as one rollback-capable transaction. */
    public ActionResult commitDeploymentInventory(ServerPlayer player,
                                                  InventoryClearPlan plan,
                                                  PreparedLoadout prepared,
                                                  UUID sessionId, UUID issueToken) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(plan, "plan");
        if (!plan.result().success()) {
            return plan.result();
        }
        if (!prepared.result().success()) {
            return prepared.result();
        }
        if (!inventoryMatchesPlan(player, plan)) {
            return ActionResult.failure(ActionResult.Code.INVENTORY_BLOCKED,
                    "部署事务期间物品栏发生变化，请重试");
        }
        try {
            plan.inventoryBefore().keySet().forEach(slot ->
                    player.getInventory().setItem(slot, ItemStack.EMPTY));
            for (int menuSlot = 0; menuSlot <= 4; menuSlot++) {
                player.inventoryMenu.getSlot(menuSlot).set(ItemStack.EMPTY);
            }
            player.inventoryMenu.setCarried(ItemStack.EMPTY);
            installPreparedLoadout(player, prepared, sessionId, issueToken);
        } catch (RuntimeException exception) {
            plan.inventoryBefore().forEach((slot, stack) ->
                    player.getInventory().setItem(slot, stack.copy()));
            plan.craftingBefore().forEach((slot, stack) ->
                    player.inventoryMenu.getSlot(slot).set(stack.copy()));
            player.inventoryMenu.setCarried(plan.carriedBefore().copy());
            player.inventoryMenu.broadcastChanges();
            return ActionResult.failure(ActionResult.Code.INVENTORY_BLOCKED,
                    "部署物品事务未提交，原物品已恢复");
        }
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        if (plan.clearedAny()) {
            player.sendSystemMessage(Component.literal("部署前随身物品已清空"));
        }
        return ActionResult.ok("部署物品事务已提交");
    }

    /** Every ACTIVE tick validates that no weapon or utility bypassed the selected class kit. */
    public ActionResult validateActiveInventory(ServerPlayer player, UUID sessionId,
                                                UUID issueToken, boolean deepArmorScan) {
        Objects.requireNonNull(player, "player");
        // Temporary operator-only testing bypass. It allows creative-mode or /give test items to
        // coexist with an ACTIVE deployment without weakening normal-player provenance checks.
        // Packet-level container, pickup/drop and persistent block/fluid restrictions remain in
        // force, so this bypass is intentionally narrower than disabling managed inventories.
        if (player.hasPermissions(ADMIN_PERMISSION_LEVEL)) {
            return ActionResult.ok("管理员测试模式：已跳过活动配装来源校验");
        }
        if (player.containerMenu != player.inventoryMenu) {
            return ActionResult.failure(ActionResult.Code.INVENTORY_BLOCKED,
                    "已部署状态不能打开外部容器");
        }
        if (!player.containerMenu.getCarried().isEmpty()) {
            return ActionResult.failure(ActionResult.Code.INVENTORY_BLOCKED,
                    "已部署状态不能携带光标物品");
        }
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            boolean validIssued = KitProvenance.isValidAtSlot(stack, sessionId,
                    player.getUUID(), issueToken, slot);
            if (!validIssued && !isAdministratorDeploymentTool(player, stack)) {
                return ActionResult.failure(ActionResult.Code.INVENTORY_BLOCKED,
                        "检测到兵种配装之外的物品，已终止当前部署");
            }
        }
        ItemStack offhand = player.getInventory().getItem(40);
        if (!offhand.isEmpty()
                && !KitProvenance.isValidAtSlot(offhand, sessionId, player.getUUID(),
                issueToken, 40)
                && !isAdministratorDeploymentTool(player, offhand)) {
            return ActionResult.failure(ActionResult.Code.INVENTORY_BLOCKED,
                    "检测到未授权副手物品，已终止当前部署");
        }
        for (int menuSlot = 1; menuSlot <= 4; menuSlot++) {
            if (!player.inventoryMenu.getSlot(menuSlot).getItem().isEmpty()) {
                return ActionResult.failure(ActionResult.Code.INVENTORY_BLOCKED,
                        "已部署状态不能使用个人合成栏");
            }
        }
        for (int slot = 36; slot < 40; slot++) {
            ItemStack armor = player.getInventory().getItem(slot);
            if (armor.isEmpty()) {
                continue;
            }
            boolean issued = KitProvenance.isIssued(armor);
            boolean invalidArmor = !issued
                    || !KitProvenance.isValidAtSlot(armor, sessionId, player.getUUID(),
                    issueToken, slot);
            if (invalidArmor) {
                return ActionResult.failure(ActionResult.Code.INVENTORY_BLOCKED,
                        "检测到护甲槽中的战局标记异常");
            }
        }
        return ActionResult.ok("当前生命装备来源有效");
    }

    /**
     * ACTIVE starts with an empty general inventory except for issued slots. Any clean
     * remainder appearing there is therefore untrusted and must not survive into the next stash.
     */
    public void clearManagedCombatInventory(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        for (int slot = 0; slot < 36; slot++) {
            player.getInventory().setItem(slot, ItemStack.EMPTY);
        }
        player.getInventory().setItem(40, ItemStack.EMPTY);
        for (int slot = 36; slot < 40; slot++) {
            player.getInventory().setItem(slot, ItemStack.EMPTY);
        }
        for (int menuSlot = 0; menuSlot <= 4; menuSlot++) {
            player.inventoryMenu.getSlot(menuSlot).set(ItemStack.EMPTY);
        }
        player.inventoryMenu.setCarried(ItemStack.EMPTY);
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
    }

    private static boolean inventoryMatchesPlan(ServerPlayer player, InventoryClearPlan plan) {
        if (player.containerMenu != player.inventoryMenu
                || !sameStack(player.inventoryMenu.getCarried(), plan.carriedBefore())) {
            return false;
        }
        for (Map.Entry<Integer, ItemStack> entry : plan.inventoryBefore().entrySet()) {
            if (!sameStack(player.getInventory().getItem(entry.getKey()), entry.getValue())) {
                return false;
            }
        }
        for (Map.Entry<Integer, ItemStack> entry : plan.craftingBefore().entrySet()) {
            if (!sameStack(player.inventoryMenu.getSlot(entry.getKey()).getItem(),
                    entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameStack(ItemStack first, ItemStack second) {
        if (first.isEmpty() || second.isEmpty()) {
            return first.isEmpty() && second.isEmpty();
        }
        return first.getCount() == second.getCount()
                && ItemStack.isSameItemSameTags(first, second);
    }

    /** Install half of the deployment transaction; all fallible parsing happened in prepare. */
    public void installPreparedLoadout(ServerPlayer player, PreparedLoadout prepared,
                                       UUID sessionId, UUID issueToken) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(prepared, "prepared");
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(issueToken, "issueToken");
        if (!prepared.result().success()) {
            throw new IllegalArgumentException("Cannot install a failed prepared loadout");
        }
        ActionResult slotValidation = validateInstallSlots(player, prepared);
        if (!slotValidation.success()) {
            throw new IllegalStateException(slotValidation.message());
        }
        KitProvenance.purgeAllIssued(player);
        int selectedHotbar = 0;
        boolean selectedHotbarSet = false;
        for (PreparedStack resolved : prepared.stacks()) {
            ItemStack stack = resolved.stack().copy();
            int inventoryIndex = resolved.target().inventoryIndex();
            KitProvenance.stamp(stack, sessionId, player.getUUID(), issueToken,
                    inventoryIndex, resolved.ammoReserveLimit());
            player.getInventory().setItem(inventoryIndex, stack);
            if (!selectedHotbarSet && inventoryIndex >= 0 && inventoryIndex <= 8) {
                selectedHotbar = inventoryIndex;
                selectedHotbarSet = true;
            }
        }
        player.getInventory().selected = selectedHotbar;
        player.inventoryMenu.broadcastChanges();
    }

    public record InventoryClearPlan(ActionResult result,
                                     Map<Integer, ItemStack> inventoryBefore,
                                     Map<Integer, ItemStack> craftingBefore,
                                     ItemStack carriedBefore,
                                     boolean clearedAny) {
        public InventoryClearPlan {
            Objects.requireNonNull(result, "result");
            inventoryBefore = copyStackMap(inventoryBefore);
            craftingBefore = copyStackMap(craftingBefore);
            carriedBefore = carriedBefore == null ? ItemStack.EMPTY : carriedBefore.copy();
        }

        private static InventoryClearPlan success(Map<Integer, ItemStack> inventoryBefore,
                                                  Map<Integer, ItemStack> craftingBefore,
                                                  ItemStack carriedBefore,
                                                  boolean clearedAny) {
            return new InventoryClearPlan(ActionResult.ok("物品清空计划就绪"),
                    inventoryBefore, craftingBefore, carriedBefore, clearedAny);
        }

        private static InventoryClearPlan failure(ActionResult result) {
            return new InventoryClearPlan(result, Map.of(), Map.of(),
                    ItemStack.EMPTY, false);
        }

        private static Map<Integer, ItemStack> copyStackMap(Map<Integer, ItemStack> source) {
            LinkedHashMap<Integer, ItemStack> copy = new LinkedHashMap<>();
            if (source != null) {
                source.forEach((slot, stack) -> {
                    if (slot != null && stack != null) {
                        copy.put(slot, stack.copy());
                    }
                });
            }
            return Collections.unmodifiableMap(copy);
        }
    }

    public record PreparedStack(String slotId, String displayName,
                                LoadoutInventoryTarget target, ItemStack stack,
                                int ammoReserveLimit) {
        public PreparedStack {
            slotId = Objects.requireNonNullElse(slotId, "");
            displayName = Objects.requireNonNullElse(displayName, slotId);
            Objects.requireNonNull(target, "target");
            stack = stack == null ? ItemStack.EMPTY : stack.copy();
            ammoReserveLimit = Math.max(LoadoutEntry.MIN_AMMO_RESERVE_LIMIT,
                    Math.min(LoadoutEntry.MAX_AMMO_RESERVE_LIMIT, ammoReserveLimit));
        }
    }

    public record PreparedLoadout(ActionResult result, List<PreparedStack> stacks) {
        public PreparedLoadout {
            Objects.requireNonNull(result, "result");
            List<PreparedStack> copies = new ArrayList<>();
            if (stacks != null) {
                stacks.forEach(stack -> {
                    if (stack != null && !stack.stack().isEmpty()) {
                        copies.add(new PreparedStack(stack.slotId(), stack.displayName(),
                                stack.target(), stack.stack(), stack.ammoReserveLimit()));
                    }
                });
            }
            stacks = Collections.unmodifiableList(copies);
        }

        static PreparedLoadout success(List<PreparedStack> stacks) {
            return new PreparedLoadout(ActionResult.ok("配装校验通过"), stacks);
        }

        static PreparedLoadout failure(ActionResult result) {
            return new PreparedLoadout(result, List.of());
        }
    }

    public void upsertEntry(ServerPlayer administrator, String classId, String slotId,
                            LoadoutEntry candidate, String originalEntryId) {
        upsertEntry(administrator, classId, slotId, candidate, originalEntryId, "", "");
    }

    public void upsertEntry(ServerPlayer administrator, String classId, String slotId,
                            LoadoutEntry candidate, String originalEntryId,
                            String factionId, String formationId) {
        if (upsertEntryInternal(administrator, classId, slotId, candidate, originalEntryId)) {
            if (originalEntryId == null || originalEntryId.isBlank()) {
                includeNewEntryInFormation(administrator, factionId, formationId,
                        classId, slotId, candidate.id());
            }
            sendSnapshot(administrator, LoadoutSnapshotPacket.OpenTarget.REFRESH_ADMIN);
        }
    }

    private boolean upsertEntryInternal(ServerPlayer administrator, String classId, String slotId,
                                        LoadoutEntry candidate, String originalEntryId) {
        if (!requireAdministrator(administrator)) {
            return false;
        }
        LoadoutClassDefinition definition = repository.config().findClass(classId).orElse(null);
        LoadoutSlotDefinition slot = definition == null ? null
                : definition.findSlot(slotId).orElse(null);
        if (definition == null || slot == null) {
            administrator.sendSystemMessage(Component.literal("兵种或槽位不存在"));
            return false;
        }
        LoadoutStackFactory.ValidationResult validation = LoadoutStackFactory.validate(candidate);
        if (!validation.valid()) {
            administrator.sendSystemMessage(Component.literal(validation.message()));
            return false;
        }
        boolean duplicate = definition.entries(slot.id()).stream()
                .anyMatch(entry -> entry.id().equals(candidate.id())
                        && !entry.id().equals(originalEntryId));
        if (duplicate) {
            administrator.sendSystemMessage(Component.literal("同一槽位中装备 ID 必须唯一"));
            return false;
        }
        if (originalEntryId != null && !originalEntryId.isBlank()) {
            definition.entries(slot.id()).removeIf(entry -> entry.id().equals(originalEntryId));
        }
        definition.entries(slot.id()).add(candidate.copy());
        repository.saveConfig();
        administrator.sendSystemMessage(Component.translatable("message.wok_infantry.admin_saved"));
        return true;
    }

    /**
     * Saves the administrator's current main-hand stack as a reusable server-side template.
     * Registry id, count and item tag are read on the server, so this shortcut cannot be used to
     * inject arbitrary client SNBT. TaCZ attachment tags remain intact; battle provenance and
     * physical durability damage are removed from the template.
     */
    public void captureMainHandEntry(ServerPlayer administrator, String classId, String slotId,
                                     String originalEntryId, String requestedEntryId,
                                     String requestedDisplayName) {
        captureMainHandEntry(administrator, classId, slotId, originalEntryId,
                requestedEntryId, requestedDisplayName, "", "",
                LoadoutEntry.DEFAULT_AMMO_RESERVE_LIMIT);
    }

    public void captureMainHandEntry(ServerPlayer administrator, String classId, String slotId,
                                     String originalEntryId, String requestedEntryId,
                                     String requestedDisplayName, String factionId,
                                     String formationId, int ammoReserveLimit) {
        if (!requireAdministrator(administrator)) {
            return;
        }
        LoadoutClassDefinition definition = repository.config().findClass(classId).orElse(null);
        LoadoutSlotDefinition slot = definition == null ? null
                : definition.findSlot(slotId).orElse(null);
        if (definition == null || slot == null) {
            administrator.sendSystemMessage(Component.literal("兵种或槽位不存在"));
            return;
        }

        ItemStack captured = administrator.getMainHandItem().copy();
        if (captured.isEmpty()) {
            administrator.sendSystemMessage(Component.literal("主手没有可读取的物品"));
            return;
        }
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(captured.getItem());
        if (itemId == null || captured.is(Items.AIR)) {
            administrator.sendSystemMessage(Component.literal("无法确定主手物品的注册名"));
            return;
        }

        KitProvenance.clearStamp(captured);
        CompoundTag capturedTag = captured.getTag();
        if (capturedTag != null) {
            capturedTag.remove("Damage");
            if (capturedTag.isEmpty()) {
                captured.setTag(null);
                capturedTag = null;
            }
        }

        String originalId = Objects.requireNonNullElse(originalEntryId, "").trim();
        String requestedId = Objects.requireNonNullElse(requestedEntryId, "").trim();
        String entryId = !requestedId.isBlank() ? requestedId
                : !originalId.isBlank() ? originalId
                : uniqueCapturedEntryId(captureIdBase(itemId.getPath(), capturedTag),
                definition.entries(slot.id()), "");
        String requestedName = Objects.requireNonNullElse(requestedDisplayName, "").trim();
        String displayName = requestedName.isBlank()
                ? captured.getHoverName().getString() : requestedName;
        String snbt = capturedTag == null ? "" : capturedTag.toString();
        LoadoutEntry candidate = new LoadoutEntry(entryId, displayName,
                itemId.toString(), captured.getCount(), snbt, ammoReserveLimit);

        if (upsertEntryInternal(administrator, classId, slotId, candidate, originalId)) {
            if (originalId.isBlank()) {
                includeNewEntryInFormation(administrator, factionId, formationId,
                        classId, slotId, entryId);
            }
            administrator.sendSystemMessage(Component.literal("已读取主手物品到“"
                    + slot.displayName() + "”：" + displayName));
            sendSnapshot(administrator, LoadoutSnapshotPacket.OpenTarget.REFRESH_ADMIN);
        }
    }

    public void editFormationLoadoutRule(ServerPlayer administrator, String factionId,
                                         String formationId, String classId, String slotId,
                                         String entryId, FormationLoadoutEditAction action) {
        if (!requireAdministrator(administrator)) {
            return;
        }
        LoadoutClassDefinition definition = repository.config().findClass(classId).orElse(null);
        LoadoutSlotDefinition slot = definition == null ? null
                : definition.findSlot(slotId).orElse(null);
        if (definition == null || slot == null || action == null) {
            administrator.sendSystemMessage(Component.literal("兵种、槽位或操作不存在"));
            return;
        }
        if (action != FormationLoadoutEditAction.ALLOW_ALL
                && definition.entries(slot.id()).stream()
                .noneMatch(entry -> entry.id().equals(entryId))) {
            administrator.sendSystemMessage(Component.literal("全局装备池中不存在该条目"));
            return;
        }
        FormationService formations = FormationService.get(administrator).orElse(null);
        if (formations == null) {
            administrator.sendSystemMessage(Component.literal("阵营编制服务尚未启动"));
            return;
        }
        ActionResult result = formations.editLoadoutRule(factionId, formationId, classId,
                slotId, entryId, action);
        administrator.sendSystemMessage(Component.literal(result.message()));
        if (result.success()) {
            sendSnapshot(administrator, LoadoutSnapshotPacket.OpenTarget.REFRESH_ADMIN);
        }
    }

    private void includeNewEntryInFormation(ServerPlayer administrator, String factionId,
                                            String formationId, String classId, String slotId,
                                            String entryId) {
        if (factionId == null || factionId.isBlank()
                || formationId == null || formationId.isBlank()) {
            return;
        }
        FormationService formations = FormationService.get(administrator).orElse(null);
        if (formations == null) {
            administrator.sendSystemMessage(Component.literal(
                    "装备已保存到全局池，但阵营编制服务尚未启动，未能加入编制白名单"));
            return;
        }
        ActionResult result = formations.editLoadoutRule(factionId, formationId, classId,
                slotId, entryId, FormationLoadoutEditAction.INCLUDE_CAPTURED);
        if (!result.success()) {
            administrator.sendSystemMessage(Component.literal(
                    "装备已保存到全局池，但加入编制白名单失败：" + result.message()));
        } else {
            administrator.sendSystemMessage(Component.literal("已加入当前编制的严格白名单"));
        }
    }

    static String captureIdBase(String itemPath, CompoundTag tag) {
        String source = Objects.requireNonNullElse(itemPath, "captured_item");
        if (tag != null && tag.contains("GunId", Tag.TAG_STRING)) {
            ResourceLocation gunId = ResourceLocation.tryParse(tag.getString("GunId"));
            if (gunId != null && !gunId.getPath().isBlank()) {
                source = gunId.getPath();
            }
        }
        String normalized = source.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9_.-]+", "_")
                .replaceAll("^_+|_+$", "");
        return normalized.isBlank() ? "captured_item" : normalized;
    }

    static String uniqueCapturedEntryId(String base, List<LoadoutEntry> entries,
                                        String ignoredEntryId) {
        String root = Objects.requireNonNullElse(base, "captured_item");
        String ignored = Objects.requireNonNullElse(ignoredEntryId, "");
        String candidate = root;
        int suffix = 2;
        while (containsEntryId(entries, candidate, ignored)) {
            candidate = root + "_" + suffix++;
        }
        return candidate;
    }

    private static boolean containsEntryId(List<LoadoutEntry> entries, String candidate,
                                           String ignoredEntryId) {
        for (LoadoutEntry entry : entries) {
            if (entry.id().equals(candidate) && !entry.id().equals(ignoredEntryId)) {
                return true;
            }
        }
        return false;
    }

    public void deleteEntry(ServerPlayer administrator, String classId,
                            String slotId, String entryId) {
        if (!requireAdministrator(administrator)) {
            return;
        }
        LoadoutClassDefinition definition = repository.config().findClass(classId).orElse(null);
        LoadoutSlotDefinition slot = definition == null ? null
                : definition.findSlot(slotId).orElse(null);
        if (definition == null || slot == null) {
            return;
        }
        definition.entries(slot.id()).removeIf(entry -> entry.id().equals(entryId));
        repository.saveConfig();
        sendSnapshot(administrator, LoadoutSnapshotPacket.OpenTarget.REFRESH_ADMIN);
    }

    public void editSlot(ServerPlayer administrator, String classId, String slotId,
                         String displayName, LoadoutInventoryTarget target, boolean required,
                         AdminSlotPacket.Action action) {
        if (!requireAdministrator(administrator)) {
            return;
        }
        LoadoutClassDefinition definition = repository.config().findClass(classId).orElse(null);
        if (definition == null || action == null || target == null) {
            administrator.sendSystemMessage(Component.literal("职业、目标栏位或操作不存在"));
            return;
        }
        int definitionIndex = repository.config().classes().indexOf(definition);
        LoadoutClassDefinition before = definition.copy();
        boolean changed;
        switch (action) {
            case CREATE -> changed = definition.addSlot(new LoadoutSlotDefinition(
                    slotId, displayName, target, required));
            case UPDATE -> changed = definition.updateSlot(slotId, displayName, target, required);
            case MOVE_LEFT -> changed = definition.moveSlot(slotId, -1);
            case MOVE_RIGHT -> changed = definition.moveSlot(slotId, 1);
            case DELETE -> changed = definition.deleteSlot(slotId);
            default -> changed = false;
        }
        if (!changed) {
            administrator.sendSystemMessage(Component.literal(switch (action) {
                case CREATE -> "槽位 ID 或目标栏位重复，或已达到槽位上限";
                case UPDATE -> "槽位不存在，或目标栏位已被其他槽位占用";
                case MOVE_LEFT, MOVE_RIGHT -> "槽位已经位于该方向的边界";
                case DELETE -> "槽位不存在";
            }));
            return;
        }
        if (!repository.saveConfig()) {
            if (definitionIndex >= 0 && definitionIndex < repository.config().classes().size()) {
                repository.config().classes().set(definitionIndex, before);
            }
            administrator.sendSystemMessage(Component.literal(
                    "loadouts.json 写入失败；本次槽位修改已回滚"));
            return;
        }
        administrator.sendSystemMessage(Component.literal(switch (action) {
            case CREATE -> "已新增装备槽位：" + displayName;
            case UPDATE -> "已保存装备槽位：" + displayName;
            case MOVE_LEFT, MOVE_RIGHT -> "已调整装备槽位顺序";
            case DELETE -> "已删除装备槽位及其中的候选装备";
        }));
        sendSnapshot(administrator, LoadoutSnapshotPacket.OpenTarget.REFRESH_ADMIN);
    }

    public void editFormationClass(ServerPlayer administrator, String factionId,
                                   String formationId, String classId,
                                   String displayName, int squadLimit,
                                   FormationClassEditAction action) {
        if (!requireAdministrator(administrator)) {
            return;
        }
        if (!validClassId(classId) || action == null) {
            administrator.sendSystemMessage(Component.literal("职业内部 ID 或操作无效"));
            return;
        }
        FormationService formations = FormationService.get(administrator).orElse(null);
        if (formations == null) {
            administrator.sendSystemMessage(Component.literal("阵营编制服务尚未启动"));
            return;
        }

        LoadoutClassDefinition createdBacking = null;
        if (action == FormationClassEditAction.CREATE) {
            if (repository.config().findClass(classId).isPresent()) {
                administrator.sendSystemMessage(Component.literal("职业内部 ID 已存在，请重试"));
                return;
            }
            if (repository.config().classes().size() >= 64) {
                administrator.sendSystemMessage(Component.literal("全局职业数据已达到 64 项上限"));
                return;
            }
            createdBacking = new LoadoutClassDefinition(classId, displayName, true,
                    squadLimit, false);
            repository.config().classes().add(createdBacking);
            if (!repository.saveConfig()) {
                repository.config().classes().remove(createdBacking);
                administrator.sendSystemMessage(Component.literal(
                        "loadouts.json 写入失败；没有创建职业"));
                return;
            }
        } else if (repository.config().findClass(classId).isEmpty()) {
            administrator.sendSystemMessage(Component.literal("职业装备池不存在"));
            return;
        }

        ActionResult result = formations.editClass(factionId, formationId, classId,
                displayName, squadLimit, action);
        if (!result.success() && createdBacking != null) {
            repository.config().classes().remove(createdBacking);
            repository.saveConfig();
        }
        if (result.success() && action == FormationClassEditAction.DELETE
                && !formations.classReferenced(classId)) {
            LoadoutClassDefinition orphan = repository.config().findClass(classId).orElse(null);
            if (orphan != null) {
                repository.config().classes().remove(orphan);
                if (!repository.saveConfig()) {
                    repository.config().classes().add(orphan);
                    administrator.sendSystemMessage(Component.literal(
                            "职业已从编制删除，但未能清理 loadouts.json 中的孤立装备池"));
                }
            }
        }
        administrator.sendSystemMessage(Component.literal(result.message()));
        if (result.success()) {
            sendSnapshot(administrator, LoadoutSnapshotPacket.OpenTarget.REFRESH_ADMIN);
        }
    }

    public void copyClassLoadout(ServerPlayer administrator,
                                 String sourceFactionId, String sourceFormationId,
                                 String sourceClassId, String targetFactionId,
                                 String targetFormationId, String targetClassId) {
        if (!requireAdministrator(administrator)) {
            return;
        }
        FormationService formations = FormationService.get(administrator).orElse(null);
        if (formations == null) {
            administrator.sendSystemMessage(Component.literal("阵营编制服务尚未启动"));
            return;
        }
        LoadoutConfigData previousLoadouts = repository.config().copy();
        ClassLoadoutCopyPlanner.CopyPlan plan;
        try {
            plan = ClassLoadoutCopyPlanner.plan(formations.catalog(), previousLoadouts,
                    sourceFactionId, sourceFormationId, sourceClassId,
                    targetFactionId, targetFormationId, targetClassId,
                    ignored -> "copy_" + UUID.randomUUID().toString()
                            .replace("-", "").substring(0, 16));
        } catch (IllegalArgumentException exception) {
            administrator.sendSystemMessage(Component.literal(exception.getMessage()));
            return;
        }
        if (!repository.replaceConfig(plan.loadouts())) {
            administrator.sendSystemMessage(Component.literal(
                    "loadouts.json 写入失败；没有粘贴兵种配装"));
            return;
        }
        ActionResult result = formations.applyCopiedClassLoadoutCatalog(plan.formations(),
                targetFactionId, targetFormationId,
                plan.replacedClassId(), plan.targetClassId());
        if (!result.success()) {
            boolean rolledBack = repository.replaceConfig(previousLoadouts);
            administrator.sendSystemMessage(Component.literal(result.message()
                    + (rolledBack ? "；loadouts.json 已回滚"
                    : "；警告：loadouts.json 自动回滚失败")));
            return;
        }
        administrator.sendSystemMessage(Component.literal(result.message()));
        sendSnapshot(administrator, LoadoutSnapshotPacket.OpenTarget.REFRESH_ADMIN);
    }

    private LoadoutEntry resolveSelectedEntry(LoadoutClassDefinition definition,
                                               PlayerLoadoutData playerData,
                                               LoadoutSlotDefinition slot) {
        String selectedId = playerData.selectedEntry(definition.id(), slot.id());
        return definition.entries(slot.id()).stream()
                .filter(entry -> entry.id().equals(selectedId))
                .findFirst()
                .orElseGet(() -> definition.entries(slot.id()).stream()
                        .findFirst().orElse(null));
    }

    private void sendSnapshot(ServerPlayer player, LoadoutSnapshotPacket.OpenTarget openTarget) {
        PlayerLoadoutData playerData = repository.player(player.getUUID()).copy();
        for (LoadoutClassDefinition definition : repository.config().classes()) {
            Set<String> slots = definition.slotDefinitions().stream()
                    .map(LoadoutSlotDefinition::id)
                    .collect(java.util.stream.Collectors.toSet());
            playerData.retainSlots(definition.id(), slots);
        }
        BattleService.get(player).map(service -> service.assignedClass(player.getUUID()))
                .filter(classId -> repository.config().findClass(classId).isPresent())
                .ifPresent(playerData::setActiveClassId);
        boolean administratorView = openTarget == LoadoutSnapshotPacket.OpenTarget.ADMIN
                || openTarget == LoadoutSnapshotPacket.OpenTarget.REFRESH_ADMIN;
        LoadoutSnapshot snapshot = new LoadoutSnapshot(
                administratorView ? repository.config().copy() : configForPlayer(player), playerData,
                isAdministrator(player), administratorView
                ? FormationService.get(player).map(FormationService::catalog)
                .orElseGet(FormationConfigData::new)
                : new FormationConfigData());
        LoadoutNetwork.sendToPlayer(player, new LoadoutSnapshotPacket(snapshot, openTarget));
    }

    private ActionResult reserveBattleClass(ServerPlayer player,
                                            LoadoutClassDefinition definition) {
        FormationService formations = FormationService.get(player).orElse(null);
        if (formations == null) {
            return ActionResult.failure(ActionResult.Code.FORMATION_UNAVAILABLE,
                    "阵营编制服务尚未启动");
        }
        int limit = formations.classRule(player.getUUID(), definition.id())
                .map(rule -> {
                    String callsign = BattleService.get(player)
                            .flatMap(service -> service.squadOf(player.getUUID()))
                            .map(value -> value.id()).orElse("");
                    return formations.classLimits(player.getUUID(), callsign)
                            .getOrDefault(rule.classId(), rule.squadLimit());
                }).orElse(0);
        if (limit <= 0) {
            return ActionResult.failure(ActionResult.Code.INVALID_CLASS_ID,
                    "当前编制不允许该兵种");
        }
        return BattleService.get(player)
                .map(service -> service.assignClass(player, definition.id(),
                        limit))
                .orElseGet(() -> ActionResult.failure(ActionResult.Code.NOT_ASSIGNED,
                        "战局服务尚未启动"));
    }

    private LoadoutConfigData configForPlayer(ServerPlayer player) {
        LoadoutConfigData filtered = repository.config().copy();
        FormationService formations = FormationService.get(player).orElse(null);
        filtered.classes().removeIf(definition -> formations == null
                || formations.classRule(player.getUUID(), definition.id()).isEmpty());
        for (LoadoutClassDefinition definition : filtered.classes()) {
            formations.classRule(player.getUUID(), definition.id()).ifPresent(rule -> {
                String scopedName = rule.displayName().isBlank()
                        ? definition.displayName() : rule.displayName();
                definition.updateMetadata(scopedName, true, rule.squadLimit());
            });
            for (LoadoutSlotDefinition slot : definition.slotDefinitions()) {
                 definition.entries(slot.id()).removeIf(entry -> !formations.allowsLoadoutEntry(
                         player.getUUID(), definition.id(), slot.id(), entry.id()));
            }
        }
        FormationDefinition selected = formations == null ? null
                : formations.selectedFormation(player.getUUID()).orElse(null);
        if (selected != null) {
            LinkedHashMap<String, Integer> displayOrder = new LinkedHashMap<>();
            for (int index = 0; index < selected.classes().size(); index++) {
                displayOrder.put(selected.classes().get(index).classId(), index);
            }
            filtered.classes().sort(java.util.Comparator.comparingInt(definition ->
                    displayOrder.getOrDefault(definition.id(), Integer.MAX_VALUE)));
        }
        return filtered;
    }

    private static String scopedClassName(FormationService formations, UUID playerId,
                                          LoadoutClassDefinition definition) {
        String scoped = formations.classDisplayName(playerId, definition.id());
        return scoped.isBlank() ? definition.displayName() : scoped;
    }

    private static boolean validClassId(String classId) {
        if (classId == null || classId.isBlank() || classId.length() > 64) {
            return false;
        }
        for (int index = 0; index < classId.length(); index++) {
            char value = classId.charAt(index);
            if (!(value >= 'a' && value <= 'z') && !(value >= '0' && value <= '9')
                    && value != '_' && value != '-' && value != '.') {
                return false;
            }
        }
        return true;
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

    /**
     * Permission-two map maintenance is the only ordinary-item exception during ACTIVE. Keeping
     * the allow-list here prevents the physical beacon workflow from disabling an administrator's
     * deployment without weakening normal player inventory provenance.
     */
    private static boolean isAdministratorDeploymentTool(ServerPlayer player, ItemStack stack) {
        return player != null && player.hasPermissions(ADMIN_PERMISSION_LEVEL) && stack != null
                && (stack.is(InfantryItems.DEPLOYMENT_BEACON.get())
                || stack.is(InfantryItems.AMMO_SUPPLY_CRATE.get())
                || stack.is(Items.BLUE_DYE) || stack.is(Items.RED_DYE));
    }
}
