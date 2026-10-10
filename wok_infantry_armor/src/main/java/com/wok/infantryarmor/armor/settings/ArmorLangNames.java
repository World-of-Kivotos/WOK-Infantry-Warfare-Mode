package com.wok.infantryarmor.armor.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 模组自带的 zh_cn.json 译名，只读一次。
 *
 * <p>用途有两个：生成逐件配置文件每节注释里的中文名（同一个 JAR 在每台机器上得到逐字相同的注释），
 * 以及让管理员在配置里直接写材质、类型、部位的中文名。读不到文件时返回空表，调用方回退到英文 ID。</p>
 */
public final class ArmorLangNames {

    private static final Logger LOGGER = LoggerFactory.getLogger("wok_infantry_armor");
    private static final String RESOURCE = "/assets/wok_infantry_armor/lang/zh_cn.json";

    private ArmorLangNames() {
    }

    /** 译名；没有该键时返回 fallback。 */
    public static String get(String key, String fallback) {
        String value = Holder.NAMES.get(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    public static String itemName(String itemId) {
        return get("item.wok_infantry_armor." + itemId, itemId);
    }

    private static Map<String, String> load() {
        try (InputStream stream = ArmorLangNames.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                LOGGER.warn("Bundled {} not found; armor config comments fall back to item IDs.", RESOURCE);
                return Map.of();
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
                Map<String, String> names = new HashMap<>();
                for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        names.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
                return Collections.unmodifiableMap(names);
            }
        } catch (Exception exception) {
            LOGGER.warn("Unable to read bundled {}; armor config comments fall back to item IDs.", RESOURCE,
                    exception);
            return Map.of();
        }
    }

    private static final class Holder {
        private static final Map<String, String> NAMES = load();
    }
}
