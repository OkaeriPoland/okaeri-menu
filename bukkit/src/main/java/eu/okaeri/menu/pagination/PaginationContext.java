package eu.okaeri.menu.pagination;

import eu.okaeri.menu.Menu;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.entity.HumanEntity;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Manages pagination state for a specific pane and viewer.
 * Tracks current page, filters, and provides navigation methods.
 *
 * <p>Pagination contexts are now scoped to Menu instances (via ViewerState)
 * instead of global static storage. This prevents collisions between different
 * menu instances with the same pane names.
 */
@Getter
public class PaginationContext<T> {

    private final String paneId;
    private final UUID playerId;
    private final List<T> allItems;
    private final int itemsPerPage;

    private int currentPage = 0;
    private final Map<String, ItemFilter<T>> filters = new LinkedHashMap<>();
    private FilterStrategy filterStrategy = FilterStrategy.AND;  // Default to AND

    /**
     * Public constructor.
     * Called by Menu.ViewerState to create pagination contexts.
     */
    public PaginationContext(@NonNull String paneId, @NonNull UUID playerId, @NonNull List<T> allItems, int itemsPerPage) {
        this.paneId = paneId;
        this.playerId = playerId;
        this.allItems = new ArrayList<>(allItems);
        this.itemsPerPage = itemsPerPage;
    }

    /**
     * Gets or creates a pagination context for a pane and player in a specific menu.
     *
     * <p>Pagination contexts are now scoped to individual menu instances via ViewerState,
     * eliminating collisions between different menus with the same pane names.
     *
     * @param menu         The menu instance
     * @param paneId       The pane identifier
     * @param player       The player
     * @param allItems     All items to paginate
     * @param itemsPerPage Items per page
     * @param <T>          Item type
     * @return The pagination context
     * @throws IllegalArgumentException if menu is null
     * @throws IllegalStateException    if viewer state doesn't exist for player
     */
    @SuppressWarnings("unchecked")
    public static <T> @NonNull PaginationContext<T> get(@NonNull Menu menu, @NonNull String paneId, @NonNull HumanEntity player, @NonNull List<T> allItems, int itemsPerPage) {

        UUID playerId = player.getUniqueId();
        Menu.ViewerState state = menu.getViewerState(playerId);

        if (state == null) {
            throw new IllegalStateException("No viewer state for player " + player.getName() + " in this menu. Menu must be opened first.");
        }

        return state.getPaginationContext(paneId, playerId, allItems, itemsPerPage);
    }

    // ========================================
    // FILTERING
    // ========================================

    /**
     * Sets a filter using an ItemFilter instance.
     * Provides full programmatic control over filter behavior.
     *
     * @param filter The filter to add
     */
    @SuppressWarnings("unchecked")
    public void setFilter(@NonNull ItemFilter<?> filter) {
        String filterId = (filter.getFilterId() != null)
            ? filter.getFilterId()
            : ("filter-" + System.identityHashCode(filter));
        this.filters.put(filterId, (ItemFilter<T>) filter);
        this.currentPage = 0; // Reset to first page when filters change
    }

    /**
     * Adds or updates a predicate-only filter for in-memory filtering.
     *
     * @param filterId  The filter identifier
     * @param predicate The filter predicate
     */
    public void addFilter(@NonNull String filterId, @NonNull Predicate<T> predicate) {
        ItemFilter.Builder<T> builder = ItemFilter.<T>builder()
            .target(this.paneId)
            .id(filterId)
            .when(() -> true)  // Always active for programmatic filters
            .predicate(predicate);

        this.filters.put(filterId, builder.build());
        this.currentPage = 0; // Reset to first page when filters change
    }

    /**
     * Adds or updates a value-only filter for database-side filtering.
     * Use this for filters that should be extracted in LoaderContext but not applied in-memory.
     *
     * @param filterId The filter identifier
     * @param value    The filter value for database queries
     */
    public void addFilterValue(@NonNull String filterId, @NonNull Object value) {
        ItemFilter.Builder<T> builder = ItemFilter.<T>builder()
            .target(this.paneId)
            .id(filterId)
            .when(() -> true)  // Always active for programmatic filters
            .value(() -> value);

        this.filters.put(filterId, builder.build());
        this.currentPage = 0; // Reset to first page when filters change
    }

    /**
     * Adds or updates a hybrid filter with both predicate and value.
     * The predicate is used for in-memory filtering, the value for database queries.
     *
     * @param filterId  The filter identifier
     * @param predicate The filter predicate for in-memory filtering
     * @param value     The filter value for database queries
     */
    public void addFilter(@NonNull String filterId, @NonNull Predicate<T> predicate, @NonNull Object value) {
        ItemFilter.Builder<T> builder = ItemFilter.<T>builder()
            .target(this.paneId)
            .id(filterId)
            .when(() -> true)  // Always active for programmatic filters
            .predicate(predicate)
            .value(() -> value);

        this.filters.put(filterId, builder.build());
        this.currentPage = 0; // Reset to first page when filters change
    }

    /**
     * Removes a filter.
     *
     * @param filterId The filter identifier
     */
    public void removeFilter(@NonNull String filterId) {
        this.filters.remove(filterId);
        this.currentPage = 0; // Reset to first page when filters change
    }

    /**
     * Toggles a filter on/off.
     *
     * @param filterId  The filter identifier
     * @param predicate The filter predicate
     * @return true if filter is now active, false if removed
     */
    public boolean toggleFilter(@NonNull String filterId, @NonNull Predicate<T> predicate) {
        if (this.filters.containsKey(filterId)) {
            this.removeFilter(filterId);
            return false;
        } else {
            this.addFilter(filterId, predicate);
            return true;
        }
    }

    /**
     * Checks if a filter is active (present and when() returns true).
     *
     * @param filterId The filter identifier
     * @return true if active
     */
    public boolean hasFilter(@NonNull String filterId) {
        ItemFilter<T> filter = this.filters.get(filterId);
        return (filter != null) && filter.isActive();
    }

    /**
     * Gets all filters (for internal/advanced use).
     * Most code should use getActiveFilterCount() or hasFilter() instead.
     *
     * @return Unmodifiable map of all filters
     */
    @NonNull
    public Map<String, ItemFilter<T>> getFilters() {
        return Collections.unmodifiableMap(this.filters);
    }

    /**
     * Gets all currently active filters (when() returns true).
     * Provides easy iteration over active filters with access to filter metadata.
     *
     * <p>Example usage:
     * <pre>{@code
     * // Iterate over all active filters
     * for (ItemFilter<Offer> filter : pagination.getActiveFilters()) {
     *     String id = filter.getFilterId();
     *     Object value = filter.extractValue();
     *     // Use filter...
     * }
     *
     * // Check if any filters have values
     * boolean hasValueFilters = pagination.getActiveFilters().stream()
     *     .anyMatch(f -> f.extractValue() != null);
     * }</pre>
     *
     * @return Unmodifiable collection of active filters
     */
    @NonNull
    public Collection<ItemFilter<T>> getActiveFilters() {
        return this.filters.values().stream()
            .filter(ItemFilter::isActive)
            .toList();
    }

    /**
     * Gets the IDs of all currently active filters (when() returns true).
     * Useful for displaying filter counts or checking which filters are enabled.
     *
     * @return Unmodifiable set of active filter IDs
     */
    @NonNull
    public Set<String> getActiveFilterIds() {
        return this.filters.entrySet().stream()
            .filter(entry -> entry.getValue().isActive())
            .map(Map.Entry::getKey)
            .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Gets the number of currently active filters.
     *
     * @return Count of active filters
     */
    public int getActiveFilterCount() {
        return (int) this.filters.values().stream()
            .filter(ItemFilter::isActive)
            .count();
    }

    /**
     * Clears all filters.
     */
    public void clearFilters() {
        this.filters.clear();
        this.currentPage = 0;
    }

    /**
     * Sets the filter combination strategy.
     *
     * @param strategy The filter strategy (AND or OR)
     */
    public void setFilterStrategy(FilterStrategy strategy) {
        this.filterStrategy = (strategy != null) ? strategy : FilterStrategy.AND;
    }

    /**
     * Gets the current filter strategy.
     *
     * @return The filter strategy
     */
    public @NonNull FilterStrategy getFilterStrategy() {
        return this.filterStrategy;
    }

    /**
     * Gets all active filter values (lazily extracted from active filters).
     * Used for creating LoaderContext for database-side filtering.
     *
     * @return Unmodifiable map of filter IDs to their values
     */
    public @NonNull Map<String, Object> getActiveFilterValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Map.Entry<String, ItemFilter<T>> entry : this.filters.entrySet()) {
            ItemFilter<T> filter = entry.getValue();
            if (filter.isActive()) {  // Lazy when() check
                Object value = filter.extractValue();  // Lazy value extraction
                if (value != null) {
                    values.put(entry.getKey(), value);
                }
            }
        }
        return Collections.unmodifiableMap(values);
    }

    /**
     * Gets a specific filter value with type casting (lazily extracted).
     *
     * @param filterId The filter ID
     * @param type     The expected value type
     * @param <V>      The type to cast to
     * @return Optional containing the filter value, or empty if not present or wrong type
     */
    @SuppressWarnings("unchecked")
    public <V> @NonNull Optional<V> getFilterValue(@NonNull String filterId, @NonNull Class<V> type) {
        ItemFilter<T> filter = this.filters.get(filterId);
        if (filter == null) {
            return Optional.empty();
        }
        if (!filter.isActive()) {  // Lazy when() check
            return Optional.empty();
        }

        Object value = filter.extractValue();  // Lazy value extraction
        if (value == null) {
            return Optional.empty();
        }
        if (!type.isInstance(value)) {
            return Optional.empty();
        }
        return Optional.of((V) value);
    }

    /**
     * Gets filtered items (all items that pass active filters).
     * Uses the configured filter strategy to combine multiple filters.
     * Filters are lazily evaluated (when() and predicate() called on each item).
     *
     * <p>The strategy is responsible for checking {@link ItemFilter#isActive()}
     * and combining filters according to its logic.
     *
     * @return List of filtered items
     */
    public @NonNull List<T> getFilteredItems() {
        if (this.filters.isEmpty()) {
            return new ArrayList<>(this.allItems);
        }

        // Pass ALL filters to strategy - strategy handles isActive() checking
        // This allows strategies to inspect all filters for metadata-based logic
        Predicate<T> combinedFilter = this.filterStrategy.combine(this.filters.values());

        return this.allItems.stream()
            .filter(combinedFilter)
            .toList();
    }

    // ========================================
    // PAGINATION
    // ========================================

    /**
     * Gets items for the current page.
     *
     * @return List of items on current page
     */
    public @NonNull List<T> getCurrentPageItems() {
        List<T> filtered = this.getFilteredItems();
        int start = this.currentPage * this.itemsPerPage;
        int end = Math.min(start + this.itemsPerPage, filtered.size());

        if (start >= filtered.size()) {
            return Collections.emptyList();
        }

        return filtered.subList(start, end);
    }

    /**
     * Gets the total number of pages.
     *
     * @return Total pages
     */
    public int getTotalPages() {
        List<T> filtered = this.getFilteredItems();
        return (int) Math.ceil((double) filtered.size() / this.itemsPerPage);
    }

    /**
     * Gets the current page number (0-indexed).
     *
     * @return Current page
     */
    public int getCurrentPage() {
        return this.currentPage;
    }

    /**
     * Sets the current page.
     *
     * @param page The page number (0-indexed)
     * @return true if page was changed
     */
    public boolean setPage(int page) {
        int totalPages = this.getTotalPages();
        if ((page < 0) || (page >= totalPages)) {
            return false;
        }
        this.currentPage = page;
        return true;
    }

    /**
     * Goes to the next page.
     *
     * @return true if moved to next page
     */
    public boolean nextPage() {
        return this.setPage(this.currentPage + 1);
    }

    /**
     * Goes to the previous page.
     *
     * @return true if moved to previous page
     */
    public boolean previousPage() {
        return this.setPage(this.currentPage - 1);
    }

    /**
     * Goes to the first page.
     */
    public void firstPage() {
        this.currentPage = 0;
    }

    /**
     * Goes to the last page.
     */
    public void lastPage() {
        this.currentPage = Math.max(0, this.getTotalPages() - 1);
    }

    /**
     * Checks if there is a next page.
     *
     * @return true if there is a next page
     */
    public boolean hasNext() {
        return this.currentPage < (this.getTotalPages() - 1);
    }

    /**
     * Checks if there is a previous page.
     *
     * @return true if there is a previous page
     */
    public boolean hasPrevious() {
        return this.currentPage > 0;
    }

    /**
     * Gets the total number of filtered items.
     *
     * @return Total filtered items
     */
    public int getTotalItems() {
        return this.getFilteredItems().size();
    }

    /**
     * Checks if the current page is empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return this.getFilteredItems().isEmpty();
    }

    /**
     * Updates the items list (useful for dynamic content).
     *
     * @param newItems The new items
     */
    public void updateItems(@NonNull List<T> newItems) {
        this.allItems.clear();
        this.allItems.addAll(newItems);

        // Adjust current page if it's now out of bounds
        int totalPages = this.getTotalPages();
        if ((this.currentPage >= totalPages) && (totalPages > 0)) {
            this.currentPage = totalPages - 1;
        }
    }
}
