package com.wok.infantryarmor.armor;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Single source of truth for plate-armor body-part coverage.
 *
 * <p>The table is a seven-part projection of current Escape from Tarkov armor zones. Keep
 * BODY_PART_PROTECTION_MATRIX.md and these groups in sync whenever upstream coverage changes.</p>
 */
public final class PlateArmorCoverage {

    private static final Map<PlateArmorVariant, Coverage> COVERAGE = new EnumMap<>(PlateArmorVariant.class);
    private static final ProtectedBodyPart[] CHEST_ONLY = {
            ProtectedBodyPart.CHEST
    };
    private static final ProtectedBodyPart[] TORSO = {
            ProtectedBodyPart.CHEST,
            ProtectedBodyPart.ABDOMEN
    };
    private static final ProtectedBodyPart[] TORSO_AND_ARMS = {
            ProtectedBodyPart.CHEST,
            ProtectedBodyPart.ABDOMEN,
            ProtectedBodyPart.LEFT_ARM,
            ProtectedBodyPart.RIGHT_ARM
    };

    static {
        Arrays.stream(PlateArmorVariant.values())
                .forEach(variant -> COVERAGE.put(variant, Coverage.unconfigured()));

        // Current Escape from Tarkov plate-only carriers: front/back plates or upper-rib soft armor only.
        configure(CHEST_ONLY,
                PlateArmorVariant.JAYPC_OLIVE,
                PlateArmorVariant.JAYPC_BLACK,
                PlateArmorVariant.MBSS,
                PlateArmorVariant.TV115,
                PlateArmorVariant.TV110_COYOTE,
                PlateArmorVariant.TACTEC_RANGER_GREEN,
                PlateArmorVariant.TT_MKIII_COYOTE,
                PlateArmorVariant.HEXATAC_HPC_BLACK_MULTICAM,
                PlateArmorVariant.HEXGRID,
                PlateArmorVariant.SLICK);

        // Tarkov thorax/back plus stomach/sides/groin coverage, collapsed to WOK chest and abdomen.
        configure(TORSO,
                PlateArmorVariant.PACA,
                PlateArmorVariant.B6B23_1_DIGITAL_FLORA,
                PlateArmorVariant.B6B5_16,
                PlateArmorVariant.KIRASA_N_GREEN,
                PlateArmorVariant.MF_UNTAR,
                PlateArmorVariant.KORA_KULON,
                PlateArmorVariant.KORA_KULON_DIGITAL,
                PlateArmorVariant.MMAC_RANGER_GREEN,
                PlateArmorVariant.RBAV_AF_RANGER_GREEN,
                PlateArmorVariant.STRANDHOGG_RANGER_GREEN,
                PlateArmorVariant.STRANDHOGG_BLACK_MULTICAM,
                PlateArmorVariant.TROOPER_TFO_MULTICAM,
                PlateArmorVariant.BANSHEE_ATACS_AU,
                PlateArmorVariant.B6B13_FLORA,
                PlateArmorVariant.B6B3TM_01M_KHAKI,
                PlateArmorVariant.ANA_M1_OLIVE,
                PlateArmorVariant.A18_SKANDA_MULTICAM,
                PlateArmorVariant.AVS_RANGER_GREEN,
                PlateArmorVariant.AVS_MULTICAM,
                PlateArmorVariant.THOR_CONCEALABLE,
                PlateArmorVariant.STICH_PROFI_V2_BLACK,
                PlateArmorVariant.B6B23_2_MOUNTAIN_FLORA,
                PlateArmorVariant.B6B5_15_FLORA,
                PlateArmorVariant.CPC_MOD1_ATACS_FG,
                PlateArmorVariant.FCPC_V5,
                PlateArmorVariant.GLADIATOR_S_LIGHT_MULTICAM,
                PlateArmorVariant.B6B45_GENERAL,
                PlateArmorVariant.B6B45_MEDIC,
                PlateArmorVariant.GZHEL_K,
                PlateArmorVariant.GLADIATOR_S_GRAY,
                PlateArmorVariant.GLADIATOR_S_VIKING,
                PlateArmorVariant.DEFENDER_2_SPOT_CAMO,
                PlateArmorVariant.DEFENDER_2,
                PlateArmorVariant.REDUT_M,
                PlateArmorVariant.IOTV_GEN4_HIGH_MOBILITY,
                PlateArmorVariant.KORUND_VM_BLACK,
                PlateArmorVariant.STICH_DEFENSE_MOD2);

        // Tarkov models with explicit left/right upper-arm armor.
        configure(TORSO_AND_ARMS,
                PlateArmorVariant.OSPREY_MK4A_ASSAULT,
                PlateArmorVariant.OSPREY_MK4A_PROTECTION,
                PlateArmorVariant.GLADIATOR_S_DEATHLESS,
                PlateArmorVariant.IOTV_GEN4_FULL_PROTECTION,
                PlateArmorVariant.IOTV_GEN4_ASSAULT,
                PlateArmorVariant.B6B43_ZABRALO_SH,
                PlateArmorVariant.THOR_INTEGRATED);
    }

    private PlateArmorCoverage() {
    }

    public static Coverage forVariant(PlateArmorVariant variant) {
        return COVERAGE.getOrDefault(variant, Coverage.unconfigured());
    }

    private static void configure(ProtectedBodyPart[] parts, PlateArmorVariant... variants) {
        for (PlateArmorVariant variant : variants) {
            COVERAGE.put(variant, Coverage.of(parts));
        }
    }

    public record Coverage(boolean configured, List<ProtectedBodyPart> parts) {

        public Coverage {
            parts = List.copyOf(parts);
        }

        public static Coverage unconfigured() {
            return new Coverage(false, List.of());
        }

        public static Coverage of(ProtectedBodyPart... parts) {
            EnumSet<ProtectedBodyPart> ordered = EnumSet.noneOf(ProtectedBodyPart.class);
            ordered.addAll(Arrays.asList(parts));
            return new Coverage(true, List.copyOf(ordered));
        }

        public boolean protects(ProtectedBodyPart part) {
            return configured && parts.contains(part);
        }
    }
}
