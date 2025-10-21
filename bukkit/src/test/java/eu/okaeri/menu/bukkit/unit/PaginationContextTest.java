package eu.okaeri.menu.bukkit.unit;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.pagination.PaginationContext;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PaginationContext.
 * Tests page navigation, filtering, and state management.
 */
class PaginationContextTest {

    private static ServerMock server;
    private Player player;
    private List<String> testItems;
    private Menu menu;

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
        this.player = server.addPlayer();

        this.testItems = Arrays.asList(
            "Item A", "Item B", "Item C", "Item D", "Item E",
            "Item F", "Item G", "Item H", "Item I", "Item J"
        );

        // Create a menu and open it for the player to establish ViewerState
        this.menu = Menu.builder().rows(3).build();
        this.menu.open(this.player);
    }

    @Test
    @DisplayName("Should create pagination context with correct initial state")
    void testInitialState() {
        PaginationContext<String> ctx = PaginationContext.get(
            this.menu, "test-pane", this.player, this.testItems, 3
        );

        assertEquals(0, ctx.getCurrentPage());
        assertEquals(10, ctx.getTotalItems());
        assertEquals(4, ctx.getTotalPages()); // ceil(10 / 3) = 4
        assertTrue(ctx.hasNext());
        assertFalse(ctx.hasPrevious());
    }

    @Test
    @DisplayName("Should navigate to next page")
    void testNextPage() {
        PaginationContext<String> ctx = PaginationContext.get(
            this.menu, "test-pane", this.player, this.testItems, 3
        );

        assertTrue(ctx.nextPage());
        assertEquals(1, ctx.getCurrentPage());
        assertTrue(ctx.hasNext());
        assertTrue(ctx.hasPrevious());
    }

    @Test
    @DisplayName("Should navigate to previous page")
    void testPreviousPage() {
        PaginationContext<String> ctx = PaginationContext.get(
            this.menu, "test-pane", this.player, this.testItems, 3
        );

        ctx.setPage(2);
        assertTrue(ctx.previousPage());
        assertEquals(1, ctx.getCurrentPage());
    }

    @Test
    @DisplayName("Should not navigate beyond bounds")
    void testBoundsChecking() {
        PaginationContext<String> ctx = PaginationContext.get(
            this.menu, "test-pane", this.player, this.testItems, 3
        );

        // Try to go to page before first
        assertFalse(ctx.previousPage());
        assertEquals(0, ctx.getCurrentPage());

        // Go to last page
        ctx.setPage(3);

        // Try to go beyond last page
        assertFalse(ctx.nextPage());
        assertEquals(3, ctx.getCurrentPage());
    }

    @Test
    @DisplayName("Should get correct items for current page")
    void testGetCurrentPageItems() {
        PaginationContext<String> ctx = PaginationContext.get(
            this.menu, "test-pane", this.player, this.testItems, 3
        );

        // Page 0: Items 0-2
        List<String> page0 = ctx.getCurrentPageItems();
        assertEquals(3, page0.size());
        assertEquals("Item A", page0.get(0));
        assertEquals("Item C", page0.get(2));

        // Page 1: Items 3-5
        ctx.nextPage();
        List<String> page1 = ctx.getCurrentPageItems();
        assertEquals(3, page1.size());
        assertEquals("Item D", page1.get(0));

        // Last page: Items 9 (only 1 item)
        ctx.setPage(3);
        List<String> page3 = ctx.getCurrentPageItems();
        assertEquals(1, page3.size());
        assertEquals("Item J", page3.get(0));
    }

    @Test
    @DisplayName("Should filter items")
    void testFiltering() {
        PaginationContext<String> ctx = PaginationContext.get(
            this.menu, "test-pane", this.player, this.testItems, 3
        );

        // Filter to only items containing "A" or "B"
        ctx.addFilter("ab-filter", item ->
            item.contains("A") || item.contains("B")
        );

        assertEquals(2, ctx.getTotalItems()); // Only "Item A" and "Item B"
        assertEquals(1, ctx.getTotalPages()); // ceil(2 / 3) = 1

        List<String> filtered = ctx.getFilteredItems();
        assertEquals(2, filtered.size());
        assertTrue(filtered.contains("Item A"));
        assertTrue(filtered.contains("Item B"));
    }

    @Test
    @DisplayName("Should toggle filters")
    void testToggleFilter() {
        PaginationContext<String> ctx = PaginationContext.get(
            this.menu, "test-pane", this.player, this.testItems, 3
        );

        // Toggle on
        boolean active = ctx.toggleFilter("test-filter", item -> item.contains("A"));
        assertTrue(active);
        assertTrue(ctx.hasFilter("test-filter"));
        assertEquals(1, ctx.getTotalItems());

        // Toggle off
        boolean inactive = ctx.toggleFilter("test-filter", item -> item.contains("A"));
        assertFalse(inactive);
        assertFalse(ctx.hasFilter("test-filter"));
        assertEquals(10, ctx.getTotalItems());
    }

    @Test
    @DisplayName("Should combine multiple filters with AND logic")
    void testMultipleFilters() {
        PaginationContext<String> ctx = PaginationContext.get(
            this.menu, "test-pane", this.player, this.testItems, 3
        );

        // Filter 1: Contains "I"
        ctx.addFilter("filter1", item -> item.contains("I"));

        // Filter 2: Contains "A" (only "Item A" has both)
        ctx.addFilter("filter2", item -> item.contains("A"));

        assertEquals(1, ctx.getTotalItems());
        assertEquals("Item A", ctx.getFilteredItems().get(0));
    }

    @Test
    @DisplayName("Should clear all filters")
    void testClearFilters() {
        PaginationContext<String> ctx = PaginationContext.get(
            this.menu, "test-pane", this.player, this.testItems, 3
        );

        ctx.addFilter("filter1", item -> item.contains("A"));
        ctx.addFilter("filter2", item -> item.contains("B"));

        assertEquals(2, ctx.getActiveFilters().size());

        ctx.clearFilters();

        assertEquals(0, ctx.getActiveFilters().size());
        assertEquals(10, ctx.getTotalItems());
    }

    @Test
    @DisplayName("Should reset page to 0 when filters change")
    void testPageResetOnFilterChange() {
        PaginationContext<String> ctx = PaginationContext.get(
            this.menu, "test-pane", this.player, this.testItems, 3
        );

        ctx.setPage(2);
        assertEquals(2, ctx.getCurrentPage());

        ctx.addFilter("test", item -> true);

        assertEquals(0, ctx.getCurrentPage());
    }

    @Test
    @DisplayName("Should handle empty filtered results")
    void testEmptyFilteredResults() {
        PaginationContext<String> ctx = PaginationContext.get(
            this.menu, "test-pane", this.player, this.testItems, 3
        );

        ctx.addFilter("impossible", item -> item.contains("XYZ"));

        assertTrue(ctx.isEmpty());
        assertEquals(0, ctx.getTotalItems());
        assertEquals(0, ctx.getTotalPages());
        assertFalse(ctx.hasNext());
        assertFalse(ctx.hasPrevious());
    }

    @Test
    @DisplayName("Should update items dynamically")
    void testUpdateItems() {
        PaginationContext<String> ctx = PaginationContext.get(
            this.menu, "test-pane", this.player, this.testItems, 3
        );

        assertEquals(10, ctx.getTotalItems());

        List<String> newItems = Arrays.asList("New A", "New B", "New C");
        ctx.updateItems(newItems);

        assertEquals(3, ctx.getTotalItems());
        assertEquals(1, ctx.getTotalPages());
    }

    @Test
    @DisplayName("Should adjust page when items shrink")
    void testPageAdjustmentOnShrink() {
        PaginationContext<String> ctx = PaginationContext.get(
            this.menu, "test-pane", this.player, this.testItems, 3
        );

        // Go to last page (page 3)
        ctx.setPage(3);
        assertEquals(3, ctx.getCurrentPage());

        // Update to fewer items (only 3 items = 1 page)
        ctx.updateItems(Arrays.asList("A", "B", "C"));

        // Should adjust to page 0 (last valid page)
        assertEquals(0, ctx.getCurrentPage());
    }

    @Test
    @DisplayName("Should navigate to first and last page")
    void testFirstLastPageNavigation() {
        PaginationContext<String> ctx = PaginationContext.get(
            this.menu, "test-pane", this.player, this.testItems, 3
        );

        ctx.lastPage();
        assertEquals(3, ctx.getCurrentPage());

        ctx.firstPage();
        assertEquals(0, ctx.getCurrentPage());
    }
}
