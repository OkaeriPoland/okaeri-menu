package eu.okaeri.menu.item;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.MenuContext;
import eu.okaeri.menu.async.AsyncCache;
import lombok.NonNull;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * MenuItem with async data loading and suspense states.
 * For single data source where entire item depends on async data.
 * <p>
 * Example usage:
 * <pre>{@code
 * MenuItem.async()
 *     .key("player-stats")  // Optional, auto-generated if not set
 *     .data(() -> database.getPlayerStats(player))
 *     .ttl(Duration.ofSeconds(30))
 *     .loading(MenuItem.builder()
 *         .material(Material.GRAY_STAINED_GLASS_PANE)
 *         .name("&7Loading stats...")
 *         .build())
 *     .error((ex) -> MenuItem.builder()
 *         .material(Material.RED_STAINED_GLASS_PANE)
 *         .name("&cFailed to load stats")
 *         .lore("&7" + ex.getMessage())
 *         .build())
 *     .loaded((stats) -> MenuItem.builder()
 *         .material(stats.getMaterial())
 *         .name("&e" + stats.getTitle())
 *         .lore(stats.formatDescription())
 *         .build())
 *     .build()
 * }</pre>
 */
public class AsyncMenuItem extends MenuItem {

    private final String cacheKey;
    private final Supplier<?> asyncLoader;
    private final Duration ttl;
    private final MenuItem loadingState;
    private final Function<Throwable, MenuItem> errorStateFactory;
    private final Function<Object, MenuItem> successStateFactory;

    private AsyncMenuItem(@NonNull Builder builder) {
        // Create dummy MenuItem.Builder for parent constructor
        // AsyncMenuItem delegates everything to state-specific items, so parent fields unused
        super(MenuItem.item());

        this.cacheKey = builder.cacheKey;
        this.asyncLoader = builder.asyncLoader;
        this.ttl = builder.ttl;
        this.loadingState = builder.loadingState;
        this.errorStateFactory = builder.errorStateFactory;
        this.successStateFactory = builder.successStateFactory;
    }

    /**
     * Renders this async menu item by delegating to state-specific items.
     * Checks cache state and returns appropriate ItemStack for current state.
     *
     * @param context The menu context
     * @return The rendered ItemStack based on current async state
     */
    @Override
    public ItemStack render(@NonNull MenuContext context) {
        Menu.ViewerState state = context.getMenu().getViewerState(context.getEntity().getUniqueId());
        if (state == null) {
            return this.loadingState.render(context);
        }

        AsyncCache cache = state.getAsyncCache();
        AsyncCache.AsyncState asyncState = cache.getState(this.cacheKey);

        // Start load if needed (not loaded or expired)
        if ((asyncState == null) || cache.isExpired(this.cacheKey)) {
            context.loadAsync(this.cacheKey, this.asyncLoader, this.ttl);
            asyncState = AsyncCache.AsyncState.LOADING;
        }

        // Delegate to appropriate state item
        switch (asyncState) {
            case LOADING:
                return this.loadingState.render(context);

            case ERROR:
                Throwable error = cache.getError(this.cacheKey)
                    .orElse(new RuntimeException("Unknown error"));
                MenuItem errorItem = this.errorStateFactory.apply(error);
                return errorItem.render(context);

            case SUCCESS:
                Object data = cache.get(this.cacheKey, Object.class).orElse(null);
                MenuItem successItem = this.successStateFactory.apply(data);
                return successItem.render(context);

            default:
                return this.loadingState.render(context);
        }
    }

    /**
     * Handles click events by delegating to the current state's item.
     *
     * @param context The click context
     */
    @Override
    public void handleClick(@NonNull MenuItemClickContext context) {
        // Determine current state and delegate click to appropriate item
        Menu.ViewerState state = context.getMenu().getViewerState(context.getEntity().getUniqueId());
        if (state == null) {
            this.loadingState.handleClick(context);
            return;
        }

        AsyncCache cache = state.getAsyncCache();
        AsyncCache.AsyncState asyncState = cache.getState(this.cacheKey);

        switch (asyncState) {
            case LOADING:
                this.loadingState.handleClick(context);
                break;

            case ERROR:
                Throwable error = cache.getError(this.cacheKey)
                    .orElse(new RuntimeException("Unknown error"));
                MenuItem errorItem = this.errorStateFactory.apply(error);
                errorItem.handleClick(context);
                break;

            case SUCCESS:
                Object data = cache.get(this.cacheKey, Object.class).orElse(null);
                MenuItem successItem = this.successStateFactory.apply(data);
                successItem.handleClick(context);
                break;

            default:
                this.loadingState.handleClick(context);
        }
    }

    /**
     * Creates a builder for AsyncMenuItem.
     *
     * @return New AsyncMenuItem builder
     */
    @NonNull
    public static Builder itemAsync() {
        return new Builder();
    }

    public static class Builder {
        private String cacheKey = "async-item-" + UUID.randomUUID();  // Auto-generate
        private Supplier<?> asyncLoader;
        private Duration ttl = Duration.ofSeconds(30);  // Default TTL
        private MenuItem loadingState;
        private Function<Throwable, MenuItem> errorStateFactory;
        private Function<Object, MenuItem> successStateFactory;

        /**
         * Sets the cache key for this async item.
         * If not set, a random UUID-based key is auto-generated.
         *
         * @param key The cache key
         * @return This builder
         */
        @NonNull
        public Builder key(@NonNull String key) {
            this.cacheKey = key;
            return this;
        }

        /**
         * Sets the async data loader.
         *
         * @param loader The supplier that loads data asynchronously
         * @param <T>    The data type
         * @return This builder
         */
        @NonNull
        public <T> Builder data(@NonNull Supplier<T> loader) {
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
        public Builder ttl(@NonNull Duration ttl) {
            this.ttl = ttl;
            return this;
        }

        /**
         * Sets the loading state item (shown while data is loading).
         *
         * @param item The loading item
         * @return This builder
         */
        @NonNull
        public Builder loading(@NonNull MenuItem item) {
            this.loadingState = item;
            return this;
        }

        /**
         * Sets the error state factory (creates item when loader fails).
         * The factory receives the exception and returns a MenuItem to display.
         *
         * @param factory The error state factory
         * @return This builder
         */
        @NonNull
        public Builder error(@NonNull Function<Throwable, MenuItem> factory) {
            this.errorStateFactory = factory;
            return this;
        }

        /**
         * Sets the success state factory (creates item when data loaded).
         * The factory receives the loaded data and returns a MenuItem to display.
         *
         * @param factory The success state factory
         * @param <T>     The expected data type
         * @return This builder
         */
        @NonNull
        @SuppressWarnings("unchecked")
        public <T> Builder loaded(@NonNull Function<T, MenuItem> factory) {
            this.successStateFactory = (Function<Object, MenuItem>) factory;
            return this;
        }

        /**
         * Builds the AsyncMenuItem.
         *
         * @return The built AsyncMenuItem
         * @throws IllegalStateException if required fields are not set
         */
        @NonNull
        public AsyncMenuItem build() {
            // Validation
            if (this.asyncLoader == null) {
                throw new IllegalStateException("Async loader is required (use .data())");
            }
            if (this.loadingState == null) {
                throw new IllegalStateException("Loading state is required (use .loading())");
            }
            if (this.errorStateFactory == null) {
                throw new IllegalStateException("Error state factory is required (use .error())");
            }
            if (this.successStateFactory == null) {
                throw new IllegalStateException("Success state factory is required (use .loaded())");
            }

            return new AsyncMenuItem(this);
        }
    }
}
