package eu.okaeri.menu.state;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.MenuContext;
import eu.okaeri.menu.async.AsyncCache;
import eu.okaeri.menu.async.AsyncExecutor;
import eu.okaeri.menu.pagination.PaginationContext;
import eu.okaeri.menu.pane.PaginatedPane;
import lombok.Getter;
import lombok.NonNull;
import lombok.Synchronized;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Encapsulates all state for a single viewer of this menu.
 * Includes inventory, pagination contexts per pane, async data cache, and custom per-player state.
 */
@Getter
public class ViewerState {

    private @NonNull final MenuContext context;
    private Inventory inventory;  // Lazily initialized on first access

    private @NonNull final AsyncCache asyncCache;
    private @NonNull final Map<String, PaginationContext<?>> paginationContexts = new ConcurrentHashMap<>();
    private @NonNull final Map<String, Optional<Object>> customState = new ConcurrentHashMap<>();

    // Dirty flag for tracking state changes (used by MenuUpdateTask)
    private volatile boolean dirty = false;

    // Last refresh timestamp for interval-based updates
    private volatile Instant lastRefreshTime = null;

    public ViewerState(@NonNull MenuContext context, Inventory inventory) {
        this.context = context;
        this.inventory = inventory;  // Can be null - will be lazily initialized
        AsyncExecutor asyncExecutor = context.getMenu().getAsyncExecutor();
        this.asyncCache = new AsyncCache(asyncExecutor, this);  // Pass reference for invalidation
    }

    /**
     * Gets the inventory, lazily initializing it if needed.
     * <p>
     * Lazy initialization allows pagination contexts to be created before title evaluation.
     * When this method is first called:
     * 1. ViewerState is already in the menu's viewerStates map
     * 2. Pagination contexts are already initialized
     * 3. Title can call ctx.pagination() which finds this state in the map
     *
     * @return The inventory
     */
    public Inventory getInventory() {
        if (this.inventory == null) {
            this.initializeInventory();
        }
        return this.inventory;
    }

    /**
     * Initializes the inventory with a placeholder title.
     * The actual title will be evaluated and updated after first render
     * to ensure pagination data (currentItems) is fresh.
     * Synchronized to prevent concurrent initialization by multiple threads.
     */
    @Synchronized
    private void initializeInventory() {
        // Double-check pattern - another thread might have initialized it
        if (this.inventory != null) {
            return;
        }

        Menu menu = this.context.getMenu();
        Player player = this.context.getPlayer();

        // Use a placeholder title initially (will be updated after first render)
        // This prevents reading stale pagination data (e.g., empty currentItems in AsyncPaginatedPane)
        Component placeholderTitle = Component.text(" ");

        // Create inventory with placeholder
        this.inventory = Bukkit.createInventory(menu, menu.getRows() * 9, placeholderTitle);
    }

    /**
     * Marks this viewer's state as dirty (needing refresh).
     * Called automatically when async cache, pagination, or custom state changes.
     */
    public void invalidate() {
        this.dirty = true;
    }

    /**
     * Checks if dirty and clears the flag atomically.
     * Used by MenuUpdateTask to determine if refresh is needed.
     *
     * @return true if was dirty (needs refresh)
     */
    public boolean isDirtyAndClear() {
        boolean wasDirty = this.dirty;
        this.dirty = false;
        return wasDirty;
    }

    /**
     * Checks if update interval has elapsed since last refresh.
     *
     * @param interval The update interval duration
     * @return true if interval has passed
     */
    public boolean isIntervalElapsed(@NonNull Duration interval) {
        if (this.lastRefreshTime == null) {
            return true;  // Never refreshed
        }
        Instant nextRefreshTime = this.lastRefreshTime.plus(interval);
        return Instant.now().isAfter(nextRefreshTime);
    }

    /**
     * Records the current time as last refresh timestamp.
     * Called by MenuUpdateTask after refreshing.
     */
    public void recordRefresh() {
        this.lastRefreshTime = Instant.now();
    }

    /**
     * Checks if any async cache entries have expired TTLs.
     * Used by MenuUpdateTask to trigger refreshes for reactive data.
     *
     * @return true if any cached async data is expired
     */
    public boolean hasExpiredAsyncData() {
        return this.asyncCache.hasExpiredEntries();
    }

    /**
     * Updates the inventory reference (used when title changes).
     */
    public void setInventory(@NonNull Inventory inventory) {
        this.inventory = inventory;
    }

    /**
     * Gets or creates a reactive pagination context for a pane.
     * The context stores a reference to the pane and reads data on-demand.
     *
     * @param pane The paginated pane (source of truth)
     * @param <T>  Item type
     * @return Cached or new pagination context
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public <T> PaginationContext<T> getPagination(@NonNull PaginatedPane<T> pane) {
        return (PaginationContext<T>) this.paginationContexts.computeIfAbsent(pane.getName(),
            k -> new PaginationContext<>(this.context, pane)
        );
    }

    /**
     * Gets state with type checking.
     * Falls back to:
     * 1. Custom state value (including explicit null)
     * 2. Menu-level default
     * 3. Primitive wrapper default (0, false, etc.)
     * 4. null (for non-primitive types)
     */
    public <T> T getState(@NonNull String key, @NonNull Class<T> type) {
        Optional<Object> optValue = this.customState.get(key);

        if (optValue != null) {
            Object value = optValue.orElse(null);
            if ((value == null) || type.isInstance(value)) {
                return type.cast(value);
            }
            return null;
        }

        Object menuDefault = this.context.getMenu().getStateDefaults().get(key);
        if (type.isInstance(menuDefault)) {
            return type.cast(menuDefault);
        }

        if (PrimitiveDefaults.hasPrimitiveDefault(type)) {
            return PrimitiveDefaults.getDefault(type);
        }

        return null;
    }

    /**
     * Gets state with type checking and explicit default.
     * Overrides all automatic defaults.
     */
    public <T> T getState(@NonNull String key, @NonNull Class<T> type, @NonNull T defaultValue) {
        Optional<Object> optValue = this.customState.get(key);

        if (optValue != null) {
            Object value = optValue.orElse(null);
            if ((value == null) || type.isInstance(value)) {
                return type.cast(value);
            }
            return null;
        }

        return defaultValue;
    }

    /**
     * Gets state with TypeReference for complex generic types.
     * Useful for collections and maps with generic parameters.
     *
     * @param key           The state variable name
     * @param typeReference Type reference capturing generic type information
     * @param <T>           The value type
     * @return The value or appropriate default
     */
    public <T> T getState(@NonNull String key, @NonNull TypeReference<T> typeReference) {
        return this.getState(key, typeReference.getRawType());
    }

    /**
     * Gets state with TypeReference and explicit default.
     *
     * @param key           The state variable name
     * @param typeReference Type reference capturing generic type information
     * @param defaultValue  The default if not found
     * @param <T>           The value type
     * @return The value or provided default
     */
    public <T> T getState(@NonNull String key, @NonNull TypeReference<T> typeReference, @NonNull T defaultValue) {
        return this.getState(key, typeReference.getRawType(), defaultValue);
    }

    /**
     * Sets state (supports null values).
     * Marks viewer as dirty for update interval refresh.
     */
    public void setState(@NonNull String key, Object value) {
        this.customState.put(key, Optional.ofNullable(value));
        this.invalidate(); // Mark dirty when state changes
    }

    /**
     * Checks if state exists (in custom state, not defaults).
     */
    public boolean hasState(@NonNull String key) {
        return this.customState.containsKey(key);
    }

    /**
     * Removes state.
     */
    public void removeState(@NonNull String key) {
        this.customState.remove(key);
    }

    /**
     * Clears all custom state (not defaults).
     */
    public void clearState() {
        this.customState.clear();
    }
}
