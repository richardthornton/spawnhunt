package com.spawnhunt.mixin;

import com.spawnhunt.screen.SpawnHuntScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addSpawnHuntButton(CallbackInfo ci) {
        this.addRenderableWidget(
                Button.builder(Component.literal("SpawnHunt"), button -> {
                    Minecraft.getInstance().setScreen(new SpawnHuntScreen());
                })
                .bounds(this.width / 2 - 100, this.height / 4 + 156, 200, 20)
                .build()
        );
    }
}
