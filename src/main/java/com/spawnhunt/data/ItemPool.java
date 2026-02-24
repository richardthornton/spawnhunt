package com.spawnhunt.data;

import com.spawnhunt.SpawnHuntMod;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.*;

/**
 * Maintains a cached pool of all survival-obtainable items (blocks, tools, armor, food, materials, etc.).
 * <p>
 * Approach: iterate the entire item registry, keep minecraft-namespace items,
 * exclude unobtainables and variant-dependent items via an exclusion set + patterns.
 */
public class ItemPool {
    private static List<Item> pool = null;

    private static final Set<String> EXCLUDED = Set.of(
            // Creative-only / technical
            "air",
            "light",
            "barrier",
            "structure_void",
            "command_block", "chain_command_block", "repeating_command_block",
            "command_block_minecart",
            "jigsaw",
            "structure_block",
            "debug_stick",
            "knowledge_book",

            // Unobtainable in Survival
            "bedrock",
            "end_portal_frame",
            "spawner",
            "trial_spawner",
            "vault",
            "budding_amethyst",
            "reinforced_deepslate",
            "petrified_oak_slab",
            "player_head",

            // Variant-dependent (identity determined by components, not item ID)
            "potion",
            "splash_potion",
            "lingering_potion",
            "tipped_arrow",
            "enchanted_book",
            "suspicious_stew",
            "written_book",
            "firework_rocket",
            "firework_star",
            "filled_map",

            // Not fully available in 1.21.1
            "bundle"
    );

    private static boolean isExcluded(String path) {
        if (EXCLUDED.contains(path)) return true;
        if (path.endsWith("_spawn_egg")) return true;
        if (path.startsWith("infested_")) return true;
        return false;
    }

    public static List<Item> getPool() {
        if (pool == null) {
            pool = buildPool();
        }
        return pool;
    }

    private static List<Item> buildPool() {
        List<Item> result = new ArrayList<>();

        for (Item item : Registries.ITEM) {
            if (item == Items.AIR) continue;

            Identifier id = Registries.ITEM.getId(item);
            if (!id.getNamespace().equals("minecraft")) continue;

            if (isExcluded(id.getPath())) continue;

            result.add(item);
        }

        return Collections.unmodifiableList(result);
    }

    public static Item getRandomItem(Random random) {
        List<Item> p = getPool();
        return p.get(random.nextInt(p.size()));
    }

    public static void logPool() {
        List<Item> p = getPool();
        for (Item item : p) {
            SpawnHuntMod.LOGGER.debug("  pool: {}", Registries.ITEM.getId(item));
        }
        SpawnHuntMod.LOGGER.info("Item pool initialized — {} survival-obtainable items", p.size());
    }
}
