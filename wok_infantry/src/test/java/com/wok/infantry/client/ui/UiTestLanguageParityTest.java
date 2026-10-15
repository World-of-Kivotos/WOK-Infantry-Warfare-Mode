package com.wok.infantry.client.ui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The uiTest-only language files ({@code src/uiTest/resources/assets/wok_uitest/lang}) of the
 * component gallery stay paired like the production bundle: same keys, no blank values and the
 * same format arguments in Chinese and English.
 */
class UiTestLanguageParityTest {
    private static final Path LANG = Path.of("src/uiTest/resources/assets/wok_uitest/lang");
    private static final Pattern ARGUMENT = Pattern.compile("%(?:\\d+\\$)?s");

    @Test
    void chineseAndEnglishGalleryLabelsArePaired() throws IOException {
        Map<String, String> chinese = read("zh_cn.json");
        Map<String, String> english = read("en_us.json");
        assertEquals(chinese.keySet(), english.keySet(), "zh_cn and en_us gallery keys differ");
        chinese.forEach((key, value) -> {
            assertFalse(value.isBlank(), "blank zh_cn value " + key);
            assertFalse(english.get(key).isBlank(), "blank en_us value " + key);
            assertEquals(arguments(value), arguments(english.get(key)),
                    "format arguments differ for " + key);
        });
    }

    private static Map<String, String> read(String file) throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(LANG.resolve(file),
                StandardCharsets.UTF_8)).getAsJsonObject();
        Map<String, String> values = new TreeMap<>();
        root.entrySet().forEach(entry -> values.put(entry.getKey(),
                entry.getValue().getAsString()));
        return values;
    }

    private static int arguments(String value) {
        Matcher matcher = ARGUMENT.matcher(value);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
