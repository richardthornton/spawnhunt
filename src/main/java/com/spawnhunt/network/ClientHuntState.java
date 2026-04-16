package com.spawnhunt.network;

import net.minecraft.resources.Identifier;

/**
 * Client-side mirror of the server's hunt state, updated via network packets.
 * Used by the HUD renderer when playing on a multiplayer server with SpawnHunt.
 */
public class ClientHuntState {
    private static boolean active = false;
    private static Identifier targetItem = null;
    private static long elapsedMs = 0;
    private static boolean won = false;
    private static String winnerName = "";
    private static long finalTimeMs = 0;

    public static void update(HuntSyncS2CPayload payload) {
        active = payload.active();
        targetItem = (payload.active() && !payload.targetItemId().isEmpty())
                ? Identifier.parse(payload.targetItemId()) : null;
        elapsedMs = payload.elapsedMs();
        won = payload.won();
        winnerName = payload.winnerName();
        finalTimeMs = payload.finalTimeMs();
    }

    public static void handleWin(HuntWinS2CPayload payload) {
        won = true;
        winnerName = payload.winnerName();
        finalTimeMs = payload.finalTimeMs();
        targetItem = Identifier.parse(payload.targetItemId());
    }

    public static void reset() {
        active = false;
        targetItem = null;
        elapsedMs = 0;
        won = false;
        winnerName = "";
        finalTimeMs = 0;
    }

    public static boolean isActive() { return active; }
    public static Identifier getTargetItem() { return targetItem; }
    public static long getElapsedMs() { return elapsedMs; }
    public static boolean isWon() { return won; }
    public static String getWinnerName() { return winnerName; }
    public static long getFinalTimeMs() { return finalTimeMs; }
}
