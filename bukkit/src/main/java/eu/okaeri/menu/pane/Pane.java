package eu.okaeri.menu.pane;

import eu.okaeri.menu.MenuContext;
import eu.okaeri.menu.item.MenuItem;
import lombok.NonNull;
import org.bukkit.inventory.Inventory;

import java.util.Map;

/**
 * Represents a region within a menu inventory.
 * Panes have their own coordinate space and manage their own items.
 */
public interface Pane {

    /**
     * Gets the name/identifier of this pane.
     *
     * @return The pane name
     */
    @NonNull
    String getName();

    /**
     * Gets the bounds of this pane within the inventory.
     *
     * @return The pane bounds
     */
    @NonNull
    PaneBounds getBounds();

    /**
     * Renders this pane's items into the inventory.
     *
     * @param inventory The inventory to render into
     * @param context   The menu context for evaluating properties
     */
    void render(@NonNull Inventory inventory, @NonNull MenuContext context);

    /**
     * Gets a menu item at local coordinates (non-context version).
     * For static lookups that don't need per-player state.
     *
     * @param localX Local X coordinate
     * @param localY Local Y coordinate
     * @return The menu item, or null if not found
     */
    MenuItem getItem(int localX, int localY);

    /**
     * Gets a menu item at local coordinates with context for per-player lookup.
     * Used by panes to access per-player render caches.
     *
     * @param localX  Local X coordinate
     * @param localY  Local Y coordinate
     * @param context The menu context (for per-player state)
     * @return The menu item, or null if not found
     */
    MenuItem getItem(int localX, int localY, @NonNull MenuContext context);

    /**
     * Gets a menu item by global slot with context for per-player lookup.
     *
     * @param globalSlot The global inventory slot
     * @param context    The menu context (for per-player state)
     * @return The menu item, or null if not found
     */
    MenuItem getItemByGlobalSlot(int globalSlot, @NonNull MenuContext context);

    /**
     * Gets all menu items that can contain declarative filters.
     * For StaticPane: all items (since all items are static).
     * For PaginatedPane: only static items (navigation buttons, filter buttons, etc).
     * Excludes dynamically rendered paginated content.
     *
     * @return Map of local slot → MenuItem
     */
    @NonNull
    Map<Integer, MenuItem> getFilteringItems();

    /**
     * Gets the type of this pane.
     *
     * @return The pane type
     */
    @NonNull
    default PaneType getType() {
        return PaneType.STATIC;
    }

    /**
     * Pane types.
     */
    enum PaneType {
        STATIC,
        PAGINATED
    }
}
