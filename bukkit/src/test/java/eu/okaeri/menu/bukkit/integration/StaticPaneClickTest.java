package eu.okaeri.menu.bukkit.integration;

import eu.okaeri.menu.Menu;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static eu.okaeri.menu.item.MenuItem.item;
import static eu.okaeri.menu.pane.StaticPane.staticPane;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for StaticPane click routing with auto-positioned items.
 * Tests auto-item routing, visibility, reflow, and mixed static/auto scenarios.
 */
class StaticPaneClickTest extends MenuListenerTestBase {

    @Test
    @DisplayName("Should route clicks to auto-positioned items")
    void testAutoItemClickRouting() {
        AtomicInteger clickedItemId = new AtomicInteger(-1);

        Menu menu = Menu.builder(this.plugin)
            .title("Auto Item Test")
            .rows(1)
            .pane(staticPane("test")
                .bounds(0, 0, 9, 1)
                .item(item()
                    .material(Material.DIAMOND)
                    .onClick(event -> clickedItemId.set(1))
                    .build())
                .item(item()
                    .material(Material.GOLD_INGOT)
                    .onClick(event -> clickedItemId.set(2))
                    .build())
                .item(item()
                    .material(Material.EMERALD)
                    .onClick(event -> clickedItemId.set(3))
                    .build())
                .build())
            .build();

        menu.open(this.player);

        // Click auto-item 1 (slot 0)
        InventoryClickEvent event1 = this.createLeftClick(this.player, 0);
        this.listener.onInventoryClick(event1);
        assertThat(clickedItemId.get()).isEqualTo(1);

        // Click auto-item 2 (slot 1)
        clickedItemId.set(-1);
        InventoryClickEvent event2 = this.createLeftClick(this.player, 1);
        this.listener.onInventoryClick(event2);
        assertThat(clickedItemId.get()).isEqualTo(2);

        // Click auto-item 3 (slot 2)
        clickedItemId.set(-1);
        InventoryClickEvent event3 = this.createLeftClick(this.player, 2);
        this.listener.onInventoryClick(event3);
        assertThat(clickedItemId.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should not route clicks to invisible auto-items")
    void testInvisibleAutoItemNoClick() {
        AtomicBoolean clicked = new AtomicBoolean(false);

        Menu menu = Menu.builder(this.plugin)
            .title("Invisible Test")
            .rows(1)
            .pane(staticPane("test")
                .bounds(0, 0, 9, 1)
                .item(item()
                    .material(Material.DIAMOND)
                    .visible(false)
                    .onClick(event -> clicked.set(true))
                    .build())
                .build())
            .build();

        menu.open(this.player);

        // Click where invisible item would be
        InventoryClickEvent event = this.createLeftClick(this.player, 0);
        this.listener.onInventoryClick(event);

        // Should not trigger onClick (item is invisible)
        assertThat(clicked.get()).isFalse();
        assertThat(event.isCancelled()).isTrue();  // But still cancelled (empty menu slot)
    }

    @Test
    @DisplayName("Should route clicks to reflowed auto-items after visibility change")
    void testReflowedAutoItemClickRouting() {
        AtomicBoolean item1Visible = new AtomicBoolean(true);
        AtomicInteger clickedItemId = new AtomicInteger(-1);

        Menu menu = Menu.builder(this.plugin)
            .title("Reflow Test")
            .rows(1)
            .pane(staticPane("test")
                .bounds(0, 0, 9, 1)
                .item(item()
                    .material(Material.DIAMOND)
                    .visible(item1Visible::get)
                    .onClick(event -> clickedItemId.set(1))
                    .build())
                .item(item()
                    .material(Material.GOLD_INGOT)
                    .onClick(event -> clickedItemId.set(2))
                    .build())
                .item(item()
                    .material(Material.EMERALD)
                    .onClick(event -> clickedItemId.set(3))
                    .build())
                .build())
            .build();

        menu.open(this.player);

        // Initially: slot 0 = item1, click it
        InventoryClickEvent event1 = this.createLeftClick(this.player, 0);
        this.listener.onInventoryClick(event1);
        assertThat(clickedItemId.get()).isEqualTo(1);

        // Hide item1 and refresh
        item1Visible.set(false);
        menu.refresh(this.player);

        // Now: slot 0 = item2 (reflowed), click it
        clickedItemId.set(-1);
        InventoryClickEvent event2 = this.createLeftClick(this.player, 0);
        this.listener.onInventoryClick(event2);
        assertThat(clickedItemId.get()).isEqualTo(2);

        // Click slot 1 = item3
        clickedItemId.set(-1);
        InventoryClickEvent event3 = this.createLeftClick(this.player, 1);
        this.listener.onInventoryClick(event3);
        assertThat(clickedItemId.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should route clicks correctly with multiple invisible auto-items")
    void testMultipleInvisibleAutoItemsClickRouting() {
        AtomicInteger clickedItemId = new AtomicInteger(-1);

        Menu menu = Menu.builder(this.plugin)
            .title("Multiple Invisible Test")
            .rows(1)
            .pane(staticPane("test")
                .bounds(0, 0, 9, 1)
                .item(item()
                    .material(Material.DIAMOND)
                    .visible(false)
                    .onClick(event -> clickedItemId.set(1))
                    .build())
                .item(item()
                    .material(Material.GOLD_INGOT)
                    .visible(false)
                    .onClick(event -> clickedItemId.set(2))
                    .build())
                .item(item()
                    .material(Material.EMERALD)
                    .onClick(event -> clickedItemId.set(3))
                    .build())
                .item(item()
                    .material(Material.IRON_INGOT)
                    .onClick(event -> clickedItemId.set(4))
                    .build())
                .build())
            .build();

        menu.open(this.player);

        // item1 and item2 invisible, so slot 0 = item3
        InventoryClickEvent event1 = this.createLeftClick(this.player, 0);
        this.listener.onInventoryClick(event1);
        assertThat(clickedItemId.get()).isEqualTo(3);

        // Slot 1 = item4
        clickedItemId.set(-1);
        InventoryClickEvent event2 = this.createLeftClick(this.player, 1);
        this.listener.onInventoryClick(event2);
        assertThat(clickedItemId.get()).isEqualTo(4);
    }

    @Test
    @DisplayName("Should route clicks to both static and auto items correctly")
    void testMixedStaticAutoClickRouting() {
        AtomicInteger clickedItemId = new AtomicInteger(-1);

        Menu menu = Menu.builder(this.plugin)
            .title("Mixed Test")
            .rows(1)
            .pane(staticPane("test")
                .bounds(0, 0, 9, 1)
                .item(1, 0, item()  // Static at slot 1
                    .material(Material.BARRIER)
                    .onClick(event -> clickedItemId.set(100))
                    .build())
                .item(item()  // Auto at slot 0
                    .material(Material.DIAMOND)
                    .onClick(event -> clickedItemId.set(1))
                    .build())
                .item(item()  // Auto at slot 2 (skips static slot 1)
                    .material(Material.EMERALD)
                    .onClick(event -> clickedItemId.set(2))
                    .build())
                .build())
            .build();

        menu.open(this.player);

        // Click slot 0 - auto item 1
        InventoryClickEvent event1 = this.createLeftClick(this.player, 0);
        this.listener.onInventoryClick(event1);
        assertThat(clickedItemId.get()).isEqualTo(1);

        // Click slot 1 - static item
        clickedItemId.set(-1);
        InventoryClickEvent event2 = this.createLeftClick(this.player, 1);
        this.listener.onInventoryClick(event2);
        assertThat(clickedItemId.get()).isEqualTo(100);

        // Click slot 2 - auto item 2
        clickedItemId.set(-1);
        InventoryClickEvent event3 = this.createLeftClick(this.player, 2);
        this.listener.onInventoryClick(event3);
        assertThat(clickedItemId.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should handle complex visibility toggling with click routing")
    void testComplexVisibilityToggleClickRouting() {
        AtomicBoolean item2Visible = new AtomicBoolean(true);
        AtomicInteger clickedItemId = new AtomicInteger(-1);

        Menu menu = Menu.builder(this.plugin)
            .title("Toggle Test")
            .rows(1)
            .pane(staticPane("test")
                .bounds(0, 0, 9, 1)
                .item(item()
                    .material(Material.DIAMOND)
                    .onClick(event -> clickedItemId.set(1))
                    .build())
                .item(item()
                    .material(Material.GOLD_INGOT)
                    .visible(item2Visible::get)
                    .onClick(event -> clickedItemId.set(2))
                    .build())
                .item(item()
                    .material(Material.EMERALD)
                    .onClick(event -> clickedItemId.set(3))
                    .build())
                .build())
            .build();

        menu.open(this.player);

        // Initially all visible: slot 1 = item2
        InventoryClickEvent event1 = this.createLeftClick(this.player, 1);
        this.listener.onInventoryClick(event1);
        assertThat(clickedItemId.get()).isEqualTo(2);

        // Hide item2
        item2Visible.set(false);
        menu.refresh(this.player);

        // Now: slot 1 = item3 (reflowed)
        clickedItemId.set(-1);
        InventoryClickEvent event2 = this.createLeftClick(this.player, 1);
        this.listener.onInventoryClick(event2);
        assertThat(clickedItemId.get()).isEqualTo(3);

        // Show item2 again
        item2Visible.set(true);
        menu.refresh(this.player);

        // Back to: slot 1 = item2
        clickedItemId.set(-1);
        InventoryClickEvent event3 = this.createLeftClick(this.player, 1);
        this.listener.onInventoryClick(event3);
        assertThat(clickedItemId.get()).isEqualTo(2);
    }
}
