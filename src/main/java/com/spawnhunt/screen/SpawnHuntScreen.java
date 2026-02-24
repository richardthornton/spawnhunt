package com.spawnhunt.screen;

import com.spawnhunt.data.HuntState;
import com.spawnhunt.data.ItemPool;
import com.spawnhunt.data.ResultStore;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Random;

/**
 * Item selection screen — shown when the player clicks "SpawnHunt" on the title screen.
 * Displays a random survival-obtainable item at 4x scale with Reroll / Cancel / Start buttons.
 */
public class SpawnHuntScreen extends Screen {
    private static final int ICON_SCALE = 4;
    private static final int ICON_SIZE = 16 * ICON_SCALE; // 64px
    private static final int PANEL_WIDTH = 120;
    private static final int PANEL_PADDING = 6;
    private static final int PANEL_BG = 0x80000000;
    private static final int PANEL_BORDER = 0x40FFFFFF;

    private final Random random = new Random();
    private Item targetItem;

    public SpawnHuntScreen() {
        super(Text.literal("SpawnHunt"));
        this.targetItem = ItemPool.getRandomItem(random);
    }

    @Override
    protected void init() {
        int buttonY = this.height / 2 + 50;

        // Reroll button — pick a new random item
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Reroll"), button -> {
                    this.targetItem = ItemPool.getRandomItem(random);
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

        // Start button — sets hunt target, opens world creation
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Start"), button -> {
                    HuntState.startHunt(Registries.ITEM.getId(targetItem));
                    CreateWorldScreen.show(this.client, () -> {
                        this.client.setScreen(new TitleScreen());
                    });
                })
                .dimensions(this.width / 2 + 58, buttonY, 96, 20)
                .build()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dark background
        context.fill(0, 0, this.width, this.height, 0xC0000000);

        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;

        // Bobbing animation for the item icon
        float bob = (float) Math.sin(System.currentTimeMillis() / 500.0) * 3.0f;
        int iconY = (int) (this.height / 2 - ICON_SIZE + 10 + bob);
        int nameY = this.height / 2 + 10 + 8 + 4; // fixed position, below icon range

        // Title
        Text title = Text.literal("SpawnHunt");
        context.drawText(this.textRenderer, title,
                centerX - this.textRenderer.getWidth(title) / 2, 20, 0xFFFFFFFF, true);

        // Subtitle
        Text subtitle = Text.literal("Your target:");
        context.drawText(this.textRenderer, subtitle,
                centerX - this.textRenderer.getWidth(subtitle) / 2, 34, 0xFFAAAAAA, true);

        // Item icon at 4x scale, centered, with bobbing
        context.getMatrices().pushMatrix();
        context.getMatrices().translate((float) (centerX - ICON_SIZE / 2), (float) iconY);
        context.getMatrices().scale((float) ICON_SCALE, (float) ICON_SCALE);
        context.drawItem(new ItemStack(targetItem), 0, 0);
        context.getMatrices().popMatrix();

        // Translated item name below the icon (fixed position)
        Text itemName = new ItemStack(targetItem).getName();
        context.drawText(this.textRenderer, itemName,
                centerX - this.textRenderer.getWidth(itemName) / 2, nameY, 0xFFFFFF00, true);

        // Run history panels (skip if window too narrow)
        Identifier itemId = Registries.ITEM.getId(targetItem);
        int panelGap = 16;
        int neededWidth = ICON_SIZE + 2 * (PANEL_WIDTH + panelGap) + 40;
        if (this.width >= neededWidth) {
            int panelY = this.height / 2 - ICON_SIZE + 10;
            int leftPanelX = centerX - ICON_SIZE / 2 - panelGap - PANEL_WIDTH;
            int rightPanelX = centerX + ICON_SIZE / 2 + panelGap;

            List<ResultStore.RunResult> lastRuns = ResultStore.getLastRuns(itemId, 3);
            List<ResultStore.RunResult> topRuns = ResultStore.getTopRuns(itemId, 3);

            renderRunPanel(context, leftPanelX, panelY, "Last Three Runs", lastRuns);
            renderRunPanel(context, rightPanelX, panelY, "Top Three Runs", topRuns);
        }
    }

    private void renderRunPanel(DrawContext context, int x, int y, String header,
                                List<ResultStore.RunResult> runs) {
        int lineHeight = this.textRenderer.fontHeight + 2;
        int panelH = PANEL_PADDING + lineHeight + 2 + Math.max(runs.size(), 1) * lineHeight + PANEL_PADDING;

        // Background + border
        context.fill(x, y, x + PANEL_WIDTH, y + panelH, PANEL_BG);
        drawPanelBorder(context, x, y, PANEL_WIDTH, panelH);

        // Header
        context.drawText(this.textRenderer, header,
                x + PANEL_PADDING, y + PANEL_PADDING, 0xFFFFFFFF, true);

        int entryY = y + PANEL_PADDING + lineHeight + 2;

        if (runs.isEmpty()) {
            context.drawText(this.textRenderer, "No runs yet",
                    x + PANEL_PADDING, entryY, 0xFF888888, true);
        } else {
            for (int i = 0; i < runs.size(); i++) {
                String entry = (i + 1) + ". " + HuntState.formatTime(runs.get(i).timeMs);
                context.drawText(this.textRenderer, entry,
                        x + PANEL_PADDING, entryY + i * lineHeight, 0xFFDDDDDD, true);
            }
        }
    }

    private static void drawPanelBorder(DrawContext context, int x, int y, int w, int h) {
        context.fill(x, y, x + w, y + 1, PANEL_BORDER);         // top
        context.fill(x, y + h - 1, x + w, y + h, PANEL_BORDER); // bottom
        context.fill(x, y, x + 1, y + h, PANEL_BORDER);         // left
        context.fill(x + w - 1, y, x + w, y + h, PANEL_BORDER); // right
    }
}
