package com.wok.capturepoints.client;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaptureLanguageTest {
    @Test
    void chineseAndEnglishHaveTheSameKeysAndArguments() {
        Map<String, String> zh = LangSupport.bundle(LangSupport.ZH_CN);
        Map<String, String> en = LangSupport.bundle(LangSupport.EN_US);
        assertEquals(en.keySet(), zh.keySet());
        for (String key : en.keySet()) {
            assertFalse(zh.get(key).isBlank() || en.get(key).isBlank(), key);
            assertEquals(arguments(en.get(key)), arguments(zh.get(key)), key);
        }
    }

    @Test
    void everyHudKeyExists() {
        Map<String, String> zh = LangSupport.bundle(LangSupport.ZH_CN);
        for (String key : List.of(CaptureHudModel.DISABLED_KEY, CaptureHudModel.CONTESTED_KEY,
                CaptureHudModel.LOCKED_KEY, CaptureHudModel.CAPTURING_KEY,
                CaptureHudModel.CAPTURING_PLAIN_KEY, CaptureHudModel.SECURED_KEY,
                CaptureHudModel.NEUTRAL_KEY, CaptureHudModel.REMAINING_KEY,
                "team.wok_capture_points.blue", "team.wok_capture_points.red",
                "team.wok_capture_points.neutral", "hud.wok_capture_points.ratio")) {
            assertTrue(zh.containsKey(key), key);
        }
    }

    private static int arguments(String value) {
        Matcher matcher = LangSupport.ARGUMENT.matcher(value);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
