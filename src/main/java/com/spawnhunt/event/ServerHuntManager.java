package com.spawnhunt.event;

import com.spawnhunt.data.HuntState;
import com.spawnhunt.data.ItemPool;
import com.spawnhunt.data.ServerHuntState;
import com.spawnhunt.network.HuntSyncS2CPayload;
import com.spawnhunt.network.HuntWinS2CPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

public class ServerHuntManager {

    private static int tickCounter = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ServerHuntManager::onServerTick);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (ServerHuntState.isActive()) {
                ServerPlayer player = handler.getPlayer();
                if (ServerPlayNetworking.canSend(player, HuntSyncS2CPayload.ID)) {
                    ServerPlayNetworking.send(player, buildSyncPayload());
                }
            }
        });
    }

    private static void onServerTick(MinecraftServer server) {
        if (!ServerHuntState.isActive()) {
            tickCounter = 0;
            return;
        }

        tickCounter++;

        // Scan inventories for win detection (every tick when hunt is active and not won)
        if (!ServerHuntState.isWon()) {
            scanInventories(server);
        }

        // Broadcast to mod clients every 5 ticks (~250ms)
        if (tickCounter % 5 == 0) {
            broadcastSyncToModClients(server);
        }

        // Broadcast to vanilla clients every 20 ticks (~1 second)
        if (tickCounter % 20 == 0) {
            broadcastActionBarToVanillaClients(server);
        }
    }

    private static void scanInventories(MinecraftServer server) {
        Identifier targetId = ServerHuntState.getTargetItem();
        if (targetId == null) return;

        Item targetItem = BuiltInRegistries.ITEM.getValue(targetId);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty() && stack.getItem() == targetItem) {
                    handleWin(server, player);
                    return;
                }
            }
        }
    }

    private static void handleWin(MinecraftServer server, ServerPlayer winner) {
        ServerHuntState.win(winner);

        Identifier targetId = ServerHuntState.getTargetItem();
        Item item = BuiltInRegistries.ITEM.getValue(targetId);
        Component itemName = ItemPool.getDisplayName(item);
        String timeStr = HuntState.formatTimeSeconds(ServerHuntState.getFinalTimeMs());

        // Chat message to all players
        Component winMessage = Component.empty()
                .append(Component.literal("[SpawnHunt] ").withStyle(ChatFormatting.GOLD))
                .append(Component.literal(winner.getName().getString()).withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" found ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(itemName.getString()).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" in ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(timeStr).withStyle(ChatFormatting.GREEN))
                .append(Component.literal("!").withStyle(ChatFormatting.YELLOW));

        broadcastMessage(server, winMessage);

        // Send win packet to mod clients + play sound for all
        HuntWinS2CPayload winPayload = new HuntWinS2CPayload(
                winner.getName().getString(),
                ServerHuntState.getFinalTimeMs(),
                targetId.toString()
        );

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (ServerPlayNetworking.canSend(player, HuntWinS2CPayload.ID)) {
                ServerPlayNetworking.send(player, winPayload);
            } else {
                player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
        }

        // Send final sync to mod clients
        broadcastSyncToModClients(server);
    }

    private static void broadcastSyncToModClients(MinecraftServer server) {
        HuntSyncS2CPayload payload = buildSyncPayload();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (ServerPlayNetworking.canSend(player, HuntSyncS2CPayload.ID)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    private static void broadcastActionBarToVanillaClients(MinecraftServer server) {
        if (!ServerHuntState.isActive() || ServerHuntState.isWon()) return;

        Identifier targetId = ServerHuntState.getTargetItem();
        if (targetId == null) return;

        Item item = BuiltInRegistries.ITEM.getValue(targetId);
        Component itemName = ItemPool.getDisplayName(item);
        String timeStr = HuntState.formatTimeSeconds(ServerHuntState.getElapsedMs());

        Component actionBar = Component.empty()
                .append(Component.literal("SpawnHunt").withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" | ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(itemName.getString()).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" | ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(timeStr).withStyle(ChatFormatting.WHITE));

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!ServerPlayNetworking.canSend(player, HuntSyncS2CPayload.ID)) {
                player.sendOverlayMessage(actionBar);
            }
        }
    }

    private static HuntSyncS2CPayload buildSyncPayload() {
        return new HuntSyncS2CPayload(
                ServerHuntState.isActive(),
                ServerHuntState.getTargetItem() != null ? ServerHuntState.getTargetItem().toString() : "",
                ServerHuntState.getElapsedMs(),
                ServerHuntState.isWon(),
                ServerHuntState.getWinnerName(),
                ServerHuntState.getFinalTimeMs()
        );
    }

    /**
     * Sends an inactive sync payload to all mod clients (used when hunt is stopped).
     */
    public static void sendStopSync(MinecraftServer server) {
        HuntSyncS2CPayload payload = new HuntSyncS2CPayload(false, "", 0, false, "", 0);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (ServerPlayNetworking.canSend(player, HuntSyncS2CPayload.ID)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    /**
     * Sends a chat message to all players on the server.
     */
    public static void broadcastMessage(MinecraftServer server, Component message) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(message);
        }
    }
}
