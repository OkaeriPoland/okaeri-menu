package eu.okaeri.menu.item;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.MenuContext;
import eu.okaeri.menu.pane.Pane;
import eu.okaeri.menu.pane.PaneBounds;
import eu.okaeri.menu.pane.StaticPane;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Base class for menu item context objects.
 * Extends BaseMenuContext and adds event-specific functionality.
 */
@Getter
public abstract class MenuItemContext extends MenuContext {

    protected final InventoryClickEvent event;
    protected final int slot;

    protected MenuItemContext(
        @NonNull Menu menu,
        @NonNull HumanEntity entity,
        @NonNull InventoryClickEvent event,
        int slot
    ) {
        super(menu, entity);
        this.event = event;
        this.slot = slot;
    }

    /**
     * Cancels the event, preventing default inventory behavior.
     */
    public void cancel() {
        this.event.setCancelled(true);
    }

    /**
     * Gets the inventory click event.
     * <p>
     * <b>IMPORTANT:</b> This method is ONLY valid in synchronous click handlers (onClick, onLeftClick, etc.).
     * <p>
     * <b>DO NOT</b> use this in async click handlers (onClickAsync, onLeftClickAsync, etc.), even if you
     * wrap the call in a sync task via Bukkit.getScheduler().runTask(). The event object is only valid
     * during the immediate handling of the click - by the time your scheduled sync task runs, the event
     * context will be gone.
     *
     * @return The inventory click event
     * @throws IllegalStateException if called from an async context
     */
    public InventoryClickEvent getEvent() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException(
                "getEvent() can only be called in sync click handlers (onClick, onLeftClick, etc.). " +
                    "Do NOT use in async handlers (onClickAsync, etc.), even with Bukkit.getScheduler().runTask() - " +
                    "the event is only valid during immediate click handling."
            );
        }
        return this.event;
    }

    // ========================================
    // INTERACTIVE SLOT HELPERS (AUTO-DETECT)
    // ========================================

    /**
     * Sets an item in an interactive slot using local coordinates within the source pane.
     * Automatically determines which pane triggered the event and uses that as the target pane.
     * This is a convenience method for the common case where you want to update slots in the
     * same pane that triggered the event.
     *
     * @param localRow The local Y coordinate (row) within the source pane
     * @param localCol The local X coordinate (column) within the source pane
     * @param item     The item to set (can be null to clear)
     * @throws IllegalArgumentException if the source pane cannot be determined or slot is not interactive
     */
    public void setSlotItem(int localRow, int localCol, ItemStack item) {
        // Find which pane contains the current slot
        String sourcePaneName = this.findSourcePane();
        if (sourcePaneName == null) {
            throw new IllegalArgumentException(
                "Cannot auto-detect source pane for slot " + this.slot +
                    ". Use setSlotItem(paneName, localX, localY, item) instead."
            );
        }

        // Delegate to the explicit version
        this.setSlotItem(sourcePaneName, localRow, localCol, item);
    }

    /**
     * Finds the name of the pane that contains the current slot.
     * This is used for auto-detecting the source pane in event handlers.
     *
     * @return The pane name, or null if not found
     */
    private String findSourcePane() {
        int globalX = this.slot % 9;
        int globalY = this.slot / 9;

        for (Map.Entry<String, Pane> entry : this.menu.getPanes().entrySet()) {
            Pane pane = entry.getValue();
            if (pane instanceof StaticPane staticPane) {
                PaneBounds bounds = staticPane.getBounds();
                // Check if the global coordinates are within this pane's bounds
                if ((globalX >= bounds.getX()) && (globalX < (bounds.getX() + bounds.getWidth())) &&
                    (globalY >= bounds.getY()) && (globalY < (bounds.getY() + bounds.getHeight()))) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }
}
