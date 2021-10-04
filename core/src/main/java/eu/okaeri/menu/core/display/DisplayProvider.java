package eu.okaeri.menu.core.display;

import eu.okaeri.menu.core.meta.MenuItemDeclaration;
import lombok.NonNull;

public interface DisplayProvider<V, I> {
    I displayFor(@NonNull V viewer, @NonNull MenuItemDeclaration menuItem);
}
