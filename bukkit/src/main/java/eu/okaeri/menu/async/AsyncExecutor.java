package eu.okaeri.menu.async;

import lombok.NonNull;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Provider for async task execution.
 * Allows custom implementations while defaulting to Bukkit scheduler.
 */
@FunctionalInterface
public interface AsyncExecutor {

    /**
     * Executes a task asynchronously.
     *
     * @param task The task to execute
     * @param <T>  The result type
     * @return CompletableFuture for the result
     */
    <T> CompletableFuture<T> execute(@NonNull Supplier<T> task);

    /**
     * Creates default executor using Bukkit scheduler.
     * Tasks run via plugin.getServer().getScheduler().runTaskAsynchronously().
     *
     * @param plugin The plugin instance
     * @return AsyncExecutor using Bukkit scheduler
     */
    @NonNull
    static AsyncExecutor bukkit(@NonNull Plugin plugin) {
        return new BukkitAsyncExecutor(plugin);
    }
}
