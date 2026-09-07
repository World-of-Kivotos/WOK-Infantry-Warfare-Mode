package com.wok.infantry.configtransfer;

import com.google.gson.JsonObject;
import com.wok.infantry.formation.FormationConfigData;
import com.wok.infantry.loadout.LoadoutConfigData;
import com.wok.infantry.loadout.LoadoutEntry;
import com.wok.infantry.loadout.LoadoutSlotDefinition;
import com.wok.infantry.loadout.LoadoutInventoryTarget;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class CatalogArchiveTest {
    static CatalogArchive fixture() {
        var loadouts = LoadoutConfigData.defaultConfig();
        loadouts.findClass("assault").orElseThrow().entries("primary").add(new LoadoutEntry(
                "weapons/test", "中文步枪", "minecraft:stick", 1,
                "{GunId:\"example:rifle\",Attachments:{scope:\"example:scope\"},Custom:[I;1,2,3]}", 317));
        var support = loadouts.findClass("support").orElseThrow();
        support.addSlot(new LoadoutSlotDefinition("custom_medical", "自定义医疗槽", LoadoutInventoryTarget.HOTBAR_7, false));
        support.entries("custom_medical").add(new LoadoutEntry("medical_item", "医疗物品", "minecraft:apple", 3, "{custom:1b}"));
        while (support.moveSlot("custom_medical", -1)) { /* Preserve administrator-defined order. */ }
        return CatalogArchive.create(FormationConfigData.defaultConfig(), loadouts);
    }

    @Test void completeRoundTripRetainsAllFieldsAndOrderIncludingSlashEntryIds() throws Exception {
        var original = fixture();
        var restored = CatalogArchive.decode(original.encode());
        assertEquals(CatalogArchive.GSON.toJsonTree(original), CatalogArchive.GSON.toJsonTree(restored));
        var gun = restored.loadouts().findClass("assault").orElseThrow().entries("primary").get(1);
        assertEquals(317, gun.ammoReserveLimit());
        assertTrue(gun.snbt().contains("Attachments"));
        var slot = restored.loadouts().findClass("support").orElseThrow().slotDefinitions().get(0);
        assertEquals("custom_medical", slot.id());
        assertFalse(slot.required());
        assertEquals(LoadoutInventoryTarget.HOTBAR_7, slot.target());
        assertTrue(restored.summary().contains("2 阵营"));
        String json = new String(original.encode(), StandardCharsets.UTF_8);
        assertTrue(json.contains("中文步枪"));
        assertFalse(json.contains("player_loadouts"));
    }

    @Test void rejectsWrongVersionUnknownFieldsNullsAndPartialCatalogs() {
        for (String field : new String[]{"version", "unexpected", "loadouts", "formations"}) {
            JsonObject root = CatalogArchive.GSON.toJsonTree(fixture()).getAsJsonObject();
            switch (field) {
                case "version" -> root.addProperty(field, 99);
                case "unexpected" -> root.addProperty(field, "must not disappear");
                case "loadouts" -> root.add(field, com.google.gson.JsonNull.INSTANCE);
                default -> root.remove(field);
            }
            assertThrows(Exception.class, () -> CatalogArchive.decode(bytes(root)), field);
        }
    }

    @Test void rejectsDuplicateFieldsTrailingContentAndExcessiveNesting() {
        assertThrows(Exception.class, () -> CatalogArchive.decode("{\"version\":1,\"version\":2}".getBytes(StandardCharsets.UTF_8)));
        String valid = new String(fixture().encode(), StandardCharsets.UTF_8);
        assertThrows(Exception.class, () -> CatalogArchive.decode((valid + "{}").getBytes(StandardCharsets.UTF_8)));
        assertThrows(Exception.class, () -> CatalogArchive.decode(("[".repeat(60) + "0" + "]".repeat(60)).getBytes(StandardCharsets.UTF_8)));
        assertThrows(Exception.class, () -> CatalogArchive.decode(new byte[]{(byte)0xC3, (byte)0x28}));
    }

    @Test void refusesLossyNormalizationAndDanglingReferences() {
        var root = CatalogArchive.GSON.toJsonTree(fixture()).getAsJsonObject();
        var classes = root.getAsJsonObject("loadouts").getAsJsonArray("classes");
        classes.add(classes.get(0).deepCopy());
        assertThrows(IllegalArgumentException.class, () -> CatalogArchive.decode(bytes(root)));
        var references = CatalogArchive.GSON.toJsonTree(fixture()).getAsJsonObject();
        references.getAsJsonObject("loadouts").getAsJsonArray("classes").remove(1);
        var exception = assertThrows(IllegalArgumentException.class, () -> CatalogArchive.decode(bytes(references)));
        assertTrue(exception.getMessage().contains("不存在的兵种"));
    }

    @Test void refusesBadWhitelistRatherThanOpeningAllEquipment() {
        var root = CatalogArchive.GSON.toJsonTree(fixture()).getAsJsonObject();
        var rule = root.getAsJsonObject("formations").getAsJsonArray("factions").get(0)
                .getAsJsonObject().getAsJsonArray("formations").get(0).getAsJsonObject()
                .getAsJsonArray("classes").get(0).getAsJsonObject();
        var entries = new com.google.gson.JsonArray();
        entries.add("missing_weapon");
        rule.getAsJsonObject("allowedEntries").add("primary", entries);
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> CatalogArchive.decode(bytes(root))).getMessage().contains("不存在的装备"));
    }

    @Test void preservesDisabledCatalogEntriesCustomSlotsAndStrictWhitelist() throws Exception {
        var root = CatalogArchive.GSON.toJsonTree(fixture()).getAsJsonObject();
        var faction = root.getAsJsonObject("formations").getAsJsonArray("factions").get(0).getAsJsonObject();
        faction.addProperty("enabled", false);
        var entries = new com.google.gson.JsonArray();
        entries.add("weapons/test");
        faction.getAsJsonArray("formations").get(0).getAsJsonObject().getAsJsonArray("classes")
                .get(0).getAsJsonObject().getAsJsonObject("allowedEntries").add("primary", entries);
        var restored = CatalogArchive.decode(bytes(root));
        assertFalse(restored.formations().factions().get(0).enabled());
        assertEquals(root, CatalogArchive.GSON.toJsonTree(restored));
    }

    private static byte[] bytes(JsonObject root) {
        return CatalogArchive.GSON.toJson(root).getBytes(StandardCharsets.UTF_8);
    }
}
