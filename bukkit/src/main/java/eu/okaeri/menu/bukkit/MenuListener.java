package eu.okaeri.menu.bukkit;

import eu.okaeri.menu.bukkit.handler.ClickHandler;
import eu.okaeri.menu.bukkit.handler.CloseHandler;
import eu.okaeri.menu.bukkit.handler.InputHandler;
import eu.okaeri.menu.bukkit.meta.MenuInputMeta;
import eu.okaeri.menu.bukkit.meta.MenuItemMeta;
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
public class MenuListener implements Listener {

    private final Plugin plugin;
    private final MenuProvider provider;

    @EventHandler
    public void handleDrag(InventoryDragEvent event) {

        ItemStack cursor = event.getCursor();
        Inventory inventory = event.getInventory();
        HumanEntity whoClicked = event.getWhoClicked();

        if (!this.provider.knowsInstance(inventory)) {
            return;
        }

        MenuInstance menu = this.provider.findInstance(inventory).orElse(null);
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
            MenuInputMeta menuInput = menu.getInput(slot);
            if (menuInput == null) {

                // try fallback handler
                ClickHandler fallbackClickHandler = menu.getMeta().getFallbackClickHandler();

                // no handler, just cancel
                if (fallbackClickHandler == null) {
                    event.setCancelled(true);
                    return;
                }

                // build context
                MenuContext context = MenuContext.builder()
                    .action(MenuContext.Action.INPUT)
                    .inventory(inventory)
                    .doer(whoClicked)
                    .cursor(cursor)
                    .item(item)
                    .slot(slot)
                    .build();

                try {
                    fallbackClickHandler.onClick(context);
                } catch (Exception exception) {
                    event.setCancelled(true);
                    throw new RuntimeException("failed fallbackClickHandler for drag input", exception);
                }

                // true - allow input
                if (context.isAllowInput()) {
                    return;
                }

                // false - deny input
                event.setCancelled(true);
                return;
            }

            // allow input if no handler
            InputHandler inputHandler = menuInput.getInputHandler();
            if (inputHandler == null) {
                continue;
            }

            // call handler // FIXME: error handling
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
        ClickType clickType = event.getClick();
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
                MenuInstance menu = this.provider.findInstance(topInventory).orElse(null);
                if (menu == null) {
                    // where menu (???) the heck happened???
                    this.cancelAndClose(event, whoClicked);
                    return;
                }

                // got it!
                ClickHandler clickHandler = menu.getMeta().getOutsideClickHandler();
                if (clickHandler != null) {
                    MenuContext context = MenuContext.builder()
                        .action(MenuContext.Action.PICKUP)
                        .inventory(topInventory)
                        .doer(whoClicked)
                        .item(event.getCursor())
                        .slot(slot)
                        .clickType(clickType)
                        .build();
                    clickHandler.onClick(context);
                    return;
                }
            }

            // no applicable handler
            return;
        }

        // inventory not known, check for shift_left/shift_right input (MOVE_TO_OTHER_INVENTORY) and COLLECT_TO_CURSOR
        if (!this.provider.knowsInstance(clickedInventory)) {

            if ((action != MOVE_TO_OTHER_INVENTORY) && (action != COLLECT_TO_CURSOR)) {
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
        MenuInstance menu = this.provider.findInstance(clickedInventory).orElse(null);
        if (menu == null) {
            // where menu (???) the heck happened???
            this.cancelAndClose(event, whoClicked);
            return;
        }

        // input
        if ((action == PLACE_ALL) || (action == PLACE_ONE) || (action == PLACE_SOME) || (action == SWAP_WITH_CURSOR)) {

            // no input in this slot
            MenuInputMeta menuInput = menu.getInput(slot);
            if (menuInput == null) {

                // try fallback handler
                ClickHandler fallbackClickHandler = menu.getMeta().getFallbackClickHandler();

                // no handler, just cancel
                if (fallbackClickHandler == null) {
                    event.setCancelled(true);
                    return;
                }

                // build context
                MenuContext context = MenuContext.builder()
                    .action(MenuContext.Action.INPUT)
                    .inventory(clickedInventory)
                    .doer(whoClicked)
                    .item(currentItem)
                    .slot(slot)
                    .clickType(clickType)
                    .build();

                try {
                    fallbackClickHandler.onClick(context);
                } catch (Exception exception) {
                    event.setCancelled(true);
                    throw new RuntimeException("failed fallbackClickHandler for input", exception);
                }

                // true - allow input
                if (context.isAllowInput()) {
                    return;
                }

                // false - deny input
                event.setCancelled(true);
                return;
            }

            // allow input if no handler
            InputHandler inputHandler = menuInput.getInputHandler();
            if (inputHandler == null) {
                return;
            }

            // call handler // FIXME: error handling
            if (inputHandler.onInput(whoClicked, menuInput, event.getCursor(), null, slot)) {
                // item in gui
                return;
            }

            // requested cancellation
            event.setCancelled(true);
            return;
        }

        // click
        MenuItemMeta menuItem = menu.getItem(slot);
        if (menuItem == null) {

            // meta not found, try fallback handler
            ClickHandler fallbackClickHandler = menu.getMeta().getFallbackClickHandler();

            // no handler, just cancel
            if (fallbackClickHandler == null) {
                event.setCancelled(true);
                return;
            }

            // build context
            MenuContext context = MenuContext.builder()
                .action(MenuContext.Action.PICKUP)
                .inventory(clickedInventory)
                .doer(whoClicked)
                .item(currentItem)
                .slot(slot)
                .clickType(clickType)
                .build();

            try {
                fallbackClickHandler.onClick(context);
            } catch (Exception exception) {
                event.setCancelled(true);
                throw new RuntimeException("failed fallbackClickHandler for pickup", exception);
            }

            // true - allow pickup
            if (context.isAllowPickup()) {
                return;
            }

            // false - deny pickup
            event.setCancelled(true);
            return;
        }

        ClickHandler clickHandler = menuItem.getClickHandler();
        if (clickHandler == null) {
            // meta does not have own click handler, just cancel
            event.setCancelled(true);
            return;
        }

        // build context
        MenuContext context = MenuContext.builder()
            .action(MenuContext.Action.PICKUP)
            .inventory(clickedInventory)
            .doer(whoClicked)
            .item(currentItem)
            .menuItem(menuItem)
            .slot(slot)
            .clickType(clickType)
            .build();

        try {
            clickHandler.onClick(context);
        } catch (Exception exception) {
            event.setCancelled(true);
            throw new RuntimeException("failed clickHandler for pickup", exception);
        }

        // true - allow pickup
        if (context.isAllowPickup()) {
            return;
        }

        // false - deny pickup
        event.setCancelled(true);
    }

    @EventHandler
    public void handleClose(InventoryCloseEvent event) {

        Inventory inventory = event.getInventory();
        MenuInstance closedMenu = this.provider.removeInstance(inventory);

        if (closedMenu == null) {
            return;
        }

        CloseHandler closeHandler = closedMenu.getMeta().getCloseHandler();
        if (closeHandler == null) {
            return;
        }

        MenuContext context = MenuContext.builder()
            .action(MenuContext.Action.CLOSE)
            .inventory(inventory)
            .doer(event.getPlayer())
            .build();

        closeHandler.onClose(context);
    }

    private void cancelAndClose(Cancellable event, HumanEntity entity) {
        event.setCancelled(true);
        this.plugin.getServer().getScheduler().runTask(this.plugin, entity::closeInventory);
    }
}
