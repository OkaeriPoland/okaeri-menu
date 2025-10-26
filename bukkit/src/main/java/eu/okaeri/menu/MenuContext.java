package eu.okaeri.menu;

import eu.okaeri.menu.async.AsyncCache;
import eu.okaeri.menu.async.Computed;
import eu.okaeri.menu.item.MenuItem;
import eu.okaeri.menu.navigation.NavigationHistory;
import eu.okaeri.menu.pagination.PaginationContext;
import eu.okaeri.menu.pane.Pane;
import eu.okaeri.menu.pane.StaticPane;
import eu.okaeri.menu.state.PrimitiveDefaults;
import eu.okaeri.menu.state.TypeReference;
import eu.okaeri.menu.state.ViewerState;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Context provided to menu event handlers.
 * Provides access to the menu, viewer, and utility methods for navigation, refresh, etc.
 */
@Getter
@AllArgsConstructor
public class MenuContext {

    protected final Menu menu;
    protected final HumanEntity entity;

    // ========================================
    // CONVENIENCE ACCESSORS
    // ========================================

    /**
     * Gets the player as a Player instance (cast from HumanEntity).
     *
     * @return The player
     */
    @NonNull
    public Player getPlayer() {
        return (Player) this.entity;
    }

    /**
     * Gets the plugin's logger.
     * <p>
     * Convenience method for accessing the plugin logger from menu event handlers.
     *
     * @return The plugin's logger
     */
    @NonNull
    public Logger getLogger() {
        return this.menu.getPlugin().getLogger();
    }

    // ========================================
    // MENU OPERATIONS
    // ========================================

    /**
     * Schedules a menu refresh for the next tick.
     * This is important for interactive slots - the refresh must happen AFTER
     * Bukkit has processed the item placement, not during the event.
     */
    public void refresh() {
        Plugin plugin = this.menu.getPlugin();
        Bukkit.getScheduler().runTask(plugin, () -> this.menu.refresh(this.entity));
    }

    /**
     * Schedules a pane refresh for the next tick.
     * This is important for interactive slots - the refresh must happen AFTER
     * Bukkit has processed the item placement, not during the event.
     */
    public void refreshPane(@NonNull String paneName) {
        Plugin plugin = this.menu.getPlugin();
        Bukkit.getScheduler().runTask(plugin, () -> this.menu.refreshPane(this.entity, paneName));
    }

    /**
     * Gets a pane by name.
     *
     * @param paneName The pane name
     * @return The pane, or null if not found
     */
    public Pane pane(@NonNull String paneName) {
        return this.menu.getPane(paneName);
    }

    /**
     * Closes this menu for the player.
     */
    public void closeInventory() {
        this.menu.close(this.entity);
        this.entity.closeInventory();
    }

    // ========================================
    // PLAYER FEEDBACK
    // ========================================

    /**
     * Sends a message to the player.
     * The message is processed through the menu's MessageProvider,
     * supporting MiniMessage formatting, legacy colors, and placeholders.
     *
     * @param message The message template
     */
    public void sendMessage(@NonNull String message) {
        this.sendMessage(message, Map.of());
    }

    /**
     * Sends a message to the player with variable placeholders.
     * The message is processed through the menu's MessageProvider,
     * supporting MiniMessage formatting, legacy colors, and placeholders.
     *
     * @param message The message template
     * @param vars    Variables for placeholder replacement
     */
    public void sendMessage(@NonNull String message, @NonNull Map<String, Object> vars) {
        this.entity.sendMessage(this.menu.getMessageProvider().resolveSingle(this.entity, message, vars));
    }

    /**
     * Plays a sound to the player.
     *
     * @param sound The sound to play
     */
    public void playSound(@NonNull Sound sound) {
        this.entity.getWorld().playSound(this.entity.getLocation(), sound, 1.0f, 1.0f);
    }

    /**
     * Plays a sound to the player with volume and pitch.
     *
     * @param sound  The sound to play
     * @param volume The volume (0.0 to 1.0)
     * @param pitch  The pitch (0.5 to 2.0)
     */
    public void playSound(@NonNull Sound sound, float volume, float pitch) {
        this.entity.getWorld().playSound(this.entity.getLocation(), sound, volume, pitch);
    }

    // ========================================
    // NAVIGATION
    // ========================================

    /**
     * Opens another menu and tracks it in navigation history.
     * The player can navigate back to the current menu using back().
     *
     * @param targetMenu The menu to open
     */
    public void open(@NonNull Menu targetMenu) {
        NavigationHistory.open(this.entity, targetMenu);
    }

    /**
     * Opens another menu with parameters and tracks it in navigation history.
     *
     * @param targetMenu The menu to open
     * @param params     Parameters to pass to the menu
     */
    public void open(@NonNull Menu targetMenu, @NonNull Map<String, Object> params) {
        NavigationHistory.open(this.entity, targetMenu, params);
    }

    /**
     * Navigates back to the previous menu in history.
     * If there is no previous menu, closes the inventory.
     *
     * @return true if navigated back, false if there was no history
     */
    public boolean back() {
        return NavigationHistory.back(this.entity);
    }

    /**
     * Checks if there is a previous menu in navigation history.
     * Useful for conditionally showing back buttons.
     *
     * @return true if there is a previous menu
     */
    public boolean hasLast() {
        return NavigationHistory.hasLast(this.entity);
    }

    /**
     * Gets the last menu snapshot without navigating.
     *
     * @return The last menu snapshot, or null if no history
     */
    public NavigationHistory.MenuSnapshot last() {
        return NavigationHistory.last(this.entity);
    }

    /**
     * Clears the navigation history for this player.
     * Useful when you want to prevent back navigation.
     */
    public void clearHistory() {
        NavigationHistory.clear(this.entity);
    }

    /**
     * Gets the current depth of the navigation stack.
     *
     * @return The depth (0 if no history)
     */
    public int navigationDepth() {
        return NavigationHistory.depth(this.entity);
    }

    // ========================================
    // PAGINATION
    // ========================================

    /**
     * Gets the pagination context for a pane.
     * Use this to access page navigation and filtering.
     *
     * @param paneName The pane name
     * @param <T>      Item type
     * @return The pagination context
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public <T> PaginationContext<T> pagination(@NonNull String paneName) {
        return PaginationContext.get(this, paneName);
    }

    // ========================================
    // INTERACTIVE SLOT HELPERS
    // ========================================

    /**
     * Sets an item in an interactive slot using pane name and local coordinates.
     * This is a convenience method that handles coordinate conversion and validation.
     *
     * @param paneName The name of the pane
     * @param localX   The local X coordinate (column) within the pane
     * @param localY   The local Y coordinate (row) within the pane
     * @param item     The item to set (can be null to clear)
     * @throws IllegalArgumentException if the pane doesn't exist or slot is not interactive
     */
    public void setSlotItem(@NonNull String paneName, int localX, int localY, ItemStack item) {
        Pane pane = this.menu.getPanes().get(paneName);
        if (pane == null) {
            throw new IllegalArgumentException("Pane not found: " + paneName);
        }

        if (!(pane instanceof StaticPane staticPane)) {
            throw new IllegalArgumentException("setSlotItem only supports StaticPane, not " + pane.getClass().getSimpleName());
        }

        MenuItem menuItem = staticPane.getItem(localX, localY);

        if (menuItem == null) {
            throw new IllegalArgumentException("No item at local coordinates (" + localX + ", " + localY + ") in pane " + paneName);
        }

        if (!menuItem.isInteractive()) {
            throw new IllegalArgumentException(
                "Cannot set item at (" + localX + ", " + localY + ") - slot is not interactive. " +
                    "Use .interactive(), .allowPlacement(true), or .allowPickup(true) on the MenuItem."
            );
        }

        // Convert local coordinates to global slot
        int globalSlot = staticPane.getBounds().toGlobalSlot(localX, localY);

        // Get the inventory for this viewer
        ViewerState viewerState = this.menu.getViewerStates().get(this.entity.getUniqueId());
        if (viewerState != null) {
            viewerState.getInventory().setItem(globalSlot, item);
        }
    }

    // ========================================
    // ASYNC DATA ACCESS
    // ========================================

    /**
     * Access async data by key with full state information.
     * Returns {@link Computed} which can be in SUCCESS, LOADING, ERROR, or EMPTY state.
     *
     * <p>Simple usage (Optional-like):
     * <pre>{@code
     * int level = ctx.computed("stats").map(Stats::getLevel).orElse(0);
     * }</pre>
     *
     * <p>State-specific defaults:
     * <pre>{@code
     * Material icon = ctx.computed("status")
     *     .loading(Material.CLOCK)
     *     .error(Material.BARRIER)
     *     .orElse(Material.STONE);
     * }</pre>
     *
     * <p>Native pattern matching (Java 21+):
     * <pre>{@code
     * String msg = switch (ctx.computed("data")) {
     *     case Success(var value) -> "Loaded: " + value;
     *     case Loading() -> "Loading...";
     *     case Error(var err) -> "Error: " + err.getMessage();
     *     case Empty() -> "No data";
     * };
     * }</pre>
     *
     * <p>Side effects:
     * <pre>{@code
     * ctx.computed("stats")
     *     .onSuccess(s -> log.info("Loaded: {}", s))
     *     .onError(e -> log.error("Failed", e))
     *     .map(Stats::getLevel)
     *     .orElse(0);
     * }</pre>
     *
     * @param key The data key
     * @param <T> The value type
     * @return Computed in one of four states: SUCCESS, LOADING, ERROR, or EMPTY
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public <T> Computed<T> computed(@NonNull String key) {
        ViewerState state = this.menu.getViewerState(this.entity.getUniqueId());
        if (state == null) {
            return Computed.empty();
        }

        AsyncCache cache = state.getAsyncCache();
        AsyncCache.AsyncState asyncState = cache.getState(key);

        if (asyncState == null) {
            return Computed.empty();
        }

        return switch (asyncState) {
            case SUCCESS -> {
                Optional<?> value = cache.get(key, Object.class);
                yield Computed.ok((T) value.orElse(null));
            }
            case LOADING -> Computed.loading();
            case ERROR -> Computed.err(cache.getError(key).orElse(new RuntimeException("Unknown error")));
        };
    }

    /**
     * Access async data by key with type hint for IDE support.
     * Convenience overload of {@link #computed(String)}.
     *
     * @param key  The data key
     * @param type The expected type (for type safety, not enforced at runtime)
     * @param <T>  The value type
     * @return Computed in one of four states: SUCCESS, LOADING, ERROR, or EMPTY
     */
    @NonNull
    public <T> Computed<T> computed(@NonNull String key, @NonNull Class<T> type) {
        return this.computed(key);
    }

    /**
     * Access async data by key with TypeReference for complex generic types.
     *
     * <p>Useful for type-safe access to async data with complex generics:
     * <pre>{@code
     * // Load complex data
     * menu.reactive("inventory", ctx ->
     *     Map.of("weapons", List.of(sword, bow), "armor", List.of(helmet)),
     *     Duration.ofSeconds(30));
     *
     * // Access with full type safety
     * Computed<Map<String, List<Item>>> inventory = ctx.computed(
     *     "inventory",
     *     new TypeReference<Map<String, List<Item>>>() {}
     * );
     *
     * // Use with pattern matching
     * String display = switch (inventory) {
     *     case Success(var inv) -> "Items: " + inv.size();
     *     case Loading() -> "Loading inventory...";
     *     case Error(var e) -> "Failed to load";
     *     case Empty() -> "No inventory";
     * };
     * }</pre>
     *
     * @param key           The data key
     * @param typeReference Type reference capturing generic type information
     * @param <T>           The value type
     * @return Computed in one of four states: SUCCESS, LOADING, ERROR, or EMPTY
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public <T> Computed<T> computed(@NonNull String key, @NonNull TypeReference<T> typeReference) {
        return this.computed(key);
    }

    /**
     * Access full pagination context (filtered, paginated, etc) for async panes.
     * Designed for AsyncPaginatedPane where the pane's supplier reactively returns current async items.
     *
     * @param paneName The name of the pane
     * @param itemType The item type class
     * @param <T>      The item type
     * @return ComputedValue wrapping PaginationContext
     */
    @NonNull
    public <T> Computed<PaginationContext<T>> computedPagination(@NonNull String paneName, @NonNull Class<T> itemType) {
        return this.computed(paneName).map(allItems -> this.pagination(paneName));
    }

    /**
     * Invalidates a specific async cached entry and refreshes the menu.
     * Forces reload while showing old data (stale-while-revalidate pattern).
     * Old value continues to display until new data loads successfully.
     *
     * <p>Example:
     * <pre>{@code
     * .onClick(ctx -> {
     *     ctx.invalidate("playerStats");  // Reload stats, keep showing old during load
     * })
     * }</pre>
     *
     * @param key The async data key to invalidate
     */
    public void invalidate(@NonNull String key) {
        ViewerState state = this.menu.getViewerState(this.entity.getUniqueId());
        if (state != null) {
            state.getAsyncCache().invalidate(key);
        }
    }

    /**
     * Invalidates all async cached data and refreshes the menu.
     * All data reloads in background while showing old values (stale-while-revalidate).
     * Useful for "refresh all" buttons or after significant state changes.
     *
     * <p>Example:
     * <pre>{@code
     * .onClick(ctx -> {
     *     ctx.invalidate();  // Reload all async data
     * })
     * }</pre>
     */
    public void invalidate() {
        ViewerState state = this.menu.getViewerState(this.entity.getUniqueId());
        if (state != null) {
            state.getAsyncCache().invalidateAll();
        }
    }

    /**
     * Manually load async data (used by async components).
     * Schedules load on async thread, refreshes on completion.
     *
     * @param key    The cache key
     * @param loader The async data loader
     * @param ttl    Time-to-live for cached data
     * @param <T>    The result type
     * @return CompletableFuture for the load operation
     */
    @NonNull
    public <T> CompletableFuture<T> loadAsync(@NonNull String key, @NonNull Supplier<T> loader, @NonNull Duration ttl) {
        ViewerState state = this.menu.getViewerState(this.entity.getUniqueId());
        if (state == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("ViewerState not found"));
        }
        return state.getAsyncCache().getOrStartLoad(key, loader, ttl);
    }

    // ========================================
    // PER-PLAYER STATE MANAGEMENT
    // ========================================

    /**
     * Gets a per-player state variable with type checking.
     * Uses menu-level default if defined, otherwise returns primitive default (0, false, etc.) or null.
     *
     * <p>Automatic defaults:
     * <ul>
     *   <li>Uses explicitly set value if exists</li>
     *   <li>Falls back to menu-level default (from .state() block)</li>
     *   <li>Falls back to primitive defaults: Integer→0, Boolean→false, etc.</li>
     *   <li>Returns null for non-primitive types without menu defaults</li>
     * </ul>
     *
     * <p>State is scoped per-player - each viewer has independent state.
     *
     * @param key  The state variable name
     * @param type The expected type class
     * @param <T>  The value type
     * @return The value or appropriate default
     */
    public <T> T get(@NonNull String key, @NonNull Class<T> type) {
        ViewerState state = this.menu.getViewerState(this.entity.getUniqueId());
        if (state == null) {
            Object menuDefault = this.menu.getStateDefaults().get(key);
            if (type.isInstance(menuDefault)) {
                return type.cast(menuDefault);
            }
            if (PrimitiveDefaults.hasPrimitiveDefault(type)) {
                return PrimitiveDefaults.getDefault(type);
            }
            return null;
        }
        return state.getState(key, type);
    }

    /**
     * Gets a per-player state variable with explicit default.
     * Overrides menu defaults and primitive defaults.
     *
     * @param key          The state variable name
     * @param type         The expected type class
     * @param defaultValue The default if not found
     * @param <T>          The value type
     * @return The value or provided default
     */
    public <T> T get(@NonNull String key, @NonNull Class<T> type, @NonNull T defaultValue) {
        ViewerState state = this.menu.getViewerState(this.entity.getUniqueId());
        if (state == null) {
            return defaultValue;
        }
        return state.getState(key, type, defaultValue);
    }

    /**
     * Sets a per-player state variable.
     * Supports null values to explicitly override defaults.
     *
     * @param key   The state variable name
     * @param value The value to store (null is allowed)
     */
    public void set(@NonNull String key, Object value) {
        ViewerState state = this.menu.getViewerState(this.entity.getUniqueId());
        if (state != null) {
            state.setState(key, value);
        }
    }

    /**
     * Checks if a state variable is explicitly set (not just using default).
     *
     * @param key The state variable name
     * @return true if explicitly set in this player's state
     */
    public boolean has(@NonNull String key) {
        ViewerState state = this.menu.getViewerState(this.entity.getUniqueId());
        return (state != null) && state.hasState(key);
    }

    /**
     * Removes a state variable.
     * After removal, get() will return menu default again.
     *
     * @param key The state variable name
     */
    public void remove(@NonNull String key) {
        ViewerState state = this.menu.getViewerState(this.entity.getUniqueId());
        if (state != null) {
            state.removeState(key);
        }
    }

    /**
     * Clears all custom state for this player.
     * After clearing, all get() calls return menu defaults.
     */
    public void clearState() {
        ViewerState state = this.menu.getViewerState(this.entity.getUniqueId());
        if (state != null) {
            state.clearState();
        }
    }

    // ========================================
    // CONVENIENCE METHODS
    // ========================================

    /**
     * Gets a String state variable.
     */
    public String getString(@NonNull String key) {
        return this.get(key, String.class);
    }

    /**
     * Gets a String state variable with explicit default.
     */
    public String getString(@NonNull String key, String defaultValue) {
        return this.get(key, String.class, defaultValue);
    }

    /**
     * Gets an Integer state variable (returns 0 if not set).
     */
    public Integer getInteger(@NonNull String key) {
        return this.get(key, Integer.class);
    }

    /**
     * Gets an Integer state variable with explicit default.
     */
    public Integer getInteger(@NonNull String key, Integer defaultValue) {
        return this.get(key, Integer.class, defaultValue);
    }

    /**
     * Gets an int state variable (returns 0 if not set).
     */
    public int getInt(@NonNull String key) {
        Integer value = this.get(key, Integer.class);
        return (value != null) ? value : 0;
    }

    /**
     * Gets an int state variable with explicit default.
     */
    public int getInt(@NonNull String key, int defaultValue) {
        Integer value = this.get(key, Integer.class, defaultValue);
        return (value != null) ? value : defaultValue;
    }

    /**
     * Gets a Boolean state variable (returns false if not set).
     */
    public Boolean getBoolean(@NonNull String key) {
        return this.get(key, Boolean.class);
    }

    /**
     * Gets a Boolean state variable with explicit default.
     */
    public Boolean getBoolean(@NonNull String key, Boolean defaultValue) {
        return this.get(key, Boolean.class, defaultValue);
    }

    /**
     * Gets a boolean state variable (returns false if not set).
     */
    public boolean getBool(@NonNull String key) {
        Boolean value = this.get(key, Boolean.class);
        return (value != null) ? value : false;
    }

    /**
     * Gets a boolean state variable with explicit default.
     */
    public boolean getBool(@NonNull String key, boolean defaultValue) {
        Boolean value = this.get(key, Boolean.class, defaultValue);
        return (value != null) ? value : defaultValue;
    }

    /**
     * Gets a Long state variable (returns 0L if not set).
     */
    public Long getLong(@NonNull String key) {
        return this.get(key, Long.class);
    }

    /**
     * Gets a Long state variable with explicit default.
     */
    public Long getLong(@NonNull String key, Long defaultValue) {
        return this.get(key, Long.class, defaultValue);
    }

    /**
     * Gets a Double state variable (returns 0.0d if not set).
     */
    public Double getDouble(@NonNull String key) {
        return this.get(key, Double.class);
    }

    /**
     * Gets a Double state variable with explicit default.
     */
    public Double getDouble(@NonNull String key, Double defaultValue) {
        return this.get(key, Double.class, defaultValue);
    }

    /**
     * Gets a Float state variable (returns 0.0f if not set).
     */
    public Float getFloat(@NonNull String key) {
        return this.get(key, Float.class);
    }

    /**
     * Gets a Float state variable with explicit default.
     */
    public Float getFloat(@NonNull String key, Float defaultValue) {
        return this.get(key, Float.class, defaultValue);
    }

    /**
     * Gets a Map state variable.
     * Type parameters are unchecked due to type erasure - use TypeReference for runtime type safety.
     *
     * <p>Example:
     * <pre>{@code
     * Map<String, Integer> scores = ctx.getMap("scores");
     * }</pre>
     */
    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> getMap(@NonNull String key) {
        return (Map<K, V>) this.get(key, Map.class);
    }

    /**
     * Gets a Map state variable with explicit default.
     */
    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> getMap(@NonNull String key, Map<K, V> defaultValue) {
        return (Map<K, V>) this.get(key, Map.class, defaultValue);
    }

    /**
     * Gets a List state variable.
     * Type parameter is unchecked due to type erasure - use TypeReference for runtime type safety.
     *
     * <p>Example:
     * <pre>{@code
     * List<String> names = ctx.getList("names");
     * }</pre>
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getList(@NonNull String key) {
        return (List<T>) this.get(key, List.class);
    }

    /**
     * Gets a List state variable with explicit default.
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getList(@NonNull String key, List<T> defaultValue) {
        return (List<T>) this.get(key, List.class, defaultValue);
    }

    /**
     * Gets state with TypeReference for complex generic types.
     * Useful for collections and maps with generic parameters.
     *
     * @param key           The state variable name
     * @param typeReference Type reference capturing generic type information
     * @param <T>           The value type
     * @return The value or appropriate default
     */
    public <T> T get(@NonNull String key, @NonNull TypeReference<T> typeReference) {
        ViewerState state = this.menu.getViewerState(this.entity.getUniqueId());
        if (state == null) {
            Object menuDefault = this.menu.getStateDefaults().get(key);
            Class<T> rawType = typeReference.getRawType();
            if (rawType.isInstance(menuDefault)) {
                return rawType.cast(menuDefault);
            }
            return null;
        }
        return state.getState(key, typeReference);
    }

    /**
     * Gets state with TypeReference and explicit default.
     *
     * @param key           The state variable name
     * @param typeReference Type reference capturing generic type information
     * @param defaultValue  The default if not found
     * @param <T>           The value type
     * @return The value or provided default
     */
    public <T> T get(@NonNull String key, @NonNull TypeReference<T> typeReference, @NonNull T defaultValue) {
        ViewerState state = this.menu.getViewerState(this.entity.getUniqueId());
        if (state == null) {
            return defaultValue;
        }
        return state.getState(key, typeReference, defaultValue);
    }

    // ========================================
    // COMMAND EXECUTION
    // ========================================

    /**
     * Executes commands as the player who opened the menu.
     * <p>
     * This is a convenience method for menu items that execute commands.
     * Commands are logged to the server log.
     *
     * @param command the command(s) to execute (without leading slash)
     */
    public void runCommand(@NonNull String... command) {
        this.runCommand(true, command);
    }

    /**
     * Executes commands as the player who opened the menu without logging.
     * <p>
     * This is useful for commands that should not appear in server logs.
     *
     * @param command the command(s) to execute (without leading slash)
     */
    public void runCommandSilently(@NonNull String... command) {
        this.runCommand(false, command);
    }

    /**
     * Executes commands as the player who opened the menu.
     * <p>
     * This is a convenience method for menu items that execute commands.
     *
     * @param log     whether to log the command execution to server log
     * @param command the command(s) to execute (without leading slash)
     */
    public void runCommand(boolean log, @NonNull String... command) {
        for (String cmd : command) {
            if (log) {
                this.getLogger().info(this.entity.getName() + " issued server command (via GUI): /" + cmd);
            }
            Bukkit.dispatchCommand(this.entity, cmd);
        }
    }
}
