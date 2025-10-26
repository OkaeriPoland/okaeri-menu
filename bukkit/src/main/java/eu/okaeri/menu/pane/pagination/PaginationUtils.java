package eu.okaeri.menu.pane.pagination;

import eu.okaeri.menu.item.MenuItem;
import eu.okaeri.menu.pane.PaginationContext;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.Sound;

import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Utility methods for common pagination patterns.
 * All methods use the menu's MessageProvider for text formatting.
 *
 * <p>Core i18n-aware methods accept Map&lt;Locale, String&gt; for full localization support.
 * Convenience methods provide English defaults by calling the i18n versions.
 */
public final class PaginationUtils {

    // ========================================
    // I18N-AWARE METHODS (Core API)
    // ========================================

    /**
     * Creates a "Next Page" button with full i18n support.
     *
     * @param paneName The paginated pane name
     * @param material The material for the button
     * @param name     The display name in multiple languages
     * @param lore     The lore template in multiple languages (supports &lt;current&gt; and &lt;total&gt; placeholders)
     * @return MenuItem builder for next page button
     */
    public static MenuItem.@NonNull Builder nextPageButton(@NonNull String paneName, @NonNull Material material, @NonNull Map<Locale, String> name, Map<Locale, String> lore) {
        MenuItem.Builder builder = MenuItem.item()
            .material(material)
            .name(name)
            .visible(ctx -> {
                PaginationContext<?> pagination = ctx.pagination(paneName);
                return pagination.hasNext();
            })
            .onClick(event -> {
                PaginationContext<?> pagination = event.pagination(paneName);
                if (pagination.nextPage()) {
                    event.refresh();
                    event.playSound(Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                }
            });

        if ((lore != null) && !lore.isEmpty()) {
            builder.lore(lore, ctx -> {
                PaginationContext<?> pagination = ctx.pagination(paneName);
                return Map.of(
                    "current", pagination.getCurrentPage() + 1,
                    "total", pagination.getTotalPages()
                );
            });
        }

        return builder;
    }

    /**
     * Creates a "Next Page" button with i18n support and default material (ARROW).
     *
     * @param paneName The paginated pane name
     * @param name     The display name in multiple languages
     * @param lore     The lore template in multiple languages
     * @return MenuItem builder for next page button
     */
    public static MenuItem.@NonNull Builder nextPageButton(@NonNull String paneName, @NonNull Map<Locale, String> name, Map<Locale, String> lore) {
        return nextPageButton(paneName, Material.ARROW, name, lore);
    }

    /**
     * Creates a "Previous Page" button with full i18n support.
     *
     * @param paneName The paginated pane name
     * @param material The material for the button
     * @param name     The display name in multiple languages
     * @param lore     The lore template in multiple languages (supports &lt;current&gt; and &lt;total&gt; placeholders)
     * @return MenuItem builder for previous page button
     */
    public static MenuItem.@NonNull Builder previousPageButton(@NonNull String paneName, @NonNull Material material, @NonNull Map<Locale, String> name, Map<Locale, String> lore) {
        MenuItem.Builder builder = MenuItem.item()
            .material(material)
            .name(name)
            .visible(ctx -> {
                PaginationContext<?> pagination = ctx.pagination(paneName);
                return pagination.hasPrevious();
            })
            .onClick(event -> {
                PaginationContext<?> pagination = event.pagination(paneName);
                if (pagination.previousPage()) {
                    event.refresh();
                    event.playSound(Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
                }
            });

        if ((lore != null) && !lore.isEmpty()) {
            builder.lore(lore, ctx -> {
                PaginationContext<?> pagination = ctx.pagination(paneName);
                return Map.of(
                    "current", pagination.getCurrentPage() + 1,
                    "total", pagination.getTotalPages()
                );
            });
        }

        return builder;
    }

    /**
     * Creates a "Previous Page" button with i18n support and default material (ARROW).
     *
     * @param paneName The paginated pane name
     * @param name     The display name in multiple languages
     * @param lore     The lore template in multiple languages
     * @return MenuItem builder for previous page button
     */
    public static MenuItem.@NonNull Builder previousPageButton(@NonNull String paneName, @NonNull Map<Locale, String> name, Map<Locale, String> lore) {
        return previousPageButton(paneName, Material.ARROW, name, lore);
    }

    /**
     * Creates a page indicator with full i18n support (non-clickable).
     *
     * @param paneName The paginated pane name
     * @param material The material for the indicator
     * @param name     The name template in multiple languages (supports &lt;current&gt; and &lt;total&gt; placeholders)
     * @param lore     The lore template in multiple languages (supports &lt;showing&gt;, &lt;total_items&gt;, &lt;filters&gt; placeholders)
     * @return MenuItem builder for page indicator
     */
    public static MenuItem.@NonNull Builder pageIndicator(@NonNull String paneName, @NonNull Material material, @NonNull Map<Locale, String> name, Map<Locale, String> lore) {
        MenuItem.Builder builder = MenuItem.item()
            .material(material)
            .name(name, ctx -> {
                PaginationContext<?> pagination = ctx.pagination(paneName);
                return Map.of(
                    "current", pagination.getCurrentPage() + 1,
                    "total", pagination.getTotalPages()
                );
            });

        if ((lore != null) && !lore.isEmpty()) {
            builder.lore(lore, ctx -> {
                PaginationContext<?> pagination = ctx.pagination(paneName);
                return Map.of(
                    "showing", pagination.getCurrentPageItems().size(),
                    "total_items", pagination.getTotalItems(),
                    "filters", pagination.getActiveFilterCount()
                );
            });
        }

        return builder;
    }

    /**
     * Creates a page indicator with i18n support and default material (PAPER).
     *
     * @param paneName The paginated pane name
     * @param name     The name template in multiple languages
     * @param lore     The lore template in multiple languages
     * @return MenuItem builder for page indicator
     */
    public static MenuItem.@NonNull Builder pageIndicator(@NonNull String paneName, @NonNull Map<Locale, String> name, Map<Locale, String> lore) {
        return pageIndicator(paneName, Material.PAPER, name, lore);
    }

    /**
     * Creates a filter toggle button with full i18n support.
     *
     * @param paneName   The paginated pane name
     * @param filterId   The filter identifier
     * @param filterName The display name of the filter in multiple languages
     * @param lore       The lore template in multiple languages (supports &lt;status&gt; placeholder)
     * @param predicate  The filter predicate
     * @param <T>        Item type
     * @return MenuItem builder for filter button
     */
    public static <T> MenuItem.@NonNull Builder filterButton(@NonNull String paneName, @NonNull String filterId, @NonNull Map<Locale, String> filterName, Map<Locale, String> lore, @NonNull Predicate<T> predicate) {
        MenuItem.Builder builder = MenuItem.item()
            .material(ctx -> {
                PaginationContext<T> pagination = ctx.pagination(paneName);
                return pagination.hasFilter(filterId) ? Material.EMERALD : Material.COAL;
            })
            .name(filterName)
            .onClick(event -> {
                PaginationContext<T> pagination = event.pagination(paneName);
                pagination.toggleFilter(filterId, predicate);
                event.refresh();
                event.playSound(Sound.UI_BUTTON_CLICK);
            });

        if ((lore != null) && !lore.isEmpty()) {
            builder.lore(lore, ctx -> {
                PaginationContext<T> pagination = ctx.pagination(paneName);
                boolean active = pagination.hasFilter(filterId);
                return Map.of("status", active ? "<green>Active" : "<gray>Inactive");
            });
        }

        return builder;
    }

    /**
     * Creates a "Clear Filters" button with full i18n support.
     *
     * @param paneName The paginated pane name
     * @param material The material for the button
     * @param name     The display name in multiple languages
     * @param lore     The lore template in multiple languages (supports &lt;count&gt; placeholder)
     * @return MenuItem builder for clear filters button
     */
    public static MenuItem.@NonNull Builder clearFiltersButton(@NonNull String paneName, @NonNull Material material, @NonNull Map<Locale, String> name, Map<Locale, String> lore) {
        MenuItem.Builder builder = MenuItem.item()
            .material(material)
            .name(name)
            .visible(ctx -> {
                PaginationContext<?> pagination = ctx.pagination(paneName);
                return pagination.getActiveFilterCount() > 0;
            })
            .onClick(event -> {
                PaginationContext<?> pagination = event.pagination(paneName);
                pagination.clearFilters();
                event.refresh();
                event.playSound(Sound.ENTITY_ITEM_BREAK);
            });

        if ((lore != null) && !lore.isEmpty()) {
            builder.lore(lore, ctx -> {
                PaginationContext<?> pagination = ctx.pagination(paneName);
                return Map.of("count", pagination.getActiveFilterCount());
            });
        }

        return builder;
    }

    /**
     * Creates a "Clear Filters" button with i18n support and default material (BARRIER).
     *
     * @param paneName The paginated pane name
     * @param name     The display name in multiple languages
     * @param lore     The lore template in multiple languages
     * @return MenuItem builder for clear filters button
     */
    public static MenuItem.@NonNull Builder clearFiltersButton(@NonNull String paneName, @NonNull Map<Locale, String> name, Map<Locale, String> lore) {
        return clearFiltersButton(paneName, Material.BARRIER, name, lore);
    }

    /**
     * Creates an "Empty" indicator with full i18n support.
     *
     * @param paneName The paginated pane name
     * @param material The material for the indicator
     * @param name     The display name in multiple languages
     * @param lore     The lore in multiple languages
     * @return MenuItem builder for empty indicator
     */
    public static MenuItem.@NonNull Builder emptyIndicator(@NonNull String paneName, @NonNull Material material, @NonNull Map<Locale, String> name, Map<Locale, String> lore) {
        MenuItem.Builder builder = MenuItem.item()
            .material(material)
            .name(name)
            .visible(ctx -> {
                PaginationContext<?> pagination = ctx.pagination(paneName);
                return pagination.isEmpty();
            });

        if ((lore != null) && !lore.isEmpty()) {
            builder.lore(lore);
        }

        return builder;
    }

    /**
     * Creates an "Empty" indicator with i18n support and default material (BARRIER).
     *
     * @param paneName The paginated pane name
     * @param name     The display name in multiple languages
     * @param lore     The lore in multiple languages
     * @return MenuItem builder for empty indicator
     */
    public static MenuItem.@NonNull Builder emptyIndicator(@NonNull String paneName, @NonNull Map<Locale, String> name, Map<Locale, String> lore) {
        return emptyIndicator(paneName, Material.BARRIER, name, lore);
    }

    // ========================================
    // CONVENIENCE METHODS (English Defaults)
    // ========================================

    /**
     * Creates a "Next Page" button with English text.
     *
     * @param paneName The paginated pane name
     * @return MenuItem builder for next page button
     */
    public static MenuItem.@NonNull Builder nextPageButton(@NonNull String paneName) {
        return nextPageButton(
            paneName,
            Material.ARROW,
            Map.of(Locale.ENGLISH, "<green>Next Page →"),
            Map.of(Locale.ENGLISH, """
                <gray>Current: <white><current>
                <gray>Total: <white><total>
                
                <yellow>Click to go to next page!""")
        );
    }

    /**
     * Creates a "Previous Page" button with English text.
     *
     * @param paneName The paginated pane name
     * @return MenuItem builder for previous page button
     */
    public static MenuItem.@NonNull Builder previousPageButton(@NonNull String paneName) {
        return previousPageButton(
            paneName,
            Material.ARROW,
            Map.of(Locale.ENGLISH, "<red>← Previous Page"),
            Map.of(Locale.ENGLISH, """
                <gray>Current: <white><current>
                <gray>Total: <white><total>
                
                <yellow>Click to go to previous page!""")
        );
    }

    /**
     * Creates a page indicator with English text (non-clickable).
     *
     * @param paneName The paginated pane name
     * @return MenuItem builder for page indicator
     */
    public static MenuItem.@NonNull Builder pageIndicator(@NonNull String paneName) {
        return pageIndicator(
            paneName,
            Material.PAPER,
            Map.of(Locale.ENGLISH, "<yellow>Page <current> / <total>"),
            Map.of(Locale.ENGLISH, """
                <gray>Showing: <white><showing> items
                <gray>Total: <white><total_items> items
                <gray>Filters: <white><filters> active""")
        );
    }

    /**
     * Creates a filter toggle button with English text.
     *
     * @param paneName   The paginated pane name
     * @param filterId   The filter identifier
     * @param filterName The display name of the filter (English)
     * @param predicate  The filter predicate
     * @param <T>        Item type
     * @return MenuItem builder for filter button
     */
    public static <T> MenuItem.@NonNull Builder filterButton(@NonNull String paneName, @NonNull String filterId, @NonNull String filterName, @NonNull Predicate<T> predicate) {
        return filterButton(
            paneName,
            filterId,
            Map.of(Locale.ENGLISH, filterName),
            Map.of(Locale.ENGLISH, """
                <gray>Status: <status>
                
                <yellow>Click to toggle!"""),
            predicate
        );
    }

    /**
     * Creates a "Clear Filters" button with English text.
     *
     * @param paneName The paginated pane name
     * @return MenuItem builder for clear filters button
     */
    public static MenuItem.@NonNull Builder clearFiltersButton(@NonNull String paneName) {
        return clearFiltersButton(
            paneName,
            Material.BARRIER,
            Map.of(Locale.ENGLISH, "<red>✕ Clear All Filters"),
            Map.of(Locale.ENGLISH, """
                <gray>Active filters: <white><count>
                
                <yellow>Click to clear all filters!""")
        );
    }

    /**
     * Creates a "First Page" button with English text.
     *
     * @param paneName The paginated pane name
     * @return MenuItem builder for first page button
     */
    public static MenuItem.@NonNull Builder firstPageButton(@NonNull String paneName) {
        return MenuItem.item()
            .material(Material.ARROW)
            .name("<aqua>⏮ First Page")
            .visible(ctx -> {
                PaginationContext<?> pagination = ctx.pagination(paneName);
                return pagination.getCurrentPage() > 0;
            })
            .onClick(event -> {
                PaginationContext<?> pagination = event.pagination(paneName);
                pagination.firstPage();
                event.refresh();
                event.playSound(Sound.UI_BUTTON_CLICK, 0.5f, 0.6f);
            });
    }

    /**
     * Creates a "Last Page" button with English text.
     *
     * @param paneName The paginated pane name
     * @return MenuItem builder for last page button
     */
    public static MenuItem.@NonNull Builder lastPageButton(@NonNull String paneName) {
        return MenuItem.item()
            .material(Material.ARROW)
            .name("<aqua>Last Page ⏭")
            .visible(ctx -> {
                PaginationContext<?> pagination = ctx.pagination(paneName);
                return pagination.getCurrentPage() < (pagination.getTotalPages() - 1);
            })
            .onClick(event -> {
                PaginationContext<?> pagination = event.pagination(paneName);
                pagination.lastPage();
                event.refresh();
                event.playSound(Sound.UI_BUTTON_CLICK, 0.5f, 1.4f);
            });
    }

    /**
     * Creates an "Empty" indicator with English text.
     *
     * @param paneName The paginated pane name
     * @return MenuItem builder for empty indicator
     */
    public static MenuItem.@NonNull Builder emptyIndicator(@NonNull String paneName) {
        return emptyIndicator(
            paneName,
            Material.BARRIER,
            Map.of(Locale.ENGLISH, "<red>No Items"),
            Map.of(Locale.ENGLISH, """
                <gray>No items match your filters
                <gray>or the list is empty.""")
        );
    }

    /**
     * Creates a customizable pagination control bar.
     * Use this to easily add prev/next/indicator buttons.
     *
     * @param paneName The paginated pane name
     * @return Array of MenuItem builders [previous, indicator, next]
     */
    public static MenuItem.@NonNull Builder[] paginationBar(@NonNull String paneName) {
        return new MenuItem.Builder[]{
            previousPageButton(paneName),
            pageIndicator(paneName),
            nextPageButton(paneName)
        };
    }
}
