package com.wok.infantry.uitest.fixtures;

import com.wok.infantry.battle.Faction;

import java.util.List;
import java.util.Map;

/**
 * Demo data of the layout preview ({@code ui-preview/data/mock.js}) for the UI acceptance, so a
 * Java screenshot tells the same story as the preview PNG it is compared with. Ids are the core's
 * real ids ({@code FormationConfigData.defaultConfig}, {@code BattleRules.DEFAULT_CLASS_LIMITS});
 * display texts the game localises (classes, squads, roles) are not stored here — screens take them
 * from the language files. Names are player and server data, as on a real server.
 */
public final class MockData {
    private MockData() {
    }

    /** {@code viewer}: the acceptance player leads Alpha as an assault in the academy faction. */
    public static final String VIEWER_NAME = "WokGameplayTest";
    public static final String VIEWER_FACTION = "academy";
    public static final String VIEWER_SQUAD = "alpha";
    public static final String VIEWER_CLASS = "assault";

    /** {@code factions}: id, display name (server data), population and capacity. */
    public record FactionData(String id, String name, int population, int capacity) {
    }

    public static final List<FactionData> FACTIONS = List.of(
            new FactionData("academy", "学院军", 18, 40),
            new FactionData("caesar", "凯撒", 21, 40));

    /** {@code formations[].votes}: votes per formation id in the open vote. */
    public static final Map<String, Integer> VOTES = Map.of(
            "default", 3,
            "millennium_seminar_mobile", 5,
            "millennium_seminar_cavalry_corps", 1,
            "caesar_234_mechanized", 4);

    /** {@code vote.academy}: own vote and the shared (locked) formation. */
    public static final String OWN_VOTE = "millennium_seminar_mobile";
    public static final String LOCKED_FORMATION = "millennium_seminar_mobile";
    public static final int VOTED = 9;

    /**
     * The viewer's side in a fixture (0.5.0-beta.3, plan 6): the faction the viewer joined, its
     * battle side (which picks the tablet livery: blue → academy navy, red → Caesar red), the
     * enemy faction and the formation the viewer voted for and the faction locked. The Academy
     * side is the preview's demo viewer; the Caesar side mirrors it on the red side with the
     * default catalog's {@code caesar_234_mechanized}, so the same squads, classes and points are
     * shown in the red livery (preview {@code 18-device-livery.js}, states {@code caesar} and
     * {@code squads-caesar}).
     */
    public enum Side {
        ACADEMY(Faction.BLUE, 0, "millennium_seminar_mobile", "千禧年研讨会机动部队"),
        CAESAR(Faction.RED, 1, "caesar_234_mechanized", "234机械化作战单元");

        private final Faction faction;
        private final int own;
        private final String formation;
        private final String formationName;

        Side(Faction faction, int own, String formation, String formationName) {
            this.faction = faction;
            this.own = own;
            this.formation = formation;
            this.formationName = formationName;
        }

        /** Battle side of the viewer. */
        public Faction faction() {
            return faction;
        }

        /** The viewer's faction (id, catalog name, population, capacity). */
        public FactionData own() {
            return FACTIONS.get(own);
        }

        /** The other faction. */
        public FactionData enemy() {
            return FACTIONS.get(1 - own);
        }

        public String factionId() {
            return own().id();
        }

        /** The formation the viewer voted for and the faction locked. */
        public String lockedFormation() {
            return formation;
        }

        /** Catalog name of {@link #lockedFormation()} (server data). */
        public String formationName() {
            return formationName;
        }

        /** The viewer's own vote (the formation that is locked later). */
        public String ownVote() {
            return formation;
        }
    }

    /** {@code classes}: id, used and quota (the support class is full). */
    public record ClassData(String id, int used, int quota) {
    }

    public static final List<ClassData> CLASSES = List.of(
            new ClassData("assault", 3, 4),
            new ClassData("support", 2, 2),
            new ClassData("engineer", 1, 2),
            new ClassData("recon", 0, 1));

    /** {@code squads[].members}: name, role, class, online, alive, health and maximum. */
    public record MemberData(String name, String role, String classId, boolean online,
                             boolean alive, int health, int maxHealth) {
        public float healthRatio() {
            return maxHealth <= 0 ? 0.0F : (float) health / maxHealth;
        }
    }

    /** {@code squads}: callsign id, capacity, locked and members. */
    public record SquadData(String id, int capacity, boolean locked, List<MemberData> members) {
    }

    public static final List<SquadData> SQUADS = List.of(
            new SquadData("alpha", 8, false, List.of(
                    new MemberData("WokGameplayTest", "leader", "assault", true, true, 17, 20),
                    new MemberData("Hoshino_Takanashi", "member", "support", true, true, 9, 20),
                    new MemberData("Shiroko", "member", "assault", true, false, 0, 20),
                    new MemberData("Serika_K", "member", "engineer", true, true, 4, 20),
                    new MemberData("Nonomi", "member", "assault", false, true, 20, 20),
                    new MemberData("Ayane_Okusora", "member", "support", true, true, 20, 20))),
            new SquadData("bravo", 8, false, List.of(
                    new MemberData("Yuuka_Hayase", "commander", "engineer", true, true, 20, 20),
                    new MemberData("Noa_Ushio", "member", "recon", true, true, 14, 20),
                    new MemberData("Koyuki", "member", "assault", true, true, 20, 20))),
            new SquadData("charlie", 8, true, List.of(
                    new MemberData("Aris_Tendou", "leader", "support", true, true, 20, 20),
                    new MemberData("Momoi", "member", "assault", true, true, 20, 20),
                    new MemberData("Midori", "member", "recon", true, true, 20, 20),
                    new MemberData("Yuzu", "member", "engineer", true, true, 20, 20),
                    new MemberData("Hibiki_Nekozuka", "member", "assault", true, true, 20, 20),
                    new MemberData("Utaha_Shiraishi", "member", "engineer", true, true, 20, 20),
                    new MemberData("Kotori_Toyomi", "member", "support", true, true, 20, 20),
                    new MemberData("Neru_Mikamo", "member", "assault", true, true, 20, 20))),
            new SquadData("delta", 8, false, List.of()),
            new SquadData("echo", 8, false, List.of()));

    /** {@code supports}: id, preview name (server data), status, seconds and radius. */
    public record SupportData(String id, String name, String status, int seconds, int radius) {
    }

    public static final List<SupportData> SUPPORTS = List.of(
            new SupportData("wok_commander_support:recon_satellite", "卫星侦察", "ready", 0, 96),
            new SupportData("wok_commander_support:f16c_gbu12_paveway_500lb",
                    "F-16C GBU-12 500磅", "cooldown", 184, 18),
            new SupportData("wok_commander_support:millennium_f15ex_jdam_1000lb",
                    "F-15EX JDAM 1000磅", "inbound", 12, 26));

    /**
     * Support name table of the formation catalog: id, the add-on's translation key and the
     * fallback name its server registers (WOK步战附属-指挥官支援 is not loaded in the acceptance,
     * so the fallback is shown, as on a client without the add-on's language file).
     */
    public record SupportLabelData(String id, String translationKey, String fallbackName) {
    }

    public static final List<SupportLabelData> SUPPORT_LABELS = List.of(
            support("recon_satellite", "侦察卫星"),
            support("recon_drone", "无人机侦察"),
            support("millennium_f15ex_jdam_1000lb", "千禧年 F-15EX 杰达姆 1000磅空袭"),
            support("f16c_gbu12_paveway_500lb", "F-16C GBU-12 宝石路 II 500磅精准空袭"),
            support("howitzer_3round_barrage", "三连发榴弹炮击"),
            support("howitzer_105mm_rapid_3round_barrage", "快速三连发105毫米榴弹炮打击"),
            support("howitzer_105mm_5round_barrage", "五连发105毫米榴弹炮打击"));

    /** Five allow-listed supports of the {@code longcaps} formation (beta.7 capability lines). */
    public static final List<String> LONG_CAPS_SUPPORTS = List.of(
            "wok_commander_support:recon_satellite",
            "wok_commander_support:recon_drone",
            "wok_commander_support:howitzer_3round_barrage",
            "wok_commander_support:howitzer_105mm_rapid_3round_barrage",
            "wok_commander_support:howitzer_105mm_5round_barrage");

    /** Catalog names of the formations the fixtures add (preview {@code RESERVE} and longcaps). */
    public static final String RESERVE_NAME = "研讨会骑兵预备队";
    public static final String LONG_CAPS_NAME = "研讨会联合火力支援群";

    private static SupportLabelData support(String path, String fallbackName) {
        return new SupportLabelData("wok_commander_support:" + path,
                "support.wok_commander_support." + path, fallbackName);
    }

    /** {@code battle}: tickets of both sides and their maximum, objective B at 62%. */
    public static final int TICKETS_BLUE = 412;
    public static final int TICKETS_RED = 377;
    public static final int TICKETS_MAX = 500;
    public static final String OBJECTIVE = "B";
    public static final float OBJECTIVE_PROGRESS = 0.62F;

    /** {@code stamina}: arms and legs, 0–100. */
    public static final float STAMINA_ARMS = 72.0F;
    public static final float STAMINA_LEGS = 38.0F;

    /** {@code body.total}: body-health total and maximum. */
    public static final int BODY_TOTAL = 290;
    public static final int BODY_MAX = 405;

    /** The squad the viewer leads. */
    public static SquadData viewerSquad() {
        return SQUADS.get(0);
    }
}
