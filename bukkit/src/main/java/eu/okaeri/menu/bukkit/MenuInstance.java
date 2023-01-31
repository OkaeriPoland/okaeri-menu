package eu.okaeri.menu.bukkit;

import eu.okaeri.menu.bukkit.display.DisplayProvider;
import eu.okaeri.menu.bukkit.meta.MenuInputMeta;
import eu.okaeri.menu.bukkit.meta.MenuItemMeta;
import eu.okaeri.menu.bukkit.meta.MenuMeta;
import lombok.Data;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Data
public class MenuInstance {

    private static final Logger LOGGER = Logger.getLogger(MenuInstance.class.getSimpleName());

    private final Inventory inventory;
    private final OkaeriMenu menu;

    private Instant lastRenderTime;
    private boolean lastRenderAsync;
    private UUID lastRenderTarget;

    public MenuMeta getMeta() {
        return this.getMenu().getMeta();
    }

    public MenuItemMeta getItem(int slot) {
        return this.getMenu().getItemMap().get(slot);
    }

    public DisplayProvider getProvider(int slot) {
        return this.getMenu().getProviderMap().get(slot);
    }

    public MenuInputMeta getInput(int slot) {
        return this.getMenu().getInputMap().get(slot);
    }

    public MenuInstance render(@NonNull HumanEntity viewer) {

        if (this.getMenu().getMenuProvider().isWarnUnoptimizedRender() && Bukkit.isPrimaryThread()) {
            LOGGER.log(Level.WARNING, "Unoptimized synchronous render detected", new Throwable());
        }

        this.getMenu().getItemMap().forEach((position, item) -> {
            ItemStack itemStack = this.getMenu().getProviderMap().get(position).displayFor(viewer, item);
            this.getInventory().setItem(position, itemStack);
        });

        this.setLastRenderTime(Instant.now());
        this.setLastRenderAsync(!Bukkit.isPrimaryThread());
        this.setLastRenderTarget(viewer.getUniqueId()); // say no to leaks!

        return this;
    }

    public MenuInstance open(@NonNull HumanEntity viewer) {
        if (!this.getMenu().getMenuProvider().knowsInstance(this.getInventory())) {
            // ignore due to untracked instance
            return this;
        }
        viewer.openInventory(this.getInventory());
        return this;
    }

    public MenuInstance open(MenuInstance parent, @NonNull HumanEntity viewer) {
        if ((parent != null) && !parent.getMenu().getMenuProvider().knowsInstance(parent.getInventory())) {
            // ignore due to untracked parent instance
            return this;
        }
        return this.open(viewer);
    }

    public MenuInstance openSafely(@NonNull HumanEntity viewer) {
        this.getMenu().getMenuProvider().openSafely(null, this, viewer);
        return this;
    }

    public MenuInstance openSafely(MenuInstance parent, @NonNull HumanEntity viewer) {
        this.getMenu().getMenuProvider().openSafely(parent, this, viewer);
        return this;
    }

    public MenuInstance update() {

        Player target = Bukkit.getPlayer(this.getLastRenderTarget());
        if (target == null) {
            return this;
        }

        if (this.getMeta().getUpdateHook() != null) {
            this.getMeta().getUpdateHook().onUpdate(this, target);
            return this;
        }

        return this.render(target);
    }
}
