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
        int bannerY = 30;

        // "BLOCK FOUND!" title
        Text title = Text.literal("BLOCK FOUND!");
        int titleWidth = textRenderer.getWidth(title);
        context.drawText(textRenderer, title,
                centerX - titleWidth / 2, bannerY, 0xFF55FF55, true);

        // Target block name
        Identifier targetId = HuntState.getTargetBlock();
        if (targetId != null) {
            Block block = Registries.BLOCK.get(targetId);
            Text blockName = block.getName();
            int nameWidth = textRenderer.getWidth(blockName);
            context.drawText(textRenderer, blockName,
                    centerX - nameWidth / 2, bannerY + 14, 0xFFFFFF00, true);
        }

        // Final time (large, centered)
        String timeStr = HuntState.formatTime(HuntState.getFinalTimeMs());
        Text timeText = Text.literal(timeStr);
        int timeWidth = textRenderer.getWidth(timeText);

        // Background box behind the time
        int boxPadX = 10;
        int boxPadY = 4;
        int timeY = bannerY + 32;
        context.fill(centerX - timeWidth / 2 - boxPadX, timeY - boxPadY,
                centerX + timeWidth / 2 + boxPadX, timeY + textRenderer.fontHeight + boxPadY,
                BG_COLOR);

        context.drawText(textRenderer, timeText,
                centerX - timeWidth / 2, timeY, 0xFFFFFFFF, true);
    }
}
