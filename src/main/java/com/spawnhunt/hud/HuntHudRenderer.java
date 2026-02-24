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
    private static final int BG_COLOR = 0x80000000; // semi-transparent black

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!HuntState.isActive()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();

        renderTargetBlock(context, textRenderer);
        renderTimer(context, textRenderer, screenWidth);
    }

    private static void renderTargetBlock(DrawContext context, TextRenderer textRenderer) {
        Identifier targetId = HuntState.getTargetBlock();
        if (targetId == null) return;

        Block block = Registries.BLOCK.get(targetId);
        Text blockName = block.getName();
        int nameWidth = textRenderer.getWidth(blockName);

        // Icon (16x16) + 4px gap + name text
        int totalWidth = 16 + PADDING + nameWidth;
        int boxX = PADDING;
        int boxY = PADDING;
        int boxW = totalWidth + PADDING * 2;
        int boxH = 16 + PADDING * 2;

        // Background
        context.fill(boxX, boxY, boxX + boxW, boxY + boxH, BG_COLOR);

        // Block icon
        context.drawItem(new ItemStack(block.asItem()), boxX + PADDING, boxY + PADDING);

        // Block name (vertically centered with the 16px icon)
        int textY = boxY + PADDING + (16 - textRenderer.fontHeight) / 2;
        context.drawText(textRenderer, blockName,
                boxX + PADDING + 16 + PADDING, textY, 0xFFFFFFFF, true);
    }

    private static void renderTimer(DrawContext context, TextRenderer textRenderer, int screenWidth) {
        long ms = HuntState.isWon() ? HuntState.getFinalTimeMs() : HuntState.getAccumulatedMs();
        String timeStr = HuntState.formatTime(ms);
        int textWidth = textRenderer.getWidth(timeStr);

        int boxW = textWidth + PADDING * 2;
        int boxH = textRenderer.fontHeight + PADDING * 2;
        int boxX = screenWidth - boxW - PADDING;
        int boxY = PADDING;

        // Background
        context.fill(boxX, boxY, boxX + boxW, boxY + boxH, BG_COLOR);

        // Timer text — green if won, white otherwise
        int color = HuntState.isWon() ? 0xFF55FF55 : 0xFFFFFFFF;
        context.drawText(textRenderer, timeStr,
                boxX + PADDING, boxY + PADDING, color, true);
    }
}
