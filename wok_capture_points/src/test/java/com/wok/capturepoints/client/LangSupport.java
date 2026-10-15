package com.wok.capturepoints.client;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** This add-on's language files for unit tests (no Minecraft client needed). */
public final class LangSupport {
    public static final String EN_US = "assets/wok_capture_points/lang/en_us.json";
    public static final String ZH_CN = "assets/wok_capture_points/lang/zh_cn.json";
    public static final Pattern ARGUMENT = Pattern.compile("%(?:(\\d+)\\$)?s");

    private LangSupport() {
    }

    public static Map<String, String> bundle(String resource) {
        try (InputStream stream = LangSupport.class.getClassLoader()
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
    public static String render(Component component, Map<String, String> bundle) {
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
}
