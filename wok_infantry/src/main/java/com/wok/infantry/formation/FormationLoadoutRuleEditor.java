package com.wok.infantry.formation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Pure copy-on-write editor for one formation's per-slot loadout allow-list. */
public final class FormationLoadoutRuleEditor {
    private FormationLoadoutRuleEditor() {
    }

    public static FormationConfigData apply(FormationConfigData source,
                                            String factionId,
                                            String formationId,
                                            String classId,
                                            String slotId,
                                            String entryId,
                                            FormationLoadoutEditAction action) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(action, "action");
        FactionDefinition targetFaction = source.findFaction(factionId)
                .orElseThrow(() -> new IllegalArgumentException("阵营不存在"));
        FormationDefinition targetFormation = targetFaction.findFormation(formationId)
                .orElseThrow(() -> new IllegalArgumentException("编制不存在"));
        FormationClassRule targetRule = targetFormation.findClass(classId)
                .orElseThrow(() -> new IllegalArgumentException("该编制没有此兵种"));
        String normalizedSlot = Objects.requireNonNullElse(slotId, "").trim();
        String normalizedEntry = Objects.requireNonNullElse(entryId, "").trim();
        if (normalizedSlot.isEmpty()) {
            throw new IllegalArgumentException("装备槽位不能为空");
        }
        if (action != FormationLoadoutEditAction.ALLOW_ALL && normalizedEntry.isEmpty()) {
            throw new IllegalArgumentException("装备条目不能为空");
        }

        Map<String, List<String>> allowed = new LinkedHashMap<>(targetRule.allowedEntries());
        List<String> current = new ArrayList<>(allowed.getOrDefault(normalizedSlot, List.of()));
        switch (action) {
            case INCLUDE_CAPTURED -> {
                if (current.isEmpty()) {
                    current.add(normalizedEntry);
                } else if (!current.contains(normalizedEntry)) {
                    requireCapacity(current);
                    current.add(normalizedEntry);
                }
                allowed.put(normalizedSlot, current);
            }
            case EXCLUSIVE -> allowed.put(normalizedSlot,
                    new ArrayList<>(List.of(normalizedEntry)));
            case ADD -> {
                if (current.isEmpty()) {
                    throw new IllegalArgumentException("本槽位当前已开放全部装备；请先使用“仅此”建立限定");
                }
                if (!current.contains(normalizedEntry)) {
                    requireCapacity(current);
                    current.add(normalizedEntry);
                }
                allowed.put(normalizedSlot, current);
            }
            case REMOVE -> {
                if (current.isEmpty()) {
                    throw new IllegalArgumentException("本槽位当前未设置严格白名单");
                }
                if (!current.contains(normalizedEntry)) {
                    throw new IllegalArgumentException("该装备不在当前编制白名单中");
                }
                if (current.size() == 1) {
                    throw new IllegalArgumentException("每个受限槽位至少保留一个装备；可改用“全部开放”");
                }
                current.remove(normalizedEntry);
                allowed.put(normalizedSlot, current);
            }
            case ALLOW_ALL -> allowed.remove(normalizedSlot);
        }

        FormationClassRule replacementRule = new FormationClassRule(targetRule.classId(),
                targetRule.displayName(), targetRule.squadLimit(), allowed);
        List<FormationClassRule> classes = targetFormation.classes().stream()
                .map(rule -> rule.classId().equals(targetRule.classId())
                        ? replacementRule : rule.copy())
                .toList();
        FormationDefinition replacementFormation = new FormationDefinition(
                targetFormation.id(), targetFormation.displayName(), targetFormation.description(),
                targetFormation.icon(), targetFormation.category(), targetFormation.enabled(),
                targetFormation.capacity(), targetFormation.capabilities(), classes,
                targetFormation.squads(), targetFormation.vehicles());
        List<FormationDefinition> formations = targetFaction.formations().stream()
                .map(formation -> formation.id().equals(targetFormation.id())
                        ? replacementFormation : formation.copy())
                .toList();
        FactionDefinition replacementFaction = new FactionDefinition(targetFaction.id(),
                targetFaction.displayName(), targetFaction.description(),
                targetFaction.battleSideId(), targetFaction.enabled(), targetFaction.maxPlayers(),
                formations);
        List<FactionDefinition> factions = source.factions().stream()
                .map(faction -> faction.id().equals(targetFaction.id())
                        ? replacementFaction : faction.copy())
                .toList();
        FormationConfigData updated = new FormationConfigData(source.version(), factions);
        updated.normalize();
        if (updated.findFormation(targetFaction.id(), targetFormation.id()).isEmpty()) {
            throw new IllegalArgumentException("修改后的编制未通过配置校验");
        }
        return updated;
    }

    private static void requireCapacity(List<String> current) {
        if (current.size() >= FormationClassRule.MAX_ALLOWED_ENTRIES_PER_SLOT) {
            throw new IllegalArgumentException("该槽位的编制白名单已达到上限");
        }
    }
}
