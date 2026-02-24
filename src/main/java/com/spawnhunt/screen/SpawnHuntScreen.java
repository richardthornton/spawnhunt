package com.spawnhunt.screen;

import com.spawnhunt.data.BlockPool;
import net.minecraft.block.Block;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.Random;

/**
 * Block selection screen — shown when the player clicks "SpawnHunt" on the title screen.
 * Displays a random survival-obtainable block with Reroll / Cancel / Start buttons.
 * Full implementation in Phase 4.
 */
public class SpawnHuntScreen extends Screen {
    private final Random random = new Random();
    private Block targetBlock;

    public SpawnHuntScreen() {
        super(Text.literal("SpawnHunt"));
        this.targetBlock = BlockPool.getRandomBlock(random);
    }

    @Override
    protected void init() {
        // Reroll button — pick a new random block
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Reroll"), button -> {
                    this.targetBlock = BlockPool.getRandomBlock(random);
                })
                .dimensions(this.width / 2 - 154, this.height / 2 + 40, 96, 20)
                .build()
        );

        // Cancel button — return to title screen
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Cancel"), button -> {
                    this.client.setScreen(new TitleScreen());
                })
                .dimensions(this.width / 2 - 48, this.height / 2 + 40, 96, 20)
                .build()
        );

        // Start button — placeholder for Phase 5 (world creation)
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Start"), button -> {
                    // TODO Phase 5: trigger world creation with this.targetBlock
                })
                .dimensions(this.width / 2 + 58, this.height / 2 + 40, 96, 20)
                .build()
        );
    }

    @Override
    public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // Draw title
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("SpawnHunt"), this.width / 2, 20, 0xFFFFFF);

        // Draw target block name
        String blockName = Registries.BLOCK.getId(targetBlock).getPath().replace('_', ' ');
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Target: " + blockName), this.width / 2, this.height / 2, 0xFFFF00);
    }
}
