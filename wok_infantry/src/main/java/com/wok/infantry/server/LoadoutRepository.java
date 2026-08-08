package com.wok.infantry.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.loadout.LoadoutConfigData;
import com.wok.infantry.loadout.PlayerLoadoutData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

final class LoadoutRepository {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path configPath;
    private final Path playerPath;
    private LoadoutConfigData config;
    private PlayerStore players;

    LoadoutRepository(MinecraftServer server) {
        configPath = FMLPaths.CONFIGDIR.get().resolve("wok_infantry").resolve("loadouts.json");
        playerPath = server.getWorldPath(LevelResource.ROOT)
                .resolve("data").resolve("wok_infantry").resolve("player_loadouts.json");
    }

    synchronized void load() {
        config = read(configPath, LoadoutConfigData.class, LoadoutConfigData.defaultConfig());
        config.normalize();
        players = read(playerPath, PlayerStore.class, new PlayerStore());
        players.normalize();
        saveConfig();
    }

    synchronized LoadoutConfigData config() {
        return config;
    }

    synchronized PlayerLoadoutData player(UUID playerId) {
        return players.values.computeIfAbsent(playerId.toString(), ignored -> new PlayerLoadoutData());
    }

    synchronized void saveConfig() {
        write(configPath, config);
    }

    synchronized void savePlayers() {
        write(playerPath, players);
    }

    private static <T> T read(Path path, Class<T> type, T fallback) {
        if (!Files.isRegularFile(path)) {
            return fallback;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            T value = GSON.fromJson(reader, type);
            return value == null ? fallback : value;
        } catch (Exception exception) {
            WokInfantryMod.LOGGER.error("Failed to read loadout data from {}", path, exception);
            return fallback;
        }
    }

    private static void write(Path path, Object value) {
        try {
            Files.createDirectories(path.getParent());
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(value, writer);
            }
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            WokInfantryMod.LOGGER.error("Failed to save loadout data to {}", path, exception);
        }
    }

    private static final class PlayerStore {
        private Map<String, PlayerLoadoutData> values = new LinkedHashMap<>();

        private void normalize() {
            if (values == null) {
                values = new LinkedHashMap<>();
            }
            values.entrySet().removeIf(entry -> entry.getValue() == null);
        }
    }
}
