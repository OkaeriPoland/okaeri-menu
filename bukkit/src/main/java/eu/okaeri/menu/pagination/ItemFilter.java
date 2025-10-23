package eu.okaeri.menu.pagination;

import lombok.Getter;
import lombok.NonNull;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Declarative filter that can be attached to MenuItems.
 * When the item's condition is active, this filter is applied to the target pane.
 *
 * <p>Example usage for in-memory filtering:
 * <pre>{@code
 * MenuItem filterButton = MenuItem.builder()
 *     .material(Material.DIAMOND)
 *     .name("Epic Only")
 *     .filter(ItemFilter.builder()
 *         .target("items")
 *         .when(() -> epicFilterActive)
 *         .predicate(item -> item.getRarity() == Rarity.EPIC)
 *         .build())
 *     .onClick(ctx -> {
 *         epicFilterActive = !epicFilterActive;
 *         ctx.refresh();
 *     })
 *     .build();
 * }</pre>
 *
 * <p>Example usage for database-side filtering (value-only filter):
 * <pre>{@code
 * String selectedCategory = "WEAPONS";
 * MenuItem categoryFilter = MenuItem.builder()
 *     .material(Material.IRON_SWORD)
 *     .name("Category: " + selectedCategory)
 *     .filter(ItemFilter.builder()
 *         .target("items")
 *         .id("category")
 *         .value(() -> selectedCategory)  // Extract value for database query
 *         .build())  // No predicate - this is a value-only filter
 *     .build();
 * }</pre>
 */
@Getter
public class ItemFilter<T> {

    private final String targetPane;
    private final Supplier<Boolean> condition;
    private final Predicate<T> predicate;  // Nullable - null means value-only filter
    private final Supplier<Object> valueExtractor;  // For database-side filtering
    private final String filterId;  // Optional ID for tracking

    private ItemFilter(Builder<T> builder) {
        this.targetPane = builder.targetPane;
        this.condition = builder.condition;
        this.predicate = builder.predicate;
        this.valueExtractor = builder.valueExtractor;
        this.filterId = builder.filterId;
    }

    /**
     * Checks if this filter is currently active.
     *
     * @return true if the filter should be applied
     */
    public boolean isActive() {
        return this.condition.get();
    }

    /**
     * Checks if this is a value-only filter (no in-memory predicate).
     * Value-only filters are used for database-side filtering.
     *
     * @return true if this filter only extracts values
     */
    public boolean isValueOnly() {
        return this.predicate == null;
    }

    /**
     * Tests an item against this filter's predicate.
     * Value-only filters (predicate == null) always return true (pass-through).
     *
     * @param item The item to test
     * @return true if the item passes the filter, or true if value-only filter
     */
    public boolean test(T item) {
        if (this.predicate == null) {
            return true;  // Pass-through - no in-memory filtering
        }
        return this.predicate.test(item);
    }

    /**
     * Extracts the filter value for database-side filtering.
     *
     * @return The extracted value, or null if no value extractor is set
     */
    public Object extractValue() {
        return (this.valueExtractor != null) ? this.valueExtractor.get() : null;
    }

    public static <T> @NonNull Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<T> {
        private String targetPane;
        private Supplier<Boolean> condition = () -> true;
        private Predicate<T> predicate;
        private Supplier<Object> valueExtractor;
        private String filterId;

        /**
         * Sets the target pane name that this filter applies to.
         *
         * @param targetPane The pane name
         * @return This builder
         */
        public @NonNull Builder<T> target(@NonNull String targetPane) {
            this.targetPane = targetPane;
            return this;
        }

        /**
         * Sets the condition for when this filter is active.
         * If not set, the filter is always active.
         *
         * @param condition Supplier that returns true when filter should be applied
         * @return This builder
         */
        public @NonNull Builder<T> when(@NonNull Supplier<Boolean> condition) {
            this.condition = condition;
            return this;
        }

        /**
         * Sets the predicate to test items against (for in-memory filtering).
         *
         * @param predicate The filter predicate
         * @return This builder
         */
        public @NonNull Builder<T> predicate(@NonNull Predicate<T> predicate) {
            this.predicate = predicate;
            return this;
        }

        /**
         * Sets the value extractor for database-side filtering.
         * Creates a value-only filter that doesn't perform in-memory filtering.
         *
         * @param valueExtractor Supplier that returns the filter value
         * @return This builder
         */
        public @NonNull Builder<T> value(@NonNull Supplier<Object> valueExtractor) {
            this.valueExtractor = valueExtractor;
            return this;
        }

        /**
         * Sets an optional ID for this filter.
         * Useful for debugging or tracking which filters are active.
         *
         * @param filterId The filter ID
         * @return This builder
         */
        public @NonNull Builder<T> id(@NonNull String filterId) {
            this.filterId = filterId;
            return this;
        }

        /**
         * Builds the item filter.
         *
         * @return The item filter
         * @throws IllegalArgumentException if required fields are missing
         */
        public @NonNull ItemFilter<T> build() {
            if ((this.targetPane == null) || this.targetPane.isEmpty()) {
                throw new IllegalArgumentException("Target pane name is required");
            }
            if ((this.predicate == null) && (this.valueExtractor == null)) {
                throw new IllegalArgumentException("Either predicate or value extractor is required");
            }
            return new ItemFilter<>(this);
        }
    }
}
