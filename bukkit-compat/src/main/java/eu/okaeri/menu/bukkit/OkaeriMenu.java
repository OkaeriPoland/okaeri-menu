package eu.okaeri.menu.bukkit;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.bukkit.display.DisplayProvider;
import eu.okaeri.menu.bukkit.meta.*;
import lombok.Data;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Compatibility layer for v1 menu API.
 * <p>
 * This class provides backward compatibility with the v1 menu system by translating
 * v1 API calls to v2 Menu objects internally. The v1 API is preserved for backward
 * compatibility, but all operations delegate to the v2 menu system.
 * <p>
 * Users should migrate to the v2 API as this compatibility layer will be removed
 * in a future release.
 *
 * @deprecated since 0.0.18, scheduled for removal in a future version.
 *             Use the v2 menu API with {@link Menu.Builder} instead.
 */
@Data
@Deprecated(since = "0.0.18", forRemoval = true)
public class OkaeriMenu {

    private final MenuMeta meta;
    private final Map<Integer, MenuItemMeta> itemMap;
    private final Map<Integer, MenuInputMeta> inputMap;
    private final Map<Integer, DisplayProvider> providerMap;
    private final MenuProvider provider;
    private final Menu v2Menu;

    /**
     * Creates a new menu builder for constructing menus.
     *
     * @return a new MenuBuilder instance
     */
    public static MenuBuilder builder() {
        return new MenuBuilder();
    }

    /**
     * Creates a new menu item builder for constructing menu items.
     *
     * @return a new MenuItemBuilder instance
     */
    public static MenuItemBuilder item() {
        return new MenuItemBuilder();
    }

    /**
     * Creates a new display stack for managing item displays.
     *
     * @return a new MenuDisplayStack instance
     */
    public static MenuDisplayStack stack() {
        return new MenuDisplayStack();
    }

    /**
     * Creates a new input builder for constructing input slots.
     *
     * @return a new MenuInputBuilder instance
     */
    public static MenuInputBuilder input() {
        return new MenuInputBuilder();
    }

    /**
     * Creates an editor menu that allows players to arrange items.
     * <p>
     * This creates a 6-row chest menu where items can be freely moved and modified.
     * When the menu is closed, the callback is invoked with the final list of items.
     *
     * @param stacks the initial items to populate the editor (max 54 items)
     * @param name the display name of the menu
     * @param callback consumer called with the final item list when menu is closed
     * @return MenuMeta for the editor menu
     * @throws IllegalArgumentException if stacks contains more than 54 items
     */
    public static MenuMeta editor(@NonNull List<ItemStack> stacks, String name, @NonNull Consumer<List<ItemStack>> callback) {

        if (stacks.size() > (6 * 9)) {
            throw new IllegalArgumentException("Stacks is too large for the 6 row editor (size: " + stacks + ")");
        }

        return OkaeriMenu.builder()
            .rows(6)
            .name(name)
            .items(stacks.stream()
                .map(stack -> OkaeriMenu.item()
                    .display(() -> stack)
                    .click(ctx -> ctx.setAllowPickup(true))
                    .build()))
            .close(ctx -> callback.accept(Arrays.stream(ctx.getInventory().getContents())
                .filter(Objects::nonNull)
                .filter(stack -> (stack.getType() != Material.AIR) && !stack.getType().name().contains("_AIR"))
                .collect(Collectors.toList())))
            .fallbackClick(ctx -> {
                ctx.setAllowInput(true);
                ctx.setAllowPickup(true);
            })
            .build();
    }

    /**
     * Creates a new instance of this menu.
     * <p>
     * For compatibility with v1 API. This method opens the v2 menu and returns
     * a wrapper MenuInstance.
     *
     * @return a new MenuInstance wrapping the v2 menu
     * @deprecated Use {@link #open(HumanEntity)} directly instead
     */
    @Deprecated
    public MenuInstance createInstance() {
        // For v1 compatibility, return a dummy instance
        // The actual menu is managed by v2
        Inventory inventory = this.getMeta().getFactory().apply(this.getMeta());
        return new MenuInstance(inventory, this);
    }

    /**
     * Renders this menu for the specified viewer.
     * <p>
     * In v1, this method would render items without opening the inventory, allowing
     * async rendering patterns. In the compat layer, we map this to v2's wait-for-data
     * opening to maintain similar behavior - waiting for DisplayProviders to execute
     * before showing the menu (mimics v1 blocking behavior).
     * <p>
     * This supports the v1 async rendering pattern:
     * <pre>
     * CompletableFuture.runAsync(() -> menu.render(player))
     *     .thenAccept(instance -> instance.open(player));
     * </pre>
     *
     * @param viewer the player to render the menu for
     * @return a MenuInstance wrapper (for API compatibility)
     */
    public MenuInstance render(@NonNull HumanEntity viewer) {
        Player player = (Player) viewer;

        // v1 render() waited for DisplayProviders to execute
        // Map to v2 open with 2 second timeout (waits for async data)
        this.v2Menu.open(player, java.time.Duration.ofSeconds(2));

        // Return wrapper for v1 API compatibility
        Inventory inventory = viewer.getOpenInventory().getTopInventory();
        return new MenuInstance(inventory, this);
    }

    /**
     * Opens this menu for the specified viewer.
     * <p>
     * In v1, this could be called after render() or standalone. We guard against
     * double-opening if the viewer is already viewing this menu.
     * <p>
     * When used after render(), the guard prevents double-opening. When used standalone,
     * it opens immediately without waiting for async data.
     *
     * @param viewer the player to open the menu for
     * @return a MenuInstance wrapper (for API compatibility)
     */
    public MenuInstance open(@NonNull HumanEntity viewer) {
        Player player = (Player) viewer;

        // Check if player is already viewing this menu by checking inventory holder
        Inventory currentInventory = player.getOpenInventory().getTopInventory();
        boolean alreadyViewing = currentInventory.getHolder() == this.v2Menu;

        if (!alreadyViewing) {
            // Not viewing - open immediately (no wait, immediate mode)
            this.v2Menu.open(player);
        }

        // Return wrapper for v1 API compatibility
        Inventory inventory = viewer.getOpenInventory().getTopInventory();
        return new MenuInstance(inventory, this);
    }
}
