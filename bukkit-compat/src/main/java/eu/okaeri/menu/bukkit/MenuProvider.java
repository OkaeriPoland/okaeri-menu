package eu.okaeri.menu.bukkit;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.bukkit.display.DisplayProvider;
import eu.okaeri.menu.bukkit.meta.MenuInputMeta;
import eu.okaeri.menu.bukkit.meta.MenuItemMeta;
import eu.okaeri.menu.bukkit.meta.MenuMeta;
import eu.okaeri.menu.item.AsyncMenuItem;
import eu.okaeri.menu.item.MenuItem;
import eu.okaeri.menu.pane.PaneBounds;
import eu.okaeri.menu.pane.StaticPane;
import eu.okaeri.menu.state.ViewerState;
import lombok.*;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import static eu.okaeri.menu.pane.StaticPane.staticPane;

/**
 * Manages menu instances and provides menu lifecycle operations.
 * <p>
 * This class is the core of the v1 menu API compatibility layer. It tracks
 * menu instances, handles updates, and provides methods for opening and closing menus.
 * <p>
 * To create a MenuProvider, use {@link #create(Plugin)} or {@link #create(Plugin, int)}.
 * These factory methods automatically register event listeners and start update tasks.
 * <p>
 * Users should migrate to the v2 API as this compatibility layer will be removed
 * in a future release.
 *
 * @deprecated since 0.0.18, scheduled for removal in a future version.
 *             Use the v2 menu API instead.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Deprecated(since = "0.0.18", forRemoval = true)
public class MenuProvider {

    private final Plugin plugin;
    private @Getter @Setter boolean warnUnoptimizedRender = false;

    /**
     * Creates a new MenuProvider with default update interval.
     * <p>
     * This registers event listeners and starts both sync and async update tasks
     * running at 5 tick intervals.
     *
     * @param plugin the plugin instance
     * @return a new MenuProvider
     */
    public static MenuProvider create(@NonNull Plugin plugin) {
        return create(plugin, 5);
    }

    /**
     * Creates a new MenuProvider.
     * <p>
     * In the v2 translation layer, this registers the v2 MenuListener to handle
     * events for all v2 menus (including those created via v1 compat API).
     *
     * @param plugin the plugin instance
     * @param updateTicks the update interval in ticks (ignored in v2 translation)
     * @return a new MenuProvider
     */
    public static MenuProvider create(@NonNull Plugin plugin, int updateTicks) {
        return new MenuProvider(plugin);
    }

    /**
     * Renders the currently open menu for a viewer.
     * <p>
     * In v1, render() forced fresh data (no caching). This clears the async cache
     * before refreshing to match v1 behavior.
     *
     * @param viewer the entity whose menu should be rendered
     */
    public void render(@NonNull HumanEntity viewer) {
        Inventory topInventory = viewer.getOpenInventory().getTopInventory();
        if (topInventory.getHolder() instanceof Menu menu) {
            // Clear async cache to force fresh data (v1 render() behavior)
            ViewerState state = menu.getViewerState(viewer.getUniqueId());
            if (state != null) {
                state.getAsyncCache().clear();
            }
            // Refresh menu (will reload all async data)
            menu.refresh(viewer);
        }
    }

    /**
     * Closes the inventory for a viewer on the main thread.
     *
     * @param viewer the entity whose inventory should be closed
     */
    public void close(@NonNull HumanEntity viewer) {
        this.plugin.getServer().getScheduler().runTask(this.plugin, (Runnable) viewer::closeInventory);
    }

    /**
     * Closes a menu instance for all viewers.
     *
     * @param instance the menu instance to close
     */
    public void close(@NonNull MenuInstance instance) {
        instance.getInventory().getViewers().forEach(HumanEntity::closeInventory);
    }

    /**
     * Opens a menu instance for a viewer.
     * <p>
     * In v2 translation layer, this simply delegates to Menu.open().
     * Parent tracking is handled by v2's NavigationHistory.
     *
     * @param parent the parent menu instance, or null if no parent (ignored in v2)
     * @param instance the menu instance to open
     * @param viewer the entity to open the menu for
     */
    public void open(MenuInstance parent, @NonNull MenuInstance instance, @NonNull HumanEntity viewer) {
        Player player = (Player) viewer;
        instance.getMenu().getV2Menu().open(player);
    }

    /**
     * Safely opens a menu instance for a viewer.
     * <p>
     * In v2 translation layer, this delegates to Menu.open() which handles
     * thread safety internally via WaitForDataTask.
     *
     * @param parent the parent menu instance, or null if no parent (ignored in v2)
     * @param instance the menu instance to open
     * @param viewer the entity to open the menu for
     */
    public void openSafely(MenuInstance parent, @NonNull MenuInstance instance, @NonNull HumanEntity viewer) {
        Player player = (Player) viewer;
        // v2 Menu.open() handles sync internally
        instance.getMenu().getV2Menu().open(player);
    }

    /**
     * Creates an OkaeriMenu from MenuMeta by translating to v2 Menu API.
     * <p>
     * This converts the v1 menu metadata into a v2 Menu object internally:
     * - Creates one StaticPane covering all slots
     * - Converts DisplayProviders to reactive MenuItem properties
     * - Converts ClickHandlers to MenuItem onClick callbacks
     * - Maps v1 slot positions directly to pane slots
     *
     * @param menu the v1 menu metadata
     * @return a new OkaeriMenu wrapping a v2 Menu
     * @throws IllegalArgumentException if positions are invalid or conflicting
     */
    public OkaeriMenu create(@NonNull MenuMeta menu) {

        // Fail-fast validation for unsupported v1 handlers
        if (menu.getCloseHandler() != null) {
            throw new UnsupportedOperationException("CloseHandler is not supported in v2 compat layer..");
        }
        if (menu.getOutsideClickHandler() != null) {
            throw new UnsupportedOperationException("OutsideClickHandler is not supported in v2 compat layer.");
        }
        if (menu.getFallbackClickHandler() != null) {
            throw new UnsupportedOperationException("FallbackClickHandler is not supported in v2 compat layer. Use MenuItem.interactive() for input slots.");
        }
        if (menu.getUpdateHook() != null) {
            throw new UnsupportedOperationException("UpdateHook is not supported in v2 compat layer. Use manual refresh with menuProvider.render() instead.");
        }

        // Build v1 maps for validation and API compatibility
        Map<Integer, MenuItemMeta> itemMap = new LinkedHashMap<>();
        Map<Integer, MenuInputMeta> inputMap = new LinkedHashMap<>();
        Map<Integer, DisplayProvider> providerMap = new LinkedHashMap<>();
        DisplayProvider menuDisplayProvider = menu.getDisplayProvider();

        int rows = menu.getMenuChestSize() / 9;
        int size = menu.getMenuChestSize();
        int lastPosition = -1;

        // Create v2 Menu.Builder
        Menu.Builder menuBuilder = Menu.builder(this.plugin)
            .title((menu.getName() != null) ? menu.getName() : "Menu")
            .rows(rows);

        // Set update interval if present
        if (menu.getUpdate() != null) {
            menuBuilder.updateInterval(menu.getUpdate());
        }

        // Create main pane covering all slots (full width, calculated rows)
        StaticPane.Builder paneBuilder = staticPane()
            .bounds(PaneBounds.of(0, 0, 9, rows));

        // Process items
        for (MenuItemMeta item : menu.getItems()) {

            int[] positions = item.getPositionAsIntArr();

            for (int position : positions) {

                if (position == -1) {
                    position = lastPosition + 1;
                }

                if (position >= size) {
                    throw new IllegalArgumentException("position cannot be greater than menu size (" + position + " >= " + size + ")");
                }

                DisplayProvider itemDisplayProvider = item.getDisplayProvider();
                DisplayProvider currentDisplayProvider = (itemDisplayProvider != null)
                    ? itemDisplayProvider
                    : menuDisplayProvider;

                if (currentDisplayProvider == null) {
                    throw new IllegalArgumentException("item position does not have a displayProvider: " + position);
                }

                if (providerMap.put(position, currentDisplayProvider) != null) {
                    throw new IllegalArgumentException("item position cannot be overridden: " + position);
                }

                // Convert to v2 AsyncMenuItem
                // DisplayProviders are inherently async-capable (can query DB/API)
                // Use AsyncMenuItem to cache results with TTL
                MenuItem menuItem = createV1CompatAsyncMenuItem(currentDisplayProvider, item, position);

                // Convert slot position to row/column
                int col = position % 9;
                int row = position / 9;
                paneBuilder.item(col, row, menuItem);

                lastPosition = position;
                itemMap.put(position, item);
            }
        }

        // Process inputs (converted to interactive items)
        for (MenuInputMeta input : menu.getInputs()) {

            // Fail-fast validation for InputHandler
            if (input.getInputHandler() != null) {
                throw new UnsupportedOperationException("InputHandler is not supported in v2 compat layer. Use MenuItem.interactive() for editable slots.");
            }

            int[] positions = input.getPositionAsIntArr();

            for (int position : positions) {

                if (position == -1) {
                    position = lastPosition + 1;
                }

                if (position >= size) {
                    throw new IllegalArgumentException("position cannot be greater than menu size (" + position + " >= " + size + ")");
                }

                if (itemMap.containsKey(position) || inputMap.containsKey(position)) {
                    throw new IllegalArgumentException("item/input position cannot be overridden: " + position);
                }

                // Convert input to v2 interactive MenuItem
                MenuItem inputItem = MenuItem.item()
                    .interactive()
                    .build();

                // Convert slot position to row/column
                int col = position % 9;
                int row = position / 9;
                paneBuilder.item(col, row, inputItem);

                lastPosition = position;
                inputMap.put(position, input);
            }
        }

        // Build v2 menu
        menuBuilder.pane("main", paneBuilder.build());
        Menu v2Menu = menuBuilder.build();

        return new OkaeriMenu(menu, itemMap, inputMap, providerMap, this, v2Menu);
    }

    /**
     * Creates an AsyncMenuItem that wraps a v1 DisplayProvider.
     * <p>
     * This converts v1 DisplayProviders to v2 async items, extracting all ItemStack
     * properties (material, name, lore, amount) and providing proper caching with TTL.
     * <p>
     * DisplayProviders are treated as async data sources that may perform expensive
     * operations (database queries, API calls), similar to how v1 used async rendering.
     *
     * @param provider the v1 DisplayProvider to wrap
     * @param itemMeta the v1 item metadata
     * @param slot the slot position for cache key generation
     * @return an AsyncMenuItem wrapping the DisplayProvider
     */
    private static MenuItem createV1CompatAsyncMenuItem(DisplayProvider provider, MenuItemMeta itemMeta, int slot) {
        AsyncMenuItem.Builder asyncBuilder = AsyncMenuItem.itemAsync()
            .key("compat-item-" + slot)

            // DisplayProvider as async data source with viewer context
            // Context provides access to the viewer, allowing viewer-specific rendering
            .data(ctx -> provider.displayFor(ctx.getEntity(), itemMeta))

            // Loading state - use Material.AIR to be invisible during load
            .loading(MenuItem.item()
                .material(Material.GRAY_STAINED_GLASS_PANE)
                .name(" ")
                .build())

            // Loaded state - use ItemStack directly (already formatted by v1 DisplayProvider)
            .<ItemStack>loaded(stack -> {
                if (stack == null) {
                    return MenuItem.item().material(Material.AIR).build();
                }

                // This preserves the exact ItemStack from v1 DisplayProvider without re-processing
                MenuItem.Builder builder = MenuItem.item().from(ctx -> stack);

                // Add click handler
                if (itemMeta.getClickHandler() != null) {
                    builder.onClick(ctx -> {
                        // Create v1 MenuContext for backwards compatibility
                        MenuContext v1Context = MenuContext.builder()
                            .v2Context(ctx)
                            .action(MenuContext.Action.PICKUP)
                            .doer(ctx.getEntity())
                            .inventory(ctx.getEntity().getOpenInventory().getTopInventory())
                            .menuItem(itemMeta)
                            .item(ctx.getEvent().getCurrentItem())
                            .cursor(ctx.getEvent().getCursor())
                            .slot(ctx.getEvent().getRawSlot())
                            .clickType(ctx.getClickType())
                            .build();
                        itemMeta.getClickHandler().onClick(v1Context);
                    });
                }

                return builder.build();
            })

            // Error state - v1 had no error states, would just crash
            .error(ex -> MenuItem.item()
                .material(Material.RED_STAINED_GLASS_PANE)
                .name(" ")
                .build())

            // v1 called DisplayProvider fresh every render
            .ttl(Duration.ofMillis(200));

        // Click handler is added to the MenuItem returned from loaded() factory above
        return asyncBuilder.build();
    }
}
