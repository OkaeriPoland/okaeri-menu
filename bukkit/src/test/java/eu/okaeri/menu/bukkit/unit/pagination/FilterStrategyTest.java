package eu.okaeri.menu.bukkit.unit.pagination;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.MenuContext;
import eu.okaeri.menu.pane.PaginatedPane;
import eu.okaeri.menu.pane.PaginationContext;
import eu.okaeri.menu.pane.pagination.FilterStrategy;
import lombok.Value;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for FilterStrategy (AND/OR filter combination).
 */
class FilterStrategyTest {

    private ServerMock server;
    private org.bukkit.plugin.java.JavaPlugin plugin;
    private PlayerMock player;
    private Menu menu;
    private MenuContext context;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.plugin = MockBukkit.createMockPlugin();
        this.player = this.server.addPlayer();

        // Create menu and open for player to establish ViewerState
        this.menu = Menu.builder(this.plugin).rows(3).build();
        this.menu.open(this.player);

        // Create MenuContext for pagination tests
        this.context = new MenuContext(this.menu, this.player);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // Test data
    @Value
    @Accessors(fluent = true)
    static class Item {
        String name;
        int price;
        String category;
    }

    // Helper method to create pagination context
    private PaginationContext<Item> createContext(List<Item> items) {
        PaginatedPane<Item> testPane = PaginatedPane.<Item>pane()
            .name("test")
            .bounds(0, 0, 9, 1)
            .items(items)
            .itemsPerPage(10)
            .renderer((ctx, item, i) -> null)
            .build();

        return PaginationContext.get(this.context, testPane);
    }

    @Test
    @DisplayName("Default strategy should be AND")
    void testDefaultStrategy() {
        List<Item> items = List.of(
            new Item("Diamond", 100, "gem"),
            new Item("Emerald", 150, "gem")
        );

        PaginationContext<Item> ctx = this.createContext(items);

        assertThat(ctx.getFilterStrategy()).isEqualTo(FilterStrategy.AND);
    }

    @Test
    @DisplayName("AND strategy: All filters must match")
    void testAndStrategy() {
        List<Item> items = List.of(
            new Item("Diamond", 100, "gem"),
            new Item("Emerald", 150, "gem"),
            new Item("Iron Sword", 50, "weapon"),
            new Item("Gold Ring", 200, "jewelry")
        );

        PaginationContext<Item> ctx = this.createContext(items);
        ctx.setFilterStrategy(FilterStrategy.AND);

        // Add two filters
        ctx.addFilter("category", item -> "gem".equals(item.category));
        ctx.addFilter("price", item -> item.price >= 100);

        // With AND: must be BOTH gem AND price >= 100
        List<Item> filtered = ctx.getFilteredItems();

        assertThat(filtered).hasSize(2);
        assertThat(filtered).extracting(Item::name)
            .containsExactlyInAnyOrder("Diamond", "Emerald");
    }

    @Test
    @DisplayName("OR strategy: Any filter must match")
    void testOrStrategy() {
        List<Item> items = List.of(
            new Item("Diamond", 100, "gem"),          // Matches both
            new Item("Emerald", 150, "gem"),          // Matches category only
            new Item("Iron Sword", 200, "weapon"),    // Matches price only
            new Item("Copper Coin", 5, "currency")    // Matches neither
        );

        PaginationContext<Item> ctx = this.createContext(items);
        ctx.setFilterStrategy(FilterStrategy.OR);

        // Add two filters
        ctx.addFilter("category", item -> "gem".equals(item.category));
        ctx.addFilter("price", item -> item.price >= 200);

        // With OR: must match EITHER gem OR price >= 200
        List<Item> filtered = ctx.getFilteredItems();

        assertThat(filtered).hasSize(3);
        assertThat(filtered).extracting(Item::name)
            .containsExactlyInAnyOrder("Diamond", "Emerald", "Iron Sword");
        assertThat(filtered).extracting(Item::name)
            .doesNotContain("Copper Coin");
    }

    @Test
    @DisplayName("AND strategy with single filter should pass items that match")
    void testAndStrategyWithSingleFilter() {
        List<Item> items = List.of(
            new Item("Diamond", 100, "gem"),
            new Item("Emerald", 150, "gem"),
            new Item("Iron Sword", 50, "weapon")
        );

        PaginationContext<Item> ctx = this.createContext(items);
        ctx.setFilterStrategy(FilterStrategy.AND);

        ctx.addFilter("category", item -> "gem".equals(item.category));

        List<Item> filtered = ctx.getFilteredItems();

        assertThat(filtered).hasSize(2);
        assertThat(filtered).extracting(Item::name)
            .containsExactlyInAnyOrder("Diamond", "Emerald");
    }

    @Test
    @DisplayName("OR strategy with single filter should pass items that match")
    void testOrStrategyWithSingleFilter() {
        List<Item> items = List.of(
            new Item("Diamond", 100, "gem"),
            new Item("Emerald", 150, "gem"),
            new Item("Iron Sword", 50, "weapon")
        );

        PaginationContext<Item> ctx = this.createContext(items);
        ctx.setFilterStrategy(FilterStrategy.OR);

        ctx.addFilter("category", item -> "gem".equals(item.category));

        List<Item> filtered = ctx.getFilteredItems();

        assertThat(filtered).hasSize(2);
        assertThat(filtered).extracting(Item::name)
            .containsExactlyInAnyOrder("Diamond", "Emerald");
    }

    @Test
    @DisplayName("AND strategy with no matching items should return empty")
    void testAndStrategyNoMatches() {
        List<Item> items = List.of(
            new Item("Diamond", 100, "gem"),
            new Item("Emerald", 150, "gem")
        );

        PaginationContext<Item> ctx = this.createContext(items);
        ctx.setFilterStrategy(FilterStrategy.AND);

        // Impossible combination: gem AND weapon
        ctx.addFilter("gem", item -> "gem".equals(item.category));
        ctx.addFilter("weapon", item -> "weapon".equals(item.category));

        List<Item> filtered = ctx.getFilteredItems();

        assertThat(filtered).isEmpty();
    }

    @Test
    @DisplayName("OR strategy with no filters should return all items")
    void testOrStrategyNoFilters() {
        List<Item> items = List.of(
            new Item("Diamond", 100, "gem"),
            new Item("Iron Sword", 50, "weapon")
        );

        PaginationContext<Item> ctx = this.createContext(items);
        ctx.setFilterStrategy(FilterStrategy.OR);

        List<Item> filtered = ctx.getFilteredItems();

        assertThat(filtered).hasSize(2);
    }

    @Test
    @DisplayName("Should be able to switch strategies dynamically")
    void testSwitchStrategies() {
        List<Item> items = List.of(
            new Item("Diamond", 100, "gem"),
            new Item("Emerald", 200, "gem"),
            new Item("Iron Sword", 200, "weapon")
        );

        PaginationContext<Item> ctx = this.createContext(items);

        ctx.addFilter("category", item -> "gem".equals(item.category));
        ctx.addFilter("price", item -> item.price >= 200);

        // Start with AND
        ctx.setFilterStrategy(FilterStrategy.AND);
        List<Item> andResult = ctx.getFilteredItems();
        assertThat(andResult).hasSize(1);  // Only Emerald matches both
        assertThat(andResult.get(0).name).isEqualTo("Emerald");

        // Switch to OR
        ctx.setFilterStrategy(FilterStrategy.OR);
        List<Item> orResult = ctx.getFilteredItems();
        assertThat(orResult).hasSize(3);  // All match at least one filter
    }

    @Test
    @DisplayName("Setting null strategy should default to AND")
    void testNullStrategyDefaultsToAnd() {
        List<Item> items = List.of(
            new Item("Diamond", 100, "gem")
        );

        PaginationContext<Item> ctx = this.createContext(items);
        ctx.setFilterStrategy(null);

        assertThat(ctx.getFilterStrategy()).isEqualTo(FilterStrategy.AND);
    }

    @Test
    @DisplayName("AND strategy should short-circuit on first non-match")
    void testAndShortCircuit() {
        List<Item> items = List.of(
            new Item("Diamond", 100, "gem")
        );

        PaginationContext<Item> ctx = this.createContext(items);
        ctx.setFilterStrategy(FilterStrategy.AND);

        boolean[] secondFilterCalled = {false};

        ctx.addFilter("first", item -> false);  // Always fails
        ctx.addFilter("second", item -> {
            secondFilterCalled[0] = true;
            return true;
        });

        ctx.getFilteredItems();

        // Second filter should not be called due to short-circuit
        assertThat(secondFilterCalled[0]).isFalse();
    }

    @Test
    @DisplayName("OR strategy should short-circuit on first match")
    void testOrShortCircuit() {
        List<Item> items = List.of(
            new Item("Diamond", 100, "gem")
        );

        PaginationContext<Item> ctx = this.createContext(items);
        ctx.setFilterStrategy(FilterStrategy.OR);

        boolean[] secondFilterCalled = {false};

        ctx.addFilter("first", item -> true);  // Always passes
        ctx.addFilter("second", item -> {
            secondFilterCalled[0] = true;
            return true;
        });

        ctx.getFilteredItems();

        // Second filter should not be called due to short-circuit
        assertThat(secondFilterCalled[0]).isFalse();
    }

    @Test
    @DisplayName("Should handle complex AND filtering")
    void testComplexAndFiltering() {
        List<Item> items = List.of(
            new Item("Epic Diamond Sword", 500, "weapon"),
            new Item("Common Iron Sword", 50, "weapon"),
            new Item("Epic Gold Ring", 400, "jewelry"),
            new Item("Rare Diamond", 300, "gem")
        );

        PaginationContext<Item> ctx = this.createContext(items);
        ctx.setFilterStrategy(FilterStrategy.AND);

        ctx.addFilter("weapon", item -> "weapon".equals(item.category));
        ctx.addFilter("expensive", item -> item.price >= 400);
        ctx.addFilter("epic", item -> item.name.startsWith("Epic"));

        List<Item> filtered = ctx.getFilteredItems();

        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).name).isEqualTo("Epic Diamond Sword");
    }

    @Test
    @DisplayName("Should handle complex OR filtering")
    void testComplexOrFiltering() {
        List<Item> items = List.of(
            new Item("Diamond", 100, "gem"),
            new Item("Iron Sword", 200, "weapon"),
            new Item("Gold Ring", 50, "jewelry"),
            new Item("Emerald", 300, "gem")
        );

        PaginationContext<Item> ctx = this.createContext(items);
        ctx.setFilterStrategy(FilterStrategy.OR);

        ctx.addFilter("cheap", item -> item.price < 100);
        ctx.addFilter("weapon", item -> "weapon".equals(item.category));
        ctx.addFilter("expensive_gem", item -> "gem".equals(item.category) && (item.price >= 300));

        List<Item> filtered = ctx.getFilteredItems();

        assertThat(filtered).hasSize(3);
        assertThat(filtered).extracting(Item::name)
            .containsExactlyInAnyOrder("Gold Ring", "Iron Sword", "Emerald");
    }
}
