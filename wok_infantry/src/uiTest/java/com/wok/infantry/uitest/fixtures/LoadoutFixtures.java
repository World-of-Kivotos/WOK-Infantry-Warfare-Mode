package com.wok.infantry.uitest.fixtures;

import com.wok.infantry.formation.FactionDefinition;
import com.wok.infantry.formation.FormationClassEditAction;
import com.wok.infantry.formation.FormationClassEditor;
import com.wok.infantry.formation.FormationConfigData;
import com.wok.infantry.formation.FormationDefinition;
import com.wok.infantry.loadout.LoadoutClassDefinition;
import com.wok.infantry.loadout.LoadoutConfigData;
import com.wok.infantry.loadout.LoadoutInventoryTarget;
import com.wok.infantry.loadout.LoadoutSlotDefinition;
import com.wok.infantry.loadout.LoadoutSnapshot;
import com.wok.infantry.loadout.PlayerLoadoutData;

import java.util.ArrayList;
import java.util.List;

/** Loadout snapshots for the administrator loadout terminal. */
public final class LoadoutFixtures {
    private LoadoutFixtures() {
    }

    /**
     * The legacy administrator fixture ({@code wok_ui_12}–{@code 14}): the default catalog plus a
     * helmet slot on the assault class and a custom combat-medic class with its formation rule.
     */
    public static LoadoutSnapshot adminVisual() {
        LoadoutConfigData loadouts = LoadoutConfigData.defaultConfig();
        loadouts.findClass("assault").orElseThrow().addSlot(new LoadoutSlotDefinition(
                "helmet", "防弹头盔", LoadoutInventoryTarget.ARMOR_HEAD, false));
        loadouts.classes().add(new LoadoutClassDefinition(
                "custom_visual_medic", "战斗医疗员", true, 1));
        FormationConfigData formations = FormationClassEditor.apply(
                FormationConfigData.defaultConfig(), "academy", "default",
                "assault", "突破手", 8, FormationClassEditAction.UPDATE);
        formations = FormationClassEditor.apply(formations, "academy", "default",
                "custom_visual_medic", "战斗医疗员", 1,
                FormationClassEditAction.CREATE);
        return new LoadoutSnapshot(loadouts, new PlayerLoadoutData(), true, formations);
    }

    /**
     * admin-01: the default catalog, but every formation of the academy faction lost its class
     * rules (as a hand-edited configuration can leave it), so "职业管理" has nothing to manage.
     */
    public static LoadoutSnapshot adminWithoutClassRules() {
        List<FactionDefinition> factions = new ArrayList<>();
        for (FactionDefinition faction : FormationConfigData.defaultConfig().factions()) {
            if (!faction.id().equals(MockData.VIEWER_FACTION)) {
                factions.add(faction);
                continue;
            }
            List<FormationDefinition> formations = new ArrayList<>();
            for (FormationDefinition formation : faction.formations()) {
                formations.add(new FormationDefinition(formation.id(), formation.displayName(),
                        formation.description(), formation.icon(), formation.category(),
                        formation.enabled(), formation.capacity(), formation.capabilities(),
                        List.of(), formation.squads(), formation.vehicles()));
            }
            factions.add(new FactionDefinition(faction.id(), faction.displayName(),
                    faction.description(), faction.battleSideId(), faction.enabled(),
                    faction.maxPlayers(), formations));
        }
        return new LoadoutSnapshot(LoadoutConfigData.defaultConfig(), new PlayerLoadoutData(),
                true, new FormationConfigData(factions));
    }
}
