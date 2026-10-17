package com.wok.infantry.client;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.battle.CombatantStatus;
import com.wok.infantry.config.BattleGameplayConfig;
import com.wok.infantry.integration.tacz.ProneStability;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import java.lang.reflect.Method;
import java.util.Optional;

/** A continuous stance timeline; firing/NBT updates do not restart it. */
public final class ClientProneStability {
    private static float stability;
    private static String heldId = "";
    private static int heldSlot = -1;
    private static boolean machineGun;
    private static double lastX, lastZ;
    private static Method crawlGetter;
    private static boolean loggedFailure;

    public static void tick() {
        var player = Minecraft.getInstance().player;
        if (player == null || !ModList.get().isLoaded("tacz")) { reset(); return; }
        ItemStack stack = player.getMainHandItem();
        String id = stack.hasTag() ? stack.getTag().getString("GunId") : "";
        if (!id.equals(heldId) || player.getInventory().selected != heldSlot) {
            stability = 0;
            machineGun = isMachineGun(id);
            heldId = id;
            heldSlot = player.getInventory().selected;
            lastX = player.getX(); lastZ = player.getZ();
        }
        boolean prone = machineGun && player.isAlive() && !CombatantStatus.isDowned(player)
                && player.getPose() == Pose.SWIMMING && !player.isInWater() && player.onGround();
        double dx = player.getX() - lastX, dz = player.getZ() - lastZ;
        boolean moving = dx * dx + dz * dz > 0.000025D;
        stability = ProneStability.advance(stability, prone, moving, BattleGameplayConfig.PRONE_SETTLE_TICKS.get());
        lastX = player.getX(); lastZ = player.getZ();
    }

    private static boolean isMachineGun(String id) {
        if (id.equals("tacz:m249")) return true;
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) return false;
        try {
            Object value = Class.forName("com.tacz.guns.api.TimelessAPI")
                    .getMethod("getClientGunIndex", ResourceLocation.class).invoke(null, location);
            if (!(value instanceof Optional<?> index) || index.isEmpty()) return false;
            String type = String.valueOf(index.get().getClass().getMethod("getType").invoke(index.get()));
            return type.equalsIgnoreCase("mg") || type.equalsIgnoreCase("machine_gun");
        } catch (ReflectiveOperationException ignored) { return false; }
    }

    public static float crawlMultiplier(Object gunData) {
        try {
            if (crawlGetter == null) crawlGetter = gunData.getClass().getMethod("getCrawlRecoilMultiplier");
            float original = ((Number) crawlGetter.invoke(gunData)).floatValue();
            return machineGun ? ProneStability.recoilMultiplier(original, stability) : original;
        } catch (ReflectiveOperationException exception) {
            if (!loggedFailure) {
                loggedFailure = true;
                WokInfantryMod.LOGGER.error("Could not read TaCZ prone recoil multiplier", exception);
            }
            return 1.0F;
        }
    }

    public static void reset() { stability = 0; heldId = ""; heldSlot = -1; machineGun = false; }
    private ClientProneStability() {}
}
