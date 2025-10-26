package eu.okaeri.menu.async;

import lombok.Getter;
import lombok.NonNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Per-viewer cache for async data with TTL support.
 * Thread-safe for concurrent access from async threads and server thread.
 */
public class AsyncCache {

    private @NonNull final AsyncExecutor executor;
    private final eu.okaeri.menu.state.ViewerState viewerState;  // Optional - for invalidation tracking
    private final Map<String, CacheEntry<?>> cache = new ConcurrentHashMap<>();

    public AsyncCache(@NonNull AsyncExecutor executor) {
        this.executor = executor;
        this.viewerState = null;
    }

    public AsyncCache(@NonNull AsyncExecutor executor, eu.okaeri.menu.state.ViewerState viewerState) {
        this.executor = executor;
        this.viewerState = viewerState;
    }

    /**
     * Gets cached value if available.
     * Returns stale values (expired but not yet reloaded) to support stale-while-revalidate pattern.
     *
     * @param key  The cache key
     * @param type The expected type
     * @param <T>  The value type
     * @return Optional of the cached value (may be stale if reloading)
     */
    @NonNull
    public <T> Optional<T> get(@NonNull String key, @NonNull Class<T> type) {
        CacheEntry<?> entry = this.cache.get(key);
        if ((entry == null) || (entry.state != AsyncState.SUCCESS)) {
            return Optional.empty();
        }

        // Return value even if expired (stale-while-revalidate)
        // The value will be updated once background reload completes
        try {
            return Optional.of(type.cast(entry.value));
        } catch (ClassCastException ex) {
            return Optional.empty();
        }
    }

    /**
     * Gets the current state of a cache key.
     * Returns SUCCESS for stale values (supports stale-while-revalidate pattern).
     *
     * @param key The cache key
     * @return The current state, or null if not found
     */
    public AsyncState getState(@NonNull String key) {
        CacheEntry<?> entry = this.cache.get(key);
        if (entry == null) {
            return null;
        }

        // Return state even if expired (stale-while-revalidate)
        // SUCCESS state will be returned for stale data while background reload is in progress
        return entry.state;
    }

    /**
     * Gets the error for a failed cache key.
     *
     * @param key The cache key
     * @return Optional of the error
     */
    @NonNull
    public Optional<Throwable> getError(@NonNull String key) {
        CacheEntry<?> entry = this.cache.get(key);
        if ((entry == null) || (entry.state != AsyncState.ERROR)) {
            return Optional.empty();
        }
        return Optional.ofNullable(entry.error);
    }

    /**
     * Checks if a cache entry is expired based on TTL.
     *
     * @param key The cache key
     * @return true if expired or not found
     */
    public boolean isExpired(@NonNull String key) {
        CacheEntry<?> entry = this.cache.get(key);
        return (entry == null) || this.isExpired(entry);
    }

    private boolean isExpired(@NonNull CacheEntry<?> entry) {
        if ((entry.ttl == null) || (entry.loadedAt == null)) {
            return false;  // No TTL = never expires
        }
        Instant expiryTime = entry.loadedAt.plus(entry.ttl);
        return Instant.now().isAfter(expiryTime);
    }

    /**
     * Puts a value into the cache with TTL.
     * Clears any background reload future since fresh data is now available.
     * Marks ViewerState as dirty for menus with update intervals.
     *
     * @param key   The cache key
     * @param value The value to cache
     * @param ttl   Time-to-live
     * @param <T>   The value type
     */
    public <T> void put(@NonNull String key, @NonNull T value, @NonNull Duration ttl) {
        CacheEntry<T> entry = new CacheEntry<>();
        entry.value = value;
        entry.loadedAt = Instant.now();
        entry.ttl = ttl;
        entry.state = AsyncState.SUCCESS;
        entry.loadingFuture = null;  // Clear reload future - fresh data now available
        this.cache.put(key, entry);

        // Mark viewer dirty for update interval refresh
        if (this.viewerState != null) {
            this.viewerState.invalidate();
        }
    }

    /**
     * Sets an error state for a cache key.
     * Marks ViewerState as dirty for menus with update intervals.
     *
     * @param key   The cache key
     * @param error The error that occurred
     */
    public void setError(@NonNull String key, @NonNull Throwable error) {
        CacheEntry<Object> entry = new CacheEntry<>();
        entry.error = error;
        entry.state = AsyncState.ERROR;
        this.cache.put(key, entry);

        // Mark viewer dirty for update interval refresh
        if (this.viewerState != null) {
            this.viewerState.invalidate();
        }
    }

    /**
     * Invalidates a cache entry, forcing reload on next access.
     * Uses stale-while-revalidate: keeps showing old value while reloading in background.
     * Entry is marked as expired by backdating loadedAt timestamp.
     * Marks ViewerState as dirty for immediate refresh by MenuUpdateTask.
     *
     * @param key The cache key
     */
    public void invalidate(@NonNull String key) {
        CacheEntry<?> entry = this.cache.get(key);
        if ((entry != null) && (entry.state == AsyncState.SUCCESS) && (entry.ttl != null)) {
            // Set loadedAt so entry is expired NOW, regardless of TTL
            // Math: expiryTime = loadedAt + ttl, so loadedAt = now - ttl - 1s
            entry.loadedAt = Instant.now().minus(entry.ttl).minusSeconds(1);
        }

        // Mark viewer dirty for immediate refresh
        if (this.viewerState != null) {
            this.viewerState.invalidate();
        }
    }

    /**
     * Invalidates all cached entries, forcing reload on next access.
     * Uses stale-while-revalidate: keeps showing old values while reloading in background.
     * Marks ViewerState as dirty for immediate refresh by MenuUpdateTask.
     */
    public void invalidateAll() {
        this.cache.values().stream()
            .filter(entry -> (entry.state == AsyncState.SUCCESS) && (entry.ttl != null))
            .forEach(entry -> entry.loadedAt = Instant.now().minus(entry.ttl).minusSeconds(1));

        // Mark viewer dirty for immediate refresh
        if (this.viewerState != null) {
            this.viewerState.invalidate();
        }
    }

    /**
     * Gets an existing loading future or starts a new load.
     * Prevents duplicate loads for the same key.
     *
     * @param key    The cache key
     * @param loader The data loader
     * @param ttl    Time-to-live for cached data
     * @param <T>    The result type
     * @return CompletableFuture for the load operation
     */
    @NonNull
    public <T> CompletableFuture<T> getOrStartLoad(@NonNull String key, @NonNull Supplier<T> loader, @NonNull Duration ttl) {
        // Use compute to atomically check-and-create to prevent race conditions
        @SuppressWarnings("unchecked")
        CacheEntry<T> resultEntry = (CacheEntry<T>) this.cache.compute(key, (k, existing) -> {
            // Check if already loading with a valid future
            if ((existing != null) && (existing.loadingFuture != null) && !existing.loadingFuture.isDone()) {
                return existing;  // Reuse existing entry with in-progress future
            }

            // Check if we have a valid SUCCESS entry (not expired)
            if ((existing != null) && (existing.state == AsyncState.SUCCESS) && !this.isExpired(existing)) {
                // Fresh data - return as-is
                return existing;
            }

            // Handle stale or missing data
            if ((existing != null) && (existing.state == AsyncState.SUCCESS) && this.isExpired(existing)) {
                // STALE-WHILE-REVALIDATE: Keep existing value, start background reload
                @SuppressWarnings("unchecked")
                CacheEntry<T> existingTyped = (CacheEntry<T>) existing;
                existingTyped.loadingFuture = this.executor.execute(loader); // Attach reload future
                // Keep state as SUCCESS and keep existing value
                return existingTyped;
            }

            // No existing data (first load) or ERROR state - create new LOADING entry
            CompletableFuture<T> future = this.executor.execute(loader);
            CacheEntry<T> entry = new CacheEntry<>();
            entry.state = AsyncState.LOADING;
            entry.loadingFuture = future;
            entry.ttl = ttl;
            return entry;
        });

        // Attach completion handler AFTER compute() to avoid deadlock
        // Only attach for actual loads (has loadingFuture), not cache hits
        if (resultEntry.loadingFuture != null) {
            resultEntry.loadingFuture.whenComplete((value, error) -> {
                if (error != null) {
                    this.setError(key, error);
                } else {
                    this.put(key, value, ttl);
                }
            });
        }

        // Return appropriate future based on state
        if ((resultEntry.state == AsyncState.SUCCESS) && (resultEntry.loadingFuture == null)) {
            // SUCCESS entry without reload - create completed future with cached value
            @SuppressWarnings("unchecked")
            T cachedValue = resultEntry.value;
            return CompletableFuture.completedFuture(cachedValue);
        } else if (resultEntry.state == AsyncState.SUCCESS) {
            // SUCCESS entry with background reload - return reload future
            // The current value is stale but still being used
            @SuppressWarnings("unchecked")
            CompletableFuture<T> future = resultEntry.loadingFuture;
            return future;
        } else {
            // LOADING entry (first load) - return the future
            @SuppressWarnings("unchecked")
            CompletableFuture<T> future = resultEntry.loadingFuture;
            return future;
        }
    }

    /**
     * Checks if any cached entries have expired TTLs.
     * Used by MenuUpdateTask to trigger refreshes for reactive data.
     *
     * @return true if any SUCCESS entries are expired
     */
    public boolean hasExpiredEntries() {
        return this.cache.values().stream()
            .anyMatch(entry -> (entry.state == AsyncState.SUCCESS) && this.isExpired(entry));
    }

    /**
     * Clears all cached data.
     * Called when menu closes for viewer.
     */
    public void clear() {
        this.cache.clear();
    }

    /**
     * Cache entry with state, value, and metadata.
     *
     * @param <T> The value type
     */
    @Getter
    static class CacheEntry<T> {
        T value;
        Instant loadedAt;
        Duration ttl;
        CompletableFuture<T> loadingFuture;
        AsyncState state;
        Throwable error;
    }

    /**
     * State of an async cache entry.
     */
    public enum AsyncState {
        /**
         * Future in progress
         */
        LOADING,
        /**
         * Value available
         */
        SUCCESS,
        /**
         * Exception thrown
         */
        ERROR
    }
}
