package com.spawnhunt.data;

import com.spawnhunt.SpawnHuntMod;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.*;

/**
 * Maintains a cached pool of all survival-obtainable blocks.
 * <p>
 * Approach: start with every block in the registry, keep only those with an
 * item form, deduplicate blocks that share the same item (e.g. wall_torch vs
 * torch), then apply an explicit exclusion set for blocks that have items but
 * are not obtainable in pure Survival.
 */
public class BlockPool {
    private static List<Block> pool = null;

    // Blocks that have item forms but cannot be obtained in Survival without commands.
    private static final Set<String> EXCLUDED_BLOCKS = Set.of(
            // Technical / operator-only blocks
            "air", "cave_air", "void_air",
            "light",
            "barrier",
            "structure_void",
            "command_block", "chain_command_block", "repeating_command_block",
            "jigsaw",
            "structure_block",

            // Unobtainable in Survival
            "bedrock",
            "end_portal_frame",
            "spawner",
            "trial_spawner",
            "vault",
            "budding_amethyst",
            "reinforced_deepslate",
            "petrified_oak_slab",
            "player_head", "player_wall_head",

            // Infested blocks — silk touch yields the normal variant, not the infested one
            "infested_stone",
            "infested_cobblestone",
            "infested_stone_bricks",
            "infested_mossy_stone_bricks",
            "infested_cracked_stone_bricks",
            "infested_chiseled_stone_bricks",
            "infested_deepslate"
    );

    public static List<Block> getPool() {
        if (pool == null) {
            pool = buildPool();
        }
        return pool;
    }

    private static List<Block> buildPool() {
        // Step 1: Collect all blocks that have an item form, deduplicating by item.
        // When multiple blocks share the same item (e.g. torch / wall_torch),
        // prefer the block whose registry path matches the item's registry path.
        Map<Item, Block> itemToBlock = new LinkedHashMap<>();

        for (Block block : Registries.BLOCK) {
            Item item = block.asItem();
            if (item == Items.AIR) continue;

            Identifier blockId = Registries.BLOCK.getId(block);
            if (!blockId.getNamespace().equals("minecraft")) continue;
            Identifier itemId = Registries.ITEM.getId(item);

            if (!itemToBlock.containsKey(item)) {
                itemToBlock.put(item, block);
            } else if (blockId.getPath().equals(itemId.getPath())) {
                // This block's name matches the item name — it's the "primary" variant
                itemToBlock.put(item, block);
            }
        }

        // Step 2: Apply exclusion list
        List<Block> result = new ArrayList<>();
        for (Block block : itemToBlock.values()) {
            String path = Registries.BLOCK.getId(block).getPath();
            if (!EXCLUDED_BLOCKS.contains(path)) {
                result.add(block);
            }
        }

        return Collections.unmodifiableList(result);
    }

    public static Block getRandomBlock(Random random) {
        List<Block> p = getPool();
        return p.get(random.nextInt(p.size()));
    }

    /**
     * Logs every block in the pool at DEBUG level, plus the total count at INFO.
     * Used during development to manually verify the pool contents.
     */
    public static void logPool() {
        List<Block> p = getPool();
        for (Block block : p) {
            SpawnHuntMod.LOGGER.debug("  pool: {}", Registries.BLOCK.getId(block));
        }
        SpawnHuntMod.LOGGER.info("Block pool initialized — {} survival-obtainable blocks", p.size());
    }
}
