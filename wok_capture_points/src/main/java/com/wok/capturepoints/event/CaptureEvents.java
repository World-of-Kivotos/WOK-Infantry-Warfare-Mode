package com.wok.capturepoints.event;

import com.wok.capturepoints.capture.CaptureService;
import com.wok.capturepoints.command.CaptureCommands;
import com.wok.capturepoints.config.CaptureConfig;
import com.wok.capturepoints.selection.SelectionStore;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class CaptureEvents {
    private CaptureEvents() {
    }

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        CaptureService.start(event.getServer());
    }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent event) {
        CaptureService.stop(event.getServer());
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            CaptureService.get(event.getServer()).ifPresent(CaptureService::tick);
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CaptureCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CaptureService.get(player.server).ifPresent(service -> service.sync(player));
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CaptureService.get(player.server).ifPresent(service -> service.sync(player));
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        SelectionStore.clear(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void selectFirst(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getHand() == InteractionHand.MAIN_HAND
                && event.getEntity() instanceof ServerPlayer player
                && canSelect(player) && isSelector(player)) {
            ResourceLocation dimension = player.level().dimension().location();
            SelectionStore.first(player.getUUID(), dimension, event.getPos());
            player.displayClientMessage(Component.translatable(
                    "message.wok_capture_points.first_selected", event.getPos().toShortString()), true);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void selectSecond(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() == InteractionHand.MAIN_HAND
                && event.getEntity() instanceof ServerPlayer player
                && canSelect(player) && isSelector(player)) {
            ResourceLocation dimension = player.level().dimension().location();
            SelectionStore.second(player.getUUID(), dimension, event.getPos());
            player.displayClientMessage(Component.translatable(
                    "message.wok_capture_points.second_selected", event.getPos().toShortString()), true);
            event.setCanceled(true);
        }
    }

    private static boolean canSelect(ServerPlayer player) {
        return player.hasPermissions(2);
    }

    private static boolean isSelector(ServerPlayer player) {
        ResourceLocation id = ResourceLocation.tryParse(CaptureConfig.SELECTOR_ITEM.get());
        if (id == null) return false;
        Item selector = BuiltInRegistries.ITEM.get(id);
        return player.getMainHandItem().is(selector);
    }
}
