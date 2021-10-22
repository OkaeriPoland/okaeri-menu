package eu.okaeri.menu.core.handler;

import eu.okaeri.menu.core.meta.MenuItemMeta;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public interface ClickHandler<V, I> {
    boolean onClick(@NonNull V viewer, @NonNull MenuItemMeta<V, I> menuItem, @Nullable I item, int slot);
}
