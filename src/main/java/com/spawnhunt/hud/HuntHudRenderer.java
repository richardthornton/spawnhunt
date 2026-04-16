package com.spawnhunt.hud;

import com.spawnhunt.data.HuntState;
import com.spawnhunt.data.ResultStore;
import com.spawnhunt.network.ClientHuntState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import com.spawnhunt.data.ItemPool;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class HuntHudRenderer {

    private static final int TOP_MARGIN = 22;
    private static final int LINE_GAP = 2;
    private static final float WIN_TIMER_SCALE = 1.5f;
    private static final float BEST_SCALE = 0.75f;
    private static final int BEST_COLOR = 0xFFAAFFAA;
    private static final int WIN_NAME_COLOR = 0xFF55FF55;

    public static void extractRenderState(GuiGraphicsExtractor context, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        // Multiplayer (server-authoritative) takes priority
        if (ClientHuntState.isActive()) {
            renderHud(context, client.font,
                    ClientHuntState.getTargetItem(),
                    ClientHuntState.isWon(),
                    ClientHuntState.isWon() ? ClientHuntState.getFinalTimeMs() : ClientHuntState.getElapsedMs(),
                    ClientHuntState.isWon() ? ClientHuntState.getWinnerName() : null,
                    false);
            return;
        }

        // Singleplayer fallback
        if (HuntState.isActive()) {
            renderHud(context, client.font,
                    HuntState.getTargetItem(),
                    HuntState.isWon(),
                    HuntState.isWon() ? HuntState.getFinalTimeMs() : HuntState.getAccumulatedMs(),
                    null,
                    true);
        }
    }

    private static void renderHud(GuiGraphicsExtractor context, Font font,
                                   Identifier targetId, boolean won, long timeMs,
                                   String winnerName, boolean singleplayer) {
        if (targetId == null) return;

        Item item = BuiltInRegistries.ITEM.getValue(targetId);
        ItemStack stack = new ItemStack(item);
        Component itemName = ItemPool.getDisplayName(item);

        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int centreX = screenWidth / 2;
        int y = TOP_MARGIN;

        // Item icon — centred at top
        context.item(stack, centreX - 8, y);
        y += 16 + LINE_GAP;

        // Item name — centred below icon
        int nameWidth = font.width(itemName);
        context.text(font, itemName,
                centreX - nameWidth / 2, y, won ? WIN_NAME_COLOR : 0xFFFFFFFF, true);
        y += font.lineHeight + LINE_GAP;

        // Timer — centred below name, larger on win
        String timeStr = singleplayer ? HuntState.formatTime(timeMs) : HuntState.formatTimeSeconds(timeMs);
        if (won) {
            float timerWidthF = font.width(timeStr) * WIN_TIMER_SCALE;
            context.pose().pushMatrix();
            context.pose().translate(centreX - timerWidthF / 2f, (float) y);
            context.pose().scale(WIN_TIMER_SCALE, WIN_TIMER_SCALE);
            context.text(font, timeStr, 0, 0, 0xFFFFFFFF, true);
            context.pose().popMatrix();
            y += (int) (font.lineHeight * WIN_TIMER_SCALE) + LINE_GAP;
        } else {
            int timerWidth = font.width(timeStr);
            context.text(font, timeStr, centreX - timerWidth / 2, y, 0xFFFFFFFF, true);
            y += font.lineHeight + LINE_GAP;
        }

        // Winner name (multiplayer only)
        if (won && winnerName != null && !winnerName.isEmpty()) {
            String winText = winnerName + " wins!";
            int winWidth = font.width(winText);
            context.text(font, winText,
                    centreX - winWidth / 2, y, WIN_NAME_COLOR, true);
            y += font.lineHeight + LINE_GAP;
        }

        // Best time (singleplayer only)
        if (singleplayer) {
            long bestTimeMs = ResultStore.getBestTime(targetId);
            if (bestTimeMs >= 0) {
                String bestStr = "Best: " + HuntState.formatTime(bestTimeMs);
                float bestWidthF = font.width(bestStr) * BEST_SCALE;
                context.pose().pushMatrix();
                context.pose().translate(centreX - bestWidthF / 2f, (float) y);
                context.pose().scale(BEST_SCALE, BEST_SCALE);
                context.text(font, bestStr, 0, 0, BEST_COLOR, true);
                context.pose().popMatrix();
            }
        }
    }
}
