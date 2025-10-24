package eu.okaeri.menu.state;

import eu.okaeri.menu.MenuContext;
import eu.okaeri.menu.async.AsyncCache;
import eu.okaeri.menu.async.AsyncExecutor;
import eu.okaeri.menu.pagination.PaginationContext;
import eu.okaeri.menu.pane.PaginatedPane;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.inventory.Inventory;

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
    private @NonNull Inventory inventory;

    private @NonNull final AsyncCache asyncCache;
    private @NonNull final Map<String, PaginationContext<?>> paginationContexts = new ConcurrentHashMap<>();
    private @NonNull final Map<String, Optional<Object>> customState = new ConcurrentHashMap<>();

    public ViewerState(@NonNull MenuContext context, @NonNull Inventory inventory, @NonNull AsyncExecutor asyncExecutor) {
        this.context = context;
        this.inventory = inventory;
        this.asyncCache = new AsyncCache(asyncExecutor);
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
     */
    public void setState(@NonNull String key, Object value) {
        this.customState.put(key, Optional.ofNullable(value));
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
