package com.wok.infantry.configtransfer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.wok.infantry.formation.FormationConfigData;
import com.wok.infantry.loadout.LoadoutConfigData;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/** Portable administrator-authored definitions only; never includes players or match state. */
public record CatalogArchive(String format, int version, String exportedAt,
                             FormationConfigData formations, LoadoutConfigData loadouts) {
    public static final String FORMAT = "wok_infantry_catalog";
    public static final int VERSION = 1;
    public static final int MAX_BYTES = 8 * 1024 * 1024;
    // Leave room for the player's view and envelope in the existing 1 MiB snapshot protocol.
    private static final int MAX_CATALOG_CHARACTERS = 900_000;
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting()
            .disableHtmlEscaping().create();

    public static CatalogArchive create(FormationConfigData formations, LoadoutConfigData loadouts) {
        return new CatalogArchive(FORMAT, VERSION, Instant.now().toString(),
                formations.copy(), loadouts.copy());
    }

    public byte[] encode() {
        byte[] bytes = GSON.toJson(this).getBytes(StandardCharsets.UTF_8);
        require(bytes.length <= MAX_BYTES, "数据包超过 8 MiB 上限");
        return bytes;
    }

    /** Strict parsing avoids Gson's silent duplicate-key, unknown-field and repair data loss. */
    public static CatalogArchive decode(byte[] bytes) throws IOException {
        require(bytes.length <= MAX_BYTES, "数据包超过 8 MiB 上限");
        String json = StandardCharsets.UTF_8.newDecoder()
                .decode(java.nio.ByteBuffer.wrap(bytes)).toString();
        JsonElement root;
        try (JsonReader reader = new JsonReader(new StringReader(json))) {
            reader.setLenient(false);
            root = readStrict(reader, 0);
            require(reader.peek() == JsonToken.END_DOCUMENT, "JSON 末尾包含多余内容");
        }
        require(root.isJsonObject(), "数据包根节点必须是对象");
        CatalogArchive archive = GSON.fromJson(root, CatalogArchive.class);
        require(FORMAT.equals(archive.format), "不是 WOK步战阵营配装数据包");
        require(archive.version == VERSION, "不支持的数据包版本：" + archive.version);
        require(archive.exportedAt != null, "缺少导出时间");
        require(archive.formations != null && archive.loadouts != null,
                "必须同时包含 formations 与 loadouts");
        require(archive.formations.version() == FormationConfigData.CURRENT_VERSION
                && archive.loadouts.version() == 3, "阵营或配装配置版本不兼容");
        // Capture before copy/accessors, which also normalize some legacy model fields.
        require(root.equals(GSON.toJsonTree(archive)), "字段缺失、类型错误或包含未知字段");
        archive.formations.normalize();
        archive.loadouts.normalize();
        require(!archive.formations.factions().isEmpty(), "数据包没有有效阵营");
        require(root.equals(GSON.toJsonTree(archive)),
                "配置含重复、无效或超限数据；拒绝静默修复，请先修正原文件");
        archive.validateReferences();
        require(root.equals(GSON.toJsonTree(archive)), "配置包含需要修复的字段");
        require(new Gson().toJson(archive.formations).length()
                        + new Gson().toJson(archive.loadouts).length() <= MAX_CATALOG_CHARACTERS,
                "阵营与配装总量超过客户端同步上限（900000 字符）");
        return archive;
    }

    private void validateReferences() {
        for (var faction : formations.factions()) {
            for (var formation : faction.formations()) {
                for (var rule : formation.classes()) {
                    var definition = loadouts.findClass(rule.classId()).orElseThrow(() ->
                            new IllegalArgumentException(faction.id() + "/" + formation.id()
                                    + " 引用了不存在的兵种：" + rule.classId()));
                    rule.allowedEntries().forEach((slot, entries) -> {
                        require(definition.findSlot(slot).isPresent(), "白名单引用不存在的槽位："
                                + rule.classId() + "/" + slot);
                        for (String entry : entries) {
                            require(definition.entries(slot).stream()
                                            .anyMatch(value -> value.id().equals(entry)),
                                    "白名单引用不存在的装备：" + rule.classId() + "/" + slot + "/" + entry);
                        }
                    });
                }
            }
        }
        for (var definition : loadouts.classes()) {
            require(definition.displayName().length() <= 80, "兵种名称过长：" + definition.id());
            for (var slot : definition.slotDefinitions()) {
                require(slot.displayName().length() <= 80, "槽位名称过长：" + slot.id());
                for (var entry : definition.entries(slot.id())) {
                    require(entry.count() >= 1 && entry.count() <= 64,
                            "装备数量必须在 1–64：" + entry.id());
                    require(entry.itemId().matches("[a-z0-9_.-]+:[a-z0-9_./-]+")
                            && entry.itemId().length() <= 256, "物品 ID 无效：" + entry.id());
                    require(entry.displayName().length() <= 80 && entry.snbt().length() <= 32767,
                            "装备名称或 SNBT 过长：" + entry.id());
                }
            }
        }
    }

    public String summary() {
        int formationCount = 0;
        int vehicleCount = 0;
        int slotCount = 0;
        int entryCount = 0;
        for (var faction : formations.factions()) {
            formationCount += faction.formations().size();
            for (var formation : faction.formations()) vehicleCount += formation.vehicles().size();
        }
        for (var definition : loadouts.classes()) {
            slotCount += definition.slotDefinitions().size();
            for (var slot : definition.slotDefinitions()) entryCount += definition.entries(slot.id()).size();
        }
        return formations.factions().size() + " 阵营 / " + formationCount + " 编制 / "
                + loadouts.classes().size() + " 兵种\n" + slotCount + " 槽位 / "
                + entryCount + " 装备 / " + vehicleCount + " 载具配置";
    }

    private static JsonElement readStrict(JsonReader reader, int depth) throws IOException {
        require(depth <= 48, "JSON 嵌套过深");
        switch (reader.peek()) {
            case BEGIN_OBJECT -> {
                JsonObject object = new JsonObject();
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    require(!object.has(name), "JSON 字段重复：" + name);
                    object.add(name, readStrict(reader, depth + 1));
                }
                reader.endObject();
                return object;
            }
            case BEGIN_ARRAY -> {
                JsonArray array = new JsonArray();
                reader.beginArray();
                while (reader.hasNext()) array.add(readStrict(reader, depth + 1));
                reader.endArray();
                return array;
            }
            case STRING -> { return new JsonPrimitive(reader.nextString()); }
            case NUMBER -> { return new JsonPrimitive(new BigDecimal(reader.nextString())); }
            case BOOLEAN -> { return new JsonPrimitive(reader.nextBoolean()); }
            case NULL -> { reader.nextNull(); return JsonNull.INSTANCE; }
            default -> throw new IllegalArgumentException("JSON 内容不完整");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}
