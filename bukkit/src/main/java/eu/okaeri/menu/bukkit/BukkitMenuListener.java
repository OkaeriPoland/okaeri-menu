package eu.okaeri.menu.bukkit;

import eu.okaeri.menu.core.handler.ClickHandler;
import eu.okaeri.menu.core.handler.CloseHandler;
import eu.okaeri.menu.core.handler.FallbackClickHandler;
import eu.okaeri.menu.core.handler.OutsideClickHandler;
import eu.okaeri.menu.core.meta.MenuItemMeta;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import static org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY;

@RequiredArgsConstructor
public class BukkitMenuListener implements Listener {

    private final BukkitMenuProvider provider;

    @EventHandler
    public void handleClick(InventoryClickEvent event) {

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
