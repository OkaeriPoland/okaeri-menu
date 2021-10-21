package eu.okaeri.menu.core.display;

import eu.okaeri.menu.core.meta.MenuItemMeta;
import lombok.NonNull;

public interface DisplayProvider<V, I> {
    I displayFor(@NonNull V viewer, @NonNull MenuItemMeta<V, I> menuItem);
}
