package eu.okaeri.menu.bukkit.unit.pagination;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.MenuContext;
import eu.okaeri.menu.item.MenuItem;
import eu.okaeri.menu.pagination.ItemFilter;
import eu.okaeri.menu.pane.PaginatedPane;
import eu.okaeri.menu.pane.StaticPane;
import lombok.Value;
import lombok.experimental.Accessors;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for declarative filter API (ItemFilter attached to MenuItems).
 */
class DeclarativeFilterTest {

    private ServerMock server;
    private org.bukkit.plugin.java.JavaPlugin plugin;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.plugin = MockBukkit.createMockPlugin();
        this.player = this.server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // Test data
    @Value
    @Accessors(fluent = true)
    static class ShopItem {
        String name;
        int price;
        String category;
    }

    @Test
    @DisplayName("Should build ItemFilter with required fields")
    void testItemFilterBuilder() {
        ItemFilter<ShopItem> filter = ItemFilter.<ShopItem>builder()
            .target("items")
            .predicate(item -> item.price > 100)
            .build();

        assertThat(filter.getTargetPane()).isEqualTo("items");
        assertThat(filter.isActive()).isTrue();  // Default always active
    }

    @Test
    @DisplayName("Should throw when target pane is missing")
    void testItemFilterMissingTarget() {
        assertThatThrownBy(() -> ItemFilter.<ShopItem>builder()
            .predicate(item -> true)
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Target pane name is required");
    }

    @Test
    @DisplayName("Should throw when predicate is missing")
    void testItemFilterMissingPredicate() {
        assertThatThrownBy(() -> ItemFilter.<ShopItem>builder()
            .target("items")
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Predicate is required");
    }

    @Test
    @DisplayName("Should support conditional activation")
    void testItemFilterConditionalActivation() {
        boolean[] filterActive = {false};

        ItemFilter<ShopItem> filter = ItemFilter.<ShopItem>builder()
            .target("items")
            .when(() -> filterActive[0])
            .predicate(item -> item.price > 100)
            .build();

        assertThat(filter.isActive()).isFalse();

        filterActive[0] = true;
        assertThat(filter.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should test items with predicate")
    void testItemFilterPredicate() {
        ItemFilter<ShopItem> filter = ItemFilter.<ShopItem>builder()
            .target("items")
            .predicate(item -> "weapon".equals(item.category))
            .build();

        ShopItem weapon = new ShopItem("Sword", 100, "weapon");
        ShopItem armor = new ShopItem("Helmet", 50, "armor");

        assertThat(filter.test(weapon)).isTrue();
        assertThat(filter.test(armor)).isFalse();
    }

    @Test
    @DisplayName("Should attach filter to MenuItem")
    void testAttachFilterToMenuItem() {
        ItemFilter<ShopItem> filter = ItemFilter.<ShopItem>builder()
            .target("items")
            .predicate(item -> item.price > 100)
            .build();

        MenuItem menuItem = MenuItem.item()
            .material(Material.DIAMOND)
            .name("Expensive Only")
            .filter(filter)
            .build();

        assertThat(menuItem.getFilters()).hasSize(1);
        assertThat(menuItem.getFilters().get(0)).isSameAs(filter);
    }

    @Test
    @DisplayName("Should support multiple filters on one MenuItem")
    void testMultipleFiltersOnMenuItem() {
        ItemFilter<ShopItem> filter1 = ItemFilter.<ShopItem>builder()
            .target("items")
            .id("expensive")
            .predicate(item -> item.price > 100)
            .build();

        ItemFilter<ShopItem> filter2 = ItemFilter.<ShopItem>builder()
            .target("items")
            .id("weapon")
            .predicate(item -> "weapon".equals(item.category))
            .build();

        MenuItem menuItem = MenuItem.item()
            .material(Material.DIAMOND)
            .filter(filter1)
            .filter(filter2)
            .build();

        assertThat(menuItem.getFilters()).hasSize(2);
    }

    @Test
    @DisplayName("Should apply active filter to paginated pane")
    void testApplyActiveFilterToPaginatedPane() {
        boolean[] expensiveOnly = {true};

        // Create filter button
        MenuItem filterButton = MenuItem.item()
            .material(Material.DIAMOND)
            .name("Expensive Only")
            .filter(ItemFilter.<ShopItem>builder()
                .target("shop")
                .when(() -> expensiveOnly[0])
                .predicate(item -> item.price >= 100)
                .build())
            .build();

        // Create shop data
        List<ShopItem> items = Arrays.asList(
            new ShopItem("Diamond", 150, "gem"),
            new ShopItem("Iron", 50, "metal"),
            new ShopItem("Gold", 100, "metal")
        );

        // Create menu with filter button and paginated shop
        Menu menu = Menu.builder(this.plugin)
            .title("Test Shop")
            .pane("filters", StaticPane.staticPane()
                .name("filters")
                .bounds(0, 0, 9, 1)
                .item(0, 0, filterButton)
                .build())
            .pane("shop", PaginatedPane.pane(ShopItem.class)
                .name("shop")
                .bounds(0, 1, 9, 5)
                .items(items)
                .renderer((item, index) -> MenuItem.item()
                    .material(Material.STONE)
                    .name(item.name)
                    .build())
                .build())
            .build();

        Inventory inventory = this.server.createInventory(null, 54);
        MenuContext context = new MenuContext(menu, this.player);

        // Open menu to create ViewerState
        menu.open(this.player);

        // Render menu
        menu.render(inventory, context);

        // Filter is active, should only show items >= 100
        // Diamond (150) and Gold (100) should be rendered
        // Iron (50) should not be rendered
        assertThat(inventory.getItem(9)).isNotNull();  // First item in shop pane
        assertThat(inventory.getItem(10)).isNotNull(); // Second item
        assertThat(inventory.getItem(11)).isNull();    // Third slot empty (only 2 items pass filter)
    }

    @Test
    @DisplayName("Should not apply inactive filter to paginated pane")
    void testInactiveFilterNotApplied() {
        boolean[] expensiveOnly = {false};  // INACTIVE

        MenuItem filterButton = MenuItem.item()
            .material(Material.COAL)
            .name("Expensive Only (OFF)")
            .filter(ItemFilter.<ShopItem>builder()
                .target("shop")
                .when(() -> expensiveOnly[0])
                .predicate(item -> item.price >= 100)
                .build())
            .build();

        List<ShopItem> items = Arrays.asList(
            new ShopItem("Diamond", 150, "gem"),
            new ShopItem("Iron", 50, "metal"),
            new ShopItem("Gold", 100, "metal")
        );

        Menu menu = Menu.builder(this.plugin)
            .title("Test Shop")
            .pane("filters", StaticPane.staticPane()
                .name("filters")
                .bounds(0, 0, 9, 1)
                .item(0, 0, filterButton)
                .build())
            .pane("shop", PaginatedPane.pane(ShopItem.class)
                .name("shop")
                .bounds(0, 1, 9, 5)
                .items(items)
                .renderer((item, index) -> MenuItem.item()
                    .material(Material.STONE)
                    .name(item.name)
                    .build())
                .build())
            .build();

        Inventory inventory = this.server.createInventory(null, 54);
        MenuContext context = new MenuContext(menu, this.player);

        // Open menu to create ViewerState
        menu.open(this.player);

        menu.render(inventory, context);

        // Filter inactive, all 3 items should be rendered
        assertThat(inventory.getItem(9)).isNotNull();
        assertThat(inventory.getItem(10)).isNotNull();
        assertThat(inventory.getItem(11)).isNotNull();
    }

    @Test
    @DisplayName("Should toggle filter on click")
    void testToggleFilterOnClick() {
        boolean[] weaponOnly = {false};

        MenuItem filterButton = MenuItem.item()
            .material(() -> weaponOnly[0] ? Material.DIAMOND_SWORD : Material.WOODEN_SWORD)
            .name("Weapon Filter")
            .filter(ItemFilter.<ShopItem>builder()
                .target("shop")
                .when(() -> weaponOnly[0])
                .predicate(item -> "weapon".equals(item.category))
                .build())
            .onClick(ctx -> {
                weaponOnly[0] = !weaponOnly[0];
                ctx.refresh();
            })
            .build();

        List<ShopItem> items = Arrays.asList(
            new ShopItem("Sword", 100, "weapon"),
            new ShopItem("Shield", 50, "armor"),
            new ShopItem("Bow", 75, "weapon")
        );

        Menu menu = Menu.builder(this.plugin)
            .title("Test Shop")
            .pane("filters", StaticPane.staticPane()
                .name("filters")
                .bounds(0, 0, 9, 1)
                .item(0, 0, filterButton)
                .build())
            .pane("shop", PaginatedPane.pane(ShopItem.class)
                .name("shop")
                .bounds(0, 1, 9, 5)
                .items(items)
                .renderer((item, index) -> MenuItem.item()
                    .material(Material.STONE)
                    .name(item.name)
                    .build())
                .build())
            .build();

        Inventory inventory = this.server.createInventory(null, 54);
        MenuContext context = new MenuContext(menu, this.player);

        // Open menu to create ViewerState
        menu.open(this.player);

        // Initial render - filter OFF, all items
        menu.render(inventory, context);
        assertThat(inventory.getItem(9)).isNotNull();
        assertThat(inventory.getItem(10)).isNotNull();
        assertThat(inventory.getItem(11)).isNotNull();

        // Toggle filter ON
        weaponOnly[0] = true;
        menu.render(inventory, context);

        // Only 2 weapons should be rendered
        assertThat(inventory.getItem(9)).isNotNull();
        assertThat(inventory.getItem(10)).isNotNull();
        assertThat(inventory.getItem(11)).isNull();
    }

    @Test
    @DisplayName("Should support filter IDs")
    void testFilterIds() {
        ItemFilter<ShopItem> filter = ItemFilter.<ShopItem>builder()
            .target("items")
            .id("expensive-filter")
            .predicate(item -> item.price > 100)
            .build();

        assertThat(filter.getFilterId()).isEqualTo("expensive-filter");
    }

    @Test
    @DisplayName("Should handle filters targeting different panes")
    void testFiltersDifferentTargets() {
        ItemFilter<ShopItem> shopFilter = ItemFilter.<ShopItem>builder()
            .target("shop")
            .predicate(item -> item.price > 100)
            .build();

        ItemFilter<ShopItem> auctionFilter = ItemFilter.<ShopItem>builder()
            .target("auction")
            .predicate(item -> "rare".equals(item.category))
            .build();

        MenuItem button = MenuItem.item()
            .material(Material.DIAMOND)
            .filter(shopFilter)
            .filter(auctionFilter)
            .build();

        assertThat(button.getFilters()).hasSize(2);
        assertThat(button.getFilters().get(0).getTargetPane()).isEqualTo("shop");
        assertThat(button.getFilters().get(1).getTargetPane()).isEqualTo("auction");
    }

    @Test
    @DisplayName("Should handle complex filter predicates")
    void testComplexFilterPredicates() {
        ItemFilter<ShopItem> complexFilter = ItemFilter.<ShopItem>builder()
            .target("items")
            .predicate(item ->
                (item.price >= 100) &&
                    (item.price <= 500) &&
                    ("weapon".equals(item.category) || "armor".equals(item.category))
            )
            .build();

        ShopItem match1 = new ShopItem("Sword", 150, "weapon");
        ShopItem match2 = new ShopItem("Helmet", 200, "armor");
        ShopItem noMatch1 = new ShopItem("Ring", 50, "jewelry");  // Too cheap
        ShopItem noMatch2 = new ShopItem("Diamond", 600, "gem");  // Too expensive
        ShopItem noMatch3 = new ShopItem("Bread", 150, "food");   // Wrong category

        assertThat(complexFilter.test(match1)).isTrue();
        assertThat(complexFilter.test(match2)).isTrue();
        assertThat(complexFilter.test(noMatch1)).isFalse();
        assertThat(complexFilter.test(noMatch2)).isFalse();
        assertThat(complexFilter.test(noMatch3)).isFalse();
    }

    @Test
    @DisplayName("Should collect filters from multiple static panes")
    void testFiltersFromMultiplePanes() {
        boolean[] weaponFilter = {true};
        boolean[] expensiveFilter = {true};

        Menu menu = Menu.builder(this.plugin)
            .title("Multi-Filter Shop")
            .pane("categoryFilters", StaticPane.staticPane()
                .name("categoryFilters")
                .bounds(0, 0, 5, 1)
                .item(0, 0, MenuItem.item()
                    .material(Material.IRON_SWORD)
                    .filter(ItemFilter.<ShopItem>builder()
                        .target("shop")
                        .when(() -> weaponFilter[0])
                        .predicate(item -> "weapon".equals(item.category))
                        .build())
                    .build())
                .build())
            .pane("priceFilters", StaticPane.staticPane()
                .name("priceFilters")
                .bounds(5, 0, 4, 1)
                .item(0, 0, MenuItem.item()
                    .material(Material.GOLD_INGOT)
                    .filter(ItemFilter.<ShopItem>builder()
                        .target("shop")
                        .when(() -> expensiveFilter[0])
                        .predicate(item -> item.price >= 100)
                        .build())
                    .build())
                .build())
            .pane("shop", PaginatedPane.pane(ShopItem.class)
                .name("shop")
                .bounds(0, 1, 9, 5)
                .items(Arrays.asList(
                    new ShopItem("Expensive Sword", 150, "weapon"),
                    new ShopItem("Cheap Sword", 50, "weapon"),
                    new ShopItem("Expensive Helmet", 120, "armor")
                ))
                .renderer((item, index) -> MenuItem.item()
                    .material(Material.STONE)
                    .build())
                .build())
            .build();

        Inventory inventory = this.server.createInventory(null, 54);
        MenuContext context = new MenuContext(menu, this.player);

        // Open menu to create ViewerState
        menu.open(this.player);

        menu.render(inventory, context);

        // Both filters active (AND logic): weapon AND expensive
        // Only "Expensive Sword" should pass
        assertThat(inventory.getItem(9)).isNotNull();
        assertThat(inventory.getItem(10)).isNull();
    }
}
