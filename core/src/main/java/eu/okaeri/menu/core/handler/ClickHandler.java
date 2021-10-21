package eu.okaeri.menu.core.handler;

import eu.okaeri.menu.core.meta.MenuItemMeta;
import lombok.NonNull;

public interface ClickHandler<V, I> {
    void onClick(@NonNull V viewer, @NonNull MenuItemMeta<V, I> menuItem, @NonNull I item);
}
