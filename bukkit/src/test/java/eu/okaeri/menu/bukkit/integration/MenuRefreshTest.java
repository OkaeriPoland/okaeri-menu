package eu.okaeri.menu.bukkit.integration;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.item.MenuItem;
import eu.okaeri.menu.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration tests for Menu refresh mechanisms.
 * Tests full refresh, pane refresh, and title updates.
 */
class MenuRefreshTest {

    private static ServerMock server;
    private JavaPlugin plugin;
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
        this.player = server.addPlayer();
    }

    // ========================================
    // FULL MENU REFRESH
    // ========================================

    @Test
    @DisplayName("Should re-render all panes on refresh")
    void testRefreshRerendersAllPanes() {
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

        menu.open(this.player);
        int initialRenderCount = renderCount.get();

        // Refresh should re-render
        menu.refresh(this.player);

        assertThat(renderCount.get()).isGreaterThan(initialRenderCount);
    }

    @Test
    @DisplayName("Should update title on refresh when title is dynamic")
    void testRefreshDynamicTitle() {
        AtomicReference<String> title = new AtomicReference<>("Title 1");

        Menu menu = Menu.builder(this.plugin)
            .title(title::get)
            .rows(3)
            .build();

        menu.open(this.player);
        Inventory firstInventory = this.player.getOpenInventory().getTopInventory();

        // Change title and refresh
        title.set("Title 2");
        menu.refresh(this.player);

        Inventory secondInventory = this.player.getOpenInventory().getTopInventory();

        // Should have different inventory (because title changed)
        assertThat(secondInventory).isNotSameAs(firstInventory);
    }

    @Test
    @DisplayName("Should not reopen inventory if title unchanged")
    void testRefreshStaticTitle() {
        Menu menu = Menu.builder(this.plugin)
            .title("Static Title")
            .rows(3)
            .build();

        menu.open(this.player);
        Inventory firstInventory = this.player.getOpenInventory().getTopInventory();

        // Refresh with same title
        menu.refresh(this.player);

        Inventory secondInventory = this.player.getOpenInventory().getTopInventory();

        // Should be same inventory (title didn't change)
        assertThat(secondInventory).isSameAs(firstInventory);
    }

    @Test
    @DisplayName("Should invalidate all panes on refresh")
    void testRefreshInvalidatesAllPanes() {
        AtomicInteger pane1RenderCount = new AtomicInteger(0);
        AtomicInteger pane2RenderCount = new AtomicInteger(0);

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .pane("pane1", StaticPane.builder()
                .bounds(0, 0, 4, 3)
                .item(0, 0, MenuItem.item()
                    .material(() -> {
                        pane1RenderCount.incrementAndGet();
                        return Material.DIAMOND;
                    })
                    .build())
                .build())
            .pane("pane2", StaticPane.builder()
                .bounds(5, 0, 4, 3)
                .item(0, 0, MenuItem.item()
                    .material(() -> {
                        pane2RenderCount.incrementAndGet();
                        return Material.EMERALD;
                    })
                    .build())
                .build())
            .build();

        menu.open(this.player);
        int pane1Initial = pane1RenderCount.get();
        int pane2Initial = pane2RenderCount.get();

        // Refresh all
        menu.refresh(this.player);

        // Both panes should have re-rendered
        assertThat(pane1RenderCount.get()).isGreaterThan(pane1Initial);
        assertThat(pane2RenderCount.get()).isGreaterThan(pane2Initial);
    }

    @Test
    @DisplayName("Should handle refresh when player not viewing menu")
    void testRefreshNonViewer() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .build();

        // Don't open menu

        // Refresh should not throw
        assertThatCode(() -> menu.refresh(this.player))
            .doesNotThrowAnyException();
    }

    // ========================================
    // PANE REFRESH
    // ========================================

    @Test
    @DisplayName("Should refresh specific pane only")
    void testRefreshPaneSpecific() {
        AtomicInteger pane1RenderCount = new AtomicInteger(0);
        AtomicInteger pane2RenderCount = new AtomicInteger(0);

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .pane("pane1", StaticPane.builder()
                .bounds(0, 0, 4, 3)
                .item(0, 0, MenuItem.item()
                    .material(() -> {
                        pane1RenderCount.incrementAndGet();
                        return Material.DIAMOND;
                    })
                    .build())
                .build())
            .pane("pane2", StaticPane.builder()
                .bounds(5, 0, 4, 3)
                .item(0, 0, MenuItem.item()
                    .material(() -> {
                        pane2RenderCount.incrementAndGet();
                        return Material.EMERALD;
                    })
                    .build())
                .build())
            .build();

        menu.open(this.player);
        int pane1Initial = pane1RenderCount.get();
        int pane2Initial = pane2RenderCount.get();

        // Refresh only pane1
        menu.refreshPane(this.player, "pane1");

        // Only pane1 should have re-rendered
        assertThat(pane1RenderCount.get()).isGreaterThan(pane1Initial);
        assertThat(pane2RenderCount.get()).isEqualTo(pane2Initial);
    }

    @Test
    @DisplayName("Should update title on refreshPane when title is dynamic")
    void testRefreshPaneDynamicTitle() {
        AtomicReference<String> title = new AtomicReference<>("Title 1");

        Menu menu = Menu.builder(this.plugin)
            .title(title::get)
            .rows(3)
            .pane("main", StaticPane.builder()
                .bounds(0, 0, 9, 3)
                .build())
            .build();

        menu.open(this.player);
        Inventory firstInventory = this.player.getOpenInventory().getTopInventory();

        // Change title and refresh pane
        title.set("Title 2");
        menu.refreshPane(this.player, "main");

        Inventory secondInventory = this.player.getOpenInventory().getTopInventory();

        // Should have different inventory (because title changed)
        assertThat(secondInventory).isNotSameAs(firstInventory);
    }

    @Test
    @DisplayName("Should not reopen inventory on refreshPane if title unchanged")
    void testRefreshPaneStaticTitle() {
        Menu menu = Menu.builder(this.plugin)
            .title("Static Title")
            .rows(3)
            .pane("main", StaticPane.builder()
                .bounds(0, 0, 9, 3)
                .build())
            .build();

        menu.open(this.player);
        Inventory firstInventory = this.player.getOpenInventory().getTopInventory();

        // Refresh pane with same title
        menu.refreshPane(this.player, "main");

        Inventory secondInventory = this.player.getOpenInventory().getTopInventory();

        // Should be same inventory (title didn't change)
        assertThat(secondInventory).isSameAs(firstInventory);
    }

    @Test
    @DisplayName("Should handle refreshPane with invalid pane name")
    void testRefreshPaneInvalid() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .pane("main", StaticPane.builder()
                .bounds(0, 0, 9, 3)
                .build())
            .build();

        menu.open(this.player);

        // Refresh with invalid pane name should not throw
        assertThatCode(() -> menu.refreshPane(this.player, "nonexistent"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle refreshPane when player not viewing menu")
    void testRefreshPaneNonViewer() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .pane("main", StaticPane.builder()
                .bounds(0, 0, 9, 3)
                .build())
            .build();

        // Don't open menu

        // RefreshPane should not throw
        assertThatCode(() -> menu.refreshPane(this.player, "main"))
            .doesNotThrowAnyException();
    }

    // ========================================
    // TITLE UPDATES
    // ========================================

    @Test
    @DisplayName("Should preserve inventory contents when title changes")
    void testTitleChangePreservesContents() {
        AtomicReference<String> title = new AtomicReference<>("Title 1");

        Menu menu = Menu.builder(this.plugin)
            .title(title::get)
            .rows(3)
            .pane("main", StaticPane.builder()
                .bounds(0, 0, 9, 3)
                .item(0, 0, MenuItem.item()
                    .material(Material.DIAMOND)
                    .build())
                .build())
            .build();

        menu.open(this.player);

        // Change title and refresh
        title.set("Title 2");
        menu.refresh(this.player);

        // Item should still be there
        Inventory inventory = this.player.getOpenInventory().getTopInventory();
        assertThat(inventory.getItem(0)).isNotNull();
        assertThat(inventory.getItem(0).getType()).isEqualTo(Material.DIAMOND);
    }

    @Test
    @DisplayName("Should clear old inventory when title changes")
    void testTitleChangeClearsOldInventory() {
        AtomicReference<String> title = new AtomicReference<>("Title 1");

        Menu menu = Menu.builder(this.plugin)
            .title(title::get)
            .rows(3)
            .pane("main", StaticPane.builder()
                .bounds(0, 0, 9, 3)
                .item(0, 0, MenuItem.item()
                    .material(Material.DIAMOND)
                    .build())
                .build())
            .build();

        menu.open(this.player);
        Menu.ViewerState state = menu.getViewerState(this.player.getUniqueId());
        Inventory oldInventory = state.getInventory();

        // Change title and refresh
        title.set("Title 2");
        menu.refresh(this.player);

        // Old inventory should be cleared (for glitch safety)
        assertThat(oldInventory.getItem(0)).isNull();
    }

    // ========================================
    // DYNAMIC CONTENT UPDATES
    // ========================================

    @Test
    @DisplayName("Should reflect dynamic content changes on refresh")
    void testDynamicContentRefresh() {
        AtomicReference<Material> material = new AtomicReference<>(Material.DIAMOND);

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .pane("main", StaticPane.builder()
                .bounds(0, 0, 9, 3)
                .item(0, 0, MenuItem.item()
                    .material(material::get)
                    .build())
                .build())
            .build();

        menu.open(this.player);

        Inventory inventory = this.player.getOpenInventory().getTopInventory();
        assertThat(inventory.getItem(0).getType()).isEqualTo(Material.DIAMOND);

        // Change material and refresh
        material.set(Material.EMERALD);
        menu.refresh(this.player);

        assertThat(inventory.getItem(0).getType()).isEqualTo(Material.EMERALD);
    }

    @Test
    @DisplayName("Should reflect dynamic visibility changes on refresh")
    void testDynamicVisibilityRefresh() {
        AtomicReference<Boolean> visible = new AtomicReference<>(true);

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .pane("main", StaticPane.builder()
                .bounds(0, 0, 9, 3)
                .item(0, 0, MenuItem.item()
                    .material(Material.DIAMOND)
                    .visible(visible::get)
                    .build())
                .build())
            .build();

        menu.open(this.player);

        Inventory inventory = this.player.getOpenInventory().getTopInventory();
        assertThat(inventory.getItem(0)).isNotNull();

        // Hide item and refresh
        visible.set(false);
        menu.refresh(this.player);

        assertThat(inventory.getItem(0)).isNull();
    }

    // ========================================
    // MULTIPLE VIEWERS
    // ========================================

    @Test
    @DisplayName("Should refresh only specific viewer")
    void testRefreshSpecificViewer() {
        Player player2 = server.addPlayer("Player2");

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .build();

        menu.open(this.player);
        menu.open(player2);

        Menu.ViewerState state1 = menu.getViewerState(this.player.getUniqueId());
        Menu.ViewerState state2 = menu.getViewerState(player2.getUniqueId());

        Inventory inv1Before = state1.getInventory();
        Inventory inv2Before = state2.getInventory();

        // Refresh only player1
        menu.refresh(this.player);

        Inventory inv1After = state1.getInventory();
        Inventory inv2After = state2.getInventory();

        // Player1's inventory might be same (if title didn't change)
        // Player2's inventory should definitely be same
        assertThat(inv2After).isSameAs(inv2Before);
    }

    @Test
    @DisplayName("Should refresh different viewers independently")
    void testIndependentViewerRefresh() {
        Player player2 = server.addPlayer("Player2");
        AtomicReference<String> title = new AtomicReference<>("Title 1");

        Menu menu = Menu.builder(this.plugin)
            .title(title::get)
            .rows(3)
            .build();

        menu.open(this.player);
        menu.open(player2);

        Inventory inv1Before = this.player.getOpenInventory().getTopInventory();
        Inventory inv2Before = player2.getOpenInventory().getTopInventory();

        // Change title and refresh only player1
        title.set("Title 2");
        menu.refresh(this.player);

        Inventory inv1After = this.player.getOpenInventory().getTopInventory();
        Inventory inv2After = player2.getOpenInventory().getTopInventory();

        // Player1 should have new inventory
        assertThat(inv1After).isNotSameAs(inv1Before);

        // Player2 should still have old inventory
        assertThat(inv2After).isSameAs(inv2Before);
    }
}
