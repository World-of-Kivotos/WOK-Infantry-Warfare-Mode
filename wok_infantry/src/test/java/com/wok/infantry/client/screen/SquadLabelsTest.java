package com.wok.infantry.client.screen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.battle.ClassQuotaView;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.FormationContextView;
import com.wok.infantry.battle.PermissionView;
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
    void singleRoleNamesPutTheCommanderFirstAndNameTheUnassigned() {
        assertEquals(SquadLabels.COMMANDER_KEY, key(SquadLabels.roleName(true, true, true)));
        assertEquals(SquadLabels.COMMANDER_KEY, key(SquadLabels.roleName(false, false, true)));
        assertEquals(SquadLabels.LEADER_KEY, key(SquadLabels.roleName(true, true, false)));
        assertEquals(SquadLabels.MEMBER_KEY, key(SquadLabels.roleName(true, false, false)));
        assertEquals(SquadLabels.UNASSIGNED_KEY, key(SquadLabels.roleName(false, false, false)));
        assertEquals(SquadLabels.COMMANDER_SHORT_KEY,
                key(SquadLabels.roleNameShort(true, true, true)));
        assertEquals(SquadLabels.LEADER_SHORT_KEY,
                key(SquadLabels.roleNameShort(true, true, false)));
        assertEquals(SquadLabels.MEMBER_SHORT_KEY,
                key(SquadLabels.roleNameShort(true, false, false)));
        assertEquals(SquadLabels.UNASSIGNED_KEY,
                key(SquadLabels.roleNameShort(false, false, false)));
    }

    @Test
    void squadScreenStaticLabelsDelegateToSquadLabels() {
        assertEquals(SquadLabels.callsign(SquadCallsign.DELTA),
                SquadScreen.callsign(SquadCallsign.DELTA));
        assertEquals(SquadLabels.className("medic"), SquadScreen.className("medic"));
        assertEquals("class.wok_infantry.assault", key(SquadScreen.className((String) null)));
        assertEquals("突击兵", SquadScreen.className("assault", " 突击兵 ").getString());
        BattleSnapshot snapshot = new BattleSnapshot(new UUID(0L, 1L), Faction.BLUE, null,
                false, false, 1, 0, 40, 8, List.of(), List.of(), List.of(),
                new PermissionView(false, false, false, false, false, false, false),
                List.of(new ClassQuotaView("medic", "医疗兵", 2, 0)), 0L, 0L);
        assertEquals("医疗兵", SquadScreen.className(snapshot, "medic").getString());
        assertEquals("class.wok_infantry.sniper", key(SquadLabels.className(snapshot,
                "sniper")));
    }

    @Test
    void publicFactionAndFormationNamesComeFromTheContext() {
        BattleSnapshot plain = new BattleSnapshot(new UUID(0L, 1L), Faction.RED, null,
                false, false, 1, 0, 40, 8, List.of(), List.of(), List.of(),
                new PermissionView(false, false, false, false, false, false, false),
                List.of(), 0L, 0L);
        assertEquals("faction.wok_infantry.red", key(SquadLabels.factionName(plain)));
        assertEquals("faction.wok_infantry.blue", key(SquadLabels.enemyFactionName(plain)));
        assertNull(SquadLabels.formationName(plain), "no formation before the lock");
        assertEquals("faction.wok_infantry.unassigned", key(SquadLabels.factionName(null)));

        BattleSnapshot named = plain.withFormationContext(new FormationContextView("default",
                "常规编制", "assault", "kaiser", "凯撒", 40, "academy", "学院军", 40));
        assertEquals("凯撒", SquadLabels.factionName(named).getString());
        assertEquals("学院军", SquadLabels.enemyFactionName(named).getString());
        assertEquals("常规编制", SquadLabels.formationName(named).getString());
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
