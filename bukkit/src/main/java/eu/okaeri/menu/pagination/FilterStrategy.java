package eu.okaeri.menu.pagination;

import lombok.NonNull;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * Strategy for combining multiple filters in pagination.
 * Determines how multiple active filters are applied to items.
 */
public enum FilterStrategy {

    /**
     * AND strategy: All filters must match (item passes if ALL filters return true).
     * This is the default strategy.
     */
    AND {
        @Override
        public <T> @NonNull Predicate<T> combine(@NonNull Collection<Predicate<T>> filters) {
            return item -> {
                for (Predicate<T> filter : filters) {
                    if (!filter.test(item)) {
                        return false;  // Short-circuit on first failure
                    }
                }
                return true;  // All filters passed
            };
        }
    },

    /**
     * OR strategy: Any filter must match (item passes if ANY filter returns true).
     */
    OR {
        @Override
        public <T> @NonNull Predicate<T> combine(@NonNull Collection<Predicate<T>> filters) {
            return item -> {
                if (filters.isEmpty()) {
                    return true;  // No filters = pass all
                }
                for (Predicate<T> filter : filters) {
                    if (filter.test(item)) {
                        return true;  // Short-circuit on first success
                    }
                }
                return false;  // No filter passed
            };
        }
    };

    /**
     * Combines multiple filter predicates into a single predicate.
     *
     * @param filters Collection of active filter predicates
     * @param <T>     The type of items being filtered
     * @return Combined predicate
     */
    public abstract <T> @NonNull Predicate<T> combine(@NonNull Collection<Predicate<T>> filters);
}
