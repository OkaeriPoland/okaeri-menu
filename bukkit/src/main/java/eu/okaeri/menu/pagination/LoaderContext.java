package eu.okaeri.menu.pagination;

import lombok.Getter;
import lombok.NonNull;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Context provided to async paginated pane loaders.
 * Provides access to pagination state and filter values for database queries.
 *
 * <p>LoaderContext delegates to the underlying {@link PaginationContext} and provides
 * convenient access to pagination state and filter values. Advanced use cases can
 * access the full pagination context via {@link #getPagination()}.
 *
 * <p>Example usage:
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
 * <p>Advanced example with full pagination access:
 * <pre>{@code
 * .loader(ctx -> {
 *     // Access full pagination context for advanced features
 *     PaginationContext<?> pagination = ctx.getPagination();
 *
 *     // Check pagination state
 *     if (ctx.hasNext()) {
 *         // Could preload next page
 *     }
 *
 *     // Get all active filter IDs
 *     Set<String> filterIds = ctx.getActiveFilterIds();
 *
 *     return loadData(ctx);
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
}
