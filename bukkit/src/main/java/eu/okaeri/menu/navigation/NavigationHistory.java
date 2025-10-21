package eu.okaeri.menu.navigation;

import eu.okaeri.menu.Menu;
import lombok.NonNull;
import org.bukkit.entity.HumanEntity;

import java.util.*;

/**
 * Manages navigation history for menu systems.
 * Tracks which menus a player has visited in order to enable back navigation.
 */
public class NavigationHistory {

    // Per-player navigation stacks
    private static final Map<UUID, Deque<MenuSnapshot>> HISTORY = new HashMap<>();

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

        // Open the menu
        menu.open(player);
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
        previous.getMenu().open(player);
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
