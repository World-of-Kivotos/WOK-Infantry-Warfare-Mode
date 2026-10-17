package com.wok.infantry.mixin;

import com.wok.infantry.deployment.DeploymentService;
import com.wok.infantry.network.ServerRequestLimiter;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Rejects managed inventory clicks before vanilla mutates any slot.
 *
 * <p>This cannot be implemented with ItemCraftedEvent: that event fires after QUICK_MOVE has
 * copied the result into the inventory, and mutating its stack can make vanilla skip consuming
 * the recipe inputs. The packet boundary is the earliest authoritative hook.</p>
 */
@Mixin(value = ServerGamePacketListenerImpl.class, remap = false)
public abstract class ServerGamePacketListenerImplMixin {
    @Inject(method = {"handleContainerClick", "m_5914_"}, at = @At("HEAD"),
            cancellable = true, remap = false)
    private void wokInfantry$guardManagedContainerClick(
            ServerboundContainerClickPacket packet, CallbackInfo callback) {
        ServerGamePacketListenerImpl listener =
                (ServerGamePacketListenerImpl) (Object) this;
        ServerPlayer player = listener.player;
        // This mixin runs before vanilla's identical guard. Off-thread calls are scheduled and
        // aborted here; only the main-thread re-entry below may touch menus or deployment state.
        PacketUtils.ensureRunningOnSameThread(packet, listener, player.serverLevel());

        DeploymentService deployment = DeploymentService.get(player).orElse(null);
        if (deployment == null) {
            return;
        }
        boolean waiting = deployment.isWaitingParticipant(player.getUUID());
        boolean active = deployment.isActive(player.getUUID());
        if (!waiting && !active) {
            return;
        }

        AbstractContainerMenu menu = player.containerMenu;
        if (packet.getContainerId() != menu.containerId) {
            // Preserve vanilla's stale-container handling and state-id resynchronization.
            return;
        }
        int slot = packet.getSlotNum();
        boolean personalCraftingSlot = menu == player.inventoryMenu && slot >= 0 && slot <= 4;
        // ACTIVE kits are slot-bound, and WAITING players have no external personal-storage
        // exception. Hotbar selection and normal item use do not use container-click packets.
        boolean forbidden = active || (menu == player.inventoryMenu
                ? personalCraftingSlot : true);
        if (!forbidden) {
            return;
        }

        callback.cancel();
        // Client predictions may already have moved a stack locally; restore authoritative state.
        // A shared per-player gate prevents a tiny malicious C2S click from amplifying into a
        // full 46-slot response on every packet.
        if (ServerRequestLimiter.allow(player, ServerRequestLimiter.Kind.CONTAINER_RESYNC)) {
            menu.sendAllDataToRemote();
        }
    }
}
