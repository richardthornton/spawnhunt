package com.spawnhunt.event;

import com.spawnhunt.SpawnHuntMod;
import com.spawnhunt.data.HuntState;
import com.spawnhunt.data.ResultStore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

public class InventoryListener {

    public static void tick(MinecraftClient client) {
        if (!HuntState.isActive() || HuntState.isWon()) return;

        ClientPlayerEntity player = client.player;
        if (player == null) return;

        Identifier targetId = HuntState.getTargetItem();
        if (targetId == null) return;

        Item targetItem = Registries.ITEM.get(targetId);

        // Scan main inventory (0-35), armor (36-39), and offhand (40)
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == targetItem) {
                HuntState.win();
                ResultStore.recordResult(targetId, HuntState.getFinalTimeMs());
                player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                SpawnHuntMod.LOGGER.info("SpawnHunt: WIN! Found {} in {}",
                        targetId, HuntState.formatTime(HuntState.getFinalTimeMs()));
                return;
            }
        }
    }
}
