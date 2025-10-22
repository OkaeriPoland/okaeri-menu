package eu.okaeri.menu.bukkit.test;

import eu.okaeri.menu.async.AsyncExecutor;
import lombok.NonNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Synchronous AsyncExecutor for deterministic testing.
 * Executes tasks immediately on the calling thread instead of async.
 * Eliminates all timing issues and Thread.sleep() in tests.
 */
public class SyncTestExecutor implements AsyncExecutor {

    /**
     * Executes the task synchronously and returns completed future.
     */
    @Override
    public <T> CompletableFuture<T> execute(@NonNull Supplier<T> task) {
        try {
            T result = task.get();
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            CompletableFuture<T> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * Creates a new synchronous test executor.
     */
    public static SyncTestExecutor create() {
        return new SyncTestExecutor();
    }
}
