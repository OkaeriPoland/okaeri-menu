package eu.okaeri.menu.pagination;

import lombok.NonNull;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * Strategy for combining multiple filters in pagination.
 * Determines how filters are applied to items.
 *
 * <p>Strategies receive ALL filters and are responsible for:
 * <ul>
 *   <li>Checking which filters are active (via {@link ItemFilter#isActive()})</li>
 *   <li>Combining active filters according to the strategy logic</li>
 *   <li>Inspecting filter metadata (id, value, target) for conditional logic</li>
 * </ul>
 *
 * <p>This design allows custom strategies to implement complex filtering logic,
 * such as priority-based filtering, grouped filters, or dynamic strategy selection.
 *
 * <p>Example custom strategy:
 * <pre>{@code
 * public class PriorityFilterStrategy implements FilterStrategy {
 *     @Override
 *     public <T> Predicate<T> combine(Collection<ItemFilter<T>> filters) {
 *         return item -> {
 *             // Required filters (prefix "required-") must ALL match
 *             boolean requiredPass = filters.stream()
 *                 .filter(ItemFilter::isActive)
 *                 .filter(f -> f.getFilterId().startsWith("required-"))
 *                 .allMatch(f -> f.test(item));
 *
 *             // Optional filters (prefix "optional-") - ANY must match
 *             boolean optionalPass = filters.stream()
 *                 .filter(ItemFilter::isActive)
 *                 .filter(f -> f.getFilterId().startsWith("optional-"))
 *                 .anyMatch(f -> f.test(item));
 *
 *             return requiredPass && optionalPass;
 *         };
 *     }
 * }
 * }</pre>
 */
@FunctionalInterface
public interface FilterStrategy {

    /**
     * AND strategy: All active filters must match (item passes if ALL active filters return true).
     * This is the default strategy.
     *
     * <p>Filters are checked via {@link ItemFilter#isActive()} before testing.
     */
    FilterStrategy AND = new FilterStrategy() {
        @Override
        public <T> @NonNull Predicate<T> combine(@NonNull Collection<ItemFilter<T>> filters) {
            return item -> {
                for (ItemFilter<T> filter : filters) {
                    // Strategy handles isActive() check
                    if (filter.isActive() && !filter.test(item)) {
                        return false;  // Short-circuit on first failure
                    }
                }
                return true;  // All active filters passed
            };
        }
    };

    /**
     * OR strategy: Any active filter must match (item passes if ANY active filter returns true).
     *
     * <p>Filters are checked via {@link ItemFilter#isActive()} before testing.
     * If no filters are active, all items pass.
     */
    FilterStrategy OR = new FilterStrategy() {
        @Override
        public <T> @NonNull Predicate<T> combine(@NonNull Collection<ItemFilter<T>> filters) {
            return item -> {
                // Count and test active filters
                boolean hasActiveFilters = false;
                for (ItemFilter<T> filter : filters) {
                    if (filter.isActive()) {
                        hasActiveFilters = true;
                        if (filter.test(item)) {
                            return true;  // Short-circuit on first success
                        }
                    }
                }
                return !hasActiveFilters;  // No active filters = pass all
            };
        }
    };

    /**
     * Combines multiple filters into a single predicate.
     *
     * <p>Implementations MUST check {@link ItemFilter#isActive()} to determine
     * which filters should be applied. This allows strategies to have full control
     * over filter collection and combination logic.
     *
     * <p>Filters can be inspected for metadata (id, value, target) to enable
     * conditional combination logic in custom strategy implementations.
     *
     * @param filters Collection of ALL filters (active and inactive)
     * @param <T>     The type of items being filtered
     * @return Combined predicate that tests items against active filters
     */
    <T> @NonNull Predicate<T> combine(@NonNull Collection<ItemFilter<T>> filters);
}
