package eu.okaeri.menu.bukkit;

import eu.okaeri.menu.core.meta.MenuDeclaration;
import lombok.Data;
import org.bukkit.inventory.Inventory;

@Data
public class BukkitMenuInstance {

    private final Inventory inventory;
    private final BukkitMenu menu;

    public MenuDeclaration getDeclaration() {
        return this.getMenu().getDeclaration();
    }
}
