package eu.okaeri.menu.bukkit.integration;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.bukkit.test.PooledAsyncExecutor;
import eu.okaeri.menu.bukkit.test.SyncTestExecutor;
import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
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
                .bounds(0, 0, 9, 2)
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
                .bounds(0, 0, 9, 3)
                .items(items)
                .renderer((ctx, item, index) -> item()
                    .material(Material.DIAMOND)
                    .onClick(event -> paginatedClicks.incrementAndGet())
                    .build())
                .staticItem(8, 2, item()  // Bottom-right corner
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
                .bounds(0, 0, 9, 2)
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
                .bounds(0, 0, 9, 2)
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
}
