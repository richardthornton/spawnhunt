package com.spawnhunt.data;

import net.minecraft.util.Identifier;

/**
 * Singleton holding all runtime state for the current hunt.
 * Not persisted — exiting the world ends the hunt.
 */
public class HuntState {
    private static boolean active = false;
    private static Identifier targetItem = null;
    private static long startTimeMs = 0;
    private static long accumulatedMs = 0;
    private static long lastTickTimeMs = 0;
    private static boolean paused = false;
    private static boolean won = false;
    private static long finalTimeMs = 0;

    public static void reset() {
        active = false;
        targetItem = null;
        startTimeMs = 0;
        accumulatedMs = 0;
        lastTickTimeMs = 0;
        paused = false;
        won = false;
        finalTimeMs = 0;
    }

    public static void startHunt(Identifier item) {
        reset();
        active = true;
        targetItem = item;
    }

    public static void beginTimer() {
        startTimeMs = System.currentTimeMillis();
        lastTickTimeMs = startTimeMs;
    }

    public static void tick(boolean gamePaused) {
        if (!active || won || lastTickTimeMs == 0) return;

        long now = System.currentTimeMillis();
        if (!gamePaused && !paused) {
            accumulatedMs += now - lastTickTimeMs;
        }
        lastTickTimeMs = now;
        paused = gamePaused;
    }

    public static void win() {
        if (!active || won) return;
        won = true;
        finalTimeMs = accumulatedMs;
    }

    public static boolean isActive() { return active; }
    public static Identifier getTargetItem() { return targetItem; }
    public static long getAccumulatedMs() { return accumulatedMs; }
    public static boolean isWon() { return won; }
    public static long getFinalTimeMs() { return finalTimeMs; }

    public static String formatTime(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long millis = ms % 1000;
        return String.format("%02d:%02d.%03d", minutes, seconds, millis);
    }
}
