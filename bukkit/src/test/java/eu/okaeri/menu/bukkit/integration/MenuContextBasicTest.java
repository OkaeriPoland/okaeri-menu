package eu.okaeri.menu.bukkit.integration;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.MenuContext;
import eu.okaeri.menu.async.Computed;
import eu.okaeri.menu.item.MenuItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static eu.okaeri.menu.pane.StaticPane.staticPane;
import static org.assertj.core.api.Assertions.*;


/**
 * Integration tests for basic MenuContext operations.
 * Tests refresh(), pane(), closeInventory(), and loadAsync().
 */
class MenuContextBasicTest {

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
    // MENU OPERATIONS
    // ========================================

    @Test
    @DisplayName("Should get pane by name")
    void testGetPane() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .pane(staticPane("main")
                .bounds(0, 0, 3, 9)
                .build())
            .build();

        menu.open(this.player);
        MenuContext context = new MenuContext(menu, this.player);

        assertThat(context.pane("main")).isNotNull();
        assertThat(context.pane("nonexistent")).isNull();
    }

    @Test
    @DisplayName("Should close inventory")
    void testCloseInventory() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .build();

        menu.open(this.player);
        MenuContext context = new MenuContext(menu, this.player);

        // ViewerState should exist
        assertThat(menu.getViewerState(this.player.getUniqueId())).isNotNull();

        // Close via context
        context.closeInventory();

        // ViewerState should be cleaned up
        assertThat(menu.getViewerState(this.player.getUniqueId())).isNull();
    }

    // ========================================
    // ASYNC DATA
    // ========================================

    @Test
    @DisplayName("Should return empty computed value when no ViewerState")
    void testComputedNoViewerState() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .build();

        // Don't open menu
        MenuContext context = new MenuContext(menu, this.player);

        Computed<String> computed = context.computed("test_key");

        assertThat(computed.isPresent()).isFalse();
    }

    @Test
    @DisplayName("Should return empty computed value when key not found")
    void testComputedKeyNotFound() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .build();

        menu.open(this.player);
        MenuContext context = new MenuContext(menu, this.player);

        Computed<String> computed = context.computed("nonexistent");

        assertThat(computed.isPresent()).isFalse();
    }

    @Test
    @DisplayName("Should load data async and cache it")
    void testLoadAsync() throws Exception {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .build();

        menu.open(this.player);
        MenuContext context = new MenuContext(menu, this.player);

        // Load data
        CompletableFuture<String> future = context.loadAsync(
            "test_key",
            () -> "Test Value",
            Duration.ofSeconds(1)
        );

        String result = future.get(1, TimeUnit.SECONDS);
        assertThat(result).isEqualTo("Test Value");

        // Wait for cache update
        Thread.sleep(100);

        // Should be in cache
        Computed<String> computed = context.computed("test_key");
        assertThat(computed.isPresent()).isTrue();
        assertThat(computed.toOptional()).hasValue("Test Value");
    }

    @Test
    @DisplayName("Should fail loadAsync when no ViewerState")
    void testLoadAsyncNoViewerState() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .build();

        MenuContext context = new MenuContext(menu, this.player);

        CompletableFuture<String> future = context.loadAsync(
            "test_key",
            () -> "Test Value",
            Duration.ofSeconds(1)
        );

        assertThat(future).isCompletedExceptionally();
    }

    @Test
    @DisplayName("Should return empty getComputed when data not loaded")
    void testGetComputedEmpty() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .build();

        menu.open(this.player);
        MenuContext context = new MenuContext(menu, this.player);

        Optional<String> value = context.computed("test_key", String.class).toOptional();

        assertThat(value).isEmpty();
    }

    @Test
    @DisplayName("Should return value via getComputed when loaded")
    void testGetComputedPresent() throws Exception {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .build();

        menu.open(this.player);
        MenuContext context = new MenuContext(menu, this.player);

        // Load data
        context.loadAsync("test_key", () -> "Test Value", Duration.ofSeconds(1)).get(1, TimeUnit.SECONDS);
        Thread.sleep(100);

        Optional<String> value = context.computed("test_key", String.class).toOptional();

        assertThat(value).hasValue("Test Value");
    }

    @Test
    @DisplayName("Should invalidate cached data")
    void testInvalidate() throws Exception {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .build();

        menu.open(this.player);
        MenuContext context = new MenuContext(menu, this.player);

        // Load data
        context.loadAsync("test_key", () -> "Test Value", Duration.ofSeconds(10)).get(1, TimeUnit.SECONDS);
        Thread.sleep(100);

        Computed<String> before = context.computed("test_key");
        assertThat(before.isPresent()).isTrue();

        // Invalidate (stale-while-revalidate: value still present but expired)
        context.invalidate("test_key");

        Computed<String> after = context.computed("test_key");
        assertThat(after.isPresent()).isTrue();  // Value still present during stale-while-revalidate
    }

    @Test
    @DisplayName("Should handle invalidate when no ViewerState")
    void testInvalidateNoViewerState() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .build();

        MenuContext context = new MenuContext(menu, this.player);

        assertThatCode(() -> context.invalidate("test_key"))
            .doesNotThrowAnyException();
    }

    // ========================================
    // SLOT CONTENTS
    // ========================================

    @Test
    @DisplayName("Should read items from pane bounds")
    void testGetSlotContents() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane(staticPane("content")
                .bounds(0, 0, 1, 9)
                .item(0, 0, MenuItem.item().interactive().build())
                .item(0, 1, MenuItem.item().interactive().build())
                .build())
            .build();

        menu.open(this.player);

        // Place items
        Inventory inv = this.player.getOpenInventory().getTopInventory();
        inv.setItem(0, new ItemStack(Material.DIAMOND, 5));
        inv.setItem(1, new ItemStack(Material.EMERALD, 3));

        MenuContext context = new MenuContext(menu, this.player);
        List<ItemStack> items = context.getSlotContents("content");

        assertThat(items).hasSize(2);
        assertThat(items.get(0).getType()).isEqualTo(Material.DIAMOND);
        assertThat(items.get(0).getAmount()).isEqualTo(5);
        assertThat(items.get(1).getType()).isEqualTo(Material.EMERALD);
        assertThat(items.get(1).getAmount()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should filter null and AIR items from slot contents")
    void testGetSlotContentsFiltersEmpty() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane(staticPane("content")
                .bounds(0, 0, 1, 9)
                .item(0, 0, MenuItem.item().interactive().build())
                .item(0, 1, MenuItem.item().interactive().build())
                .item(0, 2, MenuItem.item().interactive().build())
                .build())
            .build();

        menu.open(this.player);

        // Only place item in slot 1 (slot 0 and 2 are empty)
        Inventory inv = this.player.getOpenInventory().getTopInventory();
        inv.setItem(1, new ItemStack(Material.GOLD_INGOT));

        MenuContext context = new MenuContext(menu, this.player);
        List<ItemStack> items = context.getSlotContents("content");

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getType()).isEqualTo(Material.GOLD_INGOT);
    }

    @Test
    @DisplayName("Should return cloned items from slot contents")
    void testGetSlotContentsReturnsClones() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane(staticPane("content")
                .bounds(0, 0, 1, 9)
                .item(0, 0, MenuItem.item().interactive().build())
                .build())
            .build();

        menu.open(this.player);

        Inventory inv = this.player.getOpenInventory().getTopInventory();
        inv.setItem(0, new ItemStack(Material.DIAMOND));

        MenuContext context = new MenuContext(menu, this.player);
        List<ItemStack> items = context.getSlotContents("content");

        // Modify returned item - should not affect inventory
        items.get(0).setAmount(64);

        assertThat(inv.getItem(0).getAmount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return empty list for empty pane")
    void testGetSlotContentsEmptyPane() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane(staticPane("content")
                .bounds(0, 0, 1, 9)
                .build())
            .build();

        menu.open(this.player);

        MenuContext context = new MenuContext(menu, this.player);
        List<ItemStack> items = context.getSlotContents("content");

        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("Should throw for non-existent pane name")
    void testGetSlotContentsInvalidPane() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .build();

        menu.open(this.player);

        MenuContext context = new MenuContext(menu, this.player);

        assertThatThrownBy(() -> context.getSlotContents("nonexistent"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Pane not found");
    }

    @Test
    @DisplayName("Should handle pane with non-zero bounds offset")
    void testGetSlotContentsWithOffset() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .pane(staticPane("content")
                .bounds(1, 2, 1, 3)  // Starts at row 1, col 2
                .item(0, 0, MenuItem.item().interactive().build())
                .build())
            .build();

        menu.open(this.player);

        // Place item at global slot 11 (row 1, col 2 = pane local 0,0)
        Inventory inv = this.player.getOpenInventory().getTopInventory();
        inv.setItem(11, new ItemStack(Material.IRON_INGOT));

        MenuContext context = new MenuContext(menu, this.player);
        List<ItemStack> items = context.getSlotContents("content");

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getType()).isEqualTo(Material.IRON_INGOT);
    }

    @Test
    @DisplayName("Should return empty list when ViewerState is null")
    void testGetSlotContentsNoViewerState() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane(staticPane("content")
                .bounds(0, 0, 1, 9)
                .build())
            .build();

        // Don't open menu - no ViewerState
        MenuContext context = new MenuContext(menu, this.player);
        List<ItemStack> items = context.getSlotContents("content");

        assertThat(items).isEmpty();
    }
}
