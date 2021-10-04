package eu.okaeri.menu.bukkit;

import eu.okaeri.menu.core.handler.ClickHandler;
import eu.okaeri.menu.core.meta.MenuItemDeclaration;
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

        BukkitMenuInstance menu = this.provider.findInstance(clickedInventory);
        MenuItemDeclaration<HumanEntity, ItemStack> menuItem = menu.getMenu().getItemMap().get(event.getSlot());

        if (menuItem == null) {
            event.setCancelled(true); // FIXME: fallback click handler [?]
            return;
        }

        event.setCancelled(true);
        ClickHandler<HumanEntity, ItemStack> clickHandler = menuItem.getClickHandler();

        if (clickHandler == null) {
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

        // TODO: trigger event?
        System.out.println("closed " + closedMenu);
    }
}
