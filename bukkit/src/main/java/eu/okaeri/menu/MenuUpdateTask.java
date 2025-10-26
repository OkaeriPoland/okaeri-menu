package eu.okaeri.menu;

import eu.okaeri.menu.state.ViewerState;
import lombok.Getter;
import lombok.NonNull;
import lombok.Synchronized;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Handles automatic periodic updates for menus with reactive properties.
 * Runs every tick and refreshes viewers if any of:
 * 1. State changed (dirty flag)
 * 2. Update interval elapsed since last refresh
 * 3. Async cache has expired entries (for reactive TTL polling)
 */
public class MenuUpdateTask {

    private final Menu menu;
    private final Plugin plugin;
    private final Duration updateInterval;

    public MenuUpdateTask(@NonNull Menu menu) {
        this.menu = menu;
        this.plugin = menu.getPlugin();
        this.updateInterval = menu.getUpdateInterval();
    }

    @Getter
    private BukkitTask task;
    private boolean running = false;

    /**
     * Starts the update task.
     * Task runs every tick and checks both dirty flag and interval elapsed.
     */
    @Synchronized
    public void start() {
        if (this.running) {
            return;
        }

        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    MenuUpdateTask.this.updateMenu();
                } catch (Exception exception) {
                    // FAIL-SAFE: Log but don't crash the scheduler
                    MenuUpdateTask.this.plugin.getLogger().log(Level.WARNING, "Exception during menu auto-update", exception);
                }
            }
        }.runTaskTimer(this.plugin, 1L, 1L);  // Run every tick

        this.running = true;
        this.plugin.getLogger().fine("Started menu update task with interval: " + this.updateInterval);
    }

    /**
     * Stops the update task.
     */
    @Synchronized
    public void stop() {
        if (!this.running) {
            return;
        }

        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }

        this.running = false;
        this.plugin.getLogger().fine("Stopped menu update task");
    }

    /**
     * Updates the menu for all current viewers.
     * Refreshes viewers if any condition is met: dirty, interval elapsed, or expired async cache.
     */
    private void updateMenu() {
        // Get all viewers (copy to avoid concurrent modification)
        UUID[] viewerIds = this.menu.getViewerStates().keySet().toArray(new UUID[0]);

        if (viewerIds.length == 0) {
            // No viewers, could stop task to save resources
            // But keep running in case viewers come back soon
            return;
        }

        // Refresh viewers based on three conditions
        for (UUID viewerId : viewerIds) {
            Player player = Bukkit.getPlayer(viewerId);
            if ((player != null) && player.isOnline()) {
                try {
                    ViewerState state = this.menu.getViewerState(viewerId);
                    if (state == null) {
                        continue;
                    }

                    // Check three conditions for refresh:
                    // 1. State changed (dirty flag)
                    // 2. Update interval elapsed (if configured)
                    // 3. Async cache has expired entries (for reactive TTL polling)
                    boolean dirty = state.isDirtyAndClear();
                    boolean intervalElapsed = (this.updateInterval != null) && state.isIntervalElapsed(this.updateInterval);
                    boolean hasExpiredAsync = state.hasExpiredAsyncData();

                    if (dirty || intervalElapsed || hasExpiredAsync) {
                        this.menu.refresh(player);
                    }
                } catch (Exception exception) {
                    this.plugin.getLogger().log(Level.WARNING, "Exception refreshing menu for player " + player.getName(), exception);
                }
            } else {
                // Player is offline, clean up
                this.menu.getViewerStates().remove(viewerId);
            }
        }
    }

    /**
     * Checks if the task is running.
     */
    public boolean isRunning() {
        return this.running;
    }
}
