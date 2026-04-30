package com.spawnhunt.event;

import com.spawnhunt.SpawnHuntMod;
import com.spawnhunt.data.HuntState;
import com.spawnhunt.data.ResultStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.resources.Identifier;

public class InventoryListener {

    public static void tick(Minecraft client) {
        if (!HuntState.isActive() || HuntState.isWon()) return;

        LocalPlayer player = client.player;
        if (player == null) return;

        Identifier targetId = HuntState.getTargetItem();
        if (targetId == null) return;

        Item targetItem = BuiltInRegistries.ITEM.getValue(targetId);

        // Scan main inventory (0-35), armor (36-39), and offhand (40)
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
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
