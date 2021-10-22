package eu.okaeri.menu.bukkit;

import eu.okaeri.menu.core.display.DisplayProvider;
import eu.okaeri.menu.core.meta.*;
import lombok.Data;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

@Data
public class BukkitMenu {

    private final MenuMeta<HumanEntity, ItemStack> meta;
    private final Map<Integer, MenuItemMeta<HumanEntity, ItemStack>> itemMap;
    private final Map<Integer, MenuInputMeta<HumanEntity, ItemStack>> inputMap;
    private final Map<Integer, DisplayProvider<HumanEntity, ItemStack>> providerMap;
    private final BukkitMenuProvider menuProvider;

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

    public static MenuBuilder<HumanEntity, ItemStack> builder() {
        return new MenuBuilder<>();
    }

    public static MenuItemBuilder<HumanEntity, ItemStack> item() {
        return new MenuItemBuilder<>();
    }

    public static MenuInputBuilder<HumanEntity, ItemStack> input() {
        return new MenuInputBuilder<>();
    }
}
