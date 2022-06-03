package eu.okaeri.menu.bukkit;

import eu.okaeri.menu.core.display.DisplayProvider;
import eu.okaeri.menu.core.meta.*;
import lombok.Data;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

@Data
public class BukkitMenu {

    private final MenuMeta<HumanEntity, ItemStack, ClickType> meta;
    private final Map<Integer, MenuItemMeta<HumanEntity, ItemStack, ClickType>> itemMap;
    private final Map<Integer, MenuInputMeta<HumanEntity, ItemStack, ClickType>> inputMap;
    private final Map<Integer, DisplayProvider<HumanEntity, ItemStack, ClickType>> providerMap;
    private final BukkitMenuProvider menuProvider;

    public static MenuBuilder<HumanEntity, ItemStack, ClickType> builder() {
        return new MenuBuilder<>();
    }

    public static MenuItemBuilder<HumanEntity, ItemStack, ClickType> item() {
        return new MenuItemBuilder<>();
    }

    public static MenuInputBuilder<HumanEntity, ItemStack, ClickType> input() {
        return new MenuInputBuilder<>();
    }

    public BukkitMenuInstance createInstance() {

        int size = this.meta.getMenuChestSize();
        Inventory inventory = Bukkit.createInventory(null, size, this.meta.getName());

        BukkitMenuInstance menuInstance = new BukkitMenuInstance(inventory, this);
        this.menuProvider.trackInstance(inventory, menuInstance);

        return menuInstance;
    }

    public BukkitMenuInstance open(@NonNull HumanEntity viewer) {
        BukkitMenuInstance menuInstance = this.createInstance().render(viewer);
        viewer.openInventory(menuInstance.getInventory());
        return menuInstance;
    }
}
