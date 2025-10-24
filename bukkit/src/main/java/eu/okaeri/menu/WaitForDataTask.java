package eu.okaeri.menu;

import eu.okaeri.menu.async.AsyncCache;
import eu.okaeri.menu.state.ViewerState;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Polls async cache until data is ready, then opens the menu.
 * Runs on main thread using Bukkit scheduler with tick-based polling.
 * <p>
 * Polling strategy:
 * - Check every tick (50ms) for data readiness
 * - Open menu when ALL async components are SUCCESS or ERROR
 * - Open menu anyway after timeout expires (fallback)
 * - Cancel if player goes offline
 */
@RequiredArgsConstructor
class WaitForDataTask extends BukkitRunnable {

    private final Menu menu;
    private final Player player;
    private final List<String> asyncCacheKeys;
    private final Duration timeout;
    private final ViewerState viewerState;
    private final CompletableFuture<MenuOpenResult> future;

    private Instant startTime;

    void start() {
        this.startTime = Instant.now();
        // Poll every tick (50ms) for responsive UX
        this.runTaskTimer(this.menu.getPlugin(), 0L, 1L);
    }

    @Override
    public void run() {
        Duration elapsed = Duration.between(this.startTime, Instant.now());
        AsyncCache cache = this.viewerState.getAsyncCache();

        // Check if player is still online
        if (!this.player.isOnline()) {
            this.cancel();
            this.menu.getViewerStates().remove(this.player.getUniqueId());
            this.future.complete(MenuOpenResult.playerOffline(elapsed));
            return;
        }

        // Check timeout
        if (elapsed.compareTo(this.timeout) > 0) {
            // Timeout - open anyway with whatever state we have
            this.menu.getPlugin().getLogger().log(Level.WARNING,
                "Menu open timeout for player " + this.player.getName() +
                    " after " + elapsed.toMillis() + "ms (timeout: " + this.timeout.toMillis() + "ms)");
            this.cancel();
            this.openMenu();

            // Collect component states
            Set<String> successful = new HashSet<>();
            Set<String> failed = new HashSet<>();
            Set<String> pending = new HashSet<>();

            for (String key : this.asyncCacheKeys) {
                switch (cache.getState(key)) {
                    case SUCCESS -> successful.add(key);
                    case ERROR -> failed.add(key);
                    default -> pending.add(key);
                }
            }

            this.future.complete(MenuOpenResult.timeout(elapsed, successful, failed, pending));
            return;
        }

        // Check if all async components are ready (SUCCESS or ERROR)
        Set<String> successful = new HashSet<>();
        Set<String> failed = new HashSet<>();
        boolean allReady = true;

        for (String key : this.asyncCacheKeys) {
            AsyncCache.AsyncState state = cache.getState(key);
            if (state == AsyncCache.AsyncState.SUCCESS) {
                successful.add(key);
            } else if (state == AsyncCache.AsyncState.ERROR) {
                failed.add(key);
            } else {
                allReady = false;
            }
        }

        if (allReady) {
            // All data loaded - open menu
            this.cancel();
            this.openMenu();
            this.future.complete(MenuOpenResult.preloaded(elapsed, successful, failed));
        }

        // Not ready yet - continue polling next tick
    }

    private void openMenu() {
        // Ensure we're on main thread
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(this.menu.getPlugin(), this::openMenu);
            return;
        }

        MenuContext context = new MenuContext(this.menu, this.player);
        Inventory inventory = this.viewerState.getInventory();

        // Render all panes again (async components will show SUCCESS/ERROR state)
        this.menu.render(inventory, context);

        // Open inventory
        if (this.player.getOpenInventory().getTopInventory() != inventory) {
            this.player.openInventory(inventory);
        }

        // Start update task if this is the first viewer
        boolean isFirstViewer = this.menu.getViewerStates().size() == 1;
        if (isFirstViewer && (this.menu.getUpdateTask() != null) && !this.menu.getUpdateTask().isRunning()) {
            this.menu.getUpdateTask().start();
        }
    }
}
