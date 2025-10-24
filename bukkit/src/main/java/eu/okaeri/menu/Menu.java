package eu.okaeri.menu;

import eu.okaeri.menu.async.AsyncExecutor;
import eu.okaeri.menu.async.AsyncLoader;
import eu.okaeri.menu.item.AsyncMenuItem;
import eu.okaeri.menu.item.MenuItem;
import eu.okaeri.menu.message.DefaultMessageProvider;
import eu.okaeri.menu.message.MessageProvider;
import eu.okaeri.menu.navigation.NavigationHistory;
import eu.okaeri.menu.pane.AbstractPane;
import eu.okaeri.menu.pane.AsyncPaginatedPane;
import eu.okaeri.menu.pane.Pane;
import eu.okaeri.menu.pane.PaneBounds;
import eu.okaeri.menu.reactive.ReactiveProperty;
import eu.okaeri.menu.state.StateDefaults;
import eu.okaeri.menu.state.ViewerState;
import lombok.Getter;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Represents a menu with pane-based layout.
 * Each menu contains one or more panes that manage different regions of the inventory.
 */
@Getter
public class Menu implements InventoryHolder {

    // Per-viewer state (inventory + pagination contexts)
    private final Map<UUID, ViewerState> viewerStates = new ConcurrentHashMap<>();

    private @NonNull final ReactiveProperty<String> title;
    private final int rows;
    private @NonNull final Map<String, Pane> panes = new LinkedHashMap<>();
    private final Duration updateInterval;
    private @NonNull final Plugin plugin;
    private @NonNull final AsyncExecutor asyncExecutor;
    private @NonNull final MessageProvider messageProvider;
    private @NonNull final Map<String, Object> stateDefaults;
    private @NonNull final AsyncLoader asyncLoader;
    private @NonNull final MenuUpdateTask updateTask;

    private Menu(@NonNull Builder builder) {
        this.title = builder.title;
        this.rows = builder.rows;
        this.panes.putAll(builder.panes);
        this.updateInterval = builder.updateInterval;
        this.plugin = builder.plugin;
        this.asyncExecutor = (builder.asyncExecutor != null) ? builder.asyncExecutor : AsyncExecutor.bukkit(this.plugin);
        this.messageProvider = (builder.messageProvider != null) ? builder.messageProvider : new DefaultMessageProvider();
        this.stateDefaults = builder.stateDefaults.isEmpty() ? Map.of() : Map.copyOf(builder.stateDefaults);
        this.asyncLoader = builder.asyncLoader;
        this.updateTask = new MenuUpdateTask(this);

        // Auto-register MenuListener (idempotent - safe to call multiple times)
        MenuListener.register(this.plugin);

        // Start navigation history cleanup task (idempotent - safe to call multiple times)
        NavigationHistory.startCleanupTask(this.plugin);
    }

    /**
     * Opens this menu for a player immediately.
     * Async data will show loading states and load in the background.
     *
     * @param player The player to open the menu for
     * @return Future with open result (completes immediately with IMMEDIATE status)
     */
    public CompletableFuture<MenuOpenResult> open(@NonNull Player player) {
        this.openImmediate(player);
        return CompletableFuture.completedFuture(MenuOpenResult.immediate());
    }

    /**
     * Opens this menu for a player after waiting for async data to load.
     * Waits until all AsyncPaginatedPane and AsyncMenuItem instances are SUCCESS or ERROR,
     * or until timeout expires.
     *
     * @param player  The player to open the menu for
     * @param timeout Maximum time to wait for data (e.g., Duration.ofSeconds(3))
     * @return Future with detailed result about data loading and open status
     */
    public CompletableFuture<MenuOpenResult> open(@NonNull Player player, @NonNull Duration timeout) {
        // Collect all async cache keys
        List<String> asyncCacheKeys = this.collectAsyncCacheKeys();

        if (asyncCacheKeys.isEmpty()) {
            // No async components - just open immediately
            this.openImmediate(player);
            return CompletableFuture.completedFuture(MenuOpenResult.immediate());
        }

        CompletableFuture<MenuOpenResult> future = new CompletableFuture<>();
        this.openWithWait(player, asyncCacheKeys, timeout, future);
        return future;
    }

    /**
     * Gets or creates viewer state for a player.
     */
    private ViewerState getOrCreateViewerState(@NonNull Player player, @NonNull MenuContext context) {
        return this.viewerStates.computeIfAbsent(player.getUniqueId(), uuid -> {
            String titleTemplate = this.title.get(context);
            Component titleComponent = this.messageProvider.resolveSingle(player, titleTemplate, Map.of());
            Inventory inv = Bukkit.createInventory(this, this.rows * 9, titleComponent);
            return new ViewerState(context, inv, this.asyncExecutor);
        });
    }

    /**
     * Opens menu immediately (current behavior).
     */
    private void openImmediate(@NonNull Player player) {
        MenuContext context = new MenuContext(this, player);

        // Create or get viewer state for this player
        ViewerState state = this.getOrCreateViewerState(player, context);
        Inventory inventory = state.getInventory();

        // Render all panes
        this.render(inventory, context);

        // Open for player only if not already viewing this inventory
        if (player.getOpenInventory().getTopInventory() != inventory) {
            player.openInventory(inventory);
        }

        // Start update task after opening (so it doesn't refresh before first render)
        this.updateTask.start();
    }

    /**
     * Collects all async cache keys from menu-level reactive sources,
     * AsyncPaginatedPane instances, and AsyncMenuItem instances.
     *
     * @return List of cache keys to wait for
     */
    @NonNull
    private List<String> collectAsyncCacheKeys() {
        List<String> cacheKeys = new ArrayList<>();

        // Add menu-level reactive source keys
        cacheKeys.addAll(this.asyncLoader.getKeys());

        for (Pane pane : this.panes.values()) {
            // Check if pane itself is async
            if (pane instanceof AsyncPaginatedPane<?> asyncPane) {
                cacheKeys.add(asyncPane.getName());
            }

            // Check for AsyncMenuItem in static items
            if (pane instanceof AbstractPane abstractPane) {
                for (MenuItem menuItem : abstractPane.getFilteringItems().values()) {
                    if (menuItem instanceof AsyncMenuItem asyncItem) {
                        cacheKeys.add(asyncItem.getCacheKey());
                    }
                }
            }
        }

        return cacheKeys;
    }

    /**
     * Opens menu after waiting for async data to load.
     * Polls using scheduler until all async components are ready or timeout.
     */
    private void openWithWait(@NonNull Player player, @NonNull List<String> asyncCacheKeys, @NonNull Duration timeout, @NonNull CompletableFuture<MenuOpenResult> future) {
        MenuContext context = new MenuContext(this, player);

        // Create viewer state (but don't open inventory yet)
        ViewerState state = this.getOrCreateViewerState(player, context);

        // Trigger initial render to start async loads
        // This will call AsyncPaginatedPane.render() and AsyncMenuItem.render()
        // which internally call context.loadAsync() if data not cached
        Inventory inventory = state.getInventory();
        this.render(inventory, context);

        // Schedule polling task to check for completion
        new WaitForDataTask(this, player, asyncCacheKeys, timeout, state, future).start();
    }

    /**
     * Renders all panes into the inventory.
     * If a pane fails to render, it's cleared and the error is logged,
     * but other panes continue rendering normally.
     *
     * @param inventory The inventory to render into
     * @param context   The reactive context
     */
    public void render(@NonNull Inventory inventory, @NonNull MenuContext context) {
        // Trigger all menu-level reactive loads before rendering panes
        // loadAsync() is idempotent - respects TTL and won't reload if cached
        this.asyncLoader.trigger(context);

        for (Pane pane : this.panes.values()) {
            try {
                pane.render(inventory, context);
            } catch (Exception exception) {
                // Log error with pane name for debugging
                this.plugin.getLogger().log(Level.WARNING,
                    "Exception rendering pane '" + pane.getName() + "', clearing pane bounds",
                    exception);

                // Clear the pane's slots to show empty (safe fallback)
                pane.getBounds().slots().clear(inventory);
            }
        }
    }

    /**
     * Refreshes the menu for a specific viewer.
     * Updates both the title (if changed) and all pane contents.
     * <p>
     * Note: Dynamic title updates require reopening the inventory in Paper API 1.21+,
     * which may reset cursor position. The menu only reopens if the title actually changed.
     *
     * @param player The player viewing the menu
     */
    public void refresh(@NonNull HumanEntity player) {
        ViewerState state = this.viewerStates.get(player.getUniqueId());
        if (state != null) {
            Inventory inventory = state.getInventory();
            MenuContext context = new MenuContext(this, player);

            // Invalidate title to ensure it re-evaluates
            this.title.invalidate();

            // Check if title has changed (compare serialized strings, not Component objects)
            String newTitleTemplate = this.title.get(context);
            Component newTitleComponent = this.messageProvider.resolveSingle(player, newTitleTemplate, Map.of());
            Component currentTitle = player.getOpenInventory().title();

            // Serialize both to legacy strings for accurate comparison
            String newTitleSerialized = LegacyComponentSerializer.legacySection().serialize(newTitleComponent);
            String currentTitleSerialized = LegacyComponentSerializer.legacySection().serialize(currentTitle);
            boolean titleChanged = !newTitleSerialized.equals(currentTitleSerialized);

            // Only reopen if title changed (to avoid cursor reset)
            if (titleChanged) {

                // Update inventory title by creating new one with same contents
                Inventory newInventory = Bukkit.createInventory(this, this.rows * 9, newTitleComponent);

                // Copy all items to new inventory
                for (int i = 0; i < inventory.getSize(); i++) {
                    newInventory.setItem(i, inventory.getItem(i));
                }

                // Update state with new inventory
                state.setInventory(newInventory);

                // Reopen with new inventory
                player.openInventory(newInventory);

                // Clean up the old inventory for glitch safety
                inventory.clear();
            }

            // Invalidate all panes
            for (Pane pane : this.panes.values()) {
                pane.invalidate();
            }

            // Re-render (use current inventory from state)
            this.render(state.getInventory(), context);

            // Record refresh timestamp for interval tracking
            state.recordRefresh();
        }
    }

    /**
     * Refreshes a specific pane for a viewer.
     * <p>
     * Only updates the specified pane's content, not the menu title or other panes.
     * Use {@link #refresh(HumanEntity)} to refresh the entire menu including title.
     *
     * @param player   The player viewing the menu
     * @param paneName The name of the pane to refresh
     */
    public void refreshPane(@NonNull HumanEntity player, @NonNull String paneName) {
        ViewerState state = this.viewerStates.get(player.getUniqueId());
        Pane pane = this.panes.get(paneName);

        if ((state != null) && (pane != null)) {
            MenuContext context = new MenuContext(this, player);
            pane.invalidate();
            pane.render(state.getInventory(), context);
        }
    }

    /**
     * Gets a pane by name.
     *
     * @param name The pane name
     * @return The pane, or null if not found
     */
    public Pane getPane(@NonNull String name) {
        return this.panes.get(name);
    }

    /**
     * Closes the menu for a player and cleans up.
     * Automatically removes all viewer state including pagination contexts.
     * <p>
     * NOTE: This method only cleans up viewer state. It does NOT call player.closeInventory()
     * to avoid conflicts with menu navigation (where inventory is already being closed/replaced).
     * If you need to programmatically close the inventory, call player.closeInventory() separately.
     *
     * @param player The player
     */
    public void close(@NonNull HumanEntity player) {
        this.viewerStates.remove(player.getUniqueId());  // Cleans up all state including pagination!

        // Stop update task if this was the last viewer
        if (this.viewerStates.isEmpty() && (this.updateTask != null) && this.updateTask.isRunning()) {
            this.updateTask.stop();
        }
    }

    /**
     * Gets the viewer state for a player.
     * Public for access by PaginationContext from pagination package.
     *
     * @param playerId The player's UUID
     * @return The viewer state, or null if not found
     */
    public ViewerState getViewerState(UUID playerId) {
        return this.viewerStates.get(playerId);
    }

    /**
     * Gets all viewer states.
     * Package-private for access by MenuUpdateTask.
     *
     * @return Map of player UUIDs to viewer states
     */
    Map<UUID, ViewerState> getViewerStates() {
        return this.viewerStates;
    }

    @Override
    public @NonNull Inventory getInventory() {
        // Return a default inventory (not per-viewer)
        // This is mainly for InventoryHolder compliance
        return Bukkit.createInventory(this, this.rows * 9, Component.empty());
    }

    /**
     * Gets the plugin instance associated with this menu.
     * Used for task scheduling (e.g., delayed refreshes).
     *
     * @return The plugin instance, or null if not set
     */
    public @NonNull Plugin getPlugin() {
        return this.plugin;
    }

    /**
     * Creates a new menu builder.
     *
     * @param plugin The plugin instance (required for async operations and scheduling)
     * @return A new menu builder
     */
    @NonNull
    public static Builder builder(@NonNull Plugin plugin) {
        return new Builder(plugin);
    }

    public static class Builder {
        private @NonNull ReactiveProperty<String> title = ReactiveProperty.of("Menu");
        private int rows = -1; // -1 means auto-calculate from panes
        private @NonNull Map<String, Pane> panes = new LinkedHashMap<>();
        private Duration updateInterval = null;
        private @NonNull final Plugin plugin;
        private AsyncExecutor asyncExecutor = null;
        private MessageProvider messageProvider = null;
        private @NonNull final Map<String, Object> stateDefaults = new HashMap<>();
        private @NonNull final AsyncLoader asyncLoader = new AsyncLoader();

        private Builder(@NonNull Plugin plugin) {
            this.plugin = plugin;
        }

        @NonNull
        public Builder title(@NonNull String title) {
            this.title = ReactiveProperty.of(title);
            return this;
        }

        @NonNull
        public Builder title(@NonNull Supplier<String> supplier) {
            this.title = ReactiveProperty.of(supplier);
            return this;
        }

        @NonNull
        public Builder title(@NonNull Function<MenuContext, String> function) {
            this.title = ReactiveProperty.ofContext(function);
            return this;
        }

        /**
         * Sets a localized title map that uses MessageProvider for locale lookup.
         *
         * @param localizedTitles Map of locale to title template
         * @return This builder
         * @throws IllegalArgumentException if map is empty
         */
        @NonNull
        public Builder title(@NonNull Map<Locale, String> localizedTitles) {
            if (localizedTitles.isEmpty()) {
                throw new IllegalArgumentException("Localized titles map cannot be empty");
            }
            this.title = ReactiveProperty.ofContext(ctx -> {
                Component component = ctx.getMenu().getMessageProvider().resolveSingle(ctx.getEntity(), localizedTitles, Map.of());
                return LegacyComponentSerializer.legacySection().serialize(component);
            });
            return this;
        }

        /**
         * Sets the number of rows for this menu.
         * If not called, rows will be auto-calculated from pane bounds.
         *
         * @param rows The number of rows (1-6)
         * @return This builder
         * @throws IllegalArgumentException if rows is not between 1 and 6
         */
        @NonNull
        public Builder rows(int rows) {
            if ((rows < 1) || (rows > 6)) {
                throw new IllegalArgumentException("Rows must be between 1 and 6, got: " + rows);
            }
            this.rows = rows;
            return this;
        }

        /**
         * Adds a pane to this menu.
         * Validates that the pane doesn't overlap with existing panes.
         *
         * @param name The pane name
         * @param pane The pane
         * @return This builder
         */
        @NonNull
        public Builder pane(@NonNull String name, @NonNull Pane pane) {
            // Validate no overlap with existing panes
            PaneBounds newBounds = pane.getBounds();

            for (Map.Entry<String, Pane> entry : this.panes.entrySet()) {
                PaneBounds existingBounds = entry.getValue().getBounds();
                if (newBounds.overlaps(existingBounds)) {
                    throw new IllegalArgumentException(
                        "Pane '" + name + "' overlaps with existing pane '" + entry.getKey() + "': " +
                            newBounds + " overlaps " + existingBounds
                    );
                }
            }

            // Validate pane fits within menu rows (only if rows explicitly set)
            if ((this.rows != -1) && ((newBounds.getY() + newBounds.getHeight()) > this.rows)) {
                throw new IllegalArgumentException(
                    "Pane '" + name + "' exceeds menu height: " +
                        "pane ends at row " + (newBounds.getY() + newBounds.getHeight()) +
                        " but menu has " + this.rows + " rows"
                );
            }

            this.panes.put(name, pane);
            return this;
        }

        /**
         * Sets the automatic update interval for reactive properties.
         *
         * @param interval The update interval, or null to disable
         * @return This builder
         */
        @NonNull
        public Builder updateInterval(Duration interval) {
            this.updateInterval = interval;
            return this;
        }

        /**
         * Sets a custom async executor for background tasks.
         * If not set, defaults to Bukkit's scheduler.
         *
         * @param asyncExecutor The async executor
         * @return This builder
         */
        @NonNull
        public Builder asyncExecutor(@NonNull AsyncExecutor asyncExecutor) {
            this.asyncExecutor = asyncExecutor;
            return this;
        }

        /**
         * Sets a custom message provider for formatting item names and lore.
         * If not set, defaults to {@link DefaultMessageProvider} which supports
         * §, &, and MiniMessage formats.
         *
         * @param messageProvider The message provider
         * @return This builder
         */
        @NonNull
        public Builder messageProvider(@NonNull MessageProvider messageProvider) {
            this.messageProvider = messageProvider;
            return this;
        }

        /**
         * Defines default values for per-player state variables.
         * State is scoped per-player - each viewer has independent state.
         *
         * @param configurator State configuration
         * @return This builder
         */
        @NonNull
        public Builder state(@NonNull Consumer<StateDefaults> configurator) {
            StateDefaults defaults = new StateDefaults(this.stateDefaults);
            configurator.accept(defaults);
            return this;
        }

        /**
         * Declares a menu-level reactive data source.
         * Data is loaded asynchronously, cached per-viewer, and shared across all menu items.
         * Use {@link MenuContext#computed(String)} to access the loaded data from any item.
         *
         * <p>Example:
         * <pre>{@code
         * Menu.builder(plugin)
         *     .reactive("playerStats", ctx -> database.getStats(ctx.getEntity()), Duration.ofSeconds(30))
         *     .pane("main", staticPane()
         *         .item(0, 0, item()
         *             .name(ctx -> ctx.computed("playerStats")
         *                 .map(stats -> "Level: " + stats.getLevel())
         *                 .loading("Loading...")
         *                 .orElse("Unknown"))
         *             .build())
         *         .build())
         *     .build();
         * }</pre>
         *
         * @param key    Unique identifier for this data source
         * @param loader Function that loads the data (receives MenuContext)
         * @param ttl    Time-to-live for cached data
         * @return This builder
         */
        @NonNull
        public Builder reactive(@NonNull String key, @NonNull Function<MenuContext, ?> loader, @NonNull Duration ttl) {
            this.asyncLoader.register(key, loader, ttl);
            return this;
        }

        /**
         * Declares a menu-level reactive data source with default TTL of 1 second.
         * Convenience method for {@link #reactive(String, Function, Duration)}.
         *
         * @param key    Unique identifier for this data source
         * @param loader Function that loads the data (receives MenuContext)
         * @return This builder
         */
        @NonNull
        public Builder reactive(@NonNull String key, @NonNull Function<MenuContext, ?> loader) {
            return this.reactive(key, loader, Duration.ofSeconds(1));
        }

        @NonNull
        public Menu build() {
            // Auto-calculate rows from panes if not explicitly set
            if (this.rows == -1) {
                this.rows = this.panes.values().stream()
                    .mapToInt(pane -> {
                        PaneBounds bounds = pane.getBounds();
                        return bounds.getY() + bounds.getHeight();
                    })
                    .max()
                    .orElse(1); // Default to 1 row if no panes

                // Validate auto-calculated rows
                if (this.rows > 6) {
                    throw new IllegalStateException(
                        "Auto-calculated menu height exceeds maximum: " +
                            "panes require " + this.rows + " rows but maximum is 6"
                    );
                }
            }

            return new Menu(this);
        }
    }
}
