package com.spawnhunt.hud;

import com.spawnhunt.data.HuntState;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class HuntHudRenderer {

    private static final int PADDING = 4;
    private static final int GAP = 2;
    private static final float TIMER_SCALE = 1.0f;
    private static final int BG_COLOR = 0x80000000;
    private static final int BORDER_COLOR = 0x40FFFFFF;

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!HuntState.isActive()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        TextRenderer textRenderer = client.textRenderer;
        renderHudBlock(context, textRenderer);
    }

    private static void renderHudBlock(DrawContext context, TextRenderer textRenderer) {
        Identifier targetId = HuntState.getTargetBlock();
        if (targetId == null) return;

        Block block = Registries.BLOCK.get(targetId);
        Text blockName = block.getName();
        int nameWidth = textRenderer.getWidth(blockName);

        // Timer text
        long ms = HuntState.isWon() ? HuntState.getFinalTimeMs() : HuntState.getAccumulatedMs();
        String timeStr = HuntState.formatTime(ms);
        int scaledTimerHeight = (int) (textRenderer.fontHeight * TIMER_SCALE);

        // Box sizing: text area starts after icon + gap
        int textStartX = PADDING + 16 + PADDING;
        int boxX = PADDING;
        int boxY = PADDING;
        int boxW = textStartX + nameWidth + PADDING;
        // nameY offset from boxY is: PADDING + (16 - fontH) / 2
        // timer starts at: nameY + fontH + 1, height is scaledTimerHeight
        int nameOffsetY = PADDING + (16 - textRenderer.fontHeight) / 2;
        int boxH = nameOffsetY + textRenderer.fontHeight + 1 + scaledTimerHeight + PADDING;

        // Background + border
        context.fill(boxX, boxY, boxX + boxW, boxY + boxH, BG_COLOR);
        drawBorder(context, boxX, boxY, boxW, boxH);

        // Block icon
        context.drawItem(new ItemStack(block.asItem()), boxX + PADDING, boxY + PADDING);

        // Block name (vertically centered with the 16px icon)
        int nameY = boxY + PADDING + (16 - textRenderer.fontHeight) / 2;
        context.drawText(textRenderer, blockName,
                boxX + PADDING + 16 + PADDING, nameY, 0xFFFFFFFF, true);

        // Timer just below the block name text
        int timerY = nameY + textRenderer.fontHeight + 1;
        context.getMatrices().pushMatrix();
        context.getMatrices().translate((float) (boxX + textStartX), (float) timerY);
        context.getMatrices().scale(TIMER_SCALE, TIMER_SCALE);
        context.drawText(textRenderer, timeStr, 0, 0, 0xFFFFFFFF, true);
        context.getMatrices().popMatrix();
    }

    private static void drawBorder(DrawContext context, int x, int y, int w, int h) {
        context.fill(x, y, x + w, y + 1, BORDER_COLOR);         // top
        context.fill(x, y + h - 1, x + w, y + h, BORDER_COLOR); // bottom
        context.fill(x, y, x + 1, y + h, BORDER_COLOR);         // left
        context.fill(x + w - 1, y, x + w, y + h, BORDER_COLOR); // right
    }
}
