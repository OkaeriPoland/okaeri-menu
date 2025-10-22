package eu.okaeri.menu.bukkit.unit;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.MenuContext;
import eu.okaeri.menu.reactive.ReactiveProperty;
import org.bukkit.entity.HumanEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for ReactiveProperty.
 * Tests caching, invalidation, and supplier evaluation without Bukkit dependencies.
 */
class ReactivePropertyTest {

    @Test
    @DisplayName("Should cache supplier results")
    void testCaching() {
        AtomicInteger callCount = new AtomicInteger(0);

        ReactiveProperty<String> property = ReactiveProperty.of(() -> {
            callCount.incrementAndGet();
            return "value";
        });

        MenuContext context = this.createMockContext();

        // First call
        assertThat(property.get(context)).isEqualTo("value");
        assertThat(callCount.get()).isEqualTo(1);

        // Second call - should be cached
        assertThat(property.get(context)).isEqualTo("value");
        assertThat(callCount.get()).isEqualTo(1); // No additional call
    }

    @Test
    @DisplayName("Should invalidate cache")
    void testInvalidation() {
        AtomicInteger callCount = new AtomicInteger(0);

        ReactiveProperty<String> property = ReactiveProperty.of(() -> {
            callCount.incrementAndGet();
            return "value-" + callCount.get();
        });

        MenuContext context = this.createMockContext();

        assertThat(property.get(context)).isEqualTo("value-1");
        assertThat(callCount.get()).isEqualTo(1);

        // Invalidate
        property.invalidate();

        assertThat(property.get(context)).isEqualTo("value-2");
        assertThat(callCount.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should handle static values")
    void testStaticValue() {
        ReactiveProperty<String> property = ReactiveProperty.of("static");

        MenuContext context = this.createMockContext();

        assertThat(property.get(context)).isEqualTo("static");
        assertThat(property.get(context)).isEqualTo("static");

        // Invalidation should not affect static values
        property.invalidate();
        assertThat(property.get(context)).isEqualTo("static");
    }

    @Test
    @DisplayName("Should handle null values")
    void testNullValue() {
        ReactiveProperty<String> property = ReactiveProperty.of(() -> null);

        MenuContext context = this.createMockContext();

        assertThat(property.get(context)).isNull();
    }

    @Test
    @DisplayName("Should re-evaluate after multiple invalidations")
    void testMultipleInvalidations() {
        AtomicInteger counter = new AtomicInteger(0);

        ReactiveProperty<Integer> property = ReactiveProperty.of(counter::incrementAndGet
        );

        MenuContext context = this.createMockContext();

        assertThat(property.get(context)).isEqualTo(1);

        property.invalidate();
        assertThat(property.get(context)).isEqualTo(2);

        property.invalidate();
        assertThat(property.get(context)).isEqualTo(3);
    }

    @Test
    @DisplayName("Should cache null values")
    void testNullCaching() {
        AtomicInteger callCount = new AtomicInteger(0);

        ReactiveProperty<String> property = ReactiveProperty.of(() -> {
            callCount.incrementAndGet();
            return null;
        });

        MenuContext context = this.createMockContext();

        assertThat(property.get(context)).isNull();
        assertThat(callCount.get()).isEqualTo(1);

        // Second call should use cached null
        assertThat(property.get(context)).isNull();
        assertThat(callCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle supplier exceptions gracefully")
    void testSupplierException() {
        ReactiveProperty<String> property = ReactiveProperty.of(() -> {
            throw new RuntimeException("Test exception");
        });

        MenuContext context = this.createMockContext();

        assertThatThrownBy(() -> property.get(context))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Test exception");
    }

    @Test
    @DisplayName("Should support different value types")
    void testDifferentTypes() {
        ReactiveProperty<Integer> intProp = ReactiveProperty.of(42);
        ReactiveProperty<Boolean> boolProp = ReactiveProperty.of(true);
        ReactiveProperty<Double> doubleProp = ReactiveProperty.of(3.14);

        MenuContext context = this.createMockContext();

        assertThat(intProp.get(context)).isEqualTo(42);
        assertThat(boolProp.get(context)).isTrue();
        assertThat(doubleProp.get(context)).isEqualTo(3.14);
    }

    @Test
    @DisplayName("Should handle complex objects")
    void testComplexObjects() {
        record TestData(String name, int value) {
        }

        ReactiveProperty<TestData> property = ReactiveProperty.of(
            () -> new TestData("test", 123)
        );

        MenuContext context = this.createMockContext();

        TestData result = property.get(context);
        assertThat(result.name()).isEqualTo("test");
        assertThat(result.value()).isEqualTo(123);
    }

    // Helper method
    private MenuContext createMockContext() {
        Menu menu = mock(Menu.class);
        HumanEntity player = mock(HumanEntity.class);
        return new MenuContext(menu, player);
    }
}
