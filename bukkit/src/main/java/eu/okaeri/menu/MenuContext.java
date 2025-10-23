package eu.okaeri.menu;

import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

/**
 * Context provided to menu event handlers.
 * Provides access to the menu, viewer, and utility methods for navigation, refresh, etc.
 */
public class MenuContext extends BaseMenuContext {

    public MenuContext(@NonNull Menu menu, @NonNull HumanEntity entity) {
        super(menu, entity);
    }

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
