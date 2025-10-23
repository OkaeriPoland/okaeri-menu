package eu.okaeri.menu.pane;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.MenuContext;
import eu.okaeri.menu.async.AsyncCache;
import eu.okaeri.menu.async.AsyncUtils;
import eu.okaeri.menu.item.MenuItem;
import eu.okaeri.menu.pagination.LoaderContext;
import eu.okaeri.menu.pagination.PaginationContext;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * PaginatedPane with async data loading and suspense states.
 * Automatically registers data in viewer's AsyncCache under the pane name.
 * <p>
 * States:
 * - LOADING: Fills all slots with loading item
 * - ERROR: Fills all slots with error item (global loader failure)
 * - EMPTY: Shows single empty item in center (successful load, no items)
 * - SUCCESS: Renders items using parent PaginatedPane logic
 *
 * @param <T> The type of items to paginate
 */
@Getter
public class AsyncPaginatedPane<T> extends PaginatedPane<T> {

    private final Function<LoaderContext, List<T>> asyncLoader;
    private final Duration ttl;
    private final MenuItem loadingItem;
    private final MenuItem errorItem;
    private final MenuItem emptyItem;

    // Mutable container for items to support delegation to parent render
    private final AtomicReference<List<T>> currentItems;

    private AsyncPaginatedPane(@NonNull Builder<T> builder) {
        super(convertToParentBuilder(builder, new AtomicReference<>(Collections.emptyList())));
        this.currentItems = builder.currentItemsRef;  // Capture the same reference used in parent
        this.asyncLoader = builder.asyncLoader;
        this.ttl = builder.ttl;
        this.loadingItem = builder.loadingItem;
        this.errorItem = builder.errorItem;
        this.emptyItem = builder.emptyItem;
    }

    /**
     * Converts AsyncPaginatedPane.Builder to PaginatedPane.Builder.
     * Sets items supplier to read from currentItems container.
     */
    private static <T> PaginatedPane.Builder<T> convertToParentBuilder(Builder<T> asyncBuilder, AtomicReference<List<T>> currentItemsRef) {
        asyncBuilder.currentItemsRef = currentItemsRef;  // Store reference for later

        PaginatedPane.Builder<T> parentBuilder = PaginatedPane.pane();
        parentBuilder
            .name(asyncBuilder.name)
            .bounds(asyncBuilder.bounds.getX(), asyncBuilder.bounds.getY(),
                asyncBuilder.bounds.getWidth(), asyncBuilder.bounds.getHeight())
            .renderer(asyncBuilder.itemRenderer);

        // Items supplier reads from currentItems container
        // Lambda captures the reference, which will be populated at render time
        parentBuilder.items(currentItemsRef::get);

        // Copy items per page if set
        if (asyncBuilder.itemsPerPage > 0) {
            parentBuilder.itemsPerPage(asyncBuilder.itemsPerPage);
        }

        // Copy static items
        asyncBuilder.staticItems.forEach(entry ->
            parentBuilder.staticItem(entry.getLocalX(), entry.getLocalY(), entry.getMenuItem()));

        return parentBuilder;
    }

    @Override
    public void render(@NonNull Inventory inventory, @NonNull MenuContext context) {
        String cacheKey = this.getName();  // Use pane name as cache key

        Menu.ViewerState state = context.getMenu().getViewerState(context.getEntity().getUniqueId());
        if (state == null) {
            return;
        }

        AsyncCache cache = state.getAsyncCache();
        AsyncCache.AsyncState asyncState = cache.getState(cacheKey);

        // Check if we need to load (first time or expired)
        if ((asyncState == null) || cache.isExpired(cacheKey)) {
            // Get pagination context to extract filter values
            // Use dummy page size since we're not using parent's getFilteredItems() for async panes
            PaginationContext<T> pagination = PaginationContext.get(
                context.getMenu(),
                this.getName(),
                context.getEntity(),
                Collections.emptyList(),  // Dummy items list
                this.getItemsPerPage()
            );

            // Collect filters from all filtering items in all panes
            for (Pane pane : context.getMenu().getPanes().values()) {
                for (MenuItem menuItem : pane.getFilteringItems().values()) {
                    this.applyFiltersFromItem(menuItem, pagination);
                }
            }

            // Create LoaderContext wrapping the pagination context
            LoaderContext loaderContext = LoaderContext.from(pagination);

            // Start async load (or background reload if stale data exists)
            // Wrap Function<LoaderContext, List<T>> as Supplier<List<T>>
            context.loadAsync(cacheKey, () -> this.asyncLoader.apply(loaderContext), this.ttl);
            // Re-check state after starting load
            asyncState = cache.getState(cacheKey);
        }

        // Handle null state (shouldn't happen but safety fallback)
        if (asyncState == null) {
            asyncState = AsyncCache.AsyncState.LOADING;
        }

        switch (asyncState) {
            case LOADING -> this.renderLoadingState(inventory, context);
            case ERROR -> this.renderErrorState(inventory, context);
            case SUCCESS -> {
                // Get loaded data from cache
                cache.get(cacheKey, List.class).ifPresentOrElse(
                    data -> {
                        @SuppressWarnings("unchecked")
                        List<T> items = (List<T>) data;
                        if (items.isEmpty()) {
                            this.renderEmptyState(inventory, context);
                        } else {
                            this.renderSuccessState(inventory, context, items);
                        }
                    },
                    () -> this.renderEmptyState(inventory, context)
                );
            }
            default -> this.renderEmptyState(inventory, context);
        }
    }

    /**
     * Renders loading state by filling all slots with loading item.
     */
    private void renderLoadingState(@NonNull Inventory inventory, @NonNull MenuContext context) {
        // Fill all slots with loading item
        ItemStack loadingItemStack = this.loadingItem.render(context);
        this.getBounds().slots().fill(inventory, loadingItemStack);

        // Render static items on top
        this.renderStaticItems(inventory, context, this.getStaticItems());
    }

    /**
     * Renders error state by filling all slots with error item.
     */
    private void renderErrorState(@NonNull Inventory inventory, @NonNull MenuContext context) {
        // Fill all slots with error item
        ItemStack errorItemStack = this.errorItem.render(context);
        this.getBounds().slots().fill(inventory, errorItemStack);

        // Render static items on top
        this.renderStaticItems(inventory, context, this.getStaticItems());
    }

    /**
     * Renders empty state by showing single item in center of pane.
     */
    private void renderEmptyState(@NonNull Inventory inventory, @NonNull MenuContext context) {
        PaneBounds bounds = this.getBounds();

        // Show empty item at top-left (0,0)
        ItemStack emptyItemStack = this.emptyItem.render(context);
        inventory.setItem(bounds.toGlobalSlot(0, 0), emptyItemStack);

        // Clear other slots (exclude slot 0 and static items)
        bounds.slots()
            .excludeKeys(this.getStaticItems())
            .exclude(Set.of(0))
            .clear(inventory);

        // Render static items on top
        this.renderStaticItems(inventory, context, this.getStaticItems());
    }

    /**
     * Renders success state by using parent PaginatedPane rendering logic.
     * Sets currentItems so the parent's itemsSupplier returns the loaded data.
     */
    private void renderSuccessState(@NonNull Inventory inventory, @NonNull MenuContext context, @NonNull List<T> items) {
        // Update currentItems so parent's itemsSupplier returns the loaded data
        this.currentItems.set(items);

        // Delegate to parent class render which will use items from currentItems
        super.render(inventory, context);
    }

    /**
     * Creates a new builder for AsyncPaginatedPane.
     *
     * @param <T> The type of items to paginate
     * @return A new builder instance
     */
    @NonNull
    public static <T> Builder<T> paneAsync() {
        return new Builder<>();
    }

    /**
     * Creates a new builder for AsyncPaginatedPane with explicit type.
     * This avoids the need for type parameter specification at the call site.
     *
     * @param type The class of items to paginate (not used at runtime, only for type inference)
     * @param <T>  The type of items to paginate
     * @return A new builder instance
     */
    @NonNull
    public static <T> Builder<T> paneAsync(@NonNull Class<T> type) {
        return new Builder<>();
    }

    public static class Builder<T> {

        private String name;
        private PaneBounds bounds;
        private Function<LoaderContext, List<T>> asyncLoader;
        private Duration ttl = Duration.ofSeconds(30);  // Default TTL
        private BiFunction<T, Integer, MenuItem> itemRenderer;
        private int itemsPerPage;
        private List<AbstractPane.ItemCoordinateEntry> staticItems = new ArrayList<>();
        private MenuItem loadingItem;
        private MenuItem errorItem;
        private MenuItem emptyItem;

        // Shared reference for currentItems, set during construction
        private AtomicReference<List<T>> currentItemsRef;

        /**
         * Sets the pane name (used as cache key and pagination context identifier).
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
         * Sets the pane bounds using PaneBounds object.
         *
         * @param bounds The pane bounds
         * @return This builder
         */
        @NonNull
        public Builder<T> bounds(@NonNull PaneBounds bounds) {
            this.bounds = bounds;
            return this;
        }

        /**
         * Sets the async data loader with LoaderContext.
         * The loader receives pagination state and filter values for database queries.
         *
         * @param loader The function that loads data with LoaderContext
         * @return This builder
         */
        @NonNull
        public Builder<T> loader(@NonNull Function<LoaderContext, List<T>> loader) {
            this.asyncLoader = loader;
            return this;
        }

        /**
         * Sets the time-to-live for cached data.
         * After TTL expires, data will be reloaded.
         *
         * @param ttl The TTL duration
         * @return This builder
         */
        @NonNull
        public Builder<T> ttl(@NonNull Duration ttl) {
            this.ttl = ttl;
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
         * Defaults to pane size (width * height - static items).
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
         *
         * @param localX   Local X coordinate
         * @param localY   Local Y coordinate
         * @param menuItem The menu item
         * @return This builder
         */
        @NonNull
        public Builder<T> staticItem(int localX, int localY, @NonNull MenuItem menuItem) {
            this.staticItems.add(new AbstractPane.ItemCoordinateEntry(localX, localY, menuItem));
            return this;
        }

        /**
         * Sets the loading item (shown while data is loading).
         * Fills all slots in pane.
         *
         * @param item The loading item
         * @return This builder
         */
        @NonNull
        public Builder<T> loading(@NonNull MenuItem item) {
            this.loadingItem = item;
            return this;
        }

        /**
         * Sets the error item (shown when loader fails).
         * Fills all slots in pane.
         *
         * @param item The error item
         * @return This builder
         */
        @NonNull
        public Builder<T> error(@NonNull MenuItem item) {
            this.errorItem = item;
            return this;
        }

        /**
         * Sets the empty item (shown when list is empty after loading).
         * Shows single item in center of pane.
         *
         * @param item The empty item
         * @return This builder
         */
        @NonNull
        public Builder<T> empty(@NonNull MenuItem item) {
            this.emptyItem = item;
            return this;
        }

        @NonNull
        public AsyncPaginatedPane<T> build() {
            // Validation
            if (this.name == null) {
                throw new IllegalStateException("Pane name is required");
            }
            if (this.bounds == null) {
                throw new IllegalStateException("Pane bounds are required");
            }
            if (this.asyncLoader == null) {
                throw new IllegalStateException("Async loader is required (use .loader())");
            }
            if (this.itemRenderer == null) {
                throw new IllegalStateException("Item renderer is required");
            }

            // Use AsyncUtils defaults if suspense items not provided
            if (this.loadingItem == null) {
                this.loadingItem = AsyncUtils.loadingItem().build();
            }
            if (this.errorItem == null) {
                this.errorItem = AsyncUtils.errorItem().build();
            }
            if (this.emptyItem == null) {
                this.emptyItem = AsyncUtils.emptyItem().build();
            }

            // Default items per page to pane size if not set
            if (this.itemsPerPage == 0) {
                this.itemsPerPage = (this.bounds.getWidth() * this.bounds.getHeight()) - this.staticItems.size();
            }

            return new AsyncPaginatedPane<>(this);
        }
    }
}
