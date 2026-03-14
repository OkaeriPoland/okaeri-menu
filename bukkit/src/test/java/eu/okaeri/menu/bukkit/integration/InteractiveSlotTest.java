package eu.okaeri.menu.bukkit.integration;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.item.MenuItemContext;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static eu.okaeri.menu.item.MenuItem.item;
import static eu.okaeri.menu.pane.StaticPane.staticPane;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for interactive slot behavior in MenuListener.
 * Tests pickup, placement, swap actions, and item change handlers.
 */
class InteractiveSlotTest extends MenuListenerTestBase {

    @Test
    @DisplayName("Should allow pickup from interactive slot")
    void testInteractiveSlotPickup() {
        AtomicBoolean changeHandlerCalled = new AtomicBoolean(false);

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane(staticPane("test")
                .bounds(0, 0, 1, 9)
                .item(0, 0, item()
                    .allowPickup(true)
                    .onItemChange(ctx -> changeHandlerCalled.set(true))
                    .build())
                .build())
            .build();

        menu.open(this.player);

        // Place item in slot first
        Inventory inv = this.player.getOpenInventory().getTopInventory();
        inv.setItem(0, new ItemStack(Material.DIAMOND, 1));

        // Simulate pickup
        InventoryClickEvent event = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            0,
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        );

        this.listener.onInventoryClick(event);

        // Event should NOT be cancelled (pickup allowed)
        assertThat(event.isCancelled()).isFalse();
        assertThat(changeHandlerCalled.get()).isTrue();
    }

    @Test
    @DisplayName("Should allow placement in interactive slot")
    void testInteractiveSlotPlacement() {
        AtomicBoolean changeHandlerCalled = new AtomicBoolean(false);

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane(staticPane("test")
                .bounds(0, 0, 1, 9)
                .item(0, 0, item()
                    .allowPlacement(true)
                    .onItemChange(ctx -> changeHandlerCalled.set(true))
                    .build())
                .build())
            .build();

        menu.open(this.player);

        // Simulate placement
        InventoryClickEvent event = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            0,
            ClickType.LEFT,
            InventoryAction.PLACE_ALL
        );

        this.listener.onInventoryClick(event);

        // Event should NOT be cancelled (placement allowed)
        assertThat(event.isCancelled()).isFalse();
        assertThat(changeHandlerCalled.get()).isTrue();
    }

    @Test
    @DisplayName("Should block pickup from non-interactive slot")
    void testBlockPickupFromNonInteractiveSlot() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane(staticPane("test")
                .bounds(0, 0, 1, 9)
                .item(0, 0, item()
                    .material(Material.DIAMOND)
                    .build())
                .build())
            .build();

        menu.open(this.player);

        InventoryClickEvent event = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            0,
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        );

        this.listener.onInventoryClick(event);

        // Event should be cancelled (not interactive)
        assertThat(event.isCancelled()).isTrue();
    }

    @Test
    @DisplayName("Should block placement when only pickup allowed")
    void testBlockPlacementWhenOnlyPickup() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane(staticPane("test")
                .bounds(0, 0, 1, 9)
                .item(0, 0, item()
                    .allowPickup(true)
                    // No allowPlacement
                    .build())
                .build())
            .build();

        menu.open(this.player);

        InventoryClickEvent event = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            0,
            ClickType.LEFT,
            InventoryAction.PLACE_ALL
        );

        this.listener.onInventoryClick(event);

        // Event should be cancelled (placement not allowed)
        assertThat(event.isCancelled()).isTrue();
    }

    @Test
    @DisplayName("Should provide correct before/after items to change handler")
    void testInteractiveSlotChangeContext() {
        AtomicReference<ItemStack> capturedBefore = new AtomicReference<>();
        AtomicReference<ItemStack> capturedAfter = new AtomicReference<>();

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane(staticPane("test")
                .bounds(0, 0, 1, 9)
                .item(0, 0, item()
                    .interactive()
                    .onItemChange(ctx -> {
                        capturedBefore.set(ctx.getPreviousItem());
                        capturedAfter.set(ctx.getNewItem());
                    })
                    .build())
                .build())
            .build();

        menu.open(this.player);

        // Place gold ingot in slot
        Inventory inv = this.player.getOpenInventory().getTopInventory();
        inv.setItem(0, new ItemStack(Material.GOLD_INGOT, 5));

        // Simulate pickup (removes item)
        InventoryClickEvent event = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            0,
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        );

        this.listener.onInventoryClick(event);

        // Should have captured the before (gold) and after (null)
        assertThat(capturedBefore.get()).isNotNull();
        assertThat(capturedBefore.get().getType()).isEqualTo(Material.GOLD_INGOT);
        assertThat(capturedBefore.get().getAmount()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should handle swap action in interactive slot")
    void testInteractiveSlotSwap() {
        AtomicBoolean swapDetected = new AtomicBoolean(false);

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane(staticPane("test")
                .bounds(0, 0, 1, 9)
                .item(0, 0, item()
                    .interactive()  // Both pickup and placement allowed
                    .onItemChange(ctx -> {
                        if (ctx.wasItemSwapped()) {
                            swapDetected.set(true);
                        }
                    })
                    .build())
                .build())
            .build();

        menu.open(this.player);

        // Place initial item
        Inventory inv = this.player.getOpenInventory().getTopInventory();
        inv.setItem(0, new ItemStack(Material.GOLD_INGOT, 1));

        // Simulate swap action
        InventoryClickEvent event = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            0,
            ClickType.LEFT,
            InventoryAction.SWAP_WITH_CURSOR
        );

        this.listener.onInventoryClick(event);

        // Swap requires both pickup and placement
        assertThat(event.isCancelled()).isFalse();
    }

    @Test
    @DisplayName("Should block swap when only pickup allowed")
    void testBlockSwapWhenOnlyPickup() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane(staticPane("test")
                .bounds(0, 0, 1, 9)
                .item(0, 0, item()
                    .allowPickup(true)
                    // No allowPlacement
                    .build())
                .build())
            .build();

        menu.open(this.player);

        InventoryClickEvent event = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            0,
            ClickType.LEFT,
            InventoryAction.SWAP_WITH_CURSOR
        );

        this.listener.onInventoryClick(event);

        // Should be cancelled (swap requires both)
        assertThat(event.isCancelled()).isTrue();
    }

    @Test
    @DisplayName("Should allow cancellation from change handler")
    void testChangeHandlerCanCancel() {
        // Handler cancels the event
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane(staticPane("test")
                .bounds(0, 0, 1, 9)
                .item(0, 0, item()
                    .interactive()
                    .onItemChange(MenuItemContext::cancel)
                    .build())
                .build())
            .build();

        menu.open(this.player);

        InventoryClickEvent event = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            0,
            ClickType.LEFT,
            InventoryAction.PLACE_ALL
        );

        this.listener.onInventoryClick(event);

        // Event should be cancelled by handler
        assertThat(event.isCancelled()).isTrue();
    }

    // ========================================
    // SHIFT-CLICK FROM PLAYER INVENTORY
    // ========================================

    @Test
    @DisplayName("Should route shift-click to first empty interactive placement slot")
    void testShiftClickRoutesToInteractiveSlot() {
        AtomicBoolean changeHandlerCalled = new AtomicBoolean(false);

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane(staticPane("test")
                .bounds(0, 0, 1, 9)
                .item(0, 0, item().material(Material.BARRIER).build())  // Non-interactive
                .item(0, 1, item()
                    .interactive()
                    .onItemChange(ctx -> changeHandlerCalled.set(true))
                    .build())
                .build())
            .build();

        menu.open(this.player);

        // Place item in player inventory to shift-click
        this.player.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 5));

        // Simulate shift-click from player inventory (slot 9+ is player inventory in the view)
        InventoryClickEvent event = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            9,  // Player inventory slot
            ClickType.SHIFT_LEFT,
            InventoryAction.MOVE_TO_OTHER_INVENTORY
        );

        this.listener.onInventoryClick(event);

        // Event is cancelled (we handle it manually)
        assertThat(event.isCancelled()).isTrue();
        // Item should have been moved to slot 1 (first interactive slot)
        Inventory topInv = this.player.getOpenInventory().getTopInventory();
        assertThat(topInv.getItem(1)).isNotNull();
        assertThat(topInv.getItem(1).getType()).isEqualTo(Material.DIAMOND);
        // Change handler should have been called
        assertThat(changeHandlerCalled.get()).isTrue();
    }

    @Test
    @DisplayName("Should block shift-click when no interactive slots exist")
    void testShiftClickBlockedWithoutInteractiveSlots() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane(staticPane("test")
                .bounds(0, 0, 1, 9)
                .item(0, 0, item().material(Material.BARRIER).build())
                .build())
            .build();

        menu.open(this.player);

        this.player.getInventory().setItem(0, new ItemStack(Material.DIAMOND));

        InventoryClickEvent event = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            9,
            ClickType.SHIFT_LEFT,
            InventoryAction.MOVE_TO_OTHER_INVENTORY
        );

        this.listener.onInventoryClick(event);

        // Should be cancelled with no item moved
        assertThat(event.isCancelled()).isTrue();
        Inventory topInv = this.player.getOpenInventory().getTopInventory();
        assertThat(topInv.getItem(1)).isNull();
    }

    @Test
    @DisplayName("Should skip occupied interactive slots during shift-click")
    void testShiftClickSkipsOccupiedSlots() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane(staticPane("test")
                .bounds(0, 0, 1, 9)
                .item(0, 0, item().interactive().build())
                .item(0, 1, item().interactive().build())
                .build())
            .build();

        menu.open(this.player);

        // Occupy first interactive slot
        Inventory topInv = this.player.getOpenInventory().getTopInventory();
        topInv.setItem(0, new ItemStack(Material.GOLD_INGOT));

        // Shift-click diamond from player inventory
        this.player.getInventory().setItem(0, new ItemStack(Material.DIAMOND));

        InventoryClickEvent event = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            9,
            ClickType.SHIFT_LEFT,
            InventoryAction.MOVE_TO_OTHER_INVENTORY
        );

        this.listener.onInventoryClick(event);

        // Diamond should go to slot 1 (slot 0 is occupied)
        assertThat(topInv.getItem(0).getType()).isEqualTo(Material.GOLD_INGOT);  // Unchanged
        assertThat(topInv.getItem(1)).isNotNull();
        assertThat(topInv.getItem(1).getType()).isEqualTo(Material.DIAMOND);
    }

    @Test
    @DisplayName("Should still block collect-to-cursor even with interactive slots")
    void testCollectToCursorStillBlocked() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane(staticPane("test")
                .bounds(0, 0, 1, 9)
                .item(0, 0, item().interactive().build())
                .build())
            .build();

        menu.open(this.player);

        InventoryClickEvent event = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            9,
            ClickType.LEFT,
            InventoryAction.COLLECT_TO_CURSOR
        );

        this.listener.onInventoryClick(event);

        assertThat(event.isCancelled()).isTrue();
    }

    // ========================================
    // INTERACTIVE SLOT INITIAL ITEMS
    // ========================================

    @Test
    @DisplayName("Should show initial item in interactive slot after menu open")
    void testInteractiveSlotInitialItemOnOpen() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane(staticPane("test")
                .bounds(0, 0, 1, 9)
                .item(0, 0, item()
                    .from(new ItemStack(Material.EMERALD))
                    .interactive()
                    .build())
                .build())
            .build();

        menu.open(this.player);

        Inventory inv = this.player.getOpenInventory().getTopInventory();
        assertThat(inv.getItem(0)).isNotNull();
        assertThat(inv.getItem(0).getType()).isEqualTo(Material.EMERALD);
    }

    @Test
    @DisplayName("Should preserve player-placed items after refresh")
    void testInteractiveSlotPreservesItemsAfterRefresh() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane(staticPane("test")
                .bounds(0, 0, 1, 9)
                .item(0, 0, item().interactive().build())
                .build())
            .build();

        menu.open(this.player);

        // Place item
        Inventory inv = this.player.getOpenInventory().getTopInventory();
        inv.setItem(0, new ItemStack(Material.DIAMOND_SWORD));

        // Refresh
        menu.refresh(this.player);

        // Item should be preserved
        assertThat(inv.getItem(0)).isNotNull();
        assertThat(inv.getItem(0).getType()).isEqualTo(Material.DIAMOND_SWORD);
    }
}
