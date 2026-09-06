package com.wok.infantry.event;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.block.entity.RallyRadioBlockEntity;
import com.wok.infantry.registry.InfantryBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-authoritative damage bridge for rally radios, which vanilla blocks do not health-track. */
@Mod.EventBusSubscriber(modid = WokInfantryMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RallyRadioEvents {
    private static final int MELEE_DAMAGE = 10;
    private static final int PROJECTILE_DAMAGE = 25;
    private static final int EXPLOSION_DAMAGE = 100;

    private RallyRadioEvents() {
    }

    @SubscribeEvent
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !level.getBlockState(event.getPos()).is(InfantryBlocks.RALLY_RADIO.get())) {
            return;
        }
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer player) {
            damage(level, event.getPos(), player, MELEE_DAMAGE);
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof BlockHitResult hit)
                || !(event.getProjectile().level() instanceof ServerLevel level)
                || !level.getBlockState(hit.getBlockPos()).is(
                InfantryBlocks.RALLY_RADIO.get())) {
            return;
        }
        Projectile projectile = event.getProjectile();
        damage(level, hit.getBlockPos(), projectile.getOwner(), PROJECTILE_DAMAGE);
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Entity source = event.getExplosion().getExploder();
        event.getAffectedBlocks().stream().filter(pos -> level.getBlockState(pos).is(
                        InfantryBlocks.RALLY_RADIO.get()))
                .toList().forEach(pos -> damage(level, pos, source, EXPLOSION_DAMAGE));
        event.getAffectedBlocks().removeIf(pos -> level.getBlockState(pos).is(
                InfantryBlocks.RALLY_RADIO.get()));
    }

    private static void damage(ServerLevel level, BlockPos pos, Entity attacker, int amount) {
        if (!(level.getBlockEntity(pos) instanceof RallyRadioBlockEntity radio)
                || !radio.initialized() || friendly(level, radio, attacker)) return;
        if (radio.damage(amount)) {
            level.destroyBlock(pos, false, attacker);
        } else {
            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 2);
        }
    }

    private static boolean friendly(ServerLevel level, RallyRadioBlockEntity radio,
                                    Entity attacker) {
        if (!(attacker instanceof ServerPlayer player) || radio.faction() == null) return false;
        return BattleService.get(level.getServer())
                .flatMap(service -> service.factionOf(player.getUUID()))
                .filter(radio.faction()::equals).isPresent();
    }
}
