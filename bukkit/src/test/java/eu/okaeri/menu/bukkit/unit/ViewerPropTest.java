package eu.okaeri.menu.bukkit.unit;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.MenuContext;
import eu.okaeri.menu.async.AsyncExecutor;
import eu.okaeri.menu.state.ViewerProp;
import eu.okaeri.menu.state.ViewerState;
import org.bukkit.entity.HumanEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ReactiveProperty.
 * Tests caching, invalidation, and supplier evaluation without Bukkit dependencies.
 */
class ViewerPropTest {

    @Test
    @DisplayName("Should cache supplier results")
    void testCaching() {
        AtomicInteger callCount = new AtomicInteger(0);

        ViewerProp<String> property = ViewerProp.of(() -> {
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

        ViewerProp<String> property = ViewerProp.of(() -> {
            callCount.incrementAndGet();
            return "value-" + callCount.get();
        });

        MenuContext context = this.createMockContext();

        assertThat(property.get(context)).isEqualTo("value-1");
        assertThat(callCount.get()).isEqualTo(1);

        // Invalidate (per-player via ViewerState)
        context.getViewerState().invalidateProp(property);

        assertThat(property.get(context)).isEqualTo("value-2");
        assertThat(callCount.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should handle static values")
    void testStaticValue() {
        ViewerProp<String> property = ViewerProp.of("static");

        MenuContext context = this.createMockContext();

        assertThat(property.get(context)).isEqualTo("static");
        assertThat(property.get(context)).isEqualTo("static");

        // Invalidation should not affect static values (per-player)
        context.getViewerState().invalidateProp(property);
        assertThat(property.get(context)).isEqualTo("static");
    }

    @Test
    @DisplayName("Should handle null values")
    void testNullValue() {
        ViewerProp<String> property = ViewerProp.of(() -> null);

        MenuContext context = this.createMockContext();

        assertThat(property.get(context)).isNull();
    }

    @Test
    @DisplayName("Should re-evaluate after multiple invalidations")
    void testMultipleInvalidations() {
        AtomicInteger counter = new AtomicInteger(0);

        ViewerProp<Integer> property = ViewerProp.of(counter::incrementAndGet
        );

        MenuContext context = this.createMockContext();

        assertThat(property.get(context)).isEqualTo(1);

        context.getViewerState().invalidateProp(property);
        assertThat(property.get(context)).isEqualTo(2);

        context.getViewerState().invalidateProp(property);
        assertThat(property.get(context)).isEqualTo(3);
    }

    @Test
    @DisplayName("Should cache null values")
    void testNullCaching() {
        AtomicInteger callCount = new AtomicInteger(0);

        ViewerProp<String> property = ViewerProp.of(() -> {
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
        ViewerProp<String> property = ViewerProp.of(() -> {
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
        ViewerProp<Integer> intProp = ViewerProp.of(42);
        ViewerProp<Boolean> boolProp = ViewerProp.of(true);
        ViewerProp<Double> doubleProp = ViewerProp.of(3.14);

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

        ViewerProp<TestData> property = ViewerProp.of(
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
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        MenuContext context = new MenuContext(menu, player);
        AsyncExecutor asyncExecutor = mock(AsyncExecutor.class);

        // Mock BEFORE creating ViewerState (ViewerState constructor needs AsyncExecutor)
        when(menu.getAsyncExecutor()).thenReturn(asyncExecutor);

        ViewerState viewerState = new ViewerState(context, null);
        when(menu.getViewerState(player.getUniqueId())).thenReturn(viewerState);

        return context;
    }
}
