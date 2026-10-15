package com.wok.infantry.formation;

import com.wok.infantry.battle.BattleRules;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Pure copy-on-write editor for formation-owned professions. */
public final class FormationClassEditor {
    private FormationClassEditor() {
    }

    public static FormationConfigData apply(FormationConfigData source,
                                            String factionId,
                                            String formationId,
                                            String classId,
                                            String displayName,
                                            int squadLimit,
                                            FormationClassEditAction action) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(action, "action");
        FactionDefinition targetFaction = source.findFaction(factionId)
                .orElseThrow(() -> new IllegalArgumentException("阵营不存在"));
        FormationDefinition targetFormation = targetFaction.findFormation(formationId)
                .orElseThrow(() -> new IllegalArgumentException("编制不存在"));
        String normalizedClassId = Objects.requireNonNullElse(classId, "")
                .trim().toLowerCase(java.util.Locale.ROOT);
        if (normalizedClassId.isEmpty() || normalizedClassId.length() > 64
                || !normalizedClassId.matches("[a-z0-9_.-]+")) {
            throw new IllegalArgumentException("职业内部 ID 无效");
        }
        String normalizedName = Objects.requireNonNullElse(displayName, "").trim();
        boolean editsMetadata = action == FormationClassEditAction.CREATE
                || action == FormationClassEditAction.UPDATE;
        if (editsMetadata
                && (normalizedName.isEmpty() || normalizedName.length() > 40)) {
            throw new IllegalArgumentException("职业名称必须为 1–40 个字符");
        }
        if (editsMetadata
                && (squadLimit < 1 || squadLimit > BattleRules.SQUAD_CAPACITY)) {
            throw new IllegalArgumentException("每小队职业名额必须为 1–8");
        }
        FormationClassRule existing = targetFormation.findClass(normalizedClassId).orElse(null);
        List<FormationClassRule> classes = new ArrayList<>();
        switch (action) {
            case CREATE -> {
                if (existing != null) {
                    throw new IllegalArgumentException("当前编制已经包含这个职业");
                }
                if (targetFormation.classes().size() >= FormationDefinition.MAX_CLASSES) {
                    throw new IllegalArgumentException("当前编制的职业数量已达到上限");
                }
                targetFormation.classes().stream().map(FormationClassRule::copy)
                        .forEach(classes::add);
                classes.add(new FormationClassRule(normalizedClassId, normalizedName,
                        squadLimit, Map.of()));
            }
            case UPDATE -> {
                if (existing == null) {
                    throw new IllegalArgumentException("当前编制没有这个职业");
                }
                targetFormation.classes().stream().map(rule ->
                                rule.classId().equals(normalizedClassId)
                                        ? new FormationClassRule(rule.classId(), normalizedName,
                                        squadLimit, rule.allowedEntries())
                                        : rule.copy())
                        .forEach(classes::add);
            }
            case DELETE -> {
                if (existing == null) {
                    throw new IllegalArgumentException("当前编制没有这个职业");
                }
                if (targetFormation.classes().size() <= 1) {
                    throw new IllegalArgumentException("每个编制至少保留一个职业；请先新建其他职业");
                }
                targetFormation.classes().stream()
                        .filter(rule -> !rule.classId().equals(normalizedClassId))
                        .map(FormationClassRule::copy).forEach(classes::add);

            }
            case MOVE_UP, MOVE_DOWN -> {
                if (existing == null) {
                    throw new IllegalArgumentException("当前编制没有这个职业");
                }
                targetFormation.classes().stream().map(FormationClassRule::copy)
                        .forEach(classes::add);
                int currentIndex = java.util.stream.IntStream.range(0, classes.size())
                        .filter(index -> classes.get(index).classId().equals(normalizedClassId))
                        .findFirst().orElse(-1);
                int targetIndex = currentIndex
                        + (action == FormationClassEditAction.MOVE_UP ? -1 : 1);
                if (currentIndex < 0 || targetIndex < 0 || targetIndex >= classes.size()) {
                    throw new IllegalArgumentException(action == FormationClassEditAction.MOVE_UP
                            ? "该职业已经位于列表最前" : "该职业已经位于列表最后");
                }
                java.util.Collections.swap(classes, currentIndex, targetIndex);
            }
        }

        boolean deletedCurrentDefault = action == FormationClassEditAction.DELETE
                && targetFormation.defaultClass().map(FormationClassRule::classId)
                .filter(normalizedClassId::equals).isPresent();
        String promotedFallbackId = null;
        if (deletedCurrentDefault && classes.stream().noneMatch(rule ->
                coversEverySquad(targetFormation, rule))) {
            int requiredLimit = targetFormation.squads().stream()
                    .mapToInt(FormationSquadDefinition::capacity).max().orElse(1);
            FormationClassRule promoted = classes.get(0);
            classes.set(0, new FormationClassRule(promoted.classId(),
                    promoted.displayName(), Math.max(promoted.squadLimit(), requiredLimit),
                    promoted.allowedEntries()));
            promotedFallbackId = promoted.classId();
        }
        String finalPromotedFallbackId = promotedFallbackId;
        List<FormationSquadDefinition> squads = targetFormation.squads().stream().map(squad -> {
            Map<String, Integer> limits = new java.util.LinkedHashMap<>(squad.classLimits());
            if (action == FormationClassEditAction.DELETE) {
                limits.remove(normalizedClassId);
                if (finalPromotedFallbackId != null) {
                    limits.put(finalPromotedFallbackId, squad.capacity());
                }
            } else if (editsMetadata) {
                limits.put(normalizedClassId, Math.min(squad.capacity(), squadLimit));
            }
            return new FormationSquadDefinition(squad.callsign(), squad.displayName(),
                    squad.capacity(), limits);
        }).toList();
        FormationDefinition replacementFormation = new FormationDefinition(
                targetFormation.id(), targetFormation.displayName(), targetFormation.description(),
                targetFormation.icon(), targetFormation.category(), targetFormation.enabled(),
                targetFormation.capacity(), targetFormation.capabilities(), classes,
                squads, targetFormation.vehicles());
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
        FormationDefinition verified = updated.findFormation(targetFaction.id(),
                targetFormation.id()).orElseThrow(() ->
                new IllegalArgumentException("修改后的编制未通过配置校验"));
        if (action == FormationClassEditAction.DELETE) {
            if (verified.findClass(normalizedClassId).isPresent()) {
                throw new IllegalArgumentException("职业删除未通过配置校验");
            }
        } else if (verified.findClass(normalizedClassId).isEmpty()) {
            throw new IllegalArgumentException("职业修改未通过配置校验");
        }
        FormationClassRule defaultClass = verified.defaultClass()
                .orElseThrow(() -> new IllegalArgumentException("编制必须至少保留一个职业"));
        for (FormationSquadDefinition squad : verified.squads()) {
            if (squad.classLimit(defaultClass.classId(), defaultClass.squadLimit())
                    < squad.capacity()) {
                throw new IllegalArgumentException(
                        "默认职业名额必须覆盖小队容量：" + squad.displayName());
            }
        }
        return updated;
    }

    private static boolean coversEverySquad(FormationDefinition formation,
                                             FormationClassRule rule) {
        return formation.squads().stream().allMatch(squad ->
                squad.classLimit(rule.classId(), rule.squadLimit()) >= squad.capacity());
    }
}
