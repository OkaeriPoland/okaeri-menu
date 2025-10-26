package eu.okaeri.menu.bukkit.unit.async;

import eu.okaeri.menu.async.AsyncCache;
import eu.okaeri.menu.async.AsyncExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AsyncCache.
 * Tests TTL expiration, state transitions, thread safety, and cache operations.
 */
class AsyncCacheTest {

    private AsyncExecutor mockExecutor;
    private AsyncCache cache;

    @BeforeEach
    void setUp() {
        this.mockExecutor = mock(AsyncExecutor.class);
        this.cache = new AsyncCache(this.mockExecutor);
    }

    // ========================================
    // BASIC OPERATIONS
    // ========================================

    @Test
    @DisplayName("Should store and retrieve values")
    void testBasicPutAndGet() {
        this.cache.put("key1", "value1", Duration.ofSeconds(30));

        Optional<String> result = this.cache.get("key1", String.class);
        assertThat(result).isPresent().contains("value1");
    }

    @Test
    @DisplayName("Should return empty for non-existent keys")
    void testGetNonExistentKey() {
        Optional<String> result = this.cache.get("nonexistent", String.class);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should reject null values")
    void testNullValue() {
        // AsyncCache uses @NonNull, so null values should throw NPE
        assertThatThrownBy(() ->
            this.cache.put("null-key", null, Duration.ofSeconds(30))
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should store different types")
    void testDifferentTypes() {
        this.cache.put("string", "text", Duration.ofSeconds(30));
        this.cache.put("integer", 42, Duration.ofSeconds(30));
        this.cache.put("double", 3.14, Duration.ofSeconds(30));

        assertThat(this.cache.get("string", String.class)).contains("text");
        assertThat(this.cache.get("integer", Integer.class)).contains(42);
        assertThat(this.cache.get("double", Double.class)).contains(3.14);
    }

    @Test
    @DisplayName("Should overwrite existing values")
    void testOverwrite() {
        this.cache.put("key", "old-value", Duration.ofSeconds(30));
        this.cache.put("key", "new-value", Duration.ofSeconds(30));

        Optional<String> result = this.cache.get("key", String.class);
        assertThat(result).contains("new-value");
    }

    // ========================================
    // STATE MANAGEMENT
    // ========================================

    @Test
    @DisplayName("Should track LOADING state")
    void testLoadingState() {
        CompletableFuture<String> future = new CompletableFuture<>();
        when(this.mockExecutor.execute(any())).thenAnswer(inv -> future);

        this.cache.getOrStartLoad("key", () -> "value", Duration.ofSeconds(30));

        assertThat(this.cache.getState("key")).isEqualTo(AsyncCache.AsyncState.LOADING);
    }

    @Test
    @DisplayName("Should transition to SUCCESS state")
    void testSuccessState() {
        this.cache.put("key", "value", Duration.ofSeconds(30));

        assertThat(this.cache.getState("key")).isEqualTo(AsyncCache.AsyncState.SUCCESS);
    }

    @Test
    @DisplayName("Should transition to ERROR state")
    void testErrorState() {
        Throwable error = new RuntimeException("Test error");
        this.cache.setError("key", error);

        assertThat(this.cache.getState("key")).isEqualTo(AsyncCache.AsyncState.ERROR);
        assertThat(this.cache.getError("key")).contains(error);
    }

    @Test
    @DisplayName("Should return null state for non-existent key")
    void testNonExistentKeyState() {
        assertThat(this.cache.getState("nonexistent")).isNull();
    }

    @Test
    @DisplayName("Should store and retrieve error")
    void testErrorStorage() {
        Throwable error = new IllegalArgumentException("Invalid argument");
        this.cache.setError("error-key", error);

        Optional<Throwable> result = this.cache.getError("error-key");
        assertThat(result).isPresent().contains(error);
    }

    // ========================================
    // TTL & EXPIRATION
    // ========================================

    @Test
    @DisplayName("Should not expire within TTL")
    void testNotExpiredWithinTTL() {
        this.cache.put("key", "value", Duration.ofSeconds(30));

        assertThat(this.cache.isExpired("key")).isFalse();
    }

    @Test
    @DisplayName("Should expire after TTL")
    void testExpiredAfterTTL() throws InterruptedException {
        this.cache.put("key", "value", Duration.ofMillis(50));

        // Wait for expiration
        Thread.sleep(100);

        assertThat(this.cache.isExpired("key")).isTrue();
    }

    @Test
    @DisplayName("Should treat non-existent keys as expired")
    void testNonExistentKeyIsExpired() {
        // Non-existent keys are considered "expired" (not in cache)
        assertThat(this.cache.isExpired("nonexistent")).isTrue();
    }

    @Test
    @DisplayName("Should treat zero TTL as immediately expired")
    void testZeroTTL() {
        this.cache.put("key", "value", Duration.ZERO);

        // Duration.ZERO means immediately expired
        assertThat(this.cache.isExpired("key")).isTrue();
        // Expired entries are removed on access, so getState() may return null
        // This is expected behavior for zero-TTL entries
        AsyncCache.AsyncState state = this.cache.getState("key");
        assertThat((state == null) || (state == AsyncCache.AsyncState.SUCCESS)).isTrue();
    }

    // ========================================
    // INVALIDATION
    // ========================================

    @Test
    @DisplayName("Should invalidate single key (stale-while-revalidate)")
    void testInvalidateSingleKey() {
        this.cache.put("key1", "value1", Duration.ofSeconds(30));
        this.cache.put("key2", "value2", Duration.ofSeconds(30));

        this.cache.invalidate("key1");

        // Stale-while-revalidate: value still present but marked as expired
        assertThat(this.cache.get("key1", String.class)).contains("value1");
        assertThat(this.cache.isExpired("key1")).isTrue();

        // Other key unaffected
        assertThat(this.cache.get("key2", String.class)).contains("value2");
        assertThat(this.cache.isExpired("key2")).isFalse();
    }

    @Test
    @DisplayName("Should invalidate non-existent key without error")
    void testInvalidateNonExistentKey() {
        assertThatCode(() -> this.cache.invalidate("nonexistent"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should clear all entries")
    void testClearAll() {
        this.cache.put("key1", "value1", Duration.ofSeconds(30));
        this.cache.put("key2", "value2", Duration.ofSeconds(30));

        this.cache.clear();

        assertThat(this.cache.get("key1", String.class)).isEmpty();
        assertThat(this.cache.get("key2", String.class)).isEmpty();
    }

    // ========================================
    // GET_OR_START_LOAD
    // ========================================

    @Test
    @DisplayName("Should start load for new key")
    void testGetOrStartLoadNewKey() {
        AtomicInteger callCount = new AtomicInteger(0);
        CompletableFuture<String> future = CompletableFuture.completedFuture("value");

        when(this.mockExecutor.execute(any())).thenAnswer(inv -> future);

        CompletableFuture<String> result = this.cache.getOrStartLoad(
            "key",
            () -> {
                callCount.incrementAndGet();
                return "value";
            },
            Duration.ofSeconds(30)
        );

        assertThat(result).isNotNull();
        verify(this.mockExecutor, times(1)).execute(any());
    }

    @Test
    @DisplayName("Should not start duplicate load for same key")
    void testGetOrStartLoadDuplicatePrevention() {
        CompletableFuture<String> future = new CompletableFuture<>();
        when(this.mockExecutor.execute(any())).thenAnswer(inv -> future);

        // Start first load
        CompletableFuture<String> first = this.cache.getOrStartLoad(
            "key",
            () -> "value",
            Duration.ofSeconds(30)
        );

        // Try to start second load while first is still loading
        CompletableFuture<String> second = this.cache.getOrStartLoad(
            "key",
            () -> "value",
            Duration.ofSeconds(30)
        );

        // Should return same future
        assertThat(first).isSameAs(second);

        // Executor should only be called once
        verify(this.mockExecutor, times(1)).execute(any());
    }

    @Test
    @DisplayName("Should restart load after expiration")
    void testGetOrStartLoadAfterExpiration() throws InterruptedException {
        AtomicInteger callCount = new AtomicInteger(0);
        when(this.mockExecutor.execute(any())).thenAnswer(inv -> {
            callCount.incrementAndGet();
            return CompletableFuture.completedFuture("value");
        });

        // First load with short TTL
        this.cache.getOrStartLoad("key", () -> "value", Duration.ofMillis(50));
        // Simulate completion by putting the value (in real usage, this happens in whenComplete callback)
        this.cache.put("key", "value", Duration.ofMillis(50));

        int firstCount = callCount.get();

        // Wait for expiration
        Thread.sleep(100);

        // Second load should execute after expiration
        this.cache.getOrStartLoad("key", () -> "value2", Duration.ofSeconds(30));

        // Should have more calls after the second getOrStartLoad
        assertThat(callCount.get()).isGreaterThan(firstCount);
    }

    // ========================================
    // THREAD SAFETY
    // ========================================

    @Test
    @DisplayName("Should handle concurrent get operations")
    void testConcurrentGet() throws InterruptedException {
        this.cache.put("key", "value", Duration.ofSeconds(30));

        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    Optional<String> result = this.cache.get("key", String.class);
                    if (result.isPresent() && "value".equals(result.get())) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(successCount.get()).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("Should handle concurrent put operations")
    void testConcurrentPut() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            int index = i;
            new Thread(() -> {
                try {
                    this.cache.put("key-" + index, "value-" + index, Duration.ofSeconds(30));
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

        // Verify all values were stored
        for (int i = 0; i < threadCount; i++) {
            assertThat(this.cache.get("key-" + i, String.class)).contains("value-" + i);
        }
    }

    @Test
    @DisplayName("Should handle concurrent getOrStartLoad")
    void testConcurrentGetOrStartLoad() throws InterruptedException {
        CompletableFuture<String> future = new CompletableFuture<>();
        when(this.mockExecutor.execute(any())).thenAnswer(inv -> future);

        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    this.cache.getOrStartLoad("key", () -> "value", Duration.ofSeconds(30));
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

        // With atomic compute(), executor should be called exactly once for all concurrent requests
        // All threads should get the same future for the same key
        verify(this.mockExecutor, times(1)).execute(any());
    }

    // ========================================
    // EDGE CASES
    // ========================================

    @Test
    @DisplayName("Should handle very short TTL")
    void testVeryShortTTL() throws InterruptedException {
        this.cache.put("key", "value", Duration.ofMillis(1));

        // Wait briefly
        Thread.sleep(10);

        assertThat(this.cache.isExpired("key")).isTrue();
    }

    @Test
    @DisplayName("Should handle very long TTL")
    void testVeryLongTTL() {
        this.cache.put("key", "value", Duration.ofDays(365));

        assertThat(this.cache.isExpired("key")).isFalse();
        assertThat(this.cache.get("key", String.class)).contains("value");
    }

    @Test
    @DisplayName("Should handle empty string keys")
    void testEmptyStringKey() {
        this.cache.put("", "value", Duration.ofSeconds(30));

        assertThat(this.cache.get("", String.class)).contains("value");
    }

    @Test
    @DisplayName("Should handle special characters in keys")
    void testSpecialCharacterKeys() {
        String key = "key:with:special!@#$%characters";
        this.cache.put(key, "value", Duration.ofSeconds(30));

        assertThat(this.cache.get(key, String.class)).contains("value");
    }

    @Test
    @DisplayName("Should handle large values")
    void testLargeValue() {
        String largeValue = "x".repeat(10000);
        this.cache.put("large", largeValue, Duration.ofSeconds(30));

        assertThat(this.cache.get("large", String.class)).contains(largeValue);
    }

    // ========================================
    // INTEGRATION SCENARIOS
    // ========================================

    @Test
    @DisplayName("Should complete full load lifecycle")
    void testFullLoadLifecycle() {
        CompletableFuture<String> future = new CompletableFuture<>();
        when(this.mockExecutor.execute(any())).thenAnswer(inv -> future);

        // Start load
        CompletableFuture<String> result = this.cache.getOrStartLoad(
            "key",
            () -> "value",
            Duration.ofSeconds(30)
        );

        // Initially LOADING
        assertThat(this.cache.getState("key")).isEqualTo(AsyncCache.AsyncState.LOADING);

        // Complete future
        this.cache.put("key", "value", Duration.ofSeconds(30));

        // Now SUCCESS
        assertThat(this.cache.getState("key")).isEqualTo(AsyncCache.AsyncState.SUCCESS);
        assertThat(this.cache.get("key", String.class)).contains("value");
    }

    @Test
    @DisplayName("Should handle load error lifecycle")
    void testLoadErrorLifecycle() {
        CompletableFuture<String> future = new CompletableFuture<>();
        when(this.mockExecutor.execute(any())).thenAnswer(inv -> future);

        // Start load
        this.cache.getOrStartLoad("key", () -> "value", Duration.ofSeconds(30));

        // Initially LOADING
        assertThat(this.cache.getState("key")).isEqualTo(AsyncCache.AsyncState.LOADING);

        // Set error
        Throwable error = new RuntimeException("Load failed");
        this.cache.setError("key", error);

        // Now ERROR
        assertThat(this.cache.getState("key")).isEqualTo(AsyncCache.AsyncState.ERROR);
        assertThat(this.cache.getError("key")).contains(error);
    }

    @Test
    @DisplayName("Should allow retry after error")
    void testRetryAfterError() {
        // First attempt fails
        Throwable error = new RuntimeException("First attempt failed");
        this.cache.setError("key", error);
        assertThat(this.cache.getState("key")).isEqualTo(AsyncCache.AsyncState.ERROR);

        // Invalidate to allow retry
        this.cache.invalidate("key");

        // Second attempt succeeds
        this.cache.put("key", "value", Duration.ofSeconds(30));
        assertThat(this.cache.getState("key")).isEqualTo(AsyncCache.AsyncState.SUCCESS);
        assertThat(this.cache.get("key", String.class)).contains("value");
    }
}
