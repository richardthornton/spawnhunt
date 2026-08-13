package com.spawnhunt.mixin;

import com.spawnhunt.data.HuntState;
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
        // The Start button arms a hunt, but the timer only begins on world join.
        // Being back at the title screen with an armed-but-unstarted hunt means
        // world creation never completed — clear it so it doesn't latch onto the
        // next world or server joined.
        if (HuntState.isActive() && !HuntState.hasTimerStarted()) {
            HuntState.reset();
        }

        this.addRenderableWidget(
                Button.builder(Component.literal("SpawnHunt"), button -> {
                    Minecraft.getInstance().setScreen(new SpawnHuntScreen());
                })
                .bounds(this.width / 2 - 100, this.height / 4 + 156, 200, 20)
                .build()
        );
    }
}
