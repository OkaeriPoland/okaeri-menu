package eu.okaeri.menu.pane;

import eu.okaeri.menu.MenuContext;
import lombok.NonNull;
import org.bukkit.inventory.Inventory;

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
     * Invalidates all reactive properties in this pane,
     * marking them for re-evaluation on next render.
     */
    void invalidate();

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
