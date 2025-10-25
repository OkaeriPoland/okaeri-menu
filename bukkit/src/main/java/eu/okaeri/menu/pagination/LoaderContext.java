package eu.okaeri.menu.pagination;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.MenuContext;
import eu.okaeri.menu.state.TypeReference;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Logger;

/**
 * Context provided to async paginated pane loaders.
 * Provides access to pagination state, filter values, player information, and per-player state.
 *
 * <p>LoaderContext delegates to the underlying {@link PaginationContext} and {@link MenuContext}
 * to provide convenient access to all loader-relevant information. Advanced use cases can
 * access the full pagination context via {@link #getPagination()}.
 *
 * <p><b>Basic example with filters and pagination:</b>
 * <pre>{@code
 * AsyncPaginatedPane.paneAsync(Offer.class)
 *     .loader(ctx -> {
 *         // Extract filter values for database query
 *         UUID sellerId = ctx.getFilter("seller", UUID.class).orElse(null);
 *         String category = ctx.getFilter("category", String.class).orElse("ALL");
 *
 *         // Access pagination info
 *         int page = ctx.getCurrentPage();
 *         int size = ctx.getPageSize();
 *
 *         // Build database query with filters
 *         return offerRepository.find(q -> q
 *             .where(and(
 *                 on("seller", sellerId == null ? ne("") : eq(sellerId)),
 *                 on("category", category.equals("ALL") ? ne("") : eq(category))
 *             ))
 *             .skip(page * size)
 *             .limit(size + 1))  // N+1 pattern
 *             .toList();
 *     })
 *     .build();
 * }</pre>
 *
 * <p><b>Player-specific data loading:</b>
 * <pre>{@code
 * .loader(ctx -> {
 *     // Access player viewing the menu
 *     Player player = ctx.getPlayer();
 *     UUID playerId = player.getUniqueId();
 *
 *     // Load player's personal items
 *     return database.getPlayerItems(playerId, ctx.getCurrentPage());
 * })
 * }</pre>
 *
 * <p><b>Using per-player state:</b>
 * <pre>{@code
 * .loader(ctx -> {
 *     // Read per-player preferences
 *     String sortOrder = ctx.getString("sortOrder", "newest");
 *     boolean showHidden = ctx.getBool("showHidden", false);
 *
 *     // Cache intermediate results
 *     ctx.set("lastLoadTime", System.currentTimeMillis());
 *
 *     // Load with preferences
 *     return database.find(sortOrder, showHidden, ctx.getCurrentPage());
 * })
 * }</pre>
 *
 * <p><b>Multi-pane coordination:</b>
 * <pre>{@code
 * .loader(ctx -> {
 *     // Access filters from another pane
 *     PaginationContext<?> mainPane = ctx.pagination("mainPane");
 *     String searchQuery = mainPane.getFilterValue("search", String.class).orElse("");
 *
 *     // Load related data based on other pane's state
 *     return database.findRelated(searchQuery, ctx.getCurrentPage());
 * })
 * }</pre>
 */
@Getter
public class LoaderContext {

    private final PaginationContext<?> pagination;

    /**
     * Creates a LoaderContext from a PaginationContext.
     * This is the recommended way to create LoaderContext instances.
     *
     * @param pagination The pagination context
     * @return A new LoaderContext wrapping the pagination context
     */
    public static @NonNull LoaderContext from(@NonNull PaginationContext<?> pagination) {
        return new LoaderContext(pagination);
    }

    /**
     * Creates a LoaderContext wrapping a PaginationContext.
     *
     * @param pagination The pagination context to wrap
     */
    public LoaderContext(@NonNull PaginationContext<?> pagination) {
        this.pagination = pagination;
    }

    // ========================================
    // PAGINATION STATE
    // ========================================

    /**
     * Gets the current page number (0-indexed).
     *
     * @return The current page
     */
    public int getCurrentPage() {
        return this.pagination.getCurrentPage();
    }

    /**
     * Gets the number of items per page.
     *
     * @return Items per page
     */
    public int getPageSize() {
        return this.pagination.getItemsPerPage();
    }

    /**
     * Gets the total number of pages.
     *
     * @return Total pages
     */
    public int getTotalPages() {
        return this.pagination.getTotalPages();
    }

    /**
     * Checks if there is a next page.
     *
     * @return true if there is a next page
     */
    public boolean hasNext() {
        return this.pagination.hasNext();
    }

    /**
     * Checks if there is a previous page.
     *
     * @return true if there is a previous page
     */
    public boolean hasPrevious() {
        return this.pagination.hasPrevious();
    }

    /**
     * Gets the total number of filtered items.
     *
     * @return Total filtered items
     */
    public int getTotalItems() {
        return this.pagination.getTotalItems();
    }

    // ========================================
    // FILTER ACCESS
    // ========================================

    /**
     * Gets a filter value by ID with type casting.
     *
     * @param filterId The filter ID
     * @param type     The expected value type
     * @param <T>      The type to cast to
     * @return Optional containing the filter value, or empty if not present or wrong type
     */
    public <T> @NonNull Optional<T> getFilter(@NonNull String filterId, @NonNull Class<T> type) {
        return this.pagination.getFilterValue(filterId, type);
    }

    /**
     * Checks if a filter is active.
     *
     * @param filterId The filter ID
     * @return true if the filter is active
     */
    public boolean hasFilter(@NonNull String filterId) {
        return this.pagination.hasFilter(filterId);
    }

    /**
     * Gets the number of active filters.
     *
     * @return Count of active filters
     */
    public int getActiveFilterCount() {
        return this.pagination.getActiveFilterCount();
    }

    /**
     * Gets the IDs of all currently active filters.
     *
     * @return Unmodifiable set of active filter IDs
     */
    public @NonNull Set<String> getActiveFilterIds() {
        return this.pagination.getActiveFilterIds();
    }

    /**
     * Gets all currently active filters (when() returns true).
     * Provides easy iteration over active filters with access to filter metadata.
     *
     * <p>This is useful for inspecting filter values, checking filter types,
     * or building complex database queries based on filter metadata.
     *
     * <p>Example usage:
     * <pre>{@code
     * // Iterate over all active filters
     * for (ItemFilter<?> filter : ctx.getActiveFilters()) {
     *     String id = filter.getFilterId();
     *     Object value = filter.extractValue();
     *     // Build query based on filter...
     * }
     *
     * // Check if any filters have values
     * boolean hasValueFilters = ctx.getActiveFilters().stream()
     *     .anyMatch(f -> f.extractValue() != null);
     * }</pre>
     *
     * @return Unmodifiable collection of active filters
     */
    @SuppressWarnings("unchecked")
    public @NonNull Collection<ItemFilter<?>> getActiveFilters() {
        return (Collection<ItemFilter<?>>) (Collection<?>) this.pagination.getActiveFilters();
    }

    // ========================================
    // ENTITY / PLAYER ACCESS
    // ========================================

    /**
     * Gets the entity viewing this menu.
     * Useful for permission checks, querying player-specific data, etc.
     *
     * @return The viewer entity
     */
    @NonNull
    public HumanEntity getEntity() {
        return this.pagination.getMenuContext().getEntity();
    }

    /**
     * Gets the player viewing this menu (cast from HumanEntity).
     * Convenience method for player-specific operations.
     *
     * @return The viewer player
     */
    @NonNull
    public Player getPlayer() {
        return this.pagination.getMenuContext().getPlayer();
    }

    // ========================================
    // MENU METADATA
    // ========================================

    /**
     * Gets the menu instance.
     * Useful for accessing menu configuration and state.
     *
     * @return The menu
     */
    @NonNull
    public Menu getMenu() {
        return this.pagination.getMenuContext().getMenu();
    }

    /**
     * Gets the plugin's logger.
     * Convenience method for logging during data loading.
     *
     * @return The plugin logger
     */
    @NonNull
    public Logger getLogger() {
        return this.pagination.getMenuContext().getLogger();
    }

    // ========================================
    // PER-PLAYER STATE
    // ========================================

    /**
     * Gets a per-player state variable with type checking.
     *
     * @param key  The state variable name
     * @param type The expected type class
     * @param <T>  The value type
     * @return The value or appropriate default
     */
    public <T> T get(@NonNull String key, @NonNull Class<T> type) {
        return this.pagination.getMenuContext().get(key, type);
    }

    /**
     * Gets a per-player state variable with explicit default.
     *
     * @param key          The state variable name
     * @param type         The expected type class
     * @param defaultValue The default if not found
     * @param <T>          The value type
     * @return The value or provided default
     */
    public <T> T get(@NonNull String key, @NonNull Class<T> type, @NonNull T defaultValue) {
        return this.pagination.getMenuContext().get(key, type, defaultValue);
    }

    /**
     * Checks if a state variable is explicitly set.
     *
     * @param key The state variable name
     * @return true if explicitly set in this player's state
     */
    public boolean has(@NonNull String key) {
        return this.pagination.getMenuContext().has(key);
    }

    /**
     * Gets a string state variable.
     *
     * @param key The state variable name
     * @return The value or null
     */
    public String getString(@NonNull String key) {
        return this.pagination.getMenuContext().getString(key);
    }

    /**
     * Gets a string state variable with default.
     *
     * @param key          The state variable name
     * @param defaultValue The default value
     * @return The value or default
     */
    public String getString(@NonNull String key, String defaultValue) {
        return this.pagination.getMenuContext().getString(key, defaultValue);
    }

    /**
     * Gets an integer state variable (primitive).
     *
     * @param key The state variable name
     * @return The value or 0
     */
    public int getInt(@NonNull String key) {
        return this.pagination.getMenuContext().getInt(key);
    }

    /**
     * Gets an integer state variable with default (primitive).
     *
     * @param key          The state variable name
     * @param defaultValue The default value
     * @return The value or default
     */
    public int getInt(@NonNull String key, int defaultValue) {
        return this.pagination.getMenuContext().getInt(key, defaultValue);
    }

    /**
     * Gets a boolean state variable (primitive).
     *
     * @param key The state variable name
     * @return The value or false
     */
    public boolean getBool(@NonNull String key) {
        return this.pagination.getMenuContext().getBool(key);
    }

    /**
     * Gets a boolean state variable with default (primitive).
     *
     * @param key          The state variable name
     * @param defaultValue The default value
     * @return The value or default
     */
    public boolean getBool(@NonNull String key, boolean defaultValue) {
        return this.pagination.getMenuContext().getBool(key, defaultValue);
    }

    /**
     * Gets a long state variable (wrapper).
     *
     * @param key The state variable name
     * @return The value or null
     */
    public Long getLong(@NonNull String key) {
        return this.pagination.getMenuContext().getLong(key);
    }

    /**
     * Gets a long state variable with default.
     *
     * @param key          The state variable name
     * @param defaultValue The default value
     * @return The value or default
     */
    public Long getLong(@NonNull String key, Long defaultValue) {
        return this.pagination.getMenuContext().getLong(key, defaultValue);
    }

    /**
     * Gets a double state variable (wrapper).
     *
     * @param key The state variable name
     * @return The value or null
     */
    public Double getDouble(@NonNull String key) {
        return this.pagination.getMenuContext().getDouble(key);
    }

    /**
     * Gets a double state variable with default.
     *
     * @param key          The state variable name
     * @param defaultValue The default value
     * @return The value or default
     */
    public Double getDouble(@NonNull String key, Double defaultValue) {
        return this.pagination.getMenuContext().getDouble(key, defaultValue);
    }

    /**
     * Gets a float state variable (wrapper).
     *
     * @param key The state variable name
     * @return The value or null
     */
    public Float getFloat(@NonNull String key) {
        return this.pagination.getMenuContext().getFloat(key);
    }

    /**
     * Gets a float state variable with default.
     *
     * @param key          The state variable name
     * @param defaultValue The default value
     * @return The value or default
     */
    public Float getFloat(@NonNull String key, Float defaultValue) {
        return this.pagination.getMenuContext().getFloat(key, defaultValue);
    }

    /**
     * Gets a map state variable.
     *
     * @param key The state variable name
     * @param <K> Key type
     * @param <V> Value type
     * @return The map or null
     */
    public <K, V> Map<K, V> getMap(@NonNull String key) {
        return this.pagination.getMenuContext().getMap(key);
    }

    /**
     * Gets a map state variable with default.
     *
     * @param key          The state variable name
     * @param defaultValue The default value
     * @param <K>          Key type
     * @param <V>          Value type
     * @return The map or default
     */
    public <K, V> Map<K, V> getMap(@NonNull String key, Map<K, V> defaultValue) {
        return this.pagination.getMenuContext().getMap(key, defaultValue);
    }

    /**
     * Gets a list state variable.
     *
     * @param key The state variable name
     * @param <T> Element type
     * @return The list or null
     */
    public <T> List<T> getList(@NonNull String key) {
        return this.pagination.getMenuContext().getList(key);
    }

    /**
     * Gets a list state variable with default.
     *
     * @param key          The state variable name
     * @param defaultValue The default value
     * @param <T>          Element type
     * @return The list or default
     */
    public <T> List<T> getList(@NonNull String key, List<T> defaultValue) {
        return this.pagination.getMenuContext().getList(key, defaultValue);
    }

    /**
     * Gets state with TypeReference for complex generic types.
     *
     * @param key           The state variable name
     * @param typeReference Type reference capturing generic type information
     * @param <T>           The value type
     * @return The value or appropriate default
     */
    public <T> T get(@NonNull String key, @NonNull TypeReference<T> typeReference) {
        return this.pagination.getMenuContext().get(key, typeReference);
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
    public <T> T get(@NonNull String key, @NonNull TypeReference<T> typeReference, @NonNull T defaultValue) {
        return this.pagination.getMenuContext().get(key, typeReference, defaultValue);
    }

    // ========================================
    // PER-PLAYER STATE - MUTATIONS
    // ========================================

    /**
     * Sets a per-player state variable.
     * Supports null values to explicitly override defaults.
     *
     * <p>Useful for caching intermediate results or tracking load state.
     *
     * @param key   The state variable name
     * @param value The value to store (null is allowed)
     */
    public void set(@NonNull String key, Object value) {
        this.pagination.getMenuContext().set(key, value);
    }

    /**
     * Removes a per-player state variable.
     * Useful for invalidating cached data.
     *
     * @param key The state variable name
     */
    public void remove(@NonNull String key) {
        this.pagination.getMenuContext().remove(key);
    }

    // ========================================
    // MULTI-PANE ACCESS
    // ========================================

    /**
     * Gets the pagination context for another pane.
     * Useful for accessing filters or state from other panes.
     *
     * @param paneName The pane name
     * @param <T>      The item type
     * @return The pagination context for the pane
     */
    public <T> @NonNull PaginationContext<T> pagination(@NonNull String paneName) {
        return this.pagination.getMenuContext().pagination(paneName);
    }
}
