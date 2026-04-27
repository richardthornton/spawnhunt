package com.spawnhunt.mixin;

import com.spawnhunt.SpawnHuntMod;
import com.spawnhunt.data.HuntState;
import com.spawnhunt.data.ItemPool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Difficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {

    @Shadow
    public abstract WorldCreationUiState getUiState();

    @Shadow
    abstract void onCreate();

    @Inject(method = "init", at = @At("TAIL"))
    private void spawnhunt$autoConfigureAndCreate(CallbackInfo ci) {
        if (!HuntState.isActive()) return;

        WorldCreationUiState uiState = this.getUiState();

        Item targetItem = BuiltInRegistries.ITEM.getValue(HuntState.getTargetItem());
        String itemName = ItemPool.getDisplayName(targetItem).getString();
        uiState.setName("SpawnHunt - " + itemName);
        if (HuntState.isHardcore()) {
            uiState.setGameMode(WorldCreationUiState.SelectedGameMode.HARDCORE);
            uiState.setDifficulty(Difficulty.HARD);
        } else {
            uiState.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
            uiState.setDifficulty(Difficulty.NORMAL);
        }
        uiState.setAllowCommands(false);

        SpawnHuntMod.LOGGER.info("SpawnHunt: auto-creating world '{}'", uiState.getName());

        // Schedule onCreate() on the next tick so init() finishes first
        Minecraft.getInstance().execute(this::onCreate);
    }
}
