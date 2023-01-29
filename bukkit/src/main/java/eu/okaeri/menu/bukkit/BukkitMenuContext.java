package eu.okaeri.menu.bukkit;

import eu.okaeri.menu.core.meta.MenuItemMeta;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

@Builder
@Data(staticConstructor = "of")
public class BukkitMenuContext {

    private final Action action;
    private final HumanEntity doer;
    private final Inventory inventory;

    private final MenuItemMeta<HumanEntity, ItemStack, BukkitMenuContext> menuItem;
    private final ItemStack item;

    private final ItemStack cursor;
    private final int slot;
    private final ClickType clickType;

    @Builder.Default private boolean allowPickup = false;
    @Builder.Default private boolean allowInput = false;

    @Deprecated
    public HumanEntity getWhoClicked() {
        return this.doer;
    }

    public Player getPlayer() {
        return (Player) this.doer;
    }

    public void sendMessage(@NonNull String text) {
        this.doer.sendMessage(text);
    }

    enum Action {
        INPUT,
        PICKUP,
        CLOSE
    }
}
