package eu.okaeri.menu;

import eu.okaeri.menu.item.InventoryActionCalculator;
import eu.okaeri.menu.item.MenuItem;
import eu.okaeri.menu.item.MenuItemChangeContext;
import eu.okaeri.menu.item.MenuItemClickContext;
import eu.okaeri.menu.navigation.NavigationHistory;
import eu.okaeri.menu.pane.PaginatedPane;
import eu.okaeri.menu.pane.Pane;
import eu.okaeri.menu.pane.StaticPane;
import lombok.NonNull;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

/**
 * Handles inventory events for the new menu system.
 * <p>
 * FAIL-SAFE DESIGN:
 * - All event handlers wrapped in try-catch to prevent exceptions from causing duplication glitches
 * - Events are cancelled by default on any exception
 * - Extensive logging for debugging without breaking functionality
 * <p>
 * AUTO-REGISTRATION:
 * - Automatically registered when the first Menu is created
 * - Prevents duplicate registration (which would cause double event firing)
 * - Thread-safe singleton pattern
 */
public class MenuListener implements Listener {

    private static volatile MenuListener instance;
    private static final Object LOCK = new Object();

    private final @NonNull Plugin plugin;

    /**
     * Private constructor - use {@link #register(Plugin)} instead.
     * Prevents direct instantiation to avoid double registration bugs.
     *
     * @param plugin The plugin instance
     */
    private MenuListener(@NonNull Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers the MenuListener if not already registered.
     * This method is idempotent - calling it multiple times is safe.
     * <p>
     * This is automatically called when the first Menu is created.
     * Manual registration is not recommended - use Menu.builder() instead.
     *
     * @param plugin The plugin instance
     * @return The singleton MenuListener instance (useful for testing)
     */
    public static MenuListener register(@NonNull Plugin plugin) {
        if (instance != null) {
            return instance; // Already registered
        }

        synchronized (LOCK) {
            if (instance != null) {
                return instance; // Double-check after acquiring lock
            }

            instance = new MenuListener(plugin);
            plugin.getServer().getPluginManager().registerEvents(instance, plugin);
            return instance;
        }
    }

    /**
     * Unregisters the MenuListener.
     * Call this in your plugin's onDisable() method.
     */
    public static void unregister() {
        synchronized (LOCK) {
            if (instance != null) {
                org.bukkit.event.HandlerList.unregisterAll(instance);
                instance = null;
            }
        }
    }

    /**
     * Checks if the listener is registered.
     * Useful for testing and debugging.
     *
     * @return true if registered
     */
    public static boolean isRegistered() {
        return instance != null;
    }

    /**
     * Gets the singleton instance.
     * Used for testing purposes to access the registered listener.
     * <p>
     * <b>Note:</b> This may return null if no menu has been created yet.
     * In production code, just create a menu and the listener will auto-register.
     * <p>
     * For tests, use {@link #register(Plugin)} which returns the instance directly.
     *
     * @return The singleton instance, or null if not registered yet
     */
    public static MenuListener getInstance() {
        return instance;
    }

    /**
     * Handles inventory click events.
     * CRITICAL: This method uses fail-safe exception handling to prevent duplication glitches.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClick(@NonNull InventoryClickEvent event) {
        try {
            this.handleInventoryClickSafe(event);
        } catch (Throwable throwable) {
            // FAIL-SAFE: Cancel event on ANY exception to prevent duplication/item loss
            event.setCancelled(true);
            this.plugin.getLogger().log(Level.SEVERE, "FAIL-SAFE: Exception in menu click handler, event cancelled to prevent duplication", throwable);
        }
    }

    /**
     * Handles inventory drag events.
     * CRITICAL: This method uses fail-safe exception handling.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryDrag(@NonNull InventoryDragEvent event) {
        try {
            this.handleInventoryDragSafe(event);
        } catch (Throwable throwable) {
            // FAIL-SAFE: Cancel event on ANY exception
            event.setCancelled(true);
            this.plugin.getLogger().log(Level.SEVERE, "FAIL-SAFE: Exception in menu drag handler, event cancelled", throwable);
        }
    }

    /**
     * Handles inventory close events.
     * CRITICAL: This method uses fail-safe exception handling.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(@NonNull InventoryCloseEvent event) {
        try {
            this.handleInventoryCloseSafe(event);
        } catch (Throwable throwable) {
            // Note: Can't cancel close event, but log the error
            this.plugin.getLogger().log(Level.SEVERE, "FAIL-SAFE: Exception in menu close handler", throwable);
        }
    }

    /**
     * Handles player quit events to prevent navigation history memory leaks.
     * Clears the navigation history for the player who quit.
     * <p>
     * This provides immediate cleanup as a safety net alongside the periodic
     * cleanup task in NavigationHistory.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NonNull PlayerQuitEvent event) {
        try {
            NavigationHistory.clear(event.getPlayer());
        } catch (Throwable throwable) {
            this.plugin.getLogger().log(Level.WARNING, "Exception in player quit navigation cleanup", throwable);
        }
    }

    // ========================================
    // SAFE HANDLER IMPLEMENTATIONS
    // ========================================

    private void handleInventoryClickSafe(@NonNull InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();

        // Clicked outside inventory (drop, etc.)
        if (clickedInventory == null) {
            // Check if they have a menu open at all
            Inventory topInventory = event.getView().getTopInventory();
            if (this.isMenuInventory(topInventory)) {
                // They have a menu open but clicked outside
                // Cancel to prevent item drops while menu is open
                event.setCancelled(true);
            }
            return;
        }

        // Check if the clicked inventory is a menu
        boolean clickedInMenu = this.isMenuInventory(clickedInventory);

        // Check if they have a menu open in the top inventory
        Inventory topInventory = event.getView().getTopInventory();
        boolean hasMenuOpen = this.isMenuInventory(topInventory);

        // Handle click in menu inventory
        if (clickedInMenu) {
            this.handleMenuClick(event, clickedInventory);
            return;
        }

        // Handle click in player inventory while menu is open
        if (hasMenuOpen) {
            this.handlePlayerInventoryClickWhileMenuOpen(event, topInventory);
        }

        // Not a menu-related click, ignore
    }

    private void handleMenuClick(@NonNull InventoryClickEvent event, @NonNull Inventory inventory) {
        Menu menu = this.getMenuFromInventory(inventory);
        if (menu == null) {
            this.plugin.getLogger().warning("Inventory is menu type but Menu instance is null! This should not happen.");
            event.setCancelled(true);
            return;
        }

        HumanEntity player = event.getWhoClicked();
        int rawSlot = event.getRawSlot();
        int inventorySize = inventory.getSize();

        // Verify click is actually in the menu (not bottom inventory)
        if ((rawSlot < 0) || (rawSlot >= inventorySize)) {
            event.setCancelled(true);
            return;
        }

        // Find the clicked item
        MenuItem clickedItem = this.findMenuItemAtSlot(menu, rawSlot);

        if (clickedItem == null) {
            // No item at this slot, cancel by default
            event.setCancelled(true);
            return;
        }

        // Check if this is an interactive slot - handle BEFORE cancelling
        if (clickedItem.isInteractive()) {
            this.handleInteractiveSlotClick(event, menu, player, rawSlot, clickedItem, inventory);
            return;
        }

        // Non-interactive items: cancel by default, only allow if explicitly permitted
        event.setCancelled(true);

        // Create click context for non-interactive items
        MenuItemClickContext clickContext = new MenuItemClickContext(
            menu,
            player,
            event,
            rawSlot,
            event.getClick()
        );

        // Handle the click
        try {
            clickedItem.handleClick(clickContext);
        } catch (Exception exception) {
            // Exception in user click handler
            // Event is already cancelled, so we're safe
            this.plugin.getLogger().log(Level.WARNING, "Exception in menu item click handler (slot " + rawSlot + ")", exception);
        }
    }

    /**
     * Handles clicks on interactive slots (slots that allow item pickup/placement).
     * Calculates the new item state directly from the event without using scheduled tasks.
     * Event starts un-cancelled, and will be cancelled only if the action should be blocked.
     */
    private void handleInteractiveSlotClick(@NonNull InventoryClickEvent event, @NonNull Menu menu, @NonNull HumanEntity player,
                                            int rawSlot, @NonNull MenuItem clickedItem, @NonNull Inventory inventory) {
        // Check if this action is supported (can be calculated)
        if (!InventoryActionCalculator.isActionSupported(event.getAction())) {
            // Complex action that we can't calculate - cancel it
            event.setCancelled(true);
            return;
        }

        // Capture current state
        ItemStack previousItem = inventory.getItem(rawSlot);
        ItemStack previousItemClone = (previousItem != null) ? previousItem.clone() : null;

        // Calculate what the new item will be
        ItemStack calculatedNewItem = InventoryActionCalculator.calculateNewSlotItem(event);

        // Verify the action is allowed based on permissions
        if (!this.shouldAllowInteraction(event, clickedItem, previousItem, calculatedNewItem)) {
            // Cancel the action - not allowed based on allowPickup/allowPlacement
            event.setCancelled(true);
            this.plugin.getLogger().fine("Interactive slot action blocked (slot " + rawSlot + ")");
            return;
        }

        // Create change context with calculated new item
        MenuItemChangeContext changeContext = new MenuItemChangeContext(
            menu,
            player,
            event,
            rawSlot,
            previousItemClone,
            (calculatedNewItem != null) ? calculatedNewItem.clone() : null
        );

        // Call the item change handler (handler can cancel via ctx.cancel() if needed)
        try {
            clickedItem.handleItemChange(changeContext);
        } catch (Exception exception) {
            // Exception in handler - cancel event for safety
            event.setCancelled(true);
            this.plugin.getLogger().log(Level.WARNING,
                "Exception in interactive slot change handler (slot " + rawSlot + "), action blocked",
                exception);
        }

        // If handler called ctx.cancel(), event is now cancelled and action won't proceed
        // Otherwise, event remains un-cancelled and Bukkit will handle the item interaction
    }

    /**
     * Determines if an interaction with an interactive slot should be allowed.
     * Checks permissions based on the action type.
     */
    private boolean shouldAllowInteraction(@NonNull InventoryClickEvent event, @NonNull MenuItem item,
                                           ItemStack currentItem, ItemStack calculatedNewItem) {
        switch (event.getAction()) {
            // Pickup actions - require allowPickup
            case PICKUP_ALL:
            case PICKUP_HALF:
            case PICKUP_ONE:
            case PICKUP_SOME:
            case DROP_ALL_SLOT:
            case DROP_ONE_SLOT:
                return item.isAllowPickup();

            // Placement actions - require allowPlacement
            case PLACE_ALL:
            case PLACE_SOME:
            case PLACE_ONE:
                return item.isAllowPlacement();

            // Swap actions - require both
            case SWAP_WITH_CURSOR:
            case HOTBAR_SWAP:
                return item.isAllowPickup() && item.isAllowPlacement();

            // Nothing/Unknown - allow
            case NOTHING:
            default:
                return true;
        }
    }

    private void handlePlayerInventoryClickWhileMenuOpen(@NonNull InventoryClickEvent event, @NonNull Inventory menuInventory) {
        // Player clicked their own inventory while menu is open
        // Allow normal inventory interactions (pickup/place items on cursor)
        // But prevent actions that move items into the menu

        switch (event.getAction()) {
            // Block actions that automatically move items into the menu
            case MOVE_TO_OTHER_INVENTORY:    // Shift-click
            case COLLECT_TO_CURSOR:           // Double-click to collect
                event.setCancelled(true);
                this.plugin.getLogger().fine("Blocked shift-click/collect while menu open");
                break;

            // Block hotbar swaps that could affect menu slots
            case HOTBAR_SWAP:
            case HOTBAR_MOVE_AND_READD:
                // Only block if it would affect the menu inventory
                int hotbarButton = event.getHotbarButton();
                if ((hotbarButton >= 0) && (hotbarButton <= 8)) {
                    event.setCancelled(true);
                    this.plugin.getLogger().fine("Blocked hotbar swap while menu open");
                }
                break;

            // Allow all other actions in player inventory (PICKUP, PLACE, etc.)
            default:
                // Don't cancel - allow normal inventory interactions
                break;
        }
    }

    private void handleInventoryDragSafe(@NonNull InventoryDragEvent event) {
        Inventory inventory = event.getInventory();

        if (!this.isMenuInventory(inventory)) {
            return;
        }

        // Check if any of the dragged slots are in the menu inventory
        int inventorySize = inventory.getSize();
        boolean draggedInMenu = event.getRawSlots().stream()
            .anyMatch(slot -> (slot >= 0) && (slot < inventorySize));

        if (draggedInMenu) {
            // Only cancel if dragging into menu slots
            event.setCancelled(true);
            this.plugin.getLogger().fine("Prevented drag into menu inventory");
        }
        // Otherwise allow the drag (player dragging in their own inventory)
    }

    private void handleInventoryCloseSafe(@NonNull InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();

        if (!this.isMenuInventory(inventory)) {
            return;
        }

        Menu menu = this.getMenuFromInventory(inventory);
        if (menu == null) {
            return;
        }

        HumanEntity player = event.getPlayer();

        // Check if this close event is for the player's current menu inventory
        // or if it's an old inventory being replaced (e.g., for title updates)
        Menu.ViewerState viewerState = menu.getViewerState(player.getUniqueId());
        if (viewerState != null) {
            Inventory currentInventory = viewerState.getInventory();

            // If the closed inventory is NOT the current one, it means we're replacing
            // the inventory (e.g., for dynamic title update). Don't clean up in this case.
            if (currentInventory != inventory) {
                this.plugin.getLogger().fine("Ignoring close event for replaced inventory (title update)");
                return;
            }
        }

        // Cleanup: Remove viewer-specific data
        // This is important to prevent memory leaks
        try {
            menu.close(player);
        } catch (Exception exception) {
            this.plugin.getLogger().log(Level.WARNING, "Exception during menu cleanup on close", exception);
        }
    }

    // ========================================
    // UTILITY METHODS
    // ========================================

    /**
     * Checks if an inventory is a menu inventory.
     */
    private boolean isMenuInventory(Inventory inventory) {
        if (inventory == null) {
            return false;
        }

        InventoryHolder holder = inventory.getHolder();
        return holder instanceof Menu;
    }

    /**
     * Gets the Menu instance from an inventory.
     */
    private Menu getMenuFromInventory(Inventory inventory) {
        if (inventory == null) {
            return null;
        }

        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof Menu) {
            return (Menu) holder;
        }

        return null;
    }

    /**
     * Finds a menu item at the specified global slot.
     * Searches all panes in the menu.
     */
    private MenuItem findMenuItemAtSlot(@NonNull Menu menu, int globalSlot) {
        for (Pane pane : menu.getPanes().values()) {
            MenuItem item = null;

            if (pane instanceof StaticPane) {
                StaticPane staticPane = (StaticPane) pane;
                item = staticPane.getItemByGlobalSlot(globalSlot);
            } else if (pane instanceof PaginatedPane) {
                PaginatedPane<?> paginatedPane = (PaginatedPane<?>) pane;
                item = paginatedPane.getItemByGlobalSlot(globalSlot);
            }

            if (item != null) {
                return item;
            }
        }

        return null;
    }
}
