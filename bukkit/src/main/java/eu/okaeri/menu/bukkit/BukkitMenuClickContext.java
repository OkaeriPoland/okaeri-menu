package eu.okaeri.menu.bukkit;

import eu.okaeri.menu.core.meta.MenuItemMeta;
import lombok.Data;
import lombok.NonNull;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

@Data(staticConstructor = "of")
public class BukkitMenuClickContext {

    public static BukkitMenuClickContext of(HumanEntity whoClicked, ItemStack cursor, ClickType clickType) {
        return new BukkitMenuClickContext(whoClicked, null, cursor, -1, clickType);
    }

    private final HumanEntity whoClicked;
    private final MenuItemMeta<HumanEntity, ItemStack, BukkitMenuClickContext> menuItem;
    private final ItemStack item;
    private final int slot;
    private final ClickType clickType;

    private boolean allowPickup = false;

    public Player getPlayer() {
        return (Player) this.whoClicked;
    }

    public void sendMessage(@NonNull String text) {
        this.whoClicked.sendMessage(text);
    }
}
