package eu.okaeri.menu.bukkit;

import eu.okaeri.menu.core.display.DisplayProvider;
import eu.okaeri.menu.core.meta.MenuItemMeta;
import eu.okaeri.menu.core.meta.MenuMeta;
import lombok.Data;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

@Data
public class BukkitMenuInstance {

    private final Inventory inventory;
    private final BukkitMenu menu;

    public MenuMeta<HumanEntity, ItemStack> getMeta() {
        return this.getMenu().getMeta();
    }

    public MenuItemMeta<HumanEntity, ItemStack> getItem(int slot) {
        return this.getMenu().getItemMap().get(slot);
    }

    public DisplayProvider<HumanEntity, ItemStack> getProvider(int slot) {
        return this.getMenu().getProviderMap().get(slot);
    }
}
