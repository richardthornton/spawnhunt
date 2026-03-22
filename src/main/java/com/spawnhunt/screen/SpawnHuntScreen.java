package com.spawnhunt.screen;

import com.spawnhunt.data.HuntState;
import com.spawnhunt.data.ItemPool;
import com.spawnhunt.data.ResultStore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Item selection screen — shown when the player clicks "SpawnHunt" on the title screen.
 * Vertical layout optimised for YouTube Shorts capture.
 */
public class SpawnHuntScreen extends Screen {
    private static final int ICON_SCALE = 4;
    private static final int ICON_SIZE = 16 * ICON_SCALE; // 64px
    private static final float PANEL_SCALE = 0.75f;
    private static final int PANEL_PADDING = 6;
    private static final int PANEL_BG = 0x80000000;
    private static final int PANEL_BORDER = 0x60444444;
    // Vertical layout spacing (shared between init and render)
    private static final int TEXT_H = 10;          // height of a single text line
    private static final int GAP_SMALL = 4;        // gap after title, icon, item name
    private static final int GAP_MEDIUM = 8;       // gap after subtitle, after button rows
    private static final int GAP_PRE_BTN = 10;     // gap before button block
    private static final int CHECKBOX_H = 20;
    private static final int TOTAL_HEIGHT = 196;
    private static final int BTN_W = 76;
    private static final int BTN_H = 20;
    private static final int BTN_GAP = 4;

    // Rolling animation constants
    private static final double ROLL_DURATION_MS = 5000.0;
    private static final double ROLL_BASE_DELAY = 50.0;
    private static final double ROLL_MAX_DELAY = 400.0;

    private final Random random = new Random();
    private Item targetItem;
    private boolean hardcore;
    private boolean showHistory;

    // Rolling animation state
    private boolean isRolling;
    private boolean rollPending;       // deferred start until first render (sound manager not ready in constructor)
    private long rollStartMs;
    private List<Long> rollTickTimes;  // cumulative timestamps for each item switch
    private List<Item> rollItems;      // items to display at each tick index
    private int rollIndex;
    private Item displayItem;          // item currently shown (rolling item or final target)

    // Button references for enabling/disabling during roll
    private ButtonWidget startButton;
    private ButtonWidget rerollButton;
    private ButtonWidget listButton;

    // Clickable "History" / "Back" link bounds (set during render)
    private int historyLinkX, historyLinkY, historyLinkW, historyLinkH;

    public SpawnHuntScreen() {
        super(Text.literal("SpawnHunt"));
        this.targetItem = ItemPool.getRandomItem(random);
        this.displayItem = this.targetItem;
        this.hardcore = true;
        this.rollPending = true;
    }

    public SpawnHuntScreen(Item preselectedItem) {
        this(preselectedItem, true);
    }

    public SpawnHuntScreen(Item preselectedItem, boolean hardcore) {
        super(Text.literal("SpawnHunt"));
        this.targetItem = preselectedItem;
        this.displayItem = preselectedItem;
        this.hardcore = hardcore;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        int topY = (this.height - TOTAL_HEIGHT) / 2;

        // Button Y: walk the same layout steps as render()
        int btnBlockY = topY
                + TEXT_H + GAP_SMALL          // title
                + TEXT_H + GAP_MEDIUM         // subtitle
                + ICON_SIZE + GAP_SMALL       // icon area
                + TEXT_H + GAP_SMALL          // item name
                + TEXT_H + GAP_PRE_BTN;       // history link
        int leftBtnX = centerX - BTN_W - BTN_GAP / 2;
        int rightBtnX = centerX + BTN_GAP / 2;

        // Row 1: Start, Reroll
        this.startButton = this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Start"), button -> {
                    HuntState.startHunt(Registries.ITEM.getId(targetItem), this.hardcore);
                    CreateWorldScreen.show(this.client, () -> {
                        this.client.setScreen(new TitleScreen());
                    });
                })
                .dimensions(leftBtnX, btnBlockY, BTN_W, BTN_H)
                .build()
        );

        this.rerollButton = this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Reroll"), button -> {
                    this.targetItem = ItemPool.getRandomItem(random);
                    this.showHistory = false;
                    startRolling();
                })
                .dimensions(rightBtnX, btnBlockY, BTN_W, BTN_H)
                .build()
        );

        // Row 2: List, Cancel
        int row2Y = btnBlockY + BTN_H + BTN_GAP;
        this.listButton = this.addDrawableChild(
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
        int checkboxY = row2Y + BTN_H + GAP_MEDIUM;
        this.addDrawableChild(
                CheckboxWidget.builder(Text.literal("Hardcore"), this.textRenderer)
                        .pos(centerX - 30, checkboxY)
                        .checked(this.hardcore)
                        .callback((checkbox, checked) -> this.hardcore = checked)
                        .build()
        );

        // Apply initial button state if already rolling
        updateButtonStates();
    }

    private void startRolling() {
        // Pre-compute tick schedule with quadratic easing (fast → slow)
        rollTickTimes = new ArrayList<>();
        rollItems = new ArrayList<>();

        double time = 0;
        while (time < ROLL_DURATION_MS) {
            rollTickTimes.add((long) time);
            rollItems.add(ItemPool.getRandomItem(random));
            double progress = time / ROLL_DURATION_MS;
            double delay = ROLL_BASE_DELAY + (ROLL_MAX_DELAY - ROLL_BASE_DELAY) * progress * progress;
            time += delay;
        }

        // Final tick is always the target item
        rollTickTimes.add((long) time);
        rollItems.add(targetItem);

        rollIndex = 0;
        displayItem = rollItems.get(0);
        rollStartMs = System.currentTimeMillis();
        isRolling = true;
        updateButtonStates();
    }

    private void updateRolling() {
        if (!isRolling) return;

        long elapsed = System.currentTimeMillis() - rollStartMs;
        int newIndex = rollIndex;

        // Find the highest tick index we've reached
        while (newIndex + 1 < rollTickTimes.size() && elapsed >= rollTickTimes.get(newIndex + 1)) {
            newIndex++;
        }

        if (newIndex != rollIndex) {
            rollIndex = newIndex;
            displayItem = rollItems.get(rollIndex);

            boolean isFinal = rollIndex >= rollTickTimes.size() - 1;

            // Play click sound for each intermediate item (not the final reveal)
            if (!isFinal) {
                MinecraftClient.getInstance().getSoundManager()
                        .play(PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f));
            }

            // Stop rolling when we reach the final item
            if (isFinal) {
                isRolling = false;
                displayItem = targetItem;
                updateButtonStates();
            }
        }
    }

    private void updateButtonStates() {
        if (startButton != null) startButton.active = !isRolling;
        if (rerollButton != null) rerollButton.active = !isRolling;
        if (listButton != null) listButton.active = !isRolling;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Deferred roll start (sound manager not available in constructor)
        if (rollPending) {
            rollPending = false;
            startRolling();
        }

        // Advance rolling animation
        updateRolling();

        // Dark background
        context.fill(0, 0, this.width, this.height, 0xC0000000);

        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int topY = (this.height - TOTAL_HEIGHT) / 2;
        int curY = topY;

        // Title
        Text title = Text.literal("SpawnHunt");
        context.drawText(this.textRenderer, title,
                centerX - this.textRenderer.getWidth(title) / 2, curY, 0xFFFFFFFF, true);
        curY += TEXT_H + GAP_SMALL;

        // Subtitle
        Text subtitle = Text.literal("Your target:");
        context.drawText(this.textRenderer, subtitle,
                centerX - this.textRenderer.getWidth(subtitle) / 2, curY, 0xFFAAAAAA, true);
        curY += TEXT_H + GAP_MEDIUM;

        // Icon area (or history panels)
        int iconAreaY = curY;
        if (showHistory) {
            renderHistoryArea(context, centerX, iconAreaY);
        } else {
            int iconY;
            if (isRolling) {
                // No bobbing during roll — static position
                iconY = iconAreaY;
            } else {
                // Bobbing animation when settled
                float bob = (float) Math.sin(System.currentTimeMillis() / 500.0) * 3.0f;
                iconY = (int) (iconAreaY + bob);
            }

            context.getMatrices().pushMatrix();
            context.getMatrices().translate((float) (centerX - ICON_SIZE / 2), (float) iconY);
            context.getMatrices().scale((float) ICON_SCALE, (float) ICON_SCALE);
            context.drawItem(new ItemStack(displayItem), 0, 0);
            context.getMatrices().popMatrix();
        }
        curY += ICON_SIZE + GAP_SMALL;

        // Item name
        Text itemName = ItemPool.getDisplayName(displayItem);
        int nameColor = isRolling ? 0xFF888888 : 0xFFFFFF00;
        context.drawText(this.textRenderer, itemName,
                centerX - this.textRenderer.getWidth(itemName) / 2, curY, nameColor, true);
        curY += TEXT_H + GAP_SMALL;

        // History toggle link (hidden during rolling)
        if (!isRolling) {
            Text linkText = Text.literal(showHistory ? "< Back" : "History >");
            int linkW = this.textRenderer.getWidth(linkText);
            int linkX = centerX - linkW / 2;
            boolean hovering = mouseX >= linkX && mouseX <= linkX + linkW
                    && mouseY >= curY && mouseY <= curY + TEXT_H;
            int linkColor = hovering ? 0xFF88CCFF : 0xFF6699CC;
            context.drawText(this.textRenderer, linkText, linkX, curY, linkColor, true);

            historyLinkX = linkX;
            historyLinkY = curY;
            historyLinkW = linkW;
            historyLinkH = TEXT_H;
        } else {
            // Clear link bounds so clicks don't register during rolling
            historyLinkW = 0;
            historyLinkH = 0;
        }
    }

    private void renderHistoryArea(DrawContext context, int centerX, int areaY) {
        Identifier itemId = Registries.ITEM.getId(targetItem);
        List<ResultStore.RunResult> lastRuns = ResultStore.getLastRuns(itemId, 3);
        List<ResultStore.RunResult> topRuns = ResultStore.getTopRuns(itemId, 3);

        String lastHeader = "Last Three Runs";
        String topHeader = "Top Three Runs";
        int lastW = computePanelWidth(lastHeader);
        int topW = computePanelWidth(topHeader);

        int gap = 4;
        int totalW = lastW + gap + topW;
        int panelH = computePanelHeight(Math.max(Math.max(lastRuns.size(), topRuns.size()), 1));

        // Center both panels side-by-side in the icon area
        int panelY = areaY + (ICON_SIZE - panelH) / 2;
        int leftX = centerX - totalW / 2;

        renderRunPanel(context, leftX, panelY, lastHeader, lastRuns, lastW);
        renderRunPanel(context, leftX + lastW + gap, panelY, topHeader, topRuns, topW);
    }

    private int computePanelWidth(String header) {
        return (int) (this.textRenderer.getWidth(header) * PANEL_SCALE) + PANEL_PADDING * 2;
    }

    private int computePanelHeight(int entryCount) {
        int scaledLineHeight = (int) ((this.textRenderer.fontHeight + 2) * PANEL_SCALE);
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
        int scaledLineHeight = (int) ((this.textRenderer.fontHeight + 2) * PANEL_SCALE);
        int panelH = PANEL_PADDING + scaledLineHeight + 2 + Math.max(runs.size(), 1) * scaledLineHeight + PANEL_PADDING;

        // Background + border
        context.fill(x, y, x + panelW, y + panelH, PANEL_BG);
        drawPanelBorder(context, x, y, panelW, panelH);

        // Header (scaled)
        context.getMatrices().pushMatrix();
        context.getMatrices().translate((float) (x + PANEL_PADDING), (float) (y + PANEL_PADDING));
        context.getMatrices().scale(PANEL_SCALE, PANEL_SCALE);
        context.drawText(this.textRenderer, header, 0, 0, 0xFFFFFFFF, true);
        context.getMatrices().popMatrix();

        int entryY = y + PANEL_PADDING + scaledLineHeight + 2;

        if (runs.isEmpty()) {
            context.getMatrices().pushMatrix();
            context.getMatrices().translate((float) (x + PANEL_PADDING), (float) entryY);
            context.getMatrices().scale(PANEL_SCALE, PANEL_SCALE);
            context.drawText(this.textRenderer, "No runs yet", 0, 0, 0xFF888888, true);
            context.getMatrices().popMatrix();
        } else {
            for (int i = 0; i < runs.size(); i++) {
                String entry = (i + 1) + ". " + HuntState.formatTime(runs.get(i).timeMs);
                context.getMatrices().pushMatrix();
                context.getMatrices().translate((float) (x + PANEL_PADDING), (float) (entryY + i * scaledLineHeight));
                context.getMatrices().scale(PANEL_SCALE, PANEL_SCALE);
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
