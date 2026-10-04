package com.wok.infantry.client.screen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.battle.SquadView;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SquadLabelsTest {
    @Test
    void everyCallsignHasAShortNameInBothLanguages() throws IOException {
        for (String resource : List.of("assets/wok_infantry/lang/zh_cn.json",
                "assets/wok_infantry/lang/en_us.json")) {
            JsonObject bundle = bundle(resource);
            for (SquadCallsign callsign : SquadCallsign.values()) {
                String key = key(SquadLabels.callsignShort(callsign));
                assertEquals("squad.wok_infantry." + callsign.id() + ".short", key);
                assertTrue(bundle.has(key), resource + " is missing " + key);
                assertTrue(bundle.has(key(SquadLabels.callsign(callsign))));
            }
        }
    }

    @Test
    void rolesShowBothTagsForAMemberWhoHoldsBoth() {
        assertNull(SquadLabels.roleShort(false, false));
        assertEquals(SquadLabels.LEADER_SHORT_KEY, key(SquadLabels.roleShort(true, false)));
        assertEquals(SquadLabels.COMMANDER_SHORT_KEY, key(SquadLabels.roleShort(false, true)));
        Component both = SquadLabels.roleShort(true, true);
        assertEquals(SquadLabels.LEADER_SHORT_KEY, key(both));
        assertEquals(2, both.getSiblings().size(), "separator and the commander tag");
        assertEquals(SquadLabels.COMMANDER_SHORT_KEY, key(both.getSiblings().get(1)));
        assertEquals("★◆", SquadLabels.roleSymbols(true, true));
        assertEquals("◆", SquadLabels.roleSymbols(false, true));
        assertEquals("", SquadLabels.roleSymbols(false, false));
    }

    @Test
    void memberCountUsesTheSquadsOwnCapacity() {
        assertEquals("3/6", SquadLabels.memberCountText(3, 6));
        assertEquals("0/0", SquadLabels.memberCountText(-1, -2));
        SquadView squad = new SquadView(SquadCallsign.BRAVO, new UUID(0L, 1L), List.of(), 6);
        Component count = SquadLabels.memberCount(squad);
        TranslatableContents contents = (TranslatableContents) count.getContents();
        assertEquals(SquadLabels.MEMBER_COUNT_KEY, contents.getKey());
        assertEquals(List.of(0, 6), List.of(contents.getArgs()));
    }

    @Test
    void dimensionNamesFallBackToTheirPath() {
        Component name = SquadLabels.dimensionName(
                ResourceLocation.fromNamespaceAndPath("wok_infantry", "lobby"));
        assertEquals("dimension.wok_infantry.lobby", key(name));
        assertNotNull(SquadLabels.dimensionName(null));
    }

    private static String key(Component component) {
        return ((TranslatableContents) component.getContents()).getKey();
    }

    private static JsonObject bundle(String resource) throws IOException {
        try (InputStream stream = SquadLabelsTest.class.getClassLoader()
                .getResourceAsStream(resource)) {
            assertNotNull(stream, resource);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }
}
