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
    private static final int MAX_PLAYER_RECORDS = 4_096;

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
        String key = playerId.toString();
        PlayerLoadoutData existing = players.values.get(key);
        if (existing != null) {
            return existing;
        }
        while (players.values.size() >= MAX_PLAYER_RECORDS) {
            java.util.Iterator<String> iterator = players.values.keySet().iterator();
            if (!iterator.hasNext()) {
                break;
            }
            iterator.next();
            iterator.remove();
        }
        PlayerLoadoutData created = new PlayerLoadoutData();
        players.values.put(key, created);
        return created;
    }

    synchronized boolean saveConfig() {
        return write(configPath, config);
    }

    /** Atomically persists and publishes a complete replacement configuration. */
    synchronized boolean replaceConfig(LoadoutConfigData replacement) {
        if (replacement == null) {
            return false;
        }
        LoadoutConfigData candidate = replacement.copy();
        candidate.normalize();
        if (!write(configPath, candidate)) {
            return false;
        }
        config = candidate;
        return true;
    }

    synchronized boolean savePlayers() {
        return write(playerPath, players);
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

    private static boolean write(Path path, Object value) {
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
            return true;
        } catch (IOException exception) {
            WokInfantryMod.LOGGER.error("Failed to save loadout data to {}", path, exception);
            return false;
        }
    }

    private static final class PlayerStore {
        private Map<String, PlayerLoadoutData> values = new LinkedHashMap<>();

        private void normalize() {
            if (values == null) {
                values = new LinkedHashMap<>();
            }
            values.entrySet().removeIf(entry -> entry.getValue() == null);
            while (values.size() > MAX_PLAYER_RECORDS) {
                java.util.Iterator<String> iterator = values.keySet().iterator();
                if (!iterator.hasNext()) {
                    break;
                }
                iterator.next();
                iterator.remove();
            }
        }
    }
}
