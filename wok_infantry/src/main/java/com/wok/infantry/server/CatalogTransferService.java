package com.wok.infantry.server;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.configtransfer.CatalogArchive;
import com.wok.infantry.configtransfer.CatalogFiles;
import com.wok.infantry.configtransfer.CatalogTransferAction;
import com.wok.infantry.configtransfer.CatalogTransferResult;
import com.wok.infantry.network.battle.BattleNetwork;
import com.wok.infantry.network.battle.BattleOpenTarget;
import com.wok.infantry.network.formation.FormationNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Command and packet entry points share permission, preview and transaction checks. */
public final class CatalogTransferService {
    private static final int PAGE_SIZE = 8;
    private final MinecraftServer server;
    private final LoadoutRepository repository;
    private final LoadoutService loadouts;
    private final CatalogFiles files;
    private final Map<String, Preview> previews = new HashMap<>();

    CatalogTransferService(MinecraftServer server, LoadoutRepository repository, LoadoutService loadouts) {
        this.server = server;
        this.repository = repository;
        this.loadouts = loadouts;
        files = new CatalogFiles(repository.configPath().getParent());
    }

    public CatalogTransferResult execute(CommandSourceStack source, CatalogTransferAction action,
                                         String name, String token, int page) {
        if (!source.hasPermission(LoadoutService.ADMIN_PERMISSION_LEVEL)) {
            return CatalogTransferResult.message(false, "只有管理员可以导入导出阵营配装", "");
        }
        if (!server.isSameThread()) throw new IllegalStateException("Catalog transfer must run on server thread");
        try {
            var formations = FormationService.get(server).orElseThrow(() ->
                    new IllegalArgumentException("阵营编制服务尚未启动"));
            String actor = source.getEntity() == null ? "console" : source.getEntity().getStringUUID();
            previews.entrySet().removeIf(entry -> entry.getValue().expiresAt < System.currentTimeMillis());
            return switch (action) {
                case LIST -> {
                    var names = files.list();
                    int pages = Math.max(1, (names.size() + PAGE_SIZE - 1) / PAGE_SIZE);
                    int actualPage = Math.max(0, Math.min(pages - 1, page));
                    yield new CatalogTransferResult(true, "服务端目录：config/wok_infantry/catalogs", "",
                            names.subList(actualPage * PAGE_SIZE, Math.min(names.size(), (actualPage + 1) * PAGE_SIZE)),
                            actualPage, pages);
                }
                case EXPORT -> {
                    CatalogArchive archive = snapshot(formations);
                    String exported = files.export(name, archive.encode());
                    yield CatalogTransferResult.message(true, "已导出 " + exported + "\n" + archive.summary(), "");
                }
                case PREVIEW -> {
                    previews.remove(actor);
                    byte[] bytes = files.read(name);
                    CatalogArchive archive = validate(bytes);
                    String previewToken = UUID.randomUUID().toString();
                    if (previews.size() >= 64) previews.clear();
                    previews.put(actor, new Preview(CatalogFiles.fileName(name), CatalogFiles.digest(bytes),
                            revision(formations), previewToken, System.currentTimeMillis() + 300_000));
                    yield CatalogTransferResult.message(true, archive.summary()
                            + "\n将整体替换管理员配置；先自动备份，玩家记录保留。", previewToken);
                }
                case IMPORT -> {
                    Preview preview = previews.remove(actor);
                    if (preview == null || !preview.name.equals(CatalogFiles.fileName(name))
                            || !preview.token.equals(token)) {
                        throw new IllegalArgumentException("请先预览此数据包，再确认导入（预览有效期 5 分钟）");
                    }
                    byte[] bytes = files.read(name);
                    if (!preview.digest.equals(CatalogFiles.digest(bytes))
                            || !preview.revision.equals(revision(formations))) {
                        throw new IllegalArgumentException("数据包或当前配置在预览后已变化，请重新预览");
                    }
                    CatalogArchive archive = validate(bytes);
                    String backupName = "before-import-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                            .withZone(ZoneOffset.UTC).format(Instant.now()) + "-" + UUID.randomUUID().toString().substring(0, 8);
                    String backup = files.export(backupName, snapshot(formations).encode());
                    files.replace(archive);
                    repository.publishImported(archive.loadouts());
                    var applied = formations.publishImportedCatalog(archive.formations());
                    previews.clear();
                    String warning = applied.success() ? "" : "\n" + applied.message();
                    try { refreshClients(formations); }
                    catch (RuntimeException exception) {
                        WokInfantryMod.LOGGER.error("Catalog imported but client refresh failed", exception);
                        warning += "\n配置已保存，部分客户端刷新失败，请重新打开界面或重新连接";
                    }
                    WokInfantryMod.LOGGER.info("{} imported catalog {}; backup={}", source.getTextName(), name, backup);
                    yield CatalogTransferResult.message(true, "已导入 " + preview.name
                            + "\n备份：" + backup + warning, "");
                }
            };
        } catch (IOException | RuntimeException exception) {
            WokInfantryMod.LOGGER.warn("Catalog {} failed for {}", action, source.getTextName(), exception);
            String message = exception.getMessage() == null ? "文件读取或配置校验失败" : exception.getMessage();
            return CatalogTransferResult.message(false, message.substring(0, Math.min(1500, message.length())), "");
        }
    }

    private CatalogArchive validate(byte[] bytes) throws IOException {
        CatalogArchive archive = CatalogArchive.decode(bytes);
        for (var definition : archive.loadouts().classes()) {
            for (var slot : definition.slotDefinitions()) {
                for (var entry : definition.entries(slot.id())) {
                    var result = LoadoutStackFactory.validate(entry);
                    if (!result.valid()) throw new IllegalArgumentException(
                            definition.id() + "/" + slot.id() + "/" + entry.id() + "：" + result.message());
                }
            }
        }
        return archive;
    }

    private CatalogArchive snapshot(FormationService formations) {
        return CatalogArchive.create(formations.catalog(), repository.config());
    }

    private String revision(FormationService formations) {
        return CatalogFiles.digest((CatalogArchive.GSON.toJson(formations.catalog())
                + CatalogArchive.GSON.toJson(repository.config())).getBytes(StandardCharsets.UTF_8));
    }

    private void refreshClients(FormationService formations) {
        loadouts.refreshCatalogClients();
        if (!FormationNetwork.isInitialized()) return;
        server.getPlayerList().getPlayers().forEach(player -> {
            boolean required = formations.snapshotFor(player).selectionRequired();
            if (BattleNetwork.isInitialized()) {
                if (required) BattleNetwork.sendClearToPlayer(player);
                else BattleService.get(player).ifPresent(battle ->
                        BattleNetwork.sendSnapshotToPlayer(battle, player, BattleOpenTarget.NONE));
            }
            FormationNetwork.sendSnapshotToPlayer(player, false);
        });
    }

    private record Preview(String name, String digest, String revision, String token, long expiresAt) {}
}
