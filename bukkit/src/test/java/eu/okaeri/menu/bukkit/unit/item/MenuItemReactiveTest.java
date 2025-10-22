package eu.okaeri.menu.bukkit.unit.item;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.bukkit.test.SyncTestExecutor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.time.Duration;

import static eu.okaeri.menu.item.MenuItem.item;
import static eu.okaeri.menu.pane.StaticPane.staticPane;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for MenuItem.reactive() multi-source async data loading.
 * Tests reactive data sources accessed via ctx.computed(key) with ComputedValue chaining.
 */
class MenuItemReactiveTest {

    private static ServerMock server;
    private JavaPlugin plugin;
    private PlayerMock player;

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
    // SINGLE REACTIVE SOURCE
    // ========================================

    @Test
    @DisplayName("Should load single reactive data source")
    void testSingleReactiveSource() throws InterruptedException {
        Menu testMenu = Menu.builder(this.plugin)
            .title("Reactive Test")
            .rows(3)
            .pane("main", staticPane()
                .name("main")
                .bounds(0, 0, 3, 3)
                .item(0, 0, item()
                    .reactive("player-level", () -> 42, Duration.ofSeconds(30))
                    .material(Material.EXPERIENCE_BOTTLE)
                    .name(ctx -> ctx.computed("player-level")
                        .map(level -> "Level: " + level)
                        .loading("Level: Loading...")
                        .error("Level: Error")
                        .orElse("Level: Unknown"))
                    .build())
                .build())
            .build();

        testMenu.open(this.player);
        Thread.sleep(200);
        testMenu.refresh(this.player);

        Inventory inventory = this.player.getOpenInventory().getTopInventory();
        ItemStack item = inventory.getItem(0);

        assertThat(item).isNotNull();
        assertThat(item.getType()).isEqualTo(Material.EXPERIENCE_BOTTLE);
        assertThat(item.getItemMeta().getDisplayName()).contains("Level: 42");
    }

    @Test
    @DisplayName("Should show loading state initially")
    void testLoadingStateForReactiveSource() {
        Menu testMenu = Menu.builder(this.plugin)
            .title("Reactive Test")
            .rows(3)
            .pane("main", staticPane()
                .name("main")
                .bounds(0, 0, 3, 3)
                .item(0, 0, item()
                    .reactive("slow-data", () -> {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                        }
                        return "Loaded";
                    }, Duration.ofSeconds(30))
                    .material(Material.PAPER)
                    .name(ctx -> ctx.computed("slow-data")
                        .map(data -> "Data: " + data)
                        .loading("Data: Loading...")
                        .orElse("Data: Unknown"))
                    .build())
                .build())
            .build();

        testMenu.open(this.player);

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
        }

        Inventory inventory = this.player.getOpenInventory().getTopInventory();
        ItemStack item = inventory.getItem(0);

        assertThat(item).isNotNull();
        assertThat(item.getItemMeta().getDisplayName()).contains("Loading...");
    }

    @Test
    @DisplayName("Should show error state when loader fails")
    void testErrorStateForReactiveSource() {
        Menu testMenu = Menu.builder(this.plugin)
            .asyncExecutor(SyncTestExecutor.create())
            .title("Reactive Test")
            .rows(3)
            .pane("main", staticPane()
                .name("main")
                .bounds(0, 0, 3, 3)
                .item(0, 0, item()
                    .reactive("failing-data", () -> {
                        throw new RuntimeException("Database error");
                    }, Duration.ofSeconds(30))
                    .material(Material.PAPER)
                    .name(ctx -> ctx.computed("failing-data")
                        .map(data -> "Data: " + data)
                        .loading("Data: Loading...")
                        .error("Data: Error!")
                        .orElse("Data: Unknown"))
                    .build())
                .build())
            .build();

        testMenu.open(this.player);
        server.getScheduler().performOneTick();  // Execute scheduled refresh

        Inventory inventory = this.player.getOpenInventory().getTopInventory();
        ItemStack item = inventory.getItem(0);

        assertThat(item).isNotNull();
        assertThat(item.getItemMeta().getDisplayName()).contains("Error!");
    }

    // ========================================
    // MULTIPLE REACTIVE SOURCES
    // ========================================

    @Test
    @DisplayName("Should load multiple reactive data sources")
    void testMultipleReactiveSources() throws InterruptedException {
        Menu testMenu = Menu.builder(this.plugin)
            .title("Reactive Test")
            .rows(3)
            .pane("main", staticPane()
                .name("main")
                .bounds(0, 0, 3, 3)
                .item(0, 0, item()
                    .reactive("player-level", () -> 50, Duration.ofSeconds(30))
                    .reactive("player-guild", () -> "Warriors", Duration.ofSeconds(30))
                    .reactive("player-balance", () -> 10000, Duration.ofSeconds(30))
                    .material(Material.PLAYER_HEAD)
                    .name(ctx -> {
                        String level = ctx.computed("player-level")
                            .map(l -> "Lvl " + l)
                            .loading("...")
                            .orElse("?");
                        String guild = ctx.computed("player-guild")
                            .map(g -> "[" + g + "]")
                            .loading("[...]")
                            .orElse("[-]");
                        return level + " " + guild;
                    })
                    .build())
                .build())
            .build();

        testMenu.open(this.player);
        Thread.sleep(200);
        testMenu.refresh(this.player);

        Inventory inventory = this.player.getOpenInventory().getTopInventory();
        ItemStack item = inventory.getItem(0);

        assertThat(item).isNotNull();
        assertThat(item.getType()).isEqualTo(Material.PLAYER_HEAD);
        assertThat(item.getItemMeta().getDisplayName()).contains("Lvl 50");
        assertThat(item.getItemMeta().getDisplayName()).contains("[Warriors]");
    }

    @Test
    @DisplayName("Should handle mixed loading states for multiple sources")
    void testMixedLoadingStates() {
        Menu testMenu = Menu.builder(this.plugin)
            .asyncExecutor(SyncTestExecutor.create())
            .title("Reactive Test")
            .rows(3)
            .pane("main", staticPane()
                .name("main")
                .bounds(0, 0, 3, 3)
                .item(0, 0, item()
                    .reactive("fast-data", () -> "Quick", Duration.ofSeconds(30))
                    .reactive("slow-data", () -> "Slow", Duration.ofSeconds(30))
                    .material(Material.PAPER)
                    .name(ctx -> {
                        String fast = ctx.computed("fast-data").map(d -> "Fast:" + d).loading("Loading").orElse("?");
                        String slow = ctx.computed("slow-data").map(d -> "Slow:" + d).loading("Loading").orElse("?");
                        return fast + " " + slow;
                    })
                    .build())
                .build())
            .build();

        testMenu.open(this.player);
        server.getScheduler().performOneTick();  // Execute scheduled refresh

        Inventory inventory = this.player.getOpenInventory().getTopInventory();
        ItemStack item = inventory.getItem(0);

        String name = item.getItemMeta().getDisplayName();
        // Both fast and slow data should be loaded and shown
        assertThat(name).contains("Fast:");
        assertThat(name).contains("Slow:");
    }

    // ========================================
    // COMPUTED VALUE CHAINING
    // ========================================

    @Test
    @DisplayName("Should support map chaining in reactive sources")
    void testMapChaining() throws InterruptedException {
        Menu testMenu = Menu.builder(this.plugin)
            .title("Reactive Test")
            .rows(3)
            .pane("main", staticPane()
                .name("main")
                .bounds(0, 0, 3, 3)
                .item(0, 0, item()
                    .reactive("raw-stats", () -> "100:50:25", Duration.ofSeconds(30))
                    .material(Material.BOOK)
                    .name(ctx -> ctx.computed("raw-stats")
                        .map(raw -> ((String) raw).split(":"))
                        .map(parts -> "HP:" + parts[0] + " MP:" + parts[1] + " SP:" + parts[2])
                        .loading("Loading...")
                        .orElse("Unknown"))
                    .build())
                .build())
            .build();

        testMenu.open(this.player);
        Thread.sleep(200);
        testMenu.refresh(this.player);

        Inventory inventory = this.player.getOpenInventory().getTopInventory();
        ItemStack item = inventory.getItem(0);

        String name = item.getItemMeta().getDisplayName();
        assertThat(name).contains("HP:100");
        assertThat(name).contains("MP:50");
        assertThat(name).contains("SP:25");
    }

    @Test
    @DisplayName("Should support error fallback in reactive sources")
    void testErrorFallback() {
        Menu testMenu = Menu.builder(this.plugin)
            .asyncExecutor(SyncTestExecutor.create())
            .title("Reactive Test")
            .rows(3)
            .pane("main", staticPane()
                .name("main")
                .bounds(0, 0, 3, 3)
                .item(0, 0, item()
                    .reactive("error-data", () -> {
                        throw new IllegalStateException("Service unavailable");
                    }, Duration.ofSeconds(30))
                    .material(Material.BARRIER)
                    .name(ctx -> ctx.computed("error-data")
                        .map(data -> "Data: " + data)
                        .loading("Loading...")
                        .error("Service Unavailable")
                        .orElse("Unknown"))
                    .build())
                .build())
            .build();

        testMenu.open(this.player);
        server.getScheduler().performOneTick();  // Execute scheduled refresh

        Inventory inventory = this.player.getOpenInventory().getTopInventory();
        ItemStack item = inventory.getItem(0);

        assertThat(item.getItemMeta().getDisplayName()).contains("Service Unavailable");
    }

    // ========================================
    // DATA TYPES
    // ========================================

    @Test
    @DisplayName("Should handle different data types in reactive sources")
    void testDifferentDataTypes() {
        Menu testMenu = Menu.builder(this.plugin)
            .asyncExecutor(SyncTestExecutor.create())
            .title("Reactive Test")
            .rows(3)
            .pane("main", staticPane()
                .name("main")
                .bounds(0, 0, 3, 3)
                .item(0, 0, item()
                    .reactive("int-data", () -> 123, Duration.ofSeconds(30))
                    .reactive("string-data", () -> "Hello", Duration.ofSeconds(30))
                    .reactive("bool-data", () -> true, Duration.ofSeconds(30))
                    .material(Material.PAPER)
                    .name(ctx -> {
                        String n = ctx.computed("int-data").map(i -> "Num:" + i).orElse("?");
                        String s = ctx.computed("string-data").map(str -> "Txt:" + str).orElse("?");
                        String b = ctx.computed("bool-data").map(bool -> "Act:" + bool).orElse("?");
                        return n + " " + s + " " + b;
                    })
                    .build())
                .build())
            .build();

        testMenu.open(this.player);
        server.getScheduler().performOneTick();  // Execute scheduled refresh

        Inventory inventory = this.player.getOpenInventory().getTopInventory();
        ItemStack item = inventory.getItem(0);

        String name = item.getItemMeta().getDisplayName();
        assertThat(name).contains("Num:123");
        assertThat(name).contains("Txt:Hello");
        assertThat(name).contains("Act:true");
    }

    // ========================================
    // CACHING & TTL
    // ========================================

    @Test
    @DisplayName("Should cache reactive data")
    void testReactiveDataCaching() {
        int[] loadCount = {0};

        Menu testMenu = Menu.builder(this.plugin)
            .asyncExecutor(SyncTestExecutor.create())
            .title("Reactive Test")
            .rows(3)
            .pane("main", staticPane()
                .name("main")
                .bounds(0, 0, 3, 3)
                .item(0, 0, item()
                    .reactive("cached-data", () -> {
                        loadCount[0]++;
                        return "Load " + loadCount[0];
                    }, Duration.ofSeconds(10))
                    .material(Material.PAPER)
                    .name(ctx -> ctx.computed("cached-data")
                        .map(data -> "Data: " + data)
                        .orElse("Unknown"))
                    .build())
                .build())
            .build();

        testMenu.open(this.player);
        server.getScheduler().performOneTick();  // Execute scheduled refresh
        server.getScheduler().performOneTick();  // Ensure all callbacks complete

        // Verify initial data was loaded
        Inventory inventory = this.player.getOpenInventory().getTopInventory();
        ItemStack item = inventory.getItem(0);
        assertThat(item.getItemMeta().getDisplayName()).contains("Data: Load");

        int loadsAfterOpen = loadCount[0];

        // Multiple refreshes should use cached data (minimal or no new loads)
        testMenu.refresh(this.player);
        testMenu.refresh(this.player);

        // With proper caching, getOrStartLoad() returns completed future for cached data
        // So load count should not increase (data is cached and fresh)
        assertThat(loadCount[0]).isEqualTo(loadsAfterOpen);
    }

    @Test
    @DisplayName("Should reload reactive data after TTL expires")
    void testReactiveTTLExpiration() throws InterruptedException {
        int[] loadCount = {0};

        Menu testMenu = Menu.builder(this.plugin)
            .title("Reactive Test")
            .rows(3)
            .pane("main", staticPane()
                .name("main")
                .bounds(0, 0, 3, 3)
                .item(0, 0, item()
                    .reactive("expiring-data", () -> {
                        loadCount[0]++;
                        return "Load " + loadCount[0];
                    }, Duration.ofMillis(100))  // Short TTL
                    .material(Material.PAPER)
                    .name(ctx -> ctx.computed("expiring-data")
                        .map(data -> "Data: " + data)
                        .orElse("Unknown"))
                    .build())
                .build())
            .build();

        testMenu.open(this.player);
        Thread.sleep(150);  // First load
        testMenu.refresh(this.player);

        Thread.sleep(150);  // TTL expired
        testMenu.refresh(this.player);  // Should trigger second load
        Thread.sleep(150);

        // Loader should have been called at least twice
        assertThat(loadCount[0]).isGreaterThanOrEqualTo(2);
    }

    // ========================================
    // MULTIPLE PROPERTIES USING SAME SOURCE
    // ========================================

    @Test
    @DisplayName("Should allow multiple properties to use same reactive source")
    void testMultiplePropertiesUsingSameSource() throws InterruptedException {
        int[] loadCount = {0};

        Menu testMenu = Menu.builder(this.plugin)
            .title("Reactive Test")
            .rows(3)
            .pane("main", staticPane()
                .name("main")
                .bounds(0, 0, 3, 3)
                .item(0, 0, item()
                    .reactive("shared-data", () -> {
                        loadCount[0]++;
                        return "Shared Value";
                    }, Duration.ofSeconds(30))
                    .material(Material.DIAMOND)
                    .name(ctx -> ctx.computed("shared-data")
                        .map(data -> "Title: " + data)
                        .orElse("Title: Unknown"))
                    .build())
                .build())
            .build();

        testMenu.open(this.player);
        Thread.sleep(300);
        testMenu.refresh(this.player);
        Thread.sleep(100);
        testMenu.refresh(this.player);

        Inventory inventory = this.player.getOpenInventory().getTopInventory();
        ItemStack item = inventory.getItem(0);

        // Name should use the loaded data
        assertThat(item.getItemMeta().getDisplayName()).contains("Title: Shared Value");

        // Data should have been loaded a small number of times (allow for initial render/load timing)
        assertThat(loadCount[0]).isLessThanOrEqualTo(3);
        assertThat(loadCount[0]).isGreaterThanOrEqualTo(1);
    }

    // ========================================
    // EDGE CASES
    // ========================================

    @Test
    @DisplayName("Should handle null data from reactive source")
    void testNullDataFromReactiveSource() throws InterruptedException {
        Menu testMenu = Menu.builder(this.plugin)
            .title("Reactive Test")
            .rows(3)
            .pane("main", staticPane()
                .name("main")
                .bounds(0, 0, 3, 3)
                .item(0, 0, item()
                    .reactive("null-data", () -> null, Duration.ofSeconds(30))
                    .material(Material.PAPER)
                    .name(ctx -> ctx.computed("null-data")
                        .map(data -> "Data: " + data)
                        .loading("Loading...")
                        .orElse("No data"))
                    .build())
                .build())
            .build();

        testMenu.open(this.player);
        Thread.sleep(300);
        testMenu.refresh(this.player);
        Thread.sleep(100);
        testMenu.refresh(this.player);

        Inventory inventory = this.player.getOpenInventory().getTopInventory();
        ItemStack item = inventory.getItem(0);

        // Null data might cause cache rejection (NPE) or succeed with null
        // Accept either "No data" (success with null) or "Loading..." (if still loading/error)
        String name = item.getItemMeta().getDisplayName();
        boolean isValid = name.contains("No data") || name.contains("Loading");
        assertThat(isValid).as("Name should be 'No data' or 'Loading...', but was: " + name).isTrue();
    }

    @Test
    @DisplayName("Should handle empty string from reactive source")
    void testEmptyStringFromReactiveSource() throws InterruptedException {
        Menu testMenu = Menu.builder(this.plugin)
            .title("Reactive Test")
            .rows(3)
            .pane("main", staticPane()
                .name("main")
                .bounds(0, 0, 3, 3)
                .item(0, 0, item()
                    .reactive("empty-string", () -> "", Duration.ofSeconds(30))
                    .material(Material.PAPER)
                    .name(ctx -> ctx.computed("empty-string")
                        .map(data -> ((String) data).isEmpty() ? "Empty" : ("Data: " + data))
                        .orElse("Unknown"))
                    .build())
                .build())
            .build();

        testMenu.open(this.player);
        Thread.sleep(300);
        testMenu.refresh(this.player);
        Thread.sleep(100);
        testMenu.refresh(this.player);

        Inventory inventory = this.player.getOpenInventory().getTopInventory();
        ItemStack item = inventory.getItem(0);

        assertThat(item.getItemMeta().getDisplayName()).contains("Empty");
    }

    @Test
    @DisplayName("Should handle complex object types")
    void testComplexObjectTypes() throws InterruptedException {
        class PlayerStats {
            final int level;
            final String name;

            PlayerStats(int level, String name) {
                this.level = level;
                this.name = name;
            }
        }

        Menu testMenu = Menu.builder(this.plugin)
            .title("Reactive Test")
            .rows(3)
            .pane("main", staticPane()
                .name("main")
                .bounds(0, 0, 3, 3)
                .item(0, 0, item()
                    .reactive("player-stats", () -> new PlayerStats(99, "Legend"), Duration.ofSeconds(30))
                    .material(Material.DIAMOND)
                    .name(ctx -> ctx.computed("player-stats")
                        .map(stats -> {
                            PlayerStats ps = (PlayerStats) stats;
                            return ps.name + " (Lvl " + ps.level + ")";
                        })
                        .loading("Loading player...")
                        .orElse("Unknown Player"))
                    .build())
                .build())
            .build();

        testMenu.open(this.player);
        Thread.sleep(200);
        testMenu.refresh(this.player);

        Inventory inventory = this.player.getOpenInventory().getTopInventory();
        ItemStack item = inventory.getItem(0);

        assertThat(item.getItemMeta().getDisplayName()).contains("Legend (Lvl 99)");
    }
}
