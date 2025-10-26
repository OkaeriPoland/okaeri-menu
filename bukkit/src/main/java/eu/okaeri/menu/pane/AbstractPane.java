package eu.okaeri.menu.pane;

import eu.okaeri.menu.MenuContext;
import eu.okaeri.menu.item.MenuItem;
import lombok.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for panes that provides common functionality.
 * Handles coordinate conversion and shared rendering logic.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractPane implements Pane {

    protected @NonNull final String name;
    protected @NonNull final PaneBounds bounds;
    protected final Map<Integer, MenuItem> staticItems = new HashMap<>();

    /**
     * Checks if a global slot is within this pane's bounds.
     *
     * @param globalSlot The global inventory slot
     * @return true if the slot is within bounds
     */
    protected boolean isGlobalSlotInBounds(int globalSlot) {
        int globalX = globalSlot % 9;
        int globalY = globalSlot / 9;

        return (globalX >= this.bounds.getX()) &&
            (globalX < (this.bounds.getX() + this.bounds.getWidth())) &&
            (globalY >= this.bounds.getY()) &&
            (globalY < (this.bounds.getY() + this.bounds.getHeight()));
    }

    /**
     * Converts a global slot to local coordinates within this pane.
     *
     * @param globalSlot The global inventory slot
     * @return Array [localX, localY], or null if slot not in bounds
     */
    protected int[] globalSlotToLocal(int globalSlot) {
        if (!this.isGlobalSlotInBounds(globalSlot)) {
            return null;
        }

        int globalX = globalSlot % 9;
        int globalY = globalSlot / 9;
        int localX = globalX - this.bounds.getX();
        int localY = globalY - this.bounds.getY();

        return new int[]{localX, localY};
    }

    /**
     * Converts local coordinates to local slot index.
     *
     * @param localX Local X coordinate
     * @param localY Local Y coordinate
     * @return The local slot index
     */
    protected int localCoordinatesToSlot(int localX, int localY) {
        return (localY * this.bounds.getWidth()) + localX;
    }

    /**
     * Gets a menu item by global slot with context for per-player lookup.
     * Converts global slot to local coordinates and delegates to getItem().
     *
     * @param globalSlot The global inventory slot
     * @param context    The menu context (for per-player state)
     * @return The menu item, or null if not found
     */
    @Override
    public MenuItem getItemByGlobalSlot(int globalSlot, @NonNull MenuContext context) {
        int[] local = this.globalSlotToLocal(globalSlot);
        if (local == null) {
            return null;
        }
        return this.getItem(local[0], local[1], context);
    }

    /**
     * Gets a menu item by global slot (for click handling).
     * Uses template method pattern - subclasses implement getItemAtLocalSlot().
     *
     * <p><b>DEPRECATED:</b> This method cannot access per-player render caches.
     * For panes that use per-player caching (StaticPane auto-items, PaginatedPane),
     * this will only return static items. Use {@link #getItemByGlobalSlot(int, MenuContext)} instead.
     *
     * @param globalSlot The global inventory slot
     * @return The menu item, or null if not found (may be incomplete for cached items)
     * @deprecated Use context-aware {@link #getItemByGlobalSlot(int, MenuContext)} instead
     */
    @Deprecated
    public MenuItem getItemByGlobalSlot(int globalSlot) {
        int[] local = this.globalSlotToLocal(globalSlot);
        if (local == null) {
            return null;
        }
        return this.getItem(local[0], local[1]);
    }

    /**
     * Gets the menu item at local coordinates.
     * Subclasses implement this to return items from their storage.
     *
     * @param localX Local X coordinate
     * @param localY Local Y coordinate
     * @return The menu item, or null if none
     */
    @Override
    public abstract MenuItem getItem(int localX, int localY);

    /**
     * Gets items that can have filtering behavior (for declarative filters).
     * Returns the static items map by default.
     *
     * @return Map of local slot to MenuItem
     */
    @NonNull
    @Override
    public Map<Integer, MenuItem> getFilteringItems() {
        return this.staticItems;
    }

    /**
     * Renders static items (navigation buttons, decorations, etc).
     * Shared implementation used by paginated panes.
     * <p>
     * Clears slots where items are not visible (renders as null).
     *
     * @param inventory   The inventory to render into
     * @param context     The menu context
     * @param staticItems Map of local slot → MenuItem
     */
    protected void renderStaticItems(@NonNull Inventory inventory, @NonNull MenuContext context, @NonNull Map<Integer, MenuItem> staticItems) {
        this.bounds.slots().forEachMap(staticItems, (localSlot, globalSlot, menuItem) -> {
            ItemStack itemStack = menuItem.render(context);
            inventory.setItem(globalSlot, itemStack);
        });
    }

    /**
     * Helper method to convert item coordinate entries to a slot map with validation.
     * Used by builders to validate and convert entries after bounds are finalized.
     *
     * @param entries The list of coordinate entries
     * @param bounds  The finalized pane bounds
     * @return Map of local slot indices to MenuItems
     * @throws IllegalArgumentException if any coordinate is out of bounds
     */
    protected static Map<Integer, MenuItem> convertEntriesToSlotMap(
        @NonNull List<ItemCoordinateEntry> entries,
        @NonNull PaneBounds bounds
    ) {
        Map<Integer, MenuItem> result = new HashMap<>();
        for (ItemCoordinateEntry entry : entries) {
            // Validate coordinates against bounds
            bounds.validate(entry.getLocalX(), entry.getLocalY());

            int localSlot = bounds.coordinatesToLocalSlot(entry.getLocalX(), entry.getLocalY());
            result.put(localSlot, entry.getMenuItem());
        }
        return result;
    }

    /**
     * Helper class to store item coordinates during pane building.
     * Package-private so builders can use it.
     */
    @Value
    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    protected static class ItemCoordinateEntry {
        int localX;
        int localY;
        MenuItem menuItem;
    }
}
