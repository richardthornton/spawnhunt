package com.spawnhunt.screen;

import com.spawnhunt.data.ItemPool;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.Click;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ItemChooserScreen extends Screen {
    private final Item currentItem;
    private final boolean hardcore;
    private TextFieldWidget searchField;
    private ItemListWidget itemList;
    private ButtonWidget selectButton;
    private List<Item> sortedItems;

    public ItemChooserScreen(Item currentItem, boolean hardcore) {
        super(Text.literal("Choose Item"));
        this.currentItem = currentItem;
        this.hardcore = hardcore;
    }

    @Override
    protected void init() {
        sortedItems = new ArrayList<>(ItemPool.getPool());
        sortedItems.sort(Comparator.comparing(item ->
                ItemPool.getDisplayName(item).getString().toLowerCase(Locale.ROOT)));

        searchField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 22, 200, 20, Text.literal("Search"));
        searchField.setChangedListener(text -> refreshList());
        this.addDrawableChild(searchField);

        itemList = new ItemListWidget(this.client, this.width, this.height - 84, 48, 20);
        this.addDrawableChild(itemList);

        refreshList();

        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Back"), button -> close())
                        .dimensions(this.width / 2 - 104, this.height - 28, 100, 20).build()
        );

        selectButton = ButtonWidget.builder(Text.literal("Select"), button -> selectAndReturn())
                .dimensions(this.width / 2 + 4, this.height - 28, 100, 20).build();
        this.addDrawableChild(selectButton);

        this.setInitialFocus(searchField);
    }

    void selectAndReturn() {
        ItemListWidget.Entry entry = itemList.getSelectedOrNull();
        if (entry != null) {
            this.client.setScreen(new SpawnHuntScreen(entry.getItem(), this.hardcore));
        }
    }

    private void refreshList() {
        String query = searchField.getText().toLowerCase(Locale.ROOT).trim();
        itemList.clearEntries();

        ItemListWidget.Entry toSelect = null;
        for (Item item : sortedItems) {
            String name = ItemPool.getDisplayName(item).getString().toLowerCase(Locale.ROOT);
            if (query.isEmpty() || name.contains(query)) {
                ItemListWidget.Entry entry = itemList.addItem(item);
                if (item == currentItem && toSelect == null) {
                    toSelect = entry;
                }
            }
        }

        if (toSelect != null) {
            itemList.setSelected(toSelect);
            itemList.centerScrollOn(toSelect);
        } else {
            itemList.setScrollY(0);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);
        selectButton.active = itemList.getSelectedOrNull() != null;
    }

    @Override
    public void close() {
        this.client.setScreen(new SpawnHuntScreen(currentItem, this.hardcore));
    }

    class ItemListWidget extends AlwaysSelectedEntryListWidget<ItemListWidget.Entry> {
        public ItemListWidget(MinecraftClient client, int width, int height, int y, int itemHeight) {
            super(client, width, height, y, itemHeight);
        }

        @Override
        public void clearEntries() {
            super.clearEntries();
        }

        @Override
        public int addEntry(Entry entry) {
            return super.addEntry(entry);
        }

        @Override
        public void centerScrollOn(Entry entry) {
            super.centerScrollOn(entry);
        }

        public Entry addItem(Item item) {
            Entry entry = new Entry(item);
            addEntry(entry);
            return entry;
        }

        class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> {
            private final Item item;
            private final Text displayName;
            private long lastClickTime;

            public Entry(Item item) {
                this.item = item;
                this.displayName = ItemPool.getDisplayName(item);
            }

            public Item getItem() {
                return item;
            }

            @Override
            public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float delta) {
                int x = this.getX();
                int y = this.getY();
                context.drawItem(new ItemStack(item), x + 2, y + 1);
                context.drawText(ItemChooserScreen.this.textRenderer, displayName,
                        x + 24, y + 5, 0xFFFFFFFF, true);
            }

            @Override
            public Text getNarration() {
                return displayName;
            }

            @Override
            public boolean mouseClicked(Click click, boolean selected) {
                ItemListWidget.this.setSelected(this);
                long now = Util.getMeasuringTimeMs();
                if (now - lastClickTime < 250L) {
                    ItemChooserScreen.this.selectAndReturn();
                }
                lastClickTime = now;
                return true;
            }
        }
    }
}
