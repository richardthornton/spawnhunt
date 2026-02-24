package com.spawnhunt.screen;

import com.spawnhunt.data.HuntState;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class VictoryOverlay {

    private static final int BG_COLOR = 0xB0000000;

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!HuntState.isActive() || !HuntState.isWon()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int centerX = screenWidth / 2;

        int fontH = textRenderer.fontHeight;
        int padY = 6;
        int lineGap = 4;

        // Compute content height: 3 lines of text + gaps + equal padding top/bottom
        int contentHeight = fontH * 3 + lineGap * 2;
        int bannerTop = 20;
        int bannerBottom = bannerTop + padY + contentHeight + padY;
        int bannerPadX = 60;

        // Full banner background
        context.fill(centerX - bannerPadX, bannerTop,
                centerX + bannerPadX, bannerBottom, BG_COLOR);
        // Banner border
        context.fill(centerX - bannerPadX, bannerTop,
                centerX + bannerPadX, bannerTop + 1, 0x6055FF55);
        context.fill(centerX - bannerPadX, bannerBottom - 1,
                centerX + bannerPadX, bannerBottom, 0x6055FF55);

        int textY = bannerTop + padY;

        // "BLOCK FOUND!" title
        Text title = Text.literal("BLOCK FOUND!");
        int titleWidth = textRenderer.getWidth(title);
        context.drawText(textRenderer, title,
                centerX - titleWidth / 2, textY, 0xFF55FF55, true);
        textY += fontH + lineGap;

        // Target block name
        Identifier targetId = HuntState.getTargetBlock();
        if (targetId != null) {
            Block block = Registries.BLOCK.get(targetId);
            Text blockName = block.getName();
            int nameWidth = textRenderer.getWidth(blockName);
            context.drawText(textRenderer, blockName,
                    centerX - nameWidth / 2, textY, 0xFFFFFF00, true);
        }
        textY += fontH + lineGap;

        // Final time
        String timeStr = HuntState.formatTime(HuntState.getFinalTimeMs());
        Text timeText = Text.literal(timeStr);
        int timeWidth = textRenderer.getWidth(timeText);
        context.drawText(textRenderer, timeText,
                centerX - timeWidth / 2, textY, 0xFFFFFFFF, true);
    }
}
