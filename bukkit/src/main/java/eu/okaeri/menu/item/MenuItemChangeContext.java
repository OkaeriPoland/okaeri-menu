package eu.okaeri.menu.item;

import eu.okaeri.menu.Menu;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Context for menu item change events (interactive slots).
 * Provides information about items being placed or removed.
 */
@Getter
public class MenuItemChangeContext extends BaseMenuItemContext {

    private final ItemStack previousItem;
    private final ItemStack newItem;

    public MenuItemChangeContext(@NonNull Menu menu, @NonNull HumanEntity player, @NonNull InventoryClickEvent event, int slot,
                                 ItemStack previousItem, ItemStack newItem) {
        super(menu, player, event, slot);
        this.previousItem = previousItem;
        this.newItem = newItem;
    }

    /**
     * Checks if an item was placed into the slot.
     */
    public boolean wasItemPlaced() {
        return ((this.previousItem == null) || this.previousItem.getType().isAir()) &&
            ((this.newItem != null) && !this.newItem.getType().isAir());
    }

    /**
     * Checks if an item was removed from the slot.
     */
    public boolean wasItemRemoved() {
        return ((this.previousItem != null) && !this.previousItem.getType().isAir()) &&
            ((this.newItem == null) || this.newItem.getType().isAir());
    }

    /**
     * Checks if the item was swapped/changed.
     */
    public boolean wasItemSwapped() {
        return ((this.previousItem != null) && !this.previousItem.getType().isAir()) &&
            ((this.newItem != null) && !this.newItem.getType().isAir()) &&
            !this.previousItem.isSimilar(this.newItem);
    }

    /**
     * Checks if the item amount increased (adding to stack).
     */
    public boolean wasItemAmountIncreased() {
        return (this.previousItem != null) && !this.previousItem.getType().isAir() && (this.newItem != null) && !this.newItem.getType().isAir() && this.previousItem.isSimilar(this.newItem) && (this.newItem.getAmount() > this.previousItem.getAmount());
    }

    /**
     * Checks if the item amount decreased (partial pickup).
     */
    public boolean wasItemAmountDecreased() {
        return (this.previousItem != null) && !this.previousItem.getType().isAir() && (this.newItem != null) && !this.newItem.getType().isAir() && this.previousItem.isSimilar(this.newItem) && (this.newItem.getAmount() < this.previousItem.getAmount());
    }
}
