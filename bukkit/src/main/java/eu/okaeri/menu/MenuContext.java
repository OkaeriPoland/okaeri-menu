package eu.okaeri.menu;

import lombok.NonNull;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

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
}
