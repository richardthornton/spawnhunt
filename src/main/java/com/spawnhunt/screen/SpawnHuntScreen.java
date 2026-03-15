package com.spawnhunt.screen;

import com.spawnhunt.data.HuntState;
import com.spawnhunt.data.ItemPool;
import com.spawnhunt.data.ResultStore;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Random;

/**
 * Item selection screen — shown when the player clicks "SpawnHunt" on the title screen.
 * Vertical layout optimised for YouTube Shorts capture.
 */
public class SpawnHuntScreen extends Screen {
    private static final int ICON_SCALE = 4;
    private static final int ICON_SIZE = 16 * ICON_SCALE; // 64px
    private static final int PANEL_PADDING = 6;
    private static final int PANEL_BG = 0x80000000;
    private static final int PANEL_BORDER = 0x60444444;
    private static final int BTN_W = 76;
    private static final int BTN_H = 20;
    private static final int BTN_GAP = 4;

    private final Random random = new Random();
    private Item targetItem;
    private boolean hardcore;
    private boolean showHistory;

    // Clickable "History" / "Back" link bounds (set during render)
    private int historyLinkX, historyLinkY, historyLinkW, historyLinkH;

    public SpawnHuntScreen() {
        super(Text.literal("SpawnHunt"));
        this.targetItem = ItemPool.getRandomItem(random);
        this.hardcore = true;
    }

    public SpawnHuntScreen(Item preselectedItem) {
        this(preselectedItem, true);
    }

    public SpawnHuntScreen(Item preselectedItem, boolean hardcore) {
        super(Text.literal("SpawnHunt"));
        this.targetItem = preselectedItem;
        this.hardcore = hardcore;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        // Vertical layout anchor: compute total content height and center it
        // Title(10) + 4 + Subtitle(10) + 8 + Icon(64) + 4 + Name(10) + 4 + HistoryLink(10)
        //   + 10 + ButtonRow1(20) + 4 + ButtonRow2(20) + 8 + Checkbox(20) = ~196
        int totalHeight = 196;
        int topY = (this.height - totalHeight) / 2;

        // Button positions: 2x2 grid centered
        int btnBlockY = topY + 10 + 4 + 10 + 8 + ICON_SIZE + 4 + 10 + 4 + 10 + 10;
        int leftBtnX = centerX - BTN_W - BTN_GAP / 2;
        int rightBtnX = centerX + BTN_GAP / 2;

        // Row 1: Start, Reroll
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Start"), button -> {
                    HuntState.startHunt(Registries.ITEM.getId(targetItem), this.hardcore);
                    CreateWorldScreen.show(this.client, () -> {
                        this.client.setScreen(new TitleScreen());
                    });
                })
                .dimensions(leftBtnX, btnBlockY, BTN_W, BTN_H)
                .build()
        );

        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Reroll"), button -> {
                    this.targetItem = ItemPool.getRandomItem(random);
                    this.showHistory = false;
                })
                .dimensions(rightBtnX, btnBlockY, BTN_W, BTN_H)
                .build()
        );

        // Row 2: List, Cancel
        int row2Y = btnBlockY + BTN_H + BTN_GAP;
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("List"), button -> {
                    this.client.setScreen(new ItemChooserScreen(this.targetItem, this.hardcore));
                })
                .dimensions(leftBtnX, row2Y, BTN_W, BTN_H)
                .build()
        );

        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Cancel"), button -> {
                    this.client.setScreen(new TitleScreen());
                })
                .dimensions(rightBtnX, row2Y, BTN_W, BTN_H)
                .build()
        );

        // Hardcore checkbox — centered below buttons
        int checkboxY = row2Y + BTN_H + 8;
        this.addDrawableChild(
                CheckboxWidget.builder(Text.literal("Hardcore"), this.textRenderer)
                        .pos(centerX - 30, checkboxY)
                        .checked(this.hardcore)
                        .callback((checkbox, checked) -> this.hardcore = checked)
                        .build()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dark background
        context.fill(0, 0, this.width, this.height, 0xC0000000);

        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int totalHeight = 196;
        int topY = (this.height - totalHeight) / 2;
        int curY = topY;

        // Title
        Text title = Text.literal("SpawnHunt");
        context.drawText(this.textRenderer, title,
                centerX - this.textRenderer.getWidth(title) / 2, curY, 0xFFFFFFFF, true);
        curY += 10 + 4;

        // Subtitle
        Text subtitle = Text.literal("Your target:");
        context.drawText(this.textRenderer, subtitle,
                centerX - this.textRenderer.getWidth(subtitle) / 2, curY, 0xFFAAAAAA, true);
        curY += 10 + 8;

        // Icon area (or history panels)
        int iconAreaY = curY;
        if (showHistory) {
            renderHistoryArea(context, centerX, iconAreaY);
        } else {
            // Bobbing animation
            float bob = (float) Math.sin(System.currentTimeMillis() / 500.0) * 3.0f;
            int iconY = (int) (iconAreaY + bob);

            context.getMatrices().pushMatrix();
            context.getMatrices().translate((float) (centerX - ICON_SIZE / 2), (float) iconY);
            context.getMatrices().scale((float) ICON_SCALE, (float) ICON_SCALE);
            context.drawItem(new ItemStack(targetItem), 0, 0);
            context.getMatrices().popMatrix();
        }
        curY += ICON_SIZE + 4;

        // Item name
        Text itemName = ItemPool.getDisplayName(targetItem);
        context.drawText(this.textRenderer, itemName,
                centerX - this.textRenderer.getWidth(itemName) / 2, curY, 0xFFFFFF00, true);
        curY += 10 + 4;

        // History toggle link
        Text linkText = Text.literal(showHistory ? "< Back" : "History >");
        int linkW = this.textRenderer.getWidth(linkText);
        int linkX = centerX - linkW / 2;
        boolean hovering = mouseX >= linkX && mouseX <= linkX + linkW
                && mouseY >= curY && mouseY <= curY + 10;
        int linkColor = hovering ? 0xFF88CCFF : 0xFF6699CC;
        context.drawText(this.textRenderer, linkText, linkX, curY, linkColor, true);

        // Store link bounds for click detection
        historyLinkX = linkX;
        historyLinkY = curY;
        historyLinkW = linkW;
        historyLinkH = 10;
    }

    private void renderHistoryArea(DrawContext context, int centerX, int areaY) {
        Identifier itemId = Registries.ITEM.getId(targetItem);
        List<ResultStore.RunResult> lastRuns = ResultStore.getLastRuns(itemId, 3);
        List<ResultStore.RunResult> topRuns = ResultStore.getTopRuns(itemId, 3);

        String lastHeader = "Last Three Runs";
        String topHeader = "Top Three Runs";
        float scale = 0.75f;
        int lastW = computePanelWidth(lastHeader, scale);
        int topW = computePanelWidth(topHeader, scale);

        int gap = 4;
        int totalW = lastW + gap + topW;
        int panelH = computePanelHeight(Math.max(Math.max(lastRuns.size(), topRuns.size()), 1));

        // Center both panels side-by-side in the icon area
        int panelY = areaY + (ICON_SIZE - panelH) / 2;
        int leftX = centerX - totalW / 2;

        renderRunPanel(context, leftX, panelY, lastHeader, lastRuns, lastW);
        renderRunPanel(context, leftX + lastW + gap, panelY, topHeader, topRuns, topW);
    }

    private int computePanelWidth(String header, float scale) {
        return (int) (this.textRenderer.getWidth(header) * scale) + PANEL_PADDING * 2;
    }

    private int computePanelHeight(int entryCount) {
        float scale = 0.75f;
        int scaledLineHeight = (int) ((this.textRenderer.fontHeight + 2) * scale);
        return PANEL_PADDING + scaledLineHeight + 2 + Math.max(entryCount, 1) * scaledLineHeight + PANEL_PADDING;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0 && click.x() >= historyLinkX && click.x() <= historyLinkX + historyLinkW
                && click.y() >= historyLinkY && click.y() <= historyLinkY + historyLinkH) {
            showHistory = !showHistory;
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    private void renderRunPanel(DrawContext context, int x, int y, String header,
                                List<ResultStore.RunResult> runs, int panelW) {
        float scale = 0.75f;
        int scaledLineHeight = (int) ((this.textRenderer.fontHeight + 2) * scale);
        int panelH = PANEL_PADDING + scaledLineHeight + 2 + Math.max(runs.size(), 1) * scaledLineHeight + PANEL_PADDING;

        // Background + border
        context.fill(x, y, x + panelW, y + panelH, PANEL_BG);
        drawPanelBorder(context, x, y, panelW, panelH);

        // Header (scaled)
        context.getMatrices().pushMatrix();
        context.getMatrices().translate((float) (x + PANEL_PADDING), (float) (y + PANEL_PADDING));
        context.getMatrices().scale(scale, scale);
        context.drawText(this.textRenderer, header, 0, 0, 0xFFFFFFFF, true);
        context.getMatrices().popMatrix();

        int entryY = y + PANEL_PADDING + scaledLineHeight + 2;

        if (runs.isEmpty()) {
            context.getMatrices().pushMatrix();
            context.getMatrices().translate((float) (x + PANEL_PADDING), (float) entryY);
            context.getMatrices().scale(scale, scale);
            context.drawText(this.textRenderer, "No runs yet", 0, 0, 0xFF888888, true);
            context.getMatrices().popMatrix();
        } else {
            for (int i = 0; i < runs.size(); i++) {
                String entry = (i + 1) + ". " + HuntState.formatTime(runs.get(i).timeMs);
                context.getMatrices().pushMatrix();
                context.getMatrices().translate((float) (x + PANEL_PADDING), (float) (entryY + i * scaledLineHeight));
                context.getMatrices().scale(scale, scale);
                context.drawText(this.textRenderer, entry, 0, 0, 0xFFDDDDDD, true);
                context.getMatrices().popMatrix();
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
