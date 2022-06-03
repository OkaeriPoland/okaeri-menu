package eu.okaeri.menu.core.handler;

import eu.okaeri.menu.core.meta.MenuItemMeta;
import lombok.NonNull;

public interface SimpleClickHandler<V, I, C> {
    void onClick(@NonNull V viewer, @NonNull MenuItemMeta<V, I, C> menuItem, @NonNull C clickType);
}
