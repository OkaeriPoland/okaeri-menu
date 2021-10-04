package eu.okaeri.menu.bukkit;

import eu.okaeri.menu.core.display.DisplayProvider;
import eu.okaeri.menu.core.meta.MenuBuilder;
import eu.okaeri.menu.core.meta.MenuDeclaration;
import eu.okaeri.menu.core.meta.MenuItemBuilder;
import eu.okaeri.menu.core.meta.MenuItemDeclaration;
import lombok.Data;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

@Data
public class BukkitMenu {

    private final MenuDeclaration declaration;
    private final Map<Integer, MenuItemDeclaration<HumanEntity, ItemStack>> itemMap;
    private final Map<Integer, DisplayProvider<HumanEntity, ItemStack>> providerMap;
    private final BukkitMenuProvider menuProvider;

    public BukkitMenuInstance open(@NonNull HumanEntity viewer) {

        int size = this.declaration.getMenuChestSize();
        Inventory inventory = Bukkit.createInventory(null, size, this.declaration.getName());

        this.itemMap.forEach((position, item) -> {
            ItemStack itemStack = this.providerMap.get(position).displayFor(viewer, item);
            inventory.setItem(position, itemStack);
        });

        viewer.openInventory(inventory);
        BukkitMenuInstance menuInstance = new BukkitMenuInstance(inventory, this);
        this.menuProvider.trackInstance(inventory, menuInstance);

        return menuInstance;
    }

    public static MenuBuilder<HumanEntity, ItemStack> builder() {
        return new MenuBuilder<>();
    }

    public static MenuItemBuilder<HumanEntity, ItemStack> item() {
        return new MenuItemBuilder<>();
    }
}
