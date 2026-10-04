package com.wok.infantry.client.ui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the shared-component rules of the WOK步战 UI in the client sources:
 * <ul>
 *   <li>clip only through {@code UiScale.enableScissor} (pose-aware, and reported to the layout
 *       probe), never {@code GuiGraphics.enableScissor} directly;</li>
 *   <li>no {@code drawCenteredString} (it always draws a text shadow);</li>
 *   <li>confirmations as {@code TacticalConfirmDialog} modal layers, not vanilla
 *       {@code ConfirmScreen} (which re-initialises the parent and loses its drafts).</li>
 * </ul>
 * Screens that still wait for their batch are listed in {@link #ALLOWED} with the batch that
 * migrates them; the list must only shrink (an entry that no longer matches fails too, so it is
 * removed together with the fix). It is meant to be empty after tier 3.
 */
class UiArchitectureTest {
    private static final Path CLIENT_SOURCES = Path.of("src/main/java/com/wok/infantry/client");

    private record Rule(String id, Pattern pattern, String hint) {
    }

    private static final List<Rule> RULES = List.of(
            new Rule("direct-scissor", Pattern.compile("\\bgraphics\\.enableScissor\\("),
                    "clip with UiScale.enableScissor"),
            new Rule("shadow-centered-text", Pattern.compile("\\.drawCenteredString\\("),
                    "draw with TextFit (no shadow)"),
            new Rule("vanilla-confirm", Pattern.compile("new ConfirmScreen\\("),
                    "open a TacticalConfirmDialog with TacticalScreen.openModal"));

    /** file (relative to the client package) + rule → the batch that removes it. */
    private static final Map<String, String> ALLOWED = Map.of(
            "screen/UiScale.java#direct-scissor", "the pose-aware wrapper itself",
            "screen/CatalogTransferScreen.java#direct-scissor", "B10 (catalog transfer)",
            "screen/TacticalMapScreen.java#direct-scissor", "B7 (tactical map, tier 2)",
            "screen/AdminLoadoutScreen.java#vanilla-confirm", "B10 (administrator terminal)");

    @Test
    void clientSourcesUseTheSharedClippingTextAndConfirmationComponents() throws IOException {
        assertTrue(Files.isDirectory(CLIENT_SOURCES),
                () -> "run from the module directory; missing " + CLIENT_SOURCES.toAbsolutePath());
        Map<String, List<String>> found = new TreeMap<>();
        try (Stream<Path> files = Files.walk(CLIENT_SOURCES)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String relative = CLIENT_SOURCES.relativize(file).toString().replace('\\', '/');
                List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                for (Rule rule : RULES) {
                    for (int index = 0; index < lines.size(); index++) {
                        if (rule.pattern().matcher(lines.get(index)).find()) {
                            found.computeIfAbsent(relative + "#" + rule.id(),
                                            key -> new ArrayList<>())
                                    .add((index + 1) + ": " + lines.get(index).trim()
                                            + "  -> " + rule.hint());
                        }
                    }
                }
            }
        }
        List<String> problems = new ArrayList<>();
        found.forEach((key, hits) -> {
            if (!ALLOWED.containsKey(key)) {
                problems.add(key + " " + hits);
            }
        });
        ALLOWED.forEach((key, owner) -> {
            if (!found.containsKey(key)) {
                problems.add(key + " no longer occurs: remove it from ALLOWED (" + owner + ")");
            }
        });
        assertTrue(problems.isEmpty(), () -> String.join("\n", problems));
    }
}
