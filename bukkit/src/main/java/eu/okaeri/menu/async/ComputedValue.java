package eu.okaeri.menu.async;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.function.Function;

/**
 * Fluent API for accessing async data with loading/error fallbacks.
 * Inspired by Java Optional and Rust Result.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ComputedValue<T> {

    private final T value;
    private final AsyncCache.AsyncState state;
    private final Throwable error;

    /**
     * Creates a ComputedValue in SUCCESS state.
     *
     * @param value The successful value
     * @param <T>   The value type
     * @return ComputedValue wrapping the value
     */
    @NonNull
    public static <T> ComputedValue<T> success(T value) {
        return new ComputedValue<>(value, AsyncCache.AsyncState.SUCCESS, null);
    }

    /**
     * Creates a ComputedValue in LOADING state.
     *
     * @param <T> The value type
     * @return ComputedValue in loading state
     */
    @NonNull
    public static <T> ComputedValue<T> loading() {
        return new ComputedValue<>(null, AsyncCache.AsyncState.LOADING, null);
    }

    /**
     * Creates a ComputedValue in ERROR state.
     *
     * @param error The error that occurred
     * @param <T>   The value type
     * @return ComputedValue in error state
     */
    @NonNull
    public static <T> ComputedValue<T> error(Throwable error) {
        return new ComputedValue<>(null, AsyncCache.AsyncState.ERROR, error);
    }

    /**
     * Creates an empty ComputedValue (no data, not loading).
     *
     * @param <T> The value type
     * @return Empty ComputedValue
     */
    @NonNull
    public static <T> ComputedValue<T> empty() {
        return new ComputedValue<>(null, null, null);
    }

    /**
     * Maps the value if in SUCCESS state.
     * Loading/error states pass through unchanged.
     *
     * @param mapper The mapping function
     * @param <R>    The result type
     * @return Mapped ComputedValue
     */
    @NonNull
    public <R> ComputedValue<R> map(@NonNull Function<T, R> mapper) {
        if ((this.state == AsyncCache.AsyncState.SUCCESS) && (this.value != null)) {
            try {
                R mapped = mapper.apply(this.value);
                return new ComputedValue<>(mapped, AsyncCache.AsyncState.SUCCESS, null);
            } catch (Exception ex) {
                return new ComputedValue<>(null, AsyncCache.AsyncState.ERROR, ex);
            }
        }

        // Pass through other states
        return new ComputedValue<>(null, this.state, this.error);
    }

    /**
     * Provides a fallback value for LOADING state.
     * If currently loading, returns the fallback; otherwise continues the chain.
     *
     * @param fallback The loading fallback value
     * @return ComputedValue with loading fallback applied
     */
    @NonNull
    public ComputedValue<T> loading(T fallback) {
        if (this.state == AsyncCache.AsyncState.LOADING) {
            return new ComputedValue<>(fallback, AsyncCache.AsyncState.SUCCESS, null);
        }
        return this;
    }

    /**
     * Provides a fallback value for ERROR state.
     * If currently in error, returns the fallback; otherwise continues the chain.
     *
     * @param fallback The error fallback value
     * @return ComputedValue with error fallback applied
     */
    @NonNull
    public ComputedValue<T> error(T fallback) {
        if (this.state == AsyncCache.AsyncState.ERROR) {
            return new ComputedValue<>(fallback, AsyncCache.AsyncState.SUCCESS, null);
        }
        return this;
    }

    /**
     * Gets the value or returns the fallback.
     * This is the terminal operation that unwraps the ComputedValue.
     *
     * @param fallback The fallback value
     * @return The computed value or fallback
     */
    public T orElse(T fallback) {
        if ((this.state == AsyncCache.AsyncState.SUCCESS) && (this.value != null)) {
            return this.value;
        }
        return fallback;
    }

    /**
     * Converts to Optional (empty if loading/error/null).
     *
     * @return Optional of the value
     */
    @NonNull
    public Optional<T> toOptional() {
        if (this.state == AsyncCache.AsyncState.SUCCESS) {
            return Optional.ofNullable(this.value);
        }
        return Optional.empty();
    }

    /**
     * Checks if this value is in SUCCESS state with a non-null value.
     *
     * @return true if present
     */
    public boolean isPresent() {
        return (this.state == AsyncCache.AsyncState.SUCCESS) && (this.value != null);
    }

    /**
     * Checks if this value is in LOADING state.
     *
     * @return true if loading
     */
    public boolean isLoading() {
        return this.state == AsyncCache.AsyncState.LOADING;
    }

    /**
     * Checks if this value is in ERROR state.
     *
     * @return true if error
     */
    public boolean isError() {
        return this.state == AsyncCache.AsyncState.ERROR;
    }

    /**
     * Gets the error if in ERROR state.
     *
     * @return Optional of the error
     */
    @NonNull
    public Optional<Throwable> getError() {
        return Optional.ofNullable(this.error);
    }
}
