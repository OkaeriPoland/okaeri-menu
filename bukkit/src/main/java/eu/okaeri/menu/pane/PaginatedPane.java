package eu.okaeri.menu.pane;

import eu.okaeri.menu.MenuContext;
import eu.okaeri.menu.item.MenuItem;
import eu.okaeri.menu.pagination.ItemFilter;
import eu.okaeri.menu.pagination.PaginationContext;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;
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
    private final BiFunction<T, Integer, MenuItem> itemRenderer;
    private final int itemsPerPage;

    // Cache for rendered items
    private boolean dirty = true;

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
        // Get pagination context for this viewer
        List<T> allItems = this.itemsSupplier.get();
        PaginationContext<T> pagination = PaginationContext.get(context.getMenu(), this.name, context.getEntity(), allItems, this.itemsPerPage);

        // Update items in case they've changed (for dynamic suppliers)
        pagination.updateItems(allItems);

        // Apply declarative filters from menu items
        this.applyItemFilters(context, pagination);

        // Get items for current page
        List<T> pageItems = pagination.getCurrentPageItems();

        // Render paginated items
        int index = 0;
        for (T item : pageItems) {
            if (index >= this.itemsPerPage) {
                break; // Safety check
            }

            // Calculate position in pane
            int localX = index % this.bounds.getWidth();
            int localY = index / this.bounds.getWidth();

            // Check if position is occupied by static item
            int localSlot = (localY * this.bounds.getWidth()) + localX;
            if (this.staticItems.containsKey(localSlot)) {
                index++;
                continue; // Skip positions with static items
            }

            // Render the item
            MenuItem menuItem = this.itemRenderer.apply(item, index);
            if (menuItem != null) {
                ItemStack itemStack = menuItem.render(context);
                if (itemStack != null) {
                    int globalSlot = this.bounds.toGlobalSlot(localX, localY);
                    inventory.setItem(globalSlot, itemStack);
                }
            }

            index++;
        }

        // Clear unfilled slots (slots beyond the last rendered item, but not static items)
        // This prevents old items from showing when page has fewer items
        this.bounds.slots()
            .excludeKeys(this.staticItems)
            .range(index, this.bounds.getSlotCount())
            .clear(inventory);

        // Render static items (buttons, decorations, etc.)
        this.renderStaticItems(inventory, context, this.staticItems);

        this.dirty = false;
    }

    /**
     * Applies declarative filters from menu items to this pane's pagination context.
     * Collects all ItemFilters from all menu items that target this pane and are active.
     * Only manages declarative filters (prefixed with "declarative:"), leaving programmatic filters intact.
     *
     * @param context    The reactive context
     * @param pagination The pagination context to apply filters to
     */
    @SuppressWarnings("unchecked")
    private void applyItemFilters(@NonNull MenuContext context, @NonNull PaginationContext<T> pagination) {
        // Clear only declarative filters (ones we manage)
        pagination.clearFiltersWithPrefix("declarative:");

        // Collect filters from all filtering items in all panes
        for (Pane pane : context.getMenu().getPanes().values()) {
            for (MenuItem menuItem : pane.getFilteringItems().values()) {
                this.applyFiltersFromItem(menuItem, pagination);
            }
        }
    }

    /**
     * Applies filters from a single menu item to the pagination context.
     * Prefixes filter IDs with "declarative:" to distinguish from programmatic filters.
     */
    @SuppressWarnings("unchecked")
    private void applyFiltersFromItem(@NonNull MenuItem menuItem, @NonNull PaginationContext<T> pagination) {
        for (ItemFilter<?> filter : menuItem.getFilters()) {
            // Check if this filter targets this pane
            if (filter.getTargetPane().equals(this.name)) {
                // Check if the filter is active
                if (filter.isActive()) {
                    // Apply the filter with "declarative:" prefix (cast is safe because filter targets this pane's item type)
                    String filterId = (filter.getFilterId() != null)
                        ? ("declarative:" + filter.getFilterId())
                        : ("declarative:filter-" + System.identityHashCode(filter));
                    pagination.addFilter(
                        filterId,
                        (Predicate<T>) filter.getPredicate()
                    );
                }
            }
        }
    }

    @Override
    public void invalidate() {
        this.dirty = true;
    }

    @Override
    public MenuItem getItem(int localX, int localY) {
        int localSlot = this.localCoordinatesToSlot(localX, localY);
        return this.staticItems.get(localSlot);
    }

    /**
     * Adds a static item at a local position.
     * Static items are always rendered and don't get replaced by paginated content.
     *
     * @param localX   Local X coordinate
     * @param localY   Local Y coordinate
     * @param menuItem The menu item
     */
    public void setStaticItem(int localX, int localY, @NonNull MenuItem menuItem) {
        int localSlot = (localY * this.bounds.getWidth()) + localX;
        this.staticItems.put(localSlot, menuItem);
        this.invalidate();
    }

    @NonNull
    public static <T> Builder<T> pane() {
        return new Builder<>();
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

    public static class Builder<T> {
        private String name;
        private PaneBounds bounds;
        private Supplier<List<T>> itemsSupplier;
        private BiFunction<T, Integer, MenuItem> itemRenderer;
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
         * @param x      X position (0-8)
         * @param y      Y position (0-5)
         * @param width  Width (1-9)
         * @param height Height (1-6)
         * @return This builder
         */
        @NonNull
        public Builder<T> bounds(int x, int y, int width, int height) {
            this.bounds = new PaneBounds(x, y, width, height);
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
         * The function receives the item and its index, and returns a MenuItem.
         *
         * @param renderer The renderer function
         * @return This builder
         */
        @NonNull
        public Builder<T> renderer(@NonNull BiFunction<T, Integer, MenuItem> renderer) {
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
         * @param localX   Local X coordinate (0 to width-1)
         * @param localY   Local Y coordinate (0 to height-1)
         * @param menuItem The menu item
         * @return This builder
         * @throws IllegalArgumentException if coordinates are out of bounds
         */
        @NonNull
        public Builder<T> staticItem(int localX, int localY, @NonNull MenuItem menuItem) {
            // Validate immediately for better error reporting
            this.bounds.validate(localX, localY);
            // Store coordinates for conversion during build()
            this.staticItemEntries.add(new AbstractPane.ItemCoordinateEntry(localX, localY, menuItem));
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
