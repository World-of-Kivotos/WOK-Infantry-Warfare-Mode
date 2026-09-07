package com.wok.infantry.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.battle.TacticalMarkerType;
import com.wok.infantry.deployment.DeploymentPhase;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Protects the client UI from missing labels and incompatible format arguments. */
class ClientTranslationContractTest {
    private static final String EN_US = "assets/wok_infantry/lang/en_us.json";
    private static final String ZH_CN = "assets/wok_infantry/lang/zh_cn.json";
    private static final Pattern STRING_ARGUMENT = Pattern.compile("%(?:(\\d+)\\$)?s");
    private static final Pattern OWN_LITERAL_KEY = Pattern.compile(
            "(?:key\\.categories\\.wok_infantry|"
                    + "(?:screen|gui|key|hud|role|faction|squad|class|marker|message)"
                    + "\\.wok_infantry(?:\\.[a-z0-9_]+)+)");
    private static final Map<String, Integer> EXPECTED_ARGUMENT_COUNTS = Map.ofEntries(
            Map.entry("screen.wok_infantry.faction_population", 2),
            Map.entry("screen.wok_infantry.enemy_population", 2),
            Map.entry("screen.wok_infantry.squad.member_count", 2),
            Map.entry("screen.wok_infantry.class.quota", 2),
            Map.entry("screen.wok_infantry.deployment.waiting", 1),
            Map.entry("screen.wok_infantry.deployment.main_base", 4),
            Map.entry("screen.wok_infantry.deployment.field_beacon", 4),
            Map.entry("screen.wok_infantry.deployment.rally", 4),
            Map.entry("screen.wok_infantry.deployment.resupply_cooldown", 1),
            Map.entry("screen.wok_infantry.map.marker_source_ttl", 2),
            Map.entry("screen.wok_infantry.map.footer", 2),
            Map.entry("screen.wok_infantry.map.support.impact_radius", 1),
            Map.entry("screen.wok_infantry.map.support.scan_radius", 1),
            Map.entry("screen.wok_infantry.map.terrain.loading", 2),
            Map.entry("screen.wok_infantry.map.terrain.failed", 2),
            Map.entry("screen.wok_infantry.loadout.choices", 1),
            Map.entry("screen.wok_infantry.loadout.page", 2),
            Map.entry("screen.wok_infantry.loadout.quantity", 1),
            Map.entry("screen.wok_infantry.ammo_supply.points", 2),
            Map.entry("screen.wok_infantry.ammo_supply.detail", 5),
            Map.entry("screen.wok_infantry.ammo_supply.vehicle_title", 2),
            Map.entry("screen.wok_infantry.ammo_supply.vehicle_detail", 4),
            Map.entry("screen.wok_infantry.ammo_supply.rounds", 1),
            Map.entry("message.wok_infantry.ammo_supply.success", 3),
            Map.entry("message.wok_infantry.ammo_supply.partial", 3),
            Map.entry("message.wok_infantry.ammo_supply.at_limit", 2),
            Map.entry("message.wok_infantry.ammo_supply.cooldown", 1),
            Map.entry("message.wok_infantry.ammo_supply.refilled", 1),
            Map.entry("message.wok_infantry.ammo_supply.selected_at_limit", 2),
            Map.entry("message.wok_infantry.ammo_supply.points_insufficient", 2),
            Map.entry("message.wok_infantry.ammo_supply.selected_success", 4),
            Map.entry("message.wok_infantry.ammo_supply.vehicle_cannot_supply", 2),
            Map.entry("message.wok_infantry.ammo_supply.vehicle_success", 5),
            Map.entry("message.wok_infantry.supply_transport.unauthorized", 1),
            Map.entry("message.wok_infantry.supply_transport.cargo_unavailable", 1),
            Map.entry("message.wok_infantry.supply_transport.empty", 3),
            Map.entry("message.wok_infantry.supply_transport.inventory_full", 1),
            Map.entry("message.wok_infantry.supply_transport.taken", 4),
            Map.entry("message.wok_infantry.supply_transport.taken_heavy", 5),
            Map.entry("message.wok_infantry.deployment_beacon.bound", 1),
            Map.entry("message.wok_infantry.vehicle_deployment.bound", 2),
            Map.entry("message.wok_infantry.vehicle_deployment.unbound", 1),
            Map.entry("message.wok_infantry.rally_radio.status", 3),
            Map.entry("message.wok_infantry.attack_direction_length", 2),
            Map.entry("message.wok_infantry.marker_created", 1),
            Map.entry("message.wok_infantry.weapon_ads.applied", 3),
            Map.entry("message.wok_infantry.weapon_ads.reset", 1),
            Map.entry("message.wok_infantry.weapon_tuning.applied", 1),
            Map.entry("message.wok_infantry.weapon_tuning.reset", 1),
            Map.entry("message.wok_infantry.stamina.admin.status", 2),
            Map.entry("message.wok_infantry.stamina.admin.set", 2),
            Map.entry("hud.wok_infantry.squad_count", 2));
    private static final Map<String, String> EXPECTED_ENGLISH_COMPACT_LABELS = Map.ofEntries(
            Map.entry("screen.wok_infantry.tab.map_short", "Map"),
            Map.entry("gui.wok_infantry.leave_squad_short", "Leave"),
            Map.entry("gui.wok_infantry.transfer_leader_short", "Promote"),
            Map.entry("gui.wok_infantry.claim_commander_short", "Command"),
            Map.entry("gui.wok_infantry.resign_commander_short", "Resign"),
            Map.entry("gui.wok_infantry.transfer_commander_short", "Transfer"),
            Map.entry("gui.wok_infantry.map.center_short", "Center"),
            Map.entry("screen.wok_infantry.map.footer_compact", "RMB cancel · pan · zoom"),
            Map.entry("screen.wok_infantry.map.terrain.grid_short", "Grid"),
            Map.entry("screen.wok_infantry.map.terrain.journeymap_short", "JM"),
            Map.entry("screen.wok_infantry.map.terrain.xaero_short", "Xaero"),
            Map.entry("screen.wok_infantry.map.terrain.initializing_short", "Map Init"));
    private static final Map<String, String> EXPECTED_CHINESE_COMPACT_LABELS = Map.ofEntries(
            Map.entry("screen.wok_infantry.tab.map_short", "地图"),
            Map.entry("gui.wok_infantry.leave_squad_short", "退出"),
            Map.entry("gui.wok_infantry.transfer_leader_short", "移交"),
            Map.entry("gui.wok_infantry.claim_commander_short", "指挥"),
            Map.entry("gui.wok_infantry.resign_commander_short", "卸任"),
            Map.entry("gui.wok_infantry.transfer_commander_short", "移交"),
            Map.entry("gui.wok_infantry.map.center_short", "居中"),
            Map.entry("screen.wok_infantry.map.footer_compact", "右键取消 · 平移 · 缩放"),
            Map.entry("screen.wok_infantry.map.terrain.grid_short", "网格"),
            Map.entry("screen.wok_infantry.map.terrain.journeymap_short", "JM"),
            Map.entry("screen.wok_infantry.map.terrain.xaero_short", "Xaero"),
            Map.entry("screen.wok_infantry.map.terrain.initializing_short", "地图初始化"));

    @Test
    void englishAndChineseExposeTheSameNonBlankKeysAndFormatArguments() throws IOException {
        Map<String, String> english = readBundle(EN_US);
        Map<String, String> chinese = readBundle(ZH_CN);

        assertEquals(english.keySet(), chinese.keySet(),
                "en_us and zh_cn must expose exactly the same UI contract");
        Set<String> parameterizedKeys = new TreeSet<>();
        for (String key : english.keySet()) {
            assertFalse(english.get(key).isBlank(), () -> EN_US + " has a blank value for " + key);
            assertFalse(chinese.get(key).isBlank(), () -> ZH_CN + " has a blank value for " + key);
            assertEquals(argumentSignature(english.get(key)), argumentSignature(chinese.get(key)),
                    () -> "format arguments differ for " + key);
            if (!argumentSignature(english.get(key)).isEmpty()) {
                parameterizedKeys.add(key);
            }
        }
        assertEquals(EXPECTED_ARGUMENT_COUNTS.keySet(), parameterizedKeys,
                "every parameterized translation must declare its Java call-site contract");
        EXPECTED_ARGUMENT_COUNTS.forEach((key, argumentCount) -> {
            assertTrue(english.containsKey(key), () -> EN_US + " is missing " + key);
            assertEquals(sequentialArgumentSignature(argumentCount),
                    argumentSignature(english.get(key)),
                    () -> key + " no longer matches its Java call-site argument contract");
        });
    }

    @Test
    void everyLiteralAndDynamicBattleUiKeyExistsInBothLocales() throws Exception {
        Map<String, String> english = readBundle(EN_US);
        Map<String, String> chinese = readBundle(ZH_CN);
        Set<String> required = new TreeSet<>(requiredDynamicKeys());
        required.addAll(literalTranslationKeysFromCompiledMainClasses());

        required.forEach(key -> {
            assertTrue(english.containsKey(key), () -> EN_US + " is missing " + key);
            assertTrue(chinese.containsKey(key), () -> ZH_CN + " is missing " + key);
        });
    }

    @Test
    void compactLabelsRemainConciseAndActionOrientedInBothLocales() throws IOException {
        Map<String, String> english = readBundle(EN_US);
        Map<String, String> chinese = readBundle(ZH_CN);

        EXPECTED_ENGLISH_COMPACT_LABELS.forEach((key, value) ->
                assertEquals(value, english.get(key), () -> "unexpected compact label " + key));
        EXPECTED_CHINESE_COMPACT_LABELS.forEach((key, value) ->
                assertEquals(value, chinese.get(key), () -> "unexpected compact label " + key));
    }

    private static Set<String> literalTranslationKeysFromCompiledMainClasses() throws Exception {
        Path classesRoot = Path.of(ClientBattleState.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
        Path packageRoot = classesRoot.resolve("com/wok/infantry");
        assertTrue(Files.isDirectory(packageRoot),
                () -> "main class output is not a directory: " + packageRoot);
        Set<String> keys = new HashSet<>();
        try (Stream<Path> paths = Files.walk(packageRoot)) {
            for (Path classFile : paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".class")).toList()) {
                collectLiteralTranslationKeys(classFile, keys);
            }
        }
        assertFalse(keys.isEmpty(), "compiled main classes must expose literal UI keys");
        return keys;
    }

    private static void collectLiteralTranslationKeys(Path classFile, Set<String> keys)
            throws IOException {
        try (DataInputStream input = new DataInputStream(Files.newInputStream(classFile))) {
            assertEquals(0xCAFEBABE, input.readInt(), () -> "invalid class file " + classFile);
            input.readUnsignedShort();
            input.readUnsignedShort();
            int constantPoolCount = input.readUnsignedShort();
            for (int index = 1; index < constantPoolCount; index++) {
                int tag = input.readUnsignedByte();
                switch (tag) {
                    case 1 -> {
                        String value = input.readUTF();
                        if (OWN_LITERAL_KEY.matcher(value).matches()) {
                            keys.add(value);
                        }
                    }
                    case 3, 4 -> input.skipNBytes(4);
                    case 5, 6 -> {
                        input.skipNBytes(8);
                        index++;
                    }
                    case 7, 8, 16, 19, 20 -> input.skipNBytes(2);
                    case 9, 10, 11, 12, 17, 18 -> input.skipNBytes(4);
                    case 15 -> input.skipNBytes(3);
                    default -> throw new IOException(
                            "unknown class constant tag " + tag + " in " + classFile);
                }
            }
        }
    }

    private static Set<String> requiredDynamicKeys() {
        Map<String, Boolean> keys = new LinkedHashMap<>();
        keys.put("key.categories.wok_infantry", true);
        keys.put("key.wok_infantry.open_loadout", true);
        keys.put("key.wok_infantry.open_squad", true);
        keys.put("key.wok_infantry.open_tactical_map", true);
        keys.put("faction.wok_infantry.unassigned", true);
        for (Faction faction : Faction.values()) {
            keys.put("faction.wok_infantry." + faction.id(), true);
        }
        for (SquadCallsign callsign : SquadCallsign.values()) {
            keys.put("squad.wok_infantry." + callsign.id(), true);
        }
        for (String classId : BattleRules.DEFAULT_CLASS_LIMITS.keySet()) {
            keys.put("class.wok_infantry." + classId, true);
        }
        for (TacticalMarkerType markerType : TacticalMarkerType.values()) {
            keys.put("marker.wok_infantry." + markerType.id(), true);
        }
        keys.put("marker.wok_infantry.main_base", true);
        for (DeploymentPhase phase : DeploymentPhase.values()) {
            keys.put("screen.wok_infantry.deployment.phase."
                    + phase.name().toLowerCase(Locale.ROOT), true);
        }
        for (String terrain : Set.of("grid", "journeymap", "xaero", "initializing",
                "loading", "failed", "zoom_in")) {
            keys.put("screen.wok_infantry.map.terrain." + terrain, true);
        }
        return keys.keySet();
    }

    private static Map<Integer, Integer> argumentSignature(String value) {
        Map<Integer, Integer> result = new TreeMap<>();
        Matcher matcher = STRING_ARGUMENT.matcher(value);
        int implicitIndex = 1;
        while (matcher.find()) {
            int index = matcher.group(1) == null
                    ? implicitIndex++ : Integer.parseInt(matcher.group(1));
            result.merge(index, 1, Integer::sum);
        }
        return result;
    }

    private static Map<Integer, Integer> sequentialArgumentSignature(int count) {
        Map<Integer, Integer> result = new TreeMap<>();
        for (int index = 1; index <= count; index++) {
            result.put(index, 1);
        }
        return result;
    }

    private static Map<String, String> readBundle(String resource) throws IOException {
        try (InputStream stream = ClientTranslationContractTest.class.getClassLoader()
                .getResourceAsStream(resource)) {
            assertNotNull(stream, () -> "missing processed resource " + resource);
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, String> result = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                assertTrue(entry.getValue().isJsonPrimitive()
                                && entry.getValue().getAsJsonPrimitive().isString(),
                        () -> resource + " must contain only string values: " + entry.getKey());
                result.put(entry.getKey(), entry.getValue().getAsString());
            }
            return result;
        }
    }
}
