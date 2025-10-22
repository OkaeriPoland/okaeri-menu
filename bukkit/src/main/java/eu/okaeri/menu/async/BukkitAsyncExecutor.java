package eu.okaeri.menu.async;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Default async executor using Bukkit's scheduler.
 * Integrates with Bukkit plugin lifecycle (auto-cancels on disable).
 */
@RequiredArgsConstructor
public class BukkitAsyncExecutor implements AsyncExecutor {

    @NonNull
    private final Plugin plugin;

    @Override
    public <T> CompletableFuture<T> execute(@NonNull Supplier<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            try {
                T result = task.get();
                future.complete(result);
            } catch (Throwable ex) {
                future.completeExceptionally(ex);
            }
        });

        return future;
    }
}
