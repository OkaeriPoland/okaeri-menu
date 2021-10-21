package eu.okaeri.menu.bukkit;

import eu.okaeri.menu.core.handler.*;
import eu.okaeri.menu.core.meta.MenuInputMeta;
import eu.okaeri.menu.core.meta.MenuItemMeta;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

import static org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY;

@RequiredArgsConstructor
public class BukkitMenuListener implements Listener {

    private final BukkitMenuProvider provider;

    @EventHandler
    public void handleDrag(InventoryDragEvent event) {

        System.out.println("drag");
        System.out.println(event.getResult());
        System.out.println(event.getCursor());

        Inventory inventory = event.getInventory();
        HumanEntity whoClicked = event.getWhoClicked();

        if (!this.provider.knowsInstance(inventory)) {
            return;
        }

        BukkitMenuInstance menu = this.provider.findInstance(inventory);
        if (menu == null) {
            // where menu (???) the heck happened???
            event.setCancelled(true);
            whoClicked.closeInventory();
            return;
        }

        // check for inputs
        for (Map.Entry<Integer, ItemStack> newItem : event.getNewItems().entrySet()) {

            int slot = newItem.getKey();
            ItemStack item = newItem.getValue();

            // do not allow input into gui elements
            MenuItemMeta<HumanEntity, ItemStack> menuItem = menu.getItem(slot);
            if (menuItem != null) {
                event.setCancelled(true);
                return;
            }

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
            if (inputHandler.onInput(whoClicked, menuInput, item)) {
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

        System.out.println("click");
        System.out.println(event.getAction());
        System.out.println(event.getClick());

        Inventory clickedInventory = event.getClickedInventory();
        HumanEntity whoClicked = event.getWhoClicked();

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
                    event.setCancelled(true);
                    whoClicked.closeInventory();
                    return;
                }

                // got it!
                OutsideClickHandler<HumanEntity> clickHandler = menu.getMeta().getOutsideClickHandler();
                if (clickHandler != null) {
                    clickHandler.onClick(whoClicked);
                    return;
                }
            }

            // no applicable handler
            return;
        }

        // inventory not known, check for input
        if (!this.provider.knowsInstance(clickedInventory)) {

            InventoryAction action = event.getAction();
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

            event.setCancelled(true); // FIXME: input handler
            return;
        }

        // standard click
        BukkitMenuInstance menu = this.provider.findInstance(clickedInventory);
        if (menu == null) {
            // where menu (???) the heck happened???
            event.setCancelled(true);
            whoClicked.closeInventory();
            return;
        }

        MenuItemMeta<HumanEntity, ItemStack> menuItem = menu.getItem(event.getSlot());
        if (menuItem == null) {
            // meta not found, try fallback handler
            FallbackClickHandler<HumanEntity, ItemStack> fallbackClickHandler = menu.getMeta().getFallbackClickHandler();
            if (fallbackClickHandler != null) {
                event.setCancelled(true);
                fallbackClickHandler.onClick(whoClicked, null, event.getCurrentItem());
            }
            return;
        }

        event.setCancelled(true);
        ClickHandler<HumanEntity, ItemStack> clickHandler = menuItem.getClickHandler();

        if (clickHandler == null) {
            // meta does not have own click handler, try fallback handler
            FallbackClickHandler<HumanEntity, ItemStack> fallbackClickHandler = menu.getMeta().getFallbackClickHandler();
            if (fallbackClickHandler != null) {
                fallbackClickHandler.onClick(whoClicked, menuItem, event.getCurrentItem());
            }
            return;
        }

        clickHandler.onClick(whoClicked, menuItem, event.getCurrentItem());
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
}
