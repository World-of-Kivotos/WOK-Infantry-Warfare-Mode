package com.wok.bodyhealth.prone;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The pure prone classes must stay loadable without Forge, a level or any optional mod: they may
 * only use the JDK, Gson, Minecraft's phys package, Mth and BodyPart.
 */
final class PureLayerImportsTest {
    private static final List<String> PURE_CLASSES = List.of(
            "SegmentId", "LocalObb", "BodyLayout", "ProneSegmentTables", "VanillaCrawlLayout",
            "ProneMode", "ProneSample", "ProneHistory", "ProneRewind", "RewindPolicy", "TaaPhaseMath", "BodyYawSim",
            "ProneLayouts", "WorldObb", "ProneGeometry", "ProneSegmentClip", "OcclusionTest", "ProneHitSettings",
            "ProneHitResult", "ProneHitLedger", "ProneHitMemory", "ProneScope", "ProneDecision");
    /** Same-package glue that touches Forge or entities; pure classes must not reach for it. */
    private static final Pattern GLUE = Pattern.compile("\\b(ProneHitService|ProneHistoryTracker|ProneSampler"
            + "|TaaProneBridge|ProneProjectileKind|ProneConsumer|ProneCandidateInjector|ProneMixinStatus"
            + "|ProneMixinGuard|ProneHitboxDebug)\\b");
    private static final Pattern COMMENTS = Pattern.compile("/\\*.*?\\*/|//[^\\n]*", Pattern.DOTALL);
    private static final Pattern IMPORT = Pattern.compile("^import\\s+(?:static\\s+)?([\\w.]+)", Pattern.MULTILINE);
    private static final Pattern QUALIFIED = Pattern.compile(
            "\\b(net\\.minecraft(?:forge)?\\.[\\w.]+|com\\.(?:tacz|atsuishio|locknar)\\.[\\w.]+"
                    + "|org\\.(?:apache|slf4j|spongepowered)\\.[\\w.]+|javax\\.[\\w.]+|com\\.wok\\.[\\w.]+)");

    @Test
    void pureClassesOnlyUseWhitelistedPackages() throws IOException {
        Path dir = Path.of("src/main/java/com/wok/bodyhealth/prone");
        assumeTrue(Files.isDirectory(dir), "run from the wok_body_health project directory");
        List<String> problems = new ArrayList<>();
        for (String name : PURE_CLASSES) {
            Path file = dir.resolve(name + ".java");
            assertTrue(Files.isRegularFile(file), "missing " + file);
            String source = COMMENTS.matcher(Files.readString(file, StandardCharsets.UTF_8)).replaceAll(" ");
            Matcher glue = GLUE.matcher(source);
            while (glue.find()) {
                problems.add(name + " uses glue class " + glue.group(1));
            }
            Matcher imports = IMPORT.matcher(source);
            while (imports.find()) {
                if (!allowed(imports.group(1))) {
                    problems.add(name + " imports " + imports.group(1));
                }
            }
            Matcher qualified = QUALIFIED.matcher(source.replaceAll("(?m)^(?:import|package) .*$", ""));
            while (qualified.find()) {
                if (!allowed(qualified.group(1))) {
                    problems.add(name + " references " + qualified.group(1));
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    private static boolean allowed(String name) {
        return name.startsWith("java.")
                || name.startsWith("com.google.gson.")
                || name.startsWith("net.minecraft.world.phys.")
                || name.equals("net.minecraft.util.Mth")
                || name.equals("com.wok.bodyhealth.health.BodyPart")
                || name.startsWith("com.wok.bodyhealth.prone.");
    }
}
