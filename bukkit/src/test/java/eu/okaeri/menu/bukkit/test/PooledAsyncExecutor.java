package eu.okaeri.menu.bukkit.test;

import eu.okaeri.menu.async.AsyncExecutor;
import lombok.NonNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Thread pool-based AsyncExecutor for testing real async behavior.
 * Uses a single-threaded executor for deterministic execution order.
 * <p>
 * Use this when tests need to verify:
 * - Loading states (suspense UI)
 * - Async transitions (loading → success)
 * - Time-based behavior (TTL expiration)
 * <p>
 * Use SyncTestExecutor when tests only care about final state.
 */
public class PooledAsyncExecutor implements AsyncExecutor {

    private final ExecutorService executor;

    private PooledAsyncExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    /**
     * Creates a single-threaded pooled executor for deterministic async execution.
     */
    public static PooledAsyncExecutor create() {
        return new PooledAsyncExecutor(Executors.newSingleThreadExecutor());
    }

    /**
     * Creates a multi-threaded pooled executor for concurrent async execution.
     *
     * @param threads number of threads in the pool
     */
    public static PooledAsyncExecutor create(int threads) {
        return new PooledAsyncExecutor(Executors.newFixedThreadPool(threads));
    }

    @Override
    public <T> CompletableFuture<T> execute(@NonNull Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, this.executor);
    }

    /**
     * Shuts down the executor and waits for tasks to complete.
     * Call this in @AfterAll or @AfterEach to clean up resources.
     */
    public void shutdown() {
        this.executor.shutdown();
        try {
            if (!this.executor.awaitTermination(5, TimeUnit.SECONDS)) {
                this.executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            this.executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Immediately shuts down the executor without waiting.
     */
    public void shutdownNow() {
        this.executor.shutdownNow();
    }
}
