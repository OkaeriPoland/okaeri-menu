package eu.okaeri.menu.bukkit.unit;

import eu.okaeri.menu.pagination.PaginationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PaginationFilter.
 * Tests filter composition and utility methods without Bukkit dependencies.
 */
class PaginationFilterTest {

    record TestItem(String name, int value, String category) {
    }

    @Test
    @DisplayName("Should accept all items with all() filter")
    void testAllFilter() {
        Predicate<TestItem> filter = PaginationFilter.all();

        assertThat(filter.test(new TestItem("A", 1, "cat1"))).isTrue();
        assertThat(filter.test(new TestItem("B", 2, "cat2"))).isTrue();
    }

    @Test
    @DisplayName("Should reject all items with none() filter")
    void testNoneFilter() {
        Predicate<TestItem> filter = PaginationFilter.none();

        assertThat(filter.test(new TestItem("A", 1, "cat1"))).isFalse();
        assertThat(filter.test(new TestItem("B", 2, "cat2"))).isFalse();
    }

    @Test
    @DisplayName("Should combine filters with AND logic")
    void testAndFilter() {
        Predicate<TestItem> nameFilter = item -> item.name().startsWith("A");
        Predicate<TestItem> valueFilter = item -> item.value() > 5;

        Predicate<TestItem> combined = PaginationFilter.and(nameFilter, valueFilter);

        assertThat(combined.test(new TestItem("ABC", 10, "cat1"))).isTrue();
        assertThat(combined.test(new TestItem("ABC", 3, "cat1"))).isFalse(); // Fails value check
        assertThat(combined.test(new TestItem("XYZ", 10, "cat1"))).isFalse(); // Fails name check
    }

    @Test
    @DisplayName("Should combine filters with OR logic")
    void testOrFilter() {
        Predicate<TestItem> nameFilter = item -> "A".equals(item.name());
        Predicate<TestItem> valueFilter = item -> item.value() > 10;

        Predicate<TestItem> combined = PaginationFilter.or(nameFilter, valueFilter);

        assertThat(combined.test(new TestItem("A", 5, "cat1"))).isTrue(); // Passes name check
        assertThat(combined.test(new TestItem("B", 15, "cat1"))).isTrue(); // Passes value check
        assertThat(combined.test(new TestItem("B", 5, "cat1"))).isFalse(); // Fails both
    }

    @Test
    @DisplayName("Should invert filter with NOT logic")
    void testNotFilter() {
        Predicate<TestItem> nameFilter = item -> "A".equals(item.name());
        Predicate<TestItem> inverted = PaginationFilter.not(nameFilter);

        assertThat(inverted.test(new TestItem("A", 1, "cat1"))).isFalse();
        assertThat(inverted.test(new TestItem("B", 1, "cat1"))).isTrue();
    }

    @Test
    @DisplayName("Should filter by string contains (case-insensitive)")
    void testContainsFilter() {
        Predicate<TestItem> filter = PaginationFilter.contains(TestItem::name, "tes");

        assertThat(filter.test(new TestItem("Test", 1, "cat1"))).isTrue();
        assertThat(filter.test(new TestItem("TESTING", 1, "cat1"))).isTrue();
        assertThat(filter.test(new TestItem("latest", 1, "cat1"))).isTrue();
        assertThat(filter.test(new TestItem("Sample", 1, "cat1"))).isFalse();
    }

    @Test
    @DisplayName("Should filter by exact string match (case-insensitive)")
    void testEqualsFilter() {
        Predicate<TestItem> filter = PaginationFilter.equals(TestItem::name, "test");

        assertThat(filter.test(new TestItem("Test", 1, "cat1"))).isTrue();
        assertThat(filter.test(new TestItem("TEST", 1, "cat1"))).isTrue();
        assertThat(filter.test(new TestItem("Testing", 1, "cat1"))).isFalse();
    }

    @Test
    @DisplayName("Should filter by string starts with (case-insensitive)")
    void testStartsWithFilter() {
        Predicate<TestItem> filter = PaginationFilter.startsWith(TestItem::name, "te");

        assertThat(filter.test(new TestItem("Test", 1, "cat1"))).isTrue();
        assertThat(filter.test(new TestItem("TECHNOLOGY", 1, "cat1"))).isTrue();
        assertThat(filter.test(new TestItem("Latest", 1, "cat1"))).isFalse();
    }

    @Test
    @DisplayName("Should filter by numeric range")
    void testRangeFilter() {
        Predicate<TestItem> filter = PaginationFilter.range(TestItem::value, 5, 10);

        assertThat(filter.test(new TestItem("A", 5, "cat1"))).isTrue();  // Min inclusive
        assertThat(filter.test(new TestItem("A", 7, "cat1"))).isTrue();  // Middle
        assertThat(filter.test(new TestItem("A", 10, "cat1"))).isTrue(); // Max inclusive
        assertThat(filter.test(new TestItem("A", 4, "cat1"))).isFalse(); // Below min
        assertThat(filter.test(new TestItem("A", 11, "cat1"))).isFalse(); // Above max
    }

    @Test
    @DisplayName("Should filter by minimum value")
    void testMinFilter() {
        Predicate<TestItem> filter = PaginationFilter.min(TestItem::value, 10);

        assertThat(filter.test(new TestItem("A", 10, "cat1"))).isTrue();
        assertThat(filter.test(new TestItem("A", 15, "cat1"))).isTrue();
        assertThat(filter.test(new TestItem("A", 9, "cat1"))).isFalse();
    }

    @Test
    @DisplayName("Should filter by maximum value")
    void testMaxFilter() {
        Predicate<TestItem> filter = PaginationFilter.max(TestItem::value, 10);

        assertThat(filter.test(new TestItem("A", 10, "cat1"))).isTrue();
        assertThat(filter.test(new TestItem("A", 5, "cat1"))).isTrue();
        assertThat(filter.test(new TestItem("A", 11, "cat1"))).isFalse();
    }

    @Test
    @DisplayName("Should filter by property equality")
    void testPropertyFilter() {
        Predicate<TestItem> filter = PaginationFilter.property(TestItem::category, "weapons");

        assertThat(filter.test(new TestItem("A", 1, "weapons"))).isTrue();
        assertThat(filter.test(new TestItem("A", 1, "armor"))).isFalse();
    }

    @Test
    @DisplayName("Should filter by null property")
    void testIsNullFilter() {
        record NullableItem(String name, Integer value) {
        }

        Predicate<NullableItem> filter = PaginationFilter.isNull(NullableItem::value);

        assertThat(filter.test(new NullableItem("A", null))).isTrue();
        assertThat(filter.test(new NullableItem("A", 5))).isFalse();
    }

    @Test
    @DisplayName("Should filter by non-null property")
    void testIsNotNullFilter() {
        record NullableItem(String name, Integer value) {
        }

        Predicate<NullableItem> filter = PaginationFilter.isNotNull(NullableItem::value);

        assertThat(filter.test(new NullableItem("A", 5))).isTrue();
        assertThat(filter.test(new NullableItem("A", null))).isFalse();
    }

    @Test
    @DisplayName("Should handle complex filter composition")
    void testComplexComposition() {
        // (name starts with "A" OR value > 10) AND category = "weapons"
        Predicate<TestItem> nameOrValue = PaginationFilter.or(
            PaginationFilter.startsWith(TestItem::name, "A"),
            PaginationFilter.min(TestItem::value, 10)
        );

        Predicate<TestItem> complex = PaginationFilter.and(
            nameOrValue,
            PaginationFilter.property(TestItem::category, "weapons")
        );

        assertThat(complex.test(new TestItem("ABC", 5, "weapons"))).isTrue(); // Name matches
        assertThat(complex.test(new TestItem("XYZ", 15, "weapons"))).isTrue(); // Value matches
        assertThat(complex.test(new TestItem("ABC", 5, "armor"))).isFalse(); // Wrong category
        assertThat(complex.test(new TestItem("XYZ", 5, "weapons"))).isFalse(); // Neither name nor value
    }

    @Test
    @DisplayName("Should handle null values gracefully in string filters")
    void testNullHandling() {
        record NullableItem(String name) {
        }

        Predicate<NullableItem> filter = PaginationFilter.contains(NullableItem::name, "test");

        assertThat(filter.test(new NullableItem(null))).isFalse();
    }
}
