package eu.okaeri.menu.bukkit.unit.async;

import eu.okaeri.menu.async.ComputedValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ComputedValue fluent API.
 * Tests state management, transformations, fallbacks, and chaining behavior.
 */
class ComputedValueTest {

    // ========================================
    // FACTORY METHODS
    // ========================================

    @Test
    @DisplayName("Should create SUCCESS state with value")
    void testSuccessFactory() {
        ComputedValue<String> value = ComputedValue.success("test");

        assertThat(value.isPresent()).isTrue();
        assertThat(value.isLoading()).isFalse();
        assertThat(value.isError()).isFalse();
        assertThat(value.orElse("fallback")).isEqualTo("test");
    }

    @Test
    @DisplayName("Should create LOADING state")
    void testLoadingFactory() {
        ComputedValue<String> value = ComputedValue.loading();

        assertThat(value.isPresent()).isFalse();
        assertThat(value.isLoading()).isTrue();
        assertThat(value.isError()).isFalse();
    }

    @Test
    @DisplayName("Should create ERROR state with exception")
    void testErrorFactory() {
        Throwable error = new RuntimeException("Test error");
        ComputedValue<String> value = ComputedValue.error(error);

        assertThat(value.isPresent()).isFalse();
        assertThat(value.isLoading()).isFalse();
        assertThat(value.isError()).isTrue();
        assertThat(value.getError()).contains(error);
    }

    @Test
    @DisplayName("Should create empty state")
    void testEmptyFactory() {
        ComputedValue<String> value = ComputedValue.empty();

        assertThat(value.isPresent()).isFalse();
        assertThat(value.isLoading()).isFalse();
        assertThat(value.isError()).isFalse();
        assertThat(value.toOptional()).isEmpty();
    }

    // ========================================
    // MAP TRANSFORMATION
    // ========================================

    @Test
    @DisplayName("Should map SUCCESS value")
    void testMapSuccess() {
        ComputedValue<String> original = ComputedValue.success("hello");
        ComputedValue<Integer> mapped = original.map(String::length);

        assertThat(mapped.isPresent()).isTrue();
        assertThat(mapped.orElse(0)).isEqualTo(5);
    }

    @Test
    @DisplayName("Should chain multiple map operations")
    void testMapChaining() {
        ComputedValue<String> result = ComputedValue.success(10)
            .map(n -> n * 2)
            .map(n -> n + 5)
            .map(n -> "Value: " + n);

        assertThat(result.orElse("")).isEqualTo("Value: 25");
    }

    @Test
    @DisplayName("Should pass through LOADING state in map")
    void testMapLoading() {
        ComputedValue<String> loading = ComputedValue.loading();
        ComputedValue<Integer> mapped = loading.map(String::length);

        assertThat(mapped.isLoading()).isTrue();
        assertThat(mapped.isPresent()).isFalse();
    }

    @Test
    @DisplayName("Should pass through ERROR state in map")
    void testMapError() {
        Throwable error = new RuntimeException("Original error");
        ComputedValue<String> errorValue = ComputedValue.error(error);
        ComputedValue<Integer> mapped = errorValue.map(String::length);

        assertThat(mapped.isError()).isTrue();
        assertThat(mapped.getError()).contains(error);
    }

    @Test
    @DisplayName("Should convert to ERROR if mapper throws exception")
    void testMapperException() {
        ComputedValue<String> original = ComputedValue.success("test");
        ComputedValue<Integer> mapped = original.map(s -> {
            throw new IllegalArgumentException("Mapper failed");
        });

        assertThat(mapped.isError()).isTrue();
        assertThat(mapped.getError())
            .isPresent()
            .get()
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(mapped.getError().get().getMessage()).isEqualTo("Mapper failed");
    }

    @Test
    @DisplayName("Should not map null SUCCESS value")
    void testMapNullSuccess() {
        ComputedValue<String> nullValue = ComputedValue.success(null);
        ComputedValue<Integer> mapped = nullValue.map(String::length);

        // Null value in SUCCESS state should not be mapped
        assertThat(mapped.isPresent()).isFalse();
    }

    // ========================================
    // LOADING FALLBACK
    // ========================================

    @Test
    @DisplayName("Should apply loading fallback when in LOADING state")
    void testLoadingFallback() {
        ComputedValue<String> loading = ComputedValue.<String>loading()
            .loading("Loading...");

        assertThat(loading.isPresent()).isTrue();
        assertThat(loading.orElse("other")).isEqualTo("Loading...");
    }

    @Test
    @DisplayName("Should ignore loading fallback when in SUCCESS state")
    void testLoadingFallbackIgnoredForSuccess() {
        ComputedValue<String> success = ComputedValue.success("data")
            .loading("Loading...");

        assertThat(success.orElse("fallback")).isEqualTo("data");
    }

    @Test
    @DisplayName("Should ignore loading fallback when in ERROR state")
    void testLoadingFallbackIgnoredForError() {
        ComputedValue<String> error = ComputedValue.<String>error(new RuntimeException())
            .loading("Loading...");

        assertThat(error.isError()).isTrue();
    }

    // ========================================
    // ERROR FALLBACK
    // ========================================

    @Test
    @DisplayName("Should apply error fallback when in ERROR state")
    void testErrorFallback() {
        ComputedValue<String> error = ComputedValue.<String>error(new RuntimeException("Failed"))
            .error("Error occurred");

        assertThat(error.isPresent()).isTrue();
        assertThat(error.orElse("other")).isEqualTo("Error occurred");
    }

    @Test
    @DisplayName("Should ignore error fallback when in SUCCESS state")
    void testErrorFallbackIgnoredForSuccess() {
        ComputedValue<String> success = ComputedValue.success("data")
            .error("Error occurred");

        assertThat(success.orElse("fallback")).isEqualTo("data");
    }

    @Test
    @DisplayName("Should ignore error fallback when in LOADING state")
    void testErrorFallbackIgnoredForLoading() {
        ComputedValue<String> loading = ComputedValue.<String>loading()
            .error("Error occurred");

        assertThat(loading.isLoading()).isTrue();
    }

    // ========================================
    // OR_ELSE TERMINAL OPERATION
    // ========================================

    @Test
    @DisplayName("Should return value in SUCCESS state with orElse")
    void testOrElseSuccess() {
        String result = ComputedValue.success("value").orElse("fallback");

        assertThat(result).isEqualTo("value");
    }

    @Test
    @DisplayName("Should return fallback in LOADING state with orElse")
    void testOrElseLoading() {
        String result = ComputedValue.<String>loading().orElse("fallback");

        assertThat(result).isEqualTo("fallback");
    }

    @Test
    @DisplayName("Should return fallback in ERROR state with orElse")
    void testOrElseError() {
        String result = ComputedValue.<String>error(new RuntimeException()).orElse("fallback");

        assertThat(result).isEqualTo("fallback");
    }

    @Test
    @DisplayName("Should return fallback for null value in SUCCESS state")
    void testOrElseNullSuccess() {
        String result = ComputedValue.<String>success(null).orElse("fallback");

        assertThat(result).isEqualTo("fallback");
    }

    @Test
    @DisplayName("Should allow null as orElse fallback")
    void testOrElseNullFallback() {
        String result = ComputedValue.<String>loading().orElse(null);

        assertThat(result).isNull();
    }

    // ========================================
    // TO_OPTIONAL CONVERSION
    // ========================================

    @Test
    @DisplayName("Should convert SUCCESS to present Optional")
    void testToOptionalSuccess() {
        Optional<String> optional = ComputedValue.success("value").toOptional();

        assertThat(optional).isPresent().contains("value");
    }

    @Test
    @DisplayName("Should convert LOADING to empty Optional")
    void testToOptionalLoading() {
        Optional<String> optional = ComputedValue.<String>loading().toOptional();

        assertThat(optional).isEmpty();
    }

    @Test
    @DisplayName("Should convert ERROR to empty Optional")
    void testToOptionalError() {
        Optional<String> optional = ComputedValue.<String>error(new RuntimeException()).toOptional();

        assertThat(optional).isEmpty();
    }

    @Test
    @DisplayName("Should convert null SUCCESS to empty Optional")
    void testToOptionalNullSuccess() {
        Optional<String> optional = ComputedValue.<String>success(null).toOptional();

        assertThat(optional).isEmpty();
    }

    // ========================================
    // CHAINING SCENARIOS
    // ========================================

    @Test
    @DisplayName("Should chain map and fallbacks for SUCCESS")
    void testFullChainSuccess() {
        Integer result = ComputedValue.success(10)
            .map(n -> n * 2)
            .loading(-1)
            .error(-1)
            .orElse(0);

        assertThat(result).isEqualTo(20);
    }

    @Test
    @DisplayName("Should chain map and fallbacks for LOADING")
    void testFullChainLoading() {
        String result = ComputedValue.<Integer>loading()
            .map(n -> n * 2)
            .map(n -> "Value: " + n)
            .loading("Loading...")
            .error("Error!")
            .orElse("Unknown");

        assertThat(result).isEqualTo("Loading...");
    }

    @Test
    @DisplayName("Should chain map and fallbacks for ERROR")
    void testFullChainError() {
        String result = ComputedValue.<Integer>error(new RuntimeException())
            .map(n -> n * 2)
            .map(n -> "Value: " + n)
            .loading("Loading...")
            .error("Error!")
            .orElse("Unknown");

        assertThat(result).isEqualTo("Error!");
    }

    @Test
    @DisplayName("Should handle complex transformation chain")
    void testComplexChain() {
        // Simulate fetching user data, extracting email, and formatting
        ComputedValue<String> result = ComputedValue.success("john.doe@example.com")
            .map(email -> email.split("@")[0])  // Get username part
            .map(username -> username.replace(".", " "))  // Replace dots with spaces
            .map(name -> name.substring(0, 1).toUpperCase() + name.substring(1))  // Capitalize
            .loading("Loading user...")
            .error("Unknown user")
            .map(name -> "Hello, " + name + "!");

        assertThat(result.orElse("")).isEqualTo("Hello, John doe!");
    }

    @Test
    @DisplayName("Should handle error in middle of chain")
    void testErrorInMiddleOfChain() {
        ComputedValue<String> result = ComputedValue.success(10)
            .map(n -> n * 2)  // 20
            .map(n -> {
                if (n > 15) throw new IllegalStateException("Too large");
                return n;
            })
            .map(n -> "Value: " + n)  // Should not execute
            .error("Fallback value")
            .loading("Loading...");

        assertThat(result.orElse("Unknown")).isEqualTo("Fallback value");
    }

    // ========================================
    // STATE CHECKS
    // ========================================

    @Test
    @DisplayName("Should correctly report isPresent for all states")
    void testIsPresent() {
        assertThat(ComputedValue.success("value").isPresent()).isTrue();
        assertThat(ComputedValue.success(null).isPresent()).isFalse();
        assertThat(ComputedValue.loading().isPresent()).isFalse();
        assertThat(ComputedValue.error(new RuntimeException()).isPresent()).isFalse();
        assertThat(ComputedValue.empty().isPresent()).isFalse();
    }

    @Test
    @DisplayName("Should correctly report isLoading for all states")
    void testIsLoading() {
        assertThat(ComputedValue.success("value").isLoading()).isFalse();
        assertThat(ComputedValue.loading().isLoading()).isTrue();
        assertThat(ComputedValue.error(new RuntimeException()).isLoading()).isFalse();
        assertThat(ComputedValue.empty().isLoading()).isFalse();
    }

    @Test
    @DisplayName("Should correctly report isError for all states")
    void testIsError() {
        assertThat(ComputedValue.success("value").isError()).isFalse();
        assertThat(ComputedValue.loading().isError()).isFalse();
        assertThat(ComputedValue.error(new RuntimeException()).isError()).isTrue();
        assertThat(ComputedValue.empty().isError()).isFalse();
    }

    // ========================================
    // ERROR RETRIEVAL
    // ========================================

    @Test
    @DisplayName("Should retrieve error from ERROR state")
    void testGetError() {
        Throwable error = new IllegalArgumentException("Test error");
        ComputedValue<String> value = ComputedValue.error(error);

        assertThat(value.getError()).isPresent().contains(error);
    }

    @Test
    @DisplayName("Should return empty Optional for error in SUCCESS state")
    void testGetErrorInSuccessState() {
        assertThat(ComputedValue.success("value").getError()).isEmpty();
    }

    @Test
    @DisplayName("Should return empty Optional for error in LOADING state")
    void testGetErrorInLoadingState() {
        assertThat(ComputedValue.loading().getError()).isEmpty();
    }

    // ========================================
    // EDGE CASES
    // ========================================

    @Test
    @DisplayName("Should handle empty string values")
    void testEmptyStringValue() {
        ComputedValue<String> value = ComputedValue.success("");

        assertThat(value.isPresent()).isTrue();
        assertThat(value.orElse("fallback")).isEqualTo("");
    }

    @Test
    @DisplayName("Should handle zero as valid value")
    void testZeroValue() {
        ComputedValue<Integer> value = ComputedValue.success(0);

        assertThat(value.isPresent()).isTrue();
        assertThat(value.orElse(-1)).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle false as valid value")
    void testFalseValue() {
        ComputedValue<Boolean> value = ComputedValue.success(false);

        assertThat(value.isPresent()).isTrue();
        assertThat(value.orElse(true)).isEqualTo(false);
    }

    @Test
    @DisplayName("Should handle large object transformations")
    void testLargeObjectTransformation() {
        String largeString = "x".repeat(10000);
        ComputedValue<Integer> length = ComputedValue.success(largeString)
            .map(String::length);

        assertThat(length.orElse(0)).isEqualTo(10000);
    }

    @Test
    @DisplayName("Should handle type changes in map chain")
    void testTypeChangesInChain() {
        ComputedValue<String> result = ComputedValue.success(42)
            .map(n -> n * 2)  // Integer -> Integer
            .map(n -> (double) n / 3)  // Integer -> Double
            .map(d -> String.format("%.2f", d));  // Double -> String

        assertThat(result.orElse("")).isEqualTo("28.00");
    }

    // ========================================
    // INTEGRATION WITH ASYNC PATTERNS
    // ========================================

    @Test
    @DisplayName("Should integrate with typical async menu pattern - success case")
    void testAsyncMenuPatternSuccess() {
        // Simulate async data fetch that succeeds
        ComputedValue<String> playerName = ComputedValue.success("Steve");

        String displayName = playerName
            .map(name -> "Player: " + name)
            .loading("Loading player...")
            .error("Unknown player")
            .orElse("No data");

        assertThat(displayName).isEqualTo("Player: Steve");
    }

    @Test
    @DisplayName("Should integrate with typical async menu pattern - loading case")
    void testAsyncMenuPatternLoading() {
        // Simulate async data fetch that is still loading
        ComputedValue<String> playerName = ComputedValue.loading();

        String displayName = playerName
            .map(name -> "Player: " + name)
            .loading("Loading player...")
            .error("Unknown player")
            .orElse("No data");

        assertThat(displayName).isEqualTo("Loading player...");
    }

    @Test
    @DisplayName("Should integrate with typical async menu pattern - error case")
    void testAsyncMenuPatternError() {
        // Simulate async data fetch that failed
        ComputedValue<String> playerName = ComputedValue.error(
            new RuntimeException("Database connection failed")
        );

        String displayName = playerName
            .map(name -> "Player: " + name)
            .loading("Loading player...")
            .error("Unknown player")
            .orElse("No data");

        assertThat(displayName).isEqualTo("Unknown player");
    }

    @Test
    @DisplayName("Should support conditional display based on state")
    void testConditionalDisplay() {
        ComputedValue<Integer> playerCount = ComputedValue.success(5);

        // Conditional formatting
        String display = playerCount
            .map(count -> count + " player" + (count != 1 ? "s" : ""))
            .loading("Counting...")
            .error("Unknown")
            .orElse("No players");

        assertThat(display).isEqualTo("5 players");
    }
}
