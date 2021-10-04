package eu.okaeri.menu.core.handler;

import eu.okaeri.menu.core.meta.MenuItemDeclaration;
import lombok.NonNull;

public interface SimpleClickHandler<V> {
    void onClick(@NonNull V viewer, MenuItemDeclaration menuItem);
}
