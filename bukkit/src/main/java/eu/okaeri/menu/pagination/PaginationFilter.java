package eu.okaeri.menu.pagination;

import lombok.NonNull;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Helper class for creating common pagination filters.
 * Provides combinators and utility methods for filter composition.
 */
public class PaginationFilter {

    /**
     * Creates a filter that always returns true (no filtering).
     *
     * @param <T> Item type
     * @return Accept-all filter
     */
    public static <T> @NonNull Predicate<T> all() {
        return item -> true;
    }

    /**
     * Creates a filter that always returns false (filter everything).
     *
     * @param <T> Item type
     * @return Reject-all filter
     */
    public static <T> @NonNull Predicate<T> none() {
        return item -> false;
    }

    /**
     * Combines multiple filters with AND logic.
     * An item must pass ALL filters to be included.
     *
     * @param filters The filters to combine
     * @param <T>     Item type
     * @return Combined filter
     */
    @SafeVarargs
    public static <T> @NonNull Predicate<T> and(@NonNull Predicate<T>... filters) {
        return item -> {
            for (Predicate<T> filter : filters) {
                if (!filter.test(item)) {
                    return false;
                }
            }
            return true;
        };
    }

    /**
     * Combines multiple filters with OR logic.
     * An item must pass AT LEAST ONE filter to be included.
     *
     * @param filters The filters to combine
     * @param <T>     Item type
     * @return Combined filter
     */
    @SafeVarargs
    public static <T> @NonNull Predicate<T> or(@NonNull Predicate<T>... filters) {
        return item -> {
            for (Predicate<T> filter : filters) {
                if (filter.test(item)) {
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * Inverts a filter (NOT logic).
     *
     * @param filter The filter to invert
     * @param <T>    Item type
     * @return Inverted filter
     */
    public static <T> @NonNull Predicate<T> not(@NonNull Predicate<T> filter) {
        return item -> !filter.test(item);
    }

    /**
     * Creates a filter for string contains (case-insensitive).
     *
     * @param extractor Function to extract string from item
     * @param search    Search string
     * @param <T>       Item type
     * @return String contains filter
     */
    public static <T> @NonNull Predicate<T> contains(@NonNull Function<T, String> extractor, @NonNull String search) {
        String lowerSearch = search.toLowerCase();
        return item -> {
            String value = extractor.apply(item);
            return (value != null) && value.toLowerCase().contains(lowerSearch);
        };
    }

    /**
     * Creates a filter for exact string match (case-insensitive).
     *
     * @param extractor Function to extract string from item
     * @param match     String to match
     * @param <T>       Item type
     * @return Exact match filter
     */
    public static <T> @NonNull Predicate<T> equals(@NonNull Function<T, String> extractor, @NonNull String match) {
        String lowerMatch = match.toLowerCase();
        return item -> {
            String value = extractor.apply(item);
            return (value != null) && value.toLowerCase().equals(lowerMatch);
        };
    }

    /**
     * Creates a filter for string starts with (case-insensitive).
     *
     * @param extractor Function to extract string from item
     * @param prefix    Prefix to match
     * @param <T>       Item type
     * @return Starts with filter
     */
    public static <T> @NonNull Predicate<T> startsWith(@NonNull Function<T, String> extractor, @NonNull String prefix) {
        String lowerPrefix = prefix.toLowerCase();
        return item -> {
            String value = extractor.apply(item);
            return (value != null) && value.toLowerCase().startsWith(lowerPrefix);
        };
    }

    /**
     * Creates a filter for numeric range.
     *
     * @param extractor Function to extract number from item
     * @param min       Minimum value (inclusive)
     * @param max       Maximum value (inclusive)
     * @param <T>       Item type
     * @return Range filter
     */
    public static <T> @NonNull Predicate<T> range(@NonNull Function<T, Number> extractor, double min, double max) {
        return item -> {
            Number value = extractor.apply(item);
            if (value == null) {
                return false;
            }
            double doubleValue = value.doubleValue();
            return (doubleValue >= min) && (doubleValue <= max);
        };
    }

    /**
     * Creates a filter for minimum value.
     *
     * @param extractor Function to extract number from item
     * @param min       Minimum value (inclusive)
     * @param <T>       Item type
     * @return Minimum value filter
     */
    public static <T> @NonNull Predicate<T> min(@NonNull Function<T, Number> extractor, double min) {
        return item -> {
            Number value = extractor.apply(item);
            return (value != null) && (value.doubleValue() >= min);
        };
    }

    /**
     * Creates a filter for maximum value.
     *
     * @param extractor Function to extract number from item
     * @param max       Maximum value (inclusive)
     * @param <T>       Item type
     * @return Maximum value filter
     */
    public static <T> @NonNull Predicate<T> max(@NonNull Function<T, Number> extractor, double max) {
        return item -> {
            Number value = extractor.apply(item);
            return (value != null) && (value.doubleValue() <= max);
        };
    }

    /**
     * Creates a filter based on a property value.
     *
     * @param extractor Function to extract property from item
     * @param expected  Expected value
     * @param <T>       Item type
     * @param <V>       Property type
     * @return Property equals filter
     */
    public static <T, V> @NonNull Predicate<T> property(@NonNull Function<T, V> extractor, V expected) {
        return item -> {
            V value = extractor.apply(item);
            return Objects.equals(value, expected);
        };
    }

    /**
     * Creates a filter that checks if a property is null.
     *
     * @param extractor Function to extract property from item
     * @param <T>       Item type
     * @return Is null filter
     */
    public static <T> @NonNull Predicate<T> isNull(@NonNull Function<T, ?> extractor) {
        return item -> extractor.apply(item) == null;
    }

    /**
     * Creates a filter that checks if a property is not null.
     *
     * @param extractor Function to extract property from item
     * @param <T>       Item type
     * @return Is not null filter
     */
    public static <T> @NonNull Predicate<T> isNotNull(@NonNull Function<T, ?> extractor) {
        return item -> extractor.apply(item) != null;
    }
}
