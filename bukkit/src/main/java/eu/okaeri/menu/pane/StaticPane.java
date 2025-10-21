package eu.okaeri.menu.pane;

import eu.okaeri.menu.MenuContext;
import eu.okaeri.menu.item.MenuItem;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * A static pane with manually positioned items.
 */
@Getter
public class StaticPane implements Pane {

    private final String name;
    private final PaneBounds bounds;
    private final Map<Integer, MenuItem> items = new HashMap<>();  // Local coordinates -> MenuItem
    private final MenuItem fillerItem;  // Optional filler for empty slots

    private StaticPane(Builder builder) {
        this.name = builder.name;
        this.bounds = builder.bounds;
        this.items.putAll(builder.items);
        this.fillerItem = builder.fillerItem;
    }

    @Override
    public void render(@NonNull Inventory inventory, @NonNull MenuContext context) {
        // Clear pane area first (but preserve interactive slots)
        for (int localY = 0; localY < this.bounds.getHeight(); localY++) {
            for (int localX = 0; localX < this.bounds.getWidth(); localX++) {
                int globalSlot = this.bounds.toGlobalSlot(localX, localY);
                if (globalSlot < inventory.getSize()) {
                    MenuItem menuItem = this.getItem(localX, localY);
                    // Don't clear interactive slots - they manage their own state
                    if ((menuItem == null) || menuItem.shouldRender()) {
                        inventory.setItem(globalSlot, null);
                    }
                }
            }
        }

        // Render filler items in empty slots
        if (this.fillerItem != null) {
            for (int localY = 0; localY < this.bounds.getHeight(); localY++) {
                for (int localX = 0; localX < this.bounds.getWidth(); localX++) {
                    int localCoord = (localY * this.bounds.getWidth()) + localX;

                    // Only fill if no item is defined at this position
                    if (!this.items.containsKey(localCoord)) {
                        int globalSlot = this.bounds.toGlobalSlot(localX, localY);
                        if (globalSlot < inventory.getSize()) {
                            ItemStack rendered = this.fillerItem.render(context);
                            inventory.setItem(globalSlot, rendered);
                        }
                    }
                }
            }
        }

        // Render items (skip interactive items)
        for (Map.Entry<Integer, MenuItem> entry : this.items.entrySet()) {
            int localCoord = entry.getKey();
            MenuItem menuItem = entry.getValue();

            // Skip re-rendering interactive items - they manage their own state
            if (!menuItem.shouldRender()) {
                continue;
            }

            int localX = localCoord % this.bounds.getWidth();
            int localY = localCoord / this.bounds.getWidth();

            int globalSlot = this.bounds.toGlobalSlot(localX, localY);
            if (globalSlot < inventory.getSize()) {
                ItemStack rendered = menuItem.render(context);
                inventory.setItem(globalSlot, rendered);
            }
        }
    }

    @Override
    public void invalidate() {
        for (MenuItem item : this.items.values()) {
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
    public MenuItem getItem(int localX, int localY) {
        int localCoord = (localY * this.bounds.getWidth()) + localX;
        return this.items.get(localCoord);
    }

    /**
     * Gets the menu item at the specified global slot.
     *
     * @param globalSlot The global slot index
     * @return The menu item, or null if slot not in this pane or no item
     */
    public MenuItem getItemByGlobalSlot(int globalSlot) {
        int globalX = globalSlot % 9;
        int globalY = globalSlot / 9;

        // Check if slot is within this pane's bounds
        if ((globalX < this.bounds.getX()) || (globalX >= (this.bounds.getX() + this.bounds.getWidth()))) {
            return null;
        }
        if ((globalY < this.bounds.getY()) || (globalY >= (this.bounds.getY() + this.bounds.getHeight()))) {
            return null;
        }

        int localX = globalX - this.bounds.getX();
        int localY = globalY - this.bounds.getY();
        return this.getItem(localX, localY);
    }

    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name = "unnamed";
        private PaneBounds bounds = PaneBounds.fullInventory();
        private Map<Integer, MenuItem> items = new HashMap<>();
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
         *
         * @param localX Column within the pane (0 to width-1)
         * @param localY Row within the pane (0 to height-1)
         * @param item   The menu item
         * @return This builder
         */
        @NonNull
        public Builder item(int localX, int localY, @NonNull MenuItem item) {
            if ((localX < 0) || (localX >= this.bounds.getWidth())) {
                throw new IllegalArgumentException("Local X out of bounds: " + localX + " (width=" + this.bounds.getWidth() + ")");
            }
            if ((localY < 0) || (localY >= this.bounds.getHeight())) {
                throw new IllegalArgumentException("Local Y out of bounds: " + localY + " (height=" + this.bounds.getHeight() + ")");
            }

            int localCoord = (localY * this.bounds.getWidth()) + localX;
            this.items.put(localCoord, item);
            return this;
        }

        @NonNull
        public StaticPane build() {
            return new StaticPane(this);
        }
    }
}
