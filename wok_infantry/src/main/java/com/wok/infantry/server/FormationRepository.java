package com.wok.infantry.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.formation.FormationConfigData;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/** Owns the single server configuration file for the faction/formation catalog. */
final class FormationRepository {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
            .disableHtmlEscaping().create();

    private final Path configPath;
    private FormationConfigData config = new FormationConfigData();

    FormationRepository() {
        this(FMLPaths.CONFIGDIR.get().resolve("wok_infantry").resolve("formations.json"));
    }

    FormationRepository(Path configPath) {
        this.configPath = Objects.requireNonNull(configPath, "configPath");
    }

    /**
     * Parses an existing catalog without ever replacing it on a read/JSON failure.
     * Defaults are written only when the file does not exist yet.
     */
    synchronized LoadResult load() {
        if (!Files.isRegularFile(configPath)) {
            FormationConfigData defaults = FormationConfigData.defaultConfig();
            config = defaults;
            boolean persisted = save(defaults);
            return LoadResult.success(config, persisted
                    ? "已创建默认阵营编制配置"
                    : "默认阵营编制配置仅在内存中生效；写入文件失败");
        }

        FormationConfigData loaded;
        try {
            loaded = readExisting();
        } catch (IOException | RuntimeException exception) {
            WokInfantryMod.LOGGER.error(
                    "Failed to parse formation configuration from {}; retaining active catalog",
                    configPath, exception);
            return LoadResult.failure(config,
                    "formations.json 读取或解析失败；已保留当前生效目录与原文件");
        }
        loaded.normalize();
        if (loaded.factions().isEmpty()) {
            WokInfantryMod.LOGGER.error(
                    "Formation configuration at {} has no valid factions; retaining active catalog",
                    configPath);
            return LoadResult.failure(config,
                    "formations.json 没有有效阵营；已保留当前生效目录与原文件");
        }
        config = loaded;
        return LoadResult.success(config, "阵营编制配置解析成功");
    }

    synchronized FormationConfigData config() {
        return config.copy();
    }

    /** Persists a validated copy and publishes it only after the atomic file replace succeeds. */
    synchronized boolean replace(FormationConfigData replacement) {
        if (replacement == null) {
            return false;
        }
        FormationConfigData normalized = replacement.normalizedCopy();
        if (normalized.factions().isEmpty() || !save(normalized)) {
            return false;
        }
        config = normalized;
        return true;
    }

    synchronized Path path() {
        return configPath;
    }

    private FormationConfigData readExisting() throws IOException {
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            FormationConfigData parsed = GSON.fromJson(reader, FormationConfigData.class);
            if (parsed == null) {
                throw new IllegalArgumentException("formation configuration root is null");
            }
            return parsed;
        }
    }

    private boolean save(FormationConfigData target) {
        try {
            Files.createDirectories(configPath.getParent());
            Path temporary = configPath.resolveSibling(configPath.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(target, writer);
            }
            try {
                Files.move(temporary, configPath, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, configPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException exception) {
            WokInfantryMod.LOGGER.error("Failed to save formation configuration to {}",
                    configPath, exception);
            return false;
        }
    }

    record LoadResult(boolean success, FormationConfigData config, String message) {
        LoadResult {
            Objects.requireNonNull(config, "config");
            message = Objects.requireNonNullElse(message, "");
            config = config.copy();
        }

        @Override
        public FormationConfigData config() {
            return config.copy();
        }

        static LoadResult success(FormationConfigData config, String message) {
            return new LoadResult(true, config, message);
        }

        static LoadResult failure(FormationConfigData config, String message) {
            return new LoadResult(false, config, message);
        }
    }
}
