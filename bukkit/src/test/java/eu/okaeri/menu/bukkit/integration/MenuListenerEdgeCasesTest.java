package eu.okaeri.menu.bukkit.integration;

import eu.okaeri.menu.Menu;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static eu.okaeri.menu.item.MenuItem.item;
import static eu.okaeri.menu.pane.StaticPane.staticPane;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration tests for MenuListener edge cases and exception handling.
 * Tests player inventory interactions, exception safety, and unusual click patterns.
 */
class MenuListenerEdgeCasesTest extends MenuListenerTestBase {

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
            .pane(staticPane("test")
                .bounds(0, 0, 9, 1)
                .item(0, 0, item()
                    .material(Material.TNT)
                    .onClick(event -> {
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
            .pane(staticPane("test")
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
    // EDGE CASES
    // ========================================

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
            .pane(staticPane("test")
                .bounds(0, 0, 9, 1)
                .item(0, 0, item()
                    .material(Material.STONE)
                    .onClick(event -> clickCount.incrementAndGet())
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
            .pane(staticPane("test")
                .bounds(0, 0, 9, 1)
                .item(0, 0, item()
                    .material(Material.COMPASS)
                    .onClick(event -> capturedClickType.set(event.getClickType()))
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
            .pane(staticPane("test")
                .bounds(0, 0, 9, 1)
                .item(0, 0, item()
                    .material(Material.DIAMOND)
                    .onClick(event -> slot0Clicks.incrementAndGet())
                    .build())
                .item(8, 0, item()
                    .material(Material.EMERALD)
                    .onClick(event -> slot8Clicks.incrementAndGet())
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
    // FAIL-SAFE EXCEPTION HANDLING
    // ========================================

    @Test
    @DisplayName("Should cancel event and prevent item duplication when routing logic crashes")
    void testFailSafeClickExceptionPreventsItemDuplication() throws Exception {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane(staticPane("test")
                .bounds(0, 0, 9, 1)
                .item(0, 0, item()
                    .material(Material.DIAMOND)
                    .build())
                .build())
            .build();

        menu.open(this.player);

        // Corrupt the menu's internal panes map using reflection to trigger NPE in routing logic
        // This simulates a corrupted state that could occur from concurrent modification or bugs
        Field panesField = Menu.class.getDeclaredField("panes");
        panesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ?> panes = (Map<String, ?>) panesField.get(menu);
        panes.put("corrupted", null);  // Inject null pane - will cause NPE when iterating

        InventoryClickEvent event = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            0,
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        );

        // CRITICAL: The outer fail-safe catch block should handle the exception
        assertThatCode(() -> this.listener.onInventoryClick(event))
            .as("Outer fail-safe should catch routing exceptions without crashing")
            .doesNotThrowAnyException();

        // CRITICAL: Event MUST be cancelled to prevent item duplication
        assertThat(event.isCancelled())
            .as("Event must be cancelled on routing exception to prevent item duplication/loss")
            .isTrue();
    }
}
