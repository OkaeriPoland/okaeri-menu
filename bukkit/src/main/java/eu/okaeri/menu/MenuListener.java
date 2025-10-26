package eu.okaeri.menu;

import eu.okaeri.menu.item.InventoryActionCalculator;
import eu.okaeri.menu.item.MenuItem;
import eu.okaeri.menu.item.MenuItemChangeContext;
import eu.okaeri.menu.item.MenuItemClickContext;
import eu.okaeri.menu.navigation.NavigationHistory;
import eu.okaeri.menu.pane.Pane;
import eu.okaeri.menu.state.ViewerState;
import lombok.NonNull;
import lombok.Synchronized;
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
    @Synchronized
    public static MenuListener register(@NonNull Plugin plugin) {
        if (instance != null) {
            return instance; // Already registered
        }
        instance = new MenuListener(plugin);
        plugin.getServer().getPluginManager().registerEvents(instance, plugin);
        return instance;
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
        MenuItem clickedItem = this.findMenuItemAtSlot(menu, rawSlot, player);

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

        // Check if item is visible before handling click
        // Invisible items should not be clickable (prevents clicking items that are not rendered)
        if (!clickedItem.getVisible().get(clickContext)) {
            // Item is invisible, don't handle click
            return;
        }

        // Handle sync click handlers
        try {
            clickedItem.handleClick(clickContext);
        } catch (Exception exception) {
            // Exception in user click handler
            // Event is already cancelled, so we're safe
            this.plugin.getLogger().log(Level.WARNING, "Exception in menu item click handler (slot " + rawSlot + ")", exception);
        }

        // Handle async click handlers (if any)
        if (clickedItem.hasAsyncClickHandler()) {
            MenuContext menuContext = new MenuContext(menu, player);
            menu.getAsyncExecutor().execute(() -> {
                try {
                    clickedItem.handleAsyncClick(clickContext);
                } catch (Exception exception) {
                    // Exception in async click handler
                    this.plugin.getLogger().log(Level.WARNING, "Exception in async menu item click handler (slot " + rawSlot + ")", exception);
                }
                return null;
            });
        }
    }

    /**
     * Handles clicks on interactive slots (slots that allow item pickup/placement).
     * Calculates the new item state directly from the event without using scheduled tasks.
     * Event starts un-cancelled, and will be cancelled only if the action should be blocked.
     */
    private void handleInteractiveSlotClick(
        @NonNull InventoryClickEvent event,
        @NonNull Menu menu,
        @NonNull HumanEntity player,
        int rawSlot,
        @NonNull MenuItem clickedItem,
        @NonNull Inventory inventory
    ) {

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
    private boolean shouldAllowInteraction(
        @NonNull InventoryClickEvent event,
        @NonNull MenuItem item,
        ItemStack currentItem,
        ItemStack calculatedNewItem
    ) {
        return switch (event.getAction()) {
            // Pickup actions - require allowPickup
            case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME, DROP_ALL_SLOT, DROP_ONE_SLOT -> item.isAllowPickup();

            // Placement actions - require allowPlacement
            case PLACE_ALL, PLACE_SOME, PLACE_ONE -> item.isAllowPlacement();

            // Swap actions - require both
            case SWAP_WITH_CURSOR, HOTBAR_SWAP -> item.isAllowPickup() && item.isAllowPlacement();

            // Nothing/Unknown - deny
            case NOTHING -> false;
            default -> false;
        };
    }

    private void handlePlayerInventoryClickWhileMenuOpen(@NonNull InventoryClickEvent event, @NonNull Inventory menuInventory) {
        // Player clicked their own inventory while menu is open
        // Allow normal inventory interactions (pickup/place items on cursor)
        // But prevent actions that move items into the menu

        switch (event.getAction()) {
            // Block actions that automatically move items into the menu
            case MOVE_TO_OTHER_INVENTORY, COLLECT_TO_CURSOR -> {    // Shift-click, Double-click to collect
                event.setCancelled(true);
                this.plugin.getLogger().fine("Blocked shift-click/collect while menu open");
            }

            // Block hotbar swaps that could affect menu slots
            case HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> {
                // Only block if it would affect the menu inventory
                int hotbarButton = event.getHotbarButton();
                if ((hotbarButton >= 0) && (hotbarButton <= 8)) {
                    event.setCancelled(true);
                    this.plugin.getLogger().fine("Blocked hotbar swap while menu open");
                }
            }

            // Allow all other actions in player inventory (PICKUP, PLACE, etc.)
            default -> {
                // Don't cancel - allow normal inventory interactions
            }
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
        ViewerState viewerState = menu.getViewerState(player.getUniqueId());
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
        if (holder instanceof Menu menu) {
            return menu;
        }
        return null;
    }

    /**
     * Finds a menu item at the specified global slot.
     * Searches all panes in the menu using per-player context.
     */
    private MenuItem findMenuItemAtSlot(@NonNull Menu menu, int globalSlot, @NonNull HumanEntity player) {
        MenuContext context = new MenuContext(menu, player);
        for (Pane pane : menu.getPanes().values()) {
            MenuItem item = pane.getItemByGlobalSlot(globalSlot, context);
            if (item != null) {
                return item;
            }
        }
        return null;
    }
}
