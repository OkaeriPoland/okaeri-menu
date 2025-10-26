package eu.okaeri.menu.bukkit.unit;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.MenuContext;
import eu.okaeri.menu.pane.PaginatedPane;
import eu.okaeri.menu.pane.PaginationContext;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PaginationContext.
 * Tests page navigation, filtering, and state management.
 */
class PaginationContextTest {

    private static ServerMock server;
    private org.bukkit.plugin.java.JavaPlugin plugin;
    private Player player;
    private List<String> testItems;
    private Menu menu;
    private MenuContext menuContext;

    // Default test pane and context - reset for each test
    private PaginatedPane<String> testPane;
    private PaginationContext<String> ctx;

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
        this.player = server.addPlayer();

        this.testItems = Arrays.asList(
            "Item A", "Item B", "Item C", "Item D", "Item E",
            "Item F", "Item G", "Item H", "Item I", "Item J"
        );

        // Create a menu and open it for the player to establish ViewerState
        this.menu = Menu.builder(this.plugin).rows(3).build();
        this.menu.open(this.player);

        // Create MenuContext for pagination tests
        this.menuContext = new MenuContext(this.menu, this.player);

        // Create default test pane and context
        this.testPane = this.createPane(this.testItems, 3);
        this.ctx = PaginationContext.get(this.menuContext, this.testPane);
    }

    /**
     * Helper to create a pane with custom items and items per page.
     */
    private PaginatedPane<String> createPane(List<String> items, int itemsPerPage) {
        return PaginatedPane.<String>pane()
            .name("test-pane")
            .bounds(0, 0, 1, 9)
            .items(items)
            .itemsPerPage(itemsPerPage)
            .renderer((ctx, item, i) -> null)
            .build();
    }

    /**
     * Helper to create a pane with dynamic supplier (for reactive tests).
     * Uses a different name to avoid cache conflicts.
     */
    private PaginatedPane<String> createDynamicPane(List<String> dynamicItems, int itemsPerPage) {
        return PaginatedPane.<String>pane()
            .name("dynamic-pane")  // Different name to avoid cache conflicts
            .bounds(0, 0, 1, 9)
            .items(() -> dynamicItems)  // Supplier reads from mutable list
            .itemsPerPage(itemsPerPage)
            .renderer((ctx, item, i) -> null)
            .build();
    }

    @Test
    @DisplayName("Should create pagination context with correct initial state")
    void testInitialState() {
        assertEquals(0, this.ctx.getCurrentPage());
        assertEquals(10, this.ctx.getTotalItems());
        assertEquals(4, this.ctx.getTotalPages()); // ceil(10 / 3) = 4
        assertTrue(this.ctx.hasNext());
        assertFalse(this.ctx.hasPrevious());
    }

    @Test
    @DisplayName("Should navigate to next page")
    void testNextPage() {
        assertTrue(this.ctx.nextPage());
        assertEquals(1, this.ctx.getCurrentPage());
        assertTrue(this.ctx.hasNext());
        assertTrue(this.ctx.hasPrevious());
    }

    @Test
    @DisplayName("Should navigate to previous page")
    void testPreviousPage() {
        this.ctx.setPage(2);
        assertTrue(this.ctx.previousPage());
        assertEquals(1, this.ctx.getCurrentPage());
    }

    @Test
    @DisplayName("Should not navigate beyond bounds")
    void testBoundsChecking() {
        // Try to go to page before first
        assertFalse(this.ctx.previousPage());
        assertEquals(0, this.ctx.getCurrentPage());

        // Go to last page
        this.ctx.setPage(3);

        // Try to go beyond last page
        assertFalse(this.ctx.nextPage());
        assertEquals(3, this.ctx.getCurrentPage());
    }

    @Test
    @DisplayName("Should get correct items for current page")
    void testGetCurrentPageItems() {
        // Page 0: Items 0-2
        List<String> page0 = this.ctx.getCurrentPageItems();
        assertEquals(3, page0.size());
        assertEquals("Item A", page0.get(0));
        assertEquals("Item C", page0.get(2));

        // Page 1: Items 3-5
        this.ctx.nextPage();
        List<String> page1 = this.ctx.getCurrentPageItems();
        assertEquals(3, page1.size());
        assertEquals("Item D", page1.get(0));

        // Last page: Items 9 (only 1 item)
        this.ctx.setPage(3);
        List<String> page3 = this.ctx.getCurrentPageItems();
        assertEquals(1, page3.size());
        assertEquals("Item J", page3.get(0));
    }

    @Test
    @DisplayName("Should filter items")
    void testFiltering() {
        // Filter to only items containing "A" or "B"
        this.ctx.addFilter("ab-filter", item ->
            item.contains("A") || item.contains("B")
        );

        assertEquals(2, this.ctx.getTotalItems()); // Only "Item A" and "Item B"
        assertEquals(1, this.ctx.getTotalPages()); // ceil(2 / 3) = 1

        List<String> filtered = this.ctx.getFilteredItems();
        assertEquals(2, filtered.size());
        assertTrue(filtered.contains("Item A"));
        assertTrue(filtered.contains("Item B"));
    }

    @Test
    @DisplayName("Should toggle filters")
    void testToggleFilter() {
        // Toggle on
        boolean active = this.ctx.toggleFilter("test-filter", item -> item.contains("A"));
        assertTrue(active);
        assertTrue(this.ctx.hasFilter("test-filter"));
        assertEquals(1, this.ctx.getTotalItems());

        // Toggle off
        boolean inactive = this.ctx.toggleFilter("test-filter", item -> item.contains("A"));
        assertFalse(inactive);
        assertFalse(this.ctx.hasFilter("test-filter"));
        assertEquals(10, this.ctx.getTotalItems());
    }

    @Test
    @DisplayName("Should combine multiple filters with AND logic")
    void testMultipleFilters() {
        // Filter 1: Contains "I"
        this.ctx.addFilter("filter1", item -> item.contains("I"));

        // Filter 2: Contains "A" (only "Item A" has both)
        this.ctx.addFilter("filter2", item -> item.contains("A"));

        assertEquals(1, this.ctx.getTotalItems());
        assertEquals("Item A", this.ctx.getFilteredItems().get(0));
    }

    @Test
    @DisplayName("Should clear all filters")
    void testClearFilters() {
        this.ctx.addFilter("filter1", item -> item.contains("A"));
        this.ctx.addFilter("filter2", item -> item.contains("B"));

        assertEquals(2, this.ctx.getActiveFilterCount());

        this.ctx.clearFilters();

        assertEquals(0, this.ctx.getActiveFilterCount());
        assertEquals(10, this.ctx.getTotalItems());
    }

    @Test
    @DisplayName("Should reset page to 0 when filters change")
    void testPageResetOnFilterChange() {
        this.ctx.setPage(2);
        assertEquals(2, this.ctx.getCurrentPage());

        this.ctx.addFilter("test", item -> true);

        assertEquals(0, this.ctx.getCurrentPage());
    }

    @Test
    @DisplayName("Should handle empty filtered results")
    void testEmptyFilteredResults() {
        this.ctx.addFilter("impossible", item -> item.contains("XYZ"));

        assertTrue(this.ctx.isEmpty());
        assertEquals(0, this.ctx.getTotalItems());
        assertEquals(0, this.ctx.getTotalPages());
        assertFalse(this.ctx.hasNext());
        assertFalse(this.ctx.hasPrevious());
    }

    @Test
    @DisplayName("Should reactively update items from pane supplier")
    void testReactiveItemUpdates() {
        // Use a mutable list that the supplier will read from
        List<String> dynamicItems = new ArrayList<>(this.testItems);

        PaginatedPane<String> dynamicPane = this.createDynamicPane(dynamicItems, 3);
        PaginationContext<String> dynamicCtx = PaginationContext.get(this.menuContext, dynamicPane);

        assertEquals(10, dynamicCtx.getTotalItems());

        // Modify the list that the supplier reads from
        dynamicItems.clear();
        dynamicItems.addAll(Arrays.asList("New A", "New B", "New C"));

        // Context should now see the new items reactively
        assertEquals(3, dynamicCtx.getTotalItems());
        assertEquals(1, dynamicCtx.getTotalPages());
    }

    @Test
    @DisplayName("Should adjust page when items shrink reactively")
    void testPageAdjustmentOnReactiveShrink() {
        // Use a mutable list that the supplier will read from
        List<String> dynamicItems = new ArrayList<>(this.testItems);

        PaginatedPane<String> dynamicPane = this.createDynamicPane(dynamicItems, 3);
        PaginationContext<String> dynamicCtx = PaginationContext.get(this.menuContext, dynamicPane);

        // Go to last page (page 3)
        dynamicCtx.setPage(3);
        assertEquals(3, dynamicCtx.getCurrentPage());

        // Modify the list to have fewer items
        dynamicItems.clear();
        dynamicItems.addAll(Arrays.asList("A", "B", "C"));

        // Attempting to navigate should now respect the new item count
        // The page number doesn't auto-adjust, but navigation will be bounded
        assertFalse(dynamicCtx.nextPage()); // Can't go further from page 3

        // Set to valid page for new item count
        dynamicCtx.setPage(0);
        assertEquals(0, dynamicCtx.getCurrentPage());
        assertEquals(1, dynamicCtx.getTotalPages()); // Only 1 page now
    }

    @Test
    @DisplayName("Should navigate to first and last page")
    void testFirstLastPageNavigation() {
        this.ctx.lastPage();
        assertEquals(3, this.ctx.getCurrentPage());

        this.ctx.firstPage();
        assertEquals(0, this.ctx.getCurrentPage());
    }
}
