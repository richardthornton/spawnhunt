package com.spawnhunt.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.spawnhunt.data.ItemPool;
import com.spawnhunt.data.HuntState;
import com.spawnhunt.data.ServerHuntState;
import com.spawnhunt.event.ServerHuntManager;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Random;

public class SpawnHuntCommand {

    private static final Random RANDOM = new Random();

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("spawnhunt")
                .then(CommandManager.literal("start")
                        .requires(source -> source.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS)))
                        .then(CommandManager.literal("random")
                                .executes(SpawnHuntCommand::startRandom))
                        .then(CommandManager.argument("item", IdentifierArgumentType.identifier())
                                .suggests((context, builder) -> {
                                    String remaining = builder.getRemaining().toLowerCase();
                                    for (Item item : ItemPool.getPool()) {
                                        String id = Registries.ITEM.getId(item).toString();
                                        if (id.startsWith(remaining) || id.startsWith("minecraft:" + remaining)) {
                                            builder.suggest(id);
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(SpawnHuntCommand::startSpecific)))
                .then(CommandManager.literal("stop")
                        .requires(source -> source.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS)))
                        .executes(SpawnHuntCommand::stop))
                .then(CommandManager.literal("restart")
                        .requires(source -> source.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS)))
                        .executes(SpawnHuntCommand::restartRandom)
                        .then(CommandManager.argument("item", IdentifierArgumentType.identifier())
                                .suggests((context, builder) -> {
                                    String remaining = builder.getRemaining().toLowerCase();
                                    for (Item item : ItemPool.getPool()) {
                                        String id = Registries.ITEM.getId(item).toString();
                                        if (id.startsWith(remaining) || id.startsWith("minecraft:" + remaining)) {
                                            builder.suggest(id);
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(SpawnHuntCommand::restartSpecific)))
                .then(CommandManager.literal("status")
                        .executes(SpawnHuntCommand::status)));
    }

    private static int startRandom(CommandContext<ServerCommandSource> context) {
        if (ServerHuntState.isActive()) {
            context.getSource().sendError(Text.literal("A hunt is already active! Use /spawnhunt stop first."));
            return 0;
        }

        Item item = ItemPool.getRandomItem(RANDOM);
        Identifier itemId = Registries.ITEM.getId(item);
        ServerHuntState.start(itemId);

        Text itemName = ItemPool.getDisplayName(item);
        Text message = Text.empty()
                .append(Text.literal("[SpawnHunt] ").formatted(Formatting.GOLD))
                .append(Text.literal("Hunt started! Find: ").formatted(Formatting.YELLOW))
                .append(Text.literal(itemName.getString()).formatted(Formatting.AQUA));

        ServerHuntManager.broadcastMessage(context.getSource().getServer(), message);
        return 1;
    }

    private static int startSpecific(CommandContext<ServerCommandSource> context) {
        if (ServerHuntState.isActive()) {
            context.getSource().sendError(Text.literal("A hunt is already active! Use /spawnhunt stop first."));
            return 0;
        }

        Identifier itemId = IdentifierArgumentType.getIdentifier(context, "item");
        Item item = Registries.ITEM.get(itemId);

        if (!ItemPool.getPool().contains(item)) {
            context.getSource().sendError(Text.literal("Item not in the SpawnHunt pool: " + itemId));
            return 0;
        }

        ServerHuntState.start(itemId);

        Text itemName = ItemPool.getDisplayName(item);
        Text message = Text.empty()
                .append(Text.literal("[SpawnHunt] ").formatted(Formatting.GOLD))
                .append(Text.literal("Hunt started! Find: ").formatted(Formatting.YELLOW))
                .append(Text.literal(itemName.getString()).formatted(Formatting.AQUA));

        ServerHuntManager.broadcastMessage(context.getSource().getServer(), message);
        return 1;
    }

    private static int stop(CommandContext<ServerCommandSource> context) {
        if (!ServerHuntState.isActive()) {
            context.getSource().sendError(Text.literal("No hunt is currently active."));
            return 0;
        }

        ServerHuntState.stop();

        Text message = Text.empty()
                .append(Text.literal("[SpawnHunt] ").formatted(Formatting.GOLD))
                .append(Text.literal("Hunt stopped.").formatted(Formatting.RED));

        ServerHuntManager.broadcastMessage(context.getSource().getServer(), message);
        ServerHuntManager.sendStopSync(context.getSource().getServer());
        return 1;
    }

    private static int restartRandom(CommandContext<ServerCommandSource> context) {
        ServerHuntState.stop();
        ServerHuntManager.sendStopSync(context.getSource().getServer());

        Item item = ItemPool.getRandomItem(RANDOM);
        Identifier itemId = Registries.ITEM.getId(item);
        ServerHuntState.start(itemId);

        Text itemName = ItemPool.getDisplayName(item);
        Text message = Text.empty()
                .append(Text.literal("[SpawnHunt] ").formatted(Formatting.GOLD))
                .append(Text.literal("New hunt! Find: ").formatted(Formatting.YELLOW))
                .append(Text.literal(itemName.getString()).formatted(Formatting.AQUA));

        ServerHuntManager.broadcastMessage(context.getSource().getServer(), message);
        return 1;
    }

    private static int restartSpecific(CommandContext<ServerCommandSource> context) {
        Identifier itemId = IdentifierArgumentType.getIdentifier(context, "item");
        Item item = Registries.ITEM.get(itemId);

        if (!ItemPool.getPool().contains(item)) {
            context.getSource().sendError(Text.literal("Item not in the SpawnHunt pool: " + itemId));
            return 0;
        }

        ServerHuntState.stop();
        ServerHuntManager.sendStopSync(context.getSource().getServer());
        ServerHuntState.start(itemId);

        Text itemName = ItemPool.getDisplayName(item);
        Text message = Text.empty()
                .append(Text.literal("[SpawnHunt] ").formatted(Formatting.GOLD))
                .append(Text.literal("New hunt! Find: ").formatted(Formatting.YELLOW))
                .append(Text.literal(itemName.getString()).formatted(Formatting.AQUA));

        ServerHuntManager.broadcastMessage(context.getSource().getServer(), message);
        return 1;
    }

    private static int status(CommandContext<ServerCommandSource> context) {
        if (!ServerHuntState.isActive()) {
            context.getSource().sendFeedback(() -> Text.literal("[SpawnHunt] No hunt active.").formatted(Formatting.GRAY), false);
            return 1;
        }

        Identifier targetId = ServerHuntState.getTargetItem();
        Item item = Registries.ITEM.get(targetId);
        Text itemName = ItemPool.getDisplayName(item);
        String timeStr = HuntState.formatTimeSeconds(ServerHuntState.getElapsedMs());
        int playerCount = context.getSource().getServer().getCurrentPlayerCount();

        Text status;
        if (ServerHuntState.isWon()) {
            status = Text.empty()
                    .append(Text.literal("[SpawnHunt] ").formatted(Formatting.GOLD))
                    .append(Text.literal(ServerHuntState.getWinnerName()).formatted(Formatting.GREEN))
                    .append(Text.literal(" found ").formatted(Formatting.YELLOW))
                    .append(Text.literal(itemName.getString()).formatted(Formatting.AQUA))
                    .append(Text.literal(" in ").formatted(Formatting.YELLOW))
                    .append(Text.literal(HuntState.formatTimeSeconds(ServerHuntState.getFinalTimeMs())).formatted(Formatting.WHITE));
        } else {
            status = Text.empty()
                    .append(Text.literal("[SpawnHunt] ").formatted(Formatting.GOLD))
                    .append(Text.literal("Target: ").formatted(Formatting.YELLOW))
                    .append(Text.literal(itemName.getString()).formatted(Formatting.AQUA))
                    .append(Text.literal(" | Time: ").formatted(Formatting.YELLOW))
                    .append(Text.literal(timeStr).formatted(Formatting.WHITE))
                    .append(Text.literal(" | Players: ").formatted(Formatting.YELLOW))
                    .append(Text.literal(String.valueOf(playerCount)).formatted(Formatting.WHITE));
        }

        context.getSource().sendFeedback(() -> status, false);
        return 1;
    }
}
