package eu.okaeri.menu.bukkit.unit.pane;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.bukkit.test.PooledAsyncExecutor;
import eu.okaeri.menu.bukkit.test.SyncTestExecutor;
import eu.okaeri.menu.item.MenuItem;
import eu.okaeri.menu.pane.AsyncPaginatedPane;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static eu.okaeri.menu.pane.AsyncPaginatedPane.paneAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for AsyncPaginatedPane.
 * Tests async loading states, rendering behavior, and cache integration.
 * <p>
 * Uses two executors:
 * - SyncTestExecutor: For tests that only verify final state (instant completion)
 * - PooledAsyncExecutor: For tests that need to observe loading states or time-based behavior
 */
class AsyncPaginatedPaneTest {

    private static ServerMock server;
    private static PooledAsyncExecutor pooledExecutor;

    private JavaPlugin plugin;
    private PlayerMock player;
    private Menu menu;

    @BeforeAll
    static void setUpServer() {
        server = MockBukkit.mock();
        pooledExecutor = PooledAsyncExecutor.create();
    }

    @AfterAll
    static void tearDownServer() {
        pooledExecutor.shutdown();
        MockBukkit.unmock();
    }

    @BeforeEach
    void setUp() {
        this.plugin = MockBukkit.createMockPlugin();
        this.player = server.addPlayer();

        // Don't open menu here - each test creates and opens its own menu
        // to avoid ViewerState conflicts
    }

    // ========================================
    // ASYNC-SPECIFIC BUILDER VALIDATION
    // ========================================
    // Note: Generic validations (name, bounds, renderer, itemsPerPage) are tested in PaginatedPaneTest
    // Only async-specific requirements are tested here

    @Test
    @DisplayName("Should require async loader")
    void testRequireLoader() {
        assertThatThrownBy(() ->
            paneAsync(String.class)
                .name("test-pane")
                .bounds(0, 0, 3, 3)
                .renderer((item, index) -> MenuItem.item().material(Material.STONE).build())
                .build()
        ).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("loader");
    }

    @Test
    @DisplayName("Should use default TTL of 30 seconds")
    void testDefaultTTL() {
        AsyncPaginatedPane<String> pane = paneAsync(String.class)
            .name("test-pane")
            .bounds(0, 0, 3, 3)
            .loader(Collections::emptyList)
            .renderer((item, index) -> MenuItem.item().material(Material.STONE).build())
            .build();

        assertThat(pane.getTtl()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    @DisplayName("Should allow custom TTL")
    void testCustomTTL() {
        AsyncPaginatedPane<String> pane = paneAsync(String.class)
            .name("test-pane")
            .bounds(0, 0, 3, 3)
            .loader(Collections::emptyList)
            .ttl(Duration.ofMinutes(5))
            .renderer((item, index) -> MenuItem.item().material(Material.STONE).build())
            .build();

        assertThat(pane.getTtl()).isEqualTo(Duration.ofMinutes(5));
    }

    // ========================================
    // LOADING STATE RENDERING
    // ========================================

    @Test
    @DisplayName("Should render success state with loaded data")
    void testRenderSuccessStateWithData() {
        List<String> testData = Arrays.asList("A", "B", "C");

        Menu testMenu = Menu.builder(this.plugin)
            .asyncExecutor(SyncTestExecutor.create())  // Sync executor for instant completion
            .title("Async Menu")
            .rows(3)
            .pane("async-pane", paneAsync(String.class)
                .name("async-pane")
                .bounds(0, 0, 3, 2)  // 3x2 = 6 slots
                .loader(() -> testData)
                .renderer((item, index) -> MenuItem.item()
                    .material(Material.DIAMOND)
                    .name(item)
                    .build())
                .build())
            .build();

        // Open triggers first render (LOADING state) and starts async load
        testMenu.open(this.player);

        // With SyncTestExecutor, async task completes immediately
        // The whenComplete schedules a refresh via runTask
        // Execute the scheduled refresh task
        server.getScheduler().performOneTick();

        Inventory inventory = this.player.getOpenInventory().getTopInventory();

        // Verify items are rendered as diamonds (SUCCESS state)
        ItemStack firstItem = inventory.getItem(0);
        assertThat(firstItem).isNotNull();
        assertThat(firstItem.getType()).isEqualTo(Material.DIAMOND);

        ItemStack secondItem = inventory.getItem(1);
        assertThat(secondItem).isNotNull();
        assertThat(secondItem.getType()).isEqualTo(Material.DIAMOND);
    }

    // ========================================
    // EMPTY STATE RENDERING
    // ========================================

    @Test
    @DisplayName("Should render empty state when loader returns empty list")
    void testRenderEmptyState() {
        Menu testMenu = Menu.builder(this.plugin)
            .asyncExecutor(SyncTestExecutor.create())  // Sync executor for instant completion
            .title("Async Menu")
            .rows(3)
            .pane("async-pane", paneAsync(String.class)
                .name("async-pane")
                .bounds(0, 0, 3, 3)  // 3x3 = 9 slots
                .loader(Collections::emptyList)
                .renderer((item, index) -> MenuItem.item()
                    .material(Material.DIAMOND)
                    .build())
                .loading(MenuItem.item()
                    .material(Material.CLOCK)
                    .build())
                .error(MenuItem.item()
                    .material(Material.BARRIER)
                    .build())
                .empty(MenuItem.item()
                    .material(Material.GRAY_STAINED_GLASS_PANE)
                    .name("No items")
                    .build())
                .build())
            .build();

        // Open triggers first render (LOADING state) and starts async load
        testMenu.open(this.player);

        // Execute the scheduled refresh task after async load completes
        server.getScheduler().performOneTick();

        Inventory inventory = this.player.getOpenInventory().getTopInventory();

        // Empty item should be at (0,0) of pane = global slot 0
        ItemStack emptyItem = inventory.getItem(0);
        assertThat(emptyItem).isNotNull();
        assertThat(emptyItem.getType()).isEqualTo(Material.GRAY_STAINED_GLASS_PANE);
    }

    // ========================================
    // SUCCESS STATE RENDERING
    // ========================================

    @Test
    @DisplayName("Should render success state with items")
    void testRenderSuccessState() {
        List<String> testData = Arrays.asList("Item A", "Item B", "Item C");

        Menu testMenu = Menu.builder(this.plugin)
            .asyncExecutor(SyncTestExecutor.create())
            .title("Async Menu")
            .rows(3)
            .pane("async-pane", paneAsync(String.class)
                .name("async-pane")
                .bounds(0, 0, 3, 2)  // 3x2 = 6 slots
                .loader(() -> testData)
                .renderer((item, index) -> MenuItem.item()
                    .material(Material.DIAMOND)
                    .name(item)
                    .build())
                .build())
            .build();

        testMenu.open(this.player);
        server.getScheduler().performOneTick();  // Execute scheduled refresh after async load

        Inventory inventory = this.player.getOpenInventory().getTopInventory();

        // First 3 slots should have diamond items
        ItemStack item0 = inventory.getItem(0);
        assertThat(item0).isNotNull();
        assertThat(item0.getType()).isEqualTo(Material.DIAMOND);
    }

    // ========================================
    // STATIC ITEMS
    // ========================================

    @Test
    @DisplayName("Should render static items in loading state")
    void testStaticItemsInLoadingState() throws InterruptedException {
        // This test NEEDS real async behavior to observe loading state
        Menu testMenu = Menu.builder(this.plugin)
            .asyncExecutor(pooledExecutor)  // Use pooled executor for real async
            .title("Async Menu")
            .rows(3)
            .pane("async-pane", paneAsync(String.class)
                .name("async-pane")
                .bounds(0, 0, 3, 2)
                .loader(() -> {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                    }
                    return Arrays.asList("A", "B");
                })
                .renderer((item, index) -> MenuItem.item()
                    .material(Material.DIAMOND)
                    .build())
                .staticItem(0, 1, MenuItem.item()
                    .material(Material.ARROW)
                    .name("Previous")
                    .build())
                .staticItem(2, 1, MenuItem.item()
                    .material(Material.ARROW)
                    .name("Next")
                    .build())
                .loading(MenuItem.item()
                    .material(Material.CLOCK)
                    .name("Loading...")
                    .build())
                .error(MenuItem.item()
                    .material(Material.BARRIER)
                    .build())
                .empty(MenuItem.item()
                    .material(Material.GRAY_STAINED_GLASS_PANE)
                    .build())
                .build())
            .build();

        testMenu.open(this.player);
        server.getScheduler().performOneTick();  // Execute scheduled refresh after async load
        testMenu.refresh(this.player);

        // Check immediately - data should still be loading
        Thread.sleep(50);  // Small delay for async task to start
        testMenu.refresh(this.player);

        Inventory inventory = this.player.getOpenInventory().getTopInventory();

        // Static items should be visible even while data is loading
        // Pane at (0,0), static at local (0,1) = global slot 9, static at local (2,1) = global slot 11
        ItemStack prevButton = inventory.getItem(9);
        ItemStack nextButton = inventory.getItem(11);

        assertThat(prevButton).isNotNull();
        assertThat(prevButton.getType()).isEqualTo(Material.ARROW);

        assertThat(nextButton).isNotNull();
        assertThat(nextButton.getType()).isEqualTo(Material.ARROW);

        // Loading items should be visible in non-static slots
        // Slot 0 (x=0, y=0 within pane at 0,0) = global slot 0
        ItemStack loadingItem = inventory.getItem(0);
        assertThat(loadingItem).isNotNull();
        assertThat(loadingItem.getType()).isEqualTo(Material.CLOCK);
    }

    // ========================================
    // CACHE INTEGRATION
    // ========================================

    @Test
    @DisplayName("Should use pane name as cache key")
    void testCacheKeyUsage() {
        AsyncPaginatedPane<String> pane = paneAsync(String.class)
            .name("my-pane")
            .bounds(0, 0, 3, 3)
            .loader(() -> Arrays.asList("A", "B", "C"))
            .renderer((item, index) -> MenuItem.item().material(Material.STONE).build())
            .build();

        assertThat(pane.getName()).isEqualTo("my-pane");
    }

    @Test
    @DisplayName("Should reload data after TTL expiration")
    void testTTLExpiration() throws InterruptedException {
        int[] loadCount = {0};

        // This test NEEDS real async behavior and time passage
        Menu testMenu = Menu.builder(this.plugin)
            .asyncExecutor(pooledExecutor)  // Use pooled executor for real async
            .title("Async Menu")
            .rows(3)
            .pane("async-pane", paneAsync(String.class)
                .name("async-pane")
                .bounds(0, 0, 3, 2)
                .loader(() -> {
                    loadCount[0]++;
                    return List.of("Load " + loadCount[0]);
                })
                .ttl(Duration.ofMillis(100))  // Short TTL for faster test
                .renderer((item, index) -> MenuItem.item()
                    .material(Material.DIAMOND)
                    .name(item)
                    .build())
                .build())
            .build();

        testMenu.open(this.player);
        server.getScheduler().performOneTick();  // Execute scheduled refresh after async load
        testMenu.refresh(this.player);  // First load
        Thread.sleep(50);  // Wait for async load
        testMenu.refresh(this.player);

        Thread.sleep(150);  // Wait for TTL to expire
        testMenu.refresh(this.player);  // Should trigger second load
        Thread.sleep(50);  // Wait for async load

        // Loader should have been called at least twice due to TTL expiration
        assertThat(loadCount[0]).isGreaterThanOrEqualTo(2);
    }

    // ========================================
    // EDGE CASES
    // ========================================

    @Test
    @DisplayName("Should handle single item list")
    void testSingleItem() {
        Menu testMenu = Menu.builder(this.plugin)
            .asyncExecutor(SyncTestExecutor.create())
            .title("Async Menu")
            .rows(3)
            .pane("async-pane", paneAsync(String.class)
                .name("async-pane")
                .bounds(0, 0, 3, 2)
                .loader(() -> Collections.singletonList("Only Item"))
                .renderer((item, index) -> MenuItem.item()
                    .material(Material.EMERALD)
                    .name(item)
                    .build())
                .build())
            .build();

        testMenu.open(this.player);
        server.getScheduler().performOneTick();  // Execute scheduled refresh after async load

        Inventory inventory = this.player.getOpenInventory().getTopInventory();
        ItemStack item = inventory.getItem(0);

        assertThat(item).isNotNull();
        assertThat(item.getType()).isEqualTo(Material.EMERALD);
    }

    @Test
    @DisplayName("Should handle large item list")
    void testLargeItemList() {
        // Create 100 items
        List<String> largeList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeList.add("Item " + i);
        }

        Menu testMenu = Menu.builder(this.plugin)
            .asyncExecutor(SyncTestExecutor.create())
            .title("Async Menu")
            .rows(3)
            .pane("async-pane", paneAsync(String.class)
                .name("async-pane")
                .bounds(0, 0, 7, 2)  // 7x2 = 14 slots
                .itemsPerPage(10)
                .loader(() -> largeList)
                .renderer((item, index) -> MenuItem.item()
                    .material(Material.STONE)
                    .name(item)
                    .build())
                .build())
            .build();

        testMenu.open(this.player);
        server.getScheduler().performOneTick();  // Execute scheduled refresh after async load

        // First page should show first 10 items (slots 0-9)
        Inventory inventory = this.player.getOpenInventory().getTopInventory();
        ItemStack firstItem = inventory.getItem(0);

        assertThat(firstItem).isNotNull();
        assertThat(firstItem.getType()).isEqualTo(Material.STONE);
    }

    // ========================================
    // ASYNC UTILS DEFAULT TESTS
    // ========================================

    @Test
    @DisplayName("Should use AsyncUtils defaults when suspense items not provided")
    void testUsesAsyncUtilsDefaults() {
        // Create pane WITHOUT providing loading/error/empty items
        AsyncPaginatedPane<String> pane = paneAsync(String.class)
            .name("test-pane")
            .bounds(0, 0, 3, 3)
            .loader(() -> Arrays.asList("Item 1", "Item 2"))
            .renderer((item, index) -> MenuItem.item()
                .material(Material.STONE)
                .name(item)
                .build())
            .build();

        // Defaults should be set
        assertThat(pane.getLoadingItem()).isNotNull();
        assertThat(pane.getErrorItem()).isNotNull();
        assertThat(pane.getEmptyItem()).isNotNull();
    }

    @Test
    @DisplayName("Should use custom suspense items when provided")
    void testUsesCustomSuspenseItems() {
        MenuItem customLoading = MenuItem.item()
            .material(Material.CLOCK)
            .name("Custom Loading")
            .build();
        MenuItem customError = MenuItem.item()
            .material(Material.REDSTONE_BLOCK)
            .name("Custom Error")
            .build();
        MenuItem customEmpty = MenuItem.item()
            .material(Material.GLASS_PANE)
            .name("Custom Empty")
            .build();

        AsyncPaginatedPane<String> pane = paneAsync(String.class)
            .name("test-pane")
            .bounds(0, 0, 3, 3)
            .loader(() -> Arrays.asList("Item 1", "Item 2"))
            .renderer((item, index) -> MenuItem.item()
                .material(Material.STONE)
                .name(item)
                .build())
            .loading(customLoading)
            .error(customError)
            .empty(customEmpty)
            .build();

        // Custom items should be used
        assertThat(pane.getLoadingItem()).isSameAs(customLoading);
        assertThat(pane.getErrorItem()).isSameAs(customError);
        assertThat(pane.getEmptyItem()).isSameAs(customEmpty);
    }

    @Test
    @DisplayName("Should render with default loading item")
    void testRendersWithDefaultLoadingItem() {
        Menu testMenu = Menu.builder(this.plugin)
            .asyncExecutor(pooledExecutor)
            .title("Async Menu")
            .rows(3)
            .pane("async-pane", paneAsync(String.class)
                .name("async-pane")
                .bounds(0, 0, 3, 3)
                .loader(() -> {
                    // Simulate async delay to stay in LOADING state
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return Arrays.asList("Item 1", "Item 2");
                })
                .renderer((item, index) -> MenuItem.item()
                    .material(Material.STONE)
                    .name(item)
                    .build())
                // NOT providing loading/error/empty - should use defaults
                .build())
            .build();

        testMenu.open(this.player);

        // Should render loading state with default item
        Inventory inventory = this.player.getOpenInventory().getTopInventory();
        ItemStack loadingSlot = inventory.getItem(0);

        assertThat(loadingSlot).isNotNull();
        // Default loading item uses HOPPER
        assertThat(loadingSlot.getType()).isEqualTo(Material.HOPPER);
    }

    @Test
    @DisplayName("Should render with default empty item")
    void testRendersWithDefaultEmptyItem() {
        Menu testMenu = Menu.builder(this.plugin)
            .asyncExecutor(SyncTestExecutor.create())
            .title("Async Menu")
            .rows(3)
            .pane("async-pane", paneAsync(String.class)
                .name("async-pane")
                .bounds(0, 0, 3, 3)
                .loader(Collections::emptyList)  // Returns empty list
                .renderer((item, index) -> MenuItem.item()
                    .material(Material.STONE)
                    .name(item)
                    .build())
                // NOT providing loading/error/empty - should use defaults
                .build())
            .build();

        testMenu.open(this.player);
        server.getScheduler().performOneTick();  // Execute scheduled refresh

        // Should render empty state with default item at (0,0)
        Inventory inventory = this.player.getOpenInventory().getTopInventory();
        ItemStack emptySlot = inventory.getItem(0);

        assertThat(emptySlot).isNotNull();
        // Default empty item uses LIGHT_GRAY_STAINED_GLASS_PANE
        assertThat(emptySlot.getType()).isEqualTo(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
    }
}
