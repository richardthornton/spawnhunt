package com.spawnhunt.mixin;

import com.spawnhunt.data.HuntState;
import com.spawnhunt.screen.SpawnHuntScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    /** Vanilla's gap between menu rows: rows step by 24 and are 20 tall. */
    private static final int ROW_GAP = 4;
    /** LogoRenderer draws at y=30 with a height of 44. */
    private static final int LOGO_BOTTOM = 74;
    /** The copyright line is a 10px band pinned to the bottom; stay clear of it. */
    private static final int COPYRIGHT_CLEARANCE = 12;

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
                    Minecraft.getInstance().gui.setScreen(new SpawnHuntScreen());
                })
                .bounds(this.width / 2 - BUTTON_WIDTH / 2, makeRoomForSpawnHuntRow(), BUTTON_WIDTH, BUTTON_HEIGHT)
                .build()
        );
    }

    /**
     * Returns the y for a SpawnHunt row below vanilla's menu stack, sliding the stack up
     * first if the row wouldn't otherwise fit.
     *
     * The position is derived from the widgets vanilla just laid out rather than a fixed
     * offset — the menu has been reshuffled more than once (26.2 moved the Options/Quit row
     * onto the offset this mixin used to hardcode), and reading the real layout survives the
     * next reshuffle too.
     *
     * At the largest GUI scales the scaled screen bottoms out around 240px tall, and
     * vanilla's own stack fills that to within a few pixels of the copyright line — there is
     * no room for a fourth row. But the stack starts at height/4 + 48 while the logo ends at
     * 74, so on those short screens the slack is above the menu, not below it. Shifting the
     * stack up by just what's missing keeps vanilla's spacing intact and stays clear of the
     * logo. On a normally-sized window nothing needs to move and nothing does.
     */
    private int makeRoomForSpawnHuntRow() {
        int top = Integer.MAX_VALUE;
        int bottom = Integer.MIN_VALUE;
        for (GuiEventListener child : this.children()) {
            // The copyright line is pinned to the bottom of the screen rather than being part
            // of the menu stack, so it neither anchors the new row nor gets shifted.
            if (child instanceof AbstractWidget widget && !(child instanceof PlainTextButton)) {
                top = Math.min(top, widget.getY());
                bottom = Math.max(bottom, widget.getY() + widget.getHeight());
            }
        }
        if (bottom == Integer.MIN_VALUE) {
            return this.height / 4 + 168;
        }

        int missing = (bottom + ROW_GAP + BUTTON_HEIGHT) - (this.height - COPYRIGHT_CLEARANCE);
        int headroom = top - LOGO_BOTTOM - ROW_GAP;
        int shift = Math.max(0, Math.min(missing, headroom));
        if (shift > 0) {
            for (GuiEventListener child : this.children()) {
                if (child instanceof AbstractWidget widget && !(child instanceof PlainTextButton)) {
                    widget.setY(widget.getY() - shift);
                }
            }
            bottom -= shift;
        }
        return bottom + ROW_GAP;
    }
}
