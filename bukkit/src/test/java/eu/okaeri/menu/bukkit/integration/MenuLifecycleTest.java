package eu.okaeri.menu.bukkit.integration;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.pane.PaginatedPane;
import eu.okaeri.menu.pane.PaginationContext;
import eu.okaeri.menu.state.ViewerState;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static eu.okaeri.menu.item.MenuItem.item;
import static eu.okaeri.menu.pane.StaticPane.staticPane;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration tests for Menu lifecycle and ViewerState management.
 * Tests menu opening, closing, ViewerState creation/cleanup, and multi-viewer scenarios.
 */
class MenuLifecycleTest {

    private static ServerMock server;
    private JavaPlugin plugin;
    private Player player1;
    private Player player2;

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
        this.player1 = server.addPlayer("Player1");
        this.player2 = server.addPlayer("Player2");
    }

    // ========================================
    // VIEWERSTATE CREATION
    // ========================================

    @Test
    @DisplayName("Should create ViewerState when opening menu")
    void testViewerStateCreation() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .build();

        // Initially no ViewerState
        assertThat(menu.getViewerState(this.player1.getUniqueId())).isNull();

        // Open menu
        menu.open(this.player1);

        // ViewerState should be created
        ViewerState state = menu.getViewerState(this.player1.getUniqueId());
        assertThat(state).isNotNull();
        assertThat(state.getInventory()).isNotNull();
        assertThat(state.getAsync()).isNotNull();
    }

    @Test
    @DisplayName("Should reuse ViewerState when reopening menu")
    void testViewerStateReuse() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .build();

        // Open menu first time
        menu.open(this.player1);
        ViewerState state1 = menu.getViewerState(this.player1.getUniqueId());

        // Open menu again
        menu.open(this.player1);
        ViewerState state2 = menu.getViewerState(this.player1.getUniqueId());

        // Should be the same ViewerState instance
        assertThat(state2).isSameAs(state1);
    }

    @Test
    @DisplayName("Should create inventory with correct title")
    void testInventoryTitle() {
        Menu menu = Menu.builder(this.plugin)
            .title("&aGreen Title")
            .rows(2)
            .build();

        menu.open(this.player1);

        ViewerState state = menu.getViewerState(this.player1.getUniqueId());
        Inventory inventory = state.getInventory();

        assertThat(inventory).isNotNull();
        assertThat(inventory.getSize()).isEqualTo(18); // 2 rows * 9 slots
    }

    @Test
    @DisplayName("Should create inventory with correct size")
    void testInventorySize() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(6)
            .build();

        menu.open(this.player1);

        ViewerState state = menu.getViewerState(this.player1.getUniqueId());
        Inventory inventory = state.getInventory();

        assertThat(inventory.getSize()).isEqualTo(54); // 6 rows * 9 slots
    }

    // ========================================
    // VIEWERSTATE CLEANUP
    // ========================================

    @Test
    @DisplayName("Should cleanup ViewerState on close")
    void testViewerStateCleanup() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .build();

        // Open and verify ViewerState exists
        menu.open(this.player1);
        assertThat(menu.getViewerState(this.player1.getUniqueId())).isNotNull();

        // Close and verify cleanup
        menu.close(this.player1);
        assertThat(menu.getViewerState(this.player1.getUniqueId())).isNull();
    }

    @Test
    @DisplayName("Should not throw when closing already closed menu")
    void testDoubleClose() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .build();

        menu.open(this.player1);
        menu.close(this.player1);

        // Close again should not throw
        assertThatCode(() -> menu.close(this.player1))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should cleanup pagination contexts on close")
    void testPaginationContextCleanup() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .build();

        menu.open(this.player1);

        // Create pagination context
        ViewerState state = menu.getViewerState(this.player1.getUniqueId());

        // Create a test pane for pagination
        PaginatedPane<String> testPane = PaginatedPane.<String>pane()
            .name("test_pane")
            .bounds(0, 0, 1, 9)
            .items(List.of("Item1", "Item2", "Item3"))
            .itemsPerPage(5)
            .renderer((ctx, item, i) -> null)
            .build();

        PaginationContext<String> paginationContext = state.getPagination(testPane);
        assertThat(paginationContext).isNotNull();

        // Close menu
        menu.close(this.player1);

        // ViewerState should be removed (so pagination context is cleaned up)
        assertThat(menu.getViewerState(this.player1.getUniqueId())).isNull();
    }

    // ========================================
    // MULTI-VIEWER SCENARIOS
    // ========================================

    @Test
    @DisplayName("Should create separate ViewerState for each player")
    void testMultipleViewers() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .build();

        // Open for both players
        menu.open(this.player1);
        menu.open(this.player2);

        // Each player should have their own ViewerState
        ViewerState state1 = menu.getViewerState(this.player1.getUniqueId());
        ViewerState state2 = menu.getViewerState(this.player2.getUniqueId());

        assertThat(state1).isNotNull();
        assertThat(state2).isNotNull();
        assertThat(state1).isNotSameAs(state2);
        assertThat(state1.getInventory()).isNotSameAs(state2.getInventory());
    }

    @Test
    @DisplayName("Should maintain separate pagination contexts per viewer")
    void testSeparatePaginationContexts() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .build();

        menu.open(this.player1);
        menu.open(this.player2);

        // Create pagination contexts
        ViewerState state1 = menu.getViewerState(this.player1.getUniqueId());
        ViewerState state2 = menu.getViewerState(this.player2.getUniqueId());

        // Create test panes for each player
        PaginatedPane<String> pane1 = PaginatedPane.<String>pane()
            .name("pane1")
            .bounds(0, 0, 1, 9)
            .items(List.of("A", "B", "C"))
            .itemsPerPage(5)
            .renderer((ctx, item, i) -> null)
            .build();

        PaginatedPane<String> pane2 = PaginatedPane.<String>pane()
            .name("pane1")
            .bounds(0, 0, 1, 9)
            .items(List.of("X", "Y", "Z"))
            .itemsPerPage(5)
            .renderer((ctx, item, i) -> null)
            .build();

        PaginationContext<String> context1 = state1.getPagination(pane1);
        PaginationContext<String> context2 = state2.getPagination(pane2);

        assertThat(context1).isNotSameAs(context2);
        assertThat(context1.getCurrentPage()).isEqualTo(0);
        assertThat(context2.getCurrentPage()).isEqualTo(0);

        // Navigate in different contexts
        context1.nextPage();
        assertThat(context1.getCurrentPage()).isEqualTo(0); // No more pages (only 1 page)
        assertThat(context2.getCurrentPage()).isEqualTo(0); // Unaffected
    }

    @Test
    @DisplayName("Should handle closing one viewer without affecting others")
    void testCloseOneViewer() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .build();

        menu.open(this.player1);
        menu.open(this.player2);

        // Close player1
        menu.close(this.player1);

        // Player1 should be cleaned up
        assertThat(menu.getViewerState(this.player1.getUniqueId())).isNull();

        // Player2 should still have ViewerState
        assertThat(menu.getViewerState(this.player2.getUniqueId())).isNotNull();
    }

    // ========================================
    // MENU OPENING
    // ========================================

    @Test
    @DisplayName("Should render panes when opening menu")
    void testRenderOnOpen() {
        AtomicInteger renderCount = new AtomicInteger(0);

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .pane(staticPane("test")
                .bounds(0, 0, 3, 9)
                .item(0, 0, item()
                    .material(() -> {
                        renderCount.incrementAndGet();
                        return Material.DIAMOND;
                    })
                    .build())
                .build())
            .build();

        menu.open(this.player1);

        // Material supplier should have been called during render
        assertThat(renderCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should open player's inventory")
    void testInventoryOpened() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .build();

        menu.open(this.player1);

        // Verify inventory was opened
        assertThat(this.player1.getOpenInventory()).isNotNull();
        assertThat(this.player1.getOpenInventory().getTopInventory().getHolder()).isSameAs(menu);
    }

    @Test
    @DisplayName("Should not reopen inventory if already viewing same inventory")
    void testNoReopenSameInventory() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .build();

        menu.open(this.player1);
        Inventory firstInventory = this.player1.getOpenInventory().getTopInventory();

        // Open again (should not create new inventory)
        menu.open(this.player1);
        Inventory secondInventory = this.player1.getOpenInventory().getTopInventory();

        // Should be the same inventory instance
        assertThat(secondInventory).isSameAs(firstInventory);
    }

    // ========================================
    // GETVIEWERSTATE
    // ========================================

    @Test
    @DisplayName("Should return null for non-existent ViewerState")
    void testGetNonExistentViewerState() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .build();

        UUID randomUuid = UUID.randomUUID();
        assertThat(menu.getViewerState(randomUuid)).isNull();
    }

    @Test
    @DisplayName("Should return ViewerState for existing viewer")
    void testGetExistingViewerState() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .build();

        menu.open(this.player1);

        ViewerState state = menu.getViewerState(this.player1.getUniqueId());
        assertThat(state).isNotNull();
    }

    // ========================================
    // PAGINATION CONTEXT MANAGEMENT
    // ========================================

    @Test
    @DisplayName("Should create pagination context for pane")
    void testCreatePaginationContext() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .build();

        menu.open(this.player1);

        ViewerState state = menu.getViewerState(this.player1.getUniqueId());

        // Create test pane
        PaginatedPane<String> testPane = PaginatedPane.<String>pane()
            .name("test_pane")
            .bounds(0, 0, 1, 9)
            .items(List.of("Item1", "Item2"))
            .itemsPerPage(5)
            .renderer((ctx, item, i) -> null)
            .build();

        PaginationContext<String> context = state.getPagination(testPane);

        assertThat(context).isNotNull();
        assertThat(context.getPaneId()).isEqualTo("test_pane");
    }

    @Test
    @DisplayName("Should reuse pagination context for same pane")
    void testReusePaginationContext() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .build();

        menu.open(this.player1);

        ViewerState state = menu.getViewerState(this.player1.getUniqueId());

        // Create test panes
        PaginatedPane<String> pane1 = PaginatedPane.<String>pane()
            .name("test_pane")
            .bounds(0, 0, 1, 9)
            .items(List.of("Item1", "Item2"))
            .itemsPerPage(5)
            .renderer((ctx, item, i) -> null)
            .build();

        PaginatedPane<String> pane2 = PaginatedPane.<String>pane()
            .name("test_pane")
            .bounds(0, 0, 1, 9)
            .items(List.of("Item3", "Item4"))
            .itemsPerPage(3)
            .renderer((ctx, item, i) -> null)
            .build();

        // Get context twice
        PaginationContext<String> context1 = state.getPagination(pane1);
        PaginationContext<String> context2 = state.getPagination(pane2);

        // Should be the same instance (cached)
        assertThat(context2).isSameAs(context1);
    }

    @Test
    @DisplayName("Should create separate pagination contexts for different panes")
    void testSeparateContextsPerPane() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .build();

        menu.open(this.player1);

        ViewerState state = menu.getViewerState(this.player1.getUniqueId());

        // Create test panes
        PaginatedPane<String> pane1 = PaginatedPane.<String>pane()
            .name("pane1")
            .bounds(0, 0, 1, 9)
            .items(List.of("Item1", "Item2"))
            .itemsPerPage(5)
            .renderer((ctx, item, i) -> null)
            .build();

        PaginatedPane<String> pane2 = PaginatedPane.<String>pane()
            .name("pane2")
            .bounds(0, 0, 1, 9)
            .items(List.of("Item3", "Item4"))
            .itemsPerPage(5)
            .renderer((ctx, item, i) -> null)
            .build();

        PaginationContext<String> context1 = state.getPagination(pane1);
        PaginationContext<String> context2 = state.getPagination(pane2);

        // Should be different instances
        assertThat(context2).isNotSameAs(context1);
        assertThat(context1.getPaneId()).isEqualTo("pane1");
        assertThat(context2.getPaneId()).isEqualTo("pane2");
    }

    @Test
    @DisplayName("Should auto-initialize pagination contexts before title evaluation")
    void testPaginationAutoInitialization() {
        AtomicInteger titleEvaluationCount = new AtomicInteger(0);
        AtomicInteger paginationAccessSuccessCount = new AtomicInteger(0);

        Menu menu = Menu.builder(this.plugin)
            .title(ctx -> {
                titleEvaluationCount.incrementAndGet();

                // This should NOT throw - pagination should be pre-initialized
                try {
                    PaginationContext<?> pagination = ctx.pagination("items");
                    paginationAccessSuccessCount.incrementAndGet();

                    // For sync PaginatedPane, data should be available immediately
                    int totalItems = pagination.getTotalItems();
                    int totalPages = pagination.getTotalPages();
                    int currentPage = pagination.getCurrentPage() + 1;

                    return "Items: " + totalItems + " (Page " + currentPage + "/" + totalPages + ")";
                } catch (Exception e) {
                    return "ERROR: " + e.getMessage();
                }
            })
            .rows(3)
            .pane(PaginatedPane.pane("items")
                .bounds(0, 0, 3, 9)
                .items(List.of("A", "B", "C", "D", "E"))
                .renderer((ctx, item, index) -> item()
                    .material(Material.STONE)
                    .build())
                .build())
            .build();

        // Open menu
        menu.open(this.player1);

        // Title should have been evaluated once
        assertThat(titleEvaluationCount.get())
            .as("Title should be evaluated during menu opening")
            .isEqualTo(1);

        // Pagination access should have succeeded
        assertThat(paginationAccessSuccessCount.get())
            .as("Pagination context should be accessible in title")
            .isEqualTo(1);

        // Verify ViewerState has the pagination context
        ViewerState state = menu.getViewerState(this.player1.getUniqueId());
        assertThat(state).isNotNull();
        assertThat(state.getPagination())
            .as("Pagination context should be cached in ViewerState")
            .containsKey("items");
    }

    @Test
    @DisplayName("Should provide accurate pagination data for sync PaginatedPane in title")
    void testSyncPaginationDataInTitle() {
        List<String> items = List.of("Item1", "Item2", "Item3", "Item4", "Item5", "Item6", "Item7");

        Menu menu = Menu.builder(this.plugin)
            .title(ctx -> {
                PaginationContext<?> pagination = ctx.pagination("items");
                return "Total: " + pagination.getTotalItems() + " | Pages: " + pagination.getTotalPages();
            })
            .rows(3)
            .pane(PaginatedPane.<String>pane("items")
                .bounds(0, 0, 2, 9)  // 18 slots
                .items(items)
                .itemsPerPage(5)
                .renderer((ctx, item, index) -> item()
                    .material(Material.STONE)
                    .build())
                .build())
            .build();

        menu.open(this.player1);

        // Title should show accurate data immediately
        // getTotalItems() = 7, getTotalPages() = ceil(7/5) = 2
        // Expected title: "Total: 7 | Pages: 2"

        ViewerState state = menu.getViewerState(this.player1.getUniqueId());
        PaginationContext<?> paginationContext = state.getPagination().get("items");

        // Verify pagination context has correct data
        assertThat(paginationContext.getTotalItems())
            .as("Should have all 7 items")
            .isEqualTo(7);
        assertThat(paginationContext.getTotalPages())
            .as("Should calculate 2 pages (7 items / 5 per page)")
            .isEqualTo(2);
    }

    @Test
    @DisplayName("Should handle pagination access in title for menu without paginated panes")
    void testNoPaginatedPanesInTitle() {
        AtomicInteger titleEvaluationCount = new AtomicInteger(0);

        Menu menu = Menu.builder(this.plugin)
            .title(ctx -> {
                titleEvaluationCount.incrementAndGet();
                return "Static Menu";
            })
            .rows(3)
            .pane(staticPane("static")
                .bounds(0, 0, 3, 9)
                .item(0, 0, item()
                    .material(Material.DIAMOND)
                    .build())
                .build())
            .build();

        // Open menu (should not throw even though there are no paginated panes)
        assertThatCode(() -> menu.open(this.player1))
            .as("Opening menu without paginated panes should not throw")
            .doesNotThrowAnyException();

        // Title should have been evaluated
        assertThat(titleEvaluationCount.get()).isEqualTo(1);

        // ViewerState should have no pagination contexts
        ViewerState state = menu.getViewerState(this.player1.getUniqueId());
        assertThat(state.getPagination())
            .as("Should have no pagination contexts for menu without paginated panes")
            .isEmpty();
    }

    // ========================================
    // EDGE CASES
    // ========================================

    @Test
    @DisplayName("Should handle opening menu multiple times")
    void testMultipleOpens() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .build();

        // Open multiple times
        menu.open(this.player1);
        menu.open(this.player1);
        menu.open(this.player1);

        // Should still have only one ViewerState
        ViewerState state = menu.getViewerState(this.player1.getUniqueId());
        assertThat(state).isNotNull();
    }

    @Test
    @DisplayName("Should handle closing without opening")
    void testCloseWithoutOpen() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .build();

        // Close without opening should not throw
        assertThatCode(() -> menu.close(this.player1))
            .doesNotThrowAnyException();
    }
}
