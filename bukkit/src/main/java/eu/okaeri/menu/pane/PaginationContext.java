package eu.okaeri.menu.pane;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.MenuContext;
import eu.okaeri.menu.pane.pagination.FilterStrategy;
import eu.okaeri.menu.pane.pagination.ItemFilter;
import eu.okaeri.menu.state.ViewerState;
import lombok.NonNull;

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
public class PaginationContext<T> {

    private MenuContext context;
    private final PaginatedPane<T> pane;

    private int currentPage = 0;
    private final Map<String, ItemFilter<T>> filters = new LinkedHashMap<>();
    private FilterStrategy filterStrategy = FilterStrategy.AND;  // Default to AND

    /**
     * Creates a reactive pagination context.
     * Stores reference to pane and reads data on-demand.
     *
     * @param context The menu context
     * @param pane    The paginated pane (source of truth for items)
     */
    public PaginationContext(@NonNull MenuContext context, @NonNull PaginatedPane<T> pane) {
        this.context = context;
        this.pane = pane;
    }

    /**
     * Gets the underlying pane reference.
     *
     * @return The paginated pane
     */
    @NonNull
    public PaginatedPane<T> getPane() {
        return this.pane;
    }

    /**
     * Gets the pane identifier (derived from pane).
     *
     * @return The pane ID
     */
    @NonNull
    public String getPaneId() {
        return this.pane.getName();
    }

    /**
     * Gets the menu context associated with this pagination.
     *
     * @return The menu context
     */
    @NonNull
    public MenuContext getMenuContext() {
        return this.context;
    }

    /**
     * Sets the current context for this pagination.
     * Called automatically when pagination is accessed via MenuContext.
     *
     * @param context The menu context
     */
    public void setContext(@NonNull MenuContext context) {
        this.context = context;
    }

    /**
     * Marks the viewer's state as dirty for update interval refresh.
     * Called automatically when pagination state changes.
     */
    private void invalidate() {
        if (this.context != null) {
            ViewerState state = this.context.getMenu().getViewerState(this.context.getEntity().getUniqueId());
            if (state == null) {
                return;
            }
            state.invalidate();
        }
    }

    /**
     * Gets all items (reactive - reads from pane on-demand).
     * For AsyncPaginatedPane, this reads from the current AtomicReference value.
     *
     * @return All items from the pane's supplier
     */
    @NonNull
    public List<T> getAllItems() {
        return this.pane.getItemsSupplier().get();
    }

    /**
     * Gets items per page (reactive - reads from pane on-demand).
     *
     * @return Items per page configured on the pane
     */
    public int getItemsPerPage() {
        return this.pane.getItemsPerPage();
    }

    /**
     * Gets or creates a pagination context for a pane by name.
     * Automatically extracts the pane from the menu and delegates to the pane-based overload.
     *
     * @param context The menu context (contains menu and entity)
     * @param paneId  The pane identifier
     * @param <T>     Item type
     * @return The pagination context with context already set
     * @throws IllegalArgumentException if pane doesn't exist or is not a PaginatedPane
     * @throws IllegalStateException    if viewer state doesn't exist for player
     */
    @SuppressWarnings("unchecked")
    public static <T> @NonNull PaginationContext<T> get(@NonNull MenuContext context, @NonNull String paneId) {

        // Extract pane and validate type
        Pane pane = context.getMenu().getPane(paneId);
        if (pane == null) {
            throw new IllegalArgumentException("Pane '" + paneId + "' does not exist in menu");
        }
        if (!(pane instanceof PaginatedPane<?> paginatedPane)) {
            throw new IllegalArgumentException("Pane '" + paneId + "' is not a PaginatedPane");
        }

        // Cast to specific type parameter (unchecked due to type erasure)
        PaginatedPane<T> typedPane = (PaginatedPane<T>) paginatedPane;

        // Delegate to pane-based overload
        return get(context, typedPane);
    }

    /**
     * Gets or creates a pagination context for a pane.
     * <p>
     * The pagination context is cached per-viewer in ViewerState.
     * The context reactively reads items from the pane's supplier on-demand,
     * working seamlessly with AsyncPaginatedPane's AtomicReference pattern.
     *
     * @param context The menu context (contains menu and entity)
     * @param pane    The paginated pane
     * @param <T>     Item type
     * @return The pagination context with context already set
     * @throws IllegalStateException if viewer state doesn't exist for player
     */
    @SuppressWarnings("unchecked")
    public static <T> @NonNull PaginationContext<T> get(@NonNull MenuContext context, @NonNull PaginatedPane<T> pane) {

        Menu menu = context.getMenu();
        UUID playerId = context.getEntity().getUniqueId();
        ViewerState state = menu.getViewerState(playerId);

        if (state == null) {
            throw new IllegalStateException("No viewer state for player " + context.getEntity().getName() + " in this menu. Menu must be opened first.");
        }

        // Get or create cached pagination context
        return state.getPagination(pane);
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
        this.invalidate();
    }

    /**
     * Adds or updates a predicate-only filter for in-memory filtering.
     *
     * @param filterId  The filter identifier
     * @param predicate The filter predicate
     */
    public void addFilter(@NonNull String filterId, @NonNull Predicate<T> predicate) {
        ItemFilter.Builder<T> builder = ItemFilter.<T>builder()
            .target(this.getPaneId())
            .id(filterId)
            .when(ctx -> true)  // Always active for programmatic filters
            .predicate(predicate);

        this.filters.put(filterId, builder.build());
        this.currentPage = 0; // Reset to first page when filters change
        this.invalidate();
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
            .target(this.getPaneId())
            .id(filterId)
            .when(ctx -> true)  // Always active for programmatic filters
            .value(() -> value);

        this.filters.put(filterId, builder.build());
        this.currentPage = 0; // Reset to first page when filters change
        this.invalidate();
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
            .target(this.getPaneId())
            .id(filterId)
            .when(ctx -> true)  // Always active for programmatic filters
            .predicate(predicate)
            .value(() -> value);

        this.filters.put(filterId, builder.build());
        this.currentPage = 0; // Reset to first page when filters change
        this.invalidate();
    }

    /**
     * Removes a filter.
     *
     * @param filterId The filter identifier
     */
    public void removeFilter(@NonNull String filterId) {
        this.filters.remove(filterId);
        this.currentPage = 0; // Reset to first page when filters change
        this.invalidate();
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
        return (filter != null) && filter.isActive(this.context);
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
            .filter(f -> f.isActive(this.context))
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
            .filter(entry -> entry.getValue().isActive(this.context))
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
            .filter(f -> f.isActive(this.context))
            .count();
    }

    /**
     * Clears all filters.
     */
    public void clearFilters() {
        this.filters.clear();
        this.currentPage = 0;
        this.invalidate();
    }

    /**
     * Sets the filter combination strategy.
     *
     * @param strategy The filter strategy (AND or OR)
     */
    public void setFilterStrategy(FilterStrategy strategy) {
        this.filterStrategy = (strategy != null) ? strategy : FilterStrategy.AND;
        this.invalidate();
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
            if (filter.isActive(this.context)) {  // Lazy when() check
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
        if (!filter.isActive(this.context)) {  // Lazy when() check
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
     * <p>The strategy is responsible for checking {@link ItemFilter#isActive(MenuContext)}
     * and combining filters according to its logic.
     *
     * @return List of filtered items
     */
    public @NonNull List<T> getFilteredItems() {
        List<T> allItems = this.getAllItems();  // Reactive read from pane

        if (this.filters.isEmpty()) {
            return new ArrayList<>(allItems);
        }

        // Pass context and ALL filters to strategy - strategy handles isActive() checking
        // This allows strategies to inspect all filters for metadata-based logic
        Predicate<T> combinedFilter = this.filterStrategy.combine(this.context, this.filters.values());

        return allItems.stream()
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
        int itemsPerPage = this.getItemsPerPage();  // Reactive read from pane
        int start = this.currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, filtered.size());

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
        int itemsPerPage = this.getItemsPerPage();  // Reactive read from pane
        return (int) Math.ceil((double) filtered.size() / itemsPerPage);
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
        this.invalidate();
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
        this.invalidate();
    }

    /**
     * Goes to the last page.
     */
    public void lastPage() {
        this.currentPage = Math.max(0, this.getTotalPages() - 1);
        this.invalidate();
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
        List<T> filtered = this.getFilteredItems();
        return filtered.size();
    }

    /**
     * Checks if the filtered items list is empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        List<T> filtered = this.getFilteredItems();
        return filtered.isEmpty();
    }
}
