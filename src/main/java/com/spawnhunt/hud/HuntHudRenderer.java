package com.spawnhunt.hud;

import com.spawnhunt.data.HuntState;
import com.spawnhunt.data.ResultStore;
import com.spawnhunt.network.ClientHuntState;
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
    private static final float WIN_TIMER_SCALE = 1.5f;
    private static final float BEST_SCALE = 0.75f;
    private static final int BEST_COLOR = 0xFFAAFFAA;
    private static final int WIN_NAME_COLOR = 0xFF55FF55;

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // Multiplayer (server-authoritative) takes priority
        if (ClientHuntState.isActive()) {
            renderHud(context, client.textRenderer,
                    ClientHuntState.getTargetItem(),
                    ClientHuntState.isWon(),
                    ClientHuntState.isWon() ? ClientHuntState.getFinalTimeMs() : ClientHuntState.getElapsedMs(),
                    ClientHuntState.isWon() ? ClientHuntState.getWinnerName() : null,
                    false);
            return;
        }

        // Singleplayer fallback
        if (HuntState.isActive()) {
            renderHud(context, client.textRenderer,
                    HuntState.getTargetItem(),
                    HuntState.isWon(),
                    HuntState.isWon() ? HuntState.getFinalTimeMs() : HuntState.getAccumulatedMs(),
                    null,
                    true);
        }
    }

    private static void renderHud(DrawContext context, TextRenderer textRenderer,
                                   Identifier targetId, boolean won, long timeMs,
                                   String winnerName, boolean singleplayer) {
        if (targetId == null) return;

        Item item = Registries.ITEM.get(targetId);
        ItemStack stack = new ItemStack(item);
        Text itemName = ItemPool.getDisplayName(item);

        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int centreX = screenWidth / 2;
        int y = TOP_MARGIN;

        // Item icon — centred at top
        context.drawItem(stack, centreX - 8, y);
        y += 16 + LINE_GAP;

        // Item name — centred below icon
        int nameWidth = textRenderer.getWidth(itemName);
        context.drawText(textRenderer, itemName,
                centreX - nameWidth / 2, y, won ? WIN_NAME_COLOR : 0xFFFFFFFF, true);
        y += textRenderer.fontHeight + LINE_GAP;

        // Timer — centred below name, larger on win
        String timeStr = singleplayer ? HuntState.formatTime(timeMs) : HuntState.formatTimeSeconds(timeMs);
        if (won) {
            float timerWidthF = textRenderer.getWidth(timeStr) * WIN_TIMER_SCALE;
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(centreX - timerWidthF / 2f, (float) y);
            context.getMatrices().scale(WIN_TIMER_SCALE, WIN_TIMER_SCALE);
            context.drawText(textRenderer, timeStr, 0, 0, 0xFFFFFFFF, true);
            context.getMatrices().popMatrix();
            y += (int) (textRenderer.fontHeight * WIN_TIMER_SCALE) + LINE_GAP;
        } else {
            int timerWidth = textRenderer.getWidth(timeStr);
            context.drawText(textRenderer, timeStr, centreX - timerWidth / 2, y, 0xFFFFFFFF, true);
            y += textRenderer.fontHeight + LINE_GAP;
        }

        // Winner name (multiplayer only)
        if (won && winnerName != null && !winnerName.isEmpty()) {
            String winText = winnerName + " wins!";
            int winWidth = textRenderer.getWidth(winText);
            context.drawText(textRenderer, winText,
                    centreX - winWidth / 2, y, WIN_NAME_COLOR, true);
            y += textRenderer.fontHeight + LINE_GAP;
        }

        // Best time (singleplayer only)
        if (singleplayer) {
            long bestTimeMs = ResultStore.getBestTime(targetId);
            if (bestTimeMs >= 0) {
                String bestStr = "Best: " + HuntState.formatTime(bestTimeMs);
                float bestWidthF = textRenderer.getWidth(bestStr) * BEST_SCALE;
                context.getMatrices().pushMatrix();
                context.getMatrices().translate(centreX - bestWidthF / 2f, (float) y);
                context.getMatrices().scale(BEST_SCALE, BEST_SCALE);
                context.drawText(textRenderer, bestStr, 0, 0, BEST_COLOR, true);
                context.getMatrices().popMatrix();
            }
        }
    }
}
