package eu.okaeri.menu.bukkit;

import eu.okaeri.menu.bukkit.meta.MenuItemMeta;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Logger;

@Builder
@Data(staticConstructor = "of")
public class MenuContext {

    private static final Logger LOGGER = Logger.getLogger(MenuContext.class.getSimpleName());

    private final Action action;
    private final HumanEntity doer;
    private final Inventory inventory;

    private final MenuItemMeta menuItem;
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

    public void runCommand(boolean log, @NonNull String... command) {
        for (String cmd : command) {
            if (log) {
                LOGGER.info(this.doer.getName() + " issued server command (via GUI): /" + cmd);
            }
            Bukkit.dispatchCommand(this.doer, cmd);
        }
    }

    public void runCommand(@NonNull String... command) {
        this.runCommand(true, command);
    }

    public void runCommandSilently(@NonNull String... command) {
        this.runCommand(false, command);
    }

    public void closeInventory() {
        this.doer.closeInventory();
    }

    enum Action {
        INPUT,
        PICKUP,
        CLOSE
    }
}
