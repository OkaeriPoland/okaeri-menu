package eu.okaeri.menu.async;

import lombok.NonNull;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Fluent API for accessing async data with loading/error fallbacks.
 * Inspired by Java Optional and Rust Result.
 * <p>
 * A sealed interface representing async computed data in one of four states:
 * <ul>
 *   <li><b>SUCCESS</b> - Data loaded successfully with a value</li>
 *   <li><b>LOADING</b> - Async operation in progress</li>
 *   <li><b>ERROR</b> - Async operation failed with an error</li>
 *   <li><b>EMPTY</b> - No data loaded, not loading</li>
 * </ul>
 */
public sealed interface Computed<T> permits Computed.Success, Computed.Loading, Computed.Error, Computed.Empty {

    /**
     * Represents successful computation with a value.
     * The value may be null (use {@link #isEmpty()} to check).
     *
     * @param value The successful value (may be null)
     * @param <T>   The value type
     */
    record Success<T>(T value) implements Computed<T> {
    }

    /**
     * Represents ongoing async loading.
     *
     * @param <T> The value type
     */
    record Loading<T>() implements Computed<T> {
    }

    /**
     * Represents failed computation with an error.
     *
     * @param error The error that occurred
     * @param <T>   The value type
     */
    record Error<T>(@NonNull Throwable error) implements Computed<T> {
    }

    /**
     * Represents empty state (no data, not loading).
     *
     * @param <T> The value type
     */
    record Empty<T>() implements Computed<T> {
    }

    // ===== Static Factory Methods =====

    /**
     * Creates a Computed in SUCCESS state.
     *
     * @param value The successful value
     * @param <T>   The value type
     * @return Computed wrapping the value
     */
    @NonNull
    static <T> Computed<T> success(T value) {
        return new Success<>(value);
    }

    /**
     * Creates a Computed in SUCCESS state.
     * Rust-style alias for {@link #success(Object)}.
     *
     * @param value The successful value
     * @param <T>   The value type
     * @return Computed wrapping the value
     */
    @NonNull
    static <T> Computed<T> ok(T value) {
        return new Success<>(value);
    }

    /**
     * Creates a Computed in LOADING state.
     *
     * @param <T> The value type
     * @return Computed in loading state
     */
    @NonNull
    static <T> Computed<T> loading() {
        return new Loading<>();
    }

    /**
     * Creates a Computed in ERROR state.
     *
     * @param error The error that occurred
     * @param <T>   The value type
     * @return Computed in error state
     */
    @NonNull
    static <T> Computed<T> error(Throwable error) {
        return new Error<>(error);
    }

    /**
     * Creates a Computed in ERROR state.
     * Rust-style alias for {@link #error(Throwable)}.
     *
     * @param error The error that occurred
     * @param <T>   The value type
     * @return Computed in error state
     */
    @NonNull
    static <T> Computed<T> err(Throwable error) {
        return new Error<>(error);
    }

    /**
     * Creates an empty Computed (no data, not loading).
     *
     * @param <T> The value type
     * @return Empty Computed
     */
    @NonNull
    static <T> Computed<T> empty() {
        return new Empty<>();
    }

    // ===== Optional-Like API (Value Extraction) =====

    /**
     * Returns the value if present (SUCCESS state), otherwise throws {@link NoSuchElementException}.
     *
     * @return The value if SUCCESS
     * @throws NoSuchElementException if not SUCCESS
     */
    default T get() {
        if (this instanceof Success(var value)) {
            return value;
        }
        if (this instanceof Loading) {
            throw new NoSuchElementException("Data is loading");
        }
        if (this instanceof Error(var err)) {
            throw new NoSuchElementException("Data load failed: " + err.getMessage());
        }
        throw new NoSuchElementException("No data present");
    }

    /**
     * Returns {@code true} if in SUCCESS state with non-null value.
     *
     * @return {@code true} if SUCCESS with non-null value, {@code false} otherwise
     */
    default boolean isPresent() {
        return (this instanceof Success(var value)) && (value != null);
    }

    /**
     * Returns {@code true} if NOT in SUCCESS state.
     *
     * @return {@code true} if LOADING/ERROR/EMPTY, {@code false} if SUCCESS
     */
    default boolean isEmpty() {
        return !(this instanceof Success);
    }

    /**
     * Performs action with value if SUCCESS with non-null value, otherwise does nothing.
     *
     * @param action Action to perform with value
     */
    default void ifPresent(@NonNull Consumer<? super T> action) {
        if ((this instanceof Success(var value)) && (value != null)) {
            action.accept(value);
        }
    }

    /**
     * Performs action if SUCCESS with non-null value, otherwise performs empty action.
     *
     * @param action      Action for SUCCESS state with non-null value
     * @param emptyAction Action for non-SUCCESS states or null value
     */
    default void ifPresentOrElse(@NonNull Consumer<? super T> action, @NonNull Runnable emptyAction) {
        if ((this instanceof Success(var value)) && (value != null)) {
            action.accept(value);
        } else {
            emptyAction.run();
        }
    }

    /**
     * Returns value if SUCCESS and predicate matches, otherwise preserves state or returns EMPTY.
     *
     * @param predicate Predicate to test value
     * @return Value if SUCCESS and matches, otherwise preserved state or EMPTY
     */
    default Computed<T> filter(@NonNull Predicate<? super T> predicate) {
        return switch (this) {
            case Success(var value) -> predicate.test(value) ? this : new Empty<>();
            case Loading() -> this;
            case Error(var err) -> this;
            case Empty() -> this;
        };
    }

    /**
     * Transforms the value if SUCCESS, preserves other states.
     *
     * @param mapper The transformation function
     * @param <U>    The result type
     * @return Transformed value in SUCCESS, or preserved state
     */
    default <U> Computed<U> map(@NonNull Function<? super T, ? extends U> mapper) {
        return switch (this) {
            case Success(var value) -> {
                try {
                    U mapped = mapper.apply(value);
                    yield new Success<>(mapped);
                } catch (Exception ex) {
                    yield new Error<>(ex);
                }
            }
            case Loading() -> new Loading<>();
            case Error(var err) -> new Error<>(err);
            case Empty() -> new Empty<>();
        };
    }

    /**
     * Chains async operations without nesting Computed.
     *
     * @param mapper Function returning another Computed
     * @param <U>    The result type
     * @return Flattened result
     */
    @SuppressWarnings("unchecked")
    default <U> Computed<U> flatMap(@NonNull Function<? super T, ? extends Computed<? extends U>> mapper) {
        return switch (this) {
            case Success(var value) -> {
                try {
                    Computed<? extends U> result = mapper.apply(value);
                    yield (Computed<U>) result;
                } catch (Exception ex) {
                    yield new Error<>(ex);
                }
            }
            case Loading() -> new Loading<>();
            case Error(var err) -> new Error<>(err);
            case Empty() -> new Empty<>();
        };
    }

    /**
     * Returns this if SUCCESS with non-null value, otherwise returns supplied alternative.
     *
     * @param supplier Alternative Computed supplier
     * @return This if SUCCESS with non-null value, otherwise supplied value
     */
    @SuppressWarnings("unchecked")
    default Computed<T> or(@NonNull Supplier<? extends Computed<? extends T>> supplier) {
        if ((this instanceof Success(var value)) && (value != null)) {
            return this;
        }
        return (Computed<T>) supplier.get();
    }

    /**
     * Returns Stream with value if SUCCESS with non-null value, otherwise empty Stream.
     *
     * @return Stream of value or empty
     */
    default Stream<T> stream() {
        if ((this instanceof Success(var value)) && (value != null)) {
            return Stream.of(value);
        }
        return Stream.empty();
    }

    /**
     * Returns value if SUCCESS with non-null value, otherwise returns {@code other}.
     *
     * @param other Value to return if not SUCCESS or null value (may be null)
     * @return Value if SUCCESS with non-null value, otherwise {@code other}
     */
    default T orElse(T other) {
        if (this instanceof Success(var value)) {
            return (value != null) ? value : other;
        }
        return other;
    }

    /**
     * Returns value if SUCCESS with non-null value, otherwise returns result from supplier.
     *
     * @param supplier Supplier for fallback value
     * @return Value if SUCCESS with non-null value, otherwise supplied value
     */
    default T orElseGet(@NonNull Supplier<? extends T> supplier) {
        if (this instanceof Success(var value)) {
            return (value != null) ? value : supplier.get();
        }
        return supplier.get();
    }

    /**
     * Returns value if SUCCESS, otherwise throws {@link NoSuchElementException}.
     *
     * @return Value if SUCCESS
     * @throws NoSuchElementException if not SUCCESS
     */
    default T orElseThrow() {
        return this.get();
    }

    /**
     * Returns value if SUCCESS with non-null value, otherwise throws custom exception.
     *
     * @param exceptionSupplier Supplier for exception to throw
     * @param <X>               Exception type
     * @return Value if SUCCESS with non-null value
     * @throws X if not SUCCESS or null value
     */
    default <X extends Throwable> T orElseThrow(@NonNull Supplier<? extends X> exceptionSupplier) throws X {
        if ((this instanceof Success(var value)) && (value != null)) {
            return value;
        }
        throw exceptionSupplier.get();
    }

    /**
     * Converts to Optional, empty for all non-SUCCESS states.
     * If SUCCESS state contains null value, returns empty Optional.
     *
     * @return Optional of value if SUCCESS, otherwise empty
     */
    default Optional<T> toOptional() {
        if (this instanceof Success(var value)) {
            return Optional.ofNullable(value);
        }
        return Optional.empty();
    }

    /**
     * Returns value if SUCCESS, otherwise returns {@code null}.
     *
     * @return Value if SUCCESS, otherwise {@code null}
     */
    default T orNull() {
        if (this instanceof Success(var value)) {
            return value;
        }
        return null;
    }

    // ===== Async-Specific State Inspection =====

    /**
     * Returns {@code true} if in SUCCESS state.
     *
     * @return {@code true} if SUCCESS, otherwise {@code false}
     */
    default boolean isSuccess() {
        return this instanceof Success;
    }

    /**
     * Returns {@code true} if in LOADING state.
     *
     * @return {@code true} if LOADING, otherwise {@code false}
     */
    default boolean isLoading() {
        return this instanceof Loading;
    }

    /**
     * Returns {@code true} if in ERROR state.
     *
     * @return {@code true} if ERROR, otherwise {@code false}
     */
    default boolean isError() {
        return this instanceof Error;
    }

    /**
     * Returns the error if in ERROR state.
     *
     * @return Optional of error if ERROR, otherwise empty
     */
    default Optional<Throwable> getError() {
        if (this instanceof Error(var err)) {
            return Optional.of(err);
        }
        return Optional.empty();
    }

    // ===== State Normalizers (Convenience) =====

    /**
     * Provides a fallback value for LOADING state.
     * If currently loading, converts to SUCCESS with the fallback; otherwise continues the chain.
     * <p>
     * This is a convenience method for the common pattern of providing loading-specific defaults.
     *
     * @param fallback The loading fallback value
     * @return Computed with loading state normalized to SUCCESS, or unchanged
     */
    default Computed<T> loading(T fallback) {
        if (this instanceof Loading) {
            return new Success<>(fallback);
        }
        return this;
    }

    /**
     * Provides a fallback value for ERROR state.
     * If currently in error, converts to SUCCESS with the fallback; otherwise continues the chain.
     * <p>
     * This is a convenience method for the common pattern of providing error-specific defaults.
     *
     * @param fallback The error fallback value
     * @return Computed with error state normalized to SUCCESS, or unchanged
     */
    default Computed<T> error(T fallback) {
        if (this instanceof Error) {
            return new Success<>(fallback);
        }
        return this;
    }

    // ===== Side Effects (Groovy-Inspired) =====

    /**
     * Executes action with this Computed instance, returns this unchanged.
     * Inspired by Groovy's tap() - execute side effect and continue chaining.
     * <p>
     * The action receives this Computed instance, allowing you to inspect state,
     * pattern match, log, etc. without consuming or transforming the value.
     *
     * <p>Example:
     * <pre>{@code
     * computed("stats")
     *     .tap(self -> switch (self) {
     *         case Success(var stats) -> log.info("Loaded: {}", stats);
     *         case Loading() -> log.debug("Loading...");
     *         case Error(var e) -> log.error("Failed", e);
     *         case Empty() -> log.warn("Empty");
     *     })
     *     .map(Stats::getLevel)
     *     .orElse(0);
     * }</pre>
     *
     * @param action Action to execute with this Computed instance
     * @return This Computed unchanged (for chaining)
     */
    default Computed<T> tap(@NonNull Consumer<? super Computed<T>> action) {
        action.accept(this);
        return this;
    }

    /**
     * Executes action with value if SUCCESS, returns this unchanged.
     * Chainable side effect for success state.
     *
     * @param action Action to execute with value if SUCCESS with non-null value
     * @return This Computed unchanged (for chaining)
     */
    default Computed<T> onSuccess(@NonNull Consumer<? super T> action) {
        if ((this instanceof Success(var value)) && (value != null)) {
            action.accept(value);
        }
        return this;
    }

    /**
     * Executes action if LOADING, returns this unchanged.
     * Chainable side effect for loading state.
     *
     * @param action Action to execute if LOADING
     * @return This Computed unchanged (for chaining)
     */
    default Computed<T> onLoading(@NonNull Runnable action) {
        if (this instanceof Loading) {
            action.run();
        }
        return this;
    }

    /**
     * Executes action with error if ERROR, returns this unchanged.
     * Chainable side effect for error state.
     *
     * @param action Action to execute with error if ERROR
     * @return This Computed unchanged (for chaining)
     */
    default Computed<T> onError(@NonNull Consumer<Throwable> action) {
        if (this instanceof Error(var err)) {
            action.accept(err);
        }
        return this;
    }

    /**
     * Executes action if EMPTY, returns this unchanged.
     * Chainable side effect for empty state.
     *
     * @param action Action to execute if EMPTY
     * @return This Computed unchanged (for chaining)
     */
    default Computed<T> onEmpty(@NonNull Runnable action) {
        if (this instanceof Empty) {
            action.run();
        }
        return this;
    }
}
