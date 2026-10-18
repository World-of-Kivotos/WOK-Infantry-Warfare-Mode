package com.wok.infantry.formation;

import com.wok.infantry.loadout.LoadoutClassDefinition;
import com.wok.infantry.loadout.LoadoutConfigData;
import com.wok.infantry.loadout.LoadoutEntry;
import com.wok.infantry.loadout.LoadoutSlotDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/** Pure planner for a deep, independent copy of one formation-owned profession loadout. */
public final class ClassLoadoutCopyPlanner {
    private ClassLoadoutCopyPlanner() {
    }

    public static CopyPlan plan(FormationConfigData formations,
                                LoadoutConfigData loadouts,
                                String sourceFactionId,
                                String sourceFormationId,
                                String sourceClassId,
                                String targetFactionId,
                                String targetFormationId,
                                String targetClassId,
                                Function<String, String> classIdFactory) {
        Objects.requireNonNull(formations, "formations");
        Objects.requireNonNull(loadouts, "loadouts");
        Objects.requireNonNull(classIdFactory, "classIdFactory");
        if (Objects.equals(sourceFactionId, targetFactionId)
                && Objects.equals(sourceFormationId, targetFormationId)
                && Objects.equals(sourceClassId, targetClassId)) {
            throw new IllegalArgumentException("复制源兵种与粘贴目标兵种不能相同");
        }

        FormationDefinition sourceFormation = formations.findFormation(sourceFactionId,
                        sourceFormationId)
                .orElseThrow(() -> new IllegalArgumentException("复制源编制不存在"));
        FormationDefinition targetFormation = formations.findFormation(targetFactionId,
                        targetFormationId)
                .orElseThrow(() -> new IllegalArgumentException("粘贴目标编制不存在"));
        FormationClassRule sourceRule = sourceFormation.findClass(sourceClassId)
                .orElseThrow(() -> new IllegalArgumentException("复制源兵种不存在"));
        FormationClassRule targetRule = targetFormation.findClass(targetClassId)
                .orElseThrow(() -> new IllegalArgumentException("粘贴目标兵种不存在"));
        LoadoutClassDefinition sourceBacking = loadouts.findClass(sourceRule.classId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "复制源兵种缺少装备池：" + sourceRule.classId()));
        LoadoutClassDefinition targetBacking = loadouts.findClass(targetRule.classId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "粘贴目标兵种缺少装备池：" + targetRule.classId()));

        LoadoutConfigData copiedLoadouts = loadouts.copy();
        Set<String> usedClassIds = new LinkedHashSet<>();
        copiedLoadouts.classes().forEach(definition -> usedClassIds.add(definition.id()));
        String copiedClassId = Objects.requireNonNullElse(
                classIdFactory.apply(targetRule.classId()), "").trim();
        if (!validId(copiedClassId) || !usedClassIds.add(copiedClassId)) {
            throw new IllegalArgumentException("生成的复制兵种 ID 无效或重复");
        }

        String copiedDisplayName = targetRule.displayName().isBlank()
                ? targetBacking.displayName() : targetRule.displayName();
        LoadoutClassDefinition copiedBacking = copyBacking(sourceBacking, copiedClassId,
                copiedDisplayName, targetBacking.enabled(), targetBacking.squadLimit());
        FormationClassRule replacementRule = new FormationClassRule(copiedClassId,
                targetRule.displayName(), targetRule.squadLimit(),
                sourceRule.allowedEntries());

        List<FormationClassRule> replacementRules = new ArrayList<>();
        for (FormationClassRule rule : targetFormation.classes()) {
            replacementRules.add(rule.classId().equals(targetRule.classId())
                    ? replacementRule : rule.copy());
        }
        List<FormationSquadDefinition> replacementSquads = targetFormation.squads().stream()
                .map(squad -> replaceSquadClassId(squad, targetFormation.classes(),
                        targetRule.classId(), copiedClassId))
                .toList();
        FormationDefinition replacementFormation = new FormationDefinition(
                targetFormation.id(), targetFormation.displayName(), targetFormation.description(),
                targetFormation.icon(), targetFormation.category(), targetFormation.enabled(),
                targetFormation.capacity(), targetFormation.capabilities(), replacementRules,
                replacementSquads, targetFormation.vehicles());

        List<FactionDefinition> copiedFactions = formations.factions().stream().map(faction -> {
            if (!faction.id().equals(targetFactionId)) {
                return faction.copy();
            }
            List<FormationDefinition> replacements = faction.formations().stream()
                    .map(formation -> formation.id().equals(targetFormationId)
                            ? replacementFormation : formation.copy()).toList();
            return new FactionDefinition(faction.id(), faction.displayName(),
                    faction.description(), faction.battleSideId(), faction.enabled(),
                    faction.maxPlayers(), replacements);
        }).toList();
        FormationConfigData copiedFormations = new FormationConfigData(
                formations.version(), copiedFactions);
        copiedFormations.normalize();

        Set<String> referencedClassIds = new LinkedHashSet<>();
        copiedFormations.factions().forEach(faction -> faction.formations().forEach(formation ->
                formation.classes().forEach(rule -> referencedClassIds.add(rule.classId()))));
        copiedLoadouts.classes().removeIf(definition ->
                definition.id().equals(targetRule.classId())
                        && !referencedClassIds.contains(definition.id()));
        if (copiedLoadouts.classes().size() >= LoadoutConfigData.MAX_CLASSES) {
            throw new IllegalArgumentException("全局兵种装备池没有足够空间完成复制");
        }
        copiedLoadouts.classes().add(copiedBacking);
        copiedLoadouts.normalize();

        FormationDefinition verifiedTarget = copiedFormations.findFormation(targetFactionId,
                        targetFormationId)
                .orElseThrow(() -> new IllegalArgumentException("复制后的目标编制无效"));
        FormationClassRule verifiedRule = verifiedTarget.findClass(copiedClassId)
                .orElseThrow(() -> new IllegalArgumentException("复制后的目标兵种无效"));
        if (!verifiedRule.displayName().equals(targetRule.displayName())
                || verifiedRule.squadLimit() != targetRule.squadLimit()) {
            throw new IllegalArgumentException("复制意外改变了目标兵种属性");
        }
        return new CopyPlan(copiedFormations, copiedLoadouts,
                targetRule.classId(), copiedClassId);
    }

    private static FormationSquadDefinition replaceSquadClassId(
            FormationSquadDefinition squad,
            List<FormationClassRule> orderedRules,
            String targetClassId,
            String copiedClassId) {
        LinkedHashMap<String, Integer> limits = new LinkedHashMap<>();
        for (FormationClassRule rule : orderedRules) {
            String id = rule.classId().equals(targetClassId) ? copiedClassId : rule.classId();
            limits.put(id, squad.classLimit(rule.classId(), rule.squadLimit()));
        }
        return new FormationSquadDefinition(squad.callsign(), squad.displayName(),
                squad.capacity(), limits);
    }

    private static LoadoutClassDefinition copyBacking(LoadoutClassDefinition source,
                                                       String copiedId,
                                                       String displayName,
                                                       boolean enabled,
                                                       int squadLimit) {
        LoadoutClassDefinition copied = new LoadoutClassDefinition(copiedId,
                displayName, enabled, squadLimit, false);
        for (LoadoutSlotDefinition slot : source.slotDefinitions()) {
            if (!copied.addSlot(slot)) {
                throw new IllegalArgumentException("复制装备槽位失败：" + slot.id());
            }
            source.entries(slot.id()).stream().map(LoadoutEntry::copy)
                    .forEach(copied.entries(slot.id())::add);
        }
        return copied;
    }

    private static boolean validId(String value) {
        if (value == null || value.isBlank() || value.length() > 64) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (!(character >= 'a' && character <= 'z')
                    && !(character >= '0' && character <= '9')
                    && character != '_' && character != '-' && character != '.') {
                return false;
            }
        }
        return true;
    }

    public record CopyPlan(FormationConfigData formations,
                           LoadoutConfigData loadouts,
                           String replacedClassId,
                           String targetClassId) {
    }
}
