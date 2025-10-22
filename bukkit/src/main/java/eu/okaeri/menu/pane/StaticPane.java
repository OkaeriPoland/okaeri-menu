package eu.okaeri.menu.pane;

import eu.okaeri.menu.MenuContext;
import eu.okaeri.menu.item.MenuItem;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A static pane with manually positioned items.
 */
@Getter
public class StaticPane extends AbstractPane {

    private final MenuItem fillerItem;  // Optional filler for empty slots

    private StaticPane(Builder builder) {
        super(builder.name, builder.bounds);
        this.staticItems.putAll(builder.items);
        this.fillerItem = builder.fillerItem;
    }

    @Override
    public void render(@NonNull Inventory inventory, @NonNull MenuContext context) {
        // Render filler items in empty slots first (as base layer)
        if (this.fillerItem != null) {
            ItemStack fillerItemStack = this.fillerItem.render(context);
            this.bounds.slots()
                .excludeKeys(this.staticItems)
                .fill(inventory, fillerItemStack);
        }

        // Render items (overwrites filler where items exist)
        this.bounds.slots().forEachMap(this.staticItems, (localSlot, globalSlot, menuItem) -> {
            // Skip re-rendering interactive items - they manage their own state
            if (!menuItem.shouldRender()) {
                return;
            }

            if (globalSlot < inventory.getSize()) {
                ItemStack rendered = menuItem.render(context);
                inventory.setItem(globalSlot, rendered);
            }
        });

        // Clear empty slots (no item and no filler)
        if (this.fillerItem == null) {
            this.bounds.slots()
                .excludeKeys(this.staticItems)
                .clear(inventory);
        }
    }

    @Override
    public void invalidate() {
        for (MenuItem item : this.staticItems.values()) {
            item.invalidate();
        }
        if (this.fillerItem != null) {
            this.fillerItem.invalidate();
        }
    }

    /**
     * Gets the menu item at the specified local coordinates.
     *
     * @param localX Column within this pane
     * @param localY Row within this pane
     * @return The menu item, or null if none
     */
    @Override
    public MenuItem getItem(int localX, int localY) {
        int localCoord = (localY * this.bounds.getWidth()) + localX;
        return this.staticItems.get(localCoord);
    }

    @NonNull
    public static Builder staticPane() {
        return new Builder();
    }

    public static class Builder {
        private String name = "unnamed";
        private PaneBounds bounds = PaneBounds.fullInventory();
        private List<AbstractPane.ItemCoordinateEntry> itemEntries = new ArrayList<>();
        private Map<Integer, MenuItem> items = new HashMap<>();  // Populated in build()
        private MenuItem fillerItem = null;

        @NonNull
        public Builder name(@NonNull String name) {
            this.name = name;
            return this;
        }

        @NonNull
        public Builder bounds(int x, int y, int width, int height) {
            this.bounds = PaneBounds.of(x, y, width, height);
            return this;
        }

        @NonNull
        public Builder bounds(@NonNull PaneBounds bounds) {
            this.bounds = bounds;
            return this;
        }

        /**
         * Sets a filler item to be displayed in empty slots.
         * The filler is purely decorative and non-interactive.
         *
         * @param filler The filler menu item
         * @return This builder
         */
        @NonNull
        public Builder filler(@NonNull MenuItem filler) {
            this.fillerItem = filler;
            return this;
        }

        /**
         * Adds an item at the specified local coordinates.
         * Coordinates are validated immediately and stored for conversion during build().
         *
         * @param localX Column within the pane (0 to width-1)
         * @param localY Row within the pane (0 to height-1)
         * @param item   The menu item
         * @return This builder
         * @throws IllegalArgumentException if coordinates are out of bounds
         */
        @NonNull
        public Builder item(int localX, int localY, @NonNull MenuItem item) {
            // Validate immediately for better error reporting
            this.bounds.validate(localX, localY);
            // Store coordinates for conversion during build()
            this.itemEntries.add(new AbstractPane.ItemCoordinateEntry(localX, localY, item));
            return this;
        }

        @NonNull
        public StaticPane build() {
            // Calculate slots from coordinates using final bounds with validation
            this.items = convertEntriesToSlotMap(this.itemEntries, this.bounds);
            return new StaticPane(this);
        }
    }
}
