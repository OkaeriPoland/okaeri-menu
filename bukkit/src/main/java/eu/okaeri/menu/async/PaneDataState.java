package eu.okaeri.menu.async;

import eu.okaeri.menu.pagination.PaginationContext;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Provides filtered and paginated view of async pane data.
 * Accessible via ctx.paneData(paneName, itemType).
 */
@Getter
@RequiredArgsConstructor
public class PaneDataState<T> {

    @NonNull
    private final List<T> allItems;
    @NonNull
    private final PaginationContext<T> paginationContext;

    /**
     * Gets all items from async loader (before filtering).
     *
     * @return All items
     */
    @NonNull
    public List<T> all() {
        return this.allItems;
    }

    /**
     * Gets items after filters applied.
     *
     * @return Filtered items
     */
    @NonNull
    public List<T> filtered() {
        return this.paginationContext.getFilteredItems();
    }

    /**
     * Gets items on current page.
     *
     * @return Current page items
     */
    @NonNull
    public List<T> currentPage() {
        return this.paginationContext.getCurrentPageItems();
    }

    /**
     * Gets total item count (before filtering).
     *
     * @return Total count
     */
    public int total() {
        return this.allItems.size();
    }

    /**
     * Gets filtered item count.
     *
     * @return Filtered count
     */
    public int filteredCount() {
        return this.paginationContext.getFilteredItems().size();
    }

    /**
     * Gets current page number (0-indexed).
     *
     * @return Current page
     */
    public int page() {
        return this.paginationContext.getCurrentPage();
    }

    /**
     * Gets total page count.
     *
     * @return Total pages
     */
    public int pages() {
        return this.paginationContext.getTotalPages();
    }

    /**
     * Gets items per page.
     *
     * @return Items per page
     */
    public int itemsPerPage() {
        return this.paginationContext.getItemsPerPage();
    }
}
