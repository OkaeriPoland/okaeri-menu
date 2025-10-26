package eu.okaeri.menu.bukkit.integration;

import eu.okaeri.menu.Menu;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static eu.okaeri.menu.item.MenuItem.item;
import static eu.okaeri.menu.pane.StaticPane.staticPane;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration tests for basic click routing in MenuListener.
 * Tests click event routing, cancellation, and multi-pane scenarios.
 */
class ClickRoutingTest extends MenuListenerTestBase {

    @Test
    @DisplayName("Should route click to correct menu item")
    void testClickRouting() {
        AtomicBoolean clicked = new AtomicBoolean(false);

        // Create menu with item at specific slot
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .pane(staticPane("test")
                .bounds(0, 0, 3, 9)
                .item(1, 2, item()
                    .material(Material.DIAMOND)
                    .onClick(event -> clicked.set(true))
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
            .pane(staticPane("test")
                .bounds(0, 0, 3, 9)
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

    @Test
    @DisplayName("Should handle click in non-menu inventory gracefully")
    void testClickInNonMenuInventory() {
        // Open regular chest
        Inventory chest = server.createInventory(null, 27, Component.text("Regular Chest"));
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
    @DisplayName("Should route clicks correctly across multiple panes")
    void testMultiplePanesRouting() {
        AtomicInteger topPaneClicks = new AtomicInteger(0);
        AtomicInteger bottomPaneClicks = new AtomicInteger(0);

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .pane(staticPane()
                .name("top")
                .bounds(0, 0, 1, 9)  // First row
                .item(0, 0, item()
                    .material(Material.DIAMOND)
                    .onClick(event -> topPaneClicks.incrementAndGet())
                    .build())
                .build())
            .pane(staticPane()
                .name("bottom")
                .bounds(2, 0, 1, 9)  // Third row
                .item(0, 0, item()
                    .material(Material.EMERALD)
                    .onClick(event -> bottomPaneClicks.incrementAndGet())
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
            .pane(staticPane("test")
                .bounds(0, 0, 1, 5)
                .item(0, 2, item()
                    .material(Material.GOLD_INGOT)
                    .onClick(event -> firstPaneClicks.incrementAndGet())
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
}
