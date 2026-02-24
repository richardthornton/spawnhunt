package com.spawnhunt.screen;

import com.spawnhunt.data.BlockPool;
import com.spawnhunt.data.HuntState;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.Random;

/**
 * Block selection screen — shown when the player clicks "SpawnHunt" on the title screen.
 * Displays a random survival-obtainable block at 4x scale with Reroll / Cancel / Start buttons.
 */
public class SpawnHuntScreen extends Screen {
    private static final int ICON_SCALE = 4;
    private static final int ICON_SIZE = 16 * ICON_SCALE; // 64px

    private final Random random = new Random();
    private Block targetBlock;

    public SpawnHuntScreen() {
        super(Text.literal("SpawnHunt"));
        this.targetBlock = BlockPool.getRandomBlock(random);
    }

    @Override
    protected void init() {
        int buttonY = this.height / 2 + 50;

        // Reroll button — pick a new random block
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Reroll"), button -> {
                    this.targetBlock = BlockPool.getRandomBlock(random);
                })
                .dimensions(this.width / 2 - 154, buttonY, 96, 20)
                .build()
        );

        // Cancel button — return to title screen
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Cancel"), button -> {
                    this.client.setScreen(new TitleScreen());
                })
                .dimensions(this.width / 2 - 48, buttonY, 96, 20)
                .build()
        );

        // Start button — sets hunt target, world creation in Phase 5
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Start"), button -> {
                    HuntState.startHunt(Registries.BLOCK.getId(targetBlock));
                    // TODO Phase 5: trigger world creation
                })
                .dimensions(this.width / 2 + 58, buttonY, 96, 20)
                .build()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // Title
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("SpawnHunt"),
                this.width / 2, 20, 0xFFFFFF);

        // Block icon at 4x scale, centered
        int iconX = this.width / 2 - ICON_SIZE / 2;
        int iconY = this.height / 2 - ICON_SIZE + 10;

        context.getMatrices().pushMatrix();
        context.getMatrices().translate((float) iconX, (float) iconY);
        context.getMatrices().scale((float) ICON_SCALE, (float) ICON_SCALE);
        context.drawItem(new ItemStack(targetBlock.asItem()), 0, 0);
        context.getMatrices().popMatrix();

        // Translated block name below the icon
        Text blockName = targetBlock.getName();
        context.drawCenteredTextWithShadow(this.textRenderer, blockName,
                this.width / 2, iconY + ICON_SIZE + 8, 0xFFFF00);
    }
}
