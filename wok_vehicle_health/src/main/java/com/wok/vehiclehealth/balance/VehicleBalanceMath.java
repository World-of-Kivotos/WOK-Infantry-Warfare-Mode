package com.wok.vehiclehealth.balance;

public final class VehicleBalanceMath {
    public static float tankGunDamage(float standardizedArmorDamage,
                                      VehicleCategory targetCategory,
                                      ImpactAspect aspect) {
        if (!(standardizedArmorDamage > 0.0F)) {
            return 0.0F;
        }
        return standardizedArmorDamage * tankGunAspectMultiplier(targetCategory, aspect);
    }

    public static float infantryAntiTankDamage(InfantryAntiTankWeapon weapon,
                                               VehicleCategory targetCategory,
                                               ImpactAspect aspect) {
        if (weapon == InfantryAntiTankWeapon.CARL_GUSTAF_THERMOBARIC) {
            return switch (targetCategory) {
                case TANK -> 40.0F;
                case ARMORED_FIGHTING_VEHICLE -> 60.0F;
                case LIGHT_TACTICAL, HELICOPTER, LOGISTICS -> 120.0F;
            };
        }

        float baseDamage = weapon == InfantryAntiTankWeapon.CARL_GUSTAF_HEAT
                ? 320.0F
                : 200.0F;
        if (weapon == InfantryAntiTankWeapon.M72_LIGHT_AT
                && targetCategory == VehicleCategory.TANK) {
            baseDamage *= 0.75F;
        }
        return baseDamage * infantryAtAspectMultiplier(targetCategory, aspect);
    }

    public static int shotsToKill(float health, float damagePerHit) {
        if (!(health > 0.0F) || !(damagePerHit > 0.0F)) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.ceil(health / damagePerHit);
    }

    public static ImpactAspect impactAspect(double forwardDot) {
        if (forwardDot > 0.5D) {
            return ImpactAspect.FRONT;
        }
        if (forwardDot < -0.5D) {
            return ImpactAspect.REAR;
        }
        return ImpactAspect.SIDE;
    }

    private static float tankGunAspectMultiplier(VehicleCategory targetCategory,
                                                 ImpactAspect aspect) {
        return switch (targetCategory) {
            case TANK -> switch (aspect) {
                case FRONT -> 0.50F;
                case SIDE -> 0.85F;
                case REAR -> 1.00F;
            };
            // Large-caliber tank AP overmatches AFV side and rear armor.
            case ARMORED_FIGHTING_VEHICLE -> switch (aspect) {
                case FRONT -> 0.65F;
                case SIDE -> 1.20F;
                case REAR -> 1.35F;
            };
            case LIGHT_TACTICAL, LOGISTICS -> switch (aspect) {
                case FRONT -> 0.80F;
                case SIDE -> 1.00F;
                case REAR -> 1.15F;
            };
            case HELICOPTER -> 1.00F;
        };
    }

    private static float infantryAtAspectMultiplier(VehicleCategory targetCategory,
                                                     ImpactAspect aspect) {
        return switch (targetCategory) {
            case TANK -> switch (aspect) {
                case FRONT -> 0.50F;
                case SIDE -> 0.85F;
                case REAR -> 1.00F;
            };
            case ARMORED_FIGHTING_VEHICLE -> switch (aspect) {
                case FRONT -> 0.65F;
                case SIDE -> 0.90F;
                case REAR -> 1.00F;
            };
            case LIGHT_TACTICAL, LOGISTICS -> switch (aspect) {
                case FRONT -> 0.80F;
                case SIDE -> 1.00F;
                case REAR -> 1.15F;
            };
            case HELICOPTER -> 1.00F;
        };
    }

    private VehicleBalanceMath() {
    }
}
