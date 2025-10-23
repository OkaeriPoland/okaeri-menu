package eu.okaeri.menu;

import eu.okaeri.menu.async.AsyncCache;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.List;
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
    private final Menu.ViewerState viewerState;

    private long startTime;

    void start() {
        this.startTime = System.currentTimeMillis();
        // Poll every tick (50ms) for responsive UX
        this.runTaskTimer(this.menu.getPlugin(), 0L, 1L);
    }

    @Override
    public void run() {
        // Check if player is still online
        if (!this.player.isOnline()) {
            this.cancel();
            this.menu.getViewerStates().remove(this.player.getUniqueId());
            return;
        }

        AsyncCache cache = this.viewerState.getAsyncCache();

        // Check timeout
        long elapsed = System.currentTimeMillis() - this.startTime;
        if (elapsed > this.timeout.toMillis()) {
            // Timeout - open anyway with whatever state we have
            this.menu.getPlugin().getLogger().log(Level.WARNING,
                "Menu open timeout for player " + this.player.getName() +
                    " after " + elapsed + "ms (timeout: " + this.timeout.toMillis() + "ms)");
            this.cancel();
            this.openMenu();
            return;
        }

        // Check if all async components are ready (SUCCESS or ERROR)
        boolean allReady = this.asyncCacheKeys.stream().allMatch(cacheKey -> {
            AsyncCache.AsyncState state = cache.getState(cacheKey);
            return (state == AsyncCache.AsyncState.SUCCESS) || (state == AsyncCache.AsyncState.ERROR);
        });

        if (allReady) {
            // All data loaded - open menu
            this.cancel();
            this.openMenu();
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
