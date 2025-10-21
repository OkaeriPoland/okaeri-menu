package eu.okaeri.menu.pagination;

import eu.okaeri.menu.Menu;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.entity.HumanEntity;

import java.util.*;
import java.util.function.Predicate;

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
    private final Map<String, Predicate<T>> activeFilters = new LinkedHashMap<>();
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
        if (menu == null) {
            throw new IllegalArgumentException("Menu cannot be null");
        }

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
     * Adds or updates a filter.
     *
     * @param filterId  The filter identifier
     * @param predicate The filter predicate
     */
    public void addFilter(@NonNull String filterId, @NonNull Predicate<T> predicate) {
        this.activeFilters.put(filterId, predicate);
        this.currentPage = 0; // Reset to first page when filters change
    }

    /**
     * Removes a filter.
     *
     * @param filterId The filter identifier
     */
    public void removeFilter(@NonNull String filterId) {
        this.activeFilters.remove(filterId);
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
        if (this.activeFilters.containsKey(filterId)) {
            this.removeFilter(filterId);
            return false;
        } else {
            this.addFilter(filterId, predicate);
            return true;
        }
    }

    /**
     * Checks if a filter is active.
     *
     * @param filterId The filter identifier
     * @return true if active
     */
    public boolean hasFilter(@NonNull String filterId) {
        return this.activeFilters.containsKey(filterId);
    }

    /**
     * Clears all filters.
     */
    public void clearFilters() {
        this.activeFilters.clear();
        this.currentPage = 0;
    }

    /**
     * Clears filters with a specific prefix.
     * Useful for separating declarative and programmatic filters.
     *
     * @param prefix The filter ID prefix
     */
    public void clearFiltersWithPrefix(@NonNull String prefix) {
        this.activeFilters.keySet().removeIf(filterId -> filterId.startsWith(prefix));
        if (!this.activeFilters.isEmpty()) {
            this.currentPage = 0; // Reset to first page if filters were removed
        }
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
     * Gets filtered items (all items that pass active filters).
     * Uses the configured filter strategy to combine multiple filters.
     *
     * @return List of filtered items
     */
    public @NonNull List<T> getFilteredItems() {
        if (this.activeFilters.isEmpty()) {
            return new ArrayList<>(this.allItems);
        }

        // Combine all filters using the strategy
        Predicate<T> combinedFilter = this.filterStrategy.combine(this.activeFilters.values());

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
