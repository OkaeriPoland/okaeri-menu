package eu.okaeri.menu.bukkit;

import eu.okaeri.menu.bukkit.display.DisplayProvider;
import eu.okaeri.menu.bukkit.meta.MenuInputMeta;
import eu.okaeri.menu.bukkit.meta.MenuItemMeta;
import eu.okaeri.menu.bukkit.meta.MenuMeta;
import lombok.Data;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a menu instance with an inventory and associated menu configuration.
 * <p>
 * This class is part of the compatibility layer for the v1 menu API. It manages
 * the lifecycle of a menu including rendering, opening, and updating.
 * <p>
 * Users should migrate to the v2 API as this compatibility layer will be removed
 * in a future release.
 *
 * @deprecated since 0.0.18, scheduled for removal in a future version.
 *             Use the v2 menu API instead.
 */
@Data
@Deprecated(since = "0.0.18", forRemoval = true)
public class MenuInstance {

    private static final Logger LOGGER = Logger.getLogger(MenuInstance.class.getSimpleName());

    private final Inventory inventory;
    private final OkaeriMenu menu;

    private Instant lastRenderTime;
    private boolean lastRenderAsync;
    private UUID lastRenderTarget;

    /**
     * Gets the menu metadata.
     *
     * @return the MenuMeta associated with this instance
     */
    public MenuMeta getMeta() {
        return this.getMenu().getMeta();
    }

    /**
     * Gets the menu item at the specified slot.
     *
     * @param slot the slot position
     * @return the MenuItemMeta at that position, or null if none exists
     */
    public MenuItemMeta getItem(int slot) {
        return this.getMenu().getItemMap().get(slot);
    }

    /**
     * Gets the display provider for the specified slot.
     *
     * @param slot the slot position
     * @return the DisplayProvider at that position, or null if none exists
     */
    public DisplayProvider getProvider(int slot) {
        return this.getMenu().getProviderMap().get(slot);
    }

    /**
     * Gets the input configuration for the specified slot.
     *
     * @param slot the slot position
     * @return the MenuInputMeta at that position, or null if none exists
     */
    public MenuInputMeta getInput(int slot) {
        return this.getMenu().getInputMap().get(slot);
    }

    /**
     * Renders the menu for the specified viewer.
     * <p>
     * In the v2 translation layer, this delegates to Menu.refresh() to trigger
     * re-rendering of all menu items using the v2 reactive system.
     * <p>
     * If unoptimized render warnings are enabled and this is called on the main thread,
     * a warning will be logged.
     *
     * @param viewer the entity to render the menu for
     * @return this MenuInstance for chaining
     */
    public MenuInstance render(@NonNull HumanEntity viewer) {

        if (this.getMenu().getProvider().isWarnUnoptimizedRender() && Bukkit.isPrimaryThread()) {
            LOGGER.log(Level.WARNING, "Unoptimized synchronous render detected", new Throwable());
        }

        // Delegate to v2 Menu.refresh() for reactive rendering
        Player player = (Player) viewer;
        this.getMenu().getV2Menu().refresh(player);

        this.setLastRenderTime(Instant.now());
        this.setLastRenderAsync(!Bukkit.isPrimaryThread());
        this.setLastRenderTarget(viewer.getUniqueId()); // say no to leaks!

        return this;
    }

    /**
     * Opens this menu for the specified viewer.
     *
     * @param viewer the entity to open the menu for
     * @return this MenuInstance for chaining
     */
    public MenuInstance open(@NonNull HumanEntity viewer) {
        return this.open(null, viewer);
    }

    /**
     * Opens this menu for the specified viewer with a parent menu context.
     * <p>
     * In the v2 translation layer, this delegates to Menu.open() directly.
     * The parent parameter is ignored as v2 uses NavigationHistory for back navigation.
     *
     * @param parent the parent menu instance, or null if no parent (ignored in v2)
     * @param viewer the entity to open the menu for
     * @return this MenuInstance for chaining
     */
    public MenuInstance open(MenuInstance parent, @NonNull HumanEntity viewer) {
        Player player = (Player) viewer;
        this.getMenu().getV2Menu().open(player);
        return this;
    }

    /**
     * Safely opens this menu for the specified viewer.
     * <p>
     * This ensures the operation is performed on the main thread if necessary.
     *
     * @param viewer the entity to open the menu for
     * @return this MenuInstance for chaining
     */
    public MenuInstance openSafely(@NonNull HumanEntity viewer) {
        return this.openSafely(null, viewer);
    }

    /**
     * Safely opens this menu for the specified viewer with a parent menu context.
     * <p>
     * This ensures the operation is performed on the main thread if necessary.
     * In the v2 translation layer, this delegates to Menu.open() which handles sync internally.
     *
     * @param parent the parent menu instance, or null if no parent (ignored in v2)
     * @param viewer the entity to open the menu for
     * @return this MenuInstance for chaining
     */
    public MenuInstance openSafely(MenuInstance parent, @NonNull HumanEntity viewer) {
        Player player = (Player) viewer;
        // v2 Menu.open() handles sync internally via WaitForDataTask
        this.getMenu().getV2Menu().open(player);
        return this;
    }

    /**
     * Updates this menu instance.
     * <p>
     * If an update hook is configured, it will be called. Otherwise, the menu
     * will be re-rendered for the last viewer who saw it by delegating to v2 Menu.refresh().
     * <p>
     * If the last render target is no longer online, this method does nothing.
     *
     * @return this MenuInstance for chaining
     */
    public MenuInstance update() {

        Player target = Bukkit.getPlayer(this.getLastRenderTarget());
        if (target == null) {
            return this;
        }

        if (this.getMeta().getUpdateHook() != null) {
            this.getMeta().getUpdateHook().onUpdate(this, target);
            return this;
        }

        // Delegate to v2 Menu.refresh()
        this.getMenu().getV2Menu().refresh(target);
        this.setLastRenderTime(Instant.now());
        return this;
    }
}
