package com.wok.capturepoints.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.wok.capturepoints.capture.CapturePoint;
import com.wok.capturepoints.capture.CaptureSavedData;
import com.wok.capturepoints.capture.CaptureService;
import com.wok.capturepoints.capture.CaptureTeam;
import com.wok.capturepoints.config.CaptureConfig;
import com.wok.capturepoints.selection.SelectionStore;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.regex.Pattern;

public final class CaptureCommands {
    private static final Pattern ID = Pattern.compile("[a-z0-9][a-z0-9_-]{0,31}");

    private CaptureCommands() {
    }

    public static void register(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("wokcapture")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("point")
                        .then(Commands.literal("set")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(context -> set(context, null))
                                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                                .executes(context -> set(context,
                                                        StringArgumentType.getString(context, "name"))))))
                        .then(Commands.literal("remove")
                                .then(pointArgument().executes(CaptureCommands::remove)))
                        .then(Commands.literal("info")
                                .then(pointArgument().executes(CaptureCommands::info)))
                        .then(Commands.literal("owner")
                                .then(pointArgument().then(Commands.argument("team", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                new String[]{"neutral", "blue", "red"}, builder))
                                        .executes(CaptureCommands::owner))))
                        .then(Commands.literal("order")
                                .then(pointArgument().then(Commands.argument("value", IntegerArgumentType.integer())
                                        .executes(CaptureCommands::order))))
                        .then(Commands.literal("duration")
                                .then(pointArgument()
                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 3600))
                                                .executes(CaptureCommands::duration))
                                        .then(Commands.literal("default")
                                                .executes(CaptureCommands::durationDefault))))
                        .then(Commands.literal("enabled")
                                .then(pointArgument().then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(CaptureCommands::enabled))))
                        .then(Commands.literal("reset")
                                .then(pointArgument().executes(CaptureCommands::reset)))
                        .then(Commands.literal("list").executes(CaptureCommands::list)))
                .then(Commands.literal("rules")
                        .then(Commands.literal("status").executes(CaptureCommands::rulesStatus))
                        .then(Commands.literal("duration")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 3600))
                                        .executes(CaptureCommands::rulesDuration)))
                        .then(Commands.literal("sequential")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(CaptureCommands::rulesSequential)))
                        .then(Commands.literal("defaults").executes(CaptureCommands::rulesDefaults)))
                .then(Commands.literal("selection")
                        .then(Commands.literal("clear").executes(CaptureCommands::clearSelection)))
                .then(Commands.literal("sync").executes(CaptureCommands::sync)));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String>
    pointArgument() {
        return Commands.argument("id", StringArgumentType.word())
                .suggests((context, builder) -> CaptureService.get(context.getSource().getServer())
                        .map(service -> SharedSuggestionProvider.suggest(
                                service.data().points().stream().map(CapturePoint::id), builder))
                        .orElseGet(builder::buildFuture));
    }

    private static int set(CommandContext<CommandSourceStack> context, String requestedName)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String id = normalizeId(StringArgumentType.getString(context, "id"));
        if (id == null) return fail(context, "message.wok_capture_points.invalid_id");
        SelectionStore.Selection selection = SelectionStore.get(player.getUUID());
        if (selection == null || !selection.complete()) {
            return fail(context, "message.wok_capture_points.selection_incomplete");
        }
        int maxAxis = CaptureConfig.MAX_REGION_AXIS.get();
        if (Math.abs(selection.first().getX() - selection.second().getX()) + 1 > maxAxis
                || Math.abs(selection.first().getY() - selection.second().getY()) + 1 > maxAxis
                || Math.abs(selection.first().getZ() - selection.second().getZ()) + 1 > maxAxis) {
            return fail(context, "message.wok_capture_points.selection_too_large", maxAxis);
        }
        CaptureService service = CaptureService.start(context.getSource().getServer());
        CaptureSavedData data = service.data();
        CapturePoint point = data.point(id);
        if (point == null && data.size() >= CaptureConfig.MAX_POINTS.get()) {
            return fail(context, "message.wok_capture_points.too_many_points",
                    CaptureConfig.MAX_POINTS.get());
        }
        String name = requestedName == null || requestedName.isBlank()
                ? id.toUpperCase(Locale.ROOT) : requestedName.trim();
        if (name.length() > 64) name = name.substring(0, 64);
        if (point == null) {
            int inferredOrder = id.length() == 1 && Character.isLetter(id.charAt(0))
                    ? Character.toLowerCase(id.charAt(0)) - 'a' : data.nextOrder();
            point = new CapturePoint(id, name, selection.dimension(), selection.first(),
                    selection.second(), inferredOrder);
        } else {
            point.move(selection.dimension(), selection.first(), selection.second());
            if (requestedName != null && !requestedName.isBlank()) point.setDisplayName(name);
        }
        data.put(point);
        service.syncAll();
        CapturePoint savedPoint = point;
        context.getSource().sendSuccess(() -> Component.translatable(
                "message.wok_capture_points.point_set", savedPoint.displayName(), savedPoint.id(),
                savedPoint.min().toShortString(), savedPoint.max().toShortString()), true);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> context) {
        CaptureService service = CaptureService.start(context.getSource().getServer());
        String id = normalizeId(StringArgumentType.getString(context, "id"));
        CapturePoint removed = service.data().remove(id);
        if (removed == null) return fail(context, "message.wok_capture_points.not_found", id);
        service.syncAll();
        context.getSource().sendSuccess(() -> Component.translatable(
                "message.wok_capture_points.removed", removed.displayName()), true);
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> context) {
        CaptureService service = CaptureService.start(context.getSource().getServer());
        CapturePoint point = service.data().point(normalizeId(StringArgumentType.getString(context, "id")));
        if (point == null) return fail(context, "message.wok_capture_points.not_found",
                StringArgumentType.getString(context, "id"));
        int seconds = point.captureSecondsOverride() > 0
                ? point.captureSecondsOverride() : service.defaultCaptureSeconds();
        context.getSource().sendSuccess(() -> Component.translatable(
                "message.wok_capture_points.info", point.displayName(), point.id(), point.order(),
                point.owner().id(), seconds, point.enabled(), point.dimension(),
                point.min().toShortString(), point.max().toShortString()), false);
        return 1;
    }

    private static int owner(CommandContext<CommandSourceStack> context) {
        CaptureTeam team = CaptureTeam.byId(StringArgumentType.getString(context, "team")).orElse(null);
        if (team == null) return fail(context, "message.wok_capture_points.invalid_team");
        return mutatePoint(context, point -> point.setOwner(team), "message.wok_capture_points.owner_set");
    }

    private static int order(CommandContext<CommandSourceStack> context) {
        int value = IntegerArgumentType.getInteger(context, "value");
        return mutatePoint(context, point -> point.setOrder(value), "message.wok_capture_points.updated");
    }

    private static int duration(CommandContext<CommandSourceStack> context) {
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        return mutatePoint(context, point -> point.setCaptureSecondsOverride(seconds),
                "message.wok_capture_points.updated");
    }

    private static int durationDefault(CommandContext<CommandSourceStack> context) {
        return mutatePoint(context, point -> point.setCaptureSecondsOverride(-1),
                "message.wok_capture_points.updated");
    }

    private static int enabled(CommandContext<CommandSourceStack> context) {
        boolean value = BoolArgumentType.getBool(context, "value");
        return mutatePoint(context, point -> point.setEnabled(value),
                "message.wok_capture_points.updated");
    }

    private static int reset(CommandContext<CommandSourceStack> context) {
        return mutatePoint(context, point -> point.setOwner(CaptureTeam.NEUTRAL),
                "message.wok_capture_points.updated");
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        CaptureService service = CaptureService.start(context.getSource().getServer());
        if (service.data().size() == 0) return fail(context, "message.wok_capture_points.empty");
        for (CapturePoint point : service.data().points()) {
            context.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                    "%s (%s) order=%d owner=%s enabled=%s", point.displayName(), point.id(),
                    point.order(), point.owner().id(), point.enabled())), false);
        }
        return service.data().size();
    }

    private static int rulesStatus(CommandContext<CommandSourceStack> context) {
        CaptureService service = CaptureService.start(context.getSource().getServer());
        context.getSource().sendSuccess(() -> Component.translatable(
                "message.wok_capture_points.rules", service.defaultCaptureSeconds(),
                service.sequential(), CaptureConfig.CONTEST_MODE.get().name(),
                CaptureConfig.MAX_SPEED_PLAYERS.get()), false);
        return 1;
    }

    private static int rulesDuration(CommandContext<CommandSourceStack> context) {
        CaptureService service = CaptureService.start(context.getSource().getServer());
        service.data().setDurationOverride(IntegerArgumentType.getInteger(context, "seconds"));
        service.syncAll();
        return rulesStatus(context);
    }

    private static int rulesSequential(CommandContext<CommandSourceStack> context) {
        CaptureService service = CaptureService.start(context.getSource().getServer());
        service.data().setSequentialOverride(BoolArgumentType.getBool(context, "value"));
        service.syncAll();
        return rulesStatus(context);
    }

    private static int rulesDefaults(CommandContext<CommandSourceStack> context) {
        CaptureService service = CaptureService.start(context.getSource().getServer());
        service.data().setDurationOverride(-1);
        service.data().setSequentialOverride(null);
        service.syncAll();
        return rulesStatus(context);
    }

    private static int clearSelection(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        SelectionStore.clear(context.getSource().getPlayerOrException().getUUID());
        context.getSource().sendSuccess(() -> Component.translatable(
                "message.wok_capture_points.selection_cleared"), false);
        return 1;
    }

    private static int sync(CommandContext<CommandSourceStack> context) {
        CaptureService service = CaptureService.start(context.getSource().getServer());
        service.syncAll();
        context.getSource().sendSuccess(() -> Component.translatable(
                "message.wok_capture_points.synced"), false);
        return 1;
    }

    private static int mutatePoint(CommandContext<CommandSourceStack> context,
                                   java.util.function.Consumer<CapturePoint> mutation,
                                   String successKey) {
        CaptureService service = CaptureService.start(context.getSource().getServer());
        String id = normalizeId(StringArgumentType.getString(context, "id"));
        CapturePoint point = service.data().point(id);
        if (point == null) return fail(context, "message.wok_capture_points.not_found", id);
        mutation.accept(point);
        service.data().changed();
        service.syncAll();
        context.getSource().sendSuccess(() -> Component.translatable(successKey,
                point.displayName()), true);
        return 1;
    }

    private static String normalizeId(String value) {
        if (value == null) return null;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return ID.matcher(normalized).matches() ? normalized : null;
    }

    private static int fail(CommandContext<CommandSourceStack> context, String key,
                            Object... arguments) {
        context.getSource().sendFailure(Component.translatable(key, arguments));
        return 0;
    }
}
