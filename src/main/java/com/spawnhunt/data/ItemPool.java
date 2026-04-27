package com.spawnhunt.data;

import com.spawnhunt.SpawnHuntMod;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import net.minecraft.network.chat.Component;

import java.util.*;

/**
 * Maintains a cached pool of all survival-obtainable items (blocks, tools, armor, food, materials, etc.).
 * <p>
 * Approach: iterate the entire item registry, keep minecraft-namespace items,
 * exclude unobtainables and variant-dependent items via an exclusion set + patterns.
 */
public class ItemPool {
    private static List<Item> pool = null;
    private static final Map<Item, Component> displayNameCache = new HashMap<>();

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
            "test_block",
            "test_instance_block",
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
            "filled_map"
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

        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;

            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (!id.getNamespace().equals("minecraft")) continue;

            if (isExcluded(id.getPath())) continue;

            result.add(item);
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * MC 26.1: item components are data-driven and not bound until world load.
     * This binds a minimal component map (with ITEM_MODEL) so ItemStacks can be
     * created pre-world for the selection screen. Vanilla overwrites with full
     * data-driven components during world load.
     */
    public static void ensureComponentsBound() {
        int bound = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            if (!item.builtInRegistryHolder().areComponentsBound()) {
                Identifier id = BuiltInRegistries.ITEM.getKey(item);
                DataComponentMap components = DataComponentMap.builder()
                        .set(DataComponents.ITEM_MODEL, id)
                        .build();
                item.builtInRegistryHolder().bindComponents(components);
                bound++;
            }
        }
        if (bound > 0) {
            SpawnHuntMod.LOGGER.info("SpawnHunt: pre-bound model components for {} items (pre-world rendering)", bound);
        }
    }

    private static final String MUSIC_DISC_PREFIX = "music_disc_";

    /**
     * Returns a display name for the item. For music discs, returns
     * "Music Disc - {song}" (e.g. "Music Disc - chirp") since getName()
     * alone just returns "Music Disc" for all of them.
     */
    public static Component getDisplayName(Item item) {
        Component cached = displayNameCache.get(item);
        if (cached != null) return cached;

        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        String path = id.getPath();

        Component name;
        if (path.startsWith(MUSIC_DISC_PREFIX)) {
            String songId = path.substring(MUSIC_DISC_PREFIX.length());
            name = Component.literal("Music Disc - " + songId);
        } else {
            // Use the translation key directly — safe pre-world (no ItemStack needed)
            name = Component.translatable(item.getDescriptionId());
        }

        displayNameCache.put(item, name);
        return name;
    }

    public static Item getRandomItem(Random random) {
        List<Item> p = getPool();
        return p.get(random.nextInt(p.size()));
    }

    public static void logPool() {
        List<Item> p = getPool();
        for (Item item : p) {
            SpawnHuntMod.LOGGER.debug("  pool: {}", BuiltInRegistries.ITEM.getKey(item));
        }
        SpawnHuntMod.LOGGER.info("Item pool initialized — {} survival-obtainable items", p.size());
    }
}
