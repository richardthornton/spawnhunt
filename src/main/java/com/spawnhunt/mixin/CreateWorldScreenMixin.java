package com.spawnhunt.mixin;

import com.spawnhunt.SpawnHuntMod;
import com.spawnhunt.data.HuntState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.WorldCreator;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.world.Difficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {

    @Shadow
    public abstract WorldCreator getWorldCreator();

    @Shadow
    abstract void createLevel();

    @Inject(method = "init", at = @At("TAIL"))
    private void spawnhunt$autoConfigureAndCreate(CallbackInfo ci) {
        if (!HuntState.isActive()) return;

        WorldCreator creator = this.getWorldCreator();

        Item targetItem = Registries.ITEM.get(HuntState.getTargetItem());
        String itemName = new ItemStack(targetItem).getName().getString();
        creator.setWorldName("SpawnHunt - " + itemName);
        if (HuntState.isHardcore()) {
            creator.setGameMode(WorldCreator.Mode.HARDCORE);
            creator.setDifficulty(Difficulty.HARD);
        } else {
            creator.setGameMode(WorldCreator.Mode.SURVIVAL);
            creator.setDifficulty(Difficulty.NORMAL);
        }
        creator.setCheatsEnabled(false);

        SpawnHuntMod.LOGGER.info("SpawnHunt: auto-creating world '{}'", creator.getWorldName());

        // Schedule createLevel() on the next tick so init() finishes first
        MinecraftClient.getInstance().send(this::createLevel);
    }
}
