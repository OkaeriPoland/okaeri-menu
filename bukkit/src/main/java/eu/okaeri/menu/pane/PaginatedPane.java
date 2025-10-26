package eu.okaeri.menu.pane;

import eu.okaeri.menu.MenuContext;
import eu.okaeri.menu.item.MenuItem;
import eu.okaeri.menu.pane.pagination.ItemFilter;
import eu.okaeri.menu.util.TriFunction;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A pane that displays paginated content.
 * Automatically handles splitting items across pages and rendering the current page.
 *
 * @param <T> The type of items to paginate
 */
@Getter
public class PaginatedPane<T> extends AbstractPane {

    private final Supplier<List<T>> itemsSupplier;
    private final TriFunction<MenuContext, T, Integer, MenuItem> itemRenderer;
    private final int itemsPerPage;

    protected PaginatedPane(@NonNull Builder<T> builder) {
        super(builder.name, builder.bounds);
        this.itemsSupplier = builder.itemsSupplier;
        this.itemRenderer = builder.itemRenderer;
        this.itemsPerPage = builder.itemsPerPage;
        // Convert entries if builder hasn't been through build() yet (for AsyncPaginatedPane)
        if (builder.staticItems.isEmpty() && !builder.staticItemEntries.isEmpty()) {
            this.staticItems.putAll(convertEntriesToSlotMap(builder.staticItemEntries, builder.bounds));
        } else {
            this.staticItems.putAll(builder.staticItems);
        }
    }

    @Override
    public void render(@NonNull Inventory inventory, @NonNull MenuContext context) {
        // Get pagination context for this viewer (reactive - reads items from pane on-demand)
        PaginationContext<T> pagination = PaginationContext.get(context, this);

        // Get per-player rendered items cache and clear it
        Map<Integer, MenuItem> renderedItems = context.getViewerState().getPaneRenderCache(this.getName());
        renderedItems.clear();

        // Apply declarative filters from menu items
        this.applyItemFilters(context, pagination);

        // Get items for current page
        List<T> pageItems = pagination.getCurrentPageItems();

        // Render paginated items
        // Use separate counters: slotIndex tracks position, itemIndex tracks which item to render
        int slotIndex = 0;
        int itemIndex = 0;

        while ((itemIndex < pageItems.size()) && (slotIndex < this.bounds.getSlotCount())) {
            // Calculate position in pane
            int localRow = slotIndex / this.bounds.getWidth();
            int localCol = slotIndex % this.bounds.getWidth();
            int localSlot = (localRow * this.bounds.getWidth()) + localCol;

            // Check if position is occupied by static item
            if (this.staticItems.containsKey(localSlot)) {
                slotIndex++; // Skip this slot, retry same item at next position
                continue;
            }

            // Get the current item to render
            T item = pageItems.get(itemIndex);

            // Render the item
            MenuItem menuItem = this.itemRenderer.apply(context, item, itemIndex);
            if (menuItem != null) {
                ItemStack itemStack = menuItem.render(context);
                if (itemStack != null) {
                    int globalSlot = this.bounds.toGlobalSlot(localRow, localCol);
                    inventory.setItem(globalSlot, itemStack);
                }
                // Track this item for click routing (per-player)
                renderedItems.put(localSlot, menuItem);
            }

            itemIndex++; // Move to next item
            slotIndex++; // Move to next slot
        }

        // Clear unfilled slots (slots beyond the last rendered item, but not static items)
        // This prevents old items from showing when page has fewer items
        this.bounds.slots()
            .excludeKeys(this.staticItems)
            .range(slotIndex, this.bounds.getSlotCount())
            .clear(inventory);

        // Render static items (buttons, decorations, etc.)
        this.renderStaticItems(inventory, context, this.staticItems);
    }

    /**
     * Applies declarative filters from menu items to this pane's pagination context.
     * Collects all ItemFilters from all menu items that target this pane.
     * Stores the filters directly so they can be lazily evaluated (when() and value() suppliers).
     *
     * @param context    The reactive context
     * @param pagination The pagination context to apply filters to
     */
    @SuppressWarnings("unchecked")
    protected void applyItemFilters(@NonNull MenuContext context, @NonNull PaginationContext<T> pagination) {
        // Sync declarative filters from menu items
        for (Pane pane : context.getMenu().getPanes().values()) {
            for (MenuItem menuItem : pane.getFilteringItems().values()) {
                this.applyFiltersFromItem(context, menuItem, pagination);
            }
        }
    }

    /**
     * Applies filters from a single menu item to the pagination context.
     * Stores the filter directly (no prefix needed) for lazy evaluation.
     * Eagerly validates filter values to catch exceptions early.
     */
    @SuppressWarnings("unchecked")
    protected void applyFiltersFromItem(@NonNull MenuContext context, @NonNull MenuItem menuItem, @NonNull PaginationContext<T> pagination) {
        for (ItemFilter<?> filter : menuItem.getFilters()) {
            // Check if this filter targets this pane
            if (filter.getTargetPane().equals(this.name)) {
                // Eagerly validate by extracting value (will throw if value() supplier fails)
                // This ensures exceptions are caught by Menu.render()'s error handling
                if (filter.isActive(context)) {
                    filter.extractValue();  // May throw - that's intentional!
                }

                // Store the filter directly (lazy when() and value() evaluation)
                pagination.setFilter(filter);
            }
        }
    }

    @Override
    public MenuItem getItem(int localRow, int localCol) {
        // This method is used by StaticPane and for non-paginated operations
        // For PaginatedPane clicks, getItemByGlobalSlot(globalSlot, context) is used
        int localSlot = this.localCoordinatesToSlot(localRow, localCol);
        return this.staticItems.get(localSlot);
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
        int localSlot = this.localCoordinatesToSlot(localRow, localCol);
        // Check static items first (they have priority)
        MenuItem staticItem = this.staticItems.get(localSlot);
        if (staticItem != null) {
            return staticItem;
        }
        // Check rendered paginated items (per-player)
        Map<Integer, MenuItem> renderedItems = context.getViewerState().getPaneRenderCache(this.getName());
        return renderedItems.get(localSlot);
    }

    /**
     * Adds a static item at a local position.
     * Static items are always rendered and don't get replaced by paginated content.
     *
     * @param localRow Local Y coordinate
     * @param localCol Local X coordinate
     * @param menuItem The menu item
     */
    public void setStaticItem(int localRow, int localCol, @NonNull MenuItem menuItem) {
        int localSlot = (localRow * this.bounds.getWidth()) + localCol;
        this.staticItems.put(localSlot, menuItem);
    }

    @NonNull
    public static <T> Builder<T> pane() {
        return new Builder<>();
    }

    /**
     * Creates a new builder for PaginatedPane with a pre-set name.
     *
     * @param name The pane name
     * @param <T>  The type of items to paginate
     * @return A new builder instance with name pre-set
     */
    @NonNull
    public static <T> Builder<T> pane(@NonNull String name) {
        return new Builder<T>().name(name);
    }

    /**
     * Creates a new builder for PaginatedPane with explicit type.
     * This avoids the need for type parameter specification at the call site.
     *
     * @param type The class of items to paginate (not used at runtime, only for type inference)
     * @param <T>  The type of items to paginate
     * @return A new builder instance
     */
    @NonNull
    public static <T> Builder<T> pane(@NonNull Class<T> type) {
        return new Builder<>();
    }

    /**
     * Creates a new builder for PaginatedPane with both name and type.
     *
     * @param name The pane name
     * @param type The class of items to paginate (not used at runtime, only for type inference)
     * @param <T>  The type of items to paginate
     * @return A new builder instance with name pre-set
     */
    @NonNull
    public static <T> Builder<T> pane(@NonNull String name, @NonNull Class<T> type) {
        return new Builder<T>().name(name);
    }

    public static class Builder<T> {
        private String name;
        private PaneBounds bounds;
        private Supplier<List<T>> itemsSupplier;
        private TriFunction<MenuContext, T, Integer, MenuItem> itemRenderer;
        private int itemsPerPage;
        private List<AbstractPane.ItemCoordinateEntry> staticItemEntries = new ArrayList<>();
        private Map<Integer, MenuItem> staticItems = new HashMap<>();  // Populated in build()

        /**
         * Sets the pane name (used as pagination context identifier).
         *
         * @param name The name
         * @return This builder
         */
        @NonNull
        public Builder<T> name(@NonNull String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the pane bounds.
         *
         * @param y      Y position (0-5)
         * @param x      X position (0-8)
         * @param height Height (1-6)
         * @param width  Width (1-9)
         * @return This builder
         */
        @NonNull
        public Builder<T> bounds(int y, int x, int height, int width) {
            this.bounds = new PaneBounds(y, x, height, width);
            return this;
        }

        /**
         * Sets the items supplier (for dynamic content).
         *
         * @param supplier The supplier
         * @return This builder
         */
        @NonNull
        public Builder<T> items(@NonNull Supplier<List<T>> supplier) {
            this.itemsSupplier = supplier;
            return this;
        }

        /**
         * Sets the items (static list).
         *
         * @param items The items
         * @return This builder
         */
        @NonNull
        public Builder<T> items(@NonNull List<T> items) {
            this.itemsSupplier = () -> items;
            return this;
        }

        /**
         * Sets the item renderer function.
         * The function receives the menu context, item, and its index, and returns a MenuItem.
         *
         * @param renderer The renderer function (context, item, index) -> MenuItem
         * @return This builder
         */
        @NonNull
        public Builder<T> renderer(@NonNull TriFunction<MenuContext, T, Integer, MenuItem> renderer) {
            this.itemRenderer = renderer;
            return this;
        }

        /**
         * Sets the items per page.
         * Defaults to pane size (width * height).
         *
         * @param itemsPerPage Items per page
         * @return This builder
         */
        @NonNull
        public Builder<T> itemsPerPage(int itemsPerPage) {
            this.itemsPerPage = itemsPerPage;
            return this;
        }

        /**
         * Adds a static item that's always rendered (e.g., navigation buttons).
         * Coordinates are validated immediately and stored for conversion during build().
         *
         * @param localRow Local Y coordinate (0 to height-1)
         * @param localCol Local X coordinate (0 to width-1)
         * @param menuItem The menu item
         * @return This builder
         * @throws IllegalArgumentException if coordinates are out of bounds
         */
        @NonNull
        public Builder<T> staticItem(int localRow, int localCol, @NonNull MenuItem menuItem) {
            // Validate immediately for better error reporting
            this.bounds.validate(localRow, localCol);
            // Store coordinates for conversion during build()
            this.staticItemEntries.add(new AbstractPane.ItemCoordinateEntry(localRow, localCol, menuItem));
            return this;
        }

        @NonNull
        public PaginatedPane<T> build() {
            if (this.name == null) {
                throw new IllegalStateException("Pane name is required");
            }
            if (this.bounds == null) {
                throw new IllegalStateException("Pane bounds are required");
            }
            if (this.itemsSupplier == null) {
                throw new IllegalStateException("Items supplier is required");
            }
            if (this.itemRenderer == null) {
                throw new IllegalStateException("Item renderer is required");
            }

            // Calculate slots from coordinates using final bounds with validation
            this.staticItems = convertEntriesToSlotMap(this.staticItemEntries, this.bounds);

            // Default items per page to pane size if not set
            if (this.itemsPerPage == 0) {
                this.itemsPerPage = this.bounds.getSlotCount() - this.staticItems.size();
            }

            return new PaginatedPane<>(this);
        }
    }
}
