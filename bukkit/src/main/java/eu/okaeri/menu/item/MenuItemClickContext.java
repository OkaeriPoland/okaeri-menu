package eu.okaeri.menu.item;

import eu.okaeri.menu.Menu;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Context for menu item click events.
 * Provides access to the menu and helper methods for common operations.
 */
@Getter
public class MenuItemClickContext extends MenuItemContext {

    private final ClickType clickType;

    public MenuItemClickContext(
        @NonNull Menu menu,
        @NonNull HumanEntity player,
        @NonNull InventoryClickEvent event,
        int slot,
        @NonNull ClickType clickType
    ) {
        super(menu, player, event, slot);
        this.clickType = clickType;
    }

    public boolean isLeftClick() {
        return (this.clickType == ClickType.LEFT) || (this.clickType == ClickType.SHIFT_LEFT);
    }

    public boolean isRightClick() {
        return (this.clickType == ClickType.RIGHT) || (this.clickType == ClickType.SHIFT_RIGHT);
    }

    public boolean isShiftClick() {
        return (this.clickType == ClickType.SHIFT_LEFT) || (this.clickType == ClickType.SHIFT_RIGHT);
    }

    public boolean isMiddleClick() {
        return this.clickType == ClickType.MIDDLE;
    }

    /**
     * Checks if the current click type matches any of the provided types.
     * Useful for checking multiple click types at once.
     *
     * @param types The click types to check against
     * @return true if the current click type matches any of the provided types
     */
    public boolean is(@NonNull ClickType... types) {
        for (ClickType type : types) {
            if (this.clickType == type) {
                return true;
            }
        }
        return false;
    }
}
