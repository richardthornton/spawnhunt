package com.spawnhunt.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.spawnhunt.SpawnHuntMod;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ResultStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type DATA_TYPE = new TypeToken<Map<String, List<RunResult>>>() {}.getType();

    private static Map<String, List<RunResult>> cache = null;
    private static String cachedUuid = null;

    public static class RunResult {
        public long timeMs;
        public long timestamp;

        public RunResult(long timeMs, long timestamp) {
            this.timeMs = timeMs;
            this.timestamp = timestamp;
        }
    }

    private static String getUuid() {
        var user = Minecraft.getInstance().getUser();
        var uuid = user.getProfileId();
        return uuid != null ? uuid.toString() : "offline";
    }

    private static Path getFilePath() {
        String uuid = getUuid();
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("spawnhunt").resolve("results").resolve(uuid + ".json");
    }

    private static void ensureLoaded() {
        String uuid = getUuid();
        if (cache != null && uuid.equals(cachedUuid)) return;

        cachedUuid = uuid;
        Path path = getFilePath();

        if (Files.exists(path)) {
            try {
                String json = Files.readString(path);
                cache = GSON.fromJson(json, DATA_TYPE);
                if (cache == null) cache = new HashMap<>();
            } catch (Exception e) {
                SpawnHuntMod.LOGGER.warn("SpawnHunt: Failed to load results from {}, starting fresh", path, e);
                cache = new HashMap<>();
            }
        } else {
            cache = new HashMap<>();
        }
    }

    private static void save() {
        Path path = getFilePath();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(cache));
        } catch (IOException e) {
            SpawnHuntMod.LOGGER.warn("SpawnHunt: Failed to save results to {}", path, e);
        }
    }

    public static void recordResult(Identifier blockId, long timeMs) {
        ensureLoaded();
        String key = blockId.toString();
        cache.computeIfAbsent(key, k -> new ArrayList<>())
                .add(new RunResult(timeMs, System.currentTimeMillis()));
        save();
    }

    public static List<RunResult> getLastRuns(Identifier blockId, int count) {
        ensureLoaded();
        List<RunResult> runs = cache.getOrDefault(blockId.toString(), Collections.emptyList());
        if (runs.isEmpty()) return Collections.emptyList();

        List<RunResult> sorted = new ArrayList<>(runs);
        sorted.sort(Comparator.comparingLong((RunResult r) -> r.timestamp).reversed());
        return sorted.subList(0, Math.min(count, sorted.size()));
    }

    public static List<RunResult> getTopRuns(Identifier blockId, int count) {
        ensureLoaded();
        List<RunResult> runs = cache.getOrDefault(blockId.toString(), Collections.emptyList());
        if (runs.isEmpty()) return Collections.emptyList();

        List<RunResult> sorted = new ArrayList<>(runs);
        sorted.sort(Comparator.comparingLong(r -> r.timeMs));
        return sorted.subList(0, Math.min(count, sorted.size()));
    }

    public static long getBestTime(Identifier blockId) {
        ensureLoaded();
        List<RunResult> runs = cache.getOrDefault(blockId.toString(), Collections.emptyList());
        if (runs.isEmpty()) return -1;
        return runs.stream().mapToLong(r -> r.timeMs).min().orElse(-1);
    }
}
