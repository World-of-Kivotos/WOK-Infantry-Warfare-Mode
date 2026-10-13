package com.wok.infantry.client.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Font and language stand-ins for HUD unit tests (no Minecraft client needed). */
final class HudTestSupport {
    static final String EN_US = "assets/wok_infantry/lang/en_us.json";
    static final String ZH_CN = "assets/wok_infantry/lang/zh_cn.json";
    private static final Pattern ARGUMENT = Pattern.compile("%(?:(\\d+)\\$)?s");

    private HudTestSupport() {
    }

    /** Width like the vanilla font: ASCII 6px, everything else (CJK, ★, …) 9px. */
    static int width(String text) {
        int width = 0;
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            width += codePoint < 0x80 ? 6 : 9;
            i += Character.charCount(codePoint);
        }
        return width;
    }

    static Map<String, String> bundle(String resource) {
        try (InputStream stream = HudTestSupport.class.getClassLoader()
                .getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalStateException("missing " + resource);
            }
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, String> result = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                result.put(entry.getKey(), entry.getValue().getAsString());
            }
            return result;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /** Renders {@code component} with {@code bundle}; a missing key renders as "!key!". */
    static String render(Component component, Map<String, String> bundle) {
        StringBuilder out = new StringBuilder();
        if (component.getContents() instanceof TranslatableContents translatable) {
            String pattern = bundle.get(translatable.getKey());
            if (pattern == null) {
                out.append('!').append(translatable.getKey()).append('!');
            } else {
                Object[] args = translatable.getArgs();
                Matcher matcher = ARGUMENT.matcher(pattern);
                int implicit = 0;
                StringBuilder formatted = new StringBuilder();
                while (matcher.find()) {
                    int index = matcher.group(1) == null ? implicit++
                            : Integer.parseInt(matcher.group(1)) - 1;
                    Object arg = index < args.length ? args[index] : "?";
                    String value = arg instanceof Component nested ? render(nested, bundle)
                            : String.valueOf(arg);
                    matcher.appendReplacement(formatted, Matcher.quoteReplacement(value));
                }
                matcher.appendTail(formatted);
                out.append(formatted);
            }
        } else if (component.getContents() instanceof LiteralContents literal) {
            out.append(literal.text());
        }
        for (Component sibling : component.getSiblings()) {
            out.append(render(sibling, bundle));
        }
        return out.toString();
    }

    /** Every translation key used by {@code component}, its arguments and siblings. */
    static Set<String> keys(Component component) {
        Set<String> keys = new TreeSet<>();
        collect(component, keys);
        return keys;
    }

    private static void collect(Component component, Set<String> keys) {
        if (component.getContents() instanceof TranslatableContents translatable) {
            keys.add(translatable.getKey());
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Component nested) {
                    collect(nested, keys);
                }
            }
        }
        for (Component sibling : component.getSiblings()) {
            collect(sibling, keys);
        }
    }
}
