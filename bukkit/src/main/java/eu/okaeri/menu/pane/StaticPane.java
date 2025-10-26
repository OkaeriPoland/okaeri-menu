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
    private final List<MenuItem> autoItems;  // Auto-positioned items that reflow based on visibility

    private StaticPane(Builder builder) {
        super(builder.name, builder.bounds);
        this.staticItems.putAll(builder.items);
        this.fillerItem = builder.fillerItem;
        this.autoItems = new ArrayList<>(builder.autoItems);
    }

    @Override
    public void render(@NonNull Inventory inventory, @NonNull MenuContext context) {
        // Track which slots are occupied (for filler/clearing at the end)
        Map<Integer, Boolean> occupiedSlots = new HashMap<>();

        // Get per-player auto-item slot cache and clear it
        Map<Integer, MenuItem> autoItemSlotCache = context.getViewerState().getPaneRenderCache(this.getName());
        autoItemSlotCache.clear();

        // Step 1: Render static positioned items
        this.bounds.slots().forEachMap(this.staticItems, (localSlot, globalSlot, menuItem) -> {
            // Skip re-rendering interactive items - they manage their own state
            if (!menuItem.shouldRender()) {
                return;
            }

            if (globalSlot < inventory.getSize()) {
                ItemStack rendered = menuItem.render(context);
                inventory.setItem(globalSlot, rendered);
                occupiedSlots.put(localSlot, true);
            }
        });

        // Step 2: Render auto-positioned items (reflow based on visibility)
        int slotIndex = 0;
        for (MenuItem item : this.autoItems) {
            if (slotIndex >= this.bounds.getSlotCount()) {
                break;
            }

            // Find next free slot (skip slots occupied by static items)
            while ((slotIndex < this.bounds.getSlotCount()) && occupiedSlots.containsKey(slotIndex)) {
                slotIndex++;
            }
            if (slotIndex >= this.bounds.getSlotCount()) {
                break;
            }

            // Render item - returns null if visible=false
            ItemStack rendered = item.render(context);
            if (rendered != null) {
                // Calculate position from slot index
                int localRow = slotIndex / this.bounds.getWidth();
                int localCol = slotIndex % this.bounds.getWidth();
                int globalSlot = this.bounds.toGlobalSlot(localRow, localCol);

                if (globalSlot < inventory.getSize()) {
                    inventory.setItem(globalSlot, rendered);
                    occupiedSlots.put(slotIndex, true);
                    // Cache auto-item position for click routing (per-player)
                    autoItemSlotCache.put(slotIndex, item);
                }
                slotIndex++;  // Move to next slot after rendering
            }
            // If null (invisible), don't increment - next item takes this slot
        }

        // Step 3: Fill or clear remaining empty slots
        if (this.fillerItem != null) {
            ItemStack fillerItemStack = this.fillerItem.render(context);
            this.bounds.slots()
                .excludeKeys(occupiedSlots)
                .fill(inventory, fillerItemStack);
        } else {
            this.bounds.slots()
                .excludeKeys(occupiedSlots)
                .clear(inventory);
        }
    }

    /**
     * Gets the menu item at the specified local coordinates.
     * Checks static items first, then looks up auto-item from the render cache.
     *
     * @param localRow Row within this pane
     * @param localCol Column within this pane
     * @return The menu item, or null if none
     */
    @Override
    public MenuItem getItem(int localRow, int localCol) {
        // This method is used by non-context-aware code
        // For click routing, getItemByGlobalSlot(globalSlot, context) is used
        int localCoord = (localRow * this.bounds.getWidth()) + localCol;
        // Only return static items (auto-items need context)
        return this.staticItems.get(localCoord);
    }

    /**
     * Gets the menu item at local coordinates with context for per-player lookup.
     *
     * @param localRow Local Y coordinate
     * @param localCol Local X coordinate
     * @param context  The menu context (for per-player state)
     * @return The menu item, or null if not found
     */
    @Override
    public MenuItem getItem(int localRow, int localCol, @NonNull MenuContext context) {
        int localCoord = (localRow * this.bounds.getWidth()) + localCol;

        // Check static items first
        MenuItem staticItem = this.staticItems.get(localCoord);
        if (staticItem != null) {
            return staticItem;
        }

        // Check auto-item cache (per-player, populated during last render)
        Map<Integer, MenuItem> autoItemSlotCache = context.getViewerState().getPaneRenderCache(this.getName());
        return autoItemSlotCache.get(localCoord);
    }

    @NonNull
    public static Builder staticPane() {
        return new Builder();
    }

    @NonNull
    public static Builder staticPane(@NonNull String name) {
        return new Builder().name(name);
    }

    public static class Builder {
        private String name;
        private PaneBounds bounds = PaneBounds.fullInventory();
        private List<AbstractPane.ItemCoordinateEntry> itemEntries = new ArrayList<>();
        private Map<Integer, MenuItem> items = new HashMap<>();  // Populated in build()
        private List<MenuItem> autoItems = new ArrayList<>();  // Auto-positioned items
        private MenuItem fillerItem = null;

        @NonNull
        public Builder name(@NonNull String name) {
            this.name = name;
            return this;
        }

        @NonNull
        public Builder bounds(int y, int x, int height, int width) {
            this.bounds = PaneBounds.of(y, x, height, width);
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
         * @param localRow Row within the pane (0 to height-1)
         * @param localCol Column within the pane (0 to width-1)
         * @param item     The menu item
         * @return This builder
         * @throws IllegalArgumentException if coordinates are out of bounds
         */
        @NonNull
        public Builder item(int localRow, int localCol, @NonNull MenuItem item) {
            // Validate immediately for better error reporting
            this.bounds.validate(localRow, localCol);
            // Store coordinates for conversion during build()
            this.itemEntries.add(new AbstractPane.ItemCoordinateEntry(localRow, localCol, item));
            return this;
        }

        /**
         * Adds an auto-positioned item that fills the first available slot.
         * Auto-positioned items automatically reflow when visibility changes - when an item
         * becomes invisible (visible=false), it's completely skipped and the next item takes its place.
         *
         * <p>Auto-positioned items are rendered after static positioned items and filler,
         * filling slots left-to-right, top-to-bottom within the pane bounds.
         *
         * @param item The menu item to auto-position
         * @return This builder
         */
        @NonNull
        public Builder item(@NonNull MenuItem item) {
            this.autoItems.add(item);
            return this;
        }

        @NonNull
        public StaticPane build() {
            // Validation
            if (this.name == null) {
                throw new IllegalStateException("Pane name is required");
            }

            // Calculate slots from coordinates using final bounds with validation
            this.items = convertEntriesToSlotMap(this.itemEntries, this.bounds);
            return new StaticPane(this);
        }
    }
}
