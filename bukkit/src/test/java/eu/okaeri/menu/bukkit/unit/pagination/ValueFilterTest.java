package eu.okaeri.menu.bukkit.unit.pagination;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.pagination.ItemFilter;
import eu.okaeri.menu.pagination.LoaderContext;
import eu.okaeri.menu.pagination.PaginationContext;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for value-only filters (database-side filtering).
 */
class ValueFilterTest {

    private ServerMock server;
    private org.bukkit.plugin.java.JavaPlugin plugin;
    private PlayerMock player;
    private Menu menu;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.plugin = MockBukkit.createMockPlugin();
        this.player = this.server.addPlayer();

        // Create menu and open for player to establish ViewerState
        this.menu = Menu.builder(this.plugin).rows(3).build();
        this.menu.open(this.player);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // Test data
    @Value
    @Accessors(fluent = true)
    static class Offer {
        String name;
        int price;
        UUID sellerId;
        String category;
    }

    // ========================================
    // ItemFilter Value-Only Building
    // ========================================

    @Test
    @DisplayName("Should build ItemFilter with value extractor only")
    void testBuildValueOnlyFilter() {
        String category = "WEAPONS";

        ItemFilter<Offer> filter = ItemFilter.<Offer>builder()
            .target("offers")
            .id("category")
            .value(() -> category)
            .build();

        assertThat(filter.getTargetPane()).isEqualTo("offers");
        assertThat(filter.getFilterId()).isEqualTo("category");
        assertThat(filter.isValueOnly()).isTrue();
        assertThat(filter.extractValue()).isEqualTo("WEAPONS");
    }

    @Test
    @DisplayName("Should build ItemFilter with both predicate and value")
    void testBuildMixedFilter() {
        String category = "WEAPONS";

        ItemFilter<Offer> filter = ItemFilter.<Offer>builder()
            .target("offers")
            .id("category")
            .predicate(item -> category.equals(item.category))
            .value(() -> category)
            .build();

        assertThat(filter.isValueOnly()).isFalse();
        assertThat(filter.extractValue()).isEqualTo("WEAPONS");

        Offer weapon = new Offer("Sword", 100, UUID.randomUUID(), "WEAPONS");
        Offer armor = new Offer("Helmet", 50, UUID.randomUUID(), "ARMOR");

        assertThat(filter.test(weapon)).isTrue();
        assertThat(filter.test(armor)).isFalse();
    }

    @Test
    @DisplayName("Should throw when neither predicate nor value is provided")
    void testBuildWithoutPredicateOrValue() {
        assertThatThrownBy(() -> ItemFilter.<Offer>builder()
            .target("offers")
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Either predicate or value extractor is required");
    }

    @Test
    @DisplayName("Should throw when target is missing")
    void testBuildValueFilterWithoutTarget() {
        assertThatThrownBy(() -> ItemFilter.<Offer>builder()
            .value(() -> "WEAPONS")
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Target pane name is required");
    }

    // ========================================
    // Value-Only Filter Behavior
    // ========================================

    @Test
    @DisplayName("isValueOnly should return true when predicate is null")
    void testIsValueOnlyTrue() {
        ItemFilter<Offer> filter = ItemFilter.<Offer>builder()
            .target("offers")
            .value(() -> "WEAPONS")
            .build();

        assertThat(filter.isValueOnly()).isTrue();
    }

    @Test
    @DisplayName("isValueOnly should return false when predicate is present")
    void testIsValueOnlyFalse() {
        ItemFilter<Offer> filter = ItemFilter.<Offer>builder()
            .target("offers")
            .predicate(item -> true)
            .build();

        assertThat(filter.isValueOnly()).isFalse();
    }

    @Test
    @DisplayName("extractValue should return the extracted value")
    void testExtractValue() {
        String[] currentCategory = {"WEAPONS"};

        ItemFilter<Offer> filter = ItemFilter.<Offer>builder()
            .target("offers")
            .value(() -> currentCategory[0])
            .build();

        assertThat(filter.extractValue()).isEqualTo("WEAPONS");

        currentCategory[0] = "ARMOR";
        assertThat(filter.extractValue()).isEqualTo("ARMOR");
    }

    @Test
    @DisplayName("extractValue should return null when no value extractor")
    void testExtractValueNull() {
        ItemFilter<Offer> filter = ItemFilter.<Offer>builder()
            .target("offers")
            .predicate(item -> true)
            .build();

        assertThat(filter.extractValue()).isNull();
    }

    @Test
    @DisplayName("test should return true for value-only filters (pass-through)")
    void testValueOnlyAlwaysPassesTest() {
        ItemFilter<Offer> filter = ItemFilter.<Offer>builder()
            .target("offers")
            .value(() -> "WEAPONS")
            .build();

        Offer weapon = new Offer("Sword", 100, UUID.randomUUID(), "WEAPONS");
        Offer armor = new Offer("Helmet", 50, UUID.randomUUID(), "ARMOR");

        // Value-only filters don't perform in-memory filtering
        assertThat(filter.test(weapon)).isTrue();
        assertThat(filter.test(armor)).isTrue();
    }

    @Test
    @DisplayName("Should support different value types")
    void testDifferentValueTypes() {
        UUID sellerId = UUID.randomUUID();
        Integer minPrice = 100;
        Boolean isRare = true;

        ItemFilter<Offer> uuidFilter = ItemFilter.<Offer>builder()
            .target("offers")
            .id("seller")
            .value(() -> sellerId)
            .build();

        ItemFilter<Offer> intFilter = ItemFilter.<Offer>builder()
            .target("offers")
            .id("minPrice")
            .value(() -> minPrice)
            .build();

        ItemFilter<Offer> boolFilter = ItemFilter.<Offer>builder()
            .target("offers")
            .id("rare")
            .value(() -> isRare)
            .build();

        assertThat(uuidFilter.extractValue()).isEqualTo(sellerId);
        assertThat(intFilter.extractValue()).isEqualTo(100);
        assertThat(boolFilter.extractValue()).isEqualTo(true);
    }

    // ========================================
    // PaginationContext Value Tracking
    // ========================================

    @Test
    @DisplayName("Should store filter values in PaginationContext")
    void testPaginationContextStoreValues() {
        List<Offer> items = List.of();
        PaginationContext<Offer> ctx = PaginationContext.get(this.menu, "test", this.player, items, 10);

        ctx.addFilter("category", null, "WEAPONS");
        ctx.addFilter("minPrice", null, 100);

        Map<String, Object> values = ctx.getActiveFilterValues();
        assertThat(values).hasSize(2);
        assertThat(values.get("category")).isEqualTo("WEAPONS");
        assertThat(values.get("minPrice")).isEqualTo(100);
    }

    @Test
    @DisplayName("Should retrieve typed filter values")
    void testGetFilterValueWithType() {
        List<Offer> items = List.of();
        PaginationContext<Offer> ctx = PaginationContext.get(this.menu, "test", this.player, items, 10);

        UUID sellerId = UUID.randomUUID();
        ctx.addFilter("seller", null, sellerId);
        ctx.addFilter("category", null, "WEAPONS");

        assertThat(ctx.getFilterValue("seller", UUID.class))
            .isPresent()
            .contains(sellerId);

        assertThat(ctx.getFilterValue("category", String.class))
            .isPresent()
            .contains("WEAPONS");
    }

    @Test
    @DisplayName("Should return empty when filter value not found")
    void testGetFilterValueNotFound() {
        List<Offer> items = List.of();
        PaginationContext<Offer> ctx = PaginationContext.get(this.menu, "test", this.player, items, 10);

        assertThat(ctx.getFilterValue("nonexistent", String.class))
            .isEmpty();
    }

    @Test
    @DisplayName("Should return empty when filter value type mismatch")
    void testGetFilterValueTypeMismatch() {
        List<Offer> items = List.of();
        PaginationContext<Offer> ctx = PaginationContext.get(this.menu, "test", this.player, items, 10);

        ctx.addFilter("category", null, "WEAPONS");

        // Try to retrieve as Integer (wrong type)
        assertThat(ctx.getFilterValue("category", Integer.class))
            .isEmpty();
    }

    @Test
    @DisplayName("Should support both predicate and value in same filter")
    void testAddFilterWithBothPredicateAndValue() {
        List<Offer> items = List.of(
            new Offer("Sword", 100, UUID.randomUUID(), "WEAPONS"),
            new Offer("Shield", 50, UUID.randomUUID(), "ARMOR")
        );
        PaginationContext<Offer> ctx = PaginationContext.get(this.menu, "test", this.player, items, 10);

        String category = "WEAPONS";
        ctx.addFilter("category", item -> category.equals(item.category), category);

        // Predicate should work for in-memory filtering
        List<Offer> filtered = ctx.getFilteredItems();
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).name()).isEqualTo("Sword");

        // Value should be stored for database queries
        assertThat(ctx.getFilterValue("category", String.class))
            .isPresent()
            .contains("WEAPONS");
    }

    @Test
    @DisplayName("Should remove filter values when filter is removed")
    void testRemoveFilterRemovesValue() {
        List<Offer> items = List.of();
        PaginationContext<Offer> ctx = PaginationContext.get(this.menu, "test", this.player, items, 10);

        ctx.addFilter("category", null, "WEAPONS");
        assertThat(ctx.getActiveFilterValues()).hasSize(1);

        ctx.removeFilter("category");
        assertThat(ctx.getActiveFilterValues()).isEmpty();
    }

    @Test
    @DisplayName("Should clear all filter values when filters are cleared")
    void testClearFiltersClearsValues() {
        List<Offer> items = List.of();
        PaginationContext<Offer> ctx = PaginationContext.get(this.menu, "test", this.player, items, 10);

        ctx.addFilter("category", null, "WEAPONS");
        ctx.addFilter("minPrice", null, 100);
        assertThat(ctx.getActiveFilterValues()).hasSize(2);

        ctx.clearFilters();
        assertThat(ctx.getActiveFilterValues()).isEmpty();
    }

    @Test
    @DisplayName("Should clear filter values with prefix")
    void testClearFiltersWithPrefixClearsValues() {
        List<Offer> items = List.of();
        PaginationContext<Offer> ctx = PaginationContext.get(this.menu, "test", this.player, items, 10);

        ctx.addFilter("declarative:category", null, "WEAPONS");
        ctx.addFilter("declarative:seller", null, UUID.randomUUID());
        ctx.addFilter("programmatic:minPrice", null, 100);

        ctx.clearFiltersWithPrefix("declarative:");

        Map<String, Object> values = ctx.getActiveFilterValues();
        assertThat(values).hasSize(1);
        assertThat(values).containsKey("programmatic:minPrice");
    }

    @Test
    @DisplayName("Should return unmodifiable map from getActiveFilterValues")
    void testActiveFilterValuesUnmodifiable() {
        List<Offer> items = List.of();
        PaginationContext<Offer> ctx = PaginationContext.get(this.menu, "test", this.player, items, 10);

        ctx.addFilter("category", null, "WEAPONS");

        Map<String, Object> values = ctx.getActiveFilterValues();

        assertThatThrownBy(() -> values.put("hack", "value"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    // ========================================
    // LoaderContext
    // ========================================

    @Test
    @DisplayName("Should create LoaderContext with pagination state and filter values")
    void testCreateLoaderContext() {
        // Create LoaderContext directly with filter values
        Map<String, Object> filterValues = Map.of(
            "category", "WEAPONS",
            "minPrice", 100
        );

        LoaderContext loaderCtx = new LoaderContext(2, 10, filterValues);

        assertThat(loaderCtx.getCurrentPage()).isEqualTo(2);
        assertThat(loaderCtx.getPageSize()).isEqualTo(10);
        assertThat(loaderCtx.getActiveFilterCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("LoaderContext should retrieve typed filter values")
    void testLoaderContextGetFilter() {
        UUID sellerId = UUID.randomUUID();
        Map<String, Object> filters = Map.of(
            "seller", sellerId,
            "category", "WEAPONS",
            "minPrice", 100
        );

        LoaderContext ctx = new LoaderContext(0, 10, filters);

        assertThat(ctx.getFilter("seller", UUID.class))
            .isPresent()
            .contains(sellerId);

        assertThat(ctx.getFilter("category", String.class))
            .isPresent()
            .contains("WEAPONS");

        assertThat(ctx.getFilter("minPrice", Integer.class))
            .isPresent()
            .contains(100);
    }

    @Test
    @DisplayName("LoaderContext should return empty for missing filter")
    void testLoaderContextGetFilterMissing() {
        LoaderContext ctx = new LoaderContext(0, 10, Map.of());

        assertThat(ctx.getFilter("nonexistent", String.class))
            .isEmpty();
    }

    @Test
    @DisplayName("LoaderContext should return empty on type mismatch")
    void testLoaderContextGetFilterTypeMismatch() {
        Map<String, Object> filters = Map.of("category", "WEAPONS");
        LoaderContext ctx = new LoaderContext(0, 10, filters);

        assertThat(ctx.getFilter("category", Integer.class))
            .isEmpty();
    }

    @Test
    @DisplayName("LoaderContext should check filter presence")
    void testLoaderContextHasFilter() {
        Map<String, Object> filters = Map.of(
            "category", "WEAPONS",
            "minPrice", 100
        );

        LoaderContext ctx = new LoaderContext(0, 10, filters);

        assertThat(ctx.hasFilter("category")).isTrue();
        assertThat(ctx.hasFilter("minPrice")).isTrue();
        assertThat(ctx.hasFilter("seller")).isFalse();
    }

    @Test
    @DisplayName("LoaderContext should provide unmodifiable filters map")
    void testLoaderContextUnmodifiableFilters() {
        Map<String, Object> filters = Map.of("category", "WEAPONS");
        LoaderContext ctx = new LoaderContext(0, 10, filters);

        assertThatThrownBy(() -> ctx.getActiveFilters().put("hack", "value"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    // ========================================
    // Integration Tests
    // ========================================

    @Test
    @DisplayName("Should support null values in filters")
    void testNullFilterValue() {
        List<Offer> items = List.of();
        PaginationContext<Offer> ctx = PaginationContext.get(this.menu, "test", this.player, items, 10);

        ctx.addFilter("seller", null, null);

        assertThat(ctx.getActiveFilterValues()).isEmpty();
    }

    @Test
    @DisplayName("Should update filter value when filter is re-added")
    void testUpdateFilterValue() {
        List<Offer> items = List.of();
        PaginationContext<Offer> ctx = PaginationContext.get(this.menu, "test", this.player, items, 10);

        ctx.addFilter("category", null, "WEAPONS");
        assertThat(ctx.getFilterValue("category", String.class))
            .isPresent()
            .contains("WEAPONS");

        ctx.addFilter("category", null, "ARMOR");
        assertThat(ctx.getFilterValue("category", String.class))
            .isPresent()
            .contains("ARMOR");
    }

    @Test
    @DisplayName("Should handle mixed value-only and predicate filters")
    void testMixedFilterTypes() {
        List<Offer> items = List.of(
            new Offer("Sword", 150, UUID.randomUUID(), "WEAPONS"),
            new Offer("Bow", 75, UUID.randomUUID(), "WEAPONS"),
            new Offer("Helmet", 100, UUID.randomUUID(), "ARMOR")
        );
        PaginationContext<Offer> ctx = PaginationContext.get(this.menu, "test", this.player, items, 10);

        // Value-only filter (for database)
        ctx.addFilter("category", null, "WEAPONS");

        // Predicate filter (for in-memory)
        ctx.addFilter("expensive", item -> item.price >= 100, 100);

        // In-memory filtering should only use predicate
        List<Offer> filtered = ctx.getFilteredItems();
        assertThat(filtered).hasSize(2); // Sword (150) and Helmet (100)

        // Values should be available for database queries
        assertThat(ctx.getActiveFilterValues()).hasSize(2);
        assertThat(ctx.getFilterValue("category", String.class))
            .isPresent()
            .contains("WEAPONS");
        assertThat(ctx.getFilterValue("expensive", Integer.class))
            .isPresent()
            .contains(100);
    }
}
