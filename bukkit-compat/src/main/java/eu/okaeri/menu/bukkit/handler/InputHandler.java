package eu.okaeri.menu.bukkit.handler;

import eu.okaeri.menu.bukkit.meta.MenuInputMeta;
import lombok.NonNull;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * @deprecated Use the new 2.0 API with {@link eu.okaeri.menu.item.MenuItem.Builder#interactive()}
 */
@Deprecated(since = "0.0.18", forRemoval = true)
public interface InputHandler {
    boolean onInput(@NonNull HumanEntity viewer, @NonNull MenuInputMeta menuInput, @NonNull ItemStack cursor, @Nullable ItemStack item, int slot);
}
