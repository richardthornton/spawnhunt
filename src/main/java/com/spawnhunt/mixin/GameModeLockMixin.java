package com.spawnhunt.mixin;

import com.spawnhunt.data.HuntState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class GameModeLockMixin {

    @Inject(method = "changeGameMode", at = @At("HEAD"), cancellable = true)
    private void spawnhunt$blockGameModeChange(GameMode gameMode, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;

        // Only lock on integrated server (singleplayer / open-to-LAN)
        if (self.getEntityWorld().getServer().isDedicated()) return;

        // Only lock during active, unwon hunts
        if (!HuntState.isActive() || HuntState.isWon()) return;

        // Allow staying in survival
        if (gameMode == GameMode.SURVIVAL) return;

        // Allow spectator when dead (hardcore death → "Spectate World")
        if (gameMode == GameMode.SPECTATOR && self.isDead()) return;

        // Block all other game mode changes
        self.sendMessage(
                Text.literal("[SpawnHunt] Game mode locked to Survival during active hunt.")
                        .formatted(Formatting.RED),
                false
        );
        cir.setReturnValue(false);
    }
}
