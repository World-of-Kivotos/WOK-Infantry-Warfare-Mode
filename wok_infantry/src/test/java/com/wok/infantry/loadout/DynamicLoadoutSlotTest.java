package com.wok.infantry.loadout;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DynamicLoadoutSlotTest {
    @Test
    void legacyConfigMigratesWithoutChangingCapturedSnbt() {
        String snbt = "{GunId:\"tacz:m4a1\",Attachments:{Scope:\"tacz:acog\"}}";
        String json = """
                {"version":2,"classes":[{"id":"custom_team","displayName":"队长",
                "enabled":true,"squadLimit":1,"slots":{"primary":[{"id":"m4",
                "displayName":"M4A1","itemId":"tacz:modern_kinetic_gun","count":1,
                "snbt":"{GunId:\\"tacz:m4a1\\",Attachments:{Scope:\\"tacz:acog\\"}}"}],
                "secondary":[],"melee":[],"gadget_one":[],"gadget_two":[],"throwable":[]}}]}
                """;
        LoadoutConfigData data = new Gson().fromJson(json, LoadoutConfigData.class);

        data.normalize();

        LoadoutClassDefinition definition = data.findClass("custom_team").orElseThrow();
        assertEquals(3, data.version());
        assertEquals(snbt, definition.entries("primary").get(0).snbt());
        assertTrue(definition.findSlot("primary").orElseThrow().required());
        assertFalse(definition.findSlot("secondary").orElseThrow().required());
        assertEquals(LoadoutInventoryTarget.HOTBAR_1,
                definition.findSlot("primary").orElseThrow().target());
    }

    @Test
    void administratorCanAddMoveEditAndDeleteArmorSlot() {
        LoadoutClassDefinition definition = new LoadoutClassDefinition(
                "custom", "突破手", true, 2, false);
        LoadoutSlotDefinition primary = new LoadoutSlotDefinition("primary", "主武器",
                LoadoutInventoryTarget.HOTBAR_1, true);
        LoadoutSlotDefinition helmet = new LoadoutSlotDefinition("helmet", "防弹头盔",
                LoadoutInventoryTarget.ARMOR_HEAD, false);

        assertTrue(definition.addSlot(primary));
        assertTrue(definition.addSlot(helmet));
        assertFalse(definition.addSlot(new LoadoutSlotDefinition("duplicate_target", "重复",
                LoadoutInventoryTarget.ARMOR_HEAD, true)));
        definition.entries("helmet").add(new LoadoutEntry("armor", "头盔",
                "minecraft:diamond_helmet", 1, "{display:{color:1}}"));

        assertTrue(definition.moveSlot("helmet", -1));
        assertEquals(List.of("helmet", "primary"), definition.slotDefinitions().stream()
                .map(LoadoutSlotDefinition::id).toList());
        assertTrue(definition.updateSlot("helmet", "重型头盔",
                LoadoutInventoryTarget.ARMOR_CHEST, true));
        assertEquals("重型头盔", definition.findSlot("helmet").orElseThrow().displayName());
        assertTrue(definition.findSlot("helmet").orElseThrow().required());

        LoadoutClassDefinition copy = definition.copy();
        assertEquals("{display:{color:1}}", copy.entries("helmet").get(0).snbt());
        assertTrue(definition.deleteSlot("helmet"));
        assertTrue(definition.findSlot("helmet").isEmpty());
        assertTrue(definition.entries("helmet").isEmpty());
        assertEquals(1, copy.entries("helmet").size());
    }
}
