package com.spawnhunt.screen;

import com.spawnhunt.data.ItemPool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ItemChooserScreen extends Screen {
    private final Item currentItem;
    private final boolean hardcore;
    private EditBox searchField;
    private ItemListWidget itemList;
    private Button selectButton;
    private List<Item> sortedItems;

    public ItemChooserScreen(Item currentItem, boolean hardcore) {
        super(Component.literal("Choose Item"));
        this.currentItem = currentItem;
        this.hardcore = hardcore;
    }

    @Override
    protected void init() {
        sortedItems = new ArrayList<>(ItemPool.getPool());
        sortedItems.sort(Comparator.comparing(item ->
                ItemPool.getDisplayName(item).getString().toLowerCase(Locale.ROOT)));

        searchField = new EditBox(this.getFont(), this.width / 2 - 100, 22, 200, 20, Component.literal("Search"));
        searchField.setResponder(text -> refreshList());
        this.addRenderableWidget(searchField);

        itemList = new ItemListWidget(this.minecraft, this.width, this.height - 84, 48, 20);
        this.addRenderableWidget(itemList);

        refreshList();

        this.addRenderableWidget(
                Button.builder(Component.literal("Back"), button -> onClose())
                        .bounds(this.width / 2 - 104, this.height - 28, 100, 20).build()
        );

        selectButton = Button.builder(Component.literal("Select"), button -> selectAndReturn())
                .bounds(this.width / 2 + 4, this.height - 28, 100, 20).build();
        this.addRenderableWidget(selectButton);

        this.setInitialFocus(searchField);
    }

    void selectAndReturn() {
        ItemListWidget.Entry entry = itemList.getSelected();
        if (entry != null) {
            this.minecraft.setScreen(new SpawnHuntScreen(entry.getItem(), this.hardcore));
        }
    }

    private void refreshList() {
        String query = searchField.getValue().toLowerCase(Locale.ROOT).trim();
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
            itemList.setScrollAmount(0);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(this.getFont(), this.title, this.width / 2, 8, 0xFFFFFF);
        selectButton.active = itemList.getSelected() != null;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(new SpawnHuntScreen(currentItem, this.hardcore));
    }

    class ItemListWidget extends ObjectSelectionList<ItemListWidget.Entry> {
        public ItemListWidget(Minecraft client, int width, int height, int y, int itemHeight) {
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

        class Entry extends ObjectSelectionList.Entry<Entry> {
            private final Item item;
            private final Component displayName;
            private long lastClickTime;

            public Entry(Item item) {
                this.item = item;
                this.displayName = ItemPool.getDisplayName(item);
            }

            public Item getItem() {
                return item;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float delta) {
                int x = this.getContentX();
                int y = this.getContentY();
                context.item(new ItemStack(item), x + 2, y + 1);
                context.text(Minecraft.getInstance().font, displayName,
                        x + 24, y + 5, 0xFFFFFFFF, true);
            }

            @Override
            public Component getNarration() {
                return displayName;
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean selected) {
                ItemListWidget.this.setSelected(this);
                long now = Util.getMillis();
                if (now - lastClickTime < 250L) {
                    ItemChooserScreen.this.selectAndReturn();
                }
                lastClickTime = now;
                return true;
            }
        }
    }
}
