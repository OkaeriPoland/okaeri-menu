package eu.okaeri.menu.bukkit.integration;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.bukkit.test.PooledAsyncExecutor;
import eu.okaeri.menu.bukkit.test.SyncTestExecutor;
import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static eu.okaeri.menu.item.MenuItem.item;
import static eu.okaeri.menu.pane.AsyncPaginatedPane.paneAsync;
import static eu.okaeri.menu.pane.PaginatedPane.pane;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for click routing in PaginatedPane and AsyncPaginatedPane.
 * Tests that paginated data items and static items route clicks correctly.
 */
class PaginatedPaneClickTest extends MenuListenerTestBase {

    @Test
    @DisplayName("Should route clicks to paginated items on current page")
    void testPaginatedItemClickRouting() {
        AtomicInteger clickedItemIndex = new AtomicInteger(-1);
        List<String> items = Arrays.asList("Item 1", "Item 2", "Item 3");

        Menu menu = Menu.builder(this.plugin)
            .title("Paginated Test")
            .rows(3)
            .pane(pane(String.class)
                .name("items")
                .bounds(0, 0, 2, 9)
                .items(items)
                .renderer((ctx, item, index) -> item()
                    .material(Material.DIAMOND)
                    .name(item)
                    .onClick(event -> clickedItemIndex.set(index))
                    .build())
                .build())
            .build();

        menu.open(this.player);

        // Click first paginated item (slot 0)
        this.listener.onInventoryClick(this.createLeftClick(this.player, 0));
        assertThat(clickedItemIndex.get()).isEqualTo(0);

        // Click second paginated item (slot 1)
        clickedItemIndex.set(-1);
        this.listener.onInventoryClick(this.createLeftClick(this.player, 1));
        assertThat(clickedItemIndex.get()).isEqualTo(1);

        // Click third paginated item (slot 2)
        clickedItemIndex.set(-1);
        this.listener.onInventoryClick(this.createLeftClick(this.player, 2));
        assertThat(clickedItemIndex.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should route clicks to static items in paginated pane")
    void testPaginatedPaneStaticItemClicks() {
        AtomicInteger staticClicks = new AtomicInteger(0);
        AtomicInteger paginatedClicks = new AtomicInteger(0);
        List<String> items = Arrays.asList("A", "B", "C");

        Menu menu = Menu.builder(this.plugin)
            .title("Paginated with Static")
            .rows(3)
            .pane(pane(String.class)
                .name("items")
                .bounds(0, 0, 3, 9)
                .items(items)
                .renderer((ctx, item, index) -> item()
                    .material(Material.DIAMOND)
                    .onClick(event -> paginatedClicks.incrementAndGet())
                    .build())
                .item(2, 8, item()  // Bottom-right corner
                    .material(Material.ARROW)
                    .name("Next Page")
                    .onClick(event -> staticClicks.incrementAndGet())
                    .build())
                .build())
            .build();

        menu.open(this.player);

        // Click static item (slot 26 = row 2, col 8)
        this.listener.onInventoryClick(this.createLeftClick(this.player, 26));
        assertThat(staticClicks.get()).isEqualTo(1);

        // Click paginated item (slot 0)
        this.listener.onInventoryClick(this.createLeftClick(this.player, 0));
        assertThat(paginatedClicks.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should route clicks correctly when paginated items flow around static items")
    void testPaginatedItemsFlowAroundStaticItemsClickRouting() {
        AtomicInteger staticClicks = new AtomicInteger(0);
        AtomicInteger clickedItemIndex = new AtomicInteger(-1);
        List<String> items = Arrays.asList("A", "B", "C", "D", "E");

        Menu menu = Menu.builder(this.plugin)
            .title("Flow Around Static")
            .rows(3)
            .pane(pane(String.class)
                .name("items")
                .bounds(0, 0, 2, 9)  // 18 slots total
                .items(items)
                .renderer((ctx, item, index) -> item()
                    .material(Material.DIAMOND)
                    .name(item)
                    .onClick(event -> clickedItemIndex.set(index))
                    .build())
                .item(0, 2, item()  // Static at slot 2
                    .material(Material.BARRIER)
                    .name("Static")
                    .onClick(event -> staticClicks.incrementAndGet())
                    .build())
                .build())
            .build();

        menu.open(this.player);

        // Expected layout: [A, B, STATIC, C, D, E, ...]
        //                  [0, 1,   2,    3, 4, 5, ...]

        // Click slot 0 → Item A (index 0)
        this.listener.onInventoryClick(this.createLeftClick(this.player, 0));
        assertThat(clickedItemIndex.get())
            .as("Slot 0 should route to item A (index 0)")
            .isEqualTo(0);

        // Click slot 1 → Item B (index 1)
        clickedItemIndex.set(-1);
        this.listener.onInventoryClick(this.createLeftClick(this.player, 1));
        assertThat(clickedItemIndex.get())
            .as("Slot 1 should route to item B (index 1)")
            .isEqualTo(1);

        // Click slot 2 → Static item
        this.listener.onInventoryClick(this.createLeftClick(this.player, 2));
        assertThat(staticClicks.get())
            .as("Slot 2 should route to static item")
            .isEqualTo(1);

        // CRITICAL: Click slot 3 → Item C (index 2), NOT item D!
        // This verifies the fix: items flow around static items without being lost
        clickedItemIndex.set(-1);
        this.listener.onInventoryClick(this.createLeftClick(this.player, 3));
        assertThat(clickedItemIndex.get())
            .as("Slot 3 should route to item C (index 2), not D - items must flow around static")
            .isEqualTo(2);

        // Click slot 4 → Item D (index 3)
        clickedItemIndex.set(-1);
        this.listener.onInventoryClick(this.createLeftClick(this.player, 4));
        assertThat(clickedItemIndex.get())
            .as("Slot 4 should route to item D (index 3)")
            .isEqualTo(3);

        // Click slot 5 → Item E (index 4)
        clickedItemIndex.set(-1);
        this.listener.onInventoryClick(this.createLeftClick(this.player, 5));
        assertThat(clickedItemIndex.get())
            .as("Slot 5 should route to item E (index 4)")
            .isEqualTo(4);
    }

    @Test
    @DisplayName("Should route clicks to async paginated items after data loads")
    void testAsyncPaginatedItemClickRouting() {
        AtomicInteger clickedItemIndex = new AtomicInteger(-1);
        List<String> items = Arrays.asList("Async 1", "Async 2", "Async 3");

        Menu menu = Menu.builder(this.plugin)
            .asyncExecutor(SyncTestExecutor.create())
            .title("Async Paginated Test")
            .rows(3)
            .pane(paneAsync(String.class)
                .name("items")
                .bounds(0, 0, 2, 9)
                .loader(ctx -> items)
                .renderer((ctx, item, index) -> item()
                    .material(Material.EMERALD)
                    .name(item)
                    .onClick(event -> clickedItemIndex.set(index))
                    .build())
                .build())
            .build();

        menu.open(this.player);
        server.getScheduler().performOneTick();  // Execute async refresh

        // Click first async item (slot 0)
        this.listener.onInventoryClick(this.createLeftClick(this.player, 0));
        assertThat(clickedItemIndex.get()).isEqualTo(0);

        // Click second async item (slot 1)
        clickedItemIndex.set(-1);
        this.listener.onInventoryClick(this.createLeftClick(this.player, 1));
        assertThat(clickedItemIndex.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should not route clicks during LOADING state")
    void testAsyncPaginatedClickDuringLoading() {
        AtomicInteger clickCount = new AtomicInteger(0);

        Menu menu = Menu.builder(this.plugin)
            .asyncExecutor(PooledAsyncExecutor.create())
            .title("Loading Test")
            .rows(3)
            .pane(paneAsync(String.class)
                .name("items")
                .bounds(0, 0, 2, 9)
                .loader(ctx -> {
                    // Simulate realistic slow database query/API call
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    return List.of("Item 1");
                })
                .renderer((ctx, item, index) -> item()
                    .material(Material.DIAMOND)
                    .onClick(event -> clickCount.incrementAndGet())
                    .build())
                .build())
            .build();

        // Open menu - renders LOADING state immediately, starts async load in background
        menu.open(this.player);

        // User clicks during loading (realistic scenario)
        // Click should NOT route to any handler - loading items are placeholders, not tracked in renderedItems
        this.listener.onInventoryClick(this.createLeftClick(this.player, 0));
        assertThat(clickCount.get())
            .as("Clicks during LOADING state should not route to item handlers")
            .isEqualTo(0);
    }

    @Test
    @DisplayName("Should not route clicks to invisible static items in paginated pane")
    void testInvisibleStaticItemInPaginatedPaneNoClick() {
        AtomicInteger staticClicks = new AtomicInteger(0);
        AtomicInteger paginatedClicks = new AtomicInteger(0);
        List<String> items = Arrays.asList("A", "B", "C");

        Menu menu = Menu.builder(this.plugin)
            .title("Invisible Static in Paginated")
            .rows(3)
            .pane(pane(String.class)
                .name("items")
                .bounds(0, 0, 3, 9)
                .items(items)
                .renderer((ctx, item, index) -> item()
                    .material(Material.DIAMOND)
                    .onClick(event -> paginatedClicks.incrementAndGet())
                    .build())
                .item(2, 8, item()
                    .material(Material.ARROW)
                    .name("Next Page")
                    .visible(false)  // Invisible static item
                    .onClick(event -> staticClicks.incrementAndGet())
                    .build())
                .build())
            .build();

        menu.open(this.player);

        // Click where invisible static item is (slot 26 = row 2, col 8)
        this.listener.onInventoryClick(this.createLeftClick(this.player, 26));
        assertThat(staticClicks.get())
            .as("Invisible static item should not be clickable")
            .isEqualTo(0);

        // Paginated items should still work
        this.listener.onInventoryClick(this.createLeftClick(this.player, 0));
        assertThat(paginatedClicks.get())
            .as("Paginated items should still be clickable")
            .isEqualTo(1);
    }

    @Test
    @DisplayName("Should not route clicks to static items that become invisible in paginated pane")
    void testPaginatedPaneStaticItemBecomesInvisibleNoClick() {
        AtomicBoolean staticVisible = new AtomicBoolean(true);
        AtomicInteger staticClicks = new AtomicInteger(0);
        List<String> items = Arrays.asList("A", "B", "C");

        Menu menu = Menu.builder(this.plugin)
            .title("Dynamic Static Visibility")
            .rows(3)
            .pane(pane(String.class)
                .name("items")
                .bounds(0, 0, 3, 9)
                .items(items)
                .renderer((ctx, item, index) -> item()
                    .material(Material.DIAMOND)
                    .build())
                .item(2, 8, item()
                    .material(Material.ARROW)
                    .name("Button")
                    .visible(staticVisible::get)
                    .onClick(event -> staticClicks.incrementAndGet())
                    .build())
                .build())
            .build();

        menu.open(this.player);

        // Initially visible - click should work
        this.listener.onInventoryClick(this.createLeftClick(this.player, 26));
        assertThat(staticClicks.get())
            .as("Visible static item should be clickable")
            .isEqualTo(1);

        // Make invisible and refresh
        staticVisible.set(false);
        menu.refresh(this.player);

        // Now invisible - click should NOT work
        this.listener.onInventoryClick(this.createLeftClick(this.player, 26));
        assertThat(staticClicks.get())
            .as("Static item should not be clickable after becoming invisible")
            .isEqualTo(1);  // Still 1, not incremented
    }
}
