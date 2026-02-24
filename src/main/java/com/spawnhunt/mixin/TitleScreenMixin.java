package com.spawnhunt.mixin;

import com.spawnhunt.screen.SpawnHuntScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addSpawnHuntButton(CallbackInfo ci) {
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("SpawnHunt"), button -> {
                    MinecraftClient.getInstance().setScreen(new SpawnHuntScreen());
                })
                .dimensions(this.width / 2 - 100, this.height / 4 + 156, 200, 20)
                .build()
        );
    }
}
