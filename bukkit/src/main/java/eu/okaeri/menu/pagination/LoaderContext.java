package eu.okaeri.menu.pagination;

import lombok.Getter;
import lombok.NonNull;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Context provided to async paginated pane loaders.
 * Contains pagination state and active filter values for database queries.
 *
 * <p>Example usage:
 * <pre>{@code
 * AsyncPaginatedPane.paneAsync(Offer.class)
 *     .loader(ctx -> {
 *         // Extract filter values for database query
 *         UUID sellerId = ctx.getFilter("seller", UUID.class).orElse(null);
 *         String category = ctx.getFilter("category", String.class).orElse("ALL");
 *
 *         // Build database query with filters
 *         return offerRepository.find(q -> q
 *             .where(and(
 *                 on("seller", sellerId == null ? ne("") : eq(sellerId)),
 *                 on("category", category.equals("ALL") ? ne("") : eq(category))
 *             ))
 *             .skip(ctx.getCurrentPage() * ctx.getPageSize())
 *             .limit(ctx.getPageSize() + 1))  // N+1 pattern
 *             .toList();
 *     })
 *     .build();
 * }</pre>
 */
@Getter
public class LoaderContext {

    private final int currentPage;
    private final int pageSize;
    private final Map<String, Object> activeFilters;

    /**
     * Creates a new LoaderContext.
     *
     * @param currentPage   The current page number (0-indexed)
     * @param pageSize      The number of items per page
     * @param activeFilters Map of active filter IDs to their values
     */
    public LoaderContext(int currentPage, int pageSize, @NonNull Map<String, Object> activeFilters) {
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.activeFilters = Collections.unmodifiableMap(activeFilters);
    }

    /**
     * Gets a filter value by ID with type casting.
     *
     * @param filterId The filter ID
     * @param type     The expected value type
     * @param <T>      The type to cast to
     * @return Optional containing the filter value, or empty if not present or wrong type
     */
    @SuppressWarnings("unchecked")
    public <T> @NonNull Optional<T> getFilter(@NonNull String filterId, @NonNull Class<T> type) {
        Object value = this.activeFilters.get(filterId);
        if (value == null) {
            return Optional.empty();
        }
        if (!type.isInstance(value)) {
            return Optional.empty();
        }
        return Optional.of((T) value);
    }

    /**
     * Checks if a filter is active.
     *
     * @param filterId The filter ID
     * @return true if the filter is active
     */
    public boolean hasFilter(@NonNull String filterId) {
        return this.activeFilters.containsKey(filterId);
    }

    /**
     * Gets the number of active filters.
     *
     * @return Count of active filters
     */
    public int getActiveFilterCount() {
        return this.activeFilters.size();
    }
}
