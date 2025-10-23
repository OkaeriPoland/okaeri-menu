package eu.okaeri.menu.navigation;

import eu.okaeri.menu.Menu;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Manages navigation history for menu systems.
 * Tracks which menus a player has visited in order to enable back navigation.
 * <p>
 * MEMORY LEAK PREVENTION:
 * - Depth limited to 10 entries per player
 * - Periodic cleanup task removes orphaned entries (every 60s)
 * - PlayerQuitEvent cleanup (via MenuListener)
 */
public final class NavigationHistory {

    /**
     * Maximum depth of navigation history per player
     */
    private static final int MAX_DEPTH = 10;

    /**
     * Cleanup interval in ticks (60 seconds = 1200 ticks)
     */
    private static final long CLEANUP_INTERVAL = 60 * 20;

    // Per-player navigation stacks
    private static final Map<UUID, Deque<MenuSnapshot>> HISTORY = new HashMap<>();

    // Cleanup task
    private static BukkitTask cleanupTask;

    /**
     * Opens a menu and records it in navigation history.
     *
     * @param player The player
     * @param menu   The menu to open
     */
    public static void open(@NonNull HumanEntity player, @NonNull Menu menu) {
        open(player, menu, null);
    }

    /**
     * Opens a menu and records it in navigation history with parameters.
     *
     * @param player The player
     * @param menu   The menu to open
     * @param params Optional parameters to pass to the menu
     */
    public static void open(@NonNull HumanEntity player, @NonNull Menu menu, Map<String, Object> params) {
        UUID playerId = player.getUniqueId();

        // Get or create history stack
        Deque<MenuSnapshot> stack = HISTORY.computeIfAbsent(playerId, k -> new ArrayDeque<>());

        // Push current menu to history
        stack.push(new MenuSnapshot(menu, params));

        // Limit depth to prevent unbounded growth
        while (stack.size() > MAX_DEPTH) {
            stack.removeLast(); // Remove oldest entry
        }

        // Open the menu
        menu.open((Player) player);
    }

    /**
     * Navigates back to the previous menu in history.
     * If there is no previous menu, closes the current menu.
     *
     * @param player The player
     * @return true if navigated back, false if there was no history
     */
    public static boolean back(@NonNull HumanEntity player) {
        UUID playerId = player.getUniqueId();
        Deque<MenuSnapshot> stack = HISTORY.get(playerId);

        if ((stack == null) || stack.isEmpty()) {
            // No history, just close current menu
            player.closeInventory();
            return false;
        }

        // Pop current menu
        stack.pop();

        if (stack.isEmpty()) {
            // No more history, close
            player.closeInventory();
            HISTORY.remove(playerId);
            return false;
        }

        // Get previous menu
        MenuSnapshot previous = stack.peek();

        // Open previous menu (don't add to history again)
        previous.getMenu().open((Player) player);
        return true;
    }

    /**
     * Gets the last menu in history without navigating.
     *
     * @param player The player
     * @return The last menu snapshot, or null if no history
     */
    public static MenuSnapshot last(@NonNull HumanEntity player) {
        UUID playerId = player.getUniqueId();
        Deque<MenuSnapshot> stack = HISTORY.get(playerId);

        if ((stack == null) || (stack.size() < 2)) {
            return null;
        }

        // Get the menu before current (skip current menu at index 0)
        Iterator<MenuSnapshot> iterator = stack.iterator();
        iterator.next(); // Skip current
        return iterator.hasNext() ? iterator.next() : null;
    }

    /**
     * Checks if there is a previous menu in history.
     *
     * @param player The player
     * @return true if there is a previous menu
     */
    public static boolean hasLast(@NonNull HumanEntity player) {
        UUID playerId = player.getUniqueId();
        Deque<MenuSnapshot> stack = HISTORY.get(playerId);
        return (stack != null) && (stack.size() > 1);
    }

    /**
     * Clears all navigation history for a player.
     *
     * @param player The player
     */
    public static void clear(@NonNull HumanEntity player) {
        HISTORY.remove(player.getUniqueId());
    }

    /**
     * Clears all navigation history (e.g., on plugin disable).
     */
    public static void clearAll() {
        HISTORY.clear();
    }

    /**
     * Gets the current depth of the navigation stack.
     *
     * @param player The player
     * @return The depth (0 if no history)
     */
    public static int depth(@NonNull HumanEntity player) {
        UUID playerId = player.getUniqueId();
        Deque<MenuSnapshot> stack = HISTORY.get(playerId);
        return (stack == null) ? 0 : stack.size();
    }

    /**
     * Gets the maximum allowed depth for navigation history.
     *
     * @return The maximum depth (10)
     */
    public static int getMaxDepth() {
        return MAX_DEPTH;
    }

    // ========================================
    // MEMORY LEAK PREVENTION
    // ========================================

    /**
     * Starts the periodic cleanup task.
     * This removes orphaned navigation history entries where the player
     * is no longer viewing the top menu.
     * <p>
     * This method is idempotent - calling it multiple times is safe.
     * If the task is already running, this method does nothing.
     * <p>
     * The task is automatically started when the first menu is created.
     *
     * @param plugin The plugin instance
     */
    public static void startCleanupTask(@NonNull Plugin plugin) {
        if (cleanupTask != null) {
            return; // Already running, don't recreate
        }

        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin,
            NavigationHistory::cleanup,
            CLEANUP_INTERVAL,
            CLEANUP_INTERVAL
        );
    }

    /**
     * Stops the periodic cleanup task.
     * Call this when the plugin disables.
     */
    public static void stopCleanupTask() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
    }

    /**
     * Cleans up orphaned navigation history entries.
     * Removes entries where the player is no longer viewing the top menu.
     * <p>
     * This is called automatically by the cleanup task, but can also be
     * called manually if needed.
     */
    public static void cleanup() {
        Iterator<Map.Entry<UUID, Deque<MenuSnapshot>>> it = HISTORY.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Deque<MenuSnapshot>> entry = it.next();
            UUID playerId = entry.getKey();
            Deque<MenuSnapshot> stack = entry.getValue();

            // Remove empty stacks
            if (stack.isEmpty()) {
                it.remove();
                continue;
            }

            // Check if player is still viewing the top menu
            MenuSnapshot top = stack.peek();
            Menu topMenu = top.getMenu();

            // If the player is not viewing this menu anymore, clean up the entire history
            if (topMenu.getViewerState(playerId) == null) {
                it.remove();
            }
        }
    }

    /**
     * Gets the total number of players with navigation history.
     * Useful for monitoring and debugging.
     *
     * @return The number of players in the history map
     */
    public static int getTotalPlayers() {
        return HISTORY.size();
    }

    /**
     * Represents a snapshot of a menu state in history.
     */
    public static class MenuSnapshot {
        private final Menu menu;
        private final Map<String, Object> params;

        public MenuSnapshot(@NonNull Menu menu, Map<String, Object> params) {
            this.menu = menu;
            this.params = (params != null) ? new HashMap<>(params) : new HashMap<>();
        }

        @NonNull
        public Menu getMenu() {
            return this.menu;
        }

        @NonNull
        public Map<String, Object> getParams() {
            return Collections.unmodifiableMap(this.params);
        }

        public Object getParam(@NonNull String key) {
            return this.params.get(key);
        }

        @SuppressWarnings("unchecked")
        public <T> T getParam(@NonNull String key, @NonNull Class<T> type) {
            Object value = this.params.get(key);
            if ((value != null) && type.isAssignableFrom(value.getClass())) {
                return (T) value;
            }
            return null;
        }
    }
}
