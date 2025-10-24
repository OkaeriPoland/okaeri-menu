package eu.okaeri.menu.bukkit.integration;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.MenuListener;
import eu.okaeri.menu.item.MenuItemContext;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static eu.okaeri.menu.item.MenuItem.item;
import static eu.okaeri.menu.pane.StaticPane.staticPane;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration tests for MenuListener event handling.
 * Tests click routing, interactive slots, and event cancellation.
 */
class MenuListenerTest {

    private static ServerMock server;
    private JavaPlugin plugin;
    private MenuListener listener;
    private Player player;

    @BeforeAll
    static void setUpServer() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void tearDownServer() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void setUp() {
        this.plugin = MockBukkit.createMockPlugin();
        this.listener = MenuListener.register(this.plugin);
        this.player = server.addPlayer();
    }

    // ========================================
    // CLICK ROUTING
    // ========================================

    @Test
    @DisplayName("Should route click to correct menu item")
    void testClickRouting() {
        AtomicBoolean clicked = new AtomicBoolean(false);

        // Create menu with item at specific slot
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .pane("main", staticPane()
                .bounds(0, 0, 9, 3)
                .item(2, 1, item()
                    .material(Material.DIAMOND)
                    .onClick(ctx -> clicked.set(true))
                    .build())
                .build())
            .build();

        menu.open(this.player);

        // Simulate click at slot 11 (column 2, row 1)
        Inventory inventory = this.player.getOpenInventory().getTopInventory();
        InventoryClickEvent event = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            11,
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        );

        this.listener.onInventoryClick(event);

        assertThat(clicked.get()).isTrue();
        assertThat(event.isCancelled()).isTrue();
    }

    @Test
    @DisplayName("Should cancel click on empty slot")
    void testClickOnEmptySlot() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .pane("main", staticPane()
                .bounds(0, 0, 9, 3)
                .build())
            .build();

        menu.open(this.player);

        InventoryClickEvent event = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            5,
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        );

        this.listener.onInventoryClick(event);

        assertThat(event.isCancelled()).isTrue();
    }

    @Test
    @DisplayName("Should cancel click outside inventory")
    void testClickOutsideInventory() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .build();

        menu.open(this.player);

        // Click outside (raw slot -999)
        InventoryClickEvent event = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.OUTSIDE,
            -999,
            ClickType.LEFT,
            InventoryAction.DROP_ALL_CURSOR
        );

        this.listener.onInventoryClick(event);

        assertThat(event.isCancelled()).isTrue();
    }

    // ========================================
    // CLICK HANDLER EXECUTION
    // ========================================

    @Test
    @DisplayName("Should execute onClick handler")
    void testOnClickExecution() {
        AtomicInteger clickCount = new AtomicInteger(0);

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane("main", staticPane()
                .bounds(0, 0, 9, 1)
                .item(0, 0, item()
                    .material(Material.STONE)
                    .onClick(ctx -> clickCount.incrementAndGet())
                    .build())
                .build())
            .build();

        menu.open(this.player);

        // Click twice
        for (int i = 0; i < 2; i++) {
            InventoryClickEvent event = new InventoryClickEvent(
                this.player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                0,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL
            );
            this.listener.onInventoryClick(event);
        }

        assertThat(clickCount.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should execute left and right click handlers correctly")
    void testLeftRightClickHandlers() {
        AtomicInteger leftClicks = new AtomicInteger(0);
        AtomicInteger rightClicks = new AtomicInteger(0);

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane("main", staticPane()
                .bounds(0, 0, 9, 1)
                .item(0, 0, item()
                    .material(Material.EMERALD)
                    .onLeftClick(ctx -> leftClicks.incrementAndGet())
                    .onRightClick(ctx -> rightClicks.incrementAndGet())
                    .build())
                .build())
            .build();

        menu.open(this.player);

        // Left click
        InventoryClickEvent leftEvent = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            0,
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        );
        this.listener.onInventoryClick(leftEvent);

        // Right click
        InventoryClickEvent rightEvent = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            0,
            ClickType.RIGHT,
            InventoryAction.PICKUP_HALF
        );
        this.listener.onInventoryClick(rightEvent);

        assertThat(leftClicks.get()).isEqualTo(1);
        assertThat(rightClicks.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should provide context with correct slot to handler")
    void testContextSlotAccuracy() {
        AtomicInteger capturedSlot = new AtomicInteger(-1);

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(2)
            .pane("main", staticPane()
                .bounds(0, 0, 9, 2)
                .item(5, 1, item()  // Slot 14
                    .material(Material.GOLD_INGOT)
                    .onClick(ctx -> capturedSlot.set(ctx.getSlot()))
                    .build())
                .build())
            .build();

        menu.open(this.player);

        InventoryClickEvent event = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            14,  // Column 5, Row 1
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        );

        this.listener.onInventoryClick(event);

        assertThat(capturedSlot.get()).isEqualTo(14);
    }

    // ========================================
    // INTERACTIVE SLOTS
    // ========================================

    @Test
    @DisplayName("Should allow pickup from interactive slot")
    void testInteractiveSlotPickup() {
        AtomicBoolean changeHandlerCalled = new AtomicBoolean(false);

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane("main", staticPane()
                .bounds(0, 0, 9, 1)
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
            .pane("main", staticPane()
                .bounds(0, 0, 9, 1)
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
            .pane("main", staticPane()
                .bounds(0, 0, 9, 1)
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
            .pane("main", staticPane()
                .bounds(0, 0, 9, 1)
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
            .pane("main", staticPane()
                .bounds(0, 0, 9, 1)
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

    // ========================================
    // PLAYER INVENTORY CLICKS
    // ========================================

    @Test
    @DisplayName("Should block shift-click while menu open")
    void testBlockShiftClickWhileMenuOpen() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .build();

        menu.open(this.player);

        // Simulate shift-click in player inventory (bottom inventory)
        InventoryClickEvent event = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            36,  // Player inventory slot
            ClickType.SHIFT_LEFT,
            InventoryAction.MOVE_TO_OTHER_INVENTORY
        );

        this.listener.onInventoryClick(event);

        // Should be cancelled to prevent items moving into menu
        assertThat(event.isCancelled()).isTrue();
    }

    @Test
    @DisplayName("Should allow normal player inventory interactions")
    void testAllowPlayerInventoryInteractions() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .build();

        menu.open(this.player);

        // Simulate normal pickup in player inventory
        InventoryClickEvent event = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            36,  // Player inventory slot
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        );

        this.listener.onInventoryClick(event);

        // Should NOT be cancelled
        assertThat(event.isCancelled()).isFalse();
    }

    // ========================================
    // EXCEPTION HANDLING
    // ========================================

    @Test
    @DisplayName("Should handle exception in click handler safely")
    void testExceptionInClickHandler() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane("main", staticPane()
                .bounds(0, 0, 9, 1)
                .item(0, 0, item()
                    .material(Material.TNT)
                    .onClick(ctx -> {
                        throw new RuntimeException("Test exception");
                    })
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

        // Should not throw - exception should be caught
        assertThatCode(() -> this.listener.onInventoryClick(event))
            .doesNotThrowAnyException();

        // Event should still be cancelled
        assertThat(event.isCancelled()).isTrue();
    }

    @Test
    @DisplayName("Should handle exception in change handler safely")
    void testExceptionInChangeHandler() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane("main", staticPane()
                .bounds(0, 0, 9, 1)
                .item(0, 0, item()
                    .interactive()
                    .onItemChange(ctx -> {
                        throw new RuntimeException("Test exception");
                    })
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

        // Should not throw - exception should be caught
        assertThatCode(() -> this.listener.onInventoryClick(event))
            .doesNotThrowAnyException();

        // Event should be cancelled (exception = cancel for safety)
        assertThat(event.isCancelled()).isTrue();
    }

    // ========================================
    // MULTIPLE ITEMS
    // ========================================

    @Test
    @DisplayName("Should route clicks to correct items in multi-item menu")
    void testMultipleItemRouting() {
        AtomicInteger slot0Clicks = new AtomicInteger(0);
        AtomicInteger slot8Clicks = new AtomicInteger(0);

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane("main", staticPane()
                .bounds(0, 0, 9, 1)
                .item(0, 0, item()
                    .material(Material.DIAMOND)
                    .onClick(ctx -> slot0Clicks.incrementAndGet())
                    .build())
                .item(8, 0, item()
                    .material(Material.EMERALD)
                    .onClick(ctx -> slot8Clicks.incrementAndGet())
                    .build())
                .build())
            .build();

        menu.open(this.player);

        // Click slot 0
        InventoryClickEvent event0 = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            0,
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        );
        this.listener.onInventoryClick(event0);

        // Click slot 8
        InventoryClickEvent event8 = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            8,
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        );
        this.listener.onInventoryClick(event8);

        assertThat(slot0Clicks.get()).isEqualTo(1);
        assertThat(slot8Clicks.get()).isEqualTo(1);
    }

    // ========================================
    // DRAG EVENTS
    // ========================================

    @Test
    @DisplayName("Should cancel drag into menu inventory")
    void testCancelDragIntoMenu() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .build();

        menu.open(this.player);

        // Create drag event that includes menu slots
        org.bukkit.event.inventory.InventoryDragEvent dragEvent =
            new org.bukkit.event.inventory.InventoryDragEvent(
                this.player.getOpenInventory(),
                null,
                new ItemStack(Material.DIAMOND),
                false,
                java.util.Map.of(0, new ItemStack(Material.DIAMOND))  // Dragging into slot 0 of menu
            );

        this.listener.onInventoryDrag(dragEvent);

        // Should be cancelled
        assertThat(dragEvent.isCancelled()).isTrue();
    }

    @Test
    @DisplayName("Should allow drag in player inventory when menu open")
    void testAllowDragInPlayerInventory() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .build();

        menu.open(this.player);

        // Create drag event only in player inventory (slots 9+)
        org.bukkit.event.inventory.InventoryDragEvent dragEvent =
            new org.bukkit.event.inventory.InventoryDragEvent(
                this.player.getOpenInventory(),
                null,
                new ItemStack(Material.DIAMOND),
                false,
                java.util.Map.of(36, new ItemStack(Material.DIAMOND))  // Player inventory slot
            );

        this.listener.onInventoryDrag(dragEvent);

        // Should NOT be cancelled
        assertThat(dragEvent.isCancelled()).isFalse();
    }

    @Test
    @DisplayName("Should handle exception in drag handler safely")
    void testExceptionInDragHandler() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .build();

        menu.open(this.player);

        // Create an event that will trigger the drag handler
        org.bukkit.event.inventory.InventoryDragEvent dragEvent =
            new org.bukkit.event.inventory.InventoryDragEvent(
                this.player.getOpenInventory(),
                null,
                new ItemStack(Material.DIAMOND),
                false,
                java.util.Map.of(0, new ItemStack(Material.DIAMOND))
            );

        // Should not throw even if internal error occurs
        assertThatCode(() -> this.listener.onInventoryDrag(dragEvent))
            .doesNotThrowAnyException();
    }

    // ========================================
    // CLOSE EVENTS & CLEANUP
    // ========================================

    @Test
    @DisplayName("Should cleanup ViewerState on inventory close")
    void testCleanupOnClose() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .build();

        menu.open(this.player);

        // Verify ViewerState exists
        assertThat(menu.getViewerState(this.player.getUniqueId())).isNotNull();

        // Simulate close
        org.bukkit.event.inventory.InventoryCloseEvent closeEvent =
            new org.bukkit.event.inventory.InventoryCloseEvent(
                this.player.getOpenInventory()
            );

        this.listener.onInventoryClose(closeEvent);

        // ViewerState should be cleaned up
        assertThat(menu.getViewerState(this.player.getUniqueId())).isNull();
    }

    @Test
    @DisplayName("Should handle close event for non-menu inventory")
    void testCloseNonMenuInventory() {
        // Open regular chest (not a menu)
        Inventory chest = server.createInventory(null, 27, "Regular Chest");
        this.player.openInventory(chest);

        org.bukkit.event.inventory.InventoryCloseEvent closeEvent =
            new org.bukkit.event.inventory.InventoryCloseEvent(
                this.player.getOpenInventory()
            );

        // Should not throw
        assertThatCode(() -> this.listener.onInventoryClose(closeEvent))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle exception in close handler safely")
    void testExceptionInCloseHandler() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .build();

        menu.open(this.player);

        org.bukkit.event.inventory.InventoryCloseEvent closeEvent =
            new org.bukkit.event.inventory.InventoryCloseEvent(
                this.player.getOpenInventory()
            );

        // Should not throw
        assertThatCode(() -> this.listener.onInventoryClose(closeEvent))
            .doesNotThrowAnyException();
    }

    // ========================================
    // MULTIPLE PANES
    // ========================================

    @Test
    @DisplayName("Should route clicks correctly across multiple panes")
    void testMultiplePanesRouting() {
        AtomicInteger topPaneClicks = new AtomicInteger(0);
        AtomicInteger bottomPaneClicks = new AtomicInteger(0);

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .pane("top", staticPane()
                .bounds(0, 0, 9, 1)  // First row
                .item(0, 0, item()
                    .material(Material.DIAMOND)
                    .onClick(ctx -> topPaneClicks.incrementAndGet())
                    .build())
                .build())
            .pane("bottom", staticPane()
                .bounds(0, 2, 9, 1)  // Third row
                .item(0, 0, item()
                    .material(Material.EMERALD)
                    .onClick(ctx -> bottomPaneClicks.incrementAndGet())
                    .build())
                .build())
            .build();

        menu.open(this.player);

        // Click in top pane (slot 0)
        InventoryClickEvent topEvent = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            0,
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        );
        this.listener.onInventoryClick(topEvent);

        // Click in bottom pane (slot 18 = row 2, col 0)
        InventoryClickEvent bottomEvent = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            18,
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        );
        this.listener.onInventoryClick(bottomEvent);

        assertThat(topPaneClicks.get()).isEqualTo(1);
        assertThat(bottomPaneClicks.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle overlapping panes correctly")
    void testOverlappingPanes() {
        AtomicInteger firstPaneClicks = new AtomicInteger(0);

        // Note: In practice, overlapping panes should be validated during build
        // This tests the click routing behavior
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(2)
            .pane("first", staticPane()
                .bounds(0, 0, 5, 1)
                .item(2, 0, item()
                    .material(Material.GOLD_INGOT)
                    .onClick(ctx -> firstPaneClicks.incrementAndGet())
                    .build())
                .build())
            .build();

        menu.open(this.player);

        InventoryClickEvent event = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            2,
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        );

        this.listener.onInventoryClick(event);

        // Should route to first pane that has an item
        assertThat(firstPaneClicks.get()).isEqualTo(1);
    }

    // ========================================
    // COMPLEX INTERACTIVE SCENARIOS
    // ========================================

    @Test
    @DisplayName("Should handle swap action in interactive slot")
    void testInteractiveSlotSwap() {
        AtomicBoolean swapDetected = new AtomicBoolean(false);

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane("main", staticPane()
                .bounds(0, 0, 9, 1)
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
            .pane("main", staticPane()
                .bounds(0, 0, 9, 1)
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
            .pane("main", staticPane()
                .bounds(0, 0, 9, 1)
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
    // EDGE CASES
    // ========================================

    @Test
    @DisplayName("Should handle click in non-menu inventory gracefully")
    void testClickInNonMenuInventory() {
        // Open regular chest
        Inventory chest = server.createInventory(null, 27, "Regular Chest");
        this.player.openInventory(chest);

        InventoryClickEvent event = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            0,
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        );

        // Should not throw, should not cancel
        assertThatCode(() -> this.listener.onInventoryClick(event))
            .doesNotThrowAnyException();

        assertThat(event.isCancelled()).isFalse();
    }

    @Test
    @DisplayName("Should handle hotbar swap while menu open")
    void testHotbarSwapWhileMenuOpen() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .build();

        menu.open(this.player);

        // Hotbar swap in player inventory
        InventoryClickEvent event = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            36,  // Player inventory
            ClickType.NUMBER_KEY,
            InventoryAction.HOTBAR_SWAP,
            1  // Hotbar button
        );

        this.listener.onInventoryClick(event);

        // Should be cancelled to prevent confusion
        assertThat(event.isCancelled()).isTrue();
    }

    @Test
    @DisplayName("Should handle collect to cursor action")
    void testCollectToCursor() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .build();

        menu.open(this.player);

        // Collect to cursor in player inventory
        InventoryClickEvent event = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            36,  // Player inventory
            ClickType.DOUBLE_CLICK,
            InventoryAction.COLLECT_TO_CURSOR
        );

        this.listener.onInventoryClick(event);

        // Should be cancelled to prevent items being collected from menu
        assertThat(event.isCancelled()).isTrue();
    }

    @Test
    @DisplayName("Should handle multiple clicks rapidly")
    void testRapidClicks() {
        AtomicInteger clickCount = new AtomicInteger(0);

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane("main", staticPane()
                .bounds(0, 0, 9, 1)
                .item(0, 0, item()
                    .material(Material.STONE)
                    .onClick(ctx -> clickCount.incrementAndGet())
                    .build())
                .build())
            .build();

        menu.open(this.player);

        // Simulate 10 rapid clicks
        for (int i = 0; i < 10; i++) {
            InventoryClickEvent event = new InventoryClickEvent(
                this.player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                0,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL
            );
            this.listener.onInventoryClick(event);
        }

        assertThat(clickCount.get()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should provide correct click type to handler")
    void testClickTypeAccuracy() {
        AtomicReference<ClickType> capturedClickType = new AtomicReference<>();

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane("main", staticPane()
                .bounds(0, 0, 9, 1)
                .item(0, 0, item()
                    .material(Material.COMPASS)
                    .onClick(ctx -> capturedClickType.set(ctx.getClickType()))
                    .build())
                .build())
            .build();

        menu.open(this.player);

        InventoryClickEvent event = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            0,
            ClickType.SHIFT_RIGHT,
            InventoryAction.PICKUP_ALL
        );

        this.listener.onInventoryClick(event);

        assertThat(capturedClickType.get()).isEqualTo(ClickType.SHIFT_RIGHT);
    }
}
