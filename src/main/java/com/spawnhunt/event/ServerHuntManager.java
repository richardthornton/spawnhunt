package com.spawnhunt.event;

import com.spawnhunt.data.HuntState;
import com.spawnhunt.data.ItemPool;
import com.spawnhunt.data.ServerHuntState;
import com.spawnhunt.network.HuntSyncS2CPayload;
import com.spawnhunt.network.HuntWinS2CPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class ServerHuntManager {

    private static int tickCounter = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ServerHuntManager::onServerTick);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (ServerHuntState.isActive()) {
                ServerPlayerEntity player = handler.getPlayer();
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

        Item targetItem = Registries.ITEM.get(targetId);

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (!stack.isEmpty() && stack.getItem() == targetItem) {
                    handleWin(server, player);
                    return;
                }
            }
        }
    }

    private static void handleWin(MinecraftServer server, ServerPlayerEntity winner) {
        ServerHuntState.win(winner);

        Identifier targetId = ServerHuntState.getTargetItem();
        Item item = Registries.ITEM.get(targetId);
        Text itemName = ItemPool.getDisplayName(item);
        String timeStr = HuntState.formatTimeSeconds(ServerHuntState.getFinalTimeMs());

        // Chat message to all players
        Text winMessage = Text.empty()
                .append(Text.literal("[SpawnHunt] ").formatted(Formatting.GOLD))
                .append(Text.literal(winner.getName().getString()).formatted(Formatting.GREEN))
                .append(Text.literal(" found ").formatted(Formatting.YELLOW))
                .append(Text.literal(itemName.getString()).formatted(Formatting.AQUA))
                .append(Text.literal(" in ").formatted(Formatting.YELLOW))
                .append(Text.literal(timeStr).formatted(Formatting.GREEN))
                .append(Text.literal("!").formatted(Formatting.YELLOW));

        broadcastMessage(server, winMessage);

        // Send win packet to mod clients + play sound for all
        HuntWinS2CPayload winPayload = new HuntWinS2CPayload(
                winner.getName().getString(),
                ServerHuntState.getFinalTimeMs(),
                targetId.toString()
        );

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
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
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, HuntSyncS2CPayload.ID)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    private static void broadcastActionBarToVanillaClients(MinecraftServer server) {
        if (!ServerHuntState.isActive() || ServerHuntState.isWon()) return;

        Identifier targetId = ServerHuntState.getTargetItem();
        if (targetId == null) return;

        Item item = Registries.ITEM.get(targetId);
        Text itemName = ItemPool.getDisplayName(item);
        String timeStr = HuntState.formatTimeSeconds(ServerHuntState.getElapsedMs());

        Text actionBar = Text.empty()
                .append(Text.literal("SpawnHunt").formatted(Formatting.GOLD))
                .append(Text.literal(" | ").formatted(Formatting.GRAY))
                .append(Text.literal(itemName.getString()).formatted(Formatting.AQUA))
                .append(Text.literal(" | ").formatted(Formatting.GRAY))
                .append(Text.literal(timeStr).formatted(Formatting.WHITE));

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!ServerPlayNetworking.canSend(player, HuntSyncS2CPayload.ID)) {
                player.sendMessage(actionBar, true);
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
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, HuntSyncS2CPayload.ID)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    /**
     * Sends a chat message to all players on the server.
     */
    public static void broadcastMessage(MinecraftServer server, Text message) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(message, false);
        }
    }
}
