package eu.okaeri.menu.bukkit.unit.pane;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.MenuContext;
import eu.okaeri.menu.bukkit.test.PooledAsyncExecutor;
import eu.okaeri.menu.bukkit.test.SyncTestExecutor;
import eu.okaeri.menu.item.MenuItem;
import eu.okaeri.menu.pane.AsyncPaginatedPane;
import eu.okaeri.menu.pane.PaginationContext;
import eu.okaeri.menu.pane.StaticPane;
import eu.okaeri.menu.pane.pagination.ItemFilter;
import eu.okaeri.menu.pane.pagination.LoaderContext;
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
                .renderer((ctx, item, index) -> MenuItem.item().material(Material.STONE).build())
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
            .loader(ctx -> Collections.emptyList())
            .renderer((ctx, item, index) -> MenuItem.item().material(Material.STONE).build())
            .build();

        assertThat(pane.getTtl()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    @DisplayName("Should allow custom TTL")
    void testCustomTTL() {
        AsyncPaginatedPane<String> pane = paneAsync(String.class)
            .name("test-pane")
            .bounds(0, 0, 3, 3)
            .loader(ctx -> Collections.emptyList())
            .ttl(Duration.ofMinutes(5))
            .renderer((ctx, item, index) -> MenuItem.item().material(Material.STONE).build())
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
            .pane(paneAsync(String.class)
                .name("async-pane")
                .bounds(0, 0, 3, 2)  // 3x2 = 6 slots
                .loader(ctx -> testData)
                .renderer((ctx, item, index) -> MenuItem.item()
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
            .pane(paneAsync(String.class)
                .name("async-pane")
                .bounds(0, 0, 3, 3)  // 3x3 = 9 slots
                .loader(ctx -> Collections.emptyList())
                .renderer((ctx, item, index) -> MenuItem.item()
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
            .pane(paneAsync(String.class)
                .name("async-pane")
                .bounds(0, 0, 3, 2)  // 3x2 = 6 slots
                .loader(ctx -> testData)
                .renderer((ctx, item, index) -> MenuItem.item()
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
            .pane(paneAsync(String.class)
                .name("async-pane")
                .bounds(0, 0, 3, 2)
                .loader(ctx -> {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                    }
                    return Arrays.asList("A", "B");
                })
                .renderer((ctx, item, index) -> MenuItem.item()
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

    @Test
    @DisplayName("Should clear slots for invisible static items after data loads")
    void testInvisibleStaticItemsClearedAfterLoad() {
        Menu testMenu = Menu.builder(this.plugin)
            .asyncExecutor(SyncTestExecutor.create())  // Sync executor for instant completion
            .title("Async Menu")
            .rows(3)
            .pane(paneAsync(String.class)
                .name("async-pane")
                .bounds(0, 0, 3, 2)
                .loader(ctx -> Arrays.asList("A", "B"))
                .renderer((ctx, item, index) -> MenuItem.item()
                    .material(Material.DIAMOND)
                    .build())
                .staticItem(0, 0, MenuItem.item()
                    .material(Material.TORCH)
                    .name("Create")
                    .visible(ctx -> false)  // Always invisible
                    .build())
                .loading(MenuItem.item()
                    .material(Material.CLOCK)
                    .name("Loading...")
                    .build())
                .build())
            .build();

        // Open triggers first render (LOADING state) and starts async load
        testMenu.open(this.player);

        // Execute the scheduled refresh task after async load completes
        server.getScheduler().performOneTick();

        Inventory inventory = this.player.getOpenInventory().getTopInventory();

        // Slot 0 should be cleared (null) because static item is invisible
        // Previously this bug would leave the loading item in the slot
        ItemStack slot0 = inventory.getItem(0);
        assertThat(slot0)
            .as("Invisible static item slot should be cleared, not showing loading item")
            .isNull();

        // Verify data items rendered correctly in other slots
        ItemStack slot1 = inventory.getItem(1);
        assertThat(slot1).isNotNull();
        assertThat(slot1.getType()).isEqualTo(Material.DIAMOND);
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
            .loader(ctx -> Arrays.asList("A", "B", "C"))
            .renderer((ctx, item, index) -> MenuItem.item().material(Material.STONE).build())
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
            .pane(paneAsync(String.class)
                .name("async-pane")
                .bounds(0, 0, 3, 2)
                .loader(ctx -> {
                    loadCount[0]++;
                    return List.of("Load " + loadCount[0]);
                })
                .ttl(Duration.ofMillis(100))  // Short TTL for faster test
                .renderer((ctx, item, index) -> MenuItem.item()
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
            .pane(paneAsync(String.class)
                .name("async-pane")
                .bounds(0, 0, 3, 2)
                .loader(ctx -> Collections.singletonList("Only Item"))
                .renderer((ctx, item, index) -> MenuItem.item()
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
            .pane(paneAsync(String.class)
                .name("async-pane")
                .bounds(0, 0, 7, 2)  // 7x2 = 14 slots
                .itemsPerPage(10)
                .loader(ctx -> largeList)
                .renderer((ctx, item, index) -> MenuItem.item()
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
            .loader(ctx -> Arrays.asList("Item 1", "Item 2"))
            .renderer((ctx, item, index) -> MenuItem.item()
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
            .loader(ctx -> Arrays.asList("Item 1", "Item 2"))
            .renderer((ctx, item, index) -> MenuItem.item()
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
            .pane(paneAsync(String.class)
                .name("async-pane")
                .bounds(0, 0, 3, 3)
                .loader(ctx -> {
                    // Simulate async delay to stay in LOADING state
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return Arrays.asList("Item 1", "Item 2");
                })
                .renderer((ctx, item, index) -> MenuItem.item()
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
            .pane(paneAsync(String.class)
                .name("async-pane")
                .bounds(0, 0, 3, 3)
                .loader(ctx -> Collections.emptyList())  // Returns empty list
                .renderer((ctx, item, index) -> MenuItem.item()
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

    // ========================================
    // LOADER CONTEXT TESTS
    // ========================================

    @Test
    @DisplayName("Loader should receive LoaderContext with pagination state")
    void testLoaderReceivesContext() {
        LoaderContext[] capturedContext = {null};

        Menu testMenu = Menu.builder(this.plugin)
            .asyncExecutor(SyncTestExecutor.create())
            .title("Async Menu")
            .rows(3)
            .pane(paneAsync(String.class)
                .name("async-pane")
                .bounds(0, 0, 3, 3)
                .itemsPerPage(5)
                .loader(ctx -> {
                    capturedContext[0] = ctx;
                    return Arrays.asList("A", "B", "C");
                })
                .renderer((ctx, item, index) -> MenuItem.item()
                    .material(Material.STONE)
                    .build())
                .build())
            .build();

        testMenu.open(this.player);
        server.getScheduler().performOneTick();

        assertThat(capturedContext[0]).isNotNull();
        assertThat(capturedContext[0].getCurrentPage()).isEqualTo(0);
        assertThat(capturedContext[0].getPageSize()).isEqualTo(5);
    }

    @Test
    @DisplayName("LoaderContext should have correct page size from pane")
    void testLoaderContextPageSize() {
        LoaderContext[] capturedContext = {null};

        Menu testMenu = Menu.builder(this.plugin)
            .asyncExecutor(SyncTestExecutor.create())
            .title("Async Menu")
            .rows(3)
            .pane(paneAsync(String.class)
                .name("async-pane")
                .bounds(0, 0, 9, 2)  // 9x2 = 18 slots
                .itemsPerPage(10)
                .loader(ctx -> {
                    capturedContext[0] = ctx;
                    return Collections.emptyList();
                })
                .renderer((ctx, item, index) -> MenuItem.item()
                    .material(Material.STONE)
                    .build())
                .build())
            .build();

        testMenu.open(this.player);
        server.getScheduler().performOneTick();

        assertThat(capturedContext[0]).isNotNull();
        assertThat(capturedContext[0].getPageSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("LoaderContext should receive correct page size with static items")
    void testLoaderContextPageSizeWithStaticItems() {
        LoaderContext[] capturedContext = {null};

        // Scenario: 9x1 pane (9 slots), 2 static items
        // Expected page size: 9 - 2 = 7
        Menu testMenu = Menu.builder(this.plugin)
            .asyncExecutor(SyncTestExecutor.create())
            .title("Async Menu")
            .rows(3)
            .pane(paneAsync(String.class)
                .name("async-pane")
                .bounds(0, 0, 9, 1)  // 9 slots
                .loader(ctx -> {
                    capturedContext[0] = ctx;
                    return Collections.emptyList();
                })
                .renderer((ctx, item, index) -> MenuItem.item()
                    .material(Material.STONE)
                    .build())
                .staticItem(0, 0, MenuItem.item().material(Material.BARRIER).build())
                .staticItem(8, 0, MenuItem.item().material(Material.BARRIER).build())
                .build())
            .build();

        testMenu.open(this.player);
        server.getScheduler().performOneTick();

        assertThat(capturedContext[0]).isNotNull();
        assertThat(capturedContext[0].getPageSize())
            .as("Page size should be pane slots - static items: 9 - 2 = 7")
            .isEqualTo(7);
    }

    @Test
    @DisplayName("Should distribute async items correctly across multiple pages with static items")
    void testAsyncMultiPagePaginationWithStaticItems() {
        // Scenario: 9 items from loader, 9x1 pane, 2 static items
        // Page 0: STATIC, 1, 2, 3, 4, 5, 6, 7, STATIC (7 data items)
        // Page 1: STATIC, 8, 9, empty..., STATIC (2 data items)
        List<Integer> allItems = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);

        Menu testMenu = Menu.builder(this.plugin)
            .asyncExecutor(SyncTestExecutor.create())
            .title("Async Multi-Page")
            .rows(3)
            .pane(paneAsync(Integer.class)
                .name("async-pane")
                .bounds(0, 0, 9, 1)
                .loader(ctx -> allItems)  // Return all 9 items
                .renderer((ctx, item, index) -> MenuItem.item()
                    .material(Material.STONE)
                    .amount(item)
                    .build())
                .staticItem(0, 0, MenuItem.item().material(Material.BARRIER).build())
                .staticItem(8, 0, MenuItem.item().material(Material.BARRIER).build())
                .build())
            .build();

        testMenu.open(this.player);
        server.getScheduler().performOneTick();  // Execute async load

        Inventory inventory = this.player.getOpenInventory().getTopInventory();
        MenuContext context = new MenuContext(testMenu, this.player);
        PaginationContext<?> pagination = context.pagination("async-pane");

        // Verify total pages calculation
        assertThat(pagination.getTotalPages())
            .as("Should have 2 pages: ceil(9 items / 7 per page)")
            .isEqualTo(2);

        // === PAGE 0 ===
        assertThat(pagination.getCurrentPage()).isEqualTo(0);

        // Verify static items
        assertThat(inventory.getItem(0).getType())
            .as("Slot 0 should have static barrier on page 0")
            .isEqualTo(Material.BARRIER);
        assertThat(inventory.getItem(8).getType())
            .as("Slot 8 should have static barrier on page 0")
            .isEqualTo(Material.BARRIER);

        // Verify page 0 has items 1-7
        assertThat(inventory.getItem(1).getAmount()).isEqualTo(1);
        assertThat(inventory.getItem(2).getAmount()).isEqualTo(2);
        assertThat(inventory.getItem(3).getAmount()).isEqualTo(3);
        assertThat(inventory.getItem(4).getAmount()).isEqualTo(4);
        assertThat(inventory.getItem(5).getAmount()).isEqualTo(5);
        assertThat(inventory.getItem(6).getAmount()).isEqualTo(6);
        assertThat(inventory.getItem(7).getAmount()).isEqualTo(7);

        // === PAGE 1 ===
        pagination.nextPage();
        testMenu.refresh(this.player);

        // Verify static items still present
        assertThat(inventory.getItem(0).getType())
            .as("Slot 0 should have static barrier on page 1")
            .isEqualTo(Material.BARRIER);
        assertThat(inventory.getItem(8).getType())
            .as("Slot 8 should have static barrier on page 1")
            .isEqualTo(Material.BARRIER);

        // Verify page 1 has items 8-9
        assertThat(inventory.getItem(1))
            .as("Slot 1 on page 1 should have item 8")
            .isNotNull()
            .extracting(ItemStack::getAmount)
            .isEqualTo(8);

        assertThat(inventory.getItem(2))
            .as("Slot 2 on page 1 should have item 9")
            .isNotNull()
            .extracting(ItemStack::getAmount)
            .isEqualTo(9);

        // Verify remaining slots are cleared
        assertThat(inventory.getItem(3))
            .as("Slot 3 on page 1 should be empty (no more items)")
            .isNull();

        // CRITICAL: Verify all 9 items are accessible across both pages
        pagination.setPage(0);
        List<?> page0Items = pagination.getCurrentPageItems();
        pagination.setPage(1);
        List<?> page1Items = pagination.getCurrentPageItems();

        assertThat(page0Items.size()).isEqualTo(7);
        assertThat(page1Items.size()).isEqualTo(2);
        assertThat(page0Items.size() + page1Items.size())
            .as("All 9 items should be accessible across both pages")
            .isEqualTo(9);
    }

    @Test
    @DisplayName("LoaderContext should contain filter values from ItemFilters")
    void testLoaderContextReceivesFilterValues() {
        LoaderContext[] capturedContext = {null};
        String[] currentCategory = {"WEAPONS"};
        Integer[] minPrice = {100};

        Menu testMenu = Menu.builder(this.plugin)
            .asyncExecutor(SyncTestExecutor.create())
            .title("Async Menu")
            .rows(3)
            .pane(StaticPane.staticPane()
                .name("filters")
                .bounds(0, 0, 9, 1)
                .item(0, 0, MenuItem.item()
                    .material(Material.IRON_SWORD)
                    .name("Category Filter")
                    .filter(ItemFilter.builder()
                        .target("items")
                        .id("category")
                        .value(() -> currentCategory[0])
                        .build())
                    .build())
                .item(1, 0, MenuItem.item()
                    .material(Material.GOLD_INGOT)
                    .name("Price Filter")
                    .filter(ItemFilter.builder()
                        .target("items")
                        .id("minPrice")
                        .value(() -> minPrice[0])
                        .build())
                    .build())
                .build())
            .pane(paneAsync(String.class)
                .name("items")
                .bounds(0, 1, 9, 2)
                .loader(ctx -> {
                    capturedContext[0] = ctx;
                    return Collections.emptyList();
                })
                .renderer((ctx, item, index) -> MenuItem.item()
                    .material(Material.STONE)
                    .build())
                .build())
            .build();

        testMenu.open(this.player);
        server.getScheduler().performOneTick();

        assertThat(capturedContext[0]).isNotNull();
        assertThat(capturedContext[0].getActiveFilterCount()).isEqualTo(2);
        assertThat(capturedContext[0].getFilter("category", String.class))
            .isPresent()
            .contains("WEAPONS");
        assertThat(capturedContext[0].getFilter("minPrice", Integer.class))
            .isPresent()
            .contains(100);
    }

    @Test
    @DisplayName("LoaderContext should update when filter values change")
    void testLoaderContextUpdatesWithFilterChanges() {
        LoaderContext[] capturedContext = {null};
        String[] currentCategory = {"WEAPONS"};

        Menu testMenu = Menu.builder(this.plugin)
            .asyncExecutor(SyncTestExecutor.create())
            .title("Async Menu")
            .rows(3)
            .pane(StaticPane.staticPane()
                .name("filters")
                .bounds(0, 0, 9, 1)
                .item(0, 0, MenuItem.item()
                    .material(Material.IRON_SWORD)
                    .filter(ItemFilter.builder()
                        .target("items")
                        .id("category")
                        .value(() -> currentCategory[0])
                        .build())
                    .build())
                .build())
            .pane(paneAsync(String.class)
                .name("items")
                .bounds(0, 1, 9, 2)
                .ttl(Duration.ofMillis(1))  // Very short TTL for refresh
                .loader(ctx -> {
                    capturedContext[0] = ctx;
                    return Collections.emptyList();
                })
                .renderer((ctx, item, index) -> MenuItem.item()
                    .material(Material.STONE)
                    .build())
                .build())
            .build();

        testMenu.open(this.player);
        server.getScheduler().performOneTick();

        assertThat(capturedContext[0].getFilter("category", String.class))
            .isPresent()
            .contains("WEAPONS");

        // Change filter value
        currentCategory[0] = "ARMOR";

        // Trigger refresh after TTL expires
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        testMenu.refresh(this.player);
        server.getScheduler().performOneTick();

        assertThat(capturedContext[0].getFilter("category", String.class))
            .isPresent()
            .contains("ARMOR");
    }

    @Test
    @DisplayName("LoaderContext should be empty when no filters are active")
    void testLoaderContextEmptyWithoutFilters() {
        LoaderContext[] capturedContext = {null};

        Menu testMenu = Menu.builder(this.plugin)
            .asyncExecutor(SyncTestExecutor.create())
            .title("Async Menu")
            .rows(3)
            .pane(paneAsync(String.class)
                .name("items")
                .bounds(0, 0, 9, 3)
                .loader(ctx -> {
                    capturedContext[0] = ctx;
                    return Arrays.asList("A", "B", "C");
                })
                .renderer((ctx, item, index) -> MenuItem.item()
                    .material(Material.STONE)
                    .build())
                .build())
            .build();

        testMenu.open(this.player);
        server.getScheduler().performOneTick();

        assertThat(capturedContext[0]).isNotNull();
        assertThat(capturedContext[0].getActiveFilterCount()).isEqualTo(0);
        assertThat(capturedContext[0].getActiveFilterIds()).isEmpty();
    }

    @Test
    @DisplayName("LoaderContext should receive correct page number with active filters")
    void testLoaderContextWithPaginationAndFilters() {
        LoaderContext[] capturedContext = {null};
        String[] category = {"WEAPONS"};

        Menu testMenu = Menu.builder(this.plugin)
            .asyncExecutor(SyncTestExecutor.create())
            .title("Async Menu")
            .rows(3)
            .pane(StaticPane.staticPane()
                .name("filters")
                .bounds(0, 0, 9, 1)
                .item(0, 0, MenuItem.item()
                    .material(Material.IRON_SWORD)
                    .filter(ItemFilter.builder()
                        .target("items")
                        .id("category")
                        .value(() -> category[0])
                        .build())
                    .build())
                .build())
            .pane(paneAsync(String.class)
                .name("items")
                .bounds(0, 1, 9, 2)
                .itemsPerPage(5)
                .loader(ctx -> {
                    capturedContext[0] = ctx;
                    return Collections.emptyList();
                })
                .renderer((ctx, item, index) -> MenuItem.item()
                    .material(Material.STONE)
                    .build())
                .build())
            .build();

        testMenu.open(this.player);
        server.getScheduler().performOneTick();

        // Initially on page 0 with filter
        assertThat(capturedContext[0]).isNotNull();
        assertThat(capturedContext[0].getCurrentPage()).isEqualTo(0);
        assertThat(capturedContext[0].getFilter("category", String.class))
            .isPresent()
            .contains("WEAPONS");

        // TODO: Once pagination navigation is wired to AsyncPaginatedPane,
        // test that navigating to page 2 results in LoaderContext.currentPage=2
        // For now, this test verifies page 0 + filters work together
    }

    @Test
    @DisplayName("Inactive filters should not be included in LoaderContext")
    void testInactiveFilterNotIncludedInLoaderContext() {
        LoaderContext[] capturedContext = {null};
        String[] category = {"WEAPONS"};

        Menu testMenu = Menu.builder(this.plugin)
            .asyncExecutor(SyncTestExecutor.create())
            .title("Async Menu")
            .rows(3)
            .pane(StaticPane.staticPane()
                .name("filters")
                .bounds(0, 0, 9, 1)
                .item(0, 0, MenuItem.item()
                    .material(Material.IRON_SWORD)
                    .filter(ItemFilter.builder()
                        .target("items")
                        .id("category")
                        .when(ctx -> ctx.getBool("filterActive"))  // Conditional activation via state
                        .value(() -> category[0])
                        .build())
                    .build())
                .build())
            .pane(paneAsync(String.class)
                .name("items")
                .bounds(0, 1, 9, 2)
                .ttl(Duration.ofMillis(1))  // Very short TTL for refresh
                .loader(ctx -> {
                    capturedContext[0] = ctx;
                    return Collections.emptyList();
                })
                .renderer((ctx, item, index) -> MenuItem.item()
                    .material(Material.STONE)
                    .build())
                .build())
            .build();

        testMenu.open(this.player);
        server.getScheduler().performOneTick();

        // Filter is inactive, should not be in LoaderContext
        assertThat(capturedContext[0]).isNotNull();
        assertThat(capturedContext[0].getActiveFilterCount()).isEqualTo(0);
        assertThat(capturedContext[0].hasFilter("category")).isFalse();

        // Activate filter via state API
        MenuContext ctx = new MenuContext(testMenu, this.player);
        ctx.set("filterActive", true);

        // Trigger refresh after TTL expires
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        testMenu.refresh(this.player);
        server.getScheduler().performOneTick();

        // Filter is now active, should be in LoaderContext
        assertThat(capturedContext[0]).isNotNull();
        assertThat(capturedContext[0].getActiveFilterCount()).isEqualTo(1);
        assertThat(capturedContext[0].getFilter("category", String.class))
            .isPresent()
            .contains("WEAPONS");
    }

    @Test
    @DisplayName("LoaderContext should collect filter values from multiple panes")
    void testFilterValuesFromMultiplePanes() {
        LoaderContext[] capturedContext = {null};
        String[] category = {"WEAPONS"};
        Integer[] minPrice = {100};

        Menu testMenu = Menu.builder(this.plugin)
            .asyncExecutor(SyncTestExecutor.create())
            .title("Async Menu")
            .rows(4)
            // First pane with category filter
            .pane(StaticPane.staticPane()
                .name("categoryFilters")
                .bounds(0, 0, 5, 1)
                .item(0, 0, MenuItem.item()
                    .material(Material.IRON_SWORD)
                    .filter(ItemFilter.builder()
                        .target("items")
                        .id("category")
                        .value(() -> category[0])
                        .build())
                    .build())
                .build())
            // Second pane with price filter
            .pane(StaticPane.staticPane()
                .name("priceFilters")
                .bounds(5, 0, 4, 1)
                .item(0, 0, MenuItem.item()
                    .material(Material.GOLD_INGOT)
                    .filter(ItemFilter.builder()
                        .target("items")
                        .id("minPrice")
                        .value(() -> minPrice[0])
                        .build())
                    .build())
                .build())
            // Target async pane
            .pane(paneAsync(String.class)
                .name("items")
                .bounds(0, 1, 9, 3)
                .loader(ctx -> {
                    capturedContext[0] = ctx;
                    return Collections.emptyList();
                })
                .renderer((ctx, item, index) -> MenuItem.item()
                    .material(Material.STONE)
                    .build())
                .build())
            .build();

        testMenu.open(this.player);
        server.getScheduler().performOneTick();

        // Should collect filters from both panes
        assertThat(capturedContext[0]).isNotNull();
        assertThat(capturedContext[0].getActiveFilterCount()).isEqualTo(2);
        assertThat(capturedContext[0].getFilter("category", String.class))
            .isPresent()
            .contains("WEAPONS");
        assertThat(capturedContext[0].getFilter("minPrice", Integer.class))
            .isPresent()
            .contains(100);
    }

    @Test
    @DisplayName("LoaderContext should support mixed predicate and value-only filters")
    void testLoaderContextMixedFilters() {
        LoaderContext[] capturedContext = {null};
        String[] category = {"WEAPONS"};

        Menu testMenu = Menu.builder(this.plugin)
            .asyncExecutor(SyncTestExecutor.create())
            .title("Async Menu")
            .rows(3)
            .pane(StaticPane.staticPane()
                .name("filters")
                .bounds(0, 0, 9, 1)
                .item(0, 0, MenuItem.item()
                    .material(Material.IRON_SWORD)
                    .filter(ItemFilter.builder()
                        .target("items")
                        .id("category")
                        .predicate(item -> category[0].equals(item))  // Has predicate
                        .value(() -> category[0])  // Also has value
                        .build())
                    .build())
                .item(1, 0, MenuItem.item()
                    .material(Material.GOLD_INGOT)
                    .filter(ItemFilter.builder()
                        .target("items")
                        .id("seller")
                        .value(() -> "some-uuid")  // Value-only
                        .build())
                    .build())
                .build())
            .pane(paneAsync(String.class)
                .name("items")
                .bounds(0, 1, 9, 2)
                .loader(ctx -> {
                    capturedContext[0] = ctx;
                    return Collections.emptyList();
                })
                .renderer((ctx, item, index) -> MenuItem.item()
                    .material(Material.STONE)
                    .build())
                .build())
            .build();

        testMenu.open(this.player);
        server.getScheduler().performOneTick();

        assertThat(capturedContext[0]).isNotNull();
        assertThat(capturedContext[0].getActiveFilterCount()).isEqualTo(2);
        assertThat(capturedContext[0].getFilter("category", String.class))
            .isPresent()
            .contains("WEAPONS");
        assertThat(capturedContext[0].getFilter("seller", String.class))
            .isPresent()
            .contains("some-uuid");
    }

    @Test
    @DisplayName("Exception in filter value extraction should clear pane but not crash menu")
    void testFilterValueExtractionErrorHandling() {
        LoaderContext[] capturedContext = {null};

        // Create a menu with two panes:
        // 1. Pane with failing filter (should be cleared)
        // 2. Pane without filters (should render successfully)
        Menu testMenu = Menu.builder(this.plugin)
            .asyncExecutor(SyncTestExecutor.create())
            .title("Error Test Menu")
            .rows(4)
            .pane(StaticPane.staticPane()
                .name("failingFilters")
                .bounds(0, 0, 9, 1)
                .item(0, 0, MenuItem.item()
                    .material(Material.BARRIER)
                    .filter(ItemFilter.builder()
                        .target("failingPane")
                        .id("broken")
                        .value(() -> {
                            throw new RuntimeException("Filter value extraction failed!");
                        })
                        .build())
                    .build())
                .build())
            .pane(paneAsync(String.class)
                .name("failingPane")
                .bounds(0, 1, 9, 1)
                .loader(ctx -> {
                    // Should never be called because filter extraction fails first
                    return Arrays.asList("Should", "Not", "Appear");
                })
                .renderer((ctx, item, index) -> MenuItem.item()
                    .material(Material.DIAMOND)
                    .build())
                .build())
            .pane(StaticPane.staticPane()
                .name("workingPane")
                .bounds(0, 2, 9, 1)
                .item(0, 0, MenuItem.item()
                    .material(Material.EMERALD)
                    .name("Working Item")
                    .build())
                .build())
            .build();

        // Open menu - should not throw exception
        testMenu.open(this.player);
        server.getScheduler().performOneTick();

        Inventory inventory = this.player.getOpenInventory().getTopInventory();

        // Verify failingPane was cleared (slots 9-17, all should be null)
        for (int slot = 9; slot < 18; slot++) {
            assertThat(inventory.getItem(slot))
                .as("Slot %d in failingPane should be cleared after error", slot)
                .isNull();
        }

        // Verify workingPane still rendered (slot 18 should have the emerald)
        assertThat(inventory.getItem(18))
            .as("Slot 18 in workingPane should have item despite other pane failing")
            .isNotNull();
        assertThat(inventory.getItem(18).getType())
            .isEqualTo(Material.EMERALD);

        // Verify failingFilters pane still rendered (slot 0 should have barrier)
        // The StaticPane itself renders fine - it's the failingPane that fails during filter extraction
        assertThat(inventory.getItem(0))
            .as("Slot 0 in failingFilters should still be rendered (only failingPane failed)")
            .isNotNull();
        assertThat(inventory.getItem(0).getType())
            .isEqualTo(Material.BARRIER);
    }

    @Test
    @DisplayName("Async loader exception should trigger ERROR state and render error items")
    void testLoaderErrorTriggersErrorState() {
        Menu testMenu = Menu.builder(this.plugin)
            .asyncExecutor(SyncTestExecutor.create())
            .title("Loader Error Test")
            .rows(3)
            .pane(paneAsync(String.class)
                .name("failingPane")
                .bounds(0, 0, 9, 2)
                .loader(ctx -> {
                    throw new RuntimeException("Loader failed!");
                })
                .renderer((ctx, item, index) -> MenuItem.item()
                    .material(Material.DIAMOND)
                    .build())
                .error(MenuItem.item()
                    .material(Material.REDSTONE_BLOCK)
                    .name("Error!")
                    .build())
                .build())
            .pane(StaticPane.staticPane()
                .name("workingPane")
                .bounds(0, 2, 9, 1)
                .item(0, 0, MenuItem.item()
                    .material(Material.EMERALD)
                    .name("Working Item")
                    .build())
                .build())
            .build();

        // Open menu - loader will fail async
        testMenu.open(this.player);
        server.getScheduler().performOneTick();

        Inventory inventory = this.player.getOpenInventory().getTopInventory();

        // Verify failingPane filled with error items (slots 0-17)
        for (int slot = 0; slot < 18; slot++) {
            assertThat(inventory.getItem(slot))
                .as("Slot %d should have error item", slot)
                .isNotNull();
            assertThat(inventory.getItem(slot).getType())
                .as("Slot %d should be REDSTONE_BLOCK (error item)", slot)
                .isEqualTo(Material.REDSTONE_BLOCK);
        }

        // Verify workingPane still rendered (slot 18 should have the emerald)
        assertThat(inventory.getItem(18))
            .as("Slot 18 in workingPane should have item despite other pane failing")
            .isNotNull();
        assertThat(inventory.getItem(18).getType())
            .isEqualTo(Material.EMERALD);
    }

    @Test
    @DisplayName("Exception in renderer during SUCCESS state should clear pane")
    void testRendererErrorDuringSuccessState() {
        Menu testMenu = Menu.builder(this.plugin)
            .asyncExecutor(SyncTestExecutor.create())
            .title("Renderer Error Test")
            .rows(3)
            .pane(paneAsync(String.class)
                .name("failingPane")
                .bounds(0, 0, 9, 1)
                .loader(ctx -> Arrays.asList("A", "B", "C"))  // Loader succeeds
                .renderer((ctx, item, index) -> {
                    throw new RuntimeException("Renderer failed!");  // Renderer fails
                })
                .build())
            .pane(StaticPane.staticPane()
                .name("workingPane")
                .bounds(0, 1, 9, 1)
                .item(0, 0, MenuItem.item()
                    .material(Material.GOLD_INGOT)
                    .name("Working Item")
                    .build())
                .build())
            .build();

        // Open menu - loader succeeds but renderer will fail
        testMenu.open(this.player);
        server.getScheduler().performOneTick();

        Inventory inventory = this.player.getOpenInventory().getTopInventory();

        // Verify failingPane was cleared (slots 0-8, all should be null)
        for (int slot = 0; slot < 9; slot++) {
            assertThat(inventory.getItem(slot))
                .as("Slot %d in failingPane should be cleared after error", slot)
                .isNull();
        }

        // Verify workingPane still rendered (slot 9 should have the gold ingot)
        assertThat(inventory.getItem(9))
            .as("Slot 9 in workingPane should have item despite other pane failing")
            .isNotNull();
        assertThat(inventory.getItem(9).getType())
            .isEqualTo(Material.GOLD_INGOT);
    }
}
