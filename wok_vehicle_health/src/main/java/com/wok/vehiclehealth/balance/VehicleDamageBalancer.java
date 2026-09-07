package com.wok.vehiclehealth.balance;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class VehicleDamageBalancer {
    public static float normalize(VehicleEntity target,
                                  DamageSource source,
                                  float rawAmount,
                                  float addonComputedAmount) {
        VehicleBalanceProfile targetProfile = VehicleBalanceProfiles.find(target).orElse(null);
        if (targetProfile == null) {
            return addonComputedAmount;
        }

        Optional<VehicleExplosionContexts.PendingExplosion> explosion =
                VehicleExplosionContexts.consume(target);
        if (explosion.isPresent()) {
            InfantryAntiTankWeapon weapon = explosion.get().weapon();
            if (weapon == InfantryAntiTankWeapon.CARL_GUSTAF_THERMOBARIC) {
                return VehicleBalanceMath.infantryAntiTankDamage(
                        weapon, targetProfile.category(),
                        impactAspect(target, explosion.get().origin(), null));
            }
            // HEAT and M72 splash must not double-count their normalized direct impact.
            return 0.0F;
        }

        Entity direct = source.getDirectEntity();
        VehicleBalanceProfile attacker = attackingVehicleProfile(source, direct).orElse(null);
        if (source.is(ModDamageTypes.PROJECTILE_EXPLOSION)
                && SbwProjectileInspector.isArmorPiercingCannonShell(direct)
                && attacker != null
                && attacker.hasNormalizedTankGun()) {
            // AP lethality is represented by its normalized direct hit only.
            return 0.0F;
        }

        Optional<InfantryAntiTankWeapon> infantryWeapon = TaczProjectileInspector.inspect(direct);
        if (infantryWeapon.isPresent()) {
            if (!(rawAmount > 0.0F)) {
                return 0.0F;
            }
            InfantryAntiTankWeapon weapon = infantryWeapon.get();
            if (weapon == InfantryAntiTankWeapon.CARL_GUSTAF_THERMOBARIC) {
                return 0.0F;
            }
            return VehicleBalanceMath.infantryAntiTankDamage(
                    weapon, targetProfile.category(), impactAspect(target, null, direct));
        }

        if (source.is(ModDamageTypes.PROJECTILE_HIT)
                && SbwProjectileInspector.isArmorPiercingCannonShell(direct)
                && rawAmount >= 400.0F) {
            if (attacker != null && attacker.hasNormalizedTankGun()) {
                return VehicleBalanceMath.tankGunDamage(
                        attacker.tankGunArmorDamage(), targetProfile.category(),
                        impactAspect(target, null, direct));
            }
        }

        return addonComputedAmount;
    }

    private static Optional<VehicleBalanceProfile> attackingVehicleProfile(DamageSource source,
                                                                            Entity direct) {
        VehicleEntity vehicle = vehicleFrom(source.getEntity());
        if (vehicle == null && direct instanceof Projectile projectile) {
            vehicle = vehicleFrom(projectile.getOwner());
        }
        return vehicle == null ? Optional.empty() : VehicleBalanceProfiles.find(vehicle);
    }

    private static VehicleEntity vehicleFrom(Entity entity) {
        if (entity instanceof VehicleEntity vehicle) {
            return vehicle;
        }
        return entity != null && entity.getVehicle() instanceof VehicleEntity vehicle
                ? vehicle
                : null;
    }

    private static ImpactAspect impactAspect(VehicleEntity target,
                                             Vec3 explicitOrigin,
                                             Entity direct) {
        Vec3 toSource = explicitOrigin == null ? null : explicitOrigin.subtract(target.position());
        if ((toSource == null || toSource.lengthSqr() < 0.0001D) && direct != null) {
            Vec3 motion = direct.getDeltaMovement();
            if (motion.lengthSqr() > 0.0001D) {
                toSource = motion.scale(-1.0D);
            } else {
                toSource = direct.position().subtract(target.position());
            }
        }
        if (toSource == null || toSource.lengthSqr() < 0.0001D) {
            return ImpactAspect.SIDE;
        }
        Vec3 horizontal = new Vec3(toSource.x, 0.0D, toSource.z).normalize();
        double yaw = Math.toRadians(target.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        return VehicleBalanceMath.impactAspect(forward.dot(horizontal));
    }

    private VehicleDamageBalancer() {
    }
}
