package eu.okaeri.menu.bukkit.handler;

import eu.okaeri.menu.bukkit.meta.MenuInputMeta;
import lombok.NonNull;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface InputHandler {
    boolean onInput(@NonNull HumanEntity viewer, @NonNull MenuInputMeta menuInput, @NonNull ItemStack cursor, @Nullable ItemStack item, int slot);
}
