package com.spawnhunt.mixin;

import com.spawnhunt.data.HuntState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class GameModeLockMixin {

    @Inject(method = "setGameMode", at = @At("HEAD"), cancellable = true)
    private void spawnhunt$blockGameModeChange(GameType gameType, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayer self = (ServerPlayer) (Object) this;

        // Only lock on integrated server (singleplayer / open-to-LAN)
        if (self.level().getServer().isDedicatedServer()) return;

        // Only lock during active, unwon hunts
        if (!HuntState.isActive() || HuntState.isWon()) return;

        // Allow staying in survival
        if (gameType == GameType.SURVIVAL) return;

        // Allow spectator when dead (hardcore death → "Spectate World")
        if (gameType == GameType.SPECTATOR && self.isDeadOrDying()) return;

        // Block all other game mode changes
        self.sendSystemMessage(
                Component.literal("[SpawnHunt] Game mode locked to Survival during active hunt.")
                        .withStyle(ChatFormatting.RED)
        );
        cir.setReturnValue(false);
    }
}
