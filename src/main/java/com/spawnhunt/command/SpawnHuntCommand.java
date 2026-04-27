package com.spawnhunt.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.spawnhunt.data.ItemPool;
import com.spawnhunt.data.HuntState;
import com.spawnhunt.data.ServerHuntState;
import com.spawnhunt.event.ServerHuntManager;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

import java.util.concurrent.ThreadLocalRandom;

public class SpawnHuntCommand {

    private static final SuggestionProvider<CommandSourceStack> ITEM_SUGGESTIONS = (context, builder) -> {
        String remaining = builder.getRemaining().toLowerCase();
        for (Item item : ItemPool.getPool()) {
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            if (id.startsWith(remaining) || id.startsWith("minecraft:" + remaining)) {
                builder.suggest(id);
            }
        }
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spawnhunt")
                .then(Commands.literal("start")
                        .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                        .then(Commands.literal("random")
                                .executes(SpawnHuntCommand::startRandom))
                        .then(Commands.argument("item", IdentifierArgument.id())
                                .suggests(ITEM_SUGGESTIONS)
                                .executes(SpawnHuntCommand::startSpecific)))
                .then(Commands.literal("stop")
                        .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                        .executes(SpawnHuntCommand::stop))
                .then(Commands.literal("restart")
                        .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                        .executes(SpawnHuntCommand::restartRandom)
                        .then(Commands.argument("item", IdentifierArgument.id())
                                .suggests(ITEM_SUGGESTIONS)
                                .executes(SpawnHuntCommand::restartSpecific)))
                .then(Commands.literal("status")
                        .executes(SpawnHuntCommand::status)));
    }

    private static int startRandom(CommandContext<CommandSourceStack> context) {
        if (ServerHuntState.isActive() && !ServerHuntState.isWon()) {
            context.getSource().sendFailure(Component.literal("A hunt is already active! Use /spawnhunt stop first."));
            return 0;
        }
        if (ServerHuntState.isActive()) {
            doStop(context);
        }
        return doStart(context, ItemPool.getRandomItem(ThreadLocalRandom.current()));
    }

    private static int startSpecific(CommandContext<CommandSourceStack> context) {
        if (ServerHuntState.isActive() && !ServerHuntState.isWon()) {
            context.getSource().sendFailure(Component.literal("A hunt is already active! Use /spawnhunt stop first."));
            return 0;
        }
        if (ServerHuntState.isActive()) {
            doStop(context);
        }
        Item item = resolveItem(context);
        if (item == null) return 0;
        return doStart(context, item);
    }

    private static int stop(CommandContext<CommandSourceStack> context) {
        if (!ServerHuntState.isActive()) {
            context.getSource().sendFailure(Component.literal("No hunt is currently active."));
            return 0;
        }

        doStop(context);

        Component message = Component.empty()
                .append(Component.literal("[SpawnHunt] ").withStyle(ChatFormatting.GOLD))
                .append(Component.literal("Hunt stopped.").withStyle(ChatFormatting.RED));

        ServerHuntManager.broadcastMessage(context.getSource().getServer(), message);
        return 1;
    }

    private static int restartRandom(CommandContext<CommandSourceStack> context) {
        doStop(context);
        return doStart(context, ItemPool.getRandomItem(ThreadLocalRandom.current()));
    }

    private static int restartSpecific(CommandContext<CommandSourceStack> context) {
        Item item = resolveItem(context);
        if (item == null) return 0;
        doStop(context);
        return doStart(context, item);
    }

    private static Item resolveItem(CommandContext<CommandSourceStack> context) {
        Identifier itemId = IdentifierArgument.getId(context, "item");

        if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
            context.getSource().sendFailure(Component.literal("Unknown item: " + itemId));
            return null;
        }

        Item item = BuiltInRegistries.ITEM.getValue(itemId);

        if (!ItemPool.getPool().contains(item)) {
            context.getSource().sendFailure(Component.literal("Item not in the SpawnHunt pool: " + itemId));
            return null;
        }

        return item;
    }

    private static int doStart(CommandContext<CommandSourceStack> context, Item item) {
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
        ServerHuntState.start(itemId);

        Component itemName = ItemPool.getDisplayName(item);
        Component message = Component.empty()
                .append(Component.literal("[SpawnHunt] ").withStyle(ChatFormatting.GOLD))
                .append(Component.literal("Hunt started! Find: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(itemName.getString()).withStyle(ChatFormatting.AQUA));

        ServerHuntManager.broadcastMessage(context.getSource().getServer(), message);
        return 1;
    }

    private static void doStop(CommandContext<CommandSourceStack> context) {
        ServerHuntState.stop();
        ServerHuntManager.sendStopSync(context.getSource().getServer());
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        if (!ServerHuntState.isActive()) {
            context.getSource().sendSuccess(() -> Component.literal("[SpawnHunt] No hunt active.").withStyle(ChatFormatting.GRAY), false);
            return 1;
        }

        Identifier targetId = ServerHuntState.getTargetItem();
        Item item = BuiltInRegistries.ITEM.getValue(targetId);
        Component itemName = ItemPool.getDisplayName(item);
        String timeStr = HuntState.formatTimeSeconds(ServerHuntState.getElapsedMs());
        int playerCount = context.getSource().getServer().getPlayerCount();

        Component status;
        if (ServerHuntState.isWon()) {
            status = Component.empty()
                    .append(Component.literal("[SpawnHunt] ").withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(ServerHuntState.getWinnerName()).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(" found ").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(itemName.getString()).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" in ").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(HuntState.formatTimeSeconds(ServerHuntState.getFinalTimeMs())).withStyle(ChatFormatting.WHITE));
        } else {
            status = Component.empty()
                    .append(Component.literal("[SpawnHunt] ").withStyle(ChatFormatting.GOLD))
                    .append(Component.literal("Target: ").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(itemName.getString()).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" | Time: ").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(timeStr).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" | Players: ").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(String.valueOf(playerCount)).withStyle(ChatFormatting.WHITE));
        }

        context.getSource().sendSuccess(() -> status, false);
        return 1;
    }
}
