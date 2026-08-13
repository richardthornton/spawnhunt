package com.spawnhunt.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

import java.util.UUID;

/**
 * Server-authoritative hunt state for multiplayer.
 * Uses wall-clock timing (no pause concept in multiplayer).
 */
public class ServerHuntState {
    private static boolean active = false;
    // Both forms of the target are kept so the per-tick inventory scan doesn't
    // have to hit the registry 20×/sec.
    private static Identifier targetId = null;
    private static Item targetItem = null;
    private static long startTimeMs = 0;
    private static boolean won = false;
    private static UUID winnerUuid = null;
    private static String winnerName = "";
    private static long finalTimeMs = 0;

    public static void start(Item item) {
        reset();
        active = true;
        targetItem = item;
        targetId = BuiltInRegistries.ITEM.getKey(item);
        startTimeMs = System.currentTimeMillis();
    }

    public static void stop() {
        reset();
    }

    public static void win(ServerPlayer player) {
        if (!active || won) return;
        finalTimeMs = System.currentTimeMillis() - startTimeMs;
        won = true;
        winnerUuid = player.getUUID();
        winnerName = player.getName().getString();
    }

    public static void reset() {
        active = false;
        targetId = null;
        targetItem = null;
        startTimeMs = 0;
        won = false;
        winnerUuid = null;
        winnerName = "";
        finalTimeMs = 0;
    }

    public static long getElapsedMs() {
        if (!active) return 0;
        if (won) return finalTimeMs;
        return System.currentTimeMillis() - startTimeMs;
    }

    public static boolean isActive() { return active; }
    public static Identifier getTargetId() { return targetId; }
    public static Item getTargetItem() { return targetItem; }
    public static boolean isWon() { return won; }
    public static UUID getWinnerUuid() { return winnerUuid; }
    public static String getWinnerName() { return winnerName; }
    public static long getFinalTimeMs() { return finalTimeMs; }
}
