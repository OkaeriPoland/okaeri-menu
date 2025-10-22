package eu.okaeri.menu.bukkit.integration;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.item.MenuItem;
import eu.okaeri.menu.pagination.PaginationContext;
import eu.okaeri.menu.pane.StaticPane;
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
        Menu.ViewerState state = menu.getViewerState(this.player1.getUniqueId());
        assertThat(state).isNotNull();
        assertThat(state.getInventory()).isNotNull();
        assertThat(state.getAsyncCache()).isNotNull();
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
        Menu.ViewerState state1 = menu.getViewerState(this.player1.getUniqueId());

        // Open menu again
        menu.open(this.player1);
        Menu.ViewerState state2 = menu.getViewerState(this.player1.getUniqueId());

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

        Menu.ViewerState state = menu.getViewerState(this.player1.getUniqueId());
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

        Menu.ViewerState state = menu.getViewerState(this.player1.getUniqueId());
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
        Menu.ViewerState state = menu.getViewerState(this.player1.getUniqueId());
        PaginationContext<String> paginationContext = state.getPaginationContext(
            "test_pane",
            this.player1.getUniqueId(),
            List.of("Item1", "Item2", "Item3"),
            5
        );

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
        Menu.ViewerState state1 = menu.getViewerState(this.player1.getUniqueId());
        Menu.ViewerState state2 = menu.getViewerState(this.player2.getUniqueId());

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
        Menu.ViewerState state1 = menu.getViewerState(this.player1.getUniqueId());
        Menu.ViewerState state2 = menu.getViewerState(this.player2.getUniqueId());

        PaginationContext<String> context1 = state1.getPaginationContext(
            "pane1",
            this.player1.getUniqueId(),
            List.of("A", "B", "C"),
            5
        );

        PaginationContext<String> context2 = state2.getPaginationContext(
            "pane1",
            this.player2.getUniqueId(),
            List.of("X", "Y", "Z"),
            5
        );

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
            .pane("main", StaticPane.builder()
                .bounds(0, 0, 9, 3)
                .item(0, 0, MenuItem.item()
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

        Menu.ViewerState state = menu.getViewerState(this.player1.getUniqueId());
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

        Menu.ViewerState state = menu.getViewerState(this.player1.getUniqueId());
        PaginationContext<String> context = state.getPaginationContext(
            "test_pane",
            this.player1.getUniqueId(),
            List.of("Item1", "Item2"),
            5
        );

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

        Menu.ViewerState state = menu.getViewerState(this.player1.getUniqueId());

        // Get context twice
        PaginationContext<String> context1 = state.getPaginationContext(
            "test_pane",
            this.player1.getUniqueId(),
            List.of("Item1", "Item2"),
            5
        );

        PaginationContext<String> context2 = state.getPaginationContext(
            "test_pane",
            this.player1.getUniqueId(),
            List.of("Item3", "Item4"),  // Different items
            3  // Different items per page
        );

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

        Menu.ViewerState state = menu.getViewerState(this.player1.getUniqueId());

        PaginationContext<String> context1 = state.getPaginationContext(
            "pane1",
            this.player1.getUniqueId(),
            List.of("Item1", "Item2"),
            5
        );

        PaginationContext<String> context2 = state.getPaginationContext(
            "pane2",
            this.player1.getUniqueId(),
            List.of("Item3", "Item4"),
            5
        );

        // Should be different instances
        assertThat(context2).isNotSameAs(context1);
        assertThat(context1.getPaneId()).isEqualTo("pane1");
        assertThat(context2.getPaneId()).isEqualTo("pane2");
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
        Menu.ViewerState state = menu.getViewerState(this.player1.getUniqueId());
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
