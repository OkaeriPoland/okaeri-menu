package eu.okaeri.menu.bukkit.display;

import eu.okaeri.menu.bukkit.meta.MenuItemMeta;
import lombok.NonNull;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;

public interface DisplayProvider {
    ItemStack displayFor(@NonNull HumanEntity viewer, @NonNull MenuItemMeta menuItem);
}
