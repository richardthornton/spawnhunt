package com.spawnhunt.hud;

import com.spawnhunt.data.HuntState;
import com.spawnhunt.data.ResultStore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import com.spawnhunt.data.ItemPool;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class HuntHudRenderer {

    private static final int TOP_MARGIN = 22;
    private static final int LINE_GAP = 2;
    private static final float TIMER_SCALE = 1.0f;
    private static final float WIN_TIMER_SCALE = 1.5f;
    private static final float BEST_SCALE = 0.75f;
    private static final int BEST_COLOR = 0xFFAAFFAA;
    private static final int WIN_NAME_COLOR = 0xFF55FF55;

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!HuntState.isActive()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        TextRenderer textRenderer = client.textRenderer;
        renderHudBlock(context, textRenderer);
    }

    private static void renderHudBlock(DrawContext context, TextRenderer textRenderer) {
        Identifier targetId = HuntState.getTargetItem();
        if (targetId == null) return;

        Item item = Registries.ITEM.get(targetId);
        ItemStack stack = new ItemStack(item);
        Text itemName = ItemPool.getDisplayName(item);
        boolean won = HuntState.isWon();

        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int centreX = screenWidth / 2;
        int y = TOP_MARGIN;

        // Item icon — centred at top, pinned to screen edge
        context.drawItem(stack, centreX - 8, y);
        y += 16 + LINE_GAP;

        // Item name — centred below icon
        int nameWidth = textRenderer.getWidth(itemName);
        context.drawText(textRenderer, itemName,
                centreX - nameWidth / 2, y, won ? WIN_NAME_COLOR : 0xFFFFFFFF, true);
        y += textRenderer.fontHeight + LINE_GAP;

        // Timer — centred below name, larger on win
        long ms = won ? HuntState.getFinalTimeMs() : HuntState.getAccumulatedMs();
        String timeStr = HuntState.formatTime(ms);
        float timerScale = won ? WIN_TIMER_SCALE : TIMER_SCALE;
        int timerWidth = (int) (textRenderer.getWidth(timeStr) * timerScale);
        context.getMatrices().pushMatrix();
        context.getMatrices().translate((float) (centreX - timerWidth / 2), (float) y);
        context.getMatrices().scale(timerScale, timerScale);
        context.drawText(textRenderer, timeStr, 0, 0, 0xFFFFFFFF, true);
        context.getMatrices().popMatrix();
        y += (int) (textRenderer.fontHeight * timerScale) + LINE_GAP;

        // Best time — centred below timer
        long bestTimeMs = ResultStore.getBestTime(targetId);
        if (bestTimeMs >= 0) {
            String bestStr = "Best: " + HuntState.formatTime(bestTimeMs);
            int bestWidth = (int) (textRenderer.getWidth(bestStr) * BEST_SCALE);
            context.getMatrices().pushMatrix();
            context.getMatrices().translate((float) (centreX - bestWidth / 2), (float) y);
            context.getMatrices().scale(BEST_SCALE, BEST_SCALE);
            context.drawText(textRenderer, bestStr, 0, 0, BEST_COLOR, true);
            context.getMatrices().popMatrix();
        }
    }
}
