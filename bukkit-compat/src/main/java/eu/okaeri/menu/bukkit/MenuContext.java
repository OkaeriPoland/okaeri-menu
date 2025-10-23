package eu.okaeri.menu.bukkit;

import eu.okaeri.menu.bukkit.meta.MenuItemMeta;
import eu.okaeri.menu.item.MenuItemClickContext;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Logger;

/**
 * Compatibility adapter for MenuContext v1 API.
 * Bridges old v1 API to new v2 API by delegating to {@link MenuItemClickContext}.
 *
 * @deprecated Use the new 2.0 API with {@link MenuItemClickContext} and {@link eu.okaeri.menu.BaseMenuContext}
 */
@Builder
@Data(staticConstructor = "of")
@Deprecated(since = "0.0.18", forRemoval = true)
public class MenuContext {

    private static final Logger LOGGER = Logger.getLogger(MenuContext.class.getSimpleName());

    /**
     * Internal reference to the new v2 context.
     * This field holds the new MenuItemClickContext that this adapter wraps.
     */
    private final MenuItemClickContext v2Context;

    // Legacy v1 fields
    private final Action action;
    private final HumanEntity doer;
    private final Inventory inventory;

    private final MenuItemMeta menuItem;
    private final ItemStack item;

    private final ItemStack cursor;
    private final int slot;
    private final ClickType clickType;

    @Builder.Default private boolean allowPickup = false;
    @Builder.Default private boolean allowInput = false;

    /**
     * Gets the HumanEntity that clicked.
     *
     * @deprecated Use {@link #doer} field directly
     * @return The player who clicked
     */
    @Deprecated
    public HumanEntity getWhoClicked() {
        return this.doer;
    }

    /**
     * Gets the player (casted from doer).
     *
     * @return The player who clicked
     */
    public Player getPlayer() {
        return (Player) this.doer;
    }

    /**
     * Sends a message to the player.
     * If a v2Context is available, delegates to the new API.
     * Otherwise falls back to direct message sending.
     *
     * @param text The message to send
     */
    public void sendMessage(@NonNull String text) {
        if (this.v2Context != null) {
            // Delegate to v2 API which supports MessageProvider
            this.v2Context.sendMessage(text);
        } else {
            // Fallback for legacy usage
            this.doer.sendMessage(text);
        }
    }

    /**
     * Executes commands as the player with logging.
     *
     * @param log Whether to log the command execution
     * @param command Commands to execute
     */
    public void runCommand(boolean log, @NonNull String... command) {
        for (String cmd : command) {
            if (log) {
                LOGGER.info(this.doer.getName() + " issued server command (via GUI): /" + cmd);
            }
            Bukkit.dispatchCommand(this.doer, cmd);
        }
    }

    /**
     * Executes commands as the player with logging enabled.
     *
     * @param command Commands to execute
     */
    public void runCommand(@NonNull String... command) {
        this.runCommand(true, command);
    }

    /**
     * Executes commands as the player without logging.
     *
     * @param command Commands to execute
     */
    public void runCommandSilently(@NonNull String... command) {
        this.runCommand(false, command);
    }

    /**
     * Closes the inventory for this player.
     * If a v2Context is available, delegates to the new API.
     * Otherwise falls back to direct inventory closing.
     */
    public void closeInventory() {
        if (this.v2Context != null) {
            // Delegate to v2 API which also updates menu state
            this.v2Context.closeInventory();
        } else {
            // Fallback for legacy usage
            this.doer.closeInventory();
        }
    }

    /**
     * Action type for menu interactions.
     */
    enum Action {
        /**
         * Player is inputting an item into a slot.
         */
        INPUT,

        /**
         * Player is picking up an item from a slot.
         */
        PICKUP,

        /**
         * Player is closing the menu.
         */
        CLOSE
    }
}
