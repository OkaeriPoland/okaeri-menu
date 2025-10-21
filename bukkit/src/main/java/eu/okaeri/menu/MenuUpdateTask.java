package eu.okaeri.menu;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
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
 * Refreshes the menu at the specified interval for all viewers.
 */
@RequiredArgsConstructor
public class MenuUpdateTask {

    private final @NonNull Menu menu;
    private final @NonNull Plugin plugin;
    private final @NonNull Duration updateInterval;

    @Getter
    private BukkitTask task;
    private boolean running = false;

    /**
     * Starts the update task.
     */
    public void start() {
        if (this.running || (this.updateInterval == null)) {
            return;
        }

        long ticks = this.updateInterval.toMillis() / 50; // Convert milliseconds to ticks (50ms = 1 tick)
        if (ticks <= 0) {
            ticks = 1; // Minimum 1 tick
        }

        long intervalTicks = ticks;

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
        }.runTaskTimer(this.plugin, intervalTicks, intervalTicks);

        this.running = true;
        this.plugin.getLogger().fine("Started menu update task with interval: " + this.updateInterval);
    }

    /**
     * Stops the update task.
     */
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
     */
    private void updateMenu() {
        // Get all viewers (copy to avoid concurrent modification)
        UUID[] viewerIds = this.menu.getViewerStates().keySet().toArray(new UUID[0]);

        if (viewerIds.length == 0) {
            // No viewers, could stop task to save resources
            // But keep running in case viewers come back soon
            return;
        }

        // Refresh for each viewer
        for (UUID viewerId : viewerIds) {
            Player player = Bukkit.getPlayer(viewerId);
            if ((player != null) && player.isOnline()) {
                try {
                    this.menu.refresh(player);
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
