package eu.okaeri.menu.bukkit.display;

import eu.okaeri.menu.bukkit.meta.MenuItemMeta;
import lombok.NonNull;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;

/**
 * @deprecated Use the new 2.0 API with reactive properties: {@link eu.okaeri.menu.item.MenuItem.Builder#material(java.util.function.Supplier)}
 */
@Deprecated(since = "0.0.18", forRemoval = true)
public interface DisplayProvider {
    ItemStack displayFor(@NonNull HumanEntity viewer, @NonNull MenuItemMeta menuItem);
}
