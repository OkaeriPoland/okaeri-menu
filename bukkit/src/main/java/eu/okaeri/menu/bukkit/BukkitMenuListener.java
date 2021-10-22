package eu.okaeri.menu.bukkit;

import eu.okaeri.menu.core.handler.*;
import eu.okaeri.menu.core.meta.MenuInputMeta;
import eu.okaeri.menu.core.meta.MenuItemMeta;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Map;

import static org.bukkit.event.inventory.InventoryAction.*;

@RequiredArgsConstructor
public class BukkitMenuListener implements Listener {

    private final Plugin plugin;
    private final BukkitMenuProvider provider;

    @EventHandler
    public void handleDrag(InventoryDragEvent event) {

        ItemStack cursor = event.getCursor();
        Inventory inventory = event.getInventory();
        HumanEntity whoClicked = event.getWhoClicked();

        if (!this.provider.knowsInstance(inventory)) {
            return;
        }

        BukkitMenuInstance menu = this.provider.findInstance(inventory);
        if (menu == null) {
            // where menu (???) the heck happened???
            this.cancelAndClose(event, whoClicked);
            return;
        }

        // check how many of items were placed into non-custom gui
        int size = event.getInventory().getSize();
        long ownInventoryItems = event.getNewItems().keySet().stream()
                .filter(position -> position >= size)
                .count();

        // drag inside own inventory
        if (ownInventoryItems == event.getNewItems().size()) {
            return;
        }

        // mixed drag, not supported
        if (ownInventoryItems > 0) {
            event.setCancelled(true);
            return;
        }

        // drag in custom gui only, check for inputs
        for (Map.Entry<Integer, ItemStack> newItem : event.getNewItems().entrySet()) {

            int slot = newItem.getKey();
            ItemStack item = newItem.getValue();

            // no input for one of the items
            MenuInputMeta<HumanEntity, ItemStack> menuInput = menu.getInput(slot);
            if (menuInput == null) {
                event.setCancelled(true);
                return;
            }

            // allow input if no handler
            InputHandler<HumanEntity, ItemStack> inputHandler = menuInput.getInputHandler();
            if (inputHandler == null) {
                continue;
            }

            // call handler
            if (inputHandler.onInput(whoClicked, menuInput, cursor, item, slot)) {
                // item in gui
                continue;
            }

            // requested cancellation
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void handleClick(InventoryClickEvent event) {

        int slot = event.getSlot();
        InventoryAction action = event.getAction();
        Inventory clickedInventory = event.getClickedInventory();
        HumanEntity whoClicked = event.getWhoClicked();
        ItemStack currentItem = event.getCurrentItem();

        // clicked outside of the inventory
        if (clickedInventory == null) {

            InventoryView openInventory = whoClicked.getOpenInventory();
            if (openInventory == null) {
                return;
            }

            Inventory topInventory = openInventory.getTopInventory();
            if (topInventory == null) {
                return;
            }

            // known gui
            if (this.provider.knowsInstance(topInventory)) {

                // resolve current menu
                BukkitMenuInstance menu = this.provider.findInstance(topInventory);
                if (menu == null) {
                    // where menu (???) the heck happened???
                    this.cancelAndClose(event, whoClicked);
                    return;
                }

                // got it!
                OutsideClickHandler<HumanEntity, ItemStack> clickHandler = menu.getMeta().getOutsideClickHandler();
                if (clickHandler != null) {
                    clickHandler.onClick(whoClicked, event.getCursor());
                    return;
                }
            }

            // no applicable handler
            return;
        }

        // inventory not known, check for shift_left/shift_right input
        if (!this.provider.knowsInstance(clickedInventory)) {

            if (action != MOVE_TO_OTHER_INVENTORY) {
                return;
            }

            InventoryView openInventory = whoClicked.getOpenInventory();
            if (openInventory == null) {
                return;
            }

            Inventory topInventory = openInventory.getTopInventory();
            if (topInventory == null) {
                return;
            }

            if (!this.provider.knowsInstance(topInventory)) {
                return;
            }

            // FIXME: should this be handled by some special handler?
            event.setCancelled(true);
            return;
        }

        // standard click/input
        BukkitMenuInstance menu = this.provider.findInstance(clickedInventory);
        if (menu == null) {
            // where menu (???) the heck happened???
            this.cancelAndClose(event, whoClicked);
            return;
        }

        // input
        if ((action == PLACE_ALL) || (action == PLACE_ONE) || (action == PLACE_SOME) || (action == SWAP_WITH_CURSOR)) {

            // no input in this slot
            MenuInputMeta<HumanEntity, ItemStack> menuInput = menu.getInput(slot);
            if (menuInput == null) {
                event.setCancelled(true);
                return;
            }

            // allow input if no handler
            InputHandler<HumanEntity, ItemStack> inputHandler = menuInput.getInputHandler();
            if (inputHandler == null) {
                return;
            }

            // call handler
            if (inputHandler.onInput(whoClicked, menuInput, event.getCursor(), null, slot)) {
                // item in gui
                return;
            }

            // requested cancellation
            event.setCancelled(true);
            return;
        }

        // click
        MenuItemMeta<HumanEntity, ItemStack> menuItem = menu.getItem(slot);
        if (menuItem == null) {

            // meta not found, try fallback handler
            FallbackClickHandler<HumanEntity, ItemStack> fallbackClickHandler = menu.getMeta().getFallbackClickHandler();

            // no handler, just cancel
            if (fallbackClickHandler == null) {
                event.setCancelled(true);
                return;
            }

            // true - allow pickup
            if (fallbackClickHandler.onClick(whoClicked, currentItem, slot)) {
                return;
            }

            // false - deny pickup
            event.setCancelled(true);
            return;
        }

        ClickHandler<HumanEntity, ItemStack> clickHandler = menuItem.getClickHandler();
        if (clickHandler == null) {
            // meta does not have own click handler, just cancel
            event.setCancelled(true);
            return;
        }

        if (clickHandler.onClick(whoClicked, menuItem, currentItem, slot)) {
            // true - allow pickup
            return;
        }

        // false - deny pickup
        event.setCancelled(true);
    }

    @EventHandler
    public void handleClose(InventoryCloseEvent event) {

        Inventory inventory = event.getInventory();
        BukkitMenuInstance closedMenu = this.provider.removeInstance(inventory);

        if (closedMenu == null) {
            return;
        }

        CloseHandler<HumanEntity> closeHandler = closedMenu.getMeta().getCloseHandler();
        if (closeHandler == null) {
            return;
        }

        closeHandler.onClose(event.getPlayer());
    }

    private void cancelAndClose(Cancellable event, HumanEntity entity) {
        event.setCancelled(true);
        this.plugin.getServer().getScheduler().runTask(this.plugin, entity::closeInventory);
    }
}
