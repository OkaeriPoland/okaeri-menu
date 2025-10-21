package eu.okaeri.menu;

import eu.okaeri.menu.item.MenuItem;
import eu.okaeri.menu.navigation.NavigationHistory;
import eu.okaeri.menu.pane.PaginatedPane;
import eu.okaeri.menu.pane.Pane;
import eu.okaeri.menu.pane.StaticPane;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Map;

/**
 * Base class for all menu contexts.
 * Provides common menu operations: refresh, navigation, sounds, messages, etc.
 */
@Getter
@AllArgsConstructor
public abstract class BaseMenuContext {

    protected final Menu menu;
    protected final HumanEntity entity;

    // ========================================
    // MENU OPERATIONS
    // ========================================

    /**
     * Schedules a menu refresh for the next tick.
     * This is important for interactive slots - the refresh must happen AFTER
     * Bukkit has processed the item placement, not during the event.
     */
    public void refresh() {
        Plugin plugin = this.menu.getPlugin();
        if (plugin != null) {
            Bukkit.getScheduler().runTask(plugin, () -> this.menu.refresh(this.entity));
        } else {
            // Fallback: immediate refresh (may cause issues with interactive slots)
            this.menu.refresh(this.entity);
        }
    }

    /**
     * Schedules a pane refresh for the next tick.
     * This is important for interactive slots - the refresh must happen AFTER
     * Bukkit has processed the item placement, not during the event.
     */
    public void refreshPane(@NonNull String paneName) {
        Plugin plugin = this.menu.getPlugin();
        if (plugin != null) {
            Bukkit.getScheduler().runTask(plugin, () -> this.menu.refreshPane(this.entity, paneName));
        } else {
            // Fallback: immediate refresh (may cause issues with interactive slots)
            this.menu.refreshPane(this.entity, paneName);
        }
    }

    /**
     * Gets a pane by name.
     *
     * @param paneName The pane name
     * @return The pane, or null if not found
     */
    public Pane pane(@NonNull String paneName) {
        return this.menu.getPane(paneName);
    }

    /**
     * Closes this menu for the player.
     */
    public void closeInventory() {
        this.menu.close(this.entity);
        this.entity.closeInventory();
    }

    // ========================================
    // PLAYER FEEDBACK
    // ========================================

    /**
     * Sends a message to the player.
     * The message is processed through the menu's MessageProvider,
     * supporting MiniMessage formatting, legacy colors, and placeholders.
     *
     * @param message The message template
     */
    public void sendMessage(@NonNull String message) {
        Component component = this.menu.getMessageProvider().resolve(this.entity, message, Map.of());
        this.entity.sendMessage(component);
    }

    /**
     * Sends a message to the player with variable placeholders.
     * The message is processed through the menu's MessageProvider,
     * supporting MiniMessage formatting, legacy colors, and placeholders.
     *
     * @param message The message template
     * @param vars    Variables for placeholder replacement
     */
    public void sendMessage(@NonNull String message, @NonNull Map<String, Object> vars) {
        Component component = this.menu.getMessageProvider().resolve(this.entity, message, vars);
        this.entity.sendMessage(component);
    }

    /**
     * Plays a sound to the player.
     *
     * @param sound The sound to play
     */
    public void playSound(@NonNull Sound sound) {
        this.entity.getWorld().playSound(this.entity.getLocation(), sound, 1.0f, 1.0f);
    }

    /**
     * Plays a sound to the player with volume and pitch.
     *
     * @param sound  The sound to play
     * @param volume The volume (0.0 to 1.0)
     * @param pitch  The pitch (0.5 to 2.0)
     */
    public void playSound(@NonNull Sound sound, float volume, float pitch) {
        this.entity.getWorld().playSound(this.entity.getLocation(), sound, volume, pitch);
    }

    // ========================================
    // NAVIGATION
    // ========================================

    /**
     * Opens another menu and tracks it in navigation history.
     * The player can navigate back to the current menu using back().
     *
     * @param targetMenu The menu to open
     */
    public void open(@NonNull Menu targetMenu) {
        NavigationHistory.open(this.entity, targetMenu);
    }

    /**
     * Opens another menu with parameters and tracks it in navigation history.
     *
     * @param targetMenu The menu to open
     * @param params     Parameters to pass to the menu
     */
    public void open(@NonNull Menu targetMenu, @NonNull Map<String, Object> params) {
        NavigationHistory.open(this.entity, targetMenu, params);
    }

    /**
     * Navigates back to the previous menu in history.
     * If there is no previous menu, closes the inventory.
     *
     * @return true if navigated back, false if there was no history
     */
    public boolean back() {
        return NavigationHistory.back(this.entity);
    }

    /**
     * Checks if there is a previous menu in navigation history.
     * Useful for conditionally showing back buttons.
     *
     * @return true if there is a previous menu
     */
    public boolean hasLast() {
        return NavigationHistory.hasLast(this.entity);
    }

    /**
     * Gets the last menu snapshot without navigating.
     *
     * @return The last menu snapshot, or null if no history
     */
    public NavigationHistory.MenuSnapshot last() {
        return NavigationHistory.last(this.entity);
    }

    /**
     * Clears the navigation history for this player.
     * Useful when you want to prevent back navigation.
     */
    public void clearHistory() {
        NavigationHistory.clear(this.entity);
    }

    /**
     * Gets the current depth of the navigation stack.
     *
     * @return The depth (0 if no history)
     */
    public int navigationDepth() {
        return NavigationHistory.depth(this.entity);
    }

    // ========================================
    // PAGINATION
    // ========================================

    /**
     * Gets the pagination context for a pane.
     * Use this to access page navigation and filtering.
     *
     * @param paneName The pane name
     * @param <T>      Item type
     * @return The pagination context
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public <T> eu.okaeri.menu.pagination.PaginationContext<T> pagination(@NonNull String paneName) {
        Pane pane = this.menu.getPane(paneName);
        if (!(pane instanceof PaginatedPane)) {
            throw new IllegalArgumentException("Pane '" + paneName + "' is not a PaginatedPane");
        }

        PaginatedPane<T> paginatedPane = (PaginatedPane<T>) pane;

        // Get items from the pane and create/get context
        java.util.List<T> items = paginatedPane.getItemsSupplier().get();
        return eu.okaeri.menu.pagination.PaginationContext.get(
            this.menu,  // Menu instance for scoped pagination contexts
            paneName,
            this.entity,
            items,
            paginatedPane.getItemsPerPage()
        );
    }

    // ========================================
    // INTERACTIVE SLOT HELPERS
    // ========================================

    /**
     * Sets an item in an interactive slot using pane name and local coordinates.
     * This is a convenience method that handles coordinate conversion and validation.
     *
     * @param paneName The name of the pane
     * @param localX   The local X coordinate (column) within the pane
     * @param localY   The local Y coordinate (row) within the pane
     * @param item     The item to set (can be null to clear)
     * @throws IllegalArgumentException if the pane doesn't exist or slot is not interactive
     */
    public void setSlotItem(@NonNull String paneName, int localX, int localY, ItemStack item) {
        Pane pane = this.menu.getPanes().get(paneName);
        if (pane == null) {
            throw new IllegalArgumentException("Pane not found: " + paneName);
        }

        if (!(pane instanceof StaticPane)) {
            throw new IllegalArgumentException("setSlotItem only supports StaticPane, not " + pane.getClass().getSimpleName());
        }

        StaticPane staticPane = (StaticPane) pane;
        MenuItem menuItem = staticPane.getItem(localX, localY);

        if (menuItem == null) {
            throw new IllegalArgumentException("No item at local coordinates (" + localX + ", " + localY + ") in pane " + paneName);
        }

        if (!menuItem.isInteractive()) {
            throw new IllegalArgumentException(
                "Cannot set item at (" + localX + ", " + localY + ") - slot is not interactive. " +
                    "Use .interactive(), .allowPlacement(true), or .allowPickup(true) on the MenuItem."
            );
        }

        // Convert local coordinates to global slot
        int globalSlot = staticPane.getBounds().toGlobalSlot(localX, localY);

        // Get the inventory for this viewer
        Menu.ViewerState viewerState = this.menu.getViewerStates().get(this.entity.getUniqueId());
        if (viewerState != null) {
            viewerState.getInventory().setItem(globalSlot, item);
        }
    }
}
