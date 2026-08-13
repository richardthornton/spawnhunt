package com.spawnhunt.data;

import net.minecraft.resources.Identifier;

/**
 * Singleton holding all runtime state for the current hunt.
 * Not persisted — exiting the world ends the hunt.
 * <p>
 * Written only from the client thread (screens, client tick). {@code active} and
 * {@code won} are additionally read from the server thread by
 * {@link com.spawnhunt.mixin.GameModeLockMixin} on the integrated server, so those
 * two are volatile; the rest stay client-thread-only.
 * <p>
 * When arming or finishing a hunt, write the volatile flag <em>last</em>: a volatile write
 * only publishes the stores that precede it, so a reader seeing {@code active}/{@code won}
 * is then guaranteed to see the plain fields set alongside them.
 */
public class HuntState {
    private static volatile boolean active = false;
    private static Identifier targetItem = null;
    private static long startTimeMs = 0;
    private static long accumulatedMs = 0;
    private static long lastTickTimeMs = 0;
    private static boolean paused = false;
    private static volatile boolean won = false;
    private static long finalTimeMs = 0;
    private static boolean hardcore = true;

    public static void reset() {
        active = false;
        targetItem = null;
        startTimeMs = 0;
        accumulatedMs = 0;
        lastTickTimeMs = 0;
        paused = false;
        won = false;
        finalTimeMs = 0;
        hardcore = true;
    }

    public static void startHunt(Identifier item, boolean hardcoreMode) {
        reset();
        targetItem = item;
        hardcore = hardcoreMode;
        active = true; // volatile write last — publishes the fields above to any reader that sees it
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
        finalTimeMs = accumulatedMs;
        won = true; // volatile write last, as in startHunt
    }

    public static boolean isActive() { return active; }
    /** False between {@link #startHunt} and the world join that calls {@link #beginTimer}. */
    public static boolean hasTimerStarted() { return startTimeMs != 0; }
    public static Identifier getTargetItem() { return targetItem; }
    public static long getAccumulatedMs() { return accumulatedMs; }
    public static boolean isWon() { return won; }
    public static long getFinalTimeMs() { return finalTimeMs; }
    public static boolean isHardcore() { return hardcore; }

    public static String formatTime(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long millis = ms % 1000;
        return String.format("%02d:%02d.%03d", minutes, seconds, millis);
    }

    public static String formatTimeSeconds(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
